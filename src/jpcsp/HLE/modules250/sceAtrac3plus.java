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
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;

public class sceAtrac3plus extends jpcsp.HLE.modules150.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @HLEFunction(nid = 0xB3B5D042, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetOutputChannel(@CheckArgument("checkAtracID") int atID, TPointer32 outputChannelAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetOutputChannel atracID=0x%X, outputChannelAddr=%s", atID, outputChannelAddr));
        }

        outputChannelAddr.setValue(2);

        return 0;
    }

    @HLEFunction(nid = 0xECA32A99, version = 250, checkInsideInterrupt = true)
    public boolean sceAtracIsSecondBufferNeeded(@CheckArgument("checkAtracID") int atID) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracIsSecondBufferNeeded atracId=0x%X", atID));
        }

        AtracID id = atracIDs.get(atID);
        // 0 -> Second buffer isn't needed.
        // 1 -> Second buffer is needed.
        return id.isSecondBufferNeeded();
    }

    @HLEFunction(nid = 0x132F1ECA, version = 250, checkInsideInterrupt = true)
    public int sceAtracReinit(int at3IDNum, int at3plusIDNum) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracReinit at3IDNum=%d, at3plusIDNum=%d", at3IDNum, at3plusIDNum));
        }

        if (at3IDNum + at3plusIDNum * 2 > 6) {
            // The total ammount of AT3 IDs and AT3+ IDs (x2) can't be superior to 6.
            return SceKernelErrors.ERROR_ATRAC_NO_ID;
        }

        hleAtracReinit(at3IDNum, at3plusIDNum);

        return 0;
    }

    @HLEFunction(nid = 0x2DD3E298, version = 250, checkInsideInterrupt = true)
    public int sceAtracGetBufferInfoForResetting(@CheckArgument("checkAtracID") int atID, int sample, TPointer32 bufferInfoAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetBufferInfoForResetting atracID=0x%X, sample=%d, bufferInfoAddr=%s", atID, sample, bufferInfoAddr));
        }

        AtracID id = atracIDs.get(atID);
        id.getBufferInfoForResetting(sample, bufferInfoAddr);

        return 0;
    }

    @HLEFunction(nid = 0x5CF9D852, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBuffer(@CheckArgument("checkAtracID") int atID, TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetMOutHalfwayBuffer atID=0x%X, MOutHalfBuffer=%s, readSize=0x%08X, MOutHalfBufferSize=0x%08X", atID, MOutHalfBuffer, readSize, MOutHalfBufferSize));
        }

        AtracID id = atracIDs.get(atID);
        id.setData(MOutHalfBuffer.getAddress(), readSize, MOutHalfBufferSize, false);

        return 0;
    }

    @HLEFunction(nid = 0xF6837A1A, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutData(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        log.warn(String.format("Unimplemented function sceAtracSetMOutData %08x %08x %08x %08x %08x %08x", unknown1, unknown2, unknown3, unknown4, unknown5, unknown6));

        return 0;
    }

    @HLEFunction(nid = 0x472E3825, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutDataAndGetID(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        log.warn(String.format("Unimplemented function sceAtracSetMOutDataAndGetID %08x %08x %08x %08x %08x %08x", unknown1, unknown2, unknown3, unknown4, unknown5, unknown6));

        return 0;
    }

    @HLEFunction(nid = 0x9CD7DE03, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetMOutHalfwayBufferAndGetID(TPointer MOutHalfBuffer, int readSize, int MOutHalfBufferSize) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetMOutHalfwayBufferAndGetID buffer=%s, readSize=0x%08X, bufferSize=0x%08X", MOutHalfBuffer, readSize, MOutHalfBufferSize));
        }

    	int codecType = getCodecType(MOutHalfBuffer.getAddress());
        int atID = hleCreateAtracID(codecType);
        if (atracIDs.containsKey(atID)) {
            atracIDs.get(atID).setData(MOutHalfBuffer.getAddress(), readSize, MOutHalfBufferSize, false);
        }

        return atID;
    }

    @HLEFunction(nid = 0x5622B7C1, version = 250, checkInsideInterrupt = true)
    public int sceAtracSetAA3DataAndGetID(TPointer buffer, int bufferSize, int fileSize, @CanBeNull TPointer32 metadataSizeAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetAA3DataAndGetID buffer=%s, bufferSize=0x%08X, fileSize=0x%08X, metadataSizeAddr=%s", buffer, bufferSize, fileSize, metadataSizeAddr));
        }

    	int codecType = getCodecType(buffer.getAddress());
        int atID = hleCreateAtracID(codecType);
        if (atracIDs.containsKey(atID)) {
            atracIDs.get(atID).setData(buffer.getAddress(), bufferSize, bufferSize, false);
        }
        metadataSizeAddr.setValue(0x400); // Dummy common value found in most .AA3 files.

        return atID;
    }

    @HLEFunction(nid = 0x5DD66588, version = 250)
    public int sceAtracSetAA3HalfwayBufferAndGetID(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
        log.warn(String.format("Unimplemented function sceAtracSetAA3HalfwayBufferAndGetID %08x %08x %08x %08x %08x %08x", unknown1, unknown2, unknown3, unknown4, unknown5, unknown6));

        return 0;
    }
}