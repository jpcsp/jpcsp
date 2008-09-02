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
package jpcsp.HLE;

import jpcsp.Memory;

/** http://psp.jim.sh/pspsdk-doc/structSceIoDirent.html */
public class SceIoDirent {
    public SceIoStat stat;
    public String filename;

    public SceIoDirent(SceIoStat stat, String filename) {
        this.stat = stat;
        this.filename = filename;
    }

    public void write(Memory mem, int address) {
        int len, i;

        stat.write(mem, address);

        len = filename.length();
        if (len > 256)
            len = 256;

        for (i = 0; i < len; i++)
            mem.write8(address + SceIoStat.sizeof() + i, (byte)filename.charAt(i));

        // Zero out remaining space, we need at least 1 to safely terminate the string
        for (; i < 256; i++)
            mem.write8(address + SceIoStat.sizeof() + i, (byte)0);

        // 2 ints reserved
        mem.write32(address + SceIoStat.sizeof() + 256, 0xcdcdcdcd);
        mem.write32(address + SceIoStat.sizeof() + 256 + 4, 0xcdcdcdcd);
    }

    public static int sizeof() {
        return SceIoStat.sizeof() + 256 + 8;
    }
}
