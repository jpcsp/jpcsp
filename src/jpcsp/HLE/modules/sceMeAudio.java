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

import static jpcsp.HLE.modules.sceAudiocodec.audiocodecBufferSize;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;

public class sceMeAudio extends HLEModule {
    public static Logger log = Modules.getLogger("sceMeAudio");
	public static final int AUDIOCODEC_AT3P_UNKNOWN_52 = 44100;
	public static final int AUDIOCODEC_AT3P_UNKNOWN_60 = 2;
	public static final int AUDIOCODEC_AT3P_UNKNOWN_64 = 0x2E8;

    // Called by sceAudiocodecCheckNeedMem
	@HLEUnimplemented
	@HLEFunction(nid = 0x81956A0B, version = 150)
	public int sceMeAudio_driver_81956A0B(int codecType, TPointer workArea) {
		int neededMem = 0;

		switch (codecType) {
			case sceAudiocodec.PSP_CODEC_AT3:
				neededMem = 0x3DE0;
				break;
			case sceAudiocodec.PSP_CODEC_AT3PLUS:
				neededMem = 0x7BC0;
				workArea.setValue32(52, AUDIOCODEC_AT3P_UNKNOWN_52);
				workArea.setValue32(60, AUDIOCODEC_AT3P_UNKNOWN_60);
				workArea.setValue32(64, AUDIOCODEC_AT3P_UNKNOWN_64);
				break;
			case sceAudiocodec.PSP_CODEC_MP3:
				break;
			case sceAudiocodec.PSP_CODEC_AAC:
				neededMem = 0x658C;
				break;
		}

		if (neededMem != 0) {
			workArea.setValue32(16, neededMem);
			Modules.sceAudiocodecModule.hleAudiocodecGetEDRAM(workArea);
		}

		return 0;
	}

	// Called by sceAudiocodecInit
	@HLEUnimplemented
	@HLEFunction(nid = 0x6AD33F60, version = 150)
	public int sceMeAudio_driver_6AD33F60(int codecType, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea) {
		return Modules.sceAudiocodecModule.hleAudiocodecInit(workArea, codecType, 2);
	}

	// Called by sceAudiocodecDecode
	@HLEUnimplemented
	@HLEFunction(nid = 0x9A9E21EE, version = 150)
	public int sceMeAudio_driver_9A9E21EE(int codecType, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea) {
		return Modules.sceAudiocodecModule.hleAudiocodecDecode(workArea, codecType);
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB57F033A, version = 150)
	public int sceMeAudio_driver_B57F033A() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xC300D466, version = 150)
	public int sceMeAudio_driver_C300D466() {
		return 0;
	}
}
