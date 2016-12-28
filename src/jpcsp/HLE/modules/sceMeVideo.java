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

public class sceMeVideo extends HLEModule {
    public static Logger log = Modules.getLogger("sceMeVideo");

    // Called by sceVideocodecOpen
	@HLEUnimplemented
	@HLEFunction(nid = 0xC441994C, version = 150)
	public int sceMeVideo_driver_C441994C(int type, TPointer buffer) {
		return 0;
	}

	// Called by sceVideocodecInit
	@HLEUnimplemented
	@HLEFunction(nid = 0xE8CD3C75, version = 150)
	public int sceMeVideo_driver_E8CD3C75(int type, TPointer buffer) {
		return 0;
	}

	// Called by sceVideocodecGetVersion (=> unknown == 3)
	// Called by sceVideocodecSetMemory (=> unknown == 1)
	@HLEUnimplemented
	@HLEFunction(nid = 0x6D68B223, version = 150)
	public int sceMeVideo_driver_6D68B223(int type, int unknown, TPointer buffer) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x21521BE5, version = 150)
	public int sceMeVideo_driver_21521BE5() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x4D78330C, version = 150)
	public int sceMeVideo_driver_4D78330C() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8768915D, version = 150)
	public int sceMeVideo_driver_8768915D() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8DD56014, version = 150)
	public int sceMeVideo_driver_8DD56014() {
		return 0;
	}
}
