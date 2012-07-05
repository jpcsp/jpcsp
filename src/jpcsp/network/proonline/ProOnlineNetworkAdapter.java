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
package jpcsp.network.proonline;

import static jpcsp.HLE.modules150.sceNetAdhoc.isSameMacAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.GUI.ChatGUI;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetAdhoc;
import jpcsp.HLE.modules150.sceNet;
import jpcsp.HLE.modules150.sceNetAdhoc.GameModeArea;
import jpcsp.network.BaseNetworkAdapter;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocMatchingEventMessage;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.MatchingObject;
import jpcsp.network.adhoc.PdpObject;
import jpcsp.network.adhoc.PtpObject;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlPacketBaseC2S;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlPacketBaseS2C;
import jpcsp.network.upnp.UPnP;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class ProOnlineNetworkAdapter extends BaseNetworkAdapter {
	protected static Logger log = Logger.getLogger("ProOnline");
	private static boolean enabled = false;
	private UPnP upnp;
	private Socket metaSocket;
	private static final int metaPort = 27312;
	private static String metaServer = "coldbird.uk.to";
	private static final int pingTimeoutMillis = 2000;
	private volatile boolean exit;
	private volatile boolean friendFinderActive;
	// All access to macIps have to be synchronized because they can be executed
	// from different threads (PSP thread + Friend Finder thread).
	private List<MacIp> macIps = new LinkedList<MacIp>();
	private PacketFactory packetFactory = new PacketFactory();
	private PortManager portManager;
	private InetAddress broadcastInetAddress;
	private ChatGUI chatGUI;
	private boolean connectComplete;

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		ProOnlineNetworkAdapter.enabled = enabled;
		if (enabled) {
			log.info("Enabling ProLine network");
		}
	}

	protected class FriendFinder extends Thread {
		@Override
		public void run() {
			friendFinder();
		}
	}

	@Override
	public void start() {
		super.start();

		log.info("ProOnline start");

		try {
			broadcastInetAddress = InetAddress.getByAddress(new byte[] { 1, 1, 1, 1 });
		} catch (UnknownHostException e) {
			log.error("Unable to set the broadcast address", e);
		}
		upnp = new UPnP();
		upnp.discover();
	}

	protected void sendToMetaServer(SceNetAdhocctlPacketBaseC2S packet) throws IOException {
		if (metaSocket != null) {
			metaSocket.getOutputStream().write(packet.getBytes());
			metaSocket.getOutputStream().flush();
			if (log.isTraceEnabled()) {
				log.trace(String.format("Sent packet to meta server: %s", packet));
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Message not sent to meta server because not connected: %s", packet));
			}
		}
	}

	protected void safeSendToMetaServer(SceNetAdhocctlPacketBaseC2S packet) {
		try {
			sendToMetaServer(packet);
		} catch (IOException e) {
			// Ignore exception
		}
	}

	private void openChat() {
		if (chatGUI == null || !chatGUI.isVisible()) {
			chatGUI = new ChatGUI();
			Emulator.getMainGUI().startWindowDialog(chatGUI);
			for (String nickName : Modules.sceNetAdhocctlModule.getPeersNickName()) {
				chatGUI.addMember(nickName);
			}
		}
	}

	private void closeChat() {
		if (chatGUI != null) {
			chatGUI.dispose();
			chatGUI = null;
		}
	}

	private void waitForFriendFinderToExit() {
		while (friendFinderActive && exit) {
			Utilities.sleep(1, 0);
		}
	}

	@Override
	public void sceNetAdhocctlInit() {
		if (log.isDebugEnabled()) {
			log.debug("sceNetAdhocctlInit");
		}

		// Wait for a previous instance of the Friend Finder thread to terminate
		waitForFriendFinderToExit();

		terminatePortManager();
		closeConnectionToMetaServer();
		connectToMetaServer();
		exit = false;

		portManager = new PortManager(upnp);

		Thread friendFinderThread = new FriendFinder();
		friendFinderThread.setName("ProOnline Friend Finder");
		friendFinderThread.setDaemon(true);
		friendFinderThread.start();
	}

	@Override
	public void sceNetAdhocctlConnect() {
		if (log.isDebugEnabled()) {
			log.debug("sceNetAdhocctlConnect redirecting to sceNetAdhocctlCreate");
		}

		sceNetAdhocctlCreate();
	}

	@Override
	public void sceNetAdhocctlCreate() {
		if (log.isDebugEnabled()) {
			log.debug("sceNetAdhocctlCreate");
		}

		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlConnectPacketC2S(this));
			openChat();
		} catch (IOException e) {
			log.error("sceNetAdhocctlCreate", e);
		}
	}

	@Override
	public void sceNetAdhocctlJoin() {
		if (log.isDebugEnabled()) {
			log.debug("sceNetAdhocctlJoin redirecting to sceNetAdhocctlCreate");
		}

		sceNetAdhocctlCreate();
	}

	@Override
	public void sceNetAdhocctlDisconnect() {
		if (log.isDebugEnabled()) {
			log.debug("sceNetAdhocctlDisconnect");
		}

		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlDisconnectPacketC2S(this));
			setConnectComplete(false);
			deleteAllFriends();
			closeChat();
		} catch (IOException e) {
			log.error("sceNetAdhocctlDisconnect", e);
		}
	}

	@Override
	public void sceNetAdhocctlTerm() {
		if (log.isDebugEnabled()) {
			log.debug("sceNetAdhocctlTerm");
		}

		exit = true;
		terminatePortManager();
	}

	@Override
	public void sceNetAdhocctlScan() {
		if (log.isDebugEnabled()) {
			log.debug("sceNetAdhocctlScan");
		}

		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlScanPacketC2S(this));
		} catch (IOException e) {
			log.error("sceNetAdhocctlScan", e);
		}
	}

	public void sceNetPortOpen(String protocol, int port) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetPortOpen %s, port=%d", protocol, port));
		}
		portManager.addPort(port, protocol);
	}

	public void sceNetPortClose(String protocol, int port) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetPortClose %s, port=%d", protocol, port));
		}
		portManager.removePort(port, protocol);
	}

	private void connectToMetaServer() {
		try {
			metaSocket = new Socket(metaServer, metaPort);
			metaSocket.setReuseAddress(true);
			metaSocket.setSoTimeout(500);

			PacketFactory.SceNetAdhocctlLoginPacketC2S loginPacket = new PacketFactory.SceNetAdhocctlLoginPacketC2S(this);

			sendToMetaServer(loginPacket);
		} catch (UnknownHostException e) {
			log.error("connectToMetaServer", e);
		} catch (IOException e) {
			log.error("connectToMetaServer", e);
		}
	}

	/**
	 * Delete all the port/host mappings
	 */
	private void terminatePortManager() {
		if (portManager != null) {
			portManager.clear();
			portManager = null;
		}
	}

	private void closeConnectionToMetaServer() {
		if (metaSocket != null) {
			try {
				metaSocket.close();
			} catch (IOException e) {
				log.error("friendFinder", e);
			}
			metaSocket = null;
		}
	}

	protected void friendFinder() {
		long lastPing = Emulator.getClock().currentTimeMillis();
		byte[] buffer = new byte[1024];
		int offset = 0;

		if (log.isDebugEnabled()) {
			log.debug("Starting friendFinder");
		}

		friendFinderActive = true;

		while (!exit) {
			long now = Emulator.getClock().currentTimeMillis();
			if (now - lastPing >= pingTimeoutMillis) {
				lastPing = now;
				safeSendToMetaServer(new PacketFactory.SceNetAdhocctlPingPacketC2S(this));
			}

			try {
				int length = metaSocket.getInputStream().read(buffer, offset, buffer.length - offset);
				if (length > 0) {
					offset += length;
				} else if (length < 0) {
					// The connection has been closed by the server, try to reconnect...
					closeConnectionToMetaServer();
					connectToMetaServer();
				}
			} catch (SocketTimeoutException e) {
				// Ignore read timeout
			} catch (IOException e) {
				log.error("friendFinder", e);
			}

			if (offset > 0) {
				if (log.isTraceEnabled()) {
					log.trace(String.format("Received from meta server: OPCODE %d", buffer[0]));
				}

				int consumed = 0;
				SceNetAdhocctlPacketBaseS2C packet = packetFactory.createPacket(this, buffer, offset);
				if (packet == null) {
					// Skip the unknown opcode
					consumed = 1;
				} else if (offset >= packet.getLength()) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Incoming server packet %s", packet));
					}
					packet.process();
					consumed = packet.getLength();
				}

				if (consumed > 0) {
					System.arraycopy(buffer, consumed, buffer, 0, offset - consumed);
					offset -= consumed;
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("Exiting friendFinder");
		}

		// Be clean, send a disconnect message to the server
		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlDisconnectPacketC2S(this));
		} catch (IOException e) {
			// Ignore error
		}

		closeConnectionToMetaServer();
		exit = false;

		friendFinderActive = false;
	}

	public static String convertIpToString(int ip) {
		return String.format("%d.%d.%d.%d", ip & 0xFF, (ip >> 8) & 0xFF, (ip >> 16) & 0xFF, (ip >> 24) & 0xFF);
	}

	protected synchronized void addFriend(String nickName, pspNetMacAddress mac, int ip) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Adding friend nickName='%s', mac=%s, ip=%s", nickName, mac, convertIpToString(ip)));
		}

		if (chatGUI != null) {
			chatGUI.addMember(nickName);
		}
		Modules.sceNetAdhocctlModule.hleNetAdhocctlAddPeer(nickName, mac);

		boolean found = false;
		for (MacIp macIp : macIps) {
			if (mac.equals(macIp.mac)) {
				macIp.setIp(ip);
				found = true;
				break;
			}
		}

		if (!found) {
			MacIp macIp = new MacIp(mac.macAddress, ip);
			macIps.add(macIp);

			portManager.addHost(convertIpToString(ip));
		}
	}

	protected synchronized void deleteFriend(int ip) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Deleting friend ip=%s", convertIpToString(ip)));
		}

		for (MacIp macIp : macIps) {
			if (macIp.ip == ip) {
				// Delete the nickName from the Chat members
				if (chatGUI != null) {
					String nickName = Modules.sceNetAdhocctlModule.getPeerNickName(macIp.mac);
					if (nickName != null) {
						chatGUI.removeMember(nickName);
					}
				}
				// Delete the MacIp mapping
				macIps.remove(macIp);
				// Delete the peer
				Modules.sceNetAdhocctlModule.hleNetAdhocctlDeletePeer(macIp.mac);
				// Delete the router ports
				portManager.removeHost(convertIpToString(ip));
				break;
			}
		}
	}

	protected synchronized void deleteAllFriends() {
		while (!macIps.isEmpty()) {
			MacIp macIp = macIps.get(0);
			deleteFriend(macIp.ip);
		}
	}

	public boolean isBroadcast(SocketAddress socketAddress) {
		if (socketAddress instanceof InetSocketAddress) {
			InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
			return inetSocketAddress.getAddress().equals(broadcastInetAddress);
		}

		return false;
	}

	public int getBroadcastPort(SocketAddress socketAddress) {
		if (socketAddress instanceof InetSocketAddress) {
			InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
			if (inetSocketAddress.getAddress().equals(broadcastInetAddress)) {
				return inetSocketAddress.getPort();
			}
		}

		return -1;
	}

	@Override
	public SocketAddress getSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
		InetAddress inetAddress = getInetAddress(macAddress);
		if (inetAddress == null) {
			throw new UnknownHostException(String.format("ProOnline: unknown MAC address %s", sceNet.convertMacAddressToString(macAddress)));
//			return sceNetInet.getBroadcastInetSocketAddress(realPort);
		}

		return new InetSocketAddress(inetAddress, realPort);
	}

	public synchronized int getNumberMacIps() {
		return macIps.size();
	}

	public synchronized MacIp getMacIp(int index) {
		if (index < 0 || index >= macIps.size()) {
			return null;
		}
		return macIps.get(index);
	}

	public InetAddress getInetAddress(byte[] macAddress) {
		if (sceNetAdhoc.isAnyMacAddress(macAddress)) {
			return broadcastInetAddress;
		}

		MacIp macIp = getMacIp(macAddress);
		if (macIp == null) {
			return null;
		}

		return macIp.inetAddress;
	}

	public int getIp(byte[] macAddress) {
		MacIp macIp = getMacIp(macAddress);
		if (macIp == null) {
			return 0;
		}

		return macIp.ip;
	}

	public synchronized MacIp getMacIp(byte[] macAddress) {
		for (MacIp macIp : macIps) {
			if (isSameMacAddress(macAddress, macIp.mac)) {
				return macIp;
			}
		}

		return null;
	}

	public synchronized MacIp getMacIp(InetAddress inetAddress) {
		for (MacIp macIp : macIps) {
			if (inetAddress.equals(macIp.inetAddress)) {
				return macIp;
			}
		}

		return null;
	}

	public byte[] getMacAddress(InetAddress inetAddress) {
		MacIp macIp = getMacIp(inetAddress);
		if (macIp == null) {
			return null;
		}

		return macIp.mac;
	}

	@Override
	public PdpObject createPdpObject() {
		return new ProOnlinePdpObject(this);
	}

	@Override
	public PtpObject createPtpObject() {
		return new ProOnlinePtpObject(this);
	}

	@Override
	public AdhocMessage createAdhocPdpMessage(int address, int length, byte[] destMacAddress) {
		return new ProOnlineAdhocMessage(this, address, length, destMacAddress);
	}

	@Override
	public AdhocMessage createAdhocPdpMessage(byte[] message, int length) {
		return new ProOnlineAdhocMessage(this, message, length);
	}

	@Override
	public AdhocMessage createAdhocPtpMessage(int address, int length) {
		return new ProOnlineAdhocMessage(this, address, length);
	}

	@Override
	public AdhocMessage createAdhocPtpMessage(byte[] message, int length) {
		return new ProOnlineAdhocMessage(this, message, length);
	}

	@Override
	public AdhocMessage createAdhocGameModeMessage(GameModeArea gameModeArea) {
		log.error("Adhoc GameMode not supported by ProOnline");
		return null;
	}

	@Override
	public AdhocMessage createAdhocGameModeMessage(byte[] message, int length) {
		log.error("Adhoc GameMode not supported by ProOnline");
		return null;
	}

	@Override
	public MatchingObject createMatchingObject() {
		return new ProOnlineMatchingObject(this);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, int event) {
		return MatchingPacketFactory.createPacket(this, matchingObject, event);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, int event, int data, int dataLength, byte[] macAddress) {
		return MatchingPacketFactory.createPacket(this, matchingObject, event, data, dataLength, macAddress);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, byte[] message, int length) {
		return MatchingPacketFactory.createPacket(this, matchingObject, message, length);
	}

	public boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		byte[] fromMacAddress = getMacAddress(address);
		if (fromMacAddress == null) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("not for me: port=%d, address=%s, message=%s", port, address, adhocMessage));
			}
			// Unknown source IP address, ignore the message
			return false;
		}

		// Copy the source MAC address from the source InetAddress
		adhocMessage.setFromMacAddress(fromMacAddress);

		// There is no broadcasting, all messages are for me
		return true;
	}

	@Override
	public void sendChatMessage(String message) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Sending chat message '%s'", message));
		}

		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlChatPacketC2S(this, message));
		} catch (IOException e) {
			log.warn("Error while sending chat message", e);
		}
	}

	public void displayChatMessage(String nickName, String message) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Displaying chat message from '%s': '%s'", nickName, message));
		}

		chatGUI.addChatMessage(nickName, message);
	}

	public static void exit() {
		if (!isEnabled()) {
			return;
		}

		INetworkAdapter networkAdapter = Modules.sceNetModule.getNetworkAdapter();
		if (networkAdapter == null || !(networkAdapter instanceof ProOnlineNetworkAdapter)) {
			return;
		}

		ProOnlineNetworkAdapter proOnline = (ProOnlineNetworkAdapter) networkAdapter;
		proOnline.exit = true;
		proOnline.terminatePortManager();
		proOnline.waitForFriendFinderToExit();
	}

	@Override
	public boolean isConnectComplete() {
		return connectComplete;
	}

	public void setConnectComplete(boolean connectComplete) {
		this.connectComplete = connectComplete;
	}

	@Override
	public void updatePeers() {
		// Nothing to do
	}
}
