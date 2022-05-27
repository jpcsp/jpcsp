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
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.PRX;
import jpcsp.util.Utilities;

public class sceNwman extends HLEModule {
    public static Logger log = Modules.getLogger("sceNwman");
    private PRX prxEngine = new CryptoEngine().getPRXEngine();

    public int hleNwman_driver_9555D68D(byte[] buffer, int bufferOffset, int bufferSize, TPointer32 resultSizeAddr) {
        int tag = Utilities.readUnaligned32(buffer, bufferOffset + 0xD0);
        int type;
        switch (tag) {
        	case 0x06000000:
        		type = 0;
        		break;
        	case 0xE42C2303:
        		type = 2;
        		break;
    		default:
    			return -301;
        }

    	int result = prxEngine.DecryptPRX(buffer, bufferSize, type, null, null);

    	int resultSize = 0;
    	if (result > 0) {
    		resultSize = result;
    		result = 0;
    	}
    	resultSizeAddr.setValue(resultSize);

    	return result;
    }

    @HLEFunction(nid = 0x9555D68D, version = 100)
    public int sceNwman_driver_9555D68D(@BufferInfo(lengthInfo = LengthInfo.nextParameter, usage = Usage.inout) TPointer bufferAddr, int bufferSize, @BufferInfo(usage = Usage.out) TPointer32 resultSizeAddr) {
        byte[] buffer = bufferAddr.getArray8(bufferSize);
    	int result = hleNwman_driver_9555D68D(buffer, 0, bufferSize, resultSizeAddr);
    	bufferAddr.setArray(buffer);

    	return result;
    }
}
