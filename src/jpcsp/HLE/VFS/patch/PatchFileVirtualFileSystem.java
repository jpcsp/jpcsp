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

import static jpcsp.Allegrex.Common._at;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._v1;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.HLE.modules.ThreadManForUser.JR;
import static jpcsp.HLE.modules.ThreadManForUser.MOVE;
import static jpcsp.HLE.modules.ThreadManForUser.NOP;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.util.LinkedList;
import java.util.List;

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
			// sysmem.prx: disable the function MemoryProtectInit(sub_0000A2C4)
			new PrxPatchInfo("kd/sysmem.prx", 0x0000A2C4, 0x27BDFFF0, JR()),
			new PrxPatchInfo("kd/sysmem.prx", 0x0000A2C8, 0xAFB10004, MOVE(_v0, _zr)),
			// loadcore.prx used by loadCoreInit()
			new PrxPatchInfo("kd/loadcore.prx", 0x0000469C, 0x15C0FFA0, NOP()),      // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L339)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004548, 0x7C0F6244, NOP()),      // Allow loading of privileged modules being not encrypted (take SceLoadCoreExecFileInfo.modInfoAttribute from the ELF module info, https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L351)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004550, 0x14E0002C, 0x1000002C), // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L352)
			new PrxPatchInfo("kd/loadcore.prx", 0x00003D58, 0x10C0FFBE, NOP()),      // Allow linking user stub to kernel lib
			new PrxPatchInfo("kd/loadcore.prx", 0x00005D1C, 0x5040FE91, NOP()),      // Allow loading of non-encrypted modules for apiType==80 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1059)
			new PrxPatchInfo("kd/loadcore.prx", 0x00005D20, 0x3C118002, NOP()),      // Allow loading of non-encrypted modules for apiType==80 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1059)
			// exceptionman.prx used by ExcepManInit()
			new PrxPatchInfo("kd/exceptionman.prx", 0x00000568, 0xACAF0000, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/exceptionman/exceptions.c#L71)
			new PrxPatchInfo("kd/exceptionman.prx", 0x0000018C, 0xAC430004, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/exceptionman/excep.S#L148)
			// interruptman.prx used by IntrManInit()
			new PrxPatchInfo("kd/interruptman.prx", 0x0000103C, 0xAC220008, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1273)
			new PrxPatchInfo("kd/interruptman.prx", 0x00001040, 0xAC200018, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1274)
			new PrxPatchInfo("kd/interruptman.prx", 0x00001044, 0xAC200028, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1275)
			// interruptman.prx used by sceKernelSuspendIntr()/sceKernelResumeIntr()
			new PrxPatchInfo("kd/interruptman.prx", 0x00001084, 0x8C220008, MOVE(_v0, _zr)),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1297)
			new PrxPatchInfo("kd/interruptman.prx", 0x00001088, 0x8C230018, MOVE(_v1, _zr)),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1298)
			new PrxPatchInfo("kd/interruptman.prx", 0x0000108C, 0x8C210028, MOVE(_at, _zr)),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1299)
			new PrxPatchInfo("kd/interruptman.prx", 0x000010C0, 0xAC220008, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1315)
			new PrxPatchInfo("kd/interruptman.prx", 0x000010D8, 0xAC230018, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1321)
			new PrxPatchInfo("kd/interruptman.prx", 0x000010DC, 0xAC220028, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1322)
			// threadman.prx
			new PrxPatchInfo("kd/threadman.prx", 0x0000E674, 0xAC33000C, NOP()),  // Avoid access to hardware register (*(int*)0xBC60000C = 1)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E67C, 0xAC320008, NOP()),  // Avoid access to hardware register (*(int*)0xBC600008 = 48)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E684, 0xAC200010, NOP()),  // Avoid access to hardware register (*(int*)0xBC600010 = 0)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E78C, 0xAC280004, NOP()),  // Avoid access to hardware register (*(int*)0xBC600004 = 0xF0000000)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E798, 0xAC200000, NOP()),  // Avoid access to hardware register (*(int*)0xBC600000 = 0)
			// systimer.prx used by SysTimerInit
			new PrxPatchInfo("kd/systimer.prx", 0x00000334, 0xAD330000, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L203)
			new PrxPatchInfo("kd/systimer.prx", 0x00000340, 0xAD320008, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L204)
			new PrxPatchInfo("kd/systimer.prx", 0x00000348, 0xAD32000C, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L205)
			new PrxPatchInfo("kd/systimer.prx", 0x00000350, 0x8D230000, NOP()),  // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L206)
			// memlmd_01g.prx used by module_start
			new PrxPatchInfo("kd/memlmd_01g.prx", 0x000017EC, 0x0C000711, NOP()),  // Avoid call to sceUtilsBufferCopyByPolling(cmd=15) during initialization
			// Last entry is a dummy one
			new PatchInfo("XXX dummy XXX", 0, 0, 0)                                  // Dummy entry for easier formatting of the above entries
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
			if (offset >= 0) {
				int checkValue = readUnaligned32(buffer, offset);
				if (checkValue != oldValue) {
		    		log.error(String.format("Patching of file '%s' failed at offset 0x%08X, 0x%08X found instead of 0x%08X", fileName, offset, checkValue, oldValue));
				} else {
					writeUnaligned32(buffer, offset, newValue);
				}
			}
		}

		public void apply(byte[] buffer) {
			apply(buffer, offset);
		}

		@Override
		public String toString() {
			return String.format("Patch '%s' at offset 0x%08X: 0x%08X -> 0x%08X", fileName, offset, oldValue, newValue);
		}
	}

	private static class PrxPatchInfo extends PatchInfo {
		public PrxPatchInfo(String fileName, int offset, int oldValue, int newValue) {
			super(fileName, offset, oldValue, newValue);
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

		@Override
		public void apply(byte[] buffer) {
			int fileOffset = getFileOffset(buffer);

			super.apply(buffer, fileOffset);
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
