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

import static jpcsp.Allegrex.Common._sp;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import jpcsp.ParameterReader;

/**
 * @author gid15
 *
 */
public class CompilerParameterReader extends ParameterReader {
	final private ICompilerContext compilerContext;
	private boolean hasErrorPointer = false;
	private int currentParameterIndex = 0;
	private int currentStackPopIndex = 0;
	final private int[] currentStackPop = new int[10];

	public CompilerParameterReader(ICompilerContext compilerContext) {
		super(null, null);
		this.compilerContext = compilerContext;
	}

	private void loadParameterIntAt(int index) {
		if (index >= maxParameterInGprRegisters) {
			compilerContext.memRead32(_sp, (index - maxParameterInGprRegisters) << 2);
		} else {
			compilerContext.loadRegister(firstParameterInGpr + index);
		}
	}

	private void loadParameterFloatAt(int index) {
		if (index >= maxParameterInFprRegisters) {
			throw(new UnsupportedOperationException());
		}
		compilerContext.loadFRegister(firstParameterInFpr + index);
	}

	private void loadParameterLongAt(int index) {
		if ((index % 2) != 0) {
			throw(new RuntimeException("Parameter misalignment"));
		}
		loadParameterIntAt(index);
		compilerContext.getMethodVisitor().visitInsn(Opcodes.I2L);
		compilerContext.getMethodVisitor().visitLdcInsn(0xFFFFFFFFL);
		compilerContext.getMethodVisitor().visitInsn(Opcodes.LAND);
		loadParameterIntAt(index + 1);
		compilerContext.getMethodVisitor().visitInsn(Opcodes.I2L);
		compilerContext.loadImm(32);
		compilerContext.getMethodVisitor().visitInsn(Opcodes.LSHL);
		compilerContext.getMethodVisitor().visitInsn(Opcodes.LADD);
	}

	public void loadNextInt() {
		loadParameterIntAt(moveParameterIndex(1));
	}

	public void loadNextFloat() {
		loadParameterFloatAt(moveParameterIndexFloat(1));
	}

	public void loadNextLong() {
		loadParameterLongAt(moveParameterIndex(2));
	}

	public void popAllStack(int additionalCount) {
		final MethodVisitor mv = compilerContext.getMethodVisitor();

		while (additionalCount >= 2) {
			mv.visitInsn(Opcodes.POP2);
			additionalCount -= 2;
		}

		if (additionalCount > 0) {
			mv.visitInsn(Opcodes.POP);
		}

		for (int i = currentStackPopIndex - 1; i >= 0; i--) {
			mv.visitInsn(currentStackPop[i]);
		}
	}

	public boolean hasErrorPointer() {
		return hasErrorPointer;
	}

	public void setHasErrorPointer(boolean hasErrorPointer) {
		this.hasErrorPointer = hasErrorPointer;
	}

	public int getCurrentParameterIndex() {
		return currentParameterIndex;
	}

	public void incrementCurrentParameterIndex() {
		currentParameterIndex++;
	}

	public void incrementCurrentStackSize(int size) {
		if (size == 1 && currentStackPopIndex > 0 && currentStackPop[currentStackPopIndex - 1] == Opcodes.POP) {
			// Merge previous POP with this one into a POP2
			currentStackPop[currentStackPopIndex - 1] = Opcodes.POP2;
		} else {
			// When size == 2 (e.g. for a "long" value), do not merge with a previous POP,
			// use an own POP2 for this "long" value.
			// Otherwise, VerifyError would be raised with message
			// "Attempt to split long or double on the stack"
			while (size >= 2) {
				currentStackPop[currentStackPopIndex++] = Opcodes.POP2;
				size -= 2;
			}
			if (size > 0) {
				currentStackPop[currentStackPopIndex++] = Opcodes.POP;
			}
		}
	}

	public void incrementCurrentStackSize() {
		incrementCurrentStackSize(1);
	}
}
