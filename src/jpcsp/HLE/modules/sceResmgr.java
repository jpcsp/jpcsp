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
import jpcsp.HLE.TPointer32;

public class sceResmgr extends HLEModule {
    public static Logger log = Modules.getLogger("sceResmgr");

    @HLEUnimplemented
    @HLEFunction(nid = 0x9DC14891, version = 990)
    public int sceResmgr_9DC14891(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferSize, @BufferInfo(usage=Usage.out) TPointer32 resultLengthAddr) {
    	String result = "release:6.60:\n";
    	result += "build:5455,0,3,1,0:builder@vsh-build6\n";
    	result += "system:57716@release_660,0x06060010:\n";
    	result += "vsh:p6616@release_660,v58533@release_660,20110727:\n";
    	result += "target:1:WorldWide\n";

    	buffer.setStringZ(result);
    	resultLengthAddr.setValue(result.length());

    	return 0;
    }
}
