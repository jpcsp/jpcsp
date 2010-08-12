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

public class SceUtilityOskParams extends pspAbstractMemoryMappedStructure {
	public pspUtilityDialogCommon base;
	public int oskDataCount;  // Number of input fields (PSPSDK).
	public int oskDataAddr;
	public SceUtilityOskData oskData;
    public int oskState;     // SceUtilityOskState (PSPSDK): internal status of this OSK.
        public final static int PSP_UTILITY_OSK_STATE_NONE = 0;
        public final static int PSP_UTILITY_OSK_STATE_INITIALIZING = 1;
        public final static int PSP_UTILITY_OSK_STATE_INITIALIZED = 2;
        public final static int PSP_UTILITY_OSK_STATE_OPEN = 3;
        public final static int PSP_UTILITY_OSK_STATE_CLOSING = 4;
        public final static int PSP_UTILITY_OSK_STATE_CLOSED = 5;
    public int errorCode;

	public static class SceUtilityOskData extends pspAbstractMemoryMappedStructure {
        public int inputMode;  // How to parse the input chars.
            public final static int PSP_UTILITY_OSK_DATA_INPUT_MODE_NONE = 0;
            public final static int PSP_UTILITY_OSK_DATA_INPUT_MODE_JPN_CONVERT = 1;
            public final static int PSP_UTILITY_OSK_DATA_INPUT_MODE_JPN_CHARS = 2;
            public final static int PSP_UTILITY_OSK_DATA_INPUT_MODE_LATIN_CHARS = 3;
        public int inputAttr;
		public int language;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_SYSTEM = 0;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_JAPANESE = 1;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_ENGLISH = 2;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_FRENCH = 3;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_SPANISH = 4;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_GERMAN = 5;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_ITALIAN = 6;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_DUTCH = 7;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_PORTUGUESE = 8;
            public final static int PSP_UTILITY_OSK_DATA_LANGUAGE_RUSSIAN = 9;
        public int hide;    // Seems to be related to the visibility of the input field.
            public final static int PSP_UTILITY_OSK_DATA_SHOW = 0;
            public final static int PSP_UTILITY_OSK_DATA_HIDE = 1;
        public int inputAllowCharType;  // ORed flag that specifies which char types can be accepted.
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_ALL = 0x0;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_NUM = 0x1;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_ENG = 0x2;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_LOWERCASE = 0x4;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_UPPERCASE = 0x8;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN_NUM = 0x100;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN = 0x200;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN_LOWERCASE = 0x400;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN_UPPERCASE = 0x800;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN_HIRAGANA = 0x1000;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN_HALF_KATAKANA = 0x2000;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN_KATAKANA = 0x4000;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_JPN_KANJI = 0x8000;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_CYRILLIC_LOWERCASE = 0x10000;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_CYRILLIC_UPPERCASE = 0x20000;
            public final static int PSP_UTILITY_OSK_DATA_CHAR_ALLOW_URL = 0x80000;
		public int lines;
        public int showInputText;
		public int descAddr;
		public String desc;
		public int inTextAddr;
		public String inText;
		public int outTextLength;
		public int outTextAddr;
		public String outText;
		public int result;
            public final static int PSP_UTILITY_OSK_DATA_NOT_CHANGED = 0;
            public final static int PSP_UTILITY_OSK_DATA_CANCELED = 1;
            public final static int PSP_UTILITY_OSK_DATA_CHANGED = 2;
		public int outTextLimit;

		@Override
		protected void read() {
            inputMode = read32();
            inputAttr = read32();
			language = read32();
            hide = read32();
            inputAllowCharType = read32();
			lines = read32();
            showInputText = read32();
			descAddr = read32();
			desc = readStringUTF16Z(descAddr);
			inTextAddr = read32();
			inText = readStringUTF16Z(inTextAddr);
			outTextLength = read32();
			outTextAddr = read32();
			outText = readStringUTF16Z(outTextAddr);
			result = read32();
			outTextLimit = read32();
		}

		@Override
		protected void write() {
            write32(inputMode);
            write32(inputAttr);
			write32(language);
			write32(hide);
            write32(inputAllowCharType);
			write32(lines);
			write32(showInputText);
			write32(descAddr);
			writeStringUTF16Z(descAddr, desc);
			write32(inTextAddr);
			writeStringUTF16Z(inTextAddr, inText);
			outTextLength = writeStringUTF16Z(outTextAddr, outText);
			write32(outTextLength);
			write32(outTextAddr);
			write32(result);
			write32(outTextLimit);
		}

		@Override
		public int sizeof() {
			return 13 * 4;
		}
	}

	@Override
	protected void read() {
		base = new pspUtilityDialogCommon();
		read(base);
		setMaxSize(base.size);

		oskDataCount = read32();
		oskDataAddr = read32();
		if (oskDataAddr != 0) {
			oskData = new SceUtilityOskData();
			oskData.read(mem, oskDataAddr);
		} else {
			oskData = null;
		}
        oskState = read32();
		errorCode = read32();
	}

	@Override
	protected void write() {
		setMaxSize(base.size);
		write(base);

		write32(oskDataCount);
		write32(oskDataAddr);
		if (oskData != null && oskDataAddr != 0) {
			oskData.write(mem, oskDataAddr);
		}
        write32(oskState);
		write32(errorCode);
	}

	@Override
	public int sizeof() {
		return base.size;
	}

	@Override
	public String toString() {
		return String.format("desc=%s, inText=%s, outText=%s", oskData.desc, oskData.inText, oskData.outText);
	}
}