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

import static java.lang.Integer.compareUnsigned;
import static java.lang.Math.max;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.memset;
import static jpcsp.util.Utilities.read8;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.sizeof;

/**
 * KL4E Decompression.
 * 
 * Based on the implementation from
 *     https://github.com/John-K/pspdecrypt/blob/master/kl4e.c
 * and simply ported to Java.
 *
 */
public class KL4E {
	/*
	 * KL4E's behavior seems to be very similar to LZMA.
	 *
	 * It is based on two things:
	 * 1) the LZ77 algorithm
	 * 2) arithmetic coding.
	 *
	 * The first point is just the idea that, in order to compress a stream using repetitions, you can either:
	 * - output a raw byte
	 * - repeat a sequence of previous raw bytes.
	 * In the base LZ77 algorithm, the main loop first reads a bit, then:
	 * - if the bit is a 0, read the next 8 bits and output the byte which corresponds (called a literal),
	 * - if the bit is a 1, read some distance m and length n which can be encoded in various ways, and copy
	 *   the n bytes which were output m bytes earlier.
	 *
	 * Here, instead of encoding bits directly, KL4E uses some kind of arithmetic coding. The general idea is
	 * that you read 4 bytes, then if you know a 1 will happen with probability p, you consider the output
	 * is a 1 if the 4 bytes are in the [0, 2^32 * p] interval, and a 0 if it's in the [2 ^ 32 * p, 2 ^ 32]
	 * interval. You then repeat the operation by taking the subinterval in which the value is, and read another
	 * byte when the interval became smaller than 2^24. The probabilities are also updated each time, by decaying
	 * (they're multiplied by 7/8 or 15/16 each time) and receiving an additional 31 or 15 if the output was indeed a 1.
	 *
	 * Compared to 2RLZ, here all the probabilities are set to a single value given by the header (instead of
	 * just being 0x80 ie 1/2 by default), and a literal is output directly without checking if the first bit is 0.
	 *
	 * The rest is just up to how you encode distance and length codes, which is where the difference between KL3E
	 * and KL4E lies (KL4E seems to be able to have bigger maximum "distance" codes), and also what probabilities
	 * you consider (for example, Sony uses different probabilities depending on the file's offset modulo 8).
	 */

	/*
	 * Read one bit using arithmetic coding, with a given (updated) probability and its associated decay/bonus.
	 */
	private static int read_bit(int[] inputVal, int[] range, byte[] probPtr, int probPtrOffset, byte[] inBuf, int[] inBufOffset, int decay, int bonus) {
	    int bound;
	    int prob = read8(probPtr, probPtrOffset);
	    if ((range[0] >>> 24) == 0) {
	        inputVal[0] = (inputVal[0] << 8) + read8(inBuf, inBufOffset[0]++);
	        bound = range[0] * prob;
	        range[0] <<= 8;
	    } else {
	        bound = (range[0] >>> 8) * prob;
	    }
	    prob -= (prob >> decay);
	    if (compareUnsigned(inputVal[0], bound) >= 0) {
	        inputVal[0] -= bound;
	        range[0] -= bound;
	        probPtr[probPtrOffset] = (byte) prob;
	        return 0;
	    } else {
	        range[0] = bound;
	        probPtr[probPtrOffset] = (byte) (prob + bonus);
	        return 1;
	    }
	}

	/*
	 * Same as read_bit but returning false/true instead of 0/1.
	 */
	private static boolean read_boolean(int[] inputVal, int[] range, byte[] probPtr, int probPtrOffset, byte[] inBuf, int[] inBufOffset, int decay, int bonus) {
		return read_bit(inputVal, range, probPtr, probPtrOffset, inBuf, inBufOffset, decay, bonus) != 0;
	}

	/*
	 * Same as above, but with balanced probability 1/2.
	 */
	private static int read_bit_uniform(int[] inputVal, int[] range, byte[] inBuf, int[] inBufOffset) {
	    if (range[0] >>> 24 == 0) {
	    	inputVal[0] = (inputVal[0] << 8) + read8(inBuf, inBufOffset[0]++);
	        range[0] = range[0] << 7;
	    } else {
	    	range[0] = range[0] >>> 1;
	    }
	    if (compareUnsigned(inputVal[0], range[0]) >= 0) {
	    	inputVal[0] -= range[0];
	        return 0;
	    } else {
	        return 1;
	    }
	}

	/*
	 * Same as above, but without normalizing the range.
	 */
	private static int read_bit_uniform_nonormal(int[] inputVal, int[] range) {
		range[0] >>>= 1;
	    if (compareUnsigned(inputVal[0], range[0]) >= 0) {
	    	inputVal[0] -= range[0];
	        return 0;
	    } else {
	        return 1;
	    }
	}

	/*
	 * Output a raw byte by reading 8 bits using arithmetic coding.
	 */
	private static void output_raw(int[] inputVal, int[] range, byte[] probs, int probsOffset, byte[] inBuf, int[] inBufOffset, int[] curByte, byte[] curOut, int curOutOffset, int shift) {
	    int mask = ((curOutOffset & 7) << 8) | (curByte[0] & 0xFF);
	    int curProbsOffset = probsOffset + (((mask >> shift) & 7) * 255) - 1;
	    curByte[0] = 1;
	    while (curByte[0] < 0x100) {
	        int curProbOffset = curProbsOffset + curByte[0];
	        curByte[0] = (curByte[0] << 1) | read_bit(inputVal, range, probs, curProbOffset, inBuf, inBufOffset, 3, 31);
	    }
	    curOut[curOutOffset] = (byte) (curByte[0] & 0xff);
	}

	public static int decompress_kle(byte[] outBuf, int outBufOffset, int outSize, byte[] inBuf, int inBufOffset, int[] end, boolean isKl4e) {
	    final byte[] litProbs = new byte[2040];
	    final byte[] copyDistBitsProbs = new byte[304];
	    final byte[] copyDistProbs = new byte[144];
	    final byte[] copyCountBitsProbs = new byte[64];
	    final byte[] copyCountProbs = new byte[256];
	    int outEndOffset = outBufOffset + outSize;
	    int curOutOffset = outBufOffset;
	    final int[] curByte = { 0 };
	    final int[] range = { 0xffffffff };
	    int copyDist = 0;
	    int copyCount;
	    int curCopyDistBitsProbsOffset = 0;
	    final int[] inputVal = new int[1];

	    inputVal[0] = endianSwap32(readUnaligned32(inBuf, inBufOffset + 1));

	    // Handle the direct copy case (if the file is actually not compressed).
	    if ((read8(inBuf, inBufOffset + 0) & 0x80) != 0) {
	        inBufOffset += 5;
	        int dataEndOffset = inputVal[0];
	        if (dataEndOffset >= outEndOffset) {
	            return 0x80000104; // SCE_ERROR_INVALID_SIZE
	        }
	        while (curOutOffset < dataEndOffset) {
	            outBuf[curOutOffset++] = inBuf[inBufOffset++];
	        }
	        inBufOffset--;
	        if (end != null) {
	            end[0] = inBufOffset;
	        }
	        return curOutOffset - outBufOffset;
	    }

	    // Initialize probabilities from the header value.
	    byte b = (byte) (128 - (((read8(inBuf, inBufOffset + 0) >> 3) & 3) << 4));
	    memset(litProbs, b, sizeof(litProbs));
	    memset(copyCountBitsProbs, b, sizeof(copyCountBitsProbs));
	    memset(copyDistBitsProbs, b, sizeof(copyDistBitsProbs));
	    memset(copyCountProbs, b, sizeof(copyCountProbs));
	    memset(copyDistProbs, b, sizeof(copyDistProbs));

	    byte[] curCopyCountBitsProbs = copyCountBitsProbs;
	    int curCopyCountBitsProbsOffset = 0;

	    /* Shift used to determine if the probabilities should be determined more by the
	     * output's byte alignment or by the previous byte. */
	    byte shift = (byte) (read8(inBuf, inBufOffset + 0) & 0x7);
	    inBufOffset += 5;

	    // Read a literal directly.
		final int[] inBufPtr = { inBufOffset };
	    output_raw(inputVal, range, litProbs, 0, inBuf, inBufPtr, curByte, outBuf, curOutOffset, shift);

	    while (true) {
	        curOutOffset++;

	        // If we read a 0, read a literal.
	        if (!read_boolean(inputVal, range, curCopyCountBitsProbs, curCopyCountBitsProbsOffset, inBuf, inBufPtr, 4, 15)) {
	            curCopyCountBitsProbsOffset = max(curCopyCountBitsProbsOffset - 1, 0);
	            if (curOutOffset == outEndOffset) {
	                return 0x80000104; // SCE_ERROR_INVALID_SIZE
	            }
	            output_raw(inputVal, range, litProbs, 0, inBuf, inBufPtr, curByte, outBuf, curOutOffset, shift);
	            continue;
	        }

	        // Otherwise, first find the number of bits used in the 'length' code.
	        copyCount = 1;
	        int copyCountBits = -1;
	        while (copyCountBits < 6) {
	            curCopyCountBitsProbsOffset += 8;
	            if (!read_boolean(inputVal, range, curCopyCountBitsProbs, curCopyCountBitsProbsOffset, inBuf, inBufPtr, 4, 15)) {
	                break;
	            }
	            copyCountBits++;
	        }

	        // Determine the length itself, and use different distance code probabilities depending on it (and on whether it's KL3E or KL4E).
	        int powLimit = 0;
	        if (copyCountBits >= 0) {
	        	byte[] probs = copyCountProbs;
	            int probsOffset = (copyCountBits << 5) | (((curOutOffset & 3) << (copyCountBits + 3)) & 0x18) | (curCopyCountBitsProbsOffset & 7);
	            if (copyCountBits < 3) {
	                copyCount = 1;
	            } else {
	                copyCount = 2 + read_bit(inputVal, range, probs, probsOffset + 24, inBuf, inBufPtr, 3, 31);
	                if (copyCountBits > 3) {
	                    copyCount = (copyCount << 1) | read_bit(inputVal, range, probs, probsOffset + 24, inBuf, inBufPtr, 3, 31);
	                    if (copyCountBits > 4) {
	                        copyCount = (copyCount << 1) | read_bit_uniform(inputVal, range, inBuf, inBufPtr);
	                    }
	                    for (int i = 5; i < copyCountBits; i++) {
	                        copyCount = (copyCount << 1) | read_bit_uniform_nonormal(inputVal, range);
	                    }
	                }
	            }
	            copyCount = copyCount << 1;
	            if (read_boolean(inputVal, range, probs, probsOffset, inBuf, inBufPtr, 3, 31)) {
	                copyCount |= 1;
	                if (copyCountBits <= 0) {
	                    powLimit = isKl4e ? 256 : 128;
	                    curCopyDistBitsProbsOffset = 56 + copyCountBits;
	                }
	            } else {
	                if (copyCountBits <= 0) {
	                    powLimit = 64;
	                    curCopyDistBitsProbsOffset = copyCountBits;
	                }
	            }
	            if (copyCountBits > 0) {
	                copyCount = (copyCount << 1) | read_bit(inputVal, range, probs, probsOffset + 8, inBuf, inBufPtr, 3, 31);
	                if (copyCountBits != 1) {
	                    copyCount = copyCount << 1;
	                    if (read_boolean(inputVal, range, probs, probsOffset + 16, inBuf, inBufPtr, 3, 31)) {
	                        copyCount = copyCount + 1;
	                        if (copyCount == 0xFF) {
	                            if (end != null) {
	                                end[0] = inBufPtr[0];
	                            }
	                            return curOutOffset - outBufOffset;
	                        }
	                    }
	                }
	                curCopyDistBitsProbsOffset = 56 + copyCountBits;
	                powLimit = isKl4e ? 256 : 128;
	            }
	        } else {
	            powLimit = 64;
	            curCopyDistBitsProbsOffset = copyCountBits;
	        }

	        // Find out the number of bits used for distance codes.
	        int curPow = 8;
	        boolean skip = false;
	        int copyDistBits;
	        while (true) {
	            int curProbOffset = curCopyDistBitsProbsOffset + (curPow - 7);
	            curPow <<= 1;
	            copyDistBits = curPow - powLimit;
	            if (!read_boolean(inputVal, range, copyDistBitsProbs, curProbOffset, inBuf, inBufPtr, 3, 31)) {
	                if (copyDistBits >= 0) {
	                    if (copyDistBits != 0) {
	                        copyDistBits -= 8;
	                        break;
	                    }
	                    copyDist = 0;
//	                    if (copyDistBitsProbs == curOut && curCopyDistBitsProbsOffset == curOutOffset) { // Is this a mistake by Sony?
//	                        return 0x80000108; // SCE_ERROR_INVALID_FORMAT
//	                    }
	                    skip = true; // Just copy with a zero distance.
	                    break;
	                }
	            } else {
	                curPow += 8;
	                if (copyDistBits >= 0) {
	                    break;
	                }
	            }
	        }

	        if (!skip) {
	            // Find out the distance itself.
	            byte[] curProbs = copyDistProbs;
	            int curProbsOffset = copyDistBits;
	            int readBits = copyDistBits / 8;
	            if (readBits < 3) {
	                copyDist = 1;
	            } else {
	                copyDist = 2 + read_bit(inputVal, range, curProbs, curProbsOffset + 3, inBuf, inBufPtr, 3, 31);
	                if (readBits > 3) {
	                    copyDist = (copyDist << 1) | read_bit(inputVal, range, curProbs, curProbsOffset + 3, inBuf, inBufPtr, 3, 31);
	                    if (readBits > 4) {
	                        copyDist = (copyDist << 1) | read_bit_uniform(inputVal, range, inBuf, inBufPtr);
	                        readBits--;
	                    }
	                    while (readBits > 4) {
	                        copyDist = copyDist << 1;
	                        copyDist += read_bit_uniform_nonormal(inputVal, range);
	                        readBits--;
	                    }
	                }
	            }
	            copyDist = copyDist << 1;
	            if (read_boolean(inputVal, range, curProbs, curProbsOffset, inBuf, inBufPtr, 3, 31)) {
	                if (readBits > 0) {
	                    copyDist = copyDist + 1;
	                }
	            } else {
	                if (readBits <= 0) {
	                    copyDist = copyDist - 1;
	                }
	            }
	            if (readBits > 0) {
	                copyDist = copyDist << 1;
	                if (read_boolean(inputVal, range, curProbs, curProbsOffset + 1, inBuf, inBufPtr, 3, 31)) {
	                    if (readBits != 1) {
	                        copyDist = copyDist + 1;
	                    }
	                } else {
	                    if (readBits == 1) {
	                        copyDist = copyDist - 1;
	                    }
	                }
	                if (readBits != 1) {
	                    copyDist = copyDist << 1;
	                    if (!read_boolean(inputVal, range, curProbs, curProbsOffset + 2, inBuf, inBufPtr, 3, 31)) {
	                        copyDist = copyDist - 1;
	                    }
	                }
	            }

	            if (copyDist >= curOutOffset - outBufOffset) {
	                return 0x80000108; // SCE_ERROR_INVALID_FORMAT
	            }
	        }

	        // Copy the bytes with the given count and distance
	        for (int i = 0; i < copyCount + 1; i++) {
	        	outBuf[curOutOffset + i] = outBuf[curOutOffset - copyDist - 1 + i];
	        }
	        curByte[0] = read8(outBuf, curOutOffset + copyCount);
	        curOutOffset += copyCount;
	        curCopyCountBitsProbs = copyCountBitsProbs;
	        curCopyCountBitsProbsOffset = 6 + (curOutOffset & 1);
	    }
	}
}
