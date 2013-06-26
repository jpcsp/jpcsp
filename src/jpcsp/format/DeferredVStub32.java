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
package jpcsp.format;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.SceModule;

public class DeferredVStub32 extends DeferredStub {
	public DeferredVStub32(SceModule sourceModule, String moduleName, int importAddress, int nid) {
		super(sourceModule, moduleName, importAddress, nid);
	}

	@Override
	public void resolve(Memory mem, int address) {
		// Perform a R_MIPS_32 relocation

		// Retrieve the current 32bit value
		int value = mem.read32(getImportAddress());

		// Relocate the value
		value += address;

		// Write back the relocated 32bit value
		mem.write32(getImportAddress(), value);
	}

	@Override
	public String toString() {
		return String.format("Reloc32 %s", super.toString());
	}
}
