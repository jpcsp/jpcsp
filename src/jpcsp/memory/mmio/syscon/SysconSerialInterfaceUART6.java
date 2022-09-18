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

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.SRIF6;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.STIF6;

import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isRaisingBit;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Serial interface used to communicate with the PSP battery.
 * See the Baryon Sweeper for a simulation of battery responses:
 *     https://github.com/khubik2/pysweeper/blob/master/pysweeper.py
 * 
 * @author gid15
 *
 */
public class SysconSerialInterfaceUART6 implements IState {
	public static Logger log = Nec78k0Processor.log;
	private static final int STATE_VERSION = 0;
	private final MMIOHandlerSysconFirmwareSfr sfr;
	private final ISysconSerialInterface serialInterfaceEmulator;
	private int operationMode;
	private int controlRegister;
	private int clockSelection;
	private int baudRateGeneratorControl;
	private int receptionErrorStatus;

	public SysconSerialInterfaceUART6(MMIOHandlerSysconFirmwareSfr sfr) {
		this.sfr = sfr;

		// When emulating the firmware bootloader,
		// the serial interface is connected to an external system.
		// Otherwise, the serial interface is connected to the PSP battery.
		if (SysconEmulator.firmwareBootloader) {
			serialInterfaceEmulator = new SysconBootloaderEmulator(sfr);
		} else {
			serialInterfaceEmulator = new SysconBatteryEmulator(sfr, this);
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		operationMode = stream.readInt();
		controlRegister = stream.readInt();
		clockSelection = stream.readInt();
		baudRateGeneratorControl = stream.readInt();
		receptionErrorStatus = stream.readInt();
		serialInterfaceEmulator.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(operationMode);
		stream.writeInt(controlRegister);
		stream.writeInt(clockSelection);
		stream.writeInt(baudRateGeneratorControl);
		stream.writeInt(receptionErrorStatus);
		serialInterfaceEmulator.write(stream);
	}

	public void reset() {
		operationMode = 0x01;
		controlRegister = 0x16;
		clockSelection = 0x00;
		baudRateGeneratorControl = 0xFF;
		receptionErrorStatus = 0x00;
		serialInterfaceEmulator.reset();
	}

	public ISysconSerialInterface getConnectedSerialInterface() {
		return serialInterfaceEmulator;
	}

	private boolean isTransmissionEnabled() {
		return hasBit(operationMode, 7) && hasBit(operationMode, 6);
	}

	public boolean isReceptionEnabled() {
		return hasBit(operationMode, 7) && hasBit(operationMode, 5);
	}

	public void setOperationMode(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setOperationMode 0x%02X", value));
		}

		if (isRaisingBit(operationMode, value, 6)) {
			// Starting transmission
			serialInterfaceEmulator.startTransmission();
		} else if (isRaisingBit(operationMode, value, 5)) {
			// Starting reception
			serialInterfaceEmulator.startReception();
		}

		operationMode = value;
	}

	public int getOperationMode() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getOperationMode returning 0x%02X", operationMode));
		}
		return operationMode;
	}

	public int getControlRegister() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getControlRegister returning 0x%02X", controlRegister));
		}
		return controlRegister;
	}

	public void setControlRegister(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setControlRegister 0x%02X", value));
		}

		// Bit 7 is read-only
		value = clearBit(value, 7);

		controlRegister = value;
	}

	public int getClockSelection() {
		return clockSelection;
	}

	public void setClockSelection(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setClockSelection 0x%02X", value));
		}

		clockSelection = value;
	}

	public int getBaudRateGeneratorControl() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getBaudRateGeneratorControl returning 0x%02X", baudRateGeneratorControl));
		}
		return baudRateGeneratorControl;
	}

	public void setBaudRateGeneratorControl(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setBaudRateGeneratorControl 0x%02X", value));
		}

		baudRateGeneratorControl = value;
	}

	public int getReceptionErrorStatus() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getReceptionErrorStatus returning 0x%02X", receptionErrorStatus));
		}
		return receptionErrorStatus;
	}

	public void setTransmitRegister(int value) {
		if (isTransmissionEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("UART6 setTransmitRegister 0x%02X", value));
			}

			serialInterfaceEmulator.transmit(value);

			sfr.setInterruptRequest(STIF6);
		} else {
			log.error(String.format("UART6 setTransmitRegister 0x%02X but transmission not enabled", value));
		}
	}

	public int getReceiveRegister() {
		int value = 0;
		if (isReceptionEnabled()) {
			value = serialInterfaceEmulator.receive();

			sfr.setInterruptRequest(SRIF6);

			if (log.isDebugEnabled()) {
				log.debug(String.format("UART6 getReceiveRegister returning 0x%02X", value));
			}
		} else {
			log.error(String.format("UART6 getReceiveRegister reception not enabled"));
		}

		return value;
	}

	public int getTransmissionStatus() {
		return 0x00;
	}

	public void asyncReceive() {
		sfr.setInterruptRequest(SRIF6);
	}
}
