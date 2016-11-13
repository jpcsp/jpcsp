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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer8;

import org.apache.log4j.Logger;

public class sceHttpStorage extends HLEModule {
    public static Logger log = Modules.getLogger("sceHttpStorage");

    @HLEUnimplemented
    @HLEFunction(nid = 0x04EF00F8, version = 150)
    public int sceHttpStorage_04EF00F8(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.out) TPointer8 psCode) {
    	return Modules.sceChkregModule.sceChkregGetPsCode(psCode);
    }
}
