/*
Thread Manager
Function:
- HLE thread related things in http://psp.jim.sh/pspsdk-doc/group__ThreadMan.html
- Schedule threads

Note:
- incomplete and not fully tested

Todo:
- user/kernel read/write permissions on addresses (such as refer status)
- move callbacks to another file
- move scheduler to another file

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
import java.util.Map;

import jpcsp.AllegrexOpcodes;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.DumpDebugState;
import jpcsp.Debugger.DisassemblerModule.syscallsFirm15;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.*;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.scheduler.Scheduler;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;

public class ThreadMan {

    private static ThreadMan instance;
    private HashMap<Integer, SceKernelThreadInfo> threadMap;
    private LinkedList<SceKernelThreadInfo> readyThreads;
    private SceKernelThreadInfo currentThread;
    private SceKernelThreadInfo idle0, idle1;
    private int continuousIdleCycles; // watch dog timer - number of continuous cycles in any idle thread
    private int syscallFreeCycles; // watch dog timer - number of cycles since last syscall
    public Statistics statistics;

    private boolean dispatchThreadEnabled;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_DISABLED = 0;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED = 1;

    // TODO figure out a decent number of cycles to wait
    private static final int WDT_THREAD_IDLE_CYCLES = 1000000;
    private static final int WDT_THREAD_HOG_CYCLES = (0x0A000000 - 0x08400000) * 3; // memset can take a while when you're using sb!

	protected static final int CALLBACKID_REGISTER = 16; // $s0
	protected CallbackManager callbackManager = new CallbackManager();

	protected static final int IDLE_THREAD_ADDRESS           = MemoryMap.START_RAM;
	public    static final int THREAD_EXIT_HANDLER_ADDRESS   = MemoryMap.START_RAM + 0x20;
	protected static final int CALLBACK_EXIT_HANDLER_ADDRESS = MemoryMap.START_RAM + 0x30;
    protected static final int ASYNC_LOOP_ADDRESS            = MemoryMap.START_RAM + 0x40;

    private HashMap<Integer, SceKernelCallbackInfo> callbackMap;

    private boolean enableWaitThreadEndCB;
    private boolean USE_THREAD_BANLIST = false;
    private static final boolean LOG_CONTEXT_SWITCHING = true;
    private static final boolean IGNORE_DELAY = false;
    private static final boolean LOG_INSTRUCTIONS = false;
    public boolean exitCalled = false;

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

    public Iterator<SceKernelThreadInfo> iteratorByPriority() {
        Collection<SceKernelThreadInfo> c = threadMap.values();
        List<SceKernelThreadInfo> list = new LinkedList<SceKernelThreadInfo>(c);
        Collections.sort(list, idle0); // We need an instance of SceKernelThreadInfo for the comparator, so we use idle0
        return list.iterator();
    }

    /** call this when resetting the emulator
     * @param entry_addr entry from ELF header
     * @param attr from sceModuleInfo ELF section header */
    public void Initialise(int entry_addr, int attr, String pspfilename, int moduleid, boolean fromSyscall) {
        threadMap = new HashMap<Integer, SceKernelThreadInfo>();
        readyThreads = new LinkedList<SceKernelThreadInfo>();
        statistics = new Statistics();

        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();
		callbackManager.Initialize();

        install_idle_threads();
        install_thread_exit_handler();
        install_callback_exit_handler();
        install_async_loop_handler();

        // Create a thread the program will run inside

        // The stack size seems to be 0x40000 when starting the application from the VSH
        // and smaller when starting the application with sceKernelLoadExec() - guess: 0x4000.
        // This could not be reproduced on a PSP.
        int rootStackSize = (fromSyscall ? 0x4000 : 0x40000);
        currentThread = new SceKernelThreadInfo("root", entry_addr, 0x20, rootStackSize, attr);
        currentThread.moduleid = moduleid;
        threadMap.put(currentThread.uid, currentThread);

        // Set user mode bit if kernel mode bit is not present
        if ((currentThread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL) {
            currentThread.attr |= PSP_THREAD_ATTR_USER;
        }

        // Setup args by copying them onto the stack
        //Modules.log.debug("pspfilename - '" + pspfilename + "'");
        int len = pspfilename.length();
        int address = currentThread.cpuContext.gpr[29];
        writeStringZ(Memory.getInstance(), address, pspfilename);
        currentThread.cpuContext.gpr[4] = len + 1; // a0 = len + string terminator
        currentThread.cpuContext.gpr[5] = address; // a1 = pointer to arg data in stack

        currentThread.status = PSP_THREAD_READY;

        // Switch in the thread
        currentThread.status = PSP_THREAD_RUNNING;
        currentThread.restoreContext();
        syscallFreeCycles = 0;
        dispatchThreadEnabled = true;
    }

    private void install_idle_threads() {
        Memory mem = Memory.getInstance();

        // Generate 2 idle threads which can toggle between each other when there are no ready threads
        int instruction_addiu = // addiu a0, zr, 0
            ((AllegrexOpcodes.ADDIU & 0x3f) << 26)
            | ((0 & 0x1f) << 21)
            | ((4 & 0x1f) << 16);
        int instruction_lui = // lui ra, 0x08000000
            ((AllegrexOpcodes.LUI & 0x3f) << 26)
            | ((31 & 0x1f) << 16)
            | (0x0800 & 0x0000ffff);
        int instruction_jr = // jr ra
            ((AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (AllegrexOpcodes.JR & 0x3f)
            | ((31 & 0x1f) << 21);
        int instruction_syscall = // syscall 0x0201c [sceKernelDelayThread]
            ((AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (AllegrexOpcodes.SYSCALL & 0x3f)
            | ((syscallsFirm15.calls.sceKernelDelayThread.getSyscall() & 0x000fffff) << 6);

        // This memory is always reserved on a real PSP
        int reservedMem = pspSysMem.getInstance().malloc(1, pspSysMem.PSP_SMEM_Addr, 0x4000, MemoryMap.START_USERSPACE);

        mem.write32(IDLE_THREAD_ADDRESS + 0,  instruction_addiu);
        mem.write32(IDLE_THREAD_ADDRESS + 4,  instruction_lui);
        mem.write32(IDLE_THREAD_ADDRESS + 8,  instruction_jr);
        mem.write32(IDLE_THREAD_ADDRESS + 12, instruction_syscall);

        // lowest allowed priority is 0x77, so we are ok at 0x7f
        // Allocate a stack because interrupts can be processed by the
        // idle thread, using its stack.
        // The stack is allocated into the reservedMem area.
        idle0 = new SceKernelThreadInfo("idle0", IDLE_THREAD_ADDRESS | 0x80000000, 0x7f, 0, PSP_THREAD_ATTR_KERNEL);
        idle0.stackSize = 0x2000;
        idle0.stack_addr = reservedMem;
        idle0.reset();
        threadMap.put(idle0.uid, idle0);
        hleChangeThreadState(idle0, PSP_THREAD_READY);

        idle1 = new SceKernelThreadInfo("idle1", IDLE_THREAD_ADDRESS | 0x80000000, 0x7f, 0, PSP_THREAD_ATTR_KERNEL);
        idle1.stackSize = 0x2000;
        idle1.stack_addr = reservedMem + 0x2000;
        idle1.reset();
        threadMap.put(idle1.uid, idle1);
        hleChangeThreadState(idle1, PSP_THREAD_READY);

        continuousIdleCycles = 0;
    }

    private void install_thread_exit_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f000 [hleKernelExitThread]
            ((AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (AllegrexOpcodes.SYSCALL & 0x3f)
            | ((syscallsFirm15.calls.hleKernelExitThread.getSyscall() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void install_callback_exit_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f001 [hleKernelExitCallback]
            ((AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (AllegrexOpcodes.SYSCALL & 0x3f)
            | ((syscallsFirm15.calls.hleKernelExitCallback.getSyscall() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void install_async_loop_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f002 [hleKernelAsyncLoop]
            ((AllegrexOpcodes.SPECIAL & 0x3f) << 26)
            | (AllegrexOpcodes.SYSCALL & 0x3f)
            | ((syscallsFirm15.calls.hleKernelAsyncLoop.getSyscall() & 0x000fffff) << 6);

        int instruction_b = (AllegrexOpcodes.BEQ << 26) | 0xFFFE; // branch back to syscall
        int instruction_nop = (AllegrexOpcodes.SLL << 26); // nop

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(ASYNC_LOOP_ADDRESS + 0, instruction_syscall);
        mem.write32(ASYNC_LOOP_ADDRESS + 4, instruction_b);
        mem.write32(ASYNC_LOOP_ADDRESS + 8, instruction_nop);
        mem.write32(ASYNC_LOOP_ADDRESS + 12, instruction_jr);
    }

    /** to be called when exiting the emulation */
    public void exit() {
        exitCalled = true;

        if (threadMap != null) {
            Modules.log.info("----------------------------- ThreadMan exit -----------------------------");

            // Delete all the threads to collect statistics
            deleteThreads(threadMap.values());

            statistics.endTimeMillis = System.currentTimeMillis();
            Modules.log.info(String.format("ThreadMan Statistics (%,d cycles in %.3fs):", statistics.allCycles, statistics.getDurationMillis() / 1000.0));
            for (Statistics.ThreadStatistics threadStatistics : statistics.threads) {
                double percentage = 0;
                if (statistics.allCycles != 0) {
                	percentage = (threadStatistics.runClocks / (double) statistics.allCycles) * 100;
                }
                Modules.log.info("    Thread name:'" + threadStatistics.name + "' runClocks:" + threadStatistics.runClocks + " (" + String.format("%2.2f%%", percentage) + ")");
            }
        }
    }

    /** To be called from the main emulation loop
     *  This is only used when running in interpreter mode,
     *  i.e. it is no longer used when the Compiler is enabled.
     */
    public void step() {
        if (LOG_INSTRUCTIONS && Modules.log.isTraceEnabled()) {
            CpuState cpu = Emulator.getProcessor().cpu;

            if (!isIdleThread(currentThread) && cpu.pc != 0) {
            	int address = cpu.pc - 4;
            	int opcode = Memory.getInstance().read32(address);
            	Modules.log.trace(String.format("Executing %08X %s", address, Decoder.instruction(opcode).disasm(address, opcode)));
            }
        }

        if (currentThread != null) {
            currentThread.runClocks++;

            // Watch dog timer
            if (isIdleThread(currentThread)) {
                continuousIdleCycles++;
                if (continuousIdleCycles > WDT_THREAD_IDLE_CYCLES) {
                    Modules.log.info("Watch dog timer - pausing emulator (idle)");
                    Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_WDT_IDLE);
                    continuousIdleCycles = 0;
                }
            } else {
                continuousIdleCycles = 0;
                syscallFreeCycles++;
                if (syscallFreeCycles > WDT_THREAD_HOG_CYCLES) {
                    Modules.log.info("Watch dog timer - pausing emulator (thread hogging)");
                    Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_WDT_HOG);
                }
            }
        } else if (!exitCalled) {
            // We always need to be in a thread! we shouldn't get here.
            Modules.log.error("No ready threads!");
        }
    }

    /** Part of watch dog timer */
    public void clearSyscallFreeCycles() {
        syscallFreeCycles = 0;
    }

    private void internalContextSwitch(SceKernelThreadInfo newThread) {
        if (currentThread != null) {
            // Switch out old thread
            if (currentThread.status == PSP_THREAD_RUNNING) {
                hleChangeThreadState(currentThread, PSP_THREAD_READY);
            }

            // save registers
            currentThread.saveContext();
        }

        if (newThread != null) {
            // Switch in new thread
            hleChangeThreadState(newThread, PSP_THREAD_RUNNING);
            // restore registers
            newThread.restoreContext();

            if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled() && !isIdleThread(newThread)) {
                Modules.log.debug("---------------------------------------- SceUID=" + Integer.toHexString(newThread.uid) + " name:'" + newThread.name + "'");
            }
        } else {
            // When running under compiler mode this gets triggered by exit()
            if (!exitCalled) {
                DumpDebugState.dumpDebugState();

                Modules.log.info("No ready threads - pausing emulator. caller:" + getCallingFunction());
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
            }
        }

        currentThread = newThread;
        syscallFreeCycles = 0;

        RuntimeContext.update();
    }

    /** @param newThread The thread to switch in. */
    private void contextSwitch(SceKernelThreadInfo newThread) {
		if (IntrManager.getInstance().isInsideInterrupt()) {
			// No context switching inside an interrupt
			if (Modules.log.isDebugEnabled()) {
				Modules.log.debug("Inside an interrupt, not context switching to " + newThread);
			}
			return;
		}

    	if (!dispatchThreadEnabled) {
            Modules.log.info("DispatchThread disabled, not context switching to " + newThread);
    		return;
    	}

        internalContextSwitch(newThread);

    	checkThreadCallbacks(currentThread);
    }

    /** This function must have the property of never returning currentThread,
     * unless currentThread is already null.
     * @return The next thread to schedule (based on thread priorities). */
    private SceKernelThreadInfo nextThread() {
        // Find the thread with status PSP_THREAD_READY and the highest priority.
        // In this implementation low priority threads can get starved.
        // Remark: the currentThread is not present in the readyThreads List.
        SceKernelThreadInfo found = null;
        synchronized (readyThreads) {
            for (SceKernelThreadInfo thread : readyThreads) {
                if (found == null || thread.currentPriority < found.currentPriority) {
                    found = thread;
                }
            }
        }
        return found;
    }

    /**
     * Switch to the thread with status PSP_THREAD_READY and having the highest priority.
     * If the current thread is in status PSP_THREAD_READY and
     * still having the highest priority, nothing is changed.
     */
    public void hleRescheduleCurrentThread() {
		SceKernelThreadInfo newThread = nextThread();
		if (newThread != null &&
			(currentThread == null ||
			 currentThread.status != PSP_THREAD_RUNNING ||
			 currentThread.currentPriority > newThread.currentPriority)) {
			if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
				Modules.log.debug("Context switching to '" + newThread + "' after reschedule");
			}
			contextSwitch(newThread);
		}
    }

    /**
     * Same behavior as hleRescheduleCurrentThread()
     * excepted that it executes callbacks when doCallbacks == true
     */
    public void hleRescheduleCurrentThread(boolean doCallbacks) {
    	if (doCallbacks) {
    		if (currentThread != null) {
    			currentThread.doCallbacks = doCallbacks;
    		}
    		checkCallbacks();
    	}

    	hleRescheduleCurrentThread();
    }

    public int getCurrentThreadID() {
    	if (currentThread == null) {
    		return -1;
    	}

    	return currentThread.uid;
    }

    public SceKernelThreadInfo getCurrentThread() {
        return currentThread;
    }

    public boolean isIdleThread(SceKernelThreadInfo thread) {
        return (thread == idle0 || thread == idle1);
    }

    public boolean isKernelMode() {
        return ((currentThread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL);
    }

    public String getThreadName(int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            return "NOT A THREAD";
        } else {
            return thread.name;
        }
    }

    public void hleBlockCurrentThread() {
    	hleBlockCurrentThread(null);
    }

    public void hleBlockCurrentThreadCB() {
    	hleBlockCurrentThreadCB(null);
    }

    public void hleBlockCurrentThread(IAction onUnblockAction)
    {
        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
            Modules.log.debug("-------------------- block SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "' caller:" + getCallingFunction());
        }

        if (currentThread.status != PSP_THREAD_SUSPEND) {
	        currentThread.onUnblockAction = onUnblockAction;
	        hleChangeThreadState(currentThread, PSP_THREAD_SUSPEND);
        }

        hleRescheduleCurrentThread();
    }

    public void hleBlockCurrentThreadCB(IAction onUnblockAction)
    {
        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
            Modules.log.debug("-------------------- block SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "' caller:" + getCallingFunction());
        }

        if (currentThread.status != PSP_THREAD_SUSPEND) {
            currentThread.doCallbacks = true;
	        currentThread.onUnblockAction = onUnblockAction;
	        hleChangeThreadState(currentThread, PSP_THREAD_SUSPEND);
        }

        hleRescheduleCurrentThread(true);
    }

    public SceKernelThreadInfo getThreadById(int uid) {
        return threadMap.get(uid);
    }

    public void hleUnblockThread(int uid) {
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadMap.get(uid);
            hleChangeThreadState(thread, PSP_THREAD_READY);

            if (LOG_CONTEXT_SWITCHING && thread != null && Modules.log.isDebugEnabled()) {
                Modules.log.debug("-------------------- unblock SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' caller:" + getCallingFunction());
            }
        }
    }

    private String getCallingFunction() {
        String msg = "";
        StackTraceElement[] lines = new Exception().getStackTrace();
        if (lines.length >= 3) {
            msg = lines[2].toString();
            msg = msg.substring(0, msg.indexOf("("));
            //msg = "'" + msg.substring(msg.lastIndexOf(".") + 1, msg.length()) + "'";
            String[] parts = msg.split("\\.");
            msg = "'" + parts[parts.length - 2] + "." + parts[parts.length - 1] + "'";
        } else {
            for (StackTraceElement e : lines) {
                String line = e.toString();
                if (line.startsWith("jpcsp.Allegrex") || line.startsWith("jpcsp.Processor")) {
                    break;
                }
                msg += "\n" + line;
            }
        }
        return msg;
    }

    public void hleThreadWaitTimeout(SceKernelThreadInfo thread) {
    	if (thread.waitType == PSP_WAIT_NONE) {
    		// The thread is no longer waiting...
    	} else {
	        onWaitTimeout(thread);
	        hleChangeThreadState(thread, PSP_THREAD_READY);
    	}
    }

    /** Call this when a thread's wait timeout has expired.
     * You can assume the calling function will set thread.status = ready. */
    private void onWaitTimeout(SceKernelThreadInfo thread) {
        // ThreadEnd
        if (thread.wait.waitingOnThreadEnd) {
            // Untrack
            thread.wait.waitingOnThreadEnd = false;

            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        }

        // EventFlag
        else if (thread.wait.waitingOnEventFlag) {
            Managers.eventFlags.onThreadWaitTimeout(thread);
        }

        // Sema
        else if (thread.wait.waitingOnSemaphore) {
            Managers.semas.onThreadWaitTimeout(thread);
        }

        // UMD stat
        else if (thread.wait.waitingOnUmd) {
            Modules.sceUmdUserModule.onThreadWaitTimeout(thread);
        }

        // Mutex
        else if (thread.wait.waitingOnMutex) {
            Managers.mutex.onThreadWaitTimeout(thread);
        }

        // MsgPipe
        else if (thread.wait.waitingOnMsgPipeSend || thread.wait.waitingOnMsgPipeReceive) {
            Managers.msgPipes.onThreadWaitTimeout(thread);
        }

        //Mbx
        else if(thread.wait.waitingOnMbxReceive) {
            Managers.mbx.onThreadWaitTimeout(thread);
        }

        // IO has no timeout, it's always forever
    }

    private void deleteThreads(Iterable<SceKernelThreadInfo> iterable){
        //by removing the value from the iterable BEFORE it is tried to remove
        //from all collections this assures that concurrent modification exception will
        //not occur on the same thread, on the jdk collections, since only that only
        //happens when they alter the collection.
        Iterator<SceKernelThreadInfo> it = iterable.iterator();
        while (it.hasNext()) {
            SceKernelThreadInfo thread = it.next();
            it.remove();
            hleDeleteThread(thread);
        }
    }

    public void hleDeleteThread(SceKernelThreadInfo thread) {
    	if (!threadMap.containsKey(thread.uid)) {
    		Modules.log.debug(String.format("Thread %s already deleted", thread.toString()));
    		return;
    	}

    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("really deleting thread:'" + thread.name + "'");
    	}

        // cleanup thread - free the stack
        if (thread.stack_addr != 0) {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("thread:'" + thread.name + "' freeing stack " + String.format("0x%08X", thread.stack_addr));
        	}
            thread.deleteSysMemInfo();
        }

        cancelThreadWait(thread);
        threadMap.remove(thread.uid);
        SceUidManager.releaseUid(thread.uid, "ThreadMan-thread");

        // TODO remove from wait object reference count, example: sema.numWaitThreads--
        Managers.eventFlags.onThreadDeleted(thread);
        Managers.semas.onThreadDeleted(thread);
        Managers.mutex.onThreadDeleted(thread);
        Managers.msgPipes.onThreadDeleted(thread);
        Managers.mbx.onThreadDeleted(thread);
        Modules.sceUmdUserModule.onThreadDeleted(thread);
        RuntimeContext.onThreadDeleted(thread);
        // TODO blocking audio?
        // TODO async io?

        statistics.addThreadStatistics(thread);
    }

    private void removeFromReadyThreads(SceKernelThreadInfo thread) {
        synchronized (readyThreads) {
            readyThreads.remove(thread);
        }
    }

    private void addToReadyThreads(SceKernelThreadInfo thread) {
        synchronized (readyThreads) {
            readyThreads.add(thread);
        }
    }

    private void setToBeDeletedThread(SceKernelThreadInfo thread) {
        thread.doDelete = true;

        if (thread.status == PSP_THREAD_STOPPED) {
            // It's possible for a game to request the same thread to be deleted multiple times.
            // We only mark for deferred deletion.
            // Example:
            // - main thread calls sceKernelDeleteThread on child thread
            // - child thread calls sceKernelExitDeleteThread
        	if (thread.doDeleteAction == null) {
        		thread.doDeleteAction = new DeleteThreadAction(thread);
        		Scheduler.getInstance().addAction(0, thread.doDeleteAction);
        	}
        }
    }

    /** Use this to change a thread's state (ready, running, etc)
     * This function manages some lists such as waiting list and
     * deferred deletion list. */
    public void hleChangeThreadState(SceKernelThreadInfo thread, int newStatus) {
        if (thread == null) {
            return;
        }

        if (!dispatchThreadEnabled && thread == currentThread && newStatus != PSP_THREAD_RUNNING) {
            Modules.log.info("DispatchThread disabled, not changing thread state of " + thread + " to " + newStatus);
            return;
        }

        // Moving out of the following states...
        if (thread.status == PSP_THREAD_WAITING) {
        	if (thread.wait.waitTimeoutAction != null) {
        		Scheduler.getInstance().removeAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
        	}
            thread.doCallbacks = false;
        } else if (thread.status == PSP_THREAD_STOPPED) {
        	if (thread.doDeleteAction != null) {
        		Scheduler.getInstance().removeAction(0, thread.doDeleteAction);
        		thread.doDeleteAction = null;
        	}
        } else if (thread.status == PSP_THREAD_READY) {
            removeFromReadyThreads(thread);
        } else if (thread.status == PSP_THREAD_SUSPEND) {
        	if (thread.onUnblockAction != null) {
        		thread.onUnblockAction.execute();
        		thread.onUnblockAction = null;
        	}
            thread.doCallbacks = false;
        }

        thread.status = newStatus;

        // Moving to the following states...
        if (thread.status == PSP_THREAD_WAITING) {
        	if (thread.wait.waitTimeoutAction != null) {
        		Scheduler.getInstance().addAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
        	}

            // debug
            if (thread.waitType == PSP_WAIT_NONE) {
                Modules.log.warn("changeThreadState thread '" + thread.name + "' => PSP_THREAD_WAITING. waitType should NOT be PSP_WAIT_NONE. caller:" + getCallingFunction());
            }
        } else if (thread.status == PSP_THREAD_STOPPED) {
            // TODO check if stopped threads eventually get automatically deleted on a real psp
            // HACK auto delete module mgr threads
            if (thread.name.equals("root") || // should probably find the real name and change it
                thread.name.equals("SceModmgrStart") ||
                thread.name.equals("SceKernelModmgrStop")) {
                thread.doDelete = true;
            }

            if (thread.doDelete) {
            	if (thread.doDeleteAction == null) {
            		thread.doDeleteAction = new DeleteThreadAction(thread);
            		Scheduler.getInstance().addAction(0, thread.doDeleteAction);
            	}
            }
            onThreadStopped(thread);
        } else if (thread.status == PSP_THREAD_READY) {
            addToReadyThreads(thread);
            thread.waitType = PSP_WAIT_NONE;
            thread.wait.waitTimeoutAction = null;
        } else if (thread.status == PSP_THREAD_RUNNING) {
            // debug
            if (thread.waitType != PSP_WAIT_NONE) {
                Modules.log.error("changeThreadState thread '" + thread.name + "' => PSP_THREAD_RUNNING. waitType should be PSP_WAIT_NONE. caller:" + getCallingFunction());
            }
        }
    }

    private void cancelThreadWait(SceKernelThreadInfo thread) {
        // Cancel all waiting actions
        thread.wait.waitingOnEventFlag = false;
        thread.wait.waitingOnIo = false;
        thread.wait.waitingOnMbxReceive = false;
        thread.wait.waitingOnMsgPipeReceive = false;
        thread.wait.waitingOnMsgPipeSend = false;
        thread.wait.waitingOnMutex = false;
        thread.wait.waitingOnSemaphore = false;
        thread.wait.waitingOnThreadEnd = false;
        thread.wait.waitingOnUmd = false;
        thread.waitType = PSP_WAIT_NONE;
        if (thread.wait.waitTimeoutAction != null) {
        	Scheduler.getInstance().removeAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
        	thread.wait.waitTimeoutAction = null;
        }
    }

    private void terminateThread(SceKernelThreadInfo thread) {
        hleChangeThreadState(thread, PSP_THREAD_STOPPED);  // PSP_THREAD_STOPPED or PSP_THREAD_KILLED ?

        cancelThreadWait(thread);

        RuntimeContext.onThreadExit(thread);
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
                hleChangeThreadState(thread, PSP_THREAD_READY);
            }
        }
    }

	public void hleKernelExitCallback() {
		hleKernelExitCallback(Emulator.getProcessor().cpu);
	}

	private void hleKernelExitCallback(CpuState cpu) {
		int callbackId = cpu.gpr[CALLBACKID_REGISTER];
		Callback callback = callbackManager.remove(callbackId);
		if (callback != null) {
			if (Modules.log.isTraceEnabled()) {
				Modules.log.trace("End of callback " + callback);
			}
			cpu.gpr[CALLBACKID_REGISTER] = callback.getSavedIdRegister();
			cpu.gpr[31] = callback.getSavedRa();
			cpu.pc = callback.getSavedPc();
			IAction afterAction = callback.getAfterAction();
			if (afterAction != null) {
				afterAction.execute();
			}
		}
	}

	/**
	 * Execute the code at the given address.
	 * The code is executed in the context of the currentThread.
	 * Parameters ($a0, $a1, ...) may have been copied to the current CpuState
	 * before calling this method.
	 * This call can return before the completion of the code. Use the
	 * "afterAction" parameter to trigger some actions that need to be executed
	 * after the code (e.g. to evaluate a return value in $v0).
	 * 
	 * @param address     the address to be called
	 * @param afterAction the action to be executed after the completion of the code
	 */
	public void callAddress(int address, IAction afterAction) {
		callAddress(null, address, afterAction, null);
	}

	private void callAddress(SceKernelThreadInfo thread, int address, IAction afterAction, int[] parameters) {
		if (thread != null && thread != currentThread) {
			// Save the wait state of the thread to restore it after the call
    		int status = thread.status;
    		int waitType = thread.waitType;
    		int waitId = thread.waitId;
    		ThreadWaitInfo threadWaitInfo = thread.wait;
    		boolean doCallbacks = thread.doCallbacks;

            // Context switch to the requested thread
    		thread.waitType = PSP_WAIT_NONE;
    		internalContextSwitch(thread);

    		afterAction = new AfterCallAction(thread, status, waitType, waitId, threadWaitInfo, doCallbacks, afterAction);
		}

		CpuState cpu = Emulator.getProcessor().cpu;

		// Copy parameters ($a0, $a1, ...) to the cpu
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				cpu.gpr[4 + i] = parameters[i];
			}
		}

		int callbackId = callbackManager.getNewCallbackId();
		Callback callback = new Callback(callbackId, cpu.gpr[CALLBACKID_REGISTER], cpu.gpr[31], cpu.pc, afterAction);
		cpu.gpr[CALLBACKID_REGISTER] = callbackId;
		cpu.gpr[31] = CALLBACK_EXIT_HANDLER_ADDRESS;
		cpu.pc = address;

		callbackManager.addCallback(callback);
		if (thread == null) {
			RuntimeContext.executeCallback();
		} else {
			RuntimeContext.executeCallback(thread);
		}

		// When the compiler is enabled, the callback is already executed
		// when returning from RuntimeContext.executeCallback()
		if (cpu.pc == CALLBACK_EXIT_HANDLER_ADDRESS) {
			hleKernelExitCallback(cpu);
		}
	}

	/**
	 * Trigger a call to a callback in the context of a thread.
	 * This call can return before the completion of the callback. Use the
	 * "afterAction" parameter to trigger some actions that need to be executed
	 * after the callback (e.g. to evaluate a return value in $v0).
	 * 
	 * @param thread      the callback has to be executed by this thread (null means the currentThread)
	 * @param address     address of the callback
	 * @param afterAction action to be executed after the completion of the callback
	 */
	public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("Execute callback 0x%08X, afterAction=%s", address, afterAction));
		}

		callAddress(thread, address, afterAction, null);
	}

	/**
	 * Trigger a call to a callback in the context of a thread.
	 * This call can return before the completion of the callback. Use the
	 * "afterAction" parameter to trigger some actions that need to be executed
	 * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
	 * 
	 * @param thread      the callback has to be executed by this thread (null means the currentThread)
	 * @param address     address of the callback
	 * @param afterAction action to be executed after the completion of the callback
	 * @param registerA0  first parameter of the callback ($a0)
	 */
	public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, int registerA0) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("Execute callback 0x%08X($a0=0x%08X), afterAction=%s", address, registerA0, afterAction));
		}

		callAddress(thread, address, afterAction, new int[] { registerA0 });
	}

	/**
	 * Trigger a call to a callback in the context of a thread.
	 * This call can return before the completion of the callback. Use the
	 * "afterAction" parameter to trigger some actions that need to be executed
	 * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
	 * 
	 * @param thread      the callback has to be executed by this thread (null means the currentThread)
	 * @param address     address of the callback
	 * @param afterAction action to be executed after the completion of the callback
	 * @param registerA0  first parameter of the callback ($a0)
	 * @param registerA1  second parameter of the callback ($a1)
	 */
	public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, int registerA0, int registerA1) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X), afterAction=%s", address, registerA0, registerA1, afterAction));
		}

		callAddress(thread, address, afterAction, new int[] { registerA0, registerA1 });
	}

	/**
	 * Trigger a call to a callback in the context of a thread.
	 * This call can return before the completion of the callback. Use the
	 * "afterAction" parameter to trigger some actions that need to be executed
	 * after the callback (e.g. to evaluate a return value in cpu.gpr[2]).
	 * 
	 * @param thread      the callback has to be executed by this thread (null means the currentThread)
	 * @param address     address of the callback
	 * @param afterAction action to be executed after the completion of the callback
	 * @param registerA0  first parameter of the callback ($a0)
	 * @param registerA1  second parameter of the callback ($a1)
	 * @param registerA2  third parameter of the callback ($a2)
	 */
	public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, int registerA0, int registerA1, int registerA2) {
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X), afterAction=%s", address, registerA0, registerA1, registerA2, afterAction));
		}

		callAddress(thread, address, afterAction, new int[] { registerA0, registerA1, registerA2 });
	}

	public void hleKernelExitThread() {
		int exitStatus = Emulator.getProcessor().cpu.gpr[2];

		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("Thread exit detected SceUID=%x name='%s' return:0x%08X", currentThread.uid, currentThread.name, exitStatus));
		}

		sceKernelExitThread(exitStatus);
	}

    public void hleKernelAsyncLoop() {
        pspiofilemgr.getInstance().hleAsyncThread();
    }

	/** Note: Some functions allow uid = 0 = current thread, others don't.
     * if uid = 0 then $v0 is set to ERROR_ILLEGAL_THREAD and false is returned
     * if uid < 0 then $v0 is set to ERROR_NOT_FOUND_THREAD and false is returned */
    private boolean checkThreadID(int uid) {
        if (uid == 0) {
            Modules.log.warn("checkThreadID illegal thread (uid=0) caller:" + getCallingFunction());
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_THREAD;
            return false;
        } else if (uid < 0) {
            Modules.log.warn("checkThreadID not found thread " + Integer.toHexString(uid) + " (uid<0) caller:" + getCallingFunction());
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
            return false;
        } else {
            SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
            return true;
        }
    }

    public SceKernelThreadInfo hleKernelCreateThread(String name, int entry_addr,
        int initPriority, int stackSize, int attr, int option_addr) {

        if (option_addr != 0) {
            Modules.log.warn("hleKernelCreateThread unhandled SceKernelThreadOptParam");
        }

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);
        threadMap.put(thread.uid, thread);

        // inherit module id
        if (currentThread != null) {
            thread.moduleid = currentThread.moduleid;
        }

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("hleKernelCreateThread SceUID=" + Integer.toHexString(thread.uid)
        			+ " name:'" + thread.name
        			+ "' PC=" + Integer.toHexString(thread.cpuContext.pc)
        			+ " attr:0x" + Integer.toHexString(attr)
        			+ " pri:0x" + Integer.toHexString(initPriority)
        			+ " stackSize:0x" + Integer.toHexString(stackSize));
        }

        return thread;
    }

    public void sceKernelCreateThread(int name_addr, int entry_addr,
        int initPriority, int stackSize, int attr, int option_addr) {

        String name = readStringNZ(name_addr, 32);

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceKernelCreateThread redirecting to hleKernelCreateThread");
        }
        SceKernelThreadInfo thread = hleKernelCreateThread(name, entry_addr, initPriority, stackSize, attr, option_addr);

        if (thread.stackSize > 0 && thread.stack_addr == 0) {
            Modules.log.warn("sceKernelCreateThread not enough memory to create the stack");
        	hleDeleteThread(thread);
        	Emulator.getProcessor().cpu.gpr[2] = SceKernelErrors.ERROR_NO_MEMORY;
        	return;
        }

        // TODO user thread trying to create kernel thread should be disallowed with ERROR_ILLEGAL_ATTR

        // Inherit kernel mode if user mode bit is not set
        if ((currentThread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL &&
            (attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER) {
            Modules.log.debug("sceKernelCreateThread inheriting kernel mode");
            thread.attr |= PSP_THREAD_ATTR_KERNEL;
        }

        // Inherit user mode
        if ((currentThread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER) {
            if ((thread.attr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER)
                Modules.log.debug("sceKernelCreateThread inheriting user mode");
            thread.attr |= PSP_THREAD_ATTR_USER;
            // Always remove kernel mode bit
            thread.attr &= ~PSP_THREAD_ATTR_KERNEL;
        }

        Emulator.getProcessor().cpu.gpr[2] = thread.uid;
    }

    /** terminate thread */
    public void sceKernelTerminateThread(int uid) {
        if (!checkThreadID(uid)) return;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            Emulator.getProcessor().cpu.gpr[2] = 0;
            terminateThread(thread);
        }
    }

    /** mark a thread for deletion. */
    public void sceKernelDeleteThread(int uid) {
        if (uid == 0) uid = currentThread.uid;
        if (!checkThreadID(uid)) return;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            // Mark thread for deletion
            setToBeDeletedThread(thread);

            if (thread.status != PSP_THREAD_STOPPED) {
                Emulator.getProcessor().cpu.gpr[2] = ERROR_THREAD_IS_NOT_DORMANT;
            } else {
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }
        }
    }

    /** terminate thread, then mark it for deletion */
    public void sceKernelTerminateDeleteThread(int uid) {
        if (!checkThreadID(uid)) return;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            Modules.log.debug("sceKernelTerminateDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

            terminateThread(thread);

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
        "bgm thread", "sgx-psp-freq-thr", "sgx-psp-pcm-th", "ss playthread",
        "spcbgm", "scemainsamplebgmmp3", "se thread"
    };
    /* suspected sound thread names:
     * SndMain, SoundThread, At3Main, Atrac3PlayThread,
     * bgm thread, SceWaveMain, SasCore thread, soundThread,
     * ATRAC3 play thread, SAS Thread, XomAudio, sgx-psp-freq-thr,
     * sgx-psp-at3-th, sgx-psp-pcm-th, sgx-psp-sas-th, snd_tick_timer_thread,
     * snd_stream_service_thread_1, SAS / Main Audio, AudioMixThread,
     * snd_stream_service_thread_0, sound_poll_thread, stream_sound_poll_thread,
     * sndp thread, SndpThread, Ss PlayThread, SndSsThread, SPCBGM,
     * SE Thread, FMOD Software Mixer thread
     *
     * keywords:
     * snd, sound, at3, atrac3, sas, wave, pcm, audio, mpeg, fmod
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
                name.contains("audio") || name.contains("mpeg") ||
                name.contains("fmod")) {
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

    	if (Modules.log.isDebugEnabled()) {
	        Modules.log.debug("hleKernelStartThread SceUID=" + Integer.toHexString(thread.uid)
	            + " name:'" + thread.name
	            + "' dataLen=0x" + Integer.toHexString(userDataLength)
	            + " data=0x" + Integer.toHexString(userDataAddr)
	            + " gp=0x" + Integer.toHexString(gp));
    	}

        // Reset all thread parameters: a thread can be restarted when it has exited.
        thread.reset();

        // Setup args by copying them onto the stack
        //int address = thread.cpuContext.gpr[29];
        // 256 bytes padding between user data top and real stack top
        int address = (thread.stack_addr + thread.stackSize - 0x100) - ((userDataLength + 0xF) & ~0xF);
        if (userDataAddr == 0) {
            // Set the pointer to NULL when none is provided
            thread.cpuContext.gpr[4] = 0; // a0 = user data len
            thread.cpuContext.gpr[5] = 0; // a1 = pointer to arg data in stack
        } else {
            Memory.getInstance().memcpy(address, userDataAddr, userDataLength);
            thread.cpuContext.gpr[4] = userDataLength; // a0 = user data len
            thread.cpuContext.gpr[5] = address; // a1 = pointer to arg data in stack
        }

        // 64 bytes padding between program stack top and user data
        thread.cpuContext.gpr[29] = address - 0x40;

        // testing
        if (Modules.log.isDebugEnabled() && thread.cpuContext.gpr[28] != gp) {
            // from sceKernelStartModule is ok, anywhere else might be an error
            Modules.log.debug("hleKernelStartThread oldGP=0x" + Integer.toHexString(thread.cpuContext.gpr[28])
                + " newGP=0x" + Integer.toHexString(gp));
        }
        thread.cpuContext.gpr[28] = gp;

        // switch in the target thread if it's higher priority
        hleChangeThreadState(thread, PSP_THREAD_READY);
        if (thread.currentPriority < currentThread.currentPriority) {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("hleKernelStartThread switching in thread immediately");
        	}
            hleRescheduleCurrentThread();
        }
    }

    public void sceKernelStartThread(int uid, int len, int data_addr) {
        if (!checkThreadID(uid)) return;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("sceKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' banned, not starting");
            // Banned, fake start
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else {
            Modules.log.debug("sceKernelStartThread redirecting to hleKernelStartThread");
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleKernelStartThread(thread, len, data_addr, thread.gpReg_addr);
        }
    }

    /** exit the current thread */
    public void sceKernelExitThread(int exitStatus) {
    	SceKernelThreadInfo thread = currentThread;

    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelExitThread SceUID=" + Integer.toHexString(thread.uid)
    				+ " name:'" + thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));
    	}

    	thread.exitStatus = exitStatus;
        Emulator.getProcessor().cpu.gpr[2] = 0;
        hleChangeThreadState(thread, PSP_THREAD_STOPPED);
        RuntimeContext.onThreadExit(thread);

        hleRescheduleCurrentThread();
    }

    /** exit the current thread, then delete it */
    public void sceKernelExitDeleteThread(int exitStatus) {
    	SceKernelThreadInfo thread = currentThread; // save a reference for post context switch operations
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(thread.uid)
    				+ " name:'" + thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));
    	}

        // Exit
        thread.exitStatus = exitStatus;
        Emulator.getProcessor().cpu.gpr[2] = 0;
        hleChangeThreadState(thread, PSP_THREAD_STOPPED);
        RuntimeContext.onThreadExit(thread);

        // Mark thread for deletion
        setToBeDeletedThread(thread);

        hleRescheduleCurrentThread();
    }

    private void hleKernelSleepThread(boolean doCallbacks) {
    	if (currentThread.wakeupCount > 0) {
    		// sceKernelWakeupThread() has been called before, do not sleep
    		currentThread.wakeupCount--;
            Emulator.getProcessor().cpu.gpr[2] = 0;
    	} else {
	        // Go to wait state
	        // wait type
	        currentThread.waitType = PSP_WAIT_SLEEP;

	        // Wait forever (another thread will call sceKernelWakeupThread)
	        hleKernelThreadWait(currentThread, currentThread.wait, 0, true);

	        hleChangeThreadState(currentThread, PSP_THREAD_WAITING);

	        Emulator.getProcessor().cpu.gpr[2] = 0;
	        hleRescheduleCurrentThread(doCallbacks);
    	}
    }

    /** sleep the current thread (using wait) */
    public void sceKernelSleepThread() {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelSleepThread SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "'");
    	}

        hleKernelSleepThread(false);
    }

    /** sleep the current thread and handle callbacks (using wait)
     * in our implementation we have to use wait, not suspend otherwise we don't handle callbacks. */
    public void sceKernelSleepThreadCB() {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "'");
    	}

        hleKernelSleepThread(true);
        checkCallbacks();
    }

    public void sceKernelWakeupThread(int uid) {
        if (!checkThreadID(uid)) return;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (thread.status != PSP_THREAD_WAITING) {
            Modules.log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' not sleeping/waiting (status=0x" + Integer.toHexString(thread.status) + "), incrementing wakeupCount");
            thread.wakeupCount++;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not waking up");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
        	}
            hleChangeThreadState(thread, PSP_THREAD_READY);

            Emulator.getProcessor().cpu.gpr[2] = 0;

            // switch in the target thread if it's now higher priority
            if (thread.currentPriority < currentThread.currentPriority) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceKernelWakeupThread yielding to thread with higher priority");
            	}
                hleRescheduleCurrentThread();
            }
        }
    }

    public void sceKernelSuspendThread(int uid) {
        if (!checkThreadID(uid)) return;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (uid == currentThread.uid) {
            Modules.log.warn("sceKernelSuspendThread on self is not allowed");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_THREAD;
        } else {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid));
        	}
            hleChangeThreadState(thread, PSP_THREAD_SUSPEND);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelResumeThread(int uid) {
        if (!checkThreadID(uid)) return;
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
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
        	}
            hleChangeThreadState(thread, PSP_THREAD_READY);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private void hleKernelWaitThreadEnd(int uid, int micros, boolean forever, boolean callbacks) {
        if (!checkThreadID(uid)) return;
        if (Modules.log.isDebugEnabled()) {
	        Modules.log.debug("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid)
	            + " micros=" + micros
	            + " forever=" + forever
	            + " callbacks=" + callbacks);
        }

        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("hleKernelWaitThreadEnd unknown thread 0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            Modules.log.warn("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else if (thread.status == PSP_THREAD_STOPPED) {
            Modules.log.debug("hleKernelWaitThreadEnd SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' thread already stopped, not waiting");
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else {
            // wait type
            currentThread.waitType = PSP_WAIT_THREAD_END;

            // Go to wait state
            hleKernelThreadWait(currentThread, currentThread.wait, micros, forever);

            // Wait on a specific thread end
            currentThread.wait.waitingOnThreadEnd = true;
            currentThread.wait.ThreadEnd_id = uid;

            hleChangeThreadState(currentThread, PSP_THREAD_WAITING);

            hleRescheduleCurrentThread(callbacks);
        }
    }

    public void sceKernelWaitThreadEnd(int uid, int timeout_addr) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelWaitThreadEnd redirecting to hleKernelWaitThreadEnd(callbacks=false)");
    	}

        int micros = 0;
        boolean forever = true;

        if (timeout_addr != 0) { // psp does not check for valid address here
            micros = Memory.getInstance().read32(timeout_addr);
            forever = false;
        }

        hleKernelWaitThreadEnd(uid, micros, forever, false);
    }

    public void setEnableWaitThreadEndCB(boolean enable) {
        enableWaitThreadEndCB = enable;
        Modules.log.info("WaitThreadEndCB enabled: " + enableWaitThreadEndCB);
    }

    // disable in TOE (and many other games) until we get better MPEG support
    public void sceKernelWaitThreadEndCB(int uid, int timeout_addr) {
        if (enableWaitThreadEndCB) {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelWaitThreadEndCB redirecting to hleKernelWaitThreadEnd(callbacks=true)");
        	}

            int micros = 0;
            boolean forever = true;

            if (timeout_addr != 0) { // psp does not check for address inside a valid range, just 0 or not 0
                micros = Memory.getInstance().read32(timeout_addr);
                forever = false;
            }

            hleKernelWaitThreadEnd(uid, micros, forever, true);
            checkCallbacks();
        } else {
            Modules.log.warn("IGNORING:sceKernelWaitThreadEndCB - enable in settings if you know what you're doing");
        }
    }

    public void hleKernelThreadWait(SceKernelThreadInfo thread, ThreadWaitInfo wait, int micros, boolean forever) {
        wait.forever = forever;
        wait.micros = micros; // for debugging
        if (forever) {
            wait.microTimeTimeout = 0;
        	wait.waitTimeoutAction = null;
        } else {
            wait.microTimeTimeout = Emulator.getClock().microTime() + micros;
        	wait.waitTimeoutAction = new TimeoutThreadAction(thread);
        }

        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled() && !isIdleThread(thread)) {
            Modules.log.debug("-------------------- hleKernelThreadWait micros=" + micros + " forever:" + forever + " thread:'" + thread.name + "' caller:" + getCallingFunction());
        }
    }

    public void hleKernelDelayThread(int micros, boolean doCallbacks) {
    	hleKernelDelayThread(micros, doCallbacks, 0);
    }

    public void hleKernelDelayThread(int micros, boolean doCallbacks, int returnCode) {
        // wait type
        currentThread.waitType = PSP_WAIT_DELAY;

        if (IGNORE_DELAY)
            micros = 0;

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("hleKernelDelayThread micros=" + micros + ", callbacks=" + doCallbacks);
        }

        // Wait on a timeout only
        hleKernelThreadWait(currentThread, currentThread.wait, micros, false);

        hleChangeThreadState(currentThread, PSP_THREAD_WAITING);

        Emulator.getProcessor().cpu.gpr[2] = returnCode;

        hleRescheduleCurrentThread(doCallbacks);
    }

    /** wait the current thread for a certain number of microseconds */
    public void sceKernelDelayThread(int micros) {
        hleKernelDelayThread(micros, false);
    }

    /** wait the current thread for a certain number of microseconds */
    public void sceKernelDelayThreadCB(int micros) {
        hleKernelDelayThread(micros, true);
    }

    /**
     * Delay the current thread by a specified number of sysclocks
     *
     * @param sysclocks_addr - Address of delay in sysclocks
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelDelaySysClockThread(int sysclocks_addr) {
    	Memory mem = Memory.getInstance();
    	if (mem.isAddressGood(sysclocks_addr)) {
    		long sysclocks = mem.read64(sysclocks_addr);
    		int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
            hleKernelDelayThread(micros, false);
    	} else {
            Modules.log.warn("sceKernelDelaySysClockThread invalid sysclocks address 0x" + Integer.toHexString(sysclocks_addr));
            Emulator.getProcessor().cpu.gpr[2] = -1;
    	}
    }

    /**
     * Delay the current thread by a specified number of sysclocks handling callbacks
     *
     * @param sysclocks_addr - Address of delay in sysclocks
     *
     * @return 0 on success, < 0 on error
     *
     */
    public void sceKernelDelaySysClockThreadCB(int sysclocks_addr) {
    	Memory mem = Memory.getInstance();
    	if (mem.isAddressGood(sysclocks_addr)) {
            long sysclocks = mem.read64(sysclocks_addr);
    		int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
            hleKernelDelayThread(micros, true);
    	} else {
            Modules.log.warn("sceKernelDelaySysClockThreadCB invalid sysclocks address 0x" + Integer.toHexString(sysclocks_addr));
            Emulator.getProcessor().cpu.gpr[2] = -1;
    	}
    }

    public SceKernelCallbackInfo hleKernelCreateCallback(String name, int func_addr, int user_arg_addr) {
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, currentThread.uid, func_addr, user_arg_addr);
        callbackMap.put(callback.uid, callback);

        Modules.log.debug("hleKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid)
            + " name:'" + name
            + "' PC=" + Integer.toHexString(func_addr)
            + " arg=" + Integer.toHexString(user_arg_addr)
            + " thread:'" + currentThread.name + "'");

        return callback;
    }

    public void sceKernelCreateCallback(int name_addr, int func_addr, int user_arg_addr) {
        String name = readStringNZ(name_addr, 32);
        SceKernelCallbackInfo callback = hleKernelCreateCallback(name, func_addr, user_arg_addr);
        Emulator.getProcessor().cpu.gpr[2] = callback.uid;
    }

    /** @return true if successful. */
    public boolean hleKernelDeleteCallback(int uid) {
        SceKernelCallbackInfo info = callbackMap.remove(uid);
        boolean removed = info != null;
        if (removed) {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("hleKernelDeleteCallback SceUID=" + Integer.toHexString(uid)
        				+ " name:'" + info.name + "'");
        	}
        } else {
            Modules.log.warn("hleKernelDeleteCallback not a callback uid 0x" + Integer.toHexString(uid));
        }

        return removed;
    }

    public void sceKernelDeleteCallback(int uid) {
        if (hleKernelDeleteCallback(uid)) {
            // TODO automatically unregister the callback if it was registered with another system?
            // example: sceKernelDeleteCallback called before sceUmdUnRegisterUMDCallBack
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    /** Check callbacks, including those on the current thread */
    public void sceKernelCheckCallback() {
        Modules.log.debug("sceKernelCheckCallback");
        Emulator.getProcessor().cpu.gpr[2] = 0;
        currentThread.doCallbacks = true;
        checkCallbacks();
    }

    public void sceKernelReferCallbackStatus(int uid, int info_addr) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelReferCallbackStatus SceUID=" + Integer.toHexString(uid)
    				+ " info=" + Integer.toHexString(info_addr));
    	}

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
            if (size == SceKernelCallbackInfo.size) {
                info.write(mem, info_addr);
                Emulator.getProcessor().cpu.gpr[2] = 0;
            } else {
                Modules.log.warn("sceKernelReferCallbackStatus bad info size got " + size + " want " + SceKernelCallbackInfo.size);
                Emulator.getProcessor().cpu.gpr[2] = -1;
            }
        }
    }

    /** Get the current thread Id */
    public void sceKernelGetThreadId() {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelGetThreadId returning uid=0x" + Integer.toHexString(currentThread.uid));
    	}
        Emulator.getProcessor().cpu.gpr[2] = currentThread.uid;
    }

    public void sceKernelGetThreadCurrentPriority() {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelGetThreadCurrentPriority returning currentPriority=" + currentThread.currentPriority);
    	}
        Emulator.getProcessor().cpu.gpr[2] = currentThread.currentPriority;
    }

    /** @return ERROR_NOT_FOUND_THREAD on uid < 0, uid == 0 and thread not found */
    public void sceKernelGetThreadExitStatus(int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelGetThreadExitStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else  {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelGetThreadExitStatus uid=0x" + Integer.toHexString(uid) + " exitStatus=0x" + Integer.toHexString(thread.exitStatus));
        	}
            Emulator.getProcessor().cpu.gpr[2] = thread.exitStatus;
        }
    }

    private int getThreadCurrentStackSize() {
        int size = currentThread.stackSize - (Emulator.getProcessor().cpu.gpr[29] - currentThread.stack_addr) - 0x130;
        if (size < 0) {
            size = 0;
        }

        return size;
    }

    /** @return amount of free stack space.
     * TODO this isn't quite right */
    public void sceKernelCheckThreadStack() {
        int size = getThreadCurrentStackSize();
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug(String.format("sceKernelCheckThreadStack returning size=0x%X", size));
    	}

        Emulator.getProcessor().cpu.gpr[2] = size;
    }

    /** @return amount of free stack space? up to 0x1000 lower?
     * TODO this isn't quite right */
    public void sceKernelGetThreadStackFreeSize(int uid) {
        if (uid == 0) uid = currentThread.uid;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelGetThreadStackFreeSize unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else  {
            int size = getThreadCurrentStackSize() - 0xfb0;
            if (size < 0) {
                size = 0;
            }
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug(String.format("sceKernelGetThreadStackFreeSize returning size=0x%X", size));
        	}
            Emulator.getProcessor().cpu.gpr[2] = size;
        }
    }

    public void sceKernelReferThreadStatus(int uid, int addr) {
        //Get the status information for the specified thread
        if (uid == 0) uid = currentThread.uid;
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelReferThreadStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else  {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelReferThreadStatus uid=0x" + Integer.toHexString(uid) + " addr=0x" + Integer.toHexString(addr) + " thread=" + thread);
        	}
            thread.write(Memory.getInstance(), addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** Write uid's to buffer
     * return written count
     * save full count to idcount_addr */
    public void sceKernelGetThreadmanIdList(int type,
        int readbuf_addr, int readbufsize, int idcount_addr) {
        Memory mem = Memory.getInstance();

        if (Modules.log.isDebugEnabled()) {
	        Modules.log.debug("sceKernelGetThreadmanIdList type=" + type
	            + " readbuf:0x" + Integer.toHexString(readbuf_addr)
	            + " readbufsize:" + readbufsize
	            + " idcount:0x" + Integer.toHexString(idcount_addr));
        }

        int saveCount = 0;
        int fullCount = 0;

        if (type == SCE_KERNEL_TMID_Thread){
            for (SceKernelThreadInfo thread : threadMap.values()) {
                // Hide kernel mode threads when called from a user mode thread
                if (userThreadCalledKernelCurrentThread(thread)) {
                    if (saveCount < readbufsize) {
                    	if (Modules.log.isDebugEnabled()) {
                    		Modules.log.debug("sceKernelGetThreadmanIdList adding thread '" + thread.name + "'");
                    	}
                        mem.write32(readbuf_addr + saveCount * 4, thread.uid);
                        saveCount++;
                    } else {
                        Modules.log.warn("sceKernelGetThreadmanIdList NOT adding thread '" + thread.name + "' (no more space)");
                    }
                    fullCount++;
                }
            }
        } else {
            Modules.log.warn("UNIMPLEMENTED:sceKernelGetThreadmanIdList type=" + type);
        }

        if (mem.isAddressGood(idcount_addr)) {
            mem.write32(idcount_addr, fullCount);
        }

        Emulator.getProcessor().cpu.gpr[2] = saveCount;
    }

    private boolean threadCanNotCallback(SceKernelThreadInfo thread, int callbackType, int cbid, SceKernelCallbackInfo cb) {
        return !thread.callbackRegistered[callbackType] || (cbid != -1 && cb.uid != cbid);
    }

    private boolean userCurrentThreadTryingToSwitchToKernelMode(int newAttr) {
        return (currentThread.attr & PSP_THREAD_ATTR_USER) == PSP_THREAD_ATTR_USER && (newAttr & PSP_THREAD_ATTR_USER) != PSP_THREAD_ATTR_USER;
    }

    private boolean userThreadCalledKernelCurrentThread(SceKernelThreadInfo thread) {
        return !isIdleThread(thread) &&
                ((thread.attr & PSP_THREAD_ATTR_KERNEL) != PSP_THREAD_ATTR_KERNEL ||
                (currentThread.attr & PSP_THREAD_ATTR_KERNEL) == PSP_THREAD_ATTR_KERNEL);
    }

    public void sceKernelChangeThreadPriority(int uid, int priority) {
        if (uid == 0) uid = currentThread.uid;
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid)
                + " newPriority:0x" + Integer.toHexString(priority) + ") unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;

        } else if ((thread.status & PSP_THREAD_STOPPED) == PSP_THREAD_STOPPED) {
            Modules.log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid)
                + " newPriority:0x" + Integer.toHexString(priority)
                + " oldPriority:0x" + Integer.toHexString(thread.currentPriority)
                + " thread is stopped, ignoring");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_THREAD_ALREADY_DORMANT;

        // Don't do anything when new priority is 0
        } else if (priority == 0) {
            Modules.log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid)
                + " newPriority:0x" + Integer.toHexString(priority)
                + " oldPriority:0x" + Integer.toHexString(thread.currentPriority)
                + " newPriority is 0, ignoring");
            Emulator.getProcessor().cpu.gpr[2] = 0;

        // TODO check whether it affects kernel threads (probably doesn't)
        } else if (priority < 0x08 || priority >= 0x78) {
            Modules.log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid)
                + " newPriority:0x" + Integer.toHexString(priority)
                + " oldPriority:0x" + Integer.toHexString(thread.currentPriority)
                + " newPriority is outside of valid range");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_ILLEGAL_PRIORITY;

        } else {
        	if (Modules.log.isDebugEnabled()) {
	            Modules.log.debug("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid)
	                + " newPriority:0x" + Integer.toHexString(priority)
	                + " oldPriority:0x" + Integer.toHexString(thread.currentPriority));
        	}

            int oldPriority = thread.currentPriority;
            thread.currentPriority = priority;

            Emulator.getProcessor().cpu.gpr[2] = 0;

            // switch in the target thread if it's now higher priority
            if ((thread.status & PSP_THREAD_READY) == PSP_THREAD_READY &&
                priority < currentThread.currentPriority) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceKernelChangeThreadPriority yielding to thread with higher priority");
            	}
                hleRescheduleCurrentThread();
            } else if (uid == currentThread.uid && priority > oldPriority) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceKernelChangeThreadPriority rescheduling");
            	}
                // yield if we moved ourself to lower priority
                hleRescheduleCurrentThread();
            }
        }
    }

    public void sceKernelChangeCurrentThreadAttr(int removeAttr, int addAttr) {
    	if (Modules.log.isDebugEnabled()) {
	        Modules.log.debug("sceKernelChangeCurrentThreadAttr"
	                + " removeAttr:0x" + Integer.toHexString(removeAttr)
	                + " addAttr:0x" + Integer.toHexString(addAttr)
	                + " oldAttr:0x" + Integer.toHexString(currentThread.attr));
    	}

        // Probably meant to be sceKernelChangeThreadAttr unknown=uid
        if (removeAttr != 0) {
            Modules.log.warn("sceKernelChangeCurrentThreadAttr removeAttr:0x" + Integer.toHexString(removeAttr) + " non-zero");
        }

        int newAttr = (currentThread.attr & ~removeAttr) | addAttr;
        // Don't allow switching into kernel mode!
        if (userCurrentThreadTryingToSwitchToKernelMode(newAttr)) {
            Modules.log.debug("sceKernelChangeCurrentThreadAttr forcing user mode");
            newAttr |= PSP_THREAD_ATTR_USER;
        }
        currentThread.attr = newAttr;
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    /**
     * Rotate thread ready queue at a set priority
     *
     * @param priority - The priority of the queue
     *
     * @return 0 on success, < 0 on error.
     */
    public void sceKernelRotateThreadReadyQueue(int priority) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelRotateThreadReadyQueue priority=" + priority);
    	}

    	if (priority == 0) {
    		priority = currentThread.currentPriority;
    	}

        synchronized (readyThreads) {
            for (Iterator<SceKernelThreadInfo> it = readyThreads.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.currentPriority == priority) {
                    if (currentThread.currentPriority == priority) {
                        // We are rotating the ThreadReadyQueue of the currentThread,
                        // we can just yield the current thread.
                        // TODO Check if this should yield only to a thread of the same priority or also to a thread with higher priority
                    	hleRescheduleCurrentThread();
                    } else {
                        // Move the thread to the end of the list to avoid thread starvation on nextThread()
                        it.remove();
                        readyThreads.addLast(thread);
                    }
                    break;
                }
            }
        }
    }

    private int getDispatchThreadState() {
    	return dispatchThreadEnabled ? SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED : SCE_KERNEL_DISPATCHTHREAD_STATE_DISABLED;
    }

    /**
     * Suspend the dispatch thread
     *
     * @return The current state of the dispatch thread, < 0 on error
     */
    public void sceKernelSuspendDispatchThread() {
    	int state = getDispatchThreadState();
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelSuspendDispatchThread() state=" + state);
    	}
    	dispatchThreadEnabled = false;
        Emulator.getProcessor().cpu.gpr[2] = state;
    }

    /**
     * Resume the dispatch thread
     *
     * @param state - The state of the dispatch thread
	 *                (from sceKernelSuspendDispatchThread)
     * @return 0 on success, < 0 on error
     */
    public void sceKernelResumeDispatchThread(int state) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelResumeDispatchThread(state=" + state + ")");
    	}
    	if (state == SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED) {
    		dispatchThreadEnabled = true;
    	}
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    /**
     * Get the current system status.
     *
     * @param status - Pointer to a ::SceKernelSystemStatus structure.
     *
     * @return < 0 on error.
     */
    public void sceKernelReferSystemStatus(int statusAddr) {
    	Memory mem = Memory.getInstance();
    	if (mem.isAddressGood(statusAddr)) {
	    	SceKernelSystemStatus status = new SceKernelSystemStatus();
	    	status.read(mem, statusAddr);
	    	status.status = 0;
	    	status.write(mem, statusAddr);
	    	Emulator.getProcessor().cpu.gpr[2] = 0;
    	} else {
	    	Emulator.getProcessor().cpu.gpr[2] = -1;
    	}
    }

    public void sceKernelCancelWakeupThread(int uid) {
        if (uid == 0) uid = currentThread.uid;
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelCancelWakeupThread SceUID=" + Integer.toHexString(uid) + ") unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else {
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug("sceKernelCancelWakeupThread SceUID=" + Integer.toHexString(uid) + ") wakeupCount=" + thread.wakeupCount);
            }
            Emulator.getProcessor().cpu.gpr[2] = thread.wakeupCount;
            thread.wakeupCount = 0;
        }
    }

    public void sceKernelReleaseWaitThread(int uid) {
        if (uid == 0) uid = currentThread.uid;
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            Modules.log.warn("sceKernelReleaseWaitThread SceUID=" + Integer.toHexString(uid) + ") unknown thread");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_THREAD;
        } else if (thread.status != PSP_THREAD_WAITING) {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelReleaseWaitThread SceUID=" + Integer.toHexString(uid) + ") Thread not waiting: status=" + thread.status);
        	}
    		Emulator.getProcessor().cpu.gpr[2] = SceKernelErrors.ERROR_THREAD_IS_NOT_WAIT;
        } else {
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("sceKernelReleaseWaitThread SceUID=" + Integer.toHexString(uid) + ") releasing waiting thread: " + thread.toString());
        	}

    		Emulator.getProcessor().cpu.gpr[2] = 0;
    		thread.cpuContext.gpr[2] = SceKernelErrors.ERROR_WAIT_STATUS_RELEASED;
    		hleChangeThreadState(thread, PSP_THREAD_READY);

    		// Switch to the released thread if it has a higher priority
    		if (thread.currentPriority < currentThread.currentPriority) {
    			hleRescheduleCurrentThread();
    		}
        }
    }

    /** Registers a callback on the current thread.
     * @return true on success (the cbid was a valid callback uid) */
    public boolean hleKernelRegisterCallback(int callbackType, int cbid) {
        // Consistency check
        if (currentThread.callbackReady[callbackType]) {
            Modules.log.warn("hleKernelRegisterCallback(type=" + callbackType + ") ready=true");
        }

        SceKernelCallbackInfo callback = callbackMap.get(cbid);
        if (callback == null) {
            Modules.log.warn("hleKernelRegisterCallback(type=" + callbackType + ") unknown uid " + Integer.toHexString(cbid));
            return false;
        } else {
            currentThread.callbackRegistered[callbackType] = true;
            currentThread.callbackInfo[callbackType] = callback;
            return true;
        }
    }

    /** Unregisters a callback by type and cbid. May not be on the current thread.
     * @param callbackType See SceKernelThreadInfo.
     * @param cbid The UID of the callback to unregister.
     * @return SceKernelCallbackInfo of the removed callback, or null if it
     * couldn't be found. */
    public SceKernelCallbackInfo hleKernelUnRegisterCallback(int callbackType, int cbid) {
        SceKernelCallbackInfo oldInfo = null;

        for (SceKernelThreadInfo thread : threadMap.values()) {
            if (thread.callbackRegistered[callbackType] &&
                thread.callbackInfo[callbackType].uid == cbid) {
                // Warn if we are removing a pending callback, this a callback
                // that has been pushed but not yet executed.
                if (thread.callbackReady[callbackType]) {
                    Modules.log.warn("hleKernelUnRegisterCallback(type=" + callbackType + ") removing pending callback");
                }

                oldInfo = thread.callbackInfo[callbackType];
                thread.callbackRegistered[callbackType] = false;
                thread.callbackReady[callbackType] = false;
                thread.callbackInfo[callbackType] = null;
                break;
            }
        }

        if (oldInfo == null) {
            Modules.log.warn("hleKernelUnRegisterCallback(type=" + callbackType + ") cbid=" + Integer.toHexString(cbid)
                + " no matching callbacks found");
        }

        return oldInfo;
    }

    /** push callback to all threads */
    public void hleKernelNotifyCallback(int callbackType, int notifyArg) {
        hleKernelNotifyCallback(callbackType, -1, notifyArg);
    }

    /** @param cbid If cbid is -1, then push callback to all threads
     * if cbid is not -1 then only trigger that specific cbid provided it is
     * also of type callbackType. */
    public void hleKernelNotifyCallback(int callbackType, int cbid, int notifyArg) {
        boolean pushed = false;

        for (SceKernelThreadInfo thread : threadMap.values()) {
            SceKernelCallbackInfo cb = thread.callbackInfo[callbackType];

            if (threadCanNotCallback(thread, callbackType, cbid, cb)) {
                continue;
            }

            if (cb.notifyCount != 0) {
                Modules.log.warn("hleKernelNotifyCallback(type=" + callbackType
                    + ") thread:'" + thread.name
                    + "' overwriting previous notifyArg 0x" + Integer.toHexString(cb.notifyArg)
                    + " -> 0x" + Integer.toHexString(notifyArg)
                    + ", newCount=" + (cb.notifyCount + 1));
            }

            cb.notifyCount++; // keep increasing this until we actually enter the callback
            cb.notifyArg = notifyArg;
            thread.callbackReady[callbackType] = true;
            pushed = true;
        }

        if (pushed) {
            // Enter callbacks immediately,
            // except those registered to the current thread. The app must explictly
            // call sceKernelCheckCallback or a waitCB function to do that.
        	if (Modules.log.isDebugEnabled()) {
        		Modules.log.debug("hleKernelNotifyCallback(type=" + callbackType + ") calling checkCallbacks");
        	}
            checkCallbacks();
        } else {
            Modules.log.warn("hleKernelNotifyCallback(type=" + callbackType + ") no registered callbacks to push");
        }
    }

    /** runs callbacks. Check the thread doCallbacks flag.
     * @return true if we switched into a callback. */
    private boolean checkThreadCallbacks(SceKernelThreadInfo thread) {
    	boolean handled = false;

    	if (thread == null || !thread.doCallbacks) {
    		return handled;
    	}

    	for (int i = 0; i < SceKernelThreadInfo.THREAD_CALLBACK_SIZE; i++) {
            if (thread.callbackReady[i] && thread.callbackRegistered[i]) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug(String.format("Entering callback type %d %s for thread %s (current thread is %s)", i, thread.callbackInfo[i].toString(), thread.toString(), currentThread.toString()));
            	}

            	thread.callbackReady[i] = false;
        		thread.callbackInfo[i].startContext(thread, null);
                handled = true;
                break;
            }
        }

    	return handled;
    }

    /**
     * Iterates waiting threads, making sure doCallbacks is set before
     * checking for pending callbacks.
     * Handles sceKernelCheckCallback when doCallbacks is set on currentThread.
     * Handles redirects to yieldCB (from fake waitCB) on the thread that called waitCB.
     *
     * We currently call checkCallbacks() at the end of each waitCB function
     * since this has less overhead than checking on every step.
     *
     * Some trickery is used in yieldCurrentThreadCB(). By the time we get
     * inside the checkCallbacks() function the thread that called yieldCB is
     * no longer the current thread. Also the thread that called yieldCB is
     * not in the wait state (it's in the ready state). so what we do is check
     * every thread, not just the waiting threads for the doCallbacks flag.
     * Also the waitingThreads list only contains waiting threads that have a
     * finite wait period, so we have to iterate on all threads anyway.
     *
     * It is probably unsafe to call contextSwitch() when insideCallback is true.
     * insideCallback may become true after a call to checkCallbacks().
     */
    private void checkCallbacks() {
    	if (Modules.log.isTraceEnabled()) {
    		Modules.log.trace("checkCallbacks current thread is '" + currentThread.name + "' doCallbacks:" + currentThread.doCallbacks + " caller:" + getCallingFunction());
    	}

    	boolean handled;
    	do {
    		handled = false;
	    	for (SceKernelThreadInfo thread : threadMap.values()) {
	    		if (thread.doCallbacks && checkThreadCallbacks(thread)) {
	    			handled = true;
	    			break;
	    		}
	    	}
    	} while (handled);
    }

    public static class Statistics {
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

        private static class ThreadStatistics {
            public String name;
            public long runClocks;
        }
    }

	private static class CallbackManager {
		private Map<Integer, Callback> callbacks;
		private int currentCallbackId;

		public void Initialize() {
			callbacks = new HashMap<Integer, Callback>();
			currentCallbackId = 1;
		}

		public void addCallback(Callback callback) {
			callbacks.put(callback.getId(), callback);
		}

		public Callback remove(int id) {
			Callback callback = callbacks.remove(id);

			return callback;
		}

		public int getNewCallbackId() {
			return currentCallbackId++;
		}
	}

	private static class Callback {
		private int id;
		private int savedIdRegister;
		private int savedRa;
		private int savedPc;
		private IAction afterAction;

		public Callback(int id, int savedIdRegister, int savedRa, int savedPc, IAction afterAction) {
			this.id = id;
			this.savedIdRegister = savedIdRegister;
			this.savedRa = savedRa;
			this.savedPc = savedPc;
			this.afterAction = afterAction;
		}

		public IAction getAfterAction() {
			return afterAction;
		}

		public void setAfterAction(IAction afterAction) {
			this.afterAction = afterAction;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getSavedIdRegister() {
			return savedIdRegister;
		}

		public void setSavedIdRegister(int savedIdRegister) {
			this.savedIdRegister = savedIdRegister;
		}

		public int getSavedRa() {
			return savedRa;
		}

		public void setSavedRa(int savedRa) {
			this.savedRa = savedRa;
		}

		public int getSavedPc() {
			return savedPc;
		}

		public void setSavedPc(int savedPc) {
			this.savedPc = savedPc;
		}

		@Override
		public String toString() {
			return String.format("Callback id=%d,savedIdReg=0x%08X,savedPc=0x%08X", getId(), getSavedIdRegister(), getSavedPc());
		}
	}

	private class AfterCallAction implements IAction {
		SceKernelThreadInfo thread;
		int status;
		int waitType;
		int waitId;
		ThreadWaitInfo threadWaitInfo;
		boolean doCallback;
		IAction afterAction;

		public AfterCallAction(SceKernelThreadInfo thread, int status, int waitType, int waitId, ThreadWaitInfo threadWaitInfo, boolean doCallback, IAction afterAction) {
			this.thread = thread;
			this.status = status;
			this.waitType = waitType;
			this.waitId = waitId;
			this.threadWaitInfo = new ThreadWaitInfo();
			this.threadWaitInfo.copy(threadWaitInfo);
			this.doCallback = doCallback;
			this.afterAction = afterAction;
		}

		@Override
		public void execute() {
			// Restore the wait state of the thread
			thread.waitType = waitType;
			thread.waitId = waitId;
			thread.wait.copy(threadWaitInfo);

			hleChangeThreadState(thread, status);
			hleRescheduleCurrentThread(doCallback);

			if (afterAction != null) {
				afterAction.execute();
			}
		}
	}

	public class TimeoutThreadAction implements IAction {
		private SceKernelThreadInfo thread;

		public TimeoutThreadAction(SceKernelThreadInfo thread) {
			this.thread = thread;
		}

		@Override
		public void execute() {
			hleThreadWaitTimeout(thread);
		}
	}

	public class DeleteThreadAction implements IAction {
		private SceKernelThreadInfo thread;

		public DeleteThreadAction(SceKernelThreadInfo thread) {
			this.thread = thread;
		}

		@Override
		public void execute() {
			hleDeleteThread(thread);
		}
	}
}