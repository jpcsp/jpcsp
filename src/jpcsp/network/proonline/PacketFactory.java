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

import static jpcsp.HLE.modules150.sceNetAdhocctl.ADHOC_ID_LENGTH;
import static jpcsp.HLE.modules150.sceNetAdhocctl.GROUP_NAME_LENGTH;
import static jpcsp.HLE.modules150.sceNetAdhocctl.NICK_NAME_LENGTH;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.network.proonline.ProOnlineNetworkAdapter.convertIpToString;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetAdhocctl;
import jpcsp.HLE.modules.sceUtility;
import jpcsp.hardware.Wlan;

/**
 * @author gid15
 *
 */
public class PacketFactory {
	protected static Logger log = ProOnlineNetworkAdapter.log;
	protected static final int OPCODE_PING = 0;
	protected static final int OPCODE_LOGIN = 1;
	protected static final int OPCODE_CONNECT = 2;
	protected static final int OPCODE_DISCONNECT = 3;
	protected static final int OPCODE_SCAN = 4;
	protected static final int OPCODE_SCAN_COMPLETE = 5;
	protected static final int OPCODE_CONNECT_BSSID = 6;
	protected static final int OPCODE_CHAT = 7;
	private static final int CHAT_MESSAGE_LENGTH = 64;

	protected static abstract class SceNetAdhocctlPacketBase {
		protected final ProOnlineNetworkAdapter proOnline;
		protected int opcode;
		protected int offset;

		protected SceNetAdhocctlPacketBase(ProOnlineNetworkAdapter proOnline) {
			this.proOnline = proOnline;
		}

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

		protected pspNetMacAddress copyMacFromBytes(byte[] bytes) {
			pspNetMacAddress mac = new pspNetMacAddress();
			mac.setMacAddress(bytes, offset);
			offset += MAC_ADDRESS_LENGTH;

			return mac;
		}

		protected void copyToBytes(byte[] bytes, pspNetMacAddress mac) {
			System.arraycopy(mac.macAddress, 0, bytes, offset, MAC_ADDRESS_LENGTH);
			offset += MAC_ADDRESS_LENGTH;
		}

		protected void copyInt8ToBytes(byte[] bytes, int value) {
			bytes[offset++] = (byte) (value & 0xFF);
		}

		protected void copyInt32ToBytes(byte[] bytes, int value) {
			copyInt8ToBytes(bytes, value      );
			copyInt8ToBytes(bytes, value >>  8);
			copyInt8ToBytes(bytes, value >> 16);
			copyInt8ToBytes(bytes, value >> 24);
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

		@Override
		public String toString() {
			return String.format("%s", getClass().getSimpleName());
		}
	}

	protected static abstract class SceNetAdhocctlPacketBaseC2S extends SceNetAdhocctlPacketBase {
		protected ProOnlineServer proOnlineServer;

		protected SceNetAdhocctlPacketBaseC2S(ProOnlineNetworkAdapter proOnline) {
			super(proOnline);
		}

		protected SceNetAdhocctlPacketBaseC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer) {
			super(proOnline);
			this.proOnlineServer = proOnlineServer;
		}

		public abstract void process();
	}

	protected static abstract class SceNetAdhocctlPacketBaseS2C extends SceNetAdhocctlPacketBase {
		protected SceNetAdhocctlPacketBaseS2C(ProOnlineNetworkAdapter proOnline) {
			super(proOnline);
		}

		public abstract void process();
	}

	protected static class SceNetAdhocctlPingPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		public SceNetAdhocctlPingPacketC2S(ProOnlineNetworkAdapter proOnline) {
			super(proOnline);
			opcode = OPCODE_PING;
		}

		public SceNetAdhocctlPingPacketC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer, byte[] bytes, int length) {
			super(proOnline, proOnlineServer);
			init(bytes, length);
		}

		@Override
		public void process() {
			// Nothing to do
		}
	}

	protected static class SceNetAdhocctlDisconnectPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		public SceNetAdhocctlDisconnectPacketC2S(ProOnlineNetworkAdapter proOnline) {
			super(proOnline);
			opcode = OPCODE_DISCONNECT;
		}

		public SceNetAdhocctlDisconnectPacketC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer, byte[] bytes, int length) {
			super(proOnline, proOnlineServer);
			init(bytes, length);
		}

		@Override
		public void process() {
			proOnlineServer.processDisconnect();
		}
	}

	protected static class SceNetAdhocctlScanPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		public SceNetAdhocctlScanPacketC2S(ProOnlineNetworkAdapter proOnline) {
			super(proOnline);
			opcode = OPCODE_SCAN;
		}

		public SceNetAdhocctlScanPacketC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer, byte[] bytes, int length) {
			super(proOnline, proOnlineServer);
			init(bytes, length);
		}

		@Override
		public void process() {
			proOnlineServer.processScan();
		}
	}

	protected static class SceNetAdhocctlLoginPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		private pspNetMacAddress mac = new pspNetMacAddress();
		private String nickName;
		private String game;

		public SceNetAdhocctlLoginPacketC2S(ProOnlineNetworkAdapter proOnline) {
			super(proOnline);
			opcode = OPCODE_LOGIN;
			mac.setMacAddress(Wlan.getMacAddress());
			nickName = sceUtility.getSystemParamNickname();
			game = Modules.sceNetAdhocctlModule.hleNetAdhocctlGetAdhocID();
		}

		public SceNetAdhocctlLoginPacketC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer, byte[] bytes, int length) {
			super(proOnline, proOnlineServer);
			init(bytes, length);
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				mac = copyMacFromBytes(bytes);
				nickName = copyStringFromBytes(bytes, NICK_NAME_LENGTH);
				game = copyStringFromBytes(bytes, ADHOC_ID_LENGTH);
			}
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyToBytes(bytes, mac);
			copyToBytes(bytes, nickName, NICK_NAME_LENGTH);
			copyToBytes(bytes, game, ADHOC_ID_LENGTH);
		}

		@Override
		public int getLength() {
			return super.getLength() + MAC_ADDRESS_LENGTH + NICK_NAME_LENGTH + ADHOC_ID_LENGTH;
		}

		@Override
		public void process() {
			proOnlineServer.processLogin(mac, nickName, game);
		}
	}

	protected static class SceNetAdhocctlConnectPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		private String group;

		public SceNetAdhocctlConnectPacketC2S(ProOnlineNetworkAdapter proOnline) {
			super(proOnline);
			opcode = OPCODE_CONNECT;
			group = Modules.sceNetAdhocctlModule.hleNetAdhocctlGetGroupName();
		}

		public SceNetAdhocctlConnectPacketC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer, byte[] bytes, int length) {
			super(proOnline, proOnlineServer);
			init(bytes, length);
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				group = copyStringFromBytes(bytes, GROUP_NAME_LENGTH);
			}
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

		@Override
		public void process() {
			proOnlineServer.processConnect(group);
		}
	}

	protected static class SceNetAdhocctlChatPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		private String message;

		public SceNetAdhocctlChatPacketC2S(ProOnlineNetworkAdapter proOnline, String message) {
			super(proOnline);
			opcode = OPCODE_CHAT;
			this.message = message;
		}

		public SceNetAdhocctlChatPacketC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer, byte[] bytes, int length) {
			super(proOnline, proOnlineServer);
			init(bytes, length);
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				message = copyStringFromBytes(bytes, CHAT_MESSAGE_LENGTH);
			}
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyToBytes(bytes, message, CHAT_MESSAGE_LENGTH);
		}

		@Override
		public int getLength() {
			return super.getLength() + CHAT_MESSAGE_LENGTH;
		}

		@Override
		public void process() {
			proOnlineServer.processChat(message);
		}
	}

	private static class SceNetAdhocctlPingPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		public SceNetAdhocctlPingPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			super(proOnline);
			init(bytes, length);
		}

		@Override
		public void process() {
			// Nothing to do
		}

		@Override
		public String toString() {
			return String.format("PingPacketS2C");
		}
	}

	protected static class SceNetAdhocctlConnectPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		private String nickName;
		private pspNetMacAddress mac;
		private int ip;

		public SceNetAdhocctlConnectPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			super(proOnline);
			init(bytes, length);
		}

		public SceNetAdhocctlConnectPacketS2C(String nickName, pspNetMacAddress mac, int ip) {
			super(null);
			opcode = OPCODE_CONNECT;
			this.nickName = nickName;
			this.mac = mac;
			this.ip = ip;
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				nickName = copyStringFromBytes(bytes, NICK_NAME_LENGTH);
				mac = copyMacFromBytes(bytes);
				ip = copyInt32FromBytes(bytes);
			}
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyToBytes(bytes, nickName, NICK_NAME_LENGTH);
			copyToBytes(bytes, mac);
			copyInt32ToBytes(bytes, ip);
		}

		@Override
		public int getLength() {
			return super.getLength() + NICK_NAME_LENGTH + MAC_ADDRESS_LENGTH + 4;
		}

		@Override
		public String toString() {
			return String.format("ConnectPacketS2C[nickName='%s', mac=%s, ip=%s]", nickName, mac, convertIpToString(ip));
		}

		@Override
		public void process() {
			proOnline.addFriend(nickName, mac, ip);
		}
	}

	protected static class SceNetAdhocctlConnectBSSIDPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		private pspNetMacAddress mac;

		public SceNetAdhocctlConnectBSSIDPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			super(proOnline);
			init(bytes, length);
		}

		public SceNetAdhocctlConnectBSSIDPacketS2C(pspNetMacAddress mac) {
			super(null);
			opcode = OPCODE_CONNECT_BSSID;
			this.mac = mac;
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				mac = copyMacFromBytes(bytes);
			}
		}

		@Override
		public void process() {
			log.info(String.format("Received MAC address %s", mac));
			proOnline.setConnectComplete(true);
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyToBytes(bytes, mac);
		}

		@Override
		public int getLength() {
			return super.getLength() + MAC_ADDRESS_LENGTH;
		}

		@Override
		public String toString() {
			return String.format("ConnectBSSIDPacketS2C[mac=%s]", mac);
		}
	}

	protected static class SceNetAdhocctlScanPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		private String group;
		private pspNetMacAddress mac;

		public SceNetAdhocctlScanPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			super(proOnline);
			init(bytes, length);
		}

		public SceNetAdhocctlScanPacketS2C(String group, pspNetMacAddress mac) {
			super(null);
			opcode = OPCODE_SCAN;
			this.group = group;
			this.mac = mac;
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				group = copyStringFromBytes(bytes, GROUP_NAME_LENGTH);
				mac = copyMacFromBytes(bytes);
			}
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyToBytes(bytes, group, GROUP_NAME_LENGTH);
			copyToBytes(bytes, mac);
		}

		@Override
		public int getLength() {
			return super.getLength() + GROUP_NAME_LENGTH + MAC_ADDRESS_LENGTH;
		}

		@Override
		public void process() {
			Modules.sceNetAdhocctlModule.hleNetAdhocctlAddNetwork(group, mac, sceNetAdhocctl.PSP_ADHOCCTL_MODE_NORMAL);
		}

		@Override
		public String toString() {
			return String.format("ScanPacketS2C[group='%s', mac=%s]", group, mac);
		}
	}

	private static class SceNetAdhocctlScanCompletePacketS2C extends SceNetAdhocctlPacketBaseS2C {
		public SceNetAdhocctlScanCompletePacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			super(proOnline);
			init(bytes, length);
		}

		@Override
		public void process() {
			Modules.sceNetAdhocctlModule.hleNetAdhocctlScanComplete();
		}

		@Override
		public String toString() {
			return String.format("ScanCompletePacketS2C");
		}
	}

	protected static class SceNetAdhocctlDisconnectPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		private int ip;

		public SceNetAdhocctlDisconnectPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			super(proOnline);
			init(bytes, length);
		}

		public SceNetAdhocctlDisconnectPacketS2C(int ip) {
			super(null);
			opcode = OPCODE_DISCONNECT;
			this.ip = ip;
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				ip = copyInt32FromBytes(bytes);
			}
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyInt32ToBytes(bytes, ip);
		}

		@Override
		public void process() {
			proOnline.deleteFriend(ip);
		}

		@Override
		public int getLength() {
			return super.getLength() + 4;
		}

		@Override
		public String toString() {
			return String.format("DisconnectPacketS2C ip=%s", convertIpToString(ip));
		}
	}

	protected static class SceNetAdhocctlChatPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		private String message;
		private String nickName;

		public SceNetAdhocctlChatPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			super(proOnline);
			init(bytes, length);
		}

		public SceNetAdhocctlChatPacketS2C(String message, String nickName) {
			super(null);
			opcode = OPCODE_CHAT;
			this.message = message;
			this.nickName = nickName;
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				message = copyStringFromBytes(bytes, CHAT_MESSAGE_LENGTH);
				nickName = copyStringFromBytes(bytes, NICK_NAME_LENGTH);
			}
		}

		@Override
		public void process() {
			proOnline.displayChatMessage(nickName, message);
		}

		@Override
		protected void getBytes(byte[] bytes) {
			super.getBytes(bytes);
			copyToBytes(bytes, message, CHAT_MESSAGE_LENGTH);
			copyToBytes(bytes, nickName, NICK_NAME_LENGTH);
		}

		@Override
		public int getLength() {
			return super.getLength() + CHAT_MESSAGE_LENGTH + NICK_NAME_LENGTH;
		}

		@Override
		public String toString() {
			return String.format("ChatPacketS2C message='%s' from '%s'", message, nickName);
		}
	}

	public SceNetAdhocctlPacketBaseS2C createPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] buffer, int length) {
		if (length > 0) {
			switch (buffer[0]) {
				case OPCODE_PING:
					return new SceNetAdhocctlPingPacketS2C(proOnline, buffer, length);
				case OPCODE_CONNECT_BSSID:
					return new SceNetAdhocctlConnectBSSIDPacketS2C(proOnline, buffer, length);
				case OPCODE_CONNECT:
					return new SceNetAdhocctlConnectPacketS2C(proOnline, buffer, length);
				case OPCODE_SCAN:
					return new SceNetAdhocctlScanPacketS2C(proOnline, buffer, length);
				case OPCODE_SCAN_COMPLETE:
					return new SceNetAdhocctlScanCompletePacketS2C(proOnline, buffer, length);
				case OPCODE_DISCONNECT:
					return new SceNetAdhocctlDisconnectPacketS2C(proOnline, buffer, length);
				case OPCODE_CHAT:
					return new SceNetAdhocctlChatPacketS2C(proOnline, buffer, length);
				default:
					ProOnlineNetworkAdapter.log.error(String.format("Received unknown S2C opcode %d", buffer[0]));
					break;
			}
		}

		return null;
	}

	public SceNetAdhocctlPacketBaseC2S createPacketC2S(ProOnlineNetworkAdapter proOnline, ProOnlineServer proOnlineServer, byte[] buffer, int length) {
		if (length > 0) {
			switch (buffer[0]) {
				case OPCODE_LOGIN:
					return new SceNetAdhocctlLoginPacketC2S(proOnline, proOnlineServer, buffer, length);
				case OPCODE_PING:
					return new SceNetAdhocctlPingPacketC2S(proOnline, proOnlineServer, buffer, length);
				case OPCODE_CONNECT:
					return new SceNetAdhocctlConnectPacketC2S(proOnline, proOnlineServer, buffer, length);
				case OPCODE_DISCONNECT:
					return new SceNetAdhocctlDisconnectPacketC2S(proOnline, proOnlineServer, buffer, length);
				case OPCODE_SCAN:
					return new SceNetAdhocctlScanPacketC2S(proOnline, proOnlineServer, buffer, length);
				case OPCODE_CHAT:
					return new SceNetAdhocctlChatPacketC2S(proOnline, proOnlineServer, buffer, length);
				default:
					ProOnlineNetworkAdapter.log.error(String.format("Received unknown C2S opcode %d", buffer[0]));
					break;
			}
		}

		return null;
	}
}
