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

package jpcsp.HLE.modules150;

import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.Modules;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState;

public class scePsmf implements HLEModule {
	@Override
	public String getName() { return "scePsmf"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(scePsmfVerifyPsmfFunction, 0x2673646B);
            mm.addFunction(scePsmfSetPsmfFunction, 0xC22C8327);
            mm.addFunction(scePsmfGetNumberOfSpecificStreamsFunction, 0x68D42328);
            mm.addFunction(scePsmfQueryStreamOffsetFunction, 0x5B70FCC1);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(scePsmfVerifyPsmfFunction);
            mm.removeFunction(scePsmfSetPsmfFunction);
            mm.removeFunction(scePsmfGetNumberOfSpecificStreamsFunction);
            mm.removeFunction(scePsmfQueryStreamOffsetFunction);

		}
	}

    private int psmfOffset;
    private int psmfVersion;

    protected int endianSwap(int x) {
        return (x << 24) | ((x << 8) &  0xFF0000) | ((x >> 8) &  0xFF00) | ((x >> 24) &  0xFF);
    }

    /*
     * Function names extracted from "Dungeon Explorer: Warriors of Ancient Arts" (ULUS10289).
     */

	public void scePsmfVerifyPsmf(Processor processor) {
	    CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];

		Modules.log.warn("PARTIAL: scePsmfVerifyPsmf (psmf=0x" + Integer.toHexString(psmf) + ")");

        // Reads the version and seems to expect a 0014 version only.
        // TODO: Return error code if the PSMF isn't valid.
        psmfVersion = mem.read32(psmf + 4);

        cpu.gpr[2] = 0;
	}

    public void scePsmfSetPsmf(Processor processor) {
	    CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];  // PSMF struct.
        int buffer_addr = cpu.gpr[5];  // Actual PMF data.

		Modules.log.warn("PARTIAL: scePsmfSetPsmf (psmf=0x" + Integer.toHexString(psmf)
                + " buffer_addr=0x" + Integer.toHexString(buffer_addr) + ")");


        // Reads several parameters from the PMF file header and stores them in
        // a 36 byte struct.
        mem.write32(psmf, 0);
        mem.write32(psmf + 4, 0);
        mem.write32(psmf + 8, 0);
        mem.write32(psmf + 12, 0);
        mem.write32(psmf + 16, 0);
        mem.write32(psmf + 20, 0);
        mem.write32(psmf + 24, 0);
        mem.write32(psmf + 28, 0);
        mem.write32(psmf + 32, 0);
        mem.write32(psmf + 36, 0);

		cpu.gpr[2] = 0;
	}

    public void scePsmfGetNumberOfSpecificStreams(Processor processor) {
	    CpuState cpu = processor.cpu;

        int psmf = cpu.gpr[4];
        int stream = cpu.gpr[5];  // Stream type (same as sceMpeg).

		Modules.log.warn("PARTIAL: scePsmfGetNumberOfSpecificStreams (psmf=0x" + Integer.toHexString(psmf)
                + " stream=" + stream + ")");

        // Return at least one stream for each type (0 = video, 1 = audio).
		cpu.gpr[2] = 1;
	}

    public void scePsmfQueryStreamOffset(Processor processor) {
	    CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int buffer_addr = cpu.gpr[4];
        int offset_addr = cpu.gpr[5];

		Modules.log.warn("PARTIAL: scePsmfQueryStreamOffset (buffer_addr=0x" + Integer.toHexString(buffer_addr)
                + " offset_addr=0x" + Integer.toHexString(offset_addr) + ")");

        // Same as sceMpeg. Read the offset and write it at the output address.
        psmfOffset = endianSwap(mem.read32(buffer_addr + 8));
        mem.write32(offset_addr, psmfOffset);

		cpu.gpr[2] = 0;
	}

	public final HLEModuleFunction scePsmfVerifyPsmfFunction = new HLEModuleFunction("scePsmf", "scePsmfVerifyPsmf") {
		@Override
		public final void execute(Processor processor) {
			scePsmfVerifyPsmf(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfVerifyPsmf(processor);";
		}
	};

    public final HLEModuleFunction scePsmfSetPsmfFunction = new HLEModuleFunction("scePsmf", "scePsmfSetPsmf") {
		@Override
		public final void execute(Processor processor) {
			scePsmfSetPsmf(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSetPsmf(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetNumberOfSpecificStreamsFunction = new HLEModuleFunction("scePsmf", "scePsmfGetNumberOfSpecificStreams") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetNumberOfSpecificStreams(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetNumberOfSpecificStreams(processor);";
		}
	};

    public final HLEModuleFunction scePsmfQueryStreamOffsetFunction = new HLEModuleFunction("scePsmf", "scePsmfQueryStreamOffset") {
		@Override
		public final void execute(Processor processor) {
			scePsmfQueryStreamOffset(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfQueryStreamOffset(processor);";
		}
	};
};