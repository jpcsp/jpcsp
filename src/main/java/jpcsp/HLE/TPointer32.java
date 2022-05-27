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

public class TPointer32 extends TPointerBase {
	public static final TPointer32 NULL = new TPointer32();

	private TPointer32() {
		super();
	}

	public TPointer32(Memory memory, int address) {
		super(memory, address, false);
	}

	public TPointer32(Memory memory, int address, boolean canBeNull) {
		super(memory, address, canBeNull);
	}

	public TPointer32(TPointer pointer) {
		super(pointer.getMemory(), pointer.getAddress(), false);
	}

	public TPointer32(TPointer pointer, int offset) {
		super(pointer.getMemory(), pointer.getAddress() + offset, false);
	}

	public TPointer32 forceNonNull() {
		pointer.forceNonNull();
		return this;
	}

	public int getValue() {
		return getValue(0);
	}

	public void setValue(int value) {
		setValue(0, value);
	}

	public void setValue(boolean value) {
		setValue(0, value);
	}

	public int getValue(int offset) {
		return pointer.getValue32(offset);
	}

	public void setValue(int offset, int value) {
		if (canSetValue()) {
			pointer.setValue32(offset, value);
		}
	}

	public void setValue(int offset, boolean value) {
		if (canSetValue()) {
			pointer.setValue32(offset, value);
		}
	}

	public TPointer getPointer() {
		return getPointer(0);
	}

	public TPointer getPointer(int offset) {
		if (isNull()) {
			return TPointer.NULL;
		}

		return new TPointer(getNewPointerMemory(), getValue(offset));
	}

	public TPointer32 getPointer32() {
		return getPointer32(0);
	}

	public TPointer32 getPointer32(int offset) {
		if (isNull()) {
			return TPointer32.NULL;
		}

		return new TPointer32(getNewPointerMemory(), getValue(offset));
	}

	public void setPointer(TPointer pointer) {
		setValue(pointer.getAddress());
	}

	public void setPointer(int offset, TPointer pointer) {
		setValue(offset, pointer.getAddress());
	}

	public TPointer32 add(int addressOffset) {
		if (isNotNull()) {
			pointer.add(addressOffset);
		}

		return this;
	}

	public TPointer32 sub(int addressOffset) {
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
			pointer.setValue32(offset, pointer.getValue32(offset) + value);
		}
	}

	public void decrValue(int value) {
		decrValue(0, value);
	}

	public void decrValue(int offset, int value) {
		incrValue(offset, -value);
	}
}
