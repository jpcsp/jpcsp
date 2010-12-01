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

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class Memset extends AbstractNativeCodeSequence {
	// Memset CodeBlock
	static public void call() {
		int dstAddr = getGprA0();
		int c = getGprA1() & 0xFF;
		int n = getGprA2();

		getMemory().memset(dstAddr, (byte) c, n);

		setGprV0(dstAddr);
	}

	// Memset CodeSequence
	static public void call(int dstAddrReg, int cReg, int nReg) {
		call(dstAddrReg, cReg, nReg, 0);
	}

	// Memset CodeSequence
	static public void call(int dstAddrReg, int cReg, int nReg, int endValue) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int c = gpr[cReg];
		int n = gpr[nReg] - endValue;

		getMemory().memset(dstAddr, (byte) c, n);

		gpr[dstAddrReg] += n;
		gpr[nReg] = endValue;
	}

	/**
	 * Set memory range to a fixed value
	 * @param dstAddrReg	register number containing the start address
	 * @param cReg			register number containing the value
	 * @param nStartReg		register number giving the start value of the counter
	 * @param nEndReg		register number giving the end value of the counter
	 * @param cLength		2: take only the lower 16bit of the value
	 *                      4: take the 32bit of the value
	 */
	static public void call(int dstAddrReg, int cReg, int nStartReg, int cLength, int nEndReg) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int c = gpr[cReg];
		int nStart = gpr[nStartReg];
		int nEnd = gpr[nEndReg];
		int n = nEnd - nStart;

		if (n == 0) {
			return;
		}

		if (cLength == 2) {
			// Both bytes identical?
			if ((c & 0xFF) == ((c >> 8) & 0xFF)) {
				// This is equivalent to a normal memset
				getMemory().memset(dstAddr, (byte) c, n * 2);
			} else {
				// We have currently no built-in memset for 16bit values
				// do it manually...
				Memory mem = getMemory();
				int value32 = (c & 0xFFFF) | (c << 16);
				short value16 = (short) (c & 0xFFFF);
				if (n > 0 && (dstAddr & 3) != 0) {
					mem.write16(dstAddr, value16);
					dstAddr += 2;
					n--;
				}
				IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dstAddr, n * 2, 4);
				for (int i = 0; i < n; i += 2, dstAddr += 4) {
					memoryWriter.writeNext(value32);
				}
				memoryWriter.flush();
				if ((n & 1) != 0) {
					mem.write16(dstAddr, value16);
				}
			}
		} else if (cLength == 4) {
			// All bytes identical?
			if ((c & 0xFF) == ((c >> 8) & 0xFF) && (c & 0xFFFF) == ((c >> 16) & 0xFFFF)) {
				// This is equivalent to a normal memset
				getMemory().memset(dstAddr, (byte) c, n * 4);
			} else {
				IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dstAddr, n * 4, 4);
				for (int i = 0; i < n; i++) {
					memoryWriter.writeNext(c);
				}
				memoryWriter.flush();
			}
		} else {
			Compiler.log.error("Memset.call: unsupported cLength=0x" + Integer.toHexString(cLength));
		}

		gpr[dstAddrReg] += n * cLength;
		gpr[nStartReg] = nEnd;
	}
}
