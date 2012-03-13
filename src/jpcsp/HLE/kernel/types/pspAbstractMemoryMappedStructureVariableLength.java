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

/**
 * Abstract class representing a memory based structure starting
 * with a 32-bit value indicating the maximum memory length available
 * for the structure values.
 * 
 * @author gid15
 *
 */
public abstract class pspAbstractMemoryMappedStructureVariableLength extends pspAbstractMemoryMappedStructure {
	private int length;

	@Override
	protected void read() {
		readLength();
	}

	@Override
	protected void write() {
		readLength();
	}

	private void readLength() {
		length = read32();
		setMaxSize(length);
	}

	@Override
	public int sizeof() {
		return length;
	}
}
