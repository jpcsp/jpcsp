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

import jpcsp.util.Utilities;

public class MemoryChunkList {
	// The MemoryChunk objects are linked and kept sorted by address.
	//
	// low: MemoryChunk with the lowest address.
	// Start point to scan list by increasing address
	private MemoryChunk low;
	// high: MemoryChunk with the highest address.
	// Start point to scan the list by decreasing address
	private MemoryChunk high;

	public MemoryChunkList(MemoryChunk initialMemoryChunk) {
		low = initialMemoryChunk;
		high = initialMemoryChunk;
	}

	/**
	 * Remove a MemoryChunk from the list.
	 *
	 * @param memoryChunk the MemoryChunk to be removed
	 */
	public void remove(MemoryChunk memoryChunk) {
		if (memoryChunk.previous != null) {
			memoryChunk.previous.next = memoryChunk.next;
		}
		if (memoryChunk.next != null) {
			memoryChunk.next.previous = memoryChunk.previous;
		}

		if (low == memoryChunk) {
			low = memoryChunk.next;
		}
		if (high == memoryChunk) {
			high = memoryChunk.previous;
		}
	}

	/**
	 * Allocate a memory at the lowest address.
	 *
	 * @param size          the size of the memory to be allocated
	 * @param addrAlignment base address alignment of the requested block
	 * @return              the base address of the allocated memory,
	 *                      0 if the memory could not be allocated
	 */
	public int allocLow(int size, int addrAlignment) {
		for (MemoryChunk memoryChunk = low; memoryChunk != null; memoryChunk = memoryChunk.next) {
			if (memoryChunk.isAvailable(size, addrAlignment)) {
				return allocLow(memoryChunk, size, addrAlignment);
			}
		}

		return 0;
	}

	/**
	 * Allocate a memory at the highest address.
	 *
	 * @param size          the size of the memory to be allocated
	 * @param addrAlignment base address alignment of the requested block
	 * @return              the base address of the allocated memory
	 *                      0 if the memory could not be allocated
	 */
	public int allocHigh(int size, int addrAlignment) {
		for (MemoryChunk memoryChunk = high; memoryChunk != null; memoryChunk = memoryChunk.previous) {
			if (memoryChunk.isAvailable(size, addrAlignment)) {
				return allocHigh(memoryChunk, size, addrAlignment);
			}
		}

		return 0;
	}

	/**
	 * Allocate a memory at the given base address.
	 *
	 * @param addr        the base address of the memory to be allocated
	 * @param size        the size of the memory to be allocated
	 * @return            the base address of the allocated memory,
	 *                    0 if the memory could not be allocated
	 */
	public int alloc(int addr, int size) {
		for (MemoryChunk memoryChunk = low; memoryChunk != null; memoryChunk = memoryChunk.next) {
			if (memoryChunk.addr <= addr && addr < memoryChunk.addr + memoryChunk.size) {
				return alloc(memoryChunk, addr, size);
			}
		}

		return 0;
	}

	/**
	 * Allocate a memory from the MemoryChunk, at its lowest address.
	 * The MemoryChunk is updated accordingly or is removed if it stays empty.
	 *
	 * @param memoryChunk   the MemoryChunk where the memory should be allocated
	 * @param size          the size of the memory to be allocated
	 * @param addrAlignment base address alignment of the requested block
	 * @return              the base address of the allocated memory
	 */
	private int allocLow(MemoryChunk memoryChunk, int size, int addrAlignment) {
		int addr = Utilities.alignUp(memoryChunk.addr, addrAlignment);

		return alloc(memoryChunk, addr, size);
	}

	/**
	 * Allocate a memory from the MemoryChunk, at its highest address.
	 * The MemoryChunk is updated accordingly or is removed if it stays empty.
	 *
	 * @param memoryChunk   the MemoryChunk where the memory should be allocated
	 * @param size          the size of the memory to be allocated
	 * @param addrAlignment base address alignment of the requested block
	 * @return              the base address of the allocated memory
	 */
	private int allocHigh(MemoryChunk memoryChunk, int size, int addrAlignment) {
		int addr = Utilities.alignDown(memoryChunk.addr + memoryChunk.size, addrAlignment) - size;

		return alloc(memoryChunk, addr, size);
	}

	/**
	 * Allocate a memory from the MemoryChunk, given the base address.
	 * The base address must be inside the MemoryChunk
	 * The MemoryChunk is updated accordingly, is removed if it stays empty or
	 * is split into 2 remaining free parts.
	 *
	 * @param memoryChunk the MemoryChunk where the memory should be allocated
	 * @param addr        the base address of the memory to be allocated
	 * @param size        the size of the memory to be allocated
	 * @return            the base address of the allocated memory, or 0
	 *                    if the MemoryChunk is too small to allocate the desired size.
	 */
	private int alloc(MemoryChunk memoryChunk, int addr, int size) {
		if (addr < memoryChunk.addr || memoryChunk.addr + memoryChunk.size < addr + size) {
			// The MemoryChunk is too small to allocate the desired size
			// are the requested address is outside the MemoryChunk
			return 0;
		} else if (memoryChunk.size == size) {
			// Allocate the complete MemoryChunk
			remove(memoryChunk);
		} else if (memoryChunk.addr == addr) {
			// Allocate at the lowest address
			memoryChunk.size -= size;
			memoryChunk.addr += size;
		} else if (memoryChunk.addr + memoryChunk.size == addr + size) {
			// Allocate at the highest address
			memoryChunk.size -= size;
		} else {
			// Allocate in the middle of a MemoryChunk: it must be split
			// in 2 parts: one for lowest part and one for the highest part.
			// Update memoryChunk to contain the lowest part,
			// and create a new MemoryChunk to contain to highest part.
			int lowSize = addr - memoryChunk.addr;
			int highSize = memoryChunk.size - lowSize - size;
			MemoryChunk highMemoryChunk = new MemoryChunk(addr + size, highSize);
			memoryChunk.size = lowSize;

			addAfter(highMemoryChunk, memoryChunk);
		}

		return addr;
	}

	/**
	 * Add a new MemoryChunk after another one.
	 * This method does not check if the addresses are kept ordered.
	 *
	 * @param memoryChunk the MemoryChunk to be added
	 * @param reference   memoryChunk has to be added after this reference
	 */
	private void addAfter(MemoryChunk memoryChunk, MemoryChunk reference) {
		memoryChunk.previous = reference;
		memoryChunk.next = reference.next;
		reference.next = memoryChunk;
		if (memoryChunk.next != null) {
			memoryChunk.next.previous = memoryChunk;
		}

		if (high == reference) {
			high = memoryChunk;
		}
	}

	/**
	 * Add a new MemoryChunk before another one.
	 * This method does not check if the addresses are kept ordered.
	 *
	 * @param memoryChunk the MemoryChunk to be added
	 * @param reference   memoryChunk has to be added before this reference
	 */
	private void addBefore(MemoryChunk memoryChunk, MemoryChunk reference) {
		memoryChunk.previous = reference.previous;
		memoryChunk.next = reference;
		reference.previous = memoryChunk;
		if (memoryChunk.previous != null) {
			memoryChunk.previous.next = memoryChunk;
		}

		if (low == reference) {
			low = memoryChunk;
		}
	}

	/**
	 * Add a new MemoryChunk to the list. It is added in the list so that
	 * the addresses are kept in increasing order.
	 * The MemoryChunk might be merged into another adjacent MemoryChunk.
	 *
	 * @param memoryChunk the MemoryChunk to be added
	 */
	public void add(MemoryChunk memoryChunk) {
		// Scan the list to find the insertion point to keep the elements
		// ordered by increasing address.
		for (MemoryChunk scanChunk = low; scanChunk != null; scanChunk = scanChunk.next) {
			// Merge the MemoryChunk if it is adjacent to other elements in the list
			if (scanChunk.addr + scanChunk.size == memoryChunk.addr) {
				// The MemoryChunk is adjacent at its lowest address,
				// merge it into the previous one.
				scanChunk.size += memoryChunk.size;

				// Check if the gap to the next chunk has not been closed,
				// in which case, we can also merge the next chunk.
				MemoryChunk nextChunk = scanChunk.next;
				if (nextChunk != null) {
					if (scanChunk.addr + scanChunk.size == nextChunk.addr) {
						// Merge with nextChunk
						scanChunk.size += nextChunk.size;
						remove(nextChunk);
					}
				}
				return;
			} else if (memoryChunk.addr + memoryChunk.size == scanChunk.addr) {
				// The MemoryChunk is adjacent at its highest address,
				// merge it into the next one.
				scanChunk.addr = memoryChunk.addr;
				scanChunk.size += memoryChunk.size;

				// Check if the gap to the previous chunk has not been closed,
				// in which case, we can also merge the previous chunk.
				MemoryChunk previousChunk = scanChunk.previous;
				if (previousChunk != null) {
					if (previousChunk.addr + previousChunk.size == scanChunk.addr) {
						// Merge with previousChunk
						previousChunk.size += scanChunk.size;
						remove(scanChunk);
					}
				}
				return;
			} else if (scanChunk.addr > memoryChunk.addr) {
				// We have found the insertion point for the MemoryChunk,
				// add it before this element to keep the addresses in
				// increasing order.
				addBefore(memoryChunk, scanChunk);
				return;
			}
		}

		// The MemoryChunk has not yet been added, add it at the very end
		// of the list.
		if (high == null && low == null) {
			// The list is empty, add the element
			high = memoryChunk;
			low = memoryChunk;
		} else {
			addAfter(memoryChunk, high);
		}
	}

	public MemoryChunk getLowMemoryChunk() {
		return low;
	}

	public MemoryChunk getHighMemoryChunk() {
		return high;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		for (MemoryChunk memoryChunk = low; memoryChunk != null; memoryChunk = memoryChunk.next) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append(memoryChunk.toString());
		}

		return result.toString();
	}
}
