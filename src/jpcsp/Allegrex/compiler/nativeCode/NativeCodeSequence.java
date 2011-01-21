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

import jpcsp.Memory;

/**
 * @author gid15
 *
 */
public class NativeCodeSequence {
	protected String name;
	protected NativeOpcodeInfo[] opcodes = new NativeOpcodeInfo[0];
	private Class<INativeCodeSequence> nativeCodeSequenceClass;
	private ParameterInfo[] parameters = new ParameterInfo[0];
	private int branchInstruction = -1;
	private boolean isReturning = false;
	private boolean wholeCodeBlock = false;
	private String methodName = "call";

	private static class NativeOpcodeInfo {
		private int opcode;
		private int mask;
		private int notMask;
		private String label;
		private int maskedOpcode;

		public NativeOpcodeInfo(int opcode, int mask, String label) {
			this.opcode = opcode;
			this.mask = mask;
			this.label = label;
			maskedOpcode = opcode & mask;
			notMask = ~mask;
		}

		public boolean isMatching(int opcode) {
			return (opcode & mask) == maskedOpcode;
		}

		public int getOpcode() {
			return opcode;
		}

		public String getLabel() {
			return label;
		}

		public int getMask() {
			return mask;
		}

		public int getNotMask() {
			return notMask;
		}
	}

	private static class ParameterInfo {
		private int value;
		private boolean isLabelIndex;

		public ParameterInfo(int value, boolean isLabelIndex) {
			this.value = value;
			this.isLabelIndex = isLabelIndex;
		}

		public int getValue() {
			return value;
		}

		public int getValue(int address, NativeOpcodeInfo[] opcodes) {
			if (isLabelIndex && value >= 0 && value < opcodes.length) {
				int labelAddress = address + (value << 2);
				int targetOpcode = Memory.getInstance().read32(labelAddress);
				NativeOpcodeInfo opcode = opcodes[value];
				return targetOpcode & opcode.getNotMask();
			}

			return value;
		}
	}

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

	public void addOpcode(int opcode, int mask, String label) {
		NativeOpcodeInfo[] newOpcodes = new NativeOpcodeInfo[opcodes.length + 1];
		System.arraycopy(opcodes, 0, newOpcodes, 0, opcodes.length);
		opcodes = newOpcodes;

		opcodes[opcodes.length - 1] = new NativeOpcodeInfo(opcode, mask, label);
	}

	public int getFirstOpcode() {
		return opcodes[0].getOpcode();
	}

	public int getFirstOpcodeMask() {
		return opcodes[0].getMask();
	}

	public int getNumOpcodes() {
		return opcodes.length;
	}

	public Class<INativeCodeSequence> getNativeCodeSequenceClass() {
		return nativeCodeSequenceClass;
	}

	public void setNativeCodeSequenceClass(Class<INativeCodeSequence> nativeCodeSequenceClass) {
		this.nativeCodeSequenceClass = nativeCodeSequenceClass;
	}

	public int getLabelIndex(String label) {
		int value = -1;

		if (label == null) {
			return value;
		}

		for (int i = 0; i < opcodes.length; i++) {
			if (label.equalsIgnoreCase(opcodes[i].getLabel())) {
				value = i;
				break;
			}
		}

		return value;
	}

	public boolean isMatching(int opcodeIndex, int opcode) {
		return opcodes[opcodeIndex].isMatching(opcode);
	}

	public void setParameter(int parameter, int value, boolean isLabelIndex) {
		if (parameter >= parameters.length) {
			ParameterInfo[] newParameters = new ParameterInfo[parameter + 1];
			System.arraycopy(parameters, 0, newParameters, 0, parameters.length);
			for (int i = parameters.length; i < parameter; i++) {
				newParameters[i] = null;
			}
			parameters = newParameters;
		}

		parameters[parameter] = new ParameterInfo(value, isLabelIndex);
	}

	public int getParameterValue(int parameter, int address) {
		if (parameter >= parameters.length) {
			return 0;
		}

		ParameterInfo parameterInfo = parameters[parameter];
		if (address == 0) {
			return parameterInfo.getValue();
		}

		return parameterInfo.getValue(address, opcodes);
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
		return branchInstruction >= 0;
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
			result.append(String.format("%08X", opcodes[i].getOpcode()));
		}
		result.append("]");

		result.append("(");
		for (int i = 0; i < getNumberParameters(); i++) {
			if (i > 0) {
				result.append(",");
			}
			result.append(getParameterValue(i, 0));
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
