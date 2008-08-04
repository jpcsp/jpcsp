/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.format;
import java.io.IOException;
import java.io.RandomAccessFile;
import jpcsp.util.Utilities;
import static jpcsp.util.Utilities.*;

public class Elf32ProgramHeader {

    private long p_type;
    private long p_offset;
    private long p_vaddr;
    private long p_paddr;
    private long p_filesz;
    private long p_memsz;
    private long p_flags; // Bits: 0x1=executable, 0x2=writable, 0x4=readable, demo PRX's were found to be not writable
    private long p_align;

    private static int sizeof() {
        return 32;
    }

    public Elf32ProgramHeader(RandomAccessFile f) throws IOException {
        read(f);
    }

    private void read(RandomAccessFile f) throws IOException {
        p_type = readUWord(f);
        p_offset = readUWord(f);
        p_vaddr = readUWord(f);
        p_paddr = readUWord(f);
        p_filesz = readUWord(f);
        p_memsz = readUWord(f);
        p_flags = readUWord(f);
        p_align = readUWord(f);
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("p_type " + "\t\t " + Utilities.formatString("long", Long.toHexString(getP_type() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_offset " + "\t " + Utilities.formatString("long", Long.toHexString(getP_offset() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_vaddr " + "\t " + Utilities.formatString("long", Long.toHexString(getP_vaddr() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_paddr " + "\t " + Utilities.formatString("long", Long.toHexString(getP_paddr() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_filesz " + "\t " + Utilities.formatString("long", Long.toHexString(getP_filesz() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_memsz " + "\t " + Utilities.formatString("long", Long.toHexString(getP_memsz() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_flags " + "\t " + Utilities.formatString("long", Long.toHexString(getP_flags() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("p_align " + "\t " + Utilities.formatString("long", Long.toHexString(getP_align() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        return str.toString();
    }

    public long getP_type() {
        return p_type;
    }

    public long getP_offset() {
        return p_offset;
    }

    public long getP_vaddr() {
        return p_vaddr;
    }

    public long getP_paddr() {
        return p_paddr;
    }

    public long getP_filesz() {
        return p_filesz;
    }

    public long getP_memsz() {
        return p_memsz;
    }

    public long getP_flags() {
        return p_flags;
    }

    public long getP_align() {
        return p_align;
    }
}
