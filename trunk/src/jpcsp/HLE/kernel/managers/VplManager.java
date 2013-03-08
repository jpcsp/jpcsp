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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_ATTR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMBLOCK;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMSIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_VPOOL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_VPL;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelVplInfo;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;

import org.apache.log4j.Logger;

public class VplManager {

    public static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelVplInfo> vplMap;
    private VplWaitStateChecker vplWaitStateChecker;

    protected final static int PSP_VPL_ATTR_FIFO =         0;
    protected final static int PSP_VPL_ATTR_PRIORITY = 0x100;
    protected final static int PSP_VPL_ATTR_PASS =     0x200;   // Allow threads that want to allocate small memory blocks to bypass the waiting queue (less memory goes first).
    public final static int PSP_VPL_ATTR_ADDR_HIGH =  0x4000;   // Create the VPL in high memory.
    public final static int PSP_VPL_ATTR_EXT =        0x8000;   // Automatically extend the VPL's memory area (when allocating a block from the VPL and the remaining size is too small, this flag tells the VPL to automatically attempt to extend it's memory area).
    public final static int PSP_VPL_ATTR_MASK = PSP_VPL_ATTR_EXT | PSP_VPL_ATTR_ADDR_HIGH | PSP_VPL_ATTR_PASS | PSP_VPL_ATTR_PRIORITY | 0xFF; // Anything outside this mask is an illegal attr.

    public void reset() {
        vplMap = new HashMap<Integer, SceKernelVplInfo>();
        vplWaitStateChecker = new VplWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
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
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_TIMEOUT;
        } else {
            log.warn("VPL deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_DELETE;
        }
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return ERROR_WAIT_STATUS_RELEASED
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_STATUS_RELEASED;
        } else {
            log.warn("EventFlag deleted while we were waiting for it!");
            // Return WAIT_DELETE
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.isWaitingForType(PSP_WAIT_VPL)) {
            removeWaitingThread(thread);
        }
    }

    private void onVplDeletedCancelled(int vid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.isWaitingForType(PSP_WAIT_VPL) &&
                    thread.wait.Vpl_id == vid) {
                thread.cpuContext._v0 = result;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            }
        }
        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private void onVplDeleted(int vid) {
        onVplDeletedCancelled(vid, ERROR_KERNEL_WAIT_DELETE);
    }

    private void onVplCancelled(int vid) {
        onVplDeletedCancelled(vid, ERROR_KERNEL_WAIT_CANCELLED);
    }

    private void onVplFree(SceKernelVplInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        if ((info.attr & PSP_VPL_ATTR_PRIORITY) == PSP_VPL_ATTR_FIFO) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.isWaitingForType(PSP_WAIT_VPL) &&
                        thread.wait.Vpl_id == info.uid) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("onVplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.cpuContext._v0 = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;
                }
            }
        } else if ((info.attr & PSP_VPL_ATTR_PRIORITY) == PSP_VPL_ATTR_PRIORITY) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.isWaitingForType(PSP_WAIT_VPL) &&
                        thread.wait.Vpl_id == info.uid) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("onVplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.cpuContext._v0 = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;
                }
            }
        }
        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateVpl(SceKernelVplInfo info, int size) {
    	return info.alloc(size);
    }

    public int checkVplID(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-Vpl", true);
        if (!vplMap.containsKey(uid)) {
            log.warn(String.format("checkVplID unknown uid=0x%X", uid));
            throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_VPOOL);
        }

        return uid;
    }

    public int sceKernelCreateVpl(String name, int partitionid, int attr, int size, TPointer option) {
        if (option.isNotNull()) {
            int optionSize = option.getValue32();
            log.warn(String.format("sceKernelCreateVpl option at %s, size=%d", option, optionSize));
        }

        int memType = PSP_SMEM_Low;
        if ((attr & PSP_VPL_ATTR_ADDR_HIGH) == PSP_VPL_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }

        if ((attr & ~PSP_VPL_ATTR_MASK) != 0) {
            log.warn("sceKernelCreateVpl bad attr value 0x" + Integer.toHexString(attr));
            return ERROR_KERNEL_ILLEGAL_ATTR;
        }
        if (size <= 0) {
        	return ERROR_KERNEL_ILLEGAL_MEMSIZE;
        }

        SceKernelVplInfo info = SceKernelVplInfo.tryCreateVpl(name, partitionid, attr, size, memType);
        if (info == null) {
        	return ERROR_KERNEL_NO_MEMORY;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelCreateVpl returning %s", info));
        }
        vplMap.put(info.uid, info);

        return info.uid;
    }

    public int sceKernelDeleteVpl(int uid) {
        SceKernelVplInfo info = vplMap.remove(uid);
        if (info.freeSize < info.poolSize) {
            log.warn(String.format("sceKernelDeleteVpl approx 0x%X unfreed bytes allocated", info.poolSize - info.freeSize));
        }
        info.delete();
        onVplDeleted(uid);

        return 0;
    }

    private int hleKernelAllocateVpl(int uid, int size, TPointer32 dataAddr, TPointer32 timeoutAddr, boolean wait, boolean doCallbacks) {
        SceKernelVplInfo vpl = vplMap.get(uid);
        if (size <= 0 || size > vpl.poolSize) {
        	return ERROR_KERNEL_ILLEGAL_MEMSIZE;
        }

        int addr = tryAllocateVpl(vpl, size);
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (addr == 0) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelAllocateVpl %s fast check failed", vpl));
            }
            if (!wait) {
                return ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
            }
            vpl.numWaitThreads++;
            // Go to wait state
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            // Wait on a specific fpl
            currentThread.wait.Vpl_id = uid;
            currentThread.wait.Vpl_size = size;
            currentThread.wait.Vpl_dataAddr = dataAddr;
            threadMan.hleKernelThreadEnterWaitState(PSP_WAIT_VPL, uid, vplWaitStateChecker, timeoutAddr.getAddress(), doCallbacks);
        } else {
            // Success, do not reschedule the current thread.
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelAllocateVpl %s fast check succeeded, allocated addr=0x%08X", vpl, addr));
            }
            dataAddr.setValue(addr);
        }

        return 0;
    }

    public int sceKernelAllocateVpl(int uid, int size, TPointer32 dataAddr, TPointer32 timeoutAddr) {
        return hleKernelAllocateVpl(uid, size, dataAddr, timeoutAddr, true, false);
    }

    public int sceKernelAllocateVplCB(int uid, int size, TPointer32 dataAddr, TPointer32 timeoutAddr) {
        return hleKernelAllocateVpl(uid, size, dataAddr, timeoutAddr, true, true);
    }

    public int sceKernelTryAllocateVpl(int uid, int size, TPointer32 dataAddr) {
        return hleKernelAllocateVpl(uid, size, dataAddr, TPointer32.NULL, false, false);
    }

    public int sceKernelFreeVpl(int uid, TPointer dataAddr) {
        SceKernelVplInfo info = vplMap.get(uid);
        if (!info.free(dataAddr.getAddress())) {
        	return ERROR_KERNEL_ILLEGAL_MEMBLOCK;
        }

        onVplFree(info);

        return 0;
	}

    public int sceKernelCancelVpl(int uid, TPointer32 numWaitThreadAddr) {
        SceKernelVplInfo info = vplMap.get(uid);
        numWaitThreadAddr.setValue(info.numWaitThreads);
        onVplCancelled(uid);

        return 0;
    }

    public int sceKernelReferVplStatus(int uid, TPointer infoAddr) {
        SceKernelVplInfo info = vplMap.get(uid);
        info.write(infoAddr);

        return 0;
    }

    private class VplWaitStateChecker implements IWaitStateChecker {
        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the vpl
            // has been allocated during the callback execution.
            SceKernelVplInfo vpl = vplMap.get(wait.Vpl_id);
            if (vpl == null) {
                thread.cpuContext._v0 = ERROR_KERNEL_NOT_FOUND_VPOOL;
                return false;
            }

            // Check vpl.
            if (tryAllocateVpl(vpl, wait.Vpl_size) != 0) {
                vpl.numWaitThreads--;
                thread.cpuContext._v0 = 0;
                return false;
            }

            return true;
        }
    }
    public static final VplManager singleton = new VplManager();

    private VplManager() {
    }
}