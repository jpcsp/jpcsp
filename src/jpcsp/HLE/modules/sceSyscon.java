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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.hardware.Battery;
import jpcsp.hardware.Wlan;

public class sceSyscon extends HLEModule {
    public static Logger log = Modules.getLogger("sceSyscon");

    /**
     * Set the wlan switch callback, that will be ran when the wlan switch changes.
     *
     * @param callback The callback function.
     * @param argp The second argument that will be passed to the callback.
     *
     * @return 0.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x50446BE5, version = 150)
	public int sceSysconSetWlanSwitchCallback(TPointer callback, int argp) {
    	return 0;
	}

    /**
     * Check if the battery is low.
     *
     * @return 1 if it is low, 0 otherwise.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x1605847F, version = 150)
	public boolean sceSysconIsLowBattery() {
    	return Battery.getCurrentPowerPercent() <= Battery.getLowPercent();
	}

    /**
     * Get the wlan switch state.
     *
     * @return 1 if wlan is activated, 0 otherwise.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x2D510164, version = 150)
	public int sceSysconGetWlanSwitch() {
    	return Wlan.getSwitchState();
	}

    /**
     * Set the wlan power.
     *
     * @param power The new power value.
     *
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x48448373, version = 150)
	public int sceSysconCtrlWlanPower(int power) {
    	return 0;
	}

    /**
     * Get the wlan power status.
     *
     * @return 1 if the power is on, 0 otherwise.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x7216917F, version = 150)
	public int sceSysconGetWlanPowerStatus() {
    	return 1;
	}

    /**
     * Get the wake up req (?).
     *
     * @param req Pointer to a buffer where the req will be stored.
     *
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x9D88A8DE, version = 150)
	public int sceSysconGetWakeUpReq(TPointer req) {
    	return 0;
	}

    /**
     * Get the baryon version.
     *
     * @return The baryon version.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xFD5C58CB, version = 150)
	public int _sceSysconGetBaryonVersion() {
    	return 0;
	}

    /**
     * Reset the device.
     *
     * @param reset The reset value, passed to the syscon.
     * @param mode The resetting mode (?).
     * 
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x8CBC7987, version = 150)
	public int sceSysconResetDevice(int reset, int mode) {
    	return 0;
	}
}
