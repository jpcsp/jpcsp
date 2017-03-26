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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceMemab extends HLEModule {
    public static Logger log = Modules.getLogger("sceMemab");

    @HLEUnimplemented
    @HLEFunction(nid = 0x6DD7339A, version = 150)
    public int sceMemab_6DD7339A() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD57856A7, version = 150)
    public int sceMemab_D57856A7() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF742F283, version = 150)
    public int sceMemab_F742F283(int unknown1, int unknown2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer unknown3, int unknown4) {
    	unknown3.clear(16);

    	return 0;
    }
}
