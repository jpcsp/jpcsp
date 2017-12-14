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
package jpcsp.memory.mmio;

import jpcsp.HLE.kernel.managers.SystemTimeManager;

public class MMIOHandlerTimer extends MMIOHandlerBase {
	private static final int TIMER_COUNTER_MASK = 0x003FFFFF;
	private static final int TIMER_MODE_SHIFT = 22;
	// Indicates that a time-up handler is set for the specific timer
	public static final int TIMER_MODE_HANDLER_REGISTERED = 1 << 0;
	// Indicates that the timer is in use
	public static final int TIMER_MODE_IN_USE = 1 << 1;
	// Unknown timer mode
	public static final int TIMER_MODE_UNKNOWN = 1 << 9;
	public int timerMode;
	public int timerCounter;
	public int baseTime;
	public int prsclNumerator;
	public int prsclDenominator;

	public MMIOHandlerTimer(int baseAddress) {
		super(baseAddress);
	}

	private int getTimerData() {
		return (timerCounter & TIMER_COUNTER_MASK) | (timerMode << TIMER_MODE_SHIFT);
	}

	private void setTimerData(int timerData) {
		timerCounter = timerData & TIMER_COUNTER_MASK;
		timerMode = timerData >>> TIMER_MODE_SHIFT;
	}

	private int getNowData() {
		int systemTime = (int) SystemTimeManager.getSystemTime();
		return (timerMode << TIMER_MODE_SHIFT) | (systemTime & TIMER_COUNTER_MASK);
	}

	@Override
	public int read32(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) on %s", getPc(), address, this));
		}

		switch (address - baseAddress) {
			case 0: return getTimerData();
			case 4: return baseTime;
			case 8: return prsclNumerator;
			case 12: return prsclDenominator;
			case 256: return getNowData();
		}
		return super.read32(address);
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0: setTimerData(value); break;
			case 4: baseTime = value; break;
			case 8: prsclNumerator = value; break;
			case 12: prsclDenominator = value; break;
			case 256: break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("Timer 0x%08X: timerMode=0x%03X, timerCounter=0x%06X, baseTime=0x%08X, prsclNumerator=0x%X, prsckDenominator=0x%X", baseAddress, timerMode, timerCounter, baseTime, prsclNumerator, prsclDenominator);
	}
}
