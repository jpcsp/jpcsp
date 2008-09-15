/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.Allegrex;

/**
 *
 * @author hli
 */
public class GprState {
    public int[] gpr;

    public void reset() {
        gpr = new int[32];
    }

    public GprState() {
        reset();
    }

    public void copy(GprState that) {
        gpr = new int[32];
        for (int reg = 0; reg < 32; ++reg) {
            gpr[reg] = that.gpr[reg];
        }
    }

    public GprState(GprState that) {
        that.copy(that);
    }
}
