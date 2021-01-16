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
 * UINT tx_timer_create(TX_TIMER *timer_ptr,
 *                      CHAR *name_ptr,
 *                      VOID (*expiration_function)(ULONG),
 *                      ULONG expiration_input,
 *                      ULONG initial_ticks,
 *                      ULONG reschedule_ticks, 
 *                      UINT auto_activate)
 *
 * @author gid15
 *
 */
public class TXTimerCreate extends TXBaseCall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int timerPtr = getParameterValue(processor, 0);
		int namePtr = getParameterValue(processor, 1);
		int expirationFunction = getParameterValue(processor, 2);
		int expirationInput = getParameterValue(processor, 3);
		int initialTicks = getParameterValue(processor, 4);
		int rescheduleTicks = getParameterValue(processor, 5);
		int autoActivate = getParameterValue(processor, 6);

		String timerName = readStringZ(processor, namePtr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXTimerCreate timerPtr=0x%08X, namePtr=0x%08X('%s'), expirationFunction=0x%08X, expirationInput=0x%X, initialTicks=0x%X, rescheduleTicks=0x%X, autoActivate=0x%X", timerPtr, namePtr, timerName, expirationFunction, expirationInput, initialTicks, rescheduleTicks, autoActivate));
		}

		int result = getTxManager().timerCreate(processor, timerPtr, timerName, expirationFunction, expirationInput, initialTicks, rescheduleTicks, autoActivate);

		returnToLr(processor, result);
	}
}
