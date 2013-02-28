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

import static jpcsp.util.Utilities.readUnaligned32;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.connector.AtracCodec;
import jpcsp.settings.AbstractBoolSettingsListener;

import org.apache.log4j.Logger;

@HLELogging
public class sceAtrac3plus extends HLEModule {
    public static Logger log = Modules.getLogger("sceAtrac3plus");

    @Override
    public String getName() {
        return "sceAtrac3plus";
    }

    @Override
    public void start() {
        atracIDs = new HashMap<Integer, AtracID>();

        setSettingsListener("emu.useConnector", new EnableConnectorSettingsListener());

        super.start();
    }

    protected static final String idPurpose = "sceAtrac3plus";
    protected static final int AT3_MAGIC      = 0x0270; // "AT3"
    protected static final int AT3_PLUS_MAGIC = 0xFFFE; // "AT3PLUS"
    public    static final int RIFF_MAGIC = 0x46464952; // "RIFF"
    protected static final int WAVE_MAGIC = 0x45564157; // "WAVE"
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

    protected static final int PSP_MODE_AT_3_PLUS = sceAudiocodec.PSP_CODEC_AT3PLUS;
    protected static final int PSP_MODE_AT_3 = sceAudiocodec.PSP_CODEC_AT3;

    // Tested on PSP:
    // Only 2 atracIDs per format can be registered at the same time.
    // Note: After firmware 2.50, these limits can be changed by sceAtracReinit.
    protected int atrac3MaxIDsCount = 2;
    protected int atrac3plusMaxIDsCount = 2;
    protected int atrac3Num;
    protected int atrac3plusNum;
    protected static final int atracDecodeDelay = 2300; // Microseconds, based on PSP tests

    public static boolean useAtracCodec = false;
    protected HashMap<Integer, AtracID> atracIDs;
    protected static final int atracIDMask = 0x000000FF;

    protected static class LoopInfo {
    	protected int cuePointID;
    	protected int type;
    	protected int startSample;
    	protected int endSample;
    	protected int fraction;
    	protected int playCount;

    	@Override
		public String toString() {
			return String.format("LoopInfo[cuePointID %d, type %d, startSample %d, endSample %d, fraction %d, playCount %d]", cuePointID, type, startSample, endSample, fraction, playCount);
		}
    }

    public static class AtracID {
        // Internal info.
    	protected int id;
        protected int codecType;
        protected AtracCodec atracCodec;
        // Context (used only from firmware 6.00)
        protected SysMemInfo atracContext;
        protected SysMemInfo internalBuffer;
        // Sound data.
        protected int atracBitrate = 64;
        protected int atracChannels = 2;
        protected int atracOutputChannels = 2;
        protected int atracSampleRate = 0xAC44;
        protected int atracBytesPerFrame = 0x0230;
        protected int atracEndSample;
        protected int atracCurrentSample;
        protected int maxSamples;
        protected int atracSampleOffset;
        protected int lastDecodedSamples;
        // First buffer.
        protected int inputBufferAddr;
        protected int inputBufferSize;
        protected int inputBufferOffset;
        protected int inputBufferWritableBytes;
        protected int inputBufferNeededBytes;
        // Second buffer.
        protected int secondInputBufferAddr;
        protected int secondInputBufferSize;
        protected int secondInputBufferOffset;
        protected int secondInputBufferWritableBytes;
        protected int secondInputBufferNeededBytes;
        // Input file.
        protected int inputFileSize;
        protected int inputFileOffset;
        protected int secondInputFileSize;
        protected int secondInputFileOffset;
        protected boolean isSecondBufferNeeded;
        protected boolean isSecondBufferSet;
        protected int internalErrorInfo;
        protected int inputFileDataOffset;
        // Loops
        protected int loopNum;
        protected int numLoops;
        protected LoopInfo[] loops;
        protected int loopStartBytesWrittenFirstBuf;
        protected int loopStartBytesWrittenSecondBuf;
        protected int currentLoopNum = -1;
        protected boolean forceReloadOfData;
        protected boolean forceAllDataIsOnMemory;
        // LowLevel decoding
        protected int sourceBufferLength;

        public AtracID(int id, int codecType, AtracCodec atracCodec) {
            this.codecType = codecType;
            this.id = id;
            this.atracCodec = atracCodec;
            if (codecType == PSP_MODE_AT_3 && Modules.sceAtrac3plusModule.atrac3Num < Modules.sceAtrac3plusModule.atrac3MaxIDsCount) {
            	Modules.sceAtrac3plusModule.atrac3Num++;
                maxSamples = 1024;
                atracCodec.setAtracMaxSamples(maxSamples);
            } else if (codecType == PSP_MODE_AT_3_PLUS && Modules.sceAtrac3plusModule.atrac3plusNum < Modules.sceAtrac3plusModule.atrac3plusMaxIDsCount) {
            	Modules.sceAtrac3plusModule.atrac3plusNum++;
                maxSamples = 2048;
                atracCodec.setAtracMaxSamples(maxSamples);
            } else {
                this.id = -1;
                this.atracCodec = null;
                maxSamples = 0;
            }

            lastDecodedSamples = maxSamples;
        }

        public void release() {
        	if (id >= 0) {
        		if (codecType == PSP_MODE_AT_3) {
        			Modules.sceAtrac3plusModule.atrac3Num--;
        		} else if (codecType == PSP_MODE_AT_3_PLUS) {
        			Modules.sceAtrac3plusModule.atrac3plusNum--;
	        	}
        	}
        	releaseContext();
        	releaseInternalBuffer();
        }

        public void createContext() {
        	if (atracContext == null) {
        		atracContext = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, String.format("ThreadMan-AtracCtx-%d", id), SysMemUserForUser.PSP_SMEM_High, 200, 0);
        		if (atracContext != null) {
	        		Memory mem = Memory.getInstance();
	        		int contextAddr = atracContext.addr;
	            	mem.memset(contextAddr, (byte) 0, atracContext.size);

	            	mem.write32(contextAddr + 140, 0); // Unknown
	    	        mem.write8(contextAddr + 149, (byte) 2); // Unknown.
	    	        mem.write8(contextAddr + 151, (byte) 1); // Unknown.
	    	        mem.write16(contextAddr + 154, (short) getAtracCodecType());
	    	        //mem.write32(contextAddr + 168, 0); // Voice associated to this Atrac context using __sceSasSetVoiceATRAC3?

	    	        // Used by SampleSourceAtrac3 (input for __sceSasConcatenateATRAC3):
	    	        mem.write32(contextAddr + ATRAC3_CONTEXT_READ_SIZE_OFFSET, getInputFileOffset());
	    	        mem.write32(contextAddr + ATRAC3_CONTEXT_REQUIRED_SIZE_OFFSET, getInputFileOffset()); 
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

        private void analyzeAtracHeader() {
            Memory mem = Memory.getInstance();

            int currentAddr = inputBufferAddr;
            int bufferSize = inputBufferSize;
            atracEndSample = -1;
            atracCurrentSample = 0;
            isSecondBufferNeeded = false;
            numLoops = 0;
            inputFileDataOffset = 0;

            if (bufferSize < 12) {
            	log.error(String.format("Atrac buffer too small %d", bufferSize));
            	return;
            }

            // RIFF file format:
            // Offset 0: 'RIFF'
            // Offset 4: file length - 8
            // Offset 8: 'WAVE'
            int RIFFMagic = readUnaligned32(mem, currentAddr);
            int WAVEMagic = readUnaligned32(mem, currentAddr + 8);
            if (RIFFMagic != RIFF_MAGIC || WAVEMagic != WAVE_MAGIC) {
            	log.error(String.format("Not a RIFF/WAVE format! %08X %08X", RIFFMagic, WAVEMagic));
            	return;
            }

            inputFileSize = readUnaligned32(mem, currentAddr + 4) + 8;
            currentAddr += 12;
            bufferSize -= 12;

            boolean foundData = false;
            while (bufferSize >= 8 && !foundData) {
            	int chunkMagic = readUnaligned32(mem, currentAddr);
            	int chunkSize = readUnaligned32(mem, currentAddr + 4);
            	currentAddr += 8;
            	bufferSize -= 8;
            	if (chunkSize > bufferSize) {
            		break;
            	}

            	switch (chunkMagic) {
            		case FMT_CHUNK_MAGIC: {
            			if (chunkSize >= 16) {
                            int compressionCode = mem.read16(currentAddr);
                            atracChannels = mem.read16(currentAddr + 2);
                            // Atrac3 has always 2 output channels.
                            // Atrac3+ has the same number of output channels as the number of channels in the input data.
                            setAtracOutputChannels(codecType == PSP_MODE_AT_3_PLUS ? atracChannels : 2);
                            atracSampleRate = readUnaligned32(mem, currentAddr + 4);
                            atracBitrate = readUnaligned32(mem, currentAddr + 8);
                            atracBytesPerFrame = mem.read16(currentAddr + 12);
                            int hiBytesPerSample = mem.read16(currentAddr + 14);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("WAVE format: magic=0x%08X('%s'), chunkSize=%d, compressionCode=0x%04X, channels=%d, outputChannels=%d, sampleRate=%d, bitrate=%d, bytesPerFrame=%d, hiBytesPerSample=%d", chunkMagic, getStringFromInt32(chunkMagic), chunkSize, compressionCode, atracChannels, atracOutputChannels, atracSampleRate, atracBitrate, atracBytesPerFrame, hiBytesPerSample));
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

            			}
            			break;
            		}
            		case FACT_CHUNK_MAGIC: {
            			if (chunkSize >= 8) {
            				atracEndSample = readUnaligned32(mem, currentAddr);
            				atracSampleOffset = readUnaligned32(mem, currentAddr + 4); // The loop samples are offset by this value
                            if (log.isDebugEnabled()) {
                            	log.debug(String.format("FACT Chunk: endSample=%d, sampleOffset=%d", atracEndSample, atracSampleOffset));
                            }
            			}
            			break;
            		}
            		case SMPL_CHUNK_MAGIC: {
            			if (chunkSize >= 36) {
            				int checkNumLoops = readUnaligned32(mem, currentAddr + 28);
    	                	if (chunkSize >= 36 + checkNumLoops * 24) {
        	                	numLoops = checkNumLoops;
	    	                	loops = new LoopInfo[numLoops];
	    	                	int loopInfoAddr = currentAddr + 36;
	    	                	for (int i = 0; i < numLoops; i++) {
	    	                		LoopInfo loop = new LoopInfo();
	    	                		loops[i] = loop;
	    	                		loop.cuePointID = readUnaligned32(mem, loopInfoAddr);
	    	                		loop.type = readUnaligned32(mem, loopInfoAddr + 4);
	    	                		loop.startSample = readUnaligned32(mem, loopInfoAddr + 8) - atracSampleOffset;
	    	                		loop.endSample = readUnaligned32(mem, loopInfoAddr + 12) - atracSampleOffset;
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
            		case DATA_CHUNK_MAGIC: {
            			foundData = true;
            			// Offset of the data chunk in the input file
            			inputFileDataOffset = currentAddr - inputBufferAddr;
            			break;
            		}
            	}

            	currentAddr += chunkSize;
            	bufferSize -= chunkSize;
            }

            if (loops != null) {
            	// If a loop end is past the atrac end, assume the atrac end
	            for (LoopInfo loop : loops) {
	            	if (loop.endSample > atracEndSample) {
	            		loop.endSample = atracEndSample;
	            	}
	            }
            }
        }

        public int getAtracId() {
        	return id;
        }

        public int getAtracCodecType() {
            return codecType;
        }

        public AtracCodec getAtracCodec() {
            return atracCodec;
        }

        public int getAtracBitrate() {
            return atracBitrate;
        }

        public int getAtracChannels() {
            return atracChannels;
        }

        public void setAtracChannels(int atracChannels) {
        	this.atracChannels = atracChannels;
        }

        public int getAtracSampleRate() {
            return atracSampleRate;
        }

        public int getAtracEndSample() {
            return atracEndSample;
        }

        public int getAtracCurrentSample() {
            return atracCurrentSample;
        }

        public int getAtracBytesPerFrame() {
            return atracBytesPerFrame;
        }

        public void setAtracCurrentSample(int sample) {
            atracCurrentSample = sample;
        }

        public int getLoopNum() {
        	if (!hasLoop()) {
        		return 0;
        	}
            return loopNum;
        }

        public void setLoopNum(int num) {
            loopNum = num;
        }

        public int getMaxSamples() {
        	return maxSamples;
        }

        public int getInputBufferAddr() {
            return inputBufferAddr;
        }

        public int getInputBufferSize() {
            return inputBufferSize;
        }

        public int getInputBufferOffset() {
            return inputBufferOffset;
        }

        public void setInputBufferOffset(int offset) {
            inputBufferOffset = offset;
        }

        public int getInputBufferWritableBytes() {
            return inputBufferWritableBytes;
        }

        public void setInputBufferWritableBytes(int bytes) {
            inputBufferWritableBytes = bytes;
        }

        public int getInputBufferNeededBytes() {
            return inputBufferNeededBytes;
        }

        public int getSecondInputBufferAddr() {
            return secondInputBufferAddr;
        }

        public int getSecondInputBufferSize() {
            return secondInputBufferSize;
        }

        public int getSecondInputBufferOffset() {
            return secondInputBufferOffset;
        }

        public int getSecondInputBufferWritableBytes() {
            return secondInputBufferWritableBytes;
        }

        public int getSecondInputBufferNeededBytes() {
            return secondInputBufferNeededBytes;
        }

        public int getInputFileSize() {
            return inputFileSize;
        }

        public void setInputFileSize(int bytes) {
            inputFileSize = bytes;
        }

        public int getInputFileOffset() {
            return inputFileOffset;
        }

        public void setInputFileOffset(int offset) {
            inputFileOffset = offset;
        }

        public int getSecondInputFileSize() {
            return secondInputFileSize;
        }

        public int getSecondInputFileOffset() {
            return secondInputFileOffset;
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

        public void setData(int buffer, int readSize, int bufferSize, boolean isSecondBuf) {
        	Emulator.getClock().pause();
            if (isSecondBuf) {
                // Set the second buffer, but don't handle it.
                // This allows it to be ignored while we can't handle it.
                // TODO: After AT3+ can be decoded, implement real post loop
                // data handling.
                secondInputBufferAddr = buffer;
                secondInputBufferSize = bufferSize;
                secondInputBufferOffset = bufferSize - readSize;
                secondInputBufferWritableBytes = bufferSize - readSize;
                isSecondBufferSet = true;
            } else {
                inputBufferAddr = buffer;
                inputBufferSize = bufferSize;
                inputBufferOffset = bufferSize - readSize;
                inputBufferWritableBytes = bufferSize - readSize;
                inputFileSize = readSize;
                inputFileOffset = readSize;
                secondInputFileSize = 0x100;
                secondInputFileOffset = inputFileSize - 0x100;
                forceAllDataIsOnMemory = false;
                forceReloadOfData = false;
                analyzeAtracHeader();
                log.info(String.format("hleAtracSetData atracID=%d, buffer=0x%08X, readSize=0x%X, bufferSize=0x%X, fileSize=0x%X", getAtracId(), buffer, readSize, inputBufferSize, inputFileSize));
                if (getAtracCodec() == null) {
                    log.warn(String.format("hleAtracSetData atracID=%d is invalid", getAtracId()));
                    return;
                }
                getAtracCodec().atracSetData(getAtracId(), getAtracCodecType(), buffer, readSize, inputFileSize);
            }
        	Emulator.getClock().resume();
        }

        protected void addStreamData(int length) {
            addStreamData(inputBufferAddr, length);
        }

        public void addStreamData(int address, int length) {
        	if (length > 0) {
	            inputBufferOffset -= length;
	            inputFileOffset += length;

	            forceReloadOfData = false;

	            getAtracCodec().atracAddStreamData(address, length);
        	}
        }

        public int getRemainFrames() {
            if (inputFileOffset >= inputFileSize || atracCurrentSample >= atracEndSample) {
                return PSP_ATRAC_ALLDATA_IS_ON_MEMORY;
            }

            if (forceReloadOfData) {
            	// The play position has just been reset, request more data
            	return 0;
            }

            // When playing an external audio, do not return directly
            //   PSP_ATRAC_ALLDATA_IS_ON_MEMORY
            // Some games expect to add some atrac data.

            if (forceAllDataIsOnMemory) {
            	return PSP_ATRAC_ALLDATA_IS_ON_MEMORY;
            }

            if (getInputBufferWritableBytes() <= 0) {
                // No space available in the input buffer, try to estimate the remaining frames.
                return inputBufferSize / atracBytesPerFrame;
            }

            int remainFrames = (inputBufferSize - inputBufferOffset) / atracBytesPerFrame;

            return remainFrames;
        }

        public void getBufferInfoForResetting(int sample, TPointer32 bufferInfoAddr) {
        	// Offset of the given sample in the input file.
        	// Assuming "atracBytesPerFrame" bytes in the input file for each "maxSamples" samples.
        	int inputFileSampleOffset = inputFileDataOffset + sample / maxSamples * atracBytesPerFrame;

        	// Holds buffer related parameters.
            // Main buffer.
            bufferInfoAddr.setValue(0, inputBufferAddr);                 // Pointer to current writing position in the buffer.
            bufferInfoAddr.setValue(4, inputBufferWritableBytes);        // Number of bytes which can be written to the buffer.
            bufferInfoAddr.setValue(8, inputBufferNeededBytes);          // Number of bytes that must to be written to the buffer.
            bufferInfoAddr.setValue(12, inputFileSampleOffset);          // Read offset in the input file for the given sample.
            // Secondary buffer.
            bufferInfoAddr.setValue(16, secondInputBufferAddr);          // Pointer to current writing position in the buffer.
            bufferInfoAddr.setValue(20, secondInputBufferWritableBytes); // Number of bytes which can be written to the buffer.
            bufferInfoAddr.setValue(24, secondInputBufferNeededBytes);   // Number of bytes that must to be written to the buffer.
            bufferInfoAddr.setValue(28, secondInputFileOffset);          // Read offset for input file.
        }

        public void setDecodedSamples(int samples) {
        	int currentSample = getAtracCurrentSample();
        	int nextCurrentSample = currentSample + samples;

        	for (int i = 0; i < numLoops; i++) {
        		LoopInfo loop = loops[i];
        		if (currentSample <= loop.startSample && loop.startSample < nextCurrentSample) {
        			// We are just starting a loop
        			loopStartBytesWrittenFirstBuf = inputFileOffset;
        			loopStartBytesWrittenSecondBuf = secondInputFileOffset;
        			currentLoopNum = i;
        			break;
        		} else if (currentSample <= loop.endSample && loop.endSample <= nextCurrentSample && currentLoopNum == i) {
        			// We are just ending the current loop
        			if (loopNum == 0) {
        				// No more loop playback
        				currentLoopNum = -1;
        			} else {
        				// Replay the loop
        				log.info(String.format("Replaying atrac loop atracID=%d, loopStart=%d, loopEnd=%d", id, loop.startSample, loop.endSample));
        				setPlayPosition(loop.startSample, loopStartBytesWrittenFirstBuf, loopStartBytesWrittenSecondBuf);
        				nextCurrentSample = loop.startSample;
        				// loopNum < 0: endless loop playback
        				// loopNum > 0: play the loop loopNum times
        				if (loopNum > 0) {
        					loopNum--;
        				}
        				break;
        			}
        		}
        	}
        	setAtracCurrentSample(nextCurrentSample);
        }

        public void setPlayPosition(int sample, int bytesWrittenFirstBuf, int bytesWrittenSecondBuf) {
        	if (sample != getAtracCurrentSample()) {
	            setInputBufferOffset(getInputBufferSize() - bytesWrittenFirstBuf);
	            setAtracCurrentSample(sample);
	            getAtracCodec().atracResetPlayPosition(sample);
	            int position = getAtracCodec().getChannelPosition();
	            if (position >= 0) {
	            	setInputFileOffset(position);
	            } else {
		            setInputFileOffset(0);
	            }

	            // No need to retrieve new atrac data if the buffer contains
	            // the whole atrac file or if we are using an external audio.
	            if (getInputBufferSize() < getInputFileSize() && !getAtracCodec().isExternalAudio()) {
	            	getAtracCodec().resetChannel();
	            	forceReloadOfData = true;
	            } else {
	            	forceAllDataIsOnMemory = true;
	            }
        	}
        }

        private boolean hasLoop() {
        	return numLoops > 0;
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
        	return loops[0].startSample;
        }

        public int getLoopEndSample() {
        	if (!hasLoop()) {
        		return -1;
        	}

        	return loops[0].endSample;
        }

        public void update() {
            setInputBufferWritableBytes(getInputBufferOffset());
            if (getInputFileOffset() >= getInputFileSize()) {
                // All data is in the buffer
                setInputBufferWritableBytes(0);
            } else if (getInputBufferWritableBytes() > (getInputFileSize() - getInputFileOffset())) {
                // Do not need more data than input file size
                setInputBufferWritableBytes(getInputFileSize() - getInputFileOffset());
            }
        }

        public boolean isForceReloadOfData() {
        	return forceReloadOfData;
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

        @Override
		public String toString() {
			return String.format("AtracID[id=%d, inputBufferAddr=0x%08X, inputBufferSize=%d, inputBufferOffset=%d, inputBufferWritableBytes=%d, inputBufferNeededBytes=%d]", id, inputBufferAddr, inputBufferSize, inputBufferOffset, inputBufferWritableBytes, inputBufferNeededBytes);
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

		public int getAtracOutputChannels() {
			return atracOutputChannels;
		}

		public void setAtracOutputChannels(int atracOutputChannels) {
			this.atracOutputChannels = atracOutputChannels;
		}
    }

    private static class EnableConnectorSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnableConnector(value);
		}
    }

    public static boolean isEnableConnector() {
        return useAtracCodec;
    }

    private static void setEnableConnector(boolean useConnector) {
        useAtracCodec = useConnector;
    }

    protected static String getStringFromInt32(int n) {
    	char c1 = (char) ((n      ) & 0xFF);
    	char c2 = (char) ((n >>  8) & 0xFF);
    	char c3 = (char) ((n >> 16) & 0xFF);
    	char c4 = (char) ((n >> 24) & 0xFF);

    	return String.format("%c%c%c%c", c1, c2, c3, c4);
    }

    public int getBytesPerFrame(int atID) {
    	return atracIDs.get(atID).getAtracBytesPerFrame();
    }

    protected int getRemainFrames(AtracID id, int samples) {
    	id.update();
        int remainFrames = id.getRemainFrames();

        if (id.getInputFileOffset() < id.getInputFileSize()) {
            if (remainFrames > 0 && samples < id.getMaxSamples()) {
                // If we could not decode all the requested samples, request more data
            	id.setInputBufferOffset(id.getInputBufferSize());
            	remainFrames = 0;
            } else if (id.getAtracCodec().getChannelLength() < 32768) {
            	// The media engine is reading chunks of 32768 bytes from the channel.
                // If the channel contains less than one chunk, request more data.
            	id.setInputBufferOffset(id.getInputBufferSize());
            	remainFrames = 0;
            }
        }

        return remainFrames;
    }

    protected void hleAtracReinit(int newAT3IdCount, int newAT3plusIdCount) {
        atrac3MaxIDsCount = newAT3IdCount;
        atrac3plusMaxIDsCount = newAT3plusIdCount;
    }

    protected int hleCreateAtracID(int codecType) {
    	// "Patapon 2" expects the ID to be signed 8bit
    	int atracID = SceUidManager.getNewId(idPurpose, 0, 255);
        AtracCodec atracCodec = new AtracCodec();
        AtracID id = new AtracID(atracID, codecType, atracCodec);
        if (id.getAtracId() >= 0) {
            atracIDs.put(atracID, id);
            return atracID;
        }
        return SceKernelErrors.ERROR_ATRAC_NO_ID;
    }

    protected void hleReleaseAtracID(int atracID) {
    	AtracID id = atracIDs.remove(atracID);
    	SceUidManager.releaseId(atracID, idPurpose);
    	id.release();
    }

    public static int getCodecType(int address) {
        int at3magic = Memory.getInstance().read16(address + 20);
        if (at3magic == AT3_MAGIC) {
            return PSP_MODE_AT_3;
        } else if (at3magic == AT3_PLUS_MAGIC) {
            return PSP_MODE_AT_3_PLUS;
        }

        return 0; // Unknown Codec
    }

    public int checkAtracID(int atID) {
    	atID &= atracIDMask;

    	if (!atracIDs.containsKey(atID)) {
    		log.warn(String.format("Unknown atracID=0x%X", atID));
            throw new SceKernelErrorException(SceKernelErrors.ERROR_ATRAC_BAD_ID);
    	}

    	return atID;
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
        int atId = hleCreateAtracID(codecType);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetAtracID: returning atracID=0x%08X", atId));
        }

        return atId;
    }

    @HLEFunction(nid = 0x61EB33F5, version = 150, checkInsideInterrupt = true)
    public int sceAtracReleaseAtracID(@CheckArgument("checkAtracID") int atID) {
        AtracCodec atracCodec = atracIDs.get(atID).getAtracCodec();
        if (atracCodec != null) {
            atracCodec.finish();
        }
        hleReleaseAtracID(atID);

        return 0;
    }

    @HLEFunction(nid = 0x0E2A73AB, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetData(@CheckArgument("checkAtracID") int atID, TPointer buffer, int bufferSize) {
        atracIDs.get(atID).setData(buffer.getAddress(), bufferSize, bufferSize, false);

        return 0;
    }

    @HLEFunction(nid = 0x3F6E26B5, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetHalfwayBuffer(@CheckArgument("checkAtracID") int atID, TPointer halfBuffer, int readSize, int halfBufferSize) {
        atracIDs.get(atID).setData(halfBuffer.getAddress(), readSize, halfBufferSize, false);

        return 0;
    }

    @HLEFunction(nid = 0x7A20E7AF, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetDataAndGetID(TPointer buffer, int bufferSize) {
        if (bufferSize < 0) {
        	// Unknown error
        	return -1;
        }

        int codecType = getCodecType(buffer.getAddress());
        int atID = hleCreateAtracID(codecType);
        if (atracIDs.containsKey(atID)) {
            atracIDs.get(atID).setData(buffer.getAddress(), bufferSize, bufferSize, false);
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetDataAndGetID returning atracID=0x%08X", atID));
        }

        return atID;
    }

    @HLEFunction(nid = 0x0FAE370E, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetHalfwayBufferAndGetID(TPointer halfBuffer, int readSize, int halfBufferSize) {
        if (readSize > halfBufferSize) {
        	return SceKernelErrors.ERROR_ATRAC_INCORRECT_READ_SIZE;
        }

        if (readSize < 0 || halfBufferSize < 0) {
        	// Unknown error
        	return -1;
        }

        int codecType = getCodecType(halfBuffer.getAddress());
        int atID = hleCreateAtracID(codecType);
        if (atracIDs.containsKey(atID)) {
            atracIDs.get(atID).setData(halfBuffer.getAddress(), readSize, halfBufferSize, false);
        }

        return atID;
    }

    @HLEFunction(nid = 0x6A8C3CD5, version = 150, checkInsideInterrupt = true)
    public int sceAtracDecodeData(@CheckArgument("checkAtracID") int atID, TPointer samplesAddr, @CanBeNull TPointer32 samplesNbrAddr, @CanBeNull TPointer32 outEndAddr, @CanBeNull TPointer32 remainFramesAddr) {
        if (atracIDs.get(atID).isSecondBufferNeeded() && !atracIDs.get(atID).isSecondBufferSet()) {
            log.warn(String.format("sceAtracDecodeData atracID=0x%X needs second buffer!", atID));
            return SceKernelErrors.ERROR_ATRAC_SECOND_BUFFER_NEEDED;
        }

        AtracID id = atracIDs.get(atID);
        int result = 0;
        AtracCodec atracCodec = id.getAtracCodec();
        int samples = 0;
        int end = 1;
        if (id.isForceReloadOfData()) {
        	result = SceKernelErrors.ERROR_ATRAC_BUFFER_IS_EMPTY;
        	end = 0;
        } else if (atracCodec != null) {
            samples = atracCodec.atracDecodeData(atID, samplesAddr.getAddress(), id.getAtracOutputChannels());
            if (samples < 0) {
                // Not using decoded data.
                if (log.isDebugEnabled()) {
                    log.debug("sceAtracDecodeData faked samples");
                }
                samples = id.getMaxSamples();
                if (id.getAtracCurrentSample() >= id.getAtracEndSample()) {
                    samples = 0;
                    // No more data in input buffer
                    result = SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
                    end = 1;
                } else {
                    end = 0;
                }

                samplesAddr.clear(samples * id.getAtracOutputChannels() * 2);  // 2 empty bytes per sample and channel.
            } else if (samples == 0) {
                // Using decoded data and all samples have been decoded.
                result = SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
                end = 1;
            } else {
                // Using decoded data.
                end = atracCodec.getAtracEnd();
            }
        }
        if (samples > 0) {
            int consumedInputBytes = id.getAtracBytesPerFrame();
            if (consumedInputBytes < 0) {
                consumedInputBytes = 0;
            } else if (id.getInputBufferOffset() + consumedInputBytes > id.getInputBufferSize()) {
                consumedInputBytes = id.getInputBufferSize() - id.getInputBufferOffset();
            }
            id.setInputBufferOffset(id.getInputBufferOffset() + consumedInputBytes);
        }
        if (samples > 0) {
            id.setDecodedSamples(samples);
            if (id.getAtracCurrentSample() >= id.getAtracEndSample()) {
            	// The PSP is already setting the end flag when returning the last samples.
            	end = 1;
            }
        }
        id.setLastDecodedSamples(samples);
        int remainFrames;
        if (end == 1) {
            remainFrames = -1;
        } else {
        	remainFrames = getRemainFrames(id, samples);
        }
        samplesNbrAddr.setValue(samples);
        outEndAddr.setValue(end);
        remainFramesAddr.setValue(remainFrames);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracDecodeData returning 0x%08X, samples=%d, end=%d, remainFrames=%d, currentSample=%d/%d, %s", result, samples, end, remainFrames, id.getAtracCurrentSample(), id.getAtracEndSample(), id.toString()));
        }

        // Delay the thread decoding the Atrac data,
        // the thread is also blocking using semaphores/event flags on a real PSP.
        if (result == 0) {
        	Modules.ThreadManForUserModule.hleKernelDelayThread(atracDecodeDelay, false);
        }

        return result;
    }

    @HLEFunction(nid = 0x9AE849A7, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetRemainFrame(@CheckArgument("checkAtracID") int atID, TPointer32 remainFramesAddr) {
        AtracID id = atracIDs.get(atID);
    	int remainFrames = getRemainFrames(id, id.getLastDecodedSamples());
		remainFramesAddr.setValue(remainFrames);

		if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetRemainFrame returning %d, %s", remainFrames, id.toString()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x5D268707, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetStreamDataInfo(@CheckArgument("checkAtracID") int atID, @CanBeNull TPointer32 writeAddr, @CanBeNull TPointer32 writableBytesAddr, @CanBeNull TPointer32 readOffsetAddr) {
        AtracID id = atracIDs.get(atID);
        id.update();
        writeAddr.setValue(id.getInputBufferAddr());
        writableBytesAddr.setValue(id.getInputBufferWritableBytes());
        readOffsetAddr.setValue(id.getInputFileOffset());

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetStreamDataInfo write=0x%08X, writableBytes=%d, readOffset=%d, %s", writeAddr.getValue(), writableBytesAddr.getValue(), readOffsetAddr.getValue(), id.toString()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x7DB31251, version = 150, checkInsideInterrupt = true)
    public int sceAtracAddStreamData(@CheckArgument("checkAtracID") int atID, int bytesToAdd) {
        AtracID id = atracIDs.get(atID);
        id.addStreamData(bytesToAdd);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracAddStreamData: %s", id.toString()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x83E85EA0, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetSecondBufferInfo(@CheckArgument("checkAtracID") int atID, @CanBeNull TPointer32 outPosition, @CanBeNull TPointer32 outBytes) {
        AtracID id = atracIDs.get(atID);
        if (!id.isSecondBufferNeeded()) {
            return SceKernelErrors.ERROR_ATRAC_SECOND_BUFFER_NOT_NEEDED;
        }

        outPosition.setValue(id.getSecondInputFileOffset());
        outBytes.setValue(id.getSecondInputFileSize());

        return 0;
    }

    @HLEFunction(nid = 0x83BF7AFD, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetSecondBuffer(@CheckArgument("checkAtracID") int atID, TPointer secondBuffer, int secondBufferSize) {
        AtracID id = atracIDs.get(atID);
        id.setData(secondBuffer.getAddress(), secondBufferSize, secondBufferSize, true);

        return 0;
    }

    @HLEFunction(nid = 0xE23E3A35, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetNextDecodePosition(@CheckArgument("checkAtracID") int atID, TPointer32 posAddr) {
        AtracID id = atracIDs.get(atID);
        if (id.getAtracCurrentSample() >= id.getAtracEndSample()) {
            return SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
        }

        posAddr.setValue(id.getAtracCurrentSample());

        return 0;
    }

    @HLEFunction(nid = 0xA2BBA8BE, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetSoundSample(@CheckArgument("checkAtracID") int atID, @CanBeNull TPointer32 endSampleAddr, @CanBeNull TPointer32 loopStartSampleAddr, @CanBeNull TPointer32 loopEndSampleAddr) {
    	AtracID id = atracIDs.get(atID);
    	int endSample = id.getAtracEndSample();
        int loopStartSample = id.getLoopStartSample();
        int loopEndSample = id.getLoopEndSample();
    	if (endSample < 0) {
    		endSample = id.getAtracCodec().getAtracEndSample();
    	}
        if (endSample < 0) {
            endSample = id.getInputFileSize();
        }
        endSampleAddr.setValue(endSample);
        loopStartSampleAddr.setValue(loopStartSample);
        loopEndSampleAddr.setValue(loopEndSample);

        return 0;
    }

    @HLEFunction(nid = 0x31668BAA, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetChannel(@CheckArgument("checkAtracID") int atID, TPointer32 channelAddr) {
    	AtracID id = atracIDs.get(atID);
        channelAddr.setValue(id.getAtracChannels());

        return 0;
    }

    @HLEFunction(nid = 0xD6A5F2F7, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetMaxSample(@CheckArgument("checkAtracID") int atID, TPointer32 maxSamplesAddr) {
    	AtracID id = atracIDs.get(atID);
        maxSamplesAddr.setValue(id.getMaxSamples());

        return 0;
    }

    @HLEFunction(nid = 0x36FAABFB, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetNextSample(@CheckArgument("checkAtracID") int atID, TPointer32 nbrSamplesAddr) {
    	AtracID id = atracIDs.get(atID);
        int samples = id.getMaxSamples();
        if (id.getInputBufferOffset() >= id.getInputBufferSize()) {
            samples = 0; // No more data available in input buffer
        }
        nbrSamplesAddr.setValue(samples);

        return 0;
    }

    @HLEFunction(nid = 0xA554A158, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetBitrate(@CheckArgument("checkAtracID") int atID, TPointer32 bitrateAddr) {
    	AtracID id = atracIDs.get(atID);
        bitrateAddr.setValue(id.getAtracBitrate());

        return 0;
    }

    @HLEFunction(nid = 0xFAA4F89B, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetLoopStatus(@CheckArgument("checkAtracID") int atID, @CanBeNull TPointer32 loopNbr, @CanBeNull TPointer32 statusAddr) {
    	AtracID id = atracIDs.get(atID);
        loopNbr.setValue(id.getLoopNum());
        statusAddr.setValue(id.getLoopStatus());

        return 0;
    }

    @HLEFunction(nid = 0x868120B5, version = 150, checkInsideInterrupt = true)
    public int sceAtracSetLoopNum(@CheckArgument("checkAtracID") int atID, int loopNbr) {
    	AtracID id = atracIDs.get(atID);
        id.setLoopNum(loopNbr);
        id.getAtracCodec().setAtracLoopCount(loopNbr);

        return 0;
    }

    @HLEFunction(nid = 0xCA3CA3D2, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetBufferInfoForReseting(@CheckArgument("checkAtracID") int atID, int sample, TPointer32 bufferInfoAddr) {
    	AtracID id = atracIDs.get(atID);
        id.getBufferInfoForResetting(sample, bufferInfoAddr);

        return 0;
    }

    @HLEFunction(nid = 0x644E5607, version = 150, checkInsideInterrupt = true)
    public int sceAtracResetPlayPosition(@CheckArgument("checkAtracID") int atID, int sample, int bytesWrittenFirstBuf, int bytesWrittenSecondBuf) {
        AtracID id = atracIDs.get(atID);
        id.setPlayPosition(sample, bytesWrittenFirstBuf, bytesWrittenSecondBuf);

        return 0;
    }

    @HLEFunction(nid = 0xE88F759B, version = 150, checkInsideInterrupt = true)
    public int sceAtracGetInternalErrorInfo(@CheckArgument("checkAtracID") int atID, TPointer32 errorAddr) {
        AtracID id = atracIDs.get(atID);
        errorAddr.setValue(id.getInternalErrorInfo());

        return 0;
    }
}