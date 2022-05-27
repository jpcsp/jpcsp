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
package jpcsp.Allegrex.compiler.nativeCode;

import org.objectweb.asm.MethodVisitor;

import jpcsp.Memory;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.CodeInstruction;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Allegrex.compiler.CompilerContext;

/**
 * @author gid15
 *
 */
public class NativeCodeInstruction extends CodeInstruction {
	private NativeCodeSequence nativeCodeSequence;
	private int flags = 0;

	public NativeCodeInstruction(int address, NativeCodeSequence nativeCodeSequence) {
		this.nativeCodeSequence = nativeCodeSequence;
		this.address = address;
		init();
	}

	private void init() {
		if (nativeCodeSequence.hasBranchInstruction()) {
			// Handle like a branch/jump instruction
			flags |= Instruction.FLAG_CANNOT_BE_SPLIT;
			setBranching(true);

			int branchInstructionAddress = getAddress() + (nativeCodeSequence.getBranchInstruction() << 2);

	    	int branchOpcode = Memory.getInstance().read32(branchInstructionAddress);
	    	Instruction branchInsn = Decoder.instruction(branchOpcode);
	    	int npc = branchInstructionAddress + 4;
	    	if (branchInsn.hasFlags(Instruction.FLAG_IS_BRANCHING)) {
	    		setBranchingTo(Compiler.branchTarget(npc, branchOpcode));
	    	} else if (branchInsn.hasFlags(Instruction.FLAG_IS_JUMPING)) {
	    		setBranchingTo(Compiler.jumpTarget(npc, branchOpcode));
	    	} else {
	    		Compiler.log.error(String.format("Incorrect Branch Instruction at 0x%08X - %s", branchInstructionAddress, branchInsn.disasm(branchInstructionAddress, branchOpcode)));
	    	}
		}

		if (nativeCodeSequence.isReturning()) {
			// Handle like a "JR $ra" instruction
			flags |= Instruction.FLAG_CANNOT_BE_SPLIT;
		}
	}

	public NativeCodeSequence getNativeCodeSequence() {
		return nativeCodeSequence;
	}

	@Override
    public boolean hasFlags(int testFlags) {
        return (flags & testFlags) == testFlags;
    }

	@Override
    public void compile(CompilerContext context, MethodVisitor mv) {
        startCompile(context, mv);
        context.compileNativeCodeSequence(nativeCodeSequence, this);
	}

	@Override
	public int getEndAddress() {
		return getAddress() + ((nativeCodeSequence.getNumOpcodes() - 1) << 2);
	}

	@Override
	public int getLength() {
		return nativeCodeSequence.getNumOpcodes();
	}

	@Override
    public String toString() {
        return String.format("0x%X - %s", getAddress(), nativeCodeSequence.toString());
    }
}
