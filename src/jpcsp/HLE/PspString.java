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

import jpcsp.MemoryMap;
import jpcsp.util.Utilities;

public class PspString {
	protected String string;
	protected int address;
	protected int maxLength;

	public PspString(int address) {
		this.string = null;
		this.address = address;
		this.maxLength = MemoryMap.SIZE_RAM; // Never will be greater than the whole PSP memory :P
	}

	public PspString(int address, int maxLength) {
		this.string = null;
		this.address = address;
		this.maxLength = maxLength;
	}

	public String getString() {
		if (string == null) {
			string = Utilities.readStringNZ(address, maxLength);
		}
		return string;
	}

	public int getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return getString();
	}
}
