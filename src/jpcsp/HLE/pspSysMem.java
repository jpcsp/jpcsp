/*
Function:
- http://psp.jim.sh/pspsdk-doc/group__SysMem.html

Notes:
Current allocation scheme doesn't handle partitions, freeing blocks or the
space consumed by the program image.

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
package jpcsp.HLE;

import java.util.HashMap;
import java.util.Iterator;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.MemoryMap;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.managers.*;

/**
 *
 * @author shadow
 */
public class pspSysMem {
    private static pspSysMem instance;

    private static HashMap<Integer, SysMemInfo> blockList;
    private int heapTop, heapBottom;

    // PspSysMemBlockTypes
    public static final int PSP_SMEM_Low = 0;
    public static final int PSP_SMEM_High = 1;
    public static final int PSP_SMEM_Addr = 2;
    public static final int PSP_SMEM_LowAligned = 3;
    public static final int PSP_SMEM_HighAligned = 4;

    // Firmware versions
    public static final int PSP_FIRMWARE_100 = 0x01000300;
    public static final int PSP_FIRMWARE_150 = 0x01050001;
    public static final int PSP_FIRMWARE_151 = 0x01050100;
    public static final int PSP_FIRMWARE_152 = 0x01050200;
    public static final int PSP_FIRMWARE_200 = 0x02000010;
    public static final int PSP_FIRMWARE_201 = 0x02000010; // Same as 2.00
    public static final int PSP_FIRMWARE_250 = 0x02050010;
    public static final int PSP_FIRMWARE_260 = 0x02060010;
    public static final int PSP_FIRMWARE_270 = 0x02070010;
    public static final int PSP_FIRMWARE_271 = 0x02070110;

    public final static int PSP_ERROR_ILLEGAL_PARTITION_ID                 = 0x800200d6;
    public final static int PSP_ERROR_PARTITION_IN_USE                     = 0x800200d7;
    public final static int PSP_ERROR_ILLEGAL_MEMORY_BLOCK_ALLOCATION_TYPE = 0x800200d8;
    public final static int PSP_ERROR_FAILED_TO_ALLOCATE_MEMORY_BLOCK      = 0x800200d9;
    public final static int PSP_ERROR_ILLEGAL_CHUNK_ID                     = 0x800200de; // may not be for pspsysmem...


    private pspSysMem() { }

    public static pspSysMem getInstance() {
        if (instance == null) {
            instance = new pspSysMem();
        }
        return instance;
    }

    public void Initialise()
    {
        blockList = new HashMap<Integer, SysMemInfo>();

        // The loader should do the first malloc which will set the heapBottom corectly
        heapBottom = 0x08400000; //MemoryMap.START_RAM; //0x08900000;
        heapTop = MemoryMap.END_RAM;
    }

    // Allocates to 64-byte alignment
    // TODO use the partitionid
    public int malloc(int partitionid, int type, int size, int addr)
    {
        int allocatedAddress = 0;

        /* if (size > maxFreeMemSize())
        {
            // no mem left
            Modules.log.warn("malloc failed (want=" + size + ",free=" + maxFreeMemSize() + ")");
        }
        else */ if (type == PSP_SMEM_Low)
        {
            allocatedAddress = heapBottom;
            allocatedAddress = (allocatedAddress + 63) & ~63;
            heapBottom = allocatedAddress + size;

            if (heapBottom > heapTop)
            {
                Modules.log.warn("malloc overflowed (heapBottom=0x" + Integer.toHexString(heapBottom)
                    + ",heapTop=0x" + Integer.toHexString(heapTop) + ")");
            }
        }
        else if (type == PSP_SMEM_Addr)
        {
            int highDiff = heapTop - addr;
            int lowDiff = heapBottom - addr;
            if (highDiff < 0) highDiff = -highDiff;
            if (lowDiff < 0) lowDiff = -lowDiff;

            if (lowDiff <= highDiff)
            {
                // Alloc near bottom
                allocatedAddress = heapBottom;
                if (allocatedAddress < addr)
                    allocatedAddress = addr;
                allocatedAddress = (allocatedAddress + 63) & ~63;
                heapBottom = allocatedAddress + size;

                if (heapBottom > heapTop)
                {
                    Modules.log.warn("malloc overflowed (heapBottom=0x" + Integer.toHexString(heapBottom)
                        + ",heapTop=0x" + Integer.toHexString(heapTop) + ")");
                }
            }
            else
            {
                // Alloc near top
                allocatedAddress = heapTop - size;
                if (allocatedAddress > addr)
                    allocatedAddress = addr;
                allocatedAddress = (allocatedAddress + 63) & ~63;
                heapTop = allocatedAddress;

                if (heapTop < heapBottom)
                {
                    Modules.log.warn("malloc underflowed (heapBottom=0x" + Integer.toHexString(heapBottom)
                        + ",heapTop=0x" + Integer.toHexString(heapTop) + ")");
                }
            }
        }
        else if (type == PSP_SMEM_High)
        {
            allocatedAddress = heapTop - size + 1;
            allocatedAddress = allocatedAddress & ~63;
            heapTop = allocatedAddress - 1;

            if (heapTop < heapBottom)
            {
                Modules.log.warn("malloc underflowed (heapBottom=0x" + Integer.toHexString(heapBottom)
                    + ",heapTop=0x" + Integer.toHexString(heapTop) + ")");
            }
        }
        else if (type == PSP_SMEM_LowAligned)
        {
            allocatedAddress = heapBottom;
            allocatedAddress = (allocatedAddress + addr - 1) & ~(addr - 1);
            heapBottom = allocatedAddress + size;

            if (heapBottom > heapTop)
            {
                Modules.log.warn("malloc overflowed (heapBottom=0x" + Integer.toHexString(heapBottom)
                    + ",heapTop=0x" + Integer.toHexString(heapTop) + ")");
            }
        }
        else if (type == PSP_SMEM_HighAligned)
        {
            allocatedAddress = heapTop - size + 1;
            allocatedAddress = allocatedAddress & ~(addr - 1);
            heapTop = allocatedAddress - 1;

            if (heapTop < heapBottom)
            {
                Modules.log.warn("malloc underflowed (heapBottom=0x" + Integer.toHexString(heapBottom)
                    + ",heapTop=0x" + Integer.toHexString(heapTop) + ")");
            }
        }

        if (allocatedAddress != 0)
        {
            Modules.log.debug("malloc(size=0x" + Integer.toHexString(size)
                + ") new heapBottom=0x" + Integer.toHexString(heapBottom)
                + ", new heapTop=0x" + Integer.toHexString(heapTop));
        }

        return allocatedAddress;
    }

    public int addSysMemInfo(int partitionid, String name, int type, int size, int addr)
    {
        SysMemInfo info = new SysMemInfo(partitionid, name, type, size, addr);

        return info.uid;
    }

    // For internal use, example: ThreadMan allocating stack space
    // Also removes the associated SysMemInfo (if found) from blockList
    public void free(int addr)
    {
        boolean found = false;

        // Reverse lookup on blockList, get SysMemInfo and call free
        for (Iterator<SysMemInfo> it = blockList.values().iterator(); it.hasNext();)
        {
            SysMemInfo info = it.next();
            if (info.addr == addr)
            {
                found = true;
                free(info);
                it.remove();
                break;
            }
        }

        if (!found)
        {
            // HLE modules using malloc should also call addSysMemInfo
            Modules.log.warn("pspSysMem.free(addr) failed to find SysMemInfo for addr:0x" + Integer.toHexString(addr));
        }
    }

    private void free(SysMemInfo info)
    {
        // TODO
        Modules.log.error("UNIMPLEMENT: pspSysMem.free(info) not implemented");
    }

    public int maxFreeMemSize() {
        // Since some apps try and allocate the value of sceKernelMaxFreeMemSize,
        // which will leave no space for stacks we're going to reserve 0x09f00000
        // to 0x09ffffff for stacks, but stacks are allowed to go below that
        // (if there's free space of course).
        int heapTopGuard = heapTop;
        if (heapTopGuard > 0x09f00000)
            heapTopGuard = 0x09f00000;
        int maxFree = heapTopGuard - heapBottom - 64; // don't forget our alignment padding!

        // TODO Something not quite right here...
        if (maxFree < 0)
            maxFree = 0;

        return maxFree;
    }

    /** @return the size of the largest allocatable block */
    public void sceKernelMaxFreeMemSize()
    {
        int maxFree = maxFreeMemSize();
        Modules.log.debug("sceKernelMaxFreeMemSize " + maxFree
                + " (hex=" + Integer.toHexString(maxFree) + ")");
        Emulator.getProcessor().cpu.gpr[2] = maxFree;
    }

    public void sceKernelTotalFreeMemSize()
    {
        int totalFree = heapTop - heapBottom;
        Modules.log.debug("sceKernelTotalFreeMemSize " + totalFree
                + " (hex=" + Integer.toHexString(totalFree) + ")");
        Emulator.getProcessor().cpu.gpr[2] = totalFree;
    }

    /**
     * @param partitionid TODO probably user, kernel etc
     * 1 = kernel, 2 = user, 3 = me, 4 = kernel mirror (from potemkin/dash)
     * @param type If type is PSP_SMEM_Addr, then addr specifies the lowest
     * address to allocate the block from.
     */
    public void sceKernelAllocPartitionMemory(int partitionid, int pname, int type, int size, int addr)
    {
        pname &= 0x3fffffff;
        addr &= 0x3fffffff;
        String name = readStringZ(pname);

        // print debug info
        String typeStr;
        switch(type) {
            case PSP_SMEM_Low: typeStr = "PSP_SMEM_Low"; break;
            case PSP_SMEM_High: typeStr = "PSP_SMEM_High"; break;
            case PSP_SMEM_Addr: typeStr = "PSP_SMEM_Addr"; break;
            case PSP_SMEM_LowAligned: typeStr = "PSP_SMEM_LowAligned"; break;
            case PSP_SMEM_HighAligned: typeStr = "PSP_SMEM_HighAligned"; break;
            default: typeStr = "UNHANDLED " + type; break;
        }
        Modules.log.debug("sceKernelAllocPartitionMemory(partitionid=" + partitionid
                + ",name='" + name + "',type=" + typeStr + ",size=" + size
                + ",addr=0x" + Integer.toHexString(addr) + ")");

        if (type < PSP_SMEM_Low || type > PSP_SMEM_HighAligned)
        {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ILLEGAL_MEMORY_BLOCK_ALLOCATION_TYPE;
        }
        else
        {
            addr = malloc(partitionid, type, size, addr);
            if (addr != 0)
            {
                SysMemInfo info = new SysMemInfo(partitionid, name, type, size, addr);
                Emulator.getProcessor().cpu.gpr[2] = info.uid;
            }
            else
            {
                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_FAILED_TO_ALLOCATE_MEMORY_BLOCK;
            }
        }
    }

    public void sceKernelFreePartitionMemory(int uid) throws GeneralJpcspException
    {
        SceUidManager.checkUidPurpose(uid, "SysMem", true);
        SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelFreePartitionMemory unknown SceUID=" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ILLEGAL_CHUNK_ID;
        } else {
            free(info);
            Modules.log.warn("UNIMPLEMENT:sceKernelFreePartitionMemory SceUID=" + Integer.toHexString(info.uid) + " name:'" + info.name + "'");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelGetBlockHeadAddr(int uid) throws GeneralJpcspException
    {
        SceUidManager.checkUidPurpose(uid, "SysMem", true);
        SysMemInfo info = blockList.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelGetBlockHeadAddr unknown SceUID=" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ILLEGAL_CHUNK_ID;
        } else {
            Modules.log.debug("sceKernelGetBlockHeadAddr SceUID=" + Integer.toHexString(info.uid) + " name:'" + info.name + "' headAddr:" + Integer.toHexString(info.addr));
            Emulator.getProcessor().cpu.gpr[2] = info.addr;
        }
    }

    public void sceKernelDevkitVersion()
    {
        // Return 1.5 for now
        int version = PSP_FIRMWARE_150;
        Modules.log.debug("sceKernelDevkitVersion return:0x" + Integer.toHexString(version));
        Emulator.getProcessor().cpu.gpr[2] = version;
    }

    class SysMemInfo {
        public final int uid;
        public final int partitionid;
        public final String name;
        public final int type;
        public final int size;
        public final int addr;

        public SysMemInfo(int partitionid, String name, int type,
                int size, int addr) {
            this.partitionid = partitionid;
            this.name = name;
            this.type = type;
            this.size = size;
            this.addr = addr;

            uid = SceUidManager.getNewUid("SysMem");
            blockList.put(uid, this);
        }

    }
}
