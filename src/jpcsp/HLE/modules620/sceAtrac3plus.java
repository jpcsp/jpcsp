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
package jpcsp.HLE.modules620;

import jpcsp.Memory;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.connector.AtracCodec;
import jpcsp.util.Utilities;

@HLELogging
public class sceAtrac3plus extends jpcsp.HLE.modules600.sceAtrac3plus {
	protected int findRIFFHeader(int addr) {
		Memory mem = Memory.getInstance();
		for (int i = 0; i >= -512; i -= 4) {
			if (mem.read32(addr + i) == RIFF_MAGIC) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Found RIFF header at 0x%08X", addr + i));
					if (log.isTraceEnabled()) {
						log.trace(Utilities.getMemoryDump(addr + i, -i));
					}
				}
				return addr + i;
			}
		}

		return 0;
	}

	@HLEFunction(nid = 0x0C116E1B, version = 620)
    public int sceAtracLowLevelDecode(@CheckArgument("checkAtracID") int atID, TPointer sourceAddr, TPointer32 sourceBytesConsumedAddr, TPointer samplesAddr, TPointer32 sampleBytesAddr) {
        AtracID id = atracIDs.get(atID);
        AtracCodec atracCodec = id.getAtracCodec();

        if (log.isTraceEnabled()) {
        	log.trace(String.format("sceAtracLowLevelDecode input:%s", Utilities.getMemoryDump(sourceAddr.getAddress(), id.getSourceBufferLength())));
        }

        int sourceBytesConsumed = 0;
        if (id.getInputBufferAddr() == 0) {
        	int headerAddr = findRIFFHeader(sourceAddr.getAddress());
        	if (headerAddr != 0) {
        		sourceBytesConsumed = id.getSourceBufferLength();
        		id.setData(headerAddr, id.getSourceBufferLength() + (sourceAddr.getAddress() - headerAddr), id.getSourceBufferLength(), false);
        		if (atracCodec != null && id.getAtracCodecType() == PSP_MODE_AT_3) {
        			atracCodec.setAtracChannelStartLength(0x8000); // Only 0x8000 bytes are required to start decoding AT3
        		}
        		// Allow looping
        		id.setLoopNum(-1);
        	}
        } else {
        	// Estimate source bytes to be read based on current sample position
        	int estimatedFileOffset = (int) (((long) id.getInputFileSize()) * id.getAtracCurrentSample() / id.getAtracEndSample());
        	sourceBytesConsumed = Math.max(0, estimatedFileOffset - id.getInputFileOffset());
        	sourceBytesConsumed = Math.min(sourceBytesConsumed, id.getSourceBufferLength());
        	id.addStreamData(sourceAddr.getAddress(), sourceBytesConsumed);
        }

    	int bytesPerSample = id.getAtracOutputChannels() << 1;
        if (atracCodec != null) {
	        int samples = atracCodec.atracDecodeData(atID, samplesAddr.getAddress(), id.getAtracOutputChannels());
        	if (sourceBytesConsumed < id.getSourceBufferLength()) {
        		// Not enough data in the channel or running soon out of data?
        		if (samples < id.getMaxSamples() || atracCodec.getChannelLength() < 0x8000) {
        			// Consume as much as possible...
        			id.addStreamData(sourceAddr.getAddress() + sourceBytesConsumed, id.getSourceBufferLength() - sourceBytesConsumed);
        			sourceBytesConsumed = id.getSourceBufferLength();
        		}
        	}

        	if (samples <= 1) {
	        	samples = id.getMaxSamples();

	        	int sampleBytes = samples * bytesPerSample;
		        samplesAddr.clear(sampleBytes);
	        } else if (samples > 0) {
	        	id.setDecodedSamples(samples);

	        	// Always return MaxSamples
	        	if (samples < id.getMaxSamples()) {
	    	        int sampleBytes = samples * bytesPerSample;
	        		int fillSamples = id.getMaxSamples() - samples;
	        		int fillSampleBytes = fillSamples * bytesPerSample;
	        		samplesAddr.clear(sampleBytes, fillSampleBytes);
	        		samples = id.getMaxSamples();
	        	}
	        }

	        int sampleBytes = samples * bytesPerSample;
	        sampleBytesAddr.setValue(sampleBytes);

	        if (log.isDebugEnabled()) {
	        	log.debug(String.format("sceAtracLowLevelDecode returning %d samples (0x%X bytes), 0x%X source bytes consumed, sample position %d/%d, file position %d/%d", samples, sampleBytes, sourceBytesConsumed, id.getAtracCurrentSample(), id.getAtracEndSample(), id.getInputFileOffset(), id.getInputFileSize()));
	        	if (log.isTraceEnabled()) {
	        		log.trace(Utilities.getMemoryDump(samplesAddr.getAddress(), sampleBytes));
	        	}
	        }
        } else {
	        int samples = id.getMaxSamples();
	        int sampleBytes = samples * bytesPerSample;
	        sampleBytesAddr.setValue(sampleBytes);
	        // Return empty samples
	        samplesAddr.clear(sampleBytes);

        }

        // Consume a part of the Atrac3 source buffer
        sourceBytesConsumedAddr.setValue(sourceBytesConsumed);

        /*
         * Low level ATRAC3+ header structure:
         * 
         * 00 10 -> Codec type 0x1000
         * 01 00 -> Channel mode (Mono - 0x1 or Stereo - 0x2)
         * 44 ac -> Frequency (always 0xAC44
         * 64 00 -> Bit depth
         * c0 00 00 00 -> Unknown
         * 30 81 00 00 -> Unknown
         * 00 08 00 00 -> Header size (0x800)
         * 70 09 00 00 -> Unknown
         * 5e 2c 01 00 -> Data length? (0x12c5e)
         * 00 00 00 00 -> Unknown
         * 00 00 00 00 -> Unknown
         * 01 00 00 00 -> Unknown
         * be e2 ff ff -> Next source address
         * 00 00 00 00 -> NULL
         * 42 1d 00 00 -> Unkown
         * 00 00 00 00 -> NULL
         * fe ff ff ff -> Unknown
         * 70 03 68 09 -> Pointer to next block
         * 00 08 00 00 -> Next header size
         * 80 0b 68 09 -> Samples address
         * 00 00 00 00 -> NULL
         * 00 00 00 00 -> NULL
         * 00 00 00 00 -> NULL
         * 00 00 00 00 -> NULL
         * 70 09 00 00 -> NULL
         * 00 00 00 00 -> NULL
         * 
         */

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

        AtracID id = atracIDs.get(atID);

        id.setAtracChannels(numberOfChannels);
        if (numberOfChannels == 1 && numberOfChannels == outputChannels) {
        	id.setAtracOutputChannels(outputChannels);
        }
        id.setSourceBufferLength(sourceBufferLength);

        return 0;
    }
}