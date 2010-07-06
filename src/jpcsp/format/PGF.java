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

public class PGF {
    private int headerOffset;
    private int headerLenght;
    private String PGFMagic;
    private int revision;
    private int version;
    private int charMapLenght;
    private int charPointerLenght;
    private int charMapBpe;
    private int charPointerBpe;
    private String fontName;
    private String fontType;
    private int firstGlyph;
    private int lastGlyph;
    private int maxLeftXAdjust;
    private int maxBaseYAdjust;
    private int minCenterXAdjust;
    private int maxTopYAdjust;
    private int[] maxAdvance = new int[2];
    private int[] maxSize = new int[2];
    private int maxGlyphWidth;
    private int maxGlyphHeight;
    private int dimTableLenght;
    private int xAdjustTableLenght;
    private int yAdjustTableLenght;
    private int advanceTableLenght;
    private int shadowMapLenght;
    private int shadowMapBpe;
    private int[] shadowScale = new int[2];

    private int compCharMapBpe1;
    private int compCharMapLenght1;
    private int compCharMapBpe2;
    private int compCharMapLenght2;

    private int[][] dimensionTable;
    private int[][] xAdjustTable;
    private int[][] yAdjustTable;
    private int[] charmapCompressionTable;
    private int[] advanceTable;
    private int[] shadowCharMap;
    private int[] charMap;
    private int[] charPointerTable;

    private int[] fontData;
    private int fontDataOffset;
    private int fontDataLenght;

    private String fileNamez;

    public PGF(ByteBuffer f) throws IOException {
        read(f);
    }

    private void read(ByteBuffer f) throws IOException {
        if (f.capacity() == 0)
            return;

        // PGF Header.
        headerOffset = readUHalf(f);
        headerLenght = readUHalf(f);
        PGFMagic = readStringNZ(f, 4);
        revision = readWord(f);
        version = readWord(f);
        charMapLenght = readWord(f);
        charPointerLenght = readWord(f);
        charMapBpe = readWord(f);
        charPointerBpe = readWord(f);
        skipUnknown(f, 21);
        fontName = readStringNZ(f, 64);
        fontType = readStringNZ(f, 64);
        skipUnknown(f, 1);
        firstGlyph = readUHalf(f);
        lastGlyph = readUHalf(f);
        skipUnknown(f, 34);
        maxLeftXAdjust = readWord(f);
        maxBaseYAdjust = readWord(f);
        minCenterXAdjust = readWord(f);
        maxTopYAdjust = readWord(f);
        maxAdvance[0] = readWord(f);
        maxAdvance[1] = readWord(f);
        maxSize[0] = readWord(f);
        maxSize[1] = readWord(f);
        maxGlyphWidth = readUHalf(f);
        maxGlyphHeight = readUHalf(f);
        skipUnknown(f, 2);
        dimTableLenght= readUByte(f);
        xAdjustTableLenght = readUByte(f);
        yAdjustTableLenght = readUByte(f);
        advanceTableLenght = readUByte(f);
        skipUnknown(f, 102);
        shadowMapLenght = readWord(f);
        shadowMapBpe = readWord(f);
        skipUnknown(f, 4);
        shadowScale[0] = readWord(f);
        shadowScale[1] = readWord(f);
        skipUnknown(f, 8);

        if(revision == 3) {
            compCharMapBpe1 = readWord(f);
            compCharMapLenght1 = readUHalf(f);
            skipUnknown(f, 2);
            compCharMapBpe2 = readWord(f);
            compCharMapLenght2 = readUHalf(f);
            skipUnknown(f, 6);
        }

        // PGF Tables.
        dimensionTable = new int[2][dimTableLenght * 8];
        for(int i = 0; i < dimTableLenght * 8; i++) {
            dimensionTable[0][i] = readWord(f);
            dimensionTable[1][i] = readWord(f);
        }

        xAdjustTable = new int[2][xAdjustTableLenght * 8];
        for(int i = 0; i < xAdjustTableLenght * 8; i++) {
            xAdjustTable[0][i] = readWord(f);
            xAdjustTable[1][i] = readWord(f);
        }

        yAdjustTable = new int[2][yAdjustTableLenght * 8];
        for(int i = 0; i < yAdjustTableLenght * 8; i++) {
            yAdjustTable[0][i] = readWord(f);
            yAdjustTable[1][i] = readWord(f);
        }

        advanceTable = new int[advanceTableLenght * 8 * 2];
        for(int i = 0; i < advanceTableLenght * 8 * 2; i++) {
            advanceTable[i] = readWord(f);
        }

        shadowCharMap = new int[shadowMapLenght * 2];
        for(int i = 0; i < shadowMapLenght * 2; i++) {
            shadowCharMap[i] = readUHalf(f);
        }

         if(revision == 3) {
             charmapCompressionTable = new int[(compCharMapLenght1 * 4 + compCharMapLenght2 * 4) * 2];
             for(int i = 0; i < (compCharMapLenght1 * 4 + compCharMapLenght2 * 4) * 2; i++) {
                 charmapCompressionTable[i] = readUHalf(f);
             }
         }

        charMap = new int[(charMapLenght * charMapBpe + 31) / 8];
        for(int i = 0; i < (charMapLenght * charMapBpe + 31) / 8; i++) {
            charMap[i] = readUByte(f);
        }

        charPointerTable = new int[(charPointerLenght * charPointerBpe + 31)/ 8];
        for(int i = 0; i < ((charPointerLenght * charPointerBpe + 31) / 8); i++) {
            charPointerTable[i] = readUByte(f);
        }

        // PGF Fontdata.
        fontDataOffset = f.position();
        fontDataLenght = f.capacity() - fontDataOffset;
        fontData = new int[fontDataLenght];
        for(int i = 0; i < fontDataLenght; i++) {
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
    public int getHeaderOffset() {
        return headerOffset;
    }
    public int getHeaderLenght() {
        return headerLenght;
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
    public int getCharMapLenght() {
        return charMapLenght;
    }
    public int getCharPointerLenght() {
        return charPointerLenght;
    }
    public int getShadowMapLenght() {
        return shadowMapLenght;
    }
    public int getCompCharMapLenght() {
        return compCharMapLenght1 + compCharMapLenght2;
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
    public int[] getAdvanceTable() {
        return advanceTable;
    }
    public int[] getCharMap() {
        return charMap;
    }
    public int[] getCharPointerTable() {
        return charPointerTable;
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
}