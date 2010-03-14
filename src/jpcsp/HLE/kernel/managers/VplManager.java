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
import jpcsp.HLE.pspSysMem;
import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.util.Utilities;

public class VplManager {

    private HashMap<Integer, SceKernelVplInfo> vplMap;

    public void reset() {
        vplMap = new HashMap<Integer, SceKernelVplInfo>();
    }

    // attr = alignment?
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
                mem.write32(data_addr, 0); // TODO still write on failure?
                cpu.gpr[2] = ERROR_WAIT_TIMEOUT; // TODO if we wakeup and manage to allocate set v0 = 0

                Modules.log.warn("UNIMPLEMENTED:sceKernelAllocateVpl uid=0x" + Integer.toHexString(uid) + " wait");

                /* TODO
                // try allocate when something frees
                if (info.freeBlocks == 0) {
                    // no free blocks in this fpl
                    info.waitAllocateQueue.add(Managers.ThreadManager.getCurrentThreadId());
                } else {
                    // some free blocks, but no free mem
                    waitAllocateQueue.add(Managers.ThreadManager.getCurrentThreadId());
                }

                if (timeout_addr == 0) {
                    Managers.ThreadManager.blockCurrentThread()
                } else {
                    // wakeup after timeout has expired
                    int micros = mem.read32(timeout_addr);
                    Managers.ThreadManager.delayCurrentThread(micros);
                }
                */
            } else {
                // Alloc succeeded
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelAllocateVplCB(int uid, int size, int data_addr, int timeout_addr) {
        // TODO there's no point even considering CB support until we've added timeout support
        Modules.log.warn("sceKernelAllocateVplCB redirecting to sceKernelAllocateVpl");
        sceKernelAllocateVpl(uid, size, data_addr, timeout_addr);
        //ThreadMan.getInstance().checkCallbacks();
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
                //mem.write32(data_addr, 0); // don't write on failure, check
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
            // TODO might need to rework this to get better error codes out of it
            if (info.free(data_addr)) {
                cpu.gpr[2] = 0;
                // TODO onFreeVpl(info);
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
            // TODO
            // - for each thread waiting to allocate on this fpl, wake it up
            // - if pnum_addr is a valid pointer write the number of threads we woke up to it

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


    /* TODO
     * this will probably need hooking into pspsysmem free()
    private void onFreeSysMem() {
        for (Iterator<Integer> it = waitAllocateQueue.iterator(); it.hasNext(); ) {
            int waitingThreadId = it.next();

            SceKernelThreadInfo thread = Managers.ThreadMan.getThread(waitingThreadId);
            if (thread == null) {
                // Thread got deleted while waiting for free mem
                it.remove();
            } else {
                // re-read syscall params
                int uid = thread.cpuContext.gpr[4];
                int data_addr = thread.cpuContext.gpr[5];

                SceKernelVplInfo info = vplMap.get(uid);
                if (info == null) {
                    // Vpl got deleted while we were waiting
                    it.remove();
                } else {
                    int addr = tryAllocateVpl(info);
                    if (addr != 0) {
                        mem.write32(data_addr, addr);
                        thread.cpuContext.cpu.gpr[2] = 0;
                        Managers.ThreadMan.resumeThread(waitingThreadId);
                        it.remove();
                        break;
                    }
                }
            }

        }
    }
    */

    /* TODO
    private void onFreeVpl(SceKernelVplInfo info) {
        for (Iterator<Integer> it = info.waitAllocateQueue.iterator(); it.hasNext(); ) {
            int waitingThreadId = it.next();
            SceKernelThreadInfo thread = Managers.ThreadMan.getThread(waitingThreadId);
            if (thread == null) {
                // Thread got deleted
                it.remove();
            } else {
                int addr = tryAllocateVpl(info);
                if (addr != 0) {
                    int data_addr = thread.cpuContext.gpr[5]; // re-read syscall param
                    mem.write32(data_addr, addr);
                    thread.cpuContext.cpu.gpr[2] = 0;
                    Managers.ThreadMan.resumeThread(waitingThreadId);
                    it.remove();
                    break;
                }
            }
        }
    }
    */

    public static final VplManager singleton;

    private VplManager() {
    }

    static {
        singleton = new VplManager();
    }
}
