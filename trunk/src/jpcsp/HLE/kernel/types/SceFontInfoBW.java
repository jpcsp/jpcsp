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
package jpcsp.HLE.kernel.types;

import static jpcsp.format.BWFont.charBitmapBytes;
import static jpcsp.format.BWFont.charBitmapHeight;
import static jpcsp.format.BWFont.charBitmapWidth;
import jpcsp.format.BWFont;
import jpcsp.util.Debug;

/**
 * BW font file format.
 * Based on
 *    https://github.com/GeeckoDev/intraFont-G/blob/master/intraFont.c
 *
 * @author gid15
 *
 */
public class SceFontInfoBW extends SceFontInfo {
	private short[][] charBitmapData;
	private int[] charmapCompressed;
	private static final int[] pixelColors = new int[] { 0x00000000, 0xFFFFFFFF };

	public SceFontInfoBW(BWFont fontFile) {
		byte[] fontData = fontFile.getFontData();
		charmapCompressed = fontFile.getCharmapCompressed();
		int numberCharBitmaps = fontData.length / charBitmapBytes;
		charBitmapData = new short[numberCharBitmaps][charBitmapHeight];
		shadowScaleX = 24;
		shadowScaleY = 24;

		int fontDataIndex = 0;
		for (int i = 0; i < numberCharBitmaps; i++) {
			for (int j = 0; j < charBitmapHeight; j++, fontDataIndex += 2) {
				int bitmapRow = Integer.reverse(((fontData[fontDataIndex] & 0xFF) << 8) | (fontData[fontDataIndex + 1] & 0xFF)) >>> 16;
				charBitmapData[i][j] = (short) bitmapRow;
			}
		}
	}

	@Override
	public void printFont(int base, int bpl, int bufWidth, int bufHeight, int x, int y, int clipX, int clipY, int clipWidth, int clipHeight, int pixelformat, int charCode, int altCharCode, int glyphType) {
		if (glyphType != FONT_PGF_CHARGLYPH) {
			return;
		}

		int charIndex = getCharIndex(charCode, charmapCompressed);
		if (charIndex < 0 || charIndex >= charBitmapData.length) {
			return;
		}
		short[] bitmapData = charBitmapData[charIndex];

		for (int yy = 0; yy < charBitmapHeight; yy++) {
			int bitmapRow = bitmapData[yy] & 0xFFFF;
			for (int xx = 0; xx < charBitmapWidth; xx++, bitmapRow >>= 1) {
				int pixelX = x + xx;
				int pixelY = y + yy;
                if (pixelX >= clipX && pixelX < clipX + clipWidth && pixelY >= clipY && pixelY < clipY + clipHeight) {
    				int pixelColor = pixelColors[bitmapRow & 1];
                	Debug.setFontPixel(base, bpl, bufWidth, bufHeight, pixelX, pixelY, pixelColor, pixelformat);
                }
			}
		}
	}

	@Override
	public pspCharInfo getCharInfo(int charCode, int glyphType) {
    	pspCharInfo charInfo = new pspCharInfo();
    	if (glyphType != FONT_PGF_CHARGLYPH) {
    		return charInfo;
    	}

    	charInfo.bitmapWidth = charBitmapWidth;
    	charInfo.bitmapHeight = charBitmapHeight + 1;
    	charInfo.bitmapTop = charBitmapWidth;
    	charInfo.sfp26Width = charBitmapWidth << 6;
    	charInfo.sfp26Height = (charBitmapHeight + 1) << 6;
    	charInfo.sfp26Ascender = charBitmapHeight << 6;
    	charInfo.sfp26BearingHY = charBitmapHeight << 6;
    	charInfo.sfp26BearingVX = -480; // -7.5
    	charInfo.sfp26AdvanceH = (charBitmapWidth + 1) << 6;
    	charInfo.sfp26AdvanceV = (charBitmapHeight + 2) << 6;

    	return charInfo;
	}
}
