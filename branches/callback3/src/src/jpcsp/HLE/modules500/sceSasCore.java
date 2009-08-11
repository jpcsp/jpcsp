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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState; // New-Style Processor

public class sceSasCore extends jpcsp.HLE.modules150.sceSasCore {

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 500) {
            mm.addFunction(__sceSasSetVoicePCMFunction, 0xE1CD9561);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        super.installModule(mm, version);

        if (version >= 500) {
            mm.removeFunction(__sceSasSetVoicePCMFunction);
        }
    }

    /** based on __sceSasSetVoice, but it may have different parameters/behaviour (unchecked) */
    public void __sceSasSetVoicePCM(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int pcmAddr = cpu.gpr[6]; // may have uncached bit set
        int size = cpu.gpr[7];
        int loopmode = cpu.gpr[8];

        Modules.log.warn("UNIMPLEMENTED __sceSasSetVoicePCM "
            + String.format("sasCore=0x%08x voice=%d pcmAddr=0x%08x size=0x%08x loopmode=%d",
            sasCore, voice, pcmAddr, size, loopmode));

        if (isSasHandleGood(sasCore, "__sceSasSetVoice", cpu) && isVoiceNumberGood(voice, "__sceSasSetVoice", cpu)) {
            // TODO
            //voices[voice].samples = decodeSamples(processor, vagAddr, size);
            voices[voice].loopMode = loopmode;

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
