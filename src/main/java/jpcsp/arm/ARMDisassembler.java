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
import static jpcsp.arm.ARMInstructions.ADD_High_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Rd_Pc_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Reg_Thumb;
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
import static jpcsp.arm.ARMInstructions.CMP_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.LDR;
import static jpcsp.arm.ARMInstructions.LDRB_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.LDRH_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.LDR_Pc_Thumb;
import static jpcsp.arm.ARMInstructions.LSL_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.MOV_High_Thumb;
import static jpcsp.arm.ARMInstructions.POP_Thumb;
import static jpcsp.arm.ARMInstructions.SUB_Imm_Thumb;
import static jpcsp.arm.ARMProcessor.COND_AL;
import static jpcsp.arm.ARMProcessor.COND_CS;
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

import jpcsp.hardware.Wlan;

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
	private final Map<Integer, String> labels = new HashMap<Integer, String>();
	private final Set<Integer> dataAddresses = new HashSet<Integer>();
	private int switchNumber;
	private static final String validStringCharacters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz !\"$%&/()=?{[]}+*#',;.:-_@^<>|";

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
		return (insn >>> 28) == COND_AL;
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

	private int getThumbHighRegister(int insn) {
    	return (insn & 0x7) | ((insn >> 4) & 0x8);
	}

	private String getThumbRegisterName(int insn, int offset) {
		return ARMInstructions.getRegisterName(getThumbRegister(insn, offset));
	}

	private void addJump(List<Integer> list, int addr, int jumpTo) {
		list.add(jumpTo);
		jumpTo = clearBit(jumpTo, 0);
		branchingTo.put(addr, jumpTo);
		branchingTargets.add(jumpTo);
	}

	private void addJumpToFunction(int addr, int jumpTo) {
		pendingFunctions.add(jumpTo);
		jumpTo = clearBit(jumpTo, 0);
		branchingTo.put(addr, jumpTo);
		branchingTargets.add(jumpTo);
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

	private String getStringValue(int addr) {
		StringBuilder s = new StringBuilder();
		while (true) {
			int c = mem.internalRead8(addr++);
			if (c == 0) {
				break;
			}
			s.append((char) c);
		}

		return s.toString();
	}

	private boolean isValidStringChar(int addr) {
		int c = mem.internalRead8(addr);
		return validStringCharacters.indexOf(c) >= 0;
	}

	private boolean isStringValue(int addr) {
		for (int i = 0; i < 4; i++) {
			if (!isValidStringChar(addr + i)) {
				return false;
			}
		}

		return true;
	}

	private boolean isMACAddress(int addr) {
		byte[] macAddress = Wlan.getMacAddress();
		for (int i = 0; i < macAddress.length; i++) {
			if (macAddress[i] != (byte) mem.internalRead8(addr)) {
				return false;
			}
		}

		return true;
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
		} else if (instr == MOV_High_Thumb && getThumbHighRegister(insn) == REG_PC) {
    		// mov pc, rn
    		return true;
		} else if (instr == ADD_High_Thumb && getThumbHighRegister(insn) == REG_PC) {
			// add pc, rn
			return true;
		}

		return false;
	}

	private void checkBranch(List<Integer> pendingAddresses, int addr, int insn, ARMInstruction instr, boolean thumbMode) {
		if (instr == B) {
			int jumpTo = getJumpFromBranchOffset(addr, insn << 8 >> 6, thumbMode);
			addJump(pendingAddresses, addr, jumpTo);
		} else if (instr == BL) {
			int jumpTo = getJumpFromBranchOffset(addr, insn << 8 >> 6, thumbMode);
			addJumpToFunction(addr, jumpTo);
		} else if (isLdrFixedValue(instr, insn, false) && getRegister(insn, 12) == REG_PC) {
			int jumpTo = getLdrFixedValue(insn, addr + 8);
			addJump(pendingAddresses, addr, jumpTo);
		} else if (instr == BX) {
			if (getRegister(insn, 0) == REG_PC) {
				// bx pc
				addJump(pendingAddresses, addr, addr + 8);
			} else {
				// Check for sequence:
				//   ldr RN, [pc, #...]
				//   bx RN
				int previousInsn = mem.internalRead32(addr - 4);
				if (isLdrFixedValue(previousInsn, false) && getRegister(insn, 0) == getRegister(previousInsn, 12)) {
					int jumpTo = getLdrFixedValue(previousInsn, addr + 4);
					addJump(pendingAddresses, addr, jumpTo);
				}
			}
		} else if (instr == BX_Thumb) {
			if (getRegister(insn, 3) == REG_PC) {
				addJump(pendingAddresses, addr, addr + 4);
			} else {
				int previousInsn = mem.internalRead16(addr - 2);
				ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
				if (previousInstr == ADD_Rd_Pc_Thumb && getThumbRegister(insn, 3) == getThumbRegister(previousInsn, 8)) {
					int jumpTo = clearFlag(addr + 4, 0x3) + ((previousInsn & 0xFF) << 2);
					addJump(pendingAddresses, addr, jumpTo);
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
			addJump(pendingAddresses, addr, jumpTo);
		} else if (instr == B_Cond_Thumb) {
			int jumpTo = getJumpFromBranchOffset(addr, insn << 24 >> 23, thumbMode);
			addJump(pendingAddresses, addr, jumpTo);
		} else if (instr == MOV_High_Thumb && getThumbHighRegister(insn) == REG_PC) {
    		// Check for sequence:
    		//   ldr RM, [pc, #...]
    		//   mov pc, RM
    		int previousInsn = mem.internalRead16(addr - 2);
    		ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
    		if (previousInstr == LDR_Pc_Thumb && getThumbRegister(previousInsn, 8) == getRegister(insn, 3)) {
    			int jumpTo = getThumbLdrFixedValue(previousInsn, addr + 2);
    			addJump(pendingAddresses, addr, setBit(jumpTo, 0)); // Always jumping to Thumb code
    		}
		}
	}

	private String getAdditionalBranchInfo(int addr, boolean thumbMode) {
		int insn;
		ARMInstruction instr;
		if (thumbMode) {
			insn = mem.internalRead16(addr);
			instr = ARMDecoder.thumbInstruction(insn);
		} else {
			insn = mem.internalRead32(addr);
			instr = ARMDecoder.instruction(insn);
		}

		String additionalBranchInfo = "";

		if (instr == BKPT_Thumb) {
			IARMHLECall hleCall = interpreter.getHLECall(addr, insn & 0xFF);
			if (hleCall != null) {
				additionalBranchInfo = String.format(" (%s)", hleCall);
			}
		} else if (instr == BKPT) {
			IARMHLECall hleCall = interpreter.getHLECall(addr, ((insn >> 4) & 0xFFF0) | (insn & 0xF));
			if (hleCall != null) {
				additionalBranchInfo = String.format(" (%s)", hleCall);
			}
		} else if (interpreter.hasHLECall(addr)) {
			IARMHLECall hleCall = interpreter.getHLECall(addr);
			additionalBranchInfo = String.format(" (%s)", hleCall);
		}

		return additionalBranchInfo;
	}

	private String getAdditionalBranchInfo(int addr) {
		return getAdditionalBranchInfo(addr, hasBit(addr, 0));
	}

	private String getAdditionalValueInfo(int addr) {
		String additionalValueInfo = "";

		if (addr != 0 && ARMMemory.isAddressInRAM(addr)) {
			if (isStringValue(addr)) {
				additionalValueInfo = String.format(" (\"%s\")", getStringValue(addr));
			} else if (isMACAddress(addr)) {
				additionalValueInfo = String.format(" (MAC Address %02x:%02x:%02x:%02x:%02x)", mem.internalRead8(addr), mem.internalRead8(addr + 1), mem.internalRead8(addr + 2), mem.internalRead8(addr + 3), mem.internalRead8(addr + 4), mem.internalRead8(addr + 5));
			}
		}

		return additionalValueInfo;
	}

	private String getAdditionalInfo(int addr, int insn, ARMInstruction instr, boolean thumbMode) {
		String additionalInfo = null;

		if (isLdrFixedValue(instr, insn, true)) {
			int value = getLdrFixedValue(insn, addr + 8);
			if (hasFlag(insn, 0x00400000)) {
				additionalInfo = String.format(" <=> mov %s, #0x%02X", getRegisterName(insn, 12), value);
			} else {
				additionalInfo = String.format(" <=> mov %s, #0x%08X%s", getRegisterName(insn, 12), value, getAdditionalValueInfo(value));
			}
		} else if (instr == LDR_Pc_Thumb) {
			int value = getThumbLdrFixedValue(insn, addr + 4);
			additionalInfo = String.format(" <=> mov %s, #0x%08X%s", getThumbRegisterName(insn, 8), value, getAdditionalValueInfo(value));
		} else if (instr == BX) {
			if (getRegister(insn, 0) == REG_PC) {
				// bx pc
				additionalInfo = String.format(" <=> bx 0x%08X%s", addr + 8, getAdditionalBranchInfo(addr + 8));
			} else {
				// Check for sequence:
				//   ldr RN, [pc, #...]
				//   bx RN
				int previousInsn = mem.internalRead32(addr - 4);
				if (isLdrFixedValue(previousInsn, false) && getRegister(insn, 0) == getRegister(previousInsn, 12)) {
					int jumpTo = getLdrFixedValue(previousInsn, addr + 4);
					additionalInfo = String.format(" <=> bx 0x%08X%s", jumpTo, getAdditionalBranchInfo(jumpTo));
				}
			}
		} else if (instr == ADD && getRegister(insn, 16) == REG_PC && hasFlag(insn, 0x02000000)) {
			int imm8 = insn & 0xFF; // Unsigned 8 bit immediate value
			int immRot = (insn >> 8) & 0xF; // Rotate applied to imm8
			int value = rotateRight(imm8, immRot << 1);
			additionalInfo = String.format(" <=> movs %s, #0x%08X", getRegisterName(insn, 12), addr + 8 + value);
		} else if (instr == BX_Thumb) {
			if (getRegister(insn, 3) == REG_PC) {
				additionalInfo = String.format(" <=> bx 0x%08X%s", addr + 4, getAdditionalBranchInfo(addr + 4));
			} else {
				int previousInsn = mem.internalRead16(addr - 2);
				ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
				if (previousInstr == ADD_Rd_Pc_Thumb && getThumbRegister(insn, 3) == getThumbRegister(previousInsn, 8)) {
					int jumpTo = clearFlag(addr + 4, 0x3) + ((previousInsn & 0xFF) << 2);
					additionalInfo = String.format(" <=> bx 0x%08X%s", jumpTo, getAdditionalBranchInfo(jumpTo));
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
				additionalInfo = String.format(" <=> bl%s 0x%08X%s", instr == BLX_01_Thumb ? "x" : "", jumpTo, getAdditionalBranchInfo(jumpTo, instr == BL_11_Thumb));
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
		} else if (instr == MOV_High_Thumb && getThumbHighRegister(insn) == REG_PC) {
    		// Check for sequence:
    		//   ldr RM, [pc, #...]
    		//   mov pc, RM
    		int previousInsn = mem.internalRead16(addr - 2);
    		ARMInstruction previousInstr = ARMDecoder.thumbInstruction(previousInsn);
    		if (previousInstr == LDR_Pc_Thumb && getThumbRegister(previousInsn, 8) == getRegister(insn, 3)) {
    			int jumpTo = getThumbLdrFixedValue(previousInsn, addr + 2);
				additionalInfo = String.format(" <=> b 0x%08X%s", clearBit(jumpTo, 0), getAdditionalBranchInfo(jumpTo, true));
    		}
		}

		return additionalInfo;
	}

	private boolean isSwitchStart(List<Integer> pendingAddresses, int addr, int insn, ARMInstruction instr, boolean thumbMode) {
		// Search for sequence:
		// [ sub RN, #startValue ] optional
		// [ add RN, #-startValue ] optional
		//   cmp RN, #maxValue
		//   ...
		//   bcs defaultValueSwitch
		//   add RM, pc, #switchTable
		// [ add RM, RM, RN ] only when using ldrh
		//   ldrb/ldrh RM, [RM, RN]
		//   lsl RM, RM, #0x1
		//   add pc, RM
		if (instr == ADD_High_Thumb && getThumbHighRegister(insn) == REG_PC) {
			int registerJump = getRegister(insn, 3);
			int currentAddr = addr - 2;

			int insnLsl = mem.internalRead16(currentAddr);
			ARMInstruction instrLsl = ARMDecoder.thumbInstruction(insnLsl);
			currentAddr -= 2;

			int insnLdr = mem.internalRead16(currentAddr);
			ARMInstruction instrLdr = ARMDecoder.thumbInstruction(insnLdr);
			currentAddr -= 2;

			int insnAddBeforeLdrh = 0;
			ARMInstruction instrAddBeforeLdrh = null;
			if (instrLdr == LDRH_Reg_Thumb) {
				insnAddBeforeLdrh = mem.internalRead16(currentAddr);
				instrAddBeforeLdrh = ARMDecoder.thumbInstruction(insnAddBeforeLdrh);
				currentAddr -= 2;
			}

			int pcAdd = currentAddr;
			int insnAdd = mem.internalRead16(pcAdd);
			ARMInstruction instrAdd = ARMDecoder.thumbInstruction(insnAdd);
			currentAddr -= 2;

			int pcBranch = currentAddr;
			int insnBranch = mem.internalRead16(pcBranch);
			ARMInstruction instrBranch = ARMDecoder.thumbInstruction(insnBranch);
			currentAddr -= 2;

			int insnCmp = 0;
			ARMInstruction instrCmp = null;
			for (int i = 0; i < 5; i++) {
				insnCmp = mem.internalRead16(currentAddr);
				instrCmp = ARMDecoder.thumbInstruction(insnCmp);
				currentAddr -= 2;
				if (instrCmp == CMP_Imm_Thumb) {
					break;
				}
			}

			int registerSwitchValue = getThumbRegister(insnCmp, 8);

			int insnAddSub = mem.internalRead16(currentAddr);
			ARMInstruction instrAddSub = ARMDecoder.thumbInstruction(insnAddSub);
			currentAddr -= 2;

			int switchStartValue = 0;
			if ((instrAddSub == SUB_Imm_Thumb || instrAddSub == ADD_Imm_Thumb) && getThumbRegister(insnAddSub, 8) == registerSwitchValue) {
				switchStartValue = insnAddSub & 0xFF;
				if (instrAddSub == ADD_Imm_Thumb) {
					switchStartValue = -switchStartValue;
				}
			}

			if (instrLsl == LSL_Imm_Thumb && getThumbRegister(insnLsl, 0) == registerJump && getThumbRegister(insnLsl, 3) == registerJump &&
			    (instrLdr == LDRB_Reg_Thumb || instrLdr == LDRH_Reg_Thumb) && getThumbRegister(insnLdr, 0) == registerJump && getThumbRegister(insnLdr, 3) == registerJump && getThumbRegister(insnLdr, 6) == registerSwitchValue &&
			    (instrAddBeforeLdrh == null || (instrAddBeforeLdrh == ADD_Reg_Thumb && getThumbRegister(insnAddBeforeLdrh, 0) == registerJump && getThumbRegister(insnAddBeforeLdrh, 3) == registerJump && getThumbRegister(insnAddBeforeLdrh, 6) == registerSwitchValue)) &&
			    instrAdd == ADD_Rd_Pc_Thumb && getThumbRegister(insnAdd, 8) == registerJump &&
			    instrBranch == B_Cond_Thumb && ((insnBranch >> 8) & 0xF) == COND_CS &&
			    instrCmp == CMP_Imm_Thumb && getThumbRegister(insnCmp, 8) == registerSwitchValue) {
				int switchTableAddr = pcAdd + 4 + ((insnAdd & 0xFF) << 2);
				int switchMaxValue = insnCmp & 0xFF;
				int jumpSize = instrLdr == LDRB_Reg_Thumb ? 1 : 2;
				int lslShift = (insnLsl >> 6) & 0x1F;
				int defaultJumpTo = clearBit(getJumpFromBranchOffset(pcBranch, insnBranch << 24 >> 23, thumbMode), 0);

				switchNumber++;

				labels.put(addr, String.format("switch (%s) { // [0x%X..0x%X] Switch#%d", ARMInstructions.getRegisterName(registerSwitchValue), switchStartValue, switchStartValue + switchMaxValue - 1, switchNumber));

				if (log.isDebugEnabled()) {
					log.debug(String.format("Found switch instruction at 0x%08X", addr));
					log.debug(String.format("switch (%s) { // [0x%X..0x%X]", ARMInstructions.getRegisterName(registerSwitchValue), switchStartValue, switchStartValue + switchMaxValue - 1));
				}

				Map<Integer, List<Integer>> switches = new HashMap<Integer, List<Integer>>();
				for (int i = 0, switchTableElementAddr = switchTableAddr; i < switchMaxValue; i++, switchTableElementAddr += jumpSize) {
					int switchTableElement = jumpSize == 1 ? mem.internalRead8(switchTableElementAddr) : mem.internalRead16(switchTableElementAddr);
					int switchJumpTo = addr + 4 + (switchTableElement << lslShift);

					dataAddresses.add(switchTableElementAddr);

					if (switchJumpTo != defaultJumpTo) {
						List<Integer> switchesAtAddress = switches.get(switchJumpTo);
						if (switchesAtAddress == null) {
							switchesAtAddress = new LinkedList<Integer>();
							switches.put(switchJumpTo, switchesAtAddress);
						}
						switchesAtAddress.add(switchStartValue + i);

						if (log.isDebugEnabled()) {
							log.debug(String.format("    case 0x%X: 0x%08X", switchStartValue + i, switchJumpTo));
						}
					}
				}

				for (Integer switchJumpTo : switches.keySet()) {
					StringBuilder values = new StringBuilder();
					for (Integer switchValue : switches.get(switchJumpTo)) {
						if (values.length() > 0) {
							values.append(", ");
						}
						values.append(String.format("0x%X", switchValue));
					}
					labels.put(switchJumpTo, String.format("case %s: // Switch#%d", values, switchNumber));
					pendingAddresses.add(setBit(switchJumpTo.intValue(), 0));
				}

				labels.put(defaultJumpTo, String.format("default: // or end of switch for Switch#%d", switchNumber));
				pendingAddresses.add(setBit(defaultJumpTo, 0));

				if (log.isDebugEnabled()) {
					log.debug(String.format("    default %s: 0x%08X", switchMaxValue < 0x10 ? "" : switchMaxValue < 0x100 ? " " : "  ", defaultJumpTo));
					log.debug(String.format("} // end of switch"));
				}
			}
		}

		return false;
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
		switchNumber = 0;
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

				if (dataAddresses.contains(pc)) {
					// Somehow, we reached an address which has been identified as pure data
					disassembled.put(pc, String.format("0x%08X - data", pc));
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
				// Verify if we are starting a switch instruction
				isSwitchStart(pendingAddresses, pc, insn, instr, thumbMode);
				// Verify if this instruction is the end of this block
				endOfBlock = isEndOfBlock(pc, insn, instr, thumbMode);
				pc = nextPc;
			}
		}

		// Log the disassembled function by increasing addresses
		// with branching information in front of each line.
		for (Integer pc : disassembled.keySet()) {
			String label = labels.get(pc);
			if (label != null) {
				log(label);
			}

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

	public boolean isAlreadyDisassembled(int addr) {
		return disassembledFunctions.contains(addr);
	}

	public void disasm(int addr) {
		pendingFunctions.add(addr);
		branchingTargets.add(clearBit(addr, 0));
		disasmAll();
	}
}
