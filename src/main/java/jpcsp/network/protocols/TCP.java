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

public class TCP {
	// TCP packet structure, see https://en.wikipedia.org/wiki/Transmission_Control_Protocol
	public int sourcePort;
	public int destinationPort;
	public int sequenceNumber;
	public int acknowledgmentNumber;
	public int dataOffset;
	public int reserved;
	public boolean flagNS;  // ECN-nonce concealment protection
	public boolean flagCWR; // Congestion Window Reduced
	public boolean flagECE; // ECN-Echo has a dual role, depending on the value of the SYN flag
	public boolean flagURG; // indicates that the Urgent pointer field is significant
	public boolean flagACK; // indicates that the Acknowledgment field is significant
	public boolean flagPSH; // Push function
	public boolean flagRST; // Reset the connection
	public boolean flagSYN; // Synchronize sequence numbers
	public boolean flagFIN; // Last package from sender
	public int windowSize;
	public int checksum;
	public int urgentPointer;
	public byte[] options;
	public byte[] data;

	public TCP() {
		dataOffset = 5;
		windowSize = 0x4000;
	}

	public TCP(TCP tcp) {
		sourcePort = tcp.sourcePort;
		destinationPort = tcp.destinationPort;
		sequenceNumber = tcp.sequenceNumber;
		acknowledgmentNumber = tcp.acknowledgmentNumber;
		dataOffset = tcp.dataOffset;
		reserved = tcp.reserved;
		flagNS = tcp.flagNS;
		flagCWR = tcp.flagCWR;
		flagECE = tcp.flagECE;
		flagURG = tcp.flagURG;
		flagACK = tcp.flagACK;
		flagPSH = tcp.flagPSH;
		flagRST = tcp.flagRST;
		flagSYN = tcp.flagSYN;
		flagFIN = tcp.flagFIN;
		windowSize = tcp.windowSize;
		checksum = tcp.checksum;
		urgentPointer = tcp.urgentPointer;
		options = tcp.options;
		data = tcp.data;
	}

	public void swapSourceAndDestination() {
		int port = sourcePort;
		sourcePort = destinationPort;
		destinationPort = port;
	}

	public void computeChecksum(IPv4 ipv4) throws EOFException {
		// Computes the checksum with 0 at the checksum field
		checksum = 0;

		// The checksum also covers a 12 bytes pseudo header
		NetPacket checksumPacket = new NetPacket(12 + sizeOf());
		// Pseudo header:
		// - source IP address (4 bytes)
		// - destination IP address (4 bytes)
		// - 0 (1 byte)
		// - protocol (1 byte)
		// - TCP length (2 bytes)
		checksumPacket.writeBytes(ipv4.sourceIPAddress);
		checksumPacket.writeBytes(ipv4.destinationIPAddress);
		checksumPacket.write8(0);
		checksumPacket.write8(ipv4.protocol);
		checksumPacket.write16(sizeOf());
		write(checksumPacket);
		checksum = computeInternetChecksum(checksumPacket.getBuffer(), 0, checksumPacket.getOffset());
	}

	private int getOptionsLength() {
		return Math.max((dataOffset - 5) * 4, 0);
	}

	public void read(NetPacket packet) throws EOFException {
		sourcePort = packet.read16();
		destinationPort = packet.read16();
		sequenceNumber = packet.read32();
		acknowledgmentNumber = packet.read32();
		dataOffset = packet.readBits(4);
		reserved = packet.readBits(3);
		flagNS = packet.readBoolean();
		flagCWR = packet.readBoolean();
		flagECE = packet.readBoolean();
		flagURG = packet.readBoolean();
		flagACK = packet.readBoolean();
		flagPSH = packet.readBoolean();
		flagRST = packet.readBoolean();
		flagSYN = packet.readBoolean();
		flagFIN = packet.readBoolean();
		windowSize = packet.read16();
		checksum = packet.read16();
		urgentPointer = packet.read16();
		options = packet.readBytes(getOptionsLength());
		data = packet.readBytes(packet.getLength());
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.write16(sourcePort);
		packet.write16(destinationPort);
		packet.write32(sequenceNumber);
		packet.write32(acknowledgmentNumber);
		packet.writeBits(dataOffset, 4);
		packet.writeBits(reserved, 3);
		packet.writeBoolean(flagNS);
		packet.writeBoolean(flagCWR);
		packet.writeBoolean(flagECE);
		packet.writeBoolean(flagURG);
		packet.writeBoolean(flagACK);
		packet.writeBoolean(flagPSH);
		packet.writeBoolean(flagRST);
		packet.writeBoolean(flagSYN);
		packet.writeBoolean(flagFIN);
		packet.write16(windowSize);
		packet.write16(checksum);
		packet.write16(urgentPointer);
		packet.writeBytes(options, 0, getOptionsLength());
		packet.writeBytes(data);

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	public int sizeOf() {
		int size = dataOffset * 4;
		if (data != null) {
			size += data.length;
		}

		return size;
	}

	private void addFlagString(StringBuilder s, String flagName, boolean flagValue) {
		if (flagValue) {
			if (s.length() > 0) {
				s.append("|");
			}
			s.append(flagName);
		}
	}

	private String getFlagsString() {
		StringBuilder s = new StringBuilder();
		addFlagString(s, "NS", flagNS);
		addFlagString(s, "CWR", flagCWR);
		addFlagString(s, "ECE", flagECE);
		addFlagString(s, "URG", flagURG);
		addFlagString(s, "ACK", flagACK);
		addFlagString(s, "PSH", flagPSH);
		addFlagString(s, "RST", flagRST);
		addFlagString(s, "SYN", flagSYN);
		addFlagString(s, "FIN", flagFIN);

		return s.toString();
	}

	@Override
	public String toString() {
		return String.format("sourcePort=0x%X, destinationPort=0x%X, sequenceNumber=0x%X, acknowledgmentNumber=0x%X, dataOffset=0x%X, reserved=0x%X, flags=%s, windowSize=0x%X, checksum=0x%04X, urgentPointer=0x%X, options=%s, data=%s", sourcePort, destinationPort, sequenceNumber, acknowledgmentNumber, dataOffset, reserved, getFlagsString(), windowSize, checksum, urgentPointer, Utilities.getMemoryDump(options, 0, getOptionsLength()), Utilities.getMemoryDump(data));
	}
}
