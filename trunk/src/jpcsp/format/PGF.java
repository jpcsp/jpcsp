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

import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.readUByte;
import static jpcsp.util.Utilities.readUHalf;
import static jpcsp.util.Utilities.readWord;
import static jpcsp.util.Utilities.skipUnknown;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.HLE.ISerializeString;
import jpcsp.HLE.ISerializeStruct;
import jpcsp.HLE.kernel.types.SceFontInfo;

public class PGF {
	@ISerializeStruct(size = 168)
	static public class FontStyle {
		public static final int FONT_FAMILY_SANS_SERIF = 1;
		public static final int FONT_FAMILY_SERIF      = 2;

		public static final int FONT_STYLE_REGULAR     = 1;
		public static final int FONT_STYLE_ITALIC      = 2;
		public static final int FONT_STYLE_BOLD        = 5;
		public static final int FONT_STYLE_BOLD_ITALIC = 6;
		public static final int FONT_STYLE_DB          = 103; // Demi-Bold / semi-bold

		public static final int FONT_LANGUAGE_JAPANESE = 1;
		public static final int FONT_LANGUAGE_LATIN    = 2;
		public static final int FONT_LANGUAGE_KOREAN   = 3;

		public float fontH;
		public float fontV;
		public float fontHRes;
		public float fontVRes;
		public float fontWeight;
		public short fontFamily;
		public short fontStyle;
		public short fontStyleSub;
		public short fontLanguage;
		public short fontRegion;
		public short fontCountry;
		@ISerializeString(size = 64) String fontName;
		public int fontAttributes;
		public int fontExpire;
	}
	
	@ISerializeStruct(size = 264)
	static public class Info {
		// Glyph metrics
		public int maxGlyphWidthI;
		public int maxGlyphHeightI;
		public int maxGlyphAscenderI;
		public int maxGlyphDescenderI;
		public int maxGlyphLeftXI;
		public int maxGlyphBaseYI;
		public int minGlyphCenterXI;
		public int maxGlyphTopYI;
		public int maxGlyphAdvanceXI;
		public int maxGlyphAdvanceYI;
		// Glyph metrics (replicated as float).
		public float maxGlyphWidthF;
		public float maxGlyphHeightF;
		public float maxGlyphAscenderF;
		public float maxGlyphDescenderF;
		public float maxGlyphLeftXF;
		public float maxGlyphBaseYF;
		public float minGlyphCenterXF;
		public float maxGlyphTopYF;
		public float maxGlyphAdvanceXF;
		public float maxGlyphAdvanceYF;
		// Bitmap dimensions.
		public short maxGlyphWidth;
		public short maxGlyphHeight;
		public int charMapLength; // Number of elements in the font's charmap.
		public int shadowMapLength;   // Number of elements in the font's shadow charmap.
		public FontStyle fontStyle;
		public int Bpp = 4;
	}

    protected int headerOffset;
	protected int headerSize;

	protected String PGFMagic;
	protected int revision;
	protected int version;

	protected int charMapLength;
	protected int charPointerLength;
	protected int charMapBpe;
	protected int charPointerBpe;

	protected int bpp;
	protected int hSize;
	protected int vSize;
	protected int hResolution;
	protected int vResolution;

	protected String fontName;
	protected String fontType;

    protected int firstGlyph;
    protected int lastGlyph;

    protected int maxAscender;
    protected int maxDescender;
    protected int maxLeftXAdjust;
    protected int maxBaseYAdjust;
    protected int minCenterXAdjust;
    protected int maxTopYAdjust;

    protected int[] maxAdvance = new int[2];
    protected int[] maxSize = new int[2];
    protected int maxGlyphWidth;
    protected int maxGlyphHeight;

    protected int dimTableLength;
    protected int xAdjustTableLength;
    protected int yAdjustTableLength;
    protected int advanceTableLength;

    protected int shadowMapLength;
    protected int shadowMapBpe;
    protected int[] shadowScale = new int[2];

    protected int compCharMapBpe1;
    protected int compCharMapLength1;
    protected int compCharMapBpe2;
    protected int compCharMapLength2;

    protected int[][] dimensionTable;
    protected int[][] xAdjustTable;
    protected int[][] yAdjustTable;
    protected int[][] charmapCompressionTable1;
    protected int[][] charmapCompressionTable2;
    protected int[][] advanceTable;
    protected int[] shadowCharMap;
    protected int[] charMap;
    protected int[] charPointerTable;

    protected int[] fontData;
    protected int fontDataOffset;
    protected int fontDataLength;

    protected String fileNamez = "";

    protected PGF() {
    }

    public PGF(ByteBuffer f) throws IOException {
        read(f);
    }

    private void read(ByteBuffer f) throws IOException {
        if (f.capacity() == 0) {
            return;
        }

        // PGF Header.
        headerOffset =  readUHalf(f);
        headerSize = readUHalf(f);

        // Offset 4
        PGFMagic = readStringNZ(f, 4);  // PGF0.
        revision = readWord(f);
        version = readWord(f);

        // Offset 16
        charMapLength = readWord(f);
        charPointerLength = readWord(f);
        charMapBpe = readWord(f);
        charPointerBpe = readWord(f);
        skipUnknown(f, 2);

        // Offset 34
        bpp = readUByte(f);
        skipUnknown(f, 1);

        // Offset 36
        hSize = readWord(f);
        vSize = readWord(f);
        hResolution = readWord(f);
        vResolution = readWord(f);
        skipUnknown(f, 1);

        // Offset 53
        fontName = readStringNZ(f, 64);
        fontType = readStringNZ(f, 64);
        skipUnknown(f, 1);

        // Offset 182
        firstGlyph = readUHalf(f);
        lastGlyph = readUHalf(f);
        skipUnknown(f, 26);

        // Offset 212
        maxAscender = readWord(f);
        maxDescender = readWord(f);
        maxLeftXAdjust = readWord(f);
        maxBaseYAdjust = readWord(f);
        minCenterXAdjust = readWord(f);
        maxTopYAdjust = readWord(f);

        // Offset 236
        maxAdvance[0] = readWord(f);
        maxAdvance[1] = readWord(f);
        maxSize[0] = readWord(f);
        maxSize[1] = readWord(f);
        maxGlyphWidth = readUHalf(f);
        maxGlyphHeight = readUHalf(f);
        skipUnknown(f, 2);

        // Offset 258
        dimTableLength= readUByte(f);
        xAdjustTableLength = readUByte(f);
        yAdjustTableLength = readUByte(f);
        advanceTableLength = readUByte(f);
        skipUnknown(f, 102);  // NULL.

        // Offset 364
        shadowMapLength = readWord(f);
        shadowMapBpe = readWord(f);
        skipUnknown(f, 4);   // 24.0625.
        shadowScale[0] = readWord(f);
        shadowScale[1] = readWord(f);
        skipUnknown(f, 8);   // 15.0.

        // Offset 392
        if (revision == 3) {
            compCharMapBpe1 = readWord(f);
            compCharMapLength1 = readUHalf(f);
            skipUnknown(f, 2);
            compCharMapBpe2 = readWord(f);
            compCharMapLength2 = readUHalf(f);
            skipUnknown(f, 6);
        }

        // PGF Tables.
        dimensionTable = new int[2][dimTableLength];
        for(int i = 0; i < dimTableLength; i++) {
            dimensionTable[0][i] = readWord(f);
            dimensionTable[1][i] = readWord(f);
        }

        xAdjustTable = new int[2][xAdjustTableLength];
        for(int i = 0; i < xAdjustTableLength; i++) {
            xAdjustTable[0][i] = readWord(f);
            xAdjustTable[1][i] = readWord(f);
        }

        yAdjustTable = new int[2][yAdjustTableLength];
        for(int i = 0; i < yAdjustTableLength; i++) {
            yAdjustTable[0][i] = readWord(f);
            yAdjustTable[1][i] = readWord(f);
        }

        advanceTable = new int[2][advanceTableLength];
        for(int i = 0; i < advanceTableLength; i++) {
            advanceTable[0][i] = readWord(f);
            advanceTable[1][i] = readWord(f);
        }

        int shadowCharMapSize = ((shadowMapLength * shadowMapBpe + 31) & ~31) / 8;
        shadowCharMap = new int[shadowCharMapSize];
        for(int i = 0; i < shadowCharMapSize; i++) {
            shadowCharMap[i] = readUByte(f);
        }

        if(revision == 3) {
            charmapCompressionTable1 = new int[2][compCharMapLength1];
            for(int i = 0; i < compCharMapLength1; i++) {
                charmapCompressionTable1[0][i] = readUHalf(f);
                charmapCompressionTable1[1][i] = readUHalf(f);
            }

            charmapCompressionTable2 = new int[2][compCharMapLength2];
            for(int i = 0; i < compCharMapLength2; i++) {
                charmapCompressionTable2[0][i] = readUHalf(f);
                charmapCompressionTable2[1][i] = readUHalf(f);
            }
        }

        int charMapSize = ((charMapLength * charMapBpe + 31) & ~31) / 8;
        charMap = new int[charMapSize];
        for(int i = 0; i < charMapSize; i++) {
            charMap[i] = readUByte(f);
        }

        int charPointerSize = (((charPointerLength * charPointerBpe + 31) & ~31) / 8);
        charPointerTable = new int[charPointerSize];
        for(int i = 0; i < charPointerSize; i++) {
            charPointerTable[i] = readUByte(f);
        }

        // PGF Fontdata.
        fontDataOffset = f.position();
        fontDataLength = f.capacity() - fontDataOffset;
        fontData = new int[fontDataLength];
        for(int i = 0; i < fontDataLength; i++) {
            fontData[i] = readUByte(f);
        }
    }

    public void setFileNamez(String fileName) {
        fileNamez = fileName;
    }
    public String getFileNamez() {
        return fileNamez;
    }
    public String getPGFMagic() {
        return PGFMagic;
    }
    public int getHeaderSize() {
        return headerSize;
    }
    public int getRevision() {
        return revision;
    }
    public int getVersion() {
        return version;
    }
    public String getFontName() {
        return fontName;
    }
    public String getFontType() {
        return fontType;
    }
    public int getFirstGlyphInCharMap() {
        return firstGlyph;
    }
    public int getLastGlyphInCharMap() {
        return lastGlyph;
    }
    public int getMaxGlyphWidth() {
        return maxGlyphWidth;
    }
    public int getMaxGlyphHeight() {
        return maxGlyphHeight;
    }
    public int[] getMaxSize() {
        return maxSize;
    }
    public int getMaxLeftXAdjust() {
        return maxLeftXAdjust;
    }
    public int getMinCenterXAdjust() {
        return minCenterXAdjust;
    }
    public int getMaxBaseYAdjust() {
        return maxBaseYAdjust;
    }
    public int getMaxTopYAdjust() {
        return maxTopYAdjust;
    }
    public int getCharMapLength() {
        return charMapLength;
    }
    public int getCharPointerLength() {
        return charPointerLength;
    }
    public int getShadowMapLength() {
        return shadowMapLength;
    }
    public int getCompCharMapLength() {
        return compCharMapLength1 + compCharMapLength2;
    }
    public int getCharMapBpe() {
        return charMapBpe;
    }
    public int getCharPointerBpe() {
        return charPointerBpe;
    }
    public int getShadowMapBpe() {
        return shadowMapBpe;
    }
    public int[] getMaxAdvance() {
        return maxAdvance;
    }
    public int[][] getAdvanceTable() {
        return advanceTable;
    }
    public int[] getCharMap() {
        return charMap;
    }
    public int[] getCharPointerTable() {
        return charPointerTable;
    }
    public int[][] getCharMapCompressionTable1() {
        return charmapCompressionTable1;
    }
    public int[][] getCharMapCompressionTable2() {
        return charmapCompressionTable2;
    }
    public int[] getShadowCharMap() {
        return shadowCharMap;
    }
    public int[] getShadowScale() {
        return shadowScale;
    }
    public int[] getFontdata() {
        return fontData;
    }

	public int getHSize() {
		return hSize;
	}

	public int getVSize() {
		return vSize;
	}

	public int getHResolution() {
		return hResolution;
	}

	public int getVResolution() {
		return vResolution;
	}

	public int getMaxAscender() {
		return maxAscender;
	}

	public int getMaxDescender() {
		return maxDescender;
	}

	public int getBpp() {
		return bpp;
	}

	public int getAdvanceTableLength() {
		return advanceTableLength;
	}

	public int[][] getDimensionTable() {
		return dimensionTable;
	}

	public int getDimensionTableLength() {
		return dimTableLength;
	}

	public int[][] getXAdjustTable() {
		return xAdjustTable;
	}

	public int getXAdjustTableLength() {
		return xAdjustTableLength;
	}

	public int[][] getYAdjustTable() {
		return yAdjustTable;
	}

	public int getYAdjustTableLength() {
		return yAdjustTableLength;
	}

	public SceFontInfo createFontInfo() {
		return new SceFontInfo(this);
	}
}