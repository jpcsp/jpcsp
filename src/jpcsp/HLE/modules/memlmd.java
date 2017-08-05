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
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

public class memlmd extends HLEModule {
    public static Logger log = Modules.getLogger("memlmd");

    @HLEUnimplemented
	@HLEFunction(nid = 0x6192F715, version = 660)
	public int memlmd_6192F715(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int size) {
		return 0;
	}

    /*
     * See
     *    https://github.com/uofw/uofw/blob/master/src/loadcore/loadcore.c
     * CheckLatestSubType()
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x9D36A439, version = 660)
	public boolean memlmd_9D36A439(int subType) {
		return true;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xF26A33C3, version = 660)
	public int memlmd_F26A33C3(int unknown, int hardwarePtr) {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xEF73E85B, version = 660)
	public int memlmd_EF73E85B(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int size, @BufferInfo(usage=Usage.out) TPointer32 resultSize) {
    	resultSize.setValue(size);

    	Modules.LoadCoreForKernelModule.decodeInitModuleData(buffer, size, resultSize);

    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xCF03556B, version = 660)
	public int memlmd_CF03556B(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int size, @BufferInfo(usage=Usage.out) TPointer32 resultSize) {
    	// Same as memlmd_EF73E85B?
    	resultSize.setValue(size);

    	Modules.LoadCoreForKernelModule.decodeInitModuleData(buffer, size, resultSize);

    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2F3D7E2D, version = 660)
	public int memlmd_2F3D7E2D() {
    	// Has no parameters
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2AE425D2, version = 660)
	public boolean memlmd_2F3D7E2D(int subType) {
		return true;
	}
}
