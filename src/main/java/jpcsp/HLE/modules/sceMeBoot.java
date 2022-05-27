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

public class sceMeBoot extends HLEModule {
	public static Logger log = Modules.getLogger("sceMeBoot");

	@HLEUnimplemented
    @HLEFunction(nid = 0x5DFF5C50, version = 150)
    public int sceMeBootStart660(int unknown) {
    	return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0x99E4DBFA, version = 635)
    public int sceMeBootStart635(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x3A2E60BB, version = 620)
    public int sceMeBootStart620(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x051C1601, version = 500)
    public int sceMeBootStart500(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x8988AD49, version = 395)
    public int sceMeBootStart395(int unknown) {
    	return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0xD857CF93, version = 380)
    public int sceMeBootStart380(int unknown) {
    	return 0;
    }
	@HLEUnimplemented
    @HLEFunction(nid = 0xC287AD90, version = 371)
    public int sceMeBootStart371(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x47DB48C2, version = 150)
    public int sceMeBootStart(int unknown) {
    	return 0;
    }
}
