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
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspSysMem;
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
    // TODO need a proper malloc implementation, for now free will fail
    public final int allocAddress;
    public int freeLowAddress;
    public int freeHighAddress;

    public static final int VPL_ATTR_MASK = 0x41FF; // anything outside this mask is an illegal attr
    public static final int VPL_ATTR_UNKNOWN = 0x100;
    public static final int VPL_ATTR_ADDR_HIGH = 0x4000; // create() the vpl in hi-mem, and start alloc() from high addresses

    private SceKernelVplInfo(String name, int partitionid, int attr, int size) {
        this.name = name;
        this.attr = attr;
        this.poolSize = size - 32; // 32 byte overhead per VPL

        freeSize = poolSize;
        numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-Vpl");
        this.partitionid = partitionid;

        int memType = pspSysMem.PSP_SMEM_Low;
        if ((attr & VPL_ATTR_ADDR_HIGH) == VPL_ATTR_ADDR_HIGH)
            memType = pspSysMem.PSP_SMEM_High;

        // Reserve psp memory
        int alignedSize = (size + 7) & ~7; // 8-byte align
        int totalVplSize = alignedSize;
        int addr = pspSysMem.getInstance().malloc(partitionid, memType, totalVplSize, 0);
        if (addr == 0)
            throw new RuntimeException("SceKernelVplInfo: not enough free mem");
        pspSysMem.getInstance().addSysMemInfo(partitionid, "ThreadMan-Vpl", memType, totalVplSize, addr);

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

    public static SceKernelVplInfo tryCreateVpl(String name, int partitionid, int attr, int size) {
        SceKernelVplInfo info = null;
        int alignedSize = (size + 7) & ~7; // 8-byte align
        int totalVplSize = alignedSize;
        int maxFreeSize = pspSysMem.getInstance().maxFreeMemSize();

        if (totalVplSize <= maxFreeSize) {
            info = new SceKernelVplInfo(name, partitionid, attr, size);
        } else {
            Modules.log.warn("tryCreateVpl not enough free mem (want=" + totalVplSize + ",free=" + maxFreeSize + ",diff=" + (totalVplSize - maxFreeSize) + ")");
        }

        return info;
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

    /** @return the address or 0 on failure */
    public int tryAllocate(int size) {
        int addr = 0;
        // TODO proper malloc implementation
        if (size + 8 <= freeSize) {

            if ((attr & VPL_ATTR_ADDR_HIGH) == VPL_ATTR_ADDR_HIGH) {
                addr = freeHighAddress - size;
                freeHighAddress -= size + 8;
            } else {
                addr = freeLowAddress + 8;
                freeLowAddress += size + 8;
            }

            // write block header
            Memory mem = Memory.getInstance();
            mem.write32(addr - 8, allocAddress);
            mem.write32(addr - 4, 0); // magic?

            freeSize -= size + 8;
        }
        return addr;
    }

    /** @return true on success */
    public boolean free(int addr) {
        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(addr - 8)) {
            // check block header
            int top = mem.read32(addr - 8);
            if (top != allocAddress) {
                Modules.log.warn("Free VPL 0x" + Integer.toHexString(addr) + " bad address");
                return false;
            } else {
                // TODO free

                //size = ...
                //freeSize += size + 8;

                Modules.log.error("UNIMPLEMENTED:Free VPL 0x" + Integer.toHexString(addr) + " not implemented");
                return false;
            }
        } else {
            // address is not in valid range
            Modules.log.warn("Free VPL 0x" + Integer.toHexString(addr) + " bad address");
            return false;
        }
    }
}
