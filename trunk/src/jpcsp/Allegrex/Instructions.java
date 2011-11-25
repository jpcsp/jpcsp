/* This file is part of jpcsp.
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

package jpcsp.Allegrex;


import static jpcsp.Allegrex.Common.Instruction.FLAGS_BRANCH_INSTRUCTION;
import static jpcsp.Allegrex.Common.Instruction.FLAGS_LINK_INSTRUCTION;
import static jpcsp.Allegrex.Common.Instruction.FLAG_CANNOT_BE_SPLIT;
import static jpcsp.Allegrex.Common.Instruction.FLAG_COMPILED_PFX;
import static jpcsp.Allegrex.Common.Instruction.FLAG_ENDS_BLOCK;
import static jpcsp.Allegrex.Common.Instruction.FLAG_HAS_DELAY_SLOT;
import static jpcsp.Allegrex.Common.Instruction.FLAG_IS_JUMPING;
import static jpcsp.Allegrex.Common.Instruction.FLAG_USE_VFPU_PFXD;
import static jpcsp.Allegrex.Common.Instruction.FLAG_USE_VFPU_PFXS;
import static jpcsp.Allegrex.Common.Instruction.FLAG_USE_VFPU_PFXT;
import jpcsp.Processor;
import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.VfpuState.Vcr.PfxDst;
import jpcsp.Allegrex.VfpuState.Vcr.PfxSrc;
import jpcsp.Allegrex.compiler.ICompilerContext;
import jpcsp.HLE.SyscallHandler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This file has been auto-generated from Allegrex.isa file.
 * Changes are now performed directly in this file,
 * Allegrex.isa is no longer used.
 * 
 * @author hli, gid15
 */
public class Instructions {


public static final Instruction NOP = new Instruction(0) {

@Override
public final String name() { return "NOP"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {

return "nop";
}
};
public static final Instruction ICACHE_INDEX_INVALIDATE = new Instruction(1) {

@Override
public final String name() { return "ICACHE INDEX INVALIDATE"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x04, (short)imm16, rs);
}
};
public static final Instruction ICACHE_INDEX_UNLOCK = new Instruction(2) {

@Override
public final String name() { return "ICACHE INDEX UNLOCK"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x06, (short)imm16, rs);
}
};
public static final Instruction ICACHE_HIT_INVALIDATE = new Instruction(3) {

@Override
public final String name() { return "ICACHE HIT INVALIDATE"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x08, (short)imm16, rs);
}
};
public static final Instruction ICACHE_FILL = new Instruction(4) {

@Override
public final String name() { return "ICACHE FILL"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x0A, (short)imm16, rs);
}
};
public static final Instruction ICACHE_FILL_WITH_LOCK = new Instruction(5) {

@Override
public final String name() { return "ICACHE FILL WITH LOCK"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x0B, (short)imm16, rs);
}
};
public static final Instruction DCACHE_INDEX_WRITEBACK_INVALIDATE = new Instruction(6) {

@Override
public final String name() { return "DCACHE INDEX WRITEBACK INVALIDATE"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x14, (short)imm16, rs);
}
};
public static final Instruction DCACHE_INDEX_UNLOCK = new Instruction(7) {

@Override
public final String name() { return "DCACHE INDEX UNLOCK"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x16, (short)imm16, rs);
}
};
public static final Instruction DCACHE_CREATE_DIRTY_EXCLUSIVE = new Instruction(8) {

@Override
public final String name() { return "DCACHE CREATE DIRTY EXCLUSIVE"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x18, (short)imm16, rs);
}
};
public static final Instruction DCACHE_HIT_INVALIDATE = new Instruction(9) {

@Override
public final String name() { return "DCACHE HIT INVALIDATE"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x19, (short)imm16, rs);
}
};
public static final Instruction DCACHE_HIT_WRITEBACK = new Instruction(10) {

@Override
public final String name() { return "DCACHE HIT WRITEBACK"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1A, (short)imm16, rs);
}
};
public static final Instruction DCACHE_HIT_WRITEBACK_INVALIDATE = new Instruction(11) {

@Override
public final String name() { return "DCACHE HIT WRITEBACK INVALIDATE"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1B, (short)imm16, rs);
}
};
public static final Instruction DCACHE_CREATE_DIRTY_EXCLUSIVE_WITH_LOCK = new Instruction(12) {

@Override
public final String name() { return "DCACHE CREATE DIRTY EXCLUSIVE WITH LOCK"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1C, (short)imm16, rs);
}
};
public static final Instruction DCACHE_FILL = new Instruction(13) {

@Override
public final String name() { return "DCACHE FILL"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1E, (short)imm16, rs);
}
};
public static final Instruction DCACHE_FILL_WITH_LOCK = new Instruction(14) {

@Override
public final String name() { return "DCACHE FILL WITH LOCK"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm16 = (insn>>0)&65535;
	//int rs = (insn>>21)&31;


}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1F, (short)imm16, rs);
}
};
public static final Instruction SYSCALL = new Instruction(15) {

@Override
public final String name() { return "SYSCALL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;


                SyscallHandler.syscall(imm20);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileSyscall();
}
@Override
public String disasm(int address, int insn) {
	int imm20 = (insn>>6)&1048575;

return Common.disasmSYSCALL(imm20);
}
};
public static final Instruction ERET = new Instruction(16) {

@Override
public final String name() { return "ERET"; }

@Override
public final String category() { return "MIPS III"; }

@Override
public void interpret(Processor processor, int insn) {


}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {

return "eret";
}
};
public static final Instruction BREAK = new Instruction(17) {

@Override
public final String name() { return "BREAK"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	//int imm20 = (insn>>6)&1048575;


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm20 = (insn>>6)&1048575;

return Common.disasmBREAK(imm20);
}
};
public static final Instruction SYNC = new Instruction(18) {

@Override
public final String name() { return "SYNC"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {

return "sync";
}
};
public static final Instruction HALT = new Instruction(19) {

@Override
public final String name() { return "HALT"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {

return "halt";
}
};
public static final Instruction MFIC = new Instruction(20) {

@Override
public final String name() { return "MFIC"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int rt = (insn>>21)&31;


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>21)&31;

return Common.disasmRT("mfic", rt);
}
};
public static final Instruction MTIC = new Instruction(21) {

@Override
public final String name() { return "MTIC"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int rt = (insn>>21)&31;


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>21)&31;

return Common.disasmRT("mtic", rt);
}
};
public static final Instruction ADD = new Instruction(22) {

@Override
public final String name() { return "ADD"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                // just ignore overflow exception as it is useless
                processor.cpu.doADDU(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRsRegister0() && context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			if (context.isRsRegister0()) {
				context.loadRt();
			} else {
				context.loadRs();
				if (!context.isRtRegister0()) {
					context.loadRt();
					context.getMethodVisitor().visitInsn(Opcodes.IADD);
				}
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("add", rd, rs, rt);
}
};
public static final Instruction ADDU = new Instruction(23) {

@Override
public final String name() { return "ADDU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doADDU(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	ADD.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("addu", rd, rs, rt);
}
};
public static final Instruction ADDI = new Instruction(24) {

@Override
public final String name() { return "ADDI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                // just ignore overflow exception as it is useless
                processor.cpu.doADDIU(rt, rs, (short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	ADDIU.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("addi", rt, rs, (short)imm16);
}
};
public static final Instruction ADDIU = new Instruction(25) {

@Override
public final String name() { return "ADDIU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doADDIU(rt, rs, (short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int imm = context.getImm16(true);
		if (context.isRsRegister0()) {
			context.storeRt(imm);
		} else if (imm == 0 && context.getRsRegisterIndex() == context.getRtRegisterIndex()) {
			// Incrementing a register by 0 is a No-OP:
			// ADDIU $reg, $reg, 0
		} else {
			context.prepareRtForStore();
			context.loadRs();
			if (imm != 0) {
				context.loadImm(imm);
				context.getMethodVisitor().visitInsn(Opcodes.IADD);
			}
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("addiu", rt, rs, (short)imm16);
}
};
public static final Instruction AND = new Instruction(26) {

@Override
public final String name() { return "AND"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doAND(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRsRegister0() || context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRs();
			context.loadRt();
			context.getMethodVisitor().visitInsn(Opcodes.IAND);
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("and", rd, rs, rt);
}
};
public static final Instruction ANDI = new Instruction(27) {

@Override
public final String name() { return "ANDI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doANDI(rt, rs, imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int imm = context.getImm16(false);
		if (imm == 0 || context.isRsRegister0()) {
			context.storeRt(0);
		} else {
			context.prepareRtForStore();
			context.loadRs();
			context.loadImm(imm);
			context.getMethodVisitor().visitInsn(Opcodes.IAND);
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("andi", rt, rs, imm16);
}
};
public static final Instruction NOR = new Instruction(28) {

@Override
public final String name() { return "NOR"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doNOR(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		context.prepareRdForStore();
		context.loadRs();
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.IOR);
		context.loadImm(-1);
		context.getMethodVisitor().visitInsn(Opcodes.IXOR);
		context.storeRd();
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("nor", rd, rs, rt);
}
};
public static final Instruction OR = new Instruction(29) {

@Override
public final String name() { return "OR"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doOR(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRsRegister0() && context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			if (context.isRsRegister0()) {
				context.loadRt();
			} else {
				context.loadRs();
				if (!context.isRtRegister0()) {
					context.loadRt();
					context.getMethodVisitor().visitInsn(Opcodes.IOR);
				}
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("or", rd, rs, rt);
}
};
public static final Instruction ORI = new Instruction(30) {

@Override
public final String name() { return "ORI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doORI(rt, rs, imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int imm = context.getImm16(false);
		if (context.isRsRegister0()) {
			context.storeRt(imm);
		} else if (imm == 0 && context.getRsRegisterIndex() == context.getRtRegisterIndex()) {
			// Or-ing a register with 0 and himself is a No-OP:
			// ORI $reg, $reg, 0
		} else {
			context.prepareRtForStore();
			context.loadRs();
			if (imm != 0) {
				context.loadImm(imm);
				context.getMethodVisitor().visitInsn(Opcodes.IOR);
			}
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("ori", rt, rs, imm16);
}
};
public static final Instruction XOR = new Instruction(31) {

@Override
public final String name() { return "XOR"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doXOR(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRsRegister0() && context.isRtRegister0()) {
			context.storeRd(0);
		} else if (context.getRtRegisterIndex() == context.getRsRegisterIndex()) {
			// XOR-ing a register with himself is equivalent to setting to 0.
			// XOR $rd, $rs, $rs
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			if (context.isRsRegister0()) {
				context.loadRt();
			} else {
				context.loadRs();
				if (!context.isRtRegister0()) {
					context.loadRt();
					context.getMethodVisitor().visitInsn(Opcodes.IXOR);
				}
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("xor", rd, rs, rt);
}
};
public static final Instruction XORI = new Instruction(32) {

@Override
public final String name() { return "XORI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doXORI(rt, rs, imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int imm = context.getImm16(false);
		if (context.isRsRegister0()) {
			context.storeRt(imm);
		} else if (imm == 0 && context.getRtRegisterIndex() == context.getRsRegisterIndex()) {
			// XOR-ing a register with 0 and storing the result in the same register is a No-OP.
			// XORI $reg, $reg, 0
		} else {
			context.prepareRtForStore();
			context.loadRs();
			if (imm != 0) {
				context.loadImm(imm);
				context.getMethodVisitor().visitInsn(Opcodes.IXOR);
			}
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("xori", rt, rs, imm16);
}
};
public static final Instruction SLL = new Instruction(33) {

@Override
public final String name() { return "SLL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSLL(rd, rt, sa);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			int sa = context.getSaValue();
			if (sa != 0) {
				context.loadImm(sa);
				context.getMethodVisitor().visitInsn(Opcodes.ISHL);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("sll", rd, rt, sa);
}
};
public static final Instruction SLLV = new Instruction(34) {

@Override
public final String name() { return "SLLV"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLLV(rd, rt, rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			if (!context.isRsRegister0()) {
				context.loadRs();
				context.loadImm(31);
				context.getMethodVisitor().visitInsn(Opcodes.IAND);
				context.getMethodVisitor().visitInsn(Opcodes.ISHL);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("sllv", rd, rt, rs);
}
};
public static final Instruction SRA = new Instruction(35) {

@Override
public final String name() { return "SRA"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSRA(rd, rt, sa);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			int sa = context.getSaValue();
			if (sa != 0) {
				context.loadImm(sa);
				context.getMethodVisitor().visitInsn(Opcodes.ISHR);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("sra", rd, rt, sa);
}
};
public static final Instruction SRAV = new Instruction(36) {

@Override
public final String name() { return "SRAV"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSRAV(rd, rt, rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			if (!context.isRsRegister0()) {
				context.loadRs();
				context.loadImm(31);
				context.getMethodVisitor().visitInsn(Opcodes.IAND);
				context.getMethodVisitor().visitInsn(Opcodes.ISHR);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("srav", rd, rt, rs);
}
};
public static final Instruction SRL = new Instruction(37) {

@Override
public final String name() { return "SRL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSRL(rd, rt, sa);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			int sa = context.getSaValue();
			if (sa != 0) {
				context.loadImm(sa);
				context.getMethodVisitor().visitInsn(Opcodes.IUSHR);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("srl", rd, rt, sa);
}
};
public static final Instruction SRLV = new Instruction(38) {

@Override
public final String name() { return "SRLV"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSRLV(rd, rt, rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			if (!context.isRsRegister0()) {
				context.loadRs();
				context.loadImm(31);
				context.getMethodVisitor().visitInsn(Opcodes.IAND);
				context.getMethodVisitor().visitInsn(Opcodes.IUSHR);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("srlv", rd, rt, rs);
}
};
public static final Instruction ROTR = new Instruction(39) {

@Override
public final String name() { return "ROTR"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doROTR(rd, rt, sa);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			int sa = context.getSaValue();
			if (sa != 0) {
				// rotateRight(rt, sa) = (rt >>> sa | rt << -sa)
				context.getMethodVisitor().visitInsn(Opcodes.DUP);
				context.loadImm(sa);
				context.getMethodVisitor().visitInsn(Opcodes.IUSHR);
				context.getMethodVisitor().visitInsn(Opcodes.SWAP);
				context.loadImm(-sa);
				context.getMethodVisitor().visitInsn(Opcodes.ISHL);
				context.getMethodVisitor().visitInsn(Opcodes.IOR);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("rotr", rd, rt, sa);
}
};
public static final Instruction ROTRV = new Instruction(40) {

@Override
public final String name() { return "ROTRV"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doROTRV(rd, rt, rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRt();
			if (!context.isRsRegister0()) {
				// rotateRight(rt, rs) = (rt >>> rs | rt << -rs)
				context.loadRs();
				context.loadImm(31);
				context.getMethodVisitor().visitInsn(Opcodes.IAND);
				context.getMethodVisitor().visitInsn(Opcodes.DUP2);
				context.getMethodVisitor().visitInsn(Opcodes.IUSHR);
				context.getMethodVisitor().visitInsn(Opcodes.DUP_X2);
				context.getMethodVisitor().visitInsn(Opcodes.POP);
				context.getMethodVisitor().visitInsn(Opcodes.INEG);
				context.getMethodVisitor().visitInsn(Opcodes.ISHL);
				context.getMethodVisitor().visitInsn(Opcodes.IOR);
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("rotrv", rd, rt, rs);
}
};
public static final Instruction SLT = new Instruction(41) {

@Override
public final String name() { return "SLT"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLT(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.getRsRegisterIndex() == context.getRtRegisterIndex()) {
			context.storeRd(0);
		} else {
			MethodVisitor mv = context.getMethodVisitor();
			Label ifLtLabel = new Label();
			Label continueLabel = new Label();
			context.prepareRdForStore();
			if (context.isRsRegister0()) {
				// rd = 0 < rt ? 1 : 0
				context.loadRt();
				mv.visitJumpInsn(Opcodes.IFGT, ifLtLabel);
			} else if (context.isRtRegister0()) {
				// rd = rs < 0 ? 1 : 0
				context.loadRs();
				mv.visitJumpInsn(Opcodes.IFLT, ifLtLabel);
			} else {
				// rd = rs < rt ? 1 : 0
				context.loadRs();
				context.loadRt();
				mv.visitJumpInsn(Opcodes.IF_ICMPLT, ifLtLabel);
			}
			context.loadImm(0);
			mv.visitJumpInsn(Opcodes.GOTO, continueLabel);
			mv.visitLabel(ifLtLabel);
			context.loadImm(1);
			mv.visitLabel(continueLabel);
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("slt", rd, rs, rt);
}
};
public static final Instruction SLTI = new Instruction(42) {

@Override
public final String name() { return "SLTI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLTI(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int simm16 = context.getImm16(true);
		if (context.isRsRegister0()) {
			context.storeRt(simm16 > 0 ? 1 : 0);
		} else {
	        MethodVisitor mv = context.getMethodVisitor();
	        Label ifLtLabel = new Label();
	        Label continueLabel = new Label();
			context.prepareRtForStore();
	        // rt = rs < simm16 ? 1 : 0
	        context.loadRs();
	        if (simm16 == 0) {
	        	mv.visitJumpInsn(Opcodes.IFLT, ifLtLabel);
	        } else {
	        	context.loadImm(simm16);
	        	mv.visitJumpInsn(Opcodes.IF_ICMPLT, ifLtLabel);
	        }
	        context.loadImm(0);
	        mv.visitJumpInsn(Opcodes.GOTO, continueLabel);
	        mv.visitLabel(ifLtLabel);
	        context.loadImm(1);
	        mv.visitLabel(continueLabel);
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("slti", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SLTU = new Instruction(43) {

@Override
public final String name() { return "SLTU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLTU(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.getRsRegisterIndex() == context.getRtRegisterIndex()) {
			// rd = x < x
			context.storeRd(0);
		} else if (context.isRtRegister0()) {
			// rd = rs < 0
			context.storeRd(0);
		} else {
	        MethodVisitor mv = context.getMethodVisitor();
	        Label ifLtLabel = new Label();
	        Label continueLabel = new Label();
			context.prepareRdForStore();
			if (context.isRsRegister0()) {
				// rd = 0 < rt ? 1 : 0
				context.loadRt();
				mv.visitJumpInsn(Opcodes.IFNE, ifLtLabel);
			} else {
		        // rd = rs < rt ? 1 : 0
		        context.loadRs();
		        context.convertUnsignedIntToLong();
		        context.loadRt();
		        context.convertUnsignedIntToLong();
		        mv.visitInsn(Opcodes.LCMP); // -1 if rs < rt, 0 if rs == rt, 1 if rs > rt
		        mv.visitJumpInsn(Opcodes.IFLT, ifLtLabel);
			}
	        context.loadImm(0);
	        mv.visitJumpInsn(Opcodes.GOTO, continueLabel);
	        mv.visitLabel(ifLtLabel);
	        context.loadImm(1);
	        mv.visitLabel(continueLabel);
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("sltu", rd, rs, rt);
}
};
public static final Instruction SLTIU = new Instruction(44) {

@Override
public final String name() { return "SLTIU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLTIU(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int simm16 = context.getImm16(true);
		if (context.isRsRegister0()) {
			// rt = 0 < simm16 ? 1 : 0
			context.storeRt(0 < simm16 ? 1 : 0);
		} else if (simm16 == 0) {
			// rt = rs < 0
			context.storeRt(0);
		} else {
	        MethodVisitor mv = context.getMethodVisitor();
	        Label ifLtLabel = new Label();
	        Label continueLabel = new Label();
			context.prepareRtForStore();
			if (simm16 == 1) {
				// rt = rs < 1 ? 1 : 0   <=> rt = rs == 0 ? 1 : 0 
				context.loadRs();
				mv.visitJumpInsn(Opcodes.IFEQ, ifLtLabel);
			} else {
		        // rt = rs < simm16 ? 1 : 0
		        context.loadRs();
		        context.convertUnsignedIntToLong();
		        mv.visitLdcInsn(((long) simm16) & 0xFFFFFFFFL);
		        mv.visitInsn(Opcodes.LCMP); // -1 if rs < rt, 0 if rs == rt, 1 if rs > rt
		        mv.visitJumpInsn(Opcodes.IFLT, ifLtLabel);
			}
	        context.loadImm(0);
	        mv.visitJumpInsn(Opcodes.GOTO, continueLabel);
	        mv.visitLabel(ifLtLabel);
	        context.loadImm(1);
	        mv.visitLabel(continueLabel);
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("sltiu", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SUB = new Instruction(45) {

@Override
public final String name() { return "SUB"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                // just ignore overflow exception as it is useless
                processor.cpu.doSUBU(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRsRegister0() && context.isRtRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			if (context.isRsRegister0()) {
				context.loadRt();
				context.getMethodVisitor().visitInsn(Opcodes.INEG);
			} else {
				context.loadRs();
				if (!context.isRtRegister0()) {
					context.loadRt();
					context.getMethodVisitor().visitInsn(Opcodes.ISUB);
				}
			}
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("sub", rd, rs, rt);
}
};
public static final Instruction SUBU = new Instruction(46) {

@Override
public final String name() { return "SUBU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSUBU(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	SUB.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("subu", rd, rs, rt);
}
};
public static final Instruction LUI = new Instruction(47) {

@Override
public final String name() { return "LUI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;


                processor.cpu.doLUI(rt, imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int uimm16 = context.getImm16(false);
		context.storeRt(uimm16 << 16);
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;

return Common.disasmRTIMM("lui", rt, imm16);
}
};
public static final Instruction SEB = new Instruction(48) {

@Override
public final String name() { return "SEB"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSEB(rd, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		context.prepareRdForStore();
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.I2B);
		context.storeRd();
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("seb", rd, rt);
}
};
public static final Instruction SEH = new Instruction(49) {

@Override
public final String name() { return "SEH"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSEH(rd, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		context.prepareRdForStore();
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.I2S);
		context.storeRd();
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("seh", rd, rt);
}
};
public static final Instruction BITREV = new Instruction(50) {

@Override
public final String name() { return "BITREV"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doBITREV(rd, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRDRT("doBITREV");
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("bitrev", rd, rt);
}
};
public static final Instruction WSBH = new Instruction(51) {

@Override
public final String name() { return "WSBH"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doWSBH(rd, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRDRT("doWSBH");
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("wsbh", rd, rt);
}
};
public static final Instruction WSBW = new Instruction(52) {

@Override
public final String name() { return "WSBW"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doWSBW(rd, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRDRT("doWSBW");
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("wsbw", rd, rt);
}
};
public static final Instruction MOVZ = new Instruction(53) {

@Override
public final String name() { return "MOVZ"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMOVZ(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.loadRt();
	Label doNotChange = new Label();
	context.getMethodVisitor().visitJumpInsn(Opcodes.IFNE, doNotChange);
	context.prepareRdForStore();
	context.loadRs();
	context.storeRd();
	context.getMethodVisitor().visitLabel(doNotChange);
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("movz", rd, rs, rt);
}
};
public static final Instruction MOVN = new Instruction(54) {

@Override
public final String name() { return "MOVN"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMOVN(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.loadRt();
	Label doNotChange = new Label();
	context.getMethodVisitor().visitJumpInsn(Opcodes.IFEQ, doNotChange);
	context.prepareRdForStore();
	context.loadRs();
	context.storeRd();
	context.getMethodVisitor().visitLabel(doNotChange);
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("movn", rd, rs, rt);
}
};
public static final Instruction MAX = new Instruction(55) {

@Override
public final String name() { return "MAX"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMAX(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		context.prepareRdForStore();
		context.loadRs();
		context.loadRt();
		Label case1Label = new Label();
		Label continueLabel = new Label();
		context.getMethodVisitor().visitJumpInsn(Opcodes.IF_ICMPLE, case1Label);
		context.loadRs();
		context.getMethodVisitor().visitJumpInsn(Opcodes.GOTO, continueLabel);
		context.getMethodVisitor().visitLabel(case1Label);
		context.loadRt();
		context.getMethodVisitor().visitLabel(continueLabel);
		context.storeRd();
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("max", rd, rs, rt);
}
};
public static final Instruction MIN = new Instruction(56) {

@Override
public final String name() { return "MIN"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMIN(rd, rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		context.prepareRdForStore();
		context.loadRs();
		context.loadRt();
		Label case1Label = new Label();
		Label continueLabel = new Label();
		context.getMethodVisitor().visitJumpInsn(Opcodes.IF_ICMPGE, case1Label);
		context.loadRs();
		context.getMethodVisitor().visitJumpInsn(Opcodes.GOTO, continueLabel);
		context.getMethodVisitor().visitLabel(case1Label);
		context.loadRt();
		context.getMethodVisitor().visitLabel(continueLabel);
		context.storeRd();
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("min", rd, rs, rt);
}
};
public static final Instruction CLZ = new Instruction(57) {

@Override
public final String name() { return "CLZ"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doCLZ(rd, rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRsRegister0()) {
			context.storeRd(32);
		} else {
			context.prepareRdForStore();
			context.loadRs();
			context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "numberOfLeadingZeros", "(I)I");
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRS("clz", rd, rs);
}
};
public static final Instruction CLO = new Instruction(58) {

@Override
public final String name() { return "CLO"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doCLO(rd, rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		if (context.isRsRegister0()) {
			context.storeRd(0);
		} else {
			context.prepareRdForStore();
			context.loadRs();
			context.loadImm(-1);
			context.getMethodVisitor().visitInsn(Opcodes.IXOR);
			context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "numberOfLeadingZeros", "(I)I");
			context.storeRd();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRS("clo", rd, rs);
}
};
public static final Instruction EXT = new Instruction(59) {

@Override
public final String name() { return "EXT"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doEXT(rt, rs, lsb, msb);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int lsb = (insn>>6)&31;
		int msb = (insn>>11)&31;
		int mask = ~(~0 << (msb + 1));
		if (context.isRsRegister0() || mask == 0) {
			context.storeRt(0);
		} else {
			context.prepareRtForStore();
			context.loadRs();
			if (lsb != 0) {
				context.loadImm(lsb);
				context.getMethodVisitor().visitInsn(Opcodes.IUSHR);
			}
			if (mask != 0xFFFFFFFF) {
				context.loadImm(mask);
				context.getMethodVisitor().visitInsn(Opcodes.IAND);
			}
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmEXT(rt, rs, lsb, msb);
}
};
public static final Instruction INS = new Instruction(60) {

@Override
public final String name() { return "INS"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doINS(rt, rs, lsb, msb);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int lsb = (insn>>6)&31;
		int msb = (insn>>11)&31;
        int mask = ~(~0 << (msb - lsb + 1)) << lsb;

        if (mask == 0xFFFFFFFF && context.isRsRegister0()) {
        	context.storeRt(0);
        } else if (mask != 0) {
			context.prepareRtForStore();
			if (mask == 0xFFFFFFFF) {
				context.loadRs();
				if (lsb != 0) {
					context.loadImm(lsb);
					context.getMethodVisitor().visitInsn(Opcodes.ISHL);
				}
			} else {
				context.loadRt();
				context.loadImm(~mask);
				context.getMethodVisitor().visitInsn(Opcodes.IAND);
				if (!context.isRsRegister0()) {
					context.loadRs();
					if (lsb != 0) {
						context.loadImm(lsb);
						context.getMethodVisitor().visitInsn(Opcodes.ISHL);
					}
					if (mask != 0xFFFFFFFF) {
						context.loadImm(mask);
						context.getMethodVisitor().visitInsn(Opcodes.IAND);
					}
					context.getMethodVisitor().visitInsn(Opcodes.IOR);
				}
			}
			context.storeRt();
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmINS(rt, rs, lsb, msb);
}
};
public static final Instruction MULT = new Instruction(61) {

@Override
public final String name() { return "MULT"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMULT(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareHiloForStore();
	if (context.isRsRegister0() || context.isRtRegister0()) {
		context.getMethodVisitor().visitLdcInsn(0L);
	} else {
		context.loadRs();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.getMethodVisitor().visitInsn(Opcodes.LMUL);
	}
	context.storeHilo();
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("mult", rs, rt);
}
};
public static final Instruction MULTU = new Instruction(62) {

@Override
public final String name() { return "MULTU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMULTU(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareHiloForStore();
	if (context.isRsRegister0() || context.isRtRegister0()) {
		context.getMethodVisitor().visitLdcInsn(0L);
	} else {
		context.loadRs();
		context.convertUnsignedIntToLong();
		context.loadRt();
		context.convertUnsignedIntToLong();
		context.getMethodVisitor().visitInsn(Opcodes.LMUL);
	}
	context.storeHilo();
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("multu", rs, rt);
}
};
public static final Instruction MADD = new Instruction(63) {

@Override
public final String name() { return "MADD"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMADD(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRsRegister0() && !context.isRtRegister0()) {
		context.prepareHiloForStore();
		context.loadHilo();
		context.loadRs();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.getMethodVisitor().visitInsn(Opcodes.LMUL);
		context.getMethodVisitor().visitInsn(Opcodes.LADD);
		context.storeHilo();
	}
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("madd", rs, rt);
}
};
public static final Instruction MADDU = new Instruction(64) {

@Override
public final String name() { return "MADDU"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMADDU(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRsRegister0() && !context.isRtRegister0()) {
		context.prepareHiloForStore();
		context.loadHilo();
		context.loadRs();
		context.convertUnsignedIntToLong();
		context.loadRt();
		context.convertUnsignedIntToLong();
		context.getMethodVisitor().visitInsn(Opcodes.LMUL);
		context.getMethodVisitor().visitInsn(Opcodes.LADD);
		context.storeHilo();
	}
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("maddu", rs, rt);
}
};
public static final Instruction MSUB = new Instruction(65) {

@Override
public final String name() { return "MSUB"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMSUB(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRsRegister0() && !context.isRtRegister0()) {
		context.prepareHiloForStore();
		context.loadHilo();
		context.loadRs();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.getMethodVisitor().visitInsn(Opcodes.LMUL);
		context.getMethodVisitor().visitInsn(Opcodes.LSUB);
		context.storeHilo();
	}
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("msub", rs, rt);
}
};
public static final Instruction MSUBU = new Instruction(66) {

@Override
public final String name() { return "MSUBU"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMSUBU(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRsRegister0() && !context.isRtRegister0()) {
		context.prepareHiloForStore();
		context.loadHilo();
		context.loadRs();
		context.convertUnsignedIntToLong();
		context.loadRt();
		context.convertUnsignedIntToLong();
		context.getMethodVisitor().visitInsn(Opcodes.LMUL);
		context.getMethodVisitor().visitInsn(Opcodes.LSUB);
		context.storeHilo();
	}
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("msubu", rs, rt);
}
};
public static final Instruction DIV = new Instruction(67) {

@Override
public final String name() { return "DIV"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doDIV(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
        // According to MIPS spec., result is unpredictable when dividing by zero.
		// Here, do nothing when dividing by zero.
		Label divideByZero = new Label();
		context.loadRt();
		context.getMethodVisitor().visitJumpInsn(Opcodes.IFEQ, divideByZero);

		context.loadRs();
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.DUP2);
		context.getMethodVisitor().visitInsn(Opcodes.IREM);
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.loadImm(32);
		context.getMethodVisitor().visitInsn(Opcodes.LSHL);
		context.getMethodVisitor().visitInsn(Opcodes.DUP2_X2);
		context.getMethodVisitor().visitInsn(Opcodes.POP2);
		context.getMethodVisitor().visitInsn(Opcodes.IDIV);
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.getMethodVisitor().visitLdcInsn(0x00000000FFFFFFFFL);
		context.getMethodVisitor().visitInsn(Opcodes.LAND);
		context.getMethodVisitor().visitInsn(Opcodes.LOR);
		context.storeHilo();

		context.getMethodVisitor().visitLabel(divideByZero);
	}
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("div", rs, rt);
}
};
public static final Instruction DIVU = new Instruction(68) {

@Override
public final String name() { return "DIVU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doDIVU(rs, rt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
        // According to MIPS spec., result is unpredictable when dividing by zero.
		// Here, do nothing when dividing by zero.
		Label divideByZero = new Label();
		context.loadRt();
		context.getMethodVisitor().visitJumpInsn(Opcodes.IFEQ, divideByZero);

		context.loadRs();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.getMethodVisitor().visitLdcInsn(0x00000000FFFFFFFFL);
		context.getMethodVisitor().visitInsn(Opcodes.LAND);
		context.getMethodVisitor().visitInsn(Opcodes.DUP2);
		context.loadRt();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.getMethodVisitor().visitLdcInsn(0x00000000FFFFFFFFL);
		context.getMethodVisitor().visitInsn(Opcodes.LAND);
		context.getMethodVisitor().visitInsn(Opcodes.DUP2_X2);
		context.getMethodVisitor().visitInsn(Opcodes.LREM);
		context.loadImm(32);
		context.getMethodVisitor().visitInsn(Opcodes.LSHL);
		context.storeLTmp1();
		context.getMethodVisitor().visitInsn(Opcodes.LDIV);
		context.getMethodVisitor().visitLdcInsn(0x00000000FFFFFFFFL);
		context.getMethodVisitor().visitInsn(Opcodes.LAND);
		context.loadLTmp1();
		context.getMethodVisitor().visitInsn(Opcodes.LOR);
		context.storeHilo();

		context.getMethodVisitor().visitLabel(divideByZero);
	}
}
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("divu", rs, rt);
}
};
public static final Instruction MFHI = new Instruction(69) {

@Override
public final String name() { return "MFHI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;


                processor.cpu.doMFHI(rd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		context.prepareRdForStore();
		context.loadHilo();
		context.loadImm(32);
		context.getMethodVisitor().visitInsn(Opcodes.LUSHR);
		context.getMethodVisitor().visitInsn(Opcodes.L2I);
		context.storeRd();
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;

return Common.disasmRD("mfhi", rd);
}
};
public static final Instruction MFLO = new Instruction(70) {

@Override
public final String name() { return "MFLO"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;


                processor.cpu.doMFLO(rd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRdRegister0()) {
		context.prepareRdForStore();
		context.loadHilo();
		context.getMethodVisitor().visitLdcInsn(0xFFFFFFFFL);
		context.getMethodVisitor().visitInsn(Opcodes.LAND);
		context.getMethodVisitor().visitInsn(Opcodes.L2I);
		context.storeRd();
	}
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;

return Common.disasmRD("mflo", rd);
}
};
public static final Instruction MTHI = new Instruction(71) {

@Override
public final String name() { return "MTHI"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;


                processor.cpu.doMTHI(rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int rs = (insn>>21)&31;

return Common.disasmRS("mthi", rs);
}
};
public static final Instruction MTLO = new Instruction(72) {

@Override
public final String name() { return "MTLO"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;


                processor.cpu.doMTLO(rs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.loadHilo();
	context.getMethodVisitor().visitLdcInsn(0xFFFFFFFF00000000L);
	context.getMethodVisitor().visitInsn(Opcodes.LAND);
	if (!context.isRsRegister0()) {
		context.loadRs();
		context.getMethodVisitor().visitInsn(Opcodes.I2L);
		context.getMethodVisitor().visitLdcInsn(0x00000000FFFFFFFFL);
		context.getMethodVisitor().visitInsn(Opcodes.LAND);
		context.getMethodVisitor().visitInsn(Opcodes.LOR);
	}
	context.storeHilo();
}
@Override
public String disasm(int address, int insn) {
	int rs = (insn>>21)&31;

return Common.disasmRS("mtlo", rs);
}
};

//
// BEQ has the flag "FLAG_ENDS_BLOCK" because it can end a block when the
// conditional branch can be reduced to an unconditional branch (rt == rs).
//    "BEQ $xx, $xx, target"
// is equivalent to
//    "B target"
// This special case is recognized in the method Compiler.analyse().
//
public static final Instruction BEQ = new Instruction(73, FLAGS_BRANCH_INSTRUCTION | FLAG_ENDS_BLOCK) {

@Override
public final String name() { return "BEQ"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBEQ(rs, rt, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("beq", rs, rt, (int)(short)imm16, address);
}
};
public static final Instruction BEQL = new Instruction(74, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BEQL"; }

@Override
public final String category() { return "MIPS II"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBEQL(rs, rt, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("beql", rs, rt, (int)(short)imm16, address);
}
};
public static final Instruction BGEZ = new Instruction(75, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BGEZ"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBGEZ(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgez", rs, (int)(short)imm16, address);
}
};
public static final Instruction BGEZAL = new Instruction(76, FLAGS_LINK_INSTRUCTION | FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BGEZAL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBGEZAL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgezal", rs, (int)(short)imm16, address);
}
};
public static final Instruction BGEZALL = new Instruction(77, FLAGS_LINK_INSTRUCTION | FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BGEZALL"; }

@Override
public final String category() { return "MIPS II"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBGEZALL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgezall", rs, (int)(short)imm16, address);
}
};
public static final Instruction BGEZL = new Instruction(78, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BGEZL"; }

@Override
public final String category() { return "MIPS II"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBGEZL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgezl", rs, (int)(short)imm16, address);
}
};
public static final Instruction BGTZ = new Instruction(79, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BGTZ"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBGTZ(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgtz", rs, (int)(short)imm16, address);
}
};
public static final Instruction BGTZL = new Instruction(80, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BGTZL"; }

@Override
public final String category() { return "MIPS II"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBGTZL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgtzl", rs, (int)(short)imm16, address);
}
};
public static final Instruction BLEZ = new Instruction(81, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BLEZ"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBLEZ(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("blez", rs, (int)(short)imm16, address);
}
};
public static final Instruction BLEZL = new Instruction(82, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BLEZL"; }

@Override
public final String category() { return "MIPS II"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBLEZL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("blezl", rs, (int)(short)imm16, address);
}
};
public static final Instruction BLTZ = new Instruction(83, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BLTZ"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBLTZ(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltz", rs, (int)(short)imm16, address);
}
};
public static final Instruction BLTZAL = new Instruction(84, FLAGS_LINK_INSTRUCTION | FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BLTZAL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBLTZAL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltzal", rs, (int)(short)imm16, address);
}
};
public static final Instruction BLTZALL = new Instruction(85, FLAGS_LINK_INSTRUCTION | FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BLTZALL"; }

@Override
public final String category() { return "MIPS II"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBLTZALL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltzall", rs, (int)(short)imm16, address);
}
};
public static final Instruction BLTZL = new Instruction(86, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BLTZL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBLTZL(rs, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltzl", rs, (int)(short)imm16, address);
}
};
public static final Instruction BNE = new Instruction(87, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BNE"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBNE(rs, rt, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("bne", rs, rt, (int)(short)imm16, address);
}
};
public static final Instruction BNEL = new Instruction(88, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BNEL"; }

@Override
public final String category() { return "MIPS II"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                if (processor.cpu.doBNEL(rs, rt, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("bnel", rs, rt, (int)(short)imm16, address);
}
};
public static final Instruction J = new Instruction(89, FLAG_HAS_DELAY_SLOT | FLAG_IS_JUMPING | FLAG_CANNOT_BE_SPLIT | FLAG_ENDS_BLOCK) {

@Override
public final String name() { return "J"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;


                if (processor.cpu.doJ(imm26))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm26 = (insn>>0)&67108863;

return Common.disasmJUMP("j", imm26, address);
}
};
public static final Instruction JAL = new Instruction(90, FLAGS_LINK_INSTRUCTION | FLAG_IS_JUMPING) {

@Override
public final String name() { return "JAL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;


                if (processor.cpu.doJAL(imm26))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm26 = (insn>>0)&67108863;

return Common.disasmJUMP("jal", imm26, address);
}
};
public static final Instruction JALR = new Instruction(91, FLAG_HAS_DELAY_SLOT) {

@Override
public final String name() { return "JALR"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


                if (processor.cpu.doJALR(rd, rs))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRS("jalr", rd, rs);
}
};
public static final Instruction JR = new Instruction(92, FLAG_HAS_DELAY_SLOT | FLAG_CANNOT_BE_SPLIT | FLAG_ENDS_BLOCK) {

@Override
public final String name() { return "JR"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;


                if (processor.cpu.doJR(rs))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int rs = (insn>>21)&31;

return Common.disasmRS("jr", rs);
}
};
public static final Instruction BC1F = new Instruction(93, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BC1F"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                if (processor.cpu.doBC1F((int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1f", (int)(short)imm16, address);
}
};
public static final Instruction BC1T = new Instruction(94, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BC1T"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                if (processor.cpu.doBC1T((int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1t", (int)(short)imm16, address);
}
};
public static final Instruction BC1FL = new Instruction(95, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BC1FL"; }

@Override
public final String category() { return "MIPS II/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                if (processor.cpu.doBC1FL((int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1fl", (int)(short)imm16, address);
}
};
public static final Instruction BC1TL = new Instruction(96, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BC1TL"; }

@Override
public final String category() { return "MIPS II/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                if (processor.cpu.doBC1TL((int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1tl", (int)(short)imm16, address);
}
};
public static final Instruction BVF = new Instruction(97, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BVF"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


                if (processor.cpu.doBVF(imm3, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

	return Common.disasmVCCOFFSET("bvf", imm3, imm16, address);
}
};
public static final Instruction BVT = new Instruction(98, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BVT"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


                if (processor.cpu.doBVT(imm3, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

	return Common.disasmVCCOFFSET("bvt", imm3, imm16, address);
}
};
public static final Instruction BVFL = new Instruction(99, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BVFL"; }

@Override
public final String category() { return "MIPS II/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


                if (processor.cpu.doBVFL(imm3, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

	return Common.disasmVCCOFFSET("bvfl", imm3, imm16, address);
}
};
public static final Instruction BVTL = new Instruction(100, FLAGS_BRANCH_INSTRUCTION) {

@Override
public final String name() { return "BVTL"; }

@Override
public final String category() { return "MIPS II/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


                if (processor.cpu.doBVTL(imm3, (int)(short)imm16))
                    processor.interpretDelayslot();
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

	return Common.disasmVCCOFFSET("bvtl", imm3, imm16, address);
}
};
public static final Instruction LB = new Instruction(101) {

@Override
public final String name() { return "LB"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLB(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		context.prepareRtForStore();
		context.memRead8(context.getRsRegisterIndex(), context.getImm16(true));
		context.getMethodVisitor().visitInsn(Opcodes.I2B);
		context.storeRt();
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lb", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LBU = new Instruction(102) {

@Override
public final String name() { return "LBU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLBU(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		context.prepareRtForStore();
		context.memRead8(context.getRsRegisterIndex(), context.getImm16(true));
		context.storeRt();
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lbu", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LH = new Instruction(103) {

@Override
public final String name() { return "LH"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLH(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		context.prepareRtForStore();
		context.memRead16(context.getRsRegisterIndex(), context.getImm16(true));
		context.getMethodVisitor().visitInsn(Opcodes.I2S);
		context.storeRt();
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lh", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LHU = new Instruction(104) {

@Override
public final String name() { return "LHU"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLHU(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		context.prepareRtForStore();
		context.memRead16(context.getRsRegisterIndex(), context.getImm16(true));
		context.storeRt();
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lhu", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LW = new Instruction(105) {

@Override
public final String name() { return "LW"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLW(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		context.prepareRtForStore();
		context.memRead32(context.getRsRegisterIndex(), context.getImm16(true));
		context.storeRt();
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lw", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LWL = new Instruction(106) {

@Override
public final String name() { return "LWL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLWL(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRTRSIMM("doLWL", true);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lwl", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LWR = new Instruction(107) {

@Override
public final String name() { return "LWR"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLWR(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRTRSIMM("doLWR", true);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lwr", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SB = new Instruction(108) {

@Override
public final String name() { return "SB"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSB(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int rs = context.getRsRegisterIndex();
	int simm16 = context.getImm16(true);
	context.prepareMemWrite8(rs, simm16);
	context.loadRt();
	context.memWrite8(rs, simm16);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sb", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SH = new Instruction(109) {

@Override
public final String name() { return "SH"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSH(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int rs = context.getRsRegisterIndex();
	int simm16 = context.getImm16(true);
	context.prepareMemWrite16(rs, simm16);
	context.loadRt();
	context.memWrite16(rs, simm16);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sh", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SW = new Instruction(110) {

@Override
public final String name() { return "SW"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSW(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int rs = context.getRsRegisterIndex();
	int simm16 = context.getImm16(true);
	context.prepareMemWrite32(rs, simm16);
	context.loadRt();
	context.memWrite32(rs, simm16);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sw", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SWL = new Instruction(111) {

@Override
public final String name() { return "SWL"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSWL(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRTRSIMM("doSWL", true);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("swl", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SWR = new Instruction(112) {

@Override
public final String name() { return "SWR"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSWR(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRTRSIMM("doSWR", true);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("swr", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LL = new Instruction(113) {

@Override
public final String name() { return "LL"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLL(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileRTRSIMM("doLL", true);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("ll", rt, rs, (int)(short)imm16);
}
};
public static final Instruction LWC1 = new Instruction(114) {

@Override
public final String name() { return "LWC1"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLWC1(ft, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFtForStore();
	context.memRead32(context.getRsRegisterIndex(), context.getImm16(true));
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "intBitsToFloat", "(I)F");
	context.storeFt();
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmFTIMMRS("lwc1", ft, rs, (int)(short)imm16);
}
};
public static final Instruction LVS = new Instruction(115) {

@Override
public final String name() { return "LVS"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLVS((vt5|(vt2<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vt2 = (insn>>0)&3;
	int vt5 = (insn>>16)&31;
	int vt = vt5 | (vt2<<5);
	int simm14 = context.getImm14(true);
	int rs = context.getRsRegisterIndex();

	context.prepareVtForStore(1, vt, 0);
	context.memRead32(rs, simm14);
	context.storeVtInt(1, vt, 0);
}
@Override
public String disasm(int address, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lv", 1, (vt5|(vt2<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction LVLQ = new Instruction(116) {

@Override
public final String name() { return "LVLQ"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLVLQ((vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lvl", 4, (vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction LVRQ = new Instruction(117) {

@Override
public final String name() { return "LVRQ"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLVRQ((vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lvr", 4, (vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction LVQ = new Instruction(118) {

@Override
public final String name() { return "LVQ"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doLVQ((vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vt1 = (insn>>0)&1;
	int vt5 = (insn>>16)&31;
	int vt = vt5 | (vt1<<5);
	int simm14 = context.getImm14(true);
	int rs = context.getRsRegisterIndex();
    final int vsize = 4;

    for (int n = 0; n < vsize; n++) {
    	context.prepareVtForStore(vsize, vt, n);
    	context.memRead32(rs, simm14 + n * 4);
    	context.storeVtInt(vsize, vt, n);
    }
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lv", 4, (vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction SC = new Instruction(119) {

@Override
public final String name() { return "SC"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSC(rt, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int rs = context.getRsRegisterIndex();
	int simm16 = context.getImm16(true);
	context.prepareMemWrite32(rs, simm16);
	context.loadRt();
	context.memWrite32(rs, simm16);

	if (!context.isRtRegister0()) {
		context.storeRt(1);
	}
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sc", rt, rs, (int)(short)imm16);
}
};
public static final Instruction SWC1 = new Instruction(120) {

@Override
public final String name() { return "SWC1"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSWC1(ft, rs, (int)(short)imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int rs = context.getRsRegisterIndex();
	int simm16 = context.getImm16(true);
	context.prepareMemWrite32(rs, simm16);
	context.loadFt();
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "floatToRawIntBits", "(F)I");
	context.memWrite32(rs, simm16);
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmFTIMMRS("swc1", ft, rs, (int)(short)imm16);
}
};
public static final Instruction SVS = new Instruction(121) {

@Override
public final String name() { return "SVS"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSVS((vt5|(vt2<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vt2 = (insn>>0)&3;
	int vt5 = (insn>>16)&31;
	int vt = vt5 | (vt2<<5);
	int simm14 = context.getImm14(true);
	int rs  = context.getRsRegisterIndex();
	context.prepareMemWrite32(rs, simm14);
	context.loadVtInt(1, vt, 0);
	context.memWrite32(rs, simm14);
}
@Override
public String disasm(int address, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("sv", 1, (vt5|(vt2<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction SVLQ = new Instruction(122) {

@Override
public final String name() { return "SVLQ"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSVLQ((vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("svl", 4, (vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction SVRQ = new Instruction(123) {

@Override
public final String name() { return "SVRQ"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSVRQ((vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("svr", 4, (vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction SVQ = new Instruction(124) {

@Override
public final String name() { return "SVQ"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSVQ((vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vt1 = (insn>>0)&1;
	int vt5 = (insn>>16)&31;
	int vt = vt5 | (vt1<<5);
	int simm14 = context.getImm14(true);
	int rs = context.getRsRegisterIndex();
    int vsize = 4;

    for (int n = 0; n < vsize; n++) {
    	context.prepareMemWrite32(rs, simm14 + n * 4);
    	context.loadVtInt(vsize, vt, n);
    	context.memWrite32(rs, simm14 + n * 4);
    }
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("sv", 4, (vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction VWB = new Instruction(125) {

@Override
public final String name() { return "VWB"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


				// Checked using VfpuTest: VWB.Q is equivalent to SV.Q
                processor.cpu.doSVQ((vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vt1 = (insn>>0)&1;
	int vt5 = (insn>>16)&31;
	int vt = vt5 | (vt1<<5);
	int simm14 = context.getImm14(true);
	int rs = context.getRsRegisterIndex();
    int vsize = 4;

    for (int n = 0; n < vsize; n++) {
    	context.prepareMemWrite32(rs, simm14 + n * 4);
    	context.loadVtInt(vsize, vt, n);
    	context.memWrite32(rs, simm14 + n * 4);
    }
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("vwb", 4, (vt5|(vt1<<5)), rs, (int)(short)(imm14 << 2));
}
};
public static final Instruction ADD_S = new Instruction(126) {

@Override
public final String name() { return "ADD.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                processor.cpu.doADDS(fd, fs, ft);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.loadFt();
	context.getMethodVisitor().visitInsn(Opcodes.FADD);
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("add.s", fd, fs, ft);
}
};
public static final Instruction SUB_S = new Instruction(127) {

@Override
public final String name() { return "SUB.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                processor.cpu.doSUBS(fd, fs, ft);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.loadFt();
	context.getMethodVisitor().visitInsn(Opcodes.FSUB);
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("sub.s", fd, fs, ft);
}
};
public static final Instruction MUL_S = new Instruction(128) {

@Override
public final String name() { return "MUL.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                processor.cpu.doMULS(fd, fs, ft);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.loadFt();
	context.getMethodVisitor().visitInsn(Opcodes.FMUL);
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("mul.s", fd, fs, ft);
}
};
public static final Instruction DIV_S = new Instruction(129) {

@Override
public final String name() { return "DIV.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                processor.cpu.doDIVS(fd, fs, ft);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.loadFt();
	context.getMethodVisitor().visitInsn(Opcodes.FDIV);
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("div.s", fd, fs, ft);
}
};
public static final Instruction SQRT_S = new Instruction(130) {

@Override
public final String name() { return "SQRT.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doSQRTS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
    context.getMethodVisitor().visitInsn(Opcodes.F2D);
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "sqrt", "(D)D");
    context.getMethodVisitor().visitInsn(Opcodes.D2F);
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("sqrt.s", fd, fs);
}
};
public static final Instruction ABS_S = new Instruction(131) {

@Override
public final String name() { return "ABS.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doABSS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "abs", "(F)F");
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("abs.s", fd, fs);
}
};
public static final Instruction MOV_S = new Instruction(132) {

@Override
public final String name() { return "MOV.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doMOVS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("mov.s", fd, fs);
}
};
public static final Instruction NEG_S = new Instruction(133) {

@Override
public final String name() { return "NEG.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doNEGS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.getMethodVisitor().visitInsn(Opcodes.FNEG);
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("neg.s", fd, fs);
}
};
public static final Instruction ROUND_W_S = new Instruction(134) {

@Override
public final String name() { return "ROUND.W.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doROUNDWS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "round", "(F)I");
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "intBitsToFloat", "(I)F");
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("round.w.s", fd, fs);
}
};
public static final Instruction TRUNC_W_S = new Instruction(135) {

@Override
public final String name() { return "TRUNC.W.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doTRUNCWS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.getMethodVisitor().visitInsn(Opcodes.F2I);
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "intBitsToFloat", "(I)F");
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("trunc.w.s", fd, fs);
}
};
public static final Instruction CEIL_W_S = new Instruction(136) {

@Override
public final String name() { return "CEIL.W.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doCEILWS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.getMethodVisitor().visitInsn(Opcodes.F2D);
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "ceil", "(D)D");
	context.getMethodVisitor().visitInsn(Opcodes.D2I);
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "intBitsToFloat", "(I)F");
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("ceil.w.s", fd, fs);
}
};
public static final Instruction FLOOR_W_S = new Instruction(137) {

@Override
public final String name() { return "FLOOR.W.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doFLOORWS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.getMethodVisitor().visitInsn(Opcodes.F2D);
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "floor", "(D)D");
	context.getMethodVisitor().visitInsn(Opcodes.D2I);
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "intBitsToFloat", "(I)F");
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("floor.w.s", fd, fs);
}
};
public static final Instruction CVT_S_W = new Instruction(138) {

@Override
public final String name() { return "CVT.S.W"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doCVTSW(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFdForStore();
	context.loadFs();
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "floatToRawIntBits", "(F)I");
	context.getMethodVisitor().visitInsn(Opcodes.I2F);
	context.storeFd();
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("cvt.s.w", fd, fs);
}
};
public static final Instruction CVT_W_S = new Instruction(139) {

@Override
public final String name() { return "CVT.W.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                processor.cpu.doCVTWS(fd, fs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("cvt.w.s", fd, fs);
}
};
public static final Instruction C_COND_S = new Instruction(140) {

@Override
public final String name() { return "C.COND.S"; }

@Override
public final String category() { return "FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int fcond = (insn>>0)&15;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                processor.cpu.doCCONDS(fs, ft, fcond);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int fcond = (insn >> 0) & 15;

	Label isNaN = new Label();
	Label isNotNaN = new Label();
	Label continueLabel = new Label();
	Label trueLabel = new Label();

	context.prepareFcr31cForStore();
	context.loadFt();
	context.storeFTmp2();
	context.loadFs();
	context.getMethodVisitor().visitInsn(Opcodes.DUP);
	context.storeFTmp1();
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "isNaN", "(F)Z");
	context.getMethodVisitor().visitJumpInsn(Opcodes.IFNE, isNaN);
	context.loadFTmp2();
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "isNaN", "(F)Z");
	context.getMethodVisitor().visitJumpInsn(Opcodes.IFEQ, isNotNaN);
	context.getMethodVisitor().visitLabel(isNaN);
	context.getMethodVisitor().visitInsn((fcond & 1) != 0 ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
	context.getMethodVisitor().visitJumpInsn(Opcodes.GOTO, continueLabel);
	context.getMethodVisitor().visitLabel(isNotNaN);
	boolean equal = (fcond & 2) != 0;
	boolean less  = (fcond & 4) != 0;
	if (!equal && !less) {
		context.getMethodVisitor().visitInsn(Opcodes.ICONST_0);
	} else {
		int testOpcode = (equal ? (less ? Opcodes.IFLE : Opcodes.IFEQ) : Opcodes.IFLT);
		context.loadFTmp1();
		context.loadFTmp2();
		context.getMethodVisitor().visitInsn(Opcodes.FCMPL);
		context.getMethodVisitor().visitJumpInsn(testOpcode, trueLabel);
		context.getMethodVisitor().visitInsn(Opcodes.ICONST_0);
		context.getMethodVisitor().visitJumpInsn(Opcodes.GOTO, continueLabel);
		context.getMethodVisitor().visitLabel(trueLabel);
		context.getMethodVisitor().visitInsn(Opcodes.ICONST_1);
	}
	context.getMethodVisitor().visitLabel(continueLabel);
	context.storeFcr31c();
}
@Override
public String disasm(int address, int insn) {
	int fcond = (insn>>0)&15;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmCcondS(fcond, fs, ft);
}
};
public static final Instruction MFC1 = new Instruction(141) {

@Override
public final String name() { return "MFC1"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int c1dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doMFC1(rt, c1dr);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		context.prepareRtForStore();
		context.loadFCr();
		context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "floatToRawIntBits", "(F)I");
		context.storeRt();
	}
}
@Override
public String disasm(int address, int insn) {
	int c1dr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFS("mfc1", rt, c1dr);
}
};
public static final Instruction CFC1 = new Instruction(142) {

@Override
public final String name() { return "CFC1"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doCFC1(rt, c1cr);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFC("cfc1", rt, c1cr);
}
};
public static final Instruction MTC1 = new Instruction(143) {

@Override
public final String name() { return "MTC1"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int c1dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doMTC1(rt, c1dr);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareFCrForStore();
	context.loadRt();
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "intBitsToFloat", "(I)F");
	context.storeFCr();
}
@Override
public String disasm(int address, int insn) {
	int c1dr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFS("mtc1", rt, c1dr);
}
};
public static final Instruction CTC1 = new Instruction(144) {

@Override
public final String name() { return "CTC1"; }

@Override
public final String category() { return "MIPS I/FPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doCTC1(rt, c1cr);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFC("ctc1", rt, c1cr);
}
};
public static final Instruction MFC0 = new Instruction(145) {

@Override
public final String name() { return "MFC0"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	//int c0dr = (insn>>11)&31;
	//int rt = (insn>>16)&31;


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	//int c0dr = (insn>>11)&31;
	//int rt = (insn>>16)&31;

return "Unimplemented MFC0";
}
};
public static final Instruction CFC0 = new Instruction(146) {

@Override
public final String name() { return "CFC0"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int c0cr = (insn>>11)&31;
	//int rt = (insn>>16)&31;


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	//int c0cr = (insn>>11)&31;
	//int rt = (insn>>16)&31;

return "Unimplemented CFC0";
}
};
public static final Instruction MTC0 = new Instruction(147) {

@Override
public final String name() { return "MTC0"; }

@Override
public final String category() { return "MIPS I"; }

@Override
public void interpret(Processor processor, int insn) {
	//int c0dr = (insn>>11)&31;
	//int rt = (insn>>16)&31;


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	//int c0dr = (insn>>11)&31;
	//int rt = (insn>>16)&31;

return "Unimplemented MTC0";
}
};
public static final Instruction CTC0 = new Instruction(148) {

@Override
public final String name() { return "CTC0"; }

@Override
public final String category() { return "ALLEGREX"; }

@Override
public void interpret(Processor processor, int insn) {
	//int c0cr = (insn>>11)&31;
	//int rt = (insn>>16)&31;


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	//int c0cr = (insn>>11)&31;
	//int rt = (insn>>16)&31;

return "Unimplemented CTC0";
}
};
public static final Instruction VADD = new Instruction(149, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VADD"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVADD(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.FADD, null);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vadd", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VSUB = new Instruction(150, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VSUB"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVSUB(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.FSUB, null);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vsub", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VSBN = new Instruction(151, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VSBN"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVSBN(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	if (vsize == 1) {
	    context.startPfxCompiled();
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(n);
			context.loadVs(n);
			context.loadVtInt(n);
			context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "scalb", "(FI)F");
			context.storeVd(n);
		}
		context.endPfxCompiled();
	} else {
		// Only VSBN.S is supported
		context.compileInterpreterInstruction();
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vsbn", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VDIV = new Instruction(152, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VDIV"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVDIV(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.FDIV, null);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vdiv", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VMUL = new Instruction(153, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VMUL"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVMUL(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.FMUL, null);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vmul", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VDOT = new Instruction(154, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VDOT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVDOT(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	if (vsize > 1) {
	    context.startPfxCompiled();
		context.prepareVdForStore(1, 0);
		context.loadVs(0);
		context.loadVt(0);
		context.getMethodVisitor().visitInsn(Opcodes.FMUL);
		for (int n = 1; n < vsize; n++) {
			context.loadVs(n);
			context.loadVt(n);
			context.getMethodVisitor().visitInsn(Opcodes.FMUL);
			context.getMethodVisitor().visitInsn(Opcodes.FADD);
		}
		context.storeVd(1, 0);
		context.endPfxCompiled(1);
	} else {
		// Unsupported VDOT.S
		context.compileInterpreterInstruction();
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVD1VSVT("vdot", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VSCL = new Instruction(155, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VSCL"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVSCL(1 + one + (two<<1), vd, vs, vt);               
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	if (vsize > 1) {
	    context.startPfxCompiled();
		context.loadVt(1, 0);
		context.storeFTmp1();
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(n);
			context.loadVs(n);
			context.loadFTmp1();
			context.getMethodVisitor().visitInsn(Opcodes.FMUL);
			context.storeVd(n);
		}
		context.endPfxCompiled();
	} else {
		// Unsupported VSCL.S
		context.compileInterpreterInstruction();
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT1("vscl", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VHDP = new Instruction(156, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VHDP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVHDP(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	if (vsize > 1) {
	    context.startPfxCompiled();
		context.prepareVdForStore(1, 0);
		context.loadVs(0);
		context.loadVt(0);
		context.getMethodVisitor().visitInsn(Opcodes.FMUL);
		for (int n = 1; n < vsize - 1; n++) {
			context.loadVs(n);
			context.loadVt(n);
			context.getMethodVisitor().visitInsn(Opcodes.FMUL);
			context.getMethodVisitor().visitInsn(Opcodes.FADD);
		}
		context.loadVt(vsize - 1);
		context.getMethodVisitor().visitInsn(Opcodes.FADD);
		context.storeVd(1, 0);
		context.endPfxCompiled();
	} else {
		// Unsupported VHDP.S
		context.compileInterpreterInstruction();
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVD1VSVT("vhdp", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VCRS = new Instruction(157, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VCRS"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVCRS(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vcrs", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VDET = new Instruction(158, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VDET"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVDET(1 + one + (two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVD1VSVT("vdet", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction MFV = new Instruction(159) {

@Override
public final String name() { return "MFV"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


                processor.cpu.doMFV(rt, imm7);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		context.prepareRtForStore();
		context.loadVdInt(1, 0);
		context.storeRt();
	}
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

	return Common.disasmVDRS("mfv", imm7, rt);
}
};
public static final Instruction MFVC = new Instruction(160) {

@Override
public final String name() { return "MFVC"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


                processor.cpu.doMFVC(rt, imm7);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	if (!context.isRtRegister0()) {
		int imm7 = context.getImm7();
		MethodVisitor mv = context.getMethodVisitor();
		switch (imm7) {
			case 3: {
				context.prepareRtForStore();
				context.loadVcrCc(5);
				for (int i = 4; i >= 0; i--) {
					context.loadImm(1);
					mv.visitInsn(Opcodes.ISHL);
					context.loadVcrCc(i);
					mv.visitInsn(Opcodes.IOR);
				}
				context.storeRt();
				break;
			}
			default:
				super.compile(context, insn);
				break;
		}
	}
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

return "Unimplemented MFVC imm7=" + imm7 + ", rt=" + rt;
}
};
public static final Instruction MTV = new Instruction(161) {

@Override
public final String name() { return "MTV"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


                processor.cpu.doMTV(rt, imm7);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.prepareVdForStore(1, 0);
	context.loadRt();
	context.storeVdInt(1, 0);
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

return Common.disasmVDRS("MTV", imm7, rt);
}
};
public static final Instruction MTVC = new Instruction(162) {

@Override
public final String name() { return "MTVC"; }

@Override
public final String category() { return "MIPS I/VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


                processor.cpu.doMTVC(rt, imm7);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

return "Unimplemented MTVC imm7=" + imm7 + ", rt=" + rt;
}
};
public static final Instruction VCMP = new Instruction(163, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT) {

@Override
public final String name() { return "VCMP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm4 = (insn>>0)&15;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVCMP(1+one+(two<<1), vs, vt, imm4);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int cond = context.getImm4();
	int vsize = context.getVsize();
	MethodVisitor mv = context.getMethodVisitor();
	boolean not = (cond & 4) != 0;
    context.startPfxCompiled();
	if ((cond & 8) == 0) {
		if ((cond & 3) == 0) {
			int value = not ? 1 : 0;
			for (int n = 0; n < vsize; n++) {
				context.prepareVcrCcForStore(n);
				context.loadImm(value);
				context.storeVcrCc(n);
			}
			context.prepareVcrCcForStore(4);
			context.loadImm(value);
			context.storeVcrCc(4);
			context.prepareVcrCcForStore(5);
			context.loadImm(value);
			context.storeVcrCc(5);
		} else {
			if (vsize > 1) {
				context.loadImm(0);
				context.storeTmp1();
				context.loadImm(1);
				context.storeTmp2();
			}
			for (int n = 0; n < vsize; n++) {
				context.prepareVcrCcForStore(n);
				context.loadVs(n);
				context.loadVt(n);
				mv.visitInsn(Opcodes.FCMPG);
				int opcodeCond = Opcodes.NOP;
				switch (cond & 3) {
					case 1:
						opcodeCond = not ? Opcodes.IFNE : Opcodes.IFEQ;
						break;
					case 2:
						opcodeCond = not ? Opcodes.IFGE : Opcodes.IFLT;
						break;
					case 3:
						opcodeCond = not ? Opcodes.IFGT : Opcodes.IFLE;
						break;
				}
				Label trueLabel = new Label();
				Label afterLabel = new Label();
				mv.visitJumpInsn(opcodeCond, trueLabel);
				context.loadImm(0);
				if (vsize > 1) {
					context.loadImm(0);
					context.storeTmp2();
				} else {
					context.prepareVcrCcForStore(4);
					context.loadImm(0);
					context.storeVcrCc(4);
					context.prepareVcrCcForStore(5);
					context.loadImm(0);
					context.storeVcrCc(5);
				}
				mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
				mv.visitLabel(trueLabel);
				context.loadImm(1);
				if (vsize > 1) {
					context.loadImm(1);
					context.storeTmp1();
				} else {
					context.prepareVcrCcForStore(4);
					context.loadImm(1);
					context.storeVcrCc(4);
					context.prepareVcrCcForStore(5);
					context.loadImm(1);
					context.storeVcrCc(5);
				}
				mv.visitLabel(afterLabel);
				context.storeVcrCc(n);
			}
			if (vsize > 1) {
				context.prepareVcrCcForStore(4);
				context.loadTmp1();
				context.storeVcrCc(4);
				context.prepareVcrCcForStore(5);
				context.loadTmp2();
				context.storeVcrCc(5);
			}
		}
	} else {
		if (vsize > 1) {
			context.loadImm(0);
			context.storeTmp1();
			context.loadImm(1);
			context.storeTmp2();
		}
		for (int n = 0; n < vsize; n++) {
			context.prepareVcrCcForStore(n);
			context.loadVs(n);
			boolean updateOrAnd = false;
			switch (cond & 3) {
				case 0: {
					mv.visitInsn(Opcodes.FCONST_0);
					mv.visitInsn(Opcodes.FCMPG);
					Label trueLabel = new Label();
					Label afterLabel = new Label();
					mv.visitJumpInsn(not ? Opcodes.IFNE : Opcodes.IFEQ, trueLabel);
					context.loadImm(0);
					if (vsize > 1) {
						context.loadImm(0);
						context.storeTmp2();
					} else {
						context.prepareVcrCcForStore(4);
						context.loadImm(0);
						context.storeVcrCc(4);
						context.prepareVcrCcForStore(5);
						context.loadImm(0);
						context.storeVcrCc(5);
					}
					mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
					mv.visitLabel(trueLabel);
					context.loadImm(1);
					if (vsize > 1) {
						context.loadImm(1);
						context.storeTmp1();
					} else {
						context.prepareVcrCcForStore(4);
						context.loadImm(1);
						context.storeVcrCc(4);
						context.prepareVcrCcForStore(5);
						context.loadImm(1);
						context.storeVcrCc(5);
					}
					mv.visitLabel(afterLabel);
					break;
				}
				case 1: {
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "isNaN", "(F)Z");
					updateOrAnd = true;
					break;
				}
				case 2: {
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "isInfinite", "(F)Z");
					updateOrAnd = true;
					break;
				}
				case 3: {
					mv.visitInsn(Opcodes.DUP);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "isNaN", "(F)Z");
					mv.visitInsn(Opcodes.SWAP);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "isInfinite", "(F)Z");
					mv.visitInsn(Opcodes.IOR);
					updateOrAnd = true;
					break;
				}
			}

			if (updateOrAnd) {
				if (not) {
					context.loadImm(1);
					mv.visitInsn(Opcodes.IXOR);
				}

				if (vsize > 1) {
					mv.visitInsn(Opcodes.DUP);
					context.loadTmp1();
					mv.visitInsn(Opcodes.IOR);
					context.storeTmp1();

					mv.visitInsn(Opcodes.DUP);
					context.loadTmp2();
					mv.visitInsn(Opcodes.IAND);
					context.storeTmp2();
				} else {
					mv.visitInsn(Opcodes.DUP);
					context.storeVcrCc(4);

					mv.visitInsn(Opcodes.DUP);
					context.storeVcrCc(5);
				}
			}

			context.storeVcrCc(n);
		}
		if (vsize > 1) {
			context.prepareVcrCcForStore(4);
			context.loadTmp1();
			context.storeVcrCc(4);
			context.prepareVcrCcForStore(5);
			context.loadTmp2();
			context.storeVcrCc(5);
		}
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int imm4 = (insn>>0)&15;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVCMP("vcmp", 1+one+(two<<1), imm4, vs, vt);
}
};
public static final Instruction VMIN = new Instruction(164, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VMIN"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVMIN(1+one+(two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.NOP, "min");
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vmin", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VMAX = new Instruction(165, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VMAX"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVMAX(1+one+(two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.NOP, "max");
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vmax", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VSCMP = new Instruction(166, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSCMP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVSCMP(1+one+(two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vscmp", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VSGE = new Instruction(167, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSGE"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVSGE(1+one+(two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	context.startPfxCompiled();
	MethodVisitor mv = context.getMethodVisitor();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.loadVs(n);
		context.loadVt(n);
		mv.visitInsn(Opcodes.FCMPG);
		Label trueLabel = new Label();
		Label afterLabel = new Label();
		mv.visitJumpInsn(Opcodes.IFGE, trueLabel);
		mv.visitInsn(Opcodes.FCONST_0);
		mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
		mv.visitLabel(trueLabel);
		mv.visitInsn(Opcodes.FCONST_1);
		mv.visitLabel(afterLabel);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vsge", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VSLT = new Instruction(168, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSLT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVSLT(1+one+(two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	context.startPfxCompiled();
	MethodVisitor mv = context.getMethodVisitor();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.loadVs(n);
		context.loadVt(n);
		mv.visitInsn(Opcodes.FCMPG);
		Label trueLabel = new Label();
		Label afterLabel = new Label();
		mv.visitJumpInsn(Opcodes.IFLT, trueLabel);
		mv.visitInsn(Opcodes.FCONST_0);
		mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
		mv.visitLabel(trueLabel);
		mv.visitInsn(Opcodes.FCONST_1);
		mv.visitLabel(afterLabel);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("vslt", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VMOV = new Instruction(169, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VMOV"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVMOV(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.NOP, null);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vmov", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VABS = new Instruction(170, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VABS"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVABS(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.NOP, "abs");
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vabs", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VNEG = new Instruction(171, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VNEG"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVNEG(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.FNEG, null);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vneg", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VIDT = new Instruction(172, FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VIDT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVIDT(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    context.startPfxCompiled();
	int vsize = context.getVsize();
	int id = context.getVdRegisterIndex() & 3;
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		float value = (id == n ? 1.0f : 0.0f);
		context.getMethodVisitor().visitLdcInsn(value);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vidt", 1+one+(two<<1), vd);
}
};
public static final Instruction VSAT0 = new Instruction(173, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSAT0"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSAT0(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	for (int n = 0; n < vsize; n++) {
		// float stackValue = vs[n];
		// if (stackValue <= 0.f) {
		//     stackValue = 0.f;
		// } else if (stackValue > 1.f) {
		//     stackValue = 1.f;
		// }
		// vd[n] = stackValue;
		context.prepareVdForStore(n);
		context.loadVs(n);
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(0.f);
		mv.visitInsn(Opcodes.FCMPG);
		Label limitLabel = new Label();
		Label afterLabel = new Label();
		mv.visitJumpInsn(Opcodes.IFGT, limitLabel);
		mv.visitInsn(Opcodes.POP);
		mv.visitLdcInsn(0.f);
		mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
		mv.visitLabel(limitLabel);
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(1.f);
		mv.visitInsn(Opcodes.FCMPG);
		mv.visitJumpInsn(Opcodes.IFLE, afterLabel);
		mv.visitInsn(Opcodes.POP);
		mv.visitLdcInsn(1.f);
		mv.visitLabel(afterLabel);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsat0", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSAT1 = new Instruction(174, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSAT1"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSAT1(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	for (int n = 0; n < vsize; n++) {
		// float stackValue = vs[n];
		// if (stackValue <= -1.f) {
		//     stackValue = -1.f;
		// } else if (stackValue > 1.f) {
		//     stackValue = 1.f;
		// }
		// vd[n] = stackValue;
		context.prepareVdForStore(n);
		context.loadVs(n);
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(-1.f);
		mv.visitInsn(Opcodes.FCMPG);
		Label limitLabel = new Label();
		Label afterLabel = new Label();
		mv.visitJumpInsn(Opcodes.IFGT, limitLabel);
		mv.visitInsn(Opcodes.POP);
		mv.visitLdcInsn(-1.f);
		mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
		mv.visitLabel(limitLabel);
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(1.f);
		mv.visitInsn(Opcodes.FCMPG);
		mv.visitJumpInsn(Opcodes.IFLE, afterLabel);
		mv.visitInsn(Opcodes.POP);
		mv.visitLdcInsn(1.f);
		mv.visitLabel(afterLabel);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsat1", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VZERO = new Instruction(175, FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VZERO"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVZERO(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    context.startPfxCompiled();
	int vsize = context.getVsize();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.getMethodVisitor().visitLdcInsn(0.0f);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vzero", 1+one+(two<<1), vd);
}
};
public static final Instruction VONE = new Instruction(176, FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VONE"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVONE(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    context.startPfxCompiled();
	int vsize = context.getVsize();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.getMethodVisitor().visitLdcInsn(1.0f);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vone", 1+one+(two<<1), vd);
}
};
public static final Instruction VRCP = new Instruction(177, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VRCP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVRCP(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(1.0f, Opcodes.FDIV, null);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vrcp", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VRSQ = new Instruction(178, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VRSQ"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVRSQ(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(1.0f, Opcodes.FDIV, "sqrt");
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vrsq", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSIN = new Instruction(179, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSIN"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSIN(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsin", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VCOS = new Instruction(180, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VCOS"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVCOS(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vcos", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VEXP2 = new Instruction(181, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VEXP2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVEXP2(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vexp2", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VLOG2 = new Instruction(182, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VLOG2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVLOG2(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vlog2", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSQRT = new Instruction(183, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VSQRT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSQRT(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.compileVFPUInstr(null, Opcodes.NOP, "sqrt");
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsqrt", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VASIN = new Instruction(184, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VASIN"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVASIN(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vasin", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VNRCP = new Instruction(185, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VNRCP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVNRCP(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vnrcp", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VNSIN = new Instruction(186, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VNSIN"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVNSIN(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vnsin", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VREXP2 = new Instruction(187, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VREXP2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVREXP2(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vrexp2", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VRNDS = new Instruction(188, FLAG_USE_VFPU_PFXS) {

@Override
public final String name() { return "VRNDS"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVRNDS(1+one+(two<<1), vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVS("vrnds", 1+one+(two<<1), vs);
}
};
public static final Instruction VRNDI = new Instruction(189, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VRNDI"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVRNDI(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vrndi", 1+one+(two<<1), vd);
}
};
public static final Instruction VRNDF1 = new Instruction(190, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VRNDF1"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVRNDF1(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vrndf1", 1+one+(two<<1), vd);
}
};
public static final Instruction VRNDF2 = new Instruction(191, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VRNDF2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVRNDF2(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vrndf2", 1+one+(two<<1), vd);
}
};
public static final Instruction VF2H = new Instruction(192, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VF2H"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVF2H(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vf2h", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VH2F = new Instruction(193, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VH2F"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVH2F(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vh2f", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSBZ = new Instruction(194) {

@Override
public final String name() { return "VSBZ"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSBZ(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("VSBZ", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VLGB = new Instruction(195) {

@Override
public final String name() { return "VLGB"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVLGB(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vlgb", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VUC2I = new Instruction(196, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VUC2I"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVUC2I(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vuc2i", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VC2I = new Instruction(197, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VC2I"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVC2I(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("VC2I", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VUS2I = new Instruction(198, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VUS2I"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVUS2I(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vus2i", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VS2I = new Instruction(199, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VS2I"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVS2I(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vs2i", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VI2UC = new Instruction(200, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VI2UC"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVI2UC(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = context.getVsize();
	if (vsize == 4) {
		MethodVisitor mv = context.getMethodVisitor();
		context.startPfxCompiled();
		context.prepareVdForStore(1, 0);
		for (int n = 0; n < vsize; n++) {
			context.loadVsInt(n);
			mv.visitInsn(Opcodes.DUP);
			Label afterLabel = new Label();
			Label negativeLabel = new Label();
			mv.visitJumpInsn(Opcodes.IFLT, negativeLabel);
			context.loadImm(23);
			mv.visitInsn(Opcodes.ISHR);
			if (n > 0) {
				context.loadImm(n * 8);
				mv.visitInsn(Opcodes.ISHL);
				mv.visitInsn(Opcodes.IOR);
			}
			mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
			mv.visitLabel(negativeLabel);
			mv.visitInsn(Opcodes.POP);
			if (n == 0) {
				context.loadImm(0);
			}
			mv.visitLabel(afterLabel);
		}
		context.storeVdInt(1, 0);
		context.endPfxCompiled(true);
	} else {
		// Only supported VI2UC.Q
		context.compileInterpreterInstruction();
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vi2uc", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VI2C = new Instruction(201, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VI2C"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVI2C(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vi2c", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VI2US = new Instruction(202, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VI2US"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVI2US(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vi2us", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VI2S = new Instruction(203, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VI2S"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVI2S(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vi2s", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSRT1 = new Instruction(204, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSRT1"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSRT1(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsrt1", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSRT2 = new Instruction(205, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSRT2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSRT2(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsrt2", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VBFY1 = new Instruction(206, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VBFY1"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVBFY1(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vbfy1", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VBFY2 = new Instruction(207, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VBFY2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVBFY2(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vbfy2", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VOCP = new Instruction(208, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VOCP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVOCP(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vocp", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSOCP = new Instruction(209, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSOCP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSOCP(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vsocp", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VFAD = new Instruction(210, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VFAD"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVFAD(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVD1VS("vfad", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VAVG = new Instruction(211, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VAVG"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVAVG(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVD1VS("vavg", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSRT3 = new Instruction(212, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSRT3"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSRT3(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsrt3", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VSGN = new Instruction(251, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

	@Override
	public final String name() { return "VSGN"; }

	@Override
	public final String category() { return "VFPU"; }

	@Override
	public void interpret(Processor processor, int insn) {
		int vd = (insn>>0)&127;
		int one = (insn>>7)&1;
		int vs = (insn>>8)&127;
		int two = (insn>>15)&1;


	                processor.cpu.doVSGN(1+one+(two<<1), vd, vs);
	            
	}
	@Override
	public void compile(ICompilerContext context, int insn) {
		super.compile(context, insn);
	}
	@Override
	public String disasm(int address, int insn) {
		int vd = (insn>>0)&127;
		int one = (insn>>7)&1;
		int vs = (insn>>8)&127;
		int two = (insn>>15)&1;

	return Common.disasmVDVS("vsgn", 1+one+(two<<1), vd, vs);
	}
	};
public static final Instruction VSRT4 = new Instruction(213, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VSRT4"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVSRT4(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDVS("vsrt4", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VMFVC = new Instruction(214, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VMFVC"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int imm7 = (insn>>8)&127;


                processor.cpu.doVMFVC(vd, imm7);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int imm7 = (insn>>8)&127;

return "Unimplemented VMFVC imm7=" + imm7 + ", vd=" + vd;
}
};
public static final Instruction VMTVC = new Instruction(215) {

@Override
public final String name() { return "VMTVC"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int vs = (insn>>8)&127;


                processor.cpu.doVMTVC(vs, imm7);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int vs = (insn>>8)&127;

return "Unimplemented VMTVC imm7=" + imm7 + ", vs=" + vs;
}
};
public static final Instruction VT4444 = new Instruction(216, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VT4444"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVT4444(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vt4444", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VT5551 = new Instruction(217, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VT5551"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVT5551(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vt5551", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VT5650 = new Instruction(218, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VT5650"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVT5650(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

	return Common.disasmVDVS("vt5650", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VCST = new Instruction(219, FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VCST"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


                processor.cpu.doVCST(1+one+(two<<1), vd, imm5);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int imm5 = (insn>>16)&31;

	float constant = 0.0f;
	if (imm5 < VfpuState.floatConstants.length) {
		constant = VfpuState.floatConstants[imm5];
	}

    context.startPfxCompiled();
	int vsize = context.getVsize();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.getMethodVisitor().visitLdcInsn(constant);
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

return Common.disasmVDCST("VCST", 1+one+(two<<1), vd, imm5);
}
};
public static final Instruction VF2IN = new Instruction(220, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VF2IN"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


                processor.cpu.doVF2IN(1+one+(two<<1), vd, vs, imm5);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	int imm5 = context.getImm5();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.loadVs(n);
		if (imm5 != 0) {
			context.loadImm(imm5);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "scalb", "(FI)F");
		}
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "round", "(F)I");
		context.storeVdInt(n);
	}
	context.endPfxCompiled(false);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

	return Common.disasmVDVSIMM("vf2in", 1+one+(two<<1), vd, vs, imm5);
}
};
public static final Instruction VF2IZ = new Instruction(221, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VF2IZ"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


                processor.cpu.doVF2IZ(1+one+(two<<1), vd, vs, imm5);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	int imm5 = context.getImm5();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.loadVs(n);
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(0.f);
		mv.visitInsn(Opcodes.FCMPG);
		Label negativeLabel = new Label();
		Label afterLabel = new Label();
		mv.visitJumpInsn(Opcodes.IFLT, negativeLabel);
		if (imm5 != 0) {
			context.loadImm(imm5);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "scalb", "(FI)F");
		}
		mv.visitInsn(Opcodes.F2D);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "floor", "(D)D");
		mv.visitInsn(Opcodes.D2I);
		mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
		mv.visitLabel(negativeLabel);
		if (imm5 != 0) {
			context.loadImm(imm5);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "scalb", "(FI)F");
		}
		mv.visitInsn(Opcodes.F2D);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "ceil", "(D)D");
		mv.visitInsn(Opcodes.D2I);
		mv.visitLabel(afterLabel);
		context.storeVdInt(n);
	}
	context.endPfxCompiled(false);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

	return Common.disasmVDVSIMM("vf2iz", 1+one+(two<<1), vd, vs, imm5);
}
};
public static final Instruction VF2IU = new Instruction(222, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VF2IU"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


                processor.cpu.doVF2IU(1+one+(two<<1), vd, vs, imm5);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	int imm5 = context.getImm5();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.loadVs(n);
		if (imm5 != 0) {
			context.loadImm(imm5);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "scalb", "(FI)F");
		}
		mv.visitInsn(Opcodes.F2D);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "ceil", "(D)D");
		mv.visitInsn(Opcodes.D2I);
		context.storeVdInt(n);
	}
	context.endPfxCompiled(false);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

	return Common.disasmVDVSIMM("vf2iu", 1+one+(two<<1), vd, vs, imm5);
}
};
public static final Instruction VF2ID = new Instruction(223, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VF2ID"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


                processor.cpu.doVF2ID(1+one+(two<<1), vd, vs, imm5);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	int imm5 = context.getImm5();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.loadVs(n);
		if (imm5 != 0) {
			context.loadImm(imm5);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "scalb", "(FI)F");
		}
		mv.visitInsn(Opcodes.F2D);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "floor", "(D)D");
		mv.visitInsn(Opcodes.D2I);
		context.storeVdInt(n);
	}
	context.endPfxCompiled(false);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

	return Common.disasmVDVSIMM("vf2id", 1+one+(two<<1), vd, vs, imm5);
}
};
public static final Instruction VI2F = new Instruction(224, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VI2F"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


                processor.cpu.doVI2F(1+one+(two<<1), vd, vs, imm5);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	int imm5 = context.getImm5();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		context.loadVsInt(n);
		mv.visitInsn(Opcodes.I2F);
		if (imm5 != 0) {
			context.loadImm(-imm5);
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "scalb", "(FI)F");
		}
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

	return Common.disasmVDVSIMM("vi2f", 1+one+(two<<1), vd, vs, imm5);
}
};
public static final Instruction VCMOVT = new Instruction(225, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VCMOVT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;


                processor.cpu.doVCMOVT(1+one+(two<<1), imm3, vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	int imm3 = context.getImm3();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	if (imm3 < 6) {
		Label notMoveLabel = new Label();
		Label afterLabel = new Label();
		context.loadVcrCc(imm3);
		mv.visitJumpInsn(Opcodes.IFEQ, notMoveLabel);
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(n);
			context.loadVsInt(n);
			context.storeVdInt(n);
		}
		mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
		mv.visitLabel(notMoveLabel);
		if (context.getPfxdState().isKnown() && context.getPfxdState().pfxDst.enabled) {
			for (int n = 0; n < vsize; n++) {
				context.prepareVdForStore(n);
				context.loadVdInt(n);
				context.storeVdInt(n);
			}
		}
		mv.visitLabel(afterLabel);
	} else if (imm3 == 6) {
		for (int n = 0; n < vsize; n++) {
			Label notMoveLabel = new Label();
			Label afterLabel = new Label();
			context.loadVcrCc(n);
			mv.visitJumpInsn(Opcodes.IFEQ, notMoveLabel);
			context.prepareVdForStore(n);
			context.loadVsInt(n);
			context.storeVdInt(n);
			mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
			mv.visitLabel(notMoveLabel);
			if (context.getPfxdState().isKnown() && context.getPfxdState().pfxDst.enabled) {
				context.prepareVdForStore(n);
				context.loadVdInt(n);
				context.storeVd(n);
			}
			mv.visitLabel(afterLabel);
		}
	} else {
		// Never copy
	}
	context.endPfxCompiled(false);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;

	return Common.disasmVDVSIMM("VCMOVT", 1+one+(two<<1), vd, vs, imm3);
}
};
public static final Instruction VCMOVF = new Instruction(226, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VCMOVF"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;


                processor.cpu.doVCMOVF(1+one+(two<<1), imm3, vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	int imm3 = context.getImm3();
	MethodVisitor mv = context.getMethodVisitor();
	context.startPfxCompiled();
	if (imm3 < 6) {
		Label notMoveLabel = new Label();
		Label afterLabel = new Label();
		context.loadVcrCc(imm3);
		mv.visitJumpInsn(Opcodes.IFNE, notMoveLabel);
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(n);
			context.loadVsInt(n);
			context.storeVdInt(n);
		}
		mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
		mv.visitLabel(notMoveLabel);
		if (context.getPfxdState().isKnown() && context.getPfxdState().pfxDst.enabled) {
			for (int n = 0; n < vsize; n++) {
				context.prepareVdForStore(n);
				context.loadVdInt(n);
				context.storeVdInt(n);
			}
		}
		mv.visitLabel(afterLabel);
	} else if (imm3 == 6) {
		for (int n = 0; n < vsize; n++) {
			Label notMoveLabel = new Label();
			Label afterLabel = new Label();
			context.loadVcrCc(n);
			mv.visitJumpInsn(Opcodes.IFNE, notMoveLabel);
			context.prepareVdForStore(n);
			context.loadVsInt(n);
			context.storeVdInt(n);
			mv.visitJumpInsn(Opcodes.GOTO, afterLabel);
			mv.visitLabel(notMoveLabel);
			if (context.getPfxdState().isKnown() && context.getPfxdState().pfxDst.enabled) {
				context.prepareVdForStore(n);
				context.loadVdInt(n);
				context.storeVd(n);
			}
			mv.visitLabel(afterLabel);
		}
	} else {
		// Always copy
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(n);
			context.loadVsInt(n);
			context.storeVdInt(n);
		}
	}
	context.endPfxCompiled(false);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;

	return Common.disasmVDVSIMM("VCMOVF", 1+one+(two<<1), vd, vs, imm3);
}
};
public static final Instruction VWBN = new Instruction(227) {

@Override
public final String name() { return "VWBN"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm8 = (insn>>16)&255;


                processor.cpu.doVWBN(1+one+(two<<1), vd, vs, imm8);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm8 = (insn>>16)&255;

	return Common.disasmVDVSIMM("VWBN", 1+one+(two<<1), vd, vs, imm8);
}
};
public static final Instruction VPFXS = new Instruction(228) {

@Override
public final String name() { return "VPFXS"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int swzx = (insn>>0)&3;
	int swzy = (insn>>2)&3;
	int swzz = (insn>>4)&3;
	int swzw = (insn>>6)&3;
	int absx = (insn>>8)&1;
	int absy = (insn>>9)&1;
	int absz = (insn>>10)&1;
	int absw = (insn>>11)&1;
	int cstx = (insn>>12)&1;
	int csty = (insn>>13)&1;
	int cstz = (insn>>14)&1;
	int cstw = (insn>>15)&1;
	int negx = (insn>>16)&1;
	int negy = (insn>>17)&1;
	int negz = (insn>>18)&1;
	int negw = (insn>>19)&1;


                processor.cpu.doVPFXS(
                    negw, negz, negy, negx,
                    cstw, cstz, csty, cstx, 
                    absw, absz, absy, absx, 
                    swzw, swzz, swzy, swzx
                );
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    if (context.isPfxConsumed(FLAG_USE_VFPU_PFXS)) {
    	context.getPfxsState().setKnown(true);
    	PfxSrc pfxSrc = context.getPfxsState().pfxSrc;
    	pfxSrc.swz[0] = (insn>>0)&3;
    	pfxSrc.swz[1] = (insn>>2)&3;
    	pfxSrc.swz[2] = (insn>>4)&3;
    	pfxSrc.swz[3] = (insn>>6)&3;
    	pfxSrc.abs[0] = ((insn>>8)&1) != 0;
    	pfxSrc.abs[1] = ((insn>>9)&1) != 0;
    	pfxSrc.abs[2] = ((insn>>10)&1) != 0;
    	pfxSrc.abs[3] = ((insn>>11)&1) != 0;
    	pfxSrc.cst[0] = ((insn>>12)&1) != 0;
    	pfxSrc.cst[1] = ((insn>>13)&1) != 0;
    	pfxSrc.cst[2] = ((insn>>14)&1) != 0;
    	pfxSrc.cst[3] = ((insn>>15)&1) != 0;
    	pfxSrc.neg[0] = ((insn>>16)&1) != 0;
    	pfxSrc.neg[1] = ((insn>>17)&1) != 0;
    	pfxSrc.neg[2] = ((insn>>18)&1) != 0;
    	pfxSrc.neg[3] = ((insn>>19)&1) != 0;
    	pfxSrc.enabled = true;
    } else {
        context.getPfxsState().setKnown(false);
        context.compileInterpreterInstruction();
    }
}
@Override
public String disasm(int address, int insn) {
	int swzx = (insn>>0)&3;
	int swzy = (insn>>2)&3;
	int swzz = (insn>>4)&3;
	int swzw = (insn>>6)&3;
	int absx = (insn>>8)&1;
	int absy = (insn>>9)&1;
	int absz = (insn>>10)&1;
	int absw = (insn>>11)&1;
	int cstx = (insn>>12)&1;
	int csty = (insn>>13)&1;
	int cstz = (insn>>14)&1;
	int cstw = (insn>>15)&1;
	int negx = (insn>>16)&1;
	int negy = (insn>>17)&1;
	int negz = (insn>>18)&1;
	int negw = (insn>>19)&1;
	
	int[] swz = new int[4];
	boolean[] abs, cst, neg;
	abs = new boolean[4];
	cst = new boolean[4];
	neg = new boolean[4];
	
	swz[0] = swzx;
    swz[1] = swzy;
    swz[2] = swzz;
    swz[3] = swzw;
    abs[0] = absx != 0;
    abs[1] = absy != 0;
    abs[2] = absz != 0;
    abs[3] = absw != 0;
    cst[0] = cstx != 0;
    cst[1] = csty != 0;
    cst[2] = cstz != 0;
    cst[3] = cstw != 0;
    neg[0] = negx != 0;
    neg[1] = negy != 0;
    neg[2] = negz != 0;
    neg[3] = negw != 0;

return Common.disasmVPFX("VPFXS", swz, abs, cst, neg);
}
};
public static final Instruction VPFXT = new Instruction(229) {

@Override
public final String name() { return "VPFXT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int swzx = (insn>>0)&3;
	int swzy = (insn>>2)&3;
	int swzz = (insn>>4)&3;
	int swzw = (insn>>6)&3;
	int absx = (insn>>8)&1;
	int absy = (insn>>9)&1;
	int absz = (insn>>10)&1;
	int absw = (insn>>11)&1;
	int cstx = (insn>>12)&1;
	int csty = (insn>>13)&1;
	int cstz = (insn>>14)&1;
	int cstw = (insn>>15)&1;
	int negx = (insn>>16)&1;
	int negy = (insn>>17)&1;
	int negz = (insn>>18)&1;
	int negw = (insn>>19)&1;


                processor.cpu.doVPFXT(
                    negw, negz, negy, negx,
                    cstw, cstz, csty, cstx, 
                    absw, absz, absy, absx, 
                    swzw, swzz, swzy, swzx
                );
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    if (context.isPfxConsumed(FLAG_USE_VFPU_PFXT)) {
    	context.getPfxtState().setKnown(true);
    	PfxSrc pfxSrc = context.getPfxtState().pfxSrc;
    	pfxSrc.swz[0] = (insn>>0)&3;
    	pfxSrc.swz[1] = (insn>>2)&3;
    	pfxSrc.swz[2] = (insn>>4)&3;
    	pfxSrc.swz[3] = (insn>>6)&3;
    	pfxSrc.abs[0] = ((insn>>8)&1) != 0;
    	pfxSrc.abs[1] = ((insn>>9)&1) != 0;
    	pfxSrc.abs[2] = ((insn>>10)&1) != 0;
    	pfxSrc.abs[3] = ((insn>>11)&1) != 0;
    	pfxSrc.cst[0] = ((insn>>12)&1) != 0;
    	pfxSrc.cst[1] = ((insn>>13)&1) != 0;
    	pfxSrc.cst[2] = ((insn>>14)&1) != 0;
    	pfxSrc.cst[3] = ((insn>>15)&1) != 0;
    	pfxSrc.neg[0] = ((insn>>16)&1) != 0;
    	pfxSrc.neg[1] = ((insn>>17)&1) != 0;
    	pfxSrc.neg[2] = ((insn>>18)&1) != 0;
    	pfxSrc.neg[3] = ((insn>>19)&1) != 0;
    	pfxSrc.enabled = true;
    } else {
        context.getPfxtState().setKnown(false);
        context.compileInterpreterInstruction();
    }
}
@Override
public String disasm(int address, int insn) {
	int swzx = (insn>>0)&3;
	int swzy = (insn>>2)&3;
	int swzz = (insn>>4)&3;
	int swzw = (insn>>6)&3;
	int absx = (insn>>8)&1;
	int absy = (insn>>9)&1;
	int absz = (insn>>10)&1;
	int absw = (insn>>11)&1;
	int cstx = (insn>>12)&1;
	int csty = (insn>>13)&1;
	int cstz = (insn>>14)&1;
	int cstw = (insn>>15)&1;
	int negx = (insn>>16)&1;
	int negy = (insn>>17)&1;
	int negz = (insn>>18)&1;
	int negw = (insn>>19)&1;

	int[] swz = new int[4];
	boolean[] abs, cst, neg;
	abs = new boolean[4];
	cst = new boolean[4];
	neg = new boolean[4];
	
	swz[0] = swzx;
    swz[1] = swzy;
    swz[2] = swzz;
    swz[3] = swzw;
    abs[0] = absx != 0;
    abs[1] = absy != 0;
    abs[2] = absz != 0;
    abs[3] = absw != 0;
    cst[0] = cstx != 0;
    cst[1] = csty != 0;
    cst[2] = cstz != 0;
    cst[3] = cstw != 0;
    neg[0] = negx != 0;
    neg[1] = negy != 0;
    neg[2] = negz != 0;
    neg[3] = negw != 0;

return Common.disasmVPFX("VPFXT", swz, abs, cst, neg);
}
};
public static final Instruction VPFXD = new Instruction(230) {

@Override
public final String name() { return "VPFXD"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int satx = (insn>>0)&3;
	int saty = (insn>>2)&3;
	int satz = (insn>>4)&3;
	int satw = (insn>>6)&3;
	int mskx = (insn>>8)&1;
	int msky = (insn>>9)&1;
	int mskz = (insn>>10)&1;
	int mskw = (insn>>11)&1;


                processor.cpu.doVPFXD(
                    mskw, mskz, msky, mskx,
                    satw, satz, saty, satx
                );
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    if (context.isPfxConsumed(FLAG_USE_VFPU_PFXD)) {
    	context.getPfxdState().setKnown(true);
    	PfxDst pfxDst = context.getPfxdState().pfxDst;
    	pfxDst.sat[0] = (insn>>0)&3;
    	pfxDst.sat[1] = (insn>>2)&3;
    	pfxDst.sat[2] = (insn>>4)&3;
    	pfxDst.sat[3] = (insn>>6)&3;
    	pfxDst.msk[0] = ((insn>>8)&1) != 0;
    	pfxDst.msk[1] = ((insn>>9)&1) != 0;
    	pfxDst.msk[2] = ((insn>>10)&1) != 0;
    	pfxDst.msk[3] = ((insn>>11)&1) != 0;
    	pfxDst.enabled = true;
    } else {
        context.getPfxdState().setKnown(false);
        context.compileInterpreterInstruction();
    }
}
@Override
public String disasm(int address, int insn) {
	int satx = (insn>>0)&3;
	int saty = (insn>>2)&3;
	int satz = (insn>>4)&3;
	int satw = (insn>>6)&3;
	int mskx = (insn>>8)&1;
	int msky = (insn>>9)&1;
	int mskz = (insn>>10)&1;
	int mskw = (insn>>11)&1;
	
	int[] sat, msk;
	sat = new int[4];
	msk = new int[4];
	sat[0] = satx;
	sat[1] = saty;
	sat[2] = satz;
	sat[3] = satw;
	msk[0] = mskx;
	msk[1] = msky;
	msk[2] = mskz;
	msk[3] = mskw;

return Common.disasmVPFXD("VPFXD", sat, msk);
}
};
public static final Instruction VIIM = new Instruction(231, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VIIM"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;


                processor.cpu.doVIIM(vd, imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = 1;
	final int vd = context.getVtRegisterIndex(); // vd index stored as vt
	int imm16 = context.getImm16(false);
	context.startPfxCompiled();
	context.prepareVdForStore(vsize, vd, 0);
	context.getMethodVisitor().visitLdcInsn((float) imm16);
	context.storeVd(vsize, vd, 0);
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;

return Common.disasmVDIIM("VIIM", 1, vd, imm16);
}
};
public static final Instruction VFIM = new Instruction(232, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VFIM"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;


                processor.cpu.doVFIM(vd, imm16);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = 1;
	final int vd = context.getVtRegisterIndex(); // vd index stored as vt
	final int imm16 = context.getImm16(false);
	float value = VfpuState.halffloatToFloat(imm16);
	context.startPfxCompiled();
	context.prepareVdForStore(vsize, vd, 0);
	context.getMethodVisitor().visitLdcInsn(value);
	context.storeVd(vsize, vd, 0);
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;

return Common.disasmVDFIM("VFIM", 1, vd, imm16);
}
};
public static final Instruction VMMUL = new Instruction(233, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VMMUL"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVMMUL(1+one+(two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	int vsize = context.getVsize();
	if (vsize > 1) {
	    context.startPfxCompiled();
		int vs = context.getVsRegisterIndex();
		int vt = context.getVtRegisterIndex();
		int vd = context.getVdRegisterIndex();
		for (int i = 0; i < vsize; i++) {
			for (int j = 0; j < vsize; j++) {
				context.prepareVdForStore(vsize, vd + i, j);
				context.loadVs(vsize, vs + j, 0);
				context.loadVt(vsize, vt + i, 0);
				context.getMethodVisitor().visitInsn(Opcodes.FMUL);
				for (int n = 1; n < vsize; n++) {
					context.loadVs(vsize, vs + j, n);
					context.loadVt(vsize, vt + i, n);
					context.getMethodVisitor().visitInsn(Opcodes.FMUL);
					context.getMethodVisitor().visitInsn(Opcodes.FADD);
				}
				context.storeVd(vsize, vd + i, j);
			}
		}
		context.endPfxCompiled();
	} else {
		// Unsupported VMMUL.S
		context.compileInterpreterInstruction();
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return Common.disasmVDMVSMVTM("VMMUL", 1+one+(two<<1), vd, vs ^ 32, vt);
}
};
public static final Instruction VHTFM2 = new Instruction(234, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VHTFM2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVHTFM2(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

	return Common.disasmVDVSVT("VHTFM2", 2, vd, vs, vt);
}
};
public static final Instruction VTFM2 = new Instruction(235, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VTFM2"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVTFM2(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

	return Common.disasmVDVSVT("VTFM2", 2, vd, vs, vt);
}
};
public static final Instruction VHTFM3 = new Instruction(236, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VHTFM3"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVHTFM3(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

	return Common.disasmVDVSVT("VHTFM3", 3, vd, vs, vt);
}
};
public static final Instruction VTFM3 = new Instruction(237, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VTFM3"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVTFM3(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = 3;
	final int vs = context.getVsRegisterIndex();
	MethodVisitor mv = context.getMethodVisitor();
	context.loadVt(vsize, 0);
	context.storeFTmp1();
	context.loadVt(vsize, 1);
	context.storeFTmp2();
	context.loadVt(vsize, 2);
	context.storeFTmp3();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(vsize, n);
		context.loadVs(vsize, vs + n, 0);
		context.loadFTmp1();
		mv.visitInsn(Opcodes.FMUL);
		context.loadVs(vsize, vs + n, 1);
		context.loadFTmp2();
		mv.visitInsn(Opcodes.FMUL);
		mv.visitInsn(Opcodes.FADD);
		context.loadVs(vsize, vs + n, 2);
		context.loadFTmp3();
		mv.visitInsn(Opcodes.FMUL);
		mv.visitInsn(Opcodes.FADD);
		context.storeVd(vsize, n);
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

	return Common.disasmVDVSVT("VTFM3", 3, vd, vs, vt);
}
};
public static final Instruction VHTFM4 = new Instruction(238, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VHTFM4"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVHTFM4(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = 4;
	final int vs = context.getVsRegisterIndex();
	MethodVisitor mv = context.getMethodVisitor();
	context.loadVt(3, 0);
	context.storeFTmp1();
	context.loadVt(3, 1);
	context.storeFTmp2();
	context.loadVt(3, 2);
	context.storeFTmp3();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(vsize, n);
		context.loadVs(vsize, vs + n, 0);
		context.loadFTmp1();
		mv.visitInsn(Opcodes.FMUL);
		context.loadVs(vsize, vs + n, 1);
		context.loadFTmp2();
		mv.visitInsn(Opcodes.FMUL);
		mv.visitInsn(Opcodes.FADD);
		context.loadVs(vsize, vs + n, 2);
		context.loadFTmp3();
		mv.visitInsn(Opcodes.FMUL);
		mv.visitInsn(Opcodes.FADD);
		context.loadVs(vsize, vs + n, 3);
		mv.visitInsn(Opcodes.FADD);
		context.storeVd(vsize, n);
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

	return Common.disasmVDVSVT("VHTFM4", 4, vd, vs, vt);
}
};
public static final Instruction VTFM4 = new Instruction(239, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VTFM4"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVTFM4(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = 4;
	final int vs = context.getVsRegisterIndex();
	MethodVisitor mv = context.getMethodVisitor();
	context.loadVt(vsize, 0);
	context.storeFTmp1();
	context.loadVt(vsize, 1);
	context.storeFTmp2();
	context.loadVt(vsize, 2);
	context.storeFTmp3();
	context.loadVt(vsize, 3);
	context.storeFTmp4();
	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(vsize, n);
		context.loadVs(vsize, vs + n, 0);
		context.loadFTmp1();
		mv.visitInsn(Opcodes.FMUL);
		context.loadVs(vsize, vs + n, 1);
		context.loadFTmp2();
		mv.visitInsn(Opcodes.FMUL);
		mv.visitInsn(Opcodes.FADD);
		context.loadVs(vsize, vs + n, 2);
		context.loadFTmp3();
		mv.visitInsn(Opcodes.FMUL);
		mv.visitInsn(Opcodes.FADD);
		context.loadVs(vsize, vs + n, 3);
		context.loadFTmp4();
		mv.visitInsn(Opcodes.FMUL);
		mv.visitInsn(Opcodes.FADD);
		context.storeVd(vsize, n);
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

	return Common.disasmVDVSVT("VTFM4", 4, vd, vs, vt);
}
};
public static final Instruction VMSCL = new Instruction(240, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VMSCL"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                processor.cpu.doVMSCL(1+one+(two<<1), vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = context.getVsize();
	if (vsize > 1) {
		final int vs = context.getVsRegisterIndex();
		final int vd = context.getVdRegisterIndex();
	    context.startPfxCompiled();
		context.loadVt(1, 0);
		context.storeFTmp1();
		for (int i = 0; i < vsize; i++) {
			for (int n = 0; n < vsize; n++) {
				context.prepareVdForStore(vsize, vd + i, n);
				context.loadVs(vsize, vs + i, n);
				context.loadFTmp1();
				context.getMethodVisitor().visitInsn(Opcodes.FMUL);
				context.storeVd(vsize, vd + i, n);
			}
		}
		context.endPfxCompiled();
	} else {
		// Unsupported VMSCL.S
		context.compileInterpreterInstruction();
	}
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

	return Common.disasmVDVSVT("vmscl", 1+one+(two<<1), vd, vs, vt);
}
};
public static final Instruction VCRSP = new Instruction(241, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VCRSP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVCRSP(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	context.startPfxCompiled();
	final int vsize = 3;
	MethodVisitor mv = context.getMethodVisitor();

	// v3[0] = +v1[1] * v2[2] - v1[2] * v2[1];
	context.prepareVdForStore(vsize, 0);
	context.loadVs(vsize, 1);
	context.loadVt(vsize, 2);
	mv.visitInsn(Opcodes.FMUL);
	context.loadVs(vsize, 2);
	context.loadVt(vsize, 1);
	mv.visitInsn(Opcodes.FMUL);
	mv.visitInsn(Opcodes.FSUB);
	context.storeVd(vsize, 0);

	// v3[1] = +v1[2] * v2[0] - v1[0] * v2[2];
	context.prepareVdForStore(vsize, 1);
	context.loadVs(vsize, 2);
	context.loadVt(vsize, 0);
	mv.visitInsn(Opcodes.FMUL);
	context.loadVs(vsize, 0);
	context.loadVt(vsize, 2);
	mv.visitInsn(Opcodes.FMUL);
	mv.visitInsn(Opcodes.FSUB);
	context.storeVd(vsize, 1);

	// v3[2] = +v1[0] * v2[1] - v1[1] * v2[0];
	context.prepareVdForStore(vsize, 2);
	context.loadVs(vsize, 0);
	context.loadVt(vsize, 1);
	mv.visitInsn(Opcodes.FMUL);
	context.loadVs(vsize, 1);
	context.loadVt(vsize, 0);
	mv.visitInsn(Opcodes.FMUL);
	mv.visitInsn(Opcodes.FSUB);
	context.storeVd(vsize, 2);

	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("VCRSP", 3, vd, vs, vt);
}
};
public static final Instruction VQMUL = new Instruction(242, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXT | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VQMUL"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


                processor.cpu.doVQMUL(vd, vs, vt);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	super.compile(context, insn);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("VQMUL", 4, vd, vs, vt);
}
};
public static final Instruction VMMOV = new Instruction(243, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VMMOV"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                processor.cpu.doVMMOV(1+one+(two<<1), vd, vs);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
	final int vsize = context.getVsize();
	final int vd = context.getVdRegisterIndex();
	final int vs = context.getVsRegisterIndex();
	context.startPfxCompiled();
	for (int i = 0; i < vsize; i++) {
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(vsize, vd + i, n);
			context.loadVsInt(vsize, vs + i, n);
			context.storeVdInt(vsize, vd + i, n);
		}
	}
	context.endPfxCompiled(false);
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVDMVSM("VMMOV", 1+one+(two<<1), vd, vs);
}
};
public static final Instruction VMIDT = new Instruction(244, FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VMIDT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVMIDT(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    context.startPfxCompiled();
	int vsize = context.getVsize();
	int vd = context.getVdRegisterIndex();
	for (int i = 0; i < vsize; i++) {
		int id = (vd + i) & 3;
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(vsize, vd + i, n);
			float value = (id == n ? 1.0f : 0.0f);
			context.getMethodVisitor().visitLdcInsn(value);
			context.storeVd(vsize, vd + i, n);
		}
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

	return Common.disasmVDM("VMIDT", 1+one+(two<<1), vd);
}
};
public static final Instruction VMZERO = new Instruction(245, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VMZERO"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVMZERO(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    context.startPfxCompiled();
	int vsize = context.getVsize();
	int vd = context.getVdRegisterIndex();
	for (int i = 0; i < vsize; i++) {
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(vsize, vd + i, n);
			context.getMethodVisitor().visitLdcInsn(0.0f);
			context.storeVd(vsize, vd + i, n);
		}
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVDM("VMZERO", 1+one+(two<<1), vd);
}
};
public static final Instruction VMONE = new Instruction(246, FLAG_USE_VFPU_PFXD) {

@Override
public final String name() { return "VMONE"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                processor.cpu.doVMONE(1+one+(two<<1), vd);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    context.startPfxCompiled();
	int vsize = context.getVsize();
	int vd = context.getVdRegisterIndex();
	for (int i = 0; i < vsize; i++) {
		for (int n = 0; n < vsize; n++) {
			context.prepareVdForStore(vsize, vd + i, n);
			context.getMethodVisitor().visitLdcInsn(1.0f);
			context.storeVd(vsize, vd + i, n);
		}
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVDM("VMONE", 1+one+(two<<1), vd);
}
};
public static final Instruction VROT = new Instruction(247, FLAG_USE_VFPU_PFXS | FLAG_USE_VFPU_PFXD | FLAG_COMPILED_PFX) {

@Override
public final String name() { return "VROT"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


                processor.cpu.doVROT(1+one+(two<<1), vd, vs, imm5);
            
}
@Override
public void compile(ICompilerContext context, int insn) {
    context.startPfxCompiled();
	int vsize = context.getVsize();
	int imm5 = context.getImm5();
    int si = (imm5 >>> 2) & 3;
    int ci = (imm5 >>> 0) & 3;

    // Compute angle
    context.loadVs(1, 0);
    context.getMethodVisitor().visitInsn(Opcodes.F2D);
    context.getMethodVisitor().visitLdcInsn(Math.PI * 0.5);
    context.getMethodVisitor().visitInsn(Opcodes.DMUL);

    // Compute cos(angle)
    context.getMethodVisitor().visitInsn(Opcodes.DUP2);
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "cos", "(D)D");
    context.getMethodVisitor().visitInsn(Opcodes.D2F);
    context.storeFTmp1();

    // Compute sin(angle)
    if ((imm5 & 16) != 0) {
    	context.getMethodVisitor().visitInsn(Opcodes.DNEG);
    }
	context.getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Math.class), "sin", "(D)D");
    context.getMethodVisitor().visitInsn(Opcodes.D2F);
    context.storeFTmp2();

	for (int n = 0; n < vsize; n++) {
		context.prepareVdForStore(n);
		if (n == ci) {
			context.loadFTmp1();
		} else if (si == ci || n == si) {
			context.loadFTmp2();
		} else {
			context.getMethodVisitor().visitLdcInsn(0.f);
		}
		context.storeVd(n);
	}
	context.endPfxCompiled();
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

return Common.disasmVROT("VROT", 1+one+(two<<1), vd, vs, imm5);
}
};
public static final Instruction VNOP = new Instruction(248) {

@Override
public final String name() { return "VNOP"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(ICompilerContext context, int insn) {
}
@Override
public String disasm(int address, int insn) {

return "vnop";
}
};
public static final Instruction VFLUSH = new Instruction(249) {

@Override
public final String name() { return "VFLUSH"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {

return "vflush";
}
};
public static final Instruction VSYNC = new Instruction(250) {

@Override
public final String name() { return "VSYNC"; }

@Override
public final String category() { return "VFPU"; }

@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(ICompilerContext context, int insn) {
	// Nothing to compile
}
@Override
public String disasm(int address, int insn) {

return "vsync";
}
};
}
