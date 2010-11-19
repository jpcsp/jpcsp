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

import jpcsp.Allegrex.compiler.Compiler;

/**
 * @author gid15
 *
 */
public class MemcpySequence extends AbstractNativeCodeSequence implements INativeCodeSequence {
	static public void call(int dstAddrReg, int srcAddrReg, int lengthReg) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int srcAddr = gpr[srcAddrReg];
		int length  = gpr[lengthReg];

		getMemory().memcpy(dstAddr, srcAddr, length);

		// Update registers
		gpr[dstAddrReg] = dstAddr + length;
		gpr[srcAddrReg] = srcAddr + length;
		gpr[lengthReg]  = 0;
	}

	static public void call(int dstAddrReg, int srcAddrReg, int lengthReg, int dstOffset, int srcOffset) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int srcAddr = gpr[srcAddrReg];
		int length  = gpr[lengthReg];

		getMemory().memcpy(dstAddr + dstOffset, srcAddr + srcOffset, length);

		// Update registers
		gpr[dstAddrReg] = dstAddr + length;
		gpr[srcAddrReg] = srcAddr + length;
		gpr[lengthReg]  = 0;
	}

	static public void call(int dstAddrReg, int srcAddrReg, int targetAddrReg, int targetReg) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int srcAddr = gpr[srcAddrReg];
		int targetAddr = gpr[targetAddrReg];

		int length = targetAddr - gpr[targetReg];
		getMemory().memcpy(dstAddr, srcAddr, length);

		// Update registers
		gpr[dstAddrReg] = dstAddr + length;
		gpr[srcAddrReg] = srcAddr + length;
	}

	static public void call(int dstAddrReg, int srcAddrReg, int targetAddrReg, int targetReg, int dstOffset, int srcOffset, int valueReg, int valueBytes) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int srcAddr = gpr[srcAddrReg];
		int targetAddr = gpr[targetAddrReg];

		int length = targetAddr - gpr[targetReg];
		getMemory().memcpy(dstAddr + dstOffset, srcAddr + srcOffset, length);

		// Update registers
		gpr[dstAddrReg] = dstAddr + length;
		gpr[srcAddrReg] = srcAddr + length;

		// Update the register "valueReg" with the last value processed by the memcpy loop
		int valueAddr = srcAddr + length - valueBytes;
		int value;
		switch (valueBytes) {
		case 1: value = getMemory().read8(valueAddr);  break;
		case 2: value = getMemory().read16(valueAddr); break;
		case 4: value = getMemory().read32(valueAddr); break;
		default: value = 0; Compiler.log.error("MemcpySequence.call(): Unimplemented valueBytes=" + valueBytes); break;
		}
		gpr[valueReg] = value;
	}

	static public void callWithStep(int dstAddrReg, int srcAddrReg, int lengthReg, int step) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int srcAddr = gpr[srcAddrReg];
		int length  = gpr[lengthReg] * step;

		getMemory().memcpy(dstAddr, srcAddr, length);

		// Update registers
		gpr[dstAddrReg] = dstAddr + length;
		gpr[srcAddrReg] = srcAddr + length;
		gpr[lengthReg]  = 0;
	}

	static public void callWithCountStep(int dstAddrReg, int srcAddrReg, int lengthReg, int count, int step) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int srcAddr = gpr[srcAddrReg];
		int length  = (count - gpr[lengthReg]) * step;

		getMemory().memcpy(dstAddr, srcAddr, length);

		// Update registers
		gpr[dstAddrReg] = dstAddr + length;
		gpr[srcAddrReg] = srcAddr + length;
		gpr[lengthReg]  = count;
	}
}
