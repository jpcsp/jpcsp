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
package jpcsp.nec78k0.sfr;

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.IFtoINT;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.getInterruptName;
import static jpcsp.util.Utilities.hasBit;

import java.io.IOException;

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class Nec78k0TimerH extends AbstractSysconTimerEventCounter {
	private static final int STATE_VERSION = 0;
	private final int interruptFlagCompare;
	private final int[] countClockSelection;
	private int timerMode;
	private int timerCarrierControl;
	private int compare0;
	private int compare1;

	// Assume operation at Fprs = 5 MHz
	public static final int countClockSelectionH0[] = {
		200,    // 5 MHz
		400,    // 2.5 MHz
		800,    // 1.25 MHz
		12800,  // 78.13 kHz
		204920, // 4.88 kHz
		0,      // Prohibited
		0,      // Prohibited
		0       // Prohibited
	};
	// Assume operation at Fprs = 5 MHz
	public static final int countClockSelectionH1[] = {
		200,     // 5 MHz
		800,     // 1.25 MHz
		3200,    // 312.5 kHz
		12800,   // 78.13 kHz
		819670,  // 1.22 kHz
		531915,  // 1.88 kHz
		2127660, // 0.47 kHz
		4167     // 240 kHz
	};

	public Nec78k0TimerH(Nec78k0Sfr sfr, Nec78k0Scheduler scheduler, String name, int interruptFlagCompare, int[] countClockSelection) {
		super(sfr, scheduler, name, 0xFF);
		this.interruptFlagCompare = interruptFlagCompare;
		this.countClockSelection = countClockSelection;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		timerMode = stream.readInt();
		timerCarrierControl = stream.readInt();
		compare0 = stream.readInt();
		compare1 = stream.readInt();

		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(timerMode);
		stream.writeInt(timerCarrierControl);
		stream.writeInt(compare0);
		stream.writeInt(compare1);

		super.write(stream);
	}

	@Override
	public void reset() {
		timerMode = 0x00;
		timerCarrierControl = 0x00;
		compare0 = 0x00;
		compare1 = 0x00;

		super.reset();

		updateCompare();
	}

	@Override
	protected void onTimerCounterOverflow() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onTimerCounterCompare0() {
		if (log.isTraceEnabled()) {
			log.trace(String.format("%s onTimerCounterCompare0 triggering interrupt %s", name, getInterruptName(IFtoINT(interruptFlagCompare))));
		}

		// An interrupt is generated
		sfr.setInterruptRequest(interruptFlagCompare);

		// and the counter value is cleared
		timerCounter = 0;
	}

	private int getTimerOperationMode() {
		return (timerMode >> 2) & 0x03;
	}

	private boolean isIntervalTimerMode() {
		return getTimerOperationMode() == 0;
	}

	private boolean isPWMTimerMode() {
		return getTimerOperationMode() == 2;
	}

	@Override
	protected void onTimerCounterCompare1() {
		if (isPWMTimerMode()) {
			// TODO
		}
	}

	public void setTimerMode(int value) {
		if (hasBit(value, 0)) {
			log.error(String.format("%s setTimerMode unimplemented output enabled 0x%02X", name, value));
		}
		if (hasBit(value, 1)) {
			log.error(String.format("%s setTimerMode unimplemented output level control 0x%02X", name, value));
		}

		if (!isIntervalTimerMode()) {
			log.error(String.format("%s setTimerMode unimplemented timer operation mode 0x%02X", name, value));
		}

		int clockStep = countClockSelection[(value >> 4) & 0x07];
		setClockStep(clockStep);
		if (clockStep == 0) {
			log.error(String.format("%s setTimerMode unimplemented count clock selection 0x%02X", name, value));
		}

		if (hasBit(value, 7)) {
			setMode(false, true);
		}

		timerMode = value;
	}

	public int getTimerMode() {
		return timerMode;
	}

	public void setTimeCarrierControl(int value) {
		log.error(String.format("%s setTimeCarrierControl unimplemented 0x%02X", name, value));
	}

	private void updateCompare() {
		setCompareValue0(compare0);

		if (isIntervalTimerMode()) {
			// Compare register 1 is not used in interval timer mode
			setCompareValue1(-1);
		} else {
			setCompareValue1(compare1);
		}
	}

	public void setCompare0(int value) {
		compare0 = value;

		updateCompare();
	}

	public void setCompare1(int value) {
		compare1 = value;

		updateCompare();
	}
}
