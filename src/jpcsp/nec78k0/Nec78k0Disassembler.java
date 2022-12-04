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

import static jpcsp.nec78k0.Nec78k0Instructions.BC;
import static jpcsp.nec78k0.Nec78k0Instructions.BF_A_addr;
import static jpcsp.nec78k0.Nec78k0Instructions.BF_saddr;
import static jpcsp.nec78k0.Nec78k0Instructions.BF_sfr;
import static jpcsp.nec78k0.Nec78k0Instructions.BNC;
import static jpcsp.nec78k0.Nec78k0Instructions.BNZ;
import static jpcsp.nec78k0.Nec78k0Instructions.BR_AX;
import static jpcsp.nec78k0.Nec78k0Instructions.BR_jdisp;
import static jpcsp.nec78k0.Nec78k0Instructions.BR_word;
import static jpcsp.nec78k0.Nec78k0Instructions.BTCLR_saddr;
import static jpcsp.nec78k0.Nec78k0Instructions.BT_A_addr;
import static jpcsp.nec78k0.Nec78k0Instructions.BT_saddr;
import static jpcsp.nec78k0.Nec78k0Instructions.BT_sfr;
import static jpcsp.nec78k0.Nec78k0Instructions.BZ;
import static jpcsp.nec78k0.Nec78k0Instructions.CALL;
import static jpcsp.nec78k0.Nec78k0Instructions.DBNZ_B;
import static jpcsp.nec78k0.Nec78k0Instructions.DBNZ_C;
import static jpcsp.nec78k0.Nec78k0Instructions.DBNZ_saddr;
import static jpcsp.nec78k0.Nec78k0Instructions.RET;
import static jpcsp.nec78k0.Nec78k0Instructions.RETI;
import static jpcsp.nec78k0.Nec78k0Instructions.getCallAddressWord;
import static jpcsp.nec78k0.Nec78k0Instructions.getBranchAddressWord;
import static jpcsp.nec78k0.Nec78k0Instructions.getFunctionName;
import static jpcsp.util.Utilities.internalReadUnaligned32;

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
public class Nec78k0Disassembler {
	private final Logger log;
	private final Level level;
	private final Nec78k0Processor processor;
	private final List<Integer> pendingFunctions = new LinkedList<Integer>();
	private final Set<Integer> disassembledFunctions = new HashSet<Integer>();
	private final Map<Integer, Integer> branchingTo = new HashMap<Integer, Integer>();
	private final Set<Integer> branchingTargets = new HashSet<Integer>();
	private final Map<Integer, String> labels = new HashMap<Integer, String>();
	private final Set<Integer> dataAddresses = new HashSet<Integer>();
	public  final Set<Integer> disassembledAddresses = new HashSet<Integer>();

	public Nec78k0Disassembler(Logger log, Level level, Nec78k0Processor processor) {
		this.log = log;
		this.level = level;
		this.processor = processor;

		disassembledFunctions.add(0xFFFF);
	}

	private void log(String s) {
		log.log(level, s);
	}

	private boolean isEndOfBlock(int addr, int insn, Nec78k0Instruction instr) {
		if (instr == RET || instr == RETI) {
			return true;
		} else if (instr == BR_AX || instr == BR_jdisp || instr == BR_word) {
			return true;
		}

		return false;
	}

	private int getJdisp(int addr, int insn, Nec78k0Instruction instr) {
		return Nec78k0Instructions.getJdisp(addr, insn, instr.getInstructionSize());
	}

	private void addJump(List<Integer> list, int addr, int jumpTo) {
		list.add(jumpTo);
		branchingTo.put(addr, jumpTo);
		branchingTargets.add(jumpTo);
	}

	private void addJumpToFunction(int addr, int jumpTo) {
		if (!pendingFunctions.contains(jumpTo)) {
			pendingFunctions.add(jumpTo);
		}
		branchingTo.put(addr, jumpTo);
		branchingTargets.add(jumpTo);
	}

	private void checkBranch(List<Integer> pendingAddresses, int addr, int insn, Nec78k0Instruction instr) {
		if (instr == BR_word) {
			int jumpTo = getBranchAddressWord(insn, addr);
			addJump(pendingAddresses, addr, jumpTo);
		} else if (instr == BR_jdisp || instr == BZ || instr == BNZ || instr == BNC || instr == BC || instr == BF_A_addr || instr == BF_saddr || instr == BF_sfr || instr == BT_A_addr || instr == BT_saddr || instr == BT_sfr || instr == DBNZ_B || instr == DBNZ_C || instr == DBNZ_saddr || instr == BTCLR_saddr) {
			int jumpTo = getJdisp(addr, insn, instr);
			addJump(pendingAddresses, addr, jumpTo);
		} else if (instr == CALL) {
			int jumpTo = getCallAddressWord(insn, addr);
			addJumpToFunction(addr, jumpTo);
		}
	}

	private void disasmFunction(int startAddress) {
		if (disassembledFunctions.contains(startAddress)) {
			// Already disassembled
			return;
		}

		int check = internalReadUnaligned32(processor.mem, startAddress);
		if (check == 0 || check == 0xFFFFFFFF) {
			// This function is starting with 4 NOP's, something is wrong...
			log(String.format("Skipping Function 0x%04X", startAddress));
			return;
		}

		log(String.format("Disassembling Function %s", getFunctionName(startAddress)));

		// Store the disassembled instructions, sorted by increasing addresses.
		TreeMap<Integer, String> disassembled = new TreeMap<Integer, String>();

		List<Integer> pendingAddresses = new LinkedList<Integer>();
		pendingAddresses.add(startAddress);
		while (!pendingAddresses.isEmpty()) {
			int pc = pendingAddresses.remove(0);

			boolean endOfBlock = false;
			while (!endOfBlock) {
				if (disassembled.containsKey(pc)) {
					// This address has already been disassembled
					break;
				}

				if (dataAddresses.contains(pc)) {
					// Somehow, we reached an address which has been identified as pure data
					disassembled.put(pc, String.format("0x%04X - data", pc));
					break;
				}

				if (!Nec78k0Memory.isAddressGood(pc)) {
					// Reached an invalid memory address
					break;
				}

				// Read the instruction in Thumb or ARM mode
				int insn = processor.startNewInstruction(pc);
				Nec78k0Instruction instr = Nec78k0Decoder.instruction(processor, insn);
				insn = processor.getCurrentInstructionOpcode();
				int nextPc = processor.getNextInstructionPc();

				// Store the disassembled instruction
				String opcode;
				String alignment;
				switch (instr.getInstructionSize()) {
					case 1: opcode = String.format("0x%02X", insn); alignment = "      "; break;
					case 2: opcode = String.format("0x%04X", insn); alignment = "    "; break;
					case 3: opcode = String.format("0x%06X", insn); alignment = "  "; break;
					case 4: opcode = String.format("0x%08X", insn); alignment = ""; break;
					default: opcode = String.format("0x%X", insn); alignment = ""; break;
				}
				String disasm = String.format("0x%04X - [%s]%s - %s", pc, opcode, alignment, instr.disasm(pc, insn));
				disassembled.put(pc, disasm);

				int size = instr.getInstructionSize();
				for (int i = 0; i < size; i++) {
					disassembledAddresses.add(pc + i);
				}

				// If this instruction is branching,
				// add the branched address to the pending addresses
				checkBranch(pendingAddresses, pc, insn, instr);
				// Verify if this instruction is the end of this block
				endOfBlock = isEndOfBlock(pc, insn, instr);
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
		int savedPc = processor.getPc();

		while (!pendingFunctions.isEmpty()) {
			int addr = pendingFunctions.remove(0);
			disasmFunction(addr);
		}

		processor.setPc(savedPc);
	}

	public boolean isAlreadyDisassembled(int addr) {
		return disassembledFunctions.contains(addr);
	}

	public void disasm(int addr) {
		pendingFunctions.add(addr);
		branchingTargets.add(addr);
		disasmAll();
	}

	public void setData(int addr, int size) {
		for (int i = 0; i < size; i++) {
			disassembledAddresses.add(addr + i);
		}
	}

	public void setDataRange(int addrStart, int addrEnd) {
		setData(addrStart, addrEnd - addrStart + 1);
	}
}
