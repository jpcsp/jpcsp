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
 * The Memory Stick Pro attribute structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/mspro_block.c
 * see "struct mspro_attribute".
 */
public class MemoryStickProAttribute extends pspAbstractMemoryMappedStructure {
	public int signature;
	public int version;
	public int count;
	public final byte[] reserved = new byte[11];
	public final MemoryStickProAttributeEntry entries[] = new MemoryStickProAttributeEntry[12];

	public MemoryStickProAttribute() {
		for (int i = 0; i < entries.length; i++) {
			entries[i] = new MemoryStickProAttributeEntry();
		}
	}

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		signature = read16();
		version = read16();
		count = read8();
		read8Array(reserved);
		for (int i = 0; i < entries.length; i++) {
			read(entries[i]);
		}
	}

	@Override
	protected void write() {
		write16((short) signature);
		write16((short) version);
		write8((byte) count);
		write8Array(reserved);
		for (int i = 0; i < entries.length; i++) {
			write(entries[i]);
		}
	}

	@Override
	public int sizeof() {
		return 16 + entries.length * MemoryStickProAttributeEntry.SIZE_OF;
	}
}
