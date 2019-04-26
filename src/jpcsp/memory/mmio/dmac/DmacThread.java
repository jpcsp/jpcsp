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
package jpcsp.memory.mmio.dmac;

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.memory.mmio.MMIO.normalizeAddress;
import static jpcsp.memory.mmio.MMIOHandlerDdr.DDR_FLUSH_DMAC;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_INCREMENT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_LENGTH_SHIFT_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_DST_STEP_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_LENGTH;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_INCREMENT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_LENGTH_SHIFT_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_SRC_STEP_SHIFT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_TRIGGER_INTERRUPT;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_ATTRIBUTES_UNKNOWN;
import static jpcsp.memory.mmio.dmac.DmacProcessor.DMAC_STATUS_REQUIRES_DDR;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.memory.mmio.MMIOHandlerDdr;
import jpcsp.memory.mmio.MMIOHandlerDmac;

public class DmacThread extends Thread {
	private static Logger log = MMIOHandlerDmac.log;
	private static final int DMAC_MEMCPY_STEP2 = 0;
	private static final int DMAC_MEMCPY_STEP16 = 1;
	private static final int DMAC_MEMCPY_STEP8 = 2;
	private static final int DMAC_MEMCPY_STEP4 = 3;
	private static final int dmacMemcpyStepLength[] = new int[8];
	private final Semaphore job = new Semaphore(0);
	private final Semaphore trigger = new Semaphore(0);
	private final DmacProcessor dmacProcessor;
	private volatile Memory memSrc;
	private volatile Memory memDst;
	private volatile int src;
	private volatile int dst;
	private volatile int next;
	private volatile int attributes;
	private volatile int status;
	private volatile IAction interruptAction;
	private volatile IAction completedAction;
	private volatile boolean exit;
	private volatile boolean abortJob;

	public DmacThread(DmacProcessor dmacProcessor) {
		this.dmacProcessor = dmacProcessor;

		dmacMemcpyStepLength[DMAC_MEMCPY_STEP2] = 2;
		dmacMemcpyStepLength[DMAC_MEMCPY_STEP4] = 4;
		dmacMemcpyStepLength[DMAC_MEMCPY_STEP8] = 8;
		dmacMemcpyStepLength[DMAC_MEMCPY_STEP16] = 16;
	}

	@Override
	public void run() {
		setLog4jMDC();

		while (!exit) {
			try {
				job.acquire();
				if (!exit) {
					dmacMemcpy();
				}
			} catch (InterruptedException e) {
				// Ignore exception
			}
		}
	}

	public void exit() {
		exit = true;
	}

	public void execute(Memory memDst, Memory memSrc, int dst, int src, int next, int attributes, int status, IAction interruptAction, IAction completedAction) {
		abortJob = false;
		this.memSrc = memSrc;
		this.memDst = memDst;
		this.dst = dst;
		this.src = src;
		this.next = next;
		this.attributes = attributes;
		this.status = status;
		this.interruptAction = interruptAction;
		this.completedAction = completedAction;

		job.release();
	}

	public void abortJob() {
		abortJob = true;

		trigger.release();
	}

	private void dmacMemcpy(int dst, int src, int dstLength, int srcLength, int dstStepLength, int srcStepLength, boolean dstIncrement, boolean srcIncrement) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("dmacMemcpy dst=0x%08X, src=0x%08X, dstLength=0x%X, srcLength=0x%X, dstStepLength=%d, srcStepLength=%d, dstIncrement=%b, srcIncrement=%b", dst, src, dstLength, srcLength, dstStepLength, srcStepLength, dstIncrement, srcIncrement));
		}

		final int srcStep4  = srcIncrement ?  4 : 0;
		final int srcStep8  = srcIncrement ?  8 : 0;
		final int srcStep12 = srcIncrement ? 12 : 0;
		final int dstStep4  = dstIncrement ?  4 : 0;
		final int dstStep8  = dstIncrement ?  8 : 0;
		final int dstStep12 = dstIncrement ? 12 : 0;

		final int stepLength = Math.min(srcStepLength, dstStepLength);

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

	private boolean dmacMemcpyStep() {
		if (abortJob) {
			return false;
		}

		int srcStep = (attributes >> DMAC_ATTRIBUTES_SRC_STEP_SHIFT) & 0x7;
		int dstStep = (attributes >> DMAC_ATTRIBUTES_DST_STEP_SHIFT) & 0x7;
		int srcLengthShift = (attributes >> DMAC_ATTRIBUTES_SRC_LENGTH_SHIFT_SHIFT) & 0x7;
		int dstLengthShift = (attributes >> DMAC_ATTRIBUTES_DST_LENGTH_SHIFT_SHIFT) & 0x7;
		boolean srcIncrement = (attributes & DMAC_ATTRIBUTES_SRC_INCREMENT) != 0;
		boolean dstIncrement = (attributes & DMAC_ATTRIBUTES_DST_INCREMENT) != 0;
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

//		if (srcStepLength != dstStepLength) {
//			log.error(String.format("dmacMemcpy with different steps: srcStepLength=%d, dstSteplength=%d, dst=0x%08X, src=0x%08X, attr=0x%X, dstLength=0x%X(shift=%d), srcLength=0x%X(shift=%d)", srcStepLength, dstStepLength, dst, src, attributes, length << dstLengthShift, dstLengthShift, length << srcLengthShift, srcLengthShift));
//			return false;
//		}

		// TODO Not sure about the real meaning of this attribute flag...
		if ((attributes & DMAC_ATTRIBUTES_UNKNOWN) != 0) {
			// It seems to completely ignore the other attribute values
			srcIncrement = true;
			dstIncrement = true;
			srcStepLength = 1;
			dstStepLength = 1;
			srcLengthShift = 0;
			dstLengthShift = 0;
		}

		int srcLength = length << srcLengthShift;
		int dstLength = length << dstLengthShift;
		if (srcLength != dstLength) {
			log.error(String.format("dmacMemcpy with different lengths: srcLength=0x%X, dstLength=0x%X", srcLength, dstLength));
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("dmacMemcpy dst=0x%08X, src=0x%08X, attr=0x%08X, dstLength=0x%X(shift=%d), srcLength=0x%X(shift=%d), dstStepLength=0x%X(step=%d), srcStepLength=0x%X(step=%d), dstIncrement=%b, srcIncrement=%b, next=0x%08X, status=0x%X", dst, src, attributes, dstLength, dstLengthShift, srcLength, srcLengthShift, dstStepLength, dstStep, srcStepLength, srcStep, dstIncrement, srcIncrement, next, status));
		}

		// Update the DMAC registers
		dmacProcessor.setSrc(src);
		dmacProcessor.setDst(dst);
		dmacProcessor.setAttributes(attributes);

		int normalizedSrc = normalizeAddress(src);
		int normalizedDst = normalizeAddress(dst);

		// Check for most common case which can be implemented through a simple memcpy
		if (srcIncrement && dstIncrement && memSrc == memDst) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("dmacMemcpy dst=0x%08X, src=0x%08X, length=0x%X", normalizedDst, normalizedSrc, srcLength));
			}

			memSrc.memcpy(normalizedDst, normalizedSrc, srcLength);
		} else {
			dmacMemcpy(normalizedDst, normalizedSrc, dstLength, srcLength, dstStepLength, srcStepLength, dstIncrement, srcIncrement);
		}

		// Update the DMAC registers
		if (length > 0) {
			if (srcIncrement) {
				// Increment the src address
				dmacProcessor.setSrc(src + ((length - 1) << srcLengthShift));
			}
			if (dstIncrement) {
				// Increment the dst address
				dmacProcessor.setDst(dst + ((length - 1) << dstLengthShift));
			}
			// Clear the length field
			dmacProcessor.setAttributes(attributes & 0xFFFFF000);
		}

		// Trigger an interrupt if requested in the attributes
		if ((attributes & DMAC_ATTRIBUTES_TRIGGER_INTERRUPT) != 0) {
			if (interruptAction != null) {
				interruptAction.execute();
			}
		}

		return true;
	}

	private void checkTrigger() {
		if (MMIOHandlerDdr.getInstance().checkAndClearFlushDone(DDR_FLUSH_DMAC)) {
			trigger.release();
		}
	}

	private boolean waitForTrigger() {
		if (abortJob) {
			return false;
		}

		if (trigger.tryAcquire()) {
			return true;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("waitForTrigger starting"));
		}

		boolean acquired = false;
		while (!acquired && !abortJob) {
			try {
				trigger.acquire();
				acquired = true;
			} catch (InterruptedException e) {
				// Ignore exception
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("waitForTrigger done acquired=%b", acquired));
		}

		return acquired;
	}

	private void dmacMemcpy() {
		boolean waitForTrigger = false;
		IAction dmacDdrFlushAction = null;

		if ((status & DMAC_STATUS_REQUIRES_DDR) != 0) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("dmacMemcpy requiring a call to sceDdrFlush(0x%X), dst=0x%08X, src=0x%08X, attr=0x%08X, next=0x%08X, status=0x%X", DDR_FLUSH_DMAC, dst, src, attributes, next, status));
			}
			waitForTrigger = true;
			dmacDdrFlushAction = new DmacDdrFlushAction(this);
			MMIOHandlerDdr.getInstance().setFlushAction(DDR_FLUSH_DMAC, dmacDdrFlushAction);
			checkTrigger();

			if (!waitForTrigger()) {
				return;
			}
		}

		if (dmacMemcpyStep()) {
			while (next != 0 && !abortJob) {
				src = memSrc.read32(next + 0);
				dst = memSrc.read32(next + 4);
				attributes = memSrc.read32(next + 12);

				if (!dmacMemcpyStep()) {
					break;
				}

				if (waitForTrigger) {
					while (memSrc.read32(next + 8) == 0) {
						if (!waitForTrigger()) {
							break;
						}
					}
				}

				next = memSrc.read32(next + 8);
				dmacProcessor.setNext(next);
			}
		}

		if (dmacDdrFlushAction != null) {
			MMIOHandlerDdr.getInstance().clearFlushAction(DDR_FLUSH_DMAC, dmacDdrFlushAction);
			trigger.drainPermits();
		}

		if (completedAction != null) {
			completedAction.execute();
		}
	}

	public void ddrFlushDone() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("dmacMemcpy sceDdrFlush(0x%X) called", DDR_FLUSH_DMAC));
		}

		checkTrigger();
	}
}
