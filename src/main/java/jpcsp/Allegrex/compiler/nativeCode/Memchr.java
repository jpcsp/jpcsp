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
public class Memchr extends AbstractNativeCodeSequence {
	static public void call() {
		int srcAddr = getGprA0();
		int c1 = getGprA1() & 0xFF;
		int n = getGprA2();

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr, n, 1);
		if (memoryReader != null) {
			for (int i = 0; i < n; i++) {
				int c2 = memoryReader.readNext();
				if (c1 == c2) {
					setGprV0(srcAddr + i);
					return;
				}
			}
		}

		setGprV0(0);
	}

	// Returns index of char found or "n" if not found
	static public void call(int srcAddrReg, int cReg, int nReg) {
		int srcAddr = getRegisterValue(srcAddrReg);
		int c1 = getRegisterValue(cReg) & 0xFF;
		int n = getRegisterValue(nReg);

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr, n, 1);
		if (memoryReader != null) {
			for (int i = 0; i < n; i++) {
				int c2 = memoryReader.readNext();
				if (c1 == c2) {
					setGprV0(i);
					return;
				}
			}
		}

		setGprV0(n);
	}
}
