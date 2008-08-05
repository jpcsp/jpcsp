/*
Thread Manager
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/group__ThreadMan.html
- Schedule threads

Note:
- incomplete, untested, not wired to the rest of the emulator yet


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
package jpcsp.HLE;

import java.util.HashMap;
import java.util.Iterator;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Processor;


public class ThreadMan {
    private static ThreadMan instance;
    private static HashMap<Integer, SceKernelThreadInfo> threadlist;
    private SceKernelThreadInfo current_thread;

    public static ThreadMan get_instance() {
        if (instance == null) {
            instance = new ThreadMan();
        }
        return instance;
    }

    private ThreadMan() {
    }

    /** call this when resetting the emulator
     * @param entry from ELF header
     * @param attr from sceModuleInfo ELF section header */
    public void Initialise(int entry_addr, int attr) {
        threadlist = new HashMap<Integer, SceKernelThreadInfo>();

        current_thread = new SceKernelThreadInfo("root", entry_addr, 0x20, 0x10000, attr);
        current_thread.status = PspThreadStatus.PSP_THREAD_RUNNING;

        // Switch in this thread
        newthread.restoreContext();
    }

    /** to be called from the main emulation loop */
    public void step() {
        if (current_thread != null) {
            current_thread.runClocks++;
        } else {
            // idle thread
            // Look for a new thread to switch in
            contextSwitch(nextThread());
        }

        // decrement delaysteps on sleeping threads
        Iterator<SceKernelThreadInfo> it = threadlist.values().iterator();
        while(it.hasNext()) {
            SceKernelThreadInfo thread = it.next();
            if (thread.status == PspThreadStatus.PSP_THREAD_WAITING) {
                if (current_thread.delaysteps > 0)
                    current_thread.delaysteps--;
                if (current_thread.delaysteps == 0)
                    thread.status = PspThreadStatus.PSP_THREAD_READY;
            }
        }
    }

    private void contextSwitch(SceKernelThreadInfo newthread) {
        if (current_thread != null) {
            // Switch out old thread
            current_thread.status = PspThreadStatus.PSP_THREAD_READY;
            // save registers
            newthread.saveContext();
        }

        if (newthread != null) {
            // Switch in new thread
            newthread.status = PspThreadStatus.PSP_THREAD_RUNNING;
            newthread.wakeupCount++; // check
            // restore registers
            newthread.restoreContext();
        }

        current_thread = newthread;
    }

    // A ring buffer would be so nice here...
    // TODO thread priorities
    private SceKernelThreadInfo nextThread() {
        Iterator<SceKernelThreadInfo> it;
        SceKernelThreadInfo found = null;
        boolean foundcurrent = (current_thread == null) ? true : false;

        // Find the thread AFTER the current thread with status PSP_THREAD_READY
        it = threadlist.values().iterator();
        while(it.hasNext()) {
            SceKernelThreadInfo thread = it.next();

            if (!foundcurrent && thread == current_thread) {
                foundcurrent = true;
            }

            if (foundcurrent && thread.status == PspThreadStatus.PSP_THREAD_READY) {
                found = thread;
                break;
            }
        }

        if (found == null && current_thread != null) {
            // Find the thread BEFORE the current thread with status PSP_THREAD_READY
            foundcurrent = false;
            it = threadlist.values().iterator();
            while(it.hasNext()) {
                SceKernelThreadInfo thread = it.next();

                if (!foundcurrent && thread == current_thread) {
                    foundcurrent = true;
                    break;
                }

                if (!foundcurrent && thread.status == PspThreadStatus.PSP_THREAD_READY) {
                    found = thread;
                    break;
                }
            }
        }

        return found;
    }


    public int ThreadMan_sceKernelCreateThread(int a0, int a1, int a2, int a3, int t0, int t1) {
        String name = "TestThread"; // TODO readStringZ(Memory.get_instance().mainmemory, a0);

        // TODO use t1/SceKernelThreadOptParam?

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, a1, a2, a3, t0);

        return thread.uid;
    }

    public int ThreadMan_sceKernelTerminateThread(int a0) throws GeneralJpcspException {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan");
        SceKernelThreadInfo thread = threadlist.get(a0);
        thread.status = PspThreadStatus.PSP_THREAD_STOPPED; // PSP_THREAD_STOPPED or PSP_THREAD_KILLED ?
        return 0;
    }

    public int ThreadMan_sceKernelDeleteThread(int a0) throws GeneralJpcspException {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan");
        SceKernelThreadInfo thread = threadlist.get(a0);

        // If we're deleting the current thread switch to another first
        if (a0 == current_thread.uid)
            contextSwitch(nextThread());

        // TODO cleanup thread, example: free the stack, anything else?
        // MemoryMan.free(thread.stack_addr);

        threadlist.remove(a0);
        SceUIDMan.get_instance().releaseUid(a0, "ThreadMan");
        return 0;
    }

    public int ThreadMan_sceKernelStartThread(int a0, int a1, int a2) throws GeneralJpcspException {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan");
        SceKernelThreadInfo thread = threadlist.get(a0);


        //thread.status = PspThreadStatus.PSP_THREAD_READY;
        // We will start the thread immediately so we don't have to save a1 and a2 somewhere
        contextSwitch(thread);
        // TODO set arguments
        //a0reg = a1;
        //a1reg = a2;

        return 0;
    }

    /** exit the current thread */
    public int ThreadMan_sceKernelExitThread(int a0) {
        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
        current_thread.exitStatus = a0;
        contextSwitch(nextThread());
        return 0;
    }

    /** sleep the current thread */
    public int ThreadMan_sceKernelDelayThread(int a0) {
        current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
        current_thread.delaysteps = a0; // TODO delaysteps = a0 * steprate
        contextSwitch(nextThread());
        return 0;
    }


    enum PspThreadStatus {
        PSP_THREAD_RUNNING(1), PSP_THREAD_READY(2),
        PSP_THREAD_WAITING(4), PSP_THREAD_SUSPEND(8),
        PSP_THREAD_STOPPED(16), PSP_THREAD_KILLED(32);
        private int value;
        private PspThreadStatus(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    private class SceKernelThreadInfo {
        // SceKernelThreadInfo <http://psp.jim.sh/pspsdk-doc/structSceKernelThreadInfo.html>
        private String name;
        private int attr;
        //private int status;
        private PspThreadStatus status;
        private int entry_addr;
        private int stack_addr;
        private int stackSize;
        private int gpReg_addr;
        private int initPriority;
        private int currentPriority;
        private int waitType;
        private int waitId;
        private int wakeupCount;
        private int exitStatus;
        private int runClocks;
        private int intrPreemptCount;
        private int threadPreemptCount;
        private int releaseCount;

        // internal variables
        private int uid;
        private int pcreg, hi, lo;
        private int[] cpuregisters;
        private int delaysteps;

        public SceKernelThreadInfo(String name, int entry_addr, int initPriority, int stackSize, int attr) {
            this.name = name;
            this.entry_addr = entry_addr;
            this.initPriority = initPriority;
            this.stackSize = stackSize;
            this.attr = attr;

            status = PspThreadStatus.PSP_THREAD_SUSPEND;
            stack_addr = 0x09f00000; // TODO MemoryMan.malloc(stackSize);
            gpReg_addr = 0; // ?
            currentPriority = initPriority;
            waitType = 0; // ?
            waitId = 0; // ?
            wakeupCount = 0;
            exitStatus = 0xbaadc0de; // doesn't matter until thread exits
            runClocks = 0;
            intrPreemptCount = 0;
            threadPreemptCount = 0;
            releaseCount = 0;

            // internal state
            uid = SceUIDMan.get_instance().getNewUid("ThreadMan");
            threadlist.put(uid, this);

            saveContext();
            // Thread specific registers
            pcreg = entry_addr;
            cpuregisters[29] = stack_addr; //sp

            // TODO hook "jr ra" where ra = 0,
            // then set current_thread.exitStatus = v0 and current_thread.status = PSP_THREAD_STOPPED,
            // finally contextSwitch(nextThread())
            cpuregisters[31] = 0; // ra

            delaysteps = 0;
        }

        public void saveContext() {
            Processor cpu = Emulator.getProcessor();
            pcreg = cpu.pc;
            hi = cpu.hi;
            lo = cpu.lo;
            for (int i = 0; i < 32; i++) {
                cpuregisters[i] = cpu.cpuregisters[i];
            }
        }

        public void restoreContext() {
            Processor cpu = Emulator.getProcessor();
            cpu.pc = pcreg;
            cpu.hi = hi;
            cpu.lo = lo;
            for (int i = 0; i < 32; i++) {
                cpu.cpuregisters[i] = cpuregisters[i];
            }
        }
    }
}
