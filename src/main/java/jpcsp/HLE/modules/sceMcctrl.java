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

public class sceMcctrl extends HLEModule {
    public static Logger log = Modules.getLogger("sceMcctrl");

    // Init function?
    @HLEUnimplemented
    @HLEFunction(nid = 0x3EF531DB, version = 150)
    public int sceMcctrl_3EF531DB() {
    	// Has no parameters
    	return 0;
    }

    // Term function?
    @HLEUnimplemented
    @HLEFunction(nid = 0x877CD3A5, version = 150)
    public int sceMcctrl_877CD3A5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1EDFD6BB, version = 150)
    public int sceMcctrl_1EDFD6BB(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=288, usage=Usage.out) TPointer unknown1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=176, usage=Usage.out) TPointer unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7CAC25B2, version = 150)
    public int sceMcctrl_7CAC25B2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9618EE57, version = 150)
    public int sceMcctrl_9618EE57() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x61550814, version = 150)
    public int sceMcctrl_61550814() {
    	return 0;
    }
}
