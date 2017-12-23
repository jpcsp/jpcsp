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
package jpcsp.mediaengine;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.memory.mmio.MMIO;
import jpcsp.memory.mmio.MMIOHandlerReadWrite;

/**
 * The PSP Media Engine memory:
 * - 0x00000000 - 0x001FFFFF (2MB): ME internal RAM
 * - access to the main memory
 * - access to the MMIO
 *
 * @author gid15
 *
 */
public class MEMemory extends MMIO {
	public static final int START_ME_RAM = 0x00000000;
	public static final int END_ME_RAM = 0x001FFFFF;
	public static final int SIZE_ME_RAM = END_ME_RAM - START_ME_RAM + 1;

	public MEMemory(Memory mem, Logger log) {
		super(mem);

		MMIOHandlerReadWrite handler = new MMIOHandlerReadWrite(START_ME_RAM, SIZE_ME_RAM);
		handler.setLogger(log);
		addHandler(START_ME_RAM, SIZE_ME_RAM, handler);

		// The same memory is also visible at address range 0x40000000-0x401FFFFF
		addHandler(handler, 0x40000000, log);

		// The same memory is also visible at address range 0x80000000-0x801FFFFF
		addHandler(handler, 0x80000000, log);

		// The same memory is also visible at address range 0xA0000000-0xA01FFFFF
		addHandler(handler, 0xA0000000, log);

		// This address range is not the VRAM, but probably some unknown MMIO
		addHandler(0x44000000, 0x200000, new MMIOHandlerMeBase(0x44000000));
//		addHandler(0x44000000, 0x100000, new MMIOHandlerMe(0x44000000));
		addHandler(0x440FF000, 0x2C, new MMIOHandlerMe0FF000(0x440FF000));
		addHandler(0x44100000, 0x40, new MMIOHandlerMeDecoderQuSpectra(0x44100000));
	}

	private void addHandler(MMIOHandlerReadWrite handler, int address, Logger log) {
		MMIOHandlerReadWrite handler2 = new MMIOHandlerReadWrite(START_ME_RAM | address, SIZE_ME_RAM, handler.getInternalMemory());
		handler2.setLogger(log);
		addHandler(START_ME_RAM | address, SIZE_ME_RAM, handler2);
	}
}
