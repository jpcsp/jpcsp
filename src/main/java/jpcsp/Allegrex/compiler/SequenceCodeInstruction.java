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

import org.objectweb.asm.MethodVisitor;

public class SequenceCodeInstruction extends CodeInstruction {
    private CodeSequence codeSequence;

    public SequenceCodeInstruction(CodeSequence codeSequence) {
        this.codeSequence = codeSequence;
        setAddress(codeSequence.getStartAddress());

        CodeInstruction firstInstruction = codeSequence.getInstructions().get(0);
        setBranchTarget(firstInstruction.isBranchTarget());
        setBranching(firstInstruction.isBranching());
        setBranchingTo(firstInstruction.getBranchingTo());
    }

    public CodeSequence getCodeSequence() {
        return codeSequence;
    }

	@Override
	public int getEndAddress() {
		return codeSequence.getEndAddress();
	}

	@Override
	public int getLength() {
		return codeSequence.getLength();
	}

	@Override
    public void compile(CompilerContext context, MethodVisitor mv) {
        startCompile(context, mv);

        context.visitCall(context.getCodeBlock().getStartAddress(), getMethodName(context));
    }

	@Override
	public boolean hasFlags(int flags) {
		return false;
	}

    public String getMethodName(CompilerContext context) {
        return context.getStaticExecMethodName() + Integer.toHexString(codeSequence.getStartAddress());
    }

    @Override
    public String toString() {
        return String.format("0x%X - %s", codeSequence.getStartAddress(), codeSequence.toString());
    }
}
