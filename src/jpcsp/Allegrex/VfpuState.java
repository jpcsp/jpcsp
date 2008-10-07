/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

import jpcsp.Memory;

import java.util.Arrays;

/**
 * Vectorial Floating Point Unit, handles scalar, vector and matrix operations.
 *
 * @author hli
 */
public class VfpuState extends FpuState {

    public float[][][] vpr; // mtx, fsl, idx

	private static final float floatConstants[] =
			{ 0.0f
			, Float.MAX_VALUE
			, (float) Math.sqrt(2.0f)
			, (float) Math.sqrt(0.5f)
			, 2.0f / (float) Math.sqrt(Math.PI)
			, 2.0f / (float) Math.PI
			, 1.0f / (float) Math.PI
			, (float) Math.PI / 4.0f
			, (float) Math.PI / 2.0f
			, (float) Math.PI
			, (float) Math.E
			, (float) (Math.log(Math.E) / Math.log(2.0))	// log2(E) = log(E) / log(2)
			, (float) Math.log10(Math.E)
			, (float) Math.log(2.0)
			, (float) Math.log(10.0)
			, (float) Math.PI * 2.0f
			, (float) Math.PI / 6.0f
			, (float) Math.log10(2.0)
			, (float) (Math.log(10.0) / Math.log(2.0))		// log2(10) = log(10) / log(2)
			, (float) Math.sqrt(3.0) / 2.0f
			};

	public class Vcr {

        public class PfxSrc /* $128, $129 */ {

            public int[] swz;
            public boolean[] abs;
            public boolean[] cst;
            public boolean[] neg;
            public boolean enabled;

            public void reset() {
                Arrays.fill(swz, 0);
                Arrays.fill(abs, false);
                Arrays.fill(cst, false);
                Arrays.fill(neg, false);
                enabled = false;
            }

            public PfxSrc() {
                swz = new int[4];
                abs = new boolean[4];
                cst = new boolean[4];
                neg = new boolean[4];
                enabled = false;
            }

            public void copy(PfxSrc that) {
                swz = that.swz.clone();
                abs = that.abs.clone();
                cst = that.cst.clone();
                neg = that.neg.clone();
                enabled = that.enabled;
            }

            public PfxSrc(PfxSrc that) {
                copy(that);
            }
        }
        public PfxSrc pfxs;
        public PfxSrc pfxt;

        public class PfxDst /* 130 */ {

            public int[] sat;
            public boolean[] msk;
            public boolean enabled;

            public void reset() {
                Arrays.fill(sat, 0);
                Arrays.fill(msk, false);
                enabled = false;
            }

            public PfxDst() {
                sat = new int[4];
                msk = new boolean[4];
                enabled = false;
            }

            public void copy(PfxDst that) {
                sat = that.sat.clone();
                msk = that.msk.clone();
                enabled = that.enabled;
            }

            public PfxDst(PfxDst that) {
                copy(that);
            }
        }
        public PfxDst pfxd;
        public boolean[] /* 131 */ cc;

        public void reset() {
            pfxs.reset();
            pfxt.reset();
            pfxd.reset();
            Arrays.fill(cc, false);
        }

        public Vcr() {
            pfxs = new PfxSrc();
            pfxt = new PfxSrc();
            pfxd = new PfxDst();
            cc = new boolean[6];
        }

        public void copy(Vcr that) {
            pfxs.copy(that.pfxs);
            pfxt.copy(that.pfxt);
            pfxd.copy(that.pfxd);
            cc = that.cc.clone();
        }

        public Vcr(Vcr that) {
            pfxs = new PfxSrc(that.pfxs);
            pfxt = new PfxSrc(that.pfxt);
            pfxd = new PfxDst(that.pfxd);
            cc = that.cc.clone();
        }
    }
    public Vcr vcr;

    private void resetFpr() {
        for (float[][] m : vpr) {
            for (float[] v : m) {
                Arrays.fill(v, 0.0f);
            }
        }
    }

    @Override
    public void reset() {
        resetFpr();
        vcr.reset();
    }

    @Override
    public void resetAll() {
        super.resetAll();
        resetFpr();
        vcr.reset();
    }

    public VfpuState() {
        vpr = new float[8][4][4]; // [matrix][column][row]
        vcr = new Vcr();
    }

    public void copy(VfpuState that) {
        super.copy(that);
        vpr = that.vpr.clone();
        vcr = new Vcr(that.vcr);
    }

    public VfpuState(VfpuState that) {
        super(that);
        vpr = that.vpr.clone();
        vcr = new Vcr(that.vcr);
    }
    private static float[] v1 = new float[4];
    private static float[] v2 = new float[4];
    private static float[] v3 = new float[4];
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

    public void loadVs(int vsize, int vs) {
        int m, s, i;

        m = (vs >> 2) & 7;
        i = (vs >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vs >> 5) & 3;
                v1[0] = vpr[m][i][s];
                if (vcr.pfxs.enabled) {
                    v1[0] = applyPrefixVs(0, v1);
                    vcr.pfxs.enabled = false;
                }
                return;

            case 2:
                s = (vs & 64) >> 5;
                if ((vs & 32) != 0) {
                    v1[0] = vpr[m][s + 0][i];
                    v1[1] = vpr[m][s + 1][i];
                } else {
                    v1[0] = vpr[m][i][s + 0];
                    v1[1] = vpr[m][i][s + 1];
                }
                if (vcr.pfxs.enabled) {
                    v1[0] = applyPrefixVs(0, v1);
                    v1[1] = applyPrefixVs(1, v1);
                    vcr.pfxs.enabled = false;
                }
                return;

            case 3:
                s = (vs & 64) >> 6;
                if ((vs & 32) != 0) {
                    v1[0] = vpr[m][s + 0][i];
                    v1[1] = vpr[m][s + 1][i];
                    v1[2] = vpr[m][s + 2][i];
                } else {
                    v1[0] = vpr[m][i][s + 0];
                    v1[1] = vpr[m][i][s + 1];
                    v1[2] = vpr[m][i][s + 2];
                }
                if (vcr.pfxs.enabled) {
                    v1[0] = applyPrefixVs(0, v1);
                    v1[1] = applyPrefixVs(1, v1);
                    v1[2] = applyPrefixVs(2, v1);
                    vcr.pfxs.enabled = false;
                }
                return;

            case 4:
                if ((vs & 32) != 0) {
                    v1[0] = vpr[m][0][i];
                    v1[1] = vpr[m][1][i];
                    v1[2] = vpr[m][2][i];
                    v1[3] = vpr[m][3][i];
                } else {
                    v1[0] = vpr[m][i][0];
                    v1[1] = vpr[m][i][1];
                    v1[2] = vpr[m][i][2];
                    v1[3] = vpr[m][i][3];
                }
                if (vcr.pfxs.enabled) {
                    v1[0] = applyPrefixVs(0, v1);
                    v1[1] = applyPrefixVs(1, v1);
                    v1[2] = applyPrefixVs(2, v1);
                    v1[3] = applyPrefixVs(3, v1);
                    vcr.pfxs.enabled = false;
                }
            default:
        }
    }

    public void loadVt(int vsize, int vt) {
        int m, s, i;

        m = (vt >> 2) & 7;
        i = (vt >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vt >> 5) & 3;
                v2[0] = vpr[m][i][s];
                if (vcr.pfxt.enabled) {
                    v2[0] = applyPrefixVt(0, v2);
                    vcr.pfxt.enabled = false;
                }
                return;

            case 2:
                s = (vt & 64) >> 5;
                if ((vt & 32) != 0) {
                    v2[0] = vpr[m][s + 0][i];
                    v2[1] = vpr[m][s + 1][i];
                } else {
                    v2[0] = vpr[m][i][s + 0];
                    v2[1] = vpr[m][i][s + 1];
                }
                if (vcr.pfxt.enabled) {
                    v2[0] = applyPrefixVt(0, v2);
                    v2[1] = applyPrefixVt(1, v2);
                    vcr.pfxt.enabled = false;
                }
                return;

            case 3:
                s = (vt & 64) >> 6;
                if ((vt & 32) != 0) {
                    v2[0] = vpr[m][s + 0][i];
                    v2[1] = vpr[m][s + 1][i];
                    v2[2] = vpr[m][s + 2][i];
                } else {
                    v2[0] = vpr[m][i][s + 0];
                    v2[1] = vpr[m][i][s + 1];
                    v2[2] = vpr[m][i][s + 2];
                }
                if (vcr.pfxt.enabled) {
                    v2[0] = applyPrefixVt(0, v2);
                    v2[1] = applyPrefixVt(1, v2);
                    v2[2] = applyPrefixVt(2, v2);
                    vcr.pfxt.enabled = false;
                }
                return;

            case 4:
                if ((vt & 32) != 0) {
                    v2[0] = vpr[m][0][i];
                    v2[1] = vpr[m][1][i];
                    v2[2] = vpr[m][2][i];
                    v2[3] = vpr[m][3][i];
                } else {
                    v2[0] = vpr[m][i][0];
                    v2[1] = vpr[m][i][1];
                    v2[2] = vpr[m][i][2];
                    v2[3] = vpr[m][i][3];
                }
                if (vcr.pfxt.enabled) {
                    v2[0] = applyPrefixVt(0, v2);
                    v2[1] = applyPrefixVt(1, v2);
                    v2[2] = applyPrefixVt(2, v2);
                    v2[3] = applyPrefixVt(3, v2);
                    vcr.pfxt.enabled = false;
                }
            default:
        }
    }

    public void saveVd(int vsize, int vd, float[] vr) {
        int m, s, i;

        m = (vd >> 2) & 7;
        i = (vd >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vd >> 5) & 3;
                if (vcr.pfxd.enabled) {
                    if (!vcr.pfxd.msk[0]) {
                        vpr[m][i][s] = applyPrefixVd(0, vr[0]);
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    vpr[m][i][s] = vr[0];
                }
                break;

            case 2:
                s = (vd & 64) >> 5;
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 2; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                vpr[m][s + j][j] = applyPrefixVd(j, vr[j]);
                            }
                        }
                    } else {
                        for (int j = 0; j < 2; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                vpr[m][j][s + j] = applyPrefixVd(j, vr[j]);
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 2; ++j) {
                            vpr[m][s + j][j] = vr[j];
                        }
                    } else {
                        for (int j = 0; j < 2; ++j) {
                            vpr[m][j][s + j] = vr[j];
                        }
                    }
                }
                break;

            case 3:
                s = (vd & 64) >> 6;
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 3; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                vpr[m][s + j][j] = applyPrefixVd(j, vr[j]);
                            }
                        }
                    } else {
                        for (int j = 0; j < 3; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                vpr[m][j][s + j] = applyPrefixVd(j, vr[j]);
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 3; ++j) {
                            vpr[m][s + j][j] = vr[j];
                        }
                    } else {
                        for (int j = 0; j < 3; ++j) {
                            vpr[m][j][s + j] = vr[j];
                        }
                    }
                }
                break;

            case 4:
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 4; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                vpr[m][j][j] = applyPrefixVd(j, vr[j]);
                            }
                        }
                    } else {
                        for (int j = 0; j < 4; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                vpr[m][j][j] = applyPrefixVd(j, vr[j]);
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 4; ++j) {
                            vpr[m][j][j] = vr[j];
                        }
                    } else {
                        for (int j = 0; j < 4; ++j) {
                            vpr[m][j][j] = vr[j];
                        }
                    }
                }
                break;

            default:
                break;
        }
    }
    // group VFPU0
    // VFPU0:VADD
    public void doVADD(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v1[i] += v2[i];
        }

        saveVd(vsize, vd, v1);
    }

    // VFPU0:VSUB
    public void doVSUB(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v1[i] -= v2[i];
        }

        saveVd(vsize, vd, v1);
    }

    // VFPU0:VSBN
    public void doVSBN(int vsize, int vd, int vs, int vt) {
        if (vsize != 1) {
            doUNK("Only supported VSBN.S");
        }

        loadVs(1, vs);
        loadVt(1, vt);

        v1[0] = Math.scalb(v1[0], Float.floatToRawIntBits(v2[0]));

        saveVd(1, vd, v1);
    }

    // VFPU0:VDIV
    public void doVDIV(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v1[i] /= v2[i];
        }

        saveVd(vsize, vd, v1);
    }

    // group VFPU1
    // VFPU1:VMUL
    public void doVMUL(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v1[i] *= v2[i];
        }

        saveVd(vsize, vd, v1);
    }

    // VFPU1:VDOT
    public void doVDOT(int vsize, int vd, int vs, int vt) {
        if (vsize == 1) {
            doUNK("Unsupported VDOT.S");
        }

        loadVs(vsize, vs);
        loadVt(vsize, vt);

        float dot = v1[0] * v2[0];

        for (int i = 1; i < vsize; ++i) {
            dot += v1[i] * v2[i];
        }

        v3[0] = dot;

        saveVd(1, vd, v3);
    }

    // VFPU1:VSCL
    public void doVSCL(int vsize, int vd, int vs, int vt) {
        if (vsize == 1) {
            doUNK("Unsupported VSCL.S");
        }

        loadVs(vsize, vs);
        loadVt(vsize, vt);

        float scale = v2[0];

        for (int i = 1; i < vsize; ++i) {
            v1[i] *= scale;
        }

        saveVd(1, vd, v1);
    }

    // VFPU1:VHDP
    public void doVHDP(int vsize, int vd, int vs, int vt) {
        if (vsize == 1) {
            doUNK("Unsupported VHDP.S");
        }

        loadVs(vsize, vs);
        loadVt(vsize, vt);

        float hdp = v1[0] * v2[0];

        int i;

        for (i = 1; i < vsize - 1; ++i) {
            hdp += v1[i] * v2[i];
        }

        v2[0] += hdp;

        saveVd(1, vd, v2);
    }

    // VFPU1:VCRS
    public void doVCRS(int vsize, int vd, int vs, int vt) {
        if (vsize != 3) {
            doUNK("Only supported VCRS.T");
        }

        loadVs(3, vs);
        loadVt(3, vt);

        v3[0] = v1[1] * v2[2];
        v3[1] = v1[2] * v2[0];
        v3[2] = v1[0] * v2[1];

        saveVd(3, vd, v3);
    }

    // VFPU1:VDET
    public void doVDET(int vsize, int vd, int vs, int vt) {
        if (vsize != 2) {
            doUNK("Only supported VDET.P");
        }

        loadVs(2, vs);
        loadVt(2, vt);

        v1[0] = v1[0] * v2[1] - v1[1] * v2[0];

        saveVd(1, vd, v1);
    }

    // VFPU2

    // VFPU2:MFV
    public void doMFV(int rt, int imm7) {
        int r = (imm7 >> 5) & 3;
        int m = (imm7 >> 2) & 7;
        int c = (imm7 >> 0) & 3;

        gpr[rt] = Float.floatToRawIntBits(vpr[m][r][c]);
    }
    // VFPU2:MFVC
    public void doMFVC(int rt, int imm7) {
        doUNK("Unimplemented MFVC");
    }
    // VFPU2:MTV
    public void doMTV(int rt, int imm7) {
        int r = (imm7 >> 5) & 3;
        int m = (imm7 >> 2) & 7;
        int c = (imm7 >> 0) & 3;

        vpr[m][c][r] = Float.intBitsToFloat(gpr[rt]);
    }

    // VFPU2:MTVC
    public void doMTVC(int rt, int imm7) {
        doUNK("Unimplemented MTVC");
    }

    // VFPU2:BVF
    public boolean doBVF(int imm3, int simm16) {
        doUNK("Unimplemented BVF");
        return true;
    }
    // VFPU2:BVT
    public boolean doBVT(int imm3, int simm16) {
        doUNK("Unimplemented BVT");
        return true;
    }
    // VFPU2:BVFL
    public boolean doBVFL(int imm3, int simm16) {
        doUNK("Unimplemented BVFL");
        return true;
    }
    // VFPU2:BVTL
    public boolean doBVTL(int imm3, int simm16) {
        doUNK("Unimplemented BVTL");
        return true;
    }
    // group VFPU3

    // VFPU3:VCMP
    public void doVCMP(int vsize, int vs, int vt, int cond) {
        boolean cc_or = false;
        boolean cc_and = true;

        if ((cond & 8) == 0) {
            boolean not = ((cond & 4) == 4);

            boolean cc = false;

            loadVs(vsize, vs);
            loadVt(vsize, vt);

            for (int i = 0; i < vsize; ++i) {
                switch (cond & 3) {
                    case 0:
                        cc = not;
                        break;

                    case 1:
                        cc = not ? (v1[i] != v2[i]) : (v1[i] == v2[i]);
                        break;

                    case 2:
                        cc = not ? (v1[i] >= v2[i]) : (v1[i] < v2[i]);
                        break;

                    case 3:
                        cc = not ? (v1[i] > v2[i]) : (v1[i] <= v2[i]);
                        break;

                }


                vcr.cc[i] = cc;
                cc_or = cc_or || cc;
                cc_and = cc_and && cc;
            }

        } else {
            loadVs(vsize, vs);

            for (int i = 0; i < vsize; ++i) {
                boolean cc;
                if ((cond & 3) == 0) {
                    cc = ((cond & 4) == 0) ? (v1[i] == 0.0f) : (v1[i] != 0.0f);
                } else {
                    cc = (((cond & 1) == 1) && Float.isNaN(v1[i])) ||
                            (((cond & 2) == 2) && Float.isInfinite(v1[i]));
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

    // VFPU3:VMIN
    public void doVMIN(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v3[i] = Math.min(v1[i], v2[i]);
        }

        saveVd(vsize, vd, v3);
    }

    // VFPU3:VMAX
    public void doVMAX(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v3[i] = Math.max(v1[i], v2[i]);
        }

        saveVd(vsize, vd, v3);
    }

    // VFPU3:VSCMP
    public void doVSCMP(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v3[i] = Math.signum(v1[i] - v2[i]);
        }

        saveVd(vsize, vd, v3);
    }

    // VFPU3:VSGE
    public void doVSGE(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v3[i] = (v1[i] >= v2[i]) ? 1.0f : 0.0f;
        }

        saveVd(vsize, vd, v3);
    }

    // VFPU3:VSLT
    public void doVSLT(int vsize, int vd, int vs, int vt) {
        loadVs(vsize, vs);
        loadVt(vsize, vt);

        for (int i = 0; i < vsize; ++i) {
            v3[i] = (v1[i] < v2[i]) ? 1.0f : 0.0f;
        }

        saveVd(vsize, vd, v3);
    }

    // group VFPU4
    // VFPU4:VMOV
    public void doVMOV(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        saveVd(vsize, vd, v1);
    }

    // VFPU4:VABS
    public void doVABS(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = Math.abs(v1[i]);
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VNEG
    public void doVNEG(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = 0.0f - v1[i];
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VIDT
    public void doVIDT(int vsize, int vd) {
        int id = vd & 3;
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (id == i) ? 1.0f : 0.0f;
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VSAT0
    public void doVSAT0(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = Math.min(Math.max(0.0f, v1[i]), 1.0f);
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VSAT1
    public void doVSAT1(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = Math.min(Math.max(-1.0f, v1[i]), 1.0f);
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VZERO
    public void doVZERO(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            v3[i] = 0.0f;
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VONE
    public void doVONE(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            v3[i] = 1.0f;
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VRCP
    public void doVRCP(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = 1.0f / v1[i];
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VRSQ
    public void doVRSQ(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) (1.0 / Math.sqrt(v1[i]));
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VSIN
    public void doVSIN(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) Math.sin(2.0 * Math.PI * v1[i]);
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VCOS
    public void doVCOS(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) Math.cos(2.0 * Math.PI * v1[i]);
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VEXP2
    public void doVEXP2(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) Math.pow(2.0, v1[i]);
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VLOG2
    public void doVLOG2(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) (Math.log(v1[i]) / Math.log(2.0));
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VSQRT
    public void doVSQRT(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) (Math.sqrt(v1[i]));
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VASIN
    public void doVASIN(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) (Math.asin(v1[i]) * 0.5 / Math.PI);
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VNRCP
    public void doVNRCP(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = 0.0f - (1.0f / v1[i]);
        }
        saveVd(vsize, vd, v3);
    }

    // VFPU4:VNSIN
    public void doVNSIN(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = 0.0f - (float) Math.sin(2.0 * Math.PI * v1[i]);
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VREXP2
    public void doVREXP2(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) (1.0 / Math.pow(2.0, v1[i]));
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VRNDS
    public void doVRNDS(int vsize, int vs) {
        doUNK("Unimplemented VRNDS");
    }
    // VFPU4:VRNDS
    public void doVRNDI(int vsize, int vd) {
        doUNK("Unimplemented VRNDS");
    }
    // VFPU4:VRNDS
    public void doVRNDF1(int vsize, int vd) {
        doUNK("Unimplemented VRNDS");
    }
    // VFPU4:VRNDS
    public void doVRNDF2(int vsize, int vd) {
        doUNK("Unimplemented VRNDS");
    }
    // VFPU4:VF2H
    public void doVF2H(int vsize, int vd, int vs) {
        doUNK("Unimplemented VF2H");
    }
    // VFPU4:VH2F
    public void doVH2F(int vsize, int vd, int vs) {
        doUNK("Unimplemented VH2F");
    }
    // VFPU4:VSBZ
    public void doVSBZ(int vsize, int vd, int vs) {
        doUNK("Unimplemented VSBZ");
    }
    // VFPU4:VLGB
    public void doVLGB(int vsize, int vd, int vs) {
        doUNK("Unimplemented VLGB");
    }
    // VFPU4:VUC2I
    public void doVUC2I(int vsize, int vd, int vs) {
        doUNK("Unimplemented VUC2I");
    }
    // VFPU4:VC2I
    public void doVC2I(int vsize, int vd, int vs) {
        doUNK("Unimplemented VC2I");
    }
    // VFPU4:VUS2I
    public void doVUS2I(int vsize, int vd, int vs) {
        doUNK("Unimplemented VUS2I");
    }
    // VFPU4:VS2I
    public void doVS2I(int vsize, int vd, int vs) {
        doUNK("Unimplemented VS2I");
    }
    // VFPU4:VI2UC
    public void doVI2UC(int vsize, int vd, int vs) {
        doUNK("Unimplemented VI2UC");
    }
    // VFPU4:VI2C
    public void doVI2C(int vsize, int vd, int vs) {
        doUNK("Unimplemented VI2C");
    }
    // VFPU4:VI2US
    public void doVI2US(int vsize, int vd, int vs) {
        doUNK("Unimplemented VI2US");
    }
    // VFPU4:VI2S
    public void doVI2S(int vsize, int vd, int vs) {
        doUNK("Unimplemented VI2S");
    }
    // VFPU4:VSRT1
    public void doVSRT1(int vsize, int vd, int vs) {
        doUNK("Unimplemented VSRT1");
    }
    // VFPU4:VSRT2
    public void doVSRT2(int vsize, int vd, int vs) {
        doUNK("Unimplemented VSRT2");
    }
    // VFPU4:VBFY1
    public void doVBFY1(int vsize, int vd, int vs) {
        doUNK("Unimplemented VBFY1");
    }
    // VFPU4:VBFY2
    public void doVBFY2(int vsize, int vd, int vs) {
        doUNK("Unimplemented VBFY2");
    }
    // VFPU4:VOCP
    public void doVOCP(int vsize, int vd, int vs) {
        doUNK("Unimplemented VOCP");
    }
    // VFPU4:VSOCP
    public void doVSOCP(int vsize, int vd, int vs) {
        doUNK("Unimplemented VSOCP");
    }
    // VFPU4:VFAD
    public void doVFAD(int vsize, int vd, int vs) {
        doUNK("Unimplemented VFAD");
    }
    // VFPU4:VAVG
    public void doVAVG(int vsize, int vd, int vs) {
        doUNK("Unimplemented VAVG");
    }
    // VFPU4:VSRT3
    public void doVSRT3(int vsize, int vd, int vs) {
        doUNK("Unimplemented VSRT3");
    }
    // VFPU4:VSRT4
    public void doVSRT4(int vsize, int vd, int vs) {
        doUNK("Unimplemented VSRT4");
    }
    // VFPU4:VMFVC
    public void doVMFVC(int vd, int imm7) {
        doUNK("Unimplemented VMFVC");
    }
    // VFPU4:VMTVC
    public void doVMTVC(int vd, int imm7) {
        doUNK("Unimplemented VMTVC");
    }
    // VFPU4:VT4444
    public void doVT4444(int vsize, int vd, int vs) {
        doUNK("Unimplemented VT4444");
    }
    // VFPU4:VT5551
    public void doVT5551(int vsize, int vd, int vs) {
        doUNK("Unimplemented VT4444");
    }
    // VFPU4:VT5650
    public void doVT5650(int vsize, int vd, int vs) {
        doUNK("Unimplemented VT5650");
    }
    // VFPU4:VCST
    public void doVCST(int vsize, int vd, int imm5) {
    	float constant = 0.0f;
    	if (imm5 >= 0 && imm5 < floatConstants.length) {
    		constant = floatConstants[imm5];
    	}

    	for (int i = 0; i < vsize; ++i) {
        	v3[i] = constant;
        }

        saveVd(vsize, vd, v3);
    }
    // VFPU4:VF2IN
    public void doVF2IN(int vsize, int vd, int vs, int imm5) {
        doUNK("Unimplemented VF2IN");
    }
    // VFPU4:VF2IZ
    public void doVF2IZ(int vsize, int vd, int vs, int imm5) {
        doUNK("Unimplemented VF2IZ");
    }
    // VFPU4:VF2IU
    public void doVF2IU(int vsize, int vd, int vs, int imm5) {
        doUNK("Unimplemented VF2IU");
    }
    // VFPU4:VF2ID
    public void doVF2ID(int vsize, int vd, int vs, int imm5) {
        doUNK("Unimplemented VF2ID");
    }
    // VFPU4:VI2F
    public void doVI2F(int vsize, int vd, int vs, int imm5) {
        doUNK("Unimplemented VI2F");
    }
    // VFPU4:VCMOVT
    public void doVCMOVT(int vsize, int imm3, int vd, int vs) {
        doUNK("Unimplemented VCMOVT");
    }
    // VFPU4:VCMOVF
    public void doVCMOVF(int vsize, int imm3, int vd, int vs) {
        doUNK("Unimplemented VCMOVF");
    }
    // VFPU4:VWBN
    public void doVWBN(int vsize, int vd, int vs, int imm8) {
        doUNK("Unimplemented VWBN");
    }
    // group VFPU5
    // VFPU5:VPFXS
    public void doVPFXS(
            int negw, int negz, int negy, int negx,
            int cstw, int cstz, int csty, int cstx,
            int absw, int absz, int absy, int absx,
            int swzw, int swzz, int swzy, int swzx) {
        vcr.pfxs.swz[0] = swzx;
        vcr.pfxs.swz[1] = swzy;
        vcr.pfxs.swz[2] = swzz;
        vcr.pfxs.swz[3] = swzw;
        vcr.pfxs.abs[0] = absx != 0;
        vcr.pfxs.abs[1] = absy != 0;
        vcr.pfxs.abs[2] = absz != 0;
        vcr.pfxs.abs[3] = absw != 0;
        vcr.pfxs.cst[0] = cstx != 0;
        vcr.pfxs.cst[1] = csty != 0;
        vcr.pfxs.cst[2] = cstz != 0;
        vcr.pfxs.cst[3] = cstw != 0;
        vcr.pfxs.neg[0] = negx != 0;
        vcr.pfxs.neg[1] = negy != 0;
        vcr.pfxs.neg[2] = negz != 0;
        vcr.pfxs.neg[3] = negw != 0;
        vcr.pfxs.enabled = true;
    }

    // VFPU5:VPFXT
    public void doVPFXT(
            int negw, int negz, int negy, int negx,
            int cstw, int cstz, int csty, int cstx,
            int absw, int absz, int absy, int absx,
            int swzw, int swzz, int swzy, int swzx) {
        vcr.pfxt.swz[0] = swzx;
        vcr.pfxt.swz[1] = swzy;
        vcr.pfxt.swz[2] = swzz;
        vcr.pfxt.swz[3] = swzw;
        vcr.pfxt.abs[0] = absx != 0;
        vcr.pfxt.abs[1] = absy != 0;
        vcr.pfxt.abs[2] = absz != 0;
        vcr.pfxt.abs[3] = absw != 0;
        vcr.pfxt.cst[0] = cstx != 0;
        vcr.pfxt.cst[1] = csty != 0;
        vcr.pfxt.cst[2] = cstz != 0;
        vcr.pfxt.cst[3] = cstw != 0;
        vcr.pfxt.neg[0] = negx != 0;
        vcr.pfxt.neg[1] = negy != 0;
        vcr.pfxt.neg[2] = negz != 0;
        vcr.pfxt.neg[3] = negw != 0;
        vcr.pfxt.enabled = true;
    }

    // VFPU5:VPFXD
    public void doVPFXD(
            int mskw, int mskz, int msky, int mskx,
            int satw, int satz, int saty, int satx) {
        vcr.pfxd.sat[0] = satw;
        vcr.pfxd.sat[1] = satz;
        vcr.pfxd.sat[2] = saty;
        vcr.pfxd.sat[3] = satx;
        vcr.pfxd.msk[0] = mskw != 0;
        vcr.pfxd.msk[1] = mskz != 0;
        vcr.pfxd.msk[2] = msky != 0;
        vcr.pfxd.msk[3] = mskx != 0;
        vcr.pfxd.enabled = true;
    }

    // VFPU5:VIIM
    public void doVIIM(int vd, int imm16) {
        v3[0] = (float) imm16;

        saveVd(1, vd, v3);
    }

    // VFPU5:VFIM
    public void doVFIM(int vd, int imm16) {
        float s = ((imm16 >> 15) == 0) ? 1.0f : -1.0f;
        int e = ((imm16 >> 10) & 0x1f);
        int m = (e == 0) ? ((imm16 & 0x3ff) << 1) : ((imm16 & 0x3ff) | 0x400);

        v3[0] = s * ((float) m) * ((float) (1 << e)) / ((float) (1 << 41));
        
        saveVd(1, vd, v3);
    }

    // group VFPU6   
    // VFPU6:VMMUL
    public void doVMMUL(int vsize, int vd, int vs, int vt) {
        vs = vs ^ 32;

        // not sure :(
        for (int i = 0; i < vsize; ++i) {
            loadVt(vsize, vt + i);
            for (int j = 0; j < vsize; ++j) {
                loadVs(vsize, vs + j);
                float dot = v1[0] * v2[0];
                for (int k = 1; k < vsize; ++k) {
                    dot += v1[k] * v2[k];
                }
                v3[j] = dot;
            }
            saveVd(vsize, vd + i, v3);
        }
    }

    // VFPU6:VHTFM2
    public void doVHTFM2(int vd, int vs, int vt) {
        doUNK("Unimplemented VHTFM2 instruction");
    }

    // VFPU6:VTFM2
    public void doVTFM2(int vd, int vs, int vt) {
        doUNK("Unimplemented VTFM2 instruction");
    }

    // VFPU6:VHTFM3
    public void doVHTFM3(int vd, int vs, int vt) {
        doUNK("Unimplemented VHTFM3 instruction");
    }

    // VFPU6:VTFM3
    public void doVTFM3(int vd, int vs, int vt) {
        doUNK("Unimplemented VTFM3 instruction");
    }

    // VFPU6:VHTFM4
    public void doVHTFM4(int vd, int vs, int vt) {
        doUNK("Unimplemented VHTFM4 instruction");
    }

    // VFPU6:VTFM4
    public void doVTFM4(int vd, int vs, int vt) {
        doUNK("Unimplemented VTFM4 instruction");
    }

    // VFPU6:VMSCL
    public void doVMSCL(int vsize, int vd, int vs, int vt) {
        for (int i = 0; i < vsize; ++i) {
            this.doVSCL(vsize, vd + i, vs + i, vt);
        }
    }

    // VFPU6:VQMUL
    public void doVQMUL(int vd, int vs, int vt) {
        doUNK("Unimplemented VQMUL.Q instruction");
    }

    // VFPU6:VMMOV
    public void doVMMOV(int vsize, int vd, int vs) {
        for (int i = 0; i < vsize; ++i) {
            this.doVMOV(vsize, vd + i, vs + i);
        }
    }

    // VFPU6:VMIDT
    public void doVMIDT(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            this.doVIDT(vsize, vd + i);
        }
    }

    // VFPU6:VMZERO
    public void doVMZERO(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            this.doVZERO(vsize, vd + i);
        }
    }

    // VFPU7:VMONE
    public void doVMONE(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            this.doVONE(vsize, vd + i);
        }
    }

    // VFPU6:VROT
    public void doVROT(int vsize, int vd, int rs, int imm5) {
    	float angle = gpr[rs];
    	boolean negativeSine = (imm5 & 0x10) != 0;
    	float sine = (float) Math.sin(angle * Math.PI / 2.0f);
    	if (negativeSine) {
    		sine = -sine;
    	}
    	float cosine = (float) Math.cos(angle * Math.PI / 2.0f);
    	float initValue = 0.0f;
    	if (((imm5 >> 2) & 3) == (imm5 & 3)) {
    		initValue = sine;
    	}
		for (int i = 0; i < vsize; ++i) {
			v3[i] = initValue;
		}
		v3[(imm5 >> 2) & 3] = sine;
		v3[imm5 & 3] = cosine;

        saveVd(vsize, vd, v3);
    }

    // group VLSU     
    // LSU:LVS
    public void doLVS(int vt, int rs, int simm14_a16) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        vpr[m][c][r] = Float.intBitsToFloat(memory.read32(gpr[rs] + simm14_a16));
    }

    // LSU:SVS
    public void doSVS(int vt, int rs, int simm14_a16) {
        int r = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        if (CHECK_ALIGNMENT) {
            int address = gpr[rs] + simm14_a16;
            if ((address & 3) != 0) {
                Memory.log.error(String.format("SV.S unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        memory.write32(gpr[rs] + simm14_a16, Float.floatToRawIntBits(vpr[m][r][c]));
    }

    // LSU:LVQ
    public void doLVQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 15) != 0) {
                Memory.log.error(String.format("LV.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        if ((vt & 32) != 0) {
            for (int i = 0; i < 4; ++i) {
                vpr[m][i][c] = Float.intBitsToFloat(memory.read32(address + i*4));
            }
        } else {
            for (int i = 0; i < 4; ++i) {
                vpr[m][c][i] = Float.intBitsToFloat(memory.read32(address + i*4));
            }
        }
    }

    // LSU:LVLQ
    public void doLVLQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;
        Memory.log.error("Forbidden LVL.Q");

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("LVL.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int offset = (address >> 2) & 3;
        int j = (3 ^ offset);

        if ((vt & 32) != 0) {
            for (int i = 0; i <= offset; ++i) {
                vpr[m][j + i][c] = Float.intBitsToFloat(memory.read32(address + i*4));
            }
        } else {
            for (int i = 0; i <= offset; ++i) {
                vpr[m][c][j + i] = Float.intBitsToFloat(memory.read32(address + i*4));
            }
        }
    }

    // LSU:LVRQ
    public void doLVRQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;
        Memory.log.error("Forbidden LVR.Q");

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("LVR.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int offset = (address >> 2) & 3;

        if ((vt & 32) != 0) {
            for (int i = offset; i < 4; ++i) {
                vpr[m][i][c] = Float.intBitsToFloat(memory.read32(address + (i - offset)*4));
            }
        } else {
            for (int i = offset; i < 4; ++i) {
                vpr[m][c][i] = Float.intBitsToFloat(memory.read32(address + (i - offset)*4));
            }
        }
    }
    // LSU:SVQ
    public void doSVQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 15) != 0) {
                Memory.log.error(String.format("SV.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        if ((vt & 32) != 0) {
            for (int i = 0; i < 4; ++i) {
                memory.write32((address + i*4), Float.floatToRawIntBits(vpr[m][i][c]));
            }
        } else {
            for (int i = 0; i < 4; ++i) {
                memory.write32((address + i*4), Float.floatToRawIntBits(vpr[m][c][i]));
            }
        }
    }

    // LSU:SVLQ
    public void doSVLQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("SVL.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int offset = address & 0x15;
        int j = (3 ^ offset);

        if ((vt & 32) != 0) {
            for (int i = 0; i <= offset; ++i) {
                memory.write32((address + i), Float.floatToRawIntBits(vpr[m][j + i][c]));
            }
        } else {
            for (int i = 0; i <= offset; ++i) {
                memory.write32((address + i), Float.floatToRawIntBits(vpr[m][c][j + i]));
            }
        }
    }

    // LSU:SVRQ
    public void doSVRQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int c = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("SVR.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int offset = address & 0x15;

        if ((vt & 32) != 0) {
            for (int i = offset; i < 4; ++i) {
                memory.write32((address + i - offset), Float.floatToRawIntBits(vpr[m][i][c]));
            }
        } else {
            for (int i = offset; i < 4; ++i) {
                memory.write32((address + i - offset), Float.floatToRawIntBits(vpr[m][c][i]));
            }
        }
    }
}


