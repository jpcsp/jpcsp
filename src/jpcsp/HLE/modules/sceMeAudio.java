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
package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceMeAudio extends HLEModule {
    public static Logger log = Modules.getLogger("sceMeAudio");

    // Called by sceAudiocodecCheckNeedMem
	@HLEUnimplemented
	@HLEFunction(nid = 0x81956A0B, version = 150)
	public int sceMeAudio_driver_81956A0B(int codecType, TPointer workArea) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6AD33F60, version = 150)
	public int sceMeAudio_driver_6AD33F60() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9A9E21EE, version = 150)
	public int sceMeAudio_driver_9A9E21EE() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB57F033A, version = 150)
	public int sceMeAudio_driver_B57F033A() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xC300D466, version = 150)
	public int sceMeAudio_driver_C300D466() {
		return 0;
	}
}
