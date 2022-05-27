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
 * UINT tx_semaphore_put(TX_SEMAPHORE *semaphore_ptr)
 *
 * @author gid15
 *
 */
public class TXSemaphorePut extends TXBaseCall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int semaphorePtr = getParameterValue(processor, 0);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXSemaphorePut semaphorePtr=0x%08X", semaphorePtr));
		}

		int result = getTxManager().semaphorePut(processor, semaphorePtr);

		returnToLr(processor, result);
	}
}
