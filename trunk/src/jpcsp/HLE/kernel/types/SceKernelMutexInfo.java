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

public class SceKernelMutexInfo {

    public int size = 52;
    public String name;
    public int attr;
    public int initCount;
    public int lockedCount;
    public int numWaitThreads;

    public final int uid;
    public int threadid;

    public SceKernelMutexInfo(String name, int count, int attr) {
        this.name = name;
        this.attr = attr;

        initCount = count;
        lockedCount = count;
        numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-Mutex");
    }

    public void read(Memory mem, int address) {
        size = mem.read32(address);
        name = Utilities.readStringNZ(mem, address + 4, 31);
        attr = mem.read32(address + 36);
        initCount = mem.read32(address + 40);
        lockedCount = mem.read32(address + 44);
        numWaitThreads = mem.read32(address + 48);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, attr);
        mem.write32(address + 40, initCount);
        mem.write32(address + 44, lockedCount);
        mem.write32(address + 48, numWaitThreads);
    }

    @Override
    public String toString() {
        return String.format("SceKernelMutexInfo(uid=%x, name=%s, initCount=%d, lockedCount=%d, numWaitThreads=%d, attr=0x%X)", uid, name, initCount, lockedCount, numWaitThreads, attr);
    }
}