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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;

public class sceI2c extends HLEModule {
    public static Logger log = Modules.getLogger("sceI2c");

    @HLEUnimplemented
	@HLEFunction(nid = 0x47BDEAAA, version = 150)
	public int sceI2cMasterTransmitReceive(int transmitI2cAddress, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer transmitBuffer, int transmitBufferSize, int receiveI2cAddress, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer receiveBuffer, int receiveBufferSize) {
    	receiveBuffer.clear(receiveBufferSize);

    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x8CBD8CCF, version = 150)
	public int sceI2cMasterTransmit(int transmitI2cAddress, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer transmitBuffer, int transmitBufferSize) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x4020DC7E, version = 150)
	public int sceI2cSetPollingMode() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x49B159DE, version = 150)
	public int sceI2cMasterReceive(int receiveI2cAddress, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer receiveBuffer, int receiveBufferSize) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x62C7E1E4, version = 150)
	public int sceI2cSetClock(int unknown1, int unknown2) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD35FC17D, version = 150)
	public int sceI2cReset() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xDBE12CED, version = 150)
	public int sceI2cSetDebugHandlers() {
    	return 0;
	}
}
