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

import jpcsp.Processor;
import jpcsp.Allegrex.Common.Instruction;

/**
 * @author gid15
 *
 */
public abstract class Nec78k0Instruction extends Instruction {
	public abstract void interpret(Nec78k0Processor processor, int insn);
	public abstract int getInstructionSize();

	public Nec78k0Instruction instance(Nec78k0Processor processor, int insn) {
		return this;
	}

	@Override
	public void interpret(Processor processor, int insn) {
		log.error("Unsupported for Nec78k0Instruction");
	}

	protected boolean handleHLECall(Nec78k0Processor processor, int addr, int insn) {
		INec78k0HLECall hleCall = processor.interpreter.getHLECall(addr);
		if (hleCall == null) {
			return false;
		}

		hleCall.call(processor, insn);

		return true;
	}

	@Override
	public Nec78k0Instruction instance(int insn) {
		log.error("Unsupported for Nec78k0Instruction");
		return null;
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
