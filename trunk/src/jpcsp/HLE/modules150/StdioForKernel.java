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

import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class StdioForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("StdioForKernel");

    @Override
    public String getName() {
        return "StdioForKernel";
    }

    @HLEFunction(nid = 0xCAB439DF, version = 150)
    public int StdioForKernel_printf(CpuState cpu, PspString formatString) {
    	return Modules.SysMemUserForUserModule.hleKernelPrintf(cpu, formatString, log, "StdioForKernel_printf");
    }
}
