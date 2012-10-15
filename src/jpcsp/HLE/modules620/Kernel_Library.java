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
package jpcsp.HLE.modules620;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

import org.apache.log4j.Logger;

@HLELogging
public class Kernel_Library extends jpcsp.HLE.modules380.Kernel_Library {
	public static Logger log = jpcsp.HLE.modules150.Kernel_Library.log;

	@HLEUnimplemented
	@HLEFunction(nid = 0xFA835CDE, version = 620)
	public int sceKernel_FA835CDE(int uid) {
		// Similar to sceKernelGetBlockHeadAddr?
		SysMemInfo info = Modules.SysMemUserForUserModule.getSysMemInfo(uid);
		if (info == null) {
			return 0;
		}

		return info.addr;
	}
}
