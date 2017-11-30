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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MECODEC_INTR;
import static jpcsp.HLE.modules.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.HLE.modules.sceAudiocodec.getOutputBufferSize;
import static jpcsp.HLE.modules.sceVideocodec.VIDEOCODEC_OPEN_TYPE0_UNKNOWN0;
import static jpcsp.HLE.modules.sceVideocodec.VIDEOCODEC_OPEN_TYPE0_UNKNOWN24;
import static jpcsp.HLE.modules.sceVideocodec.VIDEOCODEC_OPEN_TYPE0_UNKNOWN4;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.sceAudiocodec;
import jpcsp.HLE.modules.sceMeCore;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

public class MMIOHandlerMeCore extends MMIOHandlerBase {
	public Logger log = sceMeCore.log;
	public static final int BASE_ADDRESS = 0xBFC00600;
	public static final int ME_CMD_VIDEOCODEC_OPEN = 0x0;
	public static final int ME_CMD_AT3P_DECODE = 0x60;
	public static final int ME_CMD_AT3P_CHECK_NEED_MEM1 = 0x63;
	public static final int ME_CMD_AT3P_SET_UNK68 = 0x64;
	public static final int ME_CMD_AT3P_CHECK_NEED_MEM2 = 0x66;
	public static final int ME_CMD_AT3P_SETUP_CHANNEL = 0x67;
	public static final int ME_CMD_AT3P_CHECK_UNK20 = 0x68;
	public static final int ME_CMD_AT3P_SET_UNK44 = 0x69;
	public static final int ME_CMD_AT3_CHECK_NEED_MEM = 0x72;
	public static final int ME_CMD_MALLOC = 0x180;
	public static final int ME_CMD_FREE = 0x181;
	public static final int ME_CMD_CALLOC = 0x182;
	public static final int ME_CMD_AW_EDRAM_BUS_CLOCK_ENABLE = 0x183;
	public static final int ME_CMD_AW_EDRAM_BUS_CLOCK_DISBABLE = 0x184;
	public static final int ME_CMD_BOOT = 0x185;
	public static final int ME_CMD_CPU = 0x186;
	public static final int ME_CMD_POWER = 0x187;
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

	private int decodeDebugIndex = 0;
	private void meAtrac3plusDecode(TPointer workArea) {
		workArea.setValue32(8, 0); // error field
		int outputBufferSize = getOutputBufferSize(workArea, PSP_CODEC_AT3PLUS);
		workArea.setValue32(36, outputBufferSize);

		int inputBufferSize;
		if (workArea.getValue32(48) == 0) {
			inputBufferSize = workArea.getValue32(64) + 2;
		} else {
			inputBufferSize = 0x100A;
		}
		workArea.setValue32(28, inputBufferSize);

		int outputBuffer = workArea.getValue32(32);
		if (log.isDebugEnabled()) {
			log.debug(String.format("Generating dummy audio data starting at 0x%04X", decodeDebugIndex));
			IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(outputBuffer, outputBufferSize, 2);
			for (int i = 0; i < outputBufferSize; i += 2) {
				memoryWriter.writeNext(decodeDebugIndex++);
			}
		} else {
			getMemory().memset(outputBuffer, (byte) 0, outputBufferSize);
		}
	}

	public void interrupt() {
		Memory mem = Memory.getInstance();
		result = 0;

		switch (cmd) {
			case ME_CMD_VIDEOCODEC_OPEN:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_VIDEOCODEC_OPEN unknownAddr=0x%08X", parameters[0]));
				}
				mem.write32(parameters[0] + 0, VIDEOCODEC_OPEN_TYPE0_UNKNOWN0);
				mem.write32(parameters[0] + 4, VIDEOCODEC_OPEN_TYPE0_UNKNOWN4);
				result = VIDEOCODEC_OPEN_TYPE0_UNKNOWN24;
				break;
			case ME_CMD_AT3P_CHECK_NEED_MEM1:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_CHECK_NEED_MEM1 unknownAddr1=0x%08X, unknownAddr2=0x%08X, unknownAddr3=0x%08X, unknownAddr4=0x%08X", parameters[0], parameters[1], parameters[2], parameters[3]));
				}
				mem.write32(parameters[0], sceAudiocodec.AUDIOCODEC_AT3P_UNKNOWN_52);
				mem.write32(parameters[1], sceAudiocodec.AUDIOCODEC_AT3P_UNKNOWN_60);
				mem.write32(parameters[2], sceAudiocodec.AUDIOCODEC_AT3P_UNKNOWN_64);
				mem.write32(parameters[3], 0); // Unknown value
				break;
			case ME_CMD_AT3P_CHECK_NEED_MEM2:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_CHECK_NEED_MEM2 unknownValue1=0x%X, neededMemAddr=0x%08X, errorAddr=0x%08X", parameters[0], parameters[1], parameters[2]));
				}
				mem.write32(parameters[1], 0); // Needed mem
				mem.write32(parameters[2], 0); // Error
				break;
			case ME_CMD_AT3P_SETUP_CHANNEL:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_SETUP_CHANNEL unknownValue1=0x%X, unknownValue2=0x%X, unknownValue3=0x%X, unknownValue4=0x%X, edramAddr=0x%08X", parameters[0], parameters[1], parameters[2], parameters[3], parameters[4]));
				}
				break;
			case ME_CMD_AT3P_SET_UNK44:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_SET_UNK44 unk44Addr=0x%08X, edramAddr=0x%08X", parameters[0], parameters[1]));
				}
				mem.write32(parameters[0], 0); // Unknown value
				break;
			case ME_CMD_AT3P_SET_UNK68:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_SET_UNK68 unk68Addr=0x%08X, edramAddr=0x%08X", parameters[0], parameters[1]));
				}
				mem.write32(parameters[0], 0); // Unknown value
				break;
			case ME_CMD_AT3P_CHECK_UNK20:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_CHECK_UNK20 unk20=0x%X, edramAddr=0x%08X", parameters[0], parameters[1]));
				}
				break;
			case ME_CMD_AT3P_DECODE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_AT3P_DECODE workArea=0x%08X", parameters[0]));
				}
				meAtrac3plusDecode(new TPointer(getMemory(), parameters[0]));
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
			case ME_CMD_CPU:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_CPU numerator=%d, denominator=%d", parameters[0], parameters[1]));
				}
				break;
			case ME_CMD_POWER:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ME_CMD_POWER unknown1=0x%X, unknown2=0x%X", parameters[0], parameters[1]));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerMeCore unknown cmd=0x%X", cmd));
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
