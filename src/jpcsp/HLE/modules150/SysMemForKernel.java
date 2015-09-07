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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class SysMemForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("SysMemForKernel");

    @Override
    public String getName() {
        return "SysMemForKernel";
    }

    @HLEFunction(nid = 0xA089ECA4, version = 150)
    public int sceKernelMemset(TPointer destAddr, int data, int size) {
        destAddr.memset((byte) data, size);

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
    @HLEUnimplemented
    @HLEFunction(nid = 0x1C1FBFE7, version = 150)
    public int sceKernelCreateHeap(int partitionId, int size, int flags, String name) {
    	return 0;
    }

    /**
     * Allocate a memory block from a heap.
     * 
     * @param heapId The UID of the heap to allocate from.
     * @param size   The number of bytes to allocate.
     * @return       The address of the allocated memory block, or NULL on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x636C953B, version = 150)
    public int sceKernelAllocHeapMemory(int heapId, int size) {
        return 0;
    }

    /**
     * Delete a heap.
     * 
     * @param heapId The UID of the heap to delete.
     * @return       0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC9805775, version = 150)
    public int sceKernelDeleteHeap(int heapId) {
        return 0;
    }
}