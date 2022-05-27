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
package jpcsp.network.protocols;

import static jpcsp.network.protocols.NetPacket.getIpAddressString;

import java.io.EOFException;

import jpcsp.HLE.kernel.types.pspNetMacAddress;

public class ARP {
	// See https://en.wikipedia.org/wiki/Address_Resolution_Protocol
    public static final int ARP_OPERATION_REQUEST = 1;
    public static final int ARP_OPERATION_REPLY = 2;
    private static final String[] ARP_OPERATION_NAMES = {
    	null, "REQUEST", "REPLY"
    };
	public int hardwareType;
	public int protocolType;
	public int hardwareAddressLength;
	public int protocolAddressLength;
	public int operation;
	public pspNetMacAddress senderHardwareAddress;
	public byte[] senderProtocolAddress;
	public pspNetMacAddress targetHardwareAddress;
	public byte[] targetProtocolAddress;

	public void read(NetPacket packet) throws EOFException {
		hardwareType = packet.read16();
		protocolType = packet.read16();
		hardwareAddressLength = packet.read8();
		protocolAddressLength = packet.read8();
		operation = packet.read16();
		senderHardwareAddress = packet.readMacAddress(hardwareAddressLength);
		senderProtocolAddress = packet.readIpAddress(protocolAddressLength);
		targetHardwareAddress = packet.readMacAddress(hardwareAddressLength);
		targetProtocolAddress = packet.readIpAddress(protocolAddressLength);
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.write16(hardwareType);
		packet.write16(protocolType);
		packet.write8(hardwareAddressLength);
		packet.write8(protocolAddressLength);
		packet.write16(operation);
		packet.writeMacAddress(senderHardwareAddress, hardwareAddressLength);
		packet.writeIpAddress(senderProtocolAddress, protocolAddressLength);
		packet.writeMacAddress(targetHardwareAddress, hardwareAddressLength);
		packet.writeIpAddress(targetProtocolAddress, protocolAddressLength);

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	public int sizeOf() {
		return 8 + 2 * (hardwareAddressLength + protocolAddressLength);
	}

	@Override
	public String toString() {
		return String.format("operation=%s, sender=%s/%s, target=%s/%s", ARP_OPERATION_NAMES[operation], senderHardwareAddress, getIpAddressString(senderProtocolAddress), targetHardwareAddress, getIpAddressString(targetProtocolAddress));
	}
}
