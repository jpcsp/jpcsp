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

import static jpcsp.Allegrex.Common._a0;
import static jpcsp.Allegrex.Common._a1;
import static jpcsp.Allegrex.Common.gprNames;
import static jpcsp.HLE.Modules.sceSasCoreModule;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.memory.mmio.MMIOHandlerMeCore;
import jpcsp.memory.mmio.MMIOHandlerMeCore.MECommand;

public class sceMeCore extends HLEModule {
	public static Logger log = Modules.getLogger("sceMeCore");

	private static String logParameters(CpuState cpu, int firstParameter, int numberParameters) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < numberParameters; i++) {
			int reg = firstParameter + i;
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(String.format("%s=0x%08X", gprNames[reg], cpu.getRegister(reg)));
		}

		return s.toString();
	}

	@HLEFunction(nid = 0x635397BB, version = 150)
	public int sceMeCore_driver_635397BB(CpuState cpu, int cmd) {
		return sceMeCore_driver_FA398D71(cpu, cmd);
	}

	// Sending a command to the Media Engine (ME) processor
	@HLEFunction(nid = 0xFA398D71, version = 150)
	public int sceMeCore_driver_FA398D71(CpuState cpu, int cmd) {
		MMIOHandlerMeCore.MECommand meCommand = MECommand.getMECommand(cmd);
		int numberOfParameters = MECommand.getNumberOfParameters(cmd);
		int result = 0;

		if (meCommand == null) {
			log.error(String.format("sceMeCore_driver_FA398D71 unknown cmd=0x%X, %s", cmd, logParameters(cpu, _a0, numberOfParameters)));
		} else {
			int sasCore;
			switch (meCommand) {
				case ME_CMD_SASCORE:
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceMeCore_driver_FA398D71 cmd=%s, %s", meCommand, logParameters(cpu, _a1, numberOfParameters - 1)));
					}
					sasCore = cpu._a1;
					sceSasCoreModule.copySasCoreToME(sasCore);
					result = sceSasCoreModule.__sceSasCore(sasCore, cpu._a2);
					sceSasCoreModule.copyMEToSasCore(sasCore);
					break;
				case ME_CMD_SASCORE_WITH_MIX:
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceMeCore_driver_FA398D71 cmd=%s, %s", meCommand, logParameters(cpu, _a1, numberOfParameters - 1)));
					}
					sasCore = cpu._a1;
					sceSasCoreModule.copySasCoreToME(sasCore);
					result = sceSasCoreModule.__sceSasCoreWithMix(sasCore, cpu._a2, cpu._t0, cpu._t1);
					sceSasCoreModule.copyMEToSasCore(sasCore);
					break;
				default:
					log.warn(String.format("sceMeCore_driver_FA398D71 unknown cmd=%s, %s", meCommand, logParameters(cpu, _a1, numberOfParameters - 1)));
					break;
			}
		}

		return result;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x905A7500, version = 150)
	public int sceMeCore_driver_905A7500() {
		return 0;
	}
}
