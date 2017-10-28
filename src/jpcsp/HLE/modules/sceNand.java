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

import static java.lang.Integer.rotateRight;
import static jpcsp.HLE.HLEModuleManager.InternalSyscallNid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer8;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.compress.CompressPrxVirtualFileSystem;
import jpcsp.HLE.VFS.fat.Fat12VirtualFile;
import jpcsp.HLE.VFS.local.LocalVirtualFileSystem;
import jpcsp.HLE.VFS.patch.PatchFileVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceNandSpare;
import jpcsp.util.Utilities;

public class sceNand extends HLEModule {
    public static Logger log = Modules.getLogger("sceNand");
    protected boolean writeProtected;
    protected int scramble;
    private static final int pageSize = 0x200; // 512B per page
    private static final int pagesPerBlock = 0x20; // 16KB per block
    private static final int totalBlocks = 0x800; // 32MB in total
    private byte[] dumpBlocks;
    private byte[] dumpSpares;
    private int[] dumpResults;
    private int[] ppnToLbn = new int[0x10000];
    private static final boolean emulateNand = true;
    private Fat12VirtualFile vFile3;
    private Fat12VirtualFile vFile603;
    private Fat12VirtualFile vFile703;

    @Override
	public void start() {
		writeProtected = true;
		scramble = 0;

		dumpBlocks = readBytes("nand.block");
		dumpSpares = readBytes("nand.spare");
		dumpResults = readInts("nand.result");

		int lbn = 0;
		for (int ppn = 0; ppn < ppnToLbn.length; ppn++) {
			if ((ppn % 0x3E00) >= 0x1440 && (ppn % 0x3E00) < 0x1440 + pagesPerBlock * 16) {
				ppnToLbn[ppn] = 0xFFFF;
			} else if (ppn >= 0x800) {
    			ppnToLbn[ppn] = lbn;
    			if ((ppn % pagesPerBlock) == pagesPerBlock - 1) {
    				lbn++;
    			}
    		} else {
    			ppnToLbn[ppn] = 0x0000;
    		}
		}

		byte[] fuseId = readBytes("nand.fuseid");
		if (fuseId != null && fuseId.length == 8) {
			Modules.sceSysregModule.setFuseId(Utilities.readUnaligned64(fuseId, 0));
		}

		super.start();
	}

    private static byte[] readBytes(String fileName) {
    	byte[] bytes = null;
    	try {
    		File file = new File(fileName);
			InputStream is = new FileInputStream(file);
			bytes = new byte[(int) file.length()];
			is.read(bytes);
			is.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

    	return bytes;
    }

    private static int[] readInts(String fileName) {
    	byte[] bytes = readBytes(fileName);
    	if (bytes == null) {
    		return null;
    	}

    	int[] ints = new int[bytes.length / 4];
    	ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(ints);

    	return ints;
    }

    protected void descramblePage(int ppn, TPointer user, byte[] blocks, int offset) {
    	int scrmb = rotateRight(scramble, 21);
    	int key = rotateRight(ppn, 17) ^ (scrmb * 7);
    	int scrambleOffset = ((ppn ^ scrmb) & 0x1F) << 4;

    	for (int i = 0; i < pageSize; i += 16) {
    		int value0 = Utilities.readUnaligned32(blocks, offset + scrambleOffset);
    		int value1 = Utilities.readUnaligned32(blocks, offset + scrambleOffset + 4);
    		int value2 = Utilities.readUnaligned32(blocks, offset + scrambleOffset + 8);
    		int value3 = Utilities.readUnaligned32(blocks, offset + scrambleOffset + 12);
    		scrambleOffset += 16;
    		if (scrambleOffset >= pageSize) {
    			scrambleOffset -= pageSize;
    		}
    		value0 -= key;
    		key += value0;
    		user.setValue32(i, value0);
    		value1 -= key;
    		key ^= value1;
    		user.setValue32(i + 4, value1);
    		value2 -= key;
    		key -= value2;
    		user.setValue32(i + 8, value2);
    		value3 -= key;
    		key += value3;
    		user.setValue32(i + 12, value3);
    		key += scrmb;
    		key = Integer.reverse(key);
    	}
    }

    protected void descramble(int ppn, TPointer user, int len, byte[] blocks, int offset) {
    	for (int i = 0; i < len; i++) {
    		descramblePage(ppn, user, blocks, offset);
    		ppn++;
    		offset += pageSize;
    		user.add(pageSize);
    	}
    }

    private void readMasterBootRecord0(TPointer buffer) {
    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x01);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x05);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0xBE);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x40);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0xEF80);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecord2(TPointer buffer) {
    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x01);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x01);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0x00);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0xBFE0);

    	// Second partition entry
    	partitionEntry += 16;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x01);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x05);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0x80);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0xC000);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0x2000);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecord602(TPointer buffer) {
    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x01);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x01);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0x80);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0x1FE0);

    	// Second partition entry
    	partitionEntry += 16;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x81);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x05);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0xA0);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0xE000);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0x800);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecord702(TPointer buffer) {
    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) 0x81);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x01);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0xA0);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0x7E0);

    	// Second partition entry
    	partitionEntry += 16;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x00);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) 0xA1);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x05);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0xBE);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0xE800);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0x780);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readMasterBootRecord742(TPointer buffer) {
    	// First partition entry
    	int partitionEntry = 446;

    	// Status of physical drive
    	buffer.setValue8(partitionEntry + 0, (byte) 0x00);
    	// CHS address of first absolute sector in partition
    	buffer.setValue8(partitionEntry + 1, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 2, (byte) 0xC1);
    	buffer.setValue8(partitionEntry + 3, (byte) 0xA1);
    	// Partition type
    	buffer.setValue8(partitionEntry + 4, (byte) 0x01);
    	// CHS address of last absolute sector in partition
    	buffer.setValue8(partitionEntry + 5, (byte) 0x01);
    	buffer.setValue8(partitionEntry + 6, (byte) 0xE0);
    	buffer.setValue8(partitionEntry + 7, (byte) 0xBE);
    	// LBA of first absolute sector in the partition
    	buffer.setUnalignedValue32(partitionEntry + 8, 0x20);
    	// Number of sectors in partition
    	buffer.setUnalignedValue32(partitionEntry + 12, 0x760);

    	// Boot signature
    	buffer.setValue8(510, (byte) 0x55);
    	buffer.setValue8(511, (byte) 0xAA);
    }

    private void readFile(TPointer buffer, IVirtualFile vFile, int ppn, int lbnStart) {
    	int lbn = ppnToLbn[ppn];
    	int sectorNumber = (lbn - lbnStart) * pagesPerBlock + (ppn % pagesPerBlock);
if (ppn >= 0x900 && ppn < 0xD040) {
	sectorNumber -= pagesPerBlock;
} else if (ppn > 0xD0C0 && ppn < 0xF060) {
	sectorNumber -= pagesPerBlock;
}
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readFile ppn=0x%X, lbnStart=0x%X, lbn=0x%X, sectorNumber=0x%X", ppn, lbnStart, lbn, sectorNumber));
    	}
    	readFile(buffer, vFile, sectorNumber);
    }

    private void readFile(TPointer buffer, IVirtualFile vFile, int sectorNumber) {
    	vFile.ioLseek(sectorNumber * pageSize);
    	vFile.ioRead(buffer, pageSize);
    }

    protected int hleNandReadPages(int ppn, TPointer user, TPointer spare, int len, boolean raw, boolean spareUserEcc) {
    	if (user.isNotNull()) {
	    	if (dumpBlocks != null && !emulateNand) {
	    		if (scramble != 0) {
	    			descramble(ppn, user, len, dumpBlocks, ppn * pageSize);
	    		} else {
	    			Utilities.writeBytes(user.getAddress(), len * pageSize, dumpBlocks, ppn * pageSize);
	    		}
	    	} else {
	    		for (int i = 0; i < len; i++) {
	    			user.clear(pageSize);
		    		if ((ppn + i) == 0x80) {
		    			for (int n = 0; n < 8; n++) {
		    				user.setValue16(n * 2, (short) (16 + n));
		    			}
		    		} else if (ppnToLbn[ppn + i] == 0) {
		    			// Master Boot Record
		    			readMasterBootRecord0(user);
		    		} else if (ppnToLbn[ppn + i] == 2) {
		    			// Master Boot Record
		    			readMasterBootRecord2(user);
		    		} else if (ppnToLbn[ppn + i] >= 0x3 && ppnToLbn[ppn + i] < 0x602) {
		    			if (vFile3 == null) {
		    				IVirtualFileSystem vfs = new LocalVirtualFileSystem("flash0/", false);

		    				// Apply patches for some files as required
		    				vfs = new PatchFileVirtualFileSystem(vfs);

		    				// All the PRX files need to be compressed so that they can fit
		    				// into the space available on flash0.
		    				vfs = new CompressPrxVirtualFileSystem(vfs);

		    				vFile3 = new Fat12VirtualFile(vfs, 0xBFE0);
		    				vFile3.scan();
		    			}
		    			readFile(user, vFile3, ppn + i, 0x3);
		    		} else if (ppnToLbn[ppn + i] == 0x602) {
		    			// Master Boot Record
		    			readMasterBootRecord602(user);
		    		} else if (ppnToLbn[ppn + i] >= 0x603 && ppnToLbn[ppn + i] < 0x702) {
		    			if (vFile603 == null) {
		    				IVirtualFileSystem vfs = new LocalVirtualFileSystem("flash1/", false);
		    				vFile603 = new Fat12VirtualFile(vfs, 0x1FE0);
		    				vFile603.scan();
		    			}
		    			readFile(user, vFile603, ppn + i, 0x603);
		    		} else if (ppnToLbn[ppn + i] == 0x702) {
		    			// Master Boot Record
		    			readMasterBootRecord702(user);
		    		} else if (ppnToLbn[ppn + i] >= 0x703 && ppnToLbn[ppn + i] < 0x742) {
		    			if (vFile703 == null) {
		    				IVirtualFileSystem vfs = new LocalVirtualFileSystem("flash2/", false);
		    				vFile703 = new Fat12VirtualFile(vfs, 0x7E0);
		    				vFile703.scan();
		    			}
		    			readFile(user, vFile703, ppn + i, 0x703);
		    		} else if (ppnToLbn[ppn + i] == 0x742) {
		    			// Master Boot Record
		    			readMasterBootRecord742(user);
		    		}
		    		user.add(pageSize);
	    		}
	    	}
    	}

    	if (spare.isNotNull()) {
        	if (dumpSpares != null && !emulateNand) {
        		if (spareUserEcc) {
        			// Write the userEcc
        			Utilities.writeBytes(spare.getAddress(), len * 16, dumpSpares, ppn * 16);
        		} else {
        			// Do not return the userEcc
    	    		for (int i = 0; i < len; i++) {
    	    			Utilities.writeBytes(spare.getAddress() + i * 12, 12, dumpSpares, (ppn + i) * 16 + 4);
    	    		}
        		}
        	} else {
    	    	SceNandSpare sceNandSpare = new SceNandSpare();
    	    	for (int i = 0; i < len; i++) {
        			sceNandSpare.blockFmt = (ppn + i) < 0x800 ? 0xFF : 0x00;
    	    		sceNandSpare.blockStat = 0xFF;
        			sceNandSpare.lbn = ppnToLbn[ppn + i];
    	    		if (ppn == 0x80) {
    	    			sceNandSpare.id = 0x6DC64A38; // For IPL area
    	    		}
    	    		sceNandSpare.reserved2[0] = 0xFF;
    	    		sceNandSpare.reserved2[1] = 0xFF;

    	    		// All values are set to 0xFF when the lbn is 0xFFFF
    	    		if (sceNandSpare.lbn == 0xFFFF) {
    	    			sceNandSpare.userEcc[0] = 0xFF;
    	    			sceNandSpare.userEcc[1] = 0xFF;
    	    			sceNandSpare.userEcc[2] = 0xFF;
    	    			sceNandSpare.reserved1 = 0xFF;
    	    			sceNandSpare.blockFmt = 0xFF;
    	    			sceNandSpare.blockStat = 0xFF;
    	    			sceNandSpare.id = 0xFFFFFFFF;
    	    			sceNandSpare.spareEcc[0] = 0xFF;
    	    			sceNandSpare.spareEcc[1] = 0xFF;
    	    			sceNandSpare.reserved2[0] = 0xFF;
    	    			sceNandSpare.reserved2[1] = 0xFF;
    	    		}

    	    		if (spareUserEcc) {
    	    			sceNandSpare.write(spare, i * sceNandSpare.sizeof());
    	    		} else {
    	    			sceNandSpare.writeNoUserEcc(spare, i * sceNandSpare.sizeofNoEcc());
    	    		}
    	    	}
        	}
    	}

    	int result = 0;
    	if (dumpResults != null) {
    		result = dumpResults[ppn / pagesPerBlock];
    	}

    	return result;
    }

    @HLEFunction(nid = 0xB07C41D4, version = 150)
    public int sceNandGetPagesPerBlock() {
    	// Has no parameters
        return pagesPerBlock;
    }

    @HLEFunction(nid = 0xCE9843E6, version = 150)
    public int sceNandGetPageSize() {
    	// Has no parameters
        return pageSize;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE4438C7, version = 150)
    public int sceNandLock(int mode) {
    	sceNandSetWriteProtect(mode == 0);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x41FFA822, version = 150)
    public int sceNandUnlock() {
    	// Has no parameters
    	sceNandSetWriteProtect(true);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01F09203, version = 150)
    public int sceNandIsBadBlock(int ppn) {
    	if ((ppn % pagesPerBlock) != 0) {
    		return -1;
    	}

    	int result = 0;
    	if (dumpSpares != null) {
    		int blockStat = dumpSpares[ppn * 16 + 5] & 0xFF;
    		if (blockStat == 0xFF) {
    			result = 0;
    		} else {
    			result = 1;
    		}
    	}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0BEE8F36, version = 150)
    public int sceNandSetScramble(int scramble) {
    	this.scramble = scramble;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x84EE5D76, version = 150)
    public boolean sceNandSetWriteProtect(boolean protect) {
    	boolean result = writeProtected;

    	writeProtected = protect;

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8932166A, version = 150)
    public int sceNandWritePagesRawExtra(int ppn, TPointer user, TPointer spare, int len) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5182C394, version = 150)
    public int sceNandReadExtraOnly(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer spare, int len) {
    	hleNandReadPages(ppn, TPointer.NULL, spare, len, true, true);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x89BDCA08, version = 150)
    public int sceNandReadPages(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE05AE88D, version = 150)
    public int sceNandReadPagesRawExtra(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, true, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC32EA051, version = 150)
    public int sceNandReadBlockWithRetry(int ppn, TPointer user, TPointer spare) {
    	return hleNandReadPages(ppn, user, spare, pagesPerBlock, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB2B021E5, version = 150)
    public int sceNandWriteBlockWithVerify(int ppn, TPointer user, TPointer spare) {
    	return 0;
    }

    @HLEFunction(nid = 0xC1376222, version = 150)
    public int sceNandGetTotalBlocks() {
    	// Has no parameters
    	return totalBlocks;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE41A11DE, version = 150)
    public int sceNandReadStatus() {
    	// Has no parameters
    	int result = 0;
    	if (!writeProtected) {
    		result |= 0x80; // not write protected
    	}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEB0A0022, version = 150)
    public int sceNandEraseBlock(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7AF7B77A, version = 150)
    public int sceNandReset(@CanBeNull @BufferInfo(usage=Usage.out) TPointer8 statusAddr) {
    	statusAddr.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public boolean sceNandIsReady() {
    	return true;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandInit2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandTransferDataToNandBuf() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandIntrHandler() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = InternalSyscallNid, version = 150)
    public int sceNandTransferDataFromNandBuf() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFCDF7610, version = 150)
    public int sceNandReadId(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer8 id, int len) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x766756EF, version = 150)
    public int sceNandReadAccess(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len, int mode) {
    	return hleNandReadPages(ppn, user, spare, len, false, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0ADC8686, version = 150)
    public int sceNandWriteAccess(int ppn, TPointer user, TPointer spare, int len, int mode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8AF0AB9F, version = 150)
    public int sceNandWritePages(int ppn, TPointer user, TPointer spare, int len) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC478C1DE, version = 150)
    public int sceNandReadPagesRawAll(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, true, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5AC02755, version = 150)
    public int sceNandVerifyBlockWithRetry(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8933B2E0, version = 150)
    public int sceNandEraseBlockWithRetry(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC29DA136, version = 150)
    public int sceNandDoMarkAsBadBlock(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2FF6081B, version = 150)
    public int sceNandDetectChipMakersBBM(int ppn) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBADD5D46, version = 150)
    public int sceNandWritePagesRawAll() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD897C343, version = 150)
    public int sceNandDetectChip() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3F76BC21, version = 150)
    public int sceNandDumpWearBBMSize() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEBA0E6C6, version = 150)
    public int sceNandCountChipMakersBBM() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2674CFFE, version = 150)
    public int sceNandEraseAllBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9B2AC433, version = 150)
    public int sceNandTestBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x716CD2B2, version = 150)
    public int sceNandWriteBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEF55F193, version = 150)
    public int sceNandCalcEcc() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x18B78661, version = 150)
    public int sceNandVerifyEcc() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x88CC9F72, version = 150)
    public int sceNandCorrectEcc() {
    	return 0;
    }
}
