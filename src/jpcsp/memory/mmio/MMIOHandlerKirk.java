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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.semaphore;
import jpcsp.util.Utilities;

public class MMIOHandlerKirk extends MMIOHandlerBase {
	private static Logger log = semaphore.log;
	public static final int RESULT_SUCCESS = 0;
	public static final int STATUS_PHASE1_IN_PROGRESS = 0x00;
	public static final int STATUS_PHASE1_COMPLETED = 0x01;
	public static final int STATUS_PHASE1_ERROR = 0x10;
	public static final int STATUS_PHASE1_MASK = STATUS_PHASE1_COMPLETED | STATUS_PHASE1_ERROR;
	public static final int STATUS_PHASE2_IN_PROGRESS = 0x00;
	public static final int STATUS_PHASE2_COMPLETED = 0x02;
	public static final int STATUS_PHASE2_ERROR = 0x20;
	public static final int STATUS_PHASE2_MASK = STATUS_PHASE2_COMPLETED | STATUS_PHASE2_ERROR;
	private static int dumpIndex = 0;
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
	private static final int commandsToBeDumped[] = {
//		PSP_KIRK_CMD_SHA1_HASH,
//		PSP_KIRK_CMD_DECRYPT_PRIVATE,
	};

	public MMIOHandlerKirk(int baseAddress) {
		super(baseAddress);
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
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	private void hleUtilsBufferCopyWithRange() {
		TPointer outAddr = new TPointer(getMemory(), normalizeAddress(destAddr));
		TPointer inAddr = new TPointer(getMemory(), normalizeAddress(sourceAddr));

		int inSize;
		int outSize;
		int dataSize;
		int dataOffset;
		switch (command) {
			case PSP_KIRK_CMD_ENCRYPT:
			case PSP_KIRK_CMD_ENCRYPT_FUSE:
			case PSP_KIRK_CMD_DECRYPT:
			case PSP_KIRK_CMD_DECRYPT_FUSE:
				// AES128_CBC_Header
				inSize = inAddr.getValue32(16) + 20;
				outSize = inSize;
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
            	outSize = 0x10; // TODO Unknown outSize?
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
			default:
				log.error(String.format("MMIOHandlerKirk.hleUtilsBufferCopyWithRange unimplemented KIRK command 0x%X", command));
				result = PSP_KIRK_INVALID_OPERATION;
				return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("hleUtilsBufferCopyWithRange input: %s", Utilities.getMemoryDump(inAddr, inSize)));
		}

		for (int commandToBeDumped : commandsToBeDumped) {
			if (command == commandToBeDumped) {
				String dumpFileName = String.format("dump.hleUtilsBufferCopyWithRange.%d", dumpIndex++);
				log.warn(String.format("MMIOHandlerKirk: hleUtilsBufferCopyWithRange dumping command=0x%X, outputSize=0x%X, inputSize=0x%X, input dumped into file '%s'", command, outSize, inSize, dumpFileName));
				try {
					OutputStream dump = new FileOutputStream(dumpFileName);
					byte[] inputBuffer = new byte[inSize];
					for (int i = 0; i < inSize; i++) {
						inputBuffer[i] = inAddr.getValue8(i);
					}
					dump.write(inputBuffer);
					dump.close();
				} catch (IOException e) {
				}
			}
		}

		result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(outAddr, outSize, inAddr, inSize, command);

		if (log.isDebugEnabled()) {
			log.debug(String.format("hleUtilsBufferCopyWithRange result=0x%X, output: %s", result, Utilities.getMemoryDump(outAddr, outSize)));
		}

		if (result != 0) {
			String dumpFileName = String.format("dump.hleUtilsBufferCopyWithRange.%d", dumpIndex++);
			log.warn(String.format("MMIOHandlerKirk: hleUtilsBufferCopyWithRange returned error result=0x%X for command=0x%X, outputSize=0x%X, inputSize=0x%X, input dumped into file '%s'", result, command, outSize, inSize, dumpFileName));
			try {
				OutputStream dump = new FileOutputStream(dumpFileName);
				byte[] inputBuffer = new byte[inSize];
				for (int i = 0; i < inSize; i++) {
					inputBuffer[i] = inAddr.getValue8(i);
				}
				dump.write(inputBuffer);
				dump.close();
			} catch (IOException e) {
			}
		}
	}

	private void startProcessing(int value) {
		switch (value) {
			case 1:
				setStatus(STATUS_PHASE1_MASK, STATUS_PHASE1_IN_PROGRESS);
				if (log.isDebugEnabled()) {
					log.debug(String.format("KIRK startProcessing 1 on %s", this));
				}
				hleUtilsBufferCopyWithRange();
				setStatus(STATUS_PHASE1_MASK, STATUS_PHASE1_COMPLETED);
				RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_MEMLMD_INTR);
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

	@Override
	public String toString() {
		return String.format("KIRK error=0x%X, command=0x%X, result=0x%X, status=0x%X, statusAsync=0x%X, statusAsyncEnd=0x%X, sourceAddr=0x%08X, destAddr=0x%08X", error, command, result, status, statusAsync, statusAsyncEnd, sourceAddr, destAddr);
	}
}
