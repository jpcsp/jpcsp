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
package jpcsp.nec78k0;

import static jpcsp.Emulator.EMU_STATUS_UNIMPLEMENTED;
import static jpcsp.nec78k0.Nec78k0Processor.REG_A;
import static jpcsp.nec78k0.Nec78k0Processor.REG_B;
import static jpcsp.nec78k0.Nec78k0Processor.REG_C;
import static jpcsp.nec78k0.Nec78k0Processor.REG_PAIR_AX;
import static jpcsp.nec78k0.Nec78k0Processor.REG_PAIR_DE;
import static jpcsp.nec78k0.Nec78k0Processor.REG_PAIR_HL;
import static jpcsp.nec78k0.Nec78k0Processor.REG_X;
import static jpcsp.nec78k0.Nec78k0Processor.getSaddr;
import static jpcsp.nec78k0.Nec78k0Processor.getSfr;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.notHasBit;
import static jpcsp.util.Utilities.setBit;

import java.util.HashMap;
import java.util.Map;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.ICompilerContext;

/**
 * @author gid15
 *
 */
public class Nec78k0Instructions {
	private static final String[] registerNames = {
		"X", "A", "C", "B", "E", "D", "L", "H"
	};
	private static final String[] registerPairNames = {
		"AX", "BC", "DE", "HL"
	};
	private static final String[] registerBankNames = {
		"RB0", "RB1", "RB2", "RB3"
	};
	private static final String pswName = "PSW";
	private static final String spName = "SP";
	private static final String cyName = "CY";
	private static final Map<Integer, String> functionNames = new HashMap<Integer, String>();

	public static String getRegisterName(int r) {
		return registerNames[r & 0x7];
	}

	public static String getRegisterPairName(int rp) {
		return registerPairNames[rp & 0x3];
	}

	public static String getRegisterBankName(int rbn) {
		return registerBankNames[rbn & 0x7];
	}

	public static String getFunctionName(int addr) {
		String functionName = functionNames.get(addr);
		if (functionName == null) {
			return String.format("0x%04X", addr);
		}

		return String.format("0x%04X (%s)", addr, functionName);
	}

	public static String getCalltFunctionName(int addr) {
		String functionName = functionNames.get(addr);
		if (functionName == null) {
			return String.format("[0x%04X]", addr);
		}

		return String.format("[0x%04X] (%s)", addr, functionName);
	}

	public static void registerFunctionName(int addr, String functionName) {
		functionNames.put(addr, functionName);
	}

	public static String getAddressName(int addr) {
		if (addr == Nec78k0Processor.SP_ADDRESS) {
			return spName;
		} else if (addr == Nec78k0Processor.PSW_ADDRESS) {
			return pswName;
		}
		return String.format("0x%04X", addr);
	}

	private static final boolean getAdditionCY(int value1, int value2, int value) {
		return hasFlag(value, 0x100);
	}

	private static final boolean getAddition16CY(int value1, int value2, int value) {
		return hasFlag(value, 0x10000);
	}

	private static final boolean getAdditionAC(int value1, int value2, int value) {
		return hasFlag(value, 0x10);
	}

	private static final boolean getSubstractionCY(int value1, int value2, boolean carry) {
		if (carry) {
			value2++;
		}
		return value1 < value2;
	}

	private static final boolean getSubstractionCY(int value1, int value2) {
		return getSubstractionCY(value1, value2, false);
	}

	private static final boolean getSubstraction16CY(int value1, int value2, boolean carry) {
		if (carry) {
			value2++;
		}
		return value1 < value2;
	}

	private static final boolean getSubstraction16CY(int value1, int value2) {
		return getSubstraction16CY(value1, value2, false);
	}

	private static final boolean getSubstractionAC(int value1, int value2, int value) {
		return hasFlag(value, 0x10);
	}

	private static final int getRegisterBank(int insn) {
		return ((insn >> 3) & 0x1) | ((insn >> 4) & 0x2);
	}

	public static final int getWord(int insn) {
		return ((insn >> 8) & 0xFF) | ((insn << 8) & 0xFF00);
	}

	private static final int getByte(int insn) {
		return insn & 0xFF;
	}

	private static final int getJdisp(int insn) {
		return (byte) insn;
	}

	public static final int getJdisp(int address, int insn, int size) {
		return address + size + getJdisp(insn);
	}

	public static final int getCallAddressWord(int insn, int addr) {
		// When located at an address above 0x8000, a
		//    call !addr
		// is actually a
		//    call !(addr | 0x8000)
		//
		// When located at an address below 0x8000, a
		//    call !addr
		// is calling the addr, even if addr is above 0x8000
		//
		// E.g.:
		//    0x0200 - call !0x0100   -> calling 0x0100
		//    0x0200 - call !0x8100   -> calling 0x8100
		//    0x8200 - call !0x0300   -> calling 0x8300
		//    0x8200 - call !0x8300   -> calling 0x8300
		return getWord(insn) | (addr & 0x8000);
	}

	public static final int getBranchAddressWord(int insn, int addr) {
		return getBranchAddress(getWord(insn), addr);
	}

	public static final int getBranchAddress(int branch, int addr) {
		// When located at an address above 0x8000, a
		//    br !addr
		// is actually a
		//    br !(addr | 0x8000)
		//
		// When located at an address below 0x8000, a
		//    br !addr
		// is actually a
		//    br !(addr & 0x7FFF)
		//
		// Same for
		//    br AX
		//
		// E.g.:
		//    0x0200 - br !0x0300   -> branching to 0x0300
		//    0x0200 - br !0x8300   -> branching to 0x0300
		//    0x8200 - br !0x8300   -> branching to 0x8300
		//    0x8200 - br !0x0300   -> branching to 0x8300
		return (branch & 0x7FFF) | (addr & 0x8000);
	}

	private static final String getBasedAddressing(String base, int value) {
		byte s8 = (byte) value;
		char sign;
		if (s8 < 0) {
			sign = '-';
			s8 = (byte) -s8;
		} else {
			sign = '+';
		}
		return String.format("[%s%c0x%02X]", base, sign, s8);
	}

	public static final Nec78k0Instruction UNK1 = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	log.error(String.format("0x%04X %s", processor.getCurrentInstructionPc(), disasm(processor.getCurrentInstructionPc(), insn)));
        	Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("Unknown instruction 0x%02X", insn);
        }
    };

	public static final Nec78k0Instruction UNK2 = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	log.error(String.format("0x%04X %s", processor.getCurrentInstructionPc(), disasm(processor.getCurrentInstructionPc(), insn)));
        	Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("Unknown instruction 0x%04X", insn);
        }
    };

	public static final Nec78k0Instruction UNK3 = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	log.error(String.format("0x%04X %s", processor.getCurrentInstructionPc(), disasm(processor.getCurrentInstructionPc(), insn)));
        	Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("Unknown instruction 0x%06X", insn);
        }
    };

	public static final Nec78k0Instruction UNK4 = new Nec78k0Instruction4() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	log.error(String.format("0x%04X %s", processor.getCurrentInstructionPc(), disasm(processor.getCurrentInstructionPc(), insn)));
        	Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("Unknown instruction 0x%08X", insn);
        }
    };

	public static abstract class STUB2 extends Nec78k0Instruction2 {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
            instance(processor, insn).interpret(processor, insn);
        }

        public Nec78k0Instruction instance(Nec78k0Instruction instruction, Nec78k0Processor processor) {
        	int size = instruction.getInstructionSize();
        	for (int i = getInstructionSize(); i < size; i++) {
        		processor.getNextInstructionOpcode();
        	}
        	return instruction;
        }

        @Override
        public void compile(ICompilerContext context, int insn) {
        	log.error(String.format("Unimplemented compile 0x%04X", insn));
        }

        @Override
        public String disasm(int address, int insn) {
        	log.error(String.format("Unimplemented disasm 0x%04X", insn));
        	return null;
        }
    }

	public static final Nec78k0Instruction NOP = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	// Nothing to do
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("nop");
        }
    };

	public static final Nec78k0Instruction MOV_A_r = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.getRegister(insn));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction ADD_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.getRegister(insn);
        	int value = value1 + value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction SEL = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rbn = getRegisterBank(insn);
        	processor.setRegisterBank(rbn);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sel %s", getRegisterBankName(getRegisterBank(insn)));
        }
    };

    public static final Nec78k0Instruction MOVW_saddrp_word = new Nec78k0Instruction4() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getWord(insn);
        	int saddrp = (insn >> 16) & 0xFF;
        	processor.mem.write16(getSaddr(saddrp), (short) imm16);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, #0x%04X", getAddressName(getSaddr(insn >> 16)), getWord(insn));
        }
    };

    public static final Nec78k0Instruction CALL = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getCallAddressWord(insn, processor.getCurrentInstructionPc());

        	if (!handleHLECall(processor, imm16, insn)) {
            	processor.call(imm16);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("call !%s", getFunctionName(getCallAddressWord(insn, address)));
        }
    };

    public static final Nec78k0Instruction RET = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.ret();
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ret");
        }
    };

    public static final Nec78k0Instruction MOVW_rp_word = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getWord(insn);
        	int rp = (insn >> 17) & 0x3;
        	processor.setRegisterPair(rp, imm16);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, #0x%04X", getRegisterPairName(insn >> 17), getWord(insn));
        }
    };

    public static final Nec78k0Instruction MOVW_addr_AX = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getWord(insn);
        	processor.mem.write16(imm16, (short) processor.getRegisterPair(REG_PAIR_AX));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw !0x%04X, %s", getWord(insn), getRegisterPairName(REG_PAIR_AX));
        }
    };

    public static final Nec78k0Instruction MOVW_AX_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getWord(insn);
        	processor.setRegisterPair(REG_PAIR_AX, processor.mem.read16(imm16));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, !0x%04X", getRegisterPairName(REG_PAIR_AX), getWord(insn));
        }
    };

    public static final Nec78k0Instruction INC_r = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(insn);
        	int value2 = 1;
        	int value = value1 + value2;
        	processor.setRegister(insn, value);
        	processor.setPswResult(value, getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("inc %s", getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction DEC_r = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(insn);
        	int value2 = 1;
        	int value = value1 - value2;
        	processor.setRegister(insn, value);
        	processor.setPswResult(value, getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("dec %s", getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction MOVW_AX_rp = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rp = (insn >> 1) & 0x3;
        	processor.setRegisterPair(REG_PAIR_AX, processor.getRegisterPair(rp));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, %s", getRegisterPairName(REG_PAIR_AX), getRegisterPairName(insn >> 1));
        }
    };

    public static final Nec78k0Instruction CMPW_AX_word = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegisterPair(REG_PAIR_AX);
        	int value2 = getWord(insn);
        	int value = value1 - value2;
        	// The AC flag become undefined, just clear it
        	processor.setPswResult16(value, getSubstraction16CY(value1, value2), false);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmpw %s, #0x%04X", getRegisterPairName(REG_PAIR_AX), getWord(insn));
        }
    };

    public static final Nec78k0Instruction CMP_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = getByte(insn);
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

    public static final Nec78k0Instruction ADD_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int saddr = getSaddr(insn >> 8);
        	int value1 = processor.mem.read8(saddr);
        	int value2 = getByte(insn);
        	int value = value1 + value2;
        	processor.mem.write8(saddr, (byte) value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

    public static final Nec78k0Instruction SUB_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int saddr = getSaddr(insn >> 8);
        	int value1 = processor.mem.read8(saddr);
        	int value2 = getByte(insn);
        	int value = value1 - value2;
        	processor.mem.write8(saddr, (byte) value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

    public static final Nec78k0Instruction BZ = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	if (processor.isZeroFlag()) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bz $0x%04X ; branch if ==", getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BNZ = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	if (!processor.isZeroFlag()) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bnz $0x%04X ; branch if !=", getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BC = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	if (processor.isCarryFlag()) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bc $0x%04X ; branch if <", getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BNC = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	if (!processor.isCarryFlag()) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bnc $0x%04X ; branch if >=", getJdisp(address, insn, getInstructionSize()));
        }
    };

	public static final Nec78k0Instruction MOV_A_HL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL));
        }
    };

	public static final Nec78k0Instruction MOV_A_DE = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.mem.read8(processor.getRegisterPair(REG_PAIR_DE)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_DE));
        }
    };

	public static final Nec78k0Instruction MOV_DE_A = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(processor.getRegisterPair(REG_PAIR_DE), (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov [%s], %s", getRegisterPairName(REG_PAIR_DE), getRegisterName(REG_A));
        }
    };

	public static final Nec78k0Instruction MOV_HL_A = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(processor.getRegisterPair(REG_PAIR_HL), (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov [%s], %s", getRegisterPairName(REG_PAIR_HL), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction INCW = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rp = (insn >> 1) & 0x3;
        	processor.setRegisterPair(rp, processor.getRegisterPair(rp) + 1);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("incw %s", getRegisterPairName(insn >> 1));
        }
    };

    public static final Nec78k0Instruction DECW = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rp = (insn >> 1) & 0x3;
        	processor.setRegisterPair(rp, processor.getRegisterPair(rp) - 1);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("decw %s", getRegisterPairName(insn >> 1));
        }
    };

    public static final Nec78k0Instruction BR_jdisp = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int jdisp = getJdisp(insn);
        	processor.branch(jdisp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("br $0x%04X", getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BR_word = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getBranchAddressWord(insn, processor.getCurrentInstructionPc());
        	processor.jump(imm16, false);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("br !0x%04X", getBranchAddressWord(insn, address));
        }
    };

    public static final Nec78k0Instruction BR_AX = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.jump(getBranchAddress(processor.getRegisterPair(REG_PAIR_AX), processor.getCurrentInstructionPc()), true);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("br %s", getRegisterPairName(REG_PAIR_AX));
        }
    };

	public static final Nec78k0Instruction MOV_r_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(insn >> 8, getByte(insn));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, #0x%02X", getRegisterName(insn >> 8), getByte(insn));
        }
    };

	public static final Nec78k0Instruction PUSH_rp = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rp = (insn >> 1) & 0x3;
        	processor.push16(processor.getRegisterPair(rp));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("push %s", getRegisterPairName(insn >> 1));
        }
    };

	public static final Nec78k0Instruction POP_rp = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rp = (insn >> 1) & 0x3;
        	processor.setRegisterPair(rp, processor.pop16());
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("pop %s", getRegisterPairName(insn >> 1));
        }
    };

    public static final Nec78k0Instruction MOV_sfr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm8 = getByte(insn);
        	int sfr = (insn >> 8) & 0xFF;
        	processor.mem.write8(getSfr(sfr), (byte) imm8);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, #0x%02X", getAddressName(getSfr(insn >> 8)), getByte(insn));
        }
    };

	public static final Nec78k0Instruction MOV_A_sfr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.mem.read8(getSfr(insn)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getRegisterName(REG_A), getAddressName(getSfr(insn)));
        }
    };

	public static final Nec78k0Instruction MOV_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.mem.read8(getSaddr(insn)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

	public static final Nec78k0Instruction CLR1_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	processor.mem.clear1(getSaddr(insn), bit);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("clr1 %s.%d", getAddressName(getSaddr(insn)), (insn >> 12) & 0x7);
        }
    };

	public static final Nec78k0Instruction CLR1_sfr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	processor.mem.clear1(getSfr(insn), bit);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("clr1 %s.%d", getAddressName(getSfr(insn)), (insn >> 12) & 0x7);
        }
    };

	public static final Nec78k0Instruction SET1_sfr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	processor.mem.set1(getSfr(insn), bit);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("set1 %s.%d", getAddressName(getSfr(insn)), (insn >> 12) & 0x7);
        }
    };

	public static final Nec78k0Instruction MOV_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(getSaddr(insn >> 8), (byte) getByte(insn));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

    public static final Nec78k0Instruction BT_sfr = new Nec78k0Instruction4() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 20) & 0x7;
        	int addr = getSfr(insn >> 8);
        	if (processor.mem.read1(addr, bit)) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bt %s.%d, $0x%04X", getAddressName(getSfr(insn >> 8)), (insn >> 20) & 0x7, getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BF_sfr = new Nec78k0Instruction4() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 20) & 0x7;
        	int addr = getSfr(insn >> 8);
        	if (!processor.mem.read1(addr, bit)) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bf %s.%d, $0x%04X", getAddressName(getSfr(insn >> 8)), (insn >> 20) & 0x7, getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BT_saddr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 20) & 0x7;
        	int addr = getSaddr(insn >> 8);
        	if (processor.mem.read1(addr, bit)) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bt %s.%d, $0x%04X", getAddressName(getSaddr(insn >> 8)), (insn >> 20) & 0x7, getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BF_saddr = new Nec78k0Instruction4() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 20) & 0x7;
        	int addr = getSaddr(insn >> 8);
        	if (!processor.mem.read1(addr, bit)) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bf %s.%d, $0x%04X", getAddressName(getSaddr(insn >> 8)), (insn >> 20) & 0x7, getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction MOVW_AX_saddrp = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegisterPair(REG_PAIR_AX, processor.mem.read16(getSaddr(insn)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, %s", getRegisterPairName(REG_PAIR_AX), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction MOVW_saddrp_AX = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write16(getSaddr(insn), (short) processor.getRegisterPair(REG_PAIR_AX));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, %s", getAddressName(getSaddr(insn)), getRegisterPairName(REG_PAIR_AX));
        }
    };

    public static final Nec78k0Instruction MOVW_rp_AX = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rp = (insn >> 1) & 0x3;
        	processor.setRegisterPair(rp, processor.getRegisterPair(REG_PAIR_AX));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, %s", getRegisterPairName(insn >> 1), getRegisterPairName(REG_PAIR_AX));
        }
    };

	public static final Nec78k0Instruction MOV_A_HLbyte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getRegisterName(REG_A), getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn));
        }
    };

	public static final Nec78k0Instruction SET1_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	processor.mem.set1(getSaddr(insn), bit);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("set1 %s.%d", getAddressName(getSaddr(insn)), (insn >> 12) & 0x7);
        }
    };

    public static final Nec78k0Instruction MOV_addr_A = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getWord(insn);
        	processor.mem.write8(imm16, (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov !0x%04X, %s", getWord(insn), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction MOV_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getWord(insn);
        	processor.setRegister(REG_A, processor.mem.read8(imm16));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction MOV_HLbyte_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn, (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn), getRegisterName(REG_A));
        }
    };

	public static final Nec78k0Instruction MOV_r_A = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(insn, processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getRegisterName(insn), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction MOV_HL_B_A = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(processor.getRegisterPair(REG_PAIR_HL) + processor.getRegister(REG_B), (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov [%s+%s], %s", getRegisterPairName(REG_PAIR_HL), getRegisterName(REG_B), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction MOV_HL_C_A = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(processor.getRegisterPair(REG_PAIR_HL) + processor.getRegister(REG_C), (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov [%s+%s], %s", getRegisterPairName(REG_PAIR_HL), getRegisterName(REG_C), getRegisterName(REG_A));
        }
    };

	public static final Nec78k0Instruction MOV_saddr_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(getSaddr(insn), (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getAddressName(getSaddr(insn)), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction AND_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) & getByte(insn);
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

	public static final Nec78k0Instruction MOV_sfr_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write8(getSfr(insn), (byte) processor.getRegister(REG_A));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, %s", getAddressName(getSfr(insn)), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction AND_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getSaddr(insn >> 8);
        	int value = processor.mem.read8(addr) & getByte(insn);
        	processor.mem.write8(addr, (byte) value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

    public static final Nec78k0Instruction MOVW_sfrp_word = new Nec78k0Instruction4() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int imm16 = getWord(insn);
        	int sfrp = (insn >> 16) & 0xFF;
        	processor.mem.write16(getSfr(sfrp), (short) imm16);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, #0x%04X", getAddressName(getSfr(insn >> 16)), getWord(insn));
        }
    };

    public static final Nec78k0Instruction OR_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) | getByte(insn);
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("or %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

    public static final Nec78k0Instruction OR_A_HLbyte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) | processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn);
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("or %s, %s", getRegisterName(REG_A), getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn));
        }
    };

    public static final Nec78k0Instruction CMP_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.mem.read8(getSaddr(insn >> 8));
        	int value2 = getByte(insn);
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

    public static final Nec78k0Instruction SUB_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = getByte(insn);
        	int value = value1 - value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

    public static final Nec78k0Instruction SUBC_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = getByte(insn);
        	int value = value1 - value2;
        	if (processor.isCarryFlag()) {
        		value--;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2, processor.isCarryFlag()), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("subc %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

    public static final Nec78k0Instruction MULU = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegisterPair(REG_PAIR_AX, processor.getRegister(REG_A) * processor.getRegister(REG_X));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mulu %s", getRegisterName(REG_X));
        }
    };

    public static final Nec78k0Instruction ADDW_AX_word = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegisterPair(REG_PAIR_AX);
        	int value2 = getWord(insn);
        	int value = value1 + value2;
        	processor.setRegisterPair(REG_PAIR_AX, value);
        	// The AC flag become undefined, just clear it
        	processor.setPswResult16(value, getAddition16CY(value1, value2, value), false);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addw %s, #0x%04X", getRegisterPairName(REG_PAIR_AX), getWord(insn));
        }
    };

    public static final Nec78k0Instruction SUBW_AX_word = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegisterPair(REG_PAIR_AX);
        	int value2 = getWord(insn);
        	int value = value1 - value2;
        	processor.setRegisterPair(REG_PAIR_AX, value);
        	// The AC flag become undefined, just clear it
        	processor.setPswResult16(value, getSubstraction16CY(value1, value2), false);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("subw %s, #0x%04X", getRegisterPairName(REG_PAIR_AX), getWord(insn));
        }
    };

	public static final Nec78k0Instruction XCH_A_r = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int tmp = processor.getRegister(REG_A);
        	processor.setRegister(REG_A, processor.getRegister(insn));
        	processor.setRegister(insn, tmp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xch %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction OR_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) | processor.mem.read8(getSaddr(insn));
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("or %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction AND_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) & processor.mem.read8(getSaddr(insn));
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction ROR = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A);
        	if (hasFlag(value, 0x1)) {
        		value = (value >> 1) | 0x80;
        		processor.setCarryFlag(true);
        	} else {
        		value >>= 1;
        		processor.setCarryFlag(false);
        	}
        	processor.setRegister(REG_A, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("ror %s, %d", getRegisterName(REG_A), 1);
        }
    };

    public static final Nec78k0Instruction HALT = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.halt();
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("halt");
        }
    };

    public static final Nec78k0Instruction STOP = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	log.info(String.format("stop at 0x%04X", processor.getCurrentInstructionPc()));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("stop");
        }
    };

    public static final Nec78k0Instruction DBNZ_B = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_B) - 1;
        	processor.setRegister(REG_B, value);
        	if (value != 0) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("dbnz %s, $0x%04X", getRegisterName(REG_B), getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction DBNZ_C = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_C) - 1;
        	processor.setRegister(REG_C, value);
        	if (value != 0) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("dbnz %s, $0x%04X", getRegisterName(REG_C), getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction DBNZ_saddr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getSaddr(insn >> 8);
        	int value = processor.mem.read8(addr) - 1;
        	processor.mem.write8(addr, (byte) value);
        	if (value != 0) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("dbnz %s, $0x%04X", getAddressName(getSaddr(insn >> 8)), getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction ADD_A_HL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL));
        	int value = value1 + value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL));
        }
    };

    public static final Nec78k0Instruction SUB_A_HL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL));
        	int value = value1 - value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL));
        }
    };

    public static final Nec78k0Instruction ADDC_A_HLbyte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn);
        	int value = value1 + value2;
        	if (processor.isCarryFlag()) {
        		value++;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addc %s, %s", getRegisterName(REG_A), getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn));
        }
    };

    public static final Nec78k0Instruction SUBC_A_HLbyte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn);
        	int value = value1 - value2;
        	if (processor.isCarryFlag()) {
        		value--;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("subc %s, %s", getRegisterName(REG_A), getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn));
        }
    };

    public static final Nec78k0Instruction ADD_A_HLbyte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn);
        	int value = value1 + value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s", getRegisterName(REG_A), getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn));
        }
    };

    public static final Nec78k0Instruction SUB_A_HLbyte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn);
        	int value = value1 - value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, %s", getRegisterName(REG_A), getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn));
        }
    };

    public static final Nec78k0Instruction ADD_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = getByte(insn);
        	int value = value1 + value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

    public static final Nec78k0Instruction ADD_r_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(insn);
        	int value2 = processor.getRegister(REG_A);
        	int value = value1 + value2;
        	processor.setRegister(insn, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s", getRegisterName(insn), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction ADDC_r_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(insn);
        	int value2 = processor.getRegister(REG_A);
        	int value = value1 + value2;
        	if (processor.isCarryFlag()) {
        		value++;
        	}
        	processor.setRegister(insn, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addc %s, %s", getRegisterName(insn), getRegisterName(REG_A));
        }
    };

	public static final Nec78k0Instruction XCHW_AX_rp = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int rp = (insn >> 1) & 0x3;
        	int tmp = processor.getRegisterPair(REG_PAIR_AX);
        	processor.setRegisterPair(REG_PAIR_AX, processor.getRegisterPair(rp));
        	processor.setRegisterPair(rp, tmp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xchw %s, %s", getRegisterPairName(REG_PAIR_AX), getRegisterPairName(insn >> 1));
        }
    };

    public static final Nec78k0Instruction MOV_A_HL_C = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + processor.getRegister(REG_C)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, [%s+%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL), getRegisterName(REG_C));
        }
    };

    public static final Nec78k0Instruction MOV_A_HL_B = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegister(REG_A, processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + processor.getRegister(REG_B)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov %s, [%s+%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL), getRegisterName(REG_B));
        }
    };

    public static final Nec78k0Instruction XOR_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) ^ processor.getRegister(insn);
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xor %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction OR_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) | processor.getRegister(insn);
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("or %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

	public static final Nec78k0Instruction XCH_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getSaddr(insn);
        	int tmp = processor.getRegister(REG_A);
        	processor.setRegister(REG_A, processor.mem.read8(addr));
        	processor.mem.write8(addr, (byte) tmp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xch %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction XOR_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) ^ processor.mem.read8(getSaddr(insn));
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xor %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction ADDC_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.getRegister(insn);
        	int value = value1 + value2;
        	if (processor.isCarryFlag()) {
        		value++;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addc %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

	public static final Nec78k0Instruction PUSH_PSW = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.push8(processor.getPsw());
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("push %s", pswName);
        }
    };

	public static final Nec78k0Instruction POP_PSW = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setPsw(processor.pop8());
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("pop %s", pswName);
        }
    };

    public static final Nec78k0Instruction CALLT = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = processor.mem.read16((insn & 0x3E) | 0x40);
        	processor.call(addr);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("callt %s", getCalltFunctionName((insn & 0x3E) | 0x40));
        }
    };

    public static final Nec78k0Instruction ROLC = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A);
        	int cy = processor.isCarryFlag() ? 0x01 : 0x00;
    		value = (value << 1) | cy;
    		processor.setCarryFlag(hasFlag(value, 0x100));
        	processor.setRegister(REG_A, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("rolc %s, %d", getRegisterName(REG_A), 1);
        }
    };

    public static final Nec78k0Instruction RORC = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A);
        	int cy = processor.isCarryFlag() ? 0x80 : 0x00;
        	boolean newCy = hasFlag(value, 0x01);
    		value = (value >> 1) | cy;
    		processor.setCarryFlag(newCy);
        	processor.setRegister(REG_A, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("rorc %s, %d", getRegisterName(REG_A), 1);
        }
    };

    public static final Nec78k0Instruction ROL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A);
    		value <<= 1;
    		if (hasFlag(value, 0x100)) {
    			value |= 0x01;
        		processor.setCarryFlag(true);
    		} else {
        		processor.setCarryFlag(false);
    		}
        	processor.setRegister(REG_A, value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("rol %s, %d", getRegisterName(REG_A), 1);
        }
    };

    public static final Nec78k0Instruction CMP_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getWord(insn));
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction CMP_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.getRegister(insn);
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction CMP_r_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(insn);
        	int value2 = processor.getRegister(REG_A);
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, %s", getRegisterName(insn), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction XOR_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) ^ getByte(insn);
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xor %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

    public static final Nec78k0Instruction BT_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	if (hasBit(processor.getRegister(REG_A), bit)) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bt %s.%d, $0x%04X", getRegisterName(REG_A), (insn >> 12) & 0x7, getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction BF_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	if (notHasBit(processor.getRegister(REG_A), bit)) {
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("bf %s.%d, $0x%04X", getRegisterName(REG_A), (insn >> 12) & 0x7, getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction CMP_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getSaddr(insn));
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction SUB_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.getRegister(insn);
        	int value = value1 - value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction SUBC_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.getRegister(insn);
        	int value = value1 - value2;
        	if (processor.isCarryFlag()) {
        		value--;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2, processor.isCarryFlag()), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("subc %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction SUBC_r_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(insn);
        	int value2 = processor.getRegister(REG_A);
        	int value = value1 - value2;
        	if (processor.isCarryFlag()) {
        		value--;
        	}
        	processor.setRegister(insn, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2, processor.isCarryFlag()), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("subc %s, %s", getRegisterName(insn), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction SUB_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getWord(insn));
        	int value = value1 - value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction SUBC_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getWord(insn));
        	int value = value1 - value2;
        	if (processor.isCarryFlag()) {
        		value--;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2, processor.isCarryFlag()), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("subc %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction SUB_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getSaddr(insn));
        	int value = value1 - value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction SUBC_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getSaddr(insn));
        	int value = value1 - value2;
        	if (processor.isCarryFlag()) {
        		value--;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2, processor.isCarryFlag()), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("subc %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction DIVUW = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegisterPair(REG_PAIR_AX);
        	int value2 = processor.getRegister(REG_C);
        	if (value2 == 0) {
        		processor.setRegister(REG_C, processor.getRegister(REG_X));
        		processor.setRegisterPair(REG_PAIR_AX, 0xFFFF);
        	} else {
        		processor.setRegister(REG_C, value1 % value2);
        		processor.setRegisterPair(REG_PAIR_AX, value1 / value2);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("divuw %s", getRegisterName(REG_C));
        }
    };

    public static final Nec78k0Instruction OR1_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 4) & 0x7;
        	if (hasBit(processor.getRegister(REG_A), bit)) {
        		processor.setCarryFlag(true);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("or1 %s, %s.%d", cyName, getRegisterName(REG_A), (insn >> 4) & 0x7);
        }
    };

    public static final Nec78k0Instruction ADD_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getWord(insn));
        	int value = value1 + value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction ADDC_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getWord(insn));
        	int value = value1 + value2;
        	if (processor.isCarryFlag()) {
        		value++;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addc %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction MOV1_CY_saddr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	processor.setCarryFlag(processor.mem.read1(getSaddr(insn), bit));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov1 %s, %s.%d", cyName, getAddressName(getSaddr(insn)), (insn >> 12) & 0x7);
        }
    };

    public static final Nec78k0Instruction MOV1_saddr_CY = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	processor.mem.write1(getSaddr(insn), bit, processor.isCarryFlag());
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov1 %s.%d, %s", getAddressName(getSaddr(insn)), (insn >> 12) & 0x7, cyName);
        }
    };

    public static final Nec78k0Instruction XOR1_CY_saddr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 12) & 0x7;
        	if (processor.mem.read1(getSaddr(insn), bit)) {
        		processor.setCarryFlag(!processor.isCarryFlag());
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xor1 %s, %s.%d", cyName, getAddressName(getSaddr(insn)), (insn >> 12) & 0x7);
        }
    };

    public static final Nec78k0Instruction XOR_A_HL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) ^ processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL));
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xor %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL));
        }
    };

    public static final Nec78k0Instruction SUB_r_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(insn);
        	int value2 = processor.getRegister(REG_A);
        	int value = value1 - value2;
        	processor.setRegister(insn, value);
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("sub %s, %s", getRegisterName(insn), getRegisterName(REG_A));
        }
    };

    public static final Nec78k0Instruction CLR1_CY = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setCarryFlag(false);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("clr1 %s", cyName);
        }
    };

    public static final Nec78k0Instruction SET1_CY = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setCarryFlag(true);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("set1 %s", cyName);
        }
    };

    public static final Nec78k0Instruction NOT1_CY = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setCarryFlag(!processor.isCarryFlag());
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("not1 %s", cyName);
        }
    };

    public static final Nec78k0Instruction XOR_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getSaddr(insn >> 8);
        	int value = processor.mem.read8(addr) ^ getByte(insn);
        	processor.mem.write8(addr, (byte) value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xor %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

    public static final Nec78k0Instruction MOVW_AX_sfrp = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.setRegisterPair(REG_PAIR_AX, processor.mem.read16(getSfr(insn)));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, %s", getRegisterPairName(REG_PAIR_AX), getAddressName(getSfr(insn)));
        }
    };

    public static final Nec78k0Instruction MOVW_sfrp_AX = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.mem.write16(getSfr(insn), (short) processor.getRegisterPair(REG_PAIR_AX));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("movw %s, %s", getAddressName(getSfr(insn)), getRegisterPairName(REG_PAIR_AX));
        }
    };

	public static final Nec78k0Instruction XCH_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getWord(insn);
        	int tmp = processor.getRegister(REG_A);
        	processor.setRegister(REG_A, processor.mem.read8(addr));
        	processor.mem.write8(addr, (byte) tmp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xch %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction XOR_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) ^ processor.mem.read8(getWord(insn));
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xor %s, !0x%04X", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction CMP_A_HL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL));
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL));
        }
    };

    public static final Nec78k0Instruction RETI = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	processor.reti();
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("reti");
        }
    };

    public static final Nec78k0Instruction INC_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getSaddr(insn);
        	int value1 = processor.mem.read8(addr);
        	int value2 = 1;
        	int value = value1 + value2;
        	processor.mem.write8(addr, (byte) value);
        	processor.setPswResult(value, getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("inc %s", getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction DEC_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getSaddr(insn);
        	int value1 = processor.mem.read8(addr);
        	int value2 = 1;
        	int value = value1 - value2;
        	processor.mem.write8(addr, (byte) value);
        	processor.setPswResult(value, getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("dec %s", getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction ADD_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getSaddr(insn));
        	int value = value1 + value2;
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("add %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction OR_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int addr = getSaddr(insn >> 8);
        	int value = processor.mem.read8(addr) | getByte(insn);
        	processor.mem.write8(addr, (byte) value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("or %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

    public static final Nec78k0Instruction CMP_A_HLbyte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL) + (byte) insn);
        	int value = value1 - value2;
        	processor.setPswResult(value, getSubstractionCY(value1, value2), getSubstractionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("cmp %s, %s", getRegisterName(REG_A), getBasedAddressing(getRegisterPairName(REG_PAIR_HL), insn));
        }
    };

    public static final Nec78k0Instruction AND_A_addr = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) & processor.mem.read8(getWord(insn));
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and %s, %s", getRegisterName(REG_A), getWord(insn));
        }
    };

    public static final Nec78k0Instruction MOV1_CY_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 4) & 0x7;
        	processor.setCarryFlag(hasBit(processor.getRegister(REG_A), bit));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("mov1 %s, %s.%d", cyName, getRegisterName(REG_A), (insn >> 4) & 0x7);
        }
    };

    public static final Nec78k0Instruction AND_A_r = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) & processor.getRegister(insn);
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and %s, %s", getRegisterName(REG_A), getRegisterName(insn));
        }
    };

    public static final Nec78k0Instruction XCH_A_DE = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int tmp = processor.getRegister(REG_A);
        	int addr = processor.getRegisterPair(REG_PAIR_DE);
        	processor.setRegister(REG_A, processor.mem.read8(addr));
        	processor.mem.write8(addr, (byte) tmp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xch %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_DE));
        }
    };

    public static final Nec78k0Instruction XCH_A_HL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int tmp = processor.getRegister(REG_A);
        	int addr = processor.getRegisterPair(REG_PAIR_HL);
        	processor.setRegister(REG_A, processor.mem.read8(addr));
        	processor.mem.write8(addr, (byte) tmp);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("xch %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL));
        }
    };

    public static final Nec78k0Instruction ADDC_A_saddr = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = processor.mem.read8(getSaddr(insn));
        	int value = value1 + value2;
        	if (processor.isCarryFlag()) {
        		value++;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addc %s, %s", getRegisterName(REG_A), getAddressName(getSaddr(insn)));
        }
    };

    public static final Nec78k0Instruction ADDC_saddr_byte = new Nec78k0Instruction3() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int saddr = getSaddr(insn >> 8);
        	int value1 = processor.mem.read8(saddr);
        	int value2 = getByte(insn);
        	int value = value1 + value2;
        	if (processor.isCarryFlag()) {
        		value++;
        	}
        	processor.mem.write8(saddr, (byte) value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addc %s, #0x%02X", getAddressName(getSaddr(insn >> 8)), getByte(insn));
        }
    };

	public static final Nec78k0Instruction CLR1_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 4) & 0x7;
        	processor.setRegister(REG_A, clearBit(processor.getRegister(REG_A), bit));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("clr1 %s.%d", getRegisterName(REG_A), (insn >> 4) & 0x7);
        }
    };

	public static final Nec78k0Instruction SET1_A = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 4) & 0x7;
        	processor.setRegister(REG_A, setBit(processor.getRegister(REG_A), bit));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("set1 %s.%d", getRegisterName(REG_A), (insn >> 4) & 0x7);
        }
    };

    public static final Nec78k0Instruction ADDC_A_byte = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value1 = processor.getRegister(REG_A);
        	int value2 = getByte(insn);
        	int value = value1 + value2;
        	if (processor.isCarryFlag()) {
        		value++;
        	}
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value, getAdditionCY(value1, value2, value), getAdditionAC(value1, value2, value));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("addc %s, #0x%02X", getRegisterName(REG_A), getByte(insn));
        }
    };

	public static final Nec78k0Instruction CLR1_HL = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 4) & 0x7;
        	int addr = processor.getRegisterPair(REG_PAIR_HL);
        	processor.mem.write8(addr, (byte) clearBit(processor.mem.read8(addr), bit));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("clr1 [%s].%d", getRegisterPairName(REG_PAIR_HL), (insn >> 4) & 0x7);
        }
    };

	public static final Nec78k0Instruction SET1_HL = new Nec78k0Instruction2() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 4) & 0x7;
        	int addr = processor.getRegisterPair(REG_PAIR_HL);
        	processor.mem.write8(addr, (byte) setBit(processor.mem.read8(addr), bit));
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("set1 [%s].%d", getRegisterPairName(REG_PAIR_HL), (insn >> 4) & 0x7);
        }
    };

    public static final Nec78k0Instruction BTCLR_saddr = new Nec78k0Instruction4() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int bit = (insn >> 20) & 0x7;
        	int addr = getSaddr(insn >> 8);
        	if (processor.mem.read1(addr, bit)) {
            	processor.mem.clear1(addr, bit);
            	int jdisp = getJdisp(insn);
            	processor.branch(jdisp);
        	}
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("btclr %s.%d, $0x%04X", getAddressName(getSaddr(insn >> 8)), (insn >> 20) & 0x7, getJdisp(address, insn, getInstructionSize()));
        }
    };

    public static final Nec78k0Instruction AND_A_HL = new Nec78k0Instruction1() {
        @Override
        public void interpret(Nec78k0Processor processor, int insn) {
        	int value = processor.getRegister(REG_A) & processor.mem.read8(processor.getRegisterPair(REG_PAIR_HL));
        	processor.setRegister(REG_A, value);
        	processor.setPswResult(value);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("and %s, [%s]", getRegisterName(REG_A), getRegisterPairName(REG_PAIR_HL));
        }
    };
}
