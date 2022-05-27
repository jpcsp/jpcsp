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

import jpcsp.Processor;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.CodeBlock;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class CachedInterpreter extends AbstractNativeCodeSequence {
	private static Instruction[] instructions;
	private static int[] opcodes;

	private static class UpdateOpcodesAction implements IAction {
		private int address;
		private int length;
		private int[] opcodes;
		private int offset;

		public UpdateOpcodesAction(int address, int length, int[] opcodes, int offset) {
			this.address = address;
			this.length = length;
			this.opcodes = opcodes;
			this.offset = offset;
		}

		@Override
		public void execute() {
			// Re-read the opcodes that have been updated by the application
			Utilities.readInt32(address, length, opcodes, offset);
		}
	}

	/*
	 * This method is interpreting a code sequence but caching the decoded instructions.
	 * No jumps or branches are allowed in the code sequence.
	 */
    static public void call(int numberInstructions, int codeBlockContextSize) {
		// First time being called?
    	if (instructions == null) {
    		final int startAddress = getPc();

    		// Read the opcodes
    		opcodes = Utilities.readInt32(startAddress, numberInstructions << 2);

    		// Decode the opcodes into instructions
    		instructions = new Instruction[numberInstructions];
    		for (int i = 0; i < numberInstructions; i++) {
    			instructions[i] = Decoder.instruction(opcodes[i]);
    		}

    		// Search for the code block including this sequence (search backwards)
    		CodeBlock codeBlock = null;
    		for (int i = 0; i < codeBlockContextSize && codeBlock == null; i++) {
    			codeBlock = RuntimeContext.getCodeBlock(startAddress - (i << 2));
    		}

    		// Define the action that need to be executed when the compiler has detected
    		// that the opcodes have been modified by the application.
    		// In that case, the "opcodes" array need to be updated.
    		if (codeBlock != null) {
    			codeBlock.setUpdateOpcodesAction(new UpdateOpcodesAction(startAddress, numberInstructions << 2, opcodes, 0));
    		} else {
    			log.error(String.format("CachedInterpreter: could not find the CodeBlock 0x%08X", startAddress));
    		}
    	}

    	// Interpret the decoded instructions
    	final Processor processor = getProcessor();
    	for (int i = 0; i < numberInstructions; i++) {
    		instructions[i].interpret(processor, opcodes[i]);
    	}
    }
}
