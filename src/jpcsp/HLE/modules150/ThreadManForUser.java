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
package jpcsp.HLE.modules150;

import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._sp;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._v1;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_PRIORITY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_THREAD;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_ALARM;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_VTIMER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_ALREADY_DORMANT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_ALREADY_SUSPEND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_IS_NOT_SUSPEND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_THREAD_IS_TERMINATED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadEventHandlerInfo.THREAD_EVENT_CREATE;
import static jpcsp.HLE.kernel.types.SceKernelThreadEventHandlerInfo.THREAD_EVENT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelThreadEventHandlerInfo.THREAD_EVENT_EXIT;
import static jpcsp.HLE.kernel.types.SceKernelThreadEventHandlerInfo.THREAD_EVENT_START;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_BLOCKED;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_UMD;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_ATTR_KERNEL;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_ATTR_USER;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_RUNNING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_STOPPED;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_SUSPEND;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING_SUSPEND;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_DELAY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_EVENTFLAG;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MSGPIPE;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MUTEX;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_NONE;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_SLEEP;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_THREAD_END;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_FPL;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_LWMUTEX;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MBX;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_SEMA;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_VPL;
import static jpcsp.HLE.modules.HLEModuleManager.HLESyscallNid;
import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.writeStringZ;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.DumpDebugState;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelAlarmInfo;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelSystemStatus;
import jpcsp.HLE.kernel.types.SceKernelThreadEventHandlerInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceKernelVTimerInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo.RegisteredCallbacks;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Interrupts;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.scheduler.Scheduler;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;
import jpcsp.HLE.CheckArgument;;

/*
 * Thread scheduling on PSP:
 * - when a thread having a higher priority than the current thread, switches to the
 *   READY state, the current thread is interrupted immediately and is loosing the
 *   RUNNING state. The new thread then moves to the RUNNING state.
 * - when a thread having the same or a lower priority than the current thread,
 *   switches to the READY state, the current thread is not interrupted and is keeping
 *   the RUNNING state.
 * - a RUNNING thread can only yield to a thread having the same priority by calling
 *   sceKernelRotateThreadReadyQueue(0).
 * - the clock precision when interrupting a RUNNING thread is about 200 microseconds.
 *   i.e., it can take up to 200us when a high priority thread moves to the READY
 *   state before it changes to the RUNNING state.
 *
 * Thread scheduling on Jpcsp:
 * - the rules for moving between states are implemented in hleChangeThreadState()
 * - the rules for choosing the thread in the RUNNING state are implemented in
 *   hleRescheduleCurrentThread()
 * - the clock precision for interrupting a RUNNING thread is about 1000 microseconds.
 *   This is due to a restriction of the Java timers used by the Thread.sleep() methods.
 *   Even the Thread.sleep(millis, nanos) seems to have the same restriction
 *   (at least on windows).
 * - preemptive scheduling is implemented in RuntimeContext by a separate
 *   Java thread (RuntimeSyncThread). This thread sets the flag RuntimeContext.wantSync
 *   when a scheduler action has to be executed. This flag is checked by the compiled
 *   code at each back branch (i.e. a branch to a lower address, usually a loop).
 *
 * Test application:
 * - taskScheduler.prx: testing the scheduler rules between threads having higher,
 *                      lower or the same priority.
 *                      The clock precision of 200us on the PSP can be observed here.
 */
public class ThreadManForUser extends HLEModule {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    @Override
    public String getName() {
        return "ThreadManForUser";
    }

    private HashMap<Integer, SceKernelThreadInfo> threadMap;
    private HashMap<Integer, SceKernelThreadEventHandlerInfo> threadEventHandlerMap;
    private HashMap<Integer, Integer> threadEventMap;
    private LinkedList<SceKernelThreadInfo> readyThreads;
    private SceKernelThreadInfo currentThread;
    private SceKernelThreadInfo idle0,  idle1;
    public Statistics statistics;
    private boolean dispatchThreadEnabled;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_DISABLED = 0;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED = 1;

    // The PSP seems to have a resolution of 200us
    protected static final int THREAD_DELAY_MINIMUM_MICROS = 200;

    protected static final int CALLBACKID_REGISTER = 16; // $s0
    protected CallbackManager callbackManager = new CallbackManager();
    protected static final int IDLE_THREAD_ADDRESS = MemoryMap.START_RAM;
    public static final int THREAD_EXIT_HANDLER_ADDRESS = MemoryMap.START_RAM + 0x20;
    public static final int CALLBACK_EXIT_HANDLER_ADDRESS = MemoryMap.START_RAM + 0x30;
    public static final int ASYNC_LOOP_ADDRESS = MemoryMap.START_RAM + 0x40;
    public static final int NET_APCTL_LOOP_ADDRESS = MemoryMap.START_RAM + 0x60;
    private HashMap<Integer, SceKernelCallbackInfo> callbackMap;
    private boolean USE_THREAD_BANLIST = false;
    private static final boolean LOG_CONTEXT_SWITCHING = true;
    private static final boolean LOG_INSTRUCTIONS = false;
    public boolean exitCalled = false;

    // see sceKernelGetThreadmanIdList
    public final static int SCE_KERNEL_TMID_Thread = 1;
    public final static int SCE_KERNEL_TMID_Semaphore = 2;
    public final static int SCE_KERNEL_TMID_EventFlag = 3;
    public final static int SCE_KERNEL_TMID_Mbox = 4;
    public final static int SCE_KERNEL_TMID_Vpl = 5;
    public final static int SCE_KERNEL_TMID_Fpl = 6;
    public final static int SCE_KERNEL_TMID_Mpipe = 7;
    public final static int SCE_KERNEL_TMID_Callback = 8;
    public final static int SCE_KERNEL_TMID_ThreadEventHandler = 9;
    public final static int SCE_KERNEL_TMID_Alarm = 10;
    public final static int SCE_KERNEL_TMID_VTimer = 11;
    public final static int SCE_KERNEL_TMID_Mutex = 12;
    public final static int SCE_KERNEL_TMID_LwMutex = 13;
    public final static int SCE_KERNEL_TMID_SleepThread = 64;
    public final static int SCE_KERNEL_TMID_DelayThread = 65;
    public final static int SCE_KERNEL_TMID_SuspendThread = 66;
    public final static int SCE_KERNEL_TMID_DormantThread = 67;
    protected static final int INTR_NUMBER = IntrManager.PSP_SYSTIMER0_INTR;
    protected Map<Integer, SceKernelAlarmInfo> alarms;
    protected Map<Integer, SceKernelVTimerInfo> vtimers;
    protected boolean needThreadReschedule;
    protected WaitThreadEndWaitStateChecker waitThreadEndWaitStateChecker;
    protected TimeoutThreadWaitStateChecker timeoutThreadWaitStateChecker;

	private class EnableThreadBanningSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setThreadBanningEnabled(value);
		}
	}

    public ThreadManForUser() {
    }

    @Override
    public void start() {
        threadMap = new HashMap<Integer, SceKernelThreadInfo>();
        threadEventMap = new HashMap<Integer, Integer>();
        threadEventHandlerMap = new HashMap<Integer, SceKernelThreadEventHandlerInfo>();
        readyThreads = new LinkedList<SceKernelThreadInfo>();
        statistics = new Statistics();

        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();
        callbackManager.Initialize();

        install_idle_threads();
        install_thread_exit_handler();
        install_callback_exit_handler();
        install_async_loop_handler();
        install_net_apctl_loop_handler();

        alarms = new HashMap<Integer, SceKernelAlarmInfo>();
        vtimers = new HashMap<Integer, SceKernelVTimerInfo>();

        dispatchThreadEnabled = true;
        needThreadReschedule = true;

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		if (threadMXBean.isThreadCpuTimeSupported()) {
			threadMXBean.setThreadCpuTimeEnabled(true);
		}

		waitThreadEndWaitStateChecker = new WaitThreadEndWaitStateChecker();
		timeoutThreadWaitStateChecker = new TimeoutThreadWaitStateChecker();

		setSettingsListener("emu.ignoreaudiothreads", new EnableThreadBanningSettingsListerner());

		super.start();
    }

    @Override
    public void stop() {
        alarms = null;
        vtimers = null;
        for (SceKernelThreadInfo thread : threadMap.values()) {
            terminateThread(thread);
        }

        super.stop();
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
    public void Initialise(SceModule module, int entry_addr, int attr, String pspfilename, int moduleid, boolean fromSyscall) {
        // Create a thread the program will run inside

        // The stack size seems to be 0x40000 when starting the application from the VSH
        // and smaller when starting the application with sceKernelLoadExec() - guess: 0x4000.
        // This could not be reproduced on a PSP.
        int rootStackSize = (fromSyscall ? 0x4000 : 0x40000);
        // Use the module_start_thread_stacksize when this information was present in the ELF file
        if (module != null && module.module_start_thread_stacksize > 0) {
            rootStackSize = module.module_start_thread_stacksize;
        }

        int rootInitPriority = 0x20;
        // Use the module_start_thread_priority when this information was present in the ELF file
        if (module != null && module.module_start_thread_priority > 0) {
            rootInitPriority = module.module_start_thread_priority;
        }
        currentThread = new SceKernelThreadInfo("root", entry_addr, rootInitPriority, rootStackSize, attr);
        currentThread.moduleid = moduleid;
        threadMap.put(currentThread.uid, currentThread);

        // Set user mode bit if kernel mode bit is not present
        if (!currentThread.isKernelMode()) {
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
    }

    private void install_idle_threads() {
        Memory mem = Memory.getInstance();

        // Generate 2 idle threads which can toggle between each other when there are no ready threads
        int instruction_addiu = // addiu a0, zr, 0
                ((AllegrexOpcodes.ADDIU & 0x3f) << 26) | ((0 & 0x1f) << 21) | ((4 & 0x1f) << 16);
        int instruction_lui = // lui ra, 0x08000000
                ((AllegrexOpcodes.LUI & 0x3f) << 26) | ((31 & 0x1f) << 16) | (0x0800 & 0x0000ffff);
        int instruction_jr = // jr ra
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.JR & 0x3f) | ((31 & 0x1f) << 21);
        int instruction_syscall = // syscall 0x0201c [sceKernelDelayThread]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName("sceKernelDelayThread").getSyscallCode() & 0x000fffff) << 6);
        

        // This memory is always reserved on a real PSP
        SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "ThreadMan-RootMem", SysMemUserForUser.PSP_SMEM_Addr, 0x4000, MemoryMap.START_USERSPACE);
        int reservedMem = info.addr;

        mem.write32(IDLE_THREAD_ADDRESS + 0, instruction_addiu);
        mem.write32(IDLE_THREAD_ADDRESS + 4, instruction_lui);
        mem.write32(IDLE_THREAD_ADDRESS + 8, instruction_jr);
        mem.write32(IDLE_THREAD_ADDRESS + 12, instruction_syscall);

        // lowest allowed priority is 0x77, so we are ok at 0x7f
        // Allocate a stack because interrupts can be processed by the
        // idle thread, using its stack.
        // The stack is allocated into the reservedMem area.
        idle0 = new SceKernelThreadInfo("idle0", IDLE_THREAD_ADDRESS | 0x80000000, 0x7f, 0, PSP_THREAD_ATTR_KERNEL);
        idle0.setSystemStack(reservedMem, 0x2000);
        idle0.reset();
        idle0.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        threadMap.put(idle0.uid, idle0);
        hleChangeThreadState(idle0, PSP_THREAD_READY);

        idle1 = new SceKernelThreadInfo("idle1", IDLE_THREAD_ADDRESS | 0x80000000, 0x7f, 0, PSP_THREAD_ATTR_KERNEL);
        idle1.setSystemStack(reservedMem + 0x2000, 0x2000);
        idle1.reset();
        idle1.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        threadMap.put(idle1.uid, idle1);
        hleChangeThreadState(idle1, PSP_THREAD_READY);
    }

    private void install_thread_exit_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f000 [hleKernelExitThread]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName("hleKernelExitThread").getSyscallCode() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void install_callback_exit_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f001 [hleKernelExitCallback]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName("hleKernelExitCallback").getSyscallCode() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void install_async_loop_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f002 [hleKernelAsyncLoop]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName("hleKernelAsyncLoop").getSyscallCode() & 0x000fffff) << 6);

        int instruction_b = (AllegrexOpcodes.BEQ << 26) | 0xFFFE; // branch back to syscall
        int instruction_nop = (AllegrexOpcodes.SLL << 26); // nop

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(ASYNC_LOOP_ADDRESS + 0, instruction_syscall);
        mem.write32(ASYNC_LOOP_ADDRESS + 4, instruction_b);
        mem.write32(ASYNC_LOOP_ADDRESS + 8, instruction_nop);
        mem.write32(ASYNC_LOOP_ADDRESS + 12, instruction_jr);
        mem.write32(ASYNC_LOOP_ADDRESS + 16, instruction_nop);
    }

    private void install_net_apctl_loop_handler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f002 [hleKernelAsyncLoop]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName("hleKernelNetApctlLoop").getSyscallCode() & 0x000fffff) << 6);

        int instruction_b = (AllegrexOpcodes.BEQ << 26) | 0xFFFE; // branch back to syscall
        int instruction_nop = (AllegrexOpcodes.SLL << 26); // nop

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(NET_APCTL_LOOP_ADDRESS + 0, instruction_syscall);
        mem.write32(NET_APCTL_LOOP_ADDRESS + 4, instruction_b);
        mem.write32(NET_APCTL_LOOP_ADDRESS + 8, instruction_nop);
        mem.write32(NET_APCTL_LOOP_ADDRESS + 12, instruction_jr);
        mem.write32(NET_APCTL_LOOP_ADDRESS + 16, instruction_nop);
    }

    /** to be called when exiting the emulation */
    public void exit() {
        exitCalled = true;

        if (threadMap != null) {
            // Delete all the threads to collect statistics
            deleteAllThreads();

            log.info("----------------------------- ThreadMan exit -----------------------------");

            if (DurationStatistics.collectStatistics) {
            	statistics.exit();
	            log.info(String.format("ThreadMan Statistics (%,d cycles in %.3fs):", statistics.allCycles, statistics.getDurationMillis() / 1000.0));
	            Collections.sort(statistics.threads);
	            for (Statistics.ThreadStatistics threadStatistics : statistics.threads) {
	                double percentage = 0;
	                if (statistics.allCycles != 0) {
	                    percentage = (threadStatistics.runClocks / (double) statistics.allCycles) * 100;
	                }
	                log.info(String.format("    Thread %-30s %,12d cycles (%5.2f%%)", threadStatistics.getQuotedName(), threadStatistics.runClocks, percentage));
	            }
            }
        }
    }

    /** To be called from the main emulation loop
     *  This is only used when running in interpreter mode,
     *  i.e. it is no longer used when the Compiler is enabled.
     */
    public void step() {
        if (LOG_INSTRUCTIONS) {
            if (log.isTraceEnabled()) {
                CpuState cpu = Emulator.getProcessor().cpu;

                if (!isIdleThread(currentThread) && cpu.pc != 0) {
                    int address = cpu.pc - 4;
                    int opcode = Memory.getInstance().read32(address);
                    log.trace(String.format("Executing %08X %s", address, Decoder.instruction(opcode).disasm(address, opcode)));
                }
            }
        }

        if (currentThread != null) {
            currentThread.runClocks++;
        } else if (!exitCalled) {
            // We always need to be in a thread! we shouldn't get here.
            log.error("No ready threads!");
        }
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

            if (LOG_CONTEXT_SWITCHING && log.isDebugEnabled() && !isIdleThread(newThread)) {
                log.debug("---------------------------------------- SceUID=" + Integer.toHexString(newThread.uid) + " name:'" + newThread.name + "'");
            }
        } else {
            // When running under compiler mode this gets triggered by exit()
            if (!exitCalled) {
                DumpDebugState.dumpDebugState();

                log.info("No ready threads - pausing emulator. caller:" + getCallingFunction());
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
            }
        }

        currentThread = newThread;

        RuntimeContext.update();
    }

    /** @param newThread The thread to switch in. */
    private boolean contextSwitch(SceKernelThreadInfo newThread) {
        if (IntrManager.getInstance().isInsideInterrupt()) {
            // No context switching inside an interrupt
            if (log.isDebugEnabled()) {
                log.debug("Inside an interrupt, not context switching to " + newThread);
            }
            return false;
        }

        if (Interrupts.isInterruptsDisabled()) {
            // No context switching when interrupts are disabled
            if (log.isDebugEnabled()) {
                log.debug("Interrupts are disabled, not context switching to " + newThread);
            }
            return false;
        }

        if (!dispatchThreadEnabled) {
            log.info("DispatchThread disabled, not context switching to " + newThread);
            return false;
        }

        internalContextSwitch(newThread);

        checkThreadCallbacks(currentThread);

        executePendingCallbacks(currentThread);

        return true;
    }

    private void executePendingCallbacks(SceKernelThreadInfo thread) {
        if (!thread.pendingCallbacks.isEmpty()) {
        	if (RuntimeContext.canExecuteCallback(thread)) {
	    		Callback callback = thread.pendingCallbacks.poll();
	        	if (log.isDebugEnabled()) {
	        		log.debug(String.format("Executing pending callback '%s' for thread '%s'", callback, thread));
	        	}
	    		callback.execute(thread);
        	}
        }
    }

    public void checkPendingCallbacks() {
    	executePendingCallbacks(currentThread);
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
     * If the current thread is having the same priority as the highest priority,
     * nothing is changed (no yielding to threads having the same priority).
     */
    public void hleRescheduleCurrentThread() {
    	if (needThreadReschedule) {
	        SceKernelThreadInfo newThread = nextThread();
	        if (newThread != null &&
	                (currentThread == null ||
	                currentThread.status != PSP_THREAD_RUNNING ||
	                currentThread.currentPriority > newThread.currentPriority)) {
	            if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
	                log.debug("Context switching to '" + newThread + "' after reschedule");
	            }

	            if (contextSwitch(newThread)) {
	            	needThreadReschedule = false;
	            }
	        } else {
	        	needThreadReschedule = false;
	        }
    	}
    }

    /**
     * Same behavior as hleRescheduleCurrentThread()
     * excepted that it executes callbacks when doCallbacks == true
     */
    public void hleRescheduleCurrentThread(boolean doCallbacks) {
    	SceKernelThreadInfo thread = currentThread;
        if (doCallbacks) {
            if (thread != null) {
            	thread.doCallbacks = doCallbacks;
            }
            checkCallbacks();
        }

        hleRescheduleCurrentThread();

        if (currentThread == thread && doCallbacks) {
        	if (thread.isRunning()) {
        		thread.doCallbacks = false;
        	}
        }
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
    	return currentThread.isKernelMode();
    }

    public String getThreadName(int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            return "NOT A THREAD";
        }
        return thread.name;
    }

    public boolean isThreadBlocked(SceKernelThreadInfo thread) {
    	return thread.isWaitingForType(JPCSP_WAIT_BLOCKED);
    }

    public boolean isDispatchThreadEnabled() {
    	return dispatchThreadEnabled;
    }

    public void hleBlockCurrentThread() {
        hleBlockCurrentThread(null);
    }

    public SceKernelCallbackInfo hleKernelReferCallbackStatus(int uid) {
    	return callbackMap.get(uid);
    }

    /**
     * Enter the current thread in a wait state.
     *
     * @param waitType         the wait type (one of SceKernelThreadInfo.PSP_WAIT_xxx)
     * @param waitId           the uid of the wait object
     * @param waitStateChecker this wait state checked will be called after the
     *                         execution of a callback in the waiting thread to check
     *                         if the thread has to return to its wait state (i.e. if
     *                         the wait condition is still valid).
     * @param timeoutAddr      0 when the thread is waiting forever
     *                         otherwise, a valid address containing a timeout value
     *                         in microseconds.
     * @param callbacks        true if callback can be executed while waiting.
     *                         false if callback cannot be execute while waiting.
     */
    public void hleKernelThreadEnterWaitState(int waitType, int waitId, IWaitStateChecker waitStateChecker, int timeoutAddr, boolean callbacks) {
        int micros = 0;
        boolean forever = true;
        if (Memory.isAddressGood(timeoutAddr)) {
            micros = Memory.getInstance().read32(timeoutAddr);
            forever = false;
        }
    	hleKernelThreadEnterWaitState(currentThread, waitType, waitId, waitStateChecker, micros, forever, callbacks);
    }

    /**
     * Enter the current thread in a wait state.
     * The thread will wait without timeout, i.e. forever.
     *
     * @param waitType         the wait type (one of SceKernelThreadInfo.PSP_WAIT_xxx)
     * @param waitId           the uid of the wait object
     * @param waitStateChecker this wait state checked will be called after the
     *                         execution of a callback in the waiting thread to check
     *                         if the thread has to return to its wait state (i.e. if
     *                         the wait condition is still valid).
     * @param callbacks        true if callback can be executed while waiting.
     *                         false if callback cannot be execute while waiting.
     */
    public void hleKernelThreadEnterWaitState(int waitType, int waitId, IWaitStateChecker waitStateChecker, boolean callbacks) {
    	hleKernelThreadEnterWaitState(currentThread, waitType, waitId, waitStateChecker, 0, true, callbacks);
    }

    /**
     * Enter a thread in a wait state.
     *
     * @param thread           the thread entering the wait state
     * @param waitType         the wait type (one of SceKernelThreadInfo.PSP_WAIT_xxx)
     * @param waitId           the uid of the wait object
     * @param waitStateChecker this wait state checked will be called after the
     *                         execution of a callback in the waiting thread to check
     *                         if the thread has to return to its wait state (i.e. if
     *                         the wait condition is still valid).
     * @param micros           a timeout value in microseconds, only relevant
     *                         when the "forever" parameter is false.
     * @param forever          true when the thread is waiting without a timeout,
     *                         false when the thread is waiting with a timeout
     *                         (see the "micros" parameter).
     * @param callbacks        true if callback can be executed while waiting.
     *                         false if callback cannot be execute while waiting.
     */
    public void hleKernelThreadEnterWaitState(SceKernelThreadInfo thread, int waitType, int waitId, IWaitStateChecker waitStateChecker, int micros, boolean forever, boolean callbacks) {
        // wait state
    	thread.waitType = waitType;
    	thread.waitId = waitId;
    	thread.wait.waitStateChecker = waitStateChecker;

    	// Go to wait state
        hleKernelThreadWait(thread, micros, forever);
        hleChangeThreadState(thread, PSP_THREAD_WAITING);
        hleRescheduleCurrentThread(callbacks);
    }

    private void hleBlockThread(SceKernelThreadInfo thread, boolean doCallbacks, IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
        if (!thread.isWaiting()) {
	    	thread.doCallbacks = doCallbacks;
	    	thread.wait.onUnblockAction = onUnblockAction;
	    	thread.waitType = JPCSP_WAIT_BLOCKED;
	    	thread.waitId = 0;
	    	thread.wait.waitStateChecker = waitStateChecker;
	        hleChangeThreadState(thread, thread.isSuspended() ? PSP_THREAD_WAITING_SUSPEND : PSP_THREAD_WAITING);
        }
    }

    public void hleBlockCurrentThread(boolean doCallbacks, IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
            log.debug("-------------------- block SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "' caller:" + getCallingFunction());
        }

    	hleBlockThread(currentThread, doCallbacks, onUnblockAction, waitStateChecker);
        hleRescheduleCurrentThread(doCallbacks);
    }

    public void hleBlockCurrentThread(IAction onUnblockAction) {
    	hleBlockCurrentThread(false, onUnblockAction, null);
    }

    public void hleBlockCurrentThread(IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
    	hleBlockCurrentThread(false, onUnblockAction, waitStateChecker);
    }

    public void hleBlockCurrentThreadCB(IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
    	hleBlockCurrentThread(true, onUnblockAction, waitStateChecker);
    }

    public SceKernelThreadInfo getThreadById(int uid) {
        return threadMap.get(uid);
    }

    public SceKernelThreadInfo getThreadByName(String name) {
        for (SceKernelThreadInfo thread : threadMap.values()) {
            if (name.equals(thread.name)) {
            	return thread;
            }
        }

        return null;
    }

    public void hleUnblockThread(int uid) {
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            SceKernelThreadInfo thread = threadMap.get(uid);
            hleChangeThreadState(thread, PSP_THREAD_READY);

            if (LOG_CONTEXT_SWITCHING && thread != null && Modules.log.isDebugEnabled()) {
                log.debug("-------------------- unblock SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' caller:" + getCallingFunction());
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
    	switch (thread.waitType) {
    		case PSP_WAIT_THREAD_END:
                // Return WAIT_TIMEOUT
    			thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_TIMEOUT;
    			break;
    		case PSP_WAIT_EVENTFLAG:
    			Managers.eventFlags.onThreadWaitTimeout(thread);
    			break;
    		case PSP_WAIT_SEMA:
    			Managers.semas.onThreadWaitTimeout(thread);
    			break;
    		case JPCSP_WAIT_UMD:
    			Modules.sceUmdUserModule.onThreadWaitTimeout(thread);
    			break;
    		case PSP_WAIT_MUTEX:
    			Managers.mutex.onThreadWaitTimeout(thread);
    			break;
    		case PSP_WAIT_LWMUTEX:
    			Managers.lwmutex.onThreadWaitTimeout(thread);
    			break;
    		case PSP_WAIT_MSGPIPE:
    			Managers.msgPipes.onThreadWaitTimeout(thread);
    			break;
    		case PSP_WAIT_MBX:
    			Managers.mbx.onThreadWaitTimeout(thread);
    			break;
    		case PSP_WAIT_FPL:
    			Managers.fpl.onThreadWaitTimeout(thread);
    			break;
    		case PSP_WAIT_VPL:
    			Managers.vpl.onThreadWaitTimeout(thread);
    			break;
    	}
    }

    private void hleThreadWaitRelease(SceKernelThreadInfo thread) {
    	// Thread was in a WAITING SUSPEND state?
    	if (thread.isSuspended()) {
    		// Go back to the SUSPEND state
    		hleChangeThreadState(thread, PSP_THREAD_SUSPEND);
    	} else if (thread.waitType != PSP_WAIT_NONE) {
            onWaitReleased(thread);
            hleChangeThreadState(thread, PSP_THREAD_READY);
        }
    }

    /** Call this when a thread's wait has been released. */
    private void onWaitReleased(SceKernelThreadInfo thread) {
    	switch (thread.waitType) {
			case PSP_WAIT_THREAD_END:
	            // Return ERROR_WAIT_STATUS_RELEASED
	            thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_STATUS_RELEASED;
				break;
			case PSP_WAIT_EVENTFLAG:
	            Managers.eventFlags.onThreadWaitReleased(thread);
				break;
			case PSP_WAIT_SEMA:
	            Managers.semas.onThreadWaitReleased(thread);
				break;
			case JPCSP_WAIT_UMD:
	            Modules.sceUmdUserModule.onThreadWaitReleased(thread);
				break;
			case PSP_WAIT_MUTEX:
	            Managers.mutex.onThreadWaitReleased(thread);
				break;
			case PSP_WAIT_LWMUTEX:
	            Managers.lwmutex.onThreadWaitReleased(thread);
				break;
			case PSP_WAIT_MSGPIPE:
	            Managers.msgPipes.onThreadWaitReleased(thread);
				break;
			case PSP_WAIT_MBX:
	            Managers.mbx.onThreadWaitReleased(thread);
				break;
			case PSP_WAIT_FPL:
	            Managers.fpl.onThreadWaitReleased(thread);
				break;
			case PSP_WAIT_VPL:
	            Managers.vpl.onThreadWaitReleased(thread);
				break;
			case JPCSP_WAIT_BLOCKED:
	        	thread.cpuContext.gpr[2] = ERROR_KERNEL_WAIT_STATUS_RELEASED;
				break;
    	}
    }

    private void deleteAllThreads() {
    	// Copy the list of threads into a new list to avoid ConcurrentListModificationException
    	List<SceKernelThreadInfo> threadsToBeDeleted = new LinkedList<SceKernelThreadInfo>(threadMap.values());

    	for (SceKernelThreadInfo thread : threadsToBeDeleted) {
    		hleDeleteThread(thread);
    	}
    }

    public void hleDeleteThread(SceKernelThreadInfo thread) {
        if (!threadMap.containsKey(thread.uid)) {
            log.debug(String.format("Thread %s already deleted", thread.toString()));
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("really deleting thread:'%s'", thread.name));
        }

        // cleanup thread - free the stack
        if (log.isDebugEnabled()) {
            log.debug(String.format("thread:'%s' freeing stack 0x%08X", thread.name, thread.getStackAddr()));
        }
        thread.freeStack();

        Managers.eventFlags.onThreadDeleted(thread);
        Managers.semas.onThreadDeleted(thread);
        Managers.mutex.onThreadDeleted(thread);
        Managers.lwmutex.onThreadDeleted(thread);
        Managers.msgPipes.onThreadDeleted(thread);
        Managers.mbx.onThreadDeleted(thread);
        Managers.fpl.onThreadDeleted(thread);
        Managers.vpl.onThreadDeleted(thread);
        Modules.sceUmdUserModule.onThreadDeleted(thread);
        RuntimeContext.onThreadDeleted(thread);
        // TODO blocking audio?
        // TODO async io?

        cancelThreadWait(thread);
        threadMap.remove(thread.uid);
        SceUidManager.releaseUid(thread.uid, "ThreadMan-thread");

        statistics.addThreadStatistics(thread);
    }

    private void removeFromReadyThreads(SceKernelThreadInfo thread) {
        synchronized (readyThreads) {
            readyThreads.remove(thread);
        	needThreadReschedule = true;
        }
    }

    private void addToReadyThreads(SceKernelThreadInfo thread, boolean addFirst) {
        synchronized (readyThreads) {
        	if (addFirst) {
        		readyThreads.addFirst(thread);
        	} else {
        		readyThreads.addLast(thread);
        	}
        	needThreadReschedule = true;
        }
    }

    private void setToBeDeletedThread(SceKernelThreadInfo thread) {
        thread.doDelete = true;

        if (thread.isStopped()) {
            // It's possible for a game to request the same thread to be deleted multiple times.
            // We only mark for deferred deletion.
            // Example:
            // - main thread calls sceKernelDeleteThread on child thread
            // - child thread calls sceKernelExitDeleteThread
            if (thread.doDeleteAction == null) {
                thread.doDeleteAction = new DeleteThreadAction(thread);
                Scheduler.getInstance().addAction(thread.doDeleteAction);
            }
        }
    }

    private void triggerThreadEvent(SceKernelThreadInfo thread, SceKernelThreadInfo contextThread, int event) {
        if (threadEventMap.containsKey(thread.uid)) {
            int handlerUid = threadEventMap.get(thread.uid);
            SceKernelThreadEventHandlerInfo handler = threadEventHandlerMap.get(handlerUid);

            // Check if this handler's mask matches the function.
            if (handler.hasEventMask(event)) {
                handler.triggerThreadEventHandler(contextThread, event);
            }
        }
    }

    public void hleKernelChangeThreadPriority(SceKernelThreadInfo thread, int newPriority) {
    	if (thread == null) {
    		return;
    	}

    	int oldPriority = thread.currentPriority;
        thread.currentPriority = newPriority;
        // switch in the target thread if it's now higher priority
        if ((thread.status & PSP_THREAD_READY) == PSP_THREAD_READY &&
        		newPriority < currentThread.currentPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelChangeThreadPriority yielding to thread with higher priority");
            }
            needThreadReschedule = true;
            hleRescheduleCurrentThread();
        } else if ((thread.status & PSP_THREAD_READY) == PSP_THREAD_READY &&
            		newPriority == oldPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelChangeThreadPriority yielding to thread with same priority");
            }
            // Move the thread to the end of the list
        	removeFromReadyThreads(thread);
            addToReadyThreads(thread, false);
            needThreadReschedule = true;
            hleRescheduleCurrentThread();
        } else if (thread.uid == currentThread.uid && newPriority > oldPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelChangeThreadPriority rescheduling");
            }
            // yield if we moved ourself to lower priority
            needThreadReschedule = true;
            hleRescheduleCurrentThread();
        }
    }

    /**
     * Change to state of a thread.
     * This function must be used when changing the state of a thread as
     * it updates the ThreadMan internal data structures and implements
     * the PSP behavior on status change.
     *
     * @param thread    the thread to be updated
     * @param newStatus the new thread status
     */
    public void hleChangeThreadState(SceKernelThreadInfo thread, int newStatus) {
        if (thread == null) {
            return;
        }

        if (thread.status == newStatus) {
            // Thread status not changed, nothing to do
            return;
        }

        if (!dispatchThreadEnabled && thread == currentThread && newStatus != PSP_THREAD_RUNNING) {
            log.info("DispatchThread disabled, not changing thread state of " + thread + " to " + newStatus);
            return;
        }

        boolean addReadyThreadsFirst = false;

        // Moving out of the following states...
        if (thread.status == PSP_THREAD_WAITING && newStatus != PSP_THREAD_WAITING_SUSPEND) {
            if (thread.wait.waitTimeoutAction != null) {
                Scheduler.getInstance().removeAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
                thread.wait.waitTimeoutAction = null;
            }
            if (thread.waitType == JPCSP_WAIT_BLOCKED) {
            	if (thread.wait.onUnblockAction != null) {
            		thread.wait.onUnblockAction.execute();
            		thread.wait.onUnblockAction = null;
            	}
            }
            thread.doCallbacks = false;
        } else if (thread.isStopped()) {
            if (thread.doDeleteAction != null) {
                Scheduler.getInstance().removeAction(0, thread.doDeleteAction);
                thread.doDeleteAction = null;
            }
        } else if (thread.isReady()) {
            removeFromReadyThreads(thread);
        } else if (thread.isSuspended()) {
            thread.doCallbacks = false;
        } else if (thread.isRunning()) {
        	needThreadReschedule = true;
        	// When a running thread has to yield to a thread having a higher
        	// priority, the thread stays in front of the ready threads having
        	// the same priority (no yielding to threads having the same priority).
        	addReadyThreadsFirst = true;
        }

        thread.status = newStatus;

        // Moving to the following states...
        if (thread.status == PSP_THREAD_WAITING) {
            if (thread.wait.waitTimeoutAction != null) {
                Scheduler.getInstance().addAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
            }

            // debug
            if (thread.waitType == PSP_WAIT_NONE) {
                log.warn("changeThreadState thread '" + thread.name + "' => PSP_THREAD_WAITING. waitType should NOT be PSP_WAIT_NONE. caller:" + getCallingFunction());
            }
        } else if (thread.isStopped()) {
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
        } else if (thread.isReady()) {
            addToReadyThreads(thread, addReadyThreadsFirst);
            thread.waitType = PSP_WAIT_NONE;
            thread.wait.waitTimeoutAction = null;
            thread.wait.waitStateChecker = null;
            thread.doCallbacks = false;
        } else if (thread.isRunning()) {
            // debug
            if (thread.waitType != PSP_WAIT_NONE && !isIdleThread(thread)) {
                log.error("changeThreadState thread '" + thread.name + "' => PSP_THREAD_RUNNING. waitType should be PSP_WAIT_NONE. caller:" + getCallingFunction());
            }
        }
    }

    private void cancelThreadWait(SceKernelThreadInfo thread) {
        // Cancel all waiting actions
        thread.wait.onUnblockAction = null;
        thread.wait.waitStateChecker = null;
        thread.waitType = PSP_WAIT_NONE;
        if (thread.wait.waitTimeoutAction != null) {
            Scheduler.getInstance().removeAction(thread.wait.microTimeTimeout, thread.wait.waitTimeoutAction);
            thread.wait.waitTimeoutAction = null;
        }
    }

    private void terminateThread(SceKernelThreadInfo thread) {
        hleChangeThreadState(thread, PSP_THREAD_STOPPED);  // PSP_THREAD_STOPPED (checked)
        cancelThreadWait(thread);
        RuntimeContext.onThreadExit(thread);
        if (thread == currentThread) {
        	hleRescheduleCurrentThread();
        }
    }

    private void onThreadStopped(SceKernelThreadInfo stoppedThread) {
        for (SceKernelThreadInfo thread : threadMap.values()) {
            // Wakeup threads that are in sceKernelWaitThreadEnd
            // We're assuming if waitingOnThreadEnd is set then thread.status = waiting
            if (thread.isWaitingForType(PSP_WAIT_THREAD_END) &&
                    thread.wait.ThreadEnd_id == stoppedThread.uid) {
            	hleThreadWaitRelease(thread);
                // Return exit status of stopped thread
                thread.cpuContext.gpr[2] = stoppedThread.exitStatus;
            }
        }
    }

    public void hleKernelExitCallback() {
        hleKernelExitCallback(Emulator.getProcessor());
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelExitCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int callbackId = cpu.gpr[CALLBACKID_REGISTER];
        Callback callback = callbackManager.remove(callbackId);
        if (callback != null) {
            if (log.isTraceEnabled()) {
                log.trace("End of callback " + callback);
            }
            cpu.gpr[CALLBACKID_REGISTER] = callback.getSavedIdRegister();
            cpu.gpr[31] = callback.getSavedRa();
            cpu.pc = callback.getSavedPc();
            IAction afterAction = callback.getAfterAction();
            if (afterAction != null) {
                afterAction.execute();
            }

            // Do we need to restore $v0/$v1?
            if (callback.isReturnVoid()) {
            	cpu.gpr[2] = callback.getSavedV0();
            	cpu.gpr[3] = callback.getSavedV1();
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
     * @param returnVoid  the code has a void return value, i.e. $v0/$v1 have to be restored
     */
    public void callAddress(int address, IAction afterAction, boolean returnVoid) {
        callAddress(null, address, afterAction, returnVoid, null);
    }

    private void callAddress(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int[] parameters) {
        if (thread != null) {
            // Save the wait state of the thread to restore it after the call
            int status = thread.status;
            int waitType = thread.waitType;
            int waitId = thread.waitId;
            ThreadWaitInfo threadWaitInfo = new ThreadWaitInfo();
            threadWaitInfo.copy(thread.wait);
            boolean doCallbacks = thread.doCallbacks;

            // Terminate the thread wait state
            thread.waitType = PSP_WAIT_NONE;

            afterAction = new AfterCallAction(thread, status, waitType, waitId, threadWaitInfo, doCallbacks, afterAction);
            hleChangeThreadState(thread, PSP_THREAD_READY);
        }

        int callbackId = callbackManager.getNewCallbackId();
        Callback callback = new Callback(callbackId, address, parameters, afterAction, returnVoid);

        callbackManager.addCallback(callback);

        boolean callbackCalled = false;
        if (thread == null || thread == currentThread) {
        	if (RuntimeContext.canExecuteCallback(thread)) {
        		thread = currentThread;
                hleChangeThreadState(thread, PSP_THREAD_RUNNING);
            	callback.execute(thread);
            	callbackCalled = true;
        	}
        }

        if (!callbackCalled) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Pushing pending callback '%s' for thread '%s'", callback, thread));
        	}
        	thread.pendingCallbacks.add(callback);
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
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X, afterAction=%s, returnVoid=%b", address, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, null);
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
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0});
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
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1});
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
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     * @param registerA2  third parameter of the callback ($a2)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1, int registerA2) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, registerA2, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1, registerA2});
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
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     * @param registerA2  third parameter of the callback ($a2)
     * @param registerA3  fourth parameter of the callback ($a3)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1, int registerA2, int registerA3) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X, $a3=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, registerA2, registerA3, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1, registerA2, registerA3});
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
     * @param returnVoid  the callback has a void return value, i.e. $v0/$v1 have to be restored
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     * @param registerA2  third parameter of the callback ($a2)
     * @param registerT0  fifth parameter of the callback ($t0)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, int registerA0, int registerA1, int registerA2, int registerA3, int registerT0) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X, $a3=0x%08X, $t0=0x%08X), afterAction=%s, returnVoid=%b", address, registerA0, registerA1, registerA2, registerA3, registerT0, afterAction, returnVoid));
        }

        callAddress(thread, address, afterAction, returnVoid, new int[]{registerA0, registerA1, registerA2, registerA3, registerT0});
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelExitThread(Processor processor) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Thread exit detected SceUID=%x name='%s' return:0x%08X", currentThread.uid, currentThread.name, processor.cpu.gpr[2]));
        }
        // NOTE: When a thread exits by itself (without calling sceKernelExitThread),
        // it's exitStatus becomes it's return value.
        // When this is detected, the $a0 register should be overwritten with the thread's $v0
        // register contents BEFORE calling sceKernelExitThread.
        sceKernelExitThread(processor, processor.cpu.gpr[2]);
    }

    public void hleKernelExitDeleteThread() {
        Processor processor = Emulator.getProcessor();
        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelExitDeleteThread SceUID=%x name='%s' return:0x%08X", currentThread.uid, currentThread.name, processor.cpu.gpr[_v0]));
        }

        sceKernelExitDeleteThread(processor, processor.cpu.gpr[_v0]);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelAsyncLoop(Processor processor) {
        Modules.IoFileMgrForUserModule.hleAsyncThread(processor);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelNetApctlLoop(Processor processor) {
    	Modules.sceNetApctlModule.hleNetApctlThread(processor);
    }

    /** Note: Some functions allow uid = 0 = current thread, others don't.
     * if uid = 0 then $v0 is set to ERROR_ILLEGAL_THREAD and false is returned
     * if uid < 0 then $v0 is set to ERROR_NOT_FOUND_THREAD and false is returned */
    private boolean checkThreadID(int uid) {
        if (uid == 0) {
            log.warn("checkThreadID illegal thread (uid=0) caller:" + getCallingFunction());
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_THREAD;
            return false;
        } else if (uid < 0) {
            log.warn("checkThreadID not found thread " + Integer.toHexString(uid) + " (uid<0) caller:" + getCallingFunction());
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
            return false;
        } else {
            SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
            return true;
        }
    }

    public SceKernelThreadInfo hleKernelCreateThread(String name, int entry_addr,
            int initPriority, int stackSize, int attr, int option_addr) {

        if (option_addr != 0) {
            Modules.log.warn("hleKernelCreateThread unhandled SceKernelThreadOptParam: " +
                    "option_addr=0x" + Integer.toHexString(option_addr));
        }

        SceKernelThreadInfo thread = new SceKernelThreadInfo(name, entry_addr, initPriority, stackSize, attr);
        threadMap.put(thread.uid, thread);

        // inherit module id
        if (currentThread != null) {
            thread.moduleid = currentThread.moduleid;
        }

        if (log.isDebugEnabled()) {
            log.debug("hleKernelCreateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' PC=" + Integer.toHexString(thread.cpuContext.pc) + " attr:0x" + Integer.toHexString(attr) + " pri:0x" + Integer.toHexString(initPriority) + " stackSize:0x" + Integer.toHexString(stackSize));
        }

        return thread;
    }

    private void setThreadBanningEnabled(boolean enabled) {
        USE_THREAD_BANLIST = enabled;
        log.info("Audio threads disabled: " + USE_THREAD_BANLIST);
    }

    /** use lower case in this list */
    private final String[] threadNameBanList = new String[]{
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
                    name.contains("fmod") || name.contains("mp3")) {
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
        if (log.isDebugEnabled()) {
            log.debug("hleKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' dataLen=0x" + Integer.toHexString(userDataLength) + " data=0x" + Integer.toHexString(userDataAddr) + " gp=0x" + Integer.toHexString(gp));
        }
        // Reset all thread parameters: a thread can be restarted when it has exited.
        thread.reset();
        // Setup args by copying them onto the stack
        //int address = thread.cpuContext.gpr[29];
        // 256 bytes padding between user data top and real stack top
        int address = (thread.getStackAddr() + thread.stackSize - 0x100) - ((userDataLength + 0xF) & ~0xF);
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
        if (log.isDebugEnabled() && thread.cpuContext.gpr[28] != gp) {
            // from sceKernelStartModule is ok, anywhere else might be an error
            log.debug("hleKernelStartThread oldGP=0x" + Integer.toHexString(thread.cpuContext.gpr[28]) + " newGP=0x" + Integer.toHexString(gp));
        }
        thread.cpuContext.gpr[28] = gp;
        // switch in the target thread if it's higher priority
        hleChangeThreadState(thread, PSP_THREAD_READY);
        if (thread.currentPriority < currentThread.currentPriority) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelStartThread switching in thread immediately");
            }
            hleRescheduleCurrentThread();
        }
    }

    public int hleKernelSleepThread(boolean doCallbacks) {
        if (currentThread.wakeupCount > 0) {
            // sceKernelWakeupThread() has been called before, do not sleep
            currentThread.wakeupCount--;
        } else {
            // Go to wait state and wait forever (another thread will call sceKernelWakeupThread)
        	hleKernelThreadEnterWaitState(PSP_WAIT_SLEEP, 0, null, doCallbacks);
        }

        return 0;
    }

    public void hleKernelWakeupThread(SceKernelThreadInfo thread) {
        if (!thread.isWaiting() || thread.waitType != PSP_WAIT_SLEEP) {
            thread.wakeupCount++;
            if (log.isDebugEnabled()) {
            	log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' not sleeping/waiting (status=0x" + Integer.toHexString(thread.status) + "), incrementing wakeupCount to " + thread.wakeupCount);
            }
        } else if (isBannedThread(thread)) {
            log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' banned, not waking up");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelWakeupThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
            }
            hleThreadWaitRelease(thread);

            // Check if we have to switch in the target thread
            // e.g. if if has a higher priority
            hleRescheduleCurrentThread();
        }
    }

    private void hleKernelWaitThreadEnd(int uid, int timeoutAddr, boolean callbacks) {
        if (!checkThreadID(uid)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelWaitThreadEnd SceUID=0x%X, callbacks=%b", uid, callbacks));
        }

        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn(String.format("hleKernelWaitThreadEnd unknown thread 0x%X", uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            log.warn(String.format("hleKernelWaitThreadEnd %s banned, not waiting", thread.toString()));
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else if (thread.isStopped()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleKernelWaitThreadEnd %s thread already stopped, not waiting", thread.toString()));
        	}
            Emulator.getProcessor().cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else {
            // Wait on a specific thread end
            currentThread.wait.ThreadEnd_id = uid;
        	hleKernelThreadEnterWaitState(PSP_WAIT_THREAD_END, uid, waitThreadEndWaitStateChecker, timeoutAddr, callbacks);
        }
    }

    /**
     * Set the wait timeout for a thread. The state of the thread is not changed.
     *
     * @param thread  the thread
     * @param wait    the same as thread.wait
     * @param micros  the timeout in microseconds (this is an unsigned value: SceUInt32)
     * @param forever true if the thread has to wait forever (micros in then ignored)
     */
    public void hleKernelThreadWait(SceKernelThreadInfo thread, int micros, boolean forever) {
        thread.wait.forever = forever;
        thread.wait.micros = micros; // for debugging
        if (forever) {
            thread.wait.microTimeTimeout = 0;
            thread.wait.waitTimeoutAction = null;
        } else {
            long longMicros = ((long) micros) & 0xFFFFFFFFL;
            thread.wait.microTimeTimeout = Emulator.getClock().microTime() + longMicros;
            thread.wait.waitTimeoutAction = new TimeoutThreadAction(thread);
            thread.wait.waitStateChecker = timeoutThreadWaitStateChecker;
        }

        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled() && !isIdleThread(thread)) {
            log.debug("-------------------- hleKernelThreadWait micros=" + micros + " forever:" + forever + " thread:'" + thread.name + "' caller:" + getCallingFunction());
        }
    }

    public void hleKernelDelayThread(int micros, boolean doCallbacks) {
        // wait type
        currentThread.waitType = PSP_WAIT_DELAY;

        if (micros < THREAD_DELAY_MINIMUM_MICROS) {
        	micros = THREAD_DELAY_MINIMUM_MICROS;
        }

        if (log.isDebugEnabled()) {
            log.debug("hleKernelDelayThread micros=" + micros + ", callbacks=" + doCallbacks);
        }

        // Wait on a timeout only
        hleKernelThreadWait(currentThread, micros, false);
        hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
        hleRescheduleCurrentThread(doCallbacks);
    }

    public SceKernelCallbackInfo hleKernelCreateCallback(String name, int func_addr, int user_arg_addr) {
        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, currentThread.uid, func_addr, user_arg_addr);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("hleKernelCreateCallback %s", callback));
        }

        callbackMap.put(callback.uid, callback);

        return callback;
    }

    /** @return true if successful. */
    public boolean hleKernelDeleteCallback(int uid) {
        SceKernelCallbackInfo info = callbackMap.remove(uid);
        boolean removed = info != null;
        if (removed) {
            if (log.isDebugEnabled()) {
                log.debug("hleKernelDeleteCallback SceUID=" + Integer.toHexString(uid) + " name:'" + info.name + "'");
            }
        } else {
            log.warn("hleKernelDeleteCallback not a callback uid 0x" + Integer.toHexString(uid));
        }

        return removed;
    }

    protected int getThreadCurrentStackSize(Processor processor) {
    	int size = processor.cpu.gpr[_sp] - currentThread.getStackAddr();
        if (size < 0) {
            size = 0;
        }
        return size;
    }

    private boolean userCurrentThreadTryingToSwitchToKernelMode(int newAttr) {
        return currentThread.isUserMode() && !SceKernelThreadInfo.isUserMode(newAttr);
    }

    private boolean userThreadCalledKernelCurrentThread(SceKernelThreadInfo thread) {
        return !isIdleThread(thread) && (!thread.isKernelMode() || currentThread.isKernelMode());
    }

    private int getDispatchThreadState() {
        return dispatchThreadEnabled ? SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED : SCE_KERNEL_DISPATCHTHREAD_STATE_DISABLED;
    }

    /** Registers a callback on the thread that created the callback.
     * @return true on success (the cbid was a valid callback uid) */
    public boolean hleKernelRegisterCallback(int callbackType, int cbid) {
        SceKernelCallbackInfo callback = callbackMap.get(cbid);
        if (callback == null) {
            log.warn("hleKernelRegisterCallback(type=" + callbackType + ") unknown uid " + Integer.toHexString(cbid));
            return false;
        }

        SceKernelThreadInfo thread = getThreadById(callback.threadId);
        if (thread == null) {
            log.warn("hleKernelRegisterCallback(type=" + callbackType + ") unknown thread uid " + Integer.toHexString(callback.threadId));
        	return false;
        }
        RegisteredCallbacks registeredCallbacks = thread.getRegisteredCallbacks(callbackType);
        if (!registeredCallbacks.addCallback(callback)) {
        	return false;
        }

        return true;
    }

    /** Unregisters a callback by type and cbid. May not be on the current thread.
     * @param callbackType See SceKernelThreadInfo.
     * @param cbid The UID of the callback to unregister.
     * @return SceKernelCallbackInfo of the removed callback, or null if it
     * couldn't be found. */
    public SceKernelCallbackInfo hleKernelUnRegisterCallback(int callbackType, int cbid) {
        SceKernelCallbackInfo callback = null;

        for (SceKernelThreadInfo thread : threadMap.values()) {
        	RegisteredCallbacks registeredCallbacks = thread.getRegisteredCallbacks(callbackType);

        	callback = registeredCallbacks.getCallbackByUid(cbid);
            if (callback != null) {
                // Warn if we are removing a pending callback, this a callback
                // that has been pushed but not yet executed.
                if (registeredCallbacks.isCallbackReady(callback)) {
                    log.warn("hleKernelUnRegisterCallback(type=" + callbackType + ") removing pending callback");
                }

                registeredCallbacks.removeCallback(callback);
                break;
            }
        }

        if (callback == null) {
            log.warn("hleKernelUnRegisterCallback(type=" + callbackType + ") cbid=" + Integer.toHexString(cbid) + " no matching callbacks found");
        }

        return callback;
    }

    /** push callback to all threads */
    public void hleKernelNotifyCallback(int callbackType, int notifyArg) {
        hleKernelNotifyCallback(callbackType, -1, notifyArg);
    }

    private void notifyCallback(SceKernelThreadInfo thread, SceKernelCallbackInfo callback, int callbackType, int notifyArg) {
        if (callback.notifyCount != 0) {
            log.warn("hleKernelNotifyCallback(type=" + callbackType + ") thread:'" + thread.name + "' overwriting previous notifyArg 0x" + Integer.toHexString(callback.notifyArg) + " -> 0x" + Integer.toHexString(notifyArg) + ", newCount=" + (callback.notifyCount + 1));
        }

        callback.notifyCount++; // keep increasing this until we actually enter the callback
        callback.notifyArg = notifyArg;
        thread.getRegisteredCallbacks(callbackType).setCallbackReady(callback);
    }

    /** @param cbid If cbid is -1, then push callback to all threads
     * if cbid is not -1 then only trigger that specific cbid provided it is
     * also of type callbackType. */
    public void hleKernelNotifyCallback(int callbackType, int cbid, int notifyArg) {
        boolean pushed = false;

        for (SceKernelThreadInfo thread : threadMap.values()) {
        	RegisteredCallbacks registeredCallbacks = thread.getRegisteredCallbacks(callbackType);

            if (!registeredCallbacks.hasCallbacks()) {
            	continue;
            }

            if (cbid != -1) {
                SceKernelCallbackInfo callback = registeredCallbacks.getCallbackByUid(cbid);
                if (callback == null) {
                	continue;
                }

                notifyCallback(thread, callback, callbackType, notifyArg);
            } else {
            	int numberOfCallbacks = registeredCallbacks.getNumberOfCallbacks();
            	for (int i = 0; i < numberOfCallbacks; i++) {
            		SceKernelCallbackInfo callback = registeredCallbacks.getCallbackByIndex(i);
            		notifyCallback(thread, callback, callbackType, notifyArg);
            	}
            }

            pushed = true;
        }

        if (pushed) {
            // Enter callbacks immediately,
            // except those registered to the current thread. The app must explictly
            // call sceKernelCheckCallback or a waitCB function to do that.
            if (log.isDebugEnabled()) {
                log.debug("hleKernelNotifyCallback(type=" + callbackType + ") calling checkCallbacks");
            }
            checkCallbacks();
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleKernelNotifyCallback(type=%d) no registered callbacks to push", callbackType));
        	}
        }
    }

    /** runs callbacks. Check the thread doCallbacks flag.
     * @return true if we switched into a callback. */
    private boolean checkThreadCallbacks(SceKernelThreadInfo thread) {
        boolean handled = false;

        if (thread == null || !thread.doCallbacks) {
            return handled;
        }

        for (int callbackType = 0; callbackType < SceKernelThreadInfo.THREAD_CALLBACK_SIZE; callbackType++) {
        	RegisteredCallbacks registeredCallbacks = thread.getRegisteredCallbacks(callbackType);
        	SceKernelCallbackInfo callback = registeredCallbacks.getNextReadyCallback();
            if (callback != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Entering callback type %d %s for thread %s (current thread is %s)", callbackType, callback.toString(), thread.toString(), currentThread.toString()));
                }

                callback.startContext(thread, null);
                handled = true;
                break;
            }
        }

        return handled;
    }

    public void cancelAlarm(SceKernelAlarmInfo sceKernelAlarmInfo) {
        Scheduler.getInstance().removeAction(sceKernelAlarmInfo.schedule, sceKernelAlarmInfo.alarmInterruptAction);
        sceKernelAlarmInfo.schedule = 0;
    }

    public void rescheduleAlarm(SceKernelAlarmInfo sceKernelAlarmInfo, int delay) {
        if (delay < 0) {
            delay = 100;
        }

        sceKernelAlarmInfo.schedule += delay;
        scheduleAlarm(sceKernelAlarmInfo);

        if (log.isDebugEnabled()) {
            log.debug(String.format("New Schedule for Alarm uid=%x: %d", sceKernelAlarmInfo.uid, sceKernelAlarmInfo.schedule));
        }
    }

    private void scheduleAlarm(SceKernelAlarmInfo sceKernelAlarmInfo) {
        Scheduler.getInstance().addAction(sceKernelAlarmInfo.schedule, sceKernelAlarmInfo.alarmInterruptAction);
    }

    protected void hleKernelSetAlarm(Processor processor, long delayUsec, int handlerAddress, int handlerArgument) {
        CpuState cpu = processor.cpu;

        long now = Scheduler.getNow();
        long schedule = now + delayUsec;
        SceKernelAlarmInfo sceKernelAlarmInfo = new SceKernelAlarmInfo(schedule, handlerAddress, handlerArgument);
        alarms.put(sceKernelAlarmInfo.uid, sceKernelAlarmInfo);

        scheduleAlarm(sceKernelAlarmInfo);

        cpu.gpr[2] = sceKernelAlarmInfo.uid;
    }

    protected long getSystemTime() {
        return SystemTimeManager.getSystemTime();
    }

    protected long getVTimerRunningTime(SceKernelVTimerInfo sceKernelVTimerInfo) {
        if (sceKernelVTimerInfo.active != SceKernelVTimerInfo.ACTIVE_RUNNING) {
        	return 0;
        }

        return getSystemTime() - sceKernelVTimerInfo.base;
    }

    public long getVTimerTime(SceKernelVTimerInfo sceKernelVTimerInfo) {
        return sceKernelVTimerInfo.current + getVTimerRunningTime(sceKernelVTimerInfo);
    }

    protected long getVTimerScheduleForScheduler(SceKernelVTimerInfo sceKernelVTimerInfo) {
        return sceKernelVTimerInfo.base + sceKernelVTimerInfo.schedule;
    }

    protected void setVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long time) {
        sceKernelVTimerInfo.current = time - getVTimerRunningTime(sceKernelVTimerInfo);
    }

    protected void startVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_RUNNING;
        sceKernelVTimerInfo.base = getSystemTime();

        if (sceKernelVTimerInfo.schedule != 0 && sceKernelVTimerInfo.handlerAddress != 0) {
            scheduleVTimer(sceKernelVTimerInfo, sceKernelVTimerInfo.schedule);
        }
    }

    protected void stopVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_STOPPED;
        // Sum the elapsed time (multiple Start/Stop sequences are added)
        sceKernelVTimerInfo.current += getVTimerRunningTime(sceKernelVTimerInfo);
    }

    protected void scheduleVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long schedule) {
        sceKernelVTimerInfo.schedule = schedule;

        if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
            Scheduler scheduler = Scheduler.getInstance();
            scheduler.addAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);
        }
    }

    public void cancelVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        Scheduler.getInstance().removeAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);
        sceKernelVTimerInfo.schedule = 0;
        sceKernelVTimerInfo.handlerAddress = 0;
        sceKernelVTimerInfo.handlerArgument = 0;
    }

    public void rescheduleVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, int delay) {
        if (delay < 0) {
            delay = 100;
        }

        sceKernelVTimerInfo.schedule += delay;

        scheduleVTimer(sceKernelVTimerInfo, sceKernelVTimerInfo.schedule);

        if (log.isDebugEnabled()) {
            log.debug(String.format("New Schedule for VTimer uid=%x: %d", sceKernelVTimerInfo.uid, sceKernelVTimerInfo.schedule));
        }
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
    public void checkCallbacks() {
        if (log.isTraceEnabled()) {
            log.trace("checkCallbacks current thread is '" + currentThread.name + "' doCallbacks:" + currentThread.doCallbacks + " caller:" + getCallingFunction());
        }

        boolean handled;
        SceKernelThreadInfo checkCurrentThread = currentThread;
        do {
            handled = false;
            for (SceKernelThreadInfo thread : threadMap.values()) {
                if (thread.doCallbacks && checkThreadCallbacks(thread)) {
                    handled = true;
                    break;
                }
            }
            // Continue until there is no more callback to be executed or
            // we have switched to another thread.
        } while (handled && checkCurrentThread == currentThread);
    }

    @HLEFunction(nid = 0x6E9EA350, version = 150)
    public void _sceKernelReturnFromCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented _sceKernelReturnFromCallback");

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x0C106E53, version = 150, checkInsideInterrupt = true)
    public void sceKernelRegisterThreadEventHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int name_addr = cpu.gpr[4];
        int thid = cpu.gpr[5];
        int mask = cpu.gpr[6];
        int handler_func = cpu.gpr[7];
        int common_addr = cpu.gpr[8];

        String name = readStringNZ(name_addr, 32);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelRegisterThreadEventHandler name=" + name + ", thid=0x" + Integer.toHexString(thid) + ", mask=0x" + Integer.toHexString(mask) + ", handler_func=0x" + Integer.toHexString(handler_func) + ", common_addr=0x" + Integer.toHexString(common_addr));
        }

        if (threadMap.containsKey(thid)) {
            SceKernelThreadEventHandlerInfo handler = new SceKernelThreadEventHandlerInfo(name, thid, mask, handler_func, common_addr);
            threadEventHandlerMap.put(handler.uid, handler);
            threadEventMap.put(thid, handler.uid);
            cpu.gpr[2] = handler.uid;
        } else {
            SceKernelThreadEventHandlerInfo handler = new SceKernelThreadEventHandlerInfo(name, thid, mask, handler_func, common_addr);
            if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_CURRENT) {
                threadEventHandlerMap.put(handler.uid, handler);
                threadEventMap.put(getCurrentThread().uid, handler.uid);
                cpu.gpr[2] = handler.uid;
            } else if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_USER) {
                threadEventHandlerMap.put(handler.uid, handler);
                for (SceKernelThreadInfo thread : threadMap.values()) {
                    if (thread.isUserMode()) {
                        threadEventMap.put(thread.uid, handler.uid);
                    }
                }
                cpu.gpr[2] = handler.uid;
            } else if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_KERN && isKernelMode()) {
                threadEventHandlerMap.put(handler.uid, handler);
                for (SceKernelThreadInfo thread : threadMap.values()) {
                    if (thread.isKernelMode()) {
                        threadEventMap.put(thread.uid, handler.uid);
                    }
                }
                cpu.gpr[2] = handler.uid;
            } else if (thid == SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_ALL && isKernelMode()) {
                threadEventHandlerMap.put(handler.uid, handler);
                for (SceKernelThreadInfo thread : threadMap.values()) {
                    threadEventMap.put(thread.uid, handler.uid);
                }
                cpu.gpr[2] = handler.uid;
            } else {
                cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
            }
        }
    }

    @HLEFunction(nid = 0x72F3C145, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelReleaseThreadEventHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReleaseThreadEventHandler uid=0x" + Integer.toHexString(uid));
        }

        if (threadEventHandlerMap.containsKey(uid)) {
        	SceKernelThreadEventHandlerInfo handler = threadEventHandlerMap.remove(uid);
        	handler.release();
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
        }
    }

    @HLEFunction(nid = 0x369EEB6B, version = 150)
    public void sceKernelReferThreadEventHandlerStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int uid = cpu.gpr[4];
        int status_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferThreadEventHandlerStatus uid=0x" + Integer.toHexString(uid) + ", status_addr=0x" + Integer.toHexString(status_addr));
        }

        if (threadEventHandlerMap.containsKey(uid)) {
            threadEventHandlerMap.get(uid).write(mem, status_addr);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
        }
    }

    @HLEFunction(nid = 0xE81CAF8F, version = 150, checkInsideInterrupt = true)
    public void sceKernelCreateCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int name_addr = cpu.gpr[4];
        int func_addr = cpu.gpr[5];
        int user_arg_addr = cpu.gpr[6];
        String name = readStringNZ(name_addr, 32);

        SceKernelCallbackInfo callback = hleKernelCreateCallback(name, func_addr, user_arg_addr);
        cpu.gpr[2] = callback.uid;
    }

    @HLEFunction(nid = 0xEDBA5844, version = 150, checkInsideInterrupt = true)
    public void sceKernelDeleteCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (hleKernelDeleteCallback(uid)) {
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }
    }

    /**
     * Manually notifies a callback. Mostly used for exit callbacks,
     * and shouldn't be used at all (only some old homebrews use this, anyway).
     */
    @HLEFunction(nid = 0xC11BA8C4, version = 150)
    public void sceKernelNotifyCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int arg = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelNotifyCallback uid=0x" + Integer.toHexString(uid) + ", arg=0x" + Integer.toHexString(arg));
        }
        SceKernelCallbackInfo callback = callbackMap.get(uid);
        if (callback != null) {
        	boolean foundCallback = false;
            for (int i = 0; i < SceKernelThreadInfo.THREAD_CALLBACK_SIZE; i++) {
            	RegisteredCallbacks registeredCallbacks = getCurrentThread().getRegisteredCallbacks(i);
                if (registeredCallbacks.hasCallback(callback)) {
                    hleKernelNotifyCallback(i, uid, arg);
                    foundCallback = true;
                    break;
                }
            }
            if (!foundCallback) {
            	// Register the callback as a temporary THREAD_CALLBACK_USER_DEFINED
            	if (hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_USER_DEFINED, uid)) {
            		hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_USER_DEFINED, uid, arg);
            	}
            }
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }
    }

    @HLEFunction(nid = 0xBA4051D6, version = 150)
    public void sceKernelCancelCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelCallback uid=0x" + Integer.toHexString(uid));
        }

        SceKernelCallbackInfo callback = callbackMap.get(uid);
        if (callback != null) {
            callback.notifyArg = 0;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }

    }

    /** Return the current notifyCount for a specific callback */
    @HLEFunction(nid = 0x2A3D44FF, version = 150)
    public void sceKernelGetCallbackCount(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetCallbackCount uid=0x" + Integer.toHexString(uid));
        }
        SceKernelCallbackInfo callback = callbackMap.get(uid);
        if (callback != null) {
            cpu.gpr[2] = callback.notifyCount;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        }
    }

    /** Check callbacks, only on the current thread. */
    @HLEFunction(nid = 0x349D6D6C, version = 150, checkInsideInterrupt = true)
    public void sceKernelCheckCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCheckCallback(void)");
        }

        // Remember the currentThread, as it might have changed after
        // the execution of a callback.
        SceKernelThreadInfo thread = currentThread;

        // 0 - The calling thread has no reported callbacks.
        // 1 - The calling thread has reported callbacks which were executed sucessfully.
        thread.doCallbacks = true;  // Callbacks are always allowed here.
        cpu.gpr[2] = checkThreadCallbacks(thread) ? 1 : 0;
        thread.doCallbacks = false; // Callbacks may not be allowed after this.
    }

    @HLEFunction(nid = 0x730ED8BC, version = 150)
    public void sceKernelReferCallbackStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int uid = cpu.gpr[4];
        int info_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferCallbackStatus SceUID=" + Integer.toHexString(uid) + " info=" + Integer.toHexString(info_addr));
        }

        SceKernelCallbackInfo info = hleKernelReferCallbackStatus(uid);
        if (info == null) {
            log.warn("sceKernelReferCallbackStatus unknown uid 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
        } else if (!Memory.isAddressGood(info_addr)) {
            log.warn("sceKernelReferCallbackStatus bad info address 0x" + Integer.toHexString(info_addr));
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            int size = mem.read32(info_addr);
            if (size >= SceKernelCallbackInfo.size) {
                info.write(mem, info_addr);
                cpu.gpr[2] = 0;
            } else {
                log.warn("sceKernelReferCallbackStatus bad info size got " + size + " want " + SceKernelCallbackInfo.size);
                cpu.gpr[2] = -1;
            }
        }
    }

    /** sleep the current thread (using wait) */
    @HLEFunction(nid = 0x9ACE131E, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelSleepThread() {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelSleepThread SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "'");
        }

        return hleKernelSleepThread(false);
    }

    /** sleep the current thread and handle callbacks (using wait)
     * in our implementation we have to use wait, not suspend otherwise we don't handle callbacks. */
    @HLEFunction(nid = 0x82826F70, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelSleepThreadCB() {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelSleepThreadCB SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "'");
        }

        int result = hleKernelSleepThread(true);
        checkCallbacks();

        return result;
    }

    @HLEFunction(nid = 0xD59EAD2F, version = 150)
    public void sceKernelWakeupThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelWakeupThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            cpu.gpr[2] = 0;
            hleKernelWakeupThread(thread);
        }
    }

    @HLEFunction(nid = 0xFCCFAD26, version = 150)
    public void sceKernelCancelWakeupThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (uid == 0) {
            uid = currentThread.uid;
        }
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelCancelWakeupThread SceUID=" + Integer.toHexString(uid) + ") unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelCancelWakeupThread SceUID=" + Integer.toHexString(uid) + ") wakeupCount=" + thread.wakeupCount);
            }
            cpu.gpr[2] = thread.wakeupCount;
            thread.wakeupCount = 0;
        }
    }

    @HLEFunction(nid = 0x9944F31F, version = 150)
    public void sceKernelSuspendThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (uid == currentThread.uid) {
            log.warn("sceKernelSuspendThread on self is not allowed");
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_THREAD;
        } else if (thread.isSuspended()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceKernelSuspendThread thread already suspended: thread=%s", thread.toString()));
        	}
        	cpu.gpr[2] = ERROR_KERNEL_THREAD_ALREADY_SUSPEND;
        } else if (thread.isStopped()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceKernelSuspendThread thread already stopped: thread=%s", thread.toString()));
        	}
        	cpu.gpr[2] = ERROR_KERNEL_THREAD_ALREADY_DORMANT;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelSuspendThread SceUID=" + Integer.toHexString(uid));
            }
            if (thread.isWaiting()) {
            	hleChangeThreadState(thread, PSP_THREAD_WAITING_SUSPEND);
            } else {
            	hleChangeThreadState(thread, PSP_THREAD_SUSPEND);
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x75156E8F, version = 150)
    public void sceKernelResumeThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " unknown thread");
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (!thread.isSuspended()) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " not suspended (status=" + thread.status + ")");
            cpu.gpr[2] = ERROR_KERNEL_THREAD_IS_NOT_SUSPEND;
        } else if (isBannedThread(thread)) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not resuming");
            cpu.gpr[2] = 0;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "'");
            }
            if (thread.isWaiting()) {
                hleChangeThreadState(thread, PSP_THREAD_WAITING);
            } else {
            	hleChangeThreadState(thread, PSP_THREAD_READY);
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x278C0DF5, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelWaitThreadEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int timeout_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitThreadEnd redirecting to hleKernelWaitThreadEnd(callbacks=false)");
        }

        hleKernelWaitThreadEnd(uid, timeout_addr, false);
    }

    @HLEFunction(nid = 0x840E8133, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelWaitThreadEndCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int timeout_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitThreadEndCB redirecting to hleKernelWaitThreadEnd(callbacks=true)");
        }

        hleKernelWaitThreadEnd(uid, timeout_addr, true);
        checkCallbacks();
    }

    /** wait the current thread for a certain number of microseconds */
    @HLEFunction(nid = 0xCEADEB47, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelDelayThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int micros = cpu.gpr[4];

        hleKernelDelayThread(micros, false);
    }

    /** wait the current thread for a certain number of microseconds */
    @HLEFunction(nid = 0x68DA9E36, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelDelayThreadCB(Processor processor) {
        CpuState cpu = processor.cpu;

        int micros = cpu.gpr[4];

        hleKernelDelayThread(micros, true);
    }

    /**
     * Delay the current thread by a specified number of sysclocks
     *
     * @param sysclocks_addr - Address of delay in sysclocks
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xBD123D9E, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelDelaySysClockThread(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int sysclocks_addr = cpu.gpr[4];

        if (Memory.isAddressGood(sysclocks_addr)) {
            long sysclocks = mem.read64(sysclocks_addr);
            int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
            hleKernelDelayThread(micros, false);
        } else {
            log.warn("sceKernelDelaySysClockThread invalid sysclocks address 0x" + Integer.toHexString(sysclocks_addr));
            cpu.gpr[2] = -1;
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
    @HLEFunction(nid = 0x1181E963, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelDelaySysClockThreadCB(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int sysclocks_addr = cpu.gpr[4];

        if (Memory.isAddressGood(sysclocks_addr)) {
            long sysclocks = mem.read64(sysclocks_addr);
            int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
            hleKernelDelayThread(micros, true);
        } else {
            log.warn("sceKernelDelaySysClockThreadCB invalid sysclocks address 0x" + Integer.toHexString(sysclocks_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0xD6DA4BA1, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateSema(int name_addr, int attr, int initVal, int maxVal, int option) {
        return Managers.semas.sceKernelCreateSema(name_addr, attr, initVal, maxVal, option);
    }

    @HLEFunction(nid = 0x28B6489C, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteSema(int semaid) {
        return Managers.semas.sceKernelDeleteSema(semaid);
    }

    @HLEFunction(nid = 0x3F53E640, version = 150)
    public int sceKernelSignalSema(int semaid, int signal) {
        return Managers.semas.sceKernelSignalSema(semaid, signal);
    }

    @HLEFunction(nid = 0x4E3A1105, version = 150, checkInsideInterrupt = true)
    public int sceKernelWaitSema(int semaid, int signal, int timeout_addr) {
        return Managers.semas.sceKernelWaitSema(semaid, signal, timeout_addr);
    }

    @HLEFunction(nid = 0x6D212BAC, version = 150, checkInsideInterrupt = true)
    public int sceKernelWaitSemaCB(int semaid, int signal, int timeout_addr) {
        return Managers.semas.sceKernelWaitSemaCB(semaid, signal, timeout_addr);
    }

    @HLEFunction(nid = 0x58B1F937, version = 150)
    public int sceKernelPollSema(int semaid, int signal) {
        return Managers.semas.sceKernelPollSema(semaid, signal);
    }

    @HLEFunction(nid = 0x8FFDF9A2, version = 150)
    public int sceKernelCancelSema(int semaid, int newcount, int numWaitThreadAddr) {
        return Managers.semas.sceKernelCancelSema(semaid, newcount, numWaitThreadAddr);
    }

    @HLEFunction(nid = 0xBC6FEBC5, version = 150)
    public int sceKernelReferSemaStatus(int semaid, int addr) {
        return Managers.semas.sceKernelReferSemaStatus(semaid, addr);
    }

    @HLEFunction(nid = 0x55C20A00, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateEventFlag(int name_addr, int attr, int initPattern, int option) {
        return Managers.eventFlags.sceKernelCreateEventFlag(name_addr, attr, initPattern, option);
    }

    @HLEFunction(nid = 0xEF9E4C70, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteEventFlag(int uid) {
        return Managers.eventFlags.sceKernelDeleteEventFlag(uid);
    }

    @HLEFunction(nid = 0x1FB15A32, version = 150)
    public int sceKernelSetEventFlag(int uid, int bitsToSet) {
        return Managers.eventFlags.sceKernelSetEventFlag(uid, bitsToSet);
    }

    @HLEFunction(nid = 0x812346E4, version = 150)
    public int sceKernelClearEventFlag(int uid, int bitsToKeep) {
        return Managers.eventFlags.sceKernelClearEventFlag(uid, bitsToKeep);
    }

    @HLEFunction(nid = 0x402FCF22, version = 150, checkInsideInterrupt = true)
    public int sceKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr) {
        return Managers.eventFlags.sceKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr);
    }

    @HLEFunction(nid = 0x328C546A, version = 150, checkInsideInterrupt = true)
    public int sceKernelWaitEventFlagCB(int uid, int bits, int wait, int outBits_addr, int timeout_addr) {
        return Managers.eventFlags.sceKernelWaitEventFlagCB(uid, bits, wait, outBits_addr, timeout_addr);
    }

    @HLEFunction(nid = 0x30FD48F0, version = 150)
    public int sceKernelPollEventFlag(int uid, int bits, int wait, int outBits_addr) {
        return Managers.eventFlags.sceKernelPollEventFlag(uid, bits, wait, outBits_addr);
    }

    @HLEFunction(nid = 0xCD203292, version = 150)
    public int sceKernelCancelEventFlag(int uid, int newPattern, int numWaitThreadAddr) {
        return Managers.eventFlags.sceKernelCancelEventFlag(uid, newPattern, numWaitThreadAddr);
    }

    @HLEFunction(nid = 0xA66B0120, version = 150)
    public int sceKernelReferEventFlagStatus(int uid, int addr) {
        return Managers.eventFlags.sceKernelReferEventFlagStatus(uid, addr);
    }

    @HLEFunction(nid = 0x8125221D, version = 150, checkInsideInterrupt = true)
    public void sceKernelCreateMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelCreateMbx(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0x86255ADA, version = 150, checkInsideInterrupt = true)
    public void sceKernelDeleteMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelDeleteMbx(cpu.gpr[4]);
    }

    @HLEFunction(nid = 0xE9B3061E, version = 150)
    public void sceKernelSendMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelSendMbx(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0x18260574, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelReceiveMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelReceiveMbx(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0xF3986382, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelReceiveMbxCB(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelReceiveMbxCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0x0D81716A, version = 150)
    public void sceKernelPollMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelPollMbx(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0x87D4DD36, version = 150)
    public void sceKernelCancelReceiveMbx(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelCancelReceiveMbx(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0xA8E8C846, version = 150)
    public void sceKernelReferMbxStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.mbx.sceKernelReferMbxStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0x7C0DC2A0, version = 150, checkInsideInterrupt = true)
    public void sceKernelCreateMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelCreateMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    @HLEFunction(nid = 0xF0B7DA1C, version = 150, checkInsideInterrupt = true)
    public void sceKernelDeleteMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelDeleteMsgPipe(cpu.gpr[4]);
    }

    @HLEFunction(nid = 0x876DBFAD, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelSendMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelSendMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    @HLEFunction(nid = 0x7C41F2C2, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelSendMsgPipeCB(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelSendMsgPipeCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    @HLEFunction(nid = 0x884C9F90, version = 150)
    public void sceKernelTrySendMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelTrySendMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    @HLEFunction(nid = 0x74829B76, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelReceiveMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelReceiveMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    @HLEFunction(nid = 0xFBFA697D, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelReceiveMsgPipeCB(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelReceiveMsgPipeCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    @HLEFunction(nid = 0xDF52098F, version = 150)
    public void sceKernelTryReceiveMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelTryReceiveMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    @HLEFunction(nid = 0x349B864D, version = 150)
    public void sceKernelCancelMsgPipe(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelCancelMsgPipe(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0x33BE4024, version = 150)
    public void sceKernelReferMsgPipeStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.msgPipes.sceKernelReferMsgPipeStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0x56C039B5, version = 150, checkInsideInterrupt = true)
    public void sceKernelCreateVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelCreateVpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8]);
    }

    @HLEFunction(nid = 0x89B3D48C, version = 150, checkInsideInterrupt = true)
    public void sceKernelDeleteVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelDeleteVpl(cpu.gpr[4]);
    }

    @HLEFunction(nid = 0xBED27435, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelAllocateVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelAllocateVpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    @HLEFunction(nid = 0xEC0A693F, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelAllocateVplCB(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelAllocateVplCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    @HLEFunction(nid = 0xAF36D708, version = 150)
    public void sceKernelTryAllocateVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelTryAllocateVpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0xB736E9FF, version = 150, checkInsideInterrupt = true)
    public void sceKernelFreeVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelFreeVpl(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0x1D371B8A, version = 150)
    public void sceKernelCancelVpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelCancelVpl(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0x39810265, version = 150)
    public void sceKernelReferVplStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.vpl.sceKernelReferVplStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0xC07BB470, version = 150, checkInsideInterrupt = true)
    public void sceKernelCreateFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelCreateFpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]);
    }

    @HLEFunction(nid = 0xED1410E0, version = 150, checkInsideInterrupt = true)
    public void sceKernelDeleteFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelDeleteFpl(cpu.gpr[4]);
    }

    @HLEFunction(nid = 0xD979E9BF, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelAllocateFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelAllocateFpl(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0xE7282CB6, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public void sceKernelAllocateFplCB(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelAllocateFplCB(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0x623AE665, version = 150)
    public void sceKernelTryAllocateFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelTryAllocateFpl(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0xF6414A71, version = 150, checkInsideInterrupt = true)
    public void sceKernelFreeFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelFreeFpl(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0xA8AA591F, version = 150)
    public void sceKernelCancelFpl(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelCancelFpl(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0xD8199E4C, version = 150)
    public void sceKernelReferFplStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.fpl.sceKernelReferFplStatus(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0x0E927AED, version = 150)
    public void _sceKernelReturnFromTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented _sceKernelReturnFromTimerHandler");

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x110DEC9A, version = 150)
    public void sceKernelUSec2SysClock(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelUSec2SysClock(cpu.gpr[4], cpu.gpr[5]);
    }

    @HLEFunction(nid = 0xC8CD158C, version = 150)
    public void sceKernelUSec2SysClockWide(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelUSec2SysClockWide(cpu.gpr[4]);
    }

    @HLEFunction(nid = 0xBA6B92E2, version = 150)
    public void sceKernelSysClock2USec(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelSysClock2USec(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    @HLEFunction(nid = 0xE1619D7C, version = 150)
    public void sceKernelSysClock2USecWide(Processor processor) {
        CpuState cpu = processor.cpu;
        Managers.systime.sceKernelSysClock2USecWide(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    @HLEFunction(nid = 0xDB738F35, version = 150)
    public int sceKernelGetSystemTime(TPointer64 time_addr) {
        return Managers.systime.sceKernelGetSystemTime(time_addr);
    }

    @HLEFunction(nid = 0x82BC5777, version = 150)
    public long sceKernelGetSystemTimeWide() {
        return Managers.systime.sceKernelGetSystemTimeWide();
    }

    @HLEFunction(nid = 0x369ED59D, version = 150)
    public int sceKernelGetSystemTimeLow() {
        return Managers.systime.sceKernelGetSystemTimeLow();
    }

    /**
     * Set an alarm.
     * @param delayUsec - The number of micro seconds till the alarm occurs.
     * @param handlerAddress - Pointer to a ::SceKernelAlarmHandler
     * @param handlerArgument - Common pointer for the alarm handler
     *
     * @return A UID representing the created alarm, < 0 on error.
     */
    @HLEFunction(nid = 0x6652B8CA, version = 150)
    public void sceKernelSetAlarm(Processor processor) {
        CpuState cpu = processor.cpu;

        int delayUsec = cpu.gpr[4];
        int handlerAddress = cpu.gpr[5];
        int handlerArgument = cpu.gpr[6];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetAlarm(%d,0x%08X,0x%08X)", delayUsec, handlerAddress, handlerArgument));
        }

        hleKernelSetAlarm(processor, delayUsec, handlerAddress, handlerArgument);
    }

    /**
     * Set an alarm using a ::SceKernelSysClock structure for the time
     *
     * @param delaySysclockAddr - Pointer to a ::SceKernelSysClock structure
     * @param handlerAddress - Pointer to a ::SceKernelAlarmHandler
     * @param handlerArgument - Common pointer for the alarm handler.
     *
     * @return A UID representing the created alarm, < 0 on error.
     */
    @HLEFunction(nid = 0xB2C25152, version = 150)
    public void sceKernelSetSysClockAlarm(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int delaySysclockAddr = cpu.gpr[4];
        int handlerAddress = cpu.gpr[5];
        int handlerArgument = cpu.gpr[6];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetSysClockAlarm(0x%08X,0x%08X,0x%08X)", delaySysclockAddr, handlerAddress, handlerArgument));
        }

        if (Memory.isAddressGood(delaySysclockAddr)) {
            long delaySysclock = mem.read64(delaySysclockAddr);
            long delayUsec = SystemTimeManager.hleSysClock2USec(delaySysclock);

            hleKernelSetAlarm(processor, delayUsec, handlerAddress, handlerArgument);
        } else {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        }
    }

    /**
     * Cancel a pending alarm.
     *
     * @param alarmUid - UID of the alarm to cancel.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x7E65B999, version = 150)
    public void sceKernelCancelAlarm(Processor processor) {
        CpuState cpu = processor.cpu;

        int alarmUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCancelAlarm(uid=0x%x)", alarmUid));
        }

        SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        if (sceKernelAlarmInfo == null) {
            log.warn(String.format("sceKernelCancelAlarm unknown uid=0x%x", alarmUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_ALARM;
        } else {
            cancelAlarm(sceKernelAlarmInfo);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Refer the status of a created alarm.
     *
     * @param alarmUid - UID of the alarm to get the info of
     * @param infoAddr - Pointer to a ::SceKernelAlarmInfo structure
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xDAA3F564, version = 150)
    public void sceKernelReferAlarmStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int alarmUid = cpu.gpr[4];
        int infoAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelReferAlarmStatus(uid=0x%x, infoAddr=0x%08X)", alarmUid, infoAddr));
        }

        SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        if (sceKernelAlarmInfo == null) {
            log.warn(String.format("sceKernelReferAlarmStatus unknown uid=0x%x", alarmUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_ALARM;
        } else if (!Memory.isAddressGood(infoAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            int size = mem.read32(infoAddr);
            sceKernelAlarmInfo.size = size;
            sceKernelAlarmInfo.write(mem, infoAddr);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Create a virtual timer
     *
     * @param nameAddr - Name for the timer.
     * @param optAddr  - Pointer to an ::SceKernelVTimerOptParam (pass NULL)
     *
     * @return The VTimer's UID or < 0 on error.
     */
    @HLEFunction(nid = 0x20FFF560, version = 150, checkInsideInterrupt = true)
    public void sceKernelCreateVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int nameAddr = cpu.gpr[4];
        int optAddr = cpu.gpr[5];
        String name = Utilities.readStringZ(nameAddr);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCreateVTimer(name=%s(0x%08X), optAddr=0x%08X)", name, nameAddr, optAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = new SceKernelVTimerInfo(name);
        vtimers.put(sceKernelVTimerInfo.uid, sceKernelVTimerInfo);
        cpu.gpr[2] = sceKernelVTimerInfo.uid;
    }

    /**
     * Delete a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error.
     */
    @HLEFunction(nid = 0x328F9E52, version = 150, checkInsideInterrupt = true)
    public void sceKernelDeleteVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelDeleteVTimer(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.remove(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelDeleteVTimer unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            sceKernelVTimerInfo.delete();
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer base
     *
     * @param vtimerUid - UID of the vtimer
     * @param baseAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xB3A59970, version = 150)
    public void sceKernelGetVTimerBase(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int baseAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerBase(uid=0x%x,baseAddr=0x%08X)", vtimerUid, baseAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerBase unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(baseAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            mem.write64(baseAddr, sceKernelVTimerInfo.base);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer base (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer base
     */
    @HLEFunction(nid = 0xB7C18B77, version = 150)
    public void sceKernelGetVTimerBaseWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerBaseWide(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerBaseWide unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            Utilities.returnRegister64(cpu, sceKernelVTimerInfo.base);
        }
    }

    /**
     * Get the timer time
     *
     * @param vtimerUid - UID of the vtimer
     * @param timeAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x034A921F, version = 150)
    public void sceKernelGetVTimerTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int timeAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerTime(uid=0x%x,timeAddr=0x%08X)", vtimerUid, timeAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerTime unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(timeAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            long time = getVTimerTime(sceKernelVTimerInfo);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelGetVTimerTime returning %d", time));
            }
            mem.write64(timeAddr, time);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer time
     */
    @HLEFunction(nid = 0xC0B3FFD2, version = 150)
    public void sceKernelGetVTimerTimeWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerTimeWide(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelGetVTimerTimeWide unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            long time = getVTimerTime(sceKernelVTimerInfo);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelGetVTimerTimeWide returning %d", time));
            }
            Utilities.returnRegister64(cpu, time);
        }
    }

    /**
     * Set the timer time
     *
     * @param vtimerUid - UID of the vtimer
     * @param timeAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x542AD630, version = 150, checkInsideInterrupt = true)
    public void sceKernelSetVTimerTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int timeAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetVTimerTime(uid=0x%x,timeAddr=0x%08X)", vtimerUid, timeAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerTime unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(timeAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            long time = mem.read64(timeAddr);
            setVTimer(sceKernelVTimerInfo, time);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Set the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     * @param time - a ::SceKernelSysClock structure
     *
     * @return Possibly the last time
     */
    @HLEFunction(nid = 0xFB6425C3, version = 150, checkInsideInterrupt = true)
    public void sceKernelSetVTimerTimeWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        // cpu.gpr[5] not used!
        long time = Utilities.getRegister64(cpu, 6);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetVTimerTime(uid=0x%x,time=0x%016X)", vtimerUid, time));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerTime unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            setVTimer(sceKernelVTimerInfo, time);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Start a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    @HLEFunction(nid = 0xC68D9437, version = 150)
    public void sceKernelStartVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelStartVTimer(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelStartVTimer unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
                cpu.gpr[2] = 1; // already started
            } else {
                startVTimer(sceKernelVTimerInfo);
                cpu.gpr[2] = 0;
            }
        }
    }

    /**
     * Stop a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    @HLEFunction(nid = 0xD0AEEE87, version = 150)
    public void sceKernelStopVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelStopVTimer(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelStopVTimer unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_STOPPED) {
                cpu.gpr[2] = 0; // already stopped
            } else {
                stopVTimer(sceKernelVTimerInfo);
                cpu.gpr[2] = 1;
            }
        }
    }

    /**
     * Set the timer handler
     *
     * @param vtimerUid - UID of the vtimer
     * @param scheduleAddr - Time to call the handler
     * @param handlerAddress - The timer handler
     * @param handlerArgument  - Common pointer
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xD8B299AE, version = 150)
    public void sceKernelSetVTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int scheduleAddr = cpu.gpr[5];
        int handlerAddress = cpu.gpr[6];
        int handlerArgument = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.warn(String.format("sceKernelSetVTimerHandler(uid=0x%x,scheduleAddr=0x%08X,handlerAddress=0x%08X,handlerArgument=0x%08X)", vtimerUid, scheduleAddr, handlerAddress, handlerArgument));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerHandler unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(scheduleAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            long schedule = mem.read64(scheduleAddr);
            sceKernelVTimerInfo.handlerAddress = handlerAddress;
            sceKernelVTimerInfo.handlerArgument = handlerArgument;
            if (handlerAddress != 0) {
                scheduleVTimer(sceKernelVTimerInfo, schedule);
            }
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Set the timer handler (wide mode)
     *
     * @param vtimerUid - UID of the vtimer
     * @param schedule - Time to call the handler
     * @param handlerAddress - The timer handler
     * @param handlerArgument  - Common pointer
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x53B00E9A, version = 150)
    public void sceKernelSetVTimerHandlerWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        // cpu.gpr[5] not used!
        long schedule = Utilities.getRegister64(cpu, 6);
        int handlerAddress = cpu.gpr[8];
        int handlerArgument = cpu.gpr[9];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelSetVTimerHandlerWide(uid=0x%x,schedule=0x%016X,handlerAddress=0x%08X,handlerArgument=0x%08X)", vtimerUid, schedule, handlerAddress, handlerArgument));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelSetVTimerHandler unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            sceKernelVTimerInfo.handlerAddress = handlerAddress;
            sceKernelVTimerInfo.handlerArgument = handlerArgument;
            if (handlerAddress != 0) {
                scheduleVTimer(sceKernelVTimerInfo, schedule);
            }
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Cancel the timer handler
     *
     * @param vtimerUid - The UID of the vtimer
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xD2D615EF, version = 150)
    public void sceKernelCancelVTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCancelVTimerHandler(uid=0x%x)", vtimerUid));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelCancelVTimerHandler unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else {
            cancelVTimer(sceKernelVTimerInfo);
            cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the status of a VTimer
     *
     * @param vtimerUid - The uid of the VTimer
     * @param infoAddr - Pointer to a ::SceKernelVTimerInfo structure
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x5F32BEAA, version = 150)
    public void sceKernelReferVTimerStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int vtimerUid = cpu.gpr[4];
        int infoAddr = cpu.gpr[5];
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelReferVTimerStatus(uid=0x%x,infoAddr=0x%08X)", vtimerUid, infoAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
            log.warn(String.format("sceKernelReferVTimerStatus unknown uid=0x%x", vtimerUid));
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_VTIMER;
        } else if (!Memory.isAddressGood(infoAddr)) {
            cpu.gpr[2] = ERROR_KERNEL_ILLEGAL_ADDR;
        } else {
            int size = mem.read32(infoAddr);
            sceKernelVTimerInfo.size = size;
            sceKernelVTimerInfo.write(mem, infoAddr);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x446D8DE6, version = 150)
    public int sceKernelCreateThread(@StringInfo(maxLength = 32) String name, int entry_addr, int initPriority, int stackSize, int attr, int option_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateThread redirecting to hleKernelCreateThread");
        }
        SceKernelThreadInfo thread = hleKernelCreateThread(name, entry_addr, initPriority, stackSize, attr, option_addr);

        if (thread.stackSize > 0 && thread.getStackAddr() == 0) {
            log.warn("sceKernelCreateThread not enough memory to create the stack");
            hleDeleteThread(thread);
            return SceKernelErrors.ERROR_KERNEL_NO_MEMORY;
        }

        // Inherit kernel mode if user mode bit is not set
        if (currentThread.isKernelMode() && !SceKernelThreadInfo.isUserMode(thread.attr)) {
            log.debug("sceKernelCreateThread inheriting kernel mode");
            thread.attr |= PSP_THREAD_ATTR_KERNEL;
        }

        // Inherit user mode
        if (currentThread.isUserMode()) {
            if (!SceKernelThreadInfo.isUserMode(thread.attr)) {
                log.debug("sceKernelCreateThread inheriting user mode");
            }
            thread.attr |= PSP_THREAD_ATTR_USER;
            // Always remove kernel mode bit
            thread.attr &= ~PSP_THREAD_ATTR_KERNEL;
        }
        //int result = thread.uid;

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_CREATE);
        return thread.uid;
    }

    /** mark a thread for deletion. */
    @HLEFunction(nid = 0x9FA03CD3, version = 150, checkInsideInterrupt = true)
    public void sceKernelDeleteThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        if (uid == 0) {
            uid = currentThread.uid;
        }
        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (!thread.isStopped()) {
            cpu.gpr[2] = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug("sceKernelDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
        	}
            // Mark thread for deletion
            setToBeDeletedThread(thread);
            cpu.gpr[2] = 0;

            triggerThreadEvent(thread, currentThread, THREAD_EVENT_DELETE);
        }
    }

    @HLEFunction(nid = 0xF475845D, version = 150, checkInsideInterrupt = true)
    public void sceKernelStartThread(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];
        int len = cpu.gpr[5];
        int data_addr = cpu.gpr[6];

        if (!checkThreadID(uid)) {
            return;
        }
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            cpu.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
        } else if (isBannedThread(thread)) {
            log.warn("sceKernelStartThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' banned, not starting");
            // Banned, fake start.
            cpu.gpr[2] = 0;
            hleRescheduleCurrentThread();
        } else if (!thread.isStopped()) {
            cpu.gpr[2] = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        } else {
        	// Remember the currentThread as it might be changed when starting the thread
            SceKernelThreadInfo callingThread = currentThread;

            log.debug("sceKernelStartThread redirecting to hleKernelStartThread");
            cpu.gpr[2] = 0;
            hleKernelStartThread(thread, len, data_addr, thread.gpReg_addr);
            thread.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT; // Update the exit status.

            triggerThreadEvent(thread, callingThread, THREAD_EVENT_START);
        }
    }

    @HLEFunction(nid = 0x532A522E, version = 150)
    public void _sceKernelExitThread(Processor processor, int exitStatus) {
        // _sceKernelExitThread is equivalent to sceKernelExitThread
        sceKernelExitThread(processor, exitStatus);
    }

    /** exit the current thread */
    @HLEFunction(nid = 0xAA73C935, version = 150, checkInsideInterrupt = true)
    public void sceKernelExitThread(Processor processor, int exitStatus) {
        SceKernelThreadInfo thread = currentThread;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelExitThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));
        }

        if (exitStatus < 0) {
            thread.exitStatus = ERROR_KERNEL_ILLEGAL_ARGUMENT;
        } else {
            thread.exitStatus = exitStatus;
        }

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);
        
        processor.parameterReader.setReturnValueInt(0);

        hleChangeThreadState(thread, PSP_THREAD_STOPPED);
        RuntimeContext.onThreadExit(thread);
        hleRescheduleCurrentThread();
    }

    /** exit the current thread, then delete it */
    @HLEFunction(nid = 0x809CE29B, version = 150, checkInsideInterrupt = true)
    public void sceKernelExitDeleteThread(Processor processor, int exitStatus) {
        SceKernelThreadInfo thread = currentThread;
        if (log.isDebugEnabled()) {
            log.debug("sceKernelExitDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "' exitStatus:0x" + Integer.toHexString(exitStatus));
        }

        thread.exitStatus = exitStatus;

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);
        triggerThreadEvent(thread, currentThread, THREAD_EVENT_DELETE);
        
        processor.parameterReader.setReturnValueInt(0);

        hleChangeThreadState(thread, PSP_THREAD_STOPPED);
        RuntimeContext.onThreadExit(thread);
        setToBeDeletedThread(thread);
        hleRescheduleCurrentThread();
    }

    /** terminate thread */
    @HLEFunction(nid = 0x616403BA, version = 150)
    public void sceKernelTerminateThread(Processor processor, int uid) {
        if (!checkThreadID(uid)) {
            return;
        }

        SceKernelThreadInfo thread = getThreadCurrentIsInvalid(uid);

        log.debug("sceKernelTerminateThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);

        processor.parameterReader.setReturnValueInt(0);
        terminateThread(thread);
        thread.exitStatus = ERROR_KERNEL_THREAD_IS_TERMINATED; // Update the exit status.
    }

    /** terminate thread, then mark it for deletion */
    @HLEFunction(nid = 0x383F7BCC, version = 150, checkInsideInterrupt = true)
    public void sceKernelTerminateDeleteThread(Processor processor, int uid) {
        if (!checkThreadID(uid)) {
            return;
        }
        
        SceKernelThreadInfo thread = getThreadCurrentIsInvalid(uid);

        log.debug("sceKernelTerminateDeleteThread SceUID=" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);
        triggerThreadEvent(thread, currentThread, THREAD_EVENT_DELETE);

        processor.parameterReader.setReturnValueInt(0);
        terminateThread(thread);
        setToBeDeletedThread(thread);
    }

    /**
     * Suspend the dispatch thread
     *
     * @return The current state of the dispatch thread, < 0 on error
     */
    @HLEFunction(nid = 0x3AD58B8C, version = 150, checkInsideInterrupt = true)
    public int sceKernelSuspendDispatchThread() {
        int state = getDispatchThreadState();

        if (log.isDebugEnabled()) {
            log.debug("sceKernelSuspendDispatchThread() state=" + state);
        }

        if (Interrupts.isInterruptsDisabled()) {
            return SceKernelErrors.ERROR_KERNEL_INTERRUPTS_ALREADY_DISABLED;
        }

        dispatchThreadEnabled = false;
        return state;
    }

    /**
     * Resume the dispatch thread
     *
     * @param state - The state of the dispatch thread
     *                (from sceKernelSuspendDispatchThread)
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x27E22EC2, version = 150, checkInsideInterrupt = true)
    public int sceKernelResumeDispatchThread(int state) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelResumeDispatchThread(state=" + state + ")");
        }
        
        boolean isInterruptsDisabled = Interrupts.isInterruptsDisabled(); 

        if (state == SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED) {
            dispatchThreadEnabled = true;
            hleRescheduleCurrentThread();
        }

        if (isInterruptsDisabled) {
            return SceKernelErrors.ERROR_KERNEL_INTERRUPTS_ALREADY_DISABLED;
        }

        return 0;
    }

    @HLEFunction(nid = 0xEA748E31, version = 150)
    public int sceKernelChangeCurrentThreadAttr(int removeAttr, int addAttr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelChangeCurrentThreadAttr" + " removeAttr:0x" + Integer.toHexString(removeAttr) + " addAttr:0x" + Integer.toHexString(addAttr) + " oldAttr:0x" + Integer.toHexString(currentThread.attr));
        }

        int newAttr = (currentThread.attr & ~removeAttr) | addAttr;
        // Don't allow switching into kernel mode!
        if (userCurrentThreadTryingToSwitchToKernelMode(newAttr)) {
            log.debug("sceKernelChangeCurrentThreadAttr forcing user mode");
            newAttr |= PSP_THREAD_ATTR_USER;
            newAttr &= ~PSP_THREAD_ATTR_KERNEL;
        }
        currentThread.attr = newAttr;
        
        return 0;
    }

    @HLEFunction(nid = 0x71BC9871, version = 150)
    public void sceKernelChangeThreadPriority(Processor processor, int uid, @CheckArgument("checkThreadPriority") int priority) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true);
        SceKernelThreadInfo thread = getThread(uid);

        if (thread.isStopped()) {
            log.warn("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid) + " newPriority:0x" + Integer.toHexString(priority) + " oldPriority:0x" + Integer.toHexString(thread.currentPriority) + " thread is stopped, ignoring");
            // Tested on PSP:
            // If the thread is stopped, it's current priority is replaced by it's initial priority.
            thread.currentPriority = thread.initPriority;
            throw(new SceKernelErrorException(ERROR_KERNEL_THREAD_ALREADY_DORMANT));
        }
        
        if (log.isDebugEnabled()) {
            log.debug("sceKernelChangeThreadPriority SceUID=" + Integer.toHexString(uid) + " newPriority:0x" + Integer.toHexString(priority) + " oldPriority:0x" + Integer.toHexString(thread.currentPriority));
        }
        processor.parameterReader.setReturnValueInt(0);
        hleKernelChangeThreadPriority(thread, priority);
    }

    public int checkThreadPriority(int priority) {
        // Priority 0 means priority of the calling thread
        if (priority == 0) {
            priority = currentThread.currentPriority;
        }

        if (currentThread.isUserMode()) {
        	// Value priority range in user mode: [8..119]
        	if (priority < 8 || priority >= 120) {
        		if (log.isDebugEnabled()) {
        			log.debug(String.format("checkThreadPriority priority:0x%x is outside of valid range in user mode", priority));
        		}
                throw(new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_PRIORITY));
        	}
        }

        if (currentThread.isKernelMode()) {
        	// Value priority range in user mode: [1..126]
        	if (priority < 1 || priority >= 127) {
        		if (log.isDebugEnabled()) {
        			log.debug(String.format("checkThreadPriority priority:0x%x is outside of valid range in kernel mode", priority));
        		}
                throw(new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_PRIORITY));
        	}
        }

        return priority;
    }
    
    protected SceKernelThreadInfo getThreadCurrentIsInvalid(int uid) {
    	SceKernelThreadInfo thread = getThread(uid);
    	if (thread == currentThread) {
            throw(new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_THREAD));
    	}
    	return thread;
    }
    
    protected SceKernelThreadInfo getThread(int uid) {
        if (uid == 0) {
            uid = currentThread.uid;
        }
        
        if (uid < 0) {
            log.warn(getCallingFunctionName(+1) + " SceUID=" + Integer.toHexString(uid) + " is invalid");
            throw(new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_THREAD));
        }
        
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn(getCallingFunctionName(+1) + " unknown uid=0x" + Integer.toHexString(uid));
            throw(new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_THREAD));
        }
        
        return thread;
    }

    /**
     * Rotate thread ready queue at a set priority
     *
     * @param priority - The priority of the queue
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x912354A7, version = 150)
    public int sceKernelRotateThreadReadyQueue(@CheckArgument("checkThreadPriority") int priority) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelRotateThreadReadyQueue priority=" + priority);
        }

        synchronized (readyThreads) {
            for (SceKernelThreadInfo thread : readyThreads) {
                if (thread.currentPriority == priority) {
                	// When rotating the ready queue of the current thread,
                	// the current thread yields and is moved to the end of its
                	// ready queue.
                	if (priority == currentThread.currentPriority) {
                		thread = currentThread;
                		// The current thread will be moved to the front of the ready queue
                		hleChangeThreadState(thread, PSP_THREAD_READY);
                	}
                    // Move the thread to the end of the ready queue
                	removeFromReadyThreads(thread);
                    addToReadyThreads(thread, false);
                    hleRescheduleCurrentThread();
                    break;
                }
            }
        }
        
        return 0;
    }

    @HLEFunction(nid = 0x2C34E053, version = 150)
    public int sceKernelReleaseWaitThread(int uid) {
    	if (uid == 0) {
        	throw(new SceKernelErrorException(SceKernelErrors.ERROR_KERNEL_ILLEGAL_THREAD));
    	}

        SceKernelThreadInfo thread = getThread(uid);

        if (thread == currentThread) {
        	if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelReleaseWaitThread(0x%X): can't release itself: %s", uid, thread));
        	}
        	return SceKernelErrors.ERROR_KERNEL_ILLEGAL_THREAD;
        }

        if (!thread.isWaiting()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelReleaseWaitThread(0x%X): thread not waiting: %s", uid, thread));
            }
            return SceKernelErrors.ERROR_KERNEL_THREAD_IS_NOT_WAIT;
        }

        // If the application is waiting on a internal condition,
        // return an illegal permission
        // (e.g. on a real PSP, it would be the case for a
        //  sceKernelWaitEventFlag issued internally by a syscall).
        if (thread.waitType >= SceKernelThreadInfo.JPCSP_FIRST_INTERNAL_WAIT_TYPE) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelReleaseWaitThread(0x%X): thread waiting in privileged status: waitType=0x%X", uid, thread.waitType));
            }
        	return SceKernelErrors.ERROR_KERNEL_ILLEGAL_PERMISSION;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelReleaseWaitThread(0x%X): releasing waiting thread: %s", uid, thread));
        }

        hleThreadWaitRelease(thread);

        // Check if we need to switch to the released thread
        // (e.g. has a higher priority)
        hleRescheduleCurrentThread();
        
        return 0;
    }

    /** Get the current thread Id */
    @HLEFunction(nid = 0x293B45B8, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetThreadId() {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadId returning uid=0x" + Integer.toHexString(currentThread.uid));
        }

        return currentThread.uid;
    }

    @HLEFunction(nid = 0x94AA61EE, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetThreadCurrentPriority(Processor processor) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadCurrentPriority returning currentPriority=" + currentThread.currentPriority);
        }

        return currentThread.currentPriority;
    }

    /** @return ERROR_NOT_FOUND_THREAD on uid < 0, uid == 0 and thread not found */
    @HLEFunction(nid = 0x3B183E26, version = 150)
    public int sceKernelGetThreadExitStatus(int uid) {
        SceKernelThreadInfo thread = getThread(uid);
        if (!thread.isStopped()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelGetThreadExitStatus not stopped uid=0x%x", uid));
            }
            return ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        }

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadExitStatus uid=0x" + Integer.toHexString(uid) + " exitStatus=0x" + Integer.toHexString(thread.exitStatus));
        }
        return thread.exitStatus;
    }

    /** @return amount of free stack space.*/
    @HLEFunction(nid = 0xD13BDE95, version = 150, checkInsideInterrupt = true)
    public int sceKernelCheckThreadStack(Processor processor) {
        int size = getThreadCurrentStackSize(processor);
        
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCheckThreadStack returning size=0x%X", size));
        }

        return size;
    }

    /**
     * @return amount of unused stack space of a thread.
     * */
    @HLEFunction(nid = 0x52089CA1, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetThreadStackFreeSize(int uid) {
    	SceKernelThreadInfo thread = getThread(uid);

    	// The stack is filled with 0xFF when the thread starts.
    	// Scan for the unused stack space by looking for the first 32-bit value
    	// differing from 0xFFFFFFFF.
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(thread.getStackAddr(), thread.stackSize, 4);
    	int unusedStackSize = thread.stackSize;
    	for (int i = 0; i < thread.stackSize; i += 4) {
    		int stackValue = memoryReader.readNext();
    		if (stackValue != -1) {
    			unusedStackSize = i;
    			break;
    		}
    	}

    	if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetThreadStackFreeSize returning size=0x%X", unusedStackSize));
        }

    	return unusedStackSize;
    }
    
    /** Get the status information for the specified thread
     **/
    @HLEFunction(nid = 0x17C1684E, version = 150)
    public int sceKernelReferThreadStatus(int uid, TPointer ptr) {
        if (log.isDebugEnabled()) {
            log.debug(
            	"sceKernelReferThreadStatus uid=0x" + Integer.toHexString(uid) +
            	" addr=0x" + Integer.toHexString(ptr.getAddress())
            );
        }
        
        SceKernelThreadInfo thread = getThread(uid);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelReferThreadStatus for thread %s", thread.toString()));
        }
        getThread(uid).write(ptr.getMemory(), ptr.getAddress());
        return 0;
    }

    @HLEFunction(nid = 0xFFC36A14, version = 150, checkInsideInterrupt = true)
    public int sceKernelReferThreadRunStatus(int uid, TPointer ptr) {
        if (log.isDebugEnabled()) {
            log.debug(
            	"sceKernelReferThreadRunStatus uid=0x" + Integer.toHexString(uid) +
            	" addr=0x" + Integer.toHexString(ptr.getAddress())
            );
        }

        getThread(uid).writeRunStatus(ptr.getMemory(), ptr.getAddress());
        
        return 0;
    }

    /**
     * Get the current system status.
     *
     * @param status - Pointer to a ::SceKernelSystemStatus structure.
     *
     * @return < 0 on error.
     */
    @HLEFunction(nid = 0x627E6F3A, version = 150)
    public int sceKernelReferSystemStatus(TPointer statusPtr) {
        SceKernelSystemStatus status = new SceKernelSystemStatus();
        status.read(statusPtr.getMemory(), statusPtr.getAddress());
        status.status = 0;
        status.write(statusPtr.getMemory(), statusPtr.getAddress());

        return 0;
    }

    /** Write uid's to buffer
     * return written count
     * save full count to idcount_addr */
    @HLEFunction(nid = 0x94416130, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetThreadmanIdList(int type, TPointer32 readBufPtr, int readBufSize, TPointer32 idCountPtr) {
        if (log.isDebugEnabled()) {
            log.debug(
            	"sceKernelGetThreadmanIdList type=" + type +
            	" readbuf:0x" + Integer.toHexString(readBufPtr.getAddress()) +
            	" readbufsize:" + readBufSize +
            	" idcount:0x" + Integer.toHexString(idCountPtr.getAddress())
            );
        }

        if (type != SCE_KERNEL_TMID_Thread) {
            log.warn("UNIMPLEMENTED:sceKernelGetThreadmanIdList type=" + type);
            idCountPtr.setValue(0);
            return 0;
        }

        int saveCount = 0;
        int fullCount = 0;

        for (SceKernelThreadInfo thread : threadMap.values()) {
            // Hide kernel mode threads when called from a user mode thread
            if (userThreadCalledKernelCurrentThread(thread)) {
                if (saveCount < readBufSize) {
                    if (log.isDebugEnabled()) {
                        log.debug("sceKernelGetThreadmanIdList adding thread '" + thread.name + "'");
                    }
                    readBufPtr.setValue(saveCount << 2, thread.uid);
                    saveCount++;
                } else {
                    log.warn("sceKernelGetThreadmanIdList NOT adding thread '" + thread.name + "' (no more space)");
                }
                fullCount++;
            }
        }

        idCountPtr.setValue(fullCount);

        return 0;
    }

    @HLEFunction(nid = 0x57CF62DD, version = 150)
    public int sceKernelGetThreadmanIdType(int uid) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetThreadmanIdType uid=0x" + uid);
        }

        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", false)) {
            return SCE_KERNEL_TMID_Thread;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-sema", false)) {
        	return SCE_KERNEL_TMID_Semaphore;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", false)) {
        	return SCE_KERNEL_TMID_EventFlag;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Mbx", false)) {
        	return SCE_KERNEL_TMID_Mbox;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Vpl", false)) {
        	return SCE_KERNEL_TMID_Vpl;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Fpl", false)) {
        	return SCE_KERNEL_TMID_Fpl;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-MsgPipe", false)) {
        	return SCE_KERNEL_TMID_Mpipe;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-callback", false)) {
        	return SCE_KERNEL_TMID_Callback;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-ThreadEventHandler", false)) {
        	return SCE_KERNEL_TMID_ThreadEventHandler;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Alarm", false)) {
        	return SCE_KERNEL_TMID_Alarm;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-VTimer", false)) {
        	return SCE_KERNEL_TMID_VTimer;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-Mutex", false)) {
        	return SCE_KERNEL_TMID_Mutex;
        }
        
        if (SceUidManager.checkUidPurpose(uid, "ThreadMan-LwMutex", false)) {
        	return SCE_KERNEL_TMID_LwMutex;
        }
        
        return SceKernelErrors.ERROR_KERNEL_ILLEGAL_ARGUMENT;
    }

    @HLEFunction(nid = 0x64D4540E, version = 150)
    public int sceKernelReferThreadProfiler() {
        // Can be safely ignored. Only valid in debug mode on a real PSP.
        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferThreadProfiler");
        }

        return 0;
    }

    @HLEFunction(nid = 0x8218B4DD, version = 150)
    public int sceKernelReferGlobalProfiler() {
        // Can be safely ignored. Only valid in debug mode on a real PSP.
        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferGlobalProfiler");
        }

        return 0;
    }

    public static class Statistics {

        private ArrayList<ThreadStatistics> threads = new ArrayList<ThreadStatistics>();
        public long allCycles = 0;
        public long startTimeMillis;
        public long endTimeMillis;
        public long allCpuMillis = 0;

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
        	if (!DurationStatistics.collectStatistics) {
        		return;
        	}

        	ThreadStatistics threadStatistics = new ThreadStatistics();
            threadStatistics.name = thread.name;
            threadStatistics.runClocks = thread.runClocks;
            threads.add(threadStatistics);

            allCycles += thread.runClocks;

            if (thread.javaThreadId > 0) {
	            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	            if (threadMXBean.isThreadCpuTimeEnabled()) {
	            	long threadCpuTimeNanos = thread.javaThreadCpuTimeNanos;
	            	if (threadCpuTimeNanos < 0) {
	            		threadCpuTimeNanos = threadMXBean.getThreadCpuTime(thread.javaThreadId);
	            	}
	            	if (threadCpuTimeNanos > 0) {
	            		allCpuMillis += threadCpuTimeNanos / 1000000L;
	            	}
	            }
            }
        }

        private static class ThreadStatistics implements Comparable<ThreadStatistics> {
            public String name;
            public long runClocks;

            @Override
			public int compareTo(ThreadStatistics o) {
				return -(new Long(runClocks).compareTo(o.runClocks));
			}

            public String getQuotedName() {
            	return "'" + name + "'";
            }
        }
    }

    public static class CallbackManager {

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

    public static class Callback {
        private int id;
        private int address;
        private int[] parameters;
        private int savedIdRegister;
        private int savedRa;
        private int savedPc;
        private int savedV0;
        private int savedV1;
        private IAction afterAction;
        private boolean returnVoid;

        public Callback(int id, int address, int[] parameters, IAction afterAction, boolean returnVoid) {
            this.id = id;
            this.address = address;
            this.parameters = parameters;
            this.afterAction = afterAction;
            this.returnVoid = returnVoid;
        }

        public IAction getAfterAction() {
            return afterAction;
        }

        public int getId() {
            return id;
        }

        public int getSavedIdRegister() {
            return savedIdRegister;
        }

        public int getSavedRa() {
            return savedRa;
        }

        public int getSavedPc() {
            return savedPc;
        }

		public int getSavedV0() {
			return savedV0;
		}

		public int getSavedV1() {
			return savedV1;
		}

		public boolean isReturnVoid() {
			return returnVoid;
		}

		public void execute(SceKernelThreadInfo thread) {
			CpuState cpu = thread.cpuContext;

			savedIdRegister = cpu.gpr[CALLBACKID_REGISTER];
	        savedRa = cpu.gpr[_ra];
	        savedPc = cpu.pc;
	        savedV0 = cpu.gpr[_v0];
	        savedV1 = cpu.gpr[_v1];

	        // Copy parameters ($a0, $a1, ...) to the cpu
	        if (parameters != null) {
	            System.arraycopy(parameters, 0, cpu.gpr, 4, parameters.length);
	        }

	        cpu.gpr[CALLBACKID_REGISTER] = id;
	        cpu.gpr[_ra] = CALLBACK_EXIT_HANDLER_ADDRESS;
	        cpu.pc = address;

	        RuntimeContext.executeCallback();
		}

		@Override
        public String toString() {
            return String.format("Callback address=0x%08X,id=%d,returnVoid=%b", address, getId(), isReturnVoid());
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
            this.threadWaitInfo = threadWaitInfo;
            this.doCallback = doCallback;
            this.afterAction = afterAction;
        }

        @Override
        public void execute() {
            boolean restoreWaitState = true;

            // After calling a callback, check if the waiting state of the thread
            // is still valid, i.e. if the thread must continue to wait or if the
            // wait condition has been reached.
            if (threadWaitInfo.waitStateChecker != null) {
                if (!threadWaitInfo.waitStateChecker.continueWaitState(thread, threadWaitInfo)) {
                    restoreWaitState = false;
                }
            }

            if (restoreWaitState) {
            	if (status == PSP_THREAD_RUNNING) {
            		doCallback = false;
            	}
                if (log.isDebugEnabled()) {
                    log.debug(String.format("AfterCallAction: restoring wait state for thread '%s' to %s, %s, doCallbacks %b", thread.toString(), SceKernelThreadInfo.getStatusName(status), SceKernelThreadInfo.getWaitName(waitType, threadWaitInfo, status), doCallback));
                }

                // Restore the wait state of the thread
                thread.waitType = waitType;
                thread.waitId = waitId;
                thread.wait.copy(threadWaitInfo);

                hleChangeThreadState(thread, status);
            } else if (thread.status != PSP_THREAD_READY) {
                if (log.isDebugEnabled()) {
                    log.debug("AfterCallAction: set thread to READY state: " + thread.toString());
                }

                hleChangeThreadState(thread, PSP_THREAD_READY);
                doCallback = false;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("AfterCallAction: leaving thread in READY state: " + thread.toString());
                }
                doCallback = false;
            }

        	thread.doCallbacks = doCallback;
            hleRescheduleCurrentThread();

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

    public class TimeoutThreadWaitStateChecker implements IWaitStateChecker {
		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
			// Waiting forever?
			if (wait.forever) {
				return true;
			}

			if (wait.microTimeTimeout <= Emulator.getClock().microTime()) {
				hleThreadWaitTimeout(thread);
				return false;
			}

			// The waitTimeoutAction has been deleted by hleChangeThreadState while
			// leaving the WAIT state. It has to be restored.
			if (wait.waitTimeoutAction == null) {
				wait.waitTimeoutAction = new TimeoutThreadAction(thread);
			}

			return true;
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

    public class WaitThreadEndWaitStateChecker implements IWaitStateChecker {
		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the thread
            // has exited during the callback execution.
			SceKernelThreadInfo threadEnd = getThreadById(wait.ThreadEnd_id);
			if (threadEnd == null) {
				// The thread has completely disappeared during the callback execution...
				thread.cpuContext.gpr[2] = ERROR_KERNEL_NOT_FOUND_THREAD;
				return false;
			}

			if (threadEnd.isStopped()) {
                // Return exit status of stopped thread
                thread.cpuContext.gpr[2] = threadEnd.exitStatus;
                return false;
			}

			return true;
		}
    }

}