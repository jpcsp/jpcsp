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

package jpcsp.Debugger.Disassembler;

/**
 *
 * @author shadow
 */
public class DisHelper {
    static String[] cpuregs = {
        "zr", "at", "v0", "v1", "a0", "a1", "a2", "a3",
        "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
        "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
        "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"
    };
   static String[] cop0regs = 
   {
	"cop0reg0", "cop0reg1", "cop0reg2", "cop0reg3", "cop0reg4", "cop0reg5", "cop0reg6", "cop0reg7", 
	"BadVaddr", "Count", "cop0reg10", "Compare", "Status", "Cause", "EPC", "PrID",
	"Config", "cop0reg17", "cop0reg18", "cop0reg19", "cop0reg20", "cop0reg21", "cop0reg22", "cop0reg23",
	"cop0reg24", "EBase", "cop0reg26", "cop0reg37", "TagLo", "TagHi", "ErrorPC", "cop0reg31"
    };
       public static String Dis_RDRSRT(String opname, int value) {

        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        if (rs == 0 && rt == 0) {
            return "li " + cpuregs[rd] + ", 0";
        } else if (rs == 0) {
            return "move " + cpuregs[rd] + ", " + cpuregs[rs];
        } else if (rt == 0) {
            return "move " + cpuregs[rd] + ", " + cpuregs[rs];
        } else {
            return opname + " " + cpuregs[rd] + ", " + cpuregs[rs] + ", " + cpuregs[rt];
        }

    }

    public static String Dis_BASEFTOFFSET(String opname, int value) {
        int base = (value >> 21) & 0x1f;
        int ft = (value >> 16) & 0x1f;
        int offset = value & 0xffff;
        if (opname.matches("swc1")) {
            return opname + " f" + Integer.toString(ft) + ", sp[" + Integer.toString(offset) + "]";
        }
        if (opname.matches("lwc1")) {
            return opname + " f" + Integer.toString(ft) + ", v1[" + Integer.toString(offset) + "]";
        }
        return "Opcode 0x" + opname + "unhandled in Dis_BASEFTOFFSET";
    }

    public static String Dis_RTRSIMM(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int imm = value & 0xffff;
        if (!opname.equals("andi") && !opname.equals("ori") && !opname.equals("xori") && (imm & 0x8000) == 0x8000) {
            imm |= 0xffff0000;
        }
        if (rs == 0) {
            return "li " + cpuregs[rt] + ", " + imm;
        } else {
            return opname + " " + cpuregs[rt] + ", " + cpuregs[rs] + ", " + imm;
        }
    }

    public static String Dis_RDRTSA(String opname, int value) {
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int sa = (value >> 6) & 0x1f;

        if (rd == 0) {
            return "nop";
        } else {
            return opname + " " + cpuregs[rd] + ", " + cpuregs[rt] + ", " + sa;
        }
    }

    public static String Dis_RD(String opname, int value) {
        int rd = (value >> 11) & 0x1f;
        return opname + " " + cpuregs[rd];

    }
    
    public static String Dis_RS(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        return opname + " " + cpuregs[rs];
    }
    public static String Dis_RDRT(String opname,int value)
    {
        int rd = (value >> 11) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        return opname + " " + cpuregs[rd] + ", " + cpuregs[rt];
    }
    public static String Dis_RSRT(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        return opname + " " + cpuregs[rs] + ", " + cpuregs[rt];
    }

    public static String Dis_RTIMMRS(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int imm = value & 0xffff;
        if ((imm & 0x8000) == 0x8000) {
            imm |= 0xffff0000;
        }
        return opname + " " + cpuregs[rt] + "," + imm + " (" + cpuregs[rs] + ")";
    }



}
