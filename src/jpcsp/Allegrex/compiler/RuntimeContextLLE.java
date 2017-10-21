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

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.HLE.kernel.managers.ExceptionManager;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.reboot;
import jpcsp.hardware.Interrupts;
import jpcsp.memory.mmio.MMIO;
import jpcsp.memory.mmio.MMIOHandlerInterruptMan;

/**
 * @author gid15
 *
 */
public class RuntimeContextLLE {
	public static Logger log = RuntimeContext.log;
	private static final boolean isLLEActive = reboot.enableReboot;
	private static Memory mmio;

	private static class CallExceptionHandlerAction implements IAction {
		private Processor processor;

		public CallExceptionHandlerAction(Processor processor) {
			this.processor = processor;
		}

		@Override
		public void execute() {
			callExceptionHandlerNow(processor);
		}
	}

	public static boolean isLLEActive() {
		return isLLEActive;
	}

	public static void start() {
		if (!isLLEActive()) {
			return;
		}

		mmio = new MMIO(Emulator.getMemory());
		mmio.Initialise();
	}

	public static Memory getMMIO() {
		return mmio;
	}

	public static void triggerInterrupt(Processor processor, int interruptNumber) {
		if (!isLLEActive()) {
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("triggerInterrupt 0x%X(%s)", interruptNumber, IntrManager.getInterruptName(interruptNumber)));
		}
		int addr = MMIOHandlerInterruptMan.BASE_ADDRESS;
		int value = mmio.read32(addr);
		value |= 1 << interruptNumber;
		mmio.write32(addr, value);

		triggerException(processor, ExceptionManager.IP2);
	}

	public static void clearInterrupt(Processor processor, int interruptNumber) {
		if (!isLLEActive()) {
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("clearInterrupt 0x%X(%s)", interruptNumber, IntrManager.getInterruptName(interruptNumber)));
		}

		int addr = MMIOHandlerInterruptMan.BASE_ADDRESS;
		int value = mmio.read32(addr);
		value &= ~(1 << interruptNumber);
		mmio.write32(addr, value);

		if (value == 0) {
			clearException(processor, ExceptionManager.IP2);
		}
	}

	public static void triggerException(Processor processor, int exceptionNumber) {
		if (!isLLEActive()) {
			return;
		}

		int cause = processor.cp0.getCause();
		cause = (cause & 0xFFFFFF00) | (ExceptionManager.EXCEP_INT << 2);
		cause |= (exceptionNumber << 8);
		processor.cp0.setCause(cause);

		if (Interrupts.isInterruptsEnabled()) {
			callExceptionHandler(processor);
		}
	}

	public static void clearException(Processor processor, int exceptionNumber) {
		if (!isLLEActive()) {
			return;
		}

		int cause = processor.cp0.getCause();
		cause &= ~(exceptionNumber << 8);
		processor.cp0.setCause(cause);
	}

	public static void onInterruptsEnabled() {
		if (!isLLEActive()) {
			return;
		}

		Processor processor = Emulator.getProcessor();

		// Is the processor already in an exception state?
		int status = processor.cp0.getStatus();
		if (log.isDebugEnabled()) {
			log.debug(String.format("cp0 Status=0x%X", status));
		}
		if ((status & 0x2) == 0) {
			// Is an exception pending?
			int cause = processor.cp0.getCause();
			if (log.isDebugEnabled()) {
				log.debug(String.format("cp0 Cause=0x%X", cause));
			}
			int excCode = cause & 0x000000FF;
			if (excCode == ExceptionManager.EXCEP_INT) {
				int ip = cause & 0x0000FF00;
				// Is a non-masked interrupt pending?
				if (ip != 0 && (status & 0x1) != 0 && (ip & status) != 0) {
					callExceptionHandler(processor);
				}
			} else {
				callExceptionHandler(processor);
			}
		}
	}

	private static void callExceptionHandler(Processor processor) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Adding action to call the exception handler"));
		}
		Emulator.getScheduler().addAction(new CallExceptionHandlerAction(processor));
	}

	private static void callExceptionHandlerNow(Processor processor) {
		if (!isLLEActive()) {
			return;
		}

		int returnAddress = processor.cpu.pc + 4;
		// Address in kernel space?
		if (returnAddress < MemoryMap.START_USERSPACE) {
			returnAddress |= 0x80000000;
		}
		processor.cp0.setEpc(returnAddress);
		int ebase = processor.cp0.getEbase();

		int status = processor.cp0.getStatus();
		status |= 0x2; // Set EXL bit
		processor.cp0.setStatus(status);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Calling exception handler at 0x%08X from 0x%08X", ebase, returnAddress));
		}

		try {
			RuntimeContext.jump(ebase, returnAddress);
		} catch (Exception e) {
			log.error("Error while calling code at EBase", e);
		}
	}
}
