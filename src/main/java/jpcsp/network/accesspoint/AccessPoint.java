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

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.HLE.modules.sceNetAdhoc.ANY_MAC_ADDRESS;
import static jpcsp.HLE.modules.sceNetInet.internetAddressToBytes;
import static jpcsp.HLE.modules.sceNetInet.internetAddressToString;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.getLocalInetAddress;
import static jpcsp.network.jpcsp.JpcspWlanAdapter.WLAN_CMD_DATA;
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
import java.io.OutputStream;
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
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetApctl;
import jpcsp.network.protocols.ARP;
import jpcsp.network.protocols.DHCP;
import jpcsp.network.protocols.DNS;
import jpcsp.network.protocols.DNS.DNSAnswerRecord;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.network.protocols.ICMP;
import jpcsp.network.protocols.IPv4;
import jpcsp.network.protocols.NetPacket;
import jpcsp.network.protocols.SSDP;
import jpcsp.network.protocols.TCP;
import jpcsp.network.protocols.UDP;
import jpcsp.network.upnp.UPnP;
import jpcsp.remote.HTTPServer;
import jpcsp.remote.IProcessHTTPRequest;
import jpcsp.network.protocols.DNS.DNSRecord;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class AccessPoint implements IProcessHTTPRequest {
    public static Logger log = Logger.getLogger("accesspoint");
    public static final int HARDWARE_TYPE_ETHERNET = 0x0001;
    public static final int IP_ADDRESS_LENGTH = 4;
    private static final int BUFFER_SIZE = 2000;
    private IAccessPointCallback callback;
    private int apSocketPort = 30020;
    private pspNetMacAddress apMacAddress;
    private byte[] apIpAddress;
    private byte[] localIpAddress;
	private DatagramSocket apSocket;
	private AccessPointThread apThread;
	private String apSsid;
	private List<TcpConnection> tcpConnections;
	private List<UdpConnection> udpConnections;
	private Random random;
	private String baseUri;
	private UPnP upnp;

	private static class TcpConnection {
		public pspNetMacAddress sourceMacAddress;
		public byte[] sourceIPAddress;
		public int sourcePort;
		public int sourceSequenceNumber;
		public pspNetMacAddress destinationMacAddress;
		public byte[] destinationIPAddress;
		public byte[] proxyDestinationIPAddress;
		public int destinationPort;
		public int proxyDestinationPort;
		public int destinationSequenceNumber;
		public SocketChannel socketChannel;
		public byte[] pendingWriteData;
		public boolean pendingConnection;

		public TcpConnection(EtherFrame frame, IPv4 ipv4, TCP tcp, Random random) {
			sourceMacAddress = frame.srcMac;
			destinationMacAddress = frame.dstMac;
			sourceIPAddress = ipv4.sourceIPAddress;
			destinationIPAddress = ipv4.destinationIPAddress;
			proxyDestinationIPAddress = ipv4.destinationIPAddress;
			sourcePort = tcp.sourcePort;
			destinationPort = tcp.destinationPort;
			proxyDestinationPort = tcp.destinationPort;
			sourceSequenceNumber = tcp.sequenceNumber + tcp.data.length;
			destinationSequenceNumber = random.nextInt();
			pendingConnection = true;
		}

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
				SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByAddress(proxyDestinationIPAddress), proxyDestinationPort);
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
			if (socketChannel != null) {
				int n = socketChannel.write(ByteBuffer.wrap(buffer, 0, length));
				if (n != length) {
					log.error(String.format("TcpConnection.write could not write 0x%X bytes, only 0x%X bytes written", length, n));
				}
			} else {
				log.error(String.format("TcpConnection.write socketChannel not created"));
			}
		}

		public byte[] read() throws IOException {
			if (socketChannel == null) {
				return null;
			}

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
			return String.format("source=%s/%s:%d(sequenceNumber=0x%X), destination=%s/%s:%d(sequenceNumber=0x%X), proxyDestination=%s:%d", sourceMacAddress, getIpAddressString(sourceIPAddress), sourcePort, sourceSequenceNumber, destinationMacAddress, getIpAddressString(destinationIPAddress), destinationPort, destinationSequenceNumber, getIpAddressString(proxyDestinationIPAddress), proxyDestinationPort);
		}
	}

	private static class UdpConnection {
		private static final long INACTIVITY_MILLIS_AUTO_CLOSE = 5 * 1000; // 5 seconds
		public pspNetMacAddress sourceMacAddress;
		public byte[] sourceIPAddress;
		public int sourcePort;
		public pspNetMacAddress destinationMacAddress;
		public byte[] destinationIPAddress;
		public int destinationPort;
		public DatagramChannel datagramChannel;
		public byte[] pendingWriteData;
		public boolean pendingConnection;
		private long lastUsed;

		private void openChannel() throws IOException {
			if (datagramChannel == null) {
				datagramChannel = DatagramChannel.open();
				// Use a non-blocking channel as we are polling for data
				datagramChannel.configureBlocking(false);
				// Connect has no timeout
				datagramChannel.socket().setSoTimeout(0);
			}
		}

		public void connect() throws IOException {
			openChannel();
			if (!datagramChannel.isConnected()) {
				SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByAddress(destinationIPAddress), destinationPort);
				datagramChannel.connect(socketAddress);

				lastUsed = now();
			}
		}

		public boolean isConnected() {
			return datagramChannel != null && datagramChannel.isConnected();
		}

		public void close() throws IOException {
			if (datagramChannel != null) {
				datagramChannel.close();
				datagramChannel = null;

				lastUsed = 0L;
			}
		}

		private long now() {
			return Emulator.getClock().currentTimeMillis();
		}

		public void closeIfNoLongerUsed() throws IOException {
			if (!isConnected() || pendingWriteData != null) {
				return;
			}

			long inactivityMillis = now() - lastUsed;
			if (inactivityMillis > INACTIVITY_MILLIS_AUTO_CLOSE) {
				close();
			}
		}

		public void write(byte[] buffer) throws IOException {
			if (buffer != null) {
				write(buffer, 0, buffer.length);
			}
		}

		public void write(byte[] buffer, int offset, int length) throws IOException {
			datagramChannel.write(ByteBuffer.wrap(buffer, 0, length));

			lastUsed = now();
		}

		public byte[] read() throws IOException {
			if (datagramChannel == null) {
				return null;
			}

			byte[] buffer = new byte[BUFFER_SIZE];
			int length = datagramChannel.read(ByteBuffer.wrap(buffer));
			if (length <= 0) {
				return null;
			}

			byte[] readBuffer = new byte[length];
			System.arraycopy(buffer, 0, readBuffer, 0, length);

			lastUsed = now();

			return readBuffer;
		}

		public void addPendingWriteData(byte[] data) {
			if (data != null && data.length > 0) {
				pendingWriteData = Utilities.extendArray(pendingWriteData, data);
			}
		}

		@Override
		public String toString() {
			return String.format("source=%s/%s:%d, destination=%s/%s:%d", sourceMacAddress, getIpAddressString(sourceIPAddress), sourcePort, destinationMacAddress, getIpAddressString(destinationIPAddress), destinationPort);
		}
	}

	private class AccessPointThread extends Thread {
		private boolean exit = false;

		@Override
		public void run() {
			setLog4jMDC();

			while (!exit) {
				boolean receivedAccessPointMessage = receiveAccessPointMessage();
				boolean receivedTcpMessage = receiveTcpMessages();
				boolean receivedUdpMessage = receiveUdpMessages();
				if (!exit && !receivedAccessPointMessage && !receivedTcpMessage && !receivedUdpMessage) {
					Utilities.sleep(10, 0);
				}
			}
		}

		public void exit() {
			exit = true;
		}
	}

	public AccessPoint(IAccessPointCallback callback) {
		this.callback = callback;

		// Generate a random MAC address for the Access Point
		apMacAddress = new pspNetMacAddress(pspNetMacAddress.getRandomMacAddress());

		apIpAddress = getIpAddress(sceNetApctl.getGateway());
		localIpAddress = getIpAddress(sceNetApctl.getLocalHostIP());

		tcpConnections = new LinkedList<TcpConnection>();
		udpConnections = new LinkedList<UdpConnection>();

		random = new Random();

		if (!createAccessPointSocket()) {
			log.error(String.format("Cannot create access point socket"));
		}

		baseUri = String.format("/AccessPoint/%d/", getPort());
		HTTPServer.getInstance().register(baseUri, this);

		apThread = new AccessPointThread();
		apThread.setDaemon(true);
		apThread.setName("Access Point Thread");
		apThread.start();

		upnp = new UPnP();
		upnp.discoverInBackground();

		if (log.isDebugEnabled()) {
			log.debug(String.format("AccessPoint using MAC=%s, IP=%s", apMacAddress, getIpAddressString(apIpAddress)));
		}
	}

	public void exit() {
		if (apThread != null) {
			apThread.exit();
			apThread = null;
		}

		if (upnp != null) {
			upnp.stop();
			upnp = null;
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

	private boolean isMyIpAddress(byte[] ipAddress) {
		if (ipAddress.length != apIpAddress.length) {
			return false;
		}

		for (int i = 0; i < apIpAddress.length; i++) {
			if (apIpAddress[i] != ipAddress[i]) {
				return false;
			}
		}

		return true;
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
	    			apSocket = new DatagramSocket(apSocketPort, getLocalInetAddress());
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

    private void sendPacket(NetPacket packet, EtherFrame etherFrame) {
    	int packetLength = packet.getOffset();

    	byte[] buffer = new byte[33 + packetLength];
    	int offset = 0;

    	buffer[offset++] = WLAN_CMD_DATA;

    	writeStringNZ(buffer, offset, 32, apSsid);
    	offset += 32;

    	System.arraycopy(packet.getBuffer(), 0, buffer, offset, packetLength);
    	offset += packetLength;

    	callback.sendPacketFromAccessPoint(buffer, offset, etherFrame);
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
				log.error(String.format("Unknow message of type 0x%04X", frame.type));
				break;
		}
	}

	private void processMessageARP(NetPacket packet) throws EOFException {
		ARP arp = new ARP();
		arp.read(packet);

		if (arp.hardwareType != HARDWARE_TYPE_ETHERNET) {
			log.error(String.format("processMessageARP unknown hardwareType=0x%X", arp.hardwareType));
			return;
		}

		if (arp.protocolType != ETHER_TYPE_IPv4) {
			log.error(String.format("processMessageARP unknown protocolType=0x%X", arp.protocolType));
			return;
		}

		if (arp.hardwareAddressLength != MAC_ADDRESS_LENGTH) {
			log.error(String.format("processMessageARP unknown hardwareAddressLength=0x%X", arp.protocolType));
			return;
		}

		if (arp.protocolAddressLength != IP_ADDRESS_LENGTH) {
			log.error(String.format("processMessageARP unknown protocolAddressLength=0x%X", arp.protocolType));
			return;
		}

		if (arp.operation != ARP_OPERATION_REQUEST && arp.operation != ARP_OPERATION_REPLY) {
			log.error(String.format("processMessageARP unknown operation=0x%X", arp.operation));
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

		sendPacket(packet, frame);
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
				log.error(String.format("processMessageDatagram unknown protocol %d", ipv4.protocol));
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
			case UDP.UDP_PORT_SSDP:
				processMessageSSDP(packet, frame, ipv4, udp);
				break;
			default:
				UdpConnection udpConnection = getUdpConnection(ipv4, udp);
				if (udpConnection == null) {
					udpConnection = new UdpConnection();
					udpConnection.sourceMacAddress = frame.srcMac;
					udpConnection.destinationMacAddress = frame.dstMac;
					udpConnection.sourceIPAddress = ipv4.sourceIPAddress;
					udpConnection.destinationIPAddress = ipv4.destinationIPAddress;
					udpConnection.sourcePort = udp.sourcePort;
					udpConnection.destinationPort = udp.destinationPort;
					udpConnection.pendingConnection = true;
					udpConnections.add(udpConnection);
				}

				byte[] data = new byte[udp.length - udp.sizeOf()];
				packet.readBytes(data);

				udpConnection.addPendingWriteData(data);
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

			sendPacket(answerPacket, answerFrame);
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
				log.error(String.format("processMessageDatagramICMP unknown type=0x%X, code=0x%X", icmp.type, icmp.code));
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

			sendPacket(answerPacket, answerFrame);
		}
	}

	private TcpConnection getTcpConnection(IPv4 ipv4, TCP tcp) {
		for (TcpConnection tcpConnection : tcpConnections) {
			if (   tcp.sourcePort == tcpConnection.sourcePort
			    && tcp.destinationPort == tcpConnection.destinationPort 
			    && Arrays.equals(ipv4.sourceIPAddress, tcpConnection.sourceIPAddress)
			    && Arrays.equals(ipv4.destinationIPAddress, tcpConnection.destinationIPAddress)
			    ) {
				return tcpConnection;
			}
		}

		// Not found
		return null;
	}

	private UdpConnection getUdpConnection(IPv4 ipv4, UDP udp) {
		for (UdpConnection udpConnection : udpConnections) {
			if (   udp.sourcePort == udpConnection.sourcePort
			    && udp.destinationPort == udpConnection.destinationPort 
			    && Arrays.equals(ipv4.sourceIPAddress, udpConnection.sourceIPAddress)
			    && Arrays.equals(ipv4.destinationIPAddress, udpConnection.destinationIPAddress)
			    ) {
				return udpConnection;
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

		TcpConnection tcpConnection = getTcpConnection(ipv4, tcp);

		boolean isInternalMessage = isMyIpAddress(ipv4.destinationIPAddress) && tcp.destinationPort == apSocketPort;

		if (tcp.flagSYN) {
			if (tcpConnection != null) {
				if (!tcpConnection.pendingConnection) {
					log.error(String.format("processMessageTCP SYN received but connection already exists: %s", tcpConnection));
					return;
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("processMessageTCP SYN received for a connection still pending (%s), retrying the connection", tcpConnection));
				}

				try {
					tcpConnection.close();
				} catch (IOException e) {
					if (log.isDebugEnabled()) {
						log.debug("error while closing connection", e);
					}
				}
				tcpConnections.remove(tcpConnection);
			}

			tcpConnection = new TcpConnection(frame, ipv4, tcp, random);
			if (isInternalMessage) {
				tcpConnection.proxyDestinationIPAddress = internetAddressToBytes(HTTPServer.getInstance().getProxyAddress());
				tcpConnection.proxyDestinationPort = HTTPServer.getInstance().getProxyPort();
			}
			tcpConnections.add(tcpConnection);
		} else if (tcp.flagACK) {
			if (tcpConnection == null) {
				// Acknowledge to an unknown connection, ignore
				if (log.isDebugEnabled()) {
					log.debug(String.format("processMessageTCP ACK received for unknown connection: %s", tcp));
				}
				return;
			}

			try {
				if (tcp.flagFIN) {
					tcpConnection.sourceSequenceNumber += tcp.data.length;
					tcpConnection.sourceSequenceNumber++;
					sendAcknowledgeTCP(tcpConnection, false);
				} else if (tcp.flagPSH || tcp.data.length > 0) {
					// Acknowledge the reception of the data
					tcpConnection.sourceSequenceNumber += tcp.data.length;
					sendAcknowledgeTCP(tcpConnection, false);

					if (log.isDebugEnabled()) {
						log.debug(String.format("processMessageTCP sending data %s: %s", tcpConnection, Utilities.getMemoryDump(tcp.data)));
					}

					// Queue the received data for the destination
					tcpConnection.addPendingWriteData(tcp.data);
				}
			} catch (IOException e) {
				log.error("processMessageTCP", e);
			}
		}
	}

	private void sendAcknowledgeTCP(TcpConnection tcpConnection, boolean flagSYN) throws EOFException {
		EtherFrame answerFrame = new EtherFrame();
		answerFrame.srcMac = tcpConnection.destinationMacAddress;
		answerFrame.dstMac = tcpConnection.sourceMacAddress;
		answerFrame.type = ETHER_TYPE_IPv4;

		IPv4 answerIPv4 = new IPv4();
		answerIPv4.protocol = IPv4_PROTOCOL_TCP;
		answerIPv4.sourceIPAddress = tcpConnection.destinationIPAddress;
		answerIPv4.destinationIPAddress = tcpConnection.sourceIPAddress;

		TCP answerTcp = new TCP();
		answerTcp.sourcePort = tcpConnection.destinationPort;
		answerTcp.destinationPort = tcpConnection.sourcePort;
		answerTcp.sequenceNumber = tcpConnection.destinationSequenceNumber;
		answerTcp.acknowledgmentNumber = tcpConnection.sourceSequenceNumber;
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

		sendPacket(answerPacket, answerFrame);
	}

	private void sendTcpData(TcpConnection tcpConnection, byte[] data) throws EOFException {
		EtherFrame answerFrame = new EtherFrame();
		answerFrame.srcMac = tcpConnection.destinationMacAddress;
		answerFrame.dstMac = tcpConnection.sourceMacAddress;
		answerFrame.type = ETHER_TYPE_IPv4;

		IPv4 answerIPv4 = new IPv4();
		answerIPv4.protocol = IPv4_PROTOCOL_TCP;
		answerIPv4.sourceIPAddress = tcpConnection.destinationIPAddress;
		answerIPv4.destinationIPAddress = tcpConnection.sourceIPAddress;

		TCP answerTcp = new TCP();
		answerTcp.sourcePort = tcpConnection.destinationPort;
		answerTcp.destinationPort = tcpConnection.sourcePort;
		answerTcp.sequenceNumber = tcpConnection.destinationSequenceNumber;
		answerTcp.acknowledgmentNumber = tcpConnection.sourceSequenceNumber;
		answerTcp.flagACK = true;
		answerTcp.flagPSH = true;
		tcpConnection.destinationSequenceNumber += data.length;
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

		sendPacket(answerPacket, answerFrame);
	}

	private boolean receiveTcpMessages() {
		boolean received = false;
		List<TcpConnection> tcpConnectionsToBeDeleted = new LinkedList<TcpConnection>();

		for (TcpConnection tcpConnection : tcpConnections) {
			if (tcpConnection.pendingConnection) {
				try {
					tcpConnection.connect();
					SocketChannel socketChannel = tcpConnection.socketChannel;
					if (socketChannel != null && socketChannel.finishConnect()) {
						tcpConnection.sourceSequenceNumber++;
						// Send SYN-ACK acknowledge
						sendAcknowledgeTCP(tcpConnection, true);
						tcpConnection.destinationSequenceNumber++;
						tcpConnection.pendingConnection = false;
					}
				} catch (IOException e) {
					// connect failed, do not send any TCP SYN-ACK, forget the connection state
					tcpConnectionsToBeDeleted.add(tcpConnection);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Pending TCP connection %s failed: %s", tcpConnection, e.toString()));
					}
				}
			}

			try {
				if (!tcpConnection.pendingConnection) {
					// Write any pending data
					byte[] pendingWriteData = tcpConnection.pendingWriteData;
					if (pendingWriteData != null) {
						tcpConnection.pendingWriteData = null;

						if (log.isDebugEnabled()) {
							log.debug(String.format("receiveTcpMessages sending pending write data: %s", Utilities.getMemoryDump(pendingWriteData)));
						}
						tcpConnection.write(pendingWriteData);
					}

					// Receive any available data
					byte[] receivedData = tcpConnection.read();
					if (receivedData != null) {
						received = true;
						sendTcpData(tcpConnection, receivedData);
					}
				}
			} catch (IOException e) {
				// Ignore exceptions
				log.error("receiveTcpMessages", e);
			}
		}

		tcpConnections.removeAll(tcpConnectionsToBeDeleted);

		return received;
	}

	private void sendUdpData(UdpConnection udpConnection, byte[] data) throws EOFException {
		EtherFrame answerFrame = new EtherFrame();
		answerFrame.srcMac = udpConnection.destinationMacAddress;
		answerFrame.dstMac = udpConnection.sourceMacAddress;
		answerFrame.type = ETHER_TYPE_IPv4;

		IPv4 answerIPv4 = new IPv4();
		answerIPv4.protocol = IPv4_PROTOCOL_UDP;
		answerIPv4.sourceIPAddress = udpConnection.destinationIPAddress;
		answerIPv4.destinationIPAddress = udpConnection.sourceIPAddress;

		UDP answerUdp = new UDP();
		answerUdp.sourcePort = udpConnection.destinationPort;
		answerUdp.destinationPort = udpConnection.sourcePort;

		// Update lengths and checksums
		answerUdp.computeChecksum();
		answerIPv4.totalLength = answerIPv4.sizeOf() + answerUdp.sizeOf();
		if (data != null) {
			answerIPv4.totalLength += data.length;
		}
		answerIPv4.computeChecksum();

		// Write the different headers in sequence
		NetPacket answerPacket = new NetPacket(BUFFER_SIZE);
		answerFrame.write(answerPacket);
		answerIPv4.write(answerPacket);
		answerUdp.write(answerPacket);
		answerPacket.writeBytes(data);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sendUdpData frame=%s", answerFrame));
			log.debug(String.format("sendUdpData IPv4=%s", answerIPv4));
			log.debug(String.format("sendUdpData UDP=%s", answerUdp));
			log.debug(String.format("sendUdpData data=%s", Utilities.getMemoryDump(data)));
		}

		sendPacket(answerPacket, answerFrame);
	}

	private boolean receiveUdpMessages() {
		boolean received = false;
		List<UdpConnection> udpConnectionsToBeDeleted = new LinkedList<UdpConnection>();

		for (UdpConnection udpConnection : udpConnections) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("receiveUdpMessages polling %s", udpConnection));
			}

			if (udpConnection.pendingConnection) {
				try {
					udpConnection.connect();
					if (udpConnection.isConnected()) {
						udpConnection.pendingConnection = false;
					}
				} catch (IOException e) {
					// connect failed, forget the connection state
					udpConnectionsToBeDeleted.add(udpConnection);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Pending UDP connection %s failed: %s", udpConnection, e.toString()));
					}
				}
			}

			try {
				if (!udpConnection.pendingConnection) {
					// Write any pending data
					byte[] pendingWriteData = udpConnection.pendingWriteData;
					if (pendingWriteData != null) {
						udpConnection.pendingWriteData = null;

						if (log.isDebugEnabled()) {
							log.debug(String.format("receiveUdpMessages sending pending write data: %s", Utilities.getMemoryDump(pendingWriteData)));
						}
						udpConnection.write(pendingWriteData);
					}

					// Receive any available data
					byte[] receivedData = udpConnection.read();
					if (receivedData != null) {
						received = true;
						sendUdpData(udpConnection, receivedData);
					}
				}

				udpConnection.closeIfNoLongerUsed();
			} catch (IOException e) {
				// Ignore exceptions
				log.error("receiveUdpMessages", e);
			}
		}

		udpConnections.removeAll(udpConnectionsToBeDeleted);

		return received;
	}

	private void sendDHCPReply(EtherFrame frame, IPv4 ipv4, UDP udp, DHCP dhcp, int messageType) throws EOFException {
		// Send back a DHCP offer message
		EtherFrame answerFrame = new EtherFrame(frame);
		answerFrame.swapSourceAndDestination();
		answerFrame.srcMac = getMacAddress();

		IPv4 answerIPv4 = new IPv4(ipv4);
		answerIPv4.destinationIPAddress = ipv4.sourceIPAddress;
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

		sendPacket(answerPacket, answerFrame);
	}

	private void processMessageDHCP(NetPacket packet, EtherFrame frame, IPv4 ipv4, UDP udp) throws EOFException {
		DHCP dhcp = new DHCP();
		dhcp.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageDHCP %s", dhcp));
		}

		if (dhcp.isDiscovery(udp, ipv4)) {
			// Send back a DHCP offer message
			sendDHCPReply(frame, ipv4, udp, dhcp, DHCP.DHCP_OPTION_MESSAGE_TYPE_DHCPOFFER);
		} else if (dhcp.isRequest(udp, ipv4, getLocalIpAddress())) {
			// Send back a DHCP acknowledgment message
			sendDHCPReply(frame, ipv4, udp, dhcp, DHCP.DHCP_OPTION_MESSAGE_TYPE_DHCPACK);
		} else if (dhcp.isRelease(udp, ipv4, getLocalIpAddress())) {
			// No message is sent back
		} else {
			log.error(String.format("Unknown DHCP request %s", dhcp));
		}
	}

	private void sendSSDPReply(EtherFrame frame, IPv4 ipv4, UDP udp, Map<String, String> headers) throws EOFException {
		StringBuilder message = new StringBuilder("HTTP/1.1 200 OK\r\n");
		for (String name : headers.keySet()) {
			message.append(String.format("%s: %s\r\n", name, headers.get(name)));
		}
		message.append("\r\n");

		// Send back a SSDP reply
		EtherFrame answerFrame = new EtherFrame(frame);
		answerFrame.swapSourceAndDestination();
		answerFrame.srcMac = getMacAddress();

		IPv4 answerIPv4 = new IPv4(ipv4);
		answerIPv4.destinationIPAddress = ipv4.sourceIPAddress;
		answerIPv4.sourceIPAddress = getIpAddress();
		answerIPv4.timeToLive--; // When a packet arrives at a router, the router decreases the TTL field.

		UDP answerUdp = new UDP(udp);
		answerUdp.swapSourceAndDestination();

		// Update lengths and checksums
		answerUdp.length = answerUdp.sizeOf() + message.length();
		answerUdp.computeChecksum();
		answerIPv4.totalLength = answerIPv4.sizeOf() + answerUdp.length;
		answerIPv4.computeChecksum();

		// Write the different headers in sequence
		NetPacket answerPacket = new NetPacket(BUFFER_SIZE);
		answerFrame.write(answerPacket);
		answerIPv4.write(answerPacket);
		answerUdp.write(answerPacket);
		answerPacket.writeString(message.toString());

		if (log.isDebugEnabled()) {
			log.debug(String.format("sendSSDPReply frame=%s", answerFrame));
			log.debug(String.format("sendSSDPReply IPv4=%s", answerIPv4));
			log.debug(String.format("sendSSDPReply UDP=%s", answerUdp));
			log.debug(String.format("sendSSDPReply message=%s", message));
		}

		sendPacket(answerPacket, answerFrame);
	}

	private void processMessageSSDP(NetPacket packet, EtherFrame frame, IPv4 ipv4, UDP udp) throws EOFException {
		SSDP ssdp = new SSDP();
		ssdp.read(packet);

		if (log.isDebugEnabled()) {
			log.debug(String.format("processMessageSSDP %s", ssdp));
		}

		String device = ssdp.getHeaderValue("ST");
		String location = String.format("http://%s:%d%sUPnP/%s", internetAddressToString(getIpAddress()), getPort(), baseUri, device);

		Map<String, String> replyHeaders = new HashMap<String, String>();
		replyHeaders.put("ST", device);
		replyHeaders.put("Location", location);

		sendSSDPReply(frame, ipv4, udp, replyHeaders);
	}

	private boolean processRequestUPnP(HTTPServer server, OutputStream os, String path, HashMap<String, String> request) throws IOException {
		if ("urn:schemas-upnp-org:device:InternetGatewayDevice:1".equals(path)) {
			StringBuilder s = new StringBuilder();
			s.append("<root>");
			s.append("  <URLBase>");
			s.append(     String.format("http://%s:%d/%s", internetAddressToString(getIpAddress()), getPort(), baseUri));
			s.append("  </URLBase>");
			s.append("  <device>");
			s.append("    <deviceList>");
			s.append("      <device>");
			s.append("        <deviceList>");
			s.append("          <device>");
			s.append("            <serviceList>");
			s.append("              <service>");
			s.append("                <serviceType>urn:schemas-upnp-org:service:WANPPPConnection:1</serviceType>");
			s.append("                <controlURL>" + baseUri + "UPnP/urn:schemas-upnp-org:service:WANPPPConnection:1</controlURL>");
			s.append("              </service>");
			s.append("            </serviceList>");
			s.append("          </device>");
			s.append("        </deviceList>");
			s.append("      </device>");
			s.append("    </deviceList>");
			s.append("  </device>");
			s.append("</root>");

			server.sendResponse(os, s.toString());
		} else if ("urn:schemas-upnp-org:service:WANPPPConnection:1".equals(path)) {
			String soapAction = request.get("soapaction");
			if (soapAction.startsWith("\"")) {
				soapAction = soapAction.substring(1, soapAction.length() - 1);
			}
			String action = soapAction.substring(soapAction.indexOf('#') + 1);
			if ("GetExternalIPAddress".equals(action)) {
				String newExternalIPAddress = upnp.getIGD().getExternalIPAddress(upnp);
				if (newExternalIPAddress == null) {
					newExternalIPAddress = "";
				}

				StringBuilder s = new StringBuilder();
				s.append("<s:Envelope>");
				s.append("  <s:Body>");
				s.append("    <m:GetExternalIPAddressResponse>");
				s.append("      <NewExternalIPAddress>" + newExternalIPAddress + "</NewExternalIPAddress>");
				s.append("    </m:GetExternalIPAddressResponse>");
				s.append("  </s:Body>");
				s.append("</s:Envelope>");

				server.sendResponse(os, s.toString());
			} else if ("AddPortMapping".equals(action)) {
				HashMap<String, String> upnpRequest = upnp.parseSimpleUPnPCommand(request.get(HTTPServer.data));

				String newRemoteHost = upnpRequest.get("NewRemoteHost");
				int newExternalPort = Integer.parseInt(upnpRequest.get("NewExternalPort"));
				String newProtocol = upnpRequest.get("NewProtocol");
				int newInternalPort = Integer.parseInt(upnpRequest.get("NewInternalPort"));
				String newInternalClient = upnpRequest.get("NewInternalClient");
				int newEnabled = Integer.parseInt(upnpRequest.get("NewEnabled"));
				String newPortMappingDescription = upnpRequest.get("NewPortMappingDescription");
				int newLeaseDuration = Integer.parseInt(upnpRequest.get("NewLeaseDuration"));

				if (newEnabled == 1) {
					upnp.getIGD().addPortMapping(upnp, newRemoteHost, newExternalPort, newProtocol, newInternalPort, newInternalClient, newPortMappingDescription, newLeaseDuration);
				}

				// Return success
				StringBuilder s = new StringBuilder();
				s.append("<s:Envelope>");
				s.append("  <s:Body>");
				s.append("    <m:AddPortMappingResponse>");
				s.append("    </m:AddPortMappingResponse>");
				s.append("  </s:Body>");
				s.append("</s:Envelope>");

				server.sendResponse(os, s.toString());
			} else if ("DeletePortMapping".equals(action)) {
				HashMap<String, String> upnpRequest = upnp.parseSimpleUPnPCommand(request.get(HTTPServer.data));

				String newRemoteHost = upnpRequest.get("NewRemoteHost");
				int newExternalPort = Integer.parseInt(upnpRequest.get("NewExternalPort"));
				String newProtocol = upnpRequest.get("NewProtocol");

				upnp.getIGD().deletePortMapping(upnp, newRemoteHost, newExternalPort, newProtocol);

				// Return success
				StringBuilder s = new StringBuilder();
				s.append("<s:Envelope>");
				s.append("  <s:Body>");
				s.append("    <m:DeletePortMappingResponse>");
				s.append("    </m:DeletePortMappingResponse>");
				s.append("  </s:Body>");
				s.append("</s:Envelope>");

				server.sendResponse(os, s.toString());
			} else {
				log.error(String.format("processRequest unimplemented SOAP action '%s' on %s", action, path));
			}
		} else {
			log.error(String.format("processRequest unimplemented %s", path));
			return false;
		}

		return true;
	}

	@Override
	public boolean processRequest(HTTPServer server, OutputStream os, String path, HashMap<String, String> request) throws IOException {
		if (!path.startsWith(baseUri)) {
			log.error(String.format("processRequest unimplemented %s", baseUri));
			return false;
		}

		String relativePath = path.substring(baseUri.length());
		if (relativePath.startsWith("UPnP/")) {
			return processRequestUPnP(server, os, relativePath.substring(5), request);
		}

		log.error(String.format("processRequest unimplemented %s", relativePath));
		return false;
	}
}
