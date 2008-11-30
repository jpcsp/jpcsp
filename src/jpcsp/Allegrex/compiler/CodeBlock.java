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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.ListIterator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import jpcsp.Allegrex.Common.Instruction;

/**
 * @author gid15
 *
 */
public class CodeBlock {
	private int startAddress;
	private LinkedList<CodeInstruction> codeInstructions = new LinkedList<CodeInstruction>();
	private IExecutable executable = null;
	private final static String objectInternalName = Type.getInternalName(Object.class);
	private final static String[] interfacesForExecutable = new String[] { Type.getInternalName(IExecutable.class) };

	public CodeBlock(int startAddress) {
		this.startAddress = startAddress;

		RuntimeContext.addCodeBlock(startAddress, this);
	}

	public void addInstruction(int address, int opcode, Instruction insn, boolean isBranchTarget, boolean isBranching, int branchingTo) {
		if (Compiler.log.isDebugEnabled()) {
			Compiler.log.debug("CodeBlock.addInstruction 0x" + Integer.toHexString(address).toUpperCase() + " - " + insn.disasm(address, opcode));
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
		}
	}

	public void setIsBranchTarget(int address) {
		if (Compiler.log.isDebugEnabled()) {
			Compiler.log.debug("CodeBlock.setIsBranchTarget 0x" + Integer.toHexString(address).toUpperCase());
		}

		CodeInstruction codeInstruction = getCodeInstruction(address);
		if (codeInstruction != null) {
			codeInstruction.setBranchTarget(true);
		}
	}

	public int getStartAddress() {
		return startAddress;
	}

	public CodeInstruction getCodeInstruction(int address) {
		for (CodeInstruction codeInstruction : codeInstructions) {
			if (codeInstruction.getAddress() == address) {
				return codeInstruction;
			}
		}

		return null;
	}

	public String getClassName() {
	    return CompilerContext.getClassName(getStartAddress());
	}

	public String getInternalClassName() {
		return getInternalName(getClassName());
	}

	private String getInternalName(String name) {
		return name.replace('.', '/');
	}

	@SuppressWarnings("unchecked")
	private Class<IExecutable> loadExecutable(CompilerContext context, String className, byte[] bytes) {
    	return (Class<IExecutable>) context.getClassLoader().defineClass(className, bytes);
	}

	private void addConstructor(ClassVisitor cv) {
	    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
	    mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, objectInternalName, "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
	    mv.visitEnd();
	}

    private void addNonStaticMethods(CompilerContext context, ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, context.getExecMethodName(), context.getExecMethodDesc(), null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassName(), context.getStaticExecMethodName(), context.getStaticExecMethodDesc());
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

	private Class<IExecutable> compile(CompilerContext context) {
		Class<IExecutable> compiledClass = null;

		context.setCodeBlock(this);
		String className = getInternalClassName();

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

    	addConstructor(cv);
    	addNonStaticMethods(context, cv);

    	MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, context.getStaticExecMethodName(), context.getStaticExecMethodDesc(), null, null);
        mv.visitCode();
        context.startMethod(mv);

        // Jump to the block start if other instructions have been inserted in front
        if (!codeInstructions.isEmpty() && codeInstructions.getFirst().getAddress() != getStartAddress()) {
            mv.visitJumpInsn(Opcodes.GOTO, getCodeInstruction(getStartAddress()).getLabel());
        }

        boolean skipNextInstruction = false;
    	for (CodeInstruction codeInstruction : codeInstructions) {
    	    if (skipNextInstruction) {
    	        skipNextInstruction = false;
    	    } else {
    	        codeInstruction.compile(context, mv);
    	        skipNextInstruction = context.isSkipNextIntruction();
    	        context.setSkipNextIntruction(false);
    	    }
		}
    	mv.visitMaxs(context.getMaxLocals(), context.getMaxStack());
        mv.visitEnd();

    	cv.visitEnd();

    	if (debugOutput != null) {
    	    Compiler.log.debug(debugOutput.toString());
    	}

    	compiledClass = loadExecutable(context, className, cw.toByteArray());

    	return compiledClass;
	}

	public synchronized IExecutable getExecutable() {
	    if (executable == null) {
	        CompilerContext recompilerContext = new CompilerContext(Compiler.getInstance().getClassLoader());
	        Class<IExecutable> classExecutable = compile(recompilerContext);
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
}
