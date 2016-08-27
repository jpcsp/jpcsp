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

public class sceNpCommerce2 extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpCommerce2");

    @HLEUnimplemented
    @HLEFunction(nid = 0x0E9956E3, version = 150)
    public int sceNpCommerce2Init() {
    	// No parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA5A34EA4, version = 150)
    public int sceNpCommerce2Term() {
    	// No parameters
    	return 0;
    }
}
