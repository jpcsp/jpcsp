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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MEMLMD_INTR;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_CERT_VERIFY;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_DECRYPT;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_DECRYPT_FUSE;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_DECRYPT_PRIVATE;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_DECRYPT_SIGN;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_GEN_KEYS;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_SIGN;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_VERIFY;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ENCRYPT;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ENCRYPT_FUSE;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_INIT;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_PRIV_SIG_CHECK;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_PRNG;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_SHA1_HASH;
import static jpcsp.crypto.KIRK.PSP_KIRK_INVALID_OPERATION;
import static jpcsp.memory.mmio.MMIO.normalizeAddress;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.semaphore;
import jpcsp.scheduler.Scheduler;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class MMIOHandlerKirk extends MMIOHandlerBase {
	private static Logger log = semaphore.log;
	private static final int STATE_VERSION = 0;
	public static final int RESULT_SUCCESS = 0;
	public static final int STATUS_PHASE1_IN_PROGRESS = 0x00;
	public static final int STATUS_PHASE1_COMPLETED = 0x01;
	public static final int STATUS_PHASE1_ERROR = 0x10;
	public static final int STATUS_PHASE1_MASK = STATUS_PHASE1_COMPLETED | STATUS_PHASE1_ERROR;
	public static final int STATUS_PHASE2_IN_PROGRESS = 0x00;
	public static final int STATUS_PHASE2_COMPLETED = 0x02;
	public static final int STATUS_PHASE2_ERROR = 0x20;
	public static final int STATUS_PHASE2_MASK = STATUS_PHASE2_COMPLETED | STATUS_PHASE2_ERROR;
	private final int signature = 0x4B52494B; // "KIRK"
	private final int version = 0x30313030; // "0010"
	private int error;
	private int command;
	private int result = RESULT_SUCCESS;
	private int status = STATUS_PHASE1_IN_PROGRESS | STATUS_PHASE2_IN_PROGRESS;
	private int statusAsync;
	private int statusAsyncEnd;
	private int sourceAddr;
	private int destAddr;
	private final CompletePhase1Action completePhase1Action = new CompletePhase1Action();
	private long completePhase1Schedule = 0L;

	private class CompletePhase1Action implements IAction {
		@Override
		public void execute() {
			completePhase1();
		}
	}

	public MMIOHandlerKirk(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		error = stream.readInt();
		command = stream.readInt();
		result = stream.readInt();
		status = stream.readInt();
		statusAsync = stream.readInt();
		statusAsyncEnd = stream.readInt();
		sourceAddr = stream.readInt();
		destAddr = stream.readInt();
		super.read(stream);

		completePhase1Schedule = 0L;
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(error);
		stream.writeInt(command);
		stream.writeInt(result);
		stream.writeInt(status);
		stream.writeInt(statusAsync);
		stream.writeInt(statusAsyncEnd);
		stream.writeInt(sourceAddr);
		stream.writeInt(destAddr);
		super.write(stream);
	}

	@Override
	public int read32(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) on %s", getPc(), address, this));
		}

		switch (address - baseAddress) {
			case 0x00: return signature;
			case 0x04: return version;
			case 0x08: return error;
			case 0x0C: return 0; // Unknown
			case 0x10: return command;
			case 0x14: return result;
			case 0x1C: return getStatus();
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
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	private int hleUtilsBufferCopyWithRange() {
		TPointer outAddr = new TPointer(getMemory(), normalizeAddress(destAddr));
		TPointer inAddr = new TPointer(getMemory(), normalizeAddress(sourceAddr));

		int inSize;
		int outSize;
		int dataSize;
		int dataOffset;
		switch (command) {
			case PSP_KIRK_CMD_ENCRYPT:
			case PSP_KIRK_CMD_ENCRYPT_FUSE:
				// AES128_CBC_Header
				dataSize = inAddr.getValue32(16);
				inSize = dataSize + 20;
				outSize = dataSize + 20;
				break;
			case PSP_KIRK_CMD_DECRYPT:
			case PSP_KIRK_CMD_DECRYPT_FUSE:
				// AES128_CBC_Header
				dataSize = inAddr.getValue32(16);
				inSize = dataSize + 20;
				outSize = dataSize;
				break;
			case PSP_KIRK_CMD_DECRYPT_PRIVATE:
				// AES128_CMAC_Header
				dataSize = inAddr.getValue32(112);
				dataOffset = inAddr.getValue32(116);
				inSize = 144 + Utilities.alignUp(dataSize, 15) + dataOffset;
				outSize = Utilities.alignUp(dataSize, 15);
				break;
			case PSP_KIRK_CMD_PRIV_SIG_CHECK:
				// AES128_CMAC_Header
				dataSize = inAddr.getValue32(112);
				dataOffset = inAddr.getValue32(116);
				inSize = 144 + Utilities.alignUp(dataSize, 15) + dataOffset;
				outSize = 0;
				break;
			case PSP_KIRK_CMD_SHA1_HASH:
				// SHA1_Header
				inSize = inAddr.getValue32(0) + 4;
				outSize = 20;
				break;
            case PSP_KIRK_CMD_ECDSA_GEN_KEYS:
            	inSize = 0;
				outSize = 0x3C;
            	break;
            case PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT:
            	inSize = 0x3C;
            	outSize = 0x28;
            	break;
            case PSP_KIRK_CMD_PRNG:
            	inSize = 0;
            	outSize = 0x14;
            	break;
            case PSP_KIRK_CMD_ECDSA_SIGN:
            	inSize = 0x34;
            	outSize = 0x28;
            	break;
            case PSP_KIRK_CMD_ECDSA_VERIFY:
            	inSize = 0x64;
            	outSize = 0;
            	break;
            case PSP_KIRK_CMD_INIT:
            	inSize = 0;
            	outSize = 0;
            	break;
            case PSP_KIRK_CMD_CERT_VERIFY:
            	inSize = 0xB8;
            	outSize = 0;
            	break;
            case PSP_KIRK_CMD_DECRYPT_SIGN:
            	inSize = 0x200;
            	outSize = 0x200;
            	break;
			default:
				log.error(String.format("MMIOHandlerKirk.hleUtilsBufferCopyWithRange unimplemented KIRK command 0x%X", command));
				result = PSP_KIRK_INVALID_OPERATION;
				return 0;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("hleUtilsBufferCopyWithRange input: %s", Utilities.getMemoryDump(inAddr, inSize)));
		}

		result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(outAddr, outSize, inAddr, inSize, command);

		if (log.isDebugEnabled()) {
			log.debug(String.format("hleUtilsBufferCopyWithRange result=0x%X, output: %s", result, Utilities.getMemoryDump(outAddr, outSize)));
		}

		return Math.max(inSize, outSize);
	}

	private void completePhase1() {
		if (completePhase1Schedule != 0L) {
			Scheduler.getInstance().removeAction(completePhase1Schedule, completePhase1Action);
			completePhase1Schedule = 0L;
		}

		setStatus(STATUS_PHASE1_MASK, STATUS_PHASE1_COMPLETED);
		RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_MEMLMD_INTR);
	}

	private void startProcessing(int value) {
		switch (value) {
			case 1:
				setStatus(STATUS_PHASE1_MASK, STATUS_PHASE1_IN_PROGRESS);
				if (log.isDebugEnabled()) {
					log.debug(String.format("KIRK startProcessing 1 on %s", this));
				}

				int size = hleUtilsBufferCopyWithRange();

				if (result == 0) {
					// Duration: 360 us per 0x1000 bytes data
					int delayUs = Math.max(0, size * 360 / 0x1000);
					completePhase1Schedule = Scheduler.getNow() + delayUs;
					Scheduler.getInstance().addAction(completePhase1Schedule, completePhase1Action);
					if (log.isDebugEnabled()) {
						log.debug(String.format("KIRK delaying completion of phase 1 by %d us", delayUs));
					}
				} else {
					completePhase1();
				}
				break;
			case 2:
				setStatus(STATUS_PHASE2_MASK, STATUS_PHASE2_IN_PROGRESS);
				log.error(String.format("Unimplemented Phase 2 KIRK command 0x%X on %s", command, this));
				log.error(String.format("source: %s", Utilities.getMemoryDump(getMemory(), normalizeAddress(sourceAddr), 0x100)));
				break;
			default:
				log.warn(String.format("0x%08X - KIRK unknown startProcessing value 0x%X on %s", getPc(), value, this));
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
				log.warn(String.format("0x%08X - KIRK unknown endProcessing value 0x%X on %s", getPc(), value, this));
				break;
		}
	}

	private void setStatus(int mask, int value) {
		status = (status & ~mask) | value;
	}

	private void updateStatus() {
		if (completePhase1Schedule != 0L) {
			if (completePhase1Schedule <= Scheduler.getNow()) {
				completePhase1();
			}
		}
	}

	private int getStatus() {
		updateStatus();

		return status;
	}

	@Override
	public String toString() {
		return String.format("KIRK error=0x%X, command=0x%X, result=0x%X, status=0x%X, statusAsync=0x%X, statusAsyncEnd=0x%X, sourceAddr=0x%08X, destAddr=0x%08X", error, command, result, status, statusAsync, statusAsyncEnd, sourceAddr, destAddr);
	}
}
