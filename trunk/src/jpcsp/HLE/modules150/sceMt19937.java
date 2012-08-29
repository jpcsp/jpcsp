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

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceMt19937 extends HLEModule {
    public static Logger log = Modules.getLogger("sceMt19937");

    @Override
	public String getName() {
		return "sceMt19937";
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0xECF5D379, version = 150)
    public int sceMt19937Init(int unknown1, int unknown2) {
    	return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0xF40C98E6, version = 150)
    public int sceMt19937UInt() {
    	return 0;
    }
}
