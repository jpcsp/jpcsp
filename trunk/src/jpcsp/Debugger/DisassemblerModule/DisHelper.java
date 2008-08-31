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

    public static int signExtend(int value) {
        return (value << 16) >> 16;
    }

    public static int signExtend8(int value) {
        return (value << 24) >> 24;
    }

    public static int zeroExtend(int value) {
        return (value & 0xffff);
    }

    public static int zeroExtend8(int value) {
        return (value & 0xff);
    }
    
    public static String[] gprNames = {
        "$zr", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3",
        "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7",
        "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
        "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
    };
    public static String[] fprNames = {
        "$f0", "$f1", "$f2", "$f3", "$f4", "$f5", "$f6", "$f7",
        "$f8", "$f9", "$f10", "$f11", "$f12", "$f13", "$f14", "$f15",
        "$f16", "$f17", "$f18", "$f19", "$f20", "$f21", "$f22", "$f23",
        "$f24", "$f25", "$f26", "$f27", "$f28", "$f29", "$f30", "$f31"
    };
    public static String[][] vprNames = {
        {
            "S000.s", "S010.s", "S020.s", "S030.s",
            "S100.s", "S110.s", "S120.s", "S130.s",
            "S200.s", "S210.s", "S220.s", "S230.s",
            "S300.s", "S310.s", "S320.s", "S330.s",
            "S400.s", "S410.s", "S420.s", "S430.s",
            "S500.s", "S510.s", "S520.s", "S530.s",
            "S600.s", "S610.s", "S620.s", "S630.s",
            "S700.s", "S710.s", "S720.s", "S730.s",
            "S001.s", "S011.s", "S021.s", "S031.s",
            "S101.s", "S111.s", "S121.s", "S131.s",
            "S201.s", "S211.s", "S221.s", "S231.s",
            "S301.s", "S311.s", "S321.s", "S331.s",
            "S401.s", "S411.s", "S421.s", "S431.s",
            "S501.s", "S511.s", "S521.s", "S531.s",
            "S601.s", "S611.s", "S621.s", "S631.s",
            "S701.s", "S711.s", "S721.s", "S731.s",
            "S002.s", "S012.s", "S022.s", "S032.s",
            "S102.s", "S112.s", "S122.s", "S132.s",
            "S202.s", "S212.s", "S222.s", "S232.s",
            "S302.s", "S312.s", "S322.s", "S332.s",
            "S402.s", "S412.s", "S422.s", "S432.s",
            "S502.s", "S512.s", "S522.s", "S532.s",
            "S602.s", "S612.s", "S622.s", "S632.s",
            "S702.s", "S712.s", "S722.s", "S732.s",
            "S003.s", "S013.s", "S023.s", "S033.s",
            "S103.s", "S113.s", "S123.s", "S133.s",
            "S203.s", "S213.s", "S223.s", "S233.s",
            "S303.s", "S313.s", "S323.s", "S333.s",
            "S403.s", "S413.s", "S423.s", "S433.s",
            "S503.s", "S513.s", "S523.s", "S533.s",
            "S603.s", "S613.s", "S623.s", "S633.s",
            "S703.s", "S713.s", "S723.s", "S733.s"
        },
        {
            "C000.p", "C010.p", "C020.p", "C030.p",
            "C100.p", "C110.p", "C120.p", "C130.p",
            "C200.p", "C210.p", "C220.p", "C230.p",
            "C300.p", "C310.p", "C320.p", "C330.p",
            "C400.p", "C410.p", "C420.p", "C430.p",
            "C500.p", "C510.p", "C520.p", "C530.p",
            "C600.p", "C610.p", "C620.p", "C630.p",
            "C700.p", "C710.p", "C720.p", "C730.p",
            "R000.p", "R001.p", "R002.p", "R003.p",
            "R100.p", "R101.p", "R102.p", "R103.p",
            "R200.p", "R201.p", "R202.p", "R203.p",
            "R300.p", "R301.p", "R302.p", "R303.p",
            "R400.p", "R401.p", "R402.p", "R403.p",
            "R500.p", "R501.p", "R502.p", "R503.p",
            "R600.p", "R601.p", "R602.p", "R603.p",
            "R700.p", "R701.p", "R702.p", "R703.p",
            "C002.p", "C012.p", "C022.p", "C032.p",
            "C102.p", "C112.p", "C122.p", "C132.p",
            "C202.p", "C212.p", "C222.p", "C232.p",
            "C302.p", "C312.p", "C322.p", "C332.p",
            "C402.p", "C412.p", "C422.p", "C432.p",
            "C502.p", "C512.p", "C522.p", "C532.p",
            "C602.p", "C612.p", "C622.p", "C632.p",
            "C702.p", "C712.p", "C722.p", "C732.p",
            "R020.p", "R021.p", "R022.p", "R023.p",
            "R120.p", "R121.p", "R122.p", "R123.p",
            "R220.p", "R221.p", "R222.p", "R223.p",
            "R320.p", "R321.p", "R322.p", "R323.p",
            "R420.p", "R421.p", "R422.p", "R423.p",
            "R520.p", "R521.p", "R522.p", "R523.p",
            "R620.p", "R621.p", "R622.p", "R623.p",
            "R720.p", "R721.p", "R722.p", "R723.p",
        }, {
            "C000.t", "C010.t", "C020.t", "C030.t",
            "C100.t", "C110.t", "C120.t", "C130.t",
            "C200.t", "C210.t", "C220.t", "C230.t",
            "C300.t", "C310.t", "C320.t", "C330.t",
            "C400.t", "C410.t", "C420.t", "C430.t",
            "C500.t", "C510.t", "C520.t", "C530.t",
            "C600.t", "C610.t", "C620.t", "C630.t",
            "C700.t", "C710.t", "C720.t", "C730.t",
            "R000.t", "R001.t", "R002.t", "R003.t",
            "R100.t", "R101.t", "R102.t", "R103.t",
            "R200.t", "R201.t", "R202.t", "R203.t",
            "R300.t", "R301.t", "R302.t", "R303.t",
            "R400.t", "R401.t", "R402.t", "R403.t",
            "R500.t", "R501.t", "R502.t", "R503.t",
            "R600.t", "R601.t", "R602.t", "R603.t",
            "R700.t", "R701.t", "R702.t", "R703.t",
            "C001.t", "C011.t", "C021.t", "C031.t",
            "C101.t", "C111.t", "C121.t", "C131.t",
            "C201.t", "C211.t", "C221.t", "C231.t",
            "C301.t", "C311.t", "C321.t", "C331.t",
            "C401.t", "C411.t", "C421.t", "C431.t",
            "C501.t", "C511.t", "C521.t", "C531.t",
            "C601.t", "C611.t", "C621.t", "C631.t",
            "C701.t", "C711.t", "C721.t", "C731.t",
            "R010.t", "R011.t", "R012.t", "R013.t",
            "R110.t", "R111.t", "R112.t", "R113.t",
            "R210.t", "R211.t", "R212.t", "R213.t",
            "R310.t", "R311.t", "R312.t", "R313.t",
            "R410.t", "R411.t", "R412.t", "R413.t",
            "R510.t", "R511.t", "R512.t", "R513.t",
            "R610.t", "R611.t", "R612.t", "R613.t",
            "R710.t", "R711.t", "R712.t", "R713.t",
        }
    };
    public static String[] fcrNames = {
        "$fcsr0", "$fcsr1", "$fcsr2", "$fcsr3", "$fcsr4", "$fcsr5", "$fcsr6", "$fcsr7",
        "$fcsr8", "$fcsr9", "$fcsr10", "$fcsr11", "$fcsr12", "$fcsr13", "$fcsr14", "$fcsr15",
        "$fcsr16", "$fcsr17", "$fcsr18", "$fcsr19", "$fcsr20", "$fcsr21", "$fcsr22", "$fcsr23",
        "$fcsr24", "$fcsr25", "$fcsr26", "$fcsr27", "$fcsr28", "$fcsr29", "$fcsr30", "$fcsr31"
    };
    public static String[] cop0Names = {
        "Index", "Random", "EntryLo0", "EntryLo1", "Context", "PageMask", "Wired", "cop0reg7",
        "BadVaddr", "Count", "EntryHi", "Compare", "Status", "Cause", "EPC", "PrID",
        "Config", "LLAddr", "WatchLo", "WatchHi", "XContext", "cop0reg21", "cop0reg22", "cop0reg23",
        "cop0reg24", "EBase", "ECC", "CacheErr", "TagLo", "TagHi", "ErrorPC", "cop0reg31"
    };
    //New helpers
    public static String Dis_RDRTSA(String opname, int rd, int rt, int sa) {
        if ((rd == 0) && sa == 0) {
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

    public static String Dis_RDRT(String opname, int rd, int rt) {
        return opname + " " + gprNames[rd] + ", " + gprNames[rt];
    }

    public static String Dis_RD(String opname, int rd) {
        return opname + " " + gprNames[rd];
    }

    public static String Dis_RSRT(String opname, int rs, int rt) {
        return opname + " " + gprNames[rs] + ", " + gprNames[rt];
    }

    public static String Dis_Ext(int rt, int rs, int rd, int sa) {
        return "ext " + gprNames[rt] + ", " + gprNames[rs] + ", " + sa + ", " + (rd + 1);
    }

    public static String Dis_Ins(int rt, int rs, int rd, int sa) {
        return "ins " + gprNames[rt] + ", " + gprNames[rs] + ", " + sa + ", " + (rd - sa + 1);
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
        return opname + " " + gprNames[rs] + ", 0x" + Integer.toHexString(simm16*4 + opcode_address + 4);
    }

    public static String Dis_RSRTOFFSET(String opname, int rs, int rt, int simm16, int opcode_address) {
        return opname + " " + gprNames[rs] + ", " + gprNames[rt] + ", 0x" + Integer.toHexString(simm16*4 + opcode_address + 4);
    }

    public static String Dis_OFFSET(String opname, int imm, int opcode_address) {
        return opname + " 0x" + Integer.toHexString(imm*4 + opcode_address + 4);
    }

    public static String Dis_RTRSIMM(String opname, int rt, int rs, int imm16) {

        /* if (!opname.equals("andi") && !opname.equals("ori") && !opname.equals("xori")) {
        imm = (imm << 16) >> 16;
        } NOT Needed??? */

        if (rs == 0) {

            if (opname.equals("andi")) {
                return "li " + gprNames[rt] + ", 0";
            } else if (opname.matches("slti")) {
                return "li " + gprNames[rt] + ", " + ((0 < imm16) ? 1 : 0);
            }

        }

        return opname + " " + gprNames[rt] + ", " + gprNames[rs] + ", " + imm16;
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

        // If we think the target is a stub try and append the syscall name
        if (opname.equals("jal") && jump != 0)
        {
            DisasmOpcodes disOp = new DisasmOpcodes();
            String secondTarget = disOp.disasm(jpcsp.Memory.get_instance().read32(jump+4), jump+4);
            if (secondTarget.startsWith("syscall") && !secondTarget.contains("[unknown]"))
                return opname + " 0x" + Integer.toHexString(jump) + " " + secondTarget.substring(13);
        }

        return opname + " 0x" + Integer.toHexString(jump);
    }

    public static String Dis_RTIMM(String opname, int rt, int imm) {
        return opname + " " + gprNames[rt] + ", " + imm;
    }

    public static String Dis_RTIMMRS(String opname, int rt, int rs, int imm) {
        return opname + " " + gprNames[rt] + ", " + imm + " (" + gprNames[rs] + ")";
    }

    public static String Dis_CCIMMRS(String opname, int code, int rs, int imm) {
        return opname + " 0x" + Integer.toHexString(code) + ", " + imm + " (" + gprNames[rs] + ")";
    }

    public static String Dis_FTIMMRS(String opname, int rt, int rs, int imm) {
        return opname + " " + fprNames[rt] + ", " + imm + " (" + gprNames[rs] + ")";
    }

    public static String Dis_FDFSFT(String opname, int fd, int fs, int ft) {
        return opname + " " + fprNames[fd] + ", " + fprNames[fs] + ", " + fprNames[ft];
    }

    public static String Dis_FDFS(String opname, int fd, int fs) {
        return opname + " " + fprNames[fd] + ", " + fprNames[fs];
    }

    public static String Dis_RTFS(String opname, int rt, int fs) {
        return opname + " " + gprNames[rt] + ", " + fprNames[fs];
    }

    public static String Dis_RTFC(String opname, int rt, int fc) {
        return opname + " " + gprNames[rt] + ", " + fcrNames[fc];
    }

    private static final String ccondsNames[] = {
        "c.f.s",
        "c.un.s",
        "c.eq.s",
        "c.ueq.s",
        "c.olt.s",
        "c.ult.s",
        "c.ole.s",
        "c.ule.s",
        "c.sf.s",
        "c.ngle.s",
        "c.seq.s",
        "c.ngl.s",
        "c.lt.s",
        "c.nge.s",
        "c.le.s",
        "c.ngt.s"
    };

    public static String Dis_Cconds(int cconds, int fs, int ft) {
        return ccondsNames[cconds] + " " + fprNames[fs] + ", " + fprNames[ft];
    }


    public static String Dis_FSFT(String opname, int fs, int ft) {
        return opname + " " + fprNames[fs] + ", " + fprNames[ft];
    }

    public static String Dis_VTIMMRS(String opname, int vsize, int vt, int rs, int imm) {
        return opname + " " + vprNames[vsize-1][vt] + ", " + imm + " (" + gprNames[rs] + ")";
    }

    public static String Dis_VDVSVT(String opname, int vsize, int vd, int vs, int vt) {
        return opname + " " + vprNames[vsize-1][vd] + ", " + vprNames[vsize-1][vs] + ", " + vprNames[vsize-1][vt];
    }

    public static String Dis_VDVSVT1(String opname, int vsize, int vd, int vs, int vt) {
        return opname + " " + vprNames[vsize-1][vd] + ", " + vprNames[vsize-1][vs] + ", " + vprNames[0][vt];
    }

    public static String Dis_VD1VSVT(String opname, int vsize, int vd, int vs, int vt) {
        return opname + " " + vprNames[0][vd] + ", " + vprNames[vsize-1][vs] + ", " + vprNames[vsize-1][vt];
    }
}
