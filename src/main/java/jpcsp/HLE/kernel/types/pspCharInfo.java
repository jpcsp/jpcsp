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

public class pspCharInfo extends pspAbstractMemoryMappedStructure {
    /*
     * Char's metrics:
     *
     *           Width / Horizontal Advance
     *           <---------->
     *      |           000 |
     *      |           000 |  Ascender
     *      |           000 |
     *      |     000   000 |
     *      | -----000--000-------- Baseline
     *      |        00000  |  Descender
     * Height /
     * Vertical Advance
     *
     * The char's bearings represent the difference between the
     * width and the horizontal advance and/or the difference
     * between the height and the vertical advance.
     * In our debug font, these measures are the same (block pixels),
     * but in real PGF fonts they can vary (italic fonts, for example).
     */
    public int bitmapWidth;
    public int bitmapHeight;
    public int bitmapLeft;
    public int bitmapTop;
    public int sfp26Width;
    public int sfp26Height;
    public int sfp26Ascender;
    public int sfp26Descender;
    public int sfp26BearingHX;
    public int sfp26BearingHY;
    public int sfp26BearingVX;
    public int sfp26BearingVY;
    public int sfp26AdvanceH;
    public int sfp26AdvanceV;

	@Override
	protected void read() {
		bitmapWidth = read32();    // Offset 0
		bitmapHeight = read32();   // Offset 4
		bitmapLeft = read32();     // Offset 8
		bitmapTop = read32();      // Offset 12
		sfp26Width = read32();     // Offset 16
		sfp26Height = read32();    // Offset 20
		sfp26Ascender = read32();  // Offset 24
		sfp26Descender = read32(); // Offset 28
		sfp26BearingHX = read32(); // Offset 32
		sfp26BearingHY = read32(); // Offset 36
		sfp26BearingVX = read32(); // Offset 40
		sfp26BearingVY = read32(); // Offset 44
		sfp26AdvanceH = read32();  // Offset 48
		sfp26AdvanceV = read32();  // Offset 52
		readUnknown(4);            // Offset 56
	}

	@Override
	protected void write() {
		write32(bitmapWidth);
		write32(bitmapHeight);
		write32(bitmapLeft);
		write32(bitmapTop);
        // Glyph metrics (in 26.6 signed fixed-point).
		write32(sfp26Width);
		write32(sfp26Height);
		write32(sfp26Ascender);
		write32(sfp26Descender);
		write32(sfp26BearingHX);
		write32(sfp26BearingHY);
		write32(sfp26BearingVX);
		write32(sfp26BearingVY);
		write32(sfp26AdvanceH);
		write32(sfp26AdvanceV);
        // Padding.
		write8((byte) 0);
		write8((byte) 0);
		write8((byte) 0);
		write8((byte) 0);
	}

	@Override
	public int sizeof() {
		return 60;
	}

	@Override
	public String toString() {
		return String.format("bitmapWidth=%d, bitmapHeight=%d, bitmapLeft=%d, bitmapTop=%d, sfp26Width=%d, sfp26Height=%d, sfp26Ascender=%d, sfp26Descender=%d, sfp26BearingHX=%d, sfp26BearingHY=%d, sfp26BearingVX=%d, sfp26BearingVY=%d, sfp26AdvanceH=%d, sfp26AdvanceV=%d", bitmapWidth, bitmapHeight, bitmapLeft, bitmapTop, sfp26Width, sfp26Height, sfp26Ascender, sfp26Descender, sfp26BearingHX, sfp26BearingHY, sfp26BearingVX, sfp26BearingVY, sfp26AdvanceH, sfp26AdvanceV);
	}
}
