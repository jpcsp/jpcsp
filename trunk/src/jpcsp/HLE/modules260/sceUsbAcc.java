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
package jpcsp.HLE.modules260;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceUsbAcc extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsbAcc");

	@Override
	public String getName() {
		return "sceUsbAcc";
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x0CD7D4AA, version = 260)
	public int sceUsbAccGetInfo(TPointer resultAddr) {
		// resultAddr is pointing to an 8-byte area.
		// Not sure about the content...
		resultAddr.clear(8);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x79A1C743, version = 260)
	public int sceUsbAccGetAuthStat() {
		return 0;
	}
}
