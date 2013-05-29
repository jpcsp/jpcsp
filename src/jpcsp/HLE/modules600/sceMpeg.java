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
package jpcsp.HLE.modules600;

import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;
import static jpcsp.HLE.modules150.SysMemUserForUser.USER_PARTITION_ID;

import jpcsp.Processor;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;

@HLELogging
public class sceMpeg extends jpcsp.HLE.modules250.sceMpeg {
	// Not sure about the real size.
	public static final int AVC_ES_BUF_SIZE = 0x2000;

	@HLEUnimplemented
    @HLEFunction(nid = 0x63B9536A, version = 600)
    public int sceMpegAvcResourceGetAvcDecTopAddr(int unknown) {
        // Unknown value, passed to sceMpegCreate(ddttop)
        return 0x12345678;
    }

    @HLEFunction(nid = 0x8160A2FE, version = 600)
    public int sceMpegAvcResourceFinish() {
    	if (avcEsBuf != null) {
    		Modules.SysMemUserForUserModule.free(avcEsBuf);
    		avcEsBuf = null;
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAF26BB01, version = 600)
    public int sceMpegAvcResourceGetAvcEsBuf() {
    	if (avcEsBuf == null) {
    		log.warn(String.format("sceMpegAvcResourceGetAvcEsBuf avcEsBuf not allocated"));
    		return -1;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegAvcResourceGetAvcEsBuf returning 0x%08X", avcEsBuf.addr));
    	}

    	return avcEsBuf.addr;
    }

    @HLELogging(level="warn")
    @HLEFunction(nid = 0xFCBDB5AD, version = 600)
    public int sceMpegAvcResourceInit(int unknown) {
        if (unknown != 1) {
        	return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

    	avcEsBuf = Modules.SysMemUserForUserModule.malloc(USER_PARTITION_ID, "sceMpegAvcEsBuf", PSP_SMEM_High, AVC_ES_BUF_SIZE, 0);
    	if (avcEsBuf != null) {
    		Processor.memory.memset(avcEsBuf.addr, (byte) 0, avcEsBuf.size);
    	}

    	return 0;
    }
}