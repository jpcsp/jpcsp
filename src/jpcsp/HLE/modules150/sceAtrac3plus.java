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

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.connector.AtracCodec;

import org.apache.log4j.Logger;

public class sceAtrac3plus implements HLEModule, HLEStartModule {

    protected static Logger log = Modules.getLogger("sceAtrac3plus");

    @Override
    public String getName() {
        return "sceAtrac3plus";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xD1F59FDB, sceAtracStartEntryFunction);
            mm.addFunction(0xD5C28CC0, sceAtracEndEntryFunction);
            mm.addFunction(0x780F88D1, sceAtracGetAtracIDFunction);
            mm.addFunction(0x61EB33F5, sceAtracReleaseAtracIDFunction);
            mm.addFunction(0x0E2A73AB, sceAtracSetDataFunction);
            mm.addFunction(0x3F6E26B5, sceAtracSetHalfwayBufferFunction);
            mm.addFunction(0x7A20E7AF, sceAtracSetDataAndGetIDFunction);
            mm.addFunction(0x0FAE370E, sceAtracSetHalfwayBufferAndGetIDFunction);
            mm.addFunction(0x6A8C3CD5, sceAtracDecodeDataFunction);
            mm.addFunction(0x9AE849A7, sceAtracGetRemainFrameFunction);
            mm.addFunction(0x5D268707, sceAtracGetStreamDataInfoFunction);
            mm.addFunction(0x7DB31251, sceAtracAddStreamDataFunction);
            mm.addFunction(0x83E85EA0, sceAtracGetSecondBufferInfoFunction);
            mm.addFunction(0x83BF7AFD, sceAtracSetSecondBufferFunction);
            mm.addFunction(0xE23E3A35, sceAtracGetNextDecodePositionFunction);
            mm.addFunction(0xA2BBA8BE, sceAtracGetSoundSampleFunction);
            mm.addFunction(0x31668BAA, sceAtracGetChannelFunction);
            mm.addFunction(0xD6A5F2F7, sceAtracGetMaxSampleFunction);
            mm.addFunction(0x36FAABFB, sceAtracGetNextSampleFunction);
            mm.addFunction(0xA554A158, sceAtracGetBitrateFunction);
            mm.addFunction(0xFAA4F89B, sceAtracGetLoopStatusFunction);
            mm.addFunction(0x868120B5, sceAtracSetLoopNumFunction);
            mm.addFunction(0xCA3CA3D2, sceAtracGetBufferInfoForResetingFunction);
            mm.addFunction(0x644E5607, sceAtracResetPlayPositionFunction);
            mm.addFunction(0xE88F759B, sceAtracGetInternalErrorInfoFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceAtracStartEntryFunction);
            mm.removeFunction(sceAtracEndEntryFunction);
            mm.removeFunction(sceAtracGetAtracIDFunction);
            mm.removeFunction(sceAtracReleaseAtracIDFunction);
            mm.removeFunction(sceAtracSetDataFunction);
            mm.removeFunction(sceAtracSetHalfwayBufferFunction);
            mm.removeFunction(sceAtracSetDataAndGetIDFunction);
            mm.removeFunction(sceAtracSetHalfwayBufferAndGetIDFunction);
            mm.removeFunction(sceAtracDecodeDataFunction);
            mm.removeFunction(sceAtracGetRemainFrameFunction);
            mm.removeFunction(sceAtracGetStreamDataInfoFunction);
            mm.removeFunction(sceAtracAddStreamDataFunction);
            mm.removeFunction(sceAtracGetSecondBufferInfoFunction);
            mm.removeFunction(sceAtracSetSecondBufferFunction);
            mm.removeFunction(sceAtracGetNextDecodePositionFunction);
            mm.removeFunction(sceAtracGetSoundSampleFunction);
            mm.removeFunction(sceAtracGetChannelFunction);
            mm.removeFunction(sceAtracGetMaxSampleFunction);
            mm.removeFunction(sceAtracGetNextSampleFunction);
            mm.removeFunction(sceAtracGetBitrateFunction);
            mm.removeFunction(sceAtracGetLoopStatusFunction);
            mm.removeFunction(sceAtracSetLoopNumFunction);
            mm.removeFunction(sceAtracGetBufferInfoForResetingFunction);
            mm.removeFunction(sceAtracResetPlayPositionFunction);
            mm.removeFunction(sceAtracGetInternalErrorInfoFunction);

        }
    }

    @Override
    public void start() {
        atracIDs = new HashMap<Integer, AtracID>();
    }

    @Override
    public void stop() {
    }

    protected static final String uidPurpose = "sceAtrac3plus";
    protected static final int AT3_MAGIC      = 0x0270; // "AT3"
    protected static final int AT3_PLUS_MAGIC = 0xFFFE; // "AT3PLUS"
    public    static final int RIFF_MAGIC = 0x46464952; // "RIFF"
    protected static final int WAVE_MAGIC = 0x45564157; // "WAVE"
    public    static final int FMT_CHUNK_MAGIC = 0x20746D66; // "FMT"
    protected static final int FACT_CHUNK_MAGIC = 0x74636166; // "FACT"
    protected static final int SMPL_CHUNK_MAGIC = 0x6C706D73; // "SMPL"
    public    static final int DATA_CHUNK_MAGIC = 0x61746164; // "DATA"

    protected static final int PSP_ATRAC_ALLDATA_IS_ON_MEMORY = -1;
    protected static final int PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY = -2;
    protected static final int PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY = -3;

    protected static final int PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA = 0;
    protected static final int PSP_ATRAC_STATUS_LOOP_STREAM_DATA = 1;

    protected static final int PSP_MODE_AT_3_PLUS = 0x00001000;
    protected static final int PSP_MODE_AT_3 = 0x00001001;

    // Tested on PSP:
    // Only 2 atracIDs per format can be registered at the same time.
    // Note: After firmware 2.50, these limits can be changed by sceAtracReinit.
    protected int atrac3MaxIDsCount = 2;
    protected int atrac3plusMaxIDsCount = 2;
    protected static final int atracDecodeDelay = 2300; // Microseconds, based on PSP tests

    public static boolean useAtracCodec = false;
    protected HashMap<Integer, AtracID> atracIDs;
    protected static final int atracIDMask = 0x000000FF; // "Patapon 2" expects the ID to be signed 8bit

    protected class AtracID {
        // Internal info.
    	protected int id;
        protected int uid;
        protected int codecType;
        protected AtracCodec atracCodec;
        protected int atrac3Num;
        protected int atrac3plusNum;
        // Sound data.
        protected int atracBitrate = 64;
        protected int atracChannels = 2;
        protected int atracSampleRate = 0xAC44;
        protected int atracBytesPerFrame = 0x0230;
        protected int atracEndSample;
        protected int atracCurrentSample;
        protected int loopNum;
        protected int maxSamples;
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

        public AtracID(int id, int uid, int codecType, AtracCodec atracCodec) {
            this.codecType = codecType;
            this.uid = uid;
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

            atracEndSample = -1;
            atracCurrentSample = 0;
            int RIFFMagic = mem.read32(inputBufferAddr);
            if (RIFFMagic == RIFF_MAGIC && inputBufferSize >= 8) {
                // RIFF file format:
                // Offset 0: 'RIFF'
                // Offset 4: file length - 8
                // Offset 8: 'WAVE'
                inputFileSize = mem.read32(inputBufferAddr + 4) + 8;
            }
            // Check for a valid "WAVE" header.
            int WAVEMagic = mem.read32(inputBufferAddr + 8);
            if (WAVEMagic == WAVE_MAGIC && inputBufferSize >= 36) {
                // Parse the "fmt" chunk.
                int fmtMagic = mem.read32(inputBufferAddr + 12);
                int fmtChunkSize = mem.read32(inputBufferAddr + 16);
                int compressionCode = mem.read16(inputBufferAddr + 20);
                atracChannels = mem.read16(inputBufferAddr + 22);
                atracSampleRate = mem.read32(inputBufferAddr + 24);
                atracBitrate = mem.read32(inputBufferAddr + 28);
                atracBytesPerFrame = mem.read16(inputBufferAddr + 32);
                int hiBytesPerSample = mem.read16(inputBufferAddr + 34);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("WAVE format: magic=0x%08X ('%s'), chunkSize=%d, compressionCode=0x%04X, channels=%d, sampleRate=%d, bitrate=%d, chunkAlign=%d, hiBytesPerSample=%d", fmtMagic, getStringFromInt32(fmtMagic), fmtChunkSize, compressionCode, atracChannels, atracSampleRate, atracBitrate, atracBytesPerFrame, hiBytesPerSample));
                    // Display rest of chunk as debug information
                    StringBuilder restChunk = new StringBuilder();
                    for (int i = 36; i < 20 + fmtChunkSize && i < inputBufferSize; i++) {
                        int b = mem.read8(inputBufferAddr + i);
                        restChunk.append(String.format(" %02X", b));
                    }
                    if (restChunk.length() > 0) {
                        log.debug(String.format("Additional chunk data:%s", restChunk));
                    }
                }

                int factMagic = mem.read32(inputBufferAddr + 20 + fmtChunkSize);
                if (factMagic == FACT_CHUNK_MAGIC) {
                    int factChunkSize = mem.read32(inputBufferAddr + 24 + fmtChunkSize);
                    if (factChunkSize >= 4) {
                        atracEndSample = mem.read32(inputBufferAddr + 24 + fmtChunkSize + 4);
                    }
                    // Check if there is a "smpl" chunk.
                    // Based on PSP tests, files that contain a "smpl" chunk always
                    // have post loop process data and need a second buffer
                    // (e.g.: .at3 files from Gran Turismo).
                    int smplMagic = mem.read32(inputBufferAddr + 28 + fmtChunkSize + factChunkSize);
                    if (smplMagic == SMPL_CHUNK_MAGIC) {
                    	// TODO Second buffer processing disabled because still incomplete
                        //isSecondBufferNeeded = true;
                    } else {
                        isSecondBufferNeeded = false;
                    }
                }
            }
        }

        public int getAtracId() {
        	return id;
        }

        public int getAtracUid() {
            return uid;
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

        public void setData(int buffer, int bufferSize, int minSize, boolean isSecondBuf) {
            if (isSecondBuf) {
                // Set the second buffer, but don't handle it.
                // This allows it to be ignored while we can't handle it.
                // TODO: After AT3+ can be decoded, implement real post loop
                // data handling.
                secondInputBufferAddr = buffer;
                secondInputBufferSize = bufferSize;
                secondInputBufferOffset = 0;
                secondInputBufferWritableBytes = 0;
                isSecondBufferSet = true;
            } else {
                inputBufferAddr = buffer;
                inputBufferSize = bufferSize;
                inputBufferOffset = 0;
                inputBufferWritableBytes = 0;
                inputFileSize = inputBufferSize;
                inputFileOffset = inputBufferSize;
                secondInputFileSize = 0x100;
                secondInputFileOffset = inputFileSize - 0x100;
                analyzeAtracHeader();
                log.info(String.format("hleAtracSetData atracID=%d, bufferSize=0x%x, fileSize=0x%x", getAtracId(), inputBufferSize, inputFileSize));
                if (getAtracCodec() == null) {
                    log.warn(String.format("hleAtracSetData atracID=%d is invalid", getAtracId()));
                    return;
                }
                getAtracCodec().atracSetData(getAtracId(), getAtracCodecType(), buffer, bufferSize, inputFileSize);
            }
        }

        protected void addStreamData(int length) {
            inputFileOffset += length;
            inputBufferOffset -= length;

            if (isEnableConnector()) {
                getAtracCodec().atracAddStreamData(inputBufferAddr, length);
            }
        }

        public int getRemainFrames() {
            if (inputFileOffset >= inputFileSize || atracCurrentSample >= atracEndSample) {
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
                mem.write32(bufferInfoAddr, inputBufferAddr);                       // Pointer to current writing position in the buffer.
                mem.write32(bufferInfoAddr + 4, inputBufferWritableBytes);          // Number of bytes which can be written to the buffer.
                mem.write32(bufferInfoAddr + 8, inputBufferNeededBytes);            // Number of bytes that must to be written to the buffer.
                mem.write32(bufferInfoAddr + 12, inputFileOffset);                // Read offset for input file.
                // Secondary buffer.
                mem.write32(bufferInfoAddr + 16, secondInputBufferAddr);            // Pointer to current writing position in the buffer.
                mem.write32(bufferInfoAddr + 20, secondInputBufferWritableBytes);   // Number of bytes which can be written to the buffer.
                mem.write32(bufferInfoAddr + 24, secondInputBufferNeededBytes);     // Number of bytes that must to be written to the buffer.
                mem.write32(bufferInfoAddr + 28, secondInputFileOffset);          // Read offset for input file.
            }
        }
    }

    public static boolean isEnableConnector() {
        return useAtracCodec;
    }

    public static void setEnableConnector(boolean useConnector) {
        sceAtrac3plus.useAtracCodec = useConnector;
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

    protected void hleAtracReinit(int newAT3IdCount, int newAT3plusIdCount) {
        atrac3MaxIDsCount = newAT3IdCount;
        atrac3plusMaxIDsCount = newAT3plusIdCount;
    }

    protected int hleCreateAtracID(int codecType) {
    	int uid = SceUidManager.getNewUid(uidPurpose);
        int atracID = uid & atracIDMask;
        AtracCodec atracCodec = new AtracCodec();
        AtracID id = new AtracID(atracID, uid, codecType, atracCodec);
        if(id.getAtracId() >= 0) {
            atracIDs.put(atracID, id);
            return atracID;
        }
        return SceKernelErrors.ERROR_ATRAC_NO_ID;
    }

    protected void hleReleaseAtracID(int atID) {
    	AtracID id = atracIDs.remove(atID);
    	int uid = id.getAtracUid();
        SceUidManager.releaseUid(uid, uidPurpose);
    	id.release();
    }

    protected int getCodecType(int address) {
        int at3magic = Memory.getInstance().read16(address + 20);
        if (at3magic == AT3_MAGIC) {
            return PSP_MODE_AT_3;
        } else if (at3magic == AT3_PLUS_MAGIC) {
            return PSP_MODE_AT_3_PLUS;
        }

        return 0; // Unknown Codec
    }

    public void sceAtracStartEntry(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceAtracStartEntry [0xD1F59FDB]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAtracEndEntry(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceAtracEndEntry [0xD5C28CC0]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAtracGetAtracID(Processor processor) {
        CpuState cpu = processor.cpu;

        int codecType = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracGetAtracID: codecType = 0x" + Integer.toHexString(codecType));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = hleCreateAtracID(codecType);
    }

    public void sceAtracReleaseAtracID(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;

        if (log.isDebugEnabled()) {
            log.debug("sceAtracReleaseAtracID: atracID = " + atID);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
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

    public void sceAtracSetData(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int buffer = cpu.gpr[5];
        int bufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetData: atID = %d, buffer = 0x%08X, bufferSize = 0x%08X", atID, buffer, bufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setData(buffer, bufferSize, bufferSize, false);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetHalfwayBuffer(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int halfBuffer = cpu.gpr[5];
        int readSize = cpu.gpr[6];
        int halfBufferSize = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetHalfwayBuffer: atID = %d, buffer = 0x%08X, readSize = 0x%08X, bufferSize = 0x%08X", atID, halfBuffer, readSize, halfBufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetHalfwayBuffer: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setData(halfBuffer, readSize, halfBufferSize, false);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetDataAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        int buffer = cpu.gpr[4];
        int bufferSize = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetDataAndGetID buffer = 0x%08X, bufferSize = 0x%08X", buffer, bufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int atID = 0;
        if (Memory.isAddressGood(buffer)) {
        	int codecType = getCodecType(buffer);
            atID = hleCreateAtracID(codecType);
            if (atracIDs.containsKey(atID)) {
                atracIDs.get(atID).setData(buffer, bufferSize, bufferSize, false);
            }
        }
        cpu.gpr[2] = atID;
    }

    public void sceAtracSetHalfwayBufferAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        int halfBuffer = cpu.gpr[4];
        int halfBufferSize = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetHalfwayBufferAndGetID buffer = 0x%08X, bufferSize = 0x%08X", halfBuffer, halfBufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int atID = 0;
        if (Memory.isAddressGood(halfBuffer)) {
        	int codecType = getCodecType(halfBuffer);
            atID = hleCreateAtracID(codecType);
            if (atracIDs.containsKey(atID)) {
                atracIDs.get(atID).setData(halfBuffer, halfBufferSize, halfBufferSize, false);
            }
        }
        cpu.gpr[2] = atID;
    }

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

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
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
            int remainFrames = -1;
            if (atracCodec != null) {
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
                } else if (id.getInputBufferSize() + consumedInputBytes > id.getInputBufferSize()) {
                    consumedInputBytes = id.getInputBufferSize() - id.getInputBufferOffset();
                }
                id.setInputBufferOffset(id.getInputBufferOffset() + consumedInputBytes);
            }
            if (end == 1) {
                remainFrames = -1;
            } else {
                remainFrames = id.getRemainFrames();
            }
            if (samples > 0) {
                id.setAtracCurrentSample(id.getAtracCurrentSample() + samples);
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
                log.debug(String.format("sceAtracDecodeData returning 0x%08X, samples=%d, end=%d, remainFrames=%d, currentSample=%d/%d", result, samples, end, remainFrames, id.getAtracCurrentSample(), id.getAtracEndSample()));
            }

            cpu.gpr[2] = result;
            // Delay the thread decoding the Atrac data,
            // the thread is also blocking using semaphores/event flags on a real PSP.
            Modules.ThreadManForUserModule.hleKernelDelayThread(atracDecodeDelay, false);
        }
    }

    public void sceAtracGetRemainFrame(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4] & atracIDMask;
        int remainFramesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetRemainFrame: atracID = %d, remainFramesAddr = 0x%08X", atID, remainFramesAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetRemainFrame: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(remainFramesAddr, atracIDs.get(atID).getRemainFrames());
            cpu.gpr[2] = 0;
        }
    }

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

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetStreamDataInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracID id = atracIDs.get(atID);
            id.setInputBufferWritableBytes(id.getInputBufferOffset());
            if (id.getInputFileOffset() >= id.getInputFileSize()) {
                // All data is in the buffer
                id.setInputBufferWritableBytes(0);
            } else if (id.getInputBufferWritableBytes() > (id.getInputFileSize() - id.getInputFileOffset())) {
                // Do not need more data than input file size
                id.setInputBufferWritableBytes(id.getInputFileSize() - id.getInputFileOffset());
            }
            if (Memory.isAddressGood(writeAddr)) {
                mem.write32(writeAddr, id.getInputBufferAddr());
            }
            if (Memory.isAddressGood(writableBytesAddr)) {
                mem.write32(writableBytesAddr, id.getInputBufferWritableBytes());
            }
            if (Memory.isAddressGood(readOffsetAddr)) {
                mem.write32(readOffsetAddr, id.getInputFileOffset());
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int bytesToAdd = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracAddStreamData: atracID=%d, bytesToAdd=0x%x", atID, bytesToAdd));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracAddStreamData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).addStreamData(bytesToAdd);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetSecondBufferInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int outPosition = cpu.gpr[5];
        int outBytes = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracGetSecondBufferInfo: atracID = " + atID + ", outPos=0x" + Integer.toHexString(outPosition) + ", outBytes=0x" + Integer.toHexString(outBytes));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
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

    public void sceAtracSetSecondBuffer(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int secondBuffer = cpu.gpr[5];
        int secondBufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetSecondBuffer: atID = %d, buffer = 0x%08X, bufferSize = 0x%08X", atID, secondBuffer, secondBufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetSecondBuffer: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setData(secondBuffer, secondBufferSize, secondBufferSize, true);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetNextDecodePosition(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int posAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetNextDecodePosition atracID = %d, posAddr = 0x%08X",
                    atID, posAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
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

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetSoundSample: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
        	int endSample = atracIDs.get(atID).getAtracEndSample();
            int loopStartSample = -1;
            int loopEndSample = -1;
        	if (endSample < 0) {
        		endSample = atracIDs.get(atID).getAtracCodec().getAtracEndSample();
        	}
            if (endSample < 0) {
                endSample = atracIDs.get(atID).getInputFileSize();
            }
            if (atracIDs.get(atID).getLoopNum() > 0) {
                loopStartSample = 0;
                loopEndSample = atracIDs.get(atID).getLoopNum() - 1;
            }
            mem.write32(endSampleAddr, endSample);
            mem.write32(loopStartSampleAddr, loopStartSample);
            mem.write32(loopEndSampleAddr, loopEndSample);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetChannel(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int channelAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracGetChannel: atracID = " + atID + ", channelAddr =0x" + Integer.toHexString(channelAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetChannel: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(channelAddr, atracIDs.get(atID).getAtracChannels());
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetMaxSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4] & atracIDMask;
        int maxSamplesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetMaxSample: atracID = %d, maxSamplesAddr = 0x%08X", atID, maxSamplesAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
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

    public void sceAtracGetNextSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int nbrSamplesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetNextSample: atracID=%d, nbrSamplesAddr=0x%08x", atID, nbrSamplesAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
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

    public void sceAtracGetBitrate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int bitrateAddr = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceAtracGetBitrate: atracID = " + atID + ", bitrateAddr =0x" + Integer.toHexString(bitrateAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetBitrate: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(bitrateAddr, atracIDs.get(atID).getAtracBitrate());
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetLoopStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int loopNbr = cpu.gpr[5];
        int statusAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetLoopStatus atracID=%d, loopNbr=0x%08x, statusAddr=0x%08X", atID, loopNbr, statusAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetLoopStatus: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            if (Memory.isAddressGood(loopNbr)) {
                mem.write32(loopNbr, atracIDs.get(atID).getLoopNum());
            }
            if (Memory.isAddressGood(statusAddr)) {
                mem.write32(statusAddr, PSP_ATRAC_STATUS_LOOP_STREAM_DATA);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int loopNbr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracSetLoopNum: atracID = " + atID + ", loopNbr = " + loopNbr);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
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

    public void sceAtracGetBufferInfoForReseting(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int sample = cpu.gpr[5];
        int bufferInfoAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetBufferInfoForReseting atracID=%d, sample=%d, bufferInfoAddr=0x%08x", atID, sample, bufferInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetBufferInfoForReseting: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).getBufferInfoForReseting(sample, bufferInfoAddr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracResetPlayPosition(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int sample = cpu.gpr[5];
        int bytesWrittenFirstBuf = cpu.gpr[6];
        int bytesWrittenSecondBuf = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracResetPlayPosition atracId=%d, sample=%d, bytesWrittenFirstBuf=%d, bytesWrittenSecondBuf=%d", atID, sample, bytesWrittenFirstBuf, bytesWrittenSecondBuf));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracResetPlayPosition: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracID id = atracIDs.get(atID);
            id.setInputBufferOffset(id.getInputBufferSize() - bytesWrittenFirstBuf);
            id.setInputFileSize(id.getInputBufferSize());
            id.setInputFileOffset(id.getInputBufferSize());
            id.setAtracCurrentSample(sample);
            id.getAtracCodec().atracResetPlayPosition(sample);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetInternalErrorInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4] & atracIDMask;
        int errorAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetInternalErrorInfo atracId=%d, errorAddr=0x%08X", atID, errorAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetInternalErrorInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(errorAddr, atracIDs.get(atID).getInternalErrorInfo());
            cpu.gpr[2] = 0;
        }
    }
    public final HLEModuleFunction sceAtracStartEntryFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracStartEntry") {

        @Override
        public final void execute(Processor processor) {
            sceAtracStartEntry(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracStartEntry(processor);";
        }
    };
    public final HLEModuleFunction sceAtracEndEntryFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracEndEntry") {

        @Override
        public final void execute(Processor processor) {
            sceAtracEndEntry(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracEndEntry(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetAtracIDFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetAtracID") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetAtracID(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetAtracID(processor);";
        }
    };
    public final HLEModuleFunction sceAtracReleaseAtracIDFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracReleaseAtracID") {

        @Override
        public final void execute(Processor processor) {
            sceAtracReleaseAtracID(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracReleaseAtracID(processor);";
        }
    };
    public final HLEModuleFunction sceAtracSetDataFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracSetData") {

        @Override
        public final void execute(Processor processor) {
            sceAtracSetData(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracSetData(processor);";
        }
    };
    public final HLEModuleFunction sceAtracSetHalfwayBufferFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracSetHalfwayBuffer") {

        @Override
        public final void execute(Processor processor) {
            sceAtracSetHalfwayBuffer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracSetHalfwayBuffer(processor);";
        }
    };
    public final HLEModuleFunction sceAtracSetDataAndGetIDFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracSetDataAndGetID") {

        @Override
        public final void execute(Processor processor) {
            sceAtracSetDataAndGetID(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracSetDataAndGetID(processor);";
        }
    };
    public final HLEModuleFunction sceAtracSetHalfwayBufferAndGetIDFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracSetHalfwayBufferAndGetID") {

        @Override
        public final void execute(Processor processor) {
            sceAtracSetHalfwayBufferAndGetID(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracSetHalfwayBufferAndGetID(processor);";
        }
    };
    public final HLEModuleFunction sceAtracDecodeDataFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracDecodeData") {

        @Override
        public final void execute(Processor processor) {
            sceAtracDecodeData(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracDecodeData(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetRemainFrameFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetRemainFrame") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetRemainFrame(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetRemainFrame(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetStreamDataInfoFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetStreamDataInfo") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetStreamDataInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetStreamDataInfo(processor);";
        }
    };
    public final HLEModuleFunction sceAtracAddStreamDataFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracAddStreamData") {

        @Override
        public final void execute(Processor processor) {
            sceAtracAddStreamData(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracAddStreamData(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetSecondBufferInfoFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetSecondBufferInfo") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetSecondBufferInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetSecondBufferInfo(processor);";
        }
    };
    public final HLEModuleFunction sceAtracSetSecondBufferFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracSetSecondBuffer") {

        @Override
        public final void execute(Processor processor) {
            sceAtracSetSecondBuffer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracSetSecondBuffer(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetNextDecodePositionFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetNextDecodePosition") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetNextDecodePosition(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetNextDecodePosition(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetSoundSampleFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetSoundSample") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetSoundSample(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetSoundSample(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetChannelFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetChannel") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetChannel(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetChannel(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetMaxSampleFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetMaxSample") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetMaxSample(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetMaxSample(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetNextSampleFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetNextSample") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetNextSample(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetNextSample(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetBitrateFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetBitrate") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetBitrate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetBitrate(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetLoopStatusFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetLoopStatus") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetLoopStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetLoopStatus(processor);";
        }
    };
    public final HLEModuleFunction sceAtracSetLoopNumFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracSetLoopNum") {

        @Override
        public final void execute(Processor processor) {
            sceAtracSetLoopNum(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracSetLoopNum(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetBufferInfoForResetingFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetBufferInfoForReseting") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetBufferInfoForReseting(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetBufferInfoForReseting(processor);";
        }
    };
    public final HLEModuleFunction sceAtracResetPlayPositionFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracResetPlayPosition") {

        @Override
        public final void execute(Processor processor) {
            sceAtracResetPlayPosition(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracResetPlayPosition(processor);";
        }
    };
    public final HLEModuleFunction sceAtracGetInternalErrorInfoFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetInternalErrorInfo") {

        @Override
        public final void execute(Processor processor) {
            sceAtracGetInternalErrorInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetInternalErrorInfo(processor);";
        }
    };
}