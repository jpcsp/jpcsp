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
package jpcsp.Allegrex;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.compiler.ICompilerContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SyscallIgnore;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.util.Utilities;

/**
 *
 * @author hli
 */
public class Common {

    public static abstract class Instruction {

        private int m_count = 0;
        private int flags = 0;
        public final static int FLAG_INTERPRETED = (1 << 0);
        public final static int FLAG_CANNOT_BE_SPLIT = (1 << 1);
        public final static int FLAG_HAS_DELAY_SLOT = (1 << 2);
        public final static int FLAG_IS_BRANCHING = (1 << 3);
        public final static int FLAG_IS_JUMPING = (1 << 4);
        public final static int FLAG_IS_CONDITIONAL = (1 << 5);
        public final static int FLAG_STARTS_NEW_BLOCK = (1 << 6);
        public final static int FLAG_ENDS_BLOCK = (1 << 7);
        public final static int FLAG_USE_VFPU_PFXS = (1 << 8);
        public final static int FLAG_USE_VFPU_PFXT = (1 << 9);
        public final static int FLAG_USE_VFPU_PFXD = (1 << 10);
        public final static int FLAG_COMPILED_PFX = (1 << 11);
        public final static int FLAG_WRITES_RT = (1 << 12);
        public final static int FLAG_WRITES_RD = (1 << 13);
        public final static int FLAGS_BRANCH_INSTRUCTION = FLAG_CANNOT_BE_SPLIT | FLAG_HAS_DELAY_SLOT | FLAG_IS_BRANCHING | FLAG_IS_CONDITIONAL;
        public final static int FLAGS_LINK_INSTRUCTION = FLAG_HAS_DELAY_SLOT | FLAG_STARTS_NEW_BLOCK;

        public abstract void interpret(Processor processor, int insn);

        public void compile(ICompilerContext context, int insn) {
            flags |= FLAG_INTERPRETED;
            context.compileInterpreterInstruction();
        }

        public abstract String disasm(int address, int insn);

        public abstract String name();

        public abstract String category();

        public void resetCount() {
            m_count = 0;
        }

        public void increaseCount() {
            m_count++;
        }

        public int getCount() {
            return m_count;
        }

        public int count() {
            return m_count;
        }

        public Instruction instance(int insn) {
            return this;
        }

        private void setInstance(int index) {
            m_instances[index] = this;
        }

        public Instruction(int index) {
            setInstance(index);
        }

        public Instruction(int index, int flags) {
            setInstance(index);
            this.flags = flags;
        }

        public Instruction() {
        }

        public int getFlags() {
            return flags;
        }

        public boolean hasFlags(int testFlags) {
            return (flags & testFlags) == testFlags;
        }

        private void appendFlagString(StringBuilder result, String flagString) {
            if (result.length() > 0) {
                result.append(" | ");
            }
            result.append(flagString);
        }

        private String flagsToString() {
            StringBuilder result = new StringBuilder();
            if (hasFlags(FLAG_INTERPRETED)) {
                appendFlagString(result, "FLAG_INTERPRETED");
            }
            if (hasFlags(FLAG_CANNOT_BE_SPLIT)) {
                appendFlagString(result, "FLAG_CANNOT_BE_SPLIT");
            }
            if (hasFlags(FLAG_HAS_DELAY_SLOT)) {
                appendFlagString(result, "FLAG_HAS_DELAY_SLOT");
            }
            if (hasFlags(FLAG_IS_BRANCHING)) {
                appendFlagString(result, "FLAG_IS_BRANCHING");
            }
            if (hasFlags(FLAG_IS_JUMPING)) {
                appendFlagString(result, "FLAG_IS_JUMPING");
            }
            if (hasFlags(FLAG_IS_CONDITIONAL)) {
                appendFlagString(result, "FLAG_IS_CONDITIONAL");
            }
            if (hasFlags(FLAG_STARTS_NEW_BLOCK)) {
                appendFlagString(result, "FLAG_STARTS_NEW_BLOCK");
            }
            if (hasFlags(FLAG_ENDS_BLOCK)) {
                appendFlagString(result, "FLAG_ENDS_BLOCK");
            }

            return result.toString();
        }

        @Override
        public String toString() {
            return name() + "(" + flagsToString() + ")";
        }
    }

    public static abstract class STUB extends Instruction {

        @Override
        public void interpret(Processor processor, int insn) {
            instance(insn).interpret(processor, insn);
        }

        @Override
        public void compile(ICompilerContext context, int insn) {
            instance(insn).compile(context, insn);
        }

        @Override
        public String disasm(int address, int insn) {
            return instance(insn).disasm(address, insn);
        }

        @Override
        public abstract Instruction instance(int insn);

        @Override
        public final String name() {
            return null;
        }

        @Override
        public final String category() {
            return null;
        }
    }
    public static final Instruction UNK = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
        	Modules.log.warn(String.format("%08X %s", processor.cpu.pc, disasm(processor.cpu.pc, insn)));
        }

        @Override
        public void compile(ICompilerContext context, int insn) {
            super.compile(context, insn);
        }

        @Override
        public String disasm(int address, int insn) {
            return String.format("Unknown instruction %32s (0x%08X)", Utilities.integerToBin(insn), insn);
        }

        @Override
        public final String name() {
            return "UNK";
        }

        @Override
        public final String category() {
            return "UNK";
        }
    };

    public static final int _zr = 0;
    public static final int _at = 1;
    public static final int _v0 = 2;
    public static final int _v1 = 3;
    public static final int _a0 = 4;
    public static final int _a1 = 5;
    public static final int _a2 = 6;
    public static final int _a3 = 7;
    public static final int _t0 = 8;
    public static final int _t1 = 9;
    public static final int _t2 = 10;
    public static final int _t3 = 11;
    public static final int _t4 = 12;
    public static final int _t5 = 13;
    public static final int _t6 = 14;
    public static final int _t7 = 15;
    public static final int _s0 = 16;
    public static final int _s1 = 17;
    public static final int _s2 = 18;
    public static final int _s3 = 19;
    public static final int _s4 = 20;
    public static final int _s5 = 21;
    public static final int _s6 = 22;
    public static final int _s7 = 23;
    public static final int _t8 = 24;
    public static final int _t9 = 25;
    public static final int _k0 = 26;
    public static final int _k1 = 27;
    public static final int _gp = 28;
    public static final int _sp = 29;
    public static final int _fp = 30;
    public static final int _ra = 31;
    public static final int _f0 = 0;
    public static final int _f12 = 12;

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
            "R720.p", "R721.p", "R722.p", "R723.p"
        },
        {
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
            "R710.t", "R711.t", "R712.t", "R713.t"
        }, {
            "C000.q", "C010.q", "C020.q", "C030.q",
            "C100.q", "C110.q", "C120.q", "C130.q",
            "C200.q", "C210.q", "C220.q", "C230.q",
            "C300.q", "C310.q", "C320.q", "C330.q",
            "C400.q", "C410.q", "C420.q", "C430.q",
            "C500.q", "C510.q", "C520.q", "C530.q",
            "C600.q", "C610.q", "C620.q", "C630.q",
            "C700.q", "C710.q", "C720.q", "C730.q",
            "R000.q", "R001.q", "R002.q", "R003.q",
            "R100.q", "R101.q", "R102.q", "R103.q",
            "R200.q", "R201.q", "R202.q", "R203.q",
            "R300.q", "R301.q", "R302.q", "R303.q",
            "R400.q", "R401.q", "R402.q", "R403.q",
            "R500.q", "R501.q", "R502.q", "R503.q",
            "R600.q", "R601.q", "R602.q", "R603.q",
            "R700.q", "R701.q", "R702.q", "R703.q",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", ""
        }
    };
    public static String[][] vprMatNames = {
        {},
        {
            "M000.p", "", "M020.p", "",
            "M100.p", "", "M120.p", "",
            "M200.p", "", "M220.p", "",
            "M300.p", "", "M320.p", "",
            "M400.p", "", "M420.p", "",
            "M500.p", "", "M520.p", "",
            "M600.p", "", "M620.p", "",
            "M700.p", "", "M720.p", "",
            "E000.p", "", "E002.p", "",
            "E100.p", "", "E102.p", "",
            "E200.p", "", "E202.p", "",
            "E300.p", "", "E302.p", "",
            "E400.p", "", "E402.p", "",
            "E500.p", "", "E502.p", "",
            "E600.p", "", "E602.p", "",
            "E700.p", "", "E702.p", "",
            "M002.p", "", "M022.p", "",
            "M102.p", "", "M122.p", "",
            "M202.p", "", "M222.p", "",
            "M302.p", "", "M322.p", "",
            "M402.p", "", "M422.p", "",
            "M502.p", "", "M522.p", "",
            "M602.p", "", "M622.p", "",
            "M702.p", "", "M722.p", "",
            "E020.p", "", "E022.p", "",
            "E120.p", "", "E122.p", "",
            "E220.p", "", "E222.p", "",
            "E320.p", "", "E322.p", "",
            "E420.p", "", "E422.p", "",
            "E520.p", "", "E522.p", "",
            "E620.p", "", "E622.p", "",
            "E720.p", "", "E722.p", ""
        },
        {
            "M000.t", "M010.t", "", "",
            "M100.t", "M110.t", "", "",
            "M200.t", "M210.t", "", "",
            "M300.t", "M310.t", "", "",
            "M400.t", "M410.t", "", "",
            "M500.t", "M510.t", "", "",
            "M600.t", "M610.t", "", "",
            "M700.t", "M710.t", "", "",
            "E000.t", "E001.t", "", "",
            "E100.t", "E101.t", "", "",
            "E200.t", "E201.t", "", "",
            "E300.t", "E301.t", "", "",
            "E400.t", "E401.t", "", "",
            "E500.t", "E501.t", "", "",
            "E600.t", "E601.t", "", "",
            "E700.t", "E701.t", "", "",
            "M001.t", "M011.t", "", "",
            "M101.t", "M111.t", "", "",
            "M201.t", "M211.t", "", "",
            "M301.t", "M311.t", "", "",
            "M401.t", "M411.t", "", "",
            "M501.t", "M511.t", "", "",
            "M601.t", "M611.t", "", "",
            "M701.t", "M711.t", "", "",
            "E010.t", "E011.t", "", "",
            "E110.t", "E111.t", "", "",
            "E210.t", "E211.t", "", "",
            "E310.t", "E311.t", "", "",
            "E410.t", "E411.t", "", "",
            "E510.t", "E511.t", "", "",
            "E610.t", "E611.t", "", "",
            "E710.t", "E711.t", "", ""
        }, {
            "M000.q", "", "", "",
            "M100.q", "", "", "",
            "M200.q", "", "", "",
            "M300.q", "", "", "",
            "M400.q", "", "", "",
            "M500.q", "", "", "",
            "M600.q", "", "", "",
            "M700.q", "", "", "",
            "E000.q", "", "", "",
            "E100.q", "", "", "",
            "E200.q", "", "", "",
            "E300.q", "", "", "",
            "E400.q", "", "", "",
            "E500.q", "", "", "",
            "E600.q", "", "", "",
            "E700.q", "", "", ""
        }
    };
    private static final String vfpuConstants[] = {
        "", "VFPU_HUGE", "VFPU_SQRT2", "VFPU_SQRT1_2",
        "VFPU_2_SQRTPI", "VFPU_2_PI", "VFPU_1_PI", "VFPU_PI_4",
        "VFPU_PI_2", "VFPU_PI", "VFPU_E", "VFPU_LOG2E",
        "VFPU_LOG10E", "VFPU_LN2", "VFPU_LN10", "VFPU_2PI",
        "VFPU_PI_6", "VFPU_LOG10TWO", "VFPU_LOG2TEN", "VFPU_SQRT3_2", "", "",
        "", "", "", "", "", "", "", "", "", ""
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
    public static String vsuffix[] = {
        ".s",
        ".p",
        ".t",
        ".q"
    };
    public static String[] vpfxNames = {
        "x",
        "y",
        "z",
        "w"
    };
    public static final String ccondsNames[] = {
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
    public static final String vcondNames[] = {
        "FL",
        "EQ",
        "LT",
        "LE",
        "TR",
        "NE",
        "GE",
        "GT",
        "EZ",
        "EN",
        "EI",
        "ES",
        "NZ",
        "NN",
        "NI",
        "NS"
    };

    public static String disasmRDRTSA(String opname, int rd, int rt, int sa) {
        if ((rd == 0) && sa == 0) {
            return "nop";
        }
		return String.format("%1$-10s %2$s, %3$s, 0x%4$04X", opname, gprNames[rd], gprNames[rt], sa);
    }

    public static String disasmRDRTRS(String opname, int rd, int rt, int rs) {
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname, gprNames[rd], gprNames[rt], gprNames[rs]);
    }

    public static String disasmRS(String opname, int rs) {
        return String.format("%1$-10s %2$s", opname, gprNames[rs]);
    }

    public static String disasmRT(String opname, int rt) {
        return String.format("%1$-10s %2$s", opname, gprNames[rt]);
    }

    public static String disasmRDRS(String opname, int rd, int rs) {
        return String.format("%1$-10s %2$s, %3$s", opname, gprNames[rd], gprNames[rs]);
    }

    public static String disasmRDRT(String opname, int rd, int rt) {
        return String.format("%1$-10s %2$s, %3$s", opname, gprNames[rd], gprNames[rt]);
    }

    public static String disasmRD(String opname, int rd) {
        return String.format("%1$-10s %2$s", opname, gprNames[rd]);
    }

    public static String disasmRSRT(String opname, int rs, int rt) {
        return String.format("%1$-10s %2$s, %3$s", opname, gprNames[rs], gprNames[rt]);
    }

    public static String disasmEXT(int rt, int rs, int lsb, int msb) {
        return String.format("%1$-10s %2$s, %3$s, %4$d, %5$d", "ext", gprNames[rt], gprNames[rs], lsb, (msb + 1));
    }

    public static String disasmINS(int rt, int rs, int lsb, int msb) {
        return String.format("%1$-10s %2$s, %3$s, %4$d, %5$d", "ins", gprNames[rt], gprNames[rs], lsb, (msb - lsb + 1));
    }

    public static String disasmRDRSRT(String opname, int rd, int rs, int rt) {
        String s = String.format("%1$-10s %2$s, %3$s, %4$s", opname, gprNames[rd], gprNames[rs], gprNames[rt]);
        if (rs == 0 && rt == 0) {

            if (opname.equals("xor") || opname.equals("nor")) {
                return String.format("%2$s <=> li %1$s, -1", gprNames[rd], s);
            }

            return String.format("%2$s <=> li %1$s, 0", gprNames[rd], s);
        } else if (rs == 0) {

            if (opname.equals("and")) {
                return String.format("%2$s <=> li %1$s, 0", gprNames[rd], s);
            }

            if (opname.equals("nor")) {
                return String.format("%3$s <=> li %1$s, not %2$s", gprNames[rd], gprNames[rt], s);
            }

            if (opname.equals("sub")) {
                return String.format("%3$s <=> neg %1$s, %2$s", gprNames[rd], gprNames[rt], s);
            }

            if (opname.equals("subu")) {
                return String.format("%3$s <=> negu %1$s, %2$s", gprNames[rd], gprNames[rt], s);
            }

            return String.format("%3$s <=> move %1$s, %2$s", gprNames[rd], gprNames[rt], s);
        } else if (rt == 0) {

            if (opname.equals("and")) {
                return String.format("%2$s <=> li %1$s, 0", gprNames[rd], s);
            }

            if (opname.equals("nor")) {
                return String.format("%3$s <=> li %1$s, not %2$s", gprNames[rd], gprNames[rs], s);
            }

            return String.format("%3$s <=> move %1$s, %2$s", gprNames[rd], gprNames[rs], s);
        }

        return s;
    }

    public static String disasmRSOFFSET(String opname, int rs, int simm16, int opcode_address) {
        return String.format("%1$-10s %2$s, 0x%3$08X", opname, gprNames[rs], ((int) (short) simm16) * 4 + opcode_address + 4);
    }

    public static String disasmRSRTOFFSET(String opname, int rs, int rt, int simm16, int opcode_address) {
    	if (rs == rt && opname.equals("beq")) {
    		return String.format("%1$-10s 0x%2$08X", "b", ((int) (short) simm16) * 4 + opcode_address + 4);
    	}

    	return String.format("%1$-10s %2$s, %3$s, 0x%4$08X", opname, gprNames[rs], gprNames[rt], ((int) (short) simm16) * 4 + opcode_address + 4);
    }

    public static String disasmOFFSET(String opname, int simm16, int opcode_address) {
        return String.format("%1$-10s 0x%2$08X", opname, ((int) (short) simm16) * 4 + opcode_address + 4);
    }

    public static String disasmRTRSIMM(String opname, int rt, int rs, int imm16) {
        String s = String.format("%1$-10s %2$s, %3$s, %4$d", opname, gprNames[rt], gprNames[rs], ((int) (short) imm16));

        if (rs == 0) {

            if (opname.equals("andi")) {
                return String.format("%2$s <=> li %1$s, 0", gprNames[rt], s);
            } else if (opname.matches("slti")) {
                return String.format("%3$s <=> li %1$s, %2$d", gprNames[rt], ((0 < imm16) ? 1 : 0), s);
            } else if (opname.matches("addiu") || opname.equals("ori")) {
                return String.format("%3$s <=> li %1$s, %2$d", gprNames[rt], imm16, s);
            }

        }

        return s;
    }

    public static String disasmSYSCALL(int code) {
    	String functionName = HLEModuleManager.getInstance().functionName(code);
    	
    	if(functionName == null) {
	        for (SyscallIgnore c : SyscallIgnore.values()) {
	            if (c.getSyscall() == code) {
	                functionName = c.toString();
	                break;
	            }
	        }
    	}
    	
    	if(functionName == null)
    		functionName = "unknown";
    	
        return String.format("%1$-10s 0x%2$05X [%3$s]", "syscall", code, functionName);
    }

    public static String disasmBREAK(int code) {
        return String.format("%1$-10s 0x%2$05X", "break", code);
    }

    public static String disasmJUMP(String opname, int uimm26, int opcode_address) {
        int jump = (opcode_address & 0xf0000000) | ((uimm26 & 0x3ffffff) << 2);
        int jumpToSyscall = jump + 4;

        // If we think the target is a stub, try and append the syscall name
        if ((opname.equals("jal") || opname.equals("j")) && jump != 0 &&
                jumpToSyscall != opcode_address && Memory.isAddressGood(jumpToSyscall)) {
            int nextOpcode = jpcsp.Memory.getInstance().read32(jumpToSyscall);
            Instruction nextInsn = Decoder.instruction(nextOpcode);
            String secondTarget = nextInsn.disasm(jumpToSyscall, nextOpcode);
            if (secondTarget.startsWith("syscall") && !secondTarget.contains("[unknown]")) {
                return String.format("%1$-10s 0x%2$08X %3$s", opname, jump, secondTarget.substring(19));
            }
        }
        return String.format("%1$-10s 0x%2$08X", opname, jump);
    }

    public static String disasmRTIMM(String opname, int rt, int imm) {
        return String.format("%1$-10s %2$s, 0x%3$04X <=> li %2$s, 0x%3$04X0000", opname, gprNames[rt], (imm & 0xFFFF));
    }

    public static String disasmRTIMMRS(String opname, int rt, int rs, int imm) {
        return String.format("%1$-10s %2$s, %4$d(%3$s)", opname, gprNames[rt], gprNames[rs], ((int) (short) imm));
    }

    public static String disasmCODEIMMRS(String opname, int code, int imm, int rs) {
        return String.format("%1$-10s 0x%2$02X, %4$d(%3$s)", opname, code, gprNames[rs], ((int) (short) imm));
    }

    public static String disasmFTIMMRS(String opname, int ft, int rs, int imm) {
        return String.format("%1$-10s %2$s, %4$d(%3$s)", opname, fprNames[ft], gprNames[rs], ((int) (short) imm));
    }

    public static String disasmFDFSFT(String opname, int fd, int fs, int ft) {
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname, fprNames[fd], fprNames[fs], fprNames[ft]);
    }

    public static String disasmFDFS(String opname, int fd, int fs) {
        return String.format("%1$-10s %2$s, %3$s", opname, fprNames[fd], fprNames[fs]);
    }

    public static String disasmRTFS(String opname, int rt, int fs) {
        return String.format("%1$-10s %2$s, %3$s", opname, gprNames[rt], fprNames[fs]);
    }

    public static String disasmRTFC(String opname, int rt, int fc) {
        return String.format("%1$-10s %2$s, %3$s", opname, gprNames[rt], fcrNames[fc]);
    }

    public static String disasmCcondS(int cconds, int fs, int ft) {
        return String.format("%1$-10s %2$s, %3$s", ccondsNames[cconds], fprNames[fs], fprNames[ft]);
    }

    public static String disasmFSFT(String opname, int fs, int ft) {
        return String.format("%1$-10s %2$s, %3$s", opname, fprNames[fs], fprNames[ft]);
    }

    public static String disasmVD(String opname, int vsize, int vd) {
        return String.format("%1$-10s %2$s", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vd]);
    }

    public static String disasmVS(String opname, int vsize, int vs) {
        return String.format("%1$-10s %2$s", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vs]);
    }

    public static String disasmVDVS(String opname, int vsize, int vd, int vs) {
        return String.format("%1$-10s %2$s, %3$s", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vd], vprNames[vsize - 1][vs]);
    }

    public static String disasmVDVSIMM(String opname, int vsize, int vd, int vs, int imm) {
        return String.format("%1$-10s %2$s, %3$s, %4$d", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vd], vprNames[vsize - 1][vs], imm);
    }

    public static String disasmVD1VS(String opname, int vsize, int vd, int vs) {
        return String.format("%1$-10s %2$s, %3$s", opname + vsuffix[vsize - 1], vprNames[0][vd], vprNames[vsize - 1][vs]);
    }

    public static String disasmVTIMMRS(String opname, int vsize, int vt, int rs, int imm) {
        return String.format(
                "%1$-10s %2$s, %4$d(%3$s)",
                opname + vsuffix[vsize - 1],
                vprNames[vsize - 1][vt],
                gprNames[rs],
                imm);
    }

    public static String disasmVDVSVT(String opname, int vsize, int vd, int vs, int vt) {
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vd], vprNames[vsize - 1][vs], vprNames[vsize - 1][vt]);
    }

    public static String disasmVDVSVT1(String opname, int vsize, int vd, int vs, int vt) {
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vd], vprNames[vsize - 1][vs], vprNames[0][vt]);
    }

    public static String disasmVD1VSVT(String opname, int vsize, int vd, int vs, int vt) {
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname + vsuffix[vsize - 1], vprNames[0][vd], vprNames[vsize - 1][vs], vprNames[vsize - 1][vt]);
    }

    public static String disasmVCMP(String opname, int vsize, int vcode, int vs, int vt) {
        if ((vcode & ~4) == 0) {
            return String.format("%1$-10s %2$s", opname + vsuffix[vsize - 1], vcondNames[vcode]);
        } else if (vcode < 8) {
            return String.format("%1$-10s %2$s, %3$s, %4$s", opname + vsuffix[vsize - 1], vcondNames[vcode], vprNames[vsize - 1][vs], vprNames[vsize - 1][vt]);
        }
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname + vsuffix[vsize - 1], vcondNames[vcode], vprNames[vsize - 1][vs], vprNames[vsize - 1][vt]);
    }

    public static String disasmVROT(String opname, int vsize, int vd, int vs, int vt) {
        int i;
        int si = (vt >>> 2) & 3;
        int ci = (vt >>> 0) & 3;
        String ca = " c", sa = " s";
        String codes[] = new String[4];
        if ((vt & 16) != 0) {
            sa = "-s";
        }

        if (si == ci) {
            for (i = 0; i < vsize; ++i) {
                codes[i] = (ci == i) ? ca : sa;
            }
        } else {
            for (i = 0; i < vsize; ++i) {
                codes[i] = (ci == i) ? ca : ((si == i) ? sa : " 0");
            }
        }

        StringBuilder rot = new StringBuilder("[");

        i = 0;
        for (;;) {
            rot.append(codes[i++]);
            if (i >= vsize) {
                break;
            }
            rot.append(",");
        }
        rot.append("]");
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vd], vprNames[0][vs], rot);
    }

    public static String disasmVDM(String opname, int vsize, int vd) {
        return String.format("%1$-10s %2$s", opname + vsuffix[vsize - 1], vprMatNames[vsize - 1][vd]);
    }

    public static String disasmVDMVSM(String opname, int vsize, int vd, int vs) {
        return String.format("%1$-10s %2$s, %3$s", opname + vsuffix[vsize - 1], vprMatNames[vsize - 1][vd], vprMatNames[vsize - 1][vs]);
    }

    public static String disasmVDCST(String opname, int vsize, int vd, int cst) {
        return String.format("%1$-10s %2$s, %3$s", opname + vsuffix[vsize - 1], vprNames[vsize - 1][vd], vfpuConstants[cst]);
    }

    public static String disasmVDIIM(String opcode, int vsize, int vd, int imm16) {
        return String.format("%1$-10s %2$s, 0x%3$04X", opcode + vsuffix[vsize - 1], vprNames[0][vd], imm16);
    }

    public static String disasmVDFIM(String opcode, int vsize, int vd, int imm16) {
        float s = ((imm16 >> 15) == 0) ? 1.0f : -1.0f;
        int e = ((imm16 >> 10) & 0x1f);
        int m = (e == 0) ? ((imm16 & 0x3ff) << 1) : ((imm16 & 0x3ff) | 0x400);

        s = s * ((float) m) / ((float) (1 << (15 - e))) / ((float) (1 << 10));
        return String.format("%1$-10s %2$s, %3$1.8f", opcode + vsuffix[vsize - 1], vprNames[0][vd], s);
    }

    public static String disasmVDMVSMVTM(String opname, int vsize, int vd, int vs, int vt) {
        return String.format("%1$-10s %2$s, %3$s, %4$s", opname + vsuffix[vsize - 1], vprMatNames[vsize - 1][vd], vprMatNames[vsize - 1][vs], vprMatNames[vsize - 1][vt]);
    }

    public static String disasmVDRS(String opname, int vd, int rt) {
        return String.format("%1$-10s %2$s, %3$s", opname + vsuffix[0], gprNames[rt], vprNames[0][vd]);
    }

    public static String disasmVPFX(String opname, int[] swz, boolean[] abs, boolean[] cst, boolean[] neg) {
        String[] values = new String[4];
        for (int i = 0; i < 4; ++i) {
            if (cst[i]) {
                switch (swz[i]) {
                    case 0:
                        values[i] = abs[i] ? "3" : "0";
                        break;
                    case 1:
                        values[i] = abs[i] ? "1/3" : "1";
                        break;
                    case 2:
                        values[i] = abs[i] ? "1/4" : "2";
                        break;
                    case 3:
                        values[i] = abs[i] ? "1/6" : "1/2";
                        break;
                }
            } else {
                values[i] = abs[i] ? "|" + vpfxNames[swz[i]] + "|"
                        : vpfxNames[swz[i]];
            }

            if (neg[i]) {
                values[i] = "-" + values[i];
            }
        }

        return String.format("%1$-10s [%2$s, %3$s, %4$s, %5$s]", opname, values[0], values[1], values[2], values[3]);
    }

    public static String disasmVPFXD(String opname, int[] sat, int[] msk) {
        String[] values = new String[4];
        for (int i = 0; i < 4; ++i) {
            if (msk[i] == 0) {
                values[i] = sat[i] == 1 ? "0:1" : "-1:1";
            } else {
                values[i] = "M";
            }
        }
        return String.format("%1$-10s [%2$s, %3$s, %4$s, %5$s]", opname, values[0], values[1], values[2], values[3]);
    }
    public static String disasmVCCOFFSET(String opname, int vcc, int simm16, int opcode_address) {
        return String.format("%1$-10s %2$d, 0x%3$08X", opname, vcc, ((int) (short) simm16) * 4 + opcode_address + 4);
    }


    protected static Instruction[] m_instances = new Instruction[252];

    public static final Instruction[] instructions() {
        return m_instances;
    }
}
