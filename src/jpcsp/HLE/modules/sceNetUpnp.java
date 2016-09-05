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
}
