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
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

// Positional 3D Audio Library
@HLELogging
public class sceP3da extends HLEModule {
    public static Logger log = Modules.getLogger("sceP3da");

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

    @HLEUnimplemented
	@HLEFunction(nid = 0x374500A5, version = 280)
	public int sceP3daBridgeInit(int channelsNum, int samplesNum) {
        p3daChannelsNum = channelsNum;
        p3daSamplesNum = samplesNum;

		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x43F756A2, version = 280)
	public int sceP3daBridgeExit() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x013016F3, version = 280)
	public int sceP3daBridgeCore(TPointer32 p3daCoreAddr, int channelsNum, int samplesNum, TPointer32 inputAddr, TPointer outputAddr) {
		int[] p3daCore = new int[2];
		for (int i = 0; i < p3daCore.length; i++) {
			p3daCore[i] = p3daCoreAddr.getValue(i << 2);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceP3daBridgeCore p3daCore[%d]=0x%08X", i, p3daCore[i]));
			}
		}

		int[] channelBuffers = new int[channelsNum];
		for (int i = 0; i < channelsNum; i++) {
			channelBuffers[i] = inputAddr.getValue(i << 2);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceP3daBridgeCore channelBuffer[%d]=0x%08X", i, channelBuffers[i]));

				// Dump the memory if the channel buffer does not contain only 0's.
				IMemoryReader memoryReader = MemoryReader.getMemoryReader(channelBuffers[i], samplesNum << 1, 2);
				for (int j = 0; j < samplesNum; j++) {
					if (memoryReader.readNext() != 0) {
						log.debug(String.format("sceP3daBridgeCore non-empty channelBuffer[%d]: %s", i, Utilities.getMemoryDump(channelBuffers[i], samplesNum << 1)));
						log.debug(String.format("sceP3daBridgeCore outputAddr: %s", Utilities.getMemoryDump(outputAddr.getAddress(), samplesNum << 2)));
						break;
					}
				}
			}
		}

        // Overwrite these values, just like in sceSasCore.
        p3daChannelsNum = channelsNum;
        p3daSamplesNum = samplesNum;

        Modules.ThreadManForUserModule.hleKernelDelayThread(600, false);

        return 0;
	}
}