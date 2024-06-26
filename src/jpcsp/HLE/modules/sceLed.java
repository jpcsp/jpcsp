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

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.util.Utilities;

public class sceLed extends HLEModule {
    public static Logger log = Modules.getLogger("sceLed");
    public static final int PSP_LED_TYPE_MS = 0;             /** Memory-Stick LED. */
    public static final int PSP_LED_TYPE_WLAN = 1;           /** W-LAN LED. */
    public static final int PSP_LED_TYPE_BT = 2;             /** Bluetooth LED. */
    public static final int SCE_LED_MODE_OFF = 0;            /** Turn a LED OFF. */
    public static final int SCE_LED_MODE_ON = 1;             /** Turn a LED ON. */
    public static final int SCE_LED_MODE_BLINK = 2;          /** Set a blink event for a LED. */
    public static final int SCE_LED_MODE_SELECTIVE_EXEC = 3; /** Register LED configuration commands and execute them. */

    public static String getLedName(int led) {
    	switch (led) {
    		case PSP_LED_TYPE_MS: return "MemoryStick";
    		case PSP_LED_TYPE_WLAN: return "WLAN";
    		case PSP_LED_TYPE_BT: return "BlueTooth";
    	}
    	return String.format("Unknown LED_TYPE_%X", led);
    }

    public static String getLedModeName(int ledMode) {
    	switch (ledMode) {
    		case SCE_LED_MODE_OFF: return "Off";
    		case SCE_LED_MODE_ON: return "On";
    		case SCE_LED_MODE_BLINK: return "Blink";
    		case SCE_LED_MODE_SELECTIVE_EXEC: return "Selective Exec";
    	}
    	return String.format("Unknown LED_MODE_%X", ledMode);
    }

    /**
     * Set a LED mode.
     * 
     * @param led The LED to set a mode for. One of ::ScePspLedTypes.
     * @param mode The mode to set for a LED. One of ::SceLedModes.
     * @param config Configuration settings for a LED. Is only used for the ::SceLedModes
     *               SCE_LED_MODE_BLINK and SCE_LED_MODE_SELECTIVE_EXEC.
     * 
     * @return SCE_ERROR_OK on success.
     */
	@HLEFunction(nid = 0xEA24BE03, version = 150)
	public int sceLedSetMode(int led, int mode, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer config) {
    	if (log.isInfoEnabled()) {
    		log.info(String.format("sceLedSetMode %s %s", getLedName(led), getLedModeName(mode)));
    	}
    	if (config.isNotNull()) {
    		log.info(String.format("sceLedSetMode %s %s unknown config: %s", getLedName(led), getLedModeName(mode), Utilities.getMemoryDump(config, 20)));
    	}

    	return 0;
	}
}
