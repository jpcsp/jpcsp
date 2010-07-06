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
import static jpcsp.util.Utilities.readUWord;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.util.Utilities;

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

    public Elf32ProgramHeader(ByteBuffer f) throws IOException {
        read(f);
    }

    private void read(ByteBuffer f) throws IOException {
        p_type = readUWord(f);
        p_offset = readUWord(f);
        p_vaddr = readUWord(f);
        p_paddr = readUWord(f);
        p_filesz = readUWord(f);
        p_memsz = readUWord(f);
        p_flags = readUWord(f);
        p_align = readUWord(f);
    }

    @Override
	public String toString() {
        StringBuilder str = new StringBuilder();
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
