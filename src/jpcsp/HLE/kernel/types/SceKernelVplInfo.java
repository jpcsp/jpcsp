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

public class SceKernelVplInfo {

    // PSP info
    public static final int size = 52;
    public String name;
    public int attr;
    public int poolSize;
    public int freeSize;
    public int numWaitThreads;

    // Internal info
    public final int uid;
    public final int partitionid;
    public int[] blockAddress;
    public int[] blockSize;
    public static final int numBlocks = 16; // TODO find a good size or use a list
    public int freeBlocks;

    public SceKernelVplInfo(String name, int partitionid, int attr, int poolSize) {
        this.name = name;
        this.attr = attr;
        this.poolSize = poolSize;

        freeSize = poolSize;
        freeBlocks = numBlocks;
        numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-Vpl");
        this.partitionid = partitionid;
        blockAddress = new int[numBlocks];
        blockSize = new int[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            blockAddress[i] = 0;
            blockSize[i] = 0;
        }
    }

    public void read(Memory mem, int address) {
        address &= 0x3FFFFFFF;
        int size        = mem.read32(address);
        name            = Utilities.readStringNZ(mem, address + 4, 31);
        attr            = mem.read32(address + 36);
        poolSize        = mem.read32(address + 40);
        freeSize        = mem.read32(address + 44);
        numWaitThreads  = mem.read32(address + 48);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);

        int i;
        for (i = 0; i < 32 && i < name.length(); i++)
            mem.write8(address + 4 + i, (byte)name.charAt(i));
        for (; i < 32; i++)
            mem.write8(address + 4 + i, (byte)0);

        mem.write32(address + 36, attr);
        mem.write32(address + 40, poolSize);
        mem.write32(address + 44, freeSize);
        mem.write32(address + 48, numWaitThreads);
    }

    /** @return the block index or -1 on failure */
    public int findFreeBlock() {
        return findBlockByAddress(0);
    }

    /** @return the block index or -1 on failure */
    public int findBlockByAddress(int addr) {
        for (int i = 0; i < numBlocks; i++)
            if (blockAddress[i] == addr)
                return i;
        return -1;
    }
}
