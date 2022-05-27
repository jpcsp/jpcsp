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
package jpcsp.format;

import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.readUByte;
import static jpcsp.util.Utilities.readUHalf;
import static jpcsp.util.Utilities.readWord;

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import jpcsp.crypto.CryptoEngine;
import jpcsp.settings.Settings;

/**
 *
 * @author shadow
 */
@SuppressWarnings("unused")
public class PSP {
    public static final int PSP_HEADER_SIZE = 336;
    public static final int PSP_MAGIC = 0x5053507E;
    public static final int SCE_KERNEL_MAX_MODULE_SEGMENT = 4;
    public static final int AES_KEY_SIZE = 16;
    public static final int CMAC_KEY_SIZE = 16;
    public static final int CMAC_HEADER_HASH_SIZE = 16;
    public static final int CMAC_DATA_HASH_SIZE = 16;
    public static final int CHECK_SIZE = 88;
    public static final int SHA1_HASH_SIZE = 20;
    public static final int KEY_DATA_SIZE = 16;
    private int magic;
    private int mod_attr;
    private int comp_mod_attr;
    private int mod_ver_lo;
    private int mod_ver_hi;
    private String modname;
    private int mod_version;
    private int nsegments;
    private int elf_size;
    private int psp_size;
    private int boot_entry;
    private int modinfo_offset;
    private int bss_size;
    private int[] seg_align = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];
    private int[] seg_address = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];
    private int[] seg_size = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];
    private int[] reserved = new int[5];
    private int devkit_version;
    private int dec_mode;
    private int pad;
    private int overlap_size;
    private int[] aes_key = new int[AES_KEY_SIZE];
    private int[] cmac_key = new int[CMAC_KEY_SIZE];
    private int[] cmac_header_hash = new int[CMAC_HEADER_HASH_SIZE];
    private int comp_size;
    private int comp_offset;
    private int unk1;
    private int unk2;
    private int[] cmac_data_hash = new int[CMAC_DATA_HASH_SIZE];
    private int tag;
    private int[] sig_check = new int[CHECK_SIZE];
    private int[] sha1_hash = new int[SHA1_HASH_SIZE];
    private int[] key_data = new int[KEY_DATA_SIZE];

    public PSP(ByteBuffer f) throws IOException {
        read(f);
    }

    private void read(ByteBuffer f) throws IOException {
        if (f.capacity() == 0) {
            return;
        }

        magic = readWord(f);
        mod_attr = readUHalf(f);
        comp_mod_attr = readUHalf(f);
        mod_ver_lo = readUByte(f);
        mod_ver_hi = readUByte(f);
        modname = readStringNZ(f, 28);
        mod_version = readUByte(f);
        nsegments = readUByte(f);
        elf_size = readWord(f);
        psp_size = readWord(f);
        boot_entry = readWord(f);
        modinfo_offset = readWord(f);
        bss_size = readWord(f);
        seg_align[0] = readUHalf(f);
        seg_align[1] = readUHalf(f);
        seg_align[2] = readUHalf(f);
        seg_align[3] = readUHalf(f);
        seg_address[0] = readWord(f);
        seg_address[1] = readWord(f);
        seg_address[2] = readWord(f);
        seg_address[3] = readWord(f);
        seg_size[0] = readWord(f);
        seg_size[1] = readWord(f);
        seg_size[2] = readWord(f);
        seg_size[3] = readWord(f);
        reserved[0] = readWord(f);
        reserved[1] = readWord(f);
        reserved[2] = readWord(f);
        reserved[3] = readWord(f);
        reserved[4] = readWord(f);
        devkit_version = readWord(f);
        dec_mode = readUByte(f);
        pad = readUByte(f);
        overlap_size = readUHalf(f);
        for (int i = 0; i < AES_KEY_SIZE; i++) {
            aes_key[i] = readUByte(f);
        }
        for (int i = 0; i < CMAC_KEY_SIZE; i++) {
            cmac_key[i] = readUByte(f);
        }
        for (int i = 0; i < CMAC_HEADER_HASH_SIZE; i++) {
            cmac_header_hash[i] = readUByte(f);
        }
        comp_size = readWord(f);
        comp_offset = readWord(f);
        unk1 = readWord(f);
        unk2 = readWord(f);
        for (int i = 0; i < CMAC_DATA_HASH_SIZE; i++) {
            cmac_data_hash[i] = readUByte(f);
        }
        tag = readWord(f);
        for (int i = 0; i < CHECK_SIZE; i++) {
            sig_check[i] = readUByte(f);
        }
        for (int i = 0; i < SHA1_HASH_SIZE; i++) {
            sha1_hash[i] = readUByte(f);
        }
        for (int i = 0; i < KEY_DATA_SIZE; i++) {
            key_data[i] = readUByte(f);
        }
    }

    public ByteBuffer decrypt(ByteBuffer f, boolean isSignChecked, byte[] key) {
        if (f.capacity() == 0) {
            return null;
        }

        CryptoEngine crypto = new CryptoEngine();
        byte[] inBuf;
        if (f.hasArray() && f.position() == 0 && f.arrayOffset() == 0) {
    		inBuf = f.array();
        } else {
        	int currentPosition = f.position();
        	int size = Math.min(Math.max(psp_size, elf_size), f.remaining());
        	inBuf = new byte[size];
        	f.get(inBuf);
        	f.position(currentPosition);
        }

        int inSize = inBuf.length;
        byte[] elfBuffer = crypto.getPRXEngine().DecryptAndUncompressPRX(inBuf, inSize, isSignChecked, key);

        if (elfBuffer == null) {
        	return null;
        }

        if (CryptoEngine.getExtractEbootStatus()) {
            try {
                String ebootPath = Settings.getInstance().getDiscTmpDirectory();
                new File(ebootPath).mkdirs();
                RandomAccessFile raf = new RandomAccessFile(ebootPath + "EBOOT.BIN", "rw");
                raf.write(elfBuffer);
                raf.close();
            } catch (IOException e) {
                // Ignore.
            }
        }

        return ByteBuffer.wrap(elfBuffer);
    }

    public boolean isValid() {
        return magic == PSP_MAGIC; // ~PSP
    }

    public String getModname() {
    	return modname;
    }

    public int getDevkitVersion() {
    	return devkit_version;
    }

    public int getModuleElfVersion() {
    	return (mod_ver_hi << 8) | mod_ver_lo;
    }
}