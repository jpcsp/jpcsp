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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_NOT_FOUND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_LOCKED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_RECURSIVE_NOT_ALLOWED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_UNLOCKED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_UNLOCK_UNDERFLOW;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_LWMUTEX;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelLwMutexInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class LwMutexManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelLwMutexInfo> lwMutexMap;
    private LwMutexWaitStateChecker lwMutexWaitStateChecker;

    private final static int PSP_LWMUTEX_ATTR_FIFO = 0;
    private final static int PSP_LWMUTEX_ATTR_PRIORITY = 0x100;
    private final static int PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE = 0x200;

    public void reset() {
        lwMutexMap = new HashMap<Integer, SceKernelLwMutexInfo>();
        lwMutexWaitStateChecker = new LwMutexWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        // Untrack
        thread.wait.waitingOnLwMutex = false;
        // Update numWaitThreads
        SceKernelLwMutexInfo info = lwMutexMap.get(thread.wait.LwMutex_id);
        if (info != null) {
            info.numWaitThreads--;
            if (info.numWaitThreads < 0) {
                log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", lwmutex " + Integer.toHexString(info.uid) + " numWaitThreads underflowed");
                info.numWaitThreads = 0;
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
            log.warn("LwMutex deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return ERROR_WAIT_STATUS_RELEASED
            thread.cpuContext.gpr[2] = ERROR_WAIT_STATUS_RELEASED;
        } else {
            log.warn("EventFlag deleted while we were waiting for it!");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnMutex) {
            // decrement numWaitThreads
            removeWaitingThread(thread);
        }
    }

    private void onLwMutexDeleted(int lwmid) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.waitType == PSP_WAIT_LWMUTEX &&
                    thread.wait.waitingOnLwMutex &&
                    thread.wait.LwMutex_id == lwmid) {
                thread.wait.waitingOnLwMutex = false;
                thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            }
        }
        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private void onLwMutexModified(SceKernelLwMutexInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        if ((info.attr & PSP_LWMUTEX_ATTR_PRIORITY) == PSP_LWMUTEX_ATTR_FIFO) {
            for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_LWMUTEX &&
                        thread.wait.waitingOnLwMutex &&
                        thread.wait.LwMutex_id == info.uid &&
                        tryLockLwMutex(info, thread.wait.LwMutex_count, thread)) {
                    // New thread is taking control of LwMutex.
                    info.threadid = thread.uid;
                    // Update numWaitThreads
                    info.numWaitThreads--;
                    // Untrack
                    thread.wait.waitingOnLwMutex = false;
                    // Return success or failure
                    thread.cpuContext.gpr[2] = 0;
                    // Wakeup
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;
                }
            }
        } else if ((info.attr & PSP_LWMUTEX_ATTR_PRIORITY) == PSP_LWMUTEX_ATTR_PRIORITY) {
            for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iteratorByPriority(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_LWMUTEX &&
                        thread.wait.waitingOnLwMutex &&
                        thread.wait.LwMutex_id == info.uid &&
                        tryLockLwMutex(info, thread.wait.LwMutex_count, thread)) {
                    // New thread is taking control of LwMutex.
                    info.threadid = thread.uid;
                    // Update numWaitThreads
                    info.numWaitThreads--;
                    // Untrack
                    thread.wait.waitingOnLwMutex = false;
                    // Return success or failure
                    thread.cpuContext.gpr[2] = 0;
                    // Wakeup
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;
                }
            }
        }
        // Reschedule only if threads waked up.
        if (reschedule) {
            Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
        }
    }

    private boolean tryLockLwMutex(SceKernelLwMutexInfo info, int count, SceKernelThreadInfo thread) {
        if (info.lockedCount == 0) {
            // If the lwmutex is not locked, allow this thread to lock it.
            info.threadid = thread.uid;
            info.lockedCount += count;
            return true;
        } else if (info.threadid == thread.uid) {
            // If the lwmutex is already locked, but it's trying to be locked by the same thread
            // that acquired it initially, check if recursive locking is allowed.
            // If not, don't increase this lwmutex's lock count, but still return true.
            if (((info.attr & PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE) == PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE)) {
                info.lockedCount += count;
            }
            return true;
        }
        return false;
    }

    private void hleKernelLockLwMutex(int uid, int count, int timeout_addr, boolean wait, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        String message = "hleKernelLockLwMutex(uid=" + Integer.toHexString(uid) + ",count=" + count + ",timeout_addr=0x" + Integer.toHexString(timeout_addr) + ") wait=" + wait + ",cb=" + doCallbacks;
        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            log.warn(message + " - unknown UID");
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            if (!tryLockLwMutex(info, count, currentThread)) {
                if (log.isDebugEnabled()) {
                    log.debug(message + " - '" + info.name + "' fast check failed");
                }
                if (wait) {
                    // Failed, but it's ok, just wait a little
                    info.numWaitThreads++;
                    // wait type
                    currentThread.waitType = PSP_WAIT_LWMUTEX;
                    currentThread.waitId = uid;
                    // Go to wait state
                    int timeout = 0;
                    boolean forever = (timeout_addr == 0);
                    if (timeout_addr != 0) {
                        if (Memory.isAddressGood(timeout_addr)) {
                            timeout = mem.read32(timeout_addr);
                        } else {
                            log.warn(message + " - bad timeout address");
                        }
                    }
                    threadMan.hleKernelThreadWait(currentThread, timeout, forever);
                    // Wait on a specific lwmutex
                    currentThread.wait.waitingOnLwMutex = true;
                    currentThread.wait.LwMutex_id = uid;
                    currentThread.wait.LwMutex_count = count;
                    currentThread.wait.waitStateChecker = lwMutexWaitStateChecker;
                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
                    threadMan.hleRescheduleCurrentThread(doCallbacks);
                } else {
                    if ((info.attr & PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE) != PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE) {
                        cpu.gpr[2] = ERROR_LWMUTEX_RECURSIVE_NOT_ALLOWED;
                    } else {
                        cpu.gpr[2] = ERROR_LWMUTEX_LOCKED;
                    }
                }
            } else {
                // Success, do not reschedule the current thread.
                if (log.isDebugEnabled()) {
                    log.debug("hleKernelLockLwMutex - '" + info.name + "' fast check succeeded");
                }
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelCreateLwMutex(int workAreaAddr, int name_addr, int attr, int count, int option_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringNZ(mem, name_addr, 32);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateLwMutex (workAreaAddr='" + Integer.toHexString(workAreaAddr) + "', name='" + name + "', attr=0x" + Integer.toHexString(attr) + ", count=0x" + Integer.toHexString(count) + ", option_addr=0x" + Integer.toHexString(option_addr) + ")");
        }

        SceKernelLwMutexInfo info = new SceKernelLwMutexInfo(workAreaAddr, name, count, attr);
        lwMutexMap.put(info.uid, info);

        // If the initial count is 0, the lwmutex is not acquired.
        if(count > 0) {
            info.threadid = Modules.ThreadManForUserModule.getCurrentThreadID();
        }

        // Return 0 in case of no error, do not return the UID of the created mutex
        cpu.gpr[2] = 0;
    }

    public void sceKernelDeleteLwMutex(int workAreaAddr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelDeleteLwMutex (workAreaAddr='" + Integer.toHexString(workAreaAddr) + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.remove(uid);
        if (info == null) {
            log.warn("sceKernelDeleteLwMutex unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            mem.write32(workAreaAddr, 0);  // Clear uid.
            cpu.gpr[2] = 0;
            onLwMutexDeleted(uid);
        }
    }

    public void sceKernelLockLwMutex(int workAreaAddr, int count, int timeout_addr) {
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelLockLwMutex redirecting to hleKernelLockLwMutex");
        }
        hleKernelLockLwMutex(uid, count, timeout_addr, true, false);
    }

    public void sceKernelLockLwMutexCB(int workAreaAddr, int count, int timeout_addr) {
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelLockLwMutexCB redirecting to hleKernelLockLwMutex");
        }
        hleKernelLockLwMutex(uid, count, timeout_addr, true, true);
    }

    public void sceKernelTryLockLwMutex(int workAreaAddr, int count) {
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelTryLockLwMutex redirecting to hleKernelLockLwMutex");
        }
        hleKernelLockLwMutex(uid, count, 0, false, false);
    }

    public void sceKernelUnlockLwMutex(int workAreaAddr, int count) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelUnlockLwMutex (workAreaAddr=0x" + Integer.toHexString(workAreaAddr) + ", count=" + count + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            log.warn("sceKernelUnlockLwMutex unknown uid");
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else if (info.lockedCount == 0) {
            log.warn("sceKernelUnlockLwMutex not locked");
            cpu.gpr[2] = ERROR_LWMUTEX_UNLOCKED;
        } else if (info.lockedCount < 0) {
            log.warn("sceKernelUnlockLwMutex underflow");
            cpu.gpr[2] = ERROR_LWMUTEX_UNLOCK_UNDERFLOW;
        } else {
            info.lockedCount -= count;
            cpu.gpr[2] = 0;
            if (info.lockedCount == 0) {
                onLwMutexModified(info);
            }
        }
    }

    public void sceKernelReferLwMutexStatus(int workAreaAddr, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferLwMutexStatus (workAreaAddr=0x" + Integer.toHexString(workAreaAddr) + ", addr=0x" + addr + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            log.warn("sceKernelReferLwMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            if (Memory.isAddressGood(addr)) {
                info.write(mem, addr);
                cpu.gpr[2] = 0;
            } else {
                log.warn("sceKernelReferLwMutexStatus bad address 0x" + Integer.toHexString(addr));
                cpu.gpr[2] = -1;
            }
        }
    }

    public void sceKernelReferLwMutexStatusByID(int uid, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferLwMutexStatus (uid=0x" + Integer.toHexString(uid) + ", addr=0x" + addr + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            log.warn("sceKernelReferLwMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            if (Memory.isAddressGood(addr)) {
                info.write(mem, addr);
                cpu.gpr[2] = 0;
            } else {
                log.warn("sceKernelReferLwMutexStatus bad address 0x" + Integer.toHexString(addr));
                cpu.gpr[2] = -1;
            }
        }
    }

    private class LwMutexWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the lwmutex
            // has been unlocked during the callback execution.
            SceKernelLwMutexInfo info = lwMutexMap.get(wait.LwMutex_id);
            if (info == null) {
                thread.cpuContext.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
                return false;
            }

            // Check the lwmutex.
            if (tryLockLwMutex(info, wait.LwMutex_count, thread)) {
                info.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final LwMutexManager singleton = new LwMutexManager();

    private LwMutexManager() {
    }

}