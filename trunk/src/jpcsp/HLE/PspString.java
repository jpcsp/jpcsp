package jpcsp.HLE;

import jpcsp.util.Utilities;

public class PspString {
	protected String string;
	protected int address;
	protected int maxLength;
	
	public PspString(int address) {
		this.string = null;
		this.address = address;
		this.maxLength = 64 * 1024 * 1024; // Never will be greater than the whole PSP memory :P
	}

	public PspString(int address, int maxLength) {
		this.string = null;
		this.address = address;
		this.maxLength = maxLength;
	}
	
	public String getString() {
		if (this.string == null) this.string = Utilities.readStringNZ(address, maxLength);
		return this.string;
	}
	
	public int getAddress() {
		return this.address;
	}

	@Override
	public String toString() {
		return string;
	}
}
