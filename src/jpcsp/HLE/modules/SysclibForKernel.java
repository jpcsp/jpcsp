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

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.nativeCode.AbstractNativeCodeSequence;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

public class SysclibForKernel extends HLEModule {
	public static Logger log = Modules.getLogger("SysclibForKernel");

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
		destAddr.getMemory().memmove(destAddr.getAddress(), srcAddr.getAddress(), size);
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
}
