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

import static jpcsp.util.Utilities.endianSwap32;

import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.util.Utilities;

public class scePaf extends HLEModule {
    public static Logger log = Modules.getLogger("scePaf");
    private HashMap<Integer, SysMemInfo> allocated = new HashMap<Integer, SysMemInfo>();
    private static final int libpngInfoStructSize = 0xFC;
    private static final int libpngReadStructSize = 0x620;
    private static final int libpngIEND = chunkType("IEND");
    private static final int libpngIHDR = chunkType("IHDR");
    private static final int libpngIDAT = chunkType("IDAT");
    private static final int libpngsRGB = chunkType("sRGB");
    private static final int libpnggAMA = chunkType("gAMA");
    private static final int libpngpHYs = chunkType("pHYs");
    private static final int libpngtEXt = chunkType("tEXt");
    private static final int libpngiTXt = chunkType("iTXt");
    private LibPngReadState libPngReadState;

    protected static class LibPngReadState {
    	public int readDataFunction;
    	public int imageWidth;
    	public int imageHeight;
    	public int bitDepth;
    	public int colorType;
    	public int compressionMethod;
    	public int filterMethod;
    	public int interlaceMethod;
    }

    private static int chunkType(String name) {
    	byte[] bytes = name.getBytes();
    	return Utilities.readUnaligned32(bytes, 0);
    }

    private static String chunkName(int chunkType) {
    	byte[] bytes = new byte[4];
    	Utilities.writeUnaligned32(bytes, 0, chunkType);
    	return new String(bytes);
    }

    @HLEFunction(nid = 0xA138A376, version = 660)
    public int scePaf_A138A376_sprintf(CpuState cpu, TPointer buffer, String format) {
    	return Modules.SysclibForKernelModule.sprintf(cpu, buffer, format);
    }

    @HLEFunction(nid = 0x0FCDFA1E, version = 150)
    public int scePaf_0FCDFA1E_malloc(int size) {
    	SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "scePaf_0FCDFA1E", SysMemUserForUser.PSP_SMEM_Low, size, 0);
    	if (sysMemInfo == null) {
    		return 0;
    	}

    	int addr = sysMemInfo.addr;
    	allocated.put(addr, sysMemInfo);

    	return addr;
    }

    @HLEFunction(nid = 0xB4652CFE, version = 150)
    public int scePaf_B4652CFE_memcpy(@CanBeNull TPointer destAddr, TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.memcpy(destAddr, srcAddr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x003B3F87, version = 150)
    public int scePaf_003B3F87() {
    	return 0;
    }

    @HLEFunction(nid = 0x0C2CD696, version = 150)
    public int scePaf_0C2CD696_malloc(int alignment, int size) {
    	SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "scePaf_0FCDFA1E", SysMemUserForUser.PSP_SMEM_LowAligned, size, alignment);
    	if (sysMemInfo == null) {
    		return 0;
    	}

    	int addr = sysMemInfo.addr;
    	allocated.put(addr, sysMemInfo);

    	return addr;
    }

    @HLEFunction(nid = 0x1F02DD65, version = 150)
    public int scePaf_1F02DD65_strncpy(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer destAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.strncpy(destAddr, srcAddr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x20BEF384, version = 150)
    public int scePaf_20BEF384() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x22420CC7, version = 150)
    public int scePaf_22420CC7() {
    	return 0;
    }

    @HLEFunction(nid = 0x3C4BC2CD, version = 150)
    public int scePaf_3C4BC2CD_strtol(@CanBeNull PspString string, @CanBeNull TPointer32 endString, int base) {
    	return Modules.SysclibForKernelModule.strtol(string, endString, base);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x65169E51, version = 150)
    public int scePaf_65169E51() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x66CCA794, version = 150)
    public int scePaf_66CCA794() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D55F3F5, version = 150)
    public int scePaf_6D55F3F5() {
    	return 0;
    }

    @HLEFunction(nid = 0x706ABBFF, version = 150)
    public int scePaf_706ABBFF_strncpy(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer destAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.strncpy(destAddr, srcAddr, size);
    }

    @HLEFunction(nid = 0x71B92320, version = 150)
    public void scePaf_71B92320_free(TPointer address) {
    	SysMemInfo sysMemInfo = allocated.remove(address.getAddress());
    	if (sysMemInfo == null) {
    		return;
    	}
    	Modules.SysMemUserForUserModule.free(sysMemInfo);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B7133D5, version = 150)
    public int scePaf_7B7133D5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8166CA82, version = 150)
    public int scePaf_8166CA82() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8E805192, version = 150)
    public int scePaf_8E805192() {
    	return 0;
    }

    @HLEFunction(nid = 0x9A418CCC, version = 150)
    public int scePaf_9A418CCC_memcpy(@CanBeNull TPointer destAddr, TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.memcpy(destAddr, srcAddr, size);
    }

    @HLEFunction(nid = 0x9CD6C5F4, version = 150)
    public int scePaf_9CD6C5F4_memcmp(@BufferInfo(lengthInfo = LengthInfo.nextNextParameter, usage = Usage.in) TPointer src1Addr, @BufferInfo(lengthInfo = LengthInfo.nextParameter, usage = Usage.in) TPointer src2Addr, int size) {
    	return Modules.SysclibForKernelModule.memcmp(src1Addr, src2Addr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9D854500, version = 150)
    public int scePaf_9D854500() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA631AC8B, version = 150)
    public int scePaf_A631AC8B() {
    	return 0;
    }

    @HLEFunction(nid = 0xB05D9677, version = 150)
    public int scePaf_B05D9677_memcmp(@BufferInfo(lengthInfo = LengthInfo.nextNextParameter, usage = Usage.in) TPointer src1Addr, @BufferInfo(lengthInfo = LengthInfo.nextParameter, usage = Usage.in) TPointer src2Addr, int size) {
    	return Modules.SysclibForKernelModule.memcmp(src1Addr, src2Addr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB110AF46, version = 150)
    public int scePaf_B110AF46() {
    	return 0;
    }

    @HLEFunction(nid = 0xBB89C9EA, version = 150)
    public int scePaf_BB89C9EA_memset(@CanBeNull TPointer destAddr, int data, int size) {
    	return Modules.SysclibForKernelModule.memset(destAddr, data, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD229572C, version = 150)
    public int scePaf_D229572C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD7DCB972, version = 150)
    public int scePaf_D7DCB972() {
    	return 0;
    }

    @HLEFunction(nid = 0xD9E2D6E1, version = 150)
    public int scePaf_D9E2D6E1_memset(@CanBeNull TPointer destAddr, int data, int size) {
    	return Modules.SysclibForKernelModule.memset(destAddr, data, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE0B32AE8, version = 150)
    public int scePaf_E0B32AE8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE49835DC, version = 150)
    public int scePaf_E49835DC() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFF9C876B, version = 150)
    public int scePaf_FF9C876B() {
    	return 0;
    }

    /*
     * From libpng 1.2.45: png_create_read_struct
     *
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x8C5CC663, version = 150)
    public int scePaf_8C5CC663_png_create_read_struct(PspString user_png_ver, int error_ptr, int error_fn, int warn_fn) {
    	SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "libpngReadStruct", SysMemUserForUser.PSP_SMEM_Low, libpngReadStructSize, 0);
    	TPointer readStructAddr = new TPointer(getMemory(), sysMemInfo.addr);
    	readStructAddr.clear(libpngReadStructSize);
    	libPngReadState = new LibPngReadState();
    	return readStructAddr.getAddress();
    }

    /*
     * From libpng 1.2.45: png_create_info_struct
     *
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x43759F51, version = 150)
    public int scePaf_43759F51_png_create_info_struct(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = libpngReadStructSize, usage = Usage.inout) TPointer png_ptr) {
    	SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "libpngInfoStruct", SysMemUserForUser.PSP_SMEM_Low, libpngInfoStructSize, 0);
    	TPointer infoStructAddr = new TPointer(getMemory(), sysMemInfo.addr);
    	infoStructAddr.clear(libpngInfoStructSize);
    	return infoStructAddr.getAddress();
    }

    /*
     * From libpng 1.2.45: png_set_read_fn
     *
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x5A12583F, version = 150)
    public void scePaf_5A12583F_png_set_read_fn(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = libpngReadStructSize, usage = Usage.inout) TPointer png_ptr, int io_ptr, TPointer read_data_fn) {
    	libPngReadState.readDataFunction = read_data_fn.getAddress();
    }

    /*
     * From libpng 1.2.45: png_read_info
     *
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x4E43A742, version = 150)
    public void scePaf_4E43A742_png_read_info(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = libpngReadStructSize, usage = Usage.inout) TPointer png_ptr, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = libpngInfoStructSize, usage = Usage.inout) TPointer info_ptr) {
    	SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
    	TPointer buffer8Addr = new TPointer(png_ptr);
    	TPointer bufferAddr = new TPointer(png_ptr, 8);
    	Modules.ThreadManForUserModule.executeCallback(thread, libPngReadState.readDataFunction, null, true, png_ptr.getAddress(), bufferAddr.getAddress(), 8);
    	log.debug(String.format("PNG header: %s", Utilities.getMemoryDump(bufferAddr, 8)));
    	while (true) {
    		// Read one chunk

    		// Read length & type
        	Modules.ThreadManForUserModule.executeCallback(thread, libPngReadState.readDataFunction, null, true, png_ptr.getAddress(), buffer8Addr.getAddress(), 8);
        	int chunkLength = endianSwap32(buffer8Addr.getValue32(0));
        	int chunkType = buffer8Addr.getValue32(4);

        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Chunk '%s'(0x%08X), length=0x%X", chunkName(chunkType), chunkType, chunkLength));
        	}

        	if (chunkType == libpngIDAT || chunkType == libpngIEND) {
        		break;
        	}

        	// Read data
    		Modules.ThreadManForUserModule.executeCallback(thread, libPngReadState.readDataFunction, null, true, png_ptr.getAddress(), bufferAddr.getAddress(), chunkLength);

    		// Read CRC
    		Modules.ThreadManForUserModule.executeCallback(thread, libPngReadState.readDataFunction, null, true, png_ptr.getAddress(), buffer8Addr.getAddress(), 4);
        	int chunkCrc = buffer8Addr.getUnalignedValue32(chunkLength);

        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Chunk crc=0x%08X", chunkCrc));
        		if (log.isTraceEnabled()) {
        			log.trace(String.format("Chunk data: %s", Utilities.getMemoryDump(bufferAddr, chunkLength)));
        		}
        	}

        	if (chunkType == libpngIHDR) {
        		libPngReadState.imageWidth = endianSwap32(bufferAddr.getValue32(0));
        		libPngReadState.imageHeight = endianSwap32(bufferAddr.getValue32(4));
        		libPngReadState.bitDepth = bufferAddr.getUnsignedValue8(8);
        		libPngReadState.colorType = bufferAddr.getUnsignedValue8(9);
        		libPngReadState.compressionMethod = bufferAddr.getUnsignedValue8(10);
        		libPngReadState.filterMethod = bufferAddr.getUnsignedValue8(11);
        		libPngReadState.interlaceMethod = bufferAddr.getUnsignedValue8(12);
        	} else if (chunkType == libpngsRGB) {
        		// Ignore
        	} else if (chunkType == libpnggAMA) {
        		// Ignore
        	} else if (chunkType == libpngpHYs) {
        		// Ignore
        	} else if (chunkType == libpngtEXt) {
        		// Ignore
        	} else if (chunkType == libpngiTXt) {
        		// Ignore
        	} else {
				log.error(String.format("libpng: unknown chunkType='%s'(0x%08X)", chunkName(chunkType), chunkType));
        	}
    	}
    }

    /*
     * From libpng 1.2.45: png_get_IHDR
     *
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x3E564415, version = 150)
    public int scePaf_3E564415_png_get_IHDR(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = libpngReadStructSize, usage = Usage.in) TPointer png_ptr, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = libpngInfoStructSize, usage = Usage.in) TPointer info_ptr, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 widthAddr, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 heightAddr, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 bitDepthAddr, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 colorTypeAddr, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 interlaceTypeAddr, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 compressionTypeAddr, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 filterTypeAddr) {
    	widthAddr.setValue(libPngReadState.imageWidth);
    	heightAddr.setValue(libPngReadState.imageHeight);
    	bitDepthAddr.setValue(libPngReadState.bitDepth);
    	colorTypeAddr.setValue(libPngReadState.colorType);
    	compressionTypeAddr.setValue(libPngReadState.compressionMethod);
    	filterTypeAddr.setValue(libPngReadState.filterMethod);
    	interlaceTypeAddr.setValue(libPngReadState.interlaceMethod);

    	return 1;
    }
}
