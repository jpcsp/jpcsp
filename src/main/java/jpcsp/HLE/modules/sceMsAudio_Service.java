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
import jpcsp.HLE.TPointer32;

public class sceMsAudio_Service extends HLEModule {
    public static Logger log = Modules.getLogger("sceMsAudio_Service");

    @HLEUnimplemented
    @HLEFunction(nid = 0x543DBDD7, version = 150)
    public int sceMSAudio_driver_543DBDD7() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x19474552, version = 150)
    public int sceMSAudio_driver_19474552(TPointer32 unknown) {
    	unknown.setValue(0);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x59B4EE6D, version = 150)
    public int sceMSAudio_driver_59B4EE6D(int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB7DB5AC6, version = 150)
    public int sceMSAudio_driver_B7DB5AC6() {
    	// Has no parameters
    	return 0;
    }
}
