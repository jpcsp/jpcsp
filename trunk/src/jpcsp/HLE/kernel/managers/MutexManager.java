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
import java.util.Iterator;

import jpcsp.HLE.kernel.types.SceKernelMutexInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.ThreadMan;
import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.util.Utilities;

// http://forums.ps2dev.org/viewtopic.php?p=79708#79708
// TODO find ERROR_NOT_FOUND_MUTEX error code
// TODO find other codes like:
// - DONE mutex already locked 0x800201c4
// - mutex already unlocked
public class MutexManager {

    private HashMap<Integer, SceKernelMutexInfo> mutexMap;

    public void reset() {
        mutexMap = new HashMap<Integer, SceKernelMutexInfo>();
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
                Modules.log.warn("removing waiting thread " + Integer.toHexString(thread.uid)
                    + ", mutex " + Integer.toHexString(info.uid) + " numWaitThreads underflowed");
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

    /** returns a uid on success */
    public void sceKernelCreateMutex(int name_addr, int attr, int count, int option_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringNZ(mem, name_addr, 32);

        Modules.log.info("sceKernelCreateMutex(name='" + name
            + "',attr=0x" + Integer.toHexString(attr)
            + ",count=0x" + Integer.toHexString(count)
            + ",option_addr=0x" + Integer.toHexString(option_addr) + ")");

        // both attr can be used at the same time
        // 0x100 - ?
        // 0x200 - ?
        if (attr != 0) Modules.log.warn("PARTIAL:sceKernelCreateMutex attr value 0x" + Integer.toHexString(attr));

        SceKernelMutexInfo info = new SceKernelMutexInfo(name, attr);
        mutexMap.put(info.uid, info);

        info.locked = count;
        info.threadid = jpcsp.HLE.ThreadMan.getInstance().getCurrentThreadID();

        cpu.gpr[2] = info.uid;
        //Emulator.PauseEmu();
    }

    public void sceKernelDeleteMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        // should only be 1 parameter
        Modules.log.debug("sceKernelDeleteMutex UID " +Integer.toHexString(uid)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteMutex unknown UID " +Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_UNKNOWN_UID; // check
        } else {
            cpu.gpr[2] = 0;
        }
    }

    /** @return true if the mutex was successfully locked */
    private boolean tryLockMutex(SceKernelMutexInfo info, int count, boolean allowSameThread) {
        if (info.locked == 0 || allowSameThread) {
            info.locked += count;
            return true;
        } else {
            return false;
        }
    }

    /** TODO look for a timeout parameter, for now we assume infinite wait
     * @return true on success */
    private void hleKernelLockMutex(int uid, int count, int timeout_addr, boolean wait, boolean do_callbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        String message = "hleKernelLockMutex(uid=" + Integer.toHexString(uid)
            + ",count=" + count
            + ",timeout_addr=0x" + Integer.toHexString(timeout_addr)
            + ") wait=" + wait
            + ",cb=" + do_callbacks;

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn(message + " - unknown UID");
            cpu.gpr[2] = ERROR_UNKNOWN_UID; // check
        } else {
            ThreadMan threadMan = ThreadMan.getInstance();
            SceKernelThreadInfo current_thread = threadMan.getCurrentThread();

            // TODO check if the special flag is 0x100 or 0x200, I doubt it's both (fiveofhearts)
            boolean allowSameThread = (info.attr & 0x300) == 0x300 && info.threadid == current_thread.uid;

            if (!tryLockMutex(info, count, allowSameThread)) {
                Modules.log.info(message + " - '" + info.name + "' fast check failed");

                if (wait) {
                    //ThreadMan threadMan = ThreadMan.getInstance();
                    //SceKernelThreadInfo current_thread = threadMan.getCurrentThread();

                    // Failed, but it's ok, just wait a little
                    info.numWaitThreads++;

                    // Do callbacks?
                    current_thread.do_callbacks = do_callbacks;

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

                    threadMan.hleKernelThreadWait(current_thread.wait, timeout, forever);

                    // Wait on a specific mutex
                    current_thread.wait.waitingOnMutex = true;
                    current_thread.wait.Mutex_id = uid;

                    threadMan.changeThreadState(current_thread, PSP_THREAD_WAITING);
                    threadMan.contextSwitch(threadMan.nextThread());

                    // doesn't really matter what we set this to, it's going to get changed before the thread will run again
                    cpu.gpr[2] = 0;
                } else {
                    // sceKernelTryLockMutex
                    cpu.gpr[2] = ERROR_MUTEX_LOCKED;
                }
            } else {
                Modules.log.debug(message + " - '" + info.name + "' fast check succeeded");
                info.threadid = current_thread.uid;
                cpu.gpr[2] = 0;
            }
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

    private void wakeWaitMutexThreads(SceKernelMutexInfo info, boolean wakeMultiple) {
        boolean handled = false;

        if (info.numWaitThreads < 0) {
            Modules.log.error("info.numWaitThreads < 0 (" + info.numWaitThreads + ")");
            // TODO should probably think about adding a kernel or hle error code
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
            return;
        }

        if (info.numWaitThreads == 0) {
            Modules.log.debug("wakeWaitMutexThreads(multiple=" + wakeMultiple + ") mutex:'" + info.name + "' fast exit (numWaitThreads == 0)");
            return;
        }

        for (Iterator<SceKernelThreadInfo> it = ThreadMan.getInstance().iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            // We're assuming if waitingOnMutex is set then thread.status = waiting
            if (thread.wait.waitingOnMutex &&
                thread.wait.Mutex_id == info.uid) {

                // Update numWaitThreads
                info.numWaitThreads--;

                // Untrack
                thread.wait.waitingOnMutex = false;

                // Return failure
                thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;

                // Wakeup
                ThreadMan.getInstance().changeThreadState(thread, PSP_THREAD_READY);

                Modules.log.info("wakeWaitMutexThreads(multiple=" + wakeMultiple + ") mutex:'" + info.name + "' waking thread:'" + thread.name + "'");
                handled = true;

                if (!wakeMultiple)
                    break;
            }
        }

        if (!handled)
            Modules.log.error("wakeWaitMutexThreads(multiple=" + wakeMultiple + ") mutex:'" + info.name + "' no threads to wake");
    }

    public void sceKernelUnlockMutex(int uid, int count) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelUnlockMutex(uid=" + Integer.toHexString(uid) + ",count=" + count + ")");

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelUnlockMutex unknown uid");
            cpu.gpr[2] = -1;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelUnlockMutex not locked");
            cpu.gpr[2] = 0; // check
        } else {
            info.locked -= count;
            if (info.locked < 0) {
                Modules.log.warn("sceKernelUnlockMutex underflow " + info.locked);
                info.locked  = 0;
            }

            if (info.locked == 0) {
                // wake one thread waiting on this mutex
                wakeWaitMutexThreads(info, false);
            }

            cpu.gpr[2] = 0;
        }
        //Emulator.PauseEmu();
    }

    public void sceKernelCancelMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("PARTIAL:sceKernelCancelMutex UID " + Integer.toHexString(uid)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelMutex unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelCancelMutex UID " + Integer.toHexString(uid) + " not locked");
            cpu.gpr[2] = -1;
        } else {
            info.locked = 0; // check

            // wake all threads waiting on this mutex
            wakeWaitMutexThreads(info, true);

            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferMutexStatus(int uid, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        // the problem here is we don't know the layout of SceKernelMutexInfo so what we're writing is probably wrong
        Modules.log.warn("PARTIAL:sceKernelReferMutexStatus UID " + Integer.toHexString(uid)
            + "addr " + String.format("0x%08X", addr)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
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


    public static final MutexManager singleton;

    private MutexManager() {
    }

    static {
        singleton = new MutexManager();
    }
}
