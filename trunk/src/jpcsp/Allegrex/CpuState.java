/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

/**
 *
 * @author hli
 */

public class CpuState extends VfpuState {

    public void reset() {
        super.reset();
    }

    public CpuState() {
        reset();
    }

    public void copy(CpuState that) {
        super.copy(that);
    }
    
    public CpuState(CpuState that) {
        copy(that);
    }
    
    public static final long signedDivMod(int x, int y) {
        return ((long) (x % y)) << 32 | (((long) (x / y)) & 0xffffffff);
    }

    public static final long unsignedDivMod(long x, long y) {
        return ((x % y)) << 32 | ((x / y) & 0xffffffff);
    }

    public static final int max(int x, int y) {
        return (x > y) ? x : y;
    }

    public static final int min(int x, int y) {
        return (x < y) ? x : y;
    }

    public static final int extractBits(int x, int pos, int len) {
        return (x >>> pos) & ~(~0 << len);
    }

    public static final int insertBits(int x, int y, int lsb, int msb) {
        int mask = ~(~0 << (msb - lsb + 1)) << lsb;
        return (x & ~mask) | ((y << lsb) & mask);
    }

    public static final int signExtend(int value) {
        return (value << 16) >> 16;
    }

    public static final int signExtend8(int value) {
        return (value << 24) >> 24;
    }

    public static final int zeroExtend(int value) {
        return (value & 0xffff);
    }

    public static final int zeroExtend8(int value) {
        return (value & 0xff);
    }

    public static final int signedCompare(int i, int j) {
        return (i - j) >>> 31;
    }

    public static final int unsignedCompare(int i, int j) {
        return ((i - j) ^ i ^ j) >>> 31;
    }

    public static final int branchTarget(int npc, int simm16) {
        return npc + (simm16 << 2);
    }

    public static final int jumpTarget(int npc, int uimm26) {
        return (npc & 0xf0000000) | (uimm26 << 2);
    }

    public final void doSLL(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] << sa);
        }
    }

    public final void doSRL(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] >>> sa);
        }
    }

    public final void doSRA(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] >> sa);
        }
    }

    public final void doSLLV(int rd, int rt, int rs) {
        if (rd != 0) {
                gpr[rd] = (gpr[rt] << (gpr[rs] & 31));
        }
    }

    public final void doSRLV(int rd, int rt, int rs) {
        if (rd != 0) {
                gpr[rd] = (gpr[rt] >>> (gpr[rs] & 31));
        }
    }

    public final void doSRAV(int rd, int rt, int rs) {
        if (rd != 0) {
                gpr[rd] = (gpr[rt] >> (gpr[rs] & 31));
        }
    }

    public final void doMFHI(int rd) {
        if (rd != 0) {
            gpr[rd] = getHi();
        }
    }

    public final void doMTHI(int rs) {
        int hi = gpr[rs];
        hilo = (((long) hi) << 32) | (hilo & 0xffffffff);
    }

    public final void doMFLO(int rd) {
        if (rd != 0) {
            gpr[rd] = getLo();
        }
    }

    public final void doMTLO(int rs) {
        int lo = gpr[rs];
        hilo = (hilo & 0xffffffff00000000L) | (((long) lo) & 0x00000000ffffffffL);
    }

    public final void doMULT(int rs, int rt) {
        hilo = ((long) gpr[rs]) * ((long) gpr[rt]);
    }

    public final void doMULTU(int rs, int rt) {
        hilo = (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
    }

    public final void doDIV(int rs, int rt) {
        int lo = gpr[rs] / gpr[rt];
        int hi = gpr[rs] % gpr[rt];
        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
    }

    public final void doDIVU(int rs, int rt) {
        long x = ((long) gpr[rs]) & 0xffffffff;
        long y = ((long) gpr[rt]) & 0xffffffff;
        int lo = (int) (x / y);
        int hi = (int) (x % y);
        hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);
    }

    public final void doADDU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] + gpr[rt];
        }
    }

    public final void doSUBU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] - gpr[rt];
        }
    }

    public final void doAND(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] & gpr[rt];
        }
    }

    public final void doOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] | gpr[rt];
        }
    }

    public final void doXOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = gpr[rs] ^ gpr[rt];
        }
    }

    public final void doNOR(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = ~(gpr[rs] | gpr[rt]);
        }
    }

    public final void doSLT(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = signedCompare(gpr[rs], gpr[rt]);
        }
    }

    public final void doSLTU(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = unsignedCompare(gpr[rs], gpr[rt]);
        }
    }


    public final void doADDIU(int rt, int rs, int simm16) {
        if (rt != 0) {
                gpr[rt] = gpr[rs] + simm16;
        }
    }

    public final void doSLTI(int rt, int rs, int simm16) {
        if (rt != 0) {
                gpr[rt] = signedCompare(gpr[rs], simm16);
        }
    }

    public final void doSLTIU(int rt, int rs, int simm16) {
        if (rt != 0) {
                gpr[rt] = unsignedCompare(gpr[rs], simm16);
        }
    }

    public final void doANDI(int rt, int rs, int uimm16) {
        if (rt != 0) {
                gpr[rt] = gpr[rs] & uimm16;
        }
    }

    public final void doORI(int rt, int rs, int uimm16) {
        if (rt != 0) {
                gpr[rt] = gpr[rs] | uimm16;
        }
    }

    public final void doXORI(int rt, int rs, int uimm16) {
        if (rt != 0) {
                gpr[rt] = gpr[rs] ^ uimm16;
        }
    }

    public final void doLUI(int rt, int uimm16) {
        if (rt != 0) {
                gpr[rt] = uimm16 << 16;
        }
    }

    public final void doROTR(int rd, int rt, int sa) {
        if (rd != 0) {
            gpr[rd] = Integer.rotateRight(gpr[rt], sa);
        }
    }

    public final void doROTRV(int rd, int rt, int rs) {
        if (rd != 0) {
            // no need of "gpr[rs] & 31", rotateRight does it for us
            gpr[rd] = Integer.rotateRight(gpr[rt], gpr[rs]);
        }
    }

    public final void doMOVZ(int rd, int rs, int rt) {
        if ((rd != 0) && (gpr[rt] == 0)) {
            gpr[rd] = gpr[rs];
        }
    }

    public final void doMOVN(int rd, int rs, int rt) {
        if ((rd != 0) && (gpr[rt] != 0)) {
            gpr[rd] = gpr[rs];
        }
    }

    public final void doCLZ(int rd, int rs) {
        if (rd != 0) {
            gpr[rd] = Integer.numberOfLeadingZeros(gpr[rs]);
        }
    }

    public final void doCLO(int rd, int rs) {
        if (rd != 0) {
            gpr[rd] = Integer.numberOfLeadingZeros(~gpr[rs]);
        }
    }

    public final void doMADD(int rs, int rt) {
        hilo += ((long) gpr[rs]) * ((long) gpr[rt]);
    }

    public final void doMADDU(int rs, int rt) {
        hilo += (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
    }

    public final void doMAX(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = Math.max(gpr[rs], gpr[rt]);
        }
    }

    public final void doMIN(int rd, int rs, int rt) {
        if (rd != 0) {
            gpr[rd] = Math.min(gpr[rs], gpr[rt]);
        }
    }

    public final void doMSUB(int rs, int rt) {
        hilo -= ((long) gpr[rs]) * ((long) gpr[rt]);
    }

    public final void doMSUBU(int rs, int rt) {
        hilo -= (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
    }

    public final void doEXT(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
            int mask = ~(~0 << (rd + 1));
            gpr[rt] = (gpr[rs] >>> sa) & mask;
        }
    }

    public final void doINS(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
            int mask = ~(~0 << (rd - sa + 1)) << sa;
            gpr[rt] = (gpr[rt] & ~mask) | ((gpr[rs] << sa) & mask);
        }
    }

    public final void doWSBH(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = Integer.rotateRight(Integer.reverseBytes(gpr[rt]), 16);
        }
    }

    public final void doWSBW(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = Integer.reverseBytes(gpr[rt]);
        }
    }

    public final void doSEB(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = (gpr[rt] << 24) >> 24;
        }
    }

    public final void doBITREV(int rd, int rt) {
        if (rd != 0) {
            gpr[rd] = Integer.reverse(gpr[rt]);
        }
    }

    public final void doSEH(int rd, int rt) {
        gpr[rd] = (gpr[rt] << 16) >> 16;
    }  
}
