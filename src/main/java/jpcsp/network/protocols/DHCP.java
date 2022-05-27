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

import static jpcsp.HLE.modules.sceNet.convertMacAddressToString;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.network.accesspoint.AccessPoint.IP_ADDRESS_LENGTH;
import static jpcsp.network.protocols.NetPacket.getIpAddressString;
import static jpcsp.network.protocols.UDP.UDP_PORT_DHCP_CLIENT;
import static jpcsp.network.protocols.UDP.UDP_PORT_DHCP_SERVER;

import java.io.EOFException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jpcsp.util.Utilities;

public class DHCP {
	// See https://en.wikipedia.org/wiki/Dynamic_Host_Configuration_Protocol
	public static final byte[] nullIPAddress = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
	public static final byte[] broadcastIPAddress = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	public static final int DHCP_BOOT_REQUEST = 1;
	public static final int DHCP_BOOT_REPLY = 2;
	// See DHCP Options in https://tools.ietf.org/html/rfc1533
	public static final int DHCP_OPTION_MAGIC_COOKIE = 0x63825363;
	public static final int DHCP_OPTION_PAD = 0;
	public static final int DHCP_OPTION_SUBNET_MASK = 1;
	public static final int DHCP_OPTION_ROUTER = 3;
	public static final int DHCP_OPTION_DNS = 6;
	public static final int DHCP_OPTION_DOMAIN_NAME = 15;
	public static final int DHCP_OPTION_BROADCAST_ADDRESS = 28;
	public static final int DHCP_OPTION_REQUESTED_IP_ADDRESS = 50;
	public static final int DHCP_OPTION_IP_ADDRESS_LEASE_TIME = 51;
	public static final int DHCP_OPTION_MESSAGE_TYPE = 53;
	public static final int DHCP_OPTION_SERVER_IDENTIFIER = 54;
	public static final int DHCP_OPTION_PARAMETER_REQUEST = 55;
	public static final int DHCP_OPTION_MESSAGE = 56;
	public static final int DHCP_OPTION_MAXIMUM_DHCP_MESSAGE = 57;
	public static final int DHCP_OPTION_CLIENT_IDENTIFIER = 61;
	public static final int DHCP_OPTION_END = 255;
	public static final String[] DHCP_OPTION_NAMES = new String[256];
	// See DHCP Message type in https://tools.ietf.org/html/rfc1533, chapter "9.4. DHCP Message Type"
	public static final int DHCP_OPTION_MESSAGE_TYPE_DHCPDISCOVER = 1;
	public static final int DHCP_OPTION_MESSAGE_TYPE_DHCPOFFER = 2;
	public static final int DHCP_OPTION_MESSAGE_TYPE_DHCPREQUEST = 3;
	public static final int DHCP_OPTION_MESSAGE_TYPE_DHCPDECLINE = 4;
	public static final int DHCP_OPTION_MESSAGE_TYPE_DHCPACK = 5;
	public static final int DHCP_OPTION_MESSAGE_TYPE_DHCPNAK = 6;
	public static final int DHCP_OPTION_MESSAGE_TYPE_DHCPRELEASE = 7;
	public int opcode;
	public int hardwareAddressType;
	public int hardwareAddressLength;
	public int hops;
	public int transactionID;
	public int seconds;
	public boolean flagBroadcast;
	public int flagsZero;
	public byte[] clientIPAddress;
	public byte[] yourIPAddress;
	public byte[] nextServerIPAddress;
	public byte[] relayAgentIPAddress;
	public byte[] clientHardwareAddress;
	public String serverHostName;
	public String bootFileName;
	public List<DHCPOption> options;

	static {
		DHCP_OPTION_NAMES[DHCP_OPTION_PAD] = "PAD";
		DHCP_OPTION_NAMES[DHCP_OPTION_SUBNET_MASK] = "SUBNET_MASK";
		DHCP_OPTION_NAMES[DHCP_OPTION_ROUTER] = "ROUTER";
		DHCP_OPTION_NAMES[DHCP_OPTION_DNS] = "DNS";
		DHCP_OPTION_NAMES[DHCP_OPTION_DOMAIN_NAME] = "DOMAIN_NAME";
		DHCP_OPTION_NAMES[DHCP_OPTION_BROADCAST_ADDRESS] = "BROADCAST_ADDRESS";
		DHCP_OPTION_NAMES[DHCP_OPTION_REQUESTED_IP_ADDRESS] = "REQUESTED_IP_ADDRESS";
		DHCP_OPTION_NAMES[DHCP_OPTION_IP_ADDRESS_LEASE_TIME] = "IP_ADDRESS_LEASE_TIME";
		DHCP_OPTION_NAMES[DHCP_OPTION_MESSAGE_TYPE] = "MESSAGE_TYPE";
		DHCP_OPTION_NAMES[DHCP_OPTION_SERVER_IDENTIFIER] = "SERVER_IDENTIFIER";
		DHCP_OPTION_NAMES[DHCP_OPTION_PARAMETER_REQUEST] = "PARAMETER_REQUEST";
		DHCP_OPTION_NAMES[DHCP_OPTION_MESSAGE] = "MESSAGE";
		DHCP_OPTION_NAMES[DHCP_OPTION_MAXIMUM_DHCP_MESSAGE] = "MAXIMUM_DHCP_MESSAGE";
		DHCP_OPTION_NAMES[DHCP_OPTION_CLIENT_IDENTIFIER] = "CLIENT_IDENTIFIER";
		DHCP_OPTION_NAMES[DHCP_OPTION_END] = "END";
	}

	public static class DHCPOption {
		int tag;
		int length;
		byte[] data;

		public DHCPOption() {
			tag = DHCP_OPTION_END;
		}

		public DHCPOption(int tag) {
			this.tag = tag;
		}

		public DHCPOption(int tag, byte data) {
			this.tag = tag;
			this.length = 1;
			this.data = new byte[] { data };
		}

		public DHCPOption(int tag, int data) {
			this.tag = tag;
			this.length = 4;
			this.data = new byte[4];
			this.data[0] = (byte) (data >> 24);
			this.data[1] = (byte) (data >> 16);
			this.data[2] = (byte) (data >> 8);
			this.data[3] = (byte) data;
		}

		public DHCPOption(int tag, byte[] data) {
			this.tag = tag;
			this.length = data == null ? 0 : data.length;
			this.data = data;
		}

		public void read(NetPacket packet) throws EOFException {
			tag = packet.read8();
			if (!isZeroLengthTag()) {
				length = packet.read8();
				data = packet.readBytes(length);
			}
		}

		private boolean isZeroLengthTag() {
			// PAD and END tags have no length.
			return tag == DHCP_OPTION_PAD || tag == DHCP_OPTION_END;
		}

		public int sizeOf() {
			if (isZeroLengthTag()) {
				return 1;
			}

			return 2 + length;
		}

		public NetPacket write(NetPacket packet) throws EOFException {
			packet.write8(tag);
			if (!isZeroLengthTag()) {
				packet.write8(length);
				packet.writeBytes(data, 0, length);
			}

			return packet;
		}

		public int getDataAsInt() {
			int value = 0;

			switch (length) {
				case 1:
					value = data[0] & 0xFF;
					break;
				case 2:
					value  = (data[0] & 0xFF) << 8;
					value |=  data[1] & 0xFF;
					break;
				case 4:
					value  = (data[0] & 0xFF) << 24;
					value |= (data[1] & 0xFF) << 16;
					value |= (data[2] & 0xFF) << 8;
					value |=  data[3] & 0xFF;
					break;
			}

			return value;
		}

		private String getTagName() {
			if (tag >= 0 && tag < DHCP_OPTION_NAMES.length) {
				if (DHCP_OPTION_NAMES[tag] != null) {
					return DHCP_OPTION_NAMES[tag];
				}
			}

			return String.format("tag=%d", tag);
		}

		@Override
		public String toString() {
			if (isZeroLengthTag()) {
				return getTagName();
			}

			String dataString;
			if (length == 1 || length == 2 || length == 4) {
				dataString = String.format("0x%X", getDataAsInt());
			} else {
				dataString = Utilities.getMemoryDump(data, 0, length);
			}
			return String.format("%s(length=0x%X, data=%s)", getTagName(), length, dataString);
		}
	}

	public DHCP() {
		options = new LinkedList<DHCP.DHCPOption>();
	}

	public DHCP(DHCP dhcp) {
		opcode = dhcp.opcode;
		hardwareAddressType = dhcp.hardwareAddressType;
		hardwareAddressLength = dhcp.hardwareAddressLength;
		hops = dhcp.hops;
		transactionID = dhcp.transactionID;
		seconds = dhcp.seconds;
		flagBroadcast = dhcp.flagBroadcast;
		flagsZero = dhcp.flagsZero;
		clientIPAddress = dhcp.clientIPAddress;
		yourIPAddress = dhcp.yourIPAddress;
		nextServerIPAddress = dhcp.nextServerIPAddress;
		relayAgentIPAddress = dhcp.relayAgentIPAddress;
		clientHardwareAddress = dhcp.clientHardwareAddress;
		serverHostName = dhcp.serverHostName;
		bootFileName = dhcp.bootFileName;
		options = dhcp.options;
	}

	public void read(NetPacket packet) throws EOFException {
		opcode = packet.read8();
		hardwareAddressType = packet.read8();
		hardwareAddressLength = packet.read8();
		hops = packet.read8();
		transactionID = packet.read32();
		seconds = packet.read16();
		flagBroadcast = packet.readBoolean();
		flagsZero = packet.readBits(15);
		clientIPAddress = packet.readBytes(IP_ADDRESS_LENGTH);
		yourIPAddress = packet.readBytes(IP_ADDRESS_LENGTH);
		nextServerIPAddress = packet.readBytes(IP_ADDRESS_LENGTH);
		relayAgentIPAddress = packet.readBytes(IP_ADDRESS_LENGTH);
		clientHardwareAddress = packet.readBytes(16);
		serverHostName = packet.readStringNZ(64);
		bootFileName = packet.readStringNZ(128);

		int optionsLength = 312;
		int magicCookie = packet.read32();
		optionsLength -= 4;
		if (magicCookie == DHCP_OPTION_MAGIC_COOKIE) {
			while (optionsLength > 0) {
				DHCPOption option = new DHCPOption();
				option.read(packet);
				options.add(option);
				optionsLength -= option.sizeOf();

				// END tag marks the end of the options
				if (option.tag == DHCP_OPTION_END) {
					break;
				}
			}
		} else {
			packet.skip8(optionsLength);
		}
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.write8(opcode);
		packet.write8(hardwareAddressType);
		packet.write8(hardwareAddressLength);
		packet.write8(hops);
		packet.write32(transactionID);
		packet.write16(seconds);
		packet.writeBoolean(flagBroadcast);
		packet.writeBits(flagsZero, 15);
		packet.writeBytes(clientIPAddress, 0, IP_ADDRESS_LENGTH);
		packet.writeBytes(yourIPAddress, 0, IP_ADDRESS_LENGTH);
		packet.writeBytes(nextServerIPAddress, 0, IP_ADDRESS_LENGTH);
		packet.writeBytes(relayAgentIPAddress, 0, IP_ADDRESS_LENGTH);
		packet.writeBytes(clientHardwareAddress, 0, 16);
		packet.writeStringNZ(serverHostName, 64);
		packet.writeStringNZ(bootFileName, 128);

		int optionsLength = 312;
		packet.write32(DHCP_OPTION_MAGIC_COOKIE);
		optionsLength -= 4;
		for (DHCPOption option : options) {
			if (optionsLength < option.sizeOf()) {
				break;
			}
			option.write(packet);
		}

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	public int sizeOf() {
		return 548;
	}

	public boolean isMessageOfType(UDP udp, IPv4 ipv4, int messageType, boolean checkIp) {
		if (opcode != DHCP_BOOT_REQUEST) {
			return false;
		}
		if (udp.sourcePort != UDP_PORT_DHCP_CLIENT || udp.destinationPort != UDP_PORT_DHCP_SERVER) {
			return false;
		}

		if (checkIp) {
			if (!Arrays.equals(ipv4.sourceIPAddress, nullIPAddress)) {
				return false;
			}
			if (!Arrays.equals(ipv4.destinationIPAddress, broadcastIPAddress)) {
				return false;
			}
		}

		DHCPOption option = getOptionByTag(DHCP_OPTION_MESSAGE_TYPE);
		if (option == null || option.length != 1) {
			return false;
		}
		int optionMessageType = option.getDataAsInt();
		if (optionMessageType != messageType) {
			return false;
		}

		return true;
	}

	public boolean isDiscovery(UDP udp, IPv4 ipv4) {
		return isMessageOfType(udp, ipv4, DHCP_OPTION_MESSAGE_TYPE_DHCPDISCOVER, true);
	}

	public boolean isRequest(UDP udp, IPv4 ipv4, byte[] requestedIpAddress) {
		if (!isMessageOfType(udp, ipv4, DHCP_OPTION_MESSAGE_TYPE_DHCPREQUEST, true)) {
			return false;
		}

		// Verify that the requested IP address is matching
		// the one specified in the options.
		DHCPOption requestedIpAddressOption = getOptionByTag(DHCP_OPTION_REQUESTED_IP_ADDRESS);
		if (requestedIpAddressOption == null || requestedIpAddressOption.length != 4) {
			return false;
		}
		if (!Arrays.equals(requestedIpAddress, requestedIpAddressOption.data)) {
			return false;
		}

		return true;
	}

	public boolean isRelease(UDP udp, IPv4 ipv4, byte[] releasedIpAddress) {
		if (!isMessageOfType(udp, ipv4, DHCP_OPTION_MESSAGE_TYPE_DHCPRELEASE, false)) {
			return false;
		}

		// Verify that the released IP address is matching
		// the one specified in the options.
		if (!Arrays.equals(releasedIpAddress, ipv4.sourceIPAddress)) {
			return false;
		}

		return true;
	}

	public void addOption(DHCPOption option) {
		options.add(option);
	}

	public void clearOptions() {
		options.clear();
	}

	public DHCPOption getOptionByTag(int tag) {
		for (DHCPOption option : options) {
			if (option.tag == tag) {
				return option;
			}
		}

		return null;
	}

	private String optionsToString() {
		StringBuilder s = new StringBuilder();

		for (DHCPOption option : options) {
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(option.toString());
		}

		return s.toString();
	}

	private String clientHardwareAddressToString() {
		if (hardwareAddressLength == MAC_ADDRESS_LENGTH) {
			return convertMacAddressToString(clientHardwareAddress);
		}

		return Utilities.getMemoryDump(clientHardwareAddress, 0, hardwareAddressLength);
	}

	@Override
	public String toString() {
		return String.format("opcode=0x%X, hardwareAddressType=0x%X, hardwareAddressLength=0x%X, hops=0x%X, transactionID=0x%08X, seconds=0x%X, flagBroadcast=%b, flags=0x%04X, clientIPAddress=%s, yourIPAddress=%s, nextServerIPAddress=%s, relayAgentIPAddress=%s, clientHardwareAddress=%s, serverHostName='%s', bootFileName='%s', options=%s", opcode, hardwareAddressType, hardwareAddressLength, hops, transactionID, seconds, flagBroadcast, flagsZero, getIpAddressString(clientIPAddress), getIpAddressString(yourIPAddress), getIpAddressString(nextServerIPAddress), getIpAddressString(relayAgentIPAddress), clientHardwareAddressToString(), serverHostName, bootFileName, optionsToString());
	}
}
