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

import org.objectweb.asm.Opcodes;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import jpcsp.ParameterReader;

/**
 * @author gid15
 *
 */
public class CompilerParameterReader extends ParameterReader {
	private ICompilerContext compilerContext;
	private boolean hasErrorPointer = false;
	private int currentParameterIndex = 0;
	private int currentStackSize = 0;

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
			throw(new NotImplementedException());
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
		int count = currentStackSize + additionalCount;
		while (count >= 2) {
			compilerContext.getMethodVisitor().visitInsn(Opcodes.POP2);
			count -= 2;
		}

		while (count > 0) {
			compilerContext.getMethodVisitor().visitInsn(Opcodes.POP);
			count--;
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
		currentStackSize += size;
	}

	public void incrementCurrentStackSize() {
		incrementCurrentStackSize(1);
	}
}
