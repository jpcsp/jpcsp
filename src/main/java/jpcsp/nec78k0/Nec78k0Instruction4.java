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

/**
 * @author gid15
 *
 */
public abstract class Nec78k0Instruction4 extends Nec78k0Instruction {
	@Override
	public int getInstructionSize() {
		return 4;
	}

	@Override
	public Nec78k0Instruction instance(Nec78k0Processor processor, int insn) {
		insn = processor.getNextInstructionOpcode();
		insn = processor.getNextInstructionOpcode();
		insn = processor.getNextInstructionOpcode();
		return super.instance(processor, insn);
	}
}
