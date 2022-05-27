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

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.Allegrex.Common.Instruction;

/**
 * @author gid15
 *
 */
public abstract class ARMInstruction extends Instruction {
	public static Logger log = ARMProcessor.log;
	public abstract void interpret(ARMProcessor processor, int insn);

	@Override
	public void interpret(Processor processor, int insn) {
		log.error("Unsupported for ARMInstruction");
	}

	protected void checkHLECall(ARMProcessor processor) {
		IARMHLECall hleCall = processor.interpreter.getHLECall(processor.getNextInstructionPc());
		if (hleCall != null) {
			hleCall.call(processor, 0);
		}
	}

	@Override
	public ARMInstruction instance(int insn) {
		return this;
	}

	@Override
    public String name() {
        return getClass().getName();
    }

    @Override
    public final String category() {
        return "Unused";
    }
}
