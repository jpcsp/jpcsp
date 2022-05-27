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
package jpcsp.HLE.modules;

import jpcsp.State;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.hardware.Audio;
import jpcsp.hardware.Battery;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceImpose extends HLEModule {
    public static Logger log = Modules.getLogger("sceImpose");

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

	public static final int PSP_IMPOSE_MAIN_VOLUME            = 0x1;
	public static final int PSP_IMPOSE_BACKLIGHT_BRIGHTNESS   = 0x2;
	public static final int PSP_IMPOSE_EQUALIZER_MODE         = 0x4;
	public static final int PSP_IMPOSE_MUTE                   = 0x8;
	public static final int PSP_IMPOSE_AVLS                   = 0x10;
	public static final int PSP_IMPOSE_TIME_FORMAT            = 0x20;
	public static final int PSP_IMPOSE_DATE_FORMAT            = 0x40;
	public static final int PSP_IMPOSE_LANGUAGE               = 0x80;
	public static final int PSP_IMPOSE_00000100               = 0x100;
	public static final int PSP_IMPOSE_BACKLIGHT_OFF_INTERVAL = 0x200;
	public static final int PSP_IMPOSE_SOUND_REDUCTION        = 0x400;
	public static final int PSP_IMPOSE_UMD_POPUP_ENABLED      = 1;
	public static final int PSP_IMPOSE_UMD_POPUP_DISABLED     = 0;
	public static final int PSP_IMPOSE_20000000               = 0x20000000;
	public static final int PSP_IMPOSE_80000001               = 0x80000001;
	public static final int PSP_IMPOSE_80000002               = 0x80000002;
	public static final int PSP_IMPOSE_80000003               = 0x80000003;
	public static final int PSP_IMPOSE_80000004               = 0x80000004;
	public static final int PSP_IMPOSE_80000005               = 0x80000005;
	public static final int PSP_IMPOSE_80000006               = 0x80000006;
	public static final int PSP_IMPOSE_80000007               = 0x80000007;
	public static final int PSP_IMPOSE_80000008               = 0x80000008;
	public static final int PSP_IMPOSE_80000009               = 0x80000009;
	public static final int PSP_IMPOSE_8000000A               = 0x8000000A;
	public static final int PSP_IMPOSE_8000000B               = 0x8000000B;

	private int imposeChanges = 0;
	private int impose80000004 = 0;
	private int impose80000007 = 0;
	private int imposeAvls = 1;

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
	@HLEFunction(nid = 0x5557F4E2, version = 660)
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

	@HLEFunction(nid = 0x531C9778, version = 352)
	@HLEFunction(nid = 0xDC3BECFF, version = 660)
	public int sceImposeGetParam(int param) {
		int value = 0;

		switch (param) {
			case PSP_IMPOSE_MAIN_VOLUME:
				// Return value [0..30]?
				if (Audio.isMuted()) {
					value = 0;
				} else {
					value = 30;
				}
				break;
			case PSP_IMPOSE_SOUND_REDUCTION:
				value = 0;
				break;
			case PSP_IMPOSE_MUTE:
				value = Audio.isMuted() ? 1 : 0;
				break;
			case PSP_IMPOSE_AVLS:
				value = imposeAvls;
				break;
			case PSP_IMPOSE_BACKLIGHT_OFF_INTERVAL:
				value = 0;
				break;
			case PSP_IMPOSE_BACKLIGHT_BRIGHTNESS:
				value = 3;
				break;
			case PSP_IMPOSE_20000000:
				value = 0;
				break;
			case PSP_IMPOSE_80000004:
				value = impose80000004;
				break;
			case PSP_IMPOSE_80000007:
				value = impose80000007;
				break;
			case PSP_IMPOSE_EQUALIZER_MODE:
			case PSP_IMPOSE_TIME_FORMAT:
			case PSP_IMPOSE_DATE_FORMAT:
			case PSP_IMPOSE_LANGUAGE:
			case PSP_IMPOSE_00000100:
			case PSP_IMPOSE_80000001:
			case PSP_IMPOSE_80000002:
			case PSP_IMPOSE_80000003:
			case PSP_IMPOSE_80000005:
			case PSP_IMPOSE_80000006:
			case PSP_IMPOSE_80000008:
			case PSP_IMPOSE_80000009:
			case PSP_IMPOSE_8000000A:
			case PSP_IMPOSE_8000000B:
				log.warn(String.format("sceImposeGetParam param=0x%X not implemented", param));
				break;
			default:
				log.warn(String.format("sceImposeGetParam param=0x%X invalid parameter", param));
				return SceKernelErrors.ERROR_INVALID_MODE;
		}

		return value;
	}

	@HLEFunction(nid = 0x810FB7FB, version = 352)
	@HLEFunction(nid = 0x3C318569, version = 660)
	public int sceImposeSetParam(int param, int value) {
		switch (param) {
			case PSP_IMPOSE_MAIN_VOLUME:
				if (value < 0 || value >= 31) {
					return SceKernelErrors.ERROR_INVALID_VALUE;
				}
				break;
			case PSP_IMPOSE_MUTE:
				if (value < 0 || value > 1) {
					return SceKernelErrors.ERROR_INVALID_VALUE;
				}
				Audio.setMuted(value != 0);
				imposeChanges |= PSP_IMPOSE_MUTE | PSP_IMPOSE_MAIN_VOLUME;
				break;
			case PSP_IMPOSE_AVLS:
				if (value < 0 || value > 1) {
					return SceKernelErrors.ERROR_INVALID_VALUE;
				}
				imposeAvls = value;
				imposeChanges |= PSP_IMPOSE_AVLS | PSP_IMPOSE_MAIN_VOLUME;
				break;
			case PSP_IMPOSE_TIME_FORMAT:
				imposeChanges |= PSP_IMPOSE_TIME_FORMAT;
				break;
			case PSP_IMPOSE_DATE_FORMAT:
				imposeChanges |= PSP_IMPOSE_DATE_FORMAT;
				break;
			case PSP_IMPOSE_LANGUAGE:
				if (value < 0 || value >= 12) {
					return SceKernelErrors.ERROR_INVALID_VALUE;
				}
				imposeChanges |= PSP_IMPOSE_LANGUAGE;
				break;
			case PSP_IMPOSE_00000100:
				if (value < 0 || value > 1) {
					return SceKernelErrors.ERROR_INVALID_VALUE;
				}
				imposeChanges |= PSP_IMPOSE_00000100;
				break;
			case PSP_IMPOSE_80000004:
				if (value < 0 || value > 1) {
					return SceKernelErrors.ERROR_INVALID_VALUE;
				}
				impose80000004 = value;
				break;
			case PSP_IMPOSE_80000007:
				impose80000007 = value;
				break;
			case PSP_IMPOSE_BACKLIGHT_BRIGHTNESS:
			case PSP_IMPOSE_EQUALIZER_MODE:
			case PSP_IMPOSE_BACKLIGHT_OFF_INTERVAL:
			case PSP_IMPOSE_SOUND_REDUCTION:
			case PSP_IMPOSE_80000001:
			case PSP_IMPOSE_80000002:
			case PSP_IMPOSE_80000003:
			case PSP_IMPOSE_80000005:
			case PSP_IMPOSE_80000006:
			case PSP_IMPOSE_80000008:
			case PSP_IMPOSE_80000009:
			case PSP_IMPOSE_8000000A:
			case PSP_IMPOSE_8000000B:
				log.warn(String.format("sceImposeSetParam param=0x%X, value=0x%X not implemented", param, value));
				break;
			default:
				log.warn(String.format("sceImposeSetParam param=0x%X, value=0x%X invalid parameter", param, value));
				return SceKernelErrors.ERROR_INVALID_MODE;
		}

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x116DDED6, version = 150)
	@HLEFunction(nid = 0x28B97035, version = 660)
	public int sceImposeSetVideoOutMode(int mode, int width, int height) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xBB12F974, version = 150)
	public int sceImposeSetStatus(int status) {
		return 0;
	}

	@HLEFunction(nid = 0xB415FC59, version = 150)
	@HLEFunction(nid = 0x0F067E16, version = 660)
	public int sceImposeChanges() {
		// Has no parameters
		int result = imposeChanges;
		imposeChanges = 0;

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceImposeChanges returning 0x%X", result));
		}

		return result;
	}

	@HLEFunction(nid = 0x9BA61B49, version = 150)
	public boolean sceImpose_9BA61B49_isHomeButtonPressed() {
		int buttons = State.controller.getButtons();
		boolean homeButtonPressed = Utilities.hasFlag(buttons, sceCtrl.PSP_CTRL_HOME);

		return homeButtonPressed;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xBCF1D254, version = 660)
	@HLEFunction(nid = 0x9DBCE0C4, version = 150)
	public int sceImpose_9DBCE0C4(int unknown) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xBB3F5DEC, version = 150)
	public int sceImpose_BB3F5DEC(int unknown1, int unknown2, int unknown3) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB497314D, version = 660)
	public int sceImpose_driver_B497314D(int param, @BufferInfo(usage = Usage.out) TPointer64 resultAddr) {
		resultAddr.setValue(0L);
		return 0;
	}
}