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
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ENCRYPT_SIGN;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_INIT;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_PRIV_SIG_CHECK;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_PRNG;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_SHA1_HASH;
import static jpcsp.crypto.KIRK.PSP_KIRK_INVALID_OPERATION;
import static jpcsp.memory.mmio.MMIO.getKeyBFD00210;
import static jpcsp.memory.mmio.MMIO.getXorKeyBFD00210;
import static jpcsp.memory.mmio.MMIO.normalizeAddress;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.u8;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceSysreg;
import jpcsp.HLE.modules.semaphore;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.syscon.SysconEmulator;
import jpcsp.scheduler.Scheduler;
import jpcsp.settings.Settings;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;
import libkirk.KirkEngine;

public class MMIOHandlerKirk extends MMIOHandlerBase {
	private static Logger log = semaphore.log;
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS = 0xBDE00000;
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
	private static MMIOHandlerKirk instance;
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
	private int stepPreDecryptKeySeed0x15 = 0;
	private boolean initDone;

	private class CompletePhase1Action implements IAction {
		@Override
		public void execute() {
			completePhase1();
		}
	}

	public static MMIOHandlerKirk getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerKirk(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerKirk(int baseAddress) {
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
		initDone = stream.readBoolean();
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
		stream.writeBoolean(initDone);
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
		initDone = false;

		initKirk();
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

	/*
	 * descramble03g based on https://github.com/John-K/pspdecrypt/blob/master/pspdecrypt_lib.cpp
	 */
	/* xor keys & original descrambling code thanks to Davee and Proxima's awesome work! */
	private static final int xorkeys[] = {
	    0x61A0C918, 0x45695E82, 0x9CAFD36E, 0xFA499B0F,
	    0x7E84B6E2, 0x91324D29, 0xB3522009, 0xA8BC0FAF,
	    0x48C3C1C5, 0xE4C2A9DC, 0x00012ED1, 0x57D9327C,
	    0xAFB8E4EF, 0x72489A15, 0xC6208D85, 0x06021249,
	    0x41BE16DB, 0x2BD98F2F, 0xD194BEEB, 0xD1A6E669,
	    0xC0AC336B, 0x88FF3544, 0x5E018640, 0x34318761,
	    0x5974E1D2, 0x1E55581B, 0x6F28379E, 0xA90E2587,
	    0x091CB883, 0xBDC2088A, 0x7E76219C, 0x9C4BEE1B,
	    0xDD322601, 0xBB477339, 0x6678CF47, 0xF3C1209B,
	    0x5A96E435, 0x908896FA, 0x5B2D962A, 0x7FEC378C,
	    0xE3A3B3AE, 0x8B902D93, 0xD0DF32EF, 0x6484D261,
	    0x0A84A153, 0x7EB16575, 0xB10E53DD, 0x1B222753,
	    0x58DD63D0, 0x8E8B8D48, 0x755B32C2, 0xA63DFFF7,
	    0x97CABF7C, 0x33BDC660, 0x64522286, 0x403F3698,
	    0x3406C651, 0x9F4B8FB9, 0xE284F475, 0xB9189A13,
	    0x12C6F917, 0x5DE6B7ED, 0xDB674F88, 0x06DDB96E,
	    0x2B2165A6, 0x0F920D3F, 0x732B3475, 0x1908D613
	};

	private static void descramble03g(TPointer buffer, int xorKeyIndex) {
		int idx = (xorKeyIndex >> 5) & 0x3F;
		int rot = xorKeyIndex & 0x1F;
		int x1 = xorkeys[idx + 0];
		int x2 = xorkeys[idx + 1];
		int x3 = xorkeys[idx + 2];
		int x4 = xorkeys[idx + 3];
	    x1 = ((x1 >>> rot) | (x1 << (0x20-rot)));
	    x2 = Integer.reverse(((x2 >>> rot) | (x2 << (0x20-rot))));
	    x3 = (((x3 >>> rot) | (x3 << (0x20-rot))) ^ x4);
	    x4 = ((x4 >>> rot) | (x4 << (0x20-rot)));
	    buffer.setValue32( 0, buffer.getValue32( 0) ^ x1);
	    buffer.setValue32( 4, buffer.getValue32( 4) ^ x2);
	    buffer.setValue32( 8, buffer.getValue32( 8) ^ x3);
	    buffer.setValue32(12, buffer.getValue32(12) ^ x4);
	}

	private static int[] getXorKeyStep1() {
		switch (Model.getGeneration()) {
			case 2:
				return new int[] { 0x61, 0x7A, 0x56, 0x42, 0xF8, 0xED, 0xC5, 0xE4, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B };
			case 3:
				return new int[] { 0x61, 0x7A, 0x56, 0x42, 0xF8, 0xED, 0xC5, 0xE4, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25, 0x25 };
			case 4:
			case 7:
			case 9:
			case 11:
				return new int[] { 0x8D, 0x5D, 0xA6, 0x08, 0xF2, 0xBB, 0xC6, 0xCC, 0x23, 0x23, 0x23, 0x23, 0x23, 0x23, 0x23, 0x23 };
		}

		log.error(String.format("getXorKeyStep1 unimplemented for PSP generation %d", Model.getGeneration()));
		return new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	}

	private static int[] getKeyStep2() {
		switch (Model.getGeneration()) {
			case 2:
			case 3:
				return new int[] { 0xDB, 0xB1, 0x1E, 0x20, 0x48, 0x83, 0xB1, 0x6F, 0x65, 0x8C, 0x3D, 0x30, 0xE0, 0xFE, 0xCB, 0xBF };
			case 4:
			case 7:
			case 9:
			case 11:
				return new int[] { 0x34, 0xDB, 0x81, 0x24, 0x1D, 0x6F, 0x40, 0x57, 0xF3, 0xDA, 0x48, 0x08, 0xE3, 0x46, 0x67, 0x42 };
			case 5:
		}

		log.error(String.format("getKeyStep2 unimplemented for PSP generation %d", Model.getGeneration()));
		return new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	}

	private static int[] getXorKeyStep2() {
		switch (Model.getGeneration()) {
			case 2:
				return new int[] { 0x0B, 0xDD, 0xED, 0xC7, 0xB5, 0x24, 0xBC, 0x22 };
			case 3:
			case 4:
			case 7:
			case 9:
			case 11:
				return new int[] { 0x16, 0xA0, 0x7A, 0xD9, 0xEB, 0xDA, 0x3B, 0xBA };
			case 5:
		}

		log.error(String.format("getXorKeyStep2 unimplemented for PSP generation %d", Model.getGeneration()));
		return new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	}

	private static int[] getKeyStep3() {
		switch (Model.getGeneration()) {
			case 2:
			case 3:
				return new int[] { 0x04, 0xF4, 0x69, 0x8A, 0x8C, 0xAA, 0x95, 0x30, 0xCE, 0x3B, 0xE8, 0x84, 0xCA, 0x9A, 0x07, 0x9A };
			case 4:
			case 7:
			case 9:
			case 11:
				return new int[] { 0xE0, 0xDC, 0x41, 0xAF, 0xC2, 0xCD, 0x1C, 0x2D, 0x95, 0x8E, 0xA6, 0x78, 0x4D, 0x16, 0x7A, 0x85 };
			case 5:
		}

		log.error(String.format("getKeyStep3 unimplemented for PSP generation %d", Model.getGeneration()));
		return new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	}

	private static int[] getXorKeyStep3() {
		switch (Model.getGeneration()) {
			case 2:
				return new int[] { 0x80, 0x85, 0xFA, 0xD6, 0xC9, 0x24, 0xEF, 0x53 };
			case 3:
			case 4:
			case 7:
			case 9:
			case 11:
				return new int[] { 0xC0, 0xB4, 0xBD, 0x73, 0xA6, 0xCC, 0xB2, 0xD2 };
			case 5:
		}

		log.error(String.format("getXorKeyStep3 unimplemented for PSP generation %d", Model.getGeneration()));
		return new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
	}

	public boolean preDecrypt(TPointer outAddr, int outSize, TPointer inAddr, int inSize, int command) {
		boolean processed = false;

		// If the syscon emulator is used, there is no need to pre-decrypt
		if (SysconEmulator.isEnabled()) {
			return processed;
		}

		if (!initDone && command == PSP_KIRK_CMD_DECRYPT_PRIVATE && Model.getGeneration() >= 3 && inSize > 0xC0) {
			final int xorKeyIndex = 1;
			if (log.isDebugEnabled()) {
				log.debug(String.format("Calling descramble03g xorKeyIndex=0x%X", xorKeyIndex));
			}
			descramble03g(inAddr, xorKeyIndex);
		}

		if (command == PSP_KIRK_CMD_DECRYPT) {
			int keySeed = inAddr.getValue32(12);
			int dataSize = outSize;
			if (keySeed == 0x15 && dataSize == 16 && Model.getGeneration() >= 2) {
				switch (stepPreDecryptKeySeed0x15) {
					case 1:
						final int[] values = getXorKeyStep1();
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
							final int key1[] = getKeyStep2();
							final byte xorKey1_1[] = getKey(getXorKeyBFD00210());
							final int keyFromVault1[] = getKeyBFD00210();
							final int xorKey1_2[] = getXorKeyStep2();
							for (int i = 0; i < 8; i++) {
								key1[i + 8] ^= keyFromVault1[i] ^ u8(xorKey1_1[i]) ^ xorKey1_2[i];
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
							final int key2[] = getKeyStep3();
							final byte xorKey2_1[] = getKey(getXorKeyBFD00210());
							final int keyFromVault2[] = getKeyBFD00210();
							final int xorKey2_2[] = getXorKeyStep3();
							for (int i = 0; i < 8; i++) {
								key2[i + 8] ^= keyFromVault2[i + 8] ^ u8(xorKey2_1[i + 8]) ^ xorKey2_2[i];
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

	private void initKirk() {
		long fuseId = sceSysreg.dummyFuseId;
		String fuseIdString = Settings.getInstance().readString(sceSysreg.settingsFuseId, null);
		if (fuseIdString != null) {
			fuseId = Settings.parseLong(fuseIdString);
		}
		libkirk.KirkEngine.kirk_init(fuseId);
	}

	public void setInitDone() {
		initDone = true;
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
				inSize = alignUp(dataSize, 15) + 20;
				outSize = alignUp(dataSize, 15);
				break;
			case PSP_KIRK_CMD_DECRYPT_PRIVATE:
				// AES128_CMAC_Header
				dataSize = inAddr.getValue32(112);
				dataOffset = inAddr.getValue32(116);
				inSize = KirkEngine.KIRK_CMD1_HEADER.SIZEOF + alignUp(dataSize, 15) + dataOffset;
				outSize = alignUp(dataSize, 15);
				break;
			case PSP_KIRK_CMD_PRIV_SIG_CHECK:
				// AES128_CMAC_Header
				dataSize = inAddr.getValue32(112);
				dataOffset = inAddr.getValue32(116);
				inSize = KirkEngine.KIRK_CMD1_HEADER.SIZEOF + alignUp(dataSize, 15) + dataOffset;
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
            	initKirk();
            	setInitDone();
            	break;
            case PSP_KIRK_CMD_CERT_VERIFY:
            	inSize = 0xB8;
            	outSize = 0;
            	break;
            case PSP_KIRK_CMD_ENCRYPT_SIGN:
				// AES128_CMAC_Header
				dataSize = inAddr.getValue32(112);
				dataOffset = inAddr.getValue32(116);
				inSize = KirkEngine.KIRK_CMD1_HEADER.SIZEOF + alignUp(dataSize, 15) + dataOffset;
				outSize = inSize;
            	break;
            case PSP_KIRK_CMD_DECRYPT_SIGN:
				// AES128_CMAC_Header
				dataSize = inAddr.getValue32(112);
				dataOffset = inAddr.getValue32(116);
				inSize = KirkEngine.KIRK_CMD1_HEADER.SIZEOF + alignUp(dataSize, 15) + dataOffset;
				outSize = alignUp(dataSize, 15);
            	break;
			default:
				log.error(String.format("MMIOHandlerKirk.hleUtilsBufferCopyWithRange unimplemented KIRK command 0x%X", command));
				result = PSP_KIRK_INVALID_OPERATION;
				return 0;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("hleUtilsBufferCopyWithRange input: %s", Utilities.getMemoryDump(inAddr, inSize)));
		}

		if (!preDecrypt(outAddr, outSize, inAddr, inSize, command)) {
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
			case 0:
				// No effect
				break;
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
				log.error(String.format("0x%08X - KIRK unknown startProcessing value 0x%X on %s", getPc(), value, this));
				break;
		}
	}

	private void endProcessing(int value) {
		switch (value) {
			case 0:
				// No effect
				break;
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
				log.error(String.format("0x%08X - KIRK unknown endProcessing value 0x%X on %s", getPc(), value, this));
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
