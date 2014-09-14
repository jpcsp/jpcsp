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

import static java.lang.Math.pow;
import static jpcsp.media.codec.mp3.Mp3Data.MODE_EXT_I_STEREO;
import static jpcsp.media.codec.mp3.Mp3Data.MODE_EXT_MS_STEREO;
import static jpcsp.media.codec.mp3.Mp3Data.band_size_long;
import static jpcsp.media.codec.mp3.Mp3Data.band_size_short;
import static jpcsp.media.codec.mp3.Mp3Data.ci_table;
import static jpcsp.media.codec.mp3.Mp3Data.exp_table;
import static jpcsp.media.codec.mp3.Mp3Data.expval_table;
import static jpcsp.media.codec.mp3.Mp3Data.lsf_nsf_table;
import static jpcsp.media.codec.mp3.Mp3Data.mp3_bitrate_tab;
import static jpcsp.media.codec.mp3.Mp3Data.mp3_freq_tab;
import static jpcsp.media.codec.mp3.Mp3Data.mp3_quant_bits;
import static jpcsp.media.codec.mp3.Mp3Data.mpa_huff_data;
import static jpcsp.media.codec.mp3.Mp3Data.mpa_pretab;
import static jpcsp.media.codec.mp3.Mp3Data.mpa_quad_bits;
import static jpcsp.media.codec.mp3.Mp3Data.mpa_quad_codes;
import static jpcsp.media.codec.mp3.Mp3Data.slen_table;
import static jpcsp.media.codec.mp3.Mp3Dsp.mdct_win;

import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.mp3.Mp3Data.HuffTable;
import jpcsp.media.codec.util.BitBuffer;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.CodecUtils;
import jpcsp.media.codec.util.FloatDSP;
import jpcsp.media.codec.util.VLC;

public class Mp3Decoder implements ICodec {
	public static Logger log = Logger.getLogger("mp3");
	public static final int MP3_ERROR = -3;
	private static final int HEADER_SIZE = 4;
	public static final int BACKSTEP_SIZE = 512;
	public static final int EXTRABYTES = 24;
	public static final int LAST_BUF_SIZE = 2 * BACKSTEP_SIZE + EXTRABYTES;
	public static final int MP3_MAX_CHANNELS = 2;
	public static final int SBLIMIT = 32; // number of subbands
	public static final int MP3_STEREO  = 0;
	public static final int MP3_JSTEREO = 1;
	public static final int MP3_DUAL    = 2;
	public static final int MP3_MONO    = 3;
	public static final int FRAC_BITS = 23; // fractional bits for sbSample snad dct
	public static final int WFRAC_BITS = 16; // fractional bits for window
	public static final int FRAC_ONE = 1 << FRAC_BITS;
	public static final float IMDCT_SCALAR = 1.759f;
	private Context ctx;
	private BitReader br;
	private BitBuffer bb = new BitBuffer(4096 * 8);
	private static boolean initializedTables = false;
	// vlc structure for decoding layer 3 Huffman tables
	private static VLC huff_vlc[] = new VLC[16];
	private static VLC huff_quad_vlc[] = new VLC[2];
	private static final int band_index_long[][] = new int[9][23];
	private static final float is_table[][] = new float[2][16];
	private static final float is_table_lsf[][][] = new float[2][2][16];
	private static final float csa_table[][] = new float[8][4];
	private static final int division_tab3[] = new int[1 << 6 ];
	private static final int division_tab5[] = new int[1 << 8 ];
	private static final int division_tab9[] = new int[1 << 11];
	private static final int division_tabs[][] = { division_tab3, division_tab5, null, division_tab9 };
	/* lower 2 bits: modulo 3, higher bits: shift */
	private static final int scale_factor_modshift[] = new int[64];
	/* [i][j]:  2^(-j/3) * FRAC_ONE * 2^(i+2) / (2^(i+2) - 1) */
	private static final int scale_factor_mult[][] = new int[15][3];
	/* mult table for layer 2 group quantization */
	private static final int scale_factor_mult2[][] = new int[][] {
		{ FIXR_OLD(1.0f * 4.0f / 3.0f), FIXR_OLD(0.7937005259f * 4.0f / 3.0f), FIXR_OLD(0.6299605249f * 4.0f / 3.0f) },
		{ FIXR_OLD(1.0f * 4.0f / 5.0f), FIXR_OLD(0.7937005259f * 4.0f / 5.0f), FIXR_OLD(0.6299605249f * 4.0f / 5.0f) },
		{ FIXR_OLD(1.0f * 4.0f / 9.0f), FIXR_OLD(0.7937005259f * 4.0f / 9.0f), FIXR_OLD(0.6299605249f * 4.0f / 9.0f) }
	};
	private static final double ISQRT2 = 0.70710678118654752440;
	private static final int idxtab[] = { 3,3,2,2,1,1,1,1,0,0,0,0,0,0,0,0 };

	private static int FIXR_OLD(float value) {
		return (int) (value * FRAC_ONE + 0.5f);
	}

	private void initStatic() {
		if (initializedTables) {
			return;
		}

		// scale factors table for layer 1/2
		for (int i = 0; i < 64; i++) {
			// 1.0 (i = 3) is normalized to 2 ^ FRAC_BITS
			int shift = i / 3;
			int mod   = i % 3;
			scale_factor_modshift[i] = mod | (shift << 2);
		}

		// scale factor multiply for layer 1
		for (int i = 0; i < 15; i++) {
			int n = i + 2;
			int norm = (int) (((1L << n) * FRAC_ONE) / ((1 << n) - 1));
			scale_factor_mult[i][0] = (int) (norm * 1.0f          * 2.0f);
			scale_factor_mult[i][1] = (int) (norm * 0.7937005259f * 2.0f);
			scale_factor_mult[i][2] = (int) (norm * 0.6299605249f * 2.0f);
		}

		Mp3Dsp.synthInit(Mp3Dsp.mpa_synth_window);

		// Huffman decode tables
		for (int i = 1; i < 16; i++) {
			HuffTable h = Mp3Data.mpa_huff_tables[i];
			int[] tmpBits = new int[512];
			int[] tmpCodes = new int[512];

			int xsize = h.xsize;

			int j = 0;
			for (int x = 0; x < xsize; x++) {
				for (int y = 0; y < xsize; y++) {
					tmpBits [(x << 5) | y | ((x != 0 && y != 0) ? 16 : 0)] = h.bits [j  ];
					tmpCodes[(x << 5) | y | ((x != 0 && y != 0) ? 16 : 0)] = h.codes[j++];
				}
			}

			huff_vlc[i] = new VLC();
			huff_vlc[i].initVLCSparse(7, 512, tmpBits, tmpCodes, null);
		}

		for (int i = 0; i < 2; i++) {
			huff_quad_vlc[i] = new VLC();
			huff_quad_vlc[i].initVLCSparse(i == 0 ? 7 : 4, 16, mpa_quad_bits[i], mpa_quad_codes[i], null);
		}

		for (int i = 0; i < 9; i++) {
			int k = 0;
			for (int j = 0; j < 22; j++) {
				band_index_long[i][j] = k;
				k += band_size_long[i][j];
			}
			band_index_long[i][22] = k;
		}

		Mp3Data.tableinit();
		Mp3Dsp.initMpadspTabs();

		for (int i = 0; i < 4; i++) {
			if (mp3_quant_bits[i] < 0) {
				for (int j = 0; j < (1 << (-mp3_quant_bits[i] + 1)); j++) {
					int val = j;
					int steps = Mp3Data.mp3_quant_steps[i];
					int val1 = val % steps;
					val /= steps;
					int val2 = val % steps;
					int val3 = val / steps;
					division_tabs[i][j] = val1 + (val2 << 4) + (val3 << 8);
				}
			}
		}

		for (int i = 0; i < 7; i++) {
			float v;
			if (i != 6) {
				float f = (float) Math.tan(i * Math.PI / 12.0);
				v = f / (1f + f);
			} else {
				v = 1f;
			}
			is_table[0][    i] = v;
			is_table[1][6 - i] = v;
		}
		// invalid values
		for (int i = 7; i < 16; i++) {
			is_table[0][i] = 0f;
			is_table[1][i] = 0f;
		}

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 2; j++) {
				int e = -(j + 1) * ((i + 1) >> 1);
				double f = pow(2.0, e / 4.0);
				int k = i & 1;
				is_table_lsf[j][k ^ 1][i] = (float) f;
				is_table_lsf[j][k    ][i] = 1f;
			}
		}

		for (int i = 0; i < 8; i++) {
			float ci = ci_table[i];
			float cs = (float) (1.0 / Math.sqrt(1.0 + ci * ci));
			float ca = cs * ci;
			csa_table[i][0] = cs;
			csa_table[i][1] = ca;
			csa_table[i][2] = ca + cs;
			csa_table[i][3] = ca - cs;
		}

		initializedTables = true;
	}

	@Override
	public int init(int bytesPerFrame, int channels, int outputChannels, int codingMode) {
		initStatic();

		ctx = new Context();

		return 0;
	}

	private int mpaCheckHeader(int header) {
		// header
		if ((header & 0xFFE00000) != 0xFFE00000) {
			return -1;
		}
		// layer check
		if ((header & (3 << 17)) == 0) {
			return -1;
		}
		// bit rate
		if ((header & (0xF << 12)) == (0xF << 12)) {
			return -1;
		}
		// frequency
		if ((header & (3 << 10)) == (3 << 10)) {
			return -1;
		}
		return 0;
	}

	public static int decodeHeader(Mp3Header s, int header) {
		int mpeg25;
		if ((header & (1 << 20)) != 0) {
			s.lsf = (header & (1 << 19)) != 0 ? 0 : 1;
			mpeg25 = 0;
		} else {
			s.lsf = 1;
			mpeg25 = 1;
		}

		s.layer = 4 - ((header >> 17) & 3);
		// extract frequency
		int sampleRateIndex = (header >> 10) & 3;
		if (sampleRateIndex >= mp3_freq_tab.length) {
			sampleRateIndex = 0;
		}
		int sampleRate = mp3_freq_tab[sampleRateIndex] >> (s.lsf + mpeg25);
		sampleRateIndex += 3 * (s.lsf + mpeg25);
		s.sampleRateIndex = sampleRateIndex;
		s.errorProtection = ((header >> 16) & 1) ^ 1;
		s.sampleRate = sampleRate;

		int bitrateIndex = (header >> 12) & 0xF;
		int padding = (header >> 9) & 1;
		//extension = (header >> 8) & 1;
		s.mode = (header >> 6) & 3;
		s.modeExt = (header >> 4) & 3;
		//copyright = (header >> 3) & 1;
		//original = (header >> 2) & 1;
		//emphasis = header & 3;

		if (s.mode == MP3_MONO) {
			s.nbChannels = 1;
		} else {
			s.nbChannels = 2;
		}

		if (bitrateIndex != 0) {
			int frameSize = mp3_bitrate_tab[s.lsf][s.layer - 1][bitrateIndex];
			s.bitRate = frameSize * 1000;
			switch (s.layer) {
				case 1:
					frameSize = (frameSize * 12000) / sampleRate;
					frameSize = (frameSize + padding) * 4;
					break;
				case 2:
					frameSize = (frameSize * 144000) / sampleRate;
					frameSize += padding;
					break;
				default:
				case 3:
					frameSize = (frameSize * 144000) / (sampleRate << s.lsf);
					frameSize += padding;
					break;
			}
			s.frameSize = frameSize;
		} else {
			// if no frame size computed, signal it
			return 1;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("layer%d, %d Hz, %d kbits/s, %s", s.layer, s.sampleRate, s.bitRate, s.nbChannels == 2 ? "stereo" : "mono"));
		}

		return 0;
	}

	private int decodeLayer1() {
		log.warn("Unimplemented MP3 Layer-1");
		// TODO
		return 0;
	}

	private int decodeLayer2() {
		log.warn("Unimplemented MP3 Layer-2");
		// TODO
		return 0;
	}

	private void initShortRegion(Granule g) {
		if (g.blockType == 2) {
			if (ctx.header.sampleRateIndex != 8) {
				g.regionSize[0] = 36 / 2;
			} else {
				g.regionSize[0] = 72 / 2;
			}
		} else {
			if (ctx.header.sampleRateIndex <= 2) {
				g.regionSize[0] = 36 / 2;
			} else if (ctx.header.sampleRateIndex != 8) {
				g.regionSize[0] = 54 / 2;
			} else {
				g.regionSize[0] = 108 / 2;
			}
		}
		g.regionSize[1] = 576 / 2;
	}

	private void initLongRegion(Granule g, int ra1, int ra2) {
		g.regionSize[0] = band_index_long[ctx.header.sampleRateIndex][ra1 + 1] >> 1;
		// should not overflow
		int l = Math.min(ra1 + ra2 + 2, 22);
		g.regionSize[1] = band_index_long[ctx.header.sampleRateIndex][l] >> 1;
	}

	private void computeBandIndexes(Granule g) {
		if (g.blockType == 2) {
			if (g.switchPoint != 0) {
				if (ctx.header.sampleRateIndex == 8) {
					log.warn(String.format("Unimplemented switch point in 8kHz"));
				}
				// if switched mode, we handle the 36 first samples as
				// long blocks. For 8000Hz, we handle the 72 first
				// exponents as long blocks
				if (ctx.header.sampleRateIndex <= 2) {
					g.longEnd = 8;
				} else {
					g.longEnd = 6;
				}

				g.shortStart = 3;
			} else {
				g.longEnd = 0;
				g.shortStart = 0;
			}
		} else {
			g.shortStart = 13;
			g.longEnd = 22;
		}
	}

	/**
	 * Convert region offsets to region sizes and truncate
	 * size to big_values.
	 */
	private void regionOffset2size(Granule g) {
		g.regionSize[2] = 576 / 2;
		for (int i = 0, j = 0; i < 3; i++) {
			int k = Math.min(g.regionSize[i], g.bigValues);
			g.regionSize[i] = k - j;
			j = k;
		}
	}

	private void lsfSfExpand(int[] slen, int sf, int n1, int n2, int n3) {
		if (n3 != 0) {
			slen[3] = sf % n3;
			sf /= n3;
		} else {
			slen[3] = 0;
		}
		if (n2 != 0) {
			slen[2] = sf % n2;
			sf /= n2;
		} else {
			slen[2] = 0;
		}
		if (n1 != 0) {
			slen[1] = sf % n1;
			sf /= n1;
		} else {
			slen[1] = 0;
		}
		slen[0] = sf;
	}

    public void exponentsFromScaleFactors(Granule g, int[] exponents) {
    	int expPtr = 0;
    	int gain = g.globalGain - 210;
    	int shift = g.scalefacScale + 1;
    	int[] gains = new int[3];

        int[] bstab = band_size_long[ctx.header.sampleRateIndex];
        int[] pretab = mpa_pretab[g.preflag];

        for (int i = 0; i < g.longEnd; i++) {
            int v0 = gain - ((g.scaleFactors[i] + pretab[ i ]) << shift) + 400;
            int len = bstab[i];
            for (int j = len; j > 0; j-- ) {
                exponents[expPtr++] = v0;
            }
        }

        if (g.shortStart < 13) {
            bstab = band_size_short[ctx.header.sampleRateIndex];
            gains[0] = gain - (g.subblockGain[0] << 3);
            gains[1] = gain - (g.subblockGain[1] << 3);
            gains[2] = gain - (g.subblockGain[2] << 3);
            int k = g.longEnd;
            for (int i = g.shortStart; i < 13; i++ ) {
                int len = bstab[i];
                for (int l = 0; l < 3; l++) {
                    int v0 = gains[l] - (g.scaleFactors[k++] << shift) + 400;
                    for (int j = len; j > 0; j--) {
                        exponents[expPtr++] = v0;
                    }
                }
            }
        }
    }

    /* compute value^(4/3) * 2^(exponent/4). It normalized to FRAC_BITS */
    private int l3Unscale(int value, int exponent) {
    	int e = Mp3Data.table_4_3_exp  [4 * value + (exponent & 3)];
    	int m = Mp3Data.table_4_3_value[4 * value + (exponent & 3)];
    	e -= exponent >> 2;
        if (e > 31) {
        	return 0;
        }
        m = (m + (1 << (e - 1))) >> e;

        return m;
    }

    private void huffmanDecode(Granule g, int[] exponents) {
    	// low frequencies (called big values)
    	int sIndex = 0;
    	for (int i = 0; i < 3; i++) {
    		int j = g.regionSize[i];
    		if (j == 0) {
    			continue;
    		}

    		// select vlc table
    		int k = g.tableSelect[i];
    		int l = mpa_huff_data[k][0];
    		int linbits = mpa_huff_data[k][1];
    		VLC vlc = huff_vlc[l];

    		if (l == 0) {
    			Arrays.fill(g.sbHybrid, sIndex, sIndex + 2 * j, 0f);
    			sIndex += 2 * j;
    			continue;
    		}

    		// read huffcode and compute each couple
    		for (; j > 0; j--) {
    			int y = vlc.getVLC2(bb, 3);

    			if (y == 0) {
    				g.sbHybrid[sIndex] = 0;
    				g.sbHybrid[sIndex + 1] = 0;
    				sIndex += 2;
    				continue;
    			}

    			int exponent = exponents[sIndex];

    			if ((y & 16) != 0) {
    				int x = y >> 5;
                    y = y & 0x0F;
                    if (x < 15) {
                    	float v = expval_table[exponent][x];
                    	if (bb.readBool()) {
                    		v = -v;
                    	}
                    	g.sbHybrid[sIndex] = v;
                    } else {
                    	x += bb.read(linbits);
                    	float v = l3Unscale(x, exponent);
                    	if (bb.readBool()) {
                    		v = -v;
                    	}
                    	g.sbHybrid[sIndex] = v;
                    }
                    if (y < 15) {
                    	float v = expval_table[exponent][y];;
                    	if (bb.readBool()) {
                    		v = -v;
                    	}
                    	g.sbHybrid[sIndex + 1] = v;
                    } else {
                    	y += bb.read(linbits);
                    	float v = l3Unscale(y, exponent);
                    	if (bb.readBool()) {
                    		v = -v;
                    	}
                    	g.sbHybrid[sIndex + 1] = v;
                    }
    			} else {
    				int x = y >> 5;
                    y = y & 0x0F;
                    x += y;
                    if (x < 15) {
                    	float v = expval_table[exponent][x];;
                    	if (bb.readBool()) {
                    		v = -v;
                    	}
                    	g.sbHybrid[sIndex + (y != 0 ? 1 : 0)] = v;
                    } else {
                    	x += bb.read(linbits);
                    	float v = l3Unscale(y, exponent);
                    	if (bb.readBool()) {
                    		v = -v;
                    	}
                    	g.sbHybrid[sIndex + (y != 0 ? 1 : 0)] = v;
                    }
                    g.sbHybrid[sIndex + (y == 0 ? 1 : 0)] = 0;
    			}
    			sIndex += 2;
    		}
    	}

    	// high frequencies
    	VLC vlc = huff_quad_vlc[g.count1tableSelect];
    	while (sIndex <= 572) {
    		if (bb.getBitsRead() >= g.granuleStartPosition + g.part23Length) {
    			break;
    		}
    		int code = vlc.getVLC2(bb);
    		g.sbHybrid[sIndex + 0] = 0;
    		g.sbHybrid[sIndex + 1] = 0;
    		g.sbHybrid[sIndex + 2] = 0;
    		g.sbHybrid[sIndex + 3] = 0;
    		while (code != 0) {
    			int pos = sIndex + idxtab[code];
    			code ^= 8 >> idxtab[code];
    			float v = exp_table[exponents[pos]];
    			if (bb.readBool()) {
    				v = -v;
    			}
    			g.sbHybrid[pos] = v;
    		}
    		sIndex += 4;
    	}

    	// skip extension bits
    	int bitsLeft = g.granuleStartPosition + g.part23Length - bb.getBitsRead();
    	if (bitsLeft > 0) {
    		bb.skip(bitsLeft);
    	}

    	Arrays.fill(g.sbHybrid, sIndex, 576, 0f);
    }

    private void computeStereo(Granule g0, Granule g1) {
    	float isTab[][];
    	int sfMax;
    	boolean nonZeroFoundShort[] = new boolean[3];

    	// intensity stereo
    	if ((ctx.header.modeExt & MODE_EXT_I_STEREO) != 0) {
    		if (ctx.header.lsf == 0) {
    			isTab = is_table;
    			sfMax = 7;
    		} else {
    			isTab = is_table_lsf[g1.scalefacCompress & 1];
    			sfMax = 16;
    		}

    		int tab0 = 576;
    		int tab1 = 576;

    		nonZeroFoundShort[0] = false;
    		nonZeroFoundShort[1] = false;
    		nonZeroFoundShort[2] = false;
    		int k = (13 - g1.shortStart) * 3 + g1.longEnd - 3;
    		for (int i = 12; i >= g1.shortStart; i--) {
    			// for last band, use previous scale factor
    			if (i != 11) {
    				k -= 3;
    			}
    			int len = band_size_short[ctx.header.sampleRateIndex][i];
    			for (int l = 2; l >= 0; k--) {
    				tab0 -= len;
    				tab1 -= len;
    				boolean doNonZero = nonZeroFoundShort[l];
    				int sf = 0;
    				if (!doNonZero) {
    					// test if non zero band. If so, stop doing i-stereo
    					for (int j = 0; j < len; j++) {
    						if (g1.sbHybrid[tab1 + j] != 0f) {
    							nonZeroFoundShort[l] = true;
    							doNonZero = true;
    							break;
    						}
    					}
    					if (!doNonZero) {
    						sf = g1.scaleFactors[k + l];
    						if (sf >= sfMax) {
    							doNonZero = true;
    						}
    					}
    				}
    				if (!doNonZero) {
    					float v1 = isTab[0][sf];
    					float v2 = isTab[1][sf];
    					for (int j = 0; j < len; j++) {
    						float tmp0 = g0.sbHybrid[tab0 + j];
    						g0.sbHybrid[tab0 + j] = tmp0 * v1;
    						g1.sbHybrid[tab1 + j] = tmp0 * v2;
    					}
    				} else {
    					if ((ctx.header.modeExt & MODE_EXT_MS_STEREO) != 0) {
    						// lower part of the spectrum: do ms stereo if enabled
    						for (int j = 0; j < len; j++) {
        						float tmp0 = g0.sbHybrid[tab0 + j];
        						float tmp1 = g1.sbHybrid[tab1 + j];
        						g0.sbHybrid[tab0 + j] = (float) ((tmp0 + tmp1) * ISQRT2);
        						g1.sbHybrid[tab1 + j] = (float) ((tmp0 - tmp1) * ISQRT2);
    						}
    					}
    				}
    			}
    		}

    		boolean nonZeroFound = nonZeroFoundShort[0] || nonZeroFoundShort[1] || nonZeroFoundShort[2];

    		for (int i = g1.longEnd - 1; i >= 0; i--) {
    			int len = band_size_long[ctx.header.sampleRateIndex][i];
    			tab0 -= len;
    			tab1 -= len;
    			// test if non zero band. If so, stop doing i-stereo
    			if (!nonZeroFound) {
    				for (int j = 0; j < len; j++) {
    					if (g1.sbHybrid[tab1 + j] != 0f) {
    						nonZeroFound = true;
    						break;
    					}
    				}
    			}
    			int sf = 0;
    			if (!nonZeroFound) {
    				// for last band, use previous scale factor
    				k = (i == 21) ? 20 : i;
    				sf = g1.scaleFactors[k];
    				if (sf >= sfMax) {
    					nonZeroFound = true;
    				}
    			}
    			if (!nonZeroFound) {
    				float v1 = isTab[0][sf];
    				float v2 = isTab[1][sf];
    				for (int j = 0; j < len; j++) {
						float tmp0 = g0.sbHybrid[tab0 + j];
						g0.sbHybrid[tab0 + j] = tmp0 * v1;
						g1.sbHybrid[tab1 + j] = tmp0 * v2;
    				}
    			} else {
					if ((ctx.header.modeExt & MODE_EXT_MS_STEREO) != 0) {
						// lower part of the spectrum: do ms stereo if enabled
						for (int j = 0; j < len; j++) {
    						float tmp0 = g0.sbHybrid[tab0 + j];
    						float tmp1 = g1.sbHybrid[tab1 + j];
    						g0.sbHybrid[tab0 + j] = (float) ((tmp0 + tmp1) * ISQRT2);
    						g1.sbHybrid[tab1 + j] = (float) ((tmp0 - tmp1) * ISQRT2);
						}
					}
    			}
    		}
    	} else if ((ctx.header.modeExt & MODE_EXT_MS_STEREO) != 0) {
    		// ms stereo ONLY
    		// NOTE: the 1/sqrt(2) normalization factor is included in the global gain
    		FloatDSP.butterflies(g0.sbHybrid, 0, g1.sbHybrid, 0, 576);
    	}
    }

    /* Reorder short blocks from bitstream order to interleaved order. It
       would be faster to do it in parsing, but the code would be far more
       complicated */
    private void reorderBlock(Granule g) {
    	if (g.blockType != 2) {
    		return;
    	}

    	final float tmp[] = new float[576];
    	int ptr;
    	if (g.switchPoint != 0) {
    		if (ctx.header.sampleRateIndex != 8) {
    			ptr = 36;
    		} else {
    			ptr = 72;
    		}
    	} else {
    		ptr = 0;
    	}

    	for (int i = g.shortStart; i < 13; i++) {
    		int len = band_size_short[ctx.header.sampleRateIndex][i];
    		int ptr1 = ptr;
    		int dst = 0;
    		for (int j = len; j > 0; j--) {
    			tmp[dst++] = g.sbHybrid[ptr + 0 * len];
    			tmp[dst++] = g.sbHybrid[ptr + 1 * len];
    			tmp[dst++] = g.sbHybrid[ptr + 2 * len];
    			ptr++;
    		}
    		ptr += 2 * len;
    		System.arraycopy(tmp, 0, g.sbHybrid, ptr1, 3 * len);
    	}
    }

    private void AA(float[] ptr, int ptrOffset, int j) {
    	float tmp0 = ptr[ptrOffset - 1 - j];
    	float tmp1 = ptr[ptrOffset     + j];
    	ptr[ptrOffset - 1 - j] = tmp0 * csa_table[j][0] - tmp1 * csa_table[j][1];
    	ptr[ptrOffset     + j] = tmp0 * csa_table[j][1] + tmp1 * csa_table[j][0];
    }

    private void computeAntialias(Granule g) {
    	int n;

    	// we antialias only "long" bands
    	if (g.blockType == 2) {
    		if (g.switchPoint == 0) {
    			return;
    		}
    		n = 1;
    	} else {
    		n = SBLIMIT - 1;
    	}

    	int ptr = 18;
    	for (int i = n; i > 0; i--) {
    		AA(g.sbHybrid, ptr, 0);
    		AA(g.sbHybrid, ptr, 1);
    		AA(g.sbHybrid, ptr, 2);
    		AA(g.sbHybrid, ptr, 3);
    		AA(g.sbHybrid, ptr, 4);
    		AA(g.sbHybrid, ptr, 5);
    		AA(g.sbHybrid, ptr, 6);
    		AA(g.sbHybrid, ptr, 7);

    		ptr += 18;
    	}
    }

    private static final float C3 = (float) (0.86602540378443864676/2);
    private static final float C4 = (float) (0.70710678118654752439/2); //0.5 / cos(pi*(9)/36)
    private static final float C5 = (float) (0.51763809020504152469/2); //0.5 / cos(pi*(5)/36)
    private static final float C6 = (float) (1.93185165257813657349/4); //0.5 / cos(pi*(15)/36)

    /* 12 points IMDCT. We compute it "by hand" by factorizing obvious cases. */
    private void imdct12(float[] out, int outOffset, float[] in, int inOffset) {
    	float in0 = in[inOffset + 0 * 3];
    	float in1 = in[inOffset + 1 * 3] + in[inOffset + 0 * 3];
    	float in2 = in[inOffset + 2 * 3] + in[inOffset + 1 * 3];
    	float in3 = in[inOffset + 3 * 3] + in[inOffset + 2 * 3];
    	float in4 = in[inOffset + 4 * 3] + in[inOffset + 3 * 3];
    	float in5 = in[inOffset + 5 * 3] + in[inOffset + 4 * 3];
    	in5 += in3;
    	in3 += in1;

    	in2 = in2 * C3 * 2f;
    	in3 = in3 * C3 * 4f;

    	float t1 = in0 - in4;
    	float t2 = (in1 - in5) * C4 * 2f;

    	out[outOffset +  7] = t1 + t2;
    	out[outOffset + 10] = t1 + t2;
    	out[outOffset +  1] = t1 - t2;
    	out[outOffset +  4] = t1 - t2;

    	in0 += in4 * 0.5f;
    	in4  = in0 + in2;
    	in5 += 2f * in1;
    	in1  = (in5 + in3) * C5;
    	out[outOffset + 8] = in4 + in1;
    	out[outOffset + 9] = in4 + in1;
    	out[outOffset + 2] = in4 - in1;
    	out[outOffset + 3] = in4 - in1;

    	in0 -= in2;
    	in5  = (in5 - in3) * C6 * 2f;
    	out[outOffset +  0] = in0 - in5;
    	out[outOffset +  5] = in0 - in5;
    	out[outOffset +  6] = in0 + in5;
    	out[outOffset + 11] = in0 + in5;
    }

    private void computeImdct(Granule g, float[] sbSamples, int sbSamplesOffset, float[] mdctbuf) {
    	float out2[] = new float[12];

    	// find last non zero block
    	int ptr = 576;
    	int ptr1 = 2 * 18;
    	float [] p = g.sbHybrid;
    	while (ptr >= ptr1) {
    		ptr -= 6;
    		if (p[ptr] != 0f || p[ptr + 1] != 0f || p[ptr + 2] != 0f || p[ptr + 3] != 0f || p[ptr + 4] != 0f || p[ptr + 5] != 0f) {
    			break;
    		}
    	}
    	int sblimit = ptr / 18 + 1;

    	int mdctLongEnd;
    	if (g.blockType == 2) {
    		if (g.switchPoint != 0) {
    			mdctLongEnd = 2;
    		} else {
    			mdctLongEnd = 0;
    		}
    	} else {
    		mdctLongEnd = sblimit;
    	}

    	Mp3Dsp.imdct36Blocks(sbSamples, sbSamplesOffset, mdctbuf, 0, g.sbHybrid, 0, mdctLongEnd, g.switchPoint, g.blockType);

    	int buf = 4 * 18 * (mdctLongEnd >> 2) + (mdctLongEnd & 3);
    	ptr = 18 * mdctLongEnd;

    	for (int j = mdctLongEnd; j < sblimit; j++) {
    		// select frequency inversion
    		float win[] = mdct_win[2 + (4 & -(j & 1))];
    		int outPtr = j;

    		for (int i = 0; i < 6; i++) {
    			sbSamples[outPtr] = mdctbuf[buf + 4 * i];
    			outPtr += SBLIMIT;
    		}
    		imdct12(out2, 0, g.sbHybrid, ptr + 0);
    		for (int i = 0; i < 6; i++) {
    			sbSamples[outPtr] = out2[i] * win[i] + mdctbuf[buf + 4 * (i + 6 * 1)];
    			mdctbuf[buf + 4 * (i + 6 * 2)] = out2[i + 6] * win[i + 6];
    			outPtr += SBLIMIT;
    		}
    		imdct12(out2, 0, g.sbHybrid, ptr + 1);
    		for (int i = 0; i < 6; i++) {
    			sbSamples[outPtr] = out2[i] * win[i] + mdctbuf[buf + 4 * (i + 6 * 2)];
    			mdctbuf[buf + 4 * (i + 6 * 0)] = out2[i + 6] * win[i + 6];
    			outPtr += SBLIMIT;
    		}
    		imdct12(out2, 0, g.sbHybrid, ptr + 2);
    		for (int i = 0; i < 6; i++) {
    			mdctbuf[buf + 4 * (i + 6 * 0)] = out2[i    ] * win[i    ] + mdctbuf[buf + 4 * (i + 6 * 0)];
    			mdctbuf[buf + 4 * (i + 6 * 1)] = out2[i + 6] * win[i + 6];
    			mdctbuf[buf + 4 * (i + 6 * 2)] = 0f;
    		}
    		ptr += 18;
    		buf += (j & 3) != 3 ? 1 : (4 * 18 - 3);
    	}
    	// zero bands
    	for (int j = sblimit; j < SBLIMIT; j++) {
    		// overlap
    		int outPtr = j;
    		for (int i = 0; i < 18; i++) {
    			sbSamples[outPtr] = mdctbuf[buf + 4 * i];
    			mdctbuf[buf + 4 * i] = 0f;
    			outPtr += SBLIMIT;
    		}
    		buf += (j & 3) != 3 ? 1 : (4 * 18 - 3);
    	}
    }

	// main layer3 decoding function
	private int decodeLayer3(int frameStart) {
		int mainDataBegin;
		int nbGranules;
		int[] exponents = new int[576];
		Mp3Header s = ctx.header;

		// read side info
		if (s.lsf != 0) {
			mainDataBegin = br.read(8);
			br.skip(s.nbChannels);
			nbGranules = 1;
		} else {
			mainDataBegin = br.read(9);
			if (s.nbChannels == 2) {
				br.skip(3);
			} else {
				br.skip(5);
			}
			nbGranules = 2;
			for (int ch = 0; ch < s.nbChannels; ch++) {
				ctx.granules[ch][0].scfsi = 0; // all scale factors are transmitted
				ctx.granules[ch][1].scfsi = br.read(4);
			}
		}

		for (int gr = 0; gr < nbGranules; gr++) {
			for (int ch = 0; ch < s.nbChannels; ch++) {
				Granule g = ctx.granules[ch][gr];
				g.part23Length = br.read(12);
				g.bigValues = br.read(9);
				if (g.bigValues > 288) {
					log.error(String.format("bigValues too big %d", g.bigValues));
					return MP3_ERROR;
				}

				g.globalGain = br.read(8);
				// if MS stereo only is selected, we precompute the
				// 1/sqrt(2) renormalization factor
				if ((s.modeExt & (MODE_EXT_MS_STEREO | MODE_EXT_I_STEREO)) == MODE_EXT_MS_STEREO) {
					g.globalGain -= 2;
				}
				if (s.lsf != 0) {
					g.scalefacCompress = br.read(9);
				} else {
					g.scalefacCompress = br.read(4);
				}
				boolean blocksplitFlag = br.readBool();
				if (blocksplitFlag) {
					g.blockType = br.read(2);
					if (g.blockType == 0) {
						log.error(String.format("invalid block type"));
						return MP3_ERROR;
					}
					g.switchPoint = br.read1();
					for (int i = 0; i < 2; i++) {
						g.tableSelect[i] = br.read(5);
					}
					for (int i = 0; i < 3; i++) {
						g.subblockGain[i] = br.read(3);
					}
					initShortRegion(g);
				} else {
					g.blockType = 0;
					g.switchPoint = 0;
					for (int i = 0; i < 3; i++) {
						g.tableSelect[i] = br.read(5);
					}
					// compute Huffman coded region sizes
					int regionAddress1 = br.read(4);
					int regionAddress2 = br.read(3);
					initLongRegion(g, regionAddress1, regionAddress2);
				}
				regionOffset2size(g);
				computeBandIndexes(g);

				g.preflag = 0;
				if (s.lsf == 0) {
					g.preflag = br.read1();
				}
				g.scalefacScale = br.read1();
				g.count1tableSelect = br.read1();
			}
		}

		if (ctx.aduMode == 0) {
			int currentPosition = br.getBytesRead();
			int copyLength = ctx.header.frameSize - (currentPosition - frameStart);
			int bitsToSkip = bb.getBitsWritten() - bb.getBitsRead() - (mainDataBegin << 3);

			for (int i = 0; i < copyLength; i++) {
				bb.writeByte(br.readByte());
			}

			bb.skip(bitsToSkip);
		}

		for (int gr = 0; gr < nbGranules; gr++) {
			for (int ch = 0; ch < s.nbChannels; ch++) {
				Granule g = ctx.granules[ch][gr];
				g.granuleStartPosition = bb.getBitsRead();

				if (s.lsf == 0) {
					// MPEG1 scale factors
					int slen1 = slen_table[0][g.scalefacCompress];
					int slen2 = slen_table[1][g.scalefacCompress];
					if (g.blockType == 2) {
						int n = g.switchPoint != 0 ? 17 : 18;
						int j = 0;
						if (slen1 != 0) {
							for (int i = 0; i < n; i++) {
								g.scaleFactors[j++] = bb.read(slen1);
							}
						} else {
							for (int i = 0; i < n; i++) {
								g.scaleFactors[j++] = 0;
							}
						}
						if (slen2 != 0) {
							for (int i = 0; i < 18; i++) {
								g.scaleFactors[j++] = bb.read(slen2);
							}
							for (int i = 0; i < 3; i++) {
								g.scaleFactors[j++] = 0;
							}
						} else {
							for (int i = 0; i < 21; i++) {
								g.scaleFactors[j++] = 0;
							}
						}
					} else {
						int sc[] = ctx.granules[ch][0].scaleFactors;
						int j = 0;
						for (int k = 0; k < 4; k++) {
							int n = (k == 0 ? 6 : 5);
							if ((g.scfsi & (0x8 >> k)) == 0) {
								int slen = (k < 2 ? slen1 : slen2);
								if (slen != 0) {
									for (int i = 0; i < n; i++) {
										g.scaleFactors[j++] = bb.read(slen);
									}
								} else {
									for (int i = 0; i < n; i++) {
										g.scaleFactors[j++] = 0;
									}
								}
							} else {
								// simple copy from last granule
								for (int i = 0; i < n; i++) {
									g.scaleFactors[j] = sc[j];
									j++;
								}
							}
						}
						g.scaleFactors[j++] = 0;
					}
				} else {
					int tindex;
					int tindex2;
					int[] slen = new int[4];

					// LSF scale factors
					if (g.blockType == 2) {
						tindex = g.switchPoint != 0 ? 2 : 1;
					} else {
						tindex = 0;
					}

					int sf = g.scalefacCompress;
					if ((s.modeExt & MODE_EXT_I_STEREO) != 0 && ch == 1) {
						// intensity stereo case
						sf >>= 1;
						if (sf < 180) {
							lsfSfExpand(slen, sf, 6, 6, 0);
							tindex2 = 3;
						} else if (sf < 244) {
							lsfSfExpand(slen, sf - 180, 4, 4, 0);
							tindex2 = 4;
						} else {
							lsfSfExpand(slen, sf - 244, 3, 0, 0);
							tindex2 = 5;
						}
					} else {
						// normal case
						if (sf < 400) {
							lsfSfExpand(slen, sf, 5, 4, 4);
							tindex2 = 0;
						} else if (sf < 500) {
							lsfSfExpand(slen, sf - 400, 5, 4, 0);
							tindex2 = 1;
						} else {
							lsfSfExpand(slen, sf - 500, 3, 0, 0);
							tindex2 = 2;
							g.preflag = 1;
						}
					}

					int j = 0;
					for (int k = 0; k < 4; k++) {
						int n = lsf_nsf_table[tindex2][tindex][k];
						int sl = slen[k];
						if (sl != 0) {
							for (int i = 0; i < n; i++) {
								g.scaleFactors[j++] = bb.read(sl);
							}
						} else {
							for (int i = 0; i < n; i++) {
								g.scaleFactors[j++] = 0;
							}
						}
					}
					for (; j < 40; j++) {
						g.scaleFactors[j] = 0;
					}
				}

				exponentsFromScaleFactors(g, exponents);

				// read Huffman coded residue
				huffmanDecode(g, exponents);
			}

			if (s.mode == MP3_JSTEREO) {
				computeStereo(ctx.granules[0][gr], ctx.granules[1][gr]);
			}

			for (int ch = 0; ch < s.nbChannels; ch++) {
				Granule g = ctx.granules[ch][gr];

				reorderBlock(g);
				computeAntialias(g);
				computeImdct(g, ctx.sbSamples[ch], 18 * gr * SBLIMIT, ctx.mdctBuf[ch]);
			}
		}

		return nbGranules * 18;
	}

	private int decodeFrame(int frameStart) {
		int nbFrames;

		if (ctx.header.errorProtection != 0) {
			br.skip(16);
		}

		switch (ctx.header.layer) {
			case 1:
				ctx.frameSize = 384;
				nbFrames = decodeLayer1();
				break;
			case 2:
				ctx.frameSize = 1152;
				nbFrames = decodeLayer2();
				break;
			case 3:
				ctx.frameSize = ctx.header.lsf != 0 ? 576 : 1152;
				// FALLTHROUGH
			default:
				nbFrames = decodeLayer3(frameStart);
				break;
		}

		if (nbFrames < 0) {
			return nbFrames;
		}

		// get output buffer
		if (ctx.samples == null) {
			ctx.samples = new float[ctx.header.nbChannels][ctx.frameSize];
		}

		// apply the synthesis filter
		for (int ch = 0; ch < ctx.header.nbChannels; ch++) {
			int sampleStride = 1;
			int samplesPtr = 0;
			for (int i = 0; i < nbFrames; i++) {
				Mp3Dsp.synthFilter(ctx, ch, ctx.samples[ch], samplesPtr, sampleStride, ctx.sbSamples[ch], i * SBLIMIT);
				samplesPtr += 32 * sampleStride;
			}
		}

		return nbFrames * 32 * 4 * ctx.header.nbChannels;
	}

	@Override
	public int decode(int inputAddr, int inputLength, int outputAddr) {
		br = new BitReader(inputAddr, inputLength);
		ctx.br = br;

		while (br.peek(8) == 0) {
			br.skip(8);
		}

		if (br.getBitsLeft() < HEADER_SIZE * 8) {
			return MP3_ERROR;
		}

		int frameStart = br.getBytesRead();
		int header = br.read(32);
		if (mpaCheckHeader(header) < 0) {
			log.error(String.format("Header missing"));
			return MP3_ERROR;
		}

		if (decodeHeader(ctx.header, header) == 1) {
			// free format: prepare to compute frame size
			ctx.header.frameSize = -1;
			return MP3_ERROR;
		}

		int ret = decodeFrame(frameStart);
		if (ret < 0) {
			return ret;
		}

		CodecUtils.writeOutput(ctx.samples, outputAddr, ctx.frameSize, ctx.header.nbChannels);

		return ctx.header.frameSize;
	}

	@Override
	public int getNumberOfSamples() {
		return ctx.frameSize;
	}
}
