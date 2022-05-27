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
package jpcsp.memory.mmio.wm1801;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class WM1801 implements IState {
	public static Logger log = Logger.getLogger("WM1801");
	private static final int STATE_VERSION = 0;
	private static WM1801 instance;
	private final static int NUMBER_REGISTERS = 128;
	private final int registers[] = new int[NUMBER_REGISTERS];
	private static final int defaultRegisterValues[] = new int[NUMBER_REGISTERS];
	public final static int REGISTER_RESET = 15;

	public static WM1801 getInstance() {
		if (instance == null) {
			instance = new WM1801();
		}
		return instance;
	}

	private WM1801() {
		reset();
	}

    @Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	stream.readInts(registers);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInts(registers);
	}

	public void reset() {
		System.arraycopy(defaultRegisterValues, 0, registers, 0, NUMBER_REGISTERS);
	}

	public void executeTransmitReceiveCommand(int[] transmitData, int[] receiveData) {
		log.error(String.format("Unimplemented executeTransmitReceiveCommand transmitData: 0x%02X 0x%02X", transmitData[0], transmitData[1]));
	}

	public void executeTransmitCommand(int[] transmitData) {
		int register = transmitData[0];
		int value = transmitData[2] | (transmitData[1] << 8);

		if (register >= 0 && register < NUMBER_REGISTERS) {
			setRegisterValue(register, value);
		} else {
			log.error(String.format("executeTransmitCommand unknown register number: register=%d, value=0x%04X on %s", register, value, this));
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("executeTransmitCommand register=%d, value=0x%04X on %s", register, value, this));
		}
	}

	private void setRegisterValue(int register, int value) {
		if (register == REGISTER_RESET) {
			// Writing to this register resets all registers to their default state
			reset();
		} else {
			registers[register] = value;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("WM8750[");

		boolean first = true;
		for (int i = 0; i < NUMBER_REGISTERS; i++) {
			if (registers[i] != defaultRegisterValues[i]) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(String.format("R%d=0x%03X", i, registers[i]));
			}
		}
		sb.append("]");

		return sb.toString();
	}
}
