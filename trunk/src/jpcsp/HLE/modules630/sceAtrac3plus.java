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

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.util.Utilities;

public class sceAtrac3plus extends jpcsp.HLE.modules600.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @HLEFunction(nid = 0x0C116E1B, version = 630)
    public int sceAtracLowLevelDecode(@CheckArgument("checkAtracID") int atID, int sourceAddr, TPointer32 decodeAddr, TPointer samplesAddr, TPointer32 decodePosAddr) {
        Memory mem = Processor.memory;

        log.warn(String.format("PARTIAL: sceAtracLowLevelDecode atID=0x%X, sourceAddr=0x%08X, decodeAddr=%s, samplesAddr=%s, decodePosAddr=%s", atID, sourceAddr, decodeAddr, samplesAddr, decodePosAddr));

        if (log.isTraceEnabled() && Memory.isAddressGood(sourceAddr)) {
        	int length = 0x130; // How to find the input length?
        	log.trace(String.format("sceAtracLowLevelDecode input:%s", Utilities.getMemoryDump(sourceAddr, length)));
        }

        decodePosAddr.setValue(0); // Set the decoding position to 0.
        if (samplesAddr.isAddressGood()) {
        	// This line is causing a problem in "Heroes Phantasia JPN(NPJH50558)".
        	// See http://www.emunewz.net/forum/showthread.php?tid=23228
            // decodeAddr.setValue(samplesAddr.getAddress() - 2160); // Rewind to the sample's header block.
        }

        // Erase all content in the samples address.
        int samples = 1024; // Always return 1024 samples?
        mem.memset(samplesAddr.getAddress(), (byte) 0, samples * 4);
        
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

    @HLEFunction(nid = 0x1575D64B, version = 630)
    public int sceAtracLowLevelInitDecoder(@CheckArgument("checkAtracID") int atID) {
    	log.warn(String.format("UNIMPLEMENTED: sceAtracLowLevelInitDecoder atID=0x%X", atID));

        return 0;
    }
}