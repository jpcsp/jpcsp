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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_COUNT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_SEMAPHORE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_SEMA_ZERO;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_SEMA;
import static jpcsp.util.Utilities.readStringNZ;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelSemaInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;

import org.apache.log4j.Logger;

public class SemaManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelSemaInfo> semaMap;
    private SemaWaitStateChecker semaWaitStateChecker;

    private final static int PSP_SEMA_ATTR_FIFO = 0;           // Signal waiting threads with a FIFO iterator.
    private final static int PSP_SEMA_ATTR_PRIORITY = 0x100;   // Signal waiting threads with a priority based iterator.

    public void reset() {
        semaMap = new HashMap<Integer, SceKernelSemaInfo>();
        semaWaitStateChecker = new SemaWaitStateChecker();
    }

    /** Don't call this unless thread.wait.waitingOnSemaphore == true
     * @return true if the thread was waiting on a valid sema */
    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        // Untrack
        thread.wait.waitingOnSemaphore = false;
        // Update numWaitThreads
        SceKernelSemaInfo sema = semaMap.get(thread.wait.Semaphore_id);
        if (sema != null) {
            sema.numWaitThreads--;

            if (sema.numWaitThreads < 0) {
                log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", sema " + Integer.toHexString(sema.uid) + " numWaitThreads underflowed");
                sema.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    /** Don't call this unless thread.wait.waitingOnSemaphore == true */
    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            log.warn("Sema deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnSemaphore) {
            // decrement numWaitThreads
            removeWaitingThread(thread);
        }
    }

    private boolean tryWaitSemaphore(SceKernelSemaInfo sema, int signal) {
        boolean success = false;
        if (sema.currentCount >= signal) {
            sema.currentCount -= signal;
            success = true;
        }
        return success;
    }

    public void sceKernelCreateSema(int name_addr, int attr, int initVal, int maxVal, int option) {
        String name;
        if (name_addr == 0) {
            log.info("sceKernelCreateSema name address is 0! Assuming empty name");
            name = "";
        } else {
            name = readStringNZ(name_addr, 32);
        }
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateSema name= " + name + " attr= 0x" + Integer.toHexString(attr) + " initVal= " + initVal + " maxVal= " + maxVal + " option= 0x" + Integer.toHexString(option));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelCreateSema called insided an interrupt");
            }
            Emulator.getProcessor().cpu.gpr[2] = ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        Memory mem = Memory.getInstance();
        if (Memory.isAddressGood(option)) {
            // The first int does not seem to be the size of the struct, found values:
            // SSX On Tour: 0, 0x08B0F9E4, 0x0892E664, 0x08AF7257 (some values are used in more than one semaphore)
            int optsize = mem.read32(option);
            log.warn("sceKernelCreateSema option at 0x" + Integer.toHexString(option) + " (size=" + optsize + ")");
        }

        SceKernelSemaInfo sema = new SceKernelSemaInfo(name, attr, initVal, maxVal);
        semaMap.put(sema.uid, sema);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateSema name= " + name + " created with uid=0x" + Integer.toHexString(sema.uid));
        }

        Emulator.getProcessor().cpu.gpr[2] = sema.uid;
    }

    public void sceKernelDeleteSema(int semaid) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelDeleteSema id=0x" + Integer.toHexString(semaid));
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.remove(semaid);
        if (sema == null) {
            log.warn("sceKernelDeleteSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            if (sema.numWaitThreads > 0) {
                log.warn("sceKernelDeleteSema numWaitThreads " + sema.numWaitThreads);

                // Find threads waiting on this sema and wake them up
                ThreadManForUser threadMan = Modules.ThreadManForUserModule;
                for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();

                    if (thread.wait.waitingOnSemaphore &&
                            thread.wait.Semaphore_id == semaid) {
                        // Untrack
                        thread.wait.waitingOnSemaphore = false;
                        // Return WAIT_DELETE
                        thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
                        // Wakeup
                        threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    }
                }
            }
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private void hleKernelWaitSema(int semaid, int signal, int timeout_addr, boolean doCallbacks) {
        if (log.isDebugEnabled()) {
            log.debug("hleKernelWaitSema(id=0x" + Integer.toHexString(semaid) + ",signal=" + signal + ",timeout=0x" + Integer.toHexString(timeout_addr) + ") callbacks=" + doCallbacks);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelWaitSema called insided an interrupt");
            }
            Emulator.getProcessor().cpu.gpr[2] = ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        if (signal <= 0) {
            log.warn("hleKernelWaitSema - bad signal " + signal);
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_COUNT;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            log.warn("hleKernelWaitSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            Memory mem = Memory.getInstance();
            int micros = 0;

            if (Memory.isAddressGood(timeout_addr)) {
                micros = mem.read32(timeout_addr);
            }

            if (!tryWaitSemaphore(sema, signal)) {
                // Failed, but it's ok, just wait a little
                if (log.isDebugEnabled()) {
                    log.debug("hleKernelWaitSema - '" + sema.name + "' fast check failed");
                }
                sema.numWaitThreads++;

                SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

                // wait type
                currentThread.waitType = PSP_WAIT_SEMA;
                currentThread.waitId = semaid;

                // Go to wait state
                threadMan.hleKernelThreadWait(currentThread, micros, (timeout_addr == 0));

                // Wait on a specific semaphore
                currentThread.wait.waitingOnSemaphore = true;
                currentThread.wait.Semaphore_id = semaid;
                currentThread.wait.Semaphore_signal = signal;
                currentThread.wait.waitStateChecker = semaWaitStateChecker;

                threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
            } else {
                // Success
                if (log.isDebugEnabled()) {
                    log.debug("hleKernelWaitSema - '" + sema.name + "' fast check succeeded");
                }
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }

            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelWaitSema(int semaid, int signal, int timeout_addr) {
        hleKernelWaitSema(semaid, signal, timeout_addr, false);
    }

    public void sceKernelWaitSemaCB(int semaid, int signal, int timeout_addr) {
        hleKernelWaitSema(semaid, signal, timeout_addr, true);
    }

    public void sceKernelSignalSema(int semaid, int signal) {
        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            log.warn("sceKernelSignalSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelSignalSema id=0x" + Integer.toHexString(semaid) + " name='" + sema.name + "' signal=" + signal);
            }

            sema.currentCount += signal;
            if (sema.currentCount > sema.maxCount) {
                sema.currentCount = sema.maxCount;
            }

            ThreadManForUser threadMan = Modules.ThreadManForUserModule;

            if ((sema.attr & PSP_SEMA_ATTR_PRIORITY) == PSP_SEMA_ATTR_FIFO) {
                for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();

                    if (thread.waitType == PSP_WAIT_SEMA &&
                            thread.wait.waitingOnSemaphore &&
                            thread.wait.Semaphore_id == semaid &&
                            tryWaitSemaphore(sema, thread.wait.Semaphore_signal)) {

                        if (log.isDebugEnabled()) {
                            log.debug("sceKernelSignalSema waking thread 0x" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
                        }

                        thread.wait.waitingOnSemaphore = false;
                        thread.cpuContext.gpr[2] = 0;
                        threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);

                        sema.numWaitThreads--;
                        if (sema.currentCount == 0) {
                            break;
                        }
                    }
                }
            } else if ((sema.attr & PSP_SEMA_ATTR_PRIORITY) == PSP_SEMA_ATTR_PRIORITY) {
                for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext();) {
                    SceKernelThreadInfo thread = it.next();

                    if (thread.waitType == PSP_WAIT_SEMA &&
                            thread.wait.waitingOnSemaphore &&
                            thread.wait.Semaphore_id == semaid &&
                            tryWaitSemaphore(sema, thread.wait.Semaphore_signal)) {

                        if (log.isDebugEnabled()) {
                            log.debug("sceKernelSignalSema waking thread 0x" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
                        }

                        thread.wait.waitingOnSemaphore = false;
                        thread.cpuContext.gpr[2] = 0;
                        threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);

                        sema.numWaitThreads--;
                        if (sema.currentCount == 0) {
                            break;
                        }
                    }
                }
            }
            Emulator.getProcessor().cpu.gpr[2] = 0;
            threadMan.hleRescheduleCurrentThread();
        }
    }

    /** This is attempt to signal the sema and always return immediately */
    public void sceKernelPollSema(int semaid, int signal) {
        String msg = "sceKernelPollSema id=0x" + Integer.toHexString(semaid) + " signal=" + signal;

        if (signal <= 0) {
            log.warn(msg + " bad signal");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_COUNT;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            log.warn(msg + " unknown uid");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else if (sema.currentCount - signal < 0) {
            if (log.isDebugEnabled()) {
                log.debug(msg + " '" + sema.name + "'");
            }
            Emulator.getProcessor().cpu.gpr[2] = ERROR_SEMA_ZERO;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(msg + " '" + sema.name + "'");
            }
            sema.currentCount -= signal;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelCancelSema(int semaid, int newcount, int numWaitThreadAddr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelSema semaid=0x" + Integer.toHexString(semaid) + " newcount=" + newcount + " numWaitThreadAddr=0x" + Integer.toHexString(numWaitThreadAddr));
        }

        if (newcount <= 0) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_COUNT;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            log.warn("sceKernelCancelSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            Memory mem = Memory.getInstance();

            // Write previous numWaitThreads count.
            if (Memory.isAddressGood(numWaitThreadAddr)) {
                mem.write32(numWaitThreadAddr, sema.numWaitThreads);
            }

            sema.numWaitThreads = 0;

            // Find threads waiting on this sema and wake them up
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnSemaphore &&
                        thread.wait.Semaphore_id == semaid) {
                    thread.wait.waitingOnSemaphore = false;
                    thread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }

            // Reset this semaphore's count based on newcount.
            // Note: If newcount is -1, the count becomes this semaphore's initCount.
            if (newcount == -1) {
                sema.currentCount = sema.initCount;
            } else {
                sema.currentCount = newcount;
            }
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceKernelReferSemaStatus(int semaid, int addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferSemaStatus id= 0x" + Integer.toHexString(semaid) + " addr= 0x" + Integer.toHexString(addr));
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaInfo sema = semaMap.get(semaid);
        if (sema == null) {
            log.warn("sceKernelReferSemaStatus - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            sema.write(Memory.getInstance(), addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private class SemaWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the sema
            // has been signaled during the callback execution.
            SceKernelSemaInfo sema = semaMap.get(wait.Semaphore_id);
            if (sema == null) {
                thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
                return false;
            }

            // Check the sema.
            if (tryWaitSemaphore(sema, wait.Semaphore_signal)) {
                sema.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final SemaManager singleton = new SemaManager();

    private SemaManager() {
    }

}