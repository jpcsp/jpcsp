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
package jpcsp.HLE.VFS.patch;

import static jpcsp.Allegrex.Common._a1;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.HLE.Modules.sceAtaModule;
import static jpcsp.HLE.Modules.sceDdrModule;
import static jpcsp.HLE.Modules.sceDmacplusModule;
import static jpcsp.HLE.Modules.sceGpioModule;
import static jpcsp.HLE.Modules.sceI2cModule;
import static jpcsp.HLE.Modules.sceLcdcModule;
import static jpcsp.HLE.Modules.sceNandModule;
import static jpcsp.HLE.Modules.scePwmModule;
import static jpcsp.HLE.Modules.sceSysconModule;
import static jpcsp.HLE.Modules.sceSysregModule;
import static jpcsp.HLE.modules.ThreadManForUser.JR;
import static jpcsp.HLE.modules.ThreadManForUser.MOVE;
import static jpcsp.HLE.modules.ThreadManForUser.NOP;
import static jpcsp.HLE.modules.ThreadManForUser.SYSCALL;
import static jpcsp.util.Utilities.read8;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned16;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.HLEModule;
import jpcsp.HLE.VFS.AbstractProxyVirtualFileSystem;
import jpcsp.HLE.VFS.ByteArrayVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.format.Elf32Header;
import jpcsp.util.Utilities;

/**
 * Virtual file system patching/modifying files (e.g. PRX's).
 * 
 * @author gid15
 *
 */
public class PatchFileVirtualFileSystem extends AbstractProxyVirtualFileSystem {
	private static final PatchInfo[] allPatches = new PatchInfo[] {
			// loadcore.prx used by loadCoreInit()
			new PrxPatchInfo("kd/loadcore.prx", 0x0000469C, 0x15C0FFA0, NOP()),      // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L339)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004548, 0x7C0F6244, NOP()),      // Allow loading of privileged modules being not encrypted (take SceLoadCoreExecFileInfo.modInfoAttribute from the ELF module info, https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L351)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004550, 0x14E0002C, 0x1000002C), // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L352)
			new PrxPatchInfo("kd/loadcore.prx", 0x00003D58, 0x10C0FFBE, NOP()),      // Allow linking user stub to kernel lib
			new PrxPatchInfo("kd/loadcore.prx", 0x00005D1C, 0x5040FE91, NOP()),      // Allow loading of non-encrypted modules for apiType==80 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1059)
			new PrxPatchInfo("kd/loadcore.prx", 0x00005D20, 0x3C118002, NOP()),      // Allow loading of non-encrypted modules for apiType==80 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1059)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004378, 0x5120FFDB, NOP()),      // Allow loading of kernel modules being not encrypted (set "execInfo->isKernelMod = SCE_TRUE" even when "decryptMode == 0": https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L118)
			// lowio.prx: syscalls from sceSysreg module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregGetFuseId"        , 0x00001AF4, 0x3C03BC10, 0x3C07BC10),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregAtaClkSelect"     , 0x00002584, 0x27BDFFF0, 0x3C028000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregAtaClkEnable"     , 0x00000510, 0x24040001, 0x0800020A),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregAtaClkDisable"    , 0x0000051C, 0x24040001, 0x0800020A),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregAtahddClkEnable"  , 0x00000528, 0x24040002, 0x0800020A),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregAtahddClkDisable" , 0x00000534, 0x24040002, 0x0800020A),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregUsbhostClkEnable" , 0x00000540, 0x3C040001, 0x0800020A),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregUsbhostClkDisable", 0x0000054C, 0x3C040001, 0x0800020A),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysreg_driver_4841B2D2" , 0x00001788, 0x27BDFFF0, 0xAFBF0008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregApbTimerClkSelect", 0x00001F98, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysreg_driver_96D74557" , 0x00002CB0, 0x27BDFFF0, 0xAFBF0000),
			// lowio.prx: syscalls from sceGpio module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceGpioModule, "sceGpioSetPortMode", 0x00002E24, 0x27BDFFE0, 0xAFB00000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceGpioModule, "sceGpioSetIntrMode", 0x00002FD0, 0x27BDFFE0, 0xAFB00000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceGpioModule, "sceGpioPortRead"   , 0x00003204, 0x3C02BE24, 0x34440004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceGpioModule, "sceGpioPortSet"    , 0x00003214, 0x3C05BE24, 0x34A30008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceGpioModule, "sceGpioPortClear"  , 0x0000322C, 0x3C05BE24, 0x34A3000C),
			// lowio.prx: syscalls from scePwm module
			new PrxSyscallPatchInfo("kd/lowio.prx", scePwmModule, "scePwm_driver_36F98EBA", 0x00003B48, 0x3C028000, 0x2C880003),
			new PrxSyscallPatchInfo("kd/lowio.prx", scePwmModule, "scePwm_driver_94552DD4", 0x000039C0, 0x3C028000, 0x2C880003),
			// lowio.prx: syscalls from sceLcdc module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceLcdcModule, "sceLcdc_driver_E9DBD35F", 0x00007FD8, 0x27BDFFD0, 0xAFB50014),
			// lowio.prx: syscalls from sceDmacplus module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceDmacplusModule, "sceDmacplusLcdcDisable"  , 0x000059DC, 0x27BDFFF0, 0xAFB00000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceDmacplusModule, "sceDmacplusLcdcEnable"   , 0x0000587C, 0x27BDFFF0, 0xAFB00000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceDmacplusModule, "sceDmacplusLcdcSetFormat", 0x000057C0, 0x3C028000, 0x30880007),
			// lowio.prx: syscalls from sceNand module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandSetWriteProtect"     , 0x00009014, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandLock"                , 0x00009094, 0x27BDFFF0, 0x3C030000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandUnlock"              , 0x00009114, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReset"               , 0x00009180, 0x27BDFFF0, 0xAFB00000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadId"              , 0x00009208, 0x24030090, 0x3C01BD10),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadAccess"          , 0x00009260, 0x27BDFFD0, 0x3C028000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWriteAccess"         , 0x00009444, 0x27BDFFD0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandEraseBlock"          , 0x00009608, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadExtraOnly"       , 0x0000970C, 0x27BDFFE0, 0xAFB3000C),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadStatus"          , 0x00009888, 0x3C09BD10, 0x35231008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandSetScramble"         , 0x000098BC, 0x3C030000, 0x00001021),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadPages"           , 0x000098CC, 0x27BDFFF0, 0x00004021),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWritePages"          , 0x00009910, 0x24080010, 0x0005400B),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadPagesRawExtra"   , 0x00009938, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWritePagesRawExtra"  , 0x00009954, 0x24090030, 0x24080020),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadPagesRawAll"     , 0x00009978, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWritePagesRawAll"    , 0x00009994, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDetectChip"          , 0x0000A010, 0x27BDFFE0, 0x03A02021),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWriteBlockWithVerify", 0x0000A134, 0x27BDFFE0, 0xAFBF0014),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadBlockWithRetry"  , 0x0000A1E8, 0x27BDFFE0, 0xAFBF0014),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandVerifyBlockWithRetry", 0x0000A26C, 0x27BDFFC0, 0xAFB50024),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandEraseBlockWithRetry" , 0x0000A3BC, 0x3C030000, 0x8C661A30),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandIsBadBlock"          , 0x0000A430, 0x3C030000, 0x8C661A30),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDoMarkAsBadBlock"    , 0x0000A4BC, 0x27BDFFE0, 0xAFB50014),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDumpWearBBMSize"     , 0x0000A5C8, 0x27BDFFE0, 0xAFB60018),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandCountChipMakersBBM"  , 0x0000A6B4, 0x27BDFFE0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDetectChipMakersBBM" , 0x0000A748, 0x27BDFFD0, 0xAFB60018),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandEraseAllBlock"       , 0x0000A840, 0x27BDFFE0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandTestBlock"           , 0x0000A8D8, 0x27BDBDE0, 0xAFB24208),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandGetPageSize"         , 0x0000AA88, 0x3C040000, 0x03E00008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandGetPagesPerBlock"    , 0x0000AA94, 0x3C040000, 0x03E00008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandGetTotalBlocks"      , 0x0000AAA0, 0x3C040000, 0x03E00008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWriteBlock"          , 0x0000AAAC, 0x27BDFFF0, 0xAFB20008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandCalcEcc"             , 0x0000AB0C, 0x27BDFFF0, 0xAFB3000C),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandVerifyEcc"           , 0x0000AD38, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandCorrectEcc"          , 0x0000AD54, 0x27BDFFF0, 0xAFBF0000),
			// lowio.prx: syscalls from sceI2c module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceI2cModule, "sceI2cMasterTransmitReceive" , 0x000044A8, 0x27BDFFD0, 0xAFB00000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceI2cModule, "sceI2cMasterTransmit"        , 0x00003D3C, 0x27BDFFE0, 0xAFB10004),
			// lowio.prx: syscalls from sceDdr module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceDdrModule, "sceDdrFlush"                 , 0x000011B4, 0x0000000F, 0x3C02BD00),
			// syscon.prx: syscalls
			new PrxSyscallPatchInfo("kd/syscon.prx", sceSysconModule, "sceSysconCmdExec"         , 0x0000154C, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/syscon.prx", sceSysconModule, "sceSysconCmdExecAsync"    , 0x00001600, 0x27BDFFE0, 0xAFB40010),
			new PrxSyscallPatchInfo("kd/syscon.prx", sceSysconModule, "sceSysconGetBaryonVersion", 0x00002DB4, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/syscon.prx", sceSysconModule, "sceSysconGetTimeStamp"    , 0x000025F0, 0x27BDFF90, 0xAFB00060),
			new PrxSyscallPatchInfo("kd/syscon.prx", sceSysconModule, "sceSysconGetPommelVersion", 0x000033E8, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/syscon.prx", sceSysconModule, "sceSysconGetPowerStatus"  , 0x000034AC, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/syscon.prx", sceSysconModule, "sceSysconReadScratchPad"  , 0x0000274C, 0x27BDFF90, 0x24C9FFFF),
			// ata.prx: syscalls
			new PrxSyscallPatchInfo("kd/ata.prx", sceAtaModule, "sceAta_driver_BE6261DA", 0x00002338, 0x0000000F, 0x00042827),
			// semawm.prx used by sceSemawm.module_start
			new PrxPatchInfo("kd/semawm.prx", 0x00005620, 0x27BDFFD0, JR()),           // Disable the internal module signature check
			new PrxPatchInfo("kd/semawm.prx", 0x00005624, 0xAFBF0024, MOVE(_v0, _zr)), // Disable the internal module signature check
			// me_wrapper.prx used by sceMeCodecWrapper.module_start
			new PrxPatchInfo("kd/me_wrapper.prx", 0x00001F38, 0x24050001, MOVE(_a1, _zr)), // Disable wait in sub_00001C30() (https://github.com/uofw/uofw/blob/master/src/me_wrapper/me_wrapper.c#L1169 changed second argument from 1 to 0)
			// Last entry is a dummy one
			new PatchInfo("XXX dummy XXX", 0, 0, 0) // Dummy entry for easier formatting of the above entries
	};

	private static class PatchInfo {
		protected String fileName;
		protected int offset;
		protected int oldValue;
		protected int newValue;

		public PatchInfo(String fileName, int offset, int oldValue, int newValue) {
			this.fileName = fileName;
			this.offset = offset;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public boolean matches(String fileName) {
			return this.fileName.equalsIgnoreCase(fileName);
		}

		protected void apply(byte[] buffer, int offset) {
			if (offset >= 0 && offset < buffer.length) {
				int checkValue = readUnaligned32(buffer, offset);
				if (checkValue != oldValue) {
		    		log.error(String.format("Patching of file '%s' failed at offset 0x%08X, 0x%08X found instead of 0x%08X", fileName, offset, checkValue, oldValue));
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Patching file '%s' at offset 0x%08X: 0x%08X -> 0x%08X", fileName, offset, oldValue, newValue));
					}
					writeUnaligned32(buffer, offset, newValue);
				}
			}
		}

		public void apply(byte[] buffer) {
			apply(buffer, offset);
		}

		protected void patch16(byte[] buffer, int offset, int oldValue, int newValue) {
			if (offset >= 0 && offset < buffer.length) {
				int checkValue = readUnaligned16(buffer, offset);
				if (checkValue != oldValue) {
		    		log.error(String.format("Patching of file '%s' failed at offset 0x%08X, 0x%04X found instead of 0x%04X", fileName, offset, checkValue, oldValue));
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Patching file '%s' at offset 0x%08X: 0x%04X -> 0x%04X", fileName, offset, oldValue, newValue));
					}
					writeUnaligned16(buffer, offset, newValue);
				}
			}
		}

		@Override
		public String toString() {
			return String.format("Patch '%s' at offset 0x%08X: 0x%08X -> 0x%08X", fileName, offset, oldValue, newValue);
		}
	}

	private static class PrxPatchInfo extends PatchInfo {
		private int removeLocationOffset = 4;

		public PrxPatchInfo(String fileName, int offset, int oldValue, int newValue) {
			super(fileName, offset, oldValue, newValue);
		}

		public PrxPatchInfo(String fileName, int offset, int oldValue, int newValue, int removeLocationOffset) {
			super(fileName, offset, oldValue, newValue);
			this.removeLocationOffset = removeLocationOffset;
		}

		private int getFileOffset(byte[] buffer) {
			int elfMagic = readUnaligned32(buffer, 0);
			if (elfMagic != Elf32Header.ELF_MAGIC) {
				return offset;
			}

			int phOffset = readUnaligned32(buffer, 28);
			int phEntSize = readUnaligned16(buffer, 42);
			int phNum = readUnaligned16(buffer, 44);

			int segmentOffset = offset;
			// Scan all the ELF program headers
			for (int i = 0; i < phNum; i++) {
				int offset = phOffset + i * phEntSize;
				int phEntFileSize = readUnaligned32(buffer, offset + 16);
				if (segmentOffset < phEntFileSize) {
					int phFileOffset = readUnaligned32(buffer, offset + 4);
					return phFileOffset + segmentOffset;
				}

				int phEntMemSize = readUnaligned32(buffer, offset + 20);
				segmentOffset -= phEntMemSize;
				if (segmentOffset < 0) {
		    		log.error(String.format("Patching of file '%s' failed: incorrect offset 0x%08X outside of program header segment #%d", fileName, offset, i));
		    		return -1;
				}
			}

    		log.error(String.format("Patching of file '%s' failed: incorrect offset 0x%08X outside of all program header segments", fileName, offset));
			return -1;
		}

		private int getRelocationSegmentNumber(byte[] buffer) {
			int phOffset = readUnaligned32(buffer, 28);
			int phEntSize = readUnaligned16(buffer, 42);
			int phNum = readUnaligned16(buffer, 44);

			// Scan all the ELF program headers
			for (int i = 0; i < phNum; i++) {
				int offset = phOffset + i * phEntSize;
				int phType = readUnaligned32(buffer, offset + 0);
				if (phType == 0x700000A1) {
					return i;
				}
			}

			return -1;
		}

		private void removeRelocation(byte[] buffer) {
			int relocationSegmentNumber = getRelocationSegmentNumber(buffer);
			if (relocationSegmentNumber < 0) {
				return;
			}

			int phOffset = readUnaligned32(buffer, 28);
			int phEntSize = readUnaligned16(buffer, 42);

			int o = readUnaligned32(buffer, phOffset + relocationSegmentNumber * phEntSize + 4);
			o += 2;

			int fbits = read8(buffer, o++);
			int flagShift = 0;
			int flagMask = (1 << fbits) - 1;

			int sbits = relocationSegmentNumber < 3 ? 1 : 2;
	        int segmentShift = fbits;
	        int segmentMask = (1 << sbits) - 1;

			int tbits = read8(buffer, o++);
	        int typeShift = fbits + sbits;
	        int typeMask = (1 << tbits) - 1;

			int nflags = read8(buffer, o++);
			int flags[] = new int[nflags];
			flags[0] = nflags;
			for (int i = 1; i < nflags; i++) {
				flags[i] = read8(buffer, o++);
			}

			int ntypes = read8(buffer, o++);
			int types[] = new int[ntypes];
			types[0] = ntypes;
			for (int i = 1; i < ntypes; i++) {
				types[i] = read8(buffer, o++);
			}

			int offsetShift = fbits + tbits + sbits;
			int OFS_BASE = 0;
			int R_BASE = 0;
			while (o < buffer.length) {
				int cmdOffset = o;
				int R_CMD = readUnaligned16(buffer, o);
				o += 2;

	            // Process the relocation flag.
	            int flagIndex = (R_CMD >> flagShift) & flagMask;
	            int R_FLAG = flags[flagIndex];

	            // Set the segment offset.
	            int S = (R_CMD >> segmentShift) & segmentMask;

	            // Process the relocation type.
	            int typeIndex = (R_CMD >> typeShift) & typeMask;
	            //int R_TYPE = types[typeIndex];

	            if ((R_FLAG & 1) == 0) {
	            	OFS_BASE = S;
	            	switch (R_FLAG & 6) {
		            	case 0:
		            		R_BASE = R_CMD >> (fbits + sbits);
		            		break;
		            	case 4:
		            		R_BASE = readUnaligned32(buffer, o);
		            		o += 4;
		            		break;
	            		default:
	            			return;
	            	}
	            } else {
	            	int R_OFFSET;
	                switch (R_FLAG & 6) {
		                case 0:
		                    R_OFFSET = (int) (short) R_CMD; // sign-extend 16 to 32 bits
		                    R_OFFSET >>= offsetShift;
		                    R_BASE += R_OFFSET;
		                    break;
	                    case 2:
		                    R_OFFSET = (R_CMD << 16) >> offsetShift;
		                    R_OFFSET &= 0xFFFF0000;
		                    R_OFFSET |= read8(buffer, o++);
		                    R_OFFSET |= read8(buffer, o++) << 8;
		                    R_BASE += R_OFFSET;
		                    break;
	                    case 4:
		                	R_BASE = readUnaligned32(buffer, o);
		                	o += 4;
		                	break;
	                	default:
	                		return;
	                }

	                switch (R_FLAG & 0x38) {
	                	case 0x0:
	                	case 0x8:
	                		break;
	                	case 0x10:
	                		o += 2;
	                		break;
                		default:
                			return;
	                }

	                if (log.isTraceEnabled()) {
	                	log.trace(String.format("Relocation R_BASE=0x%08X", R_BASE));
	                }

	                if (R_BASE == offset) {
	                	int newOffset = ((int) (short) R_CMD) >> offsetShift;
	                	if ((R_FLAG & 7) == 1) {
	                		newOffset += removeLocationOffset;
	                	} else {
	                		log.error(String.format("Unsupported relocation patch at 0x%08X, R_FLAG=0x%X", R_BASE, R_FLAG));
	                		return;
	                	}
	                	int newCmd = (flagIndex << flagShift) | (OFS_BASE << segmentShift) | (typeIndex << typeShift) | (newOffset << offsetShift);
	                	newCmd &= 0xFFFF;
	                	patch16(buffer, cmdOffset, R_CMD, newCmd);

	                	int nextCmd = readUnaligned16(buffer, o);
	                	int nextFlagIndex = (nextCmd >> flagShift) & flagMask;
	                	int nextFlag = flags[nextFlagIndex];
	                	int nextSegment = (nextCmd >> segmentShift) & segmentMask;
	                	int nextTypeIndex = (nextCmd >> typeShift) & typeMask;

	                	int newNextOffset = ((int) (short) nextCmd) >> offsetShift;
	                	if ((nextFlag & 7) == 1) {
	                		newNextOffset -= removeLocationOffset;
	                	} else {
	                		log.error(String.format("Unsupported relocation patch at 0x%08X, R_CMD=0x%04X, nextCmd=0x%04X", R_BASE, R_CMD, nextCmd));
	                		return;
	                	}
	                	int newNextCmd = (nextFlagIndex << flagShift) | (nextSegment << segmentShift) | (nextTypeIndex << typeShift) | (newNextOffset << offsetShift);
	                	patch16(buffer, o, nextCmd, newNextCmd);

	                	return;
	                }
	            }
			}
		}

		@Override
		public void apply(byte[] buffer) {
			int fileOffset = getFileOffset(buffer);

			if (fileOffset >= 0) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Patching file '%s' at PRX offset 0x%08X: 0x%08X -> 0x%08X", fileName, offset, oldValue, newValue));
				}
				super.apply(buffer, fileOffset);

				removeRelocation(buffer);
			}
		}
	}

	private static class PrxSyscallPatchInfo extends PrxPatchInfo {
		private PrxPatchInfo patchInfo2;
		private String functionName;

		public PrxSyscallPatchInfo(String fileName, HLEModule hleModule, String functionName, int offset, int oldValue1, int oldValue2) {
			super(fileName, offset, oldValue1, JR(), 8);
			this.functionName = functionName;
			patchInfo2 = new PrxPatchInfo(fileName, offset + 4, oldValue2, SYSCALL(hleModule, functionName));
		}

		@Override
		public void apply(byte[] buffer) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Patching file '%s' at PRX offset 0x%08X: %s", fileName, offset, functionName));
			}
			super.apply(buffer);
			patchInfo2.apply(buffer);
		}
	}

	public PatchFileVirtualFileSystem(IVirtualFileSystem vfs) {
		super(vfs);
	}

	private List<PatchInfo> getPatches(String fileName) {
		List<PatchInfo> filePatches = new LinkedList<PatchInfo>();
		for (PatchInfo patch : allPatches) {
			if (patch.matches(fileName)) {
				filePatches.add(patch);
			}
		}

		if (filePatches.isEmpty()) {
			return null;
		}
		return filePatches;
	}

	private IVirtualFile ioOpenPatchedFile(String fileName, int flags, int mode, List<PatchInfo> patches) {
		IVirtualFile vFile = super.ioOpen(fileName, flags, mode);
		if (vFile == null) {
			return null;
		}

		byte[] buffer = Utilities.readCompleteFile(vFile);
		vFile.ioClose();
		if (buffer == null) {
			return null;
		}

		for (PatchInfo patch : patches) {
			patch.apply(buffer);
		}

		return new ByteArrayVirtualFile(buffer);
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		List<PatchInfo> patches = getPatches(fileName);
		if (patches != null) {
			return ioOpenPatchedFile(fileName, flags, mode, patches);
		}

		return super.ioOpen(fileName, flags, mode);
	}
}
