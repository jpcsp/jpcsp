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

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.ADIF;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.INTAD;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.getInterruptName;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.now;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.notHasBit;
import static jpcsp.util.Utilities.u8;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.State;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class SysconAdConverter implements IState {
	protected Logger log = Nec78k0Processor.log;
	private static final int STATE_VERSION = 0;
	protected final MMIOHandlerSysconFirmwareSfr sfr;
	protected final SysconScheduler scheduler;
	private final AdConverterAction adConverterAction;
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

	public SysconAdConverter(MMIOHandlerSysconFirmwareSfr sfr, SysconScheduler scheduler) {
		this.sfr = sfr;
		this.scheduler = scheduler;
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

	private void updateResult() {
		switch (analogInputChannelSpecification & 0x7) {
			case 5: // Video detect
				// The result will impact the value returned by PSP_SYSCON_CMD_GET_VIDEO_CABLE:
				//      if (result <= 0x1AFF) videoCable = 0x06
				// else if (result <= 0x41FF) videoCable = 0x05
				// else if (result <= 0x69FF) videoCable = 0x01
				// else if (result <= 0x90FF) videoCable = 0x04
				// else if (result <= 0xB7FF) videoCable = 0x03
				// else if (result <= 0xDEFF) videoCable = 0x02
				// else                       videoCable = 0x00 (probably meaning that no video cable is connected)
				result = 0xFF00; // No video cable connected
				break;
			case 6: // Analog X
				State.controller.hleControllerPoll();
				result = u8(State.controller.getLx()) << 8; // 0x8000 means center
				break;
			case 7: // Analog Y
				State.controller.hleControllerPoll();
				result = u8(State.controller.getLy()) << 8; // 0x8000 means center
				break;
			default:
				result = 0x0000;
				break;
		}

		// The result is a 10-bit value, the lowest 6 bits are always 0's
		result &= 0xFFC0;
	}

	private void onConverterAction() {
		if (log.isTraceEnabled()) {
			log.trace(String.format("AD Converter onConverterAction triggering interrupt %s", getInterruptName(INTAD)));
		}

		updateResult();

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
		if ((value & 0x7) < 5) {
			log.error(String.format("AD Converter setAnalogInputChannelSpecification unimplemented 0x%02X", value));
		}
		analogInputChannelSpecification = value;
	}

	public void setPortConfiguration(int value) {
		if (value != 0x00 && value != 0x05) {
			log.error(String.format("AD Converter setPortConfiguration unimplemented 0x%02X", value));
		}

		portConfiguration = value;
	}
}
