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

/**
 *
 * @author hli
 */
public interface Operations {

    public void opUNK(String reason);

    public void opNOP();

    public void opSLL(int rd, int rt, int sa);

    public void opSRL(int rd, int rt, int sa);

    public void opSRA(int rd, int rt, int sa);

    public void opSLLV(int rd, int rt, int rs);

    public void opSRLV(int rd, int rt, int rs);

    public void opSRAV(int rd, int rt, int rs);

    public void opJR(int rs);

    public void opJALR(int rd, int rs);

    public void opMFHI(int rd);

    public void opMTHI(int rs);

    public void opMFLO(int rd);

    public void opMTLO(int rs);

    public void opMULT(int rs, int rt);

    public void opMULTU(int rs, int rt);

    public void opDIV(int rs, int rt);

    public void opDIVU(int rs, int rt);

    public void opADD(int rd, int rs, int rt);

    public void opADDU(int rd, int rs, int rt);

    public void opSUB(int rd, int rs, int rt);

    public void opSUBU(int rd, int rs, int rt);

    public void opAND(int rd, int rs, int rt);

    public void opOR(int rd, int rs, int rt);

    public void opXOR(int rd, int rs, int rt);

    public void opNOR(int rd, int rs, int rt);

    public void opSLT(int rd, int rs, int rt);

    public void opSLTU(int rd, int rs, int rt);

    public void opBLTZ(int rs, int simm16);

    public void opBGEZ(int rs, int simm16);

    public void opBLTZL(int rs, int simm16);

    public void opBGEZL(int rs, int simm16);

    public void opBLTZAL(int rs, int simm16);

    public void opBGEZAL(int rs, int simm16);

    public void opBLTZALL(int rs, int simm16);

    public void opBGEZALL(int rs, int simm16);

    public void opJ(int uimm26);

    public void opJAL(int uimm26);

    public void opBEQ(int rs, int rt, int simm16);

    public void opBNE(int rs, int rt, int simm16);

    public void opBLEZ(int rs, int simm16);

    public void opBGTZ(int rs, int simm16);

    public void opBEQL(int rs, int rt, int simm16);

    public void opBNEL(int rs, int rt, int simm16);

    public void opBLEZL(int rs, int simm16);

    public void opBGTZL(int rs, int simm16);

    public void opADDI(int rt, int rs, int simm16);

    public void opADDIU(int rt, int rs, int simm16);

    public void opSLTI(int rt, int rs, int simm16);

    public void opSLTIU(int rt, int rs, int simm16);

    public void opANDI(int rt, int rs, int uimm16);

    public void opORI(int rt, int rs, int uimm16);

    public void opXORI(int rt, int rs, int uimm16);

    public void opLUI(int rt, int uimm16);

    public void opHALT();

    public void opMFIC(int rt);

    public void opMTIC(int rt);

    public void opMFC0(int rt, int c0dr);

    public void opCFC0(int rt, int c0cr);

    public void opMTC0(int rt, int c0dr);

    public void opCTC0(int rt, int c0cr);

    public void opERET();

    public void opLB(int rt, int rs, int simm16);

    public void opLBU(int rt, int rs, int simm16);

    public void opLH(int rt, int rs, int simm16);

    public void opLHU(int rt, int rs, int simm16);

    public void opLWL(int rt, int rs, int simm16);

    public void opLW(int rt, int rs, int simm16);

    public void opLWR(int rt, int rs, int simm16);

    public void opSB(int rt, int rs, int simm16);

    public void opSH(int rt, int rs, int simm16);

    public void opSWL(int rt, int rs, int simm16);

    public void opSW(int rt, int rs, int simm16);

    public void opSWR(int rt, int rs, int simm16);

    public void opCACHE(int rt, int rs, int simm16);

    public void opLL(int rt, int rs, int simm16);

    public void opLWC1(int rt, int rs, int simm16);

    public void opLVS(int vt, int rs, int simm14);

    public void opSC(int rt, int rs, int simm16);

    public void opSWC1(int rt, int rs, int simm16);

    public void opSVS(int vt, int rs, int simm14);

    public void opROTR(int rd, int rt, int sa);

    public void opROTRV(int rd, int rt, int rs);

    public void opMOVZ(int rd, int rs, int rt);

    public void opMOVN(int rd, int rs, int rt);

    public void opSYSCALL(int code);

    public void opBREAK(int code);

    public void opSYNC();

    public void opCLZ(int rd, int rs);

    public void opCLO(int rd, int rs);

    public void opMADD(int rs, int rt);

    public void opMADDU(int rs, int rt);

    public void opMAX(int rd, int rs, int rt);

    public void opMIN(int rd, int rs, int rt);

    public void opMSUB(int rs, int rt);

    public void opMSUBU(int rs, int rt);

    public void opEXT(int rt, int rs, int rd, int sa);

    public void opINS(int rt, int rs, int rd, int sa);

    public void opWSBH(int rd, int rt);

    public void opWSBW(int rd, int rt);

    public void opSEB(int rd, int rt);

    public void opBITREV(int rd, int rt);

    public void opSEH(int rd, int rt);
    //COP1 instructions
    public void opMFC1(int rt, int c1dr);

    public void opCFC1(int rt, int c1cr);

    public void opMTC1(int rt, int c1dr);

    public void opCTC1(int rt, int c1cr);

    public void opBC1F(int simm16);

    public void opBC1T(int simm16);

    public void opBC1FL(int simm16);

    public void opBC1TL(int simm16);

    public void opADDS(int fd, int fs, int ft);

    public void opSUBS(int fd, int fs, int ft);

    public void opMULS(int fd, int fs, int ft);

    public void opDIVS(int fd, int fs, int ft);

    public void opSQRTS(int fd, int fs);

    public void opABSS(int fd, int fs);

    public void opMOVS(int fd, int fs);

    public void opNEGS(int fd, int fs);

    public void opROUNDWS(int fd, int fs);

    public void opTRUNCWS(int fd, int fs);

    public void opCEILWS(int fd, int fs);

    public void opFLOORWS(int fd, int fs);

    public void opCVTSW(int fd, int fs);

    public void opCVTWS(int fd, int fs);

    public void opCCONDS(int fs, int ft, int cond);

    // VFPU0
    public void opVADD(int vsize, int vd, int vs, int vt);

    public void opVSUB(int vsize, int vd, int vs, int vt);

    public void opVSBN(int vsize, int vd, int vs, int vt);

    public void opVDIV(int vsize, int vd, int vs, int vt);

    // VFPU1
    public void opVMUL(int vsize, int vd, int vs, int vt);

    public void opVDOT(int vsize, int vd, int vs, int vt);

    public void opVSCL(int vsize, int vd, int vs, int vt);

    public void opVHDP(int vsize, int vd, int vs, int vt);

    public void opVCRS(int vsize, int vd, int vs, int vt);

    public void opVDET(int vsize, int vd, int vs, int vt);

    // VFPU3
    public void opVCMP(int vsize, int vs, int vt, int cond);

    public void opVMIN(int vsize, int vd, int vs, int vt);

    public void opVMAX(int vsize, int vd, int vs, int vt);

    public void opVSCMP(int vsize, int vd, int vs, int vt);

    public void opVSGE(int vsize, int vd, int vs, int vt);

    public void opVSLT(int vsize, int vd, int vs, int vt);

    // VFPU5
    public void opVPFXS(int imm24);

    public void opVPFXT(int imm24);

    public void opVPFXD(int imm24);

    public void opVIIM(int vs, int imm16);

    public void opVFIM(int vs, int imm16);
}