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
import jpcsp.network.adhoc.AdhocMessage;

/**
 * @author gid15
 *
 * A JpcspAdhocPdpMessage is consisting of:
 * - 6 bytes for the MAC address of the message sender
 * - 6 bytes for the MAC address of the message recipient
 * - n bytes for the message data
 */
public class JpcspAdhocPdpMessage extends AdhocMessage {
	protected static final int HEADER_SIZE = MAC_ADDRESS_LENGTH + MAC_ADDRESS_LENGTH;

	public JpcspAdhocPdpMessage(int address, int length, byte[] destMacAddress) {
		super(address, length, destMacAddress);
	}

	public JpcspAdhocPdpMessage() {
		super();
	}

	public JpcspAdhocPdpMessage(byte[] message, int length) {
		super(message, length);
	}

	@Override
	public void setMessage(byte[] message, int length) {
		if (length >= HEADER_SIZE) {
			offset = 0;
			copyFromBytes(message, fromMacAddress);
			copyFromBytes(message, toMacAddress);
			data = new byte[length - HEADER_SIZE];
			copyFromBytes(message, data);
		}
	}

	@Override
	public byte[] getMessage() {
		byte[] message = new byte[getMessageLength()];
		offset = 0;
		addToBytes(message, fromMacAddress);
		addToBytes(message, toMacAddress);
		addToBytes(message, data);

		return message;
	}

	@Override
	public int getMessageLength() {
		return super.getMessageLength() + HEADER_SIZE;
	}
}
