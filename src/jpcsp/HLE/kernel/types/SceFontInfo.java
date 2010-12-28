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

/*
 * SceFontInfo struct based on BenHur's intraFont application.
 * This struct is used to give an easy and organized access to the PGF data.
 *
 * TODO: Generate the respective textures for each glyph.
 */

public class SceFontInfo {
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
    protected Glyph glyph;

    // Shadow characters properties and glyphs.
    protected int n_shadows;
    protected int shadowscale;
    protected Glyph shadowGlyph;
    protected float size;
    protected int color;
    protected int shadowColor;

    // Tables from PGF.
    protected int[] shadowCharMap;
    protected int[] charPointerTable;

    public SceFontInfo(PGF fontFile) {
        // Parse the file and fill the structs parameters
        // in a similar way as the one used in intraFont.

        // PGF.
        fileName = fontFile.getFileNamez();
        fileType = fontFile.getPGFMagic();

        // Characters/Shadow characters' variables.
        n_chars = fontFile.getCharPointerLength();
        charmap_compr_len = (fontFile.getRevision() == 3) ? 7 : 1;
        texYSize = 0;
        advancex = fontFile.getMaxAdvance()[0]/16;
        advancey = fontFile.getMaxAdvance()[1]/16;
        n_shadows = fontFile.getShadowMapLength();
        shadowscale = fontFile.getShadowScale()[0];
        glyph = new Glyph();
        shadowGlyph = new Glyph();
        charmap_compr = new int[fontFile.getCompCharMapLength()];
        charmap = new int[fontFile.getCharMapLength()];

        // Texture's variables (using 512 as default size for now).
        texWidth = 512;
        texHeight = 512;
        texX = 1;
        texY = 1;
        size = 1.0f;
        color = 0xFFFFFFFF;
        shadowColor = 0xFF000000;
        texture = new int[texWidth*texHeight];

        // Convert and retrieve the necessary tables.
        shadowCharMap = getTable(fontFile.getShadowCharMap(), fontFile.getShadowMapLength(), fontFile.getShadowMapBpe());
        if(fontFile.getCharMapBpe() == 16) {
            charmap = fontFile.getCharMap();
        }
        else {
            charmap = getTable(fontFile.getCharMap(), fontFile.getCharMapLength(), fontFile.getCharMapBpe());
        }
        charPointerTable = getTable(fontFile.getCharPointerTable(), fontFile.getCharPointerLength(), fontFile.getCharPointerBpe());

        // Get the font data.
        fontdata = fontFile.getFontdata();
    }

    // Retrieve bits from a byte buffer based on bpe.
    public int getBits(int bpe, int[] buf, int pos) {
        int v = 0;
        for(int i = 0; i < bpe; i++) {
            v += (((buf[(pos)/8] >> ((pos)%8) ) & 1) << i);
            pos++;
        }
        return v;
    }

    // Convert raw table into the correct bpe sized result.
    public int[] getTable(int[] rawTable, int tableLenght, int tableBpe) {
        int[] newTable = new int[tableLenght];
        for(int i = 0; i < tableLenght; i++) {
            newTable[i] = getBits(tableBpe, rawTable, i);
        }
        return newTable;
    }

    // Create and retrieve a glyph from the font data.
    protected Glyph getGlyph(int[] fontdata, int charPtr, int glyphType, int[] advancemap) {
        Glyph out = new Glyph();
        if (glyphType == 0x20) {
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

        if (out.flags == 0x20) {
            charPtr += 7;
            out.shadowID = getBits(9, fontdata, charPtr);
            charPtr += 24 + ((out.flags == 0x04) ? 0 : 56) + ((out.flags == 0x08)? 0 : 56) + ((out.flags == 0x10)? 0 : 56);
            out.advance = advancemap[getBits(8, fontdata, charPtr) * 2] / 16;
        } else {
            out.shadowID = 65535;
            out.advance = 0;
        }
        out.ptr = charPtr / 8;

        return out;
    }

    // Glyph class.
    private static class Glyph {
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
}