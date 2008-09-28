/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

/**
 * Vectorial Floating Point Unit, handles scalar, vector and matrix operations.
 *
 * @author hli
 */
public class VfpuState extends FpuState {

    public float[][][] vpr;

    public class Vcr {

        public class PfxSrc /* $128, $129 */ {

            public int[] swz;
            public boolean[] abs;
            public boolean[] cst;
            public boolean[] neg;
            public boolean enabled;

            public void reset() {
                swz = new int[4];
                abs = new boolean[4];
                cst = new boolean[4];
                neg = new boolean[4];
                enabled = false;
            }

            public PfxSrc() {
                reset();
            }

            public PfxSrc(PfxSrc that) {
                swz = new int[4];
                abs = new boolean[4];
                cst = new boolean[4];
                neg = new boolean[4];
                for (int i = 0; i < 4; ++i) {
                    swz[i] = that.swz[i];
                    abs[i] = that.abs[i];
                    cst[i] = that.cst[i];
                    neg[i] = that.neg[i];
                    enabled = that.enabled;
                }
            }
        }
        public PfxSrc pfxs;
        public PfxSrc pfxt;

        public class PfxDst /* 130 */ {

            public int[] sat;
            public boolean[] msk;
            public boolean enabled;

            public void reset() {
                sat = new int[4];
                msk = new boolean[4];
                enabled = false;
            }

            public PfxDst() {
                reset();
            }

            public PfxDst(PfxDst that) {
                sat = new int[4];
                msk = new boolean[4];
                for (int i = 0; i < 4; ++i) {
                    sat[i] = that.sat[i];
                    msk[i] = that.msk[i];
                    enabled = that.enabled;
                }
            }
        }
        public PfxDst pfxd;
        public boolean[] /* 131 */ cc;

        public void reset() {
            pfxs = new PfxSrc();
            pfxt = new PfxSrc();
            pfxd = new PfxDst();
            cc = new boolean[6];
        }

        public Vcr() {
            reset();
        }

        public Vcr(Vcr that) {
            pfxs = new PfxSrc(that.pfxs);
            pfxt = new PfxSrc(that.pfxt);
            pfxd = new PfxDst(that.pfxd);
            cc = new boolean[6];
            for (int i = 0; i < 6; ++i) {
                cc[i] = that.cc[i];
            }
        }
    }
    public Vcr vcr;

    @Override
    public void reset() {
        super.reset();
        vpr = new float[8][4][4]; // [matrix][column][row]
        vcr = new Vcr();
    }

    public VfpuState() {
        reset();
    }

    public void copy(VfpuState that) {
        super.copy(that);
        vpr = new float[8][4][4]; // [matrix][column][row]
        for (int m = 0; m < 8; ++m) {
            for (int c = 0; c < 4; ++c) {
                for (int r = 0; r < 4; ++r) {
                    vpr[m][c][r] = that.vpr[m][c][r];
                }
            }
        }
        vcr = new Vcr(that.vcr);
    }

    public VfpuState(VfpuState that) {
        copy(that);
    }

    // VFPU stuff
    private float transformVr(int swz, boolean abs, boolean cst, boolean neg, float[] x) {
        float value = 0.0f;
        if (cst) {
            switch (swz) {
                case 0:
                    value = abs ? 0.0f : 3.0f;
                    break;
                case 1:
                    value = abs ? 1.0f : (1.0f / 3.0f);
                    break;
                case 2:
                    value = abs ? 2.0f : (1.0f / 4.0f);
                    break;
                case 3:
                    value = abs ? 0.5f : (1.0f / 6.0f);
                    break;
            }
        } else {
            value = x[swz];
        }

        if (abs) {
            value = Math.abs(value);
        }
        return neg ? (0.0f - value) : value;
    }

    private float applyPrefixVs(int i, float[] x) {
        return transformVr(vcr.pfxs.swz[i], vcr.pfxs.abs[i], vcr.pfxs.cst[i], vcr.pfxs.neg[i], x);
    }

    private float applyPrefixVt(int i, float[] x) {
        return transformVr(vcr.pfxt.swz[i], vcr.pfxt.abs[i], vcr.pfxt.cst[i], vcr.pfxt.neg[i], x);
    }

    private float applyPrefixVd(int i, float value) {
        switch (vcr.pfxd.sat[i]) {
            case 1:
                return Math.max(0.0f, Math.min(1.0f, value));
            case 3:
                return Math.max(-1.0f, Math.min(1.0f, value));
        }
        return value;
    }

    public float[] loadVs(int vsize, int vs) {
        float[] result = new float[vsize];

        int m, r, c;

        m = (vs >> 2) & 7;
        c = (vs >> 0) & 3;

        switch (vsize) {
            case 1:
                r = (vs >> 5) & 3;
                result[0] = vpr[m][c][r];
                if (vcr.pfxs.enabled) {
                    result[0] = applyPrefixVs(0, result);
                    vcr.pfxs.enabled = false;
                }
                return result;

            case 2:
                r = (vs & 64) >> 5;
                if ((vs & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                }
                if (vcr.pfxs.enabled) {
                    result[0] = applyPrefixVs(0, result);
                    result[1] = applyPrefixVs(1, result);
                    vcr.pfxs.enabled = false;
                }
                return result;

            case 3:
                r = (vs & 64) >> 6;
                if ((vs & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                    result[2] = vpr[m][r + 2][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                    result[2] = vpr[m][c][r + 2];
                }
                if (vcr.pfxs.enabled) {
                    result[0] = applyPrefixVs(0, result);
                    result[1] = applyPrefixVs(1, result);
                    result[2] = applyPrefixVs(2, result);
                    vcr.pfxs.enabled = false;
                }
                return result;

            case 4:
                if ((vs & 32) != 0) {
                    result[0] = vpr[m][0][c];
                    result[1] = vpr[m][1][c];
                    result[2] = vpr[m][2][c];
                    result[3] = vpr[m][3][c];
                } else {
                    result[0] = vpr[m][c][0];
                    result[1] = vpr[m][c][1];
                    result[2] = vpr[m][c][2];
                    result[3] = vpr[m][c][3];
                }
                if (vcr.pfxs.enabled) {
                    result[0] = applyPrefixVs(0, result);
                    result[1] = applyPrefixVs(1, result);
                    result[2] = applyPrefixVs(2, result);
                    result[3] = applyPrefixVs(3, result);
                    vcr.pfxs.enabled = false;
                }
                return result;

            default:
        }
        return null;
    }

    public float[] loadVt(int vsize, int vt) {
        float[] result = new float[vsize];

        int m, r, c;

        m = (vt >> 2) & 7;
        c = (vt >> 0) & 3;

        switch (vsize) {
            case 1:
                r = (vt >> 5) & 3;
                result[0] = vpr[m][c][r];
                if (vcr.pfxt.enabled) {
                    result[0] = applyPrefixVt(0, result);
                    vcr.pfxt.enabled = false;
                }
                return result;

            case 2:
                r = (vt & 64) >> 5;
                if ((vt & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                }
                if (vcr.pfxt.enabled) {
                    result[0] = applyPrefixVt(0, result);
                    result[1] = applyPrefixVt(1, result);
                    vcr.pfxt.enabled = false;
                }
                return result;

            case 3:
                r = (vt & 64) >> 6;
                if ((vt & 32) != 0) {
                    result[0] = vpr[m][r + 0][c];
                    result[1] = vpr[m][r + 1][c];
                    result[2] = vpr[m][r + 2][c];
                } else {
                    result[0] = vpr[m][c][r + 0];
                    result[1] = vpr[m][c][r + 1];
                    result[2] = vpr[m][c][r + 2];
                }
                if (vcr.pfxt.enabled) {
                    result[0] = applyPrefixVt(0, result);
                    result[1] = applyPrefixVt(1, result);
                    result[2] = applyPrefixVt(2, result);
                    vcr.pfxt.enabled = false;
                }
                return result;

            case 4:
                if ((vt & 32) != 0) {
                    result[0] = vpr[m][0][c];
                    result[1] = vpr[m][1][c];
                    result[2] = vpr[m][2][c];
                    result[3] = vpr[m][3][c];
                } else {
                    result[0] = vpr[m][c][0];
                    result[1] = vpr[m][c][1];
                    result[2] = vpr[m][c][2];
                    result[3] = vpr[m][c][3];
                }
                if (vcr.pfxt.enabled) {
                    result[0] = applyPrefixVt(0, result);
                    result[1] = applyPrefixVt(1, result);
                    result[2] = applyPrefixVt(2, result);
                    result[3] = applyPrefixVt(3, result);
                    vcr.pfxt.enabled = false;
                }
                return result;

            default:
        }
        return null;
    }

    public void saveVd(int vsize, int vd, float[] result) {
        int m, r, c;

        m = (vd >> 2) & 7;
        c = (vd >> 0) & 3;

        switch (vsize) {
            case 1:
                r = (vd >> 5) & 3;
                if (vcr.pfxd.enabled) {
                    if (!vcr.pfxd.msk[0]) {
                        vpr[m][c][r] = applyPrefixVd(0, result[0]);
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    vpr[m][c][r] = result[0];
                }
                break;

            case 2:
                r = (vd & 64) >> 5;
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 2; ++i) {
                            if (!vcr.pfxd.msk[i]) {
                                vpr[m][r + i][c] = applyPrefixVd(i, result[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < 2; ++i) {
                            if (!vcr.pfxd.msk[i]) {
                                vpr[m][c][r + i] = applyPrefixVd(i, result[i]);
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 2; ++i) {
                            vpr[m][r + i][c] = result[i];
                        }
                    } else {
                        for (int i = 0; i < 2; ++i) {
                            vpr[m][c][r + i] = result[i];
                        }
                    }
                }
                break;

            case 3:
                r = (vd & 64) >> 6;
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 3; ++i) {
                            if (!vcr.pfxd.msk[i]) {
                                vpr[m][r + i][c] = applyPrefixVd(i, result[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < 3; ++i) {
                            if (!vcr.pfxd.msk[i]) {
                                vpr[m][c][r + i] = applyPrefixVd(i, result[i]);
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 3; ++i) {
                            vpr[m][r + i][c] = result[i];
                        }
                    } else {
                        for (int i = 0; i < 3; ++i) {
                            vpr[m][c][r + i] = result[i];
                        }
                    }
                }
                break;

            case 4:
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 4; ++i) {
                            if (!vcr.pfxd.msk[i]) {
                                vpr[m][i][c] = applyPrefixVd(i, result[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < 4; ++i) {
                            if (!vcr.pfxd.msk[i]) {
                                vpr[m][c][i] = applyPrefixVd(i, result[i]);
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int i = 0; i < 4; ++i) {
                            vpr[m][i][c] = result[i];
                        }
                    } else {
                        for (int i = 0; i < 4; ++i) {
                            vpr[m][c][i] = result[i];
                        }
                    }
                }
                break;

            default:
                break;
        }
    }

    // VFPU0
    public void doVADD(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] += x2[i];
        }

        saveVd(vsize, vd, x1);
    }

    public void doVSUB(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] -= x2[i];
        }

        saveVd(vsize, vd, x1);
    }

    public void doVSBN(int vsize, int vd, int vs, int vt) {
        if (vsize != 1) {
            doUNK("Only supported VSBN.S instruction");
        }

        float[] x1 = loadVs(1, vs);
        float[] x2 = loadVt(1, vt);

        x1[0] = Math.scalb(x1[0], Float.floatToRawIntBits(x2[0]));

        saveVd(1, vd, x1);
    }

    public void doVDIV(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] /= x2[i];
        }

        saveVd(vsize, vd, x1);
    }

    // VFPU1
    public void doVMUL(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] *= x2[i];
        }

        saveVd(vsize, vd, x1);
    }

    public void doVDOT(int vsize, int vd, int vs, int vt) {
        if (vsize == 1) {
            doUNK("Unsupported VDOT.S instruction");
        }

        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);
        float[] x3 = new float[1];

        for (int i = 0; i < vsize; ++i) {
            x3[0] += x1[i] * x2[i];
        }

        saveVd(1, vd, x3);
    }

    public void doVSCL(int vsize, int vd, int vs, int vt) {
        doUNK("Not yet supported VFPU instruction");
    }

    public void doVHDP(int vsize, int vd, int vs, int vt) {
        if (vsize == 1) {
            doUNK("Unsupported VHDP.S instruction");
        }

        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);
        float[] x3 = new float[1];

        int i;
        for (i = 0; i < vsize - 1; ++i) {
            x3[0] += x1[i] * x2[i];
        }

        x3[0] += x2[i];

        saveVd(1, vd, x3);
    }

    public void doVCRS(int vsize, int vd, int vs, int vt) {
        if (vsize != 3) {
            doUNK("Only supported VCRS.T instruction");
        }

        float[] x1 = loadVs(3, vs);
        float[] x2 = loadVt(3, vt);
        float[] x3 = new float[3];

        x3[0] = x1[1] * x2[2];
        x3[1] = x1[2] * x2[0];
        x3[2] = x1[0] * x2[1];

        saveVd(3, vd, x3);
    }

    public void doVDET(int vsize, int vd, int vs, int vt) {
        if (vsize != 2) {
            doUNK("Only supported VDET.P instruction");
        }

        float[] x1 = loadVs(2, vs);
        float[] x2 = loadVt(2, vt);
        float[] x3 = new float[1];

        x3[0] = x1[0] * x2[1] - x1[1] * x2[0];

        saveVd(1, vd, x3);
    }

    // VFPU3
    public void doVCMP(int vsize, int vs, int vt, int cond) {
        boolean cc_or = false;
        boolean cc_and = true;

        if ((cond & 8) == 0) {
            boolean not = ((cond & 4) == 4);

            boolean cc = false;

            float[] x1 = loadVs(vsize, vs);
            float[] x2 = loadVt(vsize, vt);

            for (int i = 0; i < vsize; ++i) {
                switch (cond & 3) {
                    case 0:
                        cc = not;
                        break;

                    case 1:
                        cc = not ? (x1[i] != x2[i]) : (x1[i] == x2[i]);
                        break;

                    case 2:
                        cc = not ? (x1[i] >= x2[i]) : (x1[i] < x2[i]);
                        break;

                    case 3:
                        cc = not ? (x1[i] > x2[i]) : (x1[i] <= x2[i]);
                        break;

                }


                vcr.cc[i] = cc;
                cc_or = cc_or || cc;
                cc_and = cc_and && cc;
            }

        } else {
            float[] x1 = loadVs(vsize, vs);

            for (int i = 0; i < vsize; ++i) {
                boolean cc;
                if ((cond & 3) == 0) {
                    cc = ((cond & 4) == 0) ? (x1[i] == 0.0f) : (x1[i] != 0.0f);
                } else {
                    cc = (((cond & 1) == 1) && Float.isNaN(x1[i])) ||
                            (((cond & 2) == 2) && Float.isInfinite(x1[i]));
                    if ((cond & 4) == 4) {
                        cc = !cc;
                    }

                }
                vcr.cc[i] = cc;
                cc_or = cc_or || cc;
                cc_and = cc_and && cc;
            }

        }
        vcr.cc[4] = cc_or;
        vcr.cc[5] = cc_and;
    }

    public void doVMIN(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = Math.min(x1[i], x2[i]);
        }

        saveVd(vsize, vd, x1);
    }

    public void doVMAX(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = Math.max(x1[i], x2[i]);
        }

        saveVd(vsize, vd, x1);
    }

    public void doVSCMP(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = Math.signum(x1[i] - x2[i]);
        }

        saveVd(vsize, vd, x1);
    }

    public void doVSGE(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = (x1[i] >= x2[i]) ? 1.0f : 0.0f;
        }

        saveVd(vsize, vd, x1);
    }

    public void doVSLT(int vsize, int vd, int vs, int vt) {
        float[] x1 = loadVs(vsize, vs);
        float[] x2 = loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            x1[i] = (x1[i] < x2[i]) ? 1.0f : 0.0f;
        }

        saveVd(vsize, vd, x1);
    }

    public void doVPFXS(int imm24) {
        vcr.pfxs.swz[0] = (imm24 >> 0) & 3;
        vcr.pfxs.swz[1] = (imm24 >> 2) & 3;
        vcr.pfxs.swz[2] = (imm24 >> 4) & 3;
        vcr.pfxs.swz[3] = (imm24 >> 6) & 3;
        vcr.pfxs.abs[0] = (imm24 >> 8) != 0;
        vcr.pfxs.abs[1] = (imm24 >> 9) != 0;
        vcr.pfxs.abs[2] = (imm24 >> 10) != 0;
        vcr.pfxs.abs[3] = (imm24 >> 11) != 0;
        vcr.pfxs.cst[0] = (imm24 >> 12) != 0;
        vcr.pfxs.cst[1] = (imm24 >> 13) != 0;
        vcr.pfxs.cst[2] = (imm24 >> 14) != 0;
        vcr.pfxs.cst[3] = (imm24 >> 15) != 0;
        vcr.pfxs.neg[0] = (imm24 >> 16) != 0;
        vcr.pfxs.neg[1] = (imm24 >> 17) != 0;
        vcr.pfxs.neg[2] = (imm24 >> 18) != 0;
        vcr.pfxs.neg[3] = (imm24 >> 19) != 0;
        vcr.pfxs.enabled = true;
    }

    public void doVPFXT(int imm24) {
        vcr.pfxt.swz[0] = (imm24 >> 0) & 3;
        vcr.pfxt.swz[1] = (imm24 >> 2) & 3;
        vcr.pfxt.swz[2] = (imm24 >> 4) & 3;
        vcr.pfxt.swz[3] = (imm24 >> 6) & 3;
        vcr.pfxt.abs[0] = (imm24 >> 8) != 0;
        vcr.pfxt.abs[1] = (imm24 >> 9) != 0;
        vcr.pfxt.abs[2] = (imm24 >> 10) != 0;
        vcr.pfxt.abs[3] = (imm24 >> 11) != 0;
        vcr.pfxt.cst[0] = (imm24 >> 12) != 0;
        vcr.pfxt.cst[1] = (imm24 >> 13) != 0;
        vcr.pfxt.cst[2] = (imm24 >> 14) != 0;
        vcr.pfxt.cst[3] = (imm24 >> 15) != 0;
        vcr.pfxt.neg[0] = (imm24 >> 16) != 0;
        vcr.pfxt.neg[1] = (imm24 >> 17) != 0;
        vcr.pfxt.neg[2] = (imm24 >> 18) != 0;
        vcr.pfxt.neg[3] = (imm24 >> 19) != 0;
        vcr.pfxt.enabled = true;
    }

    public void doVPFXD(int imm24) {
        vcr.pfxd.sat[0] = (imm24 >> 0) & 3;
        vcr.pfxd.sat[1] = (imm24 >> 2) & 3;
        vcr.pfxd.sat[2] = (imm24 >> 4) & 3;
        vcr.pfxd.sat[3] = (imm24 >> 6) & 3;
        vcr.pfxd.msk[0] = (imm24 >> 8) != 0;
        vcr.pfxd.msk[1] = (imm24 >> 9) != 0;
        vcr.pfxd.msk[2] = (imm24 >> 10) != 0;
        vcr.pfxd.msk[3] = (imm24 >> 11) != 0;
        vcr.pfxd.enabled = true;
    }

    public void doVIIM(int vs, int imm16) {
        float[] result = new float[1];

        result[0] = (float) imm16;

        saveVd(1, vs, result);
    }

    public void doVFIM(int vs, int imm16) {
        float[] result = new float[1];

        float s = ((imm16 >> 15) == 0) ? 1.0f : -1.0f;
        int e = ((imm16 >> 10) & 0x1f);
        int m = (e == 0) ? ((imm16 & 0x3ff) << 1) : ((imm16 & 0x3ff) | 0x400);

        result[0] = s * ((float) m) * ((float) (1 << e)) / ((float) (1 << 41));

        saveVd(1, vs, result);
    }

    public void doLVS(int vt, int rs, int simm14) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;
        vpr[m][c][r] = Float.intBitsToFloat(memory.read32(gpr[rs] + (simm14 << 2)));
    }

    public void doSVS(int vt, int rs, int simm14) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;
        memory.write32(gpr[rs] + (simm14 << 2), Float.floatToRawIntBits(vpr[m][r][c]));
    }

    public void doLVQ(int vt, int rs, int simm14) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;
        vpr[m][c][r] = Float.intBitsToFloat(memory.read32(gpr[rs] + (simm14 << 2)));
    }

    public void doSVQ(int vt, int rs, int simm14) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;
        memory.write32(gpr[rs] + (simm14 << 2), Float.floatToRawIntBits(vpr[m][r][c]));
    }

}
