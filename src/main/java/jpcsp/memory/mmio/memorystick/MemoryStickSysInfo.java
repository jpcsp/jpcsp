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
 * The Memory Stick Pro sysinfo attribute entry structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/mspro_block.c
 * see "struct mspro_sys_info".
 */
public class MemoryStickSysInfo extends pspAbstractMemoryMappedStructure {
	public static final int MEMORY_STICK_CLASS_PRO = 2;
	public int memoryStickClass;
	public int reserved0;
	public int blockSize;
	public int blockCount;
	public int userBlockCount;
	public int pageSize;
	public final byte[] reserved1 = new byte[2];
	public final byte[] assemblyDate = new byte[8];
	public int serialNumber;
	public int assemblyMakerCode;
	public final byte[] assemblyModelCode = new byte[3];
	public int memoryMakerCode;
	public int memoryModelCode;
	public final byte[] reserved2 = new byte[4];
	public int vcc;
	public int vpp;
	public int controllerNumber;
	public int controllerFunction;
	public int startSector;
	public int unitSize;
	public int memoryStickSubClass;
	public final byte[] reserved3 = new byte[4];
	public int interfaceType;
	public int controllerCode;
	public int formatType;
	public int reserved4;
	public int deviceType;
	public final byte[] reserved5 = new byte[7];
	public final byte[] memoryStickProId = new byte[16];
	public final byte[] reserved6 = new byte[16];

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		memoryStickClass = read8();    // Offset 0
		reserved0 = read8();           // Offset 1
		blockSize = read16();          // Offset 2
		blockCount = read16();         // Offset 4
		userBlockCount = read16();     // Offset 6
		pageSize = read16();           // Offset 8
		read8Array(reserved1);         // Offset 10
		read8Array(assemblyDate);      // Offset 12
		serialNumber = read32();       // Offset 20
		assemblyMakerCode = read8();   // Offset 24
		read8Array(assemblyModelCode); // Offset 25
		memoryMakerCode = read16();    // Offset 28
		memoryModelCode = read16();    // Offset 30
		read8Array(reserved2);         // Offset 32
		vcc = read8();                 // Offset 36
		vpp = read8();                 // Offset 37
		controllerNumber = read16();   // Offset 38
		controllerFunction = read16(); // Offset 40
		startSector = read16();        // Offset 42
		unitSize = read16();           // Offset 44
		memoryStickSubClass = read8(); // Offset 46
		read8Array(reserved3);         // Offset 47
		interfaceType = read8();       // Offset 51
		controllerCode = read16();     // Offset 52
		formatType = read8();          // Offset 54
		reserved4 = read8();           // Offset 55
		deviceType = read8();          // Offset 56
		read8Array(reserved5);         // Offset 57
		read8Array(memoryStickProId);  // Offset 64
		read8Array(reserved6);         // Offset 80
	}

	@Override
	protected void write() {
		write8((byte) memoryStickClass);
		write8((byte) reserved0);
		write16((short) blockSize);
		write16((short) blockCount);
		write16((short) userBlockCount);
		write16((short) pageSize);
		write8Array(reserved1);
		write8Array(assemblyDate);
		write32(serialNumber);
		write8((byte) assemblyMakerCode);
		write8Array(assemblyModelCode);
		write16((short) memoryMakerCode);
		write16((short) memoryModelCode);
		write8Array(reserved2);
		write8((byte) vcc);
		write8((byte) vpp);
		write16((short) controllerNumber);
		write16((short) controllerFunction);
		write16((short) startSector);
		write16((short) unitSize);
		write8((byte) memoryStickSubClass);
		write8Array(reserved3);
		write8((byte) interfaceType);
		write16((short) controllerCode);
		write8((byte) formatType);
		write8((byte) reserved4);
		write8((byte) deviceType);
		write8Array(reserved5);
		write8Array(memoryStickProId);
		write8Array(reserved6);
	}

	@Override
	public int sizeof() {
		return 96;
	}
}
