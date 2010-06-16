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
import jpcsp.HLE.kernel.types.SceKernelVplInfo;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import jpcsp.HLE.Modules;
import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.util.Utilities;

/*
 * TODO list:
 * 1. Check if writing result is needed in sceKernelAllocateVpl() and sceKernelTryAllocateVpl().
 *
 * 2. Implement a queue to receive blocks waiting for allocation and process
 * memory events for them (onFreeVpl).
 */

public class VplManager {

    private HashMap<Integer, SceKernelVplInfo> vplMap;

    public void reset() {
        vplMap = new HashMap<Integer, SceKernelVplInfo>();
    }

    public void sceKernelCreateVpl(int name_addr, int partitionid, int attr, int size, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringZ(name_addr);
        Modules.log.info("sceKernelCreateVpl(name=" + name
            + ",partition=" + partitionid
            + ",attr=0x" + Integer.toHexString(attr)
            + ",size=0x" + Integer.toHexString(size)
            + ",opt=0x" + Integer.toHexString(opt_addr) + ")");

        if (attr != 0) Modules.log.warn("PARTIAL:sceKernelCreateVpl attr value 0x" + Integer.toHexString(attr));

        if (mem.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateVpl option at 0x" + Integer.toHexString(opt_addr)
                + " (size=" + optsize + ")");
        }

        if ((attr & ~SceKernelVplInfo.VPL_ATTR_MASK) != 0) {
            Modules.log.warn("sceKernelCreateVpl bad attr value 0x" + Integer.toHexString(attr));
            cpu.gpr[2] = ERROR_ILLEGAL_ATTR;
        } else {
            SceKernelVplInfo info = SceKernelVplInfo.tryCreateVpl(name, partitionid, attr, size);
            if (info != null) {
                Modules.log.debug("sceKernelCreateVpl '" + name + "' assigned uid " + Integer.toHexString(info.uid));
                vplMap.put(info.uid, info);
                cpu.gpr[2] = info.uid;
            } else {
                cpu.gpr[2] = ERROR_NO_MEMORY;
            }
        }
    }

    public void sceKernelDeleteVpl(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelDeleteVpl(uid=0x" + Integer.toHexString(uid) + ")");

        SceKernelVplInfo info = vplMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {

            if (info.freeSize < info.poolSize) {
                Modules.log.warn("sceKernelDeleteVpl approx " + (info.poolSize - info.freeSize) + " unfreed bytes allocated");
            }

            // Free memory
            info.deleteSysMemInfo();

            cpu.gpr[2] = 0;
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateVpl(SceKernelVplInfo info, int size) {
        int addr = 0;

        addr = info.tryAllocate(size);
        if (addr == 0) {
            // 8 byte overhead
            Modules.log.warn("tryAllocateVpl not enough free pool mem (want=" + size + "+8,free=" + info.freeSize + ",diff=" + (size + 8 - info.freeSize) + ")");
        } else {
            Modules.log.debug("tryAllocateVpl allocated address 0x" + Integer.toHexString(addr));
        }

        return addr;
    }

    public void sceKernelAllocateVpl(int uid, int size, int data_addr, int timeout_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelAllocateVpl(uid=0x" + Integer.toHexString(uid)
            + ",size=0x" + Integer.toHexString(size)
            + ",data=0x" + Integer.toHexString(data_addr)
            + ",timeout=0x" + Integer.toHexString(timeout_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelAllocateVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            int addr = tryAllocateVpl(info, size);
            if (addr == 0) {
                // Alloc failed
                mem.write32(data_addr, 0);
                cpu.gpr[2] = ERROR_WAIT_TIMEOUT;

                Modules.log.warn("UNIMPLEMENTED:sceKernelAllocateVpl uid=0x" + Integer.toHexString(uid) + " wait");
            } else {
                // Alloc succeeded
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelAllocateVplCB(int uid, int size, int data_addr, int timeout_addr) {
        Modules.log.warn("sceKernelAllocateVplCB redirecting to sceKernelAllocateVpl");
        sceKernelAllocateVpl(uid, size, data_addr, timeout_addr);
    }

    public void sceKernelTryAllocateVpl(int uid, int size, int data_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelTryAllocateVpl(uid=0x" + Integer.toHexString(uid)
            + ",size=0x" + Integer.toHexString(size)
            + ",data=0x" + Integer.toHexString(data_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelTryAllocateVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            int addr = tryAllocateVpl(info, size);
            if (addr == 0) {
                // Alloc failed
                cpu.gpr[2] = ERROR_NO_MEMORY;
            } else {
                // Alloc succeeded
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelFreeVpl(int uid, int data_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.info("sceKernelFreeVpl(uid=0x" + Integer.toHexString(uid)
            + ",data=0x" + Integer.toHexString(data_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelFreeVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            if (info.free(data_addr)) {
                cpu.gpr[2] = 0;
            } else {
                cpu.gpr[2] = ERROR_ILLEGAL_MEMBLOCK;
            }
        }
    }

    public void sceKernelCancelVpl(int uid, int pnum_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("PARTIAL:sceKernelCancelVpl(uid=0x" + Integer.toHexString(uid)
            + ",pnum=0x" + Integer.toHexString(pnum_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceKernelReferVplStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelReferVplStatus(uid=0x" + Integer.toHexString(uid)
            + ",info=0x" + Integer.toHexString(info_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferVplStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    public static final VplManager singleton;

    private VplManager() {
    }

    static {
        singleton = new VplManager();
    }
}