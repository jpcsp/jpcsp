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
package jpcsp.filesystems.umdiso;

import static jpcsp.util.Tlzrc.lzrc_decompress;
import static jpcsp.util.Utilities.endianSwap32;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import jpcsp.crypto.AMCTRL;
import jpcsp.crypto.AMCTRL.BBCipher_Ctx;
import jpcsp.crypto.AMCTRL.BBMac_Ctx;
import jpcsp.crypto.CryptoEngine;
import jpcsp.util.Utilities;

public class PBPFileSectorDevice extends AbstractFileSectorDevice {
	private int lbaSize;
	private int psarDataOffset;
	private int blockSize;
	private int blockLBAs;
	private int numBlocks;
	private int numSectors;
	private byte[] vkey;
	private byte[] hkey = new byte[16];
	private TableInfo[] table;
	private int currentBlock;
	private AMCTRL amctrl;
	private byte[] blockBuffer;
	private byte[] tempBuffer;

	private static class TableInfo {
		public static final int FLAG_IS_UNCRYPTED = 4;
		public byte[] mac = new byte[16];
		public int offset;
		public int size;
		public int flags;
		public int unknown;
	}

	public PBPFileSectorDevice(RandomAccessFile fileAccess) {
		super(fileAccess);

		try {
			fileAccess.seek(0x24);
			psarDataOffset = endianSwap32(fileAccess.readInt());
			fileAccess.seek(psarDataOffset);
			byte[] header = new byte[256];
			int readSize = fileAccess.read(header);
			if (readSize != header.length) {
				if (psarDataOffset == fileAccess.length()) {
					throw new IOException("This is an homebrew PBP");
				}
				throw new IOException(String.format("Invalid PBP header"));
			}

			CryptoEngine cryptoEngine = new CryptoEngine();
			amctrl = cryptoEngine.getAMCTRLEngine();

			BBMac_Ctx macContext = new BBMac_Ctx();
			BBCipher_Ctx cipherContext = new BBCipher_Ctx();

			// getKey
			amctrl.hleDrmBBMacInit(macContext, 3);
			amctrl.hleDrmBBMacUpdate(macContext, header, 0xC0);
			byte[] macKeyC0 = new byte[16];
			System.arraycopy(header, 0xC0, macKeyC0, 0, macKeyC0.length);
			vkey = amctrl.GetKeyFromBBMac(macContext, macKeyC0);

	        // decrypt NP header
	        byte[] cipherData = new byte[0x60];
	        System.arraycopy(header, 0x40, cipherData, 0, cipherData.length);
	        System.arraycopy(header, 0xA0, hkey, 0, hkey.length);
	        amctrl.hleDrmBBCipherInit(cipherContext, 1, 2, hkey, vkey);
	        amctrl.hleDrmBBCipherUpdate(cipherContext, cipherData, cipherData.length);
	        amctrl.hleDrmBBCipherFinal(cipherContext);

	        int lbaStart = Utilities.readUnaligned32(cipherData, 0x14);
	        int lbaEnd = Utilities.readUnaligned32(cipherData, 0x24);
	        numSectors = lbaEnd + 1;
	        lbaSize = numSectors - lbaStart;
	        blockLBAs = Utilities.readUnaligned32(header, 0x0C);
	        blockSize = blockLBAs * sectorLength;
	        numBlocks = (lbaSize + blockLBAs - 1) / blockLBAs;

	        blockBuffer = new byte[blockSize];
	        tempBuffer = new byte[blockSize];

	        table = new TableInfo[numBlocks];

	        int tableOffset = Utilities.readUnaligned32(cipherData, 0x2C);
	        fileAccess.seek(psarDataOffset + tableOffset);
	        byte[] tableBytes = new byte[numBlocks * 32];
	        readSize = fileAccess.read(tableBytes);
	        if (readSize != tableBytes.length) {
	        	log.error(String.format("Could not read table with size %d (readSize=%d)", tableBytes.length, readSize));
	        }

	        IntBuffer tableInts = ByteBuffer.wrap(tableBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
	        for (int i = 0; i < numBlocks; i++) {
	        	int p0 = tableInts.get();
	        	int p1 = tableInts.get();
	        	int p2 = tableInts.get();
	        	int p3 = tableInts.get();
	        	int p4 = tableInts.get();
	        	int p5 = tableInts.get();
	        	int p6 = tableInts.get();
	        	int p7 = tableInts.get();
	        	int k0 = p0 ^ p1;
	        	int k1 = p1 ^ p2;
	        	int k2 = p0 ^ p3;
	        	int k3 = p2 ^ p3;

	        	TableInfo tableInfo = new TableInfo();
	        	System.arraycopy(tableBytes, i * 32, tableInfo.mac, 0, tableInfo.mac.length);
	        	tableInfo.offset  = p4 ^ k3;
	        	tableInfo.size    = p5 ^ k1;
	        	tableInfo.flags   = p6 ^ k2;
	        	tableInfo.unknown = p7 ^ k0;
	        	table[i] = tableInfo;
	        }

	        currentBlock = -1;
		} catch (IOException e) {
			log.error("Reading PBP", e);
		}
	}

	@Override
	public int getNumSectors() {
		return numSectors;
	}

	@Override
	public void readSector(int sectorNumber, byte[] buffer, int offset) throws IOException {
		int lba = sectorNumber - currentBlock;
		if (currentBlock >= 0 && lba >= 0 && lba < blockLBAs) {
			System.arraycopy(blockBuffer, lba * sectorLength, buffer, offset, sectorLength);
		} else {
			int block = sectorNumber / blockLBAs;
			lba = sectorNumber % blockLBAs;
			currentBlock = block * blockLBAs;

			if (table[block].unknown == 0) {
				fileAccess.seek(psarDataOffset + table[block].offset);

				byte [] readBuffer;
				if (table[block].size < blockSize) {
					// For compressed blocks, decode into a temporary buffer
					readBuffer = tempBuffer;
				} else {
					readBuffer = blockBuffer;
				}

				int readSize = fileAccess.read(readBuffer, 0, table[block].size);
				if (readSize == table[block].size) {
					if ((table[block].flags & TableInfo.FLAG_IS_UNCRYPTED) == 0) {
						BBCipher_Ctx cipherContext = new BBCipher_Ctx();
						amctrl.hleDrmBBCipherInit(cipherContext, 1, 2, hkey, vkey, table[block].offset >> 4);
						amctrl.hleDrmBBCipherUpdate(cipherContext, readBuffer, table[block].size);
						amctrl.hleDrmBBCipherFinal(cipherContext);
					}

					// Compressed block?
					if (table[block].size < blockSize) {
						int lzsize = lzrc_decompress(blockBuffer, blockBuffer.length, readBuffer, table[block].size);
						if (lzsize != blockSize) {
							log.error(String.format("LZRC decompress error: decompressedSized=%d, should be %d", lzsize, blockSize));
						}
					}

					System.arraycopy(blockBuffer, lba * sectorLength, buffer, offset, sectorLength);
				}
			}
		}
	}
}
