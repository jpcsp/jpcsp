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

package jpcsp.crypto;

import java.nio.ByteBuffer;
import java.util.Random;

import jpcsp.format.PSF;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;

@SuppressWarnings("unused")
public class CryptoEngine {

    // Internal vars.
    private boolean isCryptoEngineInit;
    private static boolean extractEboot;
    private static ExtractEbootSettingsListerner extractEbootSettingsListerner;
    private static CryptSavedataSettingsListerner cryptSavedataSettingsListerner;
    private static final String name = "CryptEngine";
    private static boolean cryptoSavedata;
    private static BBCipherCtx pgdCipherContext;
    private static BBMacCtx pgdMacContext;
    private int[] fuseID = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};

    // KIRK CMD1 AESCBC128-CMAC key.
    private static final int[] kirkAESKey0 = {0x98, 0xC9, 0x40, 0x97, 0x5C, 0x1D, 0x10, 0xE8, 0x7F, 0xE6, 0x0E, 0xA3, 0xFD, 0x03, 0xA8, 0xBA};

    // KIRK CMD4 and CMD7 AESCBC128 key table.
    private static final int[] kirkAESKey1 = {0x98, 0x02, 0xC4, 0xE6, 0xEC, 0x9E, 0x9E, 0x2F, 0xFC, 0x63, 0x4C, 0xE4, 0x2F, 0xBB, 0x46, 0x68};
    private static final int[] kirkAESKey2 = {0x99, 0x24, 0x4C, 0xD2, 0x58, 0xF5, 0x1B, 0xCB, 0xB0, 0x61, 0x9C, 0xA7, 0x38, 0x30, 0x07, 0x5F};
    private static final int[] kirkAESKey3 = {0x02, 0x25, 0xD7, 0xBA, 0x63, 0xEC, 0xB9, 0x4A, 0x9D, 0x23, 0x76, 0x01, 0xB3, 0xF6, 0xAC, 0x17};
    private static final int[] kirkAESKey4 = {0x84, 0x85, 0xC8, 0x48, 0x75, 0x08, 0x43, 0xBC, 0x9B, 0x9A, 0xEC, 0xA7, 0x9C, 0x7F, 0x60, 0x18};
    private static final int[] kirkAESKey5 = {0xB5, 0xB1, 0x6E, 0xDE, 0x23, 0xA9, 0x7B, 0x0E, 0xA1, 0x7C, 0xDB, 0xA2, 0xDC, 0xDE, 0xC4, 0x6E};
    private static final int[] kirkAESKey6 = {0xC8, 0x71, 0xFD, 0xB3, 0xBC, 0xC5, 0xD2, 0xF2, 0xE2, 0xD7, 0x72, 0x9D, 0xDF, 0x82, 0x68, 0x82};
    private static final int[] kirkAESKey7 = {0x0A, 0xBB, 0x33, 0x6C, 0x96, 0xD4, 0xCD, 0xD8, 0xCB, 0x5F, 0x4B, 0xE0, 0xBA, 0xDB, 0x9E, 0x03};
    private static final int[] kirkAESKey8 = {0x32, 0x29, 0x5B, 0xD5, 0xEA, 0xF7, 0xA3, 0x42, 0x16, 0xC8, 0x8E, 0x48, 0xFF, 0x50, 0xD3, 0x71};
    private static final int[] kirkAESKey9 = {0x46, 0xF2, 0x5E, 0x8E, 0x4D, 0x2A, 0xA5, 0x40, 0x73, 0x0B, 0xC4, 0x6E, 0x47, 0xEE, 0x6F, 0x0A};
    private static final int[] kirkAESKey10 = {0x5D, 0xC7, 0x11, 0x39, 0xD0, 0x19, 0x38, 0xBC, 0x02, 0x7F, 0xDD, 0xDC, 0xB0, 0x83, 0x7D, 0x9D};
    private static final int[] kirkAESKey11 = {0x12, 0x46, 0x8D, 0x7E, 0x1C, 0x42, 0x20, 0x9B, 0xBA, 0x54, 0x26, 0x83, 0x5E, 0xB0, 0x33, 0x03};
    private static final int[] kirkAESKey12 = {0xC4, 0x3B, 0xB6, 0xD6, 0x53, 0xEE, 0x67, 0x49, 0x3E, 0xA9, 0x5F, 0xBC, 0x0C, 0xED, 0x6F, 0x8A};
    private static final int[] kirkAESKey13 = {0x2C, 0xC3, 0xCF, 0x8C, 0x28, 0x78, 0xA5, 0xA6, 0x63, 0xE2, 0xAF, 0x2D, 0x71, 0x5E, 0x86, 0xBA};
    private static final int[] kirkAESKey14 = {0x0C, 0xFD, 0x67, 0x9A, 0xF9, 0xB4, 0x72, 0x4F, 0xD7, 0x8D, 0xD6, 0xE9, 0x96, 0x42, 0x28, 0x8B};
    private static final int[] kirkAESKey15 = {0xAF, 0xFE, 0x8E, 0xB1, 0x3D, 0xD1, 0x7E, 0xD8, 0x0A, 0x61, 0x24, 0x1C, 0x95, 0x92, 0x56, 0xB6};
    private static final int[] kirkAESKey16 = {0x1C, 0x9B, 0xC4, 0x90, 0xE3, 0x06, 0x64, 0x81, 0xFA, 0x59, 0xFD, 0xB6, 0x00, 0xBB, 0x28, 0x70};
    private static final int[] kirkAESKey17 = {0x11, 0x5A, 0x5D, 0x20, 0xD5, 0x3A, 0x8D, 0xD3, 0x9C, 0xC5, 0xAF, 0x41, 0x0F, 0x0F, 0x18, 0x6F};
    private static final int[] kirkAESKey18 = {0x9C, 0x9B, 0x13, 0x72, 0xF8, 0xC6, 0x40, 0xCF, 0x1C, 0x62, 0xF5, 0xD5, 0x92, 0xDD, 0xB5, 0x82};
    private static final int[] kirkAESKey19 = {0x03, 0xB3, 0x02, 0xE8, 0x5F, 0xF3, 0x81, 0xB1, 0x3B, 0x8D, 0xAA, 0x2A, 0x90, 0xFF, 0x5E, 0x61};
    
    // KIRK CMD16 key.
    private static final int[] kirkAESKey20 = {0x47, 0x5E, 0x09, 0xF4, 0xA2, 0x37, 0xDA, 0x9B, 0xEF, 0xFF, 0x3B, 0xC0, 0x77, 0x14, 0x3D, 0x8A};
    
    // KIRK CMD17 ECDSA PUB keys. 
    private static final int[] kirkCertSigs = {
        // certSig0
        0x40, 0x04, 0xC8, 0x0B, 0xD9, 0xC8, 0xBA, 0x38,
        0x22, 0x10, 0x65, 0x92, 0x3E, 0x32, 0x4B, 0x5F,
        0x0E, 0xC1, 0x65, 0xED, 0x6C, 0xFF, 0x7D, 0x9F,
        0x2C, 0x42, 0x0B, 0x84, 0xDF, 0xDA, 0x6E, 0x96,
        0xC0, 0xAE, 0xE2, 0x99, 0x27, 0xBC, 0xAF, 0x1E,
        // certSig1
        0x06, 0x48, 0x5F, 0xD0, 0x29, 0x85, 0x3B, 0x55,
        0x2F, 0x7E, 0xFD, 0xD6, 0x7A, 0x2D, 0xE7, 0xA1,
        0xA4, 0xE2, 0x55, 0x37, 0xB2, 0x45, 0x9D, 0x87,
        0x86, 0x42, 0x6D, 0x5B, 0x27, 0xEF, 0xA5, 0xA9,
        0x31, 0x1C, 0xB8, 0xAB, 0xAB, 0xFA, 0x0E, 0xCE,
        // certSig2
        0x3F, 0x8C, 0x34, 0xF2, 0x10, 0xAE, 0xC4, 0x8E,
        0x15, 0x20, 0xFF, 0x2A, 0x44, 0x89, 0x9E, 0x05,
        0x4A, 0x0D, 0xA3, 0x3D, 0xF8, 0xB9, 0x75, 0x4B,
        0x09, 0xC0, 0xEC, 0x7E, 0x61, 0x86, 0x7A, 0x51,
        0x26, 0xFE, 0x69, 0x26, 0x97, 0x21, 0x96, 0xF5,
        // certSig3
        0xCC, 0xB3, 0x44, 0x0D, 0xC4, 0x83, 0x6D, 0xD5,
        0x19, 0xE1, 0x3B, 0x28, 0x05, 0xB3, 0x08, 0x70,
        0xDC, 0xAE, 0xE4, 0x62, 0x13, 0x6B, 0x38, 0x88,
        0x65, 0x1A, 0x98, 0xE0, 0x2B, 0x29, 0xFA, 0x0C,
        0xD3, 0x4F, 0x16, 0x16, 0xF1, 0xED, 0x57, 0x86,
        // certSig4
        0x08, 0xB3, 0x36, 0x92, 0x5C, 0x2B, 0x44, 0x5D,
        0x03, 0xA9, 0xBE, 0x51, 0xB9, 0xAA, 0xBF, 0x54,
        0xE4, 0xCC, 0x14, 0x2E, 0xA7, 0x2A, 0x23, 0xBB,
        0x80, 0x60, 0xB0, 0x3B, 0x71, 0xCD, 0xE0, 0x77,
        0x2D, 0xE8, 0x2A, 0xD8, 0x93, 0x16, 0x48, 0xD6,
        // certSig5
        0x4F, 0x0A, 0x2B, 0xC9, 0x98, 0x76, 0x40, 0x86,
        0x0E, 0x22, 0xEE, 0x5D, 0x86, 0x08, 0x7C, 0x96,
        0x92, 0x47, 0x0B, 0xDF, 0x59, 0xDC, 0x4C, 0x1F,
        0x2E, 0x38, 0xF9, 0x2C, 0xE7, 0xB6, 0x68, 0x75,
        0xB5, 0x9E, 0xD1, 0x0C, 0x9D, 0x84, 0xFA, 0x6A,
    };
    
    // SPOCK CMD9 key. 
    private static final int[] spockAESKey1 = {0x9F, 0x46, 0xF9, 0xFC, 0xFA, 0xB2, 0xAD, 0x05, 0x69, 0xF6, 0x88, 0xD8, 0x79, 0x4B, 0x92, 0xBA};

    // CHNNLSV SD keys.
    private static final int[] sdHashKey1 = {0x40, 0xE6, 0x53, 0x3F, 0x05, 0x11, 0x3A, 0x4E, 0xA1, 0x4B, 0xDA, 0xD6, 0x72, 0x7C, 0x53, 0x4C};
    private static final int[] sdHashKey2 = {0xFA, 0xAA, 0x50, 0xEC, 0x2F, 0xDE, 0x54, 0x93, 0xAD, 0x14, 0xB2, 0xCE, 0xA5, 0x30, 0x05, 0xDF};
    private static final int[] sdHashKey3 = {0x36, 0xA5, 0x3E, 0xAC, 0xC5, 0x26, 0x9E, 0xA3, 0x83, 0xD9, 0xEC, 0x25, 0x6C, 0x48, 0x48, 0x72};
    private static final int[] sdHashKey4 = {0xD8, 0xC0, 0xB0, 0xF3, 0x3E, 0x6B, 0x76, 0x85, 0xFD, 0xFB, 0x4D, 0x7D, 0x45, 0x1E, 0x92, 0x03};
    private static final int[] sdHashKey5 = {0xCB, 0x15, 0xF4, 0x07, 0xF9, 0x6A, 0x52, 0x3C, 0x04, 0xB9, 0xB2, 0xEE, 0x5C, 0x53, 0xFA, 0x86};
    private static final int[] sdHashKey6 = {0x70, 0x44, 0xA3, 0xAE, 0xEF, 0x5D, 0xA5, 0xF2, 0x85, 0x7F, 0xF2, 0xD6, 0x94, 0xF5, 0x36, 0x3B};
    private static final int[] sdHashKey7 = {0xEC, 0x6D, 0x29, 0x59, 0x26, 0x35, 0xA5, 0x7F, 0x97, 0x2A, 0x0D, 0xBC, 0xA3, 0x26, 0x33, 0x00};

    // AMCTRL keys.
    private static final int[] amHashKey1 = {0x9C, 0x48, 0xB6, 0x28, 0x40, 0xE6, 0x53, 0x3F, 0x05, 0x11, 0x3A, 0x4E, 0x65, 0xE6, 0x3A, 0x64};
    private static final int[] amHashKey2 = {0x70, 0xB4, 0x7B, 0xC0, 0xA1, 0x4B, 0xDA, 0xD6, 0xE0, 0x10, 0x14, 0xED, 0x72, 0x7C, 0x53, 0x4C};
    private static final int[] amHashKey3 = {0xE3, 0x50, 0xED, 0x1D, 0x91, 0x0A, 0x1F, 0xD0, 0x29, 0xBB, 0x1C, 0x3E, 0xF3, 0x40, 0x77, 0xFB};
    private static final int[] amHashKey4 = {0x13, 0x5F, 0xA4, 0x7C, 0xAB, 0x39, 0x5B, 0xA4, 0x76, 0xB8, 0xCC, 0xA9, 0x8F, 0x3A, 0x04, 0x45};
    private static final int[] amHashKey5 = {0x67, 0x8D, 0x7F, 0xA3, 0x2A, 0x9C, 0xA0, 0xD1, 0x50, 0x8A, 0xD8, 0x38, 0x5E, 0x4B, 0x01, 0x7E};
    
    private static final int[] drmFixedKey1 = {0x38, 0x20, 0xD0, 0x11, 0x07, 0xA3, 0xFF, 0x3E, 0x0A, 0x4C, 0x20, 0x85, 0x39, 0x10, 0xB5, 0x54};
    private static final int[] drmFixedKey2 = {0xBA, 0x87, 0xE4, 0xAB, 0x2C, 0x60, 0x5F, 0x59, 0xB8, 0x3B, 0xDB, 0xA6, 0x82, 0xFD, 0xAE, 0x14};
    private static final int[] drmVersionKey1 = {0xDA, 0x7D, 0x4B, 0x5E, 0x49, 0x9A, 0x4F, 0x53, 0xB1, 0xC1, 0xA1, 0x4A, 0x74, 0x84, 0x44, 0x3B};
    private static final int[] drmVersionKey2 = {0x69, 0xB4, 0x53, 0xF2, 0xE4, 0x21, 0x89, 0x8E, 0x53, 0xE4, 0xA3, 0x5A, 0x5B, 0x91, 0x79, 0x51};
    private static final int[] drmNameKey = {0xEB, 0x71, 0x5D, 0xB8, 0xD3, 0x73, 0xCE, 0xA4, 0x6F, 0xE7, 0x1D, 0xCF, 0xFF, 0x63, 0xFA, 0xEA};
    
    private static final int[] drmActRifSig = {
        0x62, 0x27, 0xB0, 0x0A, 0x02, 0x85, 0x6F, 0xB0,
        0x41, 0x08, 0x87, 0x67, 0x19, 0xE0, 0xA0, 0x18,
        0x32, 0x91, 0xEE, 0xB9, 0x6E, 0x73, 0x6A, 0xBF,
        0x81, 0xF7, 0x0E, 0xE9, 0x16, 0x1B, 0x0D, 0xDE,
        0xB0, 0x26, 0x76, 0x1A, 0x5B, 0xC8, 0x7B, 0xFF
    };
    
    private static final int[] drmEdatSprxSig = {
        0x1F, 0x07, 0x2B, 0xCC, 0xC1, 0x62, 0xF2, 0xCF,
        0xAE, 0xA0, 0xE7, 0xF4, 0xCD, 0xFD, 0x9C, 0xAE,
        0xC6, 0xC4, 0x55, 0x21, 0x53, 0x01, 0xF4, 0xE3,
        0x70, 0xC3, 0xED, 0xE2, 0xD4, 0xF5, 0xDB, 0xC3,
        0xA7, 0xDE, 0x8C, 0xAA, 0xE8, 0xAD, 0x5B, 0x7D
    };

    
    // KIRK error values.
    private static final int PSP_KIRK_NOT_ENABLED = 0x1;
    private static final int PSP_KIRK_INVALID_MODE = 0x2;
    private static final int PSP_KIRK_INVALID_HEADER_HASH = 0x3;
    private static final int PSP_KIRK_INVALID_DATA_HASH = 0x4;
    private static final int PSP_KIRK_INVALID_SIG_CHECK = 0x5;
    private static final int PSP_KIRK_UNK1 = 0x6;
    private static final int PSP_KIRK_UNK2 = 0x7;
    private static final int PSP_KIRK_UNK3 = 0x8;
    private static final int PSP_KIRK_UNK4 = 0x9;
    private static final int PSP_KIRK_UNK5 = 0xA;
    private static final int PSP_KIRK_UNK6 = 0xB;
    private static final int PSP_KIRK_NOT_INIT = 0xC;
    private static final int PSP_KIRK_INVALID_OPERATION = 0xD;
    private static final int PSP_KIRK_INVALID_SEED = 0xE;
    private static final int PSP_KIRK_INVALID_SIZE = 0xF;
    private static final int PSP_KIRK_DATA_SIZE_IS_ZERO = 0x10;  
    private static final int PSP_SUBCWR_NOT_16_ALGINED = 0x90A;
    private static final int PSP_SUBCWR_HEADER_HASH_INVALID = 0x920;
    private static final int PSP_SUBCWR_BUFFER_TOO_SMALL = 0x1000;

    // KIRK commands.
    private static final int PSP_KIRK_CMD_DECRYPT_PRIVATE = 0x1;         // Master decryption command, used by firmware modules. Applies CMAC checking.
    private static final int PSP_KIRK_CMD_ENCRYPT_SIGN = 0x2;            // Used for key type 3 (blacklisting), encrypts and signs data with a ECDSA signature.
    private static final int PSP_KIRK_CMD_DECRYPT_SIGN = 0x3;            // Used for key type 3 (blacklisting), decrypts and signs data with a ECDSA signature.
    private static final int PSP_KIRK_CMD_ENCRYPT = 0x4;                 // Key table based encryption used for general purposes by several modules.
    private static final int PSP_KIRK_CMD_ENCRYPT_FUSE = 0x5;            // Fuse ID based encryption used for general purposes by several modules.
    private static final int PSP_KIRK_CMD_ENCRYPT_USER = 0x6;            // User specified ID based encryption used for general purposes by several modules.
    private static final int PSP_KIRK_CMD_DECRYPT = 0x7;                 // Key table based decryption used for general purposes by several modules.
    private static final int PSP_KIRK_CMD_DECRYPT_FUSE = 0x8;            // Fuse ID based decryption used for general purposes by several modules.
    private static final int PSP_KIRK_CMD_DECRYPT_USER = 0x9;            // User specified ID based decryption used for general purposes by several modules.
    private static final int PSP_KIRK_CMD_PRIV_SIG_CHECK = 0xA;          // Private signature (SCE) checking command.
    private static final int PSP_KIRK_CMD_SHA1_HASH = 0xB;               // SHA1 hash generating command.
    private static final int PSP_KIRK_CMD_ECDSA_GEN_KEYS = 0xC;          // ECDSA key generating mul1 command. 
    private static final int PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT = 0xD;    // ECDSA key generating mul2 command. 
    private static final int PSP_KIRK_CMD_PRNG = 0xE;                    // Random number generating command. 
    private static final int PSP_KIRK_CMD_INIT = 0xF;                    // KIRK initialization command.
    private static final int PSP_KIRK_CMD_ECDSA_SIGN = 0x10;             // ECDSA signing command.
    private static final int PSP_KIRK_CMD_ECDSA_VERIFY = 0x11;           // ECDSA checking command.
    private static final int PSP_KIRK_CMD_CERT_VERIFY = 0x12;            // Certificate checking command.

    // KIRK command modes.
    private static final int PSP_KIRK_CMD_MODE_CMD1 = 0x1;
    private static final int PSP_KIRK_CMD_MODE_CMD2 = 0x2;
    private static final int PSP_KIRK_CMD_MODE_CMD3 = 0x3;
    private static final int PSP_KIRK_CMD_MODE_ENCRYPT_CBC = 0x4;
    private static final int PSP_KIRK_CMD_MODE_DECRYPT_CBC = 0x5;

    // PRXDecrypter 16-byte tag keys.
    int[] keys260_0 = {0xC3, 0x24, 0x89, 0xD3, 0x80, 0x87, 0xB2, 0x4E, 0x4C, 0xD7, 0x49, 0xE4, 0x9D, 0x1D, 0x34, 0xD1};
    int[] keys260_1 = {0xF3, 0xAC, 0x6E, 0x7C, 0x04, 0x0A, 0x23, 0xE7, 0x0D, 0x33, 0xD8, 0x24, 0x73, 0x39, 0x2B, 0x4A};
    int[] keys260_2 = {0x72, 0xB4, 0x39, 0xFF, 0x34, 0x9B, 0xAE, 0x82, 0x30, 0x34, 0x4A, 0x1D, 0xA2, 0xD8, 0xB4, 0x3C};
    int[] keys280_0 = {0xCA, 0xFB, 0xBF, 0xC7, 0x50, 0xEA, 0xB4, 0x40, 0x8E, 0x44, 0x5C, 0x63, 0x53, 0xCE, 0x80, 0xB1};
    int[] keys280_1 = {0x40, 0x9B, 0xC6, 0x9B, 0xA9, 0xFB, 0x84, 0x7F, 0x72, 0x21, 0xD2, 0x36, 0x96, 0x55, 0x09, 0x74};
    int[] keys280_2 = {0x03, 0xA7, 0xCC, 0x4A, 0x5B, 0x91, 0xC2, 0x07, 0xFF, 0xFC, 0x26, 0x25, 0x1E, 0x42, 0x4B, 0xB5};
    int[] keys300_0 = {0x9F, 0x67, 0x1A, 0x7A, 0x22, 0xF3, 0x59, 0x0B, 0xAA, 0x6D, 0xA4, 0xC6, 0x8B, 0xD0, 0x03, 0x77};
    int[] keys300_1 = {0x15, 0x07, 0x63, 0x26, 0xDB, 0xE2, 0x69, 0x34, 0x56, 0x08, 0x2A, 0x93, 0x4E, 0x4B, 0x8A, 0xB2};
    int[] keys300_2 = {0x56, 0x3B, 0x69, 0xF7, 0x29, 0x88, 0x2F, 0x4C, 0xDB, 0xD5, 0xDE, 0x80, 0xC6, 0x5C, 0xC8, 0x73};
    int[] keys303_0 = {0x7b, 0xa1, 0xe2, 0x5a, 0x91, 0xb9, 0xd3, 0x13, 0x77, 0x65, 0x4a, 0xb7, 0xc2, 0x8a, 0x10, 0xaf};
    int[] keys310_0 = {0xa2, 0x41, 0xe8, 0x39, 0x66, 0x5b, 0xfa, 0xbb, 0x1b, 0x2d, 0x6e, 0x0e, 0x33, 0xe5, 0xd7, 0x3f};
    int[] keys310_1 = {0xA4, 0x60, 0x8F, 0xAB, 0xAB, 0xDE, 0xA5, 0x65, 0x5D, 0x43, 0x3A, 0xD1, 0x5E, 0xC3, 0xFF, 0xEA};
    int[] keys310_2 = {0xE7, 0x5C, 0x85, 0x7A, 0x59, 0xB4, 0xE3, 0x1D, 0xD0, 0x9E, 0xCE, 0xC2, 0xD6, 0xD4, 0xBD, 0x2B};
    int[] keys310_3 = {0x2E, 0x00, 0xF6, 0xF7, 0x52, 0xCF, 0x95, 0x5A, 0xA1, 0x26, 0xB4, 0x84, 0x9B, 0x58, 0x76, 0x2F};
    int[] keys330_0 = {0x3B, 0x9B, 0x1A, 0x56, 0x21, 0x80, 0x14, 0xED, 0x8E, 0x8B, 0x08, 0x42, 0xFA, 0x2C, 0xDC, 0x3A};
    int[] keys330_1 = {0xE8, 0xBE, 0x2F, 0x06, 0xB1, 0x05, 0x2A, 0xB9, 0x18, 0x18, 0x03, 0xE3, 0xEB, 0x64, 0x7D, 0x26};
    int[] keys330_2 = {0xAB, 0x82, 0x25, 0xD7, 0x43, 0x6F, 0x6C, 0xC1, 0x95, 0xC5, 0xF7, 0xF0, 0x63, 0x73, 0x3F, 0xE7};
    int[] keys330_3 = {0xA8, 0xB1, 0x47, 0x77, 0xDC, 0x49, 0x6A, 0x6F, 0x38, 0x4C, 0x4D, 0x96, 0xBD, 0x49, 0xEC, 0x9B};
    int[] keys330_4 = {0xEC, 0x3B, 0xD2, 0xC0, 0xFA, 0xC1, 0xEE, 0xB9, 0x9A, 0xBC, 0xFF, 0xA3, 0x89, 0xF2, 0x60, 0x1F};
    int[] keys360_0 = {0x3C, 0x2B, 0x51, 0xD4, 0x2D, 0x85, 0x47, 0xDA, 0x2D, 0xCA, 0x18, 0xDF, 0xFE, 0x54, 0x09, 0xED};
    int[] keys360_1 = {0x31, 0x1F, 0x98, 0xD5, 0x7B, 0x58, 0x95, 0x45, 0x32, 0xAB, 0x3A, 0xE3, 0x89, 0x32, 0x4B, 0x34};
    int[] keys370_0 = {0x26, 0x38, 0x0A, 0xAC, 0xA5, 0xD8, 0x74, 0xD1, 0x32, 0xB7, 0x2A, 0xBF, 0x79, 0x9E, 0x6D, 0xDB};
    int[] keys370_1 = {0x53, 0xE7, 0xAB, 0xB9, 0xC6, 0x4A, 0x4B, 0x77, 0x92, 0x17, 0xB5, 0x74, 0x0A, 0xDA, 0xA9, 0xEA};
    int[] keys370_2 = {0x71, 0x10, 0xF0, 0xA4, 0x16, 0x14, 0xD5, 0x93, 0x12, 0xFF, 0x74, 0x96, 0xDF, 0x1F, 0xDA, 0x89};
    int[] keys390_0 = {0x45, 0xEF, 0x5C, 0x5D, 0xED, 0x81, 0x99, 0x84, 0x12, 0x94, 0x8F, 0xAB, 0xE8, 0x05, 0x6D, 0x7D};
    int[] keys390_1 = {0x70, 0x1B, 0x08, 0x25, 0x22, 0xA1, 0x4D, 0x3B, 0x69, 0x21, 0xF9, 0x71, 0x0A, 0xA8, 0x41, 0xA9};
    int[] keys500_0 = {0xEB, 0x1B, 0x53, 0x0B, 0x62, 0x49, 0x32, 0x58, 0x1F, 0x83, 0x0A, 0xF4, 0x99, 0x3D, 0x75, 0xD0};
    int[] keys500_1 = {0xBA, 0xE2, 0xA3, 0x12, 0x07, 0xFF, 0x04, 0x1B, 0x64, 0xA5, 0x11, 0x85, 0xF7, 0x2F, 0x99, 0x5B};
    int[] keys500_2 = {0x2C, 0x8E, 0xAF, 0x1D, 0xFF, 0x79, 0x73, 0x1A, 0xAD, 0x96, 0xAB, 0x09, 0xEA, 0x35, 0x59, 0x8B};
    int[] keys500_c = {0xA3, 0x5D, 0x51, 0xE6, 0x56, 0xC8, 0x01, 0xCA, 0xE3, 0x77, 0xBF, 0xCD, 0xFF, 0x24, 0xDA, 0x4D};
    int[] keys505_a = {0x7B, 0x94, 0x72, 0x27, 0x4C, 0xCC, 0x54, 0x3B, 0xAE, 0xDF, 0x46, 0x37, 0xAC, 0x01, 0x4D, 0x87};
    int[] keys505_0 = {0x2E, 0x8E, 0x97, 0xA2, 0x85, 0x42, 0x70, 0x73, 0x18, 0xDA, 0xA0, 0x8A, 0xF8, 0x62, 0xA2, 0xB0};
    int[] keys505_1 = {0x58, 0x2A, 0x4C, 0x69, 0x19, 0x7B, 0x83, 0x3D, 0xD2, 0x61, 0x61, 0xFE, 0x14, 0xEE, 0xAA, 0x11};
    int[] keys570_5k = {0x6D, 0x72, 0xA4, 0xBA, 0x7F, 0xBF, 0xD1, 0xF1, 0xA9, 0xF3, 0xBB, 0x07, 0x1B, 0xC0, 0xB3, 0x66};
    int[] keys600_1 = {0xE3, 0x52, 0x39, 0x97, 0x3B, 0x84, 0x41, 0x1C, 0xC3, 0x23, 0xF1, 0xB8, 0xA9, 0x09, 0x4B, 0xF0};
    int[] keys600_2 = {0xE1, 0x45, 0x93, 0x2C, 0x53, 0xE2, 0xAB, 0x06, 0x6F, 0xB6, 0x8F, 0x0B, 0x66, 0x91, 0xE7, 0x1E};
    int[] keys620_0 = {0xD6, 0xBD, 0xCE, 0x1E, 0x12, 0xAF, 0x9A, 0xE6, 0x69, 0x30, 0xDE, 0xDA, 0x88, 0xB8, 0xFF, 0xFB};
    int[] keys620_1 = {0x1D, 0x13, 0xE9, 0x50, 0x04, 0x73, 0x3D, 0xD2, 0xE1, 0xDA, 0xB9, 0xC1, 0xE6, 0x7B, 0x25, 0xA7};
    int[] keys620_a = {0xAC, 0x34, 0xBA, 0xB1, 0x97, 0x8D, 0xAE, 0x6F, 0xBA, 0xE8, 0xB1, 0xD6, 0xDF, 0xDF, 0xF1, 0xA2};
    int[] keys620_e = {0xB1, 0xB3, 0x7F, 0x76, 0xC3, 0xFB, 0x88, 0xE6, 0xF8, 0x60, 0xD3, 0x35, 0x3C, 0xA3, 0x4E, 0xF3};
    int[] keys620_5 = {0xF1, 0xBC, 0x17, 0x07, 0xAE, 0xB7, 0xC8, 0x30, 0xD8, 0x34, 0x9D, 0x40, 0x6A, 0x8E, 0xDF, 0x4E};
    int[] keys620_5k = {0x41, 0x8A, 0x35, 0x4F, 0x69, 0x3A, 0xDF, 0x04, 0xFD, 0x39, 0x46, 0xA2, 0x5C, 0x2D, 0xF2, 0x21};
    int[] keys620_5v = {0xF2, 0x8F, 0x75, 0xA7, 0x31, 0x91, 0xCE, 0x9E, 0x75, 0xBD, 0x27, 0x26, 0xB4, 0xB4, 0x0C, 0x32};
    int[] keys630_k1 = {0x36, 0xB0, 0xDC, 0xFC, 0x59, 0x2A, 0x95, 0x1D, 0x80, 0x2D, 0x80, 0x3F, 0xCD, 0x30, 0xA0, 0x1B};
    int[] keys630_k2 = {0xd4, 0x35, 0x18, 0x02, 0x29, 0x68, 0xfb, 0xa0, 0x6a, 0xa9, 0xa5, 0xed, 0x78, 0xfd, 0x2e, 0x9d};
    int[] keys630_k3 = {0x23, 0x8D, 0x3D, 0xAE, 0x41, 0x50, 0xA0, 0xFA, 0xF3, 0x2F, 0x32, 0xCE, 0xC7, 0x27, 0xCD, 0x50};
    int[] keys630_k4 = {0xAA, 0xA1, 0xB5, 0x7C, 0x93, 0x5A, 0x95, 0xBD, 0xEF, 0x69, 0x16, 0xFC, 0x2B, 0x92, 0x31, 0xDD};
    int[] keys630_k5 = {0x87, 0x37, 0x21, 0xCC, 0x65, 0xAE, 0xAA, 0x5F, 0x40, 0xF6, 0x6F, 0x2A, 0x86, 0xC7, 0xA1, 0xC8};
    int[] keys630_k6 = {0x8D, 0xDB, 0xDC, 0x5C, 0xF2, 0x70, 0x2B, 0x40, 0xB2, 0x3D, 0x00, 0x09, 0x61, 0x7C, 0x10, 0x60};
    int[] keys630_k7 = {0x77, 0x1C, 0x06, 0x5F, 0x53, 0xEC, 0x3F, 0xFC, 0x22, 0xCE, 0x5A, 0x27, 0xFF, 0x78, 0xA8, 0x48};
    int[] keys630_k8 = {0x81, 0xD1, 0x12, 0x89, 0x35, 0xC8, 0xEA, 0x8B, 0xE0, 0x02, 0x2D, 0x2D, 0x6A, 0x18, 0x67, 0xB8};
    int[] keys636_k1 = {0x07, 0xE3, 0x08, 0x64, 0x7F, 0x60, 0xA3, 0x36, 0x6A, 0x76, 0x21, 0x44, 0xC9, 0xD7, 0x06, 0x83};
    int[] keys636_k2 = {0x91, 0xF2, 0x02, 0x9E, 0x63, 0x32, 0x30, 0xA9, 0x1D, 0xDA, 0x0B, 0xA8, 0xB7, 0x41, 0xA3, 0xCC};
    int[] keys638_k4 = {0x98, 0x43, 0xFF, 0x85, 0x68, 0xB2, 0xDB, 0x3B, 0xD4, 0x22, 0xD0, 0x4F, 0xAB, 0x5F, 0x0A, 0x31};
    int[] keys639_k3 = {0x01, 0x7B, 0xF0, 0xE9, 0xBE, 0x9A, 0xDD, 0x54, 0x37, 0xEA, 0x0E, 0xC4, 0xD6, 0x4D, 0x8E, 0x9E};    
    int[] keys660_k1 = {0x76, 0xF2, 0x6C, 0x0A, 0xCA, 0x3A, 0xBA, 0x4E, 0xAC, 0x76, 0xD2, 0x40, 0xF5, 0xC3, 0xBF, 0xF9};
    int[] keys660_k2 = {0x7A, 0x3E, 0x55, 0x75, 0xB9, 0x6A, 0xFC, 0x4F, 0x3E, 0xE3, 0xDF, 0xB3, 0x6C, 0xE8, 0x2A, 0x82};
    int[] keys660_k3 = {0xFA, 0x79, 0x09, 0x36, 0xE6, 0x19, 0xE8, 0xA4, 0xA9, 0x41, 0x37, 0x18, 0x81, 0x02, 0xE9, 0xB3};
    int[] keys660_v1 = {0xBA, 0x76, 0x61, 0x47, 0x8B, 0x55, 0xA8, 0x72, 0x89, 0x15, 0x79, 0x6D, 0xD7, 0x2F, 0x78, 0x0E};
    int[] keys660_v2 = {0xF9, 0x4A, 0x6B, 0x96, 0x79, 0x3F, 0xEE, 0x0A, 0x04, 0xC8, 0x8D, 0x7E, 0x5F, 0x38, 0x3A, 0xCF};
    int[] keys660_v3 = {0x88, 0xAF, 0x18, 0xE9, 0xC3, 0xAA, 0x6B, 0x56, 0xF7, 0xC5, 0xA8, 0xBF, 0x1A, 0x84, 0xE9, 0xF3};
    int[] keys660_v4 = {0xD1, 0xB0, 0xAE, 0xC3, 0x24, 0x36, 0x13, 0x49, 0xD6, 0x49, 0xD7, 0x88, 0xEA, 0xA4, 0x99, 0x86};
    int[] keys660_v5 = {0xCB, 0x93, 0x12, 0x38, 0x31, 0xC0, 0x2D, 0x2E, 0x7A, 0x18, 0x5C, 0xAC, 0x92, 0x93, 0xAB, 0x32};
    int[] keys660_v6 = {0x92, 0x8C, 0xA4, 0x12, 0xD6, 0x5C, 0x55, 0x31, 0x5B, 0x94, 0x23, 0x9B, 0x62, 0xB3, 0xDB, 0x47};
    int[] keys660_k4 = {0xC8, 0xA0, 0x70, 0x98, 0xAE, 0xE6, 0x2B, 0x80, 0xD7, 0x91, 0xE6, 0xCA, 0x4C, 0xA9, 0x78, 0x4E};
    int[] keys660_k5 = {0xBF, 0xF8, 0x34, 0x02, 0x84, 0x47, 0xBD, 0x87, 0x1C, 0x52, 0x03, 0x23, 0x79, 0xBB, 0x59, 0x81};
    int[] keys660_k6 = {0xD2, 0x83, 0xCC, 0x63, 0xBB, 0x10, 0x15, 0xE7, 0x7B, 0xC0, 0x6D, 0xEE, 0x34, 0x9E, 0x4A, 0xFA};
    int[] keys660_k7 = {0xEB, 0xD9, 0x1E, 0x05, 0x3C, 0xAE, 0xAB, 0x62, 0xE3, 0xB7, 0x1F, 0x37, 0xE5, 0xCD, 0x68, 0xC3};
    int[] keys660_v7 = {0xC5, 0x9C, 0x77, 0x9C, 0x41, 0x01, 0xE4, 0x85, 0x79, 0xC8, 0x71, 0x63, 0xA5, 0x7D, 0x4F, 0xFB};
    int[] keys660_v8 = {0x86, 0xA0, 0x7D, 0x4D, 0xB3, 0x6B, 0xA2, 0xFD, 0xF4, 0x15, 0x85, 0x70, 0x2D, 0x6A, 0x0D, 0x3A};
    int[] keys660_k8 = {0x85, 0x93, 0x1F, 0xED, 0x2C, 0x4D, 0xA4, 0x53, 0x59, 0x9C, 0x3F, 0x16, 0xF3, 0x50, 0xDE, 0x46};    
    int[] key_21C0 = {0x6A, 0x19, 0x71, 0xF3, 0x18, 0xDE, 0xD3, 0xA2, 0x6D, 0x3B, 0xDE, 0xC7, 0xBE, 0x98, 0xE2, 0x4C};
    int[] key_2250 = {0x50, 0xCC, 0x03, 0xAC, 0x3F, 0x53, 0x1A, 0xFA, 0x0A, 0xA4, 0x34, 0x23, 0x86, 0x61, 0x7F, 0x97};
    int[] key_22E0 = {0x66, 0x0F, 0xCB, 0x3B, 0x30, 0x75, 0xE3, 0x10, 0x0A, 0x95, 0x65, 0xC7, 0x3C, 0x93, 0x87, 0x22};
    int[] key_2D80 = {0x40, 0x02, 0xC0, 0xBF, 0x20, 0x02, 0xC0, 0xBF, 0x5C, 0x68, 0x2B, 0x95, 0x5F, 0x40, 0x7B, 0xB8};
    int[] key_2D90 = {0x55, 0x19, 0x35, 0x10, 0x48, 0xD8, 0x2E, 0x46, 0xA8, 0xB1, 0x47, 0x77, 0xDC, 0x49, 0x6A, 0x6F};
    int[] key_2DA8 = {0x80, 0x02, 0xC0, 0xBF, 0x00, 0x0A, 0xC0, 0xBF, 0x40, 0x03, 0xC0, 0xBF, 0x40, 0x00, 0x00, 0x00};
    int[] key_2DB8 = {0x4C, 0x2D, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0x00, 0xB8, 0x15, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    int[] key_D91605F0 = {0xB8, 0x8C, 0x45, 0x8B, 0xB6, 0xE7, 0x6E, 0xB8, 0x51, 0x59, 0xA6, 0x53, 0x7C, 0x5E, 0x86, 0x31};
    int[] key_D91606F0 = {0xED, 0x10, 0xE0, 0x36, 0xC4, 0xFE, 0x83, 0xF3, 0x75, 0x70, 0x5E, 0xF6, 0xA4, 0x40, 0x05, 0xF7};
    int[] key_D91608F0 = {0x5C, 0x77, 0x0C, 0xBB, 0xB4, 0xC2, 0x4F, 0xA2, 0x7E, 0x3B, 0x4E, 0xB4, 0xB4, 0xC8, 0x70, 0xAF};
    int[] key_D91609F0 = {0xD0, 0x36, 0x12, 0x75, 0x80, 0x56, 0x20, 0x43, 0xC4, 0x30, 0x94, 0x3E, 0x1C, 0x75, 0xD1, 0xBF};
    int[] key_D9160AF0 = {0x10, 0xA9, 0xAC, 0x16, 0xAE, 0x19, 0xC0, 0x7E, 0x3B, 0x60, 0x77, 0x86, 0x01, 0x6F, 0xF2, 0x63};
    int[] key_D9160BF0 = {0x83, 0x83, 0xF1, 0x37, 0x53, 0xD0, 0xBE, 0xFC, 0x8D, 0xA7, 0x32, 0x52, 0x46, 0x0A, 0xC2, 0xC2};
    int[] key_D91611F0 = {0x61, 0xB0, 0xC0, 0x58, 0x71, 0x57, 0xD9, 0xFA, 0x74, 0x67, 0x0E, 0x5C, 0x7E, 0x6E, 0x95, 0xB9};
    int[] key_D91612F0 = {0x9E, 0x20, 0xE1, 0xCD, 0xD7, 0x88, 0xDE, 0xC0, 0x31, 0x9B, 0x10, 0xAF, 0xC5, 0xB8, 0x73, 0x23};
    int[] key_D91613F0 = {0xEB, 0xFF, 0x40, 0xD8, 0xB4, 0x1A, 0xE1, 0x66, 0x91, 0x3B, 0x8F, 0x64, 0xB6, 0xFC, 0xB7, 0x12};
    int[] key_D91614F0 = {0xFD, 0xF7, 0xB7, 0x3C, 0x9F, 0xD1, 0x33, 0x95, 0x11, 0xB8, 0xB5, 0xBB, 0x54, 0x23, 0x73, 0x85};
    int[] key_D91615F0 = {0xC8, 0x03, 0xE3, 0x44, 0x50, 0xF1, 0xE7, 0x2A, 0x6A, 0x0D, 0xC3, 0x61, 0xB6, 0x8E, 0x5F, 0x51};
    int[] key_D91616F0 = {0x53, 0x03, 0xB8, 0x6A, 0x10, 0x19, 0x98, 0x49, 0x1C, 0xAF, 0x30, 0xE4, 0x25, 0x1B, 0x6B, 0x28};
    int[] key_D91617F0 = {0x02, 0xFA, 0x48, 0x73, 0x75, 0xAF, 0xAE, 0x0A, 0x67, 0x89, 0x2B, 0x95, 0x4B, 0x09, 0x87, 0xA3};
    int[] key_D91618F0 = {0x96, 0x96, 0x7C, 0xC3, 0xF7, 0x12, 0xDA, 0x62, 0x1B, 0xF6, 0x9A, 0x9A, 0x44, 0x44, 0xBC, 0x48};
    int[] key_D91619F0 = {0xE0, 0x32, 0xA7, 0x08, 0x6B, 0x2B, 0x29, 0x2C, 0xD1, 0x4D, 0x5B, 0xEE, 0xA8, 0xC8, 0xB4, 0xE9};
    int[] key_D9161AF0 = {0x27, 0xE5, 0xA7, 0x49, 0x52, 0xE1, 0x94, 0x67, 0x35, 0x66, 0x91, 0x0C, 0xE8, 0x9A, 0x25, 0x24};
    int[] key_D91620F0 = {0x52, 0x1C, 0xB4, 0x5F, 0x40, 0x3B, 0x9A, 0xDD, 0xAC, 0xFC, 0xEA, 0x92, 0xFD, 0xDD, 0xF5, 0x90};
    int[] key_D91621F0 = {0xD1, 0x91, 0x2E, 0xA6, 0x21, 0x14, 0x29, 0x62, 0xF6, 0xED, 0xAE, 0xCB, 0xDD, 0xA3, 0xBA, 0xFE};
    int[] key_D91622F0 = {0x59, 0x5D, 0x78, 0x4D, 0x21, 0xB2, 0x01, 0x17, 0x6C, 0x9A, 0xB5, 0x1B, 0xDA, 0xB7, 0xF9, 0xE6};
    int[] key_D91623F0 = {0xAA, 0x45, 0xEB, 0x4F, 0x62, 0xFB, 0xD1, 0x0D, 0x71, 0xD5, 0x62, 0xD2, 0xF5, 0xBF, 0xA5, 0x2F};
    int[] key_D91624F0 = {0x61, 0xB7, 0x26, 0xAF, 0x8B, 0xF1, 0x41, 0x58, 0x83, 0x6A, 0xC4, 0x92, 0x12, 0xCB, 0xB1, 0xE9};
    int[] key_D91628F0 = {0x49, 0xA4, 0xFC, 0x66, 0xDC, 0xE7, 0x62, 0x21, 0xDB, 0x18, 0xA7, 0x50, 0xD6, 0xA8, 0xC1, 0xB6};
    int[] key_D91680F0 = {0x2C, 0x22, 0x9B, 0x12, 0x36, 0x74, 0x11, 0x67, 0x49, 0xD1, 0xD1, 0x88, 0x92, 0xF6, 0xA1, 0xD8};
    int[] key_D91681F0 = {0x52, 0xB6, 0x36, 0x6C, 0x8C, 0x46, 0x7F, 0x7A, 0xCC, 0x11, 0x62, 0x99, 0xC1, 0x99, 0xBE, 0x98};
    int[] key_2E5E10F0 = {0x9D, 0x5C, 0x5B, 0xAF, 0x8C, 0xD8, 0x69, 0x7E, 0x51, 0x9F, 0x70, 0x96, 0xE6, 0xD5, 0xC4, 0xE8};
    int[] key_2E5E12F0 = {0x8A, 0x7B, 0xC9, 0xD6, 0x52, 0x58, 0x88, 0xEA, 0x51, 0x83, 0x60, 0xCA, 0x16, 0x79, 0xE2, 0x07};
    int[] key_2E5E13F0 = {0xFF, 0xA4, 0x68, 0xC3, 0x31, 0xCA, 0xB7, 0x4C, 0xF1, 0x23, 0xFF, 0x01, 0x65, 0x3D, 0x26, 0x36};
    int[] key_2FD30BF0 = {0xD8, 0x58, 0x79, 0xF9, 0xA4, 0x22, 0xAF, 0x86, 0x90, 0xAC, 0xDA, 0x45, 0xCE, 0x60, 0x40, 0x3F};
    int[] key_2FD311F0 = {0x3A, 0x6B, 0x48, 0x96, 0x86, 0xA5, 0xC8, 0x80, 0x69, 0x6C, 0xE6, 0x4B, 0xF6, 0x04, 0x17, 0x44};
    int[] key_2FD312F0 = {0xC5, 0xFB, 0x69, 0x03, 0x20, 0x7A, 0xCF, 0xBA, 0x2C, 0x90, 0xF8, 0xB8, 0x4D, 0xD2, 0xF1, 0xDE};
    int[] keys02G_E = {0x9D, 0x09, 0xFD, 0x20, 0xF3, 0x8F, 0x10, 0x69, 0x0D, 0xB2, 0x6F, 0x00, 0xCC, 0xC5, 0x51, 0x2E};
    int[] keys03G_E = {0x4F, 0x44, 0x5C, 0x62, 0xB3, 0x53, 0xC4, 0x30, 0xFC, 0x3A, 0xA4, 0x5B, 0xEC, 0xFE, 0x51, 0xEA};
    int[] keys05G_E = {0x5D, 0xAA, 0x72, 0xF2, 0x26, 0x60, 0x4D, 0x1C, 0xE7, 0x2D, 0xC8, 0xA3, 0x2F, 0x79, 0xC5, 0x54};
    int[] oneseg_310 = {0xC7, 0x27, 0x72, 0x85, 0xAB, 0xA7, 0xF7, 0xF0, 0x4C, 0xC1, 0x86, 0xCC, 0xE3, 0x7F, 0x17, 0xCA};
    int[] oneseg_300 = {0x76, 0x40, 0x9E, 0x08, 0xDB, 0x9B, 0x3B, 0xA1, 0x47, 0x8A, 0x96, 0x8E, 0xF3, 0xF7, 0x62, 0x92};
    int[] oneseg_280 = {0x23, 0xDC, 0x3B, 0xB5, 0xA9, 0x82, 0xD6, 0xEA, 0x63, 0xA3, 0x6E, 0x2B, 0x2B, 0xE9, 0xE1, 0x54};
    int[] oneseg_260_271 = {0x22, 0x43, 0x57, 0x68, 0x2F, 0x41, 0xCE, 0x65, 0x4C, 0xA3, 0x7C, 0xC6, 0xC4, 0xAC, 0xF3, 0x60};
    int[] oneseg_slim = {0x12, 0x57, 0x0D, 0x8A, 0x16, 0x6D, 0x87, 0x06, 0x03, 0x7D, 0xC8, 0x8B, 0x62, 0xA3, 0x32, 0xA9};
    int[] ms_app_main = {0x1E, 0x2E, 0x38, 0x49, 0xDA, 0xD4, 0x16, 0x08, 0x27, 0x2E, 0xF3, 0xBC, 0x37, 0x75, 0x80, 0x93};
    int[] demokeys_280 = {0x12, 0x99, 0x70, 0x5E, 0x24, 0x07, 0x6C, 0xD0, 0x2D, 0x06, 0xFE, 0x7E, 0xB3, 0x0C, 0x11, 0x26};
    int[] demokeys_3XX_1 = {0x47, 0x05, 0xD5, 0xE3, 0x56, 0x1E, 0x81, 0x9B, 0x09, 0x2F, 0x06, 0xDB, 0x6B, 0x12, 0x92, 0xE0};
    int[] demokeys_3XX_2 = {0xF6, 0x62, 0x39, 0x6E, 0x26, 0x22, 0x4D, 0xCA, 0x02, 0x64, 0x16, 0x99, 0x7B, 0x9A, 0xE7, 0xB8};
    int[] ebootbin_271_new = {0xF4, 0xAE, 0xF4, 0xE1, 0x86, 0xDD, 0xD2, 0x9C, 0x7C, 0xC5, 0x42, 0xA6, 0x95, 0xA0, 0x83, 0x88};
    int[] ebootbin_280_new = {0xB8, 0x8C, 0x45, 0x8B, 0xB6, 0xE7, 0x6E, 0xB8, 0x51, 0x59, 0xA6, 0x53, 0x7C, 0x5E, 0x86, 0x31};
    int[] ebootbin_300_new = {0xED, 0x10, 0xE0, 0x36, 0xC4, 0xFE, 0x83, 0xF3, 0x75, 0x70, 0x5E, 0xF6, 0xA4, 0x40, 0x05, 0xF7};
    int[] ebootbin_310_new = {0x5C, 0x77, 0x0C, 0xBB, 0xB4, 0xC2, 0x4F, 0xA2, 0x7E, 0x3B, 0x4E, 0xB4, 0xB4, 0xC8, 0x70, 0xAF};
    int[] gameshare_260_271 = {0xF9, 0x48, 0x38, 0x0C, 0x96, 0x88, 0xA7, 0x74, 0x4F, 0x65, 0xA0, 0x54, 0xC2, 0x76, 0xD9, 0xB8};
    int[] gameshare_280 = {0x2D, 0x86, 0x77, 0x3A, 0x56, 0xA4, 0x4F, 0xDD, 0x3C, 0x16, 0x71, 0x93, 0xAA, 0x8E, 0x11, 0x43};
    int[] gameshare_300 = {0x78, 0x1A, 0xD2, 0x87, 0x24, 0xBD, 0xA2, 0x96, 0x18, 0x3F, 0x89, 0x36, 0x72, 0x90, 0x92, 0x85};
    int[] gameshare_310 = {0xC9, 0x7D, 0x3E, 0x0A, 0x54, 0x81, 0x6E, 0xC7, 0x13, 0x74, 0x99, 0x74, 0x62, 0x18, 0xE7, 0xDD};
    int[] key_380210F0 = {0x32, 0x2C, 0xFA, 0x75, 0xE4, 0x7E, 0x93, 0xEB, 0x9F, 0x22, 0x80, 0x85, 0x57, 0x08, 0x98, 0x48};
    int[] key_380280F0 = {0x97, 0x09, 0x12, 0xD3, 0xDB, 0x02, 0xBD, 0xD8, 0xE7, 0x74, 0x51, 0xFE, 0xF0, 0xEA, 0x6C, 0x5C};
    int[] key_380283F0 = {0x34, 0x20, 0x0C, 0x8E, 0xA1, 0x86, 0x79, 0x84, 0xAF, 0x13, 0xAE, 0x34, 0x77, 0x6F, 0xEA, 0x89};
    int[] key_407810F0 = {0xAF, 0xAD, 0xCA, 0xF1, 0x95, 0x59, 0x91, 0xEC, 0x1B, 0x27, 0xD0, 0x4E, 0x8A, 0xF3, 0x3D, 0xE7};
    int[] drmkeys_6XX_1 = {0x36, 0xEF, 0x82, 0x4E, 0x74, 0xFB, 0x17, 0x5B, 0x14, 0x14, 0x05, 0xF3, 0xB3, 0x8A, 0x76, 0x18};
    int[] drmkeys_6XX_2 = {0x21, 0x52, 0x5D, 0x76, 0xF6, 0x81, 0x0F, 0x15, 0x2F, 0x4A, 0x40, 0x89, 0x63, 0xA0, 0x10, 0x55};

    // PRXDecrypter 144-byte tag keys.
    int g_key0[] = {
        0x7b21f3be, 0x299c5e1d, 0x1c9c5e71, 0x96cb4645, 0x3c9b1be0, 0xeb85de3d,
        0x4a7f2022, 0xc2206eaa, 0xd50b3265, 0x55770567, 0x3c080840, 0x981d55f2,
        0x5fd8f6f3, 0xee8eb0c5, 0x944d8152, 0xf8278651, 0x2705bafa, 0x8420e533,
        0x27154ae9, 0x4819aa32, 0x59a3aa40, 0x2cb3cf65, 0xf274466d, 0x3a655605,
        0x21b0f88f, 0xc5b18d26, 0x64c19051, 0xd669c94e, 0xe87035f2, 0x9d3a5909,
        0x6f4e7102, 0xdca946ce, 0x8416881b, 0xbab097a5, 0x249125c6, 0xb34c0872};
    int g_key2[] = {
        0xccfda932, 0x51c06f76, 0x046dcccf, 0x49e1821e, 0x7d3b024c, 0x9dda5865,
        0xcc8c9825, 0xd1e97db5, 0x6874d8cb, 0x3471c987, 0x72edb3fc, 0x81c8365d,
        0xe161e33a, 0xfc92db59, 0x2009b1ec, 0xb1a94ce4, 0x2f03696b, 0x87e236d8,
        0x3b2b8ce9, 0x0305e784, 0xf9710883, 0xb039db39, 0x893bea37, 0xe74d6805,
        0x2a5c38bd, 0xb08dc813, 0x15b32375, 0x46be4525, 0x0103fd90, 0xa90e87a2,
        0x52aba66a, 0x85bf7b80, 0x45e8ce63, 0x4dd716d3, 0xf5e30d2d, 0xaf3ae456};
    int g_key3[] = {
        0xa6c8f5ca, 0x6d67c080, 0x924f4d3a, 0x047ca06a, 0x08640297, 0x4fd4a758,
        0xbd685a87, 0x9b2701c2, 0x83b62a35, 0x726b533c, 0xe522fa0c, 0xc24b06b4,
        0x459d1cac, 0xa8c5417b, 0x4fea62a2, 0x0615d742, 0x30628d09, 0xc44fab14,
        0x69ff715e, 0xd2d8837d, 0xbeed0b8b, 0x1e6e57ae, 0x61e8c402, 0xbe367a06,
        0x543f2b5e, 0xdb3ec058, 0xbe852075, 0x1e7e4dcc, 0x1564ea55, 0xec7825b4,
        0xc0538cad, 0x70f72c7f, 0x49e8c3d0, 0xeda97ec5, 0xf492b0a4, 0xe05eb02a};
    int g_key44[] = {
        0xef80e005, 0x3a54689f, 0x43c99ccd, 0x1b7727be, 0x5cb80038, 0xdd2efe62,
        0xf369f92c, 0x160f94c5, 0x29560019, 0xbf3c10c5, 0xf2ce5566, 0xcea2c626,
        0xb601816f, 0x64e7481e, 0x0c34debd, 0x98f29cb0, 0x3fc504d7, 0xc8fb39f0,
        0x0221b3d8, 0x63f936a2, 0x9a3a4800, 0x6ecc32e3, 0x8e120cfd, 0xb0361623,
        0xaee1e689, 0x745502eb, 0xe4a6c61c, 0x74f23eb4, 0xd7fa5813, 0xb01916eb,
        0x12328457, 0xd2bc97d2, 0x646425d8, 0x328380a5, 0x43da8ab1, 0x4b122ac9};
    int g_key20[] = {
        0x33b50800, 0xf32f5fcd, 0x3c14881f, 0x6e8a2a95, 0x29feefd5, 0x1394eae3,
        0xbd6bd443, 0x0821c083, 0xfab379d3, 0xe613e165, 0xf5a754d3, 0x108b2952,
        0x0a4b1e15, 0x61eadeba, 0x557565df, 0x3b465301, 0xae54ecc3, 0x61423309,
        0x70c9ff19, 0x5b0ae5ec, 0x989df126, 0x9d987a5f, 0x55bc750e, 0xc66eba27,
        0x2de988e8, 0xf76600da, 0x0382dccb, 0x5569f5f2, 0x8e431262, 0x288fe3d3,
        0x656f2187, 0x37d12e9c, 0x2f539eb4, 0xa492998e, 0xed3958f7, 0x39e96523};
    int g_key3A[] = {
        0x67877069, 0x3abd5617, 0xc23ab1dc, 0xab57507d, 0x066a7f40, 0x24def9b9,
        0x06f759e4, 0xdcf524b1, 0x13793e5e, 0x0359022d, 0xaae7e1a2, 0x76b9b2fa,
        0x9a160340, 0x87822fba, 0x19e28fbb, 0x9e338a02, 0xd8007e9a, 0xea317af1,
        0x630671de, 0x0b67ca7c, 0x865192af, 0xea3c3526, 0x2b448c8e, 0x8b599254,
        0x4602e9cb, 0x4de16cda, 0xe164d5bb, 0x07ecd88e, 0x99ffe5f8, 0x768800c1,
        0x53b091ed, 0x84047434, 0xb426dbbc, 0x36f948bb, 0x46142158, 0x749bb492};
    int g_keyEBOOT1xx[] = {
        0x18CB69EF, 0x158E8912, 0xDEF90EBB, 0x4CB0FB23, 0x3687EE18, 0x868D4A6E,
        0x19B5C756, 0xEE16551D, 0xE7CB2D6C, 0x9747C660, 0xCE95143F, 0x2956F477,
        0x03824ADE, 0x210C9DF1, 0x5029EB24, 0x81DFE69F, 0x39C89B00, 0xB00C8B91,
        0xEF2DF9C2, 0xE13A93FC, 0x8B94A4A8, 0x491DD09D, 0x686A400D, 0xCED4C7E4,
        0x96C8B7C9, 0x1EAADC28, 0xA4170B84, 0x505D5DDC, 0x5DA6C3CF, 0x0E5DFA2D,
        0x6E7919B5, 0xCE5E29C7, 0xAAACDB94, 0x45F70CDD, 0x62A73725, 0xCCE6563D};
    int g_keyEBOOT2xx[] = {
        0xDA8E36FA, 0x5DD97447, 0x76C19874, 0x97E57EAF, 0x1CAB09BD, 0x9835BAC6,
        0x03D39281, 0x03B205CF, 0x2882E734, 0xE714F663, 0xB96E2775, 0xBD8AAFC7,
        0x1DD3EC29, 0xECA4A16C, 0x5F69EC87, 0x85981E92, 0x7CFCAE21, 0xBAE9DD16,
        0xE6A97804, 0x2EEE02FC, 0x61DF8A3D, 0xDD310564, 0x9697E149, 0xC2453F3B,
        0xF91D8456, 0x39DA6BC8, 0xB3E5FEF5, 0x89C593A3, 0xFB5C8ABC, 0x6C0B7212,
        0xE10DD3CB, 0x98D0B2A8, 0x5FD61847, 0xF0DC2357, 0x7701166A, 0x0F5C3B68};
    int g_keyUPDATER[] = {
        0xA5603CBF, 0xD7482441, 0xF65764CC, 0x1F90060B, 0x4EA73E45, 0xE551D192,
        0xE7B75D8A, 0x465A506E, 0x40FB1022, 0x2C273350, 0x8096DA44, 0x9947198E,
        0x278DEE77, 0x745D062E, 0xC148FA45, 0x832582AF, 0x5FDB86DA, 0xCB15C4CE,
        0x2524C62F, 0x6C2EC3B1, 0x369BE39E, 0xF7EB1FC4, 0x1E51CE1A, 0xD70536F4,
        0xC34D39D8, 0x7418FB13, 0xE3C84DE1, 0xB118F03C, 0xA2018D4E, 0xE6D8770D,
        0x5720F390, 0x17F96341, 0x60A4A68F, 0x1327DD28, 0x05944C64, 0x0C2C4C12};
    int g_keyMEIMG250[] = {
        0xA381FEBC, 0x99B9D5C9, 0x6C560A8D, 0x30309F95, 0x792646CC, 0x82B64E5E,
        0x1A3951AD, 0x0A182EC4, 0xC46131B4, 0x77C50C8A, 0x325F16C6, 0x02D1942E,
        0x0AA38AC4, 0x2A940AC6, 0x67034726, 0xE52DB133, 0xD2EF2107, 0x85C81E90,
        0xC8D164BA, 0xC38DCE1D, 0x948BA275, 0x0DB84603, 0xE2473637, 0xCD74FCDA,
        0x588E3D66, 0x6D28E822, 0x891E548B, 0xF53CF56D, 0x0BBDDB66, 0xC4B286AA,
        0x2BEBBC4B, 0xFC261FF4, 0x92B8E705, 0xDCEE6952, 0x5E0442E5, 0x8BEB7F21};
    int g_keyMEIMG260[] = {
        0x11BFD698, 0xD7F9B324, 0xDD524927, 0x16215B86, 0x504AC36D, 0x5843B217,
        0xE5A0DA47, 0xBB73A1E7, 0x2915DB35, 0x375CFD3A, 0xBB70A905, 0x272BEFCA,
        0x2E960791, 0xEA0799BB, 0xB85AE6C8, 0xC9CAF773, 0x250EE641, 0x06E74A9E,
        0x5244895D, 0x466755A5, 0x9A84AF53, 0xE1024174, 0xEEBA031E, 0xED80B9CE,
        0xBC315F72, 0x5821067F, 0xE8313058, 0xD2D0E706, 0xE6D8933E, 0xD7D17FB4,
        0x505096C4, 0xFDA50B3B, 0x4635AE3D, 0xEB489C8A, 0x422D762D, 0x5A8B3231};
    int g_keyDEMOS27X[] = {
        0x1ABF102F, 0xD596D071, 0x6FC552B2, 0xD4F2531F, 0xF025CDD9, 0xAF9AAF03,
        0xE0CF57CF, 0x255494C4, 0x7003675E, 0x907BC884, 0x002D4EE4, 0x0B687A0D,
        0x9E3AA44F, 0xF58FDA81, 0xEC26AC8C, 0x3AC9B49D, 0x3471C037, 0xB0F3834D,
        0x10DC4411, 0xA232EA31, 0xE2E5FA6B, 0x45594B03, 0xE43A1C87, 0x31DAD9D1,
        0x08CD7003, 0xFA9C2FDF, 0x5A891D25, 0x9B5C1934, 0x22F366E5, 0x5F084A32,
        0x695516D5, 0x2245BE9F, 0x4F6DD705, 0xC4B8B8A1, 0xBC13A600, 0x77B7FC3B};
    int g_keyUNK1[] = {
        0x33B50800, 0xF32F5FCD, 0x3C14881F, 0x6E8A2A95, 0x29FEEFD5, 0x1394EAE3,
        0xBD6BD443, 0x0821C083, 0xFAB379D3, 0xE613E165, 0xF5A754D3, 0x108B2952,
        0x0A4B1E15, 0x61EADEBA, 0x557565DF, 0x3B465301, 0xAE54ECC3, 0x61423309,
        0x70C9FF19, 0x5B0AE5EC, 0x989DF126, 0x9D987A5F, 0x55BC750E, 0xC66EBA27,
        0x2DE988E8, 0xF76600DA, 0x0382DCCB, 0x5569F5F2, 0x8E431262, 0x288FE3D3,
        0x656F2187, 0x37D12E9C, 0x2F539EB4, 0xA492998E, 0xED3958F7, 0x39E96523};
    int g_key_GAMESHARE1xx[] = {
        0x721B53E8, 0xFC3E31C6, 0xF85BA2A2, 0x3CF0AC72, 0x54EEA7AB, 0x5959BFCB,
        0x54B8836B, 0xBC431313, 0x989EF2CF, 0xF0CE36B2, 0x98BA4CF8, 0xE971C931,
        0xA0375DC8, 0x08E52FA0, 0xAC0DD426, 0x57E4D601, 0xC56E61C7, 0xEF1AB98A,
        0xD1D9F8F4, 0x5FE9A708, 0x3EF09D07, 0xFA0C1A8C, 0xA91EEA5C, 0x58F482C5,
        0x2C800302, 0x7EE6F6C3, 0xFF6ABBBB, 0x2110D0D0, 0xD3297A88, 0x980012D3,
        0xDC59C87B, 0x7FDC5792, 0xDB3F5DA6, 0xFC23B787, 0x22698ED3, 0xB680E812};
    int g_key_GAMESHARE2xx[] = {
        0x94A757C7, 0x9FD39833, 0xF8508371, 0x328B0B29, 0x2CBCB9DA, 0x2918B9C6,
        0x944C50BA, 0xF1DCE7D0, 0x640C3966, 0xC90B3D08, 0xF4AD17BA, 0x6CA0F84B,
        0xF7767C67, 0xA4D3A55A, 0x4A085C6A, 0x6BB27071, 0xFA8B38FB, 0x3FDB31B8,
        0x8B7196F2, 0xDB9BED4A, 0x51625B84, 0x4C1481B4, 0xF684F508, 0x30B44770,
        0x93AA8E74, 0x90C579BC, 0x246EC88D, 0x2E051202, 0xC774842E, 0xA185D997,
        0x7A2B3ADD, 0xFE835B6D, 0x508F184D, 0xEB4C4F13, 0x0E1993D3, 0xBA96DFD2};
    int g_key_INDEXDAT1xx[] = {
        0x76CB00AF, 0x111CE62F, 0xB7B27E36, 0x6D8DE8F9, 0xD54BF16A, 0xD9E90373,
        0x7599D982, 0x51F82B0E, 0x636103AD, 0x8E40BC35, 0x2F332C94, 0xF513AAE9,
        0xD22AFEE9, 0x04343987, 0xFC5BB80C, 0x12349D89, 0x14A481BB, 0x25ED3AE8,
        0x7D500E4F, 0x43D1B757, 0x7B59FDAD, 0x4CFBBF34, 0xC3D17436, 0xC1DA21DB,
        0xA34D8C80, 0x962B235D, 0x3E420548, 0x09CF9FFE, 0xD4883F5C, 0xD90E9CB5,
        0x00AEF4E9, 0xF0886DE9, 0x62A58A5B, 0x52A55546, 0x971941B5, 0xF5B79FAC};

    // PRXDecrypter TAG structs.
    private class TAG_INFO {

        int tag; // 4 byte value at offset 0xD0 in the PRX file
        int[] key; // 16 bytes keys
        int code; // code for scramble

        public TAG_INFO(int tag, int[] key, int code) {
            this.tag = tag;
            this.key = key;
            this.code = code;
        }
    }

    private class TAG_INFO_OLD {

        int tag; // 4 byte value at offset 0xD0 in the PRX file
        int[] key; // 144 bytes keys
        int code; // code for scramble
        int codeExtra; // code extra for scramble

        public TAG_INFO_OLD(int tag, int[] key, int code, int codeExtra) {
            this.tag = tag;
            this.key = intArrayToTagArray(key);
            this.code = code;
            this.codeExtra = codeExtra;
        }

        public TAG_INFO_OLD(int tag, int[] key, int code) {
            this.tag = tag;
            this.key = intArrayToTagArray(key);
            this.code = code;
            this.codeExtra = 0;
        }

        private int[] intArrayToTagArray(int[] array) {
            int[] tagArray = new int[144];
            for (int i = 0; i < array.length; i++) {
                tagArray[i * 4 + 3] = ((array[i] >> 24) & 0xFF);
                tagArray[i * 4 + 2] = ((array[i] >> 16) & 0xFF);
                tagArray[i * 4 + 1] = ((array[i] >> 8) & 0xFF);
                tagArray[i * 4 + 0] = (array[i] & 0xFF);
            }
            return tagArray;
        }
    }

    private TAG_INFO g_tagInfo[] = {       
        new TAG_INFO(0x4C9494F0, keys660_k1, 0x43),
        new TAG_INFO(0x4C9495F0, keys660_k2, 0x43),
        new TAG_INFO(0x4C9490F0, keys660_k3, 0x43),
        new TAG_INFO(0x4C9491F0, keys660_k8, 0x43),
        new TAG_INFO(0x4C9493F0, keys660_k4, 0x43),        
        new TAG_INFO(0x4C9497F0, keys660_k5, 0x43),
        new TAG_INFO(0x4C9492F0, keys660_k6, 0x43),
        new TAG_INFO(0x4C9496F0, keys660_k7, 0x43),       
        new TAG_INFO(0x457B90F0, keys660_v1, 0x5B),
        new TAG_INFO(0x457B91F0, keys660_v7, 0x5B),
        new TAG_INFO(0x457B92F0, keys660_v6, 0x5B),
        new TAG_INFO(0x457B93F0, keys660_v3, 0x5B),       
        new TAG_INFO(0x380290F0, keys660_v2, 0x5A),
        new TAG_INFO(0x380291F0, keys660_v8, 0x5A),
        new TAG_INFO(0x380292F0, keys660_v4, 0x5A),
        new TAG_INFO(0x380293F0, keys660_v5, 0x5A),       
        new TAG_INFO(0x4C948CF0, keys639_k3, 0x43),       
        new TAG_INFO(0x4C948DF0, keys638_k4, 0x43),     
        new TAG_INFO(0x4C948BF0, keys636_k2, 0x43),
        new TAG_INFO(0x4C948AF0, keys636_k1, 0x43),
        new TAG_INFO(0x4C9487F0, keys630_k8, 0x43),
        new TAG_INFO(0x457B83F0, keys630_k7, 0x5B),
        new TAG_INFO(0x4C9486F0, keys630_k6, 0x43),
        new TAG_INFO(0x457B82F0, keys630_k5, 0x5B),
        new TAG_INFO(0x457B81F0, keys630_k4, 0x5B),
        new TAG_INFO(0x4C9485F0, keys630_k3, 0x43),
        new TAG_INFO(0x457B80F0, keys630_k2, 0x5B),
        new TAG_INFO(0x4C9484F0, keys630_k1, 0x43),
        new TAG_INFO(0x457B28F0, keys620_e, 0x5B),
        new TAG_INFO(0x457B0CF0, keys620_a, 0x5B),
        new TAG_INFO(0x380228F0, keys620_5v, 0x5A),
        new TAG_INFO(0x4C942AF0, keys620_5k, 0x43),
        new TAG_INFO(0x4C9428F0, keys620_5, 0x43),
        new TAG_INFO(0x4C941DF0, keys620_1, 0x43),
        new TAG_INFO(0x4C941CF0, keys620_0, 0x43),
        new TAG_INFO(0x4C9422F0, keys600_2, 0x43),
        new TAG_INFO(0x4C941EF0, keys600_1, 0x43),
        new TAG_INFO(0x4C9429F0, keys570_5k, 0x43),
        new TAG_INFO(0x457B0BF0, keys505_a, 0x5B),
        new TAG_INFO(0x4C9419F0, keys505_1, 0x43),
        new TAG_INFO(0x4C9418F0, keys505_0, 0x43),
        new TAG_INFO(0x457B1EF0, keys500_c, 0x5B),
        new TAG_INFO(0x4C941FF0, keys500_2, 0x43),
        new TAG_INFO(0x4C9417F0, keys500_1, 0x43),
        new TAG_INFO(0x4C9416F0, keys500_0, 0x43),
        new TAG_INFO(0x4C9414F0, keys390_0, 0x43),
        new TAG_INFO(0x4C9415F0, keys390_1, 0x43),
        new TAG_INFO(0x4C9412F0, keys370_0, 0x43),
        new TAG_INFO(0x4C9413F0, keys370_1, 0x43),
        new TAG_INFO(0x457B10F0, keys370_2, 0x5B),
        new TAG_INFO(0x4C940DF0, keys360_0, 0x43),
        new TAG_INFO(0x4C9410F0, keys360_1, 0x43),
        new TAG_INFO(0x4C940BF0, keys330_0, 0x43),
        new TAG_INFO(0x457B0AF0, keys330_1, 0x5B),
        new TAG_INFO(0x38020AF0, keys330_2, 0x5A),
        new TAG_INFO(0x4C940AF0, keys330_3, 0x43),
        new TAG_INFO(0x4C940CF0, keys330_4, 0x43),
        new TAG_INFO(0xcfef09f0, keys310_0, 0x62),
        new TAG_INFO(0x457b08f0, keys310_1, 0x5B),
        new TAG_INFO(0x380208F0, keys310_2, 0x5A),
        new TAG_INFO(0xcfef08f0, keys310_3, 0x62),
        new TAG_INFO(0xCFEF07F0, keys303_0, 0x62),
        new TAG_INFO(0xCFEF06F0, keys300_0, 0x62),
        new TAG_INFO(0x457B06F0, keys300_1, 0x5B),
        new TAG_INFO(0x380206F0, keys300_2, 0x5A),
        new TAG_INFO(0xCFEF05F0, keys280_0, 0x62),
        new TAG_INFO(0x457B05F0, keys280_1, 0x5B),
        new TAG_INFO(0x380205F0, keys280_2, 0x5A),
        new TAG_INFO(0x16D59E03, keys260_0, 0x62),
        new TAG_INFO(0x76202403, keys260_1, 0x5B),
        new TAG_INFO(0x0F037303, keys260_2, 0x5A),
        new TAG_INFO(0x4C940FF0, key_2DA8, 0x43),
        new TAG_INFO(0x4467415D, key_22E0, 0x59),
        new TAG_INFO(0x00000000, key_21C0, 0x42),
        new TAG_INFO(0x01000000, key_2250, 0x43),     
        new TAG_INFO(0x2E5E10F0, key_2E5E10F0, 0x48),
        new TAG_INFO(0x2E5E12F0, key_2E5E12F0, 0x48),
        new TAG_INFO(0x2E5E13F0, key_2E5E13F0, 0x48),
        new TAG_INFO(0x2FD30BF0, key_2FD30BF0, 0x47),
        new TAG_INFO(0x2FD311F0, key_2FD311F0, 0x47),
        new TAG_INFO(0x2FD312F0, key_2FD312F0, 0x47),
        new TAG_INFO(0xD91605F0, key_D91605F0, 0x5D),
        new TAG_INFO(0xD91606F0, key_D91606F0, 0x5D),
        new TAG_INFO(0xD91608F0, key_D91608F0, 0x5D),
        new TAG_INFO(0xD91609F0, key_D91609F0, 0x5D),
        new TAG_INFO(0xD9160AF0, key_D9160AF0, 0x5D),
        new TAG_INFO(0xD9160BF0, key_D9160BF0, 0x5D),
        new TAG_INFO(0xD91611F0, key_D91611F0, 0x5D),
        new TAG_INFO(0xD91612F0, key_D91612F0, 0x5D),
        new TAG_INFO(0xD91613F0, key_D91613F0, 0x5D),
        new TAG_INFO(0xD91614F0, key_D91614F0, 0x5D),
        new TAG_INFO(0xD91615F0, key_D91615F0, 0x5D),
        new TAG_INFO(0xD91616F0, key_D91616F0, 0x5D),
        new TAG_INFO(0xD91617F0, key_D91617F0, 0x5D),
        new TAG_INFO(0xD91618F0, key_D91618F0, 0x5D),
        new TAG_INFO(0xD91619F0, key_D91619F0, 0x5D),
        new TAG_INFO(0xD9161AF0, key_D9161AF0, 0x5D),
        new TAG_INFO(0xD91620F0, key_D91620F0, 0x5D),
        new TAG_INFO(0xD91621F0, key_D91621F0, 0x5D),
        new TAG_INFO(0xD91622F0, key_D91622F0, 0x5D),
        new TAG_INFO(0xD91623F0, key_D91623F0, 0x5D),
        new TAG_INFO(0xD91624F0, key_D91624F0, 0x5D),
        new TAG_INFO(0xD91628F0, key_D91628F0, 0x5D),
        new TAG_INFO(0xD91680F0, key_D91680F0, 0x5D),
        new TAG_INFO(0xD91681F0, key_D91681F0, 0x5D),
        new TAG_INFO(0xD82310F0, keys02G_E, 0x51),
        new TAG_INFO(0xD8231EF0, keys03G_E, 0x51),
        new TAG_INFO(0xD82328F0, keys05G_E, 0x51),
        new TAG_INFO(0x279D08F0, oneseg_310, 0x61),
        new TAG_INFO(0x279D06F0, oneseg_300, 0x61),
        new TAG_INFO(0x279D05F0, oneseg_280, 0x61),
        new TAG_INFO(0xD66DF703, oneseg_260_271, 0x61),
        new TAG_INFO(0x279D10F0, oneseg_slim, 0x61),
        new TAG_INFO(0x3C2A08F0, ms_app_main, 0x67),
        new TAG_INFO(0xADF305F0, demokeys_280, 0x60),
        new TAG_INFO(0xADF306F0, demokeys_3XX_1, 0x60),
        new TAG_INFO(0xADF308F0, demokeys_3XX_2, 0x60),
        new TAG_INFO(0x8004FD03, ebootbin_271_new, 0x5D),
        new TAG_INFO(0xD91605F0, ebootbin_280_new, 0x5D),
        new TAG_INFO(0xD91606F0, ebootbin_300_new, 0x5D),
        new TAG_INFO(0xD91608F0, ebootbin_310_new, 0x5D),
        new TAG_INFO(0x0A35EA03, gameshare_260_271, 0x5E),
        new TAG_INFO(0x7B0505F0, gameshare_280, 0x5E),
        new TAG_INFO(0x7B0506F0, gameshare_300, 0x5E),
        new TAG_INFO(0x7B0508F0, gameshare_310, 0x5E),
        new TAG_INFO(0x380210F0, key_380210F0, 0x5A),
        new TAG_INFO(0x380280F0, key_380280F0, 0x5A),
        new TAG_INFO(0x380283F0, key_380283F0, 0x5A),
        new TAG_INFO(0x407810F0, key_407810F0, 0x6A),
        new TAG_INFO(0xE92410F0, drmkeys_6XX_1, 0x40),
        new TAG_INFO(0x692810F0, drmkeys_6XX_2, 0x40)};

    private TAG_INFO_OLD g_oldTagInfo[] = {
        new TAG_INFO_OLD(0x00000000, g_key0, 0x42),
        new TAG_INFO_OLD(0x02000000, g_key2, 0x45),
        new TAG_INFO_OLD(0x03000000, g_key3, 0x46),
        new TAG_INFO_OLD(0x4467415d, g_key44, 0x59, 0x59),
        new TAG_INFO_OLD(0x207bbf2f, g_key20, 0x5A, 0x5A),
        new TAG_INFO_OLD(0x3ace4dce, g_key3A, 0x5B, 0x5B),
        new TAG_INFO_OLD(0x07000000, g_key_INDEXDAT1xx, 0x4A),
        new TAG_INFO_OLD(0x08000000, g_keyEBOOT1xx, 0x4B),
        new TAG_INFO_OLD(0xC0CB167C, g_keyEBOOT2xx, 0x5D, 0x5D),
        new TAG_INFO_OLD(0x0B000000, g_keyUPDATER, 0x4E),
        new TAG_INFO_OLD(0x0C000000, g_keyDEMOS27X, 0x4F),
        new TAG_INFO_OLD(0x0F000000, g_keyMEIMG250, 0x52),
        new TAG_INFO_OLD(0x862648D1, g_keyMEIMG260, 0x52, 0x52),
        new TAG_INFO_OLD(0x207BBF2F, g_keyUNK1, 0x5A, 0x5A),
        new TAG_INFO_OLD(0x09000000, g_key_GAMESHARE1xx, 0x4C),
        new TAG_INFO_OLD(0xBB67C59F, g_key_GAMESHARE2xx, 0x5E, 0x5E),};

    // KIRK header structs.
    private class SHA1Header {

        private int dataSize;
        private byte[] data;

        public SHA1Header(ByteBuffer buf) {
            dataSize = buf.getInt();
        }

        private void readData(ByteBuffer buf, int size) {
            data = new byte[size];
            buf.get(data, 0, size);
        }
    }

    private class AES128CBCHeader {

        private int mode;
        private int unk1;
        private int unk2;
        private int keySeed;
        private int dataSize;

        public AES128CBCHeader(ByteBuffer buf) {
            mode = buf.getInt();
            unk1 = buf.getInt();
            unk2 = buf.getInt();
            keySeed = buf.getInt();
            dataSize = buf.getInt();
        }
    }

    private class AES128CMACHeader {

        private byte[] AES128Key = new byte[16];
        private byte[] CMACKey = new byte[16];
        private byte[] CMACHeaderHash = new byte[16];
        private byte[] CMACDataHash = new byte[16];
        private byte[] unk1 = new byte[32];
        private int mode;
        private byte useECDSAhash;
        private byte[] unk2 = new byte[11];
        private int dataSize;
        private int dataOffset;
        private byte[] unk3 = new byte[8];
        private byte[] unk4 = new byte[16];

        public AES128CMACHeader(ByteBuffer buf) {
            buf.get(AES128Key, 0, 16);
            buf.get(CMACKey, 0, 16);
            buf.get(CMACHeaderHash, 0, 16);
            buf.get(CMACDataHash, 0, 16);
            buf.get(unk1, 0, 32);
            mode = buf.getInt();
            useECDSAhash = buf.get();
            buf.get(unk2, 0, 11);
            dataSize = buf.getInt();
            dataOffset = buf.getInt();
            buf.get(unk3, 0, 8);
            buf.get(unk4, 0, 16);
        }
    }
    
    private class ECDSASig {
        private byte[] r = new byte[0x14];
        private byte[] s = new byte[0x14];
        
        private ECDSASig () {
        }
    }
    
    private class ECDSAPoint {
        private byte[] x = new byte[0x14];
        private byte[] y = new byte[0x14];
        
        private ECDSAPoint() {
        }
        
        private ECDSAPoint(byte[] data) {
            System.arraycopy(data, 0, x, 0, 0x14);
            System.arraycopy(data, 0x14, y, 0, 0x14);
        }
        
        public byte[] toByteArray() {
            byte[] point = new byte[0x28];
            System.arraycopy(point, 0, x, 0, 0x14);
            System.arraycopy(point, 0x14, y, 0, 0x14);
            return point;
        }
    }
    
    private class ECDSAKeygenCtx {
        private byte[] private_key = new byte[0x14];
        private ECDSAPoint public_key;
        private ByteBuffer out;
        
        private ECDSAKeygenCtx (ByteBuffer output) {
            public_key = new ECDSAPoint();
            out = output;
        }
        
        public void write() {
            out.put(private_key);
            out.put(public_key.toByteArray());
        }
    }
    
    private class ECDSAMultiplyCtx {
        private byte[] multiplier = new byte[0x14];
        private ECDSAPoint public_key = new ECDSAPoint();
        private ByteBuffer out;
        
        private ECDSAMultiplyCtx (ByteBuffer input, ByteBuffer output) {
            out = output;
            input.get(multiplier, 0, 0x14);
            input.get(public_key.x, 0, 0x14);
            input.get(public_key.y, 0, 0x14);
        }
        
        public void write() {
            out.put(multiplier);
            out.put(public_key.toByteArray());
        }
    }
    
    private class ECDSASignCtx {
        private byte[] enc = new byte[0x20];
        private byte[] hash = new byte[0x14];
        
        private ECDSASignCtx (ByteBuffer buf) {
            buf.get(enc, 0, 0x20);
            buf.get(hash, 0, 0x14);
        }
    }
    
    private class ECDSAVerifyCtx {
        private ECDSAPoint public_key;
        private byte[] hash = new byte[0x14];
        private ECDSASig sig;
        
        private ECDSAVerifyCtx (ByteBuffer buf) {
            buf.get(public_key.x, 0, 0x14);
            buf.get(public_key.y, 0, 0x14);
            buf.get(hash, 0, 0x14);
            buf.get(sig.r, 0, 0x14);
            buf.get(sig.s, 0, 0x14);
        }
    }

    // CHNNLSV SD context structs.
    private class SDCtx1 {

        private int mode;
        private int unk;
        private byte[] buf = new byte[16];

        public SDCtx1() {
            mode = 0;
            unk = 0;
        }
    }

    private class SDCtx2 {

        private int mode;
        private byte[] key = new byte[16];
        private byte[] pad = new byte[16];
        private int padSize;

        public SDCtx2() {
            mode = 0;
            padSize = 0;
        }
    }

    // AMCTRL context structs.
    private class BBCipherCtx {

        private int mode;
        private int unk;
        private byte[] buf = new byte[16];

        public BBCipherCtx() {
            mode = 0;
            unk = 0;
        }
    }

    private class BBMacCtx {

        private int mode;
        private byte[] key = new byte[16];
        private byte[] pad = new byte[16];
        private int padSize;

        public BBMacCtx() {
            mode = 0;
            padSize = 0;
        }
    }

	private static class ExtractEbootSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setExtractEbootStatus(value);
		}
	}

	private static class CryptSavedataSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setSavedataCryptoStatus(value);
		}
	}

    public CryptoEngine() {
    	installSettingsListeners();
    	isCryptoEngineInit = true;
    }

    private static void installSettingsListeners() {
    	if (extractEbootSettingsListerner == null) {
    		extractEbootSettingsListerner = new ExtractEbootSettingsListerner();
    		Settings.getInstance().registerSettingsListener(name, "emu.extractEboot", extractEbootSettingsListerner);
    	}
    	if (cryptSavedataSettingsListerner == null) {
    		cryptSavedataSettingsListerner = new CryptSavedataSettingsListerner();
    		Settings.getInstance().registerSettingsListener(name, "emu.cryptoSavedata", cryptSavedataSettingsListerner);
    	}
    }

    /*
     * Helper functions: used for status checking and parameter sorting.
     */

    public static boolean getExtractEbootStatus() {
    	installSettingsListeners();
        return extractEboot;
    }

    private static void setExtractEbootStatus(boolean status) {
        extractEboot = status;
    }

    public static boolean getSavedataCryptoStatus() {
    	installSettingsListeners();
        return cryptoSavedata;
    }

    private static void setSavedataCryptoStatus(boolean status) {
        cryptoSavedata = status;
    }

    private int[] getAESKeyFromSeed(int seed) {
        switch (seed) {
            case (0x03):
                return kirkAESKey1;
            case (0x04):
                return kirkAESKey2;
            case (0x05):
                return kirkAESKey3;
            case (0x0C):
                return kirkAESKey4;
            case (0x0D):
                return kirkAESKey5;
            case (0x0E):
                return kirkAESKey6;
            case (0x0F):
                return kirkAESKey7;
            case (0x10):
                return kirkAESKey8;
            case (0x11):
                return kirkAESKey9;
            case (0x12):
                return kirkAESKey10;
            case (0x38):
                return kirkAESKey11;
            case (0x39):
                return kirkAESKey12;
            case (0x3A):
                return kirkAESKey13;
            case (0x4B):
                return kirkAESKey14;
            case (0x53):
                return kirkAESKey15;
            case (0x57):
                return kirkAESKey16;
            case (0x5D):
                return kirkAESKey17;
            case (0x63):
                return kirkAESKey18;
            case (0x64):
                return kirkAESKey19;
            default:
                return null;
        }
    }

    private static boolean isNullKey(byte[] key) {
    	if (key != null) {
	    	for (int i = 0; i < key.length; i++) {
	    		if (key[i] != (byte) 0) {
	    			return false;
	    		}
	    	}
    	}

    	return true;
    }

    /*
     * KIRK commands: main emulated crypto functions.
     */

    // Decrypt with AESCBC128-CMAC header and sig check.
    private int executeKIRKCmd1(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }

        // Copy the input for sig check.
        ByteBuffer sigIn = in.duplicate();

        // Read in the CMD1 format header.
        AES128CMACHeader header = new AES128CMACHeader(in);

        if (header.mode != PSP_KIRK_CMD_MODE_CMD1) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode CMD1.
        }

        // Start AES128 processing.
        AES128 aes = new AES128();
        byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        // Convert the AES CMD1 key into a real byte array for SecretKeySpec.
        byte[] k = new byte[16];
        for (int i = 0; i < kirkAESKey0.length; i++) {
            k[i] = (byte) kirkAESKey0[i];
        }

        // Decrypt and extract the new AES and CMAC keys from the top of the data.
        byte[] encryptedKeys = new byte[32];
        System.arraycopy(header.AES128Key, 0, encryptedKeys, 0, 16);
        System.arraycopy(header.CMACKey, 0, encryptedKeys, 16, 16);
        byte[] decryptedKeys = aes.decryptCBC(encryptedKeys, k, iv);

        // Check for a valid signature.
        int sigCheck = executeKIRKCmd10(sigIn, size);

        if (decryptedKeys == null) {
            // Only return the sig check result if the keys are invalid
            // to allow skipping the CMAC comparision.
            // TODO: Trace why the CMAC hashes aren't matching.
        	return sigCheck;
        }

        // Get the newly decrypted AES key and proceed with the
        // full data decryption.
        byte[] aesBuf = new byte[16];
        System.arraycopy(decryptedKeys, 0, aesBuf, 0, aesBuf.length);
        // Skip the CMD1 header.
        int headerSize = 0x90;
        int headerOffset = 0x40;

        // Extract the final ELF params.
        int elfDataSize = Integer.reverseBytes(header.dataSize);
        int elfDataOffset = Integer.reverseBytes(header.dataOffset);

        // Input buffer for decryption must have a length aligned on 16 bytes
        int paddedElfDataSize = (elfDataSize + 15) & -16;

        // Decrypt all the ELF data.
        byte[] inBuf = new byte[paddedElfDataSize];
        System.arraycopy(in.array(), elfDataOffset + headerOffset + headerSize, inBuf, 0, paddedElfDataSize);
        byte[] outBuf = aes.decryptCBC(inBuf, aesBuf, iv);

        out.clear();
        out.put(outBuf);
        out.limit(elfDataSize);
        in.clear();

        return 0;
    }

    // Encrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd4(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD4 format header.
        AES128CBCHeader header = new AES128CBCHeader(in);

        if (header.mode != PSP_KIRK_CMD_MODE_ENCRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode ENCRYPT_CBC.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = getAESKeyFromSeed(header.keySeed);
        if (key == null) {
            return PSP_KIRK_INVALID_SIZE; // Dummy.
        }

        byte[] encKey = new byte[16];
        for (int i = 0; i < encKey.length; i++) {
            encKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128();
        byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.encryptCBC(inBuf, encKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // Encrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd5(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD4 format header.
        AES128CBCHeader header = new AES128CBCHeader(in);

        if (header.mode != PSP_KIRK_CMD_MODE_ENCRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode ENCRYPT_CBC.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = null;
        if(header.keySeed == 0x100) {
            key = fuseID;
        } else {
            return PSP_KIRK_INVALID_SIZE; // Dummy.
        }

        byte[] encKey = new byte[16];
        for (int i = 0; i < encKey.length; i++) {
            encKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128();
        byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.encryptCBC(inBuf, encKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // Decrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd7(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD7 format header.
        AES128CBCHeader header = new AES128CBCHeader(in);

        if (header.mode != PSP_KIRK_CMD_MODE_DECRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode DECRYPT_CBC.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = getAESKeyFromSeed(header.keySeed);
        if (key == null) {
            return PSP_KIRK_INVALID_SIZE; // Dummy.
        }

        byte[] decKey = new byte[16];
        for (int i = 0; i < decKey.length; i++) {
            decKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128();
        byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.decryptCBC(inBuf, decKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // Decrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd8(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD7 format header.
        AES128CBCHeader header = new AES128CBCHeader(in);

        if (header.mode != PSP_KIRK_CMD_MODE_DECRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode DECRYPT_CBC.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = null;
        if(header.keySeed == 0x100) {
            key = fuseID;
        } else {
            return PSP_KIRK_INVALID_SIZE; // Dummy.
        }

        byte[] decKey = new byte[16];
        for (int i = 0; i < decKey.length; i++) {
            decKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128();
        byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.decryptCBC(inBuf, decKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // CMAC Sig check.
    private int executeKIRKCmd10(ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD10 format header.
        AES128CMACHeader header = new AES128CMACHeader(in);
        if ((header.mode != PSP_KIRK_CMD_MODE_CMD1) &&
                (header.mode != PSP_KIRK_CMD_MODE_CMD2) &&
                (header.mode != PSP_KIRK_CMD_MODE_CMD3)) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for modes CMD1, CMD2 and CMD3.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        AES128 aes = new AES128();
        byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        // Convert the AES CMD1 key into a real byte array.
        byte[] k = new byte[16];
        for (int i = 0; i < kirkAESKey0.length; i++) {
            k[i] = (byte) kirkAESKey0[i];
        }

        // Decrypt and extract the new AES and CMAC keys from the top of the data.
        byte[] encryptedKeys = new byte[32];
        System.arraycopy(header.AES128Key, 0, encryptedKeys, 0, 16);
        System.arraycopy(header.CMACKey, 0, encryptedKeys, 16, 16);
        byte[] decryptedKeys = aes.decryptCBC(encryptedKeys, k, iv);

        byte[] cmacHeaderHash = new byte[16];
        byte[] cmacDataHash = new byte[16];

        byte[] cmacBuf = new byte[16];
        System.arraycopy(decryptedKeys, 16, cmacBuf, 0, cmacBuf.length);

        // Position the buffer at the CMAC keys offset.
        byte[] inBuf = new byte[in.capacity() - 0x60];
        System.arraycopy(in.array(), 0x60, inBuf, 0, inBuf.length);

        // Calculate CMAC header hash.
        aes.doInitCMAC(cmacBuf);
        aes.doUpdateCMAC(inBuf, 0, 0x30);
        cmacHeaderHash = aes.doFinalCMAC();

        int blockSize = Integer.reverseBytes(header.dataSize);
        if ((blockSize % 16) != 0) {
            blockSize += (16 - (blockSize % 16));
        }

        // Calculate CMAC data hash.
        aes.doInitCMAC(cmacBuf);
        aes.doUpdateCMAC(inBuf, 0, 0x30 + blockSize + Integer.reverseBytes(header.dataOffset));
        cmacDataHash = aes.doFinalCMAC();

        if (cmacHeaderHash != header.CMACHeaderHash) {
            return PSP_KIRK_INVALID_HEADER_HASH;
        }

        if (cmacDataHash != header.CMACDataHash) {
            return PSP_KIRK_INVALID_DATA_HASH;
        }

        return 0;
    }

    // Generate SHA1 hash.
    private int executeKIRKCmd11(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }

        SHA1Header header = new SHA1Header(in);
        SHA1 sha1 = new SHA1();

        size = (size < header.dataSize) ? size : header.dataSize;
        header.readData(in, size);

        out.clear();
        out.put(sha1.doSHA1(header.data, size));
        in.clear();

        return 0;
    }
    
    private int executeKIRKCmd12(ByteBuffer out, int size) { 
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }
        
        if (size != 0x3C) {
            return PSP_KIRK_INVALID_SIZE;
        }
        
        // Start the ECDSA context.
        ECDSA ecdsa = new ECDSA();
        ECDSAKeygenCtx ctx = new ECDSAKeygenCtx(out);
        ecdsa.setCurve();
        
        // Generate the private/public key pair and write it back.
        ctx.private_key = ecdsa.getPrivateKey();
        ctx.public_key = new ECDSAPoint(ecdsa.getPublicKey());
        
        ctx.write();
        
        return 0;
    }
    
    private int executeKIRKCmd13(ByteBuffer out, int outSize, ByteBuffer in, int inSize) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }
        
        if ((inSize != 0x3C) || (outSize != 0x28)) {
            return PSP_KIRK_INVALID_SIZE;
        }
        
        // Start the ECDSA context.
        ECDSA ecdsa = new ECDSA();
        ECDSAMultiplyCtx ctx = new ECDSAMultiplyCtx(in, out);
        ecdsa.setCurve();
        
        // Multiply the public key.
        ecdsa.multiplyPublicKey(ctx.public_key.toByteArray(), ctx.multiplier);
        
        ctx.write();
        
        return 0;
    }

    // Generate pseudo random number.
    private int executeKIRKCmd14(ByteBuffer out, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }
        
        if (size > 0) {
            Random rd = new Random();
            byte[] rdBytes = new byte[size];

            rd.nextBytes(rdBytes);

            out.clear();
            out.put(rdBytes);
        }

        return 0;
    }
    
    private int executeKIRKCmd16(ByteBuffer out, int outSize, ByteBuffer in, int inSize) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }
        
        if ((inSize != 0x34) || (outSize != 0x28)) {
            return PSP_KIRK_INVALID_SIZE;
        }
        
        // TODO
        ECDSA ecdsa = new ECDSA();
        ECDSASignCtx ctx = new ECDSASignCtx(in);
        ECDSASig sig = new ECDSASig();
        ecdsa.setCurve();
        
        return 0;
    }
    
    private int executeKIRKCmd17(ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!isCryptoEngineInit) {
            return PSP_KIRK_NOT_INIT;
        }
        
        if (size != 0x64) {
            return PSP_KIRK_INVALID_SIZE;
        }
        
        // TODO
        ECDSA ecdsa = new ECDSA();
        ECDSAVerifyCtx ctx = new ECDSAVerifyCtx(in);
        ecdsa.setCurve();
        
        return 0;
    }

    /*
     * HLE functions: high level implementation of crypto functions from
     * several modules which employ various algorithms and communicate with the
     * crypto engine in different ways.
     */

    /*
     * sceUtils - memlmd_01g.prx and memlmd_02g.prx
     */

    public void hleUtilsSetFuseID(int[] id) {
        fuseID = id;
    }

    public int hleUtilsBufferCopyWithRange(ByteBuffer out, int outsize, ByteBuffer in, int insize, int cmd) {
        switch (cmd) {
            case PSP_KIRK_CMD_DECRYPT_PRIVATE:
                return executeKIRKCmd1(out, in, insize);
            case PSP_KIRK_CMD_ENCRYPT:
                return executeKIRKCmd4(out, in, insize);
            case PSP_KIRK_CMD_ENCRYPT_FUSE:
                return executeKIRKCmd5(out, in, insize);
            case PSP_KIRK_CMD_DECRYPT:
                return executeKIRKCmd7(out, in, insize);
            case PSP_KIRK_CMD_DECRYPT_FUSE:
                return executeKIRKCmd8(out, in, insize);
            case PSP_KIRK_CMD_PRIV_SIG_CHECK:
                return executeKIRKCmd10(in, insize);
            case PSP_KIRK_CMD_SHA1_HASH:
                return executeKIRKCmd11(out, in, insize);
            case PSP_KIRK_CMD_ECDSA_GEN_KEYS:
                return executeKIRKCmd12(out, outsize);
            case PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT:
                return executeKIRKCmd13(out, outsize, in, insize);
            case PSP_KIRK_CMD_PRNG:
                return executeKIRKCmd14(out, insize);
            case PSP_KIRK_CMD_ECDSA_SIGN:
                return executeKIRKCmd16(out, outsize, in, insize);
            case PSP_KIRK_CMD_ECDSA_VERIFY:
                return executeKIRKCmd17(in, insize);
            default:
                return PSP_KIRK_INVALID_OPERATION; // Dummy.
        }
    }

    /*
     * sceSd - chnnlsv.prx
     */

    private int hleSdSetIndex(SDCtx2 ctx, int encMode) {
        // Set all parameters to 0 and assign the encMode.
        ctx.mode = encMode;
        return 0;
    }

    private int hleSdCreateList(SDCtx1 ctx, int encMode, int genMode, byte[] data, byte[] key) {
        // If the key is not a 16-byte key, return an error.
        if (key.length < 0x10) {
            return -1;
        }

        // Set the mode and the unknown parameters.
        ctx.mode = encMode;
        ctx.unk = 0x1;

        // Key generator mode 0x1 (encryption): use an encrypted pseudo random number before XORing the data with the given key.
        if (genMode == 0x1) {
            byte[] header = new byte[0x10 + 0x14];
            byte[] random = new byte[0x14];
            byte[] newKey = new byte[0x10];

            ByteBuffer bRandom = ByteBuffer.wrap(random);
            hleUtilsBufferCopyWithRange(bRandom, 0x14, null, 0, 0xE);

            for (int i = 0xF; i >= 0; i--) {
                newKey[0xF - i] = random[i];
            }
            System.arraycopy(newKey, 0, header, 0x14, 0x10);
            for (int i = 0; i < 4; i++) {
                header[0x20 + i] = 0;
            }

            // Encryption mode 0x1: encrypt with KIRK CMD4 and XOR with the given key.
            if (ctx.mode == 0x1) {
                ScrambleSD(header, 0x10, 0x4, 0x4, 0x04);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x2) { // Encryption mode 0x2: encrypt with KIRK CMD5 and XOR with the given key.
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x3) { // Encryption mode 0x3: XOR with SD keys, encrypt with KIRK CMD4 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ sdHashKey3[i]);
                }
                ScrambleSD(header, 0x10, 0xE, 0x4, 0x04);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ sdHashKey4[i]);
                }
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x4) { // Encryption mode 0x4: XOR with SD keys, encrypt with KIRK CMD5 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ sdHashKey3[i]);
                }
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ sdHashKey4[i]);
                }
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x6) { // Encryption mode 0x6: XOR with new SD keys, encrypt with KIRK CMD5 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ sdHashKey3[i]);
                }
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ sdHashKey4[i]);
                }
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else { // Encryption mode 0x0: XOR with new SD keys, encrypt with KIRK CMD4 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ sdHashKey6[i]);
                }
                ScrambleSD(header, 0x10, 0x12, 0x4, 0x04);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ sdHashKey7[i]);
                }
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            }
        } else if (genMode == 0x2) { // Key generator mode 0x02 (decryption): directly XOR the data with the given key.
            // Grab the data hash (first 16-bytes).
            System.arraycopy(data, 0, ctx.buf, 0, 0x10);
            // If the key is not null, XOR the hash with it.
            if (key != null) {
                for (int i = 0; i < 16; i++) {
                    ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                }
            }
            return 0;
        } else {
            // Invalid mode.
            return -1;
        }
    }

    private int hleSdRemoveValue(SDCtx2 ctx, byte[] data, int length) {
        if (ctx.padSize > 0x10 || (length < 0)) {
            // Invalid key or length.
            return -1;
        } else if (((ctx.padSize + length) <= 0x10)) {
            // The key hasn't been set yet.
            // Extract the hash from the data and set it as the key.
            System.arraycopy(data, 0, ctx.pad, ctx.padSize, length);
            ctx.padSize += length;
            return 0;
        } else {
            // Calculate the seed.
            int seed = 0;
            switch (ctx.mode) {
                case 0x6:
                    seed = 0x11;
                    break;
                case 0x4:
                    seed = 0xD;
                    break;
                case 0x2:
                    seed = 0x5;
                    break;
                case 0x1:
                    seed = 0x3;
                    break;
                case 0x3:
                    seed = 0xC;
                    break;
                default:
                    seed = 0x10;
                    break;
            }
            
            // Setup the buffers. 
            byte[] scrambleBuf = new byte[(length + ctx.padSize) + 0x14];  
            
            // Copy the previous key to the buffer.
            System.arraycopy(ctx.pad, 0, scrambleBuf, 0x14, ctx.padSize);
            
            // Calculate new key length.
            int kLen = ctx.padSize;          
            
            ctx.padSize += length;
            ctx.padSize &= 0x0F;
            if(ctx.padSize == 0) {
                ctx.padSize = 0x10;
            }           
            
            // Calculate new data length.
            length -= ctx.padSize;    
            
            // Copy data's footer to make a new key.
            System.arraycopy(data, length, ctx.pad, 0, ctx.padSize);
            
            // Process the encryption in 0x800 blocks.
            int blockSize = 0;
            int dataOffset = 0;
            
            while (length > 0) {
                blockSize = (length + kLen >= 0x0800) ? 0x0800 : length + kLen;            
                
                System.arraycopy(data, dataOffset, scrambleBuf, 0x14 + kLen, blockSize - kLen);
                
                // Encrypt with KIRK CMD 4 and XOR with result.
                for (int i = 0; i < 0x10; i++) {
                    scrambleBuf[0x14 + i] = (byte) (scrambleBuf[0x14 + i] ^ ctx.key[i]);
                } 
                ScrambleSD(scrambleBuf, blockSize, seed, 0x4, 0x04);             
                System.arraycopy(scrambleBuf, (blockSize + 0x4) - 0x14, ctx.key, 0, 0x10);
                
                // Adjust data length, data offset and reset any key length.
                length -= (blockSize - kLen);
                dataOffset += (blockSize - kLen);
                kLen = 0;               
            }      

            return 0;
        }
    }

    private int hleSdGetLastIndex(SDCtx2 ctx, byte[] hash, byte[] key) {
        if (ctx.padSize > 0x10) {
            // Invalid key length.
            return -1;
        }

        // Setup the buffers.           
        byte[] scrambleEmptyBuf = new byte[0x10 + 0x14];
        byte[] keyBuf = new byte[0x10];            
        byte[] scrambleKeyBuf = new byte[0x10 + 0x14];          
        byte[] resultBuf = new byte[0x10];
        byte[] scrambleResultBuf = new byte[0x10 + 0x14];     
        byte[] scrambleResultKeyBuf = new byte[0x10 + 0x14];

        // Calculate the seed.
        int seed = 0;
        switch (ctx.mode) {
            case 0x6:
                seed = 0x11;
                break;
            case 0x4:
                seed = 0xD;
                break;
            case 0x2:
                seed = 0x5;
                break;
            case 0x1:
                seed = 0x3;
                break;
            case 0x3:
                seed = 0xC;
                break;
            default:
                seed = 0x10;
                break;
        }
        
        // Encrypt an empty buffer with KIRK CMD 4.
        ScrambleSD(scrambleEmptyBuf, 0x10, seed, 0x4, 0x04);
        System.arraycopy(scrambleEmptyBuf, 0, keyBuf, 0, 0x10);
        
        // Apply custom padding management.
        byte b = ((keyBuf[0] & (byte) 0x80) != 0) ? (byte) 0x87 : 0;       
        for(int i = 0; i < 0xF; i++) {
            keyBuf[i] = (byte) ((keyBuf[i] << 1) | ((keyBuf[i + 1] >> 7) & 0x01));
        }        
        keyBuf[0xF] = (byte) ((keyBuf[0xF] << 1) ^ b);    
        
        if (ctx.padSize < 0x10) {
            byte bb = ((keyBuf[0] & (byte) 0x80) != 0) ? (byte) 0x87 : 0;       
            for(int i = 0; i < 0xF; i++) {
                keyBuf[i] = (byte) ((keyBuf[i] << 1) | ((keyBuf[i + 1] >> 7) & 0x01));
            }        
            keyBuf[0xF] = (byte) ((keyBuf[0xF] << 1) ^ bb);
            
            ctx.pad[ctx.padSize] = (byte) 0x80;
            if ((ctx.padSize + 1) < 0x10) {
                for (int i = 0; i < (0x10 - ctx.padSize - 1); i++) {
                    ctx.pad[ctx.padSize + 1 + i] = 0;
                }
            }
        }
        
        // XOR previous key with new one.
        for (int i = 0; i < 0x10; i++) {
            ctx.pad[i] = (byte) (ctx.pad[i] ^ keyBuf[i]);
        }
        
        System.arraycopy(ctx.pad, 0, scrambleKeyBuf, 0x14, 0x10);
        System.arraycopy(ctx.key, 0, resultBuf, 0, 0x10);
        
        // Encrypt with KIRK CMD 4 and XOR with result.
        for (int i = 0; i < 0x10; i++) {
            scrambleKeyBuf[0x14 + i] = (byte) (scrambleKeyBuf[0x14 + i] ^ resultBuf[i]);
        }
        ScrambleSD(scrambleKeyBuf, 0x10, seed, 0x4, 0x04);
        System.arraycopy(scrambleKeyBuf, (0x10 + 0x4) - 0x14, resultBuf, 0, 0x10);
        
        // If ctx.mode is the new mode 0x6, XOR with the new hash key 5, else, XOR with hash key 2.
        if (ctx.mode == 0x6) {
            for (int i = 0; i < 0x10; i++) {
                resultBuf[i] = (byte) (resultBuf[i] ^ sdHashKey5[i]);
            }
        } else {
            for (int i = 0; i < 0x10; i++) {
                resultBuf[i] = (byte) (resultBuf[i] ^ sdHashKey2[i]);
            }
        }
        
        // If mode is 2, 4 or 6, encrypt again with KIRK CMD 5 and then KIRK CMD 4.
        if ((ctx.mode == 0x2) || (ctx.mode == 0x4) || (ctx.mode == 0x6)) {
            System.arraycopy(resultBuf, 0, scrambleResultBuf, 0x14, 0x10);
            ScrambleSD(scrambleResultBuf, 0x10, 0x100, 0x4, 0x05);           
            ScrambleSD(scrambleResultBuf, 0x10, seed, 0x4, 0x04);
            System.arraycopy(scrambleResultBuf, 0, resultBuf, 0, 0x10);
        }
        
        // XOR with the supplied key and encrypt with KIRK CMD 4.
        if (key != null) {
            for (int i = 0; i < 0x10; i++) {
                resultBuf[i] = (byte) (resultBuf[i] ^ key[i]);
            }
            System.arraycopy(resultBuf, 0, scrambleResultKeyBuf, 0x14, 0x10);
            ScrambleSD(scrambleResultKeyBuf, 0x10, seed, 0x4, 0x04);
            System.arraycopy(scrambleResultKeyBuf, 0, resultBuf, 0, 0x10);
        }
        
        // Copy back the generated hash.
        System.arraycopy(resultBuf, 0, hash, 0, 0x10);
     
        // Clear the context fields.
        ctx.mode = 0;
        ctx.padSize = 0;
        for (int i = 0; i < 0x10; i++) {
            ctx.pad[i] = 0;
        }
        for (int i = 0; i < 0x10; i++) {
            ctx.key[i] = 0;
        }     
            
        return 0;
    }

    private int hleSdSetMember(SDCtx1 ctx, byte[] data, int length) {
        if (length <= 0) {
            return -1;
        }

        int finalSeed = 0;
        byte[] dataBuf = new byte[length + 0x14];
        byte[] keyBuf = new byte[0x10 + 0x10];
        byte[] hashBuf = new byte[0x10];

        // Copy the hash stored by hleSdCreateList.
        System.arraycopy(ctx.buf, 0, dataBuf, 0x14, 0x10);

        if (ctx.mode == 0x1) {
            // Decryption mode 0x01: decrypt the hash directly with KIRK CMD7.
            ScrambleSD(dataBuf, 0x10, 0x4, 5, 0x07);
            finalSeed = 0x53;
        } else if (ctx.mode == 0x2) {
            // Decryption mode 0x02: decrypt the hash directly with KIRK CMD8.
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            finalSeed = 0x53;
        } else if (ctx.mode == 0x3) {
            // Decryption mode 0x03: XOR the hash with SD keys and decrypt with KIRK CMD7.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ sdHashKey3[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0xE, 5, 0x07);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ sdHashKey4[i]);
            }
            finalSeed = 0x57;
        } else if (ctx.mode == 0x4) {
            // Decryption mode 0x04: XOR the hash with SD keys and decrypt with KIRK CMD8.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ sdHashKey3[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ sdHashKey4[i]);
            }
            finalSeed = 0x57;
        } else if (ctx.mode == 0x6) {
            // Decryption mode 0x06: XOR the hash with new SD keys and decrypt with KIRK CMD8.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ sdHashKey7[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ sdHashKey6[i]);
            }
            finalSeed = 0x64;
        } else {
            // Decryption master mode: XOR the hash with new SD keys and decrypt with KIRK CMD7.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ sdHashKey7[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x12, 5, 0x07);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ sdHashKey6[i]);
            }
            finalSeed = 0x64;
        }

        // Store the calculated key.
        System.arraycopy(dataBuf, 0, keyBuf, 0x10, 0x10);

        if (ctx.unk != 0x1) {
            System.arraycopy(keyBuf, 0x10, keyBuf, 0, 0xC);
            keyBuf[0xC] = (byte) ((ctx.unk - 1) & 0xFF);
            keyBuf[0xD] = (byte) (((ctx.unk - 1) >> 8) & 0xFF);
            keyBuf[0xE] = (byte) (((ctx.unk - 1) >> 16) & 0xFF);
            keyBuf[0xF] = (byte) (((ctx.unk - 1) >> 24) & 0xFF);
        }

        // Copy the first 0xC bytes of the obtained key and replicate them
        // across a new list buffer. As a terminator, add the ctx.unk parameter's
        // 4 bytes (endian swapped) to achieve a full numbered list.
        for (int i = 0x14; i < (length + 0x14); i += 0x10) {
            System.arraycopy(keyBuf, 0x10, dataBuf, i, 0xC);
            dataBuf[i + 0xC] = (byte) (ctx.unk & 0xFF);
            dataBuf[i + 0xD] = (byte) ((ctx.unk >> 8) & 0xFF);
            dataBuf[i + 0xE] = (byte) ((ctx.unk >> 16) & 0xFF);
            dataBuf[i + 0xF] = (byte) ((ctx.unk >> 24) & 0xFF);
            ctx.unk++;
        }

        System.arraycopy(dataBuf, length + 0x04, hashBuf, 0, 0x10);

        ScrambleSD(dataBuf, length, finalSeed, 5, 0x07);

        // XOR the first 16-bytes of data with the saved key to generate a new hash.
        for (int i = 0; i < 0x10; i++) {
            dataBuf[i] = (byte) (dataBuf[i] ^ keyBuf[i]);
        }

        // Copy back the last hash from the list to the first half of keyBuf.
        System.arraycopy(hashBuf, 0, keyBuf, 0, 0x10);

        // Finally, XOR the full list with the given data.
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (data[i] ^ dataBuf[i]);
        }

        return 0;
    }

    private int hleChnnlsv_21BE78B4(SDCtx1 ctx) {
        ctx.mode = 0;
        ctx.unk = 0;
        for(int i = 0; i < 0x10; i++) {
            ctx.buf[i] = 0;
        }
        return 0;
    }

    /*
     * sceDrmBB - amctrl.prx
     */

    private int hleDrmBBMacInit(BBMacCtx ctx, int encMode) {
        // Set all parameters to 0 and assign the encMode.
        ctx.mode = encMode;
        return 0;
    }

    private int hleDrmBBMacUpdate(BBMacCtx ctx, byte[] data, int length) {        
        if (ctx.padSize > 0x10) {
            // Invalid key was set.
            return -1;
        } else if ((ctx.padSize + length) <= 0x10) {
            // The key hasn't been set yet.
            // Extract the hash from the data and set it as the key.
            System.arraycopy(data, 0, ctx.pad, ctx.padSize, length);
            ctx.padSize += length;
            return 0;
        } else {
            // Calculate the seed (mode 2 == 0x3A / mode 1 and 3 == 0x3A).
            int seed = 0x38;
            if (ctx.mode == 0x2) {
                seed = 0x3A;
            }
            
            // Setup the buffers. 
            byte[] scrambleBuf = new byte[(length + ctx.padSize) + 0x14];  
            
            // Copy the previous key to the buffer.
            System.arraycopy(ctx.pad, 0, scrambleBuf, 0x14, ctx.padSize);
            
            // Calculate new key length.
            int kLen = ctx.padSize;          
            
            ctx.padSize += length;
            ctx.padSize &= 0x0F;
            if(ctx.padSize == 0) {
                ctx.padSize = 0x10;
            }           
            
            // Calculate new data length.
            length -= ctx.padSize;    
            
            // Copy data's footer to make a new key.
            System.arraycopy(data, length, ctx.pad, 0, ctx.padSize);
            
            // Process the encryption in 0x800 blocks.
            int blockSize = 0;
            int dataOffset = 0;
            
            while (length > 0) {
                blockSize = (length + kLen >= 0x0800) ? 0x0800 : length + kLen;
                
                System.arraycopy(data, dataOffset, scrambleBuf, 0x14 + kLen, blockSize - kLen);
                
                // Encrypt with KIRK CMD 4 and XOR with key.
                for (int i = 0; i < 0x10; i++) {
                    scrambleBuf[0x14 + i] = (byte) (scrambleBuf[0x14 + i] ^ ctx.key[i]);
                } 
                ScrambleSD(scrambleBuf, blockSize, seed, 0x4, 0x04);             
                System.arraycopy(scrambleBuf, (blockSize + 0x4) - 0x14, ctx.key, 0, 0x10);
                
                // Adjust data length, data offset and reset any key length.
                length -= (blockSize - kLen);
                dataOffset += (blockSize - kLen);
                kLen = 0;               
            }      

            return 0;
        }
    }

    private int hleDrmBBMacFinal(BBMacCtx ctx, byte[] hash, byte[] key) {
        if (ctx.padSize > 0x10) {
            // Invalid key was set.
            return -1;
        }

        // Setup the buffers.           
        byte[] scrambleEmptyBuf = new byte[0x10 + 0x14];
        byte[] keyBuf = new byte[0x10];            
        byte[] scrambleKeyBuf = new byte[0x10 + 0x14];          
        byte[] resultBuf = new byte[0x10];
        byte[] scrambleResultBuf = new byte[0x10 + 0x14];     
        byte[] scrambleResultKeyBuf = new byte[0x10 + 0x14];
        byte[] scrambleResultKeyBuf2 = new byte[0x10 + 0x14];
        byte[] finalBuf = new byte[0x10];

        // Calculate the seed (mode 2 == 0x3A / mode 1 and 3 == 0x3A).
        int seed = 0x38;
        if (ctx.mode == 0x2) {
            seed = 0x3A;
        }
        
        // Encrypt an empty buffer with KIRK CMD 4.
        ScrambleSD(scrambleEmptyBuf, 0x10, seed, 0x4, 0x04);
        System.arraycopy(scrambleEmptyBuf, 0, keyBuf, 0, 0x10);
        
        // Apply custom padding management.
        byte b = ((keyBuf[0] & (byte) 0x80) != 0) ? (byte) 0x87 : 0;       
        for(int i = 0; i < 0xF; i++) {
            keyBuf[i] = (byte) ((keyBuf[i] << 1) | (keyBuf[i + 1] >> 7));
        }        
        keyBuf[0xF] = (byte) ((keyBuf[0xF] << 1) ^ b);    
        
        if (ctx.padSize < 0x10) {
            byte bb = ((keyBuf[0] & (byte) 0x80) != 0) ? (byte) 0x87 : 0;       
            for(int i = 0; i < 0xF; i++) {
                keyBuf[i] = (byte) ((keyBuf[i] << 1) | (keyBuf[i + 1] >> 7));
            }        
            keyBuf[0xF] = (byte) ((keyBuf[0xF] << 1) ^ bb);
            
            ctx.pad[ctx.padSize] = (byte) 0x80;
            if ((ctx.padSize + 1) < 0x10) {
                for (int i = 0; i < (0x10 - ctx.padSize - 1); i++) {
                    ctx.pad[ctx.padSize + 1 + i] = 0;
                }
            }
        }
        
        // XOR pad.
        for (int i = 0; i < 0x10; i++) {
            ctx.pad[i] = (byte) (ctx.pad[i] ^ keyBuf[i]);
        }
        
        System.arraycopy(ctx.pad, 0, scrambleKeyBuf, 0x14, 0x10);
        System.arraycopy(ctx.key, 0, resultBuf, 0, 0x10);
        
        // Encrypt with KIRK CMD 4 and XOR with result.
        for (int i = 0; i < 0x10; i++) {
            scrambleKeyBuf[0x14 + i] = (byte) (scrambleKeyBuf[0x14 + i] ^ resultBuf[i]);
        }
        ScrambleSD(scrambleKeyBuf, 0x10, seed, 0x4, 0x04);
        System.arraycopy(scrambleKeyBuf, (0x10 + 0x4) - 0x14, resultBuf, 0, 0x10);
        
        // XOR with amHashKey3.
        for (int i = 0; i < 0x10; i++) {
            resultBuf[i] = (byte) (resultBuf[i] ^ amHashKey3[i]);
        }
        
        // If mode is 2, encrypt again with KIRK CMD 5 and then KIRK CMD 4.
        if (ctx.mode == 0x2) {
            System.arraycopy(resultBuf, 0, scrambleResultBuf, 0x14, 0x10);
            ScrambleSD(scrambleResultBuf, 0x10, 0x100, 0x4, 0x05);
            System.arraycopy(scrambleResultBuf, 0, scrambleResultKeyBuf2, 0x14, 0x10);
            ScrambleSD(scrambleResultKeyBuf2, 0x10, seed, 0x4, 0x04);
            System.arraycopy(scrambleResultKeyBuf2, 0, resultBuf, 0, 0x10);
        }
        
        // XOR with the supplied key and encrypt with KIRK CMD 4.
        if (key != null) {
            for (int i = 0; i < 0x10; i++) {
                resultBuf[i] = (byte) (resultBuf[i] ^ key[i]);
            }
            System.arraycopy(resultBuf, 0, scrambleResultKeyBuf, 0x14, 0x10);
            ScrambleSD(scrambleResultKeyBuf, 0x10, seed, 0x4, 0x04);
            System.arraycopy(scrambleResultKeyBuf, 0, finalBuf, 0, 0x10);
        }
        
        // Copy back the generated hash.
        System.arraycopy(finalBuf, 0, hash, 0, 0x10);
     
        // Clear the context fields.
        ctx.mode = 0;
        ctx.padSize = 0;
        for (int i = 0; i < 0x10; i++) {
            ctx.pad[i] = 0;
        }
        for (int i = 0; i < 0x10; i++) {
            ctx.key[i] = 0;
        }     
        
        return 0;
    }
    
    private int hleDrmBBMacFinal2(BBMacCtx ctx, byte[] hash, byte[] key) {
        byte[] resBuf = new byte[0x10];
        byte[] hashBuf = new byte[0x10];
        
        int mode = ctx.mode;
        
        // Call hleDrmBBMacFinal on an empty buffer.
        hleDrmBBMacFinal(ctx, resBuf, key);
        
        // If mode is 3, decrypt the hash first.
        if ((mode & 0x3) == 0x3) {
            hashBuf = DecryptEDATAKey(hash);
        } else {
            hashBuf = hash;
        }
                      
        // Compare the hashes.
        for (int i = 0; i < 0x10; i++) {
            if (hashBuf[i] !=  resBuf[i]) {
                return -1;
            }
        }
        
        return 0;
    }
    
    private int hleDrmBBCipherInit(BBCipherCtx ctx, int encMode, int genMode, byte[] data, byte[] key) {
        // If the key is not a 16-byte key, return an error.
        if (key.length < 0x10) {
            return -1;
        }

        // Set the mode and the unknown parameters.
        ctx.mode = encMode;
        ctx.unk = 0x1;

        // Key generator mode 0x1 (encryption): use an encrypted pseudo random number before XORing the data with the given key.
        if (genMode == 0x1) {
            byte[] header = new byte[0x10 + 0x14];
            byte[] random = new byte[0x14];
            byte[] newKey = new byte[0x10];

            ByteBuffer bRandom = ByteBuffer.wrap(random);
            hleUtilsBufferCopyWithRange(bRandom, 0x14, null, 0, 0xE);

            for (int i = 0xF; i >= 0; i--) {
                newKey[0xF - i] = random[i];
            }
            System.arraycopy(newKey, 0, header, 0x14, 0x10);
            for (int i = 0; i < 4; i++) {
                header[0x20 + i] = 0;
            }

            // Encryption mode 0x1: XOR with AMCTRL keys, encrypt with KIRK CMD4 and XOR with the given key.
            if (ctx.mode == 0x1) {
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ amHashKey4[i]);
                }
                ScrambleSD(header, 0x10, 0x39, 0x4, 0x04);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ sdHashKey5[i]);
                }
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x2){ // Encryption mode 0x2: XOR with AMCTRL keys, encrypt with KIRK CMD5 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ amHashKey4[i]);
                }
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ sdHashKey5[i]);
                }
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (key != null) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else {
                // Unsupported mode.
                return -1;
            }
        } else if (genMode == 0x2) { // Key generator mode 0x02 (decryption): directly XOR the data with the given key.
            // Grab the data hash (first 16-bytes).
            System.arraycopy(data, 0, ctx.buf, 0, 0x10);
            // If the key is not null, XOR the hash with it.
            if (key != null) {
                for (int i = 0; i < 0x10; i++) {
                    ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                }
            }
            return 0;
        } else {
            // Invalid mode.
            return -1;
        }
    }

    private int hleDrmBBCipherUpdate(BBCipherCtx ctx, byte[] data, int length) {
        if (length <= 0) {
            return -1;
        }

        byte[] dataBuf = new byte[length + 0x14];
        byte[] keyBuf = new byte[0x10 + 0x10];
        byte[] hashBuf = new byte[0x10];

        // Copy the hash stored by hleSdCreateList.
        System.arraycopy(ctx.buf, 0, dataBuf, 0x14, 0x10);

        if (ctx.mode == 0x1) {
            // Decryption mode 0x01: XOR the hash with AMCTRL keys and decrypt with KIRK CMD7.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ amHashKey5[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x39, 5, 0x07);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ amHashKey4[i]);
            }
        } else if (ctx.mode == 0x2) {
            // Decryption mode 0x02: XOR the hash with AMCTRL keys and decrypt with KIRK CMD8.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ amHashKey5[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ amHashKey4[i]);
            }
        }

        // Store the calculated key.
        System.arraycopy(dataBuf, 0, keyBuf, 0x10, 0x10);

        if (ctx.unk != 0x1) {
            System.arraycopy(keyBuf, 0x10, keyBuf, 0, 0xC);
            keyBuf[0xC] = (byte) ((ctx.unk - 1) & 0xFF);
            keyBuf[0xD] = (byte) (((ctx.unk - 1) >> 8) & 0xFF);
            keyBuf[0xE] = (byte) (((ctx.unk - 1) >> 16) & 0xFF);
            keyBuf[0xF] = (byte) (((ctx.unk - 1) >> 24) & 0xFF);
        }

        // Copy the first 0xC bytes of the obtained key and replicate them
        // across a new list buffer. As a terminator, add the ctx.unk parameter's
        // 4 bytes (endian swapped) to achieve a full numbered list.
        for (int i = 0x14; i < (length + 0x14); i += 0x10) {
            System.arraycopy(keyBuf, 0x10, dataBuf, i, 0xC);
            dataBuf[i + 0xC] = (byte) (ctx.unk & 0xFF);
            dataBuf[i + 0xD] = (byte) ((ctx.unk >> 8) & 0xFF);
            dataBuf[i + 0xE] = (byte) ((ctx.unk >> 16) & 0xFF);
            dataBuf[i + 0xF] = (byte) ((ctx.unk >> 24) & 0xFF);
            ctx.unk++;
        }

        System.arraycopy(dataBuf, length + 0x04, hashBuf, 0, 0x10);

        ScrambleSD(dataBuf, length, 0x63, 5, 0x07);

        // XOR the first 16-bytes of data with the saved key to generate a new hash.
        for (int i = 0; i < 0x10; i++) {
            dataBuf[i] = (byte) (dataBuf[i] ^ keyBuf[i]);
        }

        // Copy back the last hash from the list to the first half of keyBuf.
        System.arraycopy(hashBuf, 0, keyBuf, 0, 0x10);

        // Finally, XOR the full list with the given data.
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (data[i] ^ dataBuf[i]);
        }

        return 0;
    }

    private int hleDrmBBCipherFinal(BBCipherCtx ctx) {
        ctx.mode = 0;
        ctx.unk = 0;
        for(int i = 0; i < 0x10; i++) {
            ctx.buf[i] = 0;
        }
        return 0;
    }

    /*
     *
     * User functions: crypto functions that interact with the CryptoEngine
     * at the user level. Used for PRX, SaveData,PGD and DRM encryption/decryption.
     *
     */
    
    /*
     * PRX
     */

    private TAG_INFO GetTagInfo(int tag) {
        int iTag;
        for (iTag = 0; iTag < g_tagInfo.length; iTag++) {
            if (g_tagInfo[iTag].tag == tag) {
                return g_tagInfo[iTag];
            }
        }
        return null;
    }

    private TAG_INFO_OLD GetOldTagInfo(int tag) {
        int iTag;
        for (iTag = 0; iTag < g_oldTagInfo.length; iTag++) {
            if (g_oldTagInfo[iTag].tag == tag) {
                return g_oldTagInfo[iTag];
            }
        }
        return null;
    }

    private void ScramblePRX(byte[] buf, int size, byte code) {
        // Set CBC mode.
        buf[0] = 0;
        buf[1] = 0;
        buf[2] = 0;
        buf[3] = 5;

        // Set unkown parameters to 0.
        buf[4] = 0;
        buf[5] = 0;
        buf[6] = 0;
        buf[7] = 0;

        buf[8] = 0;
        buf[9] = 0;
        buf[10] = 0;
        buf[11] = 0;

        // Set the the key seed to code.
        buf[12] = 0;
        buf[13] = 0;
        buf[14] = 0;
        buf[15] = code;

        // Set the the data size to size.
        buf[16] = (byte) ((size >> 24) & 0xFF);
        buf[17] = (byte) ((size >> 16) & 0xFF);
        buf[18] = (byte) ((size >> 8) & 0xFF);
        buf[19] = (byte) (size & 0xFF);

        ByteBuffer bBuf = ByteBuffer.wrap(buf);
        hleUtilsBufferCopyWithRange(bBuf, size, bBuf, size, 0x07);
    }

    private void ScramblePRXV2(byte[] buf, byte code) {
        byte[] tmp = new byte[0x14 + 0xA0];

        System.arraycopy(buf, 0x10, tmp, 0x14, 0xA0);

        // Set CBC mode.
        tmp[0] = 0;
        tmp[1] = 0;
        tmp[2] = 0;
        tmp[3] = 5;

        // Set unkown parameters to 0.
        tmp[4] = 0;
        tmp[5] = 0;
        tmp[6] = 0;
        tmp[7] = 0;

        tmp[8] = 0;
        tmp[9] = 0;
        tmp[10] = 0;
        tmp[11] = 0;

        // Set the the key seed to code.
        tmp[12] = 0;
        tmp[13] = 0;
        tmp[14] = 0;
        tmp[15] = code;

        // Set the the data size to 0xA0.
        tmp[16] = 0;
        tmp[17] = 0;
        tmp[18] = 0;
        tmp[19] = (byte) 0xA0;

        ByteBuffer bBuf = ByteBuffer.wrap(tmp);
        hleUtilsBufferCopyWithRange(bBuf, 0xA0, bBuf, 0xA0, 0x07);

        System.arraycopy(tmp, 0, buf, 0x10, 0xA0);
    }

    private void ScrambleSD(byte[] buf, int size, int seed, int cbc, int kirk) {
        // Set CBC mode.
        buf[0] = 0;
        buf[1] = 0;
        buf[2] = 0;
        buf[3] = (byte) cbc;

        // Set unkown parameters to 0.
        buf[4] = 0;
        buf[5] = 0;
        buf[6] = 0;
        buf[7] = 0;

        buf[8] = 0;
        buf[9] = 0;
        buf[10] = 0;
        buf[11] = 0;

        // Set the the key seed to seed.
        buf[12] = 0;
        buf[13] = 0;
        buf[14] = 0;
        buf[15] = (byte) seed;

        // Set the the data size to size.
        buf[16] = (byte) ((size >> 24) & 0xFF);
        buf[17] = (byte) ((size >> 16) & 0xFF);
        buf[18] = (byte) ((size >> 8) & 0xFF);
        buf[19] = (byte) (size & 0xFF);

        ByteBuffer bBuf = ByteBuffer.wrap(buf);
        hleUtilsBufferCopyWithRange(bBuf, size, bBuf, size, kirk);
    }

    private void PatchELFTag(byte[] buffer) {
        buffer[0] = 0x7F;
        buffer[1] = 0x45;
        buffer[2] = 0x4C;
        buffer[3] = 0x46;
        buffer[4] = 0x00;
        buffer[5] = 0x01;
        buffer[6] = 0x01;
        buffer[7] = 0x01;
        buffer[8] = 0x00;
        buffer[9] = 0x00;
        buffer[10] = 0x00;
        buffer[11] = 0x00;
        buffer[12] = 0x00;
        buffer[13] = 0x00;
        buffer[14] = 0x00;
        buffer[15] = 0x00;
    }

    public int DecryptPRX1(byte[] inbuf, byte[] outbuf, int size, int tag) {
        // Read the .PRX user tag and find it in the list.
        TAG_INFO_OLD pti = GetOldTagInfo(Integer.reverseBytes(tag));
        if (pti == null) {
            return -1;
        }

        // Check the final ELF size.
        int retsize = ((inbuf[0xB3] & 0xFF) << 24) | ((inbuf[0xB2] & 0xFF) << 16) |
                ((inbuf[0xB1] & 0xFF) << 8) | ((inbuf[0xB0] & 0xFF));

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, outbuf, 0, size);

        for (int i = 0; i < 0x150; i++) {
            outbuf[i] = 0;
        }
        for (int i = 0; i < 0x40; i++) {
            outbuf[i] = 0x55;
        }

        // Apply custom scramble.
        outbuf[0x2C + 0] = 0;
        outbuf[0x2C + 1] = 0;
        outbuf[0x2C + 2] = 0;
        outbuf[0x2C + 3] = 5;

        outbuf[0x2C + 4] = 0;
        outbuf[0x2C + 5] = 0;
        outbuf[0x2C + 6] = 0;
        outbuf[0x2C + 7] = 0;

        outbuf[0x2C + 8] = 0;
        outbuf[0x2C + 9] = 0;
        outbuf[0x2C + 10] = 0;
        outbuf[0x2C + 11] = 0;

        outbuf[0x2C + 12] = 0;
        outbuf[0x2C + 13] = 0;
        outbuf[0x2C + 14] = 0;
        outbuf[0x2C + 15] = (byte) pti.code;

        outbuf[0x2C + 16] = 0;
        outbuf[0x2C + 17] = 0;
        outbuf[0x2C + 18] = 0;
        outbuf[0x2C + 19] = (byte) 0x70;

        byte[] header = new byte[0x150];

        System.arraycopy(inbuf, 0xD0, header, 0, 0x80);
        System.arraycopy(inbuf, 0x80, header, 0x80, 0x50);
        System.arraycopy(inbuf, 0, header, 0xD0, 0x80);

        if (pti.codeExtra != 0) {
            ScramblePRXV2(header, (byte) pti.codeExtra);
        }

        System.arraycopy(header, 0x40, outbuf, 0x40, 0x40);

        for (int iXOR = 0; iXOR < 0x70; iXOR++) {
            outbuf[0x40 + iXOR] = (byte) (outbuf[0x40 + iXOR] ^ (byte) pti.key[0x14 + iXOR]);
        }

        // Scramble the data by calling CMD7.
        ByteBuffer bScrambleOut = ByteBuffer.allocate(outbuf.length);
        ByteBuffer bScrambleIn = ByteBuffer.wrap(outbuf);
        bScrambleIn.position(0x2C);
        hleUtilsBufferCopyWithRange(bScrambleOut, 0x70, bScrambleIn, 0x70, 7);
        System.arraycopy(bScrambleOut.array(), 0, outbuf, 0x2C, 0x70);

        for (int iXOR = 0x6F; iXOR >= 0; iXOR--) {
            outbuf[0x40 + iXOR] = (byte) (outbuf[0x2C + iXOR] ^ (byte) pti.key[0x20 + iXOR]);
        }

        for (int k = 0; k < 0x30; k++) {
            outbuf[k + 0x80] = 0;
        }

        // Set mode field to 1.
        outbuf[0xA0] = 0x0;
        outbuf[0xA1] = 0x0;
        outbuf[0xA2] = 0x0;
        outbuf[0xA3] = 0x1;

        System.arraycopy(inbuf, 0xB0, outbuf, 0xB0, 0x20);
        System.arraycopy(inbuf, 0, outbuf, 0xD0, 0x80);

        // Call KIRK CMD1 for final decryption.
        ByteBuffer bDataOut = ByteBuffer.wrap(outbuf);
        ByteBuffer bHeaderIn = bDataOut.duplicate();
        bHeaderIn.position(0x40);
        hleUtilsBufferCopyWithRange(bDataOut, size, bHeaderIn, size, 0x1);

        // Restore first line of ELF data (for JPCSP only).
        PatchELFTag(outbuf);

        return retsize;
    }

    public int DecryptPRX2(byte[] inbuf, byte[] outbuf, int size, int tag) {
        // Read the .PRX user tag and find it in the list.
        TAG_INFO pti = GetTagInfo(Integer.reverseBytes(tag));
        if (pti == null) {
            return -1;
        }

        // Check the final ELF size.
        int retsize = ((inbuf[0xB3] & 0xFF) << 24) | ((inbuf[0xB2] & 0xFF) << 16) |
                ((inbuf[0xB1] & 0xFF) << 8) | ((inbuf[0xB0] & 0xFF));

        // Setup all buffers.
        byte[] header = new byte[0x150];
        byte[] key = new byte[0x90 + 0x14];
        byte[] sig = new byte[0x60 + 0x14];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, outbuf, 0, size);

        // Copy the ~PSP header to header.
        System.arraycopy(outbuf, 0, header, 0, 0x150);

        // Read in the user key and apply scramble.
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 0x10; j++) {
                key[0x14 + ((i << 4) + j)] = (byte) pti.key[j];
            }
            key[0x14 + ((i << 4))] = (byte) i;
        }
        ScramblePRX(key, 0x90, (byte) (pti.code & 0xFF));

        // Regenerate sig check.
        System.arraycopy(header, 0xD0, outbuf, 0, 0x5C);
        System.arraycopy(header, 0x140, outbuf, 0x5C, 0x10);
        System.arraycopy(header, 0x12C, outbuf, 0x6C, 0x14);
        System.arraycopy(header, 0x80, outbuf, 0x80, 0x30);
        System.arraycopy(header, 0xC0, outbuf, 0xB0, 0x10);
        System.arraycopy(header, 0xB0, outbuf, 0xC0, 0x10);
        System.arraycopy(header, 0, outbuf, 0xD0, 0x80);

        // Copy sig check.
        System.arraycopy(outbuf, 0x5C, sig, 0x14, 0x60);

        // Scramble the sig.
        ScramblePRX(sig, 0x60, (byte) (pti.code & 0xFF));

        // Copy the sig again.
        System.arraycopy(sig, 0, outbuf, 0x5C, 0x60);
        System.arraycopy(outbuf, 0x6C, sig, 0, 0x14);
        System.arraycopy(outbuf, 0x5C, outbuf, 0x70, 0x10);
        for (int k = 0; k < 0x58; k++) {
            outbuf[k + 0x18] = 0;
        }
        System.arraycopy(outbuf, 0, outbuf, 0x4, 0x4);

        // Set the SHA1 block size to digest.
        outbuf[0] = 0x00;
        outbuf[1] = 0x00;
        outbuf[2] = 0x01;
        outbuf[3] = 0x4C;
        System.arraycopy(key, 0, outbuf, 0x8, 0x10);

        // Generate SHA1 hash.
        ByteBuffer bSHA1Out = ByteBuffer.wrap(outbuf);
        hleUtilsBufferCopyWithRange(bSHA1Out, size, bSHA1Out, size, 0x0B);

        // Apply XOR calculation on sig.
        for (int iXOR = 0; iXOR < 0x40; iXOR++) {
            sig[iXOR + 0x14] = (byte) (outbuf[iXOR + 0x80] ^ key[iXOR + 0x10]);
        }

        ScramblePRX(sig, 0x40, (byte) (pti.code & 0xFF));

        for (int iXOR = 0x3F; iXOR >= 0; iXOR--) {
            outbuf[iXOR + 0x40] = (byte) (sig[iXOR] ^ key[iXOR + 0x50]);
        }

        for (int k = 0; k < 0x30; k++) {
            outbuf[k + 0x80] = 0;
        }

        // Set mode field to 1.
        outbuf[0xA0] = 0x0;
        outbuf[0xA1] = 0x0;
        outbuf[0xA2] = 0x0;
        outbuf[0xA3] = 0x1;

        System.arraycopy(outbuf, 0xC0, outbuf, 0xB0, 0x10);
        for (int k = 0; k < 0x10; k++) {
            outbuf[k + 0xC0] = 0;
        }

        // Call KIRK CMD1 for final decryption.
        ByteBuffer bDataOut = ByteBuffer.wrap(outbuf);
        ByteBuffer bHeaderIn = bDataOut.duplicate();
        bHeaderIn.position(0x40);
        hleUtilsBufferCopyWithRange(bDataOut, size, bHeaderIn, size, 0x01);

        // Restore first line of ELF data (for JPCSP only).
        PatchELFTag(outbuf);

        if (retsize < 0x150) {
            for (int k = 0; k < (0x150 - retsize); k++) {
                outbuf[k + retsize] = 0;
            }
        }

        return retsize;
    }

    public int DecryptPRX3(byte[] inbuf, byte[] outbuf, int size, int tag) {
        int keyType = 0;

        // Read the .PRX user tag and find it in the list.
        TAG_INFO pti = GetTagInfo(Integer.reverseBytes(tag));
        if (pti == null) {
            return -1;
        }

        // Check the final ELF size.
        int retsize = ((inbuf[0xB3] & 0xFF) << 24) | ((inbuf[0xB2] & 0xFF) << 16) |
                ((inbuf[0xB1] & 0xFF) << 8) | ((inbuf[0xB0] & 0xFF));

        // Setup all buffers.
        byte[] header = new byte[0x150];
        byte[] key = new byte[0x90 + 0x14];
        byte[] sig = new byte[0x60 + 0x14];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, outbuf, 0, size);

        // Copy the ~PSP header to header.
        System.arraycopy(outbuf, 0, header, 0, 0x150);

        // Set keyType based on blacklisted tags.
        if (tag == 0x380280f0) {
            keyType = 3;
        } else if (tag == 0x4C9484F0) {
            keyType = 3;
        } else if (tag == 0x457b80f0) {
            keyType = 3;
        } else if (tag == 0x4C941DF0) {
            keyType = 2;
        } else if (tag == 0x4C940FF0) {
            keyType = 2;
        } else if (tag == 0x4C941CF0) {
            keyType = 2;
        } else if (tag == 0x4C940AF0) {
            keyType = 2;
        } else if (tag == 0xCFEF08F0) {
            keyType = 2;
        } else if (tag == 0xCFEF06F0) {
            keyType = 2;
        } else if (tag == 0xCFEF05F0) {
            keyType = 2;
        } else if (tag == 0x16D59E03) {
            keyType = 2;
        } else if (tag == 0x4467415D) {
            keyType = 1;
        } else if (tag == 0x00000000) {
            keyType = 0;
        } else if (tag == 0x01000000) {
            keyType = 0;
        }

        if (keyType == 2 || keyType == 3) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 0x10; j++) {
                    key[0x14 + ((i << 4) + j)] = (byte) pti.key[j];
                }
                key[0x14 + ((i << 4))] = (byte) i;
            }

        } else {
            System.arraycopy(key, 0x14, pti.key, 0, 0x90);
        }
        ScramblePRX(key, 0x90, (byte) (pti.code & 0xFF));

        // Regenerate sig check.
        if (keyType == 2 || keyType == 3) {
            System.arraycopy(header, 0xD0, outbuf, 0, 0x5C);
            System.arraycopy(header, 0x140, outbuf, 0x5C, 0x10);
            System.arraycopy(header, 0x12C, outbuf, 0x6C, 0x14);
            System.arraycopy(header, 0x80, outbuf, 0x80, 0x30);
            System.arraycopy(header, 0xC0, outbuf, 0xB0, 0x10);
            System.arraycopy(header, 0xB0, outbuf, 0xC0, 0x10);
            System.arraycopy(header, 0, outbuf, 0xD0, 0x80);
        } else {
            System.arraycopy(header, 0xD0, outbuf, 0, 0x80);
            System.arraycopy(header, 0x80, outbuf, 0x80, 0x50);
            System.arraycopy(header, 0, outbuf, 0xD0, 0x80);
        }

        if (keyType == 1) {
            // Copy sig check.
            System.arraycopy(outbuf, 0x10, sig, 0x14, 0xA0);
            // Scramble the sig.
            ScramblePRX(sig, 0xA0, (byte) (pti.code & 0xFF));
        } else if (keyType == 2 || keyType == 3) {
            // Copy sig check.
            System.arraycopy(outbuf, 0x5C, sig, 0x14, 0x60);
            // Scramble the sig.
            ScramblePRX(sig, 0x60, (byte) (pti.code & 0xFF));
        }

        if (keyType == 2 || keyType == 3) {
            // Copy the sig again.
            System.arraycopy(sig, 0, outbuf, 0x5C, 0x60);
            System.arraycopy(outbuf, 0x6C, sig, 0, 0x14);
            System.arraycopy(outbuf, 0x5C, outbuf, 0x70, 0x10);
            if (keyType == 3) {
                System.arraycopy(outbuf, 0x60, outbuf, 0x80, 0x20);
                for (int k = 0; k < 0x38; k++) {
                    outbuf[k + 0x18] = 0;
                }
            } else {
                for (int k = 0; k < 0x58; k++) {
                    outbuf[k + 0x18] = 0;
                }
            }
            System.arraycopy(outbuf, 0, outbuf, 0x4, 0x4);
            // Set the SHA1 block size to digest.
            outbuf[0] = 0x00;
            outbuf[1] = 0x00;
            outbuf[2] = 0x01;
            outbuf[3] = 0x4C;
            System.arraycopy(key, 0, outbuf, 0x8, 0x10);
        } else {
            // Set the SHA1 block size to digest.
            outbuf[0] = 0x00;
            outbuf[1] = 0x00;
            outbuf[2] = 0x01;
            outbuf[3] = 0x4C;
            System.arraycopy(key, 0, outbuf, 0x4, 0x14);
        }

        // Generate SHA1 hash.
        ByteBuffer bSHA1Out = ByteBuffer.wrap(outbuf);
        hleUtilsBufferCopyWithRange(bSHA1Out, size, bSHA1Out, size, 0x0B);

        if (keyType == 2 || keyType == 3) {
            // Apply XOR calculation on sig.
            for (int iXOR = 0; iXOR < 0x40; iXOR++) {
                sig[iXOR + 0x14] = (byte) (outbuf[iXOR + 0x80] ^ key[iXOR + 0x10]);
            }
            ScramblePRX(sig, 0x40, (byte) (pti.code & 0xFF));
            for (int iXOR = 0x3F; iXOR >= 0; iXOR--) {
                outbuf[iXOR + 0x40] = (byte) (sig[iXOR] ^ key[iXOR + 0x50]);
            }
            for (int k = 0; k < 0x30; k++) {
                outbuf[k + 0x80] = 0;
            }
            // Set mode field to 1.
            outbuf[0xA0] = 0x0;
            outbuf[0xA1] = 0x0;
            outbuf[0xA2] = 0x0;
            outbuf[0xA3] = 0x1;
            if (keyType == 3) {
                for (int k = 0; k < 0x10; k++) {
                    outbuf[k + 0xA0] = 0;
                }
                // Set mode field to 1.
                outbuf[0xA0] = 0x0;
                outbuf[0xA1] = 0x0;
                outbuf[0xA2] = 0x0;
                outbuf[0xA3] = 0x1;
                outbuf[0xA4] = 0x0;
                outbuf[0xA5] = 0x0;
                outbuf[0xA6] = 0x0;
                outbuf[0xA7] = 0x1;
            } else {
                for (int k = 0; k < 0x30; k++) {
                    outbuf[k + 0x80] = 0;
                }
                // Set mode field to 1.
                outbuf[0xA0] = 0x0;
                outbuf[0xA1] = 0x0;
                outbuf[0xA2] = 0x0;
                outbuf[0xA3] = 0x1;
            }

        } else {
            // Apply XOR calculation on sig.
            for (int iXOR = 0; iXOR < 0x70; iXOR++) {
                sig[iXOR + 0x14] = (byte) (outbuf[iXOR + 0x40] ^ key[iXOR + 0x14]);
            }
            ScramblePRX(sig, 0x70, (byte) (pti.code & 0xFF));
            for (int iXOR = 0x6F; iXOR >= 0; iXOR--) {
                outbuf[iXOR + 0x2C] = (byte) (sig[iXOR] ^ key[iXOR + 0x20]);
            }
            System.arraycopy(key, 0xB0, outbuf, 0xB0, 0xA0);

        }

        // Set mode field to 1.
        outbuf[0xA0] = 0x0;
        outbuf[0xA1] = 0x0;
        outbuf[0xA2] = 0x0;
        outbuf[0xA3] = 0x1;

        System.arraycopy(outbuf, 0xC0, outbuf, 0xB0, 0x10);
        for (int k = 0; k < 0x10; k++) {
            outbuf[k + 0xC0] = 0;
        }

        // Call KIRK CMD1 for final decryption.
        ByteBuffer bDataOut = ByteBuffer.wrap(outbuf);
        ByteBuffer bHeaderIn = bDataOut.duplicate();
        bHeaderIn.position(0x40);
        hleUtilsBufferCopyWithRange(bDataOut, size, bHeaderIn, size, 0x01);

        // Restore first line of ELF data (for JPCSP only).
        PatchELFTag(outbuf);

        if (retsize < 0x150) {
            for (int k = 0; k < (0x150 - retsize); k++) {
                outbuf[k + retsize] = 0;
            }
        }

        return retsize;
    }
    
    /*
     * SAVEDATA
     */

    public byte[] DecryptSavedata(byte[] inbuf, int size, byte[] key, int mode) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int sdEncMode = 0;
        int sdGenMode = 2;
        SDCtx1 ctx1 = new SDCtx1();
        SDCtx2 ctx2 = new SDCtx2();

        // Align the buffers to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] outbuf = new byte[alignedSize - 0x10];
        byte[] dataBuf = new byte[alignedSize];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, dataBuf, 0, size);

        // Check the crypto modes.
        if (isNullKey(key)) {
            sdEncMode = 1;
        } else if (mode == 1 || mode == 2) { // Old crypto mode (up to firmware 2.5.2).
            sdEncMode = 3;
        }

        // Call the SD functions.
        hleSdSetIndex(ctx2, sdEncMode);
        hleSdCreateList(ctx1, sdEncMode, sdGenMode, dataBuf, key);
        hleSdRemoveValue(ctx2, dataBuf, 0x10);
        System.arraycopy(dataBuf, 0x10, outbuf, 0, alignedSize - 0x10);
        hleSdRemoveValue(ctx2, outbuf, alignedSize - 0x10);
        hleSdSetMember(ctx1, outbuf, alignedSize - 0x10);

        return outbuf;
    }

    public byte[] EncryptSavedata(byte[] inbuf, int size, byte[] key, int mode) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int sdEncMode = 0;
        int sdGenMode = 1;
        SDCtx1 ctx1 = new SDCtx1();
        SDCtx2 ctx2 = new SDCtx2();

        // Align the buffers to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] outbuf = new byte[alignedSize + 0x10];
        byte[] dataBuf = new byte[alignedSize];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, dataBuf, 0, size);

        // Check the crypto modes.
        if (isNullKey(key)) {
            sdEncMode = 1;
        } else if (mode == 1 || mode == 2) { // Old crypto mode (up to firmware 2.5.2).
            sdEncMode = 3;
        }

        // Call the SD functions.
        hleSdSetIndex(ctx2, sdEncMode);
        hleSdCreateList(ctx1, sdEncMode, sdGenMode, outbuf, key);
        hleSdRemoveValue(ctx2, outbuf, 0x10);
        hleSdRemoveValue(ctx2, dataBuf, alignedSize);
        hleSdSetMember(ctx1, dataBuf, alignedSize);
        System.arraycopy(dataBuf, 0, outbuf, 0x10, alignedSize);

        return outbuf;
    }

    public void UpdateSavedataHashes(PSF psf, byte[] data, int dataSize, byte[] key, String fileName, int mode) {
        SDCtx2 ctx2 = new SDCtx2();
        byte[] savedataParams = new byte[0x80];
        byte[] savedataFileList = new byte[0xC60];
        int alignedSize = (((dataSize + 0xF) >> 4) << 4) - 0x10;
        int encMode = 4;

        if (((mode & 0x1) == 0x1) || ((mode & 0x2) == 0x2)) { // Old crypto mode (up to firmware 2.5.2).
            encMode = 2;
        }

        // Copy the fileName + fileHash to the savedataFileList buffer.
        System.arraycopy(fileName.getBytes(), 0, savedataFileList, 0, fileName.getBytes().length);
        System.arraycopy(key, 0, savedataFileList, 0xD, 0x10);

        // Generate a new hash using a blank key and encMode (2 or 4).
        hleSdSetIndex(ctx2, encMode);
        hleSdRemoveValue(ctx2, data, alignedSize);
        hleSdGetLastIndex(ctx2, key, null);

        // Store this hash at 0x20 in the savedataParams' struct.
        System.arraycopy(key, 0, savedataParams, 0x20, 0x10);
        savedataParams[0] |= 0x01;

        // If encMode is 2 or 4, calculate a new hash with a blank key, but with mode 3 or 6.
        if ((encMode & 0x2) == 0x2) {
            savedataParams[0] |= 0x20;

            hleSdSetIndex(ctx2, 3);
            hleSdRemoveValue(ctx2, data, alignedSize);
            hleSdGetLastIndex(ctx2, key, null);

            // Store this hash at 0x70 in the savedataParams' struct.
            System.arraycopy(key, 0, savedataParams, 0x70, 0x10);
        }   
        if ((encMode & 0x4) == 0x4) {
            savedataParams[0] |= 0x40;

            hleSdSetIndex(ctx2, 6);
            hleSdRemoveValue(ctx2, data, alignedSize);
            hleSdGetLastIndex(ctx2, key, null);

            // Store this hash at 0x70 in the savedataParams' struct.
            System.arraycopy(key, 0, savedataParams, 0x70, 0x10);
        }

        // Finally, generate a last hash using a blank key and mode 1.
        hleSdSetIndex(ctx2, 1);
        hleSdRemoveValue(ctx2, data, alignedSize);
        hleSdGetLastIndex(ctx2, key, null);

        // Store this hash at 0x10 in the savedataParams' struct.
        System.arraycopy(key, 0, savedataParams, 0x10, 0x10);
        
        // Output the final PSF file containing the SAVEDATA param and file hashes.
        try {
            psf.put("SAVEDATA_FILE_LIST", savedataFileList);
            psf.put("SAVEDATA_PARAMS", savedataParams);
        } catch (Exception e) {
            // Ignore...
        }
    }
    
    /*
     * PGD
     */

    public byte[] DecryptPGD(byte[] inbuf, int size, byte[] key) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int sdEncMode = 1;
        int sdGenMode = 2;
        pgdMacContext = new BBMacCtx();
        pgdCipherContext = new BBCipherCtx();

        // Align the buffers to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] outbuf = new byte[alignedSize - 0x10];
        byte[] dataBuf = new byte[alignedSize];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, dataBuf, 0, size);

        // Call the SD functions.
        hleDrmBBMacInit(pgdMacContext, sdEncMode);
        hleDrmBBCipherInit(pgdCipherContext, sdEncMode, sdGenMode, dataBuf, key);
        hleDrmBBMacUpdate(pgdMacContext, dataBuf, 0x10);
        System.arraycopy(dataBuf, 0x10, outbuf, 0, alignedSize - 0x10);
        hleDrmBBMacUpdate(pgdMacContext, outbuf, alignedSize - 0x10);
        hleDrmBBCipherUpdate(pgdCipherContext, outbuf, alignedSize - 0x10);

        return outbuf;
    }

    public byte[] UpdatePGDCipher(byte[] inbuf, int size) {
        // Align the buffers to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] outbuf = new byte[alignedSize - 0x10];
        byte[] dataBuf = new byte[alignedSize];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, dataBuf, 0, size);

        // Call the SD functions.
        System.arraycopy(dataBuf, 0x10, outbuf, 0, alignedSize - 0x10);
        hleDrmBBCipherUpdate(pgdCipherContext, outbuf, alignedSize - 0x10);

        return outbuf;
    }

    public void FinishPGDCipher() {
        // Call the SD functions.
        hleDrmBBCipherFinal(pgdCipherContext);
    }
    
    /*
     * DRM
     */
    
    public byte[] DecryptEDATAKey(byte[] key) {
        byte[] scrambleBuf = new byte[0x10 + 0x14];
        byte[] decKey = new byte[0x10];
        
        System.arraycopy(key, 0, scrambleBuf, 0x14, 0x10);
        ScrambleSD(scrambleBuf, 0x10, 0x63, 0x5, 0x07);
        System.arraycopy(scrambleBuf, 0, decKey, 0, 0x10);
        
        return decKey;
    }
    
    public int CheckEDATANameKey(byte[] nameHash, byte[] data, byte[] name, int nameLength) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int sdEncMode = 3;
        pgdMacContext = new BBMacCtx();

        int dataSize = 0x30;
        byte[] nameKey = new byte[drmNameKey.length];
        
        for(int i = 0; i < drmNameKey.length; i++) {
            nameKey[i] = (byte)(drmNameKey[i] & 0xFF);
        }
                
        // Call the BBMac functions.
        hleDrmBBMacInit(pgdMacContext, sdEncMode);
        hleDrmBBMacUpdate(pgdMacContext, data, dataSize);
        hleDrmBBMacUpdate(pgdMacContext, name, nameLength);        
        return hleDrmBBMacFinal2(pgdMacContext, nameHash, nameKey);
    }
      
    public byte[] MakeEDATAFixedKey(byte[] data, byte[] hash) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int sdEncMode = 1;
        pgdMacContext = new BBMacCtx();

        int dataSize = 0x30;
        byte[] fixedKey = new byte[drmFixedKey1.length];
        
        for(int i = 0; i < drmFixedKey1.length; i++) {
            fixedKey[i] = (byte)(drmFixedKey1[i] & 0xFF);
        }
        // Call the BBMac functions.
        hleDrmBBMacInit(pgdMacContext, sdEncMode);
        hleDrmBBMacUpdate(pgdMacContext, data, dataSize);
        hleDrmBBMacFinal(pgdMacContext, hash, fixedKey);
        
        return hash;
    }
}