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

public class R4000OpCodes {
/*
    CPU: Instructions encoded by opcode field.  NOT COMPLETE!! (31/07/1008) shadow
    31---------26---------------------------------------------------0
    |  opcode   |                                                   |
    ------6----------------------------------------------------------
    |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
000 | *1    | *2    | J     | JAL   | BEQ   | BNE   | BLEZ  | BGTZ  |
001 | ADDI  | ADDIU | SLTI  | SLTIU | ANDI  | ORI   | XORI  | LUI   |
010 | *3    | *4    | ---   | ---   | BEQL  | BNEL  | BLEZL | BGTZL |
011 | ---   | ---   | ---   | ---   | ---   |  ---  | ---   | ---   |
100 | LB    | LH    | LWL   | LW    | LBU   | LHU   | LWR   | ---   |
101 | SB    | SH    | SWL   | SW    | ---   | ---   | SWR   | CACHE |
110 | ---   | LWC1  | ---   | PREF  | ---   | ---   | ---   | ---   |
111 | ---   | SWC1  | ---   | ---   | ---   | ---   | ---   | ---   |
 hi |-------|-------|-------|-------|-------|-------|-------|-------|
     *1 = SPECIAL, see SPECIAL list    *2 = REGIMM, see REGIMM list
     *3 = COP0                         *4 = COP1     
*/
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
    public static final byte BEQL = 0x14; // Branch on Equal Likely
    public static final byte BNEL = 0x15; // Branch on Not Equal Likely
    public static final byte BLEZL = 0x16; // Branch on Less Than or Equal to Zero Likely
    public static final byte BGTZL = 0x17; // Branch on Greater Than Zero Likely
    public static final byte LB = 0x20; //Load Byte
    public static final byte LH = 0x21; // Load Halfword
    public static final byte LWL = 0x22; // Load Word Left
    public static final byte LW = 0x23; // Load Word
    public static final byte LBU = 0x24; // Load Byte Unsigned
    public static final byte LHU = 0x25; // Load Halfword Unsigned
    public static final byte LWR = 0x26; // Load Word Right
    public static final byte SB = 0x28; // Store Byte
    public static final byte SH = 0x29; // Store Halfword
    public static final byte SWL = 0x2A; // Store Word Left
    public static final byte SW = 0x2B; // Store Word
    public static final byte SWR = 0x2E; // Store Word Right
    public static final byte CACHE = 0x2f; // Cache Operation
    public static final byte LWC1 = 0x31;
    public static final byte PREF = 0x33;
    public static final byte SWC1 = 0x39;
    
    	  /*
     SPECIAL: Instr. encoded by function field when opcode field = SPECIAL  NOT COMPLETE!!! (31/07/2008) shadow
    31---------26------------------------------------------5--------0
    | = SPECIAL |                                         | function|
    ------6----------------------------------------------------6-----
    |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
000 | SLL   | ---   | SRL   | SRA   | SLLV  |  ---  | SRLV  | SRAV  |
001 | JR    | JALR  | ---   | ---   |SYSCALL| BREAK |  ---  | SYNC  |
010 | MFHI  | MTHI  | MFLO  | MTLO  | ---   |  ---  |  ---  | ---   |
011 | MULT  | MULTU | DIV   | DIVU  | ----  |  ---  | ----  | ----- |
100 | ADD   | ADDU  | SUB   | SUBU  | AND   | OR    | XOR   | NOR   |
101 | ---   |  ---  | SLT   | SLTU  | ---   | ---   | ---   | ---   |
110 | ---   |  ---  | ---   | ---   | ---   |  ---  | ---   | ---   |
111 | ---   |  ---  | ---   | ---   | ---   |  ---  | ---   | ---   |
 hi |-------|-------|-------|-------|-------|-------|-------|-------|
*/
    public static final byte SLL = 0x0; // Shift Left Logical
    public static final byte SRL = 0x2; // Shift Right Logical
    public static final byte SRA = 0x3; // Shift Right Arithmetic
    public static final byte SLLV = 0x4; // Shift Left Logical Variable
    public static final byte SRLV = 0x6; // Shift Right Logical Variable
    public static final byte SRAV = 0x7; // Shift Right Arithmetic Variable
    public static final byte JR = 0x08; // Jump Register
    public static final byte JALR = 0x09; // Jump And Link Register
    public static final byte SYSCALL = 0xc; // System Call
    public static final byte BREAK = 0xd; // Break
    public static final byte SYNC = 0xf; // Sync
    public static final byte MFHI = 0x10; // Move From HI
    public static final byte MTHI = 0x11; // Move To HI
    public static final byte MFLO = 0x12; // Move From LO
    public static final byte MTLO = 0x13; // Move To LO
    public static final byte MULT = 0x18; // Multiply
    public static final byte MULTU = 0x19; // Multiply Unsigned
    public static final byte DIV = 0x1a; // Divide
    public static final byte DIVU = 0x1b; // Divide Unsigned
    public static final byte ADD = 0x21; // Add
    public static final byte ADDU = 0x22; // Add Unsigned
    public static final byte SUB = 0x23; // Subtract
    public static final byte SUBU = 0x24; // Subtract Unsigned
    public static final byte AND = 0x25; // AND
    public static final byte OR = 0x26; // OR
    public static final byte XOR = 0x27; // Exclusive OR
    public static final byte NOR = 0x28; // NOR   
    public static final byte SLT = 0x2b; // Set on Less Than
    public static final byte SLTU = 0x2c; // Set on Less Than Unsigned
 /*
    REGIMM: Instructions encoded by the rt field when opcode field = REGIMM.  NOT COMPLETE!!! (31/07/2008) shadow
    31---------26----------20-------16------------------------------0
    | = REGIMM  |          |   rt    |                              |
    ------6---------------------5------------------------------------
    |--000--|--001--|--010--|--011--|--100--|--101--|--110--|--111--| lo
 00 | BLTZ  | BGEZ  | BLTZL | BGEZL |  ---  |  ---  |  ---  |  ---  |
 01 | ----  | ----  | ----  | ----  |  ---- |  ---  | ---   |  ---  |
 10 | BLTZAL| BGEZAL|BLTZALL|BGEZALL|  ---  |  ---  |  ---  |  ---  |
 11 | ----  | ----  |  ---  |  ---  |  ---  |  ---  |  ---  |  ---  |
 hi |-------|-------|-------|-------|-------|-------|-------|-------|
*/
    
    public static final byte BLTZ = 0x0; // Branch on Less Than Zero
    public static final byte BGEZ = 0x1; // Branch on Greater Than or Equal to Zero
    public static final byte BLTZL = 0x2; // Branch on Less Than Zero Likely
    public static final byte BGEZL = 0x3; // Branch on Greater Than or Equal to Zero Likely
    public static final byte BLTZAL = 0x10; // Branch on Less Than Zero And Link
    public static final byte BGEZAL = 0x11; // Branch on Greater Than or Equal to Zero And Link    
    public static final byte BLTZALL = 0x12; // Branch on Less Than Zero And Link Likely
    public static final byte BGEZALL = 0x13; // Branch on Greater Than or Equal to Zero And Link Likely
    
    
    
    //These are not sure they exist and will be add later when emu will become more advance. (shadow)
   /* 
    //Coprocessor Instructions CO
    public static final byte LWCz = 0x0; // Load Word to Coprocessor z
    public static final byte SWCz = 0x0; // Store Word from Coprocessor z
    public static final byte MTCz = 0x0; // Move To Coprocessor z
    public static final byte MFCz = 0x0; // Move From Coprocessor z
    public static final byte CTCz = 0x0; // Move Control to Coprocessor z
    public static final byte CFCz = 0x0; // Move Control From Coprocessor z
    
    public static final byte BCzT = 0x0; // Branch on Coprocessor z True
    public static final byte BCzF = 0x0; // Branch on Coprocessor z False
     
    //Extensions to the ISA: Load and Store Instructions
    public static final byte LD = 0x37; // Load Doubleword
    public static final byte LDL = 0x1a; // Load Doubleword Left
    public static final byte LDR = 0x1b; // Load Doubleword Right
    public static final byte LL = 0x30; // Load Linked
    public static final byte LLD = 0x34; // Load Linked Doubleword
    public static final byte LWU = 0x27; // Load Word Unsigned
    public static final byte SC = 0x38; // Store Conditional
    public static final byte SCD = 0x3c; // Store Conditional Doubleword
    public static final byte SD = 0x3f; // Store Doubleword
    public static final byte SDL = 0x2c; // Store Doubleword Left
    public static final byte SDR = 0x2d; // Store Doubleword Right
   
    //Extensions to the ISA: Arithmetic Instructions (ALU Immediate)
    public static final byte DADDI = 0x18; // Doubleword Add Immediate
    public static final byte DADDIU = 0x19; // Doubleword Add Immediate Unsigned
            
    //Extensions to the ISA: Multiply and Divide Instructions
    public static final byte DMULT = 0x0; // Doubleword Multiply
    public static final byte DMULTU = 0x0; // Doubleword Multiply Unsigned
    public static final byte DDIV = 0x0; // Doubleword Divide
    public static final byte DDIVU = 0x0; // Doubleword Divide Unsigned
    
    
    //Extensions to the ISA: Branch Instructions

    public static final byte BCzTL = 0x0; // Branch on Coprocessor z True Likely
    public static final byte BCzFL = 0x0; // Branch on Coprocessor z False Likely
                     
    //Extensions to the ISA: Arithmetic Instructions (3-operand, R-type)
    public static final byte DADD = 0x0; // Doubleword Add
    public static final byte DADDU = 0x0; // Doubleword Add Unsigned
    public static final byte DSUB = 0x0; // Doubleword Subtract
    public static final byte DSUBU = 0x0; // Doubleword Subtract Unsigned
    
    //Extensions to the ISA: Shift Instructions
    public static final byte DSLL = 0x0; // Doubleword Shift Left Logical
    public static final byte DSRL = 0x0; // Doubleword Shift Right Logical
    public static final byte DSRA = 0x0; // Doubleword Shift Right Arithmetic
    public static final byte DSLLV = 0x0; // Doubleword Shift Left Logical Variable
    public static final byte DSRLV = 0x0; // Doubleword Shift Right Logical Variable
    public static final byte DSRAV = 0x0; // Doubleword Shift Right Arithmetic Variable
    public static final byte DSLL32 = 0x0; // Doubleword Shift Left Logical + 32
    public static final byte DSRL32 = 0x0; // Doubleword Shift Right Logical + 32
    public static final byte DSRA32 = 0x0; // Doubleword Shift Right Arithmetic + 32
    
    
    //Extensions to the ISA: Exception Instructions
    public static final byte TGE = 0x0; // Trap if Greater Than or Equal
    public static final byte TGEU = 0x0; // Trap if Greater Than or Equal Unsigned
    public static final byte TLT = 0x0; // Trap if Less Than
    public static final byte TLTU = 0x0; // Trap if Less Than Unsigned
    public static final byte TEQ = 0x0; // Trap if Equal
    public static final byte TNE = 0x0; // Trap if Not Equal
    public static final byte TGEI = 0x1; // Trap if Greater Than or Equal Immediate
    public static final byte TGEIU = 0x1; // Trap if Greater Than or Equal Immediate Unsigned
    public static final byte TLTI = 0x1; // Trap if Less Than Immediate
    public static final byte TLTIU = 0x1; // Trap if Less Than Immediate Unsigned
    public static final byte TEQI = 0x1; // Trap if Equal Immediate
    public static final byte TNEI = 0x1; // Trap if Not Equal Immediate
    
    
    //Extensions to the ISA: Coprocessor Instructions
    public static final byte DMFCz = 0x0; // Doubleword Move From Coprocessor z
    public static final byte DMTCz = 0x0; // Doubleword Move To Coprocessor z
    public static final byte LDCz = 0x0; // Load Double Coprocessor z
    public static final byte SDCz = 0x0; // Store Double Coprocessor z
    
    
    //CP0 Instructions
    public static final byte DMFC0 = 0x10; // Doubleword Move From CP0
    public static final byte DMTC0 = 0x10; // Doubleword Move To CP0
    public static final byte MTC0 = 0x10; // Move to CP0
    public static final byte MFC0 = 0x10; // Move from CP0
    public static final byte TLBR = 0x10; // Read Indexed TLB Entry
    public static final byte TLBWI = 0x10; // Write Indexed TLB Entry
    public static final byte TLBWR = 0x10; // Write Random TLB Entry
    public static final byte TLBP = 0x10; // Probe TLB for Matching Entry
    
    public static final byte ERET = 0x10; // Exception Return            */
}
