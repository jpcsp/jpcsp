/*
  Draan proudly presents:
  
  With huge help from community:
  coyotebean, Davee, hitchhikr, kgsws, liquidzigong, Mathieulh, Proxima, SilverSpring
  
  ******************** KIRK-ENGINE ********************
  An Open-Source implementation of KIRK (PSP crypto engine) algorithms and keys.
  Includes also additional routines for hash forging.
  
  ********************
  
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
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
package libkirk;

import static jpcsp.util.Utilities.endianSwap64;
import static jpcsp.util.Utilities.getMemoryDump;
import static libkirk.AES.AES_CMAC;
import static libkirk.AES.AES_cbc_decrypt;
import static libkirk.AES.AES_cbc_encrypt;
import static libkirk.AES.AES_set_key;
import static libkirk.AES.rijndael_decrypt;
import static libkirk.AES.rijndael_encrypt;
import static libkirk.AES.rijndael_set_key;
import static libkirk.EC.BIGNUMBER_SIZE;
import static libkirk.EC.ELT_SIZE;
import static libkirk.EC.ec_priv_to_pub;
import static libkirk.EC.ec_pub_mult;
import static libkirk.EC.ecdsa_set_curve;
import static libkirk.EC.ecdsa_set_priv;
import static libkirk.EC.ecdsa_set_pub;
import static libkirk.EC.ecdsa_sign;
import static libkirk.EC.ecdsa_verify;
import static libkirk.SHA1.SHAFinal;
import static libkirk.SHA1.SHAInit;
import static libkirk.SHA1.SHAUpdate;
import static libkirk.Utilities.alignUp;
import static libkirk.Utilities.curtime;
import static libkirk.Utilities.log;
import static libkirk.Utilities.memcmp;
import static libkirk.Utilities.memcpy;
import static libkirk.Utilities.memset;
import static libkirk.Utilities.read32;
import static libkirk.Utilities.read64;
import static libkirk.Utilities.read8;
import static libkirk.Utilities.write32;
import static libkirk.Utilities.write64;
import static libkirk.Utilities.write8;

import jpcsp.crypto.KeyVault;

/**
 * Ported to Java from
 * https://github.com/ProximaV/kirk-engine-full/blob/master/libkirk/kirk_engine.c
 * https://github.com/ProximaV/kirk-engine-full/blob/master/libkirk/kirk_engine.h
 */
public class KirkEngine {
	//Kirk return values
	public static final int KIRK_OPERATION_SUCCESS = 0;
	public static final int KIRK_NOT_ENABLED = 1;
	public static final int KIRK_INVALID_MODE = 2;
	public static final int KIRK_HEADER_HASH_INVALID = 3;
	public static final int KIRK_DATA_HASH_INVALID = 4;
	public static final int KIRK_SIG_CHECK_INVALID = 5;
	public static final int KIRK_UNK_1 = 6;
	public static final int KIRK_UNK_2 = 7;
	public static final int KIRK_UNK_3 = 8;
	public static final int KIRK_UNK_4 = 9;
	public static final int KIRK_UNK_5 = 0xA;
	public static final int KIRK_UNK_6 = 0xB;
	public static final int KIRK_NOT_INITIALIZED = 0xC;
	public static final int KIRK_INVALID_OPERATION = 0xD;
	public static final int KIRK_INVALID_SEED_CODE = 0xE;
	public static final int KIRK_INVALID_SIZE = 0xF;
	public static final int KIRK_DATA_SIZE_ZERO = 0x10;

	//mode passed to sceUtilsBufferCopyWithRange
	public static final int KIRK_CMD_ENCRYPT_PRIVATE      = 0;
	public static final int KIRK_CMD_DECRYPT_PRIVATE      = 1;
	public static final int KIRK_CMD_ENCRYPT_SIGN         = 2;
	public static final int KIRK_CMD_DECRYPT_SIGN         = 3;
	public static final int KIRK_CMD_ENCRYPT_IV_0         = 4;
	public static final int KIRK_CMD_ENCRYPT_IV_FUSE      = 5;
	public static final int KIRK_CMD_ENCRYPT_IV_USER      = 6;
	public static final int KIRK_CMD_DECRYPT_IV_0         = 7;
	public static final int KIRK_CMD_DECRYPT_IV_FUSE      = 8;
	public static final int KIRK_CMD_DECRYPT_IV_USER      = 9;
	public static final int KIRK_CMD_PRIV_SIGN_CHECK      = 10;
	public static final int KIRK_CMD_SHA1_HASH            = 11;
	public static final int KIRK_CMD_ECDSA_GEN_KEYS       = 12;
	public static final int KIRK_CMD_ECDSA_MULTIPLY_POINT = 13;
	public static final int KIRK_CMD_PRNG                 = 14;
	public static final int KIRK_CMD_INIT                 = 15;
	public static final int KIRK_CMD_ECDSA_SIGN           = 16;
	public static final int KIRK_CMD_ECDSA_VERIFY         = 17;
    public static final int KIRK_CMD_CERT_VERIFY          = 18;

	//"mode" in header
	public static final int KIRK_MODE_CMD1 = 1;
	public static final int KIRK_MODE_CMD2 = 2;
	public static final int KIRK_MODE_CMD3 = 3;
	public static final int KIRK_MODE_ENCRYPT_CBC = 4;
	public static final int KIRK_MODE_DECRYPT_CBC = 5;

	public static final byte[] kirk1_key = {(byte) 0x98, (byte) 0xC9, (byte) 0x40, (byte) 0x97, (byte) 0x5C, (byte) 0x1D, (byte) 0x10, (byte) 0xE8, (byte) 0x7F, (byte) 0xE6, (byte) 0x0E, (byte) 0xA3, (byte) 0xFD, (byte) 0x03, (byte) 0xA8, (byte) 0xBA};

	public static final byte[] kirk16_key  = {(byte) 0x47, (byte) 0x5E, (byte) 0x09, (byte) 0xF4, (byte) 0xA2, (byte) 0x37, (byte) 0xDA, (byte) 0x9B, (byte) 0xEF, (byte) 0xFF, (byte) 0x3B, (byte) 0xC0, (byte) 0x77, (byte) 0x14, (byte) 0x3D, (byte) 0x8A};

	/* ECC Curves for Kirk 1 and Kirk 0x11 */
	// Common Curve paramters p and a
	public static final byte[] ec_p = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
	public static final byte[] ec_a = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC}; // mon

	// Kirk 0xC,0xD,0x10,0x11,(likely 0x12)- Unique curve parameters for b, N, and base point G for Kirk 0xC,0xD,0x10,0x11,(likely 0x12) service
	// Since public key is variable, it is not specified here
	public static final byte[] ec_b2 = {(byte) 0xA6, (byte) 0x8B, (byte) 0xED, (byte) 0xC3, (byte) 0x34, (byte) 0x18, (byte) 0x02, (byte) 0x9C, (byte) 0x1D, (byte) 0x3C, (byte) 0xE3, (byte) 0x3B, (byte) 0x9A, (byte) 0x32, (byte) 0x1F, (byte) 0xCC, (byte) 0xBB, (byte) 0x9E, (byte) 0x0F, (byte) 0x0B};// mon
	public static final byte[] ec_N2 = {(byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xB5, (byte) 0xAE, (byte) 0x3C, (byte) 0x52, (byte) 0x3E, (byte) 0x63, (byte) 0x94, (byte) 0x4F, (byte) 0x21, (byte) 0x27};
	public static final byte[] Gx2 = {(byte) 0x12, (byte) 0x8E, (byte) 0xC4, (byte) 0x25, (byte) 0x64, (byte) 0x87, (byte) 0xFD, (byte) 0x8F, (byte) 0xDF, (byte) 0x64, (byte) 0xE2, (byte) 0x43, (byte) 0x7B, (byte) 0xC0, (byte) 0xA1, (byte) 0xF6, (byte) 0xD5, (byte) 0xAF, (byte) 0xDE, (byte) 0x2C};
	public static final byte[] Gy2 = {(byte) 0x59, (byte) 0x58, (byte) 0x55, (byte) 0x7E, (byte) 0xB1, (byte) 0xDB, (byte) 0x00, (byte) 0x12, (byte) 0x60, (byte) 0x42, (byte) 0x55, (byte) 0x24, (byte) 0xDB, (byte) 0xC3, (byte) 0x79, (byte) 0xD5, (byte) 0xAC, (byte) 0x5F, (byte) 0x4A, (byte) 0xDF};

	// KIRK 1 - Unique curve parameters for b, N, and base point G
	// Since public key is hard coded, it is also included
	public static final byte[] ec_b1 = {(byte) 0x65, (byte) 0xD1, (byte) 0x48, (byte) 0x8C, (byte) 0x03, (byte) 0x59, (byte) 0xE2, (byte) 0x34, (byte) 0xAD, (byte) 0xC9, (byte) 0x5B, (byte) 0xD3, (byte) 0x90, (byte) 0x80, (byte) 0x14, (byte) 0xBD, (byte) 0x91, (byte) 0xA5, (byte) 0x25, (byte) 0xF9};
	public static final byte[] ec_N1 = {(byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x01, (byte) 0xB5, (byte) 0xC6, (byte) 0x17, (byte) 0xF2, (byte) 0x90, (byte) 0xEA, (byte) 0xE1, (byte) 0xDB, (byte) 0xAD, (byte) 0x8F};
	public static final byte[] Gx1 = {(byte) 0x22, (byte) 0x59, (byte) 0xAC, (byte) 0xEE, (byte) 0x15, (byte) 0x48, (byte) 0x9C, (byte) 0xB0, (byte) 0x96, (byte) 0xA8, (byte) 0x82, (byte) 0xF0, (byte) 0xAE, (byte) 0x1C, (byte) 0xF9, (byte) 0xFD, (byte) 0x8E, (byte) 0xE5, (byte) 0xF8, (byte) 0xFA};
	public static final byte[] Gy1 = {(byte) 0x60, (byte) 0x43, (byte) 0x58, (byte) 0x45, (byte) 0x6D, (byte) 0x0A, (byte) 0x1C, (byte) 0xB2, (byte) 0x90, (byte) 0x8D, (byte) 0xE9, (byte) 0x0F, (byte) 0x27, (byte) 0xD7, (byte) 0x5C, (byte) 0x82, (byte) 0xBE, (byte) 0xC1, (byte) 0x08, (byte) 0xC0};

	public static final byte[] Px1 = {(byte) 0xED, (byte) 0x9C, (byte) 0xE5, (byte) 0x82, (byte) 0x34, (byte) 0xE6, (byte) 0x1A, (byte) 0x53, (byte) 0xC6, (byte) 0x85, (byte) 0xD6, (byte) 0x4D, (byte) 0x51, (byte) 0xD0, (byte) 0x23, (byte) 0x6B, (byte) 0xC3, (byte) 0xB5, (byte) 0xD4, (byte) 0xB9};
	public static final byte[] Py1 = {(byte) 0x04, (byte) 0x9D, (byte) 0xF1, (byte) 0xA0, (byte) 0x75, (byte) 0xC0, (byte) 0xE0, (byte) 0x4F, (byte) 0xB3, (byte) 0x44, (byte) 0x85, (byte) 0x8B, (byte) 0x61, (byte) 0xB7, (byte) 0x9B, (byte) 0x69, (byte) 0xA6, (byte) 0x3D, (byte) 0x2C, (byte) 0x39};

	/* ------------------------- INTERNAL STUFF ------------------------- */
	private static int g_fuse90;  // This is to match FuseID HW at BC100090 and BC100094
	private static int g_fuse94;
	private static final AES.AES_ctx aes_kirk1 = new AES.AES_ctx(); //global
	private static final byte[] PRNG_DATA = new byte[0x14];
	private static boolean is_kirk_initialized; //"init" emulation
	private static final boolean noRandomData = false;
	/* ------------------------- INTERNAL STUFF END ------------------------- */

	public static class ECDSA_SIG {
		public static final int SIZEOF = ELT_SIZE + ELT_SIZE;
		public final byte[] r = new byte[ELT_SIZE];
		public final byte[] s = new byte[ELT_SIZE];

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, r);
			offset = Utilities.read(buffer, offset, s);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, r);
			offset = Utilities.write(buffer, offset, s);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("r=%s, s =%s", Utilities.toString(r), Utilities.toString(s));
		}
	}

	public static class ECDSA_POINT {
		public static final int SIZEOF = ELT_SIZE + ELT_SIZE;
		public final byte[] x = new byte[ELT_SIZE];
		public final byte[] y = new byte[ELT_SIZE];

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, x);
			offset = Utilities.read(buffer, offset, y);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, x);
			offset = Utilities.write(buffer, offset, y);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("x=%s, y=%s", Utilities.toString(x), Utilities.toString(y));
		}
	}

	public static class KIRK_CMD1_HEADER {
		public static final int SIZEOF = 0x90;
		public final byte[] AES_key = new byte[16];          // 0x0
		public final byte[] CMAC_key = new byte[16];         // 0x10
		public final byte[] CMAC_header_hash = new byte[16]; // 0x20
		public final byte[] CMAC_data_hash = new byte[16];   // 0x30
		public final byte[] unused = new byte[32];           // 0x40
		public int mode;                                     // 0x60
		public int ecdsa_hash;                               // 0x64
		public final byte[] unk3 = new byte[11];             // 0x65
		public int data_size;                                // 0x70
		public int data_offset;                              // 0x74
		public final byte[] unk4 = new byte[8];              // 0x78
		public final byte[] unk5 = new byte[16];             // 0x80

		public KIRK_CMD1_HEADER() {
		}

		public KIRK_CMD1_HEADER(byte[] buffer, int offset) {
			read(buffer, offset);
		}

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, AES_key);
			offset = Utilities.read(buffer, offset, CMAC_key);
			offset = Utilities.read(buffer, offset, CMAC_header_hash);
			offset = Utilities.read(buffer, offset, CMAC_data_hash);
			offset = Utilities.read(buffer, offset, unused);
			mode = read32(buffer, offset);
			offset += 4;
			ecdsa_hash = read8(buffer, offset);
			offset++;
			offset = Utilities.read(buffer, offset, unk3);
			data_size = read32(buffer, offset);
			offset += 4;
			data_offset = read32(buffer, offset);
			offset += 4;
			offset = Utilities.read(buffer, offset, unk4);
			offset = Utilities.read(buffer, offset, unk5);

            // For PRX, the mode is big-endian, for direct sceKernelUtilsCopyWithRange,
            // the mode is little-endian. I don't know how to better differentiate these cases.
            if ((mode & 0x00FFFFFF) == 0x000000) {
            	mode = Integer.reverseBytes(mode);
            }

            return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, AES_key);
			offset = Utilities.write(buffer, offset, CMAC_key);
			offset = Utilities.write(buffer, offset, CMAC_header_hash);
			offset = Utilities.write(buffer, offset, CMAC_data_hash);
			offset = Utilities.write(buffer, offset, unused);
			offset = write32(buffer, offset, mode);
			offset = write8(buffer, offset, ecdsa_hash);
			offset = Utilities.write(buffer, offset, unk3);
			offset = write32(buffer, offset, data_size);
			offset = write32(buffer, offset, data_offset);
			offset = Utilities.write(buffer, offset, unk4);
			offset = Utilities.write(buffer, offset, unk5);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("AES_key=%s, CMAC_key=%s, CMAC_header_hash=%s, CMAC_data_hash=%s, unused=%s, mode=%d, ecdsa_hash=%d, unk3=%s, data_size=0x%X, data_offset=0x%X, unk4=%s, unk5=%s", Utilities.toString(AES_key), Utilities.toString(CMAC_key), Utilities.toString(CMAC_header_hash), Utilities.toString(CMAC_data_hash), Utilities.toString(unused), mode, ecdsa_hash, Utilities.toString(unk3), data_size, data_offset, Utilities.toString(unk4), Utilities.toString(unk5));
		}
	}

	public static class KIRK_CMD1_ECDSA_HEADER {
		public static final int SIZEOF = 0x90;
		public final byte[] AES_key = new byte[16];          // 0x0
		public final byte[] header_sig_r = new byte[20];     // 0x10
		public final byte[] header_sig_s = new byte[20];     // 0x24
		public final byte[] data_sig_r = new byte[20];       // 0x38
		public final byte[] data_sig_s = new byte[32];       // 0x4C
		public int mode;                                     // 0x60
		public int ecdsa_hash;                               // 0x64
		public final byte[] unk3 = new byte[11];             // 0x65
		public int data_size;                                // 0x70
		public int data_offset;                              // 0x74
		public final byte[] unk4 = new byte[8];              // 0x78
		public final byte[] unk5 = new byte[16];             // 0x80

		public KIRK_CMD1_ECDSA_HEADER() {
		}

		public KIRK_CMD1_ECDSA_HEADER(byte[] buffer, int offset) {
			read(buffer, offset);
		}

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, AES_key);
			offset = Utilities.read(buffer, offset, header_sig_r);
			offset = Utilities.read(buffer, offset, header_sig_s);
			offset = Utilities.read(buffer, offset, data_sig_r);
			offset = Utilities.read(buffer, offset, data_sig_s);
			mode = read32(buffer, offset);
			offset += 4;
			ecdsa_hash = read8(buffer, offset);
			offset++;
			offset = Utilities.read(buffer, offset, unk3);
			data_size = read32(buffer, offset);
			offset += 4;
			data_offset = read32(buffer, offset);
			offset += 4;
			offset = Utilities.read(buffer, offset, unk4);
			offset = Utilities.read(buffer, offset, unk5);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, AES_key);
			offset = Utilities.write(buffer, offset, header_sig_r);
			offset = Utilities.write(buffer, offset, header_sig_s);
			offset = Utilities.write(buffer, offset, data_sig_r);
			offset = Utilities.write(buffer, offset, data_sig_s);
			offset = write32(buffer, offset, mode);
			offset = write8(buffer, offset, ecdsa_hash);
			offset = Utilities.write(buffer, offset, unk3);
			offset = write32(buffer, offset, data_size);
			offset = write32(buffer, offset, data_offset);
			offset = Utilities.write(buffer, offset, unk4);
			offset = Utilities.write(buffer, offset, unk5);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("AES_key=%s, header_sig_r=%s, header_sig_s=%s, data_sig_r=%s, data_sig_s=%s, mode = 0x%X, ecdsa_hash = 0x%X, unk3=%s, data_size = 0x%X, data_offset = 0x%X, unk4=%s, unk5=%s", Utilities.toString(AES_key), Utilities.toString(header_sig_r), Utilities.toString(header_sig_s), Utilities.toString(data_sig_r), Utilities.toString(data_sig_s), mode, ecdsa_hash, Utilities.toString(unk3), data_size, data_offset, Utilities.toString(unk4), Utilities.toString(unk5));
		}
	}

	public static class KIRK_AES128CBC_HEADER {
		public static final int SIZEOF = 0x14;
		public int mode;
		public int unk_4;
		public int unk_8;
		public int keyseed;
		public int data_size;

		public KIRK_AES128CBC_HEADER(byte[] buffer, int offset) {
			read(buffer, offset);
		}

		public int read(byte[] buffer, int offset) {
			mode = read32(buffer, offset);
			offset += 4;
			unk_4 = read32(buffer, offset);
			offset += 4;
			unk_8 = read32(buffer, offset);
			offset += 4;
			keyseed = read32(buffer, offset);
			offset += 4;
			data_size = read32(buffer, offset);
			offset += 4;

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = write32(buffer, offset, mode);
			offset = write32(buffer, offset, unk_4);
			offset = write32(buffer, offset, unk_8);
			offset = write32(buffer, offset, keyseed);
			offset = write32(buffer, offset, data_size);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("mode=0x%X, unk_4=0x%X, unk_8=0x%X, keyseed=0x%02X, data_size=0x%X", mode, unk_4, unk_8, keyseed, data_size);
		}
	}

	public static class KIRK_CMD12_BUFFER {
		public static final int SIZEOF = ELT_SIZE + ECDSA_POINT.SIZEOF;
		public final byte[] private_key = new byte[ELT_SIZE];
		public final ECDSA_POINT public_key = new ECDSA_POINT();

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, private_key);
			offset = public_key.read(buffer, offset);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, private_key);
			offset = public_key.write(buffer, offset);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("private_key=%s, public_key=[%s]", Utilities.toString(private_key), public_key);
		}
	}

	public static class KIRK_SHA1_HEADER {
		public static final int SIZEOF = 4;
		public int data_size;

		public KIRK_SHA1_HEADER() {
		}

		public KIRK_SHA1_HEADER(byte[] buffer, int offset) {
			read(buffer, offset);
		}

		public int read(byte[] buffer, int offset) {
			data_size = read32(buffer, offset);
			offset += 4;

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write32(buffer, offset, data_size);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("data_size=0x%X", data_size);
		}
	}

	public static class KIRK_CMD13_BUFFER {
		public static final int SIZEOF = 0x14 + ECDSA_POINT.SIZEOF;
		public final byte[] multiplier = new byte[0x14];
		public final ECDSA_POINT public_key = new ECDSA_POINT();

		public KIRK_CMD13_BUFFER(byte[] buffer, int offset) {
			read(buffer, offset);
		}

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, multiplier);
			offset = public_key.read(buffer, offset);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, multiplier);
			offset = public_key.write(buffer, offset);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("multiplier=%s, public_key=[%s]", Utilities.toString(multiplier), public_key);
		}
	}

	public static class KIRK_CMD16_BUFFER {
		public static final int SIZEOF = 0x34;
		public final byte[] enc_private = new byte[0x20];
		public final byte[] message_hash = new byte[0x14];

		public KIRK_CMD16_BUFFER(byte[] buffer, int offset) {
			read(buffer, offset);
		}

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, enc_private);
			offset = Utilities.read(buffer, offset, message_hash);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, enc_private);
			offset = Utilities.write(buffer, offset, message_hash);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("enc_private=%s, message_hash=%s", Utilities.toString(enc_private), Utilities.toString(message_hash));
		}
	}

	public static class KIRK_CMD17_BUFFER {
		public static final int SIZEOF = ECDSA_POINT.SIZEOF + 0x14 + ECDSA_SIG.SIZEOF;
		public final ECDSA_POINT public_key = new ECDSA_POINT();
		public final byte[] message_hash = new byte[0x14];
		public final ECDSA_SIG signature = new ECDSA_SIG();

		public KIRK_CMD17_BUFFER(byte[] buffer, int offset) {
			read(buffer, offset);
		}

		public int read(byte[] buffer, int offset) {
			offset = public_key.read(buffer, offset);
			offset = Utilities.read(buffer, offset, message_hash);
			offset = signature.read(buffer, offset);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = public_key.write(buffer, offset);
			offset = Utilities.write(buffer, offset, message_hash);
			offset = signature.read(buffer, offset);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("public_key=[%s], message_hash=%s, signature=[%s]", public_key, Utilities.toString(message_hash), signature);
		}
	}

	public static class kirk16_data {
		public final byte[] fuseid = new byte[8]; //0
		public final byte[] mesh = new byte[0x40];  //0x8

		@Override
		public String toString() {
			return String.format("fuseid=%s, mesh=%s", Utilities.toString(fuseid), Utilities.toString(mesh));
		}
	}

	//small struct for temporary keeping AES & CMAC key from CMD1 header
	public static class header_keys {
		public static final int SIZEOF = 32;
		public final byte[] AES = new byte[16];
		public final byte[] CMAC = new byte[16];

		public int read(byte[] buffer, int offset) {
			offset = Utilities.read(buffer, offset, AES);
			offset = Utilities.read(buffer, offset, CMAC);

			return offset;
		}

		public int write(byte[] buffer, int offset) {
			offset = Utilities.write(buffer, offset, AES);
			offset = Utilities.write(buffer, offset, CMAC);

			return offset;
		}

		@Override
		public String toString() {
			return String.format("AES=%s, CMAC=%s", Utilities.toString(AES), Utilities.toString(CMAC));
		}
	}

	/* ------------------------- IMPLEMENTATION ------------------------- */

	public static int kirk_init(long fuseId) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("kirk_init fuseId=0x%X", fuseId));
		}
		AES.init();
		return kirk_init2("Lazy Dev should have initialized!".getBytes(), 33, (int) fuseId, (int) (fuseId >> 32));
	}

	private static int kirk_init2(byte[] rnd_seed, int seed_size, int fuseid_90, int fuseid_94) {
		final byte[] temp = new byte[0x104];

		KIRK_SHA1_HEADER header = new KIRK_SHA1_HEADER();
		// Another randomly selected data for a "key" to add to each randomization
		final byte[] key = {(byte) 0x07, (byte) 0xAB, (byte) 0xEF, (byte) 0xF8, (byte) 0x96, (byte) 0x8C, (byte) 0xF3, (byte) 0xD6, (byte) 0x14, (byte) 0xE0, (byte) 0xEB, (byte) 0xB2, (byte) 0x9D, (byte) 0x8B, (byte) 0x4E, (byte) 0x74};

		//Set PRNG_DATA initially, otherwise use what ever uninitialized data is in the buffer
		if (seed_size > 0) {
			byte[] seedbuf = new byte[seed_size + 4];
			KIRK_SHA1_HEADER seedheader = new KIRK_SHA1_HEADER();
			seedheader.data_size = seed_size;
			seedheader.write(seedbuf, 0);
			memcpy(seedbuf, KIRK_SHA1_HEADER.SIZEOF, rnd_seed, seed_size);
			kirk_CMD11(PRNG_DATA, seedbuf, seed_size + 4);
		}

		memcpy(temp, 4, PRNG_DATA, 0x14);
		write32(temp, 0x18, noRandomData ? 0 : curtime());
		memcpy(temp, 0x1C, key, 0x10);
		header.data_size = 0x100;
		header.write(temp, 0);
		kirk_CMD11(PRNG_DATA, temp, 0x104);

		//Set Fuse ID
		g_fuse90=fuseid_90;
		g_fuse94=fuseid_94;

		//Set KIRK1 main key
		AES_set_key(aes_kirk1, kirk1_key, 128);

		is_kirk_initialized = true;
		return 0;
	}

	private static byte[] kirk_4_7_get_key(int key_type) {
    	if (key_type < 0 || key_type >= KeyVault.keyvault.length) {
    		return null;
    	}

    	byte[] key = new byte[16];
    	int[] intKey = KeyVault.keyvault[key_type];
    	for (int i = 0; i < key.length; i++) {
    		key[i] = (byte) intKey[i];
    	}

    	return key;
	}

	public static int kirk_CMD0(byte[] outbuff, byte[] inbuff, int size, boolean generate_trash) {
		return kirk_CMD0(outbuff, 0, inbuff, 0, size, generate_trash);
	}

	public static int kirk_CMD0(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size, boolean generate_trash) {
		KIRK_CMD1_HEADER header = new KIRK_CMD1_HEADER();
		header_keys keys = new header_keys(); //0-15 AES key, 16-31 CMAC key
		int chk_size;
		AES.AES_ctx k1 = new AES.AES_ctx();
		AES.AES_ctx cmac_key = new AES.AES_ctx();
		final byte[] cmac_header_hash = new byte[16];
		final byte[] cmac_data_hash = new byte[16];

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}

		memcpy(outbuff, outoffset, inbuff, inoffset, size);
		header.read(outbuff, outoffset);
		keys.read(outbuff, outoffset);

		if (header.mode != KIRK_MODE_CMD1) {
			return KIRK_INVALID_MODE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_ENCRYPT_PRIVATE) %s", KIRK_CMD_ENCRYPT_PRIVATE, header));
		}

		//FILL PREDATA WITH RANDOM DATA
		if (generate_trash) {
			kirk_CMD14(outbuff, outoffset + KIRK_CMD1_HEADER.SIZEOF, header.data_offset);
		}

		//Make sure data is 16 aligned
		chk_size = header.data_size;
		if ((chk_size % 16) != 0) {
			chk_size += 16 - (chk_size % 16);
		}

		//ENCRYPT DATA
		AES_set_key(k1, keys.AES, 128);
		AES_cbc_encrypt(k1, inbuff, inoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, outbuff, outoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, chk_size);

		//CMAC HASHES
		AES_set_key(cmac_key, keys.CMAC, 128);
		AES_CMAC(cmac_key, outbuff, outoffset + 0x60, 0x30, cmac_header_hash);
		AES_CMAC(cmac_key, outbuff, outoffset + 0x60, 0x30 + chk_size + header.data_offset, cmac_data_hash);

		memcpy(header.CMAC_header_hash, cmac_header_hash, 16);
		memcpy(header.CMAC_data_hash, cmac_data_hash, 16);

		//ENCRYPT KEYS
		AES_cbc_encrypt(aes_kirk1, inbuff, outbuff, 16*2);
		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD1(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD1(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD1(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		final KIRK_CMD1_HEADER header = new KIRK_CMD1_HEADER(inbuff, inoffset);
		final header_keys keys = new header_keys(); //0-15 AES key, 16-31 CMAC key
		final AES.AES_ctx k1 = new AES.AES_ctx();

		if (size < 0x90) {
			return KIRK_INVALID_SIZE;
		}
		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (header.mode != KIRK_MODE_CMD1) {
			return KIRK_INVALID_MODE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_DECRYPT_PRIVATE) %s", KIRK_CMD_DECRYPT_PRIVATE, header));
		}

		AES_cbc_decrypt(aes_kirk1, inbuff, inoffset, keys, 16*2); //decrypt AES & CMAC key to temp buffer

		if (header.ecdsa_hash == 1) {
			SHA1.SHA_CTX sha = new SHA1.SHA_CTX();
			KIRK_CMD1_ECDSA_HEADER eheader = new KIRK_CMD1_ECDSA_HEADER(inbuff, inoffset);
			final byte[] kirk1_pub = new byte[40];
			final byte[] header_hash = new byte[20];
			final byte[] data_hash = new byte[20];
			ecdsa_set_curve(ec_p, ec_a, ec_b1, ec_N1, Gx1, Gy1);
			memcpy(kirk1_pub, Px1, 20);
			memcpy(kirk1_pub, 20, Py1, 20);
			ecdsa_set_pub(kirk1_pub);
			//Hash the Header
			SHAInit(sha);
			SHAUpdate(sha, inbuff, inoffset + 0x60, 0x30);
			SHAFinal(header_hash, sha);

			if (!ecdsa_verify(header_hash, eheader.header_sig_r, eheader.header_sig_s)) {
				return KIRK_HEADER_HASH_INVALID;
			}
			SHAInit(sha);
			SHAUpdate(sha, inbuff, inoffset + 0x60, size - 0x60);
			SHAFinal(data_hash, sha);  

			if (!ecdsa_verify(data_hash, eheader.data_sig_r, eheader.data_sig_s)) {
				return KIRK_DATA_HASH_INVALID;
			}
		} else  {
			int ret = kirk_CMD10(inbuff, inoffset, size);
			if (ret != KIRK_OPERATION_SUCCESS) {
	            // TODO Verify why the CMAC hashes aren't matching
				//return ret;
				if (log.isDebugEnabled()) {
					log.debug(String.format("Kirk KIRK_CMD_DECRYPT_PRIVATE ignoring that CMAC hashes were not matching (ret=0x%X)", ret));
				}
			}
		}

		AES_set_key(k1, keys.AES, 128);
		AES_cbc_decrypt(k1, inbuff, inoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, outbuff, outoffset, header.data_size);  

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD2(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD2(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD2(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		KIRK_CMD1_HEADER header = new KIRK_CMD1_HEADER(inbuff, inoffset);

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (!(header.mode == KIRK_MODE_CMD1 || header.mode == KIRK_MODE_CMD2 || header.mode == KIRK_MODE_CMD3)) {
			return KIRK_INVALID_MODE;
		}
		if (header.data_size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		log.warn(String.format("Kirk unimplemented cmd=0x%X(KIRK_CMD_ENCRYPT_SIGN) %s: %s", KIRK_CMD_ENCRYPT_SIGN, header, getMemoryDump(inbuff, inoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, header.data_size)));

		int ret = kirk_CMD10(inbuff, inoffset, size);
		if (ret != KIRK_OPERATION_SUCCESS) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Kirk KIRK_CMD_ENCRYPT_SIGN ignoring that CMAC hashes were not matching (ret=0x%X)", ret));
			}
		}

        // The header is kept in the output and the header.mode is updated from
        // KIRK_MODE_CMD2 to KIRK_MODE_CMD3.
		header.mode = KIRK_MODE_CMD3;
		header.write(outbuff, outoffset);

		// Not implemented, return the input data unchanged.
		memcpy(outbuff, outoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, inbuff, inoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, header.data_size);

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD3(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD3(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD3(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		KIRK_CMD1_HEADER header = new KIRK_CMD1_HEADER(inbuff, inoffset);

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (!(header.mode == KIRK_MODE_CMD1 || header.mode == KIRK_MODE_CMD2 || header.mode == KIRK_MODE_CMD3)) {
			return KIRK_INVALID_MODE;
		}
		if (header.data_size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		log.warn(String.format("Kirk unimplemented cmd=0x%X(KIRK_CMD_DECRYPT_SIGN) %s: %s", KIRK_CMD_DECRYPT_SIGN, header, getMemoryDump(inbuff, inoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, header.data_size)));

		int ret = kirk_CMD10(inbuff, inoffset, size);
		if (ret != KIRK_OPERATION_SUCCESS) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Kirk KIRK_CMD_DECRYPT_SIGN ignoring that CMAC hashes were not matching (ret=0x%X)", ret));
			}
		}

		// The output is only containing the decrypted data, there is no header.
		// Not implemented, return the input data unchanged.
		memcpy(outbuff, outoffset, inbuff, inoffset + KIRK_CMD1_HEADER.SIZEOF + header.data_offset, header.data_size);

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD4(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD4(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD4(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		KIRK_AES128CBC_HEADER header = new KIRK_AES128CBC_HEADER(inbuff, inoffset);
		AES.AES_ctx aesKey = new AES.AES_ctx();

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (header.mode != KIRK_MODE_ENCRYPT_CBC) {
			return KIRK_INVALID_MODE;
		}
		if (header.data_size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		byte[] key = kirk_4_7_get_key(header.keyseed);
		if (key == null) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_ENCRYPT_IV_0) %s", KIRK_CMD_ENCRYPT_IV_0, header));
		}

		//Set the key
		AES_set_key(aesKey, key, 128);
		AES_cbc_encrypt(aesKey, inbuff, inoffset + KIRK_AES128CBC_HEADER.SIZEOF, outbuff, outoffset + KIRK_AES128CBC_HEADER.SIZEOF, header.data_size);

        // The header is kept in the output and the header.mode is updated from
        // KIRK_MODE_ENCRYPT_CBC to KIRK_MODE_DECRYPT_CBC.
		header.mode = KIRK_MODE_DECRYPT_CBC;
		header.write(outbuff, outoffset);

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD5(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD5(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD5(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		KIRK_AES128CBC_HEADER header = new KIRK_AES128CBC_HEADER(inbuff, inoffset);
		AES.AES_ctx aesKey = new AES.AES_ctx();

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (header.mode != KIRK_MODE_ENCRYPT_CBC) {
			return KIRK_INVALID_MODE;
		}
		if (header.data_size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		byte[] key = null;
		if (header.keyseed == 0x100) {
			key = new byte[0x10];
		}
		if (key == null) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_ENCRYPT_IV_FUSE) %s", KIRK_CMD_ENCRYPT_IV_FUSE, header));
		}

		//Set the key
		AES_set_key(aesKey, key, 128);
		AES_cbc_encrypt(aesKey, inbuff, inoffset + KIRK_AES128CBC_HEADER.SIZEOF, outbuff, outoffset + KIRK_AES128CBC_HEADER.SIZEOF, header.data_size);

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD7(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD7(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD7(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		KIRK_AES128CBC_HEADER header = new KIRK_AES128CBC_HEADER(inbuff, inoffset);
		AES.AES_ctx aesKey = new AES.AES_ctx();

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (header.mode != KIRK_MODE_DECRYPT_CBC) {
			return KIRK_INVALID_MODE;
		}
		if (header.data_size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		byte[] key = kirk_4_7_get_key(header.keyseed);
		if (key == null) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_DECRYPT_IV_0) %s", KIRK_CMD_DECRYPT_IV_0, header));
		}

		//Set the key
		AES_set_key(aesKey, key, 128);
		AES_cbc_decrypt(aesKey, inbuff, inoffset + KIRK_AES128CBC_HEADER.SIZEOF, outbuff, outoffset, header.data_size);

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD8(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD8(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD8(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		KIRK_AES128CBC_HEADER header = new KIRK_AES128CBC_HEADER(inbuff, inoffset);
		AES.AES_ctx aesKey = new AES.AES_ctx();

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (header.mode != KIRK_MODE_DECRYPT_CBC) {
			return KIRK_INVALID_MODE;
		}
		if (header.data_size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		byte[] key = null;
		if (header.keyseed == 0x100) {
			key = new byte[0x10];
		}
		if (key == null) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_DECRYPT_IV_FUSE) %s", KIRK_CMD_DECRYPT_IV_FUSE, header));
		}

		//Set the key
		AES_set_key(aesKey, key, 128);
		AES_cbc_decrypt(aesKey, inbuff, inoffset + KIRK_AES128CBC_HEADER.SIZEOF, outbuff, outoffset, header.data_size);

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD10(byte[] inbuff, int insize) {
		return kirk_CMD10(inbuff, 0, insize);
	}

	public static int kirk_CMD10(byte[] inbuff, int inoffset, int insize) {
		KIRK_CMD1_HEADER header = new KIRK_CMD1_HEADER(inbuff, inoffset);
		header_keys keys = new header_keys(); //0-15 AES key, 16-31 CMAC key
		final byte[] cmac_header_hash = new byte[16];
		final byte[] cmac_data_hash = new byte[16];
		AES.AES_ctx cmac_key = new AES.AES_ctx();

		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (!(header.mode == KIRK_MODE_CMD1 || header.mode == KIRK_MODE_CMD2 || header.mode == KIRK_MODE_CMD3)) {
			return KIRK_INVALID_MODE;
		}
		if (header.data_size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_PRIV_SIGN_CHECK) %s", KIRK_CMD_PRIV_SIGN_CHECK, header));
		}

		if (header.mode == KIRK_MODE_CMD1) {
			AES_cbc_decrypt(aes_kirk1, inbuff, inoffset, keys, 32); //decrypt AES & CMAC key to temp buffer
		    AES_set_key(cmac_key, keys.CMAC, 128);
		    AES_CMAC(cmac_key, inbuff, inoffset + 0x60, 0x30, cmac_header_hash);

		    // Make sure data is 16 aligned
		    int chk_size = alignUp(header.data_size, 15);
		    AES_CMAC(cmac_key, inbuff, inoffset + 0x60, 0x30 + chk_size + header.data_offset, cmac_data_hash);

		    if (memcmp(cmac_header_hash, header.CMAC_header_hash, 16) != 0) {
		    	return KIRK_HEADER_HASH_INVALID;
		    }
		    if (memcmp(cmac_data_hash, header.CMAC_data_hash, 16) != 0) {
		    	return KIRK_DATA_HASH_INVALID;
		    }

		    return KIRK_OPERATION_SUCCESS;
	  	} else {
	  		log.warn(String.format("KIRK_MODE_CMD%d not implemented: %s", header.mode, header));
	  	}
		return KIRK_SIG_CHECK_INVALID; //Checks for cmd 2 & 3 not included right now
	}

	public static int kirk_CMD11(byte[] outbuff, byte[] inbuff, int size) {
		return kirk_CMD11(outbuff, 0, inbuff, 0, size);
	}

	public static int kirk_CMD11(byte[] outbuff, int outoffset, byte[] inbuff, int inoffset, int size) {
		KIRK_SHA1_HEADER header = new KIRK_SHA1_HEADER(inbuff, inoffset);
		SHA1.SHA_CTX sha = new SHA1.SHA_CTX();
		if (!is_kirk_initialized) {
			return KIRK_NOT_INITIALIZED;
		}
		if (header.data_size == 0 || size == 0) {
			return KIRK_DATA_SIZE_ZERO;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_SHA1_HASH) %s", KIRK_CMD_SHA1_HASH, header));
		}

		SHAInit(sha);
		SHAUpdate(sha, inbuff, KIRK_SHA1_HEADER.SIZEOF, header.data_size);
		SHAFinal(outbuff, outoffset, sha);
		return KIRK_OPERATION_SUCCESS;
	}

	// Generate an ECDSA Key pair
	// offset 0 = private key (0x14 len)
	// offset 0x14 = public key point (0x28 len)
	public static int kirk_CMD12(byte[] outbuff, int outsize) {
		return kirk_CMD12(outbuff, 0, outsize);
	}

	public static int kirk_CMD12(byte[] outbuff, int outoffset, int outsize) {
		final byte[] k = new byte[BIGNUMBER_SIZE];
		KIRK_CMD12_BUFFER keypair = new KIRK_CMD12_BUFFER();

		if (outsize != KIRK_CMD12_BUFFER.SIZEOF) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_ECDSA_GEN_KEYS)", KIRK_CMD_ECDSA_GEN_KEYS));
		}

		ecdsa_set_curve(ec_p, ec_a, ec_b2, ec_N2, Gx2, Gy2);
		//k[0] = 0;
		kirk_CMD14(k, 1, ELT_SIZE);
		ec_priv_to_pub(k, keypair.public_key);
		memcpy(keypair.private_key, k, 1, ELT_SIZE);

		keypair.write(outbuff, outoffset);

		return KIRK_OPERATION_SUCCESS;
	}

	// Point multiplication
	// offset 0 = mulitplication value (0x14 len)
	// offset 0x14 = point to multiply (0x28 len)
	public static int kirk_CMD13(byte[] outbuff, int outsize, byte[] inbuff, int insize) {
		return kirk_CMD13(outbuff, 0, outsize, inbuff, 0, insize);
	}

	public static int kirk_CMD13(byte[] outbuff, int outoffset, int outsize, byte[] inbuff, int inoffset, int insize) {
		final byte[] k = new byte[0x15];
		KIRK_CMD13_BUFFER pointmult = new KIRK_CMD13_BUFFER(inbuff, inoffset);
		//k[0]=0;
		if (outsize != 0x28 && outsize != KIRK_CMD13_BUFFER.SIZEOF) {
			return KIRK_INVALID_SIZE;
		}
		if (insize != KIRK_CMD13_BUFFER.SIZEOF) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_ECDSA_MULTIPLY_POINT) %s", KIRK_CMD_ECDSA_MULTIPLY_POINT, pointmult));
		}

		ecdsa_set_curve(ec_p, ec_a, ec_b2, ec_N2, Gx2, Gy2);
		ecdsa_set_pub(pointmult.public_key);
		memcpy(k, 1, pointmult.multiplier, 0x14);
		ec_pub_mult(k, outbuff, outoffset);
		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD14(byte[] outbuff, int outsize) {
		return kirk_CMD14(outbuff, 0, outsize);
	}

	public static int kirk_CMD14(byte[] outbuff, int outoffset, int outsize) {
		final byte[] temp = new byte[0x104];
		KIRK_SHA1_HEADER header = new KIRK_SHA1_HEADER();

		// Some randomly selected data for a "key" to add to each randomization
		final byte[] key = { (byte) 0xA7, (byte) 0x2E, (byte) 0x4C, (byte) 0xB6, (byte) 0xC3, (byte) 0x34, (byte) 0xDF, (byte) 0x85, (byte) 0x70, (byte) 0x01, (byte) 0x49, (byte) 0xFC, (byte) 0xC0, (byte) 0x87, (byte) 0xC4, (byte) 0x77 };
		//if(outsize != 0x14) return KIRK_INVALID_SIZE; // Need real error code
		if (outsize <= 0) {
			return KIRK_OPERATION_SUCCESS;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_PRNG)", KIRK_CMD_PRNG));
		}

		memcpy(temp, 4, PRNG_DATA, 0x14);
		write32(temp, 0x18, noRandomData ? 0 : curtime());
		memcpy(temp, 0x1C, key, 0x10);
		header.data_size = 0x100;
		header.write(temp, 0);
		kirk_CMD11(PRNG_DATA, temp, 0x104);
		while (outsize != 0) {
			int blockrem= outsize % 0x14;
			int block = outsize / 0x14;

			if (block != 0) {
				memcpy(outbuff, outoffset, PRNG_DATA, 0x14);
				outoffset += 0x14;
				outsize -= 0x14;
				kirk_CMD14(outbuff, outoffset, outsize);
			} else {
				if (blockrem != 0) {
					memcpy(outbuff, outoffset, PRNG_DATA, blockrem);
					outsize -= blockrem;
				}
			}
		}

		return KIRK_OPERATION_SUCCESS;
	}

	public static int kirk_CMD15(byte[] outbuff, int outsize, byte[] inbuff, int insize) {
		return kirk_CMD15(outbuff, 0, outsize, inbuff, 0, insize);
	}

	public static int kirk_CMD15(byte[] outbuff, int outoffset, int outsize, byte[] inbuff, int inoffset, int insize) {
		if (outsize != 28 || insize < 8) {
			return KIRK_OPERATION_SUCCESS;
		}

    	long input = endianSwap64(read64(inbuff, inoffset));
    	long output = input + 1;
    	outoffset = write64(outbuff, outoffset, output);

    	// Unknown output values.
    	// The values differ at each call, even for 2 calls in sequence.
    	// Maybe they represent the state of the random number generator.
    	outoffset = write32(outbuff, outoffset, 0x12345678);
    	outoffset = write32(outbuff, outoffset, 0x12345678);
    	outoffset = write32(outbuff, outoffset, 0x12345678);
    	outoffset = write32(outbuff, outoffset, 0x12345678);
    	outoffset = write32(outbuff, outoffset, 0x12345678);

    	return KIRK_OPERATION_SUCCESS;
	}

	public static void decrypt_kirk16_private(byte[] dA_out, byte[] dA_enc) {
		decrypt_kirk16_private(dA_out, 0, dA_enc, 0);
	}

	public static void decrypt_kirk16_private(byte[] dA_out, int dA_outoffset, byte[] dA_enc, int dA_encoffset) {
		kirk16_data keydata = new kirk16_data();
		final byte[] subkey_1 = new byte[0x10];
		final byte[] subkey_2 = new byte[0x10];
		AES.AES_ctx aes_ctx = new AES.AES_ctx();

		keydata.fuseid[7] = (byte) (g_fuse90 & 0xFF);
		keydata.fuseid[6] = (byte) ((g_fuse90>>8) & 0xFF);
		keydata.fuseid[5] = (byte) ((g_fuse90>>16) & 0xFF);
		keydata.fuseid[4] = (byte) ((g_fuse90>>24) & 0xFF); 
		keydata.fuseid[3] = (byte) (g_fuse94 & 0xFF);
		keydata.fuseid[2] = (byte) ((g_fuse94>>8) & 0xFF);
		keydata.fuseid[1] = (byte) ((g_fuse94>>16) & 0xFF);
		keydata.fuseid[0] = (byte) ((g_fuse94>>24) & 0xFF);

		/* set encryption key */
		rijndael_set_key(aes_ctx, kirk16_key, 128);

		/* set the subkeys */
		for (int i = 0; i < 0x10; i++) {
			/* set to the fuseid */
			subkey_2[i] = subkey_1[i] = keydata.fuseid[i % 8];
		}

		/* do aes crypto */
		for (int i = 0; i < 3; i++) {
			/* encrypt + decrypt */
			rijndael_encrypt(aes_ctx, subkey_1, subkey_1);
			rijndael_decrypt(aes_ctx, subkey_2, subkey_2);
		}

		/* set new key */
		rijndael_set_key(aes_ctx, subkey_1, 128);

		/* now lets make the key mesh */
		for (int i = 0; i < 3; i++) {
			/* do encryption in group of 3 */
			for (int k = 0; k < 3; k++) {
				/* crypto */
				rijndael_encrypt(aes_ctx, subkey_2, subkey_2);
			}

			/* copy to out block */
			memcpy(keydata.mesh, i * 0x10, subkey_2, 0x10);
		}

		/* set the key to the mesh */
		rijndael_set_key(aes_ctx, keydata.mesh, 0x20, 128);

		/* do the encryption routines for the aes key */
		for (int i = 0; i < 2; i++) {
			/* encrypt the data */
			rijndael_encrypt(aes_ctx, keydata.mesh, 0x10, keydata.mesh, 0x10);
		}

		/* set the key to that mesh shit */
		rijndael_set_key(aes_ctx, keydata.mesh, 0x10, 128);

		/* cbc decrypt the dA */
		AES_cbc_decrypt(aes_ctx, dA_enc, dA_encoffset, dA_out, dA_outoffset, 0x20);
	}

	public static void encrypt_kirk16_private(byte[] dA_out, byte[] dA_dec) {
		encrypt_kirk16_private(dA_out, 0, dA_dec, 0);
	}

	public static void encrypt_kirk16_private(byte[] dA_out, int dA_outoffset, byte[] dA_dec, int dA_decoffset) {
		kirk16_data keydata = new kirk16_data();
		final byte[] subkey_1 = new byte[0x10];
		final byte[] subkey_2 = new byte[0x10];
		AES.AES_ctx aes_ctx = new AES.AES_ctx();

		keydata.fuseid[7] = (byte) (g_fuse90 & 0xFF);
		keydata.fuseid[6] = (byte) ((g_fuse90>>8) & 0xFF);
		keydata.fuseid[5] = (byte) ((g_fuse90>>16) & 0xFF);
		keydata.fuseid[4] = (byte) ((g_fuse90>>24) & 0xFF); 
		keydata.fuseid[3] = (byte) (g_fuse94 & 0xFF);
		keydata.fuseid[2] = (byte) ((g_fuse94>>8) & 0xFF);
		keydata.fuseid[1] = (byte) ((g_fuse94>>16) & 0xFF);
		keydata.fuseid[0] = (byte) ((g_fuse94>>24) & 0xFF);

		/* set encryption key */
		rijndael_set_key(aes_ctx, kirk16_key, 128);

		/* set the subkeys */
		for (int i = 0; i < 0x10; i++) {
			/* set to the fuseid */
			subkey_2[i] = subkey_1[i] = keydata.fuseid[i % 8];
		}
	 
		/* do aes crypto */
		for (int i = 0; i < 3; i++) {
			/* encrypt + decrypt */
			rijndael_encrypt(aes_ctx, subkey_1, subkey_1);
			rijndael_decrypt(aes_ctx, subkey_2, subkey_2);
		}
	 
		/* set new key */
		rijndael_set_key(aes_ctx, subkey_1, 128);
	 
		/* now lets make the key mesh */
		for (int i = 0; i < 3; i++) {
			/* do encryption in group of 3 */
			for (int k = 0; k < 3; k++) {
				/* crypto */
				rijndael_encrypt(aes_ctx, subkey_2, subkey_2);
			}

			/* copy to out block */
			memcpy(keydata.mesh, i * 0x10, subkey_2, 0x10);
		}

		/* set the key to the mesh */
		rijndael_set_key(aes_ctx, keydata.mesh, 0x20, 128);
	 
		/* do the encryption routines for the aes key */
		for (int i = 0; i < 2; i++) {
			/* encrypt the data */
			rijndael_encrypt(aes_ctx, keydata.mesh, 0x10, keydata.mesh, 0x10);
		}
	 
		/* set the key to that mesh shit */
		rijndael_set_key(aes_ctx, keydata.mesh, 0x10, 128);

		/* cbc encrypt the dA */
		AES_cbc_encrypt(aes_ctx, dA_dec, dA_decoffset, dA_out, dA_outoffset, 0x20);
	}

	public static int kirk_CMD16(byte[] outbuff, int outsize, byte[] inbuff, int insize) {
		return kirk_CMD16(outbuff, 0, outsize, inbuff, 0, insize);
	}

	public static int kirk_CMD16(byte[] outbuff, int outoffset, int outsize, byte[] inbuff, int inoffset, int insize) {
		final byte[] dec_private = new byte[0x20];
		KIRK_CMD16_BUFFER signbuf = new KIRK_CMD16_BUFFER(inbuff, inoffset);
		ECDSA_SIG sig = new ECDSA_SIG();
		if (insize != KIRK_CMD16_BUFFER.SIZEOF) {
			return KIRK_INVALID_SIZE;
		}
		if (outsize != 0x28 && outsize != KIRK_CMD16_BUFFER.SIZEOF) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_ECDSA_SIGN) %s", KIRK_CMD_ECDSA_SIGN, signbuf));
		}

		decrypt_kirk16_private(dec_private, signbuf.enc_private);
		// Clear out the padding for safety
		memset(dec_private, 0x14, 0, 0xC);
		ecdsa_set_curve(ec_p, ec_a, ec_b2, ec_N2, Gx2, Gy2);
		ecdsa_set_priv(dec_private);
		ecdsa_sign(signbuf.message_hash, sig.r, sig.s);
		sig.write(outbuff, outoffset);
		return KIRK_OPERATION_SUCCESS;
	}

	// ECDSA Verify
	// inbuff structure:
	// 00 = public key (0x28 length)
	// 28 = message hash (0x14 length)
	// 3C = signature R (0x14 length)
	// 50 = signature S (0x14 length)
	public static int kirk_CMD17(byte[] inbuff, int insize) {
		return kirk_CMD17(inbuff, 0, insize);
	}

	public static int kirk_CMD17(byte[] inbuff, int inoffset, int insize) {
		KIRK_CMD17_BUFFER sig = new KIRK_CMD17_BUFFER(inbuff, inoffset);
		if (insize != KIRK_CMD17_BUFFER.SIZEOF) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_ECDSA_VERIFY) %s", KIRK_CMD_ECDSA_VERIFY, sig));
		}

		ecdsa_set_curve(ec_p, ec_a, ec_b2, ec_N2, Gx2, Gy2);
		ecdsa_set_pub(sig.public_key);
		// ecdsa_verify(u8 *hash, u8 *R, u8 *S)
		if (ecdsa_verify(sig.message_hash, sig.signature.r, sig.signature.s)) {
			return KIRK_OPERATION_SUCCESS;
		} else {
			return KIRK_SIG_CHECK_INVALID;
		}
	}

	public static int kirk_CMD18(byte[] inbuff, int insize) {
		return kirk_CMD18(inbuff, 0, insize);
	}

	public static int kirk_CMD18(byte[] inbuff, int inoffset, int insize) {
		if (insize != 0xB8) {
			return KIRK_INVALID_SIZE;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Kirk cmd=0x%X(KIRK_CMD_CERT_VERIFY) %s", KIRK_CMD_CERT_VERIFY, Utilities.toString(inbuff, inoffset, insize)));
		}

		// TODO Kirk CMD 18 (KIRK_CMD_CERT_VERIFY) not implemented

		return KIRK_OPERATION_SUCCESS;
	}

	public static int sceUtilsBufferCopyWithRange(byte[] outbuff, int outsize, byte[] inbuff, int insize, int cmd) {
		return sceUtilsBufferCopyWithRange(outbuff, 0, outsize, inbuff, 0, insize, cmd);
	}

	public static int sceUtilsBufferCopyWithRange(byte[] outbuff, int outoffset, int outsize, byte[] inbuff, int inoffset, int insize, int cmd) {
		switch (cmd) {
			case KIRK_CMD_ENCRYPT_PRIVATE:      return kirk_CMD0 (outbuff, outoffset, inbuff, inoffset, insize, true);
			case KIRK_CMD_DECRYPT_PRIVATE:      return kirk_CMD1 (outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_ENCRYPT_SIGN:         return kirk_CMD2 (outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_DECRYPT_SIGN:         return kirk_CMD3 (outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_ENCRYPT_IV_0:         return kirk_CMD4 (outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_ENCRYPT_IV_FUSE:      return kirk_CMD5 (outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_DECRYPT_IV_0:         return kirk_CMD7 (outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_DECRYPT_IV_FUSE:      return kirk_CMD8 (outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_PRIV_SIGN_CHECK:      return kirk_CMD10(inbuff, inoffset, insize);
			case KIRK_CMD_SHA1_HASH:            return kirk_CMD11(outbuff, outoffset, inbuff, inoffset, insize);
			case KIRK_CMD_ECDSA_GEN_KEYS:       return kirk_CMD12(outbuff, outoffset, outsize);
			case KIRK_CMD_ECDSA_MULTIPLY_POINT: return kirk_CMD13(outbuff, outoffset, outsize, inbuff, inoffset, insize);
			case KIRK_CMD_PRNG:                 return kirk_CMD14(outbuff, outoffset, outsize);
			case KIRK_CMD_INIT:                 return kirk_CMD15(outbuff, outoffset, outsize, inbuff, inoffset, insize);
			case KIRK_CMD_ECDSA_SIGN:           return kirk_CMD16(outbuff, outoffset, outsize, inbuff, inoffset, insize);
			case KIRK_CMD_ECDSA_VERIFY:         return kirk_CMD17(inbuff, inoffset, insize);
			case KIRK_CMD_CERT_VERIFY:          return kirk_CMD18(inbuff, inoffset, insize);
		}

		log.error(String.format("sceUtilsBufferCopyWithRange unimplemented Kirk cmd=0x%X", cmd));

		return -1;
	}
}
