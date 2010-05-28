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

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.managers.*;
import jpcsp.HLE.kernel.types.SceKernelErrors;

public class pspSysMem {
    private static pspSysMem instance;
    private static Logger stdout = Logger.getLogger("stdout");
    private static HashMap<Integer, SysMemInfo> blockList;
    private static MemoryChunkList freeMemoryChunks;
    private int firmwareVersion = 150;
    private boolean disableReservedThreadMemory = false;
    private static final int defaultSizeAlignment = 256;
    // PspSysMemBlockTypes
    public static final int PSP_SMEM_Low = 0;
    public static final int PSP_SMEM_High = 1;
    public static final int PSP_SMEM_Addr = 2;
    public static final int PSP_SMEM_LowAligned = 3;
    public static final int PSP_SMEM_HighAligned = 4;

    private pspSysMem() {
    }

    public static pspSysMem getInstance() {
        if (instance == null) {
            instance = new pspSysMem();
        }
        return instance;
    }

    /** @param firmwareVersion : in this format: ABB, where A = major and B = minor, for example 271 */
    public void Initialise(int firmwareVersion) {
        blockList = new HashMap<Integer, SysMemInfo>();

        int startFreeMem = MemoryMap.START_USERSPACE;
        int endFreeMem = MemoryMap.END_USERSPACE;
        MemoryChunk initialMemory = new MemoryChunk(startFreeMem, endFreeMem - startFreeMem + 1);
        freeMemoryChunks = new MemoryChunkList(initialMemory);

        setFirmwareVersion(firmwareVersion);
    }

    // This compatibility settings should now be obsolete
    // TODO Delete the "DisableReservedThreadMemory" compatibility setting
    public void setDisableReservedThreadMemory(boolean disableReservedThreadMemory) {
        this.disableReservedThreadMemory = disableReservedThreadMemory;
        Modules.log.info("Disable reserved thread memory: " + disableReservedThreadMemory);
        if (!disableReservedThreadMemory) {
        	Modules.log.info("Please inform us if this application is really running better when unchecking the option 'Disable reserved thread memory' (all other settings unchanged)");
        }
    }

    // Allocates to 256-byte alignment
    // TODO use the partitionid
    public int malloc(int partitionid, int type, int size, int addr) {
        int allocatedAddress = 0;

        int alignment = defaultSizeAlignment - 1;
        if (type == PSP_SMEM_LowAligned || type == PSP_SMEM_HighAligned) {
            // Use the alignment provided in the addr parameter
            alignment = addr - 1;
        }
        size = (size + alignment) & ~alignment;

        switch (type) {
        	case PSP_SMEM_Low:
        	case PSP_SMEM_LowAligned:
        		for (MemoryChunk memoryChunk = freeMemoryChunks.low; memoryChunk != null; memoryChunk = memoryChunk.next) {
        			if (memoryChunk.size >= size) {
        				allocatedAddress = freeMemoryChunks.allocLow(memoryChunk, size);
        				break;
        			}
        		}
        		break;
        	case PSP_SMEM_High:
        	case PSP_SMEM_HighAligned:
        		for (MemoryChunk memoryChunk = freeMemoryChunks.high; memoryChunk != null; memoryChunk = memoryChunk.previous) {
        			if (memoryChunk.size >= size) {
        				allocatedAddress = freeMemoryChunks.allocHigh(memoryChunk, size);
        				break;
        			}
        		}
        		break;
        	case PSP_SMEM_Addr:
        		for (MemoryChunk memoryChunk = freeMemoryChunks.low; memoryChunk != null; memoryChunk = memoryChunk.next) {
        			if (memoryChunk.addr <= addr && addr < memoryChunk.addr + memoryChunk.size) {
        				// The address is located in this MemoryChunk
        				allocatedAddress = freeMemoryChunks.allocInside(memoryChunk, addr, size);
        			}
        		}
        		break;
    		default:
    			Modules.log.warn(String.format("malloc: unknown type %s", getTypeName(type)));
        }

		if (allocatedAddress == 0) {
            Modules.log.warn(String.format("malloc cannot allocate partition=%d, type=%s, size=0x%X, addr=0x%08X", partitionid, getTypeName(type), size, addr));
		}

		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("malloc partition=%d, type=%s, size=0x%X, addr=0x%08X: returns 0x%08X", partitionid, getTypeName(0), size, addr, allocatedAddress));
			if (Modules.log.isTraceEnabled()) {
				Modules.log.trace("Free list after malloc: " + freeMemoryChunks);
			}
		}

		return allocatedAddress;
    }

    public int addSysMemInfo(int partitionid, String name, int type, int size, int addr) {
        SysMemInfo info = new SysMemInfo(partitionid, name, type, size, addr);

        return info.uid;
    }

    /**
     * For internal use, example: ThreadMan allocating stack space
     * Also removes the associated SysMemInfo (if found) from blockList
     * @param uidFastPath can be <= 0 to disable the fast path
     * @param memAddress slower, but due to low cohesion or "fake" allocations
     * sometimes necessary
     */
    public void free(int uidFastPath, int memAddress) {
        SysMemInfo info = null;
        if (uidFastPath > 0) {
            info = blockList.remove(uidFastPath);
        } else {
            for (Iterator<SysMemInfo> it = blockList.values().iterator(); it.hasNext();) {
                SysMemInfo i = it.next();
                if (i.addr == memAddress) {
                    it.remove();
                    info = i;
                    break;
                }
            }
        }

        if (info == null) {
            // HLE modules using malloc should also call addSysMemInfo
            Modules.log.warn("pspSysMem.free(addr) failed to find SysMemInfo with uid:" + uidFastPath);
        } else {
            free(info);
        }
    }

    private void free(SysMemInfo info) {
    	MemoryChunk memoryChunk = new MemoryChunk(info.addr, info.size);
    	freeMemoryChunks.add(memoryChunk);

    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("free " + info);
    		if (Modules.log.isTraceEnabled()) {
    			Modules.log.trace("Free list after free: " + freeMemoryChunks.toString());
    		}
    	}
    }

    public int maxFreeMemSize() {
    	int maxFreeMemSize = 0;
    	for (MemoryChunk memoryChunk = freeMemoryChunks.low; memoryChunk != null; memoryChunk = memoryChunk.next) {
        	// Since some apps try and allocate the value of sceKernelMaxFreeMemSize,
            // which will leave no space for stacks we're going to reserve 0x09f00000
            // to 0x09ffffff for stacks, but stacks are allowed to go below that
            // (if there's free space of course).
    		if (!disableReservedThreadMemory) {
    			final int heapTopGuard = 0x09f00000;
    			if (memoryChunk.addr >= heapTopGuard) {
    				break;
    			} else if (memoryChunk.addr + memoryChunk.size > heapTopGuard) {
    				int sizeToTopGuard = heapTopGuard - memoryChunk.addr;
    				if (sizeToTopGuard > maxFreeMemSize) {
    					maxFreeMemSize = sizeToTopGuard;
    				}
    				break;
    			}
    		}

    		if (memoryChunk.size > maxFreeMemSize) {
    			maxFreeMemSize = memoryChunk.size;
    		}
    	}

		return maxFreeMemSize;
    }

    /** @return the size of the largest allocatable block */
    public void sceKernelMaxFreeMemSize() {
        int maxFreeMemSize = maxFreeMemSize();

        // Some games expect size to be rounded down in 16 bytes block
        maxFreeMemSize &= ~15;

    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug(String.format("sceKernelMaxFreeMemSize %d(hex=0x%1$X)", maxFreeMemSize));
    	}
        Emulator.getProcessor().cpu.gpr[2] = maxFreeMemSize;
    }

    public int totalFreeMemSize() {
        int totalFreeMemSize = 0;
    	for (MemoryChunk memoryChunk = freeMemoryChunks.low; memoryChunk != null; memoryChunk = memoryChunk.next) {
    		totalFreeMemSize += memoryChunk.size;
    	}

    	return totalFreeMemSize;
    }

    public void sceKernelTotalFreeMemSize() {
    	int totalFreeMemSize = totalFreeMemSize();

    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug(String.format("sceKernelTotalFreeMemSize %d(hex=0x%1$X)", totalFreeMemSize));
    	}
        Emulator.getProcessor().cpu.gpr[2] = totalFreeMemSize;
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
    public void sceKernelAllocPartitionMemory(int partitionid, int pname, int type, int size, int addr) {
        addr &= Memory.addressMask;
        String name = readStringZ(pname);

        if (Modules.log.isDebugEnabled()) {
	        Modules.log.debug(String.format("sceKernelAllocPartitionMemory(partition=%d, name='%s', type=%s, size=0x%X, addr=0x%08X", partitionid, name, getTypeName(type), size, addr));
        }

        if (type < PSP_SMEM_Low || type > PSP_SMEM_HighAligned) {
            Emulator.getProcessor().cpu.gpr[2] = SceKernelErrors.ERROR_ILLEGAL_MEMBLOCK_ALLOC_TYPE;
        } else {
            addr = malloc(partitionid, type, size, addr);
            if (addr != 0) {
                SysMemInfo info = new SysMemInfo(partitionid, name, type, size, addr);
                Emulator.getProcessor().cpu.gpr[2] = info.uid;
            } else {
                Emulator.getProcessor().cpu.gpr[2] = SceKernelErrors.ERROR_FAILED_ALLOC_MEMBLOCK;
            }
        }
    }

    public void sceKernelFreePartitionMemory(int uid) throws GeneralJpcspException {
        SceUidManager.checkUidPurpose(uid, "SysMem", true);
        SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelFreePartitionMemory unknown SceUID=" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = SceKernelErrors.ERROR_ILLEGAL_CHUNK_ID;
        } else {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelFreePartitionMemory SceUID=" + Integer.toHexString(info.uid) + " name:'" + info.name + "'");
        	}
            free(info);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelGetBlockHeadAddr(int uid) throws GeneralJpcspException {
        SceUidManager.checkUidPurpose(uid, "SysMem", true);
        SysMemInfo info = blockList.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelGetBlockHeadAddr unknown SceUID=" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = SceKernelErrors.ERROR_ILLEGAL_CHUNK_ID;
        } else {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelGetBlockHeadAddr SceUID=" + Integer.toHexString(info.uid) + " name:'" + info.name + "' headAddr:" + Integer.toHexString(info.addr));
        	}
            Emulator.getProcessor().cpu.gpr[2] = info.addr;
        }
    }

    // 352+
    // Create
    public void SysMemUserForUser_FE707FDF(int name_addr, int unk2, int size, int unk4) {
        String name = readStringNZ(name_addr, 32);
        String msg = "SysMemUserForUser_FE707FDF(name='" + name
                + "',unk2=" + unk2
                + ",size=" + size
                + ",unk4=" + unk4 + ")";

        // 256 byte aligned
        size = (size + 0xFF) & ~0xFF;

        int addr = malloc(2, PSP_SMEM_Low, size, 0);
        if (addr != 0) {
            SysMemInfo info = new SysMemInfo(2, name, PSP_SMEM_Low, size, addr);

            msg += " allocated uid " + Integer.toHexString(info.uid);
            if (unk2 == 0 && unk4 == 0) {
                Modules.log.debug(msg);
            } else {
                Modules.log.warn("PARTIAL:" + msg + " unimplemented parameters");
            }

            Emulator.getProcessor().cpu.gpr[2] = info.uid;
        } else {
            Modules.log.warn(msg + " failed");
            Emulator.getProcessor().cpu.gpr[2] = SceKernelErrors.ERROR_FAILED_ALLOC_MEMBLOCK;
        }
    }

    // 352+
    // Delete
    public void SysMemUserForUser_50F61D8A(int uid) {
        SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            Modules.log.warn("SysMemUserForUser_50F61D8A(uid=0x" + Integer.toHexString(uid) + ") unknown uid");
            Emulator.getProcessor().cpu.gpr[2] = 0x800200cb; // unknown uid
        } else {
            Modules.log.debug("SysMemUserForUser_50F61D8A(uid=0x" + Integer.toHexString(uid) + ")");
            free(info);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    // 352+
    // Get
    public void SysMemUserForUser_DB83A952(int uid, int addr) {
        SysMemInfo info = blockList.get(uid);
        if (info == null) {
            Modules.log.warn("SysMemUserForUser_DB83A952(uid=0x" + Integer.toHexString(uid)
                    + ",addr=0x" + Integer.toHexString(addr) + ") unknown uid");
        } else {
            Modules.log.debug("SysMemUserForUser_DB83A952(uid=0x" + Integer.toHexString(uid)
                    + ",addr=0x" + Integer.toHexString(addr) + ") addr 0x" + Integer.toHexString(info.addr));
            jpcsp.Memory.getInstance().write32(addr, info.addr);
        }
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    /** TODO implement format string parsing and reading variable number of parameters */
    public void sceKernelPrintf(int string_addr) {
        String msg = readStringNZ(string_addr, 256);
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelPrintf(string_addr=0x%08X) '%s'", string_addr, msg));
        }

        // Format and print the message to stdout
        if (stdout.isInfoEnabled()) {
        	String formattedMsg = msg;
        	try {
        		// Translate the C-like format string to a Java format string:
        		// - %u or %i -> %d
        		// - %p -> %08X
        		String javaMsg = msg;
        		javaMsg = javaMsg.replaceAll("\\%[ui]", "%d");
        		javaMsg = javaMsg.replaceAll("\\%p", "%08X");

        		int[] gpr = Emulator.getProcessor().cpu.gpr;
            	// For now, use only the 7 register parameters: $a1-$a3, $t0-$t3
            	// Further parameters should be retrieved from the stack.
            	// String.format: If there are more arguments than format specifiers, the extra arguments are ignored.
        		formattedMsg = String.format(javaMsg, gpr[5], gpr[6], gpr[7], gpr[8], gpr[9], gpr[10], gpr[11]);
        	} catch (Exception e) {
        		// Ignore formatting exception
        	}
        	stdout.info(formattedMsg);
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    /** @param firmwareVersion : in this format: ABB, where A = major and B = minor, for example 271 */
    public void setFirmwareVersion(int firmwareVersion) {
    	this.firmwareVersion = firmwareVersion;
    }

    public void sceKernelDevkitVersion() {
        int major = firmwareVersion / 100;
        int minor = (firmwareVersion / 10) % 10;
        int revision = firmwareVersion % 10;
        int devkitVersion = (major << 24) | (minor << 16) | (revision << 8) | 0x10;
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelDevkitVersion return:0x%08X", devkitVersion));
        }
        Emulator.getProcessor().cpu.gpr[2] = devkitVersion;
    }

    /** 3.52+ */
    public void sceKernelGetModel() {
        int result = 0; // <= 0 original, 1 slim

        if (firmwareVersion < 352) {
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

        for (Iterator<SysMemInfo> it = blockList.values().iterator(); it.hasNext();) {
            SysMemInfo info = it.next();
            for (int i = info.addr; i < info.addr + info.size; i += SLOT_SIZE) {
                if (i >= 0x08800000 && i < 0x0A000000) {
                    allocated[(i - 0x08800000) / SLOT_SIZE] = true;
                }
            }
            allocatedSize += info.size;
        }

        for (MemoryChunk memoryChunk = freeMemoryChunks.low; memoryChunk != null; memoryChunk = memoryChunk.next) {
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

        System.err.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.err.println(String.format("Allocated memory:  %08X %d bytes", allocatedSize, allocatedSize));
        System.err.println(allocatedDiagram);
        System.err.println(String.format("Fragmented memory: %08X %d bytes", fragmentedSize, fragmentedSize));
        System.err.println(fragmentedDiagram);
    }

    static class SysMemInfo implements Comparable<SysMemInfo> {

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
        public String toString() {
            return String.format("SysMemInfo[uid=%x, partition=%d, name='%s', type=%s, size=0x%X, addr=0x%08X]", uid, partitionid, name, getTypeName(type), size, addr);
        }

        @Override
        public int compareTo(SysMemInfo o) {
            //there are no equal adresses. Or at least there shouldn't be...
            if (addr == o.addr) {
                Modules.log.warn("Set invariant broken for SysMemInfo " + this);
                return 0;
            }
            return addr < o.addr ? -1 : 1;
        }
    }

    static class MemoryChunk {
    	// Start address of this MemoryChunk
    	public int addr;
    	// Size of this MemoryChunk: it extends from addr to (addr + size -1)
    	public int size;
    	// The MemoryChunk are kept sorted by addr and linked with next/previous
    	// The MemoryChunk with the lowest addr has previous == null
    	// The MemoryChunk with the highest addr has next == null
    	public MemoryChunk next;
    	public MemoryChunk previous;

    	public MemoryChunk(int addr, int size) {
    		this.addr = addr;
    		this.size = size;
    	}

		@Override
		public String toString() {
			return String.format("[addr=0x%08X-0x%08X, size=0x%X]", addr, addr + size, size);
		}
    }

    static class MemoryChunkList {
    	// The MemoryChunk objects are linked and kept sorted by address.
    	//
    	// low: MemoryChunk with the lowest address.
    	// Start point to scan list by increasing address
    	private MemoryChunk low;
    	// high: MemoryChunk with the highest address.
    	// Start point to scan the list by decreasing address
    	private MemoryChunk high;

    	public MemoryChunkList(MemoryChunk initialMemoryChunk) {
    		low = initialMemoryChunk;
    		high = initialMemoryChunk;
    	}

    	/**
    	 * Remove a MemoryChunk from the list.
    	 *
    	 * @param memoryChunk the MemoryChunk to be removed
    	 */
    	public void remove(MemoryChunk memoryChunk) {
    		if (memoryChunk.previous != null) {
    			memoryChunk.previous.next = memoryChunk.next;
    		}
    		if (memoryChunk.next != null) {
    			memoryChunk.next.previous = memoryChunk.previous;
    		}

    		if (low == memoryChunk) {
    			low = memoryChunk.next;
    		}
    		if (high == memoryChunk) {
    			high = memoryChunk.previous;
    		}
    	}

    	/**
    	 * Allocate a memory from the MemoryChunk, at its lowest address.
    	 * The MemoryChunk is updated accordingly or is removed if it stays empty.
    	 * 
    	 * @param memoryChunk the MemoryChunk where the memory should be allocated
    	 * @param size        the size of the memory to be allocated
    	 * @return            the base address of the allocated memory
    	 */
    	public int allocLow(MemoryChunk memoryChunk, int size) {
    		int allocatedAddr = memoryChunk.addr;

    		if (memoryChunk.size == size) {
				remove(memoryChunk);
			} else {
				memoryChunk.size -= size;
				memoryChunk.addr += size;
			}

			return allocatedAddr;
    	}

    	/**
    	 * Allocate a memory from the MemoryChunk, at its highest address.
    	 * The MemoryChunk is updated accordingly or is removed if it stays empty.
    	 * 
    	 * @param memoryChunk the MemoryChunk where the memory should be allocated
    	 * @param size        the size of the memory to be allocated
    	 * @return            the base address of the allocated memory
    	 */
    	public int allocHigh(MemoryChunk memoryChunk, int size) {
    		int allocatedAddr;

    		if (memoryChunk.size == size) {
				allocatedAddr = memoryChunk.addr;
				freeMemoryChunks.remove(memoryChunk);
			} else {
				memoryChunk.size -= size;
				allocatedAddr = memoryChunk.addr + memoryChunk.size;
			}

    		return allocatedAddr;
    	}

    	/**
    	 * Allocate a memory from the MemoryChunk, given the base address.
    	 * The base address must be inside the MemoryChunk
    	 * The MemoryChunk is updated accordingly, is removed if it stays empty or
    	 * is split into 2 remaining free parts.
    	 * 
    	 * @param memoryChunk the MemoryChunk where the memory should be allocated
    	 * @param addr        the base address of the memory to be allocated
    	 * @param size        the size of the memory to be allocated
    	 * @return            the base address of the allocated memory, or 0
    	 *                    if the MemoryChunk is too small to allocate the desired size.
    	 */
    	public int allocInside(MemoryChunk memoryChunk, int addr, int size) {
    		if (memoryChunk.addr == addr) {
    			// Allocate at the lowest address
    			return allocLow(memoryChunk, size);
    		} else if (memoryChunk.addr + memoryChunk.size == addr + size) {
    			// Allocate at the highest address
    			return allocHigh(memoryChunk, size);
    		} else if (memoryChunk.addr + memoryChunk.size < addr + size) {
    			// The MemoryChunk is too small to allocate the desired size
    			return 0;
    		} else {
    			// Allocate in the middle of a MemoryChunk: it must be split
    			// in 2 parts: one for lowest part and one for the highest part.
    			// Update memoryChunk to contain the lowest part,
    			// and create a new MemoryChunk to contain to highest part.
    			int lowSize = addr - memoryChunk.addr;
    			int highSize = memoryChunk.size - lowSize - size;
    			MemoryChunk highMemoryChunk = new MemoryChunk(addr + size, highSize);
    			memoryChunk.size = lowSize;

    			addAfter(highMemoryChunk, memoryChunk);
    		}

    		return addr;
    	}

    	/**
    	 * Add a new MemoryChunk after another one.
    	 * This method does not check if the addresses are kept ordered.
    	 * 
    	 * @param memoryChunk the MemoryChunk to be added
    	 * @param reference   memoryChunk has to be added after this reference
    	 */
    	private void addAfter(MemoryChunk memoryChunk, MemoryChunk reference) {
    		memoryChunk.previous = reference;
    		memoryChunk.next = reference.next;
    		reference.next = memoryChunk;
    		if (memoryChunk.next != null) {
    			memoryChunk.next.previous = memoryChunk;
    		}

    		if (high == reference) {
    			high = memoryChunk;
    		}
    	}

    	/**
    	 * Add a new MemoryChunk before another one.
    	 * This method does not check if the addresses are kept ordered.
    	 * 
    	 * @param memoryChunk the MemoryChunk to be added
    	 * @param reference   memoryChunk has to be added before this reference
    	 */
    	private void addBefore(MemoryChunk memoryChunk, MemoryChunk reference) {
    		memoryChunk.previous = reference.previous;
    		memoryChunk.next = reference;
    		reference.previous = memoryChunk;
    		if (memoryChunk.previous != null) {
    			memoryChunk.previous.next = memoryChunk;
    		}

    		if (low == reference) {
    			low = memoryChunk;
    		}
    	}

    	/**
    	 * Add a new MemoryChunk to the list. It is added in the list so that
    	 * the addresses are kept in increasing order.
    	 * The MemoryChunk might be merged into another adjacent MemoryChunk.
    	 * 
    	 * @param memoryChunk the MemoryChunk to be added
    	 */
    	public void add(MemoryChunk memoryChunk) {
    		// Scan the list to find the insertion point to keep the elements
    		// ordered by increasing address.
    		for (MemoryChunk scanChunk = low; scanChunk != null; scanChunk = scanChunk.next) {
    			// Merge the MemoryChunk if it is adjacent to other elements in the list
    			if (scanChunk.addr + scanChunk.size == memoryChunk.addr) {
    				// The MemoryChunk is adjacent at its lowest address,
    				// merge it into the previous one.
    				scanChunk.size += memoryChunk.size;

    				// Check if the gap to the next chunk has not been closed,
    				// in which case, we can also merge the next chunk.
    				MemoryChunk nextChunk = scanChunk.next;
    				if (nextChunk != null) {
    					if (scanChunk.addr + scanChunk.size == nextChunk.addr) {
    						// Merge with nextChunk
    						scanChunk.size += nextChunk.size;
    						remove(nextChunk);
    					}
    				}
    				return;
    			} else if (memoryChunk.addr + memoryChunk.size == scanChunk.addr) {
    				// The MemoryChunk is adjacent at its highest address,
    				// merge it into the next one.
    				scanChunk.addr = memoryChunk.addr;
    				scanChunk.size += memoryChunk.size;

    				// Check if the gap to the previous chunk has not been closed,
    				// in which case, we can also merge the previous chunk.
    				MemoryChunk previousChunk = scanChunk.previous;
    				if (previousChunk != null) {
    					if (previousChunk.addr + previousChunk.size == scanChunk.addr) {
    						// Merge with previousChunk
    						previousChunk.size += scanChunk.size;
    						remove(scanChunk);
    					}
    				}
    				return;
    			} else if (scanChunk.addr > memoryChunk.addr) {
    				// We have found the insertion point for the MemoryChunk,
    				// add it before this element to keep the addresses in
    				// increasing order.
    				addBefore(memoryChunk, scanChunk);
    				return;
    			}
    		}

    		// The MemoryChunk has not yet been added, add it at the very end
    		// of the list.
    		if (high == null && low == null) {
    			// The list is empty, add the element
    			high = memoryChunk;
    			low = memoryChunk;
    		} else {
    			addAfter(memoryChunk, high);
    		}
    	}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			for (MemoryChunk memoryChunk = low; memoryChunk != null; memoryChunk = memoryChunk.next) {
				if (result.length() > 0) {
					result.append(", ");
				}
				result.append(memoryChunk.toString());
			}

			return result.toString();
		}
    }

    static private String getTypeName(int type) {
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
}
