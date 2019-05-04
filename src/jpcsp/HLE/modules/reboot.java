/*
 This file is part of jpcsp.

 Jpcsp is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpcsp is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.HLE.modules;

import static jpcsp.HLE.Modules.LoadCoreForKernelModule;
import static jpcsp.HLE.modules.InitForKernel.SCE_INIT_APITYPE_KERNEL_REBOOT;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Addr;
import static jpcsp.HLE.modules.SysMemUserForUser.VSHELL_PARTITION_ID;
import static jpcsp.util.Utilities.readCompleteFile;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Interpreter;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEModuleManager;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelLoadExecVSHParam;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceLoadCoreBootInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.SceSysmemUidCB;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.KIRK;
import jpcsp.crypto.PRX;
import jpcsp.format.PSP;
import jpcsp.hardware.Model;
import jpcsp.memory.IntArrayMemory;
import jpcsp.memory.mmio.MMIO;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class reboot extends HLEModule {
    public static Logger log = Modules.getLogger("reboot");
    public static boolean enableReboot = false;
    public static boolean loadCoreInitialized = false;
    private static final String rebootFileName = "flash0:/reboot.bin";
    private static final int rebootBaseAddress = MemoryMap.START_KERNEL + 0x600000;
    private static final int rebootParamAddress = MemoryMap.START_KERNEL + 0x400000;
    private static final int BOOT_IPL          = 0;
    private static final int BOOT_LOADEXEC_PRX = 1;
    private static final int BOOT_REBOOT_BIN   = 2;
    private static final int bootMethod = BOOT_LOADEXEC_PRX;

    private static class SetLog4jMDC implements IAction {
		@Override
		public void execute() {
			setLog4jMDC(Emulator.getProcessor());
		}
    }

    public boolean loadAndRun() {
    	if (!enableReboot) {
    		return false;
    	}

        Model.setModel(Settings.getInstance().readInt("emu.model"));
    	Modules.SysMemUserForUserModule.setMemory64MB(Model.getGeneration() > 1);
        RuntimeContextLLE.start();
        RuntimeContext.updateMemory();

    	final boolean fromSyscall = false;
    	Emulator.getInstance().initNewPsp(fromSyscall);
    	Emulator.getProcessor().triggerReset();
    	Emulator.getInstance().setModuleLoaded(true);
    	HLEModuleManager.getInstance().startModules(fromSyscall);

    	boolean result;
    	switch (bootMethod) {
    		case BOOT_IPL:          result = bootIpl();         break;
    		case BOOT_LOADEXEC_PRX: result = bootLoadexecPrx(); break;
    		case BOOT_REBOOT_BIN:   result = bootRebootBin();   break;
    		default:                result = false;             break;
    	}

    	// This will set the Log4j MDC values for the root thread
    	Emulator.getScheduler().addAction(new SetLog4jMDC());

    	if (result) {
    		loadCoreInitialized = true;
    	}

    	return result;
    }

    /**
     * Load MIPS code to decompress data in KL4E format.
     * 
     * @param baseAddress where to load the module containing the decompress code
     * @return            the address where the MIPS code has been loaded
     */
    private TPointer getKl4eDecompress(TPointer baseAddress) {
    	// A modified variant of a KL4E decompressor can be found in np9660.prx.
    	// This module in compressed with GZIP so that we can retrieve the code
    	// without requiring a KL4E decompressor ;-).
    	// The other modules containing a KL4E decompressor (sysmem.prx, meimg.img, paf.prx, pafmini.prx)
    	// are all compressed using KL4E.
    	String fileName = "flash0:/kd/np9660.prx";

    	byte[] buffer = readCompleteFile(fileName);
    	if (buffer == null) {
    		return null;
    	}

    	SceModule module;
    	try {
			module = Loader.getInstance().LoadModule(fileName, ByteBuffer.wrap(buffer), baseAddress, KERNEL_PARTITION_ID, KERNEL_PARTITION_ID, false, false, true, true);
		} catch (IOException e) {
			return null;
		}

    	TPointer entryAddress = null;
    	int functionSize = 0;
    	TPointer moduleTextAddr = new TPointer(baseAddress.getMemory(), module.text_addr);
    	// Search for the function entry and apply a few patches to it so that it can decompress KL4E data.
    	for (int i = 0; i < module.text_size; i += 4) {
    		int opcode = moduleTextAddr.getValue32(i);
    		if (opcode == 0x27BDF4F0) { // addiu $sp, $sp, -2832
    			// Start of the function
    			entryAddress = new TPointer(moduleTextAddr,  i);
    			// Replace "addiu $t3, $a0, -1" with "addiu $t3, $a0, 0"
    			entryAddress.setValue32(0xC, entryAddress.getValue32(0xC) & 0xFFFF0000);
    			// Replace "j loc_00001358" with "j loc_00001390"
    			entryAddress.setValue32(0x90, entryAddress.getValue32(0x90) + 14);
    		} else if (entryAddress != null) {
    			if ((opcode & 0xFFFF8000) == 0x27BD8000) { // addiu $sp, $sp, -n
    				// End of the function
    				functionSize = i;
    				break;
    			} else if ((opcode & 0xFFE007FF) == 0x000000C0) { // sll $x, $y, 3
    				// Replace "sll $x, $y, 3" with "sll $x, $y, 4"
    				moduleTextAddr.setValue32(i, (opcode & 0xFFFFF83F) | (4 << 6));
    			} else if ((opcode & 0xFFFF07FF) ==  0x000D00C2) { // srl $x, $t5, 3
    				// Replace "srl $x, $t5, 3" with "srl $x, $t5, 4"
    				moduleTextAddr.setValue32(i, (opcode & 0xFFFFF83F) | (4 << 6));
    			} else if (opcode == 0x2463001F) { // addiu $v1, $v1, 31
    				// Replace "addiu $v1, $v1, 31" with "addiu $v1, $v1, 15"
    				moduleTextAddr.setValue32(i, opcode - 16);
    			} else if (opcode == 0x24120160) { // li $s2, 0x160
    				// Replace "li $s2, 0x160" with "li $s2, 0x100"
    				moduleTextAddr.setValue32(i, opcode - 0x60);
    			}
    		}
    	}

    	if (entryAddress == null) {
    		return null;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("KL4E decompress at %s",  entryAddress));
    		if (log.isTraceEnabled()) {
    			for (int i = 0; i < functionSize; i += 4) {
    				int opcode = entryAddress.getValue32(i);
    				int addr = entryAddress.getAddress() + i;
    				log.trace(String.format("0x%08X: 0x%08X %s", addr, opcode, Decoder.instruction(opcode).disasm(addr, opcode)));
    			}
    		}
    	}

    	return entryAddress;
    }

    /**
     * Load MIPS code to decompress data in KL3E format.
     * We need a valid KL4E decompressor to load this code (see getKl4eDecompressor()).
     * 
     * @param baseAddress    where to load the module containing the decompress code
     * @param kl4eDecompress the address of the MIPS code to decompress data in KL4E
     * @param tempBuffer     a temporary buffer that can be used by the KL4E decompressor
     * @param tempBufferSize the size of the temporary buffer
     * @return               the address where the MIPS code has been loaded
     */
    private TPointer getKl3eDecompress(TPointer baseAddress, TPointer kl4eDecompress, TPointer tempBuffer, int tempBufferSize) {
    	String fileName = String.format("flash0:/kd/loadexec_%02dg.prx", Model.getGeneration());

    	byte[] buffer = readCompleteFile(fileName);
    	if (buffer == null) {
    		return null;
    	}

    	// Decrypt and decompress loadexec.prx using the provided KL4E decompressor.
    	PRX prxEngine = new CryptoEngine().getPRXEngine();
    	buffer = prxEngine.DecryptAndUncompressPRX(buffer, buffer.length, true, kl4eDecompress, tempBuffer, tempBufferSize);
    	if (buffer == null) {
    		return null;
    	}

    	SceModule module;
    	try {
			module = Loader.getInstance().LoadModule(fileName, ByteBuffer.wrap(buffer), baseAddress, KERNEL_PARTITION_ID, KERNEL_PARTITION_ID, false, false, true, true);
		} catch (IOException e) {
			return null;
		}

    	TPointer entryAddress = null;
    	int functionSize = 0;
    	TPointer moduleTextAddr = new TPointer(baseAddress.getMemory(), module.text_addr);
    	// Search for the function entry
    	for (int i = 0; i < module.text_size; i += 4) {
    		int opcode = moduleTextAddr.getValue32(i);
    		if (opcode == 0x27BDF588) { // addiu $sp, $sp, -2680
    			// Start of the function
    			entryAddress = new TPointer(moduleTextAddr,  i);
    		} else if (entryAddress != null) {
    			if ((opcode & 0xFFFF8000) == 0x27BD8000) { // addiu $sp, $sp, -n
    				// End of the function
    				functionSize = i;
    				break;
    			}
    		}
    	}

    	if (entryAddress == null) {
    		return null;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("KL3E decompress at %s",  entryAddress));
    		if (log.isTraceEnabled()) {
    			for (int i = 0; i < functionSize; i += 4) {
    				int opcode = entryAddress.getValue32(i);
    				int addr = entryAddress.getAddress() + i;
    				log.trace(String.format("0x%08X: 0x%08X %s", addr, opcode, Decoder.instruction(opcode).disasm(addr, opcode)));
    			}
    		}
    	}

    	return entryAddress;
    }

    /**
     * Boot using the reboot.prx code found in flash0:/kd/loadexec_01g.prx.
     * 
     * @return true if can be booted
     */
    private boolean bootLoadexecPrx() {
    	String loadexecFileName = String.format("flash0:/kd/loadexec_%02dg.prx", Model.getGeneration());

    	byte[] loadexecBuffer = readCompleteFile(loadexecFileName);
    	if (loadexecBuffer == null) {
    		return false;
    	}

    	SceModule rebootModule = new SceModule(true);
    	rebootModule.modname = getName();
    	rebootModule.pspfilename = loadexecFileName;
    	rebootModule.baseAddress = rebootBaseAddress;
    	rebootModule.text_addr = rebootBaseAddress;
    	rebootModule.text_size = 0;
    	rebootModule.data_size = 0;
    	rebootModule.bss_size = 0x26B80;

    	Modules.ThreadManForUserModule.Initialise(rebootModule, rebootModule.baseAddress, 0, rebootModule.pspfilename, -1, 0, false);

    	Memory mem = Memory.getInstance();

    	IntArrayMemory intArrayMemory = new IntArrayMemory(new int[0x40000 >> 2], 0, MemoryMap.START_RAM);
    	TPointer tempMemory = intArrayMemory.getPointer();
    	int offset = 0;
    	TPointer kl4eBaseAddress = new TPointer(tempMemory, offset);
    	offset += 0x10000;
    	TPointer kl3eBaseAddress = new TPointer(tempMemory, offset);
    	offset += 0x10000;
    	TPointer tempBuffer = new TPointer(tempMemory, offset);
    	int tempBufferSize = 0x20000;

    	TPointer kl4eDecompress = getKl4eDecompress(kl4eBaseAddress);
    	TPointer kl3eDecompress = getKl3eDecompress(kl3eBaseAddress, kl4eDecompress, tempBuffer, tempBufferSize);

    	PRX prxEngine = new CryptoEngine().getPRXEngine();
    	loadexecBuffer = prxEngine.DecryptAndUncompressPRX(loadexecBuffer, loadexecBuffer.length, true, kl4eDecompress, tempBuffer, tempBufferSize);
    	if (loadexecBuffer == null) {
    		return false;
    	}

    	// Search for the reboot.prx inside the loadexec.prx.
    	// The reboot.prx starts with "~PSP".
    	int rebootOffset = -1;
    	for (int i = 0; i < loadexecBuffer.length; i += 4) {
    		if (readUnaligned32(loadexecBuffer, i) == PSP.PSP_MAGIC) {
    			rebootOffset = i;
    			break;
    		}
    	}
    	if (rebootOffset < 0) {
    		return false;
    	}

    	int rebootLength = readUnaligned32(loadexecBuffer, rebootOffset + 0x2C);
    	byte[] rebootBuffer = new byte[rebootLength];
    	System.arraycopy(loadexecBuffer, rebootOffset, rebootBuffer, 0, rebootLength);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Found reboot.prx at offset 0x%X: %s", rebootOffset, Utilities.getMemoryDump(rebootBuffer)));
    	}

    	rebootBuffer = prxEngine.DecryptAndUncompressPRX(rebootBuffer, rebootLength, false);

    	try {
			offset = 0;
			tempBuffer.alignUp(15);
			TPointer inputBufferAddr = new TPointer(tempBuffer, offset);
			offset += Utilities.alignUp(rebootBuffer.length, 15); // Stack must be 16-bytes aligned
			int stackSize = Utilities.alignUp(3000, 15);
			TPointer stackBufferAddr = new TPointer(tempBuffer, offset);
			offset += stackSize;
			offset = Utilities.alignUp(offset, 63);
			TPointer outputBufferAddr = new TPointer(tempBuffer, offset);

			inputBufferAddr.setArray(0, rebootBuffer, 4, rebootBuffer.length - 4);

			int outputSize = 0x20000;
			if (log.isDebugEnabled()) {
				log.debug(String.format("Calling KL3E decompress at %s: input %s(size=0x%X), output %s(size=0x%X)", kl3eDecompress, inputBufferAddr, rebootBuffer.length, outputBufferAddr, outputSize));
			}

			Processor processor = new Processor();
			processor.cpu.setMemory(kl3eDecompress.getMemory());
			processor.cpu._a0 = outputBufferAddr.getAddress();
			processor.cpu._a1 = outputSize;
			processor.cpu._a2 = inputBufferAddr.getAddress();
			processor.cpu._a3 = 0;
			processor.cpu._sp = (stackBufferAddr.getAddress() + stackSize) | 0x80000000;
			Interpreter interpreter = new Interpreter(processor);
			interpreter.run(kl3eDecompress.getAddress());

			int result = processor.cpu._v0;
			if (result < 0) {
				return false;
			}
			rebootBuffer = outputBufferAddr.getArray8(0, result);
			if (log.isDebugEnabled()) {
				log.debug(String.format("KL3E decompress returned size=0x%X", result));
			}
		} catch (Exception e) {
			log.error("KL3E decompress", e);
		}

    	// The memory remap has already been done by the IPL code at this point
    	RuntimeContextLLE.getMMIO().remapMemoryAtProcessorReset();

    	int rebootMemSize = rebootModule.text_size + rebootModule.data_size + rebootModule.bss_size;
    	SysMemInfo rebootMemInfo = Modules.SysMemUserForUserModule.malloc(VSHELL_PARTITION_ID, "reboot", PSP_SMEM_Addr, rebootMemSize, rebootModule.text_addr);
    	if (rebootMemInfo == null) {
    		return false;
    	}

    	TPointer rebootBinAddr = new TPointer(mem, rebootBaseAddress);
    	rebootBinAddr.setArray(rebootBuffer);
    	rebootModule.text_size = rebootBuffer.length;

    	addFunctionNames(rebootModule);

		SysMemInfo rebootParamInfo = Modules.SysMemUserForUserModule.malloc(VSHELL_PARTITION_ID, "reboot-parameters", PSP_SMEM_Addr, 0x10000, rebootParamAddress);
		TPointer sceLoadCoreBootInfoAddr = new TPointer(mem, rebootParamInfo.addr);
		SceLoadCoreBootInfo sceLoadCoreBootInfo = new SceLoadCoreBootInfo();

		sceLoadCoreBootInfoAddr.clear(0x1000 + 0x1C000 + 0x380);

		TPointer startAddr = new TPointer(sceLoadCoreBootInfoAddr, 0x1000);

		TPointer sceKernelLoadExecVSHParamAddr = new TPointer(startAddr, 0x1C000);
		TPointer loadModeStringAddr = new TPointer(sceKernelLoadExecVSHParamAddr, 48);
		loadModeStringAddr.setStringZ("vsh");
		SceKernelLoadExecVSHParam sceKernelLoadExecVSHParam = new SceKernelLoadExecVSHParam();
		sceKernelLoadExecVSHParamAddr.setValue32(48);
		sceKernelLoadExecVSHParam.flags = 0x10000;
		sceKernelLoadExecVSHParam.keyAddr = loadModeStringAddr;
		sceKernelLoadExecVSHParam.write(sceKernelLoadExecVSHParamAddr);

		sceLoadCoreBootInfo.memBase = MemoryMap.START_KERNEL;
		sceLoadCoreBootInfo.memSize = MemoryMap.SIZE_RAM;
		sceLoadCoreBootInfo.startAddr = startAddr;
		sceLoadCoreBootInfo.endAddr = new TPointer(sceKernelLoadExecVSHParamAddr, 0x380);
		sceLoadCoreBootInfo.modProtId = -1;
		sceLoadCoreBootInfo.modArgProtId = -1;
		sceLoadCoreBootInfo.model = Model.getModel();
		sceLoadCoreBootInfo.dipswLo = Modules.KDebugForKernelModule.sceKernelDipswLow32();
		sceLoadCoreBootInfo.dipswHi = Modules.KDebugForKernelModule.sceKernelDipswHigh32();
		sceLoadCoreBootInfo.unknown72 = MemoryMap.END_USERSPACE | 0x80000000; // Must be larger than 0x89000000 + size of pspbtcnf.bin file
		sceLoadCoreBootInfo.cpTime = Modules.KDebugForKernelModule.sceKernelDipswCpTime();
		sceLoadCoreBootInfo.write(sceLoadCoreBootInfoAddr);

    	SceKernelThreadInfo rootThread = Modules.ThreadManForUserModule.getRootThread(null);
    	if (rootThread != null) {
			rootThread.cpuContext._a0 = sceLoadCoreBootInfoAddr.getAddress();
			rootThread.cpuContext._a1 = sceKernelLoadExecVSHParamAddr.getAddress();
			rootThread.cpuContext._a2 = SCE_INIT_APITYPE_KERNEL_REBOOT;
			rootThread.cpuContext._a3 = Modules.SysMemForKernelModule.sceKernelGetInitialRandomValue();
    	}

    	if (log.isDebugEnabled()) {
			log.debug(String.format("sceReboot arg0=%s, arg1=%s", sceLoadCoreBootInfoAddr, sceKernelLoadExecVSHParamAddr));
		}

    	return true;
    }

    /**
     * Boot using the reboot.prx code found in flash0:/reboot.bin.
     * This file needs first to be extracted using a real PSP.
     * 
     * @return true if can be booted
     */
    private boolean bootRebootBin() {
    	StringBuilder localFileName = new StringBuilder();
    	IVirtualFileSystem vfs = Modules.IoFileMgrForUserModule.getVirtualFileSystem(rebootFileName, localFileName);
    	if (vfs == null) {
    		return false;
    	}

    	IVirtualFile vFile = vfs.ioOpen(localFileName.toString(), IoFileMgrForUser.PSP_O_RDONLY, 0);
    	if (vFile == null) {
    		return false;
    	}

    	int rebootFileLength = (int) vFile.length();
    	if (rebootFileLength <= 0) {
    		return false;
    	}

    	Memory mem = Memory.getInstance();

    	SceModule rebootModule = new SceModule(true);
    	rebootModule.modname = getName();
    	rebootModule.pspfilename = rebootFileName;
    	rebootModule.baseAddress = rebootBaseAddress;
    	rebootModule.text_addr = rebootBaseAddress;
    	rebootModule.text_size = rebootFileLength;
    	rebootModule.data_size = 0;
    	rebootModule.bss_size = 0x26B80;

    	Modules.ThreadManForUserModule.Initialise(rebootModule, rebootModule.baseAddress, 0, rebootModule.pspfilename, -1, 0, false);

    	MMIO mmio = (MMIO) RuntimeContextLLE.getMMIO();
    	// The memory remap has already been done by the IPL code at this point
    	mmio.remapMemoryAtProcessorReset();

    	int rebootMemSize = rebootModule.text_size + rebootModule.data_size + rebootModule.bss_size;
    	SysMemInfo rebootMemInfo = Modules.SysMemUserForUserModule.malloc(VSHELL_PARTITION_ID, "reboot", PSP_SMEM_Addr, rebootMemSize, rebootModule.text_addr);
    	if (rebootMemInfo == null) {
    		return false;
    	}

    	TPointer rebootBinAddr = new TPointer(mem, rebootBaseAddress);
    	int readLength = vFile.ioRead(rebootBinAddr, rebootFileLength);
    	vFile.ioClose();
    	if (readLength != rebootFileLength) {
    		return false;
    	}

    	addFunctionNames(rebootModule);

		SysMemInfo rebootParamInfo = Modules.SysMemUserForUserModule.malloc(VSHELL_PARTITION_ID, "reboot-parameters", PSP_SMEM_Addr, 0x10000, rebootParamAddress);
		TPointer sceLoadCoreBootInfoAddr = new TPointer(mem, rebootParamInfo.addr);
		SceLoadCoreBootInfo sceLoadCoreBootInfo = new SceLoadCoreBootInfo();

		sceLoadCoreBootInfoAddr.clear(0x1000 + 0x1C000 + 0x380);

		TPointer startAddr = new TPointer(sceLoadCoreBootInfoAddr, 0x1000);

		TPointer sceKernelLoadExecVSHParamAddr = new TPointer(startAddr, 0x1C000);
		TPointer loadModeStringAddr = new TPointer(sceKernelLoadExecVSHParamAddr, 48);
		loadModeStringAddr.setStringZ("vsh");
		SceKernelLoadExecVSHParam sceKernelLoadExecVSHParam = new SceKernelLoadExecVSHParam();
		sceKernelLoadExecVSHParamAddr.setValue32(48);
		sceKernelLoadExecVSHParam.flags = 0x10000;
		sceKernelLoadExecVSHParam.keyAddr = loadModeStringAddr;
		sceKernelLoadExecVSHParam.write(sceKernelLoadExecVSHParamAddr);

		sceLoadCoreBootInfo.memBase = MemoryMap.START_KERNEL;
		sceLoadCoreBootInfo.memSize = MemoryMap.SIZE_RAM;
		sceLoadCoreBootInfo.startAddr = startAddr;
		sceLoadCoreBootInfo.endAddr = new TPointer(sceKernelLoadExecVSHParamAddr, 0x380);
		sceLoadCoreBootInfo.modProtId = -1;
		sceLoadCoreBootInfo.modArgProtId = -1;
		sceLoadCoreBootInfo.model = Model.getModel();
		sceLoadCoreBootInfo.dipswLo = Modules.KDebugForKernelModule.sceKernelDipswLow32();
		sceLoadCoreBootInfo.dipswHi = Modules.KDebugForKernelModule.sceKernelDipswHigh32();
		sceLoadCoreBootInfo.unknown72 = MemoryMap.END_USERSPACE | 0x80000000; // Must be larger than 0x89000000 + size of pspbtcnf.bin file
		sceLoadCoreBootInfo.cpTime = Modules.KDebugForKernelModule.sceKernelDipswCpTime();
		sceLoadCoreBootInfo.write(sceLoadCoreBootInfoAddr);

    	SceKernelThreadInfo rootThread = Modules.ThreadManForUserModule.getRootThread(null);
    	if (rootThread != null) {
			rootThread.cpuContext._a0 = sceLoadCoreBootInfoAddr.getAddress();
			rootThread.cpuContext._a1 = sceKernelLoadExecVSHParamAddr.getAddress();
			rootThread.cpuContext._a2 = SCE_INIT_APITYPE_KERNEL_REBOOT;
			rootThread.cpuContext._a3 = Modules.SysMemForKernelModule.sceKernelGetInitialRandomValue();
    	}

    	if (log.isDebugEnabled()) {
			log.debug(String.format("sceReboot arg0=%s, arg1=%s", sceLoadCoreBootInfoAddr, sceKernelLoadExecVSHParamAddr));
		}

    	return true;
    }

    /**
     * Boot using the IPL code is not working as it is computing a hash
     * on the IPL code located at 0xBFC00000 and using this hash value
     * to decrypt parts of the code loaded from the NAND.
     * 
     * @return true if can be booted
     */
    private boolean bootIpl() {
		byte[] buffer = new byte[0x20000];
    	try {
			InputStream is = new FileInputStream("nand.ipl.bin");
			is.read(buffer);
			is.close();
    	} catch (IOException e) {
			return false;
		}

    	Memory mem = Memory.getInstance();

    	byte[] page = new byte[0x1000];
		int nextAddr = 0;
		int iplBase = 0;
		int iplEntry = 0;
		for (int i = 0; i < buffer.length && iplEntry == 0; i += 0x1000) {
			System.arraycopy(buffer, i, page, 0, 0x1000);
			int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(page, 0, 0x1000, page, 0, 0x500, KIRK.PSP_KIRK_CMD_DECRYPT_PRIVATE);
			if (result != 0) {
				log.error(String.format("hleUtilsBufferCopyWithRange returned 0x%08X", result));
			}

			int addr = readUnaligned32(page, 0);
			int length = readUnaligned32(page, 4);
			iplEntry = readUnaligned32(page, 8);
			int checksum = readUnaligned32(page, 12);
			if (log.isTraceEnabled()) {
				log.trace(String.format("IPL block 0x%X: addr=0x%08X, length=0x%X, entry=0x%08X, checkSum=0x%08X", i, addr, length, iplEntry, checksum));
			}

			if (iplBase == 0) {
				iplBase = addr;
			} else if (addr != nextAddr) {
				log.error(String.format("Error at IPL offset 0x%X: 0x%08X != 0x%08X", i, addr, nextAddr));
			}
			mem.copyToMemory(addr, ByteBuffer.wrap(page, 0x10, length), length);
			nextAddr = addr + length;
		}
		int iplSize = nextAddr - iplBase;

		if (log.isTraceEnabled()) {
			log.trace(String.format("IPL size=0x%X: %s", iplSize, Utilities.getMemoryDump(mem, iplBase, iplSize)));
		}

		SceModule iplModule = new SceModule(true);
		iplModule.modname = getName();
		iplModule.pspfilename = "ipl:";
		iplModule.baseAddress = iplBase;
		iplModule.text_addr = iplBase;
		iplModule.text_size = iplSize;
		iplModule.data_size = 0;
		iplModule.bss_size = 0;

    	Modules.ThreadManForUserModule.Initialise(iplModule, iplEntry, 0, iplModule.pspfilename, -1, 0, false);

    	Compiler compiler = Compiler.getInstance();
    	compiler.addMMIORange(iplBase, iplSize);

		SceModule rebootModule = new SceModule(true);
    	rebootModule.baseAddress = rebootBaseAddress;
    	rebootModule.text_addr = rebootBaseAddress;
    	addFunctionNames(rebootModule);

    	return true;
	}

	private void addFunctionNid(int moduleAddress, SceModule module, String name) {
    	int nid = HLEModuleManager.getInstance().getNIDFromFunctionName(name);
    	if (nid != 0) {
    		int address = module.text_addr + moduleAddress;
    		LoadCoreForKernelModule.addFunctionNid(address, nid);
    	}
    }

    private void addFunctionNames(SceModule rebootModule) {
    	// These function names are taken from uOFW (https://github.com/uofw/uofw)
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x0080, "sceInit.patchGames");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x0218, "sceInit.InitCBInit");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x02E0, "sceInit.ExitInit");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x03F4, "sceInit.ExitCheck");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x0438, "sceInit.PowerUnlock");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x048C, "sceInit.invoke_init_callback");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x05F0, "sceInit.sub_05F0");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x06A8, "sceInit.CleanupPhase1");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x0790, "sceInit.CleanupPhase2");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x08F8, "sceInit.ProtectHandling");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x0CFC, "sceInit.sub_0CFC_IsModuleInUserPartition");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x0D4C, "sceInit.ClearFreeBlock");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x0DD0, "sceInit.sub_0DD0_IsApplicationTypeGame");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x1038, "sceInit.LoadModuleBufferAnchorInBtcnf");
    	LoadCoreForKernelModule.addFunctionName("sceInit",             0x1240, "sceInit.InitThreadEntry");
    	LoadCoreForKernelModule.addFunctionName("sceLoaderCore",       0x56B8, "sceLoaderCore.PspUncompress");
    	LoadCoreForKernelModule.addFunctionName("sceGE_Manager",       0x0258, "sceGE_Manager.sceGeInit");
    	LoadCoreForKernelModule.addFunctionName("sceMeCodecWrapper",   0x1C04, "sceMeCodecWrapper.decrypt");
    	LoadCoreForKernelModule.addFunctionName("sceAudio_Driver",     0x0000, "sceAudio_Driver.updateAudioBuf");
    	LoadCoreForKernelModule.addFunctionName("sceAudio_Driver",     0x137C, "sceAudio_Driver.audioOutput");
    	LoadCoreForKernelModule.addFunctionName("sceAudio_Driver",     0x0530, "sceAudio_Driver.audioOutputDmaCb");
    	LoadCoreForKernelModule.addFunctionName("sceAudio_Driver",     0x01EC, "sceAudio_Driver.dmaUpdate");
    	LoadCoreForKernelModule.addFunctionName("sceAudio_Driver",     0x1970, "sceAudio_Driver.audioIntrHandler");
    	LoadCoreForKernelModule.addFunctionName("sceAudio_Driver",     0x02B8, "sceAudio_Driver.audioMixerThread");
    	LoadCoreForKernelModule.addFunctionName("sceSYSCON_Driver",    0x0A10, "sceSYSCON_Driver._sceSysconGpioIntr");
    	LoadCoreForKernelModule.addFunctionName("sceSYSCON_Driver",    0x2434, "sceSYSCON_Driver._sceSysconPacketEnd");
    	LoadCoreForKernelModule.addFunctionName("sceDisplay_Service",  0x04EC, "sceDisplay_Service.sceDisplayInit");
    	LoadCoreForKernelModule.addFunctionName("scePower_Service",    0x0000, "scePower_Service.scePowerInit");
    	LoadCoreForKernelModule.addFunctionName("sceHP_Remote_Driver", 0x0704, "sceHP_Remote_Driver.sceHpRemoteThreadEntry");
    	LoadCoreForKernelModule.addFunctionName("sceLowIO_Driver",     0x9C7C, "sceNandTransferDataToNandBuf");

		// Mapping of subroutines defined in
		//     https://github.com/uofw/uofw/blob/master/src/reboot/unk.c
		// and https://github.com/uofw/uofw/blob/master/src/reboot/nand.c
    	int offset = -1;
    	switch (Model.getGeneration()) {
    		case 1: offset = 0x288; break; // Functions are shifted in reboot.bin 6.61
    		case 2: offset = 0x348; break; // Functions are shifted in reboot_02g.bin 6.61
    	}
    	if (offset != -1) {
    		// The below offsets are for reboot.bin 6.60
	    	addFunctionNid(offset + 0x0000EFCC, rebootModule, "sceNandInit2");
	    	addFunctionNid(offset + 0x0000F0C4, rebootModule, "sceNandIsReady");
	    	addFunctionNid(offset + 0x0000F0D4, rebootModule, "sceNandSetWriteProtect");
	    	addFunctionNid(offset + 0x0000F144, rebootModule, "sceNandLock");
	    	addFunctionNid(offset + 0x0000F198, rebootModule, "sceNandReset");
	    	addFunctionNid(offset + 0x0000F234, rebootModule, "sceNandReadId");
	    	addFunctionNid(offset + 0x0000F28C, rebootModule, "sceNandReadAccess");
	    	addFunctionNid(offset + 0x0000F458, rebootModule, "sceNandWriteAccess");
	    	addFunctionNid(offset + 0x0000F640, rebootModule, "sceNandEraseBlock");
	    	addFunctionNid(offset + 0x0000F72C, rebootModule, "sceNandReadExtraOnly");
	    	addFunctionNid(offset + 0x0000F8A8, rebootModule, "sceNandReadStatus");
	    	addFunctionNid(offset + 0x0000F8DC, rebootModule, "sceNandSetScramble");
	    	addFunctionNid(offset + 0x0000F8EC, rebootModule, "sceNandReadPages");
	    	addFunctionNid(offset + 0x0000F930, rebootModule, "sceNandWritePages");
	    	addFunctionNid(offset + 0x0000F958, rebootModule, "sceNandReadPagesRawExtra");
	    	addFunctionNid(offset + 0x0000F974, rebootModule, "sceNandWritePagesRawExtra");
	    	addFunctionNid(offset + 0x0000F998, rebootModule, "sceNandReadPagesRawAll");
	    	addFunctionNid(offset + 0x0000F9D0, rebootModule, "sceNandTransferDataToNandBuf");
	    	addFunctionNid(offset + 0x0000FC40, rebootModule, "sceNandIntrHandler");
	    	addFunctionNid(offset + 0x0000FF60, rebootModule, "sceNandTransferDataFromNandBuf");
	    	addFunctionNid(offset + 0x000103C8, rebootModule, "sceNandWriteBlockWithVerify");
	    	addFunctionNid(offset + 0x0001047C, rebootModule, "sceNandReadBlockWithRetry");
	    	addFunctionNid(offset + 0x00010500, rebootModule, "sceNandVerifyBlockWithRetry");
	    	addFunctionNid(offset + 0x00010650, rebootModule, "sceNandEraseBlockWithRetry");
	    	addFunctionNid(offset + 0x000106C4, rebootModule, "sceNandIsBadBlock");
	    	addFunctionNid(offset + 0x00010750, rebootModule, "sceNandDoMarkAsBadBlock");
	    	addFunctionNid(offset + 0x000109DC, rebootModule, "sceNandDetectChipMakersBBM");
	    	addFunctionNid(offset + 0x00010D1C, rebootModule, "sceNandGetPageSize");
	    	addFunctionNid(offset + 0x00010D28, rebootModule, "sceNandGetPagesPerBlock");
	    	addFunctionNid(offset + 0x00010D34, rebootModule, "sceNandGetTotalBlocks");
    	}
    }

    public static void dumpAllModulesAndLibraries() {
    	if (!enableReboot || !log.isTraceEnabled()) {
    		return;
    	}

    	Memory mem = Memory.getInstance();
    	int g_loadCore = 0x8802111C;
    	int registeredMods = mem.read32(g_loadCore + 524);
    	int registeredLibs = g_loadCore + 0;
    	dumpAllModules(mem, registeredMods);
    	for (int i = 0; i < 512; i += 4) {
    		dumpAllLibraries(mem, mem.read32(registeredLibs + i));
    	}
    }

    private static void dumpAllModules(Memory mem, int address) {
    	while (address != 0) {
    		String moduleName = Utilities.readStringNZ(address + 8, 27);
    		int textAddr = mem.read32(address + 108);
    		int textSize = mem.read32(address + 112);

			log.trace(String.format("Module '%s': text 0x%08X-0x%08X", moduleName, textAddr, textAddr + textSize));

			// Next
    		address = mem.read32(address);
    	}
    }

    private static void dumpAllLibraries(Memory mem, int address) {
    	while (address != 0) {
    		String libName = Utilities.readStringZ(mem.read32(address + 68));
    		int numExports = mem.read32(address + 16);
    		int entryTable = mem.read32(address + 32);

			log.trace(String.format("Library '%s':", libName));

			for (int i = 0; i < numExports; i++) {
    			int nid = mem.read32(entryTable + i * 4);
    			int entryAddress = mem.read32(entryTable + (i + numExports) * 4);

				log.trace(String.format("   0x%08X: 0x%08X", nid, entryAddress));
    		}

    		// Next
    		address = mem.read32(address);
    	}
    }

    /**
     * Set information about the current thread that can be used for logging in log4j:
     * - LLE-thread-name: the current thread name
     * - LLE-thread-uid: the current thread UID in the format 0x%X
     * - LLE-thread: the current thread name and uid
     *
     * These values can be used in LogSettings.xml inside a PatternLayout:
     *   <layout class="org.apache.log4j.PatternLayout">
     *     <param name="ConversionPattern" value="%d{HH:mm:ss,SSS} %5p %8c - %X{LLE-thread} - %m%n" />
     *   </layout>
     */
    public static void setLog4jMDC(Processor processor) {
    	if (!enableReboot) {
    		return;
    	}

    	// Do not change the name of the Media Engine Thread
    	if (processor.cp0.isMediaEngineCpu()) {
    		return;
    	}

    	boolean isInterruptContext = processor.cp0.getControlRegister(13) != 0;
    	if (isInterruptContext) {
    		RuntimeContext.setLog4jMDC("Interrupt");
    	} else {
        	Memory mem = Memory.getInstance();
        	int threadManInfo = 0x88048740;

	    	int currentThread = mem.read32(threadManInfo + 0);
	    	if (Memory.isAddressGood(currentThread)) {
				int uid = mem.read32(currentThread + 8);
				int cb = SysMemForKernel.getCBFromUid(uid);
				int nameAddr = mem.read32(cb + 16);
				String name = Utilities.readStringZ(mem, nameAddr);

				RuntimeContext.setLog4jMDC(name, uid);
	    	} else {
				RuntimeContext.setLog4jMDC("root");
	    	}
    	}
    }

    public static void dumpAllThreads() {
    	if (!enableReboot || !log.isTraceEnabled()) {
    		return;
    	}

    	Memory mem = Memory.getInstance();
    	int threadManInfo = 0x88048740;

    	int currentThread = mem.read32(threadManInfo + 0);
    	int nextThread = mem.read32(threadManInfo + 4);

    	dumpThreadTypeList(mem, mem.read32(threadManInfo + 1228));
    	dumpThread(mem, currentThread, "Current thread");
    	if (nextThread != 0 && nextThread != currentThread) {
    		dumpThread(mem, nextThread, "Next thread");
    	}
    	dumpThreadList(mem, threadManInfo + 1176, "Sleeping thread");
    	dumpThreadList(mem, threadManInfo + 1184, "Delayed thread");
    	dumpThreadList(mem, threadManInfo + 1192, "Stopped thread");
    	dumpThreadList(mem, threadManInfo + 1200, "Suspended thread");
    	dumpThreadList(mem, threadManInfo + 1208, "Dead thread");
    	dumpThreadList(mem, threadManInfo + 1216, "??? thread");
    	for (int priority = 0; priority < 128; priority++) {
    		dumpThreadList(mem, threadManInfo + 152 + priority * 8, String.format("Ready thread[prio=0x%X]", priority));
    	}
    }

    private static void dumpThreadTypeList(Memory mem, int list) {
    	for (int cb = mem.read32(list); cb != list; cb = mem.read32(cb)) {
    		SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    		sceSysmemUidCB.read(mem, cb);
    		dumpThread(mem, cb + sceSysmemUidCB.size * 4, "Thread");
    	}
    }

    private static void dumpThread(Memory mem, int address, String comment) {
		int uid = mem.read32(address + 8);
		int status = mem.read32(address + 12);
		int currentPriority = mem.read32(address + 16);

		StringBuilder waitInfo = new StringBuilder();
		if (SceKernelThreadInfo.isWaitingStatus(status)) {
			int waitType = mem.read32(address + 88);
			if (waitType != 0) {
				waitInfo.append(String.format(", waitType=0x%X(%s)", waitType, SceKernelThreadInfo.getWaitName(waitType)));
			}

			int waitTypeCBaddr = mem.read32(address + 92);
			if (waitTypeCBaddr != 0) {
				SceSysmemUidCB waitTypeCB = new SceSysmemUidCB();
				waitTypeCB.read(mem, waitTypeCBaddr);
				waitInfo.append(String.format(", waitUid=0x%X(%s)", waitTypeCB.uid, waitTypeCB.name));
			}

			if (waitType == SceKernelThreadInfo.PSP_WAIT_DELAY) {
				int waitDelay = mem.read32(address + 96);
				waitInfo.append(String.format(", waitDelay=0x%X", waitDelay));
			} else if (waitType == SceKernelThreadInfo.PSP_WAIT_EVENTFLAG) {
				int bits = mem.read32(address + 96);
				waitInfo.append(String.format(", waitEventFlagBits=0x%X", bits));
			}
		}

		int cb = SysMemForKernel.getCBFromUid(uid);
		SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
		sceSysmemUidCB.read(mem, cb);

		log.trace(String.format("%s: uid=0x%X, name='%s', status=0x%X(%s), currentPriority=0x%X%s", comment, uid, sceSysmemUidCB.name, status, SceKernelThreadInfo.getStatusName(status), currentPriority, waitInfo));
    }

    private static void dumpThreadList(Memory mem, int list, String comment) {
    	for (int address = mem.read32(list); address != list; address = mem.read32(address)) {
    		dumpThread(mem, address, comment);
    	}
    }
}
