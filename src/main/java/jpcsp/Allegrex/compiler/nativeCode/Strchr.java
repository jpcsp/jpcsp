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

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public class Strchr extends AbstractNativeCodeSequence {
	static public void call() {
		int srcAddr = getGprA0();
		int c1 = getGprA1() & 0xFF;

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr, 1);
		if (memoryReader != null) {
			for (int i = 0; true; i++) {
				int c2 = memoryReader.readNext();
				if (c1 == c2) {
					// Character found
					setGprV0(srcAddr + i);
					return;
				} else if (c2 == 0) {
					// End of Src string found
					break;
				}
			}
		}

		setGprV0(0);
	}
}
