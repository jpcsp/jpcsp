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
import jpcsp.HLE.TPointer;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;

public class sceUsbAcc extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsbAcc");

	@HLEUnimplemented
	@HLEFunction(nid = 0x0CD7D4AA, version = 260)
	public int sceUsbAccGetInfo(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 8, usage = Usage.out) TPointer resultAddr) {
		// resultAddr is pointing to an 8-byte area.
		// Not sure about the content...
		resultAddr.clear(8);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x79A1C743, version = 260)
	public int sceUsbAccGetAuthStat() {
		// Has no parameters
		return 0;
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0x2A100C1F, version = 260)
    public int sceUsbAcc_internal_2A100C1F(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer req) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2E251404, version = 260)
    public int sceUsbAccRegisterType(int type) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x18B04C82, version = 260)
    public int sceUsbAccUnregisterType(int type) {
    	return 0;
    }
}
