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

import static jpcsp.AllegrexInstructions.*;

public class Processor implements AllegrexInstructions {

    public int[]     gpr;
    public float[]   fpr;
    public float[]   vpr; 
    public long hilo;
    public int pc;
    public int cycles;

    Processor() {
        Memory.get_instance(); //intialize memory
        reset();
    }

    public void reset() {
        // intialize psp register
        pc = 0x00000000;
        hilo = 0;
        gpr = new int[32]; 
        fpr = new float[32];
        vpr = new float[128];
    }

    public int hi() {
        return (int) (hilo & 0xffffffff);
    }

    public int lo() {
        return (int) (hilo >>> 32);
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
        return (i - j) >> 31;
    }

    public static int unsignedCompare(int i, int j) {
        return ((i - j) ^ i ^ j) >> 31;
    }

    public static int branchTarget(int npc, int simm16) {
        return npc - 4 + (simm16 << 2);
    }

    public static int jumpTarget(int npc, int uimm26) {
        return (npc & 0xf0000000) | (uimm26 << 2);
    }

    private static boolean addSubOverflow(long value) {
        long tmp = value << (62 - 31);
        return ((tmp >>> 1) == (tmp & 1));
    }

    private final Decoder interpreter = new Decoder();

    public void stepCpu() {
        int insn = Memory.get_instance().read32(pc);

        cycles += 1;

        pc += 4;

        interpreter.process(this, insn);
    }

    public void doUNK(String reason) {
        System.out.println(reason);
    }

    public void doNOP() {
    }

    public void doSLL(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] << sa);
        }
    }

    public void doSRL(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] >>> sa);
        }
    }

    public void doSRA(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] >> sa);
        }
    }

    public void doSLLV(int rd, int rt, int rs) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] << (gpr[rs] & 31));
        }
    }

    public void doSRLV(int rd, int rt, int rs) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] >>> (gpr[rs] & 31));
        }
    }

    public void doSRAV(int rd, int rt, int rs) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] >>> (gpr[rs] & 31));
        }
    }

    public void doJR(int rs) {
        int previous_cycles = cycles;
        int target = gpr[rs];
        stepCpu();
        if (cycles - previous_cycles < 2) {
            cycles = previous_cycles + 2;
        }
        pc = target;
    }

    public void doJALR(int rd, int rs) {
        int previous_cycles = cycles;
        if (rd != 0) {
            gpr[rd] = pc + 4;
        }
        int target = gpr[rs];
        stepCpu();
        if (cycles - previous_cycles < 2) {
            cycles = previous_cycles + 2;
        }
        pc = target;
    }

    public void doMFHI(int rd) {
        if (rd != 0) {
            gpr[rd] = hi();
        }
    // cycles ?
    }

    public void doMTHI(int rs) {
        int hi = gpr[rs];
        hilo = (((long) hi) << 32) | (hilo & 0xffffffff);
    // cycles ?
    }

    public void doMFLO(int rd) {
        if (rd != 0) {
            gpr[rd] = lo();
        }
    // cycles ?
    }

    public void doMTLO(int rs) {
        int lo = gpr[rs];
        hilo = ((hilo >>> 32) << 32) | (((long) lo) & 0xffffffff);
    // cycles ?
    }

    public void doMULT(int rs, int rt) {
        hilo = ((long) gpr[rs]) * ((long) gpr[rs]);
        cycles += 4;
    }

    public void doMULTU(int rs, int rt) {
        hilo = (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rs]) & 0xffffffff);
        cycles += 4;
    }

    public void doDIV(int rs, int rt) {
        int lo = gpr[rs] / gpr[rt];
        int hi = gpr[rs] % gpr[rt];
        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
        cycles += 35;
    }

    public void doDIVU(int rs, int rt) {
        long x = ((long) gpr[rs]) & 0xffffffff;
        long y = ((long) gpr[rt]) & 0xffffffff;
        int lo = (int) (x / y);
        int hi = (int) (x % y);
        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
        cycles += 35;
    }

    public void doADD(int rd, int rs, int rt) {
        if (rd != 0) {
            long result = (long) gpr[rs] + (long) gpr[rt];

            if (!addSubOverflow(result)) {
                gpr[rd] = (int) result;
            } else {
                // TODO set exception overflow and break !!! (rd cannot be modify)
            }
        }
    }

    public void doADDU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = (int) ((((long) gpr[rs]) & 0xffffffff) + (((long) gpr[rt]) & 0xffffffff));
        }
    }

    public void doSUB(int rd, int rs, int rt) {
        if (rd != 0) {
            long result = (long) gpr[rs] - (long) gpr[rt];

            if (!addSubOverflow(result)) {
                gpr[rd] = (int) result;
            } else {
                // TODO set exception overflow and break !!! (rd cannot be modify)
            }
        }
    }

    public void doSUBU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = (int) ((((long) gpr[rs]) & 0xffffffff) - (((long) gpr[rt]) & 0xffffffff));
        }
    }

    public void doAND(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] & gpr[rt];
        }
    }

    public void doOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] | gpr[rt];
        }
    }

    public void doXOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] ^ gpr[rt];
        }
    }

    public void doNOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = ~(gpr[rs] | gpr[rt]);
        }
    }

    public void doSLT(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = signedCompare(gpr[rs], gpr[rt]);
        }
    }

    public void doSLTU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = unsignedCompare(gpr[rs], gpr[rt]);
        }
    }

    public void doBLTZ(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] < 0);
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBGEZ(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] >= 0);
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBLTZL(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] < 0);
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    public void doBGEZL(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] >= 0);
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    public void doBLTZAL(int rs, int simm16) {
        int previous_cycles = cycles;
        int target = pc + 4;
        boolean t = (gpr[rs] < 0);
        gpr[31] = target;
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBGEZAL(int rs, int simm16) {
        int previous_cycles = cycles;
        int target = pc + 4;
        boolean t = (gpr[rs] >= 0);
        gpr[31] = target;
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBLTZALL(int rs, int simm16) {
        int previous_cycles = cycles;
        int target = pc + 4;
        boolean t = (gpr[rs] < 0);
        gpr[31] = target;
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc = target;
            cycles += 3;
        }
    }

    public void doBGEZALL(int rs, int simm16) {
        int previous_cycles = cycles;
        int target = pc + 4;
        boolean t = (gpr[rs] >= 0);
        gpr[31] = target;
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc = target;
            cycles += 3;
        }
    }

    public void doJ(int uimm26) {
        int previous_cycles = cycles;
        stepCpu();
        if (cycles - previous_cycles < 2) {
            cycles = previous_cycles + 2;
        }
        pc = jumpTarget(pc, uimm26);
    }

    public void doJAL(int uimm26) {
        int previous_cycles = cycles;
        int target = pc + 4;
        stepCpu();
        if (cycles - previous_cycles < 2) {
            cycles = previous_cycles + 2;
        }
        gpr[31] = target;
        pc = jumpTarget(pc, uimm26);
    }

    public void doBEQ(int rs, int rt, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] == gpr[rt]);
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBNE(int rs, int rt, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] != gpr[rt]);
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBLEZ(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] <= 0);
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBGTZ(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] > 0);
        stepCpu();
        if (cycles - previous_cycles < 3) {
            cycles = previous_cycles + 3;
        }
        if (t) {
            pc = branchTarget(pc, simm16);
        }
    }

    public void doBEQL(int rs, int rt, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] == gpr[rt]);
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    public void doBNEL(int rs, int rt, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] != gpr[rt]);
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    public void doBLEZL(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] <= 0);
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    public void doBGTZL(int rs, int simm16) {
        int previous_cycles = cycles;
        boolean t = (gpr[rs] > 0);
        if (t) {
            stepCpu();
            if (cycles - previous_cycles < 3) {
                cycles = previous_cycles + 3;
            }
            pc = branchTarget(pc, simm16);
        } else {
            pc += 4;
            cycles += 3;
        }
    }

    public void doADDI(int rt, int rs, int simm16) {
        if (rt != 0) {
            long result = (long) gpr[rs] + (long) simm16;

            if (!addSubOverflow(result)) {
                gpr[rt] = (int) result;
            } else {
                // TODO set exception overflow and break !!! (rd cannot be modify)
            }
        }
    }

    public void doADDIU(int rt, int rs, int simm16) {
        if (rt != 0) {
            gpr[rt] = (int) ((((long) gpr[rs]) & 0xffffffff) + (((long) simm16) & 0xffffffff));
        }
    }

    public void doSLTI(int rt, int rs, int simm16) {
        if (rt != 0) {
            gpr[rt] = signedCompare(gpr[rs], simm16);
        }
    }

    public void doSLTIU(int rt, int rs, int simm16) {
        if (rt != 0) {
            gpr[rt] = unsignedCompare(gpr[rs], simm16);
        }
    }

    public void doANDI(int rt, int rs, int uimm16) {
        if (rt != 0) {
            gpr[rt] = gpr[rs] & uimm16;
        }
    }

    public void doORI(int rt, int rs, int uimm16) {
        if (rt != 0) {
            gpr[rt] = gpr[rs] | uimm16;
        }
    }

    public void doXORI(int rt, int rs, int uimm16) {
        if (rt != 0) {
            gpr[rt] = gpr[rs] ^ uimm16;
        }
    }

    public void doLUI(int rt, int uimm16) {
        if (rt != 0) {
            gpr[rt] = uimm16 << 16;
        }
    }

    public void doHALT() {
        // TODO
    }

    public void doMFIC(int rt) {
        // TODO
    }

    public void doMTIC(int rt) {
        // TODO
    }

    public void doMFC0(int rt, int c0dr) {
        // TODO
    }

    public void doCFC0(int rt, int c0cr) {
        // TODO
    }

    public void doMTC0(int rt, int c0dr) {
        // TODO
    }

    public void doCTC0(int rt, int c0cr) {
        // TODO
    }

    public void doERET() {
        // TODO
    }

    public void doLB(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        gpr[rt] = (Memory.get_instance().read8(virtAddr) << 24) >> 24;
    }

    public void doLBU(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        gpr[rt] = Memory.get_instance().read8(virtAddr) & 0xff;
    }

    public void doLH(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        gpr[rt] = (Memory.get_instance().read16(virtAddr) << 16) >> 16;
    }

    public void doLHU(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        gpr[rt] = Memory.get_instance().read16(virtAddr) & 0xffff;
    }

    public void doLWL(int rt, int rs, int simm16) {
        // TODO
    }

    public void doLW(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        gpr[rt] = Memory.get_instance().read32(virtAddr);
    }

    public void doLWR(int rt, int rs, int simm16) {
        // TODO
    }

    public void doSB(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        Memory.get_instance().write8(virtAddr, (byte) (gpr[rt] & 0xFF));
    }

    public void doSH(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        Memory.get_instance().write16(virtAddr, (short) (gpr[rt] & 0xFFFF));
    }

    public void doSWL(int rt, int rs, int simm16) {
        // TODO
    }

    public void doSW(int rt, int rs, int simm16) {
        int virtAddr = gpr[rs] + simm16;
        Memory.get_instance().write32(virtAddr, gpr[rt]);
    }

    public void doSWR(int rt, int rs, int simm16) {
        // TODO
    }

    public void doCACHE(int code, int rs, int simm16) {
        // TODO
    }

    public void doLL(int rt, int rs, int simm16) {
        // TODO
    }

    public void doSC(int rt, int rs, int simm16) {
        // TODO
    }

    public void doROTR(int rd, int rt, int sa) {
        if (rd != 0) {
            int at = gpr[rt];

            gpr[rd] = (at >>> sa) | (at << (32 - sa));
        }
    }

    public void doROTRV(int rd, int rt, int rs) {
        doROTR(rd, rt, (gpr[rs] & 31));
    }

    public void doMOVZ(int rd, int rs, int rt) {
        if ((rd != 0) && (gpr[rt] == 0)) {
            gpr[rd] = gpr[rs];
        }
    }

    public void doMOVN(int rd, int rs, int rt) {
        if ((rd != 0) && (gpr[rt] != 0)) {
            gpr[rd] = gpr[rs];
        }
    }

    public void doSYSCALL(int code) {
        // TODO
    }

    public void doBREAK(int code) {
        // TODO
    }

    public void doSYNC() {
        cycles += 6;
    }

    public void doCLZ(int rd, int rs) {
        if (rd != 0) {
            int count = 32;
            int value = gpr[rs];
            int i = 31;

            do {
                if (((value >>> i) & 1) == 1) {
                    count = 31 - i;
                }
            } while (count == 32 && i-- != 0);

            gpr[rd] = count;
        }
    }

    public void doCLO(int rd, int rs) {
        if (rd != 0) {
            int count = 32;
            int value = gpr[rs];
            int i = 31;

            do {
                if (((value >>> i) & 1) == 0) {
                    count = 31 - i;
                }
            } while (count == 32 && i-- != 0);

            gpr[rd] = count;
        }
    }

    public void doMADD(int rs, int rt) {
        hilo += ((long) gpr[rs]) * ((long) gpr[rs]);
        cycles += 4;
    }

    public void doMADDU(int rs, int rt) {
        hilo += (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rs]) & 0xffffffff);
        cycles += 4;
    }

    public void doMAX(int rd, int rs, int rt) {
        if (rd != 0) {
            int x = gpr[rs];
            int y = gpr[rt];
            gpr[rd] = (x > y) ? x : y;
        }
    }

    public void doMIN(int rd, int rs, int rt) {
        if (rd != 0) {
            int x = gpr[rs];
            int y = gpr[rt];
            gpr[rd] = (x < y) ? x : y;
        }
    }

    public void doMSUB(int rs, int rt) {
        hilo -= ((long) gpr[rs]) * ((long) gpr[rs]);
        cycles += 4;
    }

    public void doMSUBU(int rs, int rt) {
        hilo -= (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rs]) & 0xffffffff);
        cycles += 4;
    }

    public void doEXT(int rt, int rs, int rd, int sa) {
        int mask = ~(~1 << rd);
        gpr[rd] = (gpr[rt] >> sa) & mask;
    }

    public void doINS(int rt, int rs, int rd, int sa) {
        int mask1 = ~(~0 << sa);
        int mask2 = (~0 << rd);
        int mask3 = mask1 | mask2;
        gpr[rd] = (gpr[rt] & mask3) | ((gpr[rs] >> sa) & mask2);
    }

    public void doWSBH(int rd, int rt) {
        if (rd != 0) {
            int x = gpr[rt];
            int y = 0;
            y |= (x & 0x000000ff) << 8;
            y |= (x & 0x0000ff00) << 0;
            y |= (x & 0x00ff0000) << 24;
            y |= (x & 0xff000000) << 16;
            gpr[rd] = y;
        }
    }

    public void doWSBW(int rd, int rt) {
        if (rd != 0) {
            int x = gpr[rt];
            int y = 0;
            y |= (x & 0x000000ff) << 24;
            y |= (x & 0x0000ff00) << 16;
            y |= (x & 0x00ff0000) << 8;
            y |= (x & 0xff000000) << 0;
            gpr[rd] = y;
        }
    }

    public void doSEB(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] << 24) >> 24;
        }
    }

    public void doBITREV(int rd, int rt) {
        if (rd != 0) {
            int x = gpr[rt];
            int y = 0;
            y |= (x & 0x00000001) << 31;
            y |= (x & 0x00000002) << 30;
            y |= (x & 0x00000004) << 29;
            y |= (x & 0x00000008) << 28;
            y |= (x & 0x00000010) << 27;
            y |= (x & 0x00000020) << 26;
            y |= (x & 0x00000040) << 25;
            y |= (x & 0x00000080) << 24;
            y |= (x & 0x00000100) << 23;
            y |= (x & 0x00000200) << 22;
            y |= (x & 0x00000400) << 21;
            y |= (x & 0x00000800) << 20;
            y |= (x & 0x00001000) << 19;
            y |= (x & 0x00002000) << 18;
            y |= (x & 0x00004000) << 17;
            y |= (x & 0x00008000) << 16;
            y |= (x & 0x00010000) << 15;
            y |= (x & 0x00020000) << 14;
            y |= (x & 0x00040000) << 13;
            y |= (x & 0x00080000) << 12;
            y |= (x & 0x00100000) << 11;
            y |= (x & 0x00200000) << 10;
            y |= (x & 0x00400000) << 9;
            y |= (x & 0x00800000) << 8;
            y |= (x & 0x01000000) << 7;
            y |= (x & 0x02000000) << 6;
            y |= (x & 0x04000000) << 5;
            y |= (x & 0x08000000) << 4;
            y |= (x & 0x10000000) << 3;
            y |= (x & 0x20000000) << 2;
            y |= (x & 0x40000000) << 1;
            y |= (x & 0x80000000) << 0;
            gpr[rd] = y;
        }
    }

    public void doSEH(int rd, int rt) {
        gpr[rd] = (gpr[rt] << 16) >> 16;
    }

}