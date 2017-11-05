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

public class sceDdr extends HLEModule {
    public static Logger log = Modules.getLogger("sceDdr");

    @HLEUnimplemented
    @HLEFunction(nid = 0x87D86769, version = 150)
    public int sceDdrFlush(int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4CE55E76, version = 150)
    public int sceDdrChangePllClock(int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCF07B9E2, version = 150)
    public int sceDdrChangePllClock_660(int unknown) {
    	return sceDdrChangePllClock(unknown);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x95B9A692, version = 150)
    public int sceDdr_driver_95B9A692() {
    	// Has no parameters
    	return 0;
    }
}
