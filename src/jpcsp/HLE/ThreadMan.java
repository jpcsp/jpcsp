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


public class ThreadMan {
    private static ThreadMan instance;
    private static HashMap<Integer, SceKernelThreadInfo> threadlist;
    private static HashMap<Integer, SceKernelSemaphoreInfo> semalist;
    private static HashMap<Integer, SceKernelEventFlagInfo> eventlist;
    private static HashMap<Integer, Integer> waitthreadendlist; // <thread to wait on, thread to wakeup>
    private  ArrayList<Integer> waitingThreads;
    private SceKernelThreadInfo current_thread;
    private SceKernelThreadInfo idle0, idle1;
    private int continuousIdleCycles; // watch dog timer - number of continuous cycles in any idle thread
    private int syscallFreeCycles; // watch dog timer - number of cycles since last syscall

    // TODO figure out a decent number of cycles to wait
    private final int WDT_THREAD_IDLE_CYCLES = 1000000;
    private final int WDT_THREAD_HOG_CYCLES = 10000000;

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
        //Modules.log.debug("ThreadMan: Initialise entry:0x" + Integer.toHexString(entry_addr));

        threadlist = new HashMap<Integer, SceKernelThreadInfo>();
        semalist = new HashMap<Integer, SceKernelSemaphoreInfo>();
        eventlist = new HashMap<Integer, SceKernelEventFlagInfo>();
        waitthreadendlist = new HashMap<Integer, Integer>();
        waitingThreads= new ArrayList<Integer>();

        // Clear stack allocation info
        //pspSysMem.get_instance().malloc(2, pspSysMem.PSP_SMEM_Addr, 0x000fffff, 0x09f00000);
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
        CpuState cpu = Emulator.getProcessor().cpu;
        if (current_thread != null) {
            current_thread.runClocks++;

            //Modules.log.debug("pc=" + Emulator.getProcessor().pc + " ra=" + Emulator.getProcessor().gpr[31]);

            // Hook jr ra to 0 (thread function returned)
            if (cpu.pc == 0 && cpu.gpr[31] == 0) {
                // Thread has exited
                Modules.log.debug("Thread exit detected SceUID=" + Integer.toHexString(current_thread.uid)
                    + " name:'" + current_thread.name + "' return:" + cpu.gpr[2]);
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

            // Decrement delaysteps on sleeping threads
            if (thread.status == PspThreadStatus.PSP_THREAD_WAITING) {
                if (thread.delaysteps > 0) {
                    thread.delaysteps--;
                }
                if (thread.delaysteps == 0) {
                    thread.status = PspThreadStatus.PSP_THREAD_READY;

                    // If this thread was doing sceKernelWaitThreadEnd then remove the wakeup callback
                    if (thread.do_waitThreadEnd) {
                        thread.do_waitThreadEnd = false;
                        waitthreadendlist.remove(thread.waitThreadEndUid);
                    }
                }
            }

            // Cleanup stopped threads marked for deletion
            if (thread.status == PspThreadStatus.PSP_THREAD_STOPPED) {
                if (thread.do_delete) {
                    // cleanup thread - free the stack
                    if (thread.stack_addr != 0) {
                        pspSysMem.get_instance().free(thread.stack_addr);
                    }
                    // TODO remove from any internal lists? such as sema waiting lists

                    // Changed to thread safe iterator.remove
                    //threadlist.remove(thread.uid);
                    it.remove();

                    SceUIDMan.get_instance().releaseUid(thread.uid, "ThreadMan-thread");
                }
            }
        }
    }

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
        if (SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadlist.get(uid);
            thread.status = PspThreadStatus.PSP_THREAD_READY;
        }
    }

    private void onThreadStopped(SceKernelThreadInfo stoppedThread) {
        // Wakeup threads that are in sceKernelWaitThreadEnd
        Integer uid = waitthreadendlist.remove(stoppedThread.uid);
        if (uid != null) {
            // This should be consistent/no error checking required because waitthreadendlist can only be changed privately
            SceKernelThreadInfo waitingThread = threadlist.get(uid);
            waitingThread.status = PspThreadStatus.PSP_THREAD_READY;
            waitingThread.do_waitThreadEnd = false;
        }
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
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan-thread", true);
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
        SceUIDMan.get_instance().checkUidPurpose(a0, "ThreadMan-thread", true);
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
        SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", true);
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
            // TODO test on real psp if len is not 32-bit aligned will the psp align it?
            thread.cpuContext.gpr[4] = len; // a0 = len
            thread.cpuContext.gpr[5] = thread.cpuContext.gpr[29]; // a1 = pointer to copy of data at data_addr
            thread.status = PspThreadStatus.PSP_THREAD_READY;

            Emulator.getProcessor().cpu.gpr[2] = 0;

            // TODO does start thread defer start or really start?
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

    /** sleep the current thread until a registered callback is triggered */
    public void ThreadMan_sceKernelSleepThreadCB() {
        Modules.log.debug("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        current_thread.do_callbacks = true;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** sleep the current thread */
    public void ThreadMan_sceKernelSleepThread() {
        Modules.log.debug("sceKernelSleepThread SceUID=" + Integer.toHexString(current_thread.uid) + " name:'" + current_thread.name + "'");

        current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
        current_thread.do_callbacks = false;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** sleep the current thread for a certain number of microseconds */
    public void ThreadMan_sceKernelDelayThread(int micros) {
        current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
        //current_thread.delaysteps = micros * 200000000 / 1000000; // TODO delaysteps = micros * steprate
        current_thread.delaysteps = micros; // test version
        current_thread.do_callbacks = false;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
    }

    /** sleep the current thread for a certain number of microseconds */
    public void ThreadMan_sceKernelDelayThreadCB(int micros) {
        current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
        //current_thread.delaysteps = micros * 200000000 / 1000000; // TODO delaysteps = micros * steprate
        current_thread.delaysteps = micros; // test version
        current_thread.do_callbacks = true;
        Emulator.getProcessor().cpu.gpr[2] = 0;

        contextSwitch(nextThread());
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

    public void ThreadMan_sceKernelReferThreadStatus(int a0, int a1) {
        //Get the status information for the specified thread
        SceKernelThreadInfo thread = threadlist.get(a0);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
            return;
        }

        //Modules.log.debug("sceKernelReferThreadStatus SceKernelThreadInfo=" + Integer.toHexString(a1));

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

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void ThreadMan_sceKernelGetThreadmanIdList(int type,
        int readbuf_addr, int readbufsize, int idcount_addr) {
        Memory mem = Memory.getInstance();

        Modules.log.warn("UNIMPLEMENTED:sceKernelGetThreadmanIdList type=" + type
            + " readbuf:0x" + Integer.toHexString(readbuf_addr)
            + " readbufsize:" + readbufsize
            + " idcount:0x" + Integer.toHexString(idcount_addr));

        // TODO type=SCE_KERNEL_TMID_Thread, don't show the idle threads!

        // Fake success - 0 entries written
        if (mem.isAddressGood(idcount_addr)) {
            idcount_addr = 0;
        }
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void ThreadMan_sceKernelChangeThreadPriority(int uid, int priority) {
        if (uid == 0) uid = getCurrentThreadID();
        SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", true);
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

    public void ThreadMan_sceKernelChangeCurrentThreadAttr(int unknown, int attr) {
        Modules.log.debug("sceKernelChangeCurrentThreadAttr"
                + " unknown:" + unknown
                + " newAttr:0x" + Integer.toHexString(attr)
                + " oldAttr:0x" + Integer.toHexString(current_thread.attr));

        // Probably meant to be sceKernelChangeThreadAttr unknown=uid
        if (unknown != 0)
            Modules.log.warn("sceKernelChangeCurrentThreadAttr unknown:" + unknown + " non-zero");

        // Don't allow switching into kernel mode!
        if ((current_thread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER &&
            (attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
            Modules.log.debug("sceKernelChangeCurrentThreadAttr forcing user mode");
            attr |= PSP_THREAD_ATTR_USER;
        }

        current_thread.attr = attr;

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void ThreadMan_sceKernelWakeupThread(int uid) {
        SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", true);
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
        SceUIDMan.get_instance().checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadlist.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelWaitThreadEnd unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_THREAD;
        } else if (waitthreadendlist.get(uid) != null) {
            // TODO out current implementation only allows 1 thread to wait on another thread to end
            Modules.log.warn("UNIMPLEMENTED:sceKernelWaitThreadEnd another thread already waiting for the target thread to end");
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            waitthreadendlist.put(uid, current_thread.uid);

            if (micros > 0) {
                current_thread.status = PspThreadStatus.PSP_THREAD_WAITING;
                //current_thread.delaysteps = micros * 200000000 / 1000000; // TODO delaysteps = micros * steprate
                current_thread.delaysteps = micros; // test version
            } else {
                current_thread.status = PspThreadStatus.PSP_THREAD_SUSPEND;
            }

            current_thread.do_callbacks = false;
            current_thread.do_waitThreadEnd = true;
            current_thread.waitThreadEndUid = uid;
            Emulator.getProcessor().cpu.gpr[2] = 0;

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

    public void ThreadMan_sceKernelGetSystemTimeLow() {
        Modules.log.debug("sceKernelGetSystemTimeLow");
        long systemTime = System.nanoTime();
        Emulator.getProcessor().cpu.gpr[2] = (int)(systemTime & 0xffffffffL);
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
        if (size > 0) {
            //int p = 0x09f00000 - stackAllocated;
            //stackAllocated += size;
            //return p;

            //int p = pspSysMem.get_instance().malloc(2, pspSysMem.PSP_SMEM_HighAligned, size, 0x1000);
            int p = pspSysMem.get_instance().malloc(2, pspSysMem.PSP_SMEM_High, size, 0);
            p += size;

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
        private long delaysteps;
        private boolean do_delete;
        private boolean do_callbacks; // in this implementation, only valid for PSP_THREAD_WAITING and PSP_THREAD_SUSPEND

        private boolean do_waitThreadEnd;
        private int waitThreadEndUid;

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
            if (stackSize > 0 && (attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK)
                memset(stack_addr - stackSize, (byte)0xFF, stackSize);
            gpReg_addr = Emulator.getProcessor().cpu.gpr[28]; // inherit gpReg
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

            delaysteps = 0;
            do_delete = false;
            do_callbacks = false;
            do_waitThreadEnd = false;
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
    }

    public void ThreadMan_sceKernelCreateSema(int name_addr, int attr, int initVal, int maxVal, int option)
    {
        String name = readStringZ(Memory.getInstance().mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        Modules.log.debug("sceKernelCreateSema name=" + name + " attr= " + attr + " initVal= " + initVal + " maxVal= "+ maxVal + " option= " + option);
        int initCount = initVal;
        int currentCount = initVal;
        int maxCount = maxVal;
        if(option !=0) Modules.log.warn("sceKernelCreateSema: UNSUPPORTED Option Value");
        SceKernelSemaphoreInfo sema = new SceKernelSemaphoreInfo(name,attr,initCount,currentCount,maxCount);

        Emulator.getProcessor().cpu.gpr[2] = sema.uid;
    }

    public void ThreadMan_sceKernelWaitSema(int semaid , int signal , int timeoutptr , int timeout)
    {
          Modules.log.debug("sceKernelWaitSema id= " + semaid + " signal= " + signal + " timeout = " + timeout);
            SceUIDMan.get_instance().checkUidPurpose(semaid, "ThreadMan-sema", true);
            SceKernelSemaphoreInfo sema = semalist.get(semaid);
            if (sema == null) {
                    Modules.log.warn("sceKernelWaitSema - unknown uid " + Integer.toHexString(semaid));
                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
            } else {
                if(sema.currentCount >= signal)
                {
                  sema.currentCount-=signal;
                  Emulator.getProcessor().cpu.gpr[2] = 0;
                }
                else
                {
                    waitingThreads.add(getCurrentThreadID());
                    Modules.log.debug(getCurrentThreadID());
                    Emulator.getProcessor().cpu.gpr[2] = 0;
                    blockCurrentThread();
                }
            }
    }

    public void ThreadMan_sceKernelWaitSemaCB(int semaid , int signal , int timeoutptr , int timeout)
    {
        // TODO handle callbacks
        Modules.log.debug("sceKernelWaitSemaCB redirecting to sceKernelWaitSema");
        ThreadMan_sceKernelWaitSema(semaid, signal, timeoutptr, timeout);
    }

    public void ThreadMan_sceKernelSignalSema(int semaid , int signal)
    {
        Modules.log.debug("sceKernelSignalSema id =" + semaid + " signal =" + signal);
            SceUIDMan.get_instance().checkUidPurpose(semaid, "ThreadMan-sema", true);
            SceKernelSemaphoreInfo sema = semalist.get(semaid);
            if (sema == null) {
                    Modules.log.warn("sceKernelSignalSema - unknown uid " + Integer.toHexString(semaid));
                Emulator.getProcessor().cpu.gpr[2] = PSP_ERROR_NOT_FOUND_SEMAPHORE;
            } else {
                int oldcurrentCount = sema.currentCount;
                sema.currentCount+=signal;
                Iterator<Integer> waitThreads = waitingThreads.iterator();
                while(waitThreads.hasNext())
                {
                  Modules.log.debug("UNNNNNNNNNNNNNNNN Wait threads = " + waitThreads.next());
                }
            }
            Emulator.getProcessor().cpu.gpr[2] = 0;

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

    public void ThreadMan_sceKernelCreateEventFlag(int name_addr, int attr, int initPattern, int option)
    {
        String name = readStringZ(Memory.getInstance().mainmemory,
            (name_addr & 0x3fffffff) - MemoryMap.START_RAM);

        Modules.log.debug("sceKernelCreateEventFlag name=" + name + " attr= " + attr + " initPattern= " + initPattern+ " option= " + option);

        if(option !=0) Modules.log.warn("sceKernelCreateSema: UNSUPPORTED Option Value");
        SceKernelEventFlagInfo event = new SceKernelEventFlagInfo(name,attr,initPattern,initPattern);//initPattern and currentPattern should be the same at init

        Emulator.getProcessor().cpu.gpr[2] = event.uid;
    }
    private class SceKernelEventFlagInfo
    {
      private String name;
      private int attr;
      private int initPattern;
      private int currentPattern;
      private int numWaitThreads;//NOT sure if that should be here or merged with the semaphore waitthreads..

      private int uid;

      public SceKernelEventFlagInfo(String name,int attr,int initPattern,int currentPattern)
      {
        this.name=name;
        this.attr=attr;
        this.initPattern=initPattern;
        this.currentPattern=currentPattern;
        uid = SceUIDMan.get_instance().getNewUid("ThreadMan-eventflag");
        eventlist.put(uid, this);

      }
    }
}
