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

import static jpcsp.util.Utilities.hasBit;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class SysconSerialInterfaceCSI1n implements IState {
	protected Logger log = Nec78k0Processor.log;
	private static final int STATE_VERSION = 0;
	protected final MMIOHandlerSysconFirmwareSfr sfr;
	private final String name;
	private final int interruptFlag;
	private int operationMode;
	private int clockSelection;
	private int transmitBuffer;
	private int ioShift;
	private int index;
	private int[] buffer;

	public SysconSerialInterfaceCSI1n(MMIOHandlerSysconFirmwareSfr sfr, String name, int interruptFlag) {
		this.sfr = sfr;
		this.name = name;
		this.interruptFlag = interruptFlag;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		operationMode = stream.readInt();
		clockSelection = stream.readInt();
		transmitBuffer = stream.readInt();
		ioShift = stream.readInt();
		index = stream.readInt();
		buffer = stream.readIntsWithLength();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(operationMode);
		stream.writeInt(clockSelection);
		stream.writeInt(transmitBuffer);
		stream.writeInt(ioShift);
		stream.writeInt(index);
		stream.writeIntsWithLength(buffer);
	}

	public void reset() {
		operationMode = 0x00;
		clockSelection = 0x00;
		transmitBuffer = 0x00;
		ioShift = 0x00;
		index = 0;
	}


	public int getOperationMode() {
		return operationMode;
	}

	public void setOperationMode(int value) {
		if (hasBit(value, 7)) {
			log.error(String.format("%s setOperationMode unimplemented operation in 3-wire serial I/O mode 0x%02X", name, value));
		}
		if (hasBit(value, 6)) {
			log.error(String.format("%s setOperationMode unimplemented transmit/receive mode 0x%02X", name, value));
		}

		operationMode = value;
	}

	public int getClockSelection() {
		return clockSelection;
	}

	public void setClockSelection(int value) {
		clockSelection = value;
	}

	public void setTransmitBuffer(int value) {
		transmitBuffer = value;

		sfr.setInterruptRequest(interruptFlag);

		if (log.isDebugEnabled()) {
			log.debug(String.format("%s setTransmitBuffer 0x%02X", name, value));
		}
	}

	public int getTransmitBuffer() {
		return transmitBuffer;
	}

	public int getIOShift() {
		if (index < buffer.length) {
			ioShift = buffer[index++];
		} else {
			ioShift = 0x00;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("%s getIOShift 0x%02X", name, ioShift));
		}

		return ioShift;
	}

	public void setBuffer(int[] buffer) {
		index = 0;
		this.buffer = buffer;
	}
}
