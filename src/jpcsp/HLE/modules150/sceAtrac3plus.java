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
        atrac3IDsCount = 0;
        atrac3plusIDsCount = 0;
        atrac3Codecs = new HashMap<Integer, AtracCodec>();
        atrac3plusCodecs = new HashMap<Integer, AtracCodec>();
        atracLoopMap = new HashMap<Integer, Integer>();
    }

    @Override
    public void stop() {
    }
    protected static final String uidPurpose = "sceAtrac3plus";
    protected static final int AT3_MAGIC = 0x00000270; // "AT3"
    protected static final int AT3_PLUS_MAGIC = 0x0000FFFE; // "AT3PLUS"
    protected static final int RIFF_MAGIC = 0x46464952; // "RIFF"
    protected static final int WAVE_MAGIC = 0x45564157; // "WAVE"

    protected static final int PSP_ATRAC_ALLDATA_IS_ON_MEMORY = -1;
    protected static final int PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY = -2;
    protected static final int PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY = -3;

    protected static final int PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA = 0;
    protected static final int PSP_ATRAC_STATUS_LOOP_STREAM_DATA = 1;

    protected static final int PSP_MODE_AT_3_PLUS = 0x00001000;
    protected static final int PSP_MODE_AT_3 = 0x00001001;

    protected int inputBufferAddr;
    protected int inputBufferSize;
    protected int inputBufferOffset;
    protected int inputBufferAvailableBytes;
    protected int inputBufferNeededBytes;
    protected int secondInputBufferAddr;
    protected int secondInputBufferSize;
    protected int secondInputBufferOffset;
    protected int secondInputBufferAvailableBytes;
    protected int secondInputBufferNeededBytes;
    protected int inputFileSize;
    protected int inputFileOffset;
    protected boolean isSecondBufferNeeded;

    protected int atrac3IDsCount;
    protected int atrac3plusIDsCount;
    public static int maxSamples;
    protected int atracBitrate = 64;
    protected int atracChannels = 2;
    protected int atracSampleRate = 0xAC44;

    public static boolean useAtracCodec = false;
    protected HashMap<Integer, AtracCodec> atrac3Codecs;
    protected HashMap<Integer, AtracCodec> atrac3plusCodecs;
    public HashMap<Integer, Integer> atracLoopMap;

    public static boolean isEnableConnector() {
        return useAtracCodec;
    }

    public static void setEnableConnector(boolean useConnector) {
        sceAtrac3plus.useAtracCodec = useConnector;
    }

    protected void analyzeAtracHeader() {
        Memory mem = Memory.getInstance();

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
        if (WAVEMagic == WAVE_MAGIC) {
            // Parse the "fmt" chunk.
            int fmtMagic = mem.read32(inputBufferAddr + 12);
            int fmtChunkSize = mem.read32(inputBufferAddr + 16);
            int compressionCode = mem.read16(inputBufferAddr + 20);
            atracChannels = mem.read16(inputBufferAddr + 22);
            atracSampleRate = mem.read32(inputBufferAddr + 24);
            atracBitrate = mem.read32(inputBufferAddr + 28);
            int chunkAlign = mem.read16(inputBufferAddr + 32);
            int hiBytesPerSample = mem.read16(inputBufferAddr + 34);
            int fmtExtraBytes = mem.read16(inputBufferAddr + 36);
        }
    }

    protected AtracCodec getAtracCodec(int atracID) {
        if (getIDCodecType(atracID) == PSP_MODE_AT_3) {
            return atrac3Codecs.get(atracID);
        } else if (getIDCodecType(atracID) == PSP_MODE_AT_3_PLUS) {
            return atrac3plusCodecs.get(atracID);
        } else {
            return null;
        }
    }

    protected void setSecondBufferNeeded(boolean status) {
        isSecondBufferNeeded = status;
    }

    protected boolean isSecondBufferNeeded() {
        return isSecondBufferNeeded;
    }

    protected void hleAtracSetData(int atracID, int buffer, int bufferSize, int minSize) {
        inputBufferAddr = buffer;
        inputBufferSize = bufferSize;
        inputBufferOffset = 0;
        inputBufferAvailableBytes = 0;
        // Use the last 0x1000 bytes for the post loop process part (second buffer).
        secondInputBufferAddr = buffer + bufferSize - 0x1000;
        secondInputBufferSize = 0x1000;
        secondInputBufferOffset = 0;
        secondInputBufferAvailableBytes = 0;
        inputFileSize = inputBufferSize;
        inputFileOffset = inputBufferSize;

        analyzeAtracHeader();

        // Don't use the second buffer.
        // TODO: The second buffer is used to store post loop process data.
        // Parse the decrypted data and accquire it's offset.
        setSecondBufferNeeded(false);

        log.info(String.format("hleAtracSetData atracID=%d, bufferSize=0x%x, fileSize=0x%x", atracID, inputBufferSize, inputFileSize));

        AtracCodec codec = getAtracCodec(atracID);
        if (codec == null) {
            log.warn(String.format("hleAtracSetData atracID=%d is invalid", atracID));
            return;
        }
        if (isEnableConnector()) {
            codec.atracSetData(atracID, getIDCodecType(atracID), buffer, bufferSize, inputFileSize, true);
        } else if (sceMpeg.isEnableMediaEngine()) {
            codec.atracSetData(atracID, getIDCodecType(atracID), buffer, bufferSize, inputFileSize, false);
        }
    }

    protected void hleAtracAddStreamData(int atracID, int length) {
        inputFileOffset += length;
        inputBufferOffset -= length;

        if (isEnableConnector()) {
            getAtracCodec(atracID).atracAddStreamData(inputBufferAddr, length);
        }
    }

    protected int hleCreateAtracID(int codecType) {
        int atracID = SceUidManager.getNewUid(uidPurpose);
        if (codecType != PSP_MODE_AT_3 && codecType != PSP_MODE_AT_3_PLUS) {
            log.warn("hleGetAtracID unknown codecType " + codecType);
        }
        AtracCodec atracCodec = new AtracCodec();
        if (codecType == PSP_MODE_AT_3) {
            atrac3Codecs.put(atracID, atracCodec);
        } else if (codecType == PSP_MODE_AT_3_PLUS) {
            atrac3plusCodecs.put(atracID, atracCodec);
        }
        return atracID;
    }

    protected int getIDCodecType(int atracID) {
        if (atrac3Codecs.containsKey(atracID)) {
            return PSP_MODE_AT_3;
        } else if (atrac3plusCodecs.containsKey(atracID)) {
            return PSP_MODE_AT_3_PLUS;
        } else {
            return -1;
        }
    }

    protected int getRemainFrames(int atracID) {
        if (inputFileOffset >= inputFileSize) {
            return PSP_ATRAC_ALLDATA_IS_ON_MEMORY;
        }
        return (inputBufferSize - inputBufferOffset) / (4 * maxSamples);
    }

    protected void hleAtracGetBufferInfoForReseting(int atracID, int sample, int bufferInfoAddr) {
        Memory mem = Memory.getInstance();

        if (mem.isAddressGood(bufferInfoAddr)) {
            // Holds buffer related parameters.
            // Main buffer.
            mem.write32(bufferInfoAddr, inputBufferAddr);                       // Pointer to current writing position in the buffer.
            mem.write32(bufferInfoAddr + 4, inputBufferAvailableBytes);         // Available bytes in buffer.
            mem.write32(bufferInfoAddr + 8, inputBufferNeededBytes);            // Number of bytes that must to be written to the buffer.
            mem.write32(bufferInfoAddr + 12, inputBufferOffset);                // Read offset.
            // Secondary buffer.
            mem.write32(bufferInfoAddr + 16, secondInputBufferAddr);            // Pointer to current writing position in the buffer.
            mem.write32(bufferInfoAddr + 20, secondInputBufferAvailableBytes);  // Available bytes in buffer.
            mem.write32(bufferInfoAddr + 24, secondInputBufferNeededBytes);     // Number of bytes that must to be written to the buffer.
            mem.write32(bufferInfoAddr + 28, secondInputBufferOffset);          // Read offset.
        }
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
        // Tested on PSP:
        // Only 2 atracIDs per format can be registered at the same time.
        if ((codecType == PSP_MODE_AT_3) && (atrac3IDsCount <= 2)) {
            maxSamples = 1024;
            cpu.gpr[2] = hleCreateAtracID(codecType);
            atrac3IDsCount++;
        } else if ((codecType == PSP_MODE_AT_3_PLUS) && (atrac3plusIDsCount <= 2)) {
            maxSamples = 2048;
            cpu.gpr[2] = hleCreateAtracID(codecType);
            atrac3plusIDsCount++;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_NO_ID;
        }
    }

    public void sceAtracReleaseAtracID(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracReleaseAtracID: atracID = " + atID);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracReleaseAtracID: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracCodec atracCodec = getAtracCodec(atID);
            if (atracCodec != null) {
                atracCodec.finish();
            }
            if ((getIDCodecType(atID) == PSP_MODE_AT_3) && (atrac3IDsCount >= 0)) {
                atrac3Codecs.remove(atID);
                SceUidManager.releaseUid(atID, uidPurpose);
                atrac3IDsCount--;
            } else if ((getIDCodecType(atID) == PSP_MODE_AT_3_PLUS) && (atrac3plusIDsCount >= 0)) {
                atrac3plusCodecs.remove(atID);
                SceUidManager.releaseUid(atID, uidPurpose);
                atrac3plusIDsCount--;
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int buffer = cpu.gpr[5];
        int bufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetData: atID = %d, buffer = 0x%08X, bufferSize = 0x%08X", atID, buffer, bufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracSetData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            int codecType = 0;
            int at3magic = 0;
            if (mem.isAddressGood(buffer)) {
                at3magic = mem.read32(buffer + 20);
                at3magic &= 0x0000FFFF;

                if (at3magic == AT3_MAGIC) {
                    codecType = PSP_MODE_AT_3;
                } else if (at3magic == AT3_PLUS_MAGIC) {
                    codecType = PSP_MODE_AT_3_PLUS;
                }

                if (codecType == PSP_MODE_AT_3) {
                    maxSamples = 1024;
                } else if (codecType == PSP_MODE_AT_3_PLUS) {
                    maxSamples = 2048;
                }
            }
            hleAtracSetData(atID, buffer, bufferSize, bufferSize);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetHalfwayBuffer(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
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
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracSetHalfwayBuffer: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            int codecType = 0;
            int at3magic = 0;
            if (mem.isAddressGood(halfBuffer)) {
                at3magic = mem.read32(halfBuffer + 20);
                at3magic &= 0x0000FFFF;

                if (at3magic == AT3_MAGIC) {
                    codecType = PSP_MODE_AT_3;
                } else if (at3magic == AT3_PLUS_MAGIC) {
                    codecType = PSP_MODE_AT_3_PLUS;
                }

                if (codecType == PSP_MODE_AT_3) {
                    maxSamples = 1024;
                } else if (codecType == PSP_MODE_AT_3_PLUS) {
                    maxSamples = 2048;
                }
            }
            hleAtracSetData(atID, halfBuffer, readSize, halfBufferSize);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetDataAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer = cpu.gpr[4];
        int bufferSize = cpu.gpr[5];

        int codecType = 0;
        int at3magic = 0;
        int atID = 0;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetDataAndGetID buffer = 0x%08X, bufferSize = 0x%08X", buffer, bufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mem.isAddressGood(buffer)) {
            at3magic = mem.read32(buffer + 20);
            at3magic &= 0x0000FFFF;

            if (at3magic == AT3_MAGIC) {
                codecType = PSP_MODE_AT_3;
            } else if (at3magic == AT3_PLUS_MAGIC) {
                codecType = PSP_MODE_AT_3_PLUS;
            }

            if ((codecType == PSP_MODE_AT_3) && (atrac3IDsCount <= 2)) {
                maxSamples = 1024;
                atID = hleCreateAtracID(codecType);
                atrac3IDsCount++;
            } else if ((codecType == PSP_MODE_AT_3_PLUS) && (atrac3plusIDsCount <= 2)) {
                maxSamples = 2048;
                atID = hleCreateAtracID(codecType);
                atrac3plusIDsCount++;
            } else {
                atID = SceKernelErrors.ERROR_ATRAC_NO_ID;
            }
            hleAtracSetData(atID, buffer, bufferSize, bufferSize);
        }
        cpu.gpr[2] = atID;
    }

    public void sceAtracSetHalfwayBufferAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int halfBuffer = cpu.gpr[4];
        int halfBufferSize = cpu.gpr[5];

        int codecType = 0;
        int at3magic = 0;
        int atID = 0;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetHalfwayBufferAndGetID buffer = 0x%08X, bufferSize = 0x%08X", halfBuffer, halfBufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mem.isAddressGood(halfBuffer)) {
            at3magic = mem.read32(halfBuffer + 20);
            at3magic &= 0x0000FFFF;

            if (at3magic == AT3_MAGIC) {
                codecType = PSP_MODE_AT_3;
            } else if (at3magic == AT3_PLUS_MAGIC) {
                codecType = PSP_MODE_AT_3_PLUS;
            }

            if ((codecType == PSP_MODE_AT_3) && (atrac3IDsCount <= 2)) {
                maxSamples = 1024;
                atID = hleCreateAtracID(codecType);
                atrac3IDsCount++;
            } else if ((codecType == PSP_MODE_AT_3_PLUS) && (atrac3plusIDsCount <= 2)) {
                maxSamples = 2048;
                atID = hleCreateAtracID(codecType);
                atrac3plusIDsCount++;
            } else {
                atID = SceKernelErrors.ERROR_ATRAC_NO_ID;
            }
            hleAtracSetData(atID, halfBuffer, halfBufferSize, halfBufferSize);
        }
        cpu.gpr[2] = atID;
    }

    public void sceAtracDecodeData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
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
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracDecodeData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            int result = 0;
            AtracCodec atracCodec = getAtracCodec(atID);

            if (atracCodec != null) {
                int samples = atracCodec.atracDecodeData(atID, samplesAddr);
                if (samples < 0) {
                    // Not using decoded data.
                    int fakedSamples = maxSamples;
                    int consumedInputBytes = fakedSamples;

                    if (inputBufferOffset >= inputBufferSize) {
                        fakedSamples = 0; // No more data in input buffer
                        result = SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
                    } else {
                        // Assume consuming as many ATRAC3 input bytes as samples
                        // (this is faked because it would mean ATRAC3 does not compress audio at all)
                        consumedInputBytes = fakedSamples;
                        if (inputBufferOffset + consumedInputBytes > inputBufferSize) {
                            consumedInputBytes = inputBufferSize - inputBufferOffset;
                        }
                        inputBufferOffset += consumedInputBytes;
                        if (consumedInputBytes == 0 && inputFileOffset < inputFileSize) {
                            result = SceKernelErrors.ERROR_ATRAC_BUFFER_IS_EMPTY;
                        }
                    }
                    int end = inputFileOffset >= inputFileSize ? 1 : 0;

                    if (mem.isAddressGood(samplesAddr)) {
                        mem.memset(samplesAddr, (byte) 0, fakedSamples * 4);  // 4 empty bytes per sample.
                    }
                    if (mem.isAddressGood(samplesNbrAddr)) {
                        mem.write32(samplesNbrAddr, fakedSamples);
                    }
                    if (mem.isAddressGood(outEndAddr)) {
                        mem.write32(outEndAddr, end);
                    }
                    if (mem.isAddressGood(remainFramesAddr)) {
                        mem.write32(remainFramesAddr, getRemainFrames(atID));
                    }
                } else if (samples == 0){
                    // Using decoded data and all samples have been decoded.
                    result = SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
                } else {
                    // Using decoded data.
                    if (mem.isAddressGood(samplesNbrAddr)) {
                        mem.write32(samplesNbrAddr, samples);
                    }
                    if (mem.isAddressGood(outEndAddr)) {
                        mem.write32(outEndAddr, atracCodec.getAtracEnd());
                    }
                    if (mem.isAddressGood(remainFramesAddr)) {
                        mem.write32(remainFramesAddr, atracCodec.getAtracRemainFrames());
                    }
                }
            }
            cpu.gpr[2] = result;
        }
    }

    public void sceAtracGetRemainFrame(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int remainFramesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetRemainFrame: atracID = %d, remainFramesAddr = 0x%08X",
                    atID, remainFramesAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetRemainFrame: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracCodec atracCodec = getAtracCodec(atID);
            if (mem.isAddressGood(remainFramesAddr)) {
                if (atracCodec != null) {
                     mem.write32(remainFramesAddr, atracCodec.getAtracRemainFrames());
                } else {
                    mem.write32(remainFramesAddr, getRemainFrames(atID));
                }
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetStreamDataInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int writeAddr = cpu.gpr[5];
        int availableBytesAddr = cpu.gpr[6];
        int readOffsetAddr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetStreamDataInfo: atID=0x%08x, writeAddr=0x%08x, availableBytesAddr=0x%08x, readOffsetAddr=0x%08x",
                    atID, writeAddr, availableBytesAddr, readOffsetAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetStreamDataInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            inputBufferAvailableBytes = inputBufferOffset;
            if (inputFileOffset >= inputFileSize) {
                // All data is in the buffer
                inputBufferAvailableBytes = 0;
            } else if (inputBufferAvailableBytes > (inputFileSize - inputFileOffset)) {
                // Do not need more data than input file size
                inputBufferAvailableBytes = inputFileSize - inputFileOffset;
            }
            if (mem.isAddressGood(writeAddr)) {
                mem.write32(writeAddr, inputBufferAddr);
            }
            if (mem.isAddressGood(availableBytesAddr)) {
                mem.write32(availableBytesAddr, inputBufferAvailableBytes);
            }
            if (mem.isAddressGood(readOffsetAddr)) {
                mem.write32(readOffsetAddr, inputFileOffset);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int bytesToAdd = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracAddStreamData: atracID=%d, bytesToAdd=0x%x", atID, bytesToAdd));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracAddStreamData: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            hleAtracAddStreamData(atID, bytesToAdd);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetSecondBufferInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int outPosition = cpu.gpr[5];
        int outBytes = cpu.gpr[6];

        log.warn("PARTIAL: sceAtracGetSecondBufferInfo: atracID = " + atID + ", outPos=0x" + Integer.toHexString(outPosition) + ", outBytes=0x" + Integer.toHexString(outBytes));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetSecondBufferInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            if(isSecondBufferNeeded()) {
                if(mem.isAddressGood(outPosition) && mem.isAddressGood(outBytes)) {
                    mem.write32(outPosition, secondInputBufferAddr);
                    mem.write32(outBytes, secondInputBufferSize);
                }
                cpu.gpr[2] = 0;
            } else {
                cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_SECOND_BUFFER_NOT_NEEDED;
            }
        }
    }

    public void sceAtracSetSecondBuffer(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int secondBuffer = cpu.gpr[5];
        int secondBufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetSecondBuffer: atID = %d, buffer = 0x%08X, bufferSize = 0x%08X", atID, secondBuffer, secondBufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracSetSecondBuffer: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            int codecType = 0;
            int at3magic = 0;
            if (mem.isAddressGood(secondBuffer)) {
                at3magic = mem.read32(secondBuffer + 20);
                at3magic &= 0x0000FFFF;

                if (at3magic == AT3_MAGIC) {
                    codecType = PSP_MODE_AT_3;
                } else if (at3magic == AT3_PLUS_MAGIC) {
                    codecType = PSP_MODE_AT_3_PLUS;
                }

                if (codecType == PSP_MODE_AT_3) {
                    maxSamples = 1024;
                } else if (codecType == PSP_MODE_AT_3_PLUS) {
                    maxSamples = 2048;
                }
            }
            // The second buffer is only set after sceAtracGetSecondBufferInfo is called.
            hleAtracSetData(atID, secondBuffer, secondBufferSize, secondBufferSize);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetNextDecodePosition(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int posAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("PARTIAL: sceAtracGetNextDecodePosition atracID = %d, posAddr = 0x%08X",
                    atID, posAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetNextDecodePosition: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            AtracCodec atracCodec = getAtracCodec(atID);
            if (atracCodec.getAtracNextDecodePosition() > 0) {
                int pos = atracCodec.getAtracNextDecodePosition();
                mem.write32(posAddr, pos);

                if (atracCodec.getAtracEnd() == 0) {
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
                }
            } else {
                int pos = inputBufferOffset;
                mem.write32(posAddr, pos);

                if (inputBufferOffset < inputBufferSize) {
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_ALL_DATA_DECODED;
                }
            }
        }
    }

    public void sceAtracGetSoundSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int endSampleAddr = cpu.gpr[5];
        int loopStartSampleAddr = cpu.gpr[6];
        int loopEndSampleAddr = cpu.gpr[7];

        log.warn(String.format("PARTIAL: sceAtracGetSoundSample atracID = %d, endSampleAddr = 0x%08X, loopStartSampleAddr = 0x%08X, loopEndSampleAddr = 0x%08X", atID, endSampleAddr, loopStartSampleAddr, loopEndSampleAddr));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetSoundSample: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            int endSample = getAtracCodec(atID).getAtracEndSample();
            int loopStartSample = -1;
            int loopEndSample = -1;
            if (endSample < 0) {
                endSample = inputFileSize;
            }
            if (atracLoopMap.containsKey(atID)) {
                loopStartSample = 0;
                loopEndSample = atracLoopMap.get(atID) - 1;
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

        int atID = cpu.gpr[4];
        int channelAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracGetChannel: atracID = " + atID + ", channelAddr =0x" + Integer.toHexString(channelAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetChannel: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            // Writes the number of used channels (1 - MONO / 2 - STEREO).
            mem.write32(channelAddr, atracChannels);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetMaxSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int maxSamplesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetMaxSample: atracID = %d, maxSamplesAddr = 0x%08X", atID, maxSamplesAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetMaxSample: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            if (mem.isAddressGood(maxSamplesAddr)) {
                mem.write32(maxSamplesAddr, maxSamples);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetNextSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int nbrSamplesAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("PARTIAL: sceAtracGetNextSample: atracID=%d, nbrSamplesAddr=0x%08x", atID, nbrSamplesAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetNextSample: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            int samples = maxSamples;
            if (inputBufferOffset >= inputBufferSize) {
                samples = 0; // No more data available in input buffer
            }
            if (mem.isAddressGood(nbrSamplesAddr)) {
                mem.write32(nbrSamplesAddr, samples);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetBitrate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int bitrateAddr = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceAtracGetBitrate: atracID = " + atID + ", bitrateAddr =0x" + Integer.toHexString(bitrateAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetBitrate: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(bitrateAddr, atracBitrate);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetLoopStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int loopNbr = cpu.gpr[5];
        int statusAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetLoopStatus atracID=%d, loopNbr=0x%08x, statusAddr=0x%08X", atID, loopNbr, statusAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetLoopStatus: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            int loops = 0;
            if (atracLoopMap.containsKey(atID)) {
                loops = atracLoopMap.get(atID);
            }
            if (mem.isAddressGood(loopNbr)) {
                mem.write32(loopNbr, loops);
            }
            if (mem.isAddressGood(statusAddr)) {
                mem.write32(statusAddr, PSP_ATRAC_STATUS_LOOP_STREAM_DATA);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int loopNbr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceAtracSetLoopNum: atracID = " + atID + ", loopNbr = " + loopNbr);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracSetLoopNum: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            // Defines the amount of loops for this atID.
            atracLoopMap.put(atID, loopNbr);
            getAtracCodec(atID).setAtracLoopCount(loopNbr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetBufferInfoForReseting(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int sample = cpu.gpr[5];
        int bufferInfoAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("PARTIAL: sceAtracGetBufferInfoForReseting atracID=%d, sample=%d, bufferInfoAddr=0x%08x", atID, sample, bufferInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetBufferInfoForReseting: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            hleAtracGetBufferInfoForReseting(atID, sample, bufferInfoAddr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracResetPlayPosition(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
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
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracResetPlayPosition: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            inputBufferOffset = sample * 4;
            inputFileSize = inputBufferSize;
            inputFileOffset = inputBufferSize;
            getAtracCodec(atID).atracResetPlayPosition(sample);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetInternalErrorInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int errorAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("PARTIAL: sceAtracGetInternalErrorInfo atracId=%d, errorAddr=0x%08x", atID, errorAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getIDCodecType(atID) < 0) {
            log.warn("sceAtracGetInternalErrorInfo: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            mem.write32(errorAddr, 0);  // No error.
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