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
package jpcsp.HLE;

import jpcsp.Memory;

final public class TPointer64 extends TPointerBase {
	public static final TPointer64 NULL = new TPointer64();

	private TPointer64() {
		super();
	}

	public TPointer64(Memory memory, int address) {
		super(memory, address, false);
	}

	public TPointer64(Memory memory, int address, boolean canBeNull) {
		super(memory, address, canBeNull);
	}

	public TPointer64(TPointer pointer) {
		super(pointer.getMemory(), pointer.getAddress(), false);
	}

	public TPointer64(TPointer pointer, int offset) {
		super(pointer.getMemory(), pointer.getAddress() + offset, false);
	}

	public long getValue() {
		return pointer.getValue64();
	}

	public void setValue(long value) {
		if (canSetValue()) {
			pointer.setValue64(value);
		}
	}

	public long getValue(int offset) {
		return pointer.getValue64(offset);
	}

	public void setValue(int offset, long value) {
		if (canSetValue()) {
			pointer.setValue64(offset, value);
		}
	}

	public TPointer64 forceNonNull() {
		pointer.forceNonNull();
		return this;
	}

	public void incrValue(long value) {
		incrValue(0, value);
	}

	public void incrValue(int offset, long value) {
		if (canSetValue()) {
			pointer.setValue64(offset, pointer.getValue64(offset) + value);
		}
	}

	public void decrValue(long value) {
		decrValue(0, value);
	}

	public void decrValue(int offset, long value) {
		incrValue(offset, -value);
	}
}
