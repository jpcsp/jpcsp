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

public class SceNetWlanMessage extends pspAbstractMemoryMappedStructure {
	public static final int[] contentLengthFromMessageType = new int[] { -1, 0, 0x80, 0x120, 0x110, 0x100, 0x60, 0x20, 0x30 };
	public static final int maxContentLength = 0x120;
	public static final int WLAN_PROTOCOL_TYPE_SONY = 0x88C8;
	public static final int WLAN_PROTOCOL_SUBTYPE_CONTROL = 0x01;
	public static final int WLAN_PROTOCOL_SUBTYPE_DATA = 0x02;
	public pspNetMacAddress dstMacAddress;
	public pspNetMacAddress srcMacAddress;
	public int protocolType; // 0x88C8
	public int protocolSubType; // 1 or 2
	public int unknown16; // 1
	public int controlType; // [1..8]
	public int contentLength;

	@Override
	protected void read() {
		dstMacAddress = new pspNetMacAddress();
		read(dstMacAddress); // Offset 0
		srcMacAddress = new pspNetMacAddress();
		read(srcMacAddress); // Offset 6
		protocolType = endianSwap16((short) read16()); // Offset 12
		protocolSubType = endianSwap16((short) read16()); // Offset 14
		unknown16 = read8(); // Offset 16
		controlType = read8(); // Offset 17
		contentLength = endianSwap16((short) read16()); // Offset 18
	}

	@Override
	protected void write() {
		write(dstMacAddress); // Offset 0
		write(srcMacAddress); // Offset 6
		write16((short) endianSwap16((short) protocolType)); // Offset 12
		write16((short) endianSwap16((short) protocolSubType)); // Offset 14
		write8((byte) unknown16); // Offset 16
		write8((byte) controlType); // Offset 17
		write16((short) endianSwap16((short) contentLength)); // Offset 18
	}

	@Override
	public int sizeof() {
		return 20;
	}

	@Override
	public String toString() {
		return String.format("dstMac=%s, srcMac=%s, protocolType=0x%X, protocolSubType=0x%X, unknown16=0x%X, controlType=0x%X, contentLength=0x%X", dstMacAddress, srcMacAddress, protocolType, protocolSubType, unknown16, controlType, contentLength);
	}
}
