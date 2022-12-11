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

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.VideoEngineUtilities.getPixelFormatBytes;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContext;
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
	private int pixelMode;
	private int defaultBufferWidth;
	private static Set<int[]> intBuffers;
	private static final int MAX_INT_BUFFERS_SIZE = 12;

	public static class YCbCrImageState {
		public int width;
		public int height;
		public int lengthLuma;
        public int[] luma;
        public int[] cb;
        public int[] cr;

        public void releaseIntBuffers() {
            releaseIntBuffer(luma);
            luma = null;

            releaseIntBuffer(cb);
            cb = null;

            releaseIntBuffer(cr);
            cr = null;
        }
	}

	@Override
	public void start() {
		pixelMode = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
		defaultBufferWidth = 0;
		intBuffers = new HashSet<int[]>();

		super.start();
	}

	@Override
	public void stop() {
		// Free the temporary arrays
    	intBuffers.clear();

    	super.stop();
	}

	public static int getPixelMode(int internalPixelMode) {
    	switch (internalPixelMode) {
			case 0: return TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
			case 1: return TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
			case 2: return TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
			case 3: return TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
    	}

    	log.error(String.format("getPixelMode unknown internalPixelMode=0x%X", internalPixelMode));

    	return -1;
	}

	public int getPixelMode() {
		return pixelMode;
	}

	public static int[] getIntBuffer(int length) {
    	synchronized (intBuffers) {
        	for (int[] intBuffer : intBuffers) {
        		if (intBuffer.length >= length) {
        			intBuffers.remove(intBuffer);
        			return intBuffer;
        		}
        	}
		}

    	return new int[length];
    }

    public static void releaseIntBuffer(int[] intBuffer) {
    	if (intBuffer == null) {
    		return;
    	}

    	synchronized (intBuffers) {
        	intBuffers.add(intBuffer);

	    	if (intBuffers.size() > MAX_INT_BUFFERS_SIZE) {
	    		// Remove the smallest int buffer
	    		int[] smallestIntBuffer = null;
	    		for (int[] buffer : intBuffers) {
	    			if (smallestIntBuffer == null || buffer.length < smallestIntBuffer.length) {
	    				smallestIntBuffer = buffer;
	    			}
	    		}
	
	    		intBuffers.remove(smallestIntBuffer);
	    	}
    	}
    }

    public static void read(Memory mem, int addr, int length, int[] buffer, int offset) {
    	if (log.isTraceEnabled()) {
			log.trace(String.format("read addr=0x%08X, length=0x%X", addr, length));
		}

    	// Optimize the most common case
        if (mem.hasMemoryInt(addr)) {
        	int length4 = length >> 2;
        	int addrOffset = mem.getMemoryIntOffset(addr);
        	int[] memoryInt = mem.getMemoryInt(addr);
	        for (int i = 0, j = offset; i < length4; i++) {
	        	int value = memoryInt[addrOffset++];
	        	buffer[j++] = (value      ) & 0xFF;
	        	buffer[j++] = (value >>  8) & 0xFF;
	        	buffer[j++] = (value >> 16) & 0xFF;
	        	buffer[j++] = (value >> 24) & 0xFF;
	        }
        } else {
	        IMemoryReader memoryReader = MemoryReader.getMemoryReader(mem, addr, length, 1);
	        for (int i = 0, j = offset; i < length; i++) {
	        	buffer[j++] = memoryReader.readNext();
	        }
        }
    }

    public static void read(TPointer addr, int length, int[] buffer, int offset) {
    	read(addr.getMemory(), addr.getAddress(), length, buffer, offset);
    }

    public static void write(Memory mem, int addr, int length, int[] buffer, int offset) {
    	// Optimize the most common case
        if (mem.hasMemoryInt(addr)) {
        	int length4 = length >> 2;
        	int addrOffset = mem.getMemoryIntOffset(addr);
        	int[] memoryInt = mem.getMemoryInt(addr);
	        for (int i = 0, j = offset; i < length4; i++) {
	        	int value = buffer[j++] & 0xFF;
	        	value |= (buffer[j++] & 0xFF) << 8;
	        	value |= (buffer[j++] & 0xFF) << 16;
	        	value |= (buffer[j++]       ) << 24;
	        	memoryInt[addrOffset++] = value;
	        }
        } else {
    	    IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(mem, addr, length, 1);
	        for (int i = 0, j = offset; i < length; i++) {
	        	memoryWriter.writeNext(buffer[j++]);
	        }
	        memoryWriter.flush();
        }
    }

    public static void write(TPointer addr, int length, int[] buffer, int offset) {
    	write(addr.getMemory(), addr.getAddress(), length, buffer, offset);
    }

    private static void copy(Memory memDst, int dst, Memory memSrc, int src, int length) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("copy dst=0x%08X, src=0x%08X, length=0x%X", dst, src, length));
		}
		new TPointer(memDst, dst).memcpy(new TPointer(memSrc, src), length);
	}

	private static void copyBlocks(Memory memDst, int dst, Memory memSrc, int src, int blocks) {
		copy(memDst, dst, memSrc, src, blocks << 4);
	}

	public static void read(YCbCrImageState imageState, int width, int height, Memory mem, int buffer0, int buffer1, int buffer2, int buffer3, int buffer4, int buffer5, int buffer6, int buffer7) {
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

		read(mem, buffer0, sizeY1, bufferY1, 0);
		read(mem, buffer1, sizeY2, bufferY2, 0);
		read(mem, buffer4, sizeCrCb1, bufferCrCb1, 0);
		read(mem, buffer5, sizeCrCb2, bufferCrCb2, 0);
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

		read(mem, buffer2, sizeY1, bufferY1, 0);
		read(mem, buffer3, sizeY2, bufferY2, 0);
		read(mem, buffer6, sizeCrCb1, bufferCrCb1, 0);
		read(mem, buffer7, sizeCrCb2, bufferCrCb2, 0);
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

		imageState.width = width;
		imageState.height = height;
		imageState.lengthLuma = length;
		imageState.luma = luma;
		imageState.cb = cb;
		imageState.cr = cr;
	}

	public int hleMpegBaseCscAvc(TPointer bufferRGB, TPointer unknown, int bufferWidth, int videoPixelMode, SceMp4AvcCscStruct mp4AvcCscStruct) {
    	int rangeX = 0;
        int rangeY = 0;
        int rangeWidth = mp4AvcCscStruct.width << 4;
        int rangeHeight = mp4AvcCscStruct.height << 4;

    	return hleMpegBaseCscAvcRange(bufferRGB, unknown, bufferWidth, videoPixelMode, mp4AvcCscStruct, rangeX, rangeY, rangeWidth, rangeHeight);
    }

    private int hleMpegBaseCscAvcRange(TPointer bufferRGB, TPointer unknown, int bufferWidth, int videoPixelMode, SceMp4AvcCscStruct mp4AvcCscStruct, int rangeX, int rangeY, int rangeWidth, int rangeHeight) {
    	if (bufferWidth == 0) {
    		bufferWidth = defaultBufferWidth;
    	}

    	int width = mp4AvcCscStruct.width << 4;
    	int height = mp4AvcCscStruct.height << 4;

		int bytesPerPixel = getPixelFormatBytes(videoPixelMode);
        int destAddr = bufferRGB.getAddress();

        if (log.isTraceEnabled()) {
        	log.trace(String.format("hleMpegBaseCscAvcRange bufferWidth=%d, width=%d, height=%d, pixelMode=%d, destAddr=%s", bufferWidth, width, height, videoPixelMode, bufferRGB));
        }

        YCbCrImageState imageState = new YCbCrImageState();
        read(imageState, width, height, getMEMemory(), mp4AvcCscStruct.buffer0, mp4AvcCscStruct.buffer1, mp4AvcCscStruct.buffer2, mp4AvcCscStruct.buffer3, mp4AvcCscStruct.buffer4, mp4AvcCscStruct.buffer5, mp4AvcCscStruct.buffer6, mp4AvcCscStruct.buffer7);

        // Convert YCbCr to ABGR
        int[] abgr = getIntBuffer(imageState.lengthLuma);
        H264Utils.YUV2ABGR(width, height, imageState.luma, imageState.cb, imageState.cr, abgr);

        imageState.releaseIntBuffers();

		// Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
        VideoEngine.getInstance().addVideoTexture(destAddr, destAddr + (rangeY + rangeHeight) * bufferWidth * bytesPerPixel);
        // The PSP video player is shifting the destAddr by a few pixels to decode vertical bands
        int shiftedDestAddr = destAddr & ~0x7FF;
		if (destAddr != shiftedDestAddr) {
			VideoEngine.getInstance().addVideoTexture(shiftedDestAddr, shiftedDestAddr + (rangeY + rangeHeight) * bufferWidth * bytesPerPixel);
		}

        // Write the ABGR image
		if (videoPixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 && RuntimeContext.hasMemoryInt(bufferRGB)) {
			// Optimize the most common case
			int pixelIndex = rangeY * width + rangeX;
        	Memory mem = bufferRGB.getMemory();
        	int[] memoryInt = mem.getMemoryInt(destAddr);
        	int addrOffset = mem.getMemoryIntOffset(destAddr);
	        for (int i = 0; i < rangeHeight; i++) {
	        	System.arraycopy(abgr, pixelIndex, memoryInt, addrOffset, rangeWidth);
	        	pixelIndex += width;
	        	addrOffset += bufferWidth;
	        }
		} else {
        	TPointer addr = new TPointer(bufferRGB);
	        for (int i = 0; i < rangeHeight; i++) {
	        	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, rangeWidth * bytesPerPixel, bytesPerPixel);
	        	int pixelIndex = (i + rangeY) * width + rangeX;
	        	for (int j = 0; j < rangeWidth; j++, pixelIndex++) {
	        		int abgr8888 = abgr[pixelIndex];
	        		int pixelColor = Debug.getPixelColor(abgr8888, videoPixelMode);
	        		memoryWriter.writeNext(pixelColor);
	        	}
	        	memoryWriter.flush();
	        	addr.add(bufferWidth * bytesPerPixel);
	        }
		}
		releaseIntBuffer(abgr);

		return 0;
    }

    @HLEFunction(nid = 0xBEA18F91, version = 150)
    public int sceMpegBasePESpacketCopy(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer packetInfo) {
    	while (packetInfo.isNotNull()) {
    		TPointer bufferAddr = packetInfo.getPointer(0);
    		TPointer destinationAddr = packetInfo.getPointer(getMEMemory(), 4);
    		TPointer nextPacketInfo = packetInfo.getPointer(8);
    		int bufferLength = packetInfo.getValue32(12) & 0x00000FFF;

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMpegBasePESpacketCopy packet at %s: bufferAddr=%s, destinationAddr=%s, nextInfoAddr=%s, bufferLength=0x%X", packetInfo, bufferAddr, destinationAddr, nextPacketInfo, bufferLength));
    			if (log.isTraceEnabled()) {
        			log.debug(String.format("sceMpegBasePESpacketCopy %s", Utilities.getMemoryDump(bufferAddr, bufferLength)));
    			}
    		}
    		if (bufferLength == 0) {
    			return SceKernelErrors.ERROR_INVALID_SIZE;
    		}
    		destinationAddr.memcpy(bufferAddr, bufferLength);
    		packetInfo = nextPacketInfo;
    	}

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

    	Memory memDst = destBufferYCrCb.getMemory();
    	Memory memSrc = getMEMemory();
    	int sizeY = srcMpegYCrCbBuffer.frameWidth * srcMpegYCrCbBuffer.frameHeight;
    	int sizeCrCb = sizeY >> 2;
    	copy(memDst, destMpegYCrCbBuffer.bufferY, memSrc, srcMpegYCrCbBuffer.bufferY, sizeY);
    	copy(memDst, destMpegYCrCbBuffer.bufferY2, memSrc, srcMpegYCrCbBuffer.bufferY2, sizeY);
    	copy(memDst, destMpegYCrCbBuffer.bufferCr, memSrc, srcMpegYCrCbBuffer.bufferCr, sizeCrCb);
    	copy(memDst, destMpegYCrCbBuffer.bufferCb, memSrc, srcMpegYCrCbBuffer.bufferCb, sizeCrCb);
    	copy(memDst, destMpegYCrCbBuffer.bufferCr2, memSrc, srcMpegYCrCbBuffer.bufferCr2, sizeCrCb);
    	copy(memDst, destMpegYCrCbBuffer.bufferCb2, memSrc, srcMpegYCrCbBuffer.bufferCb2, sizeCrCb);

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

    	Memory memDst = dst.getMemory();
    	Memory memSrc = getMEMemory();
    	if ((flags & 1) != 0) {
    		copyBlocks(memDst, dstStruct.buffer0, memSrc, srcStruct.buffer0, size1);
    		copyBlocks(memDst, dstStruct.buffer1, memSrc, srcStruct.buffer1, size2);
    		copyBlocks(memDst, dstStruct.buffer4, memSrc, srcStruct.buffer4, size1 >> 1);
    		copyBlocks(memDst, dstStruct.buffer5, memSrc, srcStruct.buffer5, size2 >> 1);
    	}
    	if ((flags & 2) != 0) {
    		copyBlocks(memDst, dstStruct.buffer2, memSrc, srcStruct.buffer2, size1);
    		copyBlocks(memDst, dstStruct.buffer3, memSrc, srcStruct.buffer3, size2);
    		copyBlocks(memDst, dstStruct.buffer6, memSrc, srcStruct.buffer6, size1 >> 1);
    		copyBlocks(memDst, dstStruct.buffer7, memSrc, srcStruct.buffer7, size2 >> 1);
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
		int bytesPerPixel = getPixelFormatBytes(videoPixelMode);
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
        read(getMEMemory(), sceMpegYCrCbBuffer.bufferY, length, luma, 0);
        read(getMEMemory(), sceMpegYCrCbBuffer.bufferCb, length2, cb, 0);
        read(getMEMemory(), sceMpegYCrCbBuffer.bufferCr, length2, cr, 0);

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

    @HLEFunction(nid = 0x492B5E4B, version = 150)
    public int sceMpegBaseCscInit(int bufferWidth) {
    	// sceMpegBaseCscAvc is decoding with no alpha
    	H264Utils.setAlpha(0x00);

    	TPointer unknown = Utilities.allocatePointer(22);
    	unknown.setValue32(0, 0x00000095);
    	unknown.setValue32(4, 0x009500CC);
    	unknown.setValue32(8, 0x039803CE);
    	unknown.setValue32(12, 0x01020095);
    	unknown.setValue32(16, 0x00100000);
    	unknown.setValue16(20, (short) 0x0001);

    	return sceMpegbase_AC9E717E(bufferWidth, unknown);
    }

    @HLEFunction(nid = 0x91929A21, version = 150)
    public int sceMpegBaseCscAvc(TPointer bufferRGB, @CanBeNull TPointer unknown, int bufferWidth, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer mp4AvcCscStructAddr) {
        SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
    	mp4AvcCscStruct.read(mp4AvcCscStructAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegBaseCscAvc %s", mp4AvcCscStruct));
    	}

    	return hleMpegBaseCscAvc(bufferRGB, unknown, bufferWidth, pixelMode, mp4AvcCscStruct);
    }

    @HLEFunction(nid = 0x304882E1, version = 150)
    public int sceMpegBaseCscAvcRange(TPointer bufferRGB, @CanBeNull TPointer unknown, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer32 rangeAddr, int bufferWidth, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer mp4AvcCscStructAddr) {
        SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
    	mp4AvcCscStruct.read(mp4AvcCscStructAddr);

    	int rangeX = rangeAddr.getValue(0) << 4;
        int rangeY = rangeAddr.getValue(4) << 4;
        int rangeWidth = rangeAddr.getValue(8) << 4;
        int rangeHeight = rangeAddr.getValue(12) << 4;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegBaseCscAvcRange range x=%d, y=%d, width=%d, height=%d, %s", rangeX, rangeY, rangeWidth, rangeHeight, mp4AvcCscStruct));
        }

        return hleMpegBaseCscAvcRange(bufferRGB, unknown, bufferWidth, pixelMode, mp4AvcCscStruct, rangeX, rangeY, rangeWidth, rangeHeight);
    }

    @HLEFunction(nid = 0x0530BE4E, version = 150)
    public int sceMpegbase_0530BE4E(int internalPixelMode) {
    	int pixelMode = getPixelMode(internalPixelMode);
    	if (pixelMode < 0) {
    		return -1;
    	}

    	this.pixelMode = pixelMode;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAC9E717E, version = 150)
    public int sceMpegbase_AC9E717E(int defaultBufferWidth, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=22, usage=Usage.in) TPointer unknown) {
    	this.defaultBufferWidth = defaultBufferWidth;

    	return 0;
    }
}
