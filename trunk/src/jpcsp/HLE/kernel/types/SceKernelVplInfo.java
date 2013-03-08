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

import static jpcsp.HLE.kernel.managers.VplManager.PSP_VPL_ATTR_ADDR_HIGH;

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.ThreadWaitingList;
import jpcsp.HLE.kernel.managers.VplManager;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.util.Utilities;

public class SceKernelVplInfo extends pspAbstractMemoryMappedStructureVariableLength {
    // PSP info
    public final String name;
    public final int attr;
    public final int poolSize;
    public int freeSize;
    public final ThreadWaitingList threadWaitingList;

    public static final int vplHeaderSize = 32;
    public static final int vplBlockHeaderSize = 8;
    public static final int vplAddrAlignment = 7;

    private final SysMemInfo sysMemInfo;
    // Internal info
    public final int uid;
    public final int partitionid;
    private final int allocAddress;
    private HashMap<Integer, Integer> dataBlockMap;  //Hash map to store each data address and respective size.
    private MemoryChunkList freeMemoryChunks;

    private SceKernelVplInfo(String name, int partitionid, int attr, int size, int memType) {
        this.name = name;
        this.attr = attr;
        poolSize = size - vplHeaderSize; // 32 bytes overhead per VPL

        freeSize = poolSize;

        dataBlockMap = new HashMap<Integer, Integer>();

        uid = SceUidManager.getNewUid("ThreadMan-Vpl");
        threadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_VPL, uid, attr, VplManager.PSP_VPL_ATTR_PRIORITY);
        this.partitionid = partitionid;

        // Reserve psp memory
        int totalVplSize = Utilities.alignUp(size, vplAddrAlignment); // 8-byte align
        sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionid, String.format("ThreadMan-Vpl-0x%x-%s", uid, name), memType, totalVplSize, 0);
        if (sysMemInfo == null)
            throw new RuntimeException("SceKernelVplInfo: not enough free mem");
        int addr = sysMemInfo.addr;

        // 24 byte header, probably not necessary to mimick this
        Memory mem = Memory.getInstance();
        mem.write32(addr, addr - 1);
        mem.write32(addr + 4, size - 8);
        mem.write32(addr + 8, 0); // based on number of allocations
        mem.write32(addr + 12, addr + size - 16);
        mem.write32(addr + 16, 0); // based on allocations/fragmentation
        mem.write32(addr + 20, 0); // based on created size? magic?

        allocAddress = addr;

        MemoryChunk initialMemoryChunk = new MemoryChunk(addr + vplHeaderSize, totalVplSize - vplHeaderSize);
        freeMemoryChunks = new MemoryChunkList(initialMemoryChunk);
    }

    public static SceKernelVplInfo tryCreateVpl(String name, int partitionid, int attr, int size, int memType) {
        SceKernelVplInfo info = null;
        int totalVplSize = Utilities.alignUp(size, vplAddrAlignment); // 8-byte align
        int maxFreeSize = Modules.SysMemUserForUserModule.maxFreeMemSize();

        if (totalVplSize <= maxFreeSize) {
            info = new SceKernelVplInfo(name, partitionid, attr, totalVplSize, memType);
        } else {
            VplManager.log.warn(String.format("tryCreateVpl not enough free mem (want=%d ,free=%d, diff=%d)", totalVplSize, maxFreeSize, totalVplSize - maxFreeSize));
        }

        return info;
    }

    public void delete() {
        Modules.SysMemUserForUserModule.free(sysMemInfo);
    }

	@Override
	protected void write() {
		super.write();
		writeStringNZ(32, name);
		write32(attr);
		write32(poolSize);
		write32(freeSize);
		write32(getNumWaitingThreads());
	}

    /** @return true on success */
    public boolean free(int addr) {
        if (!dataBlockMap.containsKey(addr)) {
            // Address is not in valid range.
        	if (VplManager.log.isDebugEnabled()) {
        		VplManager.log.debug(String.format("Free VPL 0x%08X address not allocated", addr));
        	}

            return false;
        }

        // Check block header.
        Memory mem = Memory.getInstance();
        int top = mem.read32(addr - vplBlockHeaderSize);
        if (top != allocAddress) {
            VplManager.log.warn(String.format("Free VPL 0x%08X corrupted header", addr));
            return false;
        }

        // Recover free size from deallocated block.
        int deallocSize = dataBlockMap.remove(addr);

        // Free the allocated block
        freeSize += deallocSize;
        MemoryChunk memoryChunk = new MemoryChunk(addr - vplBlockHeaderSize, deallocSize);
        freeMemoryChunks.add(memoryChunk);

        if (VplManager.log.isDebugEnabled()) {
        	VplManager.log.debug(String.format("Free VPL: Block 0x%08X with size=%d freed", addr, deallocSize));
        }

        return true;
    }

    public int alloc(int size) {
    	int addr = 0;
        int allocSize = Utilities.alignUp(size, vplAddrAlignment) + vplBlockHeaderSize;

        if (allocSize <= freeSize) {
            if ((attr & PSP_VPL_ATTR_ADDR_HIGH) == PSP_VPL_ATTR_ADDR_HIGH) {
            	addr = freeMemoryChunks.allocHigh(allocSize, vplAddrAlignment);
            } else {
            	addr = freeMemoryChunks.allocLow(allocSize, vplAddrAlignment);
            }
            if (addr != 0) {
            	// 8-byte header per data block.
            	Memory mem = Memory.getInstance();
            	mem.write32(addr, allocAddress);
            	mem.write32(addr + 4, 0);
            	addr += vplBlockHeaderSize;

            	freeSize -= allocSize;

            	dataBlockMap.put(addr, allocSize);
            }
        }

        return addr;
    }

    public int getNumWaitingThreads() {
		return threadWaitingList.getNumWaitingThreads();
	}

	@Override
	public String toString() {
		return String.format("SceKernelVplInfo[uid=0x%X, name='%s', attr=0x%X]", uid, name, attr);
	}
}