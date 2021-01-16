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
package jpcsp.memory.mmio.wlan.threadx;

import jpcsp.arm.ARMProcessor;

/**
 * UINT tx_thread_create(TX_THREAD *thread_ptr,
 *                       CHAR *name_ptr,
 *                       VOID (*entry_function)(ULONG),
 *                       ULONG entry_input,
 *                       VOID *stack_start, 
 *                       ULONG stack_size,
 *                       UINT priority, 
 *                       UINT preempt_threshold,
 *                       ULONG time_slice, 
 *                       UINT auto_start)
 *
 * @author gid15
 *
 */
public class TXThreadCreate extends TXBaseCall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int threadPtr = getParameterValue(processor, 0);
		int namePtr = getParameterValue(processor, 1);
		int entryFunction = getParameterValue(processor, 2);
		int entryInput = getParameterValue(processor, 3);
		int stackStart = getParameterValue(processor, 4);
		int stackSize = getParameterValue(processor, 5);
		int priority = getParameterValue(processor, 6);
		int preemptThreshold = getParameterValue(processor, 7);
		int timeSlice = getParameterValue(processor, 8);
		int autoStart = getParameterValue(processor, 9);

		String threadName = readStringZ(processor, namePtr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXThreadCreate threadPtr=0x%08X, namePtr=0x%08X('%s'), entryFunction=0x%08X, entryInput=0x%X, stackStart=0x%08X, stackSize=0x%X, priority=0x%X, preemptThreshold=0x%X, timeSlice=0x%X, autoStart=0x%X", threadPtr, namePtr, threadName, entryFunction, entryInput, stackStart, stackSize, priority, preemptThreshold, timeSlice, autoStart));
		}

		int result = getTxManager().threadCreate(processor, threadPtr, threadName, entryFunction, entryInput, stackStart, stackSize, priority, preemptThreshold, timeSlice, autoStart);

		returnToLr(processor, result);
	}
}
