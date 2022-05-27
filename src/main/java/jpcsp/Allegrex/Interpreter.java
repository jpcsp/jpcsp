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
package jpcsp.Allegrex;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Processor;
import jpcsp.Allegrex.Common.Instruction;

public class Interpreter {
	private Processor processor;
	private static final int DUMMY_ADDRESS = -1;
	private Logger log;

	public Interpreter(Processor processor) {
		this.processor = processor;
		log = processor.getLogger();
	}

	public void run(int address) {
		processor.cpu.pc = address;
		processor.cpu._ra = DUMMY_ADDRESS;

		final boolean isTraceEnabled = log.isTraceEnabled();

		Processor previousProcessor = Emulator.setProcessor(processor);

		while (processor.cpu.pc != DUMMY_ADDRESS) {
			processor.interpret();

			if (isTraceEnabled) {
				Instruction instruction = processor.getInstruction();
				if (instruction.hasFlags(Instruction.FLAG_WRITES_RT)) {
					int rt = (processor.getOpcode() >> 16) & 31;
					log.trace(String.format("%s = 0x%08X", Common.gprNames[rt], processor.cpu.getRegister(rt)));
				}
				if (instruction.hasFlags(Instruction.FLAG_WRITES_RD)) {
					int rd = (processor.getOpcode() >> 11) & 31;
					log.trace(String.format("%s = 0x%08X", Common.gprNames[rd], processor.cpu.getRegister(rd)));
				}
				if (instruction.hasFlags(Instruction.FLAG_HAS_DELAY_SLOT)) {
					Instruction delaySlotInstruction = processor.getDelaySlotInstruction();
					if (delaySlotInstruction.hasFlags(Instruction.FLAG_WRITES_RT)) {
						int rt = (processor.getDelaySlotOpcode() >> 16) & 31;
						log.trace(String.format("%s = 0x%08X", Common.gprNames[rt], processor.cpu.getRegister(rt)));
					}
					if (delaySlotInstruction.hasFlags(Instruction.FLAG_WRITES_RD)) {
						int rd = (processor.getDelaySlotOpcode() >> 11) & 31;
						log.trace(String.format("%s = 0x%08X", Common.gprNames[rd], processor.cpu.getRegister(rd)));
					}
				}
			}
		}

		Emulator.setProcessor(previousProcessor);
	}
}
