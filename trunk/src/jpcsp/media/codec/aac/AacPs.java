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

import static jpcsp.media.codec.aac.AacPsData.HA;
import static jpcsp.media.codec.aac.AacPsData.HB;
import static jpcsp.media.codec.aac.AacPsData.Q_fract_allpass;
import static jpcsp.media.codec.aac.AacPsData.f20_0_8;
import static jpcsp.media.codec.aac.AacPsData.f34_0_12;
import static jpcsp.media.codec.aac.AacPsData.f34_1_8;
import static jpcsp.media.codec.aac.AacPsData.f34_2_4;
import static jpcsp.media.codec.aac.AacPsData.g1_Q2;
import static jpcsp.media.codec.aac.AacPsData.huff_icc_df_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_icc_df_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_icc_dt_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_icc_dt_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_df0_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_df0_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_df1_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_df1_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_dt0_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_dt0_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_dt1_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_iid_dt1_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_ipd_df_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_ipd_df_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_ipd_dt_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_ipd_dt_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_offset;
import static jpcsp.media.codec.aac.AacPsData.huff_opd_df_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_opd_df_codes;
import static jpcsp.media.codec.aac.AacPsData.huff_opd_dt_bits;
import static jpcsp.media.codec.aac.AacPsData.huff_opd_dt_codes;
import static jpcsp.media.codec.aac.AacPsData.k_to_i_20;
import static jpcsp.media.codec.aac.AacPsData.k_to_i_34;
import static jpcsp.media.codec.aac.AacPsData.pd_im_smooth;
import static jpcsp.media.codec.aac.AacPsData.pd_re_smooth;
import static jpcsp.media.codec.aac.AacPsData.phi_fract;
import static jpcsp.media.codec.aac.PSContext.PS_AP_LINKS;
import static jpcsp.media.codec.aac.PSContext.PS_MAX_DELAY;
import static jpcsp.media.codec.aac.PSContext.PS_MAX_NR_IIDICC;
import static jpcsp.media.codec.aac.PSContext.PS_MAX_NUM_ENV;
import static jpcsp.media.codec.aac.PSContext.PS_QMF_TIME_SLOTS;
import static jpcsp.util.Utilities.clipf;

import java.util.Arrays;

import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.CodecUtils;
import jpcsp.media.codec.util.VLC;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class AacPs {
	private static Logger log = AacDecoder.log;

	private static final int numQMFSlots = 32; // numTimeSlots * RATE

	private static final int numEnv_tab[][] = {
	    { 0, 1, 2, 4, },
	    { 1, 2, 3, 4, },
	};

	private static final int nr_iidiccPar_tab[] = {
	    10, 20, 34, 10, 20, 34,
	};

	private static final int nr_iidopdPar_tab[] = {
	     5, 11, 17,  5, 11, 17,
	};

    private static final int huff_iid_df1 = 0;
    private static final int huff_iid_dt1 = 1;
    private static final int huff_iid_df0 = 2;
    private static final int huff_iid_dt0 = 3;
    private static final int huff_icc_df  = 4;
    private static final int huff_icc_dt  = 5;
    private static final int huff_ipd_df  = 6;
    private static final int huff_ipd_dt  = 7;
    private static final int huff_opd_df  = 8;
    private static final int huff_opd_dt  = 9;

	private static final int huff_iid[] = {
	    huff_iid_df0,
	    huff_iid_df1,
	    huff_iid_dt0,
	    huff_iid_dt1
	};

	private static VLC vlc_ps[] = new VLC[10];


	public static void init() {
		for (int i = 0; i < vlc_ps.length; i++) {
			vlc_ps[i] = new VLC();
		}
		vlc_ps[0].initVLCSparse(9, huff_iid_df1_codes.length, huff_iid_df1_bits, huff_iid_df1_codes, null);
		vlc_ps[1].initVLCSparse(9, huff_iid_dt1_codes.length, huff_iid_dt1_bits, huff_iid_dt1_codes, null);
		vlc_ps[2].initVLCSparse(9, huff_iid_df0_codes.length, huff_iid_df0_bits, huff_iid_df0_codes, null);
		vlc_ps[3].initVLCSparse(9, huff_iid_dt0_codes.length, huff_iid_dt0_bits, huff_iid_dt0_codes, null);
		vlc_ps[4].initVLCSparse(9, huff_icc_df_codes.length,  huff_icc_df_bits,  huff_icc_df_codes,  null);
		vlc_ps[5].initVLCSparse(9, huff_icc_dt_codes.length,  huff_icc_dt_bits,  huff_icc_dt_codes,  null);
		vlc_ps[6].initVLCSparse(9, huff_ipd_df_codes.length,  huff_ipd_df_bits,  huff_ipd_df_codes,  null);
		vlc_ps[7].initVLCSparse(9, huff_ipd_dt_codes.length,  huff_ipd_dt_bits,  huff_ipd_dt_codes,  null);
		vlc_ps[8].initVLCSparse(9, huff_opd_df_codes.length,  huff_opd_df_bits,  huff_opd_df_codes,  null);
		vlc_ps[9].initVLCSparse(9, huff_opd_dt_codes.length,  huff_opd_dt_bits,  huff_opd_dt_codes,  null);
	}

	private static int readDataError(Context ac, PSContext ps, int bitCountStart, int bitsLeft) {
		ps.start = false;
		int bitsRead = ac.br.getBitsRead() - bitCountStart;
		if (bitsRead < bitsLeft) {
			ac.br.skip(bitsLeft - bitsRead);
		}
		Utilities.fill(ps.iidPar, 0);
		Utilities.fill(ps.iccPar, 0);
		Utilities.fill(ps.ipdPar, 0);
		Utilities.fill(ps.opdPar, 0);

		return bitsLeft;
	}

	private static int readParData(Context ac, PSContext ps, int par[][], int tableIdx, int e, boolean dt, int num, int offset, int mask) {
		VLC vlc = vlc_ps[tableIdx];
		if (dt) {
			int ePrev = e != 0 ? e - 1 : ps.numEnvOld - 1;
			ePrev = Math.max(ePrev, 0);
			for (int b = 0; b < num; b++) {
				int val = par[ePrev][b] + vlc.getVLC2(ac.br, 3) - offset;
				if (mask != 0) {
					val &= mask;
				}
				par[e][b] = val;
			}
		} else {
			int val = 0;
			for (int b = 0; b < num; b++) {
				val += vlc.getVLC2(ac.br, 3) - offset;
				if (mask != 0) {
					val &= mask;
				}
				par[e][b] = val;
			}
		}

		return 0;
	}

	private static int readIidData(Context ac, PSContext ps, int iid[][], int tableIdx, int e, boolean dt) {
		return readParData(ac, ps, iid, tableIdx, e, dt, ps.nrIidPar, huff_offset[tableIdx], 0);
	}

	private static int readIccData(Context ac, PSContext ps, int iid[][], int tableIdx, int e, boolean dt) {
		return readParData(ac, ps, iid, tableIdx, e, dt, ps.nrIccPar, huff_offset[tableIdx], 0);
	}

	private static int readIpdopdData(Context ac, PSContext ps, int iid[][], int tableIdx, int e, boolean dt) {
		return readParData(ac, ps, iid, tableIdx, e, dt, ps.nrIpdopdPar, 0, 0x7);
	}

	private static int readExtensionData(Context ac, PSContext ps, int psExtensionId) {
		boolean dt;
		int count = ac.br.getBitsRead();

		if (psExtensionId != 0) {
			return 0;
		}

		ps.enableIpdopd = ac.br.readBool();
		if (ps.enableIpdopd) {
			for (int e = 0; e < ps.numEnv; e++) {
				dt = ac.br.readBool();
				readIpdopdData(ac, ps, ps.ipdPar, dt ? huff_ipd_dt : huff_ipd_df, e, dt);
				dt = ac.br.readBool();
				readIpdopdData(ac, ps, ps.opdPar, dt ? huff_opd_dt : huff_opd_df, e, dt);
			}
		}
		ac.br.skip(1); // reserved_ps

		return ac.br.getBitsRead() - count;
	}

	public static int readData(Context ac, PSContext ps, int bitsLeft) {
		BitReader br = ac.br;
		int bitCountStart = br.getBitsRead();

		boolean header = br.readBool();
		if (header) { // enable_ps_header
			ps.enableIid = br.readBool();
			if (ps.enableIid) {
				int iidMode = br.read(3);
				if (iidMode > 5) {
					log.error(String.format("iidMode %d is reserved", iidMode));
					return readDataError(ac, ps, bitCountStart, bitsLeft);
				}
				ps.nrIidPar = nr_iidiccPar_tab[iidMode];
				ps.iidQuant = iidMode > 2 ? 1 : 0;
				ps.nrIpdopdPar = nr_iidopdPar_tab[iidMode];
			}
			ps.enableIcc = br.readBool();
			if (ps.enableIcc) {
				ps.iccMode = br.read(3);
				if (ps.iccMode > 5) {
					log.error(String.format("iic_mode %d is reserved", ps.iccMode));
					return readDataError(ac, ps, bitCountStart, bitsLeft);
				}
				ps.nrIccPar = nr_iidiccPar_tab[ps.iccMode];
			}
			ps.enableExt = br.readBool();
		}

		ps.frameClass = br.read1();
		ps.numEnvOld = ps.numEnv;
		ps.numEnv = numEnv_tab[ps.frameClass][br.read(2)];

		ps.borderPosition[0] = -1;
		if (ps.frameClass != 0) {
			for (int e = 1; e <= ps.numEnv; e++) {
				ps.borderPosition[e] = br.read(5);
			}
		} else {
			for (int e = 1; e <= ps.numEnv; e++) {
				ps.borderPosition[e] = (e * numQMFSlots >> CodecUtils.ff_log2_tab[ps.numEnv]) - 1;
			}
		}

		if (ps.enableIid) {
			for (int e = 0; e < ps.numEnv; e++) {
				boolean dt = br.readBool();
				if (readIidData(ac, ps, ps.iidPar, huff_iid[(dt ? 2 : 0) + ps.iidQuant], e, dt) != 0) {
					return readDataError(ac, ps, bitCountStart, bitsLeft);
				}
			}
		} else {
			Utilities.fill(ps.iidPar, 0);
		}

		if (ps.enableIcc) {
			for (int e = 0; e < ps.numEnv; e++) {
				boolean dt = br.readBool();
				if (readIccData(ac, ps, ps.iccPar, dt ? huff_icc_dt : huff_icc_df, e, dt) != 0) {
					return readDataError(ac, ps, bitCountStart, bitsLeft);
				}
			}
		} else {
			Utilities.fill(ps.iccPar, 0);
		}

		if (ps.enableExt) {
			int cnt = br.read(4);
			if (cnt == 15) {
				cnt += br.read(8);
			}
			cnt *= 8;
			while (cnt > 7) {
				int psExtensionId = br.read(2);
				cnt -= 2 + readExtensionData(ac, ps, psExtensionId);
			}
			if (cnt < 0) {
				log.error(String.format("ps extension overflow %d", cnt));
				return readDataError(ac, ps, bitCountStart, bitsLeft);
			}
			br.skip(cnt);
		}

		// Fix up envelopes
		if (ps.numEnv == 0 || ps.borderPosition[ps.numEnv] < numQMFSlots - 1) {
			// Create a fake envelope
			int source = ps.numEnv != 0 ? ps.numEnv - 1 : ps.numEnvOld - 1;
			if (source >= 0 && source != ps.numEnv) {
				if (ps.enableIid) {
					System.arraycopy(ps.iidPar[source], 0, ps.iidPar[ps.numEnv], 0, ps.iidPar[0].length);
				}
				if (ps.enableIcc) {
					System.arraycopy(ps.iccPar[source], 0, ps.iccPar[ps.numEnv], 0, ps.iccPar[0].length);
				}
				if (ps.enableIpdopd) {
					System.arraycopy(ps.ipdPar[source], 0, ps.ipdPar[ps.numEnv], 0, ps.ipdPar[0].length);
					System.arraycopy(ps.opdPar[source], 0, ps.opdPar[ps.numEnv], 0, ps.opdPar[0].length);
				}
			}
			if (ps.enableIid) {
				for (int b = 0; b < ps.nrIidPar; b++) {
					if (Math.abs(ps.iidPar[ps.numEnv][b]) > 7 + 8 * ps.iidQuant) {
						log.error(String.format("iidPar invalid"));
						return readDataError(ac, ps, bitCountStart, bitsLeft);
					}
				}
			}
			if (ps.enableIcc) {
				for (int b = 0; b < ps.nrIidPar; b++) {
					if (Math.abs(ps.iccPar[ps.numEnv][b]) > 7) {
						log.error(String.format("iccPar invalid"));
						return readDataError(ac, ps, bitCountStart, bitsLeft);
					}
				}
			}
			ps.numEnv++;
			ps.borderPosition[ps.numEnv] = numQMFSlots - 1;
		}

		ps.is34bandsOld = ps.is34bands;
		if (ps.enableIid || ps.enableIcc) {
			ps.is34bands = (ps.enableIid && ps.nrIidPar == 34) || (ps.enableIcc && ps.nrIccPar == 34);
		}

		// Baseline
		if (!ps.enableIpdopd) {
			Utilities.fill(ps.ipdPar, 0);
			Utilities.fill(ps.opdPar, 0);
		}

		if (header) {
			ps.start = true;
		}

		int bitsConsumed = br.getBitsRead() - bitCountStart;
		if (bitsConsumed > bitsLeft) {
			log.error(String.format("Expected to read %d PS bits actually read %d", bitsLeft, bitsConsumed));
			return readDataError(ac, ps, bitCountStart, bitsLeft);
		}

		return bitsConsumed;
	}

	/** Split one subband into 2 subsubbands with a symmetric real filter.
	 * The filter must have its non-center even coefficients equal to zero. */
	private static void hybrid2_re(float in[][], float out[][][], int outOffset, final float filter[], int len, int reverse) {
	    int inOffset = 0;
	    for (int i = 0; i < len; i++, inOffset++) {
	        float re_in = filter[6] * in[inOffset + 6][0]; //real inphase
	        float re_op = 0.0f;                            //real out of phase
	        float im_in = filter[6] * in[inOffset + 6][1]; //imag inphase
	        float im_op = 0.0f;                            //imag out of phase
	        for (int j = 0; j < 6; j += 2) {
	            re_op += filter[j+1] * (in[j+1][0] + in[12-j-1][0]);
	            im_op += filter[j+1] * (in[j+1][1] + in[12-j-1][1]);
	        }
	        out[outOffset +     reverse][i][0] = re_in + re_op;
	        out[outOffset +     reverse][i][1] = im_in + im_op;
	        out[outOffset + 1 - reverse][i][0] = re_in - re_op;
	        out[outOffset + 1 - reverse][i][1] = im_in - im_op;
	    }
	}

	/** Split one subband into 6 subsubbands with a complex filter */
	private static void hybrid6_cx(float in[][], float out[][][], int outOffset, final float filter[][][], int len) {
	    final int N = 8;
	    float temp[][] = new float[8][2];

	    int inOffset = 0;
	    for (int i = 0; i < len; i++, inOffset++) {
	        PSDSP.hybrid_analysis(temp, 0, in, inOffset, filter, 0, 1, N);
	        out[outOffset + 0][i][0] = temp[6][0];
	        out[outOffset + 0][i][1] = temp[6][1];
	        out[outOffset + 1][i][0] = temp[7][0];
	        out[outOffset + 1][i][1] = temp[7][1];
	        out[outOffset + 2][i][0] = temp[0][0];
	        out[outOffset + 2][i][1] = temp[0][1];
	        out[outOffset + 3][i][0] = temp[1][0];
	        out[outOffset + 3][i][1] = temp[1][1];
	        out[outOffset + 4][i][0] = temp[2][0] + temp[5][0];
	        out[outOffset + 4][i][1] = temp[2][1] + temp[5][1];
	        out[outOffset + 5][i][0] = temp[3][0] + temp[4][0];
	        out[outOffset + 5][i][1] = temp[3][1] + temp[4][1];
	    }
	}

	private static void hybrid4_8_12_cx(float in[][], float out[][][], int outOffset, final float filter[][][], int N, int len) {
		int inOffset = 0;
	    for (int i = 0; i < len; i++, inOffset++) {
	        PSDSP.hybrid_analysis(out[outOffset], i, in, inOffset, filter, 0, 32, N);
	    }
	}

	private static void hybrid_analysis(float out[][][], float in[][][], float L[][][], boolean is34, int len) {
	    for (int i = 0; i < 5; i++) {
	        for (int j = 0; j < 38; j++) {
	            in[i][j+6][0] = L[0][j][i];
	            in[i][j+6][1] = L[1][j][i];
	        }
	    }

	    if (is34) {
	        hybrid4_8_12_cx(in[0], out,  0, f34_0_12, 12, len);
	        hybrid4_8_12_cx(in[1], out, 12, f34_1_8,   8, len);
	        hybrid4_8_12_cx(in[2], out, 20, f34_2_4,   4, len);
	        hybrid4_8_12_cx(in[3], out, 24, f34_2_4,   4, len);
	        hybrid4_8_12_cx(in[4], out, 28, f34_2_4,   4, len);
	        PSDSP.hybrid_analysis_ileave(out, 27, L, 0, 5, len);
	    } else {
	        hybrid6_cx(in[0], out, 0, f20_0_8, len);
	        hybrid2_re(in[1], out, 6, g1_Q2, len, 1);
	        hybrid2_re(in[2], out, 8, g1_Q2, len, 0);
	        PSDSP.hybrid_analysis_ileave(out, 7, L, 0, 3, len);
	    }
	    //update in_buf
	    for (int i = 0; i < 5; i++) {
	    	for (int j = 0; j < 6; j++) {
		    	System.arraycopy(in[i][j + 32], 0, in[i][j], 0, in[i][0].length);
	    	}
	    }
	}

	static void hybrid_synthesis(float out[][][], float in[][][], boolean is34, int len) {
	    if (is34) {
	        for (int n = 0; n < len; n++) {
	        	Arrays.fill(out[0][n], 0, 5, 0f);
	        	Arrays.fill(out[1][n], 0, 5, 0f);
	            for (int i = 0; i < 12; i++) {
	                out[0][n][0] += in[   i][n][0];
	                out[1][n][0] += in[   i][n][1];
	            }
	            for (int i = 0; i < 8; i++) {
	                out[0][n][1] += in[12+i][n][0];
	                out[1][n][1] += in[12+i][n][1];
	            }
	            for (int i = 0; i < 4; i++) {
	                out[0][n][2] += in[20+i][n][0];
	                out[1][n][2] += in[20+i][n][1];
	                out[0][n][3] += in[24+i][n][0];
	                out[1][n][3] += in[24+i][n][1];
	                out[0][n][4] += in[28+i][n][0];
	                out[1][n][4] += in[28+i][n][1];
	            }
	        }
	        PSDSP.hybrid_synthesis_deint(out, in, 27, 5, len);
	    } else {
	        for (int n = 0; n < len; n++) {
	            out[0][n][0] = in[0][n][0] + in[1][n][0] + in[2][n][0] +
	                           in[3][n][0] + in[4][n][0] + in[5][n][0];
	            out[1][n][0] = in[0][n][1] + in[1][n][1] + in[2][n][1] +
	                           in[3][n][1] + in[4][n][1] + in[5][n][1];
	            out[0][n][1] = in[6][n][0] + in[7][n][0];
	            out[1][n][1] = in[6][n][1] + in[7][n][1];
	            out[0][n][2] = in[8][n][0] + in[9][n][0];
	            out[1][n][2] = in[8][n][1] + in[9][n][1];
	        }
	        PSDSP.hybrid_synthesis_deint(out, in, 7, 3, len);
	    }
	}

	/// All-pass filter decay slope
	private static final float DECAY_SLOPE = 0.05f;
	/// Number of frequency bands that can be addressed by the parameter index, b(k)
	private static final int   NR_PAR_BANDS[]      = { 20, 34 };
	private static final int   NR_IPDOPD_BANDS[]   = { 11, 17 };
	/// Number of frequency bands that can be addressed by the sub subband index, k
	private static final int   NR_BANDS[]          = { 71, 91 };
	/// Start frequency band for the all-pass filter decay slope
	private static final int   DECAY_CUTOFF[]      = { 10, 32 };
	/// Number of all-pass filer bands
	private static final int   NR_ALLPASS_BANDS[]  = { 30, 50 };
	/// First stereo band using the short one sample delay
	private static final int   SHORT_DELAY_BAND[]  = { 42, 62 };

	/** Table 8.46 */
	static void map_idx_10_to_20(int par_mapped[], final int par[], boolean full) {
	    int b;

	    if (full) {
	        b = 9;
	    } else {
	        b = 4;
	        par_mapped[10] = 0;
	    }

	    for (; b >= 0; b--) {
	        par_mapped[2*b+1] = par_mapped[2*b] = par[b];
	    }
	}

	static void map_idx_34_to_20(int par_mapped[], final int par[], boolean full) {
	    par_mapped[ 0] = (2*par[ 0] +   par[ 1]) / 3;
	    par_mapped[ 1] = (  par[ 1] + 2*par[ 2]) / 3;
	    par_mapped[ 2] = (2*par[ 3] +   par[ 4]) / 3;
	    par_mapped[ 3] = (  par[ 4] + 2*par[ 5]) / 3;
	    par_mapped[ 4] = (  par[ 6] +   par[ 7]) / 2;
	    par_mapped[ 5] = (  par[ 8] +   par[ 9]) / 2;
	    par_mapped[ 6] =    par[10];
	    par_mapped[ 7] =    par[11];
	    par_mapped[ 8] = (  par[12] +   par[13]) / 2;
	    par_mapped[ 9] = (  par[14] +   par[15]) / 2;
	    par_mapped[10] =    par[16];
	    if (full) {
	        par_mapped[11] =    par[17];
	        par_mapped[12] =    par[18];
	        par_mapped[13] =    par[19];
	        par_mapped[14] = (  par[20] +   par[21]) / 2;
	        par_mapped[15] = (  par[22] +   par[23]) / 2;
	        par_mapped[16] = (  par[24] +   par[25]) / 2;
	        par_mapped[17] = (  par[26] +   par[27]) / 2;
	        par_mapped[18] = (  par[28] +   par[29] +   par[30] +   par[31]) / 4;
	        par_mapped[19] = (  par[32] +   par[33]) / 2;
	    }
	}

	static void map_val_34_to_20(float par[]) {
	    par[ 0] = (2*par[ 0] +   par[ 1]) * 0.33333333f;
	    par[ 1] = (  par[ 1] + 2*par[ 2]) * 0.33333333f;
	    par[ 2] = (2*par[ 3] +   par[ 4]) * 0.33333333f;
	    par[ 3] = (  par[ 4] + 2*par[ 5]) * 0.33333333f;
	    par[ 4] = (  par[ 6] +   par[ 7]) * 0.5f;
	    par[ 5] = (  par[ 8] +   par[ 9]) * 0.5f;
	    par[ 6] =    par[10];
	    par[ 7] =    par[11];
	    par[ 8] = (  par[12] +   par[13]) * 0.5f;
	    par[ 9] = (  par[14] +   par[15]) * 0.5f;
	    par[10] =    par[16];
	    par[11] =    par[17];
	    par[12] =    par[18];
	    par[13] =    par[19];
	    par[14] = (  par[20] +   par[21]) * 0.5f;
	    par[15] = (  par[22] +   par[23]) * 0.5f;
	    par[16] = (  par[24] +   par[25]) * 0.5f;
	    par[17] = (  par[26] +   par[27]) * 0.5f;
	    par[18] = (  par[28] +   par[29] +   par[30] +   par[31]) * 0.25f;
	    par[19] = (  par[32] +   par[33]) * 0.5f;
	}

	static void map_idx_10_to_34(int par_mapped[], final int par[], boolean full) {
	    if (full) {
	        par_mapped[33] = par[9];
	        par_mapped[32] = par[9];
	        par_mapped[31] = par[9];
	        par_mapped[30] = par[9];
	        par_mapped[29] = par[9];
	        par_mapped[28] = par[9];
	        par_mapped[27] = par[8];
	        par_mapped[26] = par[8];
	        par_mapped[25] = par[8];
	        par_mapped[24] = par[8];
	        par_mapped[23] = par[7];
	        par_mapped[22] = par[7];
	        par_mapped[21] = par[7];
	        par_mapped[20] = par[7];
	        par_mapped[19] = par[6];
	        par_mapped[18] = par[6];
	        par_mapped[17] = par[5];
	        par_mapped[16] = par[5];
	    } else {
	        par_mapped[16] =      0;
	    }
	    par_mapped[15] = par[4];
	    par_mapped[14] = par[4];
	    par_mapped[13] = par[4];
	    par_mapped[12] = par[4];
	    par_mapped[11] = par[3];
	    par_mapped[10] = par[3];
	    par_mapped[ 9] = par[2];
	    par_mapped[ 8] = par[2];
	    par_mapped[ 7] = par[2];
	    par_mapped[ 6] = par[2];
	    par_mapped[ 5] = par[1];
	    par_mapped[ 4] = par[1];
	    par_mapped[ 3] = par[1];
	    par_mapped[ 2] = par[0];
	    par_mapped[ 1] = par[0];
	    par_mapped[ 0] = par[0];
	}

	static void map_idx_20_to_34(int par_mapped[], final int par[], boolean full) {
	    if (full) {
	        par_mapped[33] =  par[19];
	        par_mapped[32] =  par[19];
	        par_mapped[31] =  par[18];
	        par_mapped[30] =  par[18];
	        par_mapped[29] =  par[18];
	        par_mapped[28] =  par[18];
	        par_mapped[27] =  par[17];
	        par_mapped[26] =  par[17];
	        par_mapped[25] =  par[16];
	        par_mapped[24] =  par[16];
	        par_mapped[23] =  par[15];
	        par_mapped[22] =  par[15];
	        par_mapped[21] =  par[14];
	        par_mapped[20] =  par[14];
	        par_mapped[19] =  par[13];
	        par_mapped[18] =  par[12];
	        par_mapped[17] =  par[11];
	    }
	    par_mapped[16] =  par[10];
	    par_mapped[15] =  par[ 9];
	    par_mapped[14] =  par[ 9];
	    par_mapped[13] =  par[ 8];
	    par_mapped[12] =  par[ 8];
	    par_mapped[11] =  par[ 7];
	    par_mapped[10] =  par[ 6];
	    par_mapped[ 9] =  par[ 5];
	    par_mapped[ 8] =  par[ 5];
	    par_mapped[ 7] =  par[ 4];
	    par_mapped[ 6] =  par[ 4];
	    par_mapped[ 5] =  par[ 3];
	    par_mapped[ 4] = (par[ 2] + par[ 3]) / 2;
	    par_mapped[ 3] =  par[ 2];
	    par_mapped[ 2] =  par[ 1];
	    par_mapped[ 1] = (par[ 0] + par[ 1]) / 2;
	    par_mapped[ 0] =  par[ 0];
	}

	static void map_val_20_to_34(float par[]) {
	    par[33] =  par[19];
	    par[32] =  par[19];
	    par[31] =  par[18];
	    par[30] =  par[18];
	    par[29] =  par[18];
	    par[28] =  par[18];
	    par[27] =  par[17];
	    par[26] =  par[17];
	    par[25] =  par[16];
	    par[24] =  par[16];
	    par[23] =  par[15];
	    par[22] =  par[15];
	    par[21] =  par[14];
	    par[20] =  par[14];
	    par[19] =  par[13];
	    par[18] =  par[12];
	    par[17] =  par[11];
	    par[16] =  par[10];
	    par[15] =  par[ 9];
	    par[14] =  par[ 9];
	    par[13] =  par[ 8];
	    par[12] =  par[ 8];
	    par[11] =  par[ 7];
	    par[10] =  par[ 6];
	    par[ 9] =  par[ 5];
	    par[ 8] =  par[ 5];
	    par[ 7] =  par[ 4];
	    par[ 6] =  par[ 4];
	    par[ 5] =  par[ 3];
	    par[ 4] = (par[ 2] + par[ 3]) * 0.5f;
	    par[ 3] =  par[ 2];
	    par[ 2] =  par[ 1];
	    par[ 1] = (par[ 0] + par[ 1]) * 0.5f;
	}

	static void decorrelation(PSContext ps, float out[][][], final float s[][][], final boolean is34) {
		final int is34i = is34 ? 1 : 0;
	    float power[][] = new float[34][PS_QMF_TIME_SLOTS];
	    float transient_gain[][] = new float[34][PS_QMF_TIME_SLOTS];
	    float peak_decay_nrg[] = ps.peakDecayNrg;
	    float power_smooth[] = ps.powerSmooth;
	    float peak_decay_diff_smooth[] = ps.peakDecayDiffSmooth;
	    float delay[][][] = ps.delay;
	    float ap_delay[][][][] = ps.apDelay;
	    final int k_to_i[] = is34 ? k_to_i_34 : k_to_i_20;
	    final float peak_decay_factor = 0.76592833836465f;
	    final float transient_impact  = 1.5f;
	    final float a_smooth          = 0.25f; ///< Smoothing coefficient
	    int n0 = 0, nL = 32;

	    Utilities.fill(power, 0f);

	    if (is34 != ps.is34bandsOld) {
	    	Utilities.fill(ps.peakDecayNrg,        0f);
	    	Utilities.fill(ps.powerSmooth,         0f);
	    	Utilities.fill(ps.peakDecayDiffSmooth, 0f);
	    	Utilities.fill(ps.delay,               0f);
	    	Utilities.fill(ps.apDelay,             0f);
	    }

	    for (int k = 0; k < NR_BANDS[is34i]; k++) {
	        int i = k_to_i[k];
	        PSDSP.add_squares(power[i], 0, s[k], 0, nL - n0);
	    }

	    // Transient detection
	    for (int i = 0; i < NR_PAR_BANDS[is34i]; i++) {
	        for (int n = n0; n < nL; n++) {
	            float decayed_peak = peak_decay_factor * peak_decay_nrg[i];
	            float denom;
	            peak_decay_nrg[i] = Math.max(decayed_peak, power[i][n]);
	            power_smooth[i] += a_smooth * (power[i][n] - power_smooth[i]);
	            peak_decay_diff_smooth[i] += a_smooth * (peak_decay_nrg[i] - power[i][n] - peak_decay_diff_smooth[i]);
	            denom = transient_impact * peak_decay_diff_smooth[i];
	            transient_gain[i][n]   = (denom > power_smooth[i]) ? power_smooth[i] / denom : 1.0f;
	        }
	    }

	    // Decorrelation and transient reduction
	    //                         PS_AP_LINKS - 1
	    //                               -----
	    //                                | |  Q_fract_allpass[k][m]*z^-link_delay[m] - a[m]*g_decay_slope[k]
	    // H[k][z] = z^-2 * phi_fract[k] * | | ----------------------------------------------------------------
	    //                                | | 1 - a[m]*g_decay_slope[k]*Q_fract_allpass[k][m]*z^-link_delay[m]
	    //                               m = 0
	    // d[k][z] (out) = transient_gain_mapped[k][z] * H[k][z] * s[k][z]
	    int k;
	    for (k = 0; k < NR_ALLPASS_BANDS[is34i]; k++) {
	        int b = k_to_i[k];
	        float g_decay_slope = 1.f - DECAY_SLOPE * (k - DECAY_CUTOFF[is34i]);
	        g_decay_slope = clipf(g_decay_slope, 0.f, 1.f);
	        for (int i = 0; i < PS_MAX_DELAY; i++) {
	        	Utilities.copy(delay[k][i], delay[k][nL + i]);
	        }
	        for (int i = 0; i < numQMFSlots; i++) {
	        	Utilities.copy(delay[k][PS_MAX_DELAY + i], s[k][i]);
	        }
	        for (int m = 0; m < PS_AP_LINKS; m++) {
	        	for (int i = 0; i < 5; i++) {
	        		Utilities.copy(ap_delay[k][m][i], ap_delay[k][m][numQMFSlots + i]);
	        	}
	        }
	        PSDSP.decorrelate(out[k], 0, delay[k], PS_MAX_DELAY - 2, ap_delay[k],
	                          phi_fract[is34i][k],
	                          Q_fract_allpass[is34i][k],
	                          transient_gain[b], g_decay_slope, nL - n0);
	    }

	    for (; k < SHORT_DELAY_BAND[is34i]; k++) {
	        for (int i = 0; i < PS_MAX_DELAY; i++) {
	        	Utilities.copy(delay[k][i], delay[k][nL + i]);
	        }
	        for (int i = 0; i < numQMFSlots; i++) {
	        	Utilities.copy(delay[k][PS_MAX_DELAY + i], s[k][i]);
	        }
	        // H = delay 14
	        int i = k_to_i[k];
	        PSDSP.mul_pair_single(out[k], 0, delay[k], PS_MAX_DELAY - 14, transient_gain[i], 0, nL - n0);
	    }

	    for (; k < NR_BANDS[is34i]; k++) {
	        for (int i = 0; i < PS_MAX_DELAY; i++) {
	        	Utilities.copy(delay[k][i], delay[k][nL + i]);
	        }
	        for (int i = 0; i < numQMFSlots; i++) {
	        	Utilities.copy(delay[k][PS_MAX_DELAY + i], s[k][i]);
	        }
	        // H = delay 1
	        int i = k_to_i[k];
	        PSDSP.mul_pair_single(out[k], 0, delay[k], PS_MAX_DELAY - 1, transient_gain[i], 0, nL - n0);
	    }
	}

	private static void remap34(int p_par_mapped[][][], int par[][], int num_par, int numEnv, boolean full) {
	    int par_mapped[][] = p_par_mapped[0];
	    if (num_par == 20 || num_par == 11) {
	        for (int e = 0; e < numEnv; e++) {
	            map_idx_20_to_34(par_mapped[e], par[e], full);
	        }
	    } else if (num_par == 10 || num_par == 5) {
	        for (int e = 0; e < numEnv; e++) {
	            map_idx_10_to_34(par_mapped[e], par[e], full);
	        }
	    } else {
	        p_par_mapped[0] = par;
	    }
	}

	private static void remap20(int p_par_mapped[][][], int par[][], int num_par, int numEnv, boolean full) {
	    int par_mapped[][] = p_par_mapped[0];
	    if (num_par == 34 || num_par == 17) {
	        for (int e = 0; e < numEnv; e++) {
	            map_idx_34_to_20(par_mapped[e], par[e], full);
	        }
	    } else if (num_par == 10 || num_par == 5) {
	        for (int e = 0; e < numEnv; e++) {
	            map_idx_10_to_20(par_mapped[e], par[e], full);
	        }
	    } else {
	    	p_par_mapped[0] = par;
	    }
	}

	private static void ipdopd_reset(int ipd_hist[], int opd_hist[]) {
		Arrays.fill(ipd_hist, 0, PSContext.PS_MAX_NR_IPDOPD, 0);
		Arrays.fill(opd_hist, 0, PSContext.PS_MAX_NR_IPDOPD, 0);
	}

	private static void stereo_processing(PSContext ps, float l[][][], float r[][][], final boolean is34) {
		final int is34i = is34 ? 1 : 0;
	    float H11[][][] = ps.H11;
	    float H12[][][] = ps.H12;
	    float H21[][][] = ps.H21;
	    float H22[][][] = ps.H22;
	    int opd_hist[] = ps.opdHist;
	    int ipd_hist[] = ps.ipdHist;
	    int iid_mapped_buf[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC];
	    int icc_mapped_buf[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC];
	    int ipd_mapped_buf[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC];
	    int opd_mapped_buf[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC];
	    int iid_mapped[][][] = { iid_mapped_buf };
	    int icc_mapped[][][] = { icc_mapped_buf };
	    int ipd_mapped[][][] = { ipd_mapped_buf };
	    int opd_mapped[][][] = { opd_mapped_buf };
	    final int k_to_i[] = is34 ? k_to_i_34 : k_to_i_20;
	    final float H_LUT[][][] = ps.iccMode < 3 ? HA : HB;

	    // Remapping
	    if (ps.numEnvOld != 0) {
	    	System.arraycopy(H11[0][ps.numEnvOld], 0, H11[0][0], 0, PS_MAX_NR_IIDICC);
	    	System.arraycopy(H11[1][ps.numEnvOld], 0, H11[1][0], 0, PS_MAX_NR_IIDICC);
	    	System.arraycopy(H12[0][ps.numEnvOld], 0, H12[0][0], 0, PS_MAX_NR_IIDICC);
	    	System.arraycopy(H12[1][ps.numEnvOld], 0, H12[1][0], 0, PS_MAX_NR_IIDICC);
	    	System.arraycopy(H21[0][ps.numEnvOld], 0, H21[0][0], 0, PS_MAX_NR_IIDICC);
	    	System.arraycopy(H21[1][ps.numEnvOld], 0, H21[1][0], 0, PS_MAX_NR_IIDICC);
	    	System.arraycopy(H22[0][ps.numEnvOld], 0, H22[0][0], 0, PS_MAX_NR_IIDICC);
	    	System.arraycopy(H22[1][ps.numEnvOld], 0, H22[1][0], 0, PS_MAX_NR_IIDICC);
	    }

	    if (is34) {
	        remap34(iid_mapped, ps.iidPar, ps.nrIidPar, ps.numEnv, true);
	        remap34(icc_mapped, ps.iccPar, ps.nrIccPar, ps.numEnv, true);
	        if (ps.enableIpdopd) {
	            remap34(ipd_mapped, ps.ipdPar, ps.nrIpdopdPar, ps.numEnv, false);
	            remap34(opd_mapped, ps.opdPar, ps.nrIpdopdPar, ps.numEnv, false);
	        }
	        if (!ps.is34bandsOld) {
	            map_val_20_to_34(H11[0][0]);
	            map_val_20_to_34(H11[1][0]);
	            map_val_20_to_34(H12[0][0]);
	            map_val_20_to_34(H12[1][0]);
	            map_val_20_to_34(H21[0][0]);
	            map_val_20_to_34(H21[1][0]);
	            map_val_20_to_34(H22[0][0]);
	            map_val_20_to_34(H22[1][0]);
	            ipdopd_reset(ipd_hist, opd_hist);
	        }
	    } else {
	        remap20(iid_mapped, ps.iidPar, ps.nrIidPar, ps.numEnv, true);
	        remap20(icc_mapped, ps.iccPar, ps.nrIccPar, ps.numEnv, true);
	        if (ps.enableIpdopd) {
	            remap20(ipd_mapped, ps.ipdPar, ps.nrIpdopdPar, ps.numEnv, false);
	            remap20(opd_mapped, ps.opdPar, ps.nrIpdopdPar, ps.numEnv, false);
	        }
	        if (ps.is34bandsOld) {
	            map_val_34_to_20(H11[0][0]);
	            map_val_34_to_20(H11[1][0]);
	            map_val_34_to_20(H12[0][0]);
	            map_val_34_to_20(H12[1][0]);
	            map_val_34_to_20(H21[0][0]);
	            map_val_34_to_20(H21[1][0]);
	            map_val_34_to_20(H22[0][0]);
	            map_val_34_to_20(H22[1][0]);
	            ipdopd_reset(ipd_hist, opd_hist);
	        }
	    }

	    // Mixing
	    for (int e = 0; e < ps.numEnv; e++) {
	        for (int b = 0; b < NR_PAR_BANDS[is34i]; b++) {
	            float h11, h12, h21, h22;
	            h11 = H_LUT[iid_mapped[0][e][b] + 7 + 23 * ps.iidQuant][icc_mapped[0][e][b]][0];
	            h12 = H_LUT[iid_mapped[0][e][b] + 7 + 23 * ps.iidQuant][icc_mapped[0][e][b]][1];
	            h21 = H_LUT[iid_mapped[0][e][b] + 7 + 23 * ps.iidQuant][icc_mapped[0][e][b]][2];
	            h22 = H_LUT[iid_mapped[0][e][b] + 7 + 23 * ps.iidQuant][icc_mapped[0][e][b]][3];

	            if (ps.enableIpdopd && b < NR_IPDOPD_BANDS[is34i]) {
	                // The spec say says to only run this smoother when enableIpdopd
	                // is set but the reference decoder appears to run it finalantly
	                float h11i, h12i, h21i, h22i;
	                float ipd_adj_re, ipd_adj_im;
	                int opd_idx = opd_hist[b] * 8 + opd_mapped[0][e][b];
	                int ipd_idx = ipd_hist[b] * 8 + ipd_mapped[0][e][b];
	                float opd_re = pd_re_smooth[opd_idx];
	                float opd_im = pd_im_smooth[opd_idx];
	                float ipd_re = pd_re_smooth[ipd_idx];
	                float ipd_im = pd_im_smooth[ipd_idx];
	                opd_hist[b] = opd_idx & 0x3F;
	                ipd_hist[b] = ipd_idx & 0x3F;

	                ipd_adj_re = opd_re*ipd_re + opd_im*ipd_im;
	                ipd_adj_im = opd_im*ipd_re - opd_re*ipd_im;
	                h11i = h11 * opd_im;
	                h11  = h11 * opd_re;
	                h12i = h12 * ipd_adj_im;
	                h12  = h12 * ipd_adj_re;
	                h21i = h21 * opd_im;
	                h21  = h21 * opd_re;
	                h22i = h22 * ipd_adj_im;
	                h22  = h22 * ipd_adj_re;
	                H11[1][e+1][b] = h11i;
	                H12[1][e+1][b] = h12i;
	                H21[1][e+1][b] = h21i;
	                H22[1][e+1][b] = h22i;
	            }
	            H11[0][e+1][b] = h11;
	            H12[0][e+1][b] = h12;
	            H21[0][e+1][b] = h21;
	            H22[0][e+1][b] = h22;
	        }
	        for (int k = 0; k < NR_BANDS[is34i]; k++) {
	            float h[][] = new float[2][4];
	            float h_step[][] = new float [2][4];
	            int start = ps.borderPosition[e];
	            int stop  = ps.borderPosition[e+1];
	            float width = 1.f / (stop - start);
	            int b = k_to_i[k];
	            h[0][0] = H11[0][e][b];
	            h[0][1] = H12[0][e][b];
	            h[0][2] = H21[0][e][b];
	            h[0][3] = H22[0][e][b];
	            if (ps.enableIpdopd) {
		            // Is this necessary? ps_04_new seems unchanged
		            if ((is34 && k <= 13 && k >= 9) || (!is34 && k <= 1)) {
		                h[1][0] = -H11[1][e][b];
		                h[1][1] = -H12[1][e][b];
		                h[1][2] = -H21[1][e][b];
		                h[1][3] = -H22[1][e][b];
		            } else {
		                h[1][0] = H11[1][e][b];
		                h[1][1] = H12[1][e][b];
		                h[1][2] = H21[1][e][b];
		                h[1][3] = H22[1][e][b];
		            }
	            }
	            // Interpolation
	            h_step[0][0] = (H11[0][e+1][b] - h[0][0]) * width;
	            h_step[0][1] = (H12[0][e+1][b] - h[0][1]) * width;
	            h_step[0][2] = (H21[0][e+1][b] - h[0][2]) * width;
	            h_step[0][3] = (H22[0][e+1][b] - h[0][3]) * width;
	            if (ps.enableIpdopd) {
	                h_step[1][0] = (H11[1][e+1][b] - h[1][0]) * width;
	                h_step[1][1] = (H12[1][e+1][b] - h[1][1]) * width;
	                h_step[1][2] = (H21[1][e+1][b] - h[1][2]) * width;
	                h_step[1][3] = (H22[1][e+1][b] - h[1][3]) * width;
	            }
	            PSDSP.stereoInterpolate(l[k], start + 1, r[k], start + 1, h, h_step, stop - start, ps.enableIpdopd);
	        }
	    }
	}

	public static int psApply(PSContext ps, float L[][][], float R[][][], int top) {
	    float Lbuf[][][] = new float[91][32][2];
	    float Rbuf[][][] = new float[91][32][2];
	    final int len = 32;
	    final boolean is34 = ps.is34bands;
	    final int is34i = is34 ? 1 : 0;

	    top += NR_BANDS[is34i] - 64;
	    for (int i = top; i < NR_BANDS[is34i]; i++) {
	    	Utilities.fill(ps.delay[i], 0f);
	    }
	    if (top < NR_ALLPASS_BANDS[is34i]) {
		    for (int i = top; i < NR_ALLPASS_BANDS[is34i]; i++) {
		    	Utilities.fill(ps.apDelay[i], 0f);
		    }
	    }

	    hybrid_analysis(Lbuf, ps.inBuf, L, is34, len);
	    decorrelation(ps, Rbuf, Lbuf, is34);
	    stereo_processing(ps, Lbuf, Rbuf, is34);
	    hybrid_synthesis(L, Lbuf, is34, len);
	    hybrid_synthesis(R, Rbuf, is34, len);

	    return 0;
	}
}
