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
import static jpcsp.network.protocols.NetPacket.getIpAddressString;

import java.io.EOFException;

import jpcsp.util.Utilities;

public class IPv4 {
	// IPv4 packet format, see https://en.wikipedia.org/wiki/IPv4
    public static final int IPv4_PROTOCOL_ICMP = 1;
    public static final int IPv4_PROTOCOL_TCP = 6;
    public static final int IPv4_PROTOCOL_UDP = 17;
	public int version;
	public int internetHeaderLength;
	public int differentiatedServicesCodePoint;
	public int explicitCongestionNotification;
	public int totalLength;
	public int identification;
	public int flags;
	public int fragmentOffset;
	public int timeToLive;
	public int protocol;
	public int headerChecksum;
	public byte[] sourceIPAddress;
	public byte[] destinationIPAddress;
	public byte[] options;

	public IPv4() {
		version = 4;
		internetHeaderLength = 5;
		timeToLive = 0x40;
	}

	public IPv4(IPv4 ipv4) {
		version = ipv4.version;
		internetHeaderLength = ipv4.internetHeaderLength;
		differentiatedServicesCodePoint = ipv4.differentiatedServicesCodePoint;
		explicitCongestionNotification = ipv4.explicitCongestionNotification;
		totalLength = ipv4.totalLength;
		identification = ipv4.identification;
		flags = ipv4.flags;
		fragmentOffset = ipv4.fragmentOffset;
		timeToLive = ipv4.timeToLive;
		protocol = ipv4.protocol;
		headerChecksum = ipv4.headerChecksum;
		sourceIPAddress = ipv4.sourceIPAddress;
		destinationIPAddress = ipv4.destinationIPAddress;
		options = ipv4.options;
	}

	private int getOptionsLength() {
		return Math.max((internetHeaderLength - 5) * 4, 0);
	}

	public void swapSourceAndDestination() {
		byte[] ip = sourceIPAddress;
		sourceIPAddress = destinationIPAddress;
		destinationIPAddress = ip;
	}

	public void computeChecksum() throws EOFException {
		// Computes the checksum with 0 at the headerChecksum field
		headerChecksum = 0;
		NetPacket checksumPacket = write();
		headerChecksum = computeInternetChecksum(checksumPacket.getBuffer(), 0, checksumPacket.getOffset());
	}

	public void read(NetPacket packet) throws EOFException {
		version = packet.readBits(4);
		internetHeaderLength = packet.readBits(4);
		differentiatedServicesCodePoint = packet.readBits(6);
		explicitCongestionNotification = packet.readBits(2);
		totalLength = packet.read16();
		identification = packet.read16();
		flags = packet.readBits(3);
		fragmentOffset = packet.readBits(13);
		timeToLive = packet.read8();
		protocol = packet.read8();
		headerChecksum = packet.read16();
		sourceIPAddress = packet.readIpAddress();
		destinationIPAddress = packet.readIpAddress();
		options = packet.readBytes(getOptionsLength());
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.writeBits(version, 4);
		packet.writeBits(internetHeaderLength, 4);
		packet.writeBits(differentiatedServicesCodePoint, 6);
		packet.writeBits(explicitCongestionNotification, 2);
		packet.write16(totalLength);
		packet.write16(identification);
		packet.writeBits(flags, 3);
		packet.writeBits(fragmentOffset, 13);
		packet.write8(timeToLive);
		packet.write8(protocol);
		packet.write16(headerChecksum);
		packet.writeIpAddress(sourceIPAddress);
		packet.writeIpAddress(destinationIPAddress);
		packet.writeBytes(options, 0, getOptionsLength());

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	public int sizeOf() {
		return internetHeaderLength * 4;
	}

	@Override
	public String toString() {
		return String.format("version=0x%X, internetHeaderLength=0x%X, differentiatedServicesCodePoint=0x%X, explicitCongestionNotification=0x%X, totalLength=0x%X, identification=0x%X, flags=0x%X, fragmentOffset=0x%X, timeToLive=0x%X, protocol=0x%X, headerChecksum=0x%04X, sourceIP=%s, destinationIP=%s, options=%s", version, internetHeaderLength, differentiatedServicesCodePoint, explicitCongestionNotification, totalLength, identification, flags, fragmentOffset, timeToLive, protocol, headerChecksum, getIpAddressString(sourceIPAddress), getIpAddressString(destinationIPAddress), Utilities.getMemoryDump(options, 0, getOptionsLength()));
	}
}
