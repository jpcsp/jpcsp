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
package jpcsp.media.codec.mp3;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static jpcsp.media.codec.mp3.Mp3Decoder.FRAC_BITS;
import static jpcsp.media.codec.mp3.Mp3Decoder.IMDCT_SCALAR;
import static jpcsp.media.codec.mp3.Mp3Decoder.SBLIMIT;
import jpcsp.media.codec.util.Dct32;

public class Mp3Dsp {
	public static final int MDCT_BUF_SIZE = 36;
	public static final float mdct_win[][] = new float[8][MDCT_BUF_SIZE];
	public static final float mpa_synth_window[] = new float[512 + 256];
	private static final double C1 = 0.98480775301220805936/2;
	private static final double C2 = 0.93969262078590838405/2;
	private static final double C3 = 0.86602540378443864676/2;
	private static final double C4 = 0.76604444311897803520/2;
	private static final double C5 = 0.64278760968653932632/2;
	//private static final double C6 = 0.5/2;
	private static final double C7 = 0.34202014332566873304/2;
	private static final double C8 = 0.17364817766693034885/2;

	/* 0.5 / cos(pi*(2*i+1)/36) */
	private static final double icos36[] = new double[] {
	    0.50190991877167369479,
	    0.51763809020504152469, //0
	    0.55168895948124587824,
	    0.61038729438072803416,
	    0.70710678118654752439, //1
	    0.87172339781054900991,
	    1.18310079157624925896,
	    1.93185165257813657349, //2
	    5.73685662283492756461
	};

	/* 0.5 / cos(pi*(2*i+1)/36) */
	private static final double icos36h[] = new double[] {
	    0.50190991877167369479/2,
	    0.51763809020504152469/2, //0
	    0.55168895948124587824/2,
	    0.61038729438072803416/2,
	    0.70710678118654752439/2, //1
	    0.87172339781054900991/2,
	    1.18310079157624925896/4,
	    1.93185165257813657349/4, //2
//	    5.73685662283492756461),
	};

	/* half mpeg encoding window (full precision) */
	private static final int mpa_enwindow[] = {
	     0,    -1,    -1,    -1,    -1,    -1,    -1,    -2,
	    -2,    -2,    -2,    -3,    -3,    -4,    -4,    -5,
	    -5,    -6,    -7,    -7,    -8,    -9,   -10,   -11,
	   -13,   -14,   -16,   -17,   -19,   -21,   -24,   -26,
	   -29,   -31,   -35,   -38,   -41,   -45,   -49,   -53,
	   -58,   -63,   -68,   -73,   -79,   -85,   -91,   -97,
	  -104,  -111,  -117,  -125,  -132,  -139,  -147,  -154,
	  -161,  -169,  -176,  -183,  -190,  -196,  -202,  -208,
	   213,   218,   222,   225,   227,   228,   228,   227,
	   224,   221,   215,   208,   200,   189,   177,   163,
	   146,   127,   106,    83,    57,    29,    -2,   -36,
	   -72,  -111,  -153,  -197,  -244,  -294,  -347,  -401,
	  -459,  -519,  -581,  -645,  -711,  -779,  -848,  -919,
	  -991, -1064, -1137, -1210, -1283, -1356, -1428, -1498,
	 -1567, -1634, -1698, -1759, -1817, -1870, -1919, -1962,
	 -2001, -2032, -2057, -2075, -2085, -2087, -2080, -2063,
	  2037,  2000,  1952,  1893,  1822,  1739,  1644,  1535,
	  1414,  1280,  1131,   970,   794,   605,   402,   185,
	   -45,  -288,  -545,  -814, -1095, -1388, -1692, -2006,
	 -2330, -2663, -3004, -3351, -3705, -4063, -4425, -4788,
	 -5153, -5517, -5879, -6237, -6589, -6935, -7271, -7597,
	 -7910, -8209, -8491, -8755, -8998, -9219, -9416, -9585,
	 -9727, -9838, -9916, -9959, -9966, -9935, -9863, -9750,
	 -9592, -9389, -9139, -8840, -8492, -8092, -7640, -7134,
	  6574,  5959,  5288,  4561,  3776,  2935,  2037,  1082,
	    70,  -998, -2122, -3300, -4533, -5818, -7154, -8540,
	 -9975,-11455,-12980,-14548,-16155,-17799,-19478,-21189,
	-22929,-24694,-26482,-28289,-30112,-31947,-33791,-35640,
	-37489,-39336,-41176,-43006,-44821,-46617,-48390,-50137,
	-51853,-53534,-55178,-56778,-58333,-59838,-61289,-62684,
	-64019,-65290,-66494,-67629,-68692,-69679,-70590,-71420,
	-72169,-72835,-73415,-73908,-74313,-74630,-74856,-74992,
	 75038
	};

	public static void initMpadspTabs() {
		// compute mdct windows
		for (int i = 0; i < 36; i++) {
			for (int j = 0; j < 4; j++) {
				if (j == 2 && (i % 3) != 1) {
					continue;
				}

				double d = sin(PI * (i + 0.5) / 36.0);
				if (j == 1) {
					if      (i >= 30) d = 0.0;
					else if (i >= 24) d = sin(PI * (i - 18 + 0.5) / 12.0);
					else if (i >= 18) d = 1.0;
				} else if (j == 3) {
					if      (i <   6) d = 0.0;
					else if (i <  12) d = sin(PI * (i -  6 + 0.5) / 12.0);
					else if (i <  18) d = 1.0;
				}
				// merge last stage of imdct into the window coefficients
				d *= 0.5 * IMDCT_SCALAR / cos(PI * (2 * i + 19) / 72.0);

				if (j == 2) {
					mdct_win[j][i / 3] = (float) (d / (1 << 5));
				} else {
					int idx = i < 18 ? i : i + (MDCT_BUF_SIZE / 2 - 18);
					mdct_win[j][idx] = (float) (d / (1 << 5));
				}
			}
		}

		// NOTE: we do frequency inversion after the MDCT by changing
		// the sign of the right window coeffs
		for (int j = 0; j < 4; j++) {
			for (int i = 0; i < MDCT_BUF_SIZE; i += 2) {
				mdct_win[j + 4][i    ] =  mdct_win[j][i    ];
				mdct_win[j + 4][i + 1] = -mdct_win[j][i + 1];
			}
		}
	}

	public static void synthInit(float[] window) {
		// max = 18760, max sum over all 16 coeffs: 44736
		for (int i = 0; i < 257; i++) {
			float v = mpa_enwindow[i];
			v *= 1f / (1L << (16 + FRAC_BITS));
			window[i] = v;
			if ((i & 63) != 0) {
				v = -v;
			}
			if (i != 0) {
				window[512 - i] = v;
			}
		}

		// Needed for avoiding shuffles in ASM implementations
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 16; j++) {
				window[512 + 16 * i + j] = window[64 * i + 32 - j];
			}
		}

		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 16; j++) {
				window[512 + 128 + 16 * i + j] = window[64 * i + 48 - j];
			}
		}
	}

	private static void applyWindow(float[] synthBuf, int synthBufOffset, float[] window, int ditherState[], float[] samples, int samplesOffset, int incr) {
		System.arraycopy(synthBuf, synthBufOffset, synthBuf, synthBufOffset + 512, 32);

		int samples2 = samplesOffset + 31 * incr;
		int w = 0;
		int w2 = 31;

		float sum = ditherState[0];
		int p = synthBufOffset + 16;
		for (int i = 0; i < 8; i++) {
			sum += window[w + i * 64] * synthBuf[p + i * 64];
		}
		p = synthBufOffset + 48;
		for (int i = 0; i < 8; i++) {
			sum -= window[w + 32 + i * 64] * synthBuf[p + i * 64];
		}
		samples[samplesOffset] = sum;
		sum = 0f;
		samplesOffset += incr;
		w++;

		// we calculate two samples at the same time to avoid one memory
		// access per two sample
		for (int j = 1; j < 16; j++) {
			float sum2 = 0f;
			sum = 0f;
			p = synthBufOffset + 16 + j;
			for (int i = 0; i < 8; i++) {
				float tmp = synthBuf[p + i * 64];
				sum  += window[w  + i * 64] * tmp;
				sum2 -= window[w2 + i * 64] * tmp;
			}
			p = synthBufOffset + 48 - j;
			for (int i = 0; i < 8; i++) {
				float tmp = synthBuf[p + i * 64];
				sum  -= window[w  + 32 + i * 64] * tmp;
				sum2 -= window[w2 + 32 + i * 64] * tmp;
			}

			samples[samplesOffset] = sum;
			samplesOffset += incr;
			samples[samples2] = sum2;
			samples2 -= incr;
			w++;
			w2--;
		}

		p = synthBufOffset + 32;
		sum = 0f;
		for (int i = 0; i < 8; i++) {
			sum -= window[w + 32 + i * 64] * synthBuf[p + i * 64];
		}
		samples[samplesOffset] = sum;
		sum = 0f;
		ditherState[0] = (int) sum;
	}

	// 32 sub band synthesis filter. Input: 32 sub band samples, Output: 32 samples.
	public static void synthFilter(Context ctx, int ch, float[] samples, int samplesOffset, int incr, float[] sbSamples, int sbSamplesOffset) {
		int offset = ctx.synthBufOffset[ch];

		Dct32.dct32(ctx.synthBuf[ch], offset, sbSamples, sbSamplesOffset);
		applyWindow(ctx.synthBuf[ch], offset, mpa_synth_window, ctx.ditherState, samples, samplesOffset, incr);

		offset = (offset - 32) & 511;
		ctx.synthBufOffset[ch] = offset;
	}

	// using Lee like decomposition followed by hand coded 9 points DCT
	private static void imdct36(float[] out, int outOffset, float[] buf, int bufOffset, float[] in, int inOffset, float[] win) {
		final float tmp[] = new float[18];

		for (int i = 17; i >= 1; i--) {
			in[inOffset + i] += in[inOffset + i - 1];
		}
		for (int i = 17; i >= 3; i -= 2) {
			in[inOffset + i] += in[inOffset + i - 2];
		}

		for (int j = 0; j < 2; j++) {
			float t0;
			int tmp1 = j;
			int in1 = inOffset + j;

			float t2 = in[in1 + 2 * 4] + in[in1 + 2 * 8] - in[in1 + 2 * 2];

			float t3 = in[in1 + 2 * 0] + in[in1 + 2 * 6] * 0.5f;
			float t1 = in[in1 + 2 * 0] - in[in1 + 2 * 6];
			tmp[tmp1 +  6] = t1 - t2 * 0.5f;
			tmp[tmp1 + 16] = t1 + t2;

			t0 = (float) ((in[in1 + 2 * 2] + in[in1 + 2 * 4]) * C2 *  2f);
			t1 = (float) ((in[in1 + 2 * 4] - in[in1 + 2 * 8]) * C8 * -2f);
			t2 = (float) ((in[in1 + 2 * 2] + in[in1 + 2 * 8]) * C4 * -2f);

			tmp[tmp1 + 10] = t3 - t0 - t2;
			tmp[tmp1 +  2] = t3 + t0 + t1;
			tmp[tmp1 + 14] = t3 + t2 - t1;

			tmp[tmp1 +  4] = (float) ((in[in1 + 2 * 5] + in[in1 + 2 * 7] - in[in1 + 2 * 1]) * C3 * -2f);
			t2 = (float) ((in[in1 + 2 * 1] + in[in1 + 2 * 5]) * C1 *  2f);
			t3 = (float) ((in[in1 + 2 * 5] - in[in1 + 2 * 7]) * C7 * -2f);
			t0 = (float) ( in[in1 + 2 * 3]                    * C3 *  2f);

			t1 = (float) ((in[in1 + 2 * 1] + in[in1 + 2 * 7]) * C5 * -2f);

			tmp[tmp1 +  0] = t2 + t3 + t0;
			tmp[tmp1 + 12] = t2 + t1 - t0;
			tmp[tmp1 +  8] = t3 - t1 - t0;
		}

		int i = 0;
		for (int j = 0; j < 4; j++) {
			float t0 = tmp[i];
			float t1 = tmp[i + 2];
			float s0 = t1 + t0;
			float s2 = t1 - t0;

			float t2 = tmp[i + 1];
			float t3 = tmp[i + 3];
			float s1 = (float) ((t3 + t2) * icos36h[j] * 2f);
			float s3 = (float) ((t3 - t2) * icos36[8 - j]);

			t0 = s0 + s1;
			t1 = s0 - s1;
			out[outOffset + (9 + j) * SBLIMIT] = t1 * win[9 + j] + buf[bufOffset + 4 * (9 + j)];
			out[outOffset + (8 - j) * SBLIMIT] = t1 * win[8 - j] + buf[bufOffset + 4 * (8 - j)];
			buf[bufOffset + 4 * (9 + j)] = t0 * win[MDCT_BUF_SIZE / 2 + 9 + j];
			buf[bufOffset + 4 * (8 - j)] = t0 * win[MDCT_BUF_SIZE / 2 + 8 - j];

			t0 = s2 + s3;
			t1 = s2 - s3;
			out[outOffset + (9 + 8 - j) * SBLIMIT] = t1 * win[9 + 8 - j] + buf[bufOffset + 4 * (9 + 8 - j)];
			out[outOffset +          j  * SBLIMIT] = t1 * win[        j] + buf[bufOffset + 4 *          j ];
			buf[bufOffset + 4 * (9 + 8 - j)] = t0 * win[MDCT_BUF_SIZE / 2 + 9 + 8 - j];
			buf[bufOffset + 4 *          j ] = t0 * win[MDCT_BUF_SIZE / 2         + j];
			i += 4;
		}

		float s0 = tmp[16];
		float s1 = (float) (tmp[17] * icos36h[4] * 2f);
		float t0 = s0 + s1;
		float t1 = s0 - s1;
		out[outOffset + (9 + 4) * SBLIMIT] = t1 * win[9 + 4] + buf[bufOffset + 4 * (9 + 4)];
		out[outOffset + (8 - 4) * SBLIMIT] = t1 * win[8 - 4] + buf[bufOffset + 4 * (8 - 4)];
		buf[bufOffset + 4 * (9 + 4)] = t0 * win[MDCT_BUF_SIZE / 2 + 9 + 4];
		buf[bufOffset + 4 * (8 - 4)] = t0 * win[MDCT_BUF_SIZE / 2 + 8 - 4];
	}

	public static void imdct36Blocks(float[] out, int outOffset, float[] buf, int bufOffset, float[] in, int inOffset, int count, int switchPoint, int blockType) {
		for (int j = 0; j < count; j++) {
			// apply window & overlap with previous buffer

			// select window
			int winIdx = (switchPoint != 0 && j < 2) ? 0 : blockType;
			float win[] = mdct_win[winIdx + (4 & -(j & 1))];

			imdct36(out, outOffset, buf, bufOffset, in, inOffset, win);

			inOffset  += 18;
			bufOffset += ((j & 3) != 3 ? 1 : (72 - 3));
			outOffset++;
		}
	}
}
