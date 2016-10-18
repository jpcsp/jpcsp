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

import jpcsp.Memory;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceMp4AvcCscStruct;

public class sceMpegbase extends HLEModule {
	public static Logger log = Modules.getLogger("sceMpegbase");

    @HLEUnimplemented
    @HLEFunction(nid = 0xBEA18F91, version = 150)
    public int sceMpegBasePESpacketCopy(TPointer32 packetInfo) {
    	Memory mem = Memory.getInstance();
    	int infoAddr = packetInfo.getAddress();
    	while (infoAddr != 0) {
    		int bufferAddr = mem.read32(infoAddr + 0);
    		int unknown = mem.read32(infoAddr + 4);
    		int nextInfoAddr = mem.read32(infoAddr + 8);
    		int bufferLength = mem.read32(infoAddr + 12) & 0x00000FFF;

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMpegBasePESpacketCopy packet at 0x%08X: bufferAddr=0x%08X, unknown=0x%08X, nextInfoAddr=0x%08X, bufferLength=0x%X", infoAddr, bufferAddr, unknown, nextInfoAddr, bufferLength));
    		}
    		if (bufferLength == 0) {
    			return SceKernelErrors.ERROR_INVALID_SIZE;
    		}
        	Modules.sceMpegModule.addToVideoBuffer(mem, bufferAddr, bufferLength);
        	infoAddr = nextInfoAddr;
    	}

    	Modules.sceMpegModule.hleMpegNotifyRingbufferRead();
    	Modules.sceMpegModule.hleMpegNotifyVideoDecoderThread();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBE45C284, version = 150)
    public int sceMpegBaseYCrCbCopyVme() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7AC0321A, version = 150)
    public int sceMpegBaseYCrCbCopy(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer dst, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer src, int flags) {
    	SceMp4AvcCscStruct dstStruct = new SceMp4AvcCscStruct();
    	dstStruct.read(dst);
    	SceMp4AvcCscStruct srcStruct = new SceMp4AvcCscStruct();
    	srcStruct.read(src);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegBaseYCrCbCopy dstStruct: %s", dstStruct));
    		log.debug(String.format("sceMpegBaseYCrCbCopy srcStruct: %s", srcStruct));
    	}

    	int size1 = ((srcStruct.width + 16) >> 5) * ((srcStruct.height << 4) >> 1);
    	int size2 = (srcStruct.width >> 5) * ((srcStruct.height << 4) >> 1);
    	Memory mem = Memory.getInstance();
    	if ((flags & 1) != 0) {
    		mem.memcpy(dstStruct.buffer0, srcStruct.buffer0, size1);
    		mem.memcpy(dstStruct.buffer1, srcStruct.buffer1, size2);
    		mem.memcpy(dstStruct.buffer4, srcStruct.buffer4, size1 << 1);
    		mem.memcpy(dstStruct.buffer5, srcStruct.buffer5, size2 << 1);
    	}
    	if ((flags & 2) != 0) {
    		mem.memcpy(dstStruct.buffer2, srcStruct.buffer2, size1);
    		mem.memcpy(dstStruct.buffer3, srcStruct.buffer3, size2);
    		mem.memcpy(dstStruct.buffer6, srcStruct.buffer6, size1 << 1);
    		mem.memcpy(dstStruct.buffer7, srcStruct.buffer7, size2 << 1);
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE8EB837, version = 150)
    public int sceMpegBaseCscVme() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x492B5E4B, version = 150)
    public int sceMpegBaseCscInit(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x91929A21, version = 150)
    public int sceMpegBaseCscAvc(int mpeg, int unknown1, int unknown2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer mp4AvcCscStructAddr) {
    	SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
    	mp4AvcCscStruct.read(mp4AvcCscStructAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegBaseCscAvc %s", mp4AvcCscStruct));
    	}

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0530BE4E, version = 150)
    public int sceMpegbase_0530BE4E() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAC9E717E, version = 150)
    public int sceMpegbase_AC9E717E() {
        return 0;
    }
}
