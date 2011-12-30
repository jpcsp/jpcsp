package jpcsp.HLE;

import jpcsp.util.Utilities;

public class PspString {
	public String string;
	public int address;
	
	public PspString(int address) {
		this.address = address;
		this.string = Utilities.readStringZ(address);
	}

	public PspString(int address, int maxLength) {
		this.address = address;
		this.string = Utilities.readStringNZ(address, maxLength);
	}

	@Override
	public String toString() {
		return string;
	}
}
