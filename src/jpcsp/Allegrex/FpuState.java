/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

/**
 *
 * @author hli
 */
public class FpuState extends HiLoState {

    public static final class Fcr0 {

        public static final int imp = 0; /* FPU design number */

        public static final int rev = 0; /* FPU revision bumber */

    }

    public class Fcr31 {

        public int rm;
        public boolean c;
        public boolean fs;

        public void reset() {
            rm = 0;
            c = false;
            fs = false;
        }

        public Fcr31() {
            reset();
        }

        public Fcr31(Fcr31 that) {
            rm = that.rm;
            c = that.c;
            fs = that.fs;
        }
    }
    public float[] fpr;
    public Fcr31 fcr31;

    public void reset() {
        super.reset();
        fpr = new float[32];
        fcr31 = new Fcr31();
    }

    public FpuState() {
        reset();
    }

    public void copy(FpuState that) {
        super.copy(that);
        fpr = new float[32];
        for (int reg = 0; reg < 32; ++reg) {
            fpr[reg] = that.fpr[reg];
        }
        fcr31 = new Fcr31(that.fcr31);
    }

    public FpuState(FpuState that) {
        copy(that);
    }
}
