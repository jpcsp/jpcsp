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
package jpcsp.memory.mmio.wlan.threadx.hle;

import static jpcsp.arm.ARMProcessor.CPSR_BIT_T;
import static jpcsp.arm.ARMProcessor.NUMBER_REGISTERS;
import static jpcsp.arm.ARMProcessor.REG_PC;
import static jpcsp.arm.ARMProcessor.REG_R0;
import static jpcsp.arm.ARMProcessor.REG_SP;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_BLOCK_MEMORY;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_BYTE_MEMORY;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_EVENT_FLAG;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_MUTEX_SUSP;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_QUEUE_SUSP;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_SEMAPHORE_SUSP;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_SLEEP;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.TX_SUSPENDED;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.setBit;

/**
 * @author gid15
 *
 */
public class TXThread {
	public static final int SIZEOF = 0x94;
	public int threadPtr;
	public String threadName;
	public int entryFunction;
	public int entryInput;
	public int stackStart;
	public int stackSize;
	public int priority;
	public int preemptyThreshold;
	public int timeSlice;
	public int state = TX_SUSPENDED;
	public int[] savedRegisters = new int[NUMBER_REGISTERS];
	public int savedCpsr;
	public int waitObjectPtr;
	public int waitOption;
	public int waitStartTicks;
	public int waitEventFlagsRequestedFlags;
	public int waitEventFlagsGetOption;
	public int waitEventFlagsActualFlagsPtr;
	public boolean waitQueueReceive;
	public int waitQueueDestinationPtr;
	public int waitQueueSourcePtr;

	public void initRegisters() {
		savedCpsr = 0x10; // MODE_USER
		savedRegisters[REG_R0] = entryInput;
		savedRegisters[REG_SP] = stackStart + stackSize;
		savedRegisters[REG_PC] = clearBit(entryFunction, 0);

		// Thumb mode?
		if (hasBit(entryFunction, 0)) {
			savedCpsr = setBit(savedCpsr, CPSR_BIT_T);
		}
	}

	public boolean isWaiting() {
		switch (state) {
			case TX_SLEEP:
			case TX_QUEUE_SUSP:
			case TX_SEMAPHORE_SUSP:
			case TX_EVENT_FLAG:
			case TX_BLOCK_MEMORY:
			case TX_BYTE_MEMORY:
			case TX_MUTEX_SUSP:
				return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return String.format("TXThread threadPtr=0x%08X, threadName='%s', entryFunction=0x%08X, priority=0x%X", threadPtr, threadName, entryFunction, priority);
	}
}
