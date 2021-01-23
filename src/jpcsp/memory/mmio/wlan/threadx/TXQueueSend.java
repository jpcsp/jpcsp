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
 * UINT tx_queue_send(TX_QUEUE *queue_ptr,
 *                    VOID *source_ptr,
 *                    ULONG wait_option)
 *
 * @author gid15
 *
 */
public class TXQueueSend extends TXBaseCall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int queuePtr = getParameterValue(processor, 0);
		int sourcePtr = getParameterValue(processor, 1);
		int waitOption = getParameterValue(processor, 2);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXQueueSend queuePtr=0x%08X, sourcePtr=0x%08X, waitOption=0x%X", queuePtr, sourcePtr, waitOption));
		}

		int result = getTxManager().queueSend(processor, queuePtr, sourcePtr, waitOption);

		returnToLr(processor, result);
	}
}
