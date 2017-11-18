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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MSCM0_INTR;

import java.util.Arrays;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.hardware.MemoryStick;

public class MMIOHandlerMemoryStick extends MMIOHandlerBase {
	private static final boolean simulateMemoryStickPro = false;
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
	public static final int MS_CMD_BLOCK_READ  = 0xAA;
	public static final int MS_CMD_BLOCK_WRITE = 0x55;
	public static final int MS_CMD_BLOCK_END   = 0x33;
	public static final int MS_CMD_BLOCK_ERASE = 0x99;
	public static final int MS_CMD_FLASH_STOP  = 0xCC;
	public static final int MS_CMD_CLEAR_BUF   = 0xC3;
	public static final int MS_CMD_RESET       = 0x3C;
	// MS Pro commands
	public static final int MS_CMD_READ_DATA   = 0x20;
	public static final int MS_CMD_WRITE_DATA  = 0x21;
	public static final int MS_CMD_READ_ATRB   = 0x24;
	public static final int MS_CMD_STOP        = 0x25;
	public static final int MS_CMD_ERASE       = 0x26;
	public static final int MS_CMD_SET_IBD     = 0x46;
	public static final int MS_CMD_GET_IBD     = 0x47;
	public static final int MS_CMD_SLEEP       = 0x11;
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

	public MMIOHandlerMemoryStick(int baseAddress) {
		super(baseAddress);

		reset();
	}

	private void reset() {
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
			log.debug(String.format("startTPC tpcCode=0x%01X, unknown=0x%03X", tpcCode, unknown));
		}

		switch (tpcCode) {
			case MS_TPC_SET_RW_REG_ADDRESS:
				// Data will be set at next writeData()
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
				// Data will be retrieve at next readData()
				break;
			default:
				log.error(String.format("MMIOHandlerMemoryStick.startTPC unknown TPC 0x%01X", tpcCode));
				break;
		}

		status |= MS_STATUS_FIFO_RW;
	}

	private int getRegisterValue(int reg, int length) {
		int value = 0;
		for (int i = 0, shift = 0; i < length; i++, shift += 8) {
			value |= (registers[reg + i] & 0xFF) << shift;
		}

		return value;
	}

	private int getDataCount() {
		if (isMemoryStickPro()) {
			return getRegisterValue(0x11, 2);
		}
		return getRegisterValue(0x14, 1) << 4;
	}

	private int getDataAddress() {
		if (isMemoryStickPro()) {
			return getRegisterValue(0x13, 4);
		}
		return getRegisterValue(0x11, 3);
	}

	private void startCmd(int cmd) {
		this.cmd = cmd;

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerMemoryStick.startCmd cmd=0x%02X", cmd));
		}

		switch (cmd) {
			case MS_CMD_BLOCK_READ:
				// TODO Unknown command
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startCmd MS_CMD_BLOCK_READ dataCount=0x%04X, dataAddress=0x%08X", getDataCount(), getDataAddress()));
				}
				if (!isMemoryStickPro()) {
					registers[0x16] = 0x80;
					registers[0x17] = 0x00;
				}
				break;
		}

		// Set only the CED (Command EnD) bit in the INT register,
		// indicating a successful completion
		registers[MS_INT_REG_ADDRESS] = MS_INT_REG_CED;

		status |= 0x2000;
		sys |= 0x4000;
		setInterrupt(0x4);
	}

	private int readData16() {
		if (!isSerialMode()) {
			log.error(String.format("MMIOHandlerMemoryStick.readData16 not supported for parallel mode"));
			return 0;
		}

		// TODO
		return 0;
	}

	private int readData32() {
		if (isSerialMode()) {
			return readData32Serial();
		}
		return readData32Parallel();
	}

	private int readData32Serial() {
		int data = 0;
		switch (getTPCCode()) {
			case MS_TPC_GET_INT:
				data = registers[MS_INT_REG_ADDRESS] & 0xFF;
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerMemoryStick.startTPC MS_TPC_GET_INT registers[0x%02X]=0x%02X", MS_INT_REG_ADDRESS, data));
				}
				break;
			case MS_TPC_READ_REG:
				for (int i = 0; i < 32; i += 8) {
					if (readSize <= 0 || readAddress >= registers.length) {
						break;
					}
					data |= (registers[readAddress] & 0xFF) << i;
					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerMemoryStick.readData MS_TPC_READ_REG registers[0x%02X]=0x%02X", readAddress, registers[readAddress]));
					}
					readSize--;
					readAddress++;
				}
				break;
		}

		return data;
	}

	int count = 0;
	private int readData32Parallel() {
		count++;
		if (count == 128) {
			clearBusy();
			status |= 0x2000;
			sys |= 0x4000;
			setInterrupt(0x0004);
		}
		return 0;
	}

	private void writeDataSerial(int value) {
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
					log.debug(String.format("MMIOHandlerMemoryStick.writeData MS_TPC_SET_RW_REG_ADDRESS readAddress=0x%02X, readSize=0x%X, writeAddress=0x%02X, writeSize=0x%X", readAddress, readSize, writeAddress, writeSize));
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
						log.debug(String.format("MMIOHandlerMemoryStick.writeData MS_TPC_WRITE_REG registers[0x%02X]=0x%02X", writeAddress, registers[writeAddress]));
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

	private void writeDataParallel(int value) {
		// TODO
		setBusy();
	}

	@Override
	public int read16(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = interrupt; break;
			case 0x04: value = commandState; break;
			case 0x08: value = unk08; break;
			case 0x30: value = tpc; break;
			case 0x34: value = readData16(); break;
			case 0x38: value = status; break;
			case 0x3C: value = sys; break;
			default: value = super.read16(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read16(0x%08X) returning 0x%04X", Emulator.getProcessor().cpu.pc, address, value));
		}

		return value;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x28: value = readData32Parallel(); break;
			case 0x2C: value = readData32Parallel(); break;
			case 0x30: value = readData32Parallel(); break;
			case 0x34: value = readData32(); break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", Emulator.getProcessor().cpu.pc, address, value));
		}

		return value;
	}

	@Override
	public void write16(int address, short value) {
		switch (address - baseAddress) {
			case 0x00: clearInterrupt(value & 0xFFFF); break;
			case 0x02: break; // TODO Unknown, used in parallel mode
			case 0x04: writeCommandState(value & 0xFFFF); break;
			case 0x10: break; // TODO Unknown, used in parallel mode
			case 0x20: break; // TODO Unknown, used in parallel mode
			case 0x30: startTPC(value & 0xFFFF); break;
			case 0x38: status = value & 0xFFFF; break;
			case 0x3C: writeSys(value & 0xFFFF); break;
			default: super.write16(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x34: writeDataSerial(value); break;
			case 0x40: break; // TODO Unknown
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}

	@Override
	public void write8(int address, byte value) {
		switch (address - baseAddress) {
			case 0x24: writeDataParallel(value & 0xFF); break;
			default: super.write8(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write8(0x%08X, 0x%02X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}
}
