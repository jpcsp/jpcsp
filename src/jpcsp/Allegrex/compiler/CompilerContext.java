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

import static jpcsp.Allegrex.Common._f0;
import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._sp;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._v1;
import static jpcsp.Allegrex.Common._zr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Instructions;
import jpcsp.Allegrex.VfpuState;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.FpuState.Fcr31;
import jpcsp.Allegrex.VfpuState.Vcr;
import jpcsp.Allegrex.VfpuState.VfpuValue;
import jpcsp.Allegrex.VfpuState.Vcr.PfxDst;
import jpcsp.Allegrex.VfpuState.Vcr.PfxSrc;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeInstruction;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeManager;
import jpcsp.Allegrex.compiler.nativeCode.NativeCodeSequence;
import jpcsp.Allegrex.compiler.nativeCode.Nop;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEUidClass;
import jpcsp.HLE.HLEUidObjectMapping;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.SyscallHandler;
import jpcsp.HLE.TErrorPointer32;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.memory.SafeFastMemory;
import jpcsp.util.ClassAnalyzer;
import jpcsp.util.DurationStatistics;
import jpcsp.util.ClassAnalyzer.ParameterInfo;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @author gid15
 *
 */
public class CompilerContext implements ICompilerContext {
	private CompilerClassLoader classLoader;
	private CodeBlock codeBlock;
	private int numberInstructionsToBeSkipped;
	private boolean skipDelaySlot;
	private MethodVisitor mv;
	private CodeInstruction codeInstruction;
	private static final boolean storeGprLocal = true;
	private static final boolean storeMemoryIntLocal = false;
    private static final int LOCAL_GPR = 0;
    private static final int LOCAL_INSTRUCTION_COUNT = 1;
    private static final int LOCAL_MEMORY_INT = 2;
    private static final int LOCAL_TMP1 = 3;
    private static final int LOCAL_TMP2 = 4;
    private static final int LOCAL_TMP3 = 5;
    private static final int LOCAL_TMP4 = 6;
    private static final int LOCAL_TMP_VD0 = 7;
    private static final int LOCAL_TMP_VD1 = 8;
    private static final int LOCAL_TMP_VD2 = 9;
    private static final int LOCAL_MAX = 10;
    private static final int DEFAULT_MAX_STACK_SIZE = 11;
    private static final int SYSCALL_MAX_STACK_SIZE = 100;
    private static final int LOCAL_ERROR_POINTER = LOCAL_TMP3;
	private boolean enableIntructionCounting = false;
    public Set<Integer> analysedAddresses = new HashSet<Integer>();
    public Stack<Integer> blocksToBeAnalysed = new Stack<Integer>();
    private int currentInstructionCount;
    private int preparedRegisterForStore = -1;
    private boolean memWritePrepared = false;
    private boolean hiloPrepared = false;
    private int methodMaxInstructions;
    private NativeCodeManager nativeCodeManager;
    private final VfpuPfxSrcState vfpuPfxsState = new VfpuPfxSrcState();
    private final VfpuPfxSrcState vfpuPfxtState = new VfpuPfxSrcState();
    private final VfpuPfxDstState vfpuPfxdState = new VfpuPfxDstState();
    private Label interpretPfxLabel = null;
    private boolean pfxVdOverlap = false;
    private static final String runtimeContextInternalName = Type.getInternalName(RuntimeContext.class);
    private static final String processorDescriptor = Type.getDescriptor(Processor.class);
    private static final String cpuDescriptor = Type.getDescriptor(CpuState.class);
    private static final String cpuInternalName = Type.getInternalName(CpuState.class);
    private static final String instructionsInternalName = Type.getInternalName(Instructions.class);
    private static final String instructionInternalName = Type.getInternalName(Instruction.class);
    private static final String instructionDescriptor = Type.getDescriptor(Instruction.class);
    private static final String sceKernalThreadInfoInternalName = Type.getInternalName(SceKernelThreadInfo.class);
    private static final String sceKernalThreadInfoDescriptor = Type.getDescriptor(SceKernelThreadInfo.class);
    private static final String stringDescriptor = Type.getDescriptor(String.class);
    private static final String memoryDescriptor = Type.getDescriptor(Memory.class);
    private static final String memoryInternalName = Type.getInternalName(Memory.class);
    private static final String profilerInternalName = Type.getInternalName(Profiler.class);
    private static final String vfpuValueDescriptor = Type.getDescriptor(VfpuValue.class);
    private static final String vfpuValueInternalName = Type.getInternalName(VfpuValue.class);
	public  static final String executableDescriptor = Type.getDescriptor(IExecutable.class);
	public  static final String executableInternalName = Type.getInternalName(IExecutable.class);
	private static Set<Integer> fastSyscalls;
	private int instanceIndex;
	private NativeCodeSequence preparedCallNativeCodeBlock = null;
	private int maxStackSize = DEFAULT_MAX_STACK_SIZE;
	private CompilerTypeManager compilerTypeManager;

	public CompilerContext(CompilerClassLoader classLoader, int instanceIndex) {
    	Compiler compiler = Compiler.getInstance();
        this.classLoader = classLoader;
        this.instanceIndex = instanceIndex;
        nativeCodeManager = compiler.getNativeCodeManager();
        methodMaxInstructions = compiler.getDefaultMethodMaxInstructions();
        compilerTypeManager = compiler.getCompilerTypeManager();

        // Count instructions only when the profile is enabled or
        // when the statistics are enabled
        if (Profiler.isProfilerEnabled() || DurationStatistics.collectStatistics) {
        	enableIntructionCounting = true;
        }

        if (fastSyscalls == null) {
	        fastSyscalls = new TreeSet<Integer>();
	        addFastSyscall(0x3AD58B8C); // sceKernelSuspendDispatchThread
	        addFastSyscall(0x110DEC9A); // sceKernelUSec2SysClock
	        addFastSyscall(0xC8CD158C); // sceKernelUSec2SysClockWide
	        addFastSyscall(0xBA6B92E2); // sceKernelSysClock2USec 
	        addFastSyscall(0xE1619D7C); // sceKernelSysClock2USecWide 
	        addFastSyscall(0xDB738F35); // sceKernelGetSystemTime
	        addFastSyscall(0x82BC5777); // sceKernelGetSystemTimeWide
	        addFastSyscall(0x369ED59D); // sceKernelGetSystemTimeLow
	        addFastSyscall(0xB5F6DC87); // sceMpegRingbufferAvailableSize
	        addFastSyscall(0xE0D68148); // sceGeListUpdateStallAddr
	        addFastSyscall(0x34B9FA9E); // sceKernelDcacheWritebackInvalidateRangeFunction
	        addFastSyscall(0xE47E40E4); // sceGeEdramGetAddrFunction
	        addFastSyscall(0x1F6752AD); // sceGeEdramGetSizeFunction
	        addFastSyscall(0x74AE582A); // __sceSasGetEnvelopeHeight
	        addFastSyscall(0x68A46B95); // __sceSasGetEndFlag
        }
    }

    private void addFastSyscall(int nid) {
        fastSyscalls.add(HLEModuleManager.getInstance().getSyscallFromNid(nid));
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

    public NativeCodeManager getNativeCodeManager() {
    	return nativeCodeManager;
    }

    private void loadGpr() {
    	if (storeGprLocal) {
    		mv.visitVarInsn(Opcodes.ALOAD, LOCAL_GPR);
    	} else {
    		mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "gpr", "[I");
    	}
    }

    private void loadCpu() {
		mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "cpu", cpuDescriptor);
	}

    private void loadProcessor() {
        mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "processor", processorDescriptor);
    }

    private void loadMemory() {
		mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "memory", memoryDescriptor);
    }

    private void loadModule(String moduleName) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Modules.class), moduleName + "Module", "Ljpcsp/HLE/modules/" + moduleName + ";");
    }

    private void loadFpr() {
		mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "fpr", "[F");
    }

    private void loadVpr() {
		mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "vpr", "[" + vfpuValueDescriptor);
    }

    @Override
    public void loadRegister(int reg) {
    	if (reg == _zr) {
    		loadImm(0);
    	} else {
	    	loadGpr();
	    	loadImm(reg);
	        mv.visitInsn(Opcodes.IALOAD);
    	}
    }

    @Override
    public void loadFRegister(int reg) {
    	loadFpr();
    	loadImm(reg);
        mv.visitInsn(Opcodes.FALOAD);
    }

    private Float getPfxSrcCstValue(VfpuPfxSrcState pfxSrcState, int n) {
    	if (pfxSrcState == null ||
    	    pfxSrcState.isUnknown() ||
    	    !pfxSrcState.pfxSrc.enabled ||
    	    !pfxSrcState.pfxSrc.cst[n]) {
    		return null;
    	}

    	float value = 0.0f;
		switch (pfxSrcState.pfxSrc.swz[n]) {
			case 0:
				value = pfxSrcState.pfxSrc.abs[n] ? 3.0f : 0.0f;
				break;
			case 1:
				value = pfxSrcState.pfxSrc.abs[n] ? (1.0f / 3.0f) : 1.0f;
				break;
			case 2:
				value = pfxSrcState.pfxSrc.abs[n] ? (1.0f / 4.0f) : 2.0f;
				break;
			case 3:
				value = pfxSrcState.pfxSrc.abs[n] ? (1.0f / 6.0f) : 0.5f;
				break;
		}

		if (pfxSrcState.pfxSrc.neg[n]) {
			value = 0.0f - value;
		}

		if (Compiler.log.isTraceEnabled() && pfxSrcState.isKnown() && pfxSrcState.pfxSrc.enabled) {
			Compiler.log.trace(String.format("PFX    %08X - getPfxSrcCstValue %d -> %f", getCodeInstruction().getAddress(), n, value));
		}

		return new Float(value);
    }

    private void convertVFloatToInt() {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "floatToRawIntBits", "(F)I");
    }

    private void convertVIntToFloat() {
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "intBitsToFloat", "(I)F");
    }

    private void applyPfxSrcPostfix(VfpuPfxSrcState pfxSrcState, int n, boolean isFloat) {
    	if (pfxSrcState == null ||
    	    pfxSrcState.isUnknown() ||
    	    !pfxSrcState.pfxSrc.enabled) {
    		return;
    	}

    	if (!isFloat && (pfxSrcState.pfxSrc.abs[n] || pfxSrcState.pfxSrc.neg[n])) {
    		// The value is requested as an "int", first convert to float
    		convertVIntToFloat();
    	}

    	if (pfxSrcState.pfxSrc.abs[n]) {
			if (Compiler.log.isTraceEnabled() && pfxSrcState.isKnown() && pfxSrcState.pfxSrc.enabled) {
				Compiler.log.trace(String.format("PFX    %08X - applyPfxSrcPostfix abs(%d)", getCodeInstruction().getAddress(), n));
			}
    		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "abs", "(F)F");
    	}
    	if (pfxSrcState.pfxSrc.neg[n]) {
			if (Compiler.log.isTraceEnabled() && pfxSrcState.isKnown() && pfxSrcState.pfxSrc.enabled) {
				Compiler.log.trace(String.format("PFX    %08X - applyPfxSrcPostfix neg(%d)", getCodeInstruction().getAddress(), n));
			}
    		mv.visitInsn(Opcodes.FNEG);
    	}

    	if (!isFloat && (pfxSrcState.pfxSrc.abs[n] || pfxSrcState.pfxSrc.neg[n])) {
    		// The value is requested as an "int", convert back from float to int
			convertVFloatToInt();
    	}
    }

    private int getPfxSrcIndex(VfpuPfxSrcState pfxSrcState, int n) {
    	if (pfxSrcState == null ||
    	    pfxSrcState.isUnknown() ||
    	    !pfxSrcState.pfxSrc.enabled ||
    	    pfxSrcState.pfxSrc.cst[n]) {
    		return n;
    	}

		if (Compiler.log.isTraceEnabled() && pfxSrcState.isKnown() && pfxSrcState.pfxSrc.enabled) {
			Compiler.log.trace(String.format("PFX    %08X - getPfxSrcIndex %d -> %d", getCodeInstruction().getAddress(), n, pfxSrcState.pfxSrc.swz[n]));
		}
    	return pfxSrcState.pfxSrc.swz[n];
    }

    private void loadVRegister(int m, int c, int r, boolean isFloat) {
        loadVpr();
        loadImm(VfpuState.getVprIndex(m, c, r));
        mv.visitInsn(Opcodes.AALOAD);
        if (isFloat) {
        	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, vfpuValueInternalName, "getFloat", "()F");
        } else {
        	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, vfpuValueInternalName, "getInt", "()I");
        }
    }

    private void loadCstValue(Float cstValue, boolean isFloat) {
    	if (isFloat) {
    		mv.visitLdcInsn(cstValue.floatValue());
    	} else {
    		loadImm(Float.floatToRawIntBits(cstValue.floatValue()));
    	}
    }

    private void loadVRegister(int vsize, int reg, int n, VfpuPfxSrcState pfxSrcState, boolean isFloat) {
		if (Compiler.log.isTraceEnabled() && pfxSrcState != null && pfxSrcState.isKnown() && pfxSrcState.pfxSrc.enabled) {
			Compiler.log.trace(String.format("PFX    %08X - loadVRegister %d, %d, %d", getCodeInstruction().getAddress(), vsize, reg, n));
		}

		int m = (reg >> 2) & 7;
    	int i = (reg >> 0) & 3;
    	int s;
    	switch (vsize) {
    		case 1: {
    			s = (reg >> 5) & 3;
    			Float cstValue = getPfxSrcCstValue(pfxSrcState, n);
    			if (cstValue != null) {
    				loadCstValue(cstValue, isFloat);
    			} else {
    			    loadVRegister(m, i, s, isFloat);
	    			applyPfxSrcPostfix(pfxSrcState, n, isFloat);
    			}
    			break;
    		}
    		case 2: {
                s = (reg & 64) >> 5;
                Float cstValue = getPfxSrcCstValue(pfxSrcState, n);
                if (cstValue != null) {
    				loadCstValue(cstValue, isFloat);
                } else {
                	int index = getPfxSrcIndex(pfxSrcState, n);
	                if ((reg & 32) != 0) {
	                    loadVRegister(m, s + index, i, isFloat);
	                } else {
	                    loadVRegister(m, i, s + index, isFloat);
	                }
	                applyPfxSrcPostfix(pfxSrcState, n, isFloat);
                }
                break;
    		}
            case 3: {
                s = (reg & 64) >> 6;
                Float cstValue = getPfxSrcCstValue(pfxSrcState, n);
                if (cstValue != null) {
    				loadCstValue(cstValue, isFloat);
                } else {
                	int index = getPfxSrcIndex(pfxSrcState, n);
	                if ((reg & 32) != 0) {
	                    loadVRegister(m, s + index, i, isFloat);
	                } else {
	                    loadVRegister(m, i, s + index, isFloat);
	                }
	                applyPfxSrcPostfix(pfxSrcState, n, isFloat);
                }
                break;
    		}
            case 4: {
            	if ((reg & 64) != 0) {
            		Emulator.log.error(String.format("Unsupported Vreg=%d at ", reg, getCodeInstruction()));
            	}
                Float cstValue = getPfxSrcCstValue(pfxSrcState, n);
                if (cstValue != null) {
    				loadCstValue(cstValue, isFloat);
                } else {
                	int index = getPfxSrcIndex(pfxSrcState, n);
	                if ((reg & 32) != 0) {
	                    loadVRegister(m, index, i, isFloat);
	                } else {
	                    loadVRegister(m, i, index, isFloat);
	                }
	                applyPfxSrcPostfix(pfxSrcState, n, isFloat);
                }
            	break;
            }
    	}
    }

    public void prepareRegisterForStore(int reg) {
    	if (preparedRegisterForStore < 0) {
        	loadGpr();
        	loadImm(reg);
    		preparedRegisterForStore = reg;
    	}
    }

    public void storeRegister(int reg) {
    	if (preparedRegisterForStore == reg) {
	        mv.visitInsn(Opcodes.IASTORE);
	        preparedRegisterForStore = -1;
    	} else {
	    	loadGpr();
	        mv.visitInsn(Opcodes.SWAP);
	        loadImm(reg);
	        mv.visitInsn(Opcodes.SWAP);
	        mv.visitInsn(Opcodes.IASTORE);
    	}
    }

    public void storeRegister(int reg, int constantValue) {
    	if (preparedRegisterForStore == reg) {
    		preparedRegisterForStore = -1;
    	} else {
    		loadGpr();
    		loadImm(reg);
    	}
    	loadImm(constantValue);
        mv.visitInsn(Opcodes.IASTORE);
    }

    public void prepareFRegisterForStore(int reg) {
    	if (preparedRegisterForStore < 0) {
        	loadFpr();
        	loadImm(reg);
    		preparedRegisterForStore = reg;
    	}
    }

    public void storeFRegister(int reg) {
    	if (preparedRegisterForStore == reg) {
	        mv.visitInsn(Opcodes.FASTORE);
	        preparedRegisterForStore = -1;
    	} else {
	    	loadFpr();
	        mv.visitInsn(Opcodes.SWAP);
	        loadImm(reg);
	        mv.visitInsn(Opcodes.SWAP);
	        mv.visitInsn(Opcodes.FASTORE);
    	}
    }

    private boolean isPfxDstMasked(VfpuPfxDstState pfxDstState, int n) {
    	if (pfxDstState == null ||
    		pfxDstState.isUnknown() ||
    		!pfxDstState.pfxDst.enabled) {
    		return false;
    	}

    	return pfxDstState.pfxDst.msk[n];
    }

    private void applyPfxDstPostfix(VfpuPfxDstState pfxDstState, int n) {
    	if (pfxDstState == null ||
    		pfxDstState.isUnknown() ||
    	    !pfxDstState.pfxDst.enabled) {
    		return;
    	}

    	switch (pfxDstState.pfxDst.sat[n]) {
    		case 1:
				if (Compiler.log.isTraceEnabled() && pfxDstState != null && pfxDstState.isKnown() && pfxDstState.pfxDst.enabled) {
					Compiler.log.trace(String.format("PFX    %08X - applyPfxDstPostfix %d [0:1]", getCodeInstruction().getAddress(), n));
				}
    			mv.visitLdcInsn(1.0f);
        		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "min", "(FF)F");
    			mv.visitLdcInsn(0.0f);
        		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "max", "(FF)F");
        		break;
    		case 3:
				if (Compiler.log.isTraceEnabled() && pfxDstState != null && pfxDstState.isKnown() && pfxDstState.pfxDst.enabled) {
					Compiler.log.trace(String.format("PFX    %08X - applyPfxDstPostfix %d [-1:1]", getCodeInstruction().getAddress(), n));
				}
    			mv.visitLdcInsn(1.0f);
        		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "min", "(FF)F");
    			mv.visitLdcInsn(-1.0f);
        		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "max", "(FF)F");
        		break;
    	}
    }

    private void prepareVRegisterForStore(int m, int c, int r) {
        loadVpr();
        loadImm(VfpuState.getVprIndex(m, c, r));
        mv.visitInsn(Opcodes.AALOAD);
    }

    public void prepareVRegisterForStore(int vsize, int reg, int n, VfpuPfxDstState pfxDstState) {
    	if (preparedRegisterForStore < 0) {
            if (!isPfxDstMasked(pfxDstState, n)) {
            	int m = (reg >> 2) & 7;
            	int i = (reg >> 0) & 3;
            	int s;
            	switch (vsize) {
            		case 1: {
                        s = (reg >> 5) & 3;
                        prepareVRegisterForStore(m, i, s);
            			break;
            		}
            		case 2: {
                        s = (reg & 64) >> 5;
                        if ((reg & 32) != 0) {
                            prepareVRegisterForStore(m, s + n, i);
                        } else {
                            prepareVRegisterForStore(m, i, s + n);
                        }
                        break;
            		}
                    case 3: {
                        s = (reg & 64) >> 6;
                        if ((reg & 32) != 0) {
                            prepareVRegisterForStore(m, s + n, i);
                        } else {
                            prepareVRegisterForStore(m, i, s + n);
                        }
                        break;
            		}
                    case 4: {
                    	if ((reg & 64) != 0) {
                    		Emulator.log.error(String.format("Unsupported Vreg=%d at ", reg, getCodeInstruction()));
                    	}
                        if ((reg & 32) != 0) {
                            prepareVRegisterForStore(m, n, i);
                        } else {
                            prepareVRegisterForStore(m, i, n);
                        }
                    	break;
                    }
            	}
            }
    		preparedRegisterForStore = reg;
    	}
    }

    private void storeVRegister(int vsize, int reg, int n, VfpuPfxDstState pfxDstState, boolean isFloat) {
		if (Compiler.log.isTraceEnabled() && pfxDstState != null && pfxDstState.isKnown() && pfxDstState.pfxDst.enabled) {
			Compiler.log.trace(String.format("PFX    %08X - storeVRegister %d, %d, %d", getCodeInstruction().getAddress(), vsize, reg, n));
		}

    	if (preparedRegisterForStore == reg) {
            if (isPfxDstMasked(pfxDstState, n)) {
				if (Compiler.log.isTraceEnabled() && pfxDstState != null && pfxDstState.isKnown() && pfxDstState.pfxDst.enabled) {
					Compiler.log.trace(String.format("PFX    %08X - storeVRegister %d masked", getCodeInstruction().getAddress(), n));
				}

                mv.visitInsn(Opcodes.POP);
            } else {
                applyPfxDstPostfix(pfxDstState, n);
                if (isFloat) {
                	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, vfpuValueInternalName, "setFloat", "(F)V");
                } else {
                	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, vfpuValueInternalName, "setInt", "(I)V");
                }
            }
	        preparedRegisterForStore = -1;
    	} else {
    		Compiler.log.error("storeVRegister with non-prepared register is not supported");
    	}
    }

    public void loadFcr31() {
    	loadCpu();
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(CpuState.class), "fcr31", Type.getDescriptor(Fcr31.class));
    }

    public void loadVcr() {
    	loadCpu();
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(CpuState.class), "vcr", Type.getDescriptor(Vcr.class));
    }

	@Override
	public void loadHilo() {
		loadCpu();
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(CpuState.class), "hilo", Type.getDescriptor(long.class));
	}

	@Override
	public void prepareHiloForStore() {
		loadCpu();
		hiloPrepared = true;
	}

	@Override
	public void storeHilo() {
		if (!hiloPrepared) {
			loadCpu();
			mv.visitInsn(Opcodes.DUP_X2);
        	mv.visitInsn(Opcodes.POP);
		}
        mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(CpuState.class), "hilo", Type.getDescriptor(long.class));

        hiloPrepared = false;
	}

	@Override
	public void loadFcr31c() {
    	loadFcr31();
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Fcr31.class), "c", "Z");
    }

	@Override
	public void prepareFcr31cForStore() {
		loadFcr31();
	}

	@Override
	public void storeFcr31c() {
        mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(Fcr31.class), "c", "Z");
	}

	public void loadVcrCc() {
		loadVcrCc((codeInstruction.getOpcode() >> 18) & 7);
	}

	@Override
	public void loadVcrCc(int cc) {
    	loadVcr();
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Vcr.class), "cc", "[Z");
    	loadImm(cc);
    	mv.visitInsn(Opcodes.BALOAD);
    }

    private void loadLocalVar(int localVar) {
        mv.visitVarInsn(Opcodes.ILOAD, localVar);
    }

    private void loadInstruction(Instruction insn) {
    	String classInternalName = instructionsInternalName;

    	if (insn == Common.UNK) {
    		// UNK instruction is in Common class, not Instructions
    		classInternalName = Type.getInternalName(Common.class);
    	}

    	mv.visitFieldInsn(Opcodes.GETSTATIC, classInternalName, insn.name().replace('.', '_').replace(' ', '_'), instructionDescriptor);
    }

    private void storePc() {
    	loadCpu();
    	loadImm(codeInstruction.getAddress());
        mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(CpuState.class), "pc", Type.getDescriptor(int.class));
    }

    private void visitContinueToAddress(int returnAddress) {
        //      if (x != returnAddress) {
        //          RuntimeContext.jump(x, returnAddress);
        //      }
        Label continueLabel = new Label();
        Label isReturnAddress = new Label();

        mv.visitInsn(Opcodes.DUP);
        loadImm(returnAddress);
        visitJump(Opcodes.IF_ICMPEQ, isReturnAddress);

        loadImm(returnAddress);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "jump", "(II)V");
        mv.visitJumpInsn(Opcodes.GOTO, continueLabel);

        mv.visitLabel(isReturnAddress);
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(continueLabel);
    }

    public void visitJump() {
    	flushInstructionCount(true, false);
    	checkSync();

    	endMethod();
    	mv.visitInsn(Opcodes.IRETURN);
    }

    public void prepareCall(int address, int returnAddress, int returnRegister) {
    	preparedCallNativeCodeBlock = null;

    	// Do not call native block directly if we are profiling,
        // this would loose profiler information
        if (!Profiler.isProfilerEnabled()) {
        	// Is a native equivalent for this CodeBlock available?
        	preparedCallNativeCodeBlock = nativeCodeManager.getCompiledNativeCodeBlock(address);
        }

        if (preparedCallNativeCodeBlock == null) {
        	if (returnRegister != _zr) {
        		// Load the return register ($ra) with the return address
        		// before the delay slot is executed. The delay slot might overwrite it.
        		// For example:
        		//     addiu      $sp, $sp, -16
        		//     sw         $ra, 0($sp)
        		//     jal        0x0XXXXXXX
        		//     lw         $ra, 0($sp)
        		//     jr         $ra
        		//     addiu      $sp, $sp, 16
	        	prepareRegisterForStore(returnRegister);
	    		loadImm(returnAddress);
	            storeRegister(returnRegister);
        	}
        }
    }

    public void visitCall(int address, int returnAddress, int returnRegister, boolean returnRegisterModified) {
    	flushInstructionCount(false, false);

        if (preparedCallNativeCodeBlock != null) {
    		if (preparedCallNativeCodeBlock.getNativeCodeSequenceClass().equals(Nop.class)) {
        		// NativeCodeSequence Nop means nothing to do!
    		} else {
    			// Call NativeCodeSequence
    			if (Compiler.log.isDebugEnabled()) {
    				Compiler.log.debug(String.format("Inlining call at 0x%08X to %s", getCodeInstruction().getAddress(), preparedCallNativeCodeBlock));
    			}

    			visitNativeCodeSequence(preparedCallNativeCodeBlock, address, null);
    		}
    	} else {
	        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassName(address, instanceIndex), getStaticExecMethodName(), getStaticExecMethodDesc());
	        visitContinueToAddress(returnAddress);
    	}

        preparedCallNativeCodeBlock = null;
    }

    public void visitCall(int returnAddress, int returnRegister) {
    	flushInstructionCount(false, false);
        if (returnRegister != _zr) {
            storeRegister(returnRegister, returnAddress);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "call", "(I)I");
        visitContinueToAddress(returnAddress);
    }

    public void visitCall(int address, String methodName) {
    	flushInstructionCount(false, false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassName(address, instanceIndex), methodName, "()V");
    }

    public void visitIntepreterCall(int opcode, Instruction insn) {
    	loadInstruction(insn);
        loadProcessor();
        loadImm(opcode);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, instructionInternalName, "interpret", "(" + processorDescriptor + "I)V");
    }

    private boolean isFastSyscall(int code) {
		return fastSyscalls.contains(code);
	}

    /**
     * Generate the required Java code to load one parameter for
     * the syscall function from the CPU registers.
     *
     * The following code is generated based on the parameter type:
     * Processor: parameterValue = RuntimeContext.processor
     * int:       parameterValue = cpu.gpr[paramIndex++]
     * float:     parameterValue = cpu.fpr[paramFloatIndex++]
     * long:      parameterValue = (cpu.gpr[paramIndex++] & 0xFFFFFFFFL) + ((long) cpu.gpr[paramIndex++]) << 32)
     * boolean:   parameterValue = cpu.gpr[paramIndex++]
     * TPointer,
     * TPointer16,
     * TPointer32,
     * TPointer64,
     * TErrorPointer32:
     *            if (checkMemoryAccess()) {
     *                if (canBeNullParam && address == 0) {
     *                    goto addressGood;
     *                }
     *                if (RuntimeContext.checkMemoryPointer(address)) {
     *                    goto addressGood;
     *                }
     *                cpu.gpr[_v0] = SceKernelErrors.ERROR_INVALID_POINTER;
     *                pop all the parameters already prepared on the stack;
     *                goto afterSyscall;
     *                addressGood:
     *            }
     *            <parameterType> pointer = new <parameterType>(address);
     *            if (parameterType == TErrorPointer32.class) {
     *                parameterReader.setHasErrorPointer(true);
     *                localVar[LOCAL_ERROR_POINTER] = pointer;
     *            }
     *            parameterValue = pointer
     * HLEUidClass defined in annotation:
     *            <parameterType> uidObject = HLEUidObjectMapping.getObject("<parameterType>", uid);
     *            if (uidObject == null) {
     *                cpu.gpr[_v0] = errorValueOnNotFound;
     *                pop all the parameters already prepared on the stack;
     *                goto afterSyscall;
     *            }
     *            parameterValue = uidObject
     *
     * And then common for all the types:
     *            try {
     *                parameterValue = <module>.<methodToCheck>(parameterValue);
     *            } catch (SceKernelErrorException e) {
     *                goto catchSceKernelErrorException;
     *            }
     *            push parameterValue on stack
     *
     * @param parameterReader               the current parameter state
     * @param func                          the syscall function
     * @param parameterType                 the type of the parameter
     * @param afterSyscallLabel             the Label pointing after the call to the syscall function
     * @param catchSceKernelErrorException  the Label pointing to the SceKernelErrorException catch handler
     */
    private void loadParameter(CompilerParameterReader parameterReader, HLEModuleFunction func, Class<?> parameterType, Annotation[] parameterAnnotations, Label afterSyscallLabel, Label catchSceKernelErrorException) {
    	if (parameterType == Processor.class) {
    		loadProcessor();
    		parameterReader.incrementCurrentStackSize();
    	} else if (parameterType == CpuState.class) {
    		loadCpu();
    		parameterReader.incrementCurrentStackSize();
    	} else if (parameterType == int.class) {
    		parameterReader.loadNextInt();
    		parameterReader.incrementCurrentStackSize();
    	} else if (parameterType == float.class) {
    		parameterReader.loadNextFloat();
    		parameterReader.incrementCurrentStackSize();
    	} else if (parameterType == long.class) {
    		parameterReader.loadNextLong();
    		parameterReader.incrementCurrentStackSize(2);
    	} else if (parameterType == boolean.class) {
    		parameterReader.loadNextInt();
    		parameterReader.incrementCurrentStackSize();
    	} else if (parameterType == String.class) {
    		parameterReader.loadNextInt();

    		int maxLength = 16 * 1024;
    		for (Annotation parameterAnnotation : parameterAnnotations) {
    			if (parameterAnnotation instanceof StringInfo) {
    				StringInfo stringInfo = ((StringInfo)parameterAnnotation);
    				maxLength = stringInfo.maxLength();
    				break;
    			}
    		}
    		loadImm(maxLength);
   			mv.visitMethodInsn(
				Opcodes.INVOKESTATIC,
				runtimeContextInternalName,
				"readStringNZ", "(II)" + Type.getDescriptor(String.class)
   			);
    		parameterReader.incrementCurrentStackSize();
    	} else if (parameterType == PspString.class) {
    		parameterReader.loadNextInt();

    		int maxLength = 16 * 1024;
    		for (Annotation parameterAnnotation : parameterAnnotations) {
    			if (parameterAnnotation instanceof StringInfo) {
    				StringInfo stringInfo = ((StringInfo)parameterAnnotation);
    				maxLength = stringInfo.maxLength();
    			}
    		}
    		loadImm(maxLength);
   			mv.visitMethodInsn(
				Opcodes.INVOKESTATIC,
				runtimeContextInternalName,
				"readPspStringNZ", "(II)" + Type.getDescriptor(PspString.class)
   			);
    		parameterReader.incrementCurrentStackSize();
    	} else if (parameterType == TPointer.class || parameterType == TPointer16.class || parameterType == TPointer32.class || parameterType == TPointer64.class || parameterType == TErrorPointer32.class) {
    		// if (checkMemoryAccess()) {
    		//     if (canBeNullParam && address == 0) {
    		//         goto addressGood;
    		//     }
    		//     if (RuntimeContext.checkMemoryPointer(address)) {
    		//         goto addressGood;
    		//     }
    		//     cpu.gpr[_v0] = SceKernelErrors.ERROR_INVALID_POINTER;
    		//     pop all the parameters already prepared on the stack;
    		//     goto afterSyscall;
    		//     addressGood:
    		// }
    		// <parameterType> pointer = new <parameterType>(address);
    		// if (parameterType == TErrorPointer32.class) {
    		//     parameterReader.setHasErrorPointer(true);
    		//     localVar[LOCAL_ERROR_POINTER] = pointer;
    		// }
    		mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(parameterType));
    		mv.visitInsn(Opcodes.DUP);
    		loadMemory();
    		parameterReader.loadNextInt();

    		boolean canBeNull = false;
    		for (Annotation parameterAnnotation : parameterAnnotations) {
    			if (parameterAnnotation instanceof CanBeNull) {
    				canBeNull = true;
    				break;
    			}
    		}

    		if (checkMemoryAccess()) {
    			Label addressGood = new Label();
    			if (canBeNull) {
        			mv.visitInsn(Opcodes.DUP);
    				mv.visitJumpInsn(Opcodes.IFEQ, addressGood);
    			}
    			mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryPointer", "(I)Z");
    			mv.visitJumpInsn(Opcodes.IFNE, addressGood);
    			storeRegister(_v0, SceKernelErrors.ERROR_INVALID_POINTER);
    			parameterReader.popAllStack(4);
    			mv.visitJumpInsn(Opcodes.GOTO, afterSyscallLabel);
    			mv.visitLabel(addressGood);
    		}
    		if (parameterType == TPointer16.class || parameterType == TPointer32.class || parameterType == TPointer64.class) {
    			loadImm(canBeNull);
    			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(parameterType), "<init>", "(" + memoryDescriptor + "IZ)V");
    		} else {
    			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(parameterType), "<init>", "(" + memoryDescriptor + "I)V");
    		}
    		if (parameterType == TErrorPointer32.class) {
    			parameterReader.setHasErrorPointer(true);
    			mv.visitInsn(Opcodes.DUP);
    			mv.visitVarInsn(Opcodes.ASTORE, LOCAL_ERROR_POINTER);
    		}
    		parameterReader.incrementCurrentStackSize();
    	} else {
			HLEUidClass hleUidClass = parameterType.getAnnotation(HLEUidClass.class);
			if (hleUidClass != null) {
		   		int errorValueOnNotFound = hleUidClass.errorValueOnNotFound();
				
				// <parameterType> uidObject = HLEUidObjectMapping.getObject("<parameterType>", uid);
				// if (uidObject == null) {
				//     cpu.gpr[_v0] = errorValueOnNotFound;
	    		//     pop all the parameters already prepared on the stack;
	    		//     goto afterSyscall;
				// }
				mv.visitLdcInsn(parameterType.getName());
				// Load the UID
				parameterReader.loadNextInt();

				// Load the UID Object
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(HLEUidObjectMapping.class), "getObject", "(" + Type.getDescriptor(String.class) + "I)" + Type.getDescriptor(Object.class));
				Label foundUid = new Label();
				mv.visitInsn(Opcodes.DUP);
				mv.visitJumpInsn(Opcodes.IFNONNULL, foundUid);
				storeRegister(_v0, errorValueOnNotFound);
				parameterReader.popAllStack(1);
				mv.visitJumpInsn(Opcodes.GOTO, afterSyscallLabel);
				mv.visitLabel(foundUid);
				mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(parameterType));
	    		parameterReader.incrementCurrentStackSize();
			} else {
				Compiler.log.error(String.format("Unsupported sycall parameter type '%s'", parameterType.getName()));
				Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_UNIMPLEMENTED);
			}
    	}

    	Method methodToCheck = null;
		for (Annotation parameterAnnotation : parameterAnnotations) {
			if (parameterAnnotation instanceof CheckArgument) {
				CheckArgument checkArgument = (CheckArgument) parameterAnnotation;
				try {
					methodToCheck = func.getHLEModule().getClass().getMethod(checkArgument.value(), parameterType);
				} catch (Exception e) {
					Compiler.log.error(String.format("CheckArgument method '%s' not found in %s", checkArgument.value(), func.getModuleName()), e);
				}
				break;
			}
		}

    	if (methodToCheck != null) {
    		// try {
    		//     parameterValue = <module>.<methodToCheck>(parameterValue);
    		// } catch (SceKernelErrorException e) {
    		//     goto catchSceKernelErrorException;
    		// }
    		loadModule(func.getModuleName());
    		mv.visitInsn(Opcodes.SWAP);

    		Label tryStart = new Label();
        	Label tryEnd = new Label();
        	mv.visitTryCatchBlock(tryStart, tryEnd, catchSceKernelErrorException, Type.getInternalName(SceKernelErrorException.class));

        	mv.visitLabel(tryStart);
        	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(methodToCheck.getDeclaringClass()), methodToCheck.getName(), "(" + Type.getDescriptor(parameterType) + ")" + Type.getDescriptor(parameterType));
        	mv.visitLabel(tryEnd);
    	}

    	parameterReader.incrementCurrentParameterIndex();
    }

    /**
     * Generate the required Java code to store the return value of
     * the syscall function into the CPU registers.
     *
     * The following code is generated depending on the return type:
     * void:         -
     * int:          cpu.gpr[_v0] = intValue
     * boolean:      cpu.gpr[_v0] = booleanValue
     * long:         cpu.gpr[_v0] = (int) (longValue & 0xFFFFFFFFL)
     *               cpu.gpr[_v1] = (int) (longValue >>> 32)
     * float:        cpu.fpr[_f0] = floatValue
     * HLEUidClass:  if (moduleMethodUidGenerator == "") {
     *                   cpu.gpr[_v0] = HLEUidObjectMapping.createUidForObject("<return type>", returnValue);
     *               } else {
     *                   int uid = <module>.<moduleMethodUidGenerator>();
     *                   cpu.gpr[_v0] = HLEUidObjectMapping.addObjectMap("<return type>", uid, returnValue);
     *               }
     *
     * @param func        the syscall function
     * @param returnType  the type of the return value
     */
    private void storeReturnValue(HLEModuleFunction func, Class<?> returnType) {
    	if (returnType == void.class) {
    		// Nothing to do
    	} else if (returnType == int.class) {
    		// cpu.gpr[_v0] = intValue
    		storeRegister(_v0);
    	} else if (returnType == boolean.class) {
    		// cpu.gpr[_v0] = booleanValue
    		storeRegister(_v0);
    	} else if (returnType == long.class) {
    		// cpu.gpr[_v0] = (int) (longValue & 0xFFFFFFFFL)
    		// cpu.gpr[_v1] = (int) (longValue >>> 32)
    		mv.visitInsn(Opcodes.DUP2);
    		mv.visitLdcInsn(0xFFFFFFFFL);
    		mv.visitInsn(Opcodes.LAND);
    		mv.visitInsn(Opcodes.L2I);
    		storeRegister(_v0);
    		loadImm(32);
    		mv.visitInsn(Opcodes.LSHR);
    		mv.visitInsn(Opcodes.L2I);
    		storeRegister(_v1);
    	} else if (returnType == float.class) {
    		// cpu.fpr[_f0] = floatValue
    		storeFRegister(_f0);
    	} else {
			HLEUidClass hleUidClass = returnType.getAnnotation(HLEUidClass.class);
			if (hleUidClass != null) {
				// if (moduleMethodUidGenerator == "") {
				//     cpu.gpr[_v0] = HLEUidObjectMapping.createUidForObject("<return type>", returnValue);
				// } else {
				//     int uid = <module>.<moduleMethodUidGenerator>();
				//     cpu.gpr[_v0] = HLEUidObjectMapping.addObjectMap("<return type>", uid, returnValue);
				// }
				if (hleUidClass.moduleMethodUidGenerator().length() <= 0) {
					// No UID generator method, use the default one
					mv.visitLdcInsn(returnType.getName());
					mv.visitInsn(Opcodes.SWAP);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(HLEUidObjectMapping.class), "createUidForObject", "(" + Type.getDescriptor(String.class) + Type.getDescriptor(Object.class) + ")I");
					storeRegister(_v0);
				} else {
					mv.visitLdcInsn(returnType.getName());
					mv.visitInsn(Opcodes.SWAP);
					loadModule(func.getModuleName());
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(func.getHLEModuleMethod().getDeclaringClass()), hleUidClass.moduleMethodUidGenerator(), "()I");
					mv.visitInsn(Opcodes.SWAP);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(HLEUidObjectMapping.class), "addObjectMap", "(" + Type.getDescriptor(String.class) + "I" + Type.getDescriptor(Object.class) + ")I");
					storeRegister(_v0);
				}
			} else {
				Compiler.log.error(String.format("Unsupported sycall return value type '%s'", returnType.getName()));
			}
    	}
    }

    private void logSyscall(HLEModuleFunction func, String logPrefix, String logCheckFunction, String logFunction) {
		// Modules.getLogger(func.getModuleName()).warn("Unimplemented...");
		mv.visitLdcInsn(func.getModuleName());
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Modules.class), "getLogger", "(" + Type.getDescriptor(String.class) + ")" + Type.getDescriptor(Logger.class));
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Logger.class), logCheckFunction, "()Z");
		Label loggingDisabled = new Label();
		mv.visitJumpInsn(Opcodes.IFEQ, loggingDisabled);

		mv.visitLdcInsn(func.getModuleName());
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Modules.class), "getLogger", "(" + Type.getDescriptor(String.class) + ")" + Type.getDescriptor(Logger.class));

		StringBuilder formatString = new StringBuilder();
		if (logPrefix != null) {
			formatString.append(logPrefix);
		}
		formatString.append(func.getFunctionName());
		ParameterInfo[] parameters = new ClassAnalyzer().getParameters(func.getFunctionName(), func.getHLEModuleMethod().getDeclaringClass());
		if (parameters != null) {
			// Log message:
			//    String.format(
			//       "Unimplemented <function name>
			//                 <parameterIntegerName>=0x%X,
			//                 <parameterBooleanName>=%b,
			//                 <parameterLongName>=0x%X,
			//                 <parameterFloatName>=%f,
			//                 <parameterOtherTypeName>=%s",
			//       new Object[] {
			//                 new Integer(parameterValueInteger),
			//                 new Boolean(parameterValueBoolean),
			//                 new Long(parameterValueLong),
			//                 new Float(parameterValueFloat),
			//                 parameterValueOtherTypes
			//       })
			loadImm(parameters.length);
			mv.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
            CompilerParameterReader parameterReader = new CompilerParameterReader(this);
            Annotation[][] paramsAnotations = func.getHLEModuleMethod().getParameterAnnotations();
            int paramIndex = 0;
            for (ParameterInfo parameter : parameters) {
            	mv.visitInsn(Opcodes.DUP);
            	loadImm(paramIndex);

            	Class<?> parameterType = parameter.type;
        		formatString.append(paramIndex > 0 ? ", " : " ");
            	formatString.append(parameter.name);
            	formatString.append("=");
            	CompilerTypeInformation typeInformation = compilerTypeManager.getCompilerTypeInformation(parameterType);
            	formatString.append(typeInformation.formatString);

            	if (typeInformation.boxingTypeInternalName != null) {
            		mv.visitTypeInsn(Opcodes.NEW, typeInformation.boxingTypeInternalName);
            		mv.visitInsn(Opcodes.DUP);
            	}

        		loadParameter(parameterReader, func, parameterType, paramsAnotations[paramIndex], null, null);

            	if (typeInformation.boxingTypeInternalName != null) {
            		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, typeInformation.boxingTypeInternalName, "<init>", typeInformation.boxingMethodDescriptor);
            	}
            	mv.visitInsn(Opcodes.AASTORE);

            	paramIndex++;
            }
			mv.visitLdcInsn(formatString.toString());
			mv.visitInsn(Opcodes.SWAP);
        	mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(String.class), "format", "(" + Type.getDescriptor(String.class) + "[" + Type.getDescriptor(Object.class) + ")" + Type.getDescriptor(String.class));
		} else {
			mv.visitLdcInsn(formatString.toString());
		}
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Logger.class), logFunction, "(" + Type.getDescriptor(Object.class) + ")V");

		mv.visitLabel(loggingDisabled);
    }

    /**
     * Generate the required Java code to call a syscall function.
     * The code generated much match the Java behavior implemented in
     * jpcsp.HLE.modules.HLEModuleFunctionReflection
     *
     * The following code is generated:
     *     if (!fastSyscall) {
     *         RuntimeContext.preSyscall();
     *     }
     *     if (func.checkInsideInterrupt()) {
     *         if (IntrManager.getInstance.isInsideInterrupt()) {
     *             cpu.gpr[_v0] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
     *             goto afterSyscall;
     *         }
     *     }
     *     if (func.checkDispatchThreadEnabled()) {
     *         if (!Modules.ThreadManForUserModule.isDispatchThreadEnabled()) {
     *             cpu.gpr[_v0] = SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
     *             goto afterSyscall;
     *         }
     *     }
     *     if (func.isUnimplemented()) {
     *         Modules.getLogger(func.getModuleName()).warn("Unimplemented <function name> parameterName1=parameterValue1, parameterName2=parameterValue2, ...");
     *     }
     *     foreach parameter {
     *         loadParameter(parameter);
     *     }
     *     try {
     *         returnValue = <module name>.<function name>(...parameters...);
     *         storeReturnValue();
     *         if (parameterReader.hasErrorPointer()) {
     *             errorPointer.setValue(0);
     *         }
     *     } catch (SceKernelErrorException e) {
     *         errorCode = e.errorCode;
     *         if (Modules.log.isDebugEnabled()) {
     *             Modules.log.debug(String.format("<function name> return errorCode 0x%08X", errorCode));
     *         }
     *         if (parameterReader.hasErrorPointer()) {
     *             errorPointer.setValue(errorCode);
     *             cpu.gpr[_v0] = 0;
     *         } else {
     *             cpu.gpr[_v0] = errorCode;
     *         }
     *         reload cpu.gpr[_ra]; // an exception is always clearing the whole stack
     *     }
     *     afterSyscall:
     *     if (fastSyscall) {
     *         RuntimeContext.postSyscallFast();
     *     } else {
     *         RuntimeContext.postSyscall();
     *     }
     *
     * @param func         the syscall function
     * @param fastSyscall  true if this is a fast syscall (i.e. without context switching)
     *                     false if not (i.e. a syscall where context switching could happen)
     */
    private void visitSyscall(HLEModuleFunction func, boolean fastSyscall) {
    	// The compilation of a syscall requires more stack size than usual
    	maxStackSize = SYSCALL_MAX_STACK_SIZE;

    	if (!fastSyscall) {
    		mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "preSyscall", "()V");
    	}

    	Label afterSyscallLabel = new Label();

    	if (func.checkInsideInterrupt()) {
    		// if (IntrManager.getInstance().isInsideInterrupt()) {
    		//     cpu.gpr[_v0] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
    		//     goto afterSyscall
    		// }
    		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(IntrManager.class), "getInstance", "()" + Type.getDescriptor(IntrManager.class));
    		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(IntrManager.class), "isInsideInterrupt", "()Z");
    		Label notInsideInterrupt = new Label();
    		mv.visitJumpInsn(Opcodes.IFEQ, notInsideInterrupt);
    		storeRegister(_v0, SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT);
    		mv.visitJumpInsn(Opcodes.GOTO, afterSyscallLabel);
    		mv.visitLabel(notInsideInterrupt);
    	}

    	if (func.checkDispatchThreadEnabled()) {
    		// if (!Modules.ThreadManForUserModule.isDispatchThreadEnabled()) {
    		//     cpu.gpr[_v0] = SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
    		//     goto afterSyscall
    		// }
    		loadModule("ThreadManForUser");
    		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ThreadManForUser.class), "isDispatchThreadEnabled", "()Z");
    		Label dispatchThreadEnabled = new Label();
    		mv.visitJumpInsn(Opcodes.IFNE, dispatchThreadEnabled);
    		storeRegister(_v0, SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT);
    		mv.visitJumpInsn(Opcodes.GOTO, afterSyscallLabel);
    		mv.visitLabel(dispatchThreadEnabled);
    	}

    	if (func.isUnimplemented()) {
    		logSyscall(func, "Unimplemented ", "isInfoEnabled", "warn");
    	}

    	// Collecting the parameters and calling the module function...
        CompilerParameterReader parameterReader = new CompilerParameterReader(this);

    	loadModule(func.getModuleName());
    	parameterReader.incrementCurrentStackSize();

    	Label tryStart = new Label();
    	Label tryEnd = new Label();
    	Label catchSceKernelErrorException = new Label();
    	mv.visitTryCatchBlock(tryStart, tryEnd, catchSceKernelErrorException, Type.getInternalName(SceKernelErrorException.class));

        Class<?>[] parameterTypes = func.getHLEModuleMethod().getParameterTypes();
        Class<?> returnType = func.getHLEModuleMethod().getReturnType();
        StringBuilder methodDescriptor = new StringBuilder();
        methodDescriptor.append("(");
        
        Annotation[][] paramsAnotations = func.getHLEModuleMethod().getParameterAnnotations();
        int paramIndex = 0;
        for (Class<?> parameterType : parameterTypes) {
        	methodDescriptor.append(Type.getDescriptor(parameterType));
        	loadParameter(parameterReader, func, parameterType, paramsAnotations[paramIndex], afterSyscallLabel, catchSceKernelErrorException);
        	paramIndex++;
        }
        methodDescriptor.append(")");
        methodDescriptor.append(Type.getDescriptor(returnType));

    	mv.visitLabel(tryStart);

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(func.getHLEModuleMethod().getDeclaringClass()), func.getFunctionName(), methodDescriptor.toString());

        storeReturnValue(func, returnType);

        if (parameterReader.hasErrorPointer()) {
        	// errorPointer.setValue(0);
            mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ERROR_POINTER);
        	loadImm(0);
        	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(TErrorPointer32.class), "setValue", "(I)V");
    	}

        mv.visitLabel(tryEnd);
        mv.visitJumpInsn(Opcodes.GOTO, afterSyscallLabel);

        // catch (SceKernelErrorException e) {
        //     errorCode = e.errorCode;
        //     if (Modules.log.isDebugEnabled()) {
        //         Modules.log.debug(String.format("<function name> return errorCode 0x%08X", errorCode));
        //     }
        //     if (hasErrorPointer()) {
        //         errorPointer.setValue(errorCode);
        //         cpu.gpr[_v0] = 0;
        //     } else {
        //         cpu.gpr[_v0] = errorCode;
        //     }
        // }
        mv.visitLabel(catchSceKernelErrorException);
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(SceKernelErrorException.class), "errorCode", "I");
        {
        	// if (Modules.log.isDebugEnabled()) {
        	//     Modules.log.debug(String.format("<function name> returning errorCode 0x%08X", new Object[1] { new Integer(errorCode) }));
        	// }
        	mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Modules.class), "log", Type.getDescriptor(Logger.class));
        	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Logger.class), "isDebugEnabled", "()Z");
        	Label notDebug = new Label();
        	mv.visitJumpInsn(Opcodes.IFEQ, notDebug);
        	mv.visitInsn(Opcodes.DUP);
    		mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(Integer.class));
    		mv.visitInsn(Opcodes.DUP_X1);
    		mv.visitInsn(Opcodes.SWAP);
    		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Integer.class), "<init>", "(I)V");
    		loadImm(1);
    		mv.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
        	mv.visitInsn(Opcodes.DUP_X1);
        	mv.visitInsn(Opcodes.SWAP);
    		loadImm(0);
    		mv.visitInsn(Opcodes.SWAP);
    		mv.visitInsn(Opcodes.AASTORE);
        	mv.visitLdcInsn(String.format("%s returning errorCode 0x%%08X", func.getFunctionName()));
        	mv.visitInsn(Opcodes.SWAP);
        	mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(String.class), "format", "(" + Type.getDescriptor(String.class) + "[" + Type.getDescriptor(Object.class) + ")" + Type.getDescriptor(String.class));
        	mv.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Modules.class), "log", Type.getDescriptor(Logger.class));
        	mv.visitInsn(Opcodes.SWAP);
        	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Logger.class), "debug", "(" + Type.getDescriptor(Object.class) + ")V");
        	mv.visitLabel(notDebug);
        }
        if (parameterReader.hasErrorPointer()) {
        	// errorPointer.setValue(errorCode);
        	// cpu.gpr[_v0] = 0;
            mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ERROR_POINTER);
        	mv.visitInsn(Opcodes.SWAP);
        	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(TErrorPointer32.class), "setValue", "(I)V");
        	storeRegister(_v0, 0);
    	} else {
    		// cpu.gpr[_v0] = errorCode;
    		storeRegister(_v0);
    	}

        // Reload the $ra register, the stack is lost after an exception
        CodeInstruction previousInstruction = codeBlock.getCodeInstruction(codeInstruction.getAddress() - 4);
        if (previousInstruction != null && previousInstruction.getInsn() == Instructions.JR) {
        	int jumpRegister = (previousInstruction.getOpcode() >> 21) & 0x1F;
        	loadRegister(jumpRegister);
        }

    	mv.visitLabel(afterSyscallLabel);

        if (fastSyscall) {
    		mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "postSyscallFast", "()V");
        } else {
    		mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "postSyscall", "()V");
        }
    }

    /**
     * Generate the required Java code to perform a syscall.
     *
     * When the syscall function is an HLEModuleFunctionReflection,
     * generate the code for calling the module function directly, as
     * HLEModuleFunctionReflection.execute() would.
     *
     * Otherwise, generate the code for calling
     *     RuntimeContext.syscall()
     * or
     *     RuntimeContext.syscallFast()
     * 
     * @param opcode    opcode of the instruction
     */
    public void visitSyscall(int opcode) {
    	flushInstructionCount(false, false);

    	int code = (opcode >> 6) & 0x000FFFFF;

    	if (code == SyscallHandler.syscallUnmappedImport) {
    		storePc();
    	}

    	HLEModuleFunction func = HLEModuleManager.getInstance().getFunctionFromSyscallCode(code);
    	boolean fastSyscall = isFastSyscall(code);
    	if (func == null) {
	    	loadImm(code);
	    	if (fastSyscall) {
	    		mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "syscallFast", "(I)V");
	    	} else {
	    		mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "syscall", "(I)V");
	    	}
    	} else {
    		visitSyscall(func, fastSyscall);
    	}
    }

    public void startClass(ClassVisitor cv) {
    	if (RuntimeContext.enableLineNumbers) {
    		cv.visitSource(getCodeBlock().getClassName() + ".java", null);
    	}
    }

    public void startSequenceMethod() {
        if (storeGprLocal) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "gpr", "[I");
            mv.visitVarInsn(Opcodes.ASTORE, LOCAL_GPR);
        }

        if (storeMemoryIntLocal) {
			mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "memoryInt", "[I");
			mv.visitVarInsn(Opcodes.ASTORE, LOCAL_MEMORY_INT);
        }

        if (enableIntructionCounting) {
            currentInstructionCount = 0;
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ISTORE, LOCAL_INSTRUCTION_COUNT);
        }

        startNonBranchingCodeSequence();
    }

    public void endSequenceMethod() {
    	flushInstructionCount(false, true);
        mv.visitInsn(Opcodes.RETURN);
    }

    public void checkSync() {
    	if (RuntimeContext.enableDaemonThreadSync) {
    		Label doNotWantSync = new Label();
            mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "wantSync", "Z");
            mv.visitJumpInsn(Opcodes.IFEQ, doNotWantSync);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.syncName, "()V");
            mv.visitLabel(doNotWantSync);
    	}
    }

    private void startInternalMethod() {
    	// if (e != null)
    	Label notReplacedLabel = new Label();
    	mv.visitFieldInsn(Opcodes.GETSTATIC, codeBlock.getClassName(), getReplaceFieldName(), executableDescriptor);
    	mv.visitJumpInsn(Opcodes.IFNULL, notReplacedLabel);
    	{
    		// return e.exec(returnAddress, alternativeReturnAddress, isJump);
        	mv.visitFieldInsn(Opcodes.GETSTATIC, codeBlock.getClassName(), getReplaceFieldName(), executableDescriptor);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, executableInternalName, getExecMethodName(), getExecMethodDesc());
            mv.visitInsn(Opcodes.IRETURN);
    	}
    	mv.visitLabel(notReplacedLabel);

    	if (Profiler.isProfilerEnabled()) {
    		loadImm(getCodeBlock().getStartAddress());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, profilerInternalName, "addCall", "(I)V");
    	}

    	if (RuntimeContext.debugCodeBlockCalls) {
        	loadImm(getCodeBlock().getStartAddress());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debugCodeBlockStart, "(I)V");
        }
    }

    public void startMethod() {
    	startInternalMethod();
    	startSequenceMethod();
    }

    private void flushInstructionCount(boolean local, boolean last) {
        if (enableIntructionCounting) {
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
		        	loadImm(currentInstructionCount);
			        mv.visitInsn(Opcodes.IADD);
		        }
		        if (Profiler.isProfilerEnabled()) {
			        mv.visitInsn(Opcodes.DUP);
		    		loadImm(getCodeBlock().getStartAddress());
		            mv.visitMethodInsn(Opcodes.INVOKESTATIC, profilerInternalName, "addInstructionCount", "(II)V");
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

    private void endInternalMethod() {
        if (RuntimeContext.debugCodeBlockCalls) {
            mv.visitInsn(Opcodes.DUP);
        	loadImm(getCodeBlock().getStartAddress());
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debugCodeBlockEnd, "(II)V");
        }
    }

    public void endMethod() {
    	endInternalMethod();
    	flushInstructionCount(false, true);
    }

    public void beforeInstruction(CodeInstruction codeInstruction) {
	    if (enableIntructionCounting) {
	    	if (codeInstruction.isBranchTarget()) {
	    		flushInstructionCount(true, false);
	    	}
	    	currentInstructionCount++;
	    }

	    if (RuntimeContext.enableLineNumbers) {
	    	// Force the instruction to emit a label
    		codeInstruction.getLabel(false);
    	}
    }

    private void startNonBranchingCodeSequence() {
    	vfpuPfxsState.reset();
    	vfpuPfxtState.reset();
    	vfpuPfxdState.reset();
    }

    private boolean isNonBranchingCodeSequence(CodeInstruction codeInstruction) {
        return !codeInstruction.isBranchTarget() && !codeInstruction.isBranching();
    }

    public void startInstruction(CodeInstruction codeInstruction) {
    	if (RuntimeContext.enableLineNumbers) {
    		int lineNumber = codeInstruction.getAddress() - getCodeBlock().getLowestAddress();
    		// Java line number is unsigned 16bits
    		if (lineNumber >= 0 && lineNumber <= 0xFFFF) {
    			mv.visitLineNumber(lineNumber, codeInstruction.getLabel());
    		}
    	}

    	if (RuntimeContext.debugCodeInstruction) {
        	loadImm(codeInstruction.getAddress());
        	loadImm(codeInstruction.getOpcode());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debugCodeInstructionName, "(II)V");
	    }

	    if (RuntimeContext.enableInstructionTypeCounting) {
	    	if (codeInstruction.getInsn() != null) {
		    	loadInstruction(codeInstruction.getInsn());
		    	loadImm(codeInstruction.getOpcode());
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.instructionTypeCount, "(" + instructionDescriptor + "I)V");
	    	}
	    }

	    if (RuntimeContext.enableDebugger) {
	    	loadImm(codeInstruction.getAddress());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.debuggerName, "(I)V");
	    }

	    if (RuntimeContext.checkCodeModification && !(codeInstruction instanceof NativeCodeInstruction)) {
	    	// Generate the following sequence:
	    	//
	    	//     if (memory.read32(pc) != opcode) {
	    	//         RuntimeContext.onCodeModification(pc, opcode);
	    	//     }
	    	//
	    	loadMemory();
	    	loadImm(codeInstruction.getAddress());
	        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "read32", "(I)I");
	        loadImm(codeInstruction.getOpcode());
	        Label codeUnchanged = new Label();
	        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, codeUnchanged);

	        loadImm(codeInstruction.getAddress());
	        loadImm(codeInstruction.getOpcode());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "onCodeModification", "(II)V");

	        mv.visitLabel(codeUnchanged);
	    }

	    if (!isNonBranchingCodeSequence(codeInstruction)) {
	    	startNonBranchingCodeSequence();
	    }
    }

    private void disablePfxSrc(VfpuPfxSrcState pfxSrcState) {
        pfxSrcState.pfxSrc.enabled = false;
        pfxSrcState.setKnown(true);
    }

    private void disablePfxDst(VfpuPfxDstState pfxDstState) {
        pfxDstState.pfxDst.enabled = false;
        pfxDstState.setKnown(true);
    }

    public void endInstruction() {
        if (codeInstruction != null) {
            if (codeInstruction.hasFlags(Instruction.FLAG_USE_VFPU_PFXS)) {
                disablePfxSrc(vfpuPfxsState);
            }

            if (codeInstruction.hasFlags(Instruction.FLAG_USE_VFPU_PFXT)) {
                disablePfxSrc(vfpuPfxtState);
            }

            if (codeInstruction.hasFlags(Instruction.FLAG_USE_VFPU_PFXD)) {
                disablePfxDst(vfpuPfxdState);
            }
        }
    }

    public void visitJump(int opcode, CodeInstruction target) {
    	// Back branch? i.e probably a loop
        if (target.getAddress() <= getCodeInstruction().getAddress()) {
        	checkSync();

        	if (Profiler.isProfilerEnabled()) {
        		loadImm(getCodeInstruction().getAddress());
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, profilerInternalName, "addBackBranch", "(I)V");
        	}
        }
        visitJump(opcode, target.getLabel());
    }

    public void visitJump(int opcode, Label label) {
    	flushInstructionCount(true, false);
        mv.visitJumpInsn(opcode, label);
    }

    public void visitJump(int opcode, int address) {
        flushInstructionCount(true, false);
        if (opcode == Opcodes.GOTO) {
            loadImm(address);
            visitJump();
        } else {
            Compiler.log.error("Not implemented: branching to an unknown address");
            if (opcode == Opcodes.IF_ACMPEQ ||
                opcode == Opcodes.IF_ACMPNE ||
                opcode == Opcodes.IF_ICMPEQ ||
                opcode == Opcodes.IF_ICMPNE ||
                opcode == Opcodes.IF_ICMPGE ||
                opcode == Opcodes.IF_ICMPGT ||
                opcode == Opcodes.IF_ICMPLE ||
                opcode == Opcodes.IF_ICMPLT) {
                // 2 Arguments to POP
                mv.visitInsn(Opcodes.POP);
            }
            mv.visitInsn(Opcodes.POP);
            loadImm(address);
            visitJump();
        }
    }

    public static String getClassName(int address, int instanceIndex) {
    	return "_S1_" + instanceIndex + "_" + Integer.toHexString(address).toUpperCase();
    }

    public static int getClassAddress(String name) {
    	String hexAddress = name.substring(name.lastIndexOf("_") + 1);

        return Integer.parseInt(hexAddress, 16);
    }

    public static int getClassInstanceIndex(String name) {
    	int startIndex = name.indexOf("_", 1);
    	int endIndex = name.lastIndexOf("_");
    	String instanceIndex = name.substring(startIndex + 1, endIndex);

    	return Integer.parseInt(instanceIndex);
    }

    public String getExecMethodName() {
        return "exec";
    }

    public String getExecMethodDesc() {
        return "()I";
    }

    public String getReplaceFieldName() {
    	return "e";
    }

    public String getReplaceMethodName() {
    	return "setExecutable";
    }

    public String getReplaceMethodDesc() {
    	return "(" + executableDescriptor + ")V";
    }

    public String getStaticExecMethodName() {
        return "s";
    }

    public String getStaticExecMethodDesc() {
        return "()I";
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
        return maxStackSize;
    }

    public void visitPauseEmuWithStatus(MethodVisitor mv, int status) {
    	loadImm(status);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.pauseEmuWithStatus, "(I)V");
    }

    public void visitLogInfo(MethodVisitor mv, String message) {
    	mv.visitLdcInsn(message);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, RuntimeContext.logInfo, "(" + stringDescriptor + ")V");
    }

	@Override
	public MethodVisitor getMethodVisitor() {
		return mv;
	}

	public void setMethodVisitor(MethodVisitor mv) {
		this.mv = mv;
	}

	public CodeInstruction getCodeInstruction() {
		return codeInstruction;
	}

	public void setCodeInstruction(CodeInstruction codeInstruction) {
		this.codeInstruction = codeInstruction;
	}

	@Override
	public int getSaValue() {
		return codeInstruction.getSaValue();
    }

	@Override
	public int getRsRegisterIndex() {
		return codeInstruction.getRsRegisterIndex();
    }

	@Override
    public int getRtRegisterIndex() {
		return codeInstruction.getRtRegisterIndex();
    }

	@Override
    public int getRdRegisterIndex() {
		return codeInstruction.getRdRegisterIndex();
    }

	@Override
    public void loadRs() {
        loadRegister(getRsRegisterIndex());
    }

	@Override
    public void loadRt() {
        loadRegister(getRtRegisterIndex());
    }

	@Override
    public void loadRd() {
        loadRegister(getRdRegisterIndex());
    }

	@Override
    public void loadSaValue() {
        loadImm(getSaValue());
    }

    public void loadRegisterIndex(int registerIndex) {
    	loadImm(registerIndex);
    }

    public void loadRsIndex() {
        loadRegisterIndex(getRsRegisterIndex());
    }

    public void loadRtIndex() {
        loadRegisterIndex(getRtRegisterIndex());
    }

    public void loadRdIndex() {
        loadRegisterIndex(getRdRegisterIndex());
    }

	@Override
    public int getImm16(boolean signedImm) {
		return codeInstruction.getImm16(signedImm);
    }

	@Override
	public int getImm14(boolean signedImm) {
		return codeInstruction.getImm14(signedImm);
	}

	@Override
    public void loadImm16(boolean signedImm) {
    	loadImm(getImm16(signedImm));
    }

	@Override
    public void loadImm(int imm) {
		switch (imm) {
			case -1: mv.visitInsn(Opcodes.ICONST_M1); break;
			case  0: mv.visitInsn(Opcodes.ICONST_0);  break;
			case  1: mv.visitInsn(Opcodes.ICONST_1);  break;
			case  2: mv.visitInsn(Opcodes.ICONST_2);  break;
			case  3: mv.visitInsn(Opcodes.ICONST_3);  break;
			case  4: mv.visitInsn(Opcodes.ICONST_4);  break;
			case  5: mv.visitInsn(Opcodes.ICONST_5);  break;
			default:
				if (Byte.MIN_VALUE <= imm && imm < Byte.MAX_VALUE) {
					mv.visitIntInsn(Opcodes.BIPUSH, imm);
				} else if (Short.MIN_VALUE <= imm && imm < Short.MAX_VALUE) {
					mv.visitIntInsn(Opcodes.SIPUSH, imm);
				} else {
					mv.visitLdcInsn(new Integer(imm));
				}
				break;
		}
    }

	public void loadImm(boolean imm) {
		mv.visitInsn(imm ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
	}

	@Override
	public void compileInterpreterInstruction() {
		visitIntepreterCall(codeInstruction.getOpcode(), codeInstruction.getInsn());
	}

	@Override
	public void compileRTRSIMM(String method, boolean signedImm) {
		loadCpu();
		loadRtIndex();
		loadRsIndex();
		loadImm16(signedImm);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cpuInternalName, method, "(III)V");
	}

	@Override
	public void compileRDRT(String method) {
		loadCpu();
		loadRdIndex();
		loadRtIndex();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cpuInternalName, method, "(II)V");
	}

	@Override
	public void storeRd() {
		storeRegister(getRdRegisterIndex());
	}

	@Override
	public void storeRd(int constantValue) {
		storeRegister(getRdRegisterIndex(), constantValue);
	}

	@Override
	public void storeRt() {
		storeRegister(getRtRegisterIndex());
	}

	@Override
	public void storeRt(int constantValue) {
		storeRegister(getRtRegisterIndex(), constantValue);
	}

	@Override
	public boolean isRdRegister0() {
		return getRdRegisterIndex() == _zr;
	}

	@Override
	public boolean isRtRegister0() {
		return getRtRegisterIndex() == _zr;
	}

	@Override
	public boolean isRsRegister0() {
		return getRsRegisterIndex() == _zr;
	}

	@Override
	public void prepareRdForStore() {
		prepareRegisterForStore(getRdRegisterIndex());
	}

	@Override
	public void prepareRtForStore() {
		prepareRegisterForStore(getRtRegisterIndex());
	}

	private void loadMemoryInt() {
		if (storeMemoryIntLocal) {
			mv.visitVarInsn(Opcodes.ALOAD, LOCAL_MEMORY_INT);
		} else {
			mv.visitFieldInsn(Opcodes.GETSTATIC, runtimeContextInternalName, "memoryInt", "[I");
		}
	}

	@Override
	public void memRead32(int registerIndex, int offset) {
		if (RuntimeContext.memoryInt == null) {
			loadMemory();
		} else {
			loadMemoryInt();
		}

		loadRegister(registerIndex);
		if (offset != 0) {
			loadImm(offset);
			mv.visitInsn(Opcodes.IADD);
		}

		if (RuntimeContext.debugMemoryRead) {
			if (!RuntimeContext.debugMemoryReadWriteNoSP || registerIndex != _sp) {
				mv.visitInsn(Opcodes.DUP);
				loadImm(0);
			    loadImm(codeInstruction.getAddress());
				loadImm(1);
				loadImm(32);
			    mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "debugMemoryReadWrite", "(IIIZI)V");
			}
		}

		if (RuntimeContext.memoryInt == null) {
	        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "read32", "(I)I");
		} else {
			if (registerIndex == _sp) {
				// No need to check for a valid memory access when referencing the $sp register
				loadImm(2);
    			mv.visitInsn(Opcodes.IUSHR);
			} else if (checkMemoryAccess()) {
                loadImm(codeInstruction.getAddress());
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryRead32", "(II)I");
                loadImm(2);
                mv.visitInsn(Opcodes.IUSHR);
            } else {
    			// memoryInt[(address & 0x3FFFFFFF) / 4] == memoryInt[(address << 2) >>> 4]
    			loadImm(2);
    			mv.visitInsn(Opcodes.ISHL);
    			loadImm(4);
    			mv.visitInsn(Opcodes.IUSHR);
            }
			mv.visitInsn(Opcodes.IALOAD);
		}
	}

	@Override
	public void memRead16(int registerIndex, int offset) {
		if (RuntimeContext.memoryInt == null) {
			loadMemory();
		} else {
			loadMemoryInt();
		}

		loadRegister(registerIndex);
		if (offset != 0) {
			loadImm(offset);
			mv.visitInsn(Opcodes.IADD);
		}

		if (RuntimeContext.debugMemoryRead) {
			mv.visitInsn(Opcodes.DUP);
			loadImm(0);
            loadImm(codeInstruction.getAddress());
			loadImm(1);
			loadImm(16);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "debugMemoryReadWrite", "(IIIZI)V");
		}

		if (RuntimeContext.memoryInt == null) {
	        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "read16", "(I)I");
		} else {
            if (checkMemoryAccess()) {
                loadImm(codeInstruction.getAddress());
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryRead16", "(II)I");
                loadImm(1);
                mv.visitInsn(Opcodes.IUSHR);
            } else {
    			// memoryInt[(address & 0x3FFFFFFF) / 4] == memoryInt[(address << 2) >>> 4]
    			loadImm(2);
    			mv.visitInsn(Opcodes.ISHL);
    			loadImm(3);
    			mv.visitInsn(Opcodes.IUSHR);
            }
			mv.visitInsn(Opcodes.DUP);
			loadImm(1);
			mv.visitInsn(Opcodes.IAND);
			loadImm(4);
			mv.visitInsn(Opcodes.ISHL);
			storeTmp1();
			loadImm(1);
			mv.visitInsn(Opcodes.IUSHR);
			mv.visitInsn(Opcodes.IALOAD);
			loadTmp1();
			mv.visitInsn(Opcodes.IUSHR);
			loadImm(0xFFFF);
			mv.visitInsn(Opcodes.IAND);
		}
	}

	@Override
	public void memRead8(int registerIndex, int offset) {
		if (RuntimeContext.memoryInt == null) {
			loadMemory();
		} else {
			loadMemoryInt();
		}

		loadRegister(registerIndex);
		if (offset != 0) {
			loadImm(offset);
			mv.visitInsn(Opcodes.IADD);
		}

		if (RuntimeContext.debugMemoryRead) {
			mv.visitInsn(Opcodes.DUP);
			loadImm(0);
            loadImm(codeInstruction.getAddress());
			loadImm(1);
			loadImm(8);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "debugMemoryReadWrite", "(IIIZI)V");
		}

		if (RuntimeContext.memoryInt == null) {
	        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "read8", "(I)I");
		} else {
            if (checkMemoryAccess()) {
                loadImm(codeInstruction.getAddress());
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryRead8", "(II)I");
            } else {
    			// memoryInt[(address & 0x3FFFFFFF) / 4] == memoryInt[(address << 2) >>> 4]
    			loadImm(2);
    			mv.visitInsn(Opcodes.ISHL);
    			loadImm(2);
    			mv.visitInsn(Opcodes.IUSHR);
            }
			mv.visitInsn(Opcodes.DUP);
			loadImm(3);
			mv.visitInsn(Opcodes.IAND);
			loadImm(3);
			mv.visitInsn(Opcodes.ISHL);
			storeTmp1();
			loadImm(2);
			mv.visitInsn(Opcodes.IUSHR);
			mv.visitInsn(Opcodes.IALOAD);
			loadTmp1();
			mv.visitInsn(Opcodes.IUSHR);
			loadImm(0xFF);
			mv.visitInsn(Opcodes.IAND);
		}
	}

	@Override
	public void prepareMemWrite32(int registerIndex, int offset) {
		if (RuntimeContext.memoryInt == null) {
			loadMemory();
		} else {
			loadMemoryInt();
		}

		loadRegister(registerIndex);
		if (offset != 0) {
			loadImm(offset);
			mv.visitInsn(Opcodes.IADD);
		}

		if (RuntimeContext.memoryInt != null) {
			if (registerIndex == _sp) {
				// No need to check for a valid memory access when referencing the $sp register
				loadImm(2);
    			mv.visitInsn(Opcodes.IUSHR);
			} else if (checkMemoryAccess()) {
	            loadImm(codeInstruction.getAddress());
	            mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryWrite32", "(II)I");
                loadImm(2);
                mv.visitInsn(Opcodes.IUSHR);
	        } else {
    			// memoryInt[(address & 0x3FFFFFFF) / 4] == memoryInt[(address << 2) >>> 4]
    			loadImm(2);
    			mv.visitInsn(Opcodes.ISHL);
    			loadImm(4);
    			mv.visitInsn(Opcodes.IUSHR);
	        }
		}

		memWritePrepared = true;
	}

	@Override
	public void memWrite32(int registerIndex, int offset) {
		if (!memWritePrepared) {
			if (RuntimeContext.memoryInt == null) {
				loadMemory();
			} else {
				loadMemoryInt();
			}
			mv.visitInsn(Opcodes.SWAP);

			loadRegister(registerIndex);
			if (offset != 0) {
				loadImm(offset);
				mv.visitInsn(Opcodes.IADD);
			}
            if (checkMemoryAccess()) {
                loadImm(codeInstruction.getAddress());
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryWrite32", "(II)I");
            }
			mv.visitInsn(Opcodes.SWAP);
		}

		if (RuntimeContext.debugMemoryWrite) {
			if (!RuntimeContext.debugMemoryReadWriteNoSP || registerIndex != _sp) {
				mv.visitInsn(Opcodes.DUP2);
				mv.visitInsn(Opcodes.SWAP);
				loadImm(2);
				mv.visitInsn(Opcodes.ISHL);
				mv.visitInsn(Opcodes.SWAP);
			    loadImm(codeInstruction.getAddress());
				loadImm(0);
				loadImm(32);
			    mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "debugMemoryReadWrite", "(IIIZI)V");
			}
		}

		if (RuntimeContext.memoryInt == null) {
	        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "write32", "(II)V");
		} else {
			mv.visitInsn(Opcodes.IASTORE);
		}

		memWritePrepared = false;
	}

	@Override
	public void prepareMemWrite16(int registerIndex, int offset) {
		if (RuntimeContext.memoryInt == null) {
			loadMemory();
		} else {
			loadMemoryInt();
		}

		loadRegister(registerIndex);
		if (offset != 0) {
			loadImm(offset);
			mv.visitInsn(Opcodes.IADD);
		}

		if (RuntimeContext.memoryInt != null) {
			if (checkMemoryAccess()) {
				loadImm(codeInstruction.getAddress());
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryWrite16", "(II)I");
			}
		}

		memWritePrepared = true;
	}

	@Override
	public void memWrite16(int registerIndex, int offset) {
		if (!memWritePrepared) {
			if (RuntimeContext.memoryInt == null) {
				loadMemory();
			} else {
				loadMemoryInt();
			}
			mv.visitInsn(Opcodes.SWAP);

			loadRegister(registerIndex);
			if (offset != 0) {
				loadImm(offset);
				mv.visitInsn(Opcodes.IADD);
			}

			if (RuntimeContext.memoryInt != null) {
				if (checkMemoryAccess()) {
					loadImm(codeInstruction.getAddress());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryWrite16", "(II)I");
				}
			}
			mv.visitInsn(Opcodes.SWAP);
		}

		if (RuntimeContext.memoryInt == null) {
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "write16", "(IS)V");
		} else {
			// tmp2 = value & 0xFFFF;
			// tmp1 = (address & 2) << 3;
			// memoryInt[address >> 2] = (memoryInt[address >> 2] & ((0xFFFF << tmp1) ^ 0xFFFFFFFF)) | (tmp2 << tmp1);
			loadImm(0xFFFF);
			mv.visitInsn(Opcodes.IAND);
			storeTmp2();
			mv.visitInsn(Opcodes.DUP);
			loadImm(2);
			mv.visitInsn(Opcodes.IAND);
			loadImm(3);
			mv.visitInsn(Opcodes.ISHL);
			storeTmp1();
			if (checkMemoryAccess()) {
				loadImm(2);
				mv.visitInsn(Opcodes.ISHR);
			} else {
				loadImm(2);
				mv.visitInsn(Opcodes.ISHL);
				loadImm(4);
				mv.visitInsn(Opcodes.IUSHR);
			}
			mv.visitInsn(Opcodes.DUP2);
			mv.visitInsn(Opcodes.IALOAD);
			loadImm(0xFFFF);
			loadTmp1();
			mv.visitInsn(Opcodes.ISHL);
			loadImm(-1);
			mv.visitInsn(Opcodes.IXOR);
			mv.visitInsn(Opcodes.IAND);
			loadTmp2();
			loadTmp1();
			mv.visitInsn(Opcodes.ISHL);
			mv.visitInsn(Opcodes.IOR);
			mv.visitInsn(Opcodes.IASTORE);
		}

		memWritePrepared = false;
	}

	@Override
	public void prepareMemWrite8(int registerIndex, int offset) {
		if (RuntimeContext.memoryInt == null) {
			loadMemory();
		} else {
			loadMemoryInt();
		}

		loadRegister(registerIndex);
		if (offset != 0) {
			loadImm(offset);
			mv.visitInsn(Opcodes.IADD);
		}

		if (RuntimeContext.memoryInt != null) {
			if (checkMemoryAccess()) {
				loadImm(codeInstruction.getAddress());
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryWrite8", "(II)I");
			}
		}

		memWritePrepared = true;
	}

	@Override
	public void memWrite8(int registerIndex, int offset) {
		if (!memWritePrepared) {
			if (RuntimeContext.memoryInt == null) {
				loadMemory();
			} else {
				loadMemoryInt();
			}
			mv.visitInsn(Opcodes.SWAP);

			loadRegister(registerIndex);
			if (offset != 0) {
				loadImm(offset);
				mv.visitInsn(Opcodes.IADD);
			}

			if (RuntimeContext.memoryInt != null) {
				if (checkMemoryAccess()) {
					loadImm(codeInstruction.getAddress());
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryWrite8", "(II)I");
				}
			}
			mv.visitInsn(Opcodes.SWAP);
		}

		if (RuntimeContext.memoryInt == null) {
	        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "write8", "(IB)V");
		} else {
			// tmp2 = value & 0xFF;
			// tmp1 = (address & 3) << 3;
			// memoryInt[address >> 2] = (memoryInt[address >> 2] & ((0xFF << tmp1) ^ 0xFFFFFFFF)) | (tmp2 << tmp1);
			loadImm(0xFF);
			mv.visitInsn(Opcodes.IAND);
			storeTmp2();
			mv.visitInsn(Opcodes.DUP);
			loadImm(3);
			mv.visitInsn(Opcodes.IAND);
			loadImm(3);
			mv.visitInsn(Opcodes.ISHL);
			storeTmp1();
			if (checkMemoryAccess()) {
				loadImm(2);
				mv.visitInsn(Opcodes.ISHR);
			} else {
				loadImm(2);
				mv.visitInsn(Opcodes.ISHL);
				loadImm(4);
				mv.visitInsn(Opcodes.IUSHR);
			}
			mv.visitInsn(Opcodes.DUP2);
			mv.visitInsn(Opcodes.IALOAD);
			loadImm(0xFF);
			loadTmp1();
			mv.visitInsn(Opcodes.ISHL);
			loadImm(-1);
			mv.visitInsn(Opcodes.IXOR);
			mv.visitInsn(Opcodes.IAND);
			loadTmp2();
			loadTmp1();
			mv.visitInsn(Opcodes.ISHL);
			mv.visitInsn(Opcodes.IOR);
			mv.visitInsn(Opcodes.IASTORE);
		}

		memWritePrepared = false;
	}

	@Override
	public void memWriteZero8(int registerIndex, int offset) {
		if (RuntimeContext.memoryInt == null) {
			loadMemory();
		} else {
			loadMemoryInt();
		}

		loadRegister(registerIndex);
		if (offset != 0) {
			loadImm(offset);
			mv.visitInsn(Opcodes.IADD);
		}

		if (RuntimeContext.memoryInt != null) {
			if (checkMemoryAccess()) {
				loadImm(codeInstruction.getAddress());
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "checkMemoryWrite8", "(II)I");
			}
		}

		if (RuntimeContext.memoryInt == null) {
			loadImm(0);
	        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, memoryInternalName, "write8", "(IB)V");
		} else {
			// tmp1 = (address & 3) << 3;
			// memoryInt[address >> 2] = (memoryInt[address >> 2] & ((0xFF << tmp1) ^ 0xFFFFFFFF));
			mv.visitInsn(Opcodes.DUP);
			loadImm(3);
			mv.visitInsn(Opcodes.IAND);
			loadImm(3);
			mv.visitInsn(Opcodes.ISHL);
			storeTmp1();
			if (checkMemoryAccess()) {
				loadImm(2);
				mv.visitInsn(Opcodes.ISHR);
			} else {
				loadImm(2);
				mv.visitInsn(Opcodes.ISHL);
				loadImm(4);
				mv.visitInsn(Opcodes.IUSHR);
			}
			mv.visitInsn(Opcodes.DUP2);
			mv.visitInsn(Opcodes.IALOAD);
			loadImm(0xFF);
			loadTmp1();
			mv.visitInsn(Opcodes.ISHL);
			loadImm(-1);
			mv.visitInsn(Opcodes.IXOR);
			mv.visitInsn(Opcodes.IAND);
			mv.visitInsn(Opcodes.IASTORE);
		}
	}

	@Override
	public void compileSyscall() {
		visitSyscall(codeInstruction.getOpcode());
	}

	@Override
	public void convertUnsignedIntToLong() {
		mv.visitInsn(Opcodes.I2L);
		mv.visitLdcInsn(0xFFFFFFFFL);
		mv.visitInsn(Opcodes.LAND);
	}

    public int getMethodMaxInstructions() {
        return methodMaxInstructions;
    }

    public void setMethodMaxInstructions(int methodMaxInstructions) {
        this.methodMaxInstructions = methodMaxInstructions;
    }

    private boolean checkMemoryAccess() {
        if (RuntimeContext.memoryInt == null) {
            return false;
        }

        if (RuntimeContext.memory instanceof SafeFastMemory) {
            return true;
        }

        return false;
    }

    public void compileDelaySlotAsBranchTarget(CodeInstruction codeInstruction) {
    	if (codeInstruction.getInsn() == Instructions.NOP) {
    		// NOP nothing to do
    		return;
    	}

    	boolean skipDelaySlotInstruction = true;
    	CodeInstruction previousInstruction = getCodeBlock().getCodeInstruction(codeInstruction.getAddress() - 4);
    	if (previousInstruction != null) {
    		if (Compiler.isEndBlockInsn(previousInstruction.getAddress(), previousInstruction.getOpcode(), previousInstruction.getInsn())) {
    			// The previous instruction was a J, JR or unconditional branch
    			// instruction, we do not need to skip the delay slot instruction
    			skipDelaySlotInstruction = false;
    		}
    	}

    	Label afterDelaySlot = null;
    	if (skipDelaySlotInstruction) {
    		afterDelaySlot = new Label();
    		mv.visitJumpInsn(Opcodes.GOTO, afterDelaySlot);
    	}
    	codeInstruction.compile(this, mv);
    	if (skipDelaySlotInstruction) {
    		mv.visitLabel(afterDelaySlot);
    	}
    }

    public void compileExecuteInterpreter(int startAddress) {
    	loadImm(startAddress);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, runtimeContextInternalName, "executeInterpreter", "(I)I");
        endMethod();
        mv.visitInsn(Opcodes.IRETURN);
    }

	private void visitNativeCodeSequence(NativeCodeSequence nativeCodeSequence, int address, NativeCodeInstruction nativeCodeInstruction) {
    	StringBuilder methodSignature = new StringBuilder("(");
    	int numberParameters = nativeCodeSequence.getNumberParameters();
    	for (int i = 0; i < numberParameters; i++) {
    		loadImm(nativeCodeSequence.getParameterValue(i, address));
    		methodSignature.append("I");
    	}
    	methodSignature.append(")V");
	    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(nativeCodeSequence.getNativeCodeSequenceClass()), nativeCodeSequence.getMethodName(), methodSignature.toString());

	    if (nativeCodeInstruction != null && nativeCodeInstruction.isBranching()) {
    		CodeInstruction targetInstruction = getCodeBlock().getCodeInstruction(nativeCodeInstruction.getBranchingTo());
    		if (targetInstruction != null) {
    			visitJump(Opcodes.GOTO, targetInstruction);
    		} else {
    			visitJump(Opcodes.GOTO, nativeCodeInstruction.getBranchingTo());
    		}
	    }
	}

	public void compileNativeCodeSequence(NativeCodeSequence nativeCodeSequence, NativeCodeInstruction nativeCodeInstruction) {
	    visitNativeCodeSequence(nativeCodeSequence, nativeCodeInstruction.getAddress(), nativeCodeInstruction);

	    if (nativeCodeSequence.isReturning()) {
	    	loadRegister(_ra);
	        endInternalMethod();
	        mv.visitInsn(Opcodes.IRETURN);
	    }

	    // Replacing the whole CodeBlock?
	    if (getCodeBlock().getLength() == nativeCodeSequence.getNumOpcodes() && !nativeCodeSequence.hasBranchInstruction()) {
        	nativeCodeManager.setCompiledNativeCodeBlock(getCodeBlock().getStartAddress(), nativeCodeSequence);

        	// Be more verbose when Debug enabled
	        if (Compiler.log.isDebugEnabled()) {
	        	Compiler.log.debug(String.format("Replacing CodeBlock at 0x%08X (%08X-0x%08X, length %d) by %s", getCodeBlock().getStartAddress(), getCodeBlock().getLowestAddress(), codeBlock.getHighestAddress(), codeBlock.getLength(), nativeCodeSequence));
	        } else if (Compiler.log.isInfoEnabled()) {
	        	Compiler.log.info(String.format("Replacing CodeBlock at 0x%08X by Native Code '%s'", getCodeBlock().getStartAddress(), nativeCodeSequence.getName()));
	        }
	    } else {
        	// Be more verbose when Debug enabled
	    	int endAddress = getCodeInstruction().getAddress() + (nativeCodeSequence.getNumOpcodes() - 1) * 4;
	    	if (Compiler.log.isDebugEnabled()) {
		    	Compiler.log.debug(String.format("Replacing CodeSequence at 0x%08X-0x%08X by Native Code %s", getCodeInstruction().getAddress(), endAddress, nativeCodeSequence));
	        } else if (Compiler.log.isInfoEnabled()) {
		    	Compiler.log.info(String.format("Replacing CodeSequence at 0x%08X-0x%08X by Native Code '%s'", getCodeInstruction().getAddress(), endAddress, nativeCodeSequence.getName()));
	    	}
	    }
	}

	public int getNumberInstructionsToBeSkipped() {
		return numberInstructionsToBeSkipped;
	}

	public boolean isSkipDelaySlot() {
		return skipDelaySlot;
	}

	public void skipInstructions(int numberInstructionsToBeSkipped, boolean skipDelaySlot) {
		this.numberInstructionsToBeSkipped = numberInstructionsToBeSkipped;
		this.skipDelaySlot = skipDelaySlot;
	}

	@Override
	public int getFdRegisterIndex() {
		return codeInstruction.getFdRegisterIndex();
	}

	@Override
	public int getFsRegisterIndex() {
		return codeInstruction.getFsRegisterIndex();
	}

	@Override
	public int getFtRegisterIndex() {
		return codeInstruction.getFtRegisterIndex();
	}

	@Override
	public void loadFd() {
		loadFRegister(getFdRegisterIndex());
	}

	@Override
	public void loadFs() {
		loadFRegister(getFsRegisterIndex());
	}

	@Override
	public void loadFt() {
		loadFRegister(getFtRegisterIndex());
	}

	@Override
	public void prepareFdForStore() {
		prepareFRegisterForStore(getFdRegisterIndex());
	}

	@Override
	public void prepareFtForStore() {
		prepareFRegisterForStore(getFtRegisterIndex());
	}

	@Override
	public void storeFd() {
		storeFRegister(getFdRegisterIndex());
	}

	@Override
	public void storeFt() {
		storeFRegister(getFtRegisterIndex());
	}

	@Override
	public void loadFCr() {
		loadFRegister(getCrValue());
	}

	@Override
	public void prepareFCrForStore() {
		prepareFRegisterForStore(getCrValue());
	}

	@Override
	public void prepareVcrCcForStore(int cc) {
    	if (preparedRegisterForStore < 0) {
        	loadVcr();
            mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Vcr.class), "cc", "[Z");
    		loadImm(cc);
    		preparedRegisterForStore = cc;
    	}
	}

	@Override
	public void storeVcrCc(int cc) {
    	if (preparedRegisterForStore == cc) {
	        mv.visitInsn(Opcodes.BASTORE);
	        preparedRegisterForStore = -1;
    	} else {
        	loadVcr();
            mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Vcr.class), "cc", "[Z");
	        mv.visitInsn(Opcodes.SWAP);
	        loadImm(cc);
	        mv.visitInsn(Opcodes.SWAP);
	        mv.visitInsn(Opcodes.BASTORE);
    	}
	}

	@Override
	public int getCrValue() {
		return codeInstruction.getCrValue();
	}

	@Override
	public void storeFCr() {
		storeFRegister(getCrValue());
	}

	@Override
	public int getVdRegisterIndex() {
		return codeInstruction.getVdRegisterIndex();
	}

	@Override
	public int getVsRegisterIndex() {
		return codeInstruction.getVsRegisterIndex();
	}

	@Override
	public int getVtRegisterIndex() {
		return codeInstruction.getVtRegisterIndex();
	}

	@Override
	public int getVsize() {
		return codeInstruction.getVsize();
	}

	@Override
	public void loadVs(int n) {
		loadVRegister(getVsize(), getVsRegisterIndex(), n, vfpuPfxsState, true);
	}

	@Override
	public void loadVsInt(int n) {
		loadVRegister(getVsize(), getVsRegisterIndex(), n, vfpuPfxsState, false);
	}

	@Override
	public void loadVt(int n) {
		loadVRegister(getVsize(), getVtRegisterIndex(), n, vfpuPfxtState, true);
	}

	@Override
	public void loadVtInt(int n) {
		loadVRegister(getVsize(), getVtRegisterIndex(), n, vfpuPfxtState, false);
	}

	@Override
	public void loadVt(int vsize, int n) {
		loadVRegister(vsize, getVtRegisterIndex(), n, vfpuPfxtState, true);
	}

	@Override
	public void loadVtInt(int vsize, int n) {
		loadVRegister(vsize, getVtRegisterIndex(), n, vfpuPfxtState, false);
	}

	@Override
	public void loadVt(int vsize, int vt, int n) {
		loadVRegister(vsize, vt, n, vfpuPfxtState, true);
	}

	@Override
	public void loadVtInt(int vsize, int vt, int n) {
		loadVRegister(vsize, vt, n, vfpuPfxtState, false);
	}

	@Override
	public void loadVd(int n) {
		loadVRegister(getVsize(), getVdRegisterIndex(), n, null, true);
	}

	@Override
	public void loadVdInt(int n) {
		loadVRegister(getVsize(), getVdRegisterIndex(), n, null, false);
	}

	@Override
	public void loadVd(int vsize, int n) {
		loadVRegister(vsize, getVdRegisterIndex(), n, null, true);
	}

	@Override
	public void loadVdInt(int vsize, int n) {
		loadVRegister(vsize, getVdRegisterIndex(), n, null, false);
	}

	@Override
	public void loadVd(int vsize, int vd, int n) {
		loadVRegister(vsize, vd, n, null, true);
	}

	@Override
	public void loadVdInt(int vsize, int vd, int n) {
		loadVRegister(vsize, vd, n, null, false);
	}

	@Override
	public void prepareVdForStore(int n) {
		prepareVdForStore(getVsize(), n);
	}

	@Override
	public void prepareVdForStore(int vsize, int n) {
		prepareVdForStore(vsize, getVdRegisterIndex(), n);
	}

	@Override
	public void prepareVdForStore(int vsize, int vd, int n) {
		if (pfxVdOverlap && n < vsize - 1) {
			// Do nothing, value will be store in tmp local variable
		} else {
			prepareVRegisterForStore(vsize, vd, n, vfpuPfxdState);
		}
	}

	@Override
	public void prepareVtForStore(int n) {
		prepareVRegisterForStore(getVsize(), getVtRegisterIndex(), n, null);
	}

	@Override
	public void prepareVtForStore(int vsize, int n) {
		prepareVRegisterForStore(vsize, getVtRegisterIndex(), n, null);
	}

	@Override
	public void storeVd(int n) {
		storeVd(getVsize(), n);
	}

	@Override
	public void storeVdInt(int n) {
		storeVdInt(getVsize(), n);
	}

	@Override
	public void storeVd(int vsize, int n) {
		storeVd(vsize, getVdRegisterIndex(), n);
	}

	@Override
	public void storeVdInt(int vsize, int n) {
		storeVdInt(vsize, getVdRegisterIndex(), n);
	}

	@Override
	public void storeVd(int vsize, int vd, int n) {
		if (pfxVdOverlap && n < vsize - 1) {
			storeFTmpVd(n, true);
		} else {
			storeVRegister(vsize, vd, n, vfpuPfxdState, true);
		}
	}

	@Override
	public void storeVdInt(int vsize, int vd, int n) {
		if (pfxVdOverlap && n < vsize - 1) {
			storeFTmpVd(n, false);
		} else {
			storeVRegister(vsize, vd, n, vfpuPfxdState, false);
		}
	}

	@Override
	public void storeVt(int n) {
		storeVRegister(getVsize(), getVtRegisterIndex(), n, null, true);
	}

	@Override
	public void storeVtInt(int n) {
		storeVRegister(getVsize(), getVtRegisterIndex(), n, null, false);
	}

	@Override
	public void storeVt(int vsize, int n) {
		storeVRegister(vsize, getVtRegisterIndex(), n, null, true);
	}

	@Override
	public void storeVtInt(int vsize, int n) {
		storeVRegister(vsize, getVtRegisterIndex(), n, null, false);
	}

	@Override
	public void storeVt(int vsize, int vt, int n) {
		storeVRegister(vsize, vt, n, null, true);
	}

	@Override
	public void storeVtInt(int vsize, int vt, int n) {
		storeVRegister(vsize, vt, n, null, false);
	}

	@Override
	public void prepareVtForStore(int vsize, int vt, int n) {
		prepareVRegisterForStore(vsize, vt, n, null);
	}

	@Override
	public int getImm7() {
		return codeInstruction.getImm7();
	}

	@Override
	public int getImm5() {
		return codeInstruction.getImm5();
	}

	@Override
	public int getImm4() {
		return codeInstruction.getImm4();
	}

	@Override
	public int getImm3() {
		return codeInstruction.getImm3();
	}

	@Override
	public void loadVs(int vsize, int n) {
		loadVRegister(vsize, getVsRegisterIndex(), n, vfpuPfxsState, true);
	}

	@Override
	public void loadVsInt(int vsize, int n) {
		loadVRegister(vsize, getVsRegisterIndex(), n, vfpuPfxsState, false);
	}

	@Override
	public void loadVs(int vsize, int vs, int n) {
		loadVRegister(vsize, vs, n, vfpuPfxsState, true);
	}

	@Override
	public void loadVsInt(int vsize, int vs, int n) {
		loadVRegister(vsize, vs, n, vfpuPfxsState, false);
	}

	@Override
	public void loadTmp1() {
		loadLocalVar(LOCAL_TMP1);
	}

	@Override
	public void loadTmp2() {
		loadLocalVar(LOCAL_TMP2);
	}

	@Override
	public void loadLTmp1() {
		mv.visitVarInsn(Opcodes.LLOAD, LOCAL_TMP1);
	}

	@Override
	public void loadFTmp1() {
        mv.visitVarInsn(Opcodes.FLOAD, LOCAL_TMP1);
	}

	@Override
	public void loadFTmp2() {
        mv.visitVarInsn(Opcodes.FLOAD, LOCAL_TMP2);
	}

	@Override
	public void loadFTmp3() {
        mv.visitVarInsn(Opcodes.FLOAD, LOCAL_TMP3);
	}

	@Override
	public void loadFTmp4() {
        mv.visitVarInsn(Opcodes.FLOAD, LOCAL_TMP4);
	}

	private void loadFTmpVd(int n, boolean isFloat) {
		int opcode = isFloat ? Opcodes.FLOAD : Opcodes.ILOAD;
		if (n == 0) {
	        mv.visitVarInsn(opcode, LOCAL_TMP_VD0);
		} else if (n == 1) {
	        mv.visitVarInsn(opcode, LOCAL_TMP_VD1);
		} else {
	        mv.visitVarInsn(opcode, LOCAL_TMP_VD2);
		}
	}

	@Override
	public void storeTmp1() {
		mv.visitVarInsn(Opcodes.ISTORE, LOCAL_TMP1);
	}

	@Override
	public void storeTmp2() {
		mv.visitVarInsn(Opcodes.ISTORE, LOCAL_TMP2);
	}

	@Override
	public void storeLTmp1() {
		mv.visitVarInsn(Opcodes.LSTORE, LOCAL_TMP1);
	}

	@Override
	public void storeFTmp1() {
		mv.visitVarInsn(Opcodes.FSTORE, LOCAL_TMP1);
	}

	@Override
	public void storeFTmp2() {
		mv.visitVarInsn(Opcodes.FSTORE, LOCAL_TMP2);
	}

	@Override
	public void storeFTmp3() {
		mv.visitVarInsn(Opcodes.FSTORE, LOCAL_TMP3);
	}

	@Override
	public void storeFTmp4() {
		mv.visitVarInsn(Opcodes.FSTORE, LOCAL_TMP4);
	}

	private void storeFTmpVd(int n, boolean isFloat) {
		int opcode = isFloat ? Opcodes.FSTORE : Opcodes.ISTORE;
		if (n == 0) {
			mv.visitVarInsn(opcode, LOCAL_TMP_VD0);
		} else if (n == 1) {
			mv.visitVarInsn(opcode, LOCAL_TMP_VD1);
		} else {
			mv.visitVarInsn(opcode, LOCAL_TMP_VD2);
		}
	}

	@Override
	public VfpuPfxDstState getPfxdState() {
		return vfpuPfxdState;
	}

	@Override
	public VfpuPfxSrcState getPfxsState() {
		return vfpuPfxsState;
	}

	@Override
	public VfpuPfxSrcState getPfxtState() {
		return vfpuPfxtState;
	}

    private void startPfxCompiled(VfpuPfxState vfpuPfxState, String name, String descriptor, String internalName, boolean isFloat) {
        if (vfpuPfxState.isUnknown()) {
            if (interpretPfxLabel == null) {
                interpretPfxLabel = new Label();
            }

            loadVcr();
            mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(Vcr.class), name, descriptor);
            mv.visitFieldInsn(Opcodes.GETFIELD, internalName, "enabled", "Z");
            mv.visitJumpInsn(Opcodes.IFNE, interpretPfxLabel);
        }
    }

    @Override
    public void startPfxCompiled() {
    	startPfxCompiled(true);
    }

    @Override
    public void startPfxCompiled(boolean isFloat) {
        interpretPfxLabel = null;

        if (codeInstruction.hasFlags(Instruction.FLAG_USE_VFPU_PFXS)) {
            startPfxCompiled(vfpuPfxsState, "pfxs", Type.getDescriptor(PfxSrc.class), Type.getInternalName(PfxSrc.class), isFloat);
        }

        if (codeInstruction.hasFlags(Instruction.FLAG_USE_VFPU_PFXT)) {
            startPfxCompiled(vfpuPfxtState, "pfxt", Type.getDescriptor(PfxSrc.class), Type.getInternalName(PfxSrc.class), isFloat);
        }

        if (codeInstruction.hasFlags(Instruction.FLAG_USE_VFPU_PFXD)) {
            startPfxCompiled(vfpuPfxdState, "pfxd", Type.getDescriptor(PfxDst.class), Type.getInternalName(PfxDst.class), isFloat);
        }

        pfxVdOverlap = false;
		if (getCodeInstruction().hasFlags(Instruction.FLAG_USE_VFPU_PFXS | Instruction.FLAG_USE_VFPU_PFXD)) {
			pfxVdOverlap |= isVsVdOverlap();
		}
		if (getCodeInstruction().hasFlags(Instruction.FLAG_USE_VFPU_PFXT | Instruction.FLAG_USE_VFPU_PFXD)) {
			pfxVdOverlap |= isVtVdOverlap();
		}
    }

    @Override
    public void endPfxCompiled() {
    	endPfxCompiled(true);
    }

    @Override
    public void endPfxCompiled(boolean isFloat) {
    	endPfxCompiled(getVsize(), isFloat);
    }

    @Override
    public void endPfxCompiled(int vsize) {
    	endPfxCompiled(vsize, true);
    }

    @Override
    public void endPfxCompiled(int vsize, boolean isFloat) {
    	endPfxCompiled(vsize, isFloat, true);
    }

    @Override
    public void endPfxCompiled(int vsize, boolean isFloat, boolean doFlush) {
    	if (doFlush) {
    		flushPfxCompiled(vsize, getVdRegisterIndex(), isFloat);
    	}

		if (interpretPfxLabel != null) {
            Label continueLabel = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, continueLabel);
            mv.visitLabel(interpretPfxLabel);
            compileInterpreterInstruction();
            mv.visitLabel(continueLabel);

            interpretPfxLabel = null;
        }

        pfxVdOverlap = false;
    }

    @Override
    public void flushPfxCompiled(int vsize, int vd, boolean isFloat) {
		if (pfxVdOverlap) {
			// Write back the temporary overlap variables
			pfxVdOverlap = false;
			for (int n = 0; n < vsize - 1; n++) {
				prepareVdForStore(vsize, vd, n);
				loadFTmpVd(n, isFloat);
				if (isFloat) {
					storeVd(vsize, vd, n);
				} else {
					storeVdInt(vsize, vd, n);
				}
			}
			pfxVdOverlap = true;
		}
    }

    @Override
    public boolean isPfxConsumed(int flag) {
        if (Compiler.log.isTraceEnabled()) {
        	Compiler.log.trace(String.format("PFX -> %08X: %s", getCodeInstruction().getAddress(), getCodeInstruction().getInsn().disasm(getCodeInstruction().getAddress(), getCodeInstruction().getOpcode())));
        }

        int address = getCodeInstruction().getAddress();
        while (true) {
            address += 4;
            CodeInstruction codeInstruction = getCodeBlock().getCodeInstruction(address);

            if (Compiler.log.isTraceEnabled()) {
            	Compiler.log.trace(String.format("PFX    %08X: %s", codeInstruction.getAddress(), codeInstruction.getInsn().disasm(codeInstruction.getAddress(), codeInstruction.getOpcode())));
            }

            if (codeInstruction == null || !isNonBranchingCodeSequence(codeInstruction)) {
                return false;
            }
            if (codeInstruction.hasFlags(flag)) {
            	return codeInstruction.hasFlags(Instruction.FLAG_COMPILED_PFX);
            }
        }
    }

    private boolean isVxVdOverlap(VfpuPfxSrcState pfxSrcState, int registerIndex) {
		if (!pfxSrcState.isKnown()) {
			return false;
		}

		int vsize = getVsize();
		int vd = getVdRegisterIndex();
		// Check if registers are overlapping
		if (registerIndex != vd) {
			if (vsize != 3) {
				// Different register numbers, no overlap possible
				return false;
			}
			// For vsize==3, a possible overlap exist. E.g.
			//    C000.t and C001.t
			// are partially overlapping.
			if ((registerIndex & 63) != (vd & 63)) {
				return false;
			}
		}

		if (!pfxSrcState.pfxSrc.enabled) {
			return true;
		}

		for (int n = 0; n < vsize; n++) {
			if (!pfxSrcState.pfxSrc.cst[n] && pfxSrcState.pfxSrc.swz[n] != n) {
				return true;
			}
		}

		return false;
    }

    @Override
	public boolean isVsVdOverlap() {
    	return isVxVdOverlap(vfpuPfxsState, getVsRegisterIndex());
	}

	@Override
	public boolean isVtVdOverlap() {
    	return isVxVdOverlap(vfpuPfxtState, getVtRegisterIndex());
	}

	@Override
	public void compileVFPUInstr(Object cstBefore, int opcode, String mathFunction) {
		int vsize = getVsize();
		boolean useVt = getCodeInstruction().hasFlags(Instruction.FLAG_USE_VFPU_PFXT);

		if (mathFunction == null &&
		    opcode == Opcodes.NOP &&
		    !useVt &&
		    cstBefore == null &&
		    !(vfpuPfxsState.isKnown() && vfpuPfxsState.pfxSrc.enabled) &&
		    !(vfpuPfxdState.isKnown() && vfpuPfxdState.pfxDst.enabled)) {
			// VMOV should use int instead of float
			startPfxCompiled(false);

			for (int n = 0; n < vsize; n++) {
				prepareVdForStore(n);
				loadVsInt(n);
				storeVdInt(n);
			}

			endPfxCompiled(vsize, false);
		} else {
			startPfxCompiled(true);

			for (int n = 0; n < vsize; n++) {
				prepareVdForStore(n);
				if (cstBefore != null) {
					mv.visitLdcInsn(cstBefore);
				}

				loadVs(n);
				if (useVt) {
					loadVt(n);
				}
				if (mathFunction != null) {
					if ("abs".equals(mathFunction)) {
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), mathFunction, "(F)F");
					} else if ("max".equals(mathFunction) || "min".equals(mathFunction)) {
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), mathFunction, "(FF)F");
					} else {
						mv.visitInsn(Opcodes.F2D);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), mathFunction, "(D)D");
						mv.visitInsn(Opcodes.D2F);
					}
				}
				if (opcode != Opcodes.NOP) {
					mv.visitInsn(opcode);
				}
				storeVd(n);
			}

			endPfxCompiled(vsize, true);
		}
	}
}
