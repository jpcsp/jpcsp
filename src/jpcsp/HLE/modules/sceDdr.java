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
    @HLEFunction(nid = 0xCF07B9E2, version = 660)
    public int sceDdrChangePllClock(int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x95B9A692, version = 150)
    public int sceDdr_driver_95B9A692() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0DC43DE4, version = 150)
    @HLEFunction(nid = 0x00E36648, version = 660)
    public int sceDdrGetPowerDownCounter() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0BAAE4C5, version = 660)
    public int sceDdr_driver_0BAAE4C5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2AA39A80, version = 660)
    public int sceDdr_driver_2AA39A80() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x91CD8F94, version = 150)
    @HLEFunction(nid = 0x397756C0, version = 660)
    public int sceDdrResetDevice() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3D50DEC9, version = 660)
    public int sceDdr_driver_3D50DEC9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x655A9AB0, version = 150)
    @HLEFunction(nid = 0x4F30BFE8, version = 660)
    public int sceDdrWriteMaxAllocate() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6DE74D80, version = 660)
    public int sceDdr_driver_6DE74D80() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB5694ECD, version = 150)
    @HLEFunction(nid = 0x77CD1FB3, version = 660)
    public int sceDdrExecSeqCmd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD7C6C313, version = 150)
    @HLEFunction(nid = 0x9F882141, version = 660)
    public int sceDdrSetup() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x286E1372, version = 150)
    @HLEFunction(nid = 0xE0A39D3E, version = 660)
    public int sceDdrSetPowerDownCounter() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF1005384, version = 660)
    public int sceDdr_driver_F1005384() {
    	return 0;
    }
}
