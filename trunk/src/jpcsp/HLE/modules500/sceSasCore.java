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
package jpcsp.HLE.modules500;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelErrors;

@HLELogging
public class sceSasCore extends jpcsp.HLE.modules150.sceSasCore {
    /** Identical to __sceSasSetVoice, but for raw PCM data (VAG/ADPCM is not allowed). */
    @HLEFunction(nid = 0xE1CD9561, version = 500, checkInsideInterrupt = true)
    public int __sceSasSetVoicePCM(int sasCore, int voice, TPointer pcmAddr, int size, int loopmode) {
        if (size <= 0 || size > 0x10000) {
        	log.warn(String.format("__sceSasSetVoicePCM invalid size 0x%08X", size));

        	return SceKernelErrors.ERROR_SAS_INVALID_SIZE;
        }

        checkSasAndVoiceHandlesGood(sasCore, voice);

        voices[voice].setPCM(pcmAddr.getAddress(), size);
        voices[voice].setLoopMode(loopmode);

        return 0;
    }
}