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

public class SceKernelLwMutexInfo {

    public int size = 60;
    public String name;
    public int attr;
    public int lwMutexUid;
    public int lwMutexOpaqueWorkAreaAddr;
    public int initCount;
    public int lockedCount;
    public int numWaitThreads;

    public final int uid;
    public int threadid;

    public SceKernelLwMutexInfo(int workArea, String name, int count, int attr) {
        Memory mem = Memory.getInstance();
        this.lwMutexOpaqueWorkAreaAddr = workArea;
        this.name = name;
        this.attr = attr;

        initCount = count;
        lockedCount = count;
        numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-LwMutex");
        mem.write32(lwMutexOpaqueWorkAreaAddr, uid);
    }

    public void read(Memory mem, int address) {
        size = mem.read32(address);
        name = Utilities.readStringNZ(mem, address + 4, 31);
        attr = mem.read32(address + 36);
        lwMutexUid = mem.read32(address + 40);
        lwMutexOpaqueWorkAreaAddr = mem.read32(address + 44);
        initCount = mem.read32(address + 48);
        lockedCount = mem.read32(address + 52);
        numWaitThreads = mem.read32(address + 56);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, attr);
        mem.write32(address + 40, lwMutexUid);
        mem.write32(address + 44, lwMutexOpaqueWorkAreaAddr);
        mem.write32(address + 48, initCount);
        mem.write32(address + 52, lockedCount);
        mem.write32(address + 56, numWaitThreads);
    }

    @Override
    public String toString() {
        return String.format("SceKernelLwMutexInfo(uid=%x, name=%s, mutexUid=%x, lwMutexOpaqueWorkAreaAddr=0x%X, initCount=%d, lockedCount=%d, numWaitThreads=%d, attr=0x%X, threadid=0x%X)", uid, name, lwMutexUid, lwMutexOpaqueWorkAreaAddr, initCount, lockedCount, numWaitThreads, attr, threadid);
    }
}