// Copyright 2007,2008,2010  Segher Boessenkool  <segher@kernel.crashing.org>
// Licensed under the terms of the GNU GPL, version 2
// http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
// Updated and simplified for use by Kirk Engine - July 2011
// Ported to Java by gid15
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
package libkirk;

import static java.lang.System.arraycopy;
import static jpcsp.util.Utilities.getMemoryDump;
import static libkirk.Utilities.log;
import static libkirk.Utilities.u8;

import java.util.Arrays;

/**
 * Ported to Java from
 * https://github.com/ProximaV/kirk-engine-full/blob/master/libkirk/bn.c
 */
public class BN {
	public static void bn_print(String name, byte[] a, int offset, int n) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s = %s", name, getMemoryDump(a, offset, n)));
		}
	}

	public static void bn_zero(byte[] d, int n) {
		bn_zero(d, 0, n);
	}

	public static void bn_zero(byte[] d, int offset, int n) {
		Arrays.fill(d, offset, offset + n, (byte) 0);
	}

	public static void bn_copy(byte[] d, byte[] a, int n) {
		bn_copy(d, 0, a, 0, n);
	}

	public static void bn_copy(byte[] d, int offsetDst, byte[] a, int offsetSrc, int n) {
		arraycopy(a, offsetSrc, d, offsetDst, n);
	}

	public static int bn_compare(byte[] a, byte[] b, int n) {
		return bn_compare(a, 0, b, 0, n);
	}

	public static int bn_compare(byte[] a, int offset1, byte[] b, int offset2, int n) {
		for (int i = 0; i < n; i++) {
			int a8 = u8(a, offset1 + i);
			int b8 = u8(b, offset2 + i);
			if (a8 < b8) {
				return -1;
			}
			if (a8 > b8) {
				return 1;
			}
		}

		return 0;
	}

	public static int bn_add_1(byte[] d, byte[] a, byte[] b, int n) {
		return bn_add_1(d, 0, a, 0, b, 0, n);
	}

	public static int bn_add_1(byte[] d, int offsetDst, byte[] a, int offset1, byte[] b, int offset2, int n) {
		int c = 0;

		for (int i = n - 1; i >= 0; i--) {
			int dig = u8(a, offset1 + i) + u8(b, offset2 + i) + c;
			c = u8(dig >>> 8);
			d[offsetDst + i] = (byte) dig;
		}

		return c;
	}

	public static int bn_sub_1(byte[] d, byte[] a, byte[] b, int n) {
		return bn_sub_1(d, 0, a, 0, b, 0, n);
	}

	public static int bn_sub_1(byte[] d, int offsetDst, byte[] a, int offset1, byte[] b, int offset2, int n) {
		int c = 1;

		for (int i = n - 1; i >= 0; i--) {
			int dig = u8(a, offset1 + i) + 255 - u8(b, offset2 + i) + c;
			c = u8(dig >>> 8);
			d[offsetDst + i] = (byte) dig;
		}

		return 1 - c;
	}

	public static void bn_reduce(byte[] d, byte[] N, int n) {
		bn_reduce(d, 0, N, 0, n);
	}

	public static void bn_reduce(byte[] d, int offsetDst, byte[] N, int offsetN, int n) {
		if (bn_compare(d, offsetDst, N, offsetN, n) >= 0) {
			bn_sub_1(d, offsetDst, d, offsetDst, N, offsetN, n);
		}
	}

	public static void bn_add(byte[] d, byte[] a, byte[] b, byte[] N, int n) {
		bn_add(d, 0, a, 0, b, 0, N, 0, n);
	}

	public static void bn_add(byte[] d, int offsetDst, byte[] a, int offset1, byte[] b, int offset2, byte[] N, int offsetN, int n) {
		if (bn_add_1(d, offsetDst, a, offset1, b, offset2, n) != 0) {
			bn_sub_1(d, offsetDst, d, offsetDst, N, offsetN, n);
		}

		bn_reduce(d, offsetDst, N, offsetN, n);
	}

	public static void bn_sub(byte[] d, byte[] a, byte[] b, byte[] N, int n) {
		bn_sub(d, 0, a, 0, b, 0, N, 0, n);
	}

	public static void bn_sub(byte[] d, int offsetDst, byte[] a, int offset1, byte[] b, int offset2, byte[] N, int offsetN, int n) {
		if (bn_sub_1(d, offsetDst, a, offset1, b, offset2, n) != 0) {
			bn_add_1(d, offsetDst, d, offsetDst, N, offsetN, n);
		}
	}

	private static final int inv256[] = new int[] {
			0x01, 0xab, 0xcd, 0xb7, 0x39, 0xa3, 0xc5, 0xef,
			0xf1, 0x1b, 0x3d, 0xa7, 0x29, 0x13, 0x35, 0xdf,
			0xe1, 0x8b, 0xad, 0x97, 0x19, 0x83, 0xa5, 0xcf,
			0xd1, 0xfb, 0x1d, 0x87, 0x09, 0xf3, 0x15, 0xbf,
			0xc1, 0x6b, 0x8d, 0x77, 0xf9, 0x63, 0x85, 0xaf,
			0xb1, 0xdb, 0xfd, 0x67, 0xe9, 0xd3, 0xf5, 0x9f,
			0xa1, 0x4b, 0x6d, 0x57, 0xd9, 0x43, 0x65, 0x8f,
			0x91, 0xbb, 0xdd, 0x47, 0xc9, 0xb3, 0xd5, 0x7f,
			0x81, 0x2b, 0x4d, 0x37, 0xb9, 0x23, 0x45, 0x6f,
			0x71, 0x9b, 0xbd, 0x27, 0xa9, 0x93, 0xb5, 0x5f,
			0x61, 0x0b, 0x2d, 0x17, 0x99, 0x03, 0x25, 0x4f,
			0x51, 0x7b, 0x9d, 0x07, 0x89, 0x73, 0x95, 0x3f,
			0x41, 0xeb, 0x0d, 0xf7, 0x79, 0xe3, 0x05, 0x2f,
			0x31, 0x5b, 0x7d, 0xe7, 0x69, 0x53, 0x75, 0x1f,
			0x21, 0xcb, 0xed, 0xd7, 0x59, 0xc3, 0xe5, 0x0f,
			0x11, 0x3b, 0x5d, 0xc7, 0x49, 0x33, 0x55, 0xff
	};

	public static void bn_mon_muladd_dig(byte[] d, byte[] a, int b, byte[] N, int n) {
		bn_mon_muladd_dig(d, 0, a, 0, b, N, 0, n);
	}

	public static void bn_mon_muladd_dig(byte[] d, int offsetDst, byte[] a, int offset1, int b, byte[] N, int offsetN, int n) {
		b = u8(b);
		int z = u8(-(u8(d, offsetDst + n - 1) + u8(a, offset1 + n - 1) * b) * inv256[u8(N, offsetN + n - 1) / 2]);

		int dig = u8(d, offsetDst + n - 1) + u8(a, offset1 + n - 1) * b + u8(N, offsetN + n - 1) * z;
		dig >>>= 8;

		for (int i = n - 2; i >= 0; i--) {
			dig += u8(d, offsetDst + i) + u8(a, offset1 + i) * b + u8(N, offsetN + i) * z;
			d[offsetDst + i + 1] = (byte) dig;
			dig >>>= 8;
		}

		d[offsetDst + 0] = (byte) dig;
		dig >>>= 8;

		if (dig != 0) {
			bn_sub_1(d, offsetDst, d, offsetDst, N, offsetN, n);
		}

		bn_reduce(d, offsetDst, N, offsetN, n);
	}

	public static void bn_mon_mul(byte[] d, byte[] a, byte[] b, byte[] N, int n) {
		bn_mon_mul(d, 0, a, 0, b, 0, N, 0, n);
	}

	public static void bn_mon_mul(byte[] d, int offsetDst, byte[] a, int offset1, byte[] b, int offset2, byte[] N, int offsetN, int n) {
		final byte[] t = new byte[n];

		//bn_zero(t, 0, n);

		for (int i = n - 1; i >= 0; i--) {
			bn_mon_muladd_dig(t, 0, a, offset1, b[offset2 + i], N, offsetN, n);
		}

		bn_copy(d, offsetDst, t, 0, n);
	}

	public static void bn_to_mon(byte[] d, byte[] N, int n) {
		bn_to_mon(d, 0, N, 0, n);
	}

	public static void bn_to_mon(byte[] d, int offsetDst, byte[] N, int offsetN, int n) {
		for (int i = 0; i < 8 * n; i++) {
			bn_add(d, offsetDst, d, offsetDst, d, offsetDst, N, offsetN, n);
		}
	}

	public static void bn_from_mon(byte[] d, byte[] N, int n) {
		bn_from_mon(d, 0, N, 0, n);
	}

	public static void bn_from_mon(byte[] d, int offsetDst, byte[] N, int offsetN, int n) {
		final byte[] t = new byte[n];

		//bn_zero(t, 0, n);
		t[n - 1] = (byte) 1;
		bn_mon_mul(d, offsetDst, d, offsetDst, t, 0, N, offsetN, n);
	}

	public static void bn_mon_exp(byte[] d, byte[] a, byte[] N, int n, byte[] e, int en) {
		bn_mon_exp(d, 0, a, 0, N, 0, n, e, 0, en);
	}

	public static void bn_mon_exp(byte[] d, int offsetDst, byte[] a, int offset1, byte[] N, int offsetN, int n, byte[] e, int offsetE, int en) {
		final byte[] t = new byte[n];

		bn_zero(d, offsetDst, n);
		d[offsetDst + n - 1] = (byte) 1;
		bn_to_mon(d, offsetDst, N, offsetN, n);

		for (int i = 0; i < en; i++) {
			for (int mask = 0x80; mask != 0; mask >>= 1) {
				bn_mon_mul(t, 0, d, offsetDst, d, offsetDst, N, offsetN, n);
				if ((u8(e, offsetE + i) & mask) != 0) {
					bn_mon_mul(d, offsetDst, t, 0, a, offset1, N, offsetN, n);
				} else {
					bn_copy(d, offsetDst, t, 0, n);
				}
			}
		}
	}

	public static void bn_mon_inv(byte[] d, byte[] a, byte[] N, int n) {
		bn_mon_inv(d, 0, a, 0, N, 0, n);
	}

	public static void bn_mon_inv(byte[] d, int offsetDst, byte[] a, int offset1, byte[] N, int offsetN, int n) {
		final byte[] t = new byte[n];
		final byte[] s = new byte[n];

		//bn_zero(s, 0, n);
		s[n - 1] = (byte) 2;
		bn_sub_1(t, 0, N, offsetN, s, 0, n);
		bn_mon_exp(d, offsetDst, a, offset1, N, offsetN, n, t, 0, n);
	}
}
