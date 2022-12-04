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

import static jpcsp.nec78k0.sfr.Nec78k0Sfr.ADIF;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.INTAD;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.getInterruptName;
import static jpcsp.nec78k0.sfr.Nec78k0Sfr.now;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.notHasBit;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.IAction;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class Nec78k0AdConverter implements IState {
	private static final int STATE_VERSION = 0;
	private final Nec78k0Sfr sfr;
	private final Nec78k0Scheduler scheduler;
	private final AdConverterAction adConverterAction;
	protected Logger log;
	private int mode;
	private int result;
	private int clockStep;
	private int analogInputChannelSpecification;
	private int portConfiguration;

	private class AdConverterAction implements IAction {
		@Override
		public void execute() {
			onConverterAction();
		}
	}

	public Nec78k0AdConverter(Nec78k0Sfr sfr, Nec78k0Scheduler scheduler) {
		this.sfr = sfr;
		this.scheduler = scheduler;
		log = sfr.log;
		adConverterAction = new AdConverterAction();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		mode = stream.readInt();
		analogInputChannelSpecification = stream.readInt();
		portConfiguration = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(mode);
		stream.writeInt(analogInputChannelSpecification);
		stream.writeInt(portConfiguration);
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void reset() {
		mode = 0x00;
		result = 0x0000;
		analogInputChannelSpecification = 0x00;
		portConfiguration = 0x00;

		updateScheduler();
	}

	private void updateScheduler() {
		scheduler.removeAction(adConverterAction);

		if (notHasBit(mode, 7)) {
			return;
		}

		long schedule = now() + clockStep;
		scheduler.addAction(schedule, adConverterAction);
	}

	protected int updateResult(int inputChannel) {
		return 0x0000;
	}

	private void onConverterAction() {
		if (log.isTraceEnabled()) {
			log.trace(String.format("AD Converter onConverterAction triggering interrupt %s", getInterruptName(INTAD)));
		}

		result = updateResult(analogInputChannelSpecification & 0x07);
		// The result is a 10-bit value, the lowest 6 bits are always 0's
		result &= 0xFFC0;

		sfr.setInterruptRequest(ADIF);

		updateScheduler();
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int value) {
		if (hasBit(value, 0)) {
			log.error(String.format("AD Converter setMode unimplemented comparator operation 0x%02X", value));
		}

		mode = value;

		switch ((mode >> 1) & 0x1F) {
			case 0x01: clockStep = 24; break; // 24us?
			case 0x11: clockStep = 24; break; // 24us
			case 0x02: clockStep = 24; break; // Invalid value with LV1=1, according to NEC documentation
			case 0x0D: clockStep = 32; break; // 32us
			default:
				clockStep = 1000;
				log.error(String.format("AD Converter setMode unimplemented 0x%02X", value));
				break;
		}

		updateScheduler();
	}

	public int getResult() {
		return result;
	}

	public void setAnalogInputChannelSpecification(int value) {
		analogInputChannelSpecification = value;
	}

	public void setPortConfiguration(int value) {
		if (value != 0x00 && value != 0x02 && value != 0x03 && value != 0x04 && value != 0x05) {
			log.error(String.format("AD Converter setPortConfiguration unimplemented 0x%02X", value));
		}

		portConfiguration = value;
	}
}
