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
package jpcsp.memory.mmio.wm8750;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.hardware.Model;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * The  WM8750L  is  a  low  power,  high  quality  stereo  CODEC
 * designed for portable digital audio applications.
 *
 * See documentation at
 *    http://hitmen.c02.at/files/docs/psp/WM8750.pdf
 */
public class WM8750 implements IState {
	public static Logger log = Logger.getLogger("WM8750");
	private static final int STATE_VERSION = 0;
	private static WM8750 instance;
	private final int NUMBER_REGISTERS;
	private final int registers[];
	private static final int defaultRegisterValues[] = new int[] {
		0x097, 0x097, 0x079, 0x079, 0x000, 0x008, 0x000, 0x00A, 0x000, 0x000, 0x0FF, 0x0FF, 0x00F, 0x00F, 0x000, 0x000,
		0x000, 0x07B, 0x000, 0x032, 0x000, 0x0C3, 0x0C3, 0x0C0, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000,
		0x000, 0x000, 0x050, 0x050, 0x050, 0x050, 0x050, 0x050, 0x079, 0x079, 0x079,
		// Additional unknown registers in PSP Slim
		0x000, 0x000, 0x000, 0x000, 0x000, 0x000, 0x000
	};
	public final static int REGISTER_RESET = 15;

	public static WM8750 getInstance() {
		if (instance == null) {
			instance = new WM8750();
		}
		return instance;
	}

	private WM8750() {
		NUMBER_REGISTERS = Model.getGeneration() == 1 ? 43 : 50;
		registers = new int[NUMBER_REGISTERS];

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
		// This seems to not be used for WM8750
		log.error(String.format("Unimplemented executeTransmitReceiveCommand transmitData: 0x%02X 0x%02X", transmitData[0], transmitData[1]));
	}

	public void executeTransmitCommand(int[] transmitData) {
		int register = (transmitData[0] >> 1) & 0x7F;
		int value = transmitData[1] | ((transmitData[0] & 0x01) << 8);

		if (register >= 0 && register < NUMBER_REGISTERS) {
			setRegisterValue(register, value);
		} else {
			log.error(String.format("executeTransmitCommand unknown register number: register=%d, value=0x%03X on %s", register, value, this));
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("executeTransmitCommand register=%d, value=0x%03X on %s", register, value, this));
		}
	}

	private void setRegisterValue(int register, int value) {
		if (register == REGISTER_RESET) {
			// Writing to this register resets all registers to their default state
			reset();
		} else {
			registers[register] = value & 0x1FF;
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
