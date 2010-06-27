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
package jpcsp.memory;

/**
 * @author gid15
 *
 */
public interface IMemoryWriter {
	/**
	 * Writes the next value to memory.
	 * The MemoryWriter can buffer the values before actually writing to the
	 * Memory.
	 * 
	 * MemoryWriters are created by calling the factory
	 *   MemoryWriter.getMemoryWriter(...)
	 * 
	 * When writing 8-bit values, only the lowest 8-bits of value are used.
	 * When writing 16-bit values, only the lowest 16-bits of value are used.
	 * 
	 * When the last value has been written, the flush()
	 * method has to be called in order to write any value buffered by the
	 * MemoryWriter.
	 * 
	 * @param value the value to be written.
	 */
	public void writeNext(int value);

	/**
	 * Write any value buffered by the MemoryWriter.
	 * This method has to be called when all the values has been written,
	 * as the last call to the MemoryWriter.
	 * After calling flush(), it is no longer allowed to call writeNext().
	 */
	public void flush();
}
