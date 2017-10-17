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
package jpcsp.memory.mmio;

import jpcsp.Emulator;

public class MMIOHandlerReadWrite extends MMIOHandlerBase {
	private final int[] memory;

	public MMIOHandlerReadWrite(int baseAddress, int length) {
		super(baseAddress);

		memory = new int[length >> 2];
	}

	@Override
	public int read32(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", Emulator.getProcessor().cpu.pc, address, memory[(address - baseAddress) >> 2]));
		}

		return memory[(address - baseAddress) >> 2];
	}

	@Override
	public void write32(int address, int value) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X)", Emulator.getProcessor().cpu.pc, address, value));
		}

		memory[(address - baseAddress) >> 2] = value;
	}
}
