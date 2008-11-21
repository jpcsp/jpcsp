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

        name_addr &= 0x3FFFFFFF;
        String name = Utilities.readStringZ(name_addr);
        Modules.log.info("sceKernelCreateVpl(name=" + name
            + ",partition=" + partitionid
            + ",attr=0x" + Integer.toHexString(attr)
            + ",size=0x" + Integer.toHexString(size)
            + ",opt=0x" + Integer.toHexString(opt_addr) + ")");

        if (mem.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            if (optsize != 4) {
                Modules.log.warn("sceKernelCreateVpl opt size mismatch"
                    + " (got=" + optsize + ",expected=" + 4 + ")");
            }
        }

        SceKernelVplInfo info = new SceKernelVplInfo(name, partitionid, attr, size);
        vplMap.put(info.uid, info);
        cpu.gpr[2] = info.uid;
    }

    public void sceKernelDeleteVpl(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.info("sceKernelDeleteVpl(uid=0x" + Integer.toHexString(uid) + ")");

        SceKernelVplInfo info = vplMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else {
            if (info.freeSize < info.poolSize) {
                Modules.log.warn("sceKernelDeleteVpl " + (info.numBlocks - info.freeBlocks) + " unfreed mem");

                // Free mem
                for (int i = 0; i < info.numBlocks; i++) {
                    int addr = info.blockAddress[i];
                    if (addr != 0) {
                        pspSysMem.getInstance().free(addr);
                        info.blockAddress[i] = 0;
                        info.freeBlocks++;
                    }
                }
            }

            cpu.gpr[2] = 0;
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateVpl(SceKernelVplInfo info, int size) {
        int block;
        int addr = 0;

        if (info.freeBlocks == 0 || (block = info.findFreeBlock()) == -1) {
            // TODO increase block count or switch to using a list
            Modules.log.warn("tryAllocateVpl no free blocks (numBlocks=" + info.numBlocks + ")");
            return 0;
        } else if (size > info.freeSize) {
            Modules.log.warn("tryAllocateVpl no free pool mem (want=" + size + ",free=" + info.freeSize + ")");
            return 0;
        } else if (size > pspSysMem.getInstance().maxFreeMemSize()) {
            Modules.log.warn("tryAllocateVpl no free sys mem (want=" + size + ",free=" + pspSysMem.getInstance().maxFreeMemSize() + ")");
            return 0;
        } else {
            addr = pspSysMem.getInstance().malloc(info.partitionid, pspSysMem.PSP_SMEM_Low, size, 0);
            if (addr != 0) {
                pspSysMem.getInstance().addSysMemInfo(info.partitionid, "ThreadMan-Vpl", pspSysMem.PSP_SMEM_Low, size, addr);
                info.blockAddress[block] = addr;
                info.blockSize[block] = size;
                info.freeBlocks--;
                info.freeSize -= size;
            }
        }

        return addr;
    }

    public void sceKernelAllocateVpl(int uid, int size, int data_addr, int timeout_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.info("sceKernelAllocateVpl(uid=0x" + Integer.toHexString(uid)
            + ",size=0x" + Integer.toHexString(size)
            + ",data=0x" + Integer.toHexString(data_addr)
            + ",timeout=0x" + Integer.toHexString(timeout_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelAllocateVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else {
            int addr = tryAllocateVpl(info, size);
            if (addr == 0) {
                // Alloc failed
                mem.write32(data_addr, 0); // TODO still write on failure?
                cpu.gpr[2] = -1; // TODO if we wakeup and manage to allocate set v0 = 0

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

        Modules.log.info("sceKernelTryAllocateVpl(uid=0x" + Integer.toHexString(uid)
            + ",size=0x" + Integer.toHexString(size)
            + ",data=0x" + Integer.toHexString(data_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelTryAllocateVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else {
            int addr = tryAllocateVpl(info, size);
            if (addr == 0) {
                // Alloc failed
                mem.write32(data_addr, 0); // TODO still write on failure?
                cpu.gpr[2] = -1;
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
            cpu.gpr[2] = -1;
        } else {
            int block = info.findBlockByAddress(data_addr);
            if (block == -1) {
                Modules.log.warn("sceKernelFreeVpl unknown block=0x" + Integer.toHexString(data_addr));
                cpu.gpr[2] = -1;
            } else {
                pspSysMem.getInstance().free(data_addr);
                info.blockAddress[block] = 0;
                info.freeBlocks++;
                info.freeSize += info.blockSize[block];
                info.blockSize[block] = 0;
                cpu.gpr[2] = 0;
                // TODO onFreeVpl(info);
            }
        }
    }

    public void sceKernelCancelVpl(int uid, int pnum_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.info("sceKernelCancelVpl(uid=0x" + Integer.toHexString(uid)
            + ",pnum=0x" + Integer.toHexString(pnum_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
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

        Modules.log.info("sceKernelReferVplStatus(uid=0x" + Integer.toHexString(uid)
            + ",info=0x" + Integer.toHexString(info_addr) + ")");

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferVplStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
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
