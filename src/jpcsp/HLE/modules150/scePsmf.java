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

			mm.addFunction(scePsmfSetPsmfFunction, 0xC22C8327);
            mm.addFunction(scePsmfGetCurrentStreamTypeFunction, 0xC7DB3A5B);
            mm.addFunction(scePsmfGetCurrentStreamNumberFunction, 0x28240568);
            mm.addFunction(scePsmfSpecifyStreamWithStreamTypeFunction, 0x1E6D9013);
            mm.addFunction(scePsmfSpecifyStreamFunction, 0x4BC9BDE0);
            mm.addFunction(scePsmfGetPresentationStartTimeFunction, 0x76D3AEBA);
            mm.addFunction(scePsmfGetPresentationEndTimeFunction, 0xBD8AE0D8);
            mm.addFunction(scePsmfGetNumberOfStreamsFunction, 0xEAED89CD);
            mm.addFunction(scePsmfGetNumberOfEPentriesFunction, 0x7491C438);
            mm.addFunction(scePsmfGetVideoInfoFunction, 0x0BA514E5);
            mm.addFunction(scePsmfGetAudioInfoFunction, 0xA83F7113);
            mm.addFunction(scePsmfCheckEPmapFunction, 0x971A3A90);
            mm.addFunction(scePsmfGetEPWithIdFunction, 0x4E624A34);
            mm.addFunction(scePsmfGetEPWithTimestampFunction, 0x7C0E7AC3);
            mm.addFunction(scePsmfGetEPidWithTimestampFunction, 0x5F457515);
            mm.addFunction(scePsmfQueryStreamOffsetFunction, 0x5B70FCC1);
            mm.addFunction(scePsmfQueryStreamSizeFunction, 0x9553CC91);
            mm.addFunction(scePsmfGetNumberOfSpecificStreamsFunction, 0x68D42328);
            mm.addFunction(scePsmfSpecifyStreamWithStreamTypeNumberFunction, 0x0C120E1D);
            mm.addFunction(scePsmfVerifyPsmfFunction, 0x2673646B);
            mm.addFunction(scePsmfGetHeaderSizeFunction, 0xB78EB9E9);
            mm.addFunction(scePsmfGetStreamSizeFunction, 0xA5EBFE81);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

            mm.removeFunction(scePsmfSetPsmfFunction);
            mm.removeFunction(scePsmfGetCurrentStreamTypeFunction);
            mm.removeFunction(scePsmfGetCurrentStreamNumberFunction);
            mm.removeFunction(scePsmfSpecifyStreamWithStreamTypeFunction);
            mm.removeFunction(scePsmfSpecifyStreamFunction);
            mm.removeFunction(scePsmfGetPresentationStartTimeFunction);
            mm.removeFunction(scePsmfGetPresentationEndTimeFunction);
            mm.removeFunction(scePsmfGetNumberOfStreamsFunction);
            mm.removeFunction(scePsmfGetNumberOfEPentriesFunction);
            mm.removeFunction(scePsmfGetVideoInfoFunction);
            mm.removeFunction(scePsmfGetAudioInfoFunction);
            mm.removeFunction(scePsmfCheckEPmapFunction);
            mm.removeFunction(scePsmfGetEPWithIdFunction);
            mm.removeFunction(scePsmfGetEPWithTimestampFunction);
            mm.removeFunction(scePsmfGetEPidWithTimestampFunction);
            mm.removeFunction(scePsmfQueryStreamOffsetFunction);
            mm.removeFunction(scePsmfQueryStreamSizeFunction);
            mm.removeFunction(scePsmfGetNumberOfSpecificStreamsFunction);
            mm.removeFunction(scePsmfSpecifyStreamWithStreamTypeNumberFunction);
            mm.removeFunction(scePsmfVerifyPsmfFunction);
            mm.removeFunction(scePsmfGetHeaderSizeFunction);
            mm.removeFunction(scePsmfGetStreamSizeFunction);

		}
	}

    private int psmfOffset;
    private int psmfVersion;

    protected int endianSwap(int x) {
        return (x << 24) | ((x << 8) &  0xFF0000) | ((x >> 8) &  0xFF00) | ((x >> 24) &  0xFF);
    }

    public void scePsmfSetPsmf(Processor processor) {
	    CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];  // PSMF struct.
        int buffer_addr = cpu.gpr[5];  // Actual PMF data.

		Modules.log.warn("PARTIAL: scePsmfSetPsmf (psmf=0x" + Integer.toHexString(psmf)
                + " buffer_addr=0x" + Integer.toHexString(buffer_addr) + ")");


        int PSMFVersion = mem.read32(buffer_addr + 4);

        // Reads several parameters from the PMF file header and stores them in
        // a 36 byte struct.
        mem.write32(psmf, PSMFVersion);        // PMSF type (holds the version).
        mem.write32(psmf + 4, 0);              // Unknown.
        mem.write32(psmf + 8, 0);              // Unknown.
        mem.write32(psmf + 12, 0);             // Unknown.
        mem.write32(psmf + 16, 2);             // Number of PSMF streams (set to 2).
        mem.write32(psmf + 20, 0);             // Unknown.
        mem.write32(psmf + 24, 0);             // Unknown.
        mem.write32(psmf + 28, 0);             // Unknown.
        mem.write32(psmf + 32, 0);             // Unknown.
        mem.write32(psmf + 36, 0);             // Unknown.

		cpu.gpr[2] = 0;
	}

    public void scePsmfGetCurrentStreamType(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetCurrentStreamType [0xC7DB3A5B]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetCurrentStreamNumber(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetCurrentStreamNumber [0x28240568]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfSpecifyStreamWithStreamType(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfSpecifyStreamWithStreamType [0x1E6D9013]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfSpecifyStream(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfSpecifyStream [0x4BC9BDE0]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetPresentationStartTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetPresentationStartTime [0x76D3AEBA]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetPresentationEndTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetPresentationEndTime [0xBD8AE0D8]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetNumberOfStreams(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int psmf = cpu.gpr[4];

        Modules.log.warn("PARTIAL: scePsmfGetNumberOfStreams psmf=0x" + Integer.toHexString(psmf));

        int streams = mem.read32(psmf + 12);  // This should be set by scePsmfSetPsmf().

        cpu.gpr[2] = streams;
    }

    public void scePsmfGetNumberOfEPentries(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetNumberOfEPentries [0x7491C438]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetVideoInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetVideoInfo [0x0BA514E5]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetAudioInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetAudioInfo [0xA83F7113]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfCheckEPmap(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfCheckEPmap [0x971A3A90]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetEPWithId(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetEPWithId [0x4E624A34]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetEPWithTimestamp(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetEPWithTimestamp [0x7C0E7AC3]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetEPidWithTimestamp(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetEPidWithTimestamp [0x5F457515]");

        cpu.gpr[2] = 0xDEADC0DE;
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

    public void scePsmfQueryStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfQueryStreamSize [0x9553CC91]");

        cpu.gpr[2] = 0xDEADC0DE;
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

    public void scePsmfSpecifyStreamWithStreamTypeNumber(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfSpecifyStreamWithStreamTypeNumber [0x0C120E1D]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

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

    public void scePsmfGetHeaderSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetHeaderSize [0xB78EB9E9]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void scePsmfGetStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        Modules.log.warn("Unimplemented NID function scePsmfGetStreamSize [0xA5EBFE81]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

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

	public final HLEModuleFunction scePsmfGetCurrentStreamTypeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetCurrentStreamType") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetCurrentStreamType(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetCurrentStreamType(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetCurrentStreamNumberFunction = new HLEModuleFunction("scePsmf", "scePsmfGetCurrentStreamNumber") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetCurrentStreamNumber(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetCurrentStreamNumber(processor);";
		}
	};

    public final HLEModuleFunction scePsmfSpecifyStreamWithStreamTypeFunction = new HLEModuleFunction("scePsmf", "scePsmfSpecifyStreamWithStreamType") {
		@Override
		public final void execute(Processor processor) {
			scePsmfSpecifyStreamWithStreamType(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamType(processor);";
		}
	};

    public final HLEModuleFunction scePsmfSpecifyStreamFunction = new HLEModuleFunction("scePsmf", "scePsmfSpecifyStream") {
		@Override
		public final void execute(Processor processor) {
			scePsmfSpecifyStream(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSpecifyStream(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetPresentationStartTimeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetPresentationStartTime") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetPresentationStartTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetPresentationStartTime(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetPresentationEndTimeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetPresentationEndTime") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetPresentationEndTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetPresentationEndTime(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetNumberOfStreamsFunction = new HLEModuleFunction("scePsmf", "scePsmfGetNumberOfStreams") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetNumberOfStreams(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetNumberOfStreams(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetNumberOfEPentriesFunction = new HLEModuleFunction("scePsmf", "scePsmfGetNumberOfEPentries") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetNumberOfEPentries(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetNumberOfEPentries(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetVideoInfoFunction = new HLEModuleFunction("scePsmf", "scePsmfGetVideoInfo") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetVideoInfo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetVideoInfo(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetAudioInfoFunction = new HLEModuleFunction("scePsmf", "scePsmfGetAudioInfo") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetAudioInfo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetAudioInfo(processor);";
		}
	};

    public final HLEModuleFunction scePsmfCheckEPmapFunction = new HLEModuleFunction("scePsmf", "scePsmfGetCurrenscePsmfCheckEPmaptStreamType") {
		@Override
		public final void execute(Processor processor) {
			scePsmfCheckEPmap(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfCheckEPmap(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetEPWithIdFunction = new HLEModuleFunction("scePsmf", "scePsmfGetEPWithId") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetEPWithId(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetEPWithId(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetEPWithTimestampFunction = new HLEModuleFunction("scePsmf", "scePsmfGetEPWithTimestamp") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetEPWithTimestamp(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetEPWithTimestamp(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetEPidWithTimestampFunction = new HLEModuleFunction("scePsmf", "scePsmfGetEPidWithTimestamp") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetEPidWithTimestamp(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetEPidWithTimestamp(processor);";
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

    public final HLEModuleFunction scePsmfQueryStreamSizeFunction = new HLEModuleFunction("scePsmf", "scePsmfQueryStreamSize") {
		@Override
		public final void execute(Processor processor) {
			scePsmfQueryStreamSize(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfQueryStreamSize(processor);";
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

    public final HLEModuleFunction scePsmfSpecifyStreamWithStreamTypeNumberFunction = new HLEModuleFunction("scePsmf", "scePsmfSpecifyStreamWithStreamTypeNumber") {
		@Override
		public final void execute(Processor processor) {
			scePsmfSpecifyStreamWithStreamTypeNumber(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfSpecifyStreamWithStreamTypeNumber(processor);";
		}
	};

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

    public final HLEModuleFunction scePsmfGetHeaderSizeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetHeaderSize") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetHeaderSize(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetHeaderSize(processor);";
		}
	};

    public final HLEModuleFunction scePsmfGetStreamSizeFunction = new HLEModuleFunction("scePsmf", "scePsmfGetStreamSize") {
		@Override
		public final void execute(Processor processor) {
			scePsmfGetStreamSize(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePsmfModule.scePsmfGetStreamSize(processor);";
		}
	};
}