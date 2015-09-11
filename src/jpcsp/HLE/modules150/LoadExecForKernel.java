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
package jpcsp.HLE.modules150;

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.util.Utilities;

@HLELogging
public class LoadExecForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("LoadExecForKernel");

	@Override
	public String getName() {
		return "LoadExecForKernel";
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0xA3D5E142, version = 150, checkInsideInterrupt = true)
    public int sceKernelExitVSHVSH(@CanBeNull TPointer param) {
		if (param.isNotNull()) {
			log.info(String.format("sceKernelExitVSHVSH param=%s", Utilities.getMemoryDump(param.getAddress(), 36)));
		}

		return 0;
	}
}
