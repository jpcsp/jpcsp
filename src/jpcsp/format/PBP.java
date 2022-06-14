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

import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.util.ByteUtil;
import jpcsp.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static jpcsp.util.Utilities.formatString;
import static jpcsp.util.Utilities.read32;
import static jpcsp.util.Utilities.readUWord;

public class PBP {

    public static final int PBP_MAGIC = 0x50425000;
    private static final String PBP_UNPACK_PATH_PREFIX = "unpacked-pbp/";

    static private final String[] FILE_NAMES = new String[]{
        "param.sfo",
        "icon0.png",
        "icon1.pmf",
        "pic0.png",
        "pic1.png",
        "snd0.at3",
        "psp.data",
        "psar.data",};

    static private final int TOTAL_FILES = 8;

    static private final int PARAM_SFO = 0;
    static private final int ICON0_PNG = 1;
    static private final int ICON1_PMF = 2;
    static private final int PIC0_PNG = 3;
    static private final int PIC1_PNG = 4;
    static private final int SND0_AT3 = 5;
    static private final int PSP_DATA = 6;
    static private final int PSAR_DATA = 7;

    static public final int PBP_HEADER_SIZE = 8 + TOTAL_FILES * 4;
    static public final int PBP_PSP_DATA_OFFSET = 8 + PSP_DATA * 4;
    static public final int PBP_PSAR_DATA_OFFSET = 8 + PSAR_DATA * 4;

    private int size_pbp;

    private int p_magic;
    private int p_version;
    private int[] p_offsets;
    private Elf32 elf32;
    private PSF psf;

    public boolean isValid() {
        return size_pbp != 0 && p_magic == PBP_MAGIC;
    }

    public void setElf32(Elf32 elf) {
        elf32 = elf;
    }

    public Elf32 getElf32() {
        return elf32;
    }

    public PSF getPSF() {
        return psf;
    }

    public PBP(ByteBuffer f) throws IOException {
        size_pbp = f.limit();
        if (size_pbp == 0) {
            return;
        }
        p_magic = readUWord(f);
        if (isValid()) {
            p_version = readUWord(f);

            p_offsets = new int[]{readUWord(f), readUWord(f), readUWord(f), readUWord(f), readUWord(f), readUWord(f), readUWord(f), readUWord(f), size_pbp};

        }
    }

    private PBP() {
    }

    public PSF readPSF(ByteBuffer f) throws IOException {
        if (getOffsetParam() > 0) {
            f.position(getOffsetParam());
            psf = new PSF(getOffsetParam());
            psf.read(f);
            return psf;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("-----PBP HEADER---------" + "\n");
        str.append("p_magic \t\t").append(formatString("long", Long.toHexString(p_magic & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_version \t\t").append(formatString("long", Long.toHexString(p_version & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_param_sfo \t").append(formatString("long", Long.toHexString(getOffsetParam() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_icon0_png \t").append(formatString("long", Long.toHexString(getOffsetIcon0() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_icon1_pmf \t").append(formatString("long", Long.toHexString(getOffsetIcon1() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_pic0_png \t").append(formatString("long", Long.toHexString(getOffsetPic0() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_pic1_png \t").append(formatString("long", Long.toHexString(getOffsetPic1() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_snd0_at3 \t").append(formatString("long", Long.toHexString(getOffsetSnd0() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_psp_data \t").append(formatString("long", Long.toHexString(getOffsetPspData() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        str.append("p_offset_psar_data \t").append(formatString("long", Long.toHexString(getOffsetPsarData() & 0xFFFFFFFFL).toUpperCase())).append("\n");
        return str.toString();
    }

    private String getName(int index) {
        return FILE_NAMES[index];
    }

    private int getOffset(int index) {
        return this.p_offsets[index];
    }

    private int getSize(int index) {
        return this.p_offsets[index + 1] - this.p_offsets[index];
    }

    private byte[] getBytes(ByteBuffer f, int index) {
        return ByteUtil.readBytes(f, getOffset(index), getSize(index));
    }

    public int getMagic() {
        return p_magic;
    }

    public int getVersion() {
        return p_version;
    }

    public int getOffsetParam() {
        return getOffset(PARAM_SFO);
    }

    public int getOffsetIcon0() {
        return getOffset(ICON0_PNG);
    }

    public int getOffsetIcon1() {
        return getOffset(ICON1_PMF);
    }

    public int getOffsetPic0() {
        return getOffset(PIC0_PNG);
    }

    public int getOffsetPic1() {
        return getOffset(PIC1_PNG);
    }

    public int getOffsetSnd0() {
        return getOffset(SND0_AT3);
    }

    public int getOffsetPspData() {
        return getOffset(PSP_DATA);
    }

    public int getOffsetPsarData() {
        return getOffset(PSAR_DATA);
    }

    public int getSizeIcon0() {
        return getSize(ICON0_PNG);
    }

    public int getSizePsarData() {
        return getSize(PSAR_DATA);
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String children1 : children) {
                boolean success = deleteDir(new File(dir, children1));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static void unpackPBP(ByteBuffer f) throws IOException {
        f.position(0);//seek to 0
        PBP pbp = new PBP(f);
        if (!pbp.isValid()) {
            return;
        }
        File dir = new File(PBP_UNPACK_PATH_PREFIX);
        deleteDir(dir);//delete all files and directory
        dir.mkdir();

        for (int index = 0; index < TOTAL_FILES; index++) {
            byte[] bytes = pbp.getBytes(f, index);
            if (bytes != null && bytes.length > 0) {
                FileUtil.writeBytes(new File(PBP_UNPACK_PATH_PREFIX + pbp.getName(index)), bytes);
            }
        }
    }

    /**
     * Unpack a PBP file, avoiding to consume too much memory (i.e. not reading
     * each section completely in memory).
     *
     * @param vFile the PBP file
     * @throws IOException
     */
    public static void unpackPBP(IVirtualFile vFile) throws IOException {
        vFile.ioLseek(0L);
        PBP pbp = new PBP();
        pbp.size_pbp = (int) vFile.length();
        pbp.p_magic = read32(vFile);
        if (!pbp.isValid()) {
            return;
        }
        pbp.p_version = read32(vFile);
        pbp.p_offsets = new int[]{read32(vFile), read32(vFile), read32(vFile), read32(vFile), read32(vFile), read32(vFile), read32(vFile), read32(vFile), pbp.size_pbp};

        File dir = new File(PBP_UNPACK_PATH_PREFIX);
        deleteDir(dir); //delete all files and directory
        dir.mkdir();

        final byte[] buffer = new byte[10 * 1024];
        for (int index = 0; index < TOTAL_FILES; index++) {
            int size = pbp.getSize(index);
            if (size > 0) {
                long offset = pbp.getOffset(index) & 0xFFFFFFFFL;
                if (vFile.ioLseek(offset) == offset) {
                    try ( OutputStream os = new FileOutputStream(new File(PBP_UNPACK_PATH_PREFIX + pbp.getName(index)))) {
                        while (size > 0) {
                            int length = Math.min(size, buffer.length);
                            int readLength = vFile.ioRead(buffer, 0, length);
                            if (readLength > 0) {
                                os.write(buffer, 0, readLength);
                                size -= readLength;
                            }
                            if (readLength != length) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
