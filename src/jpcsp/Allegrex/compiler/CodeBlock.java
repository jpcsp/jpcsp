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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeInstruction;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeManager;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeSequence;

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

	public CodeBlock(int startAddress, int instanceCount) {
		this.startAddress = startAddress;
		this.instanceIndex = instanceCount;
		lowestAddress = startAddress;
		highestAddress = startAddress;

		RuntimeContext.addCodeBlock(startAddress, this);
	}

	public void addInstruction(int address, int opcode, Instruction insn, boolean isBranchTarget, boolean isBranching, int branchingTo) {
		if (Compiler.log.isTraceEnabled()) {
			Compiler.log.trace("CodeBlock.addInstruction 0x" + Integer.toHexString(address).toUpperCase() + " - " + insn.disasm(address, opcode));
		}

		CodeInstruction codeInstruction = new CodeInstruction(address, opcode, insn, isBranchTarget, isBranching, branchingTo);

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

		if (address > highestAddress) {
			highestAddress = address;
		}
	}

	public void setIsBranchTarget(int address) {
		if (Compiler.log.isTraceEnabled()) {
			Compiler.log.trace("CodeBlock.setIsBranchTarget 0x" + Integer.toHexString(address).toUpperCase());
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
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, objectInternalName, "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
	    mv.visitEnd();
	}

    private void addNonStaticMethods(CompilerContext context, ClassVisitor cv) {
    	MethodVisitor mv;

    	// public int exec(int returnAddress, int alternativeReturnAddress, boolean isJump) throws Exception;
    	mv = cv.visitMethod(Opcodes.ACC_PUBLIC, context.getExecMethodName(), context.getExecMethodDesc(), null, exceptions);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassName(), context.getStaticExecMethodName(), context.getStaticExecMethodDesc());
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(3, 4);
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
                    } else if (currentCodeSequence.getLength() >= sequenceMaxInstructionsWithDelay) {
                    	boolean doSplit = false;
                		if (currentCodeSequence.getLength() >= sequenceMaxInstructions) {
                			doSplit = true;
                		} else if (codeInstruction.hasFlags(Instruction.FLAG_HAS_DELAY_SLOT)) {
                			doSplit = true;
                		}
                    	if (doSplit) {
                        	addCodeSequence(codeSequences, currentCodeSequence);
                            currentCodeSequence = new CodeSequence(address);
                    	}
                    }
                    currentCodeSequence.setEndAddress(address);
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
            if (Compiler.log.isDebugEnabled()) {
                Compiler.log.debug("Sequence to be split: " + codeSequence.toString());
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

    private void prepare(CompilerContext context, int methodMaxInstructions) {
    	scanNativeCodeSequences(context);

    	if (codeInstructions.size() > methodMaxInstructions) {
            if (Compiler.log.isInfoEnabled()) {
                Compiler.log.info("Splitting " + getClassName() + " (" + codeInstructions.size() + "/" + methodMaxInstructions + ")");
            }
            splitCodeSequences(context, methodMaxInstructions);
        }
    }

    private void compile(CompilerContext context, MethodVisitor mv, List<CodeInstruction> codeInstructions) {
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
		if (Compiler.log.isInfoEnabled()) {
		    Compiler.log.info("Compiling for Interpreter " + className);
		}

        int computeFlag = ClassWriter.COMPUTE_FRAMES;
		if (context.isAutomaticMaxLocals() || context.isAutomaticMaxStack()) {
		    computeFlag |= ClassWriter.COMPUTE_MAXS;
		}
    	ClassWriter cw = new ClassWriter(computeFlag);
    	ClassVisitor cv = cw;
    	if (Compiler.log.isDebugEnabled()) {
    		cv = new CheckClassAdapter(cv);
    	}

    	StringWriter debugOutput = null;
    	if (Compiler.log.isDebugEnabled()) {
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
    	    Compiler.log.debug(debugOutput.toString());
    	}

	    compiledClass = loadExecutable(context, className, cw.toByteArray());

    	return compiledClass;
    }

    private Class<IExecutable> compile(CompilerContext context) throws ClassFormatError {
		Class<IExecutable> compiledClass = null;

		context.setCodeBlock(this);
		String className = getInternalClassName();
		if (Compiler.log.isDebugEnabled()) {
		    Compiler.log.debug("Compiling " + className);
		}

        prepare(context, context.getMethodMaxInstructions());

        currentSequence = null;
        int computeFlag = ClassWriter.COMPUTE_FRAMES;
		if (context.isAutomaticMaxLocals() || context.isAutomaticMaxStack()) {
		    computeFlag |= ClassWriter.COMPUTE_MAXS;
		}
    	ClassWriter cw = new ClassWriter(computeFlag);
    	ClassVisitor cv = cw;
    	if (Compiler.log.isDebugEnabled()) {
    		cv = new CheckClassAdapter(cv);
    	}
        StringWriter debugOutput = null;
    	if (Compiler.log.isTraceEnabled()) {
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
            mv.visitJumpInsn(Opcodes.GOTO, getCodeInstruction(getStartAddress()).getLabel());
        }

        compile(context, mv, codeInstructions);
        mv.visitMaxs(context.getMaxStack(), context.getMaxLocals());
        mv.visitEnd();

        for (SequenceCodeInstruction sequenceCodeInstruction : sequenceCodeInstructions) {
            if (Compiler.log.isDebugEnabled()) {
                Compiler.log.debug("Compiling Sequence " + sequenceCodeInstruction.getMethodName(context));
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
    	    Compiler.log.trace(debugOutput.toString());
    	}

    	try {
    		compiledClass = loadExecutable(context, className, cw.toByteArray());
    	} catch (NullPointerException e) {
    		Compiler.log.error("Error while compiling " + className + ": " + e);
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
                    Compiler.log.error(e);
                } catch (IllegalAccessException e) {
                    Compiler.log.error(e);
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
                    Compiler.log.error(e);
                } catch (IllegalAccessException e) {
                    Compiler.log.error(e);
                }
	        }
    	}

    	return executable;
    }

    public int getInstanceIndex() {
    	return instanceIndex;
    }

    public int getNewInstanceIndex() {
    	instanceIndex++;
    	return instanceIndex;
    }
}
