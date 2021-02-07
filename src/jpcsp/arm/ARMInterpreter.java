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
import static jpcsp.util.Utilities.hasBit;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.Emulator;

/**
 * @author gid15
 *
 */
public class ARMInterpreter {
	public static Logger log = ARMProcessor.log;
	private ARMProcessor processor;
	private final Map<Integer, IARMHLECall> hleCalls = new HashMap<Integer, IARMHLECall>();
	public static final int PC_END_RUN = 0xFFFFFFFC;
	private boolean exitInterpreter;
	private boolean inInterpreter;

	public ARMInterpreter(ARMProcessor processor) {
		this.processor = processor;
		processor.setInterpreter(this);

		installHLECall(PC_END_RUN, 0, null);
	}

	public void run() {
		inInterpreter = true;
		while (!Emulator.pause && !exitInterpreter && !processor.isNextInstructionPc(PC_END_RUN)) {
			processor.interpret();
		}
		inInterpreter = false;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Exiting ARMInterpreter loop at 0x%08X", processor.getNextInstructionPc()));
		}
		exitInterpreter = false;
	}

	public void exitInterpreter() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Request to exit ARMInterpreter inInterpreter=%b", inInterpreter));
		}

		if (inInterpreter) {
			exitInterpreter = true;
		}
	}

	public void disasm(int addr, int length) {
		boolean thumbMode = hasBit(addr, 0);
		if (thumbMode) {
			int pc = clearBit(addr, 0);
			log.info(String.format("Disassembling 0x%08X-0x%08X", pc, pc + length - 2));
			for (int i = 0; i < length; i += 2, pc += 2) {
				int insn = processor.mem.internalRead16(pc);
				ARMInstruction instruction = ARMDecoder.thumbInstruction(insn);
				log.info(String.format("0x%08X: [0x%04X] - %s", pc, insn, instruction.disasm(pc, insn)));
			}
		} else {
			int pc = clearFlag(addr, 0x3);
			log.info(String.format("Disassembling 0x%08X-0x%08X", pc, pc + length - 4));
			for (int i = 0; i < length; i += 4, pc += 4) {
				int insn = processor.mem.internalRead32(pc);
				ARMInstruction instruction = ARMDecoder.instruction(insn);
				log.info(String.format("0x%08X: [0x%08X] - %s", pc, insn, instruction.disasm(pc, insn)));
			}
		}
	}

	public boolean hasHLECall(int addr) {
		return hleCalls.containsKey(addr);
	}

	public IARMHLECall getHLECall(int addr) {
		return hleCalls.get(addr);
	}

	public IARMHLECall getHLECall(int addr, int imm) {
		return getHLECall(addr);
	}

	public boolean interpretHLE(int addr, int imm) {
		IARMHLECall hleCall = getHLECall(addr, imm);
		if (hleCall == null) {
			return false;
		}

		hleCall.call(processor, imm);

		return true;
	}

	public void registerHLECall(int addr, IARMHLECall hleCall) {
		hleCalls.put(clearBit(addr, 0), hleCall);
	}

	public void installHLECall(int addr, int imm, IARMHLECall hleCall) {
		if (hasBit(addr, 0)) {
			addr = clearBit(addr, 0);
			processor.mem.write16(addr, (short) (0xBE00 | (imm & 0x00FF)));
		} else {
			processor.mem.write32(addr, 0xE1200070 | ((imm & 0xFFF0) << 4) | (imm & 0x000F));
		}
		registerHLECall(addr, hleCall);
	}

	public void delayHLE(int count) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("delayHLE count=0x%X", count));
		}
		// Just ignore delay loops
	}
}
