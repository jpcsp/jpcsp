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
package jpcsp.HLE.modules500;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;

import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

import org.apache.log4j.Logger;

public class sceHeap extends HLEModule {
    protected static Logger log = Modules.getLogger("sceHeap");

    @Override
    public String getName() {
        return "sceHeap";
    }

    protected final static int PSP_HEAP_ATTR_ADDR_HIGH = 0x4000;       // Create the heap in high memory.
    protected final static int PSP_HEAP_ATTR_EXT =       0x8000;       // Automatically extend the heap's memory.
    private HashMap<Integer, SysMemInfo> heapMap = new HashMap<Integer, SysMemInfo>();
    private HashMap<Integer, SysMemInfo> heapMemMap = new HashMap<Integer, SysMemInfo>();

    @HLEFunction(nid = 0x0E875980, version = 500, checkInsideInterrupt = true)
    public int sceHeapReallocHeapMemory(TPointer heapAddr, TPointer memAddr, int memSize) {
        log.warn(String.format("Unimplemented sceHeapReallocHeapMemory heapAddr=%s, memAddr=%s, memSize=0x%X", heapAddr, memAddr, memSize));

        return 0;
    }

    @HLEFunction(nid = 0x1C84B58D, version = 500, checkInsideInterrupt = true)
    public int sceHeapReallocHeapMemoryWithOption(TPointer heapAddr, TPointer memAddr, int memSize, TPointer paramAddr) {
        log.warn(String.format("Unimplemented sceHeapReallocHeapMemoryWithOption heapAddr=%s, memAddr=%s, memSize=0x%X, paramAddr=%s", heapAddr, memAddr, memSize, paramAddr));

        return 0;
    }

    @HLEFunction(nid = 0x2ABADC63, version = 500, checkInsideInterrupt = true)
    public int sceHeapFreeHeapMemory(TPointer heapAddr, TPointer memAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceHeapFreeHeapMemory heapAddr=%s, memAddr=%s", heapAddr, memAddr));
        }

        // Try to free memory back to the heap.
        if (heapMemMap.containsKey(memAddr.getAddress())) {
            Modules.SysMemUserForUserModule.free(heapMemMap.get(memAddr.getAddress()));
            return 0;
        } else if (heapMap.containsKey(heapAddr.getAddress())) {
            return SceKernelErrors.ERROR_INVALID_ID;
        } else {
            return SceKernelErrors.ERROR_INVALID_POINTER;
        }
    }

    @HLEFunction(nid = 0x2A0C2009, version = 500, checkInsideInterrupt = true)
    public int sceHeapGetMallinfo(TPointer heapAddr, TPointer infoAddr) {
        log.warn(String.format("Unimplemented sceHeapGetMallinfo heapAddr=%s, infoAddr=%s", heapAddr, infoAddr));

        return 0;
    }

    @HLEFunction(nid = 0x2B7299D8, version = 500, checkInsideInterrupt = true)
    public int sceHeapAllocHeapMemoryWithOption(TPointer heapAddr, int memSize, @CanBeNull TPointer32 paramAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceHeapAllocHeapMemoryWithOption heapAddr=%s, memSize=0x%X, paramAddr=%s", heapAddr, memSize, paramAddr));
        }

        if (paramAddr.isNotNull()) {
            int paramSize = paramAddr.getValue(0);
            if (paramSize == 8) {
                int memAlign = paramAddr.getValue(4);
                if (log.isDebugEnabled()) {
                	log.debug(String.format("sceHeapAllocHeapMemoryWithOption options: struct size=%d, alignment=0x%X", paramSize, memAlign));
                }
            } else {
                log.warn(String.format("sceHeapAllocHeapMemoryWithOption option at %s(size=%d)", paramAddr, paramSize));
            }
        }

        // Try to allocate memory from the heap and return it's address.
        SysMemInfo heapInfo = null;
        SysMemInfo heapMemInfo = null;
        if (heapMap.containsKey(heapAddr.getAddress())) {
            heapInfo = heapMap.get(heapAddr.getAddress());
            heapMemInfo = Modules.SysMemUserForUserModule.malloc(heapInfo.partitionid, "ThreadMan-HeapMem", heapInfo.type, memSize, 0);
        }
        if (heapMemInfo == null) {
        	return 0;
        }

        heapMemMap.put(heapMemInfo.addr, heapMemInfo);

        return heapMemInfo.addr;
    }

    @HLEFunction(nid = 0x4929B40D, version = 500, checkInsideInterrupt = true)
    public int sceHeapGetTotalFreeSize(TPointer heapAddr) {
        log.warn(String.format("Unimplemented sceHeapGetTotalFreeSize heapAddr=%s", heapAddr));

        return 0;
    }

    @HLEFunction(nid = 0x7012BBDD, version = 500, checkInsideInterrupt = true)
    public int sceHeapIsAllocatedHeapMemory(TPointer heapAddr, TPointer memAddr) {
        log.warn(String.format("Unimplemented sceHeapIsAllocatedHeapMemory heapAddr=%s, memAddr=%s", heapAddr, memAddr));

        return 0;
    }

    @HLEFunction(nid = 0x70210B73, version = 500, checkInsideInterrupt = true)
    public int sceHeapDeleteHeap(TPointer heapAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceHeapDeleteHeap heapAddr=%s", heapAddr));
        }

        if (!heapMap.containsKey(heapAddr.getAddress())) {
        	return SceKernelErrors.ERROR_INVALID_ID;
        }

        Modules.SysMemUserForUserModule.free(heapMap.get(heapAddr.getAddress()));

        return 0;
    }

    @HLEFunction(nid = 0x7DE281C2, version = 500, checkInsideInterrupt = true)
    public int sceHeapCreateHeap(PspString name, int heapSize, int attr, @CanBeNull TPointer paramAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceHeapCreateHeap name=%s, heapSize=0x%X, attr=0x%X, paramAddr=%s", name, heapSize, attr, paramAddr));
        }

        if (paramAddr .isNotNull()) {
            log.warn(String.format("sceHeapCreateHeap unknown option at %s", paramAddr));
        }

        int memType = PSP_SMEM_Low;
        if ((attr & PSP_HEAP_ATTR_ADDR_HIGH) == PSP_HEAP_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }

        // Allocate a virtual heap memory space and return it's address.
        SysMemInfo info = null;
        int alignment = 4;
        int totalHeapSize = (heapSize + (alignment - 1)) & (~(alignment - 1));
        int maxFreeSize = Modules.SysMemUserForUserModule.maxFreeMemSize();
        if (totalHeapSize <= maxFreeSize) {
            info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, name.getString(), memType, totalHeapSize, 0);
        } else {
            log.warn(String.format("sceHeapCreateHeap not enough free mem (want=%d, free=%d, diff=%d", totalHeapSize, maxFreeSize, totalHeapSize - maxFreeSize));
        }
        if (info == null) {
        	return 0; // Returns NULL on error.
        }

        heapMap.put(info.addr, info);

        return info.addr;
    }

    @HLEFunction(nid = 0xA8E102A0, version = 500, checkInsideInterrupt = true)
    public int sceHeapAllocHeapMemory(TPointer heapAddr, int memSize) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceHeapAllocHeapMemoryWithOption heapAddr=%s, memSize=0x%X", heapAddr, memSize));
        }

        // Try to allocate memory from the heap and return it's address.
        SysMemInfo heapInfo = null;
        SysMemInfo heapMemInfo = null;
        if (heapMap.containsKey(heapAddr.getAddress())) {
            heapInfo = heapMap.get(heapAddr.getAddress());
            heapMemInfo = Modules.SysMemUserForUserModule.malloc(heapInfo.partitionid, "ThreadMan-HeapMem", heapInfo.type, memSize, 0);
        }
        if (heapMemInfo == null) {
        	return 0;
        }

        heapMemMap.put(heapMemInfo.addr, heapMemInfo);

        return heapMemInfo.addr;
    }
}