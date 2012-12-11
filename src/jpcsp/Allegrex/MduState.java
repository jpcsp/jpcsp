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
 * Multiply Divide Unit, handles accumulators.
 *
 * @author hli
 */
public class MduState extends GprState {

    public long hilo;

    public void setHi(int value) {
        hilo = (hilo & 0xffffffffL) | (((long) value) << 32);
    }

    public int getHi() {
        return (int) (hilo >>> 32);
    }

    public void setLo(int value) {
        hilo = (hilo & ~0xffffffffL) | ((value) & 0xffffffffL);
    }

    public int getLo() {
        return (int) (hilo & 0xffffffffL);
    }

    @Override
    public void reset() {
        hilo = 0;
    }

    @Override
    public void resetAll() {
        super.resetAll();
        hilo = 0;
    }

    public MduState() {
        hilo = 0;
    }

    public void copy(MduState that) {
        super.copy(that);
        hilo = that.hilo;
    }

    public MduState(MduState that) {
        super(that);
        hilo = that.hilo;
    }

    public static final long signedDivMod(int x, int y) {
        return (((long) (x % y)) << 32) | (((x / y)) & 0xffffffffL);
    }

    public static final long unsignedDivMod(long x, long y) {
        return ((x % y) << 32) | ((x / y) & 0xffffffffL);
    }

    public final void doMFHI(int rd) {
        if (rd != 0) {
            setRegister(rd, getHi());
        }
    }

    public final void doMTHI(int rs) {
        int hi = getRegister(rs);
        hilo = (((long) hi) << 32) | (hilo & 0xffffffffL);
    }

    public final void doMFLO(int rd) {
        if (rd != 0) {
            setRegister(rd, getLo());
        }
    }

    public final void doMTLO(int rs) {
        int lo = getRegister(rs);
        hilo = (hilo & 0xffffffff00000000L) | ((lo) & 0x00000000ffffffffL);
    }

    public final void doMULT(int rs, int rt) {
        hilo = ((long) getRegister(rs)) * ((long) getRegister(rt));
    }

    public final void doMULTU(int rs, int rt) {
        hilo = (getRegister(rs) & 0xffffffffL) * (getRegister(rt) & 0xffffffffL);
    }

    public final void doDIV(int rs, int rt) {
    	int rsValue = getRegister(rs);
    	int rtValue = getRegister(rt);
        if (rtValue == 0) {
            // According to MIPS spec., result is unpredictable when dividing by zero.
            // However on a PSP, hi is set to $rs register value and lo is set to 0x0000FFFF/0xFFFFFFFF.
            // This has been tested on a real PSP using vfputest.pbp.
            long lo = rsValue > 0xFFFF ? 0xFFFFFFFFL : 0x0000FFFFL;
            hilo = (((long) rsValue) << 32) | lo;
        } else {
            int lo = rsValue / rtValue;
            int hi = rsValue % rtValue;
            hilo = (((long) hi) << 32) | ((lo) & 0xffffffffL);
        }
    }

    public final void doDIVU(int rs, int rt) {
    	int rsValue = getRegister(rs);
    	int rtValue = getRegister(rt);
        if (rtValue == 0) {
            // According to MIPS spec., result is unpredictable when dividing by zero.
            // However on a PSP, hi is set to $rs register value and lo is set to 0x0000FFFF/0xFFFFFFFF.
            // This has been tested on a real PSP using vfputest.pbp.
            long lo = rsValue > 0xFFFF ? 0xFFFFFFFFL : 0x0000FFFFL;
            hilo = (((long) rsValue) << 32) | lo;
        } else {
            long x = rsValue & 0xFFFFFFFFL;
            long y = rtValue & 0xFFFFFFFFL;
            hilo = ((x % y) << 32) | ((x / y) & 0xFFFFFFFFL);
        }
    }

    public final void doMADD(int rs, int rt) {
        hilo += ((long) getRegister(rs)) * ((long) getRegister(rt));
    }

    public final void doMADDU(int rs, int rt) {
        hilo += (getRegister(rs) & 0xffffffffL) * (getRegister(rt) & 0xffffffffL);
    }

    public final void doMSUB(int rs, int rt) {
        hilo -= ((long) getRegister(rs)) * ((long) getRegister(rt));
    }

    public final void doMSUBU(int rs, int rt) {
        hilo -= (getRegister(rs) & 0xffffffffL) * (getRegister(rt) & 0xffffffffL);
    }
}