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
package jpcsp.HLE.kernel.managers;

import java.util.HashMap;
import jpcsp.HLE.kernel.types.SceKernelFplInfo;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import jpcsp.HLE.Modules;
import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.util.Utilities;

/*
 * TODO list:
 * 1. Implement a queue to receive blocks waiting for allocation and process
 * memory events for them (onFreeFpl).
 *
 * 2. Find the correct error code sceKernelCreateFpl() when blocksize is 0.
 */

public class FplManager {

    private HashMap<Integer, SceKernelFplInfo> fplMap;

    public void reset() {
        fplMap = new HashMap<Integer, SceKernelFplInfo>();
    }

    public void sceKernelCreateFpl(int name_addr, int partitionid, int attr, int blocksize, int blocks, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringZ(name_addr);
        Modules.log.info("sceKernelCreateFpl(name='" + name
            + "',partition=" + partitionid
            + ",attr=0x" + Integer.toHexString(attr)
            + ",blocksize=0x" + Integer.toHexString(blocksize)
            + ",blocks=" + blocks
            + ",opt=0x" + Integer.toHexString(opt_addr) + ")");

        if (attr != 0) Modules.log.warn("PARTIAL:sceKernelCreateFpl attr value 0x" + Integer.toHexString(attr));

        if (mem.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateFpl option at 0x" + Integer.toHexString(opt_addr)
                + " (size=" + optsize + ")");

            // [1] = 0x40 phantasy star
            for (int i = 4; i < optsize && i < 200; i += 4) {
                Modules.log.debug(String.format("opt[%d] = %08X", (i / 4), mem.read32(opt_addr + i)));
            }
        }

        if ((attr & ~SceKernelFplInfo.FPL_ATTR_MASK) != 0) {
            Modules.log.warn("sceKernelCreateFpl bad attr value 0x" + Integer.toHexString(attr));
            cpu.gpr[2] = ERROR_ILLEGAL_ATTR;
        } else if (blocksize == 0) {
            Modules.log.warn("sceKernelCreateFpl bad blocksize, cannot be 0");
            cpu.gpr[2] = -1;
        } else {
            SceKernelFplInfo info = SceKernelFplInfo.tryCreateFpl(name, partitionid, attr, blocksize, blocks);
            if (info != null) {
                Modules.log.debug("sceKernelCreateFpl '" + name + "' assigned uid " + Integer.toHexString(info.uid));
                fplMap.put(info.uid, info);
                cpu.gpr[2] = info.uid;
            } else {
                cpu.gpr[2] = ERROR_NO_MEMORY;
            }
        }
    }

    public void sceKernelDeleteFpl(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;
        String msg = "sceKernelDeleteFpl(uid=0x" + Integer.toHexString(uid) + ")";

        SceKernelFplInfo info = fplMap.remove(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            msg += " '" + info.name + "'";
            Modules.log.debug(msg);

            if (info.freeBlocks < info.numBlocks) {
                Modules.log.warn(msg + " " + (info.numBlocks - info.freeBlocks) + " unfreed blocks, continuing");
            }

            // Free memory
            info.deleteSysMemInfo();

            cpu.gpr[2] = 0;
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateFpl(SceKernelFplInfo info) {
        int block;
        int addr = 0;

        if (info.freeBlocks == 0 || (block = info.findFreeBlock()) == -1) {
            Modules.log.warn("tryAllocateFpl no free blocks (numBlocks=" + info.numBlocks + ")");
            return 0;
        } else {
            addr = info.allocateBlock(block);
        }

        return addr;
    }

    public void sceKernelAllocateFpl(int uid, int data_addr, int timeout_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelAllocateFpl(uid=0x" + Integer.toHexString(uid)
            + ",data=0x" + Integer.toHexString(data_addr)
            + ",timeout=0x" + Integer.toHexString(timeout_addr) + ")");

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelAllocateFpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            int addr = tryAllocateFpl(info);
            if (addr == 0) {
                // Alloc failed - don't write to data_addr, yet
                cpu.gpr[2] = ERROR_WAIT_TIMEOUT;

                Modules.log.warn("UNIMPLEMENTED:sceKernelAllocateFpl uid=0x" + Integer.toHexString(uid) + " wait");

            } else {
                // Alloc succeeded
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelAllocateFplCB(int uid, int data_addr, int timeout_addr) {
        Modules.log.warn("sceKernelAllocateFplCB redirecting to sceKernelAllocateFpl");
        sceKernelAllocateFpl(uid, data_addr, timeout_addr);
    }

    public void sceKernelTryAllocateFpl(int uid, int data_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelTryAllocateFpl(uid=0x" + Integer.toHexString(uid)
            + ",data=0x" + Integer.toHexString(data_addr) + ")");

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelTryAllocateFpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            int addr = tryAllocateFpl(info);
            if (addr == 0) {
                // Alloc failed - don't write to data_addr
                cpu.gpr[2] = ERROR_NO_MEMORY;
            } else {
                // Alloc succeeded
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelFreeFpl(int uid, int data_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        String msg = "sceKernelFreeFpl(uid=0x" + Integer.toHexString(uid)
            + ",data=0x" + Integer.toHexString(data_addr) + ")";

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            msg += " '" + info.name + "'";
            int block = info.findBlockByAddress(data_addr);
            if (block == -1) {
                Modules.log.warn(msg + " unknown block address=0x" + Integer.toHexString(data_addr));
                cpu.gpr[2] = ERROR_ILLEGAL_MEMBLOCK;
            } else {
                Modules.log.debug(msg);
                info.freeBlock(block);
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelCancelFpl(int uid, int pnum_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("UNIMPLEMENTED:sceKernelCancelFpl(uid=0x" + Integer.toHexString(uid)
            + ",pnum=0x" + Integer.toHexString(pnum_addr) + ")");

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelFpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceKernelReferFplStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelReferFplStatus(uid=0x" + Integer.toHexString(uid)
            + ",info=0x" + Integer.toHexString(info_addr) + ")");

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferFplStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    public static final FplManager singleton;

    private FplManager() {
    }

    static {
        singleton = new FplManager();
    }
}