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
import jpcsp.State;
import jpcsp.format.PSF;

public class SAVEDATA {
    
    private static KIRK kirk;
    
    public SAVEDATA() {
        kirk = new KIRK();
    }
    
    // CHNNLSV SD context structs.
    private class SD_Ctx1 {

        private int mode;
        private byte[] key;
        private byte[] pad;
        private int padSize;

        public SD_Ctx1() {
            mode = 0;
            padSize = 0;
            key = new byte[16];
            pad = new byte[16];
        }
    }

    private class SD_Ctx2 {

        private int mode;
        private int unk;
        private byte[] buf;

        public SD_Ctx2() {
            mode = 0;
            unk = 0;
            buf = new byte[16];
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
    
    private int getModeSeed(int mode) {
        int seed;
        switch (mode) {
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
        return seed;
    }

    private byte[] cryptMember(SD_Ctx2 ctx, byte[] data, int data_offset, int length) {
        int finalSeed;
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
            dataBuf = xorHash(dataBuf, 0x14, KeyVault.sdHashKey4, 0, 0x10);
            ScrambleSD(dataBuf, 0x10, 0xE, 5, 0x07);
            dataBuf = xorHash(dataBuf, 0, KeyVault.sdHashKey3, 0, 0x10);
            finalSeed = 0x57;
        } else if (ctx.mode == 0x4) {
            // Decryption mode 0x04: XOR the hash with SD keys and decrypt with KIRK CMD8.
            dataBuf = xorHash(dataBuf, 0x14, KeyVault.sdHashKey4, 0, 0x10);
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            dataBuf = xorHash(dataBuf, 0, KeyVault.sdHashKey3, 0, 0x10);
            finalSeed = 0x57;
        } else if (ctx.mode == 0x6) {
            // Decryption mode 0x06: XOR the hash with new SD keys and decrypt with KIRK CMD8.
            dataBuf = xorHash(dataBuf, 0x14, KeyVault.sdHashKey7, 0, 0x10);
            ScrambleSD(dataBuf, 0x10, 0x100, 5, 0x08);
            dataBuf = xorHash(dataBuf, 0, KeyVault.sdHashKey6, 0, 0x10);
            finalSeed = 0x64;
        } else {
            // Decryption mode 0x05: XOR the hash with new SD keys and decrypt with KIRK CMD7.
            dataBuf = xorHash(dataBuf, 0x14, KeyVault.sdHashKey7, 0, 0x10);
            ScrambleSD(dataBuf, 0x10, 0x12, 5, 0x07);
            dataBuf = xorHash(dataBuf, 0, KeyVault.sdHashKey6, 0, 0x10);
            finalSeed = 0x64;
        }

        // Store the calculated key.
        System.arraycopy(dataBuf, 0, keyBuf, 0x10, 0x10);

        // Apply extra padding if ctx.unk is not 1.
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
        dataBuf = xorKey(dataBuf, 0, keyBuf, 0, 0x10);

        // Copy back the last hash from the list to the first half of keyBuf.
        System.arraycopy(hashBuf, 0, keyBuf, 0, 0x10);

        // Finally, XOR the full list with the given data.
        data = xorKey(data, data_offset, dataBuf, 0, length);
        
        return data;
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
            byte[] header = new byte[0x25];
            byte[] seed = new byte[0x14];

            // Generate SHA-1 to act as seed for encryption.
            ByteBuffer bSeed = ByteBuffer.wrap(seed);
            kirk.hleUtilsBufferCopyWithRange(bSeed, 0x14, null, 0, 0xE);
                       
            // Propagate SHA-1 in kirk header.
            System.arraycopy(seed, 0, header, 0, 0x10);
            System.arraycopy(seed, 0, header, 0x14, 0x10);    

            // Encryption mode 0x1: encrypt with KIRK CMD4 and XOR with the given key.
            if (ctx.mode == 0x1) {
                ScrambleSD(header, 0x10, 0x4, 0x4, 0x04);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
                return 0;
            } else if (ctx.mode == 0x2) { // Encryption mode 0x2: encrypt with KIRK CMD5 and XOR with the given key.
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
                return 0;
            } else if (ctx.mode == 0x3) { // Encryption mode 0x3: XOR with SD keys, encrypt with KIRK CMD4 and XOR with the given key.
                header = xorHash(header, 0x14, KeyVault.sdHashKey3, 0, 0x10);
                ScrambleSD(header, 0x10, 0xE, 0x4, 0x04);
                header = xorHash(header, 0, KeyVault.sdHashKey4, 0, 0x10);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
                return 0;
            } else if (ctx.mode == 0x4) { // Encryption mode 0x4: XOR with SD keys, encrypt with KIRK CMD5 and XOR with the given key.
                header = xorHash(header, 0x14, KeyVault.sdHashKey3, 0, 0x10);
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                header = xorHash(header, 0, KeyVault.sdHashKey4, 0, 0x10);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
                return 0;
            } else if (ctx.mode == 0x6) { // Encryption mode 0x6: XOR with new SD keys, encrypt with KIRK CMD5 and XOR with the given key.
                header = xorHash(header, 0x14, KeyVault.sdHashKey6, 0, 0x10);
                ScrambleSD(header, 0x10, 0x100, 0x4, 0x05);
                header = xorHash(header, 0, KeyVault.sdHashKey7, 0, 0x10);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
                return 0;
            } else { // Encryption mode 0x5: XOR with new SD keys, encrypt with KIRK CMD4 and XOR with the given key.
                header = xorHash(header, 0x14, KeyVault.sdHashKey6, 0, 0x10);
                ScrambleSD(header, 0x10, 0x12, 0x4, 0x04);
                header = xorHash(header, 0, KeyVault.sdHashKey7, 0, 0x10);
                System.arraycopy(header, 0, ctx.buf, 0, 0x10);
                System.arraycopy(header, 0, data, 0, 0x10);
                // If the key is not null, XOR the hash with it.
                if (!isNullKey(key)) {
                    ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
                }
                return 0;
            }
        } else if (genMode == 0x2) { // Key generator mode 0x02 (decryption): directly XOR the data with the given key.
            // Grab the data hash (first 16-bytes).
            System.arraycopy(data, 0, ctx.buf, 0, 0x10);
            // If the key is not null, XOR the hash with it.
            if (!isNullKey(key)) {
                ctx.buf = xorKey(ctx.buf, 0, key, 0, 0x10);
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
            int seed = getModeSeed(ctx.mode);

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
            int blockSize;
            int dataOffset = 0;

            while (length > 0) {
                blockSize = (length + kLen >= 0x800) ? 0x800 : length + kLen;

                System.arraycopy(data, dataOffset, scrambleBuf, 0x14 + kLen, blockSize - kLen);

                // Encrypt with KIRK CMD 4 and XOR with result.
                scrambleBuf = xorKey(scrambleBuf, 0x14, ctx.key, 0, 0x10);
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
        int seed = getModeSeed(ctx.mode);

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
        ctx.pad = xorKey(ctx.pad, 0, keyBuf, 0, 0x10);

        System.arraycopy(ctx.pad, 0, scrambleKeyBuf, 0x14, 0x10);
        System.arraycopy(ctx.key, 0, resultBuf, 0, 0x10);

        // Encrypt with KIRK CMD 4 and XOR with result.
        scrambleKeyBuf = xorKey(scrambleKeyBuf, 0x14, resultBuf, 0, 0x10);
        ScrambleSD(scrambleKeyBuf, 0x10, seed, 0x4, 0x04);
        System.arraycopy(scrambleKeyBuf, (0x10 + 0x4) - 0x14, resultBuf, 0, 0x10);

        // If ctx.mode is new mode 0x5 or 0x6, XOR with the new hash key 5, else, XOR with hash key 2.
        if ((ctx.mode == 0x5) || (ctx.mode == 0x6)) {
            resultBuf = xorHash(resultBuf, 0, KeyVault.sdHashKey5, 0, 0x10);
        } else if ((ctx.mode == 0x3) || (ctx.mode == 0x4)) {
            resultBuf = xorHash(resultBuf, 0, KeyVault.sdHashKey2, 0, 0x10);
        }

        // If mode is 2, 4 or 6, encrypt again with KIRK CMD 5 and then KIRK CMD 4.
        if ((ctx.mode == 0x2) || (ctx.mode == 0x4) || (ctx.mode == 0x6)) {
            System.arraycopy(resultBuf, 0, scrambleResultBuf, 0x14, 0x10);
            ScrambleSD(scrambleResultBuf, 0x10, 0x100, 0x4, 0x05);
            System.arraycopy(scrambleResultBuf, 0, scrambleResultBuf, 0x14, 0x10);
            for(int i = 0; i < 0x14; i++) {
                scrambleResultBuf[i] = 0;
            }
            ScrambleSD(scrambleResultBuf, 0x10, seed, 0x4, 0x04);
            System.arraycopy(scrambleResultBuf, 0, resultBuf, 0, 0x10);
        }

        // XOR with the supplied key and encrypt with KIRK CMD 4.
        if (key != null) {
            resultBuf = xorKey(resultBuf, 0, key, 0, 0x10);
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

        // Parse the data in 0x800 blocks first.
        int index = 0;
        if (length >= 0x800) {
            for (index = 0; length >= 0x800; index += 0x800) {
                data = cryptMember(ctx, data, index, 0x800);
                length -= 0x800;
            }
        }

        // Finally parse the rest of the data.
        cryptMember(ctx, data, index, length);

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
            // After firmware version 2.7.1 the decryption mode used is 5.
            // Note: Due to a mislabel, 3 games from firmware 2.8.1 (Sonic Rivals, 
            // Star Trek: Tactical Assault and Brothers in Arms: D-Day) 
            // still use the decryption mode 3.
            if (Emulator.getInstance().getFirmwareVersion() > 271 && 
                    !((State.discId.equals("ULUS10195") || State.discId.equals("ULES00622"))
                    || (State.discId.equals("ULUS10193") || State.discId.equals("ULES00608"))
                    || (State.discId.equals("ULUS10150") || State.discId.equals("ULES00623")))) {
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
            // After firmware version 2.7.1 the encryption mode used is 5.
            // Note: Due to a mislabel, 3 games from firmware 2.8.1 (Sonic Rivals, 
            // Star Trek: Tactical Assault and Brothers in Arms: D-Day) 
            // still use the encryption mode 3.
            if (Emulator.getInstance().getFirmwareVersion() > 271 && 
                    !((State.discId.equals("ULUS10195") || State.discId.equals("ULES00622"))
                    || (State.discId.equals("ULUS10193") || State.discId.equals("ULES00608"))
                    || (State.discId.equals("ULUS10150") || State.discId.equals("ULES00623")))) {
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

        // Generate a new hash using a key.
        hleSdSetIndex(ctx1, mode);
        hleSdRemoveValue(ctx1, data, size);
        hleSdGetLastIndex(ctx1, hash, key);
        
        return hash;
    }

    public void UpdateSavedataHashes(PSF psf, byte[] data, int size, byte[] params) {
        // Setup the params, hashes, modes and key (empty).
        byte[] savedataParams = new byte[0x80];
        byte[] key = new byte[0x10];
        byte[] hash_0x70 = new byte[0x10];
        byte[] hash_0x20 = new byte[0x10]; 
        byte[] hash_0x10 = new byte[0x10];
        // Set mode to 4 and check_bit to 1 (0x41 is the most recent bit flag).
        int mode = 4;
        int check_bit = 1;
        
        // Check for previous SAVEDATA_PARAMS.
        for (int i = 0; i < params.length; i++) {
            if (params[i] != 0) {
                // Extract the mode setup from the already existing data.
                mode = ((params[0] >> 4) & 0xF);
                check_bit = ((params[0]) & 0xF);
                break;
            }
        }
        
        // New mode (after firmware 2.7.1).
        if ((mode & 0x4) == 0x4) {
            // Generate a type 6 hash.
            hash_0x20 = GenerateSavedataHash(data, size, 6, key);
            // Generate a type 5 hash.
            hash_0x70 = GenerateSavedataHash(data, size, 5, key);
            // Set the SAVEDATA_PARAMS byte to 0x40.
            savedataParams[0] |= 0x40;
        } else if ((mode & 0x2) == 0x2) { // Last old mode (firmware 2.0.0 to 2.7.1).
            // Generate a type 4 hash.
            hash_0x20 = GenerateSavedataHash(data, size, 4, key);
            // Generate a type 3 hash.
            hash_0x70 = GenerateSavedataHash(data, size, 3, key);
            // Set the SAVEDATA_PARAMS byte to 0x20.
            savedataParams[0] |= 0x20;
        } else { // First old mode (before firmware 2.0.0).
            // Generate a type 2 hash.
            hash_0x20 = GenerateSavedataHash(data, size, 2, key);
            // Set the SAVEDATA_PARAMS byte to 0x00.
            savedataParams[0] |= 0x00;
        }
        
        if ((check_bit & 0x1) == 0x1) {
            // Generate a type 1 hash.
            hash_0x10 = GenerateSavedataHash(data, size, 1, key);
            // Set the SAVEDATA_PARAMS byte to 0x01.
            savedataParams[0] |= 0x01;
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