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

public class SceNetIfMessage extends pspAbstractMemoryMappedStructure {
	public static final int TYPE_SHORT_MESSAGE = 2;
	public static final int TYPE_LONG_MESSAGE = 9 | TYPE_SHORT_MESSAGE;
	public static final int TYPE_MULTICAST_ANY = 0x100; // The message has been addressed to the MAC address FF:FF:FF:FF:FF:FF
	public static final int TYPE_MULTICAST_GROUP = 0x200; // The message has been addresses to a multicast MAC address, but which is not FF:FF:FF:FF:FF:FF
	public int nextDataAddr;
	public int nextMessageAddr;
	public int dataAddr;
	public int dataLength;
	public int unknown16;
	public int type;
	public int unknown20;
	public int totalDataLength;
	public int unknown28;
	public int unknown32;
	public int unknown34;
	public int unknown48;
	public int unknown60;
	public int unknown68;
	public int unknown72;

	@Override
	protected void read() {
		nextDataAddr = read32(); // Offset 0
		nextMessageAddr = read32(); // Offset 4
		dataAddr = read32(); // Offset 8
		dataLength = read32(); // Offset 12
		unknown16 = read16(); // Offset 16
		type = read16(); // Offset 18
		unknown20 = read32(); // Offset 20
		totalDataLength = read32(); // Offset 24
		unknown28 = read32(); // Offset 28
		unknown32 = (short) read16(); // Offset 32
		unknown34 = (short) read16(); // Offset 34
		readUnknown(12); // Offset 36
		unknown48 = read32(); // Offset 48
		readUnknown(8); // Offset 52
		unknown60 = read32(); // Offset 60
		readUnknown(4); // Offset 64
		unknown68 = read32(); // Offset 68
		unknown72 = read32(); // Offset 72
	}

	@Override
	protected void write() {
		write32(nextDataAddr);
		write32(nextMessageAddr);
		write32(dataAddr);
		write32(dataLength);
		write16((short) unknown16);
		write16((short) type);
		write32(unknown20);
		write32(totalDataLength);
		write32(unknown28);
		write16((short) unknown32);
		write16((short) unknown34);
		writeSkip(12);
		write32(unknown48);
		writeSkip(8);
		write32(unknown60);
		writeSkip(4);
		write32(unknown68);
		write32(unknown72);
	}

	@Override
	public int sizeof() {
		return 76;
	}

	@Override
	public String toString() {
		return String.format("nextDataAddr=0x%08X, nextMessageAddr=0x%08X, dataAddr=0x%08X, dataLength=0x%X, unknown16=0x%X, type=0x%X, unknown20=0x%X, totalDataLength=0x%X, unknown28=0x%X, unknown32=0x%X, unknown34=0x%X", nextDataAddr, nextMessageAddr, dataAddr, dataLength, unknown16, type, unknown20, totalDataLength, unknown28, unknown32, unknown34);
	}
}
