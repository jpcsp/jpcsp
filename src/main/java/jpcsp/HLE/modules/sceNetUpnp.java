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

public class sceNetUpnp extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetUpnp");

    @HLEUnimplemented
    @HLEFunction(nid = 0xE24220B5, version = 150)
    public int sceNetUpnpInit(int unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x540491EF, version = 150)
    public int sceNetUpnpTerm() {
    	// No parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3432B2E5, version = 150)
    public int sceNetUpnpStart() {
    	// No parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3E32ED9E, version = 150)
    public int sceNetUpnpStop() {
    	// No parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x27045362, version = 150)
    public int sceNetUpnpGetNatInfo(TPointer unknown) {
    	// Unknown structure of 16 bytes
    	unknown.clear(16);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8513C6D1, version = 150)
    public int sceNetUpnp_8513C6D1(TPointer unknown1, TPointer unknown2, TPointer unknown3) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFDA78483, version = 150)
    public int sceNetUpnp_FDA78483() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1038E77A, version = 150)
    public int sceNetUpnp_1038E77A(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.out) TPointer unknown) {
    	unknown.clear(48);
    	unknown.setValue32(4, 1);

    	return 0;
    }
}
