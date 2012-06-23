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

import jpcsp.HLE.modules150.sceNet;
import jpcsp.HLE.modules150.sceNetAdhoc;
import jpcsp.hardware.Wlan;

public class pspNetMacAddress extends pspAbstractMemoryMappedStructure {
	public final byte[] macAddress = new byte[Wlan.MAC_ADDRESS_LENGTH];

	@Override
	protected void read() {
		for (int i = 0; i < macAddress.length; i++) {
			macAddress[i] = (byte) read8();
		}
	}

	@Override
	protected void write() {
		for (int i = 0; i < macAddress.length; i++) {
			write8(macAddress[i]);
		}
	}

	public void setMacAddress(byte[] macAddress) {
		setMacAddress(macAddress, 0);
	}

	public void setMacAddress(byte[] macAddress, int offset) {
		System.arraycopy(macAddress, offset, this.macAddress, 0, Math.min(macAddress.length - offset, this.macAddress.length));
	}

	@Override
	public int sizeof() {
		return macAddress.length;
	}

	/**
	 * Is the MAC address the special ANY MAC address (FF:FF:FF:FF:FF:FF)?
	 * 
	 * @return    true if this is the special ANY MAC address
	 *            false otherwise
	 */
	public boolean isAnyMacAddress() {
		for (int i = 0; i < macAddress.length; i++) {
			if (macAddress[i] != (byte) 0xFF) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof pspNetMacAddress) {
			pspNetMacAddress macAddress = (pspNetMacAddress) object;
			return sceNetAdhoc.isSameMacAddress(macAddress.macAddress, this.macAddress);
		}
		return super.equals(object);
	}

	public boolean equals(byte[] macAddress) {
		return sceNetAdhoc.isSameMacAddress(macAddress, this.macAddress);
	}

	@Override
	public String toString() {
		return sceNet.convertMacAddressToString(macAddress);
	}
}
