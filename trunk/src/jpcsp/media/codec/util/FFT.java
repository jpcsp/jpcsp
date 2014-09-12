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
package jpcsp.media.codec.util;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import jpcsp.media.codec.CodecFactory;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class FFT {
	private static Logger log = CodecFactory.log;
	int nbits;
	boolean inverse;
	int revtab[] = new int[0];
	float tmpBuf[] = new float[0];
	int mdctSize; // size of MDCT (i.e. number of input data * 2)
	int mdctBits; // n = 2^nbits
	// pre/post rotation tables
	float tcos[] = new float[0];
	float tsin[] = new float[0];
	public static final double M_SQRT1_2 = 0.70710678118654752440; // 1/sqrt(2)
	private static final float sqrthalf = (float) M_SQRT1_2;
	private static final float[] ff_cos_16  = new float[16 / 2];
	private static final float[] ff_cos_32  = new float[32 / 2];
	private static final float[] ff_cos_64  = new float[64 / 2];
	private static final float[] ff_cos_128 = new float[128 / 2];
	private static final float[] ff_cos_256 = new float[256 / 2];
	private static final float[] ff_cos_512 = new float[512 / 2];

	public void copy(FFT that) {
		nbits = that.nbits;
		inverse = that.inverse;
		Utilities.copy(revtab, that.revtab);
		Utilities.copy(tmpBuf, that.tmpBuf);
		mdctSize = that.mdctSize;
		mdctBits = that.mdctBits;
		Utilities.copy(tcos, that.tcos);
		Utilities.copy(tsin, that.tsin);
	}

	private static void initFfCosTabs(float[] tab, int m) {
		double freq = 2 * Math.PI / m;
		for (int i = 0; i <= m / 4; i++) {
			tab[i] = (float) cos(i * freq);
		}
		for (int i = 1; i < m / 4; i++) {
			tab[m / 2 - i] = tab[i];
		}
	}

	private static int splitRadixPermutation(int i, int n, boolean inverse) {
		if (n <= 2) {
			return i & 1;
		}
		int m = n >> 1;
		if ((i & m) == 0) {
			return splitRadixPermutation(i, m, inverse) * 2;
		}
		m >>= 1;
		return splitRadixPermutation(i, m, inverse) * 4 + (inverse == ((i & m) == 0) ? 1 : -1);
	}

	private int fftInit(int nbits, boolean inverse) {
		if (nbits < 2 || nbits > 16) {
			revtab = null;
			tmpBuf = null;
			return -1;
		}

		this.nbits = nbits;
		this.inverse = inverse;

		int n = 1 << nbits;
		revtab = new int[n];
		tmpBuf = new float[n * 2];

		initFfCosTabs(ff_cos_16 ,  16);
		initFfCosTabs(ff_cos_32 ,  32);
		initFfCosTabs(ff_cos_64 ,  64);
		initFfCosTabs(ff_cos_128, 128);
		initFfCosTabs(ff_cos_256, 256);
		initFfCosTabs(ff_cos_512, 512);

		for (int i = 0; i < n; i++) {
			revtab[-splitRadixPermutation(i, n, inverse) & (n - 1)] = i;
		}

		return 0;
	}

	public int mdctInit(int nbits, boolean inverse, double scale) {
		int n = 1 << nbits;
		mdctBits = nbits;
		mdctSize = n;
		int n4 = n >> 2;

		int ret = fftInit(mdctBits - 2, inverse);
		if (ret < 0) {
			return ret;
		}

		tcos = new float[n4];
		tsin = new float[n4];

		double theta = 1.0 / 8.0 + (scale < 0 ? n4 : 0);
		scale = sqrt(abs(scale));
		for (int i = 0; i < n4; i++) {
			double alpha = 2 * Math.PI * (i + theta) / n;
			tcos[i] = (float) (-cos(alpha) * scale);
			tsin[i] = (float) (-sin(alpha) * scale);
		}

		return 0;
	}

	/**
	 * Compute inverse MDCT of size N = 2^nbits
	 * @param output N samples
	 * @param input N/2 samples
	 */
	public void imdctCalc(float[] output, int outputOffset, float[] input, int inputOffset) {
		int n = 1 << mdctBits;
		int n2 = n >> 1;
		int n4 = n >> 2;

		imdctHalf(output, outputOffset + n4, input, inputOffset);

		for (int k = 0; k < n4; k++) {
			output[outputOffset + k] = -output[outputOffset + n2 - k - 1];
			output[outputOffset + n - k - 1] = output[outputOffset + n2 + k];
		}
	}

	/**
	 * Compute the middle half of the inverse MDCT of size N = 2^nbits,
	 * thus excluding the parts that can be derived by symmetry
	 * @param output N/2 samples
	 * @param input N/2 samples
	 */
	public void imdctHalf(float[] output, int outputOffset, final float[] input, int inputOffset) {
		int n = 1 << mdctBits;
		int n2 = n >> 1;
		int n4 = n >> 2;
		int n8 = n >> 3;

		// pre rotation
		int in1 = 0;
		int in2 = n2 - 1;
		for (int k = 0; k < n4; k++) {
			int j = revtab[k];
			CMUL(output, outputOffset + j * 2, outputOffset + j * 2 + 1, input[inputOffset + in2], input[inputOffset + in1], tcos[k], tsin[k]);
			in1 += 2;
			in2 -= 2;
		}
		fftCalcFloat(output, outputOffset);

		// post rotation + reordering
		final float[] r = new float[4];
		for (int k = 0; k < n8; k++) {
			CMUL(r, 0, 3, output[outputOffset + (n8 - k - 1) * 2 + 1], output[outputOffset + (n8 - k - 1) * 2 + 0], tsin[n8 - k - 1], tcos[n8 - k - 1]);
			CMUL(r, 2, 1, output[outputOffset + (n8 + k    ) * 2 + 1], output[outputOffset + (n8 + k    ) * 2 + 0], tsin[n8 + k    ], tcos[n8 + k    ]);
			output[outputOffset + (n8 - k - 1) * 2 + 0] = r[0];
			output[outputOffset + (n8 - k - 1) * 2 + 1] = r[1];
			output[outputOffset + (n8 + k    ) * 2 + 0] = r[2];
			output[outputOffset + (n8 + k    ) * 2 + 1] = r[3];
		}
	}

	private static void CMUL(float[] d, int dre, int dim, float are, float aim, float bre, float bim) {
		d[dre] = are * bre - aim * bim;
		d[dim] = are * bim + aim * bre;
	}

	private void fft4(float[] z, int o) {
	    // BF(t3, t1, z[0].re, z[1].re);
	    // BF(t8, t6, z[3].re, z[2].re);
	    // BF(z[2].re, z[0].re, t1, t6);
	    // BF(t4, t2, z[0].im, z[1].im);
	    // BF(t7, t5, z[2].im, z[3].im);
	    // BF(z[3].im, z[1].im, t4, t8);
	    // BF(z[3].re, z[1].re, t3, t7);
	    // BF(z[2].im, z[0].im, t2, t5);
		double t3 = z[o + 0] - z[o + 2];
		double t1 = z[o + 0] + z[o + 2];
		double t8 = z[o + 6] - z[o + 4];
		double t6 = z[o + 6] + z[o + 4];
		z[o + 4] = (float) (t1 - t6);
		z[o + 0] = (float) (t1 + t6);
		double t4 = z[o + 1] - z[o + 3];
		double t2 = z[o + 1] + z[o + 3];
		double t7 = z[o + 5] - z[o + 7];
		double t5 = z[o + 5] + z[o + 7];
		z[o + 7] = (float) (t4 - t8);
		z[o + 3] = (float) (t4 + t8);
		z[o + 6] = (float) (t3 - t7);
		z[o + 2] = (float) (t3 + t7);
		z[o + 5] = (float) (t2 - t5);
		z[o + 1] = (float) (t2 + t5);
	}

	private void fft8(float[] z, int o) {
		fft4(z, o);

	    // BF(t1, z[5].re, z[4].re, -z[5].re);
	    // BF(t2, z[5].im, z[4].im, -z[5].im);
		// BF(t5, z[7].re, z[6].re, -z[7].re);
		// BF(t6, z[7].im, z[6].im, -z[7].im);
		double t1 = z[o +  8] + z[o + 10];
		z[o + 10] = z[o +  8] - z[o + 10];
		double t2 = z[o +  9] + z[o + 11];
		z[o + 11] = z[o +  9] - z[o + 11];
		double t5 = z[o + 12] + z[o + 14];
		z[o + 14] = z[o + 12] - z[o + 14];
		double t6 = z[o + 13] + z[o + 15];
		z[o + 15] = z[o + 13] - z[o + 15];

		// BUTTERFLIES(z[0],z[2],z[4],z[6]);
		double t3 = t5 - t1;
		t5 = t5 + t1;
		z[o +  8] = (float) (z[o + 0] - t5);
		z[o +  0] = (float) (z[o + 0] + t5);
		z[o + 13] = (float) (z[o + 5] - t3);
		z[o +  5] = (float) (z[o + 5] + t3);
		double t4 = t2 - t6;
		t6 = t2 + t6;
		z[o + 12] = (float) (z[o + 4] - t4);
		z[o +  4] = (float) (z[o + 4] + t4);
		z[o +  9] = (float) (z[o + 1] - t6);
		z[o +  1] = (float) (z[o + 1] + t6);

		// TRANSFORM(z[1],z[3],z[5],z[7],sqrthalf,sqrthalf);
		//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
		t1 =   z[o + 10] * sqrthalf + z[o + 11] * sqrthalf;
		t2 = - z[o + 10] * sqrthalf + z[o + 11] * sqrthalf;
		//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
		t5 = z[o + 14] * sqrthalf - z[o + 15] * sqrthalf;
		t6 = z[o + 14] * sqrthalf + z[o + 15] * sqrthalf;
		//   BUTTERFLIES(a0,a1,a2,a3)
		t3 = t5 - t1;
		t5 = t5 + t1;
		z[o + 10] = (float) (z[o + 2] - t5);
		z[o +  2] = (float) (z[o + 2] + t5);
		z[o + 15] = (float) (z[o + 7] - t3);
		z[o +  7] = (float) (z[o + 7] + t3);
		t4 = t2 - t6;
		t6 = t2 + t6;
		z[o + 14] = (float) (z[o + 6] - t4);
		z[o +  6] = (float) (z[o + 6] + t4);
		z[o + 11] = (float) (z[o + 3] - t6);
		z[o +  3] = (float) (z[o + 3] + t6);
	}

	private void pass(float[] z, int o, float[] cos, int n) {
		int o0 = o;
		int o1 = o + 2 * n * 2;
		int o2 = o + 4 * n * 2;
		int o3 = o + 6 * n * 2;
		int wre = 0;
		int wim = 2 * n;
		n--;

		// TRANSFORM_ZERO(z[0],z[o1],z[o2],z[o3]);
		double t1 = z[o2 + 0];
		double t2 = z[o2 + 1];
		double t5 = z[o3 + 0];
		double t6 = z[o3 + 1];
		//   BUTTERFLIES(a0,a1,a2,a3)
		double t3 = t5 - t1;
		t5 = t5 + t1;
		z[o2 + 0] = (float) (z[o0 + 0] - t5);
		z[o0 + 0] = (float) (z[o0 + 0] + t5);
		z[o3 + 1] = (float) (z[o1 + 1] - t3);
		z[o1 + 1] = (float) (z[o1 + 1] + t3);
		double t4 = t2 - t6;
		t6 = t2 + t6;
		z[o3 + 0] = (float) (z[o1 + 0] - t4);
		z[o1 + 0] = (float) (z[o1 + 0] + t4);
		z[o2 + 1] = (float) (z[o0 + 1] - t6);
		z[o0 + 1] = (float) (z[o0 + 1] + t6);
		// TRANSFORM(z[1],z[o1+1],z[o2+1],z[o3+1],wre[1],wim[-1]);
		//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
		t1 =   z[o2 + 2] * cos[wre + 1] + z[o2 + 3] * cos[wim - 1];
		t2 = - z[o2 + 2] * cos[wim - 1] + z[o2 + 3] * cos[wre + 1];
		//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
		t5 = z[o3 + 2] * cos[wre + 1] - z[o3 + 3] * cos[wim - 1];
		t6 = z[o3 + 2] * cos[wim - 1] + z[o3 + 3] * cos[wre + 1];
		//   BUTTERFLIES(a0,a1,a2,a3)
		t3 = t5 - t1;
		t5 = t5 + t1;
		z[o2 + 2] = (float) (z[o0 + 2] - t5);
		z[o0 + 2] = (float) (z[o0 + 2] + t5);
		z[o3 + 3] = (float) (z[o1 + 3] - t3);
		z[o1 + 3] = (float) (z[o1 + 3] + t3);
		t4 = t2 - t6;
		t6 = t2 + t6;
		z[o3 + 2] = (float) (z[o1 + 2] - t4);
		z[o1 + 2] = (float) (z[o1 + 2] + t4);
		z[o2 + 3] = (float) (z[o0 + 3] - t6);
		z[o0 + 3] = (float) (z[o0 + 3] + t6);

		do {
			o0 += 4;
			o1 += 4;
			o2 += 4;
			o3 += 4;
			wre += 2;
			wim -= 2;
			// TRANSFORM(z[0],z[o1],z[o2],z[o3],wre[0],wim[0]);
			//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
			t1 =   z[o2 + 0] * cos[wre] + z[o2 + 1] * cos[wim];
			t2 = - z[o2 + 0] * cos[wim] + z[o2 + 1] * cos[wre];
			//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
			t5 = z[o3 + 0] * cos[wre] - z[o3 + 1] * cos[wim];
			t6 = z[o3 + 0] * cos[wim] + z[o3 + 1] * cos[wre];
			//   BUTTERFLIES(a0,a1,a2,a3)
			t3 = t5 - t1;
			t5 = t5 + t1;
			z[o2 + 0] = (float) (z[o0 + 0] - t5);
			z[o0 + 0] = (float) (z[o0 + 0] + t5);
			z[o3 + 1] = (float) (z[o1 + 1] - t3);
			z[o1 + 1] = (float) (z[o1 + 1] + t3);
			t4 = t2 - t6;
			t6 = t2 + t6;
			z[o3 + 0] = (float) (z[o1 + 0] - t4);
			z[o1 + 0] = (float) (z[o1 + 0] + t4);
			z[o2 + 1] = (float) (z[o0 + 1] - t6);
			z[o0 + 1] = (float) (z[o0 + 1] + t6);
			// TRANSFORM(z[1],z[o1+1],z[o2+1],z[o3+1],wre[1],wim[-1]);
			//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
			t1 =   z[o2 + 2] * cos[wre + 1] + z[o2 + 3] * cos[wim - 1];
			t2 = - z[o2 + 2] * cos[wim - 1] + z[o2 + 3] * cos[wre + 1];
			//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
			t5 = z[o3 + 2] * cos[wre + 1] - z[o3 + 3] * cos[wim - 1];
			t6 = z[o3 + 2] * cos[wim - 1] + z[o3 + 3] * cos[wre + 1];
			//   BUTTERFLIES(a0,a1,a2,a3)
			t3 = t5 - t1;
			t5 = t5 + t1;
			z[o2 + 2] = (float) (z[o0 + 2] - t5);
			z[o0 + 2] = (float) (z[o0 + 2] + t5);
			z[o3 + 3] = (float) (z[o1 + 3] - t3);
			z[o1 + 3] = (float) (z[o1 + 3] + t3);
			t4 = t2 - t6;
			t6 = t2 + t6;
			z[o3 + 2] = (float) (z[o1 + 2] - t4);
			z[o1 + 2] = (float) (z[o1 + 2] + t4);
			z[o2 + 3] = (float) (z[o0 + 3] - t6);
			z[o0 + 3] = (float) (z[o0 + 3] + t6);
		} while (--n != 0);
	}

	private void fft16(float[] z, int o) {
		fft8(z, o);
		fft4(z, o + 16);
		fft4(z, o + 24);
		pass(z, o, ff_cos_16, 2);
	}

	private void fft32(float[] z, int o) {
		fft16(z, o);
		fft8(z, o + 32);
		fft8(z, o + 48);
		pass(z, o, ff_cos_32, 4);
	}

	private void fft64(float[] z, int o) {
		fft32(z, o);
		fft16(z, o + 64);
		fft16(z, o + 96);
		pass(z, o, ff_cos_64, 8);
	}

	private void fft128(float[] z, int o) {
		fft64(z, o);
		fft32(z, o + 128);
		fft32(z, o + 192);
		pass(z, o, ff_cos_128, 16);
	}

	private void fft256(float[] z, int o) {
		fft128(z, o);
		fft64(z, o + 256);
		fft64(z, o + 384);
		pass(z, o, ff_cos_256, 32);
	}

	private void fft512(float[] z, int o) {
		fft256(z, o);
		fft128(z, o + 512);
		fft128(z, o + 768);
		pass(z, o, ff_cos_512, 64);
	}

	public void fftCalcFloat(float[] z, int o) {
		switch (nbits) {
			case 2: fft4  (z, 0); break;
			case 3: fft8  (z, o); break;
			case 4: fft16 (z, 0); break;
			case 5: fft32 (z, 0); break;
			case 6: fft64 (z, o); break;
			case 7: fft128(z, o); break;
			case 8: fft256(z, 0); break;
			case 9: fft512(z, 0); break;
			default:
				log.error(String.format("FFT nbits=%d not implemented", nbits));
				break;
		}
	}

	/**
	 * Compute MDCT of size N = 2^nbits
	 * @param input N samples
	 * @param out N/2 samples
	 */
	public void mdctCalc(float[] output, int outputOffset, float[] input, int inputOffset) {
		int n = 1 << mdctBits;
		int n2 = n >> 1;
		int n4 = n >> 2;
		int n8 = n >> 3;
		int n3 = 3 * n4;

		// pre rotation
		for (int i = 0; i < n8; i++) {
			float re = -input[inputOffset + 2 * i + n3] - input[inputOffset + n3 - 1 - 2 * i];
			float im = -input[inputOffset + n4 + 2 * i] + input[inputOffset + n4 - 1 - 2 * i];
			int j = revtab[i];
			CMUL(output, outputOffset + 2 * j + 0, outputOffset + 2 * j + 1, re, im, -tcos[i], tsin[i]);

			re =  input[inputOffset + 2 * i     ] - input[inputOffset + n2 - 1 - 2 * i];
			im = -input[inputOffset + n2 + 2 * i] - input[inputOffset + n  - 1 - 2 * i];
			j = revtab[n8 + i];
			CMUL(output, outputOffset + 2 * j + 0, outputOffset + 2 * j + 1, re, im, -tcos[n8 + i], tsin[n8 + i]);
		}

		fftCalcFloat(output, outputOffset);

		// post rotation
		final float r[] = new float[4];
		for (int i = 0; i < n8; i++) {
			CMUL(r, 3, 0, output[outputOffset + (n8 - i - 1) * 2 + 0], output[outputOffset + (n8 - i - 1) * 2 + 1], -tsin[n8 - i - 1], -tcos[n8 - i - 1]);
			CMUL(r, 1, 2, output[outputOffset + (n8 + i    ) * 2 + 0], output[outputOffset + (n8 + i    ) * 2 + 1], -tsin[n8 + i    ], -tcos[n8 + i    ]);
			output[outputOffset + (n8 - i - 1) * 2 + 0] = r[0];
			output[outputOffset + (n8 - i - 1) * 2 + 1] = r[1];
			output[outputOffset + (n8 + i    ) * 2 + 0] = r[2];
			output[outputOffset + (n8 + i    ) * 2 + 1] = r[3];
		}
	}
}
