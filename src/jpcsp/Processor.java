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
package jpcsp;

import static jpcsp.Allegrex.BcuState.jumpTarget;
import static jpcsp.Allegrex.Cp0State.STATUS_BEV;
import static jpcsp.Allegrex.Cp0State.STATUS_ERL;
import static jpcsp.Allegrex.Cp0State.STATUS_IE;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.notHasFlag;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;

import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.Cp0State;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Instructions;

import org.apache.log4j.Logger;

public class Processor implements IState {
	private static final int STATE_VERSION = 0;
    public CpuState cpu = new CpuState();
    public Cp0State cp0 = new Cp0State();
    public static final Memory memory = Memory.getInstance();
    protected Logger log = Logger.getLogger("cpu");
    private int opcode;
    private Instruction instruction;
    private int delaySlotOpcode;
    private Instruction delaySlotInstruction;

	public Processor() {
    	setLogger(log);
    	cpu.initialize();
        reset();
    }

    protected void setLogger(Logger log) {
    	this.log = log;
    	cpu.setLogger(log);
    }

    public Logger getLogger() {
    	return log;
    }

    public void setCpu(CpuState cpu) {
    	this.cpu = cpu;
    }

    public void reset() {
        cpu.reset();
        cp0.reset();
    }

    @Override
    public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
		cpu.read(stream);
		cp0.read(stream);
    }

    @Override
    public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	cpu.write(stream);
    	cp0.write(stream);
    }

    public void interpret() {
        opcode = cpu.fetchOpcode();
        instruction = Decoder.instruction(opcode);
        if (log.isTraceEnabled()) {
        	log.trace(String.format("Interpreting 0x%08X: [0x%08X] - %s", cpu.pc - 4, opcode, instruction.disasm(cpu.pc - 4, opcode)));
        }
        instruction.interpret(this, opcode);

    	if (RuntimeContext.debugCodeBlockCalls) {
    		if (instruction == Instructions.JAL && cp0.isMediaEngineCpu()) {
    			RuntimeContext.debugCodeBlockStart(cpu, cpu.pc);
    		} else if (instruction == Instructions.JR && ((opcode >> 21) & 31) == Common._ra && Memory.isAddressGood(cpu._ra)) {
    			int opcodeCaller = cpu.memory.read32(cpu._ra - 8);
    			Instruction insnCaller = Decoder.instruction(opcodeCaller);
    			int codeBlockStart = cpu.pc;
    			if (insnCaller == Instructions.JAL) {
    				codeBlockStart = jumpTarget(cpu.pc, (opcodeCaller) & 0x3FFFFFF);
    			}
				RuntimeContext.debugCodeBlockEnd(cpu, codeBlockStart, cpu._ra);
    		}
    	}
    }

    public void interpretDelayslot() {
        delaySlotOpcode = cpu.nextOpcode();
        delaySlotInstruction = Decoder.instruction(delaySlotOpcode);
        if (log.isTraceEnabled()) {
        	log.trace(String.format("Interpreting 0x%08X: [0x%08X] - %s", cpu.pc - 4, delaySlotOpcode, delaySlotInstruction.disasm(cpu.pc - 4, delaySlotOpcode)));
        }
        delaySlotInstruction.interpret(this, delaySlotOpcode);
        cpu.nextPc();
    }

    public boolean isInterruptsEnabled() {
    	int status = cp0.getStatus();

    	// When the status ERL bit is set, all interrupts are disabled regardless of all other settings
    	if (hasFlag(status, STATUS_ERL)) {
    		return false;
    	}

    	// When the status IE bit is not set, all interrupts are disabled
    	if (notHasFlag(status, STATUS_IE)) {
    		return false;
    	}

    	return true;
    }

	public boolean isInterruptsDisabled() {
		return !isInterruptsEnabled();
	}

	public void setInterruptsEnabled(boolean interruptsEnabled) {
		int status = cp0.getStatus();

		if (interruptsEnabled) {
			if (notHasFlag(status, STATUS_IE)) {
				// Enabling interrupts
				cp0.setStatus(setFlag(status, STATUS_IE));

				// Interrupts have been enabled
				IntrManager.getInstance().onInterruptsEnabled();
			}
		} else {
			if (hasFlag(status, STATUS_IE)) {
				// Disabling interrupts
				cp0.setStatus(clearFlag(status, STATUS_IE));
			}
		}
	}

	public void enableInterrupts() {
		setInterruptsEnabled(true);
	}

	public void disableInterrupts() {
		setInterruptsEnabled(false);
	}

	public void step() {
        interpret();
    }

	public int getOpcode() {
		return opcode;
	}

    public Instruction getInstruction() {
		return instruction;
	}

	public int getDelaySlotOpcode() {
		return delaySlotOpcode;
	}

	public Instruction getDelaySlotInstruction() {
		return delaySlotInstruction;
	}

	public static boolean isDelaySlotInstruction(int instruction) {
		switch ((instruction >> 26) & 0x3F) {
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
				switch (instruction & 0x3F) {
					case AllegrexOpcodes.JR:
					case AllegrexOpcodes.JALR:
						return true;
				}
				break;
			case AllegrexOpcodes.REGIMM:
				switch ((instruction >> 16) & 0x1F) {
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
				switch ((instruction >> 21) & 0x1F) {
					case AllegrexOpcodes.COP1BC:
						switch ((instruction >> 16) & 0x1F) {
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

	public static boolean isInstructionInDelaySlot(Memory memory, int address) {
		int previousInstruction = memory.read32(address - 4);

		return isDelaySlotInstruction(previousInstruction);
	}

	public void triggerReset() {
		int status = 0;
		// BEV = 1 during bootstrapping
		status = setFlag(status, STATUS_BEV);
		// ERL = 1 after reset
		status = setFlag(status, STATUS_ERL);
		// Set the initial status
		cp0.setStatus(status);
		// Set Ebase = 0
		cp0.setEbase(0);

		cpu.pc = 0xBFC00000;
	}

	@Override
	public String toString() {
		return "Main Processor";
	}
}