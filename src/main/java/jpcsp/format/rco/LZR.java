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
package jpcsp.format.rco;

import java.util.Arrays;

/**
 * LZR decompression
 * Based on libLZR Version 0.11 by BenHur - http://www.psp-programming.com/benhur
 * https://github.com/Grumbel/rfactortools/blob/master/other/quickbms/src/compression/libLZR.c
 */
public class LZR {
	private static int u8(byte b) {
		return b & 0xFF;
	}

	private static long u32(int i) {
		return i & 0xFFFFFFFFL;
	}

	private static class IntObject {
		private static final IntObject Null = new IntObject(0);
		private int value;

		public IntObject(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public int incr() {
			return value++;
		}

		public void sub(int sub) {
			value -= sub;
		}

		@Override
		public String toString() {
			return String.format("0x%08X", value);
		}
	}

	private static class BoolObject {
		private boolean value;

		public boolean getValue() {
			return value;
		}

		public void setValue(boolean value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return Boolean.toString(value);
		}
	}

	private static void fillBuffer(IntObject testMask, IntObject mask, IntObject buffer, byte[] in, IntObject nextIn) {
		// if necessary, fill up in buffer and shift mask
		if (testMask.getValue() >= 0 && testMask.getValue() <= 0x00FFFFFF) {
			buffer.setValue((buffer.getValue() << 8) + u8(in[nextIn.incr()]));
			mask.setValue(testMask.getValue() << 8);
		}
	}

	private static boolean nextBit(byte[] buf, int bufPtr1, IntObject number, IntObject testMask, IntObject mask, IntObject buffer, byte[] in, IntObject nextIn) {
		fillBuffer(testMask, mask, buffer, in, nextIn);
		int value = (mask.getValue() >>> 8) * u8(buf[bufPtr1]);
		if (testMask != mask) {
			testMask.setValue(value);
		}
		buf[bufPtr1] -= u8(buf[bufPtr1]) >> 3;
		number.setValue(number.getValue() << 1);
		if (u32(buffer.getValue()) < u32(value)) {
			mask.setValue(value);
			buf[bufPtr1] += 31;
			number.incr();
			return true;
		}

		buffer.sub(value);
		mask.sub(value);

		return false;
	}

	private static int getNumber(int nBits, byte[] buf, int bufPtr, int inc, BoolObject flag, IntObject mask, IntObject buffer, byte[] in, IntObject nextIn) {
		// Extract and return a number (consisting of n_bits bits) from in stream
		IntObject number = new IntObject(1);
		if (nBits >= 3) {
			nextBit(buf, bufPtr + 3 * inc, number, mask, mask, buffer, in, nextIn);
			if (nBits >= 4) {
				nextBit(buf, bufPtr + 3 * inc, number, mask, mask, buffer, in, nextIn);
				if (nBits >= 5) {
					fillBuffer(mask, mask, buffer, in, nextIn);
					for (; nBits >= 5; nBits--) {
						number.setValue(number.getValue() << 1);
						mask.setValue(mask.getValue() >>> 1);
						if (u32(buffer.getValue()) < u32(mask.getValue())) {
							number.incr();
						} else {
							buffer.sub(mask.getValue());
						}
					}
				}
			}
		}
		flag.setValue(nextBit(buf, bufPtr, number, mask, mask, buffer, in, nextIn));
		if (nBits >= 1) {
			nextBit(buf, bufPtr + inc, number, mask, mask, buffer, in, nextIn);
			if (nBits >= 2) {
				nextBit(buf, bufPtr + 2 * inc, number, mask, mask, buffer, in, nextIn);
			}
		}

		return number.getValue();
	}

	public static int decompress(byte[] out, int outCapacity, byte[] in) {
		int type = in[0];
		IntObject buffer = new IntObject((u8(in[1]) << 24) | (u8(in[2]) << 16) | (u8(in[3]) << 8) | u8(in[4]));

		IntObject nextIn = new IntObject(5);
		int nextOut = 0;
		int outEnd = outCapacity;

		if (type < 0) {
			// copy from stream without decompression
			int seqEnd = nextOut + buffer.getValue();
			if (seqEnd > outEnd) {
				return -1;
			}
			while (nextOut < seqEnd) {
				out[nextOut++] = in[nextIn.incr()];
			}
			return nextOut;
		}

		// Create and inti buffer
		byte buf[] = new byte[2800];
		Arrays.fill(buf, (byte) 0x80);
		int bufOff = 0;

		IntObject mask = new IntObject(0xFFFFFFFF);
		IntObject testMask = new IntObject(0);
		int lastChar = 0;

		while (true) {
			int bufPtr1 = bufOff + 2488;
			if (!nextBit(buf, bufPtr1, IntObject.Null, mask, mask, buffer, in, nextIn)) {
				// Single new char
				if (bufOff > 0) {
					bufOff--;
				}
				if (nextOut == outEnd) {
					return -1;
				}
				bufPtr1 = (((((nextOut & 0x07) << 8) + lastChar) >> type) & 0x07) * 0xFF - 0x01;
				IntObject j = new IntObject(1);
				while (j.getValue() <= 0xFF) {
					nextBit(buf, bufPtr1 + j.getValue(), j, mask, mask, buffer, in, nextIn);
				}
				out[nextOut++] = (byte) j.getValue();
			} else {
				// Sequence of chars that exists in out stream

				// Find number of bits of sequence length
				testMask.setValue(mask.getValue());
				int nBits = -1;
				BoolObject flag = new BoolObject();
				do {
					bufPtr1 += 8;
					flag.setValue(nextBit(buf, bufPtr1, IntObject.Null, testMask, mask, buffer, in, nextIn));
					if (flag.getValue()) {
						nBits++;
					}
				} while (flag.getValue() && nBits < 6);

				// Find sequence length
				int bufPtr2 = nBits + 2033;
				int j = 64;
				int seqLen;
				if (flag.getValue() || nBits >= 0) {
					bufPtr1 = (nBits << 5) + (((nextOut << nBits) & 0x03) << 3) + bufOff + 2552;
					seqLen = getNumber(nBits, buf, bufPtr1, 8, flag, mask, buffer, in, nextIn);
					if (seqLen == 0xFF) {
						return nextOut; // End of data stream
					}
					if (flag.getValue() || nBits > 0) {
						bufPtr2 += 56;
						j = 352;
					}
				} else {
					seqLen = 1;
				}

				// Find number of bits of sequence offset
				IntObject i = new IntObject(1);
				do {
					nBits = (i.getValue() << 4) - j;
					flag.setValue(nextBit(buf, bufPtr2 + (i.getValue() << 3), i, mask, mask, buffer, in, nextIn));
				} while (nBits < 0);

				// Find sequence offset
				int seqOff;
				if (flag.getValue() || nBits > 0) {
					if (!flag.getValue()) {
						nBits -= 8;
					}
					seqOff = getNumber(nBits / 8, buf, nBits + 2344, 1, flag, mask, buffer, in, nextIn);
				} else {
					seqOff = 1;
				}

				// Copy sequence
				int nextSeq = nextOut - seqOff;
				if (nextSeq < 0) {
					return -1;
				}
				int seqEnd = nextOut + seqLen + 1;
				if (seqEnd > outEnd) {
					return -1;
				}
				bufOff = ((seqEnd + 1) & 0x01) + 0x06;
				do {
					out[nextOut++] = out[nextSeq++];
				} while (nextOut < seqEnd);
			}
			lastChar = u8(out[nextOut - 1]);
		}
	}
}
