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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_ATTR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_MEMBLOCK;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_MEMSIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_VPOOL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NO_MEMORY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CAN_NOT_WAIT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_VPL;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelVplInfo;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class VplManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelVplInfo> vplMap;
    private VplWaitStateChecker vplWaitStateChecker;

    protected final static int PSP_VPL_ATTR_FIFO = 0;
    protected final static int PSP_VPL_ATTR_PRIORITY = 0x100;
    protected final static int PSP_VPL_ATTR_MASK = 0x41FF;            // Anything outside this mask is an illegal attr.
    protected final static int PSP_VPL_ATTR_ADDR_HIGH = 0x4000;       // Create the vpl in high memory.
    protected final static int PSP_VPL_ATTR_EXT = 0x8000;             // Extend the vpl memory area (exact purpose is unknown).

    public void reset() {
        vplMap = new HashMap<Integer, SceKernelVplInfo>();
        vplWaitStateChecker = new VplWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        // Untrack
        thread.wait.waitingOnVpl = false;
        // Update numWaitThreads
        SceKernelVplInfo fpl = vplMap.get(thread.wait.Vpl_id);
        if (fpl != null) {
            fpl.numWaitThreads--;
            if (fpl.numWaitThreads < 0) {
                log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", vpl " + Integer.toHexString(fpl.uid) + " numWaitThreads underflowed");
                fpl.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            log.warn("VPL deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnVpl) {
            removeWaitingThread(thread);
        }
    }

    private void onVplDeletedCancelled(int vid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.waitType == PSP_WAIT_VPL &&
                    thread.wait.waitingOnVpl &&
                    thread.wait.Vpl_id == vid) {
                // VPL was deleted
                log.warn("VPL deleted while we were waiting for it! thread:" + Integer.toHexString(thread.uid) + "/'" + thread.name + "'");
                // Untrack
                thread.wait.waitingOnVpl = false;
                // Return ERROR_WAIT_DELETE / ERROR_WAIT_CANCELLED
                thread.cpuContext.gpr[2] = result;
                // Wakeup
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
            }
        }
        threadMan.hleRescheduleCurrentThread();
    }

    private void onVplDeleted(int vid) {
        onVplDeletedCancelled(vid, ERROR_WAIT_DELETE);
    }

    private void onVplCancelled(int vid) {
        onVplDeletedCancelled(vid, ERROR_WAIT_CANCELLED);
    }

    private void onVplFree(SceKernelVplInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if ((info.attr & PSP_VPL_ATTR_PRIORITY) == PSP_VPL_ATTR_FIFO) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_VPL &&
                        thread.wait.waitingOnVpl &&
                        thread.wait.Vpl_id == info.uid) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("onVplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.wait.waitingOnVpl = false;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        } else if ((info.attr & PSP_VPL_ATTR_PRIORITY) == PSP_VPL_ATTR_PRIORITY) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_VPL &&
                        thread.wait.waitingOnVpl &&
                        thread.wait.Vpl_id == info.uid) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("onVplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.wait.waitingOnVpl = false;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateVpl(SceKernelVplInfo info, int size) {
        int addr = 0;
        int alignedSize = (size + 7) & ~7;
        if (alignedSize + 8 <= info.freeSize) {
            if ((info.attr & PSP_VPL_ATTR_ADDR_HIGH) == PSP_VPL_ATTR_ADDR_HIGH) {
                addr = info.freeHighAddress - alignedSize;
                info.freeHighAddress -= alignedSize + 8;
            } else {
                addr = info.freeLowAddress + 8;
                info.freeLowAddress += alignedSize + 8;
            }

            Memory mem = Memory.getInstance();
            mem.write32(addr - 8, info.allocAddress);
            mem.write32(addr - 4, 0);

            info.freeSize -= alignedSize + 8;
            info.dataBlockMap.put(addr, alignedSize);
        }
        return addr;
    }

    public void sceKernelCreateVpl(int name_addr, int partitionid, int attr, int size, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringZ(name_addr);
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateVpl(name=" + name + ",partition=" + partitionid + ",attr=0x" + Integer.toHexString(attr) + ",size=0x" + Integer.toHexString(size) + ",opt=0x" + Integer.toHexString(opt_addr) + ")");
        }

        if (Memory.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            log.warn("sceKernelCreateVpl option at 0x" + Integer.toHexString(opt_addr) + " (size=" + optsize + ")");
        }

        int memType = PSP_SMEM_Low;
        if ((attr & PSP_VPL_ATTR_ADDR_HIGH) == PSP_VPL_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }

        if ((attr & ~PSP_VPL_ATTR_MASK) != 0) {
            log.warn("sceKernelCreateVpl bad attr value 0x" + Integer.toHexString(attr));
            cpu.gpr[2] = ERROR_ILLEGAL_ATTR;
        } else if (size <= 0) {
        	cpu.gpr[2] = ERROR_ILLEGAL_MEMSIZE;
        } else {
            SceKernelVplInfo info = SceKernelVplInfo.tryCreateVpl(name, partitionid, attr, size, memType);
            if (info != null) {
                log.debug("sceKernelCreateVpl '" + name + "' assigned uid " + Integer.toHexString(info.uid));
                vplMap.put(info.uid, info);
                cpu.gpr[2] = info.uid;
            } else {
                cpu.gpr[2] = ERROR_NO_MEMORY;
            }
        }
    }

    public void sceKernelDeleteVpl(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelDeleteVpl(uid=0x" + Integer.toHexString(uid) + ")");
        }

        SceKernelVplInfo info = vplMap.remove(uid);
        if (info == null) {
            log.warn("sceKernelDeleteVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            if (info.freeSize < info.poolSize) {
                log.warn("sceKernelDeleteVpl approx " + (info.poolSize - info.freeSize) + " unfreed bytes allocated");
            }
            info.deleteSysMemInfo();
            cpu.gpr[2] = 0;
            onVplDeleted(uid);
        }
    }

    private void hleKernelAllocateVpl(int uid, int size, int data_addr, int timeout_addr, boolean wait, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        if (log.isDebugEnabled()) {
            log.debug("hleKernelAllocateVpl uid=0x" + Integer.toHexString(uid) + " size=0x" + Integer.toHexString(size) + " data_addr=0x" + Integer.toHexString(data_addr) + " timeout_addr=0x" + Integer.toHexString(timeout_addr) + " callbacks=" + doCallbacks);
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-Vpl", true);
        SceKernelVplInfo vpl = vplMap.get(uid);
        if (vpl == null) {
            log.warn("hleKernelAllocateVpl unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else if (size <= 0 || size > vpl.poolSize) {
        	cpu.gpr[2] = ERROR_ILLEGAL_MEMSIZE;
        } else {
            int addr = tryAllocateVpl(vpl, size);
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            int micros = 0;
            if (Memory.isAddressGood(timeout_addr)) {
                micros = mem.read32(timeout_addr);
            }
            if (addr == 0) {
                log.debug("hleKernelAllocateVpl fast check failed");
                if (wait) {
                    vpl.numWaitThreads++;
                    // Go to wait state
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                    currentThread.waitType = PSP_WAIT_VPL;
                    currentThread.waitId = uid;
                    // Wait on a specific fpl
                    threadMan.hleKernelThreadWait(currentThread, micros, (timeout_addr == 0));

                    currentThread.wait.waitingOnVpl = true;
                    currentThread.wait.Vpl_id = uid;
                    currentThread.wait.Vpl_size = size;
                    currentThread.wait.Vpl_dataAddr = data_addr;
                    currentThread.wait.waitStateChecker = vplWaitStateChecker;

                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
                    cpu.gpr[2] = ERROR_WAIT_CAN_NOT_WAIT;
                } else {
                    cpu.gpr[2] = 0;
                }
            } else {
                log.debug("hleKernelAllocateVpl fast check succeeded");
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelAllocateVpl(int uid, int size, int data_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelAllocateVpl redirecting to hleKernelAllocateVpl(callbacks=false)");
        }
        hleKernelAllocateVpl(uid, size, data_addr, timeout_addr, true, false);
    }

    public void sceKernelAllocateVplCB(int uid, int size, int data_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelAllocateVplCB redirecting to hleKernelAllocateVpl(callbacks=true)");
        }
        hleKernelAllocateVpl(uid, size, data_addr, timeout_addr, true, true);
    }

    public void sceKernelTryAllocateVpl(int uid, int size, int data_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelTryAllocateVpl redirecting to hleKernelAllocateVpl");
        }
        hleKernelAllocateVpl(uid, size, data_addr, 0, false, false);
    }

    public void sceKernelFreeVpl(int uid, int data_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelFreeVpl(uid=0x" + Integer.toHexString(uid) + ",data=0x" + Integer.toHexString(data_addr) + ")");
        }

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            log.warn("sceKernelFreeVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            if (info.free(data_addr)) {
                cpu.gpr[2] = 0;
                onVplFree(info);
            } else {
                cpu.gpr[2] = ERROR_ILLEGAL_MEMBLOCK;
            }
        }
    }

    public void sceKernelCancelVpl(int uid, int numWaitThreadAddr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelVpl(uid=0x" + Integer.toHexString(uid) + ",numWaitThreadAddr=0x" + Integer.toHexString(numWaitThreadAddr) + ")");
        }

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            log.warn("sceKernelCancelVpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            Memory mem = Memory.getInstance();
            if (Memory.isAddressGood(numWaitThreadAddr)) {
                mem.write32(numWaitThreadAddr, info.numWaitThreads);
            }
            cpu.gpr[2] = 0;
            onVplCancelled(uid);
        }
    }

    public void sceKernelReferVplStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferVplStatus(uid=0x" + Integer.toHexString(uid) + ",info=0x" + Integer.toHexString(info_addr) + ")");
        }

        SceKernelVplInfo info = vplMap.get(uid);
        if (info == null) {
            log.warn("sceKernelReferVplStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_VPOOL;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    private class VplWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the vpl
            // has been allocated during the callback execution.
            SceKernelVplInfo vpl = vplMap.get(wait.Vpl_id);
            if (vpl == null) {
                thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_VPOOL;
                return false;
            }

            // Check vpl.
            if (tryAllocateVpl(vpl, wait.Vpl_size) != 0) {
                vpl.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final VplManager singleton = new VplManager();

    private VplManager() {
    }
    
}