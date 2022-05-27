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

import java.io.IOException;
import java.util.Arrays;

import jpcsp.Emulator;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Floating Point Unit, handles floating point operations, including BCU and LSU
 *
 * @author hli, gid15
 */
public class FpuState extends BcuState {
	private static final int STATE_VERSION = 0;
	public static final boolean IMPLEMENT_ROUNDING_MODES = true;
	private static final String roundingModeNames[] = {
		"Round to neareast number",
		"Round toward zero",
		"Round toward positive infinity",
		"Round toward negative infinity"
	};
	public static final int ROUNDING_MODE_NEAREST             = 0;
	public static final int ROUNDING_MODE_TOWARD_ZERO         = 1;
	public static final int ROUNDING_MODE_TOWARD_POSITIVE_INF = 2;
	public static final int ROUNDING_MODE_TOWARD_NEGATIVE_INF = 3;

    public static final class Fcr0 {
        public static final int imp = 0; /* FPU design number */
        public static final int rev = 0; /* FPU revision bumber */
    }

    public class Fcr31 {
    	private static final int STATE_VERSION = 0;
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

        public void copy(Fcr31 that) {
        	rm = that.rm;
        	c = that.c;
        	fs = that.fs;
        }

        public void read(StateInputStream stream) throws IOException {
        	stream.readVersion(STATE_VERSION);
        	rm = stream.readInt();
        	c = stream.readBoolean();
        	fs = stream.readBoolean();
        }

        public void write(StateOutputStream stream) throws IOException {
        	stream.writeVersion(STATE_VERSION);
        	stream.writeInt(rm);
        	stream.writeBoolean(c);
        	stream.writeBoolean(fs);
        }
    }

    public final float[] fpr = new float[32];
    public final Fcr31 fcr31 = new Fcr31();

    @Override
    public void reset() {
        Arrays.fill(fpr, 0.0f);
        fcr31.reset();
    }

    @Override
    public void resetAll() {
        super.resetAll();
        Arrays.fill(fpr, 0.0f);
        fcr31.reset();
    }

    public FpuState() {
    }

    public void copy(FpuState that) {
        super.copy(that);
        System.arraycopy(that.fpr, 0, fpr, 0, fpr.length);
        fcr31.copy(that.fcr31);
    }

    public FpuState(FpuState that) {
        super(that);
        System.arraycopy(that.fpr, 0, fpr, 0, fpr.length);
        fcr31.copy(that.fcr31);
    }

    @Override
    public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	stream.readFloats(fpr);
    	fcr31.read(stream);
    	super.read(stream);
    }

    @Override
    public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeFloats(fpr);
    	fcr31.write(stream);
    	super.write(stream);
    }

    public float round(double d) {
    	float f = (float) d;

    	if (Float.isInfinite(f) || Float.isNaN(f)) {
    		return f;
    	}

    	if (fcr31.fs) {
    		// Flush-to-zero for denormalized numbers
			int exp = Math.getExponent(f);
			if (exp < Float.MIN_EXPONENT) {
				return 0f;
			}
    	}

    	switch (fcr31.rm) {
			case ROUNDING_MODE_NEAREST:
				// This is the java default rounding mode, nothing more to do.
				break;
    		case ROUNDING_MODE_TOWARD_ZERO:
    			if (d < 0.0) {
    				if (d > f) {
    					f = Math.nextUp(f);
    				}
    			} else {
    				if (d < f) {
    					f = Math.nextAfter(f, 0.0);
    				}
    			}
    			break;
    		case ROUNDING_MODE_TOWARD_POSITIVE_INF:
    			if (d > f) {
    				f = Math.nextUp(f);
    			}
    			break;
    		case ROUNDING_MODE_TOWARD_NEGATIVE_INF:
    			if (d < f) {
    				f = Math.nextAfter(f, Double.NEGATIVE_INFINITY);
    			}
    			break;
			default:
				Emulator.log.error(String.format("Unknown rounding mode %d", fcr31.rm));
				break;
    	}

    	return f;
    }

    public void doMFC1(int rt, int c1dr) {
    	if (rt != 0) {
    		setRegister(rt, Float.floatToRawIntBits(fpr[c1dr]));
    	}
    }

    public void doCFC1(int rt, int c1cr) {
        if (rt != 0) {
            switch (c1cr) {
                case 0:
                    setRegister(rt, (Fcr0.imp << 8) | (Fcr0.rev));
                    break;

                case 31:
                	setRegister(rt, (fcr31.fs ? (1 << 24) : 0) | (fcr31.c ? (1 << 23) : 0) | (fcr31.rm & 3));
                    break;

                default:
                    doUNK(String.format("Unsupported cfc1 instruction for fcr%d", c1cr));
            }
        }
    }

    public void doMTC1(int rt, int c1dr) {
        fpr[c1dr] = Float.intBitsToFloat(getRegister(rt));
    }

    public void doCTC1(int rt, int c1cr) {
        switch (c1cr) {
            case 31:
                int bits = getRegister(rt) & 0x01800003;
                fcr31.rm = bits & 3;
                fcr31.fs = ((bits >> 24) & 1) != 0;
                fcr31.c  = ((bits >> 23) & 1) != 0;
                if (fcr31.rm != ROUNDING_MODE_NEAREST) {
                	// Only rounding mode 0 is supported in Java
                	Emulator.log.warn(String.format("CTC1 unsupported rounding mode '%s' (rm=%d)", roundingModeNames[fcr31.rm], fcr31.rm));
                }
                if (fcr31.fs) {
                	// Flush-to-zero is not supported in Java
                	Emulator.log.warn(String.format("CTC1 unsupported flush-to-zero fs=%b", fcr31.fs));
                }
                break;

            default:
                doUNK(String.format("Unsupported ctc1 instruction for fcr%d", c1cr));
        }
    }

    public boolean doBC1F(int simm16) {
        npc = !fcr31.c ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }

    public boolean doBC1T(int simm16) {
        npc = fcr31.c ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }

    public boolean doBC1FL(int simm16) {
        if (!fcr31.c) {
            npc = branchTarget(pc, simm16);
            return true;
        }
		pc += 4;
        return false;
    }

    public boolean doBC1TL(int simm16) {
        if (fcr31.c) {
            npc = branchTarget(pc, simm16);
            return true;
        }
		pc += 4;
        return false;
    }

    public void doADDS(int fd, int fs, int ft) {
        fpr[fd] = fpr[fs] + fpr[ft];
    }

    public void doSUBS(int fd, int fs, int ft) {
        fpr[fd] = fpr[fs] - fpr[ft];
    }

    public void doMULS(int fd, int fs, int ft) {
    	if (IMPLEMENT_ROUNDING_MODES) {
    		fpr[fd] = round(fpr[fs] * (double) fpr[ft]);
    	} else {
    		fpr[fd] = fpr[fs] * fpr[ft];
    	}
    }

    public void doDIVS(int fd, int fs, int ft) {
        fpr[fd] = fpr[fs] / fpr[ft];
    }

    public void doSQRTS(int fd, int fs) {
        fpr[fd] = (float) Math.sqrt(fpr[fs]);
    }

    public void doABSS(int fd, int fs) {
        fpr[fd] = Math.abs(fpr[fs]);
    }

    public void doMOVS(int fd, int fs) {
        fpr[fd] = fpr[fs];
    }

    public void doNEGS(int fd, int fs) {
        fpr[fd] = 0.0f - fpr[fs];
    }

    public void doROUNDWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat(Math.round(fpr[fs]));
    }

    public void doTRUNCWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat((int) (fpr[fs]));
    }

    public void doCEILWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat((int) Math.ceil(fpr[fs]));
    }

    public void doFLOORWS(int fd, int fs) {
        fpr[fd] = Float.intBitsToFloat((int) Math.floor(fpr[fs]));
    }

    public void doCVTSW(int fd, int fs) {
        fpr[fd] = Float.floatToRawIntBits(fpr[fs]);
    }

    public void doCVTWS(int fd, int fs) {
        switch (fcr31.rm) {
            case ROUNDING_MODE_TOWARD_ZERO:
                fpr[fd] = Float.intBitsToFloat((int) (fpr[fs]));
                break;
            case ROUNDING_MODE_TOWARD_POSITIVE_INF:
                fpr[fd] = Float.intBitsToFloat((int) Math.ceil(fpr[fs]));
                break;
            case ROUNDING_MODE_TOWARD_NEGATIVE_INF:
                fpr[fd] = Float.intBitsToFloat((int) Math.floor(fpr[fs]));
                break;
            default:
                fpr[fd] = Float.intBitsToFloat((int) Math.rint(fpr[fs]));
                break;
        }
    }

    public void doCCONDS(int fs, int ft, int cond) {
        float x = fpr[fs];
        float y = fpr[ft];

        if (Float.isNaN(x) || Float.isNaN(y)) {
        	fcr31.c = (cond & 1) != 0;
        } else {
            boolean equal = ((cond & 2) != 0) && (x == y);
            boolean less = ((cond & 4) != 0) && (x < y);

            fcr31.c = less || equal;
        }
    }

    public void doLWC1(int ft, int rs, int simm16) {
        fpr[ft] = Float.intBitsToFloat(memory.read32(getRegister(rs) + simm16));
    }

    public void doSWC1(int ft, int rs, int simm16) {
        memory.write32(getRegister(rs) + simm16, Float.floatToRawIntBits(fpr[ft]));
    }
}