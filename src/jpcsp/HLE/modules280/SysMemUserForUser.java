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
package jpcsp.HLE.modules280;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;

@HLELogging
public class SysMemUserForUser extends jpcsp.HLE.modules200.SysMemUserForUser {
	@HLEUnimplemented
	@HLEFunction(nid = 0x2A3E5280, version = 280)
	public int sceKernelQueryMemoryInfo() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x39F49610, version = 280)
	public int sceKernelGetPTRIG() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6231A71D, version = 280)
	public int sceKernelSetPTRIG() {
		return 0;
	}
}