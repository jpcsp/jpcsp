/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.Allegrex;

/**
 *
 * @author hli
 */
public class HiLoState extends GprState {

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

    public void reset() {
        super.reset();
        hilo = 0;
    }

    public HiLoState() {
        reset();
    }

    public void copy(HiLoState that) {
        super.copy(that);
        hilo = that.hilo;
    }

    public HiLoState(HiLoState that) {
        copy(that);
    }
}
