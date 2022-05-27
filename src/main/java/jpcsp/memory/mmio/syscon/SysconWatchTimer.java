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

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.WTIF;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.WTIIF;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.now;
import static jpcsp.util.Utilities.hasBit;

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
public class SysconWatchTimer implements IState {
	private static final int STATE_VERSION = 0;
	private Logger log = Nec78k0Processor.log;
	private final MMIOHandlerSysconFirmwareSfr sfr;
	private final SysconScheduler scheduler;
	private final TimerAction watchTimer;
	private final TimerAction intervalTimer;
	private int operationMode;

	private class TimerAction implements IAction {
		private final int interruptBit;
		private final String name;
		private long nextTimer;
		private int step;

		public TimerAction(String name, int interruptBit) {
			this.name = name;
			this.interruptBit = interruptBit;
		}

		@Override
		public void execute() {
			if (hasBit(operationMode, 0)) {
				long now = now();
				if (now >= nextTimer) {
					sfr.setInterruptRequest(interruptBit);
					nextTimer += step;
				}
				scheduler.addAction(nextTimer, this);
			}
		}

		public void reset() {
			nextTimer = 0L;
			step = 0;
			scheduler.removeAction(this);
		}

		public void setStep(int step) {
			this.step = step;

			scheduler.removeAction(this);
			nextTimer = now() + step;
			scheduler.addAction(nextTimer, this);
		}

		@Override
		public String toString() {
			return String.format("%s next=0x%X, step=0x%X", name, nextTimer, step);
		}
	}

	public SysconWatchTimer(MMIOHandlerSysconFirmwareSfr sfr, SysconScheduler scheduler) {
		this.sfr = sfr;
		this.scheduler = scheduler;

		watchTimer = new TimerAction("Watch Timer", WTIF);
		intervalTimer = new TimerAction("Interval Timer", WTIIF);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		operationMode = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(operationMode);
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void reset() {
		operationMode = 0;
		watchTimer.reset();
		intervalTimer.reset();
	}

	public int getOperationMode() {
		return operationMode;
	}

	public void setOperationMode(int value) {
		if (operationMode != value) {
			operationMode = value;

			if (hasBit(operationMode, 7)) {
				switch ((operationMode >> 2) & 0x3) {
					case 0: watchTimer.setStep(500000); break; // 0.5s
					case 1: watchTimer.setStep(250000); break; // 0.25s
					case 2: watchTimer.setStep(977); break; // 977us
					case 3: watchTimer.setStep(488); break; // 488us
				}

				switch ((operationMode >> 4) & 0x7) {
					case 0: intervalTimer.setStep(488); break; // 488us
					case 1: intervalTimer.setStep(977); break; // 977us
					case 2: intervalTimer.setStep(1950); break; // 1.95ms
					case 3: intervalTimer.setStep(3910); break; // 3.91ms
					case 4: intervalTimer.setStep(7810); break; // 7.81ms
					case 5: intervalTimer.setStep(15600); break; // 15.6ms
					case 6: intervalTimer.setStep(31300); break; // 31.3ms
					case 7: intervalTimer.setStep(62500); break; // 62.5ms
				}
			} else {
				// Assume operation at Fprs = 5 MHz
				switch ((operationMode >> 2) & 0x3) {
					case 0: watchTimer.setStep(419000); break; // 0.419s
					case 1: watchTimer.setStep(210000); break; // 0.21s
					case 2: watchTimer.setStep(819); break; // 819us
					case 3: watchTimer.setStep(410); break; // 410us
				}

				switch ((operationMode >> 4) & 0x7) {
					case 0: intervalTimer.setStep(410); break; // 410us
					case 1: intervalTimer.setStep(820); break; // 820us
					case 2: intervalTimer.setStep(1640); break; // 1.64ms
					case 3: intervalTimer.setStep(3280); break; // 3.28ms
					case 4: intervalTimer.setStep(6550); break; // 6.55ms
					case 5: intervalTimer.setStep(13100); break; // 13.1ms
					case 6: intervalTimer.setStep(26200); break; // 26.2ms
					case 7: intervalTimer.setStep(52400); break; // 52.4ms
				}
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("setWatchTimerOperationMode WTM=0x%02X, %s, %s", operationMode, watchTimer, intervalTimer));
			}
		}
	}
}
