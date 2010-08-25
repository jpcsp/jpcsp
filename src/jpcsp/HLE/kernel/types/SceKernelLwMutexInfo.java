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

    // PSP info
    public int size = 52;
    public String name;
    public int attr;
    public int mutexUid;
    public int mutexOpaqueWorkAreaAddr;
    public int numWaitThreads;
    public int locked;
    public int threadid;

    // Internal info
    public final int uid;

    public SceKernelLwMutexInfo(int workArea, String name, int attr) {
        Memory mem = Memory.getInstance();
        this.mutexOpaqueWorkAreaAddr = workArea;
        this.name = name;
        this.attr = attr;

        numWaitThreads = 0;
        locked = 0;

        uid = SceUidManager.getNewUid("ThreadMan-LwMutex");
        mem.write32(mutexOpaqueWorkAreaAddr, uid);
    }

    public void read(Memory mem, int address) {
        size                    = mem.read32(address);
        name                    = Utilities.readStringNZ(mem, address + 4, 31);
        attr                    = mem.read32(address + 36);
        mutexUid                = mem.read32(address + 40);
        mutexOpaqueWorkAreaAddr = mem.read32(address + 44);
        numWaitThreads          = mem.read32(address + 48);
        locked                  = mem.read32(address + 52);
        threadid                = mem.read32(address + 56);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, attr);
        mem.write32(address + 40, mutexUid);
        mem.write32(address + 44, mutexOpaqueWorkAreaAddr);
        mem.write32(address + 48, numWaitThreads);
        mem.write32(address + 52, locked);
        mem.write32(address + 56, threadid);
    }

	@Override
	public String toString() {
		return String.format("SceKernelLwMutexInfo(uid=%x, name=%s, locked=%d, numWaitThreads=%d, attr=0x%X", uid, name, locked, numWaitThreads, attr);
	}
}