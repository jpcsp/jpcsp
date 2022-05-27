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

		getMemory(dstAddr).memsetWithVideoCheck(dstAddr, (byte) c, n);

		setGprV0(dstAddr);
	}

	// Memset CodeSequence
	static public void call(int dstAddrReg, int cReg, int nReg) {
		call(dstAddrReg, cReg, nReg, 0);
	}

	// Memset CodeSequence
	static public void call(int dstAddrReg, int cReg, int nReg, int endValue) {
		int dstAddr = getRegisterValue(dstAddrReg);
		int c = getRegisterValue(cReg);
		int n = getRegisterValue(nReg) - endValue;

		getMemory(dstAddr).memsetWithVideoCheck(dstAddr, (byte) c, n);

		setRegisterValue(dstAddrReg, dstAddr + n);
		setRegisterValue(nReg, endValue);
	}

	// Memset32 CodeBlock
	static public void callMemset32() {
		int dstAddr = getGprA0();
		int c = getGprA1();
		int n = getGprA2();

		if (n > 0) {
			// All bytes identical?
			if ((c & 0xFF) == ((c >> 8) & 0xFF) && (c & 0xFFFF) == ((c >> 16) & 0xFFFF)) {
				// This is equivalent to a normal memset
				getMemory(dstAddr).memsetWithVideoCheck(dstAddr, (byte) c, n);
			} else {
				IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(getMemory(dstAddr), dstAddr, n, 4);
				for (int i = 0; i < n; i += 4) {
					memoryWriter.writeNext(c);
				}
				memoryWriter.flush();
			}
		}

		setGprV0(dstAddr + n);
	}

	// Memset CodeSequence
	static public void callWithStep(int dstAddrReg, int cReg, int nReg, int endValue, int direction, int step) {
		int dstAddr = getRegisterValue(dstAddrReg);
		int c = getRegisterValue(cReg);
		int n = (endValue - getRegisterValue(nReg)) * direction * step;

		getMemory(dstAddr).memsetWithVideoCheck(dstAddr, (byte) c, n);

		setRegisterValue(dstAddrReg, dstAddr + n);
		setRegisterValue(nReg, endValue);
	}

	// Memset CodeSequence
	static public void callWithStepReg(int dstAddrReg, int cReg, int nReg, int endValueReg, int direction, int step) {
		int endValue = getRegisterValue(endValueReg);
		callWithStep(dstAddrReg, cReg, nReg, endValue, direction, step);
	}
}
