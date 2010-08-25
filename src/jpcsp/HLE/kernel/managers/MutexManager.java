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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MUTEX_LOCK_OVERFLOW;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MUTEX_UNLOCKED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MUTEX_NOT_FOUND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MUTEX_RECURSIVE_NOT_ALLOWED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MUTEX_UNLOCK_UNDERFLOW;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MUTEX;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelMutexInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;

public class MutexManager {

    private HashMap<Integer, SceKernelMutexInfo> mutexMap;
    private MutexWaitStateChecker mutexWaitStateChecker;
    private final static int PSP_MUTEX_ATTR_FIFO = 0;
    private final static int PSP_MUTEX_ATTR_PRIORITY = 0x100;
    private final static int PSP_MUTEX_ATTR_ALLOW_RECURSIVE = 0x200;

    public void reset() {
        mutexMap = new HashMap<Integer, SceKernelMutexInfo>();
        mutexWaitStateChecker = new MutexWaitStateChecker();
    }

    /** Don't call this unless thread.wait.waitingOnMutex == true
     * @return true if the thread was waiting on a valid mutex */
    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        // Untrack
        thread.wait.waitingOnMutex = false;
        // Update numWaitThreads
        SceKernelMutexInfo info = mutexMap.get(thread.wait.Mutex_id);
        if (info != null) {
            info.numWaitThreads--;
            if (info.numWaitThreads < 0) {
                Modules.log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", mutex " + Integer.toHexString(info.uid) + " numWaitThreads underflowed");
                info.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    /** Don't call this unless thread.wait.waitingOnMutex == true */
    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            Modules.log.warn("Mutex deleted while we were waiting for it! (timeout expired)");
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

    private boolean tryLockMutex(SceKernelMutexInfo info, int count, SceKernelThreadInfo thread) {
        // Allow Mutex locking when not locked or when using ALLOW_RECURSIVE.
        if (info.locked == 0 || info.attr == PSP_MUTEX_ATTR_ALLOW_RECURSIVE) {
            info.threadid = thread.uid;
            info.locked += count;
            return true;
        }
        return false;
    }

    private void wakeWaitMutexThreads(SceKernelMutexInfo info, boolean wakeAll) {
        if (info.numWaitThreads <= 0) {
            return;
        }

        if (wakeAll) {
            for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_MUTEX &&
                        thread.wait.waitingOnMutex &&
                        thread.wait.Mutex_id == info.uid) {
                    // Update numWaitThreads
                    info.numWaitThreads--;
                    // Untrack
                    thread.wait.waitingOnMutex = false;
                    // Return success or failure
                    thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
                    // Wakeup
                    Modules.ThreadManForUserModule.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
            // No thread is having control of Mutex
            info.threadid = -1;
        } else {

            if ((info.attr & PSP_MUTEX_ATTR_FIFO) == PSP_MUTEX_ATTR_FIFO) {
                for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iterator(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();
                    if (thread.waitType == PSP_WAIT_MUTEX &&
                            thread.wait.waitingOnMutex &&
                            thread.wait.Mutex_id == info.uid &&
                            tryLockMutex(info, thread.wait.Mutex_count, thread)) {
                        // Update numWaitThreads
                        info.numWaitThreads--;
                        // Untrack
                        thread.wait.waitingOnMutex = false;
                        // Return success or failure
                        thread.cpuContext.gpr[2] = 0;
                        // Wakeup
                        Modules.ThreadManForUserModule.hleChangeThreadState(thread, PSP_THREAD_READY);
                        break;
                    }
                }
            } else if ((info.attr & PSP_MUTEX_ATTR_PRIORITY) == PSP_MUTEX_ATTR_PRIORITY) {
                for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iteratorByPriority(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();
                    if (thread.waitType == PSP_WAIT_MUTEX &&
                            thread.wait.waitingOnMutex &&
                            thread.wait.Mutex_id == info.uid &&
                            tryLockMutex(info, thread.wait.Mutex_count, thread)) {
                        // Update numWaitThreads
                        info.numWaitThreads--;
                        // Untrack
                        thread.wait.waitingOnMutex = false;
                        // Return success or failure
                        thread.cpuContext.gpr[2] = 0;
                        // Wakeup
                        Modules.ThreadManForUserModule.hleChangeThreadState(thread, PSP_THREAD_READY);
                        break;
                    }
                }
            }
        }
    }

    public void sceKernelCreateMutex(int name_addr, int attr, int count, int option_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = "";
        if (mem.isAddressGood(name_addr)) {
            name = Utilities.readStringNZ(mem, name_addr, 32);
        }

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelCreateMutex(name='" + name + "',attr=0x" + Integer.toHexString(attr) + ",count=0x" + Integer.toHexString(count) + ",option_addr=0x" + Integer.toHexString(option_addr) + ")");
        }

        SceKernelMutexInfo info = new SceKernelMutexInfo(name, attr);
        mutexMap.put(info.uid, info);

        info.locked = count;
        info.threadid = Modules.ThreadManForUserModule.getCurrentThreadID();

        cpu.gpr[2] = info.uid;
    }

    public void sceKernelDeleteMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelDeleteMutex(uid=" + Integer.toHexString(uid));
        }

        SceKernelMutexInfo info = mutexMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteMutex unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_MUTEX_NOT_FOUND;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    private void hleKernelLockMutex(int uid, int count, int timeout_addr, boolean wait, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        String message = "hleKernelLockMutex(uid=" + Integer.toHexString(uid) + ",count=" + count + ",timeout_addr=0x" + Integer.toHexString(timeout_addr) + ") wait=" + wait + ",cb=" + doCallbacks;

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn(message + " - unknown UID");
            cpu.gpr[2] = ERROR_MUTEX_NOT_FOUND;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

            if (!tryLockMutex(info, count, currentThread)) {
                if (Modules.log.isDebugEnabled()) {
                    Modules.log.debug(message + " - '" + info.name + "' fast check failed");
                }
                if (wait) {
                    // Failed, but it's ok, just wait a little
                    info.numWaitThreads++;
                    // wait type
                    currentThread.waitType = PSP_WAIT_MUTEX;
                    currentThread.waitId = uid;
                    // Go to wait state
                    int timeout = 0;
                    boolean forever = (timeout_addr == 0);
                    if (timeout_addr != 0) {
                        if (mem.isAddressGood(timeout_addr)) {
                            timeout = mem.read32(timeout_addr);
                        } else {
                            Modules.log.warn(message + " - bad timeout address");
                        }
                    }
                    threadMan.hleKernelThreadWait(currentThread, timeout, forever);
                    // Wait on a specific mutex
                    currentThread.wait.waitingOnMutex = true;
                    currentThread.wait.Mutex_id = uid;
                    currentThread.wait.Mutex_count = count;
                    currentThread.wait.waitStateChecker = mutexWaitStateChecker;
                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);

                    cpu.gpr[2] = 0;
                } else {
                    if (info.attr != PSP_MUTEX_ATTR_ALLOW_RECURSIVE) {
                        cpu.gpr[2] = ERROR_MUTEX_RECURSIVE_NOT_ALLOWED;
                    } else {
                        cpu.gpr[2] = ERROR_MUTEX_LOCK_OVERFLOW;
                    }
                }
            } else {
                Modules.log.debug(message + " - '" + info.name + "' fast check succeeded");
                cpu.gpr[2] = 0;
            }
            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelLockMutex(int uid, int count, int timeout_addr) {
        Modules.log.debug("sceKernelLockMutex redirecting to hleKernelLockMutex");
        hleKernelLockMutex(uid, count, timeout_addr, true, false);
    }

    public void sceKernelLockMutexCB(int uid, int count, int timeout_addr) {
        Modules.log.debug("sceKernelLockMutex redirecting to hleKernelLockMutex");
        hleKernelLockMutex(uid, count, timeout_addr, true, true);
    }

    public void sceKernelTryLockMutex(int uid, int count) {
        Modules.log.debug("sceKernelTryLockMutex redirecting to hleKernelLockMutex");
        hleKernelLockMutex(uid, count, 0, false, false);
    }

    public void sceKernelUnlockMutex(int uid, int count) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelUnlockMutex(uid=" + Integer.toHexString(uid) + ", count=" + count + ")");

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelUnlockMutex unknown uid");
            cpu.gpr[2] = ERROR_MUTEX_NOT_FOUND;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelUnlockMutex not locked");
            cpu.gpr[2] = ERROR_MUTEX_UNLOCKED;
        } else {
            info.locked -= count;
            if (info.locked < 0) {
                Modules.log.warn("sceKernelUnlockMutex underflow " + info.locked);
                cpu.gpr[2] = ERROR_MUTEX_UNLOCK_UNDERFLOW;
            } else if (info.locked == 0) {
                // Wake the next thread waiting on this mutex.
                wakeWaitMutexThreads(info, false);
                cpu.gpr[2] = 0;
            } else {
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelCancelMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("PARTIAL: sceKernelCancelMutex uid=" + Integer.toHexString(uid));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelMutex unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_MUTEX_NOT_FOUND;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelCancelMutex UID " + Integer.toHexString(uid) + " not locked");
            cpu.gpr[2] = -1;
        } else {
            info.locked = 0;
            // Wake all threads waiting on this mutex.
            wakeWaitMutexThreads(info, true);

            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferMutexStatus(int uid, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("PARTIAL: sceKernelReferMutexStatus uid=" + Integer.toHexString(uid) + "addr=" + String.format("0x%08X", addr));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_MUTEX_NOT_FOUND;
        } else {
            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(addr)) {
                info.write(mem, addr);
                cpu.gpr[2] = 0;
            } else {
                Modules.log.warn("sceKernelReferMutexStatus bad address 0x" + Integer.toHexString(addr));
                cpu.gpr[2] = -1;
            }
        }
    }

    private class MutexWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the mutex
            // has been unlocked during the callback execution.
            SceKernelMutexInfo info = mutexMap.get(wait.Mutex_id);
            if (info == null) {
                thread.cpuContext.gpr[2] = ERROR_MUTEX_NOT_FOUND;
                return false;
            }

            // Check the mutex
            if (!tryLockMutex(info, wait.Mutex_count, thread)) {
                info.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final MutexManager singleton;

    private MutexManager() {
    }


    static {
        singleton = new MutexManager();
    }
}