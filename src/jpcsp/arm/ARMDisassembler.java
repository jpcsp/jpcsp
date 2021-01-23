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
import static jpcsp.arm.ARMInstructions.ADD;
import static jpcsp.arm.ARMInstructions.ADD_Rd_Pc_Thumb;
import static jpcsp.arm.ARMInstructions.B;
import static jpcsp.arm.ARMInstructions.BKPT;
import static jpcsp.arm.ARMInstructions.BKPT_Thumb;
import static jpcsp.arm.ARMInstructions.BL;
import static jpcsp.arm.ARMInstructions.BLX_01_Thumb;
import static jpcsp.arm.ARMInstructions.BL_10_Thumb;
import static jpcsp.arm.ARMInstructions.BL_11_Thumb;
import static jpcsp.arm.ARMInstructions.BX;
import static jpcsp.arm.ARMInstructions.BX_Thumb;
import static jpcsp.arm.ARMInstructions.B_Cond_Thumb;
import static jpcsp.arm.ARMInstructions.B_Thumb;
import static jpcsp.arm.ARMInstructions.LDR;
import static jpcsp.arm.ARMInstructions.LDR_Pc_Thumb;
import static jpcsp.arm.ARMInstructions.MOV_High_Thumb;
import static jpcsp.arm.ARMInstructions.POP_Thumb;
import static jpcsp.arm.ARMProcessor.REG_PC;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.notHasFlag;
import static jpcsp.util.Utilities.setBit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class ARMDisassembler {
	private final Logger log;
	private final Level level;
	private final ARMMemory mem;
	private final ARMInterpreter interpreter;
	private final List<Integer> pendingFunctions = new LinkedList<Integer>();
	private final Set<Integer> disassembledFunctions = new HashSet<Integer>();
	private final Map<Integer, Integer> branchingTo = new HashMap<Integer, Integer>();
	private final Set<Integer> branchingTargets = new HashSet<Integer>();

	public ARMDisassembler(Logger log, Level level, ARMMemory mem, ARMInterpreter interpreter) {
		this.log = log;
		this.level = level;
		this.mem = mem;
		this.interpreter = interpreter;
	}

	private void log(String s) {
		log.log(level, s);
	}

	private boolean isAlwaysCondition(int insn) {
		return (insn >>> 28) == 0xE;
	}

	private int getJumpFromBranchOffset(int addr, int offset, boolean thumbMode) {
		return addr + offset + (thumbMode ? 4 + 1 : 8);
	}

	private int getRegister(int insn, int offset) {
		return (insn >> offset) & 0xF;
	}

	private String getRegisterName(int insn, int offset) {
		return ARMInstructions.getRegisterName(getRegister(insn, offset));
	}

	private int getThumbRegister(int insn, int offset) {
		return (insn >> offset) & 0x7;
	}

	private String getThumbRegisterName(int insn, int offset) {
		return ARMInstructions.getRegisterName(getThumbRegister(insn, offset));
	}

	private void addJump(List<Integer> list, int addr, int jumpTo) {
		branchingTo.put(addr, jumpTo);
		branchingTargets.add(clearBit(jumpTo, 0));
		list.add(jumpTo);
	}

	private void addJumpToFunction(int addr, int jumpTo) {
		branchingTo.put(addr, jumpTo);
		branchingTargets.add(clearBit(jumpTo, 0));
		pendingFunctions.add(jumpTo);
	}

	private boolean isLdrFixedValue(ARMInstruction instr, int insn, boolean allowRead8) {
		return instr == LDR && notHasFlag(insn, 0x02000000) && (notHasFlag(insn, 0x00400000) || allowRead8) && getRegister(insn, 16) == REG_PC;
	}

	private boolean isLdrFixedValue(int insn, boolean allowRead8) {
		return isLdrFixedValue(ARMDecoder.instruction(insn), insn, allowRead8);
	}

	private int getLdrFixedValue(int insn, int base) {
		int ptr = base;
		if (hasFlag(insn, 0x01000000)) {
			int offset = insn & 0xFFF;
			if (hasFlag(insn, 0x00800000)) {
				ptr += offset;
			} else {
				ptr -= offset;
			}
		}

		int value;
		if (hasFlag(insn, 0x00400000)) {
			value = mem.internalRead8(ptr);
		} else {
			value = mem.internalRead32(ptr);
		}

		return value;
	}

	private int getThumbLdrFixedValue(int insn, int base) {
		int value = mem.internalRead32(clearBit(base, 1) + ((insn & 0xFF) << 2));

		return value;
	}

	private boolean isEndOfBlock(int addr, int insn, ARMInstruction instr, boolean thumbMode) {
		if (instr == B_Thumb || instr == BX_Thumb) {
			return true;
		} else if ((instr == B || instr == BX) && isAlwaysCondition(insn)) {
			return true;
		} else if (instr == POP_Thumb && hasFlag(insn, 0x0100)) {
			// pop pc
			return true;
		} else if (instr == LDR && isAlwaysCondition(insn) && getRegister(insn, 12) == REG_PC) {
			return true;
		} else if (instr == BKPT || instr == BKPT_Thumb) {
			return true;
		} else if (instr == MOV_High_Thumb) {
        	int rd = (insn & 0x7) | ((insn >> 4) & 0x8);
        	if (rd == REG_PC) {
        		// mov pc, rn
        		return true;
        	}
		}

		return false;
	}

	private void checkBranch(List<Integer> list, int addr, int insn, ARMInstruction instr, boolean thumbMode) {
		if (instr == B) {
			int jumpTo = getJumpFromBranchOffset(addr, insn << 8 >> 6, thumbMode);
			addJump(list, addr, jumpTo);
		} else if (instr == BL) {
			int jumpTo = getJumpFromBranchOffset(addr, insn << 8 >> 6, thumbMode);
			addJumpToFunction(addr, jumpTo);
		} else if (isLdrFixedValue(instr, insn, false) && getRegister(insn, 12) == REG_PC) {
			int jumpTo = getLdrFixedValue(insn, addr + 8);
			addJump(list, addr, jumpTo);
		} else if (instr == BX) {
			if (getRegister(insn, 0) == REG_PC) {
				// bx pc
				addJump(list, addr, addr + 8);
			} else {
				// Check for sequence:
				//   ldr RN, [pc, #...]
				//   bx RN
				int previousInsn = mem.internalRead32(addr - 4);
				if (isLdrFixedValue(previousInsn, false) && getRegister(insn, 0) == getRegister(previousInsn, 12)) {
					int jumpTo = getLdrFixedValue(previousInsn, addr + 4);
					addJump(list, addr, jumpTo);
				}
			}
		} else if (instr == BX_Thumb) {
			if (getRegister(insn, 3) == REG_PC) {
				addJump(list, addr, addr + 4);
			} else {
				int previousInsn = mem.internalRead16(addr - 2);
				ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
				if (previousInstr == ADD_Rd_Pc_Thumb && getThumbRegister(insn, 3) == getThumbRegister(previousInsn, 8)) {
					int jumpTo = clearFlag(addr + 4, 0x3) + ((previousInsn & 0xFF) << 2);
					addJump(list, addr, jumpTo);
				}
			}
		} else if (instr == BL_11_Thumb || instr == BLX_01_Thumb) {
			int previousInsn = mem.internalRead16(addr - 2);
			ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
			if (previousInstr == BL_10_Thumb) {
	        	int offset1 = (previousInsn << 21) >> 9;
	        	int offset2 = (insn & 0x7FF) << 1;
	        	int jumpTo = addr + 2 + offset1 + offset2;
	        	if (instr == BL_11_Thumb) {
	        		jumpTo = setBit(jumpTo, 0);
	        	} else if (instr == BLX_01_Thumb) {
	        		jumpTo = clearFlag(jumpTo, 0x3);
	        	}
	        	addJumpToFunction(addr, jumpTo);
			}
		} else if (instr == B_Thumb) {
			int jumpTo = getJumpFromBranchOffset(addr, insn << 21 >> 20, thumbMode);
			addJump(list, addr, jumpTo);
		} else if (instr == B_Cond_Thumb) {
			int jumpTo = getJumpFromBranchOffset(addr, insn << 24 >> 23, thumbMode);
			addJump(list, addr, jumpTo);
		} else if (instr == MOV_High_Thumb) {
        	int rd = (insn & 0x7) | ((insn >> 4) & 0x8);
        	if (rd == REG_PC) {
        		// Check for sequence:
        		//   ldr RM, [pc, #...]
        		//   mov pc, RM
        		int previousInsn = mem.internalRead16(addr - 2);
        		ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
        		if (previousInstr == LDR_Pc_Thumb && getThumbRegister(previousInsn, 8) == getRegister(insn, 3)) {
        			int jumpTo = getThumbLdrFixedValue(previousInsn, addr + 2);
        			addJump(list, addr, setBit(jumpTo, 0)); // Always jumping to Thumb code
        		}
        	}
		}
	}

	private String getAdditionalInfo(int addr, int insn, ARMInstruction instr, boolean thumbMode) {
		String additionalInfo = null;

		if (isLdrFixedValue(instr, insn, true)) {
			int value = getLdrFixedValue(insn, addr + 8);
			if (hasFlag(insn, 0x00400000)) {
				additionalInfo = String.format(" <=> mov %s, #0x%02X", getRegisterName(insn, 12), value);
			} else {
				additionalInfo = String.format(" <=> mov %s, #0x%08X", getRegisterName(insn, 12), value);
			}
		} else if (instr == LDR_Pc_Thumb) {
			int value = getThumbLdrFixedValue(insn, addr + 4);
			additionalInfo = String.format(" <=> mov %s, #0x%08X", getThumbRegisterName(insn, 8), value);
		} else if (instr == BX) {
			if (getRegister(insn, 0) == REG_PC) {
				// bx pc
				additionalInfo = String.format(" <=> bx 0x%08X", addr + 8);
			} else {
				// Check for sequence:
				//   ldr RN, [pc, #...]
				//   bx RN
				int previousInsn = mem.internalRead32(addr - 4);
				if (isLdrFixedValue(previousInsn, false) && getRegister(insn, 0) == getRegister(previousInsn, 12)) {
					int jumpTo = getLdrFixedValue(previousInsn, addr + 4);
					additionalInfo = String.format(" <=> bx 0x%08X", jumpTo);
				}
			}
		} else if (instr == ADD && getRegister(insn, 16) == REG_PC && hasFlag(insn, 0x02000000)) {
			int imm8 = insn & 0xFF; // Unsigned 8 bit immediate value
			int immRot = (insn >> 8) & 0xF; // Rotate applied to imm8
			int value = rotateRight(imm8, immRot << 1);
			additionalInfo = String.format(" <=> movs %s, #0x%08X", getRegisterName(insn, 12), addr + 8 + value);
		} else if (instr == BX_Thumb) {
			if (getRegister(insn, 3) == REG_PC) {
				additionalInfo = String.format(" <=> bx 0x%08X", addr + 4);
			} else {
				int previousInsn = mem.internalRead16(addr - 2);
				ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
				if (previousInstr == ADD_Rd_Pc_Thumb && getThumbRegister(insn, 3) == getThumbRegister(previousInsn, 8)) {
					int jumpTo = clearFlag(addr + 4, 0x3) + ((previousInsn & 0xFF) << 2);
					additionalInfo = String.format(" <=> bx 0x%08X", jumpTo);
				}
			}
		} else if (instr == BL_11_Thumb || instr == BLX_01_Thumb) {
			int previousInsn = mem.internalRead16(addr - 2);
			ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
			if (previousInstr == BL_10_Thumb) {
	        	int offset1 = (previousInsn << 21) >> 9;
	        	int offset2 = (insn & 0x7FF) << 1;
	        	int jumpTo = addr + 2 + offset1 + offset2;
	        	if (instr == BLX_01_Thumb) {
	        		jumpTo = clearFlag(jumpTo, 0x3);
	        	}
				additionalInfo = String.format(" <=> bl%s 0x%08X", instr == BLX_01_Thumb ? "x" : "", jumpTo);
			}
		} else if (instr == BKPT_Thumb) {
			IARMHLECall hleCall = interpreter.getHLECall(addr, insn & 0xFF);
			if (hleCall != null) {
				additionalInfo = String.format(" <=> %s", hleCall);
			}
		} else if (instr == BKPT) {
			IARMHLECall hleCall = interpreter.getHLECall(addr, ((insn >> 4) & 0xFFF0) | (insn & 0xF));
			if (hleCall != null) {
				additionalInfo = String.format(" <=> %s", hleCall);
			}
		} else if (instr == MOV_High_Thumb) {
        	int rd = (insn & 0x7) | ((insn >> 4) & 0x8);
        	if (rd == REG_PC) {
        		// Check for sequence:
        		//   ldr RM, [pc, #...]
        		//   mov pc, RM
        		int previousInsn = mem.internalRead16(addr - 2);
        		ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
        		if (previousInstr == LDR_Pc_Thumb && getThumbRegister(previousInsn, 8) == getRegister(insn, 3)) {
        			int jumpTo = getThumbLdrFixedValue(previousInsn, addr + 2);
    				additionalInfo = String.format(" <=> b 0x%08X", clearBit(jumpTo, 0));
        		}
        	}
		}

		return additionalInfo;
	}

	private void disasmFunction(int startAddress) {
		if (disassembledFunctions.contains(startAddress)) {
			// Already disassembled
			return;
		}
		log(String.format("Disassembling Function 0x%08X", clearBit(startAddress, 0)));

		// Store the disassembled instructions, sorted by increasing addresses.
		TreeMap<Integer, String> disassembled = new TreeMap<Integer, String>();

		List<Integer> pendingAddresses = new LinkedList<Integer>();
		pendingAddresses.add(startAddress);
		while (!pendingAddresses.isEmpty()) {
			int pc = pendingAddresses.remove(0);
			boolean thumbMode = hasBit(pc, 0);
			pc = clearBit(pc, 0);

			boolean endOfBlock = false;
			while (!endOfBlock) {
				if (disassembled.containsKey(pc)) {
					// This address has already been disassembled
					break;
				}

				if (!ARMMemory.isAddressGood(pc)) {
					// Reached an invalid memory address
					break;
				}

				// Read the instruction in Thumb or ARM mode
				int insn;
				String insnString;
				ARMInstruction instr;
				int nextPc;
				if (thumbMode) {
					insn = mem.internalRead16(pc);
					insnString = String.format("%04X", insn);
					instr = ARMDecoder.thumbInstruction(insn);
					nextPc = pc + 2;
				} else {
					insn = mem.internalRead32(pc);
					insnString = String.format("%08X", insn);
					instr = ARMDecoder.instruction(insn);
					nextPc = pc + 4;
				}

				// Store the disassembled instruction
				String additionalInfo = getAdditionalInfo(pc, insn, instr, thumbMode);
				String disasm = String.format("0x%08X - [0x%s] - %s%s", pc, insnString, instr.disasm(pc, insn), additionalInfo == null ? "" : additionalInfo);
				disassembled.put(pc, disasm);

				// If this instruction is branching,
				// add the branched address to the pending addresses
				checkBranch(pendingAddresses, pc, insn, instr, thumbMode);
				// Verify if this instruction is the end of this block
				endOfBlock = isEndOfBlock(pc, insn, instr, thumbMode);
				pc = nextPc;
			}
		}

		// Log the disassembled function by increasing addresses
		// with branching information in front of each line.
		for (Integer pc : disassembled.keySet()) {
			char branchingTarget = branchingTargets.contains(pc) ? '>' : ' ';

			char branchingFlag;
			Integer branchTo = branchingTo.get(pc);
			if (branchTo == null) {
				// Not branching
				branchingFlag = ' ';
			} else if (!disassembled.containsKey(branchTo)) {
				// Branching out of this function
				branchingFlag = '<';
			} else if (branchTo.intValue() <= pc.intValue()) {
				// Branching backwards
				branchingFlag = '^';
			} else {
				// Branching forwards
				branchingFlag = 'v';
			}

			log(String.format("%c%c %s", branchingFlag, branchingTarget, disassembled.get(pc)));
		}

		// Remember that this function has been disassembled
		disassembledFunctions.add(startAddress);
	}

	private void disasmAll() {
		while (!pendingFunctions.isEmpty()) {
			int addr = pendingFunctions.remove(0);
			disasmFunction(addr);
		}
	}

	public void disasm(int addr) {
		pendingFunctions.add(addr);
		branchingTargets.add(clearBit(addr, 0));
		disasmAll();
	}
}
