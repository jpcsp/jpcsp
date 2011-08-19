package jpcsp.HLE;

import jpcsp.Memory;

abstract public class TPointerBase implements ITPointerBase {
	TPointer pointer;
	
	public TPointerBase(Memory memory, int address) {
		pointer = new TPointer(memory, address);
	}
	
	public boolean isAddressGood() {
		return Memory.isAddressGood(pointer.getAddress());
	}
	
	public int getAddress() {
		return pointer.getAddress();
	}
}
