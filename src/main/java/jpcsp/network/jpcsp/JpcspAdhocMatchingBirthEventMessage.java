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
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_INTERNAL_BIRTH;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.network.BaseNetworkAdapter.log;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.adhoc.AdhocMatchingEventMessage;
import jpcsp.network.adhoc.MatchingObject;

/**
 * @author gid15
 *
 * A JpcspAdhocMatchingBirthEventMessage is consisting of:
 * - 6 bytes for the MAC address of the message sender
 * - 6 bytes for the MAC address of the message recipient
 * - 1 byte for the event
 * - 6 bytes for the MAC address of the new member
 */
public class JpcspAdhocMatchingBirthEventMessage extends AdhocMatchingEventMessage {
	protected byte[] birthMacAddress;

	public JpcspAdhocMatchingBirthEventMessage(MatchingObject matchingObject, byte[] toMacAddress, byte[] birthMacAddress) {
		super(matchingObject, PSP_ADHOC_MATCHING_EVENT_INTERNAL_BIRTH, 0, 0, toMacAddress);
		this.birthMacAddress = new byte[MAC_ADDRESS_LENGTH];
		setBirthMacAddress(birthMacAddress);
	}

	public JpcspAdhocMatchingBirthEventMessage(MatchingObject matchingObject, byte[] message, int length) {
		super(matchingObject, message, length);
	}

	public byte[] getBirthMacAddress() {
		return birthMacAddress;
	}

	public void setBirthMacAddress(byte[] birthMacAddress) {
		System.arraycopy(birthMacAddress, 0, this.birthMacAddress, 0, this.birthMacAddress.length);
	}

	@Override
	public byte[] getMessage() {
		byte[] message = new byte[getMessageLength()];
		offset = 0;
		addInt32ToBytes(message, getId());
		addToBytes(message, fromMacAddress);
		addToBytes(message, toMacAddress);
		addToBytes(message, (byte) getEvent());
		addToBytes(message, birthMacAddress);

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
			birthMacAddress = new byte[MAC_ADDRESS_LENGTH];
			copyFromBytes(message, birthMacAddress);
		}
	}

	@Override
	public int getMessageLength() {
		return JpcspAdhocMatchingEventMessage.HEADER_SIZE + MAC_ADDRESS_LENGTH;
	}

	@Override
	public void processOnReceive(int macAddr, int optData, int optLen) {
		pspNetMacAddress fromMacAddress = new pspNetMacAddress();
		fromMacAddress.read(Memory.getInstance(), macAddr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("MatchingPacketBirth.processOnReceive fromMacAddress=%s, optData=0x%08X, optLen=0x%X, macAddress=%s", fromMacAddress, optData, optLen, new pspNetMacAddress(birthMacAddress)));
		}

		getMatchingObject().addMember(birthMacAddress);

		super.processOnReceive(macAddr, optData, optLen);
	}

	@Override
	public String toString() {
		return String.format("%s[id=0x%X, fromMacAddress=%s, toMacAddress=%s, birthMacAddress=%s]", getClass().getSimpleName(), getId(), convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), convertMacAddressToString(birthMacAddress));
	}
}
