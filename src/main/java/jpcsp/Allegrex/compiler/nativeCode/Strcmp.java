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

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class Strcmp extends AbstractNativeCodeSequence {
	static public void call() {
		int str1 = getGprA0();
		int str2 = getGprA1();
		if (str1 == 0 || str2 == 0) {
			if (str1 == str2) {
				setGprV0(0);
			} if (str1 != 0) {
				setGprV0(1);
			} else {
				setGprV0(-1);
			}
		} else {
			if (!Memory.isAddressGood(str1)) {
				getMemory(str1).invalidMemoryAddress(str1, "strcmp", Emulator.EMU_STATUS_MEM_READ);
				return;
			}
			if (!Memory.isAddressGood(str2)) {
				getMemory(str2).invalidMemoryAddress(str2, "strcmp", Emulator.EMU_STATUS_MEM_READ);
				return;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("strcmp src1=%s, src2=%s", Utilities.getMemoryDump(str1, getStrlen(str1)), Utilities.getMemoryDump(str2, getStrlen(str2))));
			}

			setGprV0(strcmp(str1, str2));
		}
	}

	static public void call(int valueEqual, int valueLower, int valueHigher) {
		int str1 = getGprA0();
		int str2 = getGprA1();
		if (str1 == 0 || str2 == 0) {
			if (str1 == str2) {
				setGprV0(valueEqual);
			} if (str1 != 0) {
				setGprV0(valueHigher);
			} else {
				setGprV0(valueLower);
			}
		} else {
			if (!Memory.isAddressGood(str1)) {
				getMemory(str1).invalidMemoryAddress(str1, "strcmp", Emulator.EMU_STATUS_MEM_READ);
				return;
			}
			if (!Memory.isAddressGood(str2)) {
				getMemory(str2).invalidMemoryAddress(str2, "strcmp", Emulator.EMU_STATUS_MEM_READ);
				return;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("strcmp src1=%s, src2=%s", Utilities.getMemoryDump(str1, getStrlen(str1)), Utilities.getMemoryDump(str2, getStrlen(str2))));
			}

			int cmp = strcmp(str1, str2);
			if (cmp < 0) {
				setGprV0(valueLower);
			} else if (cmp > 0) {
				setGprV0(valueHigher);
			} else {
				setGprV0(valueEqual);
			}
		}
	}
}
