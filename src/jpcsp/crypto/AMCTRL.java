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

public class AMCTRL {
    
    private static KIRK kirk;
    
    public AMCTRL() {
        kirk = new KIRK();
    }
    
    // AMCTRL context structs.
    public static class BBCipher_Ctx {

        private int mode;
        private int seed;
        private byte[] buf = new byte[16];

        public BBCipher_Ctx() {
            mode = 0;
            seed = 0;
        }
    }

    public static class BBMac_Ctx {

        private int mode;
        private byte[] key = new byte[16];
        private byte[] pad = new byte[16];
        private int padSize;

        public BBMac_Ctx() {
            mode = 0;
            padSize = 0;
        }
    }
    
    private void ScrambleBB(byte[] buf, int size, int seed, int cbc, int kirk_code) {
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
        kirk.hleUtilsBufferCopyWithRange(bBuf, size, bBuf, size, kirk_code);
    }
    
    public byte[] DecryptBBMacKey(byte[] key) {
        byte[] scrambleBuf = new byte[0x10 + 0x14];
        byte[] decKey = new byte[0x10];

        System.arraycopy(key, 0, scrambleBuf, 0x14, 0x10);
        ScrambleBB(scrambleBuf, 0x10, 0x63, 0x5, 0x07);
        System.arraycopy(scrambleBuf, 0, decKey, 0, 0x10);

        return decKey;
    }

    public int GetKeyFromBBMac(BBMac_Ctx ctx, byte[] bbmac, byte[] key) {
        byte[] tmpBuf = new byte[0x10];
        byte[] scrambleBuf = new byte[0x10 + 0x14];
        byte[] decKey = new byte[0x10];
        byte[] scrambleDecBuf = new byte[0x10 + 0x14];
        byte[] finalKey = new byte[0x10];

        hleDrmBBMacFinal(ctx, tmpBuf, null);

        if ((ctx.mode & 0x3) == 0x3) {
            System.arraycopy(bbmac, 0, scrambleBuf, 0x14, 0x10);
            ScrambleBB(scrambleBuf, 0x10, 0x63, 0x5, 0x07);
            System.arraycopy(scrambleBuf, 0, decKey, 0, 0x10);
        } else {
            System.arraycopy(bbmac, 0, decKey, 0, 0x10);
        }

        int seed = 0x38;
        if ((ctx.mode & 0x2) == 0x2) {
            seed = 0x3A;
        }

        System.arraycopy(decKey, 0, scrambleDecBuf, 0x14, 0x10);
        ScrambleBB(scrambleDecBuf, 0x10, seed, 0x5, 0x07);
        System.arraycopy(scrambleDecBuf, 0, finalKey, 0, 0x10);

        for (int i = 0; i < 0x10; i++) {
            key[i] = (byte) (tmpBuf[i] ^ finalKey[i]);
        }

        return 0;
    }
    
    /*
     * sceDrmBB - amctrl.prx
     */
    public int hleDrmBBMacInit(BBMac_Ctx ctx, int encMode) {
        // Set all parameters to 0 and assign the encMode.
        ctx.mode = encMode;
        return 0;
    }

    public int hleDrmBBMacUpdate(BBMac_Ctx ctx, byte[] data, int length) {
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
            if (ctx.padSize == 0) {
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
                ScrambleBB(scrambleBuf, blockSize, seed, 0x4, 0x04);
                System.arraycopy(scrambleBuf, (blockSize + 0x4) - 0x14, ctx.key, 0, 0x10);

                // Adjust data length, data offset and reset any key length.
                length -= (blockSize - kLen);
                dataOffset += (blockSize - kLen);
                kLen = 0;
            }

            return 0;
        }
    }

    public int hleDrmBBMacFinal(BBMac_Ctx ctx, byte[] hash, byte[] key) {
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

        // Calculate the seed (mode 2 == 0x3A / mode 1 and 3 == 0x3A).
        int seed = 0x38;
        if (ctx.mode == 0x2) {
            seed = 0x3A;
        }

        // Encrypt an empty buffer with KIRK CMD 4.
        ScrambleBB(scrambleEmptyBuf, 0x10, seed, 0x4, 0x04);
        System.arraycopy(scrambleEmptyBuf, 0, keyBuf, 0, 0x10);

        // Apply custom padding management.
        int b, b1, b2, b3;

        b = ((keyBuf[0] & 0x80) == 0x80) ? 0x87 : 0;
        for (int i = 0; i < 0xF; i++) {
            b1 = (keyBuf[i + 0] & 0xFF);
            b2 = (keyBuf[i + 1] & 0xFF);
            b1 = ((b1 << 1) & 0xFF);
            b2 = ((b2 >> 7) & 0xFF);
            b2 = ((b2 | b1) & 0xFF);
            keyBuf[i + 0] = (byte) (b2 & 0xFF);
        }
        b3 = (keyBuf[0xF] & 0xFF);
        b3 = ((b3 << 1) & 0xFF);
        b3 = ((b3 ^ b) & 0xFF);
        keyBuf[0xF] = (byte) (b3 & 0xFF);

        if (ctx.padSize < 0x10) {
            b = ((keyBuf[0] & 0x80) == 0x80) ? 0x87 : 0;
            for (int i = 0; i < 0xF; i++) {
                b1 = (keyBuf[i + 0] & 0xFF);
                b2 = (keyBuf[i + 1] & 0xFF);
                b1 = ((b1 << 1) & 0xFF);
                b2 = ((b2 >> 7) & 0xFF);
                b2 = ((b2 | b1) & 0xFF);
                keyBuf[i + 0] = (byte) (b2 & 0xFF);
            }
            b3 = (keyBuf[0xF] & 0xFF);
            b3 = ((b3 << 1) & 0xFF);
            b3 = ((b3 ^ b) & 0xFF);
            keyBuf[0xF] = (byte) (b3 & 0xFF);

            ctx.pad[ctx.padSize] = (byte) 0x80;
            if ((ctx.padSize + 1) < 0x10) {
                for (int i = 0; i < (0x10 - ctx.padSize - 1); i++) {
                    ctx.pad[ctx.padSize + 1 + i] = 0;
                }
            }
        }

        // XOR pad.
        for (int i = 0; i < 0x10; i++) {
            ctx.pad[i] = (byte) ((ctx.pad[i] & 0xFF) ^ (keyBuf[i] & 0xFF));
        }

        System.arraycopy(ctx.pad, 0, scrambleKeyBuf, 0x14, 0x10);
        System.arraycopy(ctx.key, 0, resultBuf, 0, 0x10);

        // Encrypt with KIRK CMD 4 and XOR with result.
        for (int i = 0; i < 0x10; i++) {
            scrambleKeyBuf[0x14 + i] = (byte) (scrambleKeyBuf[0x14 + i] ^ resultBuf[i]);
        }
        ScrambleBB(scrambleKeyBuf, 0x10, seed, 0x4, 0x04);
        System.arraycopy(scrambleKeyBuf, (0x10 + 0x4) - 0x14, resultBuf, 0, 0x10);

        // XOR with amHashKey3.
        for (int i = 0; i < 0x10; i++) {
            resultBuf[i] = (byte) (resultBuf[i] ^ KeyVault.amHashKey3[i]);
        }

        // If mode is 2, encrypt again with KIRK CMD 5 and then KIRK CMD 4.
        if (ctx.mode == 0x2) {
            System.arraycopy(resultBuf, 0, scrambleResultBuf, 0x14, 0x10);
            ScrambleBB(scrambleResultBuf, 0x10, 0x100, 0x4, 0x05);
            System.arraycopy(scrambleResultBuf, 0, scrambleResultKeyBuf2, 0x14, 0x10);
            ScrambleBB(scrambleResultKeyBuf2, 0x10, seed, 0x4, 0x04);
            System.arraycopy(scrambleResultKeyBuf2, 0, resultBuf, 0, 0x10);
        }

        // XOR with the supplied key and encrypt with KIRK CMD 4.
        if (key != null) {
            for (int i = 0; i < 0x10; i++) {
                resultBuf[i] = (byte) (resultBuf[i] ^ key[i]);
            }
            System.arraycopy(resultBuf, 0, scrambleResultKeyBuf, 0x14, 0x10);
            ScrambleBB(scrambleResultKeyBuf, 0x10, seed, 0x4, 0x04);
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

    public int hleDrmBBMacFinal2(BBMac_Ctx ctx, byte[] hash, byte[] key) {
        byte[] resBuf = new byte[0x10];
        byte[] hashBuf = new byte[0x10];

        int mode = ctx.mode;

        // Call hleDrmBBMacFinal on an empty buffer.
        hleDrmBBMacFinal(ctx, resBuf, key);

        // If mode is 3, decrypt the hash first.
        if ((mode & 0x3) == 0x3) {
            hashBuf = DecryptBBMacKey(hash);
        } else {
            hashBuf = hash;
        }

        // Compare the hashes.
        for (int i = 0; i < 0x10; i++) {
            if (hashBuf[i] != resBuf[i]) {
                return -1;
            }
        }

        return 0;
    }

    public int hleDrmBBCipherInit(BBCipher_Ctx ctx, int encMode, int genMode, byte[] data, byte[] key, int seed) {
        // If the key is not a 16-byte key, return an error.
        if (key.length < 0x10) {
            return -1;
        }

        // Set the mode and the unknown parameters.
        ctx.mode = encMode;
        ctx.seed = 0x1;

        // Key generator mode 0x1 (encryption): use an encrypted pseudo random number before XORing the data with the given key.
        if (genMode == 0x1) {
            byte[] header = new byte[0x10 + 0x14];
            byte[] random = new byte[0x14];
            byte[] newKey = new byte[0x10];

            ctx.seed = 1;

            ByteBuffer bRandom = ByteBuffer.wrap(random);
            kirk.hleUtilsBufferCopyWithRange(bRandom, 0x14, null, 0, 0xE);

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
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ KeyVault.amHashKey4[i]);
                }
                ScrambleBB(header, 0x10, 0x39, 0x4, 0x04);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ KeyVault.amHashKey5[i]);
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
            } else if (ctx.mode == 0x2) { // Encryption mode 0x2: XOR with AMCTRL keys, encrypt with KIRK CMD5 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + i] = (byte) (header[0x14 + i] ^ KeyVault.amHashKey4[i]);
                }
                ScrambleBB(header, 0x10, 0x100, 0x4, 0x05);
                for (int i = 0; i < 0x10; i++) {
                    header[i] = (byte) (header[i] ^ KeyVault.amHashKey5[i]);
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
            ctx.seed = seed + 1;
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

    public int hleDrmBBCipherUpdate(BBCipher_Ctx ctx, byte[] data, int length) {
        if (length <= 0) {
            return -1;
        }

        byte[] dataBuf = new byte[length + 0x14];
        byte[] keyBuf = new byte[0x10 + 0x10];
        byte[] hashBuf = new byte[0x10];

        System.arraycopy(ctx.buf, 0, dataBuf, 0x14, 0x10);

        if (ctx.mode == 0x1) {
            // Decryption mode 0x01: XOR the hash with AMCTRL keys and decrypt with KIRK CMD7.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ KeyVault.amHashKey5[i]);
            }
            ScrambleBB(dataBuf, 0x10, 0x39, 5, 0x07);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ KeyVault.amHashKey4[i]);
            }
        } else if (ctx.mode == 0x2) {
            // Decryption mode 0x02: XOR the hash with AMCTRL keys and decrypt with KIRK CMD8.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ KeyVault.amHashKey5[i]);
            }
            ScrambleBB(dataBuf, 0x10, 0x100, 5, 0x08);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ KeyVault.amHashKey4[i]);
            }
        }

        // Store the calculated key.
        System.arraycopy(dataBuf, 0, keyBuf, 0x10, 0x10);

        if (ctx.seed != 0x1) {
            System.arraycopy(keyBuf, 0x10, keyBuf, 0, 0xC);
            keyBuf[0xC] = (byte) ((ctx.seed - 1) & 0xFF);
            keyBuf[0xD] = (byte) (((ctx.seed - 1) >> 8) & 0xFF);
            keyBuf[0xE] = (byte) (((ctx.seed - 1) >> 16) & 0xFF);
            keyBuf[0xF] = (byte) (((ctx.seed - 1) >> 24) & 0xFF);
        }

        // Copy the first 0xC bytes of the obtained key and replicate them
        // across a new list buffer. As a terminator, add the ctx1.seed parameter's
        // 4 bytes (endian swapped) to achieve a full numbered list.
        for (int i = 0x14; i < (length + 0x14); i += 0x10) {
            System.arraycopy(keyBuf, 0x10, dataBuf, i, 0xC);
            dataBuf[i + 0xC] = (byte) (ctx.seed & 0xFF);
            dataBuf[i + 0xD] = (byte) ((ctx.seed >> 8) & 0xFF);
            dataBuf[i + 0xE] = (byte) ((ctx.seed >> 16) & 0xFF);
            dataBuf[i + 0xF] = (byte) ((ctx.seed >> 24) & 0xFF);
            ctx.seed++;
        }

        System.arraycopy(dataBuf, length + 0x04, hashBuf, 0, 0x10);

        ScrambleBB(dataBuf, length, 0x63, 5, 0x07);

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

    public int hleDrmBBCipherFinal(BBCipher_Ctx ctx) {
        ctx.mode = 0;
        ctx.seed = 0;
        for (int i = 0; i < 0x10; i++) {
            ctx.buf[i] = 0;
        }
        return 0;
    }
}