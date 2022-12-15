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
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.MemoryInputStream;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointerFunction;

import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.log4j.Logger;

public class UtilsForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("UtilsForKernel");
    public TPointerFunction clockHandler;
    public TPointerFunction timeHandler;
    public TPointerFunction getTimeOfDayHandler;
    public TPointerFunction rtcTickHandler;

    @HLEUnimplemented
    @HLEFunction(nid = 0xA6B0A6B8, version = 150)
    public int UtilsForKernel_A6B0A6B8() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x39FFB756, version = 150)
    public int UtilsForKernel_39FFB756(int unknown) {
    	return 0;
    }

    /**
     * KL4E decompression.
     *
     * @param dest
     * @param destSize
     * @param src
     * @param decompressedSizeAddr
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x6C6887EE, version = 150)
    public int UtilsForKernel_6C6887EE(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer dest, int destSize, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=0x100, usage=Usage.in) TPointer src, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 endOfDecompressedDestAddr) {
    	return 0;
    }

    @HLEFunction(nid = 0xE8DB3CE6, version = 150)
    public int sceKernelDeflateDecompress(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer dest, int destSize, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=0x100, usage=Usage.in) TPointer src, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 endOfDecompressedDestAddr) {
		int decompressedLength = 0;
		byte[] buffer = new byte[destSize];
    	try {
    		Inflater inflater = new Inflater(true); // ZLIB header and checksum fields are ignored
			InflaterInputStream is = new InflaterInputStream(new MemoryInputStream(src), inflater);
			IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dest, destSize, 1);
			while (true) {
				int length = is.read(buffer);
				if (length < 0) {
					// End of stream
					break;
				}
				if (decompressedLength + length > destSize) {
					log.warn(String.format("sceKernelDeflateDecompress : decompress buffer too small inBuffer=%s, outLength=%d", src, destSize));
					is.close();
					return SceKernelErrors.ERROR_INVALID_SIZE;
				}

				for (int i = 0; i < length; i++) {
					memoryWriter.writeNext(buffer[i] & 0xFF);
				}
				decompressedLength += length;
			}
			is.close();
			memoryWriter.flush();
			endOfDecompressedDestAddr.setValue(src.getAddress() + (int) inflater.getBytesRead());
		} catch (IOException e) {
			return SceKernelErrors.ERROR_INVALID_FORMAT;
		}

    	return decompressedLength;
    }

    @HLEFunction(nid = 0x78934841, version = 150)
    public int sceKernelGzipDecompress(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer dest, int destSize, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=0x100, usage=Usage.in) TPointer src, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 crc32Addr) {
    	return Modules.sceDefltModule.sceGzipDecompress(dest, destSize, src, crc32Addr);
    }

    @HLEFunction(nid = 0x23A0C5BA, version = 150)
    public int sceKernelRegisterRtcFunc(TPointerFunction clockHandler, TPointerFunction timeHandler, TPointerFunction getTimeOfDayHandler, TPointerFunction rtcTickHandler) {
    	this.clockHandler = clockHandler;
    	this.timeHandler = timeHandler;
    	this.getTimeOfDayHandler = getTimeOfDayHandler;
    	this.rtcTickHandler = rtcTickHandler;

    	return 0;
    }

    @HLEFunction(nid = 0x41887EF4, version = 150)
    public int sceKernelReleaseRtcFunc() {
    	clockHandler = TPointerFunction.NULL;
    	timeHandler = TPointerFunction.NULL;
    	getTimeOfDayHandler = TPointerFunction.NULL;
    	rtcTickHandler = TPointerFunction.NULL;

    	return 0;
    }
}
