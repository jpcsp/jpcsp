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
 * UINT tx_queue_create(TX_QUEUE *queue_ptr,
 *                      CHAR *name_ptr,
 *                      UINT message_size,
 *                      VOID *queue_start,
 *                      ULONG queue_size)
 *
 * @author gid15
 *
 */
public class TXQueueCreate extends TXBaseCall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int queuePtr = getParameterValue(processor, 0);
		int namePtr = getParameterValue(processor, 1);
		int messageSize = getParameterValue(processor, 2);
		int queueStart = getParameterValue(processor, 3);
		int queueSize = getParameterValue(processor, 4);

		String queueName = readStringZ(processor, namePtr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXQueueCreate queuePtr=0x%08X, namePtr=0x%08X('%s'), messageSize=0x%X, queueStart=0x%08X, queueSize=0x%X", queuePtr, namePtr, queueName, messageSize, queueStart, queueSize));
		}

		int result = getTxManager().queueCreate(processor, queuePtr, queueName, messageSize, queueStart, queueSize);

		returnToLr(processor, result);
	}
}
