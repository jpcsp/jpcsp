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

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.SRIF6;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.STIF6;

import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isFallingBit;
import static jpcsp.util.Utilities.isRaisingBit;

import java.io.IOException;

import org.apache.log4j.Logger;

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
public class Nec78k0SerialInterfaceUART6 implements IState {
	private static final int STATE_VERSION = 0;
	private final Nec78k0Sfr sfr;
	private Logger log;
	private Nec78k0SerialInterface serialInterfaceEmulator;
	private int operationMode;
	private int controlRegister;
	private int clockSelection;
	private int baudRateGeneratorControl;
	private int receptionErrorStatus;

	public Nec78k0SerialInterfaceUART6(Nec78k0Sfr sfr) {
		this.sfr = sfr;
		log = sfr.log;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		operationMode = stream.readInt();
		controlRegister = stream.readInt();
		clockSelection = stream.readInt();
		baudRateGeneratorControl = stream.readInt();
		receptionErrorStatus = stream.readInt();
		if (serialInterfaceEmulator != null) {
			serialInterfaceEmulator.read(stream);
		}
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(operationMode);
		stream.writeInt(controlRegister);
		stream.writeInt(clockSelection);
		stream.writeInt(baudRateGeneratorControl);
		stream.writeInt(receptionErrorStatus);
		if (serialInterfaceEmulator != null) {
			serialInterfaceEmulator.write(stream);
		}
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void reset() {
		operationMode = 0x01;
		controlRegister = 0x16;
		clockSelection = 0x00;
		baudRateGeneratorControl = 0xFF;
		receptionErrorStatus = 0x00;
		if (serialInterfaceEmulator != null) {
			serialInterfaceEmulator.reset();
		}
	}

	public void setSerialInterface(Nec78k0SerialInterface serialInterfaceEmulator) {
		this.serialInterfaceEmulator = serialInterfaceEmulator;
	}

	public Nec78k0SerialInterface getConnectedSerialInterface() {
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

		int oldOperationMode = operationMode;
		operationMode = value;

		if (isRaisingBit(oldOperationMode, operationMode, 6)) {
			// Starting transmission
			serialInterfaceEmulator.startTransmission();
		} else if (isFallingBit(oldOperationMode, operationMode, 6)) {
			// Ending transmission
			serialInterfaceEmulator.endTransmission();
		}

		if (isRaisingBit(oldOperationMode, operationMode, 5)) {
			// Starting reception
			serialInterfaceEmulator.startReception();
		} else if (isFallingBit(oldOperationMode, operationMode, 5)) {
			// Ending reception
			serialInterfaceEmulator.endReception();
		}
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

			if (serialInterfaceEmulator.hasReceived()) {
				sfr.setInterruptRequest(SRIF6);
			}

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
