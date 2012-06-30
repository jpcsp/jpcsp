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
	protected static final int OPCODE_PING = 0;
	protected static final int OPCODE_LOGIN = 1;
	protected static final int OPCODE_CONNECT = 2;
	protected static final int OPCODE_DISCONNECT = 3;
	protected static final int OPCODE_SCAN = 4;
	protected static final int OPCODE_SCAN_COMPLETE = 5;
	protected static final int OPCODE_CONNECT_BSSID = 6;
	protected static final int OPCODE_CHAT = 7;

	protected static abstract class SceNetAdhocctlPacketBase {
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

	protected static abstract class SceNetAdhocctlPacketBaseC2S extends SceNetAdhocctlPacketBase {
	}

	protected static abstract class SceNetAdhocctlPacketBaseS2C extends SceNetAdhocctlPacketBase {
		public abstract void process();
	}

	protected static class SceNetAdhocctlPingPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		public SceNetAdhocctlPingPacketC2S() {
			opcode = OPCODE_PING;
		}
	}

	protected static class SceNetAdhocctlDisconnectPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		public SceNetAdhocctlDisconnectPacketC2S() {
			opcode = OPCODE_DISCONNECT;
		}
	}

	protected static class SceNetAdhocctlScanPacketC2S extends SceNetAdhocctlPacketBaseC2S {
		public SceNetAdhocctlScanPacketC2S() {
			opcode = OPCODE_SCAN;
		}
	}

	protected static class SceNetAdhocctlLoginPacketC2S extends SceNetAdhocctlPacketBaseC2S {
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
			copyToBytes(bytes, mac);
			copyToBytes(bytes, nickName, NICK_NAME_LENGTH);
			copyToBytes(bytes, game, ADHOC_ID_LENGTH);
		}

		@Override
		public int getLength() {
			return super.getLength() + MAC_ADDRESS_LENGTH + NICK_NAME_LENGTH + ADHOC_ID_LENGTH;
		}
	}

	protected static class SceNetAdhocctlConnectPacketC2S extends SceNetAdhocctlPacketBaseC2S {
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

	private static class SceNetAdhocctlPingPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		public SceNetAdhocctlPingPacketS2C(byte[] bytes, int length) {
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

	private static class SceNetAdhocctlConnectPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		String nickName;
		pspNetMacAddress mac;
		int ip;
		final ProOnlineNetworkAdapter proOnline;

		public SceNetAdhocctlConnectPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			this.proOnline = proOnline;
			init(bytes, length);
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

	private static class SceNetAdhocctlConnectBSSIDPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		pspNetMacAddress mac;

		public SceNetAdhocctlConnectBSSIDPacketS2C(byte[] bytes, int length) {
			init(bytes, length);
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
			ProOnlineNetworkAdapter.log.info(String.format("Received MAC address %s", mac));
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

	private static class SceNetAdhocctlScanPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		String group;
		pspNetMacAddress mac;

		public SceNetAdhocctlScanPacketS2C(byte[] bytes, int length) {
			init(bytes, length);
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
		public SceNetAdhocctlScanCompletePacketS2C(byte[] bytes, int length) {
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

	private static class SceNetAdhocctlDisconnectPacketS2C extends SceNetAdhocctlPacketBaseS2C {
		final ProOnlineNetworkAdapter proOnline;
		private int ip;

		public SceNetAdhocctlDisconnectPacketS2C(ProOnlineNetworkAdapter proOnline, byte[] bytes, int length) {
			this.proOnline = proOnline;
			init(bytes, length);
		}

		@Override
		protected void init(byte[] bytes, int length) {
			super.init(bytes, length);
			if (length >= getLength()) {
				ip = copyInt32FromBytes(bytes);
			}
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
			return String.format("DisconnectPacketS2C");
		}
	}

	public SceNetAdhocctlPacketBaseS2C createPacket(ProOnlineNetworkAdapter proOnline, byte[] buffer, int length) {
		switch (buffer[0]) {
			case OPCODE_PING:
				return new SceNetAdhocctlPingPacketS2C(buffer, length);
			case OPCODE_CONNECT_BSSID:
				return new SceNetAdhocctlConnectBSSIDPacketS2C(buffer, length);
			case OPCODE_CONNECT:
				return new SceNetAdhocctlConnectPacketS2C(proOnline, buffer, length);
			case OPCODE_SCAN:
				return new SceNetAdhocctlScanPacketS2C(buffer, length);
			case OPCODE_SCAN_COMPLETE:
				return new SceNetAdhocctlScanCompletePacketS2C(buffer, length);
			case OPCODE_DISCONNECT:
				return new SceNetAdhocctlDisconnectPacketS2C(proOnline, buffer, length);
			default:
				ProOnlineNetworkAdapter.log.error(String.format("Received unknown opcode %d", buffer[0]));
				break;
		}

		return null;
	}
}
