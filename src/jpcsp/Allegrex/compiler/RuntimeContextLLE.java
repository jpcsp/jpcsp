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

import static jpcsp.Emulator.getProcessor;

import org.apache.log4j.Logger;

import jpcsp.AllegrexOpcodes;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.kernel.managers.ExceptionManager;
import jpcsp.HLE.kernel.managers.IntrManager;
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
	public static int pendingInterruptIPbits;

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

		MMIOHandlerInterruptMan.getInstance().triggerInterrupt(interruptNumber);
	}

	public static void clearInterrupt(Processor processor, int interruptNumber) {
		if (!isLLEActive()) {
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("clearInterrupt 0x%X(%s)", interruptNumber, IntrManager.getInterruptName(interruptNumber)));
		}

		MMIOHandlerInterruptMan.getInstance().clearInterrupt(interruptNumber);
	}

	public static void triggerInterruptException(Processor processor, int IPbits) {
		if (!isLLEActive()) {
			return;
		}

		pendingInterruptIPbits |= IPbits;
	}

	public static void triggerSyscallException(Processor processor, int syscallCode) {
		processor.cp0.setSyscallCode(syscallCode << 2);

		// Check if the syscall was executed in a delay slot,
		// i.e. if the previous instruction is "jr $ra".
		int returnAddress;
		int cause = processor.cp0.getCause();

		if (isInstructionInDelaySlot(processor.cpu.pc)) {
			cause |= 0x80000000; // Set BD flag (Branch Delay Slot)

			// The syscall will return to the address contained in the $ra register
			returnAddress = processor.cpu._ra;
		} else {
			cause &= ~0x80000000; // Clear BD flag (Branch Delay Slot)

			// The syscall will return to the address following the syscall instruction
			returnAddress = processor.cpu.pc + 4;
		}

		processor.cp0.setCause(cause);

		triggerException(processor, ExceptionManager.EXCEP_SYS, returnAddress);
	}

	private static void setExceptionCause(Processor processor, int exceptionNumber) {
		int cause = processor.cp0.getCause();
		cause = (cause & 0xFFFFFF00) | (exceptionNumber << 2);
		processor.cp0.setCause(cause);
	}

	public static void triggerException(Processor processor, int exceptionNumber, int returnAddress) {
		if (!isLLEActive()) {
			return;
		}

		setExceptionCause(processor, exceptionNumber);

		int ebase = prepareExceptionHandlerCall(processor);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Calling exception handler for %s at 0x%08X, epc=0x%08X", MMIOHandlerInterruptMan.getInstance().toStringInterruptTriggered(), ebase, processor.cp0.getEpc()));
		}

		// Jump to the EBase address
		int address = ebase;
		while (true) {
			try {
				RuntimeContext.jump(address, returnAddress);
				break;
			} catch (StackPopException e) {
				address = e.getRa();
			} catch (Exception e) {
				log.error("Error while calling code at EBase", e);
				break;
			}
		}
	}

	public static void clearInterruptException(Processor processor, int IPbits) {
		if (!isLLEActive()) {
			return;
		}

		pendingInterruptIPbits &= ~IPbits;

		int cause = processor.cp0.getCause();
		cause &= ~(IPbits << 8);
		processor.cp0.setCause(cause);
	}

	private static boolean isInterruptExceptionAllowed(Processor processor, int IPbits) {
		if (IPbits == 0) {
			return false;
		}

		if (Interrupts.isInterruptsDisabled()) {
			return false;
		}

		int status = processor.cp0.getStatus();
		if (log.isDebugEnabled()) {
			log.debug(String.format("cp0 Status=0x%X", status));
		}

		// Is the processor already in an exception state?
		if ((status & 0x2) != 0) {
			return false;
		}

		// Is the interrupt masked?
		if ((status & 0x1) == 0 || ((IPbits << 8) & status) == 0) {
			return false;
		}

		return true;
	}

	private static boolean isInstructionInDelaySlot(int address) {
		Memory mem = Memory.getInstance();
		int previousInstruction = mem.read32(address - 4);
		switch ((previousInstruction >> 26) & 0x3F) {
			case AllegrexOpcodes.J:
			case AllegrexOpcodes.JAL:
			case AllegrexOpcodes.BEQ:
			case AllegrexOpcodes.BNE:
			case AllegrexOpcodes.BLEZ:
			case AllegrexOpcodes.BGTZ:
			case AllegrexOpcodes.BEQL:
			case AllegrexOpcodes.BNEL:
			case AllegrexOpcodes.BLEZL:
			case AllegrexOpcodes.BGTZL:
				return true;
			case AllegrexOpcodes.SPECIAL:
				switch (previousInstruction & 0x3F) {
					case AllegrexOpcodes.JR:
					case AllegrexOpcodes.JALR:
						return true;
				}
				break;
			case AllegrexOpcodes.REGIMM:
				switch ((previousInstruction >> 16) & 0x1F) {
					case AllegrexOpcodes.BLTZ:
					case AllegrexOpcodes.BGEZ:
					case AllegrexOpcodes.BLTZL:
					case AllegrexOpcodes.BGEZL:
					case AllegrexOpcodes.BLTZAL:
					case AllegrexOpcodes.BGEZAL:
					case AllegrexOpcodes.BLTZALL:
					case AllegrexOpcodes.BGEZALL:
						return true;
				}
				break;
			case AllegrexOpcodes.COP1:
				switch ((previousInstruction >> 21) & 0x1F) {
					case AllegrexOpcodes.COP1BC:
						switch ((previousInstruction >> 16) & 0x1F) {
							case AllegrexOpcodes.BC1F:
							case AllegrexOpcodes.BC1T:
							case AllegrexOpcodes.BC1FL:
							case AllegrexOpcodes.BC1TL:
								return true;
						}
						break;
				}
				break;
		}

		return false;
	}

	private static int prepareExceptionHandlerCall(Processor processor) {
		int epc = processor.cpu.pc;

		int cause = processor.cp0.getCause();
		if (isInstructionInDelaySlot(epc)) {
			cause |= 0x80000000; // Set BD flag (Branch Delay Slot)
			epc -= 4; // The EPC is set to the instruction having the delay slot
		} else {
			cause &= ~0x80000000; // Clear BD flag (Branch Delay Slot)
		}
		processor.cp0.setCause(cause);

		// Set the EPC
		processor.cp0.setEpc(epc);

		int ebase = processor.cp0.getEbase();

		// Set the EXL bit
		int status = processor.cp0.getStatus();
		status |= 0x2; // Set EXL bit
		processor.cp0.setStatus(status);

		return ebase;
	}

	public static int checkPendingInterruptException(int returnAddress) {
		Processor processor = getProcessor();
		if (isInterruptExceptionAllowed(processor, pendingInterruptIPbits)) {
			int cause = processor.cp0.getCause();
			cause |= (pendingInterruptIPbits << 8);
			pendingInterruptIPbits = 0;
			processor.cp0.setCause(cause);

			setExceptionCause(processor, ExceptionManager.EXCEP_INT);
			int ebase = prepareExceptionHandlerCall(processor);

			if (log.isDebugEnabled()) {
				log.debug(String.format("Calling exception handler for %s at 0x%08X, epc=0x%08X", MMIOHandlerInterruptMan.getInstance().toStringInterruptTriggered(), ebase, processor.cp0.getEpc()));
			}

			return ebase;
		}

		return returnAddress;
	}
}
