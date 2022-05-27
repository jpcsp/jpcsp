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
import java.util.Arrays;

import jpcsp.crypto.AMCTRL;
import jpcsp.crypto.AMCTRL.BBCipher_Ctx;
import jpcsp.crypto.AMCTRL.BBMac_Ctx;
import jpcsp.crypto.CryptoEngine;
import jpcsp.util.Utilities;

public class PBPFileSectorDevice extends AbstractFileSectorDevice implements IBrowser {
	private int lbaSize;
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
	private int offsetParamSFO;
	private int offsetIcon0;
	private int offsetIcon1;
	private int offsetPic0;
	private int offsetPic1;
	private int offsetSnd0;
	private int offsetPspData;
	private int offsetPsarData;

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
			int magic      = endianSwap32(fileAccess.readInt());
			int version    = endianSwap32(fileAccess.readInt());
			offsetParamSFO = endianSwap32(fileAccess.readInt());
			offsetIcon0    = endianSwap32(fileAccess.readInt());
			offsetIcon1    = endianSwap32(fileAccess.readInt());
			offsetPic0     = endianSwap32(fileAccess.readInt());
			offsetPic1     = endianSwap32(fileAccess.readInt());
			offsetSnd0     = endianSwap32(fileAccess.readInt());
			offsetPspData  = endianSwap32(fileAccess.readInt());
			offsetPsarData = endianSwap32(fileAccess.readInt());
			if (magic != 0x50425000) {
				throw new IOException(String.format("Invalid PBP header 0x%08X", magic));
			}
			if (version != 0x00010000 && version != 0x00000100 && version != 0x00010001) {
				throw new IOException(String.format("Invalid PBP version 0x%08X", version));
			}
			fileAccess.seek(offsetPsarData);
			byte[] header = new byte[256];
			int readSize = fileAccess.read(header);
			if (readSize != header.length) {
				int psarDataLength = (int) (fileAccess.length() - offsetPsarData);
				if (psarDataLength != 0 && psarDataLength != 16) {
					throw new IOException(String.format("Invalid PBP header"));
				}
			} else if (header[0] == 'N' && header[1] == 'P' && header[2] == 'U' && header[3] == 'M' && header[4] == 'D' && header[5] == 'I' && header[6] == 'M' && header[7] == 'G') {
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
		        amctrl.hleDrmBBCipherInit(cipherContext, 1, 2, hkey, vkey, 0);
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
		        fileAccess.seek(offsetPsarData + tableOffset);
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
			}
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
		if (table == null) {
			Arrays.fill(buffer, offset, offset + sectorLength, (byte) 0);
		} else if (currentBlock >= 0 && lba >= 0 && lba < blockLBAs) {
			System.arraycopy(blockBuffer, lba * sectorLength, buffer, offset, sectorLength);
		} else {
			int block = sectorNumber / blockLBAs;
			lba = sectorNumber % blockLBAs;
			currentBlock = block * blockLBAs;

			if (table[block].unknown == 0) {
				fileAccess.seek(offsetPsarData + table[block].offset);

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

	private byte[] read(int offset, int length) throws IOException {
		if (length <= 0) {
			return null;
		}

		byte[] buffer = new byte[length];
		fileAccess.seek(offset & 0xFFFFFFFFL);
		int read = fileAccess.read(buffer);
		if (read < 0) {
			return null;
		}

		// Read less than expected?
		if (read < length) {
			// Shrink the buffer to the read size
			byte[] newBuffer = new byte[read];
			System.arraycopy(buffer, 0, newBuffer, 0, read);
			buffer = newBuffer;
		}

		return buffer;
	}

	@Override
	public byte[] readParamSFO() throws IOException {
		return read(offsetParamSFO, offsetIcon0 - offsetParamSFO);
	}

	@Override
	public byte[] readIcon0() throws IOException {
		return read(offsetIcon0, offsetIcon1 - offsetIcon0);
	}

	@Override
	public byte[] readIcon1() throws IOException {
		return read(offsetIcon1, offsetPic0 - offsetIcon1);
	}

	@Override
	public byte[] readPic0() throws IOException {
		return read(offsetPic0, offsetPic1 - offsetPic0);
	}

	@Override
	public byte[] readPic1() throws IOException {
		return read(offsetPic1, offsetSnd0 - offsetPic1);
	}

	@Override
	public byte[] readSnd0() throws IOException {
		return read(offsetSnd0, offsetPspData - offsetSnd0);
	}

	@Override
	public byte[] readPspData() throws IOException {
		return read(offsetPspData, offsetPsarData - offsetPspData);
	}

	@Override
	public byte[] readPsarData() throws IOException {
		return read(offsetPsarData, (int) (fileAccess.length() - offsetPsarData));
	}
}
