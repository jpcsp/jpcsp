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
import static jpcsp.Allegrex.Common.gprNames;
import static jpcsp.HLE.Modules.sceSasCoreModule;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.memory.mmio.MMIOHandlerMeCore.MECommand;

public class sceMeCore extends HLEModule {
	public static Logger log = Modules.getLogger("sceMeCore");

	private static String logParameters(int[] parameters, int offset, int numberParameters) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < numberParameters; i++) {
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(String.format("%s=0x%08X", gprNames[i + offset + _a0], parameters[i + offset]));
		}

		return s.toString();
	}

	public int hleMeCore_driver_FA398D71(MECommand cmd) {
		return hleMeCore_driver_FA398D71(Emulator.getProcessor().cpu, cmd);
	}

	// Sending a command to the Media Engine (ME) processor
	public int hleMeCore_driver_FA398D71(CpuState cpu, MECommand cmd) {
		int numberOfParameters = cmd == null ? 8 : cmd.getNumberOfParameters();
		int[] parameters = new int[numberOfParameters];
		for (int i = 0; i < numberOfParameters; i++) {
			parameters[i] = cpu.getRegister(_a0 + i);
		}

		return hleMeCore_driver_FA398D71(cmd, parameters);
	}

	public int hleMeCore_driver_FA398D71(MECommand cmd, int ... parameters) {
		int numberOfParameters = cmd.getNumberOfParameters();
		int result = 0;

		if (cmd == null) {
			log.error(String.format("sceMeCore_driver_FA398D71 unknown cmd=0x%X, %s", cmd, logParameters(parameters, 0, numberOfParameters)));
		} else {
			int sasCore;
			switch (cmd) {
				case ME_CMD_SASCORE:
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceMeCore_driver_FA398D71 cmd=%s, %s", cmd, logParameters(parameters, 1, numberOfParameters - 1)));
					}
					sasCore = parameters[1];
					sceSasCoreModule.copySasCoreToME(sasCore);
					result = sceSasCoreModule.__sceSasCore(sasCore, parameters[2]);
					sceSasCoreModule.copyMEToSasCore(sasCore);
					break;
				case ME_CMD_SASCORE_WITH_MIX:
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceMeCore_driver_FA398D71 cmd=%s, %s", cmd, logParameters(parameters, 1, numberOfParameters - 1)));
					}
					sasCore = parameters[1];
					sceSasCoreModule.copySasCoreToME(sasCore);
					result = sceSasCoreModule.__sceSasCoreWithMix(sasCore, parameters[2], parameters[4], parameters[5]);
					sceSasCoreModule.copyMEToSasCore(sasCore);
					break;
				case ME_CMD_AW_EDRAM_BUS_CLOCK_DISABLE:
				case ME_CMD_AW_EDRAM_BUS_CLOCK_ENABLE:
				case ME_CMD_CPU:
				case ME_CMD_POWER:
				case ME_CMD_VIDEOCODEC_INIT_TYPE0:
				case ME_CMD_VIDEOCODEC_INIT_TYPE1:
				case ME_CMD_VIDEOCODEC_SET_MEMORY_TYPE0:
				case ME_CMD_VIDEOCODEC_GET_VERSION_TYPE0:
				case ME_CMD_VIDEOCODEC_GET_VERSION_TYPE1:
				case ME_CMD_VIDEOCODEC_STOP_TYPE0:
				case ME_CMD_VIDEOCODEC_STOP_TYPE1:
					// Can be ignored
					if (log.isDebugEnabled()) {
						log.debug(String.format("sceMeCore_driver_FA398D71 cmd=%s, %s", cmd, logParameters(parameters, 1, numberOfParameters - 1)));
					}
					break;
				default:
					log.warn(String.format("sceMeCore_driver_FA398D71 unknown cmd=%s, %s", cmd, logParameters(parameters, 1, numberOfParameters - 1)));
					break;
			}
		}

		return result;
	}

	@HLEFunction(nid = 0x635397BB, version = 150)
	public int sceMeCore_driver_635397BB(CpuState cpu, int cmd) {
		return hleMeCore_driver_FA398D71(cpu, MECommand.getMECommand(cmd));
	}

	// Sending a command to the Media Engine (ME) processor
	@HLEFunction(nid = 0xFA398D71, version = 150)
	public int sceMeCore_driver_FA398D71(CpuState cpu, int cmd) {
		return hleMeCore_driver_FA398D71(cpu, MECommand.getMECommand(cmd));
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x905A7500, version = 150)
	public int sceMeCore_driver_905A7500() {
		return 0;
	}
}
