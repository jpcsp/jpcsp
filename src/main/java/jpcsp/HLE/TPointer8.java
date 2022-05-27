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

public class TPointer8 extends TPointerBase {
	public static final TPointer8 NULL = new TPointer8();

	private TPointer8() {
		super();
	}

	public TPointer8(Memory memory, int address) {
		super(memory, address, false);
	}

	public TPointer8(Memory memory, int address, boolean canBeNull) {
		super(memory, address, canBeNull);
	}

	public int getValue() {
		return pointer.getValue8();
	}

	public int getUnsignedValue() {
		return pointer.getUnsignedValue8();
	}

	public void setValue(int value) {
		if (canSetValue()) {
			pointer.setValue8((byte) value);
		}
	}

	public int getValue(int offset) {
		return pointer.getValue8(offset);
	}

	public int getUnsignedValue(int offset) {
		return pointer.getUnsignedValue8(offset);
	}

	public void setValue(int offset, int value) {
		if (canSetValue()) {
			pointer.setValue8(offset, (byte) value);
		}
	}

	public TPointer8 forceNonNull() {
		pointer.forceNonNull();
		return this;
	}

	public TPointer8 add(int addressOffset) {
		if (isNotNull()) {
			pointer.add(addressOffset);
		}

		return this;
	}

	public TPointer8 sub(int addressOffset) {
		if (isNotNull()) {
			pointer.sub(addressOffset);
		}

		return this;
	}

	public void incrValue(int value) {
		incrValue(0, value);
	}

	public void incrValue(int offset, int value) {
		if (canSetValue()) {
			pointer.setValue8(offset, (byte) (pointer.getValue8(offset) + value));
		}
	}

	public void decrValue(int value) {
		decrValue(0, value);
	}

	public void decrValue(int offset, int value) {
		incrValue(offset, -value);
	}
}
