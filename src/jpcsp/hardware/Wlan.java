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

public class Wlan {
    public static int PSP_WLAN_SWITCH_OFF = 0;
    public static int PSP_WLAN_SWITCH_ON = 1;
    private static int switchState = PSP_WLAN_SWITCH_ON;
    public final static int MAC_ADDRESS_LENGTH = 6;
    private static byte[] macAddress = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };

    public static int getSwitchState() {
		return switchState;
	}

    public static void setSwitchState(int switchState) {
		Wlan.switchState = switchState;
	}

    public static byte[] getMacAddress() {
    	return macAddress;
    }

    public static void setMacAddress(byte[] newMacAddress) {
    	System.arraycopy(newMacAddress, 0, macAddress, 0, MAC_ADDRESS_LENGTH);
    }
}
