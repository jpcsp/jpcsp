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

import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import jpcsp.network.adhoc.AdhocMatchingEventMessage;
import jpcsp.network.adhoc.MatchingObject;

/**
 * @author gid15
 *
 * A JpcspAdhocMatchingEventMessage is consisting of:
 * - 6 bytes for the MAC address of the message sender
 * - 6 bytes for the MAC address of the message recipient
 * - 1 byte for the event
 * - n bytes for the message data
 */
public class JpcspAdhocMatchingEventMessage extends AdhocMatchingEventMessage {
	protected static final int HEADER_SIZE = MAC_ADDRESS_LENGTH + MAC_ADDRESS_LENGTH + 1;

	public JpcspAdhocMatchingEventMessage(MatchingObject matchingObject, int event) {
		super(matchingObject, event);
	}

	public JpcspAdhocMatchingEventMessage(MatchingObject matchingObject, int event, int address, int length, byte[] toMacAddress) {
		super(matchingObject, event, address, length, toMacAddress);
	}

	public JpcspAdhocMatchingEventMessage(MatchingObject matchingObject, byte[] message, int length) {
		super(matchingObject, message, length);
	}

	@Override
	public byte[] getMessage() {
		byte[] message = new byte[getMessageLength()];
		offset = 0;
		addToBytes(message, fromMacAddress);
		addToBytes(message, toMacAddress);
		addToBytes(message, (byte) getEvent());
		addToBytes(message, data);

		return message;
	}

	@Override
	public void setMessage(byte[] message, int length) {
		if (length >= HEADER_SIZE) {
			offset = 0;
			copyFromBytes(message, fromMacAddress);
			copyFromBytes(message, toMacAddress);
			setEvent(copyByteFromBytes(message));
			data = new byte[length - HEADER_SIZE];
			copyFromBytes(message, data);
		}
	}

	@Override
	public int getMessageLength() {
		return super.getMessageLength() + HEADER_SIZE;
	}
}
