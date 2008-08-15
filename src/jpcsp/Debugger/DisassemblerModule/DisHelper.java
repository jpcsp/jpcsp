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
package jpcsp.Debugger.DisassemblerModule;

/**
 *
 * @author shadow, hlide
 */
public class DisHelper {

    static String[] gprNames = {
        "$zr", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
        "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
        "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
        "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
    };
    static String[] fprNames = {
        "$f0", "$f1", "$f2", "$f3", "$f4", "$f5", "$f6", "$f7",
        "$f8", "$f9", "$f10", "$f11", "$f12", "$f13", "$f14", "$f15",
        "$f16", "$f17", "$f18", "$f19", "$f20", "$f21", "$f22", "$f23",
        "$f24", "$f25", "$f26", "$f27", "$f28", "$f29", "$f30", "$f31"
    };
    static String[] fcrNames = {
        "$fcsr0", "$fcsr1", "$fcsr2", "$fcsr3", "$fcsr4", "$fcsr5", "$fcsr6", "$fcsr7",
        "$fcsr8", "$fcsr9", "$fcsr10", "$fcsr11", "$fcsr12", "$fcsr13", "$fcsr14", "$fcsr15",
        "$fcsr16", "$fcsr17", "$fcsr18", "$fcsr19", "$fcsr20", "$fcsr21", "$fcsr22", "$fcsr23",
        "$fcsr24", "$fcsr25", "$fcsr26", "$fcsr27", "$fcsr28", "$fcsr29", "$fcsr30", "$fcsr31"
    };
    static String[] cop0Names = {
        "Index", "Random", "EntryLo0", "EntryLo1", "Context", "PageMask", "Wired", "cop0reg7",
        "BadVaddr", "Count", "EntryHi", "Compare", "Status", "Cause", "EPC", "PrID",
        "Config", "LLAddr", "WatchLo", "WatchHi", "XContext", "cop0reg21", "cop0reg22", "cop0reg23",
        "cop0reg24", "EBase", "ECC", "CacheErr", "TagLo", "TagHi", "ErrorPC", "cop0reg31"
    };
    //New helpers
    public static String Dis_RDRTSA(String opname, int rd, int rt, int sa) {
        if (rd == 0) {
            return "nop";
        } else {
            return opname + " " + gprNames[rd] + ", " + gprNames[rt] + ", " + sa;
        }
    }

    public static String Dis_RDRTRS(String opname, int rd, int rt, int rs) {
        //TODO CHECK IF rt  rs  =0
        return opname + " " + gprNames[rd] + ", " + gprNames[rt] + ", " + gprNames[rs];
    }

    public static String Dis_RS(String opname, int rs) {
        return opname + " " + gprNames[rs];
    }

    public static String Dis_RT(String opname, int rt) {
        return opname + " " + gprNames[rt];
    }

    public static String Dis_RDRS(String opname, int rd, int rs) {
        return opname + " " + gprNames[rd] + ", " + gprNames[rs];
    }

    public static String Dis_RD(String opname, int rd) {
        return opname + " " + gprNames[rd];
    }

    public static String Dis_RSRT(String opname, int rs, int rt) {
        return opname + " " + gprNames[rs] + ", " + gprNames[rt];
    }

    public static String Dis_RDRSRT(String opname, int rd, int rs, int rt) {
        if (rs == 0 && rt == 0) {

            if (opname.equals("xor") || opname.equals("nor")) {
                return "li " + gprNames[rd] + ", -1";
            }

            return "li " + gprNames[rd] + ", 0";

        } else if (rs == 0) {

            if (opname.equals("and")) {
                return "li " + gprNames[rd] + ", 0";
            }

            if (opname.equals("nor")) {
                return "not " + gprNames[rd] + ", " + gprNames[rt];
            }

            if (opname.matches("sub")) {
                return "neg " + gprNames[rd] + ", " + gprNames[rt];
            }

            return "move " + gprNames[rd] + ", " + gprNames[rs];

        } else if (rt == 0) {

            if (opname.equals("and")) {
                return "li " + gprNames[rd] + ", 0";
            }

            if (opname.equals("nor")) {
                return "not " + gprNames[rd] + ", " + gprNames[rs];
            }

            return "move " + gprNames[rd] + ", " + gprNames[rs];

        }

        return opname + " " + gprNames[rd] + ", " + gprNames[rs] + ", " + gprNames[rt];
    }

    public static String Dis_RSOFFSET(String opname, int rs, int simm16, int opcode_address) {
        return opname + " " + gprNames[rs] + ", 0x" + Integer.toHexString(simm16 * 4 + opcode_address + 4);
    }

    public static String Dis_RSRTOFFSET(String opname, int rs, int rt, int simm16, int opcode_address) {
        return opname + " " + gprNames[rs] + ", " + gprNames[rt] + " 0x" + Integer.toHexString(simm16 * 4 + opcode_address + 4);
    }

    public static String Dis_OFFSET(String opname, int imm, int opcode_address) {
        return opname + " " + " 0x" + Integer.toHexString(imm * 4 + opcode_address + 4);
    }

    public static String Dis_RTRSIMM(String opname, int rt, int rs, int simm16) {

        /* if (!opname.equals("andi") && !opname.equals("ori") && !opname.equals("xori")) {
        imm = (imm << 16) >> 16;
        } NOT Needed??? */

        if (rs == 0) {

            if (opname.equals("andi")) {
                return "li " + gprNames[rt] + ", 0";
            } else if (opname.matches("slti")) {
                return "li " + gprNames[rt] + ", " + simm16;
            }

        }

        return opname + " " + gprNames[rt] + ", " + gprNames[rs] + ", " + simm16;
    }

    public static String Dis_Syscall(int code) {  /* probably okay */
        String s = new String();
        for (syscalls.calls c : syscalls.calls.values()) {
            if (c.getValue() == code) {
                s = "syscall " + Integer.toHexString(code) + "     " + c;
                return s;

            }
        }
        s = "syscall 0x" + Integer.toHexString(code) + " [unknown]";
        return s;
    }

    public static String Dis_Break(int code) {
        return "break 0x" + Integer.toHexString(code);
    }

    public static String Dis_JUMP(String opname, int uimm26, int opcode_address) {
        int jump = (opcode_address & 0xf0000000) | ((uimm26 & 0x3ffffff) << 2);
        return opname + " 0x" + Integer.toHexString(jump);
    }

    public static String Dis_RTIMM(String opname, int rt, int imm) {
        return opname + " " + gprNames[rt] + ", " + imm;
    }

    public static String Dis_RTIMMRS(String opname, int rt, int rs, int imm) {
        return opname + " " + gprNames[rt] + ", " + imm + " (" + gprNames[rs] + ")";
    }

    public static String Dis_FTIMMRS(String opname, int rt, int rs, int imm) {
        return opname + " " + fprNames[rt] + ", " + imm + " (" + gprNames[rs] + ")";
    }

    public static String Dis_RDRT(String opname, int rd, int rt) {
        return opname + " " + gprNames[rd] + ", " + gprNames[rt];
    }

    public static String Dis_FDFSFT(String opname, int fd, int fs, int ft) {
        return opname + " " + fprNames[fd] + ", " + fprNames[fs] + ", " + fprNames[ft];
    }

    public static String Dis_FDFS(String opname, int fd, int fs) {
        return opname + " " + fprNames[fd] + ", " + fprNames[fs];
    }

    public static String Dis_FSFT(String opname, int fs, int ft) {
        return opname + " " + fprNames[fs] + ", " + fprNames[ft];
    }
}
