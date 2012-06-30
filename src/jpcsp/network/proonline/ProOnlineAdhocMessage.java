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
import jpcsp.network.adhoc.AdhocMessage;

/**
 * @author gid15
 *
 * A generic ProOnlineAdhocMessage is consisting of:
 * - n bytes for the message data
 */
public class ProOnlineAdhocMessage extends AdhocMessage {
	private ProOnlineNetworkAdapter proOnline;

	public ProOnlineAdhocMessage(ProOnlineNetworkAdapter networkAdapter, byte[] message, int length) {
		super(message, length);
		this.proOnline = networkAdapter;
	}

	public ProOnlineAdhocMessage(ProOnlineNetworkAdapter networkAdapter, int address, int length) {
		super(address, length);
		this.proOnline = networkAdapter;
	}

	public ProOnlineAdhocMessage(ProOnlineNetworkAdapter networkAdapter, int address, int length, byte[] toMacAddress) {
		super(address, length, toMacAddress);
		this.proOnline = networkAdapter;
	}

	@Override
	public byte[] getMessage() {
		byte[] message = new byte[getMessageLength()];
		offset = 0;
		addToBytes(message, data);

		return message;
	}

	@Override
	public void setMessage(byte[] message, int length) {
		if (length >= 0) {
			offset = 0;
			data = new byte[length];
			copyFromBytes(message, data);
		}
	}

	@Override
	public String toString() {
		return String.format("%s[fromMacAddress=%s, toMacAddress=%s(ip=%s), dataLength=%d]", getClass().getName(), convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), proOnline.getInetAddress(toMacAddress), getDataLength());
	}
}
