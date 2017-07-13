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

    @Override
	public void start() {
		writeProtected = true;
		scramble = 0;

		dumpBlocks = readBytes("nand.block");
		dumpSpares = readBytes("nand.spare");
		dumpResults = readInts("nand.result");

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

    protected int hleNandReadPages(int ppn, TPointer user, TPointer spare, int len, boolean raw) {
    	if (user.isNotNull()) {
	    	if (dumpBlocks != null) {
	    		if (scramble != 0) {
	    			descramble(ppn, user, len, dumpBlocks, ppn * pageSize);
	    		} else {
	    			Utilities.writeBytes(user.getAddress(), len * pageSize, dumpBlocks, ppn * pageSize);
	    		}
	    	} else {
	    		user.clear(len * pageSize);
	    	}
    	}

    	if (spare.isNotNull()) {
	    	if (dumpSpares != null) {
	    		// It doesn't return the userEcc
	    		for (int i = 0; i < len; i++) {
	    			Utilities.writeBytes(spare.getAddress() + i * 12, 12, dumpSpares, (ppn + i) * 16);
	    		}
	    	} else {
	    		spare.clear(len * 12);
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
    	if (dumpSpares != null) {
    		Utilities.writeBytes(spare.getAddress(), len * 16, dumpSpares, ppn * 16);
    	} else {
	    	SceNandSpare sceNandSpare = new SceNandSpare();
	    	for (int i = 0; i < len; i++) {
	    		sceNandSpare.blockStat = 0xFF;
	    		sceNandSpare.lbn = 0xFFFF; // Use for bad blocks to point to an alternate block?
	    		sceNandSpare.id = 0x6DC64A38; // For IPL area
	    		//sceNandSpare.reserved2[0] = 0x80 | 0x60;
	    		sceNandSpare.write(spare, i * sceNandSpare.sizeof());
	    	}
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x89BDCA08, version = 150)
    public int sceNandReadPages(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE05AE88D, version = 150)
    public int sceNandReadPagesRawExtra(int ppn, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=pageSize, usage=Usage.out) TPointer user, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer spare, int len) {
    	return hleNandReadPages(ppn, user, spare, len, true);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC32EA051, version = 150)
    public int sceNandReadBlockWithRetry(int ppn, TPointer user, TPointer spare) {
    	return 0;
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
}
