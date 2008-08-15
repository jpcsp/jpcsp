/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp;

/**
 *
 * @author hli
 */
public interface AllegrexInstructions {
      
    public void doUNK(String reason);

    public void doNOP();

    public void doSLL(int rd, int rt, int sa);

    public void doSRL(int rd, int rt, int sa);

    public void doSRA(int rd, int rt, int sa);

    public void doSLLV(int rd, int rt, int rs);

    public void doSRLV(int rd, int rt, int rs);
    
    public void doSRAV(int rd, int rt, int rs);

    public void doJR(int rs);

    public void doJALR(int rd, int rs);
    
    public void doMFHI(int rd);

    public void doMTHI(int rs);

    public void doMFLO(int rd);
    
    public void doMTLO(int rs);

    public void doMULT(int rs, int rt);

    public void doMULTU(int rs, int rt);

    public void doDIV(int rs, int rt);
    
    public void doDIVU(int rs, int rt);
    
    public void doADD(int rd, int rs, int rt);

    public void doADDU(int rd, int rs, int rt);
    
    public void doSUB(int rd, int rs, int rt);

    public void doSUBU(int rd, int rs, int rt);
    
    public void doAND(int rd, int rs, int rt);

    public void doOR(int rd, int rs, int rt);

    public void doXOR(int rd, int rs, int rt);

    public void doNOR(int rd, int rs, int rt);

    public void doSLT(int rd, int rs, int rt);

    public void doSLTU(int rd, int rs, int rt);

    public void doBLTZ(int rs, int simm16);
    
    public void doBGEZ(int rs, int simm16);
    
    public void doBLTZL(int rs, int simm16);
    
    public void doBGEZL(int rs, int simm16);
    
    public void doBLTZAL(int rs, int simm16);
    
    public void doBGEZAL(int rs, int simm16);
    
    public void doBLTZALL(int rs, int simm16);
    
    public void doBGEZALL(int rs, int simm16);
    
    public void doJ(int uimm26);
    
    public void doJAL(int uimm26);

    public void doBEQ(int rs, int rt, int simm16);
    
    public void doBNE(int rs, int rt, int simm16);
    
    public void doBLEZ(int rs, int simm16);
    
    public void doBGTZ(int rs, int simm16);
    
    public void doBEQL(int rs, int rt, int simm16);
    
    public void doBNEL(int rs, int rt, int simm16);
    
    public void doBLEZL(int rs, int simm16);

    public void doBGTZL(int rs, int simm16);

    public void doADDI(int rt, int rs, int simm16);
    
    public void doADDIU(int rt, int rs, int simm16);
    
    public void doSLTI(int rt, int rs, int simm16);
    
    public void doSLTIU(int rt, int rs, int simm16);
    
    public void doANDI(int rt, int rs, int uimm16);
    
    public void doORI(int rt, int rs, int uimm16);
    
    public void doXORI(int rt, int rs, int uimm16);
    
    public void doLUI(int rt, int uimm16);

    public void doHALT();
    
    public void doMFIC(int rt);

    public void doMTIC(int rt);

    public void doMFC0(int rt, int c0dr);
    
    public void doCFC0(int rt, int c0cr);
    
    public void doMTC0(int rt, int c0dr);
    
    public void doCTC0(int rt, int c0cr);

    public void doERET();

    public void doLB(int rt, int rs, int simm16);

    public void doLBU(int rt, int rs, int simm16);

    public void doLH(int rt, int rs, int simm16);

    public void doLHU(int rt, int rs, int simm16);

    public void doLWL(int rt, int rs, int simm16);

    public void doLW(int rt, int rs, int simm16);

    public void doLWR(int rt, int rs, int simm16);

    public void doSB(int rt, int rs, int simm16);

    public void doSH(int rt, int rs, int simm16);

    public void doSWL(int rt, int rs, int simm16);

    public void doSW(int rt, int rs, int simm16);

    public void doSWR(int rt, int rs, int simm16);

    public void doCACHE(int rt, int rs, int simm16);

    public void doLL(int rt, int rs, int simm16);

    public void doLWC1(int rt, int rs, int simm16);

    public void doSC(int rt, int rs, int simm16);
    
    public void doSWC1(int rt, int rs, int simm16);
    
    public void doROTR(int rd, int rt, int sa);
    
    public void doROTRV(int rd, int rt, int rs);
    
    public void doMOVZ(int rd, int rs, int rt);
    
    public void doMOVN(int rd, int rs, int rt);
    
    public void doSYSCALL(int code);
    
    public void doBREAK(int code);
    
    public void doSYNC();

    public void doCLZ(int rd, int rs);
   
    public void doCLO(int rd, int rs);
   
    public void doMADD(int rs, int rt);
   
    public void doMADDU(int rs, int rt);
   
    public void doMAX(int rd, int rs, int rt);

    public void doMIN(int rd, int rs, int rt);
    
    public void doMSUB(int rs, int rt);
    
    public void doMSUBU(int rs, int rt);
    
    public void doEXT(int rt, int rs, int rd, int sa);

    public void doINS(int rt, int rs, int rd, int sa);

    public void doWSBH(int rd, int rt);
    
    public void doWSBW(int rd, int rt);

    public void doSEB(int rd, int rt);
    
    public void doBITREV(int rd, int rt);
    
    public void doSEH(int rd, int rt);
    
    //COP1 instructions
    public void doMFC1(int rt, int c1dr);
    
    public void doCFC1(int rt, int c1cr);
    
    public void doMTC1(int rt, int c1dr);    
    
    public void doCTC1(int rt, int c1cr);
    
    public void doBC1F(int simm16);
    
    public void doBC1T (int simm16);
    
    public void doBC1FL(int simm16);
    
    public void doBC1TL(int simm16);
    
    public void doADDS(int fd , int fs ,int ft);
    
    public void doSUBS(int fd , int fs ,int ft); 
    
    public void doMULS(int fd , int fs ,int ft); 
    
    public void doDIVS(int fd , int fs ,int ft); 
    
    public void doSQRTS(int fd,int fs);
    
    public void doABSS(int fd,int fs);
    
    public void doMOVS(int fd,int fs);
    
    public void doNEGS(int fd,int fs);
    
    public void doROUNDWS(int fd,int fs);
    
    public void doTRUNCWS(int fd,int fs);
    
    public void doCEILWS(int fd,int fs);
    
    public void doFLOORWS(int fd,int fs);
    
    public void doCVTSW(int fd,int fs);
    
    public void doCVTWS(int fd,int fs);
    
    public void doCF(int fs,int ft);
    
    public void doCUN(int fs,int ft);
    
    public void doCEQ(int fs,int ft);
    
    public void doCUEQ(int fs,int ft);
    
    public void doCOLT(int fs,int ft);
    
    public void doCULT(int fs,int ft);
    
    public void doCOLE(int fs,int ft);
    
    public void doCULE(int fs,int ft);
    
    public void doCSF(int fs,int ft);
    
    public void doCNGLE(int fs,int ft);
    
    public void doCSEQ(int fs,int ft);
    
    public void doCNGL(int fs,int ft);
    
    public void doCLT(int fs,int ft);
    
    public void doCNGE(int fs,int ft);
    
    public void doCLE(int fs,int ft);
    
    public void doCNGT(int fs,int ft);
}
