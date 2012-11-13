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
package jpcsp.HLE.modules600;

import jpcsp.Memory;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules150.sceAtrac3plus.AtracID;

@HLELogging
public class sceSasCore extends jpcsp.HLE.modules500.sceSasCore {
	private static final int SASCORE_ATRAC3_CONTEXT_OFFSET = 20;
	private static final int SASCORE_VOICE_SIZE = 56;

	protected void setSasCoreAtrac3Context(int sasCore, int voice, int atrac3Context) {
		Memory mem = Memory.getInstance();
        mem.write32(sasCore + SASCORE_VOICE_SIZE * voice + SASCORE_ATRAC3_CONTEXT_OFFSET, atrac3Context);
	}

	protected int getSasCoreAtrac3Context(int sasCore, int voice) {
		Memory mem = Memory.getInstance();
		return mem.read32(sasCore + SASCORE_VOICE_SIZE * voice + SASCORE_ATRAC3_CONTEXT_OFFSET);
	}

	@HLELogging(level="info")
	@HLEFunction(nid = 0x4AA9EAD6, version = 600, checkInsideInterrupt = true)
    public int __sceSasSetVoiceATRAC3(int sasCore, int voice, int atrac3Context) {
		// atrac3Context is the value returned by _sceAtracGetContextAddress

        checkSasAndVoiceHandlesGood(sasCore, voice);

        AtracID atracId = Modules.sceAtrac3plusModule.getAtracIdFromContext(atrac3Context);
        if (atracId != null) {
        	voices[voice].setAtracId(atracId);

        	// Store the atrac3Context address into the sasCore structure.
        	setSasCoreAtrac3Context(sasCore, voice, atrac3Context);
        }

        return 0;
    }

    @HLEFunction(nid = 0x7497EA85, version = 600, checkInsideInterrupt = true)
    public int __sceSasConcatenateATRAC3(int sasCore, int voice, @CanBeNull TPointer atrac3DataAddr, int atrac3DataLength) {
        checkSasAndVoiceHandlesGood(sasCore, voice);

        int atrac3Context = getSasCoreAtrac3Context(sasCore, voice);
        AtracID atracID = Modules.sceAtrac3plusModule.getAtracIdFromContext(atrac3Context);
        atracID.addStreamData(atrac3DataAddr.getAddress(), atrac3DataLength);

        return 0;
    }

    @HLEFunction(nid = 0xF6107F00, version = 600, checkInsideInterrupt = true)
    public int __sceSasUnsetATRAC3(int sasCore, int voice) {
        checkSasAndVoiceHandlesGood(sasCore, voice);

        voices[voice].setAtracId(null);

        // Reset the atrac3Context address
        setSasCoreAtrac3Context(sasCore, voice, 0);

        return 0;
    }
}