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
 * The Memory Stick boot system item structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/ms_block.h
 */
public class MemoryStickSystemItem extends pspAbstractMemoryMappedStructure {
	public static final int MS_SYSENT_TYPE_INVALID_BLOCK = 0x01;
	public static final int MS_SYSENT_TYPE_CIS_IDI = 0x0A;
	public int startAddr;
	public int dataSize;
	public int dataTypeId;
	public final byte reserved[] = new byte[3];

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		startAddr = read32();
		dataSize = read32();
		dataTypeId = read8();
		read8Array(reserved);
	}

	@Override
	protected void write() {
		write32(startAddr);
		write32(dataSize);
		write8((byte) dataTypeId);
		write8Array(reserved);
	}

	@Override
	public int sizeof() {
		return 12;
	}
}
