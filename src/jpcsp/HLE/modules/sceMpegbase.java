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

import static jpcsp.Allegrex.compiler.RuntimeContext.memoryInt;
import static jpcsp.HLE.modules.sceMpeg.getIntBuffer;
import static jpcsp.HLE.modules.sceMpeg.releaseIntBuffer;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.MemoryMap;
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
import jpcsp.HLE.kernel.types.SceMpegYCrCbBuffer;
import jpcsp.HLE.kernel.types.SceMpegYCrCbBufferSrc;
import jpcsp.graphics.VideoEngine;
import jpcsp.media.codec.h264.H264Utils;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Debug;

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
    public int sceMpegBaseYCrCbCopyVme(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.in) TPointer destBufferYCrCb, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer32 srcBufferYCrCb, int type) {
    	SceMpegYCrCbBuffer destMpegYCrCbBuffer = new SceMpegYCrCbBuffer();
    	destMpegYCrCbBuffer.read(destBufferYCrCb);

    	SceMpegYCrCbBufferSrc srcMpegYCrCbBuffer = new SceMpegYCrCbBufferSrc();
    	srcMpegYCrCbBuffer.read(srcBufferYCrCb);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegBaseYCrCbCopyVme destMpegYCrCbBuffer: %s", destMpegYCrCbBuffer));
    		log.debug(String.format("sceMpegBaseYCrCbCopyVme srcMpegYCrCbBuffer: %s", srcMpegYCrCbBuffer));
    	}

    	Memory mem = destBufferYCrCb.getMemory();
    	int sizeY = srcMpegYCrCbBuffer.frameWidth * srcMpegYCrCbBuffer.frameHeight;
    	int sizeCrCb = sizeY >> 2;
    	int baseAddr = MemoryMap.START_RAM;
    	mem.memcpy(destMpegYCrCbBuffer.bufferY, srcMpegYCrCbBuffer.bufferY | baseAddr, sizeY);
    	mem.memcpy(destMpegYCrCbBuffer.bufferY2, srcMpegYCrCbBuffer.bufferY2 | baseAddr, sizeY);
    	mem.memcpy(destMpegYCrCbBuffer.bufferCr, srcMpegYCrCbBuffer.bufferCr | baseAddr, sizeCrCb);
    	mem.memcpy(destMpegYCrCbBuffer.bufferCb, srcMpegYCrCbBuffer.bufferCb | baseAddr, sizeCrCb);
    	mem.memcpy(destMpegYCrCbBuffer.bufferCr2, srcMpegYCrCbBuffer.bufferCr2 | baseAddr, sizeCrCb);
    	mem.memcpy(destMpegYCrCbBuffer.bufferCb2, srcMpegYCrCbBuffer.bufferCb2 | baseAddr, sizeCrCb);

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
    public int sceMpegBaseCscVme(TPointer bufferRGB, TPointer bufferRGB2, int bufferWidth, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.in) TPointer32 bufferYCrCb) {
    	SceMpegYCrCbBuffer sceMpegYCrCbBuffer = new SceMpegYCrCbBuffer();
    	sceMpegYCrCbBuffer.read(bufferYCrCb);

    	int width = sceMpegYCrCbBuffer.frameBufferWidth16 << 4;
    	int height = sceMpegYCrCbBuffer.frameBufferHeight16 << 4;

    	int videoPixelMode = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
		int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
        int rangeX = 0;
        int rangeY = 0;
        int rangeWidth = width;
        int rangeHeight = height;
        int destAddr = bufferRGB.getAddress();

        if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegBaseCscVme sceMpegYCrCbBuffer: %s", sceMpegYCrCbBuffer));
    	}

        int width2 = width >> 1;
    	int height2 = height >> 1;
        int length = width * height;
        int length2 = width2 * height2;

        // Read the YCbCr image
        int[] luma = getIntBuffer(length);
        int[] cb = getIntBuffer(length2);
        int[] cr = getIntBuffer(length2);
        int dataAddrY = sceMpegYCrCbBuffer.bufferY;
        int dataAddrCr = sceMpegYCrCbBuffer.bufferCr;
        int dataAddrCb = sceMpegYCrCbBuffer.bufferCb;
        if (memoryInt != null) {
        	// Optimize the most common case
        	int length4 = length >> 2;
            int offset = dataAddrY >> 2;
            for (int i = 0, j = 0; i < length4; i++) {
            	int value = memoryInt[offset++];
            	luma[j++] = (value      ) & 0xFF;
            	luma[j++] = (value >>  8) & 0xFF;
            	luma[j++] = (value >> 16) & 0xFF;
            	luma[j++] = (value >> 24) & 0xFF;
            }

            int length16 = length2 >> 2;
            offset = dataAddrCb >> 2;
            for (int i = 0, j = 0; i < length16; i++) {
            	int value = memoryInt[offset++];
            	cb[j++] = (value      ) & 0xFF;
            	cb[j++] = (value >>  8) & 0xFF;
            	cb[j++] = (value >> 16) & 0xFF;
            	cb[j++] = (value >> 24) & 0xFF;
            }

            offset = dataAddrCr >> 2;
            for (int i = 0, j = 0; i < length16; i++) {
            	int value = memoryInt[offset++];
            	cr[j++] = (value      ) & 0xFF;
            	cr[j++] = (value >>  8) & 0xFF;
            	cr[j++] = (value >> 16) & 0xFF;
            	cr[j++] = (value >> 24) & 0xFF;
            }
        } else {
	        IMemoryReader memoryReader = MemoryReader.getMemoryReader(dataAddrY, length, 1);
	        for (int i = 0; i < length; i++) {
	        	luma[i] = memoryReader.readNext();
	        }

	        memoryReader = MemoryReader.getMemoryReader(dataAddrCb, 1);
	        for (int i = 0; i < length2; i++) {
	        	cb[i] = memoryReader.readNext();
	        }

	        memoryReader = MemoryReader.getMemoryReader(dataAddrCr, 1);
	        for (int i = 0; i < length2; i++) {
	        	cr[i] = memoryReader.readNext();
	        }
        }

        // Convert YCbCr to ABGR
        int[] abgr = getIntBuffer(length);
        H264Utils.YUV2ABGR(width, height, luma, cb, cr, abgr);

        releaseIntBuffer(luma);
        releaseIntBuffer(cb);
        releaseIntBuffer(cr);

		// Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
        VideoEngine.getInstance().addVideoTexture(destAddr, destAddr + (rangeY + rangeHeight) * bufferWidth * bytesPerPixel);

        // Write the ABGR image
		if (videoPixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 && memoryInt != null) {
			// Optimize the most common case
			int pixelIndex = rangeY * width + rangeX;
	        for (int i = 0; i < rangeHeight; i++) {
	        	int addr = destAddr + (i * bufferWidth) * bytesPerPixel;
	        	System.arraycopy(abgr, pixelIndex, memoryInt, addr >> 2, rangeWidth);
	        	pixelIndex += width;
	        }
		} else {
        	int addr = destAddr;
	        for (int i = 0; i < rangeHeight; i++) {
	        	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, rangeWidth * bytesPerPixel, bytesPerPixel);
	        	int pixelIndex = (i + rangeY) * width + rangeX;
	        	for (int j = 0; j < rangeWidth; j++, pixelIndex++) {
	        		int abgr8888 = abgr[pixelIndex];
	        		int pixelColor = Debug.getPixelColor(abgr8888, videoPixelMode);
	        		memoryWriter.writeNext(pixelColor);
	        	}
	        	memoryWriter.flush();
	        	addr += bufferWidth * bytesPerPixel;
	        }
		}
		releaseIntBuffer(abgr);

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
