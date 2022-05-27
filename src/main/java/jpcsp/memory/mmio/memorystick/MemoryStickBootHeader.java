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
 * The Memory Stick boot header structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/ms_block.h
 */
public class MemoryStickBootHeader extends pspAbstractMemoryMappedStructure {
	public static final int MS_BOOT_BLOCK_ID = 0x0001;
	public static final int MS_BOOT_BLOCK_FORMAT_VERSION = 0x0100;
	public static final int MS_BOOT_BLOCK_DATA_ENTRIES = 2;
	public int blockId;
	public int formatVersion;
	public final byte reserved0[] = new byte[184];
	public int numberOfDataEntry;
	public final byte reserved1[] = new byte[179];

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		blockId = read16();
		formatVersion = read16();
		read8Array(reserved0);
		numberOfDataEntry = read8();
		read8Array(reserved1);
	}

	@Override
	protected void write() {
		write16((short) blockId);
		write16((short) formatVersion);
		write8Array(reserved0);
		write8((byte) numberOfDataEntry);
		write8Array(reserved1);
	}

	@Override
	public int sizeof() {
		return 368;
	}
}
