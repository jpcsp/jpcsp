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
 * The Memory Stick boot page structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/ms_block.h
 */
public class MemoryStickBootPage extends pspAbstractMemoryMappedStructure {
	public final MemoryStickBootHeader header = new MemoryStickBootHeader();
	public final MemoryStickSystemEntry entry = new MemoryStickSystemEntry();
	public final MemoryStickBootAttributesInfo attr = new MemoryStickBootAttributesInfo();

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		read(header);
		read(entry);
		read(attr);
	}

	@Override
	protected void write() {
		write(header);
		write(entry);
		write(attr);
	}

	@Override
	public int sizeof() {
		// Will return 512
		return header.sizeof() + entry.sizeof() + attr.sizeof();
	}
}
