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
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelTls;

@HLELogging
public class Kernel_Library extends jpcsp.HLE.modules380.Kernel_Library {
	@HLEFunction(nid = 0xFA835CDE, version = 620)
	public int sceKernel_FA835CDE(int uid) {
		SceKernelTls tls = Modules.ThreadManForUserModule.getKernelTls(uid);
		if (tls == null) {
			return 0;
		}

		int addr = tls.getTlsAddress();
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernel_FA835CDE returning 0x%08X", addr));
		}

		return addr;
	}
}
