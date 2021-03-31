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
import static jpcsp.util.Utilities.hasBit;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.nec78k0.Nec78k0Processor;

/**
 * @author gid15
 *
 */
public class SysconWatchTimer extends Thread {
	private Logger log = Nec78k0Processor.log;
	private final MMIOHandlerSysconFirmwareSfr sfr;
	private Nec78k0Processor processor;
	private final Semaphore update = new Semaphore(0);
	private int watchTimerOperationMode;
	private long nextWatchTimer;
	private int stepWatchTimer;
	private long nextIntervalTimer;
	private int stepIntervalTimer;

	public SysconWatchTimer(MMIOHandlerSysconFirmwareSfr sfr) {
		this.sfr = sfr;
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void setProcessor(Nec78k0Processor processor) {
		this.processor = processor;
		update.release();
	}

	@Override
	public void run() {
		while (processor == null) {
			try {
				update.acquire();
			} catch (InterruptedException e) {
				// Ignore exception
			}
		}

		while (true) {
			try {
				update.tryAcquire(1, 10L, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// Ignore exception
			}

			if (hasBit(watchTimerOperationMode, 0)) {
				long now = now();
				if (now >= nextWatchTimer) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("SysconWatchTimer triggering WatchTimer Interrupt"));
					}
					sfr.setInterruptRequest(WTIF);
					nextWatchTimer += stepWatchTimer;
				}
				if (now >= nextIntervalTimer) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("SysconWatchTimer triggering IntervalTimer Interrupt"));
					}
					sfr.setInterruptRequest(WTIIF);
					nextIntervalTimer += stepIntervalTimer;
				}

				if (log.isTraceEnabled()) {
					log.trace(String.format("SysconWatchTimer now=0x%X, nextWatchTimer=0x%X, nextIntervalTimer=0x%X", now, nextWatchTimer, nextIntervalTimer));
				}
			}
		}
	}

	private long now() {
		return Emulator.getClock().microTime();
	}

	public void setWatchTimerOperationMode(int watchTimerOperationMode) {
		this.watchTimerOperationMode = watchTimerOperationMode;

		if (hasBit(watchTimerOperationMode, 7)) {
			switch ((watchTimerOperationMode >> 2) & 0x3) {
				case 0: stepWatchTimer = 500000; break; // 0.5s
				case 1: stepWatchTimer = 250000; break; // 0.25s
				case 2: stepWatchTimer = 977; break; // 977us
				case 3: stepWatchTimer = 488; break; // 488us
			}

			switch ((watchTimerOperationMode >> 4) & 0x7) {
				case 0: stepIntervalTimer = 488; break; // 488us
				case 1: stepIntervalTimer = 977; break; // 977us
				case 2: stepIntervalTimer = 1950; break; // 1.95ms
				case 3: stepIntervalTimer = 3910; break; // 3.91ms
				case 4: stepIntervalTimer = 7810; break; // 7.81ms
				case 5: stepIntervalTimer = 15600; break; // 15.6ms
				case 6: stepIntervalTimer = 31300; break; // 31.3ms
				case 7: stepIntervalTimer = 62500; break; // 62.5ms
			}
		} else {
			// Assume operation at Fprs = 5 MHz
			switch ((watchTimerOperationMode >> 2) & 0x3) {
				case 0: stepWatchTimer = 419000; break; // 0.419s
				case 1: stepWatchTimer = 210000; break; // 0.21s
				case 2: stepWatchTimer = 819; break; // 819us
				case 3: stepWatchTimer = 410; break; // 410us
			}

			switch ((watchTimerOperationMode >> 4) & 0x7) {
				case 0: stepIntervalTimer = 410; break; // 410us
				case 1: stepIntervalTimer = 820; break; // 820us
				case 2: stepIntervalTimer = 1640; break; // 1.64ms
				case 3: stepIntervalTimer = 3280; break; // 3.28ms
				case 4: stepIntervalTimer = 6550; break; // 6.55ms
				case 5: stepIntervalTimer = 13100; break; // 13.1ms
				case 6: stepIntervalTimer = 26200; break; // 26.2ms
				case 7: stepIntervalTimer = 52400; break; // 52.4ms
			}
		}

		long now = now();
		nextWatchTimer = now + stepWatchTimer;
		nextIntervalTimer = now + stepIntervalTimer;

		if (log.isDebugEnabled()) {
			log.debug(String.format("setWatchTimerOperationMode WTM=0x%02X, stepWatchTimer=%d, stepIntervalTimer=%d", watchTimerOperationMode, stepWatchTimer, stepIntervalTimer));
		}

		update.release();
	}
}
