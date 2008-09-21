/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.Allegrex;

/**
 *
 * @author hli
 */
public class MduState extends GprState {

    public long hilo;

    public void setHi(int value) {
        hilo = (hilo & 0xffffffffL) | ((long) value << 32);
    }

    public int getHi() {
        return (int) (hilo >>> 32);
    }

    public void setLo(int value) {
        hilo = (hilo & ~0xffffffffL) | (((long) value) & 0xffffffffL);
    }

    public int getLo() {
        return (int) (hilo & 0xffffffffL);
    }

    @Override
    public void reset() {
        super.reset();
        hilo = 0;
    }

    public MduState() {
        reset();
    }

    public void copy(MduState that) {
        super.copy(that);
        hilo = that.hilo;
    }

    public MduState(MduState that) {
        copy(that);
    }
    
    public static final long signedDivMod(int x, int y) {
        return ((long) (x % y)) << 32 | (((long) (x / y)) & 0xffffffff);
    }

    public static final long unsignedDivMod(long x, long y) {
        return ((x % y)) << 32 | ((x / y) & 0xffffffff);
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

    public final void doMADD(int rs, int rt) {
        hilo += ((long) gpr[rs]) * ((long) gpr[rt]);
    }

    public final void doMADDU(int rs, int rt) {
        hilo += (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
    }

    public final void doMSUB(int rs, int rt) {
        hilo -= ((long) gpr[rs]) * ((long) gpr[rt]);
    }

    public final void doMSUBU(int rs, int rt) {
        hilo -= (((long) gpr[rs]) & 0xffffffff) * (((long) gpr[rt]) & 0xffffffff);
    }
    
}
