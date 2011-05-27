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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.compiler.CodeBlock;
import jpcsp.Allegrex.compiler.CodeInstruction;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author gid15
 *
 */
public class NativeCodeManager {
	private static int defaultOpcodeMask = 0xFFFFFFFF;
	private HashMap<Integer, List<NativeCodeSequence>> nativeCodeSequencesByFirstOpcode;
	private List<NativeCodeSequence> nativeCodeSequenceWithMaskInFirstOpcode;
	private HashMap<Integer, NativeCodeSequence> compiledNativeCodeBlocks;

	public NativeCodeManager(Element configuration) {
		compiledNativeCodeBlocks = new HashMap<Integer, NativeCodeSequence>();
		nativeCodeSequencesByFirstOpcode = new HashMap<Integer, List<NativeCodeSequence>>();
		nativeCodeSequenceWithMaskInFirstOpcode = new LinkedList<NativeCodeSequence>();

		load(configuration);
	}

	public void reset() {
		compiledNativeCodeBlocks.clear();
	}

	@SuppressWarnings("unchecked")
	private Class<INativeCodeSequence> getNativeCodeSequenceClass(String className) {
		try {
			return (Class<INativeCodeSequence>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			Compiler.log.error(e);
			return null;
		}
	}

	private String getContent(Node node) {
		if (node.hasChildNodes()) {
			return getContent(node.getChildNodes());
		}

		return node.getNodeValue();
	}

	private String getContent(NodeList nodeList) {
		if (nodeList == null || nodeList.getLength() <= 0) {
			return null;
		}

		StringBuilder content = new StringBuilder();
		int n = nodeList.getLength();
		for (int i = 0; i < n; i++) {
			Node node = nodeList.item(i);
			content.append(getContent(node));
		}

		return content.toString();
	}

	private void loadBeforeCodeInstructions(NativeCodeSequence nativeCodeSequence, String codeInstructions) {
		BufferedReader reader = new BufferedReader(new StringReader(codeInstructions));
		if (reader == null) {
			return;
		}

		Pattern codeInstructionPattern = Pattern.compile("\\s*(\\w+\\s*:?\\s*)?\\[(\\p{XDigit}+)\\].*");
		final int opcodeGroup = 2;
		final int address = 0;
		
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				line = line.trim();
				if (line.length() > 0) {
					try {
						Matcher codeInstructionMatcher = codeInstructionPattern.matcher(line);
						int opcode = 0;
						if (codeInstructionMatcher.matches()) {
							opcode = Utilities.parseAddress(codeInstructionMatcher.group(opcodeGroup));
						} else {
							opcode = Utilities.parseAddress(line.trim());
						}

						Common.Instruction insn = Decoder.instruction(opcode);
						CodeInstruction codeInstruction = new CodeInstruction(address, opcode, insn, false, false, 0);

						nativeCodeSequence.addBeforeCodeInstruction(codeInstruction);
					} catch (NumberFormatException e) {
						Compiler.log.error(e);
					}
				}
			}
		} catch (IOException e) {
			Compiler.log.error(e);
		}
	}

	private void loadNativeCodeOpcodes(NativeCodeSequence nativeCodeSequence, String codeInstructions) {
		BufferedReader reader = new BufferedReader(new StringReader(codeInstructions));
		if (reader == null) {
			return;
		}

		Pattern codeInstructionPattern = Pattern.compile("\\s*((\\w+)\\s*:?\\s*)?\\[(\\p{XDigit}+)(/(\\p{XDigit}+))?\\].*");
		final int labelGroup = 2;
		final int opcodeGroup = 3;
		final int opcodeMaskGroup = 5;

		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				line = line.trim();
				if (line.length() > 0) {
					try {
						Matcher codeInstructionMatcher = codeInstructionPattern.matcher(line);
						int opcode = 0;
						int mask = defaultOpcodeMask;
						String label = null;
						if (codeInstructionMatcher.matches()) {
							opcode = Utilities.parseAddress(codeInstructionMatcher.group(opcodeGroup));
							String opcodeMaskString = codeInstructionMatcher.group(opcodeMaskGroup);
							if (opcodeMaskString != null) {
								mask = Utilities.parseAddress(opcodeMaskString);
							}
							label = codeInstructionMatcher.group(labelGroup);
						} else {
							opcode = Utilities.parseAddress(line.trim());
						}

						nativeCodeSequence.addOpcode(opcode, mask, label);
					} catch (NumberFormatException e) {
						Compiler.log.error(e);
					}
				}
			}
		} catch (IOException e) {
			Compiler.log.error(e);
		}
	}

	private void setParameter(NativeCodeSequence nativeCodeSequence, int parameter, String valueString) {
		if (valueString == null || valueString.length() <= 0) {
			nativeCodeSequence.setParameter(parameter, 0, false);
			return;
		}

		for (int i = 0; i < Common.gprNames.length; i++) {
			if (Common.gprNames[i].equals(valueString)) {
				nativeCodeSequence.setParameter(parameter, i, false);
				return;
			}
		}

		for (int i = 0; i < Common.fprNames.length; i++) {
			if (Common.fprNames[i].equals(valueString)) {
				nativeCodeSequence.setParameter(parameter, i, false);
				return;
			}
		}

		if (valueString.startsWith("@")) {
			String label = valueString.substring(1);
			int labelIndex = nativeCodeSequence.getLabelIndex(label);
			if (labelIndex >= 0) {
				nativeCodeSequence.setParameter(parameter, labelIndex, true);
				return;
			}
		}

		try {
			int value;
			if (valueString.startsWith("0x")) {
				value = Integer.parseInt(valueString.substring(2), 16);
			} else {
				value = Integer.parseInt(valueString);
			}
			nativeCodeSequence.setParameter(parameter, value, false);
		} catch (NumberFormatException e) {
			Compiler.log.error(e);
		}
	}

	private void loadNativeCodeSequence(Element element) {
		String name = element.getAttribute("name");
		String className = getContent(element.getElementsByTagName("Class"));

		Class<INativeCodeSequence> nativeCodeSequenceClass = getNativeCodeSequenceClass(className);
		if (nativeCodeSequenceClass == null) {
			return;
		}

		NativeCodeSequence nativeCodeSequence = new NativeCodeSequence(name, nativeCodeSequenceClass);

		String isReturningString = getContent(element.getElementsByTagName("IsReturning"));
		if (isReturningString != null) {
			nativeCodeSequence.setReturning(Boolean.parseBoolean(isReturningString));
		}

		String wholeCodeBlockString = getContent(element.getElementsByTagName("WholeCodeBlock"));
		if (wholeCodeBlockString != null) {
			nativeCodeSequence.setWholeCodeBlock(Boolean.parseBoolean(wholeCodeBlockString));
		}

		String methodName = getContent(element.getElementsByTagName("Method"));
		if (methodName != null) {
			nativeCodeSequence.setMethodName(methodName);
		}

		String codeInstructions = getContent(element.getElementsByTagName("CodeInstructions"));
		loadNativeCodeOpcodes(nativeCodeSequence, codeInstructions);

		// The "Parameters" and "BranchInstruction" have to be parsed after "CodeInstructions"
		// because they are using them (e.g. instruction labels)
		String parametersList = getContent(element.getElementsByTagName("Parameters"));
		if (parametersList != null) {
			String[] parameters = parametersList.split(" *, *");
			for (int parameter = 0; parameters != null && parameter < parameters.length; parameter++) {
				setParameter(nativeCodeSequence, parameter, parameters[parameter].trim());
			}
		}

		String branchInstructionLabel = getContent(element.getElementsByTagName("BranchInstruction"));
		if (branchInstructionLabel != null) {
			if (branchInstructionLabel.startsWith("@")) {
				branchInstructionLabel = branchInstructionLabel.substring(1);
			}
			int branchInstructionOffset = nativeCodeSequence.getLabelIndex(branchInstructionLabel.trim());
			if (branchInstructionOffset >= 0) {
				nativeCodeSequence.setBranchInstruction(branchInstructionOffset);
			} else {
				Compiler.log.error(String.format("BranchInstruction: label '%s' not found", branchInstructionLabel));
			}
		}

		String beforeCodeInstructions = getContent(element.getElementsByTagName("BeforeCodeInstructions"));
		if (beforeCodeInstructions != null) {
			loadBeforeCodeInstructions(nativeCodeSequence, beforeCodeInstructions);
		}

		addNativeCodeSequence(nativeCodeSequence);
	}

	private void load(Element configuration) {
		if (configuration == null) {
			return;
		}

		NodeList nativeCodeBlocks = configuration.getElementsByTagName("NativeCodeSequence");
		int n = nativeCodeBlocks.getLength();
		for (int i = 0; i < n; i++) {
			Element nativeCodeSequence = (Element) nativeCodeBlocks.item(i);
			loadNativeCodeSequence(nativeCodeSequence);
		}
	}

	private void addNativeCodeSequence(NativeCodeSequence nativeCodeSequence) {
		if (nativeCodeSequence.getNumOpcodes() > 0) {
			int firstOpcodeMask = nativeCodeSequence.getFirstOpcodeMask();
			if (firstOpcodeMask == defaultOpcodeMask) {
				// First opcode has not mask: fast lookup allowed
				int firstOpcode = nativeCodeSequence.getFirstOpcode();

				if (!nativeCodeSequencesByFirstOpcode.containsKey(firstOpcode)) {
					nativeCodeSequencesByFirstOpcode.put(firstOpcode, new LinkedList<NativeCodeSequence>());
				}
				nativeCodeSequencesByFirstOpcode.get(firstOpcode).add(nativeCodeSequence);
			} else {
				// First opcode has not mask: only slow lookup possible
				nativeCodeSequenceWithMaskInFirstOpcode.add(nativeCodeSequence);
			}
		}
	}

	public void setCompiledNativeCodeBlock(int address, NativeCodeSequence nativeCodeBlock) {
		compiledNativeCodeBlocks.put(address, nativeCodeBlock);
	}

	public NativeCodeSequence getCompiledNativeCodeBlock(int address) {
		return compiledNativeCodeBlocks.get(address);
	}

	private boolean isNativeCodeSequence(NativeCodeSequence nativeCodeSequence, CodeInstruction codeInstruction, CodeBlock codeBlock) {
		int address = codeInstruction.getAddress();
		int numOpcodes = nativeCodeSequence.getNumOpcodes();

		// Can this NativeCodeSequence only match a whole CodeBlock?
		if (nativeCodeSequence.isWholeCodeBlock()) {
			// Match only a whole CodeBlock: same StartAddress, same Length
			if (codeBlock.getStartAddress() != address) {
				return false;
			}

			if (codeBlock.getLength() != numOpcodes) {
				return false;
			}
		}

		IMemoryReader codeBlockReader = MemoryReader.getMemoryReader(address, 4);
		for (int i = 0; i < numOpcodes; i++) {
			int opcode = codeBlockReader.readNext();
			if (!nativeCodeSequence.isMatching(i, opcode)) {
				return false;
			}
		}

		return true;
	}

	public NativeCodeSequence getNativeCodeSequence(CodeInstruction codeInstruction, CodeBlock codeBlock) {
		int firstOpcode = codeInstruction.getOpcode();

		// Fast lookup using the first opcode
		if (nativeCodeSequencesByFirstOpcode.containsKey(firstOpcode)) {
			for (Iterator<NativeCodeSequence> it = nativeCodeSequencesByFirstOpcode.get(firstOpcode).iterator(); it.hasNext(); ) {
				NativeCodeSequence nativeCodeSequence = it.next();
				if (isNativeCodeSequence(nativeCodeSequence, codeInstruction, codeBlock)) {
					return nativeCodeSequence;
				}
			}
		}

		// Slow lookup for sequences having an opcode mask in the first opcode
		for (NativeCodeSequence nativeCodeSequence : nativeCodeSequenceWithMaskInFirstOpcode) {
			if (isNativeCodeSequence(nativeCodeSequence, codeInstruction, codeBlock)) {
				return nativeCodeSequence;
			}
		}

		return null;
	}
}