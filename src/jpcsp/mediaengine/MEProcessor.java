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

import static jpcsp.Allegrex.compiler.RuntimeContextLLE.pendingInterruptIPbitsME;
import static jpcsp.HLE.kernel.managers.IntrManager.EXCEP_INT;
import static jpcsp.mediaengine.MEMemory.SIZE_ME_RAM;
import static jpcsp.util.Utilities.dumpToFile;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.notHasFlag;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.Cp0State;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.memory.mmio.MMIOHandlerInterruptMan;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

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
	private static final boolean DUMP = false;
	private static final int STATE_VERSION = 0;
	public static final int CPUID_ME = 1;
	private static MEProcessor instance;
	private MEMemory meMemory;
	private final int[] vmeRegisters = new int[0x590]; // Highest VME register number seen is 0x058F
	private boolean halt;
	private Instruction optimizedInstructions1[];
	private Instruction optimizedInstructions2[];
	private static final int optimizedRunStart1 = MemoryMap.START_RAM + 0x300000;
	private static final int optimizedRunEnd1   = optimizedRunStart1 + 0x8E194;
	private static final int optimizedRunStart2 = MemoryMap.START_RAM;
	private static final int optimizedRunEnd2   = optimizedRunStart2 + 0x3000;

	private class ExitAction implements IAction {
		@Override
		public void execute() {
			METhread.exit();
			halt = true;
		}
	}

	public static MEProcessor getInstance() {
		if (instance == null) {
			instance = new MEProcessor();
		}
		return instance;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readInts(vmeRegisters);
		halt = stream.readBoolean();
		super.read(stream);

		optimizedInstructions1 = null;
		optimizedInstructions2 = null;
		sync();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInts(vmeRegisters);
		stream.writeBoolean(halt);
		super.write(stream);
	}

	private MEProcessor() {
		setLogger(log);
		meMemory = new MEMemory(RuntimeContextLLE.getMMIO(), log);
		cpu.setMemory(meMemory);

		// CPUID is 1 for the ME
		cp0.setCpuid(CPUID_ME);

		halt = true;

		RuntimeContextLLE.registerExitAction(new ExitAction());
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
		pendingInterruptIPbitsME |= IP;

		if (pendingInterruptIPbitsME != 0) {
			halt = false;
			METhread.getInstance().sync();
		}
	}

	public void sync() {
		METhread meThread = METhread.getInstance();
		meThread.setProcessor(this);
		meThread.sync();
	}

	public void triggerReset() {
		super.triggerReset();

		halt = false;

		// Force a re-read of all the instructions as another me*img.img file could have been loaded
		optimizedInstructions1 = null;
		optimizedInstructions2 = null;

		sync();
	}

	public void halt() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MEProcessor.halt: pendingInterruptIPbitsME=0x%X, isInterruptExecutionAllowed=%b, doTriggerException=%b, status=0x%X, pc=0x%08X", pendingInterruptIPbitsME, isInterruptExecutionAllowed(), MMIOHandlerInterruptMan.getInstance(this).doTriggerException(), cp0.getStatus(), cpu.pc));
		}

		if (pendingInterruptIPbitsME == 0 && !MMIOHandlerInterruptMan.getInstance(this).doTriggerException()) {
			halt = true;
		}
	}

	public boolean isHalted() {
		return halt;
	}

	private boolean isInterruptExecutionAllowed() {
		if (pendingInterruptIPbitsME == 0) {
			return false;
		}

		if (isInterruptsDisabled()) {
			return false;
		}

		int status = cp0.getStatus();

		// Is the processor already in an exception state?
		if (hasFlag(status, Cp0State.STATUS_EXL)) {
			return false;
		}

		// Is the interrupt masked?
		if (((pendingInterruptIPbitsME << 8) & status) == 0) {
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
		if (!forceNoDelaySlot && epc != 0 && isInstructionInDelaySlot(cpu.memory, epc)) {
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
		status = setFlag(status, Cp0State.STATUS_EXL); // Set EXL bit
		cp0.setStatus(status);

		int ebase;
		// BEV bit set?
		if (notHasFlag(status, Cp0State.STATUS_BEV)) {
			ebase = cp0.getEbase();
		} else {
			ebase = 0xBFC00000;
		}

		halt = false;

		return ebase;
	}

	private void checkPendingInterruptException() {
		if (isInterruptExecutionAllowed()) {
			int cause = cp0.getCause();
			cause |= (pendingInterruptIPbitsME << 8);
			pendingInterruptIPbitsME = 0;
			cp0.setCause(cause);

			setExceptionCause(EXCEP_INT);
			cpu.pc = prepareExceptionHandlerCall(false);

			if (log.isDebugEnabled()) {
				log.debug(String.format("MEProcessor calling exception handler at 0x%08X, IP bits=0x%02X", cpu.pc, (cause >> 8) & 0xFF));
			}
		}
	}

	private void initOptimizedRun1() {
		optimizedInstructions1 = new Instruction[(optimizedRunEnd1 - optimizedRunStart1) >> 2];
		for (int pc = optimizedRunStart1; pc < optimizedRunEnd1; pc += 4) {
			int opcode = memory.read32(pc);
			optimizedInstructions1[(pc - optimizedRunStart1) >> 2] = Decoder.instruction(opcode);
		}
	}

	private void initOptimizedRun2() {
		optimizedInstructions2 = new Instruction[(optimizedRunEnd2 - optimizedRunStart2) >> 2];
		for (int pc = optimizedRunStart2; pc < optimizedRunEnd2; pc += 4) {
			int opcode = memory.read32(pc);
			optimizedInstructions2[(pc - optimizedRunStart2) >> 2] = Decoder.instruction(opcode);
		}
	}

	private void optimizedRun1() {
		int[] memoryInt = RuntimeContext.getMemoryInt();

		if (optimizedInstructions1 == null) {
			initOptimizedRun1();
		}

		final boolean isTraceEnabled = log.isTraceEnabled();
		int count = 0;
		long start = Emulator.getClock().currentTimeMillis();
		int startPc = cpu.pc;

		while (!halt && !Emulator.pause) {
			if (pendingInterruptIPbitsME != 0) {
				checkPendingInterruptException();
			}

			int pc = cpu.pc & Memory.addressMask;
			if (pc >= optimizedRunEnd1) {
				break;
			}
			int insnIndex = (pc - optimizedRunStart1) >> 2;
			int opcode = memoryInt[pc >> 2];
			cpu.pc += 4;

			Instruction insn = optimizedInstructions1[insnIndex];
	        if (isTraceEnabled) {
	        	log.trace(String.format("Interpreting 0x%08X: [0x%08X] - %s", cpu.pc - 4, opcode, insn.disasm(cpu.pc - 4, opcode)));
	        }
			insn.interpret(this, opcode);
			count++;
		}

		long end = Emulator.getClock().currentTimeMillis();
		if (count > 0 && log.isDebugEnabled()) {
			int duration = Math.max((int) (end - start), 1);
			log.debug(String.format("MEProcessor.optimizedRun1 %d instructions executed from 0x%08X in %d ms: %d instructions per ms", count, startPc, duration, (count + duration / 2) / duration));
		}
	}

	private void optimizedRun2() {
		int[] memoryInt = RuntimeContext.getMemoryInt();

		if (optimizedInstructions2 == null) {
			initOptimizedRun2();
		}

		final boolean isTraceEnabled = log.isTraceEnabled();
		int count = 0;
		long start = Emulator.getClock().currentTimeMillis();
		int startPc = cpu.pc;

		while (!halt && !Emulator.pause) {
			if (pendingInterruptIPbitsME != 0) {
				checkPendingInterruptException();
			}

			int pc = cpu.pc & Memory.addressMask;
			if (pc >= optimizedRunEnd2) {
				break;
			}
			int insnIndex = (pc - optimizedRunStart2) >> 2;
			int opcode = memoryInt[pc >> 2];
			cpu.pc += 4;

			Instruction insn = optimizedInstructions2[insnIndex];
	        if (isTraceEnabled) {
	        	log.trace(String.format("Interpreting 0x%08X: [0x%08X] - %s", cpu.pc - 4, opcode, insn.disasm(cpu.pc - 4, opcode)));
	        }
			insn.interpret(this, opcode);
			count++;
		}

		long end = Emulator.getClock().currentTimeMillis();
		if (count > 0 && log.isDebugEnabled()) {
			int duration = Math.max((int) (end - start), 1);
			log.debug(String.format("MEProcessor.optimizedRun2 %d instructions executed from 0x%08X in %d ms: %d instructions per ms", count, startPc, duration, (count + duration / 2) / duration));
		}
	}

	private void normalRun() {
		int count = 0;
		long start = Emulator.getClock().currentTimeMillis();
		final boolean hasMemoryInt = RuntimeContext.hasMemoryInt();
		int startPc = cpu.pc;

		while (!halt && !Emulator.pause) {
			if (pendingInterruptIPbitsME != 0) {
				checkPendingInterruptException();
			}

			step();
			count++;

			if (DUMP) {
				if (cpu.pc == 0x883000E0) {
					dumpToFile("MEMemory.dump", new TPointer(meMemory, 0).forceNonNull(), SIZE_ME_RAM);
					dumpToFile("meimg.img", new TPointer(meMemory, optimizedRunStart1), optimizedRunEnd1 - optimizedRunStart1);
				}
			} else {
				if (hasMemoryInt) {
					int pc = cpu.pc & Memory.addressMask;
					if (pc >= optimizedRunStart1 && pc < optimizedRunEnd1) {
						break;
					}
					if (pc >= optimizedRunStart2 && pc < optimizedRunEnd2) {
						break;
					}
				}
			}
		}

		long end = Emulator.getClock().currentTimeMillis();
		if (count > 0 && log.isDebugEnabled()) {
			int duration = Math.max((int) (end - start), 1);
			log.debug(String.format("MEProcessor.normalRun %d instructions executed from 0x%08X in %d ms: %d instructions per ms", count, startPc, duration, (count + duration / 2) / duration));
		}
	}

	public void run() {
		if (!Emulator.run) {
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MEProcessor starting run: halt=%b, pendingInterruptIPbitsME=0x%X, pc=0x%08X", halt, pendingInterruptIPbitsME, cpu.pc));
		}

		final boolean hasMemoryInt = RuntimeContext.hasMemoryInt();

		while (!halt && !Emulator.pause) {
			int pc = cpu.pc & Memory.addressMask;
			if (hasMemoryInt && pc >= optimizedRunStart1 && pc < optimizedRunEnd1) {
				optimizedRun1();
			} else if (hasMemoryInt && pc >= optimizedRunStart2 && pc < optimizedRunEnd2) {
				optimizedRun2();
			} else {
				normalRun();
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MEProcessor exiting run: halt=%b, pendingInterruptIPbitsME=0x%X, isInterruptExecutionAllowed=%b, status=0x%X, pc=0x%08X", halt, pendingInterruptIPbitsME, isInterruptExecutionAllowed(), cp0.getStatus(), cpu.pc));
		}
	}
}
