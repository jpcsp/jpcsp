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

import static java.lang.Integer.rotateRight;
import static jpcsp.Emulator.EMU_STATUS_UNIMPLEMENTED;
import static jpcsp.arm.ARMProcessor.COPROCESSOR_SYSTEM_CONTROL;
import static jpcsp.arm.ARMProcessor.REG_LR;
import static jpcsp.arm.ARMProcessor.REG_PC;
import static jpcsp.arm.ARMProcessor.REG_R0;
import static jpcsp.arm.ARMProcessor.REG_SP;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.notHasBit;
import static jpcsp.util.Utilities.notHasFlag;
import static jpcsp.util.Utilities.setFlag;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.ICompilerContext;
import jpcsp.Allegrex.compiler.RuntimeContext;

/**
 * @author gid15
 *
 */
public class ARMInstructions {
	public static Logger log = ARMInstruction.log;
	private static final String[] conditionNames = {
			"eq", "ne",
			"cs", "cc",
			"mi", "pl",
			"vs", "vc",
			"hi", "ls",
			"ge", "lt",
			"gt", "le",
			"", ""
	};
	private static final String[] registerNames = {
			"r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7",
			"r8", "r9", "r10", "fp", "ip", "sp", "lr", "pc"
	};
	private static final String[] cpRegisterNames = {
			"cr0", "cr1", "cr2", "cr3", "cr4", "cr5", "cr6", "cr7",
			"cr8", "cr9", "cr10", "cr11", "cr12", "cr13", "cr14", "cr15"
	};
	private static final String[] shiftTypeNames = {
			"lsl", "lsr", "asr", "rotr"
	};
	private static final String[] LDM_STM = {
			"da", "ia", "db", "ib"
	};

	private static final String getConditionName(int insn) {
		return conditionNames[insn >>> 28];
	}

	public static final String getRegisterName(int reg) {
		return registerNames[reg & 0xF];
	}

	private static final void appendRegister(StringBuilder s, int reg) {
		if (s.length() > 0) {
			s.append(", ");
		}
		s.append(getRegisterName(reg));
	}

	private static final String getRegisterList(int insn) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < 16; i++) {
			if (hasBit(insn, i)) {
				appendRegister(s, i);
			}
		}
		return s.toString();
	}

	private static final String getRegisterListThumb(int insn, int lrFlag, int pcFlag) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			if (hasBit(insn, i)) {
				appendRegister(s, i);
			}
		}
		if (hasFlag(insn, lrFlag)) {
			appendRegister(s, REG_LR);
		}
		if (hasFlag(insn, pcFlag)) {
			appendRegister(s, REG_PC);
		}
		return s.toString();
	}

	private static final String getCoprocessorRegisterName(int reg) {
		return cpRegisterNames[reg & 0xF];
	}

	private static final String getAddressingMode1(int insn) {
		if (hasFlag(insn, 0x02000000)) {
			// 32-bit immediate
			int imm8 = insn & 0xFF; // Unsigned 8 bit immediate value
			int immShift = (insn >> 8) & 0xF; // Shift applied to imm8
			int value = rotateRight(imm8, immShift << 1);
			return String.format("0x%X", value);
		} else {
			StringBuilder s = new StringBuilder();
			s.append(getRegisterName(insn & 0xF));
			// Do not display any shift operation if shifting by 0
			if ((insn & 0xF90) != 0x000) {
				int shiftType = (insn >> 5) & 0x3;
				s.append(' ');
				s.append(shiftTypeNames[shiftType]);
				s.append(' ');
				if (hasFlag(insn, 0x010)) {
					s.append(getRegisterName(insn >> 8));
				} else {
					int shift = (insn >> 7) & 0x1F; // 5 bit unsigned integer
					if (shift == 0 && shiftType >= 1 && shiftType <= 2) {
						// For the arithmetic and logical right shifts,
						// an immediate shift value of 0 means a shift of 32.
						shift = 32;
					}
					s.append('#');
					s.append(shift);
				}
			}

			return s.toString();
		}
	}

	private static final int getAddressingMode1(ARMProcessor processor, int insn) {
		int value;
		boolean shifterCarryOut;
		if (hasFlag(insn, 0x02000000)) {
			// 32-bit immediate
			int imm8 = insn & 0xFF; // Unsigned 8 bit immediate value
			int immRot = (insn >> 8) & 0xF; // Rotate applied to imm8
			value = rotateRight(imm8, immRot << 1);
			if (immRot == 0) {
				shifterCarryOut = processor.hasCflag();
			} else {
				shifterCarryOut = value < 0;
			}
		} else {
			int shiftType = (insn >> 5) & 0x3;
			int shift;
			if (hasFlag(insn, 0x00000010)) {
				// Register shifts
				int shiftRegister = (insn >> 8) & 0xF;
				shift = processor.getRegister(shiftRegister);

				// Only the lower 8 bits of the register value are considered
				shift &= 0xFF;
			} else {
				// Immediate shifts
				shift = (insn >> 7) & 0x1F; // 5 bit unsigned integer

				if (shift == 0 && shiftType >= 1 && shiftType <= 2) {
					// For the arithmetic and logical right shifts,
					// an immediate shift value of 0 means a shift of 32.
					shift = 32;
				}
			}

			int register = insn & 0xF;
			value = processor.getRegister(register);

			if (shift == 0) {
				shifterCarryOut = processor.hasCflag();
			} else {
				switch (shiftType) {
					case 0:
						shifterCarryOut = (value << (shift -1)) < 0;
						value <<= shift;
						break;
					case 1:
						shifterCarryOut = ((value >>> (shift - 1)) & 0x1) != 0;
						value >>>= shift;
						break;
					case 2:
						shifterCarryOut = ((value >> (shift - 1)) & 0x1) != 0;
						value >>= shift;
						break;
					case 3:
						shifterCarryOut = (rotateRight(value, shift - 1) & 0x1) != 0;
						value = rotateRight(value, shift);
						break;
					default:
						// Cannot happen
						log.error(String.format("Unknown shiftType=%d", shiftType));
						shifterCarryOut = false;
						break;
				}
			}
		}
		processor.shifterCarryOut = shifterCarryOut;

		return value;
	}

	private static final String getAddressingMode2(int insn) {
		StringBuilder s = new StringBuilder();
		s.append('[');
		s.append(getRegisterName(insn >> 16));
		boolean up = hasFlag(insn, 0x00800000);
		boolean pre = hasFlag(insn, 0x01000000);
		if (!pre) {
			s.append(']');
		}

		if (notHasFlag(insn, 0x02000000)) {
			// Immediate offset
			int imm12 = insn & 0xFFF;
			if (imm12 != 0) {
				s.append(", #");
				if (!up) {
					s.append('-');
				}
				s.append(String.format("0x%X", imm12));
			}
		} else {
			s.append(", ");
			if (!up) {
				s.append('-');
			}
			s.append(getRegisterName(insn & 0xF));
			// Do not display any shift operation if shifting by 0
			if ((insn & 0xF90) != 0x000) {
				int shiftType = (insn >> 5) & 0x3;
				int shift = (insn >> 7) & 0x1F; // 5 bit unsigned integer
				if (shift == 0 && shiftType >= 1 && shiftType <= 2) {
					// For the arithmetic and logical right shifts,
					// an immediate shift value of 0 means a shift of 32.
					shift = 32;
				}
				s.append(", ");
				s.append(shiftTypeNames[shiftType]);
				s.append(" #");
				s.append(shift);
			}
		}

		if (pre) {
			s.append(']');
		}

		return s.toString();
	}

	private static final int getAddressingMode2(ARMProcessor processor, int insn) {
		int rn = (insn >> 16) & 0xF;
		int base = processor.getRegister(rn);

		int offset;
		if (notHasFlag(insn, 0x02000000)) {
			// Immediate offset
			offset = insn & 0xFFF; // Unsigned 12 bit immediate offset
		} else {
			int shiftType = (insn >> 5) & 0x3;
			int shift = (insn >> 7) & 0x1F; // 5 bit unsigned integer

			if (shift == 0 && shiftType >= 1 && shiftType <= 2) {
				// For the arithmetic and logical right shifts,
				// an immediate shift value of 0 means a shift of 32.
				shift = 32;
			}

			int register = insn & 0xF;
			offset = processor.getRegister(register);

			switch (shiftType) {
				case 0: offset <<= shift; break;
				case 1: offset >>>= shift; break;
				case 2:	offset >>= shift; break;
				case 3: offset = rotateRight(offset, shift); break;
			}
		}

		int addr;
		// P flag?
		if (hasFlag(insn, 0x01000000)) {
			// U flag?
			if (hasFlag(insn, 0x00800000)) {
				addr = base + offset;
			} else {
				addr = base - offset;
			}

			// W flag?
			if (hasFlag(insn, 0x00200000)) {
				processor.setRegister(rn, addr);
			}
		} else {
			addr = base;

			int writebackAddr;
			// U flag?
			if (hasFlag(insn, 0x00800000)) {
				writebackAddr = base + offset;
			} else {
				writebackAddr = base - offset;
			}
			processor.setRegister(rn, writebackAddr);
		}

		return addr;
	}

	private static final boolean getSubstractC(int value1, int value2, int value) {
		boolean neg = value < 0;
		boolean neg1 = value1 < 0;
		boolean neg2 = value2 < 0;
		boolean borrow = neg1 ? neg2 && neg : neg2 || neg;
		return !borrow;
	}

	private static final boolean getSubstractV(int value1, int value2, int value) {
		return (value1 ^ value2) < 0 && (value1 ^ value) < 0;
	}

	private static final boolean getAdditionC(int value1, int value2, int value) {
		return value < value1;
	}

	private static final boolean getAdditionV(int value1, int value2, int value) {
		return (value1 ^ value2) >= 0 && (value1 ^ value) < 0;
	}

	public static abstract class STUB extends ARMInstruction {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
            instance(insn).interpret(processor, insn);
        }

        @Override
        public void compile(ICompilerContext context, int insn) {
            instance(insn).compile(context, insn);
        }

        @Override
        public String disasm(int address, int insn) {
            return instance(insn).disasm(address, insn);
        }

        @Override
        public abstract ARMInstruction instance(int insn);
    }

    public static final ARMInstruction UNK = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	log.error(String.format("0x%08X %s", processor.getCurrentInstructionPc(), disasm(processor.getCurrentInstructionPc(), insn)));
        	Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("Unknown instruction 0x%08X", insn);
        }
    };

    public static final ARMInstruction UNK_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	log.error(String.format("0x%08X %s", processor.getCurrentInstructionPc(), disasm(processor.getCurrentInstructionPc(), insn)));
        	Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("Unknown Thumb instruction 0x%04X", insn);
        }
    };

    public static final ARMInstruction B = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int offset = insn << 8 >> 6;
        		processor.branch(offset);

        		checkHLECall(processor);

        		if (offset == -8) {
                    log.error("Pausing emulator - branch to self (death loop)");
        			Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_JUMPSELF);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("b%s 0x%08X", getConditionName(insn), address + 8 + (insn << 8 >> 6));
        }
    };

    public static final ARMInstruction BL = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		processor.branchWithLink(insn << 8 >> 6);

        		checkHLECall(processor);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bl%s 0x%08X", getConditionName(insn), address + 8 + (insn << 8 >> 6));
        }
    };

    public static final ARMInstruction BLX_uncond = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
    		processor.branchWithLink((insn << 8 >> 6) + ((insn >> 23) & 0x2));
        	processor.setThumbMode();

        	checkHLECall(processor);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("blx 0x%08X", address + 8 + (insn << 8 >> 6) + ((insn >> 23) & 0x2));
        }
    };

    public static final ARMInstruction BX_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rm = (insn >> 3) & 0xF;
        	int addr = processor.getRegister(rm);
    		processor.jumpWithMode(addr);

    		checkHLECall(processor);

    		if (RuntimeContext.debugCodeBlockCalls && log.isDebugEnabled()) {
    			if (rm == REG_LR) {
    				log.debug(String.format("Returning from CodeBlock to 0x%08X, sp=0x%08X, r0=0x%08X", clearBit(addr, 0), processor.getSp(), processor.getRegister(0)));
    			} else if (rm == 0) {
    				log.debug(String.format("Starting CodeBlock 0x%08X, lr=0x%08X, sp=0x%08X", addr, clearBit(processor.getLr(), 0), processor.getSp()));
    			} else if (rm == 1) {
    				log.debug(String.format("Starting CodeBlock 0x%08X, r0=0x%08X, lr=0x%08X, sp=0x%08X", addr, processor.getRegister(0), clearBit(processor.getLr(), 0), processor.getSp()));
    			} else if (rm == 2) {
    				log.debug(String.format("Starting CodeBlock 0x%08X, r0=0x%08X, r1=0x%08X, lr=0x%08X, sp=0x%08X", addr, processor.getRegister(0), processor.getRegister(1), clearBit(processor.getLr(), 0), processor.getSp()));
    			} else if (rm == 3) {
    				log.debug(String.format("Starting CodeBlock 0x%08X, r0=0x%08X, r1=0x%08X, r2=0x%08X, lr=0x%08X, sp=0x%08X", addr, processor.getRegister(0), processor.getRegister(1), processor.getRegister(2), clearBit(processor.getLr(), 0), processor.getSp()));
    			} else {
    				log.debug(String.format("Starting CodeBlock 0x%08X, r0=0x%08X, r1=0x%08X, r2=0x%08X, r3=0x%08X, lr=0x%08X, sp=0x%08X", addr, processor.getRegister(0), processor.getRegister(1), processor.getRegister(2), processor.getRegister(3), clearBit(processor.getLr(), 0), processor.getSp()));
    			}
			}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bx %s", getRegisterName(insn >> 3));
        }
    };

    public static final ARMInstruction BLX_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rm = (insn >> 3) & 0xF;
        	int addr = processor.getRegister(rm);
        	processor.linkWithThumb();
    		processor.jumpWithMode(addr);

    		checkHLECall(processor);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("blx %s", getRegisterName(insn >> 3));
        }
    };

    public static final ARMInstruction BL_10_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int offset = (insn << 21) >> 9;
        	processor.setLr(processor.getPc() + offset);
        }

        @Override
        public String disasm(int address, int insn) {
        	int offset = (insn << 21) >> 9;
        	if (offset == 0) {
        		return String.format("bl +0 (part 1)");
        	} else if (offset < 0) {
                return String.format("bl -0x%03X000 (part 1)", -(offset >> 12));
        	}
            return String.format("bl +0x%03X000 (part 1)", offset >> 12);
        }
    };

    public static final ARMInstruction BL_11_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int offset = insn & 0x7FF;
        	int lr = processor.getLr();
        	processor.linkWithThumb();
        	int addr = lr + (offset << 1);
        	processor.jump(addr);

        	checkHLECall(processor);

        	if (RuntimeContext.debugCodeBlockCalls && log.isDebugEnabled()) {
    			// Log if not branching to a "bx rn" instruction
    			if ((processor.mem.internalRead16(addr) & 0xFF87) != 0x4700) {
    				log.debug(String.format("Starting CodeBlock 0x%08X, r0=0x%08X, r1=0x%08X, r2=0x%08X, r3=0x%08X, lr=0x%08X, sp=0x%08X", addr, processor.getRegister(0), processor.getRegister(1), processor.getRegister(2), processor.getRegister(3), clearBit(processor.getLr(), 0), processor.getSp()));
    			}
			}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bl 0x%08X (part 2)", address + 2 + ((insn & 0x7FF) << 1));
        }
    };

    public static final ARMInstruction BLX_01_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int offset = insn & 0x7FF;
        	int lr = processor.getLr();
        	processor.linkWithThumb();
        	int addr = clearFlag(lr + (offset << 1), 0x3);
        	processor.jump(addr);
        	processor.setARMMode();

        	checkHLECall(processor);

    		if (RuntimeContext.debugCodeBlockCalls && log.isDebugEnabled()) {
				log.debug(String.format("Starting CodeBlock 0x%08X, r0=0x%08X, r1=0x%08X, r2=0x%08X, r3=0x%08X, lr=0x%08X, sp=0x%08X", addr, processor.getRegister(0), processor.getRegister(1), processor.getRegister(2), processor.getRegister(3), clearBit(processor.getLr(), 0), processor.getSp()));
			}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("blx 0x%08X (part 2)", address + 2 + ((insn & 0x7FF) << 1));
        }
    };

    public static final ARMInstruction MCR = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int cpOperation = (insn >> 21) & 0x7;
        		int cpRegNumber = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int cpNumber = (insn >> 8) & 0xF;
        		int cpInformation = (insn >> 5) & 0x7;
        		int cpRegNumberOperand = insn & 0xF;
        		int rdValue = processor.getRegister(rd);

        		if (cpNumber == COPROCESSOR_SYSTEM_CONTROL && cpInformation == 0 && cpOperation == 0 && cpRegNumberOperand == 0) {
        			processor.setSystemControlCoprocessorRegister(cpRegNumber, rdValue);
        		} else {
        			log.error(String.format("Unimplemented mcr cpOperation=0x%X, cpRegNumber=%s, rd=%s(value=0x%08X), cpNumber=0x%X, cpInformation=0x%X, cpRegNumberOperand=%s", cpOperation, getCoprocessorRegisterName(cpRegNumber), getRegisterName(rd), rdValue, cpNumber, cpInformation, getCoprocessorRegisterName(cpRegNumberOperand)));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mcr%s %d, %d, %s, %s, %s, {%d}", getConditionName(insn), (insn >> 8) & 0xF, (insn >> 5) & 0x7, getRegisterName(insn >> 12), getCoprocessorRegisterName(insn >> 16), getCoprocessorRegisterName(insn), (insn >> 21) & 0x7);
        }
    };

    public static final ARMInstruction MRC = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int cpOperation = (insn >> 21) & 0x7;
        		int cpRegNumber = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int cpNumber = (insn >> 8) & 0xF;
        		int cpInformation = (insn >> 5) & 0x7;
        		int cpRegNumberOperand = insn & 0xF;

        		int value;
        		if (cpNumber == COPROCESSOR_SYSTEM_CONTROL && cpInformation == 0 && cpOperation == 0 && cpRegNumberOperand == 0) {
        			value = processor.getSystemControlCoprocessorRegister(cpRegNumber);
        		} else {
        			log.error(String.format("Unimplemented mrc cpOperation=0x%X, cpRegNumber=%s, rd=%s, cpNumber=0x%X, cpInformation=0x%X, cpRegNumberOperand=%s", cpOperation, getCoprocessorRegisterName(cpRegNumber), getRegisterName(rd), cpNumber, cpInformation, getCoprocessorRegisterName(cpRegNumberOperand)));
        			value = 0;
        		}
    			processor.setRegister(rd, value);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mrc%s %d, %d, %s, %s, %s, {%d}", getConditionName(insn), (insn >> 8) & 0xF, (insn >> 5) & 0x7, getRegisterName(insn >> 12), getCoprocessorRegisterName(insn >> 16), getCoprocessorRegisterName(insn), (insn >> 21) & 0x7);
        }
    };

    public static final ARMInstruction CDP = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int cpOperation = (insn >> 21) & 0x7;
        		int cpRegNumber = (insn >> 16) & 0xF;
        		int cpRegNumberDest = (insn >> 12) & 0xF;
        		int cpNumber = (insn >> 8) & 0xF;
        		int cpInformation = (insn >> 5) & 0x7;
        		int cpRegNumberOperand = insn & 0xF;

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("Unimplemented cdp cpOperation=0x%X, cpRegNumber=0x%X, cpRegNumberDest=0x%X, cpNumber=0x%X, cpInformation0x%X, cpRegNumberOperand=0x%X", cpOperation, cpRegNumber, cpRegNumberDest, cpNumber, cpInformation, cpRegNumberOperand, cpRegNumberOperand));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cdp%s %d, %d, %s, %s, %s, {%d}", getConditionName(insn), (insn >> 8) & 0xF, (insn >> 5) & 0x7, getCoprocessorRegisterName(insn >> 12), getCoprocessorRegisterName(insn >> 16), getCoprocessorRegisterName(insn), (insn >> 21) & 0x7);
        }
    };

    public static final ARMInstruction AND = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 & value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, processor.shifterCarryOut);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction EOR = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 ^ value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, processor.shifterCarryOut);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("eor%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction SUB = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 - value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub%s%s %s, %s, %s", getConditionName(insn), hasFlag(insn, 0x00100000) ? "s" : "", getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction RSB = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value2 - value1;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, getSubstractC(value2, value1, value), getSubstractV(value2, value1, value));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("rsb%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction ADD = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 + value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction ADC = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 + value2;
        		if (processor.hasCflag()) {
        			value++;
        		}
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("adc%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction SBC = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 - value2;
        		if (!processor.hasCflag()) {
        			value--;
        		}
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sbc%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction RSC = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value2 - value1;
        		if (!processor.hasCflag()) {
        			value--;
        		}
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, getSubstractC(value2, value1, value), getSubstractV(value2, value1, value));
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("rsc%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction TST = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 & value2;
        		processor.setCpsrResult(value, processor.shifterCarryOut);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("tst%s %s, %s", getConditionName(insn), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction TEQ = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 ^ value2;
        		processor.setCpsrResult(value, processor.shifterCarryOut);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("teq%s %s, %s", getConditionName(insn), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction CMP = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 - value2;
    			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp%s %s, %s", getConditionName(insn), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction CMN = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 + value2;
    			processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmn%s %s, %s", getConditionName(insn), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction ORR = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 | value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, processor.shifterCarryOut);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("orr%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction MOV = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rd = (insn >> 12) & 0xF;
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, processor.shifterCarryOut);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov%s%s %s, %s", getConditionName(insn), hasFlag(insn, 0x00100000) ? "s" : "", getRegisterName(insn >> 12), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction BIC = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int rd = (insn >> 12) & 0xF;
        		int value1 = processor.getRegister(rn);
        		int value2 = getAddressingMode1(processor, insn);
        		int value = value1 & ~value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, processor.shifterCarryOut);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bic%s %s, %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getRegisterName(insn >> 16), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction MVN = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rd = (insn >> 12) & 0xF;
        		int value2 = getAddressingMode1(processor, insn);
        		int value = ~value2;
        		processor.setRegister(rd, value);
        		if (hasFlag(insn, 0x00100000)) {
        			processor.setCpsrResult(value, processor.shifterCarryOut);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mvn%s %s, %s", getConditionName(insn), getRegisterName(insn >> 12), getAddressingMode1(insn));
        }
    };

    public static final ARMInstruction LDM = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int addr = processor.getRegister(rn);

        		// Pre- or Post-addressing?
        		int pre = hasFlag(insn, 0x01000000) ? 4 : 0;

        		// Increment/Decrement?
        		if (hasFlag(insn, 0x00800000)) {
    				addr += pre;
            		for (int i = 0; i < 16; i++) {
            			if (hasBit(insn, i)) {
            				int value = processor.mem.read32(addr);
            				processor.setRegister(i, value);
        					addr += 4;
            			}
            		}
        			addr -= pre;
        		} else {
    				addr -= pre;
            		for (int i = 15; i >= 0; i++) {
            			if (hasBit(insn, i)) {
            				int value = processor.mem.read32(addr);
            				processor.setRegister(i, value);
        					addr -= 4;
            			}
            		}
        			addr += pre;
        		}

        		if (hasFlag(insn, 0x00200000)) {
        			processor.setRegister(rn, addr);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldm%s%s %s%s, {%s}", LDM_STM[(insn >> 23) & 0x3], getConditionName(insn), getRegisterName(insn >> 16), hasFlag(insn, 0x00200000) ? "!" : "", getRegisterList(insn));
        }
    };

    public static final ARMInstruction STM = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = (insn >> 16) & 0xF;
        		int addr = processor.getRegister(rn);

        		// Pre- or Post-addressing?
        		int pre = hasFlag(insn, 0x01000000) ? 4 : 0;

        		// Increment/Decrement?
        		if (hasFlag(insn, 0x00800000)) {
    				addr += pre;
            		for (int i = 0; i < 16; i++) {
            			if (hasBit(insn, i)) {
            				int value = processor.getRegister(i);
            				processor.mem.write32(addr, value);
        					addr += 4;
            			}
            		}
        			addr -= pre;
        		} else {
    				addr -= pre;
            		for (int i = 15; i >= 0; i--) {
            			if (hasBit(insn, i)) {
            				int value = processor.getRegister(i);
            				processor.mem.write32(addr, value);
        					addr -= 4;
            			}
            		}
        			addr += pre;
        		}

        		if (hasFlag(insn, 0x00200000)) {
        			processor.setRegister(rn, addr);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("stm%s%s %s%s, {%s}", LDM_STM[(insn >> 23) & 0x3], getConditionName(insn), getRegisterName(insn >> 16), hasFlag(insn, 0x00200000) ? "!" : "", getRegisterList(insn));
        }
    };

    public static final ARMInstruction LDR = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rd = (insn >> 12) & 0xF;
        		int addr = getAddressingMode2(processor, insn);

    			int value;
    			if (hasFlag(insn, 0x00400000)) {
    				value = processor.mem.read8(addr);
    			} else {
    				value = processor.mem.read32(addr);
    			}
        		processor.setRegister(rd, value);

        		if (rd == REG_PC) {
        			checkHLECall(processor);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldr%s%s%s %s, %s", hasFlag(insn, 0x00400000) ? "b" : "", hasFlag(insn, 0x01000000) && hasFlag(insn, 0x00200000) ? "t" : "", getConditionName(insn), getRegisterName(insn >> 12), getAddressingMode2(insn));
        }
    };

    public static final ARMInstruction STR = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rd = (insn >> 12) & 0xF;
        		int addr = getAddressingMode2(processor, insn);
        		int value = processor.getRegister(rd);

        		if (hasFlag(insn, 0x00400000)) {
        			processor.mem.write8(addr, (byte) value);
        		} else {
        			processor.mem.write32(addr, value);
        		}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("str%s%s%s %s, %s", hasFlag(insn, 0x00400000) ? "b" : "", hasFlag(insn, 0x01000000) && hasFlag(insn, 0x00200000) ? "t" : "", getConditionName(insn), getRegisterName(insn >> 12), getAddressingMode2(insn));
        }
    };

    public static final ARMInstruction BX = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rn = insn & 0xF;
        		int addr = processor.getRegister(rn);
        		processor.jumpWithMode(addr);

        		checkHLECall(processor);

        		if (RuntimeContext.debugCodeBlockCalls && log.isDebugEnabled()) {
        			if (rn == REG_LR) {
        				log.debug(String.format("Returning from CodeBlock to 0x%08X, sp=0x%08X, r0=0x%08X", clearBit(addr, 0), processor.getSp(), processor.getRegister(0)));
        			}
    			}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bx%s %s", getConditionName(insn), getRegisterName(insn));
        }
    };

    public static final ARMInstruction PUSH_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int sp = processor.getSp();

        	if (hasFlag(insn, 0x0100)) {
    			sp -= 4;
        		int value = processor.getLr();
    			processor.mem.write32(sp, value);
        	}

        	for (int i = 7; i >= 0; i--) {
        		if (hasBit(insn, i)) {
        			sp -= 4;
        			int value = processor.getRegister(i);
        			processor.mem.write32(sp, value);
        		}
        	}

        	processor.setSp(sp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("push %s", getRegisterListThumb(insn, 0x0100, 0));
        }
    };

    public static final ARMInstruction POP_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int sp = processor.getSp();

        	for (int i = 0; i < 8; i++) {
        		if (hasBit(insn, i)) {
        			int value = processor.mem.read32(sp);
        			processor.setRegister(i, value);
        			sp += 4;
        		}
        	}

        	if (hasFlag(insn, 0x0100)) {
    			int addr = processor.mem.read32(sp);
    			processor.jumpWithMode(addr);
    			sp += 4;

    			checkHLECall(processor);

    			if (RuntimeContext.debugCodeBlockCalls && log.isDebugEnabled()) {
    				log.debug(String.format("Returning from CodeBlock to 0x%08X, sp=0x%08X, r0=0x%08X", clearBit(addr, 0), sp, processor.getRegister(0)));
    			}
        	}

        	processor.setSp(sp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("pop %s", getRegisterListThumb(insn, 0, 0x0100));
        }
    };

    public static final ARMInstruction ADD_Reg_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm3 = (insn >> 6) & 0x7;
        	int value1 = processor.getRegister(rn);
        	int value2 = imm3;
        	int value = value1 + value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
        	int imm3 = (insn >> 6) & 0x7;
        	if (imm3 == 0) {
                return String.format("mov %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        	}
            return String.format("add %s, %s, #%d", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), imm3);
        }
    };

    public static final ARMInstruction SUB_Reg_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm3 = (insn >> 6) & 0x7;
        	int value1 = processor.getRegister(rn);
        	int value2 = imm3;
        	int value = value1 - value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
        	int imm3 = (insn >> 6) & 0x7;
        	if (imm3 == 0) {
                return String.format("mov %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        	}
            return String.format("sub %s, %s, #%d", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), imm3);
        }
    };

    public static final ARMInstruction ADD_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int value1 = processor.getRegister(rn);
        	int value2 = processor.getRegister(rm);
        	int value = value1 + value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction SUB_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int value1 = processor.getRegister(rn);
        	int value2 = processor.getRegister(rm);
        	int value = value1 - value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction ADD_Sp_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm7 = insn & 0x7F;
        	int sp = processor.getSp();
        	processor.setSp(sp + (imm7 << 2));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s, #0x%X", getRegisterName(REG_SP), getRegisterName(REG_SP), (insn & 0x7F) << 2);
        }
    };

    public static final ARMInstruction SUB_Sp_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm7 = insn & 0x7F;
        	int sp = processor.getSp();
        	processor.setSp(sp - (imm7 << 2));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, %s, #0x%X", getRegisterName(REG_SP), getRegisterName(REG_SP), (insn & 0x7F) << 2);
        }
    };

    public static final ARMInstruction LDR_Stack_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	int rd = (insn >> 8) & 0x7;
        	int sp = processor.getSp();
        	int addr = sp + (imm8 << 2);
        	int value = processor.mem.read32(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldr %s, [%s, #0x%X]", getRegisterName((insn >> 8) & 0x7), getRegisterName(REG_SP), (insn & 0xFF) << 2);
        }
    };

    public static final ARMInstruction STR_Stack_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	int rd = (insn >> 8) & 0x7;
        	int sp = processor.getSp();
        	int addr = sp + (imm8 << 2);
        	int value = processor.getRegister(rd);
        	processor.mem.write32(addr, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("str %s, [%s, #0x%X]", getRegisterName((insn >> 8) & 0x7), getRegisterName(REG_SP), (insn & 0xFF) << 2);
        }
    };

    public static final ARMInstruction MOV_Immediate_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	int rd = (insn >> 8) & 0x7;
        	int value = imm8;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, #0x%X", getRegisterName((insn >> 8) & 0x7), insn & 0xFF);
        }
    };

    public static final ARMInstruction LDR_Pc_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	int rd = (insn >> 8) & 0x7;
        	int pc = processor.getPc();
        	int addr = clearBit(pc, 1) + (imm8 << 2);
        	int value = processor.mem.read32(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldr %s, [%s, #0x%X]", getRegisterName((insn >> 8) & 0x7), getRegisterName(REG_PC), (insn & 0xFF) << 2);
        }
    };

    public static final ARMInstruction LDR_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
        	int addr = processor.getRegister(rn) + (imm5 << 2);
        	int value = processor.mem.read32(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldr %s, [%s, #0x%X]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), ((insn >> 6) & 0x1F) << 2);
        }
    };

    public static final ARMInstruction STR_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
        	int addr = processor.getRegister(rn) + (imm5 << 2);
        	int value = processor.getRegister(rd);
        	processor.mem.write32(addr, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("str %s, [%s, #0x%X]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), ((insn >> 6) & 0x1F) << 2);
        }
    };

    public static final ARMInstruction MOV_High_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = (insn & 0x7) | ((insn >> 4) & 0x8);
        	int rm = (insn >> 3) & 0xF;
        	int value = processor.getRegister(rm);
        	if (rd == REG_PC) {
        		value = clearBit(value, 0);
        	}
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getRegisterName((insn & 0x7) | ((insn >> 4) & 0x8)), getRegisterName(insn >> 3));
        }
    };

    public static final ARMInstruction ADD_High_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = (insn & 0x7) | ((insn >> 4) & 0x8);
        	int rm = (insn >> 3) & 0xF;
        	int value = processor.getRegister(rm) + processor.getRegister(rd);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s", getRegisterName((insn & 0x7) | ((insn >> 4) & 0x8)), getRegisterName(insn >> 3));
        }
    };

    public static final ARMInstruction ADD_Rd_Sp_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	int rd = (insn >> 8) & 0x7;
        	int sp = processor.getSp();
        	int value = sp + (imm8 << 2);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s, #0x%X", getRegisterName((insn >> 8) & 0x7), getRegisterName(REG_SP), (insn & 0xFF) << 2);
        }
    };

    public static final ARMInstruction ADD_Rd_Pc_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	int rd = (insn >> 8) & 0x7;
        	int pc = processor.getPc();
        	int value = clearFlag(pc, 0x3) + (imm8 << 2);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s, #0x%X", getRegisterName((insn >> 8) & 0x7), getRegisterName(REG_PC), (insn & 0xFF) << 2);
        }
    };

    public static final ARMInstruction LDM_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = (insn >> 8) & 0x7;
        	int addr = processor.getRegister(rd);

        	for (int i = 0; i < 8; i++) {
        		if (hasBit(insn, i)) {
        			int value = processor.mem.read32(addr);
        			processor.setRegister(i, value);
        			addr += 4;
        		}
        	}

			// If the base register <Rn> is specified in <registers>, the final value of <Rn>
			// is the loaded value (not the written-back value)
        	if (notHasBit(insn, rd)) {
        		processor.setRegister(rd, addr);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldm%s %s!, %s", LDM_STM[1], getRegisterName((insn >> 8) & 0x7), getRegisterListThumb(insn, 0, 0));
        }
    };

    public static final ARMInstruction STM_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = (insn >> 8) & 0x7;
        	int addr = processor.getRegister(rd);

        	for (int i = 0; i < 8; i++) {
        		if (hasBit(insn, i)) {
        			int value = processor.getRegister(i);
        			processor.mem.write32(addr, value);
        			addr += 4;
        		}
        	}

        	processor.setRegister(rd, addr);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("stm%s %s!, %s", LDM_STM[1], getRegisterName((insn >> 8) & 0x7), getRegisterListThumb(insn, 0, 0));
        }
    };

    public static final ARMInstruction CMP_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	int rn = (insn >> 8) & 0x7;
        	int value1 = processor.getRegister(rn);
        	int value2 = imm8;
        	int value = value1 - value2;
			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, #0x%X", getRegisterName((insn >> 8) & 0x7), insn & 0xFF);
        }
    };

    public static final ARMInstruction B_Cond_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int cond = (insn >> 8) & 0xF;
        	if (processor.isConditionCode(cond)) {
        		// Check for the following sequence, which is a delay loop:
        		//   0x00005BE0: [0x3801] - sub r0, #0x1
        		//   0x00005BE2: [0xD2FD] - bcs 0x00005BE0
        		if (insn == 0xD2FD && processor.mem.internalRead16(processor.getCurrentInstructionPc() - 2) == 0x3801) {
        			processor.interpreter.delayHLE(processor.getRegister(REG_R0));
        			// Simulate the completion of the delay loop
        			processor.setRegister(REG_R0, -1);
        			// Continue execution after this branch instruction
        		} else {
        			processor.branch(insn << 24 >> 23);
        		}

        		checkHLECall(processor);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("b%s 0x%08X", conditionNames[(insn >> 8) & 0xF], address + 4 + (insn << 24 >> 23));
        }
    };

    public static final ARMInstruction B_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
    		processor.branch(insn << 21 >> 20);

    		checkHLECall(processor);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("b 0x%08X", address + 4 + (insn << 21 >> 20));
        }
    };

    public static final ARMInstruction LDRB_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = processor.mem.read8(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldrb %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction STRB_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = processor.getRegister(rd);
        	processor.mem.write8(addr, (byte) value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("strb %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction LDRSB_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = (byte) processor.mem.read8(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldrsb %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction LDRH_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = processor.mem.read16(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldrh %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction LDRSH_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = (short) processor.mem.read16(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldrsh %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction STRH_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = processor.getRegister(rd);
        	processor.mem.write16(addr, (short) value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("strh %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction LDRB_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
        	int addr = processor.getRegister(rn) + imm5;
        	int value = processor.mem.read8(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldrb %s, [%s, #0x%X]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), (insn >> 6) & 0x1F);
        }
    };

    public static final ARMInstruction STRB_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
        	int addr = processor.getRegister(rn) + imm5;
        	int value = processor.getRegister(rd);
        	processor.mem.write8(addr, (byte) value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("strb %s, [%s, #0x%X]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), (insn >> 6) & 0x1F);
        }
    };

    public static final ARMInstruction LDRH_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
        	int addr = processor.getRegister(rn) + (imm5 << 1);
        	int value = processor.mem.read16(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldrh %s, [%s, #0x%X]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), ((insn >> 6) & 0x1F) << 1);
        }
    };

    public static final ARMInstruction STRH_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
        	int addr = processor.getRegister(rn) + (imm5 << 1);
        	int value = processor.getRegister(rd);
        	processor.mem.write16(addr, (short) value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("strh %s, [%s, #0x%X]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), ((insn >> 6) & 0x1F) << 1);
        }
    };

    public static final ARMInstruction LSL_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
    		int rmValue = processor.getRegister(rm);
        	if (imm5 == 0) {
        		int value = rmValue;
            	processor.setRegister(rd, value);
            	processor.setCpsrResult(value);
        	} else {
        		int value = rmValue << imm5;
            	processor.setRegister(rd, value);
            	processor.setCpsrResult(value, (rmValue << (imm5 - 1)) < 0);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
        	int imm5 = (insn >> 6) & 0x1F;
        	if (imm5 == 0) {
                return String.format("mov %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        	}
            return String.format("lsl %s, %s, #0x%X", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), imm5);
        }
    };

    public static final ARMInstruction LSR_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
    		int rmValue = processor.getRegister(rm);
    		int rdValue = processor.getRegister(rd);
    		int shift = imm5 == 0 ? 32 : imm5;
    		int value = rmValue >>> shift;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, hasFlag(rdValue >>> (shift - 1), 0x1));
        }

        @Override
        public String disasm(int address, int insn) {
        	int imm5 = (insn >> 6) & 0x1F;
            return String.format("lsr %s, %s, #0x%X", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), imm5 == 0 ? 32 : imm5);
        }
    };

    public static final ARMInstruction LDR_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = processor.mem.read32(addr);
        	processor.setRegister(rd, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ldr %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction STR_Reg_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rn = (insn >> 3) & 0x7;
        	int rm = (insn >> 6) & 0x7;
        	int addr = processor.getRegister(rn) + processor.getRegister(rm);
        	int value = processor.getRegister(rd);
        	processor.mem.write32(addr, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("str %s, [%s, %s]", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), getRegisterName((insn >> 6) & 0x7));
        }
    };

    public static final ARMInstruction ADD_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = (insn >> 8) & 0x7;
        	int imm8 = insn & 0xFF;
        	int value1 = processor.getRegister(rd);
        	int value2 = imm8;
        	int value = value1 + value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, #0x%X", getRegisterName((insn >> 8) & 0x7), insn & 0xFF);
        }
    };

    public static final ARMInstruction SUB_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = (insn >> 8) & 0x7;
        	int imm8 = insn & 0xFF;
        	int value1 = processor.getRegister(rd);
        	int value2 = imm8;
        	int value = value1 - value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, #0x%X", getRegisterName((insn >> 8) & 0x7), insn & 0xFF);
        }
    };

    public static final ARMInstruction CMN_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rn = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rn);
        	int value2 = processor.getRegister(rm);
        	int value = value1 + value2;
        	processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmn %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction MUL_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 * value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mul %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction CMP_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rn = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rn);
        	int value2 = processor.getRegister(rm);
        	int value = value1 - value2;
			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction CMP_High_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rn = (insn & 0x7) | ((insn >> 4) & 0x8);
        	int rm = (insn >> 3) & 0xF;
        	int value1 = processor.getRegister(rn);
        	int value2 = processor.getRegister(rm);
        	int value = value1 - value2;
			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
			return String.format("cmp %s, %s", getRegisterName((insn & 0x7) | ((insn >> 4) & 0x8)), getRegisterName(insn >> 3));
        }
    };

    public static final ARMInstruction AND_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 & value2;
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction EOR_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 ^ value2;
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("eor %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction LSL_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rs = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rs) & 0xFF;
        	if (value2 == 0) {
            	processor.setCpsrResult(value1);
        	} else {
        		int value = value1 << value2;
            	processor.setRegister(rd, value);
            	processor.setCpsrResult(value, (value1 << (value2 - 1)) < 0);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("lsl %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction LSR_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rs = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rs) & 0xFF;
        	if (value2 == 0) {
            	processor.setCpsrResult(value1);
        	} else {
        		int value = value1 >>> value2;
            	processor.setRegister(rd, value);
            	processor.setCpsrResult(value, hasFlag(value1 >>> (value2 - 1), 0x1));
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("lsr %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction ASR_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rs = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rs) & 0xFF;
        	if (value2 == 0) {
            	processor.setCpsrResult(value1);
        	} else {
        		int value = value1 >> value2;
            	processor.setRegister(rd, value);
            	processor.setCpsrResult(value, hasFlag(value1 >> (value2 - 1), 0x1));
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("asr %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction ASR_Imm_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int imm5 = (insn >> 6) & 0x1F;
        	int value1 = processor.getRegister(rm);
        	int value2 = imm5 == 0 ? 32 : imm5;
    		int value = value1 >> value2;
        	processor.setRegister(rd, value);
        	processor.setCpsrResult(value, hasFlag(value1 >> (value2 - 1), 0x1));
        }

        @Override
        public String disasm(int address, int insn) {
        	int imm5 = (insn >> 6) & 0x1F;
            return String.format("asr %s, %s, #0x%X", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7), imm5 == 0 ? 32 : imm5);
        }
    };

    public static final ARMInstruction ORR_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 | value2;
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("orr %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction ADC_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 + value2;
        	if (processor.hasCflag()) {
        		value++;
        	}
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value, getAdditionC(value1, value2, value), getAdditionV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("adc %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction SBC_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 - value2;
        	if (!processor.hasCflag()) {
        		value--;
        	}
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sbc %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction ROR_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rs = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rs) & 0xFF;
        	if (value2 == 0) {
            	processor.setCpsrResult(value1);
        	} else {
        		int value = rotateRight(value1, value2);
            	processor.setRegister(rd, value);
            	processor.setCpsrResult(value, hasFlag(rotateRight(value1, value2 - 1), 0x1));
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ror %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction TST_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 & value2;
			processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("tst %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction NEG_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = 0;
        	int value2 = processor.getRegister(rm);
        	int value = value1 - value2;
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value, getSubstractC(value1, value2, value), getSubstractV(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("neg %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction BIC_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value1 = processor.getRegister(rd);
        	int value2 = processor.getRegister(rm);
        	int value = value1 & ~value2;
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bic %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction MVN_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int rd = insn & 0x7;
        	int rm = (insn >> 3) & 0x7;
        	int value2 = processor.getRegister(rm);
        	int value = ~value2;
        	processor.setRegister(rd, value);
			processor.setCpsrResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mvn %s, %s", getRegisterName(insn & 0x7), getRegisterName((insn >> 3) & 0x7));
        }
    };

    public static final ARMInstruction SWI = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	processor.softwareInterruptException();
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("swi 0x%06X", insn & 0x00FFFFFF);
        }
    };

    public static final ARMInstruction SWI_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	processor.softwareInterruptException();
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("swi 0x%02X", insn & 0x00FF);
        }
    };

    public static final ARMInstruction MSR = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
	        	int operand;
	        	if (hasFlag(insn, 0x02000000)) {
	        		int imm8 = insn & 0xFF;
	        		int rotateImm = (insn >> 8) & 0xF;
	        		operand = rotateRight(imm8, rotateImm << 1);
	        	} else {
	            	int rm = insn & 0xF;
	            	operand = processor.getRegister(rm);
	        	}

	        	if (hasFlag(insn, 0x00400000)) {
	        		if (processor.currentModeHasSpsr()) {
		        		int spsr = processor.getSpsr();
		        		if (hasFlag(insn, 0x00010000)) {
		        			spsr = setFlag(spsr, operand, 0x000000FF);
		        		}
		        		if (hasFlag(insn, 0x00020000)) {
		        			spsr = setFlag(spsr, operand, 0x0000FF00);
		        		}
		        		if (hasFlag(insn, 0x00040000)) {
		        			spsr = setFlag(spsr, operand, 0x00FF0000);
		        		}
		        		if (hasFlag(insn, 0x00080000)) {
		        			spsr = setFlag(spsr, operand, 0xFF000000);
		        		}
		        		processor.setSpsr(spsr);
	        		}
	        	} else {
	        		int cpsr = processor.getCpsr();
	        		if (processor.inAPrivilegeMode()) {
		        		if (hasFlag(insn, 0x00010000)) {
		        			cpsr = setFlag(cpsr, operand, 0x000000FF);
		        		}
		        		if (hasFlag(insn, 0x00020000)) {
		        			cpsr = setFlag(cpsr, operand, 0x0000FF00);
		        		}
		        		if (hasFlag(insn, 0x00040000)) {
		        			cpsr = setFlag(cpsr, operand, 0x00FF0000);
		        		}
	        		}
	        		if (hasFlag(insn, 0x00080000)) {
	        			cpsr = setFlag(cpsr, operand, 0xFF000000);
	        		}
	        		processor.setCpsr(cpsr);
	        	}
        	}
        }

        @Override
        public String disasm(int address, int insn) {
        	StringBuilder fields = new StringBuilder();
        	if (hasFlag(insn, 0x00400000)) {
        		fields.append("SPSR_");
        	} else {
        		fields.append("CPSR_");
        	}
        	if (hasFlag(insn, 0x00010000)) {
        		fields.append('c');
        	}
        	if (hasFlag(insn, 0x00020000)) {
        		fields.append('x');
        	}
        	if (hasFlag(insn, 0x00040000)) {
        		fields.append('s');
        	}
        	if (hasFlag(insn, 0x00080000)) {
        		fields.append('f');
        	}

        	StringBuilder operand = new StringBuilder();
        	if (hasFlag(insn, 0x02000000)) {
        		int imm8 = insn & 0xFF;
        		int rotateImm = (insn >> 8) & 0xF;
        		int value = rotateRight(imm8, rotateImm << 1);
        		operand.append(String.format("#0x%X", value));
        	} else {
        		operand.append(getRegisterName(insn));
        	}
            return String.format("msr%s %s, %s", getConditionName(insn), fields, operand);
        }
    };

    public static final ARMInstruction MRS = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	if (processor.isCondition(insn)) {
        		int rd = (insn >> 12) & 0xF;
	        	int value;
	        	if (hasFlag(insn, 0x00400000)) {
	        		value = processor.getSpsr();
	        	} else {
	        		value = processor.getCpsr();
	        	}
	        	processor.setRegister(rd, value);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mrs%s %s, %s", getConditionName(insn), getRegisterName(insn >> 12), hasFlag(insn, 0x00400000) ? "SPSR" : "CPSR");
        }
    };

    public static final ARMInstruction BKPT = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	// The BKPT must be unconditional
        	int imm16 = ((insn >> 4) & 0xFFF0) | (insn & 0xF);
        	if (!processor.interpreter.interpretHLE(processor.getCurrentInstructionPc(), imm16)) {
            	processor.prefetchAbortException();
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bkpt 0x%X", ((insn >> 4) & 0xFFF0) | (insn & 0xF));
        }
    };

    public static final ARMInstruction BKPT_Thumb = new ARMInstruction() {
        @Override
        public void interpret(ARMProcessor processor, int insn) {
        	int imm8 = insn & 0xFF;
        	if (!processor.interpreter.interpretHLE(processor.getCurrentInstructionPc(), imm8)) {
        		processor.prefetchAbortException();
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bkpt 0x%X", insn & 0xFF);
        }
    };
}
