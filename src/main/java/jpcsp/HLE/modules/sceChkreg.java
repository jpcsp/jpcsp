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
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer8;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;

public class sceChkreg extends HLEModule {
    public static Logger log = Modules.getLogger("sceChkreg");
    public static final int PS_CODE_JAPAN = 3;
    public static final int PS_CODE_NORTH_AMERICA = 4;
    public static final int PS_CODE_EUROPE = 5;
    public static final int PS_CODE_KOREA = 6;
    public static final int PS_CODE_AUSTRALIA = 9;
    public static final int PS_CODE_HONGKONG = 10;
    public static final int PS_CODE_TAIWAN = 11;
    public static final int PS_CODE_RUSSIA = 12;
    public static final int PS_CODE_CHINA = 13;

    public int getValueReturnedBy6894A027() {
    	return 1; // Fake value
    }

    @HLEFunction(nid = 0x54495B19, version = 150)
    public int sceChkregCheckRegion(int unknown1, int unknown2) {
    	// 0: region is not correct
    	// 1: region is correct
    	return 1;
    }

    @HLEFunction(nid = 0x59F8491D, version = 150)
    public int sceChkregGetPsCode(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.out) TPointer8 psCode) {
    	psCode.setValue(0, 1);
    	psCode.setValue(1, 0);
    	psCode.setValue(2, PS_CODE_EUROPE);
    	psCode.setValue(3, 0);
    	psCode.setValue(4, 1);
    	psCode.setValue(5, 0);
    	psCode.setValue(6, 1);
    	psCode.setValue(7, 0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6894A027, version = 150)
    public int sceChkreg_driver_6894A027(@BufferInfo(usage=Usage.out) TPointer8 unknown1, int unknown2) {
    	unknown1.setValue(getValueReturnedBy6894A027());

    	return 0;
    }

	@HLEUnimplemented
	@HLEFunction(nid = 0x7939C851, version = 150)
	public int sceChkregGetPspModel() {
		// Has no parameters
		return 1;
	}
}
