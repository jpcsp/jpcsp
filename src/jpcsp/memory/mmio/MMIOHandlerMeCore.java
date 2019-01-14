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

import static jpcsp.HLE.modules.sceAudiocodec.PSP_CODEC_AT3PLUS;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.sceMeCore;
import jpcsp.media.codec.ICodec;
import jpcsp.mediaengine.MEProcessor;
import jpcsp.util.Utilities;

public class MMIOHandlerMeCore {
	public static Logger log = sceMeCore.log;
	public static final int BASE_ADDRESS = 0xBFC00600;
	private static MMIOHandlerMeCore instance;
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
		ME_CMD_SASCORE(0x100, 4),
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
			instance = new MMIOHandlerMeCore();
		}
		return instance;
	}

	private MMIOHandlerMeCore() {
	}

	public void hleStartMeCommand() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Starting %s", this));
		}
	}

	public void hleCompleteMeCommand() {
    	Memory meMemory = MEProcessor.getInstance().getMEMemory();
    	Memory mem = Memory.getInstance();

    	int result;
    	switch (getCmd()) {
			case 0x2: // ME_CMD_VIDEOCODEC_DECODE_TYPE0
		    	int mp4Data = getParameter(1);
		    	int mp4Size = getParameter(2);
		    	TPointer buffer2 = getParameterPointer(3);
            	TPointer mpegAvcYuvStruct = getParameterPointer(4);
            	TPointer buffer3 = getParameterPointer(5);
            	TPointer decodeSEI = getParameterPointer(6);
				result = Modules.sceVideocodecModule.videocodecDecodeType0(meMemory, mp4Data, mp4Size, buffer2, mpegAvcYuvStruct, buffer3, decodeSEI);
				setResult(result);
				break;
			case 0x4: // ME_CMD_VIDEOCODEC_DELETE_TYPE0
				Modules.sceVideocodecModule.videocodecDelete();
				break;
			case 0x67: // ME_CMD_AT3P_SETUP_CHANNEL
				Modules.sceAudiocodecModule.initCodec(getParameter(4), PSP_CODEC_AT3PLUS, getParameter(2) + 2, getParameter(1), getParameter(3), 0);
				break;
			case 0x60: // ME_CMD_AT3P_DECODE
				TPointer workArea = getParameterPointer(0);
				int edram = workArea.getValue32(12);
		    	int inputBufferSize;
				if (workArea.getValue32(48) == 0) {
					inputBufferSize = workArea.getValue32(64) + 2;
				} else {
					inputBufferSize = 0x100A;
				}
				ICodec audioCodec = Modules.sceAudiocodecModule.getCodec(edram);
				int inputBuffer = workArea.getValue32(24);

				// Skip any audio frame header (found in PSMF files)
				if (mem.read8(inputBuffer) == 0x0F && mem.read8(inputBuffer + 1) == 0xD0) {
					int frameHeader23 = (mem.read8(inputBuffer + 2) << 8) | mem.read8(inputBuffer + 3);
					int audioFrameLength = (frameHeader23 & 0x3FF) << 3;
					inputBufferSize = audioFrameLength;
					inputBuffer += 8;
				}

				if (log.isTraceEnabled()) {
					log.trace(String.format("ME_CMD_AT3P_DECODE inputBuffer: %s", Utilities.getMemoryDump(inputBuffer, inputBufferSize)));
				}
				result = audioCodec.decode(meMemory, inputBuffer, inputBufferSize, meMemory, workArea.getValue32(32));
				setResult(result);
				if (log.isDebugEnabled()) {
					if (result < 0) {
						log.debug(String.format("ME_CMD_AT3P_DECODE audiocodec.decode returned error 0x%08X, data: %s", result, Utilities.getMemoryDump(inputBuffer, inputBufferSize)));
					} else {
						log.debug(String.format("ME_CMD_AT3P_DECODE audiocodec.decode bytesConsumed=0x%X", result));
					}
				}
				break;
		}
	}

	private int read32(int offset) {
		return RuntimeContextLLE.getMMIO().read32(BASE_ADDRESS + offset);
	}

	private void write32(int offset, int value) {
		RuntimeContextLLE.getMMIO().write32(BASE_ADDRESS + offset, value);
	}

	public int getCmd() {
		return read32(0x00);
	}

	public int getUnknown() {
		return read32(0x04);
	}

	public int getParameter(int index) {
		return read32(0x08 + (index << 2));
	}

	public TPointer getParameterPointer(int index) {
		return new TPointer(Memory.getInstance(), getParameter(index));
	}

	public int getResult() {
		return read32(0x28);
	}

	public void setResult(int result) {
		write32(0x28, result);
	}

	@Override
	public String toString() {
		int cmd = getCmd();
		StringBuilder s = new StringBuilder(String.format("cmd=0x%X(%s), result=0x%08X", cmd, MECommand.getCommandName(cmd), getResult()));
		int numberOfParameters = MECommand.getNumberOfParameters(cmd);
		for (int i = 0; i < numberOfParameters; i++) {
			s.append(String.format(", parameters[%d]=0x%08X", i, getParameter(i)));
		}
		return s.toString();
	}
}
