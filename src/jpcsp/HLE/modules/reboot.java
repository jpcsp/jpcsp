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
import static jpcsp.HLE.Modules.sceNandModule;
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
import static jpcsp.format.PSP.PSP_HEADER_SIZE;
import static jpcsp.util.Utilities.patch;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEModuleManager;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceKernelLoadExecVSHParam;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceLoadCoreBootInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Model;

public class reboot extends HLEModule {
    public static Logger log = Modules.getLogger("reboot");
    private static final boolean enableReboot = false;
    private static final String rebootFileName = "flash0:/reboot.bin";
    private static final int rebootBaseAddress = MemoryMap.START_KERNEL + 0x600000;
    private static final int rebootParamAddress = MemoryMap.START_KERNEL + 0x400000;

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

		patch(mem, rebootModule, 0x00004AA0, 0x18600007, 0x18600039); // Skip initialization accessing hardware register 0xBC1000nn
		patch(mem, rebootModule, 0x00012AD4, 0xFFFFFFFF, 0x00100000); // Change initial value of static variable: https://github.com/uofw/uofw/blob/master/src/reboot/unk.c#L563
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

		patchSyscall(0x0000EFCC, sceNandModule  , "sceNandInit2"                  , mem, rebootModule, 0x27BDFFF0, 0xAFB10004);
		patchSyscall(0x0000F0C4, sceNandModule  , "sceNandIsReady"                , mem, rebootModule, 0x3C03BD10, 0x8C631004);
		patchSyscall(0x0000F0D4, sceNandModule  , "sceNandSetWriteProtect"        , mem, rebootModule, 0x3C03BD10, 0x8C631004);
		patchSyscall(0x0000F144, sceNandModule  , "sceNandLock"                   , mem, rebootModule, 0x3C058864, 0x8CA6909C);
		patchSyscall(0x0000F198, sceNandModule  , "sceNandReset"                  , mem, rebootModule, 0x27BDFFF0, 0xAFB00000);
		patchSyscall(0x0000F234, sceNandModule  , "sceNandReadId"                 , mem, rebootModule, 0x24030090, 0x3C01BD10);
		patchSyscall(0x0000F28C, sceNandModule  , "sceNandReadAccess"             , mem, rebootModule, 0x27BDFFF0, 0x3C028000);
		patchSyscall(0x0000F458, sceNandModule  , "sceNandWriteAccess"            , mem, rebootModule, 0x27BDFFE0, 0x3C028000);
		patchSyscall(0x0000F640, sceNandModule  , "sceNandEraseBlock"             , mem, rebootModule, 0x27BDFFF0, 0xAFB00000);
		patchSyscall(0x0000F72C, sceNandModule  , "sceNandReadExtraOnly"          , mem, rebootModule, 0x27BDFFE0, 0xAFB20008);
		patchSyscall(0x0000F8A8, sceNandModule  , "sceNandReadStatus"             , mem, rebootModule, 0x3C09BD10, 0x35231008);
		patchSyscall(0x0000F8DC, sceNandModule  , "sceNandSetScramble"            , mem, rebootModule, 0x3C038864, 0x00001021);
		patchSyscall(0x0000F8EC, sceNandModule  , "sceNandReadPages"              , mem, rebootModule, 0x27BDFFF0, 0x00004021);
		patchSyscall(0x0000F930, sceNandModule  , "sceNandWritePages"             , mem, rebootModule, 0x24080010, 0x0005400B);
		patchSyscall(0x0000F958, sceNandModule  , "sceNandReadPagesRawExtra"      , mem, rebootModule, 0x27BDFFF0, 0xAFBF0000);
		patchSyscall(0x0000F974, sceNandModule  , "sceNandWritePagesRawExtra"     , mem, rebootModule, 0x24090030, 0x24080020);
		patchSyscall(0x0000F998, sceNandModule  , "sceNandReadPagesRawAll"        , mem, rebootModule, 0x27BDFFF0, 0xAFBF0000);
		patchSyscall(0x0000F9D0, sceNandModule  , "sceNandTransferDataToNandBuf"  , mem, rebootModule, 0x27BDFFE0, 0xAFB00010);
		patchSyscall(0x0000FC40, sceNandModule  , "sceNandIntrHandler"            , mem, rebootModule, 0x27BDFFF0, 0xAFBF0008);
		patchSyscall(0x0000FF60, sceNandModule  , "sceNandTransferDataFromNandBuf", mem, rebootModule, 0x27BDFFE0, 0xAFB20018);
		patchSyscall(0x000103C8, sceNandModule  , "sceNandWriteBlockWithVerify"   , mem, rebootModule, 0x27BDFFE0, 0xAFBF0014);
		patchSyscall(0x0001047C, sceNandModule  , "sceNandReadBlockWithRetry"     , mem, rebootModule, 0x27BDFFE0, 0xAFBF0014);
		patchSyscall(0x00010500, sceNandModule  , "sceNandVerifyBlockWithRetry"   , mem, rebootModule, 0x27BDFFC0, 0xAFB50024);
		patchSyscall(0x00010650, sceNandModule  , "sceNandEraseBlockWithRetry"    , mem, rebootModule, 0x3C038864, 0x8C6696D4);
		patchSyscall(0x000106C4, sceNandModule  , "sceNandIsBadBlock"             , mem, rebootModule, 0x3C038864, 0x8C6696D4);
		patchSyscall(0x00010750, sceNandModule  , "sceNandDoMarkAsBadBlock"       , mem, rebootModule, 0x27BDFFE0, 0xAFB50014);
		patchSyscall(0x000109DC, sceNandModule  , "sceNandDetectChipMakersBBM"    , mem, rebootModule, 0x27BDFFD0, 0xAFB60018);
		patchSyscall(0x00010D1C, sceNandModule  , "sceNandGetPageSize"            , mem, rebootModule, 0x3C048864, 0x03E00008);
		patchSyscall(0x00010D28, sceNandModule  , "sceNandGetPagesPerBlock"       , mem, rebootModule, 0x3C048864, 0x03E00008);
		patchSyscall(0x00010D34, sceNandModule  , "sceNandGetTotalBlocks"         , mem, rebootModule, 0x3C048864, 0x03E00008);
		patchSyscall(0x00011548, this           , "hleGetUniqueId"                , mem, rebootModule, 0x3C03BC10, 0x3C07BC10);
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

    	if (log.isDebugEnabled()) {
			log.debug(String.format("sceReboot arg0=%s, arg1=%s", sceLoadCoreBootInfoAddr, sceKernelLoadExecVSHParamAddr));
		}

    	return true;
    }

    private static void patchSyscall(int offset, HLEModule hleModule, String functionName, Memory mem, SceModule rebootModule, int oldCode1, int oldCode2) {
		patch(mem, rebootModule, offset + 0, oldCode1, JR());
		patch(mem, rebootModule, offset + 4, oldCode2, ThreadManForUser.SYSCALL(hleModule, functionName));
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public long hleGetUniqueId() {
    	return 0L;
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
