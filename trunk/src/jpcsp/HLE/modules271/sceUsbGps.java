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
package jpcsp.HLE.modules271;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceUsbGps extends HLEModule {
	public static Logger log = Modules.getLogger("sceUsbGps");
	public static final int GPS_STATE_OFF = 0;
	// "MapThis!" is describing both following states as "Activating"
	public static final int GPS_STATE_ACTIVATING1 = 1;
	public static final int GPS_STATE_ACTIVATING2 = 2;
	public static final int GPS_STATE_ON = 3;
	protected int gpsState;

	@Override
	public String getName() {
		return "sceUsbGps";
	}

	@Override
	public void start() {
		gpsState = GPS_STATE_OFF;
		super.start();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x268F95CA, version = 271)
	public int sceUsbGpsSetInitDataLocation(long unknown) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x31F95CDE, version = 271)
	public int sceUsbGpsGetPowerSaveMode() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x54D26AA4, version = 271)
	public int sceUsbGpsGetInitDataLocation(TPointer64 unknown) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x63D1F89D, version = 271)
	public int sceUsbGpsResetInitialPosition() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x69E4AAA8, version = 271)
	public int sceUsbGpsSaveInitData() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6EED4811, version = 271)
	public int sceUsbGpsClose() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x7C16AC3A, version = 271)
	public int sceUsbGpsGetState(TPointer32 stateAddr) {
		stateAddr.setValue(gpsState);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x934EC2B2, version = 271)
	public int sceUsbGpsGetData(TPointer gpsDataAddr, TPointer satDataAddr) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9D8F99E8, version = 271)
	public int sceUsbGpsSetPowerSaveMode(int unknown1, int unknown2) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9F267D34, version = 271)
	public int sceUsbGpsOpen() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA259CD67, version = 271)
	public int sceUsbGpsReset(int unknown) {
		return 0;
	}
}
