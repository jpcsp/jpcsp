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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import jpcsp.util.Utilities;
import static jpcsp.util.Utilities.*;

public class PBP {

    private static String info;
    private long p_magic;
    private long p_version;
    private long p_offset_param_sfo;
    private long p_icon0_png;
    private long offset_icon1_pmf;
    private long offset_pic0_png;
    private long offset_pic1_png;
    private long offset_snd0_at3;
    private long offset_psp_data;
    private long offset_psar_data;
    private long pbp_size;
    private long size_p_offset_param_sfo;
    private long size_p_icon0_png;
    private long size_offset_icon1_pmf;
    private long size_offset_pic0_png;
    private long size_offset_pic1_png;
    private long size_offset_snd0_at3;
    private long size_offset_psp_data;
    private long size_offset_psar_data;

    public PBP(RandomAccessFile f) throws IOException {
        read(f);
    }

    public boolean isValid() {
        return Long.toHexString(getP_magic() & 0xFFFFFFFFL).equals("50425000");
    }

    public static void setInfo(String msg) {
        info = msg;
    }

    public static String getInfo() {
        return info;
    }

    private void read(RandomAccessFile f) throws IOException {
        p_magic = readUWord(f);
        p_version = readUWord(f);
        p_offset_param_sfo = readUWord(f);
        p_icon0_png = readUWord(f);
        offset_icon1_pmf = readUWord(f);
        offset_pic0_png = readUWord(f);
        offset_pic1_png = readUWord(f);
        offset_snd0_at3 = readUWord(f);
        offset_psp_data = readUWord(f);
        offset_psar_data = readUWord(f);
        pbp_size = f.length();
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("-----PBP HEADER---------" + "\n");
        str.append("p_magic " + "\t\t" + Utilities.formatString("long", Long.toHexString(getP_magic() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_version " + "\t\t" + Utilities.formatString("long", Long.toHexString(getP_version() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset_param_sfo " + "\t" + Utilities.formatString("long", Long.toHexString(getP_offset_param_sfo() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_icon0_png " + "\t\t" + Utilities.formatString("long", Long.toHexString(getP_icon0_png() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("offset_icon1_pmf " + "\t" + Utilities.formatString("long", Long.toHexString(getOffset_icon1_pmf() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("offset_pic0_png " + "\t" + Utilities.formatString("long", Long.toHexString(getOffset_pic0_png() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("offset_pic1_png " + "\t" + Utilities.formatString("long", Long.toHexString(getOffset_pic1_png() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("offset_snd0_at3 " + "\t" + Utilities.formatString("long", Long.toHexString(getOffset_snd0_at3() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("offset_psp_data " + "\t" + Utilities.formatString("long", Long.toHexString(getOffset_psp_data() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("offset_psar_data " + "\t" + Utilities.formatString("long", Long.toHexString(getOffset_psar_data() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        return str.toString();
    }

    public long getP_magic() {
        return p_magic;
    }

    public long getP_version() {
        return p_version;
    }

    public long getP_offset_param_sfo() {
        return p_offset_param_sfo;
    }

    public long getP_icon0_png() {
        return p_icon0_png;
    }

    public long getOffset_icon1_pmf() {
        return offset_icon1_pmf;
    }

    public long getOffset_pic0_png() {
        return offset_pic0_png;
    }

    public long getOffset_pic1_png() {
        return offset_pic1_png;
    }

    public long getOffset_snd0_at3() {
        return offset_snd0_at3;
    }

    public long getOffset_psp_data() {
        return offset_psp_data;
    }

    public long getOffset_psar_data() {
        return offset_psar_data;
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public void unpackPBP(RandomAccessFile f) throws IOException {
        size_p_offset_param_sfo = p_icon0_png - p_offset_param_sfo;
        size_p_icon0_png = offset_icon1_pmf - p_icon0_png;
        size_offset_icon1_pmf = offset_pic0_png - offset_icon1_pmf;
        size_offset_pic0_png = offset_pic1_png - offset_pic0_png;
        size_offset_pic1_png = offset_snd0_at3 - offset_pic1_png;
        size_offset_snd0_at3 = offset_psp_data - offset_snd0_at3;
        size_offset_psp_data = offset_psar_data - offset_psp_data;
        size_offset_psar_data = pbp_size - offset_psar_data; //not needed?

        File dir = new File("unpacked-pbp");
        deleteDir(dir);//delete all files and directory

        dir.mkdir();
        if (size_p_offset_param_sfo > 0)//correct
        {
            byte[] data = new byte[(int) size_p_offset_param_sfo];
            f.seek(p_offset_param_sfo);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/param.sfo");
            f1.write(data);
            f1.close();
        }
        if (size_p_icon0_png > 0) {
            byte[] data = new byte[(int) size_p_icon0_png];
            f.seek(p_icon0_png);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/icon0.png");
            f1.write(data);
            f1.close();
        }
        if (size_offset_icon1_pmf > 0) {
            byte[] data = new byte[(int) size_offset_icon1_pmf];
            f.seek(offset_icon1_pmf);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/icon1.pmf");
            f1.write(data);
            f1.close();
        }
        if (size_offset_pic0_png > 0) {
            byte[] data = new byte[(int) size_offset_pic0_png];
            f.seek(offset_pic0_png);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/pic0.png");
            f1.write(data);
            f1.close();
        }
        if (size_offset_pic1_png > 0) {
            byte[] data = new byte[(int) size_offset_pic1_png];
            f.seek(offset_pic1_png);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/pic1.png");
            f1.write(data);
            f1.close();
        }
        if (size_offset_snd0_at3 > 0) {
            byte[] data = new byte[(int) size_offset_snd0_at3];
            f.seek(offset_snd0_at3);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/snd0.at3");
            f1.write(data);
            f1.close();
        }
        if (size_offset_psp_data > 0) {
            byte[] data = new byte[(int) size_offset_psp_data];
            f.seek(offset_psp_data);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/data.psp");
            f1.write(data);
            f1.close();
        }
        if (size_offset_psar_data > 0) {
            byte[] data = new byte[(int) size_offset_psar_data];
            f.seek(offset_psar_data);
            f.readFully(data);
            FileOutputStream f1 = new FileOutputStream("unpacked-pbp/data.psar");
            f1.write(data);
            f1.close();
        }
    }
}
