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
package jpcsp.memory.mmio.syscon;

import static jpcsp.HLE.Modules.sceSysconModule;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_BASE;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_CHALLENGE1;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_CHALLENGE2;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_CYCLE;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_ELEC;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_FULL_CAP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_IFC;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_INFO;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_LIMIT_TIME;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_MANUFACTURER;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_SERIAL;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_TEMP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_VOLT;
import static jpcsp.HLE.modules.sceSyscon.getSysconCmdName;
import static jpcsp.util.Utilities.getByte0;
import static jpcsp.util.Utilities.getByte1;
import static jpcsp.util.Utilities.getByte2;
import static jpcsp.util.Utilities.getByte3;
import static libkirk.AES.AES_cbc_encrypt;
import static libkirk.AES.AES_set_key;

import java.io.IOException;

import jpcsp.hardware.Battery;
import jpcsp.nec78k0.sfr.Nec78k0SerialInterface;
import jpcsp.nec78k0.sfr.Nec78k0SerialInterfaceUART6;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import libkirk.AES.AES_ctx;

/**
 * The syscon firmware is communicating with the PSP battery using
 * a serial interface.
 * Simulate the behavior of the PSP battery in this communication.
 * 
 * See the Baryon Sweeper for a simulation of battery responses:
 *     https://github.com/khubik2/pysweeper/blob/master/pysweeper.py
 * 
 * @author gid15
 *
 */
public class SysconBatteryEmulator extends Nec78k0SerialInterface {
	private static final int STATE_VERSION = 0;
	private static final int key0[] = { 92, 82, 217, 28, 243, 130, 172, 164, 137, 216, 129, 120, 236, 22, 41, 123 };
	private static final int key1[] = { 157, 79, 80, 252, 225, 182, 142, 18, 9, 48, 125, 219, 166, 165, 181, 170 };
	private static final int key2[] = { 9, 117, 152, 136, 100, 172, 247, 98, 27, 192, 144, 157, 240, 252, 171, 255 };
	private static final int key3[] = { 201, 17, 92, 226, 6, 74, 38, 134, 216, 214, 217, 208, 140, 222, 48, 89 };
	private static final int key4[] = { 102, 117, 57, 210, 251, 66, 115, 178, 144, 63, 215, 163, 158, 210, 198, 12 };
	private static final int key5[] = { 244, 250, 239, 32, 244, 219, 171, 49, 209, 134, 116, 253, 143, 153, 5, 102 };
	private static final int key6[] = { 234, 12, 129, 19, 99, 215, 233, 48, 249, 97, 19, 90, 79, 53, 45, 220 };
	private static final int key8[] = { 10, 46, 115, 48, 92, 56, 45, 79, 49, 13, 10, 237, 132, 164, 24, 0 };
	private static final int key10[] = { 172, 0, 192, 227, 232, 10, 240, 104, 63, 221, 23, 69, 25, 69, 67, 189 };
	private static final int key13[] = { 223, 243, 252, 214, 8, 176, 85, 151, 207, 9, 162, 59, 209, 125, 63, 210 };
	private static final int key47[] = new int[16];
	private static boolean key47created;
	private static final int key151[] = new int[16];
	private static boolean key151created;
	private static final int key179[] = { 3, 190, 182, 84, 153, 20, 4, 131, 186, 24, 122, 100, 239, 144, 38, 29 };
	private static final int key217[] = { 199, 172, 19, 6, 222, 254, 57, 236, 131, 161, 72, 59, 14, 226, 236, 137 };
	private static final int key235[] = { 65, 132, 153, 190, 157, 53, 163, 185, 252, 106, 208, 214, 240, 65, 187, 38 };
	private static final int challenge1secret0[] = { 210, 7, 34, 83, 164, 242, 116, 104 };
	private static final int challenge1secret1[] = { 245, 215, 212, 181, 117, 240, 142, 78 };
	private static final int challenge1secret2[] = { 179, 122, 22, 239, 85, 123, 208, 137 };
	private static final int challenge1secret3[] = { 204, 105, 149, 129, 253, 137, 18, 108 };
	private static final int challenge1secret4[] = { 160, 78, 50, 187, 167, 19, 158, 70 };
	private static final int challenge1secret5[] = { 73, 94, 3, 71, 148, 147, 29, 123 };
	private static final int challenge1secret6[] = { 176, 184, 9, 131, 57, 137, 250, 226 };
	public  static final int challenge1secret8[] = { 173, 64, 67, 178, 86, 235, 69, 139 };
	public  static final int challenge1secret10[] = { 194, 55, 126, 138, 116, 9, 108, 95 };
	public  static final int challenge1secret13[] = { 88, 28, 127, 25, 68, 249, 98, 98 };
	private static final int challenge1secret47[] = new int[8];
	private static final int challenge1secret151[] = new int[8];
	private static final int challenge1secret179[] = { 219, 211, 174, 164, 219, 4, 100, 16 };
	private static final int challenge1secret217[] = { 144, 225, 240, 192, 1, 120, 227, 255 };
	private static final int challenge1secret235[] = { 11, 217, 2, 126, 133, 31, 161, 35 };
	private static final int challenge2secret0[] = { 244, 224, 67, 19, 173, 46, 180, 219 };
	private static final int challenge2secret1[] = { 254, 125, 120, 153, 191, 236, 71, 197 };
	private static final int challenge2secret2[] = { 134, 94, 62, 239, 157, 251, 177, 253 };
	private static final int challenge2secret3[] = { 48, 111, 58, 3, 216, 108, 190, 228 };
	private static final int challenge2secret4[] = { 255, 114, 189, 43, 131, 184, 157, 47 };
	private static final int challenge2secret5[] = { 132, 34, 223, 234, 226, 27, 99, 194 };
	private static final int challenge2secret6[] = { 88, 185, 90, 174, 243, 153, 219, 208 };
	public  static final int challenge2secret8[] = { 103, 192, 114, 21, 217, 107, 57, 161 };
	public  static final int challenge2secret10[] = { 9, 62, 197, 25, 175, 15, 80, 45 };
	public  static final int challenge2secret13[] = { 49, 128, 83, 135, 92, 32, 62, 36 };
	private static final int challenge2secret47[] = new int[8];
	private static final int challenge2secret151[] = new int[8];
	private static final int challenge2secret179[] = { 227, 43, 143, 86, 178, 100, 18, 152 };
	private static final int challenge2secret217[] = { 195, 74, 106, 123, 32, 95, 232, 249 };
	private static final int challenge2secret235[] = { 247, 145, 237, 11, 63, 73, 164, 72 };
	private int keyId;
	private final int[] challenge1 = new int[8];

	public SysconBatteryEmulator(MMIOHandlerSysconFirmwareSfr sfr, Nec78k0SerialInterfaceUART6 serialInterface) {
		super(sfr, serialInterface);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		keyId = stream.readInt();
		stream.readInts(challenge1);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(keyId);
		stream.writeInts(challenge1);
		super.write(stream);
	}


	private static byte[] intsToBytes(int[] a, int offset, int length) {
		byte[] b = new byte[length];
		for (int i = 0; i < length; i++) {
			b[i] = (byte) a[offset + i];
		}

		return b;
	}

	private static byte[] intsToBytes(int[] a) {
		return intsToBytes(a, 0, a.length);
	}

	private static int[] bytesToInts(byte[] a, int[] b, int offset) {
		for (int i = 0; i < a.length; i++) {
			b[offset + i] = a[i] & 0xFF;
		}

		return b;
	}

	private static int[] bytesToInts(byte[] a) {
		return bytesToInts(a, new int[a.length], 0);
	}

	public static String intsToString(int[] a, int start, int length) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(String.format("0x%02X", a[start + i]));
		}

		return s.toString();
	}

	public static String intsToString(int[] a) {
		return intsToString(a, 0, a.length);
	}

	private void createDynamicKeys() {
		if (!key47created) {
			createDynamicKey(new int[] { 0xC9, 0xF8, 0x09, 0x45, 0x2A, 0xA2, 0xBE, 0xD5, 0x40, 0xD0, 0x2B, 0xAC, 0xFB, 0x1A, 0x8B, 0xEA }, key47, challenge1secret47, challenge2secret47);
			key47created = true;
			if (log.isDebugEnabled()) {
				log.debug(String.format("createDynamicKeys: key47=%s, challenge1secret47=%s, challenge2secret47=%s", intsToString(key47), intsToString(challenge1secret47), intsToString(challenge2secret47)));
			}
		}

		if (!key151created) {
			createDynamicKey(new int[] { 0x66, 0x1A, 0x4D, 0x7F, 0x6E, 0xCD, 0x33, 0xC5, 0x2B, 0xF5, 0xF2, 0x95, 0x86, 0xA7, 0x64, 0x48 }, key151, challenge1secret151, challenge2secret151);
			key151created = true;
			if (log.isDebugEnabled()) {
				log.debug(String.format("createDynamicKeys: key151=%s, challenge1secret151=%s, challenge2secret151=%s", intsToString(key151), intsToString(challenge1secret151), intsToString(challenge2secret151)));
			}
		}
	}

	private void read32SecureFlash(int address, int[] array, int offset) {
		int data32 = sfr.getSecureFlash().read32(address);
		array[offset + 0] = getByte0(data32);
		array[offset + 1] = getByte1(data32);
		array[offset + 2] = getByte2(data32);
		array[offset + 3] = getByte3(data32);
	}

	private void createDynamicKey(int[] secret, int[] key, int[] challenge1secret, int[] challenge2secret) {
		final int[] keyF600 = new int[16];
		for (int i = 0; i < 8; i += 4) {
			read32SecureFlash(0x0790 + i, keyF600, i);
		}
		System.arraycopy(new int[] { 0x0B, 0x23, 0x85, 0x01, 0x0F, 0xB2, 0x79, 0xBD }, 0, keyF600, 8, 8);

		final int[] keyF610 = new int[16];
		System.arraycopy(new int[] { 0xE1, 0xC3, 0xEC, 0xA9, 0x19, 0x59, 0x04, 0x0D }, 0, keyF610, 0, 8);
		for (int i = 0; i < 8; i += 4) {
			read32SecureFlash(0x0798 + i, keyF610, i + 8);
		}

		final int[] keyF5E0 = new int[32];
		for (int i = 0; i < 16; i += 4) {
			read32SecureFlash(0x07C0 + i, keyF5E0, i);
		}

		System.arraycopy(secret, 0, keyF5E0, 16, 16);

		finalKeyEncryptionCBC(keyF610, keyF600, keyF5E0);

		System.arraycopy(keyF5E0, 0, key, 0, 16);
		System.arraycopy(keyF5E0, 16, challenge1secret, 0, 8);
		System.arraycopy(keyF5E0, 24, challenge2secret, 0, 8);
	}

	private void finalKeyEncryptionCBC(int[] key1, int[] key2, int[] key3) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("finalKeyEncryptionCBC key1=%s, key2=%s, key3=%s", intsToString(key1), intsToString(key2), intsToString(key3)));
		}

		AES_ctx ctx = new AES_ctx();

		AES_set_key(ctx, intsToBytes(key1), key1.length * 8);

		byte[] resultBytes = new byte[16];

		AES_cbc_encrypt(ctx, intsToBytes(key3, 0, 16), resultBytes, 16);
		bytesToInts(resultBytes, key3, 0);
		if (log.isDebugEnabled()) {
			log.debug(String.format("finalKeyEncryptionCBC after first encrypt: key3=%s", intsToString(key3, 0, 16)));
		}

		final int[] xor = new int[16];
		System.arraycopy(key3, 0, xor, 0, 16);

		for (int i = 0; i < 16; i++) {
			key3[i + 16] ^= xor[i];
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("finalKeyEncryptionCBC after first xor: key3=%s", intsToString(key3, 16, 16)));
		}

		AES_cbc_encrypt(ctx, intsToBytes(key3, 16, 16), resultBytes, 16);
		bytesToInts(resultBytes, key3, 16);
		if (log.isDebugEnabled()) {
			log.debug(String.format("finalKeyEncryptionCBC after second encrypt: key3=%s", intsToString(key3, 16, 16)));
		}

		AES_set_key(ctx, intsToBytes(key2), key2.length * 8);
		AES_cbc_encrypt(ctx, intsToBytes(key3, 0, 16), resultBytes, 16);
		bytesToInts(resultBytes, key3, 0);
		if (log.isDebugEnabled()) {
			log.debug(String.format("finalKeyEncryptionCBC after third encrypt: key3=%s", intsToString(key3, 0, 16)));
		}

		AES_cbc_encrypt(ctx, intsToBytes(key3, 16, 16), resultBytes, 16);
		bytesToInts(resultBytes, key3, 16);
		if (log.isDebugEnabled()) {
			log.debug(String.format("finalKeyEncryptionCBC after fourth encrypt: key3=%s", intsToString(key3, 16, 16)));
		}

		for (int i = 0; i < 16; i++) {
			key3[i + 16] ^= xor[i];
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("finalKeyEncryptionCBC after second xor: key3=%s", intsToString(key3, 16, 16)));
		}
	}

	public int[] getKey(int id) {
		switch (id) {
			case 0: return key0;
			case 1: return key1;
			case 2: return key2;
			case 3: return key3;
			case 4: return key4;
			case 5: return key5;
			case 6: return key6;
			case 8: return key8;
			case 10: return key10;
			case 13: return key13;
			case 47: createDynamicKeys(); return key47;
			case 151: createDynamicKeys(); return key151;
			case 179: return key179;
			case 217: return key217;
			case 235: return key235;
		}

		log.error(String.format("Unknown key 0x%02X", id));

		return null;
	}

	public int[] getChallenge1Secret(int id) {
		switch (id) {
			case 0: return challenge1secret0;
			case 1: return challenge1secret1;
			case 2: return challenge1secret2;
			case 3: return challenge1secret3;
			case 4: return challenge1secret4;
			case 5: return challenge1secret5;
			case 6: return challenge1secret6;
			case 8: return challenge1secret8;
			case 10: return challenge1secret10;
			case 13: return challenge1secret13;
			case 47: createDynamicKeys(); return challenge1secret47;
			case 151: createDynamicKeys(); return challenge1secret151;
			case 179: return challenge1secret179;
			case 217: return challenge1secret217;
			case 235: return challenge1secret235;
		}

		log.error(String.format("Unknown challenge1 secret 0x%02X", id));

		return null;
	}

	public int[] getChallenge2Secret(int id) {
		switch (id) {
			case 0: return challenge2secret0;
			case 1: return challenge2secret1;
			case 2: return challenge2secret2;
			case 3: return challenge2secret3;
			case 4: return challenge2secret4;
			case 5: return challenge2secret5;
			case 6: return challenge2secret6;
			case 8: return challenge2secret8;
			case 10: return challenge2secret10;
			case 13: return challenge2secret13;
			case 47: createDynamicKeys(); return challenge2secret47;
			case 151: createDynamicKeys(); return challenge2secret151;
			case 179: return challenge2secret179;
			case 217: return challenge2secret217;
			case 235: return challenge2secret235;
		}

		log.error(String.format("Unknown challenge2 secret 0x%02X", id));

		return null;
	}

	private int[] mixChallenge1(int keyId, int[] challenge) {
		final int[] data = new int[16];

		final int[] secret1 = getChallenge1Secret(keyId);
		data[ 0] = secret1[0];
		data[ 4] = secret1[1];
		data[ 8] = secret1[2];
		data[12] = secret1[3];
		data[ 1] = secret1[4];
		data[ 5] = secret1[5];
		data[ 9] = secret1[6];
		data[13] = secret1[7];
		data[ 2] = challenge[0];
		data[ 6] = challenge[1];
		data[10] = challenge[2];
		data[14] = challenge[3];
		data[ 3] = challenge[4];
		data[ 7] = challenge[5];
		data[11] = challenge[6];
		data[15] = challenge[7];

		return data;
	}

	private int[] mixChallenge2(int keyId, int[] challenge) {
		final int[] data = new int[16];

		final int[] secret2 = getChallenge2Secret(keyId);
		data[ 0] = challenge[0];
		data[ 4] = challenge[1];
		data[ 8] = challenge[2];
		data[12] = challenge[3];
		data[ 1] = challenge[4];
		data[ 5] = challenge[5];
		data[ 9] = challenge[6];
		data[13] = challenge[7];
		data[ 2] = secret2[0];
		data[ 6] = secret2[1];
		data[10] = secret2[2];
		data[14] = secret2[3];
		data[ 3] = secret2[4];
		data[ 7] = secret2[5];
		data[11] = secret2[6];
		data[15] = secret2[7];

		return data;
	}

	private static int[] encryptAES(int[] key, int[] data) {
		AES_ctx ctx = new AES_ctx();

		AES_set_key(ctx, intsToBytes(key), key.length * 8);

		byte[] resultBytes = new byte[data.length];

		AES_cbc_encrypt(ctx, intsToBytes(data), resultBytes, data.length);

		return bytesToInts(resultBytes);
	}

	private static int[] matrixSwap(int[] a) {
		if (a == null) {
			return null;
		}

		int[] b = new int[a.length];
		b[ 0] = a[ 0];
		b[ 1] = a[ 4];
		b[ 2] = a[ 8];
		b[ 3] = a[12];
		b[ 4] = a[ 1];
		b[ 5] = a[ 5];
		b[ 6] = a[ 9];
		b[ 7] = a[13];
		b[ 8] = a[ 2];
		b[ 9] = a[ 6];
		b[10] = a[10];
		b[11] = a[14];
		b[12] = a[ 3];
		b[13] = a[ 7];
		b[14] = a[11];
		b[15] = a[15];

		return b;
	}

	public void executeSysconCmdBattery(int batteryCommand, int sysconCmdBattery, int length, int transmissionLength) {
		switch (sysconCmdBattery) {
			case PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(3);
				addReceptionBufferData8(0x00);
				addReceptionBufferData16(0x1234);
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_VOLT:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(2);
				addReceptionBufferData16(Battery.getVoltage());
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_SERIAL:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(4);
				addReceptionBufferData32(Battery.readEepromBatterySerialNumber());
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_INFO:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(5);
				addReceptionBufferData16(0x0000); // Must be 0?
				addReceptionBufferData8(0x11); // Unknown
				addReceptionBufferData8(0x0B); // Must be > 0x0A?
				addReceptionBufferData8(0x22); // Unknown
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_IFC:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(2);
				addReceptionBufferData16(0x04E2); // Must be >= 0x04E2?
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_FULL_CAP:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(2);
				addReceptionBufferData16(Battery.getFullCapacity());
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_CYCLE:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(2);
				addReceptionBufferData16(sceSysconModule.getBatteryCycle());
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_LIMIT_TIME:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(2);
				addReceptionBufferData16(sceSysconModule.getBatteryLimitTime());
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_TEMP:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(1);
				addReceptionBufferData8(Battery.getTemperature());
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_ELEC:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				startReceptionBuffer(2);
				addReceptionBufferData16(sceSysconModule.getBatteryElec());
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_GET_MANUFACTURER:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery received syscon command %s", getSysconCmdName(sysconCmdBattery)));
				}
				final String s = "SonyEnergyDevices";
				startReceptionBuffer(s.length());
				for (int i = 0; i < s.length(); i++) {
					addReceptionBufferData8(s.charAt(i) & 0xFF);
				}
				endReceptionBuffer();
				break;
			case PSP_SYSCON_CMD_BATTERY_CHALLENGE1:
				if (length == 11) {
					keyId = transmissionBuffer.peek(3);
					int[] requestData = new int[8];
					transmissionBuffer.peek(4, requestData, 0, requestData.length);

					if (log.isDebugEnabled()) {
						log.debug(String.format("UART6 Battery received syscon command %s, keyId=0x%02X, challenge=%s", getSysconCmdName(sysconCmdBattery), keyId, intsToString(requestData)));
					}
					int[] key = getKey(keyId);
					if (key != null) {
						int[] data = mixChallenge1(keyId, requestData);
						int[] challenge1a = encryptAES(key, matrixSwap(data));
						int[] challenge1b = matrixSwap(encryptAES(key, challenge1a));
						System.arraycopy(challenge1b, 0, challenge1, 0, challenge1.length);

						startReceptionBuffer(16);
						addReceptionBufferData8(challenge1a, 0, 8);
						addReceptionBufferData8(challenge1b, 0, 8);
						endReceptionBuffer();
					} else {
						// Unknown key, return error code 0x15
						startReceptionBuffer(16, 0x15);
						addReceptionBufferData8(new int[16], 0, 16);
						endReceptionBuffer();
					}
				} else {
					log.error(String.format("UART6 Battery invalid length=%d for received syscon command %s", length, getSysconCmdName(sysconCmdBattery), transmissionBuffer.toString(8)));
				}
				break;
			case PSP_SYSCON_CMD_BATTERY_CHALLENGE2:
				if (length == 10) {
					if (log.isDebugEnabled()) {
						int[] unusedData = new int[8];
						transmissionBuffer.peek(3, unusedData, 0, unusedData.length);
						log.debug(String.format("UART6 Battery received syscon command %s, unused data=%s", getSysconCmdName(sysconCmdBattery), intsToString(unusedData)));
					}
					int[] key = getKey(keyId);
					if (key != null) {
						int[] data2 = mixChallenge2(keyId, challenge1);
						int[] challenge2 = encryptAES(key, matrixSwap(data2));
						int[] response2 = encryptAES(key, challenge2);

						startReceptionBuffer(8);
						addReceptionBufferData8(response2, 0, 8);
						endReceptionBuffer();
					} else {
						startReceptionBuffer(8);
						addReceptionBufferData8(new int[8], 0, 8);
						endReceptionBuffer();
					}
				} else {
					log.error(String.format("UART6 Battery invalid length=%d for received syscon command %s", length, getSysconCmdName(sysconCmdBattery), transmissionBuffer.toString(8)));
				}
				break;
			default:
				log.error(String.format("UART6 Battery setOperationMode starting reception for unknown battery command 0x%02X(%s): %s", batteryCommand, getSysconCmdName(sysconCmdBattery), transmissionBuffer.toString(8)));
				break;
		}
	}

	@Override
	public void startReception() {
		// Starting reception
		int transmissionLength = transmissionBuffer.size();

		if (transmissionLength >= 2) {
			int command = transmissionBuffer.peek(0);
			int length = transmissionBuffer.peek(1);
			if (command == 0x5A) {
				if (length + 2 == transmissionLength) {
					int checksum = transmissionBuffer.peek(length + 1);
					if (isValidChecksum(transmissionBuffer, checksum, length + 1)) {
						int batteryCommand = transmissionBuffer.peek(2);
						int sysconCmdBattery = batteryCommand + PSP_SYSCON_CMD_BATTERY_BASE;
						executeSysconCmdBattery(batteryCommand, sysconCmdBattery, length, transmissionLength);
					} else {
						log.error(String.format("UART6 Battery setOperationMode invalid checksum 0x%02X: %s", checksum, transmissionBuffer.toString(8)));
					}
				} else {
					log.error(String.format("UART6 Battery setOperationMode starting reception for unknown command length 0x%02X: %s", length, transmissionBuffer.toString(8)));
				}
			} else {
				log.error(String.format("UART6 Battery setOperationMode starting reception for unknown command 0x%02X: %s", command, transmissionBuffer.toString(8)));
			}
		} else {
			log.error(String.format("UART6 Battery setOperationMode starting reception for unknown command buffer: %s", transmissionBuffer.toString(8)));
		}

		transmissionBuffer.clear();
	}
}
