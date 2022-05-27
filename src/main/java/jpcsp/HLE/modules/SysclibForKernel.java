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
package jpcsp.HLE.modules;

import static jpcsp.Allegrex.Common._a2;
import static jpcsp.Allegrex.Common._a3;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.nativeCode.AbstractNativeCodeSequence;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

public class SysclibForKernel extends HLEModule {
	public static Logger log = Modules.getLogger("SysclibForKernel");
	private static final String validNumberCharactersUpperCase = "0123456789ABCDEF";
	private static final String validNumberCharactersLowerCase = "0123456789abcdef";

    @HLEFunction(nid = 0x10F3BB61, version = 150)
    public int memset(@CanBeNull TPointer destAddr, int data, int size) {
    	if (destAddr.isNotNull()) {
    		destAddr.memset((byte) data, size);
    	}

        return 0;
    }

    @HLEFunction(nid = 0xEC6F1CF2, version = 150)
    public int strcpy(@CanBeNull TPointer destAddr, @CanBeNull TPointer srcAddr) {
    	if (destAddr.isNotNull() && srcAddr.isNotNull()) {
    		AbstractNativeCodeSequence.strcpy(destAddr.getAddress(), srcAddr.getAddress());
    	}

        return 0;
    }

    @HLEFunction(nid = 0xC0AB8932, version = 150)
    public int strcmp(@CanBeNull TPointer src1Addr, @CanBeNull TPointer src2Addr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("strcmp '%s', '%s'", Utilities.readStringZ(src1Addr.getAddress()), Utilities.readStringZ(src2Addr.getAddress())));
    	}
		return AbstractNativeCodeSequence.strcmp(src1Addr.getAddress(), src2Addr.getAddress());
    }

    @HLEFunction(nid = 0x52DF196C, version = 150)
    public int strlen(@CanBeNull TPointer srcAddr) {
    	return AbstractNativeCodeSequence.getStrlen(srcAddr.getAddress());
    }

	@HLEFunction(nid = 0x81D0D1F7, version = 150)
    public int memcmp(TPointer src1Addr, TPointer src2Addr, int size) {
		int result = 0;

		if (size > 0) {
			IMemoryReader memoryReader1 = MemoryReader.getMemoryReader(src1Addr.getAddress(), size, 1);
			IMemoryReader memoryReader2 = MemoryReader.getMemoryReader(src2Addr.getAddress(), size, 1);
			for (int i = 0; i < size; i++) {
				int c1 = memoryReader1.readNext();
				int c2 = memoryReader2.readNext();
				if (c1 != c2) {
					result = c1 < c2 ? -1 : 1;
					break;
				}
			}
		}

		return result;
	}

	@HLEFunction(nid = 0xAB7592FF, version = 150)
    public int memcpy(@CanBeNull TPointer destAddr, TPointer srcAddr, int size) {
		if (destAddr.isNotNull() && destAddr.getAddress() != srcAddr.getAddress()) {
			destAddr.getMemory().memcpyWithVideoCheck(destAddr.getAddress(), srcAddr.getAddress(), size);
		}

		return destAddr.getAddress();
    }

	@HLEFunction(nid = 0x7AB35214, version = 150)
    public int strncmp(@CanBeNull TPointer src1Addr, TPointer src2Addr, int size) {
		int result = 0;
		if (size > 0) {
			IMemoryReader memoryReader1 = MemoryReader.getMemoryReader(src1Addr.getAddress(), size, 1);
			IMemoryReader memoryReader2 = MemoryReader.getMemoryReader(src2Addr.getAddress(), size, 1);

			if (memoryReader1 != null && memoryReader2 != null) {
				for (int i = 0; i < size; i++) {
					int c1 = memoryReader1.readNext();
					int c2 = memoryReader2.readNext();
					if (c1 != c2) {
						result = c1 - c2;
						break;
					} else if (c1 == 0) {
						// c1 == 0 and c2 == 0
						break;
					}
				}
			}
		}
		return result;
    }

	@HLEFunction(nid = 0xA48D2592, version = 150)
    public int memmove(@CanBeNull TPointer destAddr, TPointer srcAddr, int size) {
		if (destAddr.isNotNull() && destAddr.getAddress() != srcAddr.getAddress()) {
			destAddr.getMemory().memmove(destAddr.getAddress(), srcAddr.getAddress(), size);
		}
		return 0;
    }

	@HLEFunction(nid = 0x7661E728, version = 150)
    public int sprintf(CpuState cpu, TPointer buffer, String format) {
		String formattedString = Modules.SysMemUserForUserModule.hleKernelSprintf(cpu, format, _a2);
		Utilities.writeStringZ(buffer.getMemory(), buffer.getAddress(), formattedString);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sprintf returning '%s'", formattedString));
		}

		return formattedString.length();
    }

	/**
	 * Returns a pointer to the first occurrence of s2 in s1, or a null pointer if s2 is not part of s1.
	 * The matching process does not include the terminating null-characters, but it stops there.
	 * @param  s1 string to be scanned
	 * @param  s2 string containing the sequence of characters to match
	 * @return a pointer to the first occurrence in s1 or the entire sequence of characters specified in s2,
	 *         or a null pointer if the sequence is not present in s1.
	 */
	@HLEFunction(nid = 0x0D188658, version = 150)
    public int strstr(PspString s1, PspString s2) {
		int index = s1.getString().indexOf(s2.getString());
		if (index < 0) {
			return 0;
		}
		return s1.getAddress() + index;
    }

	@HLEFunction(nid = 0x476FD94A, version = 150)
    public int strcat(@CanBeNull TPointer destAddr, @CanBeNull TPointer srcAddr) {
		if (destAddr.isNull() || srcAddr.isNull()) {
			return 0;
		}

		int dstLength = AbstractNativeCodeSequence.getStrlen(destAddr.getAddress());
		int srcLength = AbstractNativeCodeSequence.getStrlen(srcAddr.getAddress());
		destAddr.memcpy(dstLength, srcAddr.getAddress(), srcLength + 1);

        return destAddr.getAddress();
    }

	@HLEFunction(nid = 0xC2145E80, version = 150)
    public int snprintf(CpuState cpu, TPointer buffer, int n, String format) {
		String formattedString = Modules.SysMemUserForUserModule.hleKernelSprintf(cpu, format, _a3);
		if (formattedString.length() >= n) {
			formattedString = formattedString.substring(0, n - 1);
		}

		Utilities.writeStringZ(buffer.getMemory(), buffer.getAddress(), formattedString);

		if (log.isDebugEnabled()) {
			log.debug(String.format("snprintf returning '%s'", formattedString));
		}

		return formattedString.length();
    }

	private boolean isNumberValidCharacter(int c, int base) {
		if (base > validNumberCharactersUpperCase.length()) {
			base = validNumberCharactersUpperCase.length();
		}

		if (validNumberCharactersUpperCase.substring(0, base).indexOf(c) >= 0) {
			return true;
		}

		if (validNumberCharactersLowerCase.substring(0, base).indexOf(c) >= 0) {
			return true;
		}

		return false;
	}

	@HLEFunction(nid = 0x47DD934D, version = 150)
    public int strtol(@CanBeNull PspString string, @CanBeNull TPointer32 endString, int base) {
		// base == 0 seems to be handled as base == 10.
		if (base == 0) {
			base = 10;
		}

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(string.getAddress(), 1);
		String s = string.getString();

		// Skip any leading "0x" in case of base 16
		if (base == 16 && (s.startsWith("0x") || s.startsWith("0X"))) {
			memoryReader.skip(2);
			s = s.substring(2);
		}

		for (int i = 0; true; i++) {
			int c = memoryReader.readNext();
			if (c == 0 || !isNumberValidCharacter(c, base)) {
				endString.setValue(string.getAddress() + i);
				s = s.substring(0, i);
				break;
			}
		}

		int result;
		if (s.length() == 0) {
			result = 0;
		} else {
			result = Integer.parseInt(s, base);
		}

		if (log.isDebugEnabled()) {
			if (base == 10) {
				log.debug(String.format("strtol on '%s' returning %d", s, result));
			} else {
				log.debug(String.format("strtol on '%s' returning 0x%X", s, result));
			}
		}

		return result;
    }

	@HLEFunction(nid = 0x6A7900E1, version = 150)
    public int strtoul(@CanBeNull PspString string, @CanBeNull TPointer32 endString, int base) {
		// Assume same as strtol
		return strtol(string, endString, base);
    }

	@HLEFunction(nid = 0xB49A7697, version = 150)
    public int strncpy(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer destAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer srcAddr, int size) {
    	int srcLength = AbstractNativeCodeSequence.getStrlen(srcAddr.getAddress());
		if (srcLength < size) {
			destAddr.memcpy(srcAddr.getAddress(), srcLength + 1);
			destAddr.clear(srcLength + 1, size - srcLength - 1);
		} else {
			destAddr.memcpy(srcAddr.getAddress(), size);
		}

		return destAddr.getAddress();
    }

	@HLEFunction(nid = 0x7DEE14DE, version = 150)
    public long __udivdi3(long a, long b) {
		return a / b;
    }

	@HLEFunction(nid = 0x5E8E5F42, version = 150)
    public long __umoddi3(long a, long b) {
		return a % b;
    }

	@HLEFunction(nid = 0xB1DC2AE8, version = 150)
    public int strchr(PspString string, int c) {
		int index = string.getString().indexOf(c);
		if (index < 0) {
			return 0;
		}

		return string.getAddress() + index;
    }

	@HLEFunction(nid = 0x32C767F2, version = 150)
    public int look_ctype_table(int c) {
		return Modules.sceNetModule.sceNetLook_ctype_table(c);
    }

	@HLEFunction(nid = 0x3EC5BBF6, version = 150)
    public int tolower(int c) {
		return Modules.sceNetModule.sceNetTolower(c);
    }

	@HLEFunction(nid = 0xCE2F7487, version = 150)
    public int toupper(int c) {
		return Modules.sceNetModule.sceNetToupper(c);
    }

	@HLEUnimplemented
	@HLEFunction(nid = 0x87C78FB6, version = 150)
    public int prnt() {
		return 0;
    }

	@HLEFunction(nid = 0x4C0E0274, version = 150)
    public int strrchr(PspString string, int c) {
		int index = string.getString().lastIndexOf(c);
		if (index < 0) {
			return 0;
		}

		return string.getAddress() + index;
    }

	@HLEFunction(nid = 0x86FEFCE9, version = 150)
    public void bzero(@CanBeNull TPointer destAddr, int size) {
		memset(destAddr, 0, size);
    }

	@HLEFunction(nid = 0x90C5573D, version = 150)
    public int strnlen(@CanBeNull TPointer srcAddr, int size) {
		if (srcAddr.isNull() || size == 0) {
			return 0;
		}

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr.getAddress(), size, 1);
		for (int i = 0; i < size; i++) {
			int c = memoryReader.readNext();
			if (c == 0) {
				return i;
			}
		}

		return size;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x1AB53A58, version = 150)
    public int strtok_r() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x097049BD, version = 150)
    public int bcopy() {
		return 0;
	}
}
