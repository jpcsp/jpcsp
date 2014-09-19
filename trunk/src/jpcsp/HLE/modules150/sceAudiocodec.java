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
package jpcsp.HLE.modules150;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.sceAtrac3plus.AtracID;
import jpcsp.media.codec.ICodec;
import jpcsp.util.Utilities;

@HLELogging
public class sceAudiocodec extends HLEModule {
	public static Logger log = Modules.getLogger("sceAudiocodec");

	public static final int PSP_CODEC_AT3PLUS = 0x00001000;
	public static final int PSP_CODEC_AT3     = 0x00001001;
	public static final int PSP_CODEC_MP3     = 0x00001002;
	public static final int PSP_CODEC_AAC     = 0x00001003;

	private AtracID id;
	private int atID;
	private boolean edramAllocated;

	@Override
	public String getName() {
		return "sceAudiocodec";
	}

	@Override
	public void start() {
		id = null;
		edramAllocated = false;

		super.start();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9D3F790C, version = 150)
	public int sceAudiocodecCheckNeedMem(TPointer workArea, int codecType) {
		return 0;
	}

	@HLELogging(level = "info")
	@HLEFunction(nid = 0x5B37EB1D, version = 150)
	public int sceAudiocodecInit(TPointer workArea, int codecType) {
		if (id != null) {
			Modules.sceAtrac3plusModule.hleReleaseAtracID(atID);
			atID = -1;
			id = null;
		}
		atID = Modules.sceAtrac3plusModule.hleGetAtracID(codecType);
		id = Modules.sceAtrac3plusModule.getAtracID(atID);

		return 0;
	}

	@HLELogging(level = "info")
	@HLEFunction(nid = 0x70A703F8, version = 150)
	public int sceAudiocodecDecode(TPointer workArea, int codecType) {
		if (!edramAllocated) {
			return SceKernelErrors.ERROR_INVALID_POINTER;
		}

		int inputBuffer = workArea.getValue32(24);
		int outputBuffer = workArea.getValue32(32);
		int unknown1 = workArea.getValue32(40);
		int unknown2 = workArea.getValue32(176);
		int unknown3 = workArea.getValue32(236);
		int unknown4 = workArea.getValue32(240);
		int channels = 2;		// How to find out correct value?
		int outputChannels = 2;	// How to find out correct value?
		int codingMode = 0;		// How to find out correct value?

		int inputBufferSize;
		int outputBufferSize;
		switch (codecType) {
			case PSP_CODEC_AT3PLUS:
				if (workArea.getValue32(48) == 0) {
					inputBufferSize = workArea.getValue32(64) + 2;
				} else {
					inputBufferSize = 0x100A;
				}
				if (workArea.getValue32(56) == 1 && workArea.getValue32(72) != workArea.getValue32(56)) {
					outputBufferSize = 0x2000;
				} else {
					outputBufferSize = workArea.getValue32(72) << 12;
				}
				break;
			case PSP_CODEC_AT3:
				inputBufferSize = workArea.getValue32(40) == 6 ? 0x130 : 0x180;
				outputBufferSize = 0x1000;
				break;
			case PSP_CODEC_MP3:
				inputBufferSize = workArea.getValue32(40);
				if (workArea.getValue32(56) == 1) {
					outputBufferSize = 0x1200;
				} else {
					outputBufferSize = 0x900;
				}

				// The application (e.g. homebrew "PSP ProQuake")
				// expects double the given outputBufferSize... why?
				outputBufferSize *= 2;
				break;
			case PSP_CODEC_AAC:
				if (workArea.getValue8(44) == 0) {
					inputBufferSize = 0x600;
				} else {
					inputBufferSize = 0x609;
				}
				if (workArea.getValue8(45) == 0) {
					outputBufferSize = 0x1000;
				} else {
					outputBufferSize = 0x2000;
				}
				break;
			case 0x1004:
				inputBufferSize = workArea.getValue32(40);
				outputBufferSize = 0x1200;
				break;
			default:
				return -1;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAudiocodecDecode inputBuffer=0x%08X, outputBuffer=0x%08X, inputBufferSize=0x%X, outputBufferSize=0x%X", inputBuffer, outputBuffer, inputBufferSize, outputBufferSize));
			log.debug(String.format("sceAudiocodecDecode unknown1=0x%08X, unknown2=0x%08X, unknown3=0x%08X, unknown4=0x%08X", unknown1, unknown2, unknown3, unknown4));
			if (log.isTraceEnabled()) {
				log.trace(String.format("sceAudiocodecDecode inputBuffer: %s", Utilities.getMemoryDump(inputBuffer, inputBufferSize)));
			}
		}

        ICodec codec = id.getCodec();

    	if (!id.isCodecInitialized()) {
    		codec.init(inputBufferSize, channels, outputChannels, codingMode);
    		id.setCodecInitialized();
    	}
    	codec.decode(inputBuffer, inputBufferSize, outputBuffer);

		workArea.setValue32(28, inputBufferSize);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8ACA11D5, version = 150)
	public int sceAudiocodecGetInfo() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x59176A0F, version = 150)
	public int sceAudiocodecAlcExtendParameter() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x3A20A200, version = 150)
	public int sceAudiocodecGetEDRAM(TPointer workArea, int codecType) {
		edramAllocated = true;

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x29681260, version = 150)
	public int sceAudiocodecReleaseEDRAM(TPointer workArea) {
		if (!edramAllocated) {
			return SceKernelErrors.ERROR_CODEC_AUDIO_EDRAM_NOT_ALLOCATED;
		}

		edramAllocated = false;

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6CD2A861, version = 150)
	public int sceAudiocodec_6CD2A861() {
		return 0;
	}
}
