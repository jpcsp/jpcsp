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

import static jpcsp.media.codec.aac.PSContext.PS_AP_LINKS;

public class PSDSP {
	private static void stereoInterpolate(float l[][], int lOffset, float r[][], int rOffset, float h[][], float h_step[][], int len) {
	    float h0 = h[0][0];
	    float h1 = h[0][1];
	    float h2 = h[0][2];
	    float h3 = h[0][3];
	    float hs0 = h_step[0][0];
	    float hs1 = h_step[0][1];
	    float hs2 = h_step[0][2];
	    float hs3 = h_step[0][3];
	    int n;

	    for (n = 0; n < len; n++) {
	        //l is s, r is d
	        float l_re = l[lOffset + n][0];
	        float l_im = l[lOffset + n][1];
	        float r_re = r[rOffset + n][0];
	        float r_im = r[rOffset + n][1];
	        h0 += hs0;
	        h1 += hs1;
	        h2 += hs2;
	        h3 += hs3;
	        l[lOffset + n][0] = h0 * l_re + h2 * r_re;
	        l[lOffset + n][1] = h0 * l_im + h2 * r_im;
	        r[rOffset + n][0] = h1 * l_re + h3 * r_re;
	        r[rOffset + n][1] = h1 * l_im + h3 * r_im;
	    }
	}

	private static void stereoInterpolateIpdopd(float l[][], int lOffset, float r[][], int rOffset, float h[][], float h_step[][], int len) {
	    float h00  = h[0][0],      h10  = h[1][0];
	    float h01  = h[0][1],      h11  = h[1][1];
	    float h02  = h[0][2],      h12  = h[1][2];
	    float h03  = h[0][3],      h13  = h[1][3];
	    float hs00 = h_step[0][0], hs10 = h_step[1][0];
	    float hs01 = h_step[0][1], hs11 = h_step[1][1];
	    float hs02 = h_step[0][2], hs12 = h_step[1][2];
	    float hs03 = h_step[0][3], hs13 = h_step[1][3];
	    int n;

	    for (n = 0; n < len; n++) {
	        //l is s, r is d
	        float l_re = l[lOffset + n][0];
	        float l_im = l[lOffset + n][1];
	        float r_re = r[rOffset + n][0];
	        float r_im = r[rOffset + n][1];
	        h00 += hs00;
	        h01 += hs01;
	        h02 += hs02;
	        h03 += hs03;
	        h10 += hs10;
	        h11 += hs11;
	        h12 += hs12;
	        h13 += hs13;

	        l[lOffset + n][0] = h00 * l_re + h02 * r_re - h10 * l_im - h12 * r_im;
	        l[lOffset + n][1] = h00 * l_im + h02 * r_im + h10 * l_re + h12 * r_re;
	        r[rOffset + n][0] = h01 * l_re + h03 * r_re - h11 * l_im - h13 * r_im;
	        r[rOffset + n][1] = h01 * l_im + h03 * r_im + h11 * l_re + h13 * r_re;
	    }
	}

	public static void stereoInterpolate(float l[][], int lOffset, float r[][], int rOffset, float h[][], float h_step[][], int len, boolean ipdopd) {
		if (ipdopd) {
			stereoInterpolateIpdopd(l, lOffset, r, rOffset, h, h_step, len);
		} else {
			stereoInterpolate(l, lOffset, r, rOffset, h, h_step, len);
		}
	}

	public static void add_squares(float dst[], int dstOffset, final float src[][], int srcOffset, int n) {
	    for (int i = 0; i < n; i++) {
	        dst[dstOffset + i] += src[srcOffset + i][0] * src[srcOffset + i][0] + src[srcOffset + i][1] * src[srcOffset + i][1];
	    }
	}

	public static void mul_pair_single(float dst[][], int dstOffset, float src0[][], int src0Offset, float src1[], int src1Offset, int n) {
	    for (int i = 0; i < n; i++) {
	        dst[dstOffset + i][0] = src0[src0Offset + i][0] * src1[src1Offset + i];
	        dst[dstOffset + i][1] = src0[src0Offset + i][1] * src1[src1Offset + i];
	    }
	}

	public static void hybrid_analysis(float out[][], int outOffset, float in[][], int inOffset, float filter[][][], int filterOffset, int stride, int n) {
	    for (int i = 0; i < n; i++) {
	        float sum_re = filter[filterOffset + i][6][0] * in[inOffset + 6][0];
	        float sum_im = filter[filterOffset + i][6][0] * in[inOffset + 6][1];

	        for (int j = 0; j < 6; j++) {
	            float in0_re = in[inOffset + j][0];
	            float in0_im = in[inOffset + j][1];
	            float in1_re = in[inOffset + 12-j][0];
	            float in1_im = in[inOffset + 12-j][1];
	            sum_re += filter[filterOffset + i][j][0] * (in0_re + in1_re) -
	                      filter[filterOffset + i][j][1] * (in0_im - in1_im);
	            sum_im += filter[filterOffset + i][j][0] * (in0_im + in1_im) +
	                      filter[filterOffset + i][j][1] * (in0_re - in1_re);
	        }
	        out[outOffset + i * stride][0] = sum_re;
	        out[outOffset + i * stride][1] = sum_im;
	    }
	}

	public static void hybrid_analysis_ileave(float out[][][], int outOffset, float L[][][], int Loffset, int i, int len) {
	    for (; i < 64; i++) {
	        for (int j = 0; j < len; j++) {
	            out[outOffset + i][j][0] = L[Loffset + 0][j][i];
	            out[outOffset + i][j][1] = L[Loffset + 1][j][i];
	        }
	    }
	}

	public static void hybrid_synthesis_deint(float out[][][], float in[][][], int inOffset, int i, int len) {
	    for (; i < 64; i++) {
	        for (int n = 0; n < len; n++) {
	            out[0][n][i] = in[inOffset + i][n][0];
	            out[1][n][i] = in[inOffset + i][n][1];
	        }
	    }
	}

    private static final float a[] = {
    	0.65143905753106f,
        0.56471812200776f,
        0.48954165955695f
    };

    public static void decorrelate(float out[][], int outOffset, float delay[][], int delayOffset, float ap_delay[][][], float phi_fract[], float Q_fract[][], float transient_gain[], float g_decay_slope, int len) {
	    float ag[] = new float[PS_AP_LINKS];

	    for (int m = 0; m < PS_AP_LINKS; m++) {
	        ag[m] = a[m] * g_decay_slope;
	    }

	    for (int n = 0; n < len; n++) {
	        float in_re = delay[delayOffset + n][0] * phi_fract[0] - delay[delayOffset + n][1] * phi_fract[1];
	        float in_im = delay[delayOffset + n][0] * phi_fract[1] + delay[delayOffset + n][1] * phi_fract[0];
	        for (int m = 0; m < PS_AP_LINKS; m++) {
	            float a_re                = ag[m] * in_re;
	            float a_im                = ag[m] * in_im;
	            float link_delay_re       = ap_delay[m][n+2-m][0];
	            float link_delay_im       = ap_delay[m][n+2-m][1];
	            float fractional_delay_re = Q_fract[m][0];
	            float fractional_delay_im = Q_fract[m][1];
	            float apd_re = in_re;
	            float apd_im = in_im;
	            in_re = link_delay_re * fractional_delay_re -
	                    link_delay_im * fractional_delay_im - a_re;
	            in_im = link_delay_re * fractional_delay_im +
	                    link_delay_im * fractional_delay_re - a_im;
	            ap_delay[m][n+5][0] = apd_re + ag[m] * in_re;
	            ap_delay[m][n+5][1] = apd_im + ag[m] * in_im;
	        }
	        out[outOffset + n][0] = transient_gain[n] * in_re;
	        out[outOffset + n][1] = transient_gain[n] * in_im;
	    }
	}
}
