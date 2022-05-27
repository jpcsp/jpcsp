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
package jpcsp.Allegrex.compiler.nativeCode;

import static jpcsp.Allegrex.Common._ra;
import jpcsp.AllegrexOpcodes;

/**
 * @author gid15
 *
 */
public class PatchCallingJAL extends AbstractNativeCodeSequence {
	/*
	 * This method is very frequently used by the gpSP homebrew.
	 * Try to optimize it to avoid too much recompilations.
	 */
	public static int call(int addressReg) {
		int patchAddr = getRegisterValue(_ra) - 8;

		int jumpAddr = getRegisterValue(addressReg);
		int opcode = (AllegrexOpcodes.JAL << 26) | ((jumpAddr >> 2) & 0x03FFFFFF);
		write32(patchAddr, opcode);

		int delaySlotOpcode = read32(patchAddr + 4);
		interpret(delaySlotOpcode);

		if (log.isDebugEnabled()) {
			log.debug(String.format("PatchCallingJAL at 0x%08X to 0x%08X", patchAddr, jumpAddr));
		}

		return jumpAddr;
	}
}
