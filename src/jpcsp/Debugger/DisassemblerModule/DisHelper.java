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
        "$f0",  "$f1",  "$f2",  "$f3",  "$f4",  "$f5",  "$f6",  "$f7",
        "$f8",  "$f9",  "$f10", "$f11", "$f12", "$f13", "$f14", "$f15",
        "$f16", "$f17", "$f18", "$f19", "$f20", "$f21", "$f22", "$f23",
        "$f24", "$f25", "$f26", "$f27", "$f28", "$f29", "$f30", "$f31"
    };
    
    static String[] cop0Names = {
        "Index", "Random", "EntryLo0", "EntryLo1", "Context", "PageMask", "Wired", "cop0reg7",
        "BadVaddr", "Count", "EntryHi", "Compare", "Status", "Cause", "EPC", "PrID",
        "Config", "LLAddr", "WatchLo", "WatchHi", "XContext", "cop0reg21", "cop0reg22", "cop0reg23",
        "cop0reg24", "EBase", "ECC", "CacheErr", "TagLo", "TagHi", "ErrorPC", "cop0reg31"
    };

    public static String Dis_RDRSRT(String opname, int value) {

        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        
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

    public static String Dis_BASEFTOFFSET(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        int ft = (value >> 16) & 0x1f;
        int offset = ((value & 0xffff) << 16) >> 16;
        if (opname.matches("swc1")) {
            return opname + " " + fprNames[ft] + ", " + Integer.toString(offset) + "(" + gprNames[rs] + ")";
        }
        if (opname.matches("lwc1")) {
            return opname + " " + fprNames[ft] + ", " + Integer.toString(offset) + "(" + gprNames[rs] + ")";
        }
        return "Opcode 0x" + opname + "unhandled in Dis_BASEFTOFFSET";
    }

    public static String Dis_RTRSIMM(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int imm = value & 0xffff;
        
        if (!opname.equals("andi") && !opname.equals("ori") && !opname.equals("xori")) {
            imm = (imm << 16) >> 16;
        }
        
        if (rs == 0) {
            
            if (opname.equals("andi")) {
                return "li " + gprNames[rt] + ", 0";
            } else if (opname.matches("slti")) {
                return "li " + gprNames[rt] + ", " + imm;
            }

        }
        
        return opname + " " + gprNames[rt] + ", " + gprNames[rs] + ", " + imm;
    }

    public static String Dis_RDRTSA(String opname, int value) {
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int sa = (value >> 6) & 0x1f;

        if (rd == 0) {
            return "nop";
        } else {
            return opname + " " + gprNames[rd] + ", " + gprNames[rt] + ", " + sa;
        }
    }

    public static String Dis_RD(String opname, int value) {
        int rd = (value >> 11) & 0x1f;
        
        return opname + " " + gprNames[rd];
    }

    public static String Dis_RS(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        
        return opname + " " + gprNames[rs];
    }

    public static String Dis_RDRT(String opname, int value) {
        int rd = (value >> 11) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        
        return opname + " " + gprNames[rd] + ", " + gprNames[rt];
    }

    public static String Dis_RSRT(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        
        return opname + " " + gprNames[rs] + ", " + gprNames[rt];
    }

    public static String Dis_RTIMMRS(String opname, int value) {
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int imm = ((value & 0xffff) << 16) >> 16;
        
        return opname + " " + gprNames[rt] + ", " + imm + " (" + gprNames[rs] + ")";
    }
}
