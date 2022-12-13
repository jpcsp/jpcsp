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

public class sceLcdc extends HLEModule {
    public static Logger log = Modules.getLogger("sceLcdc");

    @HLEUnimplemented
	@HLEFunction(nid = 0xE9DBD35F, version = 660)
	public int sceLcdc_driver_E9DBD35F(int unknown1, int displayWidth, int displayHeight, int unknown2) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xC004BBE0, version = 660)
	public int sceLcdc_driver_C004BBE0() {
    	// Has no parameters
    	return 0;
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0x0BFF31C5, version = 660)
    public int sceLcdc_driver_0BFF31C5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1F01BD08, version = 660)
    public int sceLcdc_driver_1F01BD08() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D80BB2E, version = 660)
    public int sceLcdc_driver_2D80BB2E() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x35F45B50, version = 660)
    public int sceLcdc_driver_35F45B50() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3EDBFCB9, version = 660)
    public int sceLcdc_driver_3EDBFCB9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x451FE1A1, version = 660)
    public int sceLcdc_driver_451FE1A1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x525E7EB1, version = 660)
    public int sceLcdc_driver_525E7EB1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x56307DBC, version = 660)
    public int sceLcdc_driver_56307DBC() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5DF2F52A, version = 660)
    public int sceLcdc_driver_5DF2F52A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x75C4DA9A, version = 660)
    public int sceLcdc_driver_75C4DA9A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x88C3D167, version = 660)
    public int sceLcdc_driver_88C3D167() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x90AA786C, version = 660)
    public int sceLcdc_driver_90AA786C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x94F83F74, version = 660)
    public int sceLcdc_driver_94F83F74() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9D4FB8B1, version = 660)
    public int sceLcdc_driver_9D4FB8B1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA3DCEF64, version = 660)
    public int sceLcdc_driver_A3DCEF64() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBAF165CE, version = 660)
    public int sceLcdc_driver_BAF165CE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBB839185, version = 660)
    public int sceLcdc_driver_BB839185() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC943B9A2, version = 660)
    public int sceLcdc_driver_C943B9A2() {
    	return 0;
    }
}
