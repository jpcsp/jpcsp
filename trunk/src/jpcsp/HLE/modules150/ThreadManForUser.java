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

import static jpcsp.Allegrex.Common._a0;
import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._s0;
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
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_AUDIO;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_CTRL;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_DISPLAY_VBLANK;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_GE_LIST;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_NET;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_UMD;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.JPCSP_WAIT_USB;
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
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.DumpDebugState;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
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
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.scheduler.Scheduler;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.DurationStatistics;

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
@HLELogging
public class ThreadManForUser extends HLEModule {
    public static Logger log = Modules.getLogger("ThreadManForUser");

    @Override
    public String getName() {
        return "ThreadManForUser";
    }

    private HashMap<Integer, SceKernelThreadInfo> threadMap;
    private HashMap<Integer, SceKernelThreadEventHandlerInfo> threadEventHandlers;
    private LinkedList<SceKernelThreadInfo> readyThreads;
    private SceKernelThreadInfo currentThread;
    private SceKernelThreadInfo idle0, idle1;
    public Statistics statistics;
    private boolean dispatchThreadEnabled;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_DISABLED = 0;
    private static final int SCE_KERNEL_DISPATCHTHREAD_STATE_ENABLED = 1;

    // The PSP seems to have a resolution of 200us
    protected static final int THREAD_DELAY_MINIMUM_MICROS = 200;

    protected static final int CALLBACKID_REGISTER = _s0;
    protected CallbackManager callbackManager = new CallbackManager();
    public static final int INTERNAL_THREAD_ADDRESS_START = MemoryMap.START_RAM;
    protected static final int IDLE_THREAD_ADDRESS = INTERNAL_THREAD_ADDRESS_START;
    public static final int THREAD_EXIT_HANDLER_ADDRESS = INTERNAL_THREAD_ADDRESS_START + 0x20;
    public static final int CALLBACK_EXIT_HANDLER_ADDRESS = INTERNAL_THREAD_ADDRESS_START + 0x30;
    public static final int ASYNC_LOOP_ADDRESS = INTERNAL_THREAD_ADDRESS_START + 0x40;
    public static final int NET_APCTL_LOOP_ADDRESS = INTERNAL_THREAD_ADDRESS_START + 0x60;
    public static final int NET_ADHOC_MATCHING_EVENT_LOOP_ADDRESS = INTERNAL_THREAD_ADDRESS_START + 0x80;
    public static final int NET_ADHOC_MATCHING_INPUT_LOOP_ADDRESS = INTERNAL_THREAD_ADDRESS_START + 0xA0;
    public static final int NET_ADHOC_CTL_LOOP_ADDRESS = INTERNAL_THREAD_ADDRESS_START + 0xC0;
    public static final int INTERNAL_THREAD_ADDRESS_END = INTERNAL_THREAD_ADDRESS_START + 0x100;
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
    protected SleepThreadWaitStateChecker sleepThreadWaitStateChecker;

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
        threadEventHandlers = new HashMap<Integer, SceKernelThreadEventHandlerInfo>();
        readyThreads = new LinkedList<SceKernelThreadInfo>();
        statistics = new Statistics();

        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();
        callbackManager.Initialize();

        // Reserve the memory user the internal handlers
        Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "ThreadMan-InternalHandlers", SysMemUserForUser.PSP_SMEM_Addr, 0x100, MemoryMap.START_RAM);
        installIdleThreads();
        installThreadExitHandler();
        installCallbackExitHandler();
        installAsyncLoopHandler();
        installNetApctlLoopHandler();
        installNetAdhocMatchingEventLoopHandler();
        installNetAdhocMatchingInputLoopHandler();
        installNetAdhocCtlLoopHandler();

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
		sleepThreadWaitStateChecker = new SleepThreadWaitStateChecker();

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
    public void Initialise(SceModule module, int entry_addr, int attr, String pspfilename, int moduleid, int gp, boolean fromSyscall) {
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
        if (log.isDebugEnabled()) {
        	log.debug(String.format("Creating root thread: entry=0x%08X, priority=%d, stackSize=0x%X, attr=0x%X", entry_addr, rootInitPriority, rootStackSize, attr));
        }
        currentThread = new SceKernelThreadInfo("root", entry_addr, rootInitPriority, rootStackSize, attr);
        currentThread.moduleid = moduleid;
        threadMap.put(currentThread.uid, currentThread);

        // Set user mode bit if kernel mode bit is not present
        if (!currentThread.isKernelMode()) {
            currentThread.attr |= PSP_THREAD_ATTR_USER;
        }

        // Setup args by copying them onto the stack
        hleKernelSetThreadArguments(currentThread, pspfilename);

        // Setup threads $gp
        currentThread.cpuContext._gp = gp;
        idle0.cpuContext._gp = gp;
        idle1.cpuContext._gp = gp;

        currentThread.status = PSP_THREAD_READY;

        // Switch in the thread
        currentThread.status = PSP_THREAD_RUNNING;
        currentThread.restoreContext();
    }

    public void hleKernelSetThreadArguments(SceKernelThreadInfo thread, String argument) {
    	// The game "Kamen Rider Climax Heroes OOO - ULJS00331" is expecting to receive
    	// an argumentSize 1 byte (0x00) larger than the real string...
    	// But this is breaking several other games: "Burnout Legends - ULES00125",
    	// "Assassin's Creed: Bloodlines - USA - ULUS10455"...
    	// So, keep the change now specific to ULJS00331 until we find out what is going on...
    	int extraBytes = "ULJS00331".equals(State.discId) ? 1 : 0;
    	int address = prepareThreadArguments(thread, argument.length() + 1 + extraBytes);
    	writeStringZ(Memory.getInstance(), address, argument);
    	if (extraBytes > 0) {
    		Memory.getInstance().memset(address + argument.length() + 1, (byte) 0, extraBytes);
    	}
    }

    public void hleKernelSetThreadArguments(SceKernelThreadInfo thread, byte[] argument, int argumentSize) {
    	int address = prepareThreadArguments(thread, argumentSize);
    	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, argumentSize, 1);
    	for (int i = 0; i < argumentSize; i++) {
    		memoryWriter.writeNext(argument[i] & 0xFF);
    	}
    	memoryWriter.flush();
    }

    public void hleKernelSetThreadArguments(SceKernelThreadInfo thread, int argumentAddr, int argumentSize) {
    	int address = prepareThreadArguments(thread, argumentAddr == 0 ? -1 : argumentSize);
    	if (argumentAddr != 0) {
    		Memory.getInstance().memcpy(address, argumentAddr, argumentSize);
    	}
    }

    private int prepareThreadArguments(SceKernelThreadInfo thread, int argumentSize) {
        // 256 bytes padding between user data top and real stack top
        int address = (thread.getStackAddr() + thread.stackSize - 0x100) - ((argumentSize + 0xF) & ~0xF);
        if (argumentSize < 0) {
            // Set the pointer to NULL when none is provided
            thread.cpuContext._a0 = 0; // a0 = user data len
            thread.cpuContext._a1 = 0; // a1 = pointer to arg data in stack
        } else {
            thread.cpuContext._a0 = argumentSize; // a0 = user data len
            thread.cpuContext._a1 = address; // a1 = pointer to arg data in stack
        }
        // 64 bytes padding between program stack top and user data
        thread.cpuContext._sp = address - 0x40;

        return address;
    }

    private void installIdleThreads() {
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
        SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "ThreadMan-RootMem", SysMemUserForUser.PSP_SMEM_Addr, 0x4000, MemoryMap.START_USERSPACE);
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

    private void installThreadExitHandler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f000 [hleKernelExitThread]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName("hleKernelExitThread").getSyscallCode() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(THREAD_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void installCallbackExitHandler() {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f001 [hleKernelExitCallback]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName("hleKernelExitCallback").getSyscallCode() & 0x000fffff) << 6);

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (31 << 21);

        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 0, instruction_syscall);
        mem.write32(CALLBACK_EXIT_HANDLER_ADDRESS + 4, instruction_jr);
    }

    private void installLoopHandler(String hleFunctionName, int address) {
        Memory mem = Memory.getInstance();

        int instruction_syscall = // syscall 0x6f002 [hleKernelAsyncLoop]
                ((AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (AllegrexOpcodes.SYSCALL & 0x3f) | ((this.getHleFunctionByName(hleFunctionName).getSyscallCode() & 0x000fffff) << 6);

        int instruction_b = (AllegrexOpcodes.BEQ << 26) | 0xFFFE; // branch back to syscall
        int instruction_nop = (AllegrexOpcodes.SLL << 26); // nop

        // Add a "jr $ra" instruction to indicate the end of the CodeBlock to the compiler
        int instruction_jr = AllegrexOpcodes.JR | (_ra << 21);

        mem.write32(address + 0, instruction_syscall);
        mem.write32(address + 4, instruction_b);
        mem.write32(address + 8, instruction_nop);
        mem.write32(address + 12, instruction_jr);
        mem.write32(address + 16, instruction_nop);
    }

    private void installAsyncLoopHandler() {
    	installLoopHandler("hleKernelAsyncLoop", ASYNC_LOOP_ADDRESS);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelAsyncLoop(Processor processor) {
        Modules.IoFileMgrForUserModule.hleAsyncThread(processor);
    }

    private void installNetApctlLoopHandler() {
    	installLoopHandler("hleKernelNetApctlLoop", NET_APCTL_LOOP_ADDRESS);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelNetApctlLoop(Processor processor) {
    	Modules.sceNetApctlModule.hleNetApctlThread(processor);
    }

    private void installNetAdhocMatchingEventLoopHandler() {
    	installLoopHandler("hleKernelNetAdhocMatchingEventLoop", NET_ADHOC_MATCHING_EVENT_LOOP_ADDRESS);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelNetAdhocMatchingEventLoop(Processor processor) {
    	Modules.sceNetAdhocMatchingModule.hleNetAdhocMatchingEventThread(processor);
    }

    private void installNetAdhocMatchingInputLoopHandler() {
    	installLoopHandler("hleKernelNetAdhocMatchingInputLoop", NET_ADHOC_MATCHING_INPUT_LOOP_ADDRESS);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelNetAdhocMatchingInputLoop(Processor processor) {
    	Modules.sceNetAdhocMatchingModule.hleNetAdhocMatchingInputThread(processor);
    }

    private void installNetAdhocCtlLoopHandler() {
    	installLoopHandler("hleKernelNetAdhocctlLoop", NET_ADHOC_CTL_LOOP_ADDRESS);
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelNetAdhocctlLoop(Processor processor) {
    	Modules.sceNetAdhocctlModule.hleNetAdhocctlThread(processor);
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

    public boolean isDispatchThreadEnabled() {
    	return dispatchThreadEnabled;
    }

    public SceKernelCallbackInfo getCallbackInfo(int uid) {
    	return callbackMap.get(uid);
    }

    public boolean isCurrentThreadStackAddress(int address) {
    	return currentThread.isStackAddress(address & Memory.addressMask);
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
    	hleKernelThreadEnterWaitState(currentThread, waitType, waitId, waitStateChecker, timeoutAddr, callbacks);
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
     * @param timeoutAddr      0 when the thread is waiting forever
     *                         otherwise, a valid address containing a timeout value
     *                         in microseconds.
     * @param callbacks        true if callback can be executed while waiting.
     *                         false if callback cannot be execute while waiting.
     */
    public void hleKernelThreadEnterWaitState(SceKernelThreadInfo thread, int waitType, int waitId, IWaitStateChecker waitStateChecker, int timeoutAddr, boolean callbacks) {
        int micros = 0;
        boolean forever = true;
        if (Memory.isAddressGood(timeoutAddr)) {
            micros = Memory.getInstance().read32(timeoutAddr);
            forever = false;
        }
    	hleKernelThreadEnterWaitState(thread, waitType, waitId, waitStateChecker, micros, forever, callbacks);
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

    private void hleBlockThread(SceKernelThreadInfo thread, int waitType, int waitId, boolean doCallbacks, IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
        if (!thread.isWaiting()) {
	    	thread.doCallbacks = doCallbacks;
	    	thread.wait.onUnblockAction = onUnblockAction;
	    	thread.waitType = waitType;
	    	thread.waitId = waitId;
	    	thread.wait.waitStateChecker = waitStateChecker;
	    	thread.wait.forever = true;
	        hleChangeThreadState(thread, thread.isSuspended() ? PSP_THREAD_WAITING_SUSPEND : PSP_THREAD_WAITING);
        }
    }

    public void hleBlockCurrentThread(int waitType, int waitId, boolean doCallbacks, IAction onUnblockAction, IWaitStateChecker waitStateChecker) {
        if (LOG_CONTEXT_SWITCHING && Modules.log.isDebugEnabled()) {
            log.debug("-------------------- block SceUID=" + Integer.toHexString(currentThread.uid) + " name:'" + currentThread.name + "' caller:" + getCallingFunction());
        }

    	hleBlockThread(currentThread, waitType, waitId, doCallbacks, onUnblockAction, waitStateChecker);
        hleRescheduleCurrentThread(doCallbacks);
    }

    public void hleBlockCurrentThread(int waitType) {
        hleBlockCurrentThread(waitType, 0, false, null, null);
    }

    public void hleBlockCurrentThread(int waitType, IAction onUnblockAction) {
    	hleBlockCurrentThread(waitType, 0, false, onUnblockAction, null);
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

            // Remove PSP_THREAD_WAITING from the thread state,
            // i.e. change the thread state
            // - from PSP_THREAD_WAITING_SUSPEND to PSP_THREAD_SUSPEND
            // - from PSP_THREAD_WAITING to PSP_THREAD_READY
            hleChangeThreadState(thread, thread.isSuspended() ? PSP_THREAD_SUSPEND : PSP_THREAD_READY);
        }
    }

    /** Call this when a thread's wait timeout has expired.
     * You can assume the calling function will set thread.status = ready. */
    private void onWaitTimeout(SceKernelThreadInfo thread) {
    	switch (thread.waitType) {
    		case PSP_WAIT_THREAD_END:
                // Return WAIT_TIMEOUT
    			if (thread.wait.ThreadEnd_returnExitStatus) {
    				thread.cpuContext._v0 = ERROR_KERNEL_WAIT_TIMEOUT;
    			}
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
				if (thread.wait.ThreadEnd_returnExitStatus) {
					thread.cpuContext._v0 = ERROR_KERNEL_WAIT_STATUS_RELEASED;
				}
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
        	case JPCSP_WAIT_GE_LIST:
        	case JPCSP_WAIT_NET:
        	case JPCSP_WAIT_AUDIO:
        	case JPCSP_WAIT_DISPLAY_VBLANK:
        	case JPCSP_WAIT_CTRL:
        	case JPCSP_WAIT_USB:
	        	thread.cpuContext._v0 = ERROR_KERNEL_WAIT_STATUS_RELEASED;
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
        if (thread.unloadModuleAtDeletion) {
        	SceModule module = Managers.modules.getModuleByUID(thread.moduleid);
        	if (module != null) {
        		module.unload();
        	}
        }
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
    	for (SceKernelThreadEventHandlerInfo handler : threadEventHandlers.values()) {
    		if (handler.appliesFor(getCurrentThread(), thread, event)) {
    			handler.triggerThreadEventHandler(contextThread, event);
    		}
    	}
    }

    public void hleKernelChangeThreadPriority(SceKernelThreadInfo thread, int newPriority) {
    	if (thread == null) {
    		return;
    	}

        thread.currentPriority = newPriority;
        if (thread.isRunning()) {
    		// The current thread will be moved to the front of the ready queue
    		hleChangeThreadState(thread, PSP_THREAD_READY);
        }

        if (thread.isReady()) {
        	// A ready thread is yielding when changing priority and moved to the end of the ready thread list.
            if (log.isDebugEnabled()) {
                log.debug("hleKernelChangeThreadPriority rescheduling ready thread");
            }
        	removeFromReadyThreads(thread);
        	addToReadyThreads(thread, false);
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
        	if (thread.wait.onUnblockAction != null) {
        		thread.wait.onUnblockAction.execute();
        		thread.wait.onUnblockAction = null;
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
                    thread.name.equals("SceModmgrStop")) {
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
            thread.wait.onUnblockAction = null;
            thread.doCallbacks = false;
        } else if (thread.isRunning()) {
            // debug
            if (thread.waitType != PSP_WAIT_NONE && !isIdleThread(thread)) {
                log.error(String.format("changeThreadState thread %s => PSP_THREAD_RUNNING. waitType should be PSP_WAIT_NONE. caller: %s", thread, getCallingFunction()));
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
            	if (thread.wait.ThreadEnd_returnExitStatus) {
	                // Return exit status of stopped thread
	                thread.cpuContext._v0 = stoppedThread.exitStatus;
            	}
            }
        }
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelExitCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int callbackId = cpu.getRegister(CALLBACKID_REGISTER);
        Callback callback = callbackManager.remove(callbackId);
        if (callback != null) {
            if (log.isTraceEnabled()) {
                log.trace("End of callback " + callback);
            }
            callback.executeExit(cpu);
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
        callAddress(null, address, afterAction, returnVoid, false, null);
    }

    private void callAddress(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, boolean preserveCpuState, int[] parameters) {
        if (thread != null) {
            // Save the wait state of the thread to restore it after the call
            afterAction = new AfterCallAction(thread);

            // Terminate the thread wait state
            thread.waitType = PSP_WAIT_NONE;

            hleChangeThreadState(thread, PSP_THREAD_READY);
        }

        int callbackId = callbackManager.getNewCallbackId();
        Callback callback = new Callback(callbackId, address, parameters, afterAction, returnVoid, preserveCpuState);

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

        callAddress(thread, address, afterAction, returnVoid, false, null);
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

        callAddress(thread, address, afterAction, returnVoid, false, new int[]{registerA0});
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

        callAddress(thread, address, afterAction, returnVoid, false, new int[]{registerA0, registerA1});
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

        callAddress(thread, address, afterAction, returnVoid, false, new int[]{registerA0, registerA1, registerA2});
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
     * @param preserverCpuState preserve the complete CpuState while executing the callback.
     *                    All the registers will be restored after the callback execution.
     * @param registerA0  first parameter of the callback ($a0)
     * @param registerA1  second parameter of the callback ($a1)
     * @param registerA2  third parameter of the callback ($a2)
     */
    public void executeCallback(SceKernelThreadInfo thread, int address, IAction afterAction, boolean returnVoid, boolean preserverCpuState, int registerA0, int registerA1, int registerA2) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Execute callback 0x%08X($a0=0x%08X, $a1=0x%08X, $a2=0x%08X), afterAction=%s, returnVoid=%b, preserverCpuState=%b", address, registerA0, registerA1, registerA2, afterAction, returnVoid, preserverCpuState));
        }

        callAddress(thread, address, afterAction, returnVoid, preserverCpuState, new int[]{registerA0, registerA1, registerA2});
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

        callAddress(thread, address, afterAction, returnVoid, false, new int[]{registerA0, registerA1, registerA2, registerA3});
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

        callAddress(thread, address, afterAction, returnVoid, false, new int[]{registerA0, registerA1, registerA2, registerA3, registerT0});
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleKernelExitThread(int exitStatus) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Thread exit detected SceUID=%x name='%s' return:0x%08X", currentThread.uid, currentThread.name, exitStatus));
        }
        sceKernelExitThread(exitStatus);
    }

    public int hleKernelExitDeleteThread() {
    	int exitStatus = Emulator.getProcessor().cpu._v0;
        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelExitDeleteThread SceUID=%x name='%s' return:0x%08X", currentThread.uid, currentThread.name, exitStatus));
        }

        return sceKernelExitDeleteThread(exitStatus);
    }

    /**
     * Check the validity of the thread UID.
     * Do not allow uid=0.
     * 
     * @param uid   thread UID to be checked
     * @return      valid thread UID
     */
    public int checkThreadID(int uid) {
        if (uid == 0) {
    		log.warn("checkThreadID illegal thread uid=0");
            throw new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_THREAD);
        }
        return checkThreadIDAllow0(uid);
    }

    /**
     * Check the validity of the thread UID.
     * Allow uid=0.
     * 
     * @param uid   thread UID to be checked
     * @return      valid thread UID (0 has been replaced by the UID of the currentThread)
     */
    public int checkThreadIDAllow0(int uid) {
        if (uid == 0) {
        	uid = currentThread.uid;
        }
    	if (!threadMap.containsKey(uid)) {
    		log.warn(String.format("checkThreadID not found thread 0x%08X", uid));
            throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_THREAD);
    	}

        if (!SceUidManager.checkUidPurpose(uid, "ThreadMan-thread", true)) {
            throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_THREAD);
        }

        return uid;
    }

    /**
     * Check the validity of the thread UID.
     * No special check on uid=0, i.e. return ERROR_KERNEL_NOT_FOUND_THREAD for uid=0.
     * 
     * @param uid   thread UID to be checked
     * @return      valid thread UID
     */
    public int checkThreadIDNoCheck0(int uid) {
        if (uid == 0) {
    		log.warn(String.format("checkThreadID not found thread 0x%08X", uid));
            throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_THREAD);
        }
        return checkThreadIDAllow0(uid);
    }

    /**
     * Check the validity of the VTimer UID.
     * 
     * @param uid   VTimer UID to be checked
     * @return      valid VTimer UID
     */
    public int checkVTimerID(int uid) {
    	if (!vtimers.containsKey(uid)) {
    		throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_VTIMER);
    	}

    	return uid;
    }

    public int checkSemaID(int uid) {
    	return Managers.semas.checkSemaID(uid);
    }

    public int checkEventFlagID(int uid) {
    	return Managers.eventFlags.checkEventFlagID(uid);
    }

    public int checkMbxID(int uid) {
    	return Managers.mbx.checkMbxID(uid);
    }

    public int checkMsgPipeID(int uid) {
    	return Managers.msgPipes.checkMsgPipeID(uid);
    }

    public int checkVplID(int uid) {
    	return Managers.vpl.checkVplID(uid);
    }

    public int checkFplID(int uid) {
    	return Managers.fpl.checkFplID(uid);
    }

    public int checkAlarmID(int uid) {
        if (!alarms.containsKey(uid)) {
            log.warn(String.format("checkAlarmID unknown uid=0x%x", uid));
            return ERROR_KERNEL_NOT_FOUND_ALARM;
        }

        return uid;
    }

    public int checkCallbackID(int uid) {
    	if (!callbackMap.containsKey(uid)) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK);
    	}

    	return uid;
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
            log.debug(String.format("hleKernelCreateThread SceUID=0x%X, name='%s', PC=0x%08X, attr=0x%X, priority=0x%X, stackSize=0x%X", thread.uid, thread.name, thread.cpuContext.pc, attr, initPriority, stackSize));
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
            log.debug(String.format("hleKernelStartThread SceUID=0x%X, name='%s', dataLen=0x%X, data=0x%08X, gp=0x%08X", thread.uid, thread.name, userDataLength, userDataAddr, gp));
        }
        // Reset all thread parameters: a thread can be restarted when it has exited.
        thread.reset();

        // Setup args by copying them onto the stack
        hleKernelSetThreadArguments(thread, userDataAddr, userDataLength);

        // Set thread $gp
        thread.cpuContext._gp = gp;

        // Update the exit status.
        thread.exitStatus = ERROR_KERNEL_THREAD_IS_NOT_DORMANT;

        // switch in the target thread if it's higher priority
        hleChangeThreadState(thread, PSP_THREAD_READY);

        // Execute the event in the context of the starting thread
        triggerThreadEvent(thread, thread, THREAD_EVENT_START);

        RuntimeContext.onThreadStart(thread);

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
        	hleKernelThreadEnterWaitState(PSP_WAIT_SLEEP, 0, sleepThreadWaitStateChecker, doCallbacks);
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

    public int hleKernelWaitThreadEnd(SceKernelThreadInfo waitingThread, int uid, TPointer32 timeoutAddr, boolean callbacks, boolean returnExitStatus) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelWaitThreadEnd SceUID=0x%X, callbacks=%b", uid, callbacks));
        }

        SceKernelThreadInfo thread = threadMap.get(uid);
        if (thread == null) {
            log.warn(String.format("hleKernelWaitThreadEnd unknown thread 0x%X", uid));
            return ERROR_KERNEL_NOT_FOUND_THREAD;
        }

        int result = 0;
        if (isBannedThread(thread)) {
            log.warn(String.format("hleKernelWaitThreadEnd %s banned, not waiting", thread));
            hleRescheduleCurrentThread();
        } else if (thread.isStopped()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleKernelWaitThreadEnd %s thread already stopped, not waiting, exitStatus=0x%08X", thread, thread.exitStatus));
        	}
        	if (returnExitStatus) {
        		// Return the thread exit status
        		result = thread.exitStatus;
        	}
            hleRescheduleCurrentThread();
        } else {
            // Wait on a specific thread end
        	waitingThread.wait.ThreadEnd_id = uid;
        	waitingThread.wait.ThreadEnd_returnExitStatus = returnExitStatus;
        	hleKernelThreadEnterWaitState(waitingThread, PSP_WAIT_THREAD_END, uid, waitThreadEndWaitStateChecker, timeoutAddr.getAddress(), callbacks);
        }

        return result;
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

    public int hleKernelDelayThread(int micros, boolean doCallbacks) {
        if (micros < THREAD_DELAY_MINIMUM_MICROS) {
        	micros = THREAD_DELAY_MINIMUM_MICROS;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelDelayThread micros=%d, callbacks=%b", micros, doCallbacks));
        }

    	hleKernelThreadEnterWaitState(currentThread, PSP_WAIT_DELAY, 0, null, micros, false, doCallbacks);

        return 0;
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
    public void hleKernelDeleteCallback(int uid) {
        SceKernelCallbackInfo callback = callbackMap.remove(uid);
        if (callback != null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelDeleteCallback %s", callback));
            }
            SceKernelThreadInfo thread = getThreadById(callback.threadId);
            if (thread != null) {
            	thread.deleteCallback(callback);
            }
        } else {
            log.warn(String.format("hleKernelDeleteCallback not a callback uid 0x%X", uid));
        }
    }

    protected int getThreadCurrentStackSize(Processor processor) {
    	int size = processor.cpu._sp - currentThread.getStackAddr();
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

                CheckCallbackReturnValue checkCallbackReturnValue = new CheckCallbackReturnValue(thread, callback.uid);
                callback.startContext(thread, checkCallbackReturnValue);
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

    protected int hleKernelSetAlarm(long delayUsec, TPointer handlerAddress, int handlerArgument) {
        long now = Scheduler.getNow();
        long schedule = now + delayUsec;
        SceKernelAlarmInfo sceKernelAlarmInfo = new SceKernelAlarmInfo(schedule, handlerAddress.getAddress(), handlerArgument);
        alarms.put(sceKernelAlarmInfo.uid, sceKernelAlarmInfo);

        scheduleAlarm(sceKernelAlarmInfo);

        return sceKernelAlarmInfo.uid;
    }

    protected long getSystemTime() {
        return SystemTimeManager.getSystemTime();
    }

    protected long getVTimerScheduleForScheduler(SceKernelVTimerInfo sceKernelVTimerInfo) {
        return sceKernelVTimerInfo.base + sceKernelVTimerInfo.schedule;
    }

    protected long setVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long time) {
    	long current = sceKernelVTimerInfo.getCurrentTime();
    	sceKernelVTimerInfo.base = sceKernelVTimerInfo.base + sceKernelVTimerInfo.getCurrentTime() - time;
        sceKernelVTimerInfo.current = 0;

        return current;
    }

    protected void startVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_RUNNING;
        sceKernelVTimerInfo.base = getSystemTime();

        if (sceKernelVTimerInfo.schedule != 0 && sceKernelVTimerInfo.handlerAddress != 0) {
            scheduleVTimer(sceKernelVTimerInfo, sceKernelVTimerInfo.schedule);
        }
    }

    protected void stopVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
        Scheduler.getInstance().removeAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);
        // Sum the elapsed time (multiple Start/Stop sequences are added)
        sceKernelVTimerInfo.current = sceKernelVTimerInfo.getCurrentTime();
        sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_STOPPED;
        sceKernelVTimerInfo.base = 0;
    }

    protected void scheduleVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long schedule) {
    	// Remove any previous schedule
        Scheduler.getInstance().removeAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);

        sceKernelVTimerInfo.schedule = schedule;

        if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING && sceKernelVTimerInfo.handlerAddress != 0) {
            Scheduler scheduler = Scheduler.getInstance();
            long schedulerSchedule = getVTimerScheduleForScheduler(sceKernelVTimerInfo);
            scheduler.addAction(schedulerSchedule, sceKernelVTimerInfo.vtimerInterruptAction);
            if (log.isDebugEnabled()) {
            	log.debug(String.format("Scheduling VTimer %s at %d(now=%d)", sceKernelVTimerInfo, schedulerSchedule, Scheduler.getNow()));
            }
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

        long schedule = sceKernelVTimerInfo.schedule + delay;
        scheduleVTimer(sceKernelVTimerInfo, schedule);

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

    @HLEUnimplemented
    @HLEFunction(nid = 0x6E9EA350, version = 150)
    public int _sceKernelReturnFromCallback() {
        return 0;
    }

    @HLEFunction(nid = 0x0C106E53, version = 150, checkInsideInterrupt = true)
    public int sceKernelRegisterThreadEventHandler(@StringInfo(maxLength = 32) String name, int thid, int mask, TPointer handlerFunc, int commonAddr) {
        switch (thid) {
	        case SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_CURRENT:
	        	// Only allowed for THREAD_EVENT_EXIT (doesn't make sense for the other events).
	        	if (mask != SceKernelThreadEventHandlerInfo.THREAD_EVENT_EXIT) {
	        		return SceKernelErrors.ERROR_KERNEL_OUT_OF_RANGE;
	        	}
	        	thid = getCurrentThreadID();
	        	break;
	        case SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_USER:
	        	// Always allowed
	        	break;
	        case SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_KERN:
	        case SceKernelThreadEventHandlerInfo.THREAD_EVENT_ID_ALL:
	        	// Only allowed in kernel mode
	        	if (!isKernelMode()) {
	        		return ERROR_KERNEL_NOT_FOUND_THREAD;
	        	}
	        	break;
        	default:
        		SceKernelThreadInfo thread = getThreadById(thid);
        		if (thread == null) {
	        		return ERROR_KERNEL_NOT_FOUND_THREAD;
        		}
        		break;
        }

        SceKernelThreadEventHandlerInfo handler = new SceKernelThreadEventHandlerInfo(name, thid, mask, handlerFunc.getAddress(), commonAddr);
        threadEventHandlers.put(handler.uid, handler);

        return handler.uid;
    }

    @HLEFunction(nid = 0x72F3C145, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelReleaseThreadEventHandler(int uid) {
        if (!threadEventHandlers.containsKey(uid)) {
        	return ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
        }

    	SceKernelThreadEventHandlerInfo handler = threadEventHandlers.remove(uid);
    	handler.release();
        return 0;
    }

    @HLEFunction(nid = 0x369EEB6B, version = 150)
    public int sceKernelReferThreadEventHandlerStatus(int uid, TPointer statusPointer) {
        if (!threadEventHandlers.containsKey(uid)) {
        	return ERROR_KERNEL_NOT_FOUND_THREAD_EVENT_HANDLER;
        }

        threadEventHandlers.get(uid).write(statusPointer.getMemory(), statusPointer.getAddress());
        return 0;
    }

    @HLEFunction(nid = 0xE81CAF8F, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateCallback(@StringInfo(maxLength = 32) String name, int func_addr, int user_arg_addr) {
    	SceKernelCallbackInfo callback = hleKernelCreateCallback(name, func_addr, user_arg_addr);
        return callback.uid;
    }

    @HLEFunction(nid = 0xEDBA5844, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelDeleteCallback(@CheckArgument("checkCallbackID") int uid) {
        hleKernelDeleteCallback(uid);
        
        return 0;
    }

    /**
     * Manually notifies a callback. Mostly used for exit callbacks,
     * and shouldn't be used at all (only some old homebrews use this, anyway).
     */
    @HLEFunction(nid = 0xC11BA8C4, version = 150)
    public int sceKernelNotifyCallback(@CheckArgument("checkCallbackID") int uid, int arg) {
        SceKernelCallbackInfo callback = getCallbackInfo(uid);

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
        return 0;
    }

    @HLEFunction(nid = 0xBA4051D6, version = 150)
    public int sceKernelCancelCallback(@CheckArgument("checkCallbackID") int uid) {
        SceKernelCallbackInfo callback = getCallbackInfo(uid);

        callback.notifyArg = 0;

        return 0;
    }

    /** Return the current notifyCount for a specific callback */
    @HLEFunction(nid = 0x2A3D44FF, version = 150)
    public int sceKernelGetCallbackCount(@CheckArgument("checkCallbackID") int uid) {
        SceKernelCallbackInfo callback = getCallbackInfo(uid);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetCallbackCount returning count=%d", callback.notifyCount));
        }

        return callback.notifyCount;
    }

    /** Check callbacks, only on the current thread. */
    @HLEFunction(nid = 0x349D6D6C, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelCheckCallback() {
        // Remember the currentThread, as it might have changed after
        // the execution of a callback.
        SceKernelThreadInfo thread = currentThread;
        boolean doCallbacks = thread.doCallbacks;
        thread.doCallbacks = true; // Force callbacks execution.

        // 0 - The calling thread has no reported callbacks.
        // 1 - The calling thread has reported callbacks which were executed successfully.
    	int result = checkThreadCallbacks(thread) ? 1 : 0;

    	thread.doCallbacks = doCallbacks; // Reset to the previous value.

        return result;
    }

    @HLEFunction(nid = 0x730ED8BC, version = 150)
    public int sceKernelReferCallbackStatus(@CheckArgument("checkCallbackID") int uid, TPointer infoAddr) {
        SceKernelCallbackInfo info = getCallbackInfo(uid);

        info.write(infoAddr);

        return 0;
    }

    /** sleep the current thread (using wait) */
    @HLEFunction(nid = 0x9ACE131E, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelSleepThread() {
        return hleKernelSleepThread(false);
    }

    /** sleep the current thread and handle callbacks (using wait)
     * in our implementation we have to use wait, not suspend otherwise we don't handle callbacks. */
    @HLEFunction(nid = 0x82826F70, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelSleepThreadCB() {
        int result = hleKernelSleepThread(true);
        checkCallbacks();

        return result;
    }

    @HLEFunction(nid = 0xD59EAD2F, version = 150)
    public int sceKernelWakeupThread(@CheckArgument("checkThreadID") int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);

        hleKernelWakeupThread(thread);

        return 0;
    }

    @HLEFunction(nid = 0xFCCFAD26, version = 150)
    public int sceKernelCancelWakeupThread(@CheckArgument("checkThreadIDAllow0") int uid) {
        SceKernelThreadInfo thread = getThreadById(uid);

        int result = thread.wakeupCount;
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCancelWakeupThread thread=%s returning %d", thread, result));
        }

        thread.wakeupCount = 0;

        return result;
    }

    @HLEFunction(nid = 0x9944F31F, version = 150)
    public int sceKernelSuspendThread(@CheckArgument("checkThreadID") int uid) {
        SceKernelThreadInfo thread = getThreadCurrentIsInvalid(uid);

        if (thread.isSuspended()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceKernelSuspendThread thread already suspended: thread=%s", thread.toString()));
        	}
        	return ERROR_KERNEL_THREAD_ALREADY_SUSPEND;
        }

        if (thread.isStopped()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceKernelSuspendThread thread already stopped: thread=%s", thread.toString()));
        	}
        	return ERROR_KERNEL_THREAD_ALREADY_DORMANT;
        }

        if (thread.isWaiting()) {
        	hleChangeThreadState(thread, PSP_THREAD_WAITING_SUSPEND);
        } else {
        	hleChangeThreadState(thread, PSP_THREAD_SUSPEND);
        }

        return 0;
    }

    @HLEFunction(nid = 0x75156E8F, version = 150)
    public int sceKernelResumeThread(@CheckArgument("checkThreadID") int uid) {
        SceKernelThreadInfo thread = getThreadById(uid);

        if (!thread.isSuspended()) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " not suspended (status=" + thread.status + ")");
            return ERROR_KERNEL_THREAD_IS_NOT_SUSPEND;
        }
        if (isBannedThread(thread)) {
            log.warn("sceKernelResumeThread SceUID=" + Integer.toHexString(uid) + " name:'" + thread.name + "' banned, not resuming");
            return 0;
        }

        if (thread.isWaiting()) {
            hleChangeThreadState(thread, PSP_THREAD_WAITING);
        } else {
        	hleChangeThreadState(thread, PSP_THREAD_READY);
        }

        return 0;
    }

    @HLEFunction(nid = 0x278C0DF5, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelWaitThreadEnd(@CheckArgument("checkThreadID") int uid, @CanBeNull TPointer32 timeoutAddr) {
        return hleKernelWaitThreadEnd(currentThread, uid, timeoutAddr, false, true);
    }

    @HLEFunction(nid = 0x840E8133, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelWaitThreadEndCB(@CheckArgument("checkThreadID") int uid, @CanBeNull TPointer32 timeoutAddr) {
        int result = hleKernelWaitThreadEnd(currentThread, uid, timeoutAddr, true, true);
        checkCallbacks();

        return result;
    }

    /** wait the current thread for a certain number of microseconds */
    @HLEFunction(nid = 0xCEADEB47, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelDelayThread(int micros) {
        return hleKernelDelayThread(micros, /* doCallbacks = */ false);
    }

    /** wait the current thread for a certain number of microseconds */
    @HLEFunction(nid = 0x68DA9E36, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelDelayThreadCB(int micros) {
        return hleKernelDelayThread(micros, /* doCallbacks = */ true);
    }

    /**
     * Delay the current thread by a specified number of sysclocks
     *
     * @param sysclocksPointer - Address of delay in sysclocks
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xBD123D9E, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelDelaySysClockThread(TPointer64 sysclocksPointer) {
        long sysclocks = sysclocksPointer.getValue();
        int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
        return hleKernelDelayThread(micros, false);
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
    public int sceKernelDelaySysClockThreadCB(TPointer64 sysclocksAddr) {
        long sysclocks = sysclocksAddr.getValue();
        int micros = SystemTimeManager.hleSysClock2USec32(sysclocks);
        return hleKernelDelayThread(micros, true);
    }

    @HLEFunction(nid = 0xD6DA4BA1, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateSema(String name, int attr, int initVal, int maxVal, @CanBeNull TPointer option) {
        return Managers.semas.sceKernelCreateSema(name, attr, initVal, maxVal, option);
    }

    @HLEFunction(nid = 0x28B6489C, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteSema(@CheckArgument("checkSemaID") int semaid) {
        return Managers.semas.sceKernelDeleteSema(semaid);
    }

    @HLEFunction(nid = 0x3F53E640, version = 150)
    public int sceKernelSignalSema(@CheckArgument("checkSemaID") int semaid, int signal) {
        return Managers.semas.sceKernelSignalSema(semaid, signal);
    }

    @HLEFunction(nid = 0x4E3A1105, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelWaitSema(@CheckArgument("checkSemaID") int semaid, int signal, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.semas.sceKernelWaitSema(semaid, signal, timeoutAddr);
    }

    @HLEFunction(nid = 0x6D212BAC, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelWaitSemaCB(@CheckArgument("checkSemaID") int semaid, int signal, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.semas.sceKernelWaitSemaCB(semaid, signal, timeoutAddr);
    }

    @HLEFunction(nid = 0x58B1F937, version = 150)
    public int sceKernelPollSema(@CheckArgument("checkSemaID") int semaid, int signal) {
        return Managers.semas.sceKernelPollSema(semaid, signal);
    }

    @HLEFunction(nid = 0x8FFDF9A2, version = 150)
    public int sceKernelCancelSema(@CheckArgument("checkSemaID") int semaid, int newcount, @CanBeNull TPointer32 numWaitThreadAddr) {
        return Managers.semas.sceKernelCancelSema(semaid, newcount, numWaitThreadAddr);
    }

    @HLEFunction(nid = 0xBC6FEBC5, version = 150)
    public int sceKernelReferSemaStatus(@CheckArgument("checkSemaID") int semaid, TPointer addr) {
        return Managers.semas.sceKernelReferSemaStatus(semaid, addr);
    }

    @HLEFunction(nid = 0x55C20A00, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateEventFlag(String name, int attr, int initPattern, @CanBeNull TPointer option) {
        return Managers.eventFlags.sceKernelCreateEventFlag(name, attr, initPattern, option);
    }

    @HLEFunction(nid = 0xEF9E4C70, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteEventFlag(@CheckArgument("checkEventFlagID") int uid) {
        return Managers.eventFlags.sceKernelDeleteEventFlag(uid);
    }

    @HLEFunction(nid = 0x1FB15A32, version = 150)
    public int sceKernelSetEventFlag(@CheckArgument("checkEventFlagID") int uid, int bitsToSet) {
        return Managers.eventFlags.sceKernelSetEventFlag(uid, bitsToSet);
    }

    @HLEFunction(nid = 0x812346E4, version = 150)
    public int sceKernelClearEventFlag(@CheckArgument("checkEventFlagID") int uid, int bitsToKeep) {
        return Managers.eventFlags.sceKernelClearEventFlag(uid, bitsToKeep);
    }

    @HLEFunction(nid = 0x402FCF22, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelWaitEventFlag(@CheckArgument("checkEventFlagID") int uid, int bits, int wait, @CanBeNull TPointer32 outBitsAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.eventFlags.sceKernelWaitEventFlag(uid, bits, wait, outBitsAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0x328C546A, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelWaitEventFlagCB(@CheckArgument("checkEventFlagID") int uid, int bits, int wait, @CanBeNull TPointer32 outBitsAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.eventFlags.sceKernelWaitEventFlagCB(uid, bits, wait, outBitsAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0x30FD48F0, version = 150)
    public int sceKernelPollEventFlag(@CheckArgument("checkEventFlagID") int uid, int bits, int wait, @CanBeNull TPointer32 outBitsAddr) {
        return Managers.eventFlags.sceKernelPollEventFlag(uid, bits, wait, outBitsAddr);
    }

    @HLEFunction(nid = 0xCD203292, version = 150)
    public int sceKernelCancelEventFlag(@CheckArgument("checkEventFlagID") int uid, int newPattern, @CanBeNull TPointer32 numWaitThreadAddr) {
        return Managers.eventFlags.sceKernelCancelEventFlag(uid, newPattern, numWaitThreadAddr);
    }

    @HLEFunction(nid = 0xA66B0120, version = 150)
    public int sceKernelReferEventFlagStatus(@CheckArgument("checkEventFlagID") int uid, TPointer addr) {
        return Managers.eventFlags.sceKernelReferEventFlagStatus(uid, addr);
    }

    @HLEFunction(nid = 0x8125221D, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateMbx(String name, int attr, @CanBeNull TPointer option) {
        return Managers.mbx.sceKernelCreateMbx(name, attr, option);
    }

    @HLEFunction(nid = 0x86255ADA, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteMbx(@CheckArgument("checkMbxID") int uid) {
        return Managers.mbx.sceKernelDeleteMbx(uid);
    }

    @HLEFunction(nid = 0xE9B3061E, version = 150)
    public int sceKernelSendMbx(@CheckArgument("checkMbxID") int uid, TPointer msgAddr) {
        return Managers.mbx.sceKernelSendMbx(uid, msgAddr);
    }

    @HLEFunction(nid = 0x18260574, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelReceiveMbx(@CheckArgument("checkMbxID") int uid, TPointer32 addrMsgAddr, @CanBeNull TPointer32 timeoutAddr) {
    	return Managers.mbx.sceKernelReceiveMbx(uid, addrMsgAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0xF3986382, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelReceiveMbxCB(@CheckArgument("checkMbxID") int uid,  TPointer32 addrMsgAddr, @CanBeNull TPointer32 timeoutAddr) {
    	return Managers.mbx.sceKernelReceiveMbxCB(uid, addrMsgAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0x0D81716A, version = 150)
    public int sceKernelPollMbx(@CheckArgument("checkMbxID") int uid,  TPointer32 addrMsgAddr) {
    	return Managers.mbx.sceKernelPollMbx(uid, addrMsgAddr);
    }

    @HLEFunction(nid = 0x87D4DD36, version = 150)
    public int sceKernelCancelReceiveMbx(@CheckArgument("checkMbxID") int uid, @CanBeNull TPointer32 pnumAddr) {
    	return Managers.mbx.sceKernelCancelReceiveMbx(uid, pnumAddr);
    }

    @HLEFunction(nid = 0xA8E8C846, version = 150)
    public int sceKernelReferMbxStatus(@CheckArgument("checkMbxID") int uid, TPointer infoAddr) {
    	return Managers.mbx.sceKernelReferMbxStatus(uid, infoAddr);
    }

    @HLEFunction(nid = 0x7C0DC2A0, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateMsgPipe(String name, int partitionid, int attr, int size, @CanBeNull TPointer option) {
        return Managers.msgPipes.sceKernelCreateMsgPipe(name, partitionid, attr, size, option);
    }

    @HLEFunction(nid = 0xF0B7DA1C, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteMsgPipe(@CheckArgument("checkMsgPipeID") int uid) {
        return Managers.msgPipes.sceKernelDeleteMsgPipe(uid);
    }

    @HLEFunction(nid = 0x876DBFAD, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelSendMsgPipe(@CheckArgument("checkMsgPipeID") int uid, TPointer msgAddr, int size, int waitMode, @CanBeNull TPointer32 resultSizeAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.msgPipes.sceKernelSendMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0x7C41F2C2, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelSendMsgPipeCB(@CheckArgument("checkMsgPipeID") int uid, TPointer msgAddr, int size, int waitMode, @CanBeNull TPointer32 resultSizeAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.msgPipes.sceKernelSendMsgPipeCB(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0x884C9F90, version = 150)
    public int sceKernelTrySendMsgPipe(@CheckArgument("checkMsgPipeID") int uid, TPointer msgAddr, int size, int waitMode, @CanBeNull TPointer32 resultSizeAddr) {
        return Managers.msgPipes.sceKernelTrySendMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr);
    }

    @HLEFunction(nid = 0x74829B76, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelReceiveMsgPipe(@CheckArgument("checkMsgPipeID") int uid, TPointer msgAddr, int size, int waitMode, @CanBeNull TPointer32 resultSizeAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.msgPipes.sceKernelReceiveMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0xFBFA697D, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelReceiveMsgPipeCB(@CheckArgument("checkMsgPipeID") int uid, TPointer msgAddr, int size, int waitMode, @CanBeNull TPointer32 resultSizeAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.msgPipes.sceKernelReceiveMsgPipeCB(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0xDF52098F, version = 150)
    public int sceKernelTryReceiveMsgPipe(@CheckArgument("checkMsgPipeID") int uid, TPointer msgAddr, int size, int waitMode, @CanBeNull TPointer32 resultSizeAddr) {
        return Managers.msgPipes.sceKernelTryReceiveMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr);
    }

    @HLEFunction(nid = 0x349B864D, version = 150)
    public int sceKernelCancelMsgPipe(@CheckArgument("checkMsgPipeID") int uid, @CanBeNull TPointer32 sendAddr, @CanBeNull TPointer32 recvAddr) {
        return Managers.msgPipes.sceKernelCancelMsgPipe(uid, sendAddr, recvAddr);
    }

    @HLEFunction(nid = 0x33BE4024, version = 150)
    public int sceKernelReferMsgPipeStatus(@CheckArgument("checkMsgPipeID") int uid, TPointer infoAddr) {
        return Managers.msgPipes.sceKernelReferMsgPipeStatus(uid, infoAddr);
    }

    @HLEFunction(nid = 0x56C039B5, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateVpl(String name, int partitionid, int attr, int size, @CanBeNull TPointer option) {
        return Managers.vpl.sceKernelCreateVpl(name, partitionid, attr, size, option);
    }

    @HLEFunction(nid = 0x89B3D48C, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteVpl(@CheckArgument("checkVplID") int uid) {
        return Managers.vpl.sceKernelDeleteVpl(uid);
    }

    @HLEFunction(nid = 0xBED27435, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelAllocateVpl(@CheckArgument("checkVplID") int uid, int size, TPointer32 dataAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.vpl.sceKernelAllocateVpl(uid, size, dataAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0xEC0A693F, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelAllocateVplCB(@CheckArgument("checkVplID") int uid, int size, TPointer32 dataAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.vpl.sceKernelAllocateVplCB(uid, size, dataAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0xAF36D708, version = 150)
    public int sceKernelTryAllocateVpl(@CheckArgument("checkVplID") int uid, int size, TPointer32 dataAddr) {
        return Managers.vpl.sceKernelTryAllocateVpl(uid, size, dataAddr);
    }

    @HLEFunction(nid = 0xB736E9FF, version = 150, checkInsideInterrupt = true)
    public int sceKernelFreeVpl(@CheckArgument("checkVplID") int uid, TPointer dataAddr) {
        return Managers.vpl.sceKernelFreeVpl(uid, dataAddr);
    }

    @HLEFunction(nid = 0x1D371B8A, version = 150)
    public int sceKernelCancelVpl(@CheckArgument("checkVplID") int uid, @CanBeNull TPointer32 numWaitThreadAddr) {
        return Managers.vpl.sceKernelCancelVpl(uid, numWaitThreadAddr);
    }

    @HLEFunction(nid = 0x39810265, version = 150)
    public int sceKernelReferVplStatus(@CheckArgument("checkVplID") int uid, TPointer infoAddr) {
        return Managers.vpl.sceKernelReferVplStatus(uid, infoAddr);
    }

    @HLEFunction(nid = 0xC07BB470, version = 150, checkInsideInterrupt = true)
    public int sceKernelCreateFpl(String name, int partitionid, int attr, int blocksize, int blocks, @CanBeNull TPointer option) {
        return Managers.fpl.sceKernelCreateFpl(name, partitionid, attr, blocksize, blocks, option);
    }

    @HLEFunction(nid = 0xED1410E0, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteFpl(@CheckArgument("checkFplID") int uid) {
        return Managers.fpl.sceKernelDeleteFpl(uid);
    }

    @HLEFunction(nid = 0xD979E9BF, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelAllocateFpl(@CheckArgument("checkFplID") int uid, TPointer32 dataAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.fpl.sceKernelAllocateFpl(uid, dataAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0xE7282CB6, version = 150, checkInsideInterrupt = true, checkDispatchThreadEnabled = true)
    public int sceKernelAllocateFplCB(@CheckArgument("checkFplID") int uid, TPointer32 dataAddr, @CanBeNull TPointer32 timeoutAddr) {
        return Managers.fpl.sceKernelAllocateFplCB(uid, dataAddr, timeoutAddr);
    }

    @HLEFunction(nid = 0x623AE665, version = 150)
    public int sceKernelTryAllocateFpl(@CheckArgument("checkFplID") int uid, TPointer32 dataAddr) {
        return Managers.fpl.sceKernelTryAllocateFpl(uid, dataAddr);
    }

    @HLEFunction(nid = 0xF6414A71, version = 150, checkInsideInterrupt = true)
    public int sceKernelFreeFpl(@CheckArgument("checkFplID") int uid, TPointer dataAddr) {
        return Managers.fpl.sceKernelFreeFpl(uid, dataAddr);
    }

    @HLEFunction(nid = 0xA8AA591F, version = 150)
    public int sceKernelCancelFpl(@CheckArgument("checkFplID") int uid, @CanBeNull TPointer32 numWaitThreadAddr) {
        return Managers.fpl.sceKernelCancelFpl(uid, numWaitThreadAddr);
    }

    @HLEFunction(nid = 0xD8199E4C, version = 150)
    public int sceKernelReferFplStatus(@CheckArgument("checkFplID") int uid, TPointer infoAddr) {
        return Managers.fpl.sceKernelReferFplStatus(uid, infoAddr);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0E927AED, version = 150)
    public int _sceKernelReturnFromTimerHandler() {
        return 0;
    }

    @HLEFunction(nid = 0x110DEC9A, version = 150)
    public int sceKernelUSec2SysClock(int usec, TPointer64 sysClockAddr) {
        return Managers.systime.sceKernelUSec2SysClock(usec, sysClockAddr);
    }

    @HLEFunction(nid = 0xC8CD158C, version = 150)
    public long sceKernelUSec2SysClockWide(int usec) {
        return Managers.systime.sceKernelUSec2SysClockWide(usec);
    }

    @HLEFunction(nid = 0xBA6B92E2, version = 150)
    public int sceKernelSysClock2USec(TPointer64 sysClockAddr, @CanBeNull TPointer32 secAddr, @CanBeNull TPointer32 microSecAddr) {
        return Managers.systime.sceKernelSysClock2USec(sysClockAddr, secAddr, microSecAddr);
    }

    @HLEFunction(nid = 0xE1619D7C, version = 150)
    public int sceKernelSysClock2USecWide(long sysClock, @CanBeNull TPointer32 secAddr, @CanBeNull TPointer32 microSecAddr) {
        return Managers.systime.sceKernelSysClock2USecWide(sysClock, secAddr, microSecAddr);
    }

    @HLEFunction(nid = 0xDB738F35, version = 150)
    public int sceKernelGetSystemTime(TPointer64 timeAddr) {
        return Managers.systime.sceKernelGetSystemTime(timeAddr);
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
    public int sceKernelSetAlarm(int delayUsec, TPointer handlerAddress, int handlerArgument) {
        return hleKernelSetAlarm(delayUsec, handlerAddress, handlerArgument);
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
    public int sceKernelSetSysClockAlarm(TPointer64 delaySysclockAddr, TPointer handlerAddress, int handlerArgument) {
        long delaySysclock = delaySysclockAddr.getValue();
        long delayUsec = SystemTimeManager.hleSysClock2USec(delaySysclock);

        return hleKernelSetAlarm(delayUsec, handlerAddress, handlerArgument);
    }

    /**
     * Cancel a pending alarm.
     *
     * @param alarmUid - UID of the alarm to cancel.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x7E65B999, version = 150)
    public int sceKernelCancelAlarm(@CheckArgument("checkAlarmID") int alarmUid) {
        SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        cancelAlarm(sceKernelAlarmInfo);

        return 0;
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
    public int sceKernelReferAlarmStatus(@CheckArgument("checkAlarmID") int alarmUid, TPointer infoAddr) {
        SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        sceKernelAlarmInfo.write(infoAddr);

        return 0;
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
    public int sceKernelCreateVTimer(String name, @CanBeNull TPointer optAddr) {
        SceKernelVTimerInfo sceKernelVTimerInfo = new SceKernelVTimerInfo(name);
        vtimers.put(sceKernelVTimerInfo.uid, sceKernelVTimerInfo);

        return sceKernelVTimerInfo.uid;
    }

    /**
     * Delete a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error.
     */
    @HLEFunction(nid = 0x328F9E52, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteVTimer(@CheckArgument("checkVTimerID") int vtimerUid) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.remove(vtimerUid);
        sceKernelVTimerInfo.delete();

        return 0;
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
    public int sceKernelGetVTimerBase(@CheckArgument("checkVTimerID") int vtimerUid, TPointer64 baseAddr) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        baseAddr.setValue(sceKernelVTimerInfo.base);

        return 0;
    }

    /**
     * Get the timer base (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer base
     */
    @HLEFunction(nid = 0xB7C18B77, version = 150)
    public long sceKernelGetVTimerBaseWide(@CheckArgument("checkVTimerID") int vtimerUid) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);

        return sceKernelVTimerInfo.base;
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
    public int sceKernelGetVTimerTime(@CheckArgument("checkVTimerID") int vtimerUid, TPointer64 timeAddr) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        long time = sceKernelVTimerInfo.getCurrentTime();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerTime returning %d", time));
        }
        timeAddr.setValue(time);

        return 0;
    }

    /**
     * Get the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer time
     */
    @HLEFunction(nid = 0xC0B3FFD2, version = 150)
    public long sceKernelGetVTimerTimeWide(@CheckArgument("checkVTimerID") int vtimerUid) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        long time = sceKernelVTimerInfo.getCurrentTime();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetVTimerTimeWide returning %d", time));
        }

        return time;
    }

    /**
     * Set the timer time
     *
     * @param vtimerUid - UID of the vtimer
     * @param timeAddr - Pointer to a ::SceKernelSysClock structure
     *                   The previous value of the vtimer is returned back in this structure.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x542AD630, version = 150, checkInsideInterrupt = true)
    public int sceKernelSetVTimerTime(@CheckArgument("checkVTimerID") int vtimerUid, TPointer64 timeAddr) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        long time = timeAddr.getValue();
        timeAddr.setValue(setVTimer(sceKernelVTimerInfo, time));

        return 0;
    }

    /**
     * Set the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     * @param time - a ::SceKernelSysClock structure
     *
     * @return the last time of the vtimer or -1 if the vtimerUid is invalid
     */
    @HLEFunction(nid = 0xFB6425C3, version = 150, checkInsideInterrupt = true)
    public long sceKernelSetVTimerTimeWide(int vtimerUid, long time) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	// sceKernelSetVTimerTimeWide returns -1 instead of ERROR_KERNEL_NOT_FOUND_VTIMER
        	// when the vtimerUid is invalid.
        	return -1;
        }

        return setVTimer(sceKernelVTimerInfo, time);
    }

    /**
     * Start a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    @HLEFunction(nid = 0xC68D9437, version = 150)
    public int sceKernelStartVTimer(@CheckArgument("checkVTimerID") int vtimerUid) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
            return 1; // already started
        }

        startVTimer(sceKernelVTimerInfo);

        return 0;
    }

    /**
     * Stop a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    @HLEFunction(nid = 0xD0AEEE87, version = 150)
    public int sceKernelStopVTimer(@CheckArgument("checkVTimerID") int vtimerUid) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_STOPPED) {
            return 0; // already stopped
        }

        stopVTimer(sceKernelVTimerInfo);

        return 1;
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
    public int sceKernelSetVTimerHandler(@CheckArgument("checkVTimerID") int vtimerUid, TPointer64 scheduleAddr, @CanBeNull TPointer handlerAddress, int handlerArgument) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        long schedule = scheduleAddr.getValue();
        sceKernelVTimerInfo.handlerAddress = handlerAddress.getAddress();
        sceKernelVTimerInfo.handlerArgument = handlerArgument;
        scheduleVTimer(sceKernelVTimerInfo, schedule);

        return 0;
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
    public int sceKernelSetVTimerHandlerWide(@CheckArgument("checkVTimerID") int vtimerUid, long schedule, TPointer handlerAddress, int handlerArgument) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        sceKernelVTimerInfo.handlerAddress = handlerAddress.getAddress();
        sceKernelVTimerInfo.handlerArgument = handlerArgument;
        scheduleVTimer(sceKernelVTimerInfo, schedule);

        return 0;
    }

    /**
     * Cancel the timer handler
     *
     * @param vtimerUid - The UID of the vtimer
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xD2D615EF, version = 150)
    public int sceKernelCancelVTimerHandler(@CheckArgument("checkVTimerID") int vtimerUid) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        cancelVTimer(sceKernelVTimerInfo);

        return 0;
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
    public int sceKernelReferVTimerStatus(@CheckArgument("checkVTimerID") int vtimerUid, TPointer infoAddr) {
        SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        sceKernelVTimerInfo.write(Memory.getInstance(), infoAddr.getAddress());

        return 0;
    }

    @HLEFunction(nid = 0x446D8DE6, version = 150)
    public int sceKernelCreateThread(@StringInfo(maxLength = 32) String name, int entry_addr, int initPriority, int stackSize, int attr, int option_addr) {
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

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_CREATE);

        return thread.uid;
    }

    /** mark a thread for deletion. */
    @HLEFunction(nid = 0x9FA03CD3, version = 150, checkInsideInterrupt = true)
    public int sceKernelDeleteThread(@CheckArgument("checkThreadIDAllow0") int uid) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (!thread.isStopped()) {
            return ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        }

        // Mark thread for deletion
        setToBeDeletedThread(thread);

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_DELETE);

        return 0;
    }

    @HLEFunction(nid = 0xF475845D, version = 150, checkInsideInterrupt = true)
    public int sceKernelStartThread(@CheckArgument("checkThreadID") int uid, int len, int data_addr) {
        SceKernelThreadInfo thread = threadMap.get(uid);
        if (isBannedThread(thread)) {
            log.warn(String.format("sceKernelStartThread %s banned, not starting", thread));
            // Banned, fake start.
            hleRescheduleCurrentThread();
            return 0;
        }
        if (!thread.isStopped()) {
            return ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        }

        hleKernelStartThread(thread, len, data_addr, thread.gpReg_addr);

        return 0;
    }

    @HLEFunction(nid = 0x532A522E, version = 150)
    public int _sceKernelExitThread(int exitStatus) {
        // _sceKernelExitThread is equivalent to sceKernelExitThread
        return sceKernelExitThread(exitStatus);
    }

    /** exit the current thread */
    @HLEFunction(nid = 0xAA73C935, version = 150, checkInsideInterrupt = true)
    public int sceKernelExitThread(int exitStatus) {
    	// PSP is only returning an error for a SDK after 3.07
    	if (!isDispatchThreadEnabled() && Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion() > 0x0307FFFF) {
    		return SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
    	}

    	SceKernelThreadInfo thread = currentThread;

        if (exitStatus < 0) {
        	thread.setExitStatus(ERROR_KERNEL_ILLEGAL_ARGUMENT);
        } else {
        	thread.setExitStatus(exitStatus);
        }

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);

        hleChangeThreadState(thread, PSP_THREAD_STOPPED);
        RuntimeContext.onThreadExit(thread);
        hleRescheduleCurrentThread();

        return 0;
    }

    /** exit the current thread, then delete it */
    @HLEFunction(nid = 0x809CE29B, version = 150, checkInsideInterrupt = true)
    public int sceKernelExitDeleteThread(int exitStatus) {
    	// PSP is only returning an error for a SDK after 3.07
    	if (!isDispatchThreadEnabled() && Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion() > 0x0307FFFF) {
    		return SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
    	}

        SceKernelThreadInfo thread = currentThread;
        thread.setExitStatus(exitStatus);

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);
        triggerThreadEvent(thread, currentThread, THREAD_EVENT_DELETE);
        
        hleChangeThreadState(thread, PSP_THREAD_STOPPED);
        RuntimeContext.onThreadExit(thread);
        setToBeDeletedThread(thread);
        hleRescheduleCurrentThread();

        return 0;
    }

    /** terminate thread */
    @HLEFunction(nid = 0x616403BA, version = 150)
    public int sceKernelTerminateThread(@CheckArgument("checkThreadID") int uid) {
    	// PSP is only returning an error for a SDK after 3.07
    	if (IntrManager.getInstance().isInsideInterrupt() && Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion() > 0x0307FFFF) {
    		return SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
    	}
    	// PSP is only returning an error for a SDK after 3.07
    	if (!isDispatchThreadEnabled() && Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion() > 0x0307FFFF) {
    		return SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
    	}

    	SceKernelThreadInfo thread = getThreadCurrentIsInvalid(uid);

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);

        // Return this exit status to threads currently waiting on the thread end
        thread.setExitStatus(ERROR_KERNEL_THREAD_IS_TERMINATED);

        terminateThread(thread);

        // Return this exit status to threads that will wait on this thread end later on
        thread.setExitStatus(ERROR_KERNEL_THREAD_ALREADY_DORMANT);

        return 0;
    }

    /** terminate thread, then mark it for deletion */
    @HLEFunction(nid = 0x383F7BCC, version = 150, checkInsideInterrupt = true)
    public int sceKernelTerminateDeleteThread(@CheckArgument("checkThreadID") int uid) {
    	// PSP is only returning an error for a SDK after 3.07
    	if (!isDispatchThreadEnabled() && Modules.SysMemUserForUserModule.hleKernelGetCompiledSdkVersion() > 0x0307FFFF) {
    		return SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
    	}

    	SceKernelThreadInfo thread = getThreadCurrentIsInvalid(uid);

        triggerThreadEvent(thread, currentThread, THREAD_EVENT_EXIT);
        triggerThreadEvent(thread, currentThread, THREAD_EVENT_DELETE);

        terminateThread(thread);
        setToBeDeletedThread(thread);

        return 0;
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
            log.debug(String.format("sceKernelChangeCurrentThreadAttr removeAttr=0x%X, addAttr=0x%X, currentAttr=0x%X", removeAttr, addAttr, currentThread.attr));
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
    public int sceKernelChangeThreadPriority(@CheckArgument("checkThreadIDAllow0") int uid, @CheckArgument("checkThreadPriority") int priority) {
        SceKernelThreadInfo thread = getThreadById(uid);

        if (thread.isStopped()) {
            // Tested on PSP:
            // If the thread is stopped, it's current priority is replaced by it's initial priority.
            thread.currentPriority = thread.initPriority;
            return ERROR_KERNEL_THREAD_ALREADY_DORMANT;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelChangeThreadPriority thread=%s, newPriority=0x%X, oldPriority=0x%X", thread, priority, thread.currentPriority));
        }
        hleKernelChangeThreadPriority(thread, priority);

        return 0;
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
    	SceKernelThreadInfo thread = getThreadById(uid);
    	if (thread == currentThread) {
            throw(new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_THREAD));
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
    public int sceKernelReleaseWaitThread(@CheckArgument("checkThreadID") int uid) {
        SceKernelThreadInfo thread = getThreadCurrentIsInvalid(uid);

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
            log.debug(String.format("sceKernelGetThreadId returning uid=0x%X", currentThread.uid));
        }

        return currentThread.uid;
    }

    @HLEFunction(nid = 0x94AA61EE, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetThreadCurrentPriority() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetThreadCurrentPriority returning currentPriority=%d", currentThread.currentPriority));
        }

        return currentThread.currentPriority;
    }

    /** @return ERROR_NOT_FOUND_THREAD on uid < 0, uid == 0 and thread not found */
    @HLEFunction(nid = 0x3B183E26, version = 150)
    public int sceKernelGetThreadExitStatus(@CheckArgument("checkThreadIDNoCheck0") int uid) {
        SceKernelThreadInfo thread = getThreadById(uid);
        if (!thread.isStopped()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelGetThreadExitStatus not stopped uid=0x%x", uid));
            }
            return ERROR_KERNEL_THREAD_IS_NOT_DORMANT;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetThreadExitStatus thread=%s returning exitStatus=0x%08X", thread, thread.exitStatus));
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
    public int sceKernelGetThreadStackFreeSize(@CheckArgument("checkThreadIDAllow0") int uid) {
    	SceKernelThreadInfo thread = getThreadById(uid);

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
    public int sceKernelReferThreadStatus(@CheckArgument("checkThreadIDAllow0") int uid, TPointer addr) {
        SceKernelThreadInfo thread = getThreadById(uid);
    	thread.write(addr);

        return 0;
    }

    @HLEFunction(nid = 0xFFC36A14, version = 150, checkInsideInterrupt = true)
    public int sceKernelReferThreadRunStatus(@CheckArgument("checkThreadIDAllow0") int uid, TPointer addr) {
    	SceKernelThreadInfo thread = getThreadById(uid);
        thread.writeRunStatus(addr);
        
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
        if (type != SCE_KERNEL_TMID_Thread) {
            log.warn(String.format("UNIMPLEMENTED:sceKernelGetThreadmanIdList type=%d", type));
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
                        log.debug(String.format("sceKernelGetThreadmanIdList adding thread %s", thread));
                    }
                    readBufPtr.setValue(saveCount << 2, thread.uid);
                    saveCount++;
                } else {
                    log.warn(String.format("sceKernelGetThreadmanIdList NOT adding thread %s (no more space)", thread));
                }
                fullCount++;
            }
        }

        idCountPtr.setValue(fullCount);

        return 0;
    }

    @HLEFunction(nid = 0x57CF62DD, version = 150)
    public int sceKernelGetThreadmanIdType(int uid) {
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
        return 0;
    }

    @HLEFunction(nid = 0x8218B4DD, version = 150)
    public int sceKernelReferGlobalProfiler() {
        // Can be safely ignored. Only valid in debug mode on a real PSP.
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
        private boolean preserveCpuState;
        private CpuState savedCpuState;

        public Callback(int id, int address, int[] parameters, IAction afterAction, boolean returnVoid, boolean preserveCpuState) {
            this.id = id;
            this.address = address;
            this.parameters = parameters;
            this.afterAction = afterAction;
            this.returnVoid = returnVoid;
            this.preserveCpuState = preserveCpuState;
        }

        public int getId() {
            return id;
        }

		public void execute(SceKernelThreadInfo thread) {
			CpuState cpu = thread.cpuContext;

			savedIdRegister = cpu.getRegister(CALLBACKID_REGISTER);
	        savedRa = cpu._ra;
	        savedPc = cpu.pc;
	        savedV0 = cpu._v0;
	        savedV1 = cpu._v1;
	        if (preserveCpuState) {
	        	savedCpuState = new CpuState(cpu);
			}

	        // Copy parameters ($a0, $a1, ...) to the cpu
	        if (parameters != null) {
	        	for (int i = 0; i < parameters.length; i++) {
	        		cpu.setRegister(_a0 + i, parameters[i]);
	        	}
	        }

	        cpu.setRegister(CALLBACKID_REGISTER, id);
	        cpu._ra = CALLBACK_EXIT_HANDLER_ADDRESS;
	        cpu.pc = address;

	        RuntimeContext.executeCallback();
		}

		public void executeExit(CpuState cpu) {
            cpu.setRegister(CALLBACKID_REGISTER, savedIdRegister);
            cpu._ra = savedRa;
            cpu.pc = savedPc;

			if (afterAction != null) {
                afterAction.execute();
            }

			if (preserveCpuState) {
				cpu.copy(savedCpuState);
			} else {
	            // Do we need to restore $v0/$v1?
	            if (returnVoid) {
	            	cpu._v0 = savedV0;
	            	cpu._v1 = savedV1;
	            }
			}
		}

		public void setAfterAction(IAction afterAction) {
			this.afterAction = afterAction;
		}

		@Override
        public String toString() {
            return String.format("Callback address=0x%08X,id=%d,returnVoid=%b", address, getId(), returnVoid);
        }
    }

    private class AfterCallAction implements IAction {
        private SceKernelThreadInfo thread;
        private int status;
        private int waitType;
        private int waitId;
        private ThreadWaitInfo threadWaitInfo;
        private boolean doCallbacks;
        private IAction afterAction;

        public AfterCallAction(SceKernelThreadInfo thread) {
        	this.thread = thread;
            status = thread.status;
            waitType = thread.waitType;
            waitId = thread.waitId;
            threadWaitInfo = new ThreadWaitInfo(thread.wait);
            doCallbacks = thread.doCallbacks;
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
            		doCallbacks = false;
            	}
                if (log.isDebugEnabled()) {
                    log.debug(String.format("AfterCallAction: restoring wait state for thread '%s' to %s, %s, doCallbacks %b", thread.toString(), SceKernelThreadInfo.getStatusName(status), SceKernelThreadInfo.getWaitName(waitType, waitId, threadWaitInfo, status), doCallbacks));
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
                doCallbacks = false;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("AfterCallAction: leaving thread in READY state: " + thread.toString());
                }
                doCallbacks = false;
            }

        	thread.doCallbacks = doCallbacks;
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
				thread.cpuContext._v0 = ERROR_KERNEL_NOT_FOUND_THREAD;
				return false;
			}

			if (threadEnd.isStopped()) {
                // Return exit status of stopped thread
                thread.cpuContext._v0 = threadEnd.exitStatus;
                return false;
			}

			return true;
		}
    }

    public static class SleepThreadWaitStateChecker implements IWaitStateChecker {
		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
	        if (thread.wakeupCount > 0) {
	            // sceKernelWakeupThread() has been called while the thread was waiting
	            thread.wakeupCount--;
	            // Return 0
	            thread.cpuContext._v0 = 0;
	            return false;
	        }

	        return true;
		}
    }

    /**
     * A callback is deleted when its return value is non-zero.
     */
    private class CheckCallbackReturnValue implements IAction {
    	private SceKernelThreadInfo thread;
    	private int callbackUid;

    	public CheckCallbackReturnValue(SceKernelThreadInfo thread, int callbackUid) {
    		this.thread = thread;
			this.callbackUid = callbackUid;
		}

		@Override
		public void execute() {
			int callbackReturnValue = thread.cpuContext._v0;
			if (callbackReturnValue != 0) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Callback uid=0x%X has returned value 0x%08X: deleting the callback", callbackUid, callbackReturnValue));
				}
				hleKernelDeleteCallback(callbackUid);
			}
		}
    }
}