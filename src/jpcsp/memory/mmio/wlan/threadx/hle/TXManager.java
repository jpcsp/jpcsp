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
package jpcsp.memory.mmio.wlan.threadx.hle;

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.arm.ARMInterpreter.PC_END_RUN;
import static jpcsp.arm.ARMProcessor.REG_PC;
import static jpcsp.arm.ARMProcessor.REG_R0;
import static jpcsp.arm.ARMProcessor.REG_R1;
import static jpcsp.arm.ARMProcessor.REG_R2;
import static jpcsp.arm.ARMProcessor.REG_R3;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasFlag;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.arm.ARMDisassembler;
import jpcsp.arm.ARMInterpreter;
import jpcsp.arm.ARMProcessor;
import jpcsp.arm.IARMHLECall;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.wlan.threadx.TXJumpCall;
import jpcsp.memory.mmio.wlan.HLEMemcmp;
import jpcsp.memory.mmio.wlan.HLEMemcpy;
import jpcsp.memory.mmio.wlan.HLEMemset0;
import jpcsp.memory.mmio.wlan.HLENullCall;
import jpcsp.memory.mmio.wlan.HLEStrncpy;
import jpcsp.memory.mmio.wlan.threadx.TXEventFlagsCreate;
import jpcsp.memory.mmio.wlan.threadx.TXEventFlagsGet;
import jpcsp.memory.mmio.wlan.threadx.TXEventFlagsSet;
import jpcsp.memory.mmio.wlan.threadx.TXExecutionISRExit;
import jpcsp.memory.mmio.wlan.threadx.TXInterruptControl;
import jpcsp.memory.mmio.wlan.threadx.TXKernelEnter;
import jpcsp.memory.mmio.wlan.threadx.TXQueueCreate;
import jpcsp.memory.mmio.wlan.threadx.TXQueueReceive;
import jpcsp.memory.mmio.wlan.threadx.TXQueueSend;
import jpcsp.memory.mmio.wlan.threadx.TXSemaphoreCreate;
import jpcsp.memory.mmio.wlan.threadx.TXSemaphoreGet;
import jpcsp.memory.mmio.wlan.threadx.TXSemaphorePut;
import jpcsp.memory.mmio.wlan.threadx.TXThreadCreate;
import jpcsp.memory.mmio.wlan.threadx.TXThreadResume;
import jpcsp.memory.mmio.wlan.threadx.TXThreadSleep;
import jpcsp.memory.mmio.wlan.threadx.TXTimeGet;
import jpcsp.memory.mmio.wlan.threadx.TXTimerActivate;
import jpcsp.memory.mmio.wlan.threadx.TXTimerChange;
import jpcsp.memory.mmio.wlan.threadx.TXTimerCreate;
import jpcsp.memory.mmio.wlan.threadx.TXTimerDeactivate;
import jpcsp.util.Utilities;

/**
 * Basic HLE implementation for the real-time operating system (RTOS) ThreadX.
 * See https://github.com/azure-rtos/threadx
 * 
 * @author gid15
 *
 */
public class TXManager {
	public static final Logger log = Logger.getLogger("threadx");
	// Thread execution state values
	public static final int TX_READY = 0;
	public static final int TX_COMPLETED = 1;
	public static final int TX_TERMINATED = 2;
	public static final int TX_SUSPENDED = 3;
	public static final int TX_SLEEP = 4;
	public static final int TX_QUEUE_SUSP = 5;
	public static final int TX_SEMAPHORE_SUSP = 6;
	public static final int TX_EVENT_FLAG = 7;
	public static final int TX_BLOCK_MEMORY = 8;
	public static final int TX_BYTE_MEMORY = 9;
	public static final int TX_IO_DRIVER = 10;
	public static final int TX_FILE = 11;
	public static final int TX_TCP_IP = 12;
	public static final int TX_MUTEX_SUSP = 13;
	public static final int TX_PRIORITY_CHANGE = 14;
	// API return values
	public static final int TX_SUCCESS             = 0x00;
	public static final int TX_DELETED             = 0x01;
	public static final int TX_NO_MEMORY           = 0x10;
	public static final int TX_POOL_ERROR          = 0x02;
	public static final int TX_PTR_ERROR           = 0x03;
	public static final int TX_WAIT_ERROR          = 0x04;
	public static final int TX_SIZE_ERROR          = 0x05;
	public static final int TX_GROUP_ERROR         = 0x06;
	public static final int TX_NO_EVENTS           = 0x07;
	public static final int TX_OPTION_ERROR        = 0x08;
	public static final int TX_QUEUE_ERROR         = 0x09;
	public static final int TX_QUEUE_EMPTY         = 0x0A;
	public static final int TX_QUEUE_FULL          = 0x0B;
	public static final int TX_SEMAPHORE_ERROR     = 0x0C;
	public static final int TX_NO_INSTANCE         = 0x0D;
	public static final int TX_THREAD_ERROR        = 0x0E;
	public static final int TX_PRIORITY_ERROR      = 0x0F;
	public static final int TX_START_ERROR         = 0x10;
	public static final int TX_DELETE_ERROR        = 0x11;
	public static final int TX_RESUME_ERROR        = 0x12;
	public static final int TX_CALLER_ERROR        = 0x13;
	public static final int TX_SUSPEND_ERROR       = 0x14;
	public static final int TX_TIMER_ERROR         = 0x15;
	public static final int TX_TICK_ERROR          = 0x16;
	public static final int TX_ACTIVATE_ERROR      = 0x17;
	public static final int TX_THRESH_ERROR        = 0x18;
	public static final int TX_SUSPEND_LIFTED      = 0x19;
	public static final int TX_WAIT_ABORTED        = 0x1A;
	public static final int TX_WAIT_ABORT_ERROR    = 0x1B;
	public static final int TX_MUTEX_ERROR         = 0x1C;
	public static final int TX_NOT_AVAILABLE       = 0x1D;
	public static final int TX_NOT_OWNED           = 0x1E;
	public static final int TX_INHERIT_ERROR       = 0x1F;
	public static final int TX_NOT_DONE            = 0x20;
	public static final int TX_CEILING_EXCEEDED    = 0x21;
	public static final int TX_INVALID_CEILING     = 0x22;
	public static final int TX_FEATURE_NOT_ENABLED = 0xFF;
	// API input parameters and general constants
	public static final int TX_AND = 0x2;
	public static final int TX_CLEAR = 0x1;
	public static final int TX_NO_WAIT = 0x0;
	public static final int TX_WAIT_FOREVER = 0xFFFFFFFF;
	//
	public static final boolean disassembleFunctions = true;
	private final Map<Integer, TXThread> txThreads = new HashMap<Integer, TXThread>();
	private final Map<Integer, TXTimer> txTimers = new HashMap<Integer, TXTimer>();
	private final List<TXTimer> activatedTimers = new LinkedList<TXTimer>();
	private final Map<Integer, TXEventFlagsGroup> txEventFlags = new HashMap<Integer, TXEventFlagsGroup>();
	private final Map<Integer, TXQueue> txQueues = new HashMap<Integer, TXQueue>();
	private final Map<Integer, TXSemaphore> txSemaphores = new HashMap<Integer, TXSemaphore>();
	private int systemTick;
	private static final int TICK_NANOS = 10 * 1000000; // 10ms
	private TXThread currentThread;
	private TXThread nextThread;
	private int txIrqHandler;
	private int hleCallIndex;
	public static final int TX_INITIALIZE_IN_PROGRESS = 0xF0F0F0F0;
	public static final int TX_INITIALIZE_ALMOST_DONE = 0xF0F0F0F1;
	public static final int TX_INITIALIZE_IS_FINISHED = 0x00000000;
	public int threadSystemState = TX_INITIALIZE_IN_PROGRESS;
	private boolean pendingIrqException;
	private ARMDisassembler disassembler;

	public TXManager() {
	}

	public void installHLECalls(ARMInterpreter interpreter) {
		RuntimeContext.setLog4jMDC("TX_boot");

		installHLECall(interpreter, 0xFFFF2B79, 0xFFFF233D, new TXKernelEnter(0x00000E69, 0x00000F41, 0x0000C979), new TXKernelEnter(0x00000F59, 0x0000103D, 0x0000EFD9));
		installHLECall(interpreter, 0xFFFF1C01, 0xFFFF13E5, new TXEventFlagsCreate());
		installHLECall(interpreter, 0x00000000, 0xFFFF1479, new TXEventFlagsGet());
		installHLECall(interpreter, 0xFFFF1D39, 0xFFFF1519, new TXEventFlagsSet());
		installHLECall(interpreter, 0xFFFF2341, 0xFFFF1B1D, new TXQueueCreate());
		installHLECall(interpreter, 0x00000000, 0xFFFF1CE1, new TXQueueReceive());
		installHLECall(interpreter, 0x00000000, 0xFFFF1D2D, new TXQueueSend());
		installHLECall(interpreter, 0xFFFF25A5, 0xFFFF1D79, new TXSemaphoreCreate());
		installHLECall(interpreter, 0xFFFF263D, 0xFFFF1E0D, new TXSemaphoreGet());
		installHLECall(interpreter, 0xFFFF26C5, 0xFFFF1E95, new TXSemaphorePut());
		installHLECall(interpreter, 0xFFFF0000, 0xFFFF1ECD, new TXTimerActivate());
		installHLECall(interpreter, 0xFFFF2719, 0xFFFF1EE9, new TXThreadCreate());
		installHLECall(interpreter, 0x00000000, 0xFFFF1F91, new TXTimerDeactivate());
		installHLECall(interpreter, 0xFFFF2945, 0xFFFF2111, new TXTimerCreate());
		installHLECall(interpreter, 0x00000000, 0xFFFF20D5, new TXTimerChange());
		installHLECall(interpreter, 0x00000000, 0xFFFF21C9, new TXThreadResume());
		installHLECall(interpreter, 0x00000000, 0xFFFF39E5, new TXTimeGet());
		installHLECall(interpreter, 0x00000000, 0xFFFF3F59, new TXThreadSleep());
		installHLECall(interpreter, 0xFFFF4C69, 0xFFFF4409, new TXJumpCall(REG_R0));
		installHLECall(interpreter, 0xFFFF4C6B, 0xFFFF440B, new TXJumpCall(REG_R1));
		installHLECall(interpreter, 0xFFFF4C6D, 0xFFFF440D, new TXJumpCall(REG_R2));
		installHLECall(interpreter, 0xFFFF4C6F, 0xFFFF440F, new TXJumpCall(REG_R3));
		installHLECall(interpreter, 0xFFFF4C79, 0xFFFF4419, new TXExecutionISRExit());
		installHLECall(interpreter, 0x00000000, 0xFFFF4599, new HLENullCall(0));
		installHLECall(interpreter, 0xFFFF4F5D, 0xFFFF46FD, new TXInterruptControl());

		// These HLE calls are only used to display debugging information
		registerHLECall(interpreter, 0x00000000, 0x000007A5, new HLEMemcpy());
		registerHLECall(interpreter, 0x00000000, 0x000007A8, new HLEMemcpy());
		registerHLECall(interpreter, 0x00000000, 0x000006ED, new HLEMemcpy());
		registerHLECall(interpreter, 0x00000000, 0x000006F0, new HLEMemcpy());
		registerHLECall(interpreter, 0x00000000, 0xC000002D, new HLEMemcpy());
		registerHLECall(interpreter, 0x00000000, 0xC0000030, new HLEMemcpy());
		registerHLECall(interpreter, 0x00000000, 0xC000000C, new HLEMemcmp());
		registerHLECall(interpreter, 0x00000000, 0x00000535, new HLEMemcmp());
		registerHLECall(interpreter, 0x00000000, 0x00000649, new HLEMemset0());
		registerHLECall(interpreter, 0x00000000, 0x0000064C, new HLEMemset0());
		registerHLECall(interpreter, 0x00000000, 0x00000691, new HLEMemset0());
		registerHLECall(interpreter, 0x00000000, 0x00000694, new HLEMemset0());
		registerHLECall(interpreter, 0x00000000, 0xC000018D, new HLEMemset0());
		registerHLECall(interpreter, 0x00000000, 0xC0000190, new HLEMemset0());
		registerHLECall(interpreter, 0x00000000, 0xC00000FD, new HLEStrncpy());
		registerHLECall(interpreter, 0x00000000, 0xC0000100, new HLEStrncpy());
		registerHLECall(interpreter, 0x00000000, 0x0000F031, new HLEStrncpy());
	}

	private void installHLECall(ARMInterpreter interpreter, int address1, int address2, IARMHLECall hleCall) {
		installHLECall(interpreter, address1, address2, hleCall, hleCall);
	}

	private void installHLECall(ARMInterpreter interpreter, int address1, int address2, IARMHLECall hleCall1, IARMHLECall hleCall2) {
		hleCallIndex++;

		if (Model.getGeneration() >= 2) {
			// Required for PSP generation 2 or later
			if (address2 != 0) {
				interpreter.installHLECall(address2, hleCallIndex, hleCall2);
			}
		} else {
			// Required for PSP generation 1
			if (address1 != 0) {
				interpreter.installHLECall(address1, hleCallIndex, hleCall1);
			}
		}
	}

	private void registerHLECall(ARMInterpreter interpreter, int address1, int address2, IARMHLECall hleCall) {
		registerHLECall(interpreter, address1, address2, hleCall, hleCall);
	}

	private void registerHLECall(ARMInterpreter interpreter, int address1, int address2, IARMHLECall hleCall1, IARMHLECall hleCall2) {
		if (Model.getGeneration() >= 2) {
			// Required for PSP generation 2 or later
			if (address2 != 0) {
				interpreter.registerHLECall(address2, hleCall2);
			}
		} else {
			// Required for PSP generation 1
			if (address1 != 0) {
				interpreter.registerHLECall(address1, hleCall1);
			}
		}
	}

	private void clear(ARMProcessor processor, int address, int size) {
		processor.mem.memset(address, (byte) 0, size);
	}

	public void setTxIrqHandler(int txIrqHandler) {
		this.txIrqHandler = txIrqHandler;

		if (log.isDebugEnabled()) {
			log.debug(String.format("setTxIrqHandler txIrqHandler=0x%08X", this.txIrqHandler));
		}
	}

	public int getSystemTick() {
		return systemTick;
	}

	public void threadSchedule(ARMProcessor processor) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Entering threadSchedule"));
		}

		boolean testIrq = false;
		long previousSystemTickNanos = System.nanoTime();
		while (!Emulator.pause) {
			long nowNanos = System.nanoTime();
			int elapseNanos = (int) (nowNanos - previousSystemTickNanos);
			while (elapseNanos >= TICK_NANOS) {
				incrementSystemTick(processor);
				previousSystemTickNanos += TICK_NANOS;
				elapseNanos -= TICK_NANOS;
			}

			if (pendingIrqException) {
				pendingIrqException = false;
				triggerIrqException(processor);
			}

			if (nextThread == null) {
				if (currentThread != null) {
					threadContextSave(processor, currentThread);
					currentThread = null;
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("No thread to be executed"));
				}
				Utilities.sleep(1);

				// Execute once for testing of IRQ exception
				if (testIrq) {
					processor.mem.getHandlerWlanFirmware().test();
					triggerIrqException(processor);

					if (false) {
						int previousInstr = 0;
						for (int addr = 0x0; addr < 0x11000; addr += 2) {
							int instr = processor.mem.internalRead16(addr);
							if ((instr & 0xF800) == 0xF800 && (previousInstr & 0xF800) == 0xF000) {
								int jumpAddr = addr + 2 + ((previousInstr << 21) >> 9);
								jumpAddr += (instr & 0x7FF) << 1;
								if ((jumpAddr & 0xFFFF0000) == 0xFFFF0000) {
									log.debug(String.format("Found BL 0x%08X at 0x%08X", jumpAddr, addr - 2));
									processor.interpreter.disasm(addr - 20 + 1, 24);
								}
							}
							previousInstr = instr;
						}
						processor.interpreter.disasm(0xC000220F, 0x20);
					}
					testIrq = false;
				}
			} else if (nextThread != currentThread) {
				threadContextSave(processor, currentThread);
				threadContextRestore(processor, nextThread);
				currentThread = nextThread;
				setLog4jMDC("TX_" + currentThread.threadName);
			}

			if (currentThread != null) {
				threadRun(processor, currentThread);
			}
		}
	}

	private void checkWaitingThreads(ARMProcessor processor) {
		boolean systemReturn = false;
		for (TXThread txThread : txThreads.values()) {
			if (txThread.isWaiting()) {
				if (txThread.waitOption != TX_WAIT_FOREVER) {
					// Has the wait expired?
					if (txThread.waitStartTicks + txThread.waitOption <= systemTick) {
						if (threadResume(processor, txThread)) {
							systemReturn = true;
						}
					}
				}
			}
		}

		if (systemReturn) {
			threadSystemReturn(processor);
		}
	}

	public int threadCreate(ARMProcessor processor, int threadPtr, String threadName, int entryFunction, int entryInput, int stackStart, int stackSize, int priority, int preemptThreshold, int timeSlice, int autoStart) {
		clear(processor, threadPtr, TXThread.SIZEOF);

		TXThread txThread = new TXThread();
		txThread.threadPtr = threadPtr;
		txThread.threadName = threadName;
		txThread.entryFunction = entryFunction;
		txThread.entryInput = entryInput;
		txThread.stackStart = stackStart;
		txThread.stackSize = stackSize;
		txThread.priority = priority;
		txThread.preemptyThreshold = preemptThreshold;
		txThread.timeSlice = timeSlice;
		txThread.initRegisters();

		txThreads.put(threadPtr, txThread);

		if (disassembleFunctions) {
			disassemble(processor, String.format("Disassembling %s", txThread), entryFunction);
		}

		if (autoStart != 0) {
			if (threadResume(processor, txThread)) {
				threadSystemReturn(processor);
			}
		}

		return TX_SUCCESS;
	}

	public int threadResume(ARMProcessor processor, int threadPtr) {
		TXThread txThread = txThreads.get(threadPtr);
		if (txThread == null) {
			log.error(String.format("threadResume unknown threadPtr=0x%08X", threadPtr));
			return TX_PTR_ERROR;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXThreadResume %s", txThread));
		}

		if (threadResume(processor, txThread)) {
			threadSystemReturn(processor);
		}

		return TX_SUCCESS;
	}

	public boolean threadResume(ARMProcessor processor, TXThread txThread) {
		if (txThread.state != TX_READY) {
			txThread.state = TX_READY;
			if (log.isDebugEnabled()) {
				log.debug(String.format("threadResume %s", txThread));
			}
		}

		if (currentThread == null) {
			nextThread = txThread;
		} else if (txThread.priority < currentThread.priority) {
			nextThread = txThread;
		}

		boolean preemption = false;
		if (currentThread != nextThread) {
			preemption = true;
		}

		return preemption;
	}

	public int threadSleep(ARMProcessor processor, int timerTicks) {
		TXThread txThread = currentThread;

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXThreadSleep %s", txThread));
		}

		setThreadWait(processor, txThread, TX_SLEEP, 0, timerTicks);

		return TX_SUCCESS;
	}

	private void rescheduleThread(ARMProcessor processor, TXThread changedThread) {
		nextThread = null;
		for (TXThread txThread : txThreads.values()) {
			if (txThread.state == TX_READY) {
				if (nextThread == null) {
					nextThread = txThread;
				} else if (nextThread.priority > txThread.priority) {
					nextThread = txThread;
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("rescheduleThread nextThread=%s", nextThread));
		}
	}

	public void threadSuspend(ARMProcessor processor, TXThread txThread) {
		rescheduleThread(processor, txThread);
		threadSystemReturn(processor);
	}

	private void threadSystemReturn(ARMProcessor processor) {
		if (threadSystemState == TX_INITIALIZE_IS_FINISHED) {
			// Return to the threadSchedule loop
			processor.interpreter.exitInterpreter();
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("threadSystemReturn threadSystemState=0x%X", threadSystemState));
			}
		}
	}

	private void setThreadWait(ARMProcessor processor, TXThread txThread, int state, int waitObjectPtr, int waitOption) {
		txThread.state = state;
		txThread.waitObjectPtr = waitObjectPtr;
		txThread.waitOption = waitOption;
		txThread.waitStartTicks = systemTick + 1;
		threadSuspend(processor, txThread);
	}

	private void threadRun(ARMProcessor processor, TXThread txThread) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("threadRun %s", txThread));
		}

		processor.interpreter.run();
	}

	private void threadContextSave(ARMProcessor processor, TXThread txThread) {
		if (txThread == null) {
			return;
		}

		txThread.savedCpsr = processor.getCpsr();
		for (int i = 0; i < ARMProcessor.NUMBER_REGISTERS - 1; i++) {
			txThread.savedRegisters[i] = processor.getRegister(i);
		}
		txThread.savedRegisters[REG_PC] = processor.getNextInstructionPc();
	}

	private void threadContextRestore(ARMProcessor processor, TXThread txThread) {
		if (txThread == null) {
			return;
		}

		processor.setCpsr(txThread.savedCpsr);
		for (int i = 0; i < ARMProcessor.NUMBER_REGISTERS; i++) {
			processor.setRegister(i, txThread.savedRegisters[i]);
		}
	}

	private void incrementSystemTick(ARMProcessor processor) {
		systemTick++;

		checkExpiredTimers(processor);
		checkWaitingThreads(processor);
	}

	private void checkExpiredTimers(ARMProcessor processor) {
		List<TXTimer> expiredTimers = null;
		for (TXTimer txTimer : activatedTimers) {
			if (txTimer.expirationTicks <= systemTick) {
				if (expiredTimers == null) {
					expiredTimers = new LinkedList<TXTimer>();
				}
				expiredTimers.add(txTimer);
			}
		}

		if (expiredTimers != null) {
			activatedTimers.removeAll(expiredTimers);
			threadContextSave(processor, currentThread);
			for (TXTimer txTimer : expiredTimers) {
				processExpiredTimer(processor, txTimer);
			}
			threadContextRestore(processor, currentThread);
		}
	}

	private void processExpiredTimer(ARMProcessor processor, TXTimer txTimer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Starting CodeBlock 0x%08X for %s", clearBit(txTimer.expirationFunction, 0), txTimer));
		}

		processor.setRegister(ARMProcessor.REG_R0, txTimer.expirationInput);
		processor.setLr(PC_END_RUN);
		processor.jumpWithMode(txTimer.expirationFunction);
		processor.interpreter.run();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Returning from CodeBlock 0x%08X for %s", clearBit(txTimer.expirationFunction, 0), txTimer));
		}

		txTimer.remainingTicks = txTimer.rescheduleTicks;
		timerActivate(processor, txTimer);
	}

	public int timerCreate(ARMProcessor processor, int timerPtr, String timerName, int expirationFunction, int expirationInput, int initialTicks, int rescheduleTicks, int autoActivate) {
		clear(processor, timerPtr, TXTimer.SIZEOF);

		TXTimer txTimer = new TXTimer();
		txTimer.timerPtr = timerPtr;
		txTimer.timerName = timerName;
		txTimer.expirationFunction = expirationFunction;
		txTimer.expirationInput = expirationInput;
		txTimer.remainingTicks = initialTicks;
		txTimer.rescheduleTicks = rescheduleTicks;

		txTimers.put(timerPtr, txTimer);

		if (disassembleFunctions) {
			disassemble(processor, String.format("Disassembling %s", txTimer), expirationFunction);
		}

		if (autoActivate != 0) {
			timerActivate(processor, txTimer);
		}

		return TX_SUCCESS;
	}

	public void timerActivate(ARMProcessor processor, TXTimer txTimer) {
		if (txTimer.remainingTicks > 0) {
			txTimer.expirationTicks = systemTick + txTimer.remainingTicks;
			activatedTimers.add(txTimer);
		}
	}

	public int timerActivate(ARMProcessor processor, int timerPtr) {
		TXTimer txTimer = txTimers.get(timerPtr);
		if (txTimer == null) {
			log.error(String.format("timerActivate unknown timerPtr=0x%08X", timerPtr));
			return TX_PTR_ERROR;
		}

		timerActivate(processor, txTimer);

		return TX_SUCCESS;
	}

	public void timerDeactivate(ARMProcessor processor, TXTimer txTimer) {
		activatedTimers.remove(txTimer);
	}

	public int timerDeactivate(ARMProcessor processor, int timerPtr) {
		TXTimer txTimer = txTimers.get(timerPtr);
		if (txTimer == null) {
			log.error(String.format("timerDeactivate unknown timerPtr=0x%08X", timerPtr));
			return TX_PTR_ERROR;
		}

		timerDeactivate(processor, txTimer);

		return TX_SUCCESS;
	}

	public int timerChange(ARMProcessor processor, int timerPtr, int initialTicks, int rescheduleTicks) {
		TXTimer txTimer = txTimers.get(timerPtr);
		if (txTimer == null) {
			log.error(String.format("timerChange unknown timerPtr=0x%08X", timerPtr));
			return TX_PTR_ERROR;
		}

		txTimer.remainingTicks = initialTicks;
		txTimer.rescheduleTicks = rescheduleTicks;

		return TX_SUCCESS;
	}

	public void triggerIrqException() {
		pendingIrqException = true;
	}

	private void triggerIrqException(ARMProcessor processor) {
		if (currentThread != null) {
			threadContextSave(processor, currentThread);
		}

		threadSystemState++;
		if (log.isDebugEnabled()) {
			log.debug(String.format("Starting CodeBlock 0x%08X for IRQ Handler", clearBit(txIrqHandler, 0)));
		}

		processor.setLr(PC_END_RUN);
		processor.jumpWithMode(txIrqHandler);
		processor.interpreter.run();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Returning from CodeBlock 0x%08X for IRQ Handler", clearBit(txIrqHandler, 0)));
		}
		threadSystemState--;

		if (currentThread != null) {
			threadContextRestore(processor, currentThread);
		}
	}

	public int eventFlagsCreate(ARMProcessor processor, int groupPtr, String groupName) {
		clear(processor, groupPtr, TXEventFlagsGroup.SIZEOF);

		TXEventFlagsGroup txEventFlagsGroup = new TXEventFlagsGroup();
		txEventFlagsGroup.groupPtr = groupPtr;
		txEventFlagsGroup.groupName = groupName;

		txEventFlags.put(groupPtr, txEventFlagsGroup);

		return TX_SUCCESS;
	}

	private boolean checkEventFlagsGet(ARMProcessor processor, int requestedFlags, int getOption, int actualFlagsPtr, TXEventFlagsGroup txEventFlagsGroup) {
		boolean retrieved;
		if (hasFlag(getOption, TX_AND)) {
			// TX_AND
			retrieved = (txEventFlagsGroup.current & requestedFlags) == requestedFlags;
		} else {
			// TX_OR
			retrieved = hasFlag(txEventFlagsGroup.current, requestedFlags);
		}

		if (retrieved) {
			int retrievedFlags = txEventFlagsGroup.current & requestedFlags;
			if (hasFlag(getOption, TX_CLEAR)) {
				txEventFlagsGroup.current = clearFlag(txEventFlagsGroup.current, retrievedFlags);
			}
			processor.mem.write32(actualFlagsPtr, retrievedFlags);
		}

		return retrieved;
	}

	private void checkThreadsWaitingOnEventFlags(ARMProcessor processor, TXEventFlagsGroup txEventFlagsGroup) {
		boolean systemReturn = false;
		for (TXThread txThread : txThreads.values()) {
			if (txThread.state == TX_EVENT_FLAG && txThread.waitObjectPtr == txEventFlagsGroup.groupPtr) {
				if (checkEventFlagsGet(processor, txThread.waitEventFlagsRequestedFlags, txThread.waitEventFlagsGetOption, txThread.waitEventFlagsActualFlagsPtr, txEventFlagsGroup)) {
					txThread.savedRegisters[REG_R0] = TX_SUCCESS;
					if (threadResume(processor, txThread)) {
						systemReturn = true;
					}
				}
			}
		}

		if (systemReturn) {
			threadSystemReturn(processor);
		}
	}

	public int eventFlagsSet(ARMProcessor processor, int groupPtr, int flagsToSet, int setOption) {
		TXEventFlagsGroup txEventFlagsGroup = txEventFlags.get(groupPtr);
		if (txEventFlagsGroup == null) {
			log.error(String.format("eventFlagsSet unknown groupPtr=0x%08X", groupPtr));
			return TX_PTR_ERROR;
		}

		if (hasFlag(setOption, TX_AND)) {
			// TX_AND
			txEventFlagsGroup.current &= flagsToSet;
		} else {
			// TX_OR
			txEventFlagsGroup.current |= flagsToSet;
		}

		checkThreadsWaitingOnEventFlags(processor, txEventFlagsGroup);

		return TX_SUCCESS;
	}

	public int eventFlagsGet(ARMProcessor processor, int groupPtr, int requestedFlags, int getOption, int actualFlagsPtr, int waitOption) {
		TXEventFlagsGroup txEventFlagsGroup = txEventFlags.get(groupPtr);
		if (txEventFlagsGroup == null) {
			log.error(String.format("eventFlagsGet unknown groupPtr=0x%08X", groupPtr));
			return TX_PTR_ERROR;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("TXEventFlagsGet txEventFlagsGroup=%s", txEventFlagsGroup));
		}

		int result;
		if (checkEventFlagsGet(processor, requestedFlags, getOption, actualFlagsPtr, txEventFlagsGroup)) {
			result = TX_SUCCESS;
		} else if (waitOption == TX_NO_WAIT) {
			result = TX_NO_EVENTS;
		} else {
			TXThread txThread = currentThread;
			if (log.isDebugEnabled()) {
				log.debug(String.format("TXEventFlagsGet suspending %s", txThread));
			}
			setThreadWait(processor, txThread, TX_EVENT_FLAG, groupPtr, waitOption);
			txThread.waitEventFlagsRequestedFlags = requestedFlags;
			txThread.waitEventFlagsGetOption = getOption;
			txThread.waitEventFlagsActualFlagsPtr = actualFlagsPtr;
			result = TX_NO_EVENTS;
		}

		return result;
	}

	public int queueCreate(ARMProcessor processor, int queuePtr, String queueName, int messageSize, int queueStart, int queueSize) {
		clear(processor, queuePtr, TXQueue.SIZEOF);

		TXQueue txQueue = new TXQueue();
		txQueue.queuePtr = queuePtr;
		txQueue.queueName = queueName;
		txQueue.messageSize = messageSize;
		txQueue.queueStart = queueStart;
		txQueue.capacity = queueSize / (messageSize << 2);
		txQueue.init();

		txQueues.put(queuePtr, txQueue);

		return TX_SUCCESS;
	}

	private void checkThreadsWaitingOnQueueReceive(ARMProcessor processor, TXQueue txQueue) {
		boolean systemReturn = false;
		for (TXThread txThread : txThreads.values()) {
			if (txThread.state == TX_QUEUE_SUSP && txThread.waitObjectPtr == txQueue.queuePtr && txThread.waitQueueReceive) {
				if (checkQueueReceive(processor, txThread.waitQueueDestinationPtr, txThread.waitOption, txQueue)) {
					txThread.savedRegisters[REG_R0] = TX_SUCCESS;
					if (threadResume(processor, txThread)) {
						systemReturn = true;
					}
				}
			}
		}

		if (systemReturn) {
			threadSystemReturn(processor);
		}
	}

	private void checkThreadsWaitingOnQueueSend(ARMProcessor processor, TXQueue txQueue) {
		boolean systemReturn = false;
		for (TXThread txThread : txThreads.values()) {
			if (txThread.state == TX_QUEUE_SUSP && txThread.waitObjectPtr == txQueue.queuePtr && !txThread.waitQueueReceive) {
				if (checkQueueSend(processor, txThread.waitQueueSourcePtr, txThread.waitOption, txQueue)) {
					txThread.savedRegisters[REG_R0] = TX_SUCCESS;
					if (threadResume(processor, txThread)) {
						systemReturn = true;
					}
				}
			}
		}

		if (systemReturn) {
			threadSystemReturn(processor);
		}
	}

	private boolean checkQueueReceive(ARMProcessor processor, int destinationPtr, int waitOption, TXQueue txQueue) {
		if (txQueue.enqueued <= 0) {
			return false;
		}

		int sizeInBytes = txQueue.messageSize << 2;
		processor.mem.memcpy(destinationPtr, txQueue.queueRead, sizeInBytes);
		txQueue.queueRead += sizeInBytes;
		// Are we at the end of queue?
		if (txQueue.queueRead == txQueue.queueEnd) {
			// If yes, wrap around to the beginning
			txQueue.queueRead = txQueue.queueStart;
		}
		txQueue.availableStorage++;
		txQueue.enqueued--;

		checkThreadsWaitingOnQueueSend(processor, txQueue);

		return true;
	}

	private boolean checkQueueSend(ARMProcessor processor, int sourcePtr, int waitOption, TXQueue txQueue) {
		if (txQueue.availableStorage <= 0) {
			return false;
		}

		int sizeInBytes = txQueue.messageSize << 2;
		processor.mem.memcpy(txQueue.queueWrite, sourcePtr, sizeInBytes);
		txQueue.queueWrite += sizeInBytes;
		// Are we at the end of queue?
		if (txQueue.queueWrite == txQueue.queueEnd) {
			// If yes, wrap around to the beginning
			txQueue.queueWrite = txQueue.queueStart;
		}
		txQueue.availableStorage--;
		txQueue.enqueued++;

		checkThreadsWaitingOnQueueReceive(processor, txQueue);

		return true;
	}

	public int queueReceive(ARMProcessor processor, int queuePtr, int destinationPtr, int waitOption) {
		TXQueue txQueue = txQueues.get(queuePtr);
		if (txQueue == null) {
			log.error(String.format("queueReceive unknown queuePtr=0x%08X", queuePtr));
			return TX_PTR_ERROR;
		}

		int result;
		if (checkQueueReceive(processor, destinationPtr, waitOption, txQueue)) {
			result = TX_SUCCESS;
		} else if (waitOption == TX_NO_WAIT) {
			result = TX_QUEUE_EMPTY;
		} else {
			TXThread txThread = currentThread;
			if (log.isDebugEnabled()) {
				log.debug(String.format("TXQueueReceive suspending %s", txThread));
			}
			setThreadWait(processor, txThread, TX_QUEUE_SUSP, queuePtr, waitOption);
			txThread.waitQueueReceive = true;
			txThread.waitQueueDestinationPtr = destinationPtr;
			result = TX_QUEUE_EMPTY;
		}

		return result;
	}

	public int queueSend(ARMProcessor processor, int queuePtr, int sourcePtr, int waitOption) {
		TXQueue txQueue = txQueues.get(queuePtr);
		if (txQueue == null) {
			log.error(String.format("queueSend unknown queuePtr=0x%08X", queuePtr));
			return TX_PTR_ERROR;
		}

		int result;
		if (checkQueueSend(processor, sourcePtr, waitOption, txQueue)) {
			result = TX_SUCCESS;
		} else if (waitOption == TX_NO_WAIT) {
			result = TX_QUEUE_FULL;
		} else {
			TXThread txThread = currentThread;
			if (log.isDebugEnabled()) {
				log.debug(String.format("TXQueueSend suspending %s", txThread));
			}
			setThreadWait(processor, txThread, TX_QUEUE_SUSP, queuePtr, waitOption);
			txThread.waitQueueReceive = false;
			txThread.waitQueueSourcePtr = sourcePtr;
			result = TX_QUEUE_FULL;
		}

		return result;
	}

	public int semaphoreCreate(ARMProcessor processor, int semaphorePtr, String semaphoreName, int initialCount) {
		clear(processor, semaphorePtr, TXSemaphore.SIZEOF);

		TXSemaphore txSemaphore = new TXSemaphore();
		txSemaphore.semaphorePtr = semaphorePtr;
		txSemaphore.semaphoreName = semaphoreName;
		txSemaphore.count = initialCount;

		txSemaphores.put(semaphorePtr, txSemaphore);

		return TX_SUCCESS;
	}

	private boolean checkSemaphoreGet(ARMProcessor processor, int waitOption, TXSemaphore txSemaphore) {
		if (txSemaphore.count <= 0) {
			return false;
		}

		txSemaphore.count--;

		return true;
	}

	private void checkThreadsWaitingOnSemaphoreGet(ARMProcessor processor, TXSemaphore txSemaphore) {
		boolean systemReturn = false;
		for (TXThread txThread : txThreads.values()) {
			if (txThread.state == TX_SEMAPHORE_SUSP && txThread.waitObjectPtr == txSemaphore.semaphorePtr) {
				if (checkSemaphoreGet(processor, txThread.waitOption, txSemaphore)) {
					txThread.savedRegisters[REG_R0] = TX_SUCCESS;
					if (threadResume(processor, txThread)) {
						systemReturn = true;
					}
				}
			}
		}

		if (systemReturn) {
			threadSystemReturn(processor);
		}
	}

	public int semaphoreGet(ARMProcessor processor, int semaphorePtr, int waitOption) {
		TXSemaphore txSemaphore = txSemaphores.get(semaphorePtr);
		if (txSemaphore == null) {
			log.error(String.format("semaphoreGet unknown semaphorePtr=0x%08X", semaphorePtr));
			return TX_PTR_ERROR;
		}

		int result;
		if (checkSemaphoreGet(processor, waitOption, txSemaphore)) {
			result = TX_SUCCESS;
		} else if (waitOption == TX_NO_WAIT) {
			result = TX_NO_INSTANCE;
		} else {
			TXThread txThread = currentThread;
			if (log.isDebugEnabled()) {
				log.debug(String.format("TXSemaphoreGet suspending %s", txThread));
			}
			setThreadWait(processor, txThread, TX_SEMAPHORE_SUSP, semaphorePtr, waitOption);
			result = TX_NO_INSTANCE;
		}

		return result;
	}

	public int semaphorePut(ARMProcessor processor, int semaphorePtr) {
		TXSemaphore txSemaphore = txSemaphores.get(semaphorePtr);
		if (txSemaphore == null) {
			log.error(String.format("semaphorePut unknown semaphorePtr=0x%08X", semaphorePtr));
			return TX_PTR_ERROR;
		}

		txSemaphore.count++;

		checkThreadsWaitingOnSemaphoreGet(processor, txSemaphore);

		return TX_SUCCESS;
	}

	public int timeGet(ARMProcessor processor) {
		return getSystemTick();
	}

	public void disassemble(ARMProcessor processor, String comment, int addr) {
		if (disassembleFunctions) {
			final Level logLevel = Level.INFO;
			if (log.isEnabledFor(logLevel)) {
				if (disassembler == null) {
					disassembler = new ARMDisassembler(log, logLevel, processor.mem, processor.interpreter);
				}

				if (!disassembler.isAlreadyDisassembled(addr)) {
					if (comment != null) {
						log.log(logLevel, comment);
					}

					disassembler.disasm(addr);
				}
			}
		}
	}
}
