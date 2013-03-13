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
package jpcsp.Allegrex.compiler;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public class MemoryRange {
	private int address;
	private int length;
	private int[] values;

	public MemoryRange(int address, int length) {
		setAddress(address);
		setLength(length);
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address & Memory.addressMask;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void updateValues() {
		values = new int[length >> 2];

		if (RuntimeContext.memoryInt == null) {
			IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 4);
			for (int i = 0; i < values.length; i++) {
				values[i] = memoryReader.readNext();
			}
		} else {
			System.arraycopy(RuntimeContext.memoryInt, address >> 2, values, 0, values.length);
		}
	}

	public boolean isOverlappingWithAddress(int address) {
		return this.address <= address && address < this.address + length;
	}

	public void extendBottom(int size) {
		address -= size;
		length += size;
	}

	public void extendTop(int size) {
		length += size;
	}

	public boolean areValuesChanged() {
		if (RuntimeContext.memoryInt == null) {
			IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 4);
			for (int i = 0; i < values.length; i++) {
				if (values[i] != memoryReader.readNext()) {
					return true;
				}
			}
		} else {
			// Optimized for the most common case (i.e. using memoryInt)
			int[] memoryInt = RuntimeContext.memoryInt;
			int memoryIndex = address >> 2;
			for (int i = 0; i < values.length; i++, memoryIndex++) {
				if (memoryInt[memoryIndex] != values[i]) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean isOverlappingWithAddressRange(int address, int size) {
		// Address range is completely above or below our range: no overlap
		// E.g.:
		//                           [...MemoryRange...]
		//      [...address&size...]           or        [...address&size...]
		if (address >= this.address + length || address + size < this.address) {
			return false;
		}

		// The range begin or end is within our range: overlap
		// E.g.:
		//                    [...MemoryRange...]
		//      [...address&size...]  or   [...address&size...]
		if (isOverlappingWithAddress(address) || isOverlappingWithAddress(address + size)) {
			return true;
		}

		// Range overlaps completely our range: overlap
		// E.g.:
		//         [...MemoryRange...]
		//      [.....address&size.....]
		if (address < this.address && address + size >= this.address + length) {
			return true;
		}

		// No overlap found
		return false;
	}

	@Override
	public String toString() {
		return String.format("[0x%08X-0x%08X]", address, address + length);
	}
}
