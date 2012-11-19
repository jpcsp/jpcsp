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

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceAudioRouting extends HLEModule {
	public static Logger log = Modules.getLogger("sceAudioRouting");
	protected static final int AUDIO_ROUTING_SPEAKER_OFF = 0;
	protected static final int AUDIO_ROUTING_SPEAKER_ON = 1;
	protected int audioRoutingMode = AUDIO_ROUTING_SPEAKER_ON;
	protected int audioRoutineVolumeMode = AUDIO_ROUTING_SPEAKER_ON;

	@Override
	public String getName() {
		return "sceAudioRouting";
	}

	/**
	 * Set routing mode.
	 *
	 * @param mode The routing mode to set (0 or 1)
	 *
	 * @return the previous routing mode, or < 0 on error
	*/
	@HLELogging(level="info")
	@HLEFunction(nid = 0x36FD8AA9, version = 150)
	public int sceAudioRoutingSetMode(int mode) {
		int previousMode = audioRoutingMode;

		audioRoutingMode = mode;

		return previousMode;
	}

	/**
	 * Get routing mode.
	 *
	 * @return the current routing mode.
	*/
	@HLEFunction(nid = 0x39240E7D, version = 150)
	public int sceAudioRoutingGetMode() {
		return audioRoutingMode;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x28235C56, version = 150)
	public int sceAudioRoutingGetVolumeMode() {
		return audioRoutineVolumeMode;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xBB548475, version = 150)
	public int sceAudioRoutingSetVolumeMode(int mode) {
		int previousMode = audioRoutineVolumeMode;

		audioRoutineVolumeMode = mode;

		return previousMode;
	}
}
