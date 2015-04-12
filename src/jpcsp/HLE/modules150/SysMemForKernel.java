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
package jpcsp.HLE.modules150;

import jpcsp.Allegrex.compiler.nativeCode.AbstractNativeCodeSequence;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.PspString;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class SysMemForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("SysMemForKernel");

    @Override
    public String getName() {
        return "SysMemForKernel";
    }

    @HLEFunction(nid = 0xA089ECA4, version = 150)
    public int sceKernelMemset(TPointer destAddr, int data, int size) {
        destAddr.memset((byte) data, size);

        return 0;
    }

    @HLEFunction(nid = 0x10F3BB61, version = 150)
    public int SysclibForKernel_memset(@CanBeNull TPointer destAddr, int data, int size) {
    	if (destAddr.isNotNull()) {
    		destAddr.memset((byte) data, size);
    	}

        return 0;
    }

    @HLEFunction(nid = 0xEC6F1CF2, version = 150)
    public int SysclibForKernel_strcpy(@CanBeNull TPointer destAddr, @CanBeNull TPointer srcAddr) {
    	if (destAddr.isNotNull() && srcAddr.isNotNull()) {
    		AbstractNativeCodeSequence.strcpy(destAddr.getAddress(), srcAddr.getAddress());
    	}

        return 0;
    }

    @HLEFunction(nid = 0xC0AB8932, version = 150)
    public int SysclibForKernel_strcmp(@CanBeNull TPointer src1Addr, @CanBeNull TPointer src2Addr) {
		return AbstractNativeCodeSequence.strcmp(src1Addr.getAddress(), src2Addr.getAddress());
    }
	
	@HLEFunction(nid = 0x52DF196C, version = 150)
    public int SysclibForKernel_strlen(@CanBeNull PspString srcAddr) {
		return AbstractNativeCodeSequence.getStrlen(srcAddr.getAddress());
    }
}
