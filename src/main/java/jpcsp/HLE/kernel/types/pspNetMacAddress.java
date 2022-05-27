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

import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.validMacAddressOUIs;
import static jpcsp.util.Utilities.hasBit;

import java.util.Random;

import jpcsp.hardware.Wlan;

public class pspNetMacAddress extends pspAbstractMemoryMappedStructure {
	public final byte[] macAddress = new byte[MAC_ADDRESS_LENGTH];

	public pspNetMacAddress() {
	}

	public pspNetMacAddress(byte[] macAddress) {
		setMacAddress(macAddress);
	}

	public pspNetMacAddress(byte[] macAddress, int offset) {
		setMacAddress(macAddress, offset);
	}

	@Override
	protected void read() {
		for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
			macAddress[i] = (byte) read8();
		}
	}

	@Override
	protected void write() {
		for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
			write8(macAddress[i]);
		}
	}

	public void setMacAddress(byte[] macAddress) {
		setMacAddress(macAddress, 0);
	}

	public void setMacAddress(byte[] macAddress, int offset) {
		System.arraycopy(macAddress, offset, this.macAddress, 0, MAC_ADDRESS_LENGTH);
	}

	@Override
	public int sizeof() {
		return MAC_ADDRESS_LENGTH;
	}

	/**
	 * Is the MAC address the special ANY MAC address (FF:FF:FF:FF:FF:FF)?
	 * 
	 * @return    true if this is the special ANY MAC address
	 *            false otherwise
	 */
	public boolean isAnyMacAddress() {
		return isAnyMacAddress(macAddress);
	}

	/**
	 * Is the MAC address the special ANY MAC address (FF:FF:FF:FF:FF:FF)?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @return    true if this is the special ANY MAC address
	 *            false otherwise
	 */
	public static boolean isAnyMacAddress(byte[] macAddress) {
		return isAnyMacAddress(macAddress, 0);
	}

	/**
	 * Is the MAC address the special ANY MAC address (FF:FF:FF:FF:FF:FF)?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @param offset     the buffer offset where the MAC address is stored
	 * @return    true if this is the special ANY MAC address
	 *            false otherwise
	 */
	public static boolean isAnyMacAddress(byte[] macAddress, int offset) {
		for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
			if (macAddress[offset + i] != (byte) 0xFF) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Is the MAC address the empty MAC address (00:00:00:00:00:00)?
	 * 
	 * @return    true if this is the empty MAC address
	 *            false otherwise
	 */
	public boolean isEmptyMacAddress() {
		return isEmptyMacAddress(macAddress);
	}

	/**
	 * Is the MAC address the empty MAC address (00:00:00:00:00:00)?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @return    true if this is the empty MAC address
	 *            false otherwise
	 */
	public static boolean isEmptyMacAddress(byte[] macAddress) {
		return isEmptyMacAddress(macAddress, 0);
	}

	/**
	 * Is the MAC address the empty MAC address (00:00:00:00:00:00)?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @param offset     the buffer offset where the MAC address is stored
	 * @return    true if this is the empty MAC address
	 *            false otherwise
	 */
	public static boolean isEmptyMacAddress(byte[] macAddress, int offset) {
		for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
			if (macAddress[offset + i] != (byte) 0x00) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Is the MAC address a multicast MAC address?
	 * 
	 * @return true if the MAC address is a multicast MAC address
	 */
	public boolean isMulticastMacAddress() {
		return isMulticastMacAddress(macAddress);
	}

	/**
	 * Is the MAC address a multicast MAC address?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @return true if the MAC address is a multicast MAC address
	 */
	public static boolean isMulticastMacAddress(byte[] macAddress) {
		return isMulticastMacAddress(macAddress, 0);
	}

	/**
	 * Is the MAC address a multicast MAC address?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @param offset     the buffer offset where the MAC address is stored
	 * @return true if the MAC address is a multicast MAC address
	 */
	public static boolean isMulticastMacAddress(byte[] macAddress, int offset) {
		// See http://en.wikipedia.org/wiki/Mac_address:
		// bit 0: 0=Unicast / 1=Multicast
		return hasBit(macAddress[offset], 0);
	}

	/**
	 * Is this MAC address matching my own MAC address?
	 * 
	 * @return true if this MAC address is matching my own MAC address
	 */
	public boolean isMyMacAddress() {
		return isMyMacAddress(macAddress);
	}

	/**
	 * Is the given MAC address matching my own MAC address?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @return true      if the MAC address is matching my own MAC address
	 */
	public static boolean isMyMacAddress(byte[] macAddress) {
		return isMyMacAddress(macAddress, 0);
	}

	/**
	 * Is the given MAC address matching my own MAC address?
	 * 
	 * @param macAddress the buffer containing the MAC address
	 * @param offset     the buffer offset where the MAC address is stored
	 * @return true      if the MAC address is matching my own MAC address
	 */
	public static boolean isMyMacAddress(byte[] macAddress, int offset) {
		return equals(Wlan.getMacAddress(), 0, macAddress, offset);
	}

	/**
	 * Is this MAC address equal to the given Object?
	 * 
	 * @param object the object
	 * @return true  if the object is a pspNetMacAddress instance
	 *               with a MAC address which is equal to this MAC address
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof pspNetMacAddress) {
			pspNetMacAddress macAddress = (pspNetMacAddress) object;
			return equals(macAddress.macAddress);
		}
		return super.equals(object);
	}

	/**
	 * Is this MAC address equal to the given MAC address?
	 * 
	 * @param macAddress the buffer containing the given MAC address
	 * @return true      if this MAC address is equal to the given MAC address
	 */
	public boolean equals(byte[] macAddress) {
		return equals(macAddress, this.macAddress);
	}

	/**
	 * Are two given MAC addresses equal?
	 * 
	 * @param macAddress1 the buffer containing the first MAC address
	 * @param macAddress2 the buffer containing the second MAC address
	 * @return true       if both MAC addresses are equal
	 */
	public static boolean equals(byte[] macAddress1, byte[] macAddress2) {
		return equals(macAddress1, 0, macAddress2, 0);
	}

	/**
	 * Are two given MAC addresses equal?
	 * 
	 * @param macAddress1 the buffer containing the first MAC address
	 * @param offset1     the buffer offset where the first MAC address is stored
	 * @param macAddress2 the buffer containing the second MAC address
	 * @param offset2     the buffer offset where the second MAC address is stored
	 * @return true       if both MAC addresses are equal
	 */
	public static boolean equals(byte[] macAddress1, int offset1, byte[] macAddress2, int offset2) {
		if (macAddress1 == null) {
			return macAddress2 == null;
		}
		if (macAddress2 == null) {
			return false;
		}

		for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
			if (macAddress1[offset1 + i] != macAddress2[offset2 + i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Generate a random MAC address.
	 * The generated MAC address will have a valid OUI
	 * (Organizationally Unique Identifier),
	 * i.e. one of the possible OUI's used by real PSPs.
	 * 
	 * @return byte[] the generated random MAC address
	 */
	public static byte[] getRandomMacAddress() {
		byte[] macAddress = new byte[MAC_ADDRESS_LENGTH];
		Random random = new Random();

		// Select one random OUI from the list of valid ones
		byte[] oui = validMacAddressOUIs[random.nextInt(validMacAddressOUIs.length)];
		macAddress[0] = oui[0];
		macAddress[1] = oui[1];
		macAddress[2] = oui[2];

		for (int i = 3; i < MAC_ADDRESS_LENGTH; i++) {
			macAddress[i] = (byte) random.nextInt(256);
		}

		// Both least significant bits of the first byte have a special meaning
		// (see http://en.wikipedia.org/wiki/Mac_address):
		// bit 0: 0=Unicast / 1=Multicast
		// bit 1: 0=Globally unique / 1=Locally administered
		macAddress[0] &= 0xFC;

		return macAddress;
	}

    /**
     * Convert a 6-byte MAC address into a string representation (xx:xx:xx:xx:xx:xx)
     * in lower-case.
     * The PSP always returns MAC addresses in lower-case.
     *
	 * @param macAddress the buffer containing the MAC address
     * @return           string representation of the MAC address: xx:xx:xx:xx:xx:xx (in lower-case).
     */
	public static String toString(byte[] macAddress) {
		return toString(macAddress, 0);
	}

    /**
     * Convert a 6-byte MAC address into a string representation (xx:xx:xx:xx:xx:xx)
     * in lower-case.
     * The PSP always returns MAC addresses in lower-case.
     *
	 * @param macAddress the buffer containing the MAC address
	 * @param offset     the buffer offset where the MAC address is stored
     * @return           string representation of the MAC address: xx:xx:xx:xx:xx:xx (in lower-case).
     */
	public static String toString(byte[] macAddress, int offset) {
    	return String.format("%02x:%02x:%02x:%02x:%02x:%02x", macAddress[offset + 0], macAddress[offset + 1], macAddress[offset + 2], macAddress[offset + 3], macAddress[offset + 4], macAddress[offset + 5]);
	}

	@Override
	public String toString() {
		// When the base address is not set, return the MAC address only:
		// "nn:nn:nn:nn:nn:nn"
		if (getBaseAddress() == 0) {
			return toString(macAddress);
		}
		// When the MAC address is not set, return the base address only:
		// "0xNNNNNNNN"
		if (isEmptyMacAddress()) {
			return super.toString();
		}

		// When both the base address and the MAC address are set,
		// return "0xNNNNNNNN(nn:nn:nn:nn:nn:nn)"
		return String.format("%s(%s)", super.toString(), toString(macAddress));
	}
}
