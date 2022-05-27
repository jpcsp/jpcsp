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
package jpcsp.memory.mmio.memorystick;

import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;

public class MemoryStickPbr32 extends pspAbstractMemoryMappedStructure {
	public final byte[] bootSector = new byte[96];

	@Override
	protected void read() {
		read8Array(bootSector);
	}

	@Override
	protected void write() {
		write8Array(bootSector);
	}

	@Override
	public int sizeof() {
		return 96;
	}
}
