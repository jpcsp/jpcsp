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
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Instructions;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.HLE.SyscallHandler;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.pspdisplay;
import jpcsp.HLE.pspge;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.memory.FastMemory;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class RuntimeContext {
    public  static Logger log = Logger.getLogger("runtime");
	public  static boolean isActive = true;
	public  static volatile int gpr[];
	public  static volatile int memoryInt[];
	public  static volatile Processor processor;
	public  static volatile CpuState cpu;
	public  static volatile Memory memory;
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
	private static final int idleSleepMillis = 1;
	private static volatile Map<Integer, CodeBlock> codeBlocks = Collections.synchronizedMap(new HashMap<Integer, CodeBlock>());
	private static volatile Map<SceKernelThreadInfo, RuntimeThread> threads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	private static volatile Map<SceKernelThreadInfo, RuntimeThread> toBeStoppedThreads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	private static volatile Map<SceKernelThreadInfo, RuntimeThread> alreadyStoppedThreads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	private static volatile List<Thread> alreadySwitchedStoppedThreads = Collections.synchronizedList(new ArrayList<Thread>());
	private static volatile Map<SceKernelThreadInfo, RuntimeThread> toBeDeletedThreads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	public  static volatile SceKernelThreadInfo currentThread = null;
	private static volatile RuntimeThread currentRuntimeThread = null;
	private static volatile Object waitForEnd = new Object();
	private static volatile Emulator emulator;
	private static volatile boolean isIdle = false;
	private static volatile boolean reset = false;
	private static DurationStatistics idleDuration = new DurationStatistics("Idle Time");
	private static Map<Instruction, Integer> instructionTypeCounts = Collections.synchronizedMap(new HashMap<Instruction, Integer>());
	private static SceKernelThreadInfo pendingCallbackThread = null;
    private static SceKernelThreadInfo pendingCallbackReturnThread = null;
	private static CpuState pendingCallbackCpuState = null;
	private static boolean insideCallback = false;
	public  static boolean enableDaemonThreadSync = true;
	public  static final String syncName = "sync";
	private static final long syncIntervalMillis = 1000 / 60;	// Sync at least every 1/60 Second
	public  static volatile boolean wantSync = false;
	private static long lastSyncTime;
	private static RuntimeSyncThread runtimeSyncThread = null;
	private static Set<Integer> fastSyscalls;

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

        if (log.isDebugEnabled()) {
        	log.debug("RuntimeContext.jumpCall returning 0x" + Integer.toHexString(returnValue));
        }

        return returnValue;
	}

	public static int jump(int address, int returnAddress) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("RuntimeContext.jump address=0x" + Integer.toHexString(address) + ", returnAddress=0x" + Integer.toHexString(returnAddress));
		}
	    return jumpCall(address, returnAddress, true);
	}

    public static void call(int address, int returnAddress) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("RuntimeContext.call address=0x" + Integer.toHexString(address) + ", returnAddress=0x" + Integer.toHexString(returnAddress));
		}
        int returnValue = jumpCall(address, returnAddress, false);

        if (returnValue != returnAddress) {
            log.error("RuntimeContext.call incorrect returnAddress 0x" + Integer.toHexString(returnValue) + " - 0x" + Integer.toHexString(returnAddress));
        }
    }

	public static int executeInterpreter(int address, int returnAddress, int alternativeReturnAddress, boolean isJump) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("RuntimeContext.executeInterpreter address=0x" + Integer.toHexString(address) + ", returnAddress=0x" + Integer.toHexString(returnAddress) + ", alternativeReturnAddress=0x" + Integer.toHexString(alternativeReturnAddress) + ", isJump=" + isJump);
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
    		if (memory.isAddressGood(syscallAddress)) {
        		int syscallOpcode = memory.read32(syscallAddress);
        		Instruction syscallInstruction = Decoder.instruction(syscallOpcode);
        		if (syscallInstruction == Instructions.SYSCALL) {
            		String syscallDisasm = syscallInstruction.disasm(syscallAddress, syscallOpcode);
        			comment = syscallDisasm.substring(19);
        		}
    		}
    		log.debug("Starting CodeBlock 0x" + Integer.toHexString(address) + comment + ", returnAddress=0x" + Integer.toHexString(returnAddress) + ", alternativeReturnAddress=0x" + Integer.toHexString(alternativeReturnAddress) + ", isJump=" + isJump);
    	}
    }

    public static void debugCodeBlockEnd(int address, int returnAddress) {
    	if (log.isDebugEnabled()) {
    		log.debug("Returning from CodeBlock 0x" + Integer.toHexString(address) + " to 0x" + Integer.toHexString(returnAddress));
    	}
    }

    public static void debugCodeInstruction(int address, int opcode) {
    	if (log.isTraceEnabled()) {
    		Instruction insn = Decoder.instruction(opcode);
    		log.trace("Executing 0x" + Integer.toHexString(address).toUpperCase() + " - " + insn.disasm(address, opcode));
    	}
    }

    private static boolean initialise() {
        if (!isActive) {
            return false;
        }

        fastSyscalls = new TreeSet<Integer>();
        fastSyscalls.add(0x2058);	// sceKernelGetSystemTime
        fastSyscalls.add(0x2059);	// sceKernelGetSystemTimeWide
        fastSyscalls.add(0x205a);	// sceKernelGetSystemTimeLow (used very often by Skate Park City)

        if (enableDaemonThreadSync && runtimeSyncThread == null) {
        	runtimeSyncThread = new RuntimeSyncThread(syncIntervalMillis);
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

        return true;
    }

    public static void executeCallback(SceKernelThreadInfo callbackThreadInfo) {
    	if (!isActive) {
    		return;
    	}

    	CpuState callbackCpuState = Emulator.getProcessor().cpu;
        RuntimeThread callbackThread = threads.get(callbackThreadInfo);

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
    	} else {
    	    // Switch to the callback thread so that it can execute the callback.
    	    pendingCallbackThread = callbackThreadInfo;
    	    pendingCallbackReturnThread = currentThread;
    	    pendingCallbackCpuState = callbackCpuState;
            // The callback thread is switching back to us just after
            // executing the callback.
            switchThread(callbackThreadInfo);
    	}

    	try {
            syncThread();
        } catch (StopThreadException e) {
            // This exception is not expected at this point...
            log.warn(e);
        }
    }

    private static void executeCallbackImmediately(CpuState cpu) {
        if (cpu == null) {
            return;
        }

        insideCallback = true;
        Emulator.getProcessor().cpu = cpu;
    	update();

    	int pc = cpu.pc;

		if (log.isDebugEnabled()) {
			log.debug("Start of Callback 0x" + Integer.toHexString(pc));
		}

    	IExecutable executable = getExecutable(pc);
        int newPc = 0;
		try {
			newPc = executable.exec(0, 0, false);
		} catch (StopThreadException e) {
			// Ignore exception
		} catch (Exception e) {
			log.error(e);
		}
    	cpu.pc = newPc;

		if (log.isDebugEnabled()) {
			log.debug("End of Callback 0x" + Integer.toHexString(pc));
		}
		insideCallback = false;
    }

    private static void updateStaticVariables() {
		emulator = Emulator.getInstance();
		processor = Emulator.getProcessor();
		cpu = processor.cpu;
		if (cpu != null) {
		    gpr = processor.cpu.gpr;
		}
    }

    public static void update() {
        if (!isActive) {
            return;
        }

        updateStaticVariables();

        ThreadMan threadManager = ThreadMan.getInstance();
        if (!insideCallback) {
            SceKernelThreadInfo newThread = threadManager.getCurrentThread();
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

    	if (threadInfo == null || ThreadMan.getInstance().isIdleThread(threadInfo)) {
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

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        	// Ignore exception
        }
    }

    private static void syncIdle() throws StopThreadException {
        if (isIdle) {
            ThreadMan threadMan = ThreadMan.getInstance();

            log.debug("Starting Idle State...");
            idleDuration.start();
            while (isIdle) {
            	checkStoppedThread();
                syncEmulator(true);
                syncPause();
                threadMan.step();
                if (threadMan.isIdleThread(threadMan.getCurrentThread())) {
                    threadMan.contextSwitch(threadMan.nextThread());
                }

                if (isIdle) {
                    sleep(idleSleepMillis);
                }
            }
            idleDuration.end();
            log.debug("Ending Idle State");
        }
    }

    private static void syncThread() throws StopThreadException {
    	ThreadMan.getInstance().step();

        syncIdle();

        if (toBeDeletedThreads.containsValue(getRuntimeThread())) {
        	return;
        }

        Thread currentThread = Thread.currentThread();
    	if (log.isDebugEnabled()) {
    		log.debug("syncThread currentThread=" + currentThread.getName() + ", currentRuntimeThread=" + currentRuntimeThread.getName());
    	}
    	if (currentThread != currentRuntimeThread && !alreadySwitchedStoppedThreads.contains(currentThread)) {
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

    	pspge.getInstance().step();
		pspdisplay.getInstance().step(immediately);
        HLEModuleManager.getInstance().step();
        State.controller.checkControllerState();
    }

    public static void sync() throws StopThreadException {
    	syncPause();
        syncThread();
    	syncEmulator(false);
        syncDebugger();
    	syncPause();
    	checkStoppedThread();

    	lastSyncTime = System.currentTimeMillis();
    	wantSync = false;
    }

    public static void syscall(int code) throws StopThreadException {
    	if (fastSyscalls.contains(code)) {
    		// Fast syscall: no context switching
    		SyscallHandler.syscall(code);
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

    	IExecutable executable = getExecutable(processor.cpu.pc);
		thread.setInSyscall(false);
    	try {
    		updateStaticVariables();
    		executable.exec(0, 0, false);
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

    	if (!thread.getThreadInfo().do_delete) {
    		thread.setInSyscall(true);

            log.info("Thread exit detected SceUID=" + Integer.toHexString(threadInfo.uid)
                + " name:'" + threadInfo.name + "' return:0x" + Integer.toHexString(gpr[2]));

			ThreadMan threadManager = ThreadMan.getInstance();
			threadInfo.exitStatus = gpr[2];
			threadManager.changeThreadState(threadInfo, SceKernelThreadInfo.PSP_THREAD_STOPPED);
			threadManager.contextSwitch(threadManager.nextThread());

			if (!reset) {
				try {
			    	if (log.isDebugEnabled()) {
			    		log.debug("End of Thread " + threadInfo.name + " - sync");
			    	}

					sync();
				} catch (StopThreadException e) {
		    		// Ignore Exception
				}
			}
    	}

		threads.remove(threadInfo);
		toBeStoppedThreads.remove(threadInfo);
		toBeDeletedThreads.remove(threadInfo);

		if (!reset) {
			// Go out of the idle state before really ending this thread
			// otherwise, no thread will break the idle state...
			try {
				if (isIdle) {
			    	if (log.isDebugEnabled()) {
			    		log.debug("End of Thread " + threadInfo.name + " - syncThread");
			    	}

					syncThread();
				}
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
    	if (ThreadMan.getInstance().exitCalled) {
    		return;
    	}

    	if (!initialise()) {
        	isActive = false;
        	return;
        }

        log.info("Using Compiler");

        while (!toBeStoppedThreads.isEmpty()) {
        	wakeupToBeStoppedThreads();
        	sleep(1);
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
    		log.debug("Deleting Thread " + thread.name);
    		toBeStoppedThreads.put(thread, runtimeThread);
    		if (runtimeThread.isInSyscall() && Thread.currentThread() != runtimeThread) {
    			toBeDeletedThreads.put(thread, runtimeThread);
    			log.debug("Continue Thread " + runtimeThread.getName());
    			runtimeThread.continueRuntimeExecution();
    		}
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
				sleep(1);
			}
		}
    }

    public static void exit() {
        if (isActive) {
    		log.debug("RuntimeContext.exit");
        	stopAllThreads();
            log.info(idleDuration.toString());

            if (enableInstructionTypeCounting) {
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
            		log.info(String.format("  %10s %s %d", highestCountInsn.name(), (highestCountInsn.hasFlags(Instruction.FLAG_INTERPRETED) ? "I" : "C"), highestCount));
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

    public static void instructionTypeCount(Instruction insn) {
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
        if (!memory.isRawAddressGood(rawAddress)) {
        	if (memory.read32AllowedInvalidAddress(rawAddress)) {
        		rawAddress = 0;
        	} else {
                int normalizedAddress = memory.normalizeAddress(address);
                if (memory.isRawAddressGood(normalizedAddress)) {
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
        if (!memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (memory.isRawAddressGood(normalizedAddress)) {
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
        if (!memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (memory.isRawAddressGood(normalizedAddress)) {
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
        if (!memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "write32", Emulator.EMU_STATUS_MEM_WRITE);
	            syncPause();
	            rawAddress = 0;
            }
        }

        return rawAddress;
    }

    public static int checkMemoryWrite16(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "write16", Emulator.EMU_STATUS_MEM_WRITE);
	            syncPause();
	            rawAddress = 0;
            }
        }

        return rawAddress;
    }

    public static int checkMemoryWrite8(int address, int pc) throws StopThreadException {
        int rawAddress = address & Memory.addressMask;
        if (!memory.isRawAddressGood(rawAddress)) {
            int normalizedAddress = memory.normalizeAddress(address);
            if (memory.isRawAddressGood(normalizedAddress)) {
                rawAddress = normalizedAddress;
            } else {
	            processor.cpu.pc = pc;
	            memory.invalidMemoryAddress(address, "write8", Emulator.EMU_STATUS_MEM_WRITE);
	            syncPause();
	            rawAddress = 0;
            }
        }

        return rawAddress;
    }

    public static void debugMemoryReadWrite(int address, int value, int pc, boolean isRead, int width) {
    	if (log.isTraceEnabled()) {
	    	StringBuffer message = new StringBuffer();
	    	message.append(String.format("0x%08X - ", pc));
	    	if (isRead) {
	    		message.append(String.format("read%d(0x%08X)=0x", width, address));
	    		if (width == 8) {
	    			message.append(String.format("%02X", memory.read8(address)));
	    		} else if (width == 16) {
	    			message.append(String.format("%04X", memory.read16(address)));
	    		} else if (width == 32) {
	    			message.append(String.format("%08X", memory.read32(address)));
	    		}
	    	} else {
	    		message.append(String.format("write%d(0x%08X, 0x", width, address));
	    		if (width == 8) {
	    			message.append(String.format("%02X", value));
	    		} else if (width == 16) {
	    			message.append(String.format("%04X", value));
	    		} else if (width == 32) {
	    			message.append(String.format("%08X", value));
	    		}
	    		message.append(")");
	    	}
	    	log.trace(message.toString());
    	}
    }

    public static boolean syncDaemon() {
    	long now = System.currentTimeMillis();
    	if (now - lastSyncTime >= syncIntervalMillis) {
			wantSync = true;
    	}

    	return enableDaemonThreadSync;
    }

    public static void exitSyncDaemon() {
    	runtimeSyncThread = null;
    }

    public static void setIsHomebrew(boolean isHomebrew) {
    	if (isHomebrew) {
    		enableDaemonThreadSync = true;
    	} else {
    		enableDaemonThreadSync = false;
    	}
    }
}
