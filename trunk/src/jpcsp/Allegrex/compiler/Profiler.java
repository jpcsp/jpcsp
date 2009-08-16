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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.nativeCode.NativeCodeSequence;

/**
 * @author gid15
 *
 */
public class Profiler {
    public  static Logger log = Logger.getLogger("profiler");
	public  static boolean enableProfiler = true;
	private static HashMap<Integer, Long> callCounts = new HashMap<Integer, Long>();
	private static HashMap<Integer, Long> instructionCounts = new HashMap<Integer, Long>();
	private static HashMap<Integer, Long> backBranchCounts = new HashMap<Integer, Long>();
	private static final Long zero = new Long(0);
	private static final int detailedCodeBlockLogThreshold = 20;
	private static final int codeLogMaxLength = 100;
	private static final int backBranchMaxLength = 30;
	private static final int backBranchContextBefore = 5;
	private static final int backBranchContextAfter = 3;

	public static void initialise() {
		reset();
	}

	public static void reset() {
		if (!enableProfiler) {
			return;
		}

		callCounts.clear();
		instructionCounts.clear();
		backBranchCounts.clear();
	}

	public static void exit() {
        if (enableProfiler) {
        	ArrayList<Integer> sortedBackBranches = new ArrayList<Integer>(backBranchCounts.keySet());
        	Collections.sort(sortedBackBranches, new BackBranchComparator());

        	ArrayList<CodeBlock> sortedCodeBlocks = new ArrayList<CodeBlock>(RuntimeContext.getCodeBlocks().values());
        	Collections.sort(sortedCodeBlocks, new CodeBlockComparator());

        	long allCycles = 0;
        	for (Long instructionCount : instructionCounts.values()) {
        		allCycles += instructionCount;
        	}

        	int count = 0;
        	log.info(String.format("CodeBlocks profiling information (%,d total cycles):", allCycles));
        	for (CodeBlock codeBlock : sortedCodeBlocks) {
        		long callCount = getCallCount(codeBlock);
        		long instructionCount = getInstructionCount(codeBlock);

        		if (callCount == 0 && instructionCount == 0) {
        			// This and the following CodeBlocks have not been called since
        			// the Profiler has been reset. Skip them.
        			break;
        		}

        		String name = codeBlock.getClassName();
        		NativeCodeSequence nativeCodeSequence = Compiler.getInstance().getNativeCodeManager().getCompiledNativeCodeBlock(codeBlock.getStartAddress());
        		if (nativeCodeSequence != null) {
        			name = String.format("%s (%s)", name, nativeCodeSequence.getName());
        		}
        		int lowestAddress = codeBlock.getLowestAddress();
        		int highestAddress = codeBlock.getHighestAddress();
        		int length = (highestAddress - lowestAddress) / 4 + 1;
                double percentage = 0;
                if (allCycles != 0) {
                	percentage = (instructionCount / (double) allCycles) * 100;
                }
        		log.info(String.format("%s %,d instructions (%2.3f%%), %,d calls (%08X - %08X, length %d)", name, instructionCount, percentage, callCount, lowestAddress, highestAddress, length));

        		if (count < detailedCodeBlockLogThreshold) {
        			if (codeBlock.getLength() <= codeLogMaxLength) {
        				logCode(codeBlock);
        			}
        		}

        		for (Iterator<Integer> it = sortedBackBranches.iterator(); it.hasNext(); ) {
        			int address = it.next();
        			if (address >= lowestAddress && address <= highestAddress) {
        				CodeInstruction codeInstruction = codeBlock.getCodeInstruction(address);
        				if (codeInstruction != null) {
        					int branchingToAddress = codeInstruction.getBranchingTo();
        					// Add 2 for branch instruction itself and delay slot
        					int backBranchLength = (address - branchingToAddress) / 4 + 2;
            				log.info(String.format("  Back Branch %08X %,d times (length %d)", address, backBranchCounts.get(address), backBranchLength));

            				if (count < detailedCodeBlockLogThreshold) {
            					if (backBranchLength <= backBranchMaxLength) {
            						logCode(codeBlock, branchingToAddress, backBranchLength, backBranchContextBefore, backBranchContextAfter, address);
            					}
            				}
        				}
        			}
        		}

        		count++;
        	}
        }
    }

	private static void logCode(CodeBlock codeBlock) {
		logCode(codeBlock, codeBlock.getLowestAddress(), codeBlock.getLength(), 0, 0, -1);
	}

	private static void logCode(CodeBlock codeBlock, int startAddress, int length, int contextBefore, int contextAfter, int highlightAddress) {
		for (int i = -contextBefore; i < length + contextAfter; i++) {
			int address = startAddress + (i * 4);
			CodeInstruction codeInstruction = codeBlock.getCodeInstruction(address);
			if (codeInstruction != null) {
				int opcode = codeInstruction.getOpcode();
				String prefix = "   ";
				if (address == highlightAddress) {
					prefix = "-->";
				}
				log.info(String.format("%s %08X:[%08X]: %s", prefix, address, opcode, codeInstruction.getInsn().disasm(address, opcode)));
			}
		}
	}

	private static long getCallCount(CodeBlock codeBlock) {
		Long callCount = callCounts.get(codeBlock.getStartAddress());
		if (callCount == null) {
			return 0;
		}

		return callCount;
	}

	private static long getInstructionCount(CodeBlock codeBlock) {
		Long instructionCount = instructionCounts.get(codeBlock.getStartAddress());
		if (instructionCount == null) {
			return 0;
		}

		return instructionCount;
	}

	public static void addCall(int address) {
		Long callCount = callCounts.get(address);
		if (callCount == null) {
			callCount = zero;
		}

		callCounts.put(address, callCount + 1);
	}

	public static void addInstructionCount(int count, int address) {
		Long instructionCount = instructionCounts.get(address);
		if (instructionCount == null) {
			instructionCount = zero;
		}

		instructionCounts.put(address, instructionCount + count);
	}

	public static void addBackBranch(int address) {
		Long backBranchCount = backBranchCounts.get(address);
		if (backBranchCount == null) {
			backBranchCount = zero;
		}

		backBranchCounts.put(address, backBranchCount + 1);
	}

	private static class BackBranchComparator implements Comparator<Integer> {
		@Override
		public int compare(Integer address1, Integer address2) {
			long count1 = backBranchCounts.get(address1);
			long count2 = backBranchCounts.get(address2);

			return (count2 > count1 ? 1 : -1);
		}
	}

	private static class CodeBlockComparator implements Comparator<CodeBlock> {
		@Override
		public int compare(CodeBlock codeBlock1, CodeBlock codeBlock2) {
			long instructionCallCount1 = getInstructionCount(codeBlock1);
			long instructionCallCount2 = getInstructionCount(codeBlock2);

			if (instructionCallCount1 != instructionCallCount2) {
				return (instructionCallCount2 > instructionCallCount1 ? 1 : -1);
			}

			long callCount1 = getCallCount(codeBlock1);
			long callCount2 = getCallCount(codeBlock2);

			if (callCount1 != callCount2) {
				return (callCount2 > callCount1 ? 1 : -1);
			}

			return codeBlock2.getStartAddress() - codeBlock1.getStartAddress();
		}
    }
}
