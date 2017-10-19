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
import jpcsp.Allegrex.compiler.Compiler;
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
import jpcsp.HLE.kernel.types.SceSysmemUidCB;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Model;
import jpcsp.util.Utilities;

public class reboot extends HLEModule {
    public static Logger log = Modules.getLogger("reboot");
    public static final boolean enableReboot = false;
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

    	markMMIO();

//		patch(mem, rebootModule, 0x00004AA0, 0x18600007, 0x18600039); // Skip initialization accessing hardware register 0xBC1000nn
//		patch(mem, rebootModule, 0x00012AD4, 0xFFFFFFFF, 0x00100000); // Change initial value of static variable: https://github.com/uofw/uofw/blob/master/src/reboot/unk.c#L563
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

    private static void markMMIO() {
    	int[] addresses = {
    			0x0800A310, 0x0800A33C, 0x0800A3A4, 0x0800A3D8, 0x0800A3E0,
    			0x0800A3E8, 0x0800A3F0, 0x0800A3F8, 0x0800A400, 0x0800A40C,
    			0x0800A4B4, 0x0800A4DC, 0x0800D860, 0x0802158C, 0x08021968,
    			0x08022E3C, 0x08022E40, 0x08022E44, 0x08022E74, 0x08022E78,
    			0x08022E7C, 0x08022F6C, 0x08022F78, 0x08022F88, 0x08022F94, 0x080230D8,
    			0x080230E8, 0x080230F4, 0x08023D3C, 0x08023D40, 0x08023D44,
    			0x08023D84, 0x08023D88, 0x08023D8C, 0x08023DC0, 0x08023DD8,
    			0x08023DDC, 0x0802E858, 0x0802E85C, 0x0802E864, 0x0803CE74,
    			0x0803CE7C, 0x0803CE84, 0x0803CF8C, 0x0803CF98, 0x0803D464,
    			0x0803D4F4, 0x0803F530, 0x0804E634, 0x0804E640, 0x0804E648,
    			0x0804E650, 0x080590BC, 0x080590D8, 0x080594D0, 0x080594D8,
    			0x080594E0, 0x080594E8, 0x080594F0, 0x08059504, 0x08059534,
    			0x08059540, 0x080595BC, 0x080737D8, 0x080737FC, 0x08073AD8,
    			0x08073AFC, 0x08073EA0, 0x08073EC4, 0x080742F8, 0x0807431C,
    			0x08074F9C, 0x080750D0, 0x080750E4, 0x080752CC, 0x0807571C,
    			0x0807574C, 0x08076348, 0x08076350, 0x08076358, 0x08076360,
    			0x08076368, 0x08076370, 0x080763A0, 0x080763B0, 0x080763BC,
    			0x080763C4, 0x080763D0, 0x080763F0, 0x08076D60, 0x08076D74,
    			0x08076DA4, 0x08076DB8, 0x08076E98, 0x080789E8, 0x08078A84,
    			0x08078A98, 0x08078D40, 0x08078D48, 0x08078D70, 0x08078D90,
    			0x08078D9C, 0x080795F4, 0x0807960C, 0x08079624, 0x08079638,
    			0x08079698, 0x0807AC78, 0x0807AC88, 0x0807AFE8, 0x0807B05C,
    			0x0807B068, 0x0807B06C, 0x0807B070, 0x0807B074, 0x0807B078,
    			0x0807B07C, 0x0807B080, 0x0807B084, 0x0807B088, 0x0807B094,
    			0x0807B0A0, 0x0807B0AC, 0x0807B0EC, 0x0807C1BC, 0x0807C20C,
    			0x0807C28C, 0x08083384, 0x0808338C, 0x08083394, 0x0808339C,
    			0x080833A8, 0x080833D4, 0x080833E4, 0x080833F4, 0x08083404,
    			0x0808340C, 0x08083414, 0x0808341C, 0x08083424, 0x0808342C,
    			0x08083434, 0x0808343C, 0x08083444, 0x08083450, 0x08083458,
    			0x08083460, 0x08083580, 0x08083594, 0x0808359C, 0x080835A8,
    			0x080835B4, 0x080835BC, 0x080835D8, 0x080835E8, 0x080835F8,
    			0x08083608, 0x08083618, 0x08083624, 0x0808575C, 0x08085764,
    			0x0808578C, 0x08085794, 0x0808579C, 0x080857A4, 0x080857B4,
    			0x080857E8, 0x0808585C, 0x08092AF8, 0x08092B04, 0x08092B10,
    			0x08092B18, 0x080BC73C, 0x080BC9EC, 0x080BC9FC, 0x080BCA08,
    			0x080BCA2C, 0x080BCBBC, 0x080BCCE4, 0x080BCD20, 0x080BCE58,
    			0x080BCE64, 0x080BCE70, 0x080BCE7C, 0x080BCE84, 0x080BCE88,
    			0x080BCE8C, 0x080BCE90, 0x080BCE94, 0x080BCE9C, 0x080BCEA0,
    			0x080BCEA4, 0x080BCEE8, 0x080BCEF4, 0x080BCF04, 0x080BCF10,
    			0x080BCF20, 0x080BCF2C, 0x080BCF3C, 0x080BCF48, 0x080BCF58,
    			0x080BCF64, 0x080BCF74, 0x080BCF80, 0x080BCF90, 0x080BCFA0,
    			0x080BCFB0, 0x080BCFC0, 0x080BCFD4, 0x080BCFE4, 0x080BCFF8,
    			0x080BD008, 0x080BD024, 0x080BD248, 0x080BD24C, 0x080BD250,
    			0x080BD254, 0x080BD258, 0x080BD25C, 0x080BD260, 0x080BD264,
    			0x080BE5DC, 0x080BE5E4, 0x080BE670, 0x080BE67C, 0x080FDB4C,
    			0x080FDB60, 0x080FDB68, 0x080FDB7C, 0x080FDB8C, 0x080FDB94,
    			0x080FDBA8, 0x080FDBB0, 0x080FDBC0, 0x080FDBC8, 0x080FDBD8,
    			0x080FDBE0, 0x080FDBF8, 0x080FDC00, 0x080FDC34, 0x08604ACC,
    			0x08604AE0, 0x08604AE8, 0x08604AF4, 0x08604AFC, 0x08604B08,
    			0x08604B2C, 0x08604B38, 0x08604B40, 0x08604B4C, 0x08604B54,
    			0x08604B6C, 0x08604B7C, 0x08604E24, 0x08604E34, 0x08604E70,
    			0x08611414
    	};

    	Compiler compiler = Compiler.getInstance();
    	for (int address : addresses) {
    		compiler.addMMIORange(address, 1);
    	}
    	compiler.addMMIORange(MemoryMap.START_KERNEL, 0x800000);
    }

    private static void patchSyscall(int offset, HLEModule hleModule, String functionName, Memory mem, SceModule rebootModule, int oldCode1, int oldCode2) {
		patch(mem, rebootModule, offset + 0, oldCode1, JR());
		patch(mem, rebootModule, offset + 4, oldCode2, ThreadManForUser.SYSCALL(hleModule, functionName));
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

    public static void dumpAllThreads() {
    	Memory mem = Memory.getInstance();
    	int threadManInfo = 0x88048740;

    	dumpThread(mem, mem.read32(threadManInfo + 0), "Current thread:");
    	dumpThreadList(mem, threadManInfo + 1176, "Sleeping threads:");
    	dumpThreadList(mem, threadManInfo + 1184, "Delayed threads:");
    	dumpThreadList(mem, threadManInfo + 1192, "Stopped threads:");
    	dumpThreadList(mem, threadManInfo + 1200, "Suspended threads:");
    	for (int priority = 0; priority < 128; priority++) {
    		dumpThreadList(mem, threadManInfo + 152 + priority * 8, String.format("Ready threads[prio=0x%X]:", priority));
    	}
    }

    private static void dumpThread(Memory mem, int address, String comment) {
		int uid = mem.read32(address + 8);
		int status = mem.read32(address + 12);
		int currentPriority = mem.read32(address + 16);
		int waitType = mem.read32(address + 88);
		int waitDelay = mem.read32(address + 96);

		int cb = SysMemForKernel.getCBFromUid(uid);
		SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
		sceSysmemUidCB.read(mem, cb);

		if (log.isDebugEnabled()) {
			if (comment != null) {
				log.debug(comment);
			}
			log.debug(String.format("Thread uid=0x%X, name='%s', status=0x%X(%s), currentPriority=0x%X, waitType=0x%X(%s), waitDelay=0x%X: %s", uid, sceSysmemUidCB.name, status, SceKernelThreadInfo.getStatusName(status), currentPriority, waitType, SceKernelThreadInfo.getWaitName(waitType, 0, new ThreadWaitInfo(), status), waitDelay, Utilities.getMemoryDump(address, 0x140)));
		}
    }

    private static void dumpThreadList(Memory mem, int list, String comment) {
    	for (int address = mem.read32(list); address != list; address = mem.read32(address)) {
    		dumpThread(mem, address, comment);
    		comment = null;
    	}
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
