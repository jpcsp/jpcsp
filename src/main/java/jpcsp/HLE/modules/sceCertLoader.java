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
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;

public class sceCertLoader extends HLEModule {
    public static Logger log = Modules.getLogger("sceCertLoader");

    @HLEUnimplemented
    @HLEFunction(nid = 0xDD629A24, version = 150)
    public int sceLoadCertFromFlash(int unknown1, int unknown2, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknown3, int unknown4, int unknown5, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknown6) {
    	unknown3.setValue(0);
    	unknown6.setValue(7100);

    	return 0;
    }
}
