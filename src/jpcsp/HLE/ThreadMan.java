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

import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.*;
import jpcsp.HLE.kernel.managers.*;
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
    private static final int WDT_THREAD_HOG_CYCLES = 50000000;

    // TODO probably need to use the SceKernelCallbackInfo struct threadid as a filter somewhere?
    // IMPORTANT: do not call saveContext while processingCallbacks is true,
    // otherwise you will clobber the context with a callback context.
    private boolean processingCallbacks;
    private PendingCallback currentCallback; // this is actually a list of 1 type of callback
    private LinkedList<PendingCallback> pendingCallbackList;
    private HashMap<Integer, SceKernelCallbackInfo> callbackMap;

    private static final boolean USE_THREAD_BANLIST = false;

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

        processingCallbacks = false;
        currentCallback = null;
        pendingCallbackList = new LinkedList<PendingCallback>();
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
        int alignlen = (len + 1 + 15) & ~15; // string terminator + 16 byte align
        Memory mem = Memory.getInstance();
        for (int i = 0; i < len; i++)
            mem.write8((current_thread.stack_addr - alignlen) + i, (byte)pspfilename.charAt(i));
        for (int i = len; i < alignlen; i++)
            mem.write8((current_thread.stack_addr - alignlen) + i, (byte)0);
        current_thread.cpuContext.gpr[29] -= alignlen; // Adjust sp for size of args
        current_thread.cpuContext.gpr[4] = len + 1; // a0 = len + string terminator
        current_thread.cpuContext.gpr[5] = current_thread.cpuContext.gpr[29]; // a1 = pointer to arg data in stack

        // HACK min stack size is set to 512, let's set sp to stack top - 512.
        // this allows plenty of padding between the sp and the eboot path
        // that we store at the top of the stack area. should help NesterJ and
        // Calender, both use sp+16 at the beginning (expected sp-16).
        current_thread.cpuContext.gpr[29] -= 512 - alignlen;

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
        int instruction_syscall = // syscall <code>
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
                Modules.log.info("    Thread " + threadStatistics.name + ": " + threadStatistics.runClocks + " (" + String.format("%2.2f%%", (threadStatistics.runClocks / (double) statistics.allCycles) * 100) + ")");
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
                if (processingCallbacks) {
                    // Callback has exited
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
            if (current_thread == idle0 || current_thread == idle1) {
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

        // Access waitingThreads using array indexing because
        // this is more efficient than iterator access for short lists
        for (int i = 0; i < waitingThreads.size(); i++) {
            SceKernelThreadInfo thread = waitingThreads.get(i);
            thread.wait.steps++;
            if (!thread.wait.forever && thread.wait.steps >= thread.wait.timeout) {
                onWaitTimeout(thread);
                changeThreadState(thread, PSP_THREAD_READY);
            }
        }

        // Cleanup stopped threads (deferred deletion)
        for (int i = 0; i < toBeDeletedThreads.size(); i++) {
            SceKernelThreadInfo thread = toBeDeletedThreads.get(i);
            if (thread.do_delete) {
                // cleanup thread - free the stack
                if (thread.stack_addr != 0) {
                    pspSysMem.getInstance().free(thread.stack_addr);
                }
                deleteThread(thread);
            }
        }

        // Start processing callbacks
        if (!processingCallbacks &&
            pendingCallbackList.size() != 0 &&
            canProcessCallbacks()) {

            Modules.log.debug("Processing " + pendingCallbackList.size() + " pending callback types ...");

            // save current thread context
            // this call probably isn't necessary due to the way thread contexts are handled
            current_thread.saveContext();

            // start the process
            checkCallbacks();
        }
    }

    /** Part of watch dog timer */
    public void clearSyscallFreeCycles() {
        syscallFreeCycles = 0;
    }

    /** @param newthread The thread to switch in. */
    public void contextSwitch(SceKernelThreadInfo newthread) {

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

            //Modules.log.debug("ThreadMan: switched to thread SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
            //Modules.log.debug("---------------------------------------- SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
            /*
            Modules.log.debug("restoreContext SceUID=" + Integer.toHexString(newthread.uid)
                + " name:" + newthread.name
                + " PC:" + Integer.toHexString(newthread.cpuContext.pc)
                + " NPC:" + Integer.toHexString(newthread.cpuContext.npc));
            */

            //Emulator.PauseEmu();
        } else {
            // Shouldn't get here now we are using idle threads
            Modules.log.info("No ready threads - pausing emulator");
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
        }

        current_thread = newthread;
        syscallFreeCycles = 0;
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

    private boolean canProcessCallbacks() {
        // iterate waiting list and check do_callbacks flag
        for (Iterator<SceKernelThreadInfo> it = waitingThreads.iterator();
            it.hasNext(); ) {
            SceKernelThreadInfo info = it.next();
            if (info.do_callbacks) {
                Modules.log.debug("canProcessCallbacks = true, thread '" + info.name + "' is waiting on CB");
                return true;
            }
        }
        return false;
    }

    private void switchInCallback(SceKernelCallbackInfo info, int[] gpr) {

        Modules.log.debug("switchInCallback PC:" + Integer.toHexString(info.callback_addr)
            + " name:'" + info.name
            + "' thread:" + Integer.toHexString(info.threadId));

        // create a new context for the callback to run in
        CpuState callbackContext = new CpuState(current_thread.cpuContext);

        // mark $ra so we know when the callback exits
        callbackContext.gpr[31] = 0;

        // where the callback starts executing
        callbackContext.pc = info.callback_addr;

        // callback user data
        callbackContext.gpr[4] = info.callback_arg_addr;

        // callback event data
        for (int i = 5; i < 4+8; i++) {
            callbackContext.gpr[i] = gpr[i];
        }

        info.notifyCount++;
        Emulator.getProcessor().cpu = callbackContext;
    }

    private void checkCallbacks() {
        processingCallbacks = false;
        for (Iterator<PendingCallback> it = pendingCallbackList.iterator();
            it.hasNext(); ) {
            currentCallback = it.next();

            // TODO make sure checkCallbacks is called when current_thread is a waiting thread that has do_callbacks set
            // or, make currentCallback.next() only return callbacks that match any waiting thread with do_callbacks set
            SceKernelCallbackInfo info = currentCallback.next(current_thread.uid);
            if (info != null) {
                processingCallbacks = true;
                switchInCallback(info, currentCallback.gpr);
                break;
            } else {
                Modules.log.debug("Finished processing a callback type");
                it.remove();
            }
        }

        if (!processingCallbacks) {
            Modules.log.debug("Finished processing all ready callbacks");
            //Emulator.PauseEmu();

            // resume normal operation
            current_thread.restoreContext();
        }
    }


    public int getCurrentThreadID() {
        return current_thread.uid;
    }

    public SceKernelThreadInfo getCurrentThread() {
        return current_thread;
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
        contextSwitch(nextThread());
    }

    public void blockCurrentThread()
    {
        changeThreadState(current_thread, PSP_THREAD_SUSPEND);
        contextSwitch(nextThread());
    }

    public void unblockThread(int uid)
    {
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadMap.get(uid);
            changeThreadState(thread, PSP_THREAD_READY);
        }
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
                thread.cpuContext.gpr[2] = ERROR_ERROR_WAIT_DELETE;
            }
        }
    }

    private void deleteThread(SceKernelThreadInfo thread) {
        waitingThreads.remove(thread);
        toBeDeletedThreads.remove(thread);
        threadMap.remove(thread.uid);
        SceUidManager.releaseUid(thread.uid, "ThreadMan-thread");
        // TODO remove from any internal lists? such as sema waiting lists

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


    public int createThread(String name, int entry_addr, int initPriority, int stackSize, int attr, int option_addr, boolean startImmediately, int userDataLength, int userDataAddr, int gp) {
        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);
        threadMap.put(thread.uid, thread);

        // Copy user data to the new thread's stack, since we are not
        // starting the thread immediately, only marking it as ready,
        // the data needs to be saved somewhere safe.
        int alignlen = (userDataLength + 15) & ~15; // 16 byte align
        Memory mem = Memory.getInstance();
        for (int i = 0; i < userDataLength; i++)
            mem.write8((thread.stack_addr - alignlen) + i, (byte)mem.read8(userDataAddr + i));
        for (int i = userDataLength; i < alignlen; i++)
            mem.write8((thread.stack_addr - alignlen) + i, (byte)0);
        thread.cpuContext.gpr[29] -= alignlen; // Adjust sp for size of user data
        thread.cpuContext.gpr[4] = userDataLength; // a0 = userDataLength
        thread.cpuContext.gpr[5] = thread.cpuContext.gpr[29]; // a1 = pointer to copy of data at data_addr
        thread.cpuContext.gpr[28] = gp;

        if (startImmediately) {
            changeThreadState(thread, PSP_THREAD_READY);
            contextSwitch(thread);
        }

        return thread.uid;
    }

    public void ThreadMan_sceKernelCreateThread(int name_addr, int entry_addr,
        int initPriority, int stackSize, int attr, int option_addr) {
        String name = readStringZ(Memory.getInstance().mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        // TODO use option_addr/SceKernelThreadOptParam?
        if (option_addr != 0)
            Modules.log.warn("sceKernelCreateThread unhandled SceKernelThreadOptParam");

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);
        threadMap.put(thread.uid, thread);

        Modules.log.debug("sceKernelCreateThread SceUID=" + Integer.toHexString(thread.uid)
            + " name:'" + thread.name + "' PC=" + Integer.toHexString(thread.cpuContext.pc)
            + " attr:" + Integer.toHexString(attr));

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

    /** use lower case in this list */
    private final String[] threadNameBanList = new String[] {
        "bgm thread", "sgx-psp-freq-thr"
    };
    /* suspected sound thread names:
     * SndMain, SoundThread, At3Main, Atrac3PlayThread,
     * bgm thread, SceWaveMain, SasCore thread, soundThread,
     * ATRAC3 play thread, SAS Thread, XomAudio, sgx-psp-freq-thr,
     * sgx-psp-at3-th, sgx-psp-pcm-th, snd_tick_timer_thread, snd_stream_service_thread_0,
     * snd_stream_service_thread_1, SAS / Main Audio
     *
     * keywords:
     * snd, sound, at3, atrac3, sas, wave, pcm, audio
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
                name.contains("audio") || name.contains("pcm")) {
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
            Modules.log.debug("sceKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Copy user data to the new thread's stack, since we are not
            // starting the thread immediately, only marking it as ready,
            // the data needs to be saved somewhere safe.
            int alignlen = (len + 15) & ~15; // 16 byte align
            Memory mem = Memory.getInstance();
            for (int i = 0; i < len; i++)
                mem.write8((thread.stack_addr - alignlen) + i, (byte)mem.read8(data_addr + i));
            for (int i = len; i < alignlen; i++)
                mem.write8((thread.stack_addr - alignlen) + i, (byte)0);
            thread.cpuContext.gpr[29] -= alignlen; // Adjust sp for size of user data
            thread.cpuContext.gpr[4] = len; // a0 = len
            thread.cpuContext.gpr[5] = thread.cpuContext.gpr[29]; // a1 = pointer to copy of data at data_addr
            changeThreadState(thread, PSP_THREAD_READY);

            Emulator.getProcessor().cpu.gpr[2] = 0;

            // TODO does start thread defer start or really start?
            // threadstatus.pbp on real PSP shows the callback thread prints
            // before the main thread, so it starts immediately.
            contextSwitch(thread);
        }
    }

    /** exit the current thread */
    public void ThreadMan_sceKernelExitThread(int exitStatus) {
        Modules.log.debug("sceKernelExitThread SceUID=" + Integer.toHexString(current_thread.uid)
            + " name:'" + current_thread.name + "' exitStatus:" + exitStatus);

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

    /** suspend the current thread and handle callbacks */
    public void ThreadMan_sceKernelSleepThreadCB() {
        Modules.log.debug("PARTIAL:sceKernelSleepThreadCB SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        changeThreadState(current_thread, PSP_THREAD_SUSPEND);
        current_thread.do_callbacks = true;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** suspend the current thread */
    public void ThreadMan_sceKernelSleepThread() {
        Modules.log.debug("sceKernelSleepThread SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        changeThreadState(current_thread, PSP_THREAD_SUSPEND);
        current_thread.do_callbacks = false;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
    }

    public static int microsToSteps(int micros) {
        //return micros * 200000000 / 1000000; // TODO steps = micros * steprate
        return (micros < WDT_THREAD_IDLE_CYCLES) ? micros : WDT_THREAD_IDLE_CYCLES - 1; // test version
    }

    private void hleKernelDelayThread(int micros, boolean do_callbacks) {
        // Go to wait state, callbacks
        current_thread.do_callbacks = do_callbacks;

        // Wait on a timeout only
        current_thread.wait.forever = false; // Delay Thread can never be infinite
        current_thread.wait.timeout = microsToSteps(micros);
        current_thread.wait.steps = 0;

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
        hleKernelDelayThread(micros, true);
    }

    public void ThreadMan_sceKernelCreateCallback(int a0, int a1, int a2) {
        String name = readStringZ(Memory.getInstance().mainmemory, (a0 & 0x3fffffff) - MemoryMap.START_RAM);
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, current_thread.uid, a1, a2);

        Modules.log.debug("sceKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid)
            + " PC=" + Integer.toHexString(callback.callback_addr)
            + " name:'" + callback.name
            + "' thread:'" + current_thread.name + "'");

        Emulator.getProcessor().cpu.gpr[2] = callback.uid;
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
        if (uid == 0) uid = getCurrentThreadID();
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

    public void ThreadMan_sceKernelWakeupThread(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (thread.status != PSP_THREAD_SUSPEND) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " not suspended (status=" + thread.status + ")");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_THREAD_IS_NOT_SUSPEND;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not waking up");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Modules.log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
            changeThreadState(thread, PSP_THREAD_READY);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelWaitThreadEnd(int uid, int micros) {
        Modules.log.debug("sceKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " timeout=" + micros);
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelWaitThreadEnd unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            // Go to wait state, no callbacks
            current_thread.do_callbacks = false;

            // Wait on a specific thread end
            current_thread.wait.forever = (micros == 0); // TODO check this
            current_thread.wait.timeout = microsToSteps(micros);
            current_thread.wait.steps = 0;

            current_thread.wait.waitingOnThreadEnd = true;
            current_thread.wait.ThreadEnd_id = uid;

            changeThreadState(current_thread, PSP_THREAD_WAITING);

            contextSwitch(nextThread());
        }
    }

    public void ThreadMan_sceKernelCheckCallback() {
        Modules.log.debug("PARTIAL:sceKernelCheckCallback");
        // TODO process callbacks
        yieldCurrentThread();
    }

    // TODO add other pushXXXCallback functions
    public void pushUMDCallback(Iterator<Integer> cbidList, int event) {
        int[] gpr = new int[32];
        gpr[5] = event;
        PendingCallback pending = new PendingCallback(cbidList, gpr);
        //pendingCallbackList.add(pending);
    }

    private class PendingCallback {
        private LinkedList<Integer> cbidList;
        private int[] gpr;

        /** @param gpr elements 5 to 8 are good for parameters to the callback
         * function. gpr 4 will be automatically set to the callback's user data
         * pointer. */
        public PendingCallback(Iterator<Integer> cbIt, int[] gpr) {
            cbidList = new LinkedList<Integer>();
            while(cbIt.hasNext()) {
                cbidList.add(cbIt.next());
            }

            this.gpr = gpr;
        }

        public SceKernelCallbackInfo next(int uid) {
            SceKernelCallbackInfo found = null;

            for (Iterator<Integer> it = cbidList.iterator(); it.hasNext(); ) {
                int cbid = it.next();
                SceKernelCallbackInfo info = callbackMap.get(cbid);
                if (info.threadId == uid) {
                    found = info;
                    it.remove();
                    break;
                }
            }

            return found;
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

            notifyCount = 0; // probably number of times the callback was called
            notifyArg = 0; // ?

            // internal state
            uid = SceUidManager.getNewUid("ThreadMan-callback");
            callbackMap.put(uid, this);
        }
    }

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
        String name = readStringZ(Memory.getInstance().mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);
        Modules.log.debug("sceKernelCreateSema name= " + name + " attr= 0x" + Integer.toHexString(attr) + " initVal= " + initVal + " maxVal= "+ maxVal + " option= 0x" + Integer.toHexString(option));

        if (attr != 0) Modules.log.warn("UNIMPLEMENTED:sceKernelCreateSema attr value 0x" + Integer.toHexString(attr));
        if (option != 0) Modules.log.warn("UNIMPLEMENTED:sceKernelCreateSema option value at 0x" + Integer.toHexString(option));

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
                        thread.cpuContext.gpr[2] = ERROR_ERROR_WAIT_DELETE;

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
    private boolean waitSemaphore(SceKernelSemaphoreInfo sema, int signal)
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

            if (!waitSemaphore(sema, signal))
            {
                // Failed, but it's ok, just wait a little
                Modules.log.debug("hleKernelWaitSema - fast check failed");
                sema.numWaitThreads++;

                // Go to wait state
                current_thread.do_callbacks = do_callbacks;

                // Wait on a specific semaphore
                current_thread.wait.forever = (timeout_addr == 0);
                current_thread.wait.timeout = microsToSteps(micros);
                current_thread.wait.steps = 0;

                current_thread.wait.waitingOnSemaphore = true;
                current_thread.wait.Semaphore_id = semaid;
                current_thread.wait.Semaphore_signal = signal;

                changeThreadState(current_thread, PSP_THREAD_WAITING);

                contextSwitch(nextThread());
            }
            else
            {
                // Success
                Modules.log.debug("hleKernelWaitSema - fast check succeeded");
                Emulator.getProcessor().cpu.gpr[2] = 0;
                // TODO yield anyway?
                contextSwitch(nextThread());
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
    }

    public void ThreadMan_sceKernelSignalSema(int semaid, int signal)
    {
        Modules.log.debug("sceKernelSignalSema id= 0x" + Integer.toHexString(semaid) + " signal= " + signal);

        if (signal <= 0) {
            Modules.log.warn("sceKernelSignalSema - bad signal " + signal);
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
            Modules.log.warn("sceKernelSignalSema - overflow signal=" + signal + " current=" + sema.currentCount + " max=" + sema.maxCount);
            Emulator.getProcessor().cpu.gpr[2] = ERROR_SEMA_OVERFLOW;
            // TODO clamp and continue anyway?
        } else {
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
        public ArrayList<ThreadStatistics> threads = new ArrayList<ThreadStatistics>();
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
