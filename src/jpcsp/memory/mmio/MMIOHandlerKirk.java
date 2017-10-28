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

import static jpcsp.Emulator.getProcessor;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MEMLMD_INTR;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;

public class MMIOHandlerKirk extends MMIOHandlerBase {
	public static final int RESULT_SUCCESS = 0;
	public static final int RESULT_KIRK_NOT_ENABLED = 1;
	public static final int RESULT_INVALID_MODE = 2;
	public static final int RESULT_HEADER_CHECK_INVALID = 3;
	public static final int RESULT_DATA_CHECK_INVALID = 4;
	public static final int RESULT_SIG_CHECK_INVALID = 5;
	public static final int RESULT_KIRK_NOT_INITIALIZED = 12;
	public static final int RESULT_INVALID_OPERATION = 13;
	public static final int RESULT_INVALID_SEED_CODE = 14;
	public static final int RESULT_INVALID_SIZE = 15;
	public static final int RESULT_DATA_SIZE_IS_ZERO = 16;
	public static final int STATUS_PHASE1_MASK = 0x11;
	public static final int STATUS_PHASE1_IN_PROGRESS = 0x00;
	public static final int STATUS_PHASE1_COMPLETED = 0x01;
	public static final int STATUS_PHASE1_ERROR = 0x10;
	public static final int STATUS_PHASE2_MASK = 0x22;
	public static final int STATUS_PHASE2_IN_PROGRESS = 0x00;
	public static final int STATUS_PHASE2_COMPLETED = 0x02;
	public static final int STATUS_PHASE2_ERROR = 0x20;
	public int signature = 0x4B52494B; // KIRK
	public int version = 0x30313030; // 0010
	public int error;
	public int command;
	public int result = RESULT_SUCCESS;
	public int status = STATUS_PHASE1_IN_PROGRESS | STATUS_PHASE2_IN_PROGRESS;
	public int statusAsync;
	public int statusAsyncEnd;
	public int sourceAddr;
	public int destAddr;

	public MMIOHandlerKirk(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public int read32(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, this));
		}

		switch (address - baseAddress) {
			case 0x00: return signature;
			case 0x04: return version;
			case 0x08: return error;
			case 0x10: return command;
			case 0x14: return result;
			case 0x1C: return status;
			case 0x20: return statusAsync;
			case 0x24: return statusAsyncEnd;
			case 0x2C: return sourceAddr;
			case 0x30: return destAddr;
		}
		return super.read32(address);
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x08: error = value; break;
			case 0x0C: startProcessing(value); break;
			case 0x10: command = value; break;
			case 0x14: result = value; break;
			case 0x1C: status = value; break;
			case 0x20: statusAsync = value; break;
			case 0x24: statusAsyncEnd = value; break;
			case 0x28: endProcessing(value); break;
			case 0x2C: sourceAddr = value; break;
			case 0x30: destAddr = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}

	private void startProcessing(int value) {
		switch (value) {
			case 1:
				setStatus(STATUS_PHASE1_MASK, STATUS_PHASE1_IN_PROGRESS);
				if (log.isDebugEnabled()) {
					log.debug(String.format("KIRK startProcessing 1 on %s", this));
				}
				result = RESULT_SUCCESS;
				setStatus(STATUS_PHASE1_MASK, STATUS_PHASE1_COMPLETED);
				RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_MEMLMD_INTR);
				break;
			case 2:
				setStatus(STATUS_PHASE2_MASK, STATUS_PHASE2_IN_PROGRESS);
				if (log.isDebugEnabled()) {
					log.debug(String.format("KIRK startProcessing 2 on %s", this));
				}
				break;
			default:
				log.warn(String.format("0x%08X - KIRK unknown startProcessing value 0x%X on %s", Emulator.getProcessor().cpu.pc, value, this));
				break;
		}
	}

	private void endProcessing(int value) {
		switch (value) {
			case 1:
				if (log.isDebugEnabled()) {
					log.debug(String.format("KIRK endProcessing 1 on %s", this));
				}
				RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_MEMLMD_INTR);
				break;
			case 2:
				if (log.isDebugEnabled()) {
					log.debug(String.format("KIRK endProcessing 2 on %s", this));
				}
				break;
			default:
				log.warn(String.format("0x%08X - KIRK unknown endProcessing value 0x%X on %s", Emulator.getProcessor().cpu.pc, value, this));
				break;
		}
	}

	private void setStatus(int mask, int value) {
		status = (status & ~mask) | value;
	}

	@Override
	public String toString() {
		return String.format("KIRK error=0x%X, command=0x%X, result=0x%X, status=0x%X, statusAsync=0x%X, statusAsyncEnd=0x%X, sourceAddr=0x%08X, destAddr=0x%08X", error, command, result, status, statusAsync, statusAsyncEnd, sourceAddr, destAddr);
	}
}
