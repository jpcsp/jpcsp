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
package jpcsp.network.jpcsp;

import static jpcsp.HLE.modules.sceNet.convertMacAddressToString;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.network.BaseNetworkAdapter.log;

import java.util.LinkedList;

import jpcsp.HLE.modules.sceNet;
import jpcsp.HLE.modules.sceNetAdhocMatching;
import jpcsp.network.adhoc.AdhocMatchingEventMessage;
import jpcsp.network.adhoc.MatchingObject;

/**
 * @author gid15
 *
 * A JpcspAdhocMatchingAcceptEventMessage is consisting of:
 * - 6   bytes for the MAC address of the message sender
 * - 6   bytes for the MAC address of the message recipient
 * - 4   bytes for the number N of members
 * - 6*N bytes for the each member MAC address
 * - X   bytes data
 */
public class JpcspAdhocMatchingAcceptEventMessage extends AdhocMatchingEventMessage {
	protected static final int HEADER_SIZE = JpcspAdhocMatchingEventMessage.HEADER_SIZE + 4;
	protected LinkedList<byte[]> members;

	public JpcspAdhocMatchingAcceptEventMessage(MatchingObject matchingObject, int event, int address, int length, byte[] toMacAddress) {
		super(matchingObject, event, address, length, toMacAddress);
	}

	public JpcspAdhocMatchingAcceptEventMessage(MatchingObject matchingObject, byte[] message, int length) {
		super(matchingObject, message, length);
	}

	protected int getMemberCount() {
		// Matching object not yet initialized
		if (getMatchingObject() == null) {
			return 0;
		}

		// Send members only for MODE_HOST
		if (getMatchingObject().getMode() != sceNetAdhocMatching.PSP_ADHOC_MATCHING_MODE_HOST) {
			return 0;
		}
		return getMatchingObject().getMembers().size();
	}

	@Override
	public byte[] getMessage() {
		byte[] message = new byte[getMessageLength()];
		offset = 0;
		addInt32ToBytes(message, getId());
		addToBytes(message, fromMacAddress);
		addToBytes(message, toMacAddress);
		addToBytes(message, (byte) getEvent());
		int memberCount = getMemberCount();
		addInt32ToBytes(message, memberCount);
		for (int i = 0; i < memberCount; i++) {
			addToBytes(message, getMatchingObject().getMembers().get(i).macAddress);
		}
		addToBytes(message, data);

		return message;
	}

	@Override
	public void setMessage(byte[] message, int length) {
		if (length >= getMessageLength()) {
			offset = 0;
			setId(copyInt32FromBytes(message));
			copyFromBytes(message, fromMacAddress);
			copyFromBytes(message, toMacAddress);
			setEvent(copyByteFromBytes(message));
			int memberCount = copyInt32FromBytes(message);
			members = new LinkedList<byte[]>();
			for (int i = 0; i < memberCount; i++) {
				byte[] mac = new byte[MAC_ADDRESS_LENGTH];
				copyFromBytes(message, mac);
				members.add(mac);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received Member#%d: MAC %s", i, convertMacAddressToString(mac)));
				}
			}
			data = new byte[length - HEADER_SIZE - MAC_ADDRESS_LENGTH * memberCount];
			copyFromBytes(message, data);
		}
	}

	@Override
	public int getMessageLength() {
		return HEADER_SIZE + MAC_ADDRESS_LENGTH * getMemberCount() + getDataLength();
	}

	@Override
	public void processOnReceive(int macAddr, int optData, int optLen) {
		if (members != null && !members.isEmpty()) {
			// Add all the members in the same order as received
			getMatchingObject().clearMembers();
			for (byte[] member : members) {
				getMatchingObject().addMember(member);
			}
		}

		super.processOnReceive(macAddr, optData, optLen);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (members != null) {
			for (byte[] member : members) {
				if (s.length() > 0) {
					s.append(", ");
				}
				s.append(sceNet.convertMacAddressToString(member));
			}
		}
		return String.format("%s[id=0x%X, fromMacAddress=%s, toMacAddress=%s, event=%d, members=%s]", getClass().getSimpleName(), getId(), convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), getEvent(), s);
	}
}
