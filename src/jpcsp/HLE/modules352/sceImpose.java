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
package jpcsp.HLE.modules352;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.kernel.types.SceKernelErrors;

public class sceImpose extends jpcsp.HLE.modules150.sceImpose {
	public static final int PSP_IMPOSE_MAIN_VOLUME            = 0x1;
	public static final int PSP_IMPOSE_BACKLIGHT_BRIGHTNESS   = 0x2;
	public static final int PSP_IMPOSE_EQUALIZER_MODE         = 0x4;
	public static final int PSP_IMPOSE_MUTE                   = 0x8;
	public static final int PSP_IMPOSE_AVLS                   = 0x10;
	public static final int PSP_IMPOSE_TIME_FORMAT            = 0x20;
	public static final int PSP_IMPOSE_DATE_FORMAT            = 0x40;
	public static final int PSP_IMPOSE_LANGUAGE               = 0x80;
	public static final int PSP_IMPOSE_BACKLIGHT_OFF_INTERVAL = 0x200;
	public static final int PSP_IMPOSE_SOUND_REDUCTION        = 0x400;
	public static final int PSP_IMPOSE_UMD_POPUP_ENABLED      = 1;
	public static final int PSP_IMPOSE_UMD_POPUP_DISABLED     = 0;

	@HLEFunction(nid = 0x531C9778, version = 352)
	public int sceImposeGetParam(int param) {
		int value = 0;

		switch (param) {
			case PSP_IMPOSE_MAIN_VOLUME:
				// Return value [0..30]?
				value = 30;
				break;
			case PSP_IMPOSE_BACKLIGHT_BRIGHTNESS:
			case PSP_IMPOSE_EQUALIZER_MODE:
			case PSP_IMPOSE_MUTE:
			case PSP_IMPOSE_AVLS:
			case PSP_IMPOSE_TIME_FORMAT:
			case PSP_IMPOSE_DATE_FORMAT:
			case PSP_IMPOSE_LANGUAGE:
			case PSP_IMPOSE_BACKLIGHT_OFF_INTERVAL:
			case PSP_IMPOSE_SOUND_REDUCTION:
				log.warn(String.format("sceImposeGetParam param=0x%X not implemented", param));
				break;
			default:
				log.warn(String.format("sceImposeGetParam param=0x%X invalid parameter", param));
				return SceKernelErrors.ERROR_INVALID_MODE;
		}

		return value;
	}

	@HLEFunction(nid = 0x810FB7FB, version = 352)
	public int sceImposeSetParam(int param, int value) {
		switch (param) {
			case PSP_IMPOSE_MAIN_VOLUME:
			case PSP_IMPOSE_BACKLIGHT_BRIGHTNESS:
			case PSP_IMPOSE_EQUALIZER_MODE:
			case PSP_IMPOSE_MUTE:
			case PSP_IMPOSE_AVLS:
			case PSP_IMPOSE_TIME_FORMAT:
			case PSP_IMPOSE_DATE_FORMAT:
			case PSP_IMPOSE_LANGUAGE:
			case PSP_IMPOSE_BACKLIGHT_OFF_INTERVAL:
			case PSP_IMPOSE_SOUND_REDUCTION:
				log.warn(String.format("sceImposeSetParam param=0x%X, value=0x%X not implemented", param, value));
				break;
			default:
				log.warn(String.format("sceImposeSetParam param=0x%X, value=0x%X invalid parameter", param, value));
				return SceKernelErrors.ERROR_INVALID_MODE;
		}

		return 0;
	}
}
