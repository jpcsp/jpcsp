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

public class PRX {
    
    private static KIRK kirk;
    
    public PRX() {
        kirk = new KIRK();
    }
    
    // PRXDecrypter TAG struct.
    private class TAG_INFO {

        int tag; // 4 byte value at offset 0xD0 in the PRX file
        int[] key; // 144 bytes keys
        int code; // code for scramble
        int codeExtra; // code extra for scramble (old tags)

        public TAG_INFO(int tag, int[] key, int code, int codeExtra) {
            this.tag = tag;
            this.key = intArrayToTagArray(key);
            this.code = code;
            this.codeExtra = codeExtra;
        }

        public TAG_INFO(int tag, int[] key, int code) {
            this.tag = tag;
            this.key = key;
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
        new TAG_INFO(0x4C949CF0, KeyVault.keys210_vita_k0, 0x43),
        new TAG_INFO(0x4C9494F0, KeyVault.keys660_k1, 0x43),
        new TAG_INFO(0x4C9495F0, KeyVault.keys660_k2, 0x43),
        new TAG_INFO(0x4C9490F0, KeyVault.keys660_k3, 0x43),
        new TAG_INFO(0x4C9491F0, KeyVault.keys660_k8, 0x43),
        new TAG_INFO(0x4C9493F0, KeyVault.keys660_k4, 0x43),
        new TAG_INFO(0x4C9497F0, KeyVault.keys660_k5, 0x43),
        new TAG_INFO(0x4C9492F0, KeyVault.keys660_k6, 0x43),
        new TAG_INFO(0x4C9496F0, KeyVault.keys660_k7, 0x43),
        new TAG_INFO(0x457B90F0, KeyVault.keys660_v1, 0x5B),
        new TAG_INFO(0x457B91F0, KeyVault.keys660_v7, 0x5B),
        new TAG_INFO(0x457B92F0, KeyVault.keys660_v6, 0x5B),
        new TAG_INFO(0x457B93F0, KeyVault.keys660_v3, 0x5B),
        new TAG_INFO(0x380290F0, KeyVault.keys660_v2, 0x5A),
        new TAG_INFO(0x380291F0, KeyVault.keys660_v8, 0x5A),
        new TAG_INFO(0x380292F0, KeyVault.keys660_v4, 0x5A),
        new TAG_INFO(0x380293F0, KeyVault.keys660_v5, 0x5A),
        new TAG_INFO(0x4C948CF0, KeyVault.keys639_k3, 0x43),
        new TAG_INFO(0x4C948DF0, KeyVault.keys638_k4, 0x43),
        new TAG_INFO(0x4C948BF0, KeyVault.keys636_k2, 0x43),
        new TAG_INFO(0x4C948AF0, KeyVault.keys636_k1, 0x43),
        new TAG_INFO(0x457B8AF0, KeyVault.keys636_1, 0x5B),
        new TAG_INFO(0x4C9487F0, KeyVault.keys630_k8, 0x43),
        new TAG_INFO(0x457B83F0, KeyVault.keys630_k7, 0x5B),
        new TAG_INFO(0x4C9486F0, KeyVault.keys630_k6, 0x43),
        new TAG_INFO(0x457B82F0, KeyVault.keys630_k5, 0x5B),
        new TAG_INFO(0x457B81F0, KeyVault.keys630_k4, 0x5B),
        new TAG_INFO(0x4C9485F0, KeyVault.keys630_k3, 0x43),
        new TAG_INFO(0x457B80F0, KeyVault.keys630_k2, 0x5B),
        new TAG_INFO(0x4C9484F0, KeyVault.keys630_k1, 0x43),
        new TAG_INFO(0x457B28F0, KeyVault.keys620_e, 0x5B),
        new TAG_INFO(0x457B0CF0, KeyVault.keys620_a, 0x5B),
        new TAG_INFO(0x380228F0, KeyVault.keys620_5v, 0x5A),
        new TAG_INFO(0x4C942AF0, KeyVault.keys620_5k, 0x43),
        new TAG_INFO(0x4C9428F0, KeyVault.keys620_5, 0x43),
        new TAG_INFO(0x4C941DF0, KeyVault.keys620_1, 0x43),
        new TAG_INFO(0x4C941CF0, KeyVault.keys620_0, 0x43),
        new TAG_INFO(0x4C9422F0, KeyVault.keys600_2, 0x43),
        new TAG_INFO(0x4C941EF0, KeyVault.keys600_1, 0x43),
        new TAG_INFO(0x4C9429F0, KeyVault.keys570_5k, 0x43),
        new TAG_INFO(0x457B0BF0, KeyVault.keys505_a, 0x5B),
        new TAG_INFO(0x4C9419F0, KeyVault.keys505_1, 0x43),
        new TAG_INFO(0x4C9418F0, KeyVault.keys505_0, 0x43),
        new TAG_INFO(0x457B1EF0, KeyVault.keys500_c, 0x5B),
        new TAG_INFO(0x4C941FF0, KeyVault.keys500_2, 0x43),
        new TAG_INFO(0x4C9417F0, KeyVault.keys500_1, 0x43),
        new TAG_INFO(0x4C9416F0, KeyVault.keys500_0, 0x43),
        new TAG_INFO(0x4C9414F0, KeyVault.keys390_0, 0x43),
        new TAG_INFO(0x4C9415F0, KeyVault.keys390_1, 0x43),
        new TAG_INFO(0x4C9412F0, KeyVault.keys370_0, 0x43),
        new TAG_INFO(0x4C9413F0, KeyVault.keys370_1, 0x43),
        new TAG_INFO(0x457B10F0, KeyVault.keys370_2, 0x5B),
        new TAG_INFO(0x4C940DF0, KeyVault.keys360_0, 0x43),
        new TAG_INFO(0x4C9410F0, KeyVault.keys360_1, 0x43),
        new TAG_INFO(0x4C940BF0, KeyVault.keys330_0, 0x43),
        new TAG_INFO(0x457B0AF0, KeyVault.keys330_1, 0x5B),
        new TAG_INFO(0x38020AF0, KeyVault.keys330_2, 0x5A),
        new TAG_INFO(0x4C940AF0, KeyVault.keys330_3, 0x43),
        new TAG_INFO(0x4C940CF0, KeyVault.keys330_4, 0x43),
        new TAG_INFO(0xcfef09f0, KeyVault.keys310_0, 0x62),
        new TAG_INFO(0x457b08f0, KeyVault.keys310_1, 0x5B),
        new TAG_INFO(0x380208F0, KeyVault.keys310_2, 0x5A),
        new TAG_INFO(0xcfef08f0, KeyVault.keys310_3, 0x62),
        new TAG_INFO(0xCFEF07F0, KeyVault.keys303_0, 0x62),
        new TAG_INFO(0xCFEF06F0, KeyVault.keys300_0, 0x62),
        new TAG_INFO(0x457B06F0, KeyVault.keys300_1, 0x5B),
        new TAG_INFO(0x380206F0, KeyVault.keys300_2, 0x5A),
        new TAG_INFO(0xCFEF05F0, KeyVault.keys280_0, 0x62),
        new TAG_INFO(0x457B05F0, KeyVault.keys280_1, 0x5B),
        new TAG_INFO(0x380205F0, KeyVault.keys280_2, 0x5A),
        new TAG_INFO(0x16D59E03, KeyVault.keys260_0, 0x62),
        new TAG_INFO(0x76202403, KeyVault.keys260_1, 0x5B),
        new TAG_INFO(0x0F037303, KeyVault.keys260_2, 0x5A),
        new TAG_INFO(0x4C940FF0, KeyVault.key_2DA8, 0x43),
        new TAG_INFO(0x4467415D, KeyVault.key_22E0, 0x59),
        new TAG_INFO(0x00000000, KeyVault.key_21C0, 0x42),
        new TAG_INFO(0x01000000, KeyVault.key_2250, 0x43),
        new TAG_INFO(0x2E5E10F0, KeyVault.key_2E5E10F0, 0x48),
        new TAG_INFO(0x2E5E12F0, KeyVault.key_2E5E12F0, 0x48),
        new TAG_INFO(0x2E5E13F0, KeyVault.key_2E5E13F0, 0x48),
        new TAG_INFO(0x2FD30BF0, KeyVault.key_2FD30BF0, 0x47),
        new TAG_INFO(0x2FD311F0, KeyVault.key_2FD311F0, 0x47),
        new TAG_INFO(0x2FD312F0, KeyVault.key_2FD312F0, 0x47),
        new TAG_INFO(0x2FD313F0, KeyVault.key_2FD313F0, 0x47),
        new TAG_INFO(0xD91605F0, KeyVault.key_D91605F0, 0x5D),
        new TAG_INFO(0xD91606F0, KeyVault.key_D91606F0, 0x5D),
        new TAG_INFO(0xD91608F0, KeyVault.key_D91608F0, 0x5D),
        new TAG_INFO(0xD91609F0, KeyVault.key_D91609F0, 0x5D),
        new TAG_INFO(0xD9160AF0, KeyVault.key_D9160AF0, 0x5D),
        new TAG_INFO(0xD9160BF0, KeyVault.key_D9160BF0, 0x5D),
        new TAG_INFO(0xD91611F0, KeyVault.key_D91611F0, 0x5D),
        new TAG_INFO(0xD91612F0, KeyVault.key_D91612F0, 0x5D),
        new TAG_INFO(0xD91613F0, KeyVault.key_D91613F0, 0x5D),
        new TAG_INFO(0xD91614F0, KeyVault.key_D91614F0, 0x5D),
        new TAG_INFO(0xD91615F0, KeyVault.key_D91615F0, 0x5D),
        new TAG_INFO(0xD91616F0, KeyVault.key_D91616F0, 0x5D),
        new TAG_INFO(0xD91617F0, KeyVault.key_D91617F0, 0x5D),
        new TAG_INFO(0xD91618F0, KeyVault.key_D91618F0, 0x5D),
        new TAG_INFO(0xD91619F0, KeyVault.key_D91619F0, 0x5D),
        new TAG_INFO(0xD9161AF0, KeyVault.key_D9161AF0, 0x5D),
        new TAG_INFO(0xD91620F0, KeyVault.key_D91620F0, 0x5D),
        new TAG_INFO(0xD91621F0, KeyVault.key_D91621F0, 0x5D),
        new TAG_INFO(0xD91622F0, KeyVault.key_D91622F0, 0x5D),
        new TAG_INFO(0xD91623F0, KeyVault.key_D91623F0, 0x5D),
        new TAG_INFO(0xD91624F0, KeyVault.key_D91624F0, 0x5D),
        new TAG_INFO(0xD91628F0, KeyVault.key_D91628F0, 0x5D),
        new TAG_INFO(0xD91680F0, KeyVault.key_D91680F0, 0x5D),
        new TAG_INFO(0xD91681F0, KeyVault.key_D91681F0, 0x5D),
        new TAG_INFO(0xD82310F0, KeyVault.keys02G_E, 0x51),
        new TAG_INFO(0xD8231EF0, KeyVault.keys03G_E, 0x51),
        new TAG_INFO(0xD82328F0, KeyVault.keys05G_E, 0x51),
        new TAG_INFO(0x279D08F0, KeyVault.oneseg_310, 0x61),
        new TAG_INFO(0x279D06F0, KeyVault.oneseg_300, 0x61),
        new TAG_INFO(0x279D05F0, KeyVault.oneseg_280, 0x61),
        new TAG_INFO(0xD66DF703, KeyVault.oneseg_260_271, 0x61),
        new TAG_INFO(0x279D10F0, KeyVault.oneseg_slim, 0x61),
        new TAG_INFO(0x3C2A08F0, KeyVault.ms_app_main, 0x67),
        new TAG_INFO(0xADF305F0, KeyVault.demokeys_280, 0x60),
        new TAG_INFO(0xADF306F0, KeyVault.demokeys_3XX_1, 0x60),
        new TAG_INFO(0xADF308F0, KeyVault.demokeys_3XX_2, 0x60),
        new TAG_INFO(0x8004FD03, KeyVault.ebootbin_271_new, 0x5D),
        new TAG_INFO(0xD91605F0, KeyVault.ebootbin_280_new, 0x5D),
        new TAG_INFO(0xD91606F0, KeyVault.ebootbin_300_new, 0x5D),
        new TAG_INFO(0xD91608F0, KeyVault.ebootbin_310_new, 0x5D),
        new TAG_INFO(0x0A35EA03, KeyVault.gameshare_260_271, 0x5E),
        new TAG_INFO(0x7B0505F0, KeyVault.gameshare_280, 0x5E),
        new TAG_INFO(0x7B0506F0, KeyVault.gameshare_300, 0x5E),
        new TAG_INFO(0x7B0508F0, KeyVault.gameshare_310, 0x5E),
        new TAG_INFO(0x380210F0, KeyVault.key_380210F0, 0x5A),
        new TAG_INFO(0x380280F0, KeyVault.key_380280F0, 0x5A),
        new TAG_INFO(0x380283F0, KeyVault.key_380283F0, 0x5A),
        new TAG_INFO(0x407810F0, KeyVault.key_407810F0, 0x6A),
        new TAG_INFO(0xE92410F0, KeyVault.drmkeys_6XX_1, 0x40),
        new TAG_INFO(0x692810F0, KeyVault.drmkeys_6XX_2, 0x40),
        new TAG_INFO(0x00000000, KeyVault.g_key0, 0x42),
        new TAG_INFO(0x02000000, KeyVault.g_key2, 0x45),
        new TAG_INFO(0x03000000, KeyVault.g_key3, 0x46),
        new TAG_INFO(0x4467415d, KeyVault.g_key44, 0x59, 0x59),
        new TAG_INFO(0x207bbf2f, KeyVault.g_key20, 0x5A, 0x5A),
        new TAG_INFO(0x3ace4dce, KeyVault.g_key3A, 0x5B, 0x5B),
        new TAG_INFO(0x07000000, KeyVault.g_key_INDEXDAT1xx, 0x4A),
        new TAG_INFO(0x08000000, KeyVault.g_keyEBOOT1xx, 0x4B),
        new TAG_INFO(0xC0CB167C, KeyVault.g_keyEBOOT2xx, 0x5D, 0x5D),
        new TAG_INFO(0x0B000000, KeyVault.g_keyUPDATER, 0x4E),
        new TAG_INFO(0x0C000000, KeyVault.g_keyDEMOS27X, 0x4F),
        new TAG_INFO(0x0F000000, KeyVault.g_keyMEIMG250, 0x52),
        new TAG_INFO(0x862648D1, KeyVault.g_keyMEIMG260, 0x52, 0x52),
        new TAG_INFO(0x207BBF2F, KeyVault.g_keyUNK1, 0x5A, 0x5A),
        new TAG_INFO(0x09000000, KeyVault.g_key_GAMESHARE1xx, 0x4C),
        new TAG_INFO(0xBB67C59F, KeyVault.g_key_GAMESHARE2xx, 0x5E, 0x5E)};
    
    private TAG_INFO GetTagInfo(int tag) {
        int iTag;
        for (iTag = 0; iTag < g_tagInfo.length; iTag++) {
            if (g_tagInfo[iTag].tag == tag) {
                return g_tagInfo[iTag];
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
        kirk.hleUtilsBufferCopyWithRange(bBuf, size, bBuf, size, 0x07);
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

    private boolean TestBlacklist(byte[] inbuf, byte[] blacklist, int blacklistsize) {
        if (blacklistsize / 16 != 0) {
            for (int i = 0; i < blacklistsize / 16; i++) {
                for (int j = 0; j < 0x10; j++) {
                    byte b = blacklist[(i * 16) + j];
                    byte bb = inbuf[0x140 + j];
                    if (b != bb) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void MixXOR(byte[] outbuf, int size, byte[] inbuf, byte[] xor) {
        for (int i = 0; i < size; i++) {
            outbuf[i] = (byte) ((inbuf[i] & 0xFF) ^ (xor[i] & 0xFF));
        }
    }

    private void RoundXOR(byte[] buf, int size, byte[] xor1, byte[] xor2) {
        for (int i = 0; i < size; i++) {
            if (!isNullKey(xor1)) {
                buf[i] ^= xor1[i & 0xf];
            }

            if (!isNullKey(xor2)) {
                buf[i] ^= xor2[i & 0xf];
            }
        }
    }
    
    public int DecryptPRX(byte[] buf, int size, byte[] blacklist, int blacklistsize, int type, byte[] xor1, byte[] xor2) {
        // Setup temporary buffers.
        byte[] buf1 = new byte[0x150];
        byte[] buf2 = new byte[0x150];
        byte[] buf3 = new byte[0x90];
        byte[] buf4 = new byte[0xB4];
        byte[] buf5 = new byte[0x20];
        byte[] buf6 = new byte[0x28];
        byte[] sigbuf = new byte[0x28];
        byte[] sha1buf = new byte[0x14];
        int unk_0xD4 = 0;
        
        // Copy the first header to buf1.
        System.arraycopy(buf, 0, buf1, 0, 0x150);
        
        // Fetch the PRX tag.
        int tag = ((buf[0xD0] & 0xFF) << 24) | ((buf[0xD1] & 0xFF) << 16)
                | ((buf[0xD2] & 0xFF) << 8) | (buf[0xD3] & 0xFF);       

        // Get the tag info.
        TAG_INFO pti = GetTagInfo(Integer.reverseBytes(tag));
        if (pti == null) {
            return -1;
        }
        
        // Check for blacklisted tags.
        if ((blacklist != null) && (blacklistsize != 0)) {
            if (TestBlacklist(buf1, blacklist, blacklistsize)) {
                return -1;
            }
	}
        
        // Fetch the final ELF size.
        int retsize = ((buf[0xB3] & 0xFF) << 24) | ((buf[0xB2] & 0xFF) << 16)
                | ((buf[0xB1] & 0xFF) << 8) | ((buf[0xB0] & 0xFF));
        
        // Read in the user key and apply scramble.
        if ((type >= 2) && (type <= 7) || (type == 9) || (type == 10)) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 0x10; j++) {
                    buf2[0x14 + ((i << 4) + j)] = (byte) pti.key[j];
                }
                buf2[0x14 + ((i << 4))] = (byte) i;
            }
        } else {
            for (int i = 0; i < 0x90; i++) {
                buf2[0x14 + i] = (byte) pti.key[i];
            }
        }
        
        ScramblePRX(buf2, 0x90, (byte) (pti.code & 0xFF));
        
        // Round XOR key for PRX type 3,5,7 and 10.
        if ((type == 3) || (type == 5) || (type == 7) || (type == 10)) {
            if(!isNullKey(xor2)) {
                RoundXOR(buf2, 0x90, xor2, null);
            }
        }
        
        System.arraycopy(buf2, 0, buf3, 0, 0x90);
        
        // Type 9 and 10 specific step.
        if ((type == 9) || (type == 10)) {
            System.arraycopy(buf, 0x104, buf6, 0, buf6.length);
            
            for (int i = 0; i < buf6.length; i++) {
                buf[0x104 + i] = 0;
            }
            
            System.arraycopy(buf6, 0, sigbuf, 0, sigbuf.length);
            
            buf[0] = (byte) (((size - 4) & 0xFF) << 24);
            buf[1] = (byte) (((size - 4) & 0xFF) << 16);
            buf[2] = (byte) (((size - 4) & 0xFF) << 8);
            buf[3] = (byte) (((size - 4) & 0xFF));

            // Generate SHA1 hash.
            ByteBuffer bSHA1 = ByteBuffer.wrap(buf);
            kirk.hleUtilsBufferCopyWithRange(bSHA1, size, bSHA1, size, KIRK.PSP_KIRK_CMD_SHA1_HASH);
            
            System.arraycopy(buf, 0, sha1buf, 0, sha1buf.length);
            System.arraycopy(buf, 0, buf1, 0, 0x20);
            
            if ((((tag << 16) & 0xFF) & 0x16) == 0x16) {
                System.arraycopy(KeyVault.g_pubkey_28752, 0, buf4, 0, KeyVault.g_pubkey_28752.length);
            } else if ((((tag << 16) & 0xFF) & 0x5E) == 0x5E) {
                System.arraycopy(KeyVault.g_pubkey_28712, 0, buf4, 0, KeyVault.g_pubkey_28712.length);
            } else {
                System.arraycopy(KeyVault.g_pubkey_28672, 0, buf4, 0, KeyVault.g_pubkey_28672.length);
            }
            
            System.arraycopy(sha1buf, 0, buf4, 0x28, sha1buf.length);
            System.arraycopy(sigbuf, 0, buf4, 0x28 + sha1buf.length, sigbuf.length);

            // Verify ECDSA signature.
            ByteBuffer bECDSA = ByteBuffer.wrap(buf4);
            kirk.hleUtilsBufferCopyWithRange(null, 0, bECDSA, 100, KIRK.PSP_KIRK_CMD_ECDSA_VERIFY);
        }

        if (type == 3) {            
            System.arraycopy(buf1, 0xEC, buf2, 0, 0x40);
            for(int i = 0; i < 0x50; i++) {
                buf2[0x40 + i] = 0;
            }
            
            buf2[0x60] = 0x03;
            buf2[0x70] = 0x50;
            
            System.arraycopy(buf1, 0x80, buf2, 0x90, 0x30);
            System.arraycopy(buf1, 0xC0, buf2, 0x90 + 0x30, 0x10);
            System.arraycopy(buf1, 0x12C, buf2, 0x90 + 0x30 + 0x10, 0x10);
            
            byte[] tmp = new byte[0x50];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = buf2[0x90 + i];
            }
        
            // Round XOR with xor1 and xor2.
            RoundXOR(tmp, 0x50, xor1, xor2);
            
            System.arraycopy(tmp, 0, buf2, 0x90, 0x50);
            
            // Decrypt signature (type 3).
            ByteBuffer bSIGin = ByteBuffer.wrap(buf2);
            ByteBuffer bSIGout = ByteBuffer.wrap(buf4);
            kirk.hleUtilsBufferCopyWithRange(bSIGout, 0xB4, bSIGin, 0x150, KIRK.PSP_KIRK_CMD_DECRYPT_SIGN);

            // Regenerate signature.
            System.arraycopy(buf1, 0xD0, buf2, 0, 0x4);
            for(int i = 0; i < 0x58; i++) {
                buf2[0x4 + i] = 0;
            }
            System.arraycopy(buf1, 0x140, buf2, 0x5C, 0x10);
            System.arraycopy(buf1, 0x12C, buf2, 0x6C, 0x14);
            System.arraycopy(buf4, 0x40, buf2, 0x6C, 0x10);
            System.arraycopy(buf4, 0, buf2, 0x80, 0x30);
            System.arraycopy(buf4, 0x30, buf2, 0xB0, 0x10);
            System.arraycopy(buf1, 0xB0, buf2, 0xC0, 0x10);
            System.arraycopy(buf1, 0, buf2, 0xD0, 0x80);
        } else if ((type == 5) || (type == 7) || (type == 10)) { 
            System.arraycopy(buf1, 0x80, buf2, 0x14, 0x30);
            System.arraycopy(buf1, 0xC0, buf2, 0x44, 0x10);
            System.arraycopy(buf1, 0x12C, buf2, 0x54, 0x10);
            
            byte[] tmp = new byte[0x50];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = buf2[0x14 + i];
            }
        
            // Round XOR with xor1 and xor2.
            RoundXOR(tmp, 0x50, xor1, xor2);
            
            System.arraycopy(tmp, 0, buf2, 0x14, 0x50);

            // Apply scramble.
            ScramblePRX(buf2, 0x50, (byte) (pti.code & 0xFF));
        
            // Copy to buf4.
            System.arraycopy(buf2, 0, buf4, 0, 0x50);

            // Regenerate signature.
            System.arraycopy(buf1, 0xD0, buf2, 0, 0x4);
            for (int i = 0; i < 0x58; i++) {
                buf2[0x4 + i] = 0;
            }
            System.arraycopy(buf1, 0x140, buf2, 0x5C, 0x10);
            System.arraycopy(buf1, 0x12C, buf2, 0x6C, 0x14);
            System.arraycopy(buf4, 0x40, buf2, 0x6C, 0x10);
            System.arraycopy(buf4, 0, buf2, 0x80, 0x30);
            System.arraycopy(buf4, 0x30, buf2, 0xB0, 0x10);
            System.arraycopy(buf1, 0xB0, buf2, 0xC0, 0x10);
            System.arraycopy(buf1, 0, buf2, 0xD0, 0x80);
        } else if ((type == 2) || (type == 4) || (type == 6) || (type == 9)) {
            // Regenerate sig check.
            System.arraycopy(buf1, 0xD0, buf2, 0, 0x5C);
            System.arraycopy(buf1, 0x140, buf2, 0x5C, 0x10);
            System.arraycopy(buf1, 0x12C, buf2, 0x6C, 0x14);
            System.arraycopy(buf1, 0x80, buf2, 0x80, 0x30);
            System.arraycopy(buf1, 0xC0, buf2, 0xB0, 0x10);
            System.arraycopy(buf1, 0xB0, buf2, 0xC0, 0x10);
            System.arraycopy(buf1, 0, buf2, 0xD0, 0x80);
        } else {
            // Regenerate sig check.
            System.arraycopy(buf1, 0xD0, buf2, 0, 0x80);
            System.arraycopy(buf1, 0x80, buf2, 0x80, 0x50);
            System.arraycopy(buf1, 0, buf2, 0xD0, 0x80);
        }
        
        if (type == 1) {
            System.arraycopy(buf2, 0x10, buf4, 0x14, 0xA0);
            ScramblePRX(buf4, 0xA0, (byte) (pti.code & 0xFF));
            System.arraycopy(buf4, 0, buf2, 0x10, 0xA0);
        } else if ((type >= 2) && (type <= 7) || (type == 9) || (type == 10)) { 
            System.arraycopy(buf2, 0x5C, buf4, 0x14, 0x60);
            
            if ((type == 3) || (type == 5) || (type == 7) || (type == 10)) {
                byte[] tmp = new byte[0x60];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = buf4[0x14 + i];
                }
                RoundXOR(tmp, 0x60, xor1, null);
                System.arraycopy(tmp, 0, buf4, 0x14, 0x60);
            }
            ScramblePRX(buf4, 0x60, (byte) (pti.code & 0xFF));
            System.arraycopy(buf4, 0, buf2, 0x5C, 0x60);
        }
        
        if ((type >= 2) && (type <= 7) || (type == 9) || (type == 10)) {
            System.arraycopy(buf2, 0x6C, buf4, 0, 0x14);
            
            if (type == 4) {
                System.arraycopy(buf2, 0, buf2, 0x18, 0x67);
                for(int i = 0; i < 0x18; i++) {
                    buf2[i] = 0;
                }
            } else {
                System.arraycopy(buf2, 0x5C, buf2, 0x70, 0x10);
                
                if ((type == 6) || (type == 7)) {
                    System.arraycopy(buf2, 0x3C, buf5, 0, 0x20);
                    System.arraycopy(buf5, 0, buf2, 0x50, 0x20);
                    for(int i = 0; i < 0x38; i++) {
                        buf2[0x18 + i] = 0;
                    }
                } else {
                    for(int i = 0; i < 0x58; i++) {
                        buf2[0x18 + i] = 0;
                    }
                }
                
                if (unk_0xD4 == 0x80 ) {
                    buf2[0x18] = (byte) 0x80;
                }
            }
            
            // Set the SHA1 block size to digest.
            System.arraycopy(buf2, 0, buf2, 0x4, 4);
            buf2[0] = 0x00;
            buf2[1] = 0x00;
            buf2[2] = 0x01;
            buf2[3] = 0x4C;
            System.arraycopy(buf3, 0, buf2, 0x8, 0x10);
        } else {
            // Set the SHA1 block size to digest.
            System.arraycopy(buf2, 0x4, buf4, 0, 0x14);
            buf2[0] = 0x00;
            buf2[1] = 0x00;
            buf2[2] = 0x01;
            buf2[3] = 0x4C;
            System.arraycopy(buf3, 0, buf2, 0x4, 0x14);
        }
        
        // Generate SHA1 hash.
        ByteBuffer bSHA1Out = ByteBuffer.wrap(buf2);
        kirk.hleUtilsBufferCopyWithRange(bSHA1Out, 0x150, bSHA1Out, 0x150, KIRK.PSP_KIRK_CMD_SHA1_HASH);
        
        if ((type >= 2) && (type <= 7) || (type == 9) || (type == 10)) {
            byte[] tmp1 = new byte[0x40];
            byte[] tmp2 = new byte[0x40];
            byte[] tmp3 = new byte[0x40 + 0x14];
            byte[] tmp4 = new byte[0x40];
            byte[] tmp5 = new byte[0x40];
            byte[] tmp6 = new byte[0x40];
            
            for (int i = 0; i < 0x40; i++) {
                tmp1[i] = buf2[0x80 + i];
                tmp2[i] = buf3[0x10 + i];
            }
            
            MixXOR(tmp1, 0x40, tmp1, tmp2);
            System.arraycopy(tmp1, 0, tmp3, 0x14, 0x40);
            ScramblePRX(tmp3, 0x40, (byte) (pti.code & 0xFF));
            System.arraycopy(tmp3, 0, buf2, 0x80, 0x40);
            
            for (int i = 0; i < 0x40; i++) {
                tmp4[i] = buf[0x40 + i];
                tmp5[i] = buf2[0x80 + i];
                tmp6[i] = buf3[0x50 + i];
            }
            
            MixXOR(tmp4, 0x40, tmp5, tmp6);
            System.arraycopy(tmp4, 0, buf, 0x40, 0x40);

            if ((type == 6) || (type == 7)) {
                System.arraycopy(buf5, 0, buf, 0x80, 0x20);
                for(int i = 0; i < 0x10; i++) {
                    buf[0xA0 + i] = 0;
                }
                buf[0xA4] = 0x0;
                buf[0xA5] = 0x0;
                buf[0xA6] = 0x0;
                buf[0xA7] = 0x1;
                buf[0xA0] = 0x0;
                buf[0xA1] = 0x0;
                buf[0xA2] = 0x0;
                buf[0xA3] = 0x1;
            } else {
                for(int i = 0; i < 0x30; i++) {
                    buf[0x80 + i] = 0;
                }
                buf[0xA0] = 0x0;
                buf[0xA1] = 0x0;
                buf[0xA2] = 0x0;
                buf[0xA3] = 0x1;
            }

            System.arraycopy(buf2, 0xC0, buf, 0xB0, 0x10);
            for(int i = 0; i < 0x10; i++) {
                buf[0xC0 + i] = 0;
            }
            System.arraycopy(buf2, 0xD0, buf, 0xD0, 0x80);
        } else {
            byte[] tmp7 = new byte[0x70];
            byte[] tmp8 = new byte[0x70];
            byte[] tmp9 = new byte[0x70 + 0x14];
            byte[] tmp10 = new byte[0x70];
            byte[] tmp11 = new byte[0x70];
            byte[] tmp12 = new byte[0x70];
            for (int i = 0; i < 0x70; i++) {
                tmp7[i] = buf2[0x40 + i];
                tmp8[i] = buf3[0x14 + i];
            }
            
            MixXOR(tmp7, 0x70, tmp7, tmp8);
            System.arraycopy(tmp7, 0, tmp9, 0x14, 0x70);
            ScramblePRX(tmp9, 0x70, (byte) (pti.code & 0xFF));
            System.arraycopy(tmp9, 0, buf2, 0x40, 0x40);
               
            for (int i = 0; i < 0x70; i++) {
                tmp10[i] = buf[0x40 + i];
                tmp11[i] = buf2[0x40 + i];
                tmp12[i] = buf3[0x20 + i];
            }
            
            MixXOR(tmp10, 0x70, tmp11, tmp12);
            System.arraycopy(tmp10, 0, buf, 0x40, 0x70);
            System.arraycopy(buf2, 0xB0, buf, 0xB0, 0xA0);
        }
        
        if (type == 8) {
            if ((buf[0xA4] & 0x1) != 0x1) {
		return -1;
            }
        }

	if (unk_0xD4 == 0x80) {
            if ((buf[0x590] & 0x1) == 0x1) {
                return -1;
            }
            buf[0x590] |= 0x80;
	}
        
        // Call KIRK CMD1 for final decryption.
        ByteBuffer bDataOut = ByteBuffer.wrap(buf);
        ByteBuffer bHeaderIn = bDataOut.duplicate();
        bHeaderIn.position(0x40);
        kirk.hleUtilsBufferCopyWithRange(bDataOut, size, bHeaderIn, size, KIRK.PSP_KIRK_CMD_DECRYPT_PRIVATE);

	if (retsize < 0x150) {
            for(int i = 0; i < (0x150 - retsize); i++) {
                buf[retsize + i] = 0;
            }
	} 

	return retsize;
    }
}