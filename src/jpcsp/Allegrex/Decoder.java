package jpcsp.Allegrex;

import jpcsp.Allegrex.Common.*;
import jpcsp.Allegrex.Instructions.*;

public class Decoder {

    public static final Instruction table_0[] = {
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_1[(insn & 0x0000003f) >>> 0].instance(insn);
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_2[(insn & 0x00030000) >>> 16].instance(insn);
            }
        },
        jpcsp.Allegrex.Instructions.J,
        jpcsp.Allegrex.Instructions.JAL,
        jpcsp.Allegrex.Instructions.BEQ,
        jpcsp.Allegrex.Instructions.BNE,
        jpcsp.Allegrex.Instructions.BLEZ,
        jpcsp.Allegrex.Instructions.BGTZ,
        jpcsp.Allegrex.Instructions.ADDI,
        jpcsp.Allegrex.Instructions.ADDIU,
        jpcsp.Allegrex.Instructions.SLTI,
        jpcsp.Allegrex.Instructions.SLTIU,
        jpcsp.Allegrex.Instructions.ANDI,
        jpcsp.Allegrex.Instructions.ORI,
        jpcsp.Allegrex.Instructions.XORI,
        jpcsp.Allegrex.Instructions.LUI,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_3[(insn & 0x00c00000) >>> 22].instance(insn);
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_4[(insn & 0x03800000) >>> 23].instance(insn);
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00200000) == 0x00000000) {
                    return table_7[(insn & 0x00030000) >>> 16].instance(insn);
                } else {
                    if ((insn & 0x00000080) == 0x00000000) {
                        if ((insn & 0x00800000) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.MFV;
                        } else {
                            return jpcsp.Allegrex.Instructions.MTV;
                        }
                    } else {
                        if ((insn & 0x00800000) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.MFVC;
                        } else {
                            return jpcsp.Allegrex.Instructions.MTVC;
                        }
                    }
                }
            }
        },
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.BEQL,
        jpcsp.Allegrex.Instructions.BNEL,
        jpcsp.Allegrex.Instructions.BLEZL,
        jpcsp.Allegrex.Instructions.BGTZL,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_8[(insn & 0x01800000) >>> 23].instance(insn);
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_9[(insn & 0x03800000) >>> 23].instance(insn);
            }
        },
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_10[(insn & 0x03800000) >>> 23].instance(insn);
            }
        },
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000021) == 0x00000020) {
                    if ((insn & 0x00000080) == 0x00000000) {
                        if ((insn & 0x00000100) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.SEH;
                        } else {
                            return jpcsp.Allegrex.Instructions.BITREV;
                        }
                    } else {
                        if ((insn & 0x00000040) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.WSBH;
                        } else {
                            return jpcsp.Allegrex.Instructions.WSBW;
                        }
                    }
                } else {
                    if ((insn & 0x00000001) == 0x00000000) {
                        if ((insn & 0x00000004) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.EXT;
                        } else {
                            return jpcsp.Allegrex.Instructions.INS;
                        }
                    } else {
                        return jpcsp.Allegrex.Instructions.SEB;
                    }
                }
            }
        },
        jpcsp.Allegrex.Instructions.LB,
        jpcsp.Allegrex.Instructions.LH,
        jpcsp.Allegrex.Instructions.LWL,
        jpcsp.Allegrex.Instructions.LW,
        jpcsp.Allegrex.Instructions.LBU,
        jpcsp.Allegrex.Instructions.LHU,
        jpcsp.Allegrex.Instructions.LWR,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.SB,
        jpcsp.Allegrex.Instructions.SH,
        jpcsp.Allegrex.Instructions.SWL,
        jpcsp.Allegrex.Instructions.SW,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.SWR,
        jpcsp.Allegrex.Instructions.CACHE,
        jpcsp.Allegrex.Instructions.LL,
        jpcsp.Allegrex.Instructions.LWC1,
        jpcsp.Allegrex.Instructions.LVS,
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_11[(insn & 0x003f0000) >>> 16].instance(insn);
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000002) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.LVLQ;
                } else {
                    return jpcsp.Allegrex.Instructions.LVRQ;
                }
            }
        },
        jpcsp.Allegrex.Instructions.LVQ,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_12[(insn & 0x03000000) >>> 24].instance(insn);
            }
        },
        jpcsp.Allegrex.Instructions.SC,
        jpcsp.Allegrex.Instructions.SWC1,
        jpcsp.Allegrex.Instructions.SVS,
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_13[(insn & 0x03800000) >>> 23].instance(insn);
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000002) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.SVLQ;
                } else {
                    return jpcsp.Allegrex.Instructions.SVRQ;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000002) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.SVQ;
                } else {
                    return jpcsp.Allegrex.Instructions.SWB;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000001) == 0x00000000) {
                    if ((insn & 0x00000020) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VNOP;
                    } else {
                        return jpcsp.Allegrex.Instructions.VSYNC;
                    }
                } else {
                    return jpcsp.Allegrex.Instructions.VFLUSH;
                }
            }
        },
    };
    public static final Instruction table_1[] = {
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x001fffc0) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.NOP;
                } else {
                    return jpcsp.Allegrex.Instructions.SLL;
                }
            }
        },
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00200000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.SRL;
                } else {
                    return jpcsp.Allegrex.Instructions.ROTR;
                }
            }
        },
        jpcsp.Allegrex.Instructions.SRA,
        jpcsp.Allegrex.Instructions.SLLV,
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000040) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.SRLV;
                } else {
                    return jpcsp.Allegrex.Instructions.ROTRV;
                }
            }
        },
        jpcsp.Allegrex.Instructions.SRAV,
        jpcsp.Allegrex.Instructions.JR,
        jpcsp.Allegrex.Instructions.JALR,
        jpcsp.Allegrex.Instructions.MOVZ,
        jpcsp.Allegrex.Instructions.MOVN,
        jpcsp.Allegrex.Instructions.SYSCALL,
        jpcsp.Allegrex.Instructions.BREAK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.SYNC,
        jpcsp.Allegrex.Instructions.MFHI,
        jpcsp.Allegrex.Instructions.MTHI,
        jpcsp.Allegrex.Instructions.MFLO,
        jpcsp.Allegrex.Instructions.MTLO,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.CLZ,
        jpcsp.Allegrex.Instructions.CLO,
        jpcsp.Allegrex.Instructions.MULT,
        jpcsp.Allegrex.Instructions.MULTU,
        jpcsp.Allegrex.Instructions.DIV,
        jpcsp.Allegrex.Instructions.DIVU,
        jpcsp.Allegrex.Instructions.MADD,
        jpcsp.Allegrex.Instructions.MADDU,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.ADD,
        jpcsp.Allegrex.Instructions.ADDU,
        jpcsp.Allegrex.Instructions.SUB,
        jpcsp.Allegrex.Instructions.SUBU,
        jpcsp.Allegrex.Instructions.AND,
        jpcsp.Allegrex.Instructions.OR,
        jpcsp.Allegrex.Instructions.XOR,
        jpcsp.Allegrex.Instructions.NOR,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.SLT,
        jpcsp.Allegrex.Instructions.SLTU,
        jpcsp.Allegrex.Instructions.MAX,
        jpcsp.Allegrex.Instructions.MIN,
        jpcsp.Allegrex.Instructions.MSUB,
        jpcsp.Allegrex.Instructions.MSUBU,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
    };
    public static final Instruction table_2[] = {
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00100000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.BLTZ;
                } else {
                    return jpcsp.Allegrex.Instructions.BLTZAL;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00100000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.BGEZ;
                } else {
                    return jpcsp.Allegrex.Instructions.BGEZAL;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00100000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.BLTZL;
                } else {
                    return jpcsp.Allegrex.Instructions.BLTZALL;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00100000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.BGEZL;
                } else {
                    return jpcsp.Allegrex.Instructions.BGEZALL;
                }
            }
        },
    };
    public static final Instruction table_3[] = {
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000008) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.MFC0;
                } else {
                    return jpcsp.Allegrex.Instructions.ERET;
                }
            }
        },
        jpcsp.Allegrex.Instructions.CFC0,
        jpcsp.Allegrex.Instructions.MTC0,
        jpcsp.Allegrex.Instructions.CTC0,
    };
    public static final Instruction table_4[] = {
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.MFC1;
                } else {
                    return jpcsp.Allegrex.Instructions.CFC1;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.MTC1;
                } else {
                    return jpcsp.Allegrex.Instructions.CTC1;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_5[(insn & 0x00030000) >>> 16].instance(insn);
            }
        },
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                return table_6[(insn & 0x0000001f) >>> 0].instance(insn);
            }
        },
        jpcsp.Allegrex.Instructions.CVT_S_W,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
    };
    public static final Instruction table_5[] = {
        jpcsp.Allegrex.Instructions.BC1F,
        jpcsp.Allegrex.Instructions.BC1T,
        jpcsp.Allegrex.Instructions.BC1FL,
        jpcsp.Allegrex.Instructions.BC1TL,
    };
    public static final Instruction table_6[] = {
        jpcsp.Allegrex.Instructions.ADD_S,
        jpcsp.Allegrex.Instructions.SUB_S,
        jpcsp.Allegrex.Instructions.MUL_S,
        jpcsp.Allegrex.Instructions.DIV_S,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000020) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.SQRT_S;
                } else {
                    return jpcsp.Allegrex.Instructions.CVT_W_S;
                }
            }
        },
        jpcsp.Allegrex.Instructions.ABS_S,
        jpcsp.Allegrex.Instructions.MOV_S,
        jpcsp.Allegrex.Instructions.NEG_S,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.ROUND_W_S,
        jpcsp.Allegrex.Instructions.TRUNC_W_S,
        jpcsp.Allegrex.Instructions.CEIL_W_S,
        jpcsp.Allegrex.Instructions.FLOOR_W_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
        jpcsp.Allegrex.Instructions.C_COND_S,
    };
    public static final Instruction table_7[] = {
        jpcsp.Allegrex.Instructions.BVF,
        jpcsp.Allegrex.Instructions.BVT,
        jpcsp.Allegrex.Instructions.BVFL,
        jpcsp.Allegrex.Instructions.BVTL,
    };
    public static final Instruction table_8[] = {
        jpcsp.Allegrex.Instructions.VADD,
        jpcsp.Allegrex.Instructions.VSUB,
        jpcsp.Allegrex.Instructions.VSBN,
        jpcsp.Allegrex.Instructions.VDIV,
    };
    public static final Instruction table_9[] = {
        jpcsp.Allegrex.Instructions.VMUL,
        jpcsp.Allegrex.Instructions.VDOT,
        jpcsp.Allegrex.Instructions.VSCL,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.VHDP,
        jpcsp.Allegrex.Instructions.VDET,
        jpcsp.Allegrex.Instructions.VCRS,
        jpcsp.Allegrex.Instructions.UNK,
    };
    public static final Instruction table_10[] = {
        jpcsp.Allegrex.Instructions.VCMP,
        jpcsp.Allegrex.Instructions.UNK,
        jpcsp.Allegrex.Instructions.VMIN,
        jpcsp.Allegrex.Instructions.VMAX,
        jpcsp.Allegrex.Instructions.VSLT,
        jpcsp.Allegrex.Instructions.VSCMP,
        jpcsp.Allegrex.Instructions.VSGE,
        jpcsp.Allegrex.Instructions.UNK,
    };
    public static final Instruction table_11[] = {
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x02000000) == 0x00000000) {
                    if ((insn & 0x00400000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VMOV;
                    } else {
                        return jpcsp.Allegrex.Instructions.VSRT1;
                    }
                } else {
                    if ((insn & 0x01400000) == 0x00000000) {
                        if ((insn & 0x00800000) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.VF2IN;
                        } else {
                            return jpcsp.Allegrex.Instructions.VI2F;
                        }
                    } else {
                        if ((insn & 0x01000000) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.VF2IU;
                        } else {
                            return jpcsp.Allegrex.Instructions.VWBN;
                        }
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VABS;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VSRT2;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VNEG;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VBFY1;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VIDT;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VBFY2;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VSAT0;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VOCP;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VSAT1;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VSOCP;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VZERO;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VFAD;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VONE;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VAVG;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VSRT3;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VSRT4;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VRCP;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VMFVC;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VRSQ;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VMTVC;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VSIN;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCOS;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VEXP2;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VLOG2;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VSQRT;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VASIN;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VNRCP;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VT4444;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01400000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VNSIN;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VT5551;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VT5650;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VREXP2;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x02000000) == 0x00000000) {
                    if ((insn & 0x00400000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VRNDS;
                    } else {
                        return jpcsp.Allegrex.Instructions.VCST;
                    }
                } else {
                    if ((insn & 0x01400000) == 0x00400000) {
                        return jpcsp.Allegrex.Instructions.VF2ID;
                    } else {
                        if ((insn & 0x01800000) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.VF2IZ;
                        } else {
                            if ((insn & 0x01000000) == 0x00000000) {
                                return jpcsp.Allegrex.Instructions.VCMOVT;
                            } else {
                                return jpcsp.Allegrex.Instructions.VWBN;
                            }
                        }
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x02000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VRNDI;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VCMOVT;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x02000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VRNDF1;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VCMOVT;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x02000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VRNDF2;
                } else {
                    if ((insn & 0x01000000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VCMOVT;
                    } else {
                        return jpcsp.Allegrex.Instructions.VWBN;
                    }
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVT;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVT;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVT;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVT;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VCMOVF;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VF2H;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VH2F;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        jpcsp.Allegrex.Instructions.VWBN,
        jpcsp.Allegrex.Instructions.VWBN,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VSBZ;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VLGB;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VUC2I;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VC2I;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VUS2I;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VS2I;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VI2UC;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VI2C;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VI2US;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x01000000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VI2S;
                } else {
                    return jpcsp.Allegrex.Instructions.VWBN;
                }
            }
        },
    };
    public static final Instruction table_12[] = {
        jpcsp.Allegrex.Instructions.VPFXS,
        jpcsp.Allegrex.Instructions.VPFXT,
        jpcsp.Allegrex.Instructions.VPFXD,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00800000) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VIIM;
                } else {
                    return jpcsp.Allegrex.Instructions.VFIM;
                }
            }
        },
    };
    public static final Instruction table_13[] = {
        jpcsp.Allegrex.Instructions.VMMUL,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000080) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VHTFM2;
                } else {
                    return jpcsp.Allegrex.Instructions.VTFM2;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000080) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VTFM3;
                } else {
                    return jpcsp.Allegrex.Instructions.VHTFM3;
                }
            }
        },
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00000080) == 0x00000000) {
                    return jpcsp.Allegrex.Instructions.VHTFM4;
                } else {
                    return jpcsp.Allegrex.Instructions.VTFM4;
                }
            }
        },
        jpcsp.Allegrex.Instructions.VMSCL,
        jpcsp.Allegrex.Instructions.VQMUL,
        jpcsp.Allegrex.Instructions.UNK,
        new STUB() {

            @Override
            public Instruction instance(int insn) {
                if ((insn & 0x00210000) == 0x00000000) {
                    if ((insn & 0x00020000) == 0x00000000) {
                        return jpcsp.Allegrex.Instructions.VMMOV;
                    } else {
                        return jpcsp.Allegrex.Instructions.VMZERO;
                    }
                } else {
                    if ((insn & 0x00200000) == 0x00000000) {
                        if ((insn & 0x00040000) == 0x00000000) {
                            return jpcsp.Allegrex.Instructions.VMIDT;
                        } else {
                            return jpcsp.Allegrex.Instructions.VMONE;
                        }
                    } else {
                        return jpcsp.Allegrex.Instructions.VROT;
                    }
                }
            }
        },
    };

    public static final Instruction instruction(int insn) {
        return table_0[(insn & 0xfc000000) >>> 26].instance(insn);
    }
}
