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
import jpcsp.util.Utilities;

public class SceKernelSemaInfo {
    public final String name;
    public final int attr;
    public final int initCount;
    public int currentCount;
    public final int maxCount;
    public int numWaitThreads;

    public final int uid;

    public SceKernelSemaInfo(String name, int attr, int initCount, int maxCount)
    {
        this.name = name;
        this.attr = attr;
        this.initCount = initCount;
        this.currentCount = initCount;
        this.maxCount = maxCount;
        this.numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-sema");
    }

    public void write(Memory mem, int address)
    {
        mem.write32(address, 56); // size
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, attr);
        mem.write32(address + 40, initCount);
        mem.write32(address + 44, currentCount);
        mem.write32(address + 48, maxCount);
        mem.write32(address + 52, numWaitThreads);
    }
}
