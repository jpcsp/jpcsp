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
package jpcsp.media.codec.util;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Arrays;

import jpcsp.media.codec.atrac3plus.Atrac3plusDecoder;

import org.apache.log4j.Logger;

public class VLC {
	private static Logger log = Atrac3plusDecoder.log;
	public int bits;
	public int[][] table;
	public int tableSize;
	public int tableAllocated;

	private static class VLCcode implements Comparable<VLCcode> {
		int bits;
		int symbol;
		int code;

		@Override
		public int compareTo(VLCcode o) {
			return (this.code >>> 1) - (o.code >>> 1);
		}
	}

	public int initVLCSparse(int[] bits, int[] codes, int[] symbols) {
		return initVLCSparse(bits.length, codes.length, bits, codes, symbols);
	}

	public int initVLCSparse(int nbBits, int nbCodes, int[] bits, int[] codes, int[] symbols) {
		VLCcode buf[] = new VLCcode[nbCodes + 1];

		this.bits = nbBits;

		int j = 0;
		for (int i = 0; i < nbCodes; i++) {
			buf[j] = new VLCcode();
			buf[j].bits = bits[i];
			if (!(buf[j].bits > nbBits)) {
				continue;
			}
			if (buf[j].bits > 3 * nbBits || buf[j].bits > 32) {
				log.error(String.format("Too long VLC (%d) in initVLC", buf[j].bits));
				return -1;
			}
			buf[j].code = codes[i];
			if (buf[j].code >= (1 << buf[j].bits)) {
				log.error(String.format("Invalid code in initVLC"));
				return -1;
			}
			buf[j].code <<= 32 - buf[j].bits;
			if (symbols != null) {
				buf[j].symbol = symbols[i];
			} else {
				buf[j].symbol = i;
			}
			j++;
		}

		Arrays.sort(buf, 0, j);

		for (int i = 0; i < nbCodes; i++) {
			buf[j] = new VLCcode();
			buf[j].bits = bits[i];
			if (!(buf[j].bits != 0 && buf[j].bits <= nbBits)) {
				continue;
			}
			buf[j].code = codes[i];
			buf[j].code <<= 32 - buf[j].bits;
			if (symbols != null) {
				buf[j].symbol = symbols[i];
			} else {
				buf[j].symbol = i;
			}
			j++;
		}

		nbCodes = j;

		return buildTable(nbBits, nbCodes, buf, 0);
	}

	private int buildTable(int tableNbBits, int nbCodes, VLCcode[] codes, int codeOffset) {
		int tableSize = 1 << tableNbBits;
		if (tableNbBits > 30) {
			return -1;
		}

		int tableIndex = allocTable(tableSize);
		if (tableIndex < 0) {
			return tableIndex;
		}

		// first pass: map codes and compute auxiliary table sizes
		for (int i = 0; i < nbCodes; i++) {
			int n      = codes[codeOffset + i].bits;
			int code   = codes[codeOffset + i].code;
			int symbol = codes[codeOffset + i].symbol;
			if (n <= tableNbBits) {
				// no need to add another table
				int j = code >>> (32 - tableNbBits);
				int nb = 1 << (tableNbBits - n);
				int inc = 1;
				for (int k = 0; k < nb; k++) {
					int bits = table[tableIndex + j][1];
					if (bits != 0 && bits != n) {
						log.error(String.format("incorrect codes"));
						return -1;
					}
					table[tableIndex + j][1] = n; //bits
					table[tableIndex + j][0] = symbol;
					j += inc;
				}
			} else {
				// fill auxiliary table recursively
				n -= tableNbBits;
				int codePrefix = code >>> (32 - tableNbBits);
				int subtableBits = n;
				codes[codeOffset + i].bits = n;
				codes[codeOffset + i].code = code << tableNbBits;
				int k;
				for (k = i + 1; k < nbCodes; k++) {
					n = codes[codeOffset + k].bits - tableNbBits;
					if (n <= 0) {
						break;
					}
					code = codes[codeOffset + k].code;
					if ((code >>> (32 - tableNbBits)) != codePrefix) {
						break;
					}
					codes[codeOffset + k].bits = n;
					codes[codeOffset + k].code = code << tableNbBits;
					subtableBits = max(subtableBits, n);
				}
				subtableBits = min(subtableBits, tableNbBits);
				int j = codePrefix;
				table[tableIndex + j][1] = -subtableBits;
				int index = buildTable(subtableBits, k - i, codes, codeOffset + i);
				if (index < 0) {
					return index;
				}
				table[tableIndex + j][0] = index; //code
				i = k - 1;
			}
		}

		for (int i = 0; i < tableSize; i++) {
			if (table[tableIndex + i][1] == 0) { //bits
				table[tableIndex + i][0] = -1; //codes
			}
		}

		return tableIndex;
	}

	private int allocTable(int size) {
		int index = tableSize;

		tableSize += size;
		tableAllocated = tableSize;
		int[][] newTable = new int[tableAllocated][2];
		if (table != null) {
			for (int i = 0; i < index; i++) {
				newTable[i][0] = table[i][0];
				newTable[i][1] = table[i][1];
			}
		}
		table = newTable;

		return index;
	}

	/**
	 * Parse a vlc code.
	 * @param bits is the number of bits which will be read at once, must be
	 *             identical to nb_bits in init_vlc()
	 * @param maxDepth is the number of times bits bits must be read to completely
	 *                 read the longest vlc code
	 *                 = (max_vlc_length + bits - 1) / bits
	 */
	public int getVLC2(IBitReader br, int maxDepth) {
		int nbBits;
		int index = br.peek(bits);
		int code = table[index][0];
		int n = table[index][1];

		if (maxDepth > 1 && n < 0) {
			br.skip(bits);

			nbBits = -n;

			index = br.peek(nbBits) + code;
			code = table[index][0];
			n = table[index][1];
			if (maxDepth > 2 && n < 0) {
				br.skip(nbBits);

				nbBits = -n;

				index = br.peek(nbBits) + code;
				code = table[index][0];
				n = table[index][1];
			}
		}
		br.skip(n);

		return code;
	}

	public int getVLC2(IBitReader br) {
		return getVLC2(br, 1);
	}
}
