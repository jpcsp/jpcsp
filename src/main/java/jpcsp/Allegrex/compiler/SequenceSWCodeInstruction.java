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
package jpcsp.Allegrex.compiler;

import jpcsp.Allegrex.Common;

import org.objectweb.asm.MethodVisitor;

/**
 * @author gid15
 *
 */
public class SequenceSWCodeInstruction extends CodeInstruction {
	protected int baseRegister;
	protected int[] offsets;
	protected int[] registers;

	public SequenceSWCodeInstruction(int baseRegister, int[] offsets, int[] registers) {
		this.baseRegister = baseRegister;
		this.offsets = offsets;
		this.registers = registers;
	}

	@Override
	public void compile(CompilerContext context, MethodVisitor mv) {
		startCompile(context, mv);
		compileInstruction(context);
		context.endInstruction();
	}

	protected String getInstructionName() {
		return "sw";
	}

	protected void compileInstruction(CompilerContext context) {
		context.compileSWsequence(baseRegister, offsets, registers);
	}

	@Override
	public boolean hasFlags(int flags) {
		return false;
	}

	@Override
	public String disasm(int address, int opcode) {
		StringBuilder result = new StringBuilder(String.format("%-10s ", getInstructionName()));

		for (int i = 0; i < registers.length; i++) {
			if (i > 0) {
				result.append("/");
			}
			result.append(Common.gprNames[registers[i]]);
		}
		result.append(", ");
		for (int i = 0; i < offsets.length; i++) {
			if (i > 0) {
				result.append("/");
			}
			result.append(offsets[i]);
		}
		result.append("(");
		result.append(Common.gprNames[baseRegister]);
		result.append(")");

		return result.toString();
	}
}
