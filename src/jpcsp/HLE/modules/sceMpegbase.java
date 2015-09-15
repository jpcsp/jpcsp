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

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;

public class sceMpegbase extends HLEModule {
	public static Logger log = Modules.getLogger("sceMpegbase");

    @HLEUnimplemented
    @HLEFunction(nid = 0xBEA18F91, version = 150)
    public int sceMpegBasePESpacketCopy(TPointer32 packetInfo) {
    	Memory mem = Memory.getInstance();
    	int infoAddr = packetInfo.getAddress();
    	while (infoAddr != 0) {
    		int bufferAddr = mem.read32(infoAddr + 0);
    		int unknown = mem.read32(infoAddr + 4);
    		int nextInfoAddr = mem.read32(infoAddr + 8);
    		int bufferLength = mem.read32(infoAddr + 12) & 0x00000FFF;

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMpegBasePESpacketCopy packet at 0x%08X: bufferAddr=0x%08X, unknown=0x%08X, nextInfoAddr=0x%08X, bufferLength=0x%X", infoAddr, bufferAddr, unknown, nextInfoAddr, bufferLength));
    		}
    		if (bufferLength == 0) {
    			return SceKernelErrors.ERROR_INVALID_SIZE;
    		}
        	Modules.sceMpegModule.addToVideoBuffer(mem, bufferAddr, bufferLength);
        	infoAddr = nextInfoAddr;
    	}

    	Modules.sceMpegModule.hleMpegNotifyRingbufferRead();
    	Modules.sceMpegModule.hleMpegNotifyVideoDecoderThread();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBE45C284, version = 150)
    public int sceMpegBaseYCrCbCopyVme() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7AC0321A, version = 150)
    public int sceMpegBaseYCrCbCopy() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE8EB837, version = 150)
    public int sceMpegBaseCscVme() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x492B5E4B, version = 150)
    public int sceMpegBaseCscInit() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x91929A21, version = 150)
    public int sceMpegBaseCscAvc() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0530BE4E, version = 150)
    public int sceMpegbase_0530BE4E() {
        return 0;
    }
}
