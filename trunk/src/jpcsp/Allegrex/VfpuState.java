/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

/**
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
            pfxs.reset();
            pfxt.reset();
            pfxd.reset();
            cc = new boolean[6];
        }

        public Vcr() {
            reset();
        }

        public Vcr(Vcr that) {
            pfxs = new PfxSrc(that.pfxs);
            pfxt = new PfxSrc(that.pfxt);
            pfxd = new PfxDst(that.pfxd);
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
            for (int c = 0; m < 8; ++m) {
                for (int r = 0; m < 8; ++m) {
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
    
}
