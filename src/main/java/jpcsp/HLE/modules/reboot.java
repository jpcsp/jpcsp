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

import static jpcsp.Allegrex.Common._t9;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.getFirmwareVersion;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.getMMIO;
import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.HLE.Modules.LoadCoreForKernelModule;
import static jpcsp.HLE.modules.InitForKernel.SCE_INIT_APITYPE_KERNEL_REBOOT;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Addr;
import static jpcsp.HLE.modules.SysMemUserForUser.USER_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.VSHELL_PARTITION_ID;
import static jpcsp.util.HLEUtilities.JAL;
import static jpcsp.util.HLEUtilities.LUI;
import static jpcsp.util.HLEUtilities.MOVE;
import static jpcsp.util.HLEUtilities.SYSCALL;
import static jpcsp.MemoryMap.START_SCRATCHPAD;
import static jpcsp.hardware.Model.MODEL_PSP_BRITE;
import static jpcsp.hardware.Model.MODEL_PSP_FAT;
import static jpcsp.hardware.Model.MODEL_PSP_SLIM;
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_SERVICE_BATTERY;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.readCompleteFile;
import static jpcsp.util.Utilities.readUnaligned32;
import static libkirk.KirkEngine.KIRK_CMD_DECRYPT_IV_0;
import static libkirk.KirkEngine.KIRK_CMD_DECRYPT_PRIVATE;
import static libkirk.KirkEngine.KIRK_CMD_ECDSA_VERIFY;
import static libkirk.KirkEngine.KIRK_CMD_SHA1_HASH;
import static libkirk.KirkEngine.KIRK_MODE_CMD1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Interpreter;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEModuleManager;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.local.LocalVirtualFile;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelLoadExecVSHParam;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceLoadCoreBootInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.SceSysmemUidCB;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.PRX;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.format.PSP;
import jpcsp.hardware.Model;
import jpcsp.hardware.Nand;
import jpcsp.memory.ByteArrayMemory;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.IntArrayMemory;
import jpcsp.memory.MemoryWriter;
import jpcsp.memory.mmio.MMIO;
import jpcsp.memory.mmio.MMIOHandlerGpio;
import jpcsp.memory.mmio.MMIOHandlerKirk;
import jpcsp.memory.mmio.memorystick.MMIOHandlerMemoryStick;
import jpcsp.util.HLEUtilities;
import jpcsp.util.Utilities;
import libkirk.KirkEngine;
import libkirk.KirkEngine.KIRK_AES128CBC_HEADER;

public class reboot extends HLEModule {
    public static Logger log = Modules.getLogger("reboot");
    public static boolean enableReboot = false;
    public static boolean loadCoreInitialized = false;
    private static final String rebootFileName = "flash0:/reboot.bin";
    private static final int rebootBaseAddress = MemoryMap.START_KERNEL + 0x600000;
    private static final int BOOT_PREIPL       = 0;
    private static final int BOOT_IPL          = 1;
    private static final int BOOT_LOADEXEC_PRX = 2;
    private static final int BOOT_REBOOT_BIN   = 3;
    // The address of the Pre-IPL code
    private final int preIplAddress = 0xBFC00000;
    private final int preIplSize = 0x1000;
    private final static int IPL_KEY_MODEL_GENERATION_1 = 0x59119BCF;
    private final static int IPL_KEY_MODEL_GENERATION_2 = 0xBC6532EC;
	// The Pre-IPL code is copying itself to 0x80010000
	private final TPointer preIplCopy = new TPointer(Memory.getInstance(), START_SCRATCHPAD | 0x80000000);
	private int iplEntry;
	private int iplBase;
	private int iplSize;
	private boolean usingKbooti;

    private static class SetLog4jMDC implements IAction {
		@Override
		public void execute() {
			setLog4jMDC(Emulator.getProcessor());
		}
    }

    private boolean isFilePresent(String fileName) {
    	StringBuilder localFileName = new StringBuilder();
    	IVirtualFileSystem vfs = Modules.IoFileMgrForUserModule.getVirtualFileSystem(fileName, localFileName);
    	if (vfs == null) {
    		return false;
    	}

    	IVirtualFile vFile = vfs.ioOpen(localFileName.toString(), IoFileMgrForUser.PSP_O_RDONLY, 0);
    	if (vFile == null) {
    		return false;
    	}

    	int length = (int) vFile.length();
    	vFile.ioClose();

    	return length > 0;
    }

    public boolean isAvailable() {
    	Model.init();

    	String generationSuffix = "";
    	// Starting with Firmware 5.00, some file names include the PSP generation number
    	if (getFirmwareVersion() >= 500) {
    		generationSuffix = String.format("_%02dg", Model.getGeneration());
    	}
    	final String[] fileNames = {
    			"flash0:/kd/loadexec%s.prx",
    			"flash0:/kd/sysmem.prx"
    	};

    	for (String fileName : fileNames) {
    		String completeFileName = String.format(fileName, generationSuffix);
    		if (!isFilePresent(completeFileName)) {
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("reboot is not available because the file '%s' is missing", completeFileName));
    			}
    			return false;
    		}
    	}

    	return true;
    }

	public boolean isUsingKbooti() {
		return usingKbooti;
	}

	public boolean loadAndRun() {
    	if (!enableReboot) {
    		return false;
    	}

        Model.init();
    	Modules.SysMemUserForUserModule.setMemory64MB(Model.getModel() > MODEL_PSP_FAT);
    	RuntimeContextLLE.reset();
        RuntimeContextLLE.start();
        RuntimeContext.updateMemory();

    	final boolean fromSyscall = false;
    	Emulator.getInstance().initNewPsp(fromSyscall);
    	Emulator.getProcessor().triggerReset();
    	Emulator.getInstance().setModuleLoaded(true);
    	HLEModuleManager.getInstance().startModules(fromSyscall);

    	boolean result;
    	int bootMethod = getBestBootMethod();
    	switch (bootMethod) {
    		case BOOT_IPL:          result = bootIpl();         break;
    		case BOOT_LOADEXEC_PRX: result = bootLoadexecPrx(); break;
    		case BOOT_REBOOT_BIN:   result = bootRebootBin();   break;
    		case BOOT_PREIPL:       result = bootPreIpl();      break;
    		default:                result = false;             break;
    	}

    	// This will set the Log4j MDC values for the root thread
    	Emulator.getScheduler().addAction(new SetLog4jMDC());

    	if (result) {
    		loadCoreInitialized = true;
    	}

    	return result;
    }

    private String getPreIplFileName() {
    	return String.format("preIpl_%02dg.bin", Model.getGeneration());
    }

    private int getBestBootMethod() {
    	int bootMethod;

		File preIplFile = new File(getPreIplFileName());
		if (preIplFile.canRead() && preIplFile.length() == 0x1000) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Best boot method is BOOT_PREIPL for PSP Model '%s'", Model.getModelName()));
			}
			bootMethod = BOOT_PREIPL;
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Best boot method is BOOT_IPL for PSP Model '%s'", Model.getModelName()));
			}
			bootMethod = BOOT_IPL;
		}

    	return bootMethod;
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
			module = Loader.getInstance().LoadModule(fileName, ByteBuffer.wrap(buffer), baseAddress, KERNEL_PARTITION_ID, KERNEL_PARTITION_ID, false, false, true, true, null);
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
    	buffer = prxEngine.DecryptAndUncompressPRX(buffer, buffer.length, true, null, kl4eDecompress, tempBuffer, tempBufferSize);
    	if (buffer == null) {
    		return null;
    	}

    	SceModule module;
    	try {
			module = Loader.getInstance().LoadModule(fileName, ByteBuffer.wrap(buffer), baseAddress, KERNEL_PARTITION_ID, KERNEL_PARTITION_ID, false, false, true, true, null);
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

    private void initRebootParameters(Memory mem, SceModule rebootModule) {
    	addFunctionNames(rebootModule);

    	int rebootParamAddressOffset;
    	int rebootParamPartitionId;
    	if (Model.getModel() == Model.MODEL_PSP_FAT) {
        	rebootParamAddressOffset = 0x400000;
        	rebootParamPartitionId = VSHELL_PARTITION_ID;
    	} else {
        	rebootParamAddressOffset = 0x3800000;
        	rebootParamPartitionId = USER_PARTITION_ID;
    	}
    	int rebootParamAddress = MemoryMap.START_KERNEL + rebootParamAddressOffset + 0x40;

		SysMemInfo rebootParamInfo = Modules.SysMemUserForUserModule.malloc(rebootParamPartitionId, "reboot-parameters", PSP_SMEM_Addr, 0x10000, rebootParamAddress);
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
		sceLoadCoreBootInfo.model = Model.getGeneration() - 1;
		sceLoadCoreBootInfo.dipswLo = Modules.KDebugForKernelModule.sceKernelDipswLow32();
		sceLoadCoreBootInfo.dipswHi = Modules.KDebugForKernelModule.sceKernelDipswHigh32();
		if (Modules.KDebugForKernelModule.sceKernelDipsw(30) != 0x1) {
			sceLoadCoreBootInfo.dipswHi = clearBit(sceLoadCoreBootInfo.dipswHi, 24);
			sceLoadCoreBootInfo.dipswHi = clearBit(sceLoadCoreBootInfo.dipswHi, 26);
		}
		sceLoadCoreBootInfo.unknown72 = ((MemoryMap.END_USERSPACE | 0x80000000) & ~0xFF) - 0x400; // Must be larger than 0x89000000 + size of pspbtcnf.bin file
		sceLoadCoreBootInfo.unknown76 = sceLoadCoreBootInfo.unknown72;
		sceLoadCoreBootInfo.cpTime = Modules.KDebugForKernelModule.sceKernelDipswCpTime();
		sceLoadCoreBootInfo.write(sceLoadCoreBootInfoAddr);

    	SceKernelThreadInfo rootThread = Modules.ThreadManForUserModule.getRootThread(null);
    	if (rootThread != null) {
			rootThread.cpuContext._a0 = sceLoadCoreBootInfoAddr.getAddress() | MemoryMap.START_KERNEL;
			rootThread.cpuContext._a1 = sceKernelLoadExecVSHParamAddr.getAddress() | MemoryMap.START_KERNEL;
			rootThread.cpuContext._a2 = SCE_INIT_APITYPE_KERNEL_REBOOT;
			rootThread.cpuContext._a3 = Modules.SysMemForKernelModule.sceKernelGetInitialRandomValue();
    	}

    	if (log.isDebugEnabled()) {
			log.debug(String.format("sceReboot arg0=%s, arg1=%s", sceLoadCoreBootInfoAddr, sceKernelLoadExecVSHParamAddr));
		}
    }

	private SceModule createRebootModule() {
    	SceModule rebootModule = new SceModule(true);
    	rebootModule.modname = getName();
    	rebootModule.baseAddress = rebootBaseAddress;
    	rebootModule.text_addr = rebootBaseAddress;
    	rebootModule.text_size = 0;
    	rebootModule.data_size = 0;
    	rebootModule.bss_size = 0x26B80;

    	Modules.ThreadManForUserModule.Initialise(rebootModule, rebootModule.baseAddress, 0, rebootModule.pspfilename, -1, 0, false);

    	return rebootModule;
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

    	SceModule rebootModule = createRebootModule();

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
    	loadexecBuffer = prxEngine.DecryptAndUncompressPRX(loadexecBuffer, loadexecBuffer.length, true, null, kl4eDecompress, tempBuffer, tempBufferSize);
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

    	rebootBuffer = prxEngine.DecryptAndUncompressPRX(rebootBuffer, rebootLength, false, null);

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
    	getMMIO().remapMemoryAtProcessorReset();

    	addMMIORange(0xBFC00F00, 0x100);

    	MMIOHandlerKirk.getInstance().setInitDone();

    	int rebootMemSize = rebootModule.text_size + rebootModule.data_size + rebootModule.bss_size;
    	SysMemInfo rebootMemInfo = Modules.SysMemUserForUserModule.malloc(VSHELL_PARTITION_ID, "reboot", PSP_SMEM_Addr, rebootMemSize, rebootModule.text_addr);
    	if (rebootMemInfo == null) {
    		return false;
    	}

    	Memory mem = Memory.getInstance();
    	TPointer rebootBinAddr = new TPointer(mem, rebootBaseAddress);
    	rebootBinAddr.setArray(rebootBuffer);
    	rebootModule.text_size = rebootBuffer.length;

    	initRebootParameters(mem, rebootModule);

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

    	SceModule rebootModule = createRebootModule();
    	rebootModule.text_size = rebootFileLength;

    	MMIO mmio = (MMIO) getMMIO();
    	// The memory remap has already been done by the IPL code at this point
    	mmio.remapMemoryAtProcessorReset();

    	int rebootMemSize = rebootModule.text_size + rebootModule.data_size + rebootModule.bss_size;
    	SysMemInfo rebootMemInfo = Modules.SysMemUserForUserModule.malloc(VSHELL_PARTITION_ID, "reboot", PSP_SMEM_Addr, rebootMemSize, rebootModule.text_addr);
    	if (rebootMemInfo == null) {
    		return false;
    	}

    	Memory mem = Memory.getInstance();
    	TPointer rebootBinAddr = new TPointer(mem, rebootBaseAddress);
    	int readLength = vFile.ioRead(rebootBinAddr, rebootFileLength);
    	vFile.ioClose();
    	if (readLength != rebootFileLength) {
    		return false;
    	}

    	initRebootParameters(mem, rebootModule);

    	return true;
    }

    private void addMMIORangeIPL() {
		// The IPL will be loaded at 0x040EC000 from the Nand
		addMMIORange(0x040EC000, 0x1E000);
		// The 2nd part of the IPL will be uncompressed at 0x04000000
		addMMIORange(0x04000000, 0xE0000);

		// For multiloader_ipl.bin
		addMMIORange(0x041E0000, 0x4000);
		addMMIORange(0x040E0000, 0x5000);

		// For Pro-CFW CIPL
		addMMIORange(0xBFD00000, 0x1000);
    }

    /**
     * Boot using the IPL code is not working as it is computing a hash
     * on the IPL code located at 0xBFC00000 and using this hash value
     * to decrypt parts of the code loaded from the NAND.
     * 
     * @return true if can be booted
     */
    private boolean bootIpl() {
    	if (loadKbooti()) {
    		usingKbooti = true;
    	} else {
			// The Pre-IPL is testing the GPIO port 4 to decide if needs to boot
			// from the Nand (normal battery) or from the MemoryStick (service/Pandora battery)
	    	boolean bootFromNand = !MMIOHandlerGpio.getInstance().readPort(GPIO_PORT_SERVICE_BATTERY);
	    	if (log.isInfoEnabled()) {
	    		log.info(String.format("Loading IPL from %s", bootFromNand ? "Nand" : "MemoryStick"));
	    	}

	    	if (!loadIpl(bootFromNand)) {
	    		return false;
	    	}
    	}

		addMMIORange(preIplAddress, preIplSize);
    	addMMIORange(preIplCopy, 0x1000);
    	addMMIORangeIPL();
    	if (Model.getModel() <= MODEL_PSP_SLIM) {
	    	HLEUtilities.getInstance().installHLESyscallWithJump(new TPointer(preIplCopy, 0x000), this, "hlePreIplStart");
	    	HLEUtilities.getInstance().installHLESyscall(new TPointer(preIplCopy, 0x2A0), this, "hlePreIplIcacheInvalidateAll");
	    	HLEUtilities.getInstance().installHLESyscall(new TPointer(preIplCopy, 0x2D8), this, "hlePreIplDcacheWritebackInvalidateAll");
	    	HLEUtilities.getInstance().installHLESyscall(new TPointer(preIplCopy, 0x334), this, "hlePreIplNandReadPage");
	    	HLEUtilities.getInstance().installHLESyscall(new TPointer(preIplCopy, 0x418), this, "hlePreIplMemoryStickReadSector");
    	}

		SceModule iplModule = new SceModule(true);
		iplModule.modname = getName();
		iplModule.baseAddress = iplBase;
		iplModule.text_addr = iplBase;
		iplModule.text_size = iplSize;
		iplModule.data_size = 0;
		iplModule.bss_size = 0;

    	Modules.ThreadManForUserModule.Initialise(iplModule, iplEntry, 0, iplModule.pspfilename, -1, 0, false);

		SceModule rebootModule = new SceModule(true);
    	rebootModule.baseAddress = rebootBaseAddress;
    	rebootModule.text_addr = rebootBaseAddress;
    	addFunctionNames(rebootModule);

    	return true;
	}

    /**
     * Boot using the PRE-IPL code. This requires a dump of the PRE-IPL
     * from a real PSP.
     * 
     * @return true if can be booted
     */
    private boolean bootPreIpl() {
    	TPointer preIpl = new TPointer(getMMIO(), preIplAddress);

    	File preIplFile = new File(getPreIplFileName());
		try {
			IVirtualFile vFile = new LocalVirtualFile(new SeekableRandomFile(preIplFile, "r"));
			int result = vFile.ioRead(preIpl, preIplSize);
			vFile.ioClose();

			if (result != preIplSize) {
				return false;
			}
		} catch (FileNotFoundException e) {
			return false;
		}

		addMMIORange(preIpl, preIplSize);
		addMMIORange(preIplCopy, preIplSize);
		addMMIORangeIPL();

		SceModule iplModule = new SceModule(true);
		iplModule.modname = getName();
		iplModule.baseAddress = preIplAddress;
		iplModule.text_addr = preIplAddress;
		iplModule.text_size = preIplSize;
		iplModule.data_size = 0;
		iplModule.bss_size = 0;

    	Modules.ThreadManForUserModule.Initialise(iplModule, preIplAddress, 0, iplModule.pspfilename, -1, 0, false);

		SceModule rebootModule = new SceModule(true);
    	rebootModule.baseAddress = rebootBaseAddress;
    	rebootModule.text_addr = rebootBaseAddress;
    	addFunctionNames(rebootModule);

    	switch (getFirmwareVersion()) {
    		case 639:
	        	addFunctionNid(0x04008A88, "sceIdStorageReadLeaf");
	        	addFunctionNid(0x04009310, "sceIdStorageLookup");
	        	addFunctionNid(0x04008620, "sceIdStorageInit");
	        	addFunctionNid(0x0400C450, "sceSysregGetFuseId");
	        	addFunctionNid(0x0400067C, "sceKernelUtilsSha1Digest");
	        	addFunctionNid(0x0400C410, "sceSysregGetTachyonVersion");
	        	addFunctionNid(0x04003A20, "sceKernelUtilsSha1Digest");
    			break;
	    	case 660:
	    	case 661:
	        	addFunctionNid(0x04008C24, "sceIdStorageReadLeaf");
	        	addFunctionNid(0x040094AC, "sceIdStorageLookup");
	        	addFunctionNid(0x040087BC, "sceIdStorageInit");
	        	addFunctionNid(0x0400C5F4, "sceSysregGetFuseId");
	        	addFunctionNid(0x0400067C, "sceKernelUtilsSha1Digest");
	        	addFunctionNid(0x0400C5B4, "sceSysregGetTachyonVersion");
	        	addFunctionNid(0x04003AF0, "sceKernelUtilsSha1Digest");
	        	break;
    	}

    	return true;
	}

    private void addFunctionNid(int address, String name) {
    	int nid = HLEModuleManager.getInstance().getNIDFromFunctionName(name);
    	if (nid != 0) {
    		LoadCoreForKernelModule.addFunctionNid(address, nid);
    	}
    }

    private void addFunctionNid(int moduleAddress, SceModule module, String name) {
    	addFunctionNid(module.text_addr + moduleAddress, name);
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
    	if (isInterruptContext && getFirmwareVersion() > 200) {
    		RuntimeContext.setLog4jMDC("Interrupt");
    	} else {
        	Memory mem = Memory.getInstance();
        	int threadManInfo = LoadCoreForKernelModule.getThreadManInfo();
        	if (threadManInfo != 0) {
		    	int currentThread = mem.internalRead32(threadManInfo + LoadCoreForKernelModule.threadManInfoCurrentThreadOffset);
		    	if (Memory.isAddressGood(currentThread)) {
					int uid = mem.internalRead32(currentThread + 8);
					int cb = SysMemForKernel.getCBFromUid(uid);
					int nameAddr = mem.internalRead32(cb + 16);
					String name = Utilities.readInternalStringZ(mem, nameAddr);
	
					RuntimeContext.setLog4jMDC(name, uid);
		    	} else {
					RuntimeContext.setLog4jMDC("root");
		    	}
	    	} else {
				RuntimeContext.setLog4jMDC("root");
        	}
    	}
    }

    public static void resetThreadManInfo(Processor processor) {
    	if (!enableReboot) {
    		return;
    	}

    	if (processor.cp0.isMediaEngineCpu()) {
    		return;
    	}

    	Memory mem = Memory.getInstance();
    	int threadManInfo = LoadCoreForKernelModule.getThreadManInfo();
    	if (threadManInfo != 0) {
    		mem.write32(threadManInfo + 0, 0);
    	}

    	setLog4jMDC(processor);
    }

    public static void dumpAllThreads() {
    	if (!enableReboot || !log.isTraceEnabled()) {
    		return;
    	}

    	Memory mem = Memory.getInstance();
    	int threadManInfo = LoadCoreForKernelModule.getThreadManInfo();
    	if (threadManInfo == 0) {
    		return;
    	}
    	int currentThread = mem.read32(threadManInfo + LoadCoreForKernelModule.threadManInfoCurrentThreadOffset);
    	int nextThread = mem.read32(threadManInfo + LoadCoreForKernelModule.threadManInfoNextThreadOffset);

    	if (LoadCoreForKernelModule.threadManInfoThreadTypeOffset != -1) {
    		dumpThreadTypeList(mem, mem.read32(threadManInfo + LoadCoreForKernelModule.threadManInfoThreadTypeOffset));
    	}
    	dumpThread(mem, currentThread, "Current thread");
    	if (nextThread != 0 && nextThread != currentThread) {
    		dumpThread(mem, nextThread, "Next thread");
    	}
    	dumpThreadList(mem, threadManInfo + LoadCoreForKernelModule.threadManInfoSleepingThreadsOffset, "Sleeping thread");
    	dumpThreadList(mem, threadManInfo + LoadCoreForKernelModule.threadManInfoDelayedThreadsOffset, "Delayed thread");
    	dumpThreadList(mem, threadManInfo + LoadCoreForKernelModule.threadManInfoStoppedThreadsOffset, "Stopped thread");
    	dumpThreadList(mem, threadManInfo + LoadCoreForKernelModule.threadManInfoSuspendedThreadsOffset, "Suspended thread");
    	dumpThreadList(mem, threadManInfo + LoadCoreForKernelModule.threadManInfoDeadThreadsOffset, "Dead thread");
    	if (LoadCoreForKernelModule.threadManInfoUnknownThreadsOffset != -1) {
    		dumpThreadList(mem, threadManInfo + LoadCoreForKernelModule.threadManInfoUnknownThreadsOffset, "??? thread");
    	}
    	for (int priority = 0; priority < 128; priority++) {
    		dumpThreadList(mem, threadManInfo + LoadCoreForKernelModule.threadManInfoReadyThreadsOffset + priority * 8, String.format("Ready thread[prio=0x%X]", priority));
    	}
    }

    private static void dumpThreadTypeList(Memory mem, int list) {
    	if (list == 0) {
    		return;
    	}

    	for (int cb = mem.read32(list); cb != list; cb = mem.read32(cb)) {
    		SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    		sceSysmemUidCB.read(mem, cb);
    		dumpThread(mem, cb + sceSysmemUidCB.size * 4, "Thread");
    	}
    }

    private static void dumpThread(Memory mem, int address, String comment) {
    	if (address == 0) {
    		return;
    	}

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
    	if (list == 0) {
    		return;
    	}

    	for (int address = mem.read32(list); address != 0 && address != list; address = mem.read32(address)) {
    		dumpThread(mem, address, comment);
    	}
    }

    private void addMMIORange(int startAddress, int length) {
    	Compiler.getInstance().addMMIORange(startAddress, length);
    }

    private void addMMIORange(TPointer startAddress, int length) {
    	addMMIORange(startAddress.getAddress(), length);
    }

    private boolean loadKbooti() {
    	File kbootiFile = new File("kbooti.bin");
    	if (!kbootiFile.canRead() || kbootiFile.length() <= 0x1000) {
    		return false;
    	}

    	byte[] kbooti = new byte[(int) kbootiFile.length()];
    	try {
        	InputStream is = new FileInputStream(kbootiFile);
			is.read(kbooti);
	    	is.close();
		} catch (IOException e) {
			log.error("loadKbooti", e);
			return false;
		}

    	if (log.isInfoEnabled()) {
    		log.info(String.format("Loading IPL from %s", kbootiFile));
    	}

    	int offset = 0;
    	if (readUnaligned32(kbooti, offset + 0x60) != KIRK_MODE_CMD1) {
    		offset += 0x1000;
    	}

    	Memory mem = getMMIO();
		final TPointer decryptBuffer = new TPointer(mem, 0xBFD00000);
    	final int decryptBufferPages = 8;
    	final int decryptBufferSize = decryptBufferPages * Nand.pageSize;
		int nextAddr = 0;
		iplBase = 0;
		iplEntry = 0;
		for (int i = 0; i < Nand.pageSize && iplEntry == 0; i += 2) {
			for (int j = 0; j < Nand.pagesPerBlock && iplEntry == 0; j += decryptBufferPages) {
				decryptBuffer.setArray(0, kbooti, offset, decryptBufferSize);
				offset += decryptBufferSize;

				int dataSize = decryptBuffer.getValue32(112);
				int dataOffset = decryptBuffer.getValue32(116);
				int inSize = KirkEngine.KIRK_CMD1_HEADER.SIZEOF + Utilities.alignUp(dataSize, 15) + dataOffset;
				int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(decryptBuffer, decryptBufferSize, decryptBuffer, inSize, KIRK_CMD_DECRYPT_PRIVATE);
				if (result != 0) {
					log.error(String.format("hleUtilsBufferCopyWithRange returned 0x%08X", result));
				}

				int addr = decryptBuffer.getValue32(0);
				int length = decryptBuffer.getValue32(4);
				iplEntry = decryptBuffer.getValue32(8);
				int checksum = decryptBuffer.getValue32(12);
				if (log.isTraceEnabled()) {
					log.trace(String.format("IPL block 0x%X: addr=0x%08X, length=0x%X, entry=0x%08X, checkSum=0x%08X", i, addr, length, iplEntry, checksum));
				}

				if (iplBase == 0) {
					iplBase = addr;
				} else if (addr != nextAddr) {
					log.error(String.format("Error at IPL offset 0x%X: 0x%08X != 0x%08X", i, addr, nextAddr));
				}
				if (length > 0) {
					mem.memcpy(addr, decryptBuffer.getAddress() + 0x10, length);
				}
				nextAddr = addr + length;
			}
		}
		iplSize = nextAddr - iplBase;

		if (iplBase == 0) {
			iplBase = decryptBuffer.getAddress();
			iplSize = decryptBufferSize;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("IPL size=0x%X: %s", iplSize, Utilities.getMemoryDump(mem, iplBase, Math.min(iplSize, 1024))));
		}

		patchIpl(mem);

    	addMMIORange(iplBase, iplSize);

		return true;
    }

    private int iplKirkCommand(TPointer outAddr, int outSize, TPointer inAddr, int inSize, int cmd) {
    	if (MMIOHandlerKirk.getInstance().preDecrypt(outAddr, outSize, inAddr, inSize, cmd)) {
    		return 0;
    	}

    	return Modules.semaphoreModule.hleUtilsBufferCopyWithRange(outAddr, outSize, inAddr, inSize, cmd);
    }

    private boolean loadIpl(boolean loadFromNand) {
    	int result;
    	Memory mem = getMMIO();

    	final boolean isPsp3000 = Model.getModel() >= MODEL_PSP_BRITE;
    	final TPointer nandSpareBuffer = new TPointer(preIplCopy, 0x810);
    	final TPointer iplFatSectorBuffer = new TPointer(preIplCopy, 0x81C);

    	if (loadFromNand) {
	    	for (int iplFatPpn = sceNand.iplTablePpnStart; true; iplFatPpn += Nand.pagesPerBlock) {
	    		// Pre-IPL patched?
	        	if (preIplCopy.getValue32(0x154) == MOVE(_v0, _zr)) { 
	        		result = 0;
	        	} else {
	        		result = Modules.sceNandModule.hleNandReadPages(iplFatPpn, iplFatSectorBuffer, nandSpareBuffer, 1, true, false, true);
	        	}

	    		if (result != 0) {
	    			return false;
	    		}
	    		if (nandSpareBuffer.getValue32(4) == sceNand.iplId) {
	    			break;
	    		}
	    	}
    	}

		final TPointer decryptBuffer = new TPointer(mem, 0xBFD00000);
		final TPointer hashDecryptBuffer = new TPointer(decryptBuffer, 0xFCC);
		final TPointer hashBuffer = new TPointer(decryptBuffer, 0x004);
		final TPointer checkHashBuffer = new TPointer(decryptBuffer, 0xF80);
		final TPointer ecdsaSignatureBuffer = new TPointer(decryptBuffer, 0xFA0);
		final TPointer ecdsaMessageHash = new ByteArrayMemory(new byte[20]).getPointer();
		final TPointer ecdsaSignature = new ByteArrayMemory(new byte[40]).getPointer();
		final int hashSize = 20;
    	final int decryptBufferPages = 8;
    	final int decryptBufferSize = decryptBufferPages * Nand.pageSize;
		int nextAddr = 0;
		iplBase = 0;
		iplEntry = 0;
		int previousChecksum = 0;
		boolean ecdsaHash = false;
		for (int i = 0; i < Nand.pageSize && iplEntry == 0; i += 2) {
			for (int j = 0; j < Nand.pagesPerBlock && iplEntry == 0; j += decryptBufferPages) {
				for (int page = 0; page < decryptBufferPages; page++) {
					if (loadFromNand) {
						int ppn = iplFatSectorBuffer.getUnsignedValue16(i) * Nand.pagesPerBlock;
						if (ppn == 0) {
							break;
						}

						result = Modules.sceNandModule.hleNandReadPages(ppn + j + page, new TPointer(decryptBuffer, page * Nand.pageSize), nandSpareBuffer, 1, true, false, true);
						if (result != 0) {
							return false;
						}
						if (nandSpareBuffer.getValue32(4) != sceNand.iplId) {
							return false;
						}
					} else {
						MMIOHandlerMemoryStick.getInstance().readSector(j + page + 16, new TPointer(decryptBuffer, page * Nand.pageSize));
					}
				}

				int dataSize = decryptBuffer.getValue32(112);
				int dataOffset = decryptBuffer.getValue32(116);
				int inSize = KirkEngine.KIRK_CMD1_HEADER.SIZEOF + Utilities.alignUp(dataSize, 15) + dataOffset;
				int cmd = KIRK_CMD_DECRYPT_PRIVATE;
				if (isPsp3000) {
					cmd = decryptBuffer.getValue32(96) & 0x3;
					decryptBuffer.setUnsignedValue16(98, 0);
					ecdsaHash = hasBit(decryptBuffer.getValue32(100), 0);
				}
				result = iplKirkCommand(decryptBuffer, decryptBufferSize, decryptBuffer, inSize, cmd);
				if (result != 0) {
					log.error(String.format("hleUtilsBufferCopyWithRange cmd=0x%X returned 0x%08X", cmd, result));
					return false;
				}

				if (isPsp3000) {
					hashDecryptBuffer.setValue32(0, KirkEngine.KIRK_MODE_DECRYPT_CBC);
					hashDecryptBuffer.setValue32(4, 0);
					hashDecryptBuffer.setValue32(8, 0);
					hashDecryptBuffer.setValue32(12, 0x6C); // seed
					hashDecryptBuffer.setValue32(16, hashSize);
					result = iplKirkCommand(hashDecryptBuffer, hashSize, hashDecryptBuffer, alignUp(hashSize, 15) + KIRK_AES128CBC_HEADER.SIZEOF, KIRK_CMD_DECRYPT_IV_0);
					if (result != 0) {
						log.error(String.format("hleUtilsBufferCopyWithRange KIRK_CMD_DECRYPT_IV_0 returned 0x%08X", result));
						return false;
					}

					int addr = decryptBuffer.getValue32(0);
					int length = decryptBuffer.getValue32(4);
					TPointer ptr = new TPointer(decryptBuffer, length);
					ptr.setValue32(16, addr);
					ptr.setValue32(20, length);
					hashBuffer.setValue32(0, length + 16);
					result = iplKirkCommand(checkHashBuffer, hashSize, hashBuffer, length + 20, KIRK_CMD_SHA1_HASH);
					if (result != 0) {
						log.error(String.format("hleUtilsBufferCopyWithRange KIRK_CMD_SHA1_HASH returned 0x%08X", result));
						return false;
					}

					decryptBuffer.setValue32(4, length);

					for (int k = 0; k < hashSize; k += 4) {
						if (checkHashBuffer.getValue32(k) != hashDecryptBuffer.getValue32(k)) {
							log.error(String.format("IPL invalid hash at offset 0x%X: 0x%08X != 0x%08X", k, checkHashBuffer.getValue32(k), hashDecryptBuffer.getValue32(k)));
							return false;
						}
						ecdsaMessageHash.setValue32(k, ecdsaMessageHash.getValue32(k) ^ checkHashBuffer.getValue32(k));
					}

					ecdsaSignature.memcpy(ecdsaSignatureBuffer, 40);

					for (int k = dataSize; k < decryptBufferSize; k += 4) {
						decryptBuffer.setValue32(k, 13);
					}
				}

				int addr = decryptBuffer.getValue32(0);
				int length = decryptBuffer.getValue32(4);
				iplEntry = decryptBuffer.getValue32(8);
				int checksum = decryptBuffer.getValue32(12);
				if (log.isTraceEnabled()) {
					log.trace(String.format("IPL block 0x%X: addr=0x%08X, length=0x%X, entry=0x%08X, checkSum=0x%08X", i, addr, length, iplEntry, checksum));
				}

				if (previousChecksum != checksum) {
					log.error(String.format("IPL checksum=0x%08X not matching previousChecksum=0x%08X", checksum, previousChecksum));
					return false;
				}

				if (iplBase == 0) {
					iplBase = addr;
				} else if (addr != nextAddr) {
					log.error(String.format("Error at IPL offset 0x%X: 0x%08X != 0x%08X", i, addr, nextAddr));
				}

				if (length > 0 && addr != 0) {
					previousChecksum = 0;
					for (int k = 0; k < length; k += 4) {
						int value = decryptBuffer.getValue32(0x10 + k);
						mem.write32(addr + k, value);
						previousChecksum += value;
					}
				}
				nextAddr = addr + length;
			}
		}

		if (isPsp3000) {
			if (!ecdsaHash) {
				// ECDSA hash is mandatory for the entry IPL block
				return false;
			}

			final byte[] ecdsaPublicKey = new byte[] {
				(byte) 0xBC, (byte) 0x66, (byte) 0x06, (byte) 0x11,
				(byte) 0xA7, (byte) 0x0B, (byte) 0xD7, (byte) 0xF2,
				(byte) 0xD1, (byte) 0x40, (byte) 0xA4, (byte) 0x82,
				(byte) 0x15, (byte) 0xC0, (byte) 0x96, (byte) 0xD1,
				(byte) 0x1D, (byte) 0x2D, (byte) 0x41, (byte) 0x12,
				(byte) 0xF0, (byte) 0xE9, (byte) 0x37, (byte) 0x9A,
				(byte) 0xC4, (byte) 0xE0, (byte) 0xD3, (byte) 0x87,
				(byte) 0xC5, (byte) 0x42, (byte) 0xD0, (byte) 0x91,
				(byte) 0x34, (byte) 0x9D, (byte) 0xD1, (byte) 0x51,
				(byte) 0x69, (byte) 0xDD, (byte) 0x5A, (byte) 0x87
			};
			decryptBuffer.setArray(0, ecdsaPublicKey);
			decryptBuffer.memcpy(40, ecdsaMessageHash, 20);
			ecdsaMessageHash.clear(20);
			decryptBuffer.memcpy(60, ecdsaSignature, 40);
			ecdsaSignatureBuffer.clear(40);
			result = iplKirkCommand(TPointer.NULL, 0, decryptBuffer, 0x64, KIRK_CMD_ECDSA_VERIFY);
			if (result != 0) {
				log.error(String.format("hleUtilsBufferCopyWithRange KIRK_CMD_ECDSA_VERIFY returned 0x%08X", result));
				return false;
			}
			decryptBuffer.clear(100);

			mem.write32(preIplAddress + 0xFFC, 0x20070910);
		}

		iplSize = nextAddr - iplBase;

		if (iplBase == 0) {
			iplBase = decryptBuffer.getAddress();
			iplSize = decryptBufferSize;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("IPL size=0x%X: %s", iplSize, Utilities.getMemoryDump(mem, iplBase, iplSize)));
		}

		patchIpl(mem);

    	addMMIORange(iplBase, iplSize);

		return true;
    }

    private void patchIplKey(TPointer keyAddr, int[] keyWithoutPreIpl, int[] keyWithPreIpl) {
		int checksum = 0;
		for (int i = 0; i < keyWithoutPreIpl.length; i++) {
			keyAddr.setUnsignedValue8(i, keyWithoutPreIpl[i] ^ keyWithPreIpl[i] ^ keyAddr.getUnsignedValue8(i));
			checksum += keyWithoutPreIpl[i];
		}
		keyAddr.setUnsignedValue8(0x20, checksum);
    }

    private void patchIpl(Memory mem) {
    	TPointer keyAddr = null;
    	int iplKeyValue = 0;

    	if (Memory.isAddressGood(iplBase)) {
    		keyAddr = new TPointer(mem, iplBase + 0x2880);
    		iplKeyValue = keyAddr.getValue32(0);
    	}

    	int[] keyWithoutPreIpl;
    	int[] keyWithPreIpl;
    	switch (iplKeyValue) {
    		case IPL_KEY_MODEL_GENERATION_1:
	    		keyWithoutPreIpl = new int[] {
	    				0x18, 0x2A, 0x72, 0xA2, 0x55, 0x43, 0x56, 0xD8,
	    				0x0A, 0x5F, 0x87, 0x57, 0x77, 0xB9, 0x8F, 0x93,
	    				0xA2, 0xA8, 0xC4, 0xFB, 0x48, 0x0F, 0xD1, 0x25,
	    				0x61, 0x96, 0x4A, 0xDF, 0x00, 0x00, 0x00, 0x00
	    		};
	    		keyWithPreIpl = new int[] {
	    				0x10, 0x7C, 0xD5, 0x09, 0x6A, 0xB5, 0x89, 0x76,
	    				0x49, 0xF6, 0xEA, 0x54, 0x0B, 0x30, 0x3E, 0x2B,
	    				0x47, 0x17, 0x6D, 0x0B, 0x04, 0x70, 0x3E, 0xB0,
	    				0x3C, 0x01, 0x44, 0x89, 0x00, 0x00, 0x00, 0x00
	    		};
	    		patchIplKey(keyAddr, keyWithoutPreIpl, keyWithPreIpl);
	    		break;
    		case IPL_KEY_MODEL_GENERATION_2:
	    		keyWithoutPreIpl = new int[] {
	    				0x83, 0x03, 0x5C, 0x90, 0xAA, 0x59, 0x03, 0x86,
	    				0xE9, 0x7C, 0x99, 0x94, 0xA1, 0xC0, 0x82, 0x78,
	    				0xA8, 0x8A, 0x7E, 0x71, 0x92, 0x5A, 0x03, 0x2B,
	    				0xE8, 0xEE, 0xBE, 0x3B, 0x00, 0x00, 0x00, 0x00
	    		};
	    		keyWithPreIpl = new int[] {
	    				0x87, 0x9D, 0x0C, 0x16, 0x42, 0xC1, 0x99, 0xFD,
	    				0x77, 0xC2, 0x43, 0x2F, 0x45, 0xBD, 0x60, 0x25,
	    				0xB4, 0xA7, 0x5A, 0x42, 0x17, 0x51, 0xF1, 0x15,
	    				0xDA, 0x1F, 0xDA, 0x02, 0x00, 0x00, 0x00, 0x00
	    		};
	    		patchIplKey(keyAddr, keyWithoutPreIpl, keyWithPreIpl);
	    		break;
    		default:
    			int hashDigestPatchAddr = iplBase + 0x1224;
    			if (mem.read32(hashDigestPatchAddr) == JAL(iplBase + 0xE04)) {
    				mem.write32(iplBase + 0x1224, SYSCALL(this, "hleIplHashDigestPatch"));
    			} else {
    				log.error(String.format("patchIpl unknown IPL code at 0x%08X for PSP Model %s", iplBase, Model.getModelName()));
    			}
	        	break;
    	}
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hlePreIplStart() {
    	loadIpl(true);

    	int continueAddress = iplEntry;

		// Pre-IPL patched?
    	int opcode = preIplCopy.getValue32(0xFC);
    	if ((opcode & 0xFFFF0000) == LUI(_t9, 0)) {
    		continueAddress = opcode << 16;

        	addMMIORange(continueAddress, 0x3000);
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hlePreIplStart continueAddress=0x%08X", continueAddress));
    	}

    	return continueAddress;
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hlePreIplNandReadPage(int ppn, TPointer user, TPointer spare) {
    	return Modules.sceNandModule.hleNandReadPages(ppn, user, spare, 1, true, false, true);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hlePreIplIcacheInvalidateAll() {
    	// Nothing to do
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hlePreIplDcacheWritebackInvalidateAll() {
    	// Nothing to do
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hlePreIplMemoryStickReadSector(int lba, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=MMIOHandlerMemoryStick.PAGE_SIZE, usage=Usage.out) TPointer address) {
		MMIOHandlerMemoryStick.getInstance().readSector(lba, address);

    	addMMIORange(address, MMIOHandlerMemoryStick.PAGE_SIZE);

    	return 0;
    }

    private byte[] getHashDigest() {
    	switch (Model.getGeneration()) {
	    	case 3:
	        	return new byte[] {
	        			(byte) 0x41, (byte) 0x38, (byte) 0x9D, (byte) 0xB4, (byte) 0x0B, (byte) 0x91, (byte) 0x24, (byte) 0x98,
	        			(byte) 0x37, (byte) 0xFF, (byte) 0x3D, (byte) 0x49, (byte) 0x13, (byte) 0x5C, (byte) 0x5A, (byte) 0x58,
	        			(byte) 0xAE, (byte) 0xD7, (byte) 0x37, (byte) 0x16, (byte) 0x57, (byte) 0x1E, (byte) 0x10, (byte) 0x4F,
	        			(byte) 0xF3, (byte) 0x20, (byte) 0xFE, (byte) 0xA5
	        	};
	    	case 4:
	        	return new byte[] {
	        			(byte) 0xB6, (byte) 0x89, (byte) 0x96, (byte) 0x09, (byte) 0x68, (byte) 0xF2, (byte) 0x41, (byte) 0xA5,
	        			(byte) 0xB3, (byte) 0x7E, (byte) 0x27, (byte) 0x12, (byte) 0x55, (byte) 0xA6, (byte) 0xBB, (byte) 0x1C,
	        			(byte) 0xEB, (byte) 0x51, (byte) 0xEE, (byte) 0x0B, (byte) 0x24, (byte) 0x81, (byte) 0x9E, (byte) 0xAC,
	        			(byte) 0xEC, (byte) 0x68, (byte) 0x3B, (byte) 0x50
	        	};
	    	case 7:
	        	return new byte[] {
	        			(byte) 0x7F, (byte) 0x40, (byte) 0x66, (byte) 0x89, (byte) 0xD7, (byte) 0x53, (byte) 0x7E, (byte) 0x65,
	        			(byte) 0x74, (byte) 0xF2, (byte) 0x2B, (byte) 0x5B, (byte) 0x24, (byte) 0x8F, (byte) 0xE7, (byte) 0xCF,
	        			(byte) 0xFE, (byte) 0xA8, (byte) 0xEF, (byte) 0x38, (byte) 0x5E, (byte) 0xC0, (byte) 0x4D, (byte) 0x0D,
	        			(byte) 0xCB, (byte) 0xAD, (byte) 0xDB, (byte) 0x39
	        	};
	    	case 9:
	        	return new byte[] {
	        			(byte) 0xD6, (byte) 0x18, (byte) 0x12, (byte) 0x0E, (byte) 0x3E, (byte) 0x2B, (byte) 0xE9, (byte) 0xDF,
	        			(byte) 0x1E, (byte) 0xC0, (byte) 0x8D, (byte) 0xD4, (byte) 0x33, (byte) 0xE5, (byte) 0x85, (byte) 0x84,
	        			(byte) 0x4A, (byte) 0xC6, (byte) 0xB7, (byte) 0xEF, (byte) 0x31, (byte) 0xFF, (byte) 0x16, (byte) 0xE7,
	        			(byte) 0x03, (byte) 0xF9, (byte) 0x7E, (byte) 0x3F
	        	};
	    	case 11:
	        	return new byte[] {
	        			(byte) 0xC9, (byte) 0xF8, (byte) 0xFD, (byte) 0xAB, (byte) 0x97, (byte) 0xE8, (byte) 0x14, (byte) 0x44,
	        			(byte) 0xAD, (byte) 0x5F, (byte) 0x93, (byte) 0x4C, (byte) 0x1E, (byte) 0x61, (byte) 0x09, (byte) 0x49,
	        			(byte) 0x43, (byte) 0x72, (byte) 0xB1, (byte) 0x56, (byte) 0x91, (byte) 0x60, (byte) 0x3B, (byte) 0x67,
	        			(byte) 0xA1, (byte) 0x2D, (byte) 0x9A, (byte) 0x44
	        	};
        	default:
        		log.error(String.format("getHashDigest unimplemented for PSP model %s", Model.getModelName()));
        		break;
    	}

    	return null;
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleIplHashDigestPatch(CpuState cpu) {
    	final byte[] hashDigest = getHashDigest();
    	int hashDigestAddr = cpu._t0;

    	if (log.isDebugEnabled()) {
			log.debug(String.format("hleIplHashDigestPatch writing hash digest to 0x%08X", hashDigestAddr));
		}

		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(getMemory(), hashDigestAddr, hashDigest.length, 1);
		for (int i = 0; i < hashDigest.length; i++) {
			memoryWriter.writeNext(hashDigest[i]);
		}
		memoryWriter.flush();
    }
}
