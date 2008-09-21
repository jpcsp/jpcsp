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
package jpcsp;

//import static jpcsp.AllegrexInstructions;
import jpcsp.HLE.SyscallHandler;
import jpcsp.Allegrex.*;
import java.util.HashMap;
import java.util.Map;

public class Processor implements AllegrexInstructions {

// "New-Style" Processor
    public CpuState cpu;

    public static final jpcsp.Memory memory = jpcsp.Memory.getInstance();
    
    /* to uncomment when we need to use the "New-Style" Processor
    Processor() {
        reset();
    }

    public void reset() {
        cpu.reset();
    }
    */

    public void interpret() {

        int opcode = cpu.fetchOpcode();
        
        Common.Instruction insn = Decoder.instruction(opcode);
        
        insn.interpret(this, opcode);
    }

    public void interpretDelayslot() {

        int opcode = cpu.nextOpcode();
        
        Common.Instruction insn = Decoder.instruction(opcode);
        
        insn.interpret(this, opcode);
        
        cpu.nextPc();
    }

    /* to uncomment when we need to use the "New-Style" Processor
    public void step() {
        interpret();
    }
    */
    
// what follows is "Old-style" Processor (to be removed one day)
    
    public static final int fcr0_imp = 0; /* FPU design number */

    public static final int fcr0_rev = 0; /* FPU revision bumber */

    public int[] gpr;
    public float[] fpr;
    public float[][][] vpr;
    public long hilo;
    public int pc,  npc;
    public int fcr31_rm;
    public boolean fcr31_c;
    public boolean fcr31_fs;
    public boolean[] vcr_cc;
    public int[] vcr_pfxs_swz;
    public boolean[] vcr_pfxs_abs;
    public boolean[] vcr_pfxs_cst;
    public boolean[] vcr_pfxs_neg;
    public boolean vcr_pfxs;
    public int[] vcr_pfxt_swz;
    public boolean[] vcr_pfxt_abs;
    public boolean[] vcr_pfxt_cst;
    public boolean[] vcr_pfxt_neg;
    public boolean vcr_pfxt;
    public int[] vcr_pfxd_sat;
    public boolean[] vcr_pfxd_msk;
    public boolean vcr_pfxd;
    public long cycles;
    public long hilo_cycles;
    public long[] fpr_cycles;
    public long[][][] vpr_cycles;
    public long fcr31_cycles;
    public AllegrexBasicBlock current_bb = null;
    protected Map<Integer, AllegrexBasicBlock> basic_blocks = new HashMap<Integer, AllegrexBasicBlock>();
    public final boolean interpreter_only = true;

    public class RegisterTracking {

        public boolean loaded = false; // load the register content if not already done
        public boolean dirty = false; // save the register content back at basic block termination
        public boolean fixed = false; // the register helds a constant value
        public boolean labeled = false; // an associated local variable is created
    }
    public RegisterTracking tracked_gpr[];
    public RegisterTracking tracked_fpr[];
    public RegisterTracking tracked_hilo;

    Processor() {
        cpu = new CpuState();
        Memory.getInstance(); //intialize memory
        reset();
        testOpcodes();
        reset();
    }

    public void reset() {
        cpu.reset();
        
        // intialize psp register
        pc = npc = 0x00000000;
        hilo = 0;
        gpr = new int[32];
        fpr = new float[32];
        vpr = new float[8][4][4]; // [matrix][column][row]

        fcr31_rm = 0;
        fcr31_c = false;
        fcr31_fs = false;

        vcr_cc = new boolean[6];

        cycles = 0;
        hilo_cycles = 0;
        fpr_cycles = new long[32];
        vpr_cycles = new long[8][4][4];

        tracked_gpr = new RegisterTracking[32];
        tracked_fpr = new RegisterTracking[32];
        tracked_hilo = new RegisterTracking();

        reset_register_tracking();

        if (false) {
            for (Common.Instruction insn : Common.instructions()) {
                if (insn != null) {
                    insn.resetCount();
                }
            }
        }
    }

    public void fix_gpr(int register, int value) {
        if (register != 0 || current_bb != null) {
            tracked_gpr[register].loaded = false;
            tracked_gpr[register].dirty = true;
            tracked_gpr[register].fixed = true;
            gpr[register] = value;
        }
    }

    public void fix_fpr(int register, float value) {
        if (register != 0 || current_bb != null) {
            tracked_fpr[register].loaded = false;
            tracked_fpr[register].dirty = true;
            tracked_fpr[register].fixed = true;
            fpr[register] = value;
        }
    }

    public void load_gpr(int register, boolean force) {
        if (register != 0 && current_bb != null) {
            if (!force && tracked_gpr[register].fixed) {
                return;
            }
            if (!tracked_gpr[register].labeled) {
                current_bb.emit("int gpr_" + register + " = processor.gpr[" + register + "];");
                tracked_gpr[register].labeled = true;
            }
            tracked_gpr[register].loaded = true;
            tracked_gpr[register].dirty = false;
            tracked_gpr[register].fixed = false;
        }
    }

    public void load_fpr(int register, boolean force) {
        if (register != 0 && current_bb != null) {
            if (!force && tracked_fpr[register].fixed) {
                return;
            }
            if (!tracked_fpr[register].labeled) {
                current_bb.emit("int fpr_" + register + " = processor.fpr[" + register + "];");
                tracked_fpr[register].labeled = true;
            }
            tracked_fpr[register].loaded = true;
            tracked_fpr[register].dirty = false;
            tracked_fpr[register].fixed = false;
        }
    }

    public void load_hilo(boolean force) {
        if (current_bb != null) {
            if (!force && tracked_hilo.fixed) {
                return;
            }
            if (!tracked_hilo.labeled) {
                current_bb.emit("long hilo = processor.hilo;");
                tracked_hilo.labeled = true;
            }
            tracked_hilo.loaded = true;
            tracked_hilo.dirty = false;
            tracked_hilo.fixed = false;
        }
    }

    public void alter_gpr(int register) {
        if (register != 0 && current_bb != null) {
            if (!tracked_gpr[register].labeled) {
                current_bb.emit("int gpr_" + register + ";");
                tracked_gpr[register].labeled = true;
            }
            tracked_gpr[register].loaded = false;
            tracked_gpr[register].dirty = true;
            tracked_gpr[register].fixed = false;
        }
    }

    public void alter_fpr(int register) {
        if (register != 0 && current_bb != null) {
            if (!tracked_fpr[register].labeled) {
                current_bb.emit("float fpr_" + register + ";");
                tracked_fpr[register].labeled = true;
            }
            tracked_fpr[register].loaded = false;
            tracked_fpr[register].dirty = true;
            tracked_fpr[register].fixed = false;
        }
    }

    public void alter_hilo() {
        if (current_bb != null) {
            if (!tracked_hilo.labeled) {
                current_bb.emit("long hilo;");
                tracked_hilo.labeled = true;
            }
            tracked_hilo.loaded = false;
            tracked_hilo.dirty = true;
            tracked_hilo.fixed = false;
        }
    }

    public String get_gpr(int register) {
        if (tracked_gpr[register].fixed) {
            return "0x" + Integer.toHexString(gpr[register]);
        } else if (tracked_gpr[register].labeled) {
            return "gpr_" + register;
        } else {
            return "processor.gpr[" + register + "]";
        }
    }

    public String get_fpr(int register) {
        if (tracked_gpr[register].fixed) {
            return Float.toString(fpr[register]);
        } else if (tracked_gpr[register].labeled) {
            return "fpr_" + register;
        } else {
            return "processor.fpr[" + register + "]";
        }
    }

    public String get_hilo() {
        if (tracked_hilo.labeled) {
            return "hilo";
        } else {
            return "processor.hilo";
        }
    }

    public void reset_register_tracking() {
        if (current_bb != null) {
            for (int i = 1; i < 32; ++i) {
                if (tracked_gpr[i].labeled) {
                    if (tracked_gpr[i].dirty) {
                        current_bb.emit("processor.gpr[" + i + "] = " + get_gpr(i) + ";");
                    }
                }
            }

            tracked_gpr[0].loaded = false;
            tracked_gpr[0].dirty = false;
            tracked_gpr[0].fixed = true;
            tracked_gpr[0].labeled = false;

            for (int i = 0; i < 32; ++i) {
                if (tracked_fpr[i].labeled) {
                    if (tracked_fpr[i].dirty) {
                        current_bb.emit("processor.fpr[" + i + "] = " + get_fpr(i) + ";");
                    }
                }
            }
            if (tracked_hilo.labeled) {
                if (tracked_hilo.dirty) {
                    current_bb.emit("processor.hilo = hilo;");
                }
            }
        }
    }

    public int hi() {
        return (int) (hilo >>> 32);
    }

    public int lo() {
        return (int) (hilo & 0xffffffff);
    }

    public static long signedDivMod(int x, int y) {
        return ((long) (x % y)) << 32 | (((long) (x / y)) & 0xffffffff);
    }

    public static long unsignedDivMod(long x, long y) {
        return ((x % y)) << 32 | ((x / y) & 0xffffffff);
    }

    public static int max(int x, int y) {
        return (x > y) ? x : y;
    }

    public static int min(int x, int y) {
        return (x < y) ? x : y;
    }

    public static int extractBits(int x, int pos, int len) {
        return (x >>> pos) & ~(~0 << len);
    }

    public static int insertBits(int x, int y, int lsb, int msb) {
        int mask = ~(~0 << (msb - lsb + 1)) << lsb;
        return (x & ~mask) | ((y << lsb) & mask);
    }

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

    public static int signedCompare(int i, int j) {
        return (i - j) >>> 31;
    }

    public static int unsignedCompare(int i, int j) {
        return ((i - j) ^ i ^ j) >>> 31;
    }

    public static int branchTarget(int npc, int simm16) {
        return npc + (simm16 << 2);
    }

    public static int jumpTarget(int npc, int uimm26) {
        return (npc & 0xf0000000) | (uimm26 << 2);
    }

    private static boolean addSubOverflow(long value) {
        long tmp = value << (62 - 31);
        return ((tmp >>> 1) == (tmp & 1));
    }

    private void updateCyclesFdFsFt(int fd, int fs, int ft, long latency) {
        // WAW conflict (Successively writing the same register)
        cycles = Math.max(cycles, fpr_cycles[fd]);

        // cycles when the destination register is written
        fpr_cycles[fd] = cycles + latency;

        // RAW conflict (Using the result of previous FPU instructions)
        cycles = Math.max(cycles, Math.max(fpr_cycles[fs], fpr_cycles[ft]));
    }

    private void updateCyclesFdFs(int fd, int fs, long latency) {
        // WAW conflict (Successively writing the same register)
        cycles = Math.max(cycles, fpr_cycles[fd]);

        // cycles when the destination register is written
        fpr_cycles[fd] = cycles + latency;

        // RAW conflict (Using the result of previous FPU instructions)
        cycles = Math.max(cycles, fpr_cycles[fs]);
    }

    private void updateCyclesFsFt(int fs, int ft, long latency) {
        // WAW conflict (Successively writing the same register)
        cycles = Math.max(cycles, fcr31_cycles);

        // cycles when the FPU C bit is written
        fcr31_cycles = cycles + latency;

        // RAW conflict (Using the result of previous FPU instructions)
        cycles = Math.max(cycles, Math.max(fpr_cycles[fs], fpr_cycles[ft]));
    }
    protected final AllegrexDecoder decoder = new AllegrexDecoder();

    public void step() {

        // check whether a basic block is in progress
        if (!interpreter_only && current_bb == null) {
            current_bb = basic_blocks.get(pc);
            if (current_bb != null) {
                current_bb.executionCount++;
                current_bb.execute();
                current_bb = null;
                return;
            } else {
                current_bb = new AllegrexBasicBlock(this, pc);
                basic_blocks.put(pc, current_bb);
            }
        }

        npc = pc + 4;

        int insn = (Memory.getInstance()).read32(pc);

        if (false) {
            Common.Instruction i = Decoder.instruction(insn);
            i.increaseCount();
            System.out.printf("count for 0x%08x : %d\n", insn, i.getCount());
        }
        // by default, any Allegrex instruction takes 1 cycle at least
        cycles += 1;

        // by default, the next instruction to emulate is at the next address
        pc = npc;

        // process the current instruction
        decoder.process(this, insn);
    }

    public void stepDelayslot() {
        int insn = (Memory.getInstance()).read32(pc);

        if (false) {
            Common.Instruction i = Decoder.instruction(insn);
            i.increaseCount();
            System.out.printf("count for 0x%08x : %d\n", insn, i.getCount());
        }
        // by default, any Allegrex instruction takes 1 cycle at least
        cycles += 1;

        // by default, the next instruction to emulate is at the next address
        pc += 4;

        // process the current instruction
        decoder.process(this, insn);

        pc = npc;
        npc = pc + 4;
    }

    @Override
    public void doUNK(String reason) {
        System.out.println("Interpreter : " + reason);
    }

    @Override
    public void doNOP() {
    }

    @Override
    public void doSLL(int rd, int rt, int sa) {
        if (rd != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rd] = (gpr[rt] << sa);
            } else if (tracked_gpr[rt].fixed) {
                fix_gpr(rd, (gpr[rt] << sa));
            } else {
                load_gpr(rt, false);
                alter_gpr(rd);
                current_bb.emit(get_gpr(rd) + " = (" + get_gpr(rt) + " << " + sa + ");");
            }
        }
    }

    @Override
    public void doSRL(int rd, int rt, int sa) {
        if (rd != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rd] = (gpr[rt] >>> sa);
            } else if (tracked_gpr[rt].fixed) {
                fix_gpr(rd, (gpr[rt] >>> sa));
            } else {
                load_gpr(rt, false);
                alter_gpr(rd);
                current_bb.emit(get_gpr(rd) + " = (" + get_gpr(rt) + " >>> " + sa + ");");
            }
        }
    }

    @Override
    public void doSRA(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] >> sa);
            if (interpreter_only || (current_bb == null)) {
                gpr[rd] = (gpr[rt] >> sa);
            } else if (tracked_gpr[rt].fixed) {
                fix_gpr(rd, (gpr[rt] >> sa));
            } else {
                load_gpr(rt, false);
                alter_gpr(rd);
                current_bb.emit(get_gpr(rd) + " = (" + get_gpr(rt) + " >> " + sa + ");");
            }
        }
    }

    @Override
    public void doSLLV(int rd, int rt, int rs) {
        if (rd != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rd] = (gpr[rt] << (gpr[rs] & 31));
            } else if (tracked_gpr[rt].fixed && tracked_gpr[rs].fixed) {
                fix_gpr(rd, (gpr[rt] << (gpr[rs] & 31)));
            } else {
                load_gpr(rs, false);
                load_gpr(rt, false);
                alter_gpr(rd);
                current_bb.emit(get_gpr(rd) + " = (" + get_gpr(rt) + " << (" + get_gpr(rt) + " & 31));");
            }
        }
    }

    @Override
    public void doSRLV(int rd, int rt, int rs) {
        if (rd != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rd] = (gpr[rt] >>> (gpr[rs] & 31));
            } else if (tracked_gpr[rt].fixed && tracked_gpr[rs].fixed) {
                fix_gpr(rd, (gpr[rt] >>> (gpr[rs] & 31)));
            } else {
                load_gpr(rs, false);
                load_gpr(rt, false);
                alter_gpr(rd);
                current_bb.emit(get_gpr(rd) + " = (" + get_gpr(rt) + " >>> (" + get_gpr(rt) + " & 31));");
            }
        }
    }

    @Override
    public void doSRAV(int rd, int rt, int rs) {
        if (rd != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rd] = (gpr[rt] >> (gpr[rs] & 31));
            } else if (tracked_gpr[rt].fixed && tracked_gpr[rs].fixed) {
                fix_gpr(rd, (gpr[rt] >> (gpr[rs] & 31)));
            } else {
                load_gpr(rs, false);
                load_gpr(rt, false);
                alter_gpr(rd);
                current_bb.emit(get_gpr(rd) + " = (" + get_gpr(rt) + " >> (" + get_gpr(rt) + " & 31));");
            }
        }
    }

    @Override
    public void doJR(int rs) {
        long target_cycles = cycles + 2;
        npc = gpr[rs];
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doJALR(int rd, int rs) {
        long target_cycles = cycles + 3;
        if (rd != 0) {
            gpr[rd] = pc + 4;
        }
        npc = gpr[rs];
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doMFHI(int rd) {
        if (rd != 0) {
            gpr[rd] = hi();
        }
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
    }

    @Override
    public void doMTHI(int rs) {
        int hi = gpr[rs];
        hilo = (((long) hi) << 32) | (hilo & 0xffffffff);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
    }

    @Override
    public void doMFLO(int rd) {
        if (rd != 0) {
            gpr[rd] = lo();
        }
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
    }

    @Override
    public void doMTLO(int rs) {
        int lo = gpr[rs];
        hilo = ((hilo >>> 32) << 32) | (((long) lo) & 0xffffffff);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
    }

    @Override
    public void doMULT(int rs, int rt) {
        hilo = ((long) gpr[rs]) * ((long) gpr[rt]);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 4;
    }

    @Override
    public void doMULTU(int rs, int rt) {
        hilo = (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 4;
    }

    @Override
    public void doDIV(int rs, int rt) {
        int lo = gpr[rs] / gpr[rt];
        int hi = gpr[rs] % gpr[rt];
        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 35;
    }

    @Override
    public void doDIVU(int rs, int rt) {
        long x = ((long) gpr[rs]) & 0xffffffff;
        long y = ((long) gpr[rt]) & 0xffffffff;
        int lo = (int) (x / y);
        int hi = (int) (x % y);
        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 35;
    }

    @Override
    public void doADD(int rd, int rs, int rt) {
        if (rd != 0) {
            long result = (long) gpr[rs] + (long) gpr[rt];

            if (!addSubOverflow(result)) {
                gpr[rd] = (int) result;
            } else {
                doUNK("ADD raises an Overflow exception");
            }
        }
    }

    @Override
    public void doADDU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] + gpr[rt];
        }
    }

    @Override
    public void doSUB(int rd, int rs, int rt) {
        if (rd != 0) {
            long result = (long) gpr[rs] - (long) gpr[rt];

            if (!addSubOverflow(result)) {
                gpr[rd] = (int) result;
            } else {
                doUNK("SUB raises an Overflow exception");
            }
        }
    }

    @Override
    public void doSUBU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] - gpr[rt];
        }
    }

    @Override
    public void doAND(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] & gpr[rt];
        }
    }

    @Override
    public void doOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] | gpr[rt];
        }
    }

    @Override
    public void doXOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] ^ gpr[rt];
        }
    }

    @Override
    public void doNOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = ~(gpr[rs] | gpr[rt]);
        }
    }

    @Override
    public void doSLT(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = signedCompare(gpr[rs], gpr[rt]);
        }
    }

    @Override
    public void doSLTU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = unsignedCompare(gpr[rs], gpr[rt]);
        }
    }

    @Override
    public void doBLTZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        npc = (gpr[rs] < 0) ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBGEZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        npc = (gpr[rs] >= 0) ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBLTZL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        if (gpr[rs] < 0) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doBGEZL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        if (gpr[rs] >= 0) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doBLTZAL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        int target = pc + 4;
        boolean t = (gpr[rs] < 0);
        gpr[31] = target;
        npc = t ? branchTarget(pc, simm16) : target;
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBGEZAL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        int target = pc + 4;
        boolean t = (gpr[rs] >= 0);
        gpr[31] = target;
        npc = t ? branchTarget(pc, simm16) : target;
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBLTZALL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        boolean t = (gpr[rs] < 0);
        gpr[31] = pc + 4;
        if (t) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doBGEZALL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        boolean t = (gpr[rs] >= 0);
        gpr[31] = pc + 4;
        if (t) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doJ(int uimm26) {
        long target_cycles = cycles + 2;
        npc = jumpTarget(pc, uimm26);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doJAL(int uimm26) {
        long target_cycles = cycles + 2;
        gpr[31] = pc + 4;
        npc = jumpTarget(pc, uimm26);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBEQ(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;
        npc = (gpr[rs] == gpr[rt]) ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBNE(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;
        npc = (gpr[rs] != gpr[rt]) ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBLEZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        npc = (gpr[rs] <= 0) ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBGTZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        npc = (gpr[rs] > 0) ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBEQL(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;
        if (gpr[rs] == gpr[rt]) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doBNEL(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;
        if (gpr[rs] != gpr[rt]) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doBLEZL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        if (gpr[rs] <= 0) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doBGTZL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        if (gpr[rs] > 0) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    @Override
    public void doADDI(int rt, int rs, int simm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                long result = (long) gpr[rs] + (long) simm16;

                if (!addSubOverflow(result)) {
                    gpr[rt] = (int) result;
                } else {
                    doUNK("ADDI raises an Overflow exception");
                }
            } else {
                // this ADDI instruction is never generated by gcc so treat it as ADDUI 
                doADDIU(rt, rs, simm16);
            }
        }
    }

    @Override
    public void doADDIU(int rt, int rs, int simm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rt] = gpr[rs] + simm16;
            } else if (tracked_gpr[rs].fixed) {
                fix_gpr(rt, gpr[rs] + simm16);
            } else {
                load_gpr(rs, false);
                alter_gpr(rt);
                current_bb.emit(get_gpr(rt) + " = " + get_gpr(rs) + " + " + simm16 + ";");
            }
        }
    }

    @Override
    public void doSLTI(int rt, int rs, int simm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rt] = signedCompare(gpr[rs], simm16);
            } else if (tracked_gpr[rs].fixed) {
                fix_gpr(rt, signedCompare(gpr[rs], simm16));
            } else {
                load_gpr(rs, false);
                alter_gpr(rt);
                current_bb.emit(get_gpr(rt) + " = processor.signedCompare(" + get_gpr(rs) + ", " + simm16 + ");");
            }
        }
    }

    @Override
    public void doSLTIU(int rt, int rs, int simm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rt] = unsignedCompare(gpr[rs], simm16);
            } else if (tracked_gpr[rs].fixed) {
                fix_gpr(rt, unsignedCompare(gpr[rs], simm16));
            } else {
                load_gpr(rs, false);
                alter_gpr(rt);
                current_bb.emit(get_gpr(rt) + " = processor.unsignedCompare(" + get_gpr(rs) + ", " + simm16 + ");");
            }
        }
    }

    @Override
    public void doANDI(int rt, int rs, int uimm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rt] = gpr[rs] & uimm16;
            } else if (tracked_gpr[rs].fixed) {
                fix_gpr(rt, gpr[rs] & uimm16);
            } else {
                load_gpr(rs, false);
                alter_gpr(rt);
                current_bb.emit(get_gpr(rt) + " = " + get_gpr(rs) + " & " + uimm16 + ";");
            }
        }
    }

    @Override
    public void doORI(int rt, int rs, int uimm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rt] = gpr[rs] | uimm16;
            } else if (tracked_gpr[rs].fixed) {
                fix_gpr(rt, gpr[rs] | uimm16);
            } else {
                load_gpr(rs, false);
                alter_gpr(rt);
                current_bb.emit(get_gpr(rt) + " = " + get_gpr(rs) + " | " + uimm16 + ";");
            }
        }
    }

    @Override
    public void doXORI(int rt, int rs, int uimm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rt] = gpr[rs] ^ uimm16;
            } else if (tracked_gpr[rs].fixed) {
                fix_gpr(rt, gpr[rs] ^ uimm16);
            } else {
                load_gpr(rs, false);
                alter_gpr(rt);
                current_bb.emit(get_gpr(rt) + " = " + get_gpr(rs) + " ^ " + uimm16 + ";");
            }
        }
    }

    @Override
    public void doLUI(int rt, int uimm16) {
        if (rt != 0) {
            if (interpreter_only || (current_bb == null)) {
                gpr[rt] = uimm16 << 16;
            } else {
                fix_gpr(rt, uimm16 << 16);
            }
        }
    }

    @Override
    public void doHALT() {
        // TODO
        doUNK("Unsupported HALT instruction");
    }

    @Override
    public void doMFIC(int rt) {
        // TODO
        doUNK("Unsupported mfic instruction");
    }

    @Override
    public void doMTIC(int rt) {
        // TODO
        doUNK("Unsupported mtic instruction");
    }

    @Override
    public void doMFC0(int rt, int c0dr) {
        // TODO
        doUNK("Unsupported mfc0 instruction");
    }

    @Override
    public void doCFC0(int rt, int c0cr) {
        // TODO
        doUNK("Unsupported cfc0 instruction");
    }

    @Override
    public void doMTC0(int rt, int c0dr) {
        // TODO
        doUNK("Unsupported mtc0 instruction");
    }

    @Override
    public void doCTC0(int rt, int c0cr) {
        // TODO
        doUNK("Unsupported ctc0 instruction");
    }

    @Override
    public void doERET() {
        // TODO
        //ll_bit = 0;
        doUNK("Unsupported eret instruction");
    }

    @Override
    public void doLB(int rt, int rs, int simm16) {
        int word = ((Memory.getInstance()).read8(gpr[rs] + simm16) << 24) >> 24;
        if (rt != 0) {
            gpr[rt] = word;
        }
    }

    @Override
    public void doLBU(int rt, int rs, int simm16) {
        int word = (Memory.getInstance()).read8(gpr[rs] + simm16) & 0xff;
        if (rt != 0) {
            gpr[rt] = word;
        }
    }

    @Override
    public void doLH(int rt, int rs, int simm16) {
        int word = ((Memory.getInstance()).read16(gpr[rs] + simm16) << 16) >> 16;
        if (rt != 0) {
            gpr[rt] = word;
        }
    }

    @Override
    public void doLHU(int rt, int rs, int simm16) {
        int word = (Memory.getInstance()).read16(gpr[rs] + simm16) & 0xffff;
        if (rt != 0) {
            gpr[rt] = word;
        }
    }

    @Override
    public void doLWL(int rt, int rs, int simm16) {
        int address = gpr[rs] + simm16;
        int offset = address & 0x3;
        int reg = gpr[rt];

        int word = (Memory.getInstance()).read32(address & 0xfffffffc);

        switch (offset) {
            case 0:
                word = ((word & 0xff) << 24) | (reg & 0xffffff);
                break;

            case 1:
                word = ((word & 0xffff) << 16) | (reg & 0xffff);
                break;

            case 2:
                word = ((word & 0xffffff) << 8) | (reg & 0xff);
                break;

            case 3:
                break;
        }

        if (rt != 0) {
            gpr[rt] = word;
        }
    }

    @Override
    public void doLW(int rt, int rs, int simm16) {
        int word = (Memory.getInstance()).read32(gpr[rs] + simm16);
        if (rt != 0) {
            gpr[rt] = word;
        }
    }

    @Override
    public void doLWR(int rt, int rs, int simm16) {
        int address = gpr[rs] + simm16;
        int offset = address & 0x3;
        int reg = gpr[rt];

        int word = (Memory.getInstance()).read32(address & 0xfffffffc);

        switch (offset) {
            case 0:
                break;

            case 1:
                word = (reg & 0xff000000) | ((word & 0xffffff00) >> 8);
                break;

            case 2:
                word = (reg & 0xffff0000) | ((word & 0xffff0000) >> 16);
                break;

            case 3:
                word = (reg & 0xffffff00) | ((word & 0xff000000) >> 24);
                break;
        }

        if (rt != 0) {
            gpr[rt] = word;
        }
    }

    @Override
    public void doSB(int rt, int rs, int simm16) {
        (Memory.getInstance()).write8(gpr[rs] + simm16, (byte) (gpr[rt] & 0xFF));
    }

    @Override
    public void doSH(int rt, int rs, int simm16) {
        (Memory.getInstance()).write16(gpr[rs] + simm16, (short) (gpr[rt] & 0xFFFF));
    }

    @Override
    public void doSWL(int rt, int rs, int simm16) {
        int address = gpr[rs] + simm16;
        int offset = address & 0x3;
        int reg = gpr[rt];
        int data = (Memory.getInstance()).read32(address & 0xfffffffc);

        switch (offset) {
            case 0:
                data = (data & 0xffffff00) | (reg >> 24 & 0xff);
                break;

            case 1:
                data = (data & 0xffff0000) | (reg >> 16 & 0xffff);
                break;

            case 2:
                data = (data & 0xff000000) | (reg >> 8 & 0xffffff);
                break;

            case 3:
                data = reg;
                break;
        }

        (Memory.getInstance()).write32(address & 0xfffffffc, data);
    }

    @Override
    public void doSW(int rt, int rs, int simm16) {
        (Memory.getInstance()).write32(gpr[rs] + simm16, gpr[rt]);
    }

    @Override
    public void doSWR(int rt, int rs, int simm16) {
        int address = gpr[rs] + simm16;
        int offset = address & 0x3;
        int reg = gpr[rt];
        int data = (Memory.getInstance()).read32(address & 0xfffffffc);

        switch (offset) {
            case 0:
                data = reg;
                break;

            case 1:
                data = ((reg << 8) & 0xffffff00) | (data & 0xff);
                break;

            case 2:
                data = ((reg << 16) & 0xffff0000) | (data & 0xffff);
                break;

            case 3:
                data = ((reg << 24) & 0xff000000) | (data & 0xffffff);
                break;
        }

        (Memory.getInstance()).write32(address & 0xfffffffc, data);
    }

    @Override
    public void doCACHE(int code, int rs, int simm16) {
        // TODO
        doUNK("Unsupported cache instruction");
    }

    @Override
    public void doLL(int rt, int rs, int simm16) {
        int word = (Memory.getInstance()).read32(gpr[rs] + simm16);
        if (rt != 0) {
            gpr[rt] = word;
        }
    //ll_bit = 1;
    }

    @Override
    public void doLWC1(int ft, int rs, int simm16) {
        fpr[ft] = Float.intBitsToFloat((Memory.getInstance()).read32(gpr[rs] + simm16));
        cycles = Math.max(cycles, fpr_cycles[ft]);
        fpr_cycles[ft] = cycles + 1;
    }

    @Override
    public void doLVS(int vt, int rs, int simm14) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;
        vpr[m][c][r] = Float.intBitsToFloat((Memory.getInstance()).read32(gpr[rs] + (simm14 << 2)));
        cycles = Math.max(cycles, vpr_cycles[m][r][c]);
        vpr_cycles[m][r][c] = cycles + 3;
    }

    @Override
    public void doSC(int rt, int rs, int simm16) {
        (Memory.getInstance()).write32(gpr[rs] + simm16, gpr[rt]);
        if (rt != 0) {
            gpr[rt] = 1; // = ll_bit;
        }
    }

    @Override
    public void doSWC1(int ft, int rs, int simm16) {
        (Memory.getInstance()).write32(gpr[rs] + simm16, Float.floatToRawIntBits(fpr[ft]));
        cycles = Math.max(cycles, fpr_cycles[ft]);
    }

    @Override
    public void doSVS(int vt, int rs, int simm14) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;
        (Memory.getInstance()).write32(gpr[rs] + (simm14 << 2), Float.floatToRawIntBits(vpr[m][r][c]));
        cycles = Math.max(cycles, vpr_cycles[m][r][c]);
    }

    @Override
    public void doROTR(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = Integer.rotateRight(gpr[rt], sa);
        }
    }

    @Override
    public void doROTRV(int rd, int rt, int rs) {
        if (rd != 0) {
            // no need of "gpr[rs] & 31", rotateRight does it for us
            gpr[rd] = Integer.rotateRight(gpr[rt], gpr[rs]);
        }
    }

    @Override
    public void doMOVZ(int rd, int rs, int rt) {
        if ((rd != 0) && (gpr[rt] == 0)) {
            gpr[rd] = gpr[rs];
        }
    }

    @Override
    public void doMOVN(int rd, int rs, int rt) {
        if ((rd != 0) && (gpr[rt] != 0)) {
            gpr[rd] = gpr[rs];
        }
    }

    @Override
    public void doSYSCALL(int code) {
        // TODO
        // cop0.epc = pc - 4;
        // cop0.cause.exc |= SYSCALL_EXC;
        // npc = cop0.exception_handler;
        SyscallHandler.syscall(code);
    }

    @Override
    public void doBREAK(int code) {
        // TODO
        // cop0.debug_epc = pc - 4;
        // cop0.cause.exc |= BREAK_EXC;
        // npc = cop0.exception_handler;
        System.out.println("Unsupported break instruction");
    }

    @Override
    public void doSYNC() {
        cycles += 6;
    }

    @Override
    public void doCLZ(int rd, int rs) {
        if (rd != 0) {
            gpr[rd] = Integer.numberOfLeadingZeros(gpr[rs]);
        }
    }

    @Override
    public void doCLO(int rd, int rs) {
        if (rd != 0) {
            gpr[rd] = Integer.numberOfLeadingZeros(~gpr[rs]);
        }
    }

    @Override
    public void doMADD(int rs, int rt) {
        hilo += ((long) gpr[rs]) * ((long) gpr[rt]);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 4;
    }

    @Override
    public void doMADDU(int rs, int rt) {
        hilo += (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 4;
    }

    @Override
    public void doMAX(int rd, int rs, int rt) {
        if (rd != 0) {
            int x = gpr[rs];
            int y = gpr[rt];
            gpr[rd] = (x > y) ? x : y;
        }
    }

    @Override
    public void doMIN(int rd, int rs, int rt) {
        if (rd != 0) {
            int x = gpr[rs];
            int y = gpr[rt];
            gpr[rd] = (x < y) ? x : y;
        }
    }

    @Override
    public void doMSUB(int rs, int rt) {
        hilo -= ((long) gpr[rs]) * ((long) gpr[rt]);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 4;
    }

    @Override
    public void doMSUBU(int rs, int rt) {
        hilo -= (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
        if (cycles < hilo_cycles) {
            cycles = hilo_cycles;
        }
        hilo_cycles = cycles + 4;
    }

    @Override
    public void doEXT(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
            int mask = ~(~0 << (rd + 1));
            gpr[rt] = (gpr[rs] >>> sa) & mask;
        }
    }

    @Override
    public void doINS(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
            int mask = ~(~0 << (rd - sa + 1)) << sa;
            gpr[rt] = (gpr[rt] & ~mask) | ((gpr[rs] << sa) & mask);
        }
    }

    @Override
    public void doWSBH(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = Integer.rotateRight(Integer.reverseBytes(gpr[rt]), 16);
        }
    }

    @Override
    public void doWSBW(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = Integer.reverseBytes(gpr[rt]);
        }
    }

    @Override
    public void doSEB(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] << 24) >> 24;
        }
    }

    @Override
    public void doBITREV(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = Integer.reverse(gpr[rt]);
        }
    }

    @Override
    public void doSEH(int rd, int rt) {
        gpr[rd] = (gpr[rt] << 16) >> 16;
    }

    @Override
    public void doMFC1(int rt, int c1dr) {
        gpr[rt] = Float.floatToRawIntBits(fpr[c1dr]);
        cycles = Math.max(cycles, fpr_cycles[c1dr]);
    }

    @Override
    public void doCFC1(int rt, int c1cr) {
        if (rt != 0) {
            switch (c1cr) {
                case 0:
                    gpr[rt] = (fcr0_imp << 8) | (fcr0_rev);
                    break;

                case 31:
                    gpr[rt] = (fcr31_fs ? (1 << 24) : 0) | (fcr31_c ? (1 << 23) : 0) | (fcr31_rm & 3);
                    cycles = Math.max(cycles, fcr31_cycles);
                    break;

                default:
                    doUNK("Unsupported cfc1 instruction for fcr" + Integer.toString(c1cr));
            }
        }
    }

    @Override
    public void doMTC1(int rt, int c1dr) {
        fpr[c1dr] = Float.intBitsToFloat(gpr[rt]);
        cycles = Math.max(cycles, fpr_cycles[c1dr]);
        fpr_cycles[c1dr] = cycles + 1;
    }

    @Override
    public void doCTC1(int rt, int c1cr) {
        switch (c1cr) {
            case 31:
                int bits = gpr[rt] & 0x01800003;
                fcr31_rm = bits & 3;
                bits >>= 23;
                fcr31_fs = (bits > 1);
                fcr31_c = (bits >> 1) == 1;
                cycles = Math.max(cycles, fcr31_cycles);
                fcr31_cycles = cycles + 1;
                break;

            default:
                doUNK("Unsupported ctc1 instruction for fcr" + Integer.toString(c1cr));
        }
    }

    @Override
    public void doBC1F(int simm16) {
        long target_cycles = Math.max(cycles + 3, fcr31_cycles);
        npc = !fcr31_c ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBC1T(int simm16) {
        long target_cycles = Math.max(cycles + 3, fcr31_cycles);
        npc = fcr31_c ? branchTarget(pc, simm16) : (pc + 4);
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBC1FL(int simm16) {
        long target_cycles = Math.max(cycles + 3, fcr31_cycles);
        if (!fcr31_c) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles = target_cycles;
        }
    }

    @Override
    public void doBC1TL(int simm16) {
        long target_cycles = Math.max(cycles + 3, fcr31_cycles);
        if (fcr31_c) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
            pc += 4;
            cycles = target_cycles;
        }
    }

    @Override
    public void doADDS(int fd, int fs, int ft) {
        fpr[fd] = fpr[fs] + fpr[ft];
        updateCyclesFdFsFt(fd, fs, ft, 3);
    }

    @Override
    public void doSUBS(int fd, int fs, int ft) {
        fpr[fd] = fpr[fs] - fpr[ft];
        updateCyclesFdFsFt(fd, fs, ft, 3);
    }

    @Override
    public void doMULS(int fd, int fs, int ft) {
        fpr[fd] = fpr[fs] * fpr[ft];
        updateCyclesFdFsFt(fd, fs, ft, 6);
    }

    @Override
    public void doDIVS(int fd, int fs, int ft) {
        fpr[fd] = fpr[fs] / fpr[ft];
        updateCyclesFdFsFt(fd, fs, ft, 27);
    }

    @Override
    public void doSQRTS(int fd, int fs) {
        fpr[fd] = (float) Math.sqrt(fpr[fs]);
        updateCyclesFdFs(fd, fs, 27);
    }

    @Override
    public void doABSS(int fd, int fs) {
        fpr[fd] = Math.abs(fpr[fs]);
        fpr_cycles[fd] = cycles;
    }

    @Override
    public void doMOVS(int fd, int fs) {
        fpr[fd] = fpr[fs];
        fpr_cycles[fd] = cycles;
    }

    @Override
    public void doNEGS(int fd, int fs) {
        fpr[fd] = 0.0f - fpr[fs];
        fpr_cycles[fd] = cycles;
    }

    @Override
    public void doROUNDWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat(Math.round(fpr[fs]));
        updateCyclesFdFs(fd, fs, 3);
    }

    @Override
    public void doTRUNCWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat((int) (fpr[fs]));
        updateCyclesFdFs(fd, fs, 3);
    }

    @Override
    public void doCEILWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat((int) Math.ceil(fpr[fs]));
        updateCyclesFdFs(fd, fs, 3);
    }

    @Override
    public void doFLOORWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat((int) Math.floor(fpr[fs]));
        updateCyclesFdFs(fd, fs, 3);
    }

    @Override
    public void doCVTSW(int fd, int fs) {
        fpr[fd] = (float) Float.floatToRawIntBits(fpr[fs]);
        updateCyclesFdFs(fd, fs, 5);
    }

    @Override
    public void doCVTWS(int fd, int fs) {
        switch (fcr31_rm) {
            case 1:
                fpr[fd] = Float.intBitsToFloat((int) (fpr[fs]));
                break;
            case 2:
                fpr[fd] = Float.intBitsToFloat((int) Math.ceil(fpr[fs]));
                break;
            case 3:
                fpr[fd] = Float.intBitsToFloat((int) Math.floor(fpr[fs]));
                break;
            default:
                fpr[fd] = Float.intBitsToFloat((int) Math.rint(fpr[fs]));
                break;
        }
        updateCyclesFdFs(fd, fs, 3);
    }

    @Override
    public void doCCONDS(int fs, int ft, int cond) {
        float x = fpr[fs];
        float y = fpr[ft];
        boolean unordered = ((cond & 1) != 0) && (Float.isNaN(x) || Float.isNaN(y));

        if (unordered) {
            if ((cond & 8) != 0) {
                doUNK("C.cond.S instruction raises an Unordered exception");
            }

            fcr31_c = true;
        } else {
            boolean equal = ((cond & 2) != 0) && (x == y);
            boolean less = ((cond & 4) != 0) && (x < y);

            fcr31_c = less || equal;
        }

        updateCyclesFsFt(fs, ft, 1);
    }
    // VFPU stuff
    private float transformVr(int swz, boolean abs, boolean cst, boolean neg, float[] x) {
        float value = 0.0f;
        if (cst) {
            switch (swz) {
                case 0:
                    value = abs ? 0.0f : 3.0f;
                    break;
                case 1:
                    value = abs ? 1.0f : (1.0f / 3.0f);
                    break;
                case 2:
                    value = abs ? 2.0f : (1.0f / 4.0f);
                    break;
                case 3:
                    value = abs ? 0.5f : (1.0f / 6.0f);
                    break;
            }
        } else {
            value = x[swz];
        }

        if (abs) {
            value = Math.abs(value);
        }
        return neg ? (0.0f - value) : value;
    }

    private float applyPrefixVs(int i, float[] x) {
        return transformVr(vcr_pfxs_swz[i], vcr_pfxs_abs[i], vcr_pfxs_cst[i], vcr_pfxs_neg[i], x);
    }

    private float applyPrefixVt(int i, float[] x) {
        return transformVr(vcr_pfxt_swz[i], vcr_pfxt_abs[i], vcr_pfxt_cst[i], vcr_pfxt_neg[i], x);
    }

    private float applyPrefixVd(int i, float value) {
        switch (vcr_pfxd_sat[i]) {
            case 1:
                return Math.max(0.0f, Math.min(1.0f, value));
            case 3:
                return Math.max(-1.0f, Math.min(1.0f, value));
        }
        return value;
    }

    private float[] loadVs(int vsize, int vs) {
        float[] result = new float[vsize];

        int m, r, c;

        m = (vs >> 2) & 7;
        c = (vs >> 0) & 3;

        switch (vsize) {
            case 1:
                r = (vs >> 5) & 3;
                result[0] = vpr[m][c][r];
                if (vcr_pfxs) {
                    result[0] = applyPrefixVs(0, result);
                    vcr_pfxs = false;
                }
                return result;

            case 2:
                r = (vs & 64) >> 5;
                if ((vs & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                }
                if (vcr_pfxs) {
                    result[0] = applyPrefixVs(0, result);
                    result[1] = applyPrefixVs(1, result);
                    vcr_pfxs = false;
                }
                return result;

            case 3:
                r = (vs & 64) >> 6;
                if ((vs & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                    result[2] = vpr[m][r + 2][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                    result[2] = vpr[m][c][r + 2];
                }
                if (vcr_pfxs) {
                    result[0] = applyPrefixVs(0, result);
                    result[1] = applyPrefixVs(1, result);
                    result[2] = applyPrefixVs(2, result);
                    vcr_pfxs = false;
                }
                return result;

            case 4:
                if ((vs & 32) != 0) {
                    result[0] = vpr[m][0][c];
                    result[1] = vpr[m][1][c];
                    result[2] = vpr[m][2][c];
                    result[3] = vpr[m][3][c];
                } else {
                    result[0] = vpr[m][c][0];
                    result[1] = vpr[m][c][1];
                    result[2] = vpr[m][c][2];
                    result[3] = vpr[m][c][3];
                }
                if (vcr_pfxs) {
                    result[0] = applyPrefixVs(0, result);
                    result[1] = applyPrefixVs(1, result);
                    result[2] = applyPrefixVs(2, result);
                    result[3] = applyPrefixVs(3, result);
                    vcr_pfxs = false;
                }
                return result;

            default:
        }
        return null;
    }

    private float[] loadVt(int vsize, int vt) {
        float[] result = new float[vsize];

        int m, r, c;

        m = (vt >> 2) & 7;
        c = (vt >> 0) & 3;

        switch (vsize) {
            case 1:
                r = (vt >> 5) & 3;
                result[0] = vpr[m][c][r];
                if (vcr_pfxt) {
                    result[0] = applyPrefixVt(0, result);
                    vcr_pfxt = false;
                }
                return result;

            case 2:
                r = (vt & 64) >> 5;
                if ((vt & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                }
                if (vcr_pfxt) {
                    result[0] = applyPrefixVt(0, result);
                    result[1] = applyPrefixVt(1, result);
                    vcr_pfxt = false;
                }
                return result;

            case 3:
                r = (vt & 64) >> 6;
                if ((vt & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                    result[2] = vpr[m][r + 2][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                    result[2] = vpr[m][c][r + 2];
                }
                if (vcr_pfxt) {
                    result[0] = applyPrefixVt(0, result);
                    result[1] = applyPrefixVt(1, result);
                    result[2] = applyPrefixVt(2, result);
                    vcr_pfxt = false;
                }
                return result;

            case 4:
                if ((vt & 32) != 0) {
                    result[0] = vpr[m][0][c];
                    result[1] = vpr[m][1][c];
                    result[2] = vpr[m][2][c];
                    result[3] = vpr[m][3][c];
                } else {
                    result[0] = vpr[m][c][0];
                    result[1] = vpr[m][c][1];
                    result[2] = vpr[m][c][2];
                    result[3] = vpr[m][c][3];
                }
                if (vcr_pfxt) {
                    result[0] = applyPrefixVt(0, result);
                    result[1] = applyPrefixVt(1, result);
                    result[2] = applyPrefixVt(2, result);
                    result[3] = applyPrefixVt(3, result);
                    vcr_pfxt = false;
                }
                return result;

            default:
        }
        return null;
    }

    private void saveVd(int vsize, int vd, float[] result) {
        int m, r, c;

        m = (vd >> 2) & 7;
        c = (vd >> 0) & 3;

        switch (vsize) {
            case 1:
                r = (vd >> 5) & 3;
                if (vcr_pfxd) {
                    if (!vcr_pfxd_msk[0]) {
                        vpr[m][c][r] = applyPrefixVd(0, result[0]);
                    }
                    vcr_pfxd = false;
                } else {
                    vpr[m][c][r] = result[0];
                }
                break;

            case 2:
                r = (vd & 64) >> 5;
                if (vcr_pfxd) {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 2; ++i) {
                            if (!vcr_pfxd_msk[i]) {
                                vpr[m][r + i][c] = applyPrefixVd(i, result[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < 2; ++i) {
                            if (!vcr_pfxd_msk[i]) {
                                vpr[m][c][r + i] = applyPrefixVd(i, result[i]);
                            }
                        }
                    }
                    vcr_pfxd = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 2; ++i) {
                            vpr[m][r + i][c] = result[i];
                        }
                    } else {
                        for (int i = 0; i < 2; ++i) {
                            vpr[m][c][r + i] = result[i];
                        }
                    }
                }
                break;

            case 3:
                r = (vd & 64) >> 6;
                if (vcr_pfxd) {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 3; ++i) {
                            if (!vcr_pfxd_msk[i]) {
                                vpr[m][r + i][c] = applyPrefixVd(i, result[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < 3; ++i) {
                            if (!vcr_pfxd_msk[i]) {
                                vpr[m][c][r + i] = applyPrefixVd(i, result[i]);
                            }
                        }
                    }
                    vcr_pfxd = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 3; ++i) {
                            vpr[m][r + i][c] = result[i];
                        }
                    } else {
                        for (int i = 0; i < 3; ++i) {
                            vpr[m][c][r + i] = result[i];
                        }
                    }
                }
                break;

            case 4:
                if (vcr_pfxd) {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 4; ++i) {
                            if (!vcr_pfxd_msk[i]) {
                                vpr[m][i][c] = applyPrefixVd(i, result[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < 4; ++i) {
                            if (!vcr_pfxd_msk[i]) {
                                vpr[m][c][i] = applyPrefixVd(i, result[i]);
                            }
                        }
                    }
                    vcr_pfxd = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 4; ++i) {
                            vpr[m][i][c] = result[i];
                        }
                    } else {
                        for (int i = 0; i < 4; ++i) {
                            vpr[m][c][i] = result[i];
                        }
                    }
                }
                break;

            default:
                break;
        }
    }
// VFPU0
    @Override
    public void doVADD(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] += x2[i];
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVSUB(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] -= x2[i];
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVSBN(int vsize, int vd, int vs, int vt) {
        if (vsize != 1) {
            doUNK("Only supported VSBN.S instruction");
        }

        float[] x1 = loadVs(1, vs);
        float[] x2 = loadVt(1, vt);

        x1[0] = Math.scalb(x1[0], Float.floatToRawIntBits(x2[0]));

        saveVd(1, vd, x1);
    }

    @Override
    public void doVDIV(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] /= x2[i];
        }

        saveVd(vsize, vd, x1);
    }
// VFPU1
    @Override
    public void doVMUL(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] *= x2[i];
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVDOT(int vsize, int vd, int vs, int vt) {
        if (vsize == 1) {
            doUNK("Unsupported VDOT.S instruction");
        }

        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);
        float[] x3 = new float[1];

        for (int i = 0; i < vsize; ++i) {
            x3[0] += x1[i] * x2[i];
        }

        saveVd(1, vd, x3);
    }

    @Override
    public void doVSCL(int vsize, int vd, int vs, int vt) {
        doUNK("Not yet supported VFPU instruction");
    }

    @Override
    public void doVHDP(int vsize, int vd, int vs, int vt) {
        if (vsize == 1) {
            doUNK("Unsupported VHDP.S instruction");
        }

        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);
        float[] x3 = new float[1];

        int i;
        for (i = 0; i < vsize - 1; ++i) {
            x3[0] += x1[i] * x2[i];
        }

        x3[0] += x2[i];

        saveVd(1, vd, x3);
    }

    @Override
    public void doVCRS(int vsize, int vd, int vs, int vt) {
        if (vsize != 3) {
            doUNK("Only supported VCRS.T instruction");
        }

        float[] x1 = loadVs(3, vs);
        float[] x2 = loadVt(3, vt);
        float[] x3 = new float[3];

        x3[0] = x1[1] * x2[2];
        x3[1] = x1[2] * x2[0];
        x3[2] = x1[0] * x2[1];

        saveVd(3, vd, x3);
    }

    @Override
    public void doVDET(int vsize, int vd, int vs, int vt) {
        if (vsize != 2) {
            doUNK("Only supported VDET.P instruction");
        }

        float[] x1 = loadVs(2, vs);
        float[] x2 = loadVt(2, vt);
        float[] x3 = new float[1];

        x3[0] = x1[0] * x2[1] - x1[1] * x2[0];

        saveVd(1, vd, x3);
    }

// VFPU3
    @Override
    public void doVCMP(int vsize, int vs, int vt, int cond) {
        boolean cc_or = false;
        boolean cc_and = true;

        if ((cond & 8) == 0) {
            boolean not = ((cond & 4) == 4);

            boolean cc = false;

            float[] x1 = loadVs(vsize, vs);
            float[] x2 = loadVt(vsize, vt);

            for (int i = 0; i < vsize; ++i) {
                switch (cond & 3) {
                    case 0:
                        cc = not;
                        break;

                    case 1:
                        cc = not ? (x1[i] != x2[i]) : (x1[i] == x2[i]);
                        break;

                    case 2:
                        cc = not ? (x1[i] >= x2[i]) : (x1[i] < x2[i]);
                        break;

                    case 3:
                        cc = not ? (x1[i] > x2[i]) : (x1[i] <= x2[i]);
                        break;

                }


                vcr_cc[i] = cc;
                cc_or =
                        cc_or || cc;
                cc_and =
                        cc_and && cc;
            }

        } else {
            float[] x1 = loadVs(vsize, vs);

            for (int i = 0; i < vsize; ++i) {
                boolean cc;
                if ((cond & 3) == 0) {
                    cc = ((cond & 4) == 0) ? (x1[i] == 0.0f) : (x1[i] != 0.0f);
                } else {
                    cc = (((cond & 1) == 1) && Float.isNaN(x1[i])) ||
                            (((cond & 2) == 2) && Float.isInfinite(x1[i]));
                    if ((cond & 4) == 4) {
                        cc = !cc;
                    }

                }
                vcr_cc[i] = cc;
                cc_or =
                        cc_or || cc;
                cc_and =
                        cc_and && cc;
            }

        }
        vcr_cc[4] = cc_or;
        vcr_cc[5] = cc_and;
    }

    @Override
    public void doVMIN(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = Math.min(x1[i], x2[i]);
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVMAX(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = Math.max(x1[i], x2[i]);
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVSCMP(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = Math.signum(x1[i] - x2[i]);
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVSGE(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = (x1[i] >= x2[i]) ? 1.0f : 0.0f;
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVSLT(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = (x1[i] < x2[i]) ? 1.0f : 0.0f;
        }

        saveVd(vsize, vd, x1);
    }

    @Override
    public void doVPFXS(int imm24) {
        vcr_pfxs_swz[0] = (imm24 >> 0) & 3;
        vcr_pfxs_swz[1] = (imm24 >> 2) & 3;
        vcr_pfxs_swz[2] = (imm24 >> 4) & 3;
        vcr_pfxs_swz[3] = (imm24 >> 6) & 3;
        vcr_pfxs_abs[0] = (imm24 >> 8) != 0;
        vcr_pfxs_abs[1] = (imm24 >> 9) != 0;
        vcr_pfxs_abs[2] = (imm24 >> 10) != 0;
        vcr_pfxs_abs[3] = (imm24 >> 11) != 0;
        vcr_pfxs_cst[0] = (imm24 >> 12) != 0;
        vcr_pfxs_cst[1] = (imm24 >> 13) != 0;
        vcr_pfxs_cst[2] = (imm24 >> 14) != 0;
        vcr_pfxs_cst[3] = (imm24 >> 15) != 0;
        vcr_pfxs_neg[0] = (imm24 >> 16) != 0;
        vcr_pfxs_neg[1] = (imm24 >> 17) != 0;
        vcr_pfxs_neg[2] = (imm24 >> 18) != 0;
        vcr_pfxs_neg[3] = (imm24 >> 19) != 0;
        vcr_pfxs = true;
    }

    @Override
    public void doVPFXT(int imm24) {
        vcr_pfxt_swz[0] = (imm24 >> 0) & 3;
        vcr_pfxt_swz[1] = (imm24 >> 2) & 3;
        vcr_pfxt_swz[2] = (imm24 >> 4) & 3;
        vcr_pfxt_swz[3] = (imm24 >> 6) & 3;
        vcr_pfxt_abs[0] = (imm24 >> 8) != 0;
        vcr_pfxt_abs[1] = (imm24 >> 9) != 0;
        vcr_pfxt_abs[2] = (imm24 >> 10) != 0;
        vcr_pfxt_abs[3] = (imm24 >> 11) != 0;
        vcr_pfxt_cst[0] = (imm24 >> 12) != 0;
        vcr_pfxt_cst[1] = (imm24 >> 13) != 0;
        vcr_pfxt_cst[2] = (imm24 >> 14) != 0;
        vcr_pfxt_cst[3] = (imm24 >> 15) != 0;
        vcr_pfxt_neg[0] = (imm24 >> 16) != 0;
        vcr_pfxt_neg[1] = (imm24 >> 17) != 0;
        vcr_pfxt_neg[2] = (imm24 >> 18) != 0;
        vcr_pfxt_neg[3] = (imm24 >> 19) != 0;
        vcr_pfxt = true;
    }

    @Override
    public void doVPFXD(int imm24) {
        vcr_pfxd_sat[0] = (imm24 >> 0) & 3;
        vcr_pfxd_sat[1] = (imm24 >> 2) & 3;
        vcr_pfxd_sat[2] = (imm24 >> 4) & 3;
        vcr_pfxd_sat[3] = (imm24 >> 6) & 3;
        vcr_pfxd_msk[0] = (imm24 >> 8) != 0;
        vcr_pfxd_msk[1] = (imm24 >> 9) != 0;
        vcr_pfxd_msk[2] = (imm24 >> 10) != 0;
        vcr_pfxd_msk[3] = (imm24 >> 11) != 0;
        vcr_pfxd = true;
    }

    @Override
    public void doVIIM(int vs, int imm16) {
        float[] result = new float[1];

        result[0] = (float) imm16;

        saveVd(1, vs, result);
    }

    @Override
    public void doVFIM(int vs, int imm16) {
        float[] result = new float[1];

        float s = ((imm16 >> 15) == 0) ? 1.0f : -1.0f;
        int e = ((imm16 >> 10) & 0x1f);
        int m = (e == 0) ? ((imm16 & 0x3ff) << 1) : ((imm16 & 0x3ff) | 0x400);

        result[0] = s * ((float) m) * ((float) (1 << e)) / ((float) (1 << 41));

        saveVd(1, vs, result);
    }

    void testOpcodes() {
        gpr[1] = +1;
        gpr[2] = +2;
        gpr[3] = +3;
        gpr[5] = -1;
        gpr[6] = -2;
        gpr[7] = -3;
        gpr[8] = 32;
        gpr[9] = 255;
        gpr[10] = 65535;

        doSLL(4, 1, 1);
        assert (gpr[4] == 2);
        doSRL(4, 2, 1);
        assert (gpr[4] == 1);
        doSRL(4, 5, 1);
        assert (gpr[4] == 0x7fffffff);
        doSRA(4, 2, 1);
        assert (gpr[4] == 1);
        doSRA(4, 5, 1);
        assert (gpr[4] == -1);
        doSLLV(4, 1, 1);
        assert (gpr[4] == 2);
        doSRLV(4, 2, 1);
        assert (gpr[4] == 1);
        doSRLV(4, 5, 1);
        assert (gpr[4] == 0x7fffffff);
        doSRAV(4, 2, 1);
        assert (gpr[4] == 1);
        doSRAV(4, 5, 1);
        assert (gpr[4] == -1);
        doADDU(4, 3, 1);
        assert (gpr[4] == 4);
        doSUBU(4, 3, 1);
        assert (gpr[4] == 2);
        doAND(4, 5, 6);
        assert (gpr[4] == -2);
        doOR(4, 5, 6);
        assert (gpr[4] == -1);
        doXOR(4, 5, 6);
        assert (gpr[4] == 1);
        doNOR(4, 0, 1);
        assert (gpr[4] == -2);
        doSLT(4, 5, 6);
        assert (gpr[4] == 0);
        doSLT(4, 6, 5);
        assert (gpr[4] == 1);
        doSLT(4, 5, 5);
        assert (gpr[4] == 0);
        doSLT(4, 5, 1);
        assert (gpr[4] == 1);
        doSLTU(4, 5, 6);
        assert (gpr[4] == 0);
        doSLTU(4, 6, 5);
        assert (gpr[4] == 1);
        doSLTU(4, 5, 5);
        assert (gpr[4] == 0);
        doSLTU(4, 5, 1);
        assert (gpr[4] == 0);
        doADDIU(4, 1, 1);
        assert (gpr[4] == 2);
        doSLTI(4, 5, -2);
        assert (gpr[4] == 0);
        doSLTI(4, 6, -1);
        assert (gpr[4] == 1);
        doSLTI(4, 5, -1);
        assert (gpr[4] == 0);
        doSLTI(4, 5, 1);
        assert (gpr[4] == 1);
        doSLTIU(4, 5, -2);
        assert (gpr[4] == 0);
        doSLTIU(4, 6, -1);
        assert (gpr[4] == 1);
        doSLTIU(4, 5, -1);
        assert (gpr[4] == 0);
        doSLTIU(4, 5, 1);
        assert (gpr[4] == 0);
        doANDI(4, 5, -2);
        assert (gpr[4] == -2);
        doORI(4, 5, -2);
        assert (gpr[4] == -1);
        doXORI(4, 5, -2);
        assert (gpr[4] == 1);
        doLUI(4, 1);
        assert (gpr[4] == 0x00010000);
        doROTR(4, 1, 1);
        assert (gpr[4] == 0x80000000);
        doROTRV(4, 1, 1);
        assert (gpr[4] == 0x80000000);
        doROTR(4, 1, 32);
        assert (gpr[4] == 1);
        doROTRV(4, 1, 8);
        assert (gpr[4] == 1);
        gpr[4] = 0;
        doMOVZ(4, 1, 1);
        assert (gpr[4] == 0);
        gpr[4] = 0;
        doMOVZ(4, 1, 0);
        assert (gpr[4] == 1);
        gpr[4] = 0;
        doMOVN(4, 1, 1);
        assert (gpr[4] == 1);
        gpr[4] = 0;
        doMOVN(4, 1, 0);
        assert (gpr[4] == 0);
        doCLZ(4, 0);
        assert (gpr[4] == 32);
        doCLZ(4, 5);
        assert (gpr[4] == 0);
        doCLO(4, 0);
        assert (gpr[4] == 0);
        doCLO(4, 5);
        assert (gpr[4] == 32);
        doMAX(4, 1, 5);
        assert (gpr[4] == 1);
        doMAX(4, 5, 1);
        assert (gpr[4] == 1);
        doMIN(4, 1, 5);
        assert (gpr[4] == -1);
        doMIN(4, 5, 1);
        assert (gpr[4] == -1);
        doEXT(4, 3, 1, 1);
        assert (gpr[4] == 1);
        doEXT(4, 5, 30, 1);
        assert (gpr[4] == 0x7fffffff);
        gpr[4] = -3;
        doINS(4, 5, 1, 1);
        assert (gpr[4] == -1);
        gpr[4] = -1;
        doINS(4, 0, 31, 1);
        assert (gpr[4] == 1);
        doWSBH(4, 3);
        assert (gpr[4] == 0x00000300);
        doWSBW(4, 3);
        assert (gpr[4] == 0x03000000);
        doBITREV(4, 2);
        assert (gpr[4] == 0x40000000);
        doSEB(4, 8);
        assert (gpr[4] == 32);
        doSEB(4, 9);
        assert (gpr[4] == -1);
        doSEH(4, 8);
        assert (gpr[4] == 32);
        doSEH(4, 9);
        assert (gpr[4] == 255);
        doSEH(4, 10);
        assert (gpr[4] == -1);
    }
}
