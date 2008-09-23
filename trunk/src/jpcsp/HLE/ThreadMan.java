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
import static jpcsp.util.Utilities.*;


public class ThreadMan {
    private static ThreadMan instance;
    private static HashMap<Integer, SceKernelThreadInfo> threadlist;
    private static HashMap<Integer, SceKernelSemaphoreInfo> semalist;
    private  ArrayList<Integer> waitingThreads;
    private SceKernelThreadInfo current_thread;
    private SceKernelThreadInfo idle0, idle1;
    private int continuousIdleCycles; // watch dog timer

    // stack allocation MOVED TO PSPSYSMEM
    //private static int stackAllocated;

    public static ThreadMan get_instance() {
        if (instance == null) {
            instance = new ThreadMan();
        }
        return instance;
    }

    private ThreadMan() {
    }

    /** call this when resetting the emulator
     * @param entry_addr entry from ELF header
     * @param attr from sceModuleInfo ELF section header */
    public void Initialise(int entry_addr, int attr, String pspfilename) {
        //System.out.println("ThreadMan: Initialise");

        threadlist = new HashMap<Integer, SceKernelThreadInfo>();
        semalist = new HashMap<Integer, SceKernelSemaphoreInfo>();
        waitingThreads= new ArrayList<Integer>();
        // Clear stack allocation info MOVED TO PSPSYSMEM
        //stackAllocated = 0;

        install_idle_threads();

        // Create a thread the program will run inside
        current_thread = new SceKernelThreadInfo("root", entry_addr, 0x20, 0x40000, attr);

        // Set user mode bit if kernel mode bit is not present
        if ((current_thread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL) {
            current_thread.attr |= PSP_THREAD_ATTR_USER;
        }

        // Setup args by copying them onto the stack
        //System.out.println("pspfilename - '" + pspfilename + "'");
        int len = pspfilename.length();
        Memory mem = Memory.getInstance();
        for (int i = 0; i < len; i++)
            mem.write8((current_thread.stack_addr - len + 1) + i, (byte)pspfilename.charAt(i));
        current_thread.gpr[29] -= len; // Adjust sp for size of args
        current_thread.gpr[4] = len; // a0 = len
        current_thread.gpr[5] = current_thread.gpr[29] + 1; // a1 = pointer to arg data in stack
        current_thread.status = PspThreadStatus.PSP_THREAD_READY;

        // Switch in the thread
        current_thread.status = PspThreadStatus.PSP_THREAD_RUNNING;
        current_thread.restoreContext();
    }

    private void install_idle_threads() {
        // Generate 2 idle threads which can toggle between each other when there are no ready threads
        int instruction_addiu = // addiu a0, zr, 0
            ((jpcsp.AllegrexOpcodes.ADDIU & 0x3f) << 26)
            | ((0 & 0x1f) << 21)
            | ((4 & 0x1f) << 16);
        int instruction_lui = // lui ra, 0x08000000
            ((jpcsp.AllegrexOpcodes.LUI & 0x3f) << 26)
            | ((31 & 0x1f) << 16)
            | (0x0800 & 0x0000ffff);
        int instruction_jr = // jr ra
            ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (jpcsp.AllegrexOpcodes.JR & 0x3f)
            | ((31 & 0x1f) << 21);
        int instruction_syscall = // syscall <code>
            ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f)
            | ((0x201c & 0x000fffff) << 6);

        // TODO
        //pspSysMem.get_instance().malloc(1, pspSysMem.PSP_SMEM_Addr, 16, MemoryMap.START_RAM);

        Memory.getInstance().write32(MemoryMap.START_RAM + 0, instruction_addiu);
        Memory.getInstance().write32(MemoryMap.START_RAM + 4, instruction_lui);
        Memory.getInstance().write32(MemoryMap.START_RAM + 8, instruction_jr);
        Memory.getInstance().write32(MemoryMap.START_RAM + 12, instruction_syscall);

        idle0 = new SceKernelThreadInfo("idle0", MemoryMap.START_RAM, 0x7f, 0x0, 0x0);
        idle0.status = PspThreadStatus.PSP_THREAD_READY;

        idle1 = new SceKernelThreadInfo("idle1", MemoryMap.START_RAM, 0x7f, 0x0, 0x0);
        idle1.status = PspThreadStatus.PSP_THREAD_READY;

        continuousIdleCycles = 0;
    }

    /** to be called from the main emulation loop */
    public void step() {
        if (current_thread != null) {
            current_thread.runClocks++;

            //System.out.println("pc=" + Emulator.getProcessor().pc + " ra=" + Emulator.getProcessor().gpr[31]);

            // Hook jr ra to 0 (thread function returned)
            if (Emulator.getProcessor().pc == 0 && Emulator.getProcessor().gpr[31] == 0) {
                // Thread has exited
                System.out.println("Thread exit detected SceUID=" + Integer.toHexString(current_thread.uid)
                    + " name:'" + current_thread.name + "' return:" + Emulator.getProcessor().gpr[2]);
                current_thread.exitStatus = Emulator.getProcessor().gpr[2]; // v0
                current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
                contextSwitch(nextThread());
            }

            // Watch dog timer
            if (current_thread == idle0 || current_thread == idle1) {
                continuousIdleCycles++;
                // TODO figure out a decent number of cycles to wait
                if (continuousIdleCycles > 1000000) {
                    System.out.println("Watch dog timer - pausing emulator");
                    Emulator.PauseEmu();
                }
            } else {
                continuousIdleCycles = 0;
            }
        } else {
            // We always need to be in a thread! we shouldn't get here.
            System.out.println("No ready threads!");
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
                    pspSysMem.get_instance().free(thread.stack_addr);

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
    }

    private void contextSwitch(SceKernelThreadInfo newthread) {
        if (current_thread != null) {
            // Switch out old thread
            if (current_thread.status == PspThreadStatus.PSP_THREAD_RUNNING)
                current_thread.status = PspThreadStatus.PSP_THREAD_READY;
            // save registers
            current_thread.saveContext();

            /*
            System.out.println("saveContext SceUID=" + Integer.toHexString(current_thread.uid)
                + " name:" + current_thread.name
                + " PC:" + Integer.toHexString(current_thread.pcreg)
                + " NPC:" + Integer.toHexString(current_thread.npcreg));
            */
        }

        if (newthread != null) {
            // Switch in new thread
            newthread.status = PspThreadStatus.PSP_THREAD_RUNNING;
            newthread.wakeupCount++; // check
            // restore registers
            newthread.restoreContext();

            //System.out.println("ThreadMan: switched to thread SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
            /*
            System.out.println("restoreContext SceUID=" + Integer.toHexString(newthread.uid)
                + " name:" + newthread.name
                + " PC:" + Integer.toHexString(newthread.pcreg)
                + " NPC:" + Integer.toHexString(newthread.npcreg));
            */

            //Emulator.PauseEmu();
        } else {
            System.out.println("No ready threads - pausing emulator");
            Emulator.PauseEmu();
        }

        current_thread = newthread;
    }

    // This function must have the property of never returning current_thread, unless current_thread is already null
    private SceKernelThreadInfo nextThread() {
        Collection<SceKernelThreadInfo> c;
        List<SceKernelThreadInfo> list;
        Iterator<SceKernelThreadInfo> it;
        SceKernelThreadInfo found = null;

        // Find the thread with status PSP_THREAD_READY and the highest priority
        // In this implementation low priority threads can get starved
        c = threadlist.values();
        list = new LinkedList<SceKernelThreadInfo>(c);
        Collections.sort(list, idle0); // We need an instance of SceKernelThreadInfo for the comparator, so we use idle0
        it = list.iterator();
        while(it.hasNext()) {
            SceKernelThreadInfo thread = it.next();
            //System.out.println("nextThread pri=" + Integer.toHexString(thread.currentPriority) + " name:" + thread.name + " status:" + thread.status);

            if (thread != current_thread &&
                thread.status == PspThreadStatus.PSP_THREAD_READY) {
                found = thread;
                break;
            }
        }

        return found;
    }

    public int getCurrentThreadID() {
        return current_thread.uid;
    }

    public void yieldCurrentThread()
    {
       contextSwitch(nextThread());
    }

    public void blockCurrentThread()
    {
       current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
       contextSwitch(nextThread());
    }

    public void unblockThread(int uid)
    {
        if (SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadlist.get(uid);
            thread.status = PspThreadStatus.PSP_THREAD_READY;
        }
    }


    public void ThreadMan_sceKernelCreateThread(int name_addr, int entry_addr,
        int initPriority, int stackSize, int attr, int option_addr) {
        String name = readStringZ(Memory.getInstance().mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        // TODO use option_addr/SceKernelThreadOptParam?
        if (option_addr != 0)
            System.out.println("sceKernelCreateThread unhandled SceKernelThreadOptParam");

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);

        System.out.println("sceKernelCreateThread SceUID=" + Integer.toHexString(thread.uid)
            + " name:'" + thread.name + "' PC=" + Integer.toHexString(thread.pcreg)
            + " attr:" + Integer.toHexString(attr));

        // Inherit kernel mode if user mode bit is not set
        if ((current_thread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL &&
            (attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
            System.out.println("sceKernelCreateThread inheriting kernel mode");
            thread.attr |= PSP_THREAD_ATTR_KERNEL;
        }
        // Inherit user mode
        if ((current_thread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER) {
            if ((thread.attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER)
                System.out.println("sceKernelCreateThread inheriting user mode");
            thread.attr |= PSP_THREAD_ATTR_USER;
            // Always remove kernel mode bit
            thread.attr &= ~PSP_THREAD_ATTR_KERNEL;
        }

        Emulator.getProcessor().gpr[2] = thread.uid;
    }

    /** terminate thread a0 */
    public void ThreadMan_sceKernelTerminateThread(int a0) {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(a0);
        if (thread == null) {
            Emulator.getProcessor().gpr[2] = 0x80020198; //notfoundthread
        } else {
            System.out.println("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            thread.status = PspThreadStatus.PSP_THREAD_STOPPED; // PSP_THREAD_STOPPED or PSP_THREAD_KILLED ?

            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    /** delete thread a0 */
    public void ThreadMan_sceKernelDeleteThread(int a0) {
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(a0);
        if (thread == null) {
            Emulator.getProcessor().gpr[2] = 0x80020198; //notfoundthread
        } else {
            System.out.println("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Mark thread for deletion
            thread.do_delete = true;

            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelStartThread(int uid, int len, int data_addr) {
        SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Emulator.getProcessor().gpr[2] = 0x80020198; //notfoundthread
        } else {
            System.out.println("sceKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Copy user data to the new thread's stack, since we are not
            // starting the thread immediately, only marking it as ready,
            // the data needs to be saved somewhere safe.
            Memory mem = Memory.getInstance();
            for (int i = 0; i < len; i++)
                mem.write8((thread.stack_addr - len + 1) + i, (byte)mem.read8(data_addr + i));

            thread.gpr[29] -= len; // Adjust sp for size of user data
            thread.gpr[4] = len; // a0 = len
            thread.gpr[5] = thread.gpr[29] + 1; // a1 = pointer to copy of data at data_addr
            thread.status = PspThreadStatus.PSP_THREAD_READY;

            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    /** exit the current thread */
    public void ThreadMan_sceKernelExitThread(int exitStatus) {
        System.out.println("sceKernelExitThread SceUID=" + Integer.toHexString(current_thread.uid)
            + " name:'" + current_thread.name + "' exitStatus:" + exitStatus);

        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
        current_thread.exitStatus = exitStatus;
        Emulator.getProcessor().gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** exit the current thread, then delete it */
    public void ThreadMan_sceKernelExitDeleteThread(int exitStatus) {
        SceKernelThreadInfo thread = current_thread; // save a reference for post context switch operations
        System.out.println("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(current_thread.uid)
            + " name:'" + current_thread.name + "' exitStatus:" + exitStatus);

        // Exit
        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
        current_thread.exitStatus = exitStatus;
        Emulator.getProcessor().gpr[2] = 0;

        // Mark thread for deletion
        thread.do_delete = true;

        contextSwitch(nextThread());
    }

    /** sleep the current thread until a registered callback is triggered */
    public void ThreadMan_sceKernelSleepThreadCB() {
        System.out.println("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        current_thread.do_callbacks = true;
        Emulator.getProcessor().gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** sleep the current thread */
    public void ThreadMan_sceKernelSleepThread() {
        System.out.println("sceKernelSleepThread SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        current_thread.do_callbacks = false;
        Emulator.getProcessor().gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** sleep the current thread for a certain number of microseconds */
    public void ThreadMan_sceKernelDelayThread(int a0) {
        current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
        //current_thread.delaysteps = a0 * 200000000 / 1000000; // TODO delaysteps = a0 * steprate
        current_thread.delaysteps = a0; // test version
        current_thread.do_callbacks = false;
        Emulator.getProcessor().gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** sleep the current thread for a certain number of microseconds */
    public void ThreadMan_sceKernelDelayThreadCB(int millis) {
        current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
        //current_thread.delaysteps = millis * 200000000 / 1000000; // TODO delaysteps = millis * steprate
        current_thread.delaysteps = millis; // test version
        current_thread.do_callbacks = true;
        Emulator.getProcessor().gpr[2] = 0;

        contextSwitch(nextThread());
    }
    
    public void ThreadMan_sceKernelCreateCallback(int a0, int a1, int a2) {
        String name = readStringZ(Memory.getInstance().mainmemory, (a0 & 0x3fffffff) - MemoryMap.START_RAM);
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, current_thread.uid, a1, a2);

        System.out.println("sceKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid)
            + " PC=" + Integer.toHexString(callback.callback_addr) + " name:'" + callback.name + "'");

        Emulator.getProcessor().gpr[2] = callback.uid;
    }

    public void ThreadMan_sceKernelGetThreadId() {
        //Get the current thread Id
        Emulator.getProcessor().gpr[2] = current_thread.uid;
    }

    public void ThreadMan_sceKernelReferThreadStatus(int a0, int a1) {
        //Get the status information for the specified thread
        SceKernelThreadInfo thread = threadlist.get(a0);
        if (thread == null) {
            Emulator.getProcessor().gpr[2] = 0x80020198; //notfoundthread
            return;
        }

        //System.out.println("sceKernelReferThreadStatus SceKernelThreadInfo=" + Integer.toHexString(a1));

        int i, len;
        Memory mem = Memory.getInstance();
        mem.write32(a1, 106); //struct size

        //thread name max 32bytes
        len = thread.name.length();
        if (len > 31) len = 31;
        for (i=0; i < len; i++)
            mem.write8(a1 +4 +i, (byte)thread.name.charAt(i));
        mem.write8(a1 +4 +i, (byte)0);

        mem.write32(a1 +36, thread.attr);
        mem.write32(a1 +40, thread.status.getValue());
        mem.write32(a1 +44, thread.entry_addr);
        mem.write32(a1 +48, thread.stack_addr);
        mem.write32(a1 +52, thread.stackSize);
        mem.write32(a1 +56, thread.gpReg_addr);
        mem.write32(a1 +60, thread.initPriority);
        mem.write32(a1 +64, thread.currentPriority);
        mem.write32(a1 +68, thread.waitType);
        mem.write32(a1 +72, thread.waitId);
        mem.write32(a1 +78, thread.wakeupCount);
        mem.write32(a1 +82, thread.exitStatus);
        mem.write64(a1 +86, thread.runClocks);
        mem.write32(a1 +94, thread.intrPreemptCount);
        mem.write32(a1 +98, thread.threadPreemptCount);
        mem.write32(a1 +102, thread.releaseCount);

        Emulator.getProcessor().gpr[2] = 0;
    }

    public void ThreadMan_sceKernelChangeThreadPriority(int uid, int priority) {
        SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Emulator.getProcessor().gpr[2] = 0x80020198; //notfoundthread
        } else {
            System.out.println("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(thread.uid)
                    + " newPriority:0x" + Integer.toHexString(priority) + " oldPriority:0x" + Integer.toHexString(thread.currentPriority));

            thread.currentPriority = priority;

            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelChangeCurrentThreadAttr(int unknown, int attr) {
        System.out.println("sceKernelChangeCurrentThreadAttr"
                + " unknown:" + unknown
                + " newAttr:0x" + Integer.toHexString(attr)
                + " oldAttr:0x" + Integer.toHexString(current_thread.attr));

        // Don't allow switching into kernel mode!
        if ((current_thread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER &&
            (attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
            System.out.println("sceKernelChangeCurrentThreadAttr forcing user mode");
            attr |= PSP_THREAD_ATTR_USER;
        }

        current_thread.attr = attr;

        Emulator.getProcessor().gpr[2] = 0;
    }

    public void ThreadMan_sceKernelWakeupThread(int uid) {
        System.out.println("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid));
        SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Emulator.getProcessor().gpr[2] = 0x80020198; //notfoundthread
        } else if (thread.status != PspThreadStatus.PSP_THREAD_SUSPEND) {
            System.out.println("sceKernelWakeupThread thread not suspended (status=" + thread.status + ")");
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            thread.status = PspThreadStatus.PSP_THREAD_READY;
            Emulator.getProcessor().gpr[2] = 0;
        }
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

    private static final int PSP_THREAD_ATTR_USER = 0x80000000;
    private static final int PSP_THREAD_ATTR_USBWLAN = 0xa0000000;
    private static final int PSP_THREAD_ATTR_VSH = 0xc0000000;
    private static final int PSP_THREAD_ATTR_KERNEL = 0x00001000; // TODO are module/thread attr interchangeable?
    private static final int PSP_THREAD_ATTR_VFPU = 0x00004000;
    private static final int PSP_THREAD_ATTR_SCRATCH_SRAM = 0x00008000;
    private static final int PSP_THREAD_ATTR_NO_FILLSTACK = 0x00100000; // Disables filling the stack with 0xFF on creation.
    private static final int PSP_THREAD_ATTR_CLEAR_STACK = 0x00200000; // Clear the stack when the thread is deleted.

    private int mallocStack(int size) {
        /*
        int p = 0x09f00000 - stackAllocated;
        stackAllocated += size;
        return p;
         */
        return pspSysMem.get_instance().malloc(2, pspSysMem.PSP_SMEM_High, size, 0);
    }

    private void memset(int address, byte c, int length) {
        Memory mem = Memory.getInstance();
        for (int i = 0; i < length; i++) {
            mem.write8(address + i, c);
        }
    }

    private class SceKernelThreadInfo implements Comparator<SceKernelThreadInfo> {
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
        private long runClocks;
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
        private long delaysteps;
        private boolean do_delete;
        private boolean do_callbacks; // in this implementation, only valid for PSP_THREAD_WAITING and PSP_THREAD_SUSPEND

        public SceKernelThreadInfo(String name, int entry_addr, int initPriority, int stackSize, int attr) {
            this.name = name;
            this.entry_addr = entry_addr;
            this.initPriority = initPriority;
            this.stackSize = stackSize;
            this.attr = attr;

            status = PspThreadStatus.PSP_THREAD_SUSPEND;
            stack_addr = mallocStack(stackSize);
            if ((attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK)
                memset(stack_addr - stackSize + 1, (byte)0xFF, stackSize);
            gpReg_addr = Emulator.getProcessor().gpr[28]; // inherit gpReg
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

            // Inherit context
            saveContext();
            // Thread specific registers
            pcreg = entry_addr;
            npcreg = entry_addr; // + 4;
            gpr[29] = stack_addr; //sp
            gpr[26] = gpr[29]; // k0 mirrors sp?

            // We'll hook "jr ra" where ra = 0 as the thread exiting
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

            // Assuming context switching only happens on syscall,
            // we always execute npc after a syscall,
            // so we can set pc = npc regardless of cop0.status.bd.
            //if (!cpu.cop0_status_bd)
                cpu.pc = cpu.npc;

            // TODO check attr for PSP_THREAD_ATTR_VFPU and restore vfpu registers
        }

        /** For use in the scheduler */
        @Override
        public int compare(SceKernelThreadInfo o1, SceKernelThreadInfo o2) {
            return o1.currentPriority - o2.currentPriority;
        }
    }
    public void ThreadMan_sceKernelCreateSema(int name_addr, int attr, int initVal, int maxVal, int option) 
    { 
        String name = readStringZ(Memory.getInstance().mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);
        
        System.out.println("sceKernelCreateSema name=" + name + " attr= " + attr + " initVal= " + initVal + " maxVal= "+ maxVal + " option= " + option);
        int initCount = initVal;
        int currentCount = initVal;
        int maxCount = maxVal;
        if(option !=0) System.out.println("sceKernelCreateSema: UNSUPPORTED Option Value");
        SceKernelSemaphoreInfo sema = new SceKernelSemaphoreInfo(name,attr,initCount,currentCount,maxCount);
        
        Emulator.getProcessor().gpr[2] = sema.uid;
    }
    public void ThreadMan_sceKernelWaitSema(int semaid , int signal , int timeoutptr , int timeout)
    {
          System.out.println("sceKernelWaitSema id= " + semaid + " signal= " + signal + " timeout = " + timeout);
            SceUIDMan.get_instance().checkUidPurpose(semaid, "ThreadMan-sema", true);
            SceKernelSemaphoreInfo sema = semalist.get(semaid);
            if (sema == null) {
                    System.out.println("sceKernelWaitSema - unknown uid " + Integer.toHexString(semaid));
                Emulator.getProcessor().gpr[2] = -1;
            } else {
                if(sema.currentCount >= signal)
                {
                  sema.currentCount-=signal;  
                  Emulator.getProcessor().gpr[2] = 0;
                }
                else
                {
                    waitingThreads.add(getCurrentThreadID());
                    System.out.println(getCurrentThreadID());
                    Emulator.getProcessor().gpr[2] = 0;
                    blockCurrentThread();
                }
                
            }
 
        
    }
    public void ThreadMan_sceKernelSignalSema(int semaid , int signal)
    {
        System.out.println("sceKernelSignalSema id =" + semaid + " signal =" + signal);
            SceUIDMan.get_instance().checkUidPurpose(semaid, "ThreadMan-sema", true);
            SceKernelSemaphoreInfo sema = semalist.get(semaid);
            if (sema == null) {
                    System.out.println("sceKernelSignalSema - unknown uid " + Integer.toHexString(semaid));
                Emulator.getProcessor().gpr[2] = -1;
            } else {
                int oldcurrentCount = sema.currentCount;
                sema.currentCount+=signal;
                Iterator<Integer> waitThreads = waitingThreads.iterator();
                while(waitThreads.hasNext())
                {
                  System.out.println("UNNNNNNNNNNNNNNNN Wait threads = " + waitThreads.next());   
                }
            }
            Emulator.getProcessor().gpr[2] = 0;
   
    }
    private class SceKernelSemaphoreInfo
    {
         private String name;
         private int attr;
         private int initCount;
         private int currentCount;
         private int maxCount;
         
         private int uid;
         public SceKernelSemaphoreInfo(String name, int attr, int initCount, int currentCount, int maxCount)
         {
             this.name=name;
             this.attr=attr;
             this.initCount=initCount;
             this.currentCount=currentCount;
             this.maxCount=maxCount;
             uid = SceUIDMan.get_instance().getNewUid("ThreadMan-sema");
             semalist.put(uid, this);
         }        
    }
}
