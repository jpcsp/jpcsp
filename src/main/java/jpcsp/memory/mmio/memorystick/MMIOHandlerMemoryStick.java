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

import static jpcsp.HLE.Modules.sceMSstorModule;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MSCM0_INTR;
import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_SEEK_SET;
import static jpcsp.util.Utilities.endianSwap16;

import java.io.IOException;

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.modules.sceMSstor;
import jpcsp.hardware.MemoryStick;

import static jpcsp.memory.mmio.memorystick.MemoryStickBootAttributesInfo.MS_SYSINF_CARDTYPE_RDWR;
import static jpcsp.memory.mmio.memorystick.MemoryStickBootAttributesInfo.MS_SYSINF_FORMAT_FAT;
import static jpcsp.memory.mmio.memorystick.MemoryStickBootAttributesInfo.MS_SYSINF_MSCLASS_TYPE_1;
import static jpcsp.memory.mmio.memorystick.MemoryStickBootAttributesInfo.MS_SYSINF_USAGE_GENERAL;
import static jpcsp.memory.mmio.memorystick.MemoryStickBootHeader.MS_BOOT_BLOCK_DATA_ENTRIES;
import static jpcsp.memory.mmio.memorystick.MemoryStickBootHeader.MS_BOOT_BLOCK_FORMAT_VERSION;
import static jpcsp.memory.mmio.memorystick.MemoryStickBootHeader.MS_BOOT_BLOCK_ID;
import static jpcsp.memory.mmio.memorystick.MemoryStickProAttributeEntry.MSPRO_BLOCK_ID_DEVINFO;
import static jpcsp.memory.mmio.memorystick.MemoryStickProAttributeEntry.MSPRO_BLOCK_ID_MBR;
import static jpcsp.memory.mmio.memorystick.MemoryStickProAttributeEntry.MSPRO_BLOCK_ID_PBR32;
import static jpcsp.memory.mmio.memorystick.MemoryStickProAttributeEntry.MSPRO_BLOCK_ID_SYSINFO;
import static jpcsp.memory.mmio.memorystick.MemoryStickSysInfo.MEMORY_STICK_CLASS_PRO;
import static jpcsp.memory.mmio.memorystick.MemoryStickSystemItem.MS_SYSENT_TYPE_CIS_IDI;
import static jpcsp.memory.mmio.memorystick.MemoryStickSystemItem.MS_SYSENT_TYPE_INVALID_BLOCK;

import jpcsp.memory.IntArrayMemory;
import jpcsp.memory.mmio.MMIOHandlerBaseMemoryStick;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

/**
 * MMIO for Memory Stick.
 * Based on information from
 *     https://github.com/torvalds/linux/tree/master/drivers/memstick/core
 * and https://github.com/torvalds/linux/blob/master/drivers/usb/storage/ene_ub6250.c
 *
 * @author gid15
 *
 */
public class MMIOHandlerMemoryStick extends MMIOHandlerBaseMemoryStick {
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS = 0xBD200000;
	private static MMIOHandlerMemoryStick instance;
	private static final boolean simulateMemoryStickPro = true;
	private final MemoryStickBootPage bootPage = new MemoryStickBootPage();
	private final IntArrayMemory bootPageMemory = new IntArrayMemory(new int[bootPage.sizeof() >> 2]);
	private final MemoryStickBootPage bootPageBackup = new MemoryStickBootPage();
	private final IntArrayMemory bootPageBackupMemory = new IntArrayMemory(new int[bootPageBackup.sizeof() >> 2]);
	private final IntArrayMemory disabledBlocksPageMemory = new IntArrayMemory(new int[PAGE_SIZE >> 2]);
	private final MemoryStickProAttribute msproAttribute = new MemoryStickProAttribute();
	private boolean msproAttributeMemoryInitialized = false;
	private final static int DISABLED_BLOCKS_PAGE = 1;
	private final static int CIS_IDI_PAGE = 2;
	private final static long MAX_MEMORY_STICK_SIZE = 128L * 1024 * 1024; // The maximum size of a Memory Stick (i.e. non-PRO) is 128MB
	private int BLOCK_SIZE;
	private int NUMBER_OF_PHYSICAL_BLOCKS;
	private int NUMBER_OF_LOGICAL_BLOCKS;
	private int FIRST_PAGE_LBA;
	private int NUMBER_OF_PAGES;

	public static MMIOHandlerMemoryStick getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerMemoryStick(BASE_ADDRESS);
		}

		return instance;
	}

	private MMIOHandlerMemoryStick(int baseAddress) {
		super(baseAddress);

		log = sceMSstor.log;

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		BLOCK_SIZE = stream.readInt();
		NUMBER_OF_PHYSICAL_BLOCKS = stream.readInt();
		NUMBER_OF_LOGICAL_BLOCKS = stream.readInt();
		FIRST_PAGE_LBA = stream.readInt();
		NUMBER_OF_PAGES = stream.readInt();
		msproAttributeMemoryInitialized = false;
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(BLOCK_SIZE);
		stream.writeInt(NUMBER_OF_PHYSICAL_BLOCKS);
		stream.writeInt(NUMBER_OF_LOGICAL_BLOCKS);
		stream.writeInt(FIRST_PAGE_LBA);
		stream.writeInt(NUMBER_OF_PAGES);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		Modules.sceMSstorModule.reset();

		long totalSize = MemoryStick.getTotalSize();

		if (!simulateMemoryStickPro) {
			if (totalSize > MAX_MEMORY_STICK_SIZE) {
				totalSize = MAX_MEMORY_STICK_SIZE;
				MemoryStick.setTotalSize(MAX_MEMORY_STICK_SIZE);

				if (log.isDebugEnabled()) {
					log.debug(String.format("Limiting the size of the Memory Stick (i.e. non-PRO) to %s", MemoryStick.getSizeKbString((int) (totalSize / 1024))));
				}
			}
		}

		long totalNumberOfPages = totalSize / PAGE_SIZE;
		// 16 pages per block is only valid up to 4MB Memory Stick
		if (!simulateMemoryStickPro && totalNumberOfPages <= 8192L) {
			PAGES_PER_BLOCK = 16;
			NUMBER_OF_PHYSICAL_BLOCKS = (int) (totalNumberOfPages / PAGES_PER_BLOCK);
		} else {
			for (PAGES_PER_BLOCK = 32; PAGES_PER_BLOCK < 0x8000; PAGES_PER_BLOCK <<= 1) {
				NUMBER_OF_PHYSICAL_BLOCKS = (int) (totalNumberOfPages / PAGES_PER_BLOCK);
				if (NUMBER_OF_PHYSICAL_BLOCKS < 0x10000) {
					break;
				}
			}
		}
		BLOCK_SIZE = PAGES_PER_BLOCK * PAGE_SIZE / 1024; // Number of KB per block
		NUMBER_OF_LOGICAL_BLOCKS = NUMBER_OF_PHYSICAL_BLOCKS - (NUMBER_OF_PHYSICAL_BLOCKS / 512 * 16);
		FIRST_PAGE_LBA = 2 * PAGES_PER_BLOCK;
		NUMBER_OF_PAGES = (NUMBER_OF_PHYSICAL_BLOCKS / 512 * 496 - 2) * BLOCK_SIZE * 2;
		if (!simulateMemoryStickPro) {
			if (NUMBER_OF_PAGES > 0x3DF00) {
				NUMBER_OF_PAGES = 0x3DF00; // For 128MB Memory Stick
			} else if (NUMBER_OF_PAGES > 0x1EF80) {
				NUMBER_OF_PAGES = 0x1EF80; // For 64MB Memory Stick
			}
		}
		NUMBER_OF_PAGES -= FIRST_PAGE_LBA;
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerMemoryStick.reset totalSize=0x%X(%s), pagesPerBlock=0x%X, numberOfPhysicalBlocks=0x%X", totalSize, MemoryStick.getSizeKbString((int) (totalSize / 1024)), PAGES_PER_BLOCK, NUMBER_OF_PHYSICAL_BLOCKS));
		}

		if (!simulateMemoryStickPro) {
			if (BLOCK_SIZE != 8 && BLOCK_SIZE != 16) {
				log.error(String.format("The size of a Memory Stick (i.e. non-PRO) is limited to 512MB, the current size of %s cannot be supported", MemoryStick.getSizeKbString((int) (totalSize / 1024))));
			}
		}

		if (MemoryStick.isInserted()) {
			status |= MS_STATUS_READY | 0x0020;
		}
		if (MemoryStick.isLocked()) {
			registers[MS_STATUS_REG_ADDRESS] |= MS_STATUS_REG_READONLY;
		}

		if (simulateMemoryStickPro) {
			registers[MS_TYPE_ADDRESS] = MS_TYPE_MEMORY_STICK_PRO;
			registers[MS_SYSTEM_ADDRESS] = MS_SYSTEM_SERIAL_MODE;
		}

		bootPage.header.blockId = MS_BOOT_BLOCK_ID;
		bootPage.header.formatVersion = MS_BOOT_BLOCK_FORMAT_VERSION;
		bootPage.header.numberOfDataEntry = MS_BOOT_BLOCK_DATA_ENTRIES;
		bootPage.entry.disabledBlock.startAddr = DISABLED_BLOCKS_PAGE * PAGE_SIZE - bootPage.sizeof();
		bootPage.entry.disabledBlock.dataSize = 4;
		bootPage.entry.disabledBlock.dataTypeId = MS_SYSENT_TYPE_INVALID_BLOCK;
		bootPage.entry.cisIdi.startAddr = CIS_IDI_PAGE * PAGE_SIZE - bootPage.sizeof();
		bootPage.entry.cisIdi.dataSize = PAGE_SIZE;
		bootPage.entry.cisIdi.dataTypeId = MS_SYSENT_TYPE_CIS_IDI;
		bootPage.attr.memorystickClass = MS_SYSINF_MSCLASS_TYPE_1; // must be 1
		bootPage.attr.cardType = MS_SYSINF_CARDTYPE_RDWR;
		bootPage.attr.blockSize = BLOCK_SIZE; // Number of KB per block
		bootPage.attr.numberOfBlocks = NUMBER_OF_PHYSICAL_BLOCKS; // Number of physical blocks
		bootPage.attr.numberOfEffectiveBlocks = NUMBER_OF_LOGICAL_BLOCKS; // Number of logical blocks
		bootPage.attr.pageSize = PAGE_SIZE; // Must be 0x200
		bootPage.attr.extraDataSize = 0x10;
		bootPage.attr.securitySupport = 0x01; // 1 means no security support
		bootPage.attr.formatUniqueValue4[0] = 1;
		bootPage.attr.formatUniqueValue4[1] = 1;
		bootPage.attr.transferSupporting = 0;
		bootPage.attr.formatType = MS_SYSINF_FORMAT_FAT;
		bootPage.attr.memorystickApplication = MS_SYSINF_USAGE_GENERAL;
		bootPage.attr.deviceType = 0;
		bootPage.write(bootPageMemory);

		bootPageBackup.header.blockId = MS_BOOT_BLOCK_ID;
		bootPageBackup.write(bootPageBackupMemory);

		disabledBlocksPageMemory.write16(0, (short) endianSwap16(0x0000));
		disabledBlocksPageMemory.write16(2, (short) endianSwap16(0x0001));
		disabledBlocksPageMemory.write16(4, (short) endianSwap16(NUMBER_OF_PHYSICAL_BLOCKS - 1));
	}

	@Override
	protected void initMsproAttributeMemory() {
		if (simulateMemoryStickPro && !msproAttributeMemoryInitialized) {
			msproAttribute.signature = 0xA5C3;
			msproAttribute.count = 0;
			int entryAddress = 0x1A0; // Only accepting attribute entries starting at that address

			MemoryStickSysInfo memoryStickSysInfo = new MemoryStickSysInfo();
			memoryStickSysInfo.memoryStickClass = MEMORY_STICK_CLASS_PRO;
			memoryStickSysInfo.blockSize = PAGES_PER_BLOCK;
			memoryStickSysInfo.blockCount = NUMBER_OF_PHYSICAL_BLOCKS;
			memoryStickSysInfo.userBlockCount = NUMBER_OF_LOGICAL_BLOCKS;
			memoryStickSysInfo.unitSize = PAGE_SIZE;
			memoryStickSysInfo.deviceType = 0;
			memoryStickSysInfo.interfaceType = 1;
			memoryStickSysInfo.memoryStickSubClass = 0;
			entryAddress = addMsproAttributeEntry(entryAddress, MSPRO_BLOCK_ID_SYSINFO, memoryStickSysInfo);

			MemoryStickDeviceInfo memoryStickDeviceInfo = new MemoryStickDeviceInfo();
			entryAddress = addMsproAttributeEntry(entryAddress, MSPRO_BLOCK_ID_DEVINFO, memoryStickDeviceInfo);

			MemoryStickMbr memoryStickMbr = new MemoryStickMbr();
			entryAddress = addMsproAttributeEntry(entryAddress, MSPRO_BLOCK_ID_MBR, memoryStickMbr);

			MemoryStickPbr32 memoryStickPbr32 = new MemoryStickPbr32();
			sceMSstorModule.hleMSstorPartitionIoLseek(null, 0L, PSP_SEEK_SET);
			sceMSstorModule.hleMSstorPartitionIoRead(0L, memoryStickPbr32.bootSector, 0, memoryStickPbr32.bootSector.length);
			entryAddress = addMsproAttributeEntry(entryAddress, MSPRO_BLOCK_ID_PBR32, memoryStickPbr32);

			msproAttribute.write(msproAttributeMemory);

			msproAttributeMemoryInitialized = true;
		}
	}

	private int addMsproAttributeEntry(int address, int id, pspAbstractMemoryMappedStructure attributeInfo) {
		int size = attributeInfo.sizeof();

		msproAttribute.entries[msproAttribute.count].address = address;
		msproAttribute.entries[msproAttribute.count].size = size;
		msproAttribute.entries[msproAttribute.count].id = id;

		attributeInfo.write(msproAttributeMemory, address);

		msproAttribute.count++;

		return address + size;
	}

	@Override
	protected int getInterruptNumber() {
		return PSP_MSCM0_INTR;
	}

	@Override
	protected int getInterruptBit() {
		return 0x0004;
	}

	@Override
	protected int readData16(int dataAddress, int dataIndex, boolean endOfCommand) {
		int value = 0;

		if (dataAddress == 0) {
			value = bootPageMemory.read16(dataIndex);
		} else if (dataAddress == PAGES_PER_BLOCK) {
			value = bootPageBackupMemory.read16(dataIndex);
		} else if (dataAddress == DISABLED_BLOCKS_PAGE) {
			value = disabledBlocksPageMemory.read16(dataIndex);
		} else if (dataAddress == CIS_IDI_PAGE) {
			log.error(String.format("MMIOHandlerMemoryStick.readData16 unimplemented reading from CIS_IDI_PAGE"));
		} else {
			log.error(String.format("MMIOHandlerMemoryStick.readData16 unimplemented reading from dataAddress=0x%X", dataAddress));
		}

		return value;
	}

	public void readSector(int lba, TPointer address) {
		long offset = lba * (long) PAGE_SIZE;
		sceMSstorModule.hleMSstorRawIoRead(offset, address, PAGE_SIZE);

		if (log.isDebugEnabled()) {
			byte[] buffer = new byte[PAGE_SIZE];
			for (int i = 0; i < buffer.length; i++) {
				buffer[i] = address.getValue8(i);
			}
			log.debug(String.format("MMIOHandlerMemoryStick.readSector startBlock=0x%X, lba=0x%X, offset=0x%X: %s", startBlock, lba, offset, Utilities.getMemoryDump(buffer)));
		}
	}

	@Override
	protected void readPageBuffer() {
		if (cmd == MS_CMD_BLOCK_READ || cmd == MSPRO_CMD_READ_DATA) {
			int lba = pageLba;
			if (startBlock != 0xFFFF) {
				pageLba += startBlock * PAGES_PER_BLOCK;
			}

			// Invalid page number set during boot sequence
			if (pageLba == 0x4000) {
				pageBufferPointer.clear(PAGE_SIZE);
			} else {
				readSector(lba, pageBufferPointer);
			}

			pageLba++;
		} else {
			log.error(String.format("MMIOHandlerMemoryStick.readPageBuffer unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
		}
	}

	@Override
	protected void writePageBuffer() {
		if (cmd == MSPRO_CMD_WRITE_DATA) {
			int lba = pageLba;
			if (startBlock != 0xFFFF) {
				lba += startBlock * PAGES_PER_BLOCK;
			}
			long offset = lba * (long) PAGE_SIZE;

			if (log.isDebugEnabled()) {
				byte[] buffer = new byte[PAGE_SIZE];
				for (int i = 0; i < buffer.length; i++) {
					buffer[i] = pageBufferPointer.getValue8(i);
				}
				log.debug(String.format("MMIOHandlerMemoryStick.writePageBuffer startBlock=0x%X, lba=0x%X, offset=0x%X: %s", startBlock, lba, offset, Utilities.getMemoryDump(buffer)));
			}

			sceMSstorModule.hleMSstorRawIoWrite(offset, pageBufferPointer, PAGE_SIZE);

			pageLba++;
		} else {
			log.error(String.format("MMIOHandlerMemoryStick.writePageBuffer unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
		}
	}

	@Override
	protected void writeData16(int dataAddress, int dataIndex, int value, boolean endOfCommand) {
		log.error(String.format("MMIOHandlerMemoryStick.writeData16 unimplemented dataAddress=0x%X, dataIndex=0x%X, value=0x%08X", dataAddress, dataIndex, value));
	}

	@Override
	protected void writeData32(int dataAddress, int dataIndex, int value, boolean endOfCommand) {
		log.error(String.format("MMIOHandlerMemoryStick.writeData32 unimplemented dataAddress=0x%X, dataIndex=0x%X, value=0x%08X", dataAddress, dataIndex, value));
	}
}
