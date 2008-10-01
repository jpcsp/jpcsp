/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

import jpcsp.Emulator;
import java.util.Arrays;

/**
 * General Purpose Registers, handles integer operations like ALU, shifter, etc.
 *
 * @author hli
 */
public class GprState {

    public int[] gpr;

    public void reset() {
        Arrays.fill(gpr, 0);
    }

    public void resetAll() {
        Arrays.fill(gpr, 0);
    }

    public GprState() {
        gpr = new int[32];
    }

    public void copy(GprState that) {
        gpr = that.gpr.clone();
    }

    public GprState(GprState that) {
        gpr = that.gpr.clone();
    }

    public void doUNK(String reason) {
        Emulator.log.error("Interpreter : " + reason);
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

    public static final int unsignedCompare(long i, long j) {
        return (int)(((i & 0xFFFFFFFFL) - (j & 0xFFFFFFFFL)) >>> 63);
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
            gpr[rt] = gpr[rs] + (int) (short) simm16;
        }
    }

    public final void doSLTI(int rt, int rs, int simm16) {
        if (rt != 0) {
            gpr[rt] = signedCompare(gpr[rs], (int) (short) simm16);
        }
    }

    public final void doSLTIU(int rt, int rs, int simm16) {
        if (rt != 0) {
            gpr[rt] = unsignedCompare(gpr[rs], (int) (short) simm16);
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

    public final void doEXT(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
            gpr[rt] = extractBits(gpr[rs], sa, (rd + 1)); 
        }
    }

    public final void doINS(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
            gpr[rt] = insertBits(gpr[rt], gpr[rs], sa, rd);
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
        if (rd != 0) {
            gpr[rd] = (gpr[rt] << 16) >> 16;
        }
    }
}
