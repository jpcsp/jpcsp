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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Battery;
import jpcsp.settings.Settings;

import org.apache.log4j.Logger;

@HLELogging
public class sceImpose extends HLEModule {
    public static Logger log = Modules.getLogger("sceImpose");

	@Override
	public String getName() {
		return "sceImpose";
	}

	@Override
    public void start() {
        languageMode_language = Settings.getInstance().readInt("emu.impose.language", PSP_LANGUAGE_ENGLISH);
        languageMode_button = Settings.getInstance().readInt("emu.impose.button", PSP_CONFIRM_BUTTON_CROSS);

        super.start();
    }

    public final static int PSP_LANGUAGE_JAPANESE = 0;
    public final static int PSP_LANGUAGE_ENGLISH = 1;
    public final static int PSP_LANGUAGE_FRENCH = 2;
    public final static int PSP_LANGUAGE_SPANISH = 3;
    public final static int PSP_LANGUAGE_GERMAN = 4;
    public final static int PSP_LANGUAGE_ITALIAN = 5;
    public final static int PSP_LANGUAGE_DUTCH = 6;
    public final static int PSP_LANGUAGE_PORTUGUESE = 7;
    public final static int PSP_LANGUAGE_RUSSIAN = 8;
    public final static int PSP_LANGUAGE_KOREAN = 9;
    public final static int PSP_LANGUAGE_TRADITIONAL_CHINESE = 10;
    public final static int PSP_LANGUAGE_SIMPLIFIED_CHINESE = 11;
    private int languageMode_language;

    public final static int PSP_CONFIRM_BUTTON_CIRCLE = 0;
    public final static int PSP_CONFIRM_BUTTON_CROSS = 1;
    private int languageMode_button;

    public final static int PSP_UMD_POPUP_DISABLE = 0;
    public final static int PSP_UMD_POPUP_ENABLE = 1;
    private int umdPopupStatus;

    private int backlightOffTime;

    @HLEUnimplemented
	@HLEFunction(nid = 0x381BD9E7, version = 150)
	public int sceImposeHomeButton() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5595A71A, version = 150)
	public int sceImposeSetHomePopup() {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x0F341BE4, version = 150)
	public int sceImposeGetHomePopup() {
		return 0;
	}

	@HLEFunction(nid = 0x72189C48, version = 150)
	public int sceImposeSetUMDPopup(int mode) {
        umdPopupStatus = mode;
        
        return 0;
	}

	@HLEFunction(nid = 0xE0887BC8, version = 150)
	public int sceImposeGetUMDPopup() {
		return umdPopupStatus;
	}

	@HLEFunction(nid = 0x36AA6E91, version = 150)
	public int sceImposeSetLanguageMode(int lang, int button) {
        if (log.isDebugEnabled()) {
            String langStr;
            switch (lang) {
                case PSP_LANGUAGE_JAPANESE: langStr = "JAP"; break;
                case PSP_LANGUAGE_ENGLISH: langStr = "ENG"; break;
                case PSP_LANGUAGE_FRENCH: langStr = "FR"; break;
                case PSP_LANGUAGE_KOREAN: langStr = "KOR"; break;
                default: langStr = "PSP_LANGUAGE_UNKNOWN" + lang; break;
            }

        	log.debug(String.format("sceImposeSetLanguageMode lang=%d(%s), button=%d", lang, langStr, button));
        }

        languageMode_language = lang;
        languageMode_button = button;

		return 0;
	}

    @HLEFunction(nid = 0x24FD7BCF, version = 150)
    public int sceImposeGetLanguageMode(TPointer32 langPtr, TPointer32 buttonPtr) {
    	if (log.isDebugEnabled()) {
	    	log.debug(String.format("sceImposeGetLanguageMode langPtr=%s, buttonPtr=%s returning lang=%d, button=%d", langPtr, buttonPtr, languageMode_language, languageMode_button));
    	}

        langPtr.setValue(languageMode_language);
        buttonPtr.setValue(languageMode_button);

        return 0;
    }

	@HLEFunction(nid = 0x8C943191, version = 150)
	public int sceImposeGetBatteryIconStatus(TPointer32 chargingPtr, TPointer32 iconStatusPtr) {
		int batteryPowerPercent = Battery.getCurrentPowerPercent();

        // Possible values for iconStatus: 0..3
        int iconStatus = Math.min(batteryPowerPercent / 25, 3);
        boolean charging = Battery.isCharging();

        chargingPtr.setValue(charging ? 1 : 0);
        iconStatusPtr.setValue(iconStatus);

		return 0;
	}

    @HLEFunction(nid = 0x8F6E3518, version = 150)
    public int sceImposeGetBacklightOffTime() {
		return backlightOffTime;
	}

    @HLEFunction(nid = 0x967F6D4A, version = 150)
    public int sceImposeSetBacklightOffTime(int time) {
        backlightOffTime = time;

		return 0;
	}
}