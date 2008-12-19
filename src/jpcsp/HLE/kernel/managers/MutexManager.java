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

// TODO find ERROR_NOT_FOUND_MUTEX error code
// TODO find other codes like:
// - DONE mutex already locked 0x800201c4
// - mutex already unlocked
public class MutexManager {

    private HashMap<Integer, SceKernelMutexInfo> mutexMap;

    public void reset() {
        mutexMap = new HashMap<Integer, SceKernelMutexInfo>();
    }

    /** returns a uid on success */
    public void sceKernelCreateMutex(int name_addr, int attr, int unk1, int unk2) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringNZ(mem, name_addr, 32);

        Modules.log.info("sceKernelCreateMutex(name='" + name
            + "',attr=0x" + Integer.toHexString(attr)
            + ",unk1=0x" + Integer.toHexString(unk1)
            + ",unk2=0x" + Integer.toHexString(unk2) + ")");

        if (attr != 0) Modules.log.warn("UNIMPLEMENTED:sceKernelCreateMutex attr value 0x" + Integer.toHexString(attr));

        SceKernelMutexInfo info = new SceKernelMutexInfo(name, attr);
        mutexMap.put(info.uid, info);

        cpu.gpr[2] = info.uid;
        //Emulator.PauseEmu();
    }

    public void sceKernelDeleteMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelDeleteMutex UID " +Integer.toHexString(uid)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteMutex unknown UID " +Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    /** @return true if the mutex was moved from unlocked to locked */
    private boolean tryLockMutex(SceKernelMutexInfo info) {
        if (info.locked == 0) {
            info.locked = 1;
            return true;
        } else {
            return false;
        }
    }

    /** TODO look for a timeout parameter, for now we assume infinite wait
     * @return true on success */
    private void hleKernelLockMutex(int uid, int unk1, int unk_addr, boolean wait, boolean do_callbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;

        String message = "hleKernelLockMutex UID=" + Integer.toHexString(uid)
            + ",unk1=0x" + Integer.toHexString(unk1)
            + ",unk2=0x" + Integer.toHexString(unk_addr)
            + " (wait=" + wait
            + ",CB=" + do_callbacks + ")";

        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(unk_addr)) {
            Modules.log.debug("unk_addr value 0x" + Integer.toHexString(mem.read32(unk_addr)));
        }

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn(message + " - unknown UID");
            cpu.gpr[2] = -1;
        } else if (!tryLockMutex(info)) {
            Modules.log.info(message + " - '" + info.name + "' fast check failed");

            if (wait) {
                ThreadMan threadMan = ThreadMan.getInstance();
                SceKernelThreadInfo current_thread = threadMan.getCurrentThread();

                // Failed, but it's ok, just wait a little
                info.numWaitThreads++;

                // Do callbacks?
                current_thread.do_callbacks = do_callbacks;

                // Go to wait state
                int timeout = 0;
                boolean forever = true;
                threadMan.hleKernelThreadWait(current_thread.wait, timeout, forever);

                // Wait on a specific mutex
                current_thread.wait.waitingOnMutex = true;
                current_thread.wait.Mutex_id = uid;

                threadMan.changeThreadState(current_thread, PSP_THREAD_WAITING);
                threadMan.contextSwitch(threadMan.nextThread());

                cpu.gpr[2] = 0;
            } else {
                // sceKernelTryLockMutex
                cpu.gpr[2] = ERROR_MUTEX_LOCKED;
            }
        } else {
            Modules.log.info(message + " - '" + info.name + "' fast check succeeded");
            cpu.gpr[2] = 0;
        }
    }

    // unk1 - set to 1
    public void sceKernelLockMutex(int uid, int unk1, int unk_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelLockMutex redirecting to hleKernelLockMutex(wait)"
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        hleKernelLockMutex(uid, unk1, unk_addr, true, false);
        //Emulator.PauseEmu();
    }

    public void sceKernelLockMutexCB(int uid, int unk1, int unk_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelLockMutexCB redirecting to hleKernelLockMutex(waitCB)"
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        hleKernelLockMutex(uid, unk1, unk_addr, true, true);
        //Emulator.PauseEmu();
    }

    public void sceKernelTryLockMutex(int uid, int unk1) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelTryLockMutex redirecting to hleKernelLockMutex(poll)"
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        hleKernelLockMutex(uid, unk1, 0, false, false);
        //Emulator.PauseEmu();
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

    public void sceKernelUnlockMutex(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.info("sceKernelUnlockMutex UID " + Integer.toHexString(uid)
            + String.format(" %08X %08X %08X %08X", cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]));

        SceKernelMutexInfo info = mutexMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelUnlockMutex unknown UID " + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else if (info.locked == 0) {
            Modules.log.warn("sceKernelUnlockMutex UID " + Integer.toHexString(uid) + " not locked");
            cpu.gpr[2] = -1;
        } else {
            info.locked = 0;

            // wake one thread waiting on this mutex
            wakeWaitMutexThreads(info, false);

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
