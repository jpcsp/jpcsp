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
import jpcsp.HLE.TPointer;

public class scePsheet extends HLEModule {
    public static Logger log = Modules.getLogger("scePsheet");
    protected TPointer address;
    protected int size;

    @HLEUnimplemented
    @HLEFunction(nid = 0x302AB4B8, version = 150)
    public int sceDRMInstallInit(TPointer address, int size) {
    	this.address = address;
    	this.size = size;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3CEC4078, version = 150)
    public int sceDRMInstallEnd() {
    	address = TPointer.NULL;
    	size = 0;

    	return 0;
    }
}
