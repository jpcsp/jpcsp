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
package jpcsp.HLE.modules620;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

@HLELogging
public class sceMp3 extends jpcsp.HLE.modules150.sceMp3 {
	@HLEUnimplemented
	@HLEFunction(nid = 0x1B839B83 , version = 620)
    public int sceMp3LowLevelInit(Mp3Stream mp3Stream, int unknown) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xE3EE2C81, version = 620)
    public int sceMp3LowLevelDecode(Mp3Stream mp3Stream, TPointer sourceAddr, TPointer32 sourceBytesConsumedAddr, TPointer samplesAddr, TPointer32 sampleBytesAddr) {
		sampleBytesAddr.setValue(0);
		sourceBytesConsumedAddr.setValue(0);

		return 0;
	}
}
