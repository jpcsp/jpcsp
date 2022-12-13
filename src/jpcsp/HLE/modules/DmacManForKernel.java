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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_DMAC_INVALID_ARGUMENT;
import static jpcsp.memory.mmio.MMIO.normalizeAddress;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_INCREMENT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_LENGTH_SHIFT_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_STEP_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_LENGTH;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_INCREMENT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_LENGTH_SHIFT_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_STEP_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_UNKNOWN1;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_UNKNOWN2;
import static jpcsp.memory.mmio.dmac.DmacThread.dmacMemcpyStepLength;
import static jpcsp.util.Utilities.hasFlag;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Memory;
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
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;

public class DmacManForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("DmacManForKernel");
    private SysMemInfo sysMemInfo;
    private DmaOp firstOp;
    private Map<Integer, DmaOp> dmaOps = new HashMap<Integer, DmaOp>();

    protected static class DmaOp {
    	public static final int SIZEOF = 64;
    	private final TPointer addr;
    	private DmaOp next;
    	private TPointer callbackAddr;
    	private int callbackArgument;
    	private int status;
    	private TPointer dstAddress;
    	private TPointer srcAddress;
    	private int attributes;
    	private int unknown48;
    	private int unknown52;

    	public DmaOp(TPointer addr) {
			this.addr = new TPointer(addr);
		}

    	public TPointer getAddr() {
    		return addr;
    	}

    	public DmaOp getNext() {
    		return next;
    	}

    	public void setNext(DmaOp next) {
    		this.next = next;
    	}

		public int getUnknown48() {
			return unknown48;
		}

    	public void setUnknown48(int unknown48) {
    		this.unknown48 = unknown48;
    	}

		public int getUnknown52() {
			return unknown52;
		}

    	public void setUnknown52(int unknown52) {
    		this.unknown52 = unknown52;
    	}

		public void setCallback(TPointer callbackAddr, int callbackArgument) {
    		this.callbackAddr = callbackAddr;
    		this.callbackArgument = callbackArgument;
    	}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		public TPointer getDstAddress() {
			return dstAddress;
		}

		public void setDstAddress(TPointer dstAddress) {
			this.dstAddress = dstAddress;
		}

		public TPointer getSrcAddress() {
			return srcAddress;
		}

		public void setSrcAddress(TPointer srcAddress) {
			this.srcAddress = srcAddress;
		}

		public int getAttributes() {
			return attributes;
		}

		public void setAttributes(int attributes) {
			this.attributes = attributes;
		}

		public void callCallback() {
			if (callbackAddr != null && callbackAddr.isNotNull()) {
				Modules.ThreadManForUserModule.executeCallback(null, callbackAddr.getAddress(), null, false, addr.getAddress(), 0, 0, callbackArgument);
			}
		}

		public boolean execute() {
			int nextAddr = 0;
			int srcStep = (attributes >> DMAC_ATTRIBUTES_SRC_STEP_SHIFT) & 0x7;
			int dstStep = (attributes >> DMAC_ATTRIBUTES_DST_STEP_SHIFT) & 0x7;
			int srcLengthShift = (attributes >> DMAC_ATTRIBUTES_SRC_LENGTH_SHIFT_SHIFT) & 0x7;
			int dstLengthShift = (attributes >> DMAC_ATTRIBUTES_DST_LENGTH_SHIFT_SHIFT) & 0x7;
			boolean srcIncrement = hasFlag(attributes, DMAC_ATTRIBUTES_SRC_INCREMENT);
			boolean dstIncrement = hasFlag(attributes, DMAC_ATTRIBUTES_DST_INCREMENT);
			int length = attributes & DMAC_ATTRIBUTES_LENGTH;

			int srcStepLength = dmacMemcpyStepLength[srcStep];
			if (srcStepLength == 0) {
				log.error(String.format("dmacMemcpy with unknown srcStep=%d", srcStep));
				return false;
			}

			int dstStepLength = dmacMemcpyStepLength[dstStep];
			if (dstStepLength == 0) {
				log.error(String.format("dmacMemcpy with unknown dstStep=%d", dstStep));
				return false;
			}

			// TODO Not sure about the real meaning of this attribute flag...
			if (hasFlag(attributes, DMAC_ATTRIBUTES_UNKNOWN2)) {
				// It seems to completely ignore the other attribute values
				srcIncrement = true;
				dstIncrement = true;
				srcStepLength = 1;
				dstStepLength = 1;
				srcLengthShift = 0;
				dstLengthShift = 0;
			}

			// TODO Not sure about the real meaning of this attribute flag...
			if (hasFlag(attributes, DMAC_ATTRIBUTES_UNKNOWN1)) {
				// It seems to completely ignore the srcIncrement/dstIncrement attribute values.
				// It is used by sceDmacplus when copying ME memory to SC
				srcIncrement = true;
				dstIncrement = true;
			}

			int srcLength = length << srcLengthShift;
			int dstLength = length << dstLengthShift;
			if (srcLength != dstLength) {
				log.error(String.format("dmacMemcpy with different lengths: srcLength=0x%X, dstLength=0x%X", srcLength, dstLength));
				return false;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("dmacMemcpy dstAddress=%s, srcAddress=%s, attr=0x%08X, dstLength=0x%X(shift=%d), srcLength=0x%X(shift=%d), dstStepLength=0x%X(step=%d), srcStepLength=0x%X(step=%d), dstIncrement=%b, srcIncrement=%b, next=0x%08X, status=0x%X", dstAddress, srcAddress, attributes, dstLength, dstLengthShift, srcLength, srcLengthShift, dstStepLength, dstStep, srcStepLength, srcStep, dstIncrement, srcIncrement, nextAddr, status));
			}

			int normalizedSrc = normalizeAddress(srcAddress.getAddress());
			int normalizedDst = normalizeAddress(dstAddress.getAddress());

			Memory memSrc = srcAddress.getMemory();
			Memory memDst = dstAddress.getMemory();

			// Check for most common case which can be implemented through a simple memcpy
			if (srcIncrement && dstIncrement && memSrc == memDst) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("dmacMemcpy dst=0x%08X, src=0x%08X, length=0x%X", normalizedDst, normalizedSrc, srcLength));
				}

				memSrc.memcpy(normalizedDst, normalizedSrc, srcLength);
			} else {
				final int srcStep4  = srcIncrement ?  4 : 0;
				final int srcStep8  = srcIncrement ?  8 : 0;
				final int srcStep12 = srcIncrement ? 12 : 0;
				final int dstStep4  = dstIncrement ?  4 : 0;
				final int dstStep8  = dstIncrement ?  8 : 0;
				final int dstStep12 = dstIncrement ? 12 : 0;

				final int stepLength = Math.min(srcStepLength, dstStepLength);

				int dst = normalizedDst;
				int src = normalizedSrc;
				while (dstLength > 0 && srcLength > 0) {
					switch (stepLength) {
						case 1:
							if (log.isTraceEnabled()) {
								log.trace(String.format("memcpy dst=0x%08X, src=0x%08X, length=0x%X", dst, src, 1));
							}
							memDst.write8(dst, (byte) memSrc.read8(src));
							break;
						case 2:
							if (log.isTraceEnabled()) {
								log.trace(String.format("memcpy dst=0x%08X, src=0x%08X, length=0x%X", dst, src, 2));
							}
							memDst.write16(dst, (short) memSrc.read16(src));
							break;
						case 4:
							if (log.isTraceEnabled()) {
								log.trace(String.format("memcpy dst=0x%08X, src=0x%08X, length=0x%X", dst, src, 4));
							}
							memDst.write32(dst, memSrc.read32(src));
							break;
						case 8:
							if (log.isTraceEnabled()) {
								log.trace(String.format("memcpy dst=0x%08X, src=0x%08X, length=0x%X", dst, src, 8));
							}
							memDst.write32(dst, memSrc.read32(src));
							memDst.write32(dst + dstStep4, memSrc.read32(src + srcStep4));
							break;
						case 16:
							if (log.isTraceEnabled()) {
								log.trace(String.format("memcpy dst=0x%08X, src=0x%08X, length=0x%X", dst, src, 16));
							}
							memDst.write32(dst, memSrc.read32(src));
							memDst.write32(dst + dstStep4, memSrc.read32(src + srcStep4));
							memDst.write32(dst + dstStep8, memSrc.read32(src + srcStep8));
							memDst.write32(dst + dstStep12, memSrc.read32(src + srcStep12));
							break;
					}
					dstLength -= stepLength;
					srcLength -= stepLength;

					if (dstIncrement) {
						dst += stepLength;
					}
					if (srcIncrement) {
						src += stepLength;
					}
				}
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("DmaOp addr=%s", addr);
		}
    }

    @Override
	public void start() {
    	final int countOps = 32;

		sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "DmacManForKernel", SysMemUserForUser.PSP_SMEM_Low, countOps * DmaOp.SIZEOF, 0);

		TPointer currentOpAddr = new TPointer(getMemory(), sysMemInfo.addr & Memory.addressMask);
		DmaOp previousOp = null;
		for (int i = 0; i < countOps; i++) {
			DmaOp dmaOp = new DmaOp(currentOpAddr);
			if (log.isDebugEnabled()) {
				log.debug(String.format("Creating %s", currentOpAddr));
			}
			dmaOps.put(currentOpAddr.getAddress(), dmaOp);
			if (previousOp != null) {
				previousOp.setNext(dmaOp);
			} else {
				firstOp = dmaOp;
			}

			previousOp = dmaOp;
			currentOpAddr.add(DmaOp.SIZEOF);
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("firstOp %s", firstOp));
		}

		super.start();
	}

	@Override
	public void stop() {
		if (sysMemInfo != null) {
			Modules.SysMemUserForUserModule.free(sysMemInfo);
			sysMemInfo = null;
			firstOp = null;
		}

		super.stop();
	}

	private DmaOp getDmaOp(TPointer dmaOpAddr) {
		return dmaOps.get(dmaOpAddr.getAddress());
	}

	@HLEFunction(nid = 0x59615199, version = 150)
	public int sceKernelDmaOpAlloc() {
		if (firstOp == null) {
			return 0;
		}

		DmaOp dmaOp = firstOp;
		firstOp = dmaOp.getNext();

		return dmaOp.getAddr().getAddress();
	}

	@HLEFunction(nid = 0x745E19EF, version = 150)
	public int sceKernelDmaOpFree(@CanBeNull TPointer dmaOpAddr) {
    	DmaOp dmaOp = getDmaOp(dmaOpAddr);
    	if (dmaOp == null) {
    		return ERROR_KERNEL_DMAC_INVALID_ARGUMENT;
    	}

    	dmaOp.setNext(firstOp);
    	firstOp = dmaOp;

    	return 0;
	}

	@HLEFunction(nid = 0xF64BAB99, version = 150)
	public int sceKernelDmaOpAssign(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr, int unknown1, int unknown2, int unknown3) {
    	DmaOp dmaOp = getDmaOp(dmaOpAddr);
    	if (dmaOp == null) {
    		return ERROR_KERNEL_DMAC_INVALID_ARGUMENT;
    	}
    	dmaOp.setUnknown48(unknown3);
    	int unknown52 = (unknown1 & 0xFF) | ((unknown2 & 0xFF) << 8);
    	dmaOp.setUnknown52(unknown52);

    	return 0;
	}

	@HLEFunction(nid = 0x3BDEA96C, version = 150)
	public int sceKernelDmaOpEnQueue(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr) {
    	DmaOp dmaOp = getDmaOp(dmaOpAddr);
    	if (dmaOp == null) {
    		return ERROR_KERNEL_DMAC_INVALID_ARGUMENT;
    	}

    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x5AF32783, version = 150)
	public int sceKernelDmaOpQuit(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x92700CCD, version = 150)
	public int sceKernelDmaOpDeQueue(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr) {
    	return 0;
	}

	@HLEFunction(nid = 0xCE467D9B, version = 150)
	public int sceKernelDmaOpSetupNormal(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr, int status, TPointer dstAddress, TPointer srcAddress, int attributes) {
    	DmaOp dmaOp = getDmaOp(dmaOpAddr);
    	if (dmaOp == null) {
    		return ERROR_KERNEL_DMAC_INVALID_ARGUMENT;
    	}
    	dmaOp.setStatus(status);
    	dmaOp.setDstAddress(dstAddress);
    	dmaOp.setSrcAddress(srcAddress);
    	dmaOp.setAttributes(attributes);

    	return 0;
	}

	@HLEFunction(nid = 0xD0358BE9, version = 150)
	public int sceKernelDmaOpSetCallback(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr, TPointer callbackAddr, int callbackArgument) {
    	DmaOp dmaOp = getDmaOp(dmaOpAddr);
    	if (dmaOp == null) {
    		return ERROR_KERNEL_DMAC_INVALID_ARGUMENT;
    	}
    	dmaOp.setCallback(callbackAddr, callbackArgument);
    	
    	return 0;
	}

	@HLEFunction(nid = 0xDB286D65, version = 150)
	public int sceKernelDmaOpSync(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr, int waitType, int timeout) {
    	// waitType = 0: do not wait for completion of DMA Operation, return error when still running
    	// waitType = 1: wait indefinitely for completion of the DMA Operation
    	// waitType = 2: wait for given timeout for completion of the DMA Operation
    	DmaOp dmaOp = getDmaOp(dmaOpAddr);
    	if (dmaOp == null) {
    		return ERROR_KERNEL_DMAC_INVALID_ARGUMENT;
    	}
    	dmaOp.execute();
    	dmaOp.callCallback();

    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7D21A2EF, version = 150)
	public int sceKernelDmaOpSetupLink(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr, int status, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer32 linkStructure) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x3FAD5844, version = 150)
	public int sceKernelDmaOpSetupMemcpy(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=54, usage=Usage.inout) TPointer dmaOpAddr, TPointer dstAddress, TPointer srcAddress, int length) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x32757C57, version = 150)
	public int DmacManForKernel_32757C57(@CanBeNull TPointer setupLinkCallback) {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x1C46158A, version = 150)
	public int sceKernelDmaExit() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x1FC036B7, version = 150)
	public int DmacManForKernel_1FC036B7() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2E3BC333, version = 150)
	public int sceKernelDmaChReserve() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x7B9634E1, version = 150)
	public int sceKernelDmaSoftRequest() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x904110FC, version = 150)
	public int sceKernelDmaOpAssignMultiple() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xA84B084B, version = 150)
	public int sceKernelDmaOpAllCancel() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD3F62265, version = 150)
	public int sceKernelDmaOnDebugMode() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xD8BC3120, version = 150)
	public int sceKernelDmaChExclude() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xE18A93A5, version = 150)
	public int DmacManForKernel_E18A93A5() {
    	return 0;
	}
}
