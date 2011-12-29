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

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules150.sceFont;

/**
 * @author gid15
 *
 */
public class Debug {
	public static final int fontPixelSize = 2;

	// Pixel sizes in bytes for the font:
	// FONT_PIXELFORMAT_4 :    0 (means 2 pixels per byte)
	// FONT_PIXELFORMAT_4_REV: 0 (means 2 pixels per byte)
	// FONT_PIXELFORMAT_8 :    1 byte
	// FONT_PIXELFORMAT_24:    3 bytes
	// FONT_PIXELFORMAT_32:    4 bytes
	private static final int[] fontPixelSizeInBytes = { 0, 0, 1, 3, 4 }; // 0 means 2 pixels per byte

    // For sceFont.
    // Use this function to print a char using the font buffer's dimensions.
    public static void printFontbuffer(int base, int bpl, int bufWidth, int bufHeight, int x, int y, int pixelformat, int charCode, int altCharCode) {
        if (Modules.log.isInfoEnabled()) {
        	Modules.log.info(String.format("printFontbuffer 0x%04X '%c' (%d, %d)", charCode, (char) charCode, x, y));
        }

        if (Font.font == null) {
			// Debug font not available...
        	return;
        }

        int fontBaseIndex = charCode * Font.charSize;
        if (fontBaseIndex >= Font.font.length || isFontCharNull(fontBaseIndex)) {
            fontBaseIndex = altCharCode * Font.charSize;
            if (fontBaseIndex >= Font.font.length || isFontCharNull(fontBaseIndex)) {
            	return;
            }
        }

        int pixelColor0 = getFontPixelColor(0x00000000, pixelformat);
		int pixelColor1 = getFontPixelColor(0xFFFFFFFF, pixelformat);
        for (int i = 0; i < Font.charHeight; i++) {
            for (int j = 0; j < Font.charWidth; j++) {
                int pixel = Font.font[fontBaseIndex + i] & (128 >> j);
                int pixelColor = (pixel != 0) ? pixelColor1 : pixelColor0;
                for (int pixelY = 0; pixelY < fontPixelSize; pixelY++) {
                	for (int pixelX = 0; pixelX < fontPixelSize; pixelX++) {
                        setFontPixel(base, bpl, bufWidth, bufHeight, x + j * fontPixelSize + pixelX, y + i * fontPixelSize + pixelY, pixelColor, pixelformat);
                	}
                }
            }
        }
    }

    private static boolean isFontCharNull(int index) {
        for (int i = 0; i < Font.charHeight; i++) {
            if (Font.font[index + i] != 0x00) {
            	return false;
            }
        }
        return true;
    }

    public static void setFontPixel(int base, int bpl, int bufWidth, int bufHeight, int x, int y, int pixelColor, int pixelformat) {
    	if (x < 0 || x >= bufWidth || y < 0 || y >= bufHeight) {
    		return;
    	}

		int pixelBytes = getFontPixelBytes(pixelformat);
		// pixelBytes == 0 means 2 pixels per byte
		int bufMaxWidth = (pixelBytes == 0 ? bpl * 2 : bpl / pixelBytes);
		if (x >= bufMaxWidth) {
			return;
		}

		int framebufferAddr = base + (y * bpl) + (pixelBytes == 0 ? x / 2 : x * pixelBytes);

    	Memory mem = Memory.getInstance();
        switch (pixelformat) {
        	case sceFont.PSP_FONT_PIXELFORMAT_4:
        	case sceFont.PSP_FONT_PIXELFORMAT_4_REV: {
        		int oldColor = mem.read8(framebufferAddr);
        		int newColor;
        		if ((x & 1) != pixelformat) {
        			newColor = (pixelColor << 4) | (oldColor & 0xF);
        		} else {
        			newColor = (oldColor & 0xF0) | pixelColor;
        		}
        		mem.write8(framebufferAddr, (byte) newColor);
        		break;
        	}
        	case sceFont.PSP_FONT_PIXELFORMAT_8: {
        		mem.write8(framebufferAddr, (byte) pixelColor);
        		break;
        	}
        	case sceFont.PSP_FONT_PIXELFORMAT_24: {
        		mem.write8(framebufferAddr + 0, (byte) (pixelColor >>  0));
        		mem.write8(framebufferAddr + 1, (byte) (pixelColor >>  8));
        		mem.write8(framebufferAddr + 2, (byte) (pixelColor >> 16));
        		break;
        	}
        	case sceFont.PSP_FONT_PIXELFORMAT_32: {
        		mem.write32(framebufferAddr, pixelColor);
        		break;
        	}
        }
	}

    private static int getFontPixelBytes(int pixelformat) {
    	if (pixelformat >= 0 && pixelformat < fontPixelSizeInBytes.length) {
    		return fontPixelSizeInBytes[pixelformat];
    	}

    	Modules.log.warn("Unknown pixel format for sceFont: " + pixelformat);
    	return 1;
    }

	public static int getFontPixelColor(int color, int pixelformat) {
		switch (pixelformat) {
			case sceFont.PSP_FONT_PIXELFORMAT_4:
			case sceFont.PSP_FONT_PIXELFORMAT_4_REV:
				// Use only 4-bit alpha
				color = (color >> 28) & 0xF;
				break;
			case sceFont.PSP_FONT_PIXELFORMAT_8:
				// Use only 8-bit alpha
				color = (color >> 24) & 0xFF;
				break;
			case sceFont.PSP_FONT_PIXELFORMAT_24:
				// Use RGB with 8-bit values
				color = color & 0x00FFFFFF;
				break;
			case sceFont.PSP_FONT_PIXELFORMAT_32:
				// Use RGBA with 8-bit values
				break;
		}

		return color;
	}

	public static void printFramebuffer(int base, int bufferwidth, int x, int y, int colorFg, int colorBg, int pixelformat, String s) {
		printFramebuffer(base, bufferwidth, x, y, colorFg, colorBg, pixelformat, 1, s);
	}

	public static void printFramebuffer(int base, int bufferwidth, int x, int y, int colorFg, int colorBg, int pixelformat, int size, String s) {
		if (Font.font == null) {
			// Debug font not available...
			return;
		}

		int length = s.length();
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (c == '\n') {
				x = 0;
				y += Font.charHeight * size;
			} else {
				printFramebuffer(base, bufferwidth, x, y, colorFg, colorBg, pixelformat, size, c);
				x += Font.charWidth * size;
			}
		}
	}

	private static void printFramebuffer(int base, int bufferwidth, int x, int y, int colorFg, int colorBg, int pixelformat, int size, char c) {
		int fontBaseIndex = c * 8;
		for (int i = 0; i < Font.charHeight; i++) {
			for (int j = 0; j < Font.charWidth; j++) {
				int pixel = Font.font[fontBaseIndex + i] & (128 >> j);
				if (pixel != 0) {
					setPixel(base, bufferwidth, x + j * size, y + i * size, colorFg, pixelformat, size);
				} else if (colorBg != 0) {
					setPixel(base, bufferwidth, x + j * size, y + i * size, colorBg, pixelformat, size);
				}
			}
		}
	}

	private static void setPixel(int base, int bufferwidth, int x, int y, int color, int pixelformat, int size) {
		Memory mem = Memory.getInstance();
		int pixelBytes = jpcsp.HLE.modules150.sceDisplay.getPixelFormatBytes(pixelformat);
		int framebufferAddr = base + (y * bufferwidth + x) * pixelBytes;
		int pixelColor = getPixelColor(color, pixelformat);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (pixelBytes == 4) {
					mem.write32(framebufferAddr + j * pixelBytes, pixelColor);
				} else if (pixelBytes == 2) {
					mem.write16(framebufferAddr + j * pixelBytes, (short) pixelColor);
				}
			}
			framebufferAddr += bufferwidth * pixelBytes;
		}
	}

	public static int getPixelColor(int color, int pixelformat) {
		switch (pixelformat) {
			case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				color = ((color & 0x00F80000) >> 8) | ((color & 0x0000FC00) >> 5) | ((color & 0x000000F8) >> 3);
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				color = ((color & 0x80000000) >> 16) | ((color & 0x00F80000) >> 9) | ((color & 0x0000F800) >> 6) | ((color & 0x000000F8) >> 3);
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				color = ((color & 0xF0000000) >> 16) | ((color & 0x00F00000) >> 12) | ((color & 0x0000F000) >> 8) | ((color & 0x000000F0) >> 4);
				break;
			case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				break;
		}
		return color;
	}

	public static class Font {
        public static int charSize = 8;
		public static int charWidth = 8;
		public static int charHeight = 8;
        public static char[] font = null;

        public static void setDebugFont(char[] newFont) {
            font = newFont;
        }
        public static void setDebugCharWidth(int width) {
            charWidth = width;
        }
        public static void setDebugCharHeight(int height) {
            charHeight = height;
        }
        public static void setDebugCharSize(int size) {
            charSize = size;
        }
	}
}