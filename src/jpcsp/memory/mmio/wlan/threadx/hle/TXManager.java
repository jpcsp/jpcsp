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

import static jpcsp.arm.ARMInterpreter.PC_END_RUN;
import static jpcsp.arm.ARMProcessor.REG_R0;
import static jpcsp.arm.ARMProcessor.REG_R1;
import static jpcsp.arm.ARMProcessor.REG_R2;
import static jpcsp.arm.ARMProcessor.REG_R3;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXThread.TX_THREAD_STATE_READY;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasFlag;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.arm.ARMInterpreter;
import jpcsp.arm.ARMProcessor;
import jpcsp.arm.IARMHLECall;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.wlan.threadx.TXJumpCall;
import jpcsp.memory.mmio.wlan.HLENullCall;
import jpcsp.memory.mmio.wlan.threadx.TXEventFlagsCreate;
import jpcsp.memory.mmio.wlan.threadx.TXEventFlagsSet;
import jpcsp.memory.mmio.wlan.threadx.TXExecutionISRExit;
import jpcsp.memory.mmio.wlan.threadx.TXInterruptControl;
import jpcsp.memory.mmio.wlan.threadx.TXKernelEnter;
import jpcsp.memory.mmio.wlan.threadx.TXQueueCreate;
import jpcsp.memory.mmio.wlan.threadx.TXSemaphoreCreate;
import jpcsp.memory.mmio.wlan.threadx.TXSemaphoreGet;
import jpcsp.memory.mmio.wlan.threadx.TXSemaphorePut;
import jpcsp.memory.mmio.wlan.threadx.TXThreadCreate;
import jpcsp.memory.mmio.wlan.threadx.TXTimerCreate;
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
	public static final int TX_SUCCESS = 0x0;
	public static final int TX_PTR_ERROR = 0x3;
	public static final int TX_SEMAPHORE_ERROR = 0xC;
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

	public void installHLECalls(ARMInterpreter interpreter) {
		installHLECall(interpreter, 0xFFFF2B79, 0xFFFF233D, new TXKernelEnter(0x00000E69, 0x00000F41, 0x0000C979), new TXKernelEnter(0x00000F59, 0x0000103D, 0x0000EFD9));
		installHLECall(interpreter, 0xFFFF1C01, 0xFFFF13E5, new TXEventFlagsCreate());
		installHLECall(interpreter, 0xFFFF1D39, 0xFFFF1519, new TXEventFlagsSet());
		installHLECall(interpreter, 0xFFFF2341, 0xFFFF1B1D, new TXQueueCreate());
		installHLECall(interpreter, 0xFFFF25A5, 0xFFFF1D79, new TXSemaphoreCreate());
		installHLECall(interpreter, 0xFFFF263D, 0xFFFF1E0D, new TXSemaphoreGet());
		installHLECall(interpreter, 0xFFFF26C5, 0xFFFF1E95, new TXSemaphorePut());
		installHLECall(interpreter, 0xFFFF2719, 0xFFFF1EE9, new TXThreadCreate());
		installHLECall(interpreter, 0xFFFF2945, 0xFFFF2111, new TXTimerCreate());
		installHLECall(interpreter, 0xFFFF4C69, 0xFFFF4409, new TXJumpCall(REG_R0));
		installHLECall(interpreter, 0xFFFF4C6B, 0xFFFF440B, new TXJumpCall(REG_R1));
		installHLECall(interpreter, 0xFFFF4C6D, 0xFFFF440D, new TXJumpCall(REG_R2));
		installHLECall(interpreter, 0xFFFF4C6F, 0xFFFF440F, new TXJumpCall(REG_R3));
		installHLECall(interpreter, 0xFFFF4C79, 0xFFFF4419, new TXExecutionISRExit());
		installHLECall(interpreter, 0xFFFF0000, 0xFFFF4599, new HLENullCall(0));
		installHLECall(interpreter, 0xFFFF4F5D, 0xFFFF46FD, new TXInterruptControl());
	}

	private void installHLECall(ARMInterpreter interpreter, int address1, int address2, IARMHLECall hleCall) {
		installHLECall(interpreter, address1, address2, hleCall, hleCall);
	}

	private void installHLECall(ARMInterpreter interpreter, int address1, int address2, IARMHLECall hleCall1, IARMHLECall hleCall2) {
		hleCallIndex++;

		if (Model.getGeneration() >= 2) {
			// Required for PSP generation 2 or later
			interpreter.registerHLECall(address2, hleCallIndex, hleCall2);
		} else {
			// Required for PSP generation 1
			interpreter.registerHLECall(address1, hleCallIndex, hleCall1);
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

	public void threadSchedule(ARMProcessor processor) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Entering threadSchedule"));
		}

		long previousSystemTickNanos = System.nanoTime();
		while (!Emulator.pause) {
			long nowNanos = System.nanoTime();
			int elapseNanos = (int) (nowNanos - previousSystemTickNanos);
			if (elapseNanos >= TICK_NANOS) {
				incrementSystemTick(processor);
				previousSystemTickNanos += TICK_NANOS;
			}

			if (nextThread == null) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("No thread to be executed"));
				}
				Utilities.sleep(1);
// Execute once for testing of IRQ exception
if (systemTick == 0) {
	processor.mem.getHandlerWlanFirmware().test();
	triggerIrqException(processor);
	incrementSystemTick(processor);

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
}
			} else if (nextThread != currentThread) {
				threadContextSave(processor, currentThread);
				threadContextRestore(processor, nextThread);
				currentThread = nextThread;
			}

			if (currentThread != null) {
				threadRun(processor, currentThread);
			}
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

		txThreads.put(threadPtr, txThread);

		if (autoStart != 0) {
			if (threadResume(processor, txThread)) {
				threadSystemReturn(processor);
			}
		}

		return TX_SUCCESS;
	}

	public boolean threadResume(ARMProcessor processor, TXThread txThread) {
		if (txThread.state != TX_THREAD_STATE_READY) {
			txThread.state = TX_THREAD_STATE_READY;
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

	private void threadSystemReturn(ARMProcessor processor) {
		log.error(String.format("threadSystemReturn unimplemented currentThread=%s", currentThread));
	}

	private void threadRun(ARMProcessor processor, TXThread txThread) {
		processor.interpreter.run();
	}

	private void threadContextSave(ARMProcessor processor, TXThread txThread) {
		if (txThread == null) {
			return;
		}

		for (int i = 0; i < ARMProcessor.NUMBER_REGISTERS; i++) {
			txThread.savedRegisters[i] = processor.getRegister(i);
		}
		txThread.savedCpsr = processor.getCpsr();
	}

	private void threadContextRestore(ARMProcessor processor, TXThread txThread) {
		if (txThread == null) {
			return;
		}

		for (int i = 0; i < ARMProcessor.NUMBER_REGISTERS; i++) {
			processor.setRegister(i, txThread.savedRegisters[i]);
		}
		processor.setCpsr(txThread.savedCpsr);
	}

	private void incrementSystemTick(ARMProcessor processor) {
		systemTick++;

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

	public void triggerIrqException(ARMProcessor processor) {
		threadContextSave(processor, currentThread);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Starting CodeBlock 0x%08X for IRQ Handler", clearBit(txIrqHandler, 0)));
		}

		processor.setLr(PC_END_RUN);
		processor.jumpWithMode(txIrqHandler);
		processor.interpreter.run();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Returning from CodeBlock 0x%08X for IRQ Handler", clearBit(txIrqHandler, 0)));
		}

		threadContextRestore(processor, currentThread);
	}

	public int eventFlagsCreate(ARMProcessor processor, int groupPtr, String groupName) {
		clear(processor, groupPtr, TXEventFlagsGroup.SIZEOF);

		TXEventFlagsGroup txEventFlagsGroup = new TXEventFlagsGroup();
		txEventFlagsGroup.groupPtr = groupPtr;
		txEventFlagsGroup.groupName = groupName;

		txEventFlags.put(groupPtr, txEventFlagsGroup);

		return TX_SUCCESS;
	}

	public int eventFlagsSet(ARMProcessor processor, int groupPtr, int flagsToSet, int setOption) {
		TXEventFlagsGroup txEventFlagsGroup = txEventFlags.get(groupPtr);
		if (txEventFlagsGroup == null) {
			log.error(String.format("eventFlagsSet unknown groupPtr=0x%08X", groupPtr));
			return TX_PTR_ERROR;
		}

		if (hasFlag(setOption, 0x2)) {
			txEventFlagsGroup.current &= flagsToSet;
		} else {
			txEventFlagsGroup.current |= flagsToSet;
		}

		return TX_SUCCESS;
	}

	public int queueCreate(ARMProcessor processor, int queuePtr, String queueName, int messageSize, int queueStart, int queueSize) {
		clear(processor, queuePtr, TXQueue.SIZEOF);

		TXQueue txQueue = new TXQueue();
		txQueue.queuePtr = queuePtr;
		txQueue.queueName = queueName;
		txQueue.messageSize = messageSize;
		txQueue.queueStart = queueStart;
		txQueue.capacity = queueSize;

		txQueues.put(queuePtr, txQueue);

		return TX_SUCCESS;
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

	public int semaphoreGet(ARMProcessor processor, int semaphorePtr, int waitOption) {
		TXSemaphore txSemaphore = txSemaphores.get(semaphorePtr);
		if (txSemaphore == null) {
			log.error(String.format("semaphoreGet unknown semaphorePtr=0x%08X", semaphorePtr));
			return TX_PTR_ERROR;
		}

		if (txSemaphore.count <= 0) {
			log.error(String.format("semaphoreGet unimplemented wait for %s", txSemaphore));
			return TX_SEMAPHORE_ERROR;
		}

		txSemaphore.count--;

		return TX_SUCCESS;
	}

	public int semaphorePut(ARMProcessor processor, int semaphorePtr) {
		TXSemaphore txSemaphore = txSemaphores.get(semaphorePtr);
		if (txSemaphore == null) {
			log.error(String.format("semaphorePut unknown semaphorePtr=0x%08X", semaphorePtr));
			return TX_PTR_ERROR;
		}

		txSemaphore.count++;

		return TX_SUCCESS;
	}
}
