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

// 2008-8-5 [Hlide] Allegrex Opcodes
public class AllegrexOpcodes {

// CPU: encoded by opcode field.
// 
//     31---------26---------------------------------------------------0
//     |  opcode   |                                                   |
//     ------6----------------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 | *1    | *2    | J     | JAL   | BEQ   | BNE   | BLEZ  | BGTZ  |
// 001 | ADDI  | ADDIU | SLTI  | SLTIU | ANDI  | ORI   | XORI  | LUI   |
// 010 | *3    | *4    | VFPU2 | ---   | BEQL  | BNEL  | BLEZL | BGTZL |
// 011 | VFPU0 | VFPU1 |  ---  | VFPU3 | * 5   | ---   | ---   | *6    |
// 100 | LB    | LH    | LWL   | LW    | LBU   | LHU   | LWR   | ---   |
// 101 | SB    | SH    | SWL   | SW    | ---   | ---   | SWR   | CACHE |
// 110 | LL    | LWC1  | LVS   | ---   | VFPU4 | ULVQ  | LVQ   | VFPU5 |
// 111 | SC    | SWC1  | SVS   | ---   | VFPU6 | USVQ  | SVQ   | VFPU7 |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
//  
//      *1 = SPECIAL, see SPECIAL list    *2 = REGIMM, see REGIMM list
//      *3 = COP0                         *4 = COP1  
//      *5 = SPECIAL2 , see SPECIAL2      *6 = SPECAIL3 , see SPECIAL 3
//      *ULVQ is buggy on PSP1000 PSP 
//      *VFPU0 check VFPU0 table
//      *VFPU1 check VFPU1 table
//      *VFPU2 check VFPU2 table
//      *VFPU3 check VFPU3 table
//      *VFPU4 check VFPU4 table
//      *VFPU5 check VFPU5 table
//      *VFPU6 check VFPU6 table
//      *VFPU7 check VFPU7 table
    public static final byte SPECIAL = 0x0;
    public static final byte REGIMM = 0x1;
    public static final byte J = 0x2; // Jump
    public static final byte JAL = 0x3; // Jump And Link
    public static final byte BEQ = 0x4; // Branch on Equal
    public static final byte BNE = 0x5; // Branch on Not Equal
    public static final byte BLEZ = 0x6; // Branch on Less Than or Equal to Zero
    public static final byte BGTZ = 0x7; // Branch on Greater Than Zero
    public static final byte ADDI = 0x8; // Add Immediate
    public static final byte ADDIU = 0x9; // Add Immediate Unsigned
    public static final byte SLTI = 0xa; // Set on Less Than Immediate
    public static final byte SLTIU = 0xb; // Set on Less Than Immediate Unsigned
    public static final byte ANDI = 0xc; // AND Immediate
    public static final byte ORI = 0xd; // OR Immediate
    public static final byte XORI = 0xe; // Exclusive OR Immediate
    public static final byte LUI = 0xf; // Load Upper Immediate
    public static final byte COP0 = 0x10; // Coprocessor Operation 
    public static final byte COP1 = 0x11; // Coprocessor Operation 
    public static final byte VFPU2 = 0x12;
    /*  0x13 reserved or unsupported */
    public static final byte BEQL = 0x14; // Branch on Equal Likely
    public static final byte BNEL = 0x15; // Branch on Not Equal Likely
    public static final byte BLEZL = 0x16; // Branch on Less Than or Equal to Zero Likely
    public static final byte BGTZL = 0x17; // Branch on Greater Than Zero Likely
    public static final byte VFPU0 = 0x18;
    public static final byte VFPU1 = 0x19;
    /*  0x1a reserved or unsupported */
    public static final byte VFPU3 = 0x1b;
    public static final byte SPECIAL2 = 0x1c; // Allegrex table
    /*  0x1d reserved or unsupported */
    /*  0x1e reserved or unsupported */
    public static final byte SPECIAL3 = 0x1f; //special3 table
    public static final byte LB = 0x20; //Load Byte
    public static final byte LH = 0x21; // Load Halfword
    public static final byte LWL = 0x22; // Load Word Left
    public static final byte LW = 0x23; // Load Word
    public static final byte LBU = 0x24; // Load Byte Unsigned
    public static final byte LHU = 0x25; // Load Halfword Unsigned
    public static final byte LWR = 0x26; // Load Word Right
    /*  0x27 reserved or unsupported */
    public static final byte SB = 0x28; // Store Byte
    public static final byte SH = 0x29; // Store Halfword
    public static final byte SWL = 0x2A; // Store Word Left
    public static final byte SW = 0x2B; // Store Word
    /*  0x2c reserved or unsupported */
    /*  0x2d reserved or unsupported */
    public static final byte SWR = 0x2E; // Store Word Right
    public static final byte CACHE = 0x2f; // Allegrex Cache Operation
    public static final byte LL = 0x30; // Load Linked
    public static final byte LWC1 = 0x31; // Load FPU Register
    public static final byte LVS = 0x32; // Load Scalar VFPU Register
    /*  0x32 reserved or unsupported */
    /*  0x33 reserved or unsupported */
    public static final byte VFPU4 = 0x34;
    public static final byte ULVQ = 0x35; // Load Quad VFPU Register (Unaligned)
    public static final byte LVQ = 0x36; // Load Quad VFPU Register
    public static final byte VFPU5 = 0x37;
    public static final byte SC = 0x38; // Store Conditionaly
    public static final byte SWC1 = 0x39; // Store FPU Register
    public static final byte SVS = 0x3a; // Store Scalar VFPU Register
    /*  0x3b reserved or unsupported */
    public static final byte VFPU6 = 0x3c;
    public static final byte USVQ = 0x3d; // Store Quad VFPU Register (Unaligned)
    public static final byte SVQ = 0x3e; // Store Quad VFPU Register
    public static final byte VFPU7 = 0x3f;// SPECIAL: encoded by function field when opcode field = SPECIAL
// 
//     31---------26------------------------------------------5--------0
//     |=   SPECIAL|                                         | function|
//     ------6----------------------------------------------------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 | SLL   | ---   |SRLROR | SRA   | SLLV  |  ---  |SRLRORV| SRAV  |
// 001 | JR    | JALR  | MOVZ  | MOVN  |SYSCALL| BREAK |  ---  | SYNC  |
// 010 | MFHI  | MTHI  | MFLO  | MTLO  | ---   |  ---  |  CLZ  | CLO   |
// 011 | MULT  | MULTU | DIV   | DIVU  | MADD  | MADDU | ----  | ----- |
// 100 | ADD   | ADDU  | SUB   | SUBU  | AND   | OR    | XOR   | NOR   |
// 101 | ---   |  ---  | SLT   | SLTU  | MAX   | MIN   | MSUB  | MSUBU |
// 110 | ---   |  ---  | ---   | ---   | ---   |  ---  | ---   | ---   |
// 111 | ---   |  ---  | ---   | ---   | ---   |  ---  | ---   | ---   |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte SLL = 0x0; // Shift Left Logical
    /*  0x1 reserved or unsupported */
    public static final byte SRLROR = 0x2; // Shift/Rotate Right Logical
    public static final byte SRA = 0x3; // Shift Right Arithmetic
    public static final byte SLLV = 0x4; // Shift Left Logical Variable
    /*  0x5 reserved or unsupported */
    public static final byte SRLRORV = 0x6; // Shift/Rotate Right Logical Variable
    public static final byte SRAV = 0x7; // Shift Right Arithmetic Variable
    public static final byte JR = 0x8; // Jump Register
    public static final byte JALR = 0x9; // Jump And Link Register
    public static final byte MOVZ = 0xa; // Move If Zero
    public static final byte MOVN = 0xb; // Move If Non-zero
    public static final byte SYSCALL = 0xc; // System Call
    public static final byte BREAK = 0xd; // Break
    /*  0xe reserved or unsupported */
    public static final byte SYNC = 0xf; // Sync
    public static final byte MFHI = 0x10; // Move From HI
    public static final byte MTHI = 0x11; // Move To HI
    public static final byte MFLO = 0x12; // Move From LO
    public static final byte MTLO = 0x13; // Move To LO
    /*  0x14 reserved or unsupported */
    /*  0x15 reserved or unsupported */
    public static final byte CLZ = 0x16; // Count Leading Zero
    public static final byte CLO = 0x17; // Count Leading One
    public static final byte MULT = 0x18; // Multiply
    public static final byte MULTU = 0x19; // Multiply Unsigned
    public static final byte DIV = 0x1a; // Divide
    public static final byte DIVU = 0x1b; // Divide Unsigned
    public static final byte MADD = 0x1c; // Multiply And Add
    public static final byte MADDU = 0x1d; // Multiply And Add Unsigned
    /*  0x1e reserved or unsupported */
    /*  0x1f reserved or unsupported */
    public static final byte ADD = 0x20; // Add
    public static final byte ADDU = 0x21; // Add Unsigned
    public static final byte SUB = 0x22; // Subtract
    public static final byte SUBU = 0x23; // Subtract Unsigned
    public static final byte AND = 0x24; // AND
    public static final byte OR = 0x25; // OR
    public static final byte XOR = 0x26; // Exclusive OR
    public static final byte NOR = 0x27; // NOR   
    /*  0x28 reserved or unsupported */
    /*  0x29 reserved or unsupported */
    public static final byte SLT = 0x2a; // Set on Less Than
    public static final byte SLTU = 0x2b; // Set on Less Than Unsigned
    public static final byte MAX = 0x2c; // Move Max
    public static final byte MIN = 0x2d; // Move Min
    public static final byte MSUB = 0x2e; // Multiply And Substract
    public static final byte MSUBU = 0x2f; // Multiply And Substract

// SPECIAL rs : encoded by rs field when opcode/func field = SPECIAL/SRLROR
// 
//     31---------26-----21-----------------------------------5--------0
//     |=   SPECIAL| rs  |                                    |= SRLROR|
//     ------6--------5-------------------------------------------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 | SRL   | ROTR  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 001 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 010 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 011 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte SRL = 0x0;
    public static final byte ROTR = 0x1;// SPECIAL sa : encoded by sa field when opcode/func field = SPECIAL/SRLRORV
// 
//     31---------26------------------------------------10----5--------0
//     |=   SPECIAL|                                    | sa  |=SRLRORV|
//     ------6---------------------------------------------5------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 | SRLV  | ROTRV |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 001 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 010 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 011 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte SRLV = 0x0;
    public static final byte ROTRV = 0x1; //
//     REGIMM: encoded by the rt field when opcode field = REGIMM.
//     31---------26----------20-------16------------------------------0
//     |=    REGIMM|          |   rt    |                              |
//     ------6---------------------5------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
//  00 | BLTZ  | BGEZ  | BLTZL | BGEZL |  ---  |  ---  |  ---  |  ---  |
//  01 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  10 | BLTZAL| BGEZAL|BLTZALL|BGEZALL|  ---  |  ---  |  ---  |  ---  |
//  11 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte BLTZ = 0x0; // Branch on Less Than Zero
    public static final byte BGEZ = 0x1; // Branch on Greater Than or Equal to Zero
    public static final byte BLTZL = 0x2; // Branch on Less Than Zero Likely
    public static final byte BGEZL = 0x3; // Branch on Greater Than or Equal to Zero Likely
    /*  0x4 reserved or unsupported */
    /*  0x5 reserved or unsupported */
    /*  0x6 reserved or unsupported */
    /*  0x7 reserved or unsupported */
    /*  0x8 reserved or unsupported */
    /*  0x9 reserved or unsupported */
    /*  0xa reserved or unsupported */
    /*  0xb reserved or unsupported */
    /*  0xc reserved or unsupported */
    /*  0xd reserved or unsupported */
    /*  0xe reserved or unsupported */
    /*  0xf reserved or unsupported */
    public static final byte BLTZAL = 0x10; // Branch on Less Than Zero And Link
    public static final byte BGEZAL = 0x11; // Branch on Greater Than or Equal to Zero And Link    
    public static final byte BLTZALL = 0x12; // Branch on Less Than Zero And Link Likely
    public static final byte BGEZALL = 0x13; // Branch on Greater Than or Equal to Zero And Link Likely
//     COP0: encoded by the rs field when opcode field = COP0.
//     31---------26----------23-------31------------------------------0
//     |=      COP0|          |   rs    |                              |
//     ------6---------------------5------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
//  00 |  MFC0 |  ---  |  CFC0 |  ---  |  MTC0 |  ---  |  CTC0 |  ---  |
//  01 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  10 |  *1   |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  11 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
//
//     *1 COP0 func
    public static final byte MFC0 = 0x0; // Move from Coprocessor 0
    public static final byte CFC0 = 0x2; // Move from Coprocessor 0
    public static final byte MTC0 = 0x4; // Move to Coprocessor 0
    public static final byte CTC0 = 0x6; // Move to Coprocessor 0
    public static final byte COP0ERET = 0x10;//     COP0: encoded by the func field when opcode/rs field = COP0/10000.
//     31---------26------------------------------------------5--------0
//     |=      COP0|                                         | function|
//     ------6----------------------------------------------------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
//  00 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  01 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  10 |  ERET |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  11 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte ERET = 0x10; // Exception Return            */

//     SPECIAL2 : encoded by function field when opcode field = SPECIAL2
//     31---------26------------------------------------------5--------0
//     |=  SPECIAL2|                                         | function|
//     ------6----------------------------------------------------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 | HALT  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 001 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 010 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 011 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 100 |  ---  |  ---  |  ---  |  ---  | MFIC  |  ---  | MTIC  |  ---  |
// 101 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 110 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 111 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte HALT = 0x0; // halt execution until next interrupt
    public static final byte MFIC = 0x24; // move from IC (Interrupt) register
    public static final byte MTIC = 0x26; // move to IC (Interrupt) register

//     SPECIAL3: encoded by function field when opcode field = SPECIAL3
//     31---------26------------------------------------------5--------0
//     |=  SPECIAL3|                                         | function|
//     ------6----------------------------------------------------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 |  EXT  |  ---  |  ---  |  ---  |  INS  |  ---  |  ---  |  ---  |
// 001 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 010 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 011 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 100 |  *1   |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 101 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 110 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 111 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
//       * 1 BSHFL encoding based on sa field
    public static final byte EXT = 0x0; // extract bit field
    public static final byte INS = 0x4; // insert bit field
    public static final byte BSHFL = 0x20; //BSHFL table
//     BSHFL: encoded by the sa field.
//     31---------26----------20-------16--------------8---6-----------0
//     |          |          |         |               | sa|           |
//     ------6---------------------5------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
//  00 |  ---  |  ---  | WSBH  | WSBW  |  ---  |  ---  |  ---  |  ---  |
//  01 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  10 |  SEB  |  ---  |  ---  |  ---  |BITREV |  ---  |  ---  |  ---  |
//  11 |  SEH  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte WSBH = 0x02; // Swap Bytes In Each Half Word
    public static final byte WSBW = 0x03; // Swap Bytes In Word
    public static final byte SEB = 0x10; // Sign-Extend Byte
    public static final byte BITREV = 0x14; // Revert Bits In Word
    public static final byte SEH = 0x18; // Sign-Extend HalfWord

//     COP1: encoded by the rs field when opcode field = COP1.
//     31-------26------21---------------------------------------------0
//     |=    COP1|  rs  |                                              |
//     -----6-------5---------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
//  00 |  MFC1 |  ---  |  CFC1 |  ---  |  MTC1 |  ---  |  CTC1 |  ---  |
//  01 |  *1   |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  10 |  *2   |  ---  |  ---  |  ---  |  *3   |  ---  |  ---  |  ---  |
//  11 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
//    *1 check COP1BC table
//    *2 check COP1S table;
//    *2 check COP1W table;
    public static final byte MFC1 = 0x00;
    public static final byte CFC1 = 0x02;
    public static final byte MTC1 = 0x04;
    public static final byte CTC1 = 0x06;
    public static final byte COP1BC = 0x08;
    public static final byte COP1S = 0x10;
    public static final byte COP1W = 0x14; //
//     COP1BC: encoded by the rt field
//     31---------21-------16------------------------------------------0
//     |=    COP1BC|  rt   |                                           |
//     ------11---------5-----------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
//  00 |  BC1F | BC1T  | BC1FL | BC1TL |  ---  |  ---  |  ---  |  ---  |
//  01 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  10 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  11 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte BC1F = 0x00;
    public static final byte BC1T = 0x01;
    public static final byte BC1FL = 0x02;
    public static final byte BC1TL = 0x03; //   
//     COP1S: encoded by function field
//     31---------21------------------------------------------5--------0
//     |=  COP1S  |                                          | function|
//     -----11----------------------------------------------------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 | add.s | sub.s | mul.s | div.s |sqrt.s | abs.s | mov.s | neg.s |
// 001 |  ---  |  ---  |  ---  |  ---  |            <*1>.w.s           |
// 010 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 011 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 100 |  ---  |  ---  |  ---  |  ---  |cvt.w.s|  ---  |  ---  |  ---  |
// 101 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 110 |                            c.<*2>.s                           |
// 110 |                            c.<*3>.s                           |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|   
//
// *1 : round.w.s | trunc.w.s | ceil.w.s | floor.w.s
// *2 : c.f.s | c.un.s | c.eq.s | c.ueq.s | c.olt.s | c.ult.s | c.ole.s | c.ule.s
// *3 : c.sf.s | c.ngle.s | c.seq.s | c.ngl.s | c.lt.s | c.nge.s | c.le.s  | c.ngt.s
//
    public static final byte ADDS = 0x00;
    public static final byte SUBS = 0x01;
    public static final byte MULS = 0x02;
    public static final byte DIVS = 0x03;
    public static final byte SQRTS = 0x04;
    public static final byte ABSS = 0x05;
    public static final byte MOVS = 0x06;
    public static final byte NEGS = 0x07;
    public static final byte ROUNDWS = 0xc;
    public static final byte TRUNCWS = 0xd;
    public static final byte CEILWS = 0xe;
    public static final byte FLOORWS = 0xf;
    public static final byte CVTWS = 0x24;
    public static final byte CCONDS = 0x30;
    public static final byte CF = 0x0;
    public static final byte CUN = 0x1;
    public static final byte CEQ = 0x2;
    public static final byte CUEQ = 0x3;
    public static final byte COLT = 0x4;
    public static final byte CULT = 0x5;
    public static final byte COLE = 0x6;
    public static final byte CULE = 0x7;
    public static final byte CSF = 0x8;
    public static final byte CNGLE = 0x9;
    public static final byte CSEQ = 0xa;
    public static final byte CNGL = 0xb;
    public static final byte CLT = 0xc;
    public static final byte CNGE = 0xd;
    public static final byte CLE = 0xe;
    public static final byte CNGT = 0xf; //
//     COP1W: encoded by function field
//     31---------21------------------------------------------5--------0
//     |=  COP1W  |                                          | function|
//     -----11----------------------------------------------------6-----
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
// 000 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 001 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 010 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 011 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 100 |cvt.s.w|  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 101 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 110 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
// 110 |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//  hi |-------|-------|-------|-------|-------|-------|-------|-------|   
    public static final byte CVTSW = 0x20; //
// VFPU2: /* known as COP2 */
    public static final byte MFVMFVC = 0x00;
    public static final byte MTVMTVC = 0x04;
    public static final byte VFPU2BC = 0x08; //
    public static final byte MFV = 0x0;
    public static final byte MFVC = 0x1;
    public static final byte MTV = 0x0;
    public static final byte MTVC = 0x1;
// VFPU0:
// 
//     31---------26-----23--------------------------------------------0
//     |=     VFPU0| VOP |                                             |
//     ------6--------3-------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--|
//     | VADD  | VSUB  | VSBN  |  ---  |  ---  |  ---  |  ---  | VDIV  |
//     |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte VADD = 0x00; //
    public static final byte VSUB = 0x01; //
    public static final byte VSBN = 0x02; //
    public static final byte VDIV = 0x07; //
// VFPU1:
// 
//     31---------26-----23--------------------------------------------0
//     |=     VFPU1| VOP |                                             |
//     ------6--------3-------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--|
//     | VMUL  | VDOT  | VSCL  |  ---  | VHDP  | VCRS  | VDET  |  ---  |
//     |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte VMUL = 0x00; //
    public static final byte VDOT = 0x01; //
    public static final byte VSCL = 0x02; //
    public static final byte VHDP = 0x04; //
    public static final byte VCRS = 0x05; //
    public static final byte VDET = 0x06; //

// VFPU3:
// 
//     31---------26-----23--------------------------------------------0
//     |=     VFPU3| VOP |                                             |
//     ------6--------3-------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--|
//     | VCMP  |  ---  | VMIN  | VMAX  |  ---  | VSCMP | VSGE  | VSLT  |
//     |-------|-------|-------|-------|-------|-------|-------|-------|
    public static final byte VCMP = 0x00; //
    public static final byte VMIN = 0x02; //
    public static final byte VMAX = 0x03; //
    public static final byte VSCMP = 0x05; //
    public static final byte VSGE = 0x06; //
    public static final byte VSLT = 0x07; //
// VFPU4:
//     31---------26-----24--------------------------------------------0
//     |=     VFPU4| VOP |                                             |
//     ------6--------2-------------------------------------------------
//     |-------00-------|-------01-------|------10------|------11------|
//     |    VFPU4_0     |      ---       |    VFPU4_2   |     VWBN     |
//     |----------------|----------------|--------------|--------------|
    public static final byte VFPU4_0 = 0x0; //
    public static final byte VFPU4_1 = 0x1; //
    public static final byte VFPU4_2 = 0x2; //
    public static final byte VWBN = 0x3; //

// VFPU4_0:
//     31---------26-----24--------------------------------------------0
//     |=     VFPU4| 00  |                                             |
//     ------6--------2-------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--|
//     |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//     |-------|-------|-------|-------|-------|-------|-------|-------|
//  

// VFPU4_2:
//     31---------26-----24--------------------------------------------0
//     |=     VFPU4| 10  |                                             |
//     ------6--------2-------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--|
//     | VF2IN | VF2IZ | VF2IU | VF2ID | VI2F  |   1*  |  ---  |  ---  |
//     |-------|-------|-------|-------|-------|-------|-------|-------|
//     *1 : VCMOVF/VCMOVT
    public static final byte VF2IN = 0x0; //
    public static final byte VF2IZ = 0x1; //
    public static final byte VF2IU = 0x2; //
    public static final byte VF2ID = 0x3; //
    public static final byte VI2F = 0x4; //
    public static final byte VFPU4_2_2 = 0x5; //

// VFPU4_2_2:
//     31---------26----24----19---------------------------------------0
//     |=     VFPU4| 10 | 101 |                                        |
//     ------6--------2----3--------------------------------------------
//     |-------00-------|-------01-------|------10------|------11------|
//     |     VCMOVF     |     VCMOVT     |     ----     |     ----     |
//     |----------------|----------------|--------------|--------------|
    public static final byte VCMOVF = 0x0; //
    public static final byte VCMOVT = 0x1; //

// VFPU5:
// 
//     31---------26----24---------------------------------------------0
//     |=     VFPU5| VOP |                                             |
//     ------6--------2-------------------------------------------------
//     |-------00-------|-------01-------|-------10-----|------11------|
//     |     VPFXS      |     VPFXT      |     VPFXD    |  VIIM/VFIM   |
//     |----------------|----------------|--------------|--------------|
    public static final byte VPFXS = 0x00;
    public static final byte VPFXT = 0x01;
    public static final byte VPFXD = 0x02;
    public static final byte VFPU5_3 = 0x03; //
//     31---------------23---------------------------------------------0
//     |=   VFPU5/VIFM  |                                              |
//     ---------8-------------------------------------------------------
//     |----------------0----------------|--------------1--------------|
//     |              VIIM               |            VFIM             |
//     |---------------------------------|-----------------------------|   
    public static final byte VIIM = 0x0;
    public static final byte VFIM = 0x1;    // VFPU6:
// 
//     31---------26-----23--------------------------------------------0
//     |=     VFPU6| VOP |                                             |
//     ------6--------3-------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--|
//     |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//     |-------|-------|-------|-------|-------|-------|-------|-------|
//  

// VFPU7:
// 
//     31---------26-----23--------------------------------------------0
//     |=     VFPU6| VOP |                                             |
//     ------6--------3-------------------------------------------------
//     |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--|
//     |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
//     |-------|-------|-------|-------|-------|-------|-------|-------|
//  
}            