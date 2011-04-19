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
package jpcsp.Allegrex.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Instructions;
import jpcsp.Allegrex.VfpuState;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SyscallHandler;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.hardware.Interrupts;
import jpcsp.memory.FastMemory;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class RuntimeContext {
    public  static Logger log = Logger.getLogger("runtime");
	public  static boolean isActive = true;
	public  static int gpr[];
	public  static float fpr[];
	public  static VfpuState.VfpuValue vpr[];
	public  static int memoryInt[];
	public  static Processor processor;
	public  static CpuState cpu;
	public  static Memory memory;
	public  static final boolean enableIntructionCounting = true;
	public  static       boolean enableDebugger = true;
	public  static final String debuggerName = "syncDebugger";
	public  static final boolean debugCodeBlockCalls = false;
	public  static final String debugCodeBlockStart = "debugCodeBlockStart";
	public  static final String debugCodeBlockEnd = "debugCodeBlockEnd";
	public  static final boolean debugCodeInstruction = false;
	public  static final String debugCodeInstructionName = "debugCodeInstruction";
	public  static final boolean debugMemoryRead = false;
	public  static final boolean debugMemoryWrite = false;
	public  static final boolean debugMemoryReadWriteNoSP = true;
	public  static final boolean enableInstructionTypeCounting = false;
	public  static final String instructionTypeCount = "instructionTypeCount";
	public  static final String logInfo = "logInfo";
	public  static final String pauseEmuWithStatus = "pauseEmuWithStatus";
	public  static final boolean enableLineNumbers = true;
	private static final int idleSleepMicros = 1000;
	private static final Map<Integer, CodeBlock> codeBlocks = Collections.synchronizedMap(new HashMap<Integer, CodeBlock>());
	private static final Map<SceKernelThreadInfo, RuntimeThread> threads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	private static final Map<SceKernelThreadInfo, RuntimeThread> toBeStoppedThreads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	private static final Map<SceKernelThreadInfo, RuntimeThread> alreadyStoppedThreads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	private static final List<Thread> alreadySwitchedStoppedThreads = Collections.synchronizedList(new ArrayList<Thread>());
	private static final Map<SceKernelThreadInfo, RuntimeThread> toBeDeletedThreads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	public  static volatile SceKernelThreadInfo currentThread = null;
	private static volatile RuntimeThread currentRuntimeThread = null;
	private static final Object waitForEnd = new Object();
	private static volatile Emulator emulator;
	private static volatile boolean isIdle = false;
	private static volatile boolean reset = false;
	public  static CpuDurationStatistics idleDuration = new CpuDurationStatistics("Idle Time");
	private static Map<Instruction, Integer> instructionTypeCounts = Collections.synchronizedMap(new HashMap<Instruction, Integer>());
	private static SceKernelThreadInfo pendingCallbackThread = null;
    private static SceKernelThreadInfo pendingCallbackReturnThread = null;
	private static CpuState pendingCallbackCpuState = null;
	public  static boolean enableDaemonThreadSync = true;
	public  static final String syncName = "sync";
	public  static volatile boolean wantSync = false;
	private static RuntimeSyncThread runtimeSyncThread = null;
	private static sceDisplay sceDisplayModule;

	public static void execute(Instruction insn, int opcode) {
		insn.interpret(processor, opcode);
	}

	private static int jumpCall(int address, int returnAddress, boolean isJump) throws Exception {
        IExecutable executable = getExecutable(address);
        if (executable == null) {
            // TODO Return to interpreter
            log.error("RuntimeContext.jumpCall - Cannot find executable");
            throw new RuntimeException("Cannot find executable");
        }

		int returnValue = executable.exec(returnAddress, returnAddress, isJump);

        if (debugCodeBlockCalls && log.isDebugEnabled()) {
        	log.debug(String.format("RuntimeContext.jumpCall returning 0x%08X", returnValue));
        }

        return returnValue;
	}

	public static int jump(int address, int returnAddress, int alternativeReturnAddress) throws Exception {
		int returnValue;

		if (debugCodeBlockCalls && log.isDebugEnabled()) {
			log.debug(String.format("RuntimeContext.jump address=0x%08X, returnAddress=0x%08X, alternativeReturnAddress=0x%08X", address, returnAddress, alternativeReturnAddress));
		}

		if (IntrManager.getInstance().isInsideInterrupt() || currentRuntimeThread == null) {
			// No setjmp/longjmp handling inside interrupts and idle state
			returnValue = jumpCall(address, returnAddress, true);
		} else {
			int sp = cpu.gpr[29];

			if (log.isTraceEnabled()) {
				log.trace(String.format("RuntimeContext.jump sp=0x%08X, stack=%s", sp, currentRuntimeThread.getStackString()));
			}

			// Handle setjmp/longjmp C-like calls
			if (currentRuntimeThread.hasStackState(address, sp)) {
		    	StackPopException e = new StackPopException(address, sp);
		    	if (log.isDebugEnabled()) {
					log.debug(String.format("RuntimeContext.jump throwing %s, returnAddress=0x%08X, stack=%s", e.toString(), returnAddress, currentRuntimeThread.getStackString()));
		    	}
		    	throw e;
			}

			int previousSp = currentRuntimeThread.pushStackState(returnAddress, sp);
			while (true) {
				try {
					returnValue = jumpCall(address, returnAddress, true);
				} catch (StackPopException e) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("RuntimeContext.jump catched %s, returnAddress=0x%08X, stack=%s", e.toString(), returnAddress, currentRuntimeThread.getStackString()));
					}

					if (e.getRa() == returnAddress || e.getRa() == alternativeReturnAddress) {
						returnValue = e.getRa();
						break;
					}
					currentRuntimeThread.popStackState(returnAddress, previousSp);
					throw e;
				}

			    if (returnValue == returnAddress || returnValue == alternativeReturnAddress) {
			    	break;
			    } else if (currentRuntimeThread.hasStackState(returnValue, cpu.gpr[29])) {
			    	StackPopException e = new StackPopException(returnValue, cpu.gpr[29]);
			    	if (log.isDebugEnabled()) {
						log.debug(String.format("RuntimeContext.jump throwing %s, returnAddress=0x%08X, stack=%s", e.toString(), returnAddress, currentRuntimeThread.getStackString()));
			    	}
			    	currentRuntimeThread.popStackState(returnAddress, previousSp);
			    	throw e;
			    } else {
			    	address = returnValue;
			    }
			}
	    	currentRuntimeThread.popStackState(returnAddress, previousSp);
		}

    	if (debugCodeBlockCalls && log.isDebugEnabled()) {
			log.debug(String.format("RuntimeContext.jump returning 0x%08X, address=0x%08X, returnAddress=0x%08X, alternativeReturnAddress=0x%08X", returnValue, address, returnAddress, alternativeReturnAddress));
    	}

    	return returnValue;
	}

    public static void call(int address, int returnAddress) throws Exception {
		if (debugCodeBlockCalls && log.isDebugEnabled()) {
			log.debug(String.format("RuntimeContext.call address=0x%08X, returnAddress=0x%08X", address, returnAddress));
		}
        int returnValue = jumpCall(address, returnAddress, false);

        if (returnValue != returnAddress) {
            log.error(String.format("RuntimeContext.call incorrect returnAddress 0x%08X - 0x%08X", returnValue, returnAddress));
        }
    }

	public static int executeInterpreter(int address, int returnAddress, int alternativeReturnAddress, boolean isJump) throws Exception {
		if (debugCodeBlockCalls && log.isDebugEnabled()) {
			log.debug(String.format("RuntimeContext.executeInterpreter address=0x%08X, returnAddress=0x%08X, alternativeReturnAddress=0x%08X, isJump=%b", address, returnAddress, alternativeReturnAddress, isJump));
		}

		boolean interpret = true;
		cpu.pc = address;
		int returnValue = returnAddress;
		while (interpret) {
			int opcode = cpu.fetchOpcode();
			Instruction insn = Decoder.instruction(opcode);
			insn.interpret(processor, opcode);
			if (insn.hasFlags(Instruction.FLAG_STARTS_NEW_BLOCK)) {
				cpu.pc = jumpCall(cpu.pc, cpu.gpr[31], false);
			} else if (insn.hasFlags(Instruction.FLAG_ENDS_BLOCK)) {
				if (cpu.pc == returnAddress || cpu.pc == alternativeReturnAddress) {
					interpret = false;
					returnValue = cpu.pc;
				}
			}
		}

		return returnValue;
	}

	public static void execute(int opcode) {
		Instruction insn = Decoder.instruction(opcode);
		execute(insn, opcode);
	}

    public static void debugCodeBlockStart(int address, int returnAddress, int alternativeReturnAddress, boolean isJump) {
    	if (log.isDebugEnabled()) {
    		String comment = "";
    		int syscallAddress = address + 4;
    		if (Memory.isAddressGood(syscallAddress)) {
        		int syscallOpcode = memory.read32(syscallAddress);
        		Instruction syscallInstruction = Decoder.instruction(syscallOpcode);
        		if (syscallInstruction == Instructions.SYSCALL) {
            		String syscallDisasm = syscallInstruction.disasm(syscallAddress, syscallOpcode);
        			comment = syscallDisasm.substring(19);
        		}
    		}
    		log.debug(String.format("Starting CodeBlock 0x%08X%s, returnAddress=0x%08X, alternativeReturnAddress=0x%08X, isJump=%b", address, comment, returnAddress, alternativeReturnAddress, isJump));
    	}
    }

    public static void debugCodeBlockEnd(int address, int returnAddress) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Returning from CodeBlock 0x%08X to 0x%08X", address, returnAddress));
    	}
    }

    public static void debugCodeInstruction(int address, int opcode) {
    	if (log.isTraceEnabled()) {
    		cpu.pc = address;
    		Instruction insn = Decoder.instruction(opcode);
    		char compileFlag = insn.hasFlags(Instruction.FLAG_INTERPRETED) ? 'I' : 'C';
    		log.trace(String.format("Executing 0x%08X %c - %s", address, compileFlag, insn.disasm(address, opcode)));
    	}
    }

    private static boolean initialise() {
        if (!isActive) {
            return false;
        }

        if (enableDaemonThreadSync && runtimeSyncThread == null) {
        	runtimeSyncThread = new RuntimeSyncThread();
        	runtimeSyncThread.setName("Sync Daemon");
        	runtimeSyncThread.setDaemon(true);
        	runtimeSyncThread.start();
        }

        memory = Emulator.getMemory();
		if (memory instanceof FastMemory) {
			memoryInt = ((FastMemory) memory).getAll();
		} else {
		    memoryInt = null;
		}

        if (State.debugger == null) {
        	enableDebugger = false;
        } else {
        	enableDebugger = true;
        }

        Profiler.initialise();

        sceDisplayModule = Modules.sceDisplayModule;

        return true;
    }

    public static void executeCallback() {
    	if (!isActive) {
    		return;
    	}

    	CpuState callbackCpuState = Emulator.getProcessor().cpu;
    	SceKernelThreadInfo previousCurrentThread = currentThread;
    	currentThread = Modules.ThreadManForUserModule.getCurrentThread();
	    executeCallbackImmediately(callbackCpuState);
	    currentThread = previousCurrentThread;
    }

    public static void executeCallback(SceKernelThreadInfo callbackThreadInfo) {
    	if (!isActive) {
    		return;
    	}

    	CpuState callbackCpuState = Emulator.getProcessor().cpu;
        RuntimeThread callbackThread = threads.get(callbackThreadInfo);
        boolean doSyncThread = true;

        // The callback has to be executed by its thread
    	if (Thread.currentThread() == callbackThread && currentThread == callbackThreadInfo) {
    	    // The current thread is the thread where the callback has to be executed.
    	    // Execute the callback immediately
    	    executeCallbackImmediately(callbackCpuState);
        } else if (Thread.currentThread() == callbackThread && currentThread != callbackThreadInfo) {
            // The current thread is the thread where the callback has to be executed,
            // but the ThreadMan has already switched to another thread.
            // Execute the callback immediately and restore the wanted thread.
            SceKernelThreadInfo previousThread = currentThread;
            switchThread(callbackThreadInfo);
            executeCallbackImmediately(callbackCpuState);
            switchThread(previousThread);
        } else if (Modules.ThreadManForUserModule.isIdleThread(callbackThreadInfo)) {
        	// We want to execute the callback in the idle thread.
        	// Set the currentThread to a non-null value before executing the callback.
            SceKernelThreadInfo previousThread = currentThread;
        	currentThread = Modules.ThreadManForUserModule.getCurrentThread();
    	    executeCallbackImmediately(callbackCpuState);
    	    currentThread = previousThread;
    	    doSyncThread = false;
    	} else {
    	    // Switch to the callback thread so that it can execute the callback.
    	    pendingCallbackThread = callbackThreadInfo;
    	    pendingCallbackReturnThread = currentThread;
    	    pendingCallbackCpuState = callbackCpuState;
            // The callback thread is switching back to us just after
            // executing the callback.
            switchThread(callbackThreadInfo);
    	}

    	if (doSyncThread) {
	    	try {
	            syncThreadImmediately();
	        } catch (StopThreadException e) {
	            // This exception is not expected at this point...
	            log.warn(e);
	        }
    	}
    }

    private static void executeCallbackImmediately(CpuState cpu) {
        if (cpu == null) {
            return;
        }

        Emulator.getProcessor().cpu = cpu;
    	update();

    	int pc = cpu.pc;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Start of Callback 0x%08X", pc));
		}

    	IExecutable executable = getExecutable(pc);
        int newPc = 0;
        int returnAddress = cpu.gpr[31];
		try {
			newPc = executable.exec(returnAddress, returnAddress, false);
		} catch (StopThreadException e) {
			// Ignore exception
		} catch (Exception e) {
			log.error(e);
		}
    	cpu.pc = newPc;
    	cpu.npc = newPc; // npc is used when context switching

		if (log.isDebugEnabled()) {
			log.debug(String.format("End of Callback 0x%08X", pc));
		}
    }

    private static void updateStaticVariables() {
		emulator = Emulator.getInstance();
		processor = Emulator.getProcessor();
		cpu = processor.cpu;
		if (cpu != null) {
		    gpr = processor.cpu.gpr;
		    fpr = processor.cpu.fpr;
		    vpr = processor.cpu.vpr;
		}
    }

    public static void update() {
        if (!isActive) {
            return;
        }

        updateStaticVariables();

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (!IntrManager.getInstance().isInsideInterrupt()) {
            SceKernelThreadInfo newThread = threadMan.getCurrentThread();
            if (newThread != null && newThread != currentThread) {
                switchThread(newThread);
            }
        }
	}

    private static void switchThread(SceKernelThreadInfo threadInfo) {
    	if (log.isDebugEnabled()) {
    		String name;
    		if (threadInfo == null) {
    			name = "Idle";
    		} else {
    			name = threadInfo.name;
    		}

    		if (currentThread == null) {
        		log.debug("Switching to Thread " + name);
    		} else {
        		log.debug("Switching from Thread " + currentThread.name + " to " + name);
    		}
    	}

    	if (threadInfo == null || Modules.ThreadManForUserModule.isIdleThread(threadInfo)) {
    	    isIdle = true;
    	    currentThread = null;
    	    currentRuntimeThread = null;
    	} else if (toBeStoppedThreads.containsKey(threadInfo)) {
    		// This thread must stop immediately
    		isIdle = true;
    		currentThread = null;
    		currentRuntimeThread = null;
    	} else {
        	RuntimeThread thread = threads.get(threadInfo);
        	if (thread == null) {
        		thread = new RuntimeThread(threadInfo);
        		threads.put(threadInfo, thread);
        		thread.start();
        	}

        	currentThread = threadInfo;
        	currentRuntimeThread = thread;
            isIdle = false;
    	}
    }

    private static void sleep(int micros) {
    	sleep(micros / 1000, micros % 1000);
    }

    private static void sleep(int millis, int micros) {
        try {
        	if (micros <= 0) {
        		Thread.sleep(millis);
        	} else {
        		Thread.sleep(millis, micros * 1000);
        	}
        } catch (InterruptedException e) {
        	// Ignore exception
        }
    }

    private static void syncIdle() throws StopThreadException {
        if (isIdle) {
        	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            Scheduler scheduler = Emulator.getScheduler();

            log.debug("Starting Idle State...");
            idleDuration.start();
            while (isIdle) {
            	checkStoppedThread();
            	{
            		// Do not take the duration of sceDisplay into idleDuration
            		idleDuration.end();
            		syncEmulator(true);
            		idleDuration.start();
            	}
                syncPause();
                scheduler.step();
                if (threadMan.isIdleThread(threadMan.getCurrentThread())) {
                	threadMan.hleRescheduleCurrentThread();
                }

                if (isIdle) {
                	long delay = scheduler.getNextActionDelay(idleSleepMicros);
                	if (delay > 0) {
                		int intDelay;
	                	if (delay >= idleSleepMicros) {
	                		intDelay = idleSleepMicros;
	                	} else {
	                		intDelay = (int) delay;
	                	}
                		sleep(intDelay / 1000, intDelay % 1000);
                	}
                }
            }
            idleDuration.end();
            log.debug("Ending Idle State");
        }
    }

    private static void syncThreadImmediately() throws StopThreadException {
        Thread currentThread = Thread.currentThread();
    	if (currentRuntimeThread != null &&
                currentThread != currentRuntimeThread && !alreadySwitchedStoppedThreads.contains(currentThread)) {
    		currentRuntimeThread.continueRuntimeExecution();

    		if (currentThread instanceof RuntimeThread) {
    			RuntimeThread runtimeThread = (RuntimeThread) currentThread;
    			if (!alreadyStoppedThreads.containsValue(runtimeThread)) {
	    			log.debug("Waiting to be scheduled...");
					runtimeThread.suspendRuntimeExecution();
	    			log.debug("Scheduled, restarting...");
	    	        checkStoppedThread();

	    	        updateStaticVariables();

	    			if (pendingCallbackThread == RuntimeContext.currentThread) {
	    			    pendingCallbackThread = null;
	    			    executeCallbackImmediately(pendingCallbackCpuState);
	    			    switchThread(pendingCallbackReturnThread);
	    			    syncThread();
	    			}
    			} else {
    				alreadySwitchedStoppedThreads.add(currentThread);
    			}
    		}
    	}
    }

    private static void syncThread() throws StopThreadException {
        syncIdle();

        if (toBeDeletedThreads.containsValue(getRuntimeThread())) {
        	return;
        }

        Thread currentThread = Thread.currentThread();
    	if (log.isDebugEnabled()) {
    		log.debug("syncThread currentThread=" + currentThread.getName() + ", currentRuntimeThread=" + currentRuntimeThread.getName());
    	}
    	syncThreadImmediately();
    }

    private static RuntimeThread getRuntimeThread() {
    	Thread currentThread = Thread.currentThread();
		if (currentThread instanceof RuntimeThread) {
			return (RuntimeThread) currentThread;
		}

		return null;
    }

    private static boolean isStoppedThread() {
    	if (toBeStoppedThreads.isEmpty()) {
    		return false;
    	}

		RuntimeThread runtimeThread = getRuntimeThread();
		if (runtimeThread != null && toBeStoppedThreads.containsValue(runtimeThread)) {
			if (!alreadyStoppedThreads.containsValue(runtimeThread)) {
				return true;
			}
		}

		return false;
    }

    private static void checkStoppedThread() throws StopThreadException {
    	if (isStoppedThread()) {
			throw new StopThreadException("Stopping Thread " + Thread.currentThread().getName());
		}
    }

    private static void syncPause() throws StopThreadException {
    	if (Emulator.pause) {
	        Emulator.getClock().pause();
	        try {
	            synchronized(emulator) {
	               while (Emulator.pause) {
	                   checkStoppedThread();
	                   emulator.wait();
	               }
	           }
	        } catch (InterruptedException e){
	        	// Ignore Exception
	        } finally {
	        	Emulator.getClock().resume();
	        }
    	}
    }

    public static void syncDebugger(int pc) throws StopThreadException {
    	if (State.debugger != null) {
    		processor.cpu.pc = pc;
    		syncDebugger();
    		syncPause();
    	} else if (Emulator.pause) {
    		syncPause();
    	}
    }

    private static void syncDebugger() {
        if (State.debugger != null) {
            State.debugger.step();
        }
    }

    private static void syncEmulator(boolean immediately) {
        if (log.isDebugEnabled()) {
            log.debug("syncEmulator immediately=" + immediately);
        }

        Modules.sceGe_userModule.step();
		Modules.sceDisplayModule.step(immediately);
    }

    private static void syncFast() {
        // Always sync the display to trigger the GE list processing
        Modules.sceDisplayModule.step();
    }

    public static void sync() throws StopThreadException {
    	do {
    		wantSync = false;

	    	if (IntrManager.getInstance().isInsideInterrupt()) {
	    		syncFast();
	    	} else {
		    	syncPause();
				Emulator.getScheduler().step();
				if (Interrupts.isInterruptsEnabled()) {
					Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
				}
				syncThread();
				syncEmulator(false);
		        syncDebugger();
		    	syncPause();
		    	checkStoppedThread();
	        }
    	// Check if a new sync request has been received in the meantime
    	} while (wantSync);
    }

    public static void syscallFast(int code) {
		// Fast syscall: no context switching
    	SyscallHandler.syscall(code);
    	syncFast();
    }

    public static void syscall(int code) throws StopThreadException {
    	if (IntrManager.getInstance().isInsideInterrupt()) {
    		syscallFast(code);
    	} else {
	    	RuntimeThread runtimeThread = getRuntimeThread();
	    	if (runtimeThread != null) {
	    		runtimeThread.setInSyscall(true);
	    	}
	    	checkStoppedThread();
	    	syncPause();

	    	SyscallHandler.syscall(code);

	    	checkStoppedThread();
	    	sync();
	    	if (runtimeThread != null) {
	    		runtimeThread.setInSyscall(false);
	    	}
    	}
    }

    public static void runThread(RuntimeThread thread) {
    	thread.setInSyscall(true);

    	if (isStoppedThread()) {
			// This thread has already been stopped before it is really starting...
    		return;
    	}

		thread.suspendRuntimeExecution();

    	if (isStoppedThread()) {
			// This thread has already been stopped before it is really starting...
    		return;
    	}

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        IExecutable executable = getExecutable(processor.cpu.pc);
		thread.setInSyscall(false);
    	try {
    		updateStaticVariables();
    		executable.exec(ThreadManForUser.THREAD_EXIT_HANDLER_ADDRESS, 0, false);
    		threadMan.hleKernelExitThread(processor);
    	} catch (StopThreadException e) {
    		// Ignore Exception
    	} catch (Exception e) {
    		e.printStackTrace();
    		log.error(e);
		}

		SceKernelThreadInfo threadInfo = thread.getThreadInfo();
    	alreadyStoppedThreads.put(threadInfo, thread);

    	if (log.isDebugEnabled()) {
    		log.debug("End of Thread " + threadInfo.name + " - stopped");
    	}

		threads.remove(threadInfo);
		toBeStoppedThreads.remove(threadInfo);
		toBeDeletedThreads.remove(threadInfo);

		if (!reset) {
			// Switch to the currently active thread
			try {
		    	if (log.isDebugEnabled()) {
		    		log.debug("End of Thread " + threadInfo.name + " - sync");
		    	}

		    	// Be careful to not execute Interrupts or Callbacks by this thread,
		    	// as it is already stopped and the next active thread
		    	// will be resumed immediately.
	    		syncIdle();
	    		syncThreadImmediately();
			} catch (StopThreadException e) {
			}
		}

		alreadyStoppedThreads.remove(threadInfo);
		alreadySwitchedStoppedThreads.remove(thread);

		if (log.isDebugEnabled()) {
			log.debug("End of Thread " + thread.getName());
		}

		synchronized (waitForEnd) {
			waitForEnd.notify();
		}
    }

    public static CodeBlock addCodeBlock(int address, CodeBlock codeBlock) {
        return codeBlocks.put(address, codeBlock);
    }

    public static CodeBlock getCodeBlock(int address) {
	    return codeBlocks.get(address);
	}

    public static boolean hasCodeBlock(int address) {
        return codeBlocks.containsKey(address);
    }

    public static Map<Integer, CodeBlock> getCodeBlocks() {
    	return codeBlocks;
    }

    public static IExecutable getExecutable(int address) {
        CodeBlock codeBlock = codeBlocks.get(address);
        IExecutable executable;
        if (codeBlock == null) {
            executable = Compiler.getInstance().compile(address);
        } else {
            executable = codeBlock.getExecutable();
        }

        return executable;
    }

    public static void run() {
    	if (Modules.ThreadManForUserModule.exitCalled) {
    		return;
    	}

    	if (!initialise()) {
        	isActive = false;
        	return;
        }

        log.info("Using Compiler");

        while (!toBeStoppedThreads.isEmpty()) {
        	wakeupToBeStoppedThreads();
        	sleep(idleSleepMicros);
        }

        reset = false;

        if (currentRuntimeThread == null) {
        	try {
				syncIdle();
			} catch (StopThreadException e) {
				// Thread is stopped, return immediately
				return;
			}

        	if (currentRuntimeThread == null) {
        		log.error("RuntimeContext.run: nothing to run!");
        		Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
        		return;
        	}
        }

        update();

        if (processor.cpu.pc == 0) {
        	Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNKNOWN);
        	return;
        }

        currentRuntimeThread.continueRuntimeExecution();

        while (!threads.isEmpty() && !reset) {
	        synchronized(waitForEnd) {
	        	try {
					waitForEnd.wait();
				} catch (InterruptedException e) {
				}
	        }
        }

        log.debug("End of run");
    }

    private static List<RuntimeThread> wakeupToBeStoppedThreads() {
		List<RuntimeThread> threadList = new LinkedList<RuntimeThread>();
		synchronized (toBeStoppedThreads) {
    		for (Entry<SceKernelThreadInfo, RuntimeThread> entry : toBeStoppedThreads.entrySet()) {
    			threadList.add(entry.getValue());
    		}
		}

		// Trigger the threads to start execution again.
		// Loop on a local list to avoid concurrent modification on toBeStoppedThreads.
		for (RuntimeThread runtimeThread : threadList) {
			Thread.State threadState = runtimeThread.getState();
			log.debug("Thread " + runtimeThread.getName() + ", State=" + threadState);
			if (threadState == Thread.State.TERMINATED) {
				toBeStoppedThreads.remove(runtimeThread.getThreadInfo());
			} else if (threadState == Thread.State.WAITING) {
				runtimeThread.continueRuntimeExecution();
			}
		}

		synchronized (Emulator.getInstance()) {
			Emulator.getInstance().notifyAll();
		}

		return threadList;
    }

    public static void onThreadDeleted(SceKernelThreadInfo thread) {
    	RuntimeThread runtimeThread = threads.get(thread);
    	if (runtimeThread != null) {
    		if (log.isDebugEnabled()) {
    			log.debug("Deleting Thread " + thread.toString());
    		}
    		toBeStoppedThreads.put(thread, runtimeThread);
    		if (runtimeThread.isInSyscall() && Thread.currentThread() != runtimeThread) {
    			toBeDeletedThreads.put(thread, runtimeThread);
    			log.debug("Continue Thread " + runtimeThread.getName());
    			runtimeThread.continueRuntimeExecution();
    		}
    	}
    }

    public static void onThreadExit(SceKernelThreadInfo thread) {
    	RuntimeThread runtimeThread = threads.get(thread);
    	if (runtimeThread != null) {
    		if (log.isDebugEnabled()) {
    			log.debug("Exiting Thread " + thread.toString());
    		}
    		toBeStoppedThreads.put(thread, runtimeThread);
    		threads.remove(thread);
    	}
    }

    private static void stopAllThreads() {
		synchronized (threads) {
			toBeStoppedThreads.putAll(threads);
    		threads.clear();
		}

		List<RuntimeThread> threadList = wakeupToBeStoppedThreads();

		// Wait for all threads to enter a syscall.
		// When a syscall is entered, the thread will exit
		// automatically by calling checkStoppedThread()
		boolean waitForThreads = true;
		while (waitForThreads) {
			waitForThreads = false;
			for (RuntimeThread runtimeThread : threadList) {
				if (!runtimeThread.isInSyscall()) {
					waitForThreads = true;
					break;
				}
			}

			if (waitForThreads) {
				sleep(idleSleepMicros);
			}
		}
    }

    public static void exit() {
        if (isActive) {
    		log.debug("RuntimeContext.exit");
        	stopAllThreads();
        	if (DurationStatistics.collectStatistics) {
        		log.info(idleDuration);

	            if (enableInstructionTypeCounting) {
	            	long totalCount = 0;
	            	for (Instruction insn : instructionTypeCounts.keySet()) {
	            		int count = instructionTypeCounts.get(insn);
	            		totalCount += count;
	            	}

	            	while (!instructionTypeCounts.isEmpty()) {
	            		Instruction highestCountInsn = null;
	            		int highestCount = -1;
	                	for (Instruction insn : instructionTypeCounts.keySet()) {
	                		int count = instructionTypeCounts.get(insn);
	                		if (count > highestCount) {
	                			highestCount = count;
	                			highestCountInsn = insn;
	                		}
	                	}
	                	instructionTypeCounts.remove(highestCountInsn);
	            		log.info(String.format("  %10s %s %d (%2.2f%%)", highestCountInsn.name(), (highestCountInsn.hasFlags(Instruction.FLAG_INTERPRETED) ? "I" : "C"), highestCount, highestCount * 100.0 / totalCount));
	            	}
	            }
        	}
        }
    }

    public static void reset() {
    	if (isActive) {
    		log.debug("RuntimeContext.reset");
    		Compiler.getInstance().reset();
    		codeBlocks.clear();
    		currentThread = null;
    		currentRuntimeThread = null;
    		stopAllThreads();
    		reset = true;
    		synchronized (waitForEnd) {
				waitForEnd.notify();
			}
    	}
    }

    public static void invalidateAll() {
        if (isActive) {
    		log.debug("RuntimeContext.invalidateAll");
            codeBlocks.clear();
            Compiler.getInstance().invalidateAll();
    	}
    }

    public static void instructionTypeCount(Instruction insn, int opcode) {
    	int count = 0;
    	if (instructionTypeCounts.containsKey(insn)) {
    		count = instructionTypeCounts.get(insn);
    	}
    	count++;
    	instructionTypeCounts.put(insn, count);
    }

    public static void pauseEmuWithStatus(int status) throws StopThreadException {
    	Emulator.PauseEmuWithStatus(status);
    	syncPause();
    }

    public static void logInfo(String message) {
    	log.info(message);
    }

    public static int checkMemoryRead32(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!Memory.isRawAddressGood(rawAddress)) {
        	if (memory.read32AllowedInvalidAddress(rawAddress)) {
        		rawAddress = 0;
        	} else {
                int normalizedAddress = memory.normalizeAddress(address);
                if (Memory.isRawAddressGood(normalizedAddress)) {
                    rawAddress = normalizedAddress;
                } else {
		            processor.cpu.pc = pc;
		            memory.invalidMemoryAddress(address, "read32", Emulator.EMU_STATUS_MEM_READ);
		            syncPause();
		            rawAddress = 0;
                }
        	}
        }

        return rawAddress;
    }

    public static int checkMemoryRead16(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!Memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (Memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "read16", Emulator.EMU_STATUS_MEM_READ);
	            syncPause();
	            rawAddress = 0;
            }
        }

        return rawAddress;
    }

    public static int checkMemoryRead8(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!Memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (Memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "read8", Emulator.EMU_STATUS_MEM_READ);
	            syncPause();
	            rawAddress = 0;
            }
        }

        return rawAddress;
    }

    public static int checkMemoryWrite32(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!Memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (Memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "write32", Emulator.EMU_STATUS_MEM_WRITE);
	            syncPause();
	            rawAddress = 0;
            }
        }

        sceDisplayModule.write32(rawAddress);

        return rawAddress;
    }

    public static int checkMemoryWrite16(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!Memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (Memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "write16", Emulator.EMU_STATUS_MEM_WRITE);
	            syncPause();
	            rawAddress = 0;
            }
        }

        sceDisplayModule.write16(rawAddress);

        return rawAddress;
    }

    public static int checkMemoryWrite8(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!Memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (Memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "write8", Emulator.EMU_STATUS_MEM_WRITE);
	            syncPause();
	            rawAddress = 0;
            }
        }

        sceDisplayModule.write8(rawAddress);

        return rawAddress;
    }

    public static void debugMemoryReadWrite(int address, int value, int pc, boolean isRead, int width) {
    	if (log.isTraceEnabled()) {
	    	StringBuilder message = new StringBuilder();
	    	message.append(String.format("0x%08X - ", pc));
	    	if (isRead) {
	    		message.append(String.format("read%d(0x%08X)=0x", width, address));
	    		if (width == 8) {
	    			message.append(String.format("%02X", memory.read8(address)));
	    		} else if (width == 16) {
	    			message.append(String.format("%04X", memory.read16(address)));
	    		} else if (width == 32) {
	    			message.append(String.format("%08X (%f)", memory.read32(address), Float.intBitsToFloat(memory.read32(address))));
	    		}
	    	} else {
	    		message.append(String.format("write%d(0x%08X, 0x", width, address));
	    		if (width == 8) {
	    			message.append(String.format("%02X", value));
	    		} else if (width == 16) {
	    			message.append(String.format("%04X", value));
	    		} else if (width == 32) {
	    			message.append(String.format("%08X (%f)", value, Float.intBitsToFloat(value)));
	    		}
	    		message.append(")");
	    	}
	    	log.trace(message.toString());
    	}
    }

    public static void onNextScheduleModified() {
    	checkSync(false);
    }

    private static void checkSync(boolean sleep) {
    	long delay = Emulator.getScheduler().getNextActionDelay(idleSleepMicros);
    	if (delay > 0) {
    		if (sleep) {
	    		int intDelay = (int) delay;
	    		sleep(intDelay / 1000, intDelay % 1000);
    		}
    	} else if (wantSync) {
    		if (sleep) {
    			sleep(idleSleepMicros);
    		}
    	} else {
    		wantSync = true;
    	}
    }

    public static boolean syncDaemonStep() {
    	checkSync(true);

    	return enableDaemonThreadSync;
    }

    public static void exitSyncDaemon() {
    	runtimeSyncThread = null;
    }

    public static void setIsHomebrew(boolean isHomebrew) {
    	// Currently, nothing special to do for Homebrew's
    }
}