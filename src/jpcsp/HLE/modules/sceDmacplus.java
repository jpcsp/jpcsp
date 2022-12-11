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

import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_INCREMENT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_LENGTH_SHIFT_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_STEP_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_LENGTH;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_INCREMENT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_LENGTH_SHIFT_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_STEP_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_UNKNOWN2;
import static jpcsp.util.Utilities.hasFlag;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.kernel.types.SceMp4AvcCscStruct;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.graphics.GeCommands;

public class sceDmacplus extends HLEModule {
    public static Logger log = Modules.getLogger("sceDmacplus");
    public static final int pixelFormatFromCode[] = {
    		GeCommands.PSM_32BIT_ABGR8888,
    		GeCommands.PSM_16BIT_BGR5650,
    		GeCommands.PSM_16BIT_ABGR5551,
    		GeCommands.PSM_16BIT_ABGR4444
    };
    private int pixelMode;
    private int bufferWidth;

    @HLEUnimplemented
	@HLEFunction(nid = 0xE9B746F9, version = 150)
	public int sceDmacplusLcdcDisable() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xED849260, version = 150)
	public int sceDmacplusLcdcEnable() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x88ACB6F1, version = 150)
	public int sceDmacplusLcdcSetFormat(int displayWidth, int displayFrameBufferWidth, int displayPixelFormatCoded) {
    	int pixelFormat = pixelFormatFromCode[displayPixelFormatCoded];
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceDmacplusLcdcSetFormat pixelFormat=%d", pixelFormat));
    	}
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xA3AA8D00, version = 150)
	public int sceDmacplusLcdcSetBaseAddr(int frameBufferAddress) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2D5940FF, version = 150)
	@HLEFunction(nid = 0x6945F1D3, version = 660)
	public int sceDmacplusMe2ScLLI(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer32 dmacParameters) {
		return 0;
	}

	@HLEFunction(nid = 0x3438DA0B, version = 150)
	@HLEFunction(nid = 0x282CA0D7, version = 660)
	public int sceDmacplusSc2MeLLI(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer32 dmacParameters) {
		do {
	    	int src = dmacParameters.getValue(0);
	    	int dst = dmacParameters.getValue(4);
	    	int next = dmacParameters.getValue(8);
	    	int attributes = dmacParameters.getValue(12);

			if (log.isDebugEnabled()) {
	    		log.debug(String.format("sceDmacplusSc2MeLLI src=0x%08X, dst=0x%08X, next=0x%08X, attributes=0x%X", src, dst, next, attributes));
	    	}

			int srcStep = (attributes >> DMAC_ATTRIBUTES_SRC_STEP_SHIFT) & 0x7;
			int dstStep = (attributes >> DMAC_ATTRIBUTES_DST_STEP_SHIFT) & 0x7;
			int srcLengthShift = (attributes >> DMAC_ATTRIBUTES_SRC_LENGTH_SHIFT_SHIFT) & 0x7;
			int dstLengthShift = (attributes >> DMAC_ATTRIBUTES_DST_LENGTH_SHIFT_SHIFT) & 0x7;
			boolean srcIncrement = hasFlag(attributes, DMAC_ATTRIBUTES_SRC_INCREMENT);
			boolean dstIncrement = hasFlag(attributes, DMAC_ATTRIBUTES_DST_INCREMENT);
			int length = attributes & DMAC_ATTRIBUTES_LENGTH;

			if (hasFlag(attributes, DMAC_ATTRIBUTES_UNKNOWN2)) {
				// It seems to completely ignore the other attribute values
				srcIncrement = true;
				dstIncrement = true;
				srcLengthShift = 0;
				dstLengthShift = 0;
			}

			if (!srcIncrement || !dstIncrement || srcStep != 1 || dstStep != 1) {
				log.error(String.format("sceDmacplusSc2MeLLI unimplemented srcIncrement=%b, dstIncrement=%b, srcStep=%d, dstStep=%d", srcIncrement, dstIncrement, srcStep, dstStep));
				return -1;
			}

			int srcLength = length << srcLengthShift;
			int dstLength = length << dstLengthShift;

			TPointer memcpyDst = new TPointer(getMEMemory(), dst);
			TPointer memcpySrc = new TPointer(getMemory(), src);
			int memcpyLength = Math.min(srcLength, dstLength);

			if (log.isDebugEnabled()) {
				log.debug(String.format("sceDmacplusSc2MeLLI memcpy dst=%s, src=%s, length=0x%X", memcpyDst, memcpySrc, memcpyLength));
			}
			memcpyDst.memcpy(memcpySrc, memcpyLength);

			dmacParameters = new TPointer32(dmacParameters.getMemory(), next);
		} while (dmacParameters.isNotNull());

    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xB269EAC9, version = 660)
	public int sceDmacplus_driver_B269EAC9(int internalPixelMode, int bufferWidth, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=22, usage=Usage.in) TPointer unknown3) {
    	this.pixelMode = sceMpegbase.getPixelMode(internalPixelMode);
    	this.bufferWidth = bufferWidth;

    	return 0;
	}

    // Called by sceMpegBaseCscAvc
    @HLEUnimplemented
	@HLEFunction(nid = 0xD126494B, version = 660)
	public int sceDmacplus_driver_D126494B(TPointer bufferRGB, @CanBeNull TPointer unknown, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.in) TPointer mp4AvcCscStructAddr) {
        SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
    	mp4AvcCscStruct.read(mp4AvcCscStructAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceDmacplus_driver_D126494B %s", mp4AvcCscStruct));
    	}

    	return Modules.sceMpegbaseModule.hleMpegBaseCscAvc(bufferRGB, unknown, bufferWidth, pixelMode, mp4AvcCscStruct);
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x58DE4914, version = 150)
	@HLEFunction(nid = 0x47D400CB, version = 660)
	public int sceDmacplusSc2MeSync(boolean poll, @CanBeNull @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 16, usage = Usage.out) TPointer32 result) {
    	if (result.isNotNull()) {
    		// 4 unknown values
	    	result.setValue(0, 0);
	    	result.setValue(4, 0);
	    	result.setValue(8, 0);
	    	result.setValue(12, 0);
    	}

    	return 0;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0xAB49D2CB, version = 150)
	@HLEFunction(nid = 0x5FCF43BD, version = 660)
	public int sceDmacplusMe2ScSync(boolean poll, @CanBeNull @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 16, usage = Usage.out) TPointer32 result) {
    	if (result.isNotNull()) {
    		// 4 unknown values
	    	result.setValue(0, 0);
	    	result.setValue(4, 0);
	    	result.setValue(8, 0);
	    	result.setValue(12, 0);
    	}

    	return 0;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0x3A98EE05, version = 150)
	@HLEFunction(nid = 0xBF0DB45E, version = 660)
	public int sceDmacplusAvcSync(boolean poll, @CanBeNull @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 48, usage = Usage.out) TPointer32 result) {
    	if (result.isNotNull()) {
    		// 12 unknown values
	    	result.setValue(0, 0);
	    	result.setValue(4, 0);
	    	result.setValue(8, 0);
	    	result.setValue(12, 0);
	    	result.setValue(16, 0);
	    	result.setValue(20, 0);
	    	result.setValue(24, 0);
	    	result.setValue(28, 0);
	    	result.setValue(32, 0);
	    	result.setValue(36, 0);
	    	result.setValue(40, 0);
	    	result.setValue(44, 0);
    	}

    	return 0;
    }
}
