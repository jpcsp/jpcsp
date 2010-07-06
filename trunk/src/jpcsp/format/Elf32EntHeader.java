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
import static jpcsp.util.Utilities.readUByte;
import static jpcsp.util.Utilities.readUHalf;
import static jpcsp.util.Utilities.readUWord;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.Memory;

public class Elf32EntHeader
{
    // Resolved version of modulename and in a Java String
    private String modulenamez;

    private long modulename;
    private int version;
    private int attr;
    private short size;
    private short vcount;
    private int fcount;
    private long resident;

    public static int sizeof() { return 16; }
    public Elf32EntHeader(ByteBuffer f) throws IOException
    {
        modulenamez = "";

        modulename = readUWord(f);
        version = readUHalf(f);
        attr = readUHalf(f);
        size = readUByte(f);
        vcount = readUByte(f);
        fcount = readUHalf(f);
        resident = readUWord(f);
    }

    public Elf32EntHeader(Memory mem, int address)
    {
        modulenamez = "";

        modulename = mem.read32(address);
        version = mem.read16(address + 4);
        attr = mem.read16(address + 6);
        size = (short)mem.read8(address + 8);
        vcount = (short)mem.read8(address + 9);
        fcount = mem.read16(address + 10);
        resident = mem.read32(address + 12);
    }

    @Override
	public String toString()
    {
        StringBuilder str = new StringBuilder();
        if (modulenamez != null && modulenamez.length() > 0)
            str.append(modulenamez + "\n");
        str.append("modulename" + "\t" +  formatString("long", Long.toHexString(modulename & 0xFFFFFFFFL).toUpperCase()) + "\n");
        str.append("version" + "\t\t" +  formatString("short", Long.toHexString(version & 0xFFFF).toUpperCase()) + "\n");
        str.append("attr" + "\t\t" +  formatString("short", Long.toHexString(attr & 0xFFFF).toUpperCase()) + "\n");
        str.append("size" + "\t\t" +  formatString("byte", Long.toHexString(size & 0xFFFF).toUpperCase()) + "\n");
        str.append("vcount" + "\t\t" +  formatString("byte", Long.toHexString(vcount & 0xFFFF).toUpperCase()) + "\n");
        str.append("fcount" + "\t\t" +  formatString("short", Long.toHexString(fcount & 0xFFFF).toUpperCase()) + "\n");
        str.append("resident" + "\t\t" +  formatString("long", Long.toHexString(resident & 0xFFFFFFFFL).toUpperCase()) + "\n");
        return str.toString();
    }

    public String getModuleNamez() {
        return modulenamez;
    }

    public void setModuleNamez(String moduleName) {
        modulenamez = moduleName;
    }

    public long getOffsetModuleName() {
        return modulename;
    }

    public int getVersion() {
        return version;
    }

    public int getAttr() {
        return attr;
    }

    public int getSize() {
        return size;
    }

    public int getVariableCount() {
        return vcount;
    }

    public int getFunctionCount() {
        return fcount;
    }

    public long getOffsetResident() {
        return resident;
    }
}
