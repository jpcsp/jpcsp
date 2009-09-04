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

	static public void call(int dstAddrReg, int srcAddrReg, int targetAddrReg, int targetReg, int dstOffset, int srcOffset) {
		int[] gpr = getGpr();
		int dstAddr = gpr[dstAddrReg];
		int srcAddr = gpr[srcAddrReg];
		int targetAddr = gpr[targetAddrReg];

		int length = targetAddr - gpr[targetReg];
		getMemory().memcpy(dstAddr + dstOffset, srcAddr + srcOffset, length);

		// Update registers
		gpr[dstAddrReg] = dstAddr + length;
		gpr[srcAddrReg] = srcAddr + length;
	}
}
