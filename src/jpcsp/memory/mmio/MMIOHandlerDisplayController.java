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

import static jpcsp.Emulator.getScheduler;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_VBLANK_INTR;
import static jpcsp.scheduler.Scheduler.getNow;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceDisplay;

public class MMIOHandlerDisplayController extends MMIOHandlerBase {
	public static Logger log = sceDisplay.log;
	public static final int BASE_ADDRESS = 0xBE740000;
	private static MMIOHandlerDisplayController instance;
	private long baseTimeMicros;
	private static final int displaySyncMicros = (1000000 + 30) / 60;
	private static final int numberDisplayRows = 286;
	private static final int rowSyncNanos = (displaySyncMicros + 143) / numberDisplayRows;
	private TriggerVblankInterruptAction triggerVblankInterruptAction;
	// Used for debugging: limit the number of VBLANK interrupts being triggered
	private static int maxVblankInterrupts = 0;

	private class TriggerVblankInterruptAction implements IAction {
		@Override
		public void execute() {
			triggerVblankInterrupt();
		}
	}

	public static MMIOHandlerDisplayController getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerDisplayController(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerDisplayController(int baseAddress) {
		super(baseAddress);
	}

	private long getTimeMicros() {
		return getNow() - baseTimeMicros;
	}

	private int getDisplayRowSync() {
		return (int) (getTimeMicros() / rowSyncNanos) + 1;
	}

	private int getDisplaySync() {
		return (int) (getTimeMicros() / displaySyncMicros) * numberDisplayRows;
	}

	private long getPreviousVblankSchedule() {
		return getTimeMicros() / displaySyncMicros * displaySyncMicros + baseTimeMicros;
	}

	private long getNextVblankSchedule() {
		return getPreviousVblankSchedule() + displaySyncMicros;
	}

	public static void setMaxVblankInterrupts(int maxVblankInterrupts) {
		MMIOHandlerDisplayController.maxVblankInterrupts = maxVblankInterrupts;
	}

	private void scheduleNextVblankInterrupt() {
		if (maxVblankInterrupts == 0) {
			return;
		}
		if (maxVblankInterrupts > 0) {
			maxVblankInterrupts--;
		}

		long schedule = getNextVblankSchedule();
		if (log.isDebugEnabled()) {
			log.debug(String.format("Scheduling next Vblank at 0x%X, %s", schedule, this));
		}
		getScheduler().addAction(schedule, triggerVblankInterruptAction);
	}

	public void triggerVblankInterrupt() {
		scheduleNextVblankInterrupt();
		RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_VBLANK_INTR);
	}

	private void startVblankInterrupts() {
		if (triggerVblankInterruptAction == null) {
			triggerVblankInterruptAction = new TriggerVblankInterruptAction();
			scheduleNextVblankInterrupt();
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x04: value = getDisplayRowSync(); break;
			case 0x08: value = getDisplaySync(); break;
			case 0x20: value = 0; break;
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
			case 0x00: break;
			case 0x04: baseTimeMicros = getNow(); startVblankInterrupts(); break;
			case 0x0C: break;
			case 0x10: break;
			case 0x14: break;
			case 0x24: break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("MMIOHandlerDisplayController rowSync=0x%X, displaySync=0x%X, baseTime=0x%X, now=0x%X", getDisplayRowSync(), getDisplaySync(), baseTimeMicros, getNow());
	}
}
