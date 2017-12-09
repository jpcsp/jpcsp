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
	public static Logger log = MEProcessor.log;
	public static final int START_ME_RAM = 0x00000000;
	public static final int END_ME_RAM = 0x001FFFFF;
	public static final int SIZE_ME_RAM = END_ME_RAM - START_ME_RAM + 1;

	public MEMemory(Memory mem) {
		super(mem);

		MMIOHandlerReadWrite handler = new MMIOHandlerReadWrite(START_ME_RAM, SIZE_ME_RAM);
		addHandler(START_ME_RAM, SIZE_ME_RAM, handler);
		// The same memory is also visible at address range 0x40000000-0x401FFFFF
		addHandler(START_ME_RAM | 0x40000000, SIZE_ME_RAM, new MMIOHandlerReadWrite(START_ME_RAM | 0x40000000, SIZE_ME_RAM, handler.getInternalMemory()));
		// The same memory is also visible at address range 0x80000000-0x801FFFFF
		addHandler(START_ME_RAM | 0x80000000, SIZE_ME_RAM, new MMIOHandlerReadWrite(START_ME_RAM | 0x80000000, SIZE_ME_RAM, handler.getInternalMemory()));

		// This address range is not the VRAM, but probably some unknown MMIO
		addHandlerRW(0x44000000, 0x100000);
	}
}
