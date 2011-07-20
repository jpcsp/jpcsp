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

/**
 * @author gid15
 *
 */
public class RecompileExecutable implements IExecutable {
	private CodeBlock codeBlock;

	public RecompileExecutable(CodeBlock codeBlock) {
		this.codeBlock = codeBlock;
	}

	/* (non-Javadoc)
	 * @see jpcsp.Allegrex.compiler.IExecutable#exec(int, int, boolean)
	 * 
	 * Recompile the codeBlock and set its runtime executable to the recompiled
	 * executable.
	 */
	@Override
	public int exec(int returnAddress, int alternativeReturnAddress, boolean isJump) throws Exception {
		// Recompile the codeBlock
		int newInstanceIndex = codeBlock.getNewInstanceIndex();
		IExecutable executable = Compiler.getInstance().compile(codeBlock.getStartAddress(), newInstanceIndex);

		// Set the executable used at runtime to the recompiled executable.
		codeBlock.getExecutable().setExecutable(executable);

		// Execute the recompiled executable
		return executable.exec(returnAddress, alternativeReturnAddress, isJump);
	}

	@Override
	public void setExecutable(IExecutable e) {
		// Nothing to do
	}
}
