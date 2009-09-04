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
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public abstract class AbstractNativeCodeSequence implements INativeCodeSequence {
	protected static char[] toUpperCase = buildToUpperCase();
	protected static char[] toLowerCase = buildToLowerCase();

	static char[] buildToUpperCase() {
		char[] toUpperCase = new char[256];
		for (int i = 0; i <= 0xFF; i++) {
			toUpperCase[i] = Character.toUpperCase((char) i);
		}

		return toUpperCase;
	}

	static char[] buildToLowerCase() {
		char[] toLowerCase = new char[256];
		for (int i = 0; i <= 0xFF; i++) {
			toLowerCase[i] = Character.toLowerCase((char) i);
		}

		return toLowerCase;
	}

	static protected int[] getGpr() {
		return RuntimeContext.gpr;
	}

	static protected CpuState getCpu() {
		return RuntimeContext.cpu;
	}

	static protected Memory getMemory() {
		return RuntimeContext.memory;
	}

	static protected int getRegisterValue(int register) {
		return getGpr()[register];
	}

	static protected long getLong(int low, int high) {
		return (((long) high) << 32) | low;
	}

	static protected int getGprA0() {
		return getGpr()[4];
	}

	static protected int getGprA1() {
		return getGpr()[5];
	}

	static protected int getGprA2() {
		return getGpr()[6];
	}

	static protected int getGprA3() {
		return getGpr()[7];
	}

	static protected void setGprV0(int v0) {
		getGpr()[2] = v0;
	}

	static protected void setGprV0V1(long v0v1) {
		getGpr()[2] = (int) v0v1;
		getGpr()[3] = (int) (v0v1 >> 32);
	}

	static float[] getFpr() {
		return getCpu().fpr;
	}

	static protected float getFprF12() {
		return getFpr()[12];
	}

	static protected void setFprF0(float f0) {
		getFpr()[0] = f0;
	}

	static protected int getStrlen(int srcAddr) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr, 1);

		if (memoryReader != null) {
			for (int i = 0; true; i++) {
				int c = memoryReader.readNext();
				// End of string found
				if (c == 0) {
					return i;
				}
			}
		} else {
			jpcsp.Allegrex.compiler.Compiler.log.warn("getStrlen: null MemoryReader");
		}

		return 0;
	}
}
