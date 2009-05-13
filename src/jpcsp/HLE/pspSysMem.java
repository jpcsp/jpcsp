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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.MemoryMap;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.managers.*;

public class pspSysMem {
    private static pspSysMem instance;

    private static HashMap<Integer, SysMemInfo> blockList;
    private static List<SysMemInfo> freeBlockList;
    private int heapTop, heapBottom;
    private int firmwareVersion = PSP_FIRMWARE_150;

    private boolean disableReservedThreadMemory = false;

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

    /** @param firmwareVersion : in this format: ABB, where A = major and B = minor, for example 271 */
    public void Initialise(int firmwareVersion)
    {
        blockList = new HashMap<Integer, SysMemInfo>();
        freeBlockList = new LinkedList<SysMemInfo>();

        // The loader should do the first malloc which will set the heapBottom corectly
        heapBottom = 0x08400000; //MemoryMap.START_RAM; //0x08900000;
        heapTop = MemoryMap.END_RAM;

        setFirmwareVersion(firmwareVersion);
    }

    public void setDisableReservedThreadMemory(boolean disableReservedThreadMemory) {
        this.disableReservedThreadMemory = disableReservedThreadMemory;
        Modules.log.info("Reserving thread memory: " + !disableReservedThreadMemory);
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

    private void cleanupFreeBlockList() {
    	boolean changed;
    	do {
    		changed = false;
	    	for (ListIterator<SysMemInfo> lit = freeBlockList.listIterator(); lit.hasNext(); ) {
	    		SysMemInfo info = lit.next();
	    		if (info.addr == heapTop + 1) {
	    			heapTop += info.size;
	    			lit.remove();
	    			changed = true;
	    		} else if (info.addr + info.size == heapBottom) {
	    			heapBottom -= info.size;
	    			lit.remove();
	    			changed = true;
	    		}
	    	}
    	} while (changed);
    }

    private void free(SysMemInfo info)
    {
    	freeBlockList.add(info);
    	cleanupFreeBlockList();
    	if (freeBlockList.isEmpty()) {
    		Modules.log.info("pspSysMem.free(info) successful (all blocks released)");
    	} else if (!freeBlockList.contains(info)) {
    		Modules.log.info("PARTIAL: pspSysMem.free(info) successful (" + freeBlockList.size() + " block(s) still pending)");
    	} else {
    		Modules.log.warn("PARTIAL: pspSysMem.free(info) partially implemented " + info);
    	}
    }

    public int maxFreeMemSize() {
        // Since some apps try and allocate the value of sceKernelMaxFreeMemSize,
        // which will leave no space for stacks we're going to reserve 0x09f00000
        // to 0x09ffffff for stacks, but stacks are allowed to go below that
        // (if there's free space of course).
        int heapTopGuard = heapTop;
        if (!disableReservedThreadMemory) {
            if (heapTopGuard > 0x09f00000)
                heapTopGuard = 0x09f00000;
        }
        // include 64 byte alignment padding used in allocations
        // round down to 4 byte alignment (shouldn't need this if the 64 byte thing was working properly, TODO investigate)
        int maxFree = (heapTopGuard - heapBottom - 64) & ~3;

        // Negative is ok if we are reserving thread memory, clamp back to 0
        if (maxFree < 0) {
            if (disableReservedThreadMemory) {
                Modules.log.warn("pspSysMem maxFree < 0 (" + maxFree + ") maybe overflow");
            }
            maxFree = 0;
        }

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
     * http://forums.ps2dev.org/viewtopic.php?p=75341#75341
     * 8 = slim, topaddr = 0x8A000000, size = 0x1C00000 (28 MB), attr = 0x0C
     * 8 = slim, topaddr = 0x8BC00000, size = 0x400000 (4 MB), attr = 0x0C
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
            Modules.log.warn("PARTIAL:sceKernelFreePartitionMemory SceUID=" + Integer.toHexString(info.uid) + " name:'" + info.name + "'");
            free(info);
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

    /** TODO implement format string parsing and reading variable number of parameters */
    public void sceKernelPrintf(int string_addr)
    {
        String msg = readStringNZ(string_addr, 256);
        Modules.log.info("sceKernelPrintf(string_addr=0x" + Integer.toHexString(string_addr)
            + ") '" + msg + "'");
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    /** @param firmwareVersion : in this format: ABB, where A = major and B = minor, for example 271 */
    public void setFirmwareVersion(int firmwareVersion)
    {
        int major = firmwareVersion / 100;
        int minor = (firmwareVersion / 10) % 10;
        int revision = firmwareVersion % 10;
        this.firmwareVersion = (major << 24) | (minor << 16) | (revision << 8) | 0x10;
        //Modules.log.debug(String.format("pspSysMem firmware version 0x%08X", this.firmwareVersion));
    }

    public void sceKernelDevkitVersion()
    {
        Modules.log.debug("sceKernelDevkitVersion return:0x" + Integer.toHexString(firmwareVersion));
        Emulator.getProcessor().cpu.gpr[2] = firmwareVersion;
    }

    /** 3.52+ */
    public void sceKernelGetModel()
    {
        int result = 0; // <= 0 original, 1 slim

        if (firmwareVersion < 0x03050210) {
            Modules.log.debug("sceKernelGetModel called with fw less than 3.52 loaded");
        } else {
            Modules.log.debug("sceKernelGetModel ret:" + result);
        }

        Emulator.getProcessor().cpu.gpr[2] = result;
    }

    // note: we're only looking at user memory, so 0x08800000 - 0x0A000000
    // this is mainly to make it fit on one console line
    public void dumpSysMemInfo() {
        final int MEMORY_SIZE = 0x1800000;
        final int SLOT_COUNT = 64; // 0x60000
        final int SLOT_SIZE = MEMORY_SIZE / SLOT_COUNT; // 0x60000
        boolean[] allocated = new boolean[SLOT_COUNT];
        boolean[] fragmented = new boolean[SLOT_COUNT];
        int allocatedSize = 0;
        int fragmentedSize = 0;

        for (Iterator<SysMemInfo> it = blockList.values().iterator(); it.hasNext();)
        {
            SysMemInfo info = it.next();
            for (int i = info.addr; i < info.addr + info.size; i += SLOT_SIZE) {
                if (i >= 0x08800000 && i < 0x0A000000) {
                    allocated[(i - 0x08800000) / SLOT_SIZE] = true;
                }
            }
            allocatedSize += info.size;
        }

        for (Iterator<SysMemInfo> it = freeBlockList.iterator(); it.hasNext();)
        {
            SysMemInfo info = it.next();
            for (int i = info.addr; i < info.addr + info.size; i += SLOT_SIZE) {
                if (i >= 0x08800000 && i < 0x0A000000) {
                    fragmented[(i - 0x08800000) / SLOT_SIZE] = true;
                }
            }
            fragmentedSize += info.size;
        }

        StringBuffer allocatedDiagram = new StringBuffer();
        allocatedDiagram.append("[");
        for (int i = 0; i < SLOT_COUNT; i++) {
            allocatedDiagram.append(allocated[i] ? "X" : " ");
        }
        allocatedDiagram.append("]");

        StringBuffer fragmentedDiagram = new StringBuffer();
        fragmentedDiagram.append("[");
        for (int i = 0; i < SLOT_COUNT; i++) {
            fragmentedDiagram.append(fragmented[i] ? "X" : " ");
        }
        fragmentedDiagram.append("]");

        System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.err.println(String.format("Allocated memory:  %08X %d bytes", allocatedSize, allocatedSize));
        System.err.println(allocatedDiagram);
        System.err.println(String.format("Fragmented memory: %08X %d bytes", fragmentedSize, fragmentedSize));
        System.err.println(fragmentedDiagram);
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

        @Override
        public String toString(){
            return "SysMemInfo{ uid="+Integer.toHexString(uid)+";partitionid="+partitionid+";name="+name+";type="+type+";size="+size+";addr="+Integer.toHexString(addr)+" }";
        }
    }
}
