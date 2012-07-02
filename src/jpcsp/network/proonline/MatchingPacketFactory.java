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

import static jpcsp.HLE.modules150.sceNet.convertMacAddressToString;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_ACCEPT;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_CANCEL;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DATA;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DISCONNECT;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_HELLO;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_JOIN;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import jpcsp.network.adhoc.MatchingObject;

/**
 * @author gid15
 *
 */
public class MatchingPacketFactory {
	public static final int ADHOC_MATCHING_PACKET_PING = 0;
	public static final int ADHOC_MATCHING_PACKET_HELLO = 1;
	public static final int ADHOC_MATCHING_PACKET_JOIN = 2;
	public static final int ADHOC_MATCHING_PACKET_ACCEPT = 3;
	public static final int ADHOC_MATCHING_PACKET_CANCEL = 4;
	public static final int ADHOC_MATCHING_PACKET_BULK = 5;
	public static final int ADHOC_MATCHING_PACKET_BULK_ABORT = 6;
	public static final int ADHOC_MATCHING_PACKET_BIRTH = 7;
	public static final int ADHOC_MATCHING_PACKET_DEATH = 8;
	public static final int ADHOC_MATCHING_PACKET_BYE = 9;

	private static abstract class MatchingPacketOpcode extends ProOnlineAdhocMatchingEventMessage {
		public MatchingPacketOpcode(MatchingObject matchingObject, int event, byte[] message, int length) {
			super(matchingObject, event, message, length);
		}

		public MatchingPacketOpcode(MatchingObject matchingObject, int event, int packetOpcode) {
			super(matchingObject, event, packetOpcode);
		}

		@Override
		public byte[] getMessage() {
			byte[] message = new byte[getMessageLength()];
			offset = 0;
			addToBytes(message, (byte) getPacketOpcode());

			return message;
		}

		@Override
		public void setMessage(byte[] message, int length) {
			if (length >= getMessageLength()) {
				offset = 0;
				setPacketOpcode(copyByteFromBytes(message));
			}
		}

		@Override
		public int getMessageLength() {
			return 1;
		}
	}

	private static class MatchingPacketPing extends MatchingPacketOpcode {
		public MatchingPacketPing(MatchingObject matchingObject) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING, ADHOC_MATCHING_PACKET_PING);
		}

		public MatchingPacketPing(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING, message, length);
		}
	}

	private static class MatchingPacketHello extends ProOnlineAdhocMatchingEventMessage {
		public MatchingPacketHello(MatchingObject matchingObject, int address, int length, byte[] toMacAddress) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_HELLO, ADHOC_MATCHING_PACKET_HELLO, address, length, toMacAddress);
		}

		public MatchingPacketHello(MatchingObject matchingObject) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_HELLO, ADHOC_MATCHING_PACKET_HELLO);
		}

		public MatchingPacketHello(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_HELLO,  message, length);
		}
	}

	private static class MatchingPacketJoin extends ProOnlineAdhocMatchingEventMessage {
		public MatchingPacketJoin(MatchingObject matchingObject, int address, int length, byte[] toMacAddress) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_JOIN, ADHOC_MATCHING_PACKET_JOIN, address, length, toMacAddress);
		}

		public MatchingPacketJoin(MatchingObject matchingObject) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_JOIN, ADHOC_MATCHING_PACKET_JOIN);
		}

		public MatchingPacketJoin(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_JOIN, message, length);
		}
	}

	/**
	 * @author gid15
	 *
	 * A MatchingPacketAccept is consisting of:
	 * - 1 byte for the event
	 * - 4 bytes for the message data length
	 * - 4 bytes for the sibling count
	 * - n bytes for the message data
	 * - m bytes for the sibling MAC addresses (6 bytes per sibling)
	 */
	private static class MatchingPacketAccept extends ProOnlineAdhocMatchingEventMessage {
		protected static final int HEADER_SIZE = 1 + 4 + 4;

		public MatchingPacketAccept(MatchingObject matchingObject, int address, int length, byte[] toMacAddress) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_ACCEPT, ADHOC_MATCHING_PACKET_ACCEPT, address, length, toMacAddress);
		}

		public MatchingPacketAccept(MatchingObject matchingObject) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_ACCEPT, ADHOC_MATCHING_PACKET_ACCEPT);
		}

		public MatchingPacketAccept(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_ACCEPT, message, length);
		}

		@Override
		public byte[] getMessage() {
			byte[] message = new byte[getMessageLength()];
			offset = 0;
			addToBytes(message, (byte) getEvent());
			addInt32ToBytes(message, getDataLength());
			int siblingCount = getSiblingCount();
			addInt32ToBytes(message, siblingCount);
			addToBytes(message, data);
			for (int i = 0; i < siblingCount; i++) {
				addToBytes(message, getMatchingObject().getMembers().get(i).macAddress);
			}

			return message;
		}

		@Override
		public void setMessage(byte[] message, int length) {
			if (length >= HEADER_SIZE) {
				offset = 0;
				setPacketOpcode(copyByteFromBytes(message));
				int dataLength = copyInt32FromBytes(message);
				int siblingCount = copyInt32FromBytes(message);
				int restLength = length - HEADER_SIZE - siblingCount * MAC_ADDRESS_LENGTH;
				data = new byte[Math.min(dataLength, restLength)];
				copyFromBytes(message, data);
				byte[] mac = new byte[MAC_ADDRESS_LENGTH];
				for (int i = 0; i < siblingCount; i++) {
					copyFromBytes(message, mac);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received Sibling#%i: MAC %s", i, convertMacAddressToString(mac)));
					}
				}
			}
		}

		protected int getSiblingCount() {
			return getMatchingObject().getMembers().size();
		}

		@Override
		public int getMessageLength() {
			return HEADER_SIZE + getDataLength() + getSiblingCount() * MAC_ADDRESS_LENGTH;
		}
	}

	private static class MatchingPacketCancel extends ProOnlineAdhocMatchingEventMessage {
		public MatchingPacketCancel(MatchingObject matchingObject, int address, int length, byte[] toMacAddress) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_CANCEL, ADHOC_MATCHING_PACKET_CANCEL, address, length, toMacAddress);
		}

		public MatchingPacketCancel(MatchingObject matchingObject) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_CANCEL, ADHOC_MATCHING_PACKET_CANCEL);
		}

		public MatchingPacketCancel(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_CANCEL, message, length);
		}
	}

	private static class MatchingPacketBulk extends ProOnlineAdhocMatchingEventMessage {
		public MatchingPacketBulk(MatchingObject matchingObject, int address, int length, byte[] toMacAddress) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_DATA, ADHOC_MATCHING_PACKET_BULK, address, length, toMacAddress);
		}

		public MatchingPacketBulk(MatchingObject matchingObject) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_DATA, ADHOC_MATCHING_PACKET_BULK);
		}

		public MatchingPacketBulk(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_DATA, message, length);
		}
	}

	private static class MatchingPacketBye extends MatchingPacketOpcode {
		public MatchingPacketBye(MatchingObject matchingObject) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_DISCONNECT, ADHOC_MATCHING_PACKET_BYE);
		}

		public MatchingPacketBye(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_DISCONNECT, message, length);
		}
	}

	public static ProOnlineAdhocMatchingEventMessage createPacket(ProOnlineNetworkAdapter proOnline, MatchingObject matchingObject, byte[] message, int length) {
		if (length > 0 && message != null && message.length > 0) {
			switch (message[0]) {
				case ADHOC_MATCHING_PACKET_PING:
					return new MatchingPacketPing(matchingObject, message, length);
				case ADHOC_MATCHING_PACKET_HELLO:
					return new MatchingPacketHello(matchingObject, message, length);
				case ADHOC_MATCHING_PACKET_JOIN:
					return new MatchingPacketJoin(matchingObject, message, length);
				case ADHOC_MATCHING_PACKET_ACCEPT:
					return new MatchingPacketAccept(matchingObject, message, length);
				case ADHOC_MATCHING_PACKET_CANCEL:
					return new MatchingPacketCancel(matchingObject, message, length);
				case ADHOC_MATCHING_PACKET_BULK:
					return new MatchingPacketBulk(matchingObject, message, length);
				case ADHOC_MATCHING_PACKET_BYE:
					return new MatchingPacketBye(matchingObject, message, length);
			}
		}

		return null;
	}

	public static ProOnlineAdhocMatchingEventMessage createPacket(ProOnlineNetworkAdapter proOnline, MatchingObject matchingObject, int event) {
		switch (event) {
			case PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING:
				return new MatchingPacketPing(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_HELLO:
				return new MatchingPacketHello(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_JOIN:
				return new MatchingPacketJoin(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_ACCEPT:
				return new MatchingPacketAccept(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_CANCEL:
				return new MatchingPacketCancel(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_DATA:
				return new MatchingPacketBulk(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_DISCONNECT:
				return new MatchingPacketBye(matchingObject);
		}

		return null;
	}

	public static ProOnlineAdhocMatchingEventMessage createPacket(ProOnlineNetworkAdapter proOnline, MatchingObject matchingObject, int event, int data, int dataLength, byte[] macAddress) {
		switch (event) {
			case PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING:
				return new MatchingPacketPing(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_HELLO:
				return new MatchingPacketHello(matchingObject, data, dataLength, macAddress);
			case PSP_ADHOC_MATCHING_EVENT_JOIN:
				return new MatchingPacketJoin(matchingObject, data, dataLength, macAddress);
			case PSP_ADHOC_MATCHING_EVENT_ACCEPT:
				return new MatchingPacketAccept(matchingObject, data, dataLength, macAddress);
			case PSP_ADHOC_MATCHING_EVENT_CANCEL:
				return new MatchingPacketCancel(matchingObject, data, dataLength, macAddress);
			case PSP_ADHOC_MATCHING_EVENT_DATA:
				return new MatchingPacketBulk(matchingObject, data, dataLength, macAddress);
			case PSP_ADHOC_MATCHING_EVENT_DISCONNECT:
				return new MatchingPacketBye(matchingObject);
		}

		return null;
	}
}
