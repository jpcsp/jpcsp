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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.util.Utilities;

public class mp4msv extends HLEModule {
    public static Logger log = Modules.getLogger("mp4msv");

    @HLEUnimplemented
    @HLEFunction(nid = 0x3C2183C7, version = 150)
    public int mp4msv_3C2183C7(int unknown, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer addr) {
    	if (addr.isNotNull()) {
    		// addr is pointing to five 32-bit values (20 bytes)
    		log.warn(String.format("mp4msv_3C2183C7 unknown values: %s", Utilities.getMemoryDump(addr.getAddress(), 20, 4, 20)));
    	}

    	// mp4msv_3C2183C7 is called by sceMp4Init
    	Modules.sceMp4Module.hleMp4Init();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9CA13D1A, version = 150)
    public int mp4msv_9CA13D1A(int unknown, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer32 addr) {
    	if (addr.isNotNull()) {
    		// addr is pointing to 17 32-bit values (68 bytes)
    		log.warn(String.format("mp4msv_9CA13D1A unknown values: %s", Utilities.getMemoryDump(addr.getAddress(), 68, 4, 16)));
    	}

    	// mp4msv_9CA13D1A is called by sceMp4Init
    	Modules.sceMp4Module.hleMp4Init();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF595F917, version = 150)
    public int mp4msv_F595F917(@BufferInfo(usage=Usage.out) TPointer32 unknown) {
    	unknown.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3D8D41A0, version = 150)
    public int mp4msv_3D8D41A0(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x67AF9E0F, version = 150)
    public int mp4msv_67AF9E0F(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer unknown1, int unknown2, int unknown3) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x07C60A23, version = 150)
    public int mp4msv_07C60A23(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer unknown1, @BufferInfo(usage=Usage.out) TPointer32 unknown2, @BufferInfo(usage=Usage.out) TPointer32 unknown3) {
    	unknown1.setValue32(0);
    	unknown2.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0D32271B, version = 150)
    public int mp4msv_0D32271B(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer unknown1, @BufferInfo(usage=Usage.out) TPointer32 unknown2) {
    	unknown2.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAD3AF34E, version = 150)
    public int mp4msv_AD3AF34E(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer32 unknown1, int unknown2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.out) TPointer unknown3) {
    	if (unknown1.isNull()) {
    		return 4;
    	}
    	if (unknown1.getValue(0) == 0) {
    		return 4;
    	}
    	TPointer unknown5 = unknown1.getPointer(0);
    	if (unknown5.getValue32(184) == 0) {
    		return 0x2003;
    	}
    	if (unknown2 == 0) {
    		return 6;
    	}
    	if (unknown5.getValue32(220) == 0 || unknown5.getValue32(220) < unknown2) {
    		return 0x2002;
    	}
    	int unknown4 = unknown5.getValue32(232) + 52 * (unknown2 - 1);
    	unknown3.memcpy(unknown4, 40);

    	return 0;
    }
}
