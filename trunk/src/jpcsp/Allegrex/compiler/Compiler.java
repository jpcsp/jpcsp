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

import static jpcsp.Allegrex.Common.Instruction.FLAG_ENDS_BLOCK;
import static jpcsp.Allegrex.Common.Instruction.FLAG_IS_BRANCHING;
import static jpcsp.Allegrex.Common.Instruction.FLAG_IS_JUMPING;
import static jpcsp.Allegrex.Common.Instruction.FLAG_STARTS_NEW_BLOCK;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jpcsp.AllegrexOpcodes;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.Decoder;
import jpcsp.Allegrex.Instructions;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeManager;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemorySections;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.AbstractIntSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * TODO to cleanup the code:
 * - add flags to Common.Instruction:
 *     - isBranching (see branchBlockInstructions list below) [DONE]
 *     - is end of CodeBlock (see endBlockInstructions list below) [DONE]
 *     - is starting a new CodeBlock (see newBlockInstructions list below) [DONE]
 *     - isBranching unconditional [DONE]
 *     - isBranching with 16 bits target [DONE]
 *     - isBranching with 26 bits target (see jumpBlockInstructions list below) [DONE]
 *     - is jump or call [DONE]
 *     - is compiled or interpreted [DONE]
 *     - is referencing $pc register
 *     - can switch thread context
 *     - is loading $sp address to another register
 *     - is reading Rs
 *     - is reading Rt
 *     - is writing Rt
 *     - is writing Rd
 *
 * TODO Ideas to further enhance performance:
 * - store stack variables "nn(sp)" into local variables
 *   if CodeBlock is following standard MIPS call conventions and
 *   if sp is not loaded into another register
 * - store registers in local variables and flush registers back to gpr[] when calling
 *   subroutine (if CodeBlock is following standard MIPS call conventions).
 *     subroutine parameters: $a0-$a3
 *     callee-saved registers: $fp, $s0-$s7
 *     caller-saved registers: $ra, $t0-$t9, $a0-$a3
 *     Register description:
 *       $zr (0): 0
 *       $at (1): reserved for assembler
 *       $v0-$v1 (2-3): expression evaluation & function results
 *       $a0-$a3 (4-7): arguments, not preserved across subroutine calls
 *       $t0-$t7 (8-15): temporary: caller saves, not preserved across subroutine calls
 *       $s0-$s7 (16-23): callee saves, must be preserved across subroutine calls
 *       $t8-$t9 (24-25): temporary (cont'd)
 *       $k0-$k1 (26-27): reserved for OS kernel
 *       $gp (28): Pointer to global area (Global Pointer)
 *       $sp (29): Stack Pointer
 *       $fp (30): Frame Pointer, must be preserved across subroutine calls
 *       $ra (31): Return Address
 *  - provide bytecode compilation for mostly used instructions
 *    e.g.: typical program is using the following instructions based on frequency usage:
 *            ADDIU 8979678 (14,1%)
 *             ADDU 6340735 ( 9,9%)
 *               LW 5263292 ( 8,3%)
 *               SW 5223074 ( 8,2%)
 *              LUI 3930978 ( 6,2%)
 *             ANDI 2833815 ( 4,4%)
 *               SB 2812802 ( 4,4%)
 *              BNE 2176061 ( 3,4%)
 *              SRL 1919480 ( 3,0%)
 *               OR 1888281 ( 3,0%)
 *              AND 1411970 ( 2,2%)
 *              SLL 1406954 ( 2,2%)
 *              JAL 1297959 ( 2,0%)
 *              NOP 1272929 ( 2,0%)
 *             SRAV 1132740
 *               JR 1124095
 *              LBU 1068533
 *              BEQ 1032768
 *              LHU 1011791
 *            BGTZL 943264
 *             MFLO 680659
 *             MULT 679357
 *              ORI 503067
 *                J 453007
 *             SLTI 432961
 *              SRA 293213
 *             BEQL 228478
 *          SYSCALL 215713
 *             BGEZ 168494
 *             BNEL 159020
 *             SLTU 124580
 *  - implement subroutine arguments as Java arguments instead of $a0-$a3.
 *    Automatically detect how many arguments are expected by a subroutine.
 *    Generate different Java classes if number of expected arguments is refined
 *    over time (include number in Class name).
 */
/**
 * @author gid15
 *
 */
public class Compiler implements ICompiler {
    public static Logger log = Logger.getLogger("compiler");
	private static Compiler instance;
	private static int resetCount = 0;
	private CompilerClassLoader classLoader;
	public static CpuDurationStatistics compileDuration = new CpuDurationStatistics("Compilation Time");
	private Document configuration;
	private NativeCodeManager nativeCodeManager;
    private boolean ignoreInvalidMemory = false;
    public int defaultMethodMaxInstructions = 3000;
    private static final int maxRecompileExecutable = 50;
    private CompilerTypeManager compilerTypeManager;

	private class IgnoreInvalidMemoryAccessSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setIgnoreInvalidMemory(value);
		}
	}

	private class MethodMaxInstructionsSettingsListerner extends AbstractIntSettingsListener {
		@Override
		protected void settingsValueChanged(int value) {
			setDefaultMethodMaxInstructions(value);
		}
	}

    private boolean isIgnoreInvalidMemory() {
        return ignoreInvalidMemory;
    }

    private void setIgnoreInvalidMemory(boolean enable) {
        ignoreInvalidMemory = enable;
    }

	public static Compiler getInstance() {
		if (instance == null) {
			instance = new Compiler();
		}

		return instance;
	}

	private Compiler() {
		Initialise();
	}

	public static void exit() {
	    if (instance != null) {
	    	if (DurationStatistics.collectStatistics) {
	    		log.info(compileDuration);
	    	}
	    }
	}

	public void reset() {
		resetCount++;
		classLoader = new CompilerClassLoader(this);
		compileDuration.reset();
		nativeCodeManager.reset();
	}

    public void invalidateAll() {
        // Simply generate a new class loader.
    	log.info("Compiler: invalidating all compiled classes");
        classLoader = new CompilerClassLoader(this);
    }

    public boolean checkSimpleInterpretedCodeBlock(CodeBlock codeBlock) {
    	boolean isSimple = true;
    	int insnCount = 0;
    	Instruction[] insns = new Instruction[100];
    	int[] opcodes = new int[100];
    	int opcodeJrRa = AllegrexOpcodes.JR | (Common._ra << 21); // jr $ra

    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(codeBlock.getStartAddress(), 4);
    	int notSimpleFlags = FLAG_IS_BRANCHING | FLAG_IS_JUMPING | FLAG_STARTS_NEW_BLOCK | FLAG_ENDS_BLOCK;
    	while (true) {
    		if (insnCount >= insns.length) {
    			// Extend insns array
    			Instruction[] newInsns = new Instruction[insnCount + 100];
    			System.arraycopy(insns, 0, newInsns, 0, insnCount);
    			insns = newInsns;
    			// Extend opcodes array
    			int[] newOpcodes = new int[newInsns.length];
    			System.arraycopy(opcodes, 0, newOpcodes, 0, insnCount);
    			opcodes = newOpcodes;
    		}

    		int opcode = memoryReader.readNext();

    		if (opcode == opcodeJrRa) {
				int delaySlotOpcode = memoryReader.readNext();
				Instruction delaySlotInsn = Decoder.instruction(delaySlotOpcode);
				insns[insnCount] = delaySlotInsn;
				opcodes[insnCount] = delaySlotOpcode;
				insnCount++;
				break;
    		}

    		Instruction insn = Decoder.instruction(opcode);
    		if ((insn.getFlags() & notSimpleFlags) != 0) {
    			isSimple = false;
    			break;
    		}

    		insns[insnCount] = insn;
			opcodes[insnCount] = opcode;
			insnCount++;
    	}

    	if (isSimple) {
        	if (insnCount < insns.length) {
    			// Compact insns array
    			Instruction[] newInsns = new Instruction[insnCount];
    			System.arraycopy(insns, 0, newInsns, 0, insnCount);
    			insns = newInsns;
    			// Compact opcodes array
    			int[] newOpcodes = new int[insnCount];
    			System.arraycopy(opcodes, 0, newOpcodes, 0, insnCount);
    			opcodes = newOpcodes;
        	}
        	codeBlock.setInterpretedInstructions(insns);
        	codeBlock.setInterpretedOpcodes(opcodes);
    	} else {
    		codeBlock.setInterpretedInstructions(null);
    	}

    	return isSimple;
    }

    public void invalidateCodeBlock(CodeBlock codeBlock) {
    	IExecutable executable = codeBlock.getExecutable();
    	if (executable != null) {
    		// If the application is invalidating the same code block too many times,
    		// do no longer try to recompile it each time, interpret it.
    		if (codeBlock.getInstanceIndex() > maxRecompileExecutable) {
    			codeBlock.setInterpreted(true);
    			executable.setExecutable(new InterpretExecutable(codeBlock));
    		} else {
	    		// Force a recompilation of the codeBlock at the next execution
	        	RecompileExecutable recompileExecutable = new RecompileExecutable(codeBlock);
	        	executable.setExecutable(recompileExecutable);
    		}
    	}
    }

    private void Initialise() {
    	Settings.getInstance().registerSettingsListener("Compiler", "emu.ignoreInvalidMemoryAccess", new IgnoreInvalidMemoryAccessSettingsListerner());
    	Settings.getInstance().registerSettingsListener("Compiler", "emu.compiler.methodMaxInstructions", new MethodMaxInstructionsSettingsListerner());

    	DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setIgnoringElementContentWhitespace(true);
		documentBuilderFactory.setIgnoringComments(true);
		documentBuilderFactory.setCoalescing(true);
		configuration = null;
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			configuration = documentBuilder.parse(new File("Compiler.xml"));
		} catch (ParserConfigurationException e) {
			log.error(e);
		} catch (SAXException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}

		if (configuration != null) {
			nativeCodeManager = new NativeCodeManager(configuration.getDocumentElement());
		} else {
			nativeCodeManager = new NativeCodeManager(null);
		}

		compilerTypeManager = new CompilerTypeManager();

		reset();
	}

	public static int jumpTarget(int pc, int opcode) {
		return (pc & 0xF0000000) | ((opcode & 0x03FFFFFF) << 2);
	}

	public static int branchTarget(int pc, int opcode) {
		return pc + (((short) (opcode & 0x0000FFFF)) << 2);
	}

	private IExecutable interpret(CompilerContext context, int startAddress, int instanceIndex) {
        if (log.isDebugEnabled()) {
            log.debug("Compiler.interpret Block 0x" + Integer.toHexString(startAddress));
        }
        startAddress = startAddress & Memory.addressMask;
        CodeBlock codeBlock = new CodeBlock(startAddress, instanceIndex);

        IExecutable executable = codeBlock.getInterpretedExecutable(context);
        if (log.isDebugEnabled()) {
            log.debug("Executable: " + executable);
        }

        return executable;
	}

	public static boolean isEndBlockInsn(int pc, int opcode, Instruction insn) {
        if (insn.hasFlags(Instruction.FLAG_ENDS_BLOCK)) {
        	if (insn.hasFlags(Instruction.FLAG_IS_CONDITIONAL | Instruction.FLAG_IS_BRANCHING)) {
        		// Detect the conditional
        		//    "BEQ $xx, $xx, target"
        		// which is equivalent to the unconditional
        		//    "B target"
        		if (insn == Instructions.BEQ) {
            		int rt = (opcode >> 16) & 0x1F;
            		int rs = (opcode >> 21) & 0x1F;
            		if (rs == rt) {
            			return true;
            		}
        		} else {
        			log.error(String.format("Unknown conditional instruction ending a block: %s", insn.disasm(pc, opcode)));
        		}
        	} else {
        		return true;
        	}
        }

        return false;
	}

	private IExecutable analyse(CompilerContext context, int startAddress, boolean recursive, int instanceIndex) throws ClassFormatError {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Compiler.analyse Block 0x%08X", startAddress));
        }
        MemorySections memorySections = MemorySections.getInstance();
        startAddress = startAddress & Memory.addressMask;
        CodeBlock codeBlock = new CodeBlock(startAddress, instanceIndex);
        Stack<Integer> pendingBlockAddresses = new Stack<Integer>();
        pendingBlockAddresses.clear();
        pendingBlockAddresses.push(startAddress);
        while (!pendingBlockAddresses.isEmpty()) {
            int pc = pendingBlockAddresses.pop();
            if (!Memory.isAddressGood(pc)) {
                if (isIgnoreInvalidMemory()) {
                    log.warn(String.format("IGNORING: Trying to compile an invalid address 0x%08X", pc));
                } else {
                    log.error(String.format("Trying to compile an invalid address 0x%08X", pc));
                }
            	return null;
            }
            boolean isBranchTarget = true;
            int endPc = MemoryMap.END_RAM;

            // Handle branching to a delayed instruction.
            // The delayed instruction has already been analysed, but the next
            // address maybe not.
            if (context.analysedAddresses.contains(pc) && !context.analysedAddresses.contains(pc + 4)) {
            	pc += 4;
            }

            if (context.analysedAddresses.contains(pc) && isBranchTarget) {
                codeBlock.setIsBranchTarget(pc);
            } else {
            	IMemoryReader memoryReader = MemoryReader.getMemoryReader(pc, 4);
                while (!context.analysedAddresses.contains(pc) && pc <= endPc) {
                    int opcode = memoryReader.readNext();

                    Common.Instruction insn = Decoder.instruction(opcode);

                    context.analysedAddresses.add(pc);
                    int npc = pc + 4;

                    int branchingTo = 0;
                    boolean isBranching = false;
                    boolean checkDynamicBranching = false;
                    if (insn.hasFlags(Instruction.FLAG_IS_BRANCHING)) {
                        branchingTo = branchTarget(npc, opcode);
                        isBranching = true;
                    } else if (insn.hasFlags(Instruction.FLAG_IS_JUMPING)) {
                		branchingTo = jumpTarget(npc, opcode);
                		isBranching = true;
                		checkDynamicBranching = true;
                    }

                    if (isEndBlockInsn(pc, opcode, insn)) {
                    	endPc = npc;
                    }

                    if (insn.hasFlags(Instruction.FLAG_STARTS_NEW_BLOCK)) {
                        if (recursive) {
                            context.blocksToBeAnalysed.push(branchingTo);
                        }
                    } else if (isBranching) {
                        if (branchingTo != 0) {  // Ignore "J 0x00000000" instruction
                        	if (checkDynamicBranching) {
	                        	// Analyse only the jump instructions that are jumping to
	                        	// non-writeable memory sections. A jump to a writeable memory
	                        	// section has to be interpreted at runtime to check if the
	                        	// reached code has not been changed (i.e. invalidated).
                        		if (!memorySections.canWrite(branchingTo, false)) {
	                        		pendingBlockAddresses.push(branchingTo);
	                        	}
                        	} else {
                        		pendingBlockAddresses.push(branchingTo);
                        	}
                        }
                    }

                    codeBlock.addInstruction(pc, opcode, insn, isBranchTarget, isBranching, branchingTo);
                    pc = npc;

                    isBranchTarget = false;
                }
            }
        }

        IExecutable executable = codeBlock.getExecutable(context);
        if (log.isTraceEnabled()) {
            log.trace("Executable: " + executable);
        }

        return executable;
	}

	public void analyseRecursive(int startAddress, int instanceIndex) {
	    if (RuntimeContext.hasCodeBlock(startAddress)) {
	        if (log.isDebugEnabled()) {
	            log.debug("Compiler.analyse 0x" + Integer.toHexString(startAddress).toUpperCase() + " - already analysed");
	        }
	        return;
	    }

        if (log.isDebugEnabled()) {
            log.debug("Compiler.analyse 0x" + Integer.toHexString(startAddress).toUpperCase());
        }

        CompilerContext context = new CompilerContext(classLoader, instanceIndex);
		context.blocksToBeAnalysed.push(startAddress);
		while (!context.blocksToBeAnalysed.isEmpty()) {
			int blockStartAddress = context.blocksToBeAnalysed.pop();
			analyse(context, blockStartAddress, true, instanceIndex);
		}
	}

    @Override
    public IExecutable compile(String name) {
        return compile(CompilerContext.getClassAddress(name), CompilerContext.getClassInstanceIndex(name));
    }

    @Override
    public IExecutable compile(int address) {
    	return compile(address, getResetCount());
    }

    private CompilerContext retryCompilation(CompilerContext context, int instanceIndex, int retries, Throwable e) {
        // Try again with stricter methodMaxInstructions (75% of current value)
        int methodMaxInstructions = context.getMethodMaxInstructions() * 3 / 4;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("Catched exception '%s' (can be ignored)", e.toString()));
        	log.debug(String.format("Retrying compilation again with maxInstruction=%d, retries left=%d...", methodMaxInstructions, retries - 1));
        }
        context = new CompilerContext(classLoader, instanceIndex);
        context.setMethodMaxInstructions(methodMaxInstructions);

        return context;
    }

    public IExecutable compile(int address, int instanceIndex) {
    	if (!Memory.isAddressGood(address)) {
            if(isIgnoreInvalidMemory())
                log.warn(String.format("IGNORING: Trying to compile an invalid address 0x%08X", address));
            else {
                log.error(String.format("Trying to compile an invalid address 0x%08X", address));
                Emulator.PauseEmu();
            }
    		return null;
    	}

    	// Disable the PSP clock while compiling. This could cause timing problems
    	// in some applications while compiling large MIPS functions.
    	Emulator.getClock().pause();

    	long compilationStartMicros = 0;
    	if (Profiler.isProfilerEnabled()) {
    		compilationStartMicros = System.nanoTime() / 1000;
    	}

    	compileDuration.start();
        CompilerContext context = new CompilerContext(classLoader, instanceIndex);
        IExecutable executable = null;
        ClassFormatError error = null;
        RuntimeException exception = null;
        for (int retries = 2; retries > 0; retries--) {
            try {
                executable = analyse(context, address, false, instanceIndex);
                break;
            } catch (ClassFormatError e) {
                // Catch exception
                //     java.lang.ClassFormatError: Invalid method Code length nnnnnn in class file XXXX
                //
                error = e;

                context = retryCompilation(context, instanceIndex, retries, e);
            } catch (NullPointerException e) {
            	log.error(String.format("Catched exception '%s' while compiling 0x%08X (0x%08X-0x%08X)", e.toString(), address, context.getCodeBlock().getLowestAddress(), context.getCodeBlock().getHighestAddress()));
            	break;
            } catch (RuntimeException e) {
            	// Catch exception
            	//     java.lang.RuntimeException: Method code too large!
                exception = e;

                context = retryCompilation(context, instanceIndex, retries, e);
            }
        }
        compileDuration.end();

        if (Profiler.isProfilerEnabled()) {
        	long compilationEndMicros = System.nanoTime() / 1000;
        	Profiler.addCompilation(compilationEndMicros - compilationStartMicros);
        }

        if (executable == null) {
            Compiler.log.debug("Compilation failed with maxInstruction=" + context.getMethodMaxInstructions());
            context = new CompilerContext(classLoader, instanceIndex);
            executable = interpret(context, address, instanceIndex);
            if (executable == null) {
            	if (error != null) {
            		throw error;
            	}
            	if (exception != null) {
            		throw exception;
            	}
            }
        } else if (error != null) {
            Compiler.log.debug("Compilation was now correct with maxInstruction=" + context.getMethodMaxInstructions());
        }

        // Resume the PSP clock after compilation
        Emulator.getClock().resume();

        return executable;
    }

    public CompilerClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(CompilerClassLoader classLoader) {
        this.classLoader = classLoader;
    }

	public static int getResetCount() {
		return resetCount;
	}

	public static void setResetCount(int resetCount) {
		Compiler.resetCount = resetCount;
	}

	public NativeCodeManager getNativeCodeManager() {
		return nativeCodeManager;
	}

	public int getDefaultMethodMaxInstructions() {
		return defaultMethodMaxInstructions;
	}

	public void setDefaultMethodMaxInstructions(int defaultMethodMaxInstructions) {
		if (defaultMethodMaxInstructions > 0) {
			this.defaultMethodMaxInstructions = defaultMethodMaxInstructions;

			log.info(String.format("Compiler MethodMaxInstructions: %d", defaultMethodMaxInstructions));
		}
	}

	public CompilerTypeManager getCompilerTypeManager() {
		return compilerTypeManager;
	}
}
