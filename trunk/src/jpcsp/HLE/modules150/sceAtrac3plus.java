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

import jpcsp.HLE.HLEFunction;
import java.util.HashMap;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.connector.AtracCodec;
import jpcsp.settings.AbstractBoolSettingsListener;

import org.apache.log4j.Logger;

public class sceAtrac3plus extends HLEModule {

    protected static Logger log = Modules.getLogger("sceAtrac3plus");

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

    public static final int PSP_ATRAC_ALLDATA_IS_ON_MEMORY = -1;
    public static final int PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY = -2;
    public static final int PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY = -3;

    protected static final int PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA = 0;
    protected static final int PSP_ATRAC_STATUS_LOOP_STREAM_DATA = 1;

    protected static final int PSP_MODE_AT_3_PLUS = 0x00001000;
    protected static final int PSP_MODE_AT_3 = 0x00001001;

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

    protected class AtracID {
        // Internal info.
    	protected int id;
        protected int codecType;
        protected AtracCodec atracCodec;
        // Sound data.
        protected int atracBitrate = 64;
        protected int atracChannels = 2;
        protected int atracSampleRate = 0xAC44;
        protected int atracBytesPerFrame = 0x0230;
        protected int atracEndSample;
        protected int atracCurrentSample;
        protected int maxSamples;
        protected int atracSampleOffset;
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
        // Loops
        protected int loopNum;
        protected int numLoops;
        protected LoopInfo[] loops;
        protected int loopStartBytesWrittenFirstBuf;
        protected int loopStartBytesWrittenSecondBuf;
        protected int currentLoopNum = -1;
        protected boolean forceReloadOfData;
        protected boolean forceAllDataIsOnMemory;

        public AtracID(int id, int codecType, AtracCodec atracCodec) {
            this.codecType = codecType;
            this.id = id;
            this.atracCodec = atracCodec;
            if ((codecType == PSP_MODE_AT_3) && (atrac3Num < atrac3MaxIDsCount)) {
                atrac3Num++;
                maxSamples = 1024;
                atracCodec.setAtracMaxSamples(maxSamples);
            } else if ((codecType == PSP_MODE_AT_3_PLUS) && (atrac3plusNum < atrac3plusMaxIDsCount)) {
                atrac3plusNum++;
                maxSamples = 2048;
                atracCodec.setAtracMaxSamples(maxSamples);
            } else {
                this.id = -1;
                this.atracCodec = null;
                maxSamples = 0;
            }
        }

        public void release() {
        	if (id >= 0) {
        		if (codecType == PSP_MODE_AT_3) {
        			atrac3Num--;
        		} else if (codecType == PSP_MODE_AT_3_PLUS) {
        			atrac3plusNum--;
	        	}
        	}
        }

        private void analyzeAtracHeader() {
            Memory mem = Memory.getInstance();

            int currentAddr = inputBufferAddr;
            int bufferSize = inputBufferSize;
            atracEndSample = -1;
            atracCurrentSample = 0;
            isSecondBufferNeeded = false;
            numLoops = 0;

            if (bufferSize < 12) {
            	log.error(String.format("Atrac buffer too small %d", bufferSize));
            	return;
            }

            // RIFF file format:
            // Offset 0: 'RIFF'
            // Offset 4: file length - 8
            // Offset 8: 'WAVE'
            int RIFFMagic = mem.read32(currentAddr);
            int WAVEMagic = mem.read32(currentAddr + 8);
            if (RIFFMagic != RIFF_MAGIC || WAVEMagic != WAVE_MAGIC) {
            	log.error(String.format("Not a RIFF/WAVE format! %08X %08X", RIFFMagic, WAVEMagic));
            	return;
            }

            inputFileSize = mem.read32(currentAddr + 4) + 8;
            currentAddr += 12;
            bufferSize -= 12;

            boolean foundData = false;
            while (bufferSize >= 8 && !foundData) {
            	int chunkMagic = mem.read32(currentAddr);
            	int chunkSize = mem.read32(currentAddr + 4);
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
                            atracSampleRate = mem.read32(currentAddr + 4);
                            atracBitrate = mem.read32(currentAddr + 8);
                            atracBytesPerFrame = mem.read16(currentAddr + 12);
                            int hiBytesPerSample = mem.read16(currentAddr + 14);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("WAVE format: magic=0x%08X('%s'), chunkSize=%d, compressionCode=0x%04X, channels=%d, sampleRate=%d, bitrate=%d, chunkAlign=%d, hiBytesPerSample=%d", chunkMagic, getStringFromInt32(chunkMagic), chunkSize, compressionCode, atracChannels, atracSampleRate, atracBitrate, atracBytesPerFrame, hiBytesPerSample));
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
                            atracEndSample = mem.read32(currentAddr);
                            atracSampleOffset = mem.read32(currentAddr + 4); // The loop samples are offset by this value
                            if (log.isDebugEnabled()) {
                            	log.debug(String.format("FACT Chunk: endSample=%d, sampleOffset=%d", atracEndSample, atracSampleOffset));
                            }
            			}
            			break;
            		}
            		case SMPL_CHUNK_MAGIC: {
            			if (chunkSize >= 36) {
            				int checkNumLoops = mem.read32(currentAddr + 28);
    	                	if (chunkSize >= 36 + checkNumLoops * 24) {
        	                	numLoops = checkNumLoops;
	    	                	loops = new LoopInfo[numLoops];
	    	                	int loopInfoAddr = currentAddr + 36;
	    	                	for (int i = 0; i < numLoops; i++) {
	    	                		LoopInfo loop = new LoopInfo();
	    	                		loops[i] = loop;
	    	                		loop.cuePointID = mem.read32(loopInfoAddr);
	    	                		loop.type = mem.read32(loopInfoAddr + 4);
	    	                		loop.startSample = mem.read32(loopInfoAddr + 8) - atracSampleOffset;
	    	                		loop.endSample = mem.read32(loopInfoAddr + 12) - atracSampleOffset;
	    	                		loop.fraction = mem.read32(loopInfoAddr + 16);
	    	                		loop.playCount = mem.read32(loopInfoAddr + 20);

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
            			break;
            		}
            	}

            	currentAddr += chunkSize;
            	bufferSize -= chunkSize;
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
        	if (length > 0) {
	            inputFileOffset += length;
	            inputBufferOffset -= length;

	            forceReloadOfData = false;

	            getAtracCodec().atracAddStreamData(inputBufferAddr, length);
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

        public void getBufferInfoForReseting(int sample, int bufferInfoAddr) {
            Memory mem = Memory.getInstance();

            if (Memory.isAddressGood(bufferInfoAddr)) {
                // Holds buffer related parameters.
                // Main buffer.
                mem.write32(bufferInfoAddr, inputBufferAddr);                     // Pointer to current writing position in the buffer.
                mem.write32(bufferInfoAddr + 4, inputBufferWritableBytes);        // Number of bytes which can be written to the buffer.
                mem.write32(bufferInfoAddr + 8, inputBufferNeededBytes);          // Number of bytes that must to be written to the buffer.
                mem.write32(bufferInfoAddr + 12, inputFileOffset);                // Read offset for input file.
                // Secondary buffer.
                mem.write32(bufferInfoAddr + 16, secondInputBufferAddr);          // Pointer to current writing position in the buffer.
                mem.write32(bufferInfoAddr + 20, secondInputBufferWritableBytes); // Number of bytes which can be written to the buffer.
                mem.write32(bufferInfoAddr + 24, secondInputBufferNeededBytes);   // Number of bytes that must to be written to the buffer.
                mem.write32(bufferInfoAddr + 28, secondInputFileOffset);          // Read offset for input file.
            }
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

        @Override
		public String toString() {
			return String.format("AtracID[id=%d, inputBufferAddr=0x%08X, inputBufferSize=%d, inputBufferOffset=%d, inputBufferWritableBytes=%d, inputBufferNeededBytes=%d]", id, inputBufferAddr, inputBufferSize, inputBufferOffset, inputBufferWritableBytes, inputBufferNeededBytes);
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

    protected String getStringFromInt32(int n) {
    	char c1 = (char) ((n      ) & 0xFF);
    	char c2 = (char) ((n >>  8) & 0xFF);
    	char c3 = (char) ((n >> 16) & 0xFF);
    	char c4 = (char) ((n >> 24) & 0xFF);

    	return String.format("%c%c%c%c", c1, c2, c3, c4);
    }

    public int getBytesPerFrame(int atID) {
    	return atracIDs.get(atID).getAtracBytesPerFrame();
    }

    protected int getRemainFrames(AtracID id) {
    	return getRemainFrames(id, id.getMaxSamples());
    }

    protected int getRemainFrames(AtracID id, int samples) {
    	id.update();
        int remainFrames = id.getRemainFrames();

        if (id.getInputFileOffset() < id.getInputFileSize()) {
            if (remainFrames > 0 && samples < id.getMaxSamples()) {
                // If we could not decode all the requested samples, request more data
            	id.setInputBufferOffset(id.getInputBufferSize());
            	remainFrames = 0;
            } else if (id.getAtracCodec().getChannelLength() <= 0) {
                // If the channel is empty, request more data
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

    @HLEFunction(nid = 0xD1F59FDB, version = 150, checkInsideInterrupt = true)
    public void sceAtracStartEntry(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceAtracStartEntry [0xD1F59FDB]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xD5C28CC0, version = 150, checkInsideInterrupt = true)
    public void sceAtracEndEntry(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceAtracEndEntry [0xD5C28CC0]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x780F88D1, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetAtracID(Processor processor) {
        CpuState cpu = processor.cpu;

        int codecType = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracGetAtracID: codecType = 0x" + Integer.toHexString(codecType));
        }

        
        cpu.gpr[2] = hleCreateAtracID(codecType);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetAtracID: returning atracID=0x%08X", cpu.gpr[2]));
        }
    }

    @HLEFunction(nid = 0x61EB33F5, version = 150, checkInsideInterrupt = true)
    public void sceAtracReleaseAtracID(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;

        if (log.isDebugEnabled()) {
            log.debug("sceAtracReleaseAtracID: atracID = " + atID);
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracReleaseAtracID: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracCodec atracCodec = atracIDs.get(atID).getAtracCodec();
            if (atracCodec != null) {
                atracCodec.finish();
            }
            hleReleaseAtracID(atID);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x0E2A73AB, version = 150, checkInsideInterrupt = true)
    public void sceAtracSetData(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int buffer = cpu.gpr[5];
        int bufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetData: atID = %d, buffer = 0x%08X, bufferSize = 0x%08X", atID, buffer, bufferSize));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setData(buffer, bufferSize, bufferSize, false);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x3F6E26B5, version = 150, checkInsideInterrupt = true)
    public void sceAtracSetHalfwayBuffer(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int halfBuffer = cpu.gpr[5];
        int readSize = cpu.gpr[6];
        int halfBufferSize = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetHalfwayBuffer: atID = %d, buffer = 0x%08X, readSize = 0x%08X, bufferSize = 0x%08X", atID, halfBuffer, readSize, halfBufferSize));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetHalfwayBuffer: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setData(halfBuffer, readSize, halfBufferSize, false);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x7A20E7AF, version = 150, checkInsideInterrupt = true)
    public void sceAtracSetDataAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        int buffer = cpu.gpr[4];
        int bufferSize = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetDataAndGetID buffer = 0x%08X, bufferSize = 0x%08X", buffer, bufferSize));
        }

        
        int atID = 0;
        if (Memory.isAddressGood(buffer)) {
        	int codecType = getCodecType(buffer);
            atID = hleCreateAtracID(codecType);
            if (atracIDs.containsKey(atID)) {
                atracIDs.get(atID).setData(buffer, bufferSize, bufferSize, false);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetDataAndGetID returning atracID=0x%08X", atID));
        }
        cpu.gpr[2] = atID;
    }

    @HLEFunction(nid = 0x0FAE370E, version = 150, checkInsideInterrupt = true)
    public void sceAtracSetHalfwayBufferAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        int halfBuffer = cpu.gpr[4];
        int readSize = cpu.gpr[5];
        int halfBufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetHalfwayBufferAndGetID buffer = 0x%08X, readSize = 0x%08X, bufferSize = 0x%08X", halfBuffer, readSize, halfBufferSize));
        }

        
        int atID = 0;
        if (Memory.isAddressGood(halfBuffer)) {
        	int codecType = getCodecType(halfBuffer);
            atID = hleCreateAtracID(codecType);
            if (atracIDs.containsKey(atID)) {
                atracIDs.get(atID).setData(halfBuffer, readSize, halfBufferSize, false);
            }
        }
        cpu.gpr[2] = atID;
    }

    @HLEFunction(nid = 0x6A8C3CD5, version = 150, checkInsideInterrupt = true)
    public void sceAtracDecodeData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4] & atracIDMask;
        int samplesAddr = cpu.gpr[5];
        int samplesNbrAddr = cpu.gpr[6];
        int outEndAddr = cpu.gpr[7];
        int remainFramesAddr = cpu.gpr[8];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracDecodeData: atracID=%d, samplesAddr=0x%08X, samplesNbrAddr=0x%08X, outEndAddr=0x%08X, remainFramesAddr=0x%08X",
                    atID, samplesAddr, samplesNbrAddr, outEndAddr, remainFramesAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracDecodeData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else if (atracIDs.get(atID).isSecondBufferNeeded() && !atracIDs.get(atID).isSecondBufferSet()) {
            log.warn("sceAtracDecodeData: atracID= " + atID + ", needs second buffer!");
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_SECOND_BUFFER_NEEDED;
        } else {
            AtracID id = atracIDs.get(atID);
            int result = 0;
            AtracCodec atracCodec = id.getAtracCodec();
            int samples = 0;
            int end = 1;
            if (id.isForceReloadOfData()) {
            	result = SceKernelErrors.ERROR_ATRAC_BUFFER_IS_EMPTY;
            	end = 0;
            } else if (atracCodec != null) {
                samples = atracCodec.atracDecodeData(atID, samplesAddr);
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

                    if (Memory.isAddressGood(samplesAddr)) {
                        mem.memset(samplesAddr, (byte) 0, samples * 4);  // 4 empty bytes per sample.
                    }
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
            int remainFrames;
            if (end == 1) {
                remainFrames = -1;
            } else {
            	remainFrames = getRemainFrames(id, samples);
            }
            if (Memory.isAddressGood(samplesNbrAddr)) {
                mem.write32(samplesNbrAddr, samples);
            }
            if (Memory.isAddressGood(outEndAddr)) {
                mem.write32(outEndAddr, end);
            }
            if (Memory.isAddressGood(remainFramesAddr)) {
                mem.write32(remainFramesAddr, remainFrames);
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAtracDecodeData returning 0x%08X, samples=%d, end=%d, remainFrames=%d, currentSample=%d/%d, %s", result, samples, end, remainFrames, id.getAtracCurrentSample(), id.getAtracEndSample(), id.toString()));
            }

            cpu.gpr[2] = result;
            // Delay the thread decoding the Atrac data,
            // the thread is also blocking using semaphores/event flags on a real PSP.
            if (result == 0) {
            	Modules.ThreadManForUserModule.hleKernelDelayThread(atracDecodeDelay, false);
            }
        }
    }

    @HLEFunction(nid = 0x9AE849A7, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetRemainFrame(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4] & atracIDMask;
        int remainFramesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetRemainFrame: atracID = %d, remainFramesAddr = 0x%08X", atID, remainFramesAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetRemainFrame: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
        	AtracID id = atracIDs.get(atID);
        	int remainFrames = getRemainFrames(id);
        	if (Memory.isAddressGood(remainFramesAddr)) {
        		mem.write32(remainFramesAddr, remainFrames);
        	}
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAtracGetRemainFrame: returning %d, %s", remainFrames, id.toString()));
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x5D268707, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetStreamDataInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int writeAddr = cpu.gpr[5];
        int writableBytesAddr = cpu.gpr[6];
        int readOffsetAddr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetStreamDataInfo: atID=%d, writeAddr=0x%08X, writableBytesAddr=0x%08X, readOffsetAddr=0x%08X", atID, writeAddr, writableBytesAddr, readOffsetAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetStreamDataInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracID id = atracIDs.get(atID);
            id.update();
            if (Memory.isAddressGood(writeAddr)) {
                mem.write32(writeAddr, id.getInputBufferAddr());
            }
            if (Memory.isAddressGood(writableBytesAddr)) {
                mem.write32(writableBytesAddr, id.getInputBufferWritableBytes());
            }
            if (Memory.isAddressGood(readOffsetAddr)) {
                mem.write32(readOffsetAddr, id.getInputFileOffset());
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAtracGetStreamDataInfo: write=0x%08X, writableBytes=%d, readOffset=%d, %s", mem.read32(writeAddr), mem.read32(writableBytesAddr), mem.read32(readOffsetAddr), id.toString()));
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x7DB31251, version = 150, checkInsideInterrupt = true)
    public void sceAtracAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int bytesToAdd = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracAddStreamData: atracID=%d, bytesToAdd=0x%x", atID, bytesToAdd));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracAddStreamData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).addStreamData(bytesToAdd);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAtracAddStreamData: %s", atracIDs.get(atID).toString()));
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x83E85EA0, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetSecondBufferInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int outPosition = cpu.gpr[5];
        int outBytes = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracGetSecondBufferInfo: atracID = " + atID + ", outPos=0x" + Integer.toHexString(outPosition) + ", outBytes=0x" + Integer.toHexString(outBytes));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetSecondBufferInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            if(atracIDs.get(atID).isSecondBufferNeeded()) {
                if(Memory.isAddressGood(outPosition) && Memory.isAddressGood(outBytes)) {
                    mem.write32(outPosition, atracIDs.get(atID).getSecondInputFileOffset());
                    mem.write32(outBytes, atracIDs.get(atID).getSecondInputFileSize());
                }
                cpu.gpr[2] = 0;
            } else {
                cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_SECOND_BUFFER_NOT_NEEDED;
            }
        }
    }

    @HLEFunction(nid = 0x83BF7AFD, version = 150, checkInsideInterrupt = true)
    public void sceAtracSetSecondBuffer(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int secondBuffer = cpu.gpr[5];
        int secondBufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetSecondBuffer: atID = %d, buffer = 0x%08X, bufferSize = 0x%08X", atID, secondBuffer, secondBufferSize));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetSecondBuffer: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setData(secondBuffer, secondBufferSize, secondBufferSize, true);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xE23E3A35, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetNextDecodePosition(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int posAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetNextDecodePosition atracID = %d, posAddr = 0x%08X",
                    atID, posAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetNextDecodePosition: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracID id = atracIDs.get(atID);
            if (id.getAtracCurrentSample() >= id.getAtracEndSample()) {
                cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
            } else {
                mem.write32(posAddr, id.getAtracCurrentSample());
                cpu.gpr[2] = 0;
            }
        }
    }

    @HLEFunction(nid = 0xA2BBA8BE, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetSoundSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4] & atracIDMask;
        int endSampleAddr = cpu.gpr[5];
        int loopStartSampleAddr = cpu.gpr[6];
        int loopEndSampleAddr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetSoundSample atracID = %d, endSampleAddr = 0x%08X, loopStartSampleAddr = 0x%08X, loopEndSampleAddr = 0x%08X", atID, endSampleAddr, loopStartSampleAddr, loopEndSampleAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetSoundSample: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
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
            mem.write32(endSampleAddr, endSample);
            mem.write32(loopStartSampleAddr, loopStartSample);
            mem.write32(loopEndSampleAddr, loopEndSample);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x31668BAA, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetChannel(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int channelAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracGetChannel: atracID = " + atID + ", channelAddr =0x" + Integer.toHexString(channelAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetChannel: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(channelAddr, atracIDs.get(atID).getAtracChannels());
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xD6A5F2F7, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetMaxSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4] & atracIDMask;
        int maxSamplesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetMaxSample: atracID = %d, maxSamplesAddr = 0x%08X", atID, maxSamplesAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetMaxSample: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            if (Memory.isAddressGood(maxSamplesAddr)) {
                mem.write32(maxSamplesAddr, atracIDs.get(atID).getMaxSamples());
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x36FAABFB, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetNextSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int nbrSamplesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetNextSample: atracID=%d, nbrSamplesAddr=0x%08x", atID, nbrSamplesAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetNextSample: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
        	AtracID id = atracIDs.get(atID);
            int samples = id.getMaxSamples();
            if (id.getInputBufferOffset() >= id.getInputBufferSize()) {
                samples = 0; // No more data available in input buffer
            }
            if (Memory.isAddressGood(nbrSamplesAddr)) {
                mem.write32(nbrSamplesAddr, samples);
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xA554A158, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetBitrate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int bitrateAddr = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceAtracGetBitrate: atracID = " + atID + ", bitrateAddr =0x" + Integer.toHexString(bitrateAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetBitrate: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(bitrateAddr, atracIDs.get(atID).getAtracBitrate());
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xFAA4F89B, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetLoopStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int loopNbr = cpu.gpr[5];
        int statusAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetLoopStatus atracID=%d, loopNbr=0x%08x, statusAddr=0x%08X", atID, loopNbr, statusAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetLoopStatus: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            if (Memory.isAddressGood(loopNbr)) {
                mem.write32(loopNbr, atracIDs.get(atID).getLoopNum());
            }
            if (Memory.isAddressGood(statusAddr)) {
                mem.write32(statusAddr, atracIDs.get(atID).getLoopStatus());
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x868120B5, version = 150, checkInsideInterrupt = true)
    public void sceAtracSetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int loopNbr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracSetLoopNum: atracID = " + atID + ", loopNbr = " + loopNbr);
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetLoopNum: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setLoopNum(loopNbr);
            atracIDs.get(atID).getAtracCodec().setAtracLoopCount(loopNbr);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xCA3CA3D2, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetBufferInfoForReseting(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int sample = cpu.gpr[5];
        int bufferInfoAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetBufferInfoForReseting atracID=%d, sample=%d, bufferInfoAddr=0x%08x", atID, sample, bufferInfoAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetBufferInfoForReseting: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).getBufferInfoForReseting(sample, bufferInfoAddr);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x644E5607, version = 150, checkInsideInterrupt = true)
    public void sceAtracResetPlayPosition(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int sample = cpu.gpr[5];
        int bytesWrittenFirstBuf = cpu.gpr[6];
        int bytesWrittenSecondBuf = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracResetPlayPosition atracId=%d, sample=%d, bytesWrittenFirstBuf=%d, bytesWrittenSecondBuf=%d", atID, sample, bytesWrittenFirstBuf, bytesWrittenSecondBuf));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracResetPlayPosition: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracID id = atracIDs.get(atID);
            id.setPlayPosition(sample, bytesWrittenFirstBuf, bytesWrittenSecondBuf);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xE88F759B, version = 150, checkInsideInterrupt = true)
    public void sceAtracGetInternalErrorInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int errorAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetInternalErrorInfo atracId=%d, errorAddr=0x%08X", atID, errorAddr));
        }

        
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetInternalErrorInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(errorAddr, atracIDs.get(atID).getInternalErrorInfo());
            cpu.gpr[2] = 0;
        }
    }

}