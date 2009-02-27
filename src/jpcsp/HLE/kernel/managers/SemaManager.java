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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;

public class SemaManager {

    private HashMap<Integer, SceKernelSemaInfo> semaMap;

    public void reset() {
        semaMap = new HashMap<Integer, SceKernelSemaInfo>();
    }

    public void sceKernelCreateSema(int name_addr, int attr, int initVal, int maxVal, int option)
    {
        String name = readStringNZ(name_addr, 32);
        Modules.log.debug("sceKernelCreateSema name= " + name + " attr= 0x" + Integer.toHexString(attr) + " initVal= " + initVal + " maxVal= "+ maxVal + " option= 0x" + Integer.toHexString(option));

        if (attr != 0) Modules.log.warn("UNIMPLEMENTED:sceKernelCreateSema attr value 0x" + Integer.toHexString(attr));

        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(option)) {
            // The first int does not seem to be the size of the struct, found values:
            // SSX On Tour: 0, 0x08B0F9E4, 0x0892E664, 0x08AF7257 (some values are used in more than one semaphore)
            int optsize = mem.read32(option);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateSema option at 0x" + Integer.toHexString(option)
                + " (size=" + optsize + ")");
        }

        SceKernelSemaInfo sema = new SceKernelSemaInfo(name, attr, initVal, maxVal);
        semaMap.put(sema.uid, sema);

        Emulator.getProcessor().cpu.gpr[2] = sema.uid;
    }

    public void sceKernelDeleteSema(int semaid)
    {
        Modules.log.debug("sceKernelDeleteSema id=0x" + Integer.toHexString(semaid));

        if (semaid <= 0) {
            Modules.log.warn("sceKernelDeleteSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.remove(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelDeleteSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            if (sema.numWaitThreads > 0) {
                Modules.log.warn("sceKernelDeleteSema numWaitThreads " + sema.numWaitThreads);

                // Find threads waiting on this sema and wake them up
                ThreadMan threadMan = ThreadMan.getInstance();
                for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
                    SceKernelThreadInfo thread = it.next();

                    if (thread.wait.waitingOnSemaphore &&
                        thread.wait.Semaphore_id == semaid) {
                        // Untrack
                        thread.wait.waitingOnSemaphore = false;

                        // Return WAIT_DELETE
                        thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;

                        // Wakeup
                        threadMan.changeThreadState(thread, PSP_THREAD_READY);
                    }
                }
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** May modify sema.currentCount
     * @return true on success */
    private boolean tryWaitSemaphore(SceKernelSemaInfo sema, int signal)
    {
        boolean success = false;

        if (sema.currentCount >= signal) {
            sema.currentCount -= signal;
            success = true;
        }

        return success;
    }

    private void hleKernelWaitSema(int semaid, int signal, int timeout_addr, boolean do_callbacks)
    {
        Modules.log.debug("hleKernelWaitSema(id=0x" + Integer.toHexString(semaid)
            + ",signal=" + signal
            + ",timeout=0x" + Integer.toHexString(timeout_addr)
            + ") callbacks=" + do_callbacks);

        if (signal <= 0) {
            Modules.log.warn("hleKernelWaitSema - bad signal " + signal);
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_COUNT;
            return;
        }

        if (semaid <= 0) {
            Modules.log.warn("hleKernelWaitSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            Modules.log.warn("hleKernelWaitSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            ThreadMan threadMan = ThreadMan.getInstance();
            Memory mem = Memory.getInstance();
            int micros = 0;

            if (mem.isAddressGood(timeout_addr)) {
                micros = mem.read32(timeout_addr);
            }

            if (!tryWaitSemaphore(sema, signal))
            {
                // Failed, but it's ok, just wait a little
                Modules.log.debug("hleKernelWaitSema - '" + sema.name + "' fast check failed");
                sema.numWaitThreads++;

                SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

                // Do callbacks?
                currentThread.do_callbacks = do_callbacks;

                // Go to wait state
                threadMan.hleKernelThreadWait(currentThread.wait, micros, (timeout_addr == 0));

                // Wait on a specific semaphore
                currentThread.wait.waitingOnSemaphore = true;
                currentThread.wait.Semaphore_id = semaid;
                currentThread.wait.Semaphore_signal = signal;

                threadMan.changeThreadState(currentThread, PSP_THREAD_WAITING);

                threadMan.contextSwitch(threadMan.nextThread());
            }
            else
            {
                // Success
                Modules.log.debug("hleKernelWaitSema - '" + sema.name + "' fast check succeeded");
                Emulator.getProcessor().cpu.gpr[2] = 0;

                if (!threadMan.isInsideCallback()) {
                    if (do_callbacks) {
                        threadMan.yieldCurrentThreadCB();
                    } else {
                        // TODO yield anyway?
                        //yieldCurrentThread();
                    }
                } else {
                    Modules.log.warn("hleKernelWaitSema called from inside callback!");
                }
            }
        }
    }

    public void sceKernelWaitSema(int semaid, int signal, int timeout_addr)
    {
        //Modules.log.debug("sceKernelWaitSema redirecting to hleKernelWaitSema(callbacks=false)");
        hleKernelWaitSema(semaid, signal, timeout_addr, false);
    }

    public void sceKernelWaitSemaCB(int semaid, int signal, int timeout_addr)
    {
        //Modules.log.debug("sceKernelWaitSemaCB redirecting to hleKernelWaitSema(callbacks=true)");
        hleKernelWaitSema(semaid, signal, timeout_addr, true);
    }

    public void sceKernelSignalSema(int semaid, int signal)
    {
        if (signal <= 0) {
            Modules.log.warn("sceKernelSignalSema - id=0x" + Integer.toHexString(semaid) + " bad signal " + signal);
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_COUNT;
            return;
        }

        if (semaid <= 0) {
            Modules.log.warn("sceKernelSignalSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelSignalSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
//        } else if (sema.currentCount + signal > sema.maxCount) {
//            Modules.log.warn("sceKernelSignalSema - overflow uid 0x" + Integer.toHexString(semaid) + " name='" + sema.name + "' signal=" + signal + " current=" + sema.currentCount + " max=" + sema.maxCount);
//            Emulator.getProcessor().cpu.gpr[2] = ERROR_SEMA_OVERFLOW;
//            // TODO clamp and continue anyway?
        } else {
            boolean yield = false;
            Modules.log.debug("sceKernelSignalSema id=0x" + Integer.toHexString(semaid) + " name='" + sema.name + "' signal=" + signal);

            sema.currentCount += signal;
            if (sema.currentCount > sema.maxCount)
                sema.currentCount = sema.maxCount;

            // For each thread (sorted by priority),
            // if the thread is waiting on this semaphore,
            // and signal <= currentCount,
            // then wake up the thread and adjust currentCount.
            // repeat for all remaining threads or until currentCount = 0.
            ThreadMan threadMan = ThreadMan.getInstance();
            int currentThreadCurrentPriority = threadMan.getCurrentThread().currentPriority;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnSemaphore &&
                    thread.wait.Semaphore_id == semaid &&
                    thread.wait.Semaphore_signal <= sema.currentCount) {

                    Modules.log.debug("sceKernelSignalSema waking thread 0x" + Integer.toHexString(thread.uid)
                        + " name:'" + thread.name + "'");

                    // Untrack
                    thread.wait.waitingOnSemaphore = false;

                    // Return success
                    thread.cpuContext.gpr[2] = 0;

                    // Wakeup
                    threadMan.changeThreadState(thread, PSP_THREAD_READY);

                    if (thread.currentPriority < currentThreadCurrentPriority)
                        yield = true;

                    // Adjust sema
                    sema.currentCount -= thread.wait.Semaphore_signal;
                    sema.numWaitThreads--;
                    if (sema.currentCount == 0)
                        break;
                } else if (thread.status == PSP_THREAD_READY && thread.currentPriority < currentThreadCurrentPriority) {
                    yield = true;
                }
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
            if (yield) {
                Modules.log.debug("sceKernelSignalSema yielding to thread with higher priority");
                threadMan.yieldCurrentThread();
            }
        }
    }

    /** This is attempt to signal the sema and always return immediately */
    public void sceKernelPollSema(int semaid, int signal)
    {
        Modules.log.debug("sceKernelPollSema id= 0x" + Integer.toHexString(semaid) + " signal= " + signal);

        if (signal <= 0) {
            Modules.log.warn("sceKernelPollSema - bad signal " + signal);
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_COUNT;
            return;
        }

        if (semaid <= 0) {
            Modules.log.warn("sceKernelPollSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelPollSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else if (sema.currentCount - signal < 0) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_SEMA_ZERO;
        } else {
            sema.currentCount -= signal;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelCancelSema(int semaid)
    {
        Modules.log.debug("sceKernelCancelSema id= 0x" + Integer.toHexString(semaid));

        if (semaid <= 0) {
            Modules.log.warn("sceKernelCancelSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelCancelSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            sema.numWaitThreads = 0;

            // Find threads waiting on this sema and wake them up
            ThreadMan threadMan = ThreadMan.getInstance();
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnSemaphore &&
                    thread.wait.Semaphore_id == semaid) {
                    // Untrack
                    thread.wait.waitingOnSemaphore = false;

                    // Return WAIT_CANCELLED
                    thread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;

                    // Wakeup
                    threadMan.changeThreadState(thread, PSP_THREAD_READY);
                }
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferSemaStatus(int semaid, int addr)
    {
        Modules.log.debug("sceKernelReferSemaStatus id= 0x" + Integer.toHexString(semaid) + " addr= 0x" + Integer.toHexString(addr));

        if (semaid <= 0) {
            Modules.log.warn("sceKernelReferSemaStatus bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelReferSemaStatus - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            sema.write(Memory.getInstance(), addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public static final SemaManager singleton;

    private SemaManager() {
    }

    static {
        singleton = new SemaManager();
    }
}
