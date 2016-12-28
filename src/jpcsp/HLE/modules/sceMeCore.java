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

import static jpcsp.Allegrex.Common._a1;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;

public class sceMeCore extends HLEModule {
	public static Logger log = Modules.getLogger("sceMeCore");

	private String logParameters(CpuState cpu, int firstParameter, int numberParameters) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < numberParameters; i++) {
			int reg = firstParameter + i;
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(String.format("%s=0x%08X", Common.gprNames[reg], cpu.getRegister(reg)));
		}

		return s.toString();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x635397BB, version = 150)
	public int sceMeCore_driver_635397BB() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xFA398D71, version = 150)
	public int sceMeCore_driver_FA398D71(CpuState cpu, int cmd) {
		switch (cmd) {
			case 0x100:	// Called by __sceSasCore
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceMeCore_driver_FA398D71 cmd=0x%X(__sceSasCore), %s", cmd, logParameters(cpu, _a1, 3)));
				}
				break;
			case 0x101: // Called by __sceSasCoreWithMix
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceMeCore_driver_FA398D71 cmd=0x%X(__sceSasCoreWithMix), %s", cmd, logParameters(cpu, _a1, 5)));
				}
				break;
			default:
				log.warn(String.format("sceMeCore_driver_FA398D71 unknown cmd=0x%X, %s", cmd, logParameters(cpu, _a1, 7)));
		}
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x905A7500, version = 150)
	public int sceMeCore_driver_905A7500() {
		return 0;
	}
}
