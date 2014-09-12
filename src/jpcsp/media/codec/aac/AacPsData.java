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

import static jpcsp.media.codec.util.CodecUtils.M_PI;
import static jpcsp.media.codec.util.CodecUtils.M_SQRT1_2;
import static jpcsp.media.codec.util.CodecUtils.M_SQRT2;
import static jpcsp.media.codec.util.CodecUtils.atan2f;
import static jpcsp.media.codec.util.CodecUtils.atanf;
import static jpcsp.media.codec.util.CodecUtils.cosf;
import static jpcsp.media.codec.util.CodecUtils.sinf;
import static jpcsp.media.codec.util.CodecUtils.sqrtf;

public class AacPsData {
	public static final int NR_ALLPASS_BANDS20 = 30;
	public static final int NR_ALLPASS_BANDS34 = 50;
	public static final int PS_AP_LINKS = 3;
	public static float pd_re_smooth[] = new float[8*8*8];
	public static float pd_im_smooth[] = new float[8*8*8];
	public static float HA[][][] = new float[46][8][4];
	public static float HB[][][] = new float[46][8][4];
	public static float f20_0_8 [][][] = new float[ 8][8][2];
	public static float f34_0_12[][][] = new float[12][8][2];
	public static float f34_1_8 [][][] = new float[ 8][8][2];
	public static float f34_2_4 [][][] = new float[ 4][8][2];
	public static float Q_fract_allpass[][][][] = new float[2][50][3][2];
	public static float phi_fract[][][] = new float[2][50][2];

	public static final float g0_Q8[] = {
	    0.00746082949812f, 0.02270420949825f, 0.04546865930473f, 0.07266113929591f,
	    0.09885108575264f, 0.11793710567217f, 0.125f
	};

	public static final float g0_Q12[] = {
	    0.04081179924692f, 0.03812810994926f, 0.05144908135699f, 0.06399831151592f,
	    0.07428313801106f, 0.08100347892914f, 0.08333333333333f
	};

	public static final float g1_Q8[] = {
	    0.01565675600122f, 0.03752716391991f, 0.05417891378782f, 0.08417044116767f,
	    0.10307344158036f, 0.12222452249753f, 0.125f
	};

	public static final float g2_Q4[] = {
	    -0.05908211155639f, -0.04871498374946f, 0.0f,   0.07778723915851f,
	     0.16486303567403f,  0.23279856662996f, 0.25f
	};

	static final int huff_iid_df1_bits[] = {
	    18, 18, 18, 18, 18, 18, 18, 18, 18, 17, 18, 17, 17, 16, 16, 15, 14, 14,
	    13, 12, 12, 11, 10, 10,  8,  7,  6,  5,  4,  3,  1,  3,  4,  5,  6,  7,
	     8,  9, 10, 11, 11, 12, 13, 14, 14, 15, 16, 16, 17, 17, 18, 17, 18, 18,
	    18, 18, 18, 18, 18, 18, 18
	};

	static final int huff_iid_df1_codes[] = {
	    0x01FEB4, 0x01FEB5, 0x01FD76, 0x01FD77, 0x01FD74, 0x01FD75, 0x01FE8A,
	    0x01FE8B, 0x01FE88, 0x00FE80, 0x01FEB6, 0x00FE82, 0x00FEB8, 0x007F42,
	    0x007FAE, 0x003FAF, 0x001FD1, 0x001FE9, 0x000FE9, 0x0007EA, 0x0007FB,
	    0x0003FB, 0x0001FB, 0x0001FF, 0x00007C, 0x00003C, 0x00001C, 0x00000C,
	    0x000000, 0x000001, 0x000001, 0x000002, 0x000001, 0x00000D, 0x00001D,
	    0x00003D, 0x00007D, 0x0000FC, 0x0001FC, 0x0003FC, 0x0003F4, 0x0007EB,
	    0x000FEA, 0x001FEA, 0x001FD6, 0x003FD0, 0x007FAF, 0x007F43, 0x00FEB9,
	    0x00FE83, 0x01FEB7, 0x00FE81, 0x01FE89, 0x01FE8E, 0x01FE8F, 0x01FE8C,
	    0x01FE8D, 0x01FEB2, 0x01FEB3, 0x01FEB0, 0x01FEB1
	};

	static final int huff_iid_dt1_bits[] = {
	    16, 16, 16, 16, 16, 16, 16, 16, 16, 15, 15, 15, 15, 15, 15, 14, 14, 13,
	    13, 13, 12, 12, 11, 10,  9,  9,  7,  6,  5,  3,  1,  2,  5,  6,  7,  8,
	     9, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16,
	    16, 16, 16, 16, 16, 16, 16
	};

	static final int huff_iid_dt1_codes[] = {
	    0x004ED4, 0x004ED5, 0x004ECE, 0x004ECF, 0x004ECC, 0x004ED6, 0x004ED8,
	    0x004F46, 0x004F60, 0x002718, 0x002719, 0x002764, 0x002765, 0x00276D,
	    0x0027B1, 0x0013B7, 0x0013D6, 0x0009C7, 0x0009E9, 0x0009ED, 0x0004EE,
	    0x0004F7, 0x000278, 0x000139, 0x00009A, 0x00009F, 0x000020, 0x000011,
	    0x00000A, 0x000003, 0x000001, 0x000000, 0x00000B, 0x000012, 0x000021,
	    0x00004C, 0x00009B, 0x00013A, 0x000279, 0x000270, 0x0004EF, 0x0004E2,
	    0x0009EA, 0x0009D8, 0x0013D7, 0x0013D0, 0x0027B2, 0x0027A2, 0x00271A,
	    0x00271B, 0x004F66, 0x004F67, 0x004F61, 0x004F47, 0x004ED9, 0x004ED7,
	    0x004ECD, 0x004ED2, 0x004ED3, 0x004ED0, 0x004ED1
	};

	static final int huff_iid_df0_bits[] = {
	    17, 17, 17, 17, 16, 15, 13, 10,  9,  7,  6,  5,  4,  3,  1,  3,  4,  5,
	     6,  6,  8, 11, 13, 14, 14, 15, 17, 18, 18
	};

	static final int huff_iid_df0_codes[] = {
	    0x01FFFB, 0x01FFFC, 0x01FFFD, 0x01FFFA, 0x00FFFC, 0x007FFC, 0x001FFD,
	    0x0003FE, 0x0001FE, 0x00007E, 0x00003C, 0x00001D, 0x00000D, 0x000005,
	    0x000000, 0x000004, 0x00000C, 0x00001C, 0x00003D, 0x00003E, 0x0000FE,
	    0x0007FE, 0x001FFC, 0x003FFC, 0x003FFD, 0x007FFD, 0x01FFFE, 0x03FFFE,
	    0x03FFFF
	};

	static final int huff_iid_dt0_bits[] = {
	    19, 19, 19, 20, 20, 20, 17, 15, 12, 10,  8,  6,  4,  2,  1,  3,  5,  7,
	     9, 11, 13, 14, 17, 19, 20, 20, 20, 20, 20
	};

	static final int huff_iid_dt0_codes[] = {
	    0x07FFF9, 0x07FFFA, 0x07FFFB, 0x0FFFF8, 0x0FFFF9, 0x0FFFFA, 0x01FFFD,
	    0x007FFE, 0x000FFE, 0x0003FE, 0x0000FE, 0x00003E, 0x00000E, 0x000002,
	    0x000000, 0x000006, 0x00001E, 0x00007E, 0x0001FE, 0x0007FE, 0x001FFE,
	    0x003FFE, 0x01FFFC, 0x07FFF8, 0x0FFFFB, 0x0FFFFC, 0x0FFFFD, 0x0FFFFE,
	    0x0FFFFF
	};

	static final int huff_icc_df_bits[] = {
	    14, 14, 12, 10, 7, 5, 3, 1, 2, 4, 6, 8, 9, 11, 13
	};

	static final int huff_icc_df_codes[] = {
	    0x3FFF, 0x3FFE, 0x0FFE, 0x03FE, 0x007E, 0x001E, 0x0006, 0x0000,
	    0x0002, 0x000E, 0x003E, 0x00FE, 0x01FE, 0x07FE, 0x1FFE
	};

	static final int huff_icc_dt_bits[] = {
	    14, 13, 11, 9, 7, 5, 3, 1, 2, 4, 6, 8, 10, 12, 14
	};

	static final int huff_icc_dt_codes[] = {
	    0x3FFE, 0x1FFE, 0x07FE, 0x01FE, 0x007E, 0x001E, 0x0006, 0x0000,
	    0x0002, 0x000E, 0x003E, 0x00FE, 0x03FE, 0x0FFE, 0x3FFF
	};

	static final int huff_ipd_df_bits[] = {
	    1, 3, 4, 4, 4, 4, 4, 4
	};

	static final int huff_ipd_df_codes[] = {
	    0x01, 0x00, 0x06, 0x04, 0x02, 0x03, 0x05, 0x07
	};

	static final int huff_ipd_dt_bits[] = {
	    1, 3, 4, 5, 5, 4, 4, 3
	};

	static final int huff_ipd_dt_codes[] = {
	    0x01, 0x02, 0x02, 0x03, 0x02, 0x00, 0x03, 0x03
	};

	static final int huff_opd_df_bits[] = {
	    1, 3, 4, 4, 5, 5, 4, 3
	};

	static final int huff_opd_df_codes[] = {
	    0x01, 0x01, 0x06, 0x04, 0x0F, 0x0E, 0x05, 0x00
	};

	static final int huff_opd_dt_bits[] = {
	    1, 3, 4, 5, 5, 4, 4, 3
	};

	static final int huff_opd_dt_codes[] = {
	    0x01, 0x02, 0x01, 0x07, 0x06, 0x00, 0x02, 0x03
	};

	static final int huff_offset[] = {
	    30, 30,
	    14, 14,
	    7, 7,
	    0, 0,
	    0, 0
	};

	///Table 8.48
	static final int k_to_i_20[] = {
	     1,  0,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 14, 15,
	    15, 15, 16, 16, 16, 16, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18,
	    18, 18, 18, 18, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19,
	    19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19
	};
	///Table 8.49
	static final int k_to_i_34[] = {
	     0,  1,  2,  3,  4,  5,  6,  6,  7,  2,  1,  0, 10, 10,  4,  5,  6,  7,  8,
	     9, 10, 11, 12,  9, 14, 11, 12, 13, 14, 15, 16, 13, 16, 17, 18, 19, 20, 21,
	    22, 22, 23, 23, 24, 24, 25, 25, 26, 26, 27, 27, 27, 28, 28, 28, 29, 29, 29,
	    30, 30, 30, 31, 31, 31, 31, 32, 32, 32, 32, 33, 33, 33, 33, 33, 33, 33, 33,
	    33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33
	};

	static final float g1_Q2[] = {
	    0.0f,  0.01899487526049f, 0.0f, -0.07293139167538f,
	    0.0f,  0.30596630545168f, 0.5f
	};

	private static void make_filters_from_proto(float filter[][][], float proto[], int bands) {
	    for (int q = 0; q < bands; q++) {
	        for (int n = 0; n < 7; n++) {
	            double theta = 2 * Math.PI * (q + 0.5) * (n - 6) / bands;
	            filter[q][n][0] = proto[n] * (float)  Math.cos(theta);
	            filter[q][n][1] = proto[n] * (float) -Math.sin(theta);
	        }
	    }
	}

    private static final float ipdopd_sin[] = { 0, M_SQRT1_2, 1,  M_SQRT1_2,  0, -M_SQRT1_2, -1, -M_SQRT1_2 };
    private static final float ipdopd_cos[] = { 1, M_SQRT1_2, 0, -M_SQRT1_2, -1, -M_SQRT1_2,  0,  M_SQRT1_2 };
    private static final float iid_par_dequant[] = {
        //iid_par_dequant_default
        0.05623413251903f, 0.12589254117942f, 0.19952623149689f, 0.31622776601684f,
        0.44668359215096f, 0.63095734448019f, 0.79432823472428f, 1f,
        1.25892541179417f, 1.58489319246111f, 2.23872113856834f, 3.16227766016838f,
        5.01187233627272f, 7.94328234724282f, 17.7827941003892f,
        //iid_par_dequant_fine
        0.00316227766017f, 0.00562341325190f, 0.01f,             0.01778279410039f,
        0.03162277660168f, 0.05623413251903f, 0.07943282347243f, 0.11220184543020f,
        0.15848931924611f, 0.22387211385683f, 0.31622776601684f, 0.39810717055350f,
        0.50118723362727f, 0.63095734448019f, 0.79432823472428f, 1f,
        1.25892541179417f, 1.58489319246111f, 1.99526231496888f, 2.51188643150958f,
        3.16227766016838f, 4.46683592150963f, 6.30957344480193f, 8.91250938133745f,
        12.5892541179417f, 17.7827941003892f, 31.6227766016838f, 56.2341325190349f,
        100f,              177.827941003892f, 316.227766016837f,
    };
    private static final float icc_invq[] = {
        1f, 0.937f,      0.84118f,    0.60092f,    0.36764f,   0f,      -0.589f,    -1f
    };
    private static final float acos_icc_invq[] = {
        0f, 0.35685527f, 0.57133466f, 0.92614472f, 1.1943263f, (float) Math.PI/2, 2.2006171f, (float) Math.PI
    };
    private static final int f_center_20[] = {
        -3, -1, 1, 3, 5, 7, 10, 14, 18, 22
    };
    private static final int f_center_34[] = {
         2,  6, 10, 14, 18, 22, 26, 30,
        34,-10, -6, -2, 51, 57, 15, 21,
        27, 33, 39, 45, 54, 66, 78, 42,
       102, 66, 78, 90,102,114,126, 90
    };
    private static final float fractional_delay_links[] = { 0.43f, 0.75f, 0.347f };

    public static void tableinit() {
	    final float fractional_delay_gain = 0.39f;

	    for (int pd0 = 0; pd0 < 8; pd0++) {
	        float pd0_re = ipdopd_cos[pd0];
	        float pd0_im = ipdopd_sin[pd0];
	        for (int pd1 = 0; pd1 < 8; pd1++) {
	            float pd1_re = ipdopd_cos[pd1];
	            float pd1_im = ipdopd_sin[pd1];
	            for (int pd2 = 0; pd2 < 8; pd2++) {
	                float pd2_re = ipdopd_cos[pd2];
	                float pd2_im = ipdopd_sin[pd2];
	                float re_smooth = 0.25f * pd0_re + 0.5f * pd1_re + pd2_re;
	                float im_smooth = 0.25f * pd0_im + 0.5f * pd1_im + pd2_im;
	                float pd_mag = 1 / sqrtf(im_smooth * im_smooth + re_smooth * re_smooth);
	                pd_re_smooth[pd0*64+pd1*8+pd2] = re_smooth * pd_mag;
	                pd_im_smooth[pd0*64+pd1*8+pd2] = im_smooth * pd_mag;
	            }
	        }
	    }

	    for (int iid = 0; iid < 46; iid++) {
	        float c = iid_par_dequant[iid]; ///< Linear Inter-channel Intensity Difference
	        float c1 = (float)M_SQRT2 / sqrtf(1.0f + c*c);
	        float c2 = c * c1;
	        for (int icc = 0; icc < 8; icc++) {
	            /*if (PS_BASELINE || ps->icc_mode < 3)*/ {
	                float alpha = 0.5f * acos_icc_invq[icc];
	                float beta  = alpha * (c1 - c2) * (float)M_SQRT1_2;
	                HA[iid][icc][0] = c2 * cosf(beta + alpha);
	                HA[iid][icc][1] = c1 * cosf(beta - alpha);
	                HA[iid][icc][2] = c2 * sinf(beta + alpha);
	                HA[iid][icc][3] = c1 * sinf(beta - alpha);
	            } /* else */ {
	                float alpha, gamma, mu, rho;
	                float alpha_c, alpha_s, gamma_c, gamma_s;
	                rho = Math.max(icc_invq[icc], 0.05f);
	                alpha = 0.5f * atan2f(2.0f * c * rho, c*c - 1.0f);
	                mu = c + 1.0f / c;
	                mu = sqrtf(1 + (4 * rho * rho - 4)/(mu * mu));
	                gamma = atanf(sqrtf((1.0f - mu)/(1.0f + mu)));
	                if (alpha < 0) alpha += M_PI/2;
	                alpha_c = cosf(alpha);
	                alpha_s = sinf(alpha);
	                gamma_c = cosf(gamma);
	                gamma_s = sinf(gamma);
	                HB[iid][icc][0] =  M_SQRT2 * alpha_c * gamma_c;
	                HB[iid][icc][1] =  M_SQRT2 * alpha_s * gamma_c;
	                HB[iid][icc][2] = -M_SQRT2 * alpha_s * gamma_s;
	                HB[iid][icc][3] =  M_SQRT2 * alpha_c * gamma_s;
	            }
	        }
	    }

	    for (int k = 0; k < NR_ALLPASS_BANDS20; k++) {
	        double f_center, theta;
	        if (k < f_center_20.length) {
	            f_center = f_center_20[k] * 0.125;
	        } else {
	            f_center = k - 6.5f;
	        }
	        for (int m = 0; m < PS_AP_LINKS; m++) {
	            theta = -M_PI * fractional_delay_links[m] * f_center;
	            Q_fract_allpass[0][k][m][0] = (float) Math.cos(theta);
	            Q_fract_allpass[0][k][m][1] = (float) Math.sin(theta);
	        }
	        theta = -M_PI*fractional_delay_gain*f_center;
	        phi_fract[0][k][0] = (float) Math.cos(theta);
	        phi_fract[0][k][1] = (float) Math.sin(theta);
	    }
	    for (int k = 0; k < NR_ALLPASS_BANDS34; k++) {
	        double f_center, theta;
	        if (k < f_center_34.length) {
	            f_center = f_center_34[k] / 24.0;
	        } else {
	            f_center = k - 26.5f;
	        }
	        for (int m = 0; m < PS_AP_LINKS; m++) {
	            theta = -M_PI * fractional_delay_links[m] * f_center;
	            Q_fract_allpass[1][k][m][0] = (float) Math.cos(theta);
	            Q_fract_allpass[1][k][m][1] = (float) Math.sin(theta);
	        }
	        theta = -M_PI*fractional_delay_gain*f_center;
	        phi_fract[1][k][0] = (float) Math.cos(theta);
	        phi_fract[1][k][1] = (float) Math.sin(theta);
	    }

	    make_filters_from_proto(f20_0_8,  g0_Q8,   8);
	    make_filters_from_proto(f34_0_12, g0_Q12, 12);
	    make_filters_from_proto(f34_1_8,  g1_Q8,   8);
	    make_filters_from_proto(f34_2_4,  g2_Q4,   4);
	}
}
