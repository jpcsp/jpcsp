/*

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
 * Interface for reading and writing to memory.
 * The value currently stored into memory can be retrieved
 * before writing a new value.
 */
public interface IMemoryReaderWriter extends IMemoryWriter {
	/**
	 * Read the current value from memory. This is the value
	 * that will be overwritten by the next IMemoryWriter.writeNext() call.
	 * 
	 * @return   the value that will be overwritten by the next writeNext() call.
	 */
	public int readCurrent();
}
