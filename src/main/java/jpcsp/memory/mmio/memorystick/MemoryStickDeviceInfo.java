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
 * The Memory Stick Pro devinfo attribute entry structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/mspro_block.c
 * see "struct mspro_devinfo".
 */
public class MemoryStickDeviceInfo extends pspAbstractMemoryMappedStructure {
	public int cylinders;
	public int heads;
	public int bytesPerTrack;
	public int bytesPerSector;
	public int sectorsPerTrack;
	public final byte[] reserved = new byte[6];

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		cylinders = read16();
		heads = read16();
		bytesPerTrack = read16();
		bytesPerSector = read16();
		sectorsPerTrack = read16();
		read8Array(reserved);
	}

	@Override
	protected void write() {
		write16((short) cylinders);
		write16((short) heads);
		write16((short) bytesPerTrack);
		write16((short) bytesPerSector);
		write16((short) sectorsPerTrack);
		write8Array(reserved);
	}

	@Override
	public int sizeof() {
		return 16;
	}
}
