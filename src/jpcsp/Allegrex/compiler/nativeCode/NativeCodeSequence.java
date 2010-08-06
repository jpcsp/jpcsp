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

/**
 * @author gid15
 *
 */
public class NativeCodeSequence {
	protected String name;
	protected int[] opcodes = null;
	private Class<INativeCodeSequence> nativeCodeSequenceClass;
	private int[] parameters = new int[0];
	private int branchInstruction = -1;
	private boolean isReturning = false;
	private boolean wholeCodeBlock = false;
	private String methodName = "call";

	public NativeCodeSequence(String name, Class<INativeCodeSequence> nativeCodeSequenceClass) {
		this.name = name;
		this.nativeCodeSequenceClass = nativeCodeSequenceClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addOpcode(int opcode) {
		if (opcodes == null) {
			opcodes = new int[1];
		} else {
			int[] newOpcodes = new int[opcodes.length + 1];
			System.arraycopy(opcodes, 0, newOpcodes, 0, opcodes.length);
			opcodes = newOpcodes;
		}

		opcodes[opcodes.length - 1] = opcode;
	}

	public int[] getOpcodes() {
		return opcodes;
	}

	public int getNumOpcodes() {
		return (opcodes == null ? 0 : opcodes.length);
	}

	public Class<INativeCodeSequence> getNativeCodeSequenceClass() {
		return nativeCodeSequenceClass;
	}

	public void setNativeCodeSequenceClass(Class<INativeCodeSequence> nativeCodeSequenceClass) {
		this.nativeCodeSequenceClass = nativeCodeSequenceClass;
	}

	public void setParameter(int parameter, int value) {
		if (parameter >= parameters.length) {
			int[] newParameters = new int[parameter + 1];
			System.arraycopy(parameters, 0, newParameters, 0, parameters.length);
			for (int i = parameters.length; i < parameter; i++) {
				newParameters[i] = 0;
			}
			parameters = newParameters;
		}

		parameters[parameter] = value;
	}

	public int getParameter(int parameter) {
		if (parameter >= parameters.length) {
			return 0;
		}

		return parameters[parameter];
	}

	public int getNumberParameters() {
		return parameters.length;
	}

	public int getBranchInstruction() {
		return branchInstruction;
	}

	public void setBranchInstruction(int branchInstruction) {
		this.branchInstruction = branchInstruction;
	}

	public boolean hasBranchInstruction() {
		return branchInstruction > 0;
	}

	public int getBranchInstructionAddressOffset() {
		return (getBranchInstruction() - 1) * 4;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append(name);

		result.append("[");
		for (int i = 0; opcodes != null && i < opcodes.length; i++) {
			if (i > 0) {
				result.append(",");
			}
			result.append(String.format("%08X", opcodes[i]));
		}
		result.append("]");

		result.append("(");
		for (int i = 0; i < getNumberParameters(); i++) {
			if (i > 0) {
				result.append(",");
			}
			result.append(getParameter(i));
		}
		result.append(")");

		return result.toString();
	}

	public boolean isReturning() {
		return isReturning;
	}

	public void setReturning(boolean isReturning) {
		this.isReturning = isReturning;
	}

	public boolean isWholeCodeBlock() {
		return wholeCodeBlock;
	}

	public void setWholeCodeBlock(boolean wholeCodeBlock) {
		this.wholeCodeBlock = wholeCodeBlock;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
}
