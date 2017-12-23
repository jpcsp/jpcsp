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
package jpcsp.mediaengine;

import static jpcsp.HLE.kernel.managers.ExceptionManager.EXCEP_INT;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import jpcsp.AllegrexOpcodes;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.memory.mmio.MMIOHandlerInterruptMan;
import jpcsp.util.Utilities;

/**
 * The PSP Media Engine is very close to the PSP main CPU. It has the same instructions
 * with the addition of 3 new ones. It has a FPU but no VFPU.
 *
 * The ME specific instructions are:
 * - mtvme rt, imm16
 *      opcode: 0xB0E00000 | (rt << 16) | imm16
 *      stores the content of the CPU register rt to an unknown VME register imm16
 * - mfvme rt, imm16
 *      opcode: 0x68E00000 | (rt << 16) | imm16
 *      loads the content of an unknown VME register imm16 to the CPU register rt
 * - dbreak
 *      opcode: 0x7000003F
 *      debugging break causing a trap to the address 0xBFC01000 (?)
 *
 * @author gid15
 *
 */
public class MEProcessor extends Processor {
	public static Logger log = Logger.getLogger("me");
	public static final int CPUID_ME = 1;
	private static MEProcessor instance;
	private MEMemory meMemory;
	private final int[] vmeRegisters = new int[0x590]; // Highest VME register number seen is 0x058F
	private boolean halt;
	private int pendingInterruptIPbits;
	private Instruction instructions[];
	private static final int optimizedRunStart = MemoryMap.START_RAM;
	private static final int optimizedRunEnd = MemoryMap.START_RAM + 0x3000;

	public static MEProcessor getInstance() {
		if (instance == null) {
			instance = new MEProcessor();
		}
		return instance;
	}

	private MEProcessor() {
		setLogger(log);
		meMemory = new MEMemory(RuntimeContextLLE.getMMIO(), log);
		cpu.setMemory(meMemory);

		// CPUID is 1 for the ME
		cp0.setCpuid(CPUID_ME);

		halt();
	}

	public MEMemory getMEMemory() {
		return meMemory;
	}

	public int getVmeRegister(int reg) {
		return vmeRegisters[reg];
	}

	public void setVmeRegister(int reg, int value) {
		vmeRegisters[reg] = value;
	}

	public void triggerException(int IP) {
		pendingInterruptIPbits |= IP;

		if (pendingInterruptIPbits != 0) {
			halt = false;
			METhread.getInstance().sync();
		}
	}

	public void triggerReset() {
		METhread meThread = METhread.getInstance();
		meThread.setProcessor(this);

		int status = 0;
		// BEV = 1 during bootstrapping
		status |= 0x00400000;
		// Set the initial status
		cp0.setStatus(status);
		// All interrupts disabled
		disableInterrupts();

		cpu.pc = 0xBFC00000;

		halt = false;

		meThread.sync();
	}

	public void halt() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MEProcessor.halt: pendingInterruptIPbits=0x%X, isInterruptExecutionAllowed=%b, status=0x%X, pc=0x%08X", pendingInterruptIPbits, isInterruptExecutionAllowed(), cp0.getStatus(), cpu.pc));
		}

		if (pendingInterruptIPbits == 0 && !MMIOHandlerInterruptMan.getInstance(this).doTriggerException()) {
			halt = true;
		}
	}

	public boolean isHalted() {
		return halt;
	}

	private boolean isInterruptExecutionAllowed() {
		if (pendingInterruptIPbits == 0) {
			return false;
		}

		if (isInterruptsDisabled()) {
			return false;
		}

		int status = cp0.getStatus();

		// Is the processor already in an exception state?
		if ((status & 0x2) != 0) {
			return false;
		}

		// Is the interrupt masked?
//		if ((status & 0x1) == 0 || ((pendingInterruptIPbits << 8) & status) == 0) {
		if (((pendingInterruptIPbits << 8) & status) == 0) {
			return false;
		}

		return true;
	}

	private void setExceptionCause(int exceptionNumber) {
		int cause = cp0.getCause();
		cause = (cause & 0xFFFFFF00) | (exceptionNumber << 2);
		cp0.setCause(cause);
	}

	private int prepareExceptionHandlerCall(boolean forceNoDelaySlot) {
		int epc = cpu.pc;

		int cause = cp0.getCause();
		if (!forceNoDelaySlot && epc != 0 && isInstructionInDelaySlot(epc)) {
			cause |= 0x80000000; // Set BD flag (Branch Delay Slot)
			epc -= 4; // The EPC is set to the instruction having the delay slot
		} else {
			cause &= ~0x80000000; // Clear BD flag (Branch Delay Slot)
		}
		cp0.setCause(cause);

		// Set the EPC
		cp0.setEpc(epc);

		// Set the EXL bit
		int status = cp0.getStatus();
		status |= 0x2; // Set EXL bit
		cp0.setStatus(status);

		int ebase;
		// BEV bit set?
		if ((status & 0x00400000) == 0) {
			ebase = cp0.getEbase();
		} else {
			ebase = 0xBFC00000;
		}

		halt = false;

		return ebase;
	}

	private boolean isInstructionInDelaySlot(int address) {
		int previousInstruction = cpu.memory.read32(address - 4);
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

	private void checkPendingInterruptException() {
		if (isInterruptExecutionAllowed()) {
			int cause = cp0.getCause();
			cause |= (pendingInterruptIPbits << 8);
			pendingInterruptIPbits = 0;
			cp0.setCause(cause);

			setExceptionCause(EXCEP_INT);
			cpu.pc = prepareExceptionHandlerCall(false);

			if (log.isDebugEnabled()) {
				log.debug(String.format("MEProcessor calling exception handler at 0x%08X, IP bits=0x%02X", cpu.pc, (cause >> 8) & 0xFF));
				if (cp0.getEpc() != 0) {
					log.setLevel(Level.TRACE);
				}
			}
		}
	}

	private void initRun() {
		instructions = new Instruction[(optimizedRunEnd - optimizedRunStart) >> 2];
		for (int pc = optimizedRunStart; pc < optimizedRunEnd; pc += 4) {
			int opcode = memory.read32(pc);
			instructions[(pc - optimizedRunStart) >> 2] = Decoder.instruction(opcode);
		}
	}

	private void optimizedRun() {
		int[] memoryInt = RuntimeContext.getMemoryInt();
		
		int count = 0;
		long start = Emulator.getClock().currentTimeMillis();

		while (!halt && !Emulator.pause) {
			if (pendingInterruptIPbits != 0) {
				checkPendingInterruptException();
			}

			int pc = cpu.pc & Memory.addressMask;
			if (pc >= optimizedRunEnd) {
				break;
			}
			int insnIndex = (pc - optimizedRunStart) >> 2;
			int opcode = memoryInt[pc >> 2];
			cpu.pc += 4;

			Instruction insn = instructions[insnIndex];
	        if (log.isTraceEnabled()) {
	        	log.trace(String.format("Interpreting 0x%08X: [0x%08X] - %s", cpu.pc - 4, opcode, insn.disasm(cpu.pc - 4, opcode)));
	        }
			insn.interpret(this, opcode);
			count++;
		}

		long end = Emulator.getClock().currentTimeMillis();
		if (count > 0 && log.isInfoEnabled()) {
			int duration = Math.max((int) (end - start), 1);
			log.info(String.format("MEProcessor %d instructions executed in %d ms: %d instructions per ms", count, duration, (count + duration / 2) / duration));
		}
	}

	private void normalRun() {
		int count = 0;
		long start = Emulator.getClock().currentTimeMillis();
		final boolean hasMemoryInt = RuntimeContext.hasMemoryInt();

		while (!halt && !Emulator.pause) {
			if (pendingInterruptIPbits != 0) {
				checkPendingInterruptException();
			}

			step();
			count++;

			int pc = cpu.pc & Memory.addressMask;
			if (hasMemoryInt && pc >= optimizedRunStart && pc < optimizedRunEnd) {
				break;
			}

			if (cpu.pc == 0x883000E0 && log.isDebugEnabled()) {
				log.debug(String.format("Initial ME memory content from meimg.img:"));
				log.debug(Utilities.getMemoryDump(meMemory, 0x00101000, cpu._v0));
				log.setLevel(Level.TRACE);
			}
		}

		long end = Emulator.getClock().currentTimeMillis();
		if (count > 0 && log.isInfoEnabled()) {
			int duration = Math.max((int) (end - start), 1);
			log.info(String.format("MEProcessor %d instructions executed in %d ms: %d instructions per ms", count, duration, (count + duration / 2) / duration));
		}
	}

	public void run() {
		if (instructions == null) {
			initRun();
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MEProcessor starting run: halt=%b, pendingInterruptIPbits=0x%X, pc=0x%08X", halt, pendingInterruptIPbits, cpu.pc));
		}

		final boolean hasMemoryInt = RuntimeContext.hasMemoryInt();

		while (!halt && !Emulator.pause) {
			int pc = cpu.pc & Memory.addressMask;
			if (hasMemoryInt && pc >= optimizedRunStart && pc < optimizedRunEnd) {
				optimizedRun();
			} else {
				normalRun();
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MEProcessor exiting run: halt=%b, pendingInterruptIPbits=0x%X, isInterruptExecutionAllowed=%b, status=0x%X, pc=0x%08X", halt, pendingInterruptIPbits, isInterruptExecutionAllowed(), cp0.getStatus(), cpu.pc));
		}
	}
}
