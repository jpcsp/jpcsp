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
import static jpcsp.util.Utilities.alignUp;

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
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.IVideoCodec;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

public class sceVideocodec extends HLEModule {
    public static Logger log = Modules.getLogger("sceVideocodec");
    public static final int EDRAM_MEMORY_MASK = 0x03FFFFFF;
    protected SysMemInfo memoryInfo;
    protected int frameCount;
    protected int bufferY1;
    protected int bufferY2;
    protected int bufferCr1;
    protected int bufferCr2;
    protected int bufferCb1;
    protected int bufferCb2;
    protected IVideoCodec videoCodec;

    @HLEUnimplemented
    @HLEFunction(nid = 0xC01EC829, version = 150)
    public int sceVideocodecOpen(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=384, usage=Usage.inout) TPointer buffer, int type) {
    	TPointer buffer2 = new TPointer(buffer.getMemory(), buffer.getValue32(16));

    	buffer.setValue32(0, 0x05100601);

    	switch (type) {
    		case 0:
	        	buffer.setValue32(8, 1);
	        	buffer.setValue32(24, 0x3C2C);
	        	buffer.setValue32(32, 0x15C00);

	        	buffer2.setValue32(0, 0x1F6400);
	        	buffer2.setValue32(4, 0x15C00);
	        	break;
    		case 1:
    			buffer.setValue32(8, 0);
            	buffer.setValue32(24, 0x264C);
            	buffer.setValue32(32, 0xB69E3);
    			break;
			default:
	    		log.warn(String.format("sceVideocodecOpen unknown type %d", type));
	    		return -1;
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA2F0564E, version = 150)
    public int sceVideocodecStop(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=384, usage=Usage.inout) TPointer buffer, int type) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x17099F0A, version = 150)
    public int sceVideocodecInit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=384, usage=Usage.inout) TPointer buffer, int type) {
    	buffer.setValue32(12, buffer.getValue32(20) + 8);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D31F5B1, version = 150)
    public int sceVideocodecGetEDRAM(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=384, usage=Usage.inout) TPointer buffer, int type) {
    	int size = (buffer.getValue32(24) + 63) | 0x3F;
    	SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceVideocodecEDRAM", SysMemUserForUser.PSP_SMEM_Low, size, 0);
    	if (info == null) {
    		return -1;
    	}

    	int addrEDRAM = info.addr & EDRAM_MEMORY_MASK;
    	buffer.setValue32(20, alignUp(addrEDRAM, 63));
    	buffer.setValue32(92, addrEDRAM);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4F160BF4, version = 150)
    public int sceVideocodecReleaseEDRAM(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=384, usage=Usage.inout) TPointer buffer) {
    	buffer.setValue32(20, 0);
    	buffer.setValue32(92, 0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDBA273FA, version = 150)
    public int sceVideocodecDecode(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=384, usage=Usage.inout) TPointer buffer, int type) {
    	int mp4Data = buffer.getValue32(36);
    	int mp4Size = buffer.getValue32(40);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceVideocodecDecode mp4Data:%s", Utilities.getMemoryDump(mp4Data, mp4Size)));
    	}

    	int frameWidth = 368;
    	int frameHeight = 208;
    	int frameBufferWidthY = 400;
    	int frameBufferWidthCr = 200;
    	int frameBufferWidthCb = 200;

    	if (videoCodec == null) {
    		videoCodec = CodecFactory.getVideoCodec();
    		videoCodec.init(null);
    	}

    	if (memoryInfo == null) {
    		int sizeY = frameBufferWidthY * frameHeight;
    		int sizeCr = frameBufferWidthCr * (frameHeight / 2);
    		int sizeCb = frameBufferWidthCr * (frameHeight / 2);
    		int size = (sizeY + sizeCr + sizeCb) * 2;

    		memoryInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceVideocodecDecode", SysMemUserForUser.PSP_SMEM_Low, size, 0);

    		bufferY1 = memoryInfo.addr & EDRAM_MEMORY_MASK;
    		bufferCr1 = bufferY1 + sizeY;
    		bufferCb1 = bufferCr1 + sizeCr;
    		bufferY2 = bufferCb1 + sizeCb;
    		bufferCr2 = bufferY2 + sizeY;
    		bufferCb2 = bufferCr2 + sizeCr;
    	}

//    	int bufferY = 0x48BD0; // size 0x16440 (400x228)
//    	int bufferCr = 0x5F010; // size 0x5E00 (200x120+0x40)
//    	int bufferCb = 0x64E10; // size 0x7100 (200x144+0x80)
//    	if ((frameCount % 2) == 1) {
//    		bufferY = 0x6BF10; // size 0x16440
//    		bufferCr = 0x82350; // size 0x5E00
//    		bufferCb = 0x88150; // 
//    	}
    	boolean buffer1 = (frameCount & 1) == 0;
    	int bufferY = buffer1 ? bufferY1 : bufferY2;
    	int bufferCr = buffer1 ? bufferCr1 : bufferCr2;
    	int bufferCb = buffer1 ? bufferCb1 : bufferCb2;

    	int[] mp4Buffer = getIntBuffer(mp4Size);
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(mp4Data, mp4Size, 1);
    	for (int i = 0; i < mp4Size; i++) {
    		mp4Buffer[i] = memoryReader.readNext();
    	}

    	int result = videoCodec.decode(mp4Buffer, 0, mp4Size);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceVideocodecDecode videoCodec returned 0x%X", result));
    	}

    	releaseIntBuffer(mp4Buffer);

    	Memory mem = buffer.getMemory();
    	mem.memset(bufferY | MemoryMap.START_RAM, (byte) 0x80, frameBufferWidthY * frameHeight);
    	mem.memset(bufferCr | MemoryMap.START_RAM, (byte) (buffer1 ? 0x50 : 0x80), frameBufferWidthCr * (frameHeight / 2));
    	mem.memset(bufferCb | MemoryMap.START_RAM, (byte) 0x80, frameBufferWidthCb * (frameHeight / 2));

    	buffer.setValue32(8, 0);

    	TPointer buffer2 = new TPointer(buffer.getMemory(), buffer.getValue32(16));
    	buffer2.setValue32(0, mp4Data);
    	buffer2.setValue32(4, mp4Size);
    	buffer2.setValue32(8, buffer.getValue32(56));
    	buffer2.setValue32(12, 0x40);
    	buffer2.setValue32(16, 0);
    	buffer2.setValue32(44, mp4Size);
    	buffer2.setValue32(48, frameWidth);
    	buffer2.setValue32(52, frameHeight);
    	buffer2.setValue32(60, frameCount == 0 ? 1 : 2);
    	buffer2.setValue32(64, 1);
    	buffer2.setValue32(72, -1);
    	buffer2.setValue32(76, frameCount * 0x64);
    	buffer2.setValue32(80, 2997);
    	buffer2.setValue32(84, bufferY);
    	buffer2.setValue32(88, bufferCr);
    	buffer2.setValue32(92, bufferCb);
    	buffer2.setValue32(96, frameBufferWidthY);
    	buffer2.setValue32(100, frameBufferWidthCr);
    	buffer2.setValue32(104, frameBufferWidthCb);

    	frameCount++;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x17CF7D2C, version = 150)
    public int sceVideocodecGetFrameCrop() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x26927D19, version = 150)
    public int sceVideocodecGetVersion() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2F385E7F, version = 150)
    public int sceVideocodecScanHeader() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x307E6E1C, version = 150)
    public int sceVideocodecDelete() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x627B7D42, version = 150)
    public int sceVideocodecGetSEI() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x745A7B7A, version = 150)
    public int sceVideocodecSetMemory() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x893B32B1, version = 150)
    public int sceVideocodec_893B32B1() {
    	return 0;
    }
}
