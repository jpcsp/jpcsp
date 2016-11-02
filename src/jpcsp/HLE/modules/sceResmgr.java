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

import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;

public class sceResmgr extends HLEModule {
    public static Logger log = Modules.getLogger("sceResmgr");

//    @HLEUnimplemented
//    @HLEFunction(nid = 0x9DC14891, version = 990)
//    public int sceResmgr_9DC14891(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer buffer, int bufferSize, @BufferInfo(usage=Usage.out) TPointer32 resultAddr) {
//    	resultAddr.setValue(0);
//
//    	return 0;
//    }
}
