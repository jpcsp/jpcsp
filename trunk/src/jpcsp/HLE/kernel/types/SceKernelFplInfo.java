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
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.util.Utilities;

/*
 * TODO list:
 * 1. Implement a queue to receive blocks waiting for allocation and process
 * memory events for them (onFreeFpl).
 */

public class SceKernelFplInfo {
    // PSP info
    public int size = 56;
    public String name;
    public int attr;
    public int blockSize;
    public int numBlocks;
    public int freeBlocks;
    public int numWaitThreads;

    private final int sysMemUID;
    // Internal info
    public final int uid;
    public final int partitionid;
    public int[] blockAddress;
    public boolean[] blockAllocated;

    public static final int FPL_ATTR_MASK = 0x41FF; // anything outside this mask is an illegal attr
    public static final int FPL_ATTR_UNKNOWN = 0x100;
    public static final int FPL_ATTR_ADDR_HIGH = 0x4000; // create() the fpl in hi-mem, but start alloc() from low addresses

    /** do not instantiate unless there is enough free mem.
     * use the static helper function tryCreateFpl. */
    private SceKernelFplInfo(String name, int partitionid, int attr, int blockSize, int numBlocks) {
        this.name = name;
        this.attr = attr;
        this.blockSize = blockSize;
        this.numBlocks = numBlocks;

        freeBlocks = numBlocks;
        numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-Fpl");
        this.partitionid = partitionid;
        blockAddress = new int[numBlocks];
        blockAllocated = new boolean[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            blockAllocated[i] = false;
        }

        int memType = SysMemUserForUser.PSP_SMEM_Low;
        if ((attr & FPL_ATTR_ADDR_HIGH) == FPL_ATTR_ADDR_HIGH)
            memType = SysMemUserForUser.PSP_SMEM_High;

        // Reserve psp memory
        int alignedBlockSize = (blockSize + 3) & ~3; // 32-bit align
        int totalFplSize = alignedBlockSize * numBlocks;
        int addr = Modules.SysMemUserForUserModule.malloc(partitionid, memType, totalFplSize, 0);
        if (addr == 0)
            throw new RuntimeException("SceKernelFplInfo: not enough free mem");
        sysMemUID = Modules.SysMemUserForUserModule.addSysMemInfo(partitionid, "ThreadMan-Fpl", memType, totalFplSize, addr);

        // Initialise the block addresses
        for (int i = 0; i < numBlocks; i++) {
            blockAddress[i] = addr + alignedBlockSize * i;
        }
    }

    public static SceKernelFplInfo tryCreateFpl(String name, int partitionid, int attr, int blockSize, int numBlocks) {
        SceKernelFplInfo info = null;
        int alignedBlockSize = (blockSize + 3) & ~3; // 32-bit align
        int totalFplSize = alignedBlockSize * numBlocks;
        int maxFreeSize = Modules.SysMemUserForUserModule.maxFreeMemSize();

        if (totalFplSize <= maxFreeSize) {
            info = new SceKernelFplInfo(name, partitionid, attr, blockSize, numBlocks);
        } else {
            Modules.log.warn("tryCreateFpl not enough free mem (want=" + totalFplSize + ",free=" + maxFreeSize + ",diff=" + (totalFplSize - maxFreeSize) + ")");
        }

        return info;
    }

    public void read(Memory mem, int address) {
        address &= 0x3FFFFFFF;
        size 	        = mem.read32(address);
        name            = Utilities.readStringNZ(address + 4, 31);
        attr            = mem.read32(address + 36);
        blockSize       = mem.read32(address + 40);
        numBlocks       = mem.read32(address + 44);
        freeBlocks      = mem.read32(address + 48);
        numWaitThreads  = mem.read32(address + 52);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);

        int i;
        for (i = 0; i < 32 && i < name.length(); i++)
            mem.write8(address + 4 + i, (byte)name.charAt(i));
        for (; i < 32; i++)
            mem.write8(address + 4 + i, (byte)0);

        mem.write32(address + 36, attr);
        mem.write32(address + 40, blockSize);
        mem.write32(address + 44, numBlocks);
        mem.write32(address + 48, freeBlocks);
        mem.write32(address + 52, numWaitThreads);
    }

    public boolean isBlockAllocated(int blockId) {
        return blockAllocated[blockId];
    }

    public void freeBlock(int blockId) {
        if (!isBlockAllocated(blockId))
            throw new IllegalArgumentException("Block " + blockId + " is not allocated");

        blockAllocated[blockId] = false;
        freeBlocks++;
    }

    /** @return the address of the allocated block */
    public int allocateBlock(int blockId) {
        if (isBlockAllocated(blockId))
            throw new IllegalArgumentException("Block " + blockId + " is already allocated");

        blockAllocated[blockId] = true;
        freeBlocks--;

        return blockAddress[blockId];
    }

    /** @return the block index or -1 on failure */
    public int findFreeBlock() {
        for (int i = 0; i < numBlocks; i++)
            if (!isBlockAllocated(i))
                return i;
        return -1;
    }

    /** @return the block index or -1 on failure */
    public int findBlockByAddress(int addr) {
        for (int i = 0; i < numBlocks; i++)
            if (blockAddress[i] == addr)
                return i;
        return -1;
    }

    public void deleteSysMemInfo() {
    	Modules.SysMemUserForUserModule.free(sysMemUID, blockAddress[0]);
    }
}