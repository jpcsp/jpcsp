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

import static jpcsp.network.protocols.InternetChecksum.computeInternetChecksum;

import java.io.EOFException;

import jpcsp.util.Utilities;

public class ICMP {
	// ICMP packet format, see https://en.wikipedia.org/wiki/Internet_Control_Message_Protocol
	public static final int ICMP_CONTROL_ECHO_REPLY = 0; // Ping reply
	public static final int ICMP_CONTROL_ECHO_REQUEST = 8; // Used to ping
	public int type;
	public int code;
	public int checksum;
	public int restOfHeader;
	public byte[] payload;

	public ICMP() {
	}

	public ICMP(ICMP icmp) {
		type = icmp.type;
		code = icmp.code;
		checksum = icmp.checksum;
		restOfHeader = icmp.restOfHeader;
		payload = icmp.payload;
	}

	public void read(NetPacket packet) throws EOFException {
		type = packet.read8();
		code = packet.read8();
		checksum = packet.read16();
		restOfHeader = packet.read32();
		// The rest of the packet is the payload data
		payload = packet.readBytes(packet.getLength());
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.write8(type);
		packet.write8(code);
		packet.write16(checksum);
		packet.write32(restOfHeader);
		packet.writeBytes(payload);

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	public void computeChecksum() throws EOFException {
		// Computes the checksum with 0 at the checksum field
		checksum = 0;
		NetPacket checksumPacket = write();
		checksum = computeInternetChecksum(checksumPacket.getBuffer(), 0, checksumPacket.getOffset());
	}

	public int sizeOf() {
		int size = 8;
		if (payload != null) {
			size += payload.length;
		}

		return size;
	}

	@Override
	public String toString() {
		return String.format("type=0x%X, code=0x%X, checksum=0x%04X, restOfHeader=0x%08X, payload=%s", type, code, checksum, restOfHeader, Utilities.getMemoryDump(payload, 0, payload.length));
	}
}
