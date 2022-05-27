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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AA3_INVALID_CODEC;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AA3_INVALID_HEADER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AA3_INVALID_HEADER_FLAGS;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AA3_INVALID_HEADER_VERSION;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_API_FAIL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_BAD_ID;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_BUFFER_IS_EMPTY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_INCORRECT_READ_SIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_INVALID_SIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_NO_ID;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_UNKNOWN_FORMAT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_BUSY;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules.sceAudiocodec.PSP_CODEC_AT3;
import static jpcsp.HLE.modules.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.util.Utilities.readUnaligned32;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspFileBuffer;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.modules.sceAudiocodec.AudiocodecInfo;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.atrac3.Atrac3Decoder;
import jpcsp.media.codec.atrac3plus.Atrac3plusDecoder;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceAtrac3plus extends HLEModule {
    public static Logger log = Modules.getLogger("sceAtrac3plus");

    @Override
    public void start() {
    	for (int i = 0; i < atracIDs.length; i++) {
    		atracIDs[i] = new AtracID(i);
    	}

    	// Tested on PSP:
        // Only 2 atracIDs per format can be registered at the same time.
        // Note: After firmware 2.50, these limits can be changed by sceAtracReinit.
    	hleAtracReinit(2, 2);

        super.start();
    }

    @Override
    public void stop() {
    	if (temporaryDecodeArea != null) {
    		Modules.SysMemUserForUserModule.free(temporaryDecodeArea);
    		temporaryDecodeArea = null;
    	}

    	super.stop();
    }

	@Override
	public int getMemoryUsage() {
		// No need to allocate additional memory when the module has been
		// loaded using sceKernelLoadModuleToBlock()
		// by the PSP "flash0:/kd/utility.prx".
		// The memory has already been allocated in that case.
		if (Modules.ModuleMgrForKernelModule.isMemoryAllocatedForModule("flash0:/kd/libatrac3plus.prx")) {
			return 0;
		}

		return 0x8000;
	}

	public    static final int AT3_MAGIC      = 0x0270; // "AT3"
    public    static final int AT3_PLUS_MAGIC = 0xFFFE; // "AT3PLUS"
    public    static final int RIFF_MAGIC = 0x46464952; // "RIFF"
    public    static final int WAVE_MAGIC = 0x45564157; // "WAVE"
    public    static final int FMT_CHUNK_MAGIC = 0x20746D66; // "FMT "
    protected static final int FACT_CHUNK_MAGIC = 0x74636166; // "FACT"
    protected static final int SMPL_CHUNK_MAGIC = 0x6C706D73; // "SMPL"
    public    static final int DATA_CHUNK_MAGIC = 0x61746164; // "DATA"

    private static final int ATRAC3_CONTEXT_READ_SIZE_OFFSET = 160;
    private static final int ATRAC3_CONTEXT_REQUIRED_SIZE_OFFSET = 164;
    private static final int ATRAC3_CONTEXT_DECODE_RESULT_OFFSET = 188;

    public static final int PSP_ATRAC_ALLDATA_IS_ON_MEMORY = -1;
    public static final int PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY = -2;
    public static final int PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY = -3;

    protected static final int PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA = 0;
    protected static final int PSP_ATRAC_STATUS_LOOP_STREAM_DATA = 1;

    public static final int ATRAC_HEADER_HASH_LENGTH = 512;

    public static final int atracDecodeDelay = 2300; // Microseconds, based on PSP tests

    protected AtracID atracIDs[] = new AtracID[6];

    private static SysMemInfo temporaryDecodeArea;

    protected static class LoopInfo {
    	protected int cuePointID;
    	protected int type;
    	protected int startSample;
    	protected int endSample;
    	protected int fraction;
    	protected int playCount;

    	@Override
		public String toString() {
			return String.format("LoopInfo[cuePointID %d, type %d, startSample 0x%X, endSample 0x%X, fraction %d, playCount %d]", cuePointID, type, startSample, endSample, fraction, playCount);
		}
    }

    public static class AtracFileInfo {
    	public int atracBitrate = 64;
    	public int atracChannels = 2;
    	public int atracSampleRate = 0xAC44;
    	public int atracBytesPerFrame = 0x0230;
    	public int atracEndSample;
    	public int atracSampleOffset;
    	public int atracCodingMode;
    	public int inputFileDataOffset;
    	public int inputFileSize;
    	public int inputDataSize;

    	public int loopNum;
    	public int numLoops;
    	public LoopInfo[] loops;
    }

    public static class AtracID extends AudiocodecInfo {
        // Internal info.
        protected int codecType;
        protected boolean inUse;
        protected int currentReadPosition;
        // Context (used only from firmware 6.00)
        protected SysMemInfo atracContext;
        protected SysMemInfo internalBuffer;
        // Sound data.
        protected AtracFileInfo info;
        protected int atracCurrentSample;
        protected int maxSamples;
        protected int skippedSamples;
        protected int skippedEndSamples;
        private   int startSkippedSamples;
        protected int lastDecodedSamples;
        protected int channels;
        // First buffer.
        protected pspFileBuffer inputBuffer;
        protected boolean reloadingFromLoopStart;
        // Second buffer.
        protected int secondBufferAddr = -1;
        protected int secondBufferSize;
        // Input file.
        protected int secondInputFileSize;
        protected boolean isSecondBufferNeeded;
        protected boolean isSecondBufferSet;
        protected int internalErrorInfo;
        // Loops
        protected int currentLoopNum = -1;
        // LowLevel decoding
        protected int sourceBufferLength;
        // AddStreamData
        protected int getStreamDataInfoCurrentSample;

        public AtracID(int id) {
        	super(id);
        	info = new AtracFileInfo();
        }

        @Override
		public void release() {
        	super.release();
        	setInUse(false);
        	releaseContext();
        	releaseInternalBuffer();
        }

        public int setHalfwayBuffer(int addr, int readSize, int bufferSize, boolean isMonoOutput, AtracFileInfo info) {
        	this.info = info;
        	channels = info.atracChannels;
        	int maxSizeBeforeFirstWrap = bufferSize;
        	int maxSizeAfterFirstWrap = bufferSize;

        	// If the buffer is not already full, try to align it on multiple of atracBytesPerFrame
        	if (readSize < bufferSize) {
        		int maxSizeAligned = bufferSize - ((bufferSize - info.inputFileDataOffset) % info.atracBytesPerFrame);
        		if (maxSizeAligned >= readSize) {
        			maxSizeBeforeFirstWrap = maxSizeAligned;
        			maxSizeAfterFirstWrap = bufferSize - (bufferSize % info.atracBytesPerFrame);
        		}
        	}

        	inputBuffer = new pspFileBuffer(addr, maxSizeBeforeFirstWrap, maxSizeAfterFirstWrap, readSize, readSize);
        	inputBuffer.notifyRead(info.inputFileDataOffset);
    		inputBuffer.setFileMaxSize(info.inputFileSize);
        	currentReadPosition = info.inputFileDataOffset;
        	atracCurrentSample = 0;
        	currentLoopNum = -1;
        	lastDecodedSamples = 0;

    		setOutputChannels(isMonoOutput ? 1 : 2);

        	int result = codec.init(info.atracBytesPerFrame, channels, outputChannels, info.atracCodingMode);
        	if (result < 0) {
        		return result;
        	}

        	setCodecInitialized();

        	return 0;
        }

        public int decodeData(int samplesAddr, TPointer32 outEndAddr) {
        	skippedEndSamples = 0;
        	Memory mem = Memory.getInstance();

        	if (currentReadPosition + info.atracBytesPerFrame > info.inputFileSize || getAtracCurrentSample() - info.atracSampleOffset > info.atracEndSample) {
        		if (log.isDebugEnabled()) {
        			log.debug(String.format("decodeData returning ERROR_ATRAC_ALL_DATA_DECODED"));
        		}
        		outEndAddr.setValue(true);
        		return ERROR_ATRAC_ALL_DATA_DECODED;
        	}

        	if (inputBuffer.getCurrentSize() < info.atracBytesPerFrame) {
        		if (getSecondBufferAddr() > 0 && getSecondBufferSize() >= info.atracBytesPerFrame) {
        			addSecondBufferStreamData();
        		} else {
	        		if (log.isDebugEnabled()) {
	        			log.debug(String.format("decodeData returning ERROR_ATRAC_BUFFER_IS_EMPTY"));
	        		}
	        		outEndAddr.setValue(false);
	        		return ERROR_ATRAC_BUFFER_IS_EMPTY;
        		}
        	}

        	int currentSample = getAtracCurrentSample();
        	int nextCurrentSample = currentSample;
        	if (currentSample == 0) {
        		skippedSamples = startSkippedSamples + info.atracSampleOffset;
        	} else {
        		// When looping or changing the play position, the PSP re-aligns
        		// on multiples of maxSamples
        		skippedSamples = (currentSample + startSkippedSamples + info.atracSampleOffset) % maxSamples;
        	}

        	// Skip complete frames
        	while (skippedSamples >= maxSamples) {
        		inputBuffer.notifyRead(info.atracBytesPerFrame);
        		currentReadPosition += info.atracBytesPerFrame;
        		skippedSamples -= maxSamples;
        		nextCurrentSample += maxSamples;
        	}

        	int readAddr = inputBuffer.getReadAddr();
        	if (inputBuffer.getReadSize() < info.atracBytesPerFrame) {
        		if (temporaryDecodeArea == null || temporaryDecodeArea.allocatedSize < info.atracBytesPerFrame) {
        			if (temporaryDecodeArea != null) {
        				Modules.SysMemUserForUserModule.free(temporaryDecodeArea);
        			}
            		temporaryDecodeArea = Modules.SysMemUserForUserModule.malloc(KERNEL_PARTITION_ID, "Temporary-sceAtrac3plus-DecodeData", PSP_SMEM_Low, info.atracBytesPerFrame, 0);
        		}
        		if (temporaryDecodeArea != null) {
        			readAddr = temporaryDecodeArea.addr;
        			int wrapLength = inputBuffer.getReadSize();
        			if (log.isDebugEnabled()) {
        				log.debug(String.format("decodeData from temporary buffer: 0x%08X/0x%X (from 0x%08X), 0x%08X/0x%X (from 0x%08X)", readAddr, wrapLength, inputBuffer.getReadAddr(), readAddr + wrapLength, info.atracBytesPerFrame - wrapLength, inputBuffer.getAddr()));
        			}
        			mem.memcpy(readAddr, inputBuffer.getReadAddr(), wrapLength);
        			mem.memcpy(readAddr + wrapLength, inputBuffer.getAddr(), info.atracBytesPerFrame - wrapLength);
        		}
        	}

        	SysMemInfo tempBuffer = null;
        	int decodedSamplesAddr = samplesAddr;
    		int bytesPerSample = 2 * getOutputChannels();
        	if (skippedSamples > 0) {
        		// Decode to a temporary buffer if we need to skip the first samples.
        		int tempBufferSize = getMaxSamples() * bytesPerSample;
        		tempBuffer = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceAtrac3plus-temp-decode-buffer", SysMemUserForUser.PSP_SMEM_Low, tempBufferSize, 0);
        		if (tempBuffer == null) {
        			log.warn(String.format("decodeData cannot allocate required temporary buffer of size=0x%X", tempBufferSize));
        		} else {
        			decodedSamplesAddr = tempBuffer.addr;
        		}
        	}

        	if (log.isDebugEnabled()) {
        		log.debug(String.format("decodeData from 0x%08X(0x%X) to 0x%08X(0x%X), skippedSamples=0x%X, currentSample=0x%X, outputChannels=%d", readAddr, info.atracBytesPerFrame, decodedSamplesAddr, maxSamples, skippedSamples, currentSample, outputChannels));
        		if (log.isTraceEnabled()) {
        			log.trace(String.format("decodeData from:%s", Utilities.getMemoryDump(readAddr, info.atracBytesPerFrame)));
        		}
        	}

        	int result = codec.decode(mem, readAddr, info.atracBytesPerFrame, mem, decodedSamplesAddr);
        	if (result < 0) {
        		if (log.isDebugEnabled()) {
        			log.debug(String.format("decodeData received codec decode error 0x%08X", result));
        		}
            	outEndAddr.setValue(false);
            	if (tempBuffer != null) {
            		Modules.SysMemUserForUserModule.free(tempBuffer);
            	}
        		return ERROR_ATRAC_API_FAIL;
        	}

        	inputBuffer.notifyRead(info.atracBytesPerFrame);
        	currentReadPosition += info.atracBytesPerFrame;

        	nextCurrentSample += codec.getNumberOfSamples() - skippedSamples;

        	if (nextCurrentSample - info.atracSampleOffset > info.atracEndSample) {
            	outEndAddr.setValue(info.loopNum == 0);
            	skippedEndSamples = nextCurrentSample - info.atracSampleOffset - info.atracEndSample - 1;
        	} else {
        		outEndAddr.setValue(false);
        	}
        	setAtracCurrentSample(nextCurrentSample);

        	if (skippedSamples > 0) {
        		int returnedSamples = getNumberOfSamples();
        		// Move the sample buffer to skip the needed samples
        		mem.memmove(samplesAddr, decodedSamplesAddr + skippedSamples * bytesPerSample, returnedSamples * bytesPerSample);
        	}

        	if (tempBuffer != null) {
        		Modules.SysMemUserForUserModule.free(tempBuffer);
        		tempBuffer = null;
        	}

        	for (int i = 0; i < info.numLoops; i++) {
        		LoopInfo loop = info.loops[i];
        		if (currentSample <= loop.startSample && loop.startSample < nextCurrentSample) {
        			// We are just starting a loop
        			currentLoopNum = i;
        			break;
        		} else if (currentSample <= loop.endSample && loop.endSample < nextCurrentSample && currentLoopNum == i) {
        			// We are just ending the current loop
        			if (info.loopNum == 0) {
        				// No more loop playback
        				currentLoopNum = -1;
        			} else {
        				// Replay the loop
        				log.info(String.format("Replaying atrac loop atracID=%d, loopStart=0x%X, loopEnd=0x%X", id, loop.startSample, loop.endSample));
        				setPlayPosition(loop.startSample);
        				nextCurrentSample = loop.startSample;
        				// loopNum < 0: endless loop playback
        				// loopNum > 0: play the loop loopNum times
        				if (info.loopNum > 0) {
        					info.loopNum--;
        				}
        				break;
        			}
        		}
        	}

        	return 0;
        }

        public void getStreamDataInfo(TPointer32 writeAddr, TPointer32 writableBytesAddr, TPointer32 readOffsetAddr) {
        	if (inputBuffer.getFileWriteSize() <= 0 && currentLoopNum >= 0 && info.loopNum != 0) {
        		// Read ahead to restart the loop
        		inputBuffer.setFilePosition(getFilePositionFromSample(info.loops[currentLoopNum].startSample));
        		reloadingFromLoopStart = true;
        	}

        	// Remember the CurrentSample at the time of the getStreamDataInfo
        	getStreamDataInfoCurrentSample = getAtracCurrentSample();

        	writeAddr.setValue(inputBuffer.getWriteAddr());
        	writableBytesAddr.setValue(inputBuffer.getWriteSize());
        	readOffsetAddr.setValue(inputBuffer.getFilePosition());
        }

        protected void addStreamData(int length) {
        	if (length > 0) {
        		if (getAtracCurrentSample() < getStreamDataInfoCurrentSample) {
        			// The atrac has looped since sceAtracGetStreamDataInfo() has been called.
        			// Ignore sceAtracAddStreamData as we now need atrac data from the loop start.
        			if (log.isDebugEnabled()) {
        				log.debug(String.format("addStreamData ignored as the atrac has looped inbetween: sample 0x%X -> 0x%X", getStreamDataInfoCurrentSample, getAtracCurrentSample()));
        			}
        		} else {
        			if (log.isTraceEnabled()) {
        				log.trace(String.format("addStreamData length=0x%X: %s", length, Utilities.getMemoryDump(inputBuffer.getWriteAddr(), length)));
        			}
        			inputBuffer.notifyWrite(length);
        		}
        	}
        }

        private void addSecondBufferStreamData() {
        	while (inputBuffer.getWriteSize() > 0 && secondBufferSize > 0) {
        		int length = Math.min(inputBuffer.getWriteSize(), secondBufferSize);
        		if (log.isDebugEnabled()) {
        			log.debug(String.format("addSecondBufferStreamData from 0x%08X to 0x%08X, length=0x%X", secondBufferAddr, inputBuffer.getWriteAddr(), length));
        		}
        		Memory.getInstance().memcpy(inputBuffer.getWriteAddr(), secondBufferAddr, length);
        		addStreamData(length);
        		secondBufferAddr += length;
        		secondBufferSize -= length;
        	}

        	if (secondBufferSize <= 0) {
    			secondBufferAddr = -1;
    			secondBufferSize = 0;
    		}
        }

        public void setSecondBuffer(int address, int size) {
        	secondBufferAddr = address;
        	secondBufferSize = size;
        }

        public int getSecondBufferAddr() {
        	return secondBufferAddr;
        }

        public int getSecondBufferSize() {
        	return secondBufferSize;
        }

        public int getSecondBufferReadPosition() {
        	// TODO
        	return 0;
        }

        public void createContext() {
        	if (atracContext == null) {
        		atracContext = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-AtracCtx-%d", id), SysMemUserForUser.PSP_SMEM_High, 200, 0);
        		if (atracContext != null) {
	        		Memory mem = Memory.getInstance();
	        		int contextAddr = atracContext.addr;
	            	mem.memset(contextAddr, (byte) 0, atracContext.size);

	            	if (hasLoop()) {
	            		mem.write32(contextAddr + 140, info.loops[0].endSample); // loop end
	            	} else {
	            		mem.write32(contextAddr + 140, 0); // no loop
	            	}
	    	        mem.write8(contextAddr + 149, (byte) 2); // state
	    	        mem.write8(contextAddr + 151, (byte) getChannels()); // number of channels
	    	        mem.write16(contextAddr + 154, (short) getCodecType());
	    	        //mem.write32(contextAddr + 168, 0); // Voice associated to this Atrac context using __sceSasSetVoiceATRAC3?

	    	        // Used by SampleSourceAtrac3 (input for __sceSasConcatenateATRAC3):
	    	        mem.write32(contextAddr + ATRAC3_CONTEXT_READ_SIZE_OFFSET, getInputBuffer().getFilePosition());
	    	        mem.write32(contextAddr + ATRAC3_CONTEXT_REQUIRED_SIZE_OFFSET, getInputBuffer().getFilePosition()); 
	    	        mem.write32(contextAddr + ATRAC3_CONTEXT_DECODE_RESULT_OFFSET, 0);
        		}
        	}
        }

        private void releaseContext() {
        	if (atracContext != null) {
        		Modules.SysMemUserForUserModule.free(atracContext);
        		atracContext = null;
        	}
        }

        public SysMemInfo getContext() {
        	return atracContext;
        }

        public void createInternalBuffer(int size) {
        	if (internalBuffer == null) {
        		internalBuffer = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-AtracBuf-%d", id), SysMemUserForUser.PSP_SMEM_Low, size, 0);
        	}
        }

        private void releaseInternalBuffer() {
        	if (internalBuffer != null) {
        		Modules.SysMemUserForUserModule.free(internalBuffer);
        		internalBuffer = null;
        	}
        }

        public SysMemInfo getInternalBuffer() {
        	return internalBuffer;
        }

        public int getCodecType() {
            return codecType;
        }

        public void setCodecType(int codecType) {
        	this.codecType = codecType;
        	if (codecType == PSP_CODEC_AT3) {
        		maxSamples = Atrac3Decoder.SAMPLES_PER_FRAME;
        		startSkippedSamples = 69;
        	} else if (codecType == PSP_CODEC_AT3PLUS) {
        		maxSamples = Atrac3plusDecoder.ATRAC3P_FRAME_SAMPLES;
        		startSkippedSamples = 368;
        	} else {
        		maxSamples = 0;
        		startSkippedSamples = 0;
        	}
        }

        public int getAtracBitrate() {
            return info.atracBitrate;
        }

        public int getChannels() {
            return channels;
        }

        public void setChannels(int channels) {
        	this.channels = channels;
        }

        public int getAtracSampleRate() {
            return info.atracSampleRate;
        }

        public int getAtracEndSample() {
            return info.atracEndSample;
        }

        public int getAtracCurrentSample() {
            return atracCurrentSample;
        }

        public int getAtracBytesPerFrame() {
            return info.atracBytesPerFrame;
        }

        public void setAtracCurrentSample(int sample) {
            atracCurrentSample = sample;
        }

        public int getLoopNum() {
        	if (!hasLoop()) {
        		return 0;
        	}
            return info.loopNum;
        }

        public void setLoopNum(int num) {
        	info.loopNum = num;
        }

        public int getMaxSamples() {
        	return maxSamples;
        }

        public pspFileBuffer getInputBuffer() {
            return inputBuffer;
        }

        public int getInputFileSize() {
            return info.inputFileSize;
        }

        public void setInputFileSize(int bytes) {
        	info.inputFileSize = bytes;
        }

        public int getSecondInputFileSize() {
            return secondInputFileSize;
        }

        public boolean isSecondBufferNeeded() {
            return isSecondBufferNeeded;
        }

        public boolean isSecondBufferSet() {
            return isSecondBufferSet;
        }

        public int getInternalErrorInfo() {
            return internalErrorInfo;
        }

        public int getRemainFrames() {
        	if (inputBuffer == null) {
        		return 0;
        	}
        	if (inputBufferContainsAllData()) {
        		return PSP_ATRAC_ALLDATA_IS_ON_MEMORY;
        	}
        	if ((!hasLoop() || info.loopNum == 0) && inputBuffer.getFileWriteSize() <= 0) {
        		return PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY;
        	}
        	int remainFrames = inputBuffer.getCurrentSize() / info.atracBytesPerFrame;

        	return remainFrames;
        }

        public int getBufferInfoForResetting(int sample, TPointer32 bufferInfoAddr) {
        	if (sample > getAtracEndSample()) {
        		return SceKernelErrors.ERROR_ATRAC_BAD_SAMPLE;
        	}

        	int writableBytes;
        	int minimumWriteBytes;
        	int readPosition;
        	if (inputBufferContainsAllData()) {
        		writableBytes = 0;
        		minimumWriteBytes = 0;
        		readPosition = 0;
        	} else {
        		writableBytes = inputBuffer.getMaxSize();
        		minimumWriteBytes = info.atracBytesPerFrame * 2;
            	if (sample == 0) {
            		readPosition = 0;
            	} else {
            		readPosition = getFilePositionFromSample(sample);
            	}
        	}
        	// Holds buffer related parameters.
            // Main buffer.
            bufferInfoAddr.setValue(0, inputBuffer.getAddr());          // Pointer to current writing position in the buffer.
            bufferInfoAddr.setValue(4, writableBytes);                  // Number of bytes which can be written to the buffer.
            bufferInfoAddr.setValue(8, minimumWriteBytes);              // Number of bytes that must to be written to the buffer.
            bufferInfoAddr.setValue(12, readPosition);                  // Read offset in the input file for the given sample.
            // Secondary buffer.
            bufferInfoAddr.setValue(16, getSecondBufferAddr());         // Pointer to current writing position in the buffer.
            bufferInfoAddr.setValue(20, getSecondBufferSize());         // Number of bytes which can be written to the buffer.
            bufferInfoAddr.setValue(24, getSecondBufferSize());         // Number of bytes that must to be written to the buffer.
            bufferInfoAddr.setValue(28, getSecondBufferReadPosition()); // Read offset for input file.

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceAtracGetBufferInfoForReseting returning writeAddr=0x%08X, writeMaxSize=0x%X, writeMinSize=0x%X, readPosition=0x%X", bufferInfoAddr.getValue(0), bufferInfoAddr.getValue(4), bufferInfoAddr.getValue(8), bufferInfoAddr.getValue(12)));
            }
            return 0;
        }

        public void setPlayPosition(int sample, int bytesWrittenFirstBuf, int bytesWrittenSecondBuf) {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("sceAtracResetPlayPosition: %s", Utilities.getMemoryDump(inputBuffer.getWriteAddr(), bytesWrittenFirstBuf)));
        	}

        	if (sample != atracCurrentSample) {
	        	// Do not change the position of the inputBuffer when it contains all the Atrac data
	        	if (!inputBufferContainsAllData()) {
		        	currentReadPosition = getFilePositionFromSample(sample);
	        		inputBuffer.reset(bytesWrittenFirstBuf, currentReadPosition);
	        	} else {
		        	currentReadPosition = getFilePositionFromSample(sample);
		        	inputBuffer.reset(inputBuffer.getFilePosition(), 0);
		        	inputBuffer.notifyRead(currentReadPosition);
	        	}
	        	setAtracCurrentSample(sample);
        	}
        }

        private boolean inputBufferContainsAllData() {
        	if (inputBuffer != null && inputBuffer.getMaxSize() >= info.inputFileSize) {
        		if (inputBuffer.getReadSize() + currentReadPosition >= info.inputFileSize) {
        			return true;
        		}
        	}

        	return false;
        }

        private int getFilePositionFromSample(int sample) {
    		return info.inputFileDataOffset + sample / maxSamples * info.atracBytesPerFrame;
        }

        public void setPlayPosition(int sample) {
        	if ((sample / maxSamples * maxSamples) != getAtracCurrentSample()) {
	            if (inputBufferContainsAllData()) {
	            	getInputBuffer().reset(inputBuffer.getFilePosition(), 0);
	            	getInputBuffer().notifyRead(getFilePositionFromSample(sample));
	            } else if (reloadingFromLoopStart && currentLoopNum >= 0 && sample == info.loops[currentLoopNum].startSample) {
	            	// We have already started to reload data from the loop start
	            	reloadingFromLoopStart = false;
	            } else {
	            	getInputBuffer().reset(0, getFilePositionFromSample(sample));
	            }
        		currentReadPosition = getFilePositionFromSample(sample);
	            setAtracCurrentSample(sample);
        	}
        }

        public boolean hasLoop() {
        	return info.numLoops > 0;
        }

        public int getLoopStatus() {
        	if (!hasLoop()) {
        		return PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA;
        	}
        	return PSP_ATRAC_STATUS_LOOP_STREAM_DATA;
        }

        public int getLoopStartSample() {
        	if (!hasLoop()) {
        		return -1;
        	}
        	return info.loops[0].startSample;
        }

        public int getLoopEndSample() {
        	if (!hasLoop()) {
        		return -1;
        	}

        	return info.loops[0].endSample;
        }

        public void setContextDecodeResult(int result, int requestedSize) {
        	if (getContext() != null) {
        		Memory mem = Memory.getInstance();
        		int contextAddr = getContext().addr;
        		mem.write32(contextAddr + ATRAC3_CONTEXT_DECODE_RESULT_OFFSET, result);
        		int readSize = mem.read32(contextAddr + ATRAC3_CONTEXT_READ_SIZE_OFFSET);
        		mem.write32(contextAddr + ATRAC3_CONTEXT_REQUIRED_SIZE_OFFSET, readSize + requestedSize);
        	}
        }

		public int getSourceBufferLength() {
			return sourceBufferLength;
		}

		public void setSourceBufferLength(int sourceBufferLength) {
			this.sourceBufferLength = sourceBufferLength;
		}

		public int getLastDecodedSamples() {
			return lastDecodedSamples;
		}

		public void setLastDecodedSamples(int lastDecodedSamples) {
			this.lastDecodedSamples = lastDecodedSamples;
		}

		public int getOutputChannels() {
			return outputChannels;
		}

		public void setOutputChannels(int outputChannels) {
			this.outputChannels = outputChannels;
		}

		public int getNumberOfSamples() {
			return codec.getNumberOfSamples() - skippedSamples - skippedEndSamples;
		}

		public boolean isInUse() {
			return inUse;
		}

		public void setInUse(boolean inUse) {
			this.inUse = inUse;

			if (inUse) {
				initCodec();
			} else {
				setCodecInitialized(false);
				codec = null;
			}
		}

		public void initCodec() {
			initCodec(getCodecType());
		}

        @Override
		public String toString() {
			return String.format("AtracID[id=%d, inputBuffer=%s, channels=%d, outputChannels=%d]", id, inputBuffer, getChannels(), getOutputChannels());
		}
    }

	protected static int read28(Memory mem, int address) {
		return ((mem.read8(address + 0) & 0x7F) << 21)
		     | ((mem.read8(address + 1) & 0x7F) << 14)
		     | ((mem.read8(address + 2) & 0x7F) <<  7)
		     | ((mem.read8(address + 3) & 0x7F) <<  0);
	}

    protected static String getStringFromInt32(int n) {
    	char c1 = (char) ((n      ) & 0xFF);
    	char c2 = (char) ((n >>  8) & 0xFF);
    	char c3 = (char) ((n >> 16) & 0xFF);
    	char c4 = (char) ((n >> 24) & 0xFF);

    	return String.format("%c%c%c%c", c1, c2, c3, c4);
    }

    protected int hleAtracReinit(int numAT3IdCount, int numAT3plusIdCount) {
    	for (int i = 0; i < atracIDs.length; i++) {
    		if (atracIDs[i].isInUse()) {
    			return ERROR_BUSY;
    		}
    	}

    	int i;
    	for (i = 0; i < numAT3plusIdCount && i < atracIDs.length; i++) {
    		atracIDs[i].setCodecType(PSP_CODEC_AT3PLUS);
    	}
    	for (int j = 0; j < numAT3IdCount && i < atracIDs.length; j++, i++) {
    		atracIDs[i].setCodecType(PSP_CODEC_AT3);
    	}
    	// The rest is unused
    	for (; i < atracIDs.length; i++) {
    		atracIDs[i].setCodecType(0);
    	}

    	return 0;
    }

    public int hleGetAtracID(int codecType) {
    	for (int i = 0; i < atracIDs.length; i++) {
    		if (atracIDs[i].getCodecType() == codecType && !atracIDs[i].isInUse()) {
    			atracIDs[i].setInUse(true);
    			return i;
    		}
    	}

    	return ERROR_ATRAC_NO_ID;
    }

    public AtracID getAtracID(int atID) {
    	return atracIDs[atID];
    }

    public static int analyzeRiffFile(Memory mem, int addr, int length, AtracFileInfo info) {
        int result = ERROR_ATRAC_UNKNOWN_FORMAT;

        int currentAddr = addr;
        int bufferSize = length;
        info.atracEndSample = -1;
        info.numLoops = 0;
        info.inputFileDataOffset = 0;

        if (bufferSize < 12) {
        	log.error(String.format("Atrac buffer too small %d", bufferSize));
        	return ERROR_ATRAC_INVALID_SIZE;
        }

        // RIFF file format:
        // Offset 0: 'RIFF'
        // Offset 4: file length - 8
        // Offset 8: 'WAVE'
        int magic = readUnaligned32(mem, currentAddr);
        int WAVEMagic = readUnaligned32(mem, currentAddr + 8);
        if (magic != RIFF_MAGIC || WAVEMagic != WAVE_MAGIC) {
        	log.error(String.format("Not a RIFF/WAVE format! %s", Utilities.getMemoryDump(currentAddr, 16)));
        	return ERROR_ATRAC_UNKNOWN_FORMAT;
        }

        info.inputFileSize = readUnaligned32(mem, currentAddr + 4) + 8;
        info.inputDataSize = info.inputFileSize;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("FileSize 0x%X", info.inputFileSize));
        }
        currentAddr += 12;
        bufferSize -= 12;

        boolean foundData = false;
        while (bufferSize >= 8 && !foundData) {
        	int chunkMagic = readUnaligned32(mem, currentAddr);
        	int chunkSize = readUnaligned32(mem, currentAddr + 4);
        	currentAddr += 8;
        	bufferSize -= 8;

        	switch (chunkMagic) {
        		case DATA_CHUNK_MAGIC:
        			foundData = true;
        			// Offset of the data chunk in the input file
        			info.inputFileDataOffset = currentAddr - addr;
        			info.inputDataSize = chunkSize;
        			if (log.isDebugEnabled()) {
        				log.debug(String.format("DATA Chunk: data offset=0x%X, data size=0x%X", info.inputFileDataOffset, info.inputDataSize));
        			}
        			break;
        		case FMT_CHUNK_MAGIC: {
        			if (chunkSize >= 16) {
                        int compressionCode = mem.read16(currentAddr);
                        info.atracChannels = mem.read16(currentAddr + 2);
                        info.atracSampleRate = readUnaligned32(mem, currentAddr + 4);
                        info.atracBitrate = readUnaligned32(mem, currentAddr + 8);
                        info.atracBytesPerFrame = mem.read16(currentAddr + 12);
                        int hiBytesPerSample = mem.read16(currentAddr + 14);
						int extraDataSize = mem.read16(currentAddr + 16);
						if (extraDataSize == 14) {
							info.atracCodingMode = mem.read16(currentAddr + 18 + 6);
						}
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("WAVE format: magic=0x%08X('%s'), chunkSize=%d, compressionCode=0x%04X, channels=%d, sampleRate=%d, bitrate=%d, bytesPerFrame=0x%X, hiBytesPerSample=%d, codingMode=%d", chunkMagic, getStringFromInt32(chunkMagic), chunkSize, compressionCode, info.atracChannels, info.atracSampleRate, info.atracBitrate, info.atracBytesPerFrame, hiBytesPerSample, info.atracCodingMode));
                            // Display rest of chunk as debug information
                            StringBuilder restChunk = new StringBuilder();
                            for (int i = 16; i < chunkSize; i++) {
                                int b = mem.read8(currentAddr + i);
                                restChunk.append(String.format(" %02X", b));
                            }
                            if (restChunk.length() > 0) {
                                log.debug(String.format("Additional chunk data:%s", restChunk));
                            }
                        }

                        if (compressionCode == AT3_MAGIC) {
                        	result = PSP_CODEC_AT3;
                        } else if (compressionCode == AT3_PLUS_MAGIC) {
                        	result = PSP_CODEC_AT3PLUS;
                        } else {
                        	return ERROR_ATRAC_UNKNOWN_FORMAT;
                        }
        			}
        			break;
        		}
        		case FACT_CHUNK_MAGIC: {
        			if (chunkSize >= 8) {
        				info.atracEndSample = readUnaligned32(mem, currentAddr);
        				if (info.atracEndSample > 0) {
        					info.atracEndSample -= 1;
        				}
        				if (chunkSize >= 12) {
        					// Is the value at offset 4 ignored?
        					info.atracSampleOffset = readUnaligned32(mem, currentAddr + 8); // The loop samples are offset by this value
        				} else {
        					info.atracSampleOffset = readUnaligned32(mem, currentAddr + 4); // The loop samples are offset by this value
        				}
                        if (log.isDebugEnabled()) {
                        	log.debug(String.format("FACT Chunk: chunkSize=%d, endSample=0x%X, sampleOffset=0x%X", chunkSize, info.atracEndSample, info.atracSampleOffset));
                        }
        			}
        			break;
        		}
        		case SMPL_CHUNK_MAGIC: {
        			if (chunkSize >= 36) {
        				int checkNumLoops = readUnaligned32(mem, currentAddr + 28);
	                	if (chunkSize >= 36 + checkNumLoops * 24) {
	                		info.numLoops = checkNumLoops;
	                		info.loops = new LoopInfo[info.numLoops];
    	                	int loopInfoAddr = currentAddr + 36;
    	                	for (int i = 0; i < info.numLoops; i++) {
    	                		LoopInfo loop = new LoopInfo();
    	                		info.loops[i] = loop;
    	                		loop.cuePointID = readUnaligned32(mem, loopInfoAddr);
    	                		loop.type = readUnaligned32(mem, loopInfoAddr + 4);
    	                		loop.startSample = readUnaligned32(mem, loopInfoAddr + 8) - info.atracSampleOffset;
    	                		loop.endSample = readUnaligned32(mem, loopInfoAddr + 12) - info.atracSampleOffset;
    	                		loop.fraction = readUnaligned32(mem, loopInfoAddr + 16);
    	                		loop.playCount = readUnaligned32(mem, loopInfoAddr + 20);

    	                		if (log.isDebugEnabled()) {
    	                			log.debug(String.format("Loop #%d: %s", i, loop.toString()));
    	                		}
    	                		loopInfoAddr += 24;
    	                	}
    	                	// TODO Second buffer processing disabled because still incomplete
    	                    //isSecondBufferNeeded = true;
	                	}
        			}
        			break;
        		}
        	}

        	if (chunkSize > bufferSize) {
        		break;
        	}

        	currentAddr += chunkSize;
        	bufferSize -= chunkSize;
        }

        if (info.loops != null) {
        	// If a loop end is past the atrac end, assume the atrac end
            for (LoopInfo loop : info.loops) {
            	if (loop.endSample > info.atracEndSample) {
            		loop.endSample = info.atracEndSample;
            	}
            }
        }

        return result;
    }

    protected int hleSetHalfwayBuffer(int atID, TPointer buffer, int readSize, int bufferSize, boolean isMonoOutput) {
        if (readSize > bufferSize) {
        	return SceKernelErrors.ERROR_ATRAC_INCORRECT_READ_SIZE;
        }

        // readSize and bufferSize are unsigned int's. Allow negative values.
        if (readSize < 0) {
        	readSize = MemoryMap.SIZE_RAM;
        }
        if (bufferSize < 0) {
        	bufferSize = MemoryMap.SIZE_RAM;
        }

        if (log.isTraceEnabled()) {
        	log.trace(String.format("hleSetHalfwayBuffer buffer: %s", Utilities.getMemoryDump(buffer.getAddress(), readSize)));
        }

        AtracFileInfo info = new AtracFileInfo();
        int codecType = analyzeRiffFile(buffer.getMemory(), buffer.getAddress(), readSize, info);
        if (codecType < 0) {
        	return codecType;
        }

        AtracID id = atracIDs[atID];
    	if (codecType != id.getCodecType()) {
    		return SceKernelErrors.ERROR_ATRAC_WRONG_CODEC;
        }

        int result = id.setHalfwayBuffer(buffer.getAddress(), readSize, bufferSize, isMonoOutput, info);
        if (result < 0) {
        	return result;
        }

        // Reschedule
        Modules.ThreadManForUserModule.hleYieldCurrentThread();

        return result;
    }

    protected int hleSetHalfwayBufferAndGetID(TPointer buffer, int readSize, int bufferSize, boolean isMonoOutput) {
        if (readSize > bufferSize) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleSetHalfwayBufferAndGetID returning 0x%X", ERROR_ATRAC_INCORRECT_READ_SIZE));
        	}
        	return ERROR_ATRAC_INCORRECT_READ_SIZE;
        }

        // readSize and bufferSize are unsigned int's.
        // Allow negative values.
        // "Tales of VS - ULJS00209" is even passing an uninitialized value bufferSize=0xDEADBEEF
        if (readSize < 0) {
        	readSize = MemoryMap.SIZE_RAM;
        }
        if (bufferSize < 0) {
        	bufferSize = MemoryMap.SIZE_RAM;
        }

        if (log.isTraceEnabled()) {
        	log.trace(String.format("hleSetHalfwayBufferAndGetID buffer=%s", Utilities.getMemoryDump(buffer, readSize)));
        }

        AtracFileInfo info = new AtracFileInfo();
        int codecType = analyzeRiffFile(buffer.getMemory(), buffer.getAddress(), readSize, info);
        if (codecType < 0) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleSetHalfwayBufferAndGetID returning 0x%X", codecType));
        	}
        	return codecType;
        }

        int atID = hleGetAtracID(codecType);
        if (atID < 0) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleSetHalfwayBufferAndGetID returning 0x%X", atID));
        	}
        	return atID;
        }

        AtracID id = atracIDs[atID];
    	int result = id.setHalfwayBuffer(buffer.getAddress(), readSize, bufferSize, isMonoOutput, info);
    	if (result < 0) {
    		hleReleaseAtracID(atID);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleSetHalfwayBufferAndGetID returning 0x%X", result));
        	}
    		return result;
    	}

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleSetHalfwayBufferAndGetID returning atID=0x%X", atID));
        }

        // Reschedule
        Modules.ThreadManForUserModule.hleYieldCurrentThread();

        return atID;
    }

    protected void hleReleaseAtracID(int atracID) {
    	atracIDs[atracID].release();
    }

    public int checkAtracID(int atID) {
    	if (atID < 0 || atID >= atracIDs.length || !atracIDs[atID].isInUse()) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("checkAtracID invalid atracID=0x%X", atID));
    		}
            throw new SceKernelErrorException(ERROR_ATRAC_BAD_ID);
    	}

    	return atID;
    }


	private static int read24(Memory mem, int address) {
		return (mem.read8(address + 0) << 16)
		     | (mem.read8(address + 1) <<  8)
		     | (mem.read8(address + 2) <<  0);
	}

	private static int read16(Memory mem, int address) {
		return (mem.read8(address) << 8) | mem.read8(address + 1);
	}

	private static int analyzeAA3File(TPointer buffer, int fileSize, AtracFileInfo info) {
		Memory mem = buffer.getMemory();
		int address = buffer.getAddress();
		int codecType = 0;

		int magic = read24(mem, address);
		address += 3;
		if (magic != 0x656133 && magic != 0x494433) { // 3ae | 3AE
			log.error(String.format("Unknown AA3 magic 0x%06X", magic));
			return codecType;
		}

		if (mem.read8(address) != 3 || mem.read8(address + 1) != 0) {
			log.error(String.format("Unknown AA3 bytes 0x%08X 0x%08X", mem.read8(address), mem.read8(address + 1)));
			return ERROR_AA3_INVALID_HEADER_VERSION;
		}
		address += 3;

		int headerSize = read28(mem, address);
		address += 4 + headerSize;
		if (mem.read8(address) == 0) {
			address += 16;
		}
		info.inputFileDataOffset = address - buffer.getAddress();

		magic = read24(mem, address);
		if (magic != 0x454133) { // 3AE
			log.error(String.format("Unknown AA3 magic 0x%06X", magic));
			return ERROR_AA3_INVALID_HEADER;
		}
		address += 4;

		int dataOffset = read16(mem, address);
		if (dataOffset == 0xFFFF) {
			return ERROR_AA3_INVALID_HEADER;
		}
		address += 2;

		int unknown2 = read16(mem, address);
		if (unknown2 != 0xFFFF) {
			return ERROR_AA3_INVALID_HEADER;
		}
		address += 2;

		address += 24;

		int samplesPerFrame;
		int flags = read16(mem, address + 2);
		switch (mem.read8(address)) {
			case 0: // AT3
				if ((flags & 0xE000) != 0x2000) { // Number of channels?
					return ERROR_AA3_INVALID_HEADER_FLAGS;
				}
				codecType = PSP_CODEC_AT3;
				samplesPerFrame = Atrac3Decoder.SAMPLES_PER_FRAME;
				info.atracChannels = 2;
				info.atracCodingMode = (mem.read8(address + 1) & 0x2) >> 1;
				info.atracBytesPerFrame = ((flags & 0x3FF) << 3);
				break;
			case 1: // AT3+
				if ((flags & 0x1C00) != 0x0800) {
					return ERROR_AA3_INVALID_HEADER_FLAGS;
				}
				if ((flags & 0xE000) != 0x2000) { // Number of channels?
					return ERROR_AA3_INVALID_HEADER_FLAGS;
				}
				codecType = PSP_CODEC_AT3PLUS;
				samplesPerFrame = Atrac3plusDecoder.ATRAC3P_FRAME_SAMPLES;
				info.atracChannels = 2;
				info.atracBytesPerFrame = ((flags & 0x3FF) << 3) + 8;
				break;
			default:
				return ERROR_AA3_INVALID_CODEC;
		}

		info.inputFileDataOffset += dataOffset;
		info.inputFileSize = fileSize;
		info.inputDataSize = fileSize - info.inputFileDataOffset;
		info.atracEndSample = (info.inputDataSize / info.atracBytesPerFrame) * samplesPerFrame;

		return codecType;
	}

    protected int hleSetAA3HalfwayBufferAndGetID(TPointer buffer, int readSize, int bufferSize, boolean isMonoOutput, int fileSize) {
        if (readSize > bufferSize) {
        	return ERROR_ATRAC_INCORRECT_READ_SIZE;
        }

        // readSize and bufferSize are unsigned int's.
        // Allow negative values.
        // "Tales of VS - ULJS00209" is even passing an uninitialized value bufferSize=0xDEADBEEF

        AtracFileInfo info = new AtracFileInfo();
        int codecType = analyzeAA3File(buffer, fileSize, info);
        if (codecType < 0) {
        	return codecType;
        }

        int atID = hleGetAtracID(codecType);
        if (atID < 0) {
        	return atID;
        }

        AtracID id = atracIDs[atID];
    	int result = id.setHalfwayBuffer(buffer.getAddress(), readSize, bufferSize, isMonoOutput, info);
    	if (result < 0) {
    		hleReleaseAtracID(atID);
    		return result;
    	}

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleSetHalfwayBufferAndGetID returning atID=0x%X", atID));
        }

        // Reschedule
        Modules.ThreadManForUserModule.hleYieldCurrentThread();

        return atID;
    }

	public AtracID getAtracIdFromContext(int atrac3Context) {
		for (int i = 0; i < atracIDs.length; i++) {
			AtracID id = atracIDs[i];
			if (id.isInUse()) {
				SysMemInfo context = id.getContext();
				if (context != null && context.addr == atrac3Context) {
					return id;
				}
			}
		}

		return null;
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1F59FDB, version = 150, checkInsideInterrupt = true)
    public int sceAtracStartEntry() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD5C28CC0, version = 150, checkInsideInterrupt = true)
    public int sceAtracEndEntry() {
        return 0;
    }

    @HLEFunction(nid = 0x780F88D1, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetAtracID(int codecType) {
        int atId = hleGetAtracID(codecType);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetAtracID: returning atID=0x%X", atId));
        }

        return atId;
    }

    @HLEFunction(nid = 0x61EB33F5, version = 150, checkInsideInterrupt = true)
    public int sceAtracReleaseAtracID(@CheckArgument("checkAtracID") int atID) {
        hleReleaseAtracID(atID);

        return 0;
    }

    @HLEFunction(nid = 0x0E2A73AB, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetData(@CheckArgument("checkAtracID") int atID, TPointer buffer, int bufferSize) {
    	return hleSetHalfwayBuffer(atID, buffer, bufferSize, bufferSize, false);
    }

    @HLEFunction(nid = 0x3F6E26B5, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetHalfwayBuffer(@CheckArgument("checkAtracID") int atID, TPointer halfBuffer, int readSize, int halfBufferSize) {
    	return hleSetHalfwayBuffer(atID, halfBuffer, readSize, halfBufferSize, false);
    }

    @HLEFunction(nid = 0x7A20E7AF, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetDataAndGetID(TPointer buffer, int bufferSize) {
    	return hleSetHalfwayBufferAndGetID(buffer, bufferSize, bufferSize, false);
    }

    @HLEFunction(nid = 0x0FAE370E, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetHalfwayBufferAndGetID(TPointer halfBuffer, int readSize, int halfBufferSize) {
    	return hleSetHalfwayBufferAndGetID(halfBuffer, readSize, halfBufferSize, false);
    }

    @HLEFunction(nid = 0x6A8C3CD5, version = 150, checkInsideInterrupt = true)
    public int sceAtracDecodeData(@CheckArgument("checkAtracID") int atID, @CanBeNull TPointer16 samplesAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 samplesNbrAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 outEndAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 remainFramesAddr) {
        AtracID id = atracIDs[atID];
        if (id.isSecondBufferNeeded() && !id.isSecondBufferSet()) {
            log.warn(String.format("sceAtracDecodeData atracID=0x%X needs second buffer!", atID));
            return SceKernelErrors.ERROR_ATRAC_SECOND_BUFFER_NEEDED;
        }

        int result = id.decodeData(samplesAddr.getAddress(), outEndAddr);
        if (result < 0) {
        	samplesNbrAddr.setValue(0);
        	return result;
        }

        samplesNbrAddr.setValue(id.getNumberOfSamples());
        remainFramesAddr.setValue(id.getRemainFrames());

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracDecodeData returning 0x%08X, samples=0x%X, end=%d, remainFrames=%d, currentSample=0x%X/0x%X, %s", result, samplesNbrAddr.getValue(), outEndAddr.getValue(), remainFramesAddr.getValue(), id.getAtracCurrentSample(), id.getAtracEndSample(), id));
        }

        // Delay the thread decoding the Atrac data,
        // the thread is also blocking using semaphores/event flags on a real PSP.
        if (result == 0) {
        	Modules.ThreadManForUserModule.hleKernelDelayThread(atracDecodeDelay, false);
        }

        return result;
    }

    @HLEFunction(nid = 0x9AE849A7, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetRemainFrame(@CheckArgument("checkAtracID") int atID, @BufferInfo(usage=Usage.out) TPointer32 remainFramesAddr) {
        AtracID id = atracIDs[atID];
		remainFramesAddr.setValue(id.getRemainFrames());

		if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetRemainFrame returning %d, %s", remainFramesAddr.getValue(), id));
        }

        return 0;
    }

    @HLEFunction(nid = 0x5D268707, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetStreamDataInfo(@CheckArgument("checkAtracID") int atID, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 writeAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 writableBytesAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 readOffsetAddr) {
        AtracID id = atracIDs[atID];
        id.getStreamDataInfo(writeAddr, writableBytesAddr, readOffsetAddr);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetStreamDataInfo write=0x%08X, writableBytes=0x%X, readOffset=0x%X, %s", writeAddr.getValue(), writableBytesAddr.getValue(), readOffsetAddr.getValue(), id));
        }

        return 0;
    }

    @HLEFunction(nid = 0x7DB31251, version = 150, checkInsideInterrupt = true)
    public int sceAtracAddStreamData(@CheckArgument("checkAtracID") int atID, int bytesToAdd) {
        AtracID id = atracIDs[atID];
        id.addStreamData(bytesToAdd);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracAddStreamData: %s", id));
        }

        return 0;
    }

    @HLEFunction(nid = 0x83E85EA0, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetSecondBufferInfo(@CheckArgument("checkAtracID") int atID, TPointer32 outPosition, TPointer32 outBytes) {
    	// Checked: outPosition and outBytes have to be non-NULL
        AtracID id = atracIDs[atID];
        if (!id.isSecondBufferNeeded()) {
        	// PSP clears both values when returning this error code.
        	outPosition.setValue(0);
        	outBytes.setValue(0);
            return SceKernelErrors.ERROR_ATRAC_SECOND_BUFFER_NOT_NEEDED;
        }

        outPosition.setValue(id.getSecondBufferReadPosition());
        outBytes.setValue(id.getSecondBufferSize());

        return 0;
    }

    @HLEFunction(nid = 0x83BF7AFD, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetSecondBuffer(@CheckArgument("checkAtracID") int atID, TPointer secondBuffer, int secondBufferSize) {
        AtracID id = atracIDs[atID];
        id.setSecondBuffer(secondBuffer.getAddress(), secondBufferSize);

        return 0;
    }

    @HLEFunction(nid = 0xE23E3A35, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetNextDecodePosition(@CheckArgument("checkAtracID") int atID, TPointer32 posAddr) {
        AtracID id = atracIDs[atID];
        if (id.getAtracCurrentSample() >= id.getAtracEndSample()) {
            return SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
        }

        posAddr.setValue(id.getAtracCurrentSample());

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAtracGetNextDecodePosition returning pos=%d", posAddr.getValue()));
        }

        return 0;
    }

    @HLEFunction(nid = 0xA2BBA8BE, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetSoundSample(@CheckArgument("checkAtracID") int atID, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 endSampleAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 loopStartSampleAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 loopEndSampleAddr) {
    	AtracID id = atracIDs[atID];
    	int endSample = id.getAtracEndSample();
        int loopStartSample = id.getLoopStartSample();
        int loopEndSample = id.getLoopEndSample();
    	if (endSample < 0) {
    		endSample = id.getAtracEndSample();
    	}
        if (endSample < 0) {
            endSample = id.getInputFileSize();
        }
        endSampleAddr.setValue(endSample);
        loopStartSampleAddr.setValue(loopStartSample);
        loopEndSampleAddr.setValue(loopEndSample);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAtracGetSoundSample returning endSample=0x%X, loopStartSample=0x%X, loopEndSample=0x%X", endSample, loopStartSample, loopEndSample));
        }

        return 0;
    }

    @HLEFunction(nid = 0x31668BAA, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetChannel(@CheckArgument("checkAtracID") int atID, TPointer32 channelAddr) {
    	AtracID id = atracIDs[atID];
        channelAddr.setValue(id.getChannels());

        return 0;
    }

    @HLEFunction(nid = 0xD6A5F2F7, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetMaxSample(@CheckArgument("checkAtracID") int atID, @BufferInfo(usage=Usage.out) TPointer32 maxSamplesAddr) {
    	AtracID id = atracIDs[atID];
        maxSamplesAddr.setValue(id.getMaxSamples());

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAtracGetMaxSample returning maxSamples=0x%X", id.getMaxSamples()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x36FAABFB, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetNextSample(@CheckArgument("checkAtracID") int atID, TPointer32 nbrSamplesAddr) {
    	AtracID id = atracIDs[atID];
        int samples = id.getMaxSamples();
        if (id.getInputBuffer().isEmpty()) {
            samples = 0; // No more data available in input buffer
        }
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAtracGetNextSample returning %d samples", samples));
        }
        nbrSamplesAddr.setValue(samples);

        return 0;
    }

    @HLEFunction(nid = 0xA554A158, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetBitrate(@CheckArgument("checkAtracID") int atID, TPointer32 bitrateAddr) {
    	AtracID id = atracIDs[atID];

    	// Bitrate based on https://github.com/uofw/uofw/blob/master/src/libatrac3plus/libatrac3plus.c
    	int bitrate = (id.getAtracBytesPerFrame() * 352800) / 1000;
    	if (id.getCodecType() == PSP_CODEC_AT3PLUS) {
    		bitrate = ((bitrate >> 11) + 8) & 0xFFFFFFF0;
    	} else {
    		bitrate = (bitrate + 511) >> 10;
    	}

    	bitrateAddr.setValue(bitrate);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAtracGetBitrate returning bitRate=0x%X", bitrate));
        }

        return 0;
    }

    @HLEFunction(nid = 0xFAA4F89B, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetLoopStatus(@CheckArgument("checkAtracID") int atID, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 loopNbr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 statusAddr) {
    	AtracID id = atracIDs[atID];
        loopNbr.setValue(id.getLoopNum());
        statusAddr.setValue(id.getLoopStatus());

        return 0;
    }

    @HLEFunction(nid = 0x868120B5, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetLoopNum(@CheckArgument("checkAtracID") int atID, int loopNbr) {
    	AtracID id = atracIDs[atID];
    	if (!id.hasLoop()) {
    		return SceKernelErrors.ERROR_ATRAC_NO_LOOP_INFORMATION;
    	}
        id.setLoopNum(loopNbr);

        return 0;
    }

    @HLELogging(level = "info")
    @HLEFunction(nid = 0xCA3CA3D2, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetBufferInfoForReseting(@CheckArgument("checkAtracID") int atID, int sample, TPointer32 bufferInfoAddr) {
    	AtracID id = atracIDs[atID];
        return id.getBufferInfoForResetting(sample, bufferInfoAddr);
    }

    @HLEFunction(nid = 0x644E5607, version = 150, checkInsideInterrupt = true)
    public int sceAtracResetPlayPosition(@CheckArgument("checkAtracID") int atID, int sample, int bytesWrittenFirstBuf, int bytesWrittenSecondBuf) {
        AtracID id = atracIDs[atID];
        id.setPlayPosition(sample, bytesWrittenFirstBuf, bytesWrittenSecondBuf);

        return 0;
    }

    @HLEFunction(nid = 0xE88F759B, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetInternalErrorInfo(@CheckArgument("checkAtracID") int atID, TPointer32 errorAddr) {
        AtracID id = atracIDs[atID];
        errorAddr.setValue(id.getInternalErrorInfo());

        return 0;
    }

    @HLEFunction(nid = 0xB3B5D042, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetOutputChannel(@CheckArgument("checkAtracID") int atID, TPointer32 outputChannelAddr) {
    	AtracID id = atracIDs[atID];
        outputChannelAddr.setValue(id.getOutputChannels());

        return 0;
    }

    @HLEFunction(nid = 0xECA32A99, version = 250, checkInsideInterrupt = true)
    public boolean sceAtracIsSecondBufferNeeded(@CheckArgument("checkAtracID") int atID) {
        AtracID id = atracIDs[atID];
        // 0 -> Second buffer isn't needed.
        // 1 -> Second buffer is needed.
        return id.isSecondBufferNeeded();
    }

    @HLEFunction(nid = 0x132F1ECA, version = 250, checkInsideInterrupt = true)
    public int sceAtracReinit(int at3IDNum, int at3plusIDNum) {
    	int result = 0;

    	if (at3IDNum != 0 || at3plusIDNum != 0) {
    		result = hleAtracReinit(at3IDNum, at3plusIDNum);

    		if (result >= 0) {
        		Modules.ThreadManForUserModule.hleYieldCurrentThread();
        	}
    	}

    	return result;
    }

    @HLEFunction(nid = 0x2DD3E298, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetBufferInfoForResetting(@CheckArgument("checkAtracID") int atID, int sample, TPointer32 bufferInfoAddr) {
        AtracID id = atracIDs[atID];
        return id.getBufferInfoForResetting(sample, bufferInfoAddr);
    }

    @HLEFunction(nid = 0x5CF9D852, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBuffer(@CheckArgument("checkAtracID") int atID, TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
    	return hleSetHalfwayBuffer(atID, MOutHalfBuffer, readSize, MOutHalfBufferSize, true);
    }

    // Not sure if this function does really exist
    @HLEUnimplemented
    @HLEFunction(nid = 0xF6837A1A, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutData(@CheckArgument("checkAtracID") int atID, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        return 0;
    }

    // Not sure if this function does really exist
    @HLEUnimplemented
    @HLEFunction(nid = 0x472E3825, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutDataAndGetID(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        return 0;
    }

    @HLEFunction(nid = 0x9CD7DE03, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBufferAndGetID(TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
    	return hleSetHalfwayBufferAndGetID(MOutHalfBuffer, readSize, MOutHalfBufferSize, true);
    }

    @HLEFunction(nid = 0x5622B7C1, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetAA3DataAndGetID(TPointer buffer, int bufferSize, int fileSize, int unused) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAtracSetAA3DataAndGetID buffer:%s", Utilities.getMemoryDump(buffer.getAddress(), bufferSize)));
    	}
    	return hleSetAA3HalfwayBufferAndGetID(buffer, bufferSize, bufferSize, false, fileSize);
    }

    @HLEFunction(nid = 0x5DD66588, version = 250)
    public int sceAtracSetAA3HalfwayBufferAndGetID(TPointer buffer, int readSize, int bufferSize, int fileSize, int unused) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAtracSetAA3HalfwayBufferAndGetID buffer:%s", Utilities.getMemoryDump(buffer.getAddress(), readSize)));
    	}
    	return hleSetAA3HalfwayBufferAndGetID(buffer, readSize, bufferSize, false, fileSize);
    }

    @HLELogging(level="info")
	@HLEFunction(nid = 0x231FC6B7, version = 600, checkInsideInterrupt = true)
    public int _sceAtracGetContextAddress(int atID) {
		if (atID < 0 || atID >= atracIDs.length || !atracIDs[atID].isInUse()) {
			return 0;
		}
        AtracID id = atracIDs[atID];

        id.createContext();
        SysMemInfo atracContext = id.getContext();
        if (atracContext == null) {
        	return 0;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("_sceAtracGetContextAddress returning 0x%08X", atracContext.addr));
        }

        return atracContext.addr;
    }

	@HLEFunction(nid = 0x0C116E1B, version = 620)
    public int sceAtracLowLevelDecode(@CheckArgument("checkAtracID") int atID, TPointer sourceAddr, TPointer32 sourceBytesConsumedAddr, TPointer samplesAddr, TPointer32 sampleBytesAddr) {
        AtracID id = atracIDs[atID];
        ICodec codec = id.getCodec();

        if (log.isTraceEnabled()) {
        	log.trace(String.format("sceAtracLowLevelDecode input:%s", Utilities.getMemoryDump(sourceAddr.getAddress(), id.getSourceBufferLength())));
        }

        int sourceBytesConsumed = 0;
    	int bytesPerSample = id.getOutputChannels() << 1;
    	int result = codec.decode(sourceAddr.getMemory(), sourceAddr.getAddress(), id.getSourceBufferLength(), samplesAddr.getMemory(), samplesAddr.getAddress());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAtracLowLevelDecode codec returned 0x%08X", result));
    	}
    	if (result < 0) {
    		log.info(String.format("sceAtracLowLevelDecode codec returning 0x%08X", result));
    		return result;
    	}
    	sourceBytesConsumed = result > 0 ? id.getSourceBufferLength() : 0;
    	sampleBytesAddr.setValue(codec.getNumberOfSamples() * bytesPerSample);

        // Consume a part of the Atrac3 source buffer
        sourceBytesConsumedAddr.setValue(sourceBytesConsumed);

    	Modules.ThreadManForUserModule.hleKernelDelayThread(atracDecodeDelay, false);

        return 0;
    }

	@HLELogging(level="info")
    @HLEFunction(nid = 0x1575D64B, version = 620)
    public int sceAtracLowLevelInitDecoder(@CheckArgument("checkAtracID") int atID, TPointer32 paramsAddr) {
        int numberOfChannels = paramsAddr.getValue(0);
		int outputChannels = paramsAddr.getValue(4);
		int sourceBufferLength = paramsAddr.getValue(8);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAtracLowLevelInitDecoder values at %s: numberOfChannels=%d, outputChannels=%d, sourceBufferLength=0x%08X", paramsAddr, numberOfChannels, outputChannels, sourceBufferLength));
		}

        AtracID id = atracIDs[atID];

        int result = 0;

        id.setChannels(numberOfChannels);
        id.setOutputChannels(outputChannels);
        id.setSourceBufferLength(sourceBufferLength);

    	// TODO How to find out the codingMode for AT3 audio? Assume STEREO, not JOINT_STEREO
		result = id.getCodec().init(sourceBufferLength, numberOfChannels, outputChannels, 0);
		id.setCodecInitialized();

        return result;
    }
}