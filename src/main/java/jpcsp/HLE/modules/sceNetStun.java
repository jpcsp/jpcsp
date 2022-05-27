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

public class sceNetStun extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetStun");

    @HLEUnimplemented
    @HLEFunction(nid = 0x40971567, version = 150)
    public int sceNetStun_40971567() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4403D9E7, version = 150)
    public int sceNetStun_4403D9E7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6CA54FC9, version = 150)
    public int sceNetStun_6CA54FC9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA0CF3A47, version = 150)
    public int sceNetStun_A0CF3A47() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEBF3C24D, version = 150)
    public int sceNetStun_EBF3C24D(int unknown1, int unknown2, int unknown3, int port, int unknown5, int unknown6, int unknown7) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFBF60C6B, version = 150)
    public int sceNetStun_FBF60C6B() {
    	return 0;
    }
}
