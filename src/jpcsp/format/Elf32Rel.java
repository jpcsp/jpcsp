/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.format;

import jpcsp.util.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import static jpcsp.util.Utilities.*;

public class Elf32Rel {

    private long r_offset;
    private long r_info;

    public static int sizeof() {
        return 8;
    }

    public void read(RandomAccessFile f) throws IOException {
        setR_offset(readUWord(f));
        setR_info(readUWord(f));
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("r_offset " + "\t " + Utilities.formatString("long", Long.toHexString(getR_offset() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("r_info " + "\t\t " + Utilities.formatString("long", Long.toHexString(getR_info() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        return str.toString();
    }

    public long getR_offset() {
        return r_offset;
    }

    public void setR_offset(long r_offset) {
        this.r_offset = r_offset;
    }

    public long getR_info() {
        return r_info;
    }

    public void setR_info(long r_info) {
        this.r_info = r_info;
    }
}
