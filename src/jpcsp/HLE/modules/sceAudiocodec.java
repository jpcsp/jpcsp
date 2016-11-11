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

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.mp3.Mp3Decoder;
import jpcsp.media.codec.mp3.Mp3Header;
import jpcsp.util.Utilities;

public class sceAudiocodec extends HLEModule {
	public static Logger log = Modules.getLogger("sceAudiocodec");

	public static final int PSP_CODEC_AT3PLUS = 0x00001000;
	public static final int PSP_CODEC_AT3     = 0x00001001;
	public static final int PSP_CODEC_MP3     = 0x00001002;
	public static final int PSP_CODEC_AAC     = 0x00001003;

	public static abstract class AudiocodecInfo {
		protected ICodec codec;
		protected boolean codecInitialized;

		public ICodec getCodec() {
			return codec;
		}

		public boolean isCodecInitialized() {
			return codecInitialized;
		}

		public void setCodecInitialized(boolean codecInitialized) {
			this.codecInitialized = codecInitialized;
		}

		public void setCodecInitialized() {
			setCodecInitialized(true);
		}

		public abstract void release();
		public abstract void initCodec();
	}

	private int id;
	private AudiocodecInfo info;
	private boolean edramAllocated;

	@Override
	public int getMemoryUsage() {
		return 0x4000;
	}

	@Override
	public void start() {
		id = -1;
		info = null;
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
	public int sceAudiocodecInit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType) {
		if (info != null) {
			info.release();
			info.setCodecInitialized(false);
			info = null;
		}
		id = -1;

		switch (codecType) {
			case PSP_CODEC_AT3:
			case PSP_CODEC_AT3PLUS:
				id = Modules.sceAtrac3plusModule.hleGetAtracID(codecType);
				if (id < 0) {
					return id;
				}
				info = Modules.sceAtrac3plusModule.getAtracID(id);
				break;
			case PSP_CODEC_AAC:
				Modules.sceAacModule.hleAacInit(1);
				id = Modules.sceAacModule.getFreeAacId();
				if (id < 0) {
					return id;
				}
				info = Modules.sceAacModule.getAacInfo(id);
				info.initCodec();
				break;
			case PSP_CODEC_MP3:
				id = Modules.sceMp3Module.getFreeMp3Id();
				if (id < 0) {
					return id;
				}
				info = Modules.sceMp3Module.getMp3Info(id);
				info.initCodec();
				break;
			default:
				log.warn(String.format("sceAudiocodecInit unimplemented codecType=0x%X", codecType));
				return -1;
		}

		workArea.setValue32(8, 0); // error field

		return 0;
	}

	@HLELogging(level = "info")
	@HLEFunction(nid = 0x70A703F8, version = 150)
	public int sceAudiocodecDecode(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType) {
		if (!edramAllocated) {
			return SceKernelErrors.ERROR_INVALID_POINTER;
		}

		workArea.setValue32(8, 0); // err field

		int inputBuffer = workArea.getValue32(24);
		int outputBuffer = workArea.getValue32(32);
		int unknown1 = workArea.getValue32(40);
//		int unknown2 = workArea.getValue32(176);
//		int unknown3 = workArea.getValue32(236);
//		int unknown4 = workArea.getValue32(240);
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

		workArea.setValue32(36, outputBufferSize);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAudiocodecDecode inputBuffer=0x%08X, outputBuffer=0x%08X, inputBufferSize=0x%X, outputBufferSize=0x%X", inputBuffer, outputBuffer, inputBufferSize, outputBufferSize));
			log.debug(String.format("sceAudiocodecDecode unknown1=0x%08X", unknown1));
			if (log.isTraceEnabled()) {
				log.trace(String.format("sceAudiocodecDecode inputBuffer: %s", Utilities.getMemoryDump(inputBuffer, inputBufferSize)));
			}
		}

		ICodec codec = info.getCodec();
    	if (!info.isCodecInitialized()) {
    		codec.init(inputBufferSize, channels, outputChannels, codingMode);
    		info.setCodecInitialized();
    	}

		if (codec == null) {
			log.warn(String.format("sceAudiocodecDecode no codec available for codecType=0x%X", codecType));
			return -1;
		}

		int bytesConsumed = codec.decode(inputBuffer, inputBufferSize, outputBuffer);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAudiocodecDecode bytesConsumed=0x%X", bytesConsumed));
		}

		if (codec instanceof Mp3Decoder) {
			Mp3Header mp3Header = ((Mp3Decoder) codec).getMp3Header();
			if (mp3Header != null) {
				// See https://github.com/uofw/uofw/blob/master/src/avcodec/audiocodec.c
				workArea.setValue32(68, mp3Header.bitrateIndex); // MP3 bitrateIndex [0..14]
				workArea.setValue32(72, mp3Header.rawSampleRateIndex); // MP3 freqType [0..3]

				int type;
				if (mp3Header.mpeg25 != 0) {
					type = 2;
				} else if (mp3Header.lsf != 0) {
					type = 0;
				} else {
					type = 1;
				}
				workArea.setValue32(56, type); // type [0..2]

				if (log.isDebugEnabled()) {
					log.debug(String.format("sceAudiocodecDecode MP3 bitrateIndex=%d, rawSampleRateIndex=%d, type=%d", mp3Header.bitrateIndex, mp3Header.rawSampleRateIndex, type));
				}
			}
		}

		workArea.setValue32(28, bytesConsumed > 0 ? bytesConsumed : inputBufferSize);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8ACA11D5, version = 150)
	public int sceAudiocodecGetInfo(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType) {
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
