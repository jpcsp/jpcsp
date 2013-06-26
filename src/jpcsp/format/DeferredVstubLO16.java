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

public class DeferredVstubLO16 extends DeferredStub {

	public DeferredVstubLO16(SceModule sourceModule, String moduleName, int importAddress, int nid) {
		super(sourceModule, moduleName, importAddress, nid);
	}

	@Override
	public void resolve(Memory mem, int address) {
		// Perform a R_MIPS_LO16 relocation

		// Retrieve the current address from lo16
		int loValue = (short) mem.read16(getImportAddress()); // signed 16bit

		// Relocate the address
		loValue += address;

		// Write back the relocation address to hi16 and lo16
		short relocatedLoValue = (short) loValue;
		mem.write16(getImportAddress(), relocatedLoValue);
	}

	@Override
	public String toString() {
		return String.format("LO16 %s", super.toString());
	}
}
