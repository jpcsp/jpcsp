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

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public abstract class AbstractNativeCodeSequence implements INativeCodeSequence {
	protected static Logger log = Emulator.log;
	protected static int[] toUpperCase = buildToUpperCase();
	protected static int[] toLowerCase = buildToLowerCase();

	static int[] buildToUpperCase() {
		int[] toUpperCase = new int[256];
		for (int c = 0; c < toUpperCase.length; c++) {
			toUpperCase[c] = (c >= 0x61 && c <= 0x7A) ? c - 32 : c;
		}

		return toUpperCase;
	}

	static int[] buildToLowerCase() {
		int[] toLowerCase = new int[256];
		for (int c = 0; c < toLowerCase.length; c++) {
			toLowerCase[c] = (c >= 0x41 && c <= 0x5A) ? c + 32 : c;
		}

		return toLowerCase;
	}

	static protected CpuState getCpu() {
		return RuntimeContext.cpu;
	}

	static protected Memory getMemory() {
		return RuntimeContext.memory;
	}

	static protected int getRegisterValue(int register) {
		return getCpu().getRegister(register);
	}

	static protected long getLong(int low, int high) {
		return (((long) high) << 32) | (low & 0xFFFFFFFFL);
	}

	static protected int getGprA0() {
		return getCpu()._a0;
	}

	static protected int getGprA1() {
		return getCpu()._a1;
	}

	static protected int getGprA2() {
		return getCpu()._a2;
	}

	static protected int getGprA3() {
		return getCpu()._a3;
	}

	static protected int getGprT0() {
		return getCpu()._t0;
	}

	static protected int getGprT1() {
		return getCpu()._t1;
	}

	static protected int getGprT2() {
		return getCpu()._t2;
	}

	static protected int getGprT3() {
		return getCpu()._t3;
	}

	static protected int getStackParam0() {
		return getMemory().read32(getGprSp());
	}

	static protected int getStackParam1() {
		return getMemory().read32(getGprSp() + 4);
	}

	static protected int getStackParam2() {
		return getMemory().read32(getGprSp() + 8);
	}

	static protected int getGprSp() {
		return getCpu()._sp;
	}

	static protected void setGprV0(int v0) {
		getCpu()._v0 = v0;
	}

	static protected void setGprV0V1(long v0v1) {
		getCpu()._v0 = (int) v0v1;
		getCpu()._v1 = (int) (v0v1 >> 32);
	}

	static protected void setRegisterValue(int register, int value) {
		getCpu().setRegister(register, value);
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

	static protected float getFRegisterValue(int register) {
		return getFpr()[register];
	}

	static public void strcpy(int dstAddr, int srcAddr) {
		int srcLength = getStrlen(srcAddr);
		getMemory().memcpy(dstAddr, srcAddr, srcLength + 1);
	}

	static public int strcmp(int src1Addr, int src2Addr) {
		if (src1Addr == 0) {
			return -1;
		}

		if (src2Addr == 0) {
			return 1;
		}

		IMemoryReader memoryReader1 = MemoryReader.getMemoryReader(src1Addr, 1);
		IMemoryReader memoryReader2 = MemoryReader.getMemoryReader(src2Addr, 1);

		if (memoryReader1 != null && memoryReader2 != null) {
			while (true) {
				int c1 = memoryReader1.readNext();
				int c2 = memoryReader2.readNext();
				if (c1 != c2) {
					return c1 > c2 ? 1 : -1;
				} else if (c1 == 0) {
					// c1 == 0 and c2 == 0
					break;
				}
			}
		}

		return 0;
	}

	static public int getStrlen(int srcAddr) {
		int srcAddr3 = srcAddr & 3;
		// Reading 32-bit values is much faster
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr - srcAddr3, 4);
		if (memoryReader == null) {
			Compiler.log.warn("getStrlen: null MemoryReader");
			return 0;
		}

		int value;
		int offset = 0;
		switch (srcAddr3) {
			case 1:
				value = memoryReader.readNext();
				if ((value & 0x0000FF00) == 0) {
					return 0;
				}
				if ((value & 0x00FF0000) == 0) {
					return 1;
				}
				if ((value & 0xFF000000) == 0) {
					return 2;
				}
				offset = 3;
				break;
			case 2:
				value = memoryReader.readNext();
				if ((value & 0x00FF0000) == 0) {
					return 0;
				}
				if ((value & 0xFF000000) == 0) {
					return 1;
				}
				offset = 2;
				break;
			case 3:
				value = memoryReader.readNext();
				if ((value & 0xFF000000) == 0) {
					return 0;
				}
				offset = 1;
				break;
		}

		// Read 32-bit values and check for a null-byte
		while (true) {
			value = memoryReader.readNext();
			if ((value & 0x000000FF) == 0) {
				return offset;
			}
			if ((value & 0x0000FF00) == 0) {
				return offset + 1;
			}
			if ((value & 0x00FF0000) == 0) {
				return offset + 2;
			}
			if ((value & 0xFF000000) == 0) {
				return offset + 3;
			}
			offset += 4;
		}
	}

	static protected int getStrlen(int srcAddr, int maxLength) {
		int srcAddr3 = srcAddr & 3;
		// Reading 32-bit values is much faster
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr - srcAddr3, 4);
		if (memoryReader == null) {
			Compiler.log.warn("getStrlen: null MemoryReader");
			return 0;
		}

		if (maxLength <= 0) {
			return 0;
		}

		int value;
		int offset = 0;
		switch (srcAddr3) {
			case 1:
				value = memoryReader.readNext();
				if ((value & 0x0000FF00) == 0) {
					return 0;
				}
				if ((value & 0x00FF0000) == 0) {
					return 1;
				}
				if ((value & 0xFF000000) == 0) {
					return Math.min(2, maxLength);
				}
				offset = 3;
				break;
			case 2:
				value = memoryReader.readNext();
				if ((value & 0x00FF0000) == 0) {
					return 0;
				}
				if ((value & 0xFF000000) == 0) {
					return 1;
				}
				offset = 2;
				break;
			case 3:
				value = memoryReader.readNext();
				if ((value & 0xFF000000) == 0) {
					return 0;
				}
				offset = 1;
				break;
		}

		// Read 32-bit values and check for a null-byte
		while (offset < maxLength) {
			value = memoryReader.readNext();
			if ((value & 0x000000FF) == 0) {
				return offset;
			}
			if ((value & 0x0000FF00) == 0) {
				return offset + 1;
			}
			if ((value & 0x00FF0000) == 0) {
				return Math.min(offset + 2, maxLength);
			}
			if ((value & 0xFF000000) == 0) {
				return Math.min(offset + 3, maxLength);
			}
			offset += 4;
		}

		return maxLength;
	}

	static protected int getRelocatedAddress(int address1, int address2) {
		int address = (address1 << 16) + (short) address2;
		return address & Memory.addressMask;
	}
}
