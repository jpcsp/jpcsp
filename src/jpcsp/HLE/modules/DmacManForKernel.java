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
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

public class DmacManForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("DmacManForKernel");

    @HLEUnimplemented
	@HLEFunction(nid = 0x59615199, version = 150)
	public int sceKernelDmaOpAlloc() {
    	// Has no parameters
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x745E19EF, version = 150)
	public int sceKernelDmaOpFree() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xF64BAB99, version = 150)
	public int sceKernelDmaOpAssign(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.inout) TPointer dmaOpAddr, int unknown1, int unknown2, int unknown3) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x3BDEA96C, version = 150)
	public int sceKernelDmaOpEnQueue(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.inout) TPointer dmaOpAddr) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5AF32783, version = 150)
	public int sceKernelDmaOpQuit(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.in) TPointer dmaOpAddr) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x92700CCD, version = 150)
	public int sceKernelDmaOpDeQueue() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xCE467D9B, version = 150)
	public int sceKernelDmaOpSetupNormal(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.inout) TPointer dmaOpAddr, int status, TPointer dstAddress, TPointer srcAddress, int attributes) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD0358BE9, version = 150)
	public int sceKernelDmaOpSetCallback(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.inout) TPointer dmaOpAddr, TPointer callback, int unknown) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xDB286D65, version = 150)
	public int sceKernelDmaOpSync(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.inout) TPointer dmaOpAddr, int waitType, int timeout) {
    	// waitType = 0: do not wait for completion of DMA Operation, return error when still running
    	// waitType = 1: wait indefinitely for completion of the DMA Operation
    	// waitType = 2: wait for given timeout for completion of the DMA Operation
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7D21A2EF, version = 150)
	public int sceKernelDmaOpSetupLink(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.inout) TPointer dmaOpAddr, int status, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer32 linkStructure) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x3FAD5844, version = 150)
	public int sceKernelDmaOpSetupMemcpy(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=30, usage=Usage.inout) TPointer dmaOpAddr, TPointer dstAddress, TPointer srcAddress, int length) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x32757C57, version = 150)
	public int DmacManForKernel_32757C57(@CanBeNull TPointer setupLinkCallback) {
    	return 0;
	}
}
