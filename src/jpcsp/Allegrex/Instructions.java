
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

import jpcsp.Allegrex.Common.*;

import static jpcsp.Debugger.DisassemblerModule.DisHelper.*;

/**
 *
 * @author hli
 */
public class Instructions {


public static final Instruction NOP = new Instruction(0) {
@Override
public void interpret(Processor processor, int insn) {


}
@Override
public void compile(Processor processor, int insn) {


}
@Override
public String disasm(int address, int insn) {

return "nop";
}
};
public static final Instruction ICACHE_INDEX_INVALIDATE = new Instruction(1) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x04, signExtend(imm16), rs);
}
};
public static final Instruction ICACHE_INDEX_UNLOCK = new Instruction(2) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x06, signExtend(imm16), rs);
}
};
public static final Instruction ICACHE_HIT_INVALIDATE = new Instruction(3) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x08, signExtend(imm16), rs);
}
};
public static final Instruction ICACHE_FILL = new Instruction(4) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x0A, signExtend(imm16), rs);
}
};
public static final Instruction ICACHE_FILL_WITH_LOCK = new Instruction(5) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x0B, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_INDEX_WRITEBACK_INVALIDATE = new Instruction(6) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x14, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_INDEX_UNLOCK = new Instruction(7) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x16, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_CREATE_DIRTY_EXCLUSIVE = new Instruction(8) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x18, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_HIT_INVALIDATE = new Instruction(9) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x19, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_HIT_WRITEBACK = new Instruction(10) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1A, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_HIT_WRITEBACK_INVALIDATE = new Instruction(11) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1B, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_CREATE_DIRTY_EXCLUSIVE_WITH_LOCK = new Instruction(12) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1C, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_FILL = new Instruction(13) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1E, signExtend(imm16), rs);
}
};
public static final Instruction DCACHE_FILL_WITH_LOCK = new Instruction(14) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmCODEIMMRS("cache", 0x1F, signExtend(imm16), rs);
}
};
public static final Instruction SYSCALL = new Instruction(15) {
@Override
public void interpret(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;


            
}
@Override
public String disasm(int address, int insn) {
	int imm20 = (insn>>6)&1048575;

return Common.disasmSYSCALL(imm20);
}
};
public static final Instruction ERET = new Instruction(16) {
@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(Processor processor, int insn) {


            
}
@Override
public String disasm(int address, int insn) {

return "eret";
}
};
public static final Instruction BREAK = new Instruction(17) {
@Override
public void interpret(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm20 = (insn>>6)&1048575;


            
}
@Override
public String disasm(int address, int insn) {
	int imm20 = (insn>>6)&1048575;

return Common.disasmBREAK(imm20);
}
};
public static final Instruction SYNC = new Instruction(18) {
@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(Processor processor, int insn) {


            
}
@Override
public String disasm(int address, int insn) {

return "sync";
}
};
public static final Instruction ADD = new Instruction(19) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                // just ignore overflow exception as it is useless
                processor.cpu.doADDU(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("add", rd, rs, rt);
}
};
public static final Instruction ADDU = new Instruction(20) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doADDU(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("addu", rd, rs, rt);
}
};
public static final Instruction ADDI = new Instruction(21) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                // just ignore overflow exception as it is useless
                processor.cpu.doADDIU(rt, rs, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("addi", rt, rs, signExtend(imm16));
}
};
public static final Instruction ADDIU = new Instruction(22) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doADDIU(rt, rs, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("addiu", rt, rs, signExtend(imm16));
}
};
public static final Instruction AND = new Instruction(23) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doAND(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("and", rd, rs, rt);
}
};
public static final Instruction ANDI = new Instruction(24) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doANDI(rt, rs, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("andi", rt, rs, zeroExtend(imm16));
}
};
public static final Instruction NOR = new Instruction(25) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doNOR(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("nor", rd, rs, rt);
}
};
public static final Instruction OR = new Instruction(26) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doOR(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("or", rd, rs, rt);
}
};
public static final Instruction ORI = new Instruction(27) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doORI(rt, rs, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("ori", rt, rs, zeroExtend(imm16));
}
};
public static final Instruction XOR = new Instruction(28) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doXOR(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("xor", rd, rs, rt);
}
};
public static final Instruction XORI = new Instruction(29) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doXORI(rt, rs, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("xori", rt, rs, zeroExtend(imm16));
}
};
public static final Instruction SLL = new Instruction(30) {
@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSLL(rd, rt, sa);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("sll", rd, rt, sa);
}
};
public static final Instruction SLLV = new Instruction(31) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLLV(rd, rt, rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("sllv", rd, rt, rs);
}
};
public static final Instruction SRA = new Instruction(32) {
@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSRA(rd, rt, sa);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("sra", rd, rt, sa);
}
};
public static final Instruction SRAV = new Instruction(33) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSRAV(rd, rt, rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("srav", rd, rt, rs);
}
};
public static final Instruction SRL = new Instruction(34) {
@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSRL(rd, rt, sa);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("srl", rd, rt, sa);
}
};
public static final Instruction SRLV = new Instruction(35) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSRLV(rd, rt, rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("srlv", rd, rt, rs);
}
};
public static final Instruction ROTR = new Instruction(36) {
@Override
public void interpret(Processor processor, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doROTR(rd, rt, sa);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int sa = (insn>>6)&31;
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRTSA("rotr", rd, rt, sa);
}
};
public static final Instruction ROTRV = new Instruction(37) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doROTRV(rd, rt, rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRTRS("rotrv", rd, rt, rs);
}
};
public static final Instruction SLT = new Instruction(38) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLT(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("slt", rd, rs, rt);
}
};
public static final Instruction SLTI = new Instruction(39) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLTI(rt, rs, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("slti", rt, rs, signExtend(imm16));
}
};
public static final Instruction SLTU = new Instruction(40) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLTU(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("sltu", rd, rs, rt);
}
};
public static final Instruction SLTIU = new Instruction(41) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSLTIU(rt, rs, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTRSIMM("sltiu", rt, rs, signExtend(imm16));
}
};
public static final Instruction SUB = new Instruction(42) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                // just ignore overflow exception as it is useless
                processor.cpu.doSUBU(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("sub", rd, rs, rt);
}
};
public static final Instruction SUBU = new Instruction(43) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doSUBU(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("subu", rd, rs, rt);
}
};
public static final Instruction LUI = new Instruction(44) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;


                processor.cpu.doLUI(rt, imm16);
            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;


                if (rt == 0) {
                    return;
                }
                processor.fix_gpr(rt, (imm16 << 16));
            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;

return Common.disasmRTIMM("lui", rt, zeroExtend(imm16));
}
};
public static final Instruction SEB = new Instruction(45) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSEB(rd, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("seb", rd, rt);
}
};
public static final Instruction SEH = new Instruction(46) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doSEH(rd, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("seh", rd, rt);
}
};
public static final Instruction BITREV = new Instruction(47) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doBITREV(rd, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("bitrev", rd, rt);
}
};
public static final Instruction WSBH = new Instruction(48) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doWSBH(rd, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("wsbh", rd, rt);
}
};
public static final Instruction WSBW = new Instruction(49) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;


                processor.cpu.doWSBW(rd, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRDRT("wsbw", rd, rt);
}
};
public static final Instruction MOVZ = new Instruction(50) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMOVZ(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("movz", rd, rs, rt);
}
};
public static final Instruction MOVN = new Instruction(51) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMOVN(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("movn", rd, rs, rt);
}
};
public static final Instruction MAX = new Instruction(52) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMAX(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("max", rd, rs, rt);
}
};
public static final Instruction MIN = new Instruction(53) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMIN(rd, rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRSRT("min", rd, rs, rt);
}
};
public static final Instruction CLZ = new Instruction(54) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doCLZ(rd, rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRS("clz", rd, rs);
}
};
public static final Instruction CLO = new Instruction(55) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doCLO(rd, rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRS("clo", rd, rs);
}
};
public static final Instruction EXT = new Instruction(56) {
@Override
public void interpret(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doEXT(rt, rs, lsb, msb+1);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmEXT(rt, rs, lsb, msb);
}
};
public static final Instruction INS = new Instruction(57) {
@Override
public void interpret(Processor processor, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doINS(rt, rs, lsb, msb);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int lsb = (insn>>6)&31;
	int msb = (insn>>11)&31;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmINS(rt, rs, lsb, msb);
}
};
public static final Instruction MULT = new Instruction(58) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMULT(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("mult", rs, rt);
}
};
public static final Instruction MULTU = new Instruction(59) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMULTU(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("multu", rs, rt);
}
};
public static final Instruction MADD = new Instruction(60) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMADD(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("madd", rs, rt);
}
};
public static final Instruction MADDU = new Instruction(61) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMADDU(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("maddu", rs, rt);
}
};
public static final Instruction MSUB = new Instruction(62) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMSUB(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("msub", rs, rt);
}
};
public static final Instruction MSUBU = new Instruction(63) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doMSUBU(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("msubu", rs, rt);
}
};
public static final Instruction DIV = new Instruction(64) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doDIV(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("div", rs, rt);
}
};
public static final Instruction DIVU = new Instruction(65) {
@Override
public void interpret(Processor processor, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.cpu.doDIVU(rs, rt);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRT("divu", rs, rt);
}
};
public static final Instruction MFHI = new Instruction(66) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;


                processor.cpu.doMFHI(rd);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;

return Common.disasmRD("mfhi", rd);
}
};
public static final Instruction MFLO = new Instruction(67) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;


                processor.cpu.doMFLO(rd);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;

return Common.disasmRD("mflo", rd);
}
};
public static final Instruction MTHI = new Instruction(68) {
@Override
public void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;


                processor.cpu.doMTHI(rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rs = (insn>>21)&31;

return Common.disasmRS("mthi", rs);
}
};
public static final Instruction MTLO = new Instruction(69) {
@Override
public void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;


                processor.cpu.doMTLO(rs);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rs = (insn>>21)&31;

return Common.disasmRS("mtlo", rs);
}
};
public static final Instruction BEQ = new Instruction(70) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.npc = (processor.gpr[rs] == processor.gpr[rt]) ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("beq", rs, rt, imm16, address);
}
};
public static final Instruction BEQL = new Instruction(71) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                boolean c = (processor.gpr[rs] == processor.gpr[rt]);
                processor.npc = c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                if (c) {
                    processor.stepDelayslot();
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
                if (c) {
                    processor.stepDelayslot();
                }
                processor.reset_register_tracking();
                processor.current_bb.emit(loop ? "continue;\n" : "break;\n");
                processor.current_bb.freeze();
                processor.current_bb = null;
            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("beql", rs, rt, imm16, address);
}
};
public static final Instruction BGEZ = new Instruction(72) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgez", rs, imm16, address);
}
};
public static final Instruction BGEZAL = new Instruction(73) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgezal", rs, imm16, address);
}
};
public static final Instruction BGEZALL = new Instruction(74) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgezall", rs, imm16, address);
}
};
public static final Instruction BGEZL = new Instruction(75) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgezl", rs, imm16, address);
}
};
public static final Instruction BGTZ = new Instruction(76) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgtz", rs, imm16, address);
}
};
public static final Instruction BGTZL = new Instruction(77) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bgtzl", rs, imm16, address);
}
};
public static final Instruction BLEZ = new Instruction(78) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("blez", rs, imm16, address);
}
};
public static final Instruction BLEZL = new Instruction(79) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("blezl", rs, imm16, address);
}
};
public static final Instruction BLTZ = new Instruction(80) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltz", rs, imm16, address);
}
};
public static final Instruction BLTZAL = new Instruction(81) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltzal", rs, imm16, address);
}
};
public static final Instruction BLTZALL = new Instruction(82) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltzall", rs, imm16, address);
}
};
public static final Instruction BLTZL = new Instruction(83) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rs = (insn>>21)&31;

return Common.disasmRSOFFSET("bltzl", rs, imm16, address);
}
};
public static final Instruction BNE = new Instruction(84) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                processor.npc = (processor.gpr[rs] != processor.gpr[rt]) ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


                boolean c = (processor.gpr[rs] != processor.gpr[rt]);
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
                        " != " +
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("bne", rs, rt, imm16, address);
}
};
public static final Instruction BNEL = new Instruction(85) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRSRTOFFSET("bnel", rs, rt, imm16, address);
}
};
public static final Instruction J = new Instruction(86) {
@Override
public void interpret(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;


            
}
@Override
public String disasm(int address, int insn) {
	int imm26 = (insn>>0)&67108863;

return Common.disasmJUMP("j", imm26, address);
}
};
public static final Instruction JAL = new Instruction(87) {
@Override
public void interpret(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;


                CpuState cpu = processor.cpu;
                cpu.gpr[31] = processor.pc + 4;
                processor.npc = CpuState.jumpTarget(processor.pc, imm26);
                processor.stepDelayslot();
            
}
@Override
public void compile(Processor processor, int insn) {
	int imm26 = (insn>>0)&67108863;


            
}
@Override
public String disasm(int address, int insn) {
	int imm26 = (insn>>0)&67108863;

return Common.disasmJUMP("jal", imm26, address);
}
};
public static final Instruction JALR = new Instruction(88) {
@Override
public void interpret(Processor processor, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;


                CpuState cpu = processor.cpu;
                if (rd != 0) {
                    cpu.gpr[rd] = processor.pc + 4;
                }
                processor.npc = cpu.gpr[rs];
                processor.stepDelayslot();
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rd = (insn>>11)&31;
	int rs = (insn>>21)&31;

return Common.disasmRDRS("jalr", rd, rs);
}
};
public static final Instruction JR = new Instruction(89) {
@Override
public void interpret(Processor processor, int insn) {
	int rs = (insn>>21)&31;


                CpuState cpu = processor.cpu;
                processor.npc = cpu.gpr[rs];
                processor.stepDelayslot();
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int rs = (insn>>21)&31;

return Common.disasmRS("jr", rs);
}
};
public static final Instruction BC1F = new Instruction(90) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                processor.npc = !processor.fcr31_c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1f", imm16, address);
}
};
public static final Instruction BC1T = new Instruction(91) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                processor.npc = processor.fcr31_c ? Processor.branchTarget(processor.pc, Processor.signExtend(imm16)) : (processor.pc + 4);
                processor.stepDelayslot();
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1t", imm16, address);
}
};
public static final Instruction BC1FL = new Instruction(92) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                if (!processor.fcr31_c) {
                    processor.npc = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                    processor.stepDelayslot();
                } else {
                    processor.pc += 4;
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1fl", imm16, address);
}
};
public static final Instruction BC1TL = new Instruction(93) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;


                if (processor.fcr31_c) {
                    processor.npc = Processor.branchTarget(processor.pc, Processor.signExtend(imm16));
                    processor.stepDelayslot();
                } else {
                    processor.pc += 4;
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;

return Common.disasmOFFSET("bc1tl", imm16, address);
}
};
public static final Instruction BVF = new Instruction(94) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

return "Unimplemented BVF";
}
};
public static final Instruction BVT = new Instruction(95) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

return "Unimplemented BVF";
}
};
public static final Instruction BVFL = new Instruction(96) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

return "Unimplemented BVF";
}
};
public static final Instruction BVTL = new Instruction(97) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int imm3 = (insn>>18)&7;

return "Unimplemented BVF";
}
};
public static final Instruction LB = new Instruction(98) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lb", rt, rs, imm16);
}
};
public static final Instruction LBU = new Instruction(99) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lbu", rt, rs, imm16);
}
};
public static final Instruction LH = new Instruction(100) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lh", rt, rs, imm16);
}
};
public static final Instruction LHU = new Instruction(101) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lhu", rt, rs, imm16);
}
};
public static final Instruction LW = new Instruction(102) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lw", rt, rs, imm16);
}
};
public static final Instruction LWL = new Instruction(103) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lwl", rt, rs, imm16);
}
};
public static final Instruction LWR = new Instruction(104) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("lwr", rt, rs, imm16);
}
};
public static final Instruction SB = new Instruction(105) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sb", rt, rs, imm16);
}
};
public static final Instruction SH = new Instruction(106) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sh", rt, rs, imm16);
}
};
public static final Instruction SW = new Instruction(107) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sw", rt, rs, imm16);
}
};
public static final Instruction SWL = new Instruction(108) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("swl", rt, rs, imm16);
}
};
public static final Instruction SWR = new Instruction(109) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("swr", rt, rs, imm16);
}
};
public static final Instruction LL = new Instruction(110) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("ll", rt, rs, imm16);
}
};
public static final Instruction LWC1 = new Instruction(111) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmFTIMMRS("lwc1", ft, rs, imm16);
}
};
public static final Instruction LVS = new Instruction(112) {
@Override
public void interpret(Processor processor, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lv", 1, (vt5|(vt2<<5)), rs, (imm14 << 2));
}
};
public static final Instruction LVLQ = new Instruction(113) {
@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lvl", 4, (vt5|(vt1<<5)), rs, (imm14 << 2));
}
};
public static final Instruction LVRQ = new Instruction(114) {
@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lvr", 4, (vt5|(vt1<<5)), rs, (imm14 << 2));
}
};
public static final Instruction LVQ = new Instruction(115) {
@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lv", 4, (vt5|(vt1<<5)), rs, (imm14 << 2));
}
};
public static final Instruction SC = new Instruction(116) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int rt = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmRTIMMRS("sc", rt, rs, imm16);
}
};
public static final Instruction SWC1 = new Instruction(117) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int ft = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmFTIMMRS("swc1", ft, rs, imm16);
}
};
public static final Instruction SVS = new Instruction(118) {
@Override
public void interpret(Processor processor, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt2 = (insn>>0)&3;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("sv", 1, (vt5|(vt2<<5)), rs, (imm14 << 2));
}
};
public static final Instruction SVLQ = new Instruction(119) {
@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("svl", 4, (vt5|(vt1<<5)), rs, (imm14 << 2));
}
};
public static final Instruction SVRQ = new Instruction(120) {
@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("svr", 4, (vt5|(vt1<<5)), rs, (imm14 << 2));
}
};
public static final Instruction SVQ = new Instruction(121) {
@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("lv", 4, (vt5|(vt1<<5)), rs, (imm14 << 2));
}
};
public static final Instruction SWB = new Instruction(122) {
@Override
public void interpret(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vt1 = (insn>>0)&1;
	int imm14 = (insn>>2)&16383;
	int vt5 = (insn>>16)&31;
	int rs = (insn>>21)&31;

return Common.disasmVTIMMRS("swb", 4, (vt5|(vt1<<5)), rs, (imm14 << 2));
}
};
public static final Instruction ADD_S = new Instruction(123) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = cpu.fpr[fs] + cpu.fpr[ft];
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("add.s", fd, fs, ft);
}
};
public static final Instruction SUB_S = new Instruction(124) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = cpu.fpr[fs] + (0.0f - cpu.fpr[ft]);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("sub.s", fd, fs, ft);
}
};
public static final Instruction MUL_S = new Instruction(125) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = cpu.fpr[fs] * cpu.fpr[ft];
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("mul.s", fd, fs, ft);
}
};
public static final Instruction DIV_S = new Instruction(126) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = cpu.fpr[fs] / cpu.fpr[ft];
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmFDFSFT("div.s", fd, fs, ft);
}
};
public static final Instruction SQRT_S = new Instruction(127) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = (float) Math.sqrt(cpu.fpr[fs]);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("sqrt.s", fd, fs);
}
};
public static final Instruction ABS_S = new Instruction(128) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = Math.abs(cpu.fpr[fs]);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("abs.s", fd, fs);
}
};
public static final Instruction MOV_S = new Instruction(129) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = cpu.fpr[fs];
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("mov.s", fd, fs);
}
};
public static final Instruction NEG_S = new Instruction(130) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = 0.0f - cpu.fpr[fs];
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("neg.s", fd, fs);
}
};
public static final Instruction ROUND_W_S = new Instruction(131) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = Float.intBitsToFloat(Math.round(cpu.fpr[fs]));
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("round.w.s", fd, fs);
}
};
public static final Instruction TRUNC_W_S = new Instruction(132) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = Float.intBitsToFloat((int) (cpu.fpr[fs]));
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("trunc.w.s", fd, fs);
}
};
public static final Instruction CEIL_W_S = new Instruction(133) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = Float.intBitsToFloat((int) Math.ceil(cpu.fpr[fs]));
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("ceil.w.s", fd, fs);
}
};
public static final Instruction FLOOR_W_S = new Instruction(134) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = Float.intBitsToFloat((int) Math.floor(cpu.fpr[fs]));
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("floor.w.s", fd, fs);
}
};
public static final Instruction CVT_S_W = new Instruction(135) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fd] = (float) Float.floatToRawIntBits(cpu.fpr[fs]);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("cvt.s.w", fd, fs);
}
};
public static final Instruction CVT_W_S = new Instruction(136) {
@Override
public void interpret(Processor processor, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;


                CpuState cpu = processor.cpu;
                switch (cpu.fcr31.rm) {
                    case 1:
                        cpu.fpr[fd] = Float.intBitsToFloat((int) (cpu.fpr[fs]));
                        break;
                    case 2:
                        cpu.fpr[fd] = Float.intBitsToFloat((int) Math.ceil(cpu.fpr[fs]));
                        break;
                    case 3:
                        cpu.fpr[fd] = Float.intBitsToFloat((int) Math.floor(cpu.fpr[fs]));
                        break;
                    default:
                        cpu.fpr[fd] = Float.intBitsToFloat((int) Math.rint(cpu.fpr[fs]));
                        break;
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fd = (insn>>6)&31;
	int fs = (insn>>11)&31;

return Common.disasmFDFS("cvt.w.s", fd, fs);
}
};
public static final Instruction C_COND_S = new Instruction(137) {
@Override
public void interpret(Processor processor, int insn) {
	int fcond = (insn>>0)&15;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                float x = cpu.fpr[fs];
                float y = cpu.fpr[ft];
                boolean unordered = ((fcond & 1) != 0) && (Float.isNaN(x) || Float.isNaN(y));

                if (unordered) {
                    // we ignore float exception as it is useless for games
                    //if ((fcond & 8) != 0) {
                    //}

                    cpu.fcr31.c = true;
                } else {
                    boolean equal = ((fcond & 2) != 0) && (x == y);
                    boolean less = ((fcond & 4) != 0) && (x < y);

                    cpu.fcr31.c = less || equal;
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fcond = (insn>>0)&15;
	int fs = (insn>>11)&31;
	int ft = (insn>>16)&31;

return Common.disasmCcondS(fcond, fs, ft);
}
};
public static final Instruction MFC1 = new Instruction(138) {
@Override
public void interpret(Processor processor, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                if (rt != 0) {
                    cpu.gpr[rt] = Float.floatToRawIntBits(cpu.fpr[fs]);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFS("mfc1", rt, fs);
}
};
public static final Instruction CFC1 = new Instruction(139) {
@Override
public void interpret(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                if (rt != 0) {
                    switch (c1cr) {
                    case 0:                        
                        cpu.gpr[rt] = (FpuState.Fcr0.imp << 8) | (FpuState.Fcr0.rev);
                        break;

                    case 31:
                        cpu.gpr[rt] = (cpu.fcr31.fs ? (1 << 24) : 0) | (cpu.fcr31.c ? (1 << 23) : 0) | (cpu.fcr31.rm & 3);
                        break;
                    }
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFC("cfc1", rt, c1cr);
}
};
public static final Instruction MTC1 = new Instruction(140) {
@Override
public void interpret(Processor processor, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                cpu.fpr[fs] = Float.intBitsToFloat(cpu.gpr[rt]);
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int fs = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFS("mtc1", rt, fs);
}
};
public static final Instruction CTC1 = new Instruction(141) {
@Override
public void interpret(Processor processor, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


                CpuState cpu = processor.cpu;
                if (c1cr == 31) {
                    int bits = cpu.gpr[rt] & 0x01800003;
                    cpu.fcr31.rm = bits & 3;
                    bits >>= 23;
                    cpu.fcr31.fs = (bits > 1);
                    cpu.fcr31.c = (bits >> 1) == 1;
                }
            
}
@Override
public void compile(Processor processor, int insn) {
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
@Override
public String disasm(int address, int insn) {
	int c1cr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return Common.disasmRTFC("ctc1", rt, c1cr);
}
};
public static final Instruction MFC0 = new Instruction(142) {
@Override
public void interpret(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return "Unimplemented MFC0";
}
};
public static final Instruction CFC0 = new Instruction(143) {
@Override
public void interpret(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return "Unimplemented CFC0";
}
};
public static final Instruction MTC0 = new Instruction(144) {
@Override
public void interpret(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int c0dr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return "Unimplemented MTC0";
}
};
public static final Instruction CTC0 = new Instruction(145) {
@Override
public void interpret(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int c0cr = (insn>>11)&31;
	int rt = (insn>>16)&31;

return "Unimplemented CTC0";
}
};
public static final Instruction VADD = new Instruction(146) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] += y[i];
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VSUB = new Instruction(147) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] += (0.0f - y[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VSBN = new Instruction(148) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                if (one + two == 0) {
                    CpuState cpu = processor.cpu;
                    float[] x = cpu.loadVs(1, vs);
                    float[] y = cpu.loadVt(1, vt);
                    x[0] = Math.scalb(x[0], Float.floatToRawIntBits(y[0]));
                    cpu.saveVd(1, vd, x);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VDIV = new Instruction(149) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] /= y[i];
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VMUL = new Instruction(150) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] *= y[i];
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VDOT = new Instruction(151) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                if ((one | two) == 1) {
                    CpuState cpu = processor.cpu;
                    int vsize = 1 + one + (two<<1);               
                    float[] x = cpu.loadVs(vsize, vs);
                    float[] y = cpu.loadVt(vsize, vt);
                    float[] z = new float[1];
                    for (int i = 0; i < vsize; ++i) {
                        z[0] += x[i] * y[i];
                    }
                    cpu.saveVd(1, vd, z);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VSCL = new Instruction(152) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                if ((one | two) == 1) {
                    CpuState cpu = processor.cpu;
                    int vsize = 1 + one + (two<<1);               
                    float[] x = cpu.loadVs(vsize, vs);
                    float[] y = cpu.loadVt(1, vt);
                    for (int i = 0; i < vsize; ++i) {
                        x[i] *= y[0];
                    }
                    cpu.saveVd(vsize, vd, x);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VHDP = new Instruction(153) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                if ((one | two) == 1) {
                    CpuState cpu = processor.cpu;
                    int vsize = 1 + one + (two<<1);               
                    float[] x = cpu.loadVs(vsize - 1, vs);
                    float[] y = cpu.loadVt(vsize, vt);
                    float[] z = new float[1];
                    z[0] = y[vsize - 1];
                    for (int i = 0; i < vsize - 1; ++i) {
                        z[0] += x[i] * y[i];
                    }
                    cpu.saveVd(1, vd, z);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VDET = new Instruction(154) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                if ((one == 1) && (two == 0)) {
                    CpuState cpu = processor.cpu;
                    float[] x = cpu.loadVs(2, vs);
                    float[] y = cpu.loadVt(2, vt);
                    float[] z = new float[1];
                    z[0] = x[0] * y[1] - x[1] * y[0];
                    cpu.saveVd(1, vd, z);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VCRS = new Instruction(155) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                if ((one == 0) && (two == 1)) {
                    CpuState cpu = processor.cpu;
                    float[] x = cpu.loadVs(3, vs);
                    float[] y = cpu.loadVt(3, vt);
                    float[] z = new float[3];
                    z[0] = x[1] * y[2];
                    z[1] = x[2] * y[0];
                    z[2] = x[0] * y[1];
                    cpu.saveVd(3, vd, z);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction MFV = new Instruction(156) {
@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

return "Unimplemented MFV";
}
};
public static final Instruction MFVC = new Instruction(157) {
@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

return "Unimplemented MFVC";
}
};
public static final Instruction MTV = new Instruction(158) {
@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

return "Unimplemented MTV";
}
};
public static final Instruction MTVC = new Instruction(159) {
@Override
public void interpret(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int imm7 = (insn>>0)&127;
	int rt = (insn>>16)&31;

return "Unimplemented MTVC";
}
};
public static final Instruction VCMP = new Instruction(160) {
@Override
public void interpret(Processor processor, int insn) {
	int imm3 = (insn>>0)&7;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two << 1);

                boolean ccOr = false;
                boolean ccAnd = true;

                if ((imm3 & 8) == 0) {
                    boolean ccNot = ((imm3 & 4) == 4);

                    boolean cc = false;

                    float[] x = cpu.loadVs(vsize, vs);
                    float[] y = cpu.loadVt(vsize, vt);

                    for (int i = 0; i < vsize; ++i) {
                        switch (imm3 & 3) {
                            case 0:
                                cc = ccNot;
                                break;

                            case 1:
                                cc = ccNot ? (x[i] != y[i]) : (x[i] == y[i]);
                                break;

                            case 2:
                                cc = ccNot ? (x[i] >= y[i]) : (x[i] < y[i]);
                                break;

                            case 3:
                                cc = ccNot ? (x[i] > y[i]) : (x[i] <= y[i]);
                                break;
                        }

                        cpu.vcr.cc[i] = cc;
                        ccOr = ccOr || cc;
                        ccAnd = ccAnd && cc;
                    }

                } else {
                    float[] x = cpu.loadVs(vsize, vs);

                    for (int i = 0; i < vsize; ++i) {
                        boolean cc;
                        if ((imm3 & 3) == 0) {
                            cc = ((imm3 & 4) == 0) ? (x[i] == 0.0f) : (x[i] != 0.0f);
                        } else {
                            cc = (((imm3 & 1) == 1) && Float.isNaN(x[i])) ||
                                 (((imm3 & 2) == 2) && Float.isInfinite(x[i]));
                            if ((imm3 & 4) == 4) {
                                cc = !cc;
                            }
                        }
                        cpu.vcr.cc[i] = cc;
                        ccOr = ccOr || cc;
                        ccAnd = ccAnd && cc;
                    }
                }
                cpu.vcr.cc[4] = ccOr;
                cpu.vcr.cc[5] = ccAnd;
            
}
@Override
public void compile(Processor processor, int insn) {
	int imm3 = (insn>>0)&7;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int imm3 = (insn>>0)&7;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return "Unimplemented VCMP";
}
};
public static final Instruction VMIN = new Instruction(161) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = Math.min(x[i], y[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VMAX = new Instruction(162) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = Math.max(x[i], y[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VSCMP = new Instruction(163) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = Math.signum(x[i] + (0.0f - y[i]));
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VSGE = new Instruction(164) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (x[i] >= y[i]) ? 1.0f : 0.0f;
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VSLT = new Instruction(165) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                float[] y = cpu.loadVt(vsize, vt);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (x[i] < y[i]) ? 1.0f : 0.0f;
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
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
public static final Instruction VMOV = new Instruction(166) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VABS = new Instruction(167) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = Math.abs(x[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VNEG = new Instruction(168) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = 0.0f - x[i];
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VIDT = new Instruction(169) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                if (one == 1) {
                    CpuState cpu = processor.cpu;
                    int vsize = 1 + one + (two<<1);               
                    float[] x = new float[vsize];
                    int id = vd & 3;
                    for (int i = 0; i < vsize; ++i) {
                        if (id == i) {
                            x[i] =  1.0f;
                        }
                    }
                    cpu.saveVd(vsize, vd, x);
                }
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vidt", 1+one+(two<<1), vd);
}
};
public static final Instruction VSAT0 = new Instruction(170) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = Math.min(Math.max(0.0f, x[i]), 1.0f);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VSAT1 = new Instruction(171) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = Math.min(Math.max(-1.0f, x[i]), 1.0f);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VZERO = new Instruction(172) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                cpu.saveVd(vsize, vd, new float[vsize]);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vzero", 1+one+(two<<1), vd);
}
};
public static final Instruction VONE = new Instruction(173) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);
                float[] x = new float[vsize];
                for (int i = 0; i < vsize; ++i) {
                    x[i] = 1.0f;
                }
                cpu.saveVd(vsize, vd, new float[vsize]);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vone", 1+one+(two<<1), vd);
}
};
public static final Instruction VRCP = new Instruction(174) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = 1.0f / x[i];
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VRSQ = new Instruction(175) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)(1.0 / Math.sqrt(x[i]));
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VSIN = new Instruction(176) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)Math.sin(2.0 * Math.PI * x[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VCOS = new Instruction(177) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)Math.cos(2.0 * Math.PI * x[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VEXP2 = new Instruction(178) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)Math.pow(2.0, x[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VLOG2 = new Instruction(179) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)(Math.log(x[i]) / Math.log(2.0));
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VSQRT = new Instruction(180) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)(Math.sqrt(x[i]));
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VASIN = new Instruction(181) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)(Math.asin(x[i]) * 0.5 / Math.PI);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VNRCP = new Instruction(182) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = 0.0f - (1.0f / x[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VNSIN = new Instruction(183) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = 0.0f - (float)Math.sin(2.0 * Math.PI * x[i]);
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VREXP2 = new Instruction(184) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


                CpuState cpu = processor.cpu;
                int vsize = 1 + one + (two<<1);               
                float[] x = cpu.loadVs(vsize, vs);
                for (int i = 0; i < vsize; ++i) {
                    x[i] = (float)(1.0 / Math.pow(2.0, x[i]));
                }
                cpu.saveVd(vsize, vd, x);
            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VRNDS = new Instruction(185) {
@Override
public void interpret(Processor processor, int insn) {
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return Common.disasmVS("vrnds", 1+one+(two<<1), vs);
}
};
public static final Instruction VRNDI = new Instruction(186) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vrndi", 1+one+(two<<1), vd);
}
};
public static final Instruction VRNDF1 = new Instruction(187) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vrndf1", 1+one+(two<<1), vd);
}
};
public static final Instruction VRNDF2 = new Instruction(188) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return Common.disasmVD("vrndf2", 1+one+(two<<1), vd);
}
};
public static final Instruction VF2H = new Instruction(189) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VF2H";
}
};
public static final Instruction VH2F = new Instruction(190) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VH2F";
}
};
public static final Instruction VSBZ = new Instruction(191) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VSBZ";
}
};
public static final Instruction VLGB = new Instruction(192) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VUC2I = new Instruction(193) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VUC2I";
}
};
public static final Instruction VC2I = new Instruction(194) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VC2I";
}
};
public static final Instruction VUS2I = new Instruction(195) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VUS2I";
}
};
public static final Instruction VS2I = new Instruction(196) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VS2I";
}
};
public static final Instruction VI2UC = new Instruction(197) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VI2UC";
}
};
public static final Instruction VI2C = new Instruction(198) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VI2C";
}
};
public static final Instruction VI2US = new Instruction(199) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VI2US";
}
};
public static final Instruction VI2S = new Instruction(200) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VI2S";
}
};
public static final Instruction VSRT1 = new Instruction(201) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VSRT2 = new Instruction(202) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VBFY1 = new Instruction(203) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VBFY1";
}
};
public static final Instruction VBFY2 = new Instruction(204) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VBFY2";
}
};
public static final Instruction VOCP = new Instruction(205) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VSOCP = new Instruction(206) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VSOCP";
}
};
public static final Instruction VFAD = new Instruction(207) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VAVG = new Instruction(208) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VSRT3 = new Instruction(209) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VSRT4 = new Instruction(210) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
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
public static final Instruction VMFVC = new Instruction(211) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int imm8 = (insn>>8)&255;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int imm8 = (insn>>8)&255;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int imm8 = (insn>>8)&255;

return "Unimplemented VMFVC";
}
};
public static final Instruction VMTVC = new Instruction(212) {
@Override
public void interpret(Processor processor, int insn) {
	int imm8 = (insn>>0)&255;
	int vs = (insn>>8)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm8 = (insn>>0)&255;
	int vs = (insn>>8)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int imm8 = (insn>>0)&255;
	int vs = (insn>>8)&127;

return "Unimplemented VMTVC";
}
};
public static final Instruction VT4444 = new Instruction(213) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VT4444";
}
};
public static final Instruction VT5551 = new Instruction(214) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VT5551";
}
};
public static final Instruction VT5650 = new Instruction(215) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VT5650";
}
};
public static final Instruction VCST = new Instruction(216) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return "Unimplemented VCST";
}
};
public static final Instruction VF2IN = new Instruction(217) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VF2IN";
}
};
public static final Instruction VF2IZ = new Instruction(218) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VF2IZ";
}
};
public static final Instruction VF2IU = new Instruction(219) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VF2IU";
}
};
public static final Instruction VF2ID = new Instruction(220) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VF2ID";
}
};
public static final Instruction VI2F = new Instruction(221) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VI2F";
}
};
public static final Instruction VCMOVT = new Instruction(222) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;

return "Unimplemented VCMOVT";
}
};
public static final Instruction VCMOVF = new Instruction(223) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm3 = (insn>>16)&7;

return "Unimplemented VCMOVF";
}
};
public static final Instruction VWBN = new Instruction(224) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm8 = (insn>>16)&255;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm8 = (insn>>16)&255;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm8 = (insn>>16)&255;

return "Unimplemented VWBN";
}
};
public static final Instruction VPFXS = new Instruction(225) {
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


            
}
@Override
public void compile(Processor processor, int insn) {
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

return "Unimplemented VPFXS";
}
};
public static final Instruction VPFXT = new Instruction(226) {
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


            
}
@Override
public void compile(Processor processor, int insn) {
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

return "Unimplemented VPFXT";
}
};
public static final Instruction VPFXD = new Instruction(227) {
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


            
}
@Override
public void compile(Processor processor, int insn) {
	int satx = (insn>>0)&3;
	int saty = (insn>>2)&3;
	int satz = (insn>>4)&3;
	int satw = (insn>>6)&3;
	int mskx = (insn>>8)&1;
	int msky = (insn>>9)&1;
	int mskz = (insn>>10)&1;
	int mskw = (insn>>11)&1;


            
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

return "Unimplemented VPFXD";
}
};
public static final Instruction VIIM = new Instruction(228) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;

return "Unimplemented VIIM";
}
};
public static final Instruction VFIM = new Instruction(229) {
@Override
public void interpret(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int imm16 = (insn>>0)&65535;
	int vd = (insn>>16)&127;

return "Unimplemented VFIM";
}
};
public static final Instruction VMMUL = new Instruction(230) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return "Unimplemented VMMUL";
}
};
public static final Instruction VHTFM2 = new Instruction(231) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return "Unimplemented VHTFM2";
}
};
public static final Instruction VTFM2 = new Instruction(232) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return "Unimplemented VTFM2";
}
};
public static final Instruction VHTFM3 = new Instruction(233) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return "Unimplemented VHTFM3";
}
};
public static final Instruction VTFM3 = new Instruction(234) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return "Unimplemented VTFM3";
}
};
public static final Instruction VHTFM4 = new Instruction(235) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return "Unimplemented VHTFM4";
}
};
public static final Instruction VTFM4 = new Instruction(236) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return "Unimplemented VTFM4";
}
};
public static final Instruction VMSCL = new Instruction(237) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int vt = (insn>>16)&127;

return "Unimplemented VMSCL";
}
};
public static final Instruction VQMUL = new Instruction(238) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int vs = (insn>>8)&127;
	int vt = (insn>>16)&127;

return Common.disasmVDVSVT("VQMUL", 4, vd, vs, vt);
}
};
public static final Instruction VMMOV = new Instruction(239) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;

return "Unimplemented VMMOV";
}
};
public static final Instruction VMIDT = new Instruction(240) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return "Unimplemented VMIDT";
}
};
public static final Instruction VMZERO = new Instruction(241) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return "Unimplemented VMZERO";
}
};
public static final Instruction VMONE = new Instruction(242) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int two = (insn>>15)&1;

return "Unimplemented VMONE";
}
};
public static final Instruction VROT = new Instruction(243) {
@Override
public void interpret(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


            
}
@Override
public void compile(Processor processor, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;


            
}
@Override
public String disasm(int address, int insn) {
	int vd = (insn>>0)&127;
	int one = (insn>>7)&1;
	int vs = (insn>>8)&127;
	int two = (insn>>15)&1;
	int imm5 = (insn>>16)&31;

return "Unimplemented VROT";
}
};
public static final Instruction VNOP = new Instruction(244) {
@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(Processor processor, int insn) {


            
}
@Override
public String disasm(int address, int insn) {

return "vnop";
}
};
public static final Instruction VFLUSH = new Instruction(245) {
@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(Processor processor, int insn) {


            
}
@Override
public String disasm(int address, int insn) {

return "vflush";
}
};
public static final Instruction VSYNC = new Instruction(246) {
@Override
public void interpret(Processor processor, int insn) {


            
}
@Override
public void compile(Processor processor, int insn) {


            
}
@Override
public String disasm(int address, int insn) {

return "vsync";
}
};
}
