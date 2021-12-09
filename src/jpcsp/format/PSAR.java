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
package jpcsp.format;

import static java.lang.System.arraycopy;
import static jpcsp.HLE.Modules.semaphoreModule;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_DECRYPT;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.read8;
import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.readStringZ;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;
import jpcsp.crypto.CryptoEngine;
import jpcsp.hardware.Model;
import jpcsp.util.Utilities;

/**
 * Decrypt PSAR files.
 *
 * Based on https://github.com/John-K/pspdecrypt/blob/master/PsarDecrypter.cpp
 *
 */
public class PSAR {
	public static final Logger log = Logger.getLogger("psar");
	public static final int PSAR_MAGIC = 0x52415350; // "PSAR"
	private boolean decrypted;
	private boolean oldschool;
	private int overhead;
	private int iBase;
	private int psarPosition = 0;
	private int psarVersion;
	private int tableMode;
	private static final int SIZE_A = 0x110; // size of uncompressed file entry = 272 bytes
	private Map<String, String> tableFileNames = new HashMap<String, String>();
	private CryptoEngine crypto = new CryptoEngine();
	private byte[] buffer;
	private int offset;
	private int size;
	private final byte[] dataOut = new byte[3000000];
	private final byte[] dataOut2 = new byte[3000000];

	private static class PSARFileInfo {
		public String shortFileName;
		public final String realFileNames[] = new String[13];
		public int size;
		public int pos;
		public boolean signCheck;

		public String getFirstRealFileName() {
			for (int i = 0; i < realFileNames.length; i++) {
				if (realFileNames[i] != null) {
					return realFileNames[i];
				}
			}

			return shortFileName;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(String.format("shortName='%s', ", shortFileName));
			for (int i = 0; i < realFileNames.length; i++) {
				if (realFileNames[i] != null) {
					s.append(String.format("name[%d]='%s', ", i, realFileNames[i]));
				}
			}
			s.append(String.format("size=0x%X, pos=0x%X, signCheck=%b", size, pos, signCheck));
			return s.toString();
		}
	}

	private static class TableKey {
		final public int mklow;
		final public int mkhigh;
		final public byte[] key_S;

		public TableKey(int mklow, int mkhigh, int[] key_S) {
			this.mklow = mklow;
			this.mkhigh = mkhigh;
			this.key_S = new byte[key_S.length];
			for (int i = 0; i < key_S.length; i++) {
				this.key_S[i] = (byte) key_S[i];
			}
		}
	}

	private static final int key_C[] = {
		0x07, 0x0F, 0x17, 0x1F, 0x27, 0x2F, 0x37, 0x3F, 0x06, 0x0E, 0x16, 0x1E, 0x26, 0x2E, 0x36, 0x3E, 
		0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D, 0x35, 0x3D, 0x04, 0x0C, 0x14, 0x1C, 0x01, 0x09, 0x11, 0x19, 
		0x21, 0x29, 0x31, 0x39, 0x02, 0x0A, 0x12, 0x1A, 0x22, 0x2A, 0x32, 0x3A, 0x03, 0x0B, 0x13, 0x1B, 
		0x23, 0x2B, 0x33, 0x3B, 0x24, 0x2C, 0x34, 0x3C
	};

	private static final int key_Z[] = {
		0x32, 0x2F, 0x35, 0x28, 0x3F, 0x3B, 0x3D, 0x24, 0x31, 0x3A, 0x2B, 0x36, 0x29, 0x2D, 0x34, 0x3C, 
		0x26, 0x38, 0x30, 0x39, 0x25, 0x2C, 0x33, 0x3E, 0x17, 0x0C, 0x21, 0x1B, 0x11, 0x09, 0x22, 0x18, 
		0x0D, 0x13, 0x1F, 0x10, 0x14, 0x0F, 0x19, 0x08, 0x1E, 0x0B, 0x12, 0x16, 0x0E, 0x1C, 0x23, 0x20
	};

	private static final int key_M[] = {
		0x0D, 0x01, 0x02, 0x0F, 0x08, 0x0D, 0x04, 0x08, 0x06, 0x0A, 0x0F, 0x03, 0x0B, 0x07, 0x01, 0x04, 
		0x0A, 0x0C, 0x09, 0x05, 0x03, 0x06, 0x0E, 0x0B, 0x05, 0x00, 0x00, 0x0E, 0x0C, 0x09, 0x07, 0x02, 
		0x07, 0x02, 0x0B, 0x01, 0x04, 0x0E, 0x01, 0x07, 0x09, 0x04, 0x0C, 0x0A, 0x0E, 0x08, 0x02, 0x0D, 
		0x00, 0x0F, 0x06, 0x0C, 0x0A, 0x09, 0x0D, 0x00, 0x0F, 0x03, 0x03, 0x05, 0x05, 0x06, 0x08, 0x0B, 
		0x04, 0x0D, 0x0B, 0x00, 0x02, 0x0B, 0x0E, 0x07, 0x0F, 0x04, 0x00, 0x09, 0x08, 0x01, 0x0D, 0x0A, 
		0x03, 0x0E, 0x0C, 0x03, 0x09, 0x05, 0x07, 0x0C, 0x05, 0x02, 0x0A, 0x0F, 0x06, 0x08, 0x01, 0x06, 
		0x01, 0x06, 0x04, 0x0B, 0x0B, 0x0D, 0x0D, 0x08, 0x0C, 0x01, 0x03, 0x04, 0x07, 0x0A, 0x0E, 0x07, 
		0x0A, 0x09, 0x0F, 0x05, 0x06, 0x00, 0x08, 0x0F, 0x00, 0x0E, 0x05, 0x02, 0x09, 0x03, 0x02, 0x0C, 
		0x0C, 0x0A, 0x01, 0x0F, 0x0A, 0x04, 0x0F, 0x02, 0x09, 0x07, 0x02, 0x0C, 0x06, 0x09, 0x08, 0x05, 
		0x00, 0x06, 0x0D, 0x01, 0x03, 0x0D, 0x04, 0x0E, 0x0E, 0x00, 0x07, 0x0B, 0x05, 0x03, 0x0B, 0x08, 
		0x09, 0x04, 0x0E, 0x03, 0x0F, 0x02, 0x05, 0x0C, 0x02, 0x09, 0x08, 0x05, 0x0C, 0x0F, 0x03, 0x0A, 
		0x07, 0x0B, 0x00, 0x0E, 0x04, 0x01, 0x0A, 0x07, 0x01, 0x06, 0x0D, 0x00, 0x0B, 0x08, 0x06, 0x0D, 
		0x02, 0x0E, 0x0C, 0x0B, 0x04, 0x02, 0x01, 0x0C, 0x07, 0x04, 0x0A, 0x07, 0x0B, 0x0D, 0x06, 0x01, 
		0x08, 0x05, 0x05, 0x00, 0x03, 0x0F, 0x0F, 0x0A, 0x0D, 0x03, 0x00, 0x09, 0x0E, 0x08, 0x09, 0x06, 
		0x04, 0x0B, 0x02, 0x08, 0x01, 0x0C, 0x0B, 0x07, 0x0A, 0x01, 0x0D, 0x0E, 0x07, 0x02, 0x08, 0x0D, 
		0x0F, 0x06, 0x09, 0x0F, 0x0C, 0x00, 0x05, 0x09, 0x06, 0x0A, 0x03, 0x04, 0x00, 0x05, 0x0E, 0x03, 
		0x07, 0x0D, 0x0D, 0x08, 0x0E, 0x0B, 0x03, 0x05, 0x00, 0x06, 0x06, 0x0F, 0x09, 0x00, 0x0A, 0x03, 
		0x01, 0x04, 0x02, 0x07, 0x08, 0x02, 0x05, 0x0C, 0x0B, 0x01, 0x0C, 0x0A, 0x04, 0x0E, 0x0F, 0x09, 
		0x0A, 0x03, 0x06, 0x0F, 0x09, 0x00, 0x00, 0x06, 0x0C, 0x0A, 0x0B, 0x01, 0x07, 0x0D, 0x0D, 0x08, 
		0x0F, 0x09, 0x01, 0x04, 0x03, 0x05, 0x0E, 0x0B, 0x05, 0x0C, 0x02, 0x07, 0x08, 0x02, 0x04, 0x0E, 
		0x0A, 0x0D, 0x00, 0x07, 0x09, 0x00, 0x0E, 0x09, 0x06, 0x03, 0x03, 0x04, 0x0F, 0x06, 0x05, 0x0A, 
		0x01, 0x02, 0x0D, 0x08, 0x0C, 0x05, 0x07, 0x0E, 0x0B, 0x0C, 0x04, 0x0B, 0x02, 0x0F, 0x08, 0x01, 
		0x0D, 0x01, 0x06, 0x0A, 0x04, 0x0D, 0x09, 0x00, 0x08, 0x06, 0x0F, 0x09, 0x03, 0x08, 0x00, 0x07, 
		0x0B, 0x04, 0x01, 0x0F, 0x02, 0x0E, 0x0C, 0x03, 0x05, 0x0B, 0x0A, 0x05, 0x0E, 0x02, 0x07, 0x0C, 
		0x0F, 0x03, 0x01, 0x0D, 0x08, 0x04, 0x0E, 0x07, 0x06, 0x0F, 0x0B, 0x02, 0x03, 0x08, 0x04, 0x0E, 
		0x09, 0x0C, 0x07, 0x00, 0x02, 0x01, 0x0D, 0x0A, 0x0C, 0x06, 0x00, 0x09, 0x05, 0x0B, 0x0A, 0x05, 
		0x00, 0x0D, 0x0E, 0x08, 0x07, 0x0A, 0x0B, 0x01, 0x0A, 0x03, 0x04, 0x0F, 0x0D, 0x04, 0x01, 0x02, 
		0x05, 0x0B, 0x08, 0x06, 0x0C, 0x07, 0x06, 0x0C, 0x09, 0x00, 0x03, 0x05, 0x02, 0x0E, 0x0F, 0x09, 
		0x0E, 0x00, 0x04, 0x0F, 0x0D, 0x07, 0x01, 0x04, 0x02, 0x0E, 0x0F, 0x02, 0x0B, 0x0D, 0x08, 0x01, 
		0x03, 0x0A, 0x0A, 0x06, 0x06, 0x0C, 0x0C, 0x0B, 0x05, 0x09, 0x09, 0x05, 0x00, 0x03, 0x07, 0x08, 
		0x04, 0x0F, 0x01, 0x0C, 0x0E, 0x08, 0x08, 0x02, 0x0D, 0x04, 0x06, 0x09, 0x02, 0x01, 0x0B, 0x07, 
		0x0F, 0x05, 0x0C, 0x0B, 0x09, 0x03, 0x07, 0x0E, 0x03, 0x0A, 0x0A, 0x00, 0x05, 0x06, 0x00, 0x0D
	};

	private static final int key_S[] = {
		0x9E, 0xA4, 0x33, 0x81, 0x86, 0x0C, 0x52, 0x85
	};

	private static final int key_S2[] = {
		0xB2, 0xFE, 0xD9, 0x79, 0x8A, 0x02, 0xB1, 0x87
	};

	private static final int key_S3[] = {
		0x81, 0x08, 0xC1, 0xF2, 0x35, 0x98, 0x69, 0xB0 
	};

	private static final int key_S4[] = {
		0x6D, 0x52, 0x1B, 0xA3, 0xC2, 0x36, 0xF9, 0x2B
	};

	private static final int key_S5[] = {
		0xDB, 0x4E, 0x79, 0x41, 0xF5, 0x97, 0x30, 0xAD
	};

	private static final int key_S6[] = {
		0xA6, 0x83, 0x0C, 0x2F, 0x63, 0x0B, 0x96, 0x29
	};

	private static final int table_40[] = {
		0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 
		0x00, 0x00, 0x00, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x01, 0x00, 0x00, 0x00, 
		0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 
		0x00, 0x00, 0x00, 0x20, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x01, 
		0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x40, 0x00, 0x00, 0x10, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, 
		0x00, 0x00, 0x00, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 
		0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 
		0x02, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00
	};

	private static final TableKey[] tableKeys = new TableKey[] {
		new TableKey(0xB730E5C7, 0x95620B49, key_S),
		new TableKey(0x45C9DC95, 0x5A7B3D9D, key_S2),
		new TableKey(0x6F20585A, 0x4CCE495B, key_S3),
		new TableKey(0x620BF15A, 0x73F45262, key_S4),
		new TableKey(0xFD9D4498, 0xA664C8F8, key_S5),
		new TableKey(0x3D6426E7, 0xD7BD7481, key_S6)
	};

	public PSAR(byte[] buffer, int offset, int size) {
		this.buffer = buffer;
		this.offset = offset;
		this.size = size;
	}

	public int readHeader() {
		int result = pspPSARInit(buffer, offset, dataOut, dataOut2);
		if (result < 0) {
			log.error(String.format("pspPSARInit failed with error 0x%08X", result));
			return result;
		}

		String s = readStringZ(dataOut, 0x10);
		String version = s.substring(s.lastIndexOf(',') + 1);

		tableMode = 0;
		if (version.startsWith("3.8") || version.startsWith("3.9")) {
			tableMode = 1;
		} else if (version.startsWith("4.")) {
			tableMode = 2;
		} else if (version.startsWith("5.")) {
			tableMode = 3;
		} else if (version.startsWith("6.")) {
			tableMode = 4;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Version %s, tableMode=%d", version, tableMode));
		}

		return 0;
	}

	private static int read32(int[] a, int offset) {
		return a[offset] | (a[offset + 1] << 8) | (a[offset + 2] << 16) | (a[offset + 3] << 24);
	}

	private void demangle(byte[] pIn, int pInOffset, byte[] pOut, int pOutOffset) {
		final int headerSize = 20;
		final int dataSize = 0x130;
		final int totalSize = headerSize + dataSize;
		byte[] buffer = new byte[totalSize];

		arraycopy(pIn, pInOffset, buffer, headerSize, dataSize);
		writeUnaligned32(buffer, 0, 5);
		writeUnaligned32(buffer, 4, 0);
		writeUnaligned32(buffer, 8, 0);
		writeUnaligned32(buffer, 12, 0x55);
		writeUnaligned32(buffer, 16, dataSize);

		int result = semaphoreModule.hleUtilsBufferCopyWithRange(buffer, dataSize, buffer, totalSize, PSP_KIRK_CMD_DECRYPT);
		if (result != 0) {
			log.error(String.format("demangle error 0x%X", result));
		}

		arraycopy(buffer, 0, pOut, pOutOffset, dataSize);
	}

	private int decodeBlock(byte[] pIn, int pInOffset, int cbIn, byte[] pOut, int pOutOffset) {
		if (decrypted) {
			arraycopy(pIn, pInOffset, pOut, pOutOffset, cbIn);
			return cbIn;
		}

		arraycopy(pIn, pInOffset, pOut, pOutOffset, cbIn + 0x10);

		int ret = 0;
		int cbOut = 0;

		if (!oldschool) {
			demangle(pIn, pInOffset + 0x20, pOut, pOutOffset + 0x20);
		}

		int tag = readUnaligned32(pOut, pOutOffset + 0xD0);
		if (tag == 0x0E000000) {
			TPointer32 cbOutAddr = Utilities.allocatePointer32(4);
			ret = Modules.sceMesgdModule.hleMesgd_driver_102DC8AF(pOut, pOutOffset, cbIn, cbOutAddr);
			cbOut = cbOutAddr.getValue();
		} else if (tag == 0x06000000) {
			TPointer32 cbOutAddr = Utilities.allocatePointer32(4);
			ret = Modules.sceNwmanModule.hleNwman_driver_9555D68D(pOut, pOutOffset, cbIn, cbOutAddr);
			cbOut = cbOutAddr.getValue();
		} else {
			log.error("Unknown PSAR tag");
			return -4;
		}

		if (ret != 0) {
			return ret;
		}

		return cbOut;
	}

	private int pspPSARInit(byte[] dataPSAR, int dataPSAROffset, byte[] dataOut, byte[] dataOut2) {
		if (readUnaligned32(dataPSAR, dataPSAROffset) != PSAR_MAGIC) {
			return -1;
		}

		decrypted = readUnaligned32(dataPSAR, dataPSAROffset + 0x20) == 0x2C333333;
		overhead = decrypted ? 0x0 : 0x150;
		psarVersion = read8(dataPSAR, dataPSAROffset + 4);
		oldschool = psarVersion == 1;

		int cbOut = decodeBlock(dataPSAR, dataPSAROffset + 0x10, overhead + SIZE_A, dataOut, 0);
		if (cbOut <= 0) {
			return cbOut;
		}

		if (cbOut != SIZE_A) {
			return -2;
		}

		// iBase points to the next block to decode (0x10 aligned)
		iBase = 0x10 + overhead + SIZE_A; // After first entry

		if (decrypted) {
			cbOut = decodeBlock(dataPSAR, dataPSAROffset + iBase, readUnaligned32(dataOut, 0x90), dataOut2, 0);
			if (cbOut <= 0) {
				return -3;
			}

			iBase += overhead + cbOut;
			return 0;
		}

		if (!oldschool) {
			// Second block
			cbOut = decodeBlock(dataPSAR, dataPSAROffset + iBase, overhead + 100, dataOut2, 0);
			if (cbOut <= 0) {
				// Version 2.7 is bigger
				cbOut = decodeBlock(dataPSAR, dataPSAROffset + iBase, overhead + 144, dataOut2, 0);
				if (cbOut <= 0) {
					cbOut = decodeBlock(dataPSAR, dataPSAROffset + iBase, overhead + readUnaligned16(dataOut, 0x90), dataOut2, 0);
					if (cbOut <= 0) {
						return -4;
					}
				}
			}
			int cbChunk = alignUp(cbOut, 15);
			iBase += overhead + cbChunk;
		}

		psarPosition = 0;

		return 0;
	}

	private int pspPSARGetNextFile(byte[] dataPSAR, int dataPSAROffset, int cbFile, byte[] dataOut, byte[] dataOut2, PSARFileInfo fileInfo) {
		if (iBase >= (cbFile - overhead)) {
			// No more files
			return 0;
		}

		int cbOut = decodeBlock(dataPSAR, dataPSAROffset + iBase - psarPosition, overhead + SIZE_A, dataOut, 0);
		if (cbOut <= 0) {
			return -1;
		}

		if (cbOut != SIZE_A) {
			return -1;
		}

		fileInfo.shortFileName = readStringNZ(dataOut, 4, 252);
		fileInfo.signCheck = read8(dataOut, 0x10F) == 2;

		if (readUnaligned32(dataOut, 0x100) != 0) {
			return -1;
		}

		iBase += overhead + SIZE_A;
		int cbDataChunk = readUnaligned32(dataOut, 0x104);
		int cbExpanded = readUnaligned32(dataOut, 0x108);
		if (cbExpanded == 0) {
			// Directory
			fileInfo.size = 0;
		} else {
			cbOut = decodeBlock(dataPSAR, dataPSAROffset + iBase - psarPosition, cbDataChunk, dataOut, 0);
			if (cbOut > 10 && read8(dataOut, 0) == 0x78 && read8(dataOut, 1) == 0x9C) {
				// Standard Deflate header
				Inflater inflater = new Inflater();
				inflater.setInput(dataOut, 0, cbDataChunk);
				try {
					int result = inflater.inflate(dataOut2, 0, cbExpanded);
					inflater.end();
					if (result == cbExpanded) {
						fileInfo.size = result;
					} else {
						return -1;
					}
				} catch (DataFormatException e) {
					inflater.end();
					log.error("pspPSARGetNextFile", e);
					return -1;
				}
			} else {
				iBase -= overhead + SIZE_A;
				return -1;
			}
		}

		iBase += cbDataChunk;
		fileInfo.pos = iBase;

		// File returned
		return 1;
	}

	private void generateSeed(byte[] out, int c1, int c2) {
		int bit_insert = 0x80000000;
		int r1 = 0;
		int r2 = 0;
		int base = 0;

		for (int i = 0; i < 0x38; i++) {
			int val1 = key_C[i];
			int val2;

			if ((val1 & 0x20) != 0) {
				val2 = (1 << val1);
				val1 = 0;			
			} else {
				val1 = (1 << val1);
				val2 = 0;
			}		

			val1 &= c1;
			val2 &= c2;

			if ((val1 | val2) != 0) {
				val1 = base;
				val2 = bit_insert;
			} else {
				val1 = 0;
				val2 = 0;
			}

			r1 |= val1;
			r2 |= val2;
			base >>>= 1;
			base = (base & 0x7FFFFFFF) | ((bit_insert & 1) << 31);
			bit_insert >>>= 1;
		}

		int wpar1 = (r2 >>> 4);
		int wpar2 = (r1 >>> 8) & 0x00FFFFFF;
		wpar2 = (wpar2  & 0xF0FFFFFF) | ((r2 & 0xF) << 24);

		int outOffset = 0;
		for (int i = 0x10; i != 0; i--) {
			r1 = 0x7efc;
			int val1 = (wpar1 << 4);
			r1 >>= i;
			int val2 = (wpar2 << 4);
			r1 &= 0x1;
			int shr = (r1 ^ 0x1F);
			r1++;
			val1 >>>= shr;
			val2 >>>= shr;
			wpar1 <<= r1;
			wpar2 <<= r1;
			wpar1 |= val1;
			wpar2 |= val2;
			wpar1 &= 0x0FFFFFFF;
			wpar2 &= 0x0FFFFFFF;
			c2 = (wpar2 >>> 24);
			c2 = (c2 & 0xF) | ((wpar1 & 0x0FFFFFFF) << 4);
			c1 = (wpar2 << 8);

			base = r1 = r2 = 0;
			bit_insert = 0x80000000;
			
			for (int j = 0; j < 0x30; j++) {
				val1 = key_Z[j];

				if ((val1 & 0x20) != 0) {
					val2 = (1 << val1);
					val1 = 0;				
				} else {
					val1 = (1 << val1);
					val2 = 0;
				}

				val1 &= c1;
				val2 &= c2;
				
				if ((val1 |val2) != 0) {
					val1 = base;
					val2 = bit_insert;
				} else {
					val1 = 0;
					val2 = 0;
				}

				r1 |= val1;
				r2 |= val2;
				base >>>= 1;
				base = (base & 0x7FFFFFFF) | ((bit_insert & 1) << 31); 
				bit_insert >>>= 1;
			}

			writeUnaligned32(out, outOffset, r1);
			writeUnaligned32(out, outOffset + 4, r2);
			outOffset += 8;
		}
	}

	private static void sceInsanity1(int x1, int x2, int[] r) {
		int temp = ((x2 >> 4) ^ x1) & 0x0F0F0F0F;
		x2 = x2 ^ (temp << 4);
		x1 = x1 ^ temp;
		temp = ((x2 >> 16) ^ x1) & 0xFFFF;
		x1 = x1 ^ temp;
		x2 = x2 ^ (temp << 16);
		temp = ((x1 >> 2) ^ x2) & 0x33333333;
		x1 = x1 ^ (temp << 2);
		x2 = x2 ^ temp;
		temp = ((x1 >> 8) ^ x2) & 0x00FF00FF;
		x2 = (x2 ^ temp);
		x1 = x1 ^ (temp << 8);
		temp = ((x2 >> 1) ^ x1) & 0x55555555;
		r[1] = x2 ^ (temp << 1);
		r[0] = x1 ^ temp;
	}

	private static void sceInsanity2(int x1, int x2, int[] r) {
		int h1 = (x2 & 1);
		int h2 = (x2 >> 27) & 0x1F;
		int h3 = (x2 >> 23) & 0x3F;
		int h4 = (x2 >> 19) & 0x3F;
		r[1] = (x2 >> 15) & 0x3F;
		r[1] = (r[1] & 0xFF7FFFFF) | ((h1 & 1) << 23);
		r[1] = (r[1] & 0xFF83FFFF) | ((h2 & 0x1F) << 18);
		r[1] = (r[1] & 0xFFFC0FFF) | ((h3 & 0x3F) << 12);
		r[1] = (r[1] & 0xFFFFF03F) | ((h4 & 0x3F) << 6);
		h1 = (x2 >> 11) & 0x3F;
		h2 = (x2 >> 7) & 0x3F;
		h3 = (x2 >> 3) & 0x3F;
		h4 = (x2 & 0x1F);
		r[0] = (x2 >> 31) & 0x1;
		r[0] = (r[0] & 0xFF03FFFF) | ((h1 & 0x3F) << 18);
		r[0] = (r[0] & 0xFFFC0FFF) | ((h2 & 0x3F) << 12);
		r[0] = (r[0] & 0xFFFFF03F) | ((h3 & 0x3F) << 6);
		r[0] = (r[0] & 0xFFFFFFC1) | ((h4 & 0x1F) << 1);

		r[1] = ((r[1] << 8) | ((r[0] >> 16) & 0xFF));
		r[0] = (r[0] << 16);
	}

	private static void sceInsanity3(int x1, int x2, int[] r) {
		r[1] = 0;

		int offset = 0;
		int shifter = 0;
		for (int i = 0; i < 8; i++) {
			int val = key_M[offset + (x1 & 0x3F)];
			offset += 0x40;
			x1 = (x1 >>> 6);
			x1 = (x1 & 0x03FFFFFF) | ((x2 & 0x3F) << 26);
			x2 = (x2 >>> 6);
			r[1] |= (val << shifter);
			shifter += 4;
		}

		r[0] = 0;
	}

	private static void sceInsanity4(int x1, int x2, int[] r) {
		r[0] = 0;
		r[1] = 0;

		for (int i = 0; i < 0x20; i++) {
			if ((x2 & 0x1) != 0) {
				r[1] |= read32(table_40, i * 4);
			}		

			x2 >>>= 1;		
		}
	}

	private static void sceInsanity5(int x1, int x2, int[] r) {
		int temp = ((x2 >> 1) ^ x1) & 0x55555555;
		x1 = x1 ^ temp;
		x2 = x2 ^ (temp << 1);
		temp = ((x1 >> 8) ^ x2) & 0x00FF00FF;
		x1 = x1 ^ (temp << 8);
		x2 = x2 ^ temp;
		temp = ((x1 >> 2) ^ x2) & 0x33333333;
		x2 = x2 ^ temp;
		x1 = x1 ^ (temp << 2);
		temp = ((x2 >> 16) ^ x1) & 0xFFFF;
		x2 = x2 ^ (temp << 16);
		x1 = x1 ^ temp;
		temp = ((x2 >> 4) ^ x1) & 0x0F0F0F0F;
		r[0] = x1 ^ temp;
		r[1] = x2 ^ (temp << 4);
	}

	private void sceParanoia(byte[] buf, int[] p) {
		int x1 = p[0];
		int x2 = p[1];
		final int[] r = new int[2];
		int rot1, rot2, rot3, rot4, ro1, ro2, base;

		int bufOffset = 0x78;
		sceInsanity1(x1, x2, r);

		rot1 = 0;
		rot2 = 0;
		rot3 = r[0];
		rot4 = r[1];

		for (int i = 0; i < 0x10; i++) {
			sceInsanity2(rot1, rot3, r);

			ro1 = r[0];
			ro2 = r[1];
			r[0] = readUnaligned32(buf, bufOffset);
			r[1] = readUnaligned32(buf, bufOffset + 4);
			bufOffset -= 8;
			base = (ro2 ^ r[1]);
			x1 = (base << 16);

			sceInsanity3(((ro1 ^ r[0]) >>> 16) | x1, base >>> 16, r);
			sceInsanity4(r[0], r[1], r);

			x1 = (r[0] ^ rot2);
			x2 = (r[1] ^ rot4);
			rot2 = rot1;
			rot4 = rot3;
			rot1 = x1;
			rot3 = x2;
		}

		sceInsanity5(x1 | rot4, x2, p);
	}

	private void pspDecryptT(byte[] buf, int size, int mode) {
		final byte[] m1 = new byte[0x400];
		final byte[] m2 = new byte[8];

		generateSeed(m1, tableKeys[mode].mklow, tableKeys[mode].mkhigh);

		System.arraycopy(tableKeys[mode].key_S, 0, m1, 0x80, 8);

		final int[] p = new int[2];
		int bufOffset = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < 8; j++) {
				m2[7 - j] = buf[bufOffset + j];
			}

			p[0] = Utilities.readUnaligned32(m2, 0);
			p[1] = Utilities.readUnaligned32(m2, 4);
			sceParanoia(m1, p);
			writeUnaligned32(m2, 0, p[0]);
			writeUnaligned32(m2, 4, p[1]);

			for (int j = 0; j < 8; j++) {
				m1[0x90 + j] = (byte) (m2[7 - j] ^ m1[0x80 + j]);
				m1[0x80 + j] = buf[bufOffset + j];
			}

			System.arraycopy(m1, 0x90, buf, bufOffset, 8);
			bufOffset += 8;
		}
	}

	private int pspDecryptTable(byte[] buf, int size, int mode) {
		pspDecryptT(buf, size >> 3, mode);

		int tag = readUnaligned32(buf, 0xD0);
		int type = 2;
		switch (tag) {
			case 0x0E000000:
				type = 0;
				break;
			case 0xD82310F0:
			case 0xD8231EF0:
				type = 2;
				break;
			default:
				log.error(String.format("pspDecryptTable tag=0x%08X", tag));
				break;
		}
		int retSize = crypto.getPRXEngine().DecryptPRX(buf, size, type, null, null);
		if (retSize < 0) {
			TPointer32 resultSizeAddr = Utilities.allocatePointer32(4);
			int res = Modules.sceMesgdModule.hleMesgd_driver_102DC8AF(buf, 0, size, resultSizeAddr);
			if (res < 0) {
				retSize = -1;
			}
		}

		return retSize;
	}

	private static String getKey(int tableIndex, String shortFileName) {
		return String.format("%d_%s", tableIndex, shortFileName);
	}

	private void decryptTable(byte[] buf, int size, int tableMode, int tableIndex) {
		int tableSize = pspDecryptTable(buf, size, tableMode);
		if (tableSize <= 0) {
			log.error(String.format("Cannot decrypt %dg table, error 0x%08X", tableIndex, tableSize));
		} else {
			String tableString = new String(buf, 0, tableSize);
			String tableStrings[] = tableString.split("\r\n");
			for (String line : tableStrings) {
				String elements[] = line.split(",", 2);
				String shortFileName = elements[0];
				String realFileName = elements[1];
				tableFileNames.put(getKey(tableIndex, shortFileName), realFileName);
			}
		}
	}

	private void findRealFileName(PSARFileInfo fileInfo) {
		for (int i = 1; i <= 12; i++) {
			String realFileName = tableFileNames.get(getKey(i, fileInfo.shortFileName));
			if (realFileName != null) {
				fileInfo.realFileNames[i] = realFileName;
			}
		}
	}

	public int extractFile(byte[] outBuffer, int outOffset, String fileName) {
		int resultSize = -1;

		int fileVersion = Model.getGeneration();
		while (true) {
			PSARFileInfo fileInfo = new PSARFileInfo();
			int result = pspPSARGetNextFile(buffer, offset, size, dataOut, dataOut2, fileInfo);
			if (result < 0) {
				continue;
			}
			if (result == 0) {
				// No more files
				break;
			}

			if (fileInfo.shortFileName.matches("000(0[1-9]|1[1-2])")) {
				decryptTable(dataOut2, fileInfo.size, tableMode, Integer.parseInt(fileInfo.shortFileName));
			} else {
				findRealFileName(fileInfo);
			}

			String realFileName;
			if (fileVersion < 0) {
				realFileName = fileInfo.getFirstRealFileName();
			} else {
				realFileName = fileInfo.realFileNames[fileVersion];
			}

			if (realFileName == null) {
				realFileName = fileInfo.shortFileName;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("PSAR found file '%s'", realFileName));
			}

			if (fileName.equals(realFileName)) {
				System.arraycopy(dataOut2, 0, outBuffer, outOffset, fileInfo.size);
				resultSize = fileInfo.size;
				break;
			}
		}

		return resultSize;
	}
}
