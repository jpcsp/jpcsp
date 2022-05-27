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

import static jpcsp.Allegrex.compiler.CompilerContext.executableDescriptor;
import static jpcsp.HLE.modules.ThreadManForUser.INTERNAL_THREAD_ADDRESS_END;
import static jpcsp.HLE.modules.ThreadManForUser.INTERNAL_THREAD_ADDRESS_START;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.Memory;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.nativeCode.HookCodeInstruction;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeInstruction;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeManager;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeSequence;
import jpcsp.HLE.HLEModuleFunction;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * @author gid15
 *
 */
public class CodeBlock {
	private static Logger log = Compiler.log;
	private int startAddress;
	private int lowestAddress;
	private int highestAddress;
	private LinkedList<CodeInstruction> codeInstructions = new LinkedList<CodeInstruction>();
	private LinkedList<SequenceCodeInstruction> sequenceCodeInstructions = new LinkedList<SequenceCodeInstruction>();
	private SequenceCodeInstruction currentSequence = null;
	private IExecutable executable = null;
	private final static String objectInternalName = Type.getInternalName(Object.class);
	private final static String[] interfacesForExecutable = new String[] { Type.getInternalName(IExecutable.class) };
	private final static String[] exceptions = new String[] { Type.getInternalName(Exception.class) };
	private int instanceIndex;
	private int nextInstanceIndex;
	private Instruction[] interpretedInstructions;
	private int[] interpretedOpcodes;
	private MemoryRanges memoryRanges = new MemoryRanges();
	private int flags;
	private HLEModuleFunction hleFunction;
	private IAction updateOpcodesAction;
	private final boolean debuggerEnabled;

	public CodeBlock(int startAddress, int instanceIndex) {
		this.startAddress = startAddress;
		this.instanceIndex = instanceIndex;
		lowestAddress = startAddress;
		highestAddress = startAddress;

		// Verify if we have not yet compiled a CodeBlock with a higher instanceIndex
		CodeBlock previousCodeBlock = RuntimeContext.getCodeBlock(startAddress);
		if (previousCodeBlock != null && previousCodeBlock.getInstanceIndex() > instanceIndex) {
			nextInstanceIndex = previousCodeBlock.getInstanceIndex() + 1;
		} else {
			nextInstanceIndex = instanceIndex + 1;
		}

		debuggerEnabled = RuntimeContext.enableDebugger;
	}

	private void insertInstruction(int address, CodeInstruction codeInstruction) {
		// Insert the codeInstruction in the codeInstructions list
		// and keep the list sorted by address.
		if (codeInstructions.isEmpty() || codeInstructions.getLast().getAddress() < address) {
			codeInstructions.add(codeInstruction);
		} else {
			for (ListIterator<CodeInstruction> lit = codeInstructions.listIterator(); lit.hasNext(); ) {
				CodeInstruction listItem = lit.next();
				if (listItem.getAddress() > address) {
					lit.previous();
					lit.add(codeInstruction);
					break;
				}
			}

			if (address < lowestAddress) {
				lowestAddress = address;
			}
		}
	}

	public void addInstruction(int address, int opcode, Instruction insn, boolean isBranchTarget, boolean isBranching, int branchingTo, boolean useMMIO) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("CodeBlock.addInstruction 0x%08X - %s", address, insn.disasm(address, opcode)));
		}

		CodeInstruction codeInstruction = new CodeInstruction(address, opcode, insn, isBranchTarget, isBranching, branchingTo);

		codeInstruction.setUseMMIO(useMMIO);

		insertInstruction(address, codeInstruction);

		if (address > highestAddress) {
			highestAddress = address;
		}

		memoryRanges.addAddress(address);

		flags |= insn.getFlags();
	}

	public void addEndBlockInstruction(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("CodeBlock.addEndBlockInstruction 0x%08X", address));
		}

		CodeInstruction codeInstruction = new EndBlockCodeInstruction(address);

		insertInstruction(address, codeInstruction);
	}

	public void setIsBranchTarget(int address) {
		if (log.isTraceEnabled()) {
			log.trace("CodeBlock.setIsBranchTarget 0x" + Integer.toHexString(address).toUpperCase());
		}

		CodeInstruction codeInstruction = getCodeInstruction(address);
		if (codeInstruction != null) {
			codeInstruction.setBranchTarget(true);
		}
	}

	public int getStartAddress() {
		return startAddress;
	}

	public int getLowestAddress() {
		return lowestAddress;
	}

	public int getHighestAddress() {
		return highestAddress;
	}

	public int getLength() {
		return (getHighestAddress() - getLowestAddress()) / 4 + 1;
	}

	public CodeInstruction getCodeInstruction(int address) {
	    if (currentSequence != null) {
            return currentSequence.getCodeSequence().getCodeInstruction(address);
	    }

	    for (CodeInstruction codeInstruction : codeInstructions) {
			if (codeInstruction.getAddress() == address) {
				return codeInstruction;
			}
		}

		return null;
	}

	public int getCodeInstructionOpcode(int rawAddress) {
		int address = rawAddress & Memory.addressMask;
		return memoryRanges.getValue(address);
	}

	public String getClassName() {
	    return CompilerContext.getClassName(getStartAddress(), getInstanceIndex());
	}

	public String getInternalClassName() {
		return getInternalName(getClassName());
	}

	private String getInternalName(String name) {
		return name.replace('.', '/');
	}

	@SuppressWarnings("unchecked")
	private Class<IExecutable> loadExecutable(CompilerContext context, String className, byte[] bytes) throws ClassFormatError {
        try {
            // Try to define a new class for this executable.
            return (Class<IExecutable>) context.getClassLoader().defineClass(className, bytes);
        } catch (ClassFormatError e) {
    		// This exception is catched by the Compiler
    		throw e;
        } catch (LinkageError le) {
        	// If the class already exists, try finding it in this context.
            try {
                return (Class<IExecutable>)context.getClassLoader().findClass(className);
            } catch (ClassNotFoundException cnfe) {
                // Return null if none of the above work.
                return null;
            }
        }
	}

	private void addConstructor(ClassVisitor cv) {
	    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
	    mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, objectInternalName, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
	    mv.visitEnd();
	}

    private void addNonStaticMethods(CompilerContext context, ClassVisitor cv) {
    	MethodVisitor mv;

    	// public int exec(int returnAddress, int alternativeReturnAddress, boolean isJump) throws Exception;
    	mv = cv.visitMethod(Opcodes.ACC_PUBLIC, context.getExecMethodName(), context.getExecMethodDesc(), null, exceptions);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassName(), context.getStaticExecMethodName(), context.getStaticExecMethodDesc(), false);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // private static IExecutable e;
        FieldVisitor fv = cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, context.getReplaceFieldName(), executableDescriptor, null, null);
        fv.visitEnd();

        // public void setExecutable(IExecutable e);
        mv = cv.visitMethod(Opcodes.ACC_PUBLIC, context.getReplaceMethodName(), context.getReplaceMethodDesc(), null, exceptions);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, getClassName(), context.getReplaceFieldName(), executableDescriptor);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 2);
        mv.visitEnd();

        // public IExecutable getExecutable();
        mv = cv.visitMethod(Opcodes.ACC_PUBLIC, context.getGetMethodName(), context.getGetMethodDesc(), null, exceptions);
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, getClassName(), context.getReplaceFieldName(), executableDescriptor);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void addCodeSequence(List<CodeSequence> codeSequences, CodeSequence codeSequence) {
    	if (codeSequence != null) {
    		if (codeSequence.getLength() > 1) {
    			codeSequences.add(codeSequence);
    		}
    	}
    }

    private void generateCodeSequences(List<CodeSequence> codeSequences, int sequenceMaxInstructions) {
        CodeSequence currentCodeSequence = null;

        int nextAddress = 0;
        final int sequenceMaxInstructionsWithDelay = sequenceMaxInstructions - 1;
        for (CodeInstruction codeInstruction : codeInstructions) {
            int address = codeInstruction.getAddress();
            if (address < nextAddress) {
                // Skip it
            } else {
                if (codeInstruction.hasFlags(Instruction.FLAG_CANNOT_BE_SPLIT)) {
                	addCodeSequence(codeSequences, currentCodeSequence);
                    currentCodeSequence = null;
                    if (codeInstruction.hasFlags(Instruction.FLAG_HAS_DELAY_SLOT)) {
                        nextAddress = address + 8;
                    }
                } else if (codeInstruction.isBranchTarget()) {
                	addCodeSequence(codeSequences, currentCodeSequence);
                    currentCodeSequence = new CodeSequence(address);
                } else {
                    if (currentCodeSequence == null) {
                        currentCodeSequence = new CodeSequence(address);
                    } else if (currentCodeSequence.getLength() + codeInstruction.getLength() > sequenceMaxInstructionsWithDelay) {
                    	boolean doSplit = false;
                		if (currentCodeSequence.getLength() + codeInstruction.getLength() > sequenceMaxInstructions) {
                			doSplit = true;
                		} else if (codeInstruction.hasFlags(Instruction.FLAG_HAS_DELAY_SLOT)) {
                			doSplit = true;
                		}
                    	if (doSplit) {
                        	addCodeSequence(codeSequences, currentCodeSequence);
                            currentCodeSequence = new CodeSequence(address);
                    	}
                    }
                	currentCodeSequence.setEndAddress(codeInstruction.getEndAddress());
                }
            }
        }

        addCodeSequence(codeSequences, currentCodeSequence);
    }

    private CodeSequence findCodeSequence(CodeInstruction codeInstruction, List<CodeSequence> codeSequences, CodeSequence currentCodeSequence) {
        int address = codeInstruction.getAddress();

        if (currentCodeSequence != null) {
            if (currentCodeSequence.isInside(address)) {
                return currentCodeSequence;
            }
        }

        for (CodeSequence codeSequence : codeSequences) {
            if (codeSequence.isInside(address)) {
                return codeSequence;
            }
        }

        return null;
    }

    private void splitCodeSequences(CompilerContext context, int methodMaxInstructions) {
        List<CodeSequence> codeSequences = new ArrayList<CodeSequence>();

        generateCodeSequences(codeSequences, methodMaxInstructions);
        Collections.sort(codeSequences);

        int currentMethodInstructions = codeInstructions.size();
        List<CodeSequence> sequencesToBeSplit = new ArrayList<CodeSequence>();
        for (CodeSequence codeSequence : codeSequences) {
            sequencesToBeSplit.add(codeSequence);
            if (log.isDebugEnabled()) {
                log.debug("Sequence to be split: " + codeSequence.toString());
            }
            currentMethodInstructions -= codeSequence.getLength();
            if (currentMethodInstructions <= methodMaxInstructions) {
                break;
            }
        }

        CodeSequence currentCodeSequence = null;
        for (ListIterator<CodeInstruction> lit = codeInstructions.listIterator(); lit.hasNext(); ) {
            CodeInstruction codeInstruction = lit.next();
            CodeSequence codeSequence = findCodeSequence(codeInstruction, sequencesToBeSplit, currentCodeSequence);
            if (codeSequence != null) {
                lit.remove();
                if (codeSequence.getInstructions().isEmpty()) {
                    codeSequence.addInstruction(codeInstruction);
                    SequenceCodeInstruction sequenceCodeInstruction = new SequenceCodeInstruction(codeSequence);
                    lit.add(sequenceCodeInstruction);
                    sequenceCodeInstructions.add(sequenceCodeInstruction);
                } else {
                    codeSequence.addInstruction(codeInstruction);
                }
                currentCodeSequence = codeSequence;
            }
        }
    }

    private void scanNativeCodeSequences(CompilerContext context) {
    	NativeCodeManager nativeCodeManager = context.getNativeCodeManager();
    	for (ListIterator<CodeInstruction> lit = codeInstructions.listIterator(); lit.hasNext(); ) {
    		CodeInstruction codeInstruction = lit.next();
    		NativeCodeSequence nativeCodeSequence = nativeCodeManager.getNativeCodeSequence(codeInstruction, this);
    		if (nativeCodeSequence != null) {
    			if (nativeCodeSequence.isHook()) {
    				HookCodeInstruction hookCodeInstruction = new HookCodeInstruction(nativeCodeSequence, codeInstruction);

    				// Replace the current code instruction by the hook code instruction
    				lit.remove();
    				lit.add(hookCodeInstruction);
    			} else {
    				NativeCodeInstruction nativeCodeInstruction = new NativeCodeInstruction(codeInstruction.getAddress(), nativeCodeSequence);

	    			if (nativeCodeInstruction.isBranching()) {
	    				setIsBranchTarget(nativeCodeInstruction.getBranchingTo());
	    			}

	    			if (nativeCodeSequence.isWholeCodeBlock()) {
	    				codeInstructions.clear();
	    				codeInstructions.add(nativeCodeInstruction);
	    			} else {
	    				// Remove the first opcode that started this native code sequence
		    			lit.remove();

		    			// Add any code instructions that need to be inserted before
		    			// the native code sequence
		    			List<CodeInstruction> beforeCodeInstructions = nativeCodeSequence.getBeforeCodeInstructions();
		    			if (beforeCodeInstructions != null) {
		    				for (CodeInstruction beforeCodeInstruction : beforeCodeInstructions) {
		    					CodeInstruction newCodeInstruction = new CodeInstruction(beforeCodeInstruction);
		    					newCodeInstruction.setAddress(codeInstruction.getAddress());

		    					lit.add(newCodeInstruction);
		    				}
		    			}

		    			// Add the native code sequence itself
		    			lit.add(nativeCodeInstruction);

		    			// Remove the further opcodes from the native code sequence
		    			for (int i = nativeCodeSequence.getNumOpcodes() - 1; i > 0 && lit.hasNext(); i--) {
		    				lit.next();
		    				lit.remove();
		    			}
	    			}
    			}
    		}
    	}
    }

    private void prepare(CompilerContext context, int methodMaxInstructions) {
    	memoryRanges.updateValues();

    	scanNativeCodeSequences(context);

    	if (codeInstructions.size() > methodMaxInstructions) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Splitting %s (%d/%d)", getClassName(), codeInstructions.size(), methodMaxInstructions));
            }
            splitCodeSequences(context, methodMaxInstructions);
        }
    }

    private void compile(CompilerContext context, MethodVisitor mv, List<CodeInstruction> codeInstructions) {
    	context.optimizeSequence(codeInstructions);

    	int numberInstructionsToBeSkipped = 0;
        for (CodeInstruction codeInstruction : codeInstructions) {
            if (numberInstructionsToBeSkipped > 0) {
            	if (!context.isSkipDelaySlot() && codeInstruction.isBranchTarget()) {
            		context.compileDelaySlotAsBranchTarget(codeInstruction);
            	}
            	numberInstructionsToBeSkipped--;

            	if (numberInstructionsToBeSkipped <= 0) {
                    context.skipInstructions(0, false);
            	}
            } else {
                codeInstruction.compile(context, mv);
                numberInstructionsToBeSkipped = context.getNumberInstructionsToBeSkipped();
            }
        }
    }

    private Class<IExecutable> interpret(CompilerContext context) {
		Class<IExecutable> compiledClass = null;
    	
		context.setCodeBlock(this);
		String className = getInternalClassName();
		if (log.isInfoEnabled()) {
		    log.info("Compiling for Interpreter " + className);
		}

        int computeFlag = ClassWriter.COMPUTE_FRAMES;
		if (context.isAutomaticMaxLocals() || context.isAutomaticMaxStack()) {
		    computeFlag |= ClassWriter.COMPUTE_MAXS;
		}
    	ClassWriter cw = new ClassWriter(computeFlag);
    	ClassVisitor cv = cw;
    	if (log.isDebugEnabled()) {
    		cv = new CheckClassAdapter(cv);
    	}

    	StringWriter debugOutput = null;
    	if (log.isTraceEnabled()) {
    	    debugOutput = new StringWriter();
    	    PrintWriter debugPrintWriter = new PrintWriter(debugOutput);
    	    cv = new TraceClassVisitor(cv, debugPrintWriter);
    	}
    	cv.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null, objectInternalName, interfacesForExecutable);
    	context.startClass(cv);

    	addConstructor(cv);
    	addNonStaticMethods(context, cv);

    	MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, context.getStaticExecMethodName(), context.getStaticExecMethodDesc(), null, exceptions);
        mv.visitCode();
        context.setMethodVisitor(mv);
        context.startMethod();

        context.compileExecuteInterpreter(getStartAddress());

        mv.visitMaxs(context.getMaxStack(), context.getMaxLocals());
        mv.visitEnd();

        cv.visitEnd();

    	if (debugOutput != null) {
    	    log.trace(debugOutput.toString());
    	}

	    compiledClass = loadExecutable(context, className, cw.toByteArray());

    	return compiledClass;
    }

    private Class<IExecutable> compile(CompilerContext context) throws ClassFormatError {
		Class<IExecutable> compiledClass = null;

		context.setCodeBlock(this);
		String className = getInternalClassName();
		if (log.isDebugEnabled()) {
			String functionName = Utilities.getFunctionNameByAddress(getStartAddress());

			if (functionName != null) {
				log.debug(String.format("Compiling %s (%s)", className, functionName));
			} else {
				log.debug(String.format("Compiling %s", className));
			}
		}

        prepare(context, context.getMethodMaxInstructions());

        currentSequence = null;
        int computeFlag = ClassWriter.COMPUTE_FRAMES;
		if (context.isAutomaticMaxLocals() || context.isAutomaticMaxStack()) {
		    computeFlag |= ClassWriter.COMPUTE_MAXS;
		}
    	ClassWriter cw = new ClassWriter(computeFlag);
    	ClassVisitor cv = cw;
    	if (log.isDebugEnabled()) {
    		cv = new CheckClassAdapter(cv);
    	}
        StringWriter debugOutput = null;
    	if (log.isTraceEnabled()) {
    	    debugOutput = new StringWriter();
    	    PrintWriter debugPrintWriter = new PrintWriter(debugOutput);
    	    cv = new TraceClassVisitor(cv, debugPrintWriter);
    	}
    	cv.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null, objectInternalName, interfacesForExecutable);
    	context.startClass(cv);

    	addConstructor(cv);
    	addNonStaticMethods(context, cv);

    	MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, context.getStaticExecMethodName(), context.getStaticExecMethodDesc(), null, exceptions);
        mv.visitCode();
        context.setMethodVisitor(mv);
        context.startMethod();

        // Jump to the block start if other instructions have been inserted in front
        if (!codeInstructions.isEmpty() && codeInstructions.getFirst().getAddress() != getStartAddress()) {
        	CodeInstruction startCodeInstruction = getCodeInstruction(getStartAddress());
        	if (startCodeInstruction != null) {
        		mv.visitJumpInsn(Opcodes.GOTO, startCodeInstruction.getLabel());
        	}
        }

        compile(context, mv, codeInstructions);
        mv.visitMaxs(context.getMaxStack(), context.getMaxLocals());
        mv.visitEnd();

        for (SequenceCodeInstruction sequenceCodeInstruction : sequenceCodeInstructions) {
            if (log.isDebugEnabled()) {
                log.debug("Compiling Sequence " + sequenceCodeInstruction.getMethodName(context));
            }
            currentSequence = sequenceCodeInstruction;
            mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, sequenceCodeInstruction.getMethodName(context), "()V", null, exceptions);
            mv.visitCode();
            context.setMethodVisitor(mv);
            context.startSequenceMethod();

            compile(context, mv, sequenceCodeInstruction.getCodeSequence().getInstructions());

            context.endSequenceMethod();
            mv.visitMaxs(context.getMaxStack(), context.getMaxLocals());
            mv.visitEnd();
        }
        currentSequence = null;

        cv.visitEnd();

    	if (debugOutput != null) {
    	    log.trace(debugOutput.toString());
    	}

    	try {
    		compiledClass = loadExecutable(context, className, cw.toByteArray());
    	} catch (NullPointerException e) {
    		log.error("Error while compiling " + className + ": " + e);
    	}

    	return compiledClass;
	}

    public IExecutable getExecutable() {
        return executable;
    }

    public synchronized IExecutable getExecutable(CompilerContext context) throws ClassFormatError {
	    if (executable == null) {
	        Class<IExecutable> classExecutable = compile(context);
	        if (classExecutable != null) {
	            try {
                    executable = classExecutable.newInstance();
                } catch (InstantiationException e) {
                    log.error(e);
                } catch (IllegalAccessException e) {
                    log.error(e);
                }
	        }
	    }

	    return executable;
	}

    public synchronized IExecutable getInterpretedExecutable(CompilerContext context) {
    	if (executable == null) {
	        Class<IExecutable> classExecutable = interpret(context);
	        if (classExecutable != null) {
	            try {
                    executable = classExecutable.newInstance();
                } catch (InstantiationException e) {
                    log.error(e);
                } catch (IllegalAccessException e) {
                    log.error(e);
                }
	        }
    	}

    	return executable;
    }

    public int getInstanceIndex() {
    	return instanceIndex;
    }

    public int getNewInstanceIndex() {
    	return nextInstanceIndex++;
    }

	public Instruction[] getInterpretedInstructions() {
		return interpretedInstructions;
	}

	public void setInterpretedInstructions(Instruction[] interpretedInstructions) {
		this.interpretedInstructions = interpretedInstructions;
	}

	public int[] getInterpretedOpcodes() {
		return interpretedOpcodes;
	}

	public void setInterpretedOpcodes(int[] interpretedOpcodes) {
		this.interpretedOpcodes = interpretedOpcodes;
	}

	private boolean areOpcodesChanged() {
		return memoryRanges.areValuesChanged();
	}

	public boolean isOverlappingWithAddressRange(int address, int size) {
		address &= Memory.addressMask;

		// Fast check against the lowest & highest addresses
		if (address > (highestAddress & Memory.addressMask)) {
			return false;
		}
		if (address + size < (lowestAddress & Memory.addressMask)) {
			return false;
		}

		// Full check on all the memory ranges
		return memoryRanges.isOverlappingWithAddressRange(address, size);
	}

	public boolean isInternal() {
    	int addr = getStartAddress();
    	return addr < INTERNAL_THREAD_ADDRESS_END && addr >= INTERNAL_THREAD_ADDRESS_START;
	}

    public int getFlags() {
        return flags;
    }

    public boolean hasFlags(int testFlags) {
        return (flags & testFlags) == testFlags;
    }

    public void addCodeBlock() {
		RuntimeContext.addCodeBlock(startAddress, this);
    }

	public HLEModuleFunction getHLEFunction() {
		return hleFunction;
	}

	public void setHLEFunction(HLEModuleFunction hleFunction) {
		this.hleFunction = hleFunction;
	}

	public boolean isHLEFunction() {
		return hleFunction != null;
	}

	public IAction getUpdateOpcodesAction() {
		return updateOpcodesAction;
	}

	public void setUpdateOpcodesAction(IAction updateOpcodesAction) {
		this.updateOpcodesAction = updateOpcodesAction;
	}

	public void free() {
		startAddress = 0;
		lowestAddress = 0;
		highestAddress = 0;
		if (codeInstructions != null) {
			codeInstructions.clear();
			codeInstructions = null;
		}
		if (sequenceCodeInstructions != null) {
			sequenceCodeInstructions.clear();
			sequenceCodeInstructions = null;
		}
		currentSequence = null;
		executable = null;
		instanceIndex = 0;
		interpretedInstructions = null;
		interpretedOpcodes = null;
		if (memoryRanges != null) {
			memoryRanges.clear();
			memoryRanges = null;
		}
		flags = 0;
		hleFunction = null;
		updateOpcodesAction = null;
	}

	private boolean isDebuggerChanged() {
		if (debuggerEnabled) {
			// No need to recompile when closing the debugger
			return false;
		}
		return debuggerEnabled != RuntimeContext.enableDebugger;
	}

	public boolean isNoLongerValid() {
		return isDebuggerChanged() || areOpcodesChanged();
	}

	@Override
	public String toString() {
		return String.format("CodeBlock 0x%08X[0x%08X-0x%08X]", getStartAddress(), getLowestAddress(), getHighestAddress());
	}
}
