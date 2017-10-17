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

import org.apache.log4j.Logger;

import jpcsp.Emulator;

public class MMIOHandlerBase implements IMMIOHandler {
	public static Logger log = Logger.getLogger("mmio");
	protected final int baseAddress;

	public MMIOHandlerBase(int baseAddress) {
		this.baseAddress = baseAddress;
	}

	@Override
	public int read8(int address) {
		log.error(String.format("0x%08X - Unimplemented read8(0x%08X)", Emulator.getProcessor().cpu.pc, address));
		return 0;
	}

	@Override
	public int read16(int address) {
		log.error(String.format("0x%08X - Unimplemented read16(0x%08X)", Emulator.getProcessor().cpu.pc, address));
		return 0;
	}

	@Override
	public int read32(int address) {
		log.error(String.format("0x%08X - Unimplemented read32(0x%08X)", Emulator.getProcessor().cpu.pc, address));
		return 0;
	}

	@Override
	public void write8(int address, byte value) {
		log.error(String.format("0x%08X - Unimplemented write8(0x%08X, 0x%02X)", Emulator.getProcessor().cpu.pc, address, value));
	}

	@Override
	public void write16(int address, short value) {
		log.error(String.format("0x%08X - Unimplemented write16(0x%08X, 0x%04X)", Emulator.getProcessor().cpu.pc, address, value));
	}

	@Override
	public void write32(int address, int value) {
		log.error(String.format("0x%08X - Unimplemented write32(0x%08X, 0x%08X)", Emulator.getProcessor().cpu.pc, address, value));
	}
}
