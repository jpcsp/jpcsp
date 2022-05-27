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

import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.hasFlag;

import java.io.IOException;
import java.util.Arrays;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.memory.IntArrayMemory;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

/**
 * Base MMIO implementation for the Memory Stick and the Wlan interfaces.
 * 
 * Based on information from
 *     https://github.com/torvalds/linux/tree/master/drivers/memstick/core
 * and https://github.com/torvalds/linux/blob/master/drivers/usb/storage/ene_ub6250.c
 *
 * @author gid15
 *
 */
public abstract class MMIOHandlerBaseMemoryStick extends MMIOHandlerBase {
	private static final int STATE_VERSION = 0;
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
	public static final int MS_SYS_COMMAND         = 0x0200;
	public static final int MS_SYS_INTERRUPT       = 0x0800;
	public static final int MS_SYS_RESET           = 0x8000;
	// STATUS bit
	public static final int MS_STATUS_TIMEOUT      = 0x0100;
	public static final int MS_STATUS_CRC_ERROR    = 0x0200;
	public static final int MS_STATUS_READY        = 0x1000;
	public static final int MS_STATUS_UNKNOWN      = 0x2000;
	public static final int MS_STATUS_FIFO_RW      = 0x4000;
	// MS TPC code
	public static final int MS_TPC_READ_MG_STATUS     = 0x1;
	public static final int MS_TPC_READ_PAGE_DATA     = 0x2;
	public static final int MS_TPC_READ_SHORT_DATA    = 0x3;
	public static final int MS_TPC_READ_REG           = 0x4;
	public static final int MS_TPC_READ_IO_DATA       = 0x5;
	public static final int MS_TPC_GET_INT            = 0x7;
	public static final int MS_TPC_SET_RW_REG_ADDRESS = 0x8;
	public static final int MS_TPC_EX_SET_CMD         = 0x9;
	public static final int MS_TPC_WRITE_IO_DATA      = 0xA;
	public static final int MS_TPC_WRITE_REG          = 0xB;
	public static final int MS_TPC_WRITE_SHORT_DATA   = 0xC;
	public static final int MS_TPC_WRITE_PAGE_DATA    = 0xD;
	public static final int MS_TPC_SET_CMD            = 0xE;
	// MS INT (register #1)
	public static final int MS_INT_REG_ADDRESS     = 0x01;
	public static final int MS_INT_REG_CMDNK       = 0x10; // Indicates that a command cannot be executed
	public static final int MS_INT_REG_BREQ        = 0x20; // Buffer REQuest
	public static final int MS_INT_REG_ERR         = 0x40; // ERRor
	public static final int MS_INT_REG_CED         = 0x80; // Command EnD
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
	protected int interrupt; // Possible bits: 0x000D
	protected int commandState;
	protected int unk08; // Possible bits: 0x03F0
	protected int tpc;
	protected int status;
	protected int sys;
	protected int unk40;
	protected final int[] registers = new int[256];
	protected int readAddress;
	protected int readSize;
	protected int writeAddress;
	protected int writeSize;
	protected int tpcExSetCmdIndex;
	protected int cmd;
	protected int oobLength;
	protected int startBlock;
	protected int oobIndex;
	protected final int[] pageBuffer = new int[(PAGE_SIZE) >> 2];
	protected final IntArrayMemory pageBufferMemory = new IntArrayMemory(pageBuffer);
	protected final TPointer pageBufferPointer = pageBufferMemory.getPointer();
	protected final IntArrayMemory msproAttributeMemory = new IntArrayMemory(new int[(2 * PAGE_SIZE) >> 2]);
	protected int pageLba;
	protected int numberOfPages;
	protected int pageDataIndex;
	protected int pageIndex;
	protected int dataIndex;
	protected int commandDataIndex;
	public final static int PAGE_SIZE = 0x200;
	protected int PAGES_PER_BLOCK = 16;
	private final Object dmaLock = new Object();
	private int dmaTpcCode;

	public MMIOHandlerBaseMemoryStick(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		interrupt = stream.readInt();
		commandState = stream.readInt();
		unk08 = stream.readInt();
		tpc = stream.readInt();
		status = stream.readInt();
		sys = stream.readInt();
		unk40 = stream.readInt();
		stream.readInts(registers);
		readAddress = stream.readInt();
		readSize = stream.readInt();
		writeAddress = stream.readInt();
		writeSize = stream.readInt();
		tpcExSetCmdIndex = stream.readInt();
		cmd = stream.readInt();
		oobLength = stream.readInt();
		startBlock = stream.readInt();
		oobIndex = stream.readInt();
		pageBufferMemory.read(stream);
		msproAttributeMemory.read(stream);
		pageLba = stream.readInt();
		numberOfPages = stream.readInt();
		pageDataIndex = stream.readInt();
		pageIndex = stream.readInt();
		dataIndex = stream.readInt();
		commandDataIndex = stream.readInt();
		PAGES_PER_BLOCK = stream.readInt();
		dmaTpcCode = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(interrupt);
		stream.writeInt(commandState);
		stream.writeInt(unk08);
		stream.writeInt(tpc);
		stream.writeInt(status);
		stream.writeInt(sys);
		stream.writeInt(unk40);
		stream.writeInts(registers);
		stream.writeInt(readAddress);
		stream.writeInt(readSize);
		stream.writeInt(writeAddress);
		stream.writeInt(writeSize);
		stream.writeInt(tpcExSetCmdIndex);
		stream.writeInt(cmd);
		stream.writeInt(oobLength);
		stream.writeInt(startBlock);
		stream.writeInt(oobIndex);
		pageBufferMemory.write(stream);
		msproAttributeMemory.write(stream);
		stream.writeInt(pageLba);
		stream.writeInt(numberOfPages);
		stream.writeInt(pageDataIndex);
		stream.writeInt(pageIndex);
		stream.writeInt(dataIndex);
		stream.writeInt(commandDataIndex);
		stream.writeInt(PAGES_PER_BLOCK);
		stream.writeInt(dmaTpcCode);
		super.write(stream);
	}

	private static String getTPCName(int tpc) {
		switch (tpc) {
			case MS_TPC_READ_MG_STATUS    : return "MS_TPC_READ_MG_STATUS";
			case MS_TPC_READ_PAGE_DATA    : return "MS_TPC_READ_PAGE_DATA";
			case MS_TPC_READ_SHORT_DATA   : return "MS_TPC_READ_SHORT_DATA";
			case MS_TPC_READ_REG          : return "MS_TPC_READ_REG";
			case MS_TPC_READ_IO_DATA      : return "MS_TPC_READ_IO_DATA";
			case MS_TPC_GET_INT           : return "MS_TPC_GET_INT";
			case MS_TPC_SET_RW_REG_ADDRESS: return "MS_TPC_SET_RW_REG_ADDRESS";
			case MS_TPC_EX_SET_CMD        : return "MS_TPC_EX_SET_CMD";
			case MS_TPC_WRITE_IO_DATA     : return "MS_TPC_WRITE_IO_DATA";
			case MS_TPC_WRITE_REG         : return "MS_TPC_WRITE_REG";
			case MS_TPC_WRITE_SHORT_DATA  : return "MS_TPC_WRITE_SHORT_DATA";
			case MS_TPC_WRITE_PAGE_DATA   : return "MS_TPC_WRITE_PAGE_DATA";
			case MS_TPC_SET_CMD           : return "MS_TPC_SET_CMD";
		}

		return String.format("UNKNOWN_TPC_%X", tpc);
	}

	public static String getCommandName(int cmd) {
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

	@Override
	public void reset() {
		super.reset();

		Arrays.fill(registers, 0);
		interrupt = 0;
		commandState = 0;
		unk08 = 0;
		tpc = 0;
		status = MS_STATUS_READY;
		sys = 0;
		readAddress = 0;
		readSize = 0;
		writeAddress = 0;
		writeSize = 0;
		cmd = 0;
		numberOfPages = 0;
		pageLba = 0;
		pageIndex = 0;
		pageDataIndex = 0;
		dataIndex = 0;
		startBlock = 0;
		oobLength = 0;
		oobIndex = 0;
		commandDataIndex = 0;
		dmaTpcCode = -1;

		setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_CED);
	}

	private void writeSys(int sys) {
		if (hasFlag(sys, MS_SYS_INTERRUPT)) {
			clearInterrupt(getInterruptBit());
		}

		sys &= ~(MS_SYS_COMMAND | MS_SYS_INTERRUPT);

		this.sys = sys;

		if ((sys & MS_SYS_RESET) != 0) {
			// Reset
			if (log.isDebugEnabled()) {
				log.debug(String.format("MMIOHandlerBaseMemoryStick.writeSys reset triggered"));
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

	protected int getStatus() {
		// Lowest 4 bits of the status are identical to bits 4..7 from MS_INT_REG_ADDRESS register
		return (status & ~0xF) | ((getRegisterValue(MS_INT_REG_ADDRESS) >> 4) & 0xF);
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

	protected void setInterrupt() {
		setInterrupt(getInterruptBit());
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

	protected abstract int getInterruptNumber();
	protected abstract int getInterruptBit();

	private void checkInterrupt() {
		if (hasInterrupt()) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), getInterruptNumber());
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), getInterruptNumber());
		}
	}

	protected boolean hasInterrupt() {
		return interrupt != 0;
	}

	protected boolean isMemoryStickPro() {
		return (getRegisterValue(MS_TYPE_ADDRESS) & MS_TYPE_MEMORY_STICK_PRO) != 0;
	}

	protected boolean isSerialMode() {
		if (!isMemoryStickPro()) {
			// A non-PRO memory stick is always in serial mode
			return true;
		}
		return (getRegisterValue(MS_SYSTEM_ADDRESS) & MS_SYSTEM_SERIAL_MODE) != 0;
	}

	protected int getTPCCode() {
		return tpc >> 12;
	}

	protected void startTPC(int tpc) {
		synchronized (dmaLock) {
			this.tpc = tpc;

			int tpcCode = getTPCCode();
			int size = tpc & 0x3FF;

			if (log.isDebugEnabled()) {
				log.debug(String.format("startTPC tpcCode=0x%01X(%s), size=0x%03X", tpcCode, getTPCName(tpcCode), size));
			}

			switch (tpcCode) {
				case MS_TPC_SET_RW_REG_ADDRESS:
					// Data will be set at next writeData()
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_SET_RW_REG_ADDRESS"));
					}
					break;
				case MS_TPC_READ_REG:
					// Data will be retrieve at next readData()
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_READ_REG readAddress=0x%02X, readSize=0x%X", readAddress, readSize));
					}
					break;
				case MS_TPC_WRITE_REG:
					// Register will be written during writeData()
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_WRITE_REG writeAddress=0x%02X, writeSize=0x%X", writeAddress, writeSize));
					}
					break;
				case MS_TPC_SET_CMD:
					// Clear the CED (Command EnD) bit in the INT register
					setRegisterValue(MS_INT_REG_ADDRESS, getRegisterValue(MS_INT_REG_ADDRESS) & ~MS_INT_REG_CED);
					// Register will be written during writeData()
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_SET_CMD"));
					}
					break;
				case MS_TPC_EX_SET_CMD:
					// Clear the CED (Command EnD) bit in the INT register
					setRegisterValue(MS_INT_REG_ADDRESS, getRegisterValue(MS_INT_REG_ADDRESS) & ~MS_INT_REG_CED);
					// Parameters will be written during writeTPCData()
					tpcExSetCmdIndex = 0;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_EX_SET_CMD"));
					}
					break;
				case MS_TPC_GET_INT:
					// Data will be retrieved at next readData()
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_GET_INT"));
					}
					break;
				case MS_TPC_READ_PAGE_DATA:
					// Data will be retrieved through readData16()
					readSize = 0x200;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_READ_PAGE_DATA readSize=0x%X", readSize));
					}
					break;
				case MS_TPC_READ_IO_DATA:
					// Data will be retrieved through readData16()
					readSize = size;
					pageDataIndex = 0;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_READ_IO_DATA readSize=0x%X", readSize));
					}
					break;
				case MS_TPC_WRITE_IO_DATA:
					// Data will be written through writeTPCData()
					writeSize = size;
					pageDataIndex = 0;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_WRITE_IO_DATA writeSize=0x%X", writeSize));
					}
					break;
				case MS_TPC_WRITE_PAGE_DATA:
					// Data will be written through writeTPCData()
					writeSize = size;
					pageDataIndex = 0;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.startTPC MS_TPC_WRITE_PAGE_DATA writeSize=0x%X", writeSize));
					}
					break;
				default:
					log.error(String.format("MMIOHandlerBaseMemoryStick.startTPC unknown TPC 0x%01X", tpcCode));
					break;
			}

			status |= MS_STATUS_FIFO_RW;
			dmaTpcCode = tpcCode;
		}
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

	protected void startCmd(int cmd) {
		setCmd(cmd);

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
		}

		boolean commandCompleted = true;
		switch (cmd) {
			case MS_CMD_BLOCK_READ:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MS_CMD_BLOCK_READ dataCount=0x%04X, dataAddress=0x%08X, cp=0x%02X", getDataCount(), getDataAddress(), getRegisterValue(0x14, 1)));
				}
				setBusy();
				if (!isMemoryStickPro()) {
					setRegisterValue(0x16, 0x80);
					setRegisterValue(0x17, 0x00);
				}
				break;
			case MS_CMD_BLOCK_ERASE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MS_CMD_BLOCK_ERASE dataCount=0x%04X, dataAddress=0x%08X, cp=0x%02X", getDataCount(), getDataAddress(), getRegisterValue(0x14, 1)));
				}
				clearBusy();
				break;
			case MS_CMD_BLOCK_WRITE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MS_CMD_BLOCK_WRITE dataCount=0x%04X, dataAddress=0x%08X, cp=0x%02X", getDataCount(), getDataAddress(), getRegisterValue(0x14, 1)));
				}
				break;
			case MS_CMD_SLEEP:
			case MSPRO_CMD_SLEEP:
			case MSPRO_CMD_WAKEUP:
			case MSPRO_CMD_STOP:
				// Simply ignore these commands
				break;
			case MSPRO_CMD_READ_DATA:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MSPRO_CMD_READ_DATA dataCount=0x%04X, dataAddress=0x%08X", getDataCount(), getDataAddress()));
				}
				setNumberOfPages(getDataCount());
				setStartBlock(0);
				setPageLba(getDataAddress());
				commandCompleted = false;
				break;
			case MSPRO_CMD_WRITE_DATA:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MSPRO_CMD_WRITE_DATA dataCount=0x%04X, dataAddress=0x%08X", getDataCount(), getDataAddress()));
				}
				setNumberOfPages(getDataCount());
				setStartBlock(0);
				setPageLba(getDataAddress());
				commandCompleted = false;
				setInterrupt();
				break;
			case MSPRO_CMD_READ_IO_ATRB:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MSPRO_CMD_READ_IO_ATRB dataCount=0x%04X, dataAddress=0x%08X", getDataCount(), getDataAddress()));
				}
				commandCompleted = false;
				break;
			case MSPRO_CMD_IN_IO_FIFO:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MSPRO_CMD_IN_IO_FIFO dataCount=0x%04X, dataAddress=0x%08X", getDataCount(), getDataAddress()));
				}
				commandCompleted = false;
				break;
			case MSPRO_CMD_OUT_IO_FIFO:
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.startCmd MSPRO_CMD_OUT_IO_FIFO dataCount=0x%04X, dataAddress=0x%08X", getDataCount(), getDataAddress()));
				}
				commandCompleted = false;
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.startCmd unknown cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
				break;
		}

		pageIndex = 0;
		pageDataIndex = 0;
		dataIndex = 0;
		status |= MS_STATUS_UNKNOWN;
		dmaTpcCode = -1;

		if (commandCompleted) {
			// Set only the CED (Command EnD) bit in the INT register,
			// indicating a successful completion
			setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_CED);

			sys |= 0x4000;
			setInterrupt();
		} else {
			// Set only the BREQ (Buffer REQuest) bit in the INT register
			setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_BREQ);
		}
	}

	protected abstract int readData16(int dataAddress, int dataIndex, boolean endOfCommand);

	private int readData16() {
		while (true) {
			synchronized (dmaLock) {
				if (dmaTpcCode == MS_TPC_READ_IO_DATA || dmaTpcCode == MS_TPC_READ_REG || dmaTpcCode == MS_TPC_GET_INT) {
					break;
				}
			}

			Utilities.sleep(10);
		}

		int dataAddress = getDataAddress();
		int value = 0;
		synchronized (dmaLock) {
			if (pageDataIndex < readSize) {
				boolean endOfCommand = dataIndex + 2 >= getDataCount();
				value = readData16(dataAddress, dataIndex, endOfCommand);
				pageDataIndex += 2;
				dataIndex += 2;
				if (endOfCommand) {
					// Set only the CED (Command EnD) bit in the INT register,
					// indicating a successful completion
					setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_CED);
					clearBusy();
					status |= MS_STATUS_UNKNOWN;
					sys |= 0x4000;
					setInterrupt();
				}
			}
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
			status |= MS_STATUS_UNKNOWN;
			sys |= 0x4000;
			unk08 |= 0x0040;
			unk08 &= ~0x000F; // Clear error code
			setInterrupt();
		}

		return value;
	}

	private int readPageData16() {
		int value;

		switch (cmd) {
			case MS_CMD_BLOCK_READ:
				if (pageDataIndex == 0) {
					readPageBuffer();
				}
				value = pageBufferMemory.read16(pageDataIndex);
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.readPageData16 unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
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
				status |= MS_STATUS_UNKNOWN;
				sys |= 0x4000;
				unk08 |= 0x0040;
				unk08 &= ~0x000F; // Clear error code
				setInterrupt();
			}
		}

		return value;
	}

	private void setNumberOfPages(int numberOfPages) {
		this.numberOfPages = numberOfPages;
		pageIndex = 0;
		pageDataIndex = 0;
	}

	private void setPageLba(int pageLba) {
		this.pageLba = pageLba;
		pageIndex = 0;
		pageDataIndex = 0;
	}

	private void setStartBlock(int startBlock) {
		this.startBlock = startBlock;
		pageIndex = 0;
		pageDataIndex = 0;
		oobIndex = 0;
	}

	private void setCmd(int cmd) {
		this.cmd = cmd;
	}

	protected abstract void readPageBuffer();

	private int readTPCData32() {
		int data = 0;
		switch (getTPCCode()) {
			case MS_TPC_GET_INT:
				data = getRegisterValue(MS_INT_REG_ADDRESS);
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.readTPCData32 MS_TPC_GET_INT registers[0x%02X]=0x%02X", MS_INT_REG_ADDRESS, data));
				}
				break;
			case MS_TPC_READ_REG:
				for (int i = 0; i < 32; i += 8) {
					if (readSize <= 0 || readAddress >= registers.length) {
						break;
					}
					data |= getRegisterValue(readAddress) << i;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.readTPCData32 MS_TPC_READ_REG registers[0x%02X]=0x%02X", readAddress, getRegisterValue(readAddress)));
					}
					readSize--;
					readAddress++;
				}
				break;
			case MS_TPC_READ_IO_DATA:
				data = readData16() | (readData16() << 16);
				break;
			case MS_TPC_READ_PAGE_DATA:
				if (pageDataIndex == 0) {
					readPageBuffer();
				}
				data = pageBufferMemory.read32(pageDataIndex);
				increaseReadPageDataIndex(4);
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.readTPCData32 unimplemented tpcCode=0x%01X(%s)", getTPCCode(), getTPCName(getTPCCode())));
				data = 0;
				break;
		}

		return data;
	}

	protected abstract void initMsproAttributeMemory();

	private void increaseReadPageDataIndex(int length) {
		pageDataIndex += length;
		if (pageDataIndex >= PAGE_SIZE) {
			pageDataIndex = 0;
			pageIndex++;
			if (pageIndex >= numberOfPages) {
				setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_CED);
				pageIndex = 0;
				commandDataIndex = 0;
				clearBusy();
				status |= MS_STATUS_UNKNOWN;
				sys |= 0x4000;
				unk08 |= 0x0040;
				unk08 &= ~0x000F; // Clear error code
				setInterrupt();
			}
		}
	}

	private int readPageData32() {
		int value;

		switch (cmd) {
			case MSPRO_CMD_READ_ATRB:
				initMsproAttributeMemory();
				value = msproAttributeMemory.read32((pageLba * PAGE_SIZE) + pageDataIndex);
				break;
			case MSPRO_CMD_READ_DATA:
				if (pageDataIndex == 0) {
					readPageBuffer();
				}
				value = pageBufferMemory.read32(pageDataIndex);
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.readPageData32 unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
				value = 0;
				break;
		}

		increaseReadPageDataIndex(4);

		return value;
	}

	protected int getRegisterValue(int register) {
		return registers[register] & 0xFF;
	}

	protected int getRegisterValue(int reg, int length) {
		int value = 0;
		for (int i = 0; i < length; i++) {
			value = (value << 8) | (getRegisterValue(reg + i));
		}

		return value;
	}

	protected void setRegisterValue(int register, int value) {
		registers[register] = value;
	}

	protected void setRegisterValue(int register, int length, int value) {
		for (int i = length - 1; i >= 0; i--) {
			registers[register + i] = value & 0xFF;
			value >>>= 8;
		}
	}

	private void writeTPCData16(int value) {
		if (tpc < 0) {
			// Ignore this data
			return;
		}

		switch (getTPCCode()) {
			case MS_TPC_WRITE_IO_DATA:
				int dataAddress = getDataAddress();
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.writeTPCData16 MS_TPC_WRITE_IO_DATA dataAddress=0x%X, pageDataIndex=0x%X, writeSize=0x%X, dataIndex=0x%X, value=0x%04X", dataAddress, pageDataIndex, writeSize, dataIndex, value));
				}
				if (pageDataIndex < writeSize) {
					boolean endOfCommand = dataIndex + 2 >= getDataCount();
					writeData16(dataAddress, dataIndex, value, endOfCommand);
					pageDataIndex += 2;
					dataIndex += 2;
					if (endOfCommand) {
						// Set only the CED (Command EnD) bit in the INT register,
						// indicating a successful completion
						setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_CED);
						clearBusy();
						status |= MS_STATUS_UNKNOWN;
						sys |= 0x4000;
						setInterrupt();
					}
				}
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.writeTPCData16 unimplemented TPCCode=0x%X", getTPCCode()));
				break;
		}
	}

	private void writeTPCData(int value) {
		if (tpc < 0) {
			// Ignore this data
			return;
		}

		int dataAddress;
		switch (getTPCCode()) {
			case MS_TPC_SET_RW_REG_ADDRESS:
				// Sets the read address & size, write address & size
				// for a subsequent MS_CMD_READ_REG/MS_CMD_WRITE_REG command
				readAddress = value & 0xFF;
				readSize = (value >> 8) & 0xFF;
				writeAddress = (value >> 16) & 0xFF;
				writeSize = (value >> 24) & 0xFF;
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.writeTPCData MS_TPC_SET_RW_REG_ADDRESS readAddress=0x%02X, readSize=0x%X, writeAddress=0x%02X, writeSize=0x%X", readAddress, readSize, writeAddress, writeSize));
				}
				// Ignore further data
				tpc = -1;
				break;
			case MS_TPC_WRITE_REG:
				for (int i = 0; i < 4; i++) {
					if (writeSize <= 0 || writeAddress >= registers.length) {
						break;
					}
					setRegisterValue(writeAddress, value & 0xFF);
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerBaseMemoryStick.writeTPCData MS_TPC_WRITE_REG registers[0x%02X]=0x%02X", writeAddress, getRegisterValue(writeAddress)));
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
			case MS_TPC_EX_SET_CMD:
				switch (tpcExSetCmdIndex) {
					case 0:
						setCmd(value & 0xFF);
						setRegisterValue(0x11, (value >>  8) & 0xFF);
						setRegisterValue(0x12, (value >> 16) & 0xFF);
						setRegisterValue(0x13, (value >> 24) & 0xFF);
						break;
					case 1:
						setRegisterValue(0x14, (value >>  0) & 0xFF);
						setRegisterValue(0x15, (value >>  8) & 0xFF);
						setRegisterValue(0x16, (value >> 16) & 0xFF);
						startCmd(cmd);
						break;
					default:
						log.error(String.format("Too many parameters to MS_TPC_EX_SET_CMD: 0x%X", value));
						break;
				}
				tpcExSetCmdIndex++;
				break;
			case MS_TPC_WRITE_IO_DATA:
				dataAddress = getDataAddress();
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.writeTPCData MS_TPC_WRITE_IO_DATA dataAddress=0x%X, pageDataIndex=0x%X, writeSize=0x%X, dataIndex=0x%X, value=0x%08X", dataAddress, pageDataIndex, writeSize, dataIndex, value));
				}
				if (pageDataIndex < writeSize) {
					boolean endOfCommand = dataIndex + 4 >= getDataCount();
					writeData32(dataAddress, dataIndex, value, endOfCommand);
					pageDataIndex += 4;
					dataIndex += 4;
					if (endOfCommand) {
						// Set only the CED (Command EnD) bit in the INT register,
						// indicating a successful completion
						setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_CED);
						clearBusy();
						status |= MS_STATUS_UNKNOWN;
						sys |= 0x4000;
						setInterrupt();
					}
				}
				break;
			case MS_TPC_WRITE_PAGE_DATA:
				pageBufferMemory.write32(pageDataIndex, value);
				increaseWritePageDataIndex(4);
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.writeTPCData unimplemented tpcCode=0x%01X(%s)", getTPCCode(), getTPCName(getTPCCode())));
				break;
		}
	}

	protected abstract void writeData16(int dataAddress, int dataIndex, int value, boolean endOfCommand);
	protected abstract void writeData32(int dataAddress, int dataIndex, int value, boolean endOfCommand);

	private void writeCommandData8(int value) {
		switch (commandDataIndex) {
			case 0:
				setCmd(value);
				tpc = MS_TPC_SET_CMD;
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerBaseMemoryStick.writeCommandData8 cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
				}

				switch (cmd) {
					case MSPRO_CMD_READ_ATRB:
						break;
					case MSPRO_CMD_READ_DATA:
						break;
					case MSPRO_CMD_WRITE_DATA:
						break;
					default:
						log.error(String.format("MMIOHandlerBaseMemoryStick.writeCommandData8 unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
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
				pageLba = (pageLba & 0x00FFFFFF) | (value << 24);
				break;
			case 4:
				pageLba = (pageLba & 0xFF00FFFF) | (value << 16);
				break;
			case 5:
				pageLba = (pageLba & 0xFFFF00FF) | (value << 8);
				break;
			case 6:
				setStartBlock(0);
				setPageLba((pageLba & 0xFFFFFF00) | value);
				if (log.isDebugEnabled()) {
					log.debug(String.format("pageLba=0x%X", pageLba));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.writeCommandData8 unknown data 0x%02X written at index 0x%X", value, commandDataIndex));
				break;
		}
		commandDataIndex++;
	}

	protected abstract void writePageBuffer();

	private void increaseWritePageDataIndex(int length) {
		pageDataIndex += 4;
		if (pageDataIndex >= PAGE_SIZE) {
			pageDataIndex = 0;
			if (log.isDebugEnabled()) {
				log.debug(String.format("MMIOHandlerBaseMemoryStick.increaseWritePageDataIndex writing page 0x%X/0x%X", pageIndex, numberOfPages));
			}
			writePageBuffer();
			pageIndex++;
			if (pageIndex >= numberOfPages) {
				setRegisterValue(MS_INT_REG_ADDRESS, MS_INT_REG_CED);
				pageIndex = 0;
				commandDataIndex = 0;
				clearBusy();
				status |= MS_STATUS_UNKNOWN;
				sys |= 0x4000;
				unk08 |= 0x0040;
				unk08 &= ~0x000F; // Clear error code
				setInterrupt();
			}
		}
	}

	private void writePageData32(int value) {
		switch (cmd) {
			case MSPRO_CMD_WRITE_DATA:
				pageBufferMemory.write32(pageDataIndex, value);
				break;
			default:
				log.error(String.format("MMIOHandlerBaseMemoryStick.writePageData32 unimplemented cmd=0x%02X(%s)", cmd, getCommandName(cmd)));
				break;
		}

		increaseWritePageDataIndex(4);
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
			case 0x38: value = getStatus(); break;
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
			case 0x30: value = tpc; break;
			case 0x34: value = readTPCData32(); break;
			case 0x38: value = getStatus(); break;
			case 0x3C: value = sys; break;
			case 0x40: value = unk40; break;
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
			case 0x16: setCmd(MS_CMD_BLOCK_READ); setPageLba(value); break;
			case 0x20: break; // TODO Unknown
			case 0x30: startTPC(value & 0xFFFF); break;
			case 0x34: writeTPCData16(value & 0xFFFF); break;
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
			case 0x28: writePageData32(value); break;
			case 0x30: startTPC(value); break;
			case 0x34: writeTPCData(value); break;
			case 0x3C: writeSys(value); break;
			case 0x40: unk40 = value; break;
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
