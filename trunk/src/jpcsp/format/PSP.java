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

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import jpcsp.State;
import jpcsp.connector.Connector;
import jpcsp.crypto.CryptoEngine;
import jpcsp.util.Utilities;

/**
 *
 * @author shadow
 */
@SuppressWarnings("unused")
public class PSP {

    public static final int PSP_MAGIC = 0x5053507E;
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
    private int[] seg_align = new int[4];
    private int[] seg_address = new int[4];
    private int[] seg_size = new int[4];
    private int[] reserved = new int[5];
    private int devkit_version;
    private int dec_mode;
    private int pad;
    private int overlap_size;
    private int[] aes_key = new int[16];
    private int[] cmac_key = new int[16];
    private int[] cmac_header_hash = new int[16];
    private int comp_size;
    private int comp_offset;
    private int unk1;
    private int unk2;
    private int[] cmac_data_hash = new int[16];
    private int tag;
    private int[] sig_check = new int[88];
    private int[] sha1_hash = new int[20];
    private int[] key_data = new int[16];

    public PSP(ByteBuffer f) throws IOException {
        read(f);
    }

    private void read(ByteBuffer f) throws IOException {
        if (f.capacity() == 0) {
            return;
        }

        magic = Utilities.readWord(f);
        mod_attr = Utilities.readUHalf(f);
        comp_mod_attr = Utilities.readUHalf(f);
        mod_ver_lo = Utilities.readUByte(f);
        mod_ver_hi = Utilities.readUByte(f);
        modname = Utilities.readStringNZ(f, 28);
        mod_version = Utilities.readUByte(f);
        nsegments = Utilities.readUByte(f);
        elf_size = Utilities.readWord(f);
        psp_size = Utilities.readWord(f);
        boot_entry = Utilities.readWord(f);
        modinfo_offset = Utilities.readWord(f);
        bss_size = Utilities.readWord(f);
        seg_align[0] = Utilities.readUHalf(f);
        seg_align[1] = Utilities.readUHalf(f);
        seg_align[2] = Utilities.readUHalf(f);
        seg_align[3] = Utilities.readUHalf(f);
        seg_address[0] = Utilities.readWord(f);
        seg_address[1] = Utilities.readWord(f);
        seg_address[2] = Utilities.readWord(f);
        seg_address[3] = Utilities.readWord(f);
        seg_size[0] = Utilities.readWord(f);
        seg_size[1] = Utilities.readWord(f);
        seg_size[2] = Utilities.readWord(f);
        seg_size[3] = Utilities.readWord(f);
        reserved[0] = Utilities.readWord(f);
        reserved[1] = Utilities.readWord(f);
        reserved[2] = Utilities.readWord(f);
        reserved[3] = Utilities.readWord(f);
        reserved[4] = Utilities.readWord(f);
        devkit_version = Utilities.readWord(f);
        dec_mode = Utilities.readUByte(f);
        pad = Utilities.readUByte(f);
        overlap_size = Utilities.readUHalf(f);
        for (int i = 0; i < 16; i++) {
            aes_key[i] = Utilities.readUByte(f);
        }
        for (int i = 0; i < 16; i++) {
            cmac_key[i] = Utilities.readUByte(f);
        }
        for (int i = 0; i < 16; i++) {
            cmac_header_hash[i] = Utilities.readUByte(f);
        }
        comp_size = Utilities.readWord(f);
        comp_offset = Utilities.readWord(f);
        unk1 = Utilities.readWord(f);
        unk2 = Utilities.readWord(f);
        for (int i = 0; i < 16; i++) {
            cmac_data_hash[i] = Utilities.readUByte(f);
        }
        tag = Utilities.readWord(f);
        for (int i = 0; i < 88; i++) {
            sig_check[i] = Utilities.readUByte(f);
        }
        for (int i = 0; i < 20; i++) {
            sha1_hash[i] = Utilities.readUByte(f);
        }
        for (int i = 0; i < 16; i++) {
            key_data[i] = Utilities.readUByte(f);
        }
    }

    public ByteBuffer decrypt(ByteBuffer f) {
        if (f.capacity() == 0) {
            return null;
        }

        CryptoEngine crypto = new CryptoEngine();
        byte[] inBuf = f.array();
        int inSize = inBuf.length;
        byte[] outBuf = new byte[inSize];
        int fileTag = ((inBuf[0xD0] & 0xFF) << 24) | ((inBuf[0xD1] & 0xFF) << 16)
                | ((inBuf[0xD2] & 0xFF) << 8) | (inBuf[0xD3] & 0xFF);

        int retsize = crypto.DecryptPRX1(inBuf, outBuf, inSize, fileTag);
        if (retsize <= 0) {
            retsize = crypto.DecryptPRX2(inBuf, outBuf, inSize, fileTag);
        }

        if (CryptoEngine.getExtractEbootStatus()) {
            try {
                String ebootPath = Connector.baseDirectory + State.discId + File.separatorChar;
                new File(ebootPath).mkdirs();
                RandomAccessFile raf = new RandomAccessFile(ebootPath + "EBOOT.BIN", "rw");
                raf.write(outBuf, 0, retsize);
                raf.close();
            } catch (Exception e) {
                // Ignore.
            }
        }

        return ByteBuffer.wrap(outBuf, 0, retsize);
    }

    public boolean isValid() {
        return ((magic & PSP_MAGIC) == PSP_MAGIC); // ~PSP
    }
}