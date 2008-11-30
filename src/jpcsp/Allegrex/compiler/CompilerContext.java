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

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Instructions;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.FpuState.Fcr31;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @author gid15
 *
 */
public class CompilerContext {
	private CompilerClassLoader classLoader;
	private CodeBlock codeBlock;
	private boolean skipNextIntruction;
	private static final boolean storeGprLocal = true;
	private static final boolean storeProcessorLocal = true;
	private static final int LOCAL_RETURN_ADDRESS = 0;
    private static final int LOCAL_IS_JUMP = 1;
    private static final int LOCAL_PROCESSOR = 2;
    private static final int LOCAL_GPR = 3;
    private static final int LOCAL_INSTRUCTION_COUNT = 4;
    private static final int LOCAL_MAX = 5;
    private static final int STACK_MAX = 3;
    public Set<Integer> analysedAddresses = new HashSet<Integer>();
    public Stack<Integer> blocksToBeAnalysed = new Stack<Integer>();
    private int currentInstructionCount;
    private static final String runtimeContextInternalName = Type.getInternalName(RuntimeContext.class);
    private static final String processorDescriptor = Type.getDescriptor(Processor.class);
    private static final String instructionsInternalName = Type.getInternalName(Instructions.class);
    private static final String instructionInternalName = Type.getInternalName(Instruction.class);
    private static final String instructionDescriptor = Type.getDescriptor(Instruction.class);
    private static final String sceKernalThreadInfoInternalName = Type.getInternalName(SceKernelThreadInfo.class);
    private static final String sceKernalThreadInfoDescriptor = Type.getDescriptor(SceKernelThreadInfo.class);

    public CompilerContext(CompilerClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public CompilerClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(CompilerClassLoader classLoader) {
		this.classLoader = classLoader;
	}

    public CodeBlock getCodeBlock() {
        return codeBlock;
    }

    public void setCodeBlock(CodeBlock codeBlock) {
        this.codeBlock = codeBlock;
    }

    public boolean isSkipNextIntruction() {
        return skipNextIntruction;
    }

    public void setSkipNextIntruction(boolean skipNextIntruction) {
        this.skipNextIntruction = skipNextIntruction;
    }

    private void loadGpr(MethodVisitor mv) {
    	if (storeGprLocal) {
    		mv.visitVarInsn(Opcodes.ALOAD, LOCAL_GPR);
    	} else {
    		mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "gpr", "[I");
    	}
    }

    private void loadProcessor(MethodVisitor mv) {
    	if (storeProcessorLocal) {
    		mv.visitVarInsn(Opcodes.ALOAD, LOCAL_PROCESSOR);
    	} else {
            mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "processor", processorDescriptor);
    	}
    }

    public void loadRegister(MethodVisitor mv, int reg) {
    	loadGpr(mv);
        mv.visitLdcInsn(reg);
        mv.visitInsn(Opcodes.IALOAD);
    }

    public void storeRegister(MethodVisitor mv, int reg) {
    	loadGpr(mv);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(reg);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitInsn(Opcodes.IASTORE);
    }

    public void storeRegister(MethodVisitor mv, int reg, int constantValue) {
    	loadGpr(mv);
        mv.visitLdcInsn(reg);
        mv.visitLdcInsn(constantValue);
        mv.visitInsn(Opcodes.IASTORE);
    }

    public void loadFcr31(MethodVisitor mv) {
    	loadProcessor(mv);
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Processor.class), "cpu", Type.getDescriptor(CpuState.class));
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(CpuState.class), "fcr31", Type.getDescriptor(Fcr31.class));
    }

    public void loadFcr31c(MethodVisitor mv) {
    	loadFcr31(mv);
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Fcr31.class), "c", "Z");
    }

    private void loadLocalVar(MethodVisitor mv, int localVar) {
        mv.visitVarInsn(Opcodes.ILOAD, localVar);
    }

    private void loadInstruction(MethodVisitor mv, Instruction insn) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, instructionsInternalName, insn.name().replace('.', '_').toUpperCase(), instructionDescriptor);
    }

    public void visitJump(MethodVisitor mv) {
    	flushInstructionCount(mv, true, false);

    	//
        //      jr x
        //
        // translates to:
        //
        //      while (true) {
        //          if (x == returnAddress || isJump) {
        //              return x;
        //          }
        //          x = RuntimeContext.jump(x, returnAddress)
        //      }
        //
        Label returnLabel = new Label();
        Label jumpLabel = new Label();
        Label jumpLoop = new Label();

        mv.visitLabel(jumpLoop);
        mv.visitInsn(Opcodes.DUP);
        loadLocalVar(mv, LOCAL_RETURN_ADDRESS);
        visitJump(mv, Opcodes.IF_ICMPEQ, returnLabel);
        loadLocalVar(mv, LOCAL_IS_JUMP);
        visitJump(mv, Opcodes.IFEQ, jumpLabel);

        mv.visitLabel(returnLabel);
        endMethod(mv);
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitLabel(jumpLabel);
        loadLocalVar(mv, LOCAL_RETURN_ADDRESS);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "jump", "(II)I");
        visitJump(mv, Opcodes.GOTO, jumpLoop);
    }

    public void visitCall(MethodVisitor mv, int address, int returnAddress, int returnRegister) {
        mv.visitLdcInsn(returnAddress);
        if (returnRegister != 0) {
            mv.visitInsn(Opcodes.DUP);
            storeRegister(mv, returnRegister);
        }
        mv.visitInsn(Opcodes.ICONST_0);		// isJump = false
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassName(address), getStaticExecMethodName(), getStaticExecMethodDesc());
        mv.visitInsn(Opcodes.POP);
    }

    public void visitCall(MethodVisitor mv, int returnAddress, int returnRegister) {
        if (returnRegister != 0) {
            storeRegister(mv, returnRegister, returnAddress);
        }
        mv.visitLdcInsn(returnAddress);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "call", "(II)V");
    }

    public void visitIntepreterCall(MethodVisitor mv, int opcode, Instruction insn) {
    	loadInstruction(mv, insn);
        loadProcessor(mv);
        mv.visitLdcInsn(opcode);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, instructionInternalName, "interpret", "(" + processorDescriptor + "I)V");
    }

    public void visitSyscall(MethodVisitor mv, int opcode, Instruction insn) {
    	flushInstructionCount(mv, false, false);

    	int code = (opcode >> 6) & 0x000FFFFF;
    	mv.visitLdcInsn(code);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "syscall", "(I)V");

        if (storeGprLocal) {
        	// Reload "gpr", it could have been changed due to a thread switch
        	mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "gpr", "[I");
            mv.visitVarInsn(Opcodes.ASTORE, LOCAL_GPR);
        }
    }

    public void startMethod(MethodVisitor mv) {
        if (RuntimeContext.debugCodeBlockCalls && RuntimeContext.log.isDebugEnabled()) {
        	mv.visitLdcInsn(getCodeBlock().getStartAddress());
        	loadLocalVar(mv, LOCAL_RETURN_ADDRESS);
        	loadLocalVar(mv, LOCAL_IS_JUMP);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debugCodeBlockStart, "(IIZ)V");
        }

        if (storeProcessorLocal) {
        	mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "processor", processorDescriptor);
        	mv.visitVarInsn(Opcodes.ASTORE, LOCAL_PROCESSOR);
        }

        if (storeGprLocal) {
        	mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "gpr", "[I");
            mv.visitVarInsn(Opcodes.ASTORE, LOCAL_GPR);
        }

        if (RuntimeContext.enableIntructionCounting) {
        	currentInstructionCount = 0;
        	mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ISTORE, LOCAL_INSTRUCTION_COUNT);
        }
    }

    private void flushInstructionCount(MethodVisitor mv, boolean local, boolean last) {
        if (RuntimeContext.enableIntructionCounting) {
        	if (local) {
        		if (currentInstructionCount > 0) {
        			mv.visitIincInsn(LOCAL_INSTRUCTION_COUNT, currentInstructionCount);
        		}
        	} else {
		        mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "currentThread", sceKernalThreadInfoDescriptor);
		        mv.visitInsn(Opcodes.DUP);
		        mv.visitFieldInsn(Opcodes.GETFIELD, sceKernalThreadInfoInternalName, "runClocks", "J");
		        mv.visitVarInsn(Opcodes.ILOAD, LOCAL_INSTRUCTION_COUNT);
		        if (currentInstructionCount > 0) {
		        	mv.visitLdcInsn(currentInstructionCount);
			        mv.visitInsn(Opcodes.IADD);
		        }
		        mv.visitInsn(Opcodes.I2L);
		        mv.visitInsn(Opcodes.LADD);
		        mv.visitFieldInsn(Opcodes.PUTFIELD, sceKernalThreadInfoInternalName, "runClocks", "J");
		        if (!last) {
		        	mv.visitInsn(Opcodes.ICONST_0);
		        	mv.visitVarInsn(Opcodes.ISTORE, LOCAL_INSTRUCTION_COUNT);
		        }
        	}
	        currentInstructionCount = 0;
        }
    }

    public void endMethod(MethodVisitor mv) {
        if (RuntimeContext.debugCodeBlockCalls && RuntimeContext.log.isDebugEnabled()) {
            mv.visitInsn(Opcodes.DUP);
        	mv.visitLdcInsn(getCodeBlock().getStartAddress());
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debugCodeBlockEnd, "(II)V");
        }
    	flushInstructionCount(mv, false, true);
    }

    public void startInstruction(MethodVisitor mv, CodeInstruction codeInstruction) {
	    if (RuntimeContext.debugCodeInstruction && RuntimeContext.log.isDebugEnabled()) {
        	mv.visitLdcInsn(codeInstruction.getAddress());
        	mv.visitLdcInsn(codeInstruction.getOpcode());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debugCodeInstructionName, "(II)V");
	    }

	    if (RuntimeContext.enableInstructionTypeCounting) {
	    	loadInstruction(mv, codeInstruction.getInsn());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.instructionTypeCount, "(" + instructionDescriptor + ")V");
	    }

	    if (RuntimeContext.enableIntructionCounting) {
	    	if (codeInstruction.isBranchTarget()) {
	    		flushInstructionCount(mv, true, false);
	    	}
	    	currentInstructionCount++;
	    }

	    if (RuntimeContext.enableDebugger) {
	    	mv.visitLdcInsn(codeInstruction.getAddress());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debuggerName, "(I)V");
	    }
    }

    public void visitJump(MethodVisitor mv, int opcode, Label label) {
    	flushInstructionCount(mv, true, false);
        mv.visitJumpInsn(opcode, label);
    }

    public static String getClassName(int address) {
        return "_S1_" + Integer.toHexString(address).toUpperCase();
    }

    public static int getClassAddress(String name) {
        String hexAddress = name.substring(4);

        return Integer.parseInt(hexAddress, 16);
    }

    public String getExecMethodName() {
        return "exec";
    }

    public String getExecMethodDesc() {
        return "(IZ)I";
    }

    public String getStaticExecMethodName() {
        return "s";
    }

    public String getStaticExecMethodDesc() {
        return "(IZ)I";
    }

    public boolean isAutomaticMaxLocals() {
        return false;
    }

    public int getMaxLocals() {
        return LOCAL_MAX;
    }

    public boolean isAutomaticMaxStack() {
        return false;
    }

    public int getMaxStack() {
        return STACK_MAX;
    }
}
