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

import jpcsp.format.PGF;
import jpcsp.util.Debug;
import jpcsp.HLE.modules150.sceFont;

/*
 * SceFontInfo struct based on BenHur's intraFont application.
 * This struct is used to give an easy and organized access to the PGF data.
 */

public class SceFontInfo {
    // Statics based on intraFont's findings.
    public static final int FONT_FILETYPE_PGF = 0x00;
    public static final int FONT_FILETYPE_BWFON = 0x01;
    public static final int FONT_PGF_BMP_H_ROWS = 0x01;
    public static final int FONT_PGF_BMP_V_ROWS = 0x02;
    public static final int FONT_PGF_BMP_OVERLAY = 0x03;
    public static final int FONT_PGF_METRIC_FLAG1 = 0x04;
    public static final int FONT_PGF_METRIC_FLAG2 = 0x08;
    public static final int FONT_PGF_METRIC_FLAG3 = 0x10;
    public static final int FONT_PGF_CHARGLYPH = 0x20;
    public static final int FONT_PGF_SHADOWGLYPH = 0x40;

    // PGF file.
    protected String fileName;  // The PGF file name.
    protected String fileType;  // The file type (only PGF support for now).
    protected int[] fontdata;   // Fontdata extracted from the PGF.
    protected long fontdataBits;

    // Characters properties and glyphs.
    protected int advancex;
    protected int advancey;
    protected int charmap_compr_len;
    protected int[] charmap_compr;
    protected int[] charmap;
    protected Glyph[] glyphs;
    protected int firstGlyph;

    // Shadow characters properties and glyphs.
    protected int shadowscale;
    protected Glyph[] shadowGlyphs;

    // Font style from registry
    protected pspFontStyle fontStyle;

    // Glyph class.
    protected static class Glyph {
    	protected int x;
    	protected int y;
    	protected int w;
    	protected int h;
    	protected int left;
    	protected int top;
    	protected int flags;
    	protected int shadowID;
    	protected int advanceH;
    	protected int advanceV;
    	protected int dimensionWidth, dimensionHeight;
    	protected int xAdjustH, xAdjustV;
    	protected int yAdjustH, yAdjustV;
    	protected long ptr;

        public boolean hasFlag(int flag) {
        	return (flags & flag) == flag;
        }

        @Override
    	public String toString() {
    		return String.format("Glyph[x=%d, y=%d, w=%d, h=%d, left=%d, top=%d, flags=0x%X, shadowID=%d, advance=%d, ptr=%d]", x, y, w, h, left, top, flags, shadowID, advanceH, ptr);
    	}
    }

    private int[] getTable(int[] rawTable, int bpe, int length) {
    	int[] table = new int[length];
    	for (int i = 0, bitPtr = 0; i < length; i++, bitPtr += bpe) {
    		table[i] = getBits(bpe, rawTable, bitPtr);
    	}

    	return table;
    }

    public SceFontInfo(PGF fontFile) {
        // PGF.
        fileName = fontFile.getFileNamez();
        fileType = fontFile.getPGFMagic();

        // Characters/Shadow characters' variables.
        charmap = new int[fontFile.getCharMapLength() * 2];
        charmap_compr_len = (fontFile.getRevision() == 3) ? 7 : 1;
        charmap_compr = new int[charmap_compr_len * 4];
        advancex = fontFile.getMaxAdvance()[0]/16;
        advancey = fontFile.getMaxAdvance()[1]/16;
        shadowscale = fontFile.getShadowScale()[0];
        glyphs = new Glyph[fontFile.getCharPointerLength()];
        shadowGlyphs = new Glyph[fontFile.getShadowMapLength()];
        firstGlyph = fontFile.getFirstGlyphInCharMap();

        // Get char map.
        int[] rawCharMap = fontFile.getCharMap();
        for (int i = 0; i < fontFile.getCharMapLength(); i++) {
        	charmap[i] = getBits(fontFile.getCharMapBpe(), rawCharMap, i * fontFile.getCharMapBpe());
        	if (charmap[i] >= glyphs.length) {
        		charmap[i] = 65535;
        	}
        }

        // Get raw fontdata.
        fontdata = fontFile.getFontdata();
        fontdataBits = fontdata.length * 8L;

        int[] charPointers = getTable(fontFile.getCharPointerTable(), fontFile.getCharPointerBpe(), glyphs.length);
        int[] shadowMap = getTable(fontFile.getShadowCharMap(), fontFile.getShadowMapBpe(), shadowGlyphs.length);

        // Generate glyphs for all chars.
        for (int i = 0; i < glyphs.length; i++) {
            glyphs[i] = getGlyph(fontdata, (charPointers[i] * 4 * 8), FONT_PGF_CHARGLYPH, fontFile);
        }

        // Generate shadow glyphs for all chars.
        for (int i = 0; i < glyphs.length; i++) {
        	int shadowId = glyphs[i].shadowID;
        	int charId = shadowMap[shadowId];
        	if (charId >= 0 && charId < glyphs.length) {
        		if (shadowGlyphs[shadowId] == null) {
        			shadowGlyphs[shadowId] = getGlyph(fontdata, (charPointers[charId] * 4 * 8), FONT_PGF_SHADOWGLYPH, fontFile);
        		}
        	}
        }
    }

    // Retrieve bits from a byte buffer based on bpe.
    private int getBits(int bpe, int[] buf, long pos) {
        int v = 0;
        for (int i = 0; i < bpe; i++) {
            v += (((buf[(int) (pos / 8)] >> ((pos) % 8) ) & 1) << i);
            pos++;
        }
        return v;
    }

    private boolean isIncorrectFont(PGF fontFile) {
    	// Fonts created by ttf2pgf (e.g. default Jpcsp fonts)
    	// do not contain complete Glyph information.
    	String fontName = fontFile.getFontName();
    	return fontName.startsWith("Liberation") || fontName.startsWith("Sazanami") || fontName.startsWith("UnDotum");
    }

    // Create and retrieve a glyph from the font data.
    private Glyph getGlyph(int[] fontdata, long charPtr, int glyphType, PGF fontFile) {
    	Glyph glyph = new Glyph();
        if (glyphType == FONT_PGF_SHADOWGLYPH) {
            if (charPtr + 96 > fontdataBits) {
        		return null;
        	}
            // First 14 bits are offset to shadow glyph
            charPtr += getBits(14, fontdata, charPtr) * 8;
        }
        if (charPtr + 96 > fontdataBits) {
    		return null;
    	}

        charPtr += 14;

        glyph.w = getBits(7, fontdata, charPtr);
        charPtr += 7;

        glyph.h = getBits(7, fontdata, charPtr);
        charPtr += 7;

        glyph.left = getBits(7, fontdata, charPtr);
        charPtr += 7;
        if (glyph.left >= 64) {
            glyph.left -= 128;
        }

        glyph.top = getBits(7, fontdata, charPtr);
        charPtr += 7;
        if (glyph.top >= 64) {
            glyph.top -= 128;
        }

        glyph.flags = getBits(6, fontdata, charPtr);
        charPtr += 6;

        if (glyph.hasFlag(FONT_PGF_CHARGLYPH)) {
        	// Skip magic number
            charPtr += 7;

            glyph.shadowID = getBits(9, fontdata, charPtr);
            charPtr += 9;

            int dimensionIndex = getBits(8, fontdata, charPtr);
            charPtr += 8;

            int xAdjustIndex = getBits(8, fontdata, charPtr);
            charPtr += 8;

            int yAdjustIndex = getBits(8, fontdata, charPtr);
            charPtr += 8;

            charPtr += (glyph.hasFlag(FONT_PGF_METRIC_FLAG1) ? 0 : 56) +
                       (glyph.hasFlag(FONT_PGF_METRIC_FLAG2) ? 0 : 56) +
                       (glyph.hasFlag(FONT_PGF_METRIC_FLAG3) ? 0 : 56);

        	int advanceIndex = getBits(8, fontdata, charPtr);
            charPtr += 8;

            if (dimensionIndex < fontFile.getDimensionTableLength()) {
                int[][] dimensionTable = fontFile.getDimensionTable();
                glyph.dimensionWidth = dimensionTable[0][dimensionIndex];
                glyph.dimensionHeight = dimensionTable[1][dimensionIndex];
            }

            if (xAdjustIndex < fontFile.getXAdjustTableLength()) {
                int[][] xAdjustTable = fontFile.getXAdjustTable();
                glyph.xAdjustH = xAdjustTable[0][xAdjustIndex];
                glyph.xAdjustV = xAdjustTable[1][xAdjustIndex];
            }

            if (yAdjustIndex < fontFile.getYAdjustTableLength()) {
                int[][] yAdjustTable = fontFile.getYAdjustTable();
                glyph.yAdjustH = yAdjustTable[0][yAdjustIndex];
                glyph.yAdjustV = yAdjustTable[1][yAdjustIndex];
            }

            if (dimensionIndex == 0 && xAdjustIndex == 0 && yAdjustIndex == 0 && isIncorrectFont(fontFile)) {
            	// Fonts created by ttf2pgf do not contain complete Glyph information.
            	// Provide default values.
            	glyph.dimensionWidth = glyph.w << 6;
            	glyph.dimensionHeight = glyph.h << 6;
            	glyph.xAdjustH = glyph.left << 6;
            	glyph.xAdjustV = glyph.left << 6;
            	glyph.yAdjustH = glyph.top << 6;
            	glyph.yAdjustV = glyph.top << 6;
            }

            if (advanceIndex < fontFile.getAdvanceTableLength()) {
            	int[][] advanceTable = fontFile.getAdvanceTable();
                glyph.advanceH = advanceTable[0][advanceIndex];
                glyph.advanceV = advanceTable[1][advanceIndex];
            }
        } else {
            glyph.shadowID = 65535;
            glyph.advanceH = 0;
        }

        glyph.ptr = charPtr / 8;

        return glyph;
    }

    private Glyph getCharGlyph(int charCode, int glyphType) {
    	if (charCode < firstGlyph) {
    		return null;
    	}

    	charCode -= firstGlyph;
    	if (charCode < charmap.length) {
    		charCode = charmap[charCode];
    	}

    	Glyph glyph;
        if (glyphType == FONT_PGF_CHARGLYPH) {
            if (charCode >= glyphs.length) {
                return null;
            }
            glyph = glyphs[charCode];
        } else {
            if (charCode >= shadowGlyphs.length) {
                return null;
            }
            glyph = shadowGlyphs[charCode];
        }

        return glyph;
    }

    // Generate a 4bpp texture for the given char id.
    private void generateFontTexture(int base, int bpl, int bufWidth, int bufHeight, int x, int y, int pixelformat, int charCode, int altCharCode, int glyphType) {
    	Glyph glyph = getCharGlyph(charCode, glyphType);
    	if (glyph == null) {
    		// No Glyph available for this charCode, try to use the alternate char.
            charCode = altCharCode;
            glyph = getCharGlyph(charCode, glyphType);
            if (glyph == null) {
            	return;
            }
    	}

        if (glyph.w <= 0 || glyph.h <= 0) {
        	return;
        }
        if (((glyph.flags & FONT_PGF_BMP_OVERLAY) != FONT_PGF_BMP_H_ROWS) &&
            ((glyph.flags & FONT_PGF_BMP_OVERLAY) != FONT_PGF_BMP_V_ROWS)) {
        	return;
        }

    	long bitPtr = glyph.ptr * 8;
        final int nibbleBits = 4;
        int nibble;
        int value = 0;
        int xx, yy, count;
        boolean bitmapHorizontalRows = (glyph.flags & FONT_PGF_BMP_OVERLAY) == FONT_PGF_BMP_H_ROWS;
        int numberPixels = glyph.w * glyph.h;
        int pixelIndex = 0;
        while (pixelIndex < numberPixels && bitPtr + 8 < fontdataBits) {
            nibble = getBits(nibbleBits, fontdata, bitPtr);
            bitPtr += nibbleBits;

            if (nibble < 8) {
                value = getBits(nibbleBits, fontdata, bitPtr);
                bitPtr += nibbleBits;
                count = nibble + 1;
            } else {
            	count = 16 - nibble;
            }

            for (int i = 0; i < count && pixelIndex < numberPixels; i++) {
                if (nibble >= 8) {
                    value = getBits(nibbleBits, fontdata, bitPtr);
                    bitPtr += nibbleBits;
                }

                if (bitmapHorizontalRows) {
                    xx = pixelIndex % glyph.w;
                    yy = pixelIndex / glyph.w;
                } else {
                    xx = pixelIndex / glyph.h;
                    yy = pixelIndex % glyph.h;
                }

                // 4-bit color value
                int pixelColor = value;
                switch (pixelformat) {
                	case sceFont.PSP_FONT_PIXELFORMAT_8:
                        // 8-bit color value
                		pixelColor |= pixelColor << 4;
                		break;
                	case sceFont.PSP_FONT_PIXELFORMAT_24:
                        // 24-bit color value
                		pixelColor |= pixelColor << 4;
                		pixelColor |= pixelColor << 8;
                		pixelColor |= pixelColor << 8;
                		break;
                	case sceFont.PSP_FONT_PIXELFORMAT_32:
                        // 32-bit color value
    					pixelColor |= pixelColor << 4;
    					pixelColor |= pixelColor << 8;
    					pixelColor |= pixelColor << 16;
    					break;
                }
                Debug.setFontPixel(base, bpl, bufWidth, bufHeight, x + xx, y + yy, pixelColor, pixelformat);
        		pixelIndex++;
            }
        }
    }

    public void printFont(int base, int bpl, int bufWidth, int bufHeight, int x, int y, int pixelformat, int charCode, int altCharCode) {
        generateFontTexture(base, bpl, bufWidth, bufHeight, x, y, pixelformat, charCode, altCharCode, FONT_PGF_CHARGLYPH);
    }

    public pspCharInfo getCharInfo(int charCode) {
    	pspCharInfo charInfo = new pspCharInfo();
    	Glyph glyph = getCharGlyph(charCode, FONT_PGF_CHARGLYPH);
    	if (glyph == null) {
    		return null;
    	}

    	charInfo.bitmapWidth = glyph.w;
    	charInfo.bitmapHeight = glyph.h;
    	charInfo.bitmapLeft = glyph.left;
    	charInfo.bitmapTop = glyph.top;
    	charInfo.sfp26Width = glyph.dimensionWidth;
    	charInfo.sfp26Height = glyph.dimensionHeight;
    	charInfo.sfp26Ascender = glyph.top << 6;
    	charInfo.sfp26Descender = (glyph.h - glyph.top) << 6;
    	charInfo.sfp26BearingHX = glyph.xAdjustH;
    	charInfo.sfp26BearingHY = glyph.yAdjustH;
    	charInfo.sfp26BearingVX = glyph.xAdjustV;
    	charInfo.sfp26BearingVY = glyph.yAdjustV;
    	charInfo.sfp26AdvanceH = glyph.advanceH;
    	charInfo.sfp26AdvanceV = glyph.advanceV;

    	return charInfo;
    }

	public pspFontStyle getFontStyle() {
		return fontStyle;
	}

	public void setFontStyle(pspFontStyle fontStyle) {
		this.fontStyle = fontStyle;
	}
}