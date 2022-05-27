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

import static jpcsp.HLE.modules.sceNet.convertMacAddressToString;
import static jpcsp.HLE.modules.sceNetAdhoc.isMyMacAddress;
import static jpcsp.HLE.modules.sceNetAdhoc.isSameMacAddress;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_ACCEPT;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_CANCEL;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_COMPLETE;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DATA;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DISCONNECT;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_HELLO;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_INTERNAL_BIRTH;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_JOIN;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_LEFT;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_REJECT;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_MODE_CLIENT;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_MODE_HOST;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_MODE_PTP;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.network.proonline.ProOnlineNetworkAdapter.log;

import java.util.LinkedList;
import java.util.List;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetAdhocMatching;
import jpcsp.hardware.Wlan;
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

	private static class DelayedEventCallback implements IAction {
		private MatchingObject matchingObject;
		private int event;
		private int macAddr;
		private int optLen;
		private int optData;

		public DelayedEventCallback(MatchingObject matchingObject, int event, int macAddr, int optLen, int optData) {
			this.matchingObject = matchingObject;
			this.event = event;
			this.macAddr = macAddr;
			this.optLen = optLen;
			this.optData = optData;
		}

		@Override
		public void execute() {
			matchingObject.notifyCallbackEvent(event, macAddr, optLen, optData);
		}
	}

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
				clearId(); // id is not used for ProOnline
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
		private List<byte[]> siblings;

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
			addToBytes(message, (byte) getPacketOpcode());
			addInt32ToBytes(message, getDataLength());
			int siblingCount = getSiblingCount();
			addInt32ToBytes(message, siblingCount);
			addToBytes(message, data);
			List<pspNetMacAddress> members = getMatchingObject().getMembers();
			for (int memberIndex = 0, siblingIndex = 0; memberIndex < members.size(); memberIndex++) {
				byte[] macAddress = members.get(memberIndex).macAddress;
				// ProOnline does not send as siblings the host itself and the member that just joined
				if (!isMyMacAddress(macAddress) && !isSameMacAddress(macAddress, getMatchingObject().getPendingJoinRequest())) {
					addToBytes(message, macAddress);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Sending Sibling#%d: MAC %s", siblingIndex, convertMacAddressToString(macAddress)));
					}
					siblingIndex++;
				}
			}

			return message;
		}

		@Override
		public void setMessage(byte[] message, int length) {
			if (length >= HEADER_SIZE) {
				offset = 0;
				clearId(); // id is not used for ProOnline
				setPacketOpcode(copyByteFromBytes(message));
				int dataLength = copyInt32FromBytes(message);
				int siblingCount = copyInt32FromBytes(message);
				int restLength = length - HEADER_SIZE - siblingCount * MAC_ADDRESS_LENGTH;
				data = new byte[Math.min(dataLength, restLength)];
				copyFromBytes(message, data);
				siblings = new LinkedList<byte[]>();
				for (int siblingIndex = 0; siblingIndex < siblingCount; siblingIndex++) {
					byte[] mac = new byte[MAC_ADDRESS_LENGTH];
					copyFromBytes(message, mac);
					siblings.add(mac);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received Sibling#%d: MAC %s", siblingIndex, convertMacAddressToString(mac)));
					}
				}
			}
		}

		protected int getSiblingCount() {
			// Send siblings only for MODE_HOST
			if (getMatchingObject().getMode() != sceNetAdhocMatching.PSP_ADHOC_MATCHING_MODE_HOST) {
				return 0;
			}

			int memberCount = getMatchingObject().getMembers().size();
			// Do not send as siblings the host itself and the member that just joined
			int siblingCount = memberCount - 2;

			return Math.max(siblingCount, 0);
		}

		@Override
		public int getMessageLength() {
			return HEADER_SIZE + getDataLength() + getSiblingCount() * MAC_ADDRESS_LENGTH;
		}

		@Override
		public void processOnReceive(int macAddr, int optData, int optLen) {
			if (siblings != null) {
				MatchingObject matchingObject = getMatchingObject();

				// Read the MAC address of the host which is the source MAC address of this message
				pspNetMacAddress hostMacAddress = new pspNetMacAddress();
				hostMacAddress.read(Memory.getInstance(), macAddr);

				// Add all the members in the same order as received
				matchingObject.clearMembers();

				// Add the host always first
				matchingObject.addMember(hostMacAddress.macAddress);

				// Add the siblings in the same order as received
				for (byte[] sibling : siblings) {
					matchingObject.addMember(sibling);
				}

				// Add myself (who just joined) as the last member
				matchingObject.addMember(Wlan.getMacAddress());
			}

			// Send the PSP_ADHOC_MATCHING_EVENT_ACCEPT event,
			// immediately followed by PSP_ADHOC_MATCHING_EVENT_COMPLETE
			IAction triggerEventComplete = new DelayedEventCallback(getMatchingObject(), PSP_ADHOC_MATCHING_EVENT_COMPLETE, macAddr, optLen, optData);
			getMatchingObject().notifyCallbackEvent(getEvent(), macAddr, optLen, optData, triggerEventComplete);
		}

		@Override
		public void processOnSend(int macAddr, int optData, int optLen) {
			super.processOnSend(macAddr, optData, optLen);

			// Send the PSP_ADHOC_MATCHING_EVENT_COMPLETE event from the matching input thread
			getMatchingObject().addCallbackEvent(PSP_ADHOC_MATCHING_EVENT_COMPLETE, macAddr, 0, 0);
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

		@Override
		public void processOnReceive(int macAddr, int optData, int optLen) {
			MatchingObject matchingObject = getMatchingObject();

			pspNetMacAddress macAddress = new pspNetMacAddress();
			macAddress.read(Memory.getInstance(), macAddr);

			// Set the correct event to be triggered
			switch (matchingObject.getMode()) {
				case PSP_ADHOC_MATCHING_MODE_CLIENT:
					if (matchingObject.isPendingJoinRequest(macAddress)) {
						setEvent(PSP_ADHOC_MATCHING_EVENT_REJECT);
					} else if (matchingObject.isParent(macAddress)) {
						// TODO Which event(s) need to be triggered?
						setEvent(PSP_ADHOC_MATCHING_EVENT_LEFT);
					}
					break;
				case PSP_ADHOC_MATCHING_MODE_HOST:
					if (matchingObject.isPendingJoinRequest(macAddress)) {
						setEvent(PSP_ADHOC_MATCHING_EVENT_CANCEL);
					} else {
						setEvent(PSP_ADHOC_MATCHING_EVENT_LEFT);
					}
					break;
				case PSP_ADHOC_MATCHING_MODE_PTP:
					if (matchingObject.isPendingJoinRequest(macAddress)) {
						setEvent(PSP_ADHOC_MATCHING_EVENT_REJECT);
					} else {
						setEvent(PSP_ADHOC_MATCHING_EVENT_LEFT);
					}
					break;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("MatchingPacketCancel.processOnReceive mode=%d, new event=%d", matchingObject.getMode(), getEvent()));
			}

			super.processOnReceive(macAddr, optData, optLen);
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

	private static class MatchingPacketBirth extends ProOnlineAdhocMatchingEventMessage {
		public byte[] birthMacAddress;

		public MatchingPacketBirth(MatchingObject matchingObject, byte[] toMacAddress, byte[] birthMacAddress) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_INTERNAL_BIRTH, ADHOC_MATCHING_PACKET_BIRTH, 0, 0, toMacAddress);
			this.birthMacAddress = new byte[MAC_ADDRESS_LENGTH];
			System.arraycopy(birthMacAddress, 0, this.birthMacAddress, 0, this.birthMacAddress.length);
		}

		public MatchingPacketBirth(MatchingObject matchingObject, byte[] message, int length) {
			super(matchingObject, PSP_ADHOC_MATCHING_EVENT_INTERNAL_BIRTH, message, length);
		}

		@Override
		public byte[] getMessage() {
			byte[] message = new byte[getMessageLength()];
			offset = 0;
			addToBytes(message, (byte) getPacketOpcode());
			addToBytes(message, birthMacAddress);

			return message;
		}

		@Override
		public void setMessage(byte[] message, int length) {
			if (length >= getMessageLength()) {
				offset = 0;
				clearId(); // id is not used for ProOnline
				setPacketOpcode(copyByteFromBytes(message));
				birthMacAddress = new byte[MAC_ADDRESS_LENGTH];
				copyFromBytes(message, birthMacAddress);
			}
		}

		@Override
		public int getMessageLength() {
			return 1 + MAC_ADDRESS_LENGTH;
		}

		@Override
		public void processOnReceive(int macAddr, int optData, int optLen) {
			pspNetMacAddress fromMacAddress = new pspNetMacAddress();
			fromMacAddress.read(Memory.getInstance(), macAddr);

			if (log.isDebugEnabled()) {
				log.debug(String.format("MatchingPacketBirth.processOnReceive fromMacAddress=%s, optData=0x%08X, optLen=0x%X, macAddress=%s", fromMacAddress, optData, optLen, new pspNetMacAddress(birthMacAddress)));
			}

			// Add the member that just joined as the last member
			getMatchingObject().addMember(birthMacAddress);

			super.processOnReceive(macAddr, optData, optLen);
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
				case ADHOC_MATCHING_PACKET_BIRTH:
					return new MatchingPacketBirth(matchingObject, message, length);
				default:
					log.error(String.format("MatchingPacketFactory.createPacket unimplemented packet opdate=%d", message[0]));
					break;
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
			default:
				log.error(String.format("MatchingPacketFactory.createPacket unimplemented event=%d", event));
				break;
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
			case PSP_ADHOC_MATCHING_EVENT_LEFT:
				return new MatchingPacketCancel(matchingObject, data, dataLength, macAddress);
			case PSP_ADHOC_MATCHING_EVENT_DATA:
				return new MatchingPacketBulk(matchingObject, data, dataLength, macAddress);
			case PSP_ADHOC_MATCHING_EVENT_DISCONNECT:
				return new MatchingPacketBye(matchingObject);
			case PSP_ADHOC_MATCHING_EVENT_COMPLETE:
				// There is no event complete message, the PSP_ADHOC_MATCHING_EVENT_COMPLETE
				// has already been triggered immediately.
				break;
			case PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM:
				// There is no data confirmation message, the PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM
				// has already been triggered immediately when calling sceNetAdhocMatchingSendData().
				break;
			default:
				log.error(String.format("MatchingPacketFactory.createPacket unimplemented event=%d to %s", event, new pspNetMacAddress(macAddress)));
				break;
		}

		return null;
	}

	public static ProOnlineAdhocMatchingEventMessage createBirthPacket(ProOnlineNetworkAdapter proOnline, MatchingObject matchingObject, byte[] toMacAddress, byte[] birthMacAddress) {
		return new MatchingPacketBirth(matchingObject, toMacAddress, birthMacAddress);
	}
}
