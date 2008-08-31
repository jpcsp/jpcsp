
/* this is an auto-generated file from Allegrex.isa file */
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

import jpcsp.Processor;

import static jpcsp.Debugger.DisassemblerModule.DisHelper.*;

/**
 *
 * @author hli
 */
public class Instructions {

public static final class NOP {
static void interpret(Processor processor, int insn) {



}
static void compile(Processor processor, int insn) {



}
static String disasm(int address, int insn) {


return "nop";
}
}

public static final class CACHE {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_CCIMMRS("cache", rt, signExtend(imm16), rs);
}
}

public static final class SYSCALL {
static void interpret(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;



            
}
static void compile(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;



            
}
static String disasm(int address, int insn) {
	int imm20 = (insn>>6)&1048575;


return Dis_Syscall(imm20);
}
}

public static final class ERET {
static void interpret(Processor processor, int insn) {



            
}
static void compile(Processor processor, int insn) {



            
}
static String disasm(int address, int insn) {


return "eret";
}
}

public static final class BREAK {
static void interpret(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;



            
}
static void compile(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;



            
}
static String disasm(int address, int insn) {
	int imm20 = (insn>>6)&1048575;


return Dis_Break(imm20);
}
}

public static final class SYNC {
static void interpret(Processor processor, int insn) {



            
}
static void compile(Processor processor, int insn) {



            
}
static String disasm(int address, int insn) {


return "sync";
}
}

public static final class ADD {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                // just ignore overflow exception as it is useless
                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rs] + processor.gpr[rt];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rs] + processor.gpr[rt]);
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("add", rd, rs, rt);
}
}

public static final class ADDU {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rs] + processor.gpr[rt];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rs] + processor.gpr[rt]);
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("addu", rd, rs, rt);
}
}

public static final class ADDI {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                // just ignore overflow exception as it is useless
                if (rt != 0) {
                    processor.gpr[rt] = processor.gpr[rs] + Processor.signExtend(imm16);
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, processor.gpr[rs] + Processor.signExtend(imm16));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + (" +
                        Processor.signExtend(imm16) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTRSIMM("addi", rt, rs, signExtend(imm16));
}
}

public static final class ADDIU {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = processor.gpr[rs] + Processor.signExtend(imm16);
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, processor.gpr[rs] + Processor.signExtend(imm16));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " + (" +
                        Processor.signExtend(imm16) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTRSIMM("addiu", rt, rs, signExtend(imm16));
}
}

public static final class AND {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rs] & processor.gpr[rt];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rs] & processor.gpr[rt]);
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " & " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("and", rd, rs, rt);
}
}

public static final class ANDI {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = processor.gpr[rs] & Processor.zeroExtend(imm16);
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, processor.gpr[rs] & Processor.zeroExtend(imm16));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " & (" +
                        Processor.zeroExtend(imm16) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTRSIMM("andi", rt, rs, zeroExtend(imm16));
}
}

public static final class NOR {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = ~(processor.gpr[rs] | processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, ~(processor.gpr[rs] | processor.gpr[rt]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = ~(" +
                        processor.get_gpr(rs) +
                        " | " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("nor", rd, rs, rt);
}
}

public static final class OR {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rs] | processor.gpr[rt];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rs] | processor.gpr[rt]);
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " | " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("or", rd, rs, rt);
}
}

public static final class ORI {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = processor.gpr[rs] | Processor.zeroExtend(imm16);
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, processor.gpr[rs] | Processor.zeroExtend(imm16));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " | (" +
                        Processor.zeroExtend(imm16) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTRSIMM("ori", rt, rs, zeroExtend(imm16));
}
}

public static final class XOR {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rs] ^ processor.gpr[rt];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rs] ^ processor.gpr[rt]);
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " ^ " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("xor", rd, rs, rt);
}
}

public static final class XORI {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = processor.gpr[rs] ^ Processor.zeroExtend(imm16);
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, processor.gpr[rs] ^ Processor.zeroExtend(imm16));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " ^ (" +
                        Processor.zeroExtend(imm16) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTRSIMM("xori", rt, rs, zeroExtend(imm16));
}
}

public static final class SLL {
static void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = (processor.gpr[rt] << sa);
                }
            
}
static void compile(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, (processor.gpr[rt] << sa));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " << " +
                        sa +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRTSA("sll", rd, rt, sa);
}
}

public static final class SLLV {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rt] << (processor.gpr[rs]&31);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rt] << (processor.gpr[rs]&31));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " << (" +
                        processor.get_gpr(rt) +
                        "&31));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRTRS("sllv", rd, rt, rs);
}
}

public static final class SRA {
static void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = (processor.gpr[rt] >> sa);
                }
            
}
static void compile(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, (processor.gpr[rt] >> sa));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >> " +
                        sa +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRTSA("sra", rd, rt, sa);
}
}

public static final class SRAV {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rt] >> (processor.gpr[rs]&31);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rt] >> (processor.gpr[rs]&31));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >> (" +
                        processor.get_gpr(rs) +
                        "&31));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRTRS("srav", rd, rt, rs);
}
}

public static final class SRL {
static void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = (processor.gpr[rt] >>> sa);
                }
            
}
static void compile(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, (processor.gpr[rt] >>> sa));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >>> " +
                        sa +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRTSA("srl", rd, rt, sa);
}
}

public static final class SRLV {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rt] >>> (processor.gpr[rs]&31);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rt] >>> (processor.gpr[rs]&31));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rt) +
                        " >>> (" +
                        processor.get_gpr(rs) +
                        "&31));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRTRS("srlv", rd, rt, rs);
}
}

public static final class ROTR {
static void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Integer.rotateRight(processor.gpr[rt], sa);
                }
            
}
static void compile(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Integer.rotateRight(processor.gpr[rt], sa));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.rotateRight(" +
                        processor.get_gpr(rt) +
                        ", " +
                        sa +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRTSA("rotr", rd, rt, sa);
}
}

public static final class ROTRV {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Integer.rotateRight(processor.gpr[rt], processor.gpr[rs]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Integer.rotateRight(processor.gpr[rt], processor.gpr[rs]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.rotateRight(" +
                        processor.get_gpr(rt) +
                        ", " +
                        processor.get_gpr(rs) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRTRS("rotrv", rd, rt, rs);
}
}

public static final class SLT {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Processor.signedCompare(processor.gpr[rs], processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Processor.signedCompare(processor.gpr[rs], processor.gpr[rt]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.signedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("slt", rd, rs, rt);
}
}

public static final class SLTI {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = Processor.signedCompare(processor.gpr[rs], Processor.signExtend(imm16));
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, Processor.signedCompare(processor.gpr[rs], Processor.signExtend(imm16)));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = Processor.signedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        Processor.signExtend(imm16) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTRSIMM("slti", rt, rs, signExtend(imm16));
}
}

public static final class SLTU {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Processor.unsignedCompare(processor.gpr[rt], processor.gpr[rs]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Processor.unsignedCompare(processor.gpr[rt], processor.gpr[rs]));
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.unsignedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("sltu", rd, rs, rt);
}
}

public static final class SLTIU {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = Processor.unsignedCompare(processor.gpr[rs], Processor.signExtend(imm16));
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed) {
                    processor.fix_gpr(rt, Processor.unsignedCompare(processor.gpr[rs], Processor.signExtend(imm16)));
                } else {
                    processor.load_gpr(rs, false);
                    processor.alter_gpr(rt);
                    processor.current_bb.emit(
                        processor.get_gpr(rt) +
                        " = Processor.unsignedCompare(" +
                        processor.get_gpr(rs) +
                        ", " +
                        Processor.signExtend(imm16) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTRSIMM("sltiu", rt, rs, signExtend(imm16));
}
}

public static final class SUB {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rs] - processor.gpr[rt];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rs] - processor.gpr[rt]);
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " - " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("sub", rd, rs, rt);
}
}

public static final class SUBU {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.gpr[rs] - processor.gpr[rt];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, processor.gpr[rs] - processor.gpr[rt]);
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (" +
                        processor.get_gpr(rs) +
                        " - " +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("subu", rd, rs, rt);
}
}

public static final class LUI {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;



                if (rt != 0) {
                    processor.gpr[rt] = (imm16 << 16);
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;



                if (rt == 0) {
                    return;
                }
                processor.fix_gpr(rt, (imm16 << 16));
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;


return Dis_RTIMM("lui", rt, zeroExtend(imm16));
}
}

public static final class SEB {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Processor.signExtend8(processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Processor.signExtend8(processor.gpr[rt]));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.signExtend8(" +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRT("seb", rd, rt);
}
}

public static final class SEH {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Processor.signExtend(processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Processor.signExtend(processor.gpr[rt]));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Processor.signExtend(" +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRT("seh", rd, rt);
}
}

public static final class BITREV {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Integer.reverse(processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Integer.reverse(processor.gpr[rt]));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.reverse(" +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRT("bitrev", rd, rt);
}
}

public static final class WSBH {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Integer.rotateRight(Integer.reverseBytes(processor.gpr[rt]), 16);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Integer.rotateRight(Integer.reverseBytes(processor.gpr[rt]), 16));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.rotateRight(Integer.reverseBytes(" +
                        processor.get_gpr(rt) +
                        "), 16);"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRT("wsbh", rd, rt);
}
}

public static final class WSBW {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Integer.reverseBytes(processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rd == 0) {
                    return;
                }
                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_gpr(rd, Integer.reverseBytes(processor.gpr[rt]));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = Integer.reverseBytes(" +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RDRT("wsbw", rd, rt);
}
}

public static final class MOVZ {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if ((rd != 0) && (processor.gpr[rt] == 0)) {
                    processor.gpr[rd] = processor.gpr[rs];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                        if (processor.gpr[rt] == 0) {
                            processor.fix_gpr(rd, processor.gpr[rs]);
                        }
                    } else {
                        processor.load_gpr(rs, false);
                        processor.load_gpr(rt, false);
                        processor.alter_gpr(rd);
                        processor.current_bb.emit(
                            "if (" +
                            processor.get_gpr(rt) +
                            " == 0) {\n" +
                            processor.get_gpr(rd) +
                            " = " +
                            processor.get_gpr(rs) +
                            ";}\n"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("movz", rd, rs, rt);
}
}

public static final class MOVN {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if ((rd != 0) && (processor.gpr[rt] != 0)) {
                    processor.gpr[rd] = processor.gpr[rs];
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                        if (processor.gpr[rt] != 0) {
                            processor.fix_gpr(rd, processor.gpr[rs]);
                        }
                    } else {
                        processor.load_gpr(rs, false);
                        processor.load_gpr(rt, false);
                        processor.alter_gpr(rd);
                        processor.current_bb.emit(
                            "if (" +
                            processor.get_gpr(rt) +
                            " != 0) {\n" +
                            processor.get_gpr(rd) +
                            " = " +
                            processor.get_gpr(rs) +
                            ";}\n"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("movn", rd, rs, rt);
}
}

public static final class MAX {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Processor.max(processor.gpr[rs], processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                        processor.fix_gpr(rd, Processor.max(processor.gpr[rs], processor.gpr[rt]));
                    } else {
                        processor.load_gpr(rs, false);
                        processor.load_gpr(rt, false);
                        processor.alter_gpr(rd);
                        processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Processor.max(" +
                            processor.get_gpr(rs) +
                            ", " +
                            processor.get_gpr(rt) +
                            "));"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("max", rd, rs, rt);
}
}

public static final class MIN {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Processor.min(processor.gpr[rs], processor.gpr[rt]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                        processor.fix_gpr(rd, Processor.min(processor.gpr[rs], processor.gpr[rt]));
                    } else {
                        processor.load_gpr(rs, false);
                        processor.load_gpr(rt, false);
                        processor.alter_gpr(rd);
                        processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Processor.min(" +
                            processor.get_gpr(rs) +
                            ", " +
                            processor.get_gpr(rt) +
                            "));"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RDRSRT("min", rd, rs, rt);
}
}

public static final class CLZ {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Integer.numberOfLeadingZeros(processor.gpr[rs]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    if (processor.tracked_gpr[rs].fixed) {
                        processor.fix_gpr(rd, Integer.numberOfLeadingZeros(processor.gpr[rs]));
                    } else {
                        processor.load_gpr(rs, false);
                        processor.alter_gpr(rd);
                        processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Integer.numberOfLeadingZeros(" +
                            processor.get_gpr(rs) +
                            ");"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


return Dis_RDRS("clz", rd, rs);
}
}

public static final class CLO {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = Integer.numberOfLeadingZeros(~processor.gpr[rs]);
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    if (processor.tracked_gpr[rs].fixed) {
                        processor.fix_gpr(rd, Integer.numberOfLeadingZeros(~processor.gpr[rs]));
                    } else {
                        processor.load_gpr(rs, false);
                        processor.alter_gpr(rd);
                        processor.current_bb.emit(
                            processor.get_gpr(rd) +
                            " = Integer.numberOfLeadingZeros(~" +
                            processor.get_gpr(rs) +
                            ");"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


return Dis_RDRS("clo", rd, rs);
}
}

public static final class EXT {
static void interpret(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = Processor.extractBits(processor.gpr[rs], lsb, msb+1);
                }
            
}
static void compile(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    if (processor.tracked_gpr[rs].fixed) {
                        processor.fix_gpr(rt, Processor.extractBits(processor.gpr[rs], lsb, msb+1));
                    } else {
                        processor.load_gpr(rs, false);
                        processor.alter_gpr(rt);
                        processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = Processor.extractBits(" +
                            processor.get_gpr(rs) +
                            ", " +
                            lsb +
                            ", " +
                            (msb+1) +
                            ");"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_Ext(rt, rs, lsb, msb);
}
}

public static final class INS {
static void interpret(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    processor.gpr[rt] = Processor.insertBits(processor.gpr[rt], processor.gpr[rs], lsb, msb);
                }
            
}
static void compile(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                if (rt != 0) {
                    if (processor.tracked_gpr[rs].fixed) {
                        processor.fix_gpr(rt, Processor.insertBits(processor.gpr[rt], processor.gpr[rs], lsb, msb));
                    } else {
                        processor.load_gpr(rs, false);
                        processor.load_gpr(rt, false);
                        processor.alter_gpr(rt);
                        processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = Processor.insertBits(" +
                            processor.get_gpr(rt) +
                            ", " +
                            processor.get_gpr(rs) +
                            ", " +
                            lsb +
                            ", " +
                            msb +
                            ");"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_Ins(rt, rs, lsb, msb);
}
}

public static final class MULT {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo = ((long) processor.gpr[rs]) * ((long) processor.gpr[rt]);
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " = ((long) " +
                    processor.get_gpr(rs) +
                    ") * ((long) " +
                    processor.get_gpr(rt) +
                    ");"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("mult", rs, rt);
}
}

public static final class MULTU {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo = (((long) processor.gpr[rs]) & 0xffffffff) * (((long) processor.gpr[rt]) & 0xffffffff);
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " = (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff) * (((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("multu", rs, rt);
}
}

public static final class MADD {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo += ((long) processor.gpr[rs]) * ((long) processor.gpr[rt]);
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.load_hilo(false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " += ((long) " +
                    processor.get_gpr(rs) +
                    ") * ((long) " +
                    processor.get_gpr(rt) +
                    ");"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("madd", rs, rt);
}
}

public static final class MADDU {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo += (((long) processor.gpr[rs]) & 0xffffffff) * (((long) processor.gpr[rt]) & 0xffffffff);
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.load_hilo(false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " += (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff) * (((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("maddu", rs, rt);
}
}

public static final class MSUB {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo -= ((long) processor.gpr[rs]) * ((long) processor.gpr[rt]);
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.load_hilo(false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " += ((long) " +
                    processor.get_gpr(rs) +
                    ") * ((long) " +
                    processor.get_gpr(rt) +
                    ");"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("msub", rs, rt);
}
}

public static final class MSUBU {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo -= (((long) processor.gpr[rs]) & 0xffffffff) * (((long) processor.gpr[rt]) & 0xffffffff);
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.load_hilo(false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " -= (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff) * (((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("msubu", rs, rt);
}
}

public static final class DIV {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo = Processor.signedDivMod(processor.gpr[rs], processor.gpr[rt]);
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " = Processor.signedDivMod(" +
                    processor.get_gpr(rs) +
                    ", " +
                    processor.get_gpr(rt) +
                    ");"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("div", rs, rt);
}
}

public static final class DIVU {
static void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.hilo = Processor.unsignedDivMod((((long) processor.gpr[rs]) & 0xffffffff), (((long) processor.gpr[rt]) & 0xffffffff));
            
}
static void compile(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.load_gpr(rt, false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " = Processor.unsignedDivMod(((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff, ((long) " +
                    processor.get_gpr(rt) +
                    ") & 0xffffffff);"
                );
            
}
static String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRT("divu", rs, rt);
}
}

public static final class MFHI {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.hi();
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;



                if (rd != 0) {
                    processor.load_hilo(false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (int) (" +
                        processor.get_hilo() +
                        " >>> 32);"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;


return Dis_RD("mfhi", rd);
}
}

public static final class MFLO {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.lo();
                }
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;



                if (rd != 0) {
                    processor.load_hilo(false);
                    processor.alter_gpr(rd);
                    processor.current_bb.emit(
                        processor.get_gpr(rd) +
                        " = (int) (" +
                        processor.get_hilo() +
                        " & 0xffffffff);"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;


return Dis_RD("mflo", rd);
}
}

public static final class MTHI {
static void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;



                processor.hilo = (processor.hilo & 0xffffffff) | (((long) processor.gpr[rs]) << 32);
            
}
static void compile(Processor processor, int insn) {
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " = (" +
                    processor.get_hilo() +
                    " & 0xffffffff) | (((long) " +
                    processor.get_gpr(rs) +
                    " << 32);"
                    );
            
}
static String disasm(int address, int insn) {
	int rs = (insn>>21)&31;


return Dis_RS("mthi", rs);
}
}

public static final class MTLO {
static void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;



                processor.hilo = ((processor.hilo >>> 32) << 32) | (((long) processor.gpr[rs]) & 0xffffffff);
            
}
static void compile(Processor processor, int insn) {
	int rs = (insn>>21)&31;



                processor.load_gpr(rs, false);
                processor.alter_hilo();
                processor.current_bb.emit(
                    processor.get_hilo() +
                    " = ((" +
                    processor.get_hilo() +
                    " >>> 32) << 32) | (((long) " +
                    processor.get_gpr(rs) +
                    ") & 0xffffffff);"
                    );
            
}
static String disasm(int address, int insn) {
	int rs = (insn>>21)&31;


return Dis_RS("mtlo", rs);
}
}

public static final class BEQ {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.npc = (processor.gpr[rs] == processor.gpr[rt]) ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                boolean c = (processor.gpr[rs] == processor.gpr[rt]);
                int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                boolean loop = (target == processor.current_bb.getEntry());
                if (processor.tracked_gpr[rs].fixed && processor.tracked_gpr[rt].fixed) {
                    if (c) {
                        processor.current_bb.emit("processor.pc = 0x" + Integer.toHexString(target) + ";");
                    } else {
                        processor.current_bb.emit("processor.pc += 4;");
                    }
                } else {
                    processor.load_gpr(rs, false);
                    processor.load_gpr(rt, false);
                    processor.current_bb.emit(
                        "processor.pc = (" +
                        processor.get_gpr(rs) +
                        " == " +
                        processor.get_gpr(rt) +
                        ") ? 0x" +
                        Integer.toHexString(target) +
                        " : (processor.pc + 4);"
                    );
                }
                processor.npc = c ? target : (processor.pc + 4);
                processor.stepDelayslot();
                processor.reset_register_tracking();
                processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRTOFFSET("beq", rs, rt, imm16, address);
}
}

public static final class BEQL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRTOFFSET("beql", rs, rt, imm16, address);
}
}

public static final class BGEZ {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bgez", rs, imm16, address);
}
}

public static final class BGEZAL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bgezal", rs, imm16, address);
}
}

public static final class BGEZALL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bgezall", rs, imm16, address);
}
}

public static final class BGEZL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bgezl", rs, imm16, address);
}
}

public static final class BGTZ {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bgtz", rs, imm16, address);
}
}

public static final class BGTZL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bgtzl", rs, imm16, address);
}
}

public static final class BLEZ {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("blez", rs, imm16, address);
}
}

public static final class BLEZL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("blezl", rs, imm16, address);
}
}

public static final class BLTZ {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bltz", rs, imm16, address);
}
}

public static final class BLTZAL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bltzal", rs, imm16, address);
}
}

public static final class BLTZALL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bltzall", rs, imm16, address);
}
}

public static final class BLTZL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


return Dis_RSOFFSET("bltzl", rs, imm16, address);
}
}

public static final class BNE {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



                processor.npc = (processor.gpr[rs] != processor.gpr[rt]) ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRTOFFSET("bne", rs, rt, imm16, address);
}
}

public static final class BNEL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RSRTOFFSET("bnel", rs, rt, imm16, address);
}
}

public static final class J {
static void interpret(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;



            
}
static void compile(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;



            
}
static String disasm(int address, int insn) {
	int imm26 = (insn>>0)&67108863;


return Dis_JUMP("j", imm26, address);
}
}

public static final class JAL {
static void interpret(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;



            
}
static void compile(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;



            
}
static String disasm(int address, int insn) {
	int imm26 = (insn>>0)&67108863;


return Dis_JUMP("jal", imm26, address);
}
}

public static final class JALR {
static void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;



                if (rd != 0) {
                    processor.gpr[rd] = processor.pc + 4;
                }
                processor.npc = processor.gpr[rs];
                processor.stepDelayslot();
            
}
static void compile(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;



                boolean loop = (processor.tracked_gpr[rs].fixed && (processor.gpr[rs] == processor.current_bb.getEntry()));
                if (rd != 0) {
                    processor.fix_gpr(rd, processor.pc + 4);
                }
                processor.current_bb.emit(
                    "processor.pc = " + processor.get_gpr(rs) + ";"
                );
                processor.npc = processor.gpr[rs];
                processor.stepDelayslot();
                processor.reset_register_tracking();
                processor.current_bb.emit("break;");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
static String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


return Dis_RDRS("jalr", rd, rs);
}
}

public static final class JR {
static void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;



                processor.npc = processor.gpr[rs];
                processor.stepDelayslot();
            
}
static void compile(Processor processor, int insn) {
	int rs = (insn>>21)&31;



                boolean loop = (processor.tracked_gpr[rs].fixed && (processor.gpr[rs] == processor.current_bb.getEntry()));
                processor.current_bb.emit(
                    "processor.pc = " + processor.get_gpr(rs) + ";"
                );
                processor.npc = processor.gpr[rs];
                processor.stepDelayslot();
                processor.reset_register_tracking();
                processor.current_bb.emit(loop ? "continue;" : "break;");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
static String disasm(int address, int insn) {
	int rs = (insn>>21)&31;


return Dis_RS("jr", rs);
}
}

public static final class BC1F {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                processor.npc = !processor.fcr31_c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                boolean loop = (target == processor.current_bb.getEntry());
                processor.current_bb.emit("processor.pc = (!processor.fcr31_c) ? 0x" + Integer.toHexString(target) + " : processor.pc + 4;");
                processor.npc = !processor.fcr31_c ? target : (processor.pc + 4);
                processor.stepDelayslot();
                processor.reset_register_tracking();
                processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;


return Dis_OFFSET("bc1f", imm16, address);
}
}

public static final class BC1T {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                processor.npc = processor.fcr31_c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                boolean loop = (target == processor.current_bb.getEntry());
                processor.current_bb.emit("processor.pc = processor.fcr31_c ? 0x" + Integer.toHexString(target) + " : (processor.pc + 4);");
                processor.npc = processor.fcr31_c ? target : (processor.pc + 4);
                processor.stepDelayslot();
                processor.reset_register_tracking();
                processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;


return Dis_OFFSET("bc1t", imm16, address);
}
}

public static final class BC1FL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                if (!processor.fcr31_c) {
                    processor.npc = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                    processor.stepDelayslot();
                } else {
                    processor.pc += 4;
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                boolean loop = (target == processor.current_bb.getEntry());
                processor.current_bb.emit(
                    "if (!processor.fcr31_c) {\n" +
                        "processor.pc = 0x" + Integer.toHexString(target) + ";"
                );
                processor.npc = !processor.fcr31_c ? target : (processor.pc + 4);
                processor.stepDelayslot();
                processor.current_bb.emit(
                    "} else {\n" +
                        "processor.pc += 4;" +
                    "}\n"
                );
                processor.reset_register_tracking();
                processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;


return Dis_OFFSET("bc1fl", imm16, address);
}
}

public static final class BC1TL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                if (processor.fcr31_c) {
                    processor.npc = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                    processor.stepDelayslot();
                } else {
                    processor.pc += 4;
                }
            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;



                int target = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                boolean loop = (target == processor.current_bb.getEntry());
                processor.current_bb.emit(
                    "if (processor.fcr31_c) {\n" +
                        "processor.pc = 0x" + Integer.toHexString(target) + ";"
                );
                processor.npc = processor.fcr31_c ? target : (processor.pc + 4);
                processor.stepDelayslot();
                processor.current_bb.emit(
                    "} else {\n" +
                        "processor.pc += 4;" +
                    "}\n"
                );
                processor.reset_register_tracking();
                processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;


return Dis_OFFSET("bc1tl", imm16, address);
}
}

public static final class LB {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("lb", rt, rs, imm16);
}
}

public static final class LBU {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("lbu", rt, rs, imm16);
}
}

public static final class LH {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("lh", rt, rs, imm16);
}
}

public static final class LHU {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("lhu", rt, rs, imm16);
}
}

public static final class LW {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("lw", rt, rs, imm16);
}
}

public static final class LWL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("lwl", rt, rs, imm16);
}
}

public static final class LWR {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("lwr", rt, rs, imm16);
}
}

public static final class SB {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("sb", rt, rs, imm16);
}
}

public static final class SH {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("sh", rt, rs, imm16);
}
}

public static final class SW {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("sw", rt, rs, imm16);
}
}

public static final class SWL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("swl", rt, rs, imm16);
}
}

public static final class SWR {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("swr", rt, rs, imm16);
}
}

public static final class LL {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("ll", rt, rs, imm16);
}
}

public static final class SC {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_RTIMMRS("sc", rt, rs, imm16);
}
}

public static final class LWC1 {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_FTIMMRS("lwc1", ft, rs, imm16);
}
}

public static final class SWC1 {
static void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;



            
}
static String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


return Dis_FTIMMRS("swc1", ft, rs, imm16);
}
}

public static final class ADD_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                processor.fpr[fd] = processor.fpr[fs] + processor.fpr[ft];
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                    processor.fix_fpr(fd, processor.fpr[fs] + processor.fpr[ft]);
                } else {
                    processor.load_fpr(fs, false);
                    processor.load_fpr(ft, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " + " +
                        processor.get_fpr(ft) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


return Dis_FDFSFT("add.s", fd, fs, ft);
}
}

public static final class SUB_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                processor.fpr[fd] = processor.fpr[fs] - processor.fpr[ft];
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                    processor.fix_fpr(fd, processor.fpr[fs] - processor.fpr[ft]);
                } else {
                    processor.load_fpr(fs, false);
                    processor.load_fpr(ft, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " - " +
                        processor.get_fpr(ft) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


return Dis_FDFSFT("sub.s", fd, fs, ft);
}
}

public static final class MUL_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                processor.fpr[fd] = processor.fpr[fs] * processor.fpr[ft];
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                    processor.fix_fpr(fd, processor.fpr[fs] * processor.fpr[ft]);
                } else {
                    processor.load_fpr(fs, false);
                    processor.load_fpr(ft, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " * " +
                        processor.get_fpr(ft) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


return Dis_FDFSFT("mul.s", fd, fs, ft);
}
}

public static final class DIV_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                processor.fpr[fd] = processor.fpr[fs] / processor.fpr[ft];
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                if (processor.tracked_fpr[fs].fixed & processor.tracked_fpr[ft].fixed) {
                    processor.fix_fpr(fd, processor.fpr[fs] / processor.fpr[ft]);
                } else {
                    processor.load_fpr(fs, false);
                    processor.load_fpr(ft, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        " / " +
                        processor.get_fpr(ft) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


return Dis_FDFSFT("div.s", fd, fs, ft);
}
}

public static final class SQRT_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = (float) Math.sqrt(processor.fpr[fs]);
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, (float) Math.sqrt(processor.fpr[fs]));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (float) Math.sqrt(" +
                        processor.get_fpr(fs) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("sqrt.s", fd, fs);
}
}

public static final class ABS_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = Math.abs(processor.fpr[fs]);
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, Math.abs(processor.fpr[fs]));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Math.abs(" +
                        processor.get_fpr(fs) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("abs.s", fd, fs);
}
}

public static final class MOV_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = processor.fpr[fs];
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, processor.fpr[fs]);
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (" +
                        processor.get_fpr(fs) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("mov.s", fd, fs);
}
}

public static final class NEG_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = 0.0f - processor.fpr[fs];
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, 0.0f - processor.fpr[fs]);
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = 0.0f - (" +
                        processor.get_fpr(fs) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("neg.s", fd, fs);
}
}

public static final class ROUND_W_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = Float.intBitsToFloat(Math.round(processor.fpr[fs]));
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, Float.intBitsToFloat(Math.round(processor.fpr[fs])));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat(Math.round(" +
                        processor.get_fpr(fs) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("round.w.s", fd, fs);
}
}

public static final class TRUNC_W_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = Float.intBitsToFloat((int) (processor.fpr[fs]));
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, Float.intBitsToFloat((int) (processor.fpr[fs])));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat((int) (" +
                        processor.get_fpr(fs) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("trunc.w.s", fd, fs);
}
}

public static final class CEIL_W_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = Float.intBitsToFloat((int) Math.ceil(processor.fpr[fs]));
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, Float.intBitsToFloat((int) Math.ceil(processor.fpr[fs])));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat((int) Math.ceil(" +
                        processor.get_fpr(fs) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("ceil.w.s", fd, fs);
}
}

public static final class FLOOR_W_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = Float.intBitsToFloat((int) Math.floor(processor.fpr[fs]));
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, Float.intBitsToFloat((int) Math.floor(processor.fpr[fs])));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = Float.intBitsToFloat((int) Math.floor(" +
                        processor.get_fpr(fs) +
                        "));"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("floor.w.s", fd, fs);
}
}

public static final class CVT_S_W {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.fpr[fd] = (float) Float.floatToRawIntBits(processor.fpr[fs]);
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                if (processor.tracked_fpr[fs].fixed) {
                    processor.fix_fpr(fd, (float) Float.floatToRawIntBits(processor.fpr[fs]));
                } else {
                    processor.load_fpr(fs, false);
                    processor.alter_fpr(fd);
                    processor.current_bb.emit(
                        processor.get_fpr(fd) +
                        " = (float) Float.floatToRawIntBits(" +
                        processor.get_fpr(fs) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("cvt.s.w", fd, fs);
}
}

public static final class CVT_W_S {
static void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                switch (processor.fcr31_rm) {
                    case 1:
                        processor.fpr[fd] = Float.intBitsToFloat((int) (processor.fpr[fs]));
                        break;
                    case 2:
                        processor.fpr[fd] = Float.intBitsToFloat((int) Math.ceil(processor.fpr[fs]));
                        break;
                    case 3:
                        processor.fpr[fd] = Float.intBitsToFloat((int) Math.floor(processor.fpr[fs]));
                        break;
                    default:
                        processor.fpr[fd] = Float.intBitsToFloat((int) Math.rint(processor.fpr[fs]));
                        break;
                }
            
}
static void compile(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;



                processor.load_fpr(fs, false);
                processor.alter_fpr(fd);
                processor.current_bb.emit(
                    "switch (processor.fcr31_rm) {\n" +
                        "case 1:\n" +
                            processor.get_fpr(fd) +
                            " = Float.intBitsToFloat((int) (" +
                            processor.get_fpr(fs) +
                            "));\n" +
                            "break;\n" +
                        "case 2:\n" +
                            processor.get_fpr(fd) +
                            " = Float.intBitsToFloat((int) Math.ceil(" +
                            processor.get_fpr(fs) +
                            "));\n" +
                            "break;\n" +
                        "case 3:\n" +
                            processor.get_fpr(fd) +
                            " = Float.intBitsToFloat((int) Math.floor(" +
                            processor.get_fpr(fs) +
                            "));\n" +
                            "break;\n" +
                        "default:\n" + 
                            processor.get_fpr(fd) +
                            " = Float.intBitsToFloat((int) Math.rint(" + 
                            processor.get_fpr(fs) +
                            "));\n" +
                            "break;\n" +
                    "}\n"
                );
            
}
static String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


return Dis_FDFS("cvt.w.s", fd, fs);
}
}

public static final class C_COND_S {
static void interpret(Processor processor, int insn) {
	int fcond = (insn>>0)&15;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                float x = processor.fpr[fs];
                float y = processor.fpr[ft];
                boolean unordered = ((fcond & 1) != 0) && (Float.isNaN(x) || Float.isNaN(y));

                if (unordered) {
                    // we ignore float exception as it is useless for games
                    //if ((fcond & 8) != 0) {
                    //}

                    processor.fcr31_c = true;
                } else {
                    boolean equal = ((fcond & 2) != 0) && (x == y);
                    boolean less = ((fcond & 4) != 0) && (x < y);

                    processor.fcr31_c = less || equal;
                }
            
}
static void compile(Processor processor, int insn) {
	int fcond = (insn>>0)&15;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;



                processor.load_fpr(fs, false);
                processor.load_fpr(ft, false);
                processor.current_bb.emit(
                    "{\n" +
                    "float x = " + processor.get_fpr(fs) + ";" +
                    "float y = " + processor.get_fpr(ft) + ";" +
                    "boolean unordered = ((" + fcond + " & 1) != 0) && (Float.isNaN(x) || Float.isNaN(y));" +
                    "if (unordered) {\n" +
                        "// we ignore float exception as it is useless for games\n" +
                        "//if ((" + fcond + " & 8) != 0) {\n" +
                        "//}\n" +
                        "processor.fcr31_c = true;" +
                    "} else {\n" +
                        "boolean equal = ((" + fcond + " & 2) != 0) && (x == y);" +
                        "boolean less = ((" + fcond + " & 4) != 0) && (x < y);" +
                        "processor.fcr31_c = less || equal;" +
                    "}\n}\n"
                );
            
}
static String disasm(int address, int insn) {
	int fcond = (insn>>0)&15;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


return Dis_Cconds(fcond, fs, ft);
}
}

public static final class MFC1 {
static void interpret(Processor processor, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rt != 0) {
                    processor.gpr[rt] = Float.floatToRawIntBits(processor.fpr[fs]);
                }
            
}
static void compile(Processor processor, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rt != 0) {
                    if (processor.tracked_fpr[fs].fixed) {
                        processor.fix_gpr(rt, Float.floatToRawIntBits(processor.fpr[fs]));
                    } else {
                        processor.load_fpr(fs, false);
                        processor.alter_gpr(rt);
                        processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = Float.floatToRawIntBits(" +
                            processor.get_fpr(fs) +
                            ");"
                        );
                    }
                }
            
}
static String disasm(int address, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RTFS("mfc1", rt, fs);
}
}

public static final class CFC1 {
static void interpret(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rt != 0) {
                    switch (c1cr) {
                    case 0:                        
                        processor.gpr[rt] = (Processor.fcr0_imp << 8) | (Processor.fcr0_rev);
                        break;

                    case 31:
                        processor.gpr[rt] = (processor.fcr31_fs ? (1 << 24) : 0) | (processor.fcr31_c ? (1 << 23) : 0) | (processor.fcr31_rm & 3);
                        break;
                    }
                }
            
}
static void compile(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (rt != 0) {
                    switch (c1cr) {
                    case 0:
                        processor.alter_gpr(rt);
                        processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = (Processor.fcr0_imp << 8) | (Processor.fcr0_rev);"
                        );
                        break;

                    case 31:
                        processor.alter_gpr(rt);
                        processor.current_bb.emit(
                            processor.get_gpr(rt) +
                            " = (processor.fcr31_fs ? (1 << 24) : 0) | (processor.fcr31_c ? (1 << 23) : 0) | (processor.fcr31_rm & 3);"
                        );
                        break;
                    }
                }
            
}
static String disasm(int address, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RTFC("cfc1", rt, c1cr);
}
}

public static final class MTC1 {
static void interpret(Processor processor, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;



                processor.fpr[fs] = Float.intBitsToFloat(processor.gpr[rt]);
            
}
static void compile(Processor processor, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (processor.tracked_gpr[rt].fixed) {
                    processor.fix_fpr(fs, Float.intBitsToFloat(processor.gpr[rt]));
                } else {
                    processor.load_gpr(rt, false);
                    processor.alter_fpr(fs);
                    processor.current_bb.emit(
                        processor.get_fpr(fs) +
                        " = Float.intBitsToFloat(" +
                        processor.get_gpr(rt) +
                        ");"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RTFS("mtc1", rt, fs);
}
}

public static final class CTC1 {
static void interpret(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (c1cr == 31) {
                    int bits = processor.gpr[rt] & 0x01800003;
                    processor.fcr31_rm = bits & 3;
                    bits >>= 23;
                    processor.fcr31_fs = (bits > 1);
                    processor.fcr31_c = (bits >> 1) == 1;
                }
            
}
static void compile(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



                if (c1cr == 31) {
                    processor.load_gpr(rt, false);
                    processor.current_bb.emit(
                        "{\n" +
                            "int bits = " + processor.get_gpr(rt) + " & 0x01800003;" +
                            "processor.fcr31_rm = bits & 3;" +
                            "bits >>= 23;" +
                            "processor.fcr31_fs = (bits > 1);" +
                            "processor.fcr31_c = (bits >> 1) == 1;" +
                        "}\n"
                    );
                }
            
}
static String disasm(int address, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


return Dis_RTFC("ctc1", rt, c1cr);
}
}

public static final class MFC0 {
static void interpret(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static void compile(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static String disasm(int address, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


return "Unimplemented MFC0";
}
}

public static final class CFC0 {
static void interpret(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static void compile(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static String disasm(int address, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


return "Unimplemented CFC0";
}
}

public static final class MTC0 {
static void interpret(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static void compile(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static String disasm(int address, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


return "Unimplemented MTC0";
}
}

public static final class CTC0 {
static void interpret(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static void compile(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;



            
}
static String disasm(int address, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


return "Unimplemented CTC0";
}
}

}
