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

import static jpcsp.Allegrex.Common._ra;
import jpcsp.Allegrex.Common.Instruction;

/**
 * @author gid15
 *
 */
public class InterpretExecutable implements IExecutable {
	private CodeBlock codeBlock;
	private boolean isAnalyzed;
	private boolean isSimple;

	public InterpretExecutable(CodeBlock codeBlock) {
		this.codeBlock = codeBlock;
		isAnalyzed = false;
	}

	@Override
	public int exec() throws Exception {
		// Analyze at first call only
		if (!isAnalyzed) {
			isSimple = Compiler.getInstance().checkSimpleInterpretedCodeBlock(codeBlock);
			isAnalyzed = true;
		}

		int returnAddress;
		if (isSimple) {
			final Instruction[] insns = codeBlock.getInterpretedInstructions();
			final int[] opcodes = codeBlock.getInterpretedOpcodes();
			for (int i = 0; i < insns.length; i++) {
				insns[i].interpret(RuntimeContext.processor, opcodes[i]);
			}
			returnAddress = RuntimeContext.cpu.gpr[_ra];
		} else {
			returnAddress = RuntimeContext.executeInterpreter(codeBlock.getStartAddress());
		}

		return returnAddress;
	}

	@Override
	public void setExecutable(IExecutable e) {
		// Nothing to do
	}
}
