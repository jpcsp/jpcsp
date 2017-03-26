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

public class DmacManForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("DmacManForKernel");

    @HLEUnimplemented
	@HLEFunction(nid = 0x59615199, version = 150)
	public int sceKernelDmaOpAlloc() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x745E19EF, version = 150)
	public int sceKernelDmaOpFree() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xF64BAB99, version = 150)
	public int sceKernelDmaOpAssign() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x3BDEA96C, version = 150)
	public int sceKernelDmaOpEnQueue() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5AF32783, version = 150)
	public int sceKernelDmaOpQuit() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x92700CCD, version = 150)
	public int sceKernelDmaOpDeQueue() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xCE467D9B, version = 150)
	public int sceKernelDmaOpSetupNormal() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD0358BE9, version = 150)
	public int sceKernelDmaOpSetCallback() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xDB286D65, version = 150)
	public int sceKernelDmaOpSync() {
    	return 0;
	}
}
