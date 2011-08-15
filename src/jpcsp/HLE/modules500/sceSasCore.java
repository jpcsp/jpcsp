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
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceSasCore extends jpcsp.HLE.modules150.sceSasCore {

    /** Identical to __sceSasSetVoice, but for raw PCM data (VAG/ADPCM is not allowed). */
    @HLEFunction(nid = 0xE1CD9561, version = 500, checkInsideInterrupt = true)
    public int __sceSasSetVoicePCM(int sasCore, int voice, int pcmAddr, int size, int loopsize) {
        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetVoicePCM: " + String.format("sasCore=0x%08x, voice=%d, pcmAddr=0x%08x, size=0x%08x, loopsize=0x%08x",
                    sasCore, voice, pcmAddr, size, loopsize));
        }

        if (size <= 0 || (size & 0xF) != 0) {
        	log.warn(String.format("__sceSasSetVoicePCM invalid size 0x%08X", size));
        	throw(new SceKernelErrorException(SceKernelErrors.ERROR_SAS_INVALID_PARAMETER));
        }
        
        checkSasAndVoiceHandlesGood(sasCore, voice);
        
        voices[voice].setVAG(pcmAddr, size);
        voices[voice].setLoopMode(loopsize);
        
        return 0;
    }

}