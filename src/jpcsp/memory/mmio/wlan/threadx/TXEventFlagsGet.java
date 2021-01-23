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
 * UINT tx_event_flags_get(TX_EVENT_FLAGS_GROUP *group_ptr,
 *                         ULONG requested_flags,
 *                         UINT get_option,
 *                         ULONG *actual_flags_ptr,
 *                         ULONG wait_option)
 *
 * @author gid15
 *
 */
public class TXEventFlagsGet extends TXBaseCall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int groupPtr = getParameterValue(processor, 0);
		int requestedFlags = getParameterValue(processor, 1);
		int getOption = getParameterValue(processor, 2);
		int actualFlagsPtr = getParameterValue(processor, 3);
		int waitOption = getParameterValue(processor, 4);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXEventFlagsGet groupPtr=0x%08X, requestedFlags=0x%X, getOption=0x%X, actualFlagsPtr=0x%08X, waitOption=0x%X", groupPtr, requestedFlags, getOption, actualFlagsPtr, waitOption));
		}

		int result = getTxManager().eventFlagsGet(processor, groupPtr, requestedFlags, getOption, actualFlagsPtr, waitOption);

		returnToLr(processor, result);
	}
}
