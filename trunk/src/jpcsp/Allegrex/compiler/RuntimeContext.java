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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.State;
import jpcsp.Allegrex.Decoder;
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
	public  static volatile int memory[];
	public  static volatile Processor processor;
	public  static final boolean enableIntructionCounting = true;
	public  static final boolean enableDebugger = true;
	public  static final String debuggerName = "syncDebugger";
	public  static final boolean debugCodeBlockCalls = false;
	public  static final String debugCodeBlockStart = "debugCodeBlockStart";
	public  static final String debugCodeBlockEnd = "debugCodeBlockEnd";
	public  static final boolean debugCodeInstruction = false;
	public  static final String debugCodeInstructionName = "debugCodeInstruction";
	public  static final boolean enableInstructionTypeCounting = false;
	public  static final String instructionTypeCount = "instructionTypeCount";
	private static final int idleSleepMillis = 1;
	private static volatile Map<Integer, CodeBlock> codeBlocks = Collections.synchronizedMap(new HashMap<Integer, CodeBlock>());
	private static volatile Map<SceKernelThreadInfo, RuntimeThread> threads = Collections.synchronizedMap(new HashMap<SceKernelThreadInfo, RuntimeThread>());
	public  static volatile SceKernelThreadInfo currentThread = null;
	private static volatile RuntimeThread currentRuntimeThread = null;
	private static volatile Object waitForEnd = new Object();
	private static volatile Emulator emulator;
	private static volatile boolean isIdle = false;
	private static DurationStatistics idleDuration = new DurationStatistics("Idle Time");
	private static volatile boolean exitNow = false;
	private static Map<Instruction, Integer> instructionTypeCounts = Collections.synchronizedMap(new HashMap<Instruction, Integer>());

	public static void execute(Instruction insn, int opcode) {
		insn.interpret(processor, opcode);
	}

	private static int jumpCall(int address, int returnAddress, boolean isJump) {
        IExecutable executable = getExecutable(address);
        if (executable == null) {
            // TODO Return to interpreter
            log.error("RuntimeContext.jumpCall - Cannot find executable");
            throw new RuntimeException("Cannot find executable");
        }

        int returnValue = executable.exec(returnAddress, isJump);

        if (log.isDebugEnabled()) {
        	log.debug("RuntimeContext.jumpCall returning 0x" + Integer.toHexString(returnValue));
        }

        return returnValue;
	}

	public static int jump(int address, int returnAddress) {
		if (log.isDebugEnabled()) {
			log.debug("RuntimeContext.jump address=0x" + Integer.toHexString(address) + ", returnAddress=0x" + Integer.toHexString(returnAddress));
		}
	    return jumpCall(address, returnAddress, true);
	}

    public static void call(int address, int returnAddress) {
		if (log.isDebugEnabled()) {
			log.debug("RuntimeContext.call address=0x" + Integer.toHexString(address) + ", returnAddress=0x" + Integer.toHexString(returnAddress));
		}
        int returnValue = jumpCall(address, returnAddress, false);

        if (returnValue != returnAddress) {
            log.error("RuntimeContext.call incorrect returnAddress 0x" + Integer.toHexString(returnValue) + " - 0x" + Integer.toHexString(returnAddress));
        }
    }

    public static void execute(int opcode) {
		Instruction insn = Decoder.instruction(opcode);
		execute(insn, opcode);
	}

    public static void debugCodeBlockStart(int address, int returnAddress, boolean isJump) {
    	log.debug("Starting CodeBlock 0x" + Integer.toHexString(address) + ", returnAddress=0x" + Integer.toHexString(returnAddress) + ", isJump=" + isJump);
    }

    public static void debugCodeBlockEnd(int address, int returnAddress) {
    	log.debug("Returning from CodeBlock 0x" + Integer.toHexString(address) + " to 0x" + Integer.toHexString(returnAddress));
    }

    public static void debugCodeInstruction(int address, int opcode) {
    	Instruction insn = Decoder.instruction(opcode);
		log.debug("Executing 0x" + Integer.toHexString(address).toUpperCase() + " - " + insn.disasm(address, opcode));
    }

    private static boolean initialise() {
        if (!isActive) {
            return false;
        }

        Memory mem = Emulator.getMemory();
		if (mem instanceof FastMemory) {
			memory = ((FastMemory) mem).getAll();
		} else {
			return false;
		}

		return true;
    }

    // TODO executeCallBack() has not yet been tested. Which application is using callbacks?
    public static void executeCallback() {
    	if (!isActive) {
    		return;
    	}

    	update();

    	IExecutable executable = getExecutable(processor.cpu.pc);
		executable.exec(0, false);
    }

    public static void update() {
        if (!isActive) {
            return;
        }

		emulator = Emulator.getInstance();
		processor = Emulator.getProcessor();
		gpr = processor.cpu.gpr;

		ThreadMan threadManager = ThreadMan.getInstance();
		SceKernelThreadInfo newThread = threadManager.getCurrentThread();
		if (newThread != null && newThread != currentThread) {
			switchThread(newThread);
		}
	}

    private static void switchThread(SceKernelThreadInfo threadInfo) {
    	if (log.isDebugEnabled()) {
    		if (currentThread == null) {
        		log.debug("Switching to Thread " + threadInfo.name);
    		} else {
        		log.debug("Switching from Thread " + currentThread.name + " to " + threadInfo.name);
    		}
    	}

    	if (ThreadMan.getInstance().isIdleThread(threadInfo)) {
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
        Emulator.getClock().pause();

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        	// Ignore exception
        }

        Emulator.getClock().resume();
    }

    private static void syncThread() {
        ThreadMan threadMan = ThreadMan.getInstance();
        threadMan.step();

        if (isIdle) {
            log.debug("Starting Idle State...");
            idleDuration.start();
            while (isIdle) {
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

        Thread currentThread = Thread.currentThread();
    	if (log.isDebugEnabled()) {
    		log.debug("syncThread currentThread=" + currentThread.getName() + ", currentRuntimeThread=" + currentRuntimeThread.getName());
    	}
    	if (currentThread != currentRuntimeThread) {
    		currentRuntimeThread.continueRuntimeExecution();

    		if (currentThread instanceof RuntimeThread) {
    			RuntimeThread runtimeThread = (RuntimeThread) currentThread;
    			log.debug("Waiting to be scheduled...");
				runtimeThread.suspendRuntimeExecution();
    			log.debug("Scheduled, restarting...");
    		}
    	}
    }

    private static void syncPause() {
    	if (Emulator.pause || exitNow) {
	        Emulator.getClock().pause();
	        try {
	            synchronized(emulator) {
	               while (Emulator.pause || exitNow) {
	                   emulator.wait();
	               }
	           }
	        } catch (InterruptedException e){
	        }
	        Emulator.getClock().resume();
    	}
    }

    public static void syncDebugger(int pc) {
    	if (State.debugger != null) {
    		processor.cpu.pc = pc;
    		syncDebugger();
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

    private static void sync() {
    	syncPause();
        syncThread();
    	syncEmulator(false);
        syncDebugger();
    	syncPause();
    }

    public static void syscall(int code) {
    	syncPause();
    	SyscallHandler.syscall(code);
    	sync();
    }

    public static void runThread(RuntimeThread thread) {
    	thread.suspendRuntimeExecution();

    	syncPause();

    	IExecutable executable = getExecutable(processor.cpu.pc);
		executable.exec(0, false);

		ThreadMan threadManager = ThreadMan.getInstance();
		SceKernelThreadInfo threadInfo = thread.getThreadInfo();

		threadInfo.exitStatus = gpr[2];
		threadManager.changeThreadState(threadInfo, SceKernelThreadInfo.PSP_THREAD_STOPPED);
		threadManager.contextSwitch(threadManager.nextThread());

		sync();

		threads.remove(thread);

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
        if (!initialise()) {
        	isActive = false;
        	return;
        }

        currentRuntimeThread.continueRuntimeExecution();

        while (!threads.isEmpty()) {
	        synchronized(waitForEnd) {
	        	try {
					waitForEnd.wait();
				} catch (InterruptedException e) {
				}
	        }
        }

        log.debug("End of run");
    }

    public static void exit() {
        if (isActive) {
            exitNow = true;
            log.info(idleDuration.toString());
            if (enableInstructionTypeCounting) {
            	for (Instruction insn : instructionTypeCounts.keySet()) {
            		log.info(String.format("  %10s %d", insn.name(), instructionTypeCounts.get(insn).intValue()));
            	}
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
}
