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
import static jpcsp.util.Utilities.integerToHex;
import static jpcsp.util.Utilities.readUWord;
import static jpcsp.util.Utilities.readWord;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.Memory;

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

    public Elf32SectionHeader(ByteBuffer f) throws IOException {
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

    public Elf32SectionHeader(Memory mem, int address) {
        sh_name = mem.read32(address);
        sh_type = mem.read32(address + 4);
        sh_flags = mem.read32(address + 8);
        sh_addr = mem.read32(address + 12);
        sh_offset = mem.read32(address + 16);
        sh_size = mem.read32(address + 20);
        sh_link = mem.read32(address + 24);
        sh_info = mem.read32(address + 28);
        sh_addralign = mem.read32(address + 32);
        sh_entsize = mem.read32(address + 36);
    }

    @Override
	public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("sh_name " + "\t " + formatString("long", Long.toHexString(getSh_name() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_type " + "\t " + formatString("long", Long.toHexString(getSh_type() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_flags " + "\t " + integerToHex(getSh_flags() & 0xFF) + "\n");
        str.append("sh_addr " + "\t " + formatString("long", Long.toHexString(getSh_addr() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_offset " + "\t " + formatString("long", Long.toHexString(getSh_offset() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_size " + "\t " + formatString("long", Long.toHexString(getSh_size() & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("sh_link " + "\t " + integerToHex(getSh_link() & 0xFF) + "\n");
        str.append("sh_info " + "\t " + integerToHex(getSh_info() & 0xFF) + "\n");
        str.append("sh_addralign " + "\t " + integerToHex(getSh_addralign() & 0xFF) + "\n");
        str.append("sh_entsize " + "\t " + formatString("long", Long.toHexString(getSh_entsize() & 0xFFFFFFFFL).toUpperCase()) + "\n");
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

    // Flags
    public static final int SHF_NONE        = 0x00000000;
    public static final int SHF_WRITE       = 0x00000001;
    public static final int SHF_ALLOCATE    = 0x00000002;
    public static final int SHF_EXECUTE     = 0x00000004;

    // Types
    public static final int SHT_NULL        = 0x00000000;
    public static final int SHT_PROGBITS    = 0x00000001;
    public static final int SHT_SYMTAB      = 0x00000002;
    public static final int SHT_STRTAB      = 0x00000003;
    public static final int SHT_RELA        = 0x00000004;
    public static final int SHT_HASH        = 0x00000005;
    public static final int SHT_DYNAMIC     = 0x00000006;
    public static final int SHT_NOTE        = 0x00000007;
    public static final int SHT_NOBITS      = 0x00000008;
    public static final int SHT_REL         = 0x00000009;
    public static final int SHT_SHLIB       = 0x0000000A;
    public static final int SHT_DYNSYM      = 0x0000000B;
    public static final int SHT_PRXREL      = 0x700000A0;
}
