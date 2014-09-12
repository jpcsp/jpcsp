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
package jpcsp.media.codec.aac;

import static jpcsp.media.codec.aac.AacSbrData.ff_sbr_noise_table;

import org.apache.log4j.Logger;

public class SBRDSP {
	private static Logger log = AacDecoder.log;

	public static void sum64x5(float z[], int o) {
	    for (int k = 0; k < 64; k++) {
	        float f = z[o + k] + z[o + k + 64] + z[o + k + 128] + z[o + k + 192] + z[o + k + 256];
	        z[o + k] = f;
	    }
	}

	public static void qmfPreShuffle(float z[], int o) {
		z[o + 64] = z[o + 0];
		z[o + 65] = z[o + 1];
		for (int k = 1; k < 31; k += 2) {
			z[o + 64 + 2 * k + 0] = -z[o + 64 - k];
			z[o + 64 + 2 * k + 1] =  z[o +  k + 1];
			z[o + 64 + 2 * k + 2] = -z[o + 63 - k];
			z[o + 64 + 2 * k + 3] =  z[o +  k + 2];
		}

		z[o + 64 + 2 * 31 + 0] = -z[o + 64 - 31];
		z[o + 64 + 2 * 31 + 1] =  z[o + 31 +  1];
	}

	public static void qmfPostShuffle(float W[][], float z[], int o) {
		for (int k = 0; k < 32; k += 2) {
			W[k    ][0] = -z[o + 63 - k];
			W[k    ][1] =  z[o +  k + 0];
			W[k + 1][0] = -z[o + 62 - k];
			W[k + 1][1] =  z[o +  k + 1];
		}
	}

	public static void autocorrelate(float x[][], float phi[][][]) {
	    float real_sum2 = x[0][0] * x[2][0] + x[0][1] * x[2][1];
	    float imag_sum2 = x[0][0] * x[2][1] - x[0][1] * x[2][0];
	    float real_sum1 = 0.0f, imag_sum1 = 0.0f, real_sum0 = 0.0f;
	    for (int i = 1; i < 38; i++) {
	        real_sum0 += x[i][0] * x[i    ][0] + x[i][1] * x[i    ][1];
	        real_sum1 += x[i][0] * x[i + 1][0] + x[i][1] * x[i + 1][1];
	        imag_sum1 += x[i][0] * x[i + 1][1] - x[i][1] * x[i + 1][0];
	        real_sum2 += x[i][0] * x[i + 2][0] + x[i][1] * x[i + 2][1];
	        imag_sum2 += x[i][0] * x[i + 2][1] - x[i][1] * x[i + 2][0];
	    }
	    phi[2 - 2][1][0] = real_sum2;
	    phi[2 - 2][1][1] = imag_sum2;
	    phi[2    ][1][0] = real_sum0 + x[ 0][0] * x[ 0][0] + x[ 0][1] * x[ 0][1];
	    phi[1    ][0][0] = real_sum0 + x[38][0] * x[38][0] + x[38][1] * x[38][1];
	    phi[2 - 1][1][0] = real_sum1 + x[ 0][0] * x[ 1][0] + x[ 0][1] * x[ 1][1];
	    phi[2 - 1][1][1] = imag_sum1 + x[ 0][0] * x[ 1][1] - x[ 0][1] * x[ 1][0];
	    phi[0    ][0][0] = real_sum1 + x[38][0] * x[39][0] + x[38][1] * x[39][1];
	    phi[0    ][0][1] = imag_sum1 + x[38][0] * x[39][1] - x[38][1] * x[39][0];
	}

	public static void qmfDeintNeg(float v[], int vOffset, final float src[], int srcOffset) {
	    for (int i = 0; i < 32; i++) {
	        v[vOffset +      i] =  src[srcOffset + 63 - 2 * i    ];
	        v[vOffset + 63 - i] = -src[srcOffset + 63 - 2 * i - 1];
	    }
	}

	public static void qmfDeintBfly(float v[], int vOffset, final float src0[], int src0Offset, final float src1[], int src1Offset) {
	    for (int i = 0; i < 64; i++) {
	        v[vOffset +       i] = src0[src0Offset + i] - src1[src1Offset + 63 - i];
	        v[vOffset + 127 - i] = src0[src0Offset + i] + src1[src1Offset + 63 - i];
	    }
	}

	public static void negOdd64(float x[], int o) {
	    for (int i = 1; i < 64; i += 4) {
	        x[o + i + 0] = -x[o + i + 0];
	        x[o + i + 2] = -x[o + i + 2];
	    }
	}

	public static void hf_gen(float X_high[][], int XhighOffset, final float X_low[][], int XlowOffset, final float alpha0[], final float alpha1[], float bw, int start, int end) {
		final float alpha[] = new float[4];

		alpha[0] = alpha1[0] * bw * bw;
		alpha[1] = alpha1[1] * bw * bw;
		alpha[2] = alpha0[0] * bw;
		alpha[3] = alpha0[1] * bw;

		for (int i = start; i < end; i++) {
			X_high[XhighOffset + i][0] =
				X_low[XlowOffset + i - 2][0] * alpha[0] -
				X_low[XlowOffset + i - 2][1] * alpha[1] +
				X_low[XlowOffset + i - 1][0] * alpha[2] -
				X_low[XlowOffset + i - 1][1] * alpha[3] +
				X_low[XlowOffset + i][0];
			X_high[XhighOffset + i][1] =
				X_low[XlowOffset + i - 2][1] * alpha[0] +
				X_low[XlowOffset + i - 2][0] * alpha[1] +
				X_low[XlowOffset + i - 1][1] * alpha[2] +
				X_low[XlowOffset + i - 1][0] * alpha[3] +
				X_low[XlowOffset + i][1];
		}
	}

	public static float sum_square(float x[][], int o, int n) {
	    float sum0 = 0.0f, sum1 = 0.0f;

	    for (int i = 0; i < n; i += 2)
	    {
	        sum0 += x[o + i + 0][0] * x[o + i + 0][0];
	        sum1 += x[o + i + 0][1] * x[o + i + 0][1];
	        sum0 += x[o + i + 1][0] * x[o + i + 1][0];
	        sum1 += x[o + i + 1][1] * x[o + i + 1][1];
	    }

	    return sum0 + sum1;
	}

	public static void hfGFilt(float Y[][], int Yoffset, final float Xhigh[][][], int Xoffset, final float gFilt[], int mMax, int ixh) {
	    for (int m = 0; m < mMax; m++) {
	        Y[Yoffset + m][0] = Xhigh[Xoffset + m][ixh][0] * gFilt[m];
	        Y[Yoffset + m][1] = Xhigh[Xoffset + m][ixh][1] * gFilt[m];
	    }
	}

	private static void hf_apply_noise(float Y[][], int Yoffset, float s_m[], float q_filt[], int noise, float phi_sign0, float phi_sign1, int m_max) {
	    for (int m = 0; m < m_max; m++) {
	        float y0 = Y[Yoffset + m][0];
	        float y1 = Y[Yoffset + m][1];
	        noise = (noise + 1) & 0x1ff;
	        if (s_m[m] != 0f) {
	            y0 += s_m[m] * phi_sign0;
	            y1 += s_m[m] * phi_sign1;
	        } else {
	            y0 += q_filt[m] * ff_sbr_noise_table[noise][0];
	            y1 += q_filt[m] * ff_sbr_noise_table[noise][1];
	        }
	        Y[Yoffset + m][0] = y0;
	        Y[Yoffset + m][1] = y1;
	        phi_sign1 = -phi_sign1;
	    }
	}

	public static void hf_apply_noise(float Y[][], int Yoffset, float s_m[], float q_filt[], int noise, int kx, int m_max, int indexSine) {
		float phiSign;

		switch (indexSine) {
			case 0:
				hf_apply_noise(Y, Yoffset, s_m, q_filt, noise, 1.0f, 0.0f, m_max);
				break;
			case 1:
				phiSign = 1 - 2 * (kx & 1);
				hf_apply_noise(Y, Yoffset, s_m, q_filt, noise, 0.0f, phiSign, m_max);
				break;
			case 2:
				hf_apply_noise(Y, Yoffset, s_m, q_filt, noise, -1.0f, 0.0f, m_max);
				break;
			case 3:
				phiSign = 1 - 2 * (kx & 1);
				hf_apply_noise(Y, Yoffset, s_m, q_filt, noise, 0.0f, -phiSign, m_max);
				break;
			default:
				log.error(String.format("SBRDSP.hf_apply_noise unknown indexSine %d", indexSine));
				break;
		}
	}
}
