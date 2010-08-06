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
import jpcsp.Allegrex.compiler.CodeBlock;
import jpcsp.Allegrex.compiler.CodeInstruction;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.util.Utilities;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author gid15
 *
 */
public class NativeCodeManager {
	private HashMap<Integer, List<NativeCodeSequence>> nativeCodeSequencesByFirstOpcode;
	private HashMap<Integer, NativeCodeSequence> compiledNativeCodeBlocks;

	public NativeCodeManager(Element configuration) {
		compiledNativeCodeBlocks = new HashMap<Integer, NativeCodeSequence>();
		nativeCodeSequencesByFirstOpcode = new HashMap<Integer, List<NativeCodeSequence>>();

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

	private int getParameterValue(String valueString) {
		if (valueString == null || valueString.length() <= 0) {
			return 0;
		}

		for (int i = 0; i < Common.gprNames.length; i++) {
			if (Common.gprNames[i].equals(valueString)) {
				return i;
			}
		}

		try {
			return Integer.parseInt(valueString);
		} catch (NumberFormatException e) {
			Compiler.log.error(e);
		}

		return 0;
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

	private int getInteger(String valueString, int defaultValue) {
		if (valueString == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(valueString);
		} catch (NumberFormatException e) {
			Compiler.log.error(e);
		}

		return defaultValue;
	}

	private void loadNativeCodeOpcodes(NativeCodeSequence nativeCodeSequence, String codeInstructions) {
		BufferedReader reader = new BufferedReader(new StringReader(codeInstructions));
		if (reader == null) {
			return;
		}

		Pattern codeInstructionPattern = Pattern.compile(".*\\[(\\p{XDigit}+)\\].*");
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
							opcode = Utilities.parseAddress(codeInstructionMatcher.group(1));
						} else {
							opcode = Utilities.parseAddress(line.trim());
						}

						nativeCodeSequence.addOpcode(opcode);
					} catch (NumberFormatException e) {
						Compiler.log.error(e);
					}
				}
			}
		} catch (IOException e) {
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

		String branchInstructionString = getContent(element.getElementsByTagName("BranchInstruction"));
		int branchInstruction = getInteger(branchInstructionString, -1);
		if (branchInstruction >= 0) {
			nativeCodeSequence.setBranchInstruction(branchInstruction);
		}

		String isReturningString = getContent(element.getElementsByTagName("IsReturning"));
		if (isReturningString != null) {
			nativeCodeSequence.setReturning(Boolean.parseBoolean(isReturningString));
		}

		String wholeCodeBlockString = getContent(element.getElementsByTagName("WholeCodeBlock"));
		if (wholeCodeBlockString != null) {
			nativeCodeSequence.setWholeCodeBlock(Boolean.parseBoolean(wholeCodeBlockString));
		}

		String parametersList = getContent(element.getElementsByTagName("Parameters"));
		if (parametersList != null) {
			String[] parameters = parametersList.split(" *, *");
			for (int parameter = 0; parameters != null && parameter < parameters.length; parameter++) {
				int value = getParameterValue(parameters[parameter]);
				nativeCodeSequence.setParameter(parameter, value);
			}
		}

		String methodName = getContent(element.getElementsByTagName("Method"));
		if (methodName != null) {
			nativeCodeSequence.setMethodName(methodName);
		}

		String codeInstructions = getContent(element.getElementsByTagName("CodeInstructions"));
		loadNativeCodeOpcodes(nativeCodeSequence, codeInstructions);

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
			int firstOpcode = nativeCodeSequence.getOpcodes()[0];

			if (!nativeCodeSequencesByFirstOpcode.containsKey(firstOpcode)) {
				nativeCodeSequencesByFirstOpcode.put(firstOpcode, new LinkedList<NativeCodeSequence>());
			}
			nativeCodeSequencesByFirstOpcode.get(firstOpcode).add(nativeCodeSequence);
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

		// Does this NativeCodeSequence only matching a whole CodeBlock?
		if (nativeCodeSequence.isWholeCodeBlock()) {
			// Match only a whole CodeBlock: same StartAddress, same Length
			if (codeBlock.getStartAddress() != address) {
				return false;
			}

			if (codeBlock.getLength() != nativeCodeSequence.getNumOpcodes()) {
				return false;
			}
		}

		int[] opcodes = nativeCodeSequence.getOpcodes();
		// First Opcode is already matching, start at index 1 (second opcode)
		for (int i = 1; i < opcodes.length; i++) {
			CodeInstruction codeInstruction2 = codeBlock.getCodeInstruction(address + i * 4);
			if (codeInstruction2 == null || codeInstruction2.getOpcode() != opcodes[i]) {
				return false;
			}
		}

		return true;
	}

	public NativeCodeSequence getNativeCodeSequence(CodeInstruction codeInstruction, CodeBlock codeBlock) {
		int firstOpcode = codeInstruction.getOpcode();

		if (!nativeCodeSequencesByFirstOpcode.containsKey(firstOpcode)) {
			return null;
		}

		for (Iterator<NativeCodeSequence> it = nativeCodeSequencesByFirstOpcode.get(firstOpcode).iterator(); it.hasNext(); ) {
			NativeCodeSequence nativeCodeSequence = it.next();
			if (isNativeCodeSequence(nativeCodeSequence, codeInstruction, codeBlock)) {
				return nativeCodeSequence;
			}
		}

		return null;
	}
}
