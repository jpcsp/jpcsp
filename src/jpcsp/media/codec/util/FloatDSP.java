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

public class FloatDSP {
	public static void vectorFmul(float[] dst, int dstOffset, final float[] src0, int src0Offset, final float[] src1, int src1Offset, int len) {
		for (int i = 0; i < len; i++) {
			dst[dstOffset + i] = src0[src0Offset + i] * src1[src1Offset + i];
		}
	}

	public static void vectorFmacScalar(float[] dst, int dstOffset, final float[] src, int srcOffset, float mul, int len) {
		for (int i = 0; i < len; i++) {
			dst[dstOffset + i] += src[srcOffset + i] * mul;
		}
	}

	public static void vectorFmulScalar(float[] dst, int dstOffset, final float[] src, int srcOffset, float mul, int len) {
		for (int i = 0; i < len; i++) {
			dst[dstOffset + i] = src[srcOffset + i] * mul;
		}
	}

	public static void vectorDmulScalar(double[] dst, int dstOffset, final double[] src, int srcOffset, double mul, int len) {
		for (int i = 0; i < len; i++) {
			dst[dstOffset + i] = src[srcOffset + i] * mul;
		}
	}

	public static void vectorFmulWindow(float[] dst, int dstOffset, final float[] src0, int src0Offset, final float[] src1, int src1Offset, final float[] win, int winOffset, int len) {
		for (int i = -len, j = len - 1; i < 0; i++, j--) {
			float s0 = src0[src0Offset + len + i];
			float s1 = src1[src1Offset + len + j];
			float wi = win[winOffset + len + i];
			float wj = win[winOffset + len + j];
			dst[dstOffset + len + i] = s0 * wj - s1 * wi;
			dst[dstOffset + len + j] = s0 * wi + s1 * wj;
		}
	}

	public static void vectorFmulAdd(float[] dst, int dstOffset, final float[] src0, int src0Offset, final float[] src1, int src1Offset, final float[] src2, int src2Offset, int len) {
		for (int i = 0; i < len; i++) {
			dst[dstOffset + i] = src0[src0Offset + i] * src1[src1Offset + i] + src2[src2Offset + i];
		}
	}

	public static void vectorFmulReverse(float[] dst, int dstOffset, final float[] src0, int src0Offset, final float[] src1, int src1Offset, int len) {
		for (int i = 0; i < len; i++) {
			dst[dstOffset + i] = src0[src0Offset + i] * src1[src1Offset + len - 1 - i];
		}
	}

	public static void butterflies(float[] v1, float[] v2, int len) {
		for (int i = 0; i < len; i++) {
			float t = v1[i] - v2[i];
			v1[i] += v2[i];
			v2[i] = t;
		}
	}
}
