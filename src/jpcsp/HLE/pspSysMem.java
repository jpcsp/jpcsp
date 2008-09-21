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
import jpcsp.Memory;
import jpcsp.MemoryMap;
import static jpcsp.util.Utilities.*;

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

    private pspSysMem() { }

    public static pspSysMem get_instance() {
        if (instance == null) {
            instance = new pspSysMem();
        }
        return instance;
    }

    public void Initialise(int programStartAddr, int programSize)
    {
        blockList = new HashMap<Integer, SysMemInfo>();

        System.out.println("pspSysMem reserving " + programSize + " bytes at "
                + String.format("%08x", programStartAddr) + " for app");

        heapBottom = programStartAddr + programSize;
        heapTop = MemoryMap.END_RAM;
    }

    // Allocates to 64-byte alignment
    // TODO use the partitionid
    public int malloc(int partitionid, int type, int size, int addr)
    {
        int allocatedAddress = 0;

        // TODO check when we are running out of mem!
        if (type == PSP_SMEM_Low)
        {
            allocatedAddress = heapBottom;
            allocatedAddress = (allocatedAddress + 63) & ~63;
            heapBottom = allocatedAddress + size;
        }
        else if (type == PSP_SMEM_Addr)
        {
            allocatedAddress = heapBottom;
            if (allocatedAddress < addr)
                allocatedAddress = addr;
            allocatedAddress = (allocatedAddress + 63) & ~63;
            heapBottom = allocatedAddress + size;
        }
        else if (type == PSP_SMEM_High)
        {
            allocatedAddress = (heapTop - (size + 63)) & ~63;
            heapTop = allocatedAddress;
        }
        else if (type == PSP_SMEM_LowAligned)
        {
            allocatedAddress = heapBottom;
            allocatedAddress = (allocatedAddress + addr - 1) & ~(addr - 1);
            heapBottom = allocatedAddress + size;
        }
        else if (type == PSP_SMEM_HighAligned)
        {
            allocatedAddress = (heapTop - (size + addr - 1)) & ~(addr - 1);
            heapTop = allocatedAddress;
        }

        return allocatedAddress;
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
            System.out.println("ERROR failed to map addr to SysMemInfo, possibly bad/missing cleanup or double free in HLE");
        }
    }

    private void free(SysMemInfo info)
    {
        // TODO
    }

    public void sceKernelMaxFreeMemSize()
    {
        int maxFree = heapTop - heapBottom;
        System.out.println("sceKernelMaxFreeMemSize " + maxFree
                + " (hex=" + Integer.toHexString(maxFree) + ")");
        Emulator.getProcessor().gpr[2] = maxFree;
    }

    public void sceKernelTotalFreeMemSize()
    {
        int totalFree = heapTop - heapBottom;
        System.out.println("sceKernelTotalFreeMemSize " + totalFree
                + " (hex=" + Integer.toHexString(totalFree) + ")");
        Emulator.getProcessor().gpr[2] = totalFree;
    }

    /**
     * @param partitionid TODO probably user, kernel etc
     * 0 = ?
     * 1 = kernel?
     * 2 = user?
     * @param type If type is PSP_SMEM_Addr, then addr specifies the lowest
     * address allocate the block from.
     */
    public void sceKernelAllocPartitionMemory(int partitionid, int pname, int type, int size, int addr)
    {
        pname &= 0x3fffffff;
        addr &= 0x3fffffff;
        String name = readStringZ(Memory.getInstance().mainmemory, pname - MemoryMap.START_RAM);

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
        System.out.println("sceKernelAllocPartitionMemory(partitionid=" + partitionid
                + ",name='" + name + "',type=" + typeStr + ",size=" + size
                + ",addr=0x" + Integer.toHexString(addr) + ")");

        addr = malloc(partitionid, type, size, addr);
        if (addr != 0)
        {
            SysMemInfo info = new SysMemInfo(partitionid, name, type, size, addr);
            Emulator.getProcessor().gpr[2] = info.uid;
        }
        else
        {
            Emulator.getProcessor().gpr[2] = -1;
        }
    }

    public void sceKernelFreePartitionMemory(int uid) throws GeneralJpcspException
    {
        SceUIDMan.get_instance().checkUidPurpose(uid, "SysMem", true);
        SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            System.out.println("sceKernelFreePartitionMemory unknown SceUID=" + Integer.toHexString(uid));
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            free(info);
            System.out.println("UNIMPLEMENT:sceKernelFreePartitionMemory SceUID=" + Integer.toHexString(info.uid) + " name:'" + info.name + "'");
            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    public void sceKernelGetBlockHeadAddr(int uid) throws GeneralJpcspException
    {
        SceUIDMan.get_instance().checkUidPurpose(uid, "SysMem", true);
        SysMemInfo info = blockList.get(uid);
        if (info == null) {
            System.out.println("sceKernelGetBlockHeadAddr unknown SceUID=" + Integer.toHexString(uid));
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            System.out.println("sceKernelGetBlockHeadAddr SceUID=" + Integer.toHexString(info.uid) + " name:'" + info.name + "' headAddr:" + Integer.toHexString(info.addr));
            Emulator.getProcessor().gpr[2] = info.addr;
        }
    }

    public void sceKernelDevkitVersion()
    {
        // Return 1.5 for now
        int version = PSP_FIRMWARE_150;
        System.out.println("sceKernelDevkitVersion 0x" + Integer.toHexString(version));
        Emulator.getProcessor().gpr[2] = version;
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

            uid = SceUIDMan.get_instance().getNewUid("SysMem");
            blockList.put(uid, this);
        }

    }
}
