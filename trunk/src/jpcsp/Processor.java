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

import static jpcsp.AllegrexOpcodes.*;

public class Processor {

    public int[]     gpr;
    public float[]   fpr;
    public float[]   vpr; 

    private long   hilo;
    public int     hi,  lo;
    public int     pc;
    
    private byte[] cyclesPer;

    public int cycles;
    
    Processor() {
        Memory.get_instance(); //intialize memory
        setupCycleCost();
        reset();
    }

    private void setupCycleCost() {
        cyclesPer = new byte[0xff];
        cyclesPer[MULT] = 5;
        cyclesPer[MULTU] = 5;
        cyclesPer[MADD] = 5;
        cyclesPer[MADDU] = 5;
        cyclesPer[MSUB] = 5;
        cyclesPer[MSUBU] = 5;
        cyclesPer[DIV] = 36;
        cyclesPer[DIVU] = 36;
    }

    public void reset() {
        // intialize psp register
        pc = 0x00000000;
        hi = lo = 0;
        gpr = new int[32]; 
        fpr = new float[32];
        vpr = new float[128];
    }

    public long numberCyclesDelay() {
        return 0l;
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

    private static int countLeadingZero(int value) {
        return 0; // TO DO
    }

    private static int countLeadingOne(int value) {
        return 0; // TO DO
    }

    private static boolean addSubOverflow(long value) {
        long tmp = value << (62-31);
        return ((tmp >>> 1) == (tmp & 1));
    }

    private boolean couldRaiseOverflowOnAdd(int value0, int value1) {
        boolean v1 = (((long) value0 + (long) value1) > Integer.MAX_VALUE);
        boolean v2 = (((long) value0 + (long) value1) < Integer.MIN_VALUE); //or underflow????

        return v1 | v2;
    }

    private boolean couldRaiseOverflowOnSub(int value0, int value1) {
        boolean v1 = (((long) value0 - (long) value1) > Integer.MAX_VALUE);
        boolean v2 = (((long) value0 - (long) value1) < Integer.MIN_VALUE); //or underflow????

        return v1 | v2;
    }

    public void stepCpu() {
        long temp;
        long longA, longB;
        int value = Memory.get_instance().read32(pc);
        pc += 4;
        longA = longB = 0;
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int sa = (value >> 6) & 0x1f;
        int imm = value & 0xffff;
        byte opcode = (byte) ((value >> 26) & 0x3f);
        switch (opcode) {
            case SPECIAL:
                byte special = (byte) (value & 0x3f);
                switch (special) {
                    case SLL:
                        if (value == 0)
                            doNOP();
                        else
                            doSLL(rd, rt, sa);
                        break;

                    case SRLROR:
                        if (rs == ROTR) {
                            doSLL(rd, rt, sa);
                        } else {
                            doROTR(rd, rt, sa);
                        }
                        break;

                    case SRA:
                        doSRA(rd, rt, sa);
                        break;

                    case SLLV:
                        doSLLV(rd, rt, sa);
                        break;

                    case SRLRORV: {
                        if (sa == ROTRV) {
                            doSLLV(rd, rt, rs);
                        } else {
                            doROTRV(rd, rt, rs);
                        }
                        break;
                    }

                    case SRAV:
                        doSRAV(rd, rt, sa);
                        break;

                    case JR:
                        doJR(rs);
                        break;                        

                    case JALR:
                        doJALR(rd, rs);
                        break;                        

                    case MOVZ:
                        doMOVZ(rd, rs, rt);
                        break;

                    case MOVN:
                        doMOVN(rd, rs, rt);
                        break;

                    case SYSCALL: {
                        int code = (value >> 6) & 0x000fffff;
                        doSYSCALL(code);
                        break;
                    }

                    case BREAK: {
                        int code = (value >> 6) & 0x000fffff;
                        doBREAK(code);
                        break;
                    }
                    case SYNC:
                        doSYNC();
                        break;

                    case MFHI:
                        doMFHI(rd);
                        break;
                        
                    case MTHI:
                        doMTHI(rs);
                        break;

                    case MFLO:
                        doMFLO(rd);
                        break;

                    case MTLO:
                        doMTLO(rs);
                        break;

                    case CLZ:
                        doCLZ(rd, rs);
                        break;

                    case CLO:
                        doCLO(rd, rs);
                        break;

                    case MULT:
                        hilo = (long) gpr[rs] * (long) gpr[rt];
                        hi = (int) (hilo >>> 32);
                        lo = (int) (hilo & 0xFFFFFFFF);
                        break;

                    case MULTU:
                        /* We OR the bit so we are sure the sign bit isnt set */
                        longA |= gpr[rs];
                        longB |= gpr[rt];
                        hilo = longA * longB;
                        hi = (int) (hilo >>> 32);
                        lo = (int) (hilo & 0xFFFFFFFF);
                        break;

                    case DIV:
                        lo = gpr[rs] / gpr[rt];
                        hi = gpr[rs] % gpr[rt];
                        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
                        break;

                    case DIVU: {
                        longA |= gpr[rs];
                        longB |= gpr[rt];
                        lo = (int) (longA / longB);
                        hi = (int) (longA % longB);
                        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
                        break;
                    }

                    case MADD:
                        doMADD(rs, rt);
                        break;

                    case MADDU:
                        doMADDU(rs, rt);
                        break;

                    case ADD: {
                        long result = (long) gpr[rs] + (long) gpr[rt];

                        if (addSubOverflow(result)) {
                            // TODO set exception overflow and break !!! (rd cannot be modify)
                            break;
                        }
                        gpr[rd] = (int) result;
                        break;
                    }

                    case ADDU:
                        longA |= gpr[rs];
                        longB |= gpr[rt];
                        gpr[rd] = (int) (longA + longB);
                        break;

                    case SUB: {
                        long result = (long) gpr[rs] - (long) gpr[rt];

                        if (addSubOverflow(result)) {
                            // TODO set exception overflow and break !!! (rd cannot be modify)
                            break;
                        }
                        gpr[rd] = (int) result;
                        break;
                    }

                    case SUBU:
                        gpr[rd] = gpr[rs] - gpr[rt];
                        break;

                    case AND:
                        gpr[rd] = gpr[rs] & gpr[rt];
                        break;

                    case OR:
                        gpr[rd] = gpr[rs] | gpr[rt];
                        break;

                    case XOR:
                        gpr[rd] = gpr[rs] ^ gpr[rt];
                        break;

                    case NOR:
                        gpr[rd] = ~(gpr[rs] | gpr[rt]);
                        break;

                    case SLT:
                        gpr[rd] = signedCompare(gpr[rs], gpr[rt]);
                        break;

                    case SLTU:
                        gpr[rd] = unsignedCompare(gpr[rs], gpr[rt]);
                        break;

                    case MAX:
                        gpr[rd] = (gpr[rs] > gpr[rt]) ? gpr[rs] : gpr[rt];
                        break;

                    case MIN:
                        gpr[rd] = (gpr[rs] < gpr[rt]) ? gpr[rs] : gpr[rt];
                        break;

                    case MSUB:
                        doMSUB(rs, rt);
                        break;

                    case MSUBU:
                        doMSUBU(rs, rt);
                        break;

                    default:
                        System.out.println("Unsupported special instruction " + Integer.toHexString(special));
                        break;
                }
                break;

            case J: {
                stepCpu();
                pc = ((pc & 0xF0000000) | ((value & 0x3FFFFFF) << 2));
                break;
            }

            case JAL: {
                int target = pc + 4;
                stepCpu();
                gpr[31] = target;
                pc = ((pc & 0xF0000000) | ((value & 0x3FFFFFF) << 2));
                break;
            }                

            case BEQ: {
                boolean t = (gpr[rs] == gpr[rt]);
                stepCpu();
                if (t) {
                    pc += (signExtend(imm) << 2);
                }
                break;
            }

            case BNE: {
                boolean t = (gpr[rs] != gpr[rt]);
                stepCpu();
                if (t) {
                    pc += (signExtend(imm) << 2);
                }
                break;
            }

            case BLEZ: {
                boolean t = (gpr[rs]  <= 0);
                stepCpu();
                if (t) {
                    pc += (signExtend(imm) << 2);
                }
                break;
            }

            case BGTZ: {
                boolean t = (gpr[rs] > 0);
                stepCpu();
                if (t) {
                    pc += (signExtend(imm) << 2);
                }
                break;
            }

            case ADDI: {
                long result = (long) gpr[rs] + (long) signExtend(imm);

                if (addSubOverflow(result)) {
                    // TODO set exception overflow and break !!! (rd cannot be modify)
                    break;
                }
                gpr[rd] = (int) result;
                break;
            }

            case ADDIU:
                gpr[rt] = gpr[rs] + signExtend(imm);
                break;

            case SLTI:
                gpr[rt] = signedCompare(gpr[rs], signExtend(imm));
                break;

            case SLTIU:
                gpr[rt] = unsignedCompare(gpr[rs], signExtend(imm));
                break;

            case ANDI:
                gpr[rt] = gpr[rs] & imm;
                break;

            case ORI:
                gpr[rt] = gpr[rs] | imm;
                break;

            case XORI:
                gpr[rt] = gpr[rs] ^ imm;
                break;

            case LUI:
                gpr[rt] = imm << 16;
                break;

            case SPECIAL3:
                byte special3 = (byte) (value & 0x3f);
                switch (special3) {

                    case EXT: {
                        int mask = ~(~1 << rd);
                        gpr[rd] = (gpr[rt] >> sa) & mask;
                    }
                    break;

                    case INS: {
                        int mask1 = ~(~0 << sa);
                        int mask2 = (~0 << rd);
                        int mask3 = mask1 | mask2;
                        gpr[rd] = (gpr[rt] & mask3) | ((gpr[rs] >> sa) & mask2);
                    }
                    break;

                    case BSHFL:
                        switch (sa) {
                            case WSBH: {
                                int tmp  = gpr[rt];
                                gpr[rd]  = (tmp & 256) <<  8; tmp >>= 8;
                                gpr[rd] |= (tmp & 256) <<  0; tmp >>= 8;
                                gpr[rd] |= (tmp & 256) << 24; tmp >>= 8;
                                gpr[rd] |= (tmp & 256) << 16;
                                break;
                            }

                            case WSBW: {
                                int tmp  = gpr[rt];
                                gpr[rd]  = (tmp & 256) << 24; tmp >>= 8;
                                gpr[rd] |= (tmp & 256) << 16; tmp >>= 8;
                                gpr[rd] |= (tmp & 256) <<  8; tmp >>= 8;
                                gpr[rd] |= (tmp & 256) <<  0;
                                break;
                            }

                            case SEB:
                                gpr[rd] = signExtend8(gpr[rt]);
                                break;

                            case BITREV: {
                                int tmp  = gpr[rt];
                                gpr[rd]  = (tmp & 1) << 31; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 30; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 29; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 28; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 27; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 26; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 25; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 24; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 23; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 22; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 21; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 20; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 19; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 18; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 17; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 16; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 15; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 14; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 13; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 12; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 11; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) << 10; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  9; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  8; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  7; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  6; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  5; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  3; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  2; tmp >>= 1;
                                gpr[rd] |= (tmp & 1) <<  1; tmp >>= 1;
                                gpr[rd] |= (tmp & 1);
                                break;
                            }

                            case SEH:
                                gpr[rd] = signExtend(gpr[rt]);
                                break;

                            default:
                                System.out.println("Unsupported BSHFL instruction " + Integer.toHexString(opcode));
                                break;
                        }

                    default:
                        System.out.println("Unsupported SPECIAL3 instruction " + Integer.toHexString(opcode));
                        break;
                }

            case LH:
                int virtAddr;
                virtAddr = gpr[rs] + signExtend(imm);
                gpr[rt] = signExtend(Memory.get_instance().read16(virtAddr));
                break;

            case LHU:
                virtAddr = gpr[rs] + signExtend(imm);
                gpr[rt] = Memory.get_instance().read16(virtAddr);
                break;

            case LW:
                virtAddr = gpr[rs] + signExtend(imm);
                gpr[rt] = Memory.get_instance().read32(virtAddr);
                break;

            case SB:
                virtAddr = gpr[rs] + signExtend(imm);
                Memory.get_instance().write8(virtAddr, (byte) (gpr[rt] & 0xFF));
                break;

            case SH:
                virtAddr = gpr[rs] + signExtend(imm);
                Memory.get_instance().write16(virtAddr, (short) (gpr[rt] & 0xFFFF));
                break;

            case SW:
                virtAddr = gpr[rs] + signExtend(imm);
                Memory.get_instance().write32(virtAddr, gpr[rt]);
                break;

            default:
                System.out.println("Unsupported instruction " + Integer.toHexString(opcode));
                break;
        }
    }
    
    private void doNOP()
    {
        cycles += 1;
    }

    private void doSLL(int rd, int rt, int sa)
    {
        if (rd != 0) gpr[rd] = (gpr[rt] << sa);
        
        cycles += 1;
    }

    private void doSRL(int rd, int rt, int sa)
    {
        if (rd != 0) gpr[rd] = (gpr[rt] >>> sa);
        
        cycles += 1;
    }

    private void doSRA(int rd, int rt, int sa)
    {
        if (rd != 0) gpr[rd] = (gpr[rt] >> sa);
        
        cycles += 1;
    }

    private void doSLLV(int rd, int rt, int rs)
    {
        if (rd != 0) gpr[rd] = (gpr[rt] << (gpr[rs]&31));
        
        cycles += 1;
    }

    private void doSRLV(int rd, int rt, int rs)
    {
        if (rd != 0) gpr[rd] = (gpr[rt] >>> (gpr[rs]&31));
        
        cycles += 1;
    }

    private void doSRAV(int rd, int rt, int rs)
    {
        if (rd != 0) gpr[rd] = (gpr[rt] >>> (gpr[rs]&31));
        
        cycles += 1;
    }

    private void doJR(int rs)
    {
        int target = gpr[rs];
        cycles += 2;
        stepCpu();
        pc = target;
    }

    private void doJALR(int rd, int rs)
    {
        if (rd != 0) gpr[rd] = pc + 4;
        int target = gpr[rs];
        cycles += 2;
        stepCpu();
        pc = target;
    }
    
    private void doMFHI(int rd)
    {
        if (rd != 0) gpr[rd] = hi;

        cycles += 1; // ?
    }

    private void doMTHI(int rs)
    {
        hi = gpr[rs];
        hilo = (((long)lo) << 32) | (hilo & 0xffffffff);

        cycles += 1; // ?
    }

    private void doMFLO(int rd)
    {
        if (rd != 0) gpr[rd] = lo;

        cycles += 1; // ?
    }

    private void doMTLO(int rs)
    {
        lo = gpr[rs];
        hilo = ((hilo >>> 32) << 32) | (((long)lo) & 0xffffffff);

        cycles += 1; // ?
    }

/*
    mult(000000:rs:rt:0000000000011000)
    {
        cycles="5"
        operation=
        "
            1: result:64 = s64(GPR[rs]) * s64(GPR[rs])
               LO = result[31..0]
               HI = result[63..32]
        "
    }

    multu(000000:rs:rt:0000000000011001)
    {
        cycles="5"
        operation=
        "
            1: result:64 = u64(GPR[rs]) * u64(GPR[rs])
               LO = result[31..0]
               HI = result[63..32]
        "
    }

    div(000000:rs:rt:0000000000011010)
    {
        cycles="36"
        operation=
        "
            1: LO = s32(GPR[rs]) / s32(GPR[rs])
               HI = s32(GPR[rs]) % s32(GPR[rs])
        "
    }

    divu(000000:rs:rt:0000000000011011)
    {
        cycles="36"
        operation=
        "
            1: LO = u32(GPR[rs]) / u32(GPR[rs])
               HI = u32(GPR[rs]) % u32(GPR[rs])
        "
    }

    add(000000:rs:rt:rd:00000100000)
    {
        cycles="1"
        operation=
        "
            1: result:33 = ((GPR[rs][31]) << 32) | GPR[rs]) + ((GPR[rt][31]) << 32) | GPR[rt])
               if (result[32] == result[31])
                 GPR[rd] = result[31..0]
               else
                 raise integer overflow exception
        "
    }

    addu(000000:rs:rt:rd:00000100001)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = GPR[rs] + GPR[rt]
        "
    }

    sub(000000:rs:rt:rd:00000100010)
    {
        cycles="1"
        operation=
        "
            1: result:33 = ((GPR[rs][31]) << 32) | GPR[rs]) - ((GPR[rt][31]) << 32) | GPR[rt])
               if (result[32] == result[31])
                 GPR[rd] = result[31..0]
               else
                 raise integer overflow exception
        "
    }

    subu(000000:rs:rt:rd:00000100011)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = GPR[rs] - GPR[rt]
        "
    }

    and(000000:rs:rt:rd:00000100100)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = GPR[rs] & GPR[rt]
        "
    }

    or(000000:rs:rt:rd:00000100101)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = GPR[rs] | GPR[rt]
        "
    }

    xor(000000:rs:rt:rd:00000100110)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = GPR[rs] ^ GPR[rt]
        "
    }

    nor(000000:rs:rt:rd:00000100111)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = ~(GPR[rs] | GPR[rt])
        "
    }

    slt(000000:rs:rt:rd:00000101010)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = s32(GPR[rs]) < s32(GPR[rt])
        "
    }

    sltu(000000:rs:rt:rd:00000101011)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = u32(GPR[rs]) + u32(GPR[rt])
        "
    }

    // REGIMM
   
    bltz(000001:rs:00000:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) < 0)
               execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bgez(000001:rs:00001:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) >= 0)
               execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bltzl(000001:rs:00010:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) < 0)
               if (ct)
                 execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bgezl(000001:rs:00011:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) >= 0)
               if (ct)
                 execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bltzal(000001:rs:10000:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) < 0)
               execute instruction at PC+4
               if (ct)
                 GPR(31) = PC+8
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bgezal(000001:rs:10001:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) >= 0)
               execute instruction at PC+4
               if (ct)
                 GPR(31) = PC+8
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bltzall(000001:rs:10010:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) < 0)
               if (ct)
                 execute instruction at PC+4
               if (ct)
                 GPR(31) = PC+8
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bgezall(000001:rs:10011:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) >= 0)
               if (ct)
                 execute instruction at PC+4
               if (ct)
                 GPR(31) = PC+8
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    // OPCODE #1
   
    j(000010:imm26)
    {
        cycles="2"
        operation=
        "
            1: execute instruction at PC+4
            2: PC = PC[31..28] | (u32(imm26) << 2)
        "
        delayslot="1"
    }

    jal(000011:imm26)
    {
        cycles="2"
        operation=
        "
            1: GPR(31) = PC+8
               execute instruction at PC+4
            2: PC = PC[31..28] | (u32(imm26) << 2)
        "
        delayslot="1"
    }

    beq(000100:rs:rt:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (GPR[rs] == GPR[rt])
               execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bne(000101:rs:rt:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (GPR[rs] <> GPR[rt])
               execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    blez(000110:rs:00000:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) <= 0)
               execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bgtz(000111:rs:00000:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) > 0)
               execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    addi(001000:rs:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: result:33 = ((GPR[rs][31]) << 32) | GPR[rs]) + s32(imm16)
               if (result[32] == result[31])
                 GPR[rt] = result[31..0]
               else
                 raise integer overflow exception
        "
    }

    addiu(001001:rs:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = GPR[rs] + s32(imm16)
        "
    }

    slti(001010:rs:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = s32(GPR[rs]) < s32(imm16)
        "
    }

    sltiu(001011:rs:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = u32(GPR[rs]) < u32(s32(imm16))
        "
    }

    andi(001100:rs:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = s32(GPR[rs]) & u32(imm16)
        "
    }

    ori(001101:rs:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = s32(GPR[rs]) | u32(imm16)
        "
    }

    xori(001110:rs:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = s32(GPR[rs]) ^ u32(imm16)
        "
    }

    lui(00111100000:rt:imm16)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = s32(GPR[rs]) | (u32(imm16) << 16)
        "
    }
   
    // COP0

    mfc0(01000000000:rt:c0dr:00000000000)
    {
        cycles="?"
        operation=
        "
            1: GPR[rt] = C0DR(c0dr)
        "
    }

    cfc0(01000000010:rt:c0cr:00000000000)
    {
        cycles="?"
        operation=
        "
            1: GPR[rt] = C0CR(c0cr)
        "
    }

    mtc0(01000000100:rt:c0dr:00000000000)
    {
        cycles="?"
        operation=
        "
            1: C0DR(c0dr) = GPR[rt]
        "
    }

    ctc0(01000100110:rt:c0cr:00000000000)
    {
        cycles="?"
        operation=
        "
            1: C0CR(c0dr) = GPR[rt]
        "
    }

    eret(01000000000000000000000000011000)
    {
        cycles="?"
        operation=
        "
            1: if (ERL == 1)
                 PC = ErrorEPC
               else
                 PC = RPC
               if (ERL == 0)
                 EXL = 0
               LLBit = 0
        "      
    }
   
    // OPCODE #2

    beql(010100:rs:rt:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (GPR[rs] == GPR[rt])
               if (ct)
                 execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bnel(010101:rs:rt:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (GPR[rs] <> GPR[rt])
               if (ct)
                 execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    blezl(010110:rs:00000:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) <= 0)
               if (ct)
                 execute instruction at PC+4
               if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    bgtzl(010111:rs:00000:imm16)
    {
        cycles="3"
        operation=
        "
            1: ct = (s32(GPR[rs]) > 0)
               if (ct)
                 execute instruction at PC+4
            2: if (ct)
                 PC = PC + (s16(imm16) << 2)
        "
        delayslot="1"
    }

    lb(100000:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               GPR[rt] = s32(MemoryRead8(address))
        "
    }

    lh(100001:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               if (address & 1)
                 raise address error exception
               else
                 GPR[rt] = s32(MemoryRead16(address))
        "
    }

    lwl(100010:rs:rt:imm16)
    {
        cycles="?"
    }

    lw(100011:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               if (address & 3)
                 raise address error exception
               else
                 GPR[rt] = MemoryRead32(address)
        "
    }

    lbu(100100:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               GPR[rt] = u32(MemoryRead8(address))
        "
    }

    lhu(100101:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               if (address & 1)
                 raise address error exception
               else
                 GPR[rt] = u32(MemoryRead16(address))
        "
    }

    lwr(100110:rs:rt:imm16)
    {
        cycles="?"
    }

    sb(101000:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               MemoryWrite8(address, GPR[rt][7..0])
        "
    }

    sh(101001:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               if (address & 1)
                 raise address error exception
               else
                 MemoryWrite16(address, GPR[rt][15..0])
        "
    }

    swl(101010:rs:rt:imm16)
    {
        cycles="?"
    }

    sw(101011:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               if (address & 3)
                 raise address error exception
               else
                 MemoryWrite32(address, GPR[rt])
        "
    }

    swr(101110:rs:rt:imm16)
    {
        cycles="?"
    }

    ll(110000:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               if (address & 3)
                 raise address error exception
               else
                 GPR[rt] = MemoryRead32(address)
               LLBit = 1
        "
    }

    sc(111000:rs:rt:imm16)
    {
        cycles="?"
        operation=
        "
            1: address = GPR[rs] + s32(imm16)
               if (address & 3)
                 raise address error exception
               else if (LLBit == 1)
                 MemoryWrite32(address, GPR[rt])
               GPR[rt] = u32(LLBit)
        "
    }
*/
    private void doROTR(int rd, int rt, int sa)
    {
        cycles += 1;
        
        if (rd != 0) {
            int at = gpr[rt];
    
            gpr[rd] = (at >>> sa) | (at << (32 - sa));
        }
    }

    private void doROTRV(int rd, int rt, int rs)
    {
        doROTR(rd, rt, (gpr[rs]&31));
    }

    private void doMOVZ(int rd, int rs, int rt)
    {
        cycles += 1;
        
        if ((rd != 0) && (gpr[rt] == 0))
            gpr[rd] = gpr[rs];
    }
   
    private void doMOVN(int rd, int rs, int rt)
    {       
        cycles += 1;

        if ((rd != 0) && (gpr[rt] != 0))
            gpr[rd] = gpr[rs];
    }
   
    private void doSYSCALL(int code)
    {
        // TODO
        cycles += 1;
    }

    private void doBREAK(int code)
    {
        // TODO
        cycles += 1;
    }
   
    private void doSYNC()
    {
        cycles += 7;
    }

    private void doCLZ(int rd, int rs)
    {
        cycles += 1;
        
        if (rd != 0) {
            int count = 32;
            int value = gpr[rs];
            int i = 31;
        
            do {
                if (((value >>> i) & 1) == 1)
                    count = 31 - i;
            } while (count == 32 && i-- != 0);
        
            gpr[rd] = count;
        }
    }
   
    private void doCLO(int rd, int rs)
    {
        cycles += 1;
        
        if (rd != 0) {
            int count = 32;
            int value = gpr[rs];
            int i = 31;
        
            do {
                if (((value >>> i) & 1) == 0)
                    count = 31 - i;
            } while (count == 32 && i-- != 0);
        
            gpr[rd] = count;
        }
    }
   
    private void doMADD(int rs, int rt)
    {
        cycles += 5;
               
        hilo += ((long)gpr[rs]) * ((long)gpr[rs]);
        hi = (int)(hilo >>> 32);
        lo = (int)(hilo & 0xffffffff);
    }
   
    private void doMADDU(int rs, int rt)
    {
        cycles += 5;
               
        hilo += (((long)gpr[rs]) & 0xffffffff) * (((long)gpr[rs]) & 0xffffffff);
        hi = (int)(hilo >>> 32);
        lo = (int)(hilo & 0xffffffff);
    }
   
    private void doMAX(int rd, int rs, int rt)
    {
        cycles += 1;
        
        if (rd != 0) {
            int x = gpr[rs];
            int y = gpr[rt];
            gpr[rd] = (x > y) ? x : y;
        }
    }
   
    private void doMIN(int rd, int rs, int rt)
    {
        cycles += 1;

        if (rd != 0) {
            int x = gpr[rs];
            int y = gpr[rt];
            gpr[rd] = (x < y) ? x : y;
        }
    }
   
    private void doMSUB(int rs, int rt)
    {
        cycles += 5;
               
        hilo -= ((long)gpr[rs]) * ((long)gpr[rs]);
        hi = (int)(hilo >>> 32);
        lo = (int)(hilo & 0xffffffff);
    }
   
    private void doMSUBU(int rs, int rt)
    {
        cycles += 5;
               
        hilo -= (((long)gpr[rs]) & 0xffffffff) * (((long)gpr[rs]) & 0xffffffff);
        hi = (int)(hilo >>> 32);
        lo = (int)(hilo & 0xffffffff);
    }
   
    private void doHALT()
    {
        // TODO
        cycles += 1;
    }

 /*
    ext(011111:rs:rt:(msb-lsb):lsb:000000)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt] = GPR[rs][msb..lsb];
        "      
    }

    ins(011111:rs:rt:msb:lsb:000100)
    {
        cycles="1"
        operation=
        "
            1: GPR[rt][msb..lsb] = GPR[rs][msb-lsb..0];
        "      
    }

    wsbh(01111100000:rt:rd:00010100000)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd][ 7.. 0] = GPR[rt][15.. 8];
               GPR[rd][15.. 8] = GPR[rt][ 7.. 0];
               GPR[rd][23..16] = GPR[rt][31..24];
               GPR[rd][31..24] = GPR[rt][23..16];
        "      
    }

    wsbw(01111100000:rt:rd:00011100000)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd][ 7.. 0] = GPR[rt][15.. 8];
               GPR[rd][15.. 8] = GPR[rt][23..16];
               GPR[rd][23..16] = GPR[rt][15.. 8];
               GPR[rd][31..24] = GPR[rt][ 7.. 0];
        "      
    }

    seb(01111100000:rt:rd:10000100001)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = s32(GPR[rt][7..0]);
        "      
    }

    bitrev(01111100000:rt:rd:10100100000)
    {
        cycles="1"
        operation=
        "
            1: for each i in [31..0]
                 GPR[rd][i] = GPR[rt][31-i];
        "      
    }

    seh(01111100000:rt:rd:11000100000)
    {
        cycles="1"
        operation=
        "
            1: GPR[rd] = s32(GPR[rt][15..0]);
        "      
    }

    // OPCODE #2
   
    cache(101111:rs:func:imm16)
    {
        cycles="?"
    }
*/
}
