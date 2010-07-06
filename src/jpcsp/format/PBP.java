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

import static jpcsp.util.Utilities.formatString;
import static jpcsp.util.Utilities.readUWord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PBP {
    private static final long PBP_MAGIC = 0x50425000L;
    private static final String PBP_UNPACK_PATH_PREFIX = "unpacked-pbp/";

    private String info;

    private int size_pbp;
    private int size_param_sfo;
    private int size_icon0_png;
    private int size_icon1_pmf;
    private int size_pic0_png;
    private int size_pic1_png;
    private int size_snd0_at3;
    private int size_psp_data;
    private int size_psar_data;

    private long p_magic;
    private long p_version;
    private long p_offset_param_sfo;
    private long p_offset_icon0_png;
    private long p_offset_icon1_pmf;
    private long p_offset_pic0_png;
    private long p_offset_pic1_png;
    private long p_offset_snd0_at3;
    private long p_offset_psp_data;
    private long p_offset_psar_data;
    private Elf32 elf32;
    private PSF psf;

    public boolean isValid() {
        return (size_pbp != 0 && (p_magic & 0xFFFFFFFFL) == PBP_MAGIC);
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

    public void setInfo(String msg) {
        info = msg;
    }

    public String getInfo() {
        return info;
    }

    public PBP(ByteBuffer f) throws IOException {
        size_pbp = f.capacity();
        if (size_pbp == 0)
            return;
        p_magic = readUWord(f);
        if (isValid())
        {
            p_version = readUWord(f);
            p_offset_param_sfo = readUWord(f);
            p_offset_icon0_png = readUWord(f);
            p_offset_icon1_pmf = readUWord(f);
            p_offset_pic0_png = readUWord(f);
            p_offset_pic1_png = readUWord(f);
            p_offset_snd0_at3 = readUWord(f);
            p_offset_psp_data = readUWord(f);
            p_offset_psar_data = readUWord(f);

            size_param_sfo = (int)(p_offset_icon0_png - p_offset_param_sfo);
            size_icon0_png  = (int)(p_offset_icon1_pmf - p_offset_icon0_png);
            size_icon1_pmf = (int)(p_offset_pic0_png - p_offset_icon1_pmf);
            size_pic0_png = (int)(p_offset_pic1_png - p_offset_pic0_png);
            size_pic1_png = (int)(p_offset_snd0_at3 - p_offset_pic1_png);
            size_snd0_at3 = (int)(p_offset_psp_data - p_offset_snd0_at3);
            size_psp_data = (int)(p_offset_psar_data - p_offset_psp_data);
            size_psar_data = (int)(size_pbp - p_offset_psar_data);

            info = toString();
        }
    }
    public PSF readPSF(ByteBuffer f) throws IOException {
    	if(p_offset_param_sfo > 0) {
           f.position((int)p_offset_param_sfo);
           psf = new PSF(p_offset_param_sfo);
           psf.read(f);
           return psf;
    	}
    	return null;
    }
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("-----PBP HEADER---------" + "\n");
        str.append("p_magic " + "\t\t" + formatString("long", Long.toHexString(p_magic & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_version " + "\t\t" + formatString("long", Long.toHexString(p_version & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_param_sfo " + "\t" + formatString("long", Long.toHexString(p_offset_param_sfo & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_icon0_png " + "\t" + formatString("long", Long.toHexString(p_offset_icon0_png & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_icon1_pmf " + "\t" + formatString("long", Long.toHexString(p_offset_icon1_pmf & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_pic0_png " + "\t" + formatString("long", Long.toHexString(p_offset_pic0_png & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_pic1_png " + "\t" + formatString("long", Long.toHexString(p_offset_pic1_png & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_snd0_at3 " + "\t" + formatString("long", Long.toHexString(p_offset_snd0_at3 & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_psp_data " + "\t" + formatString("long", Long.toHexString(p_offset_psp_data & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_psar_data " + "\t" + formatString("long", Long.toHexString(p_offset_psar_data & 0xFFFFFFFFL).toUpperCase()) + "\n");
        return str.toString();
    }

    public long getMagic() {
        return p_magic;
    }

    public long getVersion() {
        return p_version;
    }

    public long getOffsetParam() {
        return p_offset_param_sfo;
    }

    public long getOffsetIcon0() {
        return p_offset_icon0_png;
    }

    public long getOffsetIcon1() {
        return p_offset_icon1_pmf;
    }

    public long getOffsetPic0() {
        return p_offset_pic0_png;
    }

    public long getOffsetPic1() {
        return p_offset_pic1_png;
    }

    public long getOffsetSnd0() {
        return p_offset_snd0_at3;
    }

    public long getOffsetPspData() {
        return p_offset_psp_data;
    }

    public long getOffsetPsarData() {
        return p_offset_psar_data;
    }

    public int getSizeIcon0() {
		return size_icon0_png;
	}

	public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
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
        if (!pbp.isValid())
        {
            return;
        }
        File dir = new File(PBP_UNPACK_PATH_PREFIX);
        deleteDir(dir);//delete all files and directory
        dir.mkdir();
         if (pbp.size_param_sfo > 0) {
            byte[] data = new byte[pbp.size_param_sfo];
            f.position((int)pbp.p_offset_param_sfo);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "param.sfo");
            f1.write(data);
            f1.close();
        }
        if (pbp.size_icon0_png > 0) {
            byte[] data = new byte[pbp.size_icon0_png];
            f.position((int)pbp.p_offset_icon0_png);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "icon0.png");
            f1.write(data);
            f1.close();
        }
        if (pbp.size_icon1_pmf > 0) {
            byte[] data = new byte[pbp.size_icon1_pmf];
            f.position((int)pbp.p_offset_icon1_pmf);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "icon1.pmf");
            f1.write(data);
            f1.close();
        }
        if (pbp.size_pic0_png > 0) {
            byte[] data = new byte[pbp.size_pic0_png];
            f.position((int)pbp.p_offset_pic0_png);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "pic0.png");
            f1.write(data);
            f1.close();
        }
        if (pbp.size_pic1_png > 0) {
            byte[] data = new byte[pbp.size_pic1_png];
            f.position((int)pbp.p_offset_pic1_png);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "pic1.png");
            f1.write(data);
            f1.close();
        }
        if (pbp.size_snd0_at3 > 0) {
            byte[] data = new byte[pbp.size_snd0_at3];
            f.position((int)pbp.p_offset_snd0_at3);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "snd0.at3");
            f1.write(data);
            f1.close();
        }
        if (pbp.size_psp_data > 0) {
            byte[] data = new byte[pbp.size_psp_data ];
            f.position((int)pbp.p_offset_psp_data);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "data.psp");
            f1.write(data);
            f1.close();
        }
        if (pbp.size_psar_data  > 0) {
            byte[] data = new byte[pbp.size_psar_data];
            f.position((int)pbp.p_offset_psar_data);
            f.get(data);
            FileOutputStream f1 = new FileOutputStream(PBP_UNPACK_PATH_PREFIX + "data.psar");
            f1.write(data);
            f1.close();
        }
    }
}
