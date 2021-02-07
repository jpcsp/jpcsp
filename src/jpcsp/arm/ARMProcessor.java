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
package jpcsp.arm;

import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.getFlagFromBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.setFlag;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class ARMProcessor {
	public static Logger log = Logger.getLogger("arm");
	// Processor modes
	public static final int MODE_USER = 0;
	public static final int MODE_FIQ = 1;
	public static final int MODE_IRQ = 2;
	public static final int MODE_SUPERVISOR = 3;
	public static final int MODE_ABORT = 4;
	public static final int MODE_UNDEFINED = 5;
	public static final int MODE_SYSTEM = 6;
	// Coprocessors
	public static final int COPROCESSOR_SYSTEM_CONTROL = 15;
	// Processor registers
	public static final int REG_R0 = 0;
	public static final int REG_R1 = 1;
	public static final int REG_R2 = 2;
	public static final int REG_R3 = 3;
	public static final int REG_R4 = 4;
	public static final int REG_R5 = 5;
	public static final int REG_R6 = 6;
	public static final int REG_R7 = 7;
	public static final int REG_R8 = 8;
	public static final int REG_R9 = 9;
	public static final int REG_R10 = 10;
	public static final int REG_R11 = 11;
	public static final int REG_R12 = 12;
	public static final int REG_SP = 13;
	public static final int REG_LR = 14;
	public static final int REG_PC = 15;
	public static final int NUMBER_REGISTERS = 16;
	public static final int COND_EQ = 0x0;
	public static final int COND_NE = 0x1;
	public static final int COND_CS = 0x2;
	public static final int COND_CC = 0x3;
	public static final int COND_MI = 0x4;
	public static final int COND_PL = 0x5;
	public static final int COND_VS = 0x6;
	public static final int COND_VC = 0x7;
	public static final int COND_HI = 0x8;
	public static final int COND_LS = 0x9;
	public static final int COND_GE = 0xA;
	public static final int COND_LT = 0xB;
	public static final int COND_GT = 0xC;
	public static final int COND_LE = 0xD;
	public static final int COND_AL = 0xE;
	public static final int CPSR_BIT_N = 31;
	public static final int CPSR_BIT_Z = 30;
	public static final int CPSR_BIT_C = 29;
	public static final int CPSR_BIT_V = 28;
	public static final int CPSR_BIT_I = 7;
	public static final int CPSR_BIT_F = 6;
	public static final int CPSR_BIT_T = 5;
	private static final int CPSR_NZ_FLAGS = getFlagFromBit(CPSR_BIT_N) | getFlagFromBit(CPSR_BIT_Z);
	private static final int CPSR_NZC_FLAGS = CPSR_NZ_FLAGS | getFlagFromBit(CPSR_BIT_C);
	private static final int CPSR_NZCV_FLAGS = CPSR_NZC_FLAGS | getFlagFromBit(CPSR_BIT_V);
	private int processorMode;
	private final int[] gpr = new int[13];
	private final int[] gpr_fiq = new int[5];
	private final int[] gpr_sp = new int[6];
	private static final int[] gpr_sp_mode_mapping = { 0, 1, 2, 3, 4, 5, 0 };
	private final int[] gpr_lr = new int[6];
	private static final int[] gpr_lr_mode_mapping = { 0, 1, 2, 3, 4, 5, 0 };
	private final int[] spsr = new int[5];
	private static final int[] spsr_mode_mapping = { -1, 0, 1, 2, 3, 4, -1 };
	private static final int[] cpsr_mode_bits = { 0x10, 0x11, 0x12, 0x13, 0x17, 0x1B, 0x1F };
	private int pc;
	private int cpsr;
	private boolean thumbMode;
	public ARMMemory mem;
	public ARMInterpreter interpreter;
	private int currentInstructionPc;
	public boolean shifterCarryOut;
	private boolean highVectors = false;

	public ARMProcessor(ARMMemory mem) {
		this.mem = mem;
		mem.setProcessor(this);
		setProcessorMode(MODE_USER);
	}

	public void setInterpreter(ARMInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	public void interpret() {
		if (thumbMode) {
			interpretThumb();
		} else {
			interpretARM();
		}
	}

	private void interpretThumb() {
		currentInstructionPc = pc;
		int insn = mem.internalRead16(pc);
		pc += 2;
		ARMInstruction instruction = ARMDecoder.thumbInstruction(insn);
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X: [0x%04X] - %s", currentInstructionPc, insn, instruction.disasm(currentInstructionPc, insn)));
		}
		instruction.interpret(this, insn);
	}

	private void interpretARM() {
		currentInstructionPc = pc;
		int insn = mem.internalRead32(pc);
		pc += 4;
		ARMInstruction instruction = ARMDecoder.instruction(insn);
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X: [0x%08X] - %s", currentInstructionPc, insn, instruction.disasm(currentInstructionPc, insn)));
		}
		instruction.interpret(this, insn);
	}

	public void setThumbMode() {
		setCpsrBit(CPSR_BIT_T);
		thumbMode = true;
	}

	public void setARMMode() {
		clearCpsrBit(CPSR_BIT_T);
		thumbMode = false;
	}

	public int getProcessorMode() {
		return processorMode;
	}

	public void setProcessorMode(int processorMode) {
		this.processorMode = processorMode;
		cpsr = clearFlag(cpsr, 0x1F);
		cpsr = setFlag(cpsr, cpsr_mode_bits[processorMode]);
	}

	public boolean inAPrivilegeMode() {
		return processorMode != MODE_USER;
	}

	public int getSpsr() {
		return spsr[spsr_mode_mapping[processorMode]];
	}

	public void setSpsr(int value) {
		setSpsr(value, processorMode);
	}

	public boolean currentModeHasSpsr() {
		return spsr_mode_mapping[processorMode] >= 0;
	}

	private void setSpsr(int value, int processorMode) {
		spsr[spsr_mode_mapping[processorMode]] = value;
	}

	private void clearCpsrBit(int bit) {
		cpsr = clearBit(cpsr, bit);
	}

	private void setCpsrBit(int bit) {
		cpsr = setBit(cpsr, bit);
	}

	private boolean hasCpsrBit(int bit) {
		return hasBit(cpsr, bit);
	}

	public boolean isInterruptEnabled() {
		return !hasCpsrBit(CPSR_BIT_I);
	}

	public void setInterruptEnabled() {
		clearCpsrBit(CPSR_BIT_I);
	}

	public void setInterruptDisabled() {
		setCpsrBit(CPSR_BIT_I);
	}

	public void resetException() {
		setProcessorMode(MODE_SUPERVISOR);
		setSpsr(0); // UNPREDICTABLE value
		setLr(0); // UNPREDICTABLE value
		clearCpsrBit(CPSR_BIT_T); // Execute in ARM state
		thumbMode = false;
		setCpsrBit(CPSR_BIT_F); // Disable fast interrupts
		setCpsrBit(CPSR_BIT_I); // Disable normal interrupts
		if (highVectors) {
			setPc(0xFFFF0000);
		} else {
			setPc(0x00000000);
		}
	}

	public void undefinedInstructionException() {
		setProcessorMode(MODE_UNDEFINED);
		setLr(pc); // Address of next instruction after the undefined instruction
		setSpsr(cpsr);
		clearCpsrBit(CPSR_BIT_T); // Execute in ARM state
		thumbMode = false;
		// CPSR_BIT_F is unchanged
		setCpsrBit(CPSR_BIT_I); // Disable normal interrupts
		if (highVectors) {
			setPc(0xFFFF0004);
		} else {
			setPc(0x00000004);
		}
	}

	public void softwareInterruptException() {
		setProcessorMode(MODE_SUPERVISOR);
		setLr(pc); // Address of next instruction after the SWI instruction
		setSpsr(cpsr);
		clearCpsrBit(CPSR_BIT_T); // Execute in ARM state
		thumbMode = false;
		// CPSR_BIT_F is unchanged
		setCpsrBit(CPSR_BIT_I); // Disable normal interrupts
		if (highVectors) {
			setPc(0xFFFF0008);
		} else {
			setPc(0x00000008);
		}
	}

	public void prefetchAbortException() {
		setProcessorMode(MODE_ABORT);
		setLr(pc); // Address of the aborted instruction + 4
		setSpsr(cpsr);
		clearCpsrBit(CPSR_BIT_T); // Execute in ARM state
		thumbMode = false;
		// CPSR_BIT_F is unchanged
		setCpsrBit(CPSR_BIT_I); // Disable normal interrupts
		if (highVectors) {
			setPc(0xFFFF000C);
		} else {
			setPc(0x0000000C);
		}
	}

	public void interruptRequestException() {
		setProcessorMode(MODE_IRQ);
		setLr(pc + 4); // Address of next instruction to be executed + 4
		setSpsr(cpsr);
		clearCpsrBit(CPSR_BIT_T); // Execute in ARM state
		thumbMode = false;
		// CPSR_BIT_F is unchanged
		setCpsrBit(CPSR_BIT_I); // Disable normal interrupts
		if (highVectors) {
			setPc(0xFFFF0018);
		} else {
			setPc(0x00000018);
		}
	}

	public void fastInterruptRequestException() {
		setProcessorMode(MODE_FIQ);
		setLr(pc + 4); // Address of next instruction to be executed + 4
		setSpsr(cpsr);
		clearCpsrBit(CPSR_BIT_T); // Execute in ARM state
		thumbMode = false;
		setCpsrBit(CPSR_BIT_F); // Disable fast interrupts
		setCpsrBit(CPSR_BIT_I); // Disable normal interrupts
		if (highVectors) {
			setPc(0xFFFF001C);
		} else {
			setPc(0x0000001C);
		}
	}

	public int getRegister(int n) {
		if (n < gpr.length) {
			return gpr[n];
		}

		switch (n) {
			case REG_SP: return getSp();
			case REG_LR: return getLr();
			case REG_PC: return getPc();
		}

		log.error(String.format("Unknown register 0x%X", n));

		return 0;
	}

	public int getSp() {
		return gpr_sp[gpr_sp_mode_mapping[processorMode]];
	}

	public int getLr() {
		return gpr_lr[gpr_lr_mode_mapping[processorMode]];
	}

	public void setRegister(int n, int value) {
		if (n < 8) {
			gpr[n] = value;
		} else if (n < 13) {
			if (processorMode == MODE_FIQ) {
				gpr_fiq[n - 8] = value;
			} else {
				gpr[n] = value;
			}
		} else {
			switch (n) {
				case REG_SP: setSp(value); break;
				case REG_LR: setLr(value); break;
				case REG_PC: setPc(value); break;
				default:
					log.error(String.format("Unknown register 0x%X", n));
					break;
			}
		}
	}

	public void setSp(int value) {
		gpr_sp[gpr_sp_mode_mapping[processorMode]] = value;
	}

	public void setLr(int value) {
		gpr_lr[gpr_lr_mode_mapping[processorMode]] = value;
	}

	public int getPc() {
		if (thumbMode) {
			return pc + 2;
		}
		return pc + 4;
	}

	private void setPc(int value) {
		pc = value;
	}

	public int getCurrentInstructionPc() {
		return currentInstructionPc;
	}

	public int getNextInstructionPc() {
		return pc;
	}

	public boolean isNextInstructionPc(int addr) {
		return pc == addr;
	}

	public void link() {
		setLr(pc);
	}

	public void linkWithThumb() {
		setLr(pc | 0x1);
	}

	public void branch(int offset) {
		setPc(getPc() + offset);
	}

	public void branchWithLink(int offset) {
		link();
		branch(offset);
	}

	public void jumpWithMode(int addr) {
		if (hasBit(addr, 0)) {
			if (!thumbMode) {
				setThumbMode();
			}
			addr = clearBit(addr, 0);
		} else {
			if (thumbMode) {
				setARMMode();
			}
		}

		jump(addr);
	}

	public void jump(int addr) {
		setPc(addr);
	}

	public boolean isCondition(int insn) {
		return isConditionCode(insn >>> 28);
	}

	public boolean isConditionCode(int code) {
		switch (code) {
			case COND_EQ: return hasBit(cpsr, CPSR_BIT_Z);
			case COND_NE: return !hasBit(cpsr, CPSR_BIT_Z);
			case COND_CS: return hasBit(cpsr, CPSR_BIT_C);
			case COND_CC: return !hasBit(cpsr, CPSR_BIT_C);
			case COND_MI: return hasBit(cpsr, CPSR_BIT_N);
			case COND_PL: return !hasBit(cpsr, CPSR_BIT_N);
			case COND_VS: return hasBit(cpsr, CPSR_BIT_V);
			case COND_VC: return !hasBit(cpsr, CPSR_BIT_V);
			case COND_HI: return hasBit(cpsr, CPSR_BIT_C) && !hasBit(cpsr, CPSR_BIT_Z);
			case COND_LS: return !hasBit(cpsr, CPSR_BIT_C) || hasBit(cpsr, CPSR_BIT_Z);
			case COND_GE: return hasBit(cpsr, CPSR_BIT_N) == hasBit(cpsr, CPSR_BIT_V);
			case COND_LT: return hasBit(cpsr, CPSR_BIT_N) != hasBit(cpsr, CPSR_BIT_V);
			case COND_GT: return !hasBit(cpsr, CPSR_BIT_Z) && (hasBit(cpsr, CPSR_BIT_N) == hasBit(cpsr, CPSR_BIT_V));
			case COND_LE: return hasBit(cpsr, CPSR_BIT_Z) || (hasBit(cpsr, CPSR_BIT_N) != hasBit(cpsr, CPSR_BIT_V));
			case COND_AL: return true;
		}

		log.error(String.format("Unknown condition code 0x%X", code));

		return false;
	}

	public int getCpsr() {
		return cpsr;
	}

	public void setCpsr(int cpsr) {
		this.cpsr = cpsr;

		switch (cpsr & 0x1F) {
			case 0x10: setProcessorMode(MODE_USER); break;
			case 0x11: setProcessorMode(MODE_FIQ); break;
			case 0x12: setProcessorMode(MODE_IRQ); break;
			case 0x13: setProcessorMode(MODE_SUPERVISOR); break;
			case 0x17: setProcessorMode(MODE_ABORT); break;
			case 0x1B: setProcessorMode(MODE_UNDEFINED); break;
			case 0x1F: setProcessorMode(MODE_SYSTEM); break;
			default:
				log.error(String.format("Unknown CPSR Modes bits 0x%02X", cpsr & 0x1F));
				break;
		}

		if (hasBit(cpsr, CPSR_BIT_T)) {
			setThumbMode();
		} else {
			setARMMode();
		}
	}

	public void setCpsrfromSpsr() {
		setCpsr(spsr[spsr_mode_mapping[processorMode]]);
	}

	public void setCpsrResult(int result) {
		cpsr = clearFlag(cpsr, CPSR_NZ_FLAGS);
		if (result < 0) {
			cpsr = setBit(cpsr, CPSR_BIT_N);
		} else if (result == 0) {
			cpsr = setBit(cpsr, CPSR_BIT_Z);
		}
	}

	public void setCpsrResult(int result, boolean c) {
		cpsr = clearFlag(cpsr, CPSR_NZC_FLAGS);
		if (result < 0) {
			cpsr = setBit(cpsr, CPSR_BIT_N);
		} else if (result == 0) {
			cpsr = setBit(cpsr, CPSR_BIT_Z);
		}
		if (c) {
			cpsr = setBit(cpsr, CPSR_BIT_C);
		}
	}

	public void setCpsrResult(int result, boolean c, boolean v) {
		cpsr = clearFlag(cpsr, CPSR_NZCV_FLAGS);
		if (result < 0) {
			cpsr = setBit(cpsr, CPSR_BIT_N);
		} else if (result == 0) {
			cpsr = setBit(cpsr, CPSR_BIT_Z);
		}
		if (c) {
			cpsr = setBit(cpsr, CPSR_BIT_C);
		}
		if (v) {
			cpsr = setBit(cpsr, CPSR_BIT_V);
		}
	}

	public boolean hasCflag() {
		return hasBit(cpsr, CPSR_BIT_C);
	}

	public void setSystemControlCoprocessorRegister(int n, int value) {
		switch (n) {
			case 1: // Control bits
				if (value == 0x1F74) {
					// bit  0 [0]: MMU or Protection Unit disabled
					// bit  1 [0]: Alignment fault checking disabled
					// bit  2 [1]: Cache enabled
					// bit  3 [0]: Write buffer disabled 
					// bit  4 [1]: Exception handlers entered in 32-bit modes
					// bit  5 [1]: 26-bit address exception checking disabled
					// bit  6 [1]: Late Abort Model selected
					// bit  7 [0]: Configured for little-endian memory system
					// bit  8 [1]: System protection bit
					// bit  9 [1]: ROM protection bit
					// bit 10 [1]: Implementation defined
					// bit 11 [1]: Branch prediction enabled
					// bit 12 [1]: Instruction cache enabled
					// bit 13 [0]: Normal exception vectors selected
					// bit 14 [0]: Normal replacement strategy
					// bit 15 [0]: Normal behaviour for the architecture version 5 or above 
				} else {
					log.error(String.format("setSystemControlCoprocessorRegister unimplemented control bits 0x%X", value));
				}
				highVectors = hasBit(value, 13);
				break;
			default:
				log.error(String.format("setSystemControlCoprocessorRegister unimplemented register=%d, value=0x%X", n, value));
				break;
		}
	}

	public int getSystemControlCoprocessorRegister(int n) {
		int value;
		switch (n) {
			case 0: // Main ID register
				value = 0;
				break;
			default:
				log.error(String.format("getSystemControlCoprocessorRegister unimplemented register=%d", n));
				value = 0;
				break;
		}

		return value;
	}
}
