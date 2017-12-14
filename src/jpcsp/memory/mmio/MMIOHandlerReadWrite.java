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

import static jpcsp.memory.FastMemory.memory16Mask;
import static jpcsp.memory.FastMemory.memory16Shift;
import static jpcsp.memory.FastMemory.memory8Mask;
import static jpcsp.memory.FastMemory.memory8Shift;

public class MMIOHandlerReadWrite extends MMIOHandlerBase {
	private final int[] memory;

	public MMIOHandlerReadWrite(int baseAddress, int length) {
		super(baseAddress);

		memory = new int[length >> 2];
	}

	public MMIOHandlerReadWrite(int baseAddress, int length, int[] memory) {
		super(baseAddress);

		this.memory = memory;
	}

	public int[] getInternalMemory() {
		return memory;
	}

	@Override
	public int read32(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X)=0x%08X", getPc(), address, memory[(address - baseAddress) >> 2]));
		}

		return memory[(address - baseAddress) >> 2];
	}

	@Override
	public int read16(int address) {
		int data = (memory[(address - baseAddress) >> 2] >> memory16Shift[address & 0x02]) & 0xFFFF;
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read16(0x%08X)=0x%04X", getPc(), address, data));
		}

		return data;
	}

	@Override
	public int read8(int address) {
		int data = (memory[(address - baseAddress) >> 2] >> memory8Shift[address & 0x03]) & 0xFF;
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read8(0x%08X)=0x%02X", getPc(), address, data));
		}
	
		return data;
	}

	@Override
	public void write32(int address, int value) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X)", getPc(), address, value));
		}

		memory[(address - baseAddress) >> 2] = value;
	}

	@Override
	public void write16(int address, short value) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X)", getPc(), address, value & 0xFFFF));
		}

		int index = address & 0x02;
		int memData = (memory[(address - baseAddress) >> 2] & memory16Mask[index]) | ((value & 0xFFFF) << memory16Shift[index]);

		memory[(address - baseAddress) >> 2] = memData;
	}

	@Override
	public void write8(int address, byte value) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write8(0x%08X, 0x%02X)", getPc(), address, value & 0xFF));
		}

		int index = address & 0x03;
		int memData = (memory[(address - baseAddress) >> 2] & memory8Mask[index]) | ((value & 0xFF) << memory8Shift[index]);

		memory[(address - baseAddress) >> 2] = memData;
	}
}
