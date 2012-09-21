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
import jpcsp.HLE.HLEFunction;

public class sceSasCore extends jpcsp.HLE.modules500.sceSasCore {
	protected void setSasCoreAtrac3Context(int sasCore, int voice, int atrac3Context) {
		Memory mem = Memory.getInstance();
        mem.write32(sasCore + 56 * voice + 20, atrac3Context);
	}

	@HLEFunction(nid = 0x4AA9EAD6, version = 600, checkInsideInterrupt = true)
    public int __sceSasSetVoiceATRAC3(int sasCore, int voice, int atrac3Context) {
		// atrac3Context is the value returned by _sceAtracGetContextAddress
        log.warn(String.format("Unimplemented __sceSasSetVoiceATRAC3 sasCore=0x%08X, voice=%d, atrac3Context=0x%08X", sasCore, voice, atrac3Context));

        checkSasAndVoiceHandlesGood(sasCore, voice);

        // Store the atrac3Context address into the sasCore structure.
        setSasCoreAtrac3Context(sasCore, voice, atrac3Context);

        return 0;
    }

    @HLEFunction(nid = 0x7497EA85, version = 600, checkInsideInterrupt = true)
    public int __sceSasConcatenateATRAC3(int sasCore, int voice, int atrac3Addr, int unkown) {
        log.warn(String.format("Unimplemented __sceSasConcatenateATRAC3 sasCore=0x%08X, voice=%d, atrac3Addr=0x%08X, unknown=0x%08X", sasCore, voice, atrac3Addr, unkown));

        checkSasAndVoiceHandlesGood(sasCore, voice);

        return 0;
    }

    @HLEFunction(nid = 0xF6107F00, version = 600, checkInsideInterrupt = true)
    public int __sceSasUnsetATRAC3(int sasCore, int voice) {
        log.warn(String.format("Unimplemented __sceSasUnsetATRAC3 sasCore=0x%08X, voice=%d", sasCore, voice));

        checkSasAndVoiceHandlesGood(sasCore, voice);

        // Reset the atrac3Context address
        setSasCoreAtrac3Context(sasCore, voice, 0);

        return 0;
    }
}