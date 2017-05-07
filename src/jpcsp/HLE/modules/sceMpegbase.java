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

import static jpcsp.HLE.modules.sceMpeg.getIntBuffer;
import static jpcsp.HLE.modules.sceMpeg.releaseIntBuffer;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.compiler.RuntimeContext;
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
import jpcsp.util.Utilities;

public class sceMpegbase extends HLEModule {
	public static Logger log = Modules.getLogger("sceMpegbase");

	private static void read(int addr, int length, int[] buffer, int offset) {
		addr |= MemoryMap.START_RAM;
		if (log.isTraceEnabled()) {
			log.trace(String.format("read addr=0x%08X, length=0x%X", addr, length));
		}

    	// Optimize the most common case
        if (RuntimeContext.hasMemoryInt()) {
        	int length4 = length >> 2;
        	int addrOffset = addr >> 2;
        	int[] memoryInt = RuntimeContext.getMemoryInt();
	        for (int i = 0, j = offset; i < length4; i++) {
	        	int value = memoryInt[addrOffset++];
	        	buffer[j++] = (value      ) & 0xFF;
	        	buffer[j++] = (value >>  8) & 0xFF;
	        	buffer[j++] = (value >> 16) & 0xFF;
	        	buffer[j++] = (value >> 24) & 0xFF;
	        }
        } else {
	        IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, length, 1);
	        for (int i = 0, j = offset; i < length; i++) {
	        	buffer[j++] = memoryReader.readNext();
	        }
        }
    }

	private static void copy(Memory mem, int dst, int src, int length) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("copy dst=0x%08X, src=0x%08X, length=0x%X", dst, src, length));
		}
		mem.memcpy(dst | MemoryMap.START_RAM, src | MemoryMap.START_RAM, length);
	}

	private static void copyBlocks(Memory mem, int dst, int src, int blocks) {
		copy(mem, dst, src, blocks << 4);
	}

    private int hleMpegBaseCscAvcRange(TPointer bufferRGB, int unknown, int bufferWidth, SceMp4AvcCscStruct mp4AvcCscStruct, int rangeX, int rangeY, int rangeWidth, int rangeHeight) {
    	int width = mp4AvcCscStruct.width << 4;
    	int height = mp4AvcCscStruct.height << 4;

    	// It seems that the pixel output format is always ABGR8888.
    	int videoPixelMode = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
		int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
        int destAddr = bufferRGB.getAddress();

        int width2 = width >> 1;
    	int height2 = height >> 1;
        int length = width * height;
        int length2 = width2 * height2;

        // Read the YCbCr image.
        // See the description of the format used by the PSP in sceVideocodecDecode().
        int[] luma = getIntBuffer(length);
        int[] cb = getIntBuffer(length2);
        int[] cr = getIntBuffer(length2);
		int sizeY1 = ((width + 16) >> 5) * (height >> 1) * 16;
		int sizeY2 = (width >> 5) * (height >> 1) * 16;
		int sizeCrCb1 = sizeY1 >> 1;
		int sizeCrCb2 = sizeY1 >> 1;
		int[] bufferY1 = getIntBuffer(sizeY1);
		int[] bufferY2 = getIntBuffer(sizeY2);
		int[] bufferCrCb1 = getIntBuffer(sizeCrCb1);
		int[] bufferCrCb2 = getIntBuffer(sizeCrCb2);

		read(mp4AvcCscStruct.buffer0, sizeY1, bufferY1, 0);
		read(mp4AvcCscStruct.buffer1, sizeY2, bufferY2, 0);
		read(mp4AvcCscStruct.buffer4, sizeCrCb1, bufferCrCb1, 0);
		read(mp4AvcCscStruct.buffer5, sizeCrCb2, bufferCrCb2, 0);
		for (int x = 0, j = 0; x < width; x += 32) {
			for (int y = 0, i = x; y < height; y += 2, i += 2 * width, j += 16) {
				System.arraycopy(bufferY1, j, luma, i, 16);
			}
		}
		for (int x = 16, j = 0; x < width; x += 32) {
			for (int y = 0, i = x; y < height; y += 2, i += 2 * width, j += 16) {
				System.arraycopy(bufferY2, j, luma, i, 16);
			}
		}
		for (int x = 0, j = 0; x < width2; x += 16) {
			for (int y = 0; y < height2; y += 2) {
				for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
					cb[i] = bufferCrCb1[j++];
					cr[i] = bufferCrCb1[j++];
				}
			}
		}
		for (int x = 0, j = 0; x < width2; x += 16) {
			for (int y = 1; y < height2; y += 2) {
				for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
					cb[i] = bufferCrCb2[j++];
					cr[i] = bufferCrCb2[j++];
				}
			}
		}

		read(mp4AvcCscStruct.buffer2, sizeY1, bufferY1, 0);
		read(mp4AvcCscStruct.buffer3, sizeY2, bufferY2, 0);
		read(mp4AvcCscStruct.buffer6, sizeCrCb1, bufferCrCb1, 0);
		read(mp4AvcCscStruct.buffer7, sizeCrCb2, bufferCrCb2, 0);
		for (int x = 0, j = 0; x < width; x += 32) {
			for (int y = 1, i = x + width; y < height; y += 2, i += 2 * width, j += 16) {
				System.arraycopy(bufferY1, j, luma, i, 16);
			}
		}
		for (int x = 16, j = 0; x < width; x += 32) {
			for (int y = 1, i = x + width; y < height; y += 2, i += 2 * width, j += 16) {
				System.arraycopy(bufferY2, j, luma, i, 16);
			}
		}
		for (int x = 8, j = 0; x < width2; x += 16) {
			for (int y = 0; y < height2; y += 2) {
				for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
					cb[i] = bufferCrCb1[j++];
					cr[i] = bufferCrCb1[j++];
				}
			}
		}
		for (int x = 8, j = 0; x < width2; x += 16) {
			for (int y = 1; y < height2; y += 2) {
				for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
					cb[i] = bufferCrCb2[j++];
					cr[i] = bufferCrCb2[j++];
				}
			}
		}

        releaseIntBuffer(bufferY1);
        releaseIntBuffer(bufferY2);
        releaseIntBuffer(bufferCrCb1);
        releaseIntBuffer(bufferCrCb2);

        // Convert YCbCr to ABGR
        int[] abgr = getIntBuffer(length);
        H264Utils.YUV2ABGR(width, height, luma, cb, cr, abgr);

        releaseIntBuffer(luma);
        releaseIntBuffer(cb);
        releaseIntBuffer(cr);

		// Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
        VideoEngine.getInstance().addVideoTexture(destAddr, destAddr + (rangeY + rangeHeight) * bufferWidth * bytesPerPixel);

        // Write the ABGR image
		if (videoPixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 && RuntimeContext.hasMemoryInt()) {
			// Optimize the most common case
			int pixelIndex = rangeY * width + rangeX;
        	int addr = destAddr;
	        for (int i = 0; i < rangeHeight; i++) {
	        	System.arraycopy(abgr, pixelIndex, RuntimeContext.getMemoryInt(), addr >> 2, rangeWidth);
	        	pixelIndex += width;
	        	addr += bufferWidth * bytesPerPixel;
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

    @HLEFunction(nid = 0xBEA18F91, version = 150)
    public int sceMpegBasePESpacketCopy(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer32 packetInfo) {
    	Memory mem = Memory.getInstance();
    	int infoAddr = packetInfo.getAddress();
    	while (infoAddr != 0) {
    		int bufferAddr = mem.read32(infoAddr + 0);
    		int destinationAddr = mem.read32(infoAddr + 4) | MemoryMap.START_RAM;
    		int nextInfoAddr = mem.read32(infoAddr + 8);
    		int bufferLength = mem.read32(infoAddr + 12) & 0x00000FFF;

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMpegBasePESpacketCopy packet at 0x%08X: bufferAddr=0x%08X, destinationAddr=0x%08X, nextInfoAddr=0x%08X, bufferLength=0x%X: %s", infoAddr, bufferAddr, destinationAddr, nextInfoAddr, bufferLength, Utilities.getMemoryDump(bufferAddr, bufferLength)));
    		}
    		if (bufferLength == 0) {
    			return SceKernelErrors.ERROR_INVALID_SIZE;
    		}
    		mem.memcpy(destinationAddr, bufferAddr, bufferLength);
        	infoAddr = nextInfoAddr;
    	}

    	Modules.sceMpegModule.hleMpegNotifyRingbufferRead();
    	Modules.sceMpegModule.hleMpegNotifyVideoDecoderThread();

    	return 0;
    }

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
    	copy(mem, destMpegYCrCbBuffer.bufferY, srcMpegYCrCbBuffer.bufferY, sizeY);
    	copy(mem, destMpegYCrCbBuffer.bufferY2, srcMpegYCrCbBuffer.bufferY2, sizeY);
    	copy(mem, destMpegYCrCbBuffer.bufferCr, srcMpegYCrCbBuffer.bufferCr, sizeCrCb);
    	copy(mem, destMpegYCrCbBuffer.bufferCb, srcMpegYCrCbBuffer.bufferCb, sizeCrCb);
    	copy(mem, destMpegYCrCbBuffer.bufferCr2, srcMpegYCrCbBuffer.bufferCr2, sizeCrCb);
    	copy(mem, destMpegYCrCbBuffer.bufferCb2, srcMpegYCrCbBuffer.bufferCb2, sizeCrCb);

    	return 0;
    }

    @HLEFunction(nid = 0x7AC0321A, version = 150)
    public int sceMpegBaseYCrCbCopy(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer dst, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer src, int flags) {
    	SceMp4AvcCscStruct dstStruct = new SceMp4AvcCscStruct();
    	dstStruct.read(dst);
    	SceMp4AvcCscStruct srcStruct = new SceMp4AvcCscStruct();
    	srcStruct.read(src);

    	int size1 = ((srcStruct.width + 16) >> 5) * (srcStruct.height >> 1);
    	int size2 = (srcStruct.width >> 5) * (srcStruct.height >> 1);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegBaseYCrCbCopy dstStruct: %s", dstStruct));
    		log.debug(String.format("sceMpegBaseYCrCbCopy srcStruct: %s", srcStruct));
    		log.debug(String.format("sceMpegBaseYCrCbCopy size1=0x%X, size2=0x%X", size1, size2));
    	}

    	Memory mem = Memory.getInstance();
    	if ((flags & 1) != 0) {
    		copyBlocks(mem, dstStruct.buffer0, srcStruct.buffer0, size1);
    		copyBlocks(mem, dstStruct.buffer1, srcStruct.buffer1, size2);
    		copyBlocks(mem, dstStruct.buffer4, srcStruct.buffer4, size1 >> 1);
    		copyBlocks(mem, dstStruct.buffer5, srcStruct.buffer5, size2 >> 1);
    	}
    	if ((flags & 2) != 0) {
    		copyBlocks(mem, dstStruct.buffer2, srcStruct.buffer2, size1);
    		copyBlocks(mem, dstStruct.buffer3, srcStruct.buffer3, size2);
    		copyBlocks(mem, dstStruct.buffer6, srcStruct.buffer6, size1 >> 1);
    		copyBlocks(mem, dstStruct.buffer7, srcStruct.buffer7, size2 >> 1);
    	}

    	return 0;
    }

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
        read(sceMpegYCrCbBuffer.bufferY, length, luma, 0);
        read(sceMpegYCrCbBuffer.bufferCb, length2, cb, 0);
        read(sceMpegYCrCbBuffer.bufferCr, length2, cr, 0);

        // Convert YCbCr to ABGR
        int[] abgr = getIntBuffer(length);
        H264Utils.YUV2ABGR(width, height, luma, cb, cr, abgr);

        releaseIntBuffer(luma);
        releaseIntBuffer(cb);
        releaseIntBuffer(cr);

		// Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
        VideoEngine.getInstance().addVideoTexture(destAddr, destAddr + (rangeY + rangeHeight) * bufferWidth * bytesPerPixel);

        // Write the ABGR image
		if (videoPixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 && RuntimeContext.hasMemoryInt()) {
			// Optimize the most common case
			int pixelIndex = rangeY * width + rangeX;
	        for (int i = 0; i < rangeHeight; i++) {
	        	int addr = destAddr + (i * bufferWidth) * bytesPerPixel;
	        	System.arraycopy(abgr, pixelIndex, RuntimeContext.getMemoryInt(), addr >> 2, rangeWidth);
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
    	// sceMpegBaseCscAvc is decoding with no alpha
    	H264Utils.setAlpha(0x00);

    	return 0;
    }

    @HLEFunction(nid = 0x91929A21, version = 150)
    public int sceMpegBaseCscAvc(TPointer bufferRGB, int unknown, int bufferWidth, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer mp4AvcCscStructAddr) {
        SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
    	mp4AvcCscStruct.read(mp4AvcCscStructAddr);

    	int rangeX = 0;
        int rangeY = 0;
        int rangeWidth = mp4AvcCscStruct.width << 4;
        int rangeHeight = mp4AvcCscStruct.height << 4;

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegBaseCscAvc %s", mp4AvcCscStruct));
    	}

    	return hleMpegBaseCscAvcRange(bufferRGB, unknown, bufferWidth, mp4AvcCscStruct, rangeX, rangeY, rangeWidth, rangeHeight);
    }

    @HLEFunction(nid = 0x304882E1, version = 150)
    public int sceMpegBaseCscAvcRange(TPointer bufferRGB, int unknown, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer32 rangeAddr, int bufferWidth, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer mp4AvcCscStructAddr) {
        SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
    	mp4AvcCscStruct.read(mp4AvcCscStructAddr);

    	int rangeX = rangeAddr.getValue(0) << 4;
        int rangeY = rangeAddr.getValue(4) << 4;
        int rangeWidth = rangeAddr.getValue(8) << 4;
        int rangeHeight = rangeAddr.getValue(12) << 4;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegBaseCscAvcRange range x=%d, y=%d, width=%d, height=%d, %s", rangeX, rangeY, rangeWidth, rangeHeight, mp4AvcCscStruct));
        }

        return hleMpegBaseCscAvcRange(bufferRGB, unknown, bufferWidth, mp4AvcCscStruct, rangeX, rangeY, rangeWidth, rangeHeight);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0530BE4E, version = 150)
    public int sceMpegbase_0530BE4E(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAC9E717E, version = 150)
    public int sceMpegbase_AC9E717E(int unknown1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=22, usage=Usage.in) TPointer unknown2) {
        return 0;
    }
}
