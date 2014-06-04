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
package jpcsp.util;

import org.apache.log4j.Logger;

import jpcsp.Emulator;

/*
 * Tlzrc decompression.
 *
 * Based on tpunix implementation:
 * https://github.com/tpunix/kirk_engine/blob/master/npdpc/tlzrc.c 
 */
public class Tlzrc {
	protected static Logger log = Emulator.log;

	private static class LzrcDecode {
		// input stream
		byte[] input;
		int in_ptr;
		int in_len;

		// output stream
		byte[] output;
		int out_ptr;
		int out_len;

		// range decode
		int range;
		int code;
		int lc;

		byte[] bm_values = new byte[8 * 256 + 8 * 39 + 18 * 8 + 8 * 8 + 8 * 31];

		public int bm_literal(int i, int j)   { return                                     i * 256 + j; }
		public int bm_dist_bits(int i, int j) { return 8 * 256 +                           i *  39 + j; }
		public int bm_dist(int i, int j)      { return 8 * 256 + 8 * 39 +                  i *   8 + j; }
		public int bm_match(int i, int j)     { return 8 * 256 + 8 * 39 + 18 * 8 +         i *   8 + j; }
		public int bm_len(int i, int j)       { return 8 * 256 + 8 * 39 + 18 * 8 + 8 * 8 + i *  31 + j; }
	}

	private static int u8(byte[] a, int offset) {
		return a[offset] & 0xFF;
	}

	private static int rc_getbyte(LzrcDecode rc) {
		if (rc.in_ptr == rc.in_len) {
			log.error("End of input!");
			return 0;
		}

		return u8(rc.input, rc.in_ptr++);
	}

	private static void rc_putbyte(LzrcDecode rc, byte b) {
		if (rc.out_ptr == rc.out_len) {
			log.error("Output overflow!");
			return;
		}

		rc.output[rc.out_ptr++] = b;
	}

	private static void rc_init(LzrcDecode rc, byte[] out, int out_len, byte[] in, int in_len) {
		rc.input = in;
		rc.in_len = in_len;
		rc.in_ptr = 0;

		rc.output = out;
		rc.out_len = out_len;
		rc.out_ptr = 0;

		rc.range = -1;
		rc.lc = rc_getbyte(rc);
		rc.code = (rc_getbyte(rc) << 24) | (rc_getbyte(rc) << 16) | (rc_getbyte(rc) << 8) | rc_getbyte(rc);

		for (int i = 0; i < rc.bm_values.length; i++) {
			rc.bm_values[i] = (byte) 0x80;
		}
	}

	private static void normalize(LzrcDecode rc) {
		if (rc.range >= 0 && rc.range < 0x01000000) {
			rc.range <<= 8;
			rc.code = (rc.code << 8) | rc_getbyte(rc);
		}
	}

	// Decode a bit
	private static int rc_bit(LzrcDecode rc, byte[] probs, int offset) {
		normalize(rc);

		long bound = ((long) (rc.range >>> 8)) * u8(probs, offset);
		probs[offset] -= u8(probs, offset) >> 3;

		if ((rc.code & 0xFFFFFFFFL) < bound) {
			rc.range = (int) (bound & 0xFFFFFFFFL);
			probs[offset] += 31;
			return 1;
		}

		rc.code -= bound;
		rc.range -= bound;
		return 0;
	}

	// Decode a bittree starting from MSB
	private static int rc_bittree(LzrcDecode rc, byte[] probs, int offset, int limit) {
		int number = 1;

		do {
			number = (number << 1) | rc_bit(rc, probs, offset + number);
		} while (number < limit);

		return number;
	}

	// Decode a number
	//
	// A number is divided into three parts:
	//   MSB 2 bits
	//   direct bits (don't use probability model)
	//   LSB 3 bits
	private static int rc_number(LzrcDecode rc, byte[] prob, int offset, int n) {
		int number = 1;

		if (n > 3) {
			number = (number << 1) | rc_bit(rc, prob, offset + 3);
			if (n > 4) {
				number = (number << 1) | rc_bit(rc, prob, offset + 3);
				if (n > 5) {
					// direct bits
					normalize(rc);

					for (int i = 0; i < n - 5; i++) {
						rc.range >>>= 1;
						number <<= 1;
						if ((rc.code & 0xFFFFFFFFL) < (rc.range & 0xFFFFFFFFL)) {
							number += 1;
						} else {
							rc.code -= rc.range;
						}
					}
				}
			}
		}

		if (n > 0) {
			number = (number << 1) | rc_bit(rc, prob, offset);
			if (n > 1) {
				number = (number << 1) | rc_bit(rc, prob, offset + 1);
				if (n > 2) {
					number = (number << 1) | rc_bit(rc, prob, offset + 2);
				}
			}
		}

		return number;
	}

	public static int lzrc_decompress(byte[] out, int out_len, byte[] in, int in_len) {
		LzrcDecode rc = new LzrcDecode();

		rc_init(rc, out, out_len, in, in_len);

		if ((rc.lc & 0x80) != 0) {
			// Plain text
			System.arraycopy(rc.input, 5, rc.output, 0, rc.code);
			return rc.code;
		}

		int rc_state = 0;
		int last_byte = 0;

		while (true) {
			int match_step = 0;

			int bit = rc_bit(rc, rc.bm_values, rc.bm_match(rc_state, match_step));
			if (bit == 0) {
				// 0 -> raw char
				if (rc_state > 0) {
					rc_state--;
				}

				int b = rc_bittree(rc, rc.bm_values, rc.bm_literal((last_byte >> rc.lc) & 0x07, 0), 0x100);
				b -= 0x100;

				rc_putbyte(rc, (byte) b);
			} else {
				// 1 -> a match

				// Find bits of match length
				int len_bits = 0;
				for (int i = 0; i < 7; i++) {
					match_step++;
					bit = rc_bit(rc, rc.bm_values, rc.bm_match(rc_state, match_step));
					if (bit == 0) {
						break;
					}
					len_bits++;
				}

				// Find match length
				int match_len;
				if (len_bits == 0) {
					match_len = 1;
				} else {
					int len_state = ((len_bits - 1) << 2) + ((rc.out_ptr << (len_bits - 1)) & 0x03);
					match_len = rc_number(rc, rc.bm_values, rc.bm_len(rc_state, len_state), len_bits);
					if (match_len == 0xFF) {
						// End of stream
						return rc.out_ptr;
					}
				}

				// Find number of bits of match distance
				int dist_state = 0;
				int limit = 8;
				if (match_len > 2) {
					dist_state += 7;
					limit = 44;
				}
				int dist_bits = rc_bittree(rc, rc.bm_values, rc.bm_dist_bits(len_bits, dist_state), limit);
				dist_bits -= limit;

				// Find match distance
				int match_dist;
				if (dist_bits > 0) {
					match_dist = rc_number(rc, rc.bm_values, rc.bm_dist(dist_bits, 0), dist_bits);
				} else {
					match_dist = 1;
				}

				// Copy match bytes
				if (match_dist > rc.out_ptr || match_dist < 0) {
					log.error(String.format("match_dist out of range! 0x%08X", match_dist));
					return -1;
				}

				int match_src = rc.out_ptr - match_dist;
				for (int i = 0; i < match_len + 1; i++) {
					rc_putbyte(rc, rc.output[match_src++]);
				}
				rc_state = 6 + ((rc.out_ptr + 1) & 1);
			}
			last_byte = rc.output[rc.out_ptr - 1];
		}
	}
}
