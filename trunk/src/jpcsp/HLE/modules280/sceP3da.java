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

import jpcsp.HLE.HLEFunction;
import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

// Positional 3D Audio Library
public class sceP3da extends HLEModule {
    protected static Logger log = Modules.getLogger("sceP3da");

    @Override
	public String getName() {
		return "sceP3da";
	}
    public static final int PSP_P3DA_SAMPLES_NUM_STEP = 32;
    public static final int PSP_P3DA_SAMPLES_NUM_MIN = 64;
    public static final int PSP_P3DA_SAMPLES_NUM_DEFAULT = 256;
    public static final int PSP_P3DA_SAMPLES_NUM_MAX = 2048;

    public static final int PSP_P3DA_CHANNELS_NUM_MAX = 4;

    protected int p3daChannelsNum;
    protected int p3daSamplesNum;

	@HLEFunction(nid = 0x374500A5, version = 280)
	public void sceP3daBridgeInit(Processor processor) {
		CpuState cpu = processor.cpu;

		int channelsNum = cpu._a0; // Values: 4
		int samplesNum = cpu._a1;  // Values: 2048

		log.warn(String.format("PARTIAL: sceP3daBridgeInit channelsNum=%d, samplesNum=%d", channelsNum, samplesNum));

        p3daChannelsNum = channelsNum;
        p3daSamplesNum = samplesNum;

		cpu._v0 = 0;
	}

	@HLEFunction(nid = 0x43F756A2, version = 280)
	public void sceP3daBridgeExit(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("PARTIAL: sceP3daBridgeExit");

		cpu._v0 = 0;
	}

	@HLEFunction(nid = 0x013016F3, version = 280)
	public void sceP3daBridgeCore(Processor processor) {
		CpuState cpu = processor.cpu;

		int p3daCore = cpu._a0;    // Address to structure containing 2 32-bit values
		int channelsNum = cpu._a1; // Values: 4
		int samplesNum = cpu._a2;  // Values: 2048
		int inputAddr = cpu._a3;   // Address (always the same)
		int outputAddr = cpu._t0;  // Address (alternating between 2 values separated by 0x2000)

		log.warn(String.format("PARTIAL: sceP3daBridgeCore p3daCore=0x%08X, channelsNum=%d, samplesNum=%d, inputAddr=0x%08X, outputAddr=0x%08X", p3daCore, channelsNum, samplesNum, inputAddr, outputAddr));

        // Overwrite these values, just like in sceSasCore.
        p3daChannelsNum = channelsNum;
        p3daSamplesNum = samplesNum;

		cpu._v0 = 0;
	}

}