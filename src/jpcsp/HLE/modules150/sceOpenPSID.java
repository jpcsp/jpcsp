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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class sceOpenPSID extends HLEModule {
    public static Logger log = Modules.getLogger("sceOpenPSID");

    @Override
    public String getName() {
        return "sceOpenPSID";
    }

    protected int[] dummyOpenPSID = {0x10, 0x02, 0xA3, 0x44, 0x13, 0xF5, 0x93, 0xB0, 0xCC, 0x6E, 0xD1, 0x32, 0x27, 0x85, 0x0F, 0x9D};

    @HLEFunction(nid = 0xC69BEBCE, version = 150, checkInsideInterrupt = true)
    public int sceOpenPSIDGetOpenPSID(TPointer openPSIDAddr) {
        for (int i = 0; i < dummyOpenPSID.length; i++) {
        	openPSIDAddr.setValue8(i, (byte) dummyOpenPSID[i]);
        }

        return 0;
    }
}