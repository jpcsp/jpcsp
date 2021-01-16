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

import static jpcsp.arm.ARMProcessor.NUMBER_REGISTERS;

/**
 * @author gid15
 *
 */
public class TXThread {
	public static final int SIZEOF = 0x94;
	public static final int TX_THREAD_STATE_READY = 1;
	public static final int TX_THREAD_STATE_SUSPENDED = 2;
	public int threadPtr;
	public String threadName;
	public int entryFunction;
	public int entryInput;
	public int stackStart;
	public int stackSize;
	public int priority;
	public int preemptyThreshold;
	public int timeSlice;
	public int state = TX_THREAD_STATE_SUSPENDED;
	public int[] savedRegisters = new int[NUMBER_REGISTERS];
	public int savedCpsr;

	@Override
	public String toString() {
		return String.format("TXThread threadPtr=0x%08X, threadName='%s', entryFunction=0x%08X, entryInput=0x%X", threadPtr, threadName, entryFunction, entryInput);
	}
}
