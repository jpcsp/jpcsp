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

import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.ThreadMan;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState; // New-Style Processor
import jpcsp.HLE.Modules;
import jpcsp.connector.AtracCodec;

public class sceAtrac3plus implements HLEModule {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(sceAtracStartEntryFunction, 0xD1F59FDB);
            mm.addFunction(sceAtracEndEntryFunction, 0xD5C28CC0);
            mm.addFunction(sceAtracGetAtracIDFunction, 0x780F88D1);
            mm.addFunction(sceAtracReleaseAtracIDFunction, 0x61EB33F5);
            mm.addFunction(sceAtracSetDataFunction, 0x0E2A73AB);
            mm.addFunction(sceAtracSetHalfwayBufferFunction, 0x3F6E26B5);
            mm.addFunction(sceAtracSetDataAndGetIDFunction, 0x7A20E7AF);
            mm.addFunction(sceAtracSetHalfwayBufferAndGetIDFunction, 0x0FAE370E);
            mm.addFunction(sceAtracDecodeDataFunction, 0x6A8C3CD5);
            mm.addFunction(sceAtracGetRemainFrameFunction, 0x9AE849A7);
            mm.addFunction(sceAtracGetStreamDataInfoFunction, 0x5D268707);
            mm.addFunction(sceAtracAddStreamDataFunction, 0x7DB31251);
            mm.addFunction(sceAtracGetSecondBufferInfoFunction, 0x83E85EA0);
            mm.addFunction(sceAtracSetSecondBufferFunction, 0x83BF7AFD);
            mm.addFunction(sceAtracGetNextDecodePositionFunction, 0xE23E3A35);
            mm.addFunction(sceAtracGetSoundSampleFunction, 0xA2BBA8BE);
            mm.addFunction(sceAtracGetChannelFunction, 0x31668BAA);
            mm.addFunction(sceAtracGetMaxSampleFunction, 0xD6A5F2F7);
            mm.addFunction(sceAtracGetNextSampleFunction, 0x36FAABFB);
            mm.addFunction(sceAtracGetBitrateFunction, 0xA554A158);
            mm.addFunction(sceAtracGetLoopStatusFunction, 0xFAA4F89B);
            mm.addFunction(sceAtracSetLoopNumFunction, 0x868120B5);
            mm.addFunction(sceAtracGetBufferInfoForResetingFunction, 0xCA3CA3D2);
            mm.addFunction(sceAtracResetPlayPositionFunction, 0x644E5607);
            mm.addFunction(sceAtracGetInternalErrorInfoFunction, 0xE88F759B);

            atracCodecs = new HashMap<Integer, AtracCodec>();
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

    protected static final String uidPurpose = "sceAtrac3plus";

    protected static final int AT3_MAGIC      = 0x00000270; // "AT3"
    protected static final int AT3_PLUS_MAGIC = 0x0000FFFE; // "AT3PLUS"
    protected static final int RIFF_MAGIC     = 0x46464952; // "RIFF"
    protected static final int WAVE_MAGIC     = 0x45564157; // "WAVE"

    protected static final int PSP_ATRAC_ALLDATA_IS_ON_MEMORY              = -1;
    protected static final int PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY  = -2;
    protected static final int PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY	   = -3;


    protected static final int PSP_MODE_AT_3_PLUS = 0x00001000;
    protected static final int PSP_MODE_AT_3      = 0x00001001;

    protected int inputBufferAddr; // currently not used
    protected int inputBufferSize; // currently not used
    protected int inputBufferOffset;
    protected int inputFileSize;
    protected int inputFileOffset;
    public static int maxSamples;
    public static final int remainFrames = -1;

    protected HashMap<Integer, AtracCodec> atracCodecs;

    protected AtracCodec getAtracCodec(int atracID) {
    	return atracCodecs.get(atracID);
    }

    public void sceAtracStartEntry(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceAtracStartEntry [0xD1F59FDB]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAtracEndEntry(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceAtracEndEntry [0xD5C28CC0]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAtracGetAtracID(Processor processor) {
        CpuState cpu = processor.cpu;

        int codecType = cpu.gpr[4]; //0x1000(AT3PLUS), 0x1001(AT3).

        if (codecType == PSP_MODE_AT_3)
            maxSamples = 1024;
        else if (codecType == PSP_MODE_AT_3_PLUS)
            maxSamples = 2048;


        Modules.log.warn("PARTIAL:sceAtracGetAtracID: codecType = 0x" + Integer.toHexString(codecType));

        cpu.gpr[2] = hleCreateAtracID(codecType);
    }
    
    public void sceAtracReleaseAtracID(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];

        Modules.log.warn("Skipping:sceAtracReleaseAtracID: atracID = " + atID);
        AtracCodec atracCodec = getAtracCodec(atID);
        if(atracCodec != null)
        atracCodec.finish();
        atracCodecs.remove(atID);
        SceUidManager.releaseUid(atID, uidPurpose);
        
        cpu.gpr[2] = 0;
    }

    protected void hleAtracSetData(int atracID, int buffer, int bufferSize) {
        inputBufferAddr = buffer;
        inputBufferSize = bufferSize;
        inputBufferOffset = 0;
        inputFileSize = inputBufferSize;
        inputFileOffset = inputBufferSize;

        Memory mem = Memory.getInstance();
        int magic = mem.read32(inputBufferAddr);
        if (magic == RIFF_MAGIC && inputBufferSize >= 8) {
        	// RIFF file format:
        	// Offset 0: 'RIFF'
        	// Offset 4: file length - 8
        	// Offset 8: 'WAVE'
        	inputFileSize = mem.read32(inputBufferAddr + 4) + 8;
        }

        Modules.log.info(String.format("hleAtracSetData atracID=%d, bufferSize=0x%x, fileSize=%x", atracID, inputBufferSize, inputFileSize));
        getAtracCodec(atracID).atracSetData(buffer, bufferSize, inputFileSize);
    }

    protected void hleAtracAddStreamData(int atracID, int length) {
    	inputFileOffset += length;
    	inputBufferOffset -= length;
    	getAtracCodec(atracID).atracAddStreamData(inputBufferAddr, length);
    }

    protected int hleCreateAtracID(int codecType) {
    	int atracID = SceUidManager.getNewUid(uidPurpose);
    	if (codecType != PSP_MODE_AT_3 && codecType != PSP_MODE_AT_3_PLUS) {
			Modules.log.warn("hleGetAtracID unknown codecType " + codecType);
    	}

    	AtracCodec atracCodec = new AtracCodec();
    	atracCodecs.put(atracID, atracCodec);

    	return atracID;
    }

    protected int getRemainFrames(int atracID) {
    	if (inputFileOffset >= inputFileSize) {
    		return -1; // All data in input buffer
    	} else {
    		return (inputBufferSize - inputBufferOffset) / (4 * maxSamples);
    	}
    }

    protected void hleAtracGetBufferInfoForReseting(int atracID, int sample, int unk1Addr) {
    	Memory mem = Memory.getInstance();

    	if (mem.isAddressGood(unk1Addr)) {
    		// Address of an unknown structure of size 32
    		mem.write32(unk1Addr +  4, 0);
    		mem.write32(unk1Addr + 20, 0);
    	}
    }

    public void sceAtracSetData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int buffer = cpu.gpr[5];
        int bufferSize = cpu.gpr[6];//16384

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceAtracSetData: atID = %d, buffer = 0x%08X, bufferSize = 0x%08X", atID, buffer, bufferSize));
        }
        int codecType = 0;
        int at3magic = 0;
        if (mem.isAddressGood(buffer)) { // Check!
            at3magic = mem.read32(buffer + 20);
            at3magic &= 0x0000FFFF;

            if (at3magic == AT3_MAGIC)
                codecType = PSP_MODE_AT_3;
            else if (at3magic == AT3_PLUS_MAGIC)
                codecType = PSP_MODE_AT_3_PLUS;

            if (codecType == PSP_MODE_AT_3)
                maxSamples = 1024;
            else if (codecType == PSP_MODE_AT_3_PLUS)
                maxSamples = 2048;

            //atID = hleCreateAtracID(codecType);
            //hleAtracSetData(atID, buffer, bufferSize);
        }
        hleAtracSetData(atID, buffer, bufferSize);

        cpu.gpr[2] = 0;
    }
    
    public void sceAtracSetHalfwayBuffer(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceAtracSetHalfwayBuffer [0x3F6E26B5]");
        
        cpu.gpr[2] = 0;
    }

    public void sceAtracSetDataAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer = cpu.gpr[4];
        int bufferSize = cpu.gpr[5];

        int codecType = 0;
        int at3magic = 0;
        int atID = 0;

        if (mem.isAddressGood(buffer)) {
            at3magic = mem.read32(buffer + 20);
            at3magic &= 0x0000FFFF;

            if (at3magic == AT3_MAGIC)
                codecType = PSP_MODE_AT_3;
            else if (at3magic == AT3_PLUS_MAGIC)
                codecType = PSP_MODE_AT_3_PLUS;

            if (codecType == PSP_MODE_AT_3)
                maxSamples = 1024;
            else if (codecType == PSP_MODE_AT_3_PLUS)
                maxSamples = 2048;
            
            atID = hleCreateAtracID(codecType);
            hleAtracSetData(atID, buffer, bufferSize);
        }

        if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(String.format("sceAtracSetDataAndGetID buffer = 0x%08X, bufferSize = 0x%08X, codecType = 0x%04X", buffer, bufferSize, codecType));
            }

        cpu.gpr[2] = atID;
    }

    public void sceAtracSetHalfwayBufferAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceAtracSetHalfwayBufferAndGetID [0x0FAE370E]");
        
        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAtracDecodeData(Processor processor) {
    	CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int samplesAddr = cpu.gpr[5];
        int samplesNbrAddr = cpu.gpr[6];
        int outEndAddr = cpu.gpr[7];
        int remainFramesAddr = cpu.gpr[8];

        int result = 0;
        AtracCodec atracCodec = getAtracCodec(atID);
        if(atracCodec != null) {
        int samples = atracCodec.atracDecodeData(samplesAddr);
        if (samples < 0) {
            int fakedSamples = maxSamples;
            if (inputBufferOffset >= inputBufferSize) {
            	fakedSamples = 0; // No more data in input buffer
            }

            Modules.log.warn(String.format("Unimplemented sceAtracDecodeData: atracID=%d, samplesAddr=0x%08X, samplesNbrAddr=0x%08X, outEndAddr=0x%08X, remainFramesAddr=0x%08X, returning samples=%d",
                    atID, samplesAddr, samplesNbrAddr, outEndAddr, remainFramesAddr, fakedSamples));

            // Assume consuming as many ATRAC3 input bytes as samples
            // (this is faked because it would mean ATRAC3 does not compress audio at all)
            int consumedInputBytes = fakedSamples;
            if (inputBufferOffset + consumedInputBytes > inputBufferSize) {
            	consumedInputBytes = inputBufferSize - inputBufferOffset;
            }

        	inputBufferOffset += consumedInputBytes;

            // AtracCodec cannot decode it, return dummy values
            if (mem.isAddressGood(samplesAddr)) {
            	// Simulate empty audio
            	mem.memset(samplesAddr, (byte) 0, fakedSamples * 4); // 4 bytes per sample
            }
            if (mem.isAddressGood(samplesNbrAddr)) {
                mem.write32(samplesNbrAddr, fakedSamples); // Write dummy ammount of samples. If it's 0, some games will fall into a loop.
            }
            if (mem.isAddressGood(outEndAddr)) {
            	int end = inputFileOffset >= inputFileSize ? 1 : 0;
                mem.write32(outEndAddr, end); // end of samples?
            }
            if (mem.isAddressGood(remainFramesAddr)) {
                mem.write32(remainFramesAddr, getRemainFrames(atID));
            }

            if (consumedInputBytes == 0 && inputFileOffset < inputFileSize) {
            	result = 0x80630023; // No more data in input buffer
            }
        } else {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug(String.format("sceAtracDecodeData using AtracCodec: atracID=%d, samplesAddr=0x%08X, samplesNbr=%d, end=%d, remainFrames=%d", atID, samplesAddr, samples, atracCodec.getAtracEnd(), atracCodec.getAtracRemainFrames()));
        	}

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
        ThreadMan.getInstance().hleKernelDelayThread(10000, false);// for the spamming warn.
    }

    public void sceAtracGetRemainFrame(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int remainFramesAddr = cpu.gpr[5];

        Modules.log.warn(String.format("Unimplemented sceAtracGetRemainFrame: atracID = %d, remainFramesAddr = 0x%08X", atID, remainFramesAddr));

        if (mem.isAddressGood(remainFramesAddr)) {
        	mem.write32(remainFramesAddr, getRemainFrames(atID));
        }
        
        cpu.gpr[2] = 0;
    }
    
    public void sceAtracGetStreamDataInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int writeAddr = cpu.gpr[5];
        int availableBytesAddr = cpu.gpr[6];
        int readOffsetAddr = cpu.gpr[7];

    	int availableBytes = inputBufferOffset;
    	if (inputFileOffset >= inputFileSize) {
    		// All data is in the buffer
    		availableBytes = 0;
    	} else if (availableBytes > (inputFileSize - inputFileOffset)) {
    		// Do not need more data than input file size
    		availableBytes = inputFileSize - inputFileOffset;
    	}

        Modules.log.warn(String.format("Partial sceAtracGetStreamDataInfo: atID=0x%08x, writeAddr=0x%08x, availableBytesAddr=0x%08x, readOffsetAddr=0x%08x, returning availableBytes=%d",
                atID, writeAddr, availableBytesAddr, readOffsetAddr, availableBytes));

        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(writeAddr)) {
        	mem.write32(writeAddr, inputBufferAddr);
        }
        if (mem.isAddressGood(availableBytesAddr)) {
        	mem.write32(availableBytesAddr, inputBufferOffset);
        }
        if (mem.isAddressGood(readOffsetAddr)) {
        	mem.write32(readOffsetAddr, inputFileOffset);
        }

        cpu.gpr[2] = 0;
    }

    public void sceAtracAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int bytesToAdd = cpu.gpr[5];

        Modules.log.warn(String.format("Partial sceAtracAddStreamData: atracID=%d, bytesToAdd=0x%x", atID, bytesToAdd));

        hleAtracAddStreamData(atID, bytesToAdd);
        cpu.gpr[2] = 0;
    }

    public void sceAtracGetSecondBufferInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int atID = cpu.gpr[4];
        int outPosition = cpu.gpr[5];
        int outBytes = cpu.gpr[6];

        Modules.log.warn("Ignoring sceAtracGetSecondBufferInfo: atracID = " + atID + ", outPos=0x" + Integer.toHexString(outPosition)
                + ", outBytes=0x" + Integer.toHexString(outBytes));

        cpu.gpr[2] = 0x80630022; // No second buffer used?
    }

    public void sceAtracSetSecondBuffer(Processor processor) {
        CpuState cpu = processor.cpu;
        
        Modules.log.warn("Skipping:sceAtracSetSecondBuffer");

        cpu.gpr[2] = 0;
    }
    
    public void sceAtracGetNextDecodePosition(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceAtracGetNextDecodePosition [0xE23E3A35]");

        cpu.gpr[2] = 0xDEADC0DE;
    }
    
    public void sceAtracGetSoundSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atId = cpu.gpr[4];
        int endSampleAddr = cpu.gpr[5];
        int loopStartSampleAddr = cpu.gpr[6];
        int loopEndSampleAddr = cpu.gpr[7];

        Modules.log.warn(String.format("Skipping:sceAtracGetSoundSample atracID = %d, endSampleAddr = 0x%08X, , loopStartSampleAddr = 0x%08X, loopEndSampleAddr = 0x%08X", atId, endSampleAddr, loopStartSampleAddr, loopEndSampleAddr));

        int endSample = getAtracCodec(atId).getAtracEndSample();
        if (endSample < 0) {
        	endSample = inputFileSize;
        }
        mem.write32(endSampleAddr, endSample);
        mem.write32(loopStartSampleAddr, -1);
        mem.write32(loopEndSampleAddr, -1);
		
        cpu.gpr[2] = 0;
    }

    public void sceAtracGetChannel(Processor processor) {
        CpuState cpu = processor.cpu;
        
         //int atID = cpu.gpr[4]; // ??
         //int channel = cpu.gpr[5]; // ??
        Modules.log.warn("Unimplemented NID function sceAtracGetChannel [0x31668BAA]");

        cpu.gpr[2] = 0;
    }

    public void sceAtracGetMaxSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;
        
        int atID = cpu.gpr[4];
        int maxSamplesAddr = cpu.gpr[5];

        Modules.log.warn(String.format("Unimplemented sceAtracGetMaxSample: atracID = %d, maxSamplesAddr = 0x%08X", atID, maxSamplesAddr));

        if (mem.isAddressGood(maxSamplesAddr)) {
        	mem.write32(maxSamplesAddr, maxSamples);
        }

        cpu.gpr[2] = 0;
    }

    public void sceAtracGetNextSample(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int nbrSamplesAddr = cpu.gpr[5];

    	int samples = maxSamples;
    	if (inputBufferOffset >= inputBufferSize) {
    		samples = 0; // No more data available in input buffer
    	}

    	Modules.log.warn(String.format("Partial sceAtracGetNextSample: atracID=%d, nbrSamplesAddr=0x%08x, returning nbrSamples=%d", atID, nbrSamplesAddr, samples));

        if (mem.isAddressGood(nbrSamplesAddr)) {
        	mem.write32(nbrSamplesAddr, samples);
        }

        cpu.gpr[2] = 0;
    }

    public void sceAtracGetBitrate(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int bitrate = cpu.gpr[5];

        Modules.log.warn("Unimplemented sceAtracGetBitrate: atracID = " + atID + ", bitrate = " + bitrate);

        cpu.gpr[2] = 0;
    }

    public void sceAtracGetLoopStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int atID = cpu.gpr[4];
        int unk1Addr = cpu.gpr[5];
        int unk2Addr = cpu.gpr[6];

        Modules.log.warn(String.format("Unimplemented sceAtracGetLoopStatus atracID=%d, unk1Addr=0x%08x, unk2Addr=0x%08X", atID, unk1Addr, unk2Addr));

        if (mem.isAddressGood(unk1Addr)) {
        	mem.write32(unk1Addr, 0);
        }
        if (mem.isAddressGood(unk2Addr)) {
        	mem.write32(unk2Addr, 0);
        }

        cpu.gpr[2] = 0;
    }

    public void sceAtracSetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int loopNbr = cpu.gpr[5];

        Modules.log.warn("Unimplemented sceAtracSetLoopNum: atracID = " + atID + ", loopNbr= " + loopNbr);

        cpu.gpr[2] = 0;
    }

    public void sceAtracGetBufferInfoForReseting(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int sample = cpu.gpr[5];
        int unk1Addr = cpu.gpr[6];

        Modules.log.warn(String.format("Partial sceAtracGetBufferInfoForReseting atracID=%d, sample=%d, unk1Addr=0x%08x", atID, sample, unk1Addr));
        hleAtracGetBufferInfoForReseting(atID, sample, unk1Addr);

        cpu.gpr[2] = 0;
    }
    
    public void sceAtracResetPlayPosition(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int sample = cpu.gpr[5];
        int unknown1 = cpu.gpr[6];
        int unknown2 = cpu.gpr[7];
        Modules.log.warn(String.format("Partial sceAtracResetPlayPosition atracId=%d, sample=%d, %08X, %08X", atID, sample, unknown1, unknown2));

        inputBufferOffset = sample * 4;
        inputFileSize = inputBufferSize;
        inputFileOffset = inputBufferSize;
        getAtracCodec(atID).atracResetPlayPosition(sample);

        cpu.gpr[2] = 0;
    }
    
    public void sceAtracGetInternalErrorInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("Unimplemented NID function sceAtracGetInternalErrorInfo [0xE88F759B]");

        cpu.gpr[2] = 0xDEADC0DE;
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
};
