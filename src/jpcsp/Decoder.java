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

/**
 *
 * @author hli
 */

public class Decoder< P extends AllegrexInstructions > {

    public int rs(int instruction) {
        return (instruction >> 21) & 31;
    }

    public int rt(int instruction) {
        return (instruction >> 16) & 31;
    }

    public int rd(int instruction) {
        return (instruction >> 11) & 31;
    }

    public int sa(int instruction) {
        return (instruction >> 6) & 31;
    }

    public int simm16(int instruction) {
        return (instruction << 16) >> 16;
    }

    public int uimm16(int instruction) {
        return (instruction) & 0xffff;
    }

    public int uimm26(int instruction) {
        return (instruction) & 0x03ffffff;
    }

    public int syscode(int instruction) {
        return (instruction >> 6) & 0x000fffff;
    }

    public void process(P that, int insn) {

        byte opcode = (byte) (insn >>> 26);

        switch (opcode) {
            case SPECIAL:
                byte special = (byte) (insn & 0x3f);
                switch (special) {
                    case SLL:
                        if (insn == 0) {
                            that.doNOP();
                        } else {
                            that.doSLL(rd(insn), rt(insn), sa(insn));
                        }
                        break;

                    case SRLROR:
                        if (rs(insn) == ROTR) {
                            that.doSLL(rd(insn), rt(insn), sa(insn));
                        } else {
                            that.doROTR(rd(insn), rt(insn), sa(insn));
                        }
                        break;

                    case SRA:
                        that.doSRA(rd(insn), rt(insn), sa(insn));
                        break;

                    case SLLV:
                        that.doSLLV(rd(insn), rt(insn), sa(insn));
                        break;

                    case SRLRORV: {
                        if (sa(insn) == ROTRV) {
                            that.doSRLV(rd(insn), rt(insn), rs(insn));
                        } else {
                            that.doROTRV(rd(insn), rt(insn), rs(insn));
                        }
                        break;
                    }

                    case SRAV:
                        that.doSRAV(rd(insn), rt(insn), sa(insn));
                        break;

                    case JR:
                        that.doJR(rs(insn));
                        break;

                    case JALR:
                        that.doJALR(rd(insn), rs(insn));
                        break;

                    case MOVZ:
                        that.doMOVZ(rd(insn), rs(insn), rt(insn));
                        break;

                    case MOVN:
                        that.doMOVN(rd(insn), rs(insn), rt(insn));
                        break;

                    case SYSCALL: {
                        that.doSYSCALL(syscode(insn));
                        break;
                    }

                    case BREAK: {
                        that.doBREAK(syscode(insn));
                        break;
                    }
                    case SYNC:
                        that.doSYNC();
                        break;

                    case MFHI:
                        that.doMFHI(rd(insn));
                        break;

                    case MTHI:
                        that.doMTHI(rs(insn));
                        break;

                    case MFLO:
                        that.doMFLO(rd(insn));
                        break;

                    case MTLO:
                        that.doMTLO(rs(insn));
                        break;

                    case CLZ:
                        that.doCLZ(rd(insn), rs(insn));
                        break;

                    case CLO:
                        that.doCLO(rd(insn), rs(insn));
                        break;

                    case MULT:
                        that.doMULT(rs(insn), rt(insn));
                        break;

                    case MULTU:
                        that.doMULTU(rs(insn), rt(insn));
                        break;

                    case DIV:
                        that.doDIV(rs(insn), rt(insn));
                        break;

                    case DIVU: {
                        that.doDIVU(rs(insn), rt(insn));
                        break;
                    }

                    case MADD:
                        that.doMADD(rs(insn), rt(insn));
                        break;

                    case MADDU:
                        that.doMADDU(rs(insn), rt(insn));
                        break;

                    case ADD:
                        that.doADD(rd(insn), rs(insn), rt(insn));
                        break;

                    case ADDU:
                        that.doADDU(rd(insn), rs(insn), rt(insn));
                        break;

                    case SUB:
                        that.doSUB(rd(insn), rs(insn), rt(insn));
                        break;

                    case SUBU:
                        that.doSUBU(rd(insn), rs(insn), rt(insn));
                        break;

                    case AND:
                        that.doAND(rd(insn), rs(insn), rt(insn));
                        break;

                    case OR:
                        that.doOR(rd(insn), rs(insn), rt(insn));
                        break;

                    case XOR:
                        that.doXOR(rd(insn), rs(insn), rt(insn));
                        break;

                    case NOR:
                        that.doNOR(rd(insn), rs(insn), rt(insn));
                        break;

                    case SLT:
                        that.doSLT(rd(insn), rs(insn), rt(insn));
                        break;

                    case SLTU:
                        that.doSLTU(rd(insn), rs(insn), rt(insn));
                        break;

                    case MAX:
                        that.doMAX(rd(insn), rs(insn), rt(insn));
                        break;

                    case MIN:
                        that.doMIN(rd(insn), rs(insn), rt(insn));
                        break;

                    case MSUB:
                        that.doMSUB(rs(insn), rt(insn));
                        break;

                    case MSUBU:
                        that.doMSUBU(rs(insn), rt(insn));
                        break;

                    default:
                        that.doUNK("Unsupported special instruction " + Integer.toHexString(special));
                        break;
                }
                break;

            case J:
                that.doJ(uimm26(insn));
                break;

            case JAL:
                that.doJAL(uimm26(insn));
                break;

            case BEQ:
                that.doBEQ(rs(insn), rt(insn), simm16(insn));
                break;

            case BNE:
                that.doBNE(rs(insn), rt(insn), simm16(insn));
                break;

            case BLEZ:
                that.doBLEZ(rs(insn), simm16(insn));
                break;

            case BGTZ:
                that.doBGTZ(rs(insn), simm16(insn));
                break;

            case ADDI:
                that.doADDI(rt(insn), rs(insn), simm16(insn));
                break;

            case ADDIU:
                that.doADDIU(rt(insn), rs(insn), simm16(insn));
                break;

            case SLTI:
                that.doSLT(rt(insn), rs(insn), simm16(insn));
                break;

            case SLTIU:
                that.doSLTIU(rt(insn), rs(insn), simm16(insn));
                break;

            case ANDI:
                that.doANDI(rt(insn), rs(insn), simm16(insn));
                break;

            case ORI:
                that.doORI(rt(insn), rs(insn), simm16(insn));
                break;

            case XORI:
                that.doXORI(rt(insn), rs(insn), simm16(insn));
                break;

            case LUI:
                that.doLUI(rt(insn), simm16(insn));
                break;

            case SPECIAL3:
                byte special3 = (byte) (insn & 0x3f);
                
                switch (special3) {

                    case EXT:
                        that.doEXT(rd(insn), rt(insn), rs(insn), sa(insn));
                        break;

                    case INS:
                        that.doINS(rd(insn), rt(insn), rs(insn), sa(insn));
                        break;

                    case BSHFL:
                        switch (sa(insn)) {
                            case WSBH:
                                that.doWSBH(rd(insn), rt(insn));
                                break;


                            case WSBW:
                                that.doWSBW(rd(insn), rt(insn));
                                break;

                            case SEB:
                                that.doSEB(rd(insn), rt(insn));
                                break;

                            case BITREV:
                                that.doBITREV(rd(insn), rt(insn));
                                break;

                            case SEH:
                                that.doSEH(rd(insn), rt(insn));
                                break;

                            default:
                                System.out.println("Unsupported BSHFL instruction " + Integer.toHexString(opcode));
                                break;
                        }

                    default:
                        that.doUNK("Unsupported SPECIAL3 instruction " + Integer.toHexString(opcode));
                        break;
                }

            case LB:
                that.doLB(rt(insn), rs(insn), simm16(insn));
                break;

            case LBU:
                that.doLBU(rt(insn), rs(insn), simm16(insn));
                break;

            case LH:
                that.doLH(rt(insn), rs(insn), simm16(insn));
                break;

            case LHU:
                that.doLHU(rt(insn), rs(insn), simm16(insn));
                break;

            case LWL:
                that.doLWL(rt(insn), rs(insn), simm16(insn));
                break;

            case LW:
                that.doLW(rt(insn), rs(insn), simm16(insn));
                break;

            case LWR:
                that.doLWR(rt(insn), rs(insn), simm16(insn));
                break;

            case SB:
                that.doSB(rt(insn), rs(insn), simm16(insn));
                break;

            case SH:
                that.doSH(rt(insn), rs(insn), simm16(insn));
                break;

            case SWL:
                that.doSWL(rt(insn), rs(insn), simm16(insn));
                break;

            case SW:
                that.doSW(rt(insn), rs(insn), simm16(insn));
                break;

            case SWR:
                that.doSWR(rt(insn), rs(insn), simm16(insn));
                break;

            default:
                that.doUNK("Unsupported instruction " + Integer.toHexString(opcode));
                break;
        }
    }
}
