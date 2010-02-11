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
package jpcsp.util;

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

public class Hash {

	/**
	 * Generate a hashCode on a memory range using a rather simple but fast method.
	 * 
	 * @param hashCode		current hashCode value
	 * @param addr			start of the memory range to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @return updated hashCode value
	 */
	public static int getHashCode(int hashCode, int addr, int lengthInBytes) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 4);
		for (int i = 0; i < lengthInBytes; i += 4) {
			int value = memoryReader.readNext();
			hashCode ^= value + i + addr;
			hashCode += i + addr;
		}

		return hashCode;
	}

	/**
	 * Generate a hashCode on a memory range using a more complex but slower method.
	 * 
	 * @param hashCode		current hashCode value
	 * @param addr			start of the memory range to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @return updated hashCode value
	 */
	public static int getHashCodeComplex(int hashCode, int addr, int lengthInBytes) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 4);
		int n = lengthInBytes / 4;
		for (int i = 0; i < n; i++) {
			int value = memoryReader.readNext();
			value = Integer.rotateLeft(value, i & 31);
			hashCode ^= value + i + addr;
			hashCode += i + addr;
		}

		return hashCode;
	}
}
