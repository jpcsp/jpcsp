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
public class Memcmp extends AbstractNativeCodeSequence {
	static public void call() {
		int src1Addr = getGprA0();
		int src2Addr = getGprA1();
		int n = getGprA2();

		IMemoryReader memoryReader1 = MemoryReader.getMemoryReader(src1Addr, n, 1);
		IMemoryReader memoryReader2 = MemoryReader.getMemoryReader(src2Addr, n, 1);
		for (int i = 0; i < n; i++) {
			int c1 = memoryReader1.readNext();
			int c2 = memoryReader2.readNext();
			if (c1 != c2) {
				setGprV0(c1 - c2);
				return;
			}
		}

		setGprV0(0);
	}

	static public void call(int src1AddrReg, int src2AddrReg, int n, int resultReg, int equalValue, int notEqualValue) {
		int[] gpr = getGpr();
		int src1Addr = gpr[src1AddrReg];
		int src2Addr = gpr[src2AddrReg];

		IMemoryReader memoryReader1 = MemoryReader.getMemoryReader(src1Addr, n, 4);
		IMemoryReader memoryReader2 = MemoryReader.getMemoryReader(src2Addr, n, 4);
		for (int i = 0; i < n; i += 4) {
			int value1 = memoryReader1.readNext();
			int value2 = memoryReader2.readNext();
			if (value1 != value2) {
				gpr[resultReg] = notEqualValue;
				return;
			}
		}

		gpr[resultReg] = equalValue;
	}
}
