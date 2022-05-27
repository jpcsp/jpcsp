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

import static jpcsp.Allegrex.compiler.RuntimeContextLLE.clearInterrupt;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.triggerInterrupt;
import static jpcsp.HLE.kernel.managers.IntrManager.getInterruptName;
import static jpcsp.scheduler.Scheduler.getNow;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.isFallingFlag;
import static jpcsp.util.Utilities.isRaisingFlag;
import static jpcsp.util.Utilities.notHasFlag;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceSystimer;
import jpcsp.scheduler.Scheduler;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerTimer extends MMIOHandlerBase {
	private static Logger log = sceSystimer.log;
	private static final int STATE_VERSION = 0;
	private static final int TIMER_FREQUENCY_NUMERATOR = 48;
	private static final int TIMER_FREQUENCY_DENOMINATOR = 1;
	private static final int STATUS_COUNTER_VALUE = 0x003FFFFF;
	private static final int STATUS_TRIGGER_INTERRUPT = 0x00400000;
	private static final int STATUS_COUNTER_ACTIVE = 0x00800000;
	private static final int STATUS_VALUE_MASK = STATUS_COUNTER_VALUE | STATUS_TRIGGER_INTERRUPT | STATUS_COUNTER_ACTIVE;
	private static final int STATUS_RESET_OVERFLOW = 0x80000000;
	private static final int STATUS_COUNTER_OVERFLOW = 0xFF000000;
	private static final int COUNTER_VALUE_MASK = 0x00FFFFFF;
	private final TriggerTimerInterruptAction triggerTimerInterruptAction = new TriggerTimerInterruptAction();
	private final int interruptNumber;
	private int status;
	private int counter;
	private int prsclNumerator;
	private int prsclDenominator;
	private long schedule;
	private long start;
	private int overflowRead;

	private class TriggerTimerInterruptAction implements IAction {
		@Override
		public void execute() {
			triggerTimerInterrupt();
		}
	}

	public MMIOHandlerTimer(int baseAddress, int interruptNumber) {
		super(baseAddress);

		this.interruptNumber = interruptNumber;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		status = stream.readInt();
		counter = stream.readInt();
		prsclNumerator = stream.readInt();
		prsclDenominator = stream.readInt();
		schedule = stream.readLong();
		start = stream.readLong();
		overflowRead = stream.readInt();

		if (schedule != 0L) {
			updateInterruptSchedule();
		}
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(status);
		stream.writeInt(counter);
		stream.writeInt(prsclNumerator);
		stream.writeInt(prsclDenominator);
		stream.writeLong(schedule);
		stream.writeLong(start);
		stream.writeInt(overflowRead);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		status = 0;
		counter = 0;
		prsclNumerator = 0;
		prsclDenominator = 0;
		schedule = 0L;
		start = 0L;
		overflowRead = 0;
	}

	private void triggerTimerInterrupt() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Triggering %s interrupt for %s", getInterruptName(interruptNumber), this));
		}
		triggerInterrupt(getProcessor(), interruptNumber);
	}

	private int getStatus() {
		int currentOverflow = updateCounter();
		overflowRead = currentOverflow;

		if (hasFlag(status, STATUS_COUNTER_ACTIVE) && hasFlag(status, STATUS_TRIGGER_INTERRUPT)) {
			clearInterrupt(getProcessor(), interruptNumber);
			updateInterruptSchedule();
		}

		status = (status & STATUS_VALUE_MASK) | (counter & STATUS_COUNTER_OVERFLOW);

		return status;
	}

	private int getStatusReadOnly() {
		updateCounter();
		return (status & STATUS_VALUE_MASK) | (counter & STATUS_COUNTER_OVERFLOW);
	}

	private void updateInterruptSchedule() {
		updateInterruptSchedule(status);
	}

	private void updateInterruptSchedule(int value) {
		Scheduler scheduler = Emulator.getScheduler();
		long scheduleFromNow = ((long) (value & STATUS_COUNTER_VALUE)) * (TIMER_FREQUENCY_DENOMINATOR * prsclDenominator) / (TIMER_FREQUENCY_NUMERATOR * prsclNumerator);
		if (log.isTraceEnabled()) {
			log.trace(String.format("setTimerData will trigger interrupt in %d microseconds", scheduleFromNow));
		}
		schedule = getNow() + scheduleFromNow;
		scheduler.addAction(schedule, triggerTimerInterruptAction);
	}

	private void setStatus(int value) {
		Scheduler scheduler = Emulator.getScheduler();

		if (isRaisingFlag(status, value, STATUS_COUNTER_ACTIVE)) {
			// Starting the counter
			start = getNow();

			// Reset the counter overflow?
			if (hasFlag(value, STATUS_RESET_OVERFLOW)) {
				overflowRead = updateCounter();
			}
		} else if (isFallingFlag(status, value, STATUS_COUNTER_ACTIVE)) {
			// Stopping the counter
			if (schedule != 0L) {
				scheduler.removeAction(schedule, triggerTimerInterruptAction);
				clearInterrupt(getProcessor(), interruptNumber);
			}
		}

		// Generating an interrupt when the counter is elapsing?
		if (hasFlag(value, STATUS_TRIGGER_INTERRUPT) && hasFlag(value, STATUS_COUNTER_ACTIVE) && schedule == 0L) {
			updateInterruptSchedule(value);
		} else if (notHasFlag(value, STATUS_TRIGGER_INTERRUPT) && schedule != 0L) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("setTimerData removing the scheduled action for the interrupt"));
			}
			scheduler.removeAction(schedule, triggerTimerInterruptAction);
			schedule = 0L;
		}

		status = value & STATUS_VALUE_MASK;
	}

	private int updateCounter() {
		int overflow = 0;
		counter = 0;

		if ((status & STATUS_COUNTER_ACTIVE) != 0) {
			long duration = getNow() - start;
			long steps = duration * (TIMER_FREQUENCY_NUMERATOR * prsclNumerator) / (TIMER_FREQUENCY_DENOMINATOR * prsclDenominator);
			int maxCounter = status & STATUS_COUNTER_VALUE;
			if (maxCounter != 0) {
				overflow = (int) (steps / maxCounter);
				int currentOverflow = overflow - overflowRead;
				counter = maxCounter - (int) (steps % maxCounter);
				counter &= COUNTER_VALUE_MASK;
				counter |= (Math.min(currentOverflow, 0xFF) << 24);
			}
		}

		return overflow;
	}

	private int getCounter() {
		updateCounter();

		return counter;
	}

	@Override
	public int read32(int address) {
		int value;

		switch (address - baseAddress) {
			case 0x000: value = getStatus(); break;
			case 0x004: value = getCounter(); break;
			case 0x008: value = prsclNumerator; break;
			case 0x00C: value = prsclDenominator; break;
			case 0x100: value = getStatusReadOnly(); break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x000: setStatus(value); break;
			case 0x004: break;
			case 0x008: prsclNumerator = value; break;
			case 0x00C: prsclDenominator = value; break;
			case 0x100: break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("Timer 0x%08X(%s): status=0x%08X, counter=0x%08X, prsclNumerator=0x%X, prsckDenominator=0x%X", baseAddress, getInterruptName(interruptNumber), getStatusReadOnly(), counter, prsclNumerator, prsclDenominator);
	}
}
