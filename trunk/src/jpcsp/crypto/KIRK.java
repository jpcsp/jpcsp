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

public class KIRK {
    
    // PSP specific values.
    private int[] fuseID = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};
    private byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    
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
    public static final int PSP_KIRK_CMD_DECRYPT_PRIVATE = 0x1;         // Master decryption command, used by firmware modules. Applies CMAC checking.
    public static final int PSP_KIRK_CMD_ENCRYPT_SIGN = 0x2;            // Used for key type 3 (blacklisting), encrypts and signs data with a ECDSA signature.
    public static final int PSP_KIRK_CMD_DECRYPT_SIGN = 0x3;            // Used for key type 3 (blacklisting), decrypts and signs data with a ECDSA signature.
    public static final int PSP_KIRK_CMD_ENCRYPT = 0x4;                 // Key table based encryption used for general purposes by several modules.
    public static final int PSP_KIRK_CMD_ENCRYPT_FUSE = 0x5;            // Fuse ID based encryption used for general purposes by several modules.
    public static final int PSP_KIRK_CMD_ENCRYPT_USER = 0x6;            // User specified ID based encryption used for general purposes by several modules.
    public static final int PSP_KIRK_CMD_DECRYPT = 0x7;                 // Key table based decryption used for general purposes by several modules.
    public static final int PSP_KIRK_CMD_DECRYPT_FUSE = 0x8;            // Fuse ID based decryption used for general purposes by several modules.
    public static final int PSP_KIRK_CMD_DECRYPT_USER = 0x9;            // User specified ID based decryption used for general purposes by several modules.
    public static final int PSP_KIRK_CMD_PRIV_SIG_CHECK = 0xA;          // Private signature (SCE) checking command.
    public static final int PSP_KIRK_CMD_SHA1_HASH = 0xB;               // SHA1 hash generating command.
    public static final int PSP_KIRK_CMD_ECDSA_GEN_KEYS = 0xC;          // ECDSA key generating mul1 command. 
    public static final int PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT = 0xD;    // ECDSA key generating mul2 command. 
    public static final int PSP_KIRK_CMD_PRNG = 0xE;                    // Random number generating command. 
    public static final int PSP_KIRK_CMD_INIT = 0xF;                    // KIRK initialization command.
    public static final int PSP_KIRK_CMD_ECDSA_SIGN = 0x10;             // ECDSA signing command.
    public static final int PSP_KIRK_CMD_ECDSA_VERIFY = 0x11;           // ECDSA checking command.
    public static final int PSP_KIRK_CMD_CERT_VERIFY = 0x12;            // Certificate checking command.
    
    // KIRK command modes.
    public static final int PSP_KIRK_CMD_MODE_CMD1 = 0x1;
    public static final int PSP_KIRK_CMD_MODE_CMD2 = 0x2;
    public static final int PSP_KIRK_CMD_MODE_CMD3 = 0x3;
    public static final int PSP_KIRK_CMD_MODE_ENCRYPT_CBC = 0x4;
    public static final int PSP_KIRK_CMD_MODE_DECRYPT_CBC = 0x5;
    
    // KIRK header structs.
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

    private class AES128_CBC_Header {

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
        }
    }

    private class AES128_CMAC_Header {

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
        }
    }

    private class AES128_CMAC_ECDSA_Header {

        private byte[] AES128Key = new byte[16];
        private byte[] ECDSAHeaderSig_r = new byte[20];
        private byte[] ECDSAHeaderSig_s = new byte[20];
        private byte[] ECDSADataSig_r = new byte[20];
        private byte[] ECDSADataSig_s = new byte[20];
        private int mode;
        private byte useECDSAhash;
        private byte[] unk1 = new byte[11];
        private int dataSize;
        private int dataOffset;
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

    private class ECDSASig {

        private byte[] r = new byte[0x14];
        private byte[] s = new byte[0x14];

        private ECDSASig() {
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

        private ECDSAKeygenCtx(ByteBuffer output) {
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

        private ECDSAMultiplyCtx(ByteBuffer input, ByteBuffer output) {
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

        private ECDSASignCtx(ByteBuffer buf) {
            buf.get(enc, 0, 0x20);
            buf.get(hash, 0, 0x14);
        }
    }

    private class ECDSAVerifyCtx {

        private ECDSAPoint public_key;
        private byte[] hash = new byte[0x14];
        private ECDSASig sig;

        private ECDSAVerifyCtx(ByteBuffer buf) {
            buf.get(public_key.x, 0, 0x14);
            buf.get(public_key.y, 0, 0x14);
            buf.get(hash, 0, 0x14);
            buf.get(sig.r, 0, 0x14);
            buf.get(sig.s, 0, 0x14);
        }
    }
    
    // Helper functions.
    private int[] getAESKeyFromSeed(int seed) {
        switch (seed) {
            case (0x02):
                return KeyVault.kirkAESKey20;
            case (0x03):
                return KeyVault.kirkAESKey1;
            case (0x04):
                return KeyVault.kirkAESKey2;
            case (0x05):
                return KeyVault.kirkAESKey3;
            case (0x07):
                return KeyVault.kirkAESKey21;
            case (0x0C):
                return KeyVault.kirkAESKey4;
            case (0x0D):
                return KeyVault.kirkAESKey5;
            case (0x0E):
                return KeyVault.kirkAESKey6;
            case (0x0F):
                return KeyVault.kirkAESKey7;
            case (0x10):
                return KeyVault.kirkAESKey8;
            case (0x11):
                return KeyVault.kirkAESKey9;
            case (0x12):
                return KeyVault.kirkAESKey10;
            case (0x38):
                return KeyVault.kirkAESKey11;
            case (0x39):
                return KeyVault.kirkAESKey12;
            case (0x3A):
                return KeyVault.kirkAESKey13;
            case (0x44):
                return KeyVault.kirkAESKey22;
            case (0x4B):
                return KeyVault.kirkAESKey14;
            case (0x53):
                return KeyVault.kirkAESKey15;
            case (0x57):
                return KeyVault.kirkAESKey16;
            case (0x5D):
                return KeyVault.kirkAESKey17;
            case (0x63):
                return KeyVault.kirkAESKey18;
            case (0x64):
                return KeyVault.kirkAESKey19;
            default:
                return null;
        }
    }
    
    public KIRK() {
        
    }

    /*
     * KIRK commands: main emulated crypto functions.
     */
    
    // Decrypt with AESCBC128-CMAC header and sig check.
    private int executeKIRKCmd1(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        // Copy the input for sig check.
        ByteBuffer sigIn = in.duplicate();

        // Read in the CMD1 format header.
        AES128_CMAC_Header header = new AES128_CMAC_Header(in);

        if (header.mode != PSP_KIRK_CMD_MODE_CMD1) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode CMD1.
        }

        // Start AES128 processing.
        AES128 aes = new AES128("AES/CBC/NoPadding");

        // Convert the AES CMD1 key into a real byte array for SecretKeySpec.
        byte[] k = new byte[16];
        for (int i = 0; i < KeyVault.kirkAESKey0.length; i++) {
            k[i] = (byte) KeyVault.kirkAESKey0[i];
        }

        // Decrypt and extract the new AES and CMAC keys from the top of the data.
        byte[] encryptedKeys = new byte[32];
        System.arraycopy(header.AES128Key, 0, encryptedKeys, 0, 16);
        System.arraycopy(header.CMACKey, 0, encryptedKeys, 16, 16);
        byte[] decryptedKeys = aes.decrypt(encryptedKeys, k, iv);

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
        byte[] outBuf = aes.decrypt(inBuf, aesBuf, iv);

        out.clear();
        out.put(outBuf);
        out.limit(elfDataSize);
        in.clear();

        return 0;
    }

    // Encrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd4(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD4 format header.
        AES128_CBC_Header header = new AES128_CBC_Header(in);

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

        AES128 aes = new AES128("AES/CBC/NoPadding");

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.encrypt(inBuf, encKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // Encrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd5(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD4 format header.
        AES128_CBC_Header header = new AES128_CBC_Header(in);

        if (header.mode != PSP_KIRK_CMD_MODE_ENCRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode ENCRYPT_CBC.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = null;
        if (header.keySeed == 0x100) {
            key = fuseID;
        } else {
            return PSP_KIRK_INVALID_SIZE; // Dummy.
        }

        byte[] encKey = new byte[16];
        for (int i = 0; i < encKey.length; i++) {
            encKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128("AES/CBC/NoPadding");

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.encrypt(inBuf, encKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // Decrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd7(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD7 format header.
        AES128_CBC_Header header = new AES128_CBC_Header(in);

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

        AES128 aes = new AES128("AES/CBC/NoPadding");

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.decrypt(inBuf, decKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // Decrypt with AESCBC128 using keys from table.
    private int executeKIRKCmd8(ByteBuffer out, ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD7 format header.
        AES128_CBC_Header header = new AES128_CBC_Header(in);

        if (header.mode != PSP_KIRK_CMD_MODE_DECRYPT_CBC) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for mode DECRYPT_CBC.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        int[] key = null;
        if (header.keySeed == 0x100) {
            key = fuseID;
        } else {
            return PSP_KIRK_INVALID_SIZE; // Dummy.
        }

        byte[] decKey = new byte[16];
        for (int i = 0; i < decKey.length; i++) {
            decKey[i] = (byte) key[i];
        }

        AES128 aes = new AES128("AES/CBC/NoPadding");

        byte[] inBuf = new byte[size];
        in.get(inBuf, 0, size);
        byte[] outBuf = aes.decrypt(inBuf, decKey, iv);

        out.clear();
        out.put(outBuf);
        in.clear();

        return 0;
    }

    // CMAC Sig check.
    private int executeKIRKCmd10(ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        // Read in the CMD10 format header.
        AES128_CMAC_Header header = new AES128_CMAC_Header(in);
        if ((header.mode != PSP_KIRK_CMD_MODE_CMD1)
                && (header.mode != PSP_KIRK_CMD_MODE_CMD2)
                && (header.mode != PSP_KIRK_CMD_MODE_CMD3)) {
            return PSP_KIRK_INVALID_MODE;  // Only valid for modes CMD1, CMD2 and CMD3.
        }

        if (header.dataSize == 0) {
            return PSP_KIRK_DATA_SIZE_IS_ZERO;
        }

        AES128 aes = new AES128("AES/CBC/NoPadding");

        // Convert the AES CMD1 key into a real byte array.
        byte[] k = new byte[16];
        for (int i = 0; i < KeyVault.kirkAESKey0.length; i++) {
            k[i] = (byte) KeyVault.kirkAESKey0[i];
        }

        // Decrypt and extract the new AES and CMAC keys from the top of the data.
        byte[] encryptedKeys = new byte[32];
        System.arraycopy(header.AES128Key, 0, encryptedKeys, 0, 16);
        System.arraycopy(header.CMACKey, 0, encryptedKeys, 16, 16);
        byte[] decryptedKeys = aes.decrypt(encryptedKeys, k, iv);

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
        if (!CryptoEngine.getCryptoEngineStatus()) {
            return PSP_KIRK_NOT_INIT;
        }

        SHA1_Header header = new SHA1_Header(in);
        SHA1 sha1 = new SHA1();

        size = (size < header.dataSize) ? size : header.dataSize;
        header.readData(in, size);

        out.clear();
        out.put(sha1.doSHA1(header.data, size));
        in.clear();

        return 0;
    }
    
    // Generate ECDSA key pair.
    private int executeKIRKCmd12(ByteBuffer out, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
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

    // Multiply ECDSA point.
    private int executeKIRKCmd13(ByteBuffer out, int outSize, ByteBuffer in, int inSize) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
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
        if (!CryptoEngine.getCryptoEngineStatus()) {
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

    // Sign data with ECDSA key pair.
    private int executeKIRKCmd16(ByteBuffer out, int outSize, ByteBuffer in, int inSize) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
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

    // Verify ECDSA signature.
    private int executeKIRKCmd17(ByteBuffer in, int size) {
        // Return an error if the crypto engine hasn't been initialized.
        if (!CryptoEngine.getCryptoEngineStatus()) {
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
}