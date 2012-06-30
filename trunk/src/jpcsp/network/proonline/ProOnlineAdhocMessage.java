package jpcsp.network.proonline;

import jpcsp.network.adhoc.AdhocMessage;

public class ProOnlineAdhocMessage extends AdhocMessage {
	public ProOnlineAdhocMessage(byte[] message, int length) {
		super(message, length);
	}

	public ProOnlineAdhocMessage(int address, int length) {
		super(address, length);
	}

	public ProOnlineAdhocMessage(int address, int length, byte[] toMacAddress) {
		super(address, length, toMacAddress);
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
}
