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
package jpcsp.hardware;

import static jpcsp.HLE.modules150.sceNet.convertStringToMacAddress;

import java.util.Random;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.modules150.sceUtility;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;

public class Wlan {
	private static Logger log = Emulator.log;
    public static int PSP_WLAN_SWITCH_OFF = 0;
    public static int PSP_WLAN_SWITCH_ON = 1;
    private static int switchState = PSP_WLAN_SWITCH_ON;
    public final static int MAC_ADDRESS_LENGTH = 6;
    private static byte[] macAddress = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };
    private final static String settingsMacAddress = "macAddress";
    public static int PSP_ADHOC_CHANNEL_AUTO = 0;
    public static int PSP_ADHOC_CHANNEL_DEFAULT = 11;
    private static int signalStrength = 100;
    private static WlanSwitchSettingsListener wlanSwitchSettingsListener;

    private static class WlanSwitchSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setSwitchState(value ? PSP_WLAN_SWITCH_ON : PSP_WLAN_SWITCH_OFF);
		}
    }

    public static void initialize() {
    	String macAddressString = Settings.getInstance().readString(settingsMacAddress);
    	if (macAddressString == null || macAddressString.length() <= 0) {
    		// MAC Address not set from the settings file, generate a random one
    		Random random = new Random();
    		for (int i = 0; i < macAddress.length; i++) {
    			macAddress[i] = (byte) random.nextInt(256);
    		}
    		// Both least significant bits of the first byte have a special meaning
    		// (see http://en.wikipedia.org/wiki/Mac_address):
    		// bit 0: 0=Unicast / 1=Multicast
    		// bit 1: 0=Globally unique / 1=Locally administered
    		macAddress[0] &= 0xFC;
    		// Do not save the new MAC address to the settings so that different instances
    		// of Jpcsp are using different MAC addresses (required for Adhoc networking).
    		//Settings.getInstance().writeString(settingsMacAddress, convertMacAddressToString(macAddress));
    	} else {
    		macAddress = convertStringToMacAddress(macAddressString);
    		// Both least significant bits of the first byte have a special meaning
    		// (see http://en.wikipedia.org/wiki/Mac_address):
    		// bit 0: 0=Unicast / 1=Multicast
    		// bit 1: 0=Globally unique / 1=Locally administered
    		macAddress[0] &= 0xFC;
    	}

    	if (wlanSwitchSettingsListener == null) {
    		wlanSwitchSettingsListener = new WlanSwitchSettingsListener();
    		Settings.getInstance().registerSettingsListener("WlanSwitch", "network.wlanSwitchOn", wlanSwitchSettingsListener);
    	}
    }

    public static int getSwitchState() {
		return switchState;
	}

    public static void setSwitchState(int switchState) {
		Wlan.switchState = switchState;
    	log.info(String.format("WLAN Switch %s", switchState == PSP_WLAN_SWITCH_OFF ? "off" : "on"));
	}

    public static byte[] getMacAddress() {
    	return macAddress;
    }

    public static void setMacAddress(byte[] newMacAddress) {
    	System.arraycopy(newMacAddress, 0, macAddress, 0, MAC_ADDRESS_LENGTH);
    }

    public static int getAdhocChannel() {
    	int channel = sceUtility.getSystemParamAdhocChannel();
    	if (channel == PSP_ADHOC_CHANNEL_AUTO) {
    		channel = PSP_ADHOC_CHANNEL_DEFAULT;
    	}

    	return channel;
    }

    public static int getSignalStrenth() {
    	return signalStrength;
    }
}
