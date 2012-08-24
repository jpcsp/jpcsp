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

abstract public class TPointerBase implements ITPointerBase {
	protected TPointer pointer;
	private boolean canBeNull;

	protected TPointerBase(Memory memory, int address, boolean canBeNull) {
		pointer = new TPointer(memory, address);
		this.canBeNull = canBeNull;
	}

	@Override
	public boolean isAddressGood() {
		return Memory.isAddressGood(pointer.getAddress());
	}
	
	@Override
	public boolean isAlignedTo(int offset) {
		return pointer.isAlignedTo(offset);
	}

	@Override
	public int getAddress() {
		return pointer.getAddress();
	}

	@Override
	public boolean isNull() {
		return pointer.isNull();
	}

	@Override
	public boolean isNotNull() {
		return pointer.isNotNull();
	}

	/**
	 * Tests if the value can be set.
	 * A value can be set if the pointer cannot be NULL or is not NULL.
	 * A value can be ignored if the pointer can be NULL and is NULL.
	 * 
	 * @return true  if the value can be set
	 *         false if the value can be ignored
	 */
	protected boolean canSetValue() {
		return !canBeNull || isNotNull();
	}

	@Override
	public String toString() {
		return String.format("0x%08X", getAddress());
	}
}
