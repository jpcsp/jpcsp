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
package jpcsp.HLE.modules250;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;

@HLELogging
public class sceAtrac3plus extends jpcsp.HLE.modules150.sceAtrac3plus {
    @HLEFunction(nid = 0xB3B5D042, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetOutputChannel(@CheckArgument("checkAtracID") int atID, TPointer32 outputChannelAddr) {
    	AtracID id = atracIDs.get(atID);
        outputChannelAddr.setValue(id.getAtracOutputChannels());

        return 0;
    }

    @HLEFunction(nid = 0xECA32A99, version = 250, checkInsideInterrupt = true)
    public boolean sceAtracIsSecondBufferNeeded(@CheckArgument("checkAtracID") int atID) {
        AtracID id = atracIDs.get(atID);
        // 0 -> Second buffer isn't needed.
        // 1 -> Second buffer is needed.
        return id.isSecondBufferNeeded();
    }

    @HLEFunction(nid = 0x132F1ECA, version = 250, checkInsideInterrupt = true)
    public int sceAtracReinit(int at3IDNum, int at3plusIDNum) {
        if (at3IDNum + at3plusIDNum * 2 > 6) {
            // The total ammount of AT3 IDs and AT3+ IDs (x2) can't be superior to 6.
            return SceKernelErrors.ERROR_ATRAC_NO_ID;
        }

        hleAtracReinit(at3IDNum, at3plusIDNum);

        return 0;
    }

    @HLEFunction(nid = 0x2DD3E298, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetBufferInfoForResetting(@CheckArgument("checkAtracID") int atID, int sample, TPointer32 bufferInfoAddr) {
        AtracID id = atracIDs.get(atID);
        id.getBufferInfoForResetting(sample, bufferInfoAddr);

        return 0;
    }

    @HLEFunction(nid = 0x5CF9D852, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBuffer(@CheckArgument("checkAtracID") int atID, TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
        AtracID id = atracIDs.get(atID);
        id.setData(MOutHalfBuffer.getAddress(), readSize, MOutHalfBufferSize, false);
        if (id.getAtracChannels() == 1) {
        	// Set Mono output
        	id.setAtracOutputChannels(1);
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF6837A1A, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutData(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x472E3825, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutDataAndGetID(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        return 0;
    }

    @HLEFunction(nid = 0x9CD7DE03, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBufferAndGetID(TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
    	int codecType = getCodecType(MOutHalfBuffer.getAddress());
        int atID = hleCreateAtracID(codecType);
        if (atracIDs.containsKey(atID)) {
        	AtracID id = atracIDs.get(atID);
            id.setData(MOutHalfBuffer.getAddress(), readSize, MOutHalfBufferSize, false);
            if (id.getAtracChannels() == 1) {
            	// Set Mono output
            	id.setAtracOutputChannels(1);
            }
        }

        return atID;
    }

    @HLEFunction(nid = 0x5622B7C1, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetAA3DataAndGetID(TPointer buffer, int bufferSize, int fileSize, @CanBeNull TPointer32 metadataSizeAddr) {
    	int codecType = getCodecType(buffer.getAddress());
        int atID = hleCreateAtracID(codecType);
        if (atracIDs.containsKey(atID)) {
            atracIDs.get(atID).setData(buffer.getAddress(), bufferSize, bufferSize, false);
        }
        metadataSizeAddr.setValue(0x400); // Dummy common value found in most .AA3 files.

        return atID;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5DD66588, version = 250)
    public int sceAtracSetAA3HalfwayBufferAndGetID(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        return 0;
    }
}