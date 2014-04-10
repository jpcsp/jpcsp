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


public class SceUtilityMsgDialogParams extends pspUtilityBaseDialog {
    public int result;
    public int mode;
        public final static int PSP_UTILITY_MSGDIALOG_MODE_ERROR = 0;
        public final static int PSP_UTILITY_MSGDIALOG_MODE_TEXT  = 1;
    public int errorValue;
    public String message; // 512 bytes
    public int options;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_ERROR              = 0x00000000;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_NORMAL             = 0x00000001;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_ALLOW_SOUND        = 0x00000000;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_MUTE_SOUND         = 0x00000002;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_NONE   = 0x00000000;
    	public final static int PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_YESNO  = 0x00000010;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_OK     = 0x00000020;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_MASK   = 0x00000030;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_ENABLE_CANCEL      = 0x00000000;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_DISABLE_CANCEL     = 0x00000080;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_MASK = 0x00000100;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_NONE = 0x00000000;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_YES  = 0x00000000;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_NO   = 0x00000100;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_OK   = 0x00000000;
    public int buttonPressed;
        public final static int PSP_UTILITY_BUTTON_PRESSED_INVALID              = 0;
        public final static int PSP_UTILITY_BUTTON_PRESSED_YES                  = 1;
        public final static int PSP_UTILITY_BUTTON_PRESSED_OK                   = 1;
        public final static int PSP_UTILITY_BUTTON_PRESSED_NO                   = 2;
    	public final static int PSP_UTILITY_BUTTON_PRESSED_ESC                  = 3;
    public String enterButtonString; // 64 bytes
    public String backButtonString; // 64 bytes

    public SceUtilityMsgDialogParams() {
        base = new pspUtilityDialogCommon();
    }

    @Override
	protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.totalSizeof());

        result         = read32();
        mode            = read32();
        errorValue      = read32();
        message         = readStringNZ(512);
        options         = read32();
        buttonPressed   = read32();
        enterButtonString = readStringNZ(64);
        backButtonString = readStringNZ(64);
    }

    @Override
	protected void write() {
        write(base);
        setMaxSize(base.totalSizeof());

        write32(result);
        write32(mode);
        write32(errorValue);
        writeStringNZ(512, message);
        write32(options);
        write32(buttonPressed);
        writeStringNZ(64, enterButtonString);
        writeStringNZ(64, backButtonString);
    }

    @Override
	public int sizeof() {
        return base.totalSizeof();
    }

    public boolean isOptionYesNoDefaultYes() {
        if((options & PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_YESNO) == PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_YESNO) {
            return (options & PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_MASK) == PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_YES;
        }
        return false;
    }

    public boolean isOptionYesNoDefaultNo() {
    	if((options & PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_YESNO) == PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_YESNO) {
            return (options & PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_MASK) == PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_NO;
        }
        return false;
    }

    public boolean isOptionYesNo() {
        return isOptionYesNoDefaultYes() || isOptionYesNoDefaultNo();
    }

    public boolean isOptionOk() {
        return((options & PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_OK) == PSP_UTILITY_MSGDIALOG_OPTION_BUTTON_TYPE_OK);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("result " + String.format("0x%08X", result) + "\n");
        sb.append("mode " + ((mode == PSP_UTILITY_MSGDIALOG_MODE_ERROR)
            ? "PSP_UTILITY_MSGDIALOG_MODE_ERROR"
            : (mode == PSP_UTILITY_MSGDIALOG_MODE_TEXT)
            ? "PSP_UTILITY_MSGDIALOG_MODE_TEXT"
            : String.format("0x%08X", mode)) + "\n");
        sb.append("errorValue " + String.format("0x%08X", errorValue) + "\n");
        sb.append("message '" + message + "'\n");
        sb.append("options " + String.format("0x%08X", options) + "\n");
        if (isOptionYesNoDefaultYes())
            sb.append("options PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_YES\n");
        if (isOptionYesNoDefaultNo())
            sb.append("options PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_NO\n");
        sb.append("buttonPressed " + String.format("0x%08X'\n", buttonPressed));
        sb.append("enterButtonString '" + enterButtonString + "'\n");
        sb.append("backButtonString '" + backButtonString + "'");

        return sb.toString();
    }
}