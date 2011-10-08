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
package jpcsp.HLE.modules630;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ATRAC_BAD_ID;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.util.Utilities;

public class sceAtrac3plus extends jpcsp.HLE.modules600.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @HLEFunction(nid = 0x0C116E1B, version = 630)
    public int sceAtracLowLevelDecode(int atID, TPointer sourceAddr, TPointer32 unknownAddr, TPointer samplesAddr, TPointer32 resultAddr) {
        Memory mem = Processor.memory;
    	atID &= atracIDMask;

        log.warn(String.format("UNIMPLEMENTED: sceAtracLowLevelDecode atID=%d, sourceAddr=%s, unknownAddr=%s(content=0x%08X), samplesAddr=%s, resultAddr=%s", atID, sourceAddr, unknownAddr, unknownAddr.getValue(), samplesAddr, resultAddr));

        if (!atracIDs.containsKey(atID)) {
            log.warn(String.format("sceAtracLowLevelDecode: bad atID=%d", atID));
            throw new SceKernelErrorException(ERROR_ATRAC_BAD_ID);
        }

        if (log.isTraceEnabled()) {
        	int length = 0x130; // How to find the input length?
        	log.trace(String.format("sceAtracLowLevelDecode input:%s", Utilities.getMemoryDump(sourceAddr.getAddress(), length, 1, 16)));
        }

        resultAddr.setValue(1); // Must be non-zero

        int samples = 1024; // Always return 1024 samples?
        mem.memset(samplesAddr.getAddress(), (byte) 0, samples * 4);

        return 0;
    }

    @HLEFunction(nid = 0x1575D64B, version = 630)
    public int sceAtracLowLevelInitDecoder(int atID) {
    	atID &= atracIDMask;

    	log.warn(String.format("UNIMPLEMENTED: sceAtracLowLevelInitDecoder atID=%d", atID));

        if (!atracIDs.containsKey(atID)) {
            log.warn(String.format("sceAtracLowLevelInitDecoder: bad atID=%d", atID));
            throw new SceKernelErrorException(ERROR_ATRAC_BAD_ID);
        }

        return 0;
    }
}