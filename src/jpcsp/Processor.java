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

    public int pc;
    public int hi,  lo;
    public int gpr[] = new int[32];    //  32 x 32-bit general purpose registers

    public float fpr[] = new float[32];  //  32 x 32-bit float point registers

    public float vpr[] = new float[128]; // 128 x 32-bit float point registers

    private byte[] cyclesPer;

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

        for (int i = 0; i < 32; i++) {
            gpr[i] = 0;
        }

        for (int i = 0; i < 32; i++) {
            fpr[i] = 0;
        }

        for (int i = 0; i < 128; i++) {
            vpr[i] = 0;
        }

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
/*
    private static boolean addOverflow(int i, int j) {
        long tmp = ((long)i + (long)j) << 62;
        return ((tmp >>> 1) == (tmp & 1));
    }
    
    private static boolean subOverflow(int i, int j) {
        long tmp = ((long)i - (long)j) << 62;
        return ((tmp >>> 1) == (tmp & 1));
    }
 */   
    private static boolean addSubOverflow(long value) {
        long tmp = value << 62;
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
                    case SLL: //last update 31/07/2008 - should be okay (shadow)
                        gpr[rd] = gpr[rt] << sa;
                        break;

                    case SRLROR:
                        //last update 5/08/2008 - added ROTR (hlide)
                        //last update 31/07/2008 - should be okay (shadow)
                        //last update 31/07/2008 - >>> does not sign extend (fiveofhearts)
                        if (rs == ROTR) {
                            gpr[rd] = (gpr[rt] >>> sa) | (gpr[rt] << (32 - sa));
                        } else {
                            gpr[rd] = gpr[rt] >>> sa;
                        }
                        break;

                    case SRA:
                        //last update 31/07/2008 - >> sign extension is automatic (fiveofhearts)
                        gpr[rd] = gpr[rt] >> sa;
                        break;

                    case SLLV:
                        gpr[rd] = gpr[rt] << (gpr[rs] & 0x3F);
                        break;

                    case SRLRORV:
                        //last update 5/08/2008 - added ROTR (hlide)
                        //last update 31/07/2008 - should be okay (shadow)
                        //last update 31/07/2008 - >>> does not sign extend (fiveofhearts)
                        sa = (gpr[rs] & 0x3F);
                        if (rs == ROTRV) {
                            gpr[rd] = (gpr[rt] >>> sa) | (gpr[rt] << (32 - sa));
                        } else {
                            gpr[rd] = gpr[rt] >>> sa;
                        }
                        break;

                    case SRAV:
                        //last update 31/07/2008 - >> sign extension is automatic (fiveofhearts)
                        gpr[rd] = gpr[rt] >> (gpr[rs] & 0x3F);
                        break;

                    case JR:
                        pc = gpr[rs] - 4;
                        /* TODO: delay one cycle */
                        break;

                    case JALR:
                        gpr[rd] = pc + 8; // second instruction after

                        pc = gpr[rs] - 4;
                        /* TODO: delay one cycle */
                        break;

                    case MOVZ:
                        if (gpr[rs] == 0) {
                            gpr[rd] = gpr[rt];
                        }
                        break;

                    case MOVN:
                        if (gpr[rs] != 0) {
                            gpr[rd] = gpr[rt];
                        }
                        break;

                    case SYSCALL:
                        // not implemented
                        break;

                    case BREAK:
                        // not implemented
                        break;

                    case SYNC:
                        // not implemented
                        break;

                    case MFHI:
                        gpr[rd] = hi;
                        break;
                    case MTHI:
                        hi = gpr[rs];
                        break;

                    case MFLO:
                        gpr[rd] = lo;
                        break;

                    case MTLO:
                        lo = gpr[rs];
                        break;

                    case CLZ:
                        gpr[rd] = countLeadingZero(gpr[rs]);
                        break;

                    case CLO:
                        gpr[rd] = countLeadingOne(gpr[rs]);
                        break;

                    case MULT:
                        temp = (long) gpr[rs] * (long) gpr[rt];
                        hi = (int) ((temp >> 32) & 0xFFFFFFFF);
                        lo = (int) (temp & 0xFFFFFFFF);
                        break;

                    case MULTU:
                        /* We OR the bit so we are sure the sign bit isnt set */
                        longA |= gpr[rs];
                        longB |= gpr[rt];
                        temp = longA * longB;
                        hi = (int) ((temp >> 32) & 0xFFFFFFFF);
                        lo = (int) (temp & 0xFFFFFFFF);
                        break;

                    case DIV:
                        lo = gpr[rs] / gpr[rt];
                        hi = gpr[rs] % gpr[rt];
                        break;

                    case DIVU:
                        longA |= gpr[rs];
                        longB |= gpr[rt];
                        lo = (int) (longA / longB);
                        hi = (int) (longA % longB);
                        break;

                    case MADD:
                        temp = ((long) hi) << 32 + (long) lo;
                        temp += (long) gpr[rs] * (long) gpr[rt];
                        hi = (int) ((temp >> 32) & 0xFFFFFFFF);
                        lo = (int) (temp & 0xFFFFFFFF);
                        break;

                    case MADDU:
                        /* We OR the bit so we are sure the sign bit isnt set */
                        longA |= gpr[rs];
                        longB |= gpr[rt];
                        temp = ((long) hi) << 32 + (long) lo;
                        temp += longA * longB;
                        hi = (int) ((temp >> 32) & 0xFFFFFFFF);
                        lo = (int) (temp & 0xFFFFFFFF);
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
                        temp = ((long) hi) << 32 + (long) lo;
                        temp -= (long) gpr[rs] * (long) gpr[rt];
                        hi = (int) ((temp >> 32) & 0xFFFFFFFF);
                        lo = (int) (temp & 0xFFFFFFFF);
                        break;

                    case MSUBU:
                        /* We OR the bit so we are sure the sign bit isnt set */
                        longA |= gpr[rs];
                        longB |= gpr[rt];
                        temp = ((long) hi) << 32 + (long) lo;
                        temp -= longA * longB;
                        hi = (int) ((temp >> 32) & 0xFFFFFFFF);
                        lo = (int) (temp & 0xFFFFFFFF);
                        break;
                       
                    default:
                        System.out.println("Unsupported special instruction " + Integer.toHexString(special));
                        break;
                }
                break;

            case J:
                pc = ((pc & 0xF0000000) | ((value & 0x3FFFFFF) << 2)) - 4;
                /*TODO: delay one cycle */
                break;

            case JAL:
                gpr[31] = pc + 8; // second instruction after

                pc = ((pc & 0xF0000000) | ((value & 0x3FFFFFF) << 2)) - 4;
                break;

            case BEQ:
                if (gpr[rs] == gpr[rt]) {
                    pc += (signExtend(imm) << 2) + 4 - 4; // relative to address of first instruction after

                }
                break;

            case BNE:
                if (gpr[rs] != gpr[rt]) {
                    pc += (signExtend(imm) << 2) + 4 - 4;
                }
                break;

            case BLEZ:
                if (gpr[rs] <= 0) {
                    pc += (signExtend(imm) << 2) + 4 - 4;
                }
                break;

            case BGTZ:
                if (gpr[rs] > 0) {
                    pc += (signExtend(imm) << 2) + 4 - 4;
                }
                break;

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
                                gpr[rd] = (gpr[rt] << 24) >> 24;
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
                                gpr[rd] = (gpr[rt] << 16) >> 16;
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
        pc += 4;
    }
}
