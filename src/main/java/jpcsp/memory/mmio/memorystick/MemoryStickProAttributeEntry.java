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
package jpcsp.memory.mmio.memorystick;

import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;

/**
 * The Memory Stick Pro attribute entry structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/mspro_block.c
 * see "struct mspro_attr_entry".
 */
public class MemoryStickProAttributeEntry extends pspAbstractMemoryMappedStructure {
	public static final int MSPRO_BLOCK_ID_SYSINFO         = 0x10;
	public static final int MSPRO_BLOCK_ID_MODELNAME       = 0x15;
	public static final int MSPRO_BLOCK_ID_MBR             = 0x20;
	public static final int MSPRO_BLOCK_ID_PBR16           = 0x21;
	public static final int MSPRO_BLOCK_ID_PBR32           = 0x22;
	public static final int MSPRO_BLOCK_ID_SPECFILEVALUES1 = 0x25;
	public static final int MSPRO_BLOCK_ID_SPECFILEVALUES2 = 0x26;
	public static final int MSPRO_BLOCK_ID_DEVINFO         = 0x30;
	public static final int SIZE_OF = 12;
	public int address;
	public int size;
	public int id;
	public final byte[] reserved = new byte[3];

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		address = read32();
		size = read32();
		id = read8();
		read8Array(reserved);
	}

	@Override
	protected void write() {
		write32(address);
		write32(size);
		write8((byte) id);
		write8Array(reserved);
	}

	@Override
	public int sizeof() {
		return SIZE_OF;
	}
}
