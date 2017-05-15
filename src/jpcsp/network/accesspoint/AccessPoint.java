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
package jpcsp.network.accesspoint;

import static jpcsp.HLE.modules.sceNetAdhoc.ANY_MAC_ADDRESS;
import static jpcsp.HLE.modules.sceWlan.WLAN_CMD_DATA;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.network.protocols.ARP.ARP_OPERATION_REPLY;
import static jpcsp.network.protocols.ARP.ARP_OPERATION_REQUEST;
import static jpcsp.network.protocols.DHCP.DHCP_BOOT_REPLY;
import static jpcsp.network.protocols.DNS.DNS_RESPONSE_CODE_NAME_ERROR;
import static jpcsp.network.protocols.DNS.DNS_RESPONSE_CODE_NO_ERROR;
import static jpcsp.network.protocols.EtherFrame.ETHER_TYPE_ARP;
import static jpcsp.network.protocols.EtherFrame.ETHER_TYPE_IPv4;
import static jpcsp.network.protocols.ICMP.ICMP_CONTROL_ECHO_REQUEST;
import static jpcsp.network.protocols.IPv4.IPv4_PROTOCOL_ICMP;
import static jpcsp.network.protocols.IPv4.IPv4_PROTOCOL_TCP;
import static jpcsp.network.protocols.IPv4.IPv4_PROTOCOL_UDP;
import static jpcsp.network.protocols.NetPacket.getIpAddressString;
import static jpcsp.network.protocols.UDP.UDP_PORT_DNS;
import static jpcsp.util.Utilities.writeStringNZ;

import java.io.EOFException;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetApctl;
import jpcsp.HLE.modules.sceNetInet;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.network.protocols.ARP;
import jpcsp.network.protocols.DHCP;
import jpcsp.network.protocols.DNS;
import jpcsp.network.protocols.DNS.DNSAnswerRecord;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.network.protocols.ICMP;
import jpcsp.network.protocols.IPv4;
import jpcsp.network.protocols.NetPacket;
import jpcsp.network.protocols.TCP;
import jpcsp.network.protocols.UDP;
import jpcsp.network.protocols.DNS.DNSRecord;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class AccessPoint {
    public static Logger log = Logger.getLogger("accesspoint");
    public static final int HARDWARE_TYPE_ETHERNET = 0x0001;
    public static final int IP_ADDRESS_LENGTH = 4;
    private static final int BUFFER_SIZE = 2000;
    private static AccessPoint instance;
    private int apSocketPort = 30020;
    private pspNetMacAddress apMacAddress;
    private byte[] apIpAddress;
    private byte[] localIpAddress;
	private DatagramSocket apSocket;
	private AccessPointThread apThread;
	private String apSsid;
	private List<TcpConnectionState> tcpConnectionStates;
	private Random random;

	private static class TcpConnectionState {
		public pspNetMacAddress sourceMacAddress;
		public byte[] sourceIPAddress;
		public int sourcePort;
		public int sourceSequenceNumber;
		public pspNetMacAddress destinationMacAddress;
		public byte[] destinationIPAddress;
		public int destinationPort;
		public int destinationSequenceNumber;
		public SocketChannel socketChannel;
		public byte[] pendingWriteData;
		public boolean pendingConnection;

		private void openChannel() throws IOException {
			if (socketChannel == null) {
				socketChannel = SocketChannel.open();
				// Use a non-blocking channel as we are polling for data
				socketChannel.configureBlocking(false);
				// Connect has no timeout
				socketChannel.socket().setSoTimeout(0);
			}
		}

		public void connect() throws IOException {
			openChannel();
			if (!socketChannel.isConnected() && !socketChannel.isConnectionPending()) {
				SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByAddress(destinationIPAddress), destinationPort);
				socketChannel.connect(socketAddress);
			}
		}

		public void close() throws IOException {
			if (socketChannel != null) {
				socketChannel.close();
				socketChannel = null;
			}
		}

		public void write(byte[] buffer) throws IOException {
			if (buffer != null) {
				write(buffer, 0, buffer.length);
			}
		}

		public void write(byte[] buffer, int offset, int length) throws IOException {
			socketChannel.write(ByteBuffer.wrap(buffer, 0, length));
		}

		public byte[] read() throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int length = socketChannel.read(ByteBuffer.wrap(buffer));
			if (length <= 0) {
				return null;
			}

			byte[] readBuffer = new byte[length];
			System.arraycopy(buffer, 0, readBuffer, 0, length);

			return readBuffer;
		}

		public void addPendingWriteData(byte[] data) {
			if (data != null && data.length > 0) {
				pendingWriteData = Utilities.extendArray(pendingWriteData, data);
			}
		}

		@Override
		public String toString() {
			return String.format("source=%s/%s:0x%X(sequenceNumber=0x%X), destination=%s/%s:0x%X(sequenceNumber=0x%X)", sourceMacAddress, getIpAddressString(sourceIPAddress), sourcePort, sourceSequenceNumber, destinationMacAddress, getIpAddressString(destinationIPAddress), destinationPort, destinationSequenceNumber);
		}
	}

	private class AccessPointThread extends Thread {
		private boolean exit = false;

		@Override
		public void run() {
			while (!exit) {
				if (!receiveAccessPointMessage()) {
					if (!exit && !receiveTcpMessages()) {
						Utilities.sleep(10, 0);
					}
				}
			}
		}

		public void exit() {
			exit = true;
		}
	}

	public static AccessPoint getInstance() {
		if (instance == null) {
			instance = new AccessPoint();
		}

		return instance;
	}

	private AccessPoint() {
		// Generate a random MAC address for the Address Point
		apMacAddress = new pspNetMacAddress(pspNetMacAddress.getRandomMacAddress());

		apIpAddress = getIpAddress(sceNetApctl.getGateway());
		localIpAddress = getIpAddress(sceNetApctl.getLocalHostIP());

		tcpConnectionStates = new LinkedList<>();

		random = new Random();

		apThread = new AccessPointThread();
		apThread.setDaemon(true);
		apThread.setName("Access Point Thread");
		apThread.start();

		if (log.isDebugEnabled()) {
			log.debug(String.format("AccessPoint using MAC=%s, IP=%s", apMacAddress, getIpAddressString(apIpAddress)));
		}
	}

	public static void exit() {
		if (instance != null) {
			if (instance.apThread != null) {
				instance.apThread.exit();
				instance.apThread = null;
			}
		}
	}

	public int getPort() {
		return apSocketPort;
	}

	public pspNetMacAddress getMacAddress() {
		return apMacAddress;
	}

	public byte[] getIpAddress() {
		return apIpAddress;
	}

	private byte[] getLocalIpAddress() {
		return localIpAddress;
	}

	private static byte[] getIpAddress(String hostName) {
		try {
			InetAddress inetAddress = InetAddress.getByName(hostName);
			return inetAddress.getAddress();
		} catch (UnknownHostException e) {
			log.error("getIpAddress", e);
		}

		return null;
	}

	private static byte[] getIpAddress(int ipAddressInt) {
		byte[] ipAddress = new byte[IP_ADDRESS_LENGTH];
		ipAddress[0] = (byte) (ipAddressInt >> 24);
		ipAddress[1] = (byte) (ipAddressInt >> 16);
		ipAddress[2] = (byte) (ipAddressInt >> 8);
		ipAddress[3] = (byte) ipAddressInt;

		return ipAddress;
	}

	private boolean createAccessPointSocket() {
    	if (apSocket == null) {
			boolean retry;
			do {
				retry = false;
	    		try {
	    			apSocket = new DatagramSocket(apSocketPort);
		    		// For broadcast
	    			apSocket.setBroadcast(true);
		    		// Non-blocking (timeout = 0 would mean blocking)
	    			apSocket.setSoTimeout(1);
	    		} catch (BindException e) {
	    			if (log.isDebugEnabled()) {
	    				log.debug(String.format("createAccessPointSocket port %d already in use (%s) - retrying with port %d", apSocketPort, e, apSocketPort + 1));
	    			}
	    			// The port is already busy, retrying with another port
	    			apSocketPort++;
	    			retry = true;
				} catch (SocketException e) {
					log.error("createWlanSocket", e);
				}
			} while (retry);
    	}

    	return apSocket != null;
    }

	private boolean receiveAccessPointMessage() {
    	boolean packetReceived = false;

    	if (!createAccessPointSocket()) {
			return packetReceived;
		}

    	byte[] bytes = new byte[10000];
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		try {
			apSocket.receive(packet);
			if (log.isDebugEnabled()) {
				log.debug(String.format("receiveMessage message: %s", Utilities.getMemoryDump(packet.getData(), packet.getOffset(), packet.getLength())));
			}

			packetReceived = true;

			byte[] dataBytes = packet.getData();
			int dataOffset = packet.getOffset();
			int dataLength = packet.getLength();
			NetPacket netPacket = new NetPacket(dataBytes, dataOffset, dataLength);

			processMessage(netPacket);
		} catch (SocketTimeoutException e) {
			// Timeout can be ignored as we are polling
		} catch (IOException e) {
			log.error("receiveMessage", e);
		}

		return packetReceived;
	}

    private int getBroadcastPort() {
    	return sceWlan.getSocketPort();
    }

    private void sendPacket(byte[] buffer, int bufferLength) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sendPacket %s", Utilities.getMemoryDump(buffer, 0, bufferLength)));
    	}

    	try {
			InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(getBroadcastPort());
			if (broadcastAddress != null) {
				for (int i = 0; i < broadcastAddress.length; i++) {
					DatagramPacket packet = new DatagramPacket(buffer, bufferLength, broadcastAddress[i]);
					apSocket.send(packet);
				}
			}
		} catch (UnknownHostException e) {
			log.error("sendPacket", e);
		} catch (IOException e) {
			log.error("sendPacket", e);
		}
	}

    private void sendPacket(NetPacket packet) {
    	int packetLength = packet.getOffset();

    	byte[] buffer = new byte[33 + packetLength];
    	int offset = 0;

    	buffer[offset++] = WLAN_CMD_DATA;

    	writeStringNZ(buffer, offset, 32, apSsid);
    	offset += 32;

    	System.arraycopy(packet.getBuffer(), 0, buffer, offset, packetLength);
    	offset += packetLength;

    	sendPacket(buffer, offset);
    }

    private void processMessage(NetPacket packet) throws EOFException {
		byte cmd = packet.readByte();

		if (cmd != WLAN_CMD_DATA) {
			log.error(String.format("processMessage unknown command 0x%X", cmd));
			return;
		}

		String ssid = packet.readStringNZ(32);
		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessage ssid='%s'", ssid));
		}

		if (apSsid == null) {
			apSsid = ssid;
			if (log.isDebugEnabled()) {
				log.debug(String.format("Using ssid='%s' for the Access Point", apSsid));
			}
		}

		EtherFrame frame = new EtherFrame();
		frame.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessage %s", frame));
		}

		switch (frame.type) {
			case ETHER_TYPE_ARP:
				processMessageARP(packet);
				break;
			case ETHER_TYPE_IPv4: // See https://www.ietf.org/rfc/rfc894.txt
				processMessageDatagram(packet, frame);
				break;
			default:
				log.warn(String.format("Unknow message of type 0x%04X", frame.type));
				break;
		}
	}

	private void processMessageARP(NetPacket packet) throws EOFException {
		ARP arp = new ARP();
		arp.read(packet);

		if (arp.hardwareType != HARDWARE_TYPE_ETHERNET) {
			log.warn(String.format("processMessageARP unknown hardwareType=0x%X", arp.hardwareType));
			return;
		}

		if (arp.protocolType != ETHER_TYPE_IPv4) {
			log.warn(String.format("processMessageARP unknown protocolType=0x%X", arp.protocolType));
			return;
		}

		if (arp.hardwareAddressLength != MAC_ADDRESS_LENGTH) {
			log.warn(String.format("processMessageARP unknown hardwareAddressLength=0x%X", arp.protocolType));
			return;
		}

		if (arp.protocolAddressLength != IP_ADDRESS_LENGTH) {
			log.warn(String.format("processMessageARP unknown protocolAddressLength=0x%X", arp.protocolType));
			return;
		}

		if (arp.operation != ARP_OPERATION_REQUEST && arp.operation != ARP_OPERATION_REPLY) {
			log.warn(String.format("processMessageARP unknown operation=0x%X", arp.operation));
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageARP %s", arp));
		}

		if (arp.targetHardwareAddress.isEmptyMacAddress()) {
			// A gratuitous ARP message has been received.
			// It is used to announce a new IP address.
			// Send back a gratuitous ARP message to announce ourself.
			sendGratuitousARP();
		}
	}

	private void sendGratuitousARP() throws EOFException {
		EtherFrame frame = new EtherFrame();
		frame.dstMac = new pspNetMacAddress(ANY_MAC_ADDRESS);
		frame.srcMac = getMacAddress();
		frame.type = ETHER_TYPE_ARP;

		ARP arp = new ARP();
		arp.hardwareType = HARDWARE_TYPE_ETHERNET;
		arp.protocolType = ETHER_TYPE_IPv4;
		arp.hardwareAddressLength = MAC_ADDRESS_LENGTH;
		arp.protocolAddressLength = IP_ADDRESS_LENGTH;
		arp.operation = ARP_OPERATION_REQUEST;
		arp.senderHardwareAddress = getMacAddress();
		arp.senderProtocolAddress = getIpAddress();
		// Set the target hardware address to 00:00:00:00:00:00
		arp.targetHardwareAddress = new pspNetMacAddress();
		arp.targetProtocolAddress = getIpAddress();

		NetPacket packet = new NetPacket(EtherFrame.sizeOf() + arp.sizeOf());
		frame.write(packet);
		arp.write(packet);

		sendPacket(packet);
	}

	private void processMessageDatagram(NetPacket packet, EtherFrame frame) throws EOFException {
		IPv4 ipv4 = new IPv4();
		ipv4.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageDatagram IPv4 %s", ipv4));
		}

		switch (ipv4.protocol) {
			case IPv4_PROTOCOL_ICMP:
				processMessageDatagramICMP(packet, frame, ipv4);
				break;
			case IPv4_PROTOCOL_TCP:
				processMessageTCP(packet, frame, ipv4);
				break;
			case IPv4_PROTOCOL_UDP:
				processMessageUDP(packet, frame, ipv4);
				break;
			default:
				log.warn(String.format("processMessageDatagram unknown protocol %d", ipv4.protocol));
				break;
		}
	}

	private void processMessageUDP(NetPacket packet, EtherFrame frame, IPv4 ipv4) throws EOFException {
		UDP udp = new UDP();
		udp.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageUDP %s", udp));
		}

		switch (udp.destinationPort) {
			case UDP_PORT_DNS:
				processMessageDNS(packet, frame, ipv4, udp);
				break;
			case UDP.UDP_PORT_DHCP_SERVER:
				processMessageDHCP(packet, frame, ipv4, udp);
				break;
			default:
				log.warn(String.format("processMessageUDP unknown destination port 0x%X", udp.destinationPort));
				break;
		}
	}

	private void processMessageDNS(NetPacket packet, EtherFrame frame, IPv4 ipv4, UDP udp) throws EOFException {
		DNS dns = new DNS();
		dns.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageDNS %s", dns));
		}

		if (!dns.isResponseFlag && dns.questionCount == 1) {
			DNSRecord question = dns.questions[0];
			String hostName = question.recordName;

			DNS answerDns = new DNS(dns);
			try {
				InetAddress inetAddress = InetAddress.getByName(hostName);
				if (log.isDebugEnabled()) {
					log.debug(String.format("DNS response '%s'=%s", hostName, inetAddress));
				}

				DNSAnswerRecord answer = new DNSAnswerRecord();
				answer.recordName = hostName;
				answer.recordClass = question.recordClass;
				answer.recordType = question.recordType;
				answer.data = inetAddress.getAddress();
				answer.dataLength = answer.data.length;
				answerDns.responseCode = DNS_RESPONSE_CODE_NO_ERROR;
				answerDns.answerRecordCount = 1;
				answerDns.answerRecords = new DNSAnswerRecord[] { answer };
			} catch (UnknownHostException e) {
				answerDns.responseCode = DNS_RESPONSE_CODE_NAME_ERROR;
				if (log.isDebugEnabled()) {
					log.debug(String.format("processMessageDNS unknown host '%s'(%s)", hostName, e.toString()));
				}
			}

			answerDns.isResponseFlag = true;

			EtherFrame answerFrame = new EtherFrame(frame);
			answerFrame.swapSourceAndDestination();

			IPv4 answerIPv4 = new IPv4(ipv4);
			answerIPv4.swapSourceAndDestination();
			answerIPv4.timeToLive--; // When a packet arrives at a router, the router decreases the TTL field.

			UDP answerUdp = new UDP(udp);
			answerUdp.swapSourceAndDestination();

			// Update lengths and checksums
			answerUdp.length = answerUdp.sizeOf() + answerDns.sizeOf();
			answerUdp.computeChecksum();
			answerIPv4.totalLength = answerIPv4.sizeOf() + answerUdp.length;
			answerIPv4.computeChecksum();

			// Write the different headers in sequence
			NetPacket answerPacket = new NetPacket(BUFFER_SIZE);
			answerFrame.write(answerPacket);
			answerIPv4.write(answerPacket);
			answerUdp.write(answerPacket);
			answerDns.write(answerPacket);

			sendPacket(answerPacket);
		}
	}

	private void processMessageDatagramICMP(NetPacket packet, EtherFrame frame, IPv4 ipv4) throws EOFException {
		ICMP icmp = new ICMP();
		icmp.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageDatagramICMP %s", icmp));
		}

		switch (icmp.type) {
			case ICMP_CONTROL_ECHO_REQUEST:
				sendICMPEchoResponse(packet, frame, ipv4, icmp);
				break;
			default:
				log.warn(String.format("processMessageDatagramICMP unknown type=0x%X, code=0x%X", icmp.type, icmp.code));
				break;
		}
	}

	private void sendICMPEchoResponse(NetPacket packet, EtherFrame frame, IPv4 ipv4, ICMP icmp) throws EOFException {
		boolean reachable = false;
		try {
			InetAddress inetAddress = InetAddress.getByAddress(ipv4.destinationIPAddress);
			// Timeout after 1 second
			reachable = inetAddress.isReachable(null, ipv4.timeToLive, 1000);
		} catch (UnknownHostException e) {
		} catch (IOException e) {
		}

		if (reachable) {
			// See https://en.wikipedia.org/wiki/Ping_(networking_utility)
			EtherFrame answerFrame = new EtherFrame(frame);
			answerFrame.swapSourceAndDestination();

			IPv4 answerIPv4 = new IPv4(ipv4);
			answerIPv4.swapSourceAndDestination();
			answerIPv4.timeToLive--; // When a packet arrives at a router, the router decreases the TTL field.

			ICMP answerIcmp = new ICMP(icmp);
			answerIcmp.type = ICMP.ICMP_CONTROL_ECHO_REPLY;
			answerIcmp.computeChecksum();

			answerIPv4.totalLength = answerIPv4.sizeOf() + answerIcmp.sizeOf();
			answerIPv4.computeChecksum();

			// Write the different headers in sequence
			NetPacket answerPacket = new NetPacket(BUFFER_SIZE);
			answerFrame.write(answerPacket);
			answerIPv4.write(answerPacket);
			answerIcmp.write(answerPacket);

			sendPacket(answerPacket);
		}
	}

	private TcpConnectionState getTcpConnectionState(IPv4 ipv4, TCP tcp) {
		for (TcpConnectionState tcpConnectionState : tcpConnectionStates) {
			if (   tcp.sourcePort == tcpConnectionState.sourcePort
			    && tcp.destinationPort == tcpConnectionState.destinationPort 
			    && Arrays.equals(ipv4.sourceIPAddress, tcpConnectionState.sourceIPAddress)
			    && Arrays.equals(ipv4.destinationIPAddress, tcpConnectionState.destinationIPAddress)
			    ) {
				return tcpConnectionState;
			}
		}

		// Not found
		return null;
	}

	private void processMessageTCP(NetPacket packet, EtherFrame frame, IPv4 ipv4) throws EOFException {
		TCP tcp = new TCP();
		tcp.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageTCP %s", tcp));
		}

		TcpConnectionState tcpConnectionState = getTcpConnectionState(ipv4, tcp);
		if (tcp.flagSYN) {
			if (tcpConnectionState != null) {
				if (!tcpConnectionState.pendingConnection) {
					log.error(String.format("processMessageTCP SYN received but connection already exists: %s", tcpConnectionState));
					return;
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("processMessageTCP SYN received for a connection still pending (%s), retrying the connection", tcpConnectionState));
				}

				try {
					tcpConnectionState.close();
				} catch (IOException e) {
					if (log.isDebugEnabled()) {
						log.debug("error while closing connection", e);
					}
				}
				tcpConnectionStates.remove(tcpConnectionState);
			}

			tcpConnectionState = new TcpConnectionState();
			tcpConnectionState.sourceMacAddress = frame.srcMac;
			tcpConnectionState.destinationMacAddress = frame.dstMac;
			tcpConnectionState.sourceIPAddress = ipv4.sourceIPAddress;
			tcpConnectionState.destinationIPAddress = ipv4.destinationIPAddress;
			tcpConnectionState.sourcePort = tcp.sourcePort;
			tcpConnectionState.destinationPort = tcp.destinationPort;
			tcpConnectionState.sourceSequenceNumber = tcp.sequenceNumber + tcp.data.length;
			tcpConnectionState.destinationSequenceNumber = random.nextInt();
			tcpConnectionState.pendingConnection = true;
			tcpConnectionStates.add(tcpConnectionState);
		} else if (tcp.flagACK) {
			if (tcpConnectionState == null) {
				// Acknowledge to an unknown connection, ignore
				if (log.isDebugEnabled()) {
					log.debug(String.format("processMessageTCP ACK received for unknown connection: %s", tcp));
				}
				return;
			}

			try {
				if (tcp.flagFIN) {
					tcpConnectionState.sourceSequenceNumber += tcp.data.length;
					tcpConnectionState.sourceSequenceNumber++;
					sendAcknowledgeTCP(tcpConnectionState, false);
				} else if (tcp.flagPSH) {
					// Acknowledge the reception of the data
					tcpConnectionState.sourceSequenceNumber += tcp.data.length;
					sendAcknowledgeTCP(tcpConnectionState, false);

					// Queue the received data for the destination
					tcpConnectionState.addPendingWriteData(tcp.data);
				}
			} catch (IOException e) {
				log.error("processMessageTCP", e);
			}
		}
	}

	private void sendAcknowledgeTCP(TcpConnectionState tcpConnectionState, boolean flagSYN) throws EOFException {
		EtherFrame answerFrame = new EtherFrame();
		answerFrame.srcMac = tcpConnectionState.destinationMacAddress;
		answerFrame.dstMac = tcpConnectionState.sourceMacAddress;
		answerFrame.type = ETHER_TYPE_IPv4;

		IPv4 answerIPv4 = new IPv4();
		answerIPv4.protocol = IPv4_PROTOCOL_TCP;
		answerIPv4.sourceIPAddress = tcpConnectionState.destinationIPAddress;
		answerIPv4.destinationIPAddress = tcpConnectionState.sourceIPAddress;

		TCP answerTcp = new TCP();
		answerTcp.sourcePort = tcpConnectionState.destinationPort;
		answerTcp.destinationPort = tcpConnectionState.sourcePort;
		answerTcp.sequenceNumber = tcpConnectionState.destinationSequenceNumber;
		answerTcp.acknowledgmentNumber = tcpConnectionState.sourceSequenceNumber;
		answerTcp.flagACK = true;
		answerTcp.flagSYN = flagSYN;

		// Update lengths and checksums
		answerTcp.computeChecksum(answerIPv4);
		answerIPv4.totalLength = answerIPv4.sizeOf() + answerTcp.sizeOf();
		answerIPv4.computeChecksum();

		// Write the different headers in sequence
		NetPacket answerPacket = new NetPacket(BUFFER_SIZE);
		answerFrame.write(answerPacket);
		answerIPv4.write(answerPacket);
		answerTcp.write(answerPacket);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sendAcknowledgeTCP frame=%s", answerFrame));
			log.debug(String.format("sendAcknowledgeTCP IPv4=%s", answerIPv4));
			log.debug(String.format("sendAcknowledgeTCP TCP=%s", answerTcp));
		}

		sendPacket(answerPacket);
	}

	private void sendTcpData(TcpConnectionState tcpConnectionState, byte[] data) throws EOFException {
		EtherFrame answerFrame = new EtherFrame();
		answerFrame.srcMac = tcpConnectionState.destinationMacAddress;
		answerFrame.dstMac = tcpConnectionState.sourceMacAddress;
		answerFrame.type = ETHER_TYPE_IPv4;

		IPv4 answerIPv4 = new IPv4();
		answerIPv4.protocol = IPv4_PROTOCOL_TCP;
		answerIPv4.sourceIPAddress = tcpConnectionState.destinationIPAddress;
		answerIPv4.destinationIPAddress = tcpConnectionState.sourceIPAddress;

		TCP answerTcp = new TCP();
		answerTcp.sourcePort = tcpConnectionState.destinationPort;
		answerTcp.destinationPort = tcpConnectionState.sourcePort;
		answerTcp.sequenceNumber = tcpConnectionState.destinationSequenceNumber;
		answerTcp.acknowledgmentNumber = tcpConnectionState.sourceSequenceNumber;
		answerTcp.flagACK = true;
		answerTcp.flagPSH = true;
		tcpConnectionState.destinationSequenceNumber += data.length;
		answerTcp.data = data;

		// Update lengths and checksums
		answerTcp.computeChecksum(answerIPv4);
		answerIPv4.totalLength = answerIPv4.sizeOf() + answerTcp.sizeOf();
		answerIPv4.computeChecksum();

		// Write the different headers in sequence
		NetPacket answerPacket = new NetPacket(BUFFER_SIZE);
		answerFrame.write(answerPacket);
		answerIPv4.write(answerPacket);
		answerTcp.write(answerPacket);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sendTcpData frame=%s", answerFrame));
			log.debug(String.format("sendTcpData IPv4=%s", answerIPv4));
			log.debug(String.format("sendTcpData TCP=%s", answerTcp));
		}

		sendPacket(answerPacket);
	}

	private boolean receiveTcpMessages() {
		boolean received = false;
		List<TcpConnectionState> tcpConnectionStatesToBeDeleted = new LinkedList<TcpConnectionState>();

		for (TcpConnectionState tcpConnectionState : tcpConnectionStates) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("receiveTcpMessages polling %s", tcpConnectionState));
			}

			if (tcpConnectionState.pendingConnection) {
				try {
					tcpConnectionState.connect();
					SocketChannel socketChannel = tcpConnectionState.socketChannel;
					if (socketChannel != null && socketChannel.finishConnect()) {
						tcpConnectionState.sourceSequenceNumber++;
						// Send SYN-ACK acknowledge
						sendAcknowledgeTCP(tcpConnectionState, true);
						tcpConnectionState.destinationSequenceNumber++;
						tcpConnectionState.pendingConnection = false;
					}
				} catch (IOException e) {
					// connect failed, do not send any TCP SYN-ACK, forget the connection state
					tcpConnectionStatesToBeDeleted.add(tcpConnectionState);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Pending TCP connection %s failed: %s", tcpConnectionState, e.toString()));
					}
				}
			}

			try {
				if (!tcpConnectionState.pendingConnection) {
					// Write any pending data
					byte[] pendingWriteData = tcpConnectionState.pendingWriteData;
					if (pendingWriteData != null) {
						tcpConnectionState.pendingWriteData = null;

						if (log.isDebugEnabled()) {
							log.debug(String.format("receiveTcpMessages sending pending write data: %s", Utilities.getMemoryDump(pendingWriteData)));
						}
						tcpConnectionState.write(pendingWriteData);
					}

					// Receive any available data
					byte[] receivedData = tcpConnectionState.read();
					if (receivedData != null) {
						received = true;
						sendTcpData(tcpConnectionState, receivedData);
					}
				}
			} catch (IOException e) {
				// Ignore exceptions
				log.error("receiveTcpMessages", e);
			}
		}

		tcpConnectionStates.removeAll(tcpConnectionStatesToBeDeleted);

		return received;
	}

	private void sendDHCPReply(EtherFrame frame, IPv4 ipv4, UDP udp, DHCP dhcp, int messageType) throws EOFException {
		// Send back a DHCP offer message
		EtherFrame answerFrame = new EtherFrame(frame);
		answerFrame.swapSourceAndDestination();
		answerFrame.srcMac = getMacAddress();

		IPv4 answerIPv4 = new IPv4(ipv4);
		answerIPv4.sourceIPAddress = getIpAddress();
		answerIPv4.timeToLive--; // When a packet arrives at a router, the router decreases the TTL field.

		UDP answerUdp = new UDP(udp);
		answerUdp.swapSourceAndDestination();

		DHCP answerDhcp = new DHCP(dhcp);
		answerDhcp.opcode = DHCP_BOOT_REPLY;
		answerDhcp.yourIPAddress = getLocalIpAddress();
		answerDhcp.nextServerIPAddress = getIpAddress();

		answerDhcp.clearOptions();
		// The DHCP message type
		answerDhcp.addOption(new DHCP.DHCPOption(DHCP.DHCP_OPTION_MESSAGE_TYPE, (byte) messageType));
		// The subnet mask
		answerDhcp.addOption(new DHCP.DHCPOption(DHCP.DHCP_OPTION_SUBNET_MASK, getIpAddress(sceNetApctl.getSubnetMaskInt())));
		// The only router is myself
		answerDhcp.addOption(new DHCP.DHCPOption(DHCP.DHCP_OPTION_ROUTER, getIpAddress()));
		// The IP address lease time is forever
		answerDhcp.addOption(new DHCP.DHCPOption(DHCP.DHCP_OPTION_IP_ADDRESS_LEASE_TIME, Integer.MAX_VALUE));
		// The DHCP server identification is myself
		answerDhcp.addOption(new DHCP.DHCPOption(DHCP.DHCP_OPTION_SERVER_IDENTIFIER, getIpAddress()));
		// The only DNS server is myself
		answerDhcp.addOption(new DHCP.DHCPOption(DHCP.DHCP_OPTION_DNS, getIpAddress()));
		// The broadcast address
		answerDhcp.addOption(new DHCP.DHCPOption(DHCP.DHCP_OPTION_BROADCAST_ADDRESS, DHCP.broadcastIPAddress));

		// Update lengths and checksums
		answerUdp.length = answerUdp.sizeOf() + answerDhcp.sizeOf();
		answerUdp.computeChecksum();
		answerIPv4.totalLength = answerIPv4.sizeOf() + answerUdp.length;
		answerIPv4.computeChecksum();

		// Write the different headers in sequence
		NetPacket answerPacket = new NetPacket(BUFFER_SIZE);
		answerFrame.write(answerPacket);
		answerIPv4.write(answerPacket);
		answerUdp.write(answerPacket);
		answerDhcp.write(answerPacket);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sendDHCPReply frame=%s", answerFrame));
			log.debug(String.format("sendDHCPReply IPv4=%s", answerIPv4));
			log.debug(String.format("sendDHCPReply UDP=%s", answerUdp));
			log.debug(String.format("sendDHCPReply messageType=%d, DHCP=%s", messageType, answerDhcp));
		}

		sendPacket(answerPacket);
	}

	private void processMessageDHCP(NetPacket packet, EtherFrame frame, IPv4 ipv4, UDP udp) throws EOFException {
		DHCP dhcp = new DHCP();
		dhcp.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageDHCP %s", dhcp));
		}

		if (dhcp.isDiscovery(udp, ipv4)) {
			// Send back a DHCP offset message
			sendDHCPReply(frame, ipv4, udp, dhcp, DHCP.DHCP_OPTION_MESSAGE_TYPE_DHCPOFFER);
		} else if (dhcp.isRequest(udp, ipv4, getLocalIpAddress())) {
			// Send back a DHCP acknowledgment message
			sendDHCPReply(frame, ipv4, udp, dhcp, DHCP.DHCP_OPTION_MESSAGE_TYPE_DHCPACK);
		} else {
			log.warn(String.format("Unknown DHCP request %s", dhcp));
		}
	}
}
