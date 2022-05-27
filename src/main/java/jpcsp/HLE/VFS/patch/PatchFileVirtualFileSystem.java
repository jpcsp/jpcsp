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

import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.util.HLEUtilities.JR;
import static jpcsp.util.HLEUtilities.MOVE;
import static jpcsp.util.HLEUtilities.NOP;
import static jpcsp.util.HLEUtilities.SYSCALL;
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
			new PrxPatchInfo("kd/loadcore.prx", 0x00005790, 0x5462FFF4, NOP()),      // Allow loading of non-encrypted modules for apiType==0x300 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1149)
			new PrxPatchInfo("kd/loadcore.prx", 0x00005794, 0x3C118002, NOP()),      // Allow loading of non-encrypted modules for apiType==0x300 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1149)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004378, 0x5120FFDB, NOP()),      // Allow loading of kernel modules being not encrypted (set "execInfo->isKernelMod = SCE_TRUE" even when "decryptMode == 0": https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L118)
			// semawm.prx used by sceSemawm.module_start
			new PrxPatchInfo("kd/semawm.prx", 0x00005620, 0x27BDFFD0, JR()),           // Disable the internal module signature check
			new PrxPatchInfo("kd/semawm.prx", 0x00005624, 0xAFBF0024, MOVE(_v0, _zr)), // Disable the internal module signature check
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
			// Can only patch PRX in ELF format
			int headerMagic = Utilities.readUnaligned32(buffer, 0);
			if (headerMagic != Elf32Header.ELF_MAGIC) {
				return;
			}

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

	protected static class PrxSyscallPatchInfo extends PrxPatchInfo {
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
