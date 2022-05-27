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

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.scheduler.Scheduler;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerDisplayController extends MMIOHandlerBase {
	public static Logger log = sceDisplay.log;
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS = 0xBE740000;
	private static MMIOHandlerDisplayController instance;
	private long baseTimeMicros;
	private static final int displaySyncMicros = (1000000 + 30) / 60;
	private static final int numberDisplayRows = 286;
	private static final int rowSyncMicros = (displaySyncMicros + (numberDisplayRows / 2)) / numberDisplayRows;
	private TriggerVblankInterruptAction triggerVblankInterruptAction;
	private long triggerVblankInterruptSchedule;
	// Used for debugging: limit the number of VBLANK interrupts being triggered
	private static int maxVblankInterrupts = 0;

	private class TriggerVblankInterruptAction implements IAction {
		@Override
		public void execute() {
			// The scheduler action has been executed, no need to remove it during scheduleNextVblankInterrupt()
			triggerVblankInterruptSchedule = 0L;

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

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		maxVblankInterrupts = stream.readInt();
		baseTimeMicros = getNow() - stream.readLong();
		super.read(stream);

		triggerVblankInterruptSchedule = 0L;
		triggerVblankInterruptAction = null;

		startVblankInterrupts();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(maxVblankInterrupts);
		stream.writeLong(getTimeMicros());
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		maxVblankInterrupts = 0;
		baseTimeMicros = getNow();
	}

	private long getTimeMicros() {
		return getNow() - baseTimeMicros;
	}

	private int getDisplayRowSync(long time) {
		return getDisplaySync(time) + (((int) (time / rowSyncMicros) + 1) % numberDisplayRows);
	}

	private int getDisplaySync(long time) {
		return (int) (time / displaySyncMicros) * numberDisplayRows;
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
			if (log.isDebugEnabled()) {
				log.debug(String.format("Skipping Vblank interrupt %s", this));
			}
			return;
		}
		if (maxVblankInterrupts > 0) {
			maxVblankInterrupts--;
		}

		long schedule = getNextVblankSchedule();
		if (schedule != triggerVblankInterruptSchedule) {
			Scheduler scheduler = getScheduler();

			// Remove any action still pending
			if (triggerVblankInterruptSchedule != 0L) {
				scheduler.removeAction(triggerVblankInterruptSchedule, triggerVblankInterruptAction);
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("Scheduling next Vblank at 0x%X, %s", schedule, this));
			}
			triggerVblankInterruptSchedule = schedule;
			scheduler.addAction(triggerVblankInterruptSchedule, triggerVblankInterruptAction);
		}
	}

	public void triggerVblankInterrupt() {
		scheduleNextVblankInterrupt();
		RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_VBLANK_INTR);
	}

	private void startVblankInterrupts() {
		if (triggerVblankInterruptAction == null) {
			triggerVblankInterruptAction = new TriggerVblankInterruptAction();
			triggerVblankInterruptSchedule = 0L;

			scheduleNextVblankInterrupt();
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x04: value = getDisplayRowSync(getTimeMicros()); break;
			case 0x08: value = getDisplaySync(getTimeMicros()); break;
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
		return String.format("MMIOHandlerDisplayController rowSync=0x%X, displaySync=0x%X, baseTime=0x%X, now=0x%X", getDisplayRowSync(getTimeMicros()), getDisplaySync(getTimeMicros()), baseTimeMicros, getNow());
	}
}
