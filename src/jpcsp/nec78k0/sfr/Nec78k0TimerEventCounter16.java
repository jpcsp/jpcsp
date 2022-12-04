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
import static jpcsp.util.Utilities.setBit;

import java.io.IOException;

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class Nec78k0TimerEventCounter16 extends AbstractSysconTimerEventCounter {
	private static final int STATE_VERSION = 0;
	private final int interruptFlagCompare0;
	private final int interruptFlagCompare1;
	private int timerModeControl;
	private int prescalerMode;
	private int compareControl;
	private int outputControl;
	private int compare00;
	private int compare01;

	public Nec78k0TimerEventCounter16(Nec78k0Sfr sfr, Nec78k0Scheduler scheduler, String name, int interruptFlagCompare0, int interruptFlagCompare1) {
		super(sfr, scheduler, name, 0xFFFF);
		this.interruptFlagCompare0 = interruptFlagCompare0;
		this.interruptFlagCompare1 = interruptFlagCompare1;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		timerModeControl = stream.readInt();
		prescalerMode = stream.readInt();
		compareControl = stream.readInt();
		outputControl = stream.readInt();
		compare00 = stream.readInt();
		compare01 = stream.readInt();

		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(timerModeControl);
		stream.writeInt(prescalerMode);
		stream.writeInt(compareControl);
		stream.writeInt(outputControl);
		stream.writeInt(compare00);
		stream.writeInt(compare01);

		super.write(stream);
	}

	@Override
	public void reset() {
		timerModeControl = 0x00;
		prescalerMode = 0x00;
		compareControl = 0x00;
		outputControl = 0x00;
		compare00 = 0x0000;
		compare01 = 0x0000;

		super.reset();

		updateCompare();
	}

	@Override
	protected void onTimerCounterOverflow() {
		if (log.isTraceEnabled()) {
			log.trace(String.format("%s onTimerCounterOverflow", name));
		}
		timerModeControl = setBit(timerModeControl, 0);
	}

	@Override
	protected void onTimerCounterCompare0() {
		if (log.isTraceEnabled()) {
			log.trace(String.format("%s onTimerCounterCompare0 triggering interrupt %s", name, getInterruptName(IFtoINT(interruptFlagCompare0))));
		}
		sfr.setInterruptRequest(interruptFlagCompare0);
	}

	@Override
	protected void onTimerCounterCompare1() {
		if (log.isTraceEnabled()) {
			log.trace(String.format("%s onTimerCounterCompare1 triggering interrupt %s", name, getInterruptName(IFtoINT(interruptFlagCompare1))));
		}
		sfr.setInterruptRequest(interruptFlagCompare1);
	}

	public void setTimerModeControl(int value) {
		if (hasBit(value, 1)) {
			log.error(String.format("%s setTimerModeControl unimplemented operation enable 0x%02X", name, value));
		}

		timerModeControl = value;

		if (hasBit(value, 3)) {
			if (hasBit(value, 2)) {
				setMode(false, true);
			} else {
				log.error(String.format("%s setTimerModeControl unimplemented operation enable 0x%02X", name, value));
			}
		} else {
			if (hasBit(value, 2)) {
				setMode(true, false);
			} else {
				disable();
			}
		}
	}

	public int getTimerModeControl() {
		return timerModeControl;
	}

	private void updateCompare() {
		if (hasBit(compareControl, 0)) {
			// Acting as a capture register
			setCompareValue0(-1);
		} else {
			// Acting as a compare register
			setCompareValue0(compare00);
		}

		if (hasBit(compareControl, 2)) {
			// Acting as a capture register
			setCompareValue1(-1);
		} else {
			// Acting as a compare register
			setCompareValue1(compare01);
		}
	}

	public void setCompareControl(int value) {
		compareControl = value;

		updateCompare();
	}

	public void setOutputControl(int value) {
		outputControl = value;
	}

	public void setPrescalerMode(int value) {
		prescalerMode = value;

		// Assume operation at Fprs = 5 MHz
		switch (prescalerMode & 0x3) {
			case 0: setClockStep(200); break; // 5 MHz
			case 1: setClockStep(800); break; // 1.25 MHz 
			case 2: setClockStep(51200); break; // 19.53 kHz
			case 3: setClockStep(0); log.error(String.format("%s setPrescalerMode unimplemented 0x%02X", value)); break;
		}
	}

	public void setCompare00(int value) {
		compare00 = value;

		updateCompare();
	}

	public void setCompare01(int value) {
		compare01 = value;

		updateCompare();
	}
}
