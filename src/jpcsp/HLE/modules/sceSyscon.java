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

import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.hardware.Battery;
import jpcsp.hardware.LED;
import jpcsp.hardware.MemoryStick;
import jpcsp.hardware.Model;
import jpcsp.hardware.UMDDrive;
import jpcsp.hardware.Wlan;
import jpcsp.util.Utilities;

public class sceSyscon extends HLEModule {
    public static Logger log = Modules.getLogger("sceSyscon");
    public static final int PSP_SYSCON_CMD_NOP                           = 0x00;
    public static final int PSP_SYSCON_CMD_GET_BARYON                    = 0x01;
    public static final int PSP_SYSCON_CMD_GET_DIGITAL_KEY               = 0x02;
    public static final int PSP_SYSCON_CMD_GET_ANALOG                    = 0x03;
    public static final int PSP_SYSCON_CMD_GET_DIGITAL_KEY_ANALOG        = 0x06;
    public static final int PSP_SYSCON_CMD_GET_KERNEL_DIGITAL_KEY        = 0x07;
    public static final int PSP_SYSCON_CMD_GET_KERNEL_DIGITAL_KEY_ANALOG = 0x08;
    public static final int PSP_SYSCON_CMD_READ_CLOCK                    = 0x09;
    public static final int PSP_SYSCON_CMD_READ_ALARM                    = 0x0A;
    public static final int PSP_SYSCON_CMD_GET_POWER_SUPPLY_STATUS       = 0x0B;
    public static final int PSP_SYSCON_CMD_GET_TACHYON_WDT_STATUS        = 0x0C;
    public static final int PSP_SYSCON_CMD_GET_BATT_VOLT                 = 0x0D;
    public static final int PSP_SYSCON_CMD_GET_WAKE_UP_FACTOR            = 0x0E;
    public static final int PSP_SYSCON_CMD_GET_WAKE_UP_REQ               = 0x0F;
    public static final int PSP_SYSCON_CMD_GET_STATUS2                   = 0x10;
    public static final int PSP_SYSCON_CMD_GET_TIMESTAMP                 = 0x11;
    public static final int PSP_SYSCON_CMD_GET_VIDEO_CABLE               = 0x12;
    public static final int PSP_SYSCON_CMD_WRITE_CLOCK                   = 0x20;
    public static final int PSP_SYSCON_CMD_SET_USB_STATUS                = 0x21;
    public static final int PSP_SYSCON_CMD_WRITE_ALARM                   = 0x22;
    public static final int PSP_SYSCON_CMD_WRITE_SCRATCHPAD              = 0x23;
    public static final int PSP_SYSCON_CMD_READ_SCRATCHPAD               = 0x24;
    public static final int PSP_SYSCON_CMD_SEND_SETPARAM                 = 0x25;
    public static final int PSP_SYSCON_CMD_RECEIVE_SETPARAM              = 0x26;
    public static final int PSP_SYSCON_CMD_CTRL_BT_POWER_UNK1            = 0x29;
    public static final int PSP_SYSCON_CMD_CTRL_BT_POWER_UNK2            = 0x2A;
    public static final int PSP_SYSCON_CMD_CTRL_TACHYON_WDT              = 0x31;
    public static final int PSP_SYSCON_CMD_RESET_DEVICE                  = 0x32;
    public static final int PSP_SYSCON_CMD_CTRL_ANALOG_XY_POLLING        = 0x33;
    public static final int PSP_SYSCON_CMD_CTRL_HR_POWER                 = 0x34;
    public static final int PSP_SYSCON_CMD_GET_BATT_VOLT_AD              = 0x37;
    public static final int PSP_SYSCON_CMD_GET_POMMEL_VERSION            = 0x40;
    public static final int PSP_SYSCON_CMD_GET_POLESTAR_VERSION          = 0x41;
    public static final int PSP_SYSCON_CMD_CTRL_VOLTAGE                  = 0x42;
    public static final int PSP_SYSCON_CMD_CTRL_POWER                    = 0x45;
    public static final int PSP_SYSCON_CMD_GET_POWER_STATUS              = 0x46;
    public static final int PSP_SYSCON_CMD_CTRL_LED                      = 0x47;
    public static final int PSP_SYSCON_CMD_WRITE_POMMEL_REG              = 0x48;
    public static final int PSP_SYSCON_CMD_READ_POMMEL_REG               = 0x49;
    public static final int PSP_SYSCON_CMD_CTRL_HDD_POWER                = 0x4A;
    public static final int PSP_SYSCON_CMD_CTRL_LEPTON_POWER             = 0x4B;
    public static final int PSP_SYSCON_CMD_CTRL_MS_POWER                 = 0x4C;
    public static final int PSP_SYSCON_CMD_CTRL_WLAN_POWER               = 0x4D;
    public static final int PSP_SYSCON_CMD_WRITE_POLESTAR_REG            = 0x4E;
    public static final int PSP_SYSCON_CMD_READ_POLESTAR_REG             = 0x4F;
    public static final int PSP_SYSCON_CMD_CTRL_DVE_POWER                = 0x52;
    public static final int PSP_SYSCON_CMD_CTRL_BT_POWER                 = 0x53;
    public static final int PSP_SYSCON_CMD_CTRL_USB_POWER                = 0x55;
    public static final int PSP_SYSCON_CMD_CTRL_CHARGE                   = 0x56;
    public static final int PSP_SYSCON_CMD_BATTERY_NOP                   = 0x60;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP        = 0x61;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_TEMP              = 0x62;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_VOLT              = 0x63;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_ELEC              = 0x64;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_RCAP              = 0x65;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_CAP               = 0x66;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_FULL_CAP          = 0x67;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_IFC               = 0x68;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_LIMIT_TIME        = 0x69;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_STATUS            = 0x6A;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_CYCLE             = 0x6B;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_SERIAL            = 0x6C;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_INFO              = 0x6D;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_TEMP_AD           = 0x6E;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_VOLT_AD           = 0x6F;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_ELEC_AD           = 0x70;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_TOTAL_ELEC        = 0x71;
    public static final int PSP_SYSCON_CMD_BATTERY_GET_CHARGE_TIME       = 0x72;
    private static String cmdNames[];
    public static final int PSP_SYSCON_LED_MS    = 0; // Memory-Stick LED
    public static final int PSP_SYSCON_LED_WLAN  = 1; // W-LAN LED
    public static final int PSP_SYSCON_LED_POWER = 2; // Power LED
    public static final int PSP_SYSCON_LED_BT    = 3; // Bluetooth LED (only PSP GO)
    private final int scratchPad[] = new int[32];
    private int alarm;

	@Override
	public void start() {
		Arrays.fill(scratchPad, 0);

		// Unknown 4-bytes value at offset 8
		int scratchPad8 = 0;
		for (int i = 0; i < 5; i++, scratchPad8 >>= 8) {
			scratchPad[i + 8] = scratchPad8 & 0xFF;
		}

		// 5-bytes value at offset 16, used to initialize the clock.
		// Set this value to 0 to force the clock initialization at boot time.
		long scratchPad16 = Modules.sceRtcModule.hleGetCurrentTick() >> 19;
		if (log.isDebugEnabled()) {
			log.debug(String.format("Initializing scratchPad16=0x%X", scratchPad16));
		}
		for (int i = 0; i < 5; i++, scratchPad16 >>= 8) {
			scratchPad[i + 16] = (int) scratchPad16 & 0xFF;
		}

		// Unknown 5-bytes value at offset 24
		long scratchPad24 = 0L;
		for (int i = 0; i < 5; i++, scratchPad24 >>= 8) {
			scratchPad[i + 24] = (int) scratchPad24 & 0xFF;
		}

		alarm = 0;

		super.start();
	}

	public static String getSysconCmdName(int cmd) {
    	if (cmdNames == null) {
    		cmdNames = new String[256];
    		cmdNames[PSP_SYSCON_CMD_NOP] = "NOP";
    		cmdNames[PSP_SYSCON_CMD_GET_BARYON] = "GET_BARYON";
    		cmdNames[PSP_SYSCON_CMD_GET_DIGITAL_KEY] = "GET_DIGITAL_KEY";
    		cmdNames[PSP_SYSCON_CMD_GET_ANALOG] = "GET_ANALOG";
    		cmdNames[PSP_SYSCON_CMD_GET_DIGITAL_KEY_ANALOG] = "GET_DIGITAL_KEY_ANALOG";
    		cmdNames[PSP_SYSCON_CMD_GET_KERNEL_DIGITAL_KEY] = "GET_KERNEL_DIGITAL_KEY";
    		cmdNames[PSP_SYSCON_CMD_GET_KERNEL_DIGITAL_KEY_ANALOG] = "GET_KERNEL_DIGITAL_KEY_ANALOG";
    		cmdNames[PSP_SYSCON_CMD_READ_CLOCK] = "READ_CLOCK";
    		cmdNames[PSP_SYSCON_CMD_READ_ALARM] = "READ_ALARM";
    		cmdNames[PSP_SYSCON_CMD_GET_POWER_SUPPLY_STATUS] = "GET_POWER_SUPPLY_STATUS";
    		cmdNames[PSP_SYSCON_CMD_GET_TACHYON_WDT_STATUS] = "GET_TACHYON_WDT_STATUS";
    		cmdNames[PSP_SYSCON_CMD_GET_BATT_VOLT] = "GET_BATT_VOLT";
    		cmdNames[PSP_SYSCON_CMD_GET_WAKE_UP_FACTOR] = "GET_WAKE_UP_FACTOR";
    		cmdNames[PSP_SYSCON_CMD_GET_WAKE_UP_REQ] = "GET_WAKE_UP_REQ";
    		cmdNames[PSP_SYSCON_CMD_GET_STATUS2] = "GET_STATUS2";
    		cmdNames[PSP_SYSCON_CMD_GET_TIMESTAMP] = "GET_TIMESTAMP";
    		cmdNames[PSP_SYSCON_CMD_GET_VIDEO_CABLE] = "GET_VIDEO_CABLE";
    		cmdNames[PSP_SYSCON_CMD_WRITE_CLOCK] = "WRITE_CLOCK";
    		cmdNames[PSP_SYSCON_CMD_SET_USB_STATUS] = "SET_USB_STATUS";
    		cmdNames[PSP_SYSCON_CMD_WRITE_ALARM] = "WRITE_ALARM";
    		cmdNames[PSP_SYSCON_CMD_WRITE_SCRATCHPAD] = "WRITE_SCRATCHPAD";
    		cmdNames[PSP_SYSCON_CMD_READ_SCRATCHPAD] = "READ_SCRATCHPAD";
    		cmdNames[PSP_SYSCON_CMD_SEND_SETPARAM] = "SEND_SETPARAM";
    		cmdNames[PSP_SYSCON_CMD_RECEIVE_SETPARAM] = "RECEIVE_SETPARAM";
    		cmdNames[PSP_SYSCON_CMD_CTRL_BT_POWER_UNK1] = "CTRL_BT_POWER_UNK1";
    		cmdNames[PSP_SYSCON_CMD_CTRL_BT_POWER_UNK2] = "CTRL_BT_POWER_UNK2";
    		cmdNames[PSP_SYSCON_CMD_CTRL_TACHYON_WDT] = "CTRL_TACHYON_WDT";
    		cmdNames[PSP_SYSCON_CMD_RESET_DEVICE] = "RESET_DEVICE";
    		cmdNames[PSP_SYSCON_CMD_CTRL_ANALOG_XY_POLLING] = "CTRL_ANALOG_XY_POLLING";
    		cmdNames[PSP_SYSCON_CMD_CTRL_HR_POWER] = "CTRL_HR_POWER";
    		cmdNames[PSP_SYSCON_CMD_GET_BATT_VOLT_AD] = "GET_BATT_VOLT_AD";
    		cmdNames[PSP_SYSCON_CMD_GET_POMMEL_VERSION] = "GET_POMMEL_VERSION";
    		cmdNames[PSP_SYSCON_CMD_GET_POLESTAR_VERSION] = "GET_POLESTAR_VERSION";
    		cmdNames[PSP_SYSCON_CMD_CTRL_VOLTAGE] = "CTRL_VOLTAGE";
    		cmdNames[PSP_SYSCON_CMD_CTRL_POWER] = "CTRL_POWER";
    		cmdNames[PSP_SYSCON_CMD_GET_POWER_STATUS] = "GET_POWER_STATUS";
    		cmdNames[PSP_SYSCON_CMD_CTRL_LED] = "CTRL_LED";
    		cmdNames[PSP_SYSCON_CMD_WRITE_POMMEL_REG] = "WRITE_POMMEL_REG";
    		cmdNames[PSP_SYSCON_CMD_READ_POMMEL_REG] = "READ_POMMEL_REG";
    		cmdNames[PSP_SYSCON_CMD_CTRL_HDD_POWER] = "CTRL_HDD_POWER";
    		cmdNames[PSP_SYSCON_CMD_CTRL_LEPTON_POWER] = "CTRL_LEPTON_POWER";
    		cmdNames[PSP_SYSCON_CMD_CTRL_MS_POWER] = "CTRL_MS_POWER";
    		cmdNames[PSP_SYSCON_CMD_CTRL_WLAN_POWER] = "CTRL_WLAN_POWER";
    		cmdNames[PSP_SYSCON_CMD_WRITE_POLESTAR_REG] = "WRITE_POLESTAR_REG";
    		cmdNames[PSP_SYSCON_CMD_READ_POLESTAR_REG] = "READ_POLESTAR_REG";
    		cmdNames[PSP_SYSCON_CMD_CTRL_DVE_POWER] = "CTRL_DVE_POWER";
    		cmdNames[PSP_SYSCON_CMD_CTRL_BT_POWER] = "CTRL_BT_POWER";
    		cmdNames[PSP_SYSCON_CMD_CTRL_USB_POWER] = "CTRL_USB_POWER";
    		cmdNames[PSP_SYSCON_CMD_CTRL_CHARGE] = "CTRL_CHARGE";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_NOP] = "BATTERY_NOP";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP] = "BATTERY_GET_STATUS_CAP";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_TEMP] = "BATTERY_GET_TEMP";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_VOLT] = "BATTERY_GET_VOLT";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_ELEC] = "BATTERY_GET_ELEC";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_RCAP] = "BATTERY_GET_RCAP";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_CAP] = "BATTERY_GET_CAP";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_FULL_CAP] = "BATTERY_GET_FULL_CAP";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_IFC] = "BATTERY_GET_IFC";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_LIMIT_TIME] = "BATTERY_GET_LIMIT_TIME";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_STATUS] = "BATTERY_GET_STATUS";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_CYCLE] = "BATTERY_GET_CYCLE";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_SERIAL] = "BATTERY_GET_SERIAL";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_INFO] = "BATTERY_GET_INFO";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_TEMP_AD] = "BATTERY_GET_TEMP_AD";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_VOLT_AD] = "BATTERY_GET_VOLT_AD";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_ELEC_AD] = "BATTERY_GET_ELEC_AD";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_TOTAL_ELEC] = "BATTERY_GET_TOTAL_ELEC";
    		cmdNames[PSP_SYSCON_CMD_BATTERY_GET_CHARGE_TIME] = "BATTERY_GET_CHARGE_TIME";

    		for (int i = 0; i < cmdNames.length; i++) {
    			if (cmdNames[i] == null) {
    				cmdNames[i] = String.format("UNKNOWN_CMD_0x%02X", i);
    			}
    		}
    	}

    	return cmdNames[cmd];
    }

    public int getPowerSupplyStatus() {
    	int powerSupplyStatus = 0xC0; // Unknown value

    	if (Battery.isPresent()) {
    		powerSupplyStatus |= 0x02; // Flag indicating that there is a battery present
    	}

    	return powerSupplyStatus;
    }

    public int getBatteryStatusCap1() {
    	return 0;
    }

    public int getBatteryStatusCap2() {
    	return 0;
    }

    public int getBatteryCycle() {
    	return 0;
    }

    public int getBatteryLimitTime() {
    	return 0;
    }

    public int getBatteryElec() {
    	return 0;
    }

    public int getPommelVersion() {
    	return 0;
    }

    public int getPowerStatus() {
    	return 0;
    }

    public int[] getTimeStamp() {
    	return new int[12];
    }

    public void readScratchpad(int src, int[] values, int size) {
    	System.arraycopy(scratchPad, src, values, 0, size);
    }

    public void writeScratchpad(int dst, int[] src, int size) {
    }

    public int readClock() {
    	return 0;
    }

    public void writeClock(int clock) {
    }

    public int readAlarm() {
    	return alarm;
    }

    public void writeAlarm(int alarm) {
    	this.alarm = alarm;
    }

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
	public int sceSysconCtrlWlanPower(boolean power) {
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
		// Tachyon = 0x00140000, Baryon = 0x00030600 TA-079 v1 1g
		// Tachyon = 0x00200000, Baryon = 0x00030600 TA-079 v2 1g
		// Tachyon = 0x00200000, Baryon = 0x00040600 TA-079 v3 1g
		// Tachyon = 0x00300000, Baryon = 0x00040600 TA-081 1g
		// Tachyon = 0x00400000, Baryon = 0x00114000 TA-082 1g
		// Tachyon = 0x00400000, Baryon = 0x00121000 TA-086 1g
		// Tachyon = 0x00500000, Baryon = 0x0022B200 TA-085 2g
		// Tachyon = 0x00500000, Baryon = 0x00234000 TA-085 2g
    	int baryon = 0;
    	switch (Model.getModel()) {
    		case Model.MODEL_PSP_FAT : baryon = 0x00030600; break;
    		case Model.MODEL_PSP_SLIM: baryon = 0x0022B200; break;
    		default:
    			log.warn(String.format("_sceSysconGetBaryonVersion unknown baryon version for PSP Model %s", Model.getModelName(Model.getModel())));
    			break;
    	}
    	return baryon;
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
    	int baryon = _sceSysconGetBaryonVersion();
    	baryonVersionAddr.setValue(baryon);

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
	public int sceSysconCmdExec(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.inout) TPointer packet, int flags) {
		int cmd = packet.getValue8(12);
		int len = packet.getValue8(13);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceSysconCmdExec cmd=0x%02X, len=0x%02X, txData: %s", cmd, len, Utilities.getMemoryDump(packet.getAddress() + 14, len - 2)));
		}
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
	public int sceSysconCmdExecAsync(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.inout) TPointer packet, int flags, TPointer callback, int argp) {
		int cmd = packet.getValue8(12);
		int len = packet.getValue8(13);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceSysconCmdExecAsync cmd=0x%02X, len=0x%02X, txData: %s", cmd, len, Utilities.getMemoryDump(packet.getAddress() + 14, len - 2)));
		}
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
    	int[] timeStamp = getTimeStamp();
    	for (int i = 0; i < timeStamp.length; i++) {
    		timeStampAddr.setValue8(i, (byte) timeStamp[i]);
    	}

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
    	pommelAddr.setValue(getPommelVersion());

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
    	statusAddr.setValue(getPowerStatus());

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
    	int values[] = new int[size];
    	readScratchpad(src, values, size);
    	for (int i = 0; i < scratchPad.length; i++) {
    		dst.setValue8(i, (byte) values[i]);
    	}

    	return 0;
	}

    /**
     * Write data to the scratchpad.
     *
     * @param dst  The scratchpad address to write to.
     * @param src  A pointer to the data to copy to the scratchpad.
     * @param size The size of the data to copy.
     * @return     0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x65EB6096, version = 150)
	public int sceSysconWriteScratchPad(int dst, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer src, int size) {
    	int[] values = new int[size];
    	for (int i = 0; i < size; i++) {
    		values[i] = src.getValue8(i);
    	}
    	writeScratchpad(dst, values, size);

    	return 0;
	}

    /**
     * Control an LED.
     *
     * @param led   The led to toggle (PSP_SYSCON_LED_xxx)
     * @param state Whether to turn on or off 
     * @return      < 0 on error
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x18BFBE65, version = 150)
	public int sceSysconCtrlLED(int led, boolean state) {
    	switch (led) {
    		case PSP_SYSCON_LED_MS: LED.setLedMemoryStickOn(state); break;
    		case PSP_SYSCON_LED_WLAN: LED.setLedWlanOn(state); break;
    		case PSP_SYSCON_LED_POWER: LED.setLedPowerOn(state); break;
    		case PSP_SYSCON_LED_BT: LED.setLedBluetoothOn(state); break;
    		default: return SceKernelErrors.ERROR_INVALID_INDEX;
    	}

    	return 0;
	}

    /**
     * Receive a parameter (used by power).
     *
     * @param id    The parameter ID.
     * @param param Pointer to a buffer (length 8) where will be copied the parameter.
     * @return      0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x08234E6D, version = 150)
	public int sceSysconReceiveSetParam(int id, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.out) TPointer param) {
    	return 0;
    }

    /**
     * Set a parameter (used by power).
     *
     * @param id    The parameter ID.
     * @param param Pointer to a buffer (length 8) the parameter will be set to.
     * @return      0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x992C22C2, version = 150)
	public int sceSysconSendSetParam(int id, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.in) TPointer param) {
    	return 0;
    }

    /**
     * Control the remote control power.
     *
     * @param power  1 is on, 0 is off
     * @return       < 0 on error 
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x44439604, version = 150)
	public int sceSysconCtrlHRPower(boolean power) {
    	return 0;
    }

    /**
     * Get the power supply status.
     *
     * @param statusAddr Pointer to a s32 where the power supply status will be stored.
     * @return           0 on success. 
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xFC32141A, version = 150)
	public int sceSysconGetPowerSupplyStatus(@BufferInfo(usage=Usage.out) TPointer32 statusAddr) {
    	statusAddr.setValue(getPowerSupplyStatus());
    	return 0;
    }

    /**
     * Get the power supply status.
     *
     * @param statusAddr Pointer to a s32 where the power supply status will be stored.
     * @return           0 on success. 
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x22240B41, version = 660)
	public int sceSysconGetPowerSupplyStatus_660(@BufferInfo(usage=Usage.out) TPointer32 statusAddr) {
    	return sceSysconGetPowerSupplyStatus(statusAddr);
    }

    /**
     * Get the battery status cap.
     *
     * @param unknown1 Pointer to an unknown s32 where a value will be stored. 
     * @param unknown2 Pointer to an unknown s32 where a value will be stored. 
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x6A53F3F8, version = 150)
	public int sceSysconBatteryGetStatusCap(@BufferInfo(usage=Usage.out) TPointer32 unknown1, @BufferInfo(usage=Usage.out) TPointer32 unknown2) {
    	unknown1.setValue(getBatteryStatusCap1());
    	unknown2.setValue(getBatteryStatusCap2());
    	return 0;
    }

    /**
     * Get the battery status cap.
     *
     * @param unknown1 Pointer to an unknown s32 where a value will be stored. 
     * @param unknown2 Pointer to an unknown s32 where a value will be stored. 
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x85F5F601, version = 660)
	public int sceSysconBatteryGetStatusCap_660(@BufferInfo(usage=Usage.out) TPointer32 unknown1, @BufferInfo(usage=Usage.out) TPointer32 unknown2) {
    	return sceSysconBatteryGetStatusCap(unknown1, unknown2);
    }

    /**
     * Get the battery full capacity.
     *
     * @param capAddr Pointer to a s32 where the capacity will be stored.
     * @return        0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x71135D7D, version = 150)
	public int sceSysconBatteryGetFullCap(@BufferInfo(usage=Usage.out) TPointer32 capAddr) {
    	capAddr.setValue(Battery.getFullCapacity());
    	return 0;
    }

    /**
     * Get the battery full capacity.
     *
     * @param capAddr Pointer to a s32 where the capacity will be stored.
     * @return        0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x4C871BEA, version = 660)
	public int sceSysconBatteryGetFullCap_660(@BufferInfo(usage=Usage.out) TPointer32 capAddr) {
    	return sceSysconBatteryGetFullCap(capAddr);
    }

    /**
     * Get the battery cycle.
     *
     * @param cycleAddr Pointer to a s32 where the cycle will be stored.
     * @return          0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xB5105D51, version = 150)
	public int sceSysconBatteryGetCycle(@BufferInfo(usage=Usage.out) TPointer32 cycleAddr) {
    	cycleAddr.setValue(getBatteryCycle());
    	return 0;
    }

    /**
     * Get the battery cycle.
     *
     * @param cycleAddr Pointer to a s32 where the cycle will be stored.
     * @return          0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x68AF19F1, version = 660)
	public int sceSysconBatteryGetCycle_660(@BufferInfo(usage=Usage.out) TPointer32 cycleAddr) {
    	return sceSysconBatteryGetCycle(cycleAddr);
    }

    /**
     * Get the battery limit time.
     *
     * @param timeAddr Pointer to a s32 where the limit time will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x284FE366, version = 150)
	public int sceSysconBatteryGetLimitTime(@BufferInfo(usage=Usage.out) TPointer32 timeAddr) {
    	timeAddr.setValue(getBatteryLimitTime());
    	return 0;
    }

    /**
     * Get the battery limit time.
     *
     * @param timeAddr Pointer to a s32 where the limit time will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x4D5A19BB, version = 660)
	public int sceSysconBatteryGetLimitTime_660(@BufferInfo(usage=Usage.out) TPointer32 timeAddr) {
    	return sceSysconBatteryGetLimitTime(timeAddr);
    }

    /**
     * Get the battery temperature.
     *
     * @param tempAddr Pointer to a s32 where the temperature will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x70C10E61, version = 150)
	public int sceSysconBatteryGetTemp(@BufferInfo(usage=Usage.out) TPointer32 tempAddr) {
    	tempAddr.setValue(Battery.getTemperature());
    	return 0;
    }

    /**
     * Get the battery temperature.
     *
     * @param tempAddr Pointer to a s32 where the temperature will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xCE8B6633, version = 150)
	public int sceSysconBatteryGetTemp_660(@BufferInfo(usage=Usage.out) TPointer32 tempAddr) {
    	return sceSysconBatteryGetTemp(tempAddr);
    }

    /**
     * Get the battery electric charge.
     *
     * @param elecAddr Pointer to a s32 where the charge will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x373EC933, version = 150)
	public int sceSysconBatteryGetElec(@BufferInfo(usage=Usage.out) TPointer32 elecAddr) {
    	elecAddr.setValue(getBatteryElec());
    	return 0;
    }

    /**
     * Get the battery electric charge.
     *
     * @param elecAddr Pointer to a s32 where the charge will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x483088B0, version = 660)
	public int sceSysconBatteryGetElec_660(@BufferInfo(usage=Usage.out) TPointer32 elecAddr) {
    	return sceSysconBatteryGetElec(elecAddr);
    }

    /**
     * Get the battery voltage.
     *
     * @param voltAddr Pointer to a s32 where the voltage will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x8BDEBB1E, version = 150)
	public int sceSysconBatteryGetVolt(@BufferInfo(usage=Usage.out) TPointer32 voltAddr) {
    	voltAddr.setValue(Battery.getVoltage());
    	return 0;
    }

    /**
     * Get the battery voltage.
     *
     * @param voltAddr Pointer to a s32 where the voltage will be stored.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xA7DB34BB, version = 660)
	public int sceSysconBatteryGetVolt_660(@BufferInfo(usage=Usage.out) TPointer32 voltAddr) {
    	return sceSysconBatteryGetVolt(voltAddr);
    }

    /**
     * Read the PSP clock.
     *
     * @param clockAddr Pointer to a s32 where the clock will be stored.
     * @return          0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xC4D66C1D, version = 150)
	public int sceSysconReadClock(@BufferInfo(usage=Usage.out) TPointer32 clockAddr) {
    	clockAddr.setValue(readClock());
    	return 0;
    }

    /**
     * Read the PSP clock.
     *
     * @param clockAddr Pointer to a s32 where the clock will be stored.
     * @return          0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xF436BB12, version = 150)
	public int sceSysconReadClock_660(@BufferInfo(usage=Usage.out) TPointer32 clockAddr) {
    	return sceSysconReadClock(clockAddr);
    }

    /**
     * Read the PSP alarm.
     *
     * @param alarmAddr Pointer to a s32 where the alarm will be stored.
     * @return          0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x7A805EE4, version = 150)
	public int sceSysconReadAlarm(@BufferInfo(usage=Usage.out) TPointer32 alarmAddr) {
    	alarmAddr.setValue(readAlarm());
    	return 0;
    }

    /**
     * Read the PSP alarm.
     *
     * @param alarmAddr Pointer to a s32 where the alarm will be stored.
     * @return          0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xF2AE6D5E, version = 660)
	public int sceSysconReadAlarm_660(@BufferInfo(usage=Usage.out) TPointer32 alarmAddr) {
    	return sceSysconReadAlarm(alarmAddr);
    }

    /**
     * Set the PSP alarm.
     *
     * @param alarm The alarm value to set the PSP alarm to.
     * @return      0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x6C911742, version = 150)
	public int sceSysconWriteAlarm(int alarm) {
    	writeAlarm(alarm);
    	return 0;
    }

    /**
     * Set the PSP alarm.
     *
     * @param alarm The alarm value to set the PSP alarm to.
     * @return      0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x80711575, version = 150)
	public int sceSysconWriteAlarm_660(int alarm) {
    	return sceSysconWriteAlarm(alarm);
    }

    /**
     * Send a command to the syscon doing nothing.
     *
     * @return      0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xE6B74CB9, version = 150)
	public int sceSysconNop() {
    	return 0;
    }

    /**
     * Set the low battery callback, that will be ran when the battery is low.
     *
     * @param callback         The callback function.
     * @param callbackArgument The second argument that will be passed to the callback.
     * @return                 0.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xAD555CE5, version = 150)
	public int sceSysconSetLowBatteryCallback(TPointer callback, int callbackArgument) {
    	return 0;
    }

    /**
     * Set the low battery callback, that will be ran when the battery is low.
     *
     * @param callback         The callback function.
     * @param callbackArgument The second argument that will be passed to the callback.
     * @return                 0.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x599EB8A0, version = 660)
	public int sceSysconSetLowBatteryCallback_660(TPointer callback, int callbackArgument) {
    	return sceSysconSetLowBatteryCallback(callback, callbackArgument);
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0xA3406117, version = 150)
	public boolean sceSysconIsAcSupplied() {
    	// Has no parameters
    	return true;
    }

    /**
     * Set the Ac supply callback, that will be ran when the PSP Ac power is (dis)connected (probably).
     *
     * @param callback         The callback function.
     * @param callbackArgument The second argument that will be passed to the callback.
     * @return                 0.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xE540E532, version = 150)
	public int sceSysconSetAcSupplyCallback(TPointer callback, int callbackArgument) {
    	return 0;
    }

    /**
     * Set the Ac supply callback, that will be ran when the PSP Ac power is (dis)connected (probably).
     *
     * @param callback         The callback function.
     * @param callbackArgument The second argument that will be passed to the callback.
     * @return                 0.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x657DCEF7, version = 660)
	public int sceSysconSetAcSupplyCallback_660(TPointer callback, int callbackArgument) {
    	return sceSysconSetAcSupplyCallback(callback, callbackArgument);
    }

    /**
     * Set the power control
     *
     * @param unknown1 Unknown.
     * @param unknown2 Unknown.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xBE27FE66, version = 150)
	public int sceSysconCtrlPower(int unknown1, int unknown2) {
    	return 0;
    }

    /**
     * Set the power control
     *
     * @param unknown1 Unknown.
     * @param unknown2 Unknown.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xEDD3AB8B, version = 660)
	public int sceSysconCtrlPower_660(int unknown1, int unknown2) {
    	return sceSysconCtrlPower(unknown1, unknown2);
    }

    /**
     * Set the voltage.
     *
     * @param unknown1 Unknown.
     * @param unknown2 Unknown.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x01677F91, version = 150)
	public int sceSysconCtrlVoltage(int unknown1, int unknown2) {
    	return 0;
    }

    /**
     * Set the voltage.
     *
     * @param unknown1 Unknown.
     * @param unknown2 Unknown.
     * @return         0 on success.
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0xF7BCD2A6, version = 660)
	public int sceSysconCtrlVoltage_660(int unknown1, int unknown2) {
    	return sceSysconCtrlVoltage(unknown1, unknown2);
    }
}
