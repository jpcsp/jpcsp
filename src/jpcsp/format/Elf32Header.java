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

import static jpcsp.util.Utilities.readUByte;
import static jpcsp.util.Utilities.readUHalf;
import static jpcsp.util.Utilities.readUWord;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.MemoryMap;
import jpcsp.util.Utilities;

public class Elf32Header {


    private long e_magic;
    private int e_class;
    private int e_data;
    private int e_idver;
    private byte[] e_pad = new byte[9];
    private int e_type;
    private int e_machine;
    private long e_version;
    private long e_entry;
    private long e_phoff;
    private long e_shoff;
    private long e_flags;
    private int e_ehsize;
    private int e_phentsize;
    private int e_phnum;
    private int e_shentsize;
    private int e_shnum;
    private int e_shstrndx;

    private void read(ByteBuffer f) throws IOException {
        if (f.capacity() == 0)
            return;
        e_magic = readUWord(f);
        e_class = readUByte(f);
        e_data = readUByte(f);
        e_idver = readUByte(f);
        f.get(getE_pad());         //can raise EOF exception
        e_type = readUHalf(f);
        e_machine = readUHalf(f);
        e_version = readUWord(f);
        e_entry = readUWord(f);
        e_phoff = readUWord(f);
        e_shoff = readUWord(f);
        e_flags = readUWord(f);
        e_ehsize = readUHalf(f);
        e_phentsize = readUHalf(f);
        e_phnum = readUHalf(f);
        e_shentsize = readUHalf(f);
        e_shnum = readUHalf(f);
        e_shstrndx = readUHalf(f);
    }


     public Elf32Header(ByteBuffer f) throws IOException {
        read(f);
    }

     public boolean isValid(){
        return (Long.toHexString( getE_magic() & 0xFFFFFFFFL).toUpperCase().equals("464C457F"));
     }

     public boolean isMIPSExecutable(){
        return (Integer.toHexString(getE_machine() & 0xFFFF).equals("8"));
     }

     public boolean isPRXDetected(){
        return ((getE_type() & 0xFFFF) == 0xFFA0);
     }

     public boolean requiresRelocation(){
        return (isPRXDetected() || (getE_entry() < MemoryMap.START_RAM));
     }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("-----ELF HEADER---------" + "\n");
        str.append("e_magic " + "\t " + Utilities.formatString("long", Long.toHexString(getE_magic() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("e_class " + "\t " + Utilities.integerToHex(getE_class() & 0xFF) + "\n");
        // str.append("e_class " + "\t " +  Utilities.formatString("byte", Integer.toHexString(e_class & 0xFF ).toUpperCase())+ "\n");
        str.append("e_data " + "\t\t " + Utilities.formatString("byte", Integer.toHexString(getE_data() & 0xFF).toUpperCase()) + "\n");
        str.append("e_idver " + "\t " + Utilities.formatString("byte", Integer.toHexString(getE_idver() & 0xFF).toUpperCase()) + "\n");
        str.append("e_type " + "\t\t " + Utilities.formatString("short", Integer.toHexString(getE_type() & 0xFFFF).toUpperCase()) + "\n");
        str.append("e_machine " + "\t " + Utilities.formatString("short", Integer.toHexString(getE_machine() & 0xFFFF).toUpperCase()) + "\n");
        str.append("e_version " + "\t " + Utilities.formatString("long", Long.toHexString(getE_version() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("e_entry " + "\t " + Utilities.formatString("long", Long.toHexString(getE_entry() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("e_phoff " + "\t " + Utilities.formatString("long", Long.toHexString(getE_phoff() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("e_shoff " + "\t " + Utilities.formatString("long", Long.toHexString(getE_shoff() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("e_flags " + "\t " + Utilities.formatString("long", Long.toHexString(getE_flags() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("e_ehsize " + "\t " + Utilities.formatString("short", Integer.toHexString(getE_ehsize() & 0xFFFF).toUpperCase()) + "\n");
        str.append("e_phentsize " + "\t " + Utilities.formatString("short", Integer.toHexString(getE_phentsize() & 0xFFFF).toUpperCase()) + "\n");
        str.append("e_phnum " + "\t " + Utilities.formatString("short", Integer.toHexString(getE_phnum() & 0xFFFF).toUpperCase()) + "\n");
        str.append("e_shentsize " + "\t " + Utilities.formatString("short", Integer.toHexString(getE_shentsize() & 0xFFFF).toUpperCase()) + "\n");
        str.append("e_shnum " + "\t " + Utilities.formatString("short", Integer.toHexString(getE_shnum() & 0xFFFF).toUpperCase()) + "\n");
        str.append("e_shstrndx " + "\t " + Utilities.formatString("short", Integer.toHexString(getE_shstrndx() & 0xFFFF).toUpperCase()) + "\n");
        return str.toString();
    }

    public long getE_magic() {
        return e_magic;
    }

    public int getE_class() {
        return e_class;
    }

    public int getE_data() {
        return e_data;
    }

    public int getE_idver() {
        return e_idver;
    }

    public byte[] getE_pad() {
        return e_pad;
    }

    public int getE_type() {
        return e_type;
    }

    public int getE_machine() {
        return e_machine;
    }

    public long getE_version() {
        return e_version;
    }

    public long getE_entry() {
        return e_entry;
    }

    public long getE_phoff() {
        return e_phoff;
    }

    public long getE_shoff() {
        return e_shoff;
    }

    public long getE_flags() {
        return e_flags;
    }

    public int getE_ehsize() {
        return e_ehsize;
    }

    public int getE_phentsize() {
        return e_phentsize;
    }

    public int getE_phnum() {
        return e_phnum;
    }

    public int getE_shentsize() {
        return e_shentsize;
    }

    public int getE_shnum() {
        return e_shnum;
    }

    public int getE_shstrndx() {
        return e_shstrndx;
    }
}
