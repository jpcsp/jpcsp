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

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.memory.mmio.syscon.MMIOHandlerSyscon;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class Nec78k0SerialInterfaceCSI1n implements IState {
	private static final int STATE_VERSION = 0;
	protected final Nec78k0Sfr sfr;
	private final String name;
	private final int interruptFlag;
	private Logger log;
	private int operationMode;
	private int clockSelection;
	private int transmitBuffer;
	private int ioShift;
	private int receiveIndex;
	private final int[] receiveBuffer = new int[MMIOHandlerSyscon.MAX_DATA_LENGTH];
	private int sendIndex;
	private final int[] sendBuffer = new int[MMIOHandlerSyscon.MAX_DATA_LENGTH];

	public Nec78k0SerialInterfaceCSI1n(Nec78k0Sfr sfr, String name, int interruptFlag) {
		this.sfr = sfr;
		this.name = name;
		this.interruptFlag = interruptFlag;
		log = sfr.log;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		operationMode = stream.readInt();
		clockSelection = stream.readInt();
		transmitBuffer = stream.readInt();
		ioShift = stream.readInt();
		receiveIndex = stream.readInt();
		stream.readInts(receiveBuffer);
		sendIndex = stream.readInt();
		stream.readInts(sendBuffer);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(operationMode);
		stream.writeInt(clockSelection);
		stream.writeInt(transmitBuffer);
		stream.writeInt(ioShift);
		stream.writeInt(receiveIndex);
		stream.writeInts(receiveBuffer);
		stream.writeInt(sendIndex);
		stream.writeInts(sendBuffer);
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void reset() {
		operationMode = 0x00;
		clockSelection = 0x00;
		transmitBuffer = 0x00;
		ioShift = 0x00;
		receiveIndex = 0;
		Arrays.fill(receiveBuffer, 0);
		sendIndex = 0;
		Arrays.fill(sendBuffer, 0);
	}


	public int getOperationMode() {
		return operationMode;
	}

	public void setOperationMode(int value) {
		operationMode = value;
	}

	public int getClockSelection() {
		return clockSelection;
	}

	public void setClockSelection(int value) {
		clockSelection = value;
	}

	public void setTransmitBuffer(int value) {
		if (sendIndex < sendBuffer.length) {
			sendBuffer[sendIndex++] = value;
		}
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
		if (receiveIndex < receiveBuffer.length) {
			ioShift = receiveBuffer[receiveIndex++];
		} else {
			ioShift = 0x00;
		}

		sfr.setInterruptRequest(interruptFlag);

		if (log.isDebugEnabled()) {
			log.debug(String.format("%s getIOShift 0x%02X", name, ioShift));
		}

		return ioShift;
	}

	public void setReceiveBuffer(int[] data) {
		receiveIndex = 0;
		if (data != null) {
			System.arraycopy(data, 0, receiveBuffer, 0, Math.min(data.length, receiveBuffer.length));
		}
	}

	public int getSendBuffer(int[] data) {
		int length = sendIndex;
		sendIndex = 0;
		System.arraycopy(sendBuffer, 0, data, 0, Math.min(length, data.length));

		return length;
	}
}
