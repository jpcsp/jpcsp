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

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Allegrex.compiler.StopThreadException;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class Poll extends AbstractNativeCodeSequence {
	public static void poll1(int address1, int address2) throws StopThreadException {
		int address = getRelocatedAddress(address1, address2);

		int value;
		do {
			value = getMemory().read8(address);
			if (value == 0) {
				if (RuntimeContext.wantSync) {
					RuntimeContext.sync();
				} else {
					Utilities.sleep(1);
				}
			}
		} while (value == 0);

		setGprV0(value);
	}
}
