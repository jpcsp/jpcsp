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
public class Strcasecmp extends AbstractNativeCodeSequence {
	static public void call() {
		int src1Addr = getGprA0();
		int src2Addr = getGprA1();

		IMemoryReader memoryReader1 = MemoryReader.getMemoryReader(src1Addr, 1);
		IMemoryReader memoryReader2 = MemoryReader.getMemoryReader(src2Addr, 1);

		if (memoryReader1 != null && memoryReader2 != null) {
			while (true) {
				int c1 = toLowerCase[memoryReader1.readNext()];
				int c2 = toLowerCase[memoryReader2.readNext()];
				if (c1 != c2) {
					setGprV0(c1 - c2);
					return;
				} else if (c1 == 0) {
					// c1 == 0 and c2 == 0
					break;
				}
			}
		}

		setGprV0(0);
	}
}
