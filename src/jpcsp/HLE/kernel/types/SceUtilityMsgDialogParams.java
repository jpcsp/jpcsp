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


public class SceUtilityMsgDialogParams extends pspAbstractMemoryMappedStructure {

    public pspUtilityDialogCommon base;
    public int unknown;
    public int mode;
        public final static int PSP_UTILITY_MSGDIALOG_MODE_ERROR = 0;
        public final static int PSP_UTILITY_MSGDIALOG_MODE_TEXT  = 1;
    public int errorValue;
    public String message; // 512 bytes
    public int options;
    	public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_MASK         = 0x00000FF0;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_YES  = 0x00000010;
        public final static int PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_NO   = 0x00000110;
    public int buttonPressed;

    public SceUtilityMsgDialogParams() {
        base = new pspUtilityDialogCommon();
        base.size = 532;
    }

    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.size);

        unknown         = read32();
        mode            = read32();
        errorValue      = read32();
        message         = readStringNZ(512);
        options         = read32();
        buttonPressed   = read32();
    }

    protected void write() {
        setMaxSize(base.size);
        write(base);

        write32(unknown);
        write32(mode);
        write32(errorValue);
        writeStringNZ(512, message);
        write32(options);
        write32(buttonPressed);
    }

    public int sizeof() {
        return base.size;
    }

    public boolean isOptionYesNoDefaultYes() {
    	return (options & PSP_UTILITY_MSGDIALOG_OPTION_YESNO_MASK) == PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_YES;
    }

    public boolean isOptionYesNoDefaultNo() {
    	return (options & PSP_UTILITY_MSGDIALOG_OPTION_YESNO_MASK) == PSP_UTILITY_MSGDIALOG_OPTION_YESNO_DEFAULT_NO;
    }

    public boolean isOptionYesNo() {
    	return isOptionYesNoDefaultYes() || isOptionYesNoDefaultNo();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("unknown " + String.format("0x%08X", unknown) + "\n");
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
        sb.append("buttonPressed " + String.format("0x%08X", buttonPressed));

        return sb.toString();
    }
}
