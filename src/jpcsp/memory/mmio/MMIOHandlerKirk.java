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
import static jpcsp.HLE.modules.memlmd.getKey;
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
import static jpcsp.memory.mmio.MMIO.getKeyBFD00210;
import static jpcsp.memory.mmio.MMIO.getXorKeyBFD00210;
import static jpcsp.memory.mmio.MMIO.normalizeAddress;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.semaphore;
import jpcsp.hardware.Model;
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
	private int result;
	private int status;
	private int statusAsync;
	private int statusAsyncEnd;
	private int sourceAddr;
	private int destAddr;
	private final CompletePhase1Action completePhase1Action = new CompletePhase1Action();
	private long completePhase1Schedule;
	final int[] lastPrngOutput = new int[20];
	int stepPreDecryptKeySeed0x15 = 0;

	private class CompletePhase1Action implements IAction {
		@Override
		public void execute() {
			completePhase1();
		}
	}

	public MMIOHandlerKirk(int baseAddress) {
		super(baseAddress);

		reset();
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
		stream.readInts(lastPrngOutput);
		stepPreDecryptKeySeed0x15 = stream.readInt();
		super.read(stream);

		// If the phase1 was in progress, complete it
		if ((status & STATUS_PHASE1_MASK) == STATUS_PHASE1_IN_PROGRESS) {
			completePhase1Schedule = Scheduler.getNow();
			Scheduler.getInstance().addAction(completePhase1Schedule, completePhase1Action);
		} else {
			completePhase1Schedule = 0L;
		}
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
		stream.writeInts(lastPrngOutput);
		stream.writeInt(stepPreDecryptKeySeed0x15);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		error = 0;
		command = 0;
		result = RESULT_SUCCESS;
		status = STATUS_PHASE1_IN_PROGRESS | STATUS_PHASE2_IN_PROGRESS;
		statusAsync = 0;
		statusAsyncEnd = 0;
		sourceAddr = 0;
		destAddr = 0;
		completePhase1Schedule = 0L;
		Arrays.fill(lastPrngOutput, 0);
		stepPreDecryptKeySeed0x15 = 0;
	}

	private static boolean isEqual(TPointer addr, int length, int[] values) {
		for (int i = 0; i < length; i++) {
			if (addr.getUnsignedValue8(i) != values[i]) {
				return false;
			}
		}

		return true;
	}

	private static boolean isEmpty(TPointer addr, int length) {
		return isEqual(addr, length, new int[length]);
	}

	private boolean preDecrypt(TPointer outAddr, int outSize, TPointer inAddr, int inSize) {
		boolean processed = false;

		if (command == PSP_KIRK_CMD_DECRYPT) {
			int keySeed = inAddr.getValue32(12);
			int dataSize = outSize;
			if (keySeed == 0x15 && dataSize == 16 && Model.getGeneration() >= 2) {
				switch (stepPreDecryptKeySeed0x15) {
					case 1:
						final int[] values = new int[] { 0x61, 0x7A, 0x56, 0x42, 0xF8, 0xED, 0xC5, 0xE4, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B };
						for (int i = 0; i < values.length; i++) {
							values[i] ^= lastPrngOutput[i];
						}

						if (isEqual(new TPointer(inAddr, 20), dataSize, values)) {
				    		if (log.isDebugEnabled()) {
				    			log.debug(String.format("preDecrypt for IPL, keySeed=0x%X step %d", keySeed, stepPreDecryptKeySeed0x15));
				    		}
							processed = true;
							outAddr.clear(outSize);
							result = 0;
							stepPreDecryptKeySeed0x15++;
						} else {
							stepPreDecryptKeySeed0x15 = 0;
						}
						break;
					case 2:
						if (isEmpty(new TPointer(inAddr, 20), dataSize)) {
				    		if (log.isDebugEnabled()) {
				    			log.debug(String.format("preDecrypt for IPL, keySeed=0x%X step %d", keySeed, stepPreDecryptKeySeed0x15));
				    		}
							final int key1[] = new int[] { 0xDB, 0xB1, 0x1E, 0x20, 0x48, 0x83, 0xB1, 0x6F, 0x65, 0x8C, 0x3D, 0x30, 0xE0, 0xFE, 0xCB, 0xBF };
							final byte xorKey1_1[] = getKey(getXorKeyBFD00210());
							final int keyFromVault1[] = getKeyBFD00210();
							final int xorKey1_2[] = new int[] { 0x0B, 0xDD, 0xED, 0xC7, 0xB5, 0x24, 0xBC, 0x22 };
							for (int i = 0; i < 8; i++) {
								key1[i + 8] ^= keyFromVault1[i] ^ (xorKey1_1[i] & 0xFF) ^ xorKey1_2[i];
							}
							for (int i = 0; i < key1.length; i++) {
								outAddr.setUnsignedValue8(i, key1[i] ^ (lastPrngOutput[i] + 0x01));
							}
							processed = true;
							result = 0;
							stepPreDecryptKeySeed0x15++;
						} else {
							stepPreDecryptKeySeed0x15 = 0;
						}
						break;
					case 3:
						if (isEmpty(new TPointer(inAddr, 20), dataSize)) {
				    		if (log.isDebugEnabled()) {
				    			log.debug(String.format("preDecrypt for IPL, keySeed=0x%X step %d", keySeed, stepPreDecryptKeySeed0x15));
				    		}
							final int key2[] = new int[] { 0x04, 0xF4, 0x69, 0x8A, 0x8C, 0xAA, 0x95, 0x30, 0xCE, 0x3B, 0xE8, 0x84, 0xCA, 0x9A, 0x07, 0x9A };
							final byte xorKey2_1[] = getKey(getXorKeyBFD00210());
							final int keyFromVault2[] = getKeyBFD00210();
							final int xorKey2_2[] = new int[] { 0x80, 0x85, 0xFA, 0xD6, 0xC9, 0x24, 0xEF, 0x53 };
							for (int i = 0; i < 8; i++) {
								key2[i + 8] ^= keyFromVault2[i + 8] ^ (xorKey2_1[i + 8] & 0xFF) ^ xorKey2_2[i];
							}
							final int add16 = 0xCAB9;
							for (int i = 0; i < key2.length; i += 2) {
								int valueKey16 = key2[i] | (key2[i + 1] << 8);
								int valuePrng16 = lastPrngOutput[i] | (lastPrngOutput[i + 1] << 8);
								outAddr.setUnsignedValue16(i, valueKey16 ^ (valuePrng16 + add16));
							}
							processed = true;
							result = 0;
							stepPreDecryptKeySeed0x15 = 0;
						} else {
							stepPreDecryptKeySeed0x15 = 0;
						}
						break;
				}
			} else if (keySeed == 0x69 && dataSize == 16 && Model.getGeneration() >= 2) {
				if (isEqual(new TPointer(inAddr, 20), dataSize, lastPrngOutput)) {
		    		if (log.isDebugEnabled()) {
		    			log.debug(String.format("preDecrypt for IPL, keySeed=0x%X", keySeed));
		    		}
		    		stepPreDecryptKeySeed0x15 = 1;
					processed = true;
					outAddr.clear(outSize);
					result = 0;
				}
			}
		}

		return processed;
	}

	private void postDecrypt(TPointer outAddr, int outSize) {
		if (result == 0 && command == PSP_KIRK_CMD_PRNG) {
			// Remember the last PRNG output values
			outAddr.getArrayUnsigned8(lastPrngOutput);
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
            	inSize = 8;
            	outSize = 28;
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

		if (!preDecrypt(outAddr, outSize, inAddr, inSize)) {
			result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(outAddr, outSize, inAddr, inSize, command);
		}

		postDecrypt(outAddr, outSize);

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
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = signature; break;
			case 0x04: value = version; break;
			case 0x08: value = error; break;
			case 0x0C: value = 0; break; // Unknown
			case 0x10: value = command; break;
			case 0x14: value = result; break;
			case 0x1C: value = getStatus(); break;
			case 0x20: value = statusAsync; break;
			case 0x24: value = statusAsyncEnd; break;
			case 0x2C: value = sourceAddr; break;
			case 0x30: value = destAddr; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
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

	@Override
	public String toString() {
		return String.format("KIRK error=0x%X, command=0x%X, result=0x%X, status=0x%X, statusAsync=0x%X, statusAsyncEnd=0x%X, sourceAddr=0x%08X, destAddr=0x%08X", error, command, result, status, statusAsync, statusAsyncEnd, sourceAddr, destAddr);
	}
}
