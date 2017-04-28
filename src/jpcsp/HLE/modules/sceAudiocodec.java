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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
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
		protected final int id;
		protected int outputChannels = 2; // Always default with 2 output channels

		protected AudiocodecInfo(int id) {
			this.id = id;
		}

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

	private Map<Integer, AudiocodecInfo> infos;
	private SysMemInfo edramInfo;

	@Override
	public void start() {
		infos = new HashMap<Integer, sceAudiocodec.AudiocodecInfo>();
		edramInfo = null;

		super.start();
	}

	private int hleAudiocodecInit(TPointer workArea, int codecType, int outputChannels) {
		AudiocodecInfo info = infos.remove(workArea.getAddress());
		if (info != null) {
			info.release();
			info.setCodecInitialized(false);
			info = null;
		}

		int id;
		switch (codecType) {
			case PSP_CODEC_AT3:
			case PSP_CODEC_AT3PLUS:
				id = Modules.sceAtrac3plusModule.hleGetAtracID(codecType);
				if (id < 0) {
					return id;
				}
				info = Modules.sceAtrac3plusModule.getAtracID(id);
				info.outputChannels = outputChannels;
				break;
			case PSP_CODEC_AAC:
				Modules.sceAacModule.hleAacInit(1);
				id = Modules.sceAacModule.getFreeAacId();
				if (id < 0) {
					return id;
				}
				info = Modules.sceAacModule.getAacInfo(id);
				info.outputChannels = outputChannels;
				info.initCodec();
				break;
			case PSP_CODEC_MP3:
				id = Modules.sceMp3Module.getFreeMp3Id();
				if (id < 0) {
					return id;
				}
				info = Modules.sceMp3Module.getMp3Info(id);
				info.outputChannels = outputChannels;
				info.initCodec();
				break;
			default:
				log.warn(String.format("sceAudiocodecInit unimplemented codecType=0x%X", codecType));
				return -1;
		}

		infos.put(workArea.getAddress(), info);

		workArea.setValue32(8, 0); // error field

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9D3F790C, version = 150)
	public int sceAudiocodecCheckNeedMem(TPointer workArea, int codecType) {
		workArea.setValue32(0, 0x05100601);

		switch (codecType) {
			case PSP_CODEC_AT3:
				workArea.setValue32(16, 0x3DE0);
				break;
			case PSP_CODEC_AT3PLUS:
				workArea.setValue32(16, 0x7BC0);
				workArea.setValue32(52, 44100);
				workArea.setValue32(60, 2);
				workArea.setValue32(64, 0x2E8);
				break;
			case PSP_CODEC_MP3:
				break;
			case PSP_CODEC_AAC:
				workArea.setValue32(16, 0x658C);
				break;
		}

		return 0;
	}

	@HLELogging(level = "info")
	@HLEFunction(nid = 0x5B37EB1D, version = 150)
	public int sceAudiocodecInit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType) {
		// Same as sceAudiocodec_3DD7EE1A, but for stereo audio
		return hleAudiocodecInit(workArea, codecType, 2);
	}

	private int getOutputBufferSize(TPointer workArea, int codecType) {
		int outputBufferSize;
		switch (codecType) {
			case PSP_CODEC_AT3PLUS:
				if (workArea.getValue32(56) == 1 && workArea.getValue32(72) != workArea.getValue32(56)) {
					outputBufferSize = 0x2000;
				} else {
					outputBufferSize = workArea.getValue32(72) << 12;
				}
				break;
			case PSP_CODEC_AT3:
				outputBufferSize = 0x1000;
				break;
			case PSP_CODEC_MP3:
				if (workArea.getValue32(56) == 1) {
					outputBufferSize = 0x1200;
				} else {
					outputBufferSize = 0x900;
				}
				break;
			case PSP_CODEC_AAC:
				if (workArea.getValue8(45) == 0) {
					outputBufferSize = 0x1000;
				} else {
					outputBufferSize = 0x2000;
				}
				break;
			case 0x1004:
				outputBufferSize = 0x1200;
				break;
			default:
				outputBufferSize = 0x1000;
		}

		return outputBufferSize;
	}

	@HLEFunction(nid = 0x70A703F8, version = 150)
	public int sceAudiocodecDecode(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType) {
		workArea.setValue32(8, 0); // err field

		int inputBuffer = workArea.getValue32(24);
		int outputBuffer = workArea.getValue32(32);
		int unknown1 = workArea.getValue32(40);
		int codingMode = 0; // TODO How to find out the correct value?

		int inputBufferSize;
		switch (codecType) {
			case PSP_CODEC_AT3PLUS:
				if (workArea.getValue32(48) == 0) {
					inputBufferSize = workArea.getValue32(64) + 2;
				} else {
					inputBufferSize = 0x100A;
				}

				// Skip any audio frame header (found in PSMF files)
				Memory mem = workArea.getMemory();
				if (mem.read8(inputBuffer) == 0x0F && mem.read8(inputBuffer + 1) == 0xD0) {
					int frameHeader23 = (mem.read8(inputBuffer + 2) << 8) | mem.read8(inputBuffer + 3);
					int audioFrameLength = (frameHeader23 & 0x3FF) << 3;
					inputBufferSize = audioFrameLength;
					inputBuffer += 8;
				}
				break;
			case PSP_CODEC_AT3:
				inputBufferSize = workArea.getValue32(40) == 6 ? 0x130 : 0x180;
				break;
			case PSP_CODEC_MP3:
				inputBufferSize = workArea.getValue32(40);
				break;
			case PSP_CODEC_AAC:
				if (workArea.getValue8(44) == 0) {
					inputBufferSize = 0x600;
				} else {
					inputBufferSize = 0x609;
				}
				break;
			case 0x1004:
				inputBufferSize = workArea.getValue32(40);
				break;
			default:
				return -1;
		}

		int outputBufferSize = getOutputBufferSize(workArea, codecType);
		workArea.setValue32(36, outputBufferSize);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAudiocodecDecode inputBuffer=0x%08X, outputBuffer=0x%08X, inputBufferSize=0x%X, outputBufferSize=0x%X", inputBuffer, outputBuffer, inputBufferSize, outputBufferSize));
			log.debug(String.format("sceAudiocodecDecode unknown1=0x%08X", unknown1));
			if (log.isTraceEnabled()) {
				log.trace(String.format("sceAudiocodecDecode inputBuffer: %s", Utilities.getMemoryDump(inputBuffer, inputBufferSize)));
			}
		}

		AudiocodecInfo info = infos.get(workArea.getAddress());
		if (info == null) {
			log.warn(String.format("sceAudiocodecDecode no info available for workArea=%s", workArea));
			return -1;
		}

		ICodec codec = info.getCodec();
    	if (!info.isCodecInitialized()) {
    		codec.init(inputBufferSize, info.outputChannels, info.outputChannels, codingMode);
    		info.setCodecInitialized();
    	}

		if (codec == null) {
			log.warn(String.format("sceAudiocodecDecode no codec available for codecType=0x%X", codecType));
			return -1;
		}

		int bytesConsumed = codec.decode(inputBuffer, inputBufferSize, outputBuffer);
		if (log.isDebugEnabled()) {
			if (bytesConsumed < 0) {
				log.debug(String.format("codec.decode returned error 0x%08X, data: %s", bytesConsumed, Utilities.getMemoryDump(inputBuffer, inputBufferSize)));
			} else {
				log.debug(String.format("sceAudiocodecDecode bytesConsumed=0x%X", bytesConsumed));
			}
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

		Modules.ThreadManForUserModule.hleKernelDelayThread(sceMpeg.atracDecodeDelay, false);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8ACA11D5, version = 150)
	public int sceAudiocodecGetInfo(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x59176A0F, version = 150)
	public int sceAudiocodecAlcExtendParameter(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType, @BufferInfo(usage=Usage.out) TPointer32 sizeAddr) {
		int outputBufferSize = getOutputBufferSize(workArea, codecType);

		sizeAddr.setValue(outputBufferSize);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x3A20A200, version = 150)
	public int sceAudiocodecGetEDRAM(TPointer workArea, int codecType) {
		int neededMem = workArea.getValue32(16);
		edramInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceAudiocodec-EDRAM", SysMemUserForUser.PSP_SMEM_LowAligned, neededMem, 0x40);
		if (edramInfo == null) {
			return -1;
		}
		workArea.setValue32(12, edramInfo.addr);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x29681260, version = 150)
	public int sceAudiocodecReleaseEDRAM(TPointer workArea) {
		if (edramInfo == null) {
			return SceKernelErrors.ERROR_CODEC_AUDIO_EDRAM_NOT_ALLOCATED;
		}

		Modules.SysMemUserForUserModule.free(edramInfo);
		edramInfo = null;

		AudiocodecInfo info = infos.remove(workArea.getAddress());
		if (info != null) {
			info.release();
			info.setCodecInitialized(false);
		}

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6CD2A861, version = 150)
	public int sceAudiocodec_6CD2A861() {
		return 0;
	}

	@HLELogging(level = "info")
	@HLEFunction(nid = 0x3DD7EE1A, version = 150)
	public int sceAudiocodec_3DD7EE1A(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=108, usage=Usage.inout) TPointer workArea, int codecType) {
		// Same as sceAudiocodecInit, but for mono audio
		return hleAudiocodecInit(workArea, codecType, 1);
	}
}
