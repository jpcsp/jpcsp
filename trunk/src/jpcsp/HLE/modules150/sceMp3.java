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

import java.util.HashMap;

import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.Modules;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState;

public class sceMp3 implements HLEModule {
	@Override
	public String getName() { return "sceMp3"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(sceMp3ReserveMp3HandleFunction, 0x07EC321A);
            mm.addFunction(sceMp3NotifyAddStreamDataFunction, 0x0DB149F4);
            mm.addFunction(sceMp3ResetPlayPositionFunction, 0x2A368661);
            mm.addFunction(sceMp3InitResourceFunction, 0x35750070);
            mm.addFunction(sceMp3TermResourceFunction, 0x3C2FA058);
            mm.addFunction(sceMp3SetLoopNumFunction, 0x3CEF484F);
            mm.addFunction(sceMp3InitFunction, 0x44E07129);
            mm.addFunction(sceMp3GetMp3ChannelNumFunction, 0x7F696782);
            mm.addFunction(sceMp3GetSamplingRateFunction, 0x8F450998);
            mm.addFunction(sceMp3GetInfoToAddStreamDataFunction, 0xA703FE0F);
            mm.addFunction(sceMp3DecodeFunction, 0xD021C0FB);
            mm.addFunction(sceMp3CheckStreamDataNeededFunction, 0xD0A56296);
            mm.addFunction(sceMp3ReleaseMp3HandleFunction, 0xF5478233);
            mm.addFunction(sceMp3GetSumDecodedSampleFunction, 0x354D27EA);
            mm.addFunction(sceMp3GetBitRateFunction, 0x87677E40);
            mm.addFunction(sceMp3GetMaxOutputSampleFunction, 0x87C263D1);
            mm.addFunction(sceMp3GetLoopNumFunction, 0xD8F54A51);

            mp3HandleCount = 0;
            mp3LoopMap = new HashMap<Integer, Integer>();

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

            mm.removeFunction(sceMp3ReserveMp3HandleFunction);
            mm.removeFunction(sceMp3NotifyAddStreamDataFunction);
            mm.removeFunction(sceMp3ResetPlayPositionFunction);
            mm.removeFunction(sceMp3InitResourceFunction);
            mm.removeFunction(sceMp3TermResourceFunction);
            mm.removeFunction(sceMp3SetLoopNumFunction);
            mm.removeFunction(sceMp3InitFunction);
            mm.removeFunction(sceMp3GetMp3ChannelNumFunction);
            mm.removeFunction(sceMp3GetSamplingRateFunction);
            mm.removeFunction(sceMp3GetInfoToAddStreamDataFunction);
            mm.removeFunction(sceMp3DecodeFunction);
            mm.removeFunction(sceMp3CheckStreamDataNeededFunction);
            mm.removeFunction(sceMp3ReleaseMp3HandleFunction);
            mm.removeFunction(sceMp3GetSumDecodedSampleFunction);
            mm.removeFunction(sceMp3GetBitRateFunction);
            mm.removeFunction(sceMp3GetMaxOutputSampleFunction);
            mm.removeFunction(sceMp3GetLoopNumFunction);

		}
	}

    protected int mp3StreamStart;
    protected int mp3Unk1;
    protected int mp3StreamEnd;
    protected int mp3Unk2;
    protected int mp3Buf;
    protected int mp3BufSize;
    protected int mp3PcmBuf;
    protected int mp3PcmBufSize;

    protected int mp3HandleCount;
    protected HashMap<Integer, Integer> mp3LoopMap;

    protected int makeFakeMp3StreamHandle() {
        return 0xA3A30000 | (mp3HandleCount++ & 0xFFFF);
    }

    public void sceMp3ReserveMp3Handle(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mp3args = cpu.gpr[4];

        // SceMp3InitArg struct.
        mp3StreamStart = mem.read32(mp3args);
        mp3Unk1        = mem.read32(mp3args + 4);
        mp3StreamEnd   = mem.read32(mp3args + 8);
        mp3Unk2        = mem.read32(mp3args + 12);
        mp3Buf         = mem.read32(mp3args + 16);
        mp3BufSize     = mem.read32(mp3args + 20);
        mp3PcmBuf      = mem.read32(mp3args + 24);
        mp3PcmBufSize  = mem.read32(mp3args + 28);

        Modules.log.warn("PARTIAL: sceMp3ReserveMp3Handle "
    			+ String.format("mp3args=0x%08x", cpu.gpr[4]));

        cpu.gpr[2] = makeFakeMp3StreamHandle();
    }

    public void sceMp3NotifyAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];
        int size = cpu.gpr[5];

        Modules.log.warn("UNIMPLEMENTED: sceMp3NotifyAddStreamData "
    			+ String.format("mp3handle=0x%08x, size=%d", mp3handle, size));

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMp3ResetPlayPosition(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3ResetPlayPosition "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3InitResource(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("UNIMPLEMENTED: sceMp3InitResource");

        cpu.gpr[2] = 0;
    }

    public void sceMp3TermResource(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("UNIMPLEMENTED: sceMp3TermResource");

        cpu.gpr[2] = 0;
    }

    public void sceMp3SetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];
        int loopNbr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: sceMp3SetLoopNum "
    			+ String.format("mp3handle=0x%08x, loopNbr=%d", mp3handle, loopNbr));

        // Associate each handle with it's max loop number.
        mp3LoopMap.put(mp3handle, loopNbr);

        cpu.gpr[2] = 0;
    }

    public void sceMp3Init(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3Init "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetMp3ChannelNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3GetMp3ChannelNum "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetSamplingRate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3GetSamplingRate "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetInfoToAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mp3handle = cpu.gpr[4];
        int mp3BufAddr = cpu.gpr[5];
        int mp3BufToWriteAddr = cpu.gpr[5];
        int mp3PosAddr = cpu.gpr[5];

        Modules.log.warn("PARTIAL: sceMp3GetInfoToAddStreamData "
    			+ String.format("mp3handle=0x%08x, mp3BufAddr=0x%08x, mp3BufToWriteAddr=0x%08x, mp3PosAddr=0x%08x"
                , mp3handle, mp3BufAddr, mp3BufToWriteAddr, mp3PosAddr));

        mem.write32(mp3BufAddr, mp3Buf);
        mem.write32(mp3BufToWriteAddr, mp3BufSize);
        mem.write32(mp3PosAddr, mp3StreamStart);

        cpu.gpr[2] = 0;
    }

    public void sceMp3Decode(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];
        int outPcmAddr = cpu.gpr[5];

        Modules.log.warn("UNIMPLEMENTED: sceMp3Decode "
    			+ String.format("mp3handle=0x%08x, outPcmAddr=0x%08x", mp3handle, outPcmAddr));

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMp3CheckStreamDataNeeded(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("UNIMPLEMENTED: sceMp3CheckStreamDataNeeded "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMp3ReleaseMp3Handle(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3ReleaseMp3Handle "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetSumDecodedSample(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3GetSumDecodedSample "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetBitRate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3GetBitRate "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetMaxOutputSample(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("IGNORING: sceMp3GetMaxOutputSample "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        Modules.log.warn("PARTIAL: sceMp3GetLoopNum "
    			+ String.format("mp3handle=0x%08x", mp3handle));

        int loopNum = 0;
        if(mp3LoopMap.containsKey(mp3handle)) {
            loopNum = mp3LoopMap.get(mp3handle);
        }

        cpu.gpr[2] = loopNum;
    }


    public final HLEModuleFunction sceMp3ReserveMp3HandleFunction = new HLEModuleFunction("sceMp3", "sceMp3ReserveMp3Handle") {
		@Override
		public final void execute(Processor processor) {
			sceMp3ReserveMp3Handle(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3ReserveMp3Handle(processor);";
		}
	};

    public final HLEModuleFunction sceMp3NotifyAddStreamDataFunction = new HLEModuleFunction("sceMp3", "sceMp3NotifyAddStreamData") {
		@Override
		public final void execute(Processor processor) {
			sceMp3NotifyAddStreamData(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3NotifyAddStreamData(processor);";
		}
	};

    public final HLEModuleFunction sceMp3ResetPlayPositionFunction = new HLEModuleFunction("sceMp3", "sceMp3ResetPlayPosition") {
		@Override
		public final void execute(Processor processor) {
			sceMp3ResetPlayPosition(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3ResetPlayPosition(processor);";
		}
	};

    public final HLEModuleFunction sceMp3InitResourceFunction = new HLEModuleFunction("sceMp3", "sceMp3InitResource") {
		@Override
		public final void execute(Processor processor) {
			sceMp3InitResource(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3InitResource(processor);";
		}
	};

    public final HLEModuleFunction sceMp3TermResourceFunction = new HLEModuleFunction("sceMp3", "sceMp3TermResource") {
		@Override
		public final void execute(Processor processor) {
			sceMp3TermResource(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3TermResource(processor);";
		}
	};

    public final HLEModuleFunction sceMp3SetLoopNumFunction = new HLEModuleFunction("sceMp3", "sceMp3SetLoopNum") {
		@Override
		public final void execute(Processor processor) {
			sceMp3SetLoopNum(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3SetLoopNum(processor);";
		}
	};

    public final HLEModuleFunction sceMp3InitFunction = new HLEModuleFunction("sceMp3", "sceMp3Init") {
		@Override
		public final void execute(Processor processor) {
			sceMp3Init(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3Init(processor);";
		}
	};

    public final HLEModuleFunction sceMp3GetMp3ChannelNumFunction = new HLEModuleFunction("sceMp3", "sceMp3GetMp3ChannelNum") {
		@Override
		public final void execute(Processor processor) {
			sceMp3GetMp3ChannelNum(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetMp3ChannelNum(processor);";
		}
	};

    public final HLEModuleFunction sceMp3GetSamplingRateFunction = new HLEModuleFunction("sceMp3", "sceMp3GetSamplingRate") {
		@Override
		public final void execute(Processor processor) {
			sceMp3GetSamplingRate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetSamplingRate(processor);";
		}
	};

    public final HLEModuleFunction sceMp3GetInfoToAddStreamDataFunction = new HLEModuleFunction("sceMp3", "sceMp3GetInfoToAddStreamData") {
		@Override
		public final void execute(Processor processor) {
			sceMp3GetInfoToAddStreamData(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetInfoToAddStreamData(processor);";
		}
	};

    public final HLEModuleFunction sceMp3DecodeFunction = new HLEModuleFunction("sceMp3", "sceMp3Decode") {
		@Override
		public final void execute(Processor processor) {
			sceMp3Decode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3Decode(processor);";
		}
	};

    public final HLEModuleFunction sceMp3CheckStreamDataNeededFunction = new HLEModuleFunction("sceMp3", "sceMp3CheckStreamDataNeeded") {
		@Override
		public final void execute(Processor processor) {
			sceMp3CheckStreamDataNeeded(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3CheckStreamDataNeeded(processor);";
		}
	};

    public final HLEModuleFunction sceMp3ReleaseMp3HandleFunction = new HLEModuleFunction("sceMp3", "sceMp3ReleaseMp3Handle") {
		@Override
		public final void execute(Processor processor) {
			sceMp3ReleaseMp3Handle(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3ReleaseMp3Handle(processor);";
		}
	};

    public final HLEModuleFunction sceMp3GetSumDecodedSampleFunction = new HLEModuleFunction("sceMp3", "sceMp3GetSumDecodedSample") {
		@Override
		public final void execute(Processor processor) {
			sceMp3GetSumDecodedSample(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetSumDecodedSample(processor);";
		}
	};

    public final HLEModuleFunction sceMp3GetBitRateFunction = new HLEModuleFunction("sceMp3", "sceMp3GetBitRate") {
		@Override
		public final void execute(Processor processor) {
			sceMp3GetBitRate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetBitRate(processor);";
		}
	};

    public final HLEModuleFunction sceMp3GetMaxOutputSampleFunction = new HLEModuleFunction("sceMp3", "sceMp3GetMaxOutputSample") {
		@Override
		public final void execute(Processor processor) {
			sceMp3GetMaxOutputSample(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetMaxOutputSample(processor);";
		}
	};

    public final HLEModuleFunction sceMp3GetLoopNumFunction = new HLEModuleFunction("sceMp3", "sceMp3GetLoopNum") {
		@Override
		public final void execute(Processor processor) {
			sceMp3GetLoopNum(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceMp3Module.sceMp3GetLoopNum(processor);";
		}
	};
}