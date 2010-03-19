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
package jpcsp.HLE.kernel.types;

import jpcsp.Memory;

import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelEventFlagInfo {
    public final String name;
    public final int attr;
    public final int initPattern;
    public int currentPattern;
    public int numWaitThreads;

    public final int uid;

    public SceKernelEventFlagInfo(String name, int attr, int initPattern, int currentPattern)
    {
        this.name = name;
        this.attr = attr;
        this.initPattern = initPattern;
        this.currentPattern = currentPattern;
        this.numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-eventflag");
    }

    public void write(Memory mem, int address)
    {
        mem.write32(address, 52); // size

        int i, len = name.length();
        for (i = 0; i < 32 && i < len; i++)
            mem.write8(address + 4 + i, (byte)name.charAt(i));
        for (; i < 32; i++)
            mem.write8(address + 4 + i, (byte)0);

        mem.write32(address + 36, attr);
        mem.write32(address + 40, initPattern);
        mem.write32(address + 44, currentPattern);
        mem.write32(address + 48, numWaitThreads);
    }
}
