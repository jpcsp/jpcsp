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
import jpcsp.HLE.TPointer32;

public class sceCodepage extends HLEModule {
    public static Logger log = Modules.getLogger("sceCodepage");

    @HLEUnimplemented
    @HLEFunction(nid = 0xEE932176, version = 150)
    public int sceCodepage_driver_EE932176() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1D0DE569, version = 150)
    public int sceCodepage_driver_1D0DE569(TPointer32 unknown1, TPointer32 unknown2, TPointer32 unknown3, TPointer32 unknown4) {
    	unknown1.setValue(0);
    	unknown2.setValue(0);
    	unknown3.setValue(47880);
    	unknown4.setValue(128);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x039BF9E9, version = 150)
    public int sceCodepage_driver_039BF9E9(TPointer unknown1, int unknown2, TPointer unknown3, int unknown4, TPointer unknown5, int unknown6, TPointer unknown7, int unknown8) {
    	return 0;
    }
}
