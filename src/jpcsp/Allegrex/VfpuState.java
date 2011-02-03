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

import java.math.BigInteger;
import java.util.Arrays;

import jpcsp.Memory;

/**
 * Vectorial Floating Point Unit, handles scalar, vector and matrix operations.
 *
 * @author hli, gid15
 */
public class VfpuState extends FpuState {
    // We have a problem using Float.intBitsToFloat():
    // extract from the JDK 1.6 documentation:
    //    "...Consequently, for some int values,
    //     floatToRawIntBits(intBitsToFloat(start)) may not equal start.
    //     Moreover, which particular bit patterns represent signaling NaNs
    //     is platform dependent..."
	// Furthermore, it seems that the Java interpreter and the Java JIT compiler
	// produce different results.
    //
	// The PSP does not alter data when loading/saving values to/from VFPU registers.
    // Some applications are using sequences of lv.q/vwb.q to implement a memcpy(),
	// so it is important to keep all the bits unchanged while loading & storing
	// values from VFPU registers.
	// This is the reason why this implementation is keeping VFPU values into
	// a VfpuValue class keeping both int and float representations, instead
	// of just using float values.
	//
	// This has a performance impact on VFPU instructions but provides accuracy.
	// The compilerPerf.pbp application reports around 50% duration increase
	// for the execution of a vadd instruction.
	//

	//
    // Use a linear version of the vpr, storing the 8 x 2D-matrix
    // in a 1D-array. This is giving a better performance for the compiler.
    // For the interpreter, the methods
    //    getVpr(m, c, r)
    // and
    //    setVpr(m, c, r, value)
    // are getting a value and setting a value into the vpr array,
    // doing the mapping from the matrix index to the linear index.
    //
    //public float[][][] vpr; // [matrix][column][row]
    public final VfpuValue[] vpr = new VfpuValue[128];

    public static final class VfpuValue {
    	private int intValue;
    	private float floatValue;

    	public VfpuValue() {
    	}

    	public VfpuValue(VfpuValue that) {
    		floatValue = that.floatValue;
    		intValue = that.intValue;
    	}

    	public int getInt() {
    		return intValue;
    	}

    	public float getFloat() {
    		return floatValue;
    	}

    	public void setInt(int value) {
    		intValue = value;
    		floatValue = Float.intBitsToFloat(value);
    	}

    	public void setFloat(float value) {
    		floatValue = value;
    		intValue = Float.floatToRawIntBits(value);
    	}

    	public void reset() {
    		floatValue = 0;
    		intValue = 0;
    	}

    	public void copy(VfpuValue that) {
    		floatValue = that.floatValue;
    		intValue = that.intValue;
    	}
    }

    public static final float floatConstants[] = {
        0.0f,
        Float.MAX_VALUE,
        (float) Math.sqrt(2.0f),
        (float) Math.sqrt(0.5f),
        2.0f / (float) Math.sqrt(Math.PI),
        2.0f / (float) Math.PI,
        1.0f / (float) Math.PI,
        (float) Math.PI / 4.0f,
        (float) Math.PI / 2.0f,
        (float) Math.PI,
        (float) Math.E,
        (float) (Math.log(Math.E) / Math.log(2.0)), // log2(E) = log(E) / log(2)
        (float) Math.log10(Math.E),
        (float) Math.log(2.0),
        (float) Math.log(10.0),
        (float) Math.PI * 2.0f,
        (float) Math.PI / 6.0f,
        (float) Math.log10(2.0),
        (float) (Math.log(10.0) / Math.log(2.0)), // log2(10) = log(10) / log(2)
        (float) Math.sqrt(3.0) / 2.0f
    };

    private static class Random {
        private long seed;

        private final static long multiplier = 0x5DEECE66DL;
        private final static long addend = 0xBL;
        private final static long mask = (1L << 32) - 1;

        public Random() { this(0x3f800001); }
        
        public Random(int seed) {
            setSeed(seed);
        }

        public void setSeed(int seed) {
            this.seed = (seed) & mask;
        }

        public int getSeed() {
            return (int)seed;
        }       
        
        protected int next(int bits) {
            seed = (seed * multiplier + addend) & mask;
            return (int)(seed >>> (32 - bits));
        }

        public int nextInt() {
            return next(32);
        }

        public int nextInt(int n) {
            if (n <= 0)
                throw new IllegalArgumentException("n must be positive");

            if ((n & -n) == n)  // i.e., n is a power of 2
                return (int)((n * (long)next(31)) >> 31);

            int bits, val;
            do {
                bits = next(31);
                val = bits % n;
            } while (bits - val + (n-1) < 0);
            return val;
        }

        public float nextFloat() {
            return next(24) / ((float)(1 << 24));
        }
    }
    
    private static Random rnd;
    
    public static class Vcr {

        public static class PfxSrc /* $128, $129 */ {

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
            	System.arraycopy(that.swz, 0, swz, 0, swz.length);
            	System.arraycopy(that.abs, 0, abs, 0, abs.length);
            	System.arraycopy(that.cst, 0, cst, 0, cst.length);
            	System.arraycopy(that.neg, 0, neg, 0, neg.length);
                enabled = that.enabled;
            }

            public PfxSrc(PfxSrc that) {
                copy(that);
            }
        }
        public PfxSrc pfxs;
        public PfxSrc pfxt;

        public static class PfxDst /* 130 */ {

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
            	System.arraycopy(that.sat, 0, sat, 0, sat.length);
            	System.arraycopy(that.msk, 0, msk, 0, msk.length);
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
    	for (int i = 0; i < vpr.length; i++) {
    		vpr[i].reset();
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
        vcr = new Vcr();
        rnd = new Random();
        for (int i = 0; i < vpr.length; i++) {
        	vpr[i] = new VfpuValue();
        }
    }

    public void copy(VfpuState that) {
        super.copy(that);
        for (int i = 0; i < vpr.length; i++) {
        	vpr[i].copy(that.vpr[i]);
        }
        vcr.copy(that.vcr);
    }

    public VfpuState(VfpuState that) {
        super(that);
        for (int i = 0; i < vpr.length; i++) {
        	vpr[i] = new VfpuValue(that.vpr[i]);
        }
        vcr = new Vcr(that.vcr);
    }

    public static int getVprIndex(int m, int c, int r) {
        return (m << 4) + (c << 2) + r;
    }

    public float getVprFloat(int m, int c, int r) {
        return vpr[getVprIndex(m, c, r)].getFloat();
    }

    public int getVprInt(int m, int c, int r) {
        return vpr[getVprIndex(m, c, r)].getInt();
    }

    public void setVprFloat(int m, int c, int r, float value) {
        vpr[getVprIndex(m, c, r)].setFloat(value);
    }

    public void setVprInt(int m, int c, int r, int value) {
        vpr[getVprIndex(m, c, r)].setInt(value);
    }

    private static float[] v1 = new float[4];
    private static float[] v2 = new float[4];
    private static float[] v3 = new float[4];
    private static int[] v1i = new int[4];
    private static int[] v2i = new int[4];
    private static int[] v3i = new int[4];
    // VFPU stuff
    private float transformVr(int swz, boolean abs, boolean cst, boolean neg, float[] x) {
        float value = 0.0f;
        if (cst) {
            switch (swz) {
                case 0:
                    value = abs ? 3.0f : 0.0f;
                    break;
                case 1:
                    value = abs ? (1.0f / 3.0f) : 1.0f;
                    break;
                case 2:
                    value = abs ? (1.0f / 4.0f) : 2.0f;
                    break;
                case 3:
                    value = abs ? (1.0f / 6.0f) : 0.5f;
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

    private int transformVrInt(int swz, boolean abs, boolean cst, boolean neg, int[] x) {
    	if (!cst && !abs && !neg) {
    		return x[swz]; // Pure int value
    	}

    	float value = 0.0f;
        if (cst) {
            switch (swz) {
                case 0:
                    value = abs ? 3.0f : 0.0f;
                    break;
                case 1:
                    value = abs ? (1.0f / 3.0f) : 1.0f;
                    break;
                case 2:
                    value = abs ? (1.0f / 4.0f) : 2.0f;
                    break;
                case 3:
                    value = abs ? (1.0f / 6.0f) : 0.5f;
                    break;
            }
        } else {
            value = Float.intBitsToFloat(x[swz]);
        }

        if (abs) {
            value = Math.abs(value);
        }
        if (neg) {
        	value = 0.0f - value;
        }
        return Float.floatToRawIntBits(value);
    }

    private float applyPrefixVs(int i, float[] x) {
        return transformVr(vcr.pfxs.swz[i], vcr.pfxs.abs[i], vcr.pfxs.cst[i], vcr.pfxs.neg[i], x);
    }

    private int applyPrefixVsInt(int i, int[] x) {
        return transformVrInt(vcr.pfxs.swz[i], vcr.pfxs.abs[i], vcr.pfxs.cst[i], vcr.pfxs.neg[i], x);
    }

    private float applyPrefixVt(int i, float[] x) {
        return transformVr(vcr.pfxt.swz[i], vcr.pfxt.abs[i], vcr.pfxt.cst[i], vcr.pfxt.neg[i], x);
    }

    private int applyPrefixVtInt(int i, int[] x) {
        return transformVrInt(vcr.pfxt.swz[i], vcr.pfxt.abs[i], vcr.pfxt.cst[i], vcr.pfxt.neg[i], x);
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

    private int applyPrefixVdInt(int i, int value) {
        switch (vcr.pfxd.sat[i]) {
            case 1:
                return Float.floatToRawIntBits(Math.max(0.0f, Math.min(1.0f, Float.intBitsToFloat(value))));
            case 3:
                return Float.floatToRawIntBits(Math.max(-1.0f, Math.min(1.0f, Float.intBitsToFloat(value))));
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
                v1[0] = getVprFloat(m, i, s);
                if (vcr.pfxs.enabled) {
                    v1[0] = applyPrefixVs(0, v1);
                    vcr.pfxs.enabled = false;
                }
                break;

            case 2:
                s = (vs & 64) >> 5;
                if ((vs & 32) != 0) {
                    v1[0] = getVprFloat(m, s + 0, i);
                    v1[1] = getVprFloat(m, s + 1, i);
                } else {
                    v1[0] = getVprFloat(m, i, s + 0);
                    v1[1] = getVprFloat(m, i, s + 1);
                }
                if (vcr.pfxs.enabled) {
                    v3[0] = applyPrefixVs(0, v1);
                    v3[1] = applyPrefixVs(1, v1);
                    v1[0] = v3[0];
                    v1[1] = v3[1];
                    vcr.pfxs.enabled = false;
                }
                break;

            case 3:
                s = (vs & 64) >> 6;
                if ((vs & 32) != 0) {
                    v1[0] = getVprFloat(m, s + 0, i);
                    v1[1] = getVprFloat(m, s + 1, i);
                    v1[2] = getVprFloat(m, s + 2, i);
                } else {
                    v1[0] = getVprFloat(m, i, s + 0);
                    v1[1] = getVprFloat(m, i, s + 1);
                    v1[2] = getVprFloat(m, i, s + 2);
                }
                if (vcr.pfxs.enabled) {
                    v3[0] = applyPrefixVs(0, v1);
                    v3[1] = applyPrefixVs(1, v1);
                    v3[2] = applyPrefixVs(2, v1);
                    v1[0] = v3[0];
                    v1[1] = v3[1];
                    v1[2] = v3[2];
                    vcr.pfxs.enabled = false;
                }
                break;

            case 4:
                if ((vs & 32) != 0) {
                    v1[0] = getVprFloat(m, 0, i);
                    v1[1] = getVprFloat(m, 1, i);
                    v1[2] = getVprFloat(m, 2, i);
                    v1[3] = getVprFloat(m, 3, i);
                } else {
                    v1[0] = getVprFloat(m, i, 0);
                    v1[1] = getVprFloat(m, i, 1);
                    v1[2] = getVprFloat(m, i, 2);
                    v1[3] = getVprFloat(m, i, 3);
                }
                if (vcr.pfxs.enabled) {
                    v3[0] = applyPrefixVs(0, v1);
                    v3[1] = applyPrefixVs(1, v1);
                    v3[2] = applyPrefixVs(2, v1);
                    v3[3] = applyPrefixVs(3, v1);
                    v1[0] = v3[0];
                    v1[1] = v3[1];
                    v1[2] = v3[2];
                    v1[3] = v3[3];
                    vcr.pfxs.enabled = false;
                }
                break;

            default:
                break;
        }
    }

    public void loadVsInt(int vsize, int vs) {
        int m, s, i;

        m = (vs >> 2) & 7;
        i = (vs >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vs >> 5) & 3;
                v1i[0] = getVprInt(m, i, s);
                if (vcr.pfxs.enabled) {
                    v1i[0] = applyPrefixVsInt(0, v1i);
                    vcr.pfxs.enabled = false;
                }
                break;

            case 2:
                s = (vs & 64) >> 5;
                if ((vs & 32) != 0) {
                	v1i[0] = getVprInt(m, s + 0, i);
                	v1i[1] = getVprInt(m, s + 1, i);
                } else {
                	v1i[0] = getVprInt(m, i, s + 0);
                	v1i[1] = getVprInt(m, i, s + 1);
                }
                if (vcr.pfxs.enabled) {
                    v3i[0] = applyPrefixVsInt(0, v1i);
                    v3i[1] = applyPrefixVsInt(1, v1i);
                    v1i[0] = v3i[0];
                    v1i[1] = v3i[1];
                    vcr.pfxs.enabled = false;
                }
                break;

            case 3:
                s = (vs & 64) >> 6;
                if ((vs & 32) != 0) {
                	v1i[0] = getVprInt(m, s + 0, i);
                	v1i[1] = getVprInt(m, s + 1, i);
                	v1i[2] = getVprInt(m, s + 2, i);
                } else {
                	v1i[0] = getVprInt(m, i, s + 0);
                	v1i[1] = getVprInt(m, i, s + 1);
                	v1i[2] = getVprInt(m, i, s + 2);
                }
                if (vcr.pfxs.enabled) {
                    v3i[0] = applyPrefixVsInt(0, v1i);
                    v3i[1] = applyPrefixVsInt(1, v1i);
                    v3i[2] = applyPrefixVsInt(2, v1i);
                    v1i[0] = v3i[0];
                    v1i[1] = v3i[1];
                    v1i[2] = v3i[2];
                    vcr.pfxs.enabled = false;
                }
                break;

            case 4:
                if ((vs & 32) != 0) {
                	v1i[0] = getVprInt(m, 0, i);
                	v1i[1] = getVprInt(m, 1, i);
                	v1i[2] = getVprInt(m, 2, i);
                	v1i[3] = getVprInt(m, 3, i);
                } else {
                	v1i[0] = getVprInt(m, i, 0);
                	v1i[1] = getVprInt(m, i, 1);
                	v1i[2] = getVprInt(m, i, 2);
                	v1i[3] = getVprInt(m, i, 3);
                }
                if (vcr.pfxs.enabled) {
                    v3i[0] = applyPrefixVsInt(0, v1i);
                    v3i[1] = applyPrefixVsInt(1, v1i);
                    v3i[2] = applyPrefixVsInt(2, v1i);
                    v3i[3] = applyPrefixVsInt(3, v1i);
                    v1i[0] = v3i[0];
                    v1i[1] = v3i[1];
                    v1i[2] = v3i[2];
                    v1i[3] = v3i[3];
                    vcr.pfxs.enabled = false;
                }
                break;

            default:
                break;
        }
    }

    public void loadVt(int vsize, int vt) {
        int m, s, i;

        m = (vt >> 2) & 7;
        i = (vt >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vt >> 5) & 3;
                v2[0] = getVprFloat(m, i, s);
                if (vcr.pfxt.enabled) {
                    v2[0] = applyPrefixVt(0, v2);
                    vcr.pfxt.enabled = false;
                }
                break;

            case 2:
                s = (vt & 64) >> 5;
                if ((vt & 32) != 0) {
                    v2[0] = getVprFloat(m, s + 0, i);
                    v2[1] = getVprFloat(m, s + 1, i);
                } else {
                    v2[0] = getVprFloat(m, i, s + 0);
                    v2[1] = getVprFloat(m, i, s + 1);
                }
                if (vcr.pfxt.enabled) {
                    v3[0] = applyPrefixVt(0, v2);
                    v3[1] = applyPrefixVt(1, v2);
                    v2[0] = v3[0];
                    v2[1] = v3[1];
                    vcr.pfxt.enabled = false;
                }
                break;

            case 3:
                s = (vt & 64) >> 6;
                if ((vt & 32) != 0) {
                    v2[0] = getVprFloat(m, s + 0, i);
                    v2[1] = getVprFloat(m, s + 1, i);
                    v2[2] = getVprFloat(m, s + 2, i);
                } else {
                    v2[0] = getVprFloat(m, i, s + 0);
                    v2[1] = getVprFloat(m, i, s + 1);
                    v2[2] = getVprFloat(m, i, s + 2);
                }
                if (vcr.pfxt.enabled) {
                    v3[0] = applyPrefixVt(0, v2);
                    v3[1] = applyPrefixVt(1, v2);
                    v3[2] = applyPrefixVt(2, v2);
                    v2[0] = v3[0];
                    v2[1] = v3[1];
                    v2[2] = v3[2];
                    vcr.pfxt.enabled = false;
                }
                break;

            case 4:
                if ((vt & 32) != 0) {
                    v2[0] = getVprFloat(m, 0, i);
                    v2[1] = getVprFloat(m, 1, i);
                    v2[2] = getVprFloat(m, 2, i);
                    v2[3] = getVprFloat(m, 3, i);
                } else {
                    v2[0] = getVprFloat(m, i, 0);
                    v2[1] = getVprFloat(m, i, 1);
                    v2[2] = getVprFloat(m, i, 2);
                    v2[3] = getVprFloat(m, i, 3);
                }
                if (vcr.pfxt.enabled) {
                    v3[0] = applyPrefixVt(0, v2);
                    v3[1] = applyPrefixVt(1, v2);
                    v3[2] = applyPrefixVt(2, v2);
                    v3[3] = applyPrefixVt(3, v2);
                    v2[0] = v3[0];
                    v2[1] = v3[1];
                    v2[2] = v3[2];
                    v2[3] = v3[3];
                    vcr.pfxt.enabled = false;
                }
                break;

            default:
                break;
        }
    }

    public void loadVtInt(int vsize, int vt) {
        int m, s, i;

        m = (vt >> 2) & 7;
        i = (vt >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vt >> 5) & 3;
                v2i[0] = getVprInt(m, i, s);
                if (vcr.pfxt.enabled) {
                    v2i[0] = applyPrefixVtInt(0, v2i);
                    vcr.pfxt.enabled = false;
                }
                break;

            case 2:
                s = (vt & 64) >> 5;
                if ((vt & 32) != 0) {
                	v2i[0] = getVprInt(m, s + 0, i);
                	v2i[1] = getVprInt(m, s + 1, i);
                } else {
                	v2i[0] = getVprInt(m, i, s + 0);
                	v2i[1] = getVprInt(m, i, s + 1);
                }
                if (vcr.pfxt.enabled) {
                    v3i[0] = applyPrefixVtInt(0, v2i);
                    v3i[1] = applyPrefixVtInt(1, v2i);
                    v2i[0] = v3i[0];
                    v2i[1] = v3i[1];
                    vcr.pfxt.enabled = false;
                }
                break;

            case 3:
                s = (vt & 64) >> 6;
                if ((vt & 32) != 0) {
                	v2i[0] = getVprInt(m, s + 0, i);
                	v2i[1] = getVprInt(m, s + 1, i);
                	v2i[2] = getVprInt(m, s + 2, i);
                } else {
                	v2i[0] = getVprInt(m, i, s + 0);
                	v2i[1] = getVprInt(m, i, s + 1);
                	v2i[2] = getVprInt(m, i, s + 2);
                }
                if (vcr.pfxt.enabled) {
                    v3i[0] = applyPrefixVtInt(0, v2i);
                    v3i[1] = applyPrefixVtInt(1, v2i);
                    v3i[2] = applyPrefixVtInt(2, v2i);
                    v2i[0] = v3i[0];
                    v2i[1] = v3i[1];
                    v2i[2] = v3i[2];
                    vcr.pfxt.enabled = false;
                }
                break;

            case 4:
                if ((vt & 32) != 0) {
                    v2i[0] = getVprInt(m, 0, i);
                    v2i[1] = getVprInt(m, 1, i);
                    v2i[2] = getVprInt(m, 2, i);
                    v2i[3] = getVprInt(m, 3, i);
                } else {
                    v2i[0] = getVprInt(m, i, 0);
                    v2i[1] = getVprInt(m, i, 1);
                    v2i[2] = getVprInt(m, i, 2);
                    v2i[3] = getVprInt(m, i, 3);
                }
                if (vcr.pfxt.enabled) {
                    v3i[0] = applyPrefixVtInt(0, v2i);
                    v3i[1] = applyPrefixVtInt(1, v2i);
                    v3i[2] = applyPrefixVtInt(2, v2i);
                    v3i[3] = applyPrefixVtInt(3, v2i);
                    v2i[0] = v3i[0];
                    v2i[1] = v3i[1];
                    v2i[2] = v3i[2];
                    v2i[3] = v3i[3];
                    vcr.pfxt.enabled = false;
                }
                break;

            default:
                break;
        }
    }

    public void loadVd(int vsize, int vd) {
        int m, s, i;

        m = (vd >> 2) & 7;
        i = (vd >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vd >> 5) & 3;
                v3[0] = getVprFloat(m, i, s);
                break;

            case 2:
                s = (vd & 64) >> 5;
                if ((vd & 32) != 0) {
                	v3[0] = getVprFloat(m, s + 0, i);
                	v3[1] = getVprFloat(m, s + 1, i);
                } else {
                	v3[0] = getVprFloat(m, i, s + 0);
                	v3[1] = getVprFloat(m, i, s + 1);
                }
                break;

            case 3:
                s = (vd & 64) >> 6;
                if ((vd & 32) != 0) {
                	v3[0] = getVprFloat(m, s + 0, i);
                	v3[1] = getVprFloat(m, s + 1, i);
                	v3[2] = getVprFloat(m, s + 2, i);
                } else {
                	v3[0] = getVprFloat(m, i, s + 0);
                	v3[1] = getVprFloat(m, i, s + 1);
                	v3[2] = getVprFloat(m, i, s + 2);
                }
                break;

            case 4:
                if ((vd & 32) != 0) {
                	v3[0] = getVprFloat(m, 0, i);
                	v3[1] = getVprFloat(m, 1, i);
                	v3[2] = getVprFloat(m, 2, i);
                	v3[3] = getVprFloat(m, 3, i);
                } else {
                	v3[0] = getVprFloat(m, i, 0);
                	v3[1] = getVprFloat(m, i, 1);
                	v3[2] = getVprFloat(m, i, 2);
                	v3[3] = getVprFloat(m, i, 3);
                }
                break;

            default:
                break;
        }
    }

    public void loadVdInt(int vsize, int vd) {
        int m, s, i;

        m = (vd >> 2) & 7;
        i = (vd >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vd >> 5) & 3;
                v3i[0] = getVprInt(m, i, s);
                break;

            case 2:
                s = (vd & 64) >> 5;
                if ((vd & 32) != 0) {
                	v3i[0] = getVprInt(m, s + 0, i);
                	v3i[1] = getVprInt(m, s + 1, i);
                } else {
                	v3i[0] = getVprInt(m, i, s + 0);
                	v3i[1] = getVprInt(m, i, s + 1);
                }
                break;

            case 3:
                s = (vd & 64) >> 6;
                if ((vd & 32) != 0) {
                	v3i[0] = getVprInt(m, s + 0, i);
                	v3i[1] = getVprInt(m, s + 1, i);
                	v3i[2] = getVprInt(m, s + 2, i);
                } else {
                	v3i[0] = getVprInt(m, i, s + 0);
                	v3i[1] = getVprInt(m, i, s + 1);
                	v3i[2] = getVprInt(m, i, s + 2);
                }
                break;

            case 4:
                if ((vd & 32) != 0) {
                	v3i[0] = getVprInt(m, 0, i);
                	v3i[1] = getVprInt(m, 1, i);
                	v3i[2] = getVprInt(m, 2, i);
                	v3i[3] = getVprInt(m, 3, i);
                } else {
                	v3i[0] = getVprInt(m, i, 0);
                	v3i[1] = getVprInt(m, i, 1);
                	v3i[2] = getVprInt(m, i, 2);
                	v3i[3] = getVprInt(m, i, 3);
                }
                break;

            default:
            	break;
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
                        setVprFloat(m, i, s, applyPrefixVd(0, vr[0]));
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    setVprFloat(m, i, s, vr[0]);
                }
                break;

            case 2:
                s = (vd & 64) >> 5;
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 2; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprFloat(m, s + j, i, applyPrefixVd(j, vr[j]));
                            }
                        }
                    } else {
                        for (int j = 0; j < 2; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprFloat(m, i, s + j, applyPrefixVd(j, vr[j]));
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 2; ++j) {
                            setVprFloat(m, s + j, i, vr[j]);
                        }
                    } else {
                        for (int j = 0; j < 2; ++j) {
                            setVprFloat(m, i, s + j, vr[j]);
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
                                setVprFloat(m, s + j, i, applyPrefixVd(j, vr[j]));
                            }
                        }
                    } else {
                        for (int j = 0; j < 3; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprFloat(m, i, s + j, applyPrefixVd(j, vr[j]));
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 3; ++j) {
                            setVprFloat(m, s + j, i, vr[j]);
                        }
                    } else {
                        for (int j = 0; j < 3; ++j) {
                            setVprFloat(m, i, s + j, vr[j]);
                        }
                    }
                }
                break;

            case 4:
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 4; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprFloat(m, j, i, applyPrefixVd(j, vr[j]));
                            }
                        }
                    } else {
                        for (int j = 0; j < 4; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprFloat(m, i, j, applyPrefixVd(j, vr[j]));
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 4; ++j) {
                            setVprFloat(m, j, i, vr[j]);
                        }
                    } else {
                        for (int j = 0; j < 4; ++j) {
                            setVprFloat(m, i, j, vr[j]);
                        }
                    }
                }
                break;

            default:
                break;
        }
    }
    
    public void saveVdInt(int vsize, int vd, int[] vr) {
        int m, s, i;

        m = (vd >> 2) & 7;
        i = (vd >> 0) & 3;

        switch (vsize) {
            case 1:
                s = (vd >> 5) & 3;
                if (vcr.pfxd.enabled) {
                    if (!vcr.pfxd.msk[0]) {
                        setVprInt(m, i, s, applyPrefixVdInt(0, vr[0]));
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    setVprInt(m, i, s, vr[0]);
                }
                break;

            case 2:
                s = (vd & 64) >> 5;
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 2; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprInt(m, s + j, i, applyPrefixVdInt(j, vr[j]));
                            }
                        }
                    } else {
                        for (int j = 0; j < 2; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprInt(m, i, s + j, applyPrefixVdInt(j, vr[j]));
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 2; ++j) {
                            setVprInt(m, s + j, i, vr[j]);
                        }
                    } else {
                        for (int j = 0; j < 2; ++j) {
                            setVprInt(m, i, s + j, vr[j]);
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
                                setVprInt(m, s + j, i, applyPrefixVdInt(j, vr[j]));
                            }
                        }
                    } else {
                        for (int j = 0; j < 3; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                                setVprInt(m, i, s + j, applyPrefixVdInt(j, vr[j]));
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 3; ++j) {
                            setVprInt(m, s + j, i, vr[j]);
                        }
                    } else {
                        for (int j = 0; j < 3; ++j) {
                        	setVprInt(m, i, s + j, vr[j]);
                        }
                    }
                }
                break;

            case 4:
                if (vcr.pfxd.enabled) {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 4; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                            	setVprInt(m, j, i, applyPrefixVdInt(j, vr[j]));
                            }
                        }
                    } else {
                        for (int j = 0; j < 4; ++j) {
                            if (!vcr.pfxd.msk[j]) {
                            	setVprInt(m, i, j, applyPrefixVdInt(j, vr[j]));
                            }
                        }
                    }
                    vcr.pfxd.enabled = false;
                } else {
                    if ((vd & 32) != 0) {
                        for (int j = 0; j < 4; ++j) {
                        	setVprInt(m, j, i, vr[j]);
                        }
                    } else {
                        for (int j = 0; j < 4; ++j) {
                        	setVprInt(m, i, j, vr[j]);
                        }
                    }
                }
                break;

            default:
                break;
        }
    }

    float halffloatToFloat(int imm16) {   
        int s = (imm16 >> 15) & 0x00000001; // sign
        int e = (imm16 >> 10) & 0x0000001f; // exponent
        int f = (imm16 >>  0) & 0x000003ff; // fraction

        // need to handle 0x7C00 INF and 0xFC00 -INF?
        if (e == 0) {
            // need to handle +-0 case f==0 or f=0x8000?
            if (f == 0) {
                // Plus or minus zero
                return Float.intBitsToFloat(s << 31);                
            }
			// Denormalized number -- renormalize it
			while ((f & 0x00000400) == 0) {
			    f <<= 1;
			    e -=  1;
			}
			e += 1;
			f &= ~0x00000400;
        } else if (e == 31) {
            if (f == 0) {
                // Inf
                return Float.intBitsToFloat((s << 31) | 0x7f800000);
            }
			// NaN
			return Float.intBitsToFloat((s << 31) | 0x7f800000 | (f << 13));
        }

        e = e + (127 - 15);
        f = f << 13;
       
        return Float.intBitsToFloat((s << 31) | (e << 23) | f);
    }
    
    int floatToHalffloat(float v) {
        int i = Float.floatToRawIntBits(v);
        int s = ((i >> 16) & 0x00008000);              // sign
        int e = ((i >> 23) & 0x000000ff) - (127 - 15); // exponent
        int f = ((i >>  0) & 0x007fffff);              // fraction

        // need to handle NaNs and Inf?
        if (e <= 0) {
            if (e < -10) {
                if (s != 0) {
                    // handle -0.0
                    return 0x8000;
                }
                return 0;
            }
            f = (f | 0x00800000) >> (1 - e);
            return s | (f >> 13);
        } else if (e == 0xff - (127 - 15)) {
            if (f == 0) {
                // Inf
                return s | 0x7c00;
            }
            // NAN
            f >>= 13;
            return s | 0x7c00 | f | ((f == 0) ? 1 : 0);
        }
        if (e > 30) {
            // Overflow
            return s | 0x7c00;
        }
        return s | (e << 10) | (f >> 13);
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
        loadVtInt(1, vt);

        v1[0] = Math.scalb(v1[0], v2i[0]);

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
        loadVt(1, vt);

        float scale = v2[0];

        for (int i = 0; i < vsize; ++i) {
            v1[i] *= scale;
        }

        saveVd(vsize, vd, v1);
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

		// Tested: last element is only v2[i] (and not v1[i]*v2[i])
        v2[0] = hdp + v2[i];

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
            return;
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

        gpr[rt] = getVprInt(m, c, r);
    }
    // VFPU2:MFVC
    public void doMFVC(int rt, int imm7) {
    	if (rt != 0) {
            int value = 0;
            switch (imm7) {
                case 0: /* 128 */
                    value |= vcr.pfxs.swz[0] << 0;
                    value |= vcr.pfxs.swz[1] << 2;
                    value |= vcr.pfxs.swz[2] << 4;
                    value |= vcr.pfxs.swz[3] << 6;
                    if (vcr.pfxs.abs[0]) value |=  1 <<  8;
                    if (vcr.pfxs.abs[1]) value |=  1 <<  9;
                    if (vcr.pfxs.abs[2]) value |=  1 << 10;
                    if (vcr.pfxs.abs[3]) value |=  1 << 11;
                    if (vcr.pfxs.cst[0]) value |=  1 << 12;
                    if (vcr.pfxs.cst[1]) value |=  1 << 13;
                    if (vcr.pfxs.cst[2]) value |=  1 << 14;
                    if (vcr.pfxs.cst[3]) value |=  1 << 15;
                    if (vcr.pfxs.neg[0]) value |=  1 << 16;
                    if (vcr.pfxs.neg[1]) value |=  1 << 17;
                    if (vcr.pfxs.neg[2]) value |=  1 << 18;
                    if (vcr.pfxs.neg[3]) value |=  1 << 19;
                    gpr[rt] = value;
                    break;
                case 1: /* 129 */
                    value |= vcr.pfxt.swz[0] << 0;
                    value |= vcr.pfxt.swz[1] << 2;
                    value |= vcr.pfxt.swz[2] << 4;
                    value |= vcr.pfxt.swz[3] << 6;
                    if (vcr.pfxt.abs[0]) value |=  1 <<  8;
                    if (vcr.pfxt.abs[1]) value |=  1 <<  9;
                    if (vcr.pfxt.abs[2]) value |=  1 << 10;
                    if (vcr.pfxt.abs[3]) value |=  1 << 11;
                    if (vcr.pfxt.cst[0]) value |=  1 << 12;
                    if (vcr.pfxt.cst[1]) value |=  1 << 13;
                    if (vcr.pfxt.cst[2]) value |=  1 << 14;
                    if (vcr.pfxt.cst[3]) value |=  1 << 15;
                    if (vcr.pfxt.neg[0]) value |=  1 << 16;
                    if (vcr.pfxt.neg[1]) value |=  1 << 17;
                    if (vcr.pfxt.neg[2]) value |=  1 << 18;
                    if (vcr.pfxt.neg[3]) value |=  1 << 19;
                    gpr[rt] = value;
                    break;
                case 2: /* 130 */
                    value |= vcr.pfxd.sat[0] << 0;
                    value |= vcr.pfxd.sat[1] << 2;
                    value |= vcr.pfxd.sat[2] << 4;
                    value |= vcr.pfxd.sat[3] << 6;
                    if (vcr.pfxd.msk[0]) value |=  1 <<  8;
                    if (vcr.pfxd.msk[1]) value |=  1 <<  9;
                    if (vcr.pfxd.msk[2]) value |=  1 << 10;
                    if (vcr.pfxd.msk[3]) value |=  1 << 11;
                    gpr[rt] = value;
                    break;
                case 3: /* 131 */
                    for (int i = vcr.cc.length - 1; i >= 0; i--) {
                        value <<= 1;
                        if (vcr.cc[i]) {
                            value |= 1;
                        }
                    }
                    gpr[rt] = value;
                    break;
                case 8: /* 136 - RCX0 */
                    gpr[rt] = rnd.getSeed();
                    break;
                case 9:  /* 137 - RCX1 */
                case 10: /* 138 - RCX2 */
                case 11: /* 139 - RCX3 */
                case 12: /* 140 - RCX4 */
                case 13: /* 141 - RCX5 */
                case 14: /* 142 - RCX6 */
                case 15: /* 143 - RCX7 */
                    // as we do not know how VFPU generates a random number through those 8 registers, we ignore 7 of them
                    gpr[rt] = 0x3f800000;
                    break;
                default:
                    // These values are not supported in Jpcsp
                    doUNK("Unimplemented MFVC (rt=" + rt + ", imm7=" + imm7 + ")");
                    break;
            }
    	}
    }
    // VFPU2:MTV
    public void doMTV(int rt, int imm7) {
        int r = (imm7 >> 5) & 3;
        int m = (imm7 >> 2) & 7;
        int c = (imm7 >> 0) & 3;

        setVprInt(m, c, r, gpr[rt]);
    }

    // VFPU2:MTVC
    public void doMTVC(int rt, int imm7) {
        int value = gpr[rt]; 
        
        switch (imm7) {
            case 0: /* 128 */
                vcr.pfxs.swz[0] = ((value >> 0 ) & 3);
                vcr.pfxs.swz[1] = ((value >> 2 ) & 3);
                vcr.pfxs.swz[2] = ((value >> 4 ) & 3);
                vcr.pfxs.swz[3] = ((value >> 6 ) & 3);
                vcr.pfxs.abs[0] = ((value >> 8 ) & 1) == 1;
                vcr.pfxs.abs[1] = ((value >> 9 ) & 1) == 1;
                vcr.pfxs.abs[2] = ((value >> 10) & 1) == 1;
                vcr.pfxs.abs[3] = ((value >> 11) & 1) == 1;
                vcr.pfxs.cst[0] = ((value >> 12) & 1) == 1;
                vcr.pfxs.cst[1] = ((value >> 13) & 1) == 1;
                vcr.pfxs.cst[2] = ((value >> 14) & 1) == 1;
                vcr.pfxs.cst[3] = ((value >> 15) & 1) == 1;
                vcr.pfxs.neg[0] = ((value >> 16) & 1) == 1;
                vcr.pfxs.neg[1] = ((value >> 17) & 1) == 1;
                vcr.pfxs.neg[2] = ((value >> 18) & 1) == 1;
                vcr.pfxs.neg[3] = ((value >> 19) & 1) == 1;
                vcr.pfxs.enabled = true;
                break;               
            case 1: /* 129 */
                vcr.pfxt.swz[0] = ((value >> 0 ) & 3);
                vcr.pfxt.swz[1] = ((value >> 2 ) & 3);
                vcr.pfxt.swz[2] = ((value >> 4 ) & 3);
                vcr.pfxt.swz[3] = ((value >> 6 ) & 3);
                vcr.pfxt.abs[0] = ((value >> 8 ) & 1) == 1;
                vcr.pfxt.abs[1] = ((value >> 9 ) & 1) == 1;
                vcr.pfxt.abs[2] = ((value >> 10) & 1) == 1;
                vcr.pfxt.abs[3] = ((value >> 11) & 1) == 1;
                vcr.pfxt.cst[0] = ((value >> 12) & 1) == 1;
                vcr.pfxt.cst[1] = ((value >> 13) & 1) == 1;
                vcr.pfxt.cst[2] = ((value >> 14) & 1) == 1;
                vcr.pfxt.cst[3] = ((value >> 15) & 1) == 1;
                vcr.pfxt.neg[0] = ((value >> 16) & 1) == 1;
                vcr.pfxt.neg[1] = ((value >> 17) & 1) == 1;
                vcr.pfxt.neg[2] = ((value >> 18) & 1) == 1;
                vcr.pfxt.neg[3] = ((value >> 19) & 1) == 1;
                vcr.pfxt.enabled = true;
                break;
            case 2: /* 130 */
                vcr.pfxd.sat[0] = ((value >> 0 ) & 3);
                vcr.pfxd.sat[1] = ((value >> 2 ) & 3);
                vcr.pfxd.sat[2] = ((value >> 4 ) & 3);
                vcr.pfxd.sat[3] = ((value >> 6 ) & 3);
                vcr.pfxd.msk[0] = ((value >> 8 ) & 1) == 1;
                vcr.pfxd.msk[1] = ((value >> 9 ) & 1) == 1;
                vcr.pfxd.msk[2] = ((value >> 10) & 1) == 1;
                vcr.pfxd.msk[3] = ((value >> 11) & 1) == 1;
                vcr.pfxd.enabled = true;
                break;
            case 3: /* 131 */
                for (int i = 0; i < vcr.cc.length; i++) {
                    vcr.cc[i] = (value & 1) != 0;
                    value >>>= 1;
                }
                break;
            case 8: /* 136 - RCX0 */
                rnd.setSeed(value);
                break;
            case 9:  /* 137 - RCX1 */
            case 10: /* 138 - RCX2 */
            case 11: /* 139 - RCX3 */
            case 12: /* 140 - RCX4 */
            case 13: /* 141 - RCX5 */
            case 14: /* 142 - RCX6 */
            case 15: /* 143 - RCX7 */
                // as we do not know how VFPU generates a random number through those 8 registers, we ignore 7 of them
                break;
            default:
                // These values are not supported in Jpcsp
                doUNK("Unimplemented MTVC (rt=" + rt + ", imm7=" + imm7 + ", value=0x" + Integer.toHexString(value) + ")");
                break;
        }
    }

    // VFPU2:BVF
    public boolean doBVF(int imm3, int simm16) {
        npc = (!vcr.cc[imm3]) ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }
    // VFPU2:BVT
    public boolean doBVT(int imm3, int simm16) {
        npc = (vcr.cc[imm3]) ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }
    // VFPU2:BVFL
    public boolean doBVFL(int imm3, int simm16) {
    	if (!vcr.cc[imm3]) {
    		npc = branchTarget(pc, simm16);
    		return true;
    	}
		pc = pc + 4;
        return false;
    }
    // VFPU2:BVTL
    public boolean doBVTL(int imm3, int simm16) {
    	if (vcr.cc[imm3]) {
    		npc = branchTarget(pc, simm16);
    		return true;
    	}
		pc = pc + 4;
        return false;
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
        loadVsInt(vsize, vs);
        saveVdInt(vsize, vd, v1i);
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
            v3[i] = (float) Math.sin(0.5 * Math.PI * v1[i]);
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VCOS
    public void doVCOS(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (float) Math.cos(0.5 * Math.PI * v1[i]);
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
            v3[i] = (float) (Math.asin(v1[i]) * 2.0 / Math.PI);
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
            v3[i] = 0.0f - (float) Math.sin(0.5 * Math.PI * v1[i]);
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
        // temporary solution
        if (vsize != 1) {
            doUNK("Only supported VRNDS.S");
            return;
        }
        
        loadVsInt(1, vs);
        rnd.setSeed(v1i[0]);
    }
    // VFPU4:VRNDI
    public void doVRNDI(int vsize, int vd) {
        // temporary solution
        for (int i = 0; i < vsize; ++i) {
            v3i[i] = rnd.nextInt();
        }
        saveVdInt(vsize, vd, v3i);
    }
    // VFPU4:VRNDF1
    public void doVRNDF1(int vsize, int vd) {
        // temporary solution
        for (int i = 0; i < vsize; ++i) {
            v3[i] = 1.0f + rnd.nextFloat();
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VRNDF2
    public void doVRNDF2(int vsize, int vd) {
        // temporary solution
        for (int i = 0; i < vsize; ++i) {
            v3[i] = (1.0f + rnd.nextFloat())*2.0f;
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VF2H
    public void doVF2H(int vsize, int vd, int vs) {
        if ((vsize & 1) == 1) {
            doUNK("Only supported VF2H.P or VF2H.Q");
            return;
        }
        loadVs(vsize, vs);
        for (int i = 0; i < vsize/2; ++i) {
            v3[i] = (floatToHalffloat(v1[1+i*2])<<16)|
                    (floatToHalffloat(v1[0+i*2])<< 0);
        }
        saveVd(vsize/2, vd, v3);
    }
    // VFPU4:VH2F
    public void doVH2F(int vsize, int vd, int vs) {
        if (vsize > 2) {
            doUNK("Only supported VH2F.S or VH2F.P");
            return;
        }
        loadVsInt(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            int imm32 = v1i[i];
            v3[0+2*i] = halffloatToFloat(imm32 & 65535);
            v3[1+2*i] = halffloatToFloat(imm32 >>> 16);
        }
        saveVd(vsize*2, vd, v3);
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
        if (vsize != 1) {
            doUNK("Only supported VUC2I.S");
            return;
        }
        loadVsInt(1, vs);
        int n = v1i[0];
        // Performs pseudo-full-scale conversion
        v3i[0] = (((n      ) & 0xFF) * 0x01010101) >>> 1;
        v3i[1] = (((n >>  8) & 0xFF) * 0x01010101) >>> 1;
        v3i[2] = (((n >> 16) & 0xFF) * 0x01010101) >>> 1;
        v3i[3] = (((n >> 24) & 0xFF) * 0x01010101) >>> 1;
        saveVdInt(4, vd, v3i);
    }
    // VFPU4:VC2I
    public void doVC2I(int vsize, int vd, int vs) {
        if (vsize != 1) {
            doUNK("Only supported VC2I.S");
            return;
        }
        loadVsInt(1, vs);
        int n = v1i[0];
        v3i[0] = ((n      ) & 0xFF) << 24;
        v3i[1] = ((n >>  8) & 0xFF) << 24;
        v3i[2] = ((n >> 16) & 0xFF) << 24;
        v3i[3] = ((n >> 24) & 0xFF) << 24;
        saveVdInt(4, vd, v3i);
    }
    // VFPU4:VUS2I
    public void doVUS2I(int vsize, int vd, int vs) {
        if (vsize > 2) {
            doUNK("Only supported VUS2I.S or VUS2I.P");
            return;
        }
    	loadVsInt(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            int imm32 = v1i[i];
            v3i[0+2*i] = ((imm32       ) & 0xFFFF) << 15;
            v3i[1+2*i] = ((imm32 >>> 16) & 0xFFFF) << 15;
        }
    	saveVdInt(vsize * 2, vd, v3i);
    }
    // VFPU4:VS2I
    public void doVS2I(int vsize, int vd, int vs) {
        if (vsize > 2) {
            doUNK("Only supported VS2I.S or VS2I.P");
            return;
        }
    	loadVsInt(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
            int imm32 = v1i[i];
            v3i[0+2*i] = ((imm32       ) & 0xFFFF) << 16;
            v3i[1+2*i] = ((imm32 >>> 16) & 0xFFFF) << 16;
        }
    	saveVdInt(vsize * 2, vd, v3i);
    }

    // VFPU4:VI2UC
    public void doVI2UC(int vsize, int vd, int vs) {
        if (vsize != 4) {
            doUNK("Only supported VI2UC.Q");
            return;
        }

        loadVsInt(4, vs);

        int x = v1i[0];
        int y = v1i[1];
        int z = v1i[2];
        int w = v1i[3];

        v3i[0] = ((x < 0) ? 0 : ((x >> 23) << 0 )) |
                 ((y < 0) ? 0 : ((y >> 23) << 8 )) |
                 ((z < 0) ? 0 : ((z >> 23) << 16)) |
                 ((w < 0) ? 0 : ((w >> 23) << 24));

        saveVdInt(1, vd, v3i);
    }

    // VFPU4:VI2C
    public void doVI2C(int vsize, int vd, int vs) {
        if (vsize != 4) {
            doUNK("Only supported VI2C.Q");
            return;
        }

        loadVsInt(4, vs);

        int x = v1i[0];
        int y = v1i[1];
        int z = v1i[2];
        int w = v1i[3];

        v3i[0] = ((x >>> 24) << 0 ) |
                 ((y >>> 24) << 8 ) |
                 ((z >>> 24) << 16) |
                 ((w >>> 24) << 24);

        saveVdInt(1, vd, v3i);
    }
    // VFPU4:VI2US
    public void doVI2US(int vsize, int vd, int vs) {
        if ((vsize & 1) != 0) {
            doUNK("Only supported VI2US.P and VI2US.Q");
            return;
        }

        loadVsInt(vsize, vs);

        int x = v1i[0];
        int y = v1i[1];

        v3i[0] = ((x < 0) ? 0 : ((x >> 15) << 0 )) |
                 ((y < 0) ? 0 : ((y >> 15) << 16));

        if (vsize == 4) {
            int z = v1i[2];
            int w = v1i[3];

            v3i[1] = ((z < 0) ? 0 : ((z >> 15) << 0 )) |
                     ((w < 0) ? 0 : ((w >> 15) << 16));
            saveVdInt(2, vd, v3i);
        } else {
            saveVdInt(1, vd, v3i);
        }
    }
    // VFPU4:VI2S
    public void doVI2S(int vsize, int vd, int vs) {
        if ((vsize & 1) != 0) {
            doUNK("Only supported VI2S.P and VI2S.Q");
            return;
        }

        loadVsInt(vsize, vs);

        int x = v1i[0];
        int y = v1i[1];

        v3i[0] = ((x >>> 16) << 0 ) |
                 ((y >>> 16) << 16);

        if (vsize == 4) {
            int z = v1i[2];
            int w = v1i[3];

            v3i[1] = ((z >>> 16) << 0 ) |
                     ((w >>> 16) << 16);
            saveVdInt(2, vd, v3i);
        } else {
            saveVdInt(1, vd, v3i);
        }
    }
    // VFPU4:VSRT1
    public void doVSRT1(int vsize, int vd, int vs) {
        if (vsize != 4) {
            doUNK("Only supported VSRT1.Q");
            return;
        }

        loadVs(4, vs);
        float x = v1[0];
        float y = v1[1];
        float z = v1[2];
        float w = v1[3];
        v3[0] = Math.min(x, y);
        v3[1] = Math.max(x, y);
        v3[2] = Math.min(z, w);
        v3[3] = Math.max(z, w);
        saveVd(4, vd, v3);
    }
    // VFPU4:VSRT2
    public void doVSRT2(int vsize, int vd, int vs) {
        if (vsize != 4) {
            doUNK("Only supported VSRT2.Q");
            return;
        }

        loadVs(4, vs);
        float x = v1[0];
        float y = v1[1];
        float z = v1[2];
        float w = v1[3];
        v3[0] = Math.min(x, w);
        v3[1] = Math.min(y, z);
        v3[2] = Math.max(y, z);
        v3[3] = Math.max(x, w);
        saveVd(4, vd, v3);
    }
    // VFPU4:VBFY1
    public void doVBFY1(int vsize, int vd, int vs) {
        if ((vsize & 1) == 1) {
            doUNK("Only supported VBFY1.P or VBFY1.Q");
            return;
        }

        loadVs(vsize, vs);
        float x = v1[0];
        float y = v1[1];
        v3[0] = x + y;
        v3[1] = x - y;
        if (vsize > 2) {
            float z = v1[2];
            float w = v1[3];
            v3[2] = z + w;
            v3[3] = z - w;
            saveVd(4, vd, v3);
        } else {
            saveVd(2, vd, v3);
        }
    }
    // VFPU4:VBFY2
    public void doVBFY2(int vsize, int vd, int vs) {
        if (vsize != 4) {
            doUNK("Only supported VBFY2.Q");
            return;
        }

        loadVs(vsize, vs);
        float x = v1[0];
        float y = v1[1];
        float z = v1[2];
        float w = v1[3];
        v3[0] = x + z;
        v3[1] = y + w;
        v3[2] = x - z;
        v3[3] = y - w;
        saveVd(4, vd, v3);
    }
    // VFPU4:VOCP
    public void doVOCP(int vsize, int vd, int vs) {
        loadVs(vsize, vs);

        for (int i = 0; i < vsize; ++i) {
            v1[i] = 1.0f - v1[i];
        }

        saveVd(vsize, vd, v1);
    }
    // VFPU4:VSOCP
    public void doVSOCP(int vsize, int vd, int vs) {
        if (vsize > 2) {
            doUNK("Only supported VSOCP.S or VSOCP.P");
            return;
        }

        loadVs(vsize, vs);
        float x = v1[0];
        v3[0] = Math.min(Math.max(0.0f, 1.0f - x), 1.0f);
        v3[1] = Math.min(Math.max(0.0f, 1.0f + x), 1.0f);
        if (vsize > 1) {
            float y = v1[1];
            v3[2] = Math.min(Math.max(0.0f, 1.0f - y), 1.0f);
            v3[3] = Math.min(Math.max(0.0f, 1.0f + y), 1.0f);
            saveVd(4, vd, v3);
        } else {
            saveVd(2, vd, v3);
        }
    }
    // VFPU4:VFAD
    public void doVFAD(int vsize, int vd, int vs) {
        if (vsize == 1) {
            doUNK("Unsupported VFAD.S");
            return;
        }

        loadVs(vsize, vs);

        for (int i = 1; i < vsize; ++i) {
            v1[0] += v1[i];
        }

        saveVd(1, vd, v1);
    }
    // VFPU4:VAVG
    public void doVAVG(int vsize, int vd, int vs) {
        if (vsize == 1) {
            doUNK("Unsupported VAVG.S");
            return;
        }

        loadVs(vsize, vs);

        for (int i = 1; i < vsize; ++i) {
            v1[0] += v1[i];
        }

        v1[0] /= vsize;

        saveVd(1, vd, v1);
    }
    // VFPU4:VSRT3
    public void doVSRT3(int vsize, int vd, int vs) {
        if (vsize != 4) {
            doUNK("Only supported VSRT3.Q (vsize=" + vsize + ")");
            // The instruction is somehow supported on the PSP (see VfpuTest),
            // but leave the error message here to help debugging the Decoder.
            return;
        }

        loadVs(4, vs);
        float x = v1[0];
        float y = v1[1];
        float z = v1[2];
        float w = v1[3];
        v3[0] = Math.max(x, y);
        v3[1] = Math.min(x, y);
        v3[2] = Math.max(z, w);
        v3[3] = Math.min(z, w);
        saveVd(4, vd, v3);
    }
    // VFPU4:VSGN
    public void doVSGN(int vsize, int vd, int vs) {
        loadVs(vsize, vs);
        for (int i = 0; i < vsize; ++i) {
        	v3[i] = Math.signum(v1[i]);
        }
        saveVd(vsize, vd, v3);
    }
    // VFPU4:VSRT4
    public void doVSRT4(int vsize, int vd, int vs) {
        if (vsize != 4) {
            doUNK("Only supported VSRT4.Q");
            return;
        }

        loadVs(4, vs);
        float x = v1[0];
        float y = v1[1];
        float z = v1[2];
        float w = v1[3];
        v3[0] = Math.max(x, w);
        v3[1] = Math.max(y, z);
        v3[2] = Math.min(y, z);
        v3[3] = Math.min(x, w);
        saveVd(4, vd, v3);
    }
    // VFPU4:VMFVC
    public void doVMFVC(int vd, int imm7) {
        int value = 0;
        switch (imm7) {
            case 0: /* 128 */
                value |= vcr.pfxs.swz[0] << 0;
                value |= vcr.pfxs.swz[1] << 2;
                value |= vcr.pfxs.swz[2] << 4;
                value |= vcr.pfxs.swz[3] << 6;
                if (vcr.pfxs.abs[0]) value |=  1 <<  8;
                if (vcr.pfxs.abs[1]) value |=  1 <<  9;
                if (vcr.pfxs.abs[2]) value |=  1 << 10;
                if (vcr.pfxs.abs[3]) value |=  1 << 11;
                if (vcr.pfxs.cst[0]) value |=  1 << 12;
                if (vcr.pfxs.cst[1]) value |=  1 << 13;
                if (vcr.pfxs.cst[2]) value |=  1 << 14;
                if (vcr.pfxs.cst[3]) value |=  1 << 15;
                if (vcr.pfxs.neg[0]) value |=  1 << 16;
                if (vcr.pfxs.neg[1]) value |=  1 << 17;
                if (vcr.pfxs.neg[2]) value |=  1 << 18;
                if (vcr.pfxs.neg[3]) value |=  1 << 19;
                v3i[0] = value;
                saveVdInt(1, vd, v3i);
                break;
            case 1: /* 129 */
                value |= vcr.pfxt.swz[0] << 0;
                value |= vcr.pfxt.swz[1] << 2;
                value |= vcr.pfxt.swz[2] << 4;
                value |= vcr.pfxt.swz[3] << 6;
                if (vcr.pfxt.abs[0]) value |=  1 <<  8;
                if (vcr.pfxt.abs[1]) value |=  1 <<  9;
                if (vcr.pfxt.abs[2]) value |=  1 << 10;
                if (vcr.pfxt.abs[3]) value |=  1 << 11;
                if (vcr.pfxt.cst[0]) value |=  1 << 12;
                if (vcr.pfxt.cst[1]) value |=  1 << 13;
                if (vcr.pfxt.cst[2]) value |=  1 << 14;
                if (vcr.pfxt.cst[3]) value |=  1 << 15;
                if (vcr.pfxt.neg[0]) value |=  1 << 16;
                if (vcr.pfxt.neg[1]) value |=  1 << 17;
                if (vcr.pfxt.neg[2]) value |=  1 << 18;
                if (vcr.pfxt.neg[3]) value |=  1 << 19;
                v3i[0] = value;
                saveVdInt(1, vd, v3i);
                break;
            case 2: /* 130 */
                value |= vcr.pfxd.sat[0] << 0;
                value |= vcr.pfxd.sat[1] << 2;
                value |= vcr.pfxd.sat[2] << 4;
                value |= vcr.pfxd.sat[3] << 6;
                if (vcr.pfxd.msk[0]) value |=  1 <<  8;
                if (vcr.pfxd.msk[1]) value |=  1 <<  9;
                if (vcr.pfxd.msk[2]) value |=  1 << 10;
                if (vcr.pfxd.msk[3]) value |=  1 << 11;
                v3i[0] = value;
                saveVdInt(1, vd, v3i);
                break;
            case 3: /* 131 */
                for (int i = vcr.cc.length - 1; i >= 0; i--) {
                    value <<= 1;
                    if (vcr.cc[i]) {
                        value |= 1;
                    }
                }
                v3i[0] = value;
                saveVdInt(1, vd, v3i);
                break;
            case 8: /* 136 - RCX0 */
                v3i[0] = rnd.getSeed();
                saveVdInt(1, vd, v3i);
                break;
            case 9:  /* 137 - RCX1 */
            case 10: /* 138 - RCX2 */
            case 11: /* 139 - RCX3 */
            case 12: /* 140 - RCX4 */
            case 13: /* 141 - RCX5 */
            case 14: /* 142 - RCX6 */
            case 15: /* 143 - RCX7 */
                // as we do not know how VFPU generates a random number through those 8 registers, we ignore 7 of them
                v3i[0] = 0x3f800000;
                saveVdInt(1, vd, v3i);
                break;
            default:
                // These values are not supported in Jpcsp
                doUNK("Unimplemented VMFVC (vd=" + vd + ", imm7=" + imm7 + ")");
                break;
        }
    }
    // VFPU4:VMTVC
    public void doVMTVC(int vd, int imm7) {
    	loadVdInt(1, vd);
        int value = v1i[0];

        switch (imm7) {
            case 0: /* 128 */
                vcr.pfxs.swz[0] = ((value >> 0 ) & 3);
                vcr.pfxs.swz[1] = ((value >> 2 ) & 3);
                vcr.pfxs.swz[2] = ((value >> 4 ) & 3);
                vcr.pfxs.swz[3] = ((value >> 6 ) & 3);
                vcr.pfxs.abs[0] = ((value >> 8 ) & 1) == 1;
                vcr.pfxs.abs[1] = ((value >> 9 ) & 1) == 1;
                vcr.pfxs.abs[2] = ((value >> 10) & 1) == 1;
                vcr.pfxs.abs[3] = ((value >> 11) & 1) == 1;
                vcr.pfxs.cst[0] = ((value >> 12) & 1) == 1;
                vcr.pfxs.cst[1] = ((value >> 13) & 1) == 1;
                vcr.pfxs.cst[2] = ((value >> 14) & 1) == 1;
                vcr.pfxs.cst[3] = ((value >> 15) & 1) == 1;
                vcr.pfxs.neg[0] = ((value >> 16) & 1) == 1;
                vcr.pfxs.neg[1] = ((value >> 17) & 1) == 1;
                vcr.pfxs.neg[2] = ((value >> 18) & 1) == 1;
                vcr.pfxs.neg[3] = ((value >> 19) & 1) == 1;
                vcr.pfxs.enabled = true;
                break;               
            case 1: /* 129 */
                vcr.pfxt.swz[0] = ((value >> 0 ) & 3);
                vcr.pfxt.swz[1] = ((value >> 2 ) & 3);
                vcr.pfxt.swz[2] = ((value >> 4 ) & 3);
                vcr.pfxt.swz[3] = ((value >> 6 ) & 3);
                vcr.pfxt.abs[0] = ((value >> 8 ) & 1) == 1;
                vcr.pfxt.abs[1] = ((value >> 9 ) & 1) == 1;
                vcr.pfxt.abs[2] = ((value >> 10) & 1) == 1;
                vcr.pfxt.abs[3] = ((value >> 11) & 1) == 1;
                vcr.pfxt.cst[0] = ((value >> 12) & 1) == 1;
                vcr.pfxt.cst[1] = ((value >> 13) & 1) == 1;
                vcr.pfxt.cst[2] = ((value >> 14) & 1) == 1;
                vcr.pfxt.cst[3] = ((value >> 15) & 1) == 1;
                vcr.pfxt.neg[0] = ((value >> 16) & 1) == 1;
                vcr.pfxt.neg[1] = ((value >> 17) & 1) == 1;
                vcr.pfxt.neg[2] = ((value >> 18) & 1) == 1;
                vcr.pfxt.neg[3] = ((value >> 19) & 1) == 1;
                vcr.pfxt.enabled = true;
                break;
            case 2: /* 130 */
                vcr.pfxd.sat[0] = ((value >> 0 ) & 3);
                vcr.pfxd.sat[1] = ((value >> 2 ) & 3);
                vcr.pfxd.sat[2] = ((value >> 4 ) & 3);
                vcr.pfxd.sat[3] = ((value >> 6 ) & 3);
                vcr.pfxd.msk[0] = ((value >> 8 ) & 1) == 1;
                vcr.pfxd.msk[1] = ((value >> 9 ) & 1) == 1;
                vcr.pfxd.msk[2] = ((value >> 10) & 1) == 1;
                vcr.pfxd.msk[3] = ((value >> 11) & 1) == 1;
                vcr.pfxd.enabled = true;
                break;
            case 3: /* 131 */
                for (int i = 0; i < vcr.cc.length; i++) {
                    vcr.cc[i] = (value & 1) != 0;
                    value >>>= 1;
                }
                break;
            case 8: /* 136 - RCX0 */
                rnd.setSeed(value);
                break;
            case 9:  /* 137 - RCX1 */
            case 10: /* 138 - RCX2 */
            case 11: /* 139 - RCX3 */
            case 12: /* 140 - RCX4 */
            case 13: /* 141 - RCX5 */
            case 14: /* 142 - RCX6 */
            case 15: /* 143 - RCX7 */
                // as we do not know how VFPU generates a random number through those 8 registers, we ignore 7 of them
                break;
            default:
                // These values are not supported in Jpcsp
                doUNK("Unimplemented VMTVC (vd=" + vd + ", imm7=" + imm7 + ", value=0x" + Integer.toHexString(value) + ")");
                break;
        }
    }
    // VFPU4:VT4444
    public void doVT4444(int vsize, int vd, int vs) {
        loadVsInt(4, vs);
        int i0 = v1i[0];
        int i1 = v1i[1];
        int i2 = v1i[2];
        int i3 = v1i[3];
        int o0 = 0, o1 = 0;
        o0 |= ((i0>> 4)&15) << 0;
        o0 |= ((i0>>12)&15) << 4;
        o0 |= ((i0>>20)&15) << 8;
        o0 |= ((i0>>28)&15) <<12;
        o0 |= ((i1>> 4)&15) <<16;
        o0 |= ((i1>>12)&15) <<20;
        o0 |= ((i1>>20)&15) <<24;
        o0 |= ((i1>>28)&15) <<28;
        o1 |= ((i2>> 4)&15) << 0;
        o1 |= ((i2>>12)&15) << 4;
        o1 |= ((i2>>20)&15) << 8;
        o1 |= ((i2>>28)&15) <<12;
        o1 |= ((i3>> 4)&15) <<16;
        o1 |= ((i3>>12)&15) <<20;
        o1 |= ((i3>>20)&15) <<24;
        o1 |= ((i3>>28)&15) <<28;
        v3i[0] = o0;
        v3i[1] = o1;
        saveVdInt(2, vd, v3i);
    }
    // VFPU4:VT5551
    public void doVT5551(int vsize, int vd, int vs) {
        loadVsInt(4, vs);
        int i0 = v1i[0];
        int i1 = v1i[1];
        int i2 = v1i[2];
        int i3 = v1i[3];
        int o0 = 0, o1 = 0;
        o0 |= ((i0>> 3)&31) << 0;
        o0 |= ((i0>>11)&31) << 5;
        o0 |= ((i0>>19)&31) <<10;
        o0 |= ((i0>>31)& 1) <<15;
        o0 |= ((i1>> 3)&31) <<16;
        o0 |= ((i1>>11)&31) <<21;
        o0 |= ((i1>>19)&31) <<26;
        o0 |= ((i1>>31)& 1) <<31;
        o1 |= ((i2>> 3)&31) << 0;
        o1 |= ((i2>>11)&31) << 5;
        o1 |= ((i2>>19)&31) <<10;
        o1 |= ((i2>>31)& 1) <<15;
        o1 |= ((i3>> 3)&31) <<16;
        o1 |= ((i3>>11)&31) <<21;
        o1 |= ((i3>>19)&31) <<26;
        o1 |= ((i3>>31)& 1) <<31;
        v3i[0] = o0;
        v3i[1] = o1;
        saveVdInt(2, vd, v3i);
    }
    // VFPU4:VT5650
    public void doVT5650(int vsize, int vd, int vs) {
        loadVsInt(4, vs);
        int i0 = v1i[0];
        int i1 = v1i[1];
        int i2 = v1i[2];
        int i3 = v1i[3];
        int o0 = 0, o1 = 0;
        o0 |= ((i0>> 3)&31) << 0;
        o0 |= ((i0>>10)&63) << 5;
        o0 |= ((i0>>19)&31) <<11;
        o0 |= ((i1>> 3)&31) <<16;
        o0 |= ((i1>>10)&63) <<21;
        o0 |= ((i1>>19)&31) <<27;
        o1 |= ((i2>> 3)&31) << 0;
        o1 |= ((i2>>10)&63) << 5;
        o1 |= ((i2>>19)&31) <<11;
        o1 |= ((i3>> 3)&31) <<16;
        o1 |= ((i3>>10)&63) <<21;
        o1 |= ((i3>>19)&31) <<27;
        v3i[0] = o0;
        v3i[1] = o1;
        saveVdInt(2, vd, v3i);
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
        loadVs(vsize, vs);

        for (int i = 0; i < vsize; ++i) {
            float value = Math.scalb(v1[i], imm5);
            v3i[i] = Math.round(value);
        }

        saveVdInt(vsize, vd, v3i);
    }
    // VFPU4:VF2IZ
    public void doVF2IZ(int vsize, int vd, int vs, int imm5) {
        loadVs(vsize, vs);

        for (int i = 0; i < vsize; ++i) {
            float value = Math.scalb(v1[i], imm5);
            v3i[i] = v1[i] >= 0 ? (int) Math.floor(value) : (int) Math.ceil(value);
        }

        saveVdInt(vsize, vd, v3i);
    }
    // VFPU4:VF2IU
    public void doVF2IU(int vsize, int vd, int vs, int imm5) {
        loadVs(vsize, vs);

        for (int i = 0; i < vsize; ++i) {
            float value = Math.scalb(v1[i], imm5);
            v3i[i] = (int) Math.ceil(value);
        }

        saveVdInt(vsize, vd, v3i);
    }
    // VFPU4:VF2ID
    public void doVF2ID(int vsize, int vd, int vs, int imm5) {
        loadVs(vsize, vs);

        for (int i = 0; i < vsize; ++i) {
            float value = Math.scalb(v1[i], imm5);
            v3i[i] = (int) Math.floor(value);
        }

        saveVdInt(vsize, vd, v3i);
    }
    // VFPU4:VI2F
    public void doVI2F(int vsize, int vd, int vs, int imm5) {
        loadVsInt(vsize, vs);

        for (int i = 0; i < vsize; ++i) {
            float value = (float) v1i[i];
            v3[i] = Math.scalb(value, -imm5);
        }

        saveVd(vsize, vd, v3);
    }
    // VFPU4:VCMOVT
    public void doVCMOVT(int vsize, int imm3, int vd, int vs) {
        if (imm3 < 6) {
            if (vcr.cc[imm3]) {
        		loadVs(vsize, vs);
                saveVd(vsize, vd, v1);
            } else {
            	// Clear the PFXS flag and process the PFXD transformation
            	vcr.pfxs.enabled = false;
            	if (vcr.pfxd.enabled) {
            		loadVd(vsize, vd);
            		saveVd(vsize, vd, v3);
            	}
            }
        } else if (imm3 == 6) {
            loadVs(vsize, vs);
            loadVt(vsize, vd);
            for (int i = 0; i < vsize; ++i) {
                if (vcr.cc[i]) {
                    v2[i] = v1[i];
                }
            }
            saveVd(vsize, vd, v2);
        } else {
        	// Never copy (checked on a PSP)
        }
    }
    // VFPU4:VCMOVF
    public void doVCMOVF(int vsize, int imm3, int vd, int vs) {
        if (imm3 < 6) {
            if (!vcr.cc[imm3]) {
        		loadVs(vsize, vs);
            	saveVd(vsize, vd, v1);
            } else {
            	// Clear the PFXS flag and process the PFXD transformation
            	vcr.pfxs.enabled = false;
            	if (vcr.pfxd.enabled) {
            		loadVd(vsize, vd);
            		saveVd(vsize, vd, v3);
            	}
            }
        } else if (imm3 == 6) {
            loadVs(vsize, vs);
            loadVt(vsize, vd);
            for (int i = 0; i < vsize; ++i) {
                if (!vcr.cc[i]) {
                    v2[i] = v1[i];
                }
            }
            saveVd(vsize, vd, v2);
        } else {
        	// Always copy (checked on a PSP)
        	loadVs(vsize, vs);
        	saveVd(vsize, vd, v1);
        }
    }
    // VFPU4:VWBN
    public void doVWBN(int vsize, int vd, int vs, int imm8) {
        // Wrap BigNum.
        if (vsize != 1) {
            doUNK("Only supported VWBN.S");
            return;
    	}
        loadVs(vsize, vs);

        // Calculate modulus with exponent.
        BigInteger exp = BigInteger.valueOf((int) Math.pow(2, 127-imm8));
        BigInteger bn = BigInteger.valueOf((int) v1[0]);
        if(bn.intValue() > 0) {
            bn = bn.modPow(exp, bn);
        }
        v1[0] = (bn.floatValue() + (v1[0] < 0.0f ? exp.negate().intValue() : exp.intValue()));

        saveVd(vsize, vd, v1);
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
        vcr.pfxd.sat[0] = satx;
        vcr.pfxd.sat[1] = saty;
        vcr.pfxd.sat[2] = satz;
        vcr.pfxd.sat[3] = satw;
        vcr.pfxd.msk[0] = mskx != 0;
        vcr.pfxd.msk[1] = msky != 0;
        vcr.pfxd.msk[2] = mskz != 0;
        vcr.pfxd.msk[3] = mskw != 0;
        vcr.pfxd.enabled = true;
    }

    // VFPU5:VIIM
    public void doVIIM(int vd, int imm16) {
        v3[0] = imm16;

        saveVd(1, vd, v3);
    }

    // VFPU5:VFIM
    public void doVFIM(int vd, int imm16) {        
        v3[0] = halffloatToFloat(imm16);
        
        saveVd(1, vd, v3);
    }

    // group VFPU6   
    // VFPU6:VMMUL
    public void doVMMUL(int vsize, int vd, int vs, int vt) {
    	if (vsize == 1) {
            doUNK("Not supported VMMUL.S");
            return;
    	}

    	// you must do it for disasm, not for emulation !
        //vs = vs ^ 32;

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
        loadVt(1, vt);
        loadVs(2, vs + 0);
        v3[0] = v1[0] * v2[0] + v1[1];
        loadVs(2, vs + 1);
        v3[1] = v1[0] * v2[0] + v1[1];
        saveVd(2, vd, v3);
    }

    // VFPU6:VTFM2
    public void doVTFM2(int vd, int vs, int vt) {
        loadVt(2, vt);
        loadVs(2, vs + 0);
        v3[0] = v1[0] * v2[0] + v1[1] * v2[1];
        loadVs(2, vs + 1);
        v3[1] = v1[0] * v2[0] + v1[1] * v2[1];
        saveVd(2, vd, v3);
    }

    // VFPU6:VHTFM3
    public void doVHTFM3(int vd, int vs, int vt) {
        loadVt(2, vt);
        loadVs(3, vs + 0);
        v3[0] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2];
        loadVs(3, vs + 1);
        v3[1] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2];
        loadVs(3, vs + 2);
        v3[2] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2];
        saveVd(3, vd, v3);
    }

    // VFPU6:VTFM3
    public void doVTFM3(int vd, int vs, int vt) {
        loadVt(3, vt);
        loadVs(3, vs + 0);
        v3[0] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
        loadVs(3, vs + 1);
        v3[1] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
        loadVs(3, vs + 2);
        v3[2] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
        saveVd(3, vd, v3);
    }

    // VFPU6:VHTFM4
    public void doVHTFM4(int vd, int vs, int vt) {
        loadVt(3, vt);
        loadVs(4, vs + 0);
        v3[0] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3];
        loadVs(4, vs + 1);
        v3[1] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3];
        loadVs(4, vs + 2);
        v3[2] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3];
        loadVs(4, vs + 3);
        v3[3] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3];
        saveVd(4, vd, v3);
    }

    // VFPU6:VTFM4
    public void doVTFM4(int vd, int vs, int vt) {
        loadVt(4, vt);
        loadVs(4, vs + 0);
        v3[0] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3] * v2[3];
        loadVs(4, vs + 1);
        v3[1] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3] * v2[3];
        loadVs(4, vs + 2);
        v3[2] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3] * v2[3];
        loadVs(4, vs + 3);
        v3[3] = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2] + v1[3] * v2[3];
        saveVd(4, vd, v3);
    }

    // VFPU6:VMSCL
    public void doVMSCL(int vsize, int vd, int vs, int vt) {
        for (int i = 0; i < vsize; ++i) {
            doVSCL(vsize, vd + i, vs + i, vt);
        }
    }

    // VFPU6:VCRSP
    public void doVCRSP(int vd, int vs, int vt) {
        loadVs(3, vs);
        loadVt(3, vt);

        v3[0] = +v1[1] * v2[2] - v1[2] * v2[1];
        v3[1] = +v1[2] * v2[0] - v1[0] * v2[2];
        v3[2] = +v1[0] * v2[1] - v1[1] * v2[0];

        saveVd(3, vd, v3);
    }

    // VFPU6:VQMUL
    public void doVQMUL(int vd, int vs, int vt) {
        loadVs(4, vs);
        loadVt(4, vt);

        v3[0] = +v1[0] * v2[3] + v1[1] * v2[2] - v1[2] * v2[1] + v1[3] * v2[0];
        v3[1] = -v1[0] * v2[2] + v1[1] * v2[3] + v1[2] * v2[0] + v1[3] * v2[1];
        v3[2] = +v1[0] * v2[1] - v1[1] * v2[0] + v1[2] * v2[3] + v1[3] * v2[2];
        v3[3] = -v1[0] * v2[0] - v1[1] * v2[1] - v1[2] * v2[2] + v1[3] * v2[3];

        saveVd(4, vd, v3);
    }

    // VFPU6:VMMOV
    public void doVMMOV(int vsize, int vd, int vs) {
        for (int i = 0; i < vsize; ++i) {
            doVMOV(vsize, vd + i, vs + i);
        }
    }

    // VFPU6:VMIDT
    public void doVMIDT(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            doVIDT(vsize, vd + i);
        }
    }

    // VFPU6:VMZERO
    public void doVMZERO(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            doVZERO(vsize, vd + i);
        }
    }

    // VFPU7:VMONE
    public void doVMONE(int vsize, int vd) {
        for (int i = 0; i < vsize; ++i) {
            doVONE(vsize, vd + i);
        }
    }

    // VFPU6:VROT
    public void doVROT(int vsize, int vd, int vs, int imm5) {
        loadVs(1, vs);

        double a = 0.5 * Math.PI * v1[0];
        double ca = Math.cos(a);
        double sa = Math.sin(a);

        int i;
        int si = (imm5 >>> 2) & 3;
        int ci = (imm5 >>> 0) & 3;

        if (((imm5 & 16) != 0)) {
            sa = 0.0 - sa;
        }

        if (si == ci) {
            for (i = 0; i < vsize; ++i) {
                v3[i] = (float) sa;
            }
        } else {
            for (i = 0; i < vsize; ++i) {
                v3[i] = (float) 0.0;
            }
            v3[si] = (float) sa;
        }
        v3[ci] = (float) ca;

        saveVd(vsize, vd, v3);
    }

    // group VLSU     
    // LSU:LVS
    public void doLVS(int vt, int rs, int simm14_a16) {
        int s = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        setVprInt(m, i, s, memory.read32(gpr[rs] + simm14_a16));
    }

    // LSU:SVS
    public void doSVS(int vt, int rs, int simm14_a16) {
        int s = (vt >> 5) & 3;
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        if (CHECK_ALIGNMENT) {
            int address = gpr[rs] + simm14_a16;
            if ((address & 3) != 0) {
                Memory.log.error(String.format("SV.S unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        memory.write32(gpr[rs] + simm14_a16, getVprInt(m, i, s));
    }

    // LSU:LVQ
    public void doLVQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 15) != 0) {
                Memory.log.error(String.format("LV.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        if ((vt & 32) != 0) {
            for (int j = 0; j < 4; ++j) {
                setVprInt(m, j, i, memory.read32(address + j * 4));
            }
        } else {
            for (int j = 0; j < 4; ++j) {
                setVprInt(m, i, j, memory.read32(address + j * 4));
            }
        }
    }

    // LSU:LVLQ
    public void doLVLQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("LVL.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int k = 3 - ((address >> 2) & 3);
        address &= ~0xF;
        if ((vt & 32) != 0) {
            for (int j = k; j < 4; ++j) {
                setVprInt(m, j, i, memory.read32(address));
                address += 4;
            }
        } else {
            for (int j = k; j < 4; ++j) {
                setVprInt(m, i, j, memory.read32(address));
                address += 4;
            }
        }
    }

    // LSU:LVRQ
    public void doLVRQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("LVR.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int k = 4 - ((address >> 2) & 3);
        if ((vt & 32) != 0) {
            for (int j = 0; j < k; ++j) {
                setVprInt(m, j, i, memory.read32(address));
                address += 4;
            }
        } else {
            for (int j = 0; j < k; ++j) {
                setVprInt(m, i, j, memory.read32(address));
                address += 4;
            }
        }
    }
    // LSU:SVQ
    public void doSVQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 15) != 0) {
                Memory.log.error(String.format("SV.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        if ((vt & 32) != 0) {
            for (int j = 0; j < 4; ++j) {
                memory.write32((address + j * 4), getVprInt(m, j, i));
            }
        } else {
            for (int j = 0; j < 4; ++j) {
                memory.write32((address + j * 4), getVprInt(m, i, j));
            }
        }
    }

    // LSU:SVLQ
    public void doSVLQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("SVL.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int k = 3 - ((address >> 2) & 3);
        address &= ~0xF;
        if ((vt & 32) != 0) {
            for (int j = k; j < 4; ++j) {
                memory.write32(address, getVprInt(m, j, i));
                address += 4;
            }
        } else {
            for (int j = k; j < 4; ++j) {
                memory.write32(address, getVprInt(m, i, j));
                address += 4;
            }
        }
    }

    // LSU:SVRQ
    public void doSVRQ(int vt, int rs, int simm14_a16) {
        int m = (vt >> 2) & 7;
        int i = (vt >> 0) & 3;

        int address = gpr[rs] + simm14_a16;

        if (CHECK_ALIGNMENT) {
            if ((address & 3) != 0) {
                Memory.log.error(String.format("SVR.Q unaligned addr:0x%08x pc:0x%08x", address, pc));
            }
        }

        int k = 4 - ((address >> 2) & 3);
        if ((vt & 32) != 0) {
            for (int j = 0; j < k; ++j) {
                memory.write32(address, getVprInt(m, j, i));
                address += 4;
            }
        } else {
            for (int j = 0; j < k; ++j) {
                memory.write32(address, getVprInt(m, i, j));
                address += 4;
            }
        }
    }
}
