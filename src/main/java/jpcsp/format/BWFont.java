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

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.HLE.kernel.types.SceFontInfo;
import jpcsp.HLE.kernel.types.SceFontInfoBW;

/**
 * BW font file format.
 * Based on
 *    https://github.com/GeeckoDev/intraFont-G/blob/master/intraFont.c
 *
 * @author gid15
 *
 */
public class BWFont extends PGF {
	private byte[] fontData;
	public static final int charBitmapWidth = 16;
	public static final int charBitmapHeight = 18;
	public static final int charBitmapBytes = charBitmapWidth * charBitmapHeight / 8;
	private static final int[] bwCharmapCompressed = new int[] {
		0x00a4,  1, 0x00a7,  2, 0x00b0,  2, 0x00b7,  1, 0x00d7,    1, 0x00e0,     2, 0x00e8,  3, 0x00ec,  2,
		0x00f2,  2, 0x00f7,  1, 0x00f9,  2, 0x00fc,  1, 0x0101,    1, 0x0113,     1, 0x011b,  1, 0x012b,  1,
		0x0144,  1, 0x0148,  1, 0x014d,  1, 0x016b,  1, 0x01ce,    1, 0x01d0,     1, 0x01d2,  1, 0x01d4,  1,
		0x01d6,  1, 0x01d8,  1, 0x01da,  1, 0x01dc,  1, 0x0251,    1, 0x0261,     1, 0x02c7,  1, 0x02c9,  3,
		0x02d9,  1, 0x0391, 17, 0x03a3,  7, 0x03b1, 17, 0x03c3,    7, 0x0401,     1, 0x0410, 64, 0x0451,  1,
		0x2010,  1, 0x2013,  4, 0x2018,  2, 0x201c,  2, 0x2025,    2, 0x2030,     1, 0x2032,  2, 0x2035,  1,
		0x203b,  1, 0x20ac,  1, 0x2103,  1, 0x2105,  1, 0x2109,    1, 0x2116,     1, 0x2121,  1, 0x2160, 12,
		0x2170, 10, 0x2190,  4, 0x2196,  4, 0x2208,  1, 0x220f,    1, 0x2211,     1, 0x2215,  1, 0x221a,  1,
		0x221d,  4, 0x2223,  1, 0x2225,  1, 0x2227,  5, 0x222e,    1, 0x2234,     4, 0x223d,  1, 0x2248,  1,
		0x224c,  1, 0x2252,  1, 0x2260,  2, 0x2264,  4, 0x226e,    2, 0x2295,     1, 0x2299,  1, 0x22a5,  1,
		0x22bf,  1, 0x2312,  1, 0x2460, 10, 0x2474, 40, 0x2500,   76, 0x2550,    36, 0x2581, 15, 0x2593,  3,
		0x25a0,  2, 0x25b2,  2, 0x25bc,  2, 0x25c6,  2, 0x25cb,    1, 0x25ce,     2, 0x25e2,  4, 0x2605,  2,
		0x2609,  1, 0x2640,  1, 0x2642,  1, 0x2e81,  1, 0x2e84,    1, 0x2e88,     1, 0x2e8b,  2, 0x2e97,  1,
		0x2ea7,  1, 0x2eaa,  1, 0x2eae,  1, 0x2eb3,  1, 0x2eb6,    2, 0x2ebb,     1, 0x2eca,  1, 0x2ff0, 12,
		0x3000,  4, 0x3005, 19, 0x301d,  2, 0x3021,  9, 0x303e,    1, 0x3041,    83, 0x309b,  4, 0x30a1, 86,
		0x30fc,  3, 0x3105, 37, 0x3220, 10, 0x3231,  1, 0x32a3,    1, 0x338e,     2, 0x339c,  3, 0x33a1,  1,
		0x33c4,  1, 0x33ce,  1, 0x33d1,  2, 0x33d5,  1, 0x3400, 6582, 0x4e00, 20902, 0xe78d, 10, 0xe7c7,  2,
		0xe816,  3, 0xe81e,  1, 0xe826,  1, 0xe82b,  2, 0xe831,    2, 0xe83b,     1, 0xe843,  1, 0xe854,  2,
		0xe864,  1, 0xf92c,  1, 0xf979,  1, 0xf995,  1, 0xf9e7,    1, 0xf9f1,     1, 0xfa0c,  4, 0xfa11,  1,
		0xfa13,  2, 0xfa18,  1, 0xfa1f,  3, 0xfa23,  2, 0xfa27,    3, 0xfe30,     2, 0xfe33, 18, 0xfe49, 10,
		0xfe54,  4, 0xfe59, 14, 0xfe68,  4, 0xff01, 94, 0xffe0,    6
	};

	public BWFont(ByteBuffer f, String fileName) throws IOException {
		super();
		read(f, fileName);
	}

	private void read(ByteBuffer f, String fileName) {
		fontData = new byte[f.capacity()];
		f.get(fontData);

		firstGlyph = bwCharmapCompressed[0];
		lastGlyph = bwCharmapCompressed[bwCharmapCompressed.length - 2] + bwCharmapCompressed[bwCharmapCompressed.length - 1] - 1;
		maxSize[0] = charBitmapWidth << 6;
		maxSize[1] = charBitmapHeight << 6;
		maxAdvance[0] = charBitmapWidth << 6;
		maxAdvance[1] = charBitmapHeight << 6;
		fontName = fileName;
		fontType = fileName;
	}

	public byte[] getFontData() {
		return fontData;
	}

	public int[] getCharmapCompressed() {
		return bwCharmapCompressed;
	}

	@Override
	public SceFontInfo createFontInfo() {
		return new SceFontInfoBW(this);
	}
}
