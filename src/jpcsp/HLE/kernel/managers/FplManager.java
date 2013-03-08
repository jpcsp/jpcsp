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
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMSIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMBLOCK;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_FPOOL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_FPL;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelFplInfo;
import jpcsp.HLE.kernel.types.SceKernelFplOptParam;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;

import org.apache.log4j.Logger;

public class FplManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelFplInfo> fplMap;
    private FplWaitStateChecker fplWaitStateChecker;

    public final static int PSP_FPL_ATTR_FIFO = 0;
    public final static int PSP_FPL_ATTR_PRIORITY = 0x100;
    private final static int PSP_FPL_ATTR_MASK = 0x41FF;            // Anything outside this mask is an illegal attr.
    private final static int PSP_FPL_ATTR_ADDR_HIGH = 0x4000;       // Create the fpl in high memory.

    public void reset() {
        fplMap = new HashMap<Integer, SceKernelFplInfo>();
        fplWaitStateChecker = new FplWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        SceKernelFplInfo fpl = fplMap.get(thread.wait.Fpl_id);
        if (fpl == null) {
        	return false;
        }

        fpl.threadWaitingList.removeWaitingThread(thread);

        return true;
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_TIMEOUT;
        } else {
            log.warn("FPL deleted while we were waiting for it! (timeout expired)");
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
        if (thread.isWaitingForType(PSP_WAIT_FPL)) {
            removeWaitingThread(thread);
        }
    }

    private void onFplDeletedCancelled(int fid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.isWaitingFor(PSP_WAIT_FPL, fid)) {
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

    private void onFplDeleted(int fid) {
        onFplDeletedCancelled(fid, ERROR_KERNEL_WAIT_DELETE);
    }

    private void onFplCancelled(int fid) {
        onFplDeletedCancelled(fid, ERROR_KERNEL_WAIT_CANCELLED);
    }

    private void onFplFree(SceKernelFplInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        SceKernelThreadInfo checkedThread = null;
        while (info.freeBlocks > 0) {
            SceKernelThreadInfo thread = info.threadWaitingList.getNextWaitingThread(checkedThread);
            if (thread == null) {
            	break;
            }
            int addr = tryAllocateFpl(info);
            if (addr != 0) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("onFplFree waking thread %s", thread));
                }
                // Return the allocated address
                thread.wait.Fpl_dataAddr.setValue(addr);

                info.threadWaitingList.removeWaitingThread(thread);
                thread.cpuContext._v0 = 0;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            } else {
            	checkedThread = thread;
            }
        }

        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateFpl(SceKernelFplInfo info) {
        int block;
        int addr = 0;

        if (info.freeBlocks == 0 || (block = info.findFreeBlock()) == -1) {
            log.warn("tryAllocateFpl no free blocks (numBlocks=" + info.numBlocks + ")");
            return 0;
        }
        addr = info.allocateBlock(block);

        return addr;
    }

    public int checkFplID(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-Fpl", true);
        if (!fplMap.containsKey(uid)) {
            log.warn(String.format("checkFplID unknown uid=0x%X", uid));
            throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_FPOOL);
        }

        return uid;
    }

    public int sceKernelCreateFpl(String name, int partitionid, int attr, int blocksize, int blocks, TPointer option) {
        int memType = PSP_SMEM_Low;
        if ((attr & PSP_FPL_ATTR_ADDR_HIGH) == PSP_FPL_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }
        int memAlign = 4;  // 4-bytes is default.
        if (option.isNotNull()) {
            int optionSize = option.getValue32();
            // Up to firmware 6.20 only two FplOptParam fields exist, being the
            // first one the struct size, the second is the memory alignment (0 is default,
            // which is 4-byte/32-bit).
            if ((optionSize >= 4) && (optionSize <= 8)) {
                SceKernelFplOptParam optParams = new SceKernelFplOptParam();
                optParams.read(option);
                if (optParams.align > 0) {
                    memAlign = optParams.align;
                }
                if (log.isDebugEnabled()) {
                	log.debug(String.format("sceKernelCreateFpl options: struct size=%d, alignment=0x%X", optParams.sizeof(), optParams.align));
                }
            } else {
                log.warn(String.format("sceKernelCreateFpl option at %s, size=%d", option, optionSize));
            }
        }
        if ((attr & ~PSP_FPL_ATTR_MASK) != 0) {
            log.warn(String.format("sceKernelCreateFpl bad attr value 0x%X", attr));
            return ERROR_KERNEL_ILLEGAL_ATTR;
        }
        if (blocksize == 0) {
            log.warn("sceKernelCreateFpl bad blocksize, cannot be 0");
            return ERROR_KERNEL_ILLEGAL_MEMSIZE;
        }

        SceKernelFplInfo info = SceKernelFplInfo.tryCreateFpl(name, partitionid, attr, blocksize, blocks, memType, memAlign);
        if (info == null) {
        	return ERROR_KERNEL_NO_MEMORY;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCreateFpl returning %s", info));
        }
        fplMap.put(info.uid, info);

        return info.uid;
    }

    public int sceKernelDeleteFpl(int uid) {
        SceKernelFplInfo info = fplMap.remove(uid);
        if (info.freeBlocks < info.numBlocks) {
            log.warn(String.format("sceKernelDeleteFpl %s unfreed blocks, deleting", info.numBlocks - info.freeBlocks));
        }
        info.deleteSysMemInfo();
        onFplDeleted(uid);

        return 0;
    }

    private int hleKernelAllocateFpl(int uid, TPointer32 dataAddr, TPointer32 timeoutAddr, boolean wait, boolean doCallbacks) {
        SceKernelFplInfo fpl = fplMap.get(uid);
        int addr = tryAllocateFpl(fpl);
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (addr == 0) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelAllocateFpl %s fast check failed", fpl));
            }
            if (!wait) {
            	return ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
            }
            // Go to wait state
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            fpl.threadWaitingList.addWaitingThread(currentThread);
            currentThread.wait.Fpl_id = uid;
            currentThread.wait.Fpl_dataAddr = dataAddr;
            threadMan.hleKernelThreadEnterWaitState(PSP_WAIT_FPL, uid, fplWaitStateChecker, timeoutAddr.getAddress(), doCallbacks);
        } else {
            // Success, do not reschedule the current thread.
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelAllocateFpl %s fast check succeeded", fpl));
            }
            dataAddr.setValue(addr);
        }

        return 0;
    }

    public int sceKernelAllocateFpl(int uid, TPointer32 dataAddr, TPointer32 timeoutAddr) {
        return hleKernelAllocateFpl(uid, dataAddr, timeoutAddr, true, false);
    }

    public int sceKernelAllocateFplCB(int uid, TPointer32 dataAddr, TPointer32 timeoutAddr) {
        return hleKernelAllocateFpl(uid, dataAddr, timeoutAddr, true, true);
    }

    public int sceKernelTryAllocateFpl(int uid, TPointer32 dataAddr) {
        return hleKernelAllocateFpl(uid, dataAddr, TPointer32.NULL, false, false);
    }

    public int sceKernelFreeFpl(int uid, TPointer dataAddr) {
        SceKernelFplInfo info = fplMap.get(uid);
        int block = info.findBlockByAddress(dataAddr.getAddress());
        if (block < 0) {
            log.warn(String.format("sceKernelFreeFpl unknown block address=%s", dataAddr));
            return ERROR_KERNEL_ILLEGAL_MEMBLOCK;
        }

        info.freeBlock(block);
        onFplFree(info);

        return 0;
    }

    public int sceKernelCancelFpl(int uid, TPointer32 numWaitThreadAddr) {
        SceKernelFplInfo info = fplMap.get(uid);
        numWaitThreadAddr.setValue(info.getNumWaitThreads());
        info.threadWaitingList.removeAllWaitingThreads();
        onFplCancelled(uid);

        return 0;
    }

    public int sceKernelReferFplStatus(int uid, TPointer infoAddr) {
        SceKernelFplInfo info = fplMap.get(uid);
        info.write(infoAddr);

        return 0;
    }

    private class FplWaitStateChecker implements IWaitStateChecker {
        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the fpl
            // has been allocated during the callback execution.
            SceKernelFplInfo fpl = fplMap.get(wait.Fpl_id);
            if (fpl == null) {
                thread.cpuContext._v0 = ERROR_KERNEL_NOT_FOUND_FPOOL;
                return false;
            }

            // Check fpl.
            int addr = tryAllocateFpl(fpl);
            if (addr != 0) {
            	fpl.threadWaitingList.removeWaitingThread(thread);
            	thread.wait.Fpl_dataAddr.setValue(addr);
                thread.cpuContext._v0 = 0;
                return false;
            }

            return true;
        }
    }
    public static final FplManager singleton = new FplManager();

    private FplManager() {
    }
}