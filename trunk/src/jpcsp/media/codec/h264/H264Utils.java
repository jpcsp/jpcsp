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
package jpcsp.media.codec.h264;

public class H264Utils {
	private static final int CLAMP_BASE = 512;
	// Array to clamp values in range [0..255]
	private static final int clamp[] = new int[CLAMP_BASE * 2 + 256];
	private static final int redMap[][] = new int[256][256];
	private static final int blueMap[][] = new int[256][256];

	static {
		initClamp();
		initRedMap(0xFF);
		initBlueMap();
	}

	/**
	 * Initialize array to clamp values in range [0..255]
	 */
	private static void initClamp() {
		for (int i = 0; i < 256; i++) {
			clamp[CLAMP_BASE + i] = i;
		}
		for (int i = 0; i < CLAMP_BASE; i++) {
			clamp[i] = 0;
			clamp[i + CLAMP_BASE + 256] = 255;
		}
	}

	/**
	 * The red color component is only depending on the
	 * luma and Cr components, not Cb.
	 * Pre-build a map for all possible combinations of
	 * luma and Cr values.
	 * 
	 * @param alpha  the value of the alpha component [0..255]
	 */
	private static void initRedMap(int alpha) {
		alpha <<= 24;
		for (int luma = 0; luma <= 0xFF; luma++) {
			for (int cr = 0; cr <= 0xFF; cr++) {
				int c = luma - 16;
				int e = cr - 128;

				int red = (298 * c + 409 * e + 128) >> 8;
				red = clamp[red + CLAMP_BASE]; // clamp to [0..255]

				redMap[luma][cr] = alpha | red;
			}
		}
	}

	/**
	 * The blue color component is only depending on the
	 * luma and Cb components, not Cr.
	 * Pre-build a map for all possible combinations of
	 * luma and Cb values.
	 */
	private static void initBlueMap() {
		for (int luma = 0; luma <= 0xFF; luma++) {
			for (int cb = 0; cb <= 0xFF; cb++) {
				int c = luma - 16;
				int d = cb - 128;

				int blue = (298 * c + 516 * d + 128) >> 8;
				blue = clamp[blue + CLAMP_BASE]; // clamp to [0..255]

				blueMap[luma][cb] = blue << 16;
			}
		}
	}

	public static void YUV2ABGR(int width, int height, int luma[], int cb[], int cr[], int abgr[]) {
		final int width2 = width >> 1;

		int offset = 0;
		for (int y = 0; y < height; y++) {
			int offset2 = (y >> 1) * width2;
			for (int x = 0; x < width; x++, offset++) {
				int c = luma[offset] & 0xFF;
				int d = cb[offset2 + (x >> 1)] & 0xFF;
				int e = cr[offset2 + (x >> 1)] & 0xFF;

				// The red and blue color components have been already
				// pre-computed.
				int red = redMap[c][e];
				int blue = blueMap[c][d];

				// The green color components is depending on the
				// luma, Cr and Cb components. Pre-computing all the
				// possible combinations would result in a too high memory
				// usage: 256*256*256*4 bytes = 64Mb.
				// So compute the green color component here.
				c -= 16;
				d -= 128;
				e -= 128;

				int green = (298 * c - 100 * d - 208 * e + 128) >> 8;
				green = clamp[green + CLAMP_BASE]; // clamp to [0..255]

				abgr[offset] = blue | (green << 8) | red;
			}
		}
	}
}
