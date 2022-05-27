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

import java.util.LinkedList;
import java.util.List;

import jpcsp.Memory;

/**
 * @author gid15
 *
 */
public class MemoryRanges {
	private List<MemoryRange> ranges = new LinkedList<MemoryRange>();

	public void addAddress(int rawAddress) {
		final int length = 4;
		final int address = rawAddress & Memory.addressMask;
		for (MemoryRange memoryRange : ranges) {
			// The most common case: the address is extending the top of the range
			if (address == memoryRange.getAddress() + memoryRange.getLength()) {
				memoryRange.extendTop(length);
				return;
			}
			if (address == memoryRange.getAddress() - length) {
				memoryRange.extendBottom(length);
				return;
			}
			if (memoryRange.isOverlappingWithAddress(address)) {
				// Address already in the range, nothing more to do
				return;
			}
		}

		MemoryRange memoryRange = new MemoryRange(rawAddress, length);
		ranges.add(memoryRange);
	}

	public void updateValues() {
		for (MemoryRange memoryRange : ranges) {
			memoryRange.updateValues();
		}
	}

	public boolean areValuesChanged() {
		for (MemoryRange memoryRange : ranges) {
			if (memoryRange.areValuesChanged()) {
				return true;
			}
		}

		return false;
	}

	public boolean isOverlappingWithAddressRange(int address, int size) {
		for (MemoryRange memoryRange : ranges) {
			if (memoryRange.isOverlappingWithAddressRange(address, size)) {
				return true;
			}
		}

		return false;
	}

	public int getValue(int address) {
		for (MemoryRange memoryRange : ranges) {
			if (memoryRange.isOverlappingWithAddress(address)) {
				return memoryRange.getValue(address);
			}
		}

		return 0;
	}

	public void clear() {
		for (MemoryRange memoryRange : ranges) {
			memoryRange.free();
		}
		ranges.clear();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		for (MemoryRange memoryRange : ranges) {
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(memoryRange.toString());
		}

		return s.toString();
	}
}
