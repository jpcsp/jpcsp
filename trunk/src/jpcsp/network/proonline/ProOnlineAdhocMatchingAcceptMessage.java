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
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;

import jpcsp.network.adhoc.MatchingObject;

/**
 * @author gid15
 *
 * A ProOnlineAdhocMatchingAcceptMessage is consisting of:
 * - 1 byte for the event
 * - 4 bytes for the message data length
 * - 4 bytes for the sibling count
 * - n bytes for the message data
 * - m bytes for the sibling MAC addresses (6 bytes per sibling)
 */
public class ProOnlineAdhocMatchingAcceptMessage extends ProOnlineAdhocMatchingEventMessage {
	protected static final int HEADER_SIZE = 1 + 4 + 4;

	public ProOnlineAdhocMatchingAcceptMessage(MatchingObject matchingObject, int event) {
		super(matchingObject, event);
	}

	public ProOnlineAdhocMatchingAcceptMessage(MatchingObject matchingObject, int event, int address, int length, byte[] toMacAddress) {
		super(matchingObject, event, address, length, toMacAddress);
	}

	public ProOnlineAdhocMatchingAcceptMessage(MatchingObject matchingObject, byte[] message, int length) {
		super(matchingObject, message, length);
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
			setEvent(copyByteFromBytes(message));
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
		return super.getMessageLength() + HEADER_SIZE + getSiblingCount() * MAC_ADDRESS_LENGTH;
	}
}
