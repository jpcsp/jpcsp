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
package jpcsp.memory.mmio;

import static jpcsp.Emulator.getProcessor;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MECODEC_INTR;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;

public class MMIOHandlerMeCore extends MMIOHandlerBase {
	public static final int BASE_ADDRESS = 0xBFC00600;
	public static final int ME_CMD_AT3P_CHECK_NEED_MEM1 = 0x63;
	public static final int ME_CMD_AT3P_CHECK_NEED_MEM2 = 0x66;
	public static final int ME_CMD_AT3_CHECK_NEED_MEM = 0x72;
	public static final int ME_CMD_MALLOC = 0x180;
	public static final int ME_CMD_FREE = 0x181;
	public static final int ME_CMD_CALLOC = 0x182;
	public static final int ME_CMD_AW_EDRAM_BUS_CLOCK_ENABLE = 0x183;
	public static final int ME_CMD_AW_EDRAM_BUS_CLOCK_DISBABLE = 0x184;
	public static final int ME_CMD_BOOT = 0x185;
	private static MMIOHandlerMeCore instance;
	private int cmd;
	private int unknown;
	private final int[] parameters = new int[8];
	private int result;
	private int freeMeMemory = 0x00300000; // ???

	public static MMIOHandlerMeCore getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerMeCore(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerMeCore(int baseAddress) {
		super(baseAddress);
	}

	public void interrupt() {
		Memory mem = Memory.getInstance();
		result = 0;

		switch (cmd) {
			case ME_CMD_AT3P_CHECK_NEED_MEM1:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_CHECK_NEED_MEM1 unknownAddr1=0x%08X, unknownAddr2=0x%08X, unknownAddr3=0x%08X, unknownAddr4=0x%08X", parameters[0], parameters[1], parameters[2], parameters[3]));
				}
				mem.write32(parameters[0], 0); // Unknown value
				mem.write32(parameters[1], -1); // Unknown value
				mem.write32(parameters[2], 0); // Unknown value
				mem.write32(parameters[3], 0); // Unknown value
				break;
			case ME_CMD_AT3P_CHECK_NEED_MEM2:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_CHECK_NEED_MEM2 unknownValue1=0x%X, neededMemAddr=0x%08X, errorAddr=0x%08X", parameters[0], parameters[1], parameters[2], parameters[3]));
				}
				mem.write32(parameters[1], 0); // Needed mem
				mem.write32(parameters[2], 0); // Error
				break;
			case ME_CMD_AT3_CHECK_NEED_MEM:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3_CHECK_NEED_MEM unknown=0x%X, neededMemAddr=0x%08X, errorAddr=0x%08X", parameters[0], parameters[1], parameters[2]));
				}
				mem.write32(parameters[1], 0); // Needed mem
				mem.write32(parameters[2], 0); // Error
				break;
			case ME_CMD_MALLOC: {
				int size = parameters[0];
				result = freeMeMemory;
				freeMeMemory += size;
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_MALLOC size=0x%X returning 0x%06X", size, result));
				}
				break;
			}
			case ME_CMD_FREE: {
				int addr = parameters[0];
				freeMeMemory = addr;
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_FREE addr=0x%06X", addr));
				}
				break;
			}
			case ME_CMD_CALLOC: {
				int num = parameters[0];
				int size = parameters[1];
				result = freeMeMemory;
				freeMeMemory += num * size;
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_CALLOC num=0x%X, size=0x%X returning 0x%06X", num, size, result));
				}
				break;
			}
			case ME_CMD_AW_EDRAM_BUS_CLOCK_ENABLE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AW_EDRAM_BUS_CLOCK_ENABLE"));
				}
				break;
			case ME_CMD_AW_EDRAM_BUS_CLOCK_DISBABLE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AW_EDRAM_BUS_CLOCK_DISBABLE"));
				}
				break;
			case ME_CMD_BOOT:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_BOOT unknown=%b", parameters[0] != 0));
				}
				break;
			default:
				log.warn(String.format("MMIOHandlerMeCore unknown cmd=0x%X", cmd));
				break;
		}
		RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_MECODEC_INTR);
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = cmd; break;
			case 0x04: value = unknown; break;
			case 0x08: value = parameters[0]; break;
			case 0x0C: value = parameters[1]; break;
			case 0x10: value = parameters[2]; break;
			case 0x14: value = parameters[3]; break;
			case 0x18: value = parameters[4]; break;
			case 0x1C: value = parameters[5]; break;
			case 0x20: value = parameters[6]; break;
			case 0x24: value = parameters[7]; break;
			case 0x28: value = result; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", Emulator.getProcessor().cpu.pc, address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: cmd = value; break;
			case 0x04: unknown = value; break;
			case 0x08: parameters[0] = value; break;
			case 0x0C: parameters[1] = value; break;
			case 0x10: parameters[2] = value; break;
			case 0x14: parameters[3] = value; break;
			case 0x18: parameters[4] = value; break;
			case 0x1C: parameters[5] = value; break;
			case 0x20: parameters[6] = value; break;
			case 0x24: parameters[7] = value; break;
			case 0x28: result = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}
}
