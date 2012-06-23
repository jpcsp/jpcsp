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
package jpcsp.network;

import static jpcsp.HLE.modules150.sceNetAdhocctl.ADHOC_ID_LENGTH;
import static jpcsp.HLE.modules150.sceNetAdhocctl.GROUP_NAME_LENGTH;
import static jpcsp.HLE.modules150.sceNetAdhocctl.NICK_NAME_LENGTH;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceUtility;
import jpcsp.hardware.Wlan;
import jpcsp.network.upnp.UPnP;

import org.apache.log4j.Logger;

public class ProOnline {
	protected static Logger log = Logger.getLogger("ProOnline");
	private static ProOnline instance;
	private static boolean enabled = false;
	private UPnP upnp;
	private Socket metaSocket;
	protected static final int OPCODE_PING = 0;
	protected static final int OPCODE_LOGIN = 1;
	protected static final int OPCODE_CONNECT = 2;
	protected static final int OPCODE_DISCONNECT = 3;
	protected static final int OPCODE_SCAN = 4;
	protected static final int OPCODE_SCAN_COMPLETE = 5;
	protected static final int OPCODE_CONNECT_BSSID = 6;
	protected static final int OPCODE_CHAT = 7;
	private static final int metaPort = 27312;
	private static String metaServer = "coldbird.uk.to";
	private static final int pingTimeoutMillis = 2000;
	private volatile boolean exit;

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		ProOnline.enabled = enabled;
		if (enabled) {
			log.info("Enabling ProLine network");
		}
	}

	public static ProOnline getInstance() {
		if (instance == null && isEnabled()) {
			instance = new ProOnline();
		}

		return instance;
	}

	private ProOnline() {
	}

	private static class SceNetAdhocctlPacketBase {
		protected int opcode;
		protected int offset;

		public byte[] getBytes() {
			byte[] bytes = new byte[getLength()];
			getBytes(bytes);

			return bytes;
		}

		protected void getBytes(byte[] bytes) {
			offset = 0;
			bytes[offset] = (byte) opcode;
			offset++;
		}

		protected void copyToBytes(byte[] bytes, String s, int length) {
			for (int i = 0; i < length; i++, offset++) {
				bytes[offset] = (byte) (i < s.length() ? s.charAt(i) : 0);
			}
		}

		protected String copyStringFromBytes(byte[] bytes, int length) {
			int stringLength = length;
			for (int i = 0; i < length; i++) {
				if (bytes[offset + i] == (byte) 0) {
					stringLength = i;
					break;
				}
			}

			String s = new String(bytes, offset, stringLength);
			offset += length;

			return s;
		}

		protected int copyInt8FromBytes(byte[] bytes) {
			return bytes[offset++] & 0xFF;
		}

		protected int copyInt32FromBytes(byte[] bytes) {
			return (copyInt8FromBytes(bytes)      ) |
			       (copyInt8FromBytes(bytes) << 8 ) |
			       (copyInt8FromBytes(bytes) << 16) |
			       (copyInt8FromBytes(bytes) << 24);
		}

		protected void init(byte[] bytes, int length) {
			offset = 0;
			if (length >= getLength()) {
				opcode = bytes[offset];
				offset++;
			}
		}

		public int getLength() {
			return 1;
		}
	}

	private static class SceNetAdhocctlPingPacketC2S extends SceNetAdhocctlPacketBase {
		public SceNetAdhocctlPingPacketC2S() {
			opcode = OPCODE_PING;
		}
	}

	private static class SceNetAdhocctlDisconnectPacketC2S extends SceNetAdhocctlPacketBase {
		public SceNetAdhocctlDisconnectPacketC2S() {
			opcode = OPCODE_DISCONNECT;
		}
	}

	private static class SceNetAdhocctlLoginPacketC2S extends SceNetAdhocctlPacketBase {
		pspNetMacAddress mac = new pspNetMacAddress();
		String nickName;
		String game;

		public SceNetAdhocctlLoginPacketC2S() {
			opcode = OPCODE_LOGIN;
			mac.setMacAddress(Wlan.getMacAddress());
			nickName = sceUtility.getSystemParamNickname();
			game = Modules.sceNetAdhocctlModule.hleNetAdhocctlGetAdhocID();
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			System.arraycopy(mac.macAddress, 0, bytes, offset, MAC_ADDRESS_LENGTH);
			offset += MAC_ADDRESS_LENGTH;
			copyToBytes(bytes, nickName, NICK_NAME_LENGTH);
			copyToBytes(bytes, game, ADHOC_ID_LENGTH);
		}

		@Override
		public int getLength() {
			return super.getLength() + MAC_ADDRESS_LENGTH + NICK_NAME_LENGTH + ADHOC_ID_LENGTH;
		}
	}

	private static class SceNetAdhocctlConnectPacketC2S extends SceNetAdhocctlPacketBase {
		String group;

		public SceNetAdhocctlConnectPacketC2S() {
			opcode = OPCODE_CONNECT;
			group = Modules.sceNetAdhocctlModule.hleNetAdhocctlGetGroupName();
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyToBytes(bytes, group, GROUP_NAME_LENGTH);
		}

		@Override
		public int getLength() {
			return super.getLength() + GROUP_NAME_LENGTH;
		}
	}

	private static class SceNetAdhocctlConnectPacketS2C extends SceNetAdhocctlPacketBase {
		String nickName;
		pspNetMacAddress mac;
		int ip;

		public SceNetAdhocctlConnectPacketS2C(byte[] bytes, int length) {
			init(bytes, length);
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				nickName = copyStringFromBytes(bytes, NICK_NAME_LENGTH);
				mac.setMacAddress(bytes, offset);
				offset += MAC_ADDRESS_LENGTH;
				ip = copyInt32FromBytes(bytes);
			}
		}

		@Override
		public int getLength() {
			return super.getLength() + GROUP_NAME_LENGTH;
		}
	}

	private static class SceNetAdhocctlConnectBSSIDPacketS2C extends SceNetAdhocctlPacketBase {
		pspNetMacAddress mac = new pspNetMacAddress();

		public SceNetAdhocctlConnectBSSIDPacketS2C(byte[] bytes, int length) {
			init(bytes, length);
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				mac.setMacAddress(bytes, offset);
				offset += MAC_ADDRESS_LENGTH;
			}
		}

		@Override
		public int getLength() {
			return super.getLength() + MAC_ADDRESS_LENGTH;
		}
	}

	protected class FriendFinder extends Thread {
		@Override
		public void run() {
			friendFinder();
		}
	}

	public void init() {
		log.info("ProOnline init");

		upnp = new UPnP();
		upnp.discover();
	}

	protected void sendToMetaServer(SceNetAdhocctlPacketBase packet) throws IOException {
		metaSocket.getOutputStream().write(packet.getBytes());
		metaSocket.getOutputStream().flush();
	}

	protected void safeSendToMetaServer(SceNetAdhocctlPacketBase packet) {
		try {
			sendToMetaServer(packet);
		} catch (IOException e) {
			// Ignore exception
		}
	}

	public void proNetAdhocctlInit() {
		try {
			metaSocket = new Socket(metaServer, metaPort);
			metaSocket.setReuseAddress(true);
			metaSocket.setSoTimeout(500);

			SceNetAdhocctlLoginPacketC2S loginPacket = new SceNetAdhocctlLoginPacketC2S();

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

	public void proNetAdhocctlConnect() {
		proNetAdhocctlCreate();
	}

	public void proNetAdhocctlCreate() {
		try {
			sendToMetaServer(new SceNetAdhocctlConnectPacketC2S());
		} catch (IOException e) {
			log.error("proNetAdhocctlCreate", e);
		}
	}

	public void proNetAdhocctlDisconnect() {
		try {
			sendToMetaServer(new SceNetAdhocctlDisconnectPacketC2S());
		} catch (IOException e) {
			log.error("proNetAdhocctlDisconnect", e);
		}
	}

	public void proNetAdhocctlTerm() {
		exit = true;
	}

	protected void friendFinder() {
		long lastPing = Emulator.getClock().currentTimeMillis();
		byte[] buffer = new byte[1024];
		int offset = 0;

		while (!exit) {
			long now = Emulator.getClock().currentTimeMillis();
			if (now - lastPing >= pingTimeoutMillis) {
				lastPing = now;
				safeSendToMetaServer(new SceNetAdhocctlPingPacketC2S());
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
				int consumed = 0;
				switch (buffer[0]) {
					case OPCODE_CONNECT_BSSID:
						SceNetAdhocctlConnectBSSIDPacketS2C packet = new SceNetAdhocctlConnectBSSIDPacketS2C(buffer, offset);
						if (offset >= packet.getLength()) {
							log.info(String.format("Received MAC address %s", packet.mac));
							consumed = packet.getLength();
						}
						break;
					default:
						log.error(String.format("Received unknown opcode %d", buffer[0]));
						consumed = 1;
						break;
				}

				if (consumed > 0) {
					System.arraycopy(buffer, consumed, buffer, 0, offset - consumed);
					offset -= consumed;
				}
			}
		}

		try {
			metaSocket.close();
		} catch (IOException e) {
			log.error("friendFinder", e);
		}
		metaSocket = null;
	}
}
