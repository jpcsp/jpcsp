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

import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.crypto.PGD.PGD_MAGIC;
import static jpcsp.format.PBP.PBP_HEADER_SIZE;
import static jpcsp.format.PBP.PBP_PSAR_DATA_OFFSET;
import static jpcsp.format.PBP.PBP_PSP_DATA_OFFSET;
import static jpcsp.hardware.Model.getGeneration;
import static jpcsp.util.HLEUtilities.JAL;
import static jpcsp.util.Utilities.patch;
import static jpcsp.util.Utilities.readCompleteFile;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeStringNZ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.VFS.ByteArrayVirtualFile;
import jpcsp.HLE.VFS.FakeVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.crypto.PBPVirtualFile;
import jpcsp.HLE.VFS.local.LocalVirtualFile;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.crypto.AMCTRL;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.MMIOHandlerGe;
import jpcsp.util.HLEUtilities;
import jpcsp.util.Utilities;

public class scePopsMan extends HLEModule {
    public static Logger log = Modules.getLogger("scePopsMan");
    public static final String EBOOT_PBP = "EBOOT.PBP";
    private static final String KEYS_BIN = "KEYS.BIN";
    private static final String ebootDummyFileName = "ms0:/PSP/GAME/DUMMY0000/" + EBOOT_PBP;
    private static final int COMPILED_SKD_VERSION = 0x03050000; // popsman.prx requires at least v3.05
    private int POPS_DECOMPRESS_DATA_ADDRESS;
    private int POPS_START_ADDRESS;
    private String ebootPbp;
    private int ebootPbpUid;
    private IVirtualFile vFileEbootPbp;
    private int getIdFunction;
    private boolean isCustomPs1;
    private byte[] ebootKey;

    public static byte[] readEbootKeys(String ebootFileName) throws IOException {
    	if (ebootFileName == null || !ebootFileName.endsWith(EBOOT_PBP)) {
    		return null;
    	}

    	String keysFileName = ebootFileName.substring(0, ebootFileName.length() - EBOOT_PBP.length()) + KEYS_BIN;
    	byte[] key = readCompleteFile(keysFileName);
    	if (key == null) {
    		File keysFile = new File(keysFileName);
    		if (keysFile.canRead() && keysFile.length() > 0) {
				InputStream is = new FileInputStream(keysFile);
    			key = new byte[(int) keysFile.length()];
    			is.read(key);
    			is.close();
    		}
    	}

    	return key;
    }

    public byte[] getEbootKey() {
    	return ebootKey;
    }

	@Override
	public void start() {
    	POPS_DECOMPRESS_DATA_ADDRESS = HLEUtilities.getInstance().installHLESyscall(this, "hlePopsDecompressData");
    	POPS_START_ADDRESS = HLEUtilities.getInstance().installHLESyscall(this, "hlePopsStartHandler");

    	super.start();
	}

    public void loadOnDemand(SceModule module) throws IOException {
		String ebootFileName = module.pspfilename;
		ebootKey = readEbootKeys(ebootFileName);
		IVirtualFile ebootVirtualFile = new PBPVirtualFile(ebootKey, new LocalVirtualFile(new SeekableRandomFile(ebootFileName, "r")));
		FakeVirtualFileSystem.getInstance().registerFakeVirtualFile(ebootDummyFileName, ebootVirtualFile);
		// popsman.prx requires a valid EBOOT.PBP file name (i.e. starting with "ms0:")
		module.pspfilename = ebootDummyFileName;

		// Register a fake license file "ms0:/PSP/LICENSE/XXNNNN-XXXXNNNNN_NN-XXXXXXXXXXXXXXXX.rif"
		IVirtualFile vFile = new LocalVirtualFile(new SeekableRandomFile(ebootFileName, "r"));
		byte[] header = new byte[PBP_HEADER_SIZE];
		vFile.ioRead(header, 0, header.length);
		int pspDataOffset = Utilities.readUnaligned32(header, PBP_PSP_DATA_OFFSET);
		int licenseNameOffset = pspDataOffset + 0x560;
		vFile.ioLseek(licenseNameOffset);
		byte[] licenseBuffer = new byte[36];
		vFile.ioRead(licenseBuffer, 0, licenseBuffer.length);
		String licenseName = new String(licenseBuffer);
		String licenseFileName = "ms0:/PSP/LICENSE/" + licenseName + ".rif";
		byte[] fakeLicenseBuffer = new byte[152];
		writeStringNZ(fakeLicenseBuffer, 16, 48, licenseName);
		IVirtualFile fakeLicenseVirtualFile = new ByteArrayVirtualFile(fakeLicenseBuffer);
		FakeVirtualFileSystem.getInstance().registerFakeVirtualFile(licenseFileName, fakeLicenseVirtualFile);

		int psarDataOffset = Utilities.readUnaligned32(header, PBP_PSAR_DATA_OFFSET);
		vFile.ioLseek(psarDataOffset);
		byte[] psTitleBuffer = new byte[7];
		vFile.ioRead(psTitleBuffer, 0, psTitleBuffer.length);
		int pgdOffset = psarDataOffset;
		if ("PSTITLE".equals(new String(psTitleBuffer))) {
			pgdOffset += 0x200;
		} else {
			pgdOffset += 0x400;
		}
		vFile.ioLseek(pgdOffset);
		byte[] pgdHeader = new byte[4];
		vFile.ioRead(pgdHeader, 0, pgdHeader.length);
		isCustomPs1 = readUnaligned32(pgdHeader, 0) != PGD_MAGIC;
		if (log.isDebugEnabled()) {
			log.debug(String.format("loadOnDemand isCustomPs1=%b", isCustomPs1));
		}
		vFile.ioClose();

    	// popsman.prx is accessing some hardware registers
		RuntimeContextLLE.enableLLE();
    	RuntimeContextLLE.createMMIO();

		// popsman.prx requires at least v3.05
		Modules.SysMemUserForUserModule.hleSetCompiledSdkVersion(COMPILED_SKD_VERSION);

		// popsman.prx requires valid applicationType and bootFrom values
		Modules.InitForKernelModule.setApplicationType(InitForKernel.SCE_INIT_APPLICATION_POPS);
		Modules.InitForKernelModule.setBootFrom(InitForKernel.SCE_INIT_BOOT_MS);

		// Required by popsman.prx when doing a sceSysregMeResetDisable()
		Memory mem = RuntimeContextLLE.getMMIO();
		mem.write32(0xBFC00000, 0x40C23000); // ctc $v0, $6
		mem.write32(0xBFC00004, 0x4002B000); // mfc0 $v0, CPUID
		mem.write32(0xBFC00008, 0x1402000D); // bne $zr, $v0, 0xBFC00040
		mem.write32(0xBFC0000C, 0x00000000); // nop

		Modules.ModuleMgrForUserModule.hleKernelLoadAndStartModule("flash0:/kd/popsman.prx", 0x10);
    }

    public void patchModule(Memory mem, SceModule module) {
    	// Same patches as ProCFW
    	if ("pops".equals(module.modname) && isCustomPs1) {
    		// Only PSP 6.60 and 6.61 are supported, both versions are having the same patch offsets
    		final int[] decompPatchOffsets = new int[] {
    				0x0000DB78, // 01G
    				0x0000DB78, // 02G
    				0x0000DB78, // 03G
    				0x0000DBE8, // 04G
    				0x0000E300, // 05G
    				0xDEADBEEF, // unused
    				0x0000DBE8, // 07G
    				0xDEADBEEF, // unused
    				0x0000DBE8, // 09G
    				0xDEADBEEF, // unused
    				0x0000DBE8  // 11G
    		};
    		patch(mem, module, decompPatchOffsets[getGeneration()], 0x0E000000, JAL(POPS_DECOMPRESS_DATA_ADDRESS), 0xFE000000); // Replace "jal sub_0000E02C" with "jal hlePopsDecompressData"
    	}
    }

    public int hookDrmBBMacFinal2(AMCTRL.BBMac_Ctx ctx, int result) {
    	if (isCustomPs1) {
    		result = 0;
    	}

    	return result;
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hlePopsDecompressData(int destSize, TPointer src, TPointer dest) {
    	int result = Modules.UtilsForKernelModule.sceKernelDeflateDecompress(dest, destSize, src, TPointer32.NULL);
    	if (result >= 0) {
    		// This return value is expected
    		result = 0x92FF;
    	}
    	return result;
    }

    public void hlePopsRegisterGetIdFunc(TPointer getIdFunction) {
    	this.getIdFunction = getIdFunction.getAddress();
    	SceKernelThreadInfo rootThread = Modules.ThreadManForUserModule.getRootThread(null);

    	rootThread.cpuContext.pc = POPS_START_ADDRESS;
    	rootThread.cpuContext.npc = POPS_START_ADDRESS;
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hlePopsStartHandler(int argumentSize, TPointer argument) {
    	SceKernelThreadInfo rootThread = Modules.ThreadManForUserModule.getCurrentThread();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hlePopsStartHandler getIdFunction=0x%08X, rootThread=%s, rootThreadEntry=0x%08X", getIdFunction, rootThread, rootThread.entry_addr));
    	}

    	// Call getIdFunction
    	final int keySize = 16;
    	SysMemInfo keyBufferInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "hlePopsStartHandler", SysMemUserForUser.PSP_SMEM_High, keySize, 0);
    	TPointer fileNameAddr = new TPointer(argument);
    	TPointer keyBuffer = new TPointer(getMemory(), keyBufferInfo.addr);
    	keyBuffer.clear(keySize);
    	Modules.ThreadManForUserModule.executeCallback(rootThread, getIdFunction, null, false, fileNameAddr.getAddress(), keyBuffer.getAddress());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hlePopsStartHandler getIfFunction returning keyBuffer: %s", Utilities.getMemoryDump(keyBuffer, keySize)));
    	}
    	Modules.SysMemUserForUserModule.free(keyBufferInfo);

    	// Start the root thread
    	Modules.ThreadManForUserModule.executeCallback(rootThread, rootThread.entry_addr, null, false, argumentSize, argument.getAddress());

    	return 0;
    }

    @HLEFunction(nid = 0x29B3FB24, version = 150)
    public int scePopsManLoadModule(PspString ebootPbp, int unknown) {
    	this.ebootPbp = ebootPbp.getString();
    	ebootPbpUid = -1;
    	vFileEbootPbp = null;

    	String popsFileName = String.format("flash0:/kd/pops_%02dg.prx", Model.getGeneration());

//    	Modules.ModuleMgrForUserModule.hleKernelLoadAndStartModule("flash0:/vsh/module/paf.prx", 0x19);
    	return Modules.ModuleMgrForUserModule.hleKernelLoadAndStartModule(popsFileName, 0x20);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0090B2C8, version = 150)
    public int scePopsManExitVSHKernel() {
    	return Modules.LoadExecForKernelModule.sceKernelExitVSHKernel(TPointer.NULL);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8D5A07D2, version = 150)
    public int sceMeAudio_8D5A07D2() {
    	ebootPbpUid = 0x1234;
    	try {
			vFileEbootPbp = new LocalVirtualFile(new SeekableRandomFile(new File(ebootPbp), "r"));
		} catch (FileNotFoundException e) {
			log.error("sceMeAudio_8D5A07D2", e);
		}

    	return ebootPbpUid;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x30BE34E4, version = 150)
    public int sceMeAudio_30BE34E4(int uid, TPointer dataAddr, int offset, int size) {
    	if (uid != ebootPbpUid) {
    		return -1;
    	}
    	long seekOffset = offset & 0xFFFFFFFFL;
    	long result = vFileEbootPbp.ioLseek(seekOffset);
    	if (result != seekOffset) {
    		return -1;
    	}

    	return vFileEbootPbp.ioRead(dataAddr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0BABD960, version = 150)
    public int sceMeAudio_0BABD960() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0FA28FE6, version = 150)
    public int sceMeAudio_0FA28FE6() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x14447BA0, version = 150)
    public int sceMeAudio_14447BA0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1A23C094, version = 150)
    public int sceMeAudio_1A23C094() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2AB4FE43, version = 150)
    public int sceMeAudio_2AB4FE43() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2AC64C3F, version = 150)
    public int sceMeAudio_2AC64C3F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3771229C, version = 150)
    public int sceMeAudio_3771229C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x42F0EA37, version = 150)
    public int sceMeAudio_42F0EA37() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4F5B6D82, version = 150)
    public int sceMeAudio_4F5B6D82() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x528266FA, version = 150)
    public int sceMeAudio_528266FA(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=1024, usage=Usage.inout) TPointer buffer) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x54F2AE52, version = 150)
    public int sceMeAudio_54F2AE52() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x68C55F4C, version = 150)
    public int sceMeAudio_68C55F4C(boolean unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x69C4BCCB, version = 150)
    public int sceMeAudio_69C4BCCB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x805D1205, version = 150)
    public int sceMeAudio_805D1205() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x83378E12, version = 150)
    public int sceMeAudio_83378E12() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8A8DFE17, version = 150)
    public int sceMeAudio_8A8DFE17() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9B4AAF7D, version = 150)
    public int sceMeAudio_9B4AAF7D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA6EDDF16, version = 150)
    public int sceMeAudio_A6EDDF16(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer timeAddr) {
    	timeAddr.clear(16);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE5AC375, version = 150)
    public int sceMeAudio_AE5AC375() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBD5F7689, version = 150)
    public int sceMeAudio_BD5F7689() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC93C56F8, version = 150)
    public int sceMeAudio_C93C56F8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD4F17F54, version = 150)
    public int sceMeAudio_D4F17F54() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDE630CD2, version = 150)
    public int sceMeAudio_DE630CD2(int unknown, int spRegisterValue) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7014C540, version = 150)
    public int sceMeAudio_7014C540(int geListId, int stallAddr) {
    	return 0;
    }

    @HLEFunction(nid = 0xE7F06E2B, version = 150)
    public void sceMeAudio_E7F06E2B_setGeStallAddress(int stallAddr) {
    	RuntimeContextLLE.getMMIO().write32(MMIOHandlerGe.BASE_ADDRESS + 0x10C, stallAddr);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE907AE69, version = 150)
    public int sceMeAudio_E907AE69() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF6637A72, version = 150)
    public int sceMeAudio_F6637A72() {
    	return 0;
    }
}
