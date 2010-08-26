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
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_LOCK_OVERFLOW;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_RECURSIVE_NOT_ALLOWED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_UNLOCKED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_LWMUTEX_UNLOCK_UNDERFLOW;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
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

public class LwMutexManager {

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
                Modules.log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", lwmutex " + Integer.toHexString(info.uid) + " numWaitThreads underflowed");
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
            Modules.log.warn("LwMutex deleted while we were waiting for it! (timeout expired)");
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

    private boolean tryLockLwMutex(SceKernelLwMutexInfo info, int count, SceKernelThreadInfo thread) {
        if (info.locked == 0 || (info.attr & PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE) == PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE) {
            info.threadid = thread.uid;
            info.locked += count;
            return true;
        }
        return false;
    }

    private void wakeWaitMutexThreads(SceKernelLwMutexInfo info, boolean wakeAll) {
        if (info.numWaitThreads <= 0) {
            return;
        }

        if (wakeAll) {
            for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_LWMUTEX &&
                        thread.wait.waitingOnLwMutex &&
                        thread.wait.LwMutex_id == info.uid) {
                    // Update numWaitThreads
                    info.numWaitThreads--;
                    // Untrack
                    thread.wait.waitingOnLwMutex = false;
                    // Return success or failure
                    thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
                    // Wakeup
                    Modules.ThreadManForUserModule.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
            // No thread is having control of LwMutex
            info.threadid = -1;
        } else {

            if ((info.attr & PSP_LWMUTEX_ATTR_FIFO) == PSP_LWMUTEX_ATTR_FIFO) {
                for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iterator(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();
                    if (thread.waitType == PSP_WAIT_LWMUTEX &&
                            thread.wait.waitingOnLwMutex &&
                            thread.wait.LwMutex_id == info.uid &&
                            tryLockLwMutex(info, thread.wait.LwMutex_count, thread)) {
                        // Update numWaitThreads
                        info.numWaitThreads--;
                        // Untrack
                        thread.wait.waitingOnLwMutex = false;
                        // Return success or failure
                        thread.cpuContext.gpr[2] = 0;
                        // Wakeup
                        Modules.ThreadManForUserModule.hleChangeThreadState(thread, PSP_THREAD_READY);
                        break;
                    }
                }
            } else if ((info.attr & PSP_LWMUTEX_ATTR_PRIORITY) == PSP_LWMUTEX_ATTR_PRIORITY) {
                for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iteratorByPriority(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();
                    if (thread.waitType == PSP_WAIT_LWMUTEX &&
                            thread.wait.waitingOnLwMutex &&
                            thread.wait.LwMutex_id == info.uid &&
                            tryLockLwMutex(info, thread.wait.LwMutex_count, thread)) {
                        // Update numWaitThreads
                        info.numWaitThreads--;
                        // Untrack
                        thread.wait.waitingOnLwMutex = false;
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

    private void hleKernelLockLwMutex(int uid, int count, int timeout_addr, boolean wait, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        String message = "hleKernelLockLwMutex(uid=" + Integer.toHexString(uid) + ",count=" + count + ",timeout_addr=0x" + Integer.toHexString(timeout_addr) + ") wait=" + wait + ",cb=" + doCallbacks;

        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            Modules.log.warn(message + " - unknown UID");
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

            if (!tryLockLwMutex(info, count, currentThread)) {
                if (Modules.log.isDebugEnabled()) {
                    Modules.log.debug(message + " - '" + info.name + "' fast check failed");
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
                        if (mem.isAddressGood(timeout_addr)) {
                            timeout = mem.read32(timeout_addr);
                        } else {
                            Modules.log.warn(message + " - bad timeout address");
                        }
                    }
                    threadMan.hleKernelThreadWait(currentThread, timeout, forever);
                    // Wait on a specific mutex
                    currentThread.wait.waitingOnLwMutex = true;
                    currentThread.wait.LwMutex_id = uid;
                    currentThread.wait.LwMutex_count = count;
                    currentThread.wait.waitStateChecker = lwMutexWaitStateChecker;
                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);

                    cpu.gpr[2] = 0;
                } else {
                    if ((info.attr & PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE) != PSP_LWMUTEX_ATTR_ALLOW_RECURSIVE) {
                        cpu.gpr[2] = ERROR_LWMUTEX_RECURSIVE_NOT_ALLOWED;
                    } else {
                        cpu.gpr[2] = ERROR_LWMUTEX_LOCK_OVERFLOW;
                    }
                }
            } else {
                Modules.log.debug(message + " - '" + info.name + "' fast check succeeded");
                cpu.gpr[2] = 0;
            }
            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelCreateLwMutex(int workAreaAddr, int name_addr, int attr, int count, int option_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringNZ(mem, name_addr, 32);

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelCreateLwMutex (workAreaAddr='" + Integer.toHexString(workAreaAddr) + "', name='" + name + "', attr=0x" + Integer.toHexString(attr) + ", count=0x" + Integer.toHexString(count) + ", option_addr=0x" + Integer.toHexString(option_addr) + ")");
        }

        SceKernelLwMutexInfo info = new SceKernelLwMutexInfo(workAreaAddr, name, attr);
        lwMutexMap.put(info.uid, info);

        info.locked = count;
        info.threadid = Modules.ThreadManForUserModule.getCurrentThreadID();

        cpu.gpr[2] = info.uid;
    }

    public void sceKernelDeleteLwMutex(int workAreaAddr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelDeleteLwMutex (workAreaAddr='" + Integer.toHexString(workAreaAddr) + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteLwMutex unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            mem.write32(workAreaAddr, 0);  // Clear uid.
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelLockLwMutex(int workAreaAddr, int count, int timeout_addr) {
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        Modules.log.debug("sceKernelLockLwMutex redirecting to hleKernelLockLwMutex");
        hleKernelLockLwMutex(uid, count, timeout_addr, true, false);
    }

    public void sceKernelLockLwMutexCB(int workAreaAddr, int count, int timeout_addr) {
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        Modules.log.debug("sceKernelLockLwMutexCB redirecting to hleKernelLockLwMutex");
        hleKernelLockLwMutex(uid, count, timeout_addr, true, true);
    }

    public void sceKernelTryLockLwMutex(int workAreaAddr, int count) {
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        Modules.log.debug("sceKernelTryLockLwMutex redirecting to hleKernelLockLwMutex");
        hleKernelLockLwMutex(uid, count, 0, false, false);
    }

    public void sceKernelUnlockLwMutex(int workAreaAddr, int count) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelUnlockLwMutex (workAreaAddr=0x" + Integer.toHexString(workAreaAddr) + ", count=" + count + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelUnlockLwMutex unknown uid");
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelUnlockLwMutex not locked");
            cpu.gpr[2] = ERROR_LWMUTEX_UNLOCKED;
        } else {
            info.locked -= count;
            if (info.locked < 0) {
                Modules.log.warn("sceKernelUnlockLwMutex underflow " + info.locked);
                info.locked = ERROR_LWMUTEX_UNLOCK_UNDERFLOW;
            } else if (info.locked == 0) {
                wakeWaitMutexThreads(info, false);
                cpu.gpr[2] = 0;
            } else {
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelReferLwMutexStatus(int workAreaAddr, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(workAreaAddr);

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelReferLwMutexStatus (workAreaAddr=0x" + Integer.toHexString(workAreaAddr) + ", addr=0x" + addr + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferLwMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            if (mem.isAddressGood(addr)) {
                info.write(mem, addr);
                cpu.gpr[2] = 0;
            } else {
                Modules.log.warn("sceKernelReferLwMutexStatus bad address 0x" + Integer.toHexString(addr));
                cpu.gpr[2] = -1;
            }
        }
    }

    public void sceKernelReferLwMutexStatusByID(int uid, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelReferLwMutexStatus (uid=0x" + Integer.toHexString(uid) + ", addr=0x" + addr + ")");
        }

        SceKernelLwMutexInfo info = lwMutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferLwMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_LWMUTEX_NOT_FOUND;
        } else {
            if (mem.isAddressGood(addr)) {
                info.write(mem, addr);
                cpu.gpr[2] = 0;
            } else {
                Modules.log.warn("sceKernelReferLwMutexStatus bad address 0x" + Integer.toHexString(addr));
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

            // Check the lwmutex
            if (!tryLockLwMutex(info, wait.LwMutex_count, thread)) {
                info.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final LwMutexManager singleton;

    private LwMutexManager() {
    }


    static {
        singleton = new LwMutexManager();
    }
}