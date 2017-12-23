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

import static jpcsp.Allegrex.Common._a0;
import static jpcsp.Allegrex.Common._a1;
import static jpcsp.Allegrex.Common._a2;
import static jpcsp.Allegrex.Common._s1;
import static jpcsp.Allegrex.Common._t3;
import static jpcsp.Allegrex.Common._t4;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._v1;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.HLE.HLEModuleManager.InternalSyscallNid;
import static jpcsp.HLE.Modules.LoadCoreForKernelModule;
import static jpcsp.HLE.modules.InitForKernel.SCE_INIT_APITYPE_KERNEL_REBOOT;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Addr;
import static jpcsp.HLE.modules.SysMemUserForUser.VSHELL_PARTITION_ID;
import static jpcsp.HLE.modules.ThreadManForUser.ADDIU;
import static jpcsp.HLE.modules.ThreadManForUser.JAL;
import static jpcsp.HLE.modules.ThreadManForUser.JR;
import static jpcsp.HLE.modules.ThreadManForUser.LW;
import static jpcsp.HLE.modules.ThreadManForUser.MOVE;
import static jpcsp.HLE.modules.ThreadManForUser.NOP;
import static jpcsp.HLE.modules.ThreadManForUser.SB;
import static jpcsp.HLE.modules.ThreadManForUser.SYSCALL;
import static jpcsp.format.PSP.PSP_HEADER_SIZE;
import static jpcsp.util.Utilities.patch;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
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
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelLoadExecVSHParam;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceLoadCoreBootInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.SceSysmemUidCB;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Model;
import jpcsp.util.Utilities;

public class reboot extends HLEModule {
    public static Logger log = Modules.getLogger("reboot");
    public static boolean enableReboot = false;
    private static final String rebootFileName = "flash0:/reboot.bin";
    private static final int rebootBaseAddress = MemoryMap.START_KERNEL + 0x600000;
    private static final int rebootParamAddress = MemoryMap.START_KERNEL + 0x400000;

    private static class SetLog4jMDC implements IAction {
		@Override
		public void execute() {
			setLog4jMDC();
		}
    }

    public boolean loadAndRun() {
    	if (!enableReboot) {
    		return false;
    	}

    	Memory mem = Memory.getInstance();

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

    	SceModule rebootModule = new SceModule(true);
    	rebootModule.modname = getName();
    	rebootModule.pspfilename = rebootFileName;
    	rebootModule.baseAddress = rebootBaseAddress;
    	rebootModule.text_addr = rebootBaseAddress;
    	rebootModule.text_size = rebootFileLength;
    	rebootModule.data_size = 0;
    	rebootModule.bss_size = 0x26B80;

    	final boolean fromSyscall = false;
    	Emulator.getInstance().initNewPsp(fromSyscall);
    	Emulator.getInstance().setModuleLoaded(true);
    	HLEModuleManager.getInstance().startModules(fromSyscall);
    	Modules.ThreadManForUserModule.Initialise(rebootModule, rebootModule.baseAddress, 0, rebootModule.pspfilename, -1, 0, fromSyscall);

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

		// Mapping of subroutines defined in
		//     https://github.com/uofw/uofw/blob/master/src/reboot/unk.c
		// and https://github.com/uofw/uofw/blob/master/src/reboot/nand.c
		//
		//	sub_EFCC -> sceNandInit2
		//	sub_F0C4 -> sceNandIsReady
		//	sub_F0D4 -> sceNandSetWriteProtect
		//	sub_F144 -> sceNandLock
		//	sub_F198 -> sceNandReset
		//	sub_F234 -> sceNandReadId
		//	sub_F28C -> sceNandReadAccess
		//	sub_F458 -> sceNandWriteAccess
		//	sub_F640 -> sceNandEraseBlock
		//	sub_F72C -> sceNandReadExtraOnly
		//	sub_F8A8 -> sceNandReadStatus
		//	sub_F8DC -> sceNandSetScramble
		//	sub_F8EC -> sceNandReadPages
		//	sub_F930 -> sceNandWritePages
		//	sub_F958 -> sceNandReadPagesRawExtra
		//	sub_F974 -> sceNandWritePagesRawExtra
		//	sub_F998 -> sceNandReadPagesRawAll
		//	sub_F9D0 -> sceNandTransferDataToNandBuf
		//	sub_FC40 -> sceNandIntrHandler
		//	sub_FF60 -> sceNandTransferDataFromNandBuf
		//	sub_103C8 -> sceNandWriteBlockWithVerify
		//	sub_1047C -> sceNandReadBlockWithRetry
		//	sub_10500 -> sceNandVerifyBlockWithRetry
		//	sub_10650 -> sceNandEraseBlockWithRetry
		//	sub_106C4 -> sceNandIsBadBlock
		//	sub_10750 -> sceNandDoMarkAsBadBlock
		//	sub_109DC -> sceNandDetectChipMakersBBM
		//	sub_10D1C -> sceNandGetPageSize
		//	sub_10D28 -> sceNandGetPagesPerBlock
		//	sub_10D34 -> sceNandGetTotalBlocks
		//	sub_10DA0 -> sceNandCalcEcc
		//	sub_88610FE8 -> sceNandCorrectEcc
		//	sub_11004 -> sceNandCorrectEcc2
    	//  sub_11860 -> sceSysregEmcsmBusClockEnable
    	//  sub_11994 -> sceSysregEmcsmIoEnable
		//	unkVar90A4 -> g_scramble

    	markMMIO();

    	addFunctionNames(rebootModule);

		patch(mem, rebootModule, 0x000060A8, 0x11A0001F, NOP()); // Allow non-encrypted sysmem.prx and loadcore.prx: NOP the test at https://github.com/uofw/uofw/blob/master/src/reboot/elf.c#L680
		patch(mem, rebootModule, 0x00002734, 0x012C182B, ADDIU(_t4, _t4, 1)); // Fix KL4E decompression of uncompressed data: https://github.com/uofw/uofw/blob/master/src/reboot/main.c#L40
		patch(mem, rebootModule, 0x00002738, 0x1060FFFA, 0x012C182B);         // Fix KL4E decompression of uncompressed data: https://github.com/uofw/uofw/blob/master/src/reboot/main.c#L40
		patch(mem, rebootModule, 0x0000273C, 0x00000000, 0x1060FFF9);         // Fix KL4E decompression of uncompressed data: https://github.com/uofw/uofw/blob/master/src/reboot/main.c#L40
		patch(mem, rebootModule, 0x0000274C, 0xA1630000, SB(_v1, _t3, -1));   // Fix KL4E decompression of uncompressed data: https://github.com/uofw/uofw/blob/master/src/reboot/main.c#L48

		// The function at offset 0x00006130 is decrypting the modules.
		// See https://github.com/uofw/uofw/blob/master/src/reboot/elf.c#L707
		patch(mem, rebootModule, 0x00006174, 0x9223007C, MOVE(_a0, _s1));
		patch(mem, rebootModule, 0x00006178, 0x1060000D, ADDIU(_a1, _a0, PSP_HEADER_SIZE));
		patch(mem, rebootModule, 0x0000617C, 0x3C078002, LW(_a2, _a0, 44));
		patch(mem, rebootModule, 0x00006180, 0x24050002, JAL(rebootModule.baseAddress + 0x00003C4C)); // jal memcpy
		patch(mem, rebootModule, 0x00006184, 0x1065000A, ADDIU(_a2, _a2, -PSP_HEADER_SIZE));
		patch(mem, rebootModule, 0x00006188, 0x34E40148, MOVE(_a0, _zr));

		// The function at offset 0x000052F4 is checking the validity of the module hash from pspbtcnf.bin.
		patch(mem, rebootModule, 0x000052F4, 0x27BDFE50, JR());
		patch(mem, rebootModule, 0x000052F8, 0xAFB101A4, MOVE(_v0, _zr));

		patchSyscall(0x0000574C, this           , "hleDecryptBtcnf"               , mem, rebootModule, 0x27BDFFE0, 0xAFB20018);

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

    	// This will set the Log4j MDC values for the root thread
    	Emulator.getScheduler().addAction(new SetLog4jMDC());

    	if (log.isDebugEnabled()) {
			log.debug(String.format("sceReboot arg0=%s, arg1=%s", sceLoadCoreBootInfoAddr, sceKernelLoadExecVSHParamAddr));
		}

    	return true;
    }

    private void markMMIO() {
    	Compiler compiler = Compiler.getInstance();
    	compiler.addMMIORange(MemoryMap.START_KERNEL, 0x800000);
    	compiler.addMMIORange(0xBFC00C00, 0x240);
    }

    private static void patchSyscall(int offset, HLEModule hleModule, String functionName, Memory mem, SceModule rebootModule, int oldCode1, int oldCode2) {
		patch(mem, rebootModule, offset + 0, oldCode1, JR());
		patch(mem, rebootModule, offset + 4, oldCode2, SYSCALL(hleModule, functionName));
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

    	addFunctionNid(0x0000EFCC, rebootModule, "sceNandInit2");
    	addFunctionNid(0x0000F0C4, rebootModule, "sceNandIsReady");
    	addFunctionNid(0x0000F0D4, rebootModule, "sceNandSetWriteProtect");
    	addFunctionNid(0x0000F144, rebootModule, "sceNandLock");
    	addFunctionNid(0x0000F198, rebootModule, "sceNandReset");
    	addFunctionNid(0x0000F234, rebootModule, "sceNandReadId");
    	addFunctionNid(0x0000F28C, rebootModule, "sceNandReadAccess");
    	addFunctionNid(0x0000F458, rebootModule, "sceNandWriteAccess");
    	addFunctionNid(0x0000F640, rebootModule, "sceNandEraseBlock");
    	addFunctionNid(0x0000F72C, rebootModule, "sceNandReadExtraOnly");
    	addFunctionNid(0x0000F8A8, rebootModule, "sceNandReadStatus");
    	addFunctionNid(0x0000F8DC, rebootModule, "sceNandSetScramble");
    	addFunctionNid(0x0000F8EC, rebootModule, "sceNandReadPages");
    	addFunctionNid(0x0000F930, rebootModule, "sceNandWritePages");
    	addFunctionNid(0x0000F958, rebootModule, "sceNandReadPagesRawExtra");
    	addFunctionNid(0x0000F974, rebootModule, "sceNandWritePagesRawExtra");
    	addFunctionNid(0x0000F998, rebootModule, "sceNandReadPagesRawAll");
    	addFunctionNid(0x0000F9D0, rebootModule, "sceNandTransferDataToNandBuf");
    	addFunctionNid(0x0000FC40, rebootModule, "sceNandIntrHandler");
    	addFunctionNid(0x0000FF60, rebootModule, "sceNandTransferDataFromNandBuf");
    	addFunctionNid(0x000103C8, rebootModule, "sceNandWriteBlockWithVerify");
    	addFunctionNid(0x0001047C, rebootModule, "sceNandReadBlockWithRetry");
    	addFunctionNid(0x00010500, rebootModule, "sceNandVerifyBlockWithRetry");
    	addFunctionNid(0x00010650, rebootModule, "sceNandEraseBlockWithRetry");
    	addFunctionNid(0x000106C4, rebootModule, "sceNandIsBadBlock");
    	addFunctionNid(0x00010750, rebootModule, "sceNandDoMarkAsBadBlock");
    	addFunctionNid(0x000109DC, rebootModule, "sceNandDetectChipMakersBBM");
    	addFunctionNid(0x00010D1C, rebootModule, "sceNandGetPageSize");
    	addFunctionNid(0x00010D28, rebootModule, "sceNandGetPagesPerBlock");
    	addFunctionNid(0x00010D34, rebootModule, "sceNandGetTotalBlocks");
    }

    public static void dumpAllModulesAndLibraries() {
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

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Module '%s': text 0x%08X-0x%08X", moduleName, textAddr, textAddr + textSize));
    		}
    		// Next
    		address = mem.read32(address);
    	}
    }

    private static void dumpAllLibraries(Memory mem, int address) {
    	while (address != 0) {
    		String libName = Utilities.readStringZ(mem.read32(address + 68));
    		int numExports = mem.read32(address + 16);
    		int entryTable = mem.read32(address + 32);

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Library '%s':", libName));
    		}
    		for (int i = 0; i < numExports; i++) {
    			int nid = mem.read32(entryTable + i * 4);
    			int entryAddress = mem.read32(entryTable + (i + numExports) * 4);
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("   0x%08X: 0x%08X", nid, entryAddress));
    			}
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
    public static void setLog4jMDC() {
    	Memory mem = Memory.getInstance();
    	int threadManInfo = 0x88048740;

    	int currentThread = mem.read32(threadManInfo + 0);
    	if (Memory.isAddressGood(currentThread)) {
			int uid = mem.read32(currentThread + 8);
			int cb = SysMemForKernel.getCBFromUid(uid);
			int nameAddr = mem.read32(cb + 16);
			String name = Utilities.readStringZ(nameAddr);

			RuntimeContext.setLog4jMDC(name, uid);
    	} else {
			RuntimeContext.setLog4jMDC("root");
    	}
    }

    public static void dumpAllThreads() {
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
			}
		}

		int cb = SysMemForKernel.getCBFromUid(uid);
		SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
		sceSysmemUidCB.read(mem, cb);

		if (log.isDebugEnabled()) {
			log.debug(String.format("%s: uid=0x%X, name='%s', status=0x%X(%s), currentPriority=0x%X%s", comment, uid, sceSysmemUidCB.name, status, SceKernelThreadInfo.getStatusName(status), currentPriority, waitInfo));
			if (log.isTraceEnabled()) {
				log.trace(Utilities.getMemoryDump(address, 0x140));
			}
		}
    }

    private static void dumpThreadList(Memory mem, int list, String comment) {
    	for (int address = mem.read32(list); address != list; address = mem.read32(address)) {
    		dumpThread(mem, address, comment);
    	}
    }

    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public long hleDecryptBtcnf(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer buffer, int size, int unknown) {
    	int signature = buffer.getValue32();
    	if (signature == 0x0F803001) {
        	// The file flash0:/kd/pspbtcnf.bin is already decrypted
    	} else {
    		log.error(String.format("hleDecryptBtcnf the file flash0:/kd/pspbtcnf.bin is not decrypted. It need to be decrypted on your PSP"));
    	}

    	return size;
    }
}
