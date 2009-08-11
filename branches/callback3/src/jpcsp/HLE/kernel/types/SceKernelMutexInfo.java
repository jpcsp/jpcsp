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

import java.util.LinkedList;
import java.util.List;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.util.Utilities;

// this is all guessed
public class SceKernelMutexInfo {

    // PSP info
    public static final int size = 52;
    public String name;
    public int attr;
    public int numWaitThreads;
    public int locked;
    public int threadid;

    // Internal info
    public final int uid;

    public SceKernelMutexInfo(String name, int attr) {
        this.name = name;
        this.attr = attr;

        numWaitThreads = 0;
        locked = 0;

        uid = SceUidManager.getNewUid("ThreadMan-Mutex");
    }

    public void read(Memory mem, int address) {
        int size        = mem.read32(address);
        name            = Utilities.readStringNZ(mem, address + 4, 31);
        attr            = mem.read32(address + 36);
        numWaitThreads  = mem.read32(address + 40);
        locked          = mem.read32(address + 44);
        threadid        = mem.read32(address + 48);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, attr);
        mem.write32(address + 40, numWaitThreads);
        mem.write32(address + 44, locked);
        mem.write32(address + 48, threadid);
    }
}
