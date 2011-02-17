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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceSasCore extends jpcsp.HLE.modules150.sceSasCore {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 500) {
            mm.addFunction(0xE1CD9561, __sceSasSetVoicePCMFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.uninstallModule(mm, version);

        if (version >= 500) {
            mm.removeFunction(__sceSasSetVoicePCMFunction);
        }
    }

    /** Identical to __sceSasSetVoice, but for raw PCM data (VAG/ADPCM is not allowed). */
    public void __sceSasSetVoicePCM(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int pcmAddr = cpu.gpr[6];
        int size = cpu.gpr[7];
        int loopsize = cpu.gpr[8];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetVoicePCM: " + String.format("sasCore=0x%08x, voice=%d, pcmAddr=0x%08x, size=0x%08x, loopsize=0x%08x",
                    sasCore, voice, pcmAddr, size, loopsize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        if (size <= 0 || (size & 0xF) != 0) {
        	log.warn(String.format("__sceSasSetVoicePCM invalid size 0x%08X", size));
        	cpu.gpr[2] = SceKernelErrors.ERROR_SAS_INVALID_PARAMETER;
        } else if (isSasHandleGood(sasCore, "__sceSasSetVoicePCM", cpu) && isVoiceNumberGood(voice, "__sceSasSetVoicePCM", cpu)) {
            voices[voice].setVAG(pcmAddr, size);
            voices[voice].setLoopMode(loopsize);
            cpu.gpr[2] = 0;
        }
    }

    public final HLEModuleFunction __sceSasSetVoicePCMFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetVoicePCM") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetVoicePCM(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetVoicePCM(processor);";
        }
    };
}