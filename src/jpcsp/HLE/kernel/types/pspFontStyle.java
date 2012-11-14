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
	public static final int FONT_LANGUAGE_CHINESE  = 4;

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

	public boolean isMatching(pspFontStyle fontStyle, boolean optimum) {
		// A value 0 in each field of the fontStyle means "any value"
		if (!optimum) {
			if (fontStyle.fontH != 0f) {
				if (Math.round(fontStyle.fontH) != Math.round(fontH)) {
					return false;
				}
			}
			if (fontStyle.fontV != 0f) {
				if (Math.round(fontStyle.fontV) != Math.round(fontV)) {
					return false;
				}
			}
			if (fontStyle.fontHRes != 0f) {
				if (Math.round(fontStyle.fontHRes) != Math.round(fontHRes)) {
					return false;
				}
			}
			if (fontStyle.fontVRes != 0f) {
				if (Math.round(fontStyle.fontVRes) != Math.round(fontVRes)) {
					return false;
				}
			}
		}
		if (fontStyle.fontWeight != 0f && fontStyle.fontWeight != fontWeight) {
			return false;
		}
		if (fontStyle.fontFamily != 0 && fontStyle.fontFamily != fontFamily) {
			return false;
		}
		if (fontStyle.fontStyle != 0 && fontStyle.fontStyle != this.fontStyle) {
			return false;
		}
		if (fontStyle.fontStyleSub != 0 && fontStyle.fontStyleSub != fontStyleSub) {
			return false;
		}
		if (fontStyle.fontLanguage != 0 && fontStyle.fontLanguage != fontLanguage) {
			return false;
		}
		if (fontStyle.fontRegion != 0 && fontStyle.fontRegion != fontRegion) {
			return false;
		}
		if (fontStyle.fontCountry != 0 && fontStyle.fontCountry != fontCountry) {
			return false;
		}
		if (fontStyle.fontName.length() > 0 && !fontStyle.fontName.equals(fontName)) {
			return false;
		}
		if (fontStyle.fontFileName.length() > 0 && !fontStyle.fontFileName.equals(fontFileName)) {
			return false;
		}
		if (fontStyle.fontAttributes != 0 && fontStyle.fontAttributes != fontAttributes) {
			return false;
		}

		return true;
	}

	public boolean isEmpty() {
		if (fontH != 0f || fontV != 0f || fontHRes != 0f || fontVRes != 0f) {
			return false;
		}
		if (fontWeight != 0f || fontFamily != 0 || fontStyle != 0 || fontStyleSub != 0) {
			return false;
		}
		if (fontLanguage != 0 || fontRegion != 0 || fontCountry != 0) {
			return false;
		}
		if (fontName.length() > 0 || fontFileName.length() > 0 || fontAttributes != 0) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return String.format("fontH %f, fontV %f, fontHRes %f, fontVRes %f, fontWeight %f, fontFamily %d, fontStyle %d, fontStyleSub %d, fontLanguage %d, fontRegion %d, fontCountry %d, fontName '%s', fontFileName '%s', fontAttributes %d, fontExpire %d", fontH, fontV, fontHRes, fontVRes, fontWeight, fontFamily, fontStyle, fontStyleSub, fontLanguage, fontRegion, fontCountry, fontName, fontFileName, fontAttributes, fontExpire);
	}
}
