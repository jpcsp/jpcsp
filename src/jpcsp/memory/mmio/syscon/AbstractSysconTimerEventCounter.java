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
package jpcsp.memory.mmio.syscon;

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.now;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.IAction;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public abstract class AbstractSysconTimerEventCounter implements IState {
	protected Logger log = Nec78k0Processor.log;
	private static final int STATE_VERSION = 0;
	protected final MMIOHandlerSysconFirmwareSfr sfr;
	protected final SysconScheduler scheduler;
	protected final String name;
	protected final int overflowValue;
	protected int timerCounter;
	protected int compareValue0;
	protected int compareValue1;
	protected int clockStepNanoSeconds;
	protected boolean enabled;
	protected boolean isFreeRunning;
	protected boolean isIntervalTimer;
	private final SysconTimerEventCounterAction eventCounterAction;

	private class SysconTimerEventCounterAction implements IAction {
		private long start;

		public void setStart(long start) {
			this.start = start;
		}

		public long getStart() {
			return start;
		}

		@Override
		public void execute() {
			actionTriggered();
		}
	}

	public AbstractSysconTimerEventCounter(MMIOHandlerSysconFirmwareSfr sfr, SysconScheduler scheduler, String name, int overflowValue) {
		this.sfr = sfr;
		this.scheduler = scheduler;
		this.name = name;
		this.overflowValue = overflowValue;
		eventCounterAction = new SysconTimerEventCounterAction();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		timerCounter = stream.readInt();
		compareValue0 = stream.readInt();
		compareValue1 = stream.readInt();
		clockStepNanoSeconds = stream.readInt();
		enabled = stream.readBoolean();
		isFreeRunning = stream.readBoolean();
		isIntervalTimer = stream.readBoolean();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(timerCounter);
		stream.writeInt(compareValue0);
		stream.writeInt(compareValue1);
		stream.writeInt(clockStepNanoSeconds);
		stream.writeBoolean(enabled);
		stream.writeBoolean(isFreeRunning);
		stream.writeBoolean(isIntervalTimer);
	}

	public void reset() {
		timerCounter = 0;
		compareValue0 = -1;
		compareValue1 = -1;
		clockStepNanoSeconds = 0;
		enabled = false;
		isFreeRunning = false;
		isIntervalTimer = false;

		updateScheduler();
	}

	public void setMode(boolean isFreeRunning, boolean isIntervalTimer) {
		this.isFreeRunning = isFreeRunning;
		this.isIntervalTimer = isIntervalTimer;
		enabled = true;

		if (log.isDebugEnabled()) {
			log.debug(String.format("%s setMode isFreeRunning=%b, isIntervalTimer=%b", name, isFreeRunning, isIntervalTimer));
		}

		updateScheduler();
	}

	public void setCompare0(int value) {
		compareValue0 = value;

		updateScheduler();
	}

	public void setCompare1(int value) {
		compareValue1 = value;

		updateScheduler();
	}

	public void setClockStep(int nanoSeconds) {
		clockStepNanoSeconds = nanoSeconds;

		updateScheduler();
	}

	public void disable() {
		enabled = false;
		timerCounter = 0;

		updateScheduler();
	}

	private void updateScheduler() {
		scheduler.removeAction(eventCounterAction);

		if (clockStepNanoSeconds <= 0 || !enabled) {
			return;
		}

		int nextValue;
		if (timerCounter <= compareValue0 && timerCounter <= compareValue1) {
			int compareValue = Math.min(compareValue0, compareValue1);
			nextValue = compareValue - timerCounter;
			if (log.isTraceEnabled()) {
				log.trace(String.format("%s updateScheduler next schedule of time=0x%X reached for compareValue=0x%X (compareValue0=0x%X, compareValue1=0x%X)", name, timerCounter, compareValue, compareValue0, compareValue1));
			}
		} else if (timerCounter <= compareValue0) {
			nextValue = compareValue0 - timerCounter;
			if (log.isTraceEnabled()) {
				log.trace(String.format("%s updateScheduler next schedule of time=0x%X reached for compareValue0=0x%X", name, timerCounter, compareValue0));
			}
		} else if (timerCounter <= compareValue1) {
			nextValue = compareValue1 - timerCounter;
			if (log.isTraceEnabled()) {
				log.trace(String.format("%s updateScheduler next schedule of time=0x%X reached for compareValue1=0x%X", name, timerCounter, compareValue1));
			}
		} else {
			nextValue = overflowValue - timerCounter;
			if (log.isTraceEnabled()) {
				log.trace(String.format("%s updateScheduler next schedule of time=0x%X reached for overflowValue=0x%X", name, timerCounter, overflowValue));
			}
		}

		long start = now();
		eventCounterAction.setStart(start);

		long schedule = start + nextValue * clockStepNanoSeconds / 1000;

		if (log.isTraceEnabled()) {
			log.trace(String.format("%s updateScheduler now=0x%X, schedule=0x%X", name, start, schedule));
		}

		scheduler.addAction(schedule, eventCounterAction);
	}

	protected abstract void onTimerCounterOverflow();
	protected abstract void onTimerCounterCompare0();
	protected abstract void onTimerCounterCompare1();

	private void actionTriggered() {
		int previousTimerCounter = timerCounter;

		getTimerCounter(true);

		if (isFreeRunning && timerCounter > overflowValue) {
			timerCounter &= overflowValue;

			onTimerCounterOverflow();

			previousTimerCounter = -1;
		}

		if (previousTimerCounter <= compareValue0 && timerCounter > compareValue0) {
			onTimerCounterCompare0();

			if (isIntervalTimer && compareValue0 != 0) {
				timerCounter %= compareValue0;
			}
		}

		if (previousTimerCounter <= compareValue1 && timerCounter > compareValue1) {
			onTimerCounterCompare1();
		}

		updateScheduler();
	}

	private int getTimerCounter(boolean updateTimerCounter) {
		long start = eventCounterAction.getStart();
		long now = now();
		int duration;
		if (clockStepNanoSeconds == 0) {
			duration = 0;
		} else {
			duration = (int) ((now - start) * 1000 / clockStepNanoSeconds);
		}
		if (log.isTraceEnabled()) {
			log.trace(String.format("%s getTimerCounter now=0x%X, duration=0x%X, timerCounter=0x%X", name, now, duration, timerCounter + duration));
		}

		if (updateTimerCounter) {
			timerCounter += duration;
		}

		return timerCounter;
	}

	public int getTimerCounter() {
		return getTimerCounter(false);
	}
}
