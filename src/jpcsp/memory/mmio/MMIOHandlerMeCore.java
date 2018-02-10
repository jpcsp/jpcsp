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

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceMeCore;

public class MMIOHandlerMeCore extends MMIOHandlerBase {
	public static Logger log = sceMeCore.log;
	public static final int BASE_ADDRESS = 0xBFC00600;
	private static MMIOHandlerMeCore instance;
	private int cmd;
	private int unknown;
	private final int[] parameters = new int[8];
	private int result;
	private enum MECommand {
		ME_CMD_VIDEOCODEC_OPEN_TYPE0(0x0, 1),
		ME_CMD_VIDEOCODEC_INIT_TYPE0(0x1, 3),
		ME_CMD_VIDEOCODEC_DECODE_TYPE0(0x2, 8),
		ME_CMD_VIDEOCODEC_STOP_TYPE0(0x3, 3),
		ME_CMD_VIDEOCODEC_DELETE_TYPE0(0x4, 1),
		ME_CMD_VIDEOCODEC_SET_MEMORY_TYPE0(0x5, 6),
		ME_CMD_VIDEOCODEC_GET_VERSION_TYPE0(0x6, 1),
		ME_CMD_AVC_SELECT_CLOCK(0x7, 1),
		ME_CMD_AVC_POWER_ENABLE(0x8, 0),
		ME_CMD_VIDEOCODEC_GET_SEI_TYPE0(0x9, 2),
		ME_CMD_VIDEOCODEC_GET_FRAME_CROP_TYPE0(0xA, 2),
		ME_CMD_VIDEOCODEC_GET_UNKNOWN_TYPE0(0xB, 2),
		ME_CMD_VIDEOCODEC_UNKNOWN_CMD_0x10(0x10, 1),
		ME_CMD_VIDEOCODEC_DECODE_TYPE1(0x20, 4),
		ME_CMD_VIDEOCODEC_STOP_TYPE1(0x21, 1),
		ME_CMD_VIDEOCODEC_DELETE_TYPE1(0x22, 1),
		ME_CMD_VIDEOCODEC_OPEN_TYPE1_STEP2(0x23, 3),
		ME_CMD_VIDEOCODEC_OPEN_TYPE1(0x24, 0),
		ME_CMD_VIDEOCODEC_INIT_TYPE1(0x25, 8),
		ME_CMD_VIDEOCODEC_SCAN_HEADER_TYPE1(0x26, 2),
		ME_CMD_VIDEOCODEC_GET_VERSION_TYPE1(0x27, 8),
		ME_CMD_AT3P_DECODE(0x60, 1),
		ME_CMD_AT3P_GET_INFO3(0x61, 0),
		ME_CMD_AT3P_CHECK_NEED_MEM1(0x63, 4),
		ME_CMD_AT3P_SET_UNK68(0x64, 2),
		ME_CMD_AT3P_CHECK_NEED_MEM2(0x66, 3),
		ME_CMD_AT3P_SETUP_CHANNEL(0x67, 5),
		ME_CMD_AT3P_CHECK_UNK20(0x68, 2),
		ME_CMD_AT3P_SET_UNK44(0x69, 2),
		ME_CMD_AT3P_GET_INTERNAL_ERROR(0x6A, 2),
		ME_CMD_AT3_DECODE(0x70, 1),
		ME_CMD_AT3_GET_INTERNAL_ERROR(0x71, 2),
		ME_CMD_AT3_CHECK_NEED_MEM(0x72, 3),
		ME_CMD_AT3_INIT(0x73, 4),
		ME_CMD_AT3_GET_INFO3(0x74, 0),
		ME_CMD_MP3_GET_INFO3(0x81, 0),
		ME_CMD_MP3_GET_INFO2(0x82, 2),
		ME_CMD_MP3_SET_VALUE_FOR_INFO2(0x89, 2),
		ME_CMD_MP3_CHECK_NEED_MEM(0x8A, 3),
		ME_CMD_MP3_INIT(0x8B, 1),
		ME_CMD_MP3_DECODE(0x8C, 1),
		ME_CMD_AAC_DECODE(0x90, 5),
		ME_CMD_AAC_GET_INTERNAL_ERROR(0x91, 2),
		ME_CMD_AAC_CHECK_NEED_MEM(0x92, 0),
		ME_CMD_AAC_INIT(0x93, 2),
		ME_CMD_AAC_GET_INFO3(0x94, 0),
		ME_CMD_AAC_INIT_UNK44(0x95, 2),
		ME_CMD_AAC_INIT_UNK44_STEP2(0x97, 4),
		ME_CMD_WMA_GET_INFO3(0xE1, 0),
		ME_CMD_WMA_CHECK_NEED_MEM(0xE2, 0),
		ME_CMD_WMA_INIT(0xE3, 2),
		ME_CMD_WMA_DECODE(0xE5, 7),
		ME_CMD_WMA_GET_INTERNAL_ERROR(0xE6, 2),
		ME_CMD_SASCORE_WITH_MIX(0x101, 6),
		ME_CMD_MALLOC(0x180, 1),
		ME_CMD_FREE(0x181, 1),
		ME_CMD_CALLOC(0x182, 2),
		ME_CMD_AW_EDRAM_BUS_CLOCK_ENABLE(0x183, 0),
		ME_CMD_AW_EDRAM_BUS_CLOCK_DISABLE(0x184, 0),
		ME_CMD_BOOT(0x185, 1),
		ME_CMD_CPU(0x186, 2),
		ME_CMD_POWER(0x187, 2),
		ME_CMD_STANDBY(0x18F, 2);

		private int cmd;
		private int numberOfParameters;

		private MECommand(int cmd, int numberOfParameters) {
			this.cmd = cmd;
			this.numberOfParameters = numberOfParameters;
		}

		public int getCmd() {
			return cmd;
		}

		public int getNumberOfParameters() {
			return numberOfParameters;
		}

		public static String getCommandName(int cmd) {
			for (MECommand meCommand : MECommand.values()) {
				if (meCommand.getCmd() == cmd) {
					return meCommand.name();
				}
			}

			return String.format("ME_CMD_UNKNOWN_%X", cmd);
		}

		public static int getNumberOfParameters(int cmd) {
			for (MECommand meCommand : MECommand.values()) {
				if (meCommand.getCmd() == cmd) {
					return meCommand.getNumberOfParameters();
				}
			}

			return 8;
		}
	}

	public static MMIOHandlerMeCore getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerMeCore(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerMeCore(int baseAddress) {
		super(baseAddress);
	}

	private void writeCmd(int cmd) {
		this.cmd = cmd;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Starting cmd=0x%X(%s)", cmd, MECommand.getCommandName(cmd)));
		}
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
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x00: writeCmd(value); break;
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
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder(String.format("cmd=0x%X(%s), result=0x%08X", cmd, MECommand.getCommandName(cmd), result));
		int numberOfParameters = MECommand.getNumberOfParameters(cmd);
		for (int i = 0; i < numberOfParameters; i++) {
			s.append(String.format(", parameters[%d]=0x%08X", i, parameters[i]));
		}
		return s.toString();
	}
}
