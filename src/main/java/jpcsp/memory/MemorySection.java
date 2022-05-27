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

import jpcsp.Memory;

public class MemorySection {
	private int baseAddress;
	private int length;
	private boolean read;
	private boolean write;
	private boolean execute;

	public MemorySection(int baseAddress, int length, boolean read, boolean write, boolean execute) {
		this.baseAddress = baseAddress & Memory.addressMask;
		this.length = length;
		this.read = read;
		this.write = write;
		this.execute = execute;
	}

	public int getBaseAddress() {
		return baseAddress;
	}

	public int getEndAddress() {
		return baseAddress + length - 1;
	}

	public int getLength() {
		return length;
	}

	public boolean canRead() {
		return read;
	}

	public boolean canWrite() {
		return write;
	}

	public boolean canExecute() {
		return execute;
	}

	public boolean contains(int address) {
		address &= Memory.addressMask;
		return getBaseAddress() <= address && address <= getEndAddress();
	}

	@Override
	public String toString() {
		return String.format("MemorySection[0x%08X-0x%08X(length=0x%X) %s%s%s]", getBaseAddress(), getEndAddress(), getLength(), canRead() ? "R" : "", canWrite() ? "W" : "", canExecute() ? "X" : "");
	}
}
