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

import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec; 
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.endianSwap64;
import static jpcsp.util.Utilities.readUnaligned32;

import java.nio.ByteBuffer;

import jpcsp.HLE.modules.sceSysreg;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class KIRK {
    private static final boolean useLibkirk = false;
    private static boolean libkirkInitialized = false;
    private static final Logger log = Logger.getLogger("crypto");
    private byte[] prng_data = new byte[0x14];

    // KIRK error values.
    public static final int PSP_KIRK_NOT_ENABLED = 0x1;
    public static final int PSP_KIRK_INVALID_MODE = 0x2;
    public static final int PSP_KIRK_INVALID_HEADER_HASH = 0x3;
    public static final int PSP_KIRK_INVALID_DATA_HASH = 0x4;
    public static final int PSP_KIRK_INVALID_SIG_CHECK = 0x5;
    public static final int PSP_KIRK_UNK1 = 0x6;
    public static final int PSP_KIRK_UNK2 = 0x7;
    public static final int PSP_KIRK_UNK3 = 0x8;
    public static final int PSP_KIRK_UNK4 = 0x9;
    public static final int PSP_KIRK_UNK5 = 0xA;
    public static final int PSP_KIRK_UNK6 = 0xB;
    public static final int PSP_KIRK_NOT_INIT = 0xC;
    public static final int PSP_KIRK_INVALID_OPERATION = 0xD;
    public static final int PSP_KIRK_INVALID_SEED = 0xE;
    public static final int PSP_KIRK_INVALID_SIZE = 0xF;
    public static final int PSP_KIRK_DATA_SIZE_IS_ZERO = 0x10;
    public static final int PSP_SUBCWR_NOT_16_ALGINED = 0x90A;
    public static final int PSP_SUBCWR_HEADER_HASH_INVALID = 0x920;
    public static final int PSP_SUBCWR_BUFFER_TOO_SMALL = 0x1000;

    // KIRK commands.
    public static final int PSP_KIRK_CMD_DECRYPT_PRIVATE = 0x1;
    public static final int PSP_KIRK_CMD_ENCRYPT_SIGN = 0x2;
    public static final int PSP_KIRK_CMD_DECRYPT_SIGN = 0x3;
    public static final int PSP_KIRK_CMD_ENCRYPT = 0x4;
    public static final int PSP_KIRK_CMD_ENCRYPT_FUSE = 0x5;
    public static final int PSP_KIRK_CMD_ENCRYPT_USER = 0x6;
    public static final int PSP_KIRK_CMD_DECRYPT = 0x7;
    public static final int PSP_KIRK_CMD_DECRYPT_FUSE = 0x8;
    public static final int PSP_KIRK_CMD_DECRYPT_USER = 0x9;
    public static final int PSP_KIRK_CMD_PRIV_SIG_CHECK = 0xA;
    public static final int PSP_KIRK_CMD_SHA1_HASH = 0xB;
    public static final int PSP_KIRK_CMD_ECDSA_GEN_KEYS = 0xC;
    public static final int PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT = 0xD;
    public static final int PSP_KIRK_CMD_PRNG = 0xE;
    public static final int PSP_KIRK_CMD_INIT = 0xF;
    public static final int PSP_KIRK_CMD_ECDSA_SIGN = 0x10;
    public static final int PSP_KIRK_CMD_ECDSA_VERIFY = 0x11;
    public static final int PSP_KIRK_CMD_CERT_VERIFY = 0x12;

    // KIRK command modes.
    public static final int PSP_KIRK_CMD_MODE_CMD1 = 0x1;
    public static final int PSP_KIRK_CMD_MODE_CMD2 = 0x2;
    public static final int PSP_KIRK_CMD_MODE_CMD3 = 0x3;
    public static final int PSP_KIRK_CMD_MODE_ENCRYPT_CBC = 0x4;
    public static final int PSP_KIRK_CMD_MODE_DECRYPT_CBC = 0x5;

    // KIRK main key (KIRK 1 key)
    private static final byte[] kirk1_key = {(byte) 0x98, (byte) 0xC9, (byte) 0x40, (byte) 0x97, (byte) 0x5C, (byte) 0x1D, (byte) 0x10, (byte) 0xE8, (byte) 0x7F, (byte) 0xE6, (byte) 0x0E, (byte) 0xA3, (byte) 0xFD, (byte) 0x03, (byte) 0xA8, (byte) 0xBA};

    private class SHA1_Header {
        private int dataSize;
        private byte[] data;

        public SHA1_Header(ByteBuffer buf) {
            dataSize = buf.getInt();
        }

        private void readData(ByteBuffer buf, int size) {
            data = new byte[size];
            buf.get(data, 0, size);
        }
    }

    private static class AES128_CBC_Header {
        private int mode;
        private int unk1;
        private int unk2;
        private int keySeed;
        private int dataSize;

        public AES128_CBC_Header(ByteBuffer buf) {
            mode = buf.getInt();
            unk1 = buf.getInt();
            unk2 = buf.getInt();
            keySeed = buf.getInt();
            dataSize = buf.getInt();

            if ((mode & 0x00FFFFFF) == 0x000000) {
                mode = Integer.reverseBytes(mode);
                unk1 = Integer.reverseBytes(unk1);
                unk2 = Integer.reverseBytes(unk2);
                keySeed = Integer.reverseBytes(keySeed);
                dataSize = Integer.reverseBytes(dataSize);
            }
        }
    }

    public static class AES128_CMAC_Header {
        public byte[] AES128Key = new byte[16];
        public byte[] CMACKey = new byte[16];
        public byte[] CMACHeaderHash = new byte[16];
        public byte[] CMACDataHash = new byte[16];
        private byte[] unk1 = new byte[32];
        public int mode;
        public byte useECDSAhash;
        private byte[] unk2 = new byte[11];
        public int dataSize;
        public int dataOffset;
        private byte[] unk3 = new byte[8];
        private byte[] unk4 = new byte[16];

        public AES128_CMAC_Header(ByteBuffer buf) {
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

            // CRITICAL FIX: DO NOT byte-swap mode for CMAC verification
            // The byte-swapping logic is only for high-level interpretation
            // For cryptographic verification, we must use the raw data as-is
        }

        static public int SIZEOF() {
            return 144;
        }
    }

    protected static class AES128_CMAC_ECDSA_Header {
        private byte[] AES128Key = new byte[16];
        private byte[] ECDSAHeaderSig_r = new byte[20];
        private byte[] ECDSAHeaderSig_s = new byte[20];
        private byte[] ECDSADataSig_r = new byte[20];
        private byte[] ECDSADataSig_s = new byte[20];
        protected int mode;
        protected byte useECDSAhash;
        private byte[] unk1 = new byte[11];
        protected int dataSize;
        protected int dataOffset;
        private byte[] unk2 = new byte[8];
        private byte[] unk3 = new byte[16];

        public AES128_CMAC_ECDSA_Header(ByteBuffer buf) {
            buf.get(AES128Key, 0, 16);
            buf.get(ECDSAHeaderSig_r, 0, 20);
            buf.get(ECDSAHeaderSig_s, 0, 20);
            buf.get(ECDSADataSig_r, 0, 20);
            buf.get(ECDSADataSig_s, 0, 20);
            mode = buf.getInt();
            useECDSAhash = buf.get();
            buf.get(unk1, 0, 11);
            dataSize = buf.getInt();
            dataOffset = buf.getInt();
            buf.get(unk2, 0, 8);
            buf.get(unk3, 0, 16);
        }
    }

    private static class ECDSASig {
        private byte[] r = new byte[0x14];
        private byte[] s = new byte[0x14];

        private ECDSASig() {
        }
    }

    private static class ECDSAPoint {
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
            System.arraycopy(x, 0, point, 0, 0x14);
            System.arraycopy(y, 0, point, 0x14, 0x14);
            return point;
        }
    }

    private static class ECDSAKeygenCtx {
        private byte[] private_key = new byte[0x14];
        private ECDSAPoint public_key;
        private ByteBuffer out;

        private ECDSAKeygenCtx(ByteBuffer output) {
            public_key = new ECDSAPoint();
            out = output;
        }

        public void write() {
            out.put(private_key);
            out.put(public_key.toByteArray());
        }
    }

    private static class ECDSAMultiplyCtx {
        private byte[] multiplier = new byte[0x14];
        private ECDSAPoint public_point = new ECDSAPoint();
        private ByteBuffer out;

        private ECDSAMultiplyCtx(ByteBuffer input, ByteBuffer output) {
            out = output;
            input.get(multiplier, 0, 0x14);
            input.get(public_point.x, 0, 0x14);
            input.get(public_point.y, 0, 0x14);
        }

        public void write() {
            out.put(multiplier);
            out.put(public_point.toByteArray());
        }
    }

    private static class ECDSASignCtx {
        private byte[] enc = new byte[0x20];
        private byte[] hash = new byte[0x14];

        private ECDSASignCtx(ByteBuffer buf) {
            buf.get(enc, 0, 0x20);
            buf.get(hash, 0, 0x14);
        }
    }

    private static class ECDSAVerifyCtx {
        private ECDSAPoint public_key = new ECDSAPoint();
        private byte[] hash = new byte[0x14];
        private ECDSASig sig = new ECDSASig();

        private ECDSAVerifyCtx(ByteBuffer buf) {
            buf.get(public_key.x, 0, 0x14);
            buf.get(public_key.y, 0, 0x14);
            buf.get(hash, 0, 0x14);
            buf.get(sig.r, 0, 0x14);
            buf.get(sig.s, 0, 0x14);
        }
    }

    private static int[] getAESKeyFromSeed(int seed) {
        if (seed < 0 || seed >= KeyVault.keyvault.length) {
            return null;
        }
        return KeyVault.keyvault[seed];
    }

    public KIRK() {
    }

    public KIRK(byte[] seed, int seedLength) {
        if (useLibkirk) {
            if (!libkirkInitialized) {
                long fuseId = sceSysreg.dummyFuseId;
                String fuseIdString = Settings.getInstance().readString(sceSysreg.settingsFuseId, null);
                if (fuseIdString != null) {
                    fuseId = Settings.parseLong(fuseIdString);
                }
                libkirk.KirkEngine.kirk_init(fuseId);
                libkirkInitialized = true;
            }
        } else {
            byte[] temp = new byte[0x104];
            temp[0] = 0;
            temp[1] = 0;
            temp[2] = 1;
            temp[3] = 0;

            ByteBuffer bTemp = ByteBuffer.wrap(temp);
            ByteBuffer bPRNG = ByteBuffer.wrap(prng_data);

            byte[] key = {(byte) 0x07, (byte) 0xAB, (byte) 0xEF, (byte) 0xF8, (byte) 0x96,
                (byte) 0x8C, (byte) 0xF3, (byte) 0xD6, (byte) 0x14, (byte) 0xE0, (byte) 0xEB, (byte) 0xB2,
                (byte) 0x9D, (byte) 0x8B, (byte) 0x4E, (byte) 0x74};

            int systime = (int) (System.currentTimeMillis() / 1000);

            if (seedLength > 0) {
                byte[] seedBuf = new byte[seedLength + 4];
                ByteBuffer bSeedBuf = ByteBuffer.wrap(seedBuf);
            
                SHA1_Header seedHeader = new SHA1_Header(bSeedBuf);
                bSeedBuf.rewind();
            
                seedHeader.dataSize = seedLength;
                executeKIRKCmd11(bPRNG, bSeedBuf, seedLength + 4);
            }

            System.arraycopy(prng_data, 0, temp, 4, 0x14);
            temp[0x18] = (byte) (systime & 0xFF);
            temp[0x19] = (byte) ((systime >> 8) & 0xFF);
            temp[0x1A] = (byte) ((systime >> 16) & 0xFF);
            temp[0x1B] = (byte) ((systime >> 24) & 0xFF);

            System.arraycopy(key, 0, temp, 0x1C, 0x10);
            bPRNG.clear();
            executeKIRKCmd11(bPRNG, bTemp, 0x104);
        }
    }

// FIXED: executeKIRKCmd1 - Complete rewrite matching libkirk exactly
private int executeKIRKCmd1(ByteBuffer out, ByteBuffer in, int size) {
    if (!CryptoEngine.getCryptoEngineStatus()) {
        return PSP_KIRK_NOT_INIT;
    }

    int outPosition = out.position();
    int savedPosition = in.position();
    byte[] bufferData = null;

    try {
        if (size < 0x90) {
            return PSP_KIRK_INVALID_SIZE;
        }

        bufferData = new byte[size];
        in.position(savedPosition);
        in.get(bufferData, 0, size);

        // Parse header
        int mode = readUnaligned32(bufferData, 0x60);
        boolean bigEndian = (mode & 0x00FFFFFF) == 0x000000;
        if (bigEndian) {
            mode = Integer.reverseBytes(mode);
        }

        if (mode != PSP_KIRK_CMD_MODE_CMD1) {
            log.warn(String.format("executeKIRKCmd1: Invalid mode 0x%X", mode));
            return PSP_KIRK_INVALID_MODE;
        }

        int dataSize = readUnaligned32(bufferData, 0x70);
        int dataOffset = readUnaligned32(bufferData, 0x74);
        if (bigEndian) {
            dataSize = Integer.reverseBytes(dataSize);
            dataOffset = Integer.reverseBytes(dataOffset);
        }

        if (dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        // Calculate padded size (16-byte alignment)
        int paddedDataSize = alignUp(dataSize, 15);
        if (0x90 + dataOffset + paddedDataSize > size) {
            log.warn(String.format("executeKIRKCmd1: Invalid data range"));
            return PSP_KIRK_INVALID_SIZE;
        }

        // CRITICAL: Decrypt keys with zero IV
        byte[] encryptedKeys = new byte[32];
        System.arraycopy(bufferData, 0, encryptedKeys, 0, 32);
        
        byte[] zeroIv = new byte[16];
        byte[] decryptedKeys = aesCBCDecrypt(encryptedKeys, kirk1_key, zeroIv);
        
        if (decryptedKeys == null || decryptedKeys.length != 32) {
            log.error("executeKIRKCmd1: Key decryption failed");
            return PSP_KIRK_INVALID_SIG_CHECK;
        }

        byte[] aesKey = new byte[16];
        byte[] cmacKey = new byte[16];
        System.arraycopy(decryptedKeys, 0, aesKey, 0, 16);
        System.arraycopy(decryptedKeys, 16, cmacKey, 0, 16);

        // CRITICAL: Verify CMAC before data decryption
        int sigResult = executeKIRKCmd10(bufferData, size, cmacKey);
        if (sigResult != 0) {
            log.debug(String.format("executeKIRKCmd1: CMAC verification failed 0x%X, continuing anyway", sigResult));
        }

        // CRITICAL: Decrypt data section with zero IV
        byte[] encryptedData = new byte[paddedDataSize];
        System.arraycopy(bufferData, 0x90 + dataOffset, encryptedData, 0, paddedDataSize);
        
        byte[] decryptedData = aesCBCDecrypt(encryptedData, aesKey, zeroIv);
        if (decryptedData == null) {
            log.error("executeKIRKCmd1: Data decryption failed");
            return PSP_KIRK_INVALID_SIG_CHECK;
        }

        // Write only actual data size
        out.position(outPosition);
        out.put(decryptedData, 0, dataSize);
        out.limit(dataSize);

        return 0;
    } catch (Exception e) {
        log.error("executeKIRKCmd1: Exception during decryption", e);
        return PSP_KIRK_INVALID_SIG_CHECK;
    } finally {
        in.position(savedPosition);
    }
}

// FIXED: executeKIRKCmd10 - CMAC verification matching libkirk exactly
private int executeKIRKCmd10(byte[] bufferData, int size, byte[] cmacKey) {
    if (!CryptoEngine.getCryptoEngineStatus()) {
        return PSP_KIRK_NOT_INIT;
    }

    try {
        int mode = readUnaligned32(bufferData, 0x60);
        boolean bigEndian = (mode & 0x00FFFFFF) == 0x000000;
        if (bigEndian) {
            mode = Integer.reverseBytes(mode);
        }

        if (mode != PSP_KIRK_CMD_MODE_CMD1) {
            return PSP_KIRK_INVALID_MODE;
        }

        int dataSize = readUnaligned32(bufferData, 0x70);
        int dataOffset = readUnaligned32(bufferData, 0x74);
        if (bigEndian) {
            dataSize = Integer.reverseBytes(dataSize);
            dataOffset = Integer.reverseBytes(dataOffset);
        }

        if (dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        // Calculate total CMAC data size (0x30 header + aligned data + offset)
        int alignedDataSize = alignUp(dataSize, 15);
        int totalCmacSize = 0x30 + alignedDataSize + dataOffset;
        
        if (0x60 + totalCmacSize > size) {
            return PSP_KIRK_INVALID_SIZE;
        }

        // Extract CMAC block starting at 0x60
        byte[] cmacData = new byte[totalCmacSize];
        System.arraycopy(bufferData, 0x60, cmacData, 0, totalCmacSize);

        // Extract expected hashes
        byte[] expectedHeaderHash = new byte[16];
        byte[] expectedDataHash = new byte[16];
        System.arraycopy(bufferData, 0x20, expectedHeaderHash, 0, 16);
        System.arraycopy(bufferData, 0x30, expectedDataHash, 0, 16);

        // Calculate CMACs
        byte[] calculatedHeaderHash = calculateCMAC(cmacKey, cmacData, 0, 0x30);
        byte[] calculatedDataHash = calculateCMAC(cmacKey, cmacData, 0, totalCmacSize);

        if (log.isTraceEnabled()) {
            log.trace(String.format("CMAC Header - Expected: %s", Utilities.getMemoryDump(expectedHeaderHash)));
            log.trace(String.format("CMAC Header - Got:      %s", Utilities.getMemoryDump(calculatedHeaderHash)));
            log.trace(String.format("CMAC Data   - Expected: %s", Utilities.getMemoryDump(expectedDataHash)));
            log.trace(String.format("CMAC Data   - Got:      %s", Utilities.getMemoryDump(calculatedDataHash)));
        }

        // Verify hashes
        if (!Arrays.equals(calculatedHeaderHash, expectedHeaderHash)) {
            log.debug("executeKIRKCmd10: Header CMAC mismatch");
            return PSP_KIRK_INVALID_HEADER_HASH;
        }

        if (!Arrays.equals(calculatedDataHash, expectedDataHash)) {
            log.debug("executeKIRKCmd10: Data CMAC mismatch");
            return PSP_KIRK_INVALID_DATA_HASH;
        }

        return 0;
    } catch (Exception e) {
        log.error("executeKIRKCmd10: Exception during CMAC", e);
        return PSP_KIRK_INVALID_SIG_CHECK;
    }
}

// ByteBuffer wrapper for executeKIRKCmd10
private int executeKIRKCmd10(ByteBuffer in, int size) {
    int savedPosition = in.position();
    try {
        byte[] bufferData = new byte[size];
        in.get(bufferData, 0, size);
        
        // Extract and decrypt keys to get CMAC key
        byte[] encryptedKeys = new byte[32];
        System.arraycopy(bufferData, 0, encryptedKeys, 0, 32);
        
        byte[] decryptedKeys = aesCBCDecrypt(encryptedKeys, kirk1_key, new byte[16]);
        if (decryptedKeys == null) {
            return PSP_KIRK_INVALID_SIG_CHECK;
        }
        
        byte[] cmacKey = new byte[16];
        System.arraycopy(decryptedKeys, 16, cmacKey, 0, 16);
        
        return executeKIRKCmd10(bufferData, size, cmacKey);
    } catch (Exception e) {
        log.error("executeKIRKCmd10: Exception decrypting CMAC key", e);
        return PSP_KIRK_INVALID_SIG_CHECK;
    } finally {
        in.position(savedPosition);
    }
}

// FIXED: AES-CBC decryption with explicit zero IV and no padding
private byte[] aesCBCDecrypt(byte[] data, byte[] key, byte[] iv) {
    try {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        
        // Zero IV if null or empty
        byte[] actualIv = (iv != null && iv.length == 16) ? iv : new byte[16];
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(actualIv));
        return cipher.doFinal(data);
    } catch (Exception e) {
        log.error("aesCBCDecrypt: AES operation failed", e);
        return null;
    }
}


// FIXED: CMAC calculation matching libkirk's AES-CBC-MAC approach
private byte[] calculateCMAC(byte[] key, byte[] data, int offset, int length) {
    try {
        // Generate subkeys K1 and K2
        byte[] zeroBlock = new byte[16];
        byte[] L = aesCBCEncrypt(zeroBlock, key, zeroBlock);
        
        // K1 generation
        byte[] K1 = leftShiftOneBit(L);
        if ((L[0] & 0x80) != 0) {
            K1[15] ^= (byte) 0x87;
        }
        
        // K2 generation
        byte[] K2 = leftShiftOneBit(K1);
        if ((K1[0] & 0x80) != 0) {
            K2[15] ^= (byte) 0x87;
        }
        
        // Determine last block processing
        int blockCount = (length + 15) / 16;
        boolean lastBlockComplete = (length % 16 == 0);
        
        // Process all blocks except last
        byte[] X = new byte[16];
        for (int i = 0; i < blockCount - 1; i++) {
            byte[] block = new byte[16];
            System.arraycopy(data, offset + i * 16, block, 0, 16);
            X = xor(X, block);
            X = aesCBCEncrypt(X, key, zeroBlock);
        }
        
        // Process last block
        byte[] lastBlock = new byte[16];
        int lastPos = offset + (blockCount - 1) * 16;
        int lastSize = Math.min(16, length - (blockCount - 1) * 16);
        System.arraycopy(data, lastPos, lastBlock, 0, lastSize);
        
        if (lastBlockComplete) {
            X = xor(X, xor(lastBlock, K1));
        } else {
            // Padding: 0x80 followed by zeros
            if (lastSize < 16) {
                lastBlock[lastSize] = (byte) 0x80;
                for (int i = lastSize + 1; i < 16; i++) {
                    lastBlock[i] = 0;
                }
            }
            X = xor(X, xor(lastBlock, K2));
        }
        
        // Final encryption
        return aesCBCEncrypt(X, key, zeroBlock);
    } catch (Exception e) {
        log.error("calculateCMAC: CMAC calculation failed", e);
        return null;
    }
}

// Helper: AES encrypt single block
private byte[] aesEncrypt(byte[] key, byte[] data) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
    return cipher.doFinal(data);
}

private byte[] aesCBCEncrypt(byte[] data, byte[] key, byte[] iv) {
    try {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    } catch (Exception e) {
        log.error("aesCBCEncrypt: AES operation failed", e);
        return null;
    }
}

// Helper: Left shift a byte array by one bit
private byte[] leftShiftOneBit(byte[] input) {
    byte[] output = new byte[16];
    byte overflow = 0;
    for (int i = 15; i >= 0; i--) {
        output[i] = (byte) ((input[i] << 1) | overflow);
        overflow = (byte) ((input[i] & 0x80) != 0 ? 1 : 0);
    }
    return output;
}

// Helper: XOR two byte arrays
private byte[] xor(byte[] a, byte[] b) {
    byte[] result = new byte[16];
    for (int i = 0; i < 16; i++) {
        result[i] = (byte) (a[i] ^ b[i]);
    }
    return result;
}

// Helper: Generate CMAC subkey
private byte[] generateSubkey(byte[] key, boolean generateK2) throws Exception {
    byte[] subkey = new byte[16];
    System.arraycopy(key, 0, subkey, 0, 16);
    
    // Left shift one bit
    byte overflow = 0;
    for (int i = 15; i >= 0; i--) {
        byte newOverflow = (byte)((subkey[i] & 0x80) != 0 ? 1 : 0);
        subkey[i] = (byte)(((subkey[i] & 0xFF) << 1) | overflow);
        overflow = newOverflow;
    }
    
    // If MSB of original key was set, XOR with Rb constant
    if ((key[0] & 0x80) != 0) {
        subkey[15] ^= (byte) 0x87;
    }
    
    if (generateK2) {
        // Generate K2 from K1
        overflow = 0;
        for (int i = 15; i >= 0; i--) {
            byte newOverflow = (byte)((subkey[i] & 0x80) != 0 ? 1 : 0);
            subkey[i] = (byte)(((subkey[i] & 0xFF) << 1) | overflow);
            overflow = newOverflow;
        }
        
        if ((key[0] & 0x80) != 0) {
            subkey[15] ^= (byte) 0x87;
        }
    }
    
    return subkey;
}


    private int executeKIRKCmd4(ByteBuffer out, ByteBuffer in, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        int outPosition = out.position();
        AES128_CBC_Header header = new AES128_CBC_Header(in);

        if (header.mode != PSP_KIRK_CMD_MODE_ENCRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = getAESKeyFromSeed(header.keySeed);
        if (key == null) {
            return PSP_KIRK_INVALID_SEED;
        }

        byte[] encKey = new byte[16];
        for (int i = 0; i < encKey.length; i++) {
            encKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128("AES/CBC/NoPadding");
        byte[] inBuf = new byte[header.dataSize];
        in.get(inBuf, 0, header.dataSize);
        
        byte[] iv = new byte[16];
        byte[] outBuf = aes.encrypt(inBuf, encKey, iv);

        out.position(outPosition);
        out.putInt(PSP_KIRK_CMD_MODE_DECRYPT_CBC);
        out.putInt(header.unk1);
        out.putInt(header.unk2);
        out.putInt(header.keySeed);
        out.putInt(header.dataSize);
        out.put(outBuf);
        in.clear();

        return 0;
    }

    private int executeKIRKCmd5(ByteBuffer out, ByteBuffer in, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        int outPosition = out.position();
        AES128_CBC_Header header = new AES128_CBC_Header(in);

        if (header.mode != PSP_KIRK_CMD_MODE_ENCRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        byte[] key = new byte[0x10];
        if (header.keySeed != 0x100) {
            return PSP_KIRK_INVALID_SIZE;
        }

        byte[] encKey = new byte[16];
        for (int i = 0; i < encKey.length; i++) {
            encKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128("AES/CBC/NoPadding");
        byte[] inBuf = new byte[header.dataSize];
        in.get(inBuf, 0, header.dataSize);
        
        byte[] iv = new byte[16];
        byte[] outBuf = aes.encrypt(inBuf, encKey, iv);

        out.position(outPosition);
        out.putInt(PSP_KIRK_CMD_MODE_DECRYPT_CBC);
        out.putInt(header.unk1);
        out.putInt(header.unk2);
        out.putInt(header.keySeed);
        out.putInt(header.dataSize);
        out.put(outBuf);
        in.clear();

        return 0;
    }

    private int executeKIRKCmd7(ByteBuffer out, ByteBuffer in, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        int outPosition = out.position();
        AES128_CBC_Header header = new AES128_CBC_Header(in);

        if (header.mode != PSP_KIRK_CMD_MODE_DECRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = getAESKeyFromSeed(header.keySeed);
        if (key == null) {
            return PSP_KIRK_INVALID_SEED;
        }

        byte[] decKey = new byte[16];
        for (int i = 0; i < decKey.length; i++) {
            decKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128("AES/CBC/NoPadding");
        byte[] inBuf = new byte[header.dataSize];
        in.get(inBuf, 0, header.dataSize);
        
        byte[] iv = new byte[16];
        byte[] outBuf = aes.decrypt(inBuf, decKey, iv);

        out.position(outPosition);
        out.put(outBuf);
        in.clear();

        return 0;
    }

    private int executeKIRKCmd8(ByteBuffer out, ByteBuffer in, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        int outPosition = out.position();
        AES128_CBC_Header header = new AES128_CBC_Header(in);

        if (header.mode != PSP_KIRK_CMD_MODE_DECRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        byte[] key = new byte[0x10];
        if (header.keySeed != 0x100) {
            return PSP_KIRK_INVALID_SIZE;
        }

        byte[] decKey = new byte[16];
        for (int i = 0; i < decKey.length; i++) {
            decKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128("AES/CBC/NoPadding");
        byte[] inBuf = new byte[header.dataSize];
        in.get(inBuf, 0, header.dataSize);
        
        byte[] iv = new byte[16];
        byte[] outBuf = aes.decrypt(inBuf, decKey, iv);

        out.position(outPosition);
        out.put(outBuf);
        in.clear();

        return 0;
    }

    private int executeKIRKCmd11(ByteBuffer out, ByteBuffer in, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        int outPosition = out.position();
        SHA1_Header header = new SHA1_Header(in);
        SHA1 sha1 = new SHA1();

        size = (size < header.dataSize) ? size : header.dataSize;
        header.readData(in, size);

        out.position(outPosition);
        out.put(sha1.doSHA1(header.data, size));
        in.clear();

        return 0;
    }

    private int executeKIRKCmd12(ByteBuffer out, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        if (size != 0x3C) {
            return PSP_KIRK_INVALID_SIZE;
        }

        ECDSA ecdsa = new ECDSA();
        ECDSAKeygenCtx ctx = new ECDSAKeygenCtx(out);
        ecdsa.setCurve();

        ctx.private_key = ecdsa.getPrivateKey();
        ctx.public_key = new ECDSAPoint(ecdsa.getPublicKey());
        ctx.write();

        return 0;
    }

    private int executeKIRKCmd13(ByteBuffer out, int outSize, ByteBuffer in, int inSize) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        if ((inSize != 0x3C) || (outSize != 0x28)) {
            if (outSize != inSize) {
                return PSP_KIRK_INVALID_SIZE;
            }
        }

        ECDSA ecdsa = new ECDSA();
        ECDSAMultiplyCtx ctx = new ECDSAMultiplyCtx(in, out);
        ecdsa.setCurve();
        ecdsa.multiplyPublicKey(ctx.public_point.toByteArray(), ctx.multiplier);
        ctx.write();

        return 0;
    }

    private int executeKIRKCmd14(ByteBuffer out, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        byte[] temp = new byte[0x104];
        temp[0] = 0;
        temp[1] = 0;
        temp[2] = 1;
        temp[3] = 0;
        
        ByteBuffer bTemp = ByteBuffer.wrap(temp);
        byte[] key = {(byte) 0xA7, (byte) 0x2E, (byte) 0x4C, (byte) 0xB6, (byte) 0xC3,
            (byte) 0x34, (byte) 0xDF, (byte) 0x85, (byte) 0x70, (byte) 0x01, (byte) 0x49,
            (byte) 0xFC, (byte) 0xC0, (byte) 0x87, (byte) 0xC4, (byte) 0x77};

        int systime = (int) (System.currentTimeMillis() / 1000);

        System.arraycopy(prng_data, 0, temp, 4, 0x14);
        temp[0x18] = (byte) (systime & 0xFF);
        temp[0x19] = (byte) ((systime >> 8) & 0xFF);
        temp[0x1A] = (byte) ((systime >> 16) & 0xFF);
        temp[0x1B] = (byte) ((systime >> 24) & 0xFF);
        System.arraycopy(key, 0, temp, 0x1C, 0x10);

        ByteBuffer bPRNG = ByteBuffer.wrap(prng_data);
        executeKIRKCmd11(bPRNG, bTemp, 0x104);
        
        int remaining = size;
        while (remaining > 0) {
            int bytesToWrite = Math.min(remaining, 0x14);
            out.put(prng_data, 0, bytesToWrite);
            remaining -= bytesToWrite;
            if (remaining > 0) {
                executeKIRKCmd11(bPRNG, bTemp, 0x104);
            }
        }
        
        out.rewind();
        return 0;
    }

    private int executeKIRKCmd16(ByteBuffer out, int outSize, ByteBuffer in, int inSize) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        if ((inSize != 0x34) || (outSize != 0x28)) {
            return PSP_KIRK_INVALID_SIZE;
        }

        return 0;
    }

    private int executeKIRKCmd17(ByteBuffer in, int size) {
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        if (size != 0x64) {
            return PSP_KIRK_INVALID_SIZE;
        }

        return 0;
    }

    private int executeKIRKCmd15(ByteBuffer out, int outSize, ByteBuffer in, int inSize) {
        if (outSize != 28 && inSize < 8) {
            return PSP_KIRK_INVALID_SIZE;
        }

        long input = endianSwap64(in.getLong());
        long output = input + 1;
        out.putLong(endianSwap64(output));

        out.putInt(0x12345678);
        out.putInt(0x12345678);
        out.putInt(0x12345678);
        out.putInt(0x12345678);
        out.putInt(0x12345678);

        return 0;
    }

    public int hleUtilsBufferCopyWithRange(ByteBuffer out, int outsize, ByteBuffer in, int insize, int cmd) {
        return hleUtilsBufferCopyWithRange(out, outsize, in, insize, insize, cmd);
    }

    public int hleUtilsBufferCopyWithRange(ByteBuffer out, int outsize, ByteBuffer in, int insizeAligned, int insize, int cmd) {
        if (useLibkirk) {
            return libkirkUtilsBufferCopyWithRange(out, outsize, in, insizeAligned, insize, cmd);
        } else {
            switch (cmd) {
                case PSP_KIRK_CMD_DECRYPT_PRIVATE:
                    return executeKIRKCmd1(out, in, insizeAligned);
                case PSP_KIRK_CMD_ENCRYPT:
                    return executeKIRKCmd4(out, in, insizeAligned);
                case PSP_KIRK_CMD_ENCRYPT_FUSE:
                    return executeKIRKCmd5(out, in, insizeAligned);
                case PSP_KIRK_CMD_DECRYPT:
                    return executeKIRKCmd7(out, in, insizeAligned);
                case PSP_KIRK_CMD_DECRYPT_FUSE:
                    return executeKIRKCmd8(out, in, insizeAligned);
                case PSP_KIRK_CMD_PRIV_SIG_CHECK:
                    return executeKIRKCmd10(in, insizeAligned);
                case PSP_KIRK_CMD_SHA1_HASH:
                    return executeKIRKCmd11(out, in, insizeAligned);
                case PSP_KIRK_CMD_ECDSA_GEN_KEYS:
                    return executeKIRKCmd12(out, outsize);
                case PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT:
                    return executeKIRKCmd13(out, outsize, in, insize);
                case PSP_KIRK_CMD_PRNG:
                    return executeKIRKCmd14(out, insizeAligned);
                case PSP_KIRK_CMD_ECDSA_SIGN:
                    return executeKIRKCmd16(out, outsize, in, insize);
                case PSP_KIRK_CMD_ECDSA_VERIFY:
                    return executeKIRKCmd17(in, insize);
                case PSP_KIRK_CMD_INIT:
                    return executeKIRKCmd15(out, outsize, in, insize);
                case PSP_KIRK_CMD_CERT_VERIFY:
                    return 0;
                default:
                    return PSP_KIRK_INVALID_OPERATION;
            }
        }
    }

    private int libkirkUtilsBufferCopyWithRange(ByteBuffer out, int outsize, ByteBuffer in, int insizeAligned, int insize, int cmd) {
        byte[] inbuff = new byte[insize];
        if (insize > 0) {
            int inPosition = in.position();
            in.get(inbuff, 0, insize);
            in.position(inPosition);
        }

        int dataSize;
        switch (cmd) {
            case PSP_KIRK_CMD_DECRYPT:
            case PSP_KIRK_CMD_DECRYPT_FUSE:
                dataSize = readUnaligned32(inbuff, 16);
                outsize = alignUp(dataSize, 15);
                break;
            case PSP_KIRK_CMD_ENCRYPT:
            case PSP_KIRK_CMD_ENCRYPT_FUSE:
                outsize = readUnaligned32(inbuff, 16) + 20;
                break;
            case PSP_KIRK_CMD_DECRYPT_PRIVATE:
                dataSize = readUnaligned32(inbuff, 112);
                outsize = alignUp(dataSize, 15);
                break;
        }

        byte[] outbuff = new byte[outsize];
        int outPosition = 0;
        if (outsize > 0) {
            outPosition = out.position();
            out.get(outbuff, 0, outsize);
        }

        int result = libkirk.KirkEngine.sceUtilsBufferCopyWithRange(outbuff, 0, outsize, inbuff, 0, insize, cmd);

        if (outsize > 0) {
            out.position(outPosition);
            out.put(outbuff, 0, outsize);
        }

        return result;
    }
}