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

public class pspFontStyle extends pspAbstractMemoryMappedStructure {
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

	public float fontH;           // Horizontal size.
	public float fontV;           // Vertical size.
	public float fontHRes;        // Horizontal resolution.
	public float fontVRes;        // Vertical resolution.
	public float fontWeight;      // Font weight.
	public short fontFamily;      // Font family (SYSTEM = 0, probably more).
	public short fontStyle;       // Style (SYSTEM = 0, STANDARD = 1, probably more).
	public short fontStyleSub;    // Subset of style (only used in Asian fonts, unknown values).
	public short fontLanguage;    // Language code (UNK = 0, JAPANESE = 1, ENGLISH = 2, probably more).
	public short fontRegion;      // Region code (UNK = 0, JAPAN = 1, probably more).
	public short fontCountry;     // Country code (UNK = 0, JAPAN = 1, US = 2, probably more).
	public String fontName;       // Font name (maximum size is 64).
	public String fontFileName;   // File name (maximum size is 64).
	public int fontAttributes;    // Additional attributes.
	public int fontExpire;        // Expiration date.

	@Override
	protected void read() {
		fontH = readFloat();
		fontV = readFloat();
		fontHRes = readFloat();
		fontVRes = readFloat();
		fontWeight = readFloat();
		fontFamily = (short) read16();
		fontStyle = (short) read16();
		fontStyleSub = (short) read16();
		fontLanguage = (short) read16();
		fontRegion = (short) read16();
		fontCountry = (short) read16();
		fontName = readStringNZ(64);
		fontFileName = readStringNZ(64);
		fontAttributes = read32();
		fontExpire = read32();
	}

	@Override
	protected void write() {
		writeFloat(fontH);
		writeFloat(fontV);
		writeFloat(fontHRes);
		writeFloat(fontVRes);
		writeFloat(fontWeight);
		write16(fontFamily);
		write16(fontStyle);
		write16(fontStyleSub);
		write16(fontLanguage);
		write16(fontRegion);
		write16(fontCountry);
		writeStringNZ(64, fontName);
		writeStringNZ(64, fontFileName);
		write32(fontAttributes);
		write32(fontExpire);
	}

	@Override
	public int sizeof() {
		return 168;
	}

	@Override
	public String toString() {
		return String.format("fontH %f, fontV %f, fontHRes %f, fontVRes %f, fontWeight %f, fontFamily %d, fontStyle %d, fontStyleSub %d, fontLanguage %d, fontRegion %d, fontCountry %d, fontName '%s', fontFileName '%s', fontAttributes %d, fontExpire %d", fontH, fontV, fontHRes, fontVRes, fontWeight, fontFamily, fontStyle, fontStyleSub, fontLanguage, fontRegion, fontCountry, fontName, fontFileName, fontAttributes, fontExpire);
	}
}
