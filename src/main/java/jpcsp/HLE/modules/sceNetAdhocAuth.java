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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

public class sceNetAdhocAuth extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetAdhocAuth");

    @HLEUnimplemented
    @HLEFunction(nid = 0x86004235, version = 150)
    public int sceNetAdhocAuthInit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6074D8F1, version = 150)
    public int sceNetAdhocAuthTerm() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x015A8A64, version = 150)
    public int sceNetAdhocAuth_015A8A64(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferLength) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1F9A90B8, version = 150)
    public int sceNetAdhocAuth_1F9A90B8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2AD8C677, version = 150)
    public int sceNetAdhocAuth_2AD8C677() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2E6AA271, version = 150)
    public int sceNetAdhocAuth_2E6AA271() {
    	// Has no parameters
    	// Termination of what has been initialized with sceNetAdhocAuth_89F2A732()
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x312BD812, version = 150)
    public int sceNetAdhocAuth_312BD812() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6CE209A3, version = 150)
    public int sceNetAdhocAuth_6CE209A3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x72AAC6D3, version = 150)
    public int sceNetAdhocAuth_72AAC6D3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x76F26AB0, version = 150)
    public int sceNetAdhocAuth_76F26AB0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x89F2A732, version = 150)
    public int sceNetAdhocAuth_89F2A732(PspString interfaceName, int threadPriority, int threadStackSize, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer32 unknown1, int unknown2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=128, usage=Usage.in) TPointer ownerName) {
    	// Some initialization routine
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAAB06250, version = 150)
    public int sceNetAdhocAuth_AAB06250() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBD144DA6, version = 150)
    public int sceNetAdhocAuth_BD144DA6() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCF4D9BED, version = 150)
    public int sceNetAdhocAuth_CF4D9BED() {
    	return 0;
    }
}
