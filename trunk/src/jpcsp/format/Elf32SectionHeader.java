/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.format;

import java.io.IOException;
import java.io.RandomAccessFile;
import jpcsp.util.Utilities;
import static jpcsp.util.Utilities.*;

public class Elf32SectionHeader {

    private String sh_namez = "";
    private long sh_name;
    private int sh_type;
    private int sh_flags;
    private long sh_addr;
    private long sh_offset;
    private long sh_size;
    private int sh_link;
    private int sh_info;
    private int sh_addralign;
    private long sh_entsize;

    private static int sizeof() {
        return 40;
    }
    public Elf32SectionHeader(RandomAccessFile f) throws IOException {
        read(f);
    }
    private void read(RandomAccessFile f) throws IOException {
        sh_name = readUWord(f);
        sh_type = readWord(f);
        sh_flags = readWord(f);
        sh_addr = readUWord(f);
        sh_offset = readUWord(f);
        sh_size = readUWord(f);
        sh_link = readWord(f);
        sh_info = readWord(f);
        sh_addralign = readWord(f);
        sh_entsize = readWord(f);
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("sh_name " + "\t " + Utilities.formatString("long", Long.toHexString(getSh_name() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_type " + "\t " + Utilities.formatString("long", Long.toHexString(getSh_type() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_flags " + "\t " + Utilities.integerToHex(getSh_flags() & 0xFF) + "\n");
        str.append("sh_addr " + "\t " + Utilities.formatString("long", Long.toHexString(getSh_addr() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_offset " + "\t " + Utilities.formatString("long", Long.toHexString(getSh_offset() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_size " + "\t " + Utilities.formatString("long", Long.toHexString(getSh_size() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_link " + "\t " + Utilities.integerToHex(getSh_link() & 0xFF) + "\n");
        str.append("sh_info " + "\t " + Utilities.integerToHex(getSh_info() & 0xFF) + "\n");
        str.append("sh_addralign " + "\t " + Utilities.integerToHex(getSh_addralign() & 0xFF) + "\n");
        str.append("sh_entsize " + "\t " + Utilities.formatString("long", Long.toHexString(getSh_entsize() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        return str.toString();
    }

    public String getSh_namez() {
        return sh_namez;
    }

    public void setSh_namez(String sh_namez) {
        this.sh_namez = sh_namez;
    }

    public long getSh_name() {
        return sh_name;
    }

    public int getSh_type() {
        return sh_type;
    }

    public int getSh_flags() {
        return sh_flags;
    }

    public long getSh_addr() {
        return sh_addr;
    }

    public long getSh_offset() {
        return sh_offset;
    }

    public long getSh_size() {
        return sh_size;
    }

    public int getSh_link() {
        return sh_link;
    }

    public int getSh_info() {
        return sh_info;
    }

    public int getSh_addralign() {
        return sh_addralign;
    }

    public long getSh_entsize() {
        return sh_entsize;
    }
}
