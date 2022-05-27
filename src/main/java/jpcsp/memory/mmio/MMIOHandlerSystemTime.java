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
import static jpcsp.Emulator.getScheduler;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_THREAD0_INTR;

import java.io.IOException;

import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.scheduler.Scheduler;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerSystemTime extends MMIOHandlerBase {
	private static final int STATE_VERSION = 0;
	private long alarm;
	private TriggerAlarmInterruptAction triggerAlarmInterruptAction;

	private class TriggerAlarmInterruptAction implements IAction {
		@Override
		public void execute() {
			triggerAlarmInterrupt();
		}
	}

	public MMIOHandlerSystemTime(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		setAlarm((int) (stream.readLong() + getSystemTime()));
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeLong(alarm - getSystemTime());
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		alarm = 0L;
	}

	private int getSystemTime() {
		return (int) SystemTimeManager.getSystemTime();
	}

	private void setAlarm(int alarm) {
		Scheduler scheduler = getScheduler();

		if (triggerAlarmInterruptAction == null) {
			triggerAlarmInterruptAction = new TriggerAlarmInterruptAction();
		} else {
			scheduler.removeAction(this.alarm, triggerAlarmInterruptAction);
			clearInterrupt(getProcessor(), PSP_THREAD0_INTR);
		}

		this.alarm = alarm & 0xFFFFFFFFL;

		scheduler.addAction(this.alarm, triggerAlarmInterruptAction);
	}

	private void triggerAlarmInterrupt() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Triggering PSP_THREAD0_INTR interrupt for %s", this));
		}
		triggerInterrupt(getProcessor(), PSP_THREAD0_INTR);
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = getSystemTime(); break;
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
			case 0x00:
				// Value is set to 0 at boot time
				if (value != 0) {
					super.write32(address, value);
				}
				break;
			case 0x04:
				setAlarm(value);
				break;
			case 0x08:
				// Value is set to 0x30 at boot time
				if (value != 0x30) {
					super.write32(address, value);
				}
				break;
			case 0x0C:
				// Value is set to 0x1 at boot time
				if (value != 0x1) {
					super.write32(address, value);
				}
				break;
			case 0x10:
				// Value is set to 0 at boot time
				if (value != 0) {
					super.write32(address, value);
				}
				break;
			default:
				super.write32(address, value);
				break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("MMIOHandlerSystemTime systemTime=0x%08X, alarm=0x%08X", getSystemTime(), alarm);
	}
}
