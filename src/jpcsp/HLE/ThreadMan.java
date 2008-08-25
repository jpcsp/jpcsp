/*
Thread Manager
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/group__ThreadMan.html
- Schedule threads

Note:
- incomplete and not fully tested


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
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import static jpcsp.util.Utilities.*;


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
        //System.out.println("ThreadMan: Initialise");

        threadlist = new HashMap<Integer, SceKernelThreadInfo>();

        current_thread = new SceKernelThreadInfo("root", entry_addr, 0x20, 0x40000, attr);

        // Switch in this thread
        current_thread.status = PspThreadStatus.PSP_THREAD_RUNNING;
        current_thread.restoreContext();
    }

    /** to be called from the main emulation loop */
    public void step() /*throws GeneralJpcspException*/ {
        if (current_thread != null) {
            current_thread.runClocks++;

            //System.out.println("pc=" + Emulator.getProcessor().pc + " ra=" + Emulator.getProcessor().gpr[31]);

            // Hook jr ra to 0 (thread function returned)
            if (Emulator.getProcessor().pc == 0 && Emulator.getProcessor().gpr[31] == 0) {
                // Thread has exited
                System.out.println("Thread exit detected SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");
                current_thread.exitStatus = Emulator.getProcessor().gpr[2]; // v0
                current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
                contextSwitch(nextThread());
            }
        } else {
            // idle thread
            // Look for a new thread to switch in
            contextSwitch(nextThread());
        }

        Iterator<SceKernelThreadInfo> it = threadlist.values().iterator();
        while(it.hasNext()) {
            SceKernelThreadInfo thread = it.next();

            // Decrement delaysteps on sleeping threads
            if (thread.status == PspThreadStatus.PSP_THREAD_WAITING) {
                if (thread.delaysteps > 0)
                    thread.delaysteps--;
                if (thread.delaysteps == 0)
                    thread.status = PspThreadStatus.PSP_THREAD_READY;
            }

            // Cleanup stopped threads marked for deletion
            if (thread.status == PspThreadStatus.PSP_THREAD_STOPPED) {
                if (thread.do_delete) {
                    // TODO cleanup thread, example: free the stack, anything else?
                    // MemoryMan.free(thread.stack_addr);

                    // Changed to thread safe iterator.remove
                    //threadlist.remove(thread.uid);
                    it.remove();
                 try{
                    SceUIDMan.get_instance().releaseUid(thread.uid, "ThreadMan-thread");
                 }
                 catch(Exception e)
                 {
                   e.printStackTrace();   
                 }
                }
            }
        }

        // TODO watch dog timer?
    }

    private void contextSwitch(SceKernelThreadInfo newthread) {
        if (current_thread != null) {
            // Switch out old thread
            if (current_thread.status == PspThreadStatus.PSP_THREAD_RUNNING)
                current_thread.status = PspThreadStatus.PSP_THREAD_READY;
            // save registers
            current_thread.saveContext();
        }

        if (newthread != null) {
            // Switch in new thread
            newthread.status = PspThreadStatus.PSP_THREAD_RUNNING;
            newthread.wakeupCount++; // check
            // restore registers
            newthread.restoreContext();
            System.out.println("ThreadMan: switched to thread SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
        }

        current_thread = newthread;
    }

    // A ring buffer would be so nice here...
    // TODO thread priorities
    // This function must have the property of never returning current_thread, unless current_thread is already null
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
            } else if (foundcurrent && thread.status == PspThreadStatus.PSP_THREAD_READY) {
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


    public void ThreadMan_sceKernelCreateThread(int a0, int a1, int a2, int a3, int t0, int t1) {
        String name = readStringZ(Memory.get_instance().mainmemory, (a0 & 0x3fffffff) - MemoryMap.START_RAM);

        // TODO use t1/SceKernelThreadOptParam?

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, a1, a2, a3, t0);

        System.out.println("sceKernelCreateThread SceUID=" + Integer.toHexString(thread.uid) + " PC=" + Integer.toHexString(thread.pcreg) + " name:'" + thread.name + "'");

        Emulator.getProcessor().gpr[2] = thread.uid;
        //return thread.uid;
    }

    /** terminate thread a0 */
    public void ThreadMan_sceKernelTerminateThread(int a0) throws GeneralJpcspException {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan-thread");
        SceKernelThreadInfo thread = threadlist.get(a0);
        System.out.println("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

        thread.status = PspThreadStatus.PSP_THREAD_STOPPED; // PSP_THREAD_STOPPED or PSP_THREAD_KILLED ?

        Emulator.getProcessor().gpr[2] = 0;
        //return 0;
    }

    /** delete thread a0 */
    public void ThreadMan_sceKernelDeleteThread(int a0) throws GeneralJpcspException {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan-thread");
        SceKernelThreadInfo thread = threadlist.get(a0);
        System.out.println("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

        // Mark thread for deletion
        thread.do_delete = true;

        Emulator.getProcessor().gpr[2] = 0;
    }

    public void ThreadMan_sceKernelStartThread(int a0, int a1, int a2) throws GeneralJpcspException {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan-thread");
        SceKernelThreadInfo thread = threadlist.get(a0);

        // Set return value before context switch!
        Emulator.getProcessor().gpr[2] = 0;

        //thread.status = PspThreadStatus.PSP_THREAD_READY;
        // We will start the thread immediately so we don't have to save a1 and a2 somewhere
        contextSwitch(thread);
        // set arguments
        Emulator.getProcessor().gpr[4] = a1; // a0 = a1;
        Emulator.getProcessor().gpr[5] = a2; // a1 = a2;

        //return 0;
    }

    /** exit the current thread */
    public void ThreadMan_sceKernelExitThread(int a0) {
        System.out.println("sceKernelExitThread SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");
        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
        current_thread.exitStatus = a0;
        contextSwitch(nextThread());

        Emulator.getProcessor().gpr[2] = 0;
        //return 0;
    }

    /** exit the current thread, then delete it */
    public void ThreadMan_sceKernelExitDeleteThread(int a0) throws GeneralJpcspException {
        SceKernelThreadInfo thread = current_thread; // save a reference for post context switch operations
        System.out.println("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        // Exit
        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
        current_thread.exitStatus = a0;
        contextSwitch(nextThread());

        // Mark thread for deletion
        thread.do_delete = true;

        /* or we could just delete it here
        // Delete
        // TODO cleanup thread, example: free the stack, anything else?
        // MemoryMan.free(thread.stack_addr);

        threadlist.remove(thread.uid);
        SceUIDMan.get_instance().releaseUid(thread.uid, "ThreadMan-thread");
        */

        Emulator.getProcessor().gpr[2] = 0;
    }

    /** sleep the current thread until a registered callback is triggered */
    public void ThreadMan_sceKernelSleepThreadCB() {
        System.out.println("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        current_thread.do_callbacks = true;
        contextSwitch(nextThread());

        Emulator.getProcessor().gpr[2] = 0;
    }

    /** sleep the current thread for a certain number of microseconds */
    public void ThreadMan_sceKernelDelayThread(int a0) {
        current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
        current_thread.delaysteps = a0; // TODO delaysteps = a0 * steprate
        current_thread.do_callbacks = false;
        contextSwitch(nextThread());

        Emulator.getProcessor().gpr[2] = 0;
        //return 0;
    }

    public void ThreadMan_sceKernelCreateCallback(int a0, int a1, int a2) throws GeneralJpcspException {
        String name = readStringZ(Memory.get_instance().mainmemory, (a0 & 0x3fffffff) - MemoryMap.START_RAM);
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, current_thread.uid, a1, a2);

        System.out.println("sceKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid) + " PC=" + Integer.toHexString(callback.callback_addr) + " name:'" + callback.name + "'");

        Emulator.getProcessor().gpr[2] = callback.uid;
    }


    private class SceKernelCallbackInfo {
        private String name;
        private int threadId;
        private int callback_addr;
        private int callback_arg_addr;
        private int notifyCount;
        private int notifyArg;

        // internal variables
        private int uid;

        public SceKernelCallbackInfo(String name, int threadId, int callback_addr, int callback_arg_addr) {
            this.name = name;
            this.threadId = threadId;
            this.callback_addr = callback_addr;
            this.callback_arg_addr = callback_arg_addr;

            notifyCount = 0; // ?
            notifyArg = 0; // ?

            // internal state
            uid = SceUIDMan.get_instance().getNewUid("ThreadMan-callback");

            // TODO add to list of callbacks
        }
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
        private int pcreg, npcreg;
        private long hilo;
        private int[] gpr;
        private float[] fpr;
        private float[] vpr;
        private int delaysteps;
        private boolean do_delete;
        private boolean do_callbacks; // in this implementation, only valid for PSP_THREAD_WAITING and PSP_THREAD_SUSPEND

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
            exitStatus = 0x800201a4; // thread is not DORMANT
            runClocks = 0;
            intrPreemptCount = 0;
            threadPreemptCount = 0;
            releaseCount = 0;

            // internal state
            uid = SceUIDMan.get_instance().getNewUid("ThreadMan-thread");
            threadlist.put(uid, this);

            gpr = new int[32];
            fpr = new float[32];
            vpr = new float[128];

            saveContext();
            // Thread specific registers
            pcreg = entry_addr;
            npcreg = entry_addr; // + 4;
            gpr[29] = stack_addr; //sp

            // TODO hook "jr ra" where ra = 0,
            // then set current_thread.exitStatus = v0 and current_thread.status = PSP_THREAD_STOPPED,
            // finally contextSwitch(nextThread())
            gpr[31] = 0; // ra

            delaysteps = 0;
            do_delete = false;
            do_callbacks = false;
        }

        public void saveContext() {
            Processor cpu = Emulator.getProcessor();
            pcreg = cpu.pc;
            npcreg = cpu.npc;
            hilo = cpu.hilo;
            for (int i = 0; i < 32; i++) {
                gpr[i] = cpu.gpr[i];
            }

            // TODO check attr for PSP_THREAD_ATTR_VFPU and save vfpu registers
        }

        public void restoreContext() {
            Processor cpu = Emulator.getProcessor();
            cpu.pc = pcreg;
            cpu.npc = npcreg;
            cpu.hilo = hilo;
            for (int i = 0; i < 32; i++) {
                cpu.gpr[i] = gpr[i];
            }

            // TODO check attr for PSP_THREAD_ATTR_VFPU and restore vfpu registers
        }
    }
}
