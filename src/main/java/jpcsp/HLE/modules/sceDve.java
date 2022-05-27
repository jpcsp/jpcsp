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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class sceDve extends HLEModule {
    public static Logger log = Modules.getLogger("sceDve");

    @HLEUnimplemented
    @HLEFunction(nid = 0xDEB2F80C, version = 150)
    public int sceDve_driver_DEB2F80C(int u) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x93828323, version = 150)
    public int sceDve_driver_93828323(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0B85524C, version = 150)
    public int sceDve_driver_0B85524C(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA265B504, version = 150)
    public int sceDve_driver_A265B504(int x, int y, int z) {
        return 0;
    }
}
