package jpcsp.HLE;

import jpcsp.Memory;

final public class TPointer64 extends TPointerBase {
	public TPointer64(Memory memory, int address) {
		super(memory, address);
	}

	public long getValue() { return pointer.getValue64(0); }
	public void setValue(long value) { pointer.setValue64(0, value); }

	public long getValue(int offset) { return pointer.getValue64(offset); }
	public void setValue(int offset, long value) { pointer.setValue64(offset, value); }
}
