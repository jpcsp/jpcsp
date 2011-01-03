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
import jpcsp.HLE.modules150.sceFont;
import jpcsp.Memory;

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

    // Texture (generated from glyph).
    protected int[] texture;
    protected int texWidth;
    protected int texHeight;
    protected int texX;
    protected int texY;
    protected int texYSize;

    // Characters properties and glyphs.
    protected int n_chars;
    protected int advancex;
    protected int advancey;
    protected int charmap_compr_len;
    protected int[] charmap_compr;
    protected int[] charmap;
    protected Glyph[] glyphs;

    // Shadow characters properties and glyphs.
    protected int n_shadows;
    protected int shadowscale;
    protected Glyph[] shadowGlyphs;
    protected float size;
    protected int color;
    protected int shadowColor;

    // Tables from PGF.
    protected int[] shadowCharMap;
    protected int[] charPointerTable;
    protected int[][] advanceTable;

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
    	protected int advance;
    	protected long ptr;

        private Glyph() {
            x = 0;
            y = 0;
            w = 0;
            h = 0;
            left = 0;
            top = 0;
            flags = 0;
            shadowID = 0;
            advance = 0;
            ptr = 0;
        }

    	@Override
    	public String toString() {
    		return String.format("Glyph[x=%d, y=%d, w=%d, h=%d, left=%d, top=%d, flags=0x%X, shadowID=%d, advance=%d, ptr=%d]", x, y, w, h, left, top, flags, shadowID, advance, ptr);
    	}
    }

    public SceFontInfo(PGF fontFile) {
        // PGF.
        fileName = fontFile.getFileNamez();
        fileType = fontFile.getPGFMagic();

        // Characters/Shadow characters' variables.
        n_chars = fontFile.getCharPointerLength();
        n_shadows = fontFile.getShadowMapLength();
        charmap = new int[fontFile.getCharMapLength() * 2];
        charmap_compr_len = (fontFile.getRevision() == 3) ? 7 : 1;
        charmap_compr = new int[charmap_compr_len * 4];
        texYSize = 0;
        advancex = fontFile.getMaxAdvance()[0]/16;
        advancey = fontFile.getMaxAdvance()[1]/16;
        shadowscale = fontFile.getShadowScale()[0];
        glyphs = new Glyph[n_chars];
        shadowGlyphs = new Glyph[n_chars];

        // Texture's variables.
        texWidth = 512;
        texHeight = 512;
        texX = 1;
        texY = 1;
        size = 1.0f;
        color = 0xFFFFFFFF;
        shadowColor = 0xFF000000;
        texture = new int[texWidth * texHeight];

        // Get advance table.
        advanceTable = fontFile.getAdvanceTable();

        // Get shadow char map.
        shadowCharMap = fontFile.getShadowCharMap();

        // Get char map.
        int[] id_charmap = fontFile.getCharMap();
        for (int i = 0; i < fontFile.getCharMapLength(); i++) {
            charmap[i]= (id_charmap[i] < n_chars)? id_charmap[i] : 65535;
        }

        // Get char pointer table.
        charPointerTable = fontFile.getCharPointerTable();

        // Get raw fontdata.
        fontdata = fontFile.getFontdata();

        // Generate glyphs for all chars.
        for (int i = 0; i < n_chars; i++) {
            glyphs[i] = getGlyph(fontdata, (charPointerTable[i] * 4 * 8), FONT_PGF_CHARGLYPH, advanceTable[0]);
        }

        // Generate shadow glyphs for all chars.
        for (int i = 0; i < n_chars; i++) {
            shadowGlyphs[i] = getGlyph(fontdata, (charPointerTable[i] * 4 * 8), FONT_PGF_SHADOWGLYPH, null);
        }
    }

    // Retrieve bits from a byte buffer based on bpe.
    private int getBits(int bpe, int[] buf, int pos) {
        int v = 0;
        for (int i = 0; i < bpe; i++) {
            v += (((buf[(pos) / 8] >> ((pos) % 8) ) & 1) << i);
            pos++;
        }
        return v;
    }

    // Create and retrieve a glyph from the font data.
    private Glyph getGlyph(int[] fontdata, int charPtr, int glyphType, int[] advancemap) {
        Glyph out = new Glyph();
        if (glyphType == FONT_PGF_CHARGLYPH) {
            charPtr += 14;
        } else {
            charPtr += getBits(14, fontdata, charPtr) * 8 + 14;
        }
        out.w = getBits(7, fontdata, charPtr);
        out.h = getBits(7, fontdata, charPtr);
        out.left = getBits(7, fontdata, charPtr);
        if (out.left >= 64)
            out.left -= 128;
        out.top = getBits(7, fontdata, charPtr);
        if (out.top >= 64)
            out.top -= 128;
        out.flags = getBits(6, fontdata, charPtr);
        if (out.flags == FONT_PGF_CHARGLYPH) {
            charPtr += 7;
            out.shadowID = getBits(9, fontdata, charPtr);
            charPtr += 24 + ((out.flags == FONT_PGF_METRIC_FLAG1) ? 0 : 56)
                    + ((out.flags == FONT_PGF_METRIC_FLAG2)? 0 : 56)
                    + ((out.flags == FONT_PGF_METRIC_FLAG3)? 0 : 56);
            if(advancemap != null) {
                out.advance = advancemap[getBits(8, fontdata, charPtr) * 2] / 16;
            } else {
                out.advance = 0;
            }
        } else {
            out.shadowID = 65535;
            out.advance = 0;
        }
        out.ptr = charPtr / 8;
        return out;
    }

    // Generate a 4bpp texture for the given char id.
    private void generateFontTexture(int charId, int glyphType) {
        Glyph tmp = new Glyph();
        if (glyphType == FONT_PGF_CHARGLYPH) {
            if(charId > glyphs.length) {
                return;
            }
            tmp = glyphs[charId];
        } else {
            if(charId > shadowGlyphs.length) {
                return;
            }
            tmp = shadowGlyphs[charId];
        }
        long ptr = tmp.ptr * 8;
        if (tmp.w > 0 && tmp.h > 0) {
            if (((tmp.flags & FONT_PGF_BMP_H_ROWS) != FONT_PGF_BMP_H_ROWS)
                    || ((tmp.flags & FONT_PGF_BMP_V_ROWS) != FONT_PGF_BMP_V_ROWS)) {
                if ((texX + tmp.w + 1) > texWidth) {
                    texY += texYSize + 1;
                    texX = 1;
                }
                if ((texY + tmp.h + 1) > texHeight) {
                    texY = 1;
                    texX = 1;
                }
                tmp.x = texX;
                tmp.y = texY;
                int nibble = 0;
                int value = 0;
                for (int i = 0; i < (tmp.w * tmp.h); i++) {
                    nibble = getBits(4, fontdata, (int) ptr);
                    if (nibble < 8) {
                        value = getBits(4, fontdata, (int) ptr);
                    }
                    for (int j = 0, xx = 0, yy = 0; (j <= ((nibble < 8) ? (nibble) : (15 - nibble))) && (i < (tmp.w * tmp.h)); j++) {
                        if (nibble >= 8) {
                            value = getBits(4, fontdata, (int) ptr);
                        }
                        if ((tmp.flags & FONT_PGF_BMP_H_ROWS) == FONT_PGF_BMP_H_ROWS) {
                            xx = i % tmp.w;
                            yy = i / tmp.h;
                        } else {
                            xx = i / tmp.h;
                            yy = i % tmp.h;
                        }
                        if (((texX + xx) & 1) == (texX + xx)) {
                            texture[((texX + xx) + (texY + yy) * texWidth) >> 1] &= 0x0F;
                            texture[((texX + xx) + (texY + yy) * texWidth) >> 1] |= (value << 4);
                        } else {
                            texture[((texX + xx) + (texY + yy) * texWidth) >> 1] &= 0xF0;
                            texture[((texX + xx) + (texY + yy) * texWidth) >> 1] |= (value);
                        }
                    }
                }
            }
        }
    }

    public void printFont(int base, char c) {
        Memory mem = Memory.getInstance();
        if (sceFont.getAlternateChar() * 8 >= texture.length) {
            sceFont.setAlternateChar('?');
        }
        int fontBaseIndex = c * 8;
        if (fontBaseIndex >= texture.length) {
            fontBaseIndex = sceFont.getAlternateChar() * 8;
        }
        generateFontTexture(c, FONT_PGF_CHARGLYPH);
        for (int i = 0; i < texture.length; i++) {
            mem.write8(base + i, (byte)texture[i]);
        }
    }
}