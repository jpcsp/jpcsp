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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules150.sceNetInet;
import jpcsp.HLE.modules150.sceNetAdhoc.GameModeArea;
import jpcsp.network.BaseNetworkAdapter;
import jpcsp.network.adhoc.AdhocMatchingEventMessage;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.MatchingObject;
import jpcsp.network.adhoc.PdpObject;
import jpcsp.network.adhoc.PtpObject;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlPacketBaseC2S;
import jpcsp.network.proonline.PacketFactory.SceNetAdhocctlPacketBaseS2C;
import jpcsp.network.upnp.UPnP;

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
	private List<MacIp> macIps = new LinkedList<MacIp>();
	private PacketFactory packetFactory = new PacketFactory();

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

		upnp = new UPnP();
		upnp.discover();
	}

	protected void sendToMetaServer(SceNetAdhocctlPacketBaseC2S packet) throws IOException {
		metaSocket.getOutputStream().write(packet.getBytes());
		metaSocket.getOutputStream().flush();
		if (log.isTraceEnabled()) {
			log.trace(String.format("Sent packet to meta server: %s", packet));
		}
	}

	protected void safeSendToMetaServer(SceNetAdhocctlPacketBaseC2S packet) {
		try {
			sendToMetaServer(packet);
		} catch (IOException e) {
			// Ignore exception
		}
	}

	@Override
	public void sceNetAdhocctlInit() {
		if (log.isDebugEnabled()) {
			log.debug("proNetAdhocctlInit");
		}

		try {
			metaSocket = new Socket(metaServer, metaPort);
			metaSocket.setReuseAddress(true);
			metaSocket.setSoTimeout(500);

			PacketFactory.SceNetAdhocctlLoginPacketC2S loginPacket = new PacketFactory.SceNetAdhocctlLoginPacketC2S();

			sendToMetaServer(loginPacket);

			Thread friendFinderThread = new FriendFinder();
			friendFinderThread.setName("ProLine Friend Finder");
			friendFinderThread.setDaemon(true);
			friendFinderThread.start();
		} catch (UnknownHostException e) {
			log.error("proNetAdhocctlInit", e);
		} catch (IOException e) {
			log.error("proNetAdhocctlInit", e);
		}
	}

	@Override
	public void sceNetAdhocctlConnect() {
		if (log.isDebugEnabled()) {
			log.debug("proNetAdhocctlConnect");
		}

		sceNetAdhocctlCreate();
	}

	@Override
	public void sceNetAdhocctlCreate() {
		if (log.isDebugEnabled()) {
			log.debug("proNetAdhocctlCreate");
		}

		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlConnectPacketC2S());
		} catch (IOException e) {
			log.error("proNetAdhocctlCreate", e);
		}
	}

	@Override
	public void sceNetAdhocctlDisconnect() {
		if (log.isDebugEnabled()) {
			log.debug("proNetAdhocctlDisconnect");
		}

		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlDisconnectPacketC2S());
		} catch (IOException e) {
			log.error("proNetAdhocctlDisconnect", e);
		}
	}

	@Override
	public void sceNetAdhocctlTerm() {
		if (log.isDebugEnabled()) {
			log.debug("proNetAdhocctlTerm");
		}

		exit = true;
	}

	@Override
	public void sceNetAdhocctlScan() {
		if (log.isDebugEnabled()) {
			log.debug("proNetAdhocctlScan");
		}

		try {
			sendToMetaServer(new PacketFactory.SceNetAdhocctlScanPacketC2S());
		} catch (IOException e) {
			log.error("proNetAdhocctlScan", e);
		}
	}

	protected void friendFinder() {
		long lastPing = Emulator.getClock().currentTimeMillis();
		byte[] buffer = new byte[1024];
		int offset = 0;

		if (log.isDebugEnabled()) {
			log.debug("Starting friendFinder");
		}

		while (!exit) {
			long now = Emulator.getClock().currentTimeMillis();
			if (now - lastPing >= pingTimeoutMillis) {
				lastPing = now;
				safeSendToMetaServer(new PacketFactory.SceNetAdhocctlPingPacketC2S());
			}

			try {
				int length = metaSocket.getInputStream().read(buffer, offset, buffer.length - offset);
				if (length > 0) {
					offset += length;
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

		try {
			metaSocket.close();
		} catch (IOException e) {
			log.error("friendFinder", e);
		}
		metaSocket = null;
	}

	public static String convertIpToString(int ip) {
		return String.format("%d.%d.%d.%d", ip & 0xFF, (ip >> 8) & 0xFF, (ip >> 16) & 0xFF, (ip >> 24) & 0xFF);
	}

	protected void addFriend(String nickName, pspNetMacAddress mac, int ip) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Adding friend nickName='%s', mac=%s, ip=%s", nickName, mac, convertIpToString(ip)));
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
		}
	}

	@Override
	public SocketAddress getSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
		for (MacIp macIp : macIps) {
			if (isSameMacAddress(macAddress, macIp.mac)) {
				return new InetSocketAddress(macIp.inetAddress, realPort);
			}
		}

		return sceNetInet.getBroadcastInetSocketAddress(realPort);
	}

	public List<MacIp> getMacIps() {
		return macIps;
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
		return new ProOnlineAdhocMessage(address, length, destMacAddress);
	}

	@Override
	public AdhocMessage createAdhocPdpMessage(byte[] message, int length) {
		return new ProOnlineAdhocMessage(message, length);
	}

	@Override
	public AdhocMessage createAdhocPtpMessage(int address, int length) {
		return new ProOnlineAdhocMessage(address, length);
	}

	@Override
	public AdhocMessage createAdhocPtpMessage(byte[] message, int length) {
		return new ProOnlineAdhocMessage(message, length);
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
		return new ProOnlineAdhocMatchingEventMessage(matchingObject, event);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, int event, int data, int dataLength, byte[] macAddress) {
		return new ProOnlineAdhocMatchingEventMessage(matchingObject, event, data, dataLength, macAddress);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, byte[] message, int length) {
		return new ProOnlineAdhocMatchingEventMessage(matchingObject, message, length);
	}
}
