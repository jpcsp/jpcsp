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
package jpcsp;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.memory.DebuggerMemory;
import jpcsp.memory.DirectBufferMemory;
import jpcsp.memory.FastMemory;
import jpcsp.memory.SafeDirectBufferMemory;
import jpcsp.memory.SafeFastMemory;
import jpcsp.memory.StandardMemory;

import org.apache.log4j.Logger;

public abstract class Memory {
    public static Logger log = Logger.getLogger("memory");
    private static Memory instance = null;
    public static boolean useDirectBufferMemory = false;
    public static boolean useSafeMemory = true;
    public static boolean useDebuggerMemory = false;
	public static final int addressMask = 0x3FFFFFFF;
	private boolean ignoreInvalidMemoryAccess = false;
    protected static final int MEMORY_PAGE_SHIFT = 12;
    protected static boolean[] validMemoryPage = new boolean[0x00100000];

    public static Memory getInstance() {
        if (instance == null) {
        	//
        	// The following memory implementations are available:
        	// - StandardMemory        :  low memory requirements, performs address checking, slow
        	// - SafeFastMemory        : high memory requirements, performs address checking, fast
        	// - FastMemory            : high memory requirements, no address checking, very fast
        	// - SafeDirectBufferMemory: high memory requirements, performs address checking, moderate
        	// - DirectBufferMemory    : high memory requirements, no address checking, fast
        	//
        	// Best choices are currently
        	// 1) SafeFastMemory (address check is useful when debugging programs)
        	// 2) StandardMemory when available memory is not sufficient for 1st choice
        	//

        	// Disable address checking when the option
        	// "ignoring invalid memory access" is selected.
        	if (Settings.getInstance().readBool("emu.ignoreInvalidMemoryAccess")) {
        		useSafeMemory = false;
        	}

        	if (useDirectBufferMemory) {
        		if (useSafeMemory) {
        			instance = new SafeDirectBufferMemory();
        		} else {
        			instance = new DirectBufferMemory();
        		}
        	} else {
        		if (useSafeMemory) {
	        		instance = new SafeFastMemory();
	        	} else {
	        		instance = new FastMemory();
	        	}
        	}

        	if (instance != null) {
        		if (!instance.allocate()) {
        			instance = null;
        		}
        	}

        	if (instance == null) {
        		instance = new StandardMemory();
        		if (!instance.allocate()) {
        			instance = null;
        		}
        	}

        	if (instance == null) {
    			throw new OutOfMemoryError("Cannot allocate memory");
        	}

        	if (useDebuggerMemory || new File(DebuggerMemory.mBrkFilePath).exists()) {
        		DebuggerMemory.install();
        	}

        	log.debug("Using " + instance.getClass().getName());
        }

        return instance;
    }

    public static void setInstance(Memory mem) {
    	instance = mem;
    }

    public void invalidMemoryAddress(int address, String prefix, int status) {
	    String message = String.format("%s - Invalid memory address : 0x%X PC=%08X",
	                                   prefix,
	                                   address,
	                                   Emulator.getProcessor().cpu.pc);

	    if (ignoreInvalidMemoryAccess) {
	        Memory.log.warn("IGNORED: " + message);
	    } else {
	        Memory.log.error(message);
	        Emulator.PauseEmuWithStatus(status);
	    }
	}

	public boolean read32AllowedInvalidAddress(int address) {
        //
        // Ugly hack for programs using pspsdk :-(
        //
        // The function pspSdkInstallNoPlainModuleCheckPatch()
        // is trying to patch 2 psp modules and is expecting to have
        // the module stub implemented as a Jump instruction,
        // something like:
        //          [08XXXXXX]: j YYYYYYYY        // YYYYYYYY = XXXXXX << 2
        //          [00000000]: nop
        //
        // Jpcsp is however based on the following code sequence, e.g.:
        //          [03E00008]: jr $ra
        //          [00081B4C]: syscall 0x0206D
        //
        // The function pspSdkInstallNoPlainModuleCheckPatch()
        // is retrieving the address of the Jump instruction and reading
        // from it in kernel mode.
        // On jpcsp, it is thus trying to read at the following address
        //          0x8f800020 = (0x03E00008 << 2) || 0x80000000
        // up to    0x8f8001ac
        //
        // The hack here is to allow these memory reads and returns 0.
        //
        // Here is the C code from pspsdk:
        //
        //          int pspSdkInstallNoPlainModuleCheckPatch(void)
        //          {
        //              u32 *addr;
        //              int i;
        //
        //              addr = (u32*) (0x80000000 | ((sceKernelProbeExecutableObject & 0x03FFFFFF) << 2));
        //              //printf("sceKernelProbeExecutableObject %p\n", addr);
        //              for(i = 0; i < 100; i++)
        //              {
        //                  if((addr[i] & 0xFFE0FFFF) == LOAD_EXEC_PLAIN_CHECK)
        //                  {
        //                      //printf("Found instruction %p\n", &addr[i]);
        //                      addr[i] = (LOAD_EXEC_PLAIN_PATCH | (addr[i] & ~0xFFE0FFFF));
        //                  }
        //              }
        //
        //              addr = (u32*) (0x80000000 | ((sceKernelCheckPspConfig & 0x03FFFFFF) << 2));
        //              //printf("sceCheckPspConfig %p\n", addr);
        //              for(i = 0; i < 100; i++)
        //              {
        //                  if((addr[i] & 0xFFE0FFFF) == LOAD_EXEC_PLAIN_CHECK)
        //                  {
        //                      //printf("Found instruction %p\n", &addr[i]);
        //                      addr[i] = (LOAD_EXEC_PLAIN_PATCH | (addr[i] & ~0xFFE0FFFF));
        //                  }
        //              }
        //
        //              sceKernelDcacheWritebackAll();
        //
        //              return 0;
        //          }
        //
        if ((address >= 0x8f800020 && address <= 0x8f8001ac) ||
        	(address >= 0x0f800020 && address <= 0x0f8001ac)) { // Accept also masked address
        	Memory.log.debug("read32 - ignoring pspSdkInstallNoPlainModuleCheckPatch");
            return true;
        }

        return false;
	}

	public abstract void Initialise();
    public abstract int read8(int address);
    public abstract int read16(int address);
    public abstract int read32(int address);
    public abstract void write8(int address, byte data);
    public abstract void write16(int address, short data);
    public abstract void write32(int address, int data);
    public abstract void memset(int address, byte data, int length);
    public abstract Buffer getMainMemoryByteBuffer();
    public abstract Buffer getBuffer(int address, int length);
    public abstract void copyToMemory(int address, ByteBuffer source, int length);
    protected abstract void memcpy(int destination, int source, int length, boolean checkOverlap);
    
    public int strlen(int address) {
    	int len = 0;
    	while (read8(address) != 0) { len++; address++; }
    	return len;
    }

	public final static boolean isAddressGood(int address) {
    	return validMemoryPage[address >>> MEMORY_PAGE_SHIFT];
	}

    public final static boolean isRawAddressGood(int rawAddress) {
    	return validMemoryPage[rawAddress >> MEMORY_PAGE_SHIFT];
    }

    public boolean allocate() {
    	for (int i = 0; i < validMemoryPage.length; i++) {
    		int address = normalizeAddress(i << MEMORY_PAGE_SHIFT);

    		boolean isValid = false;
            if (address >= MemoryMap.START_RAM && address <= MemoryMap.END_RAM) {
                isValid = true;
            } else if (address >= MemoryMap.START_VRAM && address <= MemoryMap.END_VRAM) {
                isValid = true;
            } else if (address >= MemoryMap.START_SCRATCHPAD && address <= MemoryMap.END_SCRATCHPAD) {
                isValid = true;
            }

            validMemoryPage[i] = isValid;
    	}

    	return true;
    }

	public long read64(int address) {
		long low = read32(address);
		long high = read32(address + 4);
		return low | high << 32;
	}

	public void write64(int address, long data) {
		write32(address, (int) data);
		write32(address + 4, (int) (data >> 32));
	}
	
	public ByteBuffer readChunkZ(int address) {
		return readChunk(address, strlen(address));
	}
	
	public ByteBuffer readChunk(int address, int size) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(size);
		for (int n = 0; n < size; n++) {
			byteBuffer.put((byte)read8(address + n));
		}
		return byteBuffer;
	}

    // memcpy does not check overlapping source and destination areas
    public void memcpy(int destination, int source, int length) {
    	memcpy(destination, source, length, false);
    }

    // memmove reproduces the bytes correctly at destination even if the two areas overlap
    public void memmove(int destination, int source, int length) {
    	memcpy(destination, source, length, true);
    }

    public int normalizeAddress(int address) {
    	address = address & addressMask;

    	// Test on a PSP: 0x4200000 is equivalent to 0x4000000
    	if ((address & 0xFF000000) == MemoryMap.START_VRAM) {
    	    address &= 0xFF1FFFFF;
    	}

    	return address;
    }

    protected boolean areOverlapping(int destination, int source, int length) {
    	if (source + length <= destination || destination + length <= source) {
    		return false;
    	}

    	return true;
    }

    public void load(ByteBuffer buffer) {
    }

    public void save(ByteBuffer buffer) {
    }

    public boolean isIgnoreInvalidMemoryAccess() {
        return ignoreInvalidMemoryAccess;
    }

    public void setIgnoreInvalidMemoryAccess(boolean ignoreInvalidMemoryAccess) {
        this.ignoreInvalidMemoryAccess = ignoreInvalidMemoryAccess;
        Memory.log.info("Ignore invalid memory access: " + ignoreInvalidMemoryAccess);
    }
}