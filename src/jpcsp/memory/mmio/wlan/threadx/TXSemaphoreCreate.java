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
 * UINT tx_semaphore_create(TX_SEMAPHORE *semaphore_ptr,
 *                          CHAR *name_ptr,
 *                          ULONG initial_count)
 *
 * @author gid15
 *
 */
public class TXSemaphoreCreate extends TXBaseCall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int semaphorePtr = getParameterValue(processor, 0);
		int namePtr = getParameterValue(processor, 1);
		int initialCount = getParameterValue(processor, 2);

		String semaphoreName = readStringZ(processor, namePtr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXSemaphoreCreate semaphorePtr=0x%08X, namePtr=0x%08X('%s'), initialCount=0x%X", semaphorePtr, namePtr, semaphoreName, initialCount));
		}

		int result = getTxManager().semaphoreCreate(processor, semaphorePtr, semaphoreName, initialCount);

		returnToLr(processor, result);
	}
}
