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

import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.media.codec.ICodec;
import jpcsp.util.Utilities;

@HLELogging
public class sceAtrac3plus extends jpcsp.HLE.modules600.sceAtrac3plus {
	@HLEFunction(nid = 0x0C116E1B, version = 620)
    public int sceAtracLowLevelDecode(@CheckArgument("checkAtracID") int atID, TPointer sourceAddr, TPointer32 sourceBytesConsumedAddr, TPointer samplesAddr, TPointer32 sampleBytesAddr) {
        AtracID id = atracIDs.get(atID);
        ICodec codec = id.getCodec();

        if (log.isTraceEnabled()) {
        	log.trace(String.format("sceAtracLowLevelDecode input:%s", Utilities.getMemoryDump(sourceAddr.getAddress(), id.getSourceBufferLength())));
        }

        int sourceBytesConsumed = 0;
    	int bytesPerSample = id.getAtracOutputChannels() << 1;
        if (codec != null) {
        	int result = codec.decode(sourceAddr.getAddress(), id.getSourceBufferLength(), samplesAddr.getAddress());
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceAtracLowLevelDecode codec returned 0x%08X", result));
        	}
        	if (result < 0) {
        		log.info(String.format("sceAtracLowLevelDecode codec returning 0x%08X", result));
        		return result;
        	}
        	sourceBytesConsumed = result > 0 ? id.getSourceBufferLength() : 0;
        	sampleBytesAddr.setValue(codec.getNumberOfSamples() * bytesPerSample);
        }

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

        AtracID id = atracIDs.get(atID);

        int result = 0;

        id.setAtracChannels(numberOfChannels);
        if (numberOfChannels == 1 && numberOfChannels == outputChannels) {
        	id.setAtracOutputChannels(outputChannels);
        }
        id.setSourceBufferLength(sourceBufferLength);

        if (id.getCodec() != null) {
        	// TODO How to find out the codingMode for AT3 audio? Assume STEREO, not JOINT_STEREO
    		result = id.getCodec().init(sourceBufferLength, numberOfChannels, outputChannels, 0);
    		id.setCodecInitialized();
    	}

        return result;
    }
}