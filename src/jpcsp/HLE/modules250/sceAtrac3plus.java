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

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceAtrac3plus extends jpcsp.HLE.modules150.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
    	super.installModule(mm, version);

    	if (version >= 250) {
            mm.addFunction(0xB3B5D042, sceAtracGetOutputChannelFunction);
            mm.addFunction(0xECA32A99, sceAtracIsSecondBufferNeededFunction);
            mm.addFunction(0x132F1ECA, sceAtracReinitFunction);
            mm.addFunction(0x2DD3E298, sceAtracGetBufferInfoForResettingFunction);
            mm.addFunction(0x5CF9D852, sceAtracSetMOutHalfwayBufferFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
    	super.uninstallModule(mm, version);

    	if (version >= 250) {
            mm.removeFunction(sceAtracGetOutputChannelFunction);
            mm.removeFunction(sceAtracIsSecondBufferNeededFunction);
            mm.removeFunction(sceAtracReinitFunction);
            mm.removeFunction(sceAtracGetBufferInfoForResettingFunction);
            mm.removeFunction(sceAtracSetMOutHalfwayBufferFunction);
        }
    }

    public void sceAtracGetOutputChannel(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int outputChannelAddr = cpu.gpr[5];

        log.warn(String.format("Unimplemented sceAtracGetOutputChannel: atracID = %d, outputChannelAddr = 0x%08X", atID, outputChannelAddr));

        if (mem.isAddressGood(outputChannelAddr)) {
        	mem.write32(outputChannelAddr, 1);
        }

        cpu.gpr[2] = 0;
    }

    public void sceAtracIsSecondBufferNeeded(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];

        log.warn(String.format("PARTIAL: sceAtracIsSecondBufferNeeded atracId=%d", atID));

        // -1 -> Error.
        // 0 - > Second buffer isn't needed.
        // 1 -> Second buffer is needed.
        cpu.gpr[2] = isSecondBufferNeeded() ? 1 : 0;
    }

    public void sceAtracReinit(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int unk = cpu.gpr[5];

        log.warn(String.format("IGNORING: sceAtracReinit atracId=%d unk=%d", atID, unk));

        cpu.gpr[2] = 0;
    }

    public void sceAtracGetBufferInfoForResetting(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int sample = cpu.gpr[5];
        int bufferInfoAddr = cpu.gpr[6];

        log.warn(String.format("PARTIAL: sceAtracGetBufferInfoForResetting atracID=%d, sample=%d, unk1Addr=0x%08x", atID, sample, bufferInfoAddr));
        hleAtracGetBufferInfoForReseting(atID, sample, bufferInfoAddr);

        cpu.gpr[2] = 0;
    }

    public void sceAtracSetMOutHalfwayBuffer(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented function sceAtracSetMOutHalfwayBuffer "
    			+ String.format("%08x %08x %08x %08x %08x %08x",
    					cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]));

        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction sceAtracGetOutputChannelFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetOutputChannel") {
        @Override
        public final void execute(Processor processor) {
        	sceAtracGetOutputChannel(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetOutputChannel(processor);";
        }
    };

    public final HLEModuleFunction sceAtracIsSecondBufferNeededFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracIsSecondBufferNeeded") {
        @Override
        public final void execute(Processor processor) {
        	sceAtracIsSecondBufferNeeded(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracIsSecondBufferNeeded(processor);";
        }
    };

    public final HLEModuleFunction sceAtracReinitFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracReinit") {
        @Override
        public final void execute(Processor processor) {
        	sceAtracReinit(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracReinit(processor);";
        }
    };

    public final HLEModuleFunction sceAtracGetBufferInfoForResettingFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracGetBufferInfoForResetting") {
        @Override
        public final void execute(Processor processor) {
        	sceAtracGetBufferInfoForResetting(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracGetBufferInfoForResetting(processor);";
        }
    };

    public final HLEModuleFunction sceAtracSetMOutHalfwayBufferFunction = new HLEModuleFunction("sceAtrac3plus", "sceAtracSetMOutHalfwayBuffer") {
        @Override
        public final void execute(Processor processor) {
        	sceAtracSetMOutHalfwayBuffer(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAtrac3plusModule.sceAtracSetMOutHalfwayBuffer(processor);";
        }
    };
}