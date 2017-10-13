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
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.hardware.Battery;
import jpcsp.hardware.MemoryStick;
import jpcsp.hardware.UMDDrive;
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
     * Get the baryon version from the syscon.
     *
     * @param  baryonVersionAddr Pointer to a s32 where the baryon version will be stored.
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x7EC5A957, version = 150)
	public int sceSysconGetBaryonVersion(@BufferInfo(usage=Usage.out) TPointer32 baryonVersionAddr) {
    	baryonVersionAddr.setValue(0);

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

    /**
     * Get the Memory Stick power control state.
     *
     * @return 1 if powered, 0 otherwise
     */
	@HLEFunction(nid = 0x7672103B, version = 150)
	public boolean sceSysconGetMsPowerCtrl() {
		return MemoryStick.hasMsPower();
	}

    /**
     * Set the memory stick power.
     *
     * @param power The new power value.
     *
     * @return 0 on success.
     */
	@HLEUnimplemented
	@HLEFunction(nid = 0x1088ABA8, version = 150)
	public int sceSysconCtrlMsPower(boolean power) {
		MemoryStick.setMsPower(power);

		return 0;
	}

    /**
     * Get the UMD drive power control state.
     *
     * @return 1 if powered, 0 otherwise
     */
	@HLEUnimplemented
	@HLEFunction(nid = 0x577C5771, version = 660)
	public boolean sceSysconGetLeptonPowerCtrl() {
		return UMDDrive.hasUmdPower();
	}

    /**
     * Set the lepton power.
     *
     * @param power The new power value.
     *
     * @return 0 on success.
     */
	@HLEUnimplemented
	@HLEFunction(nid = 0x8A4519F5, version = 660)
	public int sceSysconCtrlLeptonPower(boolean power) {
		UMDDrive.setUmdPower(power);

		return 0;
	}

	/**
	 * Execute synchronously a syscon packet.
	 * 
	 * @param packet   The packet to execute. Its tx member needs to be initialized.
	 * @param flags    The packet flags. Check SceSysconPacketFlags.
	 * @return         0 on success.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x5B9ACC97, version = 150)
	public int sceSysconCmdExec(TPointer packet, int flags) {
		return 0;
	}

	/**
	 * Execute asynchronously a syscon packet.
	 * 
	 * @param packet   The packet to execute. Its tx member needs to be initialized.
	 * @param flags    The packet flags. Check SceSysconPacketFlags.
	 * @param callback The packet callback. Check the callback member of SceSysconPacket.
	 * @param argp     The second argument that will be passed to the callback when executed.
	 * @return         0 on success.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x3AC3D2A4, version = 150)
	public int sceSysconCmdExecAsync(TPointer packet, int flags, TPointer callback, int argp) {
		return 0;
	}

    /**
     * Get the baryon timestamp string.
     *
     * @param  timeStampAddr A pointer to a string at least 12 bytes long.
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x7BCC5EAE, version = 150)
	public int sceSysconGetTimeStamp(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer timeStampAddr) {
    	timeStampAddr.clear(12);

    	return 0;
	}

    /**
     * Get the pommel version.
     *
     * @param  pommelAddr Pointer to a s32 where the pommel version will be stored.
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xE7E87741, version = 150)
	public int sceSysconGetPommelVersion(@BufferInfo(usage=Usage.out) TPointer32 pommelAddr) {
    	pommelAddr.setValue(0);

    	return 0;
	}

    /**
     * Get the power status.
     *
     * @param  statusAddr Pointer to a s32 where the power status will be stored.
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x28363C97, version = 150)
	public int sceSysconGetPowerStatus(@BufferInfo(usage=Usage.out) TPointer32 statusAddr) {
    	statusAddr.setValue(0);

    	return 0;
	}

    /**
     * Read data from the scratchpad.
     *
     * @param src  The scratchpad address to read from. 
     * @param dst  A pointer where will be copied the read data.
     * @param size The size of the data to read from the scratchpad.
     * @return 0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xEB277C88, version = 150)
	public int sceSysconReadScratchPad(int src, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer dst, int size) {
    	dst.clear(size);

    	return 0;
	}
}
