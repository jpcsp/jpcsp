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

import static jpcsp.HLE.modules.sceAtrac3plus.atracDecodeDelay;
import static jpcsp.HLE.modules.sceMp3.ID3;
import static jpcsp.HLE.modules.sceMp3.isMp3Magic;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned32;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.mp3.Mp3Decoder;
import jpcsp.media.codec.mp3.Mp3Header;
import jpcsp.util.Utilities;

public class sceAudiocodec extends HLEModule {
	public static Logger log = Modules.getLogger("sceAudiocodec");
    public static final int audiocodecBufferSize = 108;

	public static final int PSP_CODEC_AT3PLUS = 0x00001000;
	public static final int PSP_CODEC_AT3     = 0x00001001;
	public static final int PSP_CODEC_MP3     = 0x00001002;
	public static final int PSP_CODEC_AAC     = 0x00001003;
	public static final int PSP_CODEC_WMA     = 0x00001005;

	public static final int AUDIOCODEC_AT3P_UNKNOWN_52 = 44100;
	public static final int AUDIOCODEC_AT3P_UNKNOWN_60 = 2;
	public static final int AUDIOCODEC_AT3P_UNKNOWN_64 = 0x2E8;

	public static class AudiocodecInfo {
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

		public void release() {
			setCodecInitialized(false);
		}

		public void initCodec(int codecType) {
			codec = CodecFactory.getCodec(codecType);
			setCodecInitialized(false);
		}
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
			info = null;
		}

		switch (codecType) {
			case PSP_CODEC_AT3:
			case PSP_CODEC_AT3PLUS:
			case PSP_CODEC_AAC:
			case PSP_CODEC_MP3:
				break;
			default:
				log.warn(String.format("sceAudiocodecInit unimplemented codecType=0x%X", codecType));
				return -1;
		}
		info = new AudiocodecInfo(workArea.getAddress());
		info.outputChannels = outputChannels;
		info.initCodec(codecType);

		infos.put(workArea.getAddress(), info);

		workArea.setValue32(8, 0); // error field

		return 0;
	}

	public void initCodec(int id, int codecType, int inputBufferSize, int channels, int outputChannels, int codingMode) {
		AudiocodecInfo info = infos.remove(id);
		if (info != null) {
			info.release();
		}

		info = new AudiocodecInfo(id);
		info.outputChannels = outputChannels;
		info.initCodec(codecType);
		infos.put(id, info);

		ICodec codec = info.getCodec();
		codec.init(inputBufferSize, channels, info.outputChannels, codingMode);
		info.setCodecInitialized();
	}

	public ICodec getCodec(int id) {
		AudiocodecInfo info = infos.get(id);
		if (info == null) {
			return null;
		}

		return info.getCodec();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9D3F790C, version = 150)
	public int sceAudiocodecCheckNeedMem(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea, int codecType) {
		workArea.setValue32(0, 0x05100601);

		switch (codecType) {
			case PSP_CODEC_AT3:
				workArea.setValue32(16, 0x3DE0);
				break;
			case PSP_CODEC_AT3PLUS:
				workArea.setValue32(16, 0x7BC0);
				workArea.setValue32(52, AUDIOCODEC_AT3P_UNKNOWN_52);
				workArea.setValue32(60, AUDIOCODEC_AT3P_UNKNOWN_60);
				workArea.setValue32(64, AUDIOCODEC_AT3P_UNKNOWN_64);
				break;
			case PSP_CODEC_MP3:
				break;
			case PSP_CODEC_AAC:
				workArea.setValue32(16, 0x658C);
				break;
		}

		return 0;
	}

	@HLEFunction(nid = 0x5B37EB1D, version = 150)
	public int sceAudiocodecInit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea, int codecType) {
		// Same as sceAudiocodec_3DD7EE1A, but for stereo audio
		return hleAudiocodecInit(workArea, codecType, 2);
	}

	public static int getOutputBufferSize(TPointer workArea, int codecType) {
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

	private void setMp3Header(TPointer workArea, Mp3Header mp3Header) {
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
			log.debug(String.format("Mp3 header bitrateIndex=%d, rawSampleRateIndex=%d, type=%d", mp3Header.bitrateIndex, mp3Header.rawSampleRateIndex, type));
		}
	}

	private int findMp3Header(TPointer workArea, int inputBuffer) {
        Memory mem = Memory.getInstance();
        // Skip the ID3 tags
    	if ((readUnaligned32(mem, inputBuffer) & 0x00FFFFFF) == ID3) {
    		int size = endianSwap32(readUnaligned32(mem, inputBuffer + 6));
    		// Highest bit of each byte has to be ignored (format: 0x7F7F7F7F)
    		size = (size & 0x7F) | ((size & 0x7F00) >> 1) | ((size & 0x7F0000) >> 2) | ((size & 0x7F000000) >> 3);
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Skipping ID3 of size 0x%X", size));
    		}
    		inputBuffer += 10 + size;
    	}

    	return inputBuffer;
	}

	@HLEFunction(nid = 0x70A703F8, version = 150)
	public int sceAudiocodecDecode(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea, int codecType) {
		workArea.setValue32(8, 0); // err field

		AudiocodecInfo info = infos.get(workArea.getAddress());
		if (info == null) {
			log.warn(String.format("sceAudiocodecDecode no info available for workArea=%s", workArea));
			return -1;
		}

		int inputBuffer = workArea.getValue32(24);
		int outputBuffer = workArea.getValue32(32);
		int unknown1 = workArea.getValue32(40);
		int codingMode = 0;
		int channels = info.outputChannels;

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
				switch (workArea.getValue32(40)) {
					case 0x4: inputBufferSize = 0x180; break;
					case 0x6: inputBufferSize = 0x130; break;
					case 0xB: inputBufferSize =  0xC0; codingMode = 1; break; // JOINT_STEREO
					case 0xE: inputBufferSize =  0xC0; break;
					case 0xF: inputBufferSize =  0x98; channels = 1; break; // MONO
					default:
						log.warn(String.format("sceAudiocodecDecode Atrac3 unknown value 0x%X at offset 40", workArea.getValue32(40)));
						inputBufferSize = 0x180;
						break;
				}
				break;
			case PSP_CODEC_MP3:
				inputBufferSize = workArea.getValue32(40);
				inputBuffer = findMp3Header(workArea, inputBuffer);
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

		ICodec codec = info.getCodec();
    	if (!info.isCodecInitialized()) {
    		codec.init(inputBufferSize, channels, info.outputChannels, codingMode);
    		info.setCodecInitialized();
    	}

		if (codec == null) {
			log.warn(String.format("sceAudiocodecDecode no codec available for codecType=0x%X", codecType));
			return -1;
		}

		Memory mem = workArea.getMemory();
		int bytesConsumed = codec.decode(mem, inputBuffer, inputBufferSize, mem, outputBuffer);
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
				setMp3Header(workArea, mp3Header);
			}
		}

		workArea.setValue32(28, bytesConsumed > 0 ? bytesConsumed : inputBufferSize);

		Modules.ThreadManForUserModule.hleKernelDelayThread(atracDecodeDelay, false);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8ACA11D5, version = 150)
	public int sceAudiocodecGetInfo(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea, int codecType) {
		switch (codecType) {
			case PSP_CODEC_MP3:
				workArea.setValue32(60, 3); // Unknown value, required by sceMp3Init from libmp3.prx
	            Memory mem = Memory.getInstance();
	    		int inputBuffer = workArea.getValue32(24);
	    		inputBuffer = findMp3Header(workArea, inputBuffer);
	        	int header = readUnaligned32(mem, inputBuffer);

	        	if (!isMp3Magic(header)) {
	        		log.error(String.format("Invalid Mp3 header 0x%08X", header));
	        	} else {
	        		header = Utilities.endianSwap32(header);
	        		if (log.isDebugEnabled()) {
	        			log.debug(String.format("Mp3 header: 0x%08X", header));
	        		}

	        		Mp3Header mp3Header = new Mp3Header();
	        		Mp3Decoder.decodeHeader(mp3Header, header);
	        		setMp3Header(workArea, mp3Header);
	        	}
				break;
		}
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x59176A0F, version = 150)
	public int sceAudiocodecAlcExtendParameter(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea, int codecType, @BufferInfo(usage=Usage.out) TPointer32 sizeAddr) {
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

	@HLEFunction(nid = 0x3DD7EE1A, version = 150)
	public int sceAudiocodec_3DD7EE1A(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=audiocodecBufferSize, usage=Usage.inout) TPointer workArea, int codecType) {
		// Same as sceAudiocodecInit, but for mono audio
		return hleAudiocodecInit(workArea, codecType, 1);
	}
}
