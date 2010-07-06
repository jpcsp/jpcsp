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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MUTEX_LOCKED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_MUTEX;
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

/*
 * TODO list:
 * 1. Find all error codes:
 *  -> Info: http://forums.ps2dev.org/viewtopic.php?p=79708#79708
 *      - 0x800201c3 ERROR_NOT_FOUND_MUTEX.
 *      - 0x800201c4 mutex already locked (from try lock).
 *      - 0x800201c8 overflow? (mutex already locked).
 *
 * 2. Figure out the meaning of PSP_MUTEX_UNKNOWN_ATTR and PSP_LW_MUTEX_UNKNOWN_ATTR.
 *
 * 3. Use timeout in hleKernelLockMutex().
 */
public class MutexManager {

    private HashMap<Integer, SceKernelMutexInfo> mutexMap;
    private MutexWaitStateChecker mutexWaitStateChecker;

    private final static int PSP_MUTEX_UNKNOWN_ATTR = 0x100; // TODO
    private final static int PSP_MUTEX_ALLOW_SAME_THREAD = 0x200;
    private final static int PSP_LW_MUTEX_UNKNOWN_ATTR = 0x300; //TODO

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

        if (Modules.log.isDebugEnabled()) {
	        Modules.log.debug("sceKernelCreateMutex(name='" + name
	            + "',attr=0x" + Integer.toHexString(attr)
	            + ",count=0x" + Integer.toHexString(count)
	            + ",option_addr=0x" + Integer.toHexString(option_addr) + ")");
        }

        if ((attr & ~PSP_MUTEX_ALLOW_SAME_THREAD) != 0) {
        	Modules.log.warn("PARTIAL:sceKernelCreateMutex attr value 0x" + Integer.toHexString(attr));
        }

        SceKernelMutexInfo info = new SceKernelMutexInfo(name, attr);
        mutexMap.put(info.uid, info);

        info.locked = count;
        info.threadid = Modules.ThreadManForUserModule.getCurrentThreadID();

        cpu.gpr[2] = info.uid;
    }

    public void sceKernelDeleteMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        // should only be 1 parameter
        Modules.log.debug("sceKernelDeleteMutex UID " +Integer.toHexString(uid)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteMutex unknown UID " +Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    /** @return true if the mutex was successfully locked */
    private boolean tryLockMutex(SceKernelMutexInfo info, int count, boolean allowSameThread) {
        if (info.locked == 0 || allowSameThread) {
            info.locked += count;
            return true;
        }
		return false;
    }

    private boolean allowSameThread(SceKernelMutexInfo info, SceKernelThreadInfo thread) {
        boolean allowSameThread = false;

        if (info.threadid == thread.uid &&
            (info.attr & PSP_MUTEX_ALLOW_SAME_THREAD) == PSP_MUTEX_ALLOW_SAME_THREAD) {
            allowSameThread = true;
        }

        return allowSameThread;
    }

    /**
     * @return true on success
     */
    private void hleKernelLockMutex(int uid, int count, int timeout_addr, boolean wait, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        String message = "hleKernelLockMutex(uid=" + Integer.toHexString(uid)
            + ",count=" + count
            + ",timeout_addr=0x" + Integer.toHexString(timeout_addr)
            + ") wait=" + wait
            + ",cb=" + doCallbacks;

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn(message + " - unknown UID");
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
        } else {
        	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

            boolean allowSameThread = allowSameThread(info, currentThread);
            if (!tryLockMutex(info, count, allowSameThread)) {
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

                    // doesn't really matter what we set this to, it's going to get changed before the thread will run again
                    cpu.gpr[2] = 0;
                } else {
                    // sceKernelTryLockMutex
                    cpu.gpr[2] = ERROR_MUTEX_LOCKED;
                }
            } else {
                Modules.log.debug(message + " - '" + info.name + "' fast check succeeded");
                info.threadid = currentThread.uid;
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

    private void wakeWaitMutexThreads(SceKernelMutexInfo info, boolean wakeMultiple) {
        if (info.numWaitThreads < 0) {
            Modules.log.error("info.numWaitThreads < 0 (" + info.numWaitThreads + ")");
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
            return;
        }

        if (info.numWaitThreads == 0) {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("wakeWaitMutexThreads(multiple=" + wakeMultiple + ") mutex:'" + info.name + "' fast exit (numWaitThreads == 0)");
        	}
            return;
        }

        for (Iterator<SceKernelThreadInfo> it = Modules.ThreadManForUserModule.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            boolean allowSameThread = allowSameThread(info, thread);
            if (thread.waitType == PSP_WAIT_MUTEX &&
                thread.wait.waitingOnMutex &&
                thread.wait.Mutex_id == info.uid &&
                (wakeMultiple || tryLockMutex(info, thread.wait.Mutex_count, allowSameThread))) {

                // Update numWaitThreads
                info.numWaitThreads--;

                // Untrack
                thread.wait.waitingOnMutex = false;

                // Return success or failure
                thread.cpuContext.gpr[2] = (wakeMultiple ? ERROR_WAIT_DELETE : 0);

                // Wakeup
                Modules.ThreadManForUserModule.hleChangeThreadState(thread, PSP_THREAD_READY);

                if (Modules.log.isDebugEnabled()) {
                	Modules.log.debug("wakeWaitMutexThreads(multiple=" + wakeMultiple + ") mutex:'" + info.name + "' waking thread:'" + thread.name + "'");
                }
            }
        }
    }

    public void sceKernelUnlockMutex(int uid, int count) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelUnlockMutex(uid=" + Integer.toHexString(uid) + ",count=" + count + ")");

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelUnlockMutex unknown uid");
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
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
    }

    public void sceKernelCancelMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("PARTIAL:sceKernelCancelMutex UID " + Integer.toHexString(uid)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelMutex unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelCancelMutex UID " + Integer.toHexString(uid) + " not locked");
            cpu.gpr[2] = -1;
        } else {
            info.locked = 0;

            // wake all threads waiting on this mutex
            wakeWaitMutexThreads(info, true);

            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferMutexStatus(int uid, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("PARTIAL:sceKernelReferMutexStatus UID " + Integer.toHexString(uid)
            + "addr " + String.format("0x%08X", addr)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
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

    //Firmware 3.80+.
    //Lightweight mutexes (a.k.a. Critical Sections).

    //From tests with Kenka Banchou Portable (ULJS00235), the lightweight mutexes' functions
    //seem to provide an output address, as the first argument, for writing the uid.
    //This address is later read by other related functions.

     public void sceKernelCreateLwMutex(int out_addr, int name_addr, int attr, int count, int option_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringNZ(mem, name_addr, 32);

        if (Modules.log.isDebugEnabled()) {
	        Modules.log.debug("sceKernelCreateLwMutex (uid addr='" + Integer.toHexString(out_addr) + "',name='" + name
	            + "',attr=0x" + Integer.toHexString(attr)
	            + ",count=0x" + Integer.toHexString(count)
	            + ",option_addr=0x" + Integer.toHexString(option_addr) + ")");
        }

        if (attr != 0) {
        	Modules.log.warn("PARTIAL:sceKernelCreateLwMutex attr value 0x" + Integer.toHexString(attr));
        }

        SceKernelMutexInfo info = new SceKernelMutexInfo(name, attr);
        mutexMap.put(info.uid, info);

        info.locked = count;
        info.threadid = Modules.ThreadManForUserModule.getCurrentThreadID();

        cpu.gpr[2] = info.uid;

        mem.write32(out_addr, info.uid);
    }

     public void sceKernelDeleteLwMutex(int uid_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(uid_addr);

        Modules.log.debug("sceKernelDeleteLwMutex UID " +Integer.toHexString(uid)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteLwMutex unknown UID " +Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
        } else {
            cpu.gpr[2] = 0;
            mem.write32(uid_addr, 0); //Clear uid address.
        }
     }

     public void sceKernelLockLwMutex(int uid_addr, int count, int timeout_addr) {
        Memory mem = Processor.memory;

        int uid = mem.read32(uid_addr);

        Modules.log.debug("sceKernelLockLwMutex redirecting to hleKernelLockMutex");
        hleKernelLockMutex(uid, count, timeout_addr, true, false);
    }

    public void sceKernelLockLwMutexCB(int uid_addr, int count, int timeout_addr) {
        Memory mem = Processor.memory;

        int uid = mem.read32(uid_addr);

        Modules.log.debug("sceKernelLockLwMutexCB redirecting to hleKernelLockMutex");
        hleKernelLockMutex(uid, count, timeout_addr, true, true);
    }

    public void sceKernelTryLockLwMutex(int uid_addr, int count) {
        Memory mem = Processor.memory;

        int uid = mem.read32(uid_addr);

        Modules.log.debug("sceKernelTryLockLwMutex redirecting to hleKernelLockMutex");
        hleKernelLockMutex(uid, count, 0, false, false);
    }

    public void sceKernelUnlockLwMutex(int uid_addr, int count) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(uid_addr);

        Modules.log.debug("sceKernelUnlockLwMutex(uid=" + Integer.toHexString(uid) + ",count=" + count + ")");

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelUnlockLwMutex unknown uid");
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelUnlockLwMutex not locked");
            cpu.gpr[2] = 0;
        } else {
            info.locked -= count;
            if (info.locked < 0) {
                Modules.log.warn("sceKernelUnlockLwMutex underflow " + info.locked);
                info.locked  = 0;
            }

            if (info.locked == 0) {
                wakeWaitMutexThreads(info, false);
            }

            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferLwMutexStatus(int uid_addr, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int uid = mem.read32(uid_addr);

        Modules.log.warn("PARTIAL:sceKernelReferLwMutexStatus UID " + Integer.toHexString(uid)
            + "addr " + String.format("0x%08X", addr)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferLwMutexStatus unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MUTEX;
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

    public void sceKernelReferLwMutexStatusByID() {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("Unimplemented sceKernelReferLwMutexStatusByID "
            + String.format("%08x %08x %08x %08x", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]));

        cpu.gpr[2] = 0xDEADC0DE;
    }

    private class MutexWaitStateChecker implements IWaitStateChecker {
		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
			// Check if the thread has to continue its wait state or if the mutex
			// has been unlocked during the callback execution.
			SceKernelMutexInfo info = mutexMap.get(wait.Mutex_id);
			if (info == null) {
	            thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_MUTEX;
				return false;
			}

			// Check the mutex
            boolean allowSameThread = allowSameThread(info, thread);
            if (!tryLockMutex(info, wait.Mutex_count, allowSameThread)) {
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