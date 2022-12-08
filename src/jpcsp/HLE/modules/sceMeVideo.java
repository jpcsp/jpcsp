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

import static jpcsp.HLE.modules.sceVideocodec.videocodecBufferSize;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;

public class sceMeVideo extends HLEModule {
    public static Logger log = Modules.getLogger("sceMeVideo");
    public static final int VIDEOCODEC_OPEN_TYPE0_UNKNOWN24 = 0x3C2C;
    public static final int VIDEOCODEC_OPEN_TYPE0_UNKNOWN0 = 0x1F6400;
    public static final int VIDEOCODEC_OPEN_TYPE0_UNKNOWN4 = 0x15C00;
    public static final int VIDEOCODEC_OPEN_TYPE1_UNKNOWN24 = 0x264C;
    public static final int VIDEOCODEC_OPEN_TYPE1_UNKNOWN32 = 0xB69E3;

    // Called by sceVideocodecOpen
	@HLEUnimplemented
	@HLEFunction(nid = 0xC441994C, version = 150)
	public int sceMeVideo_driver_C441994C(int type, TPointer buffer) {
    	switch (type) {
			case 0:
	        	buffer.setValue32(8, 1);
	        	buffer.setValue32(24, VIDEOCODEC_OPEN_TYPE0_UNKNOWN24);
	        	buffer.setValue32(32, VIDEOCODEC_OPEN_TYPE0_UNKNOWN4);

	        	TPointer buffer2 = buffer.getPointer(16);
	        	buffer2.setValue32(0, VIDEOCODEC_OPEN_TYPE0_UNKNOWN0);
	        	buffer2.setValue32(4, VIDEOCODEC_OPEN_TYPE0_UNKNOWN4);
	        	break;
			case 1:
				buffer.setValue32(8, 0);
	        	buffer.setValue32(24, VIDEOCODEC_OPEN_TYPE1_UNKNOWN24);
	        	buffer.setValue32(32, VIDEOCODEC_OPEN_TYPE1_UNKNOWN32);
				break;
			default:
	    		log.warn(String.format("sceVideocodecOpen unknown type %d", type));
	    		return -1;
		}

    	Modules.sceVideocodecModule.hleVideocodecStartDecoderThread();

    	return 0;
	}

	// Called by sceVideocodecInit
	@HLEUnimplemented
	@HLEFunction(nid = 0xE8CD3C75, version = 150)
	public int sceMeVideo_driver_E8CD3C75(int type, TPointer buffer) {
		return 0;
	}

	// Called by sceVideocodecGetVersion (=> unknown == 3)
	// Called by sceVideocodecSetMemory (=> unknown == 1)
	@HLEUnimplemented
	@HLEFunction(nid = 0x6D68B223, version = 150)
	public int sceMeVideo_driver_6D68B223(int type, int unknown, TPointer buffer) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x21521BE5, version = 150)
	public int sceMeVideo_driver_21521BE5() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x4D78330C, version = 150)
	public int sceMeVideo_driver_4D78330C() {
		return 0;
	}

	// Called by sceVideocodecDecode
	@HLEUnimplemented
	@HLEFunction(nid = 0x8768915D, version = 150)
	public int sceMeVideo_driver_8768915D(int type, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer) {
		return Modules.sceVideocodecModule.hleVideocodecDecode(buffer, type, null);
	}

	// Called by sceVideocodecDelete()
	@HLEUnimplemented
	@HLEFunction(nid = 0x8DD56014, version = 150)
	public int sceMeVideo_driver_8DD56014(int type, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer) {
		return 0;
	}
}
