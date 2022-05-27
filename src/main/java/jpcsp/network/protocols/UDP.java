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

import java.io.EOFException;

public class UDP {
	// UDP packet structure, see https://en.wikipedia.org/wiki/User_Datagram_Protocol
    public static final int UDP_PORT_DNS = 53;
    public static final int UDP_PORT_DHCP_SERVER = 67;
    public static final int UDP_PORT_DHCP_CLIENT = 68;
    public static final int UDP_PORT_SSDP = 1900;
	public int sourcePort;
	public int destinationPort;
	public int length;
	public int checksum;

	public UDP() {
	}

	public UDP(UDP udp) {
		sourcePort = udp.sourcePort;
		destinationPort = udp.destinationPort;
		length = udp.length;
		checksum = udp.checksum;
	}

	public void swapSourceAndDestination() {
		int port = sourcePort;
		sourcePort = destinationPort;
		destinationPort = port;
	}

	public void computeChecksum() {
		// The checksum field carries all-zeros if unused.
		checksum = 0;
	}

	public void read(NetPacket packet) throws EOFException {
		sourcePort = packet.read16();
		destinationPort = packet.read16();
		length = packet.read16();
		checksum = packet.read16();
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.write16(sourcePort);
		packet.write16(destinationPort);
		packet.write16(length);
		packet.write16(checksum);

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	public int sizeOf() {
		return 8;
	}

	@Override
	public String toString() {
		return String.format("sourcePort=0x%X, destinationPort=0x%X, length=0x%X, checksum=0x%04X", sourcePort, destinationPort, length, checksum);
	}
}
