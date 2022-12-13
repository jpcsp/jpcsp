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
import jpcsp.HLE.TPointer;
import jpcsp.Memory;
import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class sceDmac extends HLEModule {
    public static Logger log = Modules.getLogger("sceDmac");

    @HLEFunction(nid = 0x617F3FE6, version = 150)
	@HLEFunction(nid = 0xC7D33466, version = 150, moduleName = "sceDmacplus_driver")
	@HLEFunction(nid = 0x558CDFAB, version = 620, moduleName = "sceDmacplus_driver")
	@HLEFunction(nid = 0x70DB6434, version = 635, moduleName = "sceDmacplus_driver")
	@HLEFunction(nid = 0x2C5FC1E3, version = 660, moduleName = "sceDmacplus_driver")
    public int sceDmacMemcpy(TPointer dest, TPointer source, int size) {
        Memory.getInstance().memcpyWithVideoCheck(dest.getAddress(), source.getAddress(), size);

        return 0;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0xD97F94D8, version = 150)
	@HLEFunction(nid = 0xD57D5BC2, version = 600, moduleName = "sceDmacplus_driver")
	public int sceDmacTryMemcpy() {
    	return 0;
	}
}