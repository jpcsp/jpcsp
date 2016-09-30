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
package jpcsp.HLE.modules;

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.MemoryChunk;
import jpcsp.HLE.kernel.types.MemoryChunkList;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Model;

import org.apache.log4j.Logger;

public class SysMemForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("SysMemForKernel");
    protected HashMap<Integer, HeapInformation> heaps;
    private String npEnv;

    protected static class HeapInformation {
    	private static final String uidPurpose = "SysMemForKernel-Heap";
    	private static final int HEAP_BLOCK_HEADER_SIZE = 8;
    	protected final int uid;
		protected final int partitionId;
    	protected final int size;
    	protected final int flags;
    	protected final String name;
    	protected SysMemInfo sysMemInfo;
    	protected int allocatedSize;
    	protected MemoryChunkList freeMemoryChunks;

    	public HeapInformation(int partitionId, int size, int flags, String name) {
			this.partitionId = partitionId;
			this.size = size;
			this.flags = flags;
			this.name = name;

			allocatedSize = 0;

			int type = SysMemUserForUser.PSP_SMEM_Low; // Which memory type to use?
			sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionId, name, type, size, 0);
			if (sysMemInfo == null) {
				uid = -1;
			} else {
				MemoryChunk memoryChunk = new MemoryChunk(sysMemInfo.addr, size);
				freeMemoryChunks = new MemoryChunkList(memoryChunk);

				uid = SceUidManager.getNewUid(uidPurpose);
			}
    	}

    	public void free() {
    		if (sysMemInfo != null) {
    			Modules.SysMemUserForUserModule.free(sysMemInfo);
    			sysMemInfo = null;
    			freeMemoryChunks = null;

    			SceUidManager.releaseUid(uid, uidPurpose);
    		}
    	}

    	public int allocBlock(int blockSize) {
    		if (freeMemoryChunks == null) {
    			return 0;
    		}

    		int addr = freeMemoryChunks.allocLow(blockSize + HEAP_BLOCK_HEADER_SIZE, 0);
    		if (addr == 0) {
    			return 0;
    		}

    		Memory.getInstance().write32(addr, blockSize);

    		return addr + HEAP_BLOCK_HEADER_SIZE;
    	}

    	public void freeBlock(int addr) {
    		addr -= HEAP_BLOCK_HEADER_SIZE;
    		int blockSize = Memory.getInstance().read32(addr);

    		MemoryChunk memoryChunk = new MemoryChunk(addr, blockSize);
    		freeMemoryChunks.add(memoryChunk);
    	}
    }

	@Override
	public void start() {
		heaps = new HashMap<Integer, SysMemForKernel.HeapInformation>();
		npEnv = "np"; // Used in URLs to connect to the playstation sites

		super.start();
	}

	@HLEFunction(nid = 0xA089ECA4, version = 150)
    public int sceKernelMemset(@BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer destAddr, int data, int size) {
        destAddr.memset((byte) data, size);

        return 0;
    }

	@HLEFunction(nid = 0x8AE776AF, version = 660)
    public int sceKernelMemset_660(TPointer destAddr, int data, int size) {
		return sceKernelMemset(destAddr, data, size);
    }

    /**
     * Create a heap.
     * 
     * @param partitionId The UID of the partition where allocate the heap. 
     * @param size        The size in bytes of the heap.
     * @param flags       Unknown, probably some flag or type, pass 1.
     * @param name        Name assigned to the new heap.
     * @return            The UID of the new heap, or if less than 0 an error. 
     */
	@HLELogging(level = "info")
    @HLEFunction(nid = 0x1C1FBFE7, version = 150)
    public int sceKernelCreateHeap(int partitionId, int size, int flags, String name) {
    	HeapInformation info = new HeapInformation(partitionId, size, flags, name);

    	if (info.uid >= 0) {
    		heaps.put(info.uid, info);
    	}

    	return info.uid;
    }

    /**
     * Allocate a memory block from a heap.
     * 
     * @param heapId The UID of the heap to allocate from.
     * @param size   The number of bytes to allocate.
     * @return       The address of the allocated memory block, or NULL on error.
     */
    @HLEFunction(nid = 0x636C953B, version = 150)
    public int sceKernelAllocHeapMemory(int heapId, int size) {
    	HeapInformation info = heaps.get(heapId);
    	if (info == null) {
    		return 0;
    	}

    	int addr = info.allocBlock(size);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelAllocHeapMemory returning 0x%08X", addr));
    	}

    	return addr;
    }

    /**
     * Free a memory block allocated from a heap.
     * 
     * @param heapId The UID of the heap where block belongs. 
     * @param block  The block of memory to free from the heap.
     * @return       0 on success, < 0 on error. 
     */
    @HLEFunction(nid = 0x7B749390, version = 150)
    public int sceKernelFreeHeapMemory(int heapId, TPointer block) {
    	HeapInformation info = heaps.get(heapId);
    	if (info == null) {
    		return -1;
    	}

    	info.freeBlock(block.getAddress());

    	return 0;
    }

    /**
     * Delete a heap.
     * 
     * @param heapId The UID of the heap to delete.
     * @return       0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xC9805775, version = 150)
    public int sceKernelDeleteHeap(int heapId) {
    	HeapInformation info = heaps.remove(heapId);
    	if (info == null) {
    		return -1;
    	}

    	info.free();

    	return 0;
    }

    @HLEFunction(nid = 0x6373995D, version = 280)
    public int sceKernelGetModel() {
		int result = Model.getModel(); // <= 0 original, 1 slim

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernelGetModel returning %d(%s)", result, Model.getModelName(result)));
		}

		return result;
	}

    @HLEFunction(nid = 0x07C586A1, version = 660)
    public int sceKernelGetModel_660() {
		int result = Model.getModel(); // <= 0 original, 1 slim

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernelGetModel_660 returning %d(%s)", result, Model.getModelName(result)));
		}

		return result;
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0x945E45DA, version = 150)
    public int SysMemUserForUser_945E45DA(TPointer unknown) {
    	unknown.setStringNZ(9, npEnv);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7FF2F35A, version = 660)
    public int SysMemForKernel_7FF2F35A(TPointer unknown) {
    	return SysMemUserForUser_945E45DA(unknown);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA03CB480, version = 660)
    public int SysMemForKernel_A03CB480(TPointer unknown) {
    	npEnv = unknown.getStringNZ(8);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("SysMemForKernel_A03CB480 setting unknownString='%s'", npEnv));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x807179E7, version = 150)
    public int sceKernelSetParamSfo(PspString discId, int unknown1, int unknown2, PspString unknown3, int unknown4, int unknown5, PspString pspVersion) {
    	return 0;
    }
}