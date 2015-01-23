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

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static jpcsp.media.codec.aac.AacTab.POW_SF2_ZERO;
import static jpcsp.media.codec.aac.AacTab.cbrt_tab;
import static jpcsp.media.codec.aac.AacTab.ff_aac_codebook_vector_idx;
import static jpcsp.media.codec.aac.AacTab.ff_aac_codebook_vector_vals;
import static jpcsp.media.codec.aac.AacTab.ff_aac_kbd_long_1024;
import static jpcsp.media.codec.aac.AacTab.ff_aac_kbd_short_128;
import static jpcsp.media.codec.aac.AacTab.ff_aac_num_swb_1024;
import static jpcsp.media.codec.aac.AacTab.ff_aac_num_swb_128;
import static jpcsp.media.codec.aac.AacTab.ff_aac_num_swb_512;
import static jpcsp.media.codec.aac.AacTab.ff_aac_pow2sf_tab;
import static jpcsp.media.codec.aac.AacTab.ff_aac_pred_sfb_max;
import static jpcsp.media.codec.aac.AacTab.ff_aac_scalefactor_bits;
import static jpcsp.media.codec.aac.AacTab.ff_aac_scalefactor_code;
import static jpcsp.media.codec.aac.AacTab.ff_aac_spectral_bits;
import static jpcsp.media.codec.aac.AacTab.ff_aac_spectral_codes;
import static jpcsp.media.codec.aac.AacTab.ff_aac_spectral_sizes;
import static jpcsp.media.codec.aac.AacTab.ff_mpeg4audio_channels;
import static jpcsp.media.codec.aac.AacTab.ff_swb_offset_1024;
import static jpcsp.media.codec.aac.AacTab.ff_swb_offset_128;
import static jpcsp.media.codec.aac.AacTab.ff_swb_offset_512;
import static jpcsp.media.codec.aac.AacTab.ff_tns_max_bands_1024;
import static jpcsp.media.codec.aac.AacTab.ff_tns_max_bands_128;
import static jpcsp.media.codec.aac.AacTab.ff_tns_max_bands_512;
import static jpcsp.media.codec.aac.Lpc.computeLpcCoefs;
import static jpcsp.media.codec.aac.OutputConfiguration.OC_GLOBAL_HDR;
import static jpcsp.media.codec.aac.OutputConfiguration.OC_LOCKED;
import static jpcsp.media.codec.aac.OutputConfiguration.OC_NONE;
import static jpcsp.media.codec.aac.OutputConfiguration.OC_TRIAL_FRAME;
import static jpcsp.media.codec.aac.OutputConfiguration.OC_TRIAL_PCE;
import static jpcsp.media.codec.util.CodecUtils.avLog2;
import static jpcsp.media.codec.util.SineWin.ff_sine_1024;
import static jpcsp.media.codec.util.SineWin.ff_sine_128;
import static jpcsp.media.codec.util.SineWin.ff_sine_512;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.CodecUtils;
import jpcsp.media.codec.util.FFT;
import jpcsp.media.codec.util.FloatDSP;
import jpcsp.media.codec.util.SineWin;
import jpcsp.media.codec.util.VLC;
import jpcsp.util.Utilities;

public class AacDecoder implements ICodec {
	public static Logger log = Logger.getLogger("aac");
	public static final int AAC_ERROR = -4;
	public static final int MAX_CHANNELS = 64;
	public static final int MAX_ELEM_ID = 16;
	public static final int TNS_MAX_ORDER = 20;
	public static final int MAX_LTP_LONG_SFB = 40;
	public static final int MAX_PREDICTORS = 672;
	// Raw Data Block Types:
	public static final int TYPE_SCE = 0;
	public static final int TYPE_CPE = 1;
	public static final int TYPE_CCE = 2;
	public static final int TYPE_LFE = 3;
	public static final int TYPE_DSE = 4;
	public static final int TYPE_PCE = 5;
	public static final int TYPE_FIL = 6;
	public static final int TYPE_END = 7;
	// Channel layouts
	public static final int CH_FRONT_LEFT               = 0x001;
	public static final int CH_FRONT_RIGHT              = 0x002;
	public static final int CH_FRONT_CENTER             = 0x004;
	public static final int CH_LOW_FREQUENCY            = 0x008;
	public static final int CH_BACK_LEFT                = 0x010;
	public static final int CH_BACK_RIGHT               = 0x020;
	public static final int CH_FRONT_LEFT_OF_CENTER     = 0x040;
	public static final int CH_FRONT_RIGHT_OF_CENTER    = 0x080;
	public static final int CH_BACK_CENTER              = 0x100;
	public static final int CH_SIDE_LEFT                = 0x200;
	public static final int CH_SIDE_RIGHT               = 0x400;
	public static final int CH_LAYOUT_MONO              = CH_FRONT_CENTER;
	public static final int CH_LAYOUT_STEREO            = CH_FRONT_LEFT          | CH_FRONT_RIGHT;
	public static final int CH_LAYOUT_SURROUND          = CH_LAYOUT_STEREO       | CH_FRONT_CENTER;
	public static final int CH_LAYOUT_4POINT0           = CH_LAYOUT_SURROUND     | CH_BACK_CENTER;
	public static final int CH_LAYOUT_5POINT0_BACK      = CH_LAYOUT_SURROUND     | CH_BACK_LEFT            | CH_BACK_RIGHT;
	public static final int CH_LAYOUT_5POINT1_BACK      = CH_LAYOUT_5POINT0_BACK | CH_LOW_FREQUENCY;
	public static final int CH_LAYOUT_7POINT1_WIDE_BACK = CH_LAYOUT_5POINT1_BACK | CH_FRONT_LEFT_OF_CENTER | CH_FRONT_RIGHT_OF_CENTER;
	// Extension payload IDs
	public static final int EXT_FILL          = 0x0;
	public static final int EXT_FILL_DATA     = 0x1;
	public static final int EXT_DATA_ELEMENT  = 0x2;
	public static final int EXT_DYNAMIC_RANGE = 0xB;
	public static final int EXT_SBR_DATA      = 0xD;
	public static final int EXT_SBR_DATA_CRC  = 0xE;
	// Channel positions
	public static final int AAC_CHANNEL_OFF   = 0;
	public static final int AAC_CHANNEL_FRONT = 1;
	public static final int AAC_CHANNEL_SIDE  = 2;
	public static final int AAC_CHANNEL_BACK  = 3;
	public static final int AAC_CHANNEL_LFE   = 4;
	public static final int AAC_CHANNEL_CC    = 5;
	// Audio object types
	public static final int AOT_AAC_MAIN      = 1;
	public static final int AOT_AAC_LC        = 2;
	public static final int AOT_AAC_LTP       = 4;
	public static final int AOT_ER_AAC_LC     = 17;
	public static final int AOT_ER_AAC_LTP    = 19;
	public static final int AOT_ER_AAC_LD     = 23;
	public static final int AOT_ER_AAC_ELD    = 39;
	// Window Sequences
	public static final int ONLY_LONG_SEQUENCE   = 0;
	public static final int LONG_START_SEQUENCE  = 1;
	public static final int EIGHT_SHORT_SEQUENCE = 2;
	public static final int LONG_STOP_SEQUENCE  = 3;
	// Band Types
	public static final int ZERO_BT        = 0;     ///< Scalefactors and spectral data are all zero.
	public static final int FIRST_PAIR_BT  = 5;     ///< This and later band types encode two values (rather than four) with one code word.
	public static final int ESC_BT         = 11;    ///< Spectral data are coded with an escape sequence.
	public static final int NOISE_BT       = 13;    ///< Spectral data are scaled white noise not coded in the bitstream.
	public static final int INTENSITY_BT2  = 14;    ///< Scalefactor data are intensity stereo positions.
	public static final int INTENSITY_BT   = 15;    ///< Scalefactor data are intensity stereo positions.
	// Coupling Points
	public static final int BEFORE_TNS            = 0;
	public static final int BETWEEN_TNS_AND_IMDCT = 1;
	public static final int AFTER_IMDCT           = 3;

	private static VLC vlc_scalefactors;
	private static VLC vlc_spectral[] = new VLC[11];

	private Context ac;
	private BitReader br;

	private static class ElemToChannel {
		int avPosition;
		int synEle;
		int elemId;
		int aacPosition;

		public ElemToChannel() {
		}

		public ElemToChannel(int avPosition, int synEle, int elemId, int aacPosition) {
			this.avPosition = avPosition;
			this.synEle = synEle;
			this.elemId = elemId;
			this.aacPosition = aacPosition;
		}

		public void copy(ElemToChannel that) {
			avPosition  = that.avPosition;
			synEle      = that.synEle;
			elemId      = that.elemId;
			aacPosition = that.aacPosition;
		}
	}

	/* @name ltp_coef
	 * Table of the LTP coefficients
	 */
	public static final float ltp_coef[] = {
	    0.570829f, 0.696616f, 0.813004f, 0.911304f,
	    0.984900f, 1.067894f, 1.194601f, 1.369533f
	};

	/* @name tns_tmp2_map
	 * Tables of the tmp2[] arrays of LPC coefficients used for TNS.
	 * The suffix _M_N[] indicate the values of coef_compress and coef_res
	 * respectively.
	 * @{
	 */
	public static final float tns_tmp2_map_1_3[] = {
	     0.00000000f, -0.43388373f,  0.64278758f,  0.34202015f
	};

	public static final float tns_tmp2_map_0_3[] = {
	     0.00000000f, -0.43388373f, -0.78183150f, -0.97492790f,
	     0.98480773f,  0.86602539f,  0.64278758f,  0.34202015f
	};

	public static final float tns_tmp2_map_1_4[] = {
	     0.00000000f, -0.20791170f, -0.40673664f, -0.58778524f,
	     0.67369562f,  0.52643216f,  0.36124167f,  0.18374951f
	};

	public static final float tns_tmp2_map_0_4[] = {
	     0.00000000f, -0.20791170f, -0.40673664f, -0.58778524f,
	    -0.74314481f, -0.86602539f, -0.95105654f, -0.99452192f,
	     0.99573416f,  0.96182561f,  0.89516330f,  0.79801720f,
	     0.67369562f,  0.52643216f,  0.36124167f,  0.18374951f
	};

	public static final float tns_tmp2_map[][] = {
	    tns_tmp2_map_0_3,
	    tns_tmp2_map_0_4,
	    tns_tmp2_map_1_3,
	    tns_tmp2_map_1_4
	};
	// @}
	public static final int tags_per_config[] = { 0, 1, 1, 2, 3, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0 };

	public static final int aac_channel_layout_map[][][] = {
	    { { TYPE_SCE, 0, AAC_CHANNEL_FRONT }, },
	    { { TYPE_CPE, 0, AAC_CHANNEL_FRONT }, },
	    { { TYPE_SCE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 0, AAC_CHANNEL_FRONT }, },
	    { { TYPE_SCE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 0, AAC_CHANNEL_FRONT }, { TYPE_SCE, 1, AAC_CHANNEL_BACK }, },
	    { { TYPE_SCE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 1, AAC_CHANNEL_BACK }, },
	    { { TYPE_SCE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 1, AAC_CHANNEL_BACK }, { TYPE_LFE, 0, AAC_CHANNEL_LFE  }, },
	    { { TYPE_SCE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 0, AAC_CHANNEL_FRONT }, { TYPE_CPE, 1, AAC_CHANNEL_FRONT }, { TYPE_CPE, 2, AAC_CHANNEL_BACK }, { TYPE_LFE, 0, AAC_CHANNEL_LFE  } },
	};

	public static final int aac_channel_layout[] = {
	    CH_LAYOUT_MONO,
	    CH_LAYOUT_STEREO,
	    CH_LAYOUT_SURROUND,
	    CH_LAYOUT_4POINT0,
	    CH_LAYOUT_5POINT0_BACK,
	    CH_LAYOUT_5POINT1_BACK,
	    CH_LAYOUT_7POINT1_WIDE_BACK,
	    0
	};

	private static final double M_SQRT2 = 1.41421356237309504880;

	private static final float cce_scale[] = {
	    1.09050773266525765921f, //2^(1/8)
	    1.18920711500272106672f, //2^(1/4)
	    (float) M_SQRT2,
	    2f,
	};

	private static int sampleRateIdx (int rate) {
	         if (92017 <= rate) return 0;
	    else if (75132 <= rate) return 1;
	    else if (55426 <= rate) return 2;
	    else if (46009 <= rate) return 3;
	    else if (37566 <= rate) return 4;
	    else if (27713 <= rate) return 5;
	    else if (23004 <= rate) return 6;
	    else if (18783 <= rate) return 7;
	    else if (13856 <= rate) return 8;
	    else if (11502 <= rate) return 9;
	    else if (9391  <= rate) return 10;
	    else                    return 11;
	}

	@Override
	public int init(int bytesPerFrame, int channels, int outputChannels, int codingMode) {
		ac = new Context();

		ac.outputChannels = outputChannels;
		ac.oc[1].m4ac.sampleRate = 44100;
		ac.oc[1].m4ac.samplingIndex = sampleRateIdx(ac.oc[1].m4ac.sampleRate);
		ac.channels = channels;
		ac.oc[1].m4ac.sbr = -1;
		ac.oc[1].m4ac.ps = -1;

		ac.oc[1].m4ac.chanConfig = 0;
		for (int i = 0; i < ff_mpeg4audio_channels.length; i++) {
			if (ff_mpeg4audio_channels[i] == ac.channels) {
				ac.oc[1].m4ac.chanConfig = i;
				break;
			}
		}

		if (ac.oc[1].m4ac.chanConfig != 0) {
			int layoutMap[][] = new int[MAX_ELEM_ID * 4][3];
			int layoutMapTags[] = new int[1];
			int ret = setDefaultChannelConfig(layoutMap, layoutMapTags, ac.oc[1].m4ac.chanConfig);
			if (ret == 0) {
				outputConfigure(layoutMap, layoutMapTags[0], OC_GLOBAL_HDR, false);
			}
		}

		for (int i = 0; i < vlc_spectral.length; i++) {
			vlc_spectral[i] = new VLC();
			vlc_spectral[i].initVLCSparse(8, ff_aac_spectral_sizes[i], ff_aac_spectral_bits[i], ff_aac_spectral_codes[i], null);
		}

		AacSbr.sbrInit();

		ac.randomState = 0x1f2e3d4c;

		AacTab.tableinit();
		AacPs.init();
		AacPsData.tableinit();

		vlc_scalefactors = new VLC();
		vlc_scalefactors.initVLCSparse(7, ff_aac_scalefactor_code.length, ff_aac_scalefactor_bits, ff_aac_scalefactor_code, null);

		ac.mdct = new FFT();
		ac.mdct.mdctInit(11, true, 1.0 / (32768.0 * 1024.0));
		ac.mdctLd = new FFT();
		ac.mdctLd.mdctInit(10, true, 1.0 / (32768.0 * 512.0));
		ac.mdctSmall = new FFT();
		ac.mdctSmall.mdctInit(8, true, 1.0 / (32768.0 * 128.0));
		ac.mdctLtp = new FFT();
		ac.mdctLtp.mdctInit(11, false, -2.0 * 32768.0);

		SineWin.initFfSineWindows();

		return 0;
	}

	/**
	 * linear congruential pseudorandom number generator
	 *
	 * @param   previousVal    pointer to the current state of the generator
	 *
	 * @return  Returns a 32-bit pseudorandom integer
	 */
	private static int lcgRandom(int previousVal) {
		return previousVal * 1664525 + 1013904223;
	}

	/**
	 * Set up channel positions based on a default channel configuration
	 * as specified in table 1.17.
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int setDefaultChannelConfig(int[][] layoutMap, int[] tags, int channelConfig) {
		if (channelConfig < 1 || channelConfig > 7) {
			log.error(String.format("invalid default channel configuration (%d)", channelConfig));
			return AAC_ERROR;
		}

		tags[0] = tags_per_config[channelConfig];
		for (int i = 0; i < tags[0]; i++) {
			Utilities.copy(layoutMap[i], aac_channel_layout_map[channelConfig - 1][i]);
		}

		return 0;
	}

	private int parseAdtsFrameHeader() {
		AACADTSHeaderInfo hdrInfo = new AACADTSHeaderInfo();
		int layoutMap[][] = new int[MAX_ELEM_ID * 4][3];
		int layoutMapTabs[] = new int[1];
		int ret;

		int size = hdrInfo.parse(br);
		if (size > 0) {
			if (hdrInfo.numAacFrames != 1) {
				log.warn(String.format("More than one AAC RDB per ADTS frame"));
			}
			pushOutputConfiguration();
			if (hdrInfo.chanConfig != 0) {
				ac.oc[1].m4ac.chanConfig = hdrInfo.chanConfig;
				ret = setDefaultChannelConfig(layoutMap, layoutMapTabs, hdrInfo.chanConfig);
				if (ret < 0) {
					return ret;
				}

				ret = outputConfigure(layoutMap, layoutMapTabs[0], Math.max(ac.oc[1].status, OC_TRIAL_FRAME), false);
				if (ret < 0) {
					return ret;
				}
			} else {
				ac.oc[1].m4ac.chanConfig = 0;
				// dual mono frames in Japanese DTV can have chan_config 0
				// WITHOUT specifying PCE.
				// Thus, set dual mono as default.
				if (ac.dmonoMode != 0 && ac.oc[0].status == OC_NONE) {
					int layoutMapTags = 2;
					layoutMap[0][0] = TYPE_SCE;
					layoutMap[1][0] = TYPE_SCE;
					layoutMap[0][2] = AAC_CHANNEL_FRONT;
					layoutMap[1][0] = AAC_CHANNEL_FRONT;
					layoutMap[0][1] = 0;
					layoutMap[1][1] = 1;
					if (outputConfigure(layoutMap, layoutMapTags, OC_TRIAL_FRAME, false) != 0) {
						return AAC_ERROR;
					}
				}
			}

			ac.oc[1].m4ac.sampleRate    = hdrInfo.sampleRate;
			ac.oc[1].m4ac.samplingIndex = hdrInfo.samplingIndex;
			ac.oc[1].m4ac.objectType    = hdrInfo.objectType;
			if (ac.oc[0].status != OC_LOCKED ||
			    ac.oc[0].m4ac.chanConfig != hdrInfo.chanConfig ||
			    ac.oc[0].m4ac.sampleRate != hdrInfo.sampleRate) {
				ac.oc[1].m4ac.sbr = -1;
				ac.oc[1].m4ac.ps  = -1;
			}

			if (!hdrInfo.crcAbsent) {
				br.skip(16);
			}
		}

		return size;
	}

	private int frameConfigureElements() {
		// set channel pointers to internal buffers by default
		for (int type = 0; type < 4; type++) {
			for (int id = 0; id < MAX_ELEM_ID; id++) {
				ChannelElement che = ac.che[type][id];
				if (che != null) {
					che.ch[0].ret = che.ch[0].retBuf;
					che.ch[1].ret = che.ch[1].retBuf;
				}
			}
		}

		// get output buffer
		if (ac.channels == 0) {
			return 1;
		}

		ac.nbSamples = 2048;

		// map output channel pointers
		for (int ch = 0; ch < ac.channels; ch++) {
			if (ac.outputElement[ch] != null) {
				ac.outputElement[ch].ret = ac.samples[ch];
			}
		}

		return 0;
	}

	private ChannelElement getChe(int type, int elemId) {
		// For PCE based channel configurations map the channels solely based
		// on tags.
		if (ac.oc[1].m4ac.chanConfig == 0) {
			return ac.tagCheMap[type][elemId];
		}

		// Allow single CPE stereo files to be signaled with mono configuration
		if (ac.tagsMapped == 0 && type == TYPE_CPE && ac.oc[1].m4ac.chanConfig == 1) {
			int layoutMap[][] = new int[MAX_ELEM_ID * 4][3];
			int layoutMapTags[] = new int[1];
			pushOutputConfiguration();

			if (setDefaultChannelConfig(layoutMap, layoutMapTags, 2) < 0) {
				return null;
			}
			if (outputConfigure(layoutMap, layoutMapTags[0], OC_TRIAL_FRAME, true) < 0) {
				return null;
			}

			ac.oc[1].m4ac.chanConfig = 2;
			ac.oc[1].m4ac.ps = 0;
		}

		// And vice-versa
		if (ac.tagsMapped == 0 && type == TYPE_SCE && ac.oc[1].m4ac.chanConfig == 2) {
			int layoutMap[][] = new int[MAX_ELEM_ID * 4][3];
			int layoutMapTags[] = new int[1];
			pushOutputConfiguration();

			if (setDefaultChannelConfig(layoutMap, layoutMapTags, 1) < 0) {
				return null;
			}
			if (outputConfigure(layoutMap, layoutMapTags[0], OC_TRIAL_FRAME, true) < 0) {
				return null;
			}

			ac.oc[1].m4ac.chanConfig = 1;
			if (ac.oc[1].m4ac.sbr != 0) {
				ac.oc[1].m4ac.ps = -1;
			}
		}

		// For indexed channel configurations map the channels solely based
		// on position.
		switch (ac.oc[1].m4ac.chanConfig) {
			case 7:
				if (ac.tagsMapped == 3 && type == TYPE_CPE) {
					ac.tagsMapped++;
					return ac.tagCheMap[TYPE_CPE][elemId] = ac.che[TYPE_CPE][2];
				}
				// Fall-through
			case 6:
		        /* Some streams incorrectly code 5.1 audio as
		         * SCE[0] CPE[0] CPE[1] SCE[1]
		         * instead of
		         * SCE[0] CPE[0] CPE[1] LFE[0].
		         * If we seem to have encountered such a stream, transfer
		         * the LFE[0] element to the SCE[1]'s mapping */
				if (ac.tagsMapped == tags_per_config[ac.oc[1].m4ac.chanConfig] - 1 && (type == TYPE_LFE || type == TYPE_SCE)) {
					ac.tagsMapped++;
					return ac.tagCheMap[type][elemId] = ac.che[TYPE_LFE][0];
				}
				// Fall-through
			case 5:
				if (ac.tagsMapped == 2 && type == TYPE_CPE) {
					ac.tagsMapped++;
					return ac.tagCheMap[TYPE_CPE][elemId] = ac.che[TYPE_CPE][1];
				}
				// Fall-through
			case 4:
				if (ac.tagsMapped == 2 && ac.oc[1].m4ac.chanConfig == 4 && type == TYPE_SCE) {
					ac.tagsMapped++;
					return ac.tagCheMap[TYPE_SCE][elemId] = ac.che[TYPE_SCE][1];
				}
				// Fall-through
			case 3:
			case 2:
				if (ac.tagsMapped == (ac.oc[1].m4ac.chanConfig != 2 ? 1 : 0) && type == TYPE_CPE) {
					ac.tagsMapped++;
					return ac.tagCheMap[TYPE_CPE][elemId] = ac.che[TYPE_CPE][0];
				} else if (ac.oc[1].m4ac.chanConfig == 2) {
					return null;
				}
				// Fall-through
			case 1:
				if (ac.tagsMapped == 0 && type == TYPE_SCE) {
					ac.tagsMapped++;
					return ac.tagCheMap[TYPE_SCE][elemId] = ac.che[TYPE_SCE][0];
				}
				// Fall-through
			default:
				return null;
		}
	}

	private int decodePrediction(IndividualChannelStream ics) {
		if (br.readBool()) {
			ics.predictorResetGroup = br.read(5);
			if (ics.predictorResetGroup == 0 || ics.predictorResetGroup > 30) {
				log.error(String.format("Invalid Predictor Reset Group"));
				return AAC_ERROR;
			}
		}

		for (int sfb = 0; sfb < Math.min(ics.maxSfb, AacTab.ff_aac_pred_sfb_max[ac.oc[1].m4ac.samplingIndex]); sfb++) {
			ics.predictionUsed[sfb] = br.readBool();
		}

		return 0;
	}

	/**
	 * Decode Long Term Prediction data; reference: table 4.xx.
	 */
	private void decodeLtp(LongTermPrediction ltp, int maxSfb) {
		ltp.lag = br.read(11);
		ltp.coef = ltp_coef[br.read(3)];
		for (int sfb = 0; sfb < Math.min(maxSfb, MAX_LTP_LONG_SFB); sfb++) {
			ltp.used[sfb] = br.readBool();
		}
	}

	/**
	 * Decode Individual Channel Stream info; reference: table 4.6.
	 */
	private int decodeIcsInfo(IndividualChannelStream ics) {
		int aot = ac.oc[1].m4ac.objectType;
		if (aot != AOT_ER_AAC_ELD) {
			if (br.readBool()) {
				log.error(String.format("Reserved bit set"));
				return AAC_ERROR;
			}
			ics.windowSequence[1] = ics.windowSequence[0];
			ics.windowSequence[0] = br.read(2);
			if (aot == AOT_ER_AAC_LD && ics.windowSequence[0] != ONLY_LONG_SEQUENCE) {
				log.error(String.format("AAC LD is only defined for ONLY_LONG_SEQUENCE but window sequence %d found", ics.windowSequence[0]));
				ics.windowSequence[0] = ONLY_LONG_SEQUENCE;
				return AAC_ERROR;
			}
			ics.useKbWindow[1] = ics.useKbWindow[0];
			ics.useKbWindow[0] = br.readBool();
		}

		ics.numWindowGroups = 1;
		ics.groupLen[0] = 1;

		if (ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE) {
			ics.maxSfb = br.read(4);
			for (int i = 0; i < 7; i++) {
				if (br.readBool()) {
					ics.groupLen[ics.numWindowGroups - 1]++;
				} else {
					ics.numWindowGroups++;
					ics.groupLen[ics.numWindowGroups - 1] = 1;
				}
			}
			ics.numWindows  = 8;
			ics.swbOffset   =    ff_swb_offset_128[ac.oc[1].m4ac.samplingIndex];
			ics.numSwb      =   ff_aac_num_swb_128[ac.oc[1].m4ac.samplingIndex];
			ics.tnsMaxBands = ff_tns_max_bands_128[ac.oc[1].m4ac.samplingIndex];
			ics.predictorPresent = false;
		} else {
			ics.maxSfb = br.read(6);
			ics.numWindows = 1;
			if (aot == AOT_ER_AAC_LD || aot == AOT_ER_AAC_ELD) {
				ics.swbOffset   =    ff_swb_offset_512[ac.oc[1].m4ac.samplingIndex];
				ics.numSwb      =   ff_aac_num_swb_512[ac.oc[1].m4ac.samplingIndex];
				ics.tnsMaxBands = ff_tns_max_bands_512[ac.oc[1].m4ac.samplingIndex];
				if (ics.numSwb == 0 || ics.swbOffset == null) {
					return AAC_ERROR;
				}
			} else {
				ics.swbOffset   =    ff_swb_offset_1024[ac.oc[1].m4ac.samplingIndex];
				ics.numSwb      =   ff_aac_num_swb_1024[ac.oc[1].m4ac.samplingIndex];
				ics.tnsMaxBands = ff_tns_max_bands_1024[ac.oc[1].m4ac.samplingIndex];
			}

			if (aot != AOT_ER_AAC_ELD) {
				ics.predictorPresent = br.readBool();
				ics.predictorResetGroup = 0;
			}

			if (ics.predictorPresent) {
				if (aot == AOT_AAC_MAIN) {
					if (decodePrediction(ics) != 0) {
						ics.maxSfb = 0;
						return AAC_ERROR;
					}
				} else if (aot == AOT_AAC_LC || aot == AOT_ER_AAC_LC) {
					log.error(String.format("Prediction is not allowed in AAC-LC"));
					ics.maxSfb = 0;
					return AAC_ERROR;
				} else {
					if (aot == AOT_ER_AAC_LD) {
						log.error(String.format("LTP in ER AAC LD not yet implemented"));
						return AAC_ERROR;
					}
					ics.ltp.present = br.readBool();
					if (ics.ltp.present) {
						decodeLtp(ics.ltp, ics.maxSfb);
					}
				}
			}
		}

		if (ics.maxSfb > ics.numSwb) {
			log.error(String.format("Number of scalefactor bands in group (%d) exceeds limit (%d)", ics.maxSfb, ics.numSwb));
			ics.maxSfb = 0;
			return AAC_ERROR;
		}

		return 0;
	}

	/**
	 * Decode band types (section_data payload); reference: table 4.46.
	 *
	 * @param   bandType         array of the used band type
	 * @param   bandTypeRunEnd   array of the last scalefactor band of a band type run
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodeBandTypes(int bandType[], int bandTypeRunEnd[], IndividualChannelStream ics) {
		int idx = 0;
		final int bits = (ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE) ? 3 : 5;

		for (int g = 0; g < ics.numWindowGroups; g++) {
			int k = 0;
			while (k < ics.maxSfb) {
				int sectEnd = k;
				int sectBandType = br.read(4);
				if (sectBandType == 12) {
					log.error(String.format("invalid band type"));
					return AAC_ERROR;
				}

				int sectLenIncr;
				do {
					sectLenIncr = br.read(bits);
					sectEnd += sectLenIncr;
					if (br.getBitsLeft() < 0) {
						log.error(String.format("decodeBandTypes overread error"));
						return AAC_ERROR;
					}
					if (sectEnd > ics.maxSfb) {
						log.error(String.format("Number of bands (%d) exceeds limit (%d)", sectEnd, ics.maxSfb));
						return AAC_ERROR;
					}
				} while (sectLenIncr == (1 << bits) - 1);

				for (; k < sectEnd; k++) {
					bandType      [idx  ] = sectBandType;
					bandTypeRunEnd[idx++] = sectEnd;
				}
			}
		}

		return 0;
	}

	/**
	 * Decode scalefactors; reference: table 4.47.
	 *
	 * @param   sf               array of scalefactors or intensity stereo positions
	 * @param   globalGain       first scalefactor value as scalefactors are differentially coded
	 * @param   bandType         array of the used band type
	 * @param   bandTypeRunEnd   array of the last scalefactor band of a band type run
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodeScalefactors(float sf[], int globalGain, IndividualChannelStream ics, int bandType[], int bandTypeRunEnd[]) {
		int idx = 0;
		int offset[] = { globalGain, globalGain - 90, 0 };
		boolean noiseFlag = true;

		for (int g = 0; g < ics.numWindowGroups; g++) {
			for (int i = 0; i < ics.maxSfb;) {
				int runEnd = bandTypeRunEnd[idx];
				if (bandType[idx] == ZERO_BT) {
					for (; i < runEnd; i++, idx++) {
						sf[idx] = 0f;
					}
				} else if (bandType[idx] == INTENSITY_BT || bandType[idx] == INTENSITY_BT2) {
					for (; i < runEnd; i++, idx++) {
						offset[2] += vlc_scalefactors.getVLC2(br, 3) - 60;
						int clippedOffset = Utilities.clip(offset[2], -155, 100);
						if (offset[2] != clippedOffset) {
							log.warn(String.format("Clipped intensity stereo position (%d -> %d)", offset[2], clippedOffset));
						}
						sf[idx] = ff_aac_pow2sf_tab[-clippedOffset + POW_SF2_ZERO];
					}
				} else if (bandType[idx] == NOISE_BT) {
					for (; i < runEnd; i++, idx++) {
						if (noiseFlag) {
							offset[1] += br.read(9) - 256;
							noiseFlag = false;
						} else {
							offset[1] += vlc_scalefactors.getVLC2(br, 3) - 60;
						}
						int clippedOffset = Utilities.clip(offset[1], -100, 155);
						if (offset[1] != clippedOffset) {
							log.warn(String.format("Clipped intensity stereo position (%d -> %d)", offset[1], clippedOffset));
						}
						sf[idx] = -ff_aac_pow2sf_tab[clippedOffset + POW_SF2_ZERO];
					}
				} else {
					for (; i < runEnd; i++, idx++) {
						offset[0] += vlc_scalefactors.getVLC2(br, 3) - 60;
						if (offset[0] > 255) {
							log.error(String.format("Scalefactor (%d) out of range", offset[0]));
							return AAC_ERROR;
						}
						sf[idx] = -ff_aac_pow2sf_tab[offset[0] - 100 + POW_SF2_ZERO];
					}
				}
			}
		}

		return 0;
	}

	/**
	 * Decode pulse data; reference: table 4.7.
	 */
	private int decodePulses(Pulse pulse, int swbOffset[], int numSwb) {
		pulse.numPulse = br.read(2) + 1;
		int pulseSwb   = br.read(6);
		if (pulseSwb >= numSwb) {
			return -1;
		}

		pulse.pos[0]  = swbOffset[pulseSwb];
		pulse.pos[0] += br.read(5);

		if (pulse.pos[0] >= swbOffset[numSwb]) {
			return -1;
		}

		pulse.amp[0] = br.read(4);

		for (int i = 1; i < pulse.numPulse; i++) {
			pulse.pos[i] = br.read(5) + pulse.pos[i - 1];
			if (pulse.pos[i] >= swbOffset[numSwb]) {
				return -1;
			}
			pulse.amp[i] = br.read(4);
		}

		return 0;
	}

	/**
	 * Decode Temporal Noise Shaping data; reference: table 4.48.
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodeTns(TemporalNoiseShaping tns, IndividualChannelStream ics) {
		final int is8 = ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE ? 1 : 0;
		final int tnsMaxOrder = is8 != 0 ? 7 : ac.oc[1].m4ac.objectType == AOT_AAC_MAIN ? 20 : 12;

		for (int w = 0; w < ics.numWindows; w++) {
			tns.nFilt[w] = br.read(2 - is8);
			if (tns.nFilt[w] != 0) {
				int coefRes = br.read1();

				for (int filt = 0; filt < tns.nFilt[w]; filt++) {
					tns.length[w][filt] = br.read(6 - 2 * is8);
					tns.order[w][filt] = br.read(5 - 2 * is8);

					if (tns.order[w][filt] > tnsMaxOrder) {
						log.error(String.format("TNS filter order %d is greater than maximum %d", tns.order[w][filt], tnsMaxOrder));
						tns.order[w][filt] = 0;
						return AAC_ERROR;
					}

					if (tns.order[w][filt] > 0) {
						tns.direction[w][filt] = br.readBool();
						int coefCompress = br.read1();
						int coefLen = coefRes + 3 - coefCompress;
						int tmp2Idx = 2 * coefCompress + coefRes;

						for (int i = 0; i < tns.order[w][filt]; i++) {
							tns.coef[w][filt][i] = tns_tmp2_map[tmp2Idx][br.read(coefLen)];
						}
					}
				}
			}
		}

		return 0;
	}

	private static int VMUL2(float dst[], int dstOffset, float v[], int vOffset, int idx, float scale) {
		dst[dstOffset++] = v[vOffset + ((idx     ) & 15)] * scale;
		dst[dstOffset++] = v[vOffset + ((idx >> 4) & 15)] * scale;

		return dstOffset;
	}

	private static int VMUL4(float dst[], int dstOffset, float v[], int vOffset, int idx, float scale) {
		dst[dstOffset++] = v[vOffset + ((idx     ) & 3)] * scale;
		dst[dstOffset++] = v[vOffset + ((idx >> 2) & 3)] * scale;
		dst[dstOffset++] = v[vOffset + ((idx >> 4) & 3)] * scale;
		dst[dstOffset++] = v[vOffset + ((idx >> 6) & 3)] * scale;

		return dstOffset;
	}

	private static int VMUL2S(float dst[], int dstOffset, float v[], int vOffset, int idx, int sign, float scale) {
		int s0 = Float.floatToRawIntBits(scale);
		int s1 = Float.floatToRawIntBits(scale);

		s0 ^= (sign >> 1) << 31;
		s1 ^= (sign     ) << 31;

		dst[dstOffset++] = v[vOffset + ((idx     ) & 15)] * Float.intBitsToFloat(s0);
		dst[dstOffset++] = v[vOffset + ((idx >> 4) & 15)] * Float.intBitsToFloat(s1);

		return dstOffset;
	}

	private static int VMUL4S(float dst[], int dstOffset, float v[], int vOffset, int idx, int sign, float scale) {
		int nz = idx >>> 12;
		int s = Float.floatToRawIntBits(scale);
		int t;

		t = s ^ (sign & (1 << 31));
		dst[dstOffset++] = v[vOffset + ((idx     ) & 3)] * Float.intBitsToFloat(t);

		sign <<= nz & 1; nz >>= 1;
		t = s ^ (sign & (1 << 31));
		dst[dstOffset++] = v[vOffset + ((idx >> 2) & 3)] * Float.intBitsToFloat(t);

		sign <<= nz & 1; nz >>= 1;
		t = s ^ (sign & (1 << 31));
		dst[dstOffset++] = v[vOffset + ((idx >> 4) & 3)] * Float.intBitsToFloat(t);

		sign <<= nz & 1;
		t = s ^ (sign & (1 << 31));
		dst[dstOffset++] = v[vOffset + ((idx >> 6) & 3)] * Float.intBitsToFloat(t);

		return dstOffset;
	}

	/**
	 * Decode spectral data; reference: table 4.50.
	 * Dequantize and scale spectral data; reference: 4.6.3.3.
	 *
	 * @param   coef            array of dequantized, scaled spectral data
	 * @param   sf              array of scalefactors or intensity stereo positions
	 * @param   pulsePresent    set if pulses are present
	 * @param   pulse           pointer to pulse data struct
	 * @param   bandType        array of the used band type
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodeSpectrumAndDequant(float coef[], float sf[], boolean pulsePresent, Pulse pulse, IndividualChannelStream ics, int bandType[]) {
		int idx = 0;
		final int c = 1024 / ics.numWindows;
		final int offsets[] = ics.swbOffset;

		for (int g = 0; g < ics.numWindows; g++) {
			Arrays.fill(coef, g * 128 + offsets[ics.maxSfb], g * 128 + c, 0f);
		}

		int coefOffset = 0;
		for (int g = 0; g < ics.numWindowGroups; g++) {
			int gLen = ics.groupLen[g];

			for (int i = 0; i < ics.maxSfb; i++, idx++) {
				final int cbtM1 = bandType[idx] == 0 ? Integer.MAX_VALUE : bandType[idx] - 1;
				int cfo = coefOffset + offsets[i];
				int offLen = offsets[i + 1] - offsets[i];

				if (cbtM1 >= INTENSITY_BT2 - 1) {
					for (int group = 0; group < gLen; group++, cfo += 128) {
						Arrays.fill(coef, cfo, cfo + offLen, 0f);
					}
				} else if (cbtM1 == NOISE_BT - 1) {
					for (int group = 0; group < gLen; group++, cfo += 128) {
						for (int k = 0; k < offLen; k++) {
							ac.randomState = lcgRandom(ac.randomState);
							coef[cfo + k] = ac.randomState;
						}

						float bandEnergy = FloatDSP.scalarproduct(coef, cfo, coef, cfo, offLen);
						float scale = sf[idx] / (float) sqrt(bandEnergy);
						FloatDSP.vectorFmulScalar(coef, cfo, coef, cfo, scale, offLen);
					}
				} else {
					final float vq[] = ff_aac_codebook_vector_vals[cbtM1];
					final int cbVertexIdx[] = ff_aac_codebook_vector_idx[cbtM1];
					VLC vlc = vlc_spectral[cbtM1];

					switch (cbtM1 >> 1) {
						case 0:
							for (int group = 0; group < gLen; group++, cfo += 128) {
								int cf = cfo;

								for (int len = offLen; len != 0; len -= 4) {
									int code = vlc.getVLC2(br, 2);
									int cbIdx = cbVertexIdx[code];
									cf = VMUL4(coef, cf, vq, 0, cbIdx, sf[idx]);
								}
							}
							break;

						case 1:
							for (int group = 0; group < gLen; group++, cfo += 128) {
								int cf = cfo;

								for (int len = offLen; len != 0; len -= 4) {
									int code = vlc.getVLC2(br, 2);
									int cbIdx = cbVertexIdx[code];
									int nnz = (cbIdx >> 8) & 15;
									int bits = nnz != 0 ? br.peek(32) : 0;
									br.skip(nnz);
									cf = VMUL4S(coef, cf, vq, 0, cbIdx, bits, sf[idx]);
								}
							}
							break;

						case 2:
							for (int group = 0; group < gLen; group++, cfo += 128) {
								int cf = cfo;

								for (int len = offLen; len != 0; len -= 2) {
									int code = vlc.getVLC2(br, 2);
									int cbIdx = cbVertexIdx[code];
									cf = VMUL2(coef, cf, vq, 0, cbIdx, sf[idx]);
								}
							}
							break;

						case 3:
						case 4:
							for (int group = 0; group < gLen; group++, cfo += 128) {
								int cf = cfo;

								for (int len = offLen; len != 0; len -= 2) {
									int code = vlc.getVLC2(br, 2);
									int cbIdx = cbVertexIdx[code];
									int nnz = (cbIdx >> 8) & 15;
									int sign = nnz != 0 ? (br.peek(nnz) << (cbIdx >> 12)) : 0;
									br.skip(nnz);
									cf = VMUL2S(coef, cf, vq, 0, cbIdx, sign, sf[idx]);
								}
							}
							break;

						default:
							for (int group = 0; group < gLen; group++, cfo += 128) {
								int icf = cfo;

								for (int len = offLen; len != 0; len -= 2) {
									int code = vlc.getVLC2(br, 2);

									if (code == 0) {
										coef[icf++] = 0f;
										coef[icf++] = 0f;
										continue;
									}

									int cbIdx = cbVertexIdx[code];
									int nnz = cbIdx >> 12;
									int nzt = cbIdx >> 8;
									int bits = br.read(nnz) << (32 - nnz);

									for (int j = 0; j < 2; j++) {
										if ((nzt & (1 << j)) != 0) {
		                                    /* The total length of escape_sequence must be < 22 bits according
		                                       to the specification (i.e. max is 111111110xxxxxxxxxxxx). */
											int b = br.peek(32);
											b = 31 - avLog2(~b);

											if (b > 8) {
												log.error(String.format("error in spectral data, ESC overflow"));
												return AAC_ERROR;
											}

											br.skip(b + 1);
											b += 4;
											int n = (1 << b) + br.read(b);
											coef[icf++] = Float.intBitsToFloat(cbrt_tab[n] | (bits & (1 << 31)));
											bits <<= 1;
										} else {
											float v = vq[cbIdx & 15];
											if (v == 0f) {
												coef[icf++] = 0f;
											} else {
												if ((bits & (1 << 31)) != 0) {
													v = -v;
												}
												coef[icf++] = v;
												bits <<= 1;
											}
										}
										cbIdx >>= 4;
									}
								}

								FloatDSP.vectorFmulScalar(coef, cfo, coef, cfo, sf[idx], offLen);
							}
							break;
					}
				}
			}

			coefOffset += gLen << 7;
		}

		if (pulsePresent) {
			idx = 0;
			for (int i = 0; i < pulse.numPulse; i++) {
				float co = coef[pulse.pos[i]];
				while (offsets[idx + 1] <= pulse.pos[i]) {
					idx++;
				}

				if (bandType[idx] != NOISE_BT && sf[idx] != 0f) {
					float ico = -pulse.amp[i];
					if (co != 0f) {
						co /= sf[idx];
						ico = co / (float) Math.sqrt(Math.sqrt(Math.abs(co))) + (co > 0f ? -ico : ico);
					}
					coef[pulse.pos[i]] = (float) Math.cbrt(Math.abs(ico)) * ico * sf[idx];
				}
			}
		}

		return 0;
	}

	private void resetPredictState(PredictorState ps) {
		ps.r0   = 0f;
		ps.r1   = 0f;
		ps.cor0 = 0f;
		ps.cor1 = 0f;
		ps.var0 = 1f;
		ps.var1 = 1f;
	}

	private void resetAllPredictors(PredictorState ps[]) {
		for (int i = 0; i < MAX_PREDICTORS; i++) {
			resetPredictState(ps[i]);
		}
	}

	private void resetPredictorGroup(PredictorState ps[], int groupNum) {
		for (int i = groupNum - 1; i < MAX_PREDICTORS; i += 30) {
			resetPredictState(ps[i]);
		}
	}

	private static float flt16Round(float pf) {
		int i = Float.floatToRawIntBits(pf);
		i = (i + 0x00008000) & 0xFFFF0000;
		return Float.intBitsToFloat(i);
	}

	private static float flt16Even(float pf) {
		int i = Float.floatToRawIntBits(pf);
		i = (i + 0x00007FFF + ((i & 0x00010000) >> 16)) & 0xFFFF0000;
		return Float.intBitsToFloat(i);
	}

	private static float flt16Trunc(float pf) {
		int i = Float.floatToRawIntBits(pf);
		i &= 0xFFFF0000;
		return Float.intBitsToFloat(i);
	}

	private void predict(PredictorState ps, float coef[], int coefOffset, boolean outputEnable) {
		final float a     = 0.953125f; // 61.0 / 64
		final float alpha = 0.90625f;  // 29.0 / 32
		float r0 = ps.r0;
		float r1 = ps.r1;
		float cor0 = ps.cor0;
		float cor1 = ps.cor1;
		float var0 = ps.var0;
		float var1 = ps.var1;

		float k1 = var0 > 1f ? cor0 * flt16Even(a / var0) : 0f;
		float k2 = var1 > 1f ? cor1 * flt16Even(a / var1) : 0f;

		float pv = flt16Round(k1 * r0 + k2 * r1);
		if (outputEnable) {
			coef[coefOffset] += pv;
		}

		float e0 = coef[coefOffset];
		float e1 = e0 - k1 * r0;

		ps.cor1 = flt16Trunc(alpha * cor1 + r1 * e1);
		ps.var1 = flt16Trunc(alpha * var1 + 0.5f * (r1 * r1 + e1 * e1));
		ps.cor0 = flt16Trunc(alpha * cor0 + r0 * e0);
		ps.var0 = flt16Trunc(alpha * var0 + 0.5f * (r0 * r0 + e0 * e0));

		ps.r1 = flt16Trunc(a * (r0 - k1 * e0));
		ps.r0 = flt16Trunc(a * e0);
	}

	/**
	 * Apply AAC-Main style frequency domain prediction.
	 */
	private void applyPrediction(SingleChannelElement sce) {
		if (!sce.ics.predictorInitialized) {
			resetAllPredictors(sce.predictorState);
			sce.ics.predictorInitialized = true;
		}

		if (sce.ics.windowSequence[0] != EIGHT_SHORT_SEQUENCE) {
			for (int sfb = 0; sfb < ff_aac_pred_sfb_max[ac.oc[1].m4ac.samplingIndex]; sfb++) {
				for (int k = sce.ics.swbOffset[sfb]; k < sce.ics.swbOffset[sfb + 1]; k++) {
					predict(sce.predictorState[k], sce.coeffs, k, sce.ics.predictorPresent && sce.ics.predictionUsed[sfb]);
				}
			}

			if (sce.ics.predictorResetGroup != 0) {
				resetPredictorGroup(sce.predictorState, sce.ics.predictorResetGroup);
			}
		} else {
			resetAllPredictors(sce.predictorState);
		}
	}

	/**
	 * Decode an individual_channel_stream payload; reference: table 4.44.
	 *
	 * @param   commonWindow   Channels have independent [0], or shared [1], Individual Channel Stream information.
	 * @param   scaleFlag      scalable [1] or non-scalable [0] AAC (Unused until scalable AAC is implemented.)
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodeIcs(SingleChannelElement sce, boolean commonWindow, boolean scaleFlag) {
		int ret;
	    Pulse pulse = new Pulse();
	    TemporalNoiseShaping    tns = sce.tns;
	    IndividualChannelStream ics = sce.ics;
	    float out[] = sce.coeffs;

	    boolean eldSyntax = ac.oc[1].m4ac.objectType == AOT_ER_AAC_ELD;
	    boolean erSyntax  = ac.oc[1].m4ac.objectType == AOT_ER_AAC_LC  ||
	                        ac.oc[1].m4ac.objectType == AOT_ER_AAC_LTP ||
	                        ac.oc[1].m4ac.objectType == AOT_ER_AAC_LD  ||
	                        ac.oc[1].m4ac.objectType == AOT_ER_AAC_ELD;

	    int globalGain = br.read(8);

	    if (!commonWindow && !scaleFlag) {
	    	if (decodeIcsInfo(ics) < 0) {
	    		return AAC_ERROR;
	    	}
	    }

	    ret = decodeBandTypes(sce.bandType, sce.bandTypeRunEnd, ics);
	    if (ret < 0) {
	    	return ret;
	    }

	    ret = decodeScalefactors(sce.sf, globalGain, ics, sce.bandType, sce.bandTypeRunEnd);
	    if (ret < 0) {
	    	return ret;
	    }

	    boolean pulsePresent = false;
	    if (!scaleFlag) {
	    	if (!eldSyntax && (pulsePresent = br.readBool())) {
	    		if (ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE) {
	    			log.error(String.format("Pulse tool not allowed in eight short sequence"));
	    			return AAC_ERROR;
	    		}
	    		if (decodePulses(pulse, ics.swbOffset, ics.numSwb) != 0) {
	    			log.error(String.format("Pulse data corrupt or invalid"));
	    			return AAC_ERROR;
	    		}
	    	}
	    	tns.present = br.readBool();
	    	if (tns.present && !erSyntax) {
	    		if (decodeTns(tns, ics) < 0) {
	    			return AAC_ERROR;
	    		}
	    	}
	    	if (!eldSyntax && br.readBool()) {
	    		return AAC_ERROR;
	    	}
	    	// I see no textual basis in the spec for this occuring after SSR gain
	    	// control, but this is what both reference and real implementations do
	    	if (tns.present && erSyntax) {
	    		if (decodeTns(tns, ics) < 0) {
	    			return AAC_ERROR;
	    		}
	    	}
	    }

	    if (decodeSpectrumAndDequant(out, sce.sf, pulsePresent, pulse, ics, sce.bandType) < 0) {
	    	return AAC_ERROR;
	    }

	    if (ac.oc[1].m4ac.objectType == AOT_AAC_MAIN && !commonWindow) {
	    	applyPrediction(sce);
	    }

	    return 0;
	}

	/**
	 * Decode an array of 4 bit element IDs, optionally interleaved with a
	 * stereo/mono switching bit.
	 *
	 * @param type speaker type/position for these channels
	 */
	private void decodeChannelMap(int[][] layoutMap, int layoutMapOffset, int type, int n) {
		while (n-- != 0) {
			int synEle;
			switch (type) {
		        case AAC_CHANNEL_FRONT:
		        case AAC_CHANNEL_BACK:
		        case AAC_CHANNEL_SIDE:
		            synEle = br.read1();
		            break;
		        case AAC_CHANNEL_CC:
		            br.skip(1);
		            synEle = TYPE_CCE;
		            break;
		        case AAC_CHANNEL_LFE:
		            synEle = TYPE_LFE;
		            break;
		        default:
		        	log.error(String.format("decodeChannelMap invalid type %d", type));
		        	return;
			}

			layoutMap[layoutMapOffset][0] = synEle;
			layoutMap[layoutMapOffset][1] = br.read(4);
			layoutMap[layoutMapOffset][2] = type;
			layoutMapOffset++;
		}
	}

	/**
	 * Decode program configuration element; reference: table 4.2.
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodePce(MPEG4AudioConfig m4ac, int[][] layoutMap) {
		br.skip(2); // object_type

		int samplingIndex = br.read(4);
		if (m4ac.samplingIndex != samplingIndex) {
			log.warn(String.format("Sample rate index in program config element does not match the sample rate index configured by the container"));
		}

		int numFront     = br.read(4);
		int numSide      = br.read(4);
		int numBack      = br.read(4);
		int numLfe       = br.read(2);
		int numAssocData = br.read(3);
		int numCc        = br.read(4);

		if (br.readBool()) {
			br.skip(4); // mono_mixdown_tag
		}
		if (br.readBool()) {
			br.skip(4); // stereo_mixdown_tag
		}

		if (br.readBool()) {
			br.skip(3); // mixdown_coeff_index and pseudo_surround
		}

		if (br.getBitsLeft() < 4 * (numFront + numSide + numBack + numLfe + numAssocData + numCc)) {
			log.error(String.format("decode_pce: overread error"));
			return AAC_ERROR;
		}

		decodeChannelMap(layoutMap,    0, AAC_CHANNEL_FRONT, numFront);
		int tags = numFront;
		decodeChannelMap(layoutMap, tags, AAC_CHANNEL_SIDE , numSide );
		tags += numSide;
		decodeChannelMap(layoutMap, tags, AAC_CHANNEL_BACK , numBack );
		tags += numBack;
		decodeChannelMap(layoutMap, tags, AAC_CHANNEL_LFE  , numLfe  );
		tags += numLfe;

		br.skip(4 * numAssocData);

		decodeChannelMap(layoutMap, tags, AAC_CHANNEL_CC   , numCc   );
		tags += numCc;

		br.byteAlign();

		// comment field, first byte is length
		int commentLen = br.read(8) * 8;
		if (br.getBitsLeft() < commentLen) {
			log.error(String.format("decode_pce: overread error"));
			return AAC_ERROR;
		}
		br.skip(commentLen);

		return tags;
	}

	/**
	 * Save current output configuration if and only if it has been locked.
	 */
	private void pushOutputConfiguration() {
		if (ac.oc[1].status == OC_LOCKED) {
			ac.oc[0].copy(ac.oc[1]);
		}
		ac.oc[1].status = OC_NONE;
	}

	/**
	 * Restore the previous output configuration if and only if the current
	 * configuration is unlocked.
	 */
	private void popOutputConfiguration() {
		if (ac.oc[1].status != OC_LOCKED && ac.oc[0].status != OC_NONE) {
			ac.oc[1].copy(ac.oc[0]);
			ac.channels = ac.oc[1].channels;
			outputConfigure(ac.oc[1].layoutMap, ac.oc[1].layoutMapTags, ac.oc[1].status, false);
		}
	}

	private int assignPair(ElemToChannel e2cVec[], int layoutMap[][], int offset, int left, int right, int pos) {
		if (layoutMap[offset][0] == TYPE_CPE) {
			e2cVec[offset] = new ElemToChannel(left | right, TYPE_CPE, layoutMap[offset][1], pos);
			return 1;
		}

		e2cVec[offset    ] = new ElemToChannel(left , TYPE_SCE, layoutMap[offset    ][1], pos);
		e2cVec[offset + 1] = new ElemToChannel(right, TYPE_SCE, layoutMap[offset + 1][1], pos);

		return 2;
	}

	private int countPairedChannels(int layoutMap[][], int tags, int pos, int[] current) {
		int numPosChannels = 0;
		boolean firstCpe = false;
		boolean sceParity = false;
		int i;
		for (i = current[0]; i < tags; i++) {
			if (layoutMap[i][2] != pos) {
				break;
			}
			if (layoutMap[i][0] == TYPE_CPE) {
				if (sceParity) {
					if (pos == AAC_CHANNEL_FRONT && !firstCpe) {
						sceParity = false;
					} else {
						return -1;
					}
				}
				numPosChannels += 2;
				firstCpe = true;
			} else {
				numPosChannels++;
				sceParity = !sceParity;
			}
		}

		if (sceParity && ((pos == AAC_CHANNEL_FRONT && firstCpe) || pos == AAC_CHANNEL_SIDE)) {
			return -1;
		}

		current[0] = i;

		return numPosChannels;
	}

	private int sniffChannelOrder(int[][] layoutMap, int tags) {
		ElemToChannel e2cVec[] = new ElemToChannel[4 * MAX_ELEM_ID];

		if (e2cVec.length < tags) {
			return 0;
		}

		int ii[] = new int[1];
		ii[0] = 0;
		int numFrontChannels = countPairedChannels(layoutMap, tags, AAC_CHANNEL_FRONT, ii);
		if (numFrontChannels < 0) {
			return 0;
		}
		int numSideChannels = countPairedChannels(layoutMap, tags, AAC_CHANNEL_SIDE, ii);
		if (numSideChannels < 0) {
			return 0;
		}
		int numBackChannels = countPairedChannels(layoutMap, tags, AAC_CHANNEL_BACK, ii);
		if (numBackChannels < 0) {
			return 0;
		}

		int i = 0;
		if ((numFrontChannels & 1) != 0) {
			e2cVec[i] = new ElemToChannel(CH_FRONT_CENTER, TYPE_SCE, layoutMap[i][1], AAC_CHANNEL_FRONT);
			i++;
			numFrontChannels--;
		}
	    if (numFrontChannels >= 4) {
	        i += assignPair(e2cVec, layoutMap, i,
	                         CH_FRONT_LEFT_OF_CENTER,
	                         CH_FRONT_RIGHT_OF_CENTER,
	                         AAC_CHANNEL_FRONT);
	        numFrontChannels -= 2;
	    }
	    if (numFrontChannels >= 2) {
	        i += assignPair(e2cVec, layoutMap, i,
	                         CH_FRONT_LEFT,
	                         CH_FRONT_RIGHT,
	                         AAC_CHANNEL_FRONT);
	        numFrontChannels -= 2;
	    }
	    while (numFrontChannels >= 2) {
	        i += assignPair(e2cVec, layoutMap, i,
	                         Integer.MAX_VALUE,
	                         Integer.MAX_VALUE,
	                         AAC_CHANNEL_FRONT);
	        numFrontChannels -= 2;
	    }

	    if (numSideChannels >= 2) {
	        i += assignPair(e2cVec, layoutMap, i,
	                         CH_SIDE_LEFT,
	                         CH_SIDE_RIGHT,
	                         AAC_CHANNEL_FRONT);
	        numSideChannels -= 2;
	    }
	    while (numSideChannels >= 2) {
	        i += assignPair(e2cVec, layoutMap, i,
	        		         Integer.MAX_VALUE,
	        		         Integer.MAX_VALUE,
	                         AAC_CHANNEL_SIDE);
	        numSideChannels -= 2;
	    }

	    while (numBackChannels >= 4) {
	        i += assignPair(e2cVec, layoutMap, i,
	        		         Integer.MAX_VALUE,
	                         Integer.MAX_VALUE,
	                         AAC_CHANNEL_BACK);
	        numBackChannels -= 2;
	    }
	    if (numBackChannels >= 2) {
	        i += assignPair(e2cVec, layoutMap, i,
	                         CH_BACK_LEFT,
	                         CH_BACK_RIGHT,
	                         AAC_CHANNEL_BACK);
	        numBackChannels -= 2;
	    }
	    if (numBackChannels > 0) {
	        e2cVec[i] = new ElemToChannel(CH_BACK_CENTER, TYPE_SCE, layoutMap[i][1], AAC_CHANNEL_BACK);
	        i++;
	        numBackChannels--;
	    }

	    if (i < tags && layoutMap[i][2] == AAC_CHANNEL_LFE) {
	        e2cVec[i] = new ElemToChannel(CH_LOW_FREQUENCY, TYPE_LFE, layoutMap[i][1], AAC_CHANNEL_LFE);
	        i++;
	    }
	    while (i < tags && layoutMap[i][2] == AAC_CHANNEL_LFE) {
	        e2cVec[i] = new ElemToChannel(Integer.MAX_VALUE, TYPE_LFE, layoutMap[i][1], AAC_CHANNEL_LFE);
	        i++;
	    }

	    // Must choose a stable sort
	    int totalNonCcElements = i;
	    int n = i;
	    ElemToChannel tmp = new ElemToChannel();
	    do {
	    	int nextN = 0;
	    	for (i = 1; i < n; i++) {
	    		if (e2cVec[i - 1].avPosition > e2cVec[i].avPosition) {
	    			tmp.copy(e2cVec[i - 1]);
	    			e2cVec[i - 2].copy(e2cVec[i]);
	    			e2cVec[i].copy(tmp);
	    			nextN = i;
	    		}
	    	}
	    	n = nextN;
	    } while (n > 0);

	    int layout = 0;
	    for (i = 0; i < totalNonCcElements; i++) {
	    	layoutMap[i][0] = e2cVec[i].synEle;
	    	layoutMap[i][1] = e2cVec[i].elemId;
	    	layoutMap[i][2] = e2cVec[i].aacPosition;
	    	if (e2cVec[i].avPosition != Integer.MAX_VALUE) {
	    		layout |= e2cVec[i].avPosition;
	    	}
	    }

	    return layout;
	}

	/**
	 * Check for the channel element in the current channel position configuration.
	 * If it exists, make sure the appropriate element is allocated and map the
	 * channel order to match the internal FFmpeg channel layout.
	 *
	 * @param   chePos current channel position configuration
	 * @param   type channel element type
	 * @param   id channel element id
	 * @param   channels count of the number of channels in the configuration
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int cheConfigure(int chePos, int type, int id, int[] channels) {
		if (channels[0] >= MAX_CHANNELS) {
			return AAC_ERROR;
		}

		if (chePos != 0) {
			if (ac.che[type][id] == null) {
				ac.che[type][id] = new ChannelElement();
				AacSbr.ctxInit(ac.che[type][id].sbr);
			}
			if (type != TYPE_CCE) {
				if (channels[0] >= MAX_CHANNELS - ((type == TYPE_CPE || (type == TYPE_SCE && ac.oc[1].m4ac.ps == 1)) ? 1 : 0)) {
					log.error(String.format("Too many channels"));
					return AAC_ERROR;
				}
				ac.outputElement[channels[0]++] = ac.che[type][id].ch[0];
				if (type == TYPE_CPE || (type == TYPE_SCE && ac.oc[1].m4ac.ps == 1)) {
					ac.outputElement[channels[0]++] = ac.che[type][id].ch[1];
				}
			}
		} else {
			if (ac.che[type][id] != null) {
				AacSbr.ctxClose(ac.che[type][id].sbr);
				ac.che[type][id] = null;
			}
		}

		return 0;
	}

	/**
	 * Configure output channel order based on the current program
	 * configuration element.
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int outputConfigure(int[][] layoutMap, int tags, int ocType, boolean getNewFrame) {
		if (ac.oc[1].layoutMap != layoutMap) {
			for (int i = 0; i < tags; i++) {
				System.arraycopy(layoutMap[i], 0, ac.oc[1].layoutMap[i], 0, 3);
			}
			ac.oc[1].layoutMapTags = tags;
		}

		// Try to sniff a reasonable channel order, otherwise output the
		// channels in the order the PCE declared them
		int layout = sniffChannelOrder(layoutMap, tags);
		int channels[] = new int[1];
		for (int i = 0; i < tags; i++) {
			int type     = layoutMap[i][0];
			int id       = layoutMap[i][1];
			int position = layoutMap[i][2];
			// Allocate or free elements depending on if they are in the
			// current program configuration
			int ret = cheConfigure(position, type, id, channels);
			if (ret < 0) {
				return ret;
			}
		}
		if (ac.oc[1].m4ac.ps == 1 && channels[0] == 2) {
			if (layout == CH_FRONT_CENTER) {
				layout = CH_FRONT_LEFT | CH_FRONT_RIGHT;
			} else {
				layout = 0;
			}
		}

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < MAX_ELEM_ID; j++) {
				ac.tagCheMap[i][j] = ac.che[i][j];
			}
		}
		ac.oc[1].channelLayout = layout;
		ac.channels = channels[0];
		ac.oc[1].channels = channels[0];
		ac.oc[1].status = ocType;

		if (getNewFrame) {
			int ret = frameConfigureElements();
			if (ret < 0) {
				return ret;
			}
		}

		return 0;
	}

	/**
	 * Decode a channel_pair_element; reference: table 4.4.
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodeCpe(ChannelElement cpe) {
		int ret;
		int msPresent = 0;
		boolean eldSyntax = ac.oc[1].m4ac.objectType == AOT_ER_AAC_ELD;

		boolean commonWindow = eldSyntax || br.readBool();
		if (commonWindow) {
			if (decodeIcsInfo(cpe.ch[0].ics) != 0) {
				return AAC_ERROR;
			}
			boolean i = cpe.ch[1].ics.useKbWindow[0];
			cpe.ch[1].ics.copy(cpe.ch[0].ics);
			cpe.ch[1].ics.useKbWindow[1] = i;
			if (cpe.ch[1].ics.predictorPresent && ac.oc[1].m4ac.objectType != AOT_AAC_MAIN) {
				cpe.ch[1].ics.ltp.present = br.readBool();
				if (cpe.ch[1].ics.ltp.present) {
					decodeLtp(cpe.ch[1].ics.ltp, cpe.ch[1].ics.maxSfb);
				}
			}
			msPresent = br.read(2);
			if (msPresent == 3) {
				log.error(String.format("ms_present = 3 is reserved"));
				return AAC_ERROR;
			}
			if (msPresent != 0) {
				decodeMidSideStereo(cpe, msPresent);
			}
		}

		ret = decodeIcs(cpe.ch[0], commonWindow, false);
		if (ret != 0) {
			return ret;
		}
		ret = decodeIcs(cpe.ch[1], commonWindow, false);
		if (ret != 0) {
			return ret;
		}

		if (commonWindow) {
			if (msPresent != 0) {
				applyMidSideStereo(cpe);
			}
			if (ac.oc[1].m4ac.objectType == AOT_AAC_MAIN) {
				applyPrediction(cpe.ch[0]);
				applyPrediction(cpe.ch[1]);
			}
		}

		applyIntensityStereo(cpe, msPresent);

		return 0;
	}

	/**
	 * Decode Mid/Side data; reference: table 4.54.
	 *
	 * @param   ms_present  Indicates mid/side stereo presence. [0] mask is all 0s;
	 *                      [1] mask is decoded from bitstream; [2] mask is all 1s;
	 *                      [3] reserved for scalable AAC
	 */
	private void decodeMidSideStereo(ChannelElement cpe, int msPresent) {
		if (msPresent == 1) {
			for (int idx = 0; idx < cpe.ch[0].ics.numWindowGroups * cpe.ch[0].ics.maxSfb; idx++) {
				cpe.msMask[idx] = br.read1();
			}
		} else if (msPresent == 2) {
			Arrays.fill(cpe.msMask, 0, cpe.ch[0].ics.numWindowGroups * cpe.ch[0].ics.maxSfb, 1);
		}
	}

	/**
	 * Mid/Side stereo decoding; reference: 4.6.8.1.3.
	 */
	private void applyMidSideStereo(ChannelElement cpe) {
		final IndividualChannelStream ics = cpe.ch[0].ics;
		int ch0 = 0;
		int ch1 = 0;
		int idx = 0;
		final int offsets[] = ics.swbOffset;

		for (int g = 0; g < ics.numWindowGroups; g++) {
			for (int i = 0; i < ics.maxSfb; i++, idx++) {
				if (cpe.msMask[idx] != 0 && cpe.ch[0].bandType[idx] < NOISE_BT && cpe.ch[1].bandType[idx] < NOISE_BT) {
					for (int group = 0; group < ics.groupLen[g]; group++) {
						FloatDSP.butterflies(cpe.ch[0].coeffs, ch0 + group * 128 + offsets[i], cpe.ch[1].coeffs, ch1 + group * 128 + offsets[i], offsets[i + 1] - offsets[i]);
					}
				}
			}
			ch0 += ics.groupLen[g] * 128;
			ch1 += ics.groupLen[g] * 128;
		}
	}

	/**
	 * intensity stereo decoding; reference: 4.6.8.2.3
	 *
	 * @param   ms_present  Indicates mid/side stereo presence. [0] mask is all 0s;
	 *                      [1] mask is decoded from bitstream; [2] mask is all 1s;
	 *                      [3] reserved for scalable AAC
	 */
	private void applyIntensityStereo(ChannelElement cpe, int msPresent) {
		final IndividualChannelStream ics = cpe.ch[1].ics;
		final SingleChannelElement sce1 = cpe.ch[1];
		int coef0 = 0;
		int coef1 = 0;
		final int offsets[] = ics.swbOffset;
		int idx = 0;

		for (int g = 0; g < ics.numWindowGroups; g++) {
			for (int i = 0; i < ics.maxSfb; ) {
				if (sce1.bandType[idx] == INTENSITY_BT || sce1.bandType[idx] == INTENSITY_BT2) {
					final int btRunEnd = sce1.bandTypeRunEnd[idx];
					for (; i < btRunEnd; i++, idx++) {
						int c = -1 + 2 * (sce1.bandType[idx] - 14);
						if (msPresent != 0) {
							c *= 1 - 2 * cpe.msMask[idx];
						}
						float scale = c * sce1.sf[idx];
						for (int group = 0; group < ics.groupLen[g]; group++) {
							FloatDSP.vectorFmulScalar(cpe.ch[1].coeffs, coef1 + group * 128 + offsets[i], cpe.ch[0].coeffs, coef0 + group * 128 + offsets[i], scale, offsets[i + 1] - offsets[i]);
						}
					}
				} else {
					int btRunEnd = sce1.bandTypeRunEnd[idx];
					idx += btRunEnd - i;
					i = btRunEnd;
				}
			}
			coef0 += ics.groupLen[g] * 128;
			coef1 += ics.groupLen[g] * 128;
		}
	}

	/**
	 * Decode coupling_channel_element; reference: table 4.8.
	 *
	 * @return  Returns error status. 0 - OK, !0 - error
	 */
	private int decodeCce(ChannelElement che) {
		int numGain = 0;
		SingleChannelElement sce = che.ch[0];
		ChannelCoupling coup = che.coup;

		coup.couplingPoint = 2 * br.read1();
		coup.numCoupled = br.read(3);
		for (int c = 0; c <= coup.numCoupled; c++) {
			numGain++;
			coup.type[c] = br.readBool() ? TYPE_CPE : TYPE_SCE;
			coup.idSelect[c] = br.read(4);
			if (coup.type[c] == TYPE_CPE) {
				coup.chSelect[c] = br.read(2);
				if (coup.chSelect[c] == 3) {
					numGain++;
				}
			} else {
				coup.chSelect[c] = 2;
			}
		}

		coup.couplingPoint += (br.readBool() || (coup.couplingPoint >> 1) != 0) ? 1 : 0;

		boolean sign = br.readBool();
		float scale = cce_scale[br.read(2)];

		int ret = decodeIcs(sce, false, false);
		if (ret != 0) {
			return ret;
		}

		for (int c = 0; c < numGain; c++) {
			int idx = 0;
			boolean cge = true;
			int gain = 0;
			float gainCache = 1f;
			if (c != 0) {
				cge = coup.couplingPoint == AFTER_IMDCT ? true : br.readBool();
				gain = cge ? vlc_scalefactors.getVLC2(br, 3) - 60 : 0;
				gainCache = (float) pow(scale, -gain);
			}

			if (coup.couplingPoint == AFTER_IMDCT) {
				coup.gain[c][0] = gainCache;
			} else {
				for (int g = 0; g < sce.ics.numWindowGroups; g++) {
					for (int sfb = 0; sfb < sce.ics.maxSfb; sfb++, idx++) {
						if (sce.bandType[idx] != ZERO_BT) {
							if (!cge) {
								int t = vlc_scalefactors.getVLC2(br, 3) - 60;
								if (t != 0) {
									int s = 1;
									gain += t;
									t = gain;
									if (sign) {
										s -= 2 * (t & 0x1);
										t >>= 1;
									}
									gainCache = (float) pow(scale, -t) * s;
								}
							}
							coup.gain[c][idx] = gainCache;
						}
					}
				}
			}
		}

		return 0;
	}

	/**
	 * Skip data_stream_element; reference: table 4.10.
	 */
	private int skipDataStreamElement() {
		boolean byteAlign = br.readBool();
		int count = br.read(8);
		if (count == 255) {
			count += br.read(8);
		}
		if (byteAlign) {
			br.byteAlign();
		}

		if (br.getBitsLeft() < 8 * count) {
			log.error(String.format("skipDataStreamElement overread error"));
			return AAC_ERROR;
		}

		br.skip(8 * count);

		return 0;
	}

	/**
	 * Parse whether channels are to be excluded from Dynamic Range Compression; reference: table 4.53.
	 *
	 * @return  Returns number of bytes consumed.
	 */
	private int decodeDrcChannelExclusions(DynamicRangeControl cheDrc) {
		int numExclChan = 0;

		do {
			for (int i = 0; i < 7; i++) {
				cheDrc.excludeMask[numExclChan++] = br.read1();
			}
		} while (numExclChan < MAX_CHANNELS - 7 && br.readBool());

		return numExclChan / 7;
	}

	/**
	 * Decode dynamic range information; reference: table 4.52.
	 *
	 * @return  Returns number of bytes consumed.
	 */
	private int decodeDynamicRange(DynamicRangeControl cheDrc) {
		int n = 1;
		int drcNumBands = 1;

		// pce_tag_present?
		if (br.readBool()) {
			cheDrc.pceInstanceTag = br.read(4);
			br.skip(4); // tag_reserved_bits
			n++;
		}

		// excluded_chns_present?
		if (br.readBool()) {
			n += decodeDrcChannelExclusions(cheDrc);
		}

		// drc_bands_present?
		if (br.readBool()) {
			cheDrc.bandIncr = br.read(4);
			cheDrc.interpolationScheme = br.read(4);
			n++;
			drcNumBands += cheDrc.bandIncr;
			for (int i = 0; i < drcNumBands; i++) {
				cheDrc.bandTop[i] = br.read(8);
				n++;
			}
		}

		// prog_reg_level_present?
		if (br.readBool()) {
			cheDrc.progRefLevel = br.read(7);
			br.skip(1); // prog_ref_level_reserved_bits
			n++;
		}

		for (int i = 0; i < drcNumBands; i++) {
			cheDrc.dynRngSgn[i] = br.read1();
			cheDrc.dynRngCtl[i] = br.read(7);
			n++;
		}

		return n;
	}

	private int decodeFill(int len) {
		if (len >= 13 + 7 * 8) {
			br.read(13);
			len -= 13;

			byte buf[] = new byte[Math.min(256, len / 8)];
			for (int i = 0; i < buf.length; i++, len -= 8) {
				buf[i] = (byte) br.read(8);
			}

			String s = new String(buf);
			if (log.isDebugEnabled()) {
				log.debug(String.format("FILL: '%s'", s));
			}

			Pattern p = Pattern.compile("libfaac (\\d+)\\.(\\d+)");
			Matcher m = p.matcher(s);
			if (m.matches()) {
				ac.skipSamples = 1024;
			}
		}

		br.skip(len);

		return 0;
	}

	/**
	 * Decode extension data (incomplete); reference: table 4.51.
	 *
	 * @param   cnt length of TYPE_FIL syntactic element in bytes
	 *
	 * @return Returns number of bytes consumed
	 */
	private int decodeExtensionPayload(int cnt, ChannelElement che, int elemType) {
		boolean crcFlag = false;
		int res = cnt;

		switch (br.read(4)) { // extension type
			case EXT_SBR_DATA_CRC:
				crcFlag = true;
				// Fall-through
			case EXT_SBR_DATA:
				if (che == null) {
					log.error(String.format("SBR was found before the first channel element"));
					return res;
				} else if (ac.oc[1].m4ac.sbr == 0) {
					log.error(String.format("SBR signaled to be not-present but was found in the bitstream"));
					br.skip(8 * cnt - 4);
					return res;
				} else if (ac.oc[1].m4ac.sbr == -1 && ac.oc[1].status == OC_LOCKED) {
					log.error(String.format("Implicit SBR was found with a first occurrence after the first frame"));
					br.skip(8 * cnt - 4);
					return res;
				} else if (ac.oc[1].m4ac.ps == -1 && ac.oc[1].status < OC_LOCKED && ac.channels == 1) {
					ac.oc[1].m4ac.sbr = 1;
					ac.oc[1].m4ac.ps = 1;
					outputConfigure(ac.oc[1].layoutMap, ac.oc[1].layoutMapTags, ac.oc[1].status, true);
				} else {
					ac.oc[1].m4ac.sbr = 1;
				}
				res = AacSbr.decodeSbrExtension(ac, che.sbr, crcFlag, cnt, elemType);
				break;
			case EXT_DYNAMIC_RANGE:
				res = decodeDynamicRange(ac.cheDrc);
				break;
			case EXT_FILL:
				decodeFill(8 * cnt - 4);
				break;
			case EXT_FILL_DATA:
			case EXT_DATA_ELEMENT:
			default:
				br.skip(8 * cnt - 4);
				break;
		}

		return res;
	}

	private void imdctAndWindowing(SingleChannelElement sce) {
		IndividualChannelStream ics = sce.ics;
		float in[]    = sce.coeffs;
		float out[]   = sce.ret;
		float saved[] = sce.saved;
		final float swindow    [] = ics.useKbWindow[0] ? ff_aac_kbd_short_128 : ff_sine_128;
		final float lwindowPrev[] = ics.useKbWindow[1] ? ff_aac_kbd_long_1024 : ff_sine_1024;
		final float swindowPrev[] = ics.useKbWindow[1] ? ff_aac_kbd_short_128 : ff_sine_128;
		float buf[] = ac.bufMdct;
		float temp[] = ac.temp;

		// imdct
		if (ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE) {
			for (int i = 0; i < 1024; i += 128) {
				ac.mdctSmall.imdctHalf(buf, i, in, i);
			}
		} else {
			ac.mdct.imdctHalf(buf, 0, in, 0);
		}

	    /* window overlapping
	     * NOTE: To simplify the overlapping code, all 'meaningless' short to long
	     * and long to short transitions are considered to be short to short
	     * transitions. This leaves just two cases (long to long and short to short)
	     * with a little special sauce for EIGHT_SHORT_SEQUENCE.
	     */
		if ((ics.windowSequence[1] == ONLY_LONG_SEQUENCE || ics.windowSequence[1] == LONG_STOP_SEQUENCE) &&
		    (ics.windowSequence[0] == ONLY_LONG_SEQUENCE || ics.windowSequence[0] == LONG_START_SEQUENCE)) {
			FloatDSP.vectorFmulWindow(out, 0, saved, 0, buf, 0, lwindowPrev, 0, 512);
		} else {
			System.arraycopy(saved, 0, out, 0, 448);

			if (ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE) {
				FloatDSP.vectorFmulWindow(out , 448 + 0 * 128, saved, 448         , buf, 0 * 128, swindowPrev, 0, 64);
				FloatDSP.vectorFmulWindow(out , 448 + 1 * 128, buf  , 0 * 128 + 64, buf, 1 * 128, swindow    , 0, 64);
				FloatDSP.vectorFmulWindow(out , 448 + 2 * 128, buf  , 1 * 128 + 64, buf, 2 * 128, swindow    , 0, 64);
				FloatDSP.vectorFmulWindow(out , 448 + 3 * 128, buf  , 2 * 128 + 64, buf, 3 * 128, swindow    , 0, 64);
				FloatDSP.vectorFmulWindow(temp, 0            , buf  , 3 * 128 + 64, buf, 4 * 128, swindow    , 0, 64);
				System.arraycopy(temp, 0, out, 448 + 4 * 128, 64);
			} else {
				FloatDSP.vectorFmulWindow(out , 448          , saved, 448         , buf, 0      , swindowPrev, 0, 64);
				System.arraycopy(buf, 64, out, 576, 448);
			}
		}

		// buffer update
		if (ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE) {
			System.arraycopy(temp, 64, saved, 0, 64);
			FloatDSP.vectorFmulWindow(saved, 64 , buf, 4 * 128 + 64, buf, 5 * 128, swindow, 0, 64);
			FloatDSP.vectorFmulWindow(saved, 192, buf, 5 * 128 + 64, buf, 6 * 128, swindow, 0, 64);
			FloatDSP.vectorFmulWindow(saved, 320, buf, 6 * 128 + 64, buf, 7 * 128, swindow, 0, 64);
			System.arraycopy(buf, 7 * 128 + 64, saved, 448, 64);
		} else if (ics.windowSequence[0] == LONG_START_SEQUENCE) {
			System.arraycopy(buf, 512         , saved, 0  , 448);
			System.arraycopy(buf, 7 * 128 + 64, saved, 448, 64 );
		} else { // LONG_STOP or ONLY_LONG
			System.arraycopy(buf, 512         , saved, 0  , 512);
		}
	}

	private void imdctAndWindowingLd(SingleChannelElement sce) {
		IndividualChannelStream ics = sce.ics;
		float in[]    = sce.coeffs;
		float out[]   = sce.ret;
		float saved[] = sce.saved;
		float buf[]   = ac.bufMdct;

		// imdct
		ac.mdct.imdctHalf(buf, 0, in, 0);

		// window overlapping
		if (ics.useKbWindow[1]) {
			// AAC LD uses a low overlap sine window instead of a KBD window
			System.arraycopy(saved, 0, out, 0, 192);
			FloatDSP.vectorFmulWindow(out, 192, saved, 192, buf, 0, ff_sine_128, 0, 64);
			System.arraycopy(buf, 64, out, 320, 192);
		} else {
			FloatDSP.vectorFmulWindow(out, 0, saved, 0, buf, 0, ff_sine_512, 0, 256);
		}

		// buffer update
		System.arraycopy(buf, 256, saved, 0, 256);
	}

	private void imdctAndWindowingEld(SingleChannelElement sce) {
		float in[]     = sce.coeffs;
		float out[]    = sce.ret;
		float saved[]  = sce.saved;
		float window[] = AacTab.ff_aac_eld_window;
		float buf[]    = ac.bufMdct;
		final int n = 512;
		final int n2 = n >> 1;
		final int n4 = n >> 2;

	    // Inverse transform, mapped to the conventional IMDCT by
	    // Chivukula, R.K.; Reznik, Y.A.; Devarajan, V.,
	    // "Efficient algorithms for MPEG-4 AAC-ELD, AAC-LD and AAC-LC filterbanks,"
	    // International Conference on Audio, Language and Image Processing, ICALIP 2008.
	    // URL: http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=4590245&isnumber=4589950
		for (int i = 0; i < n2; i += 2) {
			float temp;
	        temp =  in[i    ]; in[i    ] = -in[n - 1 - i]; in[n - 1 - i] = temp;
	        temp = -in[i + 1]; in[i + 1] =  in[n - 2 - i]; in[n - 2 - i] = temp;
		}
		ac.mdct.imdctHalf(buf, 0, in, 0);
		for (int i = 0; i < n; i += 2) {
			buf[i] = -buf[i];
		}
	    // Like with the regular IMDCT at this point we still have the middle half
	    // of a transform but with even symmetry on the left and odd symmetry on
	    // the right

	    // window overlapping
	    // The spec says to use samples [0..511] but the reference decoder uses
	    // samples [128..639].
	    for (int i = n4; i < n2; i ++) {
	        out[i - n4] =    buf[n2 - 1 - i]       * window[i       - n4] +
	                       saved[      i + n2]     * window[i +   n - n4] +
	                      -saved[  n + n2 - 1 - i] * window[i + 2*n - n4] +
	                      -saved[2*n + n2 + i]     * window[i + 3*n - n4];
	    }
	    for (int i = 0; i < n2; i ++) {
	        out[n4 + i] =    buf[i]               * window[i + n2       - n4] +
	                      -saved[      n - 1 - i] * window[i + n2 +   n - n4] +
	                      -saved[  n + i]         * window[i + n2 + 2*n - n4] +
	                       saved[2*n + n - 1 - i] * window[i + n2 + 3*n - n4];
	    }
	    for (int i = 0; i < n4; i ++) {
	        out[n2 + n4 + i] =    buf[      i + n2]     * window[i +   n - n4] +
	                           -saved[      n2 - 1 - i] * window[i + 2*n - n4] +
	                           -saved[  n + n2 + i]     * window[i + 3*n - n4];
	    }

	    // buffer update
	    System.arraycopy(saved, 0, saved, n, 2 * n);
	    System.arraycopy(buf  , 0, saved, 0,     n);
	}

	private void imdctAndWindow(SingleChannelElement sce) {
		switch (ac.oc[1].m4ac.objectType) {
			case AOT_ER_AAC_LD:  imdctAndWindowingLd(sce);  break;
			case AOT_ER_AAC_ELD: imdctAndWindowingEld(sce); break;
			default:             imdctAndWindowing(sce);    break;
		}
	}

	/**
	 *  Apply windowing and MDCT to obtain the spectral
	 *  coefficient from the predicted sample by LTP.
	 */
	private void windowingAndMdctLtp(float out[], float in[], IndividualChannelStream ics) {
		final float lwindow    [] = ics.useKbWindow[0] ? ff_aac_kbd_long_1024 : ff_sine_1024;
		final float swindow    [] = ics.useKbWindow[0] ? ff_aac_kbd_short_128 : ff_sine_128;
		final float lwindowPrev[] = ics.useKbWindow[1] ? ff_aac_kbd_long_1024 : ff_sine_1024;
		final float swindowPrev[] = ics.useKbWindow[1] ? ff_aac_kbd_short_128 : ff_sine_128;

		if (ics.windowSequence[0] != LONG_STOP_SEQUENCE) {
			FloatDSP.vectorFmul(in, 0, in, 0, lwindowPrev, 0, 1024);
		} else {
			Arrays.fill(in, 0, 448, 0f);
			FloatDSP.vectorFmul(in, 448, in, 448, swindowPrev, 0, 128);
		}

		if (ics.windowSequence[0] != LONG_START_SEQUENCE) {
			FloatDSP.vectorFmulReverse(in, 1024, in, 1024, lwindow, 0, 1024);
		} else {
			FloatDSP.vectorFmulReverse(in, 1024 + 448, in, 1024 + 448, swindow, 0, 128);
			Arrays.fill(in, 1024 + 576, 1024 + 576 + 448, 0f);
		}
		ac.mdctLtp.mdctCalc(out, 0, in, 0);
	}

	/**
	 * Apply the long term prediction
	 */
	private void applyLtp(SingleChannelElement sce) {
		LongTermPrediction ltp = sce.ics.ltp;
		final int offsets[] = sce.ics.swbOffset;

		if (sce.ics.windowSequence[0] != EIGHT_SHORT_SEQUENCE) {
			final float predTime[] = sce.ret;
			final float predFreq[] = ac.bufMdct;
			int numSamples = 2048;

			if (ltp.lag < 1024) {
				numSamples = ltp.lag + 1024;
			}

			for (int i = 0; i < numSamples; i++) {
				predTime[i] = sce.ltpState[i + 2048 - ltp.lag] * ltp.coef;
			}
			Arrays.fill(predTime, numSamples, 2048, 0f);

			windowingAndMdctLtp(predFreq, predTime, sce.ics);

			if (sce.tns.present) {
				applyTns(predFreq, sce.tns, sce.ics, false);
			}

			for (int sfb = 0; sfb < Math.min(sce.ics.maxSfb,  MAX_LTP_LONG_SFB); sfb++) {
				if (ltp.used[sfb]) {
					for (int i = offsets[sfb]; i < offsets[sfb + 1]; i++) {
						sce.coeffs[i] += predFreq[i];
					}
				}
			}
		}
	}

	/**
	 * Update the LTP buffer for next frame
	 */
	private void updateLtp(SingleChannelElement sce) {
		IndividualChannelStream ics = sce.ics;
		float saved[]    = sce.saved;
		float savedLtp[] = sce.coeffs;
		final float lwindow[] = ics.useKbWindow[0] ? ff_aac_kbd_long_1024 : ff_sine_1024;
		final float swindow[] = ics.useKbWindow[0] ? ff_aac_kbd_short_128 : ff_sine_128;

		if (ics.windowSequence[0] == EIGHT_SHORT_SEQUENCE) {
			System.arraycopy(saved, 0, savedLtp, 0, 512);
			Arrays.fill(savedLtp, 576, 576 + 448, 0f);
			FloatDSP.vectorFmulReverse(savedLtp, 448, ac.bufMdct, 960, swindow, 64, 64);
			for (int i = 0; i < 64; i++) {
				savedLtp[i + 512] = ac.bufMdct[1023 - i] * swindow[63 - i];
			}
		} else if (ics.windowSequence[0] == LONG_START_SEQUENCE) {
			System.arraycopy(ac.bufMdct, 512, savedLtp, 0, 448);
			Arrays.fill(savedLtp, 576, 576 + 448, 0f);
			FloatDSP.vectorFmulReverse(savedLtp, 448, ac.bufMdct, 960, swindow, 64, 64);
			for (int i = 0; i < 64; i++) {
				savedLtp[i + 512] = ac.bufMdct[1023 - i] * swindow[63 - i];
			}
		} else { // LONG_STOP or ONLY_LONG
			FloatDSP.vectorFmulReverse(savedLtp, 0, ac.bufMdct, 512, lwindow, 512, 512);
			for (int i = 0; i < 512; i++) {
				savedLtp[i + 512] = ac.bufMdct[1023 - i] * lwindow[511 - i];
			}
		}

		System.arraycopy(sce.ltpState, 1024, sce.ltpState, 0   , 1024);
		System.arraycopy(sce.ret     , 0   , sce.ltpState, 1024, 1024);
		System.arraycopy(savedLtp    , 0   , sce.ltpState, 2048, 1024);
	}

	/**
	 * Decode Temporal Noise Shaping filter coefficients and apply all-pole filters; reference: 4.6.9.3.
	 *
	 * @param   decode  1 if tool is used normally, 0 if tool is used in LTP.
	 * @param   coef    spectral coefficients
	 */
	private void applyTns(float coef[], TemporalNoiseShaping tns, IndividualChannelStream ics, boolean decode) {
		final int mmm = Math.min(ics.tnsMaxBands, ics.maxSfb);
		final float lpc[] = new float[TNS_MAX_ORDER];
		final float tmp[] = new float[TNS_MAX_ORDER + 1];

		for (int w = 0; w < ics.numWindows; w++) {
			int bottom = ics.numSwb;
			for (int filt = 0; filt < tns.nFilt[w]; filt++) {
				int top = bottom;
				bottom = Math.max(0, top - tns.length[w][filt]);
				int order = tns.order[w][filt];

				if (order == 0) {
					continue;
				}

				// tns_decode_coef
				computeLpcCoefs(tns.coef[w][filt], order, lpc, 0, false, false);

				int start = ics.swbOffset[Math.min(bottom, mmm)];
				int end   = ics.swbOffset[Math.min(top   , mmm)];
				int size  = end - start;
				if (size <= 0) {
					continue;
				}

				int inc;
				if (tns.direction[w][filt]) {
					inc = -1;
					start = end - 1;
				} else {
					inc = 1;
				}
				start += w * 128;

				if (decode) {
					// ar filter
					for (int m = 0; m < size; m++, start += inc) {
						for (int i = 1; i <= Math.min(m, order); i++) {
							coef[start] -= coef[start - i * inc] * lpc[i - 1];
						}
					}
				} else {
					// ma filter
					for (int m = 0; m < size; m++, start += inc) {
						tmp[0] = coef[start];
						for (int i = 1; i <= Math.min(m, order); i++) {
							coef[start] += tmp[i] * lpc[i - 1];
						}
						for (int i = order; i > 0; i--) {
							tmp[i] = tmp[i - 1];
						}
					}
				}
			}
		}
	}

	/**
	 * Apply dependent channel coupling (applied before IMDCT).
	 *
	 * @param   index   index into coupling gain array
	 */
	private void applyDependentCoupling(SingleChannelElement target, ChannelElement cce, int index) {
		IndividualChannelStream ics = cce.ch[0].ics;
		int offsets[] = ics.swbOffset;
		float dest[] = target.coeffs;
		float src[] = cce.ch[0].coeffs;
		int idx = 0;

		if (ac.oc[1].m4ac.objectType == AOT_AAC_LTP) {
			log.error(String.format("Dependent coupling is not supported together with LTP"));
			return;
		}

		int destOffset = 0;
		int srcOffset = 0;
		for (int g = 0; g < ics.numWindowGroups; g++) {
			for (int i = 0; i < ics.maxSfb; i++, idx++) {
				if (cce.ch[0].bandType[idx] != ZERO_BT) {
					final float gain = cce.coup.gain[index][idx];
					for (int group = 0; group < ics.groupLen[g]; group++) {
						for (int k = offsets[i]; k < offsets[i + 1]; k++) {
							dest[destOffset + group * 128 + k] += gain * src[srcOffset + group * 128 + k];
						}
					}
				}
			}

			destOffset += ics.groupLen[g] * 128;
			srcOffset  += ics.groupLen[g] * 128;
		}
	}

	/**
	 * Apply independent channel coupling (applied after IMDCT).
	 *
	 * @param   index   index into coupling gain array
	 */
	private void applyIndependentCoupling(SingleChannelElement target, ChannelElement cce, int index) {
		final float gain = cce.coup.gain[index][0];
		float src[] = cce.ch[0].ret;
		float dest[] = target.ret;
		final int len = 1024 << (ac.oc[1].m4ac.sbr == 1 ? 1 : 0);

		for (int i = 0; i < len; i++) {
			dest[i] += gain * src[i];
		}
	}

	private void applyCouplingMethod(SingleChannelElement target, ChannelElement cce, int index, boolean applyDependentCoupling) {
		if (applyDependentCoupling) {
			applyDependentCoupling(target, cce, index);
		} else {
			applyIndependentCoupling(target, cce, index);
		}
	}

	/**
	 * channel coupling transformation interface
	 *
	 * @param   apply_coupling_method   pointer to (in)dependent coupling function
	 */
	private void applyChannelCoupling(ChannelElement cc, int type, int elemId, int couplingPoint, boolean applyDependentCoupling) {
		for (int i = 0; i < MAX_ELEM_ID; i++) {
			ChannelElement cce = ac.che[TYPE_CCE][i];

			if (cce != null && cce.coup.couplingPoint == couplingPoint) {
				int index = 0;
				ChannelCoupling coup = cce.coup;

				for (int c = 0; c <= coup.numCoupled; c++) {
					if (coup.type[c] == type && coup.idSelect[c] == elemId) {
						if (coup.chSelect[c] != 1) {
							applyCouplingMethod(cc.ch[0], cce, index, applyDependentCoupling);
							if (coup.chSelect[c] != 0) {
								index++;
							}
						}
						if (coup.chSelect[c] != 2) {
							applyCouplingMethod(cc.ch[1], cce, index++, applyDependentCoupling);
						}
					} else {
						index += 1 + (coup.chSelect[c] == 3 ? 1 : 0);
					}
				}
			}
		}
	}

	/**
	 * Convert spectral data to float samples, applying all supported tools as appropriate.
	 */
	private void spectralToSample() {
		for (int type = 3; type >= 0; type--) {
			for (int i = 0; i < MAX_ELEM_ID; i++) {
				ChannelElement che = ac.che[type][i];
				if (che != null) {
					if (type <= TYPE_CPE) {
						applyChannelCoupling(che, type, i, BEFORE_TNS, true);
					}
					if (ac.oc[1].m4ac.objectType == AOT_AAC_LTP) {
						if (che.ch[0].ics.predictorPresent) {
							if (che.ch[0].ics.ltp.present) {
								applyLtp(che.ch[0]);
							}
							if (che.ch[1].ics.ltp.present && type == TYPE_CPE) {
								applyLtp(che.ch[1]);
							}
						}
					}

					if (che.ch[0].tns.present) {
						applyTns(che.ch[0].coeffs, che.ch[0].tns, che.ch[0].ics, true);
					}
					if (che.ch[1].tns.present) {
						applyTns(che.ch[1].coeffs, che.ch[1].tns, che.ch[1].ics, true);
					}

					if (type <= TYPE_CPE) {
						applyChannelCoupling(che, type, i, BETWEEN_TNS_AND_IMDCT, true);
					}

					if (type != TYPE_CCE || che.coup.couplingPoint == AFTER_IMDCT) {
						imdctAndWindow(che.ch[0]);

						if (ac.oc[1].m4ac.objectType == AOT_AAC_LTP) {
							updateLtp(che.ch[0]);
						}

						if (type == TYPE_CPE) {
							imdctAndWindow(che.ch[1]);
							if (ac.oc[1].m4ac.objectType == AOT_AAC_LTP) {
								updateLtp(che.ch[1]);
							}
						}

						if (ac.oc[1].m4ac.sbr > 0) {
							AacSbr.sbrApply(ac, che.sbr, type, che.ch[0].ret, che.ch[1].ret);
						}
					}

					if (type <= TYPE_CCE) {
						applyChannelCoupling(che, type, i, AFTER_IMDCT, false);
					}
				}
			}
		}
	}

	private int decodeFrameInt() {
		int err;
		int elemType;
		int elemTypePrev = TYPE_END;
		ChannelElement che = null;
		ChannelElement chePrev = null;
		boolean audioFound = false;
		int sceCount = 0;
		boolean pceFound = false;

		if (br.peek(12) == 0xFFF) {
			err = parseAdtsFrameHeader();
			if (err < 0) {
				popOutputConfiguration();
				return err;
			}
			if (ac.oc[1].m4ac.samplingIndex > 12) {
				log.error(String.format("Invalid sampling rate index %d", ac.oc[1].m4ac.samplingIndex));
				popOutputConfiguration();
				return AAC_ERROR;
			}
		}

		err = frameConfigureElements();
		if (err < 0) {
			popOutputConfiguration();
			return err;
		}

		ac.tagsMapped = 0;
		int samples = 0;
		// parse
		while ((elemType = br.read(3)) != TYPE_END) {
			int elemId = br.read(4);

			if (elemType < TYPE_DSE) {
				che = getChe(elemType, elemId); 
				if (che == null) {
					log.error(String.format("channel element %d.%d is not allocated", elemType, elemId));
					popOutputConfiguration();
					return AAC_ERROR;
				}
				samples = 1024;
			}

			switch (elemType) {
				case TYPE_SCE:
					err = decodeIcs(che.ch[0], false, false);
					audioFound = true;
					sceCount++;
					break;

				case TYPE_CPE:
					err = decodeCpe(che);
					audioFound = true;
					break;

				case TYPE_CCE:
					err = decodeCce(che);
					break;

				case TYPE_LFE:
					err = decodeIcs(che.ch[0], false, false);
					audioFound = true;
					break;

				case TYPE_DSE:
					err = skipDataStreamElement();
					break;

				case TYPE_PCE: {
					int layoutMap[][] = new int[MAX_ELEM_ID * 4][3];
					pushOutputConfiguration();
					int tags = decodePce(ac.oc[1].m4ac, layoutMap);
					if (tags < 0) {
						err = tags;
						break;
					}
					if (pceFound) {
						log.error(String.format("Not evaluating a further program_config_element as this construct is dubious at best"));
					} else {
						err = outputConfigure(layoutMap, tags, OC_TRIAL_PCE, true);
						if (err == 0) {
							ac.oc[1].m4ac.chanConfig = 0;
						}
						pceFound = true;
					}
					break;
				}

				case TYPE_FIL:
					if (elemId == 15) {
						elemId += br.read(8) - 1;
					}
					if (br.getBitsLeft() < 8 * elemId) {
						log.error(String.format("TYPE_FIL: overread error"));
						popOutputConfiguration();
						return AAC_ERROR;
					}
					while (elemId > 0) {
						elemId -= decodeExtensionPayload(elemId, chePrev, elemTypePrev);
					}
					err = 0;
					break;

				default:
					log.error(String.format("Unknown element type %d", elemType));
					popOutputConfiguration();
					return AAC_ERROR;
			}

			chePrev = che;
			elemTypePrev = elemType;

			if (err != 0) {
				popOutputConfiguration();
				return err;
			}

			if (br.getBitsLeft() < 3) {
				log.error(String.format("overread error"));
				popOutputConfiguration();
				return AAC_ERROR;
			}
		}

		spectralToSample();

		int multiplier = ac.oc[1].m4ac.sbr == 1 ? (ac.oc[1].m4ac.extSampleRate > ac.oc[1].m4ac.sampleRate ? 1 : 0) : 0;
		samples <<= multiplier;

		if (ac.oc[1].status != 0 && audioFound) {
			ac.frameSize = samples;
			ac.oc[1].status = OutputConfiguration.OC_LOCKED;
		}

		if (samples != 0) {
			ac.nbSamples = samples;
		}

		// for dual-mono audio (SCE + SCE)
		boolean isDmono = ac.dmonoMode != 0 && sceCount == 2 && ac.oc[1].channelLayout == (CH_FRONT_LEFT | CH_FRONT_RIGHT);
		if (isDmono) {
			if (ac.dmonoMode == 1) {
				System.arraycopy(ac.samples[0], 0, ac.samples[1], 0, samples);
			} else if (ac.dmonoMode == 2) {
				System.arraycopy(ac.samples[1], 0, ac.samples[0], 0, samples);
			}
		}

		return 0;
	}

	private int decodeErFrame() {
		int samples = 1024;
		int chanConfig = ac.oc[1].m4ac.chanConfig;
		int aot = ac.oc[1].m4ac.objectType;

		if (aot == AOT_ER_AAC_LD || aot == AOT_ER_AAC_ELD) {
			samples >>= 1;
		}

		int err = frameConfigureElements();
		if (err < 0) {
			return err;
		}

		ac.tagsMapped = 0;

		if (chanConfig < 0 || chanConfig >= 8) {
			log.error(String.format("Unknown ER channel configuration %d", chanConfig));
			return AAC_ERROR;
		}

		for (int i = 0; i < tags_per_config[chanConfig]; i++) {
			final int elemType = aac_channel_layout_map[chanConfig - 1][i][0];
			final int elemId   = aac_channel_layout_map[chanConfig - 1][i][1];

			ChannelElement che = getChe(elemType, elemId);
			if (che == null) {
				log.error(String.format("channel element %d.%d is not allocated", elemType, elemId));
				return AAC_ERROR;
			}

			if (aot != AOT_ER_AAC_ELD) {
				br.skip(4);
			}

			switch (elemType) {
				case TYPE_SCE:
					err = decodeIcs(che.ch[0], false, false);
					break;
				case TYPE_CPE:
					err = decodeCpe(che);
					break;
				case TYPE_LFE:
					err = decodeIcs(che.ch[0], false, false);
					break;
			}
			if (err < 0) {
				return err;
			}
		}

		spectralToSample();

		ac.nbSamples = samples;

		br.skip(br.getBitsLeft());

		return 0;
	}

	@Override
	public int decode(int inputAddr, int inputLength, int outputAddr) {
		br = new BitReader(inputAddr, inputLength);
		ac.br = br;

		ac.dmonoMode = 0;

		int err;
		switch (ac.oc[1].m4ac.objectType) {
			case AOT_ER_AAC_LC:
			case AOT_ER_AAC_LTP:
			case AOT_ER_AAC_LD:
			case AOT_ER_AAC_ELD:
				err = decodeErFrame();
				break;
			default:
				err = decodeFrameInt();
				break;
		}

		if (err < 0) {
			return err;
		}

		CodecUtils.writeOutput(ac.samples, outputAddr, ac.nbSamples, ac.channels, ac.outputChannels);

		return br.getBytesRead();
	}

	@Override
	public int getNumberOfSamples() {
		return ac.nbSamples;
	}
}
