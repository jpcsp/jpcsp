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
 * The Memory Stick boot system entry structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/ms_block.h
 */
public class MemoryStickSystemEntry extends pspAbstractMemoryMappedStructure {
	public final MemoryStickSystemItem disabledBlock = new MemoryStickSystemItem();
	public final MemoryStickSystemItem cisIdi = new MemoryStickSystemItem();
	public final byte reserved[] = new byte[24];

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		read(disabledBlock);
		read(cisIdi);
		read8Array(reserved);
	}

	@Override
	protected void write() {
		write(disabledBlock);
		write(cisIdi);
		write8Array(reserved);
	}

	@Override
	public int sizeof() {
		return disabledBlock.sizeof() + cisIdi.sizeof() + reserved.length;
	}
}
