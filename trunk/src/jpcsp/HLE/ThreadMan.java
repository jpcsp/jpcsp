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

import jpcsp.HLE.kernel.types.*;
import jpcsp.HLE.kernel.managers.*;

public class ThreadMan {
    private static ThreadMan instance;
    private static HashMap<Integer, SceKernelThreadInfo> threadlist;
    private static HashMap<Integer, SceKernelSemaphoreInfo> semalist;
    private static HashMap<Integer, SceKernelEventFlagInfo> eventlist;
    private  ArrayList<Integer> waitingThreads;
    private SceKernelThreadInfo current_thread;
    private SceKernelThreadInfo idle0, idle1;
    private int continuousIdleCycles; // watch dog timer - number of continuous cycles in any idle thread
    private int syscallFreeCycles; // watch dog timer - number of cycles since last syscall

    // TODO figure out a decent number of cycles to wait
    private final int WDT_THREAD_IDLE_CYCLES = 1000000;
    private final int WDT_THREAD_HOG_CYCLES = 50000000;

    public final static int PSP_ERROR_UNKNOWN_UID                    = 0x800200cb;

    public final static int PSP_ERROR_NOT_FOUND_THREAD               = 0x80020198;
    public final static int PSP_ERROR_NOT_FOUND_SEMAPHORE            = 0x80020199;
    public final static int PSP_ERROR_NOT_FOUND_EVENT_FLAG           = 0x8002019a;
    public final static int PSP_ERROR_NOT_FOUND_MESSAGE_BOX          = 0x8002019b;
    public final static int PSP_ERROR_NOT_FOUND_VPOOL                = 0x8002019c;
    public final static int PSP_ERROR_NOT_FOUND_FPOOL                = 0x8002019d;
    public final static int PSP_ERROR_NOT_FOUND_MESSAGE_PIPE         = 0x8002019e;
    public final static int PSP_ERROR_NOT_FOUND_ALARM                = 0x8002019f;
    public final static int PSP_ERROR_NOT_FOUND_THREAD_EVENT_HANDLER = 0x800201a0;
    public final static int PSP_ERROR_NOT_FOUND_CALLBACK             = 0x800201a1;

    public final static int PSP_ERROR_THREAD_ALREADY_DORMANT         = 0x800201a2;
    public final static int PSP_ERROR_THREAD_ALREADY_SUSPEND         = 0x800201a3;
    public final static int PSP_ERROR_THREAD_IS_NOT_DORMANT          = 0x800201a4;
    public final static int PSP_ERROR_THREAD_IS_NOT_SUSPEND          = 0x800201a5;
    public final static int PSP_ERROR_THREAD_IS_NOT_WAIT             = 0x800201a6;

    public final static int PSP_ERROR_WAIT_TIMEOUT                   = 0x800201a8;
    public final static int PSP_ERROR_WAIT_CANCELLED                 = 0x800201a9;

    public final static int PSP_ERROR_SEMA_ZERO                      = 0x800201ad;
    public final static int PSP_ERROR_SEMA_OVERFLOW                  = 0x800201ae;
    public final static int PSP_ERROR_EVENT_FLAG_POLL_FAILED         = 0x800201af;
    public final static int PSP_ERROR_EVENT_FLAG_NO_MULTI_PERM       = 0x800201b0;
    public final static int PSP_ERROR_ERROR_WAIT_DELETE              = 0x800201b5;
    public final static int PSP_ERROR_ILLEGAL_COUNT                  = 0x800201bd;

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

    /** call this when resetting the emulator
     * @param entry_addr entry from ELF header
     * @param attr from sceModuleInfo ELF section header */
    public void Initialise(int entry_addr, int attr, String pspfilename) {
        //Modules.log.debug("ThreadMan: Initialise entry:0x" + Integer.toHexString(entry_addr));

        threadlist = new HashMap<Integer, SceKernelThreadInfo>();
        semalist = new HashMap<Integer, SceKernelSemaphoreInfo>();
        eventlist = new HashMap<Integer, SceKernelEventFlagInfo>();
        waitingThreads= new ArrayList<Integer>();

        // Clear stack allocation info
        //pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_Addr, 0x000fffff, 0x09f00000);
        //stackAllocated = 0;

        install_idle_threads();

        // Create a thread the program will run inside
        current_thread = new SceKernelThreadInfo("root", entry_addr, 0x20, 0x4000, attr);

        // Set user mode bit if kernel mode bit is not present
        if ((current_thread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL) {
            current_thread.attr |= PSP_THREAD_ATTR_USER;
        }

        // Setup args by copying them onto the stack
        //Modules.log.debug("pspfilename - '" + pspfilename + "'");
        int len = pspfilename.length();
        int alignlen = (len + 1 + 3) & ~3; // string terminator + 4 byte align
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

        current_thread.status = PspThreadStatus.PSP_THREAD_READY;

        // Switch in the thread
        current_thread.status = PspThreadStatus.PSP_THREAD_RUNNING;
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
        idle0.status = PspThreadStatus.PSP_THREAD_READY;

        idle1 = new SceKernelThreadInfo("idle1", MemoryMap.START_RAM | 0x80000000, 0x7f, 0x0, PSP_THREAD_ATTR_KERNEL);
        idle1.status = PspThreadStatus.PSP_THREAD_READY;

        continuousIdleCycles = 0;
    }

    /** to be called from the main emulation loop */
    public void step() {
        CpuState cpu = Emulator.getProcessor().cpu;
        if (current_thread != null) {
            current_thread.runClocks++;

            //Modules.log.debug("pc=" + Emulator.getProcessor().pc + " ra=" + Emulator.getProcessor().gpr[31]);

            // Hook jr ra to 0 (thread function returned)
            if (cpu.pc == 0 && cpu.gpr[31] == 0) {
                // Thread has exited
                Modules.log.debug("Thread exit detected SceUID=" + Integer.toHexString(current_thread.uid)
                    + " name:'" + current_thread.name + "' return:0x" + Integer.toHexString(cpu.gpr[2]));
                current_thread.exitStatus = cpu.gpr[2]; // v0
                current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
                onThreadStopped(current_thread);
                contextSwitch(nextThread());
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

        Iterator<SceKernelThreadInfo> it = threadlist.values().iterator();
        while(it.hasNext()) {
            SceKernelThreadInfo thread = it.next();

            // Work waiting threads
            if (thread.status == PspThreadStatus.PSP_THREAD_WAITING) {
                if (false && thread != idle0 && thread != idle1) {
                    Modules.log.debug("working waiting thread '" + thread.name
                        + "' forever = " + thread.wait.forever
                        + " timeout = " + thread.wait.timeout
                        + " steps = " + thread.wait.steps);
                }

                thread.wait.steps++;
                if (!thread.wait.forever &&
                    thread.wait.steps >= thread.wait.timeout) {
                    if (thread.status == PspThreadStatus.PSP_THREAD_WAITING) {
                        //Modules.log.debug("wait timeout");
                        onWaitTimeout(thread);
                        thread.status = PspThreadStatus.PSP_THREAD_READY;
                    }
                }
            }

            // Cleanup stopped threads (deferred deletion)
            if (thread.status == PspThreadStatus.PSP_THREAD_STOPPED) {
                if (thread.do_delete) {
                    // cleanup thread - free the stack
                    if (thread.stack_addr != 0) {
                        pspSysMem.getInstance().free(thread.stack_addr);
                    }
                    // TODO remove from any internal lists? such as sema waiting lists

                    // Changed to thread safe iterator.remove
                    //threadlist.remove(thread.uid);
                    it.remove();

                    SceUidManager.releaseUid(thread.uid, "ThreadMan-thread");
                }
            }
        }
    }

    /** Part of watch dog timer */
    public void clearSyscallFreeCycles() {
        syscallFreeCycles = 0;
    }

    private void contextSwitch(SceKernelThreadInfo newthread) {

        if (current_thread != null) {
            // Switch out old thread
            if (current_thread.status == PspThreadStatus.PSP_THREAD_RUNNING)
                current_thread.status = PspThreadStatus.PSP_THREAD_READY;
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
            newthread.status = PspThreadStatus.PSP_THREAD_RUNNING;
            newthread.wakeupCount++; // check
            // restore registers
            newthread.restoreContext();

            //Modules.log.debug("ThreadMan: switched to thread SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
            //Modules.log.debug("---------------------------------------- SceUID=" + Integer.toHexString(newthread.uid) + " name:'" + newthread.name + "'");
            /*
            Modules.log.debug("restoreContext SceUID=" + Integer.toHexString(newthread.uid)
                + " name:" + newthread.name
                + " PC:" + Integer.toHexString(newthread.pcreg)
                + " NPC:" + Integer.toHexString(newthread.npcreg));
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
            //Modules.log.debug("nextThread pri=" + Integer.toHexString(thread.currentPriority) + " name:" + thread.name + " status:" + thread.status);

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

    public String getThreadName(int uid) {
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            return "NOT A THREAD";
        } else {
            return thread.name;
        }
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
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadlist.get(uid);
            thread.status = PspThreadStatus.PSP_THREAD_READY;
        }
    }

    /** Call this when a thread's wait timeout has expired.
     * You can assume the calling function will set thread.status = ready. */
    public void onWaitTimeout(SceKernelThreadInfo thread) {
        // ThreadEnd
        if (thread.wait.waitingOnThreadEnd) {
            //Modules.log.debug("ThreadEnd timedout");

            // Untrack
            thread.wait.waitingOnThreadEnd = false;

            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = PSP_ERROR_WAIT_TIMEOUT;
        }

        // EventFlag
        else if (thread.wait.waitingOnEventFlag) {
            //Modules.log.debug("EventFlag timedout");

            // Untrack
            thread.wait.waitingOnEventFlag = false;

            // Update numWaitThreads
            SceKernelEventFlagInfo event = eventlist.get(thread.wait.EventFlag_id);
            if (event != null) {
                event.numWaitThreads--;

                // Return WAIT_TIMEOUT
                thread.cpuContext.gpr[2] = PSP_ERROR_WAIT_TIMEOUT;
            } else {
                Modules.log.warn("EventFlag deleted while we were waiting for it! (timeout expired)");

                // Return WAIT_DELETE
                thread.cpuContext.gpr[2] = PSP_ERROR_ERROR_WAIT_DELETE;
            }
        }
    }

    private void onThreadStopped(SceKernelThreadInfo stoppedThread) {
        for (Iterator<SceKernelThreadInfo> it = threadlist.values().iterator(); it.hasNext(); ) {
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
                thread.status = PspThreadStatus.PSP_THREAD_READY;
            }
        }
    }


    public int createThread(String name, int entry_addr, int initPriority, int stackSize, int attr, int option_addr, boolean startImmediately, int userDataLength, int userDataAddr, int gp) {
        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);

        // Copy user data to the new thread's stack, since we are not
        // starting the thread immediately, only marking it as ready,
        // the data needs to be saved somewhere safe.
        int alignlen = (userDataLength + 3) & ~3; // 4 byte align
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
            thread.status = PspThreadStatus.PSP_THREAD_READY;
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
    public void ThreadMan_sceKernelTerminateThread(int a0) {
        SceUidManager.checkUidPurpose(a0, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(a0);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            thread.status = PspThreadStatus.PSP_THREAD_STOPPED; // PSP_THREAD_STOPPED or PSP_THREAD_KILLED ?

            Emulator.getProcessor().cpu.gpr[2] = 0;
            onThreadStopped(thread);
        }
    }

    /** delete thread a0 */
    public void ThreadMan_sceKernelDeleteThread(int a0) {
        SceUidManager.checkUidPurpose(a0, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(a0);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Mark thread for deletion
            thread.do_delete = true;

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelStartThread(int uid, int len, int data_addr) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Copy user data to the new thread's stack, since we are not
            // starting the thread immediately, only marking it as ready,
            // the data needs to be saved somewhere safe.
            int alignlen = (len + 3) & ~3; // 4 byte align
            Memory mem = Memory.getInstance();
            for (int i = 0; i < len; i++)
                mem.write8((thread.stack_addr - alignlen) + i, (byte)mem.read8(data_addr + i));
            for (int i = len; i < alignlen; i++)
                mem.write8((thread.stack_addr - alignlen) + i, (byte)0);
            thread.cpuContext.gpr[29] -= alignlen; // Adjust sp for size of user data
            thread.cpuContext.gpr[4] = len; // a0 = len
            thread.cpuContext.gpr[5] = thread.cpuContext.gpr[29]; // a1 = pointer to copy of data at data_addr
            thread.status = PspThreadStatus.PSP_THREAD_READY;

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

        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
        current_thread.exitStatus = exitStatus;
        Emulator.getProcessor().cpu.gpr[2] = 0;
        onThreadStopped(current_thread);

        contextSwitch(nextThread());
    }

    /** exit the current thread, then delete it */
    public void ThreadMan_sceKernelExitDeleteThread(int exitStatus) {
        SceKernelThreadInfo thread = current_thread; // save a reference for post context switch operations
        Modules.log.debug("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(current_thread.uid)
            + " name:'" + current_thread.name + "' exitStatus:" + exitStatus);

        // Exit
        current_thread.status = PspThreadStatus.PSP_THREAD_STOPPED;
        current_thread.exitStatus = exitStatus;
        Emulator.getProcessor().cpu.gpr[2] = 0;
        onThreadStopped(current_thread); // TODO maybe not here in Exit and Delete thread function

        // Mark thread for deletion
        thread.do_delete = true;

        contextSwitch(nextThread());
    }

    /** suspend the current thread and handle callbacks */
    public void ThreadMan_sceKernelSleepThreadCB() {
        Modules.log.debug("PARTIAL:sceKernelSleepThreadCB SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        current_thread.do_callbacks = true;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** suspend the current thread */
    public void ThreadMan_sceKernelSleepThread() {
        Modules.log.debug("sceKernelSleepThread SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        current_thread.do_callbacks = false;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
    }

    private int microsToSteps(int micros) {
        //return micros * 200000000 / 1000000; // TODO steps = micros * steprate
        return (micros < WDT_THREAD_IDLE_CYCLES) ? micros : WDT_THREAD_IDLE_CYCLES - 1; // test version
    }

    private void hleKernelDelayThread(int micros, boolean do_callbacks) {
        // Go to wait state, callbacks
        current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
        current_thread.do_callbacks = do_callbacks;

        // Wait on a timeout only
        current_thread.wait.forever = false; // Delay Thread can never be infinite
        current_thread.wait.timeout = microsToSteps(micros);
        current_thread.wait.steps = 0;

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
            + " PC=" + Integer.toHexString(callback.callback_addr) + " name:'" + callback.name + "'");

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
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelGetThreadExitStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
        } else  {
            Modules.log.debug("sceKernelGetThreadExitStatus uid=0x" + Integer.toHexString(uid) + " exitStatus=0x" + Integer.toHexString(thread.exitStatus));
            Emulator.getProcessor().cpu.gpr[2] = thread.exitStatus;
        }
    }

    public void ThreadMan_sceKernelReferThreadStatus(int uid, int addr) {
        //Get the status information for the specified thread
        if (uid == 0) uid = current_thread.uid;
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelReferThreadStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
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
                for (Iterator<SceKernelThreadInfo> it = threadlist.values().iterator(); it.hasNext() /*&& count < readbufsize*/; ) {
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
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid)
                    + " newPriority:0x" + Integer.toHexString(priority) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
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
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
        } else if (thread.status != PspThreadStatus.PSP_THREAD_SUSPEND) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " not suspended (status=" + thread.status + ")");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_THREAD_IS_NOT_SUSPEND;
        } else {
            Modules.log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
            thread.status = PspThreadStatus.PSP_THREAD_READY;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelWaitThreadEnd(int uid, int micros) {
        Modules.log.debug("sceKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " timeout=" + micros);
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelWaitThreadEnd unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
        } else {
            // Go to wait state, no callbacks
            current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
            current_thread.do_callbacks = false;

            // Wait on a specific thread end
            current_thread.wait.forever = (micros == 0); // TODO check this
            current_thread.wait.timeout = microsToSteps(micros);
            current_thread.wait.steps = 0;

            current_thread.wait.waitingOnThreadEnd = true;
            current_thread.wait.ThreadEnd_id = uid;

            contextSwitch(nextThread());
        }
    }

    /** SceKernelSysClock time_addr http://psp.jim.sh/pspsdk-doc/structSceKernelSysClock.html
     * +1mil every second
     * high 32-bits never set on real psp? */
    public void ThreadMan_sceKernelGetSystemTime(int time_addr) {
        Modules.log.debug("sceKernelGetSystemTime 0x" + Integer.toHexString(time_addr));
        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(time_addr)) {
            long systemTime = System.nanoTime();
            int low = (int)(systemTime & 0xffffffffL);
            int hi = (int)((systemTime >> 32) & 0xffffffffL);

            mem.write32(time_addr, low);
            mem.write32(time_addr + 4, hi);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    public void ThreadMan_sceKernelGetSystemTimeWide() {
        Modules.log.debug("sceKernelGetSystemTimeWide");
        long systemTime = System.nanoTime();
        Emulator.getProcessor().cpu.gpr[2] = (int)(systemTime & 0xffffffffL);
        Emulator.getProcessor().cpu.gpr[3] = (int)((systemTime >> 32) & 0xffffffffL);
    }

    //private int timeLow = 0;
    public void ThreadMan_sceKernelGetSystemTimeLow() {
        long systemTime = System.nanoTime();
        int low = (int)(systemTime & 0x7fffffffL); // check, don't use msb?
        //int low = timeLow; timeLow += 10;
        //Modules.log.debug("sceKernelGetSystemTimeLow return:" + low);
        Emulator.getProcessor().cpu.gpr[2] = low;
    }

    public void ThreadMan_sceKernelCheckCallback() {
        Modules.log.debug("PARTIAL:sceKernelCheckCallback");
        // TODO process callbacks
        yieldCurrentThread();
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
            uid = SceUidManager.getNewUid("ThreadMan-callback");

            // TODO add to list of callbacks
        }
    }

    // TODO change back to int and respect bitfield properties (wait-suspend)
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

    private void memset(int address, byte c, int length) {
        Memory mem = Memory.getInstance();
        byte[] all = mem.mainmemory.array();
        int offset = address - MemoryMap.START_RAM + mem.mainmemory.arrayOffset();
        Modules.log.debug("memset 0x" + Integer.toHexString(address)
            + " (offset:0x" + Integer.toHexString(offset) + ") len:0x" + Integer.toHexString(length));
        Arrays.fill(all, offset, offset + length, c);
    }

    private class ThreadWaitInfo {
        boolean forever;
        int timeout; // 0 is allowed and is NOT forever
        int steps; // +1 for each emu step, when it reaches timeout the wait has expired

        // TODO change waitingOnThreadEnd, waitingOnEventFlag, etc to waitType,
        // since we can only wait on one type of event at a time.

        // Thread End
        boolean waitingOnThreadEnd;
        int ThreadEnd_id;

        // Event Flag
        boolean waitingOnEventFlag;
        int EventFlag_id;
        int EventFlag_bits;
        int EventFlag_wait;
        int EventFlag_outBits_addr;

        // Semaphore
        boolean waitingOnSemaphore;
        int Semaphore_id;
        int Semaphore_signal;
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
        private CpuState cpuContext;
        private boolean do_delete;
        private boolean do_callbacks; // in this implementation, only valid for PSP_THREAD_WAITING and PSP_THREAD_SUSPEND

        private ThreadWaitInfo wait;

        public SceKernelThreadInfo(String name, int entry_addr, int initPriority, int stackSize, int attr) {
            // Ignore 0 size from the idle threads (don't want them stealing space)
            if (stackSize != 0) {
                if (stackSize < 512) {
                    // 512 byte min
                    stackSize = 512;
                } else {
                    // 256 byte size alignment
                    stackSize = (stackSize + 0xFF) & ~0xFF;
                }
            }

            this.name = name;
            this.entry_addr = entry_addr;
            this.initPriority = initPriority;
            this.stackSize = stackSize;
            this.attr = attr;

            status = PspThreadStatus.PSP_THREAD_SUSPEND;
            stack_addr = mallocStack(stackSize);
            if (stack_addr != 0 &&
                stackSize > 0 &&
                (attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK) {
                memset(stack_addr - stackSize, (byte)0xFF, stackSize);
            }
            gpReg_addr = Emulator.getProcessor().cpu.gpr[28]; // inherit gpReg // TODO addr into ModuleInfo struct?
            currentPriority = initPriority;
            waitType = 0; // ?
            waitId = 0; // ?
            wakeupCount = 0;
            exitStatus = PSP_ERROR_THREAD_IS_NOT_DORMANT;
            runClocks = 0;
            intrPreemptCount = 0;
            threadPreemptCount = 0;
            releaseCount = 0;

            // internal state
            uid = SceUidManager.getNewUid("ThreadMan-thread");
            threadlist.put(uid, this);

            // Inherit context
            //cpuContext = new CpuState();
            //saveContext();
            cpuContext = new CpuState(Emulator.getProcessor().cpu);

            // Thread specific registers
            cpuContext.pc = entry_addr;
            cpuContext.npc = entry_addr; // + 4;
            cpuContext.gpr[29] = stack_addr; //sp
            cpuContext.gpr[26] = cpuContext.gpr[29]; // k0 mirrors sp?

            // We'll hook "jr ra" where ra = 0 as the thread exiting
            cpuContext.gpr[31] = 0; // ra

            do_delete = false;
            do_callbacks = false;
            wait = new ThreadWaitInfo();
        }

        public void saveContext() {
            cpuContext = Emulator.getProcessor().cpu;
            //cpuContext.copy(Emulator.getProcessor().cpu);

            // ignore PSP_THREAD_ATTR_VFPU flag
        }

        public void restoreContext() {
            // Assuming context switching only happens on syscall,
            // we always execute npc after a syscall,
            // so we can set pc = npc regardless of cop0.status.bd.
            //if (!cpu.cop0_status_bd)
                cpuContext.pc = cpuContext.npc;

            Emulator.getProcessor().cpu = cpuContext;
            //Emulator.getProcessor().cpu.copy(cpuContext);

            // ignore PSP_THREAD_ATTR_VFPU flag
        }

        /** For use in the scheduler */
        @Override
        public int compare(SceKernelThreadInfo o1, SceKernelThreadInfo o2) {
            return o1.currentPriority - o2.currentPriority;
        }

        public void write(Memory mem, int address) {
            mem.write32(address, 106); // size

            int i, len = name.length();
            for (i = 0; i < 32 && i < len; i++)
                mem.write8(address + 4 + i, (byte)name.charAt(i));
            for (; i < 32; i++)
                mem.write8(address + 4 + i, (byte)0);

            mem.write32(address + 36, attr);
            mem.write32(address + 40, status.getValue());
            mem.write32(address + 44, entry_addr);
            mem.write32(address + 48, stack_addr);
            mem.write32(address + 52, stackSize);
            mem.write32(address + 56, gpReg_addr);
            mem.write32(address + 60, initPriority);
            mem.write32(address + 64, currentPriority);
            mem.write32(address + 68, waitType);
            mem.write32(address + 72, waitId);
            mem.write32(address + 78, wakeupCount);
            mem.write32(address + 82, exitStatus);
            mem.write64(address + 86, runClocks);
            mem.write32(address + 94, intrPreemptCount);
            mem.write32(address + 98, threadPreemptCount);
            mem.write32(address + 102, releaseCount);
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
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.remove(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelDeleteSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            if (sema.numWaitThreads > 0) {
                Modules.log.warn("sceKernelDeleteSema numWaitThreads " + sema.numWaitThreads);

                // Find threads waiting on this sema and wake them up
                for (Iterator<SceKernelThreadInfo> it = threadlist.values().iterator(); it.hasNext(); ) {
                    SceKernelThreadInfo thread = it.next();

                    if (thread.wait.waitingOnSemaphore &&
                        thread.wait.Semaphore_id == semaid) {
                        // Untrack
                        thread.wait.waitingOnSemaphore = false;

                        // Return WAIT_DELETE
                        thread.cpuContext.gpr[2] = PSP_ERROR_ERROR_WAIT_DELETE;

                        // Wakeup
                        thread.status = PspThreadStatus.PSP_THREAD_READY;
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
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ILLEGAL_COUNT;
            return;
        }

        if (semaid <= 0) {
            Modules.log.warn("hleKernelWaitSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("hleKernelWaitSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
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
                current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
                current_thread.do_callbacks = do_callbacks;

                // Wait on a specific semaphore
                current_thread.wait.forever = (timeout_addr == 0);
                current_thread.wait.timeout = microsToSteps(micros);
                current_thread.wait.steps = 0;

                current_thread.wait.waitingOnSemaphore = true;
                current_thread.wait.Semaphore_id = semaid;
                current_thread.wait.Semaphore_signal = signal;

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
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ILLEGAL_COUNT;
            return;
        }

        if (semaid <= 0) {
            Modules.log.warn("sceKernelSignalSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelSignalSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
        } else if (sema.currentCount + signal > sema.maxCount) {
            Modules.log.warn("sceKernelSignalSema - overflow signal=" + signal + " current=" + sema.currentCount + " max=" + sema.maxCount);
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_SEMA_OVERFLOW;
            // TODO clamp and continue anyway?
        } else {
            sema.currentCount += signal;

            // For each thread (sorted by priority),
            // if the thread is waiting on this semaphore,
            // and signal <= currentCount,
            // then wake up the thread and adjust currentCount.
            // repeat for all remaining threads or until currentCount = 0.
            Collection<SceKernelThreadInfo> c = threadlist.values();
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
                    thread.status = PspThreadStatus.PSP_THREAD_READY;

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
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_ILLEGAL_COUNT;
            return;
        }

        if (semaid <= 0) {
            Modules.log.warn("sceKernelPollSema bad id=0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelPollSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
        } else if (sema.currentCount - signal < 0) {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_SEMA_ZERO;
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
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelCancelSema - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
        } else {
            sema.numWaitThreads = 0;

            // Find threads waiting on this sema and wake them up
            for (Iterator<SceKernelThreadInfo> it = threadlist.values().iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnSemaphore &&
                    thread.wait.Semaphore_id == semaid) {
                    // Untrack
                    thread.wait.waitingOnSemaphore = false;

                    // Return WAIT_CANCELLED
                    thread.cpuContext.gpr[2] = PSP_ERROR_WAIT_CANCELLED;

                    // Wakeup
                    thread.status = PspThreadStatus.PSP_THREAD_READY;
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
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_UNKNOWN_UID;
            return;
        }

        SceUidManager.checkUidPurpose(semaid, "ThreadMan-sema", true);
        SceKernelSemaphoreInfo sema = semalist.get(semaid);
        if (sema == null) {
            Modules.log.warn("sceKernelReferSemaStatus - unknown uid 0x" + Integer.toHexString(semaid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
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

    // --------------------------- Event Flag ---------------------------

    private final static int PSP_EVENT_WAITMULTIPLE = 0x200;

    private final static int PSP_EVENT_WAITAND = 0x00;
    private final static int PSP_EVENT_WAITOR = 0x01;
    private final static int PSP_EVENT_WAITCLEARALL = 0x10;
    private final static int PSP_EVENT_WAITCLEAR = 0x20;

    public void ThreadMan_sceKernelCreateEventFlag(int name_addr, int attr, int initPattern, int option)
    {
        String name = readStringZ(Memory.getInstance().mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        Modules.log.debug("sceKernelCreateEventFlag(name='" + name
            + "',attr=0x" + Integer.toHexString(attr)
            + ",initPattern=0x" + Integer.toHexString(initPattern)
            + ",option=0x" + Integer.toHexString(option) + ")");

        if (option !=0) Modules.log.warn("sceKernelCreateEventFlag: UNSUPPORTED Option Value");
        SceKernelEventFlagInfo event = new SceKernelEventFlagInfo(name, attr, initPattern, initPattern); //initPattern and currentPattern should be the same at init

        Modules.log.debug("sceKernelCreateEventFlag assigned uid=0x" + Integer.toHexString(event.uid));
        Emulator.getProcessor().cpu.gpr[2] = event.uid;
    }

    public void ThreadMan_sceKernelDeleteEventFlag(int uid)
    {
        Modules.log.debug("sceKernelDeleteEventFlag uid=0x" + Integer.toHexString(uid));
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventlist.remove(uid);
        if (event == null) {
            Modules.log.warn("sceKernelDeleteEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            if (event.numWaitThreads > 0) {
                Modules.log.warn("sceKernelDeleteEventFlag numWaitThreads " + event.numWaitThreads);
                updateWaitingEventFlags();
            }

            updateWaitingEventFlags();
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelSetEventFlag(int uid, int bitsToSet)
    {
        Modules.log.debug("sceKernelSetEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToSet=0x" + Integer.toHexString(bitsToSet));
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventlist.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelSetEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern |= bitsToSet;
            updateWaitingEventFlags();
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelClearEventFlag(int uid, int bitsToKeep)
    {
        Modules.log.debug("sceKernelClearEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToKeep=0x" + Integer.toHexString(bitsToKeep));
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventlist.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelClearEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern &= bitsToKeep;
            updateWaitingEventFlags();
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** If there was a match we attempt to write to outBits_addr.
     * @return true if there was a match. */
    private boolean checkEventFlag(SceKernelEventFlagInfo event, int bits, int wait, int outBits_addr) {
        boolean matched = false;

        if ((wait & PSP_EVENT_WAITOR) == PSP_EVENT_WAITOR &&
            (event.currentPattern & bits) != 0) {
            //Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITOR");
            matched = true;
        }

        // PSP_EVENT_WAITAND is 0x00, check last
        else if ((event.currentPattern & bits) == bits) {
            //Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITAND");
            matched = true;
        }

        if (matched) {
            // All 32 bits
            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(outBits_addr)) {
                mem.write32(outBits_addr, event.currentPattern);
            }

            // PSP_EVENT_WAITCLEARALL from noxa/pspplayer
            if ((wait & PSP_EVENT_WAITCLEARALL) == PSP_EVENT_WAITCLEARALL) {
                Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITCLEARALL");
                event.currentPattern = 0;
            }

            if ((wait & PSP_EVENT_WAITCLEAR) == PSP_EVENT_WAITCLEAR) {
                //Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITCLEAR");
                event.currentPattern &= ~bits;
            }
        }

        return matched;
    }

    // Check all waiting threads for all event flags.
    // Could be optimized for all waiting threads for a specific event flag,
    // but then DeleteEventFlag will need special case handling.
    private void updateWaitingEventFlags() {
        for (Iterator<SceKernelThreadInfo> it = threadlist.values().iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            // We're assuming if waitingOnEventFlag is set then thread.status = waiting
            if (thread.wait.waitingOnEventFlag) {

                int evid = thread.wait.EventFlag_id;
                int bits = thread.wait.EventFlag_bits;
                int wait = thread.wait.EventFlag_wait;
                int outBits_addr = thread.wait.EventFlag_outBits_addr;

                SceKernelEventFlagInfo event = eventlist.get(evid);
                if (event != null) {
                    // Check EventFlag
                    if (checkEventFlag(event, bits, wait, outBits_addr)) {
                        // Success
                        Modules.log.debug("Delete/Set/Clear EventFlag waking thread 0x" + Integer.toHexString(thread.uid)
                            + " name:'" + thread.name + "'");

                        // Update numWaitThreads
                        event.numWaitThreads--;

                        // Untrack
                        thread.wait.waitingOnEventFlag = false;

                        // Return success
                        thread.cpuContext.gpr[2] = 0;

                        // Wakeup
                        thread.status = PspThreadStatus.PSP_THREAD_READY;
                    }
                } else {
                    // EventFlag was deleted
                    Modules.log.warn("EventFlag deleted while we were waiting for it!");

                    // Untrack
                    thread.wait.waitingOnEventFlag = false;

                    // Return PSP_ERROR_ERROR_WAIT_DELETE
                    thread.cpuContext.gpr[2] = PSP_ERROR_ERROR_WAIT_DELETE;

                    // Wakeup
                    thread.status = PspThreadStatus.PSP_THREAD_READY;
                }
            }
        }
    }

    public void ThreadMan_hleKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr, boolean do_callbacks)
    {
        Modules.log.debug("hleKernelWaitEventFlag uid=0x" + Integer.toHexString(uid)
            + " bits=0x" + Integer.toHexString(bits)
            + " wait=0x" + Integer.toHexString(wait)
            + " outBits=0x" + Integer.toHexString(outBits_addr)
            + " timeout=0x" + Integer.toHexString(timeout_addr)
            + " callbacks=" + do_callbacks);

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventlist.get(uid);
        if (event == null) {
            Modules.log.warn("hleKernelWaitEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_EVENT_FLAG;
        } else if (event.numWaitThreads >= 1 &&
            (event.attr & PSP_EVENT_WAITMULTIPLE) != PSP_EVENT_WAITMULTIPLE) {
            Modules.log.warn("hleKernelWaitEventFlag already another thread waiting on it");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_EVENT_FLAG_NO_MULTI_PERM;
        } else {
            Memory mem = Memory.getInstance();
            int micros = 0;
            if (mem.isAddressGood(timeout_addr)) {
                micros = mem.read32(timeout_addr);
                //Modules.log.debug("sceKernelWaitEventFlag found timeout micros = " + micros);
            }

            if (!checkEventFlag(event, bits, wait, outBits_addr)) {
                // Failed, but it's ok, just wait a little
                Modules.log.debug("hleKernelWaitEventFlag fast check failed");
                event.numWaitThreads++;

                // Go to wait state
                current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
                current_thread.do_callbacks = do_callbacks;

                // Wait on a specific event flag
                current_thread.wait.forever = (timeout_addr == 0);
                current_thread.wait.timeout = microsToSteps(micros);
                current_thread.wait.steps = 0;

                current_thread.wait.waitingOnEventFlag = true;
                current_thread.wait.EventFlag_id = uid;
                current_thread.wait.EventFlag_bits = bits;
                current_thread.wait.EventFlag_wait = wait;
                current_thread.wait.EventFlag_outBits_addr = outBits_addr;

                contextSwitch(nextThread());
            } else {
                // Success
                Modules.log.debug("hleKernelWaitEventFlag fast check succeeded");
                Emulator.getProcessor().cpu.gpr[2] = 0;
                // TODO yield anyway?
                contextSwitch(nextThread());
            }
        }
    }

    public void ThreadMan_sceKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr)
    {
        Modules.log.debug("sceKernelWaitEventFlag redirecting to hleKernelWaitEventFlag(callbacks=false)");
        ThreadMan_hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, false);
    }

    public void ThreadMan_sceKernelWaitEventFlagCB(int uid, int bits, int wait, int outBits_addr, int timeout_addr)
    {
        Modules.log.debug("sceKernelWaitEventFlagCB redirecting to hleKernelWaitEventFlag(callbacks=true)");
        ThreadMan_hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, true);
    }

    public void ThreadMan_sceKernelPollEventFlag(int uid, int bits, int wait, int outBits_addr)
    {
        Modules.log.debug("sceKernelPollEventFlag uid=0x" + Integer.toHexString(uid)
            + " bits=0x" + Integer.toHexString(bits)
            + " wait=0x" + Integer.toHexString(wait)
            + " outBits=0x" + Integer.toHexString(outBits_addr));

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventlist.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelPollEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            if (!checkEventFlag(event, bits, wait, outBits_addr)) {
                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_EVENT_FLAG_POLL_FAILED;
            } else {
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }
        }
    }

    public void ThreadMan_sceKernelCancelEventFlag(int uid, int newPattern, int result_addr)
    {
        Modules.log.debug("sceKernelCancelEventFlag uid=0x" + Integer.toHexString(uid)
            + " newPattern=0x" + Integer.toHexString(newPattern)
            + " result=0x" + Integer.toHexString(result_addr));

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventlist.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelCancelEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern = newPattern;
            event.numWaitThreads = 0;

            // TODO CANNOT_CANCEL 0x80020261
            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(result_addr)) {
                mem.write32(result_addr, 0);
            }

            // Find threads waiting on this event flag and wake them up
            for (Iterator<SceKernelThreadInfo> it = threadlist.values().iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if (thread.wait.waitingOnEventFlag &&
                    thread.wait.EventFlag_id == uid) {
                    // Untrack
                    thread.wait.waitingOnEventFlag = false;

                    // Return WAIT_CANCELLED
                    thread.cpuContext.gpr[2] = PSP_ERROR_WAIT_CANCELLED;

                    // Wakeup
                    thread.status = PspThreadStatus.PSP_THREAD_READY;
                }
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void ThreadMan_sceKernelReferEventFlagStatus(int uid, int addr)
    {
        Modules.log.debug("sceKernelReferEventFlagStatus uid=0x" + Integer.toHexString(uid)
            + " addr=0x" + Integer.toHexString(addr));

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventlist.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelReferEventFlagStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.write(Memory.getInstance(), addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private class SceKernelEventFlagInfo
    {
        private final String name;
        private final int attr;
        private final int initPattern;
        private int currentPattern;
        private int numWaitThreads;

        private final int uid;

        public SceKernelEventFlagInfo(String name,int attr,int initPattern,int currentPattern)
        {
            this.name = name;
            this.attr = attr;
            this.initPattern = initPattern;
            this.currentPattern = currentPattern;
            this.numWaitThreads = 0;

            uid = SceUidManager.getNewUid("ThreadMan-eventflag");
            eventlist.put(uid, this);
        }

        public void write(Memory mem, int address)
        {
            mem.write32(address, 52); // size

            int i, len = name.length();
            for (i = 0; i < 32 && i < len; i++)
                mem.write8(address + 4 + i, (byte)name.charAt(i));
            for (; i < 32; i++)
                mem.write8(address + 4 + i, (byte)0);

            mem.write32(address + 36, attr);
            mem.write32(address + 40, initPattern);
            mem.write32(address + 44, currentPattern);
            mem.write32(address + 48, numWaitThreads);
        }
    }
}
