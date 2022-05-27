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
package jpcsp.HLE.kernel.types;

import static jpcsp.util.Utilities.alignUp;

public class MemoryChunk {
	// Start address of this MemoryChunk
	public int addr;
	// Size of this MemoryChunk: it extends from addr to (addr + size -1)
	public int size;
	// The MemoryChunk are kept sorted by addr and linked with next/previous
	// The MemoryChunk with the lowest addr has previous == null
	// The MemoryChunk with the highest addr has next == null
	public MemoryChunk next;
	public MemoryChunk previous;

	public MemoryChunk(int addr, int size) {
		this.addr = addr;
		this.size = size;
	}

	/**
	 * Check if the memoryChunk has enough space to allocate a block.
	 *
	 * @param requestedSize size of the requested block
	 * @param addrAlignment base address alignment of the requested block
	 * @return              true if the chunk is large enough to allocate the block
	 *                      false if the chunk is too small for the requested block
	 */
	public boolean isAvailable(int requestedSize, int addrAlignment) {
		// Quick check on requested size
		if (requestedSize > size) {
			return false;
		}

		if (alignUp(addr, addrAlignment) + requestedSize <= addr + size) {
			return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return String.format("[addr=0x%08X-0x%08X, size=0x%X]", addr, addr + size, size);
	}
}
