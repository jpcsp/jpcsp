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
package jpcsp.HLE.modules280;

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

// Positional 3D Audio Library
public class sceP3da implements HLEModule {
    protected static Logger log = Modules.getLogger("sceP3da");

    @Override
	public String getName() {
		return "sceP3da";
	}

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 280) {
			mm.addFunction(0x374500A5, sceP3daBridgeInitFunction);
			mm.addFunction(0x43F756A2, sceP3daBridgeExitFunction);
			mm.addFunction(0x013016F3, sceP3daBridgeCoreFunction);
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 280) {
			mm.removeFunction(sceP3daBridgeInitFunction);
			mm.removeFunction(sceP3daBridgeExitFunction);
			mm.removeFunction(sceP3daBridgeCoreFunction);
		}
	}
    public static final int PSP_P3DA_SAMPLES_NUM_STEP = 32;
    public static final int PSP_P3DA_SAMPLES_NUM_MIN = 64;
    public static final int PSP_P3DA_SAMPLES_NUM_DEFAULT = 256;
    public static final int PSP_P3DA_SAMPLES_NUM_MAX = 2048;

    public static final int PSP_P3DA_CHANNELS_NUM_MAX = 4;

    private int p3daChannelsNum;
    private int p3daSamplesNum;

	public void sceP3daBridgeInit(Processor processor) {
		CpuState cpu = processor.cpu;

		int channelsNum = cpu.gpr[4]; // Values: 4
		int samplesNum = cpu.gpr[5];  // Values: 2048

		log.warn(String.format("PARTIAL: sceP3daBridgeInit channelsNum=%d, samplesNum=%d", channelsNum, samplesNum));

        p3daChannelsNum = channelsNum;
        p3daSamplesNum = samplesNum;

		cpu.gpr[2] = 0;
	}

	public void sceP3daBridgeExit(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("PARTIAL: sceP3daBridgeExit");

		cpu.gpr[2] = 0;
	}

	public void sceP3daBridgeCore(Processor processor) {
		CpuState cpu = processor.cpu;

		int p3daCore = cpu.gpr[4];    // Address to structure containing 2 32-bit values
		int channelsNum = cpu.gpr[5]; // Values: 4
		int samplesNum = cpu.gpr[6];  // Values: 2048
		int inputAddr = cpu.gpr[7];   // Address (always the same)
		int outputAddr = cpu.gpr[8];  // Address (alternating between 2 values separated by 0x2000)

		log.warn(String.format("PARTIAL: sceP3daBridgeCore p3daCore=0x%08X, channelsNum=%d, samplesNum=%d, inputAddr=0x%08X, outputAddr=0x%08X", p3daCore, channelsNum, samplesNum, inputAddr, outputAddr));

        // Overwrite these values, just like in sceSasCore.
        p3daChannelsNum = channelsNum;
        p3daSamplesNum = samplesNum;

		cpu.gpr[2] = 0;
	}

	public final HLEModuleFunction sceP3daBridgeInitFunction = new HLEModuleFunction("sceP3da", "sceP3daBridgeInit") {
		@Override
		public final void execute(Processor processor) {
			sceP3daBridgeInit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceP3da.sceP3daBridgeInit(processor);";
		}
	};

	public final HLEModuleFunction sceP3daBridgeExitFunction = new HLEModuleFunction("sceP3da", "sceP3daBridgeExit") {
		@Override
		public final void execute(Processor processor) {
			sceP3daBridgeExit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceP3da.sceP3daBridgeExit(processor);";
		}
	};

	public final HLEModuleFunction sceP3daBridgeCoreFunction = new HLEModuleFunction("sceP3da", "sceP3daBridgeCore") {
		@Override
		public final void execute(Processor processor) {
			sceP3daBridgeCore(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceP3da.sceP3daBridgeCore(processor);";
		}
	};
}