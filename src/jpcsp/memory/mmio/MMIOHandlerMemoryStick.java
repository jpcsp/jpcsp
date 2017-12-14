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
package jpcsp.memory.mmio;

import static jpcsp.HLE.Modules.sceMSstorModule;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MSCM0_INTR;
import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_SEEK_SET;
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
import static jpcsp.util.Utilities.endianSwap16;

import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.modules.sceMScm;
import jpcsp.hardware.MemoryStick;
import jpcsp.memory.IntArrayMemory;
import jpcsp.memory.mmio.memorystick.MemoryStickBootPage;
import jpcsp.memory.mmio.memorystick.MemoryStickDeviceInfo;
import jpcsp.memory.mmio.memorystick.MemoryStickMbr;
import jpcsp.memory.mmio.memorystick.MemoryStickPbr32;
import jpcsp.memory.mmio.memorystick.MemoryStickProAttribute;
import jpcsp.memory.mmio.memorystick.MemoryStickSysInfo;
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
public class MMIOHandlerMemoryStick extends MMIOHandlerBase {
	public static Logger log = sceMScm.log;
	private static final boolean simulateMemoryStickPro = true;
	// Overwrite area
	public static final int MS_REG_OVR_BKST = 0x80; // Block status
	public static final int MS_REG_OVR_BKST_OK = MS_REG_OVR_BKST; // Block status OK
	public static final int MS_REG_OVR_BKST_NG = 0; // Block status NG
	public static final int MS_REG_OVR_PGST0 = 0x40; // Page status, bit 0
	public static final int MS_REG_OVR_PGST1 = 0x20; // Page status, bit 1
	public static final int MS_REG_OVR_PGST_MASK = MS_REG_OVR_PGST0 | MS_REG_OVR_PGST1;
	public static final int MS_REG_OVR_PGST_OK = MS_REG_OVR_PGST0 | MS_REG_OVR_PGST1; // Page status OK
	public static final int MS_REG_OVR_PGST_NG = MS_REG_OVR_PGST1; // Page status NG
	public static final int MS_REG_OVR_PGST_DATA_ERROR = 0; // Page status data error
	public static final int MS_REG_OVR_UDST = 0x10; // Update status
	public static final int MS_REG_OVR_UDST_UPDATING = 0; // Update status updating
	public static final int MS_REG_OVR_UDST_NO_UPDATE = MS_REG_OVR_UDST; // Update status no update
	public static final int MS_REG_OVR_RESERVED = 0x08;
	public static final int MS_REG_OVR_DEFAULT = MS_REG_OVR_BKST_OK | MS_REG_OVR_PGST_OK | MS_REG_OVR_UDST_NO_UPDATE | MS_REG_OVR_RESERVED;
	// Management flag
	public static final int MS_REG_MNG_SCMS0 = 0x20; // Serial copy management system, bit 0
	public static final int MS_REG_MNG_SCMS1 = 0x10; // Serial copy management system, bit 1
	public static final int MS_REG_MNG_SCMS_MASK = MS_REG_MNG_SCMS0 | MS_REG_MNG_SCMS1;
	public static final int MS_REG_MNG_SCMS_COPY_OK = MS_REG_MNG_SCMS0 | MS_REG_MNG_SCMS1;
	public static final int MS_REG_MNG_SCMS_ONE_COPY = MS_REG_MNG_SCMS1;
	public static final int MS_REG_MNG_SCMS_NO_COPY = 0;
	public static final int MS_REG_MNG_ATFLG = 0x08; // Address transfer table flag
	public static final int MS_REG_MNG_ATFLG_OTHER = MS_REG_MNG_ATFLG; // Address transfer other
	public static final int MS_REG_MNG_ATFLG_ATTBL = 0; // Address transfer table
	public static final int MS_REG_MNG_SYSFLG = 0x04; // System flag
	public static final int MS_REG_MNG_SYSFLG_USER = MS_REG_MNG_SYSFLG; // User block
	public static final int MS_REG_MNG_SYSFLG_BOOT = 0; // System block
	public static final int MS_REG_MNG_RESERVED = 0xC3;
	public static final int MS_REG_MNG_DEFAULT = MS_REG_MNG_SCMS_COPY_OK | MS_REG_MNG_ATFLG_OTHER | MS_REG_MNG_SYSFLG_USER | MS_REG_MNG_RESERVED;
	// commandState bit
	public static final int MS_COMMANDSTATE_BUSY   = 0x0001;
	// SYS bit
	public static final int MS_SYS_RESET           = 0x8000;
	// STATUS bit
	public static final int MS_STATUS_TIMEOUT      = 0x0100;
	public static final int MS_STATUS_CRC_ERROR    = 0x0200;
	public static final int MS_STATUS_READY        = 0x1000;
	public static final int MS_STATUS_UNKNOWN      = 0x2000;
	public static final int MS_STATUS_FIFO_RW      = 0x4000;
	// MS TPC code
	public static final int MS_TPC_READ_PAGE_DATA     = 0x2;
	public static final int MS_TPC_READ_REG           = 0x4;
	public static final int MS_TPC_GET_INT            = 0x7;
	public static final int MS_TPC_SET_RW_REG_ADDRESS = 0x8;
	public static final int MS_TPC_EX_SET_CMD         = 0x9;
	public static final int MS_TPC_WRITE_REG          = 0xB;
	public static final int MS_TPC_WRITE_PAGE_DATA    = 0xD;
	public static final int MS_TPC_SET_CMD            = 0xE;
	// MS INT (register #1)
	public static final int MS_INT_REG_ADDRESS     = 0x01;
	public static final int MS_INT_REG_CMDNK       = 0x10;
	public static final int MS_INT_REG_BREAK       = 0x20;
	public static final int MS_INT_REG_ERR         = 0x40;
	public static final int MS_INT_REG_CED         = 0x80;
	// MS Status (register #2)
	public static final int MS_STATUS_REG_ADDRESS  = 0x02;
	public static final int MS_STATUS_REG_READONLY = 0x01;
	// MS Type (register #4)
	public static final int MS_TYPE_ADDRESS          = 0x04;
	public static final int MS_TYPE_MEMORY_STICK_PRO = 0x01;
	// MS System parameter (register #16)
	public static final int MS_SYSTEM_ADDRESS        = 0x10;
	public static final int MS_SYSTEM_SERIAL_MODE    = 0x80;
	// MS commands
	public static final int MS_CMD_BLOCK_END       = 0x33;
	public static final int MS_CMD_RESET           = 0x3C;
	public static final int MS_CMD_BLOCK_WRITE     = 0x55;
	public static final int MS_CMD_SLEEP           = 0x5A;
	public static final int MS_CMD_LOAD_ID         = 0x60;
	public static final int MS_CMD_CMP_ICV         = 0x7F;
	public static final int MS_CMD_BLOCK_ERASE     = 0x99;
	public static final int MS_CMD_BLOCK_READ      = 0xAA;
	public static final int MS_CMD_CLEAR_BUF       = 0xC3;
	public static final int MS_CMD_FLASH_STOP      = 0xCC;
	// MS Pro commands
	public static final int MSPRO_CMD_FORMAT       = 0x10;
	public static final int MSPRO_CMD_SLEEP        = 0x11;
	public static final int MSPRO_CMD_WAKEUP       = 0x12;
	public static final int MSPRO_CMD_READ_DATA    = 0x20;
	public static final int MSPRO_CMD_WRITE_DATA   = 0x21;
	public static final int MSPRO_CMD_READ_ATRB    = 0x24;
	public static final int MSPRO_CMD_STOP         = 0x25;
	public static final int MSPRO_CMD_ERASE        = 0x26;
	public static final int MSPRO_CMD_READ_QUAD    = 0x27;
	public static final int MSPRO_CMD_WRITE_QUAD   = 0x28;
	public static final int MSPRO_CMD_SET_IBD      = 0x46;
	public static final int MSPRO_CMD_GET_IBD      = 0x47;
	public static final int MSPRO_CMD_IN_IO_DATA   = 0xB0;
	public static final int MSPRO_CMD_OUT_IO_DATA  = 0xB1;
	public static final int MSPRO_CMD_READ_IO_ATRB = 0xB2;
	public static final int MSPRO_CMD_IN_IO_FIFO   = 0xB3;
	public static final int MSPRO_CMD_OUT_IO_FIFO  = 0xB4;
	public static final int MSPRO_CMD_IN_IOM       = 0xB5;
	public static final int MSPRO_CMD_OUT_IOM      = 0xB6;
	private int interrupt; // Possible bits: 0x000D
	private int commandState;
	private int unk08; // Possible bits: 0x03F0
	private int tpc;
	private int status;
	private int sys;
	private final int[] registers = new int[256];
	private int readAddress;
	private int readSize;
	private int writeAddress;
	private int writeSize;
	private int cmd;
	private int oobLength;
	private int startBlock;
	private int oobIndex;
	private final int[] pageBuffer = new int[(PAGE_SIZE) >> 2];
	private final IntArrayMemory pageBufferMemory = new IntArrayMemory(pageBuffer);
	private final TPointer pageBufferPointer = pageBufferMemory.getPointer();
	private int pageStartLba;
	private int numberOfPages;
	private int pageDataIndex;
	private int pageIndex;
	private int commandDataIndex;
	private final MemoryStickBootPage bootPage = new MemoryStickBootPage();
	private final IntArrayMemory bootPageMemory = new IntArrayMemory(new int[bootPage.sizeof() >> 2]);
	private final MemoryStickBootPage bootPageBackup = new MemoryStickBootPage();
	private final IntArrayMemory bootPageBackupMemory = new IntArrayMemory(new int[bootPageBackup.sizeof() >> 2]);
	private final IntArrayMemory disabledBlocksPageMemory = new IntArrayMemory(new int[PAGE_SIZE >> 2]);
	private final MemoryStickProAttribute msproAttribute = new MemoryStickProAttribute();
	private final IntArrayMemory msproAttributeMemory = new IntArrayMemory(new int[(2 * PAGE_SIZE) >> 2]);
	private final static int PAGE_SIZE = 0x200;
	private final static int DISABLED_BLOCKS_PAGE = 1;
	private final static int CIS_IDI_PAGE = 2;
	private final static long MAX_MEMORY_STICK_SIZE = 128L * 1024 * 1024; // The maximum size of a Memory Stick (i.e. non-PRO) is 128MB
	private int PAGES_PER_BLOCK;
	private int BLOCK_SIZE;
	private int NUMBER_OF_PHYSICAL_BLOCKS;
	private int NUMBER_OF_LOGICAL_BLOCKS;
	private int FIRST_PAGE_LBA;
	private int NUMBER_OF_PAGES;

	public MMIOHandlerMemoryStick(int baseAddress) {
		super(baseAddress);

		sceMSstorModule.hleInit();
		reset();
	}

	private static String getTPCName(int tpc) {
		switch (tpc) {
			case MS_TPC_READ_PAGE_DATA    : return "READ_PAGE_DATA";
			case MS_TPC_READ_REG          : return "READ_REG";
			case MS_TPC_GET_INT           : return "GET_INT";
			case MS_TPC_SET_RW_REG_ADDRESS: return "SET_RW_REG_ADDRESS";
			case MS_TPC_EX_SET_CMD        : return "EX_SET_CMD";
			case MS_TPC_WRITE_REG         : return "WRITE_REG";
			case MS_TPC_WRITE_PAGE_DATA   : return "WRITE_PAGE_DATA";
			case MS_TPC_SET_CMD           : return "SET_CMD";
		}

		return String.format("UNKNOWN_TPC_%X", tpc);
	}

	private static String getCommandName(int cmd) {
		switch (cmd) {
			case MS_CMD_BLOCK_END      : return "BLOCK_END";
			case MS_CMD_RESET          : return "RESET";
			case MS_CMD_BLOCK_WRITE    : return "BLOCK_WRITE";
			case MS_CMD_SLEEP          : return "SLEEP";
			case MS_CMD_LOAD_ID        : return "LOAD_ID";
			case MS_CMD_CMP_ICV        : return "CMP_ICV";
			case MS_CMD_BLOCK_ERASE    : return "BLOCK_ERASE";
			case MS_CMD_BLOCK_READ     : return "BLOCK_READ";
			case MS_CMD_CLEAR_BUF      : return "CLEAR_BUF";
			case MS_CMD_FLASH_STOP     : return "FLASH_STOP";
			case MSPRO_CMD_FORMAT      : return "FORMAT";
			case MSPRO_CMD_SLEEP       : return "MSPRO_SLEEP";
			case MSPRO_CMD_WAKEUP      : return "WAKEUP";
			case MSPRO_CMD_READ_DATA   : return "READ_DATA";
			case MSPRO_CMD_WRITE_DATA  : return "WRITE_DATA";
			case MSPRO_CMD_READ_ATRB   : return "READ_ATRB";
			case MSPRO_CMD_STOP        : return "STOP";
			case MSPRO_CMD_ERASE       : return "ERASE";
			case MSPRO_CMD_READ_QUAD   : return "READ_QUAD";
			case MSPRO_CMD_WRITE_QUAD  : return "WRITE_QUAD";
			case MSPRO_CMD_SET_IBD     : return "SET_IBD";
			case MSPRO_CMD_GET_IBD     : return "GET_IBD";
			case MSPRO_CMD_IN_IO_DATA  : return "IN_IO_DATA";
			case MSPRO_CMD_OUT_IO_DATA : return "OUT_IO_DATA";
			case MSPRO_CMD_READ_IO_ATRB: return "READ_IO_ATRB";
			case MSPRO_CMD_IN_IO_FIFO  : return "IN_IO_FIFO";
			case MSPRO_CMD_OUT_IO_FIFO : return "OUT_IO_FIFO";
			case MSPRO_CMD_IN_IOM      : return "IN_IOM";
			case MSPRO_CMD_OUT_IOM     : return "OUT_IOM";
		}

		return String.format("UNKNOWN_CMD_%X", cmd);
	}

	private void reset() {
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

		Arrays.fill(registers, 0);
		interrupt = 0;
		commandState = 0;
		unk08 = 0;
		tpc = 0;
		status = 0;
		sys = 0;
		readAddress = 0;
		readSize = 0;
		writeAddress = 0;
		writeSize = 0;
		cmd = 0;
		numberOfPages = 0;
		pageStartLba = 0;
		pageIndex = 0;
		pageDataIndex = 0;
		startBlock = 0;
		oobLength = 0;
		oobIndex = 0;
		commandDataIndex = 0;

		registers[MS_INT_REG_ADDRESS] = MS_INT_REG_CED;
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

		if (simulateMemoryStickPro) {
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
			sceMSstorModule.hleMSstorPartitionIoRead(memoryStickPbr32.bootSector, 0, memoryStickPbr32.bootSector.length);
			entryAddress = addMsproAttributeEntry(entryAddress, MSPRO_BLOCK_ID_PBR32, memoryStickPbr32);

			msproAttribute.write(msproAttributeMemory);
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

	private void writeSys(int sys) {
		this.sys = sys;

		if ((sys & MS_SYS_RESET) != 0) {
			// Reset
			if (log.isDebugEnabled()) {
				log.debug(String.format("MMIOHandlerMemoryStick.writeSys reset triggered"));
			}
			reset();
		}
	}

	private void writeCommandState(int commandState) {
		this.commandState = commandState;

		if (isBusy()) {
			clearBusy();
		}
	}

	private void setBusy() {
		commandState |= MS_COMMANDSTATE_BUSY;
	}

	public void clearBusy() {
		commandState &= ~MS_COMMANDSTATE_BUSY;
	}

	private boolean isBusy() {
		return (commandState & MS_COMMANDSTATE_BUSY) != 0;
	}

	private void setInterrupt(int interrupt) {
		this.interrupt |= interrupt;
		checkInterrupt();
	}

	private void clearInterrupt(int interrupt) {
		// TODO Not sure if this is the correct behavior. (gid15)
		//
		// The PSP seems to want to clear only bit 0x1 of the interrupt,
		// but sometimes (due to a race condition), it is also clearing
		// bit 0x4, which should not.
		//
		// The PSP code is executing:
		//    ((u16 *) 0xBD200000) |= 0x1;
		// which will actually clear all the interrupt bits...
		if ((interrupt & 0x1) != 0) {
			interrupt &= ~0x4;
		}

		this.interrupt &= ~interrupt;
		checkInterrupt();
	}

	private void checkInterrupt() {
		if (interrupt != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_MSCM0_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_MSCM0_INTR);
		}
	}

	private boolean isMemoryStickPro() {
		return (registers[MS_TYPE_ADDRESS] & MS_TYPE_MEMORY_STICK_PRO) != 0;
	}

	private boolean isSerialMode() {
		if (!isMemoryStickPro()) {
			// A non-PRO memory stick is always in serial mode
			return true;
		}
		return (registers[MS_SYSTEM_ADDRESS] & MS_SYSTEM_SERIAL_MODE) != 0;
	}

	private int getTPCCode() {
		return tpc >> 12;
	}

	private void startTPC(int tpc) {
		this.tpc = tpc;

		int tpcCode = getTPCCode();
		int unknown = tpc & 0x3FF;

		if (log.isDebugEnabled()) {
			log.debug(String.format("startTPC tpcCode=0x%01X(%s), unknown=0x%03X", tpcCode, getTPCName(tpcCode), unknown));
		}

		switch (tpcCode) {
			case MS_TPC_SET_RW_REG_ADDRESS:
				// Data will be set at next writeData()
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startTPC MS_TPC_SET_RW_REG_ADDRESS"));
				}
				break;
			case MS_TPC_READ_REG:
				// Data will be retrieve at next readData()
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startTPC MS_TPC_READ_REG readAddress=0x%02X, readSize=0x%X", readAddress, readSize));
				}
				break;
			case MS_TPC_WRITE_REG:
				// Register will be written during writeData()
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startTPC MS_TPC_WRITE_REG writeAddress=0x%02X, writeSize=0x%X", writeAddress, writeSize));
				}
				break;
			case MS_TPC_SET_CMD:
				// Clear the CED (Command EnD) bit in the INT register
				registers[MS_INT_REG_ADDRESS] &= ~MS_INT_REG_CED;
				// Register will be written during writeData()
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startTPC MS_TPC_SET_CMD"));
				}
				break;
			case MS_TPC_GET_INT:
				// Data will be retrieved at next readData()
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startTPC MS_TPC_GET_INT"));
				}
				break;
			case MS_TPC_READ_PAGE_DATA:
				// Data will be retrieved through readData16()
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startTPC MS_TPC_READ_PAGE_DATA"));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerMemoryStick.startTPC unknown TPC 0x%01X", tpcCode));
				break;
		}

		status |= MS_STATUS_FIFO_RW;
	}

	private int getRegisterValue(int reg, int length) {
		int value = 0;
		for (int i = 0; i < length; i++) {
			value = (value << 8) | (registers[reg + i] & 0xFF);
		}

		return value;
	}

	private int getDataCount() {
		if (isMemoryStickPro()) {
			return getRegisterValue(0x11, 2);
		}
		return PAGE_SIZE;
	}

	private int getDataAddress() {
		if (isMemoryStickPro()) {
			return getRegisterValue(0x13, 4);
		}

		int blockAddress = getRegisterValue(0x11, 3);
		int pageAddress = getRegisterValue(0x15, 1);
		return blockAddress * PAGES_PER_BLOCK + pageAddress;
	}

	private void startCmd(int cmd) {
		setCmd(cmd);

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerMemoryStick.startCmd cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
		}

		switch (cmd) {
			case MS_CMD_BLOCK_READ:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startCmd MS_CMD_BLOCK_READ dataCount=0x%04X, dataAddress=0x%08X, cp=0x%02X", getDataCount(), getDataAddress(), getRegisterValue(0x14, 1)));
				}
				setBusy();
				if (!isMemoryStickPro()) {
					registers[0x16] = 0x80;
					registers[0x17] = 0x00;
				}
				break;
			case MS_CMD_BLOCK_ERASE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startCmd MS_CMD_BLOCK_ERASE dataCount=0x%04X, dataAddress=0x%08X, cp=0x%02X", getDataCount(), getDataAddress(), getRegisterValue(0x14, 1)));
				}
				clearBusy();
				break;
			case MS_CMD_BLOCK_WRITE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startCmd MS_CMD_BLOCK_WRITE dataCount=0x%04X, dataAddress=0x%08X, cp=0x%02X", getDataCount(), getDataAddress(), getRegisterValue(0x14, 1)));
				}
				break;
			case MS_CMD_SLEEP:
			case MSPRO_CMD_SLEEP:
				// Simply ignore these commands
				break;
			default:
				log.error(String.format("MMIOHandlerMemoryStick.startCmd unknown cmd=0x%02X", cmd));
				break;
		}

		// Set only the CED (Command EnD) bit in the INT register,
		// indicating a successful completion
		registers[MS_INT_REG_ADDRESS] = MS_INT_REG_CED;

		pageIndex = 0;
		pageDataIndex = 0;
		status |= 0x2000;
		sys |= 0x4000;
		setInterrupt(0x4);
	}

	private int readData16() {
		if (!isSerialMode()) {
			log.error(String.format("MMIOHandlerMemoryStick.readData16 not supported for parallel mode"));
			return 0;
		}

		int value = 0;
		int dataAddress = getDataAddress();
		if (dataAddress == 0) {
			value = bootPageMemory.read16(pageDataIndex);
		} else if (dataAddress == PAGES_PER_BLOCK) {
			value = bootPageBackupMemory.read16(pageDataIndex);
		} else if (dataAddress == DISABLED_BLOCKS_PAGE) {
			value = disabledBlocksPageMemory.read16(pageDataIndex);
		} else if (dataAddress == CIS_IDI_PAGE) {
			log.error(String.format("MMIOHandlerMemoryStick.readData16 unimplemented reading from CIS_IDI_PAGE"));
		}

		pageDataIndex += 2;
		if (pageDataIndex >= PAGE_SIZE) {
			clearBusy();
			status |= 0x2000;
			sys |= 0x4000;
			setInterrupt(0x0004);
		}

		return value;
	}

	private int readOOBData16() {
		int value;
		if ((oobIndex & 3) == 0) {
			// Overwrite area and management flag
			value = (MS_REG_MNG_DEFAULT << 8) | MS_REG_OVR_DEFAULT;
		} else {
			// Logical address
			value = endianSwap16(startBlock + (oobIndex >> 2));
		}

		oobIndex += 2;
		if (oobIndex >= (oobLength << 2)) {
			oobIndex = 0;
			clearBusy();
			status |= 0x2000;
			sys |= 0x4000;
			unk08 |= 0x0040;
			unk08 &= ~0x000F; // Clear error code
			setInterrupt(0x0004);
		}

		return value;
	}

	private int readPageData16() {
		if (!isSerialMode()) {
			log.error(String.format("MMIOHandlerMemoryStick.readPageData16 not supported for parallel mode"));
			return 0;
		}

		int value;

		switch (cmd) {
			case MS_CMD_BLOCK_READ:
				value = pageBufferMemory.read16(pageDataIndex);
				break;
			default:
				log.error(String.format("MMIOHandlerMemoryStick.readPageData16 unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
				value = 0;
				break;
		}

		pageDataIndex += 2;
		if (pageDataIndex >= PAGE_SIZE) {
			pageDataIndex = 0;
			pageIndex++;
			if (pageIndex >= numberOfPages) {
				pageIndex = 0;
				clearBusy();
				status |= 0x2000;
				sys |= 0x4000;
				unk08 |= 0x0040;
				unk08 &= ~0x000F; // Clear error code
				setInterrupt(0x0004);
			} else {
				readPageBuffer();
			}
		}

		return value;
	}

	private void setNumberOfPages(int numberOfPages) {
		this.numberOfPages = numberOfPages;
		pageIndex = 0;
		pageDataIndex = 0;
	}

	private void setPageStartLba(int pageStartLba) {
		this.pageStartLba = pageStartLba;
		pageIndex = 0;
		pageDataIndex = 0;

		readPageBuffer();
	}

	private void setStartBlock(int startBlock) {
		this.startBlock = startBlock;
		pageIndex = 0;
		pageDataIndex = 0;
		oobIndex = 0;
	}

	private void readMasterBootRecord() {
		// See description of MBR at
		// https://en.wikipedia.org/wiki/Master_boot_record
		pageBufferPointer.clear(PAGE_SIZE);

		// First partition entry
		TPointer partitionPointer = new TPointer(pageBufferPointer, 446);
		// Active partition
		partitionPointer.setValue8(0, (byte) 0x80);
    	// CHS address of first absolute sector in partition (not used by the PSP)
		partitionPointer.setValue8(1, (byte) 0x00);
		partitionPointer.setValue8(2, (byte) 0x00);
		partitionPointer.setValue8(3, (byte) 0x00);
		// Partition type: FAT32 with LBA
		partitionPointer.setValue8(4, (byte) 0x0C);
    	// CHS address of last absolute sector in partition (not used by the PSP)
		partitionPointer.setValue8(5, (byte) 0x00);
		partitionPointer.setValue8(6, (byte) 0x00);
		partitionPointer.setValue8(7, (byte) 0x00);
		// LBA of first absolute sector in the partition
		partitionPointer.setUnalignedValue32(8, FIRST_PAGE_LBA);
		// Number of sectors in partition
		partitionPointer.setUnalignedValue32(12, NUMBER_OF_PAGES);

		// Signature
		pageBufferPointer.setValue8(510, (byte) 0x55);
		pageBufferPointer.setValue8(511, (byte) 0xAA);
	}

	private void setCmd(int cmd) {
		this.cmd = cmd;
	}

	private void readPageBuffer() {
		if (cmd == MS_CMD_BLOCK_READ || cmd == MSPRO_CMD_READ_DATA) {
			long offset = 0L;
			int pageLba = pageStartLba + pageIndex;
			if (startBlock != 0xFFFF) {
				pageLba += startBlock * PAGES_PER_BLOCK;
			}

			// Invalid page number set during boot sequence
			if (pageStartLba == 0x4000) {
				pageBufferPointer.clear(PAGE_SIZE);
			} else if (pageLba == 0) {
				readMasterBootRecord();
			} else if (pageLba >= FIRST_PAGE_LBA) {
				offset = (pageLba - FIRST_PAGE_LBA) * (long) PAGE_SIZE;

				sceMSstorModule.hleMSstorPartitionIoLseek(null, offset, PSP_SEEK_SET);
				sceMSstorModule.hleMSstorPartitionIoRead(null, pageBufferPointer, PAGE_SIZE);
			} else {
				pageBufferPointer.clear(PAGE_SIZE);
			}

			if (log.isDebugEnabled()) {
				byte[] buffer = new byte[PAGE_SIZE];
				for (int i = 0; i < buffer.length; i++) {
					buffer[i] = pageBufferPointer.getValue8(i);
				}
				log.debug(String.format("MMIOHandlerMemoryStick.readPageBuffer startBlock=0x%X, pageLba=0x%X, offset=0x%X: %s", startBlock, pageLba, offset, Utilities.getMemoryDump(buffer)));
			}
		}
	}

	private int readTPCData32() {
		int data = 0;
		switch (getTPCCode()) {
			case MS_TPC_GET_INT:
				data = registers[MS_INT_REG_ADDRESS] & 0xFF;
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.readTPCData32 MS_TPC_GET_INT registers[0x%02X]=0x%02X", MS_INT_REG_ADDRESS, data));
				}
				break;
			case MS_TPC_READ_REG:
				for (int i = 0; i < 32; i += 8) {
					if (readSize <= 0 || readAddress >= registers.length) {
						break;
					}
					data |= (registers[readAddress] & 0xFF) << i;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerMemoryStick.readTPCData32 MS_TPC_READ_REG registers[0x%02X]=0x%02X", readAddress, registers[readAddress]));
					}
					readSize--;
					readAddress++;
				}
				break;
		}

		return data;
	}

	private int readPageData32() {
		if (isSerialMode()) {
			log.error(String.format("MMIOHandlerMemoryStick.readPageData32 not supported for serial mode"));
			return 0;
		}

		int value;

		switch (cmd) {
			case MSPRO_CMD_READ_ATRB:
				value = msproAttributeMemory.read32(((pageStartLba + pageIndex) * PAGE_SIZE) + pageDataIndex);
				break;
			case MSPRO_CMD_READ_DATA:
				value = pageBufferMemory.read32(pageDataIndex);
				break;
			default:
				log.error(String.format("MMIOHandlerMemoryStick.readPageData32 unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
				value = 0;
				break;
		}

		pageDataIndex += 4;
		if (pageDataIndex >= PAGE_SIZE) {
			pageDataIndex = 0;
			pageIndex++;
			if (pageIndex >= numberOfPages) {
				pageIndex = 0;
				commandDataIndex = 0;
				clearBusy();
				status |= 0x2000;
				sys |= 0x4000;
				unk08 |= 0x0040;
				unk08 &= ~0x000F; // Clear error code
				setInterrupt(0x0004);
			} else {
				readPageBuffer();
			}
		}

		return value;
	}

	private void writeTPCData(int value) {
		if (tpc < 0) {
			// Ignore this data
			return;
		}

		switch (getTPCCode()) {
			case MS_TPC_SET_RW_REG_ADDRESS:
				// Sets the read address & size, write address & size
				// for a subsequent MS_CMD_READ_REG/MS_CMD_WRITE_REG command
				readAddress = value & 0xFF;
				readSize = (value >> 8) & 0xFF;
				writeAddress = (value >> 16) & 0xFF;
				writeSize = (value >> 24) & 0xFF;
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.writeTPCData MS_TPC_SET_RW_REG_ADDRESS readAddress=0x%02X, readSize=0x%X, writeAddress=0x%02X, writeSize=0x%X", readAddress, readSize, writeAddress, writeSize));
				}
				// Ignore further data
				tpc = -1;
				break;
			case MS_TPC_WRITE_REG:
				for (int i = 0; i < 4; i++) {
					if (writeSize <= 0 || writeAddress >= registers.length) {
						break;
					}
					registers[writeAddress] = value & 0xFF;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerMemoryStick.writeTPCData MS_TPC_WRITE_REG registers[0x%02X]=0x%02X", writeAddress, registers[writeAddress]));
					}
					writeAddress++;
					writeSize--;
					value >>>= 8;
				}
				break;
			case MS_TPC_SET_CMD:
				startCmd(value & 0xFF);
				// Ignore further data
				tpc = -1;
				break;
		}
	}

	private void writeCommandData8(int value) {
		switch (commandDataIndex) {
			case 0:
				setCmd(value);
				tpc = MS_TPC_SET_CMD;
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.writeCommandData8 cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
				}

				switch (cmd) {
					case MSPRO_CMD_READ_ATRB:
						break;
					case MSPRO_CMD_READ_DATA:
						break;
					default:
						log.error(String.format("MMIOHandlerMemoryStick.writeCommandData8 unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
						break;
				}
				break;
			case 1:
				numberOfPages = (numberOfPages & 0x00FF) | (value << 8);
				break;
			case 2:
				setNumberOfPages((numberOfPages & 0xFF00) | value);
				if (log.isDebugEnabled()) {
					log.debug(String.format("numberOfPages=0x%X", numberOfPages));
				}
				break;
			case 3:
				pageStartLba = (pageStartLba & 0x00FFFFFF) | (value << 24);
				break;
			case 4:
				pageStartLba = (pageStartLba & 0xFF00FFFF) | (value << 16);
				break;
			case 5:
				pageStartLba = (pageStartLba & 0xFFFF00FF) | (value << 8);
				break;
			case 6:
				setStartBlock(0);
				setPageStartLba((pageStartLba & 0xFFFFFF00) | value);
				if (log.isDebugEnabled()) {
					log.debug(String.format("pageStartLba=0x%X", pageStartLba));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerMemoryStick.writeCommandData8 unknown data 0x%02X written at index 0x%X", value, commandDataIndex));
				break;
		}
		commandDataIndex++;
	}

	@Override
	public int read16(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = interrupt; break;
			case 0x04: value = commandState; break;
			case 0x08: value = unk08; break;
			case 0x24: value = readOOBData16(); break;
			case 0x28: value = readPageData16(); break;
			case 0x30: value = tpc; break;
			case 0x34: value = readData16(); break;
			case 0x38: value = status; break;
			case 0x3C: value = sys; break;
			default: value = super.read16(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read16(0x%08X) returning 0x%04X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x28: value = readPageData32(); break;
			case 0x34: value = readTPCData32(); break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write16(int address, short value) {
		switch (address - baseAddress) {
			case 0x00: clearInterrupt(value & 0xFFFF); break;
			case 0x02: break; // TODO Unknown
			case 0x04: writeCommandState(value & 0xFFFF); break;
			case 0x10: setNumberOfPages(value); break;
			case 0x12: oobLength = value; break;
			case 0x14: setStartBlock(value); break;
			case 0x16: setCmd(MS_CMD_BLOCK_READ); setPageStartLba(value); break;
			case 0x20: break; // TODO Unknown
			case 0x30: startTPC(value & 0xFFFF); break;
			case 0x38: status = value & 0xFFFF; break;
			case 0x3C: writeSys(value & 0xFFFF); break;
			default: super.write16(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X) on %s", getPc(), address, value & 0xFFFF, this));
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x34: writeTPCData(value); break;
			case 0x40: break; // TODO Unknown
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public void write8(int address, byte value) {
		switch (address - baseAddress) {
			case 0x24: writeCommandData8(value & 0xFF); break;
			default: super.write8(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write8(0x%08X, 0x%02X) on %s", getPc(), address, value & 0xFF, this));
		}
	}
}
