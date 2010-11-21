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

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.VplManager;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.util.Utilities;

public class SceKernelVplInfo {
    // PSP info
    public int size = 52;
    public String name;
    public int attr;
    public int poolSize;
    public int freeSize;
    public int numWaitThreads;

    private final SysMemInfo sysMemInfo;
    // Internal info
    public final int uid;
    public final int partitionid;
    public final int allocAddress;
    public int freeLowAddress;
    public int freeHighAddress;
    public HashMap<Integer, Integer> dataBlockMap;  //Hash map to store each data address and respective size.

    private SceKernelVplInfo(String name, int partitionid, int attr, int size, int memType) {
        this.name = name;
        this.attr = attr;
        poolSize = size - 32; // 32 byte overhead per VPL

        freeSize = poolSize;
        numWaitThreads = 0;

        dataBlockMap = new HashMap<Integer, Integer>();

        uid = SceUidManager.getNewUid("ThreadMan-Vpl");
        this.partitionid = partitionid;

        // Reserve psp memory
        int alignedSize = (size + 7) & ~7; // 8-byte align
        int totalVplSize = alignedSize;
        sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionid, "ThreadMan-Vpl", memType, totalVplSize, 0);
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

        // 32 byte overhead per VPL
        allocAddress = addr;
        freeLowAddress = addr + 32;
        freeHighAddress = addr + totalVplSize;
    }

    public static SceKernelVplInfo tryCreateVpl(String name, int partitionid, int attr, int size, int memType) {
        SceKernelVplInfo info = null;
        int alignedSize = (size + 7) & ~7; // 8-byte align
        int totalVplSize = alignedSize;
        int maxFreeSize = Modules.SysMemUserForUserModule.maxFreeMemSize();

        if (totalVplSize <= maxFreeSize) {
            info = new SceKernelVplInfo(name, partitionid, attr, totalVplSize, memType);
        } else {
            Modules.log.warn("tryCreateVpl not enough free mem (want=" + totalVplSize + ",free=" + maxFreeSize + ",diff=" + (totalVplSize - maxFreeSize) + ")");
        }

        return info;
    }

    public void deleteSysMemInfo() {
        Modules.SysMemUserForUserModule.free(sysMemInfo);
    }

    public void read(Memory mem, int address) {
        size  	        = mem.read32(address);
        name            = Utilities.readStringNZ(mem, address + 4, 32);
        attr            = mem.read32(address + 36);
        poolSize        = mem.read32(address + 40);
        freeSize        = mem.read32(address + 44);
        numWaitThreads  = mem.read32(address + 48);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, attr);
        mem.write32(address + 40, poolSize);
        mem.write32(address + 44, freeSize);
        mem.write32(address + 48, numWaitThreads);
    }

    /** @return true on success */
    public boolean free(int addr) {
        Memory mem = Memory.getInstance();
        if (Memory.isAddressGood(addr - 8)) {
            // Check block header.
            int top = mem.read32(addr - 8);
            if (top != allocAddress) {
                Modules.getLogger("ThreadManForUser").warn("Free VPL 0x" + Integer.toHexString(addr) + " bad address");
                return false;
            }
            // Recover free size from deallocated block.
            int deallocSize = (dataBlockMap.get(addr) + 8);
            if ((attr & VplManager.PSP_VPL_ATTR_ADDR_HIGH) == VplManager.PSP_VPL_ATTR_ADDR_HIGH) {
                freeHighAddress += deallocSize;
            } else {
                freeLowAddress += deallocSize;
            }
            freeSize += deallocSize;
            dataBlockMap.remove(addr);
            Modules.getLogger("ThreadManForUser").debug("Free VPL: Block 0x" + Integer.toHexString(addr)
                    + " with size=" + deallocSize + " freed");
            return true;
        }
        // Address is not in valid range.
        Modules.getLogger("ThreadManForUser").warn("Free VPL 0x" + Integer.toHexString(addr)
                    + " bad address");
        return false;
    }
}