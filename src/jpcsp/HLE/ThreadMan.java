/*
Thread Manager
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/group__ThreadMan.html
- Schedule threads

Note:
- incomplete and not fully tested

Todo:
user/kernel read/write permissions on addresses (such as refer status)


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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.*;
import jpcsp.HLE.kernel.managers.SceUidManager;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;

public class ThreadMan {
    private static ThreadMan instance;
    private HashMap<Integer, SceKernelThreadInfo> threadMap;
    private HashMap<Integer, SceKernelSemaphoreInfo> semalist;
    private ArrayList<SceKernelThreadInfo> waitingThreads;
    private ArrayList<SceKernelThreadInfo> toBeDeletedThreads;
    private SceKernelThreadInfo current_thread;
    private SceKernelThreadInfo idle0, idle1;
    private int continuousIdleCycles; // watch dog timer - number of continuous cycles in any idle thread
    private int syscallFreeCycles; // watch dog timer - number of cycles since last syscall
    public Statistics statistics;

    // TODO figure out a decent number of cycles to wait
    private static final int WDT_THREAD_IDLE_CYCLES = 1000000;
    private static final int WDT_THREAD_HOG_CYCLES = (0x0A000000 - 0x08400000) * 3; // memset can take a while when you're using sb!

    private boolean insideCallback;
    private HashMap<Integer, SceKernelCallbackInfo> callbackMap;

    private boolean USE_THREAD_BANLIST = false;
    private static final boolean LOG_CONTEXT_SWITCHING = false;

    // see sceKernelGetThreadmanIdList
    public final static int SCE_KERNEL_TMID_Thread             = 1;
    public final static int SCE_KERNEL_TMID_Semaphore          = 2;
    public final static int SCE_KERNEL_TMID_EventFlag          = 3;
    public final static int SCE_KERNEL_TMID_Mbox               = 4;
    public final static int SCE_KERNEL_TMID_Vpl                = 5;
    public final static int SCE_KERNEL_TMID_Fpl                = 6;
    public final static int SCE_KERNEL_TMID_Mpipe              = 7;
    public final static int SCE_KERNEL_TMID_Callback           = 8;
    public final static int SCE_KERNEL_TMID_ThreadEventHandler = 9;
    public final static int SCE_KERNEL_TMID_Alarm              = 10;
    public final static int SCE_KERNEL_TMID_VTimer             = 11;
    public final static int SCE_KERNEL_TMID_SleepThread        = 64;
    public final static int SCE_KERNEL_TMID_DelayThread        = 65;
    public final static int SCE_KERNEL_TMID_SuspendThread      = 66;
    public final static int SCE_KERNEL_TMID_DormantThread      = 67;

    //private static int stackAllocated;

    public static ThreadMan getInstance() {
        if (instance == null) {
            instance = new ThreadMan();
        }
        return instance;
    }

    private ThreadMan() {
    }

    public Iterator<SceKernelThreadInfo> iterator() {
        return threadMap.values().iterator();
    }

    /** call this when resetting the emulator
     * @param entry_addr entry from ELF header
     * @param attr from sceModuleInfo ELF section header */
    public void Initialise(int entry_addr, int attr, String pspfilename) {
        //Modules.log.debug("ThreadMan: Initialise entry:0x" + Integer.toHexString(entry_addr));

        threadMap = new HashMap<Integer, SceKernelThreadInfo>();
        semalist = new HashMap<Integer, SceKernelSemaphoreInfo>();
        waitingThreads = new ArrayList<SceKernelThreadInfo>();
        toBeDeletedThreads = new ArrayList<SceKernelThreadInfo>();
        statistics = new Statistics();

        insideCallback = false;
        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();

        // Clear stack allocation info
        //pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_Addr, 0x000fffff, 0x09f00000);
        //stackAllocated = 0;

        install_idle_threads();

        // Create a thread the program will run inside
        current_thread = new SceKernelThreadInfo("root", entry_addr, 0x20, 0x4000, attr);
        threadMap.put(current_thread.uid, current_thread);

        // Set user mode bit if kernel mode bit is not present
        if ((current_thread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL) {
            current_thread.attr |= PSP_THREAD_ATTR_USER;
        }

        // Setup args by copying them onto the stack
        //Modules.log.debug("pspfilename - '" + pspfilename + "'");
        int len = pspfilename.length();
        int address = current_thread.cpuContext.gpr[29];
        writeStringZ(Memory.getInstance(), address, pspfilename);
        current_thread.cpuContext.gpr[4] = len + 1; // a0 = len + string terminator
        current_thread.cpuContext.gpr[5] = address; // a1 = pointer to arg data in stack

        current_thread.status = PSP_THREAD_READY;

        // Switch in the thread
        current_thread.status = PSP_THREAD_RUNNING;
        current_thread.restoreContext();
        syscallFreeCycles = 0;
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
        int instruction_syscall = // syscall 0x0201c [sceKernelDelayThread]
            ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f)
            | ((0x201c & 0x000fffff) << 6);

        // TODO
        //pspSysMem.getInstance().malloc(1, pspSysMem.PSP_SMEM_Addr, 16, MemoryMap.START_RAM);

        Memory.getInstance().write32(MemoryMap.START_RAM + 0, instruction_addiu);
        Memory.getInstance().write32(MemoryMap.START_RAM + 4, instruction_lui);
        Memory.getInstance().write32(MemoryMap.START_RAM + 8, instruction_jr);
        Memory.getInstance().write32(MemoryMap.START_RAM + 12, instruction_syscall);

        idle0 = new SceKernelThreadInfo("idle0", MemoryMap.START_RAM | 0x80000000, 0x7f, 0x0, PSP_THREAD_ATTR_KERNEL);
        idle0.status = PSP_THREAD_READY;
        threadMap.put(idle0.uid, idle0);

        idle1 = new SceKernelThreadInfo("idle1", MemoryMap.START_RAM | 0x80000000, 0x7f, 0x0, PSP_THREAD_ATTR_KERNEL);
        idle1.status = PSP_THREAD_READY;
        threadMap.put(idle1.uid, idle1);

        continuousIdleCycles = 0;
    }

    /** to be called when exiting the emulation */
    public void exit() {
        if (threadMap != null) {
            // Delete all the threads to collect statistics
            while (!threadMap.isEmpty()) {
                SceKernelThreadInfo thread = threadMap.values().iterator().next();
                deleteThread(thread);
            }

            statistics.endTimeMillis = System.currentTimeMillis();
            Modules.log.info("ThreadMan Statistics (" + statistics.allCycles + " cycles in " + String.format("%.3f", statistics.getDurationMillis() / 1000.0) + "s):");
            for (Iterator<Statistics.ThreadStatistics> it = statistics.threads.iterator(); it.hasNext(); ) {
                Statistics.ThreadStatistics threadStatistics = it.next();
                double percentage = 0;
                if (statistics.allCycles != 0) {
                	percentage = (threadStatistics.runClocks / (double) statistics.allCycles) * 100;
                }
                Modules.log.info("    Thread name:'" + threadStatistics.name + "' runClocks:" + threadStatistics.runClocks + " (" + String.format("%2.2f%%", percentage) + ")");
            }
        }
    }

    /** to be called from the main emulation loop */
    public void step() {
        CpuState cpu = Emulator.getProcessor().cpu;
        if (current_thread != null) {
            current_thread.runClocks++;

            //Modules.log.debug("pc=" + Emulator.getProcessor().pc + " ra=" + Emulator.getProcessor().gpr[31]);

            // Hook jr ra to 0 (thread function returned)
            if (cpu.pc == 0 && cpu.gpr[31] == 0) {
                if (insideCallback) {
                    // Callback has exited
                    Modules.log.debug("Callback exit detected");

                    // Not sure which of these two to use
                    //Emulator.getProcessor().cpu = current_thread.cpuContext;
                    current_thread.restoreContext(); // also sets pc = npc

                    // keep processing callbacks
                    checkCallbacks();
                } else {
                    // Thread has exited
                    Modules.log.debug("Thread exit detected SceUID=" + Integer.toHexString(current_thread.uid)
                        + " name:'" + current_thread.name + "' return:0x" + Integer.toHexString(cpu.gpr[2]));
                    current_thread.exitStatus = cpu.gpr[2]; // v0
                    changeThreadState(current_thread, PSP_THREAD_STOPPED);
                    contextSwitch(nextThread());
                }
            }

            // Watch dog timer
            if (isIdleThread(current_thread)) {
                continuousIdleCycles++;
                if (continuousIdleCycles > WDT_THREAD_IDLE_CYCLES) {
                    Modules.log.info("Watch dog timer - pausing emulator (idle)");
                    Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_WDT_IDLE);
                }
            } else {
                continuousIdleCycles = 0;
                syscallFreeCycles++;
                if (syscallFreeCycles > WDT_THREAD_HOG_CYCLES) {
                    Modules.log.info("Watch dog timer - pausing emulator (thread hogging)");
                    Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_WDT_HOG);
                }
            }
        } else {
            // We always need to be in a thread! we shouldn't get here.
            Modules.log.error("No ready threads!");
        }

        if (!waitingThreads.isEmpty()) {
            long microTimeNow = Emulator.getClock().microTime();
            // Access waitingThreads using array indexing because
            // this is more efficient than iterator access for short lists
            for (int i = 0; i < waitingThreads.size(); i++) {
                SceKernelThreadInfo thread = waitingThreads.get(i);
                if (!thread.wait.forever && microTimeNow >= thread.wait.microTimeTimeout) {
                    onWaitTimeout(thread);
                    changeThreadState(thread, PSP_THREAD_READY);
                }
            }
        }

        // Cleanup stopped threads (deferred deletion)
        for (int i = 0; i < toBeDeletedThreads.size(); i++) {
            SceKernelThreadInfo thread = toBeDeletedThreads.get(i);
            // this check shouldn't be necessary anymore, can be inferred by the thread existing in the toBeDeletedThreads list
            if (thread.do_delete) {
                deleteThread(thread);
            } else {
                Modules.log.warn("thread:'" + thread.name + "' in toBeDeletedThreads list with do_delete = false");
            }
        }

        /* this isn't really necessary if we only handle the umd callback.
         * the other way is when we implement exit and power callback -
         * bound to some key press or ui button - we manually call checkCallbacks().
        // Try and process callbacks
        if (!insideCallback) {
            checkCallbacks();
        }
        */
    }

    /** Part of watch dog timer */
    public void clearSyscallFreeCycles() {
        syscallFreeCycles = 0;
    }

    /** @param newthread The thread to switch in. */
    public void contextSwitch(SceKernelThreadInfo newthread) {

        if (insideCallback) {
            Modules.log.warn("contextSwitch called from inside callback");
            //Emulator.PauseEmu();
        }

        if (current_thread != null) {
            // Switch out old thread
            if (current_thread.status == PSP_THREAD_RUNNING) {
                changeThreadState(current_thread, PSP_THREAD_READY);
            }

            // save registers
            current_thread.saveContext();

            /*
            Modules.log.debug("saveContext SceUID=" + Integer.toHexString(current_thread.uid)
                + " name:" + current_thread.name
                + " PC:" + Integer.toHexString(current_thread.pcreg)
                + " NPC:" + Integer.toHexString(current_thread.npcreg));
            */
        }

        if (newthread != null) {
            // Switch in new thread
            changeThreadState(newthread, PSP_THREAD_RUNNING);
            newthread.wakeupCount++; // check
            // restore registers
            newthread.restoreContext();

            if (LOG_CONTEXT_SWITCHING && !isIdleThread(newthread)) {
                //Modules.log.debug("ThreadMan: switched to thread SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
                Modules.log.debug("---------------------------------------- SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
                /*
                Modules.log.debug("restoreContext SceUID=" + Integer.toHexString(newthread.uid)
                    + " name:" + newthread.name
                    + " PC:" + Integer.toHexString(newthread.cpuContext.pc)
                    + " NPC:" + Integer.toHexString(newthread.cpuContext.npc));
                */
            }

            //Emulator.PauseEmu();
        } else {
            // Shouldn't get here now we are using idle threads
            Modules.log.info("No ready threads - pausing emulator");
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
        }

        current_thread = newthread;
        syscallFreeCycles = 0;

        RuntimeContext.update();
    }

    /** This function must have the property of never returning current_thread,
     * unless current_thread is already null.
     * @return The next thread to schedule (based on thread priorities). */
    public SceKernelThreadInfo nextThread() {
        Collection<SceKernelThreadInfo> c;
        List<SceKernelThreadInfo> list;
        Iterator<SceKernelThreadInfo> it;
        SceKernelThreadInfo found = null;

        // Find the thread with status PSP_THREAD_READY and the highest priority
        // In this implementation low priority threads can get starved
        c = threadMap.values();
        list = new LinkedList<SceKernelThreadInfo>(c);
        Collections.sort(list, idle0); // We need an instance of SceKernelThreadInfo for the comparator, so we use idle0
        it = list.iterator();
        while(it.hasNext()) {
            SceKernelThreadInfo thread = it.next();
            //Modules.log.debug("nextThread pri=" + Integer.toHexString(thread.currentPriority) + " name:" + thread.name + " status:" + thread.status);

            if (thread != current_thread &&
                thread.status == PSP_THREAD_READY) {
                found = thread;
                break;
            }
        }

        return found;
    }

    public int getCurrentThreadID() {
    	if (current_thread == null) {
    		return -1;
    	}

    	return current_thread.uid;
    }

    public SceKernelThreadInfo getCurrentThread() {
        return current_thread;
    }

    public boolean isIdleThread(SceKernelThreadInfo thread) {
        return (thread == idle0 || thread == idle1);
    }

    public String getThreadName(int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            return "NOT A THREAD";
        } else {
            return thread.name;
        }
    }

    /** Moves the current thread from running to ready and switches in the next
     * ready thread (which may be an idle thread if no other threads are ready). */
    public void yieldCurrentThread()
    {
        if (LOG_CONTEXT_SWITCHING)
            Modules.log.debug("-------------------- yield SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "' caller:" + getCallingFunction());

        contextSwitch(nextThread());
    }

    /* The same as yieldCurrentThread, except combined with sceKernelCheckCallback */
    public void yieldCurrentThreadCB()
    {
        if (LOG_CONTEXT_SWITCHING)
            Modules.log.debug("-------------------- yield CB SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "' caller:" + getCallingFunction());

        current_thread.do_callbacks = true;
        contextSwitch(nextThread());
        checkCallbacks();
    }

    public void blockCurrentThread()
    {
        if (LOG_CONTEXT_SWITCHING)
            Modules.log.debug("-------------------- block SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "' caller:" + getCallingFunction());

        changeThreadState(current_thread, PSP_THREAD_SUSPEND);
        contextSwitch(nextThread());
    }

    public void unblockThread(int uid)
    {
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadMap.get(uid);
            changeThreadState(thread, PSP_THREAD_READY);

            if (LOG_CONTEXT_SWITCHING)
                Modules.log.debug("-------------------- unblock SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' caller:" + getCallingFunction());
        }
    }

    private String getCallingFunction()
    {
        String msg = "";
        try {
            throw new Exception();
        } catch(Exception e) {
            StackTraceElement[] lines = e.getStackTrace();
            if (lines.length >= 2) {
                msg = lines[2].toString();
                msg = msg.substring(0, msg.indexOf("("));
                //msg = "'" + msg.substring(msg.lastIndexOf(".") + 1, msg.length()) + "'";
                String[] parts = msg.split("\\.");
                msg = "'" + parts[parts.length - 2] + "." + parts[parts.length - 1] + "'";
            } else {
                for (int i = 0; i < lines.length && i < 10; i++)
                {
                    String line = lines[i].toString();
                    if (line.startsWith("jpcsp.Allegrex") || line.startsWith("jpcsp.Processor"))
                        break;
                    msg += "\n" + line;
                }
            }
        }
        return msg;
    }

    /** Call this when a thread's wait timeout has expired.
     * You can assume the calling function will set thread.status = ready. */
    private void onWaitTimeout(SceKernelThreadInfo thread) {
        // ThreadEnd
        if (thread.wait.waitingOnThreadEnd) {
            //Modules.log.debug("ThreadEnd timedout");

            // Untrack
            thread.wait.waitingOnThreadEnd = false;

            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        }

        // EventFlag
        else if (thread.wait.waitingOnEventFlag) {
            //Modules.log.debug("EventFlag timedout");

            // Untrack
            thread.wait.waitingOnEventFlag = false;

            // Update numWaitThreads
            SceKernelEventFlagInfo event = Managers.eventFlags.get(thread.wait.EventFlag_id);
            if (event != null) {
                event.numWaitThreads--;

                // Return WAIT_TIMEOUT
                thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
            } else {
                Modules.log.warn("EventFlag deleted while we were waiting for it! (timeout expired)");

                // Return WAIT_DELETE
                thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
            }
        }
    }

    private void deleteThread(SceKernelThreadInfo thread) {
        Modules.log.debug("really deleting thread:'" + thread.name + "'");

        // cleanup thread - free the stack
        if (thread.stack_addr != 0) {
            Modules.log.debug("thread:'" + thread.name + "' freeing stack " + String.format("0x%08X", thread.stack_addr - thread.stackSize));
            pspSysMem.getInstance().free(thread.stack_addr - thread.stackSize);
        }

        waitingThreads.remove(thread);
        toBeDeletedThreads.remove(thread);
        threadMap.remove(thread.uid);
        SceUidManager.releaseUid(thread.uid, "ThreadMan-thread");

        // TODO remove from wait object reference count, example: sema.numWaitThreads--

        statistics.addThreadStatistics(thread);
    }

    private void setToBeDeletedThread(SceKernelThreadInfo thread) {
        thread.do_delete = true;

        if (thread.status == PSP_THREAD_STOPPED) {
            toBeDeletedThreads.add(thread);
        }
    }

    /** Use this to change a thread's state (ready, running, etc)
     * This function manages some lists such as waiting list and
     * deferred deletion list. */
    public void changeThreadState(SceKernelThreadInfo thread, int newStatus) {
        if (thread == null) {
            return;
        }

        if (thread.status == PSP_THREAD_WAITING) {
            waitingThreads.remove(thread);
            thread.do_callbacks = false;
        } else if (thread.status == PSP_THREAD_STOPPED) {
            toBeDeletedThreads.remove(thread);
        }

        thread.status = newStatus;

        if (thread.status == PSP_THREAD_WAITING) {
            if (!thread.wait.forever) {
                waitingThreads.add(thread);
            }
        } else if (thread.status == PSP_THREAD_STOPPED) {
            if (thread.do_delete) {
                toBeDeletedThreads.add(thread);
            }
            onThreadStopped(thread);
        }
    }

    private void onThreadStopped(SceKernelThreadInfo stoppedThread) {
        for (Iterator<SceKernelThreadInfo> it = threadMap.values().iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            // Wakeup threads that are in sceKernelWaitThreadEnd
            // We're assuming if waitingOnThreadEnd is set then thread.status = waiting
            if (thread.wait.waitingOnThreadEnd &&
                thread.wait.ThreadEnd_id == stoppedThread.uid) {
                // Untrack
                thread.wait.waitingOnThreadEnd = false;

                // Return success
                thread.cpuContext.gpr[2] = 0;

                // Wakeup
                changeThreadState(thread, PSP_THREAD_READY);
            }
        }
    }


    public SceKernelThreadInfo hleKernelCreateThread(String name, int entry_addr,
        int initPriority, int stackSize, int attr, int option_addr) {

        if (option_addr != 0)
            Modules.log.warn("hleKernelCreateThread unhandled SceKernelThreadOptParam");

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);
        threadMap.put(thread.uid, thread);

        Modules.log.debug("hleKernelCreateThread SceUID=" + Integer.toHexString(thread.uid)
            + " name:'" + thread.name
            + "' PC=" + Integer.toHexString(thread.cpuContext.pc)
            + " attr:0x" + Integer.toHexString(attr)
            + " pri:0x" + Integer.toHexString(initPriority));

        return thread;
    }

    public void ThreadMan_sceKernelCreateThread(int name_addr, int entry_addr,
        int initPriority, int stackSize, int attr, int option_addr) {

        String name = readStringZ(name_addr);

        Modules.log.debug("sceKernelCreateThread redirecting to hleKernelCreateThread");
        SceKernelThreadInfo thread = hleKernelCreateThread(name, entry_addr, initPriority, stackSize, attr, option_addr);

        // Inherit kernel mode if user mode bit is not set
        if ((current_thread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL &&
            (attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
            Modules.log.debug("sceKernelCreateThread inheriting kernel mode");
            thread.attr |= PSP_THREAD_ATTR_KERNEL;
        }

        // Inherit user mode
        if ((current_thread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER) {
            if ((thread.attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER)
                Modules.log.debug("sceKernelCreateThread inheriting user mode");
            thread.attr |= PSP_THREAD_ATTR_USER;
            // Always remove kernel mode bit
            thread.attr &= ~PSP_THREAD_ATTR_KERNEL;
        }

        Emulator.getProcessor().cpu.gpr[2] = thread.uid;
    }

    /** terminate thread a0 */
    public void ThreadMan_sceKernelTerminateThread(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            Emulator.getProcessor().cpu.gpr[2] = 0;
            changeThreadState(thread, PSP_THREAD_STOPPED);  // PSP_THREAD_STOPPED or PSP_THREAD_KILLED ?
        }
    }

    /** mark thread a0 for deletion */
    public void ThreadMan_sceKernelDeleteThread(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Mark thread for deletion
            setToBeDeletedThread(thread);

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** terminate thread a0, then mark it for deletion */
    public void ThreadMan_sceKernelTerminateDeleteThread(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelTerminateDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Change thread state to stopped
            changeThreadState(thread, PSP_THREAD_STOPPED);

            // Mark thread for deletion
            setToBeDeletedThread(thread);

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void setThreadBanningEnabled(boolean enabled) {
        USE_THREAD_BANLIST = enabled;
        Modules.log.info("Audio threads disabled: " + USE_THREAD_BANLIST);
    }

    /** use lower case in this list */
    private final String[] threadNameBanList = new String[] {
        "bgm thread", "sgx-psp-freq-thr"
    };
    /* suspected sound thread names:
     * SndMain, SoundThread, At3Main, Atrac3PlayThread,
     * bgm thread, SceWaveMain, SasCore thread, soundThread,
     * ATRAC3 play thread, SAS Thread, XomAudio, sgx-psp-freq-thr,
     * sgx-psp-at3-th, sgx-psp-pcm-th, sgx-psp-sas-th, snd_tick_timer_thread,
     * snd_stream_service_thread_1, SAS / Main Audio, AudioMixThread,
     * snd_stream_service_thread_0, sound_poll_thread, stream_sound_poll_thread,
     *
     * keywords:
     * snd, sound, at3, atrac3, sas, wave, pcm, audio
     *
     * false positives:
     * pcm: SPCMain (Skate Park City Main)
     *
     * ambiguous keywords:
     * bgm, freq, sgx
     */
    private boolean isBannedThread(SceKernelThreadInfo thread) {
        if (USE_THREAD_BANLIST) {
            String name = thread.name.toLowerCase();
            if (name.contains("snd") || name.contains("sound") ||
                name.contains("at3") || name.contains("atrac") ||
                name.contains("sas") || name.contains("wave") ||
                name.contains("audio") || name.contains("sgx-psp-pcm-th")) {
                return true;
            }

            for (String threadName : threadNameBanList) {
                if (name.equals(threadName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void hleKernelStartThread(SceKernelThreadInfo thread, int userDataLength, int userDataAddr, int gp) {

        Modules.log.debug("hleKernelStartThread SceUID=" + Integer.toHexString(thread.uid)
            + " name:'" + thread.name
            + "' dataLen=0x" + Integer.toHexString(userDataLength)
            + " data=0x" + Integer.toHexString(userDataAddr)
            + " gp=0x" + Integer.toHexString(gp));

        // Setup args by copying them onto the stack
        int address = thread.cpuContext.gpr[29];
        if (userDataAddr != 0) //{
            Memory.getInstance().memcpy(address, userDataAddr, userDataLength);
            thread.cpuContext.gpr[4] = userDataLength; // a0 = user data len
            thread.cpuContext.gpr[5] = address; // a1 = pointer to arg data in stack
        /*
        } else {
            // TODO check if we should do this when user data pointer = NULL
            thread.cpuContext.gpr[4] = 0; // a0 = user data len
            thread.cpuContext.gpr[5] = 0; // a1 = pointer to arg data in stack
        }*/

        // testing
        if (thread.cpuContext.gpr[28] != gp) {
            Modules.log.debug("hleKernelStartThread oldGP=0x" + Integer.toHexString(thread.cpuContext.gpr[28])
                + " newGP=0x" + Integer.toHexString(gp));
        }
        thread.cpuContext.gpr[28] = gp;

        // Start thread immediately
        changeThreadState(thread, PSP_THREAD_READY);
        contextSwitch(thread);
    }

    public void ThreadMan_sceKernelStartThread(int uid, int len, int data_addr) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("sceKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' banned, not starting");
            // Banned, fake start
            Emulator.getProcessor().cpu.gpr[2] = 0;
            contextSwitch(nextThread());
        } else {
            Modules.log.debug("sceKernelStartThread redirecting to hleKernelStartThread");
            hleKernelStartThread(thread, len, data_addr, thread.gpReg_addr);
        }
    }

    /** exit the current thread */
    public void ThreadMan_sceKernelExitThread(int exitStatus) {
        Modules.log.debug("sceKernelExitThread SceUID=" + Integer.toHexString(current_thread.uid)
            + " name:'" + current_thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));

        current_thread.exitStatus = exitStatus;
        Emulator.getProcessor().cpu.gpr[2] = 0;
        changeThreadState(current_thread, PSP_THREAD_STOPPED);

        contextSwitch(nextThread());
    }

    /** exit the current thread, then delete it */
    public void ThreadMan_sceKernelExitDeleteThread(int exitStatus) {
        SceKernelThreadInfo thread = current_thread; // save a reference for post context switch operations
        Modules.log.debug("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(current_thread.uid)
            + " name:'" + current_thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));

        // Exit
        current_thread.exitStatus = exitStatus;
        Emulator.getProcessor().cpu.gpr[2] = 0;
        changeThreadState(current_thread, PSP_THREAD_STOPPED);

        // Mark thread for deletion
        setToBeDeletedThread(thread);

        contextSwitch(nextThread());
    }

    private void hleKernelSleepThread(boolean do_callbacks) {
        // Go to wait state, callbacks
        current_thread.do_callbacks = do_callbacks;

        // Wait forever (another thread will call sceKernelWakeupThread)
        hleKernelThreadWait(current_thread.wait, 0, true);

        changeThreadState(current_thread, PSP_THREAD_WAITING);

        Emulator.getProcessor().cpu.gpr[2] = 0;
        yieldCurrentThread();
    }

    /** sleep the current thread (using wait) */
    public void ThreadMan_sceKernelSleepThread() {
        Modules.log.debug("sceKernelSleepThread SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        hleKernelSleepThread(false);
    }

    /** sleep the current thread and handle callbacks (using wait)
     * in our implementation we have to use wait, not suspend otherwise we don't handle callbacks. */
    public void ThreadMan_sceKernelSleepThreadCB() {
        Modules.log.debug("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        hleKernelSleepThread(true);
        checkCallbacks();
    }

    public void ThreadMan_sceKernelWakeupThread(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (thread.status != PSP_THREAD_WAITING) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " not sleeping/waiting (status=" + thread.status + ")");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_THREAD_IS_NOT_WAIT;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not waking up");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Modules.log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
            changeThreadState(thread, PSP_THREAD_READY);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelSuspendThread(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid));
            changeThreadState(thread, PSP_THREAD_SUSPEND);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelResumeThread(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (thread.status != PSP_THREAD_SUSPEND) {
            Modules.log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " not suspended (status=" + thread.status + ")");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_THREAD_IS_NOT_SUSPEND;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not resuming");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Modules.log.debug("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
            changeThreadState(thread, PSP_THREAD_READY);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private void hleKernelWaitThreadEnd(int uid, int micros, boolean callbacks) {
        Modules.log.debug("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid)
            + " timeout=" + micros
            + " callbacks=" + callbacks);

        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("hleKernelWaitThreadEnd unknown thread 0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 0;
            contextSwitch(nextThread()); // yield
        } else if (thread.status == PSP_THREAD_STOPPED) {
            Modules.log.debug("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' thread already stopped, not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 0;
            contextSwitch(nextThread()); // yield
        } else {
            // Go to wait state
            current_thread.do_callbacks = callbacks;

            // Wait on a specific thread end
            hleKernelThreadWait(current_thread.wait, micros, (micros == 0)); // TODO check forever

            current_thread.wait.waitingOnThreadEnd = true;
            current_thread.wait.ThreadEnd_id = uid;

            changeThreadState(current_thread, PSP_THREAD_WAITING);

            contextSwitch(nextThread());
        }
    }

    public void ThreadMan_sceKernelWaitThreadEnd(int uid, int micros) {
        Modules.log.debug("sceKernelWaitThreadEnd redirecting to hleKernelWaitThreadEnd(callbacks=false)");
        hleKernelWaitThreadEnd(uid, micros, false);
    }

    public void ThreadMan_sceKernelWaitThreadEndCB(int uid, int micros) {
        Modules.log.debug("sceKernelWaitThreadEndCB redirecting to hleKernelWaitThreadEnd(callbacks=true)");
        hleKernelWaitThreadEnd(uid, micros, true);
        checkCallbacks();
    }

    public void hleKernelThreadWait(ThreadWaitInfo wait, int micros, boolean forever) {
        wait.forever = forever;
        wait.microTimeTimeout = Emulator.getClock().microTime() + micros;

        if (LOG_CONTEXT_SWITCHING && !isIdleThread(current_thread))
            Modules.log.debug("hleKernelThreadWait micros=" + micros + " forever:" + forever + " thread:'" + current_thread.name + "' caller:" + getCallingFunction());
    }

    private void hleKernelDelayThread(int micros, boolean do_callbacks) {
        // Go to wait state, callbacks
        current_thread.do_callbacks = do_callbacks;

        // Wait on a timeout only
        hleKernelThreadWait(current_thread.wait, micros, false);

        changeThreadState(current_thread, PSP_THREAD_WAITING);

        Emulator.getProcessor().cpu.gpr[2] = 0;
        contextSwitch(nextThread());
    }

    /** wait the current thread for a certain number of microseconds */
    public void ThreadMan_sceKernelDelayThread(int micros) {
        hleKernelDelayThread(micros, false);
    }

    /** wait the current thread for a certain number of microseconds */
    public void ThreadMan_sceKernelDelayThreadCB(int micros) {
        //if (micros > 10000)
        //    Modules.log.debug("sceKernelDelayThreadCB micros=" + micros + " current thread:'" + current_thread.name + "'");

        hleKernelDelayThread(micros, true);
        checkCallbacks();
    }

    public void ThreadMan_sceKernelCreateCallback(int name_addr, int func_addr, int user_arg_addr) {
        String name = readStringZ(name_addr);
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, current_thread.uid, func_addr, user_arg_addr);
        callbackMap.put(callback.uid, callback);

        Modules.log.debug("sceKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid)
            + " PC=" + Integer.toHexString(func_addr)
            + " name:'" + name
            + "' thread:'" + current_thread.name + "'");

        Emulator.getProcessor().cpu.gpr[2] = callback.uid;
    }

    public void ThreadMan_sceKernelDeleteCallback(int uid) {
        SceKernelCallbackInfo info = callbackMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteCallback unknown uid 0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            Modules.log.debug("sceKernelDeleteCallback SceUID=" + Integer.toHexString(uid)
                + " name:'" + info.name + "'");

            // TODO automatically unregister the callback if it was registered with another system?
            // example: sceKernelDeleteCallback called before sceUmdUnRegisterUMDCallBack
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelCheckCallback() {
        Modules.log.debug("sceKernelCheckCallback");
        current_thread.do_callbacks = true;
        checkCallbacks();
    }

    public void ThreadMan_sceKernelReferCallbackStatus(int uid, int info_addr) {
        Modules.log.debug("sceKernelReferCallbackStatus SceUID=" + Integer.toHexString(uid)
            + " info=" + Integer.toHexString(info_addr));

        Memory mem = Memory.getInstance();
        SceKernelCallbackInfo info = callbackMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferCallbackStatus unknown uid 0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else if (!mem.isAddressGood(info_addr)) {
            Modules.log.warn("sceKernelReferCallbackStatus bad info address 0x" + Integer.toHexString(info_addr));
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            int size = mem.read32(info_addr);
            if (size != SceKernelCallbackInfo.size) {
                Modules.log.warn("sceKernelReferCallbackStatus bad info size got " + size + " want " + SceKernelCallbackInfo.size);
                Emulator.getProcessor().cpu.gpr[2] = -1;
            } else {
                info.write(mem, info_addr);
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }
        }
    }

    public void ThreadMan_sceKernelGetThreadId() {
        //Get the current thread Id
        Emulator.getProcessor().cpu.gpr[2] = current_thread.uid;
    }

    public void ThreadMan_sceKernelGetThreadCurrentPriority() {
        Emulator.getProcessor().cpu.gpr[2] = current_thread.currentPriority;
    }

    public void ThreadMan_sceKernelGetThreadExitStatus(int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelGetThreadExitStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else  {
            Modules.log.debug("sceKernelGetThreadExitStatus uid=0x" + Integer.toHexString(uid) + " exitStatus=0x" + Integer.toHexString(thread.exitStatus));
            Emulator.getProcessor().cpu.gpr[2] = thread.exitStatus;
        }
    }

    /** @return amount of free stack space. */
    public void ThreadMan_sceKernelCheckThreadStack() {
        Emulator.getProcessor().cpu.gpr[2] = current_thread.stackSize
            - (current_thread.stack_addr - Emulator.getProcessor().cpu.gpr[29])
            - 0x130;
    }

    /** @return amount of free stack space. */
    public void ThreadMan_sceKernelGetThreadStackFreeSize(int uid) {
        if (uid == 0) uid = current_thread.uid;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelGetThreadStackFreeSize unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else  {
        Emulator.getProcessor().cpu.gpr[2] = current_thread.stackSize
            - (current_thread.stack_addr - Emulator.getProcessor().cpu.gpr[29])
            - 0x130
            - 0xfb0;
        }
    }

    public void ThreadMan_sceKernelReferThreadStatus(int uid, int addr) {
        //Get the status information for the specified thread
        if (uid == 0) uid = current_thread.uid;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelReferThreadStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else  {
            //Modules.log.debug("sceKernelReferThreadStatus uid=0x" + Integer.toHexString(uid));
            thread.write(Memory.getInstance(), addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** Write uid's to buffer */
    public void ThreadMan_sceKernelGetThreadmanIdList(int type,
        int readbuf_addr, int readbufsize, int idcount_addr) {
        Memory mem = Memory.getInstance();

        Modules.log.debug("sceKernelGetThreadmanIdList type=" + type
            + " readbuf:0x" + Integer.toHexString(readbuf_addr)
            + " readbufsize:" + readbufsize
            + " idcount:0x" + Integer.toHexString(idcount_addr));

        // TODO type=SCE_KERNEL_TMID_Thread, don't show the idle threads!

        int count = 0;

        switch(type) {
            case SCE_KERNEL_TMID_Thread:
                for (Iterator<SceKernelThreadInfo> it = threadMap.values().iterator(); it.hasNext() /*&& count < readbufsize*/; ) {
                    SceKernelThreadInfo thread = it.next();

                    // Hide kernel mode threads when called from a user mode thread
                    if ((thread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL ||
                        (current_thread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL) {

                        if (count < readbufsize) {
                            Modules.log.debug("sceKernelGetThreadmanIdList adding thread '" + thread.name + "'");
                            mem.write32(readbuf_addr + count * 4, thread.uid);
                            count++;
                        } else {
                            Modules.log.warn("sceKernelGetThreadmanIdList NOT adding thread '" + thread.name + "'");
                        }
                    }
                }
                break;

            default:
                Modules.log.warn("UNIMPLEMENTED:sceKernelGetThreadmanIdList type=" + type);
                break;
        }

        // Fake success - 0 entries written
        if (mem.isAddressGood(idcount_addr)) {
            idcount_addr = count;
        }

        Emulator.getProcessor().cpu.gpr[2] = count; // TODO or idcount_addr?
    }

    public void ThreadMan_sceKernelChangeThreadPriority(int uid, int priority) {
        if (uid == 0) uid = current_thread.uid;
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid)
                    + " newPriority:0x" + Integer.toHexString(priority) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(thread.uid)
                    + " newPriority:0x" + Integer.toHexString(priority) + " oldPriority:0x" + Integer.toHexString(thread.currentPriority));

            thread.currentPriority = priority;

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelChangeCurrentThreadAttr(int removeAttr, int addAttr) {
        Modules.log.debug("sceKernelChangeCurrentThreadAttr"
                + " removeAttr:0x" + Integer.toHexString(removeAttr)
                + " addAttr:0x" + Integer.toHexString(addAttr)
                + " oldAttr:0x" + Integer.toHexString(current_thread.attr));

        // Probably meant to be sceKernelChangeThreadAttr unknown=uid
        if (removeAttr != 0)
            Modules.log.warn("sceKernelChangeCurrentThreadAttr removeAttr:0x" + Integer.toHexString(removeAttr) + " non-zero");

        int newAttr = (current_thread.attr & ~removeAttr) | addAttr;
        // Don't allow switching into kernel mode!
        if ((current_thread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER &&
            (newAttr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
            Modules.log.debug("sceKernelChangeCurrentThreadAttr forcing user mode");
            newAttr |= PSP_THREAD_ATTR_USER;
        }

        current_thread.attr = newAttr;

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }


    public void setUMDCallback(int cbid) {
        SceKernelCallbackInfo callback = callbackMap.get(cbid);
        if (callback == null) {
            Modules.log.error("setUMDCallback bad cbid " + Integer.toHexString(cbid));
            Emulator.PauseEmu();
        }
        current_thread.umdCallbackRegistered = true;
        current_thread.umdCallbackInfo = callback;
    }

    public void clearUMDCallback() {
        current_thread.umdCallbackRegistered = false;
        current_thread.umdCallbackInfo = null;
    }

    // TODO add other pushXXXCallback functions
    public void pushUMDCallback(Iterator<Integer> cbidList, int event) {
        Modules.log.debug("pushUMDCallback event 0x" + Integer.toHexString(event));

        while (cbidList.hasNext()) {
            int cbid = cbidList.next();
            SceKernelCallbackInfo callback = callbackMap.get(cbid);
            if (callback == null) {
                Modules.log.warn("pushUMDCallback callback not found " + Integer.toHexString(cbid));
            } else {
                SceKernelThreadInfo thread = threadMap.get(callback.threadId);
                if (thread == null) {
                    Modules.log.warn("pushUMDCallback thread not found " + Integer.toHexString(callback.threadId));
                } else if (thread.umdCallbackRegistered) { // We could ignore all the above stuff, iterate all threads and only use this condition
                    if (thread.umdCallbackReady) {
                        // Behaviour may be undefined - example: crash this thread, but continue other threads as normal
                        Modules.log.warn("pushUMDCallback thread:'" + thread.name
                            + "' already has callback pending (oldArg=0x" + Integer.toHexString(thread.umdCallbackInfo.notifyArg)
                            + ",newArg=0x" + Integer.toHexString(event) + ")");
                    }

                    thread.umdCallbackReady = true;
                    thread.umdCallbackInfo.notifyArg = event;
                }
            }
        }

        if (!insideCallback) {
            //Modules.log.debug("pushUMDCallback calling checkCallbacks");
            checkCallbacks();
        } else {
            Modules.log.error("pushUMDCallback called while inside another callback!");
            Emulator.PauseEmu();
        }
    }

    /** runs callbacks without checking do_callbacks, good for sceKernelCheckCallback.
     * @return true if we switched into a callback. */
    private boolean checkThreadCallbacks(SceKernelThreadInfo thread) {
        boolean handled = false;

        if (thread.umdCallbackRegistered && thread.umdCallbackReady) {
            Modules.log.debug("Entering UMD callback name:'" + thread.umdCallbackInfo.name + "'"
                + " PC:" + Integer.toHexString(thread.umdCallbackInfo.callback_addr)
                + " thread:'" + thread.name + "'");
            //Emulator.PauseEmu();

            // Callbacks can pre-empt, save the current thread's context
            current_thread.saveContext();

            insideCallback = true;
            thread.umdCallbackReady = false;
            // Set the callback to run with the thread context it was registered from
            thread.umdCallbackInfo.startContext(thread.cpuContext, thread);
            thread.umdCallbackInfo.notifyCount++;
            handled = true;
        }

        // TODO add other callback types here in else-if blocks

        return handled;
    }

    /**
     * Iterates waiting threads, making sure do_callbacks is set before
     * checking for pending callbacks.
     * Handles sceKernelCheckCallback when do_callbacks is set on current_thread.
     * Handles redirects to yieldCB (from fake waitCB) on the thread that called waitCB.
     *
     * We currently call checkCallbacks() at the end of each waitCB function
     * since this has less overhead than checking on every step.
     *
     * Some trickery is used in yieldCurrentThreadCB(). By the time we get
     * inside the checkCallbacks() function the thread that called yieldCB is
     * no longer the current thread. Also the thread that called yieldCB is
     * not in the wait state (it's in the ready state). so what we do is check
     * every thread, not just the waiting threads for the do_callbacks flag.
     * Also the waitingThreads list only contains waiting threads that have a
     * finite wait period, so we have to iterate on all threads anyway.
     *
     * It is probably unsafe to call contextSwitch() when insideCallback is true.
     * insideCallback may become true after a call to checkCallbacks().
     */
    public void checkCallbacks() {
        insideCallback = false;

        //Modules.log.debug("checkCallbacks current thread is '" + current_thread.name + "' do_callbacks:" + current_thread.do_callbacks);

        for (Iterator<SceKernelThreadInfo> it = threadMap.values().iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();
            // To work with our fake wait CB's (yieldCB()) we also need to check non-waiting threads.
            // because of this we are able to merge the separate check on current_thread into this loop (handle sceKernelCheckCallback).
            // Now we really need to make sure do_callbacks is consistent!
            //if (thread.status == PSP_THREAD_WAITING) {
                //Modules.log.debug("checkCallbacks: candidate thread:'" + thread.name
                //    + "' state:0x" + Integer.toHexString(thread.status)
                //    + " do_callbacks:" + thread.do_callbacks);

                if (thread.do_callbacks && checkThreadCallbacks(thread)) {
                    // callback was started
                    break;
                }
            //}
        }

        if (!insideCallback) {
            //Modules.log.debug("checkCallbacks: no suitable thread/callback combinations remaining");

            // try and keep do_callbacks in a consistent state
            // usually when we come from sceKernelCheckCallback or yieldCurrentThreadCB
            for (Iterator<SceKernelThreadInfo> it = threadMap.values().iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();
                if (thread.status != PSP_THREAD_WAITING && thread.do_callbacks) {
                    //Modules.log.debug("checkCallbacks: removing do_callbacks from non-waiting thread:'" + thread.name + "'");

                    thread.do_callbacks = false;
                }
            }
        }
    }

    /** @return the top address or 0 on failure. */
    public int mallocStack(int size) {
        if (size > 0) {
            //int p = 0x09f00000 - stackAllocated;
            //stackAllocated += size;
            //return p;

            //int p = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_HighAligned, size, 0x1000);
            int p = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_High, size, 0);
            if (p != 0) {
                pspSysMem.getInstance().addSysMemInfo(2, "ThreadMan-Stack", pspSysMem.PSP_SMEM_High, size, p);
                p += size;
            }

            return p;
        } else {
            return 0;
        }
    }

    // ------------------------- Semaphore -------------------------

    public void ThreadMan_sceKernelCreateSema(int name_addr, int attr, int initVal, int maxVal, int option)
    {
        String name = readStringZ(name_addr);
        Modules.log.debug("sceKernelCreateSema name= " + name + " attr= 0x" + Integer.toHexString(attr) + " initVal= " + initVal + " maxVal= "+ maxVal + " option= 0x" + Integer.toHexString(option));

        if (attr != 0) Modules.log.warn("UNIMPLEMENTED:sceKernelCreateSema attr value 0x" + Integer.toHexString(attr));

        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(option)) {
            int optsize = mem.read32(option);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateSema option at 0x" + Integer.toHexString(option)
                + " (size=" + optsize + ")");
        }

        SceKernelSemaphoreInfo sema = new SceKernelSemaphoreInfo(name, attr, initVal, maxVal);

        Emulator.getProcessor().cpu.gpr[2] = sema.uid;
    }

    public void ThreadMan_sceKernelDeleteSema(int semaid)
    {
        Modules.log.debug("sceKernelDeleteSema id=0x" + Integer.toHexString(semaid));

        if (semaid <= 0) {
            Modules.log.warn("sceKernelDeleteSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.remove(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelDeleteSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            if (sema.numWaitThreads > 0) {
                Modules.log.warn("sceKernelDeleteSema numWaitThreads " + sema.numWaitThreads);

                // Find threads waiting on this sema and wake them up
                for (Iterator<SceKernelThreadInfo> it = threadMap.values().iterator(); it.hasNext(); ) {
                    SceKernelThreadInfo thread = it.next();

                    if (thread.wait.waitingOnSemaphore &&
                        thread.wait.Semaphore_id == semaid) {
                        // Untrack
                        thread.wait.waitingOnSemaphore = false;

                        // Return WAIT_DELETE
                        thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;

                        // Wakeup
                        changeThreadState(thread, PSP_THREAD_READY);
                    }
                }
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** May modify sema.currentCount
     * @return true on success */
    private boolean tryWaitSemaphore(SceKernelSemaphoreInfo sema, int signal)
    {
        boolean success = false;

        if (sema.currentCount >= signal) {
            sema.currentCount -= signal;
            success = true;
        }

        return success;
    }

    private void ThreadMan_hleKernelWaitSema(int semaid, int signal, int timeout_addr, boolean do_callbacks)
    {
        Modules.log.debug("hleKernelWaitSema id= 0x" + Integer.toHexString(semaid)
            + " signal= " + signal
            + " timeout= 0x" + Integer.toHexString(timeout_addr)
            + " callbacks= " + do_callbacks);

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
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("hleKernelWaitSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
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

                // Go to wait state
                current_thread.do_callbacks = do_callbacks;

                // Wait on a specific semaphore
                hleKernelThreadWait(current_thread.wait, micros, (timeout_addr == 0));

                current_thread.wait.waitingOnSemaphore = true;
                current_thread.wait.Semaphore_id = semaid;
                current_thread.wait.Semaphore_signal = signal;

                changeThreadState(current_thread, PSP_THREAD_WAITING);

                contextSwitch(nextThread());
            }
            else
            {
                // Success
                Modules.log.debug("hleKernelWaitSema - '" + sema.name + "' fast check succeeded");
                Emulator.getProcessor().cpu.gpr[2] = 0;
                // TODO yield anyway?
                //contextSwitch(nextThread());
            }
        }
    }

    public void ThreadMan_sceKernelWaitSema(int semaid, int signal, int timeout_addr)
    {
        Modules.log.debug("sceKernelWaitSema redirecting to hleKernelWaitSema(callbacks=false)");
        ThreadMan_hleKernelWaitSema(semaid, signal, timeout_addr, false);
    }

    public void ThreadMan_sceKernelWaitSemaCB(int semaid, int signal, int timeout_addr)
    {
        Modules.log.debug("sceKernelWaitSemaCB redirecting to hleKernelWaitSema(callbacks=true)");
        ThreadMan_hleKernelWaitSema(semaid, signal, timeout_addr, true);
        checkCallbacks();
    }

    public void ThreadMan_sceKernelSignalSema(int semaid, int signal)
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
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelSignalSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else if (sema.currentCount + signal > sema.maxCount) {
            Modules.log.warn("sceKernelSignalSema - overflow uid 0x" + Integer.toHexString(semaid) + " signal=" + signal + " current=" + sema.currentCount + " max=" + sema.maxCount);
            Emulator.getProcessor().cpu.gpr[2] = ERROR_SEMA_OVERFLOW;
            // TODO clamp and continue anyway?
        } else {
            Modules.log.debug("sceKernelSignalSema id=0x" + Integer.toHexString(semaid) + " name='" + sema.name + "' signal=" + signal);

            sema.currentCount += signal;

            // For each thread (sorted by priority),
            // if the thread is waiting on this semaphore,
            // and signal <= currentCount,
            // then wake up the thread and adjust currentCount.
            // repeat for all remaining threads or until currentCount = 0.
            Collection<SceKernelThreadInfo> c = threadMap.values();
            List<SceKernelThreadInfo> list = new LinkedList<SceKernelThreadInfo>(c);
            Collections.sort(list, idle0); // We need an instance of SceKernelThreadInfo for the comparator, so we use idle0
            Iterator<SceKernelThreadInfo> it = list.iterator();
            while(it.hasNext()) {
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
                    changeThreadState(thread, PSP_THREAD_READY);

                    // Adjust sema
                    sema.currentCount -= thread.wait.Semaphore_signal;
                    sema.numWaitThreads--;
                    if (sema.currentCount == 0)
                        break;
                }
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** This is attempt to signal the sema and always return immediately */
    public void ThreadMan_sceKernelPollSema(int semaid, int signal)
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
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
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

    public void ThreadMan_sceKernelCancelSema(int semaid)
    {
        Modules.log.debug("sceKernelCancelSema id= 0x" + Integer.toHexString(semaid));

        if (semaid <= 0) {
            Modules.log.warn("sceKernelCancelSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelCancelSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            sema.numWaitThreads = 0;

            // Find threads waiting on this sema and wake them up
            for (Iterator<SceKernelThreadInfo> it = threadMap.values().iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnSemaphore &&
                    thread.wait.Semaphore_id == semaid) {
                    // Untrack
                    thread.wait.waitingOnSemaphore = false;

                    // Return WAIT_CANCELLED
                    thread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;

                    // Wakeup
                    changeThreadState(thread, PSP_THREAD_READY);
                }
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelReferSemaStatus(int semaid, int addr)
    {
        Modules.log.debug("sceKernelReferSemaStatus id= 0x" + Integer.toHexString(semaid) + " addr= 0x" + Integer.toHexString(addr));

        if (semaid <= 0) {
            Modules.log.warn("sceKernelReferSemaStatus bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelReferSemaStatus - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            sema.write(Memory.getInstance(), addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private class SceKernelSemaphoreInfo
    {
        private final String name;
        private final int attr;
        private final int initCount;
        private int currentCount;
        private final int maxCount;
        private int numWaitThreads;

        private final int uid;

        public SceKernelSemaphoreInfo(String name, int attr, int initCount, int maxCount)
        {
            this.name = name;
            this.attr = attr;
            this.initCount = initCount;
            this.currentCount = initCount;
            this.maxCount = maxCount;
            this.numWaitThreads = 0;

            uid = SceUidManager.getNewUid("ThreadMan-sema");
            semalist.put(uid, this);
        }

        public void write(Memory mem, int address)
        {
            mem.write32(address, 56); // size

            int i, len = name.length();
            for (i = 0; i < 32 && i < len; i++)
                mem.write8(address + 4 + i, (byte)name.charAt(i));
            for (; i < 32; i++)
                mem.write8(address + 4 + i, (byte)0);

            mem.write32(address + 36, attr);
            mem.write32(address + 40, initCount);
            mem.write32(address + 44, currentCount);
            mem.write32(address + 48, maxCount);
            mem.write32(address + 48, numWaitThreads);
        }
    }

    public class Statistics {
        private ArrayList<ThreadStatistics> threads = new ArrayList<ThreadStatistics>();
        public long allCycles = 0;
        public long startTimeMillis;
        public long endTimeMillis;

        public Statistics() {
            startTimeMillis = System.currentTimeMillis();
        }

        public void exit() {
            endTimeMillis = System.currentTimeMillis();
        }

        public long getDurationMillis() {
            return endTimeMillis - startTimeMillis;
        }

        private void addThreadStatistics(SceKernelThreadInfo thread) {
            ThreadStatistics threadStatistics = new ThreadStatistics();
            threadStatistics.name = thread.name;
            threadStatistics.runClocks = thread.runClocks;
            threads.add(threadStatistics);

            allCycles += thread.runClocks;
        }

        private class ThreadStatistics {
            public String name;
            public long runClocks;
        }
    }
}
