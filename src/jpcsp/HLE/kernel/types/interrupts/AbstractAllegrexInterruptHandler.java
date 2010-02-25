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
package jpcsp.HLE.kernel.types.interrupts;

import jpcsp.Allegrex.CpuState;

public class AbstractAllegrexInterruptHandler {
	private int address;
	private int[] arguments = new int[4];
	private int numberArguments;

	public AbstractAllegrexInterruptHandler(int address) {
		this.address = address;
		numberArguments = 0;
	}

	public AbstractAllegrexInterruptHandler(int address, int argument0) {
		this.address = address;
		arguments[0] = argument0;
		numberArguments = 1;
	}

	public AbstractAllegrexInterruptHandler(int address, int argument0, int argument1) {
		this.address = address;
		arguments[0] = argument0;
		arguments[1] = argument1;
		numberArguments = 2;
	}

	public AbstractAllegrexInterruptHandler(int address, int argument0, int argument1, int argument2) {
		this.address = address;
		arguments[0] = argument0;
		arguments[1] = argument1;
		arguments[2] = argument2;
		numberArguments = 3;
	}

	public AbstractAllegrexInterruptHandler(int address, int argument0, int argument1, int argument2, int argument3) {
		this.address = address;
		arguments[0] = argument0;
		arguments[1] = argument1;
		arguments[2] = argument2;
		arguments[3] = argument3;
		numberArguments = 4;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public int getArgument(int index) {
		return arguments[index];
	}

	public void setArgument(int index, int argument) {
		arguments[index] = argument;

		if (index >= numberArguments) {
			numberArguments = index + 1;
		}
	}

	public int getNumberArguments() {
		return numberArguments;
	}

	public void copyArgumentsToCpu(CpuState cpu) {
		for (int i = 0; i < numberArguments; i++) {
			cpu.gpr[4 + i] = arguments[i];
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append(String.format("0x%08X(", getAddress()));
		for (int i = 0; i < numberArguments; i++) {
			if (i > 0) {
				result.append(",");
			}
			result.append(String.format("0x%08X", getArgument(i)));
		}
		result.append(")");

		return result.toString();
	}
}
