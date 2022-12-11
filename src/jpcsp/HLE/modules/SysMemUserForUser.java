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

import static jpcsp.Allegrex.Common._a1;
import static jpcsp.Allegrex.Common._t3;
import static jpcsp.HLE.modules.sceSuspendForUser.KERNEL_VOLATILE_MEM_SIZE;
import static jpcsp.HLE.modules.sceSuspendForUser.KERNEL_VOLATILE_MEM_START;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.CpuState;
import jpcsp.Debugger.DumpDebugState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.MemoryChunk;
import jpcsp.HLE.kernel.types.MemoryChunkList;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

/*
 * TODO list:
 * 1. Use the partitionid in functions that use it as a parameter.
 *  -> Info:
 *      1 = kernel, 2 = user, 3 = me, 4 = kernel mirror (from potemkin/dash)
 *      http://forums.ps2dev.org/viewtopic.php?p=75341#75341
 *      8 = slim, topaddr = 0x8A000000, size = 0x1C00000 (28 MB), attr = 0x0C
 *      8 = slim, topaddr = 0x8BC00000, size = 0x400000 (4 MB), attr = 0x0C
 *
 * 2. Implement format string parsing and reading variable number of parameters
 * in sceKernelPrintf.
 */
public class SysMemUserForUser extends HLEModule {
    public static Logger log = Modules.getLogger("SysMemUserForUser");
    protected static Logger stdout = Logger.getLogger("stdout");
    protected static HashMap<Integer, SysMemInfo> blockList;
    protected static MemoryChunkList[] freeMemoryChunks;
    protected int firmwareVersion = 150;
    public static final int defaultSizeAlignment = 256;

    // PspSysMemBlockTypes
    public static final int PSP_SMEM_Low = 0;
    public static final int PSP_SMEM_High = 1;
    public static final int PSP_SMEM_Addr = 2;
    public static final int PSP_SMEM_LowAligned = 3;
    public static final int PSP_SMEM_HighAligned = 4;

    public static final int KERNEL_PARTITION_ID = 1;
    public static final int USER_PARTITION_ID = 2;
    public static final int VSHELL_PARTITION_ID = 5;

	protected boolean started = false;
    private int compiledSdkVersion;
    private int compilerVersion;

	@Override
	public void load() {
		reset();

		super.load();
	}

	@Override
	public void start() {
		if (!started) {
			reset();
			started = true;
		}

		compiledSdkVersion = 0;
		compilerVersion = 0;

		super.start();
	}

	@Override
	public void stop() {
		started = false;

        super.stop();
	}

	private MemoryChunkList createMemoryChunkList(int startAddr, int endAddr) {
		startAddr &= Memory.addressMask;
		endAddr &= Memory.addressMask;

		MemoryChunk initialMemory = new MemoryChunk(startAddr, endAddr - startAddr + 1);

		return new MemoryChunkList(initialMemory);
	}

	public void reset() {
		reset(false);
	}

	public void reset(boolean preserveKernelMemory) {
		if (blockList == null || freeMemoryChunks == null) {
			preserveKernelMemory = false;
		}

		if (preserveKernelMemory) {
			List<SysMemInfo> toBeFreed = new LinkedList<SysMemInfo>();
			for (SysMemInfo sysMemInfo: blockList.values()) {
				if (sysMemInfo.partitionid == USER_PARTITION_ID) {
					toBeFreed.add(sysMemInfo);
				}
			}

			for (SysMemInfo sysMemInfo : toBeFreed) {
				sysMemInfo.free();
			}
		} else {
			blockList = new HashMap<Integer, SysMemInfo>();
		}

		if (!preserveKernelMemory) {
	        // free memory chunks for each partition
	        freeMemoryChunks = new MemoryChunkList[6];
	        freeMemoryChunks[KERNEL_PARTITION_ID] = createMemoryChunkList(MemoryMap.START_KERNEL, KERNEL_VOLATILE_MEM_START - 1);
	        freeMemoryChunks[VSHELL_PARTITION_ID] = createMemoryChunkList(KERNEL_VOLATILE_MEM_START, KERNEL_VOLATILE_MEM_START + KERNEL_VOLATILE_MEM_SIZE - 1);
		}
        freeMemoryChunks[USER_PARTITION_ID] = createMemoryChunkList(MemoryMap.START_USERSPACE, MemoryMap.END_USERSPACE);
	}

    public void setMemory64MB(boolean isMemory64MB) {
    	if (isMemory64MB) {
    		setMemorySize(MemoryMap.END_RAM_64MB - MemoryMap.START_RAM + 1); // 64 MB
    	} else {
    		setMemorySize(MemoryMap.END_RAM_32MB - MemoryMap.START_RAM + 1); // 32 MB
    	}
    }

    public void setMemorySize(int memorySize) {
    	if (MemoryMap.SIZE_RAM != memorySize) {
    		int kernelSize = MemoryMap.END_KERNEL - MemoryMap.START_KERNEL + 1;
    		int kernelSize32 = kernelSize >> 2;
    		int[] savedKernelMemory = new int[kernelSize32];
    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(MemoryMap.START_KERNEL, kernelSize, 4);
    		for (int i = 0; i < kernelSize32; i++) {
    			savedKernelMemory[i] = memoryReader.readNext();
    		}

    		int previousMemorySize = MemoryMap.SIZE_RAM;
    		MemoryMap.END_RAM = MemoryMap.START_RAM + memorySize - 1;
    		MemoryMap.END_USERSPACE = MemoryMap.END_RAM;
    		MemoryMap.SIZE_RAM = MemoryMap.END_RAM - MemoryMap.START_RAM + 1;

    		if (!Memory.getInstance().allocate()) {
				log.error(String.format("Failed to resize the PSP memory from 0x%X to 0x%X", previousMemorySize, memorySize));
				Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_ANY);
			}

    		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(MemoryMap.START_KERNEL, kernelSize, 4);
    		for (int i = 0; i < kernelSize32; i++) {
    			memoryWriter.writeNext(savedKernelMemory[i]);
    		}
    		memoryWriter.flush();

    		reset(true);
    	}
    }

    public static class SysMemInfo implements Comparable<SysMemInfo> {

        public final int uid;
        public final int partitionid;
        public final String name;
        public final int type;
        public int size;
        public int allocatedSize;
        public int addr;

        public SysMemInfo(int partitionid, String name, int type, int size, int allocatedSize, int addr) {
            this.partitionid = partitionid;
            this.name = name;
            this.type = type;
            this.size = size;
            this.allocatedSize = allocatedSize;
            this.addr = addr;

            uid = SceUidManager.getNewUid("SysMem");
            blockList.put(uid, this);
        }

        @Override
        public String toString() {
            return String.format("SysMemInfo[addr=0x%08X-0x%08X, uid=0x%X, partition=%d, name='%s', type=%s, size=0x%X (allocated=0x%X)]", addr, addr + allocatedSize, uid, partitionid, name, getTypeName(type), size, allocatedSize);
        }

        public void free() {
        	blockList.remove(uid);
        }

        @Override
        public int compareTo(SysMemInfo o) {
            if (addr == o.addr) {
                log.warn("Set invariant broken for SysMemInfo " + this);
                return 0;
            }
            return addr < o.addr ? -1 : 1;
        }
    }

    static protected String getTypeName(int type) {
        String typeName;

        switch (type) {
            case PSP_SMEM_Low:
                typeName = "PSP_SMEM_Low";
                break;
            case PSP_SMEM_High:
                typeName = "PSP_SMEM_High";
                break;
            case PSP_SMEM_Addr:
                typeName = "PSP_SMEM_Addr";
                break;
            case PSP_SMEM_LowAligned:
                typeName = "PSP_SMEM_LowAligned";
                break;
            case PSP_SMEM_HighAligned:
                typeName = "PSP_SMEM_HighAligned";
                break;
            default:
                typeName = "UNHANDLED " + type;
                break;
        }

        return typeName;
    }

    private boolean isValidPartitionId(int partitionid) {
    	return partitionid >= 0 && partitionid < freeMemoryChunks.length && freeMemoryChunks[partitionid] != null;    	
    }

    // Allocates to 256-byte alignment
    public SysMemInfo malloc(int partitionid, String name, int type, int size, int addr) {
    	if (freeMemoryChunks == null) {
    		return null;
    	}

    	MemoryChunk allocatedMemoryChunk = null;
        int allocatedSize = 0;

        if (isValidPartitionId(partitionid)) {
        	MemoryChunkList freeMemoryChunk = freeMemoryChunks[partitionid];
        	int alignment = defaultSizeAlignment - 1;

	        // The allocated size has not to be aligned to the requested alignment
	        // (for PSP_SMEM_LowAligned or PSP_SMEM_HighAligned),
	        // it is only aligned to the default size alignment.
	        allocatedSize = Utilities.alignUp(size, alignment);

	        if (type == PSP_SMEM_LowAligned || type == PSP_SMEM_HighAligned) {
	            // Use the alignment provided in the addr parameter
	            alignment = addr - 1;
	        }

	        switch (type) {
	        	case PSP_SMEM_Low:
	        	case PSP_SMEM_LowAligned:
	        		allocatedMemoryChunk = freeMemoryChunk.allocLow(allocatedSize, alignment);
	        		break;
	        	case PSP_SMEM_High:
	        	case PSP_SMEM_HighAligned:
	        		allocatedMemoryChunk = freeMemoryChunk.allocHigh(allocatedSize, alignment);
	        		break;
	        	case PSP_SMEM_Addr:
	        		allocatedMemoryChunk = freeMemoryChunk.alloc(addr & Memory.addressMask, allocatedSize);
	        		break;
	    		default:
	    			log.warn(String.format("malloc: unknown type %s", getTypeName(type)));
	        }
        }

        SysMemInfo sysMemInfo;
		if (allocatedMemoryChunk == null) {
            log.warn(String.format("malloc cannot allocate partition=%d, name='%s', type=%s, size=0x%X, addr=0x%08X, maxFreeMem=0x%X, totalFreeMem=0x%X", partitionid, name, getTypeName(type), size, addr, maxFreeMemSize(partitionid), totalFreeMemSize(partitionid)));
			if (log.isDebugEnabled()) {
				log.debug("Free list: " + getDebugFreeMem());
				log.debug("Allocated blocks:\n" + getDebugAllocatedMem() + "\n");
			}
			sysMemInfo = null;
		} else {
			sysMemInfo = new SysMemInfo(partitionid, name, type, size, allocatedMemoryChunk.size, allocatedMemoryChunk.addr);

			if (log.isDebugEnabled()) {
				log.debug(String.format("malloc partition=%d, name='%s', type=%s, size=0x%X, addr=0x%08X: returns 0x%08X", partitionid, name, getTypeName(type), size, addr, allocatedMemoryChunk.addr));
				if (log.isTraceEnabled()) {
					log.trace("Free list after malloc:\n" + getDebugFreeMem() + "\n");
					log.trace("Allocated blocks after malloc:\n" + getDebugAllocatedMem() + "\n");
				}
			}
		}

		return sysMemInfo;
    }

    public String getDebugFreeMem() {
    	StringBuilder s = new StringBuilder();
    	s.append(String.format("partition=%d: ", KERNEL_PARTITION_ID));
    	s.append(freeMemoryChunks[KERNEL_PARTITION_ID].toString());
    	if (freeMemoryChunks[USER_PARTITION_ID] != null) {
	    	s.append("\n");
	    	s.append(String.format("partition=%d: ", USER_PARTITION_ID));
	    	s.append(freeMemoryChunks[USER_PARTITION_ID].toString());
    	}

    	return s.toString();
    }

    public String getDebugAllocatedMem() {
    	StringBuilder result = new StringBuilder();

    	// Sort allocated blocks by address
    	List<SysMemInfo> sortedBlockList = Collections.list(Collections.enumeration(blockList.values()));
    	Collections.sort(sortedBlockList);

    	for (SysMemInfo sysMemInfo : sortedBlockList) {
    		if (result.length() > 0) {
    			result.append("\n");
    		}
    		result.append(sysMemInfo.toString());
    	}

    	return result.toString();
    }

    private void free(int partitionId, int addr, int size) {
    	MemoryChunk memoryChunk = new MemoryChunk(addr, size);
    	freeMemoryChunks[partitionId].add(memoryChunk);
    }

    private int alloc(int partitionId, int addr, int size) {
    	MemoryChunk allocatedMemoryChunk = freeMemoryChunks[partitionId].alloc(addr, size);
    	if (allocatedMemoryChunk == null) {
    		return 0;
    	}
    	return allocatedMemoryChunk.addr;
    }

    public void free(SysMemInfo info) {
    	if (info != null) {
    		info.free();
    		free(info.partitionid, info.addr, info.allocatedSize);

	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("free %s", info.toString()));
	    		if (log.isTraceEnabled()) {
	    			log.trace("Free list after free: " + getDebugFreeMem());
					log.trace("Allocated blocks after free:\n" + getDebugAllocatedMem() + "\n");
	    		}
	    	}
    	}
    }

    public int maxFreeMemSize(int partitionid) {
    	int maxFreeMemSize = 0;
    	if (isValidPartitionId(partitionid)) {
	    	for (MemoryChunk memoryChunk = freeMemoryChunks[partitionid].getLowMemoryChunk(); memoryChunk != null; memoryChunk = memoryChunk.next) {
	    		if (memoryChunk.size > maxFreeMemSize) {
	    			maxFreeMemSize = memoryChunk.size;
	    		}
	    	}
    	}
		return maxFreeMemSize;
    }

    public int totalFreeMemSize(int partitionid) {
        int totalFreeMemSize = 0;
    	if (isValidPartitionId(partitionid)) {
	    	for (MemoryChunk memoryChunk = freeMemoryChunks[partitionid].getLowMemoryChunk(); memoryChunk != null; memoryChunk = memoryChunk.next) {
	    		totalFreeMemSize += memoryChunk.size;
	    	}
    	}

    	return totalFreeMemSize;
    }

    public SysMemInfo getSysMemInfo(int uid) {
    	return blockList.get(uid);
    }

    public SysMemInfo getSysMemInfoByAddress(int address) {
    	for (SysMemInfo info : blockList.values()) {
    		if (address >= info.addr && address < info.addr + info.size) {
    			return info;
    		}
    	}

    	return null;
    }

    public SysMemInfo separateMemoryBlock(SysMemInfo info, int size) {
    	int newAddr = info.addr + size;
    	int newSize = info.size - size;
    	int newAllocatedSize = info.allocatedSize - size;

    	// Create a new memory block
    	SysMemInfo newSysMemInfo = new SysMemInfo(info.partitionid, info.name, info.type, newSize, newAllocatedSize, newAddr);

    	// Resize the previous memory block
    	info.size -= newSize;
    	info.allocatedSize -= newAllocatedSize;

    	return newSysMemInfo;
    }

    public boolean resizeMemoryBlock(SysMemInfo info, int leftShift, int rightShift) {
    	if (rightShift < 0) {
    		int sizeToFree = -rightShift;
    		free(info.partitionid, info.addr + info.allocatedSize - sizeToFree, sizeToFree);
    		info.allocatedSize -= sizeToFree;
    		info.size -= sizeToFree;
    	} else if (rightShift > 0) {
    		int sizeToExtend = rightShift;
    		int extendAddr = alloc(info.partitionid, info.addr + info.allocatedSize, sizeToExtend);
    		if (extendAddr == 0) {
    			return false;
    		}
    		info.allocatedSize += sizeToExtend;
    		info.size += sizeToExtend;
    	}

    	if (leftShift < 0) {
    		int sizeToFree = -leftShift;
    		free(info.partitionid, info.addr, sizeToFree);
    		info.addr += sizeToFree;
    		info.size -= sizeToFree;
    		info.allocatedSize -= sizeToFree;
    	} else if (leftShift > 0) {
    		int sizeToExtend = leftShift;
    		int extendAddr = alloc(info.partitionid, info.addr - sizeToExtend, sizeToExtend);
    		if (extendAddr == 0) {
    			return false;
    		}
    		info.addr -= sizeToExtend;
    		info.allocatedSize += sizeToExtend;
    		info.size += sizeToExtend;
    	}

    	return true;
    }

    /** @param firmwareVersion : in this format: ABB, where A = major and B = minor, for example 271 */
    public void setFirmwareVersion(int firmwareVersion) {
    	this.firmwareVersion = firmwareVersion;
    }

    public int getFirmwareVersion() {
    	return firmwareVersion;
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

        for (Iterator<SysMemInfo> it = blockList.values().iterator(); it.hasNext();) {
            SysMemInfo info = it.next();
            for (int i = info.addr; i < info.addr + info.size; i += SLOT_SIZE) {
                if (i >= 0x08800000 && i < 0x0A000000) {
                    allocated[(i - 0x08800000) / SLOT_SIZE] = true;
                }
            }
            allocatedSize += info.size;
        }

        for (MemoryChunk memoryChunk = freeMemoryChunks[USER_PARTITION_ID].getLowMemoryChunk(); memoryChunk != null; memoryChunk = memoryChunk.next) {
            for (int i = memoryChunk.addr; i < memoryChunk.addr + memoryChunk.size; i += SLOT_SIZE) {
                if (i >= 0x08800000 && i < 0x0A000000) {
                    fragmented[(i - 0x08800000) / SLOT_SIZE] = true;
                }
            }
            fragmentedSize += memoryChunk.size;
        }

        StringBuilder allocatedDiagram = new StringBuilder();
        allocatedDiagram.append("[");
        for (int i = 0; i < SLOT_COUNT; i++) {
            allocatedDiagram.append(allocated[i] ? "X" : " ");
        }
        allocatedDiagram.append("]");

        StringBuilder fragmentedDiagram = new StringBuilder();
        fragmentedDiagram.append("[");
        for (int i = 0; i < SLOT_COUNT; i++) {
            fragmentedDiagram.append(fragmented[i] ? "X" : " ");
        }
        fragmentedDiagram.append("]");

        DumpDebugState.log("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        DumpDebugState.log(String.format("Allocated memory:  %08X %d bytes", allocatedSize, allocatedSize));
        DumpDebugState.log(allocatedDiagram.toString());
        DumpDebugState.log(String.format("Fragmented memory: %08X %d bytes", fragmentedSize, fragmentedSize));
        DumpDebugState.log(fragmentedDiagram.toString());

        DumpDebugState.log("Free list: " + getDebugFreeMem());
        DumpDebugState.log("Allocated blocks:\n" + getDebugAllocatedMem() + "\n");
    }

    public String hleKernelSprintf(CpuState cpu, String format, Object[] formatParameters) {
    	String formattedMsg = format;
		// Translate from the C-like format string to a Java format string:
		// - %u or %i -> %d
		// - %4u -> %4d
		// - %lld or %ld -> %d
		// - %llx or %lx -> %x
		// - %p -> %08X
		String javaMsg = format;
		javaMsg = javaMsg.replaceAll("%(\\d*)l?l?[uid]", "%$1d");
		javaMsg = javaMsg.replaceAll("%(\\d*)l?l?([xX])", "%$1$2");
		javaMsg = javaMsg.replaceAll("%p", "%08X");

		// Support for "%s" (at any place and can occur multiple times)
		int index = -1;
		for (int parameterIndex = 0; parameterIndex < formatParameters.length; parameterIndex++) {
			index = javaMsg.indexOf('%', index + 1);
			if (index < 0) {
				break;
			}
			String parameterFormat = javaMsg.substring(index);
			// Matching "%s" with optional width?
			if (parameterFormat.matches("%-?\\d*s.*")) {
				// Convert an integer address to a String by reading
				// the String at the given address
				int address = ((Integer) formatParameters[parameterIndex]).intValue();
				if (address == 0) {
					formatParameters[parameterIndex] = "(null)";
				} else {
					formatParameters[parameterIndex] = Utilities.readStringZ(address);
				}
			}
		}

		try {
    		// String.format: If there are more arguments than format specifiers, the extra arguments are ignored.
    		formattedMsg = String.format(javaMsg, formatParameters);
    	} catch (IllegalFormatException e) {
    		// Ignore formatting exception
    	}

    	return formattedMsg;
    }

    public String hleKernelSprintf(CpuState cpu, String format, int firstRegister) {
    	// For now, use only the 7 register parameters: $a1-$a3, $t0-$t3
    	// Further parameters are retrieved from the stack (assume max. 10 stack parameters).
		int registerParameters = _t3 - firstRegister + 1;
		Object[] formatParameters = new Object[registerParameters + 10];
		for (int i = 0; i < registerParameters; i++) {
			formatParameters[i] = cpu.getRegister(firstRegister + i);
		}
		Memory mem = Memory.getInstance();
		for (int i = registerParameters; i < formatParameters.length; i++) {
			formatParameters[i] = mem.read32(cpu._sp + ((i - registerParameters) << 2));
		}

		return hleKernelSprintf(cpu, format, formatParameters);
    }

    public int hleKernelPrintf(CpuState cpu, PspString formatString, Logger logger) {
        // Format and print the message to the logger
        if (logger.isInfoEnabled()) {
        	String formattedMsg = hleKernelSprintf(cpu, formatString.getString(), _a1);
        	logger.info(formattedMsg);
        }

        return 0;
    }

	public int hleKernelGetCompiledSdkVersion() {
		return compiledSdkVersion;
	}

	public void hleSetCompiledSdkVersion(int sdkVersion) {
		compiledSdkVersion = sdkVersion;
	}

	public int hleKernelGetCompilerVersion() {
		return compilerVersion;
	}

    @HLEFunction(nid = 0xA291F107, version = 150)
    public int sceKernelMaxFreeMemSize() {
		int maxFreeMemSize = maxFreeMemSize(USER_PARTITION_ID);

        // Some games expect size to be rounded down in 16 bytes block
        maxFreeMemSize &= ~15;

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelMaxFreeMemSize returning %d(hex=0x%1$X)", maxFreeMemSize));
    	}

    	return maxFreeMemSize;
	}

	@HLEFunction(nid = 0xF919F628, version = 150)
	public int sceKernelTotalFreeMemSize() {
		int totalFreeMemSize = totalFreeMemSize(USER_PARTITION_ID);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelTotalFreeMemSize returning %d(hex=0x%1$X)", totalFreeMemSize));
    	}

    	return totalFreeMemSize;
	}

	@HLEFunction(nid = 0x237DBD4F, version = 150)
	@HLEFunction(nid = 0x7158CE7E, version = 660)
	public int sceKernelAllocPartitionMemory(int partitionid, String name, int type, int size, int addr) {
        addr &= Memory.addressMask;

        if (type < PSP_SMEM_Low || type > PSP_SMEM_HighAligned) {
            return SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMBLOCK_ALLOC_TYPE;
        }

        SysMemInfo info = malloc(partitionid, name, type, size, addr);
        if (info == null) {
        	return SceKernelErrors.ERROR_KERNEL_FAILED_ALLOC_MEMBLOCK;
        }

        return info.uid;
	}

	@HLEFunction(nid = 0xB6D61D02, version = 150)
	@HLEFunction(nid = 0xC1A26C6F, version = 660)
	public int sceKernelFreePartitionMemory(int uid) {
		SceUidManager.checkUidPurpose(uid, "SysMem", true);

		SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            log.warn(String.format("sceKernelFreePartitionMemory unknown uid=0x%X", uid));
            return SceKernelErrors.ERROR_KERNEL_ILLEGAL_CHUNK_ID;
        }

        free(info);

        return 0;
	}

	@HLEFunction(nid = 0x9D9A5BA1, version = 150)
	@HLEFunction(nid = 0xF12A62F7, version = 660)
	public int sceKernelGetBlockHeadAddr(int uid) {
		SceUidManager.checkUidPurpose(uid, "SysMem", true);

		SysMemInfo info = blockList.get(uid);
        if (info == null) {
            log.warn(String.format("sceKernelGetBlockHeadAddr unknown uid=0x%X", uid));
            return SceKernelErrors.ERROR_KERNEL_ILLEGAL_CHUNK_ID;
        }

        return info.addr;
	}

	@HLEFunction(nid = 0x13A5ABEF, version = 150)
	public int sceKernelPrintf(CpuState cpu, PspString formatString) {
		return hleKernelPrintf(cpu, formatString, stdout);
	}

	@HLEFunction(nid = 0x3FC9AE6A, version = 150)
	@HLEFunction(nid = 0xC886B169, version = 660)
	public int sceKernelDevkitVersion() {
		int major = firmwareVersion / 100;
        int minor = (firmwareVersion / 10) % 10;
        int revision = firmwareVersion % 10;
        int devkitVersion = (major << 24) | (minor << 16) | (revision << 8) | 0x10;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelDevkitVersion returning 0x%08X", devkitVersion));
        }

        return devkitVersion;
	}

	@HLEFunction(nid = 0xD8DE5C1E, version = 150)
	public int SysMemUserForUser_D8DE5C1E() {
		// Seems to always return 0...
		return 0;
	}

	@HLEFunction(nid = 0xFC114573, version = 200)
    @HLEFunction(nid = 0xB4F00CB5, version = 660)
	public int sceKernelGetCompiledSdkVersion() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernelGetCompiledSdkVersion returning 0x%08X", compiledSdkVersion));
		}
		return compiledSdkVersion;
	}

	@HLEFunction(nid = 0x7591C7DB, version = 200)
	@HLEFunction(nid = 0x342061E5, version = 370)
	@HLEFunction(nid = 0x315AD3A0, version = 380)
	@HLEFunction(nid = 0xEBD5C3E6, version = 395)
	@HLEFunction(nid = 0x91DE343C, version = 500)
	@HLEFunction(nid = 0x7893F79A, version = 507)
	@HLEFunction(nid = 0x35669D4C, version = 600)
	@HLEFunction(nid = 0x1B4217BC, version = 603)
	@HLEFunction(nid = 0x358CA1BB, version = 606)
	public int sceKernelSetCompiledSdkVersion(int sdkVersion) {
        hleSetCompiledSdkVersion(sdkVersion);

        return 0;
	}

	@HLEFunction(nid = 0xF77D77CB, version = 200)
	public int sceKernelSetCompilerVersion(int compilerVersion) {
        this.compilerVersion = compilerVersion;

        return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA6848DF8, version = 200)
	public int SysMemUserForUser_A6848DF8() {
		return 0;
	}

	@HLEFunction(nid = 0x2A3E5280, version = 280)
	public int sceKernelQueryMemoryInfo(int address, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 partitionId, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 memoryBlockId) {
		int result = SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;

		for (Integer key : blockList.keySet()) {
			SysMemInfo info = blockList.get(key);
			if (info != null && info.addr <= address && address < info.addr + info.size) {
				partitionId.setValue(info.partitionid);
				memoryBlockId.setValue(info.uid);
				result = 0;
				break;
			}
		}

		return result;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x39F49610, version = 280)
	public int sceKernelGetPTRIG() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6231A71D, version = 280)
	public int sceKernelSetPTRIG() {
		return 0;
	}

	// sceKernelFreeMemoryBlock (internal name)
	@HLEFunction(nid = 0x50F61D8A, version = 352)
	public int SysMemUserForUser_50F61D8A(int uid) {
		SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            log.warn("SysMemUserForUser_50F61D8A(uid=0x" + Integer.toHexString(uid) + ") unknown uid");
            return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
        }

        free(info);

        return 0;
	}

	@HLEFunction(nid = 0xACBD88CA, version = 352)
	public int sceKernelTotalMemSize() {
		return MemoryMap.SIZE_RAM;
	}

    // sceKernelGetMemoryBlockAddr (internal name)
	@HLEFunction(nid = 0xDB83A952, version = 352)
	public int SysMemUserForUser_DB83A952(int uid, TPointer32 addr) {
		SysMemInfo info = blockList.get(uid);
        if (info == null) {
            log.warn(String.format("SysMemUserForUser_DB83A952 uid=0x%X, addr=%s: unknown uid", uid, addr));
            return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
        }

        addr.setValue(info.addr);

        return 0;
	}

	// sceKernelAllocMemoryBlock (internal name)
	@HLEFunction(nid = 0xFE707FDF, version = 352)
	public int SysMemUserForUser_FE707FDF(@StringInfo(maxLength=32) PspString name, int type, int size, @CanBeNull TPointer paramsAddr) {
        if (paramsAddr.isNotNull()) {
        	int length = paramsAddr.getValue32();
        	if (length != 4) {
        		log.warn(String.format("SysMemUserForUser_FE707FDF: unknown parameters with length=%d", length));
        	}
        }

        if (type < PSP_SMEM_Low || type > PSP_SMEM_High) {
            return SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMBLOCK_ALLOC_TYPE;
        }

        // Always allocate memory in user area (partitionid == 2).
        SysMemInfo info = malloc(SysMemUserForUser.USER_PARTITION_ID, name.getString(), type, size, 0);
        if (info == null) {
        	return SceKernelErrors.ERROR_KERNEL_FAILED_ALLOC_MEMBLOCK;
        }

        return info.uid;
	}
}