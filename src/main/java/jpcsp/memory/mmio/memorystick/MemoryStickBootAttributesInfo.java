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
 * The Memory Stick boot attributes info structure.
 * Based on information from
 * https://github.com/torvalds/linux/blob/master/drivers/memstick/core/ms_block.h
 */
public class MemoryStickBootAttributesInfo extends pspAbstractMemoryMappedStructure {
	public static final int MS_SYSINF_MSCLASS_TYPE_1 = 1;
	public static final int MS_SYSINF_CARDTYPE_RDONLY = 1;
	public static final int MS_SYSINF_CARDTYPE_RDWR = 2;
	public static final int MS_SYSINF_CARDTYPE_HYBRID = 3;
	public static final int MS_SYSINF_FORMAT_FAT = 1;
	public static final int MS_SYSINF_USAGE_GENERAL = 0;
	public int memorystickClass;
	public int cardType;
	public int blockSize;
	public int numberOfBlocks;
	public int numberOfEffectiveBlocks;
	public int pageSize;
	public int extraDataSize;
	public int securitySupport;
	public final byte assemblyTime[] = new byte[8];
	public int formatUniqueValue3;
	public final byte serialNumber[] = new byte[3];
	public int assemblyManufacturerCode;
	public final byte assemblyModelCode[] = new byte[3];
	public int memoryManufacturerCode;
	public int memoryDeviceCode;
	public int implementedCapacity;
	public final byte formatUniqueValue4[] = new byte[2];
	public int vcc;
	public int vpp;
	public int controllerNumber;
	public int controllerFunction;
	public final byte reserved0[] = new byte[9];
	public int transferSupporting;
	public int formatUniqueValue5;
	public int formatType;
	public int memorystickApplication;
	public int deviceType;
	public final byte reserved1[] = new byte[22];
	public final byte formatUniqueValue6[] = new byte[2];
	public final byte reserved2[] = new byte[15];

	@Override
	protected boolean isBigEndian() {
		return true;
	}

	@Override
	protected void read() {
		memorystickClass = read8();
		cardType = read8();
		blockSize = read16();
		numberOfBlocks = read16();
		numberOfEffectiveBlocks = read16();
		pageSize = read16();
		extraDataSize = read8();
		securitySupport = read8();
		read8Array(assemblyTime);
		formatUniqueValue3 = read8();
		read8Array(serialNumber);
		assemblyManufacturerCode = read8();
		read8Array(assemblyModelCode);
		memoryManufacturerCode = read16();
		memoryDeviceCode = read16();
		implementedCapacity = read16();
		read8Array(formatUniqueValue4);
		vcc = read8();
		vpp = read8();
		controllerNumber = read16();
		controllerFunction = read16();
		read8Array(reserved0);
		transferSupporting = read8();
		formatUniqueValue5 = read16();
		formatType = read8();
		memorystickApplication = read8();
		deviceType = read8();
		read8Array(reserved1);
		read8Array(formatUniqueValue6);
		read8Array(reserved2);
	}

	@Override
	protected void write() {
		write8((byte) memorystickClass);
		write8((byte) cardType);
		write16((short) blockSize);
		write16((short) numberOfBlocks);
		write16((short) numberOfEffectiveBlocks);
		write16((short) pageSize);
		write8((byte) extraDataSize);
		write8((byte) securitySupport);
		write8Array(assemblyTime);
		write8((byte) formatUniqueValue3);
		write8Array(serialNumber);
		write8((byte) assemblyManufacturerCode);
		write8Array(assemblyModelCode);
		write16((short) memoryManufacturerCode);
		write16((short) memoryDeviceCode);
		write16((short) implementedCapacity);
		write8Array(formatUniqueValue4);
		write8((byte) vcc);
		write8((byte) vpp);
		write16((short) controllerNumber);
		write16((short) controllerFunction);
		write8Array(reserved0);
		write8((byte) transferSupporting);
		write16((short) formatUniqueValue5);
		write8((byte) formatType);
		write8((byte) memorystickApplication);
		write8((byte) deviceType);
		write8Array(reserved1);
		write8Array(formatUniqueValue6);
		write8Array(reserved2);
	}

	@Override
	public int sizeof() {
		return 96;
	}
}
