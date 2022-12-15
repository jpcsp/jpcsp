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

import static jpcsp.HLE.Modules.InitForKernelModule;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ERROR;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.USER_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.VSHELL_PARTITION_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.defaultSizeAlignment;
import static jpcsp.HLE.modules.sceSuspendForUser.KERNEL_VOLATILE_MEM_SIZE;
import static jpcsp.HLE.modules.sceSuspendForUser.KERNEL_VOLATILE_MEM_START;
import static jpcsp.Memory.addressMask;
import static jpcsp.MemoryMap.END_USERSPACE;
import static jpcsp.MemoryMap.START_KERNEL;
import static jpcsp.MemoryMap.START_USERSPACE;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.HLE.AfterCallbackAction;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointerFunction;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.MemoryChunk;
import jpcsp.HLE.kernel.types.MemoryChunkList;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelGameInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceSysmemMemoryBlockInfo;
import jpcsp.HLE.kernel.types.SceSysmemUidCB;
import jpcsp.HLE.kernel.types.SceSysmemUidCBtype;
import jpcsp.HLE.kernel.types.pspSysmemPartitionInfo;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Model;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class SysMemForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("SysMemForKernel");
    public static final int UID_FUNCTION_INITIALIZE = 0xD310D2D9;
    public static final int UID_FUNCTION_DELETE = 0x87089863;
    public static final int UID_FUNCTION_ALLOC = 0x0DE3B1BD;
    public static final int UID_FUNCTION_FREE = 0xA9CE362D;
    public static final int UID_FUNCTION_TOTAL_FREE_SIZE = 0x01DB36E1;
    protected HashMap<Integer, HeapInformation> heaps;
    private String npEnv;
    private int dnas;
    private SysMemInfo gameInfoMem;
    private TPointer gameInfoPtr;
    private SceKernelGameInfo gameInfo;
    private SysMemInfo dummyControlBlock;
    private int uidHeap;
    private int uidTypeListRoot;
    private int uidTypeListCount;
    private int uidTypeListMetaRoot;
    private int systemStatus;
    private TPointerFunction rebootKernelFunction;

    private static class HeapBlockInformation {
    	protected SysMemInfo sysMemInfo;
		protected MemoryChunkList freeMemoryChunks;
		protected final int id;
		private static int nextId = 1;

		public HeapBlockInformation(SysMemInfo sysMemInfo, MemoryChunkList freeMemoryChunks) {
			this.sysMemInfo = sysMemInfo;
			this.freeMemoryChunks = freeMemoryChunks;
			id = nextId++;
		}

		public void free() {
			Modules.SysMemUserForUserModule.free(sysMemInfo);
			sysMemInfo = null;
			freeMemoryChunks = null;
		}

		@Override
		public String toString() {
			return String.format("HeapBlock[id=0x%X, freeMemoryChunks=%s]", id, freeMemoryChunks);
		}
    }

    protected static class HeapInformation {
    	private static final String uidPurpose = "SysMemForKernel-Heap";
    	private static final int HEAP_BLOCK_HEADER_SIZE = 8;
    	protected final Memory mem;
    	protected final int uid;
		protected final int partitionId;
    	protected final int size;
    	protected final int flags;
		protected final int type = SysMemUserForUser.PSP_SMEM_Low; // Which memory type to use?
    	protected final String name;
    	protected final List<HeapBlockInformation> heapBlocks = new LinkedList<HeapBlockInformation>();

    	public HeapInformation(Memory mem, int partitionId, int size, int flags, String name) {
    		this.mem = mem;
			this.partitionId = partitionId;
			this.size = size;
			this.flags = flags;
			this.name = name;

			if (allocNewHeapBlock() != null) {
				uid = SceUidManager.getNewUid(uidPurpose);
			} else {
				uid = -1;
			}
    	}

    	public void free() {
    		for (HeapBlockInformation heapBlock : heapBlocks) {
    			heapBlock.free();
    		}
    		heapBlocks.clear();
			if (uid != -1) {
				SceUidManager.releaseUid(uid, uidPurpose);
			}
    	}

    	private HeapBlockInformation allocNewHeapBlock() {
    		SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionId, name, type, size, 0);
    		if (sysMemInfo == null) {
    			return null;
    		}

			MemoryChunk memoryChunk = new MemoryChunk(sysMemInfo.addr, size);
			HeapBlockInformation heapBlock = new HeapBlockInformation(sysMemInfo, new MemoryChunkList(memoryChunk));
			heapBlocks.add(heapBlock);

			return heapBlock;
    	}

    	private int allocBlock(HeapBlockInformation heapBlock, int blockSize) {
    		if (heapBlock == null || heapBlock.freeMemoryChunks == null) {
    			return 0;
    		}

    		MemoryChunk allocatedMemoryChunk = heapBlock.freeMemoryChunks.allocLow(blockSize + HEAP_BLOCK_HEADER_SIZE, 0);
			if (allocatedMemoryChunk == null) {
				return 0;
			}

    		mem.write32(allocatedMemoryChunk.addr, allocatedMemoryChunk.size - HEAP_BLOCK_HEADER_SIZE);
    		mem.write32(allocatedMemoryChunk.addr + 4, heapBlock.id);

    		return allocatedMemoryChunk.addr + HEAP_BLOCK_HEADER_SIZE;
    	}

    	public int allocBlock(int blockSize) {
    		if (heapBlocks.isEmpty()) {
    			return 0;
    		}

    		for (HeapBlockInformation heapBlock : heapBlocks) {
				int addr = allocBlock(heapBlock, blockSize);
				if (addr != 0) {
					return addr;
				}
    		}

    		if (blockSize <= size - HEAP_BLOCK_HEADER_SIZE) {
    			HeapBlockInformation heapBlock = allocNewHeapBlock();
    			int addr = allocBlock(heapBlock, blockSize);
    			if (addr != 0) {
    				return addr;
    			}
    		}

    		return 0;
    	}

    	public void freeBlock(int addr) {
    		addr -= HEAP_BLOCK_HEADER_SIZE;
    		int blockSize = mem.read32(addr);
    		int heapBlockId = mem.read32(addr + 4);

    		MemoryChunk memoryChunk = new MemoryChunk(addr, blockSize + HEAP_BLOCK_HEADER_SIZE);

    		for (HeapBlockInformation heapBlock : heapBlocks) {
    			if (heapBlock.id == heapBlockId) {
    				heapBlock.freeMemoryChunks.add(memoryChunk);

    				if (heapBlock.freeMemoryChunks.isCompletelyFree()) {
    					heapBlock.free();
    					heapBlocks.remove(heapBlock);
    				}
    				return;
    			}
    		}

    		log.error(String.format("HeapInformation.freeBlock could not free addr=0x%08X, blockSize=0x%X, heapBlockId=0x%X", addr, blockSize, heapBlockId));
    	}

		@Override
		public String toString() {
			return String.format("uid=0x%X, partitionId=0x%X, size=0x%X, flags=0x%X, name='%s', heapBlocks=%s", uid, partitionId, size, flags, name, heapBlocks);
		}
    }

    @Override
	public void start() {
		heaps = new HashMap<Integer, SysMemForKernel.HeapInformation>();
		npEnv = "np"; // Used in URLs to connect to the playstation sites
		dnas = 0;
		gameInfoMem = null;
		gameInfo = new SceKernelGameInfo();

		// Disable UMD cache
		gameInfo.flags |= 0x200;
		gameInfo.umdCacheOn = 0;

		uidHeap = sceKernelCreateHeap(SysMemUserForUser.KERNEL_PARTITION_ID, 0x2000, 1, "UID Heap");

		rebootKernelFunction = TPointerFunction.NULL;

		initUidBasic();

		super.start();
	}

	protected static String getUidFunctionIdName(int id) {
		switch (id) {
			case UID_FUNCTION_INITIALIZE: return "initialize";
			case UID_FUNCTION_DELETE: return "delete";
			case UID_FUNCTION_ALLOC: return "alloc";
			case UID_FUNCTION_FREE: return "free";
			case UID_FUNCTION_TOTAL_FREE_SIZE: return "totalFreeSize";
		}

		return String.format("0x%08X", id);
	}

	protected int newUid(int addr) {
		return (addr << 5) | ((uidTypeListCount++ & 0x3F) << 1) | 1;
	}

	public static int getCBFromUid(int uid) {
		return ((uid & ~0x7F) >> 5) | MemoryMap.START_RAM;
	}

	protected void initUidRoot() {
		Memory mem = Memory.getInstance();

		SceSysmemUidCB sceSysmemUidCBRoot = new SceSysmemUidCB();
    	int root = sceKernelAllocHeapMemory(uidHeap, sceSysmemUidCBRoot.sizeof());

    	SceSysmemUidCB sceSysmemUidCBMetaRoot = new SceSysmemUidCB();
		int metaRoot = sceKernelAllocHeapMemory(uidHeap, sceSysmemUidCBMetaRoot.sizeof());

		uidTypeListCount = 1;
		uidTypeListRoot = root;
		sceSysmemUidCBRoot.meta = metaRoot;
		sceSysmemUidCBRoot.parent0 = root;
		sceSysmemUidCBRoot.nextChild = root;
		sceSysmemUidCBRoot.uid = newUid(root);
		sceSysmemUidCBRoot.childSize = 6;
		sceSysmemUidCBRoot.allocAndSetName(mem, uidHeap, "Root");
		sceSysmemUidCBRoot.write(mem, root);

		uidTypeListMetaRoot = metaRoot;
		sceSysmemUidCBMetaRoot.meta = metaRoot;
		sceSysmemUidCBMetaRoot.parent0 = metaRoot;
		sceSysmemUidCBMetaRoot.nextChild = metaRoot;
		sceSysmemUidCBMetaRoot.uid = newUid(metaRoot);
		sceSysmemUidCBMetaRoot.childSize = 6;
		sceSysmemUidCBMetaRoot.allocAndSetName(mem, uidHeap, "MetaRoot");
		sceSysmemUidCBMetaRoot.write(mem, metaRoot);
	}

	protected void initUidBasic() {
		initUidRoot();

		Memory mem = Memory.getInstance();

		SceSysmemUidCBtype sceSysmemUidCBBasic = new SceSysmemUidCBtype();
    	int basic = sceKernelAllocHeapMemory(uidHeap, sceSysmemUidCBBasic.sizeof());

    	SceSysmemUidCBtype sceSysmemUidCBMetaBasic = new SceSysmemUidCBtype();
		int metaBasic = sceKernelAllocHeapMemory(uidHeap, sceSysmemUidCBMetaBasic.sizeof());

		SceSysmemUidCBtype sceSysmemUidCBMetaRoot = new SceSysmemUidCBtype();
    	sceSysmemUidCBMetaRoot.read(mem, uidTypeListMetaRoot);

    	SceSysmemUidCBtype sceSysmemUidCBRoot = new SceSysmemUidCBtype();
    	sceSysmemUidCBRoot.read(mem, uidTypeListRoot);

    	sceSysmemUidCBBasic.meta = metaBasic;
		sceSysmemUidCBBasic.parent0 = basic;
		sceSysmemUidCBBasic.nextChild = basic;
		sceSysmemUidCBBasic.uid = newUid(basic);
		sceSysmemUidCBBasic.childSize = sceSysmemUidCBRoot.childSize + ((4 + 3) >> 2);
		sceSysmemUidCBBasic.size = sceSysmemUidCBRoot.childSize;
		sceSysmemUidCBBasic.allocAndSetName(mem, uidHeap, "Basic");
		sceSysmemUidCBBasic.next = sceSysmemUidCBRoot.next;
		sceSysmemUidCBBasic.parent1 = uidTypeListRoot;
		sceSysmemUidCBBasic.write(mem, basic);

		sceSysmemUidCBRoot.next = basic;
		sceSysmemUidCBRoot.write(mem, uidTypeListRoot);

    	sceSysmemUidCBMetaRoot.next++;
		sceSysmemUidCBMetaRoot.write(mem, uidTypeListMetaRoot);

		sceSysmemUidCBMetaBasic.meta = uidTypeListMetaRoot;
		sceSysmemUidCBMetaBasic.parent0 = metaBasic;
		sceSysmemUidCBMetaBasic.nextChild = metaBasic;
		sceSysmemUidCBMetaBasic.uid = newUid(metaBasic);
		sceSysmemUidCBMetaBasic.childSize = 6;
		sceSysmemUidCBMetaBasic.allocAndSetName(mem, uidHeap, "MetaRoot");
		sceSysmemUidCBMetaBasic.parent1 = sceSysmemUidCBRoot.meta;
		sceSysmemUidCBMetaBasic.write(mem, metaBasic);
	}

	protected SceSysmemUidCBtype searchUidTypeByName(String name) {
		int cur = uidTypeListRoot;
		SceSysmemUidCBtype sceSysmemUidCB = new SceSysmemUidCBtype();
		Memory mem = Memory.getInstance();

		do {
			sceSysmemUidCB.read(mem, cur);
			if (name.equals(sceSysmemUidCB.name)) {
				return sceSysmemUidCB;
			}

			cur = sceSysmemUidCB.next;
		} while (cur != uidTypeListRoot);

		return null;
	}

    private int getUIDFunction(SceSysmemUidCBtype type, int funcId) {
    	if (type != null && type.funcTable != 0) {
    		TPointer32 funcTable = new TPointer32(Memory.getInstance(), type.funcTable);
    		for (int offset = 0; true; offset += 8) {
    			int id = funcTable.getValue(offset);
    			if (id == 0) {
    				break;
    			}
    			if (id == funcId) {
    				return funcTable.getValue(offset + 4);
    			}
    		}
    	}

    	return 0;
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
    @HLEFunction(nid = 0x1C1FBFE7, version = 150)
    @HLEFunction(nid = 0x58148F07, version = 660)
    public int sceKernelCreateHeap(int partitionId, int size, int flags, String name) {
		// Hook to force the allocation of a larger heap for the sceLoaderCore module
		size = Modules.LoadCoreForKernelModule.hleKernelCreateHeapHook(partitionId, size, flags, name);

    	HeapInformation info = new HeapInformation(getMemory(), partitionId, size, flags, name);

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
    @HLEFunction(nid = 0x23D81675, version = 660)
    public int sceKernelAllocHeapMemory(int heapId, int size) {
    	HeapInformation info = heaps.get(heapId);
    	if (info == null) {
    		return 0;
    	}

    	// Always allocate in blocks of 8 bytes
    	size = Utilities.alignUp(size, 7);

    	int addr = info.allocBlock(size);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelAllocHeapMemory(size=0x%X) returning 0x%08X, %s", size, addr, info));
    	}

    	return addr;
    }

    @HLEFunction(nid = 0xEB7A74DB, version = 150)
    @HLEFunction(nid = 0xF2284ECC, version = 600)
    public int sceKernelAllocHeapMemoryWithOption(int heapId, int size, @CanBeNull @BufferInfo(lengthInfo = LengthInfo.variableLength, usage = Usage.in) TPointer32 option) {
    	int alignment = 8;
    	if (option.isNotNull()) {
    		alignment = option.getValue(4);
    	}

    	HeapInformation info = heaps.get(heapId);
    	if (info == null) {
    		return 0;
    	}

    	size = Utilities.alignUp(size, alignment - 1);

    	int addr = info.allocBlock(size);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelAllocHeapMemoryWithOption(size=0x%X, alignment=0x%X) returning 0x%08X, %s", size, alignment, addr, info));
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
    @HLEFunction(nid = 0x87C2AB85, version = 660)
    public int sceKernelFreeHeapMemory(int heapId, TPointer block) {
    	HeapInformation info = heaps.get(heapId);
    	if (info == null) {
    		return -1;
    	}

    	info.freeBlock(block.getAddress());

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelFreeHeapMemory after free: %s", info));
    	}

    	return 0;
    }

    /**
     * Delete a heap.
     * 
     * @param heapId The UID of the heap to delete.
     * @return       0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xC9805775, version = 150)
    @HLEFunction(nid = 0xDD6512D0, version = 660)
    public int sceKernelDeleteHeap(int heapId) {
    	HeapInformation info = heaps.remove(heapId);
    	if (info == null) {
    		return -1;
    	}

    	info.free();

    	return 0;
    }

    @HLEFunction(nid = 0x6373995D, version = 280)
    @HLEFunction(nid = 0x07C586A1, version = 660)
    public int sceKernelGetModel() {
		int result = Model.getGeneration() - 1;

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernelGetModel returning %d(%s)", result, Model.getModelName()));
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
    @HLEFunction(nid = 0x7FF2F35A, version = 150)
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
    public int sceKernelSetParamSfo(@CanBeNull PspString discId, int unknown1, @CanBeNull PspString unknown2, @CanBeNull PspString unknown3, int unknown4, int unknown5, @CanBeNull PspString pspVersion) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBFD53FB7, version = 150)
    public int sceKernelGetDNAS() {
    	return dnas;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x982A4779, version = 150)
    public int sceKernelSetDNAS(int dnas) {
    	this.dnas = dnas;

    	return 0;
    }

	@HLEFunction(nid = 0xCD617A94, version = 260)
	@HLEFunction(nid = 0xAB5E85E5, version = 620)	
	@HLEFunction(nid = 0x3C4C5630, version = 635)
	@HLEFunction(nid = 0xEF29061C, version = 660)
    public int sceKernelGetGameInfo() {
    	// Has no parameters
    	if (gameInfoMem == null) {
    		gameInfoMem = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "SceKernelGameInfo", SysMemUserForUser.PSP_SMEM_Low, SceKernelGameInfo.SIZEOF, 0);
    		gameInfoPtr = new TPointer(getMemory(), gameInfoMem.addr);
    		gameInfoPtr.setValue32(0, SceKernelGameInfo.SIZEOF);
    	}
    	gameInfo.gameId = State.discId;
    	gameInfo.sdkVersion = Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion();
    	gameInfo.compilerVersion = Modules.SysMemUserForUserModule.hleKernelGetCompilerVersion();
    	switch (InitForKernelModule.sceKernelInitApitype()) {
	        case InitForKernel.SCE_INIT_APITYPE_EMU_EBOOT_EF:
	        case InitForKernel.SCE_INIT_APITYPE_EMU_BOOT_EF:
	        case InitForKernel.SCE_INIT_APITYPE_NPDRM_EF:
	        case InitForKernel.SCE_INIT_APITYPE_DISC_EMU_EF1:
	        case InitForKernel.SCE_INIT_APITYPE_UNK:
	        case InitForKernel.SCE_INIT_APITYPE_UNK_DEBUG:
	        case InitForKernel.SCE_INIT_APITYPE_EF2:
	        case InitForKernel.SCE_INIT_APITYPE_EF3:
	        case InitForKernel.SCE_INIT_APITYPE_EF4:
	        case InitForKernel.SCE_INIT_APITYPE_EF5:
	        case InitForKernel.SCE_INIT_APITYPE_EF6:
	        case InitForKernel.SCE_INIT_APITYPE_MLNAPP_EF:
	        	gameInfo.unk112 = 0x7F;
	        	break;
        	default:
        		gameInfo.unk112 = 0;
        		break;
    	}
    	gameInfo.str88 = "6.61";
    	gameInfo.str180 = "";
    	gameInfo.str196 = "00.00";
    	gameInfo.write(gameInfoPtr);

    	return gameInfoMem.addr;
    }

	@HLEUnimplemented
	@HLEFunction(nid = 0x1AB50974, version = 150)
	public int sceKernelJointMemoryBlock(int id1, int id2) {
		return 0;
	}

	@HLEFunction(nid = 0x22A114DC, version = 150)
    public int sceKernelMemset32(TPointer destAddr, int data, int size) {
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(destAddr.getAddress(), size, 4);
		for (int i = 0; i < size; i += 4) {
			memoryWriter.writeNext(data);
		}
		memoryWriter.flush();

        return 0;
    }

	@HLEFunction(nid = 0x181065AB, version = 660)
    public int sceKernelMemcpy(@BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer dst, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer src, int length) {
    	if (dst.getAddress() != src.getAddress()) {
    		dst.getMemory().memcpyWithVideoCheck(dst.getAddress(), src.getAddress(), length);
    	}

		return dst.getAddress();
	}

	@HLEFunction(nid = 0xE860BE8F, version = 150)
	public int sceKernelQueryMemoryBlockInfo(int id, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=56, usage=Usage.out) TPointer infoPtr) {
		SysMemInfo info = Modules.SysMemUserForUserModule.getSysMemInfo(id);
		if (info == null) {
			return -1;
		}

		SceSysmemMemoryBlockInfo blockInfo = new SceSysmemMemoryBlockInfo();
		blockInfo.read(infoPtr);
		blockInfo.name = info.name;
		blockInfo.attr = 0;
		blockInfo.addr = info.addr;
		blockInfo.memSize = info.size;
		blockInfo.sizeLocked = 0;
		blockInfo.unused = 0;
		blockInfo.write(infoPtr);

		return 0;
	}

	@HLEFunction(nid = 0xC90B0992, version = 150)
	public int sceKernelGetUIDcontrolBlock(int id, TPointer32 controlBlockAddr) {
    	Memory mem = Memory.getInstance();

    	if (SceUidManager.isValidUid(id)) {
    		if (dummyControlBlock == null) {
    			dummyControlBlock = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "DummyControlBlock", SysMemUserForUser.PSP_SMEM_Low, 36, 0);
    			if (dummyControlBlock == null) {
    				return -1;
    			}
    		}

    		TPointer dummyControlBlockPtr = new TPointer(mem, dummyControlBlock.addr);
    		dummyControlBlockPtr.clear(36);
    		dummyControlBlockPtr.setValue16(22, (short) 0x00FF); // SceSysmemUidCB.attr

    		controlBlockAddr.setValue(dummyControlBlockPtr.getAddress());

    		return 0;
    	}

    	if ((id & 0x80000001) != 1) {
    		return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
    	}
    	int cb = getCBFromUid(id);
    	SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    	sceSysmemUidCB.read(mem, cb);
    	if (sceSysmemUidCB.uid != id) {
    		return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
    	}

    	controlBlockAddr.setValue(cb);

		return 0;
	}

    @HLEFunction(nid = 0xAD09C397, version = 150)
    @HLEFunction(nid = 0xD222DAA7, version = 660)
	public int sceKernelCreateUIDtypeInherit(String parentName, String name, int size, @CanBeNull TPointer32 funcTable, @CanBeNull TPointer32 metaFuncTable, @BufferInfo(usage=Usage.out) TPointer32 uidTypeOut) {
    	Memory mem = Memory.getInstance();

    	if (funcTable.isNotNull()) {
    		for (int offset = 0; true; offset += 8) {
    			int id = funcTable.getValue(offset);
    			if (id == 0) {
    				break;
    			}
    			int addr = funcTable.getValue(offset + 4);

    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceKernelCreateUIDtypeInherit - funcTable id=%s, addr=0x%08X", getUidFunctionIdName(id), addr));
    			}
    		}
    	}

    	SceSysmemUidCBtype parentUidType = searchUidTypeByName(parentName);
    	if (parentUidType == null) {
    		return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID_TYPE;
    	}

    	SceSysmemUidCBtype sceSysmemUidCB = new SceSysmemUidCBtype();
    	int uidType = sceKernelAllocHeapMemory(uidHeap, sceSysmemUidCB.sizeof());

    	SceSysmemUidCBtype sceSysmemUidCBMeta = new SceSysmemUidCBtype();
    	int metaUidType = sceKernelAllocHeapMemory(uidHeap, sceSysmemUidCBMeta.sizeof());

    	sceSysmemUidCB.allocAndSetName(mem, uidHeap, name);
    	sceSysmemUidCBMeta.allocAndSetName(mem, uidHeap, "Meta" + name);

    	if (uidType <= 0 || metaUidType <= 0 || sceSysmemUidCB.nameAddr <= 0 || sceSysmemUidCBMeta.nameAddr <= 0) {
    		if (uidType > 0) {
    			sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, uidType));
    		}
    		if (metaUidType > 0) {
    			sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, metaUidType));
    		}
    		if (sceSysmemUidCB.nameAddr > 0) {
    			sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, sceSysmemUidCB.nameAddr));
    		}
    		if (sceSysmemUidCBMeta.nameAddr > 0) {
    			sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, sceSysmemUidCBMeta.nameAddr));
    		}

    		return SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
    	}

    	SceSysmemUidCBtype parentMetaUidType = new SceSysmemUidCBtype();
    	parentMetaUidType.read(mem, parentUidType.meta);
    	parentMetaUidType.next++;
    	parentMetaUidType.write(mem, parentUidType.meta);

    	SceSysmemUidCBtype rootUidType = new SceSysmemUidCBtype();
    	rootUidType.read(mem, uidTypeListRoot);

    	sceSysmemUidCB.parent0 = uidType;
    	sceSysmemUidCB.uid = newUid(uidType);
    	sceSysmemUidCB.nextChild = uidType;
    	sceSysmemUidCB.meta = metaUidType;
    	sceSysmemUidCB.childSize = parentUidType.childSize + ((size + 3) >> 2);
    	sceSysmemUidCB.size = parentUidType.childSize;
    	sceSysmemUidCB.name = name;
    	sceSysmemUidCB.next = rootUidType.next;
    	sceSysmemUidCB.parent1 = parentUidType.getBaseAddress();
    	sceSysmemUidCB.funcTable = funcTable.getAddress();
    	sceSysmemUidCB.write(mem, uidType);

    	sceSysmemUidCBMeta.nextChild = metaUidType;
    	sceSysmemUidCBMeta.meta = uidTypeListMetaRoot;
    	sceSysmemUidCBMeta.childSize = 6;
    	sceSysmemUidCBMeta.size = 0;
    	sceSysmemUidCBMeta.name = "Meta" + name;
    	sceSysmemUidCBMeta.funcTable = metaFuncTable.getAddress();
    	sceSysmemUidCBMeta.write(mem, metaUidType);

    	uidTypeOut.setValue(uidType);

    	return 0;
	}

    @HLEFunction(nid = 0xFEFC8666, version = 150)
    @HLEFunction(nid = 0x034129FB, version = 660)
    public int sceKernelCreateUIDtype(String name, int size, @CanBeNull TPointer32 funcTable, @CanBeNull TPointer32 metaFuncTable, @BufferInfo(usage=Usage.out) TPointer32 uidTypeOut) {
    	return sceKernelCreateUIDtypeInherit("Basic", name, size, funcTable, metaFuncTable, uidTypeOut);
    }

    @HLEFunction(nid = 0x89A74008, version = 150)
    @HLEFunction(nid = 0x0A34C078, version = 660)
    public int sceKernelCreateUID(TPointer uidType, String name, int k1, @BufferInfo(usage=Usage.out) TPointer32 outUid) {
    	Memory mem = uidType.getMemory();

    	SceSysmemUidCBtype sceSysmemUidCBType = new SceSysmemUidCBtype();
    	sceSysmemUidCBType.read(uidType);

    	int uid = sceKernelAllocHeapMemory(uidHeap, sceSysmemUidCBType.childSize << 2);
    	if (uid <= 0) {
    		return SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
    	}
    	mem.memset(uid, (byte) 0, sceSysmemUidCBType.childSize << 2);

    	SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    	sceSysmemUidCB.allocAndSetName(mem, uidHeap, name);
    	if (sceSysmemUidCB.nameAddr == 0) {
			sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, uid));
    		return SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
    	}

    	sceSysmemUidCB.attr = k1;
    	sceSysmemUidCB.uid = newUid(uid);
    	sceSysmemUidCB.nextChild = sceSysmemUidCBType.nextChild;
    	sceSysmemUidCB.parent0 = uidType.getAddress();
    	sceSysmemUidCB.size = sceSysmemUidCBType.size;
    	sceSysmemUidCB.childSize = sceSysmemUidCBType.childSize;
    	sceSysmemUidCB.meta = uidType.getAddress();
    	sceSysmemUidCB.write(mem, uid);

    	sceSysmemUidCBType.nextChild = uid;
    	sceSysmemUidCBType.write(uidType);

    	SceSysmemUidCB next = new SceSysmemUidCB();
    	next.read(mem, sceSysmemUidCB.nextChild);
    	next.parent0 = uid;
    	next.write(mem, sceSysmemUidCB.nextChild);

    	outUid.setValue(uid);

    	int funcAddr = getUIDFunction(sceSysmemUidCBType, UID_FUNCTION_INITIALIZE);
    	if (funcAddr != 0) {
    		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
    		Modules.ThreadManForUserModule.executeCallback(thread, funcAddr, null, false, uid, uidType.getAddress(), UID_FUNCTION_INITIALIZE);
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x2E3402CC, version = 150)
    @HLEFunction(nid = 0xA7622297, version = 660)
    public int sceKernelRenameUID(int id, String name) {
    	if (SceUidManager.isValidUid(id)) {
    		log.warn(String.format("sceKernelRenameUID called on id=0x%X, which has not been created by sceKernelCreateUID", id));
    		return 0;
    	}

    	if ((id & 0x80000001) != 1) {
    		return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
    	}
    	Memory mem = Memory.getInstance();

    	int cb = getCBFromUid(id);
    	SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    	sceSysmemUidCB.read(mem, cb);

    	sceSysmemUidCB.freeName(uidHeap);
    	sceSysmemUidCB.allocAndSetName(mem, uidHeap, name);
    	sceSysmemUidCB.write(mem, cb);

    	return 0;
    }

    @HLEFunction(nid = 0x8F20C4C0, version = 150)
    @HLEFunction(nid = 0x361F0F88, version = 660)
    public int sceKernelDeleteUID(int id) {
    	Memory mem = Memory.getInstance();

    	int cb = getCBFromUid(id);
    	SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    	sceSysmemUidCB.read(mem, cb);

    	SceSysmemUidCBtype sceSysmemUidCBtype = new SceSysmemUidCBtype();
    	sceSysmemUidCBtype.read(mem, sceSysmemUidCB.meta);
    	int funcAddr = getUIDFunction(sceSysmemUidCBtype, UID_FUNCTION_DELETE);
    	if (funcAddr != 0) {
    		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
    		Modules.ThreadManForUserModule.executeCallback(thread, funcAddr, null, false, cb, sceSysmemUidCB.meta, UID_FUNCTION_DELETE);
    	}

    	SceSysmemUidCB parent0 = new SceSysmemUidCB();
    	parent0.read(mem, sceSysmemUidCB.parent0);
    	parent0.nextChild = sceSysmemUidCB.nextChild;
    	parent0.write(mem, sceSysmemUidCB.parent0);

    	SceSysmemUidCB nextChild = new SceSysmemUidCB();
    	nextChild.read(mem, sceSysmemUidCB.nextChild);
    	nextChild.parent0 = sceSysmemUidCB.parent0;
    	nextChild.write(mem, sceSysmemUidCB.nextChild);

    	sceSysmemUidCB.meta = 0;
    	sceSysmemUidCB.uid = 0;
    	sceSysmemUidCB.nextChild = cb;
    	sceSysmemUidCB.parent0 = cb;
    	sceSysmemUidCB.write(mem, cb);

    	if (sceSysmemUidCB.nameAddr != 0) {
    		sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, sceSysmemUidCB.nameAddr));
    	}

    	sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, cb));

    	return 0;
    }

    /*
     * Query the partition information.
     * 
     * Parameters:
     * pid	- The partition id
     * info	- Pointer to the PspSysmemPartitionInfo structure
     * 
     * Returns
     *     0 on success.
     */
    @HLEFunction(nid = 0x55A40B2C, version = 150)
    @HLEFunction(nid = 0xC4EEAF20, version = 660)
    public int sceKernelQueryMemoryPartitionInfo(int partitionId, @BufferInfo(lengthInfo=LengthInfo.variableLength, usage=Usage.out) TPointer infoPtr) {
    	pspSysmemPartitionInfo partitionInfo = new pspSysmemPartitionInfo();
    	partitionInfo.read(infoPtr);

    	switch (partitionId) {
    		case KERNEL_PARTITION_ID:
    			partitionInfo.startAddr = START_KERNEL;
    			partitionInfo.memSize = KERNEL_VOLATILE_MEM_START - (START_KERNEL & addressMask);
    			partitionInfo.attr = 0xC;
    			break;
    		case USER_PARTITION_ID:
    			partitionInfo.startAddr = START_USERSPACE;
    			partitionInfo.memSize = END_USERSPACE - START_USERSPACE + 1;
    			partitionInfo.attr = 0x3;
    			break;
    		case VSHELL_PARTITION_ID:
    			partitionInfo.startAddr = KERNEL_VOLATILE_MEM_START;
    			partitionInfo.memSize = KERNEL_VOLATILE_MEM_SIZE;
    			partitionInfo.attr = 0xF;
    			break;
			default:
				log.warn(String.format("Unimplemented sceKernelQueryMemoryPartitionInfo partitionId=0x%X", partitionId));
				return -1;
    	}

    	partitionInfo.write(infoPtr);

    	return 0;
    }

    @HLEFunction(nid = 0x41FFC7F9, version = 150)
    @HLEFunction(nid = 0x44BDF332, version = 660)
    public int sceKernelGetUIDcontrolBlockWithType(int id, TPointer32 uidType, @BufferInfo(usage=Usage.out) TPointer32 controlBlockAddr) {
    	Memory mem = Memory.getInstance();

    	if ((id & 0x80000001) != 1) {
    		return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
    	}
    	int cb = getCBFromUid(id);
    	SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    	sceSysmemUidCB.read(mem, cb);
    	if (sceSysmemUidCB.uid != id) {
    		return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
    	}

    	controlBlockAddr.setValue(cb);

    	return 0;
    }

    @HLEFunction(nid = 0x235C2646, version = 660)
    public int sceKernelCallUIDObjCommonFunction(TPointer32 uid, TPointer32 uidWithFunc, int funcId) {
    	SceSysmemUidCB sceSysmemUidCB = new SceSysmemUidCB();
    	sceSysmemUidCB.read(uid);

    	SceSysmemUidCBtype sceSysmemUidCBtype = new SceSysmemUidCBtype();
    	sceSysmemUidCBtype.read(uidWithFunc);

    	SceSysmemUidCBtype sceSysmemUidCBparent1 = new SceSysmemUidCBtype();
    	sceSysmemUidCBparent1.read(Memory.getInstance(), sceSysmemUidCBtype.parent1);

    	int funcAddr = getUIDFunction(sceSysmemUidCBparent1, funcId);
    	if (funcAddr != 0) {
    		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
    		Modules.ThreadManForUserModule.executeCallback(thread, funcAddr, null, false, uid.getAddress(), sceSysmemUidCBtype.parent1, funcId);
    	}

    	return sceSysmemUidCB.uid;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B3E7441, version = 150)
    @HLEFunction(nid = 0x1E6BB8E8, version = 660)
    public void sceKernelMemoryExtendSize() {
    	// Has no parameters
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE0058030, version = 150)
    @HLEFunction(nid = 0x7A7CD7BC, version = 660)
    public void sceKernelMemoryShrinkSize() {
    	// Has no parameters
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCBB05241, version = 150)
    @HLEFunction(nid = 0xF19BA38D, version = 660)
    public int sceKernelSetAllowReplaceUmd(boolean allow) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1404C1AA, version = 660)
    public int sceKernelSetUmdCacheOn(int umdCacheOn) {
    	gameInfo.umdCacheOn = umdCacheOn;
		gameInfo.flags |= 0x200;

    	return 0;
    }

    @HLEFunction(nid = 0x96A3CE2C, version = 150)
    public int sceKernelSetRebootKernel(TPointerFunction rebootKernelFunction) {
    	this.rebootKernelFunction = rebootKernelFunction;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x36C503A9, version = 150)
    public int sceKernelGetSystemStatus() {
    	return systemStatus;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x521AC5A4, version = 150)
    public int sceKernelSetSystemStatus(int systemStatus) {
    	int oldSystemStatus = this.systemStatus;
    	this.systemStatus = systemStatus;

    	return oldSystemStatus;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4A325AA0, version = 150)
    public int sceKernelGetInitialRandomValue() {
    	return 0x12345678;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D9E2DD6, version = 150)
    public int sceKernelSysMemRealMemorySize() {
    	return MemoryMap.SIZE_RAM;
    }

    @HLEFunction(nid = 0x9BAC123D, version = 150)
    public int sceKernelMemmove(TPointer destAddr, TPointer srcAddr, int size) {
    	if (destAddr.getAddress() != srcAddr.getAddress()) {
    		destAddr.memmove(srcAddr.getAddress(), size);
    	}

    	return destAddr.getAddress();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x83B5226D, version = 150)
    public int sceKernelSetDdrMemoryProtection(TPointer addr, int size, int set) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4972F9D1, version = 150)
    public int sceKernelGetAllowReplaceUmd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9B20ACEF, version = 150)
    public int sceKernelMemmoveWithFill(TPointer dstAddr, TPointer srcAddr, int size, int fill) {
    	// Calling memmove
    	dstAddr.memmove(srcAddr.getAddress(), size);
    	// TODO: implement the fill parameter

    	return dstAddr.getAddress();
    }

    @HLEFunction(nid = 0x310802A9, version = 150)
    public int sceKernelRegisterGetIdFunc(TPointer function) {
    	Modules.scePopsManModule.hlePopsRegisterGetIdFunc(function);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD0C1460D, version = 150)
    public int sceKernelGetId(PspString fileName, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 16, usage = Usage.out) TPointer keyAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF7E78B33, version = 150)
    public int sceKernelSeparateMemoryBlock(int id, boolean cutBefore, int size) {
    	return 0;
    }

    @HLEFunction(nid = 0xFAF29F34, version = 150)
    public int sceKernelQueryMemoryInfo(int address, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 partitionIdAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 memoryBlockIdAddr) {
		SysMemInfo info = Modules.SysMemUserForUserModule.getSysMemInfoByAddress(address);
		if (info == null) {
			return -1;
		}

		partitionIdAddr.setValue(info.partitionid);
    	memoryBlockIdAddr.setValue(info.uid);

    	return 0;
    }

    @HLEFunction(nid = 0xFB5BEB66, version = 150)
    public int sceKernelResizeMemoryBlock(int id, int leftShift, int rightShift) {
		SysMemInfo info = Modules.SysMemUserForUserModule.getSysMemInfo(id);
		if (info == null) {
			return -1;
		}

		leftShift = leftShift / defaultSizeAlignment * defaultSizeAlignment;
		rightShift = rightShift / defaultSizeAlignment * defaultSizeAlignment;

		if (!Modules.SysMemUserForUserModule.resizeMemoryBlock(info, leftShift, rightShift)) {
			return SceKernelErrors.ERROR_KERNEL_FAILED_RESIZE_MEMBLOCK;
		}

		return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x40B744A4, version = 150)
    public int SysMemForKernel_40B744A4(int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBFE08689, version = 150)
    public int SysMemForKernel_BFE08689(@CanBeNull @StringInfo(maxLength=64) PspString unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEA1CABF1, version = 150)
    public int sceKernelFillFreeBlock(int mpid, int c) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x536AD5E1, version = 150)
    public int SysMemForKernel_536AD5E1() {
    	return 0;
    }

    @HLEFunction(nid = 0x9697CD32, version = 150)
    public int sceKernelPartitionTotalFreeMemSize(int partitionId) {
		return Modules.SysMemUserForUserModule.totalFreeMemSize(partitionId);
    }

    @HLEFunction(nid = 0xE6581468, version = 150)
    public int sceKernelPartitionMaxFreeMemSize(int partitionId) {
		return Modules.SysMemUserForUserModule.maxFreeMemSize(partitionId);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEB4C0E1B, version = 150)
    public int sceKernelQueryBlockSize() {
		return 0;
    }

    @HLEFunction(nid = 0xE73FBC0B, version = 150)
    public int sceKernelRebootKernel(int arg) {
    	if (rebootKernelFunction.isNull()) {
    		return ERROR_KERNEL_ERROR;
    	}

    	AfterCallbackAction afterCallbackAction = new AfterCallbackAction(rebootKernelFunction);
    	Modules.ThreadManForUserModule.executeCallback(null, rebootKernelFunction.getAddress(), afterCallbackAction, false, arg);

    	return afterCallbackAction.getReturnValue();
    }
}