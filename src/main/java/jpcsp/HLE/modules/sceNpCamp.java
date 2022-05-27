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

public class sceNpCamp extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpCamp");

    // Init function?
    @HLEUnimplemented
    @HLEFunction(nid = 0x486E4110, version = 150)
    public int sceNpCamp_486E4110() {
    	// Has no parameters
    	return 0;
    }

    // Term function?
    @HLEUnimplemented
    @HLEFunction(nid = 0xA2D126CC, version = 150)
    public int sceNpCamp_A2D126CC() {
    	// Has no parameters
    	return 0;
    }

    // Abort function?
    @HLEUnimplemented
    @HLEFunction(nid = 0x72EC7057, version = 150)
    public int sceNpCamp_72EC7057() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x18B9D112, version = 150)
    public int sceNpCamp_18B9D112() {
    	return 0;
    }
}
