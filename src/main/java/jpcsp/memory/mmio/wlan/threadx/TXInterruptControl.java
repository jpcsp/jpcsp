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

import static jpcsp.arm.ARMProcessor.CPSR_BIT_I;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.setBit;

import jpcsp.arm.ARMProcessor;

/**
 * UINT tx_semaphore_put(TX_SEMAPHORE *semaphore_ptr)
 *
 * @author gid15
 *
 */
public class TXInterruptControl extends TXBaseCall {
	private static final int TX_INT_DISABLE = setBit(0, CPSR_BIT_I);
	private static final int TX_INT_ENABLE = 0x00;

	@Override
	public void call(ARMProcessor processor, int imm) {
		int newPosture = getParameterValue(processor, 0);

		int oldPosture = processor.isInterruptEnabled() ? TX_INT_ENABLE : TX_INT_DISABLE;

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXInterruptControl newPosture=0x%X, oldPosture=0x%X", newPosture, oldPosture));
		}

		if (hasFlag(newPosture, TX_INT_DISABLE)) {
			processor.setInterruptDisabled();
		} else {
			processor.setInterruptEnabled();
		}

		returnToLr(processor, oldPosture);
	}
}
