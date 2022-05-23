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

import static jpcsp.HLE.Modules.memlmdModule;
import static jpcsp.HLE.Modules.semaphoreModule;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_MODE_CMD1;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_MODE_DECRYPT_CBC;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.intArrayToByteArray;
import static jpcsp.util.Utilities.read8;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Processor;
import jpcsp.Allegrex.Interpreter;
import jpcsp.HLE.TPointer;
import jpcsp.format.KL4E;
import jpcsp.util.Utilities;

public class PRX {
    public static Logger log = CryptoEngine.log;
    // enum SceExecFileAttr
    public static final int SCE_EXEC_FILE_COMPRESSED      = 0x001;
    public static final int SCE_EXEC_FILE_ELF             = 0x002;
    public static final int SCE_EXEC_FILE_GZIP_OVERLAP    = 0x008;
    public static final int SCE_EXEC_FILE_KL4E_COMPRESSED = 0x200;
    // enum SceExecFileDecryptMode
    public static final int DECRYPT_MODE_NO_EXEC           =  0;
    public static final int DECRYPT_MODE_BOGUS_MODULE      =  1;
    public static final int DECRYPT_MODE_KERNEL_MODULE     =  2;
    public static final int DECRYPT_MODE_VSH_MODULE        =  3;
    public static final int DECRYPT_MODE_USER_MODULE       =  4;
    public static final int DECRYPT_MODE_UMD_GAME_EXEC     =  9;
    public static final int DECRYPT_MODE_GAMESHARING_EXEC  = 10;
    public static final int DECRYPT_MODE_MS_UPDATER        = 12;
    public static final int DECRYPT_MODE_DEMO_EXEC         = 13;
    public static final int DECRYPT_MODE_APP_MODULE        = 14;
    public static final int DECRYPT_MODE_INSTALL_EXEC      = 18;
    public static final int DECRYPT_MODE_POPS_EXEC         = 20;
    public static final int DECRYPT_MODE_SPRX              = 23;

    public PRX() {
    }

	// PRXDecrypter TAG struct.
    public static class TAG_INFO {
        int tag; // 4 byte value at offset 0xD0 in the PRX file
        int[] key; // 144 bytes keys
        int[] xor1; // Optional xor1
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
        }

        public TAG_INFO(int tag, int[] key, int[] xor1, int code) {
            this.tag = tag;
            this.key = key;
            this.xor1 = xor1;
            this.code = code;
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

        @Override
		public String toString() {
        	return String.format("tag=0x%08X, key=%s, code=0x%02X, codeExtra=0x%02X", tag, Utilities.getMemoryDump(intArrayToByteArray(key)), code, codeExtra);
		}
    }
    
    private TAG_INFO g_tagInfo[] = {
    	// 16-bytes keys
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
        new TAG_INFO(0x4C9480F0, KeyVault.keys630_k0, 0x43),
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
        new TAG_INFO(0x4C941BF0, KeyVault.keys555_k2, 0x43),
        new TAG_INFO(0x4C941AF0, KeyVault.keys555_k1, 0x43),
        new TAG_INFO(0x457B0BF0, KeyVault.keys505_a, 0x5B),
        new TAG_INFO(0x4C9420F0, KeyVault.keys505_k2, 0x43),
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
//        new TAG_INFO(0x00000000, KeyVault.key_21C0, 0x42), // Duplicate tag 0x00000000
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
        new TAG_INFO(0x63BAB403, KeyVault.key_102DC8AF_1, 0x51),
        new TAG_INFO(0x0B2B90F0, KeyVault.key_9DC14891_1, 0x5C),
        new TAG_INFO(0x0B2B91F0, KeyVault.key_9DC14891_2, 0x5C),
        new TAG_INFO(0x0B2B92F0, KeyVault.key_9DC14891_3, 0x5C),
        new TAG_INFO(0x0DAA06F0, KeyVault.key_0DAA06F0, KeyVault.xor_0DAA06F0, 0x65),
        new TAG_INFO(0x89742B04, KeyVault.key_89742B04, KeyVault.xor_89742B04, 0x65),
        new TAG_INFO(0xE92408F0, KeyVault.key_E92408F0, KeyVault.xor_E92408F0, 0x65),
    	// 144-bytes keys
        new TAG_INFO(0x00000000, KeyVault.g_key00, 0x42, 0x00),
        new TAG_INFO(0x02000000, KeyVault.key_5C3A61FE, 0x45, 0x00),
        new TAG_INFO(0x03000000, KeyVault.g_key03, 0x46, 0x00),
        new TAG_INFO(0x03000000, KeyVault.g_key04, 0x47, 0x00),
        new TAG_INFO(0x03000000, KeyVault.g_key05, 0x48, 0x00),
        new TAG_INFO(0x03000000, KeyVault.g_key06, 0x49, 0x00),
        new TAG_INFO(0x03000000, KeyVault.g_key0A, 0x4D, 0x00),
        new TAG_INFO(0x03000000, KeyVault.g_key0D, 0x50, 0x00),
        new TAG_INFO(0x03000000, KeyVault.g_key0E, 0x51, 0x00),
        new TAG_INFO(0x4467415d, KeyVault.g_key44, 0x59, 0x59),
        new TAG_INFO(0x207bbf2f, KeyVault.g_key20, 0x5A, 0x5A),
        new TAG_INFO(0x3ace4dce, KeyVault.g_key3A, 0x5B, 0x5B),
        new TAG_INFO(0x07000000, KeyVault.g_key_INDEXDAT1xx, 0x4A, 0x00),
        new TAG_INFO(0x08000000, KeyVault.g_keyEBOOT1xx, 0x4B, 0x00),
        new TAG_INFO(0xC0CB167C, KeyVault.g_keyEBOOT2xx, 0x5D, 0x5D),
        new TAG_INFO(0x0B000000, KeyVault.g_keyUPDATER, 0x4E, 0x00),
        new TAG_INFO(0x0C000000, KeyVault.g_keyDEMOS27X, 0x4F, 0x00),
        new TAG_INFO(0x0F000000, KeyVault.g_keyMEIMG250, 0x52, 0x00),
        new TAG_INFO(0x862648D1, KeyVault.key_7A0E484C, 0x52, 0x00),
        new TAG_INFO(0x207BBF2F, KeyVault.g_keyUNK1, 0x5A, 0x5A),
        new TAG_INFO(0x09000000, KeyVault.g_key_GAMESHARE1xx, 0x4C, 0x00),
        new TAG_INFO(0xBB67C59F, KeyVault.g_keyB8, 0x5C, 0x5C),
        new TAG_INFO(0xBB67C59F, KeyVault.g_key_GAMESHARE2xx, 0x5E, 0x5E),
        new TAG_INFO(0xBB67C59F, KeyVault.g_key4C, 0x5F, 0x5F),
        new TAG_INFO(0xBB67C59F, KeyVault.g_key7F, 0x60, 0x60),
        new TAG_INFO(0xBB67C59F, KeyVault.g_key1B, 0x61, 0x61),
    	new TAG_INFO(0x0E000000, KeyVault.key_102DC8AF_2, 0x51, 0x00),
    	new TAG_INFO(0x06000000, KeyVault.key_06000000, 0x49, 0x00),
    	new TAG_INFO(0xE42C2303, KeyVault.key_E42C2303, 0x49, 0x00)};

    private TAG_INFO GetTagInfo(int tag) {
    	List<TAG_INFO> tagInfos = new LinkedList<TAG_INFO>();
        for (TAG_INFO tagInfo : g_tagInfo) {
            if (tagInfo.tag == tag) {
            	tagInfos.add(tagInfo);
            }
        }

        if (tagInfos.size() == 0) {
        	return null;
        }

        if (tagInfos.size() > 1) {
	        log.warn(String.format("GetTagInfo found multiple TAG_INFO for tag=0x%08X, using the last one:", tag));
	        for (TAG_INFO tagInfo : tagInfos) {
	        	log.warn(String.format("%s", tagInfo));
	        }
        }

    	return tagInfos.get(tagInfos.size() - 1);
    }

    private int ScramblePRX(byte[] buf, int offset, int size, int code) {
    	return ScramblePRX(buf, offset, size, code, true);
    }

    private int ScramblePRX(byte[] buf, int offset, int size, int code, boolean dumpError) {
        // Set CBC mode.
    	writeUnaligned32(buf, offset + 0, PSP_KIRK_CMD_MODE_DECRYPT_CBC);

        // Set unknown parameters to 0.
    	writeUnaligned32(buf, offset + 4, 0);
    	writeUnaligned32(buf, offset + 8, 0);

        // Set the the key seed to code.
    	writeUnaligned32(buf, offset + 12, code & 0xFF);

        // Set the the data size to size.
    	writeUnaligned32(buf, offset + 16, size);

        return semaphoreModule.hleUtilsBufferCopyWithRange(buf, offset, size, buf, offset, size + 0x14, KIRK.PSP_KIRK_CMD_DECRYPT, dumpError);
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

    private void MixXOR(byte[] outbuf, int outOffset, int size, byte[] inbuf, int inOffset, byte[] xor, int xorOffset) {
        for (int i = 0; i < size; i++) {
            outbuf[outOffset + i] = (byte) ((inbuf[inOffset + i] & 0xFF) ^ (xor[xorOffset + i] & 0xFF));
        }
    }

    private void MixXOR(byte[] outbuf, int size, byte[] inbuf, byte[] xor) {
    	MixXOR(outbuf, 0, size, inbuf, 0, xor, 0);
    }

    private void RoundXOR(byte[] buf, int offset, int size, byte[] xor1, byte[] xor2) {
        for (int i = 0; i < size; i++) {
            if (!isNullKey(xor1)) {
                buf[offset + i] ^= xor1[i & 0xf];
            }

            if (!isNullKey(xor2)) {
                buf[offset + i] ^= xor2[i & 0xf];
            }
        }
    }

    public byte[] DecryptAndUncompressPRX(byte[] buf, int size, boolean isSignChecked, byte[] key) {
    	return DecryptAndUncompressPRX(buf, size, isSignChecked, key, null, null, 0);
    }

    public byte[] DecryptAndUncompressPRX(byte[] buf, int size, boolean isSignChecked, byte[] key, TPointer kl4eDecompress, TPointer tempBuffer, int tempBufferSize) {
        int compAttribute = readUnaligned16(buf, 0x6);
    	int pspSize = readUnaligned32(buf, 0x2C);
    	int elfSize = readUnaligned32(buf, 0x28);
        int decryptMode = read8(buf, 0x7C);
    	int tag = readUnaligned32(buf, 0xD0);

    	byte[] resultBuffer = new byte[Math.max(elfSize, pspSize)];
    	System.arraycopy(buf, 0, resultBuffer, 0, Math.min(size, resultBuffer.length));

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("DecryptAndUncompressPRX size=0x%X, compAttribute=0x%X, pspSize=0x%X, elfSize=0x%X, decryptMode=0x%X, tag=0x%08X, key=%s", size, compAttribute, pspSize, elfSize, decryptMode, tag, Utilities.getMemoryDump(key)));
    	}

    	byte[] xor = null;
    	int type;
        switch (decryptMode) {
        	case DECRYPT_MODE_VSH_MODULE:
        		if (isSignChecked) {
	            	int result = memlmdModule.hleMemlmd_6192F715(resultBuffer, size);
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("DecryptPRX: memlmd_6192F715 returning 0x%X: %s", result, Utilities.getMemoryDump(resultBuffer, 0, 0x80 + 0xD0)));
	            	}
        		}

                if (tag == 0x02000000) {
                	type = 8;
                } else {
                	type = 9;
                }
        		break;
        	case DECRYPT_MODE_USER_MODULE:
        		if (isSignChecked) {
	            	int result = memlmdModule.hleMemlmd_6192F715(resultBuffer, size);
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("DecryptPRX: memlmd_6192F715 returning 0x%X: %s", result, Utilities.getMemoryDump(resultBuffer, 0, 0x80 + 0xD0)));
	            	}
        		}

        		if (tag == 0x457B90F0) {
        			type = 9;
        		} else if (tag == 0x457B8AF0 || tag == 0x457B80F0) {
        			type = 6;
        		} else if (tag == 0x457B0CF0 || tag == 0x457B0BF0 || tag == 0x457B0AF0 || tag == 0x457B08F0 || tag == 0x457B08F0 || tag == 0x457B05F0 || tag == 0x76202403) {
        			type = 2;
        		} else if (tag == 0x3ACE4DCE) {
        			type = 1;
        		} else if (tag == 0x03000000) {
        			type = 0;
        		} else {
        			type = 9;
        		}
        		break;
        	case DECRYPT_MODE_UMD_GAME_EXEC:
        		type = 9;
        		break;
        	case DECRYPT_MODE_GAMESHARING_EXEC:
        		type = 2;
        		break;
        	case DECRYPT_MODE_MS_UPDATER:
        		type = 8;
        		break;
        	case DECRYPT_MODE_DEMO_EXEC:
        	case DECRYPT_MODE_APP_MODULE:
        		type = 4;
        		break;
        	case DECRYPT_MODE_POPS_EXEC:
        		type = 5;
        		break;
        	case DECRYPT_MODE_BOGUS_MODULE:
        	case DECRYPT_MODE_KERNEL_MODULE:
        		if (isSignChecked) {
	            	int result = memlmdModule.hleMemlmd_6192F715(resultBuffer, size);
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("DecryptPRX: memlmd_6192F715 returning 0x%X: %s", result, Utilities.getMemoryDump(resultBuffer, 0, 0x80 + 0xD0)));
	            	}
        		}

        		type = 2;
        		break;
        	case DECRYPT_MODE_INSTALL_EXEC:
        		type = 3;
        		break;
        	case DECRYPT_MODE_SPRX:
        		xor = new byte[0x10];
        		for (int i = 0; i < xor.length; i++) {
        			xor[i] = (byte) KeyVault.xor_91E0A9AD[i];
        		}
        		type = 5;
        		break;
    		default:
    			log.error(String.format("DecryptAndUncompressPRX unknown decryptMode=0x%X", decryptMode));
        		log.error(String.format("%s", Utilities.getMemoryDump(buf, 0, size)));
    			type = 2;
    			break;
        }

        int resultSize = DecryptPRX(resultBuffer, size, type, xor, key);
        if (resultSize < 0) {
        	log.error(String.format("DecryptPRX returning %d", resultSize));
        	return null;
        }
        if (log.isTraceEnabled()) {
        	log.trace(String.format("DecryptPRX size=0x%X returned %s", pspSize, Utilities.getMemoryDump(resultBuffer, 0, pspSize)));
        }

        if ((compAttribute & SCE_EXEC_FILE_COMPRESSED) != 0) {
        	if ((compAttribute & 0xF00) == 0) {
	        	// GZIP compressed
	        	try {
					GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(resultBuffer, 0, pspSize));
					byte[] elfBuffer = new byte[elfSize];
					int elfOffset = 0;
					while (elfOffset < elfSize) {
						int length = in.read(elfBuffer, elfOffset, elfSize - elfOffset);
						if (length <= 0) {
							break;
						}
						elfOffset += length;
					}
					in.close();

					// Return the uncompressed ELF file
					resultSize = elfOffset;
					resultBuffer = elfBuffer;
				} catch (IOException e) {
					log.error(e);
				}
        	} else if (kl4eDecompress != null && kl4eDecompress.isNotNull()) {
        		try {
        			int offset = 0;
        			tempBuffer.alignUp(15);
        			TPointer inputBufferAddr = new TPointer(tempBuffer, offset);
        			offset += Utilities.alignUp(resultSize, 15); // Stack must be 16-bytes aligned
        			int stackSize = Utilities.alignUp(3000, 15);
        			TPointer stackBufferAddr = new TPointer(tempBuffer, offset);
        			offset += stackSize;
        			offset = Utilities.alignUp(offset, 63);
        			TPointer outputBufferAddr = new TPointer(tempBuffer, offset);

        			inputBufferAddr.setArray(0, resultBuffer, 4, resultSize - 4);

        			if (log.isDebugEnabled()) {
        				log.debug(String.format("Calling KL4E decompress at %s: input %s(size=0x%X), output %s(size=0x%X)", kl4eDecompress, inputBufferAddr, resultSize, outputBufferAddr, elfSize));
        			}

        			Processor processor = new Processor();
        			processor.cpu.setMemory(kl4eDecompress.getMemory());
        			processor.cpu._a0 = outputBufferAddr.getAddress();
        			processor.cpu._a1 = elfSize;
        			processor.cpu._a2 = inputBufferAddr.getAddress();
        			processor.cpu._a3 = 0;
        			processor.cpu._sp = (stackBufferAddr.getAddress() + stackSize) | 0x80000000;
        			Interpreter interpreter = new Interpreter(processor);
        			interpreter.run(kl4eDecompress.getAddress());

        			int result = processor.cpu._v0;
					if (log.isDebugEnabled()) {
						log.debug(String.format("KL4E decompress returned 0x%08X", result));
					}
        			if (result < 0) {
        				return null;
        			}

        			resultSize = result;
					resultBuffer = outputBufferAddr.getArray8(0, resultSize);
        		} catch (Exception e) {
					log.error("KL4E decompress", e);
					return null;
				}
        	} else {
        		if (log.isDebugEnabled()) {
        			log.debug(String.format("DecryptAndUncompressPRX KL4E: %s", Utilities.getMemoryDump(resultBuffer, 0, resultSize)));
        		}

        		// KLE4 compressed
        		byte[] elfBuffer = new byte[elfSize];
        		int decompressResult = KL4E.decompress_kle(elfBuffer, 0, elfSize, resultBuffer, 4, null, true);

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("DecryptAndUncompressPRX KL4E result 0x%X: %s", decompressResult, Utilities.getMemoryDump(elfBuffer, 0, elfSize)));
        		}

				// Return the uncompressed ELF file
        		resultSize = decompressResult;
        		resultBuffer = elfBuffer;
        	}
        }

        // Truncate the resultBuffer if too long
        if (resultSize >= 0 && resultBuffer.length > resultSize) {
        	byte[] newBuffer = new byte[resultSize];
        	System.arraycopy(resultBuffer, 0, newBuffer, 0, resultSize);
        	resultBuffer = newBuffer;
        }

        if (log.isTraceEnabled()) {
        	log.trace(String.format("DecryptAndUncompressPRX returning size=0x%X: %s", resultSize, Utilities.getMemoryDump(resultBuffer)));
        }

        return resultBuffer;
    }

    private int dumpDecryptHeader(PrintStream out, int addr, int code, int length) {
    	out.format("write32 0x%08X 0x%X", addr, PSP_KIRK_CMD_MODE_DECRYPT_CBC);
    	out.println();
    	out.format("write32 0x%08X 0x%X", addr + 4, 0x0);
    	out.println();
    	out.format("write32 0x%08X 0x%X", addr + 8, 0x0);
    	out.println();
    	out.format("write32 0x%08X 0x%X", addr + 12, code);
    	out.println();
    	out.format("write32 0x%08X 0x%X", addr + 16, length);
    	out.println();

    	return addr + 20;
    }

    private int dumpBuffer(PrintStream out, int addr, byte[] buffer, int offset, int length) {
    	for (int i = 0; i < length; i += 4) {
    		out.format("write32 0x%08X 0x%08X", addr + i, Utilities.readUnaligned32(buffer, offset + i));
        	out.println();
    	}
    	return addr + length;
    }

    private int dumpBuffer(PrintStream out, int addr, int buffer, int length) {
    	if (addr != buffer) {
    		out.format("memcpy 0x%08X 0x%08X 0x%X", addr, buffer, length);
        	out.println();
    	}
    	return addr + length;
    }

    private void dumpDecrypt(PrintStream out, int addrIn, int addrOut, int length, String fileName) {
    	out.format("ExecuteKirkCommand 0x%X %s 0x%X 0x%08X 0x%X", KIRK.PSP_KIRK_CMD_DECRYPT, fileName, length, addrIn, length + 20);
    	out.println();
    	out.format("ExecuteKirkCommand 0x%X 0x%08X 0x%X 0x%08X 0x%X", KIRK.PSP_KIRK_CMD_DECRYPT, addrOut, length, addrIn, length + 20);
    	out.println();
    }

    private void dumpXor(PrintStream out, int addrOut, int addrIn, byte[] xor, int xorOffset, int xorLength, int addrXor, int length) {
    	if (xor == null) {
    		if (addrOut != addrIn) {
    			out.format("memcpy 0x%08X 0x%08X 0x%X", addrOut, addrIn, length);
    	    	out.println();
    		}
    	} else {
	    	dumpBuffer(out, addrXor, xor, xorOffset, xorLength);
	    	while (length > 0) {
	    		int stepLength = Math.min(length, xorLength);

	    		out.format("xor 0x%08X 0x%08X 0x%08X 0x%X", addrOut, addrIn, addrXor, stepLength);
	        	out.println();

	        	addrOut += stepLength;
	    		addrIn += stepLength;
	    		length -= stepLength;
	    	}
    	}
    }

    private void dumpXor(PrintStream out, int addrOut, int addrIn, byte[] xor, int addrXor, int length) {
    	dumpXor(out, addrOut, addrIn, xor, 0, xor == null ? 0 : xor.length, addrXor, length);
    }

    private void dumpErrorForType5(byte[] xor1, byte[] xor2, int code, byte[] buf1, byte[] buf3) {
    	try {
        	log.error(String.format("Copy the created file"));
        	log.error(String.format("    JpcspTrace.config"));
        	log.error(String.format("to your MemoryStick on the PSP under seplugins/JpcspTrace.config"));
        	log.error(String.format("Then run the JpcspTrace plugin on your PSP."));
        	log.error(String.format("After the run of JpcspTrace, the file PreDecrypt.xml will be created on your MemoryStick."));
        	log.error(String.format("Add the content of this file to the Jpcsp file"));
        	log.error(String.format("  src/jpcsp/crypto/PreDecrypt.xml"));
        	log.error(String.format("under the section marked with 'Pre-decrypted data for POPS'"));

        	PrintStream out = new PrintStream("JpcspTrace.config");
        	out.format("LogBufferLength 0x2000");
        	out.println();
        	out.format("BufferLogWrites");
        	out.println();

        	int baseAddr1 = 0xBFC00500;
	    	int addr;
	    	int length = 0x50;
	    	addr = dumpDecryptHeader(out, baseAddr1, code, length);
	    	addr = dumpBuffer(out, addr, buf1, 0x80, 0x30);
	    	addr = dumpBuffer(out, addr, buf1, 0xC0, 0x10);
	    	addr = dumpBuffer(out, addr, buf1, 0x12C, 0x10);
	    	int tempAddr = baseAddr1 + 20 + length;
	    	dumpXor(out, baseAddr1 + 20, baseAddr1 + 20, xor1, tempAddr, length);
	    	dumpXor(out, baseAddr1 + 20, baseAddr1 + 20, xor2, tempAddr, length);
	    	dumpDecrypt(out, baseAddr1, baseAddr1, length, "ms0:/PreDecrypt.xml");

	    	int baseAddr2 = baseAddr1 + length;
	    	length = 0x60;
	    	addr = dumpDecryptHeader(out, baseAddr2, code, length);
	    	addr = dumpBuffer(out, addr, buf1, 0x140, 0x10);
	    	addr = dumpBuffer(out, addr, baseAddr1 + 0x40, 0x10);
	    	addr = dumpBuffer(out, addr, buf1, 0x13C, 0x4);
	    	addr = dumpBuffer(out, addr, baseAddr1, 0x3C);
	    	tempAddr = baseAddr2 + 20 + length;
	    	dumpXor(out, baseAddr2 + 20, baseAddr2 + 20, xor1, tempAddr, length);
	    	dumpDecrypt(out, baseAddr2, baseAddr2, length, ">ms0:/PreDecrypt.xml");

	    	dumpBuffer(out, baseAddr2 + length, baseAddr1 + 0x3C, 0x4);
	    	int baseAddr3 = baseAddr2 + length + 0x4;
	    	length = 0x40;
	    	tempAddr = baseAddr3 + length;
	    	dumpXor(out, baseAddr3, baseAddr2 + 0x24, buf3, 0x10, 0x40, tempAddr, length);

	    	int baseAddr4 = baseAddr3 + length;
	    	length = 0x40;
	    	addr = dumpDecryptHeader(out, baseAddr4, code, length);
	    	addr = dumpBuffer(out, addr, baseAddr3, 0x40);
	    	dumpDecrypt(out, baseAddr4, baseAddr4, length, ">ms0:/PreDecrypt.xml");

        	out.format("FlushLogBuffer");
        	out.println();
        	out.close();
    	} catch (IOException e) {
			log.error(e);
		}
    }

    public int DecryptPRX(byte[] buf, int size, int type, byte[] xor1, byte[] xor2) {
    	return DecryptPRX(buf, size, type, xor1, xor2, false);
    }

    public int DecryptPRX(byte[] buf, int size, int type, byte[] xor1, byte[] xor2, boolean forceNewMethod) {
        // Fetch the PRX tag.
        int tag = readUnaligned32(buf, 0xD0);

        // Get the tag info.
        TAG_INFO pti = GetTagInfo(tag);
        if (pti == null) {
        	log.error(String.format("DecryptPRX unknown tag 0x%08X", tag));
            return -1;
        }

        return DecryptPRX(buf, size, type, xor1, xor2, forceNewMethod, pti);
    }

    public int DecryptPRX(byte[] buf, int size, int type, byte[] xor1, byte[] xor2, boolean forceNewMethod, TAG_INFO pti) {
    	int result = 0;

        if (pti.xor1 != null && xor1 == null) {
        	xor1 = intArrayToByteArray(pti.xor1);
        }

        // Fetch the PRX tag.
        int tag = readUnaligned32(buf, 0xD0);

        // Fetch the final ELF size.
        int retsize = readUnaligned32(buf, 0xB0);
        int dataOffset = readUnaligned16(buf, 0xB4);

        // Old encryption method (144 bytes key).
        if (((type >= 2 && type <= 7) || type == 9 || type == 10) && pti.key.length > 0x10 && !forceNewMethod) {
            // Setup the buffers.
            byte[] oldbuf = new byte[size];
            byte[] oldbuf1 = new byte[0x150];

            System.arraycopy(buf, 0, oldbuf, 0, size);

            for (int i = 0; i < 0x150; i++) {
                oldbuf[i] = 0;
            }
            for (int i = 0; i < 0x40; i++) {
                oldbuf[i] = 0x55;
            }

            writeUnaligned32(oldbuf, 0x2C + 0, PSP_KIRK_CMD_MODE_DECRYPT_CBC);
            writeUnaligned32(oldbuf, 0x2C + 4, 0);
            writeUnaligned32(oldbuf, 0x2C + 8, 0);
            writeUnaligned32(oldbuf, 0x2C + 12, pti.code);
            writeUnaligned32(oldbuf, 0x2C + 16, 0x70);

            System.arraycopy(buf, 0xD0, oldbuf1, 0, 0x80);
            System.arraycopy(buf, 0x80, oldbuf1, 0x80, 0x50);
            System.arraycopy(buf, 0, oldbuf1, 0xD0, 0x80);

            if (pti.codeExtra != 0) {
                byte[] tmp = new byte[0x14 + 0xA0];
                System.arraycopy(oldbuf1, 0x10, tmp, 0x14, 0xA0);
                ScramblePRX(tmp, 0, 0xA0, pti.codeExtra);
                System.arraycopy(tmp, 0, oldbuf1, 0x10, 0xA0);
            }

            System.arraycopy(oldbuf1, 0x40, oldbuf, 0x40, 0x40);

            for (int iXOR = 0; iXOR < 0x70; iXOR++) {
                oldbuf[0x40 + iXOR] = (byte) (oldbuf[0x40 + iXOR] ^ pti.key[0x14 + iXOR]);
            }

            // Scramble the data by calling CMD7.
            result = semaphoreModule.hleUtilsBufferCopyWithRange(oldbuf, 0x2C, 0x70, oldbuf, 0x2C, 0x70 + 0x14, KIRK.PSP_KIRK_CMD_DECRYPT);
            if (result != 0) {
            	log.error(String.format("DecryptPRX: KIRK command PSP_KIRK_CMD_DECRYPT returned error %d", result));
            }

            for (int iXOR = 0x6F; iXOR >= 0; iXOR--) {
                oldbuf[0x40 + iXOR] = (byte) (oldbuf[0x2C + iXOR] ^ pti.key[0x20 + iXOR]);
            }

            for (int k = 0; k < 0x30; k++) {
                oldbuf[k + 0x80] = 0;
            }

            // Set mode field to 1.
            oldbuf[0xA0] = 0x0;
            oldbuf[0xA1] = 0x0;
            oldbuf[0xA2] = 0x0;
            oldbuf[0xA3] = 0x1;

            System.arraycopy(buf, 0xB0, oldbuf, 0xB0, 0x20);
            System.arraycopy(buf, 0, oldbuf, 0xD0, 0x80);

            // Call KIRK CMD1 for final decryption.
            result = semaphoreModule.hleUtilsBufferCopyWithRange(buf, 0, size, oldbuf, 0x40, size - 0x40, KIRK.PSP_KIRK_CMD_DECRYPT_PRIVATE);
            if (result != 0) {
            	log.error(String.format("DecryptPRX: KIRK command PSP_KIRK_CMD_DECRYPT_PRIVATE returned error %d", result));
            }
        } else { // New encryption method (16 bytes key).
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

            if (size - 0x150 < retsize) {
            	return -206;
            }

            // Read in the user key and apply scramble.
            if ((type >= 2 && type <= 7) || type == 9 || type == 10) {
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

            ScramblePRX(buf2, 0, 0x90, pti.code);

            // Round XOR key for PRX type 3,5,7 and 10.
            if (type == 3 || type == 5 || type == 7 || type == 10) {
                if (!isNullKey(xor2)) {
                    RoundXOR(buf2, 0, 0x90, xor2, null);
                }
            }

            System.arraycopy(buf2, 0, buf3, 0, 0x90);

            // Type 9 and 10 specific step.
            if (type == 9 || type == 10) {
                System.arraycopy(buf, 0x104, buf6, 0, buf6.length);

                for (int i = 0; i < buf6.length; i++) {
                    buf[0x104 + i] = 0;
                }

                System.arraycopy(buf6, 0, sigbuf, 0, sigbuf.length);

                writeUnaligned32(buf, 0, size - 4);

                // Generate SHA1 hash.
                result = semaphoreModule.hleUtilsBufferCopyWithRange(buf, 0, size, buf, 0, size, KIRK.PSP_KIRK_CMD_SHA1_HASH);
                if (result != 0) {
                	log.error(String.format("DecryptPRX: KIRK command PSP_KIRK_CMD_SHA1_HASH returned error %d", result));
                }

                System.arraycopy(buf, 0, sha1buf, 0, sha1buf.length);
                System.arraycopy(buf1, 0, buf, 0, 0x20);

                int[] pubKey;
                switch ((tag >> 16) & 0xFF) {
                	case 0x16: pubKey = KeyVault.g_pubkey_28752; break;
                	case 0x5E: pubKey = KeyVault.g_pubkey_28712; break;
                	default:   pubKey = KeyVault.g_pubkey_28672; break;
                }
                for (int i = 0; i < pubKey.length; i++) {
                	buf4[i] = (byte) pubKey[i];
                }

                System.arraycopy(sha1buf, 0, buf4, 0x28, sha1buf.length);
                System.arraycopy(sigbuf, 0, buf4, 0x28 + sha1buf.length, sigbuf.length);

                // Verify ECDSA signature.
                result = semaphoreModule.hleUtilsBufferCopyWithRange(null, 0, 0, buf4, 0, 100, KIRK.PSP_KIRK_CMD_ECDSA_VERIFY);
                if (result != 0) {
                	log.error(String.format("DecryptPRX: KIRK command PSP_KIRK_CMD_ECDSA_VERIFY returned error %d", result));
                }
            }

            if (type == 3) {
                System.arraycopy(buf1, 0xEC, buf2, 0, 0x40);
                for (int i = 0; i < 0x50; i++) {
                    buf2[0x40 + i] = 0;
                }

                buf2[0x60] = 0x03;
                buf2[0x70] = 0x50;

                System.arraycopy(buf1, 0x80, buf2, 0x90, 0x30);
                System.arraycopy(buf1, 0xC0, buf2, 0xC0, 0x10);
                System.arraycopy(buf1, 0x12C, buf2, 0xD0, 0x10);

                // Round XOR with xor1 and xor2.
                RoundXOR(buf2, 0x90, 0x50, xor1, xor2);

                // Decrypt signature (type 3).
                result = semaphoreModule.hleUtilsBufferCopyWithRange(buf4, 0, 0xB4, buf2, 0, 0x150, KIRK.PSP_KIRK_CMD_DECRYPT_SIGN);
                if (result != 0) {
                	log.error(String.format("DecryptPRX: KIRK command PSP_KIRK_CMD_DECRYPT_SIGN returned error %d", result));
                }

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
            } else if (type == 5 || type == 7 || type == 10) {
            	System.arraycopy(buf1, 0x80, buf2, 0x14, 0x30);
                System.arraycopy(buf1, 0xC0, buf2, 0x44, 0x10);
                System.arraycopy(buf1, 0x12C, buf2, 0x54, 0x10);

                // Round XOR with xor1 and xor2.
                RoundXOR(buf2, 0x14, 0x50, xor1, xor2);

                // Type 5 is used for POPS or scePauth
                boolean dumpError = type != 5;

                // Apply scramble.
                result = ScramblePRX(buf2, 0, 0x50, pti.code, dumpError);

                if (result != 0 && !dumpError) {
                	dumpErrorForType5(xor1, xor2, pti.code, buf1, buf3);
                	return -1;
                }

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
            } else if (type == 2 || type == 4 || type == 6 || type == 9) {
                // Regenerate sig check.
                System.arraycopy(buf1, 0xD0, buf2, 0, 0x5C);
                System.arraycopy(buf1, 0x140, buf2, 0x5C, 0x10);
                System.arraycopy(buf1, 0x12C, buf2, 0x6C, 0x14);
                System.arraycopy(buf1, 0x80, buf2, 0x80, 0x30);
                System.arraycopy(buf1, 0xC0, buf2, 0xB0, 0x10);
                System.arraycopy(buf1, 0xB0, buf2, 0xC0, 0x10);
                System.arraycopy(buf1, 0, buf2, 0xD0, 0x80);
                if (type == 9) {
                	for (int i = 0; i < 0x28; i++) {
                		buf2[0x34 + i] = (byte) 0;
                	}
                }
            } else {
                // Regenerate sig check.
                System.arraycopy(buf1, 0xD0, buf2, 0, 0x80);
                System.arraycopy(buf1, 0x80, buf2, 0x80, 0x50);
                System.arraycopy(buf1, 0, buf2, 0xD0, 0x80);
            }

            if (type == 1) {
                System.arraycopy(buf2, 0x10, buf4, 0x14, 0xA0);
                ScramblePRX(buf4, 0, 0xA0, pti.code);
                System.arraycopy(buf4, 0, buf2, 0x10, 0xA0);
            } else if ((type >= 2 && type <= 7) || type == 9 || type == 10) {
                System.arraycopy(buf2, 0x5C, buf4, 0x14, 0x60);

                if (type == 3 || type == 5 || type == 7 || type == 10) {
                    RoundXOR(buf4, 0x14, 0x60, xor1, null);
                }
                ScramblePRX(buf4, 0, 0x60, pti.code);
                System.arraycopy(buf4, 0, buf2, 0x5C, 0x60);
            }

            if ((type >= 2 && type <= 7) || type == 9 || type == 10) {
                System.arraycopy(buf2, 0x6C, buf4, 0, 0x14);

                if (type == 4) {
                    System.arraycopy(buf2, 0, buf2, 0x18, 0x67);
                    for (int i = 0; i < 0x18; i++) {
                        buf2[i] = 0;
                    }
                } else {
                    System.arraycopy(buf2, 0x5C, buf2, 0x70, 0x10);

                    if (type == 6 || type == 7) {
                        System.arraycopy(buf2, 0x3C, buf5, 0, 0x20);
                        System.arraycopy(buf5, 0, buf2, 0x50, 0x20);
                        for (int i = 0; i < 0x38; i++) {
                            buf2[0x18 + i] = 0;
                        }
                    } else {
                        for (int i = 0; i < 0x58; i++) {
                            buf2[0x18 + i] = 0;
                        }
                    }

                    if (unk_0xD4 == 0x80) {
                        buf2[0x18] = (byte) 0x80;
                    }
                }

                // Set the SHA1 block size to digest.
                System.arraycopy(buf2, 0, buf2, 0x4, 4);
                writeUnaligned32(buf2, 0, 0x14C);
                System.arraycopy(buf3, 0, buf2, 0x8, 0x10);
            } else {
                // Set the SHA1 block size to digest.
                System.arraycopy(buf2, 0x4, buf4, 0, 0x14);
                writeUnaligned32(buf2, 0, 0x14C);
                System.arraycopy(buf3, 0, buf2, 0x4, 0x14);
            }

            // Generate SHA1 hash.
            result = semaphoreModule.hleUtilsBufferCopyWithRange(buf2, 0, 0x150, buf2, 0, 0x150, KIRK.PSP_KIRK_CMD_SHA1_HASH);
            if (result != 0) {
            	log.error(String.format("DecryptPRX: KIRK command PSP_KIRK_CMD_SHA1_HASH returned error %d", result));
            }

            if (Utilities.memcmp(buf2, 0, buf4, 0, 0x14) != 0) {
            	log.error(String.format("DecryptPRX: SHA1 Hash not matching: %s%s", Utilities.getMemoryDump(buf2, 0, 0x14), Utilities.getMemoryDump(buf4, 0, 0x14)));
            }

            if ((type >= 2 && type <= 7) || type == 9 || type == 10) {
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
                ScramblePRX(tmp3, 0, 0x40, pti.code);
                System.arraycopy(tmp3, 0, buf2, 0x80, 0x40);

                for (int i = 0; i < 0x40; i++) {
                    tmp4[i] = buf[0x40 + i];
                    tmp5[i] = buf2[0x80 + i];
                    tmp6[i] = buf3[0x50 + i];
                }

                MixXOR(tmp4, 0x40, tmp5, tmp6);
                System.arraycopy(tmp4, 0, buf, 0x40, 0x40);

                if (type == 6 || type == 7) {
                    System.arraycopy(buf5, 0, buf, 0x80, 0x20);
                    for (int i = 0; i < 0x10; i++) {
                        buf[0xA0 + i] = 0;
                    }
                    buf[0xA4] = 0x0;
                    buf[0xA5] = 0x0;
                    buf[0xA6] = 0x0;
                    buf[0xA7] = 0x1;
                    writeUnaligned32(buf, 0xA0, PSP_KIRK_CMD_MODE_CMD1);
                } else {
                    for (int i = 0; i < 0x30; i++) {
                        buf[0x80 + i] = 0;
                    }
                    writeUnaligned32(buf, 0xA0, PSP_KIRK_CMD_MODE_CMD1);
                }

                System.arraycopy(buf2, 0xC0, buf, 0xB0, 0x10);
                for (int i = 0; i < 0x10; i++) {
                    buf[0xC0 + i] = 0;
                }
                System.arraycopy(buf2, 0xD0, buf, 0xD0, 0x80);
            } else {
                MixXOR(buf2, 0x40, 0x70, buf2, 0x40, buf3, 0x14);
                ScramblePRX(buf2, 0x2C, 0x70, pti.code);
                MixXOR(buf, 0x40, 0x70, buf2, 0x2C, buf3, 0x20);
                System.arraycopy(buf2, 0xB0, buf, 0xB0, 0xA0);
            }

            // Check only done for UPDATE_VER > 6.20
            int firmwareVersion = Emulator.getInstance().getFirmwareVersion();
            if (type == 8 && firmwareVersion > 620 && firmwareVersion != 999) {
                if (buf[0xA4] != 0x01) {
                	if (log.isDebugEnabled()) {
                		log.debug(String.format("error -303, type=%d, firmwareVersion=%d: %s", type, Emulator.getInstance().getFirmwareVersion(), Utilities.getMemoryDump(buf, 0, 0x150)));
                	}
                    return -303;
                }
            }

            if (unk_0xD4 == 0x80) {
                if (buf[0x590] != 0x00) {
                    return -302;
                }
                buf[0x590] |= 0x80;
            }

            // Call KIRK CMD1 for final decryption.
            int inSize = 0x90 + alignUp(retsize, 15) + dataOffset;
            int outSize = alignUp(retsize, 15);
            result = semaphoreModule.hleUtilsBufferCopyWithRange(buf, 0, outSize, buf, 0x40, inSize, KIRK.PSP_KIRK_CMD_DECRYPT_PRIVATE);
            if (result != 0) {
            	log.error(String.format("DecryptPRX: KIRK command PSP_KIRK_CMD_DECRYPT_PRIVATE returned error %d", result));
            }

            if (retsize < 0x150) {
                for (int i = 0; i < (0x150 - retsize); i++) {
                    buf[retsize + i] = 0;
                }
            }
        }

        return retsize;
    }

    public int encryptPRX(byte[] buffer, int bufferLength, byte[] key) {
    	if (bufferLength < 0x160) {
    		return -202;
    	}

    	for (int i = 0; i < 0x18; i++) {
    		if (buffer[0xD4 + i] != 0) {
    			return -302;
    		}
    	}

    	// Fetch the PRX tag.
        int tag = readUnaligned32(buffer, 0xD0);

        // Get the tag info.
        PRX.TAG_INFO pti = GetTagInfo(tag);
        if (pti == null) {
        	log.error(String.format("encryptPRX unknown tag 0x%08X", tag));
            return -301;
        }

    	byte[] xor2 = key;
    	byte[] xor1 = null;
        if (pti.xor1 != null) {
        	xor1 = intArrayToByteArray(pti.xor1);
        }

    	final byte[] encryptBuffer = new byte[0x150];

    	System.arraycopy(buffer, 0xEC, encryptBuffer, 0, 0x40);
    	encryptBuffer[0x60] = (byte) 0x02;
    	encryptBuffer[0x70] = (byte) 0x50;
    	System.arraycopy(buffer, 0x80, encryptBuffer, 0x90, 0x30);
    	System.arraycopy(buffer, 0xC0, encryptBuffer, 0xC0, 0x10);
    	System.arraycopy(buffer, 0x12C, encryptBuffer, 0xD0, 0x10);

        RoundXOR(encryptBuffer, 0x90, 0x50, xor1, xor2);

        // Encrypt signature (type 2).
        int result = semaphoreModule.hleUtilsBufferCopyWithRange(encryptBuffer, 0, 0x150, encryptBuffer, 0, 0x150, KIRK.PSP_KIRK_CMD_ENCRYPT_SIGN);
        if (result != 0) {
        	log.error(String.format("encryptPRX: KIRK command PSP_KIRK_CMD_ENCRYPT_SIGN returned error %d", result));
        }

        RoundXOR(encryptBuffer, 0x90, 0x50, xor1, xor2);

        System.arraycopy(encryptBuffer, 0x0, buffer, 0xEC, 0x40);
        System.arraycopy(encryptBuffer, 0x90, buffer, 0x80, 0x30);
        System.arraycopy(encryptBuffer, 0xC0, buffer, 0xC0, 0x10);
        System.arraycopy(encryptBuffer, 0xD0, buffer, 0x12C, 0x10);

        return 0;
    }
}