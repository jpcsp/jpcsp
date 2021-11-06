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

import static jpcsp.util.Utilities.writeUnaligned32;

import java.nio.ByteBuffer;

import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;

public class AMCTRL {

    private static KIRK kirk;
    private static final byte[] amseed = {
        (byte) 'A', (byte) 'M', (byte) 'C', (byte) 'T', (byte) 'R',
        (byte) 'L', (byte) 'S', (byte) 'E', (byte) 'E', (byte) 'D',
        (byte) 'J', (byte) 'P', (byte) 'C', (byte) 'S', (byte) 'P',
        (byte) '0', (byte) '0', (byte) '0', (byte) '0', (byte) '0'
    };

    public AMCTRL() {
        // Start the KIRK engine with a dummy seed
        kirk = new KIRK(amseed, 0x14);
    }

    // AMCTRL context structs.
    public static class BBCipher_Ctx extends pspAbstractMemoryMappedStructure {
        private int mode;
        private int seed;
        private byte[] buf = new byte[16];

        public BBCipher_Ctx() {
            mode = 0;
            seed = 0;
        }

		@Override
		protected void read() {
    		mode = read32();
    		seed = read32();
    		read8Array(buf);
		}

		@Override
		protected void write() {
    		write32(mode);
    		write32(seed);
    		write8Array(buf);
		}

		@Override
		public int sizeof() {
			return 24;
		}
    }

    public static class BBMac_Ctx extends pspAbstractMemoryMappedStructure {
        private int mode;
        private byte[] key = new byte[16];
        private byte[] pad = new byte[16];
        private int padSize;

        public BBMac_Ctx() {
            mode = 0;
            padSize = 0;
        }

		@Override
		protected void read() {
    		mode = read32();
    		read8Array(key);
    		read8Array(pad);
    		padSize = read32();
		}

		@Override
		protected void write() {
    		write32(mode);
    		write8Array(key);
    		write8Array(pad);
    		write32(padSize);
		}

		@Override
		public int sizeof() {
			return 40;
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

    private byte[] xorHash(byte[] dest, int dest_offset, int[] src, int src_offset, int size) {
        for (int i = 0; i < size; i++) {
            dest[dest_offset + i] = (byte) (dest[dest_offset + i] ^ src[src_offset + i]);
        }
        return dest;
    }

    private byte[] xorKey(byte[] dest, int dest_offset, byte[] src, int src_offset, int size) {
        for (int i = 0; i < size; i++) {
            dest[dest_offset + i] = (byte) (dest[dest_offset + i] ^ src[src_offset + i]);
        }
        return dest;
    }

    private int getModeSeed(int mode) {
        int seed;
        switch (mode) {
            case 0x2:
                seed = 0x3A;
                break;
            default:
                seed = 0x38;
                break;
        }
        return seed;
    }

    private void ScrambleBB(byte[] buf, int size, int seed, int cbc, int kirk_code) {
        // Set CBC mode.
    	writeUnaligned32(buf, 0, cbc);

        // Set unkown parameters to 0.
    	writeUnaligned32(buf, 4, 0);
    	writeUnaligned32(buf, 8, 0);

        // Set the the key seed to seed.
    	writeUnaligned32(buf, 12, seed);

        // Set the the data size to size.
    	writeUnaligned32(buf, 16, size);

        ByteBuffer bBuf = ByteBuffer.wrap(buf);
        kirk.hleUtilsBufferCopyWithRange(bBuf, size, bBuf, size + 20, kirk_code);
    }

    private void cipherMember(BBCipher_Ctx ctx, byte[] data, int data_offset, int length) {
        byte[] dataBuf = new byte[length + 0x14];
        byte[] keyBuf1 = new byte[0x10];
        byte[] keyBuf2 = new byte[0x10];
        byte[] hashBuf = new byte[0x10];

        // Copy the hash stored by hleDrmBBCipherInit.
        System.arraycopy(ctx.buf, 0, dataBuf, 0x14, 0x10);

        if (ctx.mode == 0x2) {
            // Decryption mode 0x02: XOR the hash with AMCTRL keys and decrypt with KIRK CMD8.
            dataBuf = xorHash(dataBuf, 0x14, KeyVault.amHashKey5, 0, 0x10);
            ScrambleBB(dataBuf, 0x10, 0x100, 5, KIRK.PSP_KIRK_CMD_DECRYPT_FUSE);
            dataBuf = xorHash(dataBuf, 0, KeyVault.amHashKey4, 0, 0x10);
        } else {
            // Decryption mode 0x01: XOR the hash with AMCTRL keys and decrypt with KIRK CMD7.
            dataBuf = xorHash(dataBuf, 0x14, KeyVault.amHashKey5, 0, 0x10);
            ScrambleBB(dataBuf, 0x10, 0x39, 5, KIRK.PSP_KIRK_CMD_DECRYPT);
            dataBuf = xorHash(dataBuf, 0, KeyVault.amHashKey4, 0, 0x10);
        }

        // Store the calculated key.
        System.arraycopy(dataBuf, 0, keyBuf2, 0, 0x10);

        // Apply extra padding if ctx.seed is not 1.
        if (ctx.seed != 0x1) {
            System.arraycopy(keyBuf2, 0, keyBuf1, 0, 0xC);
            writeUnaligned32(keyBuf1, 0xC, ctx.seed - 1);
        }

        // Copy the first 0xC bytes of the obtained key and replicate them
        // across a new list buffer. As a terminator, add the ctx.seed parameter's
        // 4 bytes (endian swapped) to achieve a full numbered list.
        for (int i = 0x14; i < (length + 0x14); i += 0x10) {
            System.arraycopy(keyBuf2, 0, dataBuf, i, 0xC);
            writeUnaligned32(dataBuf, i + 0xC, ctx.seed);
            ctx.seed++;
        }

        // Copy the generated hash to hashBuf.
        System.arraycopy(dataBuf, length + 0x04, hashBuf, 0, 0x10);

        // Decrypt the hash with KIRK CMD7 and seed 0x63.
        ScrambleBB(dataBuf, length, 0x63, 5, KIRK.PSP_KIRK_CMD_DECRYPT);

        // XOR the first 16-bytes of data with the saved key to generate a new hash.
        dataBuf = xorKey(dataBuf, 0, keyBuf1, 0, 0x10);

        // Copy back the last hash from the list to the first keyBuf.
        System.arraycopy(hashBuf, 0, keyBuf1, 0, 0x10);

        // Finally, XOR the full list with the given data.
        xorKey(data, data_offset, dataBuf, 0, length);
    }

    /*
     * sceDrmBB - amctrl.prx
     */
    public int hleDrmBBMacInit(BBMac_Ctx ctx, int encMode) {
        // Set all parameters to 0 and assign the encMode.
        ctx.mode = encMode;
        ctx.padSize = 0;
        for (int i = 0; i < 0x10; i++) {
            ctx.pad[i] = 0;
        }
        for (int i = 0; i < 0x10; i++) {
            ctx.key[i] = 0;
        }
        return 0;
    }

    public int hleDrmBBMacUpdate(BBMac_Ctx ctx, byte[] data, int length) {
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
            int seed = getModeSeed(ctx.mode);

            // Setup the buffer. 
            byte[] scrambleBuf = new byte[0x800 + 0x14];

            // Copy the previous pad key to the buffer.
            System.arraycopy(ctx.pad, 0, scrambleBuf, 0x14, ctx.padSize);

            // Calculate new key length.
            int kLen = ((ctx.padSize + length) & 0x0F);
            if (kLen == 0) {
                kLen = 0x10;
            }

            // Calculate new data length.
            int nLen = ctx.padSize;
            ctx.padSize = kLen;

            // Copy data's footer to make a new key.
            int remaining = length - kLen;
            System.arraycopy(data, remaining, ctx.pad, 0, kLen);

            // Process the encryption in 0x800 blocks.
            int blockSize = 0x800;

            for (int i = 0; i < remaining; i++) {
                if (nLen == blockSize) {
                    // XOR with result and encrypt with KIRK CMD 4.
                    scrambleBuf = xorKey(scrambleBuf, 0x14, ctx.key, 0, 0x10);
                    ScrambleBB(scrambleBuf, blockSize, seed, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT);
                    System.arraycopy(scrambleBuf, blockSize + 0x4, ctx.key, 0, 0x10);
                    // Reset length.
                    nLen = 0;
                }
                // Keep copying data.
                scrambleBuf[0x14 + nLen] = data[i];
                nLen++;
            }

            // Process any leftover data.
            if (nLen > 0) {
                scrambleBuf = xorKey(scrambleBuf, 0x14, ctx.key, 0, 0x10);
                ScrambleBB(scrambleBuf, nLen, seed, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT);
                System.arraycopy(scrambleBuf, nLen + 0x4, ctx.key, 0, 0x10);
            }

            return 0;
        }
    }

    public int hleDrmBBMacFinal(BBMac_Ctx ctx, byte[] hash, byte[] key) {
        if (ctx.padSize > 0x10) {
            // Invalid key length.
            return -1;
        }

        // Calculate the seed.
        int seed = getModeSeed(ctx.mode);

        // Set up the buffer.
        byte[] scrambleBuf = new byte[0x800 + 0x14];

        // Set up necessary buffers.
        byte[] keyBuf = new byte[0x10];
        byte[] resultBuf = new byte[0x10];

        // Encrypt the buffer with KIRK CMD 4.
        ScrambleBB(scrambleBuf, 0x10, seed, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT);

        // Store the generated key.
        System.arraycopy(scrambleBuf, 0x14, keyBuf, 0, 0x10);

        // Apply custom padding management to the stored key.
        byte b = ((keyBuf[0] & (byte) 0x80) != 0) ? (byte) 0x87 : 0;
        for (int i = 0; i < 0xF; i++) {
            int b1 = ((keyBuf[i] & 0xFF) << 1);
            int b2 = ((keyBuf[i + 1] & 0xFF) >> 7);
            keyBuf[i] = (byte) (b1 | b2);
        }
        byte t = (byte) ((keyBuf[0xF] & 0xFF) << 1);
        keyBuf[0xF] = (byte) (t ^ b);

        if (ctx.padSize < 0x10) {
            byte bb = ((keyBuf[0] < 0)) ? (byte) 0x87 : 0;
            for (int i = 0; i < 0xF; i++) {
                int bb1 = ((keyBuf[i] & 0xFF) << 1);
                int bb2 = ((keyBuf[i + 1] & 0xFF) >> 7);
                keyBuf[i] = (byte) (bb1 | bb2);
            }
            byte tt = (byte) ((keyBuf[0xF] & 0xFF) << 1);
            keyBuf[0xF] = (byte) (tt ^ bb);

            ctx.pad[ctx.padSize] = (byte) 0x80;
            if ((ctx.padSize + 1) < 0x10) {
                for (int i = 0; i < (0x10 - ctx.padSize - 1); i++) {
                    ctx.pad[ctx.padSize + 1 + i] = 0;
                }
            }
        }

        // XOR previous pad key with new one and copy the result back to the buffer.
        ctx.pad = xorKey(ctx.pad, 0, keyBuf, 0, 0x10);
        System.arraycopy(ctx.pad, 0, scrambleBuf, 0x14, 0x10);

        // Save the previous result key.
        System.arraycopy(ctx.key, 0, resultBuf, 0, 0x10);

        // XOR the decrypted key with the result key.
        scrambleBuf = xorKey(scrambleBuf, 0x14, resultBuf, 0, 0x10);

        // Encrypt the key with KIRK CMD 4.
        ScrambleBB(scrambleBuf, 0x10, seed, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT);

        // Copy back the key into the result buffer.
        System.arraycopy(scrambleBuf, 0x14, resultBuf, 0, 0x10);

        // XOR with amHashKey3.
        resultBuf = xorHash(resultBuf, 0, KeyVault.amHashKey3, 0, 0x10);

        // If mode is 2, encrypt again with KIRK CMD 5 and then KIRK CMD 4.
        if (ctx.mode == 0x2) {
            // Copy the result buffer into the data buffer.
            System.arraycopy(resultBuf, 0, scrambleBuf, 0x14, 0x10);

            // Encrypt with KIRK CMD 5 (seed is always 0x100).
            ScrambleBB(scrambleBuf, 0x10, 0x100, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT_FUSE);

            // Encrypt again with KIRK CMD 4.
            ScrambleBB(scrambleBuf, 0x10, seed, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT);

            // Copy back into result buffer.
            System.arraycopy(scrambleBuf, 0x14, resultBuf, 0, 0x10);
        }

        // XOR with the supplied key and encrypt with KIRK CMD 4.
        if (key != null) {
            // XOR result buffer with user key.
            resultBuf = xorKey(resultBuf, 0, key, 0, 0x10);

            // Copy the result buffer into the data buffer.
            System.arraycopy(resultBuf, 0, scrambleBuf, 0x14, 0x10);

            // Encrypt with KIRK CMD 4.
            ScrambleBB(scrambleBuf, 0x10, seed, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT);

            // Copy back into the result buffer.
            System.arraycopy(scrambleBuf, 0x14, resultBuf, 0, 0x10);
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
        byte[] hashBuf;

        int mode = ctx.mode;

        // Call hleDrmBBMacFinal on an empty buffer.
        hleDrmBBMacFinal(ctx, resBuf, key);

        // If mode is 3, decrypt the hash first.
        if ((mode & 0x3) == 0x3) {
            hashBuf = DecryptBBMacKey(hash, 0x63);
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

        // Key generator mode 0x1 (encryption): use an encrypted pseudo random number before XORing the data with the given key.
        if (genMode == 0x1) {
            ctx.seed = 0x1;

            byte[] header = new byte[0x24];
            byte[] rseed = new byte[0x14];

            // Generate SHA-1 to act as seed for encryption.
            ByteBuffer bSeed = ByteBuffer.wrap(rseed);
            kirk.hleUtilsBufferCopyWithRange(bSeed, 0x14, null, 0, KIRK.PSP_KIRK_CMD_PRNG);

            // Propagate SHA-1 in kirk header.
            System.arraycopy(bSeed.array(), 0, header, 0, 0x14);
            System.arraycopy(bSeed.array(), 0, header, 0x14, 0x10);
            header[0x20] = 0;
            header[0x21] = 0;
            header[0x22] = 0;
            header[0x23] = 0;

            if (ctx.mode == 0x2) { // Encryption mode 0x2: XOR with AMCTRL keys, encrypt with KIRK CMD5 and XOR with the given key.
                header = xorHash(header, 0x14, KeyVault.amHashKey4, 0, 0x10);
                ScrambleBB(header, 0x10, 0x100, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT_FUSE);
                header = xorHash(header, 0x14, KeyVault.amHashKey5, 0, 0x10);
                System.arraycopy(header, 0x14, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0x14, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
            } else { // Encryption mode 0x1: XOR with AMCTRL keys, encrypt with KIRK CMD4 and XOR with the given key.
                header = xorHash(header, 0x14, KeyVault.amHashKey4, 0, 0x10);
                ScrambleBB(header, 0x10, 0x39, 0x4, KIRK.PSP_KIRK_CMD_ENCRYPT);
                header = xorHash(header, 0x14, KeyVault.amHashKey5, 0, 0x10);
                System.arraycopy(header, 0x14, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0x14, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
            }
        } else if (genMode == 0x2) { // Key generator mode 0x02 (decryption): directly XOR the data with the given key.
            ctx.seed = seed + 0x1;
            // Grab the data hash (first 16-bytes).
            System.arraycopy(data, 0, ctx.buf, 0, 0x10);
            // If the key is not null, XOR the hash with it.
            if (!isNullKey(key)) {
                ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
            }
        } else {
            // Invalid mode.
            return -1;
        }

        return 0;
    }

    public int hleDrmBBCipherUpdate(BBCipher_Ctx ctx, byte[] data, int length) {
        if (length == 0) {
            return 0;
        }
        if ((length & 0xF) != 0) {
            return -1;
        }

        // Parse the data in 0x800 blocks first.
        int index = 0;
        if (length >= 0x800) {
            for (index = 0; length >= 0x800; index += 0x800) {
                cipherMember(ctx, data, index, 0x800);
                length -= 0x800;
            }
        }

        // Finally parse the rest of the data.
        if (length >= 0x10) {
            cipherMember(ctx, data, index, length);
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

    public byte[] DecryptBBMacKey(byte[] key, int seed) {
        byte[] scrambleBuf = new byte[0x10 + 0x14];
        byte[] decKey = new byte[0x10];

        System.arraycopy(key, 0, scrambleBuf, 0x14, 0x10);
        ScrambleBB(scrambleBuf, 0x10, seed, 0x5, KIRK.PSP_KIRK_CMD_DECRYPT);
        System.arraycopy(scrambleBuf, 0, decKey, 0, 0x10);

        return decKey;
    }

    public byte[] GetKeyFromBBMac(BBMac_Ctx ctx, byte[] bbmac) {
        byte[] key = new byte[0x10];
        byte[] decKey = new byte[0x10];
        byte[] macKey = new byte[0x10];
        byte[] finalKey = new byte[0x10];

        int mode = ctx.mode; // This will be reset to 0 by hleDrmBBMacFinal
        hleDrmBBMacFinal(ctx, macKey, null);

        if ((mode & 0x3) == 0x3) {
            decKey = DecryptBBMacKey(bbmac, 0x63);
        } else {
            System.arraycopy(bbmac, 0, decKey, 0, 0x10);
        }

        int seed = getModeSeed(mode);
        finalKey = DecryptBBMacKey(decKey, seed);

        key = xorKey(macKey, 0, finalKey, 0, 0x10);

        return key;
    }
}
