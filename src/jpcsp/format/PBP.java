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
}
