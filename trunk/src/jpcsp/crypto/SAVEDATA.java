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
import jpcsp.Emulator;
import jpcsp.format.PSF;

public class SAVEDATA {
    
    private static KIRK kirk;
    
    public SAVEDATA() {
        kirk = new KIRK();
    }
    
    // CHNNLSV SD context structs.
    private class SD_Ctx1 {

        private int mode;
        private byte[] key = new byte[16];
        private byte[] pad = new byte[16];
        private int padSize;

        public SD_Ctx1() {
            mode = 0;
            padSize = 0;
        }
    }

    private class SD_Ctx2 {

        private int mode;
        private int unk;
        private byte[] buf = new byte[16];

        public SD_Ctx2() {
            mode = 0;
            unk = 0;
        }
    }
    
    private void ScrambleSD(byte[] buf, int size, int seed, int cbc, int kirk_code) {
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
    
    /*
     * sceSd - chnnlsv.prx
     */
    private int hleSdSetIndex(SD_Ctx1 ctx, int encMode) {
        // Set all parameters to 0 and assign the encMode.
        ctx.mode = encMode;
        return 0;
    }

    private int hleSdCreateList(SD_Ctx2 ctx, int encMode, int genMode, byte[] data, byte[] key) {
        // If the key is not a 16-byte key, return an error.
        if (key.length < 0x10) {
            return -1;
        }

        // Set the mode and the unknown parameters.
        ctx.mode = encMode;
        ctx.unk = 0x1;

        // Key generator mode 0x1 (encryption): use an encrypted pseudo random number before XORing the data with the given key.
        if (genMode == 0x1) {
            byte[] header = new byte[0x14 + 0x2C];
            byte[] seed = new byte[0x14];
            byte[] tmp = new byte[0x2C];

            ByteBuffer bSeed = ByteBuffer.wrap(seed);
            kirk.hleUtilsBufferCopyWithRange(bSeed, 0x14, null, 0, 0xE);
            
            System.arraycopy(seed, 0, tmp, 0x8, 0x14);
            
            for (int i = 0xF; i >= 0; i--) {
                tmp[0x14 - i] = tmp[0x8 + i];
            }
            for (int i = 0; i < 4; i++) {
                tmp[0x20 + 0x8 + i] = 0;
            }
            System.arraycopy(tmp, 0, header, 0x14, 0x2C);

            // Encryption mode 0x1: encrypt with KIRK CMD4 and XOR with the given key.
            if (ctx.mode == 0x1) {
                ScrambleSD(header, 0x10, 0x4, 0x4, 0x04);
                System.arraycopy(header, 0xC, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0xC, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x2) { // Encryption mode 0x2: encrypt with KIRK CMD5 and XOR with the given key.
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                System.arraycopy(header, 0xC, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0xC, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x3) { // Encryption mode 0x3: XOR with SD keys, encrypt with KIRK CMD4 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + 0xC + i] = (byte) (header[0x14 + 0xC + i] ^ KeyVault.sdHashKey3[i]);
                }
                ScrambleSD(header, 0x10, 0xE, 0x4, 0x04);
                for (int i = 0; i < 0x10; i++) {
                    header[0xC + i] = (byte) (0xC + header[i] ^ KeyVault.sdHashKey4[i]);
                }
                System.arraycopy(header, 0xC, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0xC, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x4) { // Encryption mode 0x4: XOR with SD keys, encrypt with KIRK CMD5 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + 0xC + i] = (byte) (header[0x14 + 0xC + i] ^ KeyVault.sdHashKey3[i]);
                }
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                for (int i = 0; i < 0x10; i++) {
                    header[0xC + i] = (byte) (header[0xC + i] ^ KeyVault.sdHashKey4[i]);
                }
                System.arraycopy(header, 0xC, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0xC, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else if (ctx.mode == 0x6) { // Encryption mode 0x6: XOR with new SD keys, encrypt with KIRK CMD5 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + 0xC + i] = (byte) (header[0x14 + 0xC + i] ^ KeyVault.sdHashKey3[i]);
                }
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                for (int i = 0; i < 0x10; i++) {
                    header[0xC + i] = (byte) (header[0xC + i] ^ KeyVault.sdHashKey4[i]);
                }
                System.arraycopy(header, 0xC, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0xC, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    for (int i = 0; i < 16; i++) {
                        ctx.buf[i] = (byte) (ctx.buf[i] ^ key[i]);
                    }
                }
                return 0;
            } else { // Encryption mode 0x0: XOR with new SD keys, encrypt with KIRK CMD4 and XOR with the given key.
                for (int i = 0; i < 0x10; i++) {
                    header[0x14 + 0xC + i] = (byte) (header[0x14 + 0xC + i] ^ KeyVault.sdHashKey6[i]);
                }
                ScrambleSD(header, 0x10, 0x12, 0x4, 0x04);
                for (int i = 0; i < 0x10; i++) {
                    header[0xC + i] = (byte) (header[0xC + i] ^ KeyVault.sdHashKey7[i]);
                }
                System.arraycopy(header, 0xC, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0xC, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
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
            if (!isNullKey(key)) {
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

    private int hleSdRemoveValue(SD_Ctx1 ctx, byte[] data, int length) {
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

    private int hleSdGetLastIndex(SD_Ctx1 ctx, byte[] hash, byte[] key) {
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
        for (int i = 0; i < 0xF; i++) {
            keyBuf[i] = (byte) ((keyBuf[i] << 1) | ((keyBuf[i + 1] >> 7) & 0x01));
        }
        keyBuf[0xF] = (byte) ((keyBuf[0xF] << 1) ^ b);

        if (ctx.padSize < 0x10) {
            byte bb = ((keyBuf[0] & (byte) 0x80) != 0) ? (byte) 0x87 : 0;
            for (int i = 0; i < 0xF; i++) {
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

        // If ctx1.mode is the new mode 0x6, XOR with the new hash key 5, else, XOR with hash key 2.
        if (ctx.mode == 0x6) {
            for (int i = 0; i < 0x10; i++) {
                resultBuf[i] = (byte) (resultBuf[i] ^ KeyVault.sdHashKey5[i]);
            }
        } else {
            for (int i = 0; i < 0x10; i++) {
                resultBuf[i] = (byte) (resultBuf[i] ^ KeyVault.sdHashKey2[i]);
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

    private int hleSdSetMember(SD_Ctx2 ctx, byte[] data, int length) {
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
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ KeyVault.sdHashKey3[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0xE, 5, 0x07);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ KeyVault.sdHashKey4[i]);
            }
            finalSeed = 0x57;
        } else if (ctx.mode == 0x4) {
            // Decryption mode 0x04: XOR the hash with SD keys and decrypt with KIRK CMD8.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ KeyVault.sdHashKey3[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ KeyVault.sdHashKey4[i]);
            }
            finalSeed = 0x57;
        } else if (ctx.mode == 0x6) {
            // Decryption mode 0x06: XOR the hash with new SD keys and decrypt with KIRK CMD8.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ KeyVault.sdHashKey7[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ KeyVault.sdHashKey6[i]);
            }
            finalSeed = 0x64;
        } else {
            // Decryption master mode: XOR the hash with new SD keys and decrypt with KIRK CMD7.
            for (int i = 0; i < 0x10; i++) {
                dataBuf[0x14 + i] = (byte) (dataBuf[0x14 + i] ^ KeyVault.sdHashKey7[i]);
            }
            ScrambleSD(dataBuf, 0x10, 0x12, 5, 0x07);
            for (int i = 0; i < 0x10; i++) {
                dataBuf[i] = (byte) (dataBuf[i] ^ KeyVault.sdHashKey6[i]);
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
        // across a new list buffer. As a terminator, add the ctx1.seed parameter's
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

    private int hleChnnlsv_21BE78B4(SD_Ctx2 ctx) {
        ctx.mode = 0;
        ctx.unk = 0;
        for (int i = 0; i < 0x10; i++) {
            ctx.buf[i] = 0;
        }
        return 0;
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
    
    public byte[] DecryptSavedata(byte[] buf, int size, byte[] key) {
        // Initialize the context structs.
        int sdDecMode;
        SD_Ctx1 ctx1 = new SD_Ctx1();
        SD_Ctx2 ctx2 = new SD_Ctx2();

        // Setup the buffers.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] tmpbuf = new byte[alignedSize];
        byte[] hash = new byte[0x10];

        // Set the decryption mode.
        if (isNullKey(key)) {
            sdDecMode = 1;
        } else {
            // After firmware version 2.5.2 the decryption mode used is 5.
            if (Emulator.getInstance().getFirmwareVersion() > 252) {
                sdDecMode = 5;
            } else {
                sdDecMode = 3;
            }
        }

        // Perform the decryption.
        hleSdSetIndex(ctx1, sdDecMode);
        hleSdCreateList(ctx2, sdDecMode, 2, buf, key);
        hleSdRemoveValue(ctx1, buf, 0x10);
        
        System.arraycopy(buf, 0x10, tmpbuf, 0, size - 0x10);
        hleSdRemoveValue(ctx1, tmpbuf, alignedSize);
        
        hleSdSetMember(ctx2, tmpbuf, alignedSize);
        
        // Clear context 2.
        hleChnnlsv_21BE78B4(ctx2);
        
        // Generate a file hash for this data.
        hleSdGetLastIndex(ctx1, hash, key);
        
        // Copy back the data.
        System.arraycopy(tmpbuf, 0, buf, 0, alignedSize - 0x10);

        return hash;
    }

    public byte[] EncryptSavedata(byte[] buf, int size, byte[] key) {
        // Initialize the context structs.
        int sdEncMode;
        SD_Ctx1 ctx1 = new SD_Ctx1();
        SD_Ctx2 ctx2 = new SD_Ctx2();

        // Setup the buffers.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] tmpbuf1 = new byte[alignedSize + 0x10];
        byte[] tmpbuf2 = new byte[alignedSize];
        byte[] hash = new byte[0x10];

        // Copy the plain data to tmpbuf.
        System.arraycopy(buf, 0, tmpbuf1, 0x10, size);

        // Set the encryption mode.
        if (isNullKey(key)) {
            sdEncMode = 1;
        } else {
            // After firmware version 2.5.2 the encryption mode used is 5.
            if (Emulator.getInstance().getFirmwareVersion() > 252) {
                sdEncMode = 5;
            } else {
                sdEncMode = 3;
            }
        }

        // Generate the encryption IV (first 0x10 bytes).
        hleSdCreateList(ctx2, sdEncMode, 1, tmpbuf1, key);
        hleSdSetIndex(ctx1, sdEncMode);
        hleSdRemoveValue(ctx1, tmpbuf1, 0x10);
        
        System.arraycopy(tmpbuf1, 0x10, tmpbuf2, 0, alignedSize);
        hleSdSetMember(ctx2, tmpbuf2, alignedSize);
        
        // Clear extra bytes.
        for (int i = 0; i < (alignedSize - size); i++) {
            tmpbuf2[size + i] = 0;
        }
        
        // Encrypt the data.
        hleSdRemoveValue(ctx1, tmpbuf2, alignedSize);
        
        // Copy back the encrypted data + IV.
        System.arraycopy(tmpbuf2, 0, tmpbuf1, 0x10, alignedSize);
        System.arraycopy(tmpbuf1, 0, buf, 0, buf.length);
        
        // Clear context 2.
        hleChnnlsv_21BE78B4(ctx2);
        
        // Generate a file hash for this data.
        hleSdGetLastIndex(ctx1, hash, key);

        return hash;
    }
    
    private byte[] GenerateSavedataHash(byte[] data, int size, int mode, byte[] key) {
        SD_Ctx1 ctx1 = new SD_Ctx1();
        byte[] hash = new byte[0x10];
        int alignedSize = (((size + 0xF) >> 4) << 4);

        // Generate a new hash using a key.
        hleSdSetIndex(ctx1, mode);
        hleSdRemoveValue(ctx1, data, alignedSize);
        hleSdGetLastIndex(ctx1, hash, key);
        
        return hash;
    }

    public void UpdateSavedataHashes(PSF psf, byte[] data, int size, String fileName) {
        // Setup the params, hashes, modes and key (empty).
        byte[] savedataParams = new byte[0x80];
        byte[] key = new byte[0x10];
        byte[] hash_0x70 = new byte[0x10];
        byte[] hash_0x20; 
        byte[] hash_0x10;
        int mode = 2;
        int check_bit = 1;
        
        // Check for previous SAVEDATA_PARAMS data in the file.
        Object savedataParamsOld = psf.get("SAVEDATA_PARAMS");
        if (savedataParamsOld != null) {
            // Extract the mode setup from the already existing data.
            byte[] savedataParamsOldArray = (byte[]) savedataParamsOld;
            mode = ((savedataParamsOldArray[0] >> 4) & 0xF);
            check_bit = ((savedataParamsOldArray[0]) & 0xF);
        }
        
        if (((mode & 0x2) == 0x2) || ((mode & 0x4) == 0x4)) {
            if ((check_bit & 0x1) == 0x1) {
                // Generate a type 2 hash.
                hash_0x20 = GenerateSavedataHash(data, size, 2, key);
                // Set the SAVEDATA_PARAMS byte to 0x41.
                savedataParams[0] |= 0x41;
                // Generate a type 3 hash.
                hash_0x70 = GenerateSavedataHash(data, size, 3, key);
                // Generate a type 1 hash.
                hash_0x10 = GenerateSavedataHash(data, size, 1, key);
            } else {
                // Generate a type 4 hash.
                hash_0x20 = GenerateSavedataHash(data, size, 4, key);
                // Set the SAVEDATA_PARAMS byte to 0x21.
                savedataParams[0] |= 0x21;
                // Generate a type 3 hash.
                hash_0x70 = GenerateSavedataHash(data, size, 3, key);
                // Generate a type 1 hash.
                hash_0x10 = GenerateSavedataHash(data, size, 1, key);
            }
        } else {
            // Generate a type 2 hash.
            hash_0x20 = GenerateSavedataHash(data, size, 2, key);
            // Set the SAVEDATA_PARAMS bit to 1.
            savedataParams[0] |= 0x01;
            // Generate a type 1 hash.
            hash_0x10 = GenerateSavedataHash(data, size, 1, key);
        }
        
        // Store the hashes at the right offsets.
        System.arraycopy(hash_0x20, 0, savedataParams, 0x20, 0x10);
        System.arraycopy(hash_0x70, 0, savedataParams, 0x70, 0x10);
        System.arraycopy(hash_0x10, 0, savedataParams, 0x10, 0x10);

        // Output the final PSF file containing the SAVEDATA param and file hashes.
        try {
            psf.put("SAVEDATA_PARAMS", savedataParams);
        } catch (Exception e) {
            // Ignore...
        }
    }
}