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
package jpcsp.media.codec.atrac3plus;

import static java.lang.Math.abs;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData1.atrac3p_spectra_tabs;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData2.atrac3p_ct_restricted_to_full;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData2.atrac3p_qu_num_to_seg;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData2.atrac3p_qu_to_subband;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData2.atrac3p_sf_shapes;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData2.atrac3p_subband_to_num_powgrps;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData2.atrac3p_wl_shapes;
import static jpcsp.media.codec.atrac3plus.Atrac3plusData2.atrac3p_wl_weights;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.AT3P_ERROR;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.ATRAC3P_POWER_COMP_OFF;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.ATRAC3P_SUBBANDS;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.ATRAC3P_SUBBAND_SAMPLES;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.CH_UNIT_STEREO;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDsp.ff_atrac3p_mant_tab;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDsp.ff_atrac3p_qu_to_spec_pos;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDsp.ff_atrac3p_sf_tab;
import static jpcsp.media.codec.util.CodecUtils.avLog2;
import static jpcsp.util.Utilities.signExtend;

import java.util.Arrays;

import jpcsp.media.codec.atrac3plus.Atrac3plusData1.Atrac3pSpecCodeTab;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.VLC;

import org.apache.log4j.Logger;

/*
 * Based on the FFmpeg version from Maxim Poliakovski.
 * All credits go to him.
 */
public class ChannelUnit {
	private static Logger log = Atrac3plusDecoder.log;
	public ChannelUnitContext ctx = new ChannelUnitContext();
	private BitReader br;
	private Atrac3plusDsp dsp;
	private int numChannels;

	private static final VLC wl_vlc_tabs[] = new VLC[4];
	private static final VLC sf_vlc_tabs[] = new VLC[8];
	private static final VLC ct_vlc_tabs[] = new VLC[4];
	private static final VLC spec_vlc_tabs[] = new VLC[112];
	private static final VLC gain_vlc_tabs[] = new VLC[11];
	private static final VLC tone_vlc_tabs[] = new VLC[7];

    private static final int wl_nb_bits[]  = { 2, 3, 5, 5 };
    private static final int wl_nb_codes[] = { 3, 5, 8, 8 };
    private static final int wl_bits[][] = {
        Atrac3plusData2.atrac3p_wl_huff_bits1, Atrac3plusData2.atrac3p_wl_huff_bits2,
        Atrac3plusData2.atrac3p_wl_huff_bits3, Atrac3plusData2.atrac3p_wl_huff_bits4
    };
    private static final int wl_codes[][] = {
    	Atrac3plusData2.atrac3p_wl_huff_code1, Atrac3plusData2.atrac3p_wl_huff_code2,
    	Atrac3plusData2.atrac3p_wl_huff_code3, Atrac3plusData2.atrac3p_wl_huff_code4
    };
    private static final int wl_xlats[][] = {
    	Atrac3plusData2.atrac3p_wl_huff_xlat1, Atrac3plusData2.atrac3p_wl_huff_xlat2, null, null
    };

    private static final int ct_nb_bits[]  = { 3, 4, 4, 4 };
    private static final int ct_nb_codes[] = { 4, 8, 8, 8 };
    private static final int ct_bits[][]  = {
    	Atrac3plusData2.atrac3p_ct_huff_bits1, Atrac3plusData2.atrac3p_ct_huff_bits2,
    	Atrac3plusData2.atrac3p_ct_huff_bits2, Atrac3plusData2.atrac3p_ct_huff_bits3
    };
    private static final int ct_codes[][] = {
    	Atrac3plusData2.atrac3p_ct_huff_code1, Atrac3plusData2.atrac3p_ct_huff_code2,
    	Atrac3plusData2.atrac3p_ct_huff_code2, Atrac3plusData2.atrac3p_ct_huff_code3
    };
    private static final int ct_xlats[][] = {
        null, null, Atrac3plusData2.atrac3p_ct_huff_xlat1, null
    };

    private static final int sf_nb_bits[]  = {  9,  9,  9,  9,  6,  6,  7,  7 };
    private static final int sf_nb_codes[] = { 64, 64, 64, 64, 16, 16, 16, 16 };
    private static final int sf_bits[][]  = {
    	Atrac3plusData2.atrac3p_sf_huff_bits1, Atrac3plusData2.atrac3p_sf_huff_bits1, Atrac3plusData2.atrac3p_sf_huff_bits2,
    	Atrac3plusData2.atrac3p_sf_huff_bits3, Atrac3plusData2.atrac3p_sf_huff_bits4, Atrac3plusData2.atrac3p_sf_huff_bits4,
    	Atrac3plusData2.atrac3p_sf_huff_bits5, Atrac3plusData2.atrac3p_sf_huff_bits6
    };
    private static final int sf_codes[][] = {
    	Atrac3plusData2.atrac3p_sf_huff_code1, Atrac3plusData2.atrac3p_sf_huff_code1, Atrac3plusData2.atrac3p_sf_huff_code2,
    	Atrac3plusData2.atrac3p_sf_huff_code3, Atrac3plusData2.atrac3p_sf_huff_code4, Atrac3plusData2.atrac3p_sf_huff_code4,
    	Atrac3plusData2.atrac3p_sf_huff_code5, Atrac3plusData2.atrac3p_sf_huff_code6
    };
    private static final int sf_xlats[][] = {
    	Atrac3plusData2.atrac3p_sf_huff_xlat1, Atrac3plusData2.atrac3p_sf_huff_xlat2, null, null,
    	Atrac3plusData2.atrac3p_sf_huff_xlat4, Atrac3plusData2.atrac3p_sf_huff_xlat5, null, null
    };

    private static final int gain_cbs[][] = {
    	Atrac3plusData2.atrac3p_huff_gain_npoints1_cb, Atrac3plusData2.atrac3p_huff_gain_npoints1_cb,
    	Atrac3plusData2.atrac3p_huff_gain_lev1_cb, Atrac3plusData2.atrac3p_huff_gain_lev2_cb,
    	Atrac3plusData2.atrac3p_huff_gain_lev3_cb, Atrac3plusData2.atrac3p_huff_gain_lev4_cb,
    	Atrac3plusData2.atrac3p_huff_gain_loc3_cb, Atrac3plusData2.atrac3p_huff_gain_loc1_cb,
    	Atrac3plusData2.atrac3p_huff_gain_loc4_cb, Atrac3plusData2.atrac3p_huff_gain_loc2_cb,
    	Atrac3plusData2.atrac3p_huff_gain_loc5_cb
    };
    private static final int gain_xlats[][] = {
        null, Atrac3plusData2.atrac3p_huff_gain_npoints2_xlat, Atrac3plusData2.atrac3p_huff_gain_lev1_xlat,
        Atrac3plusData2.atrac3p_huff_gain_lev2_xlat, Atrac3plusData2.atrac3p_huff_gain_lev3_xlat,
        Atrac3plusData2.atrac3p_huff_gain_lev4_xlat, Atrac3plusData2.atrac3p_huff_gain_loc3_xlat,
        Atrac3plusData2.atrac3p_huff_gain_loc1_xlat, Atrac3plusData2.atrac3p_huff_gain_loc4_xlat,
        Atrac3plusData2.atrac3p_huff_gain_loc2_xlat, Atrac3plusData2.atrac3p_huff_gain_loc5_xlat
    };

    private static final int tone_cbs[][] = {
    	Atrac3plusData2.atrac3p_huff_tonebands_cb,  Atrac3plusData2.atrac3p_huff_numwavs1_cb,
    	Atrac3plusData2.atrac3p_huff_numwavs2_cb,   Atrac3plusData2.atrac3p_huff_wav_ampsf1_cb,
    	Atrac3plusData2.atrac3p_huff_wav_ampsf2_cb, Atrac3plusData2.atrac3p_huff_wav_ampsf3_cb,
    	Atrac3plusData2.atrac3p_huff_freq_cb
    };
    private static final int tone_xlats[][] = {
        null, null, Atrac3plusData2.atrac3p_huff_numwavs2_xlat, Atrac3plusData2.atrac3p_huff_wav_ampsf1_xlat,
        Atrac3plusData2.atrac3p_huff_wav_ampsf2_xlat, Atrac3plusData2.atrac3p_huff_wav_ampsf3_xlat,
        Atrac3plusData2.atrac3p_huff_freq_xlat
    };

	public static void init() {
		for (int i = 0; i < 4; i++) {
			wl_vlc_tabs[i] = new VLC();
			wl_vlc_tabs[i].initVLCSparse(wl_nb_bits[i], wl_nb_codes[i], wl_bits[i], wl_codes[i], wl_xlats[i]);
			ct_vlc_tabs[i] = new VLC();
			ct_vlc_tabs[i].initVLCSparse(ct_nb_bits[i], ct_nb_codes[i], ct_bits[i], ct_codes[i], ct_xlats[i]);
		}
		for (int i = 0; i < 8; i++) {
			sf_vlc_tabs[i] = new VLC();
			sf_vlc_tabs[i].initVLCSparse(sf_nb_bits[i], sf_nb_codes[i], sf_bits[i], sf_codes[i], sf_xlats[i]);
		}

	    /* build huffman tables for spectrum decoding */
	    for (int i = 0; i < 112; i++) {
	        if (atrac3p_spectra_tabs[i].cb != null) {
	        	spec_vlc_tabs[i] = new VLC();
	        	buildCanonicalHuff(atrac3p_spectra_tabs[i].cb, atrac3p_spectra_tabs[i].xlat, spec_vlc_tabs[i]);
	        }
	    }

	    /* build huffman tables for gain data decoding */
	    for (int i = 0; i < 11; i++) {
	    	gain_vlc_tabs[i] = new VLC();
	    	buildCanonicalHuff(gain_cbs[i], gain_xlats[i], gain_vlc_tabs[i]);
	    }

	    /* build huffman tables for tone decoding */
	    for (int i = 0; i < 7; i++) {
	    	tone_vlc_tabs[i] = new VLC();
	    	buildCanonicalHuff(tone_cbs[i], tone_xlats[i], tone_vlc_tabs[i]);
	    }
	}

	private static int buildCanonicalHuff(int[] cb, int[] xlat, VLC vlc) {
		int codes[] = new int[256];
		int bits[] = new int[256];
		int cbIndex = 0;
		int index = 0;
		int code = 0;
		int minLen = cb[cbIndex++]; // get shortest codeword length
		int maxLen = cb[cbIndex++]; // get longest  codeword length

		for (int b = minLen; b <= maxLen; b++) {
			for (int i = cb[cbIndex++]; i > 0; i--) {
				bits[index] = b;
				codes[index] = code++;
				index++;
			}
			code <<= 1;
		}

		return vlc.initVLCSparse(maxLen, index, bits, codes, xlat);
	}

	public void setBitReader(BitReader br) {
		this.br = br;
	}

	public void setDsp(Atrac3plusDsp dsp) {
		this.dsp = dsp;
	}

	public void setNumChannels(int numChannels) {
		this.numChannels = numChannels;
	}

	public int decode() {
		int ret;

		ctx.numQuantUnits = br.read(5) + 1;
		if (ctx.numQuantUnits > 28 && ctx.numQuantUnits < 32) {
			log.error(String.format("Invalid number of quantization units: %d", ctx.numQuantUnits));
			return AT3P_ERROR;
		}

		ctx.muteFlag = br.readBool();

		ret = decodeQuantWordlen();
		if (ret < 0) {
			return ret;
		}

		ctx.numSubbands = atrac3p_qu_to_subband[ctx.numQuantUnits - 1] + 1;
		ctx.numCodedSubbands = ctx.usedQuantUnits > 0 ? atrac3p_qu_to_subband[ctx.usedQuantUnits - 1] + 1 : 0;

		ret = decodeScaleFactors();
		if (ret < 0) {
			return ret;
		}

		ret = decodeCodeTableIndexes();
		if (ret < 0) {
			return ret;
		}

		decodeSpectrum();

		if (numChannels == 2) {
			getSubbandFlags(ctx.swapChannels, ctx.numCodedSubbands);
			getSubbandFlags(ctx.negateCoeffs, ctx.numCodedSubbands);
		}

		decodeWindowShape();

		ret = decodeGaincData();
		if (ret < 0) {
			return ret;
		}

		ret = decodeTonesInfo();
		if (ret < 0) {
			return ret;
		}

		ctx.noisePresent = br.readBool();
		if (ctx.noisePresent) {
			ctx.noiseLevelIndex = br.read(4);
			ctx.noiseTableIndex = br.read(4);
		}

		return 0;
	}

	/**
	 * Decode number of coded quantization units.
	 *
	 * @param[in,out] chan          ptr to the channel parameters
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int numCodedUnits(Channel chan) {
		chan.fillMode = br.read(2);
		if (chan.fillMode == 0) {
			chan.numCodedVals = ctx.numQuantUnits;
		} else {
			chan.numCodedVals = br.read(5);
			if (chan.numCodedVals > ctx.numQuantUnits) {
				log.error(String.format("Invalid number of transmitted units"));
				return AT3P_ERROR;
			}

			if (chan.fillMode == 3) {
				chan.splitPoint = br.read(2) + (chan.chNum << 1) + 1;
			}
		}

		return 0;
	}

	private int getDelta(int deltaBits) {
		return deltaBits <= 0 ? 0 : br.read(deltaBits);
	}

	/**
	 * Unpack vector quantization tables.
	 *
	 * @param[in]    start_val    start value for the unpacked table
	 * @param[in]    shape_vec    ptr to table to unpack
	 * @param[out]   dst          ptr to output array
	 * @param[in]    num_values   number of values to unpack
	 */
	private void unpackVqShape(int startVal, int[] shapeVec, int[] dst, int numValues) {
		if (numValues > 0) {
			dst[0] = startVal;
			dst[1] = startVal;
			dst[2] = startVal;
			for (int i = 3; i < numValues; i++) {
				dst[i] = startVal - shapeVec[atrac3p_qu_num_to_seg[i] - 1];
			}
		}
	}

	private void unpackSfVqShape(int[] dst, int numValues) {
		int startVal = br.read(6);
		unpackVqShape(startVal, atrac3p_sf_shapes[br.read(6)], dst, numValues);
	}

	/**
	 * Add weighting coefficients to the decoded word-length information.
	 *
	 * @param[in,out] chan          ptr to the channel parameters
	 * @param[in]     wtab_idx      index of the table of weights
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int addWordlenWeights(Channel chan, int weightIdx) {
		int[] weigthsTab = atrac3p_wl_weights[chan.chNum * 3 + weightIdx - 1];

		for (int i = 0; i < ctx.numQuantUnits; i++) {
			chan.quWordlen[i] += weigthsTab[i];
			if (chan.quWordlen[i] < 0 || chan.quWordlen[i] > 7) {
				log.error(String.format("WL index out of range pos=%d, val=%d", i, chan.quWordlen[i]));
				return AT3P_ERROR;
			}
		}

		return 0;
	}

	/**
	 * Decode word length for each quantization unit of a channel.
	 *
	 * @param[in]     chNum        channel to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeChannelWordlen(int chNum) {
		int ret;
		Channel chan = ctx.channels[chNum];
		Channel refChan = ctx.channels[0];
		int weightIdx = 0;

		chan.fillMode = 0;

		switch (br.read(2)) { // switch according to coding mode
			case 0: // coded using constant number of bits
				for (int i = 0; i < ctx.numQuantUnits; i++) {
					chan.quWordlen[i] = br.read(3);
				}
				break;
			case 1:
				if (chNum > 0) {
					ret = numCodedUnits(chan);
					if (ret < 0) {
						return ret;
					}

					if (chan.numCodedVals > 0) {
						VLC vlcTab = wl_vlc_tabs[br.read(2)];

						for (int i = 0; i < chan.numCodedVals; i++) {
							int delta = vlcTab.getVLC2(br);
							chan.quWordlen[i] = (refChan.quWordlen[i] + delta) & 7;
						}
					}
				} else {
					weightIdx = br.read(2);
					ret = numCodedUnits(chan);
					if (ret < 0) {
						return ret;
					}

					if (chan.numCodedVals > 0) {
						int pos = br.read(5);
						if (pos > chan.numCodedVals) {
							log.error(String.format("WL mode 1: invalid position %d", pos));
							return AT3P_ERROR; 
						}

						int deltaBits = br.read(2);
						int minVal = br.read(3);

						for (int i = 0; i < pos; i++) {
							chan.quWordlen[i] = br.read(3);
						}

						for (int i = pos; i < chan.numCodedVals; i++) {
							chan.quWordlen[i] = (minVal + getDelta(deltaBits)) & 7;
						}
					}
				}
				break;
			case 2:
				ret = numCodedUnits(chan);
				if (ret < 0) {
					return ret;
				}

				if (chNum > 0 && chan.numCodedVals > 0) {
					VLC vlcTab = wl_vlc_tabs[br.read(2)];
					int delta = vlcTab.getVLC2(br);
					chan.quWordlen[0] = (refChan.quWordlen[0] + delta) & 7;

					for (int i = 1; i < chan.numCodedVals; i++) {
						int diff = refChan.quWordlen[i] - refChan.quWordlen[i - 1];
						delta = vlcTab.getVLC2(br);
						chan.quWordlen[i] = (chan.quWordlen[i - 1] + diff + delta) & 7;
					}
				} else if (chan.numCodedVals > 0) {
					boolean flag = br.readBool();
					VLC vlcTab = wl_vlc_tabs[br.read(1)];

					int startVal = br.read(3);
					unpackVqShape(startVal, atrac3p_wl_shapes[startVal][br.read(4)], chan.quWordlen, chan.numCodedVals);

					if (!flag) {
						for (int i = 0; i < chan.numCodedVals; i++) {
							int delta = vlcTab.getVLC2(br);
							chan.quWordlen[i] = (chan.quWordlen[i] + delta) & 7;
						}
					} else {
						int i;
						for (i = 0; i < (chan.numCodedVals & -2); i += 2) {
							if (!br.readBool()) {
								chan.quWordlen[i    ] = (chan.quWordlen[i    ] + vlcTab.getVLC2(br)) & 7;
								chan.quWordlen[i + 1] = (chan.quWordlen[i + 1] + vlcTab.getVLC2(br)) & 7;
							}
						}

						if ((chan.numCodedVals & 1) != 0) {
							chan.quWordlen[i] = (chan.quWordlen[i] + vlcTab.getVLC2(br)) & 7;
						}
					}
				}
				break;
			case 3:
				weightIdx = br.read(2);
				ret = numCodedUnits(chan);
				if (ret < 0) {
					return ret;
				}

				if (chan.numCodedVals > 0) {
					VLC vlcTab = wl_vlc_tabs[br.read(2)];

					// first coefficient is coded directly
					chan.quWordlen[0] = br.read(3);

					for (int i = 1; i < chan.numCodedVals; i++) {
						int delta = vlcTab.getVLC2(br);
						chan.quWordlen[i] = (chan.quWordlen[i - 1] + delta) & 7;
					}
				}
				break;
		}

		if (chan.fillMode == 2) {
			for (int i = chan.numCodedVals; i < ctx.numQuantUnits; i++) {
				chan.quWordlen[i] = (chNum > 0 ? br.read1() : 1);
			}
		} else if (chan.fillMode == 3) {
			int pos = (chNum > 0 ? chan.numCodedVals + chan.splitPoint : ctx.numQuantUnits - chan.splitPoint);
			for (int i = chan.numCodedVals; i < pos; i++) {
				chan.quWordlen[i] = 1;
			}
		}

		if (weightIdx != 0) {
			return addWordlenWeights(chan, weightIdx);
		}

		return 0;
	}

	/**
	 * Subtract weighting coefficients from decoded scalefactors.
	 *
	 * @param[in,out] chan          ptr to the channel parameters
	 * @param[in]     wtab_idx      index of table of weights
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int substractSfWeights(Channel chan, int wtabIdx) {
		int[] weigthsTab = Atrac3plusData2.atrac3p_sf_weights[wtabIdx - 1];

		for (int i = 0; i < ctx.usedQuantUnits; i++) {
			chan.quSfIdx[i] -= weigthsTab[i];
			if (chan.quSfIdx[i] < 0 || chan.quSfIdx[i] > 63) {
				log.error(String.format("SF index out of range pos=%d, val=%d", i, chan.quSfIdx[i]));
				return AT3P_ERROR;
			}
		}

		return 0;
	}

	/**
	 * Decode scale factor indexes for each quant unit of a channel.
	 *
	 * @param[in]     chNum        channel to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeChannelSfIdx(int chNum) {
		Channel chan = ctx.channels[chNum];
		Channel refChan = ctx.channels[0];
		int weightIdx = 0;

		chan.fillMode = 0;

		switch (br.read(2)) { // switch according to coding mode
			case 0: // coded using constant number of bits
				for (int i = 0; i < ctx.usedQuantUnits; i++) {
					chan.quSfIdx[i] = br.read(6);
				}
				break;
			case 1:
				if (chNum > 0) {
					VLC vlcTab = sf_vlc_tabs[br.read(2)];

					for (int i = 0; i < ctx.usedQuantUnits; i++) {
						int delta = vlcTab.getVLC2(br);
						chan.quSfIdx[i] = (refChan.quSfIdx[i] + delta) & 0x3F;
					}
				} else {
					weightIdx = br.read(2);
					if (weightIdx == 3) {
						unpackSfVqShape(chan.quSfIdx, ctx.usedQuantUnits);

						int numLongVals = br.read(5);
						int deltaBits = br.read(2);
						int minVal = br.read(4) - 7;

						for (int i = 0; i < numLongVals; i++) {
							chan.quSfIdx[i] = (chan.quSfIdx[i] + br.read(4) - 7) & 0x3F;
						}

						// All others are: minVal + delta
						for (int i = numLongVals; i < ctx.usedQuantUnits; i++) {
							chan.quSfIdx[i] = (chan.quSfIdx[i] + minVal + getDelta(deltaBits)) & 0x3F;
						}
					} else {
						int numLongVals = br.read(5);
						int deltaBits = br.read(3);
						int minVal = br.read(6);
						if (numLongVals > ctx.usedQuantUnits || deltaBits == 7) {
							log.error(String.format("SF mode 1: invalid parameters"));
							return AT3P_ERROR; 
						}

						// Read full-precision SF indexes
						for (int i = 0; i < numLongVals; i++) {
							chan.quSfIdx[i] = br.read(6);
						}

						// All others are: minVal + delta
						for (int i = numLongVals; i < ctx.usedQuantUnits; i++) {
							chan.quSfIdx[i] = (minVal + getDelta(deltaBits)) & 0x3F;
						}
					}
				}
				break;
			case 2:
				if (chNum > 0) {
					VLC vlcTab = sf_vlc_tabs[br.read(2)];

					int delta = vlcTab.getVLC2(br);
					chan.quSfIdx[0] = (refChan.quSfIdx[0] + delta) & 0x3F;

					for (int i = 1; i < ctx.usedQuantUnits; i++) {
						int diff = refChan.quSfIdx[i] - refChan.quSfIdx[i - 1];
						delta = vlcTab.getVLC2(br);
						chan.quSfIdx[i] = (chan.quSfIdx[i - 1] + diff + delta) & 0x3F;
					}
				} else if (chan.numCodedVals > 0) {
					VLC vlcTab = sf_vlc_tabs[br.read(2) + 4];

					unpackSfVqShape(chan.quSfIdx, ctx.usedQuantUnits);

					for (int i = 0; i < ctx.usedQuantUnits; i++) {
						int delta = vlcTab.getVLC2(br);
						chan.quSfIdx[i] = (chan.quSfIdx[i] + signExtend(delta, 4)) & 0x3F;
					}
				}
				break;
			case 3:
				if (chNum > 0) {
					// Copy coefficients from reference channel
					for (int i = 0; i < ctx.usedQuantUnits; i++) {
						chan.quSfIdx[i] = refChan.quSfIdx[i];
					}
				} else {
					weightIdx = br.read(2);
					int vlcSel = br.read(2);
					VLC vlcTab = sf_vlc_tabs[vlcSel];

					if (weightIdx == 3) {
						vlcTab = sf_vlc_tabs[vlcSel + 4];

						unpackSfVqShape(chan.quSfIdx, ctx.usedQuantUnits);

						int diff = (br.read(4) + 56) & 0x3F;
						chan.quSfIdx[0] = (chan.quSfIdx[0] + diff) & 0x3F;

						for (int i = 1; i < ctx.usedQuantUnits; i++) {
							int delta = vlcTab.getVLC2(br);
							diff = (diff + signExtend(delta, 4)) & 0x3F;
							chan.quSfIdx[i] = (diff + chan.quSfIdx[i]) & 0x3F;
						}
					} else {
						// 1st coefficient is coded directly
						chan.quSfIdx[0] = br.read(6);

						for (int i = 1; i < ctx.usedQuantUnits; i++) {
							int delta = vlcTab.getVLC2(br);
							chan.quSfIdx[i] = (chan.quSfIdx[i - 1] + delta) & 0x3F;
						}
					}
				}
				break;
		}

		if (weightIdx != 0 && weightIdx < 3) {
			return substractSfWeights(chan, weightIdx);
		}

		return 0;
	}

	/**
	 * Decode word length information for each channel.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeQuantWordlen() {
		for (int chNum = 0; chNum < numChannels; chNum++) {
			Arrays.fill(ctx.channels[chNum].quWordlen, 0);

			int ret = decodeChannelWordlen(chNum);
			if (ret < 0) {
				return ret;
			}
		}

		/* scan for last non-zero coeff in both channels and
	     * set number of quant units having coded spectrum */
		int i;
		for (i = ctx.numQuantUnits - 1; i >= 0; i--) {
			if (ctx.channels[0].quWordlen[i] != 0 || (numChannels == 2 && ctx.channels[1].quWordlen[i] != 0)) {
				break;
			}
		}
		ctx.usedQuantUnits = i + 1;

		return 0;
	}

	private int decodeScaleFactors() {
		if (ctx.usedQuantUnits == 0) {
			return 0;
		}

		for (int chNum = 0; chNum < numChannels; chNum++) {
			Arrays.fill(ctx.channels[chNum].quSfIdx, 0);

			int ret = decodeChannelSfIdx(chNum);
			if (ret < 0) {
				return ret;
			}
		}

		return 0;
	}

	/**
	 * Decode number of code table values.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int getNumCtValues() {
		if (!br.readBool()) {
			return ctx.usedQuantUnits;
		}

		int numCodedVals = br.read(5);
		if (numCodedVals > ctx.usedQuantUnits) {
			log.error(String.format("Invalid number of code table indexes: %d", numCodedVals));
			return AT3P_ERROR;
		}
		return numCodedVals;
	}

	/**
	 * Decode code table indexes for each quant unit of a channel.
	 *
	 * @param[in]     chNum        channel to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeChannelCodeTab(int chNum) {
		VLC vlcTab;
		int numVals;
		int mask = ctx.useFullTable ? 7 : 3; // mask for modular arithmetic
		Channel chan = ctx.channels[chNum];
		Channel refChan = ctx.channels[0];

		chan.tableType = br.read(1);

		switch (br.read(2)) { // switch according to coding mode
			case 0: // directly coded
				int numBits = ctx.useFullTable ? 3 : 2;
				numVals = getNumCtValues();
				if (numVals < 0) {
					return numVals;
				}
				for (int i = 0; i < numVals; i++) {
					if (chan.quWordlen[i] != 0) {
						chan.quTabIdx[i] = br.read(numBits);
					} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
						// get clone master flag
						chan.quTabIdx[i] = br.read1();
					}
				}
				break;
			case 1: // entropy-coded
				vlcTab = ctx.useFullTable ? ct_vlc_tabs[1] : ct_vlc_tabs[0];
				numVals = getNumCtValues();
				if (numVals < 0) {
					return numVals;
				}
				for (int i = 0; i < numVals; i++) {
					if (chan.quWordlen[i] != 0) {
						chan.quTabIdx[i] = vlcTab.getVLC2(br);
					} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
						// get clone master flag
						chan.quTabIdx[i] = br.read1();
					}
				}
				break;
			case 2: // entropy-coded delta
				VLC deltaVlc;
				if (ctx.useFullTable) {
					vlcTab = ct_vlc_tabs[1];
					deltaVlc = ct_vlc_tabs[2];
				} else {
					vlcTab = ct_vlc_tabs[0];
					deltaVlc = ct_vlc_tabs[0];
				}
				int pred = 0;
				numVals = getNumCtValues();
				if (numVals < 0) {
					return numVals;
				}
				for (int i = 0; i < numVals; i++) {
					if (chan.quWordlen[i] != 0) {
						chan.quTabIdx[i] = (i == 0 ? vlcTab.getVLC2(br) : (pred + deltaVlc.getVLC2(br)) & mask);
						pred = chan.quTabIdx[i];
					} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
						// get clone master flag
						chan.quTabIdx[i] = br.read1();
					}
				}
				break;
			case 3: // entropy-coded difference to master
				if (chNum > 0) {
					vlcTab = ctx.useFullTable ? ct_vlc_tabs[3] : ct_vlc_tabs[0];
					numVals = getNumCtValues();
					if (numVals < 0) {
						return numVals;
					}
					for (int i = 0; i < numVals; i++) {
						if (chan.quWordlen[i] != 0) {
							chan.quTabIdx[i] = (refChan.quTabIdx[i] + vlcTab.getVLC2(br)) & mask;
						} else if (chNum > 0 && refChan.quWordlen[i] != 0) {
							// get clone master flag
							chan.quTabIdx[i] = br.read1();
						}
					}
				}
				break;
		}

		return 0;
	}

	/**
	 * Decode code table indexes for each channel.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeCodeTableIndexes() {
		if (ctx.usedQuantUnits == 0) {
			return 0;
		}

		ctx.useFullTable = br.readBool();

		for (int chNum = 0; chNum < numChannels; chNum++) {
			Arrays.fill(ctx.channels[chNum].quTabIdx, 0);

			int ret = decodeChannelCodeTab(chNum);
			if (ret < 0) {
				return ret;
			}
		}

		return 0;
	}

	private void decodeQuSpectra(Atrac3pSpecCodeTab tab, VLC vlcTab, int[] out, int outOffset, int numSpecs) {
		int groupSize = tab.groupSize;
		int numCoeffs = tab.numCoeffs;
		int bits = tab.bits;
		boolean isSigned = tab.isSigned;
		int mask = (1 << bits) - 1;

		for (int pos = 0; pos < numSpecs; ) {
			if (groupSize == 1 || br.readBool()) {
				for (int j = 0; j < groupSize; j++) {
					int val = vlcTab.getVLC2(br);

					for (int i = 0; i < numCoeffs; i++) {
						int cf = val & mask;
						if (isSigned) {
							cf = signExtend(cf, bits);
						} else if (cf != 0 && br.readBool()) {
							cf = -cf;
						}

						out[outOffset + pos] = cf;
						pos++;
						val >>= bits;
					}
				}
			} else {
				// Group skipped
				pos += groupSize * numCoeffs;
			}
		}
	}

	private void decodeSpectrum() {
		for (int chNum = 0; chNum < numChannels; chNum++) {
			Channel chan = ctx.channels[chNum];

			Arrays.fill(chan.spectrum, 0);

			Arrays.fill(chan.powerLevs, ATRAC3P_POWER_COMP_OFF);

			for (int qu = 0; qu < ctx.usedQuantUnits; qu++) {
				int numSpecs = ff_atrac3p_qu_to_spec_pos[qu + 1] - ff_atrac3p_qu_to_spec_pos[qu];
				int wordlen = chan.quWordlen[qu];
				int codetab = chan.quTabIdx[qu];
				if (wordlen > 0) {
					if (!ctx.useFullTable) {
						codetab = atrac3p_ct_restricted_to_full[chan.tableType][wordlen - 1][codetab];
					}

					int tabIndex = (chan.tableType * 8 + codetab) * 7 + wordlen - 1;
					Atrac3pSpecCodeTab tab = atrac3p_spectra_tabs[tabIndex];

					if (tab.redirect >= 0) {
						tabIndex = tab.redirect;
					}

					decodeQuSpectra(tab, spec_vlc_tabs[tabIndex], chan.spectrum, ff_atrac3p_qu_to_spec_pos[qu], numSpecs);
				} else if (chNum > 0 && ctx.channels[0].quWordlen[qu] != 0 && codetab == 0) {
					// Copy coefficients from master
					System.arraycopy(ctx.channels[0].spectrum, ff_atrac3p_qu_to_spec_pos[qu], chan.spectrum, ff_atrac3p_qu_to_spec_pos[qu], numSpecs);
					chan.quWordlen[qu] = ctx.channels[0].quWordlen[qu];
				}
			}

	        /* Power compensation levels only present in the bitstream
	         * if there are more than 2 quant units. The lowest two units
	         * correspond to the frequencies 0...351 Hz, whose shouldn't
	         * be affected by the power compensation. */
			if (ctx.usedQuantUnits > 2) {
				int numSpecs = atrac3p_subband_to_num_powgrps[ctx.numCodedSubbands - 1];
				for (int i = 0; i < numSpecs; i++) {
					chan.powerLevs[i] = br.read(4);
				}
			}
		}
	}

	private boolean getSubbandFlags(boolean[] out, int numFlags) {
		boolean result = br.readBool();
		if (result) {
			if (br.readBool()) {
				for (int i = 0; i < numFlags; i++) {
					out[i] = br.readBool();
				}
			} else {
				for (int i = 0; i < numFlags; i++) {
					out[i] = true;
				}
			}
		} else {
			for (int i = 0; i < numFlags; i++) {
				out[i] = false;
			}
		}

		return result;
	}

	/**
	 * Decode mdct window shape flags for all channels.
	 *
	 */
	private void decodeWindowShape() {
		for (int i = 0; i < numChannels; i++) {
			getSubbandFlags(ctx.channels[i].wndShape, ctx.numSubbands);
		}
	}

	private int decodeGaincNPoints(int chNum, int codedSubbands) {
		Channel chan = ctx.channels[chNum];
		Channel refChan = ctx.channels[0];

		switch (br.read(2)) { // switch according to coding mode
			case 0: // fixed-length coding
				for (int i = 0; i < codedSubbands; i++) {
					chan.gainData[i].numPoints = br.read(3);
				}
				break;
			case 1: // variable-length coding
				for (int i = 0; i < codedSubbands; i++) {
					chan.gainData[i].numPoints = gain_vlc_tabs[0].getVLC2(br);
				}
				break;
			case 2:
				if (chNum > 0) { // VLC modulo delta to master channel
					for (int i = 0; i < codedSubbands; i++) {
						int delta = gain_vlc_tabs[1].getVLC2(br);
						chan.gainData[i].numPoints = (refChan.gainData[i].numPoints + delta) & 7;
					}
				} else { // VLC modulo delta to previous
					chan.gainData[0].numPoints = gain_vlc_tabs[0].getVLC2(br);

					for (int i = 1; i < codedSubbands; i++) {
						int delta = gain_vlc_tabs[1].getVLC2(br);
						chan.gainData[i].numPoints = (chan.gainData[i - 1].numPoints + delta) & 7;
					}
				}
				break;
			case 3:
				if (chNum > 0) { // copy data from master channel
					for (int i = 0; i < codedSubbands; i++) {
						chan.gainData[i].numPoints = refChan.gainData[i].numPoints;
					}
				} else { // shorter delta to min
					int deltaBits = br.read(2);
					int minVal = br.read(3);

					for (int i = 0; i < codedSubbands; i++) {
						chan.gainData[i].numPoints = minVal + getDelta(deltaBits);
						if (chan.gainData[i].numPoints > 7) {
							return AT3P_ERROR;
						}
					}
				}
				break;
		}

		return 0;
	}

	/**
	 * Implements coding mode 1 (master) for gain compensation levels.
	 *
	 * @param[out]    dst    ptr to the output array
	 */
	private void gaincLevelMode1m(AtracGainInfo dst) {
		if (dst.numPoints > 0) {
			dst.levCode[0] = gain_vlc_tabs[2].getVLC2(br);
		}

		for (int i = 1; i < dst.numPoints; i++) {
			int delta = gain_vlc_tabs[3].getVLC2(br);
			dst.levCode[i] = (dst.levCode[i - 1] + delta) & 0xF;
		}
	}

	/**
	 * Implements coding mode 3 (slave) for gain compensation levels.
	 *
	 * @param[out]   dst   ptr to the output array
	 * @param[in]    ref   ptr to the reference channel
	 */
	private void gaincLevelMode3s(AtracGainInfo dst, AtracGainInfo ref) {
		for (int i = 0; i < dst.numPoints; i++) {
			dst.levCode[i] = (i >= ref.numPoints ? 7 : ref.levCode[i]);
		}
	}

	/**
	 * Decode level code for each gain control point.
	 *
	 * @param[in]     ch_num          channel to process
	 * @param[in]     coded_subbands  number of subbands to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeGaincLevels(int chNum, int codedSubbands) {
		Channel chan = ctx.channels[chNum];
		Channel refChan = ctx.channels[0];

		switch (br.read(2)) { // switch according to coding mode
			case 0: // fixed-length coding
				for (int sb = 0; sb < codedSubbands; sb++) {
					for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
						chan.gainData[sb].levCode[i] = br.read(4);
					}
				}
				break;
			case 1:
				if (chNum > 0) { // VLC module delta to master channel
					for (int sb = 0; sb < codedSubbands; sb++) {
						for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
							int delta = gain_vlc_tabs[5].getVLC2(br);
							int pred = (i >= refChan.gainData[sb].numPoints ? 7 : refChan.gainData[sb].levCode[i]);
							chan.gainData[sb].levCode[i] = (pred + delta) & 0xF;
						}
					}
				} else { // VLC module delta to previous
					for (int sb = 0; sb < codedSubbands; sb++) {
						gaincLevelMode1m(chan.gainData[sb]);
					}
				}
				break;
			case 2:
				if (chNum > 0) { // VLC modulo delta to previous or clone master
					for (int sb = 0; sb < codedSubbands; sb++) {
						if (chan.gainData[sb].numPoints > 0) {
							if (br.readBool()) {
								gaincLevelMode1m(chan.gainData[sb]);
							} else {
								gaincLevelMode3s(chan.gainData[sb], refChan.gainData[sb]);
							}
						}
					}
				} else { // VLC modulo delta to lev_codes of previous subband
					if (chan.gainData[0].numPoints > 0) {
						gaincLevelMode1m(chan.gainData[0]);;
					}

					for (int sb = 1; sb < codedSubbands; sb++) {
						for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
							int delta = gain_vlc_tabs[4].getVLC2(br);
							int pred = (i >= chan.gainData[sb - 1].numPoints ? 7 : chan.gainData[sb - 1].levCode[i]);
							chan.gainData[sb].levCode[i] = (pred + delta) & 0xF;
						}
					}
				}
				break;
			case 3:
				if (chNum > 0) { // clone master
					for (int sb = 0; sb < codedSubbands; sb++) {
						gaincLevelMode3s(chan.gainData[sb], refChan.gainData[sb]);
					}
				} else { // shorter delta to min
					int deltaBits = br.read(2);
					int minVal = br.read(4);

					for (int sb = 0; sb < codedSubbands; sb++) {
						for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
							chan.gainData[sb].levCode[i] = minVal + getDelta(deltaBits);
							if (chan.gainData[sb].levCode[i] > 15) {
								return AT3P_ERROR;
							}
						}
					}
				}
				break;
		}

		return 0;
	}

	/**
	 * Implements coding mode 0 for gain compensation locations.
	 *
	 * @param[out]    dst    ptr to the output array
	 * @param[in]     pos    position of the value to be processed
	 */
	private void gaincLocMode0(AtracGainInfo dst, int pos) {
		if (pos == 0 || dst.locCode[pos - 1] < 15) {
			dst.locCode[pos] = br.read(5);
		} else if (dst.locCode[pos - 1] >= 30) {
			dst.locCode[pos] = 31;
		} else {
			int deltaBits = avLog2(30 - dst.locCode[pos - 1]) + 1;
			dst.locCode[pos] = dst.locCode[pos - 1] + br.read(deltaBits) + 1;
		}
	}

	/**
	 * Implements coding mode 1 for gain compensation locations.
	 *
	 * @param[out]    dst    ptr to the output array
	 */
	private void gaincLocMode1(AtracGainInfo dst) {
		if (dst.numPoints > 0) {
			// 1st coefficient is stored directly
			dst.locCode[0] = br.read(5);

			for (int i = 1; i < dst.numPoints; i++) {
				// Switch VLC according to the curve direction
				// (ascending/descending)
				VLC tab = (dst.levCode[i] <= dst.levCode[i - 1] ? gain_vlc_tabs[7] : gain_vlc_tabs[9]);
				dst.locCode[i] = dst.locCode[i - 1] + tab.getVLC2(br);
			}
		}
	}

	/**
	 * Decode location code for each gain control point.
	 *
	 * @param[in]     chNum          channel to process
	 * @param[in]     codedSubbands  number of subbands to process
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeGaincLocCodes(int chNum, int codedSubbands) {
		Channel chan = ctx.channels[chNum];
		Channel refChan = ctx.channels[0];

		int codingMode = br.read(2);
		switch (codingMode) { // switch according to coding mode
			case 0: // sequence of numbers in ascending order
				for (int sb = 0; sb < codedSubbands; sb++) {
					for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
						gaincLocMode0(chan.gainData[sb], i);
					}
				}
				break;
			case 1:
				if (chNum > 0) {
					for (int sb = 0; sb < codedSubbands; sb++) {
						if (chan.gainData[sb].numPoints <= 0) {
							continue;
						}
						AtracGainInfo dst = chan.gainData[sb];
						AtracGainInfo ref = refChan.gainData[sb];

						// 1st value is vlc-coded modulo delta to master
						int delta = gain_vlc_tabs[10].getVLC2(br);
						int pred = ref.numPoints > 0 ? ref.locCode[0] : 0;
						dst.locCode[0] = (pred + delta) & 0x1F;

						for (int i = 1; i < dst.numPoints; i++) {
							boolean moreThanRef = i >= ref.numPoints;
							if (dst.levCode[i] > dst.levCode[i - 1]) {
								// ascending curve
								if (moreThanRef) {
									delta = gain_vlc_tabs[9].getVLC2(br);
									dst.locCode[i] = dst.locCode[i - 1] + delta;
								} else {
									if (br.readBool()) {
										gaincLocMode0(dst, i); // direct coding
									} else {
										dst.locCode[i] = ref.locCode[i]; // clone master
									}
								}
							} else { // descending curve
								VLC tab = moreThanRef ? gain_vlc_tabs[7] : gain_vlc_tabs[10];
								delta = tab.getVLC2(br);
								if (moreThanRef) {
									dst.locCode[i] = dst.locCode[i - 1] + delta;
								} else {
									dst.locCode[i] = (ref.locCode[i] + delta) & 0x1F;
								}
							}
						}
					}
				} else { // VLC delta to previous
					for (int sb = 0; sb < codedSubbands; sb++) {
						gaincLocMode1(chan.gainData[sb]);
					}
				}
				break;
			case 2:
				if (chNum > 0) {
					for (int sb = 0; sb < codedSubbands; sb++) {
						if (chan.gainData[sb].numPoints <= 0) {
							continue;
						}
						AtracGainInfo dst = chan.gainData[sb];
						AtracGainInfo ref = refChan.gainData[sb];
						if (dst.numPoints > ref.numPoints || br.readBool()) {
							gaincLocMode1(dst);
						} else { // clone master for the whole subband
							for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
								dst.locCode[i] = ref.locCode[i];
							}
						}
					}
				} else {
					// data for the first subband is coded directly
					for (int i = 0; i < chan.gainData[0].numPoints; i++) {
						gaincLocMode0(chan.gainData[0], i);
					}

					for (int sb = 1; sb < codedSubbands; sb++) {
						if (chan.gainData[sb].numPoints <= 0) {
							continue;
						}
						AtracGainInfo dst = chan.gainData[sb];

						// 1st value is vlc-coded modulo delta to the corresponding
						// value of the previous subband if any or zero
						int delta = gain_vlc_tabs[6].getVLC2(br);
						int pred = chan.gainData[sb - 1].numPoints > 0 ? chan.gainData[sb - 1].locCode[0] : 0;
						dst.locCode[0] = (pred + delta) & 0x1F;

						for (int i = 1; i < dst.numPoints; i++) {
							boolean moreThanRef = i >= chan.gainData[sb - 1].numPoints;
							// Select VLC table according to curve direction and
							// presence of prediction
							VLC tab = gain_vlc_tabs[(dst.levCode[i] > dst.levCode[i - 1] ? 2 : 0) + (moreThanRef ? 1 : 0) + 6];
							delta = tab.getVLC2(br);
							if (moreThanRef) {
								dst.locCode[i] = dst.locCode[i - 1] + delta;
							} else {
								dst.locCode[i] = (chan.gainData[sb - 1].locCode[i] + delta) & 0x1F;
							}
						}
					}
				}
				break;
			case 3:
				if (chNum > 0) { // clone master or direct or direct coding
					for (int sb = 0; sb < codedSubbands; sb++) {
						for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
							if (i >= refChan.gainData[sb].numPoints) {
								gaincLocMode0(chan.gainData[sb], i);
							} else {
								chan.gainData[sb].locCode[i] = refChan.gainData[sb].locCode[i];
							}
						}
					}
				} else { // shorter delta to min
					int deltaBits = br.read(2) + 1;
					int minVal = br.read(5);

					for (int sb = 0; sb < codedSubbands; sb++) {
						for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
							chan.gainData[sb].locCode[i] = minVal + i + br.read(deltaBits);
						}
					}
				}
				break;
		}

		// Validate decoded information
		for (int sb = 0; sb < codedSubbands; sb++) {
			AtracGainInfo dst = chan.gainData[sb];
			for (int i = 0; i < chan.gainData[sb].numPoints; i++) {
				if (dst.locCode[i] < 0 || dst.locCode[i] > 31 || (i > 0 && dst.locCode[i] <= dst.locCode[i - 1])) {
					log.error(String.format("Invalid gain location: ch=%d, sb=%d, pos=%d, val=%d", chNum, sb, i, dst.locCode[i]));
					return AT3P_ERROR;
				}
			}
		}

		return 0;
	}

	/**
	 * Decode gain control data for all channels.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeGaincData() {
		int ret;

		for (int chNum = 0; chNum < numChannels; chNum++) {
			for (int i = 0; i < ATRAC3P_SUBBANDS; i++) {
				ctx.channels[chNum].gainData[i].clear();
			}

			if (br.readBool()) { // gain control data present?
				int codedSubbands = br.read(4) + 1;
				if (br.readBool()) { // is high band gain data replication on?
					ctx.channels[chNum].numGainSubbands = br.read(4) + 1;
				} else {
					ctx.channels[chNum].numGainSubbands = codedSubbands;
				}

				ret = decodeGaincNPoints(chNum, codedSubbands);
				if (ret < 0) {
					return ret;
				}
				ret = decodeGaincLevels(chNum, codedSubbands);
				if (ret < 0) {
					return ret;
				}
				ret = decodeGaincLocCodes(chNum, codedSubbands);
				if (ret < 0) {
					return ret;
				}

				if (codedSubbands > 0) { // propagate gain data if requested
					for (int sb = codedSubbands; sb < ctx.channels[chNum].numGainSubbands; sb++) {
						ctx.channels[chNum].gainData[sb].copy(ctx.channels[chNum].gainData[sb - 1]);
					}
				}
			} else {
				ctx.channels[chNum].numGainSubbands = 0;
			}
		}

		return 0;
	}

	/**
	 * Decode envelope for all tones of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 *                                1 - tone data present
	 */
	private void decodeTonesEnvelope(int chNum, boolean bandHasTones[]) {
		WavesData dst[] = ctx.channels[chNum].tonesInfo;
		WavesData ref[] = ctx.channels[0].tonesInfo;

		if (chNum == 0 || !br.readBool()) { // mode 0: fixed-length coding
			for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
				if (!bandHasTones[sb]) {
					continue;
				}
				dst[sb].pendEnv.hasStartPoint = br.readBool();
				dst[sb].pendEnv.startPos = (dst[sb].pendEnv.hasStartPoint ? br.read(5) : -1);
				dst[sb].pendEnv.hasStopPoint = br.readBool();
				dst[sb].pendEnv.stopPos = (dst[sb].pendEnv.hasStopPoint ? br.read(5) : 32);
			}
		} else { // mode 1(slave only): copy master
			for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
				if (!bandHasTones[sb]) {
					continue;
				}
				dst[sb].pendEnv.copy(ref[sb].pendEnv);
			}
		}
	}

	/**
	 * Decode number of tones for each subband of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 *                                1 - tone data present
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeBandNumwavs(int chNum, boolean bandHasTones[]) {
		WavesData dst[] = ctx.channels[chNum].tonesInfo;
		WavesData ref[] = ctx.channels[0].tonesInfo;

		int mode = br.read(chNum + 1);
		switch (mode) {
			case 0: // fixed-length coding
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (bandHasTones[sb]) {
						dst[sb].numWavs = br.read(4);
					}
				}
				break;
			case 1: // variable-length coding
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (bandHasTones[sb]) {
						dst[sb].numWavs = tone_vlc_tabs[1].getVLC2(br);
					}
				}
				break;
			case 2: // VLC modulo delta to master (slave only)
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (bandHasTones[sb]) {
						int delta = tone_vlc_tabs[2].getVLC2(br);
						delta = signExtend(delta, 3);
						dst[sb].numWavs = (ref[sb].numWavs + delta) & 0xF;
					}
				}
				break;
			case 3: // copy master (slave only)
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (bandHasTones[sb]) {
						dst[sb].numWavs = ref[sb].numWavs;
					}
				}
				break;
		}

		// initialize start tone index for each subband
		for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
			if (bandHasTones[sb]) {
				if (ctx.wavesInfo.tonesIndex + dst[sb].numWavs > 48) {
					log.error(String.format("Too many tones: %d (max. 48)", ctx.wavesInfo.tonesIndex + dst[sb].numWavs));
					return AT3P_ERROR;
				}
				dst[sb].startIndex = ctx.wavesInfo.tonesIndex;
				ctx.wavesInfo.tonesIndex += dst[sb].numWavs;
			}
		}

		return 0;
	}

	/**
	 * Decode frequency information for each subband of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 *                                1 - tone data present
	 */
	private void decodeTonesFrequency(int chNum, boolean bandHasTones[]) {
		WavesData dst[] = ctx.channels[chNum].tonesInfo;
		WavesData ref[] = ctx.channels[0].tonesInfo;

		if (chNum == 0 || !br.readBool()) { // mode 0: fixed-length coding
			for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue;
				}
				int iwav = dst[sb].startIndex;
				boolean direction = (dst[sb].numWavs > 1 ? br.readBool() : false);
				if (direction) { // packed numbers in descending order
					if (dst[sb].numWavs > 0) {
						ctx.wavesInfo.waves[iwav + dst[sb].numWavs - 1].freqIndex = br.read(10);
					}
					for (int i = dst[sb].numWavs - 2; i >= 0; i--) {
						int nbits = avLog2(ctx.wavesInfo.waves[iwav + i + 1].freqIndex) + 1;
						ctx.wavesInfo.waves[iwav + i].freqIndex = br.read(nbits);
					}
				} else { // packed numbers in ascending order
					for (int i = 0; i < dst[sb].numWavs; i++) {
						if (i == 0 || ctx.wavesInfo.waves[iwav + i - 1].freqIndex < 512) {
							ctx.wavesInfo.waves[iwav + i].freqIndex = br.read(10);
						} else {
							int nbits = avLog2(1023 - ctx.wavesInfo.waves[iwav + i - 1].freqIndex) + 1;
							ctx.wavesInfo.waves[iwav + i].freqIndex = br.read(nbits) + 1024 - (1 << nbits);
						}
					}
				}
			}
		} else { // mode 1: VLC module delta to master (slave only)
			for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue;
				}
				int iwav = ref[sb].startIndex;
				int owav = dst[sb].startIndex;
				for (int i = 0; i < dst[sb].numWavs; i++) {
					int delta = tone_vlc_tabs[6].getVLC2(br);
					delta = signExtend(delta, 8);
					int pred = (i < ref[sb].numWavs ? ctx.wavesInfo.waves[iwav + i].freqIndex : (ref[sb].numWavs > 0 ? ctx.wavesInfo.waves[iwav + ref[sb].numWavs - 1].freqIndex : 0));
					ctx.wavesInfo.waves[owav + i].freqIndex = (pred + delta) & 0x3FF;
				}
			}
		}
	}

	/**
	 * Decode amplitude information for each subband of a channel.
	 *
	 * @param[in]     chNum           channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 *                                1 - tone data present
	 */
	private void decodeTonesAmplitude(int chNum, boolean bandHasTones[]) {
		WavesData dst[] = ctx.channels[chNum].tonesInfo;
		WavesData ref[] = ctx.channels[0].tonesInfo;
		final int refwaves[] = new int[48];

		if (chNum > 0) {
			for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
				if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
					continue;
				}
				int wsrc = dst[sb].startIndex;
				int wref = ref[sb].startIndex;
				for (int j = 0; j < dst[sb].numWavs; j++) {
					int fi = 0;
					int maxdiff = 1024;
					for (int i = 0; i < ref[sb].numWavs; i++) {
						int diff = abs(ctx.wavesInfo.waves[wsrc + j].freqIndex - ctx.wavesInfo.waves[wref + i].freqIndex);
						if (diff < maxdiff) {
							maxdiff = diff;
							fi = i;
						}
					}

					if (maxdiff < 8) {
						refwaves[dst[sb].startIndex + j] = fi + ref[sb].startIndex;
					} else if (j < ref[sb].numWavs) {
						refwaves[dst[sb].startIndex + j] = j + ref[sb].startIndex;
					} else {
						refwaves[dst[sb].startIndex + j] = -1;
					}
				}
			}
		}

		int mode = br.read(chNum + 1);

		switch (mode) {
			case 0: // fixed-length coding
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
						continue;
					}
					if (ctx.wavesInfo.amplitudeMode != 0) {
						for (int i = 0; i < dst[sb].numWavs; i++) {
							ctx.wavesInfo.waves[dst[sb].startIndex + i].ampSf = br.read(6);
						}
					} else {
						ctx.wavesInfo.waves[dst[sb].startIndex].ampSf = br.read(6);
					}
				}
				break;
			case 1: // min + VLC delta
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
						continue;
					}
					if (ctx.wavesInfo.amplitudeMode != 0) {
						for (int i = 0; i < dst[sb].numWavs; i++) {
							ctx.wavesInfo.waves[dst[sb].startIndex + i].ampSf = tone_vlc_tabs[3].getVLC2(br) + 20;
						}
					} else {
						ctx.wavesInfo.waves[dst[sb].startIndex].ampSf = tone_vlc_tabs[4].getVLC2(br) + 24;
					}
				}
				break;
			case 2: // VLC module delta to master (slave only)
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (!bandHasTones[sb] || dst[sb].numWavs == 0) {
						continue;
					}
					for (int i = 0; i < dst[sb].numWavs; i++) {
						int delta = tone_vlc_tabs[5].getVLC2(br);
						delta = signExtend(delta, 5);
						int pred = refwaves[dst[sb].startIndex + i] >= 0 ? ctx.wavesInfo.waves[refwaves[dst[sb].startIndex + i]].ampSf : 34;
						ctx.wavesInfo.waves[dst[sb].startIndex + i].ampSf = (pred + delta) & 0x3F;
					}
				}
				break;
			case 3: // clone master (slave only)
				for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
					if (!bandHasTones[sb]) {
						continue;
					}
					for (int i = 0; i < dst[sb].numWavs; i++) {
						ctx.wavesInfo.waves[dst[sb].startIndex + i].ampSf = refwaves[dst[sb].startIndex + i] >= 0 ? ctx.wavesInfo.waves[refwaves[dst[sb].startIndex + i]].ampSf : 32;
					}
				}
				break;
		}
	}

	/**
	 * Decode phase information for each subband of a channel.
	 *
	 * @param[in]     chNnum          channel to process
	 * @param[in]     bandHasTones    ptr to an array of per-band-flags:
	 *                                1 - tone data present
	 */
	private void decodeTonesPhase(int chNum, boolean bandHasTones[]) {
		WavesData dst[] = ctx.channels[chNum].tonesInfo;

		for (int sb = 0; sb < ctx.wavesInfo.numToneBands; sb++) {
			if (!bandHasTones[sb]) {
				continue;
			}
			int wparam = dst[sb].startIndex;
			for (int i = 0; i < dst[sb].numWavs; i++) {
				ctx.wavesInfo.waves[wparam + i].phaseIndex = br.read(5);
			}
		}
	}

	/**
	 * Decode tones info for all channels.
	 *
	 * @return result code: 0 = OK, otherwise - error code
	 */
	private int decodeTonesInfo() {
		for (int chNum = 0; chNum < numChannels; chNum++) {
			for (int i = 0; i < ATRAC3P_SUBBANDS; i++) {
				ctx.channels[chNum].tonesInfo[i].clear();;
			}
		}

		ctx.wavesInfo.tonesPresent = br.readBool();
		if (!ctx.wavesInfo.tonesPresent) {
			return 0;
		}

		for (int i = 0; i < ctx.wavesInfo.waves.length; i++) {
			ctx.wavesInfo.waves[i].clear();;
		}

		ctx.wavesInfo.amplitudeMode = br.read1();
		if (ctx.wavesInfo.amplitudeMode == 0) {
			log.error(String.format("GHA amplitude mode 0"));
			return AT3P_ERROR;
		}

		ctx.wavesInfo.numToneBands = tone_vlc_tabs[0].getVLC2(br) + 1;

		if (numChannels == 2) {
			getSubbandFlags(ctx.wavesInfo.toneSharing, ctx.wavesInfo.numToneBands);
			getSubbandFlags(ctx.wavesInfo.toneMaster, ctx.wavesInfo.numToneBands);
			if (getSubbandFlags(ctx.wavesInfo.phaseShift, ctx.wavesInfo.numToneBands)) {
				log.error(String.format("GHA Phase shifting"));
				return AT3P_ERROR;
			}
		}

		ctx.wavesInfo.tonesIndex = 0;

		for (int chNum = 0; chNum < numChannels; chNum++) {
			final boolean bandHasTones[] = new boolean[16];
			for (int i = 0; i < ctx.wavesInfo.numToneBands; i++) {
				bandHasTones[i] = (chNum == 0 ? true : !ctx.wavesInfo.toneSharing[i]);
			}

			decodeTonesEnvelope(chNum, bandHasTones);
			int ret = decodeBandNumwavs(chNum, bandHasTones);
			if (ret < 0) {
				return ret;
			}

			decodeTonesFrequency(chNum, bandHasTones);
			decodeTonesAmplitude(chNum, bandHasTones);
			decodeTonesPhase(chNum, bandHasTones);
		}

		if (numChannels == 2) {
			for (int i = 0; i < ctx.wavesInfo.numToneBands; i++) {
				if (ctx.wavesInfo.toneSharing[i]) {
					ctx.channels[1].tonesInfo[i].copy(ctx.channels[0].tonesInfo[i]);
				}

				if (ctx.wavesInfo.toneMaster[i]) {
					// Swap channels 0 and 1
					WavesData tmp = new WavesData();
					tmp.copy(ctx.channels[0].tonesInfo[i]);
					ctx.channels[0].tonesInfo[i].copy(ctx.channels[1].tonesInfo[i]);
					ctx.channels[1].tonesInfo[i].copy(tmp);
				}
			}
		}

		return 0;
	}

	public void decodeResidualSpectrum(float[][] out) {
		final int sbRNGindex[] = new int[ATRAC3P_SUBBANDS];

		if (ctx.muteFlag) {
			for (int ch = 0; ch < numChannels; ch++) {
				Arrays.fill(out[ch], 0f);
			}
			return;
		}

		int RNGindex = 0;
		for (int qu = 0; qu < ctx.usedQuantUnits; qu++) {
			RNGindex += ctx.channels[0].quSfIdx[qu] + ctx.channels[1].quSfIdx[qu];
		}

		for (int sb = 0; sb < ctx.numCodedSubbands; sb++, RNGindex += 128) {
			sbRNGindex[sb] = RNGindex & 0x3FC;
		}

		// inverse quant and power compensation
		for (int ch = 0; ch < numChannels; ch++) {
			// clear channel's residual spectrum
			Arrays.fill(out[ch], 0f);

			for (int qu = 0; qu < ctx.usedQuantUnits; qu++) {
				int src = ff_atrac3p_qu_to_spec_pos[qu];
				int dst = ff_atrac3p_qu_to_spec_pos[qu];
				int nspeclines = ff_atrac3p_qu_to_spec_pos[qu + 1] - ff_atrac3p_qu_to_spec_pos[qu];

				if (ctx.channels[ch].quWordlen[qu] > 0) {
					float q = ff_atrac3p_sf_tab[ctx.channels[ch].quSfIdx[qu]] * ff_atrac3p_mant_tab[ctx.channels[ch].quWordlen[qu]];
					for (int i = 0; i < nspeclines; i++) {
						out[ch][dst + i] = ctx.channels[ch].spectrum[src + i] * q;
					}
				}
			}

			for (int sb = 0; sb < ctx.numCodedSubbands; sb++) {
				dsp.powerCompensation(ctx, ch, out[ch], sbRNGindex[sb], sb);
			}
		}

		if (ctx.unitType == CH_UNIT_STEREO) {
			final float tmp[] = new float[ATRAC3P_SUBBAND_SAMPLES];
			for (int sb = 0; sb < ctx.numCodedSubbands; sb++) {
				if (ctx.swapChannels[sb]) {
					// Swap both channels
					System.arraycopy(out[0], sb * ATRAC3P_SUBBAND_SAMPLES, tmp   ,                            0, ATRAC3P_SUBBAND_SAMPLES);
					System.arraycopy(out[1], sb * ATRAC3P_SUBBAND_SAMPLES, out[0], sb * ATRAC3P_SUBBAND_SAMPLES, ATRAC3P_SUBBAND_SAMPLES);
					System.arraycopy(tmp   ,                            0, out[1], sb * ATRAC3P_SUBBAND_SAMPLES, ATRAC3P_SUBBAND_SAMPLES);
				}

				// flip coefficients' sign if requested
				if (ctx.negateCoeffs[sb]) {
					for (int i = 0; i < ATRAC3P_SUBBAND_SAMPLES; i++) {
						out[1][sb * ATRAC3P_SUBBAND_SAMPLES + i] = -(out[1][sb * ATRAC3P_SUBBAND_SAMPLES + i]);
					}
				}
			}
		}
	}

	public void reconstructFrame(Context at3pContext) {
		for (int ch = 0; ch < numChannels; ch++) {
			for (int sb = 0; sb < ctx.numSubbands; sb++) {
				// inverse transform and windowing
				dsp.imdct(at3pContext.mdctCtx, at3pContext.samples[ch], sb * ATRAC3P_SUBBAND_SAMPLES, at3pContext.mdctBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES, (ctx.channels[ch].wndShapePrev[sb] ? 2 : 0) + (ctx.channels[ch].wndShape[sb] ? 1 : 0), sb);

				// gain compensation and overlapping
				at3pContext.gaincCtx.gainCompensation(at3pContext.mdctBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES, ctx.prevBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES, ctx.channels[ch].gainDataPrev[sb], ctx.channels[ch].gainData[sb], ATRAC3P_SUBBAND_SAMPLES, at3pContext.timeBuf[ch], sb * ATRAC3P_SUBBAND_SAMPLES);
			}

			// zero unused subbands in both output and overlapping buffers
			Arrays.fill(        ctx.prevBuf[ch], ctx.numSubbands * ATRAC3P_SUBBAND_SAMPLES,         ctx.prevBuf[ch].length, 0f);
			Arrays.fill(at3pContext.timeBuf[ch], ctx.numSubbands * ATRAC3P_SUBBAND_SAMPLES, at3pContext.timeBuf[ch].length, 0f);

			// resynthesize and add tonal signal
			if (ctx.wavesInfo.tonesPresent || ctx.wavesInfoPrev.tonesPresent) {
				for (int sb = 0; sb < ctx.numSubbands; sb++) {
					if (ctx.channels[ch].tonesInfo[sb].numWavs > 0 || ctx.channels[ch].tonesInfoPrev[sb].numWavs > 0) {
						dsp.generateTones(ctx, ch, sb, at3pContext.timeBuf[ch], sb * 128);
					}
				}
			}

			// subband synthesis and acoustic signal output
			dsp.ipqf(at3pContext.ipqfDctCtx, ctx.ipqfCtx[ch], at3pContext.timeBuf[ch], at3pContext.outpBuf[ch]);
		}

		// swap window shape and gain control buffers
		for (int ch = 0; ch < numChannels; ch++) {
			boolean tmp1[] = ctx.channels[ch].wndShape;
			ctx.channels[ch].wndShape = ctx.channels[ch].wndShapePrev;
			ctx.channels[ch].wndShapePrev = tmp1;

			AtracGainInfo tmp2[] = ctx.channels[ch].gainData;
			ctx.channels[ch].gainData = ctx.channels[ch].gainDataPrev;
			ctx.channels[ch].gainDataPrev = tmp2;

			WavesData tmp3[] = ctx.channels[ch].tonesInfo;
			ctx.channels[ch].tonesInfo = ctx.channels[ch].tonesInfoPrev;
			ctx.channels[ch].tonesInfoPrev = tmp3;
		}

		WaveSynthParams tmp = ctx.wavesInfo;
		ctx.wavesInfo = ctx.wavesInfoPrev;
		ctx.wavesInfoPrev = tmp;
	}
}
