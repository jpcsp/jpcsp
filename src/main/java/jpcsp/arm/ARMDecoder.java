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
package jpcsp.arm;

import static jpcsp.arm.ARMInstructions.ADC;
import static jpcsp.arm.ARMInstructions.ADC_Thumb;
import static jpcsp.arm.ARMInstructions.ADD;
import static jpcsp.arm.ARMInstructions.ADD_High_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Reg_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Rd_Pc_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Rd_Sp_Thumb;
import static jpcsp.arm.ARMInstructions.ADD_Sp_Thumb;
import static jpcsp.arm.ARMInstructions.AND;
import static jpcsp.arm.ARMInstructions.AND_Thumb;
import static jpcsp.arm.ARMInstructions.ASR_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.ASR_Thumb;
import static jpcsp.arm.ARMInstructions.B;
import static jpcsp.arm.ARMInstructions.BIC;
import static jpcsp.arm.ARMInstructions.BIC_Thumb;
import static jpcsp.arm.ARMInstructions.BKPT;
import static jpcsp.arm.ARMInstructions.BKPT_Thumb;
import static jpcsp.arm.ARMInstructions.BL;
import static jpcsp.arm.ARMInstructions.BLX_01_Thumb;
import static jpcsp.arm.ARMInstructions.BLX_Thumb;
import static jpcsp.arm.ARMInstructions.BLX_uncond;
import static jpcsp.arm.ARMInstructions.BL_10_Thumb;
import static jpcsp.arm.ARMInstructions.BL_11_Thumb;
import static jpcsp.arm.ARMInstructions.BX;
import static jpcsp.arm.ARMInstructions.BX_Thumb;
import static jpcsp.arm.ARMInstructions.B_Cond_Thumb;
import static jpcsp.arm.ARMInstructions.B_Thumb;
import static jpcsp.arm.ARMInstructions.CDP;
import static jpcsp.arm.ARMInstructions.CMN;
import static jpcsp.arm.ARMInstructions.CMN_Thumb;
import static jpcsp.arm.ARMInstructions.CMP;
import static jpcsp.arm.ARMInstructions.CMP_High_Thumb;
import static jpcsp.arm.ARMInstructions.CMP_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.CMP_Thumb;
import static jpcsp.arm.ARMInstructions.EOR;
import static jpcsp.arm.ARMInstructions.EOR_Thumb;
import static jpcsp.arm.ARMInstructions.LDM;
import static jpcsp.arm.ARMInstructions.LDM_Thumb;
import static jpcsp.arm.ARMInstructions.LDR;
import static jpcsp.arm.ARMInstructions.LDRB_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.LDRB_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.LDRH_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.LDRH_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.LDRSB_Thumb;
import static jpcsp.arm.ARMInstructions.LDRSH_Thumb;
import static jpcsp.arm.ARMInstructions.LDR_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.LDR_Pc_Thumb;
import static jpcsp.arm.ARMInstructions.LDR_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.LDR_Stack_Thumb;
import static jpcsp.arm.ARMInstructions.LSL_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.LSL_Thumb;
import static jpcsp.arm.ARMInstructions.LSR_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.LSR_Thumb;
import static jpcsp.arm.ARMInstructions.MCR;
import static jpcsp.arm.ARMInstructions.MOV;
import static jpcsp.arm.ARMInstructions.MOV_High_Thumb;
import static jpcsp.arm.ARMInstructions.MOV_Immediate_Thumb;
import static jpcsp.arm.ARMInstructions.MRC;
import static jpcsp.arm.ARMInstructions.MRS;
import static jpcsp.arm.ARMInstructions.MSR;
import static jpcsp.arm.ARMInstructions.MUL_Thumb;
import static jpcsp.arm.ARMInstructions.MVN;
import static jpcsp.arm.ARMInstructions.MVN_Thumb;
import static jpcsp.arm.ARMInstructions.NEG_Thumb;
import static jpcsp.arm.ARMInstructions.ORR;
import static jpcsp.arm.ARMInstructions.ORR_Thumb;
import static jpcsp.arm.ARMInstructions.POP_Thumb;
import static jpcsp.arm.ARMInstructions.PUSH_Thumb;
import static jpcsp.arm.ARMInstructions.ROR_Thumb;
import static jpcsp.arm.ARMInstructions.RSB;
import static jpcsp.arm.ARMInstructions.RSC;
import static jpcsp.arm.ARMInstructions.SBC;
import static jpcsp.arm.ARMInstructions.SBC_Thumb;
import static jpcsp.arm.ARMInstructions.STM;
import static jpcsp.arm.ARMInstructions.STM_Thumb;
import static jpcsp.arm.ARMInstructions.STR;
import static jpcsp.arm.ARMInstructions.STRB_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.STRB_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.STRH_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.STRH_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.STR_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.STR_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.STR_Stack_Thumb;
import static jpcsp.arm.ARMInstructions.SUB;
import static jpcsp.arm.ARMInstructions.SUB_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.SUB_Reg_Imm_Thumb;
import static jpcsp.arm.ARMInstructions.SUB_Reg_Thumb;
import static jpcsp.arm.ARMInstructions.SUB_Sp_Thumb;
import static jpcsp.arm.ARMInstructions.SWI;
import static jpcsp.arm.ARMInstructions.SWI_Thumb;
import static jpcsp.arm.ARMInstructions.TEQ;
import static jpcsp.arm.ARMInstructions.TST;
import static jpcsp.arm.ARMInstructions.TST_Thumb;
import static jpcsp.arm.ARMInstructions.UNK;
import static jpcsp.arm.ARMInstructions.UNK_Thumb;
import static jpcsp.util.Utilities.hasFlag;

import jpcsp.arm.ARMInstructions.STUB;

public class ARMDecoder {
    private static final ARMInstruction MCR_CDP = new STUB() {
        @Override
        public ARMInstruction instance(int insn) {
        	return hasFlag(insn, 0x00000010) ? MCR : CDP;
        }
    };

    private static final ARMInstruction MRC_CDP = new STUB() {
        @Override
        public ARMInstruction instance(int insn) {
        	return hasFlag(insn, 0x00000010) ? MRC : CDP;
        }
    };

    private static final ARMInstruction DP_Thumb = new STUB() {
		@Override
		public ARMInstruction instance(int insn) {
			return dp_Thumb[(insn >> 6) & 0xF];
		}
    };

    private static final ARMInstruction dp_Thumb[] = {
    		AND_Thumb,
    		EOR_Thumb,
    		LSL_Thumb,
    		LSR_Thumb,
    		ASR_Thumb,
    		ADC_Thumb,
    		SBC_Thumb,
    		ROR_Thumb,
    		TST_Thumb,
    		NEG_Thumb,
    		CMP_Thumb,
    		CMN_Thumb,
    		ORR_Thumb,
    		MUL_Thumb,
    		BIC_Thumb,
    		MVN_Thumb
    };

    private static final ARMInstruction table10[] = {
    		MRS, // 0x0
    		UNK, // 0x1
    		UNK, // 0x2
    		UNK, // 0x3
    		UNK, // 0x4
    		UNK, // 0x5
    		UNK, // 0x6
    		UNK, // 0x7
    		UNK, // 0x8
    		UNK, // 0x9
    		UNK, // 0xA
    		UNK, // 0xB
    		UNK, // 0xC
    		UNK, // 0xD
    		UNK, // 0xE
    		UNK  // 0xF
    };

    private static final ARMInstruction table12[] = {
    		MSR, // 0x0
    		BX, // 0x1
    		UNK, // 0x2
    		UNK, // 0x3
    		UNK, // 0x4
    		UNK, // 0x5
    		UNK, // 0x6
    		BKPT, // 0x7
    		UNK, // 0x8
    		UNK, // 0x9
    		UNK, // 0xA
    		UNK, // 0xB
    		UNK, // 0xC
    		UNK, // 0xD
    		UNK, // 0xE
    		UNK  // 0xF
    };

    private static final ARMInstruction table14[] = {
    		MRS, // 0x0
    		UNK, // 0x1
    		UNK, // 0x2
    		UNK, // 0x3
    		UNK, // 0x4
    		UNK, // 0x5
    		UNK, // 0x6
    		UNK, // 0x7
    		UNK, // 0x8
    		UNK, // 0x9
    		UNK, // 0xA
    		UNK, // 0xB
    		UNK, // 0xC
    		UNK, // 0xD
    		UNK, // 0xE
    		UNK  // 0xF
    };

    private static final ARMInstruction table16[] = {
    		MSR, // 0x0
    		UNK, // 0x1
    		UNK, // 0x2
    		UNK, // 0x3
    		UNK, // 0x4
    		UNK, // 0x5
    		UNK, // 0x6
    		UNK, // 0x7
    		UNK, // 0x8
    		UNK, // 0x9
    		UNK, // 0xA
    		UNK, // 0xB
    		UNK, // 0xC
    		UNK, // 0xD
    		UNK, // 0xE
    		UNK  // 0xF
    };

	private static final ARMInstruction[] table_opcode = {
			AND, // 0x00
			AND, // 0x01
			EOR, // 0x02
			EOR, // 0x03
			SUB, // 0x04
			SUB, // 0x05
			RSB, // 0x06
			RSB, // 0x07
			ADD, // 0x08
			ADD, // 0x09
			ADC, // 0x0A
			ADC, // 0x0B
			SBC, // 0x0C
			SBC, // 0x0D
			RSC, // 0x0E
			RSC, // 0x0F
			new STUB() {
				@Override
				public ARMInstruction instance(int insn) {
					return table10[(insn >> 4) & 0xF];
				}
			}, // 0x10
			TST, // 0x11
			new STUB() {
				@Override
				public ARMInstruction instance(int insn) {
					return table12[(insn >> 4) & 0xF];
				}
			}, // 0x12
			TEQ, // 0x13
			new STUB() {
				@Override
				public ARMInstruction instance(int insn) {
					return table14[(insn >> 4) & 0xF];
				}
			}, // 0x14
			CMP, // 0x15
			new STUB() {
				@Override
				public ARMInstruction instance(int insn) {
					return table16[(insn >> 4) & 0xF];
				}
			}, // 0x16
			CMN, // 0x17
			ORR, // 0x18
			ORR, // 0x19
			MOV, // 0x1A
			MOV, // 0x1B
			BIC, // 0x1C
			BIC, // 0x1D
			MVN, // 0x1E
			MVN, // 0x1F
			AND, // 0x20
			AND, // 0x21
			EOR, // 0x22
			EOR, // 0x23
			SUB, // 0x24
			SUB, // 0x25
			RSB, // 0x26
			RSB, // 0x27
			ADD, // 0x28
			ADD, // 0x29
			ADC, // 0x2A
			ADC, // 0x2B
			SBC, // 0x2C
			SBC, // 0x2D
			RSC, // 0x2E
			RSC, // 0x2F
			UNK, // 0x30
			TST, // 0x31
			MSR, // 0x32
			TEQ, // 0x33
			UNK, // 0x34
			CMP, // 0x35
			MSR, // 0x36
			CMN, // 0x37
			ORR, // 0x38
			ORR, // 0x39
			MOV, // 0x3A
			MOV, // 0x3B
			BIC, // 0x3C
			BIC, // 0x3D
			MVN, // 0x3E
			UNK, // 0x3F
			STR, // 0x40
			LDR, // 0x41
			STR, // 0x42
			LDR, // 0x43
			STR, // 0x44
			LDR, // 0x45
			STR, // 0x46
			LDR, // 0x47
			STR, // 0x48
			LDR, // 0x49
			STR, // 0x4A
			LDR, // 0x4B
			STR, // 0x4C
			LDR, // 0x4D
			STR, // 0x4E
			LDR, // 0x4F
			STR, // 0x50
			LDR, // 0x51
			STR, // 0x52
			LDR, // 0x53
			STR, // 0x54
			LDR, // 0x55
			STR, // 0x56
			LDR, // 0x57
			STR, // 0x58
			LDR, // 0x59
			STR, // 0x5A
			LDR, // 0x5B
			STR, // 0x5C
			LDR, // 0x5D
			STR, // 0x5E
			LDR, // 0x5F
			STR, // 0x60
			LDR, // 0x61
			STR, // 0x62
			LDR, // 0x63
			STR, // 0x64
			LDR, // 0x65
			STR, // 0x66
			LDR, // 0x67
			STR, // 0x68
			LDR, // 0x69
			STR, // 0x6A
			LDR, // 0x6B
			STR, // 0x6C
			LDR, // 0x6D
			STR, // 0x6E
			LDR, // 0x6F
			STR, // 0x70
			LDR, // 0x71
			STR, // 0x72
			LDR, // 0x73
			STR, // 0x74
			LDR, // 0x75
			STR, // 0x76
			LDR, // 0x77
			STR, // 0x78
			LDR, // 0x79
			STR, // 0x7A
			LDR, // 0x7B
			STR, // 0x7C
			LDR, // 0x7D
			STR, // 0x7E
			LDR, // 0x7F
			STM, // 0x80
			LDM, // 0x81
			STM, // 0x82
			LDM, // 0x83
			STM, // 0x84
			LDM, // 0x85
			STM, // 0x86
			LDM, // 0x87
			STM, // 0x88
			LDM, // 0x89
			STM, // 0x8A
			LDM, // 0x8B
			STM, // 0x8C
			LDM, // 0x8D
			STM, // 0x8E
			LDM, // 0x8F
			STM, // 0x90
			LDM, // 0x91
			STM, // 0x92
			LDM, // 0x93
			STM, // 0x94
			LDM, // 0x95
			STM, // 0x96
			LDM, // 0x97
			STM, // 0x98
			LDM, // 0x99
			STM, // 0x9A
			LDM, // 0x9B
			STM, // 0x9C
			LDM, // 0x9D
			STM, // 0x9E
			LDM, // 0x9F
			B, // 0xA0
			B, // 0xA1
			B, // 0xA2
			B, // 0xA3
			B, // 0xA4
			B, // 0xA5
			B, // 0xA6
			B, // 0xA7
			B, // 0xA8
			B, // 0xA9
			B, // 0xAA
			B, // 0xAB
			B, // 0xAC
			B, // 0xAD
			B, // 0xAE
			B, // 0xAF
			BL, // 0xB0
			BL, // 0xB1
			BL, // 0xB2
			BL, // 0xB3
			BL, // 0xB4
			BL, // 0xB5
			BL, // 0xB6
			BL, // 0xB7
			BL, // 0xB8
			BL, // 0xB9
			BL, // 0xBA
			BL, // 0xBB
			BL, // 0xBC
			BL, // 0xBD
			BL, // 0xBE
			BL, // 0xBF
			UNK, // 0xC0
			UNK, // 0xC1
			UNK, // 0xC2
			UNK, // 0xC3
			UNK, // 0xC4
			UNK, // 0xC5
			UNK, // 0xC6
			UNK, // 0xC7
			UNK, // 0xC8
			UNK, // 0xC9
			UNK, // 0xCA
			UNK, // 0xCB
			UNK, // 0xCC
			UNK, // 0xCD
			UNK, // 0xCE
			UNK, // 0xCF
			UNK, // 0xD0
			UNK, // 0xD1
			UNK, // 0xD2
			UNK, // 0xD3
			UNK, // 0xD4
			UNK, // 0xD5
			UNK, // 0xD6
			UNK, // 0xD7
			UNK, // 0xD8
			UNK, // 0xD9
			UNK, // 0xDA
			UNK, // 0xDB
			UNK, // 0xDC
			UNK, // 0xDD
			UNK, // 0xDE
			UNK, // 0xDF
			MCR_CDP, // 0xE0
			MRC_CDP, // 0xE1
			MCR_CDP, // 0xE2
			MRC_CDP, // 0xE3
			MCR_CDP, // 0xE4
			MRC_CDP, // 0xE5
			MCR_CDP, // 0xE6
			MRC_CDP, // 0xE7
			MCR_CDP, // 0xE8
			MRC_CDP, // 0xE9
			MCR_CDP, // 0xEA
			MRC_CDP, // 0xEB
			MCR_CDP, // 0xEC
			MRC_CDP, // 0xED
			MCR_CDP, // 0xEE
			MRC_CDP, // 0xEF
			SWI, // 0xF0
			SWI, // 0xF1
			SWI, // 0xF2
			SWI, // 0xF3
			SWI, // 0xF4
			SWI, // 0xF5
			SWI, // 0xF6
			SWI, // 0xF7
			SWI, // 0xF8
			SWI, // 0xF9
			SWI, // 0xFA
			SWI, // 0xFB
			SWI, // 0xFC
			SWI, // 0xFD
			SWI, // 0xFE
			SWI  // 0xFF
	};

	private static final ARMInstruction[] table_opcode_unconditional = {
			UNK, // 0x00
			UNK, // 0x01
			UNK, // 0x02
			UNK, // 0x03
			UNK, // 0x04
			UNK, // 0x05
			UNK, // 0x06
			UNK, // 0x07
			UNK, // 0x08
			UNK, // 0x09
			UNK, // 0x0A
			UNK, // 0x0B
			UNK, // 0x0C
			UNK, // 0x0D
			UNK, // 0x0E
			UNK, // 0x0F
			UNK, // 0x10
			UNK, // 0x11
			UNK, // 0x12
			UNK, // 0x13
			UNK, // 0x14
			UNK, // 0x15
			UNK, // 0x16
			UNK, // 0x17
			UNK, // 0x18
			UNK, // 0x19
			UNK, // 0x1A
			UNK, // 0x1B
			UNK, // 0x1C
			UNK, // 0x1D
			UNK, // 0x1E
			UNK, // 0x1F
			UNK, // 0x20
			UNK, // 0x21
			UNK, // 0x22
			UNK, // 0x23
			UNK, // 0x24
			UNK, // 0x25
			UNK, // 0x26
			UNK, // 0x27
			UNK, // 0x28
			UNK, // 0x29
			UNK, // 0x2A
			UNK, // 0x2B
			UNK, // 0x2C
			UNK, // 0x2D
			UNK, // 0x2E
			UNK, // 0x2F
			UNK, // 0x30
			UNK, // 0x31
			UNK, // 0x32
			UNK, // 0x33
			UNK, // 0x34
			UNK, // 0x35
			UNK, // 0x36
			UNK, // 0x37
			UNK, // 0x38
			UNK, // 0x39
			UNK, // 0x3A
			UNK, // 0x3B
			UNK, // 0x3C
			UNK, // 0x3D
			UNK, // 0x3E
			UNK, // 0x3F
			UNK, // 0x40
			UNK, // 0x41
			UNK, // 0x42
			UNK, // 0x43
			UNK, // 0x44
			UNK, // 0x45
			UNK, // 0x46
			UNK, // 0x47
			UNK, // 0x48
			UNK, // 0x49
			UNK, // 0x4A
			UNK, // 0x4B
			UNK, // 0x4C
			UNK, // 0x4D
			UNK, // 0x4E
			UNK, // 0x4F
			UNK, // 0x50
			UNK, // 0x51
			UNK, // 0x52
			UNK, // 0x53
			UNK, // 0x54
			UNK, // 0x55
			UNK, // 0x56
			UNK, // 0x57
			UNK, // 0x58
			UNK, // 0x59
			UNK, // 0x5A
			UNK, // 0x5B
			UNK, // 0x5C
			UNK, // 0x5D
			UNK, // 0x5E
			UNK, // 0x5F
			UNK, // 0x60
			UNK, // 0x61
			UNK, // 0x62
			UNK, // 0x63
			UNK, // 0x64
			UNK, // 0x65
			UNK, // 0x66
			UNK, // 0x67
			UNK, // 0x68
			UNK, // 0x69
			UNK, // 0x6A
			UNK, // 0x6B
			UNK, // 0x6C
			UNK, // 0x6D
			UNK, // 0x6E
			UNK, // 0x6F
			UNK, // 0x70
			UNK, // 0x71
			UNK, // 0x72
			UNK, // 0x73
			UNK, // 0x74
			UNK, // 0x75
			UNK, // 0x76
			UNK, // 0x77
			UNK, // 0x78
			UNK, // 0x79
			UNK, // 0x7A
			UNK, // 0x7B
			UNK, // 0x7C
			UNK, // 0x7D
			UNK, // 0x7E
			UNK, // 0x7F
			UNK, // 0x80
			UNK, // 0x81
			UNK, // 0x82
			UNK, // 0x83
			UNK, // 0x84
			UNK, // 0x85
			UNK, // 0x86
			UNK, // 0x87
			UNK, // 0x88
			UNK, // 0x89
			UNK, // 0x8A
			UNK, // 0x8B
			UNK, // 0x8C
			UNK, // 0x8D
			UNK, // 0x8E
			UNK, // 0x8F
			UNK, // 0x90
			UNK, // 0x91
			UNK, // 0x92
			UNK, // 0x93
			UNK, // 0x94
			UNK, // 0x95
			UNK, // 0x96
			UNK, // 0x97
			UNK, // 0x98
			UNK, // 0x99
			UNK, // 0x9A
			UNK, // 0x9B
			UNK, // 0x9C
			UNK, // 0x9D
			UNK, // 0x9E
			UNK, // 0x9F
			BLX_uncond, // 0xA0
			BLX_uncond, // 0xA1
			BLX_uncond, // 0xA2
			BLX_uncond, // 0xA3
			BLX_uncond, // 0xA4
			BLX_uncond, // 0xA5
			BLX_uncond, // 0xA6
			BLX_uncond, // 0xA7
			BLX_uncond, // 0xA8
			BLX_uncond, // 0xA9
			BLX_uncond, // 0xAA
			BLX_uncond, // 0xAB
			BLX_uncond, // 0xAC
			BLX_uncond, // 0xAD
			BLX_uncond, // 0xAE
			BLX_uncond, // 0xAF
			BLX_uncond, // 0xB0
			BLX_uncond, // 0xB1
			BLX_uncond, // 0xB2
			BLX_uncond, // 0xB3
			BLX_uncond, // 0xB4
			BLX_uncond, // 0xB5
			BLX_uncond, // 0xB6
			BLX_uncond, // 0xB7
			BLX_uncond, // 0xB8
			BLX_uncond, // 0xB9
			BLX_uncond, // 0xBA
			BLX_uncond, // 0xBB
			BLX_uncond, // 0xBC
			BLX_uncond, // 0xBD
			BLX_uncond, // 0xBE
			BLX_uncond, // 0xBF
			UNK, // 0xC0
			UNK, // 0xC1
			UNK, // 0xC2
			UNK, // 0xC3
			UNK, // 0xC4
			UNK, // 0xC5
			UNK, // 0xC6
			UNK, // 0xC7
			UNK, // 0xC8
			UNK, // 0xC9
			UNK, // 0xCA
			UNK, // 0xCB
			UNK, // 0xCC
			UNK, // 0xCD
			UNK, // 0xCE
			UNK, // 0xCF
			UNK, // 0xD0
			UNK, // 0xD1
			UNK, // 0xD2
			UNK, // 0xD3
			UNK, // 0xD4
			UNK, // 0xD5
			UNK, // 0xD6
			UNK, // 0xD7
			UNK, // 0xD8
			UNK, // 0xD9
			UNK, // 0xDA
			UNK, // 0xDB
			UNK, // 0xDC
			UNK, // 0xDD
			UNK, // 0xDE
			UNK, // 0xDF
			UNK, // 0xE0
			UNK, // 0xE1
			UNK, // 0xE2
			UNK, // 0xE3
			UNK, // 0xE4
			UNK, // 0xE5
			UNK, // 0xE6
			UNK, // 0xE7
			UNK, // 0xE8
			UNK, // 0xE9
			UNK, // 0xEA
			UNK, // 0xEB
			UNK, // 0xEC
			UNK, // 0xED
			UNK, // 0xEE
			UNK, // 0xEF
			UNK, // 0xF0
			UNK, // 0xF1
			UNK, // 0xF2
			UNK, // 0xF3
			UNK, // 0xF4
			UNK, // 0xF5
			UNK, // 0xF6
			UNK, // 0xF7
			UNK, // 0xF8
			UNK, // 0xF9
			UNK, // 0xFA
			UNK, // 0xFB
			UNK, // 0xFC
			UNK, // 0xFD
			UNK, // 0xFE
			UNK  // 0xFF
	};

	public static ARMInstruction instruction(int insn32) {
		// Unconditional instruction?
		if ((insn32 >> 28) == -1) {
			return table_opcode_unconditional[(insn32 >> 20) & 0xFF].instance(insn32);
		}
		return table_opcode[(insn32 >> 20) & 0xFF].instance(insn32);
	}

	private static final ARMInstruction[] table_thumb = {
			LSL_Imm_Thumb, // 0x00
			LSL_Imm_Thumb, // 0x01
			LSL_Imm_Thumb, // 0x02
			LSL_Imm_Thumb, // 0x03
			LSL_Imm_Thumb, // 0x04
			LSL_Imm_Thumb, // 0x05
			LSL_Imm_Thumb, // 0x06
			LSL_Imm_Thumb, // 0x07
			LSR_Imm_Thumb, // 0x08
			LSR_Imm_Thumb, // 0x09
			LSR_Imm_Thumb, // 0x0A
			LSR_Imm_Thumb, // 0x0B
			LSR_Imm_Thumb, // 0x0C
			LSR_Imm_Thumb, // 0x0D
			LSR_Imm_Thumb, // 0x0E
			LSR_Imm_Thumb, // 0x0F
			ASR_Imm_Thumb, // 0x10
			ASR_Imm_Thumb, // 0x11
			ASR_Imm_Thumb, // 0x12
			ASR_Imm_Thumb, // 0x13
			ASR_Imm_Thumb, // 0x14
			ASR_Imm_Thumb, // 0x15
			ASR_Imm_Thumb, // 0x16
			ASR_Imm_Thumb, // 0x17
			ADD_Reg_Thumb, // 0x18
			ADD_Reg_Thumb, // 0x19
			SUB_Reg_Thumb, // 0x1A
			SUB_Reg_Thumb, // 0x1B
			ADD_Reg_Imm_Thumb, // 0x1C
			ADD_Reg_Imm_Thumb, // 0x1D
			SUB_Reg_Imm_Thumb, // 0x1E
			SUB_Reg_Imm_Thumb, // 0x1F
			MOV_Immediate_Thumb, // 0x20
			MOV_Immediate_Thumb, // 0x21
			MOV_Immediate_Thumb, // 0x22
			MOV_Immediate_Thumb, // 0x23
			MOV_Immediate_Thumb, // 0x24
			MOV_Immediate_Thumb, // 0x25
			MOV_Immediate_Thumb, // 0x26
			MOV_Immediate_Thumb, // 0x27
			CMP_Imm_Thumb, // 0x28
			CMP_Imm_Thumb, // 0x29
			CMP_Imm_Thumb, // 0x2A
			CMP_Imm_Thumb, // 0x2B
			CMP_Imm_Thumb, // 0x2C
			CMP_Imm_Thumb, // 0x2D
			CMP_Imm_Thumb, // 0x2E
			CMP_Imm_Thumb, // 0x2F
			ADD_Imm_Thumb, // 0x30
			ADD_Imm_Thumb, // 0x31
			ADD_Imm_Thumb, // 0x32
			ADD_Imm_Thumb, // 0x33
			ADD_Imm_Thumb, // 0x34
			ADD_Imm_Thumb, // 0x35
			ADD_Imm_Thumb, // 0x36
			ADD_Imm_Thumb, // 0x37
			SUB_Imm_Thumb, // 0x38
			SUB_Imm_Thumb, // 0x39
			SUB_Imm_Thumb, // 0x3A
			SUB_Imm_Thumb, // 0x3B
			SUB_Imm_Thumb, // 0x3C
			SUB_Imm_Thumb, // 0x3D
			SUB_Imm_Thumb, // 0x3E
			SUB_Imm_Thumb, // 0x3F
			DP_Thumb, // 0x40
			DP_Thumb, // 0x41
			DP_Thumb, // 0x42
			DP_Thumb, // 0x43
			ADD_High_Thumb, // 0x44
			CMP_High_Thumb, // 0x45
			MOV_High_Thumb, // 0x46
			new STUB() {
				@Override
				public ARMInstruction instance(int insn) {
					return hasFlag(insn, 0x0080) ? BLX_Thumb : BX_Thumb;
				}
			}, // 0x47
			LDR_Pc_Thumb, // 0x48
			LDR_Pc_Thumb, // 0x49
			LDR_Pc_Thumb, // 0x4A
			LDR_Pc_Thumb, // 0x4B
			LDR_Pc_Thumb, // 0x4C
			LDR_Pc_Thumb, // 0x4D
			LDR_Pc_Thumb, // 0x4E
			LDR_Pc_Thumb, // 0x4F
			STR_Reg_Thumb, // 0x50
			STR_Reg_Thumb, // 0x51
			STRH_Reg_Thumb, // 0x52
			STRH_Reg_Thumb, // 0x53
			STRB_Reg_Thumb, // 0x54
			STRB_Reg_Thumb, // 0x55
			LDRSB_Thumb, // 0x56
			LDRSB_Thumb, // 0x57
			LDR_Reg_Thumb, // 0x58
			LDR_Reg_Thumb, // 0x59
			LDRH_Reg_Thumb, // 0x5A
			LDRH_Reg_Thumb, // 0x5B
			LDRB_Reg_Thumb, // 0x5C
			LDRB_Reg_Thumb, // 0x5D
			LDRSH_Thumb, // 0x5E
			LDRSH_Thumb, // 0x5F
			STR_Imm_Thumb, // 0x60
			STR_Imm_Thumb, // 0x61
			STR_Imm_Thumb, // 0x62
			STR_Imm_Thumb, // 0x63
			STR_Imm_Thumb, // 0x64
			STR_Imm_Thumb, // 0x65
			STR_Imm_Thumb, // 0x66
			STR_Imm_Thumb, // 0x67
			LDR_Imm_Thumb, // 0x68
			LDR_Imm_Thumb, // 0x69
			LDR_Imm_Thumb, // 0x6A
			LDR_Imm_Thumb, // 0x6B
			LDR_Imm_Thumb, // 0x6C
			LDR_Imm_Thumb, // 0x6D
			LDR_Imm_Thumb, // 0x6E
			LDR_Imm_Thumb, // 0x6F
			STRB_Imm_Thumb, // 0x70
			STRB_Imm_Thumb, // 0x71
			STRB_Imm_Thumb, // 0x72
			STRB_Imm_Thumb, // 0x73
			STRB_Imm_Thumb, // 0x74
			STRB_Imm_Thumb, // 0x75
			STRB_Imm_Thumb, // 0x76
			STRB_Imm_Thumb, // 0x77
			LDRB_Imm_Thumb, // 0x78
			LDRB_Imm_Thumb, // 0x79
			LDRB_Imm_Thumb, // 0x7A
			LDRB_Imm_Thumb, // 0x7B
			LDRB_Imm_Thumb, // 0x7C
			LDRB_Imm_Thumb, // 0x7D
			LDRB_Imm_Thumb, // 0x7E
			LDRB_Imm_Thumb, // 0x7F
			STRH_Imm_Thumb, // 0x80
			STRH_Imm_Thumb, // 0x81
			STRH_Imm_Thumb, // 0x82
			STRH_Imm_Thumb, // 0x83
			STRH_Imm_Thumb, // 0x84
			STRH_Imm_Thumb, // 0x85
			STRH_Imm_Thumb, // 0x86
			STRH_Imm_Thumb, // 0x87
			LDRH_Imm_Thumb, // 0x88
			LDRH_Imm_Thumb, // 0x89
			LDRH_Imm_Thumb, // 0x8A
			LDRH_Imm_Thumb, // 0x8B
			LDRH_Imm_Thumb, // 0x8C
			LDRH_Imm_Thumb, // 0x8D
			LDRH_Imm_Thumb, // 0x8E
			LDRH_Imm_Thumb, // 0x8F
			STR_Stack_Thumb, // 0x90
			STR_Stack_Thumb, // 0x91
			STR_Stack_Thumb, // 0x92
			STR_Stack_Thumb, // 0x93
			STR_Stack_Thumb, // 0x94
			STR_Stack_Thumb, // 0x95
			STR_Stack_Thumb, // 0x96
			STR_Stack_Thumb, // 0x97
			LDR_Stack_Thumb, // 0x98
			LDR_Stack_Thumb, // 0x99
			LDR_Stack_Thumb, // 0x9A
			LDR_Stack_Thumb, // 0x9B
			LDR_Stack_Thumb, // 0x9C
			LDR_Stack_Thumb, // 0x9D
			LDR_Stack_Thumb, // 0x9E
			LDR_Stack_Thumb, // 0x9F
			ADD_Rd_Pc_Thumb, // 0xA0
			ADD_Rd_Pc_Thumb, // 0xA1
			ADD_Rd_Pc_Thumb, // 0xA2
			ADD_Rd_Pc_Thumb, // 0xA3
			ADD_Rd_Pc_Thumb, // 0xA4
			ADD_Rd_Pc_Thumb, // 0xA5
			ADD_Rd_Pc_Thumb, // 0xA6
			ADD_Rd_Pc_Thumb, // 0xA7
			ADD_Rd_Sp_Thumb, // 0xA8
			ADD_Rd_Sp_Thumb, // 0xA9
			ADD_Rd_Sp_Thumb, // 0xAA
			ADD_Rd_Sp_Thumb, // 0xAB
			ADD_Rd_Sp_Thumb, // 0xAC
			ADD_Rd_Sp_Thumb, // 0xAD
			ADD_Rd_Sp_Thumb, // 0xAE
			ADD_Rd_Sp_Thumb, // 0xAF
			new STUB() {
				@Override
				public ARMInstruction instance(int insn) {
					return hasFlag(insn, 0x0080) ? SUB_Sp_Thumb : ADD_Sp_Thumb;
				}
			}, // 0xB0
			UNK_Thumb, // 0xB1
			UNK_Thumb, // 0xB2
			UNK_Thumb, // 0xB3
			PUSH_Thumb, // 0xB4
			PUSH_Thumb, // 0xB5
			UNK_Thumb, // 0xB6
			UNK_Thumb, // 0xB7
			UNK_Thumb, // 0xB8
			UNK_Thumb, // 0xB9
			UNK_Thumb, // 0xBA
			UNK_Thumb, // 0xBB
			POP_Thumb, // 0xBC
			POP_Thumb, // 0xBD
			BKPT_Thumb, // 0xBE
			UNK_Thumb, // 0xBF
			STM_Thumb, // 0xC0
			STM_Thumb, // 0xC1
			STM_Thumb, // 0xC2
			STM_Thumb, // 0xC3
			STM_Thumb, // 0xC4
			STM_Thumb, // 0xC5
			STM_Thumb, // 0xC6
			STM_Thumb, // 0xC7
			LDM_Thumb, // 0xC8
			LDM_Thumb, // 0xC9
			LDM_Thumb, // 0xCA
			LDM_Thumb, // 0xCB
			LDM_Thumb, // 0xCC
			LDM_Thumb, // 0xCD
			LDM_Thumb, // 0xCE
			LDM_Thumb, // 0xCF
			B_Cond_Thumb, // 0xD0
			B_Cond_Thumb, // 0xD1
			B_Cond_Thumb, // 0xD2
			B_Cond_Thumb, // 0xD3
			B_Cond_Thumb, // 0xD4
			B_Cond_Thumb, // 0xD5
			B_Cond_Thumb, // 0xD6
			B_Cond_Thumb, // 0xD7
			B_Cond_Thumb, // 0xD8
			B_Cond_Thumb, // 0xD9
			B_Cond_Thumb, // 0xDA
			B_Cond_Thumb, // 0xDB
			B_Cond_Thumb, // 0xDC
			B_Cond_Thumb, // 0xDD
			UNK_Thumb, // 0xDE
			SWI_Thumb, // 0xDF
			B_Thumb, // 0xE0
			B_Thumb, // 0xE1
			B_Thumb, // 0xE2
			B_Thumb, // 0xE3
			B_Thumb, // 0xE4
			B_Thumb, // 0xE5
			B_Thumb, // 0xE6
			B_Thumb, // 0xE7
			BLX_01_Thumb, // 0xE8
			BLX_01_Thumb, // 0xE9
			BLX_01_Thumb, // 0xEA
			BLX_01_Thumb, // 0xEB
			BLX_01_Thumb, // 0xEC
			BLX_01_Thumb, // 0xED
			BLX_01_Thumb, // 0xEE
			BLX_01_Thumb, // 0xEF
			BL_10_Thumb, // 0xF0
			BL_10_Thumb, // 0xF1
			BL_10_Thumb, // 0xF2
			BL_10_Thumb, // 0xF3
			BL_10_Thumb, // 0xF4
			BL_10_Thumb, // 0xF5
			BL_10_Thumb, // 0xF6
			BL_10_Thumb, // 0xF7
			BL_11_Thumb, // 0xF8
			BL_11_Thumb, // 0xF9
			BL_11_Thumb, // 0xFA
			BL_11_Thumb, // 0xFB
			BL_11_Thumb, // 0xFC
			BL_11_Thumb, // 0xFD
			BL_11_Thumb, // 0xFE
			BL_11_Thumb  // 0xFF
	};

	public static ARMInstruction thumbInstruction(int insn16) {
		return table_thumb[(insn16 >> 8) & 0xFF].instance(insn16);
	}
}
