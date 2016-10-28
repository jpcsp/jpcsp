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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

public class sceNpInstall extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpInstall");

    @HLEUnimplemented
    @HLEFunction(nid = 0x0B039B36, version = 150)
    public int sceNpInstallActivation() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5847D8C7, version = 150)
    public int sceNpInstallGetChallenge() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7AE4C8BC, version = 150)
    public int sceNpInstallDeactivation() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x91F9D50D, version = 150)
    public int sceNpInstallCheckActivation(@BufferInfo(usage=Usage.out) TPointer32 unknown1, int unknown2) {
    	unknown1.setValue(0);

    	return 0;
    }
}
