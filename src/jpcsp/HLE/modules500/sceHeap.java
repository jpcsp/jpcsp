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
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;

import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;
import jpcsp.HLE.kernel.types.MemoryChunk;
import jpcsp.HLE.kernel.types.MemoryChunkList;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

import org.apache.log4j.Logger;

@HLELogging
public class sceHeap extends HLEModule {
    public static Logger log = Modules.getLogger("sceHeap");

    @Override
    public String getName() {
        return "sceHeap";
    }

    protected final static int PSP_HEAP_ATTR_ADDR_HIGH = 0x4000;       // Create the heap in high memory.
    protected final static int PSP_HEAP_ATTR_EXT =       0x8000;       // Automatically extend the heap's memory.
    private HashMap<Integer, HeapInfo> heapMap;
    private final static int defaultAllocAlignment = 4;

    private static class HeapInfo {
    	private SysMemInfo sysMemInfo;
    	private MemoryChunkList freeMemoryChunks;
    	private HashMap<Integer, MemoryChunk> allocatedMemoryChunks;
    	private int allocType;

    	public HeapInfo(SysMemInfo sysMemInfo) {
    		this.sysMemInfo = sysMemInfo;
    		MemoryChunk memoryChunk = new MemoryChunk(sysMemInfo.addr, sysMemInfo.size);
    		freeMemoryChunks = new MemoryChunkList(memoryChunk);
    		allocatedMemoryChunks = new HashMap<Integer, MemoryChunk>();
    		allocType = sysMemInfo.type;
    	}

    	public int alloc(int size, int alignment) {
    		int allocatedAddr = 0;
    		switch (allocType) {
    			case PSP_SMEM_Low:
    				allocatedAddr = freeMemoryChunks.allocLow(size, alignment - 1);
    				break;
    			case PSP_SMEM_High:
    				allocatedAddr = freeMemoryChunks.allocHigh(size, alignment - 1);
    				break;
    		}

    		if (allocatedAddr == 0) {
    			return 0;
    		}

    		MemoryChunk memoryChunk = new MemoryChunk(allocatedAddr, size);
    		allocatedMemoryChunks.put(allocatedAddr, memoryChunk);

    		return allocatedAddr;
    	}

    	public boolean free(int addr) {
    		MemoryChunk memoryChunk = allocatedMemoryChunks.remove(addr);
    		if (memoryChunk == null) {
    			return false;
    		}

    		freeMemoryChunks.add(memoryChunk);

    		return true;
    	}

    	public void delete() {
    		Modules.SysMemUserForUserModule.free(sysMemInfo);
    	}

		@Override
		public String toString() {
			return String.format(String.format("HeapInfo 0x%08X, free=[%s]", sysMemInfo.addr, freeMemoryChunks));
		}
    }

	@Override
	public void start() {
		heapMap = new HashMap<Integer, sceHeap.HeapInfo>();
		super.start();
	}

	@Override
	public void stop() {
		for (HeapInfo heapInfo : heapMap.values()) {
			heapInfo.delete();
		}
		heapMap.clear();

		super.stop();
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0x0E875980, version = 500, checkInsideInterrupt = true)
    public int sceHeapReallocHeapMemory(TPointer heapAddr, TPointer memAddr, int memSize) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1C84B58D, version = 500, checkInsideInterrupt = true)
    public int sceHeapReallocHeapMemoryWithOption(TPointer heapAddr, TPointer memAddr, int memSize, TPointer paramAddr) {
        return 0;
    }

    @HLEFunction(nid = 0x2ABADC63, version = 500, checkInsideInterrupt = true)
    public int sceHeapFreeHeapMemory(TPointer heapAddr, TPointer memAddr) {
        // Try to free memory back to the heap.
    	HeapInfo heapInfo = heapMap.get(heapAddr.getAddress());
    	if (heapInfo == null) {
            return SceKernelErrors.ERROR_INVALID_ID;
    	}

    	if (!heapInfo.free(memAddr.getAddress())) {
            return SceKernelErrors.ERROR_INVALID_POINTER;
    	}
        if (log.isTraceEnabled()) {
        	log.trace(String.format("sceHeapFreeHeapMemory after free: %s", heapInfo));
        }

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2A0C2009, version = 500, checkInsideInterrupt = true)
    public int sceHeapGetMallinfo(TPointer heapAddr, TPointer infoAddr) {
        return 0;
    }

    @HLEFunction(nid = 0x2B7299D8, version = 500, checkInsideInterrupt = true)
    public int sceHeapAllocHeapMemoryWithOption(TPointer heapAddr, int memSize, @CanBeNull TPointer32 paramAddr) {
    	int alignment = defaultAllocAlignment;

    	if (paramAddr.isNotNull()) {
            int paramSize = paramAddr.getValue(0);
            if (paramSize == 8) {
                alignment = paramAddr.getValue(4);
                if (log.isDebugEnabled()) {
                	log.debug(String.format("sceHeapAllocHeapMemoryWithOption options: struct size=%d, alignment=0x%X", paramSize, alignment));
                }
            } else {
                log.warn(String.format("sceHeapAllocHeapMemoryWithOption option at %s(size=%d)", paramAddr, paramSize));
            }
        }

        // Try to allocate memory from the heap and return it's address.
        HeapInfo heapInfo = heapMap.get(heapAddr.getAddress());
        if (heapInfo == null) {
        	return 0;
        }

        int allocatedAddr = heapInfo.alloc(memSize, alignment);
        if (log.isTraceEnabled()) {
        	log.trace(String.format("sceHeapAllocHeapMemoryWithOption returns 0x%08X, after allocation: %s", allocatedAddr, heapInfo));
        }

        return allocatedAddr;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4929B40D, version = 500, checkInsideInterrupt = true)
    public int sceHeapGetTotalFreeSize(TPointer heapAddr) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7012BBDD, version = 500, checkInsideInterrupt = true)
    public int sceHeapIsAllocatedHeapMemory(TPointer heapAddr, TPointer memAddr) {
        return 0;
    }

    @HLEFunction(nid = 0x70210B73, version = 500, checkInsideInterrupt = true)
    public int sceHeapDeleteHeap(TPointer heapAddr) {
    	HeapInfo heapInfo = heapMap.remove(heapAddr.getAddress());
        if (heapInfo == null) {
        	return SceKernelErrors.ERROR_INVALID_ID;
        }

        heapInfo.delete();

        return 0;
    }

    @HLEFunction(nid = 0x7DE281C2, version = 500, checkInsideInterrupt = true)
    public int sceHeapCreateHeap(PspString name, int heapSize, int attr, @CanBeNull TPointer paramAddr) {
        if (paramAddr.isNotNull()) {
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
            log.warn(String.format("sceHeapCreateHeap not enough free mem (want=%d, free=%d, diff=%d)", totalHeapSize, maxFreeSize, totalHeapSize - maxFreeSize));
        }
        if (info == null) {
        	return 0; // Returns NULL on error.
        }

        HeapInfo heapInfo = new HeapInfo(info);
        heapMap.put(info.addr, heapInfo);

        return info.addr;
    }

    @HLEFunction(nid = 0xA8E102A0, version = 500, checkInsideInterrupt = true)
    public int sceHeapAllocHeapMemory(TPointer heapAddr, int memSize) {
        // Try to allocate memory from the heap and return it's address.
        HeapInfo heapInfo = heapMap.get(heapAddr.getAddress());
        if (heapInfo == null) {
        	return 0;
        }

        int allocatedAddr = heapInfo.alloc(memSize, defaultAllocAlignment);
        if (log.isTraceEnabled()) {
        	log.trace(String.format("sceHeapAllocHeapMemory returns 0x%08X, after allocation: %s", allocatedAddr, heapInfo));
        }

        return allocatedAddr;
    }
}