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

public class MMIOHandlerReadWrite8 extends MMIOHandlerBase {
	private final int[] memory;

	public MMIOHandlerReadWrite8(int baseAddress, int length) {
		super(baseAddress);

		memory = new int[length];
	}

	@Override
	public int read8(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read8(0x%08X) returning 0x%02X", getPc(), address, memory[address - baseAddress]));
		}

		return memory[address - baseAddress];
	}

	@Override
	public void write8(int address, byte value) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write8(0x%08X, 0x%02X)", getPc(), address, value & 0xFF));
		}

		memory[address - baseAddress] = value & 0xFF;
	}
}
