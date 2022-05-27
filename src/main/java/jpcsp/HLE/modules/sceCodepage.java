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

import static jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure.charset16;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer8;

public class sceCodepage extends HLEModule {
    public static Logger log = Modules.getLogger("sceCodepage");

    @HLEUnimplemented
    @HLEFunction(nid = 0xEE932176, version = 150)
    public int sceCodepage_driver_EE932176() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1D0DE569, version = 150)
    public int sceCodepage_driver_1D0DE569(TPointer32 unknown1, TPointer32 unknown2, TPointer32 unknown3, TPointer32 unknown4) {
    	unknown1.setValue(0);
    	unknown2.setValue(0);
    	unknown3.setValue(47880);
    	unknown4.setValue(128);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x039BF9E9, version = 150)
    public int sceCodepage_driver_039BF9E9(TPointer unknown1, int unknown2, TPointer unknown3, int unknown4, TPointer unknown5, int unknown6, TPointer unknown7, int unknown8) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0AE63AA, version = 150)
    public int sceCodepage_driver_B0AE63AA(int c) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x855C5C2E, version = 150)
    public int sceCodepage_driver_855C5C2E(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer destAddr, int destLength, String src) {
    	byte[] destBytes = src.getBytes(charset16);
    	int length = Math.min(destLength, destBytes.length);
    	destAddr.setArray(destBytes, length);
    	// Add trailing "\0\0"
    	if (length <= destLength - 2) {
    		destAddr.clear(length, 2);
    	}

    	return src.length();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x11123ED1, version = 150)
    public boolean sceCodepage_driver_11123ED1(int char16) {
    	if (char16 <= 0 || char16 > 0x7E) {
    		return true;
    	}
    	return false;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x47BDF633, version = 150)
    public int sceCodepage_driver_47BDF633(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer8 destAddr, int destLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer16 srcAddr) {
    	int result = destLength;
		byte[] bytes = new byte[2];
    	for (int i = 0, j = 0; j < destLength; i += 2, j++) {
    		int char16 = srcAddr.getValue(i);
    		if (char16 == 0) {
    			result = j;
    			break;
    		}

    		bytes[0] = (byte) char16;
    		bytes[1] = (byte) (char16 >> 8);
    		byte char8 = (byte) new String(bytes, charset16).charAt(0);

    		destAddr.setValue(j, char8);
    	}

    	if (result < destLength) {
        	// Add trailing '\0'
    		destAddr.setValue(result, (byte) 0);
    	}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x014E0C72, version = 150)
    public boolean sceCodepage_driver_014E0C72(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=2, usage=Usage.in) TPointer8 srcAddr) {
    	return false;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC899572E, version = 150)
    public int sceCodepage_driver_C899572E(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer destAddr, int destLength, String src) {
    	byte[] destBytes = src.getBytes(charset16);
    	int length = Math.min(destLength, destBytes.length);
    	destAddr.setArray(destBytes, length);
    	// Add trailing "\0\0"
    	if (length <= destLength - 2) {
    		destAddr.clear(length, 2);
    	}

    	return src.length();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x907CBFD2, version = 150)
    public int sceCodepage_driver_907CBFD2(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer8 destAddr, int destLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer16 srcAddr) {
    	int result = destLength;
		byte[] bytes = new byte[2];
    	for (int i = 0, j = 0; j < destLength; i += 2, j++) {
    		int char16 = srcAddr.getValue(i);
    		if (char16 == 0) {
    			result = j;
    			break;
    		}

    		bytes[0] = (byte) char16;
    		bytes[1] = (byte) (char16 >> 8);
    		byte char8 = (byte) new String(bytes, charset16).charAt(0);

    		destAddr.setValue(j, char8);
    	}

    	if (result < destLength) {
        	// Add trailing '\0'
    		destAddr.setValue(result, (byte) 0);
    	}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0AA54D6D, version = 150)
    public int sceCodepage_driver_0AA54D6D(TPointer32 unknown) {
    	unknown.setValue(0);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x266ABDD8, version = 150)
    public int sceCodepage_driver_266ABDD8() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDCD95405, version = 150)
    public int sceCodepage_driver_DCD95405(int unknown) {
    	return 0;
    }
}
