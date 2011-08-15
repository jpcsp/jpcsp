package jpcsp.HLE;

import jpcsp.Memory;

final public class TPointer32 extends TPointerBase {
	public TPointer32(Memory memory, int address) {
		super(memory, address);
	}

	public int  getValue() { return pointer.getValue32(0); }
	public void setValue(int value) { pointer.setValue32(0, value); }

	public int  getValue(int offset) { return pointer.getValue32(offset); }
	public void setValue(int offset, int value) { pointer.setValue32(offset, value); }
}
