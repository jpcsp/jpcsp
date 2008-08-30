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

import gnu.bytecode.*;

/**
 * @author Andrius
 */
public class Recompiler implements AllegrexInstructions {
    private Processor processor;
    private AllegrexDecoder decoder;
    
    public Recompiler() {
        processor = Emulator.getProcessor();
        decoder = new AllegrexDecoder();
    }
    
    public void run() {
    }
    
    @Override
    public void doUNK(String reason) {
        System.out.println("Recompiler - " + reason);
    }
    
    @Override
    public void doNOP() {
        
    }
    
    @Override
    public void doSLL(int rd, int rt, int sa) {
        
    }

    @Override
    public void doSRL(int rd, int rt, int sa) {
        
    }

    @Override
    public void doSRA(int rd, int rt, int sa) {
        
    }

    @Override
    public void doSLLV(int rd, int rt, int rs) {
        
    }

    @Override
    public void doSRLV(int rd, int rt, int rs) {
        
    }

    @Override
    public void doSRAV(int rd, int rt, int rs) {
        
    }

    @Override
    public void doJR(int rs) {
        
    }

    @Override
    public void doJALR(int rd, int rs) {
        
    }

    @Override
    public void doMFHI(int rd) {
        
    }

    @Override
    public void doMTHI(int rs) {
        
    }

    @Override
    public void doMFLO(int rd) {
        
    }

    @Override
    public void doMTLO(int rs) {
        
    }

    @Override
    public void doMULT(int rs, int rt) {
        
    }

    @Override
    public void doMULTU(int rs, int rt) {
        
    }

    @Override
    public void doDIV(int rs, int rt) {
        
    }

    @Override
    public void doDIVU(int rs, int rt) {
        
    }

    @Override
    public void doADD(int rd, int rs, int rt) {
        
    }

    @Override
    public void doADDU(int rd, int rs, int rt) {
        
    }

    @Override
    public void doSUB(int rd, int rs, int rt) {
        
    }

    @Override
    public void doSUBU(int rd, int rs, int rt) {
        
    }

    @Override
    public void doAND(int rd, int rs, int rt) {
        
    }

    @Override
    public void doOR(int rd, int rs, int rt) {
        
    }

    @Override
    public void doXOR(int rd, int rs, int rt) {
        
    }

    @Override
    public void doNOR(int rd, int rs, int rt) {
        
    }

    @Override
    public void doSLT(int rd, int rs, int rt) {
        
    }

    @Override
    public void doSLTU(int rd, int rs, int rt) {
        
    }

    @Override
    public void doBLTZ(int rs, int simm16) {
        
    }

    @Override
    public void doBGEZ(int rs, int simm16) {
        
    }

    @Override
    public void doBLTZL(int rs, int simm16) {
        
    }

    @Override
    public void doBGEZL(int rs, int simm16) {
        
    }

    @Override
    public void doBLTZAL(int rs, int simm16) {
        
    }

    @Override
    public void doBGEZAL(int rs, int simm16) {
        
    }

    @Override
    public void doBLTZALL(int rs, int simm16) {
        
    }

    @Override
    public void doBGEZALL(int rs, int simm16) {
        
    }

    @Override
    public void doJ(int uimm26) {
        
    }

    @Override
    public void doJAL(int uimm26) {
        
    }

    @Override
    public void doBEQ(int rs, int rt, int simm16) {
        
    }

    @Override
    public void doBNE(int rs, int rt, int simm16) {
        
    }

    @Override
    public void doBLEZ(int rs, int simm16) {
        
    }

    @Override
    public void doBGTZ(int rs, int simm16) {
        
    }

    @Override
    public void doBEQL(int rs, int rt, int simm16) {
        
    }

    @Override
    public void doBNEL(int rs, int rt, int simm16) {
        
    }

    @Override
    public void doBLEZL(int rs, int simm16) {
        
    }

    @Override
    public void doBGTZL(int rs, int simm16) {
        
    }

    @Override
    public void doADDI(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doADDIU(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSLTI(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSLTIU(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doANDI(int rt, int rs, int uimm16) {
        
    }

    @Override
    public void doORI(int rt, int rs, int uimm16) {
        
    }

    @Override
    public void doXORI(int rt, int rs, int uimm16) {
        
    }

    @Override
    public void doLUI(int rt, int uimm16) {
        
    }

    @Override
    public void doHALT() {
        
    }

    @Override
    public void doMFIC(int rt) {
        
    }

    @Override
    public void doMTIC(int rt) {
        
    }

    @Override
    public void doMFC0(int rt, int c0dr) {
        
    }

    @Override
    public void doCFC0(int rt, int c0cr) {
        
    }

    @Override
    public void doMTC0(int rt, int c0dr) {
        
    }

    @Override
    public void doCTC0(int rt, int c0cr) {
        
    }

    @Override
    public void doERET() {
        
    }

    @Override
    public void doLB(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doLBU(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doLH(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doLHU(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doLWL(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doLW(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doLWR(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSB(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSH(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSWL(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSW(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSWR(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doCACHE(int code, int rs, int simm16) {
        
    }

    @Override
    public void doLL(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doLWC1(int ft, int rs, int simm16) {
        
    }

    @Override
    public void doLVS(int vt, int rs, int simm14) {
        
    }

    @Override
    public void doSC(int rt, int rs, int simm16) {
        
    }

    @Override
    public void doSWC1(int ft, int rs, int simm16) {
        
    }

    @Override
    public void doSVS(int vt, int rs, int simm14) {
        
    }

    @Override
    public void doROTR(int rd, int rt, int sa) {
        
    }

    @Override
    public void doROTRV(int rd, int rt, int rs) {
        
    }

    @Override
    public void doMOVZ(int rd, int rs, int rt) {
        
    }

    @Override
    public void doMOVN(int rd, int rs, int rt) {
        
    }

    @Override
    public void doSYSCALL(int code) {
        
    }

    @Override
    public void doBREAK(int code) {
        
    }

    @Override
    public void doSYNC() {
        
    }

    @Override
    public void doCLZ(int rd, int rs) {
        
    }

    @Override
    public void doCLO(int rd, int rs) {
        
    }

    @Override
    public void doMADD(int rs, int rt) {
        
    }

    @Override
    public void doMADDU(int rs, int rt) {
        
    }

    @Override
    public void doMAX(int rd, int rs, int rt) {
        
    }

    @Override
    public void doMIN(int rd, int rs, int rt) {
        
    }

    @Override
    public void doMSUB(int rs, int rt) {
        
    }

    @Override
    public void doMSUBU(int rs, int rt) {
        
    }

    @Override
    public void doEXT(int rt, int rs, int rd, int sa) {
        
    }

    @Override
    public void doINS(int rt, int rs, int rd, int sa) {
        
    }

    @Override
    public void doWSBH(int rd, int rt) {
        
    }

    @Override
    public void doWSBW(int rd, int rt) {
        
    }

    @Override
    public void doSEB(int rd, int rt) {
        
    }

    @Override
    public void doBITREV(int rd, int rt) {
        
    }

    @Override
    public void doSEH(int rd, int rt) {
        
    }

    @Override
    public void doMFC1(int rt, int c1dr) {
        
    }

    @Override
    public void doCFC1(int rt, int c1cr) {
        
    }

    @Override
    public void doMTC1(int rt, int c1dr) {
        
    }

    @Override
    public void doCTC1(int rt, int c1cr) {
        
    }

    @Override
    public void doBC1F(int simm16) {
        
    }

    @Override
    public void doBC1T(int simm16) {
        
    }

    @Override
    public void doBC1FL(int simm16) {
        
    }

    @Override
    public void doBC1TL(int simm16) {
        
    }

    @Override
    public void doADDS(int fd, int fs, int ft) {
        
    }

    @Override
    public void doSUBS(int fd, int fs, int ft) {
        
    }

    @Override
    public void doMULS(int fd, int fs, int ft) {
        
    }

    @Override
    public void doDIVS(int fd, int fs, int ft) {
        
    }

    @Override
    public void doSQRTS(int fd, int fs) {
        
    }

    @Override
    public void doABSS(int fd, int fs) {
        
    }

    @Override
    public void doMOVS(int fd, int fs) {
        
    }

    @Override
    public void doNEGS(int fd, int fs) {
        
    }

    @Override
    public void doROUNDWS(int fd, int fs) {
        
    }

    @Override
    public void doTRUNCWS(int fd, int fs) {
        
    }

    @Override
    public void doCEILWS(int fd, int fs) {
        
    }

    @Override
    public void doFLOORWS(int fd, int fs) {
        
    }

    @Override
    public void doCVTSW(int fd, int fs) {
        
    }

    @Override
    public void doCVTWS(int fd, int fs) {
        
    }

    @Override
    public void doCCONDS(int fs, int ft, int cond) {
        
    }

    @Override
    public void doVADD(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVSUB(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVSBN(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVDIV(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVMUL(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVDOT(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVSCL(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVHDP(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVCRS(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVDET(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVCMP(int vsize, int vs, int vt, int cond) {
        
    }

    @Override
    public void doVMIN(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVMAX(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVSCMP(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVSGE(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVSLT(int vsize, int vd, int vs, int vt) {
        
    }

    @Override
    public void doVPFXS(int imm24) {
        
    }

    @Override
    public void doVPFXT(int imm24) {
        
    }

    @Override
    public void doVPFXD(int imm24) {
        
    }

    @Override
    public void doVIIM(int vs, int imm16) {

    }

    @Override
    public void doVFIM(int vs, int imm16) {

    }
}
