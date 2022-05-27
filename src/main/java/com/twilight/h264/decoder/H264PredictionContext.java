package com.twilight.h264.decoder;

public class H264PredictionContext {
	
	public static final int CODEC_ID_NONE = 0;
    /* video codecs */
	public static final int CODEC_ID_MPEG1VIDEO = 1;
	public static final int CODEC_ID_MPEG2VIDEO = 2; ///< preferred ID for MPEG-1/2 video decoding
	public static final int CODEC_ID_MPEG2VIDEO_XVMC = 3;
	public static final int CODEC_ID_H261 = 4;
	public static final int CODEC_ID_H263 = 5;
	public static final int CODEC_ID_RV10 = 6;
	public static final int CODEC_ID_RV20 = 7;
	public static final int CODEC_ID_MJPEG = 8;
	public static final int CODEC_ID_MJPEGB = 9;
	public static final int CODEC_ID_LJPEG = 10;
	public static final int CODEC_ID_SP5X = 11;
	public static final int CODEC_ID_JPEGLS = 12;
	public static final int CODEC_ID_MPEG4 = 13;
	public static final int CODEC_ID_RAWVIDEO = 14;
	public static final int CODEC_ID_MSMPEG4V1 = 15;
	public static final int CODEC_ID_MSMPEG4V2 = 16;
	public static final int CODEC_ID_MSMPEG4V3 = 17;
	public static final int CODEC_ID_WMV1 = 18;
	public static final int CODEC_ID_WMV2 = 19;
	public static final int CODEC_ID_H263P = 20;
	public static final int CODEC_ID_H263I = 21;
	public static final int CODEC_ID_FLV1 = 22;
	public static final int CODEC_ID_SVQ1 = 23;
	public static final int CODEC_ID_SVQ3 = 24;
	public static final int CODEC_ID_DVVIDEO = 25;
	public static final int CODEC_ID_HUFFYUV = 26;
	public static final int CODEC_ID_CYUV = 27;
	public static final int CODEC_ID_H264 = 28;
	public static final int CODEC_ID_INDEO3 = 29;
	public static final int CODEC_ID_VP3 = 30;
	public static final int CODEC_ID_THEORA = 31;
	public static final int CODEC_ID_ASV1 = 32;
	public static final int CODEC_ID_ASV2 = 33;
	public static final int CODEC_ID_FFV1 = 34;
	public static final int CODEC_ID_4XM = 35;
	public static final int CODEC_ID_VCR1 = 36;
	public static final int CODEC_ID_CLJR = 37;
	public static final int CODEC_ID_MDEC = 38;
	public static final int CODEC_ID_ROQ = 39;
	public static final int CODEC_ID_INTERPLAY_VIDEO = 40;
	public static final int CODEC_ID_XAN_WC3 = 41;
	public static final int CODEC_ID_XAN_WC4 = 42;
	public static final int CODEC_ID_RPZA = 43;
	public static final int CODEC_ID_CINEPAK = 44;
	public static final int CODEC_ID_WS_VQA = 45;
	public static final int CODEC_ID_MSRLE = 46;
	public static final int CODEC_ID_MSVIDEO1 = 47;
	public static final int CODEC_ID_IDCIN = 48;
	public static final int CODEC_ID_8BPS = 49;
	public static final int CODEC_ID_SMC = 50;
	public static final int CODEC_ID_FLIC = 51;
	public static final int CODEC_ID_TRUEMOTION1 = 52;
	public static final int CODEC_ID_VMDVIDEO = 53;
	public static final int CODEC_ID_MSZH = 54;
	public static final int CODEC_ID_ZLIB = 55;
	public static final int CODEC_ID_QTRLE = 56;
	public static final int CODEC_ID_SNOW = 57;
	public static final int CODEC_ID_TSCC = 58;
	public static final int CODEC_ID_ULTI = 59;
	public static final int CODEC_ID_QDRAW = 60;
	public static final int CODEC_ID_VIXL = 61;
	public static final int CODEC_ID_QPEG = 62;
	public static final int CODEC_ID_PNG = 63;
	public static final int CODEC_ID_PPM = 64;
	public static final int CODEC_ID_PBM = 65;
	public static final int CODEC_ID_PGM = 66;
	public static final int CODEC_ID_PGMYUV = 67;
	public static final int CODEC_ID_PAM = 68;
	public static final int CODEC_ID_FFVHUFF = 69;
	public static final int CODEC_ID_RV30 = 70;
	public static final int CODEC_ID_RV40 = 71;
	public static final int CODEC_ID_VC1 = 72;
	public static final int CODEC_ID_WMV3 = 73;
	public static final int CODEC_ID_LOCO = 74;
	public static final int CODEC_ID_WNV1 = 75;
	public static final int CODEC_ID_AASC = 76;
	public static final int CODEC_ID_INDEO2 = 77;
	public static final int CODEC_ID_FRAPS = 78;
	public static final int CODEC_ID_TRUEMOTION2 = 79;
	public static final int CODEC_ID_BMP = 80;
	public static final int CODEC_ID_CSCD = 81;
	public static final int CODEC_ID_MMVIDEO = 82;
	public static final int CODEC_ID_ZMBV = 83;
	public static final int CODEC_ID_AVS = 84;
	public static final int CODEC_ID_SMACKVIDEO = 85;
	public static final int CODEC_ID_NUV = 86;
	public static final int CODEC_ID_KMVC = 87;
	public static final int CODEC_ID_FLASHSV = 88;
	public static final int CODEC_ID_CAVS = 89;
	public static final int CODEC_ID_JPEG2000 = 90;
	public static final int CODEC_ID_VMNC = 91;
	public static final int CODEC_ID_VP5 = 92;
	public static final int CODEC_ID_VP6 = 93;
	public static final int CODEC_ID_VP6F = 94;
	public static final int CODEC_ID_TARGA = 95;
	public static final int CODEC_ID_DSICINVIDEO = 96;
	public static final int CODEC_ID_TIERTEXSEQVIDEO = 97;
	public static final int CODEC_ID_TIFF = 98;
	public static final int CODEC_ID_GIF = 99;
	public static final int CODEC_ID_FFH264 = 100;
	public static final int CODEC_ID_DXA = 101;
	public static final int CODEC_ID_DNXHD = 102;
	public static final int CODEC_ID_THP = 103;
	public static final int CODEC_ID_SGI = 104;
	public static final int CODEC_ID_C93 = 105;
	public static final int CODEC_ID_BETHSOFTVID = 106;
	public static final int CODEC_ID_PTX = 107;
	public static final int CODEC_ID_TXD = 108;
	public static final int CODEC_ID_VP6A = 109;
	public static final int CODEC_ID_AMV = 110;
	public static final int CODEC_ID_VB = 111;
	public static final int CODEC_ID_PCX = 112;
	public static final int CODEC_ID_SUNRAST = 113;
	public static final int CODEC_ID_INDEO4 = 114;
	public static final int CODEC_ID_INDEO5 = 115;
	public static final int CODEC_ID_MIMIC = 116;
	public static final int CODEC_ID_RL2 = 117;
	public static final int CODEC_ID_8SVX_EXP = 118;
	public static final int CODEC_ID_8SVX_FIB = 119;
	public static final int CODEC_ID_ESCAPE124 = 120;
	public static final int CODEC_ID_DIRAC = 121;
	public static final int CODEC_ID_BFI = 122;
	public static final int CODEC_ID_CMV = 123;
	public static final int CODEC_ID_MOTIONPIXELS = 124;
	public static final int CODEC_ID_TGV = 125;
	public static final int CODEC_ID_TGQ = 126;
	public static final int CODEC_ID_TQI = 127;
	public static final int CODEC_ID_AURA = 128;
	public static final int CODEC_ID_AURA2 = 129;
	public static final int CODEC_ID_V210X = 130;
	public static final int CODEC_ID_TMV = 131;
	public static final int CODEC_ID_V210 = 132;
	public static final int CODEC_ID_DPX = 133;
	public static final int CODEC_ID_MAD = 134;
	public static final int CODEC_ID_FRWU = 135;
	public static final int CODEC_ID_FLASHSV2 = 136;
	public static final int CODEC_ID_CDGRAPHICS = 137;
	public static final int CODEC_ID_R210 = 138;
	public static final int CODEC_ID_ANM = 139;
	public static final int CODEC_ID_BINKVIDEO = 140;
	public static final int CODEC_ID_IFF_ILBM = 141;
	public static final int CODEC_ID_IFF_BYTERUN1 = 142;
	public static final int CODEC_ID_KGV1 = 143;
	public static final int CODEC_ID_YOP = 144;
	public static final int CODEC_ID_VP8 = 145;
	public static final int CODEC_ID_PICTOR = 146;
	public static final int CODEC_ID_ANSI = 147;
	public static final int CODEC_ID_A64_MULTI = 148;
	public static final int CODEC_ID_A64_MULTI5 = 149;
	public static final int CODEC_ID_R10K = 150;
	public static final int CODEC_ID_MXPEG = 151;
	public static final int CODEC_ID_LAGARITH = 152;
	
	/**
	 * Prediction types
	 */
	//@{
	public static final int VERT_PRED =            0;
	public static final int HOR_PRED =             1;
	public static final int DC_PRED =              2;
	public static final int DIAG_DOWN_LEFT_PRED =  3;
	public static final int DIAG_DOWN_RIGHT_PRED = 4;
	public static final int VERT_RIGHT_PRED =      5;
	public static final int HOR_DOWN_PRED   =      6;
	public static final int VERT_LEFT_PRED  =      7;
	public static final int HOR_UP_PRED     =      8;

	// DC edge (not for VP8)
	public static final int LEFT_DC_PRED    =      9;
	public static final int TOP_DC_PRED     =      10;
	public static final int DC_128_PRED     =      11;

	// RV40 specific
	public static final int DIAG_DOWN_LEFT_PRED_RV40_NODOWN  = 12;
	public static final int HOR_UP_PRED_RV40_NODOWN          = 13;
	public static final int VERT_LEFT_PRED_RV40_NODOWN       = 14;

	// VP8 specific
	public static final int TM_VP8_PRED     =      9;    ///< "True Motion", used instead of plane
	public static final int VERT_VP8_PRED   =      10;    ///< for VP8, #VERT_PRED is the average of
	                                    ///< (left col+cur col x2+right col) / 4;
	                                    ///< this is the "unaveraged" one
	public static final int HOR_VP8_PRED     =     11;    ///< unaveraged version of #HOR_PRED, see
	                                    ///< #VERT_VP8_PRED for details
	public static final int DC_127_PRED      =     12;
	public static final int DC_129_PRED      =     13;

	public static final int DC_PRED8x8     =       0;
	public static final int HOR_PRED8x8    =       1;
	public static final int VERT_PRED8x8   =       2;
	public static final int PLANE_PRED8x8  =       3;

	// DC edge
	public static final int LEFT_DC_PRED8x8   =    4;
	public static final int TOP_DC_PRED8x8    =    5;
	public static final int DC_128_PRED8x8    =    6;

	// H264/SVQ3 (8x8) specific
	public static final int ALZHEIMER_DC_L0T_PRED8x8 =7;
	public static final int ALZHEIMER_DC_0LT_PRED8x8 =8;
	public static final int ALZHEIMER_DC_L00_PRED8x8 =9;
	public static final int ALZHEIMER_DC_0L0_PRED8x8 =10;

	// VP8 specific
	public static final int DC_127_PRED8x8        =7;
	public static final int DC_129_PRED8x8        =8;
	//@}
	
	
	public interface IPrediction4x4 {
		public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride);//FIXME move to dsp?		
	}
	public interface IPrediction8x8L {
		public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride);
	}
	public interface IPrediction8x8 {
		public void pred8x8(int[] src, int src_offset, int stride);
	}
	public interface IPrediction16x16 {
		public void pred16x16(int[] src, int src_offset, int stride);
	}
	public interface IPrediction4x4_add {
		public void pred4x4_add(int[] pix, int pix_offset/*align  4*/, short[] block/*align 16*/, int _block_offset, int stride);
	}
	public interface IPrediction8x8L_add {
		public void pred8x8l_add(int[] pix, int pix_offset/*align  8*/, short[] block/*align 16*/, int _block_offset, int stride);
	}
	public interface IPrediction8x8_add {
		public void pred8x8_add(int[] pix, int pix_offset/*align  8*/, int[] block_offset, int block_offset_offset, short[] block/*align 16*/, int _block_offset, int stride);
	}
	public interface IPrediction16x16_add {
		public void pred16x16_add(int[] pix, int pix_offset/*align 16*/, int[] block_offset, int block_offset_offset, short[] block/*align 16*/, int _block_offset, int stride);
	}
	
	public IPrediction4x4[] pred4x4 = new IPrediction4x4[9+3+3];
	public IPrediction8x8L[] pred8x8l = new IPrediction8x8L[9+3];
	public IPrediction8x8[] pred8x8 = new IPrediction8x8[4+3+4];
	public IPrediction16x16[] pred16x16 = new IPrediction16x16[4+3+2];
	public IPrediction4x4_add[] pred4x4_add = new IPrediction4x4_add[2];
	public IPrediction8x8L_add[] pred8x8l_add = new IPrediction8x8L_add[2];
	public IPrediction8x8_add[] pred8x8_add = new IPrediction8x8_add[3];
	public IPrediction16x16_add[] pred16x16_add = new IPrediction16x16_add[3];
	
	public void initializePredictionContext(int codec_id) {
		
		if(codec_id != CODEC_ID_RV40){
		    if(codec_id == CODEC_ID_VP8) {
		        pred4x4[VERT_PRED       ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_vertical_vp8_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[HOR_PRED        ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_horizontal_vp8_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    } else {
		        pred4x4[VERT_PRED       ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_vertical_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[HOR_PRED        ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_horizontal_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    }
		    pred4x4[DC_PRED             ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_dc_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    if(codec_id == CODEC_ID_SVQ3)
		        pred4x4[DIAG_DOWN_LEFT_PRED ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_down_left_svq3_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    else
		        pred4x4[DIAG_DOWN_LEFT_PRED ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_down_left_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    pred4x4[DIAG_DOWN_RIGHT_PRED] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_down_right_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[VERT_RIGHT_PRED     ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_vertical_right_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[HOR_DOWN_PRED       ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_horizontal_down_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    if (codec_id == CODEC_ID_VP8) {
		        pred4x4[VERT_LEFT_PRED  ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_vertical_left_vp8_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    } else
		        pred4x4[VERT_LEFT_PRED  ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_vertical_left_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    pred4x4[HOR_UP_PRED         ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_horizontal_up_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    if(codec_id != CODEC_ID_VP8) {
		        pred4x4[LEFT_DC_PRED    ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_left_dc_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[TOP_DC_PRED     ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_top_dc_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[DC_128_PRED     ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_128_dc_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    } else {
		        pred4x4[TM_VP8_PRED     ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_tm_vp8_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[DC_127_PRED     ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_127_dc_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[DC_129_PRED     ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_129_dc_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[VERT_VP8_PRED   ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_vertical_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		        pred4x4[HOR_VP8_PRED    ] = new IPrediction4x4() {
		        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
		        		pred4x4_horizontal_c(src, src_offset, topright, topright_offset, stride);
		        	}
		        };
		    }
		}else{
		    pred4x4[VERT_PRED           ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_vertical_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[HOR_PRED            ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_horizontal_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[DC_PRED             ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_dc_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[DIAG_DOWN_LEFT_PRED ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_down_left_rv40_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[DIAG_DOWN_RIGHT_PRED] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_down_right_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[VERT_RIGHT_PRED     ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_vertical_right_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[HOR_DOWN_PRED       ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_horizontal_down_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[VERT_LEFT_PRED      ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_vertical_left_rv40_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[HOR_UP_PRED         ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_horizontal_up_rv40_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[LEFT_DC_PRED        ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_left_dc_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[TOP_DC_PRED         ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_top_dc_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[DC_128_PRED         ] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_128_dc_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[DIAG_DOWN_LEFT_PRED_RV40_NODOWN] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_down_left_rv40_nodown_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[HOR_UP_PRED_RV40_NODOWN] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_horizontal_up_rv40_nodown_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		    pred4x4[VERT_LEFT_PRED_RV40_NODOWN] = new IPrediction4x4() {
	        	public void pred4x4(int[] src, int src_offset, int[] topright, int topright_offset, int stride) {
	        		pred4x4_vertical_left_rv40_nodown_c(src, src_offset, topright, topright_offset, stride);
	        	}
	        };
		}
		
		pred8x8l[VERT_PRED           ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_vertical_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[HOR_PRED            ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_horizontal_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[DC_PRED             ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_dc_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[DIAG_DOWN_LEFT_PRED ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_down_left_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[DIAG_DOWN_RIGHT_PRED] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_down_right_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[VERT_RIGHT_PRED     ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_vertical_right_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[HOR_DOWN_PRED       ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_horizontal_down_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[VERT_LEFT_PRED      ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_vertical_left_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[HOR_UP_PRED         ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_horizontal_up_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[LEFT_DC_PRED        ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_left_dc_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[TOP_DC_PRED         ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_top_dc_c(src, src_offset, topleft, topright, stride);
			}
		};
		pred8x8l[DC_128_PRED         ] = new IPrediction8x8L() {
			public void pred8x8l(int[] src, int src_offset, int topleft, int topright, int stride) {
				pred8x8l_128_dc_c(src, src_offset, topleft, topright, stride);
			}
		};
		
		pred8x8[VERT_PRED8x8   ] = new IPrediction8x8() {
			public void pred8x8(int[] src, int src_offset, int stride) {
				pred8x8_vertical_c(src, src_offset, stride);
			}
		};
		pred8x8[HOR_PRED8x8    ] = new IPrediction8x8() {
			public void pred8x8(int[] src, int src_offset, int stride) {
				pred8x8_horizontal_c(src, src_offset, stride);
			}
		};
		if (codec_id != CODEC_ID_VP8) {
		    pred8x8[PLANE_PRED8x8] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_plane_c(src, src_offset, stride);
				}
			};
		} else
		    pred8x8[PLANE_PRED8x8] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_tm_vp8_c(src, src_offset, stride);
				}
			};
		if(codec_id != CODEC_ID_RV40 && codec_id != CODEC_ID_VP8){
		    pred8x8[DC_PRED8x8     ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_dc_c(src, src_offset, stride);
				}
			};
		    pred8x8[LEFT_DC_PRED8x8] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_left_dc_c(src, src_offset, stride);
				}
			};
		    pred8x8[TOP_DC_PRED8x8 ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_top_dc_c(src, src_offset, stride);
				}
			};
		    pred8x8[ALZHEIMER_DC_L0T_PRED8x8 ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_mad_cow_dc_l0t(src, src_offset, stride);
				}
			};
		    pred8x8[ALZHEIMER_DC_0LT_PRED8x8 ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_mad_cow_dc_0lt(src, src_offset, stride);
				}
			};
		    pred8x8[ALZHEIMER_DC_L00_PRED8x8 ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_mad_cow_dc_l00(src, src_offset, stride);
				}
			};
		    pred8x8[ALZHEIMER_DC_0L0_PRED8x8 ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_mad_cow_dc_0l0(src, src_offset, stride);
				}
			};
		}else{
		    pred8x8[DC_PRED8x8     ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_dc_rv40_c(src, src_offset, stride);
				}
			};
		    pred8x8[LEFT_DC_PRED8x8] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_left_dc_rv40_c(src, src_offset, stride);
				}
			};
		    pred8x8[TOP_DC_PRED8x8 ] = new IPrediction8x8() {
				public void pred8x8(int[] src, int src_offset, int stride) {
					pred8x8_top_dc_rv40_c(src, src_offset, stride);
				}
			};
		    if (codec_id == CODEC_ID_VP8) {
		        pred8x8[DC_127_PRED8x8] = new IPrediction8x8() {
					public void pred8x8(int[] src, int src_offset, int stride) {
						pred8x8_127_dc_c(src, src_offset, stride);
					}
				};
		        pred8x8[DC_129_PRED8x8] = new IPrediction8x8() {
					public void pred8x8(int[] src, int src_offset, int stride) {
						pred8x8_129_dc_c(src, src_offset, stride);
					}
				};
		    }
		}
		pred8x8[DC_128_PRED8x8 ] = new IPrediction8x8() {
			public void pred8x8(int[] src, int src_offset, int stride) {
				pred8x8_128_dc_c(src, src_offset, stride);
			}
		};
		
		pred16x16[DC_PRED8x8     ] = new IPrediction16x16() {
			public void pred16x16(int[] src, int src_offset, int stride) {
				pred16x16_dc_c(src, src_offset, stride);
			}
		};
		pred16x16[VERT_PRED8x8   ] = new IPrediction16x16() {
			public void pred16x16(int[] src, int src_offset, int stride) {
				pred16x16_vertical_c(src, src_offset, stride);
			}
		};
		pred16x16[HOR_PRED8x8    ] = new IPrediction16x16() {
			public void pred16x16(int[] src, int src_offset, int stride) {
				pred16x16_horizontal_c(src, src_offset, stride);
			}
		};
		switch(codec_id){
			case CODEC_ID_SVQ3:
			   pred16x16[PLANE_PRED8x8  ] = new IPrediction16x16() {
					public void pred16x16(int[] src, int src_offset, int stride) {
						pred16x16_plane_svq3_c(src, src_offset, stride);
					}
				};
			   break;
			case CODEC_ID_RV40:
			   pred16x16[PLANE_PRED8x8  ] = new IPrediction16x16() {
					public void pred16x16(int[] src, int src_offset, int stride) {
						pred16x16_plane_rv40_c(src, src_offset, stride);
					}
				};
			   break;
			case CODEC_ID_VP8:
			   pred16x16[PLANE_PRED8x8  ] = new IPrediction16x16() {
					public void pred16x16(int[] src, int src_offset, int stride) {
						pred16x16_tm_vp8_c(src, src_offset, stride);
					}
				};
			   pred16x16[DC_127_PRED8x8] = new IPrediction16x16() {
					public void pred16x16(int[] src, int src_offset, int stride) {
						pred16x16_127_dc_c(src, src_offset, stride);
					}
				};
			   pred16x16[DC_129_PRED8x8] = new IPrediction16x16() {
					public void pred16x16(int[] src, int src_offset, int stride) {
						pred16x16_129_dc_c(src, src_offset, stride);
					}
				};
			   break;
			default:
			   pred16x16[PLANE_PRED8x8  ] = new IPrediction16x16() {
					public void pred16x16(int[] src, int src_offset, int stride) {
						pred16x16_plane_c(src, src_offset, stride);
					}
				};
			   break;
		}
		pred16x16[LEFT_DC_PRED8x8] = new IPrediction16x16() {
			public void pred16x16(int[] src, int src_offset, int stride) {
				pred16x16_left_dc_c(src, src_offset, stride);
			}
		};
		pred16x16[TOP_DC_PRED8x8 ] = new IPrediction16x16() {
			public void pred16x16(int[] src, int src_offset, int stride) {
				pred16x16_top_dc_c(src, src_offset, stride);
			}
		};
		pred16x16[DC_128_PRED8x8 ] = new IPrediction16x16() {
			public void pred16x16(int[] src, int src_offset, int stride) {
				pred16x16_128_dc_c(src, src_offset, stride);
			}
		};
		
		//special lossless h/v prediction for h264
		pred4x4_add  [VERT_PRED   ] = new IPrediction4x4_add() {
			public void pred4x4_add(int[] pix, int pix_offset/*align  4*/, short[] block/*align 16*/, int _block_offset, int stride) {
				pred4x4_vertical_add_c(pix, pix_offset, block, _block_offset, stride);
			}
		};
		pred4x4_add  [ HOR_PRED   ] = new IPrediction4x4_add() {
			public void pred4x4_add(int[] pix, int pix_offset/*align  4*/, short[] block/*align 16*/, int _block_offset, int stride) {
				pred4x4_horizontal_add_c(pix, pix_offset, block, _block_offset, stride);
			}
		};
		pred8x8l_add [VERT_PRED   ] = new IPrediction8x8L_add() {
			public void pred8x8l_add(int[] pix, int pix_offset/*align  8*/, short[] block/*align 16*/, int _block_offset, int stride) {
				pred8x8l_vertical_add_c(pix, pix_offset, block, _block_offset, stride);
			}
		};
		pred8x8l_add [ HOR_PRED   ] = new IPrediction8x8L_add() {
			public void pred8x8l_add(int[] pix, int pix_offset/*align  8*/, short[] block/*align 16*/, int _block_offset, int stride) {
				pred8x8l_horizontal_add_c(pix, pix_offset, block, _block_offset, stride);
			}
		};
		pred8x8_add  [VERT_PRED8x8] = new IPrediction8x8_add() {
			public void pred8x8_add(int[] pix, int pix_offset/*align  8*/, int[] block_offset, int block_offset_offset, short[] block/*align 16*/, int _block_offset, int stride) {
				pred8x8_vertical_add_c(pix, pix_offset, block_offset, block_offset_offset, block, _block_offset, stride);
			}
		};
		pred8x8_add  [ HOR_PRED8x8] = new IPrediction8x8_add() {
			public void pred8x8_add(int[] pix, int pix_offset/*align  8*/, int[] block_offset, int block_offset_offset, short[] block/*align 16*/, int _block_offset, int stride) {
				pred8x8_horizontal_add_c(pix, pix_offset, block_offset, block_offset_offset, block, _block_offset, stride);				
			}
		};
		pred16x16_add[VERT_PRED8x8] = new IPrediction16x16_add() {
			public void pred16x16_add(int[] pix, int pix_offset/*align  16*/, int[] block_offset, int block_offset_offset, short[] block/*align 16*/, int _block_offset, int stride) {
				pred16x16_vertical_add_c(pix, pix_offset, block_offset, block_offset_offset, block, _block_offset, stride);
			}
		};
		pred16x16_add[ HOR_PRED8x8] = new IPrediction16x16_add() {
			public void pred16x16_add(int[] pix, int pix_offset/*align  16*/, int[] block_offset, int block_offset_offset, short[] block/*align 16*/, int _block_offset, int stride) {
				pred16x16_horizontal_add_c(pix, pix_offset, block_offset, block_offset_offset, block, _block_offset, stride);
			}
		};
		
	}
	
	
	/*
	 * a0 a1 a2 a3
	 *  | |  |  |
	 *  V V  V  V
	 * a0 a1 a2 a3
	 * a0 a1 a2 a3 
	 * a0 a1 a2 a3 
	 * a0 a1 a2 a3 
	 */
	public static void pred4x4_vertical_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int a0 = src[src_offset - stride + 0];
	    int a1 = src[src_offset - stride + 1];
	    int a2 = src[src_offset - stride + 2];
	    int a3 = src[src_offset - stride + 3];

	    src[src_offset + 0*stride + 0] = a0;
	    src[src_offset + 0*stride + 1] = a1;
	    src[src_offset + 0*stride + 2] = a2;
	    src[src_offset + 0*stride + 3] = a3;

	    src[src_offset + 1*stride + 0] = a0;
	    src[src_offset + 1*stride + 1] = a1;
	    src[src_offset + 1*stride + 2] = a2;
	    src[src_offset + 1*stride + 3] = a3;

	    src[src_offset + 2*stride + 0] = a0;
	    src[src_offset + 2*stride + 1] = a1;
	    src[src_offset + 2*stride + 2] = a2;
	    src[src_offset + 2*stride + 3] = a3;

	    src[src_offset + 3*stride + 0] = a0;
	    src[src_offset + 3*stride + 1] = a1;
	    src[src_offset + 3*stride + 2] = a2;
	    src[src_offset + 3*stride + 3] = a3;
	}
	
	/*
	 * a0 -> a0 a0 a0 a0
	 * a1 -> a1 a1 a1 a1 
	 * a2 -> a2 a2 a2 a2 
	 * a3 -> a3 a3 a3 a3 
	 */
	public static void pred4x4_horizontal_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){

		int a = src[src_offset -1 + 0*stride + 0];	    
		src[src_offset + 0*stride + 0] = a;
		src[src_offset + 0*stride + 1] = a;
		src[src_offset + 0*stride + 2] = a;
		src[src_offset + 0*stride + 3] = a;

		a = src[src_offset -1 + 1*stride + 0];	    
		src[src_offset + 1*stride + 0] = a;
		src[src_offset + 1*stride + 1] = a;
		src[src_offset + 1*stride + 2] = a;
		src[src_offset + 1*stride + 3] = a;

		a = src[src_offset -1 + 2*stride + 0];	    
		src[src_offset + 2*stride + 0] = a;
		src[src_offset + 2*stride + 1] = a;
		src[src_offset + 2*stride + 2] = a;
		src[src_offset + 2*stride + 3] = a;

		a = src[src_offset -1 + 3*stride + 0];	    
		src[src_offset + 3*stride + 0] = a;
		src[src_offset + 3*stride + 1] = a;
		src[src_offset + 3*stride + 2] = a;
		src[src_offset + 3*stride + 3] = a;		
	}

	/*
	 * [dc = (SUM(X) + 4) / 8]
	 *   X  X  X  X
	 * X dc dc dc dc
	 * X dc dc dc dc
	 * X dc dc dc dc
	 * X dc dc dc dc
	 */
	public static void pred4x4_dc_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int dc= (  src[src_offset -stride] + src[src_offset +1-stride] + src[src_offset +2-stride] + src[src_offset +3-stride]
	                   + src[src_offset -1+0*stride] + src[src_offset -1+1*stride] + src[src_offset -1+2*stride] + src[src_offset -1+3*stride] + 4) >>3;

	    src[src_offset +0*stride + 0] = dc;
	    src[src_offset +1*stride + 0] = dc;
	    src[src_offset +2*stride + 0] = dc;
	    src[src_offset +3*stride + 0] = dc;

	    src[src_offset +0*stride + 1] = dc;
	    src[src_offset +1*stride + 1] = dc;
	    src[src_offset +2*stride + 1] = dc;
	    src[src_offset +3*stride + 1] = dc;

	    src[src_offset +0*stride + 2] = dc;
	    src[src_offset +1*stride + 2] = dc;
	    src[src_offset +2*stride + 2] = dc;
	    src[src_offset +3*stride + 2] = dc;

	    src[src_offset +0*stride + 3] = dc;
	    src[src_offset +1*stride + 3] = dc;
	    src[src_offset +2*stride + 3] = dc;
	    src[src_offset +3*stride + 3] = dc;	    
	}

	/*
	 * [dc = (SUM(X) + 2) / 4]
	 *  
	 * X dc dc dc dc
	 * X dc dc dc dc
	 * X dc dc dc dc
	 * X dc dc dc dc
	 */	
	public static void pred4x4_left_dc_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int dc= (  src[src_offset -1+0*stride] + src[src_offset -1+1*stride] + src[src_offset -1+2*stride] + src[src_offset -1+3*stride] + 2) >>2;

	    src[src_offset +0*stride + 0] = dc;
	    src[src_offset +1*stride + 0] = dc;
	    src[src_offset +2*stride + 0] = dc;
	    src[src_offset +3*stride + 0] = dc;

	    src[src_offset +0*stride + 1] = dc;
	    src[src_offset +1*stride + 1] = dc;
	    src[src_offset +2*stride + 1] = dc;
	    src[src_offset +3*stride + 1] = dc;

	    src[src_offset +0*stride + 2] = dc;
	    src[src_offset +1*stride + 2] = dc;
	    src[src_offset +2*stride + 2] = dc;
	    src[src_offset +3*stride + 2] = dc;

	    src[src_offset +0*stride + 3] = dc;
	    src[src_offset +1*stride + 3] = dc;
	    src[src_offset +2*stride + 3] = dc;
	    src[src_offset +3*stride + 3] = dc;	    
	}

	/*
	 * [dc = (SUM(X) + 2) / 4]
	 *   X  X  X  X
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 */	
	public static void pred4x4_top_dc_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int dc= ( src[src_offset -stride] + src[src_offset +1-stride] + src[src_offset +2-stride] + src[src_offset +3-stride] + 2) >>2;

	    src[src_offset +0*stride + 0] = dc;
	    src[src_offset +1*stride + 0] = dc;
	    src[src_offset +2*stride + 0] = dc;
	    src[src_offset +3*stride + 0] = dc;

	    src[src_offset +0*stride + 1] = dc;
	    src[src_offset +1*stride + 1] = dc;
	    src[src_offset +2*stride + 1] = dc;
	    src[src_offset +3*stride + 1] = dc;

	    src[src_offset +0*stride + 2] = dc;
	    src[src_offset +1*stride + 2] = dc;
	    src[src_offset +2*stride + 2] = dc;
	    src[src_offset +3*stride + 2] = dc;

	    src[src_offset +0*stride + 3] = dc;
	    src[src_offset +1*stride + 3] = dc;
	    src[src_offset +2*stride + 3] = dc;
	    src[src_offset +3*stride + 3] = dc;	    
	}

	/*
	 * [dc = 128]
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 */	
	public static void pred4x4_128_dc_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int dc = 128;

	    src[src_offset +0*stride + 0] = dc;
	    src[src_offset +1*stride + 0] = dc;
	    src[src_offset +2*stride + 0] = dc;
	    src[src_offset +3*stride + 0] = dc;

	    src[src_offset +0*stride + 1] = dc;
	    src[src_offset +1*stride + 1] = dc;
	    src[src_offset +2*stride + 1] = dc;
	    src[src_offset +3*stride + 1] = dc;

	    src[src_offset +0*stride + 2] = dc;
	    src[src_offset +1*stride + 2] = dc;
	    src[src_offset +2*stride + 2] = dc;
	    src[src_offset +3*stride + 2] = dc;

	    src[src_offset +0*stride + 3] = dc;
	    src[src_offset +1*stride + 3] = dc;
	    src[src_offset +2*stride + 3] = dc;
	    src[src_offset +3*stride + 3] = dc;	    
	}

	/*
	 * [dc = 127]
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 */	
	public static void pred4x4_127_dc_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int dc = 127;

	    src[src_offset +0*stride + 0] = dc;
	    src[src_offset +1*stride + 0] = dc;
	    src[src_offset +2*stride + 0] = dc;
	    src[src_offset +3*stride + 0] = dc;

	    src[src_offset +0*stride + 1] = dc;
	    src[src_offset +1*stride + 1] = dc;
	    src[src_offset +2*stride + 1] = dc;
	    src[src_offset +3*stride + 1] = dc;

	    src[src_offset +0*stride + 2] = dc;
	    src[src_offset +1*stride + 2] = dc;
	    src[src_offset +2*stride + 2] = dc;
	    src[src_offset +3*stride + 2] = dc;

	    src[src_offset +0*stride + 3] = dc;
	    src[src_offset +1*stride + 3] = dc;
	    src[src_offset +2*stride + 3] = dc;
	    src[src_offset +3*stride + 3] = dc;	    
	}

	/*
	 * [dc = 129]
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 *   dc dc dc dc
	 */	
	public static void pred4x4_129_dc_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int dc = 129;

	    src[src_offset +0*stride + 0] = 
	    src[src_offset +1*stride + 0] = 
	    src[src_offset +2*stride + 0] = 
	    src[src_offset +3*stride + 0] = 

	    src[src_offset +0*stride + 1] = 
	    src[src_offset +1*stride + 1] = 
	    src[src_offset +2*stride + 1] = 
	    src[src_offset +3*stride + 1] = 

	    src[src_offset +0*stride + 2] = 
	    src[src_offset +1*stride + 2] = 
	    src[src_offset +2*stride + 2] = 
	    src[src_offset +3*stride + 2] = 

	    src[src_offset +0*stride + 3] = 
	    src[src_offset +1*stride + 3] = 
	    src[src_offset +2*stride + 3] = 
	    src[src_offset +3*stride + 3] = dc;	    
	}
	
	public static void pred4x4_vertical_vp8_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int lt= src[src_offset -1-1*stride];

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];
	    
	    int t4= topright[topright_offset + 0];
	    //int t5= topright[topright_offset + 1];
	    //int t6= topright[topright_offset + 2];
	    //int t7= topright[topright_offset + 3];
	    
	    src[src_offset +0*stride + 0] = (lt + 2*t0 + t1 + 2) >> 2;
	    src[src_offset +0*stride + 1] = (t0 + 2*t1 + t2 + 2) >> 2;
	    src[src_offset +0*stride + 2] = (t1 + 2*t2 + t3 + 2) >> 2;
	    src[src_offset +0*stride + 3] = (t2 + 2*t3 + t4 + 2) >> 2;

	    src[src_offset +1*stride + 0] = (lt + 2*t0 + t1 + 2) >> 2;
	    src[src_offset +1*stride + 1] = (t0 + 2*t1 + t2 + 2) >> 2;
	    src[src_offset +1*stride + 2] = (t1 + 2*t2 + t3 + 2) >> 2;
	    src[src_offset +1*stride + 3] = (t2 + 2*t3 + t4 + 2) >> 2;

	    src[src_offset +2*stride + 0] = (lt + 2*t0 + t1 + 2) >> 2;
	    src[src_offset +2*stride + 1] = (t0 + 2*t1 + t2 + 2) >> 2;
	    src[src_offset +2*stride + 2] = (t1 + 2*t2 + t3 + 2) >> 2;
	    src[src_offset +2*stride + 3] = (t2 + 2*t3 + t4 + 2) >> 2;

	    src[src_offset +3*stride + 0] = (lt + 2*t0 + t1 + 2) >> 2;
	    src[src_offset +3*stride + 1] = (t0 + 2*t1 + t2 + 2) >> 2;
	    src[src_offset +3*stride + 2] = (t1 + 2*t2 + t3 + 2) >> 2;
	    src[src_offset +3*stride + 3] = (t2 + 2*t3 + t4 + 2) >> 2;	    
	}

	public static void pred4x4_horizontal_vp8_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int lt= src[src_offset -1-1*stride];

	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

		    src[src_offset +0*stride + 0] = 
		    src[src_offset +0*stride + 1] = 
		    src[src_offset +0*stride + 2] = 
		    src[src_offset +0*stride + 3] = (lt + 2*l0 + l1 + 2) >> 2;

		    src[src_offset +1*stride + 0] = 
		    src[src_offset +1*stride + 1] = 
		    src[src_offset +1*stride + 2] = 
		    src[src_offset +1*stride + 3] = (l0 + 2*l1 + l2 + 2) >> 2;

		    src[src_offset +2*stride + 0] = 
		    src[src_offset +2*stride + 1] = 
		    src[src_offset +2*stride + 2] = 
		    src[src_offset +2*stride + 3] = (l1 + 2*l2 + l3 + 2) >> 2;

		    src[src_offset +3*stride + 0] = 
		    src[src_offset +3*stride + 1] = 
		    src[src_offset +3*stride + 2] = 
		    src[src_offset +3*stride + 3] = (l2 + 2*l3 + l3 + 2) >> 2;		    
	}

	public static void pred4x4_down_right_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int lt= src[src_offset -1-1*stride];

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];
	    
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];
	    
	    src[src_offset +0+3*stride]=(l3 + 2*l2 + l1 + 2)>>2;
	    src[src_offset +0+2*stride]=
	    src[src_offset +1+3*stride]=(l2 + 2*l1 + l0 + 2)>>2;
	    src[src_offset +0+1*stride]=
	    src[src_offset +1+2*stride]=
	    src[src_offset +2+3*stride]=(l1 + 2*l0 + lt + 2)>>2;
	    src[src_offset +0+0*stride]=
	    src[src_offset +1+1*stride]=
	    src[src_offset +2+2*stride]=
	    src[src_offset +3+3*stride]=(l0 + 2*lt + t0 + 2)>>2;
	    src[src_offset +1+0*stride]=
	    src[src_offset +2+1*stride]=
	    src[src_offset +3+2*stride]=(lt + 2*t0 + t1 + 2)>>2;
	    src[src_offset +2+0*stride]=
	    src[src_offset +3+1*stride]=(t0 + 2*t1 + t2 + 2)>>2;
	    src[src_offset +3+0*stride]=(t1 + 2*t2 + t3 + 2)>>2;
	}

	public static void pred4x4_down_left_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){

 		 int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];
	    
	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    int t7= topright[topright_offset + 3];
	    
	    //	    LOAD_LEFT_EDGE

	    src[src_offset +0+0*stride]=(t0 + t2 + 2*t1 + 2)>>2;
	    src[src_offset +1+0*stride]=
	    src[src_offset +0+1*stride]=(t1 + t3 + 2*t2 + 2)>>2;
	    src[src_offset +2+0*stride]=
	    src[src_offset +1+1*stride]=
	    src[src_offset +0+2*stride]=(t2 + t4 + 2*t3 + 2)>>2;
	    src[src_offset +3+0*stride]=
	    src[src_offset +2+1*stride]=
	    src[src_offset +1+2*stride]=
	    src[src_offset +0+3*stride]=(t3 + t5 + 2*t4 + 2)>>2;
	    src[src_offset +3+1*stride]=
	    src[src_offset +2+2*stride]=
	    src[src_offset +1+3*stride]=(t4 + t6 + 2*t5 + 2)>>2;
	    src[src_offset +3+2*stride]=
	    src[src_offset +2+3*stride]=(t5 + t7 + 2*t6 + 2)>>2;
	    src[src_offset +3+3*stride]=(t6 + 3*t7 + 2)>>2;
	}

	public static void pred4x4_down_left_svq3_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){

		 //int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	     //int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	    src[src_offset +0+0*stride]=(l1 + t1)>>1;
	    src[src_offset +1+0*stride]=
	    src[src_offset +0+1*stride]=(l2 + t2)>>1;
	    src[src_offset +2+0*stride]=
	    src[src_offset +1+1*stride]=
	    src[src_offset +0+2*stride]=
	    src[src_offset +3+0*stride]=
	    src[src_offset +2+1*stride]=
	    src[src_offset +1+2*stride]=
	    src[src_offset +0+3*stride]=
	    src[src_offset +3+1*stride]=
	    src[src_offset +2+2*stride]=
	    src[src_offset +1+3*stride]=
	    src[src_offset +3+2*stride]=
	    src[src_offset +2+3*stride]=
	    src[src_offset +3+3*stride]=(l3 + t3)>>1;
	}

	public static void pred4x4_down_left_rv40_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];
	    
	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    int t7= topright[topright_offset + 3];
	    
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	     int  l4= src[src_offset -1+4*stride];
	     int  l5= src[src_offset -1+5*stride];
	     int  l6= src[src_offset -1+6*stride];
	     int  l7= src[src_offset -1+7*stride];

	    src[src_offset +0+0*stride]=(t0 + t2 + 2*t1 + 2 + l0 + l2 + 2*l1 + 2)>>3;
	    src[src_offset +1+0*stride]=
	    src[src_offset +0+1*stride]=(t1 + t3 + 2*t2 + 2 + l1 + l3 + 2*l2 + 2)>>3;
	    src[src_offset +2+0*stride]=
	    src[src_offset +1+1*stride]=
	    src[src_offset +0+2*stride]=(t2 + t4 + 2*t3 + 2 + l2 + l4 + 2*l3 + 2)>>3;
	    src[src_offset +3+0*stride]=
	    src[src_offset +2+1*stride]=
	    src[src_offset +1+2*stride]=
	    src[src_offset +0+3*stride]=(t3 + t5 + 2*t4 + 2 + l3 + l5 + 2*l4 + 2)>>3;
	    src[src_offset +3+1*stride]=
	    src[src_offset +2+2*stride]=
	    src[src_offset +1+3*stride]=(t4 + t6 + 2*t5 + 2 + l4 + l6 + 2*l5 + 2)>>3;
	    src[src_offset +3+2*stride]=
	    src[src_offset +2+3*stride]=(t5 + t7 + 2*t6 + 2 + l5 + l7 + 2*l6 + 2)>>3;
	    src[src_offset +3+3*stride]=(t6 + t7 + 1 + l6 + l7 + 1)>>2;
	}

	public static void pred4x4_down_left_rv40_nodown_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    int t7= topright[topright_offset + 3];
	    
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	    src[src_offset +0+0*stride]=(t0 + t2 + 2*t1 + 2 + l0 + l2 + 2*l1 + 2)>>3;
	    src[src_offset +1+0*stride]=
	    src[src_offset +0+1*stride]=(t1 + t3 + 2*t2 + 2 + l1 + l3 + 2*l2 + 2)>>3;
	    src[src_offset +2+0*stride]=
	    src[src_offset +1+1*stride]=
	    src[src_offset +0+2*stride]=(t2 + t4 + 2*t3 + 2 + l2 + 3*l3 + 2)>>3;
	    src[src_offset +3+0*stride]=
	    src[src_offset +2+1*stride]=
	    src[src_offset +1+2*stride]=
	    src[src_offset +0+3*stride]=(t3 + t5 + 2*t4 + 2 + l3*4 + 2)>>3;
	    src[src_offset +3+1*stride]=
	    src[src_offset +2+2*stride]=
	    src[src_offset +1+3*stride]=(t4 + t6 + 2*t5 + 2 + l3*4 + 2)>>3;
	    src[src_offset +3+2*stride]=
	    src[src_offset +2+3*stride]=(t5 + t7 + 2*t6 + 2 + l3*4 + 2)>>3;
	    src[src_offset +3+3*stride]=(t6 + t7 + 1 + 2*l3 + 1)>>2;
	}

	public static void pred4x4_vertical_right_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int lt= src[src_offset -1-1*stride];

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     //int  l3= src[src_offset -1+3*stride];

	    src[src_offset +0+0*stride]=
	    src[src_offset +1+2*stride]=(lt + t0 + 1)>>1;
	    src[src_offset +1+0*stride]=
	    src[src_offset +2+2*stride]=(t0 + t1 + 1)>>1;
	    src[src_offset +2+0*stride]=
	    src[src_offset +3+2*stride]=(t1 + t2 + 1)>>1;
	    src[src_offset +3+0*stride]=(t2 + t3 + 1)>>1;
	    src[src_offset +0+1*stride]=
	    src[src_offset +1+3*stride]=(l0 + 2*lt + t0 + 2)>>2;
	    src[src_offset +1+1*stride]=
	    src[src_offset +2+3*stride]=(lt + 2*t0 + t1 + 2)>>2;
	    src[src_offset +2+1*stride]=
	    src[src_offset +3+3*stride]=(t0 + 2*t1 + t2 + 2)>>2;
	    src[src_offset +3+1*stride]=(t1 + 2*t2 + t3 + 2)>>2;
	    src[src_offset +0+2*stride]=(lt + 2*l0 + l1 + 2)>>2;
	    src[src_offset +0+3*stride]=(l0 + 2*l1 + l2 + 2)>>2;
	}

	public static void pred4x4_vertical_left_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    //int t7= topright[topright_offset + 3];
	    
	    src[src_offset +0+0*stride]=(t0 + t1 + 1)>>1;
	    src[src_offset +1+0*stride]=
	    src[src_offset +0+2*stride]=(t1 + t2 + 1)>>1;
	    src[src_offset +2+0*stride]=
	    src[src_offset +1+2*stride]=(t2 + t3 + 1)>>1;
	    src[src_offset +3+0*stride]=
	    src[src_offset +2+2*stride]=(t3 + t4+ 1)>>1;
	    src[src_offset +3+2*stride]=(t4 + t5+ 1)>>1;
	    src[src_offset +0+1*stride]=(t0 + 2*t1 + t2 + 2)>>2;
	    src[src_offset +1+1*stride]=
	    src[src_offset +0+3*stride]=(t1 + 2*t2 + t3 + 2)>>2;
	    src[src_offset +2+1*stride]=
	    src[src_offset +1+3*stride]=(t2 + 2*t3 + t4 + 2)>>2;
	    src[src_offset +3+1*stride]=
	    src[src_offset +2+3*stride]=(t3 + 2*t4 + t5 + 2)>>2;
	    src[src_offset +3+3*stride]=(t4 + 2*t5 + t6 + 2)>>2;
	}

	public static void pred4x4_vertical_left_rv40(int[] src, int src_offset, int[] topright, int topright_offset, int stride,
	                                      int l0, int l1, int l2, int l3, int l4){

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    //int t7= topright[topright_offset + 3];
	    
	    src[src_offset +0+0*stride]=(2*t0 + 2*t1 + l1 + 2*l2 + l3 + 4)>>3;
	    src[src_offset +1+0*stride]=
	    src[src_offset +0+2*stride]=(t1 + t2 + 1)>>1;
	    src[src_offset +2+0*stride]=
	    src[src_offset +1+2*stride]=(t2 + t3 + 1)>>1;
	    src[src_offset +3+0*stride]=
	    src[src_offset +2+2*stride]=(t3 + t4+ 1)>>1;
	    src[src_offset +3+2*stride]=(t4 + t5+ 1)>>1;
	    src[src_offset +0+1*stride]=(t0 + 2*t1 + t2 + l2 + 2*l3 + l4 + 4)>>3;
	    src[src_offset +1+1*stride]=
	    src[src_offset +0+3*stride]=(t1 + 2*t2 + t3 + 2)>>2;
	    src[src_offset +2+1*stride]=
	    src[src_offset +1+3*stride]=(t2 + 2*t3 + t4 + 2)>>2;
	    src[src_offset +3+1*stride]=
	    src[src_offset +2+3*stride]=(t3 + 2*t4 + t5 + 2)>>2;
	    src[src_offset +3+3*stride]=(t4 + 2*t5 + t6 + 2)>>2;
	}

	public static void pred4x4_vertical_left_rv40_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	     int  l4= src[src_offset -1+4*stride];
	     //int  l5= src[src_offset -1+5*stride];
	     //int  l6= src[src_offset -1+6*stride];
	     //int  l7= src[src_offset -1+7*stride];

	    pred4x4_vertical_left_rv40(src, src_offset, topright, topright_offset, stride, l0, l1, l2, l3, l4);
	}

	public static void pred4x4_vertical_left_rv40_nodown_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	    pred4x4_vertical_left_rv40(src, src_offset, topright, topright_offset, stride, l0, l1, l2, l3, l3);
	}

	public static void pred4x4_vertical_left_vp8_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    int t7= topright[topright_offset + 3];
	    
	    src[src_offset +0+0*stride]=(t0 + t1 + 1)>>1;
	    src[src_offset +1+0*stride]=
	    src[src_offset +0+2*stride]=(t1 + t2 + 1)>>1;
	    src[src_offset +2+0*stride]=
	    src[src_offset +1+2*stride]=(t2 + t3 + 1)>>1;
	    src[src_offset +3+0*stride]=
	    src[src_offset +2+2*stride]=(t3 + t4 + 1)>>1;
	    src[src_offset +0+1*stride]=(t0 + 2*t1 + t2 + 2)>>2;
	    src[src_offset +1+1*stride]=
	    src[src_offset +0+3*stride]=(t1 + 2*t2 + t3 + 2)>>2;
	    src[src_offset +2+1*stride]=
	    src[src_offset +1+3*stride]=(t2 + 2*t3 + t4 + 2)>>2;
	    src[src_offset +3+1*stride]=
	    src[src_offset +2+3*stride]=(t3 + 2*t4 + t5 + 2)>>2;
	    src[src_offset +3+2*stride]=(t4 + 2*t5 + t6 + 2)>>2;
	    src[src_offset +3+3*stride]=(t5 + 2*t6 + t7 + 2)>>2;
	}

	public static void pred4x4_horizontal_up_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	    src[src_offset +0+0*stride]=(l0 + l1 + 1)>>1;
	    src[src_offset +1+0*stride]=(l0 + 2*l1 + l2 + 2)>>2;
	    src[src_offset +2+0*stride]=
	    src[src_offset +0+1*stride]=(l1 + l2 + 1)>>1;
	    src[src_offset +3+0*stride]=
	    src[src_offset +1+1*stride]=(l1 + 2*l2 + l3 + 2)>>2;
	    src[src_offset +2+1*stride]=
	    src[src_offset +0+2*stride]=(l2 + l3 + 1)>>1;
	    src[src_offset +3+1*stride]=
	    src[src_offset +1+2*stride]=(l2 + 2*l3 + l3 + 2)>>2;
	    src[src_offset +3+2*stride]=
	    src[src_offset +1+3*stride]=
	    src[src_offset +0+3*stride]=
	    src[src_offset +2+2*stride]=
	    src[src_offset +2+3*stride]=
	    src[src_offset +3+3*stride]=l3;
	}

	public static void pred4x4_horizontal_up_rv40_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	     int  l4= src[src_offset -1+4*stride];
	     int  l5= src[src_offset -1+5*stride];
	     int  l6= src[src_offset -1+6*stride];
	     //int  l7= src[src_offset -1+7*stride];

	     //int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    int t7= topright[topright_offset + 3];
	    
	    src[src_offset +0+0*stride]=(t1 + 2*t2 + t3 + 2*l0 + 2*l1 + 4)>>3;
	    src[src_offset +1+0*stride]=(t2 + 2*t3 + t4 + l0 + 2*l1 + l2 + 4)>>3;
	    src[src_offset +2+0*stride]=
	    src[src_offset +0+1*stride]=(t3 + 2*t4 + t5 + 2*l1 + 2*l2 + 4)>>3;
	    src[src_offset +3+0*stride]=
	    src[src_offset +1+1*stride]=(t4 + 2*t5 + t6 + l1 + 2*l2 + l3 + 4)>>3;
	    src[src_offset +2+1*stride]=
	    src[src_offset +0+2*stride]=(t5 + 2*t6 + t7 + 2*l2 + 2*l3 + 4)>>3;
	    src[src_offset +3+1*stride]=
	    src[src_offset +1+2*stride]=(t6 + 3*t7 + l2 + 3*l3 + 4)>>3;
	    src[src_offset +3+2*stride]=
	    src[src_offset +1+3*stride]=(l3 + 2*l4 + l5 + 2)>>2;
	    src[src_offset +0+3*stride]=
	    src[src_offset +2+2*stride]=(t6 + t7 + l3 + l4 + 2)>>2;
	    src[src_offset +2+3*stride]=(l4 + l5 + 1)>>1;
	    src[src_offset +3+3*stride]=(l4 + 2*l5 + l6 + 2)>>2;
	}

	public static void pred4x4_horizontal_up_rv40_nodown_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	     //int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     int  t3= src[src_offset +3-1*stride];

	    int t4= topright[topright_offset + 0];
	    int t5= topright[topright_offset + 1];
	    int t6= topright[topright_offset + 2];
	    int t7= topright[topright_offset + 3];
	    
	    src[src_offset +0+0*stride]=(t1 + 2*t2 + t3 + 2*l0 + 2*l1 + 4)>>3;
	    src[src_offset +1+0*stride]=(t2 + 2*t3 + t4 + l0 + 2*l1 + l2 + 4)>>3;
	    src[src_offset +2+0*stride]=
	    src[src_offset +0+1*stride]=(t3 + 2*t4 + t5 + 2*l1 + 2*l2 + 4)>>3;
	    src[src_offset +3+0*stride]=
	    src[src_offset +1+1*stride]=(t4 + 2*t5 + t6 + l1 + 2*l2 + l3 + 4)>>3;
	    src[src_offset +2+1*stride]=
	    src[src_offset +0+2*stride]=(t5 + 2*t6 + t7 + 2*l2 + 2*l3 + 4)>>3;
	    src[src_offset +3+1*stride]=
	    src[src_offset +1+2*stride]=(t6 + 3*t7 + l2 + 3*l3 + 4)>>3;
	    src[src_offset +3+2*stride]=
	    src[src_offset +1+3*stride]=l3;
	    src[src_offset +0+3*stride]=
	    src[src_offset +2+2*stride]=(t6 + t7 + 2*l3 + 2)>>2;
	    src[src_offset +2+3*stride]=
	    src[src_offset +3+3*stride]=l3;
	}

	public static void pred4x4_horizontal_down_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int lt= src[src_offset -1-1*stride];

	     int  t0= src[src_offset +0-1*stride];
	     int  t1= src[src_offset +1-1*stride];
	     int  t2= src[src_offset +2-1*stride];
	     //int  t3= src[src_offset +3-1*stride];

	     int  l0= src[src_offset -1+0*stride];
	     int  l1= src[src_offset -1+1*stride];
	     int  l2= src[src_offset -1+2*stride];
	     int  l3= src[src_offset -1+3*stride];

	    src[src_offset +0+0*stride]=
	    src[src_offset +2+1*stride]=(lt + l0 + 1)>>1;
	    src[src_offset +1+0*stride]=
	    src[src_offset +3+1*stride]=(l0 + 2*lt + t0 + 2)>>2;
	    src[src_offset +2+0*stride]=(lt + 2*t0 + t1 + 2)>>2;
	    src[src_offset +3+0*stride]=(t0 + 2*t1 + t2 + 2)>>2;
	    src[src_offset +0+1*stride]=
	    src[src_offset +2+2*stride]=(l0 + l1 + 1)>>1;
	    src[src_offset +1+1*stride]=
	    src[src_offset +3+2*stride]=(lt + 2*l0 + l1 + 2)>>2;
	    src[src_offset +0+2*stride]=
	    src[src_offset +2+3*stride]=(l1 + l2+ 1)>>1;
	    src[src_offset +1+2*stride]=
	    src[src_offset +3+3*stride]=(l0 + 2*l1 + l2 + 2)>>2;
	    src[src_offset +0+3*stride]=(l2 + l3 + 1)>>1;
	    src[src_offset +1+3*stride]=(l1 + 2*l2 + l3 + 2)>>2;
	}

	public static void pred4x4_tm_vp8_c(int[] src, int src_offset, int[] topright, int topright_offset, int stride){
	    int cm_offset = H264DSPContext.MAX_NEG_CROP - src[src_offset -1-stride];
	    int top_offset = src_offset -stride;
	    int y;

	    for (y = 0; y < 4; y++) {
	        int cm_in_offset = cm_offset + src[src_offset -1];
	        src[src_offset +0] = H264DSPContext.ff_cropTbl[ cm_in_offset + src[top_offset +0] ];
	        src[src_offset +1] = H264DSPContext.ff_cropTbl[ cm_in_offset + src[top_offset +1] ];
	        src[src_offset +2] = H264DSPContext.ff_cropTbl[ cm_in_offset + src[top_offset +2] ];
	        src[src_offset +3] = H264DSPContext.ff_cropTbl[ cm_in_offset + src[top_offset +3] ];
	        src_offset += stride;
	    }
	}

	public static void pred16x16_vertical_c(int[] src, int src_offset, int stride){
	    int i;
	    
	    int a0 = src[src_offset -stride + 0];
	    int a1 = src[src_offset -stride + 1];
	    int a2 = src[src_offset -stride + 2];
	    int a3 = src[src_offset -stride + 3];
	    
	    int b0 = src[src_offset -stride + 4];
	    int b1 = src[src_offset -stride + 5];
	    int b2 = src[src_offset -stride + 6];
	    int b3 = src[src_offset -stride + 7];

	    int c0 = src[src_offset -stride + 8];
	    int c1 = src[src_offset -stride + 9];
	    int c2 = src[src_offset -stride + 10];
	    int c3 = src[src_offset -stride + 11];

	    int d0 = src[src_offset -stride + 12];
	    int d1 = src[src_offset -stride + 13];
	    int d2 = src[src_offset -stride + 14];
	    int d3 = src[src_offset -stride + 15];

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = a0;
	    	src[src_offset +i*stride + 1] = a1;
	    	src[src_offset +i*stride + 2] = a2;
	    	src[src_offset +i*stride + 3] = a3;

	    	src[src_offset +i*stride + 4] = b0;
	    	src[src_offset +i*stride + 5] = b1;
	    	src[src_offset +i*stride + 6] = b2;
	    	src[src_offset +i*stride + 7] = b3;

	    	src[src_offset +i*stride + 8] = c0;
	    	src[src_offset +i*stride + 9] = c1;
	    	src[src_offset +i*stride + 10] = c2;
	    	src[src_offset +i*stride + 11] = c3;
	    	
	    	src[src_offset +i*stride + 12] = d0;
	    	src[src_offset +i*stride + 13] = d1;
	    	src[src_offset +i*stride + 14] = d2;
	    	src[src_offset +i*stride + 15] = d3;
	    }
	}

	public static void pred16x16_horizontal_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = 
	    	src[src_offset +i*stride + 1] = 
	    	src[src_offset +i*stride + 2] =
	    	src[src_offset +i*stride + 3] = 
	    	src[src_offset +i*stride + 4] = 
	    	src[src_offset +i*stride + 5] = 
	    	src[src_offset +i*stride + 6] = 
	    	src[src_offset +i*stride + 7] = 
	    	src[src_offset +i*stride + 8] = 
	    	src[src_offset +i*stride + 9] = 
	    	src[src_offset +i*stride + 10] = 
	    	src[src_offset +i*stride + 11] = 
	    	src[src_offset +i*stride + 12] = 
	    	src[src_offset +i*stride + 13] = 
	    	src[src_offset +i*stride + 14] = 
	    	src[src_offset +i*stride + 15] = src[src_offset -1+i*stride];
	    }
	}

	public static void pred16x16_dc_c(int[] src, int src_offset, int stride){
	    int i, dc=0;

	    for(i=0;i<16; i++){
	        dc+= src[src_offset -1+i*stride];
	    }

	    for(i=0;i<16; i++){
	        dc+= src[src_offset +i-stride];
	    }

	    dc = ((dc + 16)>>5);

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] =
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 
		    	src[src_offset +i*stride + 8] = 
		    	src[src_offset +i*stride + 9] = 
		    	src[src_offset +i*stride + 10] = 
		    	src[src_offset +i*stride + 11] = 
		    	src[src_offset +i*stride + 12] = 
		    	src[src_offset +i*stride + 13] = 
		    	src[src_offset +i*stride + 14] = 
		    	src[src_offset +i*stride + 15] = dc;
	    }
	}

	public static void pred16x16_left_dc_c(int[] src, int src_offset, int stride){
	    int i, dc=0;

	    for(i=0;i<16; i++){
		    // DebugTool.printDebugString("    **** dc += "+src[src_offset -1+i*stride]+" ["+(src_offset-1+i*stride)+"]\n");
	    	dc+= src[src_offset -1+i*stride];
	    }

	    dc = ((dc + 8)>>4);
	    // DebugTool.printDebugString("    **** avg dc = "+dc+"\n");

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] =
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 
		    	src[src_offset +i*stride + 8] = 
		    	src[src_offset +i*stride + 9] = 
		    	src[src_offset +i*stride + 10] = 
		    	src[src_offset +i*stride + 11] = 
		    	src[src_offset +i*stride + 12] = 
		    	src[src_offset +i*stride + 13] = 
		    	src[src_offset +i*stride + 14] = 
		    	src[src_offset +i*stride + 15] = dc;
	    }
	}

	public static void pred16x16_top_dc_c(int[] src, int src_offset, int stride){
	    int i, dc=0;

	    for(i=0;i<16; i++){
	        dc+= src[src_offset +i-stride];
	    }
	    dc = ((dc + 8)>>4);

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] =
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 
		    	src[src_offset +i*stride + 8] = 
		    	src[src_offset +i*stride + 9] = 
		    	src[src_offset +i*stride + 10] = 
		    	src[src_offset +i*stride + 11] = 
		    	src[src_offset +i*stride + 12] = 
		    	src[src_offset +i*stride + 13] = 
		    	src[src_offset +i*stride + 14] = 
		    	src[src_offset +i*stride + 15] = dc;
	    }
	}

	public static void pred16x16_128_dc_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] =
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 
		    	src[src_offset +i*stride + 8] = 
		    	src[src_offset +i*stride + 9] = 
		    	src[src_offset +i*stride + 10] = 
		    	src[src_offset +i*stride + 11] = 
		    	src[src_offset +i*stride + 12] = 
		    	src[src_offset +i*stride + 13] = 
		    	src[src_offset +i*stride + 14] = 
		    	src[src_offset +i*stride + 15] = 128;
	    }
	}

	public static void pred16x16_127_dc_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] =
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 
		    	src[src_offset +i*stride + 8] = 
		    	src[src_offset +i*stride + 9] = 
		    	src[src_offset +i*stride + 10] = 
		    	src[src_offset +i*stride + 11] = 
		    	src[src_offset +i*stride + 12] = 
		    	src[src_offset +i*stride + 13] = 
		    	src[src_offset +i*stride + 14] = 
		    	src[src_offset +i*stride + 15] = 127;
	    }
	}

	public static void pred16x16_129_dc_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<16; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] =
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 
		    	src[src_offset +i*stride + 8] = 
		    	src[src_offset +i*stride + 9] = 
		    	src[src_offset +i*stride + 10] = 
		    	src[src_offset +i*stride + 11] = 
		    	src[src_offset +i*stride + 12] = 
		    	src[src_offset +i*stride + 13] = 
		    	src[src_offset +i*stride + 14] = 
		    	src[src_offset +i*stride + 15] = 129;
	    }
	}

	public static void pred16x16_plane_compat_c(int[] src, int src_offset, int stride, int svq3, int rv40){
	  int i, j, k;
	  int a;
	  int cm_offset = H264DSPContext.MAX_NEG_CROP;
	  int src0_offset = src_offset +7-stride;
	  int src1_offset = src_offset +8*stride-1;
	  int src2_offset = src1_offset -2*stride;      // == src+6*stride-1;
	  int H = src[src0_offset + 1] - src[src0_offset - 1];
	  int V = src[src1_offset + 0] - src[src2_offset + 0];
	  for(k=2; k<=8; ++k) {
		  src1_offset += stride; src2_offset -= stride;
	    H += k*(src[src0_offset + k] - src[src0_offset - k]);
	    V += k*(src[src1_offset + 0] - src[src2_offset + 0]);
	  }
	  if(svq3 != 0 ){
	    H = ( 5*(H/4) ) / 16;
	    V = ( 5*(V/4) ) / 16;

	    /* required for 100% accuracy */
	    i = H; H = V; V = i;
	  }else if(rv40 != 0){
	    H = ( H + (H>>2) ) >> 4;
	    V = ( V + (V>>2) ) >> 4;
	  }else{
	    H = ( 5*H+32 ) >> 6;
	    V = ( 5*V+32 ) >> 6;
	  }

	  a = 16*(src[src1_offset + 0] + src[src2_offset + 16] + 1) - 7*(V+H);
	  for(j=16; j>0; --j) {
	    int b = a;
	    a += V;
	    for(i=-16; i<0; i+=4) {
	      src[src_offset +16+i] = H264DSPContext.ff_cropTbl[cm_offset + ( (b    ) >> 5  )];
	      src[src_offset +17+i] = H264DSPContext.ff_cropTbl[cm_offset + ( (b+  H) >> 5  )];
	      src[src_offset +18+i] = H264DSPContext.ff_cropTbl[cm_offset + ( (b+2*H) >> 5  )];
	      src[src_offset +19+i] = H264DSPContext.ff_cropTbl[cm_offset + ( (b+3*H) >> 5  )];
	      b += 4*H;
	    }
	    src_offset += stride;
	  }
	}

	public static void pred16x16_plane_c(int[] src, int src_offset, int stride){
	    pred16x16_plane_compat_c(src, src_offset, stride, 0, 0);
	}

	public static void pred16x16_plane_svq3_c(int[] src, int src_offset, int stride){
	    pred16x16_plane_compat_c(src, src_offset, stride, 1, 0);
	}

	public static void pred16x16_plane_rv40_c(int[] src, int src_offset, int stride){
	    pred16x16_plane_compat_c(src, src_offset, stride, 0, 1);
	}

	public static void pred16x16_tm_vp8_c(int[] src, int src_offset, int stride){
	    int cm_offset = H264DSPContext.MAX_NEG_CROP - src[src_offset -1-stride];
	    int top_offset = src_offset -stride;
	    int y;

	    for (y = 0; y < 16; y++) {
	        int cm_in_offset = cm_offset + src[src_offset -1];
	        src[src_offset +0]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +0]];
	        src[src_offset +1]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +1]];
	        src[src_offset +2]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +2]];
	        src[src_offset +3]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +3]];
	        src[src_offset +4]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +4]];
	        src[src_offset +5]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +5]];
	        src[src_offset +6]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +6]];
	        src[src_offset +7]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +7]];
	        src[src_offset +8]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +8]];
	        src[src_offset +9]  = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +9]];
	        src[src_offset +10] = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +10]];
	        src[src_offset +11] = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +11]];
	        src[src_offset +12] = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +12]];
	        src[src_offset +13] = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +13]];
	        src[src_offset +14] = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +14]];
	        src[src_offset +15] = H264DSPContext.ff_cropTbl[cm_in_offset + src[top_offset +15]];
	        src_offset += stride;
	    }
	}

	public static void pred8x8_vertical_c(int[] src, int src_offset, int stride){
	    int i;
	    
	    int a0 = src[src_offset -stride + 0];
	    int a1 = src[src_offset -stride + 1];
	    int a2 = src[src_offset -stride + 2];
	    int a3 = src[src_offset -stride + 3];
	    
	    int b0 = src[src_offset -stride + 4];
	    int b1 = src[src_offset -stride + 5];
	    int b2 = src[src_offset -stride + 6];
	    int b3 = src[src_offset -stride + 7];
	    	
	    for(i=0; i<8; i++){
	    	src[src_offset +i*stride + 0] = a0;
	    	src[src_offset +i*stride + 1] = a1;
	    	src[src_offset +i*stride + 2] = a2;
	    	src[src_offset +i*stride + 3] = a3;

	    	src[src_offset +i*stride + 4] = b0;
	    	src[src_offset +i*stride + 5] = b1;
	    	src[src_offset +i*stride + 6] = b2;
	    	src[src_offset +i*stride + 7] = b3;
	    }
	}

	public static void pred8x8_horizontal_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
	    	src[src_offset +i*stride + 1] = 
	    	src[src_offset +i*stride + 2] = 
	    	src[src_offset +i*stride + 3] = 
	    	src[src_offset +i*stride + 4] = 
	    	src[src_offset +i*stride + 5] = 
	    	src[src_offset +i*stride + 6] = 
	    	src[src_offset +i*stride + 7] = src[src_offset -1+i*stride];
	    }
	}

	public static void pred8x8_128_dc_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 128;
	    }
	}

	public static void pred8x8_127_dc_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 127;
	    }
	}
	public static void pred8x8_129_dc_c(int[] src, int src_offset, int stride){
	    int i;

	    for(i=0; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = 129;
	    }
	}

	public static void pred8x8_left_dc_c(int[] src, int src_offset, int stride){
	    int i;
	    int dc0, dc2;

	    dc0=dc2=0;
	    for(i=0;i<4; i++){
	        dc0+= src[src_offset -1+i*stride];
	        dc2+= src[src_offset -1+(i+4)*stride];
	    }
	    dc0= ((dc0 + 2)>>2);
	    dc2= ((dc2 + 2)>>2);

	    for(i=0; i<4; i++){
	    	src[src_offset +i*stride + 0] = 
	    		src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc0;
	    }
	    for(i=4; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
	    		src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc2;	    	
	    }
	}

	public static void pred8x8_left_dc_rv40_c(int[] src, int src_offset, int stride){
	    int i;
	    int dc0;

	    dc0=0;
	    for(i=0;i<8; i++)
	        dc0+= src[src_offset -1+i*stride];
	    dc0= ((dc0 + 4)>>3);

	    for(i=0; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
	    		src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc0;	    	
	    }
	}

	public static void pred8x8_top_dc_c(int[] src, int src_offset, int stride){
	    int i;
	    int dc0, dc1;

	    dc0=dc1=0;
	    for(i=0;i<4; i++){
	        dc0+= src[src_offset +i-stride];
	        dc1+= src[src_offset +4+i-stride];
	    }
	    dc0= ((dc0 + 2)>>2);
	    dc1= ((dc1 + 2)>>2);

	    for(i=0; i<4; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = dc0;
	    	
		    src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc1;
	    }
	    for(i=4; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = dc0;
	    	
		    src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc1;
	    }
	}

	public static void pred8x8_top_dc_rv40_c(int[] src, int src_offset, int stride){
	    int i;
	    int dc0;

	    dc0=0;
	    for(i=0;i<8; i++)
	        dc0+= src[src_offset +i-stride];
	    dc0 = ((dc0 + 4)>>3);

	    for(i=0; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
	    		src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = 
		    	src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc0;	    	
	    }
	}


	public static void pred8x8_dc_c(int[] src, int src_offset, int stride){
	    int i;
	    int dc0, dc1, dc2, dc3;

	    dc0=dc1=dc2=0;
	    for(i=0;i<4; i++){
	        dc0+= src[src_offset -1+i*stride] + src[src_offset +i-stride];
	        dc1+= src[src_offset +4+i-stride];
	        dc2+= src[src_offset -1+(i+4)*stride];
	    }
	    dc3 = ((dc1 + dc2 + 4)>>3);
	    dc0 = ((dc0 + 4)>>3);
	    dc1 = ((dc1 + 2)>>2);
	    dc2 = ((dc2 + 2)>>2);

	    for(i=0; i<4; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = dc0;
	    	
		    src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc1;
	    }
	    for(i=4; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = dc2;
	    	
		    src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc3;
	    }
	}

	//the following 4 function should not be optimized!
	public static void pred8x8_mad_cow_dc_l0t(int[] src, int src_offset, int stride){
	    pred8x8_top_dc_c(src, src_offset, stride);
	    pred4x4_dc_c(src, src_offset, null, 0, stride);
	}

	public static void pred8x8_mad_cow_dc_0lt(int[] src, int src_offset, int stride){
	    pred8x8_dc_c(src, src_offset, stride);
	    pred4x4_top_dc_c(src, src_offset, null, 0, stride);
	}

	public static void pred8x8_mad_cow_dc_l00(int[] src, int src_offset, int stride){
	    pred8x8_left_dc_c(src, src_offset, stride);
	    pred4x4_128_dc_c(src, src_offset +4*stride    , null, 0, stride);
	    pred4x4_128_dc_c(src, src_offset +4*stride + 4, null, 0, stride);
	}

	public static void pred8x8_mad_cow_dc_0l0(int[] src, int src_offset, int stride){
	    pred8x8_left_dc_c(src, src_offset, stride);
	    pred4x4_128_dc_c(src, src_offset   , null, 0, stride);
	    pred4x4_128_dc_c(src, src_offset + 4, null, 0, stride);
	}

	public static void pred8x8_dc_rv40_c(int[] src, int src_offset, int stride){
	    int i;
	    int dc0=0;

	    for(i=0;i<4; i++){
	        dc0+= src[src_offset -1+i*stride] + src[src_offset +i-stride];
	        dc0+= src[src_offset +4+i-stride];
	        dc0+= src[src_offset -1+(i+4)*stride];
	    }
	    dc0 = ((dc0 + 8)>>4);

	    for(i=0; i<4; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = dc0;
	    	
		    src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc0;
	    }
	    for(i=4; i<8; i++){
	    	src[src_offset +i*stride + 0] = 
		    	src[src_offset +i*stride + 1] = 
		    	src[src_offset +i*stride + 2] = 
		    	src[src_offset +i*stride + 3] = dc0;
	    	
		    src[src_offset +i*stride + 4] = 
		    	src[src_offset +i*stride + 5] = 
		    	src[src_offset +i*stride + 6] = 
		    	src[src_offset +i*stride + 7] = dc0;
	    }
	}

	public static void pred8x8_plane_c(int[] src, int src_offset, int stride){
	  int j, k;
	  int a;
	  int cm_offset = H264DSPContext.MAX_NEG_CROP;
	  int src0_offset = src_offset +3-stride;
	  int src1_offset = src_offset +4*stride-1;
	  int src2_offset = src1_offset -2*stride;      // == src+2*stride-1;
	  int H = src[src0_offset + 1] - src[src0_offset - 1];
	  int V = src[src1_offset + 0] - src[src2_offset + 0];
	  for(k=2; k<=4; ++k) {
	    src1_offset += stride; src2_offset -= stride;
	    H += k*(src[src0_offset + k] - src[src0_offset - k]);
	    V += k*(src[src1_offset + 0] - src[src2_offset + 0]);
	  }
	  H = ( 17*H+16 ) >> 5;
	  V = ( 17*V+16 ) >> 5;

	  a = 16*(src[src1_offset + 0] + src[src2_offset + 8]+1) - 3*(V+H);
	  for(j=8; j>0; --j) {
	    int b = a;
	    a += V;
	    src[src_offset +0] = H264DSPContext.ff_cropTbl[cm_offset +((b    ) >> 5) ];
	    src[src_offset +1] = H264DSPContext.ff_cropTbl[cm_offset +((b+  H) >> 5) ];
	    src[src_offset +2] = H264DSPContext.ff_cropTbl[cm_offset +((b+2*H) >> 5) ];
	    src[src_offset +3] = H264DSPContext.ff_cropTbl[cm_offset +((b+3*H) >> 5) ];
	    src[src_offset +4] = H264DSPContext.ff_cropTbl[cm_offset +((b+4*H) >> 5) ];
	    src[src_offset +5] = H264DSPContext.ff_cropTbl[cm_offset +((b+5*H) >> 5) ];
	    src[src_offset +6] = H264DSPContext.ff_cropTbl[cm_offset +((b+6*H) >> 5) ];
	    src[src_offset +7] = H264DSPContext.ff_cropTbl[cm_offset +((b+7*H) >> 5) ];
	    src_offset += stride;
	  }
	}

	public static void pred8x8_tm_vp8_c(int[] src, int src_offset, int stride){
	    int cm_offset = H264DSPContext.MAX_NEG_CROP - src[src_offset -1-stride];
	    int top_offset = src_offset -stride;
	    int y;

	    for (y = 0; y < 8; y++) {
	        int cm_in_offset = cm_offset + src[src_offset -1];
	        src[src_offset +0] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +0]];
	        src[src_offset +1] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +1]];
	        src[src_offset +2] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +2]];
	        src[src_offset +3] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +3]];
	        src[src_offset +4] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +4]];
	        src[src_offset +5] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +5]];
	        src[src_offset +6] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +6]];
	        src[src_offset +7] = H264DSPContext.ff_cropTbl[cm_in_offset +src[top_offset +7]];
	        src_offset += stride;
	    }
	}

//////////////////
//////////////////
	
	public static void pred8x8l_128_dc_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int y;
	    for( y = 0; y < 8; y++ ) { 
			src[src_offset + 0] =
			src[src_offset + 1] =
			src[src_offset + 2] =
			src[src_offset + 3] =
			src[src_offset + 4] =
			src[src_offset + 5] =
			src[src_offset + 6] =
			src[src_offset + 7] = 128; 
	        src_offset += stride; 
	    }
	}
	
	public static void pred8x8l_left_dc_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int l0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset -1 +0*stride] ) 
                + 2*src[src_offset -1 +0*stride] + src[src_offset -1 +1*stride] + 2) >> 2; 
		int l1 = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 +1*stride] + src[src_offset -1 +2*stride] + 2) >> 2;
		int l2 = (src[src_offset -1 +1*stride] + 2*src[src_offset -1 +2*stride] + src[src_offset -1 +3*stride] + 2) >> 2;
		int l3 = (src[src_offset -1 +2*stride] + 2*src[src_offset -1 +3*stride] + src[src_offset -1 +4*stride] + 2) >> 2;
		int l4 = (src[src_offset -1 +3*stride] + 2*src[src_offset -1 +4*stride] + src[src_offset -1 +5*stride] + 2) >> 2;
		int l5 = (src[src_offset -1 +4*stride] + 2*src[src_offset -1 +5*stride] + src[src_offset -1 +6*stride] + 2) >> 2;
		int l6 = (src[src_offset -1 +5*stride] + 2*src[src_offset -1 +6*stride] + src[src_offset -1 +7*stride] + 2) >> 2;
		int l7 = (src[src_offset -1 +6*stride] + 3*src[src_offset -1 +7*stride] + 2) >> 2;

		int dc = ((l0+l1+l2+l3+l4+l5+l6+l7+4) >> 3);
	    int y;
	    for( y = 0; y < 8; y++ ) { 
			src[src_offset + 0] =
			src[src_offset + 1] =
			src[src_offset + 2] =
			src[src_offset + 3] =
			src[src_offset + 4] =
			src[src_offset + 5] =
			src[src_offset + 6] =
			src[src_offset + 7] = dc; 
	        src_offset += stride; 
	    }
	}
	
	public static void pred8x8l_top_dc_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;

        int dc = ((t0+t1+t2+t3+t4+t5+t6+t7+4) >> 3);
	    int y;
	    for( y = 0; y < 8; y++ ) { 
			src[src_offset + 0] =
			src[src_offset + 1] =
			src[src_offset + 2] =
			src[src_offset + 3] =
			src[src_offset + 4] =
			src[src_offset + 5] =
			src[src_offset + 6] =
			src[src_offset + 7] = dc; 
	        src_offset += stride; 
	    }
	}
	public static void pred8x8l_dc_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int l0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset -1 +0*stride] ) 
                + 2*src[src_offset -1 +0*stride] + src[src_offset -1 +1*stride] + 2) >> 2; 
		int l1 = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 +1*stride] + src[src_offset -1 +2*stride] + 2) >> 2;
		int l2 = (src[src_offset -1 +1*stride] + 2*src[src_offset -1 +2*stride] + src[src_offset -1 +3*stride] + 2) >> 2;
		int l3 = (src[src_offset -1 +2*stride] + 2*src[src_offset -1 +3*stride] + src[src_offset -1 +4*stride] + 2) >> 2;
		int l4 = (src[src_offset -1 +3*stride] + 2*src[src_offset -1 +4*stride] + src[src_offset -1 +5*stride] + 2) >> 2;
		int l5 = (src[src_offset -1 +4*stride] + 2*src[src_offset -1 +5*stride] + src[src_offset -1 +6*stride] + 2) >> 2;
		int l6 = (src[src_offset -1 +5*stride] + 2*src[src_offset -1 +6*stride] + src[src_offset -1 +7*stride] + 2) >> 2;
		int l7 = (src[src_offset -1 +6*stride] + 3*src[src_offset -1 +7*stride] + 2) >> 2;

	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;

		int dc = ((l0+l1+l2+l3+l4+l5+l6+l7
	                         +t0+t1+t2+t3+t4+t5+t6+t7+8) >> 4);
	    int y;
	    for( y = 0; y < 8; y++ ) { 
			src[src_offset + 0] =
			src[src_offset + 1] =
			src[src_offset + 2] =
			src[src_offset + 3] =
			src[src_offset + 4] =
			src[src_offset + 5] =
			src[src_offset + 6] =
			src[src_offset + 7] = dc; 
	        src_offset += stride; 
	    }
	}
	
	public static void pred8x8l_horizontal_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int l0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset -1 +0*stride] ) 
                + 2*src[src_offset -1 +0*stride] + src[src_offset -1 +1*stride] + 2) >> 2; 
		int l1 = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 +1*stride] + src[src_offset -1 +2*stride] + 2) >> 2;
		int l2 = (src[src_offset -1 +1*stride] + 2*src[src_offset -1 +2*stride] + src[src_offset -1 +3*stride] + 2) >> 2;
		int l3 = (src[src_offset -1 +2*stride] + 2*src[src_offset -1 +3*stride] + src[src_offset -1 +4*stride] + 2) >> 2;
		int l4 = (src[src_offset -1 +3*stride] + 2*src[src_offset -1 +4*stride] + src[src_offset -1 +5*stride] + 2) >> 2;
		int l5 = (src[src_offset -1 +4*stride] + 2*src[src_offset -1 +5*stride] + src[src_offset -1 +6*stride] + 2) >> 2;
		int l6 = (src[src_offset -1 +5*stride] + 2*src[src_offset -1 +6*stride] + src[src_offset -1 +7*stride] + 2) >> 2;
		int l7 = (src[src_offset -1 +6*stride] + 3*src[src_offset -1 +7*stride] + 2) >> 2;

		src[src_offset + 0 * stride + 0] =
			src[src_offset + 0 * stride + 1] = 
			src[src_offset + 0 * stride + 2] = 
			src[src_offset + 0 * stride + 3] = 
			src[src_offset + 0 * stride + 4] = 
			src[src_offset + 0 * stride + 5] = 
			src[src_offset + 0 * stride + 6] = 
			src[src_offset + 0 * stride + 7] = l0;

		src[src_offset + 1 * stride + 0] =
			src[src_offset + 1 * stride + 1] = 
			src[src_offset + 1 * stride + 2] = 
			src[src_offset + 1 * stride + 3] = 
			src[src_offset + 1 * stride + 4] = 
			src[src_offset + 1 * stride + 5] = 
			src[src_offset + 1 * stride + 6] = 
			src[src_offset + 1 * stride + 7] = l1;

		src[src_offset + 2 * stride + 0] =
			src[src_offset + 2 * stride + 1] = 
			src[src_offset + 2 * stride + 2] = 
			src[src_offset + 2 * stride + 3] = 
			src[src_offset + 2 * stride + 4] = 
			src[src_offset + 2 * stride + 5] = 
			src[src_offset + 2 * stride + 6] = 
			src[src_offset + 2 * stride + 7] = l2;

		src[src_offset + 3 * stride + 0] =
			src[src_offset + 3 * stride + 1] = 
			src[src_offset + 3 * stride + 2] = 
			src[src_offset + 3 * stride + 3] = 
			src[src_offset + 3 * stride + 4] = 
			src[src_offset + 3 * stride + 5] = 
			src[src_offset + 3 * stride + 6] = 
			src[src_offset + 3 * stride + 7] = l3;

		src[src_offset + 4 * stride + 0] =
			src[src_offset + 4 * stride + 1] = 
			src[src_offset + 4 * stride + 2] = 
			src[src_offset + 4 * stride + 3] = 
			src[src_offset + 4 * stride + 4] = 
			src[src_offset + 4 * stride + 5] = 
			src[src_offset + 4 * stride + 6] = 
			src[src_offset + 4 * stride + 7] = l4;

		src[src_offset + 5 * stride + 0] =
			src[src_offset + 5 * stride + 1] = 
			src[src_offset + 5 * stride + 2] = 
			src[src_offset + 5 * stride + 3] = 
			src[src_offset + 5 * stride + 4] = 
			src[src_offset + 5 * stride + 5] = 
			src[src_offset + 5 * stride + 6] = 
			src[src_offset + 5 * stride + 7] = l5;

		src[src_offset + 6 * stride + 0] =
			src[src_offset + 6 * stride + 1] = 
			src[src_offset + 6 * stride + 2] = 
			src[src_offset + 6 * stride + 3] = 
			src[src_offset + 6 * stride + 4] = 
			src[src_offset + 6 * stride + 5] = 
			src[src_offset + 6 * stride + 6] = 
			src[src_offset + 6 * stride + 7] = l6;

		src[src_offset + 7 * stride + 0] =
			src[src_offset + 7 * stride + 1] = 
			src[src_offset + 7 * stride + 2] = 
			src[src_offset + 7 * stride + 3] = 
			src[src_offset + 7 * stride + 4] = 
			src[src_offset + 7 * stride + 5] = 
			src[src_offset + 7 * stride + 6] = 
			src[src_offset + 7 * stride + 7] = l7;
	}
	
	public static void pred8x8l_vertical_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int y;
	    
	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;

	    src[src_offset +0] = t0;
	    src[src_offset +1] = t1;
	    src[src_offset +2] = t2;
	    src[src_offset +3] = t3;
	    src[src_offset +4] = t4;
	    src[src_offset +5] = t5;
	    src[src_offset +6] = t6;
	    src[src_offset +7] = t7;
	    for( y = 1; y < 8; y++ ) {
	    	src[src_offset +y*stride +0] = src[src_offset +0];
	    	src[src_offset +y*stride +1] = src[src_offset +1];
	    	src[src_offset +y*stride +2] = src[src_offset +2];
	    	src[src_offset +y*stride +3] = src[src_offset +3];
	    	src[src_offset +y*stride +4] = src[src_offset +4];
	    	src[src_offset +y*stride +5] = src[src_offset +5];
	    	src[src_offset +y*stride +6] = src[src_offset +6];
	    	src[src_offset +y*stride +7] = src[src_offset +7];
	    } // for
	}
	
	public static void pred8x8l_down_left_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
	    
        int t8, t9, t10, t11, t12, t13, t14, t15; 
        if(has_topright!=0) { 
            t8 = (src[src_offset +7 -1*stride] + 2*src[src_offset +8 -1*stride] + src[src_offset +9 -1*stride] + 2) >> 2;
            t9 = (src[src_offset +8 -1*stride] + 2*src[src_offset +9 -1*stride] + src[src_offset +10 -1*stride] + 2) >> 2;
            t10 = (src[src_offset +9 -1*stride] + 2*src[src_offset +10 -1*stride] + src[src_offset +11 -1*stride] + 2) >> 2;
            t11 = (src[src_offset +10 -1*stride] + 2*src[src_offset +11 -1*stride] + src[src_offset +12 -1*stride] + 2) >> 2;
            t12 = (src[src_offset +11 -1*stride] + 2*src[src_offset +12 -1*stride] + src[src_offset +13 -1*stride] + 2) >> 2;
            t13 = (src[src_offset +12 -1*stride] + 2*src[src_offset +13 -1*stride] + src[src_offset +14 -1*stride] + 2) >> 2;
            t14 = (src[src_offset +13 -1*stride] + 2*src[src_offset +14 -1*stride] + src[src_offset +15 -1*stride] + 2) >> 2;
            t15 = (src[src_offset +14 -1*stride] + 3*src[src_offset +15 -1*stride] + 2) >> 2;
        } else t8=t9=t10=t11=t12=t13=t14=t15= src[src_offset +7 -1*stride];
	    	    
        src[src_offset +0 +0*stride]= (t0 + 2*t1 + t2 + 2) >> 2;
        src[src_offset +0 +1*stride]=
        	src[src_offset +1 +0*stride]= (t1 + 2*t2 + t3 + 2) >> 2;
        src[src_offset +0 +2*stride]=
        	src[src_offset +1 +1*stride]=
        	src[src_offset +2 +0*stride]= (t2 + 2*t3 + t4 + 2) >> 2;
        src[src_offset +0 +3*stride]=
        	src[src_offset +1 +2*stride]=
        	src[src_offset +2 +1*stride]=
        	src[src_offset +3 +0*stride]= (t3 + 2*t4 + t5 + 2) >> 2;
        src[src_offset +0 +4*stride]=
        	src[src_offset +1 +3*stride]=
        	src[src_offset +2 +2*stride]=
        	src[src_offset +3 +1*stride]=
        	src[src_offset +4 +0*stride]= (t4 + 2*t5 + t6 + 2) >> 2;
        src[src_offset +0 +5*stride]=
        	src[src_offset +1 +4*stride]=
        	src[src_offset +2 +3*stride]=
        	src[src_offset +3 +2*stride]=
        	src[src_offset +4 +1*stride]=
        	src[src_offset +5 +0*stride]= (t5 + 2*t6 + t7 + 2) >> 2;
        src[src_offset +0 +6*stride]=
        	src[src_offset +1 +5*stride]=
        	src[src_offset +2 +4*stride]=
        	src[src_offset +3 +3*stride]=
        	src[src_offset +4 +2*stride]=
        	src[src_offset +5 +1*stride]=
        	src[src_offset +6 +0*stride]= (t6 + 2*t7 + t8 + 2) >> 2;
        src[src_offset +0 +7*stride]=
        	src[src_offset +1 +6*stride]=
        	src[src_offset +2 +5*stride]=
        	src[src_offset +3 +4*stride]=
        	src[src_offset +4 +3*stride]=
        	src[src_offset +5 +2*stride]=
        	src[src_offset +6 +1*stride]=
        	src[src_offset +7 +0*stride]= (t7 + 2*t8 + t9 + 2) >> 2;
        	
	    src[src_offset +(1) +(7)*stride]=
	    	src[src_offset +(2) +(6)*stride]=
	    	src[src_offset +(3) +(5)*stride]=
	    	src[src_offset +(4) +(4)*stride]=
	    	src[src_offset +(5) +(3)*stride]=
	    	src[src_offset +(6) +(2)*stride]=
	    	src[src_offset +(7) +(1)*stride]= (t8 + 2*t9 + t10 + 2) >> 2;
	    src[src_offset +(2) +(7)*stride]=
	    	src[src_offset +(3) +(6)*stride]=
	    	src[src_offset +(4) +(5)*stride]=
	    	src[src_offset +(5) +(4)*stride]=
	    	src[src_offset +(6) +(3)*stride]=
	    	src[src_offset +(7) +(2)*stride]= (t9 + 2*t10 + t11 + 2) >> 2;
	    src[src_offset +(3) +(7)*stride]=
	    	src[src_offset +(4) +(6)*stride]=
	    	src[src_offset +(5) +(5)*stride]=
	    	src[src_offset +(6) +(4)*stride]=
	    	src[src_offset +(7) +(3)*stride]= (t10 + 2*t11 + t12 + 2) >> 2;
	    src[src_offset +(4) +(7)*stride]=
	    	src[src_offset +(5) +(6)*stride]=
	    	src[src_offset +(6) +(5)*stride]=
	    	src[src_offset +(7) +(4)*stride]= (t11 + 2*t12 + t13 + 2) >> 2;
	    src[src_offset +(5) +(7)*stride]=
	    	src[src_offset +(6) +(6)*stride]=
	    	src[src_offset +(7) +(5)*stride]= (t12 + 2*t13 + t14 + 2) >> 2;
	    src[src_offset +(6) +(7)*stride]=
	    	src[src_offset +(7) +(6)*stride]= (t13 + 2*t14 + t15 + 2) >> 2;
	    src[src_offset +(7) +(7)*stride]= (t14 + 3*t15 + 2) >> 2;
	}
	
	public static void pred8x8l_down_right_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;

	    int l0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset -1 +0*stride] ) 
                + 2*src[src_offset -1 +0*stride] + src[src_offset -1 +1*stride] + 2) >> 2; 
		int l1 = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 +1*stride] + src[src_offset -1 +2*stride] + 2) >> 2;
		int l2 = (src[src_offset -1 +1*stride] + 2*src[src_offset -1 +2*stride] + src[src_offset -1 +3*stride] + 2) >> 2;
		int l3 = (src[src_offset -1 +2*stride] + 2*src[src_offset -1 +3*stride] + src[src_offset -1 +4*stride] + 2) >> 2;
		int l4 = (src[src_offset -1 +3*stride] + 2*src[src_offset -1 +4*stride] + src[src_offset -1 +5*stride] + 2) >> 2;
		int l5 = (src[src_offset -1 +4*stride] + 2*src[src_offset -1 +5*stride] + src[src_offset -1 +6*stride] + 2) >> 2;
		int l6 = (src[src_offset -1 +5*stride] + 2*src[src_offset -1 +6*stride] + src[src_offset -1 +7*stride] + 2) >> 2;
		int l7 = (src[src_offset -1 +6*stride] + 3*src[src_offset -1 +7*stride] + 2) >> 2;

	    int lt = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 -1*stride] + src[src_offset +0 -1*stride] + 2) >> 2;		
		
	    src[src_offset +(0) +(7)*stride]= (l7 + 2*l6 + l5 + 2) >> 2;
	    src[src_offset +(0) +(6)*stride]=
	    	src[src_offset +(1) +(7)*stride]= (l6 + 2*l5 + l4 + 2) >> 2;
	    src[src_offset +(0) +(5)*stride]=
	    	src[src_offset +(1) +(6)*stride]=
	    	src[src_offset +(2) +(7)*stride]= (l5 + 2*l4 + l3 + 2) >> 2;
	    src[src_offset +(0) +(4)*stride]=
	    	src[src_offset +(1) +(5)*stride]=
	    	src[src_offset +(2) +(6)*stride]=
	    	src[src_offset +(3) +(7)*stride]= (l4 + 2*l3 + l2 + 2) >> 2;
	    src[src_offset +(0) +(3)*stride]=
	    	src[src_offset +(1) +(4)*stride]=
	    	src[src_offset +(2) +(5)*stride]=
	    	src[src_offset +(3) +(6)*stride]=
	    	src[src_offset +(4) +(7)*stride]= (l3 + 2*l2 + l1 + 2) >> 2;
	    src[src_offset +(0) +(2)*stride]=
	    	src[src_offset +(1) +(3)*stride]=
	    	src[src_offset +(2) +(4)*stride]=
	    	src[src_offset +(3) +(5)*stride]=
	    	src[src_offset +(4) +(6)*stride]=
	    	src[src_offset +(5) +(7)*stride]= (l2 + 2*l1 + l0 + 2) >> 2;
	    src[src_offset +(0) +(1)*stride]=
	    	src[src_offset +(1) +(2)*stride]=
	    	src[src_offset +(2) +(3)*stride]=
	    	src[src_offset +(3) +(4)*stride]=
	    	src[src_offset +(4) +(5)*stride]=
	    	src[src_offset +(5) +(6)*stride]=
	    	src[src_offset +(6) +(7)*stride]= (l1 + 2*l0 + lt + 2) >> 2;
	    src[src_offset +(0) +(0)*stride]=
	    	src[src_offset +(1) +(1)*stride]=
	    	src[src_offset +(2) +(2)*stride]=
	    	src[src_offset +(3) +(3)*stride]=
	    	src[src_offset +(4) +(4)*stride]=
	    	src[src_offset +(5) +(5)*stride]=
	    	src[src_offset +(6) +(6)*stride]=
	    	src[src_offset +(7) +(7)*stride]= (l0 + 2*lt + t0 + 2) >> 2;
	    src[src_offset +(1) +(0)*stride]=
	    	src[src_offset +(2) +(1)*stride]=
	    	src[src_offset +(3) +(2)*stride]=
	    	src[src_offset +(4) +(3)*stride]=
	    	src[src_offset +(5) +(4)*stride]=
	    	src[src_offset +(6) +(5)*stride]=
	    	src[src_offset +(7) +(6)*stride]= (lt + 2*t0 + t1 + 2) >> 2;
	    src[src_offset +(2) +(0)*stride]=
	    	src[src_offset +(3) +(1)*stride]=
	    	src[src_offset +(4) +(2)*stride]=
	    	src[src_offset +(5) +(3)*stride]=
	    	src[src_offset +(6) +(4)*stride]=
	    	src[src_offset +(7) +(5)*stride]= (t0 + 2*t1 + t2 + 2) >> 2;
	    src[src_offset +(3) +(0)*stride]=
	    	src[src_offset +(4) +(1)*stride]=
	    	src[src_offset +(5) +(2)*stride]=
	    	src[src_offset +(6) +(3)*stride]=
	    	src[src_offset +(7) +(4)*stride]= (t1 + 2*t2 + t3 + 2) >> 2;
	    src[src_offset +(4) +(0)*stride]=
	    	src[src_offset +(5) +(1)*stride]=
	    	src[src_offset +(6) +(2)*stride]=
	    	src[src_offset +(7) +(3)*stride]= (t2 + 2*t3 + t4 + 2) >> 2;
	    src[src_offset +(5) +(0)*stride]=
	    	src[src_offset +(6) +(1)*stride]=
	    	src[src_offset +(7) +(2)*stride]= (t3 + 2*t4 + t5 + 2) >> 2;
	    src[src_offset +(6) +(0)*stride]=
	    	src[src_offset +(7) +(1)*stride]= (t4 + 2*t5 + t6 + 2) >> 2;
	    src[src_offset +(7) +(0)*stride]= (t5 + 2*t6 + t7 + 2) >> 2;

	}
	public static void pred8x8l_vertical_right_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
	    
	    int l0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset -1 +0*stride] ) 
                + 2*src[src_offset -1 +0*stride] + src[src_offset -1 +1*stride] + 2) >> 2; 
		int l1 = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 +1*stride] + src[src_offset -1 +2*stride] + 2) >> 2;
		int l2 = (src[src_offset -1 +1*stride] + 2*src[src_offset -1 +2*stride] + src[src_offset -1 +3*stride] + 2) >> 2;
		int l3 = (src[src_offset -1 +2*stride] + 2*src[src_offset -1 +3*stride] + src[src_offset -1 +4*stride] + 2) >> 2;
		int l4 = (src[src_offset -1 +3*stride] + 2*src[src_offset -1 +4*stride] + src[src_offset -1 +5*stride] + 2) >> 2;
		int l5 = (src[src_offset -1 +4*stride] + 2*src[src_offset -1 +5*stride] + src[src_offset -1 +6*stride] + 2) >> 2;
		int l6 = (src[src_offset -1 +5*stride] + 2*src[src_offset -1 +6*stride] + src[src_offset -1 +7*stride] + 2) >> 2;
		//int l7 = (src[src_offset -1 +6*stride] + 3*src[src_offset -1 +7*stride] + 2) >> 2;

	    int lt = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 -1*stride] + src[src_offset +0 -1*stride] + 2) >> 2;		
		
	    src[src_offset +(0) +(6)*stride]= (l5 + 2*l4 + l3 + 2) >> 2;
	    src[src_offset +(0) +(7)*stride]= (l6 + 2*l5 + l4 + 2) >> 2;
	    src[src_offset +(0) +(4)*stride]=
	    	src[src_offset +(1) +(6)*stride]= (l3 + 2*l2 + l1 + 2) >> 2;
	    src[src_offset +(0) +(5)*stride]=
	    	src[src_offset +(1) +(7)*stride]= (l4 + 2*l3 + l2 + 2) >> 2;
	    src[src_offset +(0) +(2)*stride]=
	    	src[src_offset +(1) +(4)*stride]=
	    	src[src_offset +(2) +(6)*stride]= (l1 + 2*l0 + lt + 2) >> 2;
	    src[src_offset +(0) +(3)*stride]=
	    	src[src_offset +(1) +(5)*stride]=
	    	src[src_offset +(2) +(7)*stride]= (l2 + 2*l1 + l0 + 2) >> 2;
	    src[src_offset +(0) +(1)*stride]=
	    	src[src_offset +(1) +(3)*stride]=
	    	src[src_offset +(2) +(5)*stride]=
	    	src[src_offset +(3) +(7)*stride]= (l0 + 2*lt + t0 + 2) >> 2;
	    src[src_offset +(0) +(0)*stride]=
	    	src[src_offset +(1) +(2)*stride]=
	    	src[src_offset +(2) +(4)*stride]=
	    	src[src_offset +(3) +(6)*stride]= (lt + t0 + 1) >> 1;
	    src[src_offset +(1) +(1)*stride]=
	    	src[src_offset +(2) +(3)*stride]=
	    	src[src_offset +(3) +(5)*stride]=
	    	src[src_offset +(4) +(7)*stride]= (lt + 2*t0 + t1 + 2) >> 2;
	    src[src_offset +(1) +(0)*stride]=
	    	src[src_offset +(2) +(2)*stride]=
	    	src[src_offset +(3) +(4)*stride]=
	    	src[src_offset +(4) +(6)*stride]= (t0 + t1 + 1) >> 1;
	    src[src_offset +(2) +(1)*stride]=
	    	src[src_offset +(3) +(3)*stride]=
	    	src[src_offset +(4) +(5)*stride]=
	    	src[src_offset +(5) +(7)*stride]= (t0 + 2*t1 + t2 + 2) >> 2;
	    src[src_offset +(2) +(0)*stride]=
	    	src[src_offset +(3) +(2)*stride]=
	    	src[src_offset +(4) +(4)*stride]=
	    	src[src_offset +(5) +(6)*stride]= (t1 + t2 + 1) >> 1;
	    src[src_offset +(3) +(1)*stride]=
	    	src[src_offset +(4) +(3)*stride]=
	    	src[src_offset +(5) +(5)*stride]=
	    	src[src_offset +(6) +(7)*stride]= (t1 + 2*t2 + t3 + 2) >> 2;
	    src[src_offset +(3) +(0)*stride]=
	    	src[src_offset +(4) +(2)*stride]=
	    	src[src_offset +(5) +(4)*stride]=
	    	src[src_offset +(6) +(6)*stride]= (t2 + t3 + 1) >> 1;
	    src[src_offset +(4) +(1)*stride]=
	    	src[src_offset +(5) +(3)*stride]=
	    	src[src_offset +(6) +(5)*stride]=
	    	src[src_offset +(7) +(7)*stride]= (t2 + 2*t3 + t4 + 2) >> 2;
	    src[src_offset +(4) +(0)*stride]=
	    	src[src_offset +(5) +(2)*stride]=
	    	src[src_offset +(6) +(4)*stride]=
	    	src[src_offset +(7) +(6)*stride]= (t3 + t4 + 1) >> 1;
	    src[src_offset +(5) +(1)*stride]=
	    	src[src_offset +(6) +(3)*stride]=
	    	src[src_offset +(7) +(5)*stride]= (t3 + 2*t4 + t5 + 2) >> 2;
	    src[src_offset +(5) +(0)*stride]=
	    	src[src_offset +(6) +(2)*stride]=
	    	src[src_offset +(7) +(4)*stride]= (t4 + t5 + 1) >> 1;
	    src[src_offset +(6) +(1)*stride]=
	    	src[src_offset +(7) +(3)*stride]= (t4 + 2*t5 + t6 + 2) >> 2;
	    src[src_offset +(6) +(0)*stride]=
	    	src[src_offset +(7) +(2)*stride]= (t5 + t6 + 1) >> 1;
	    src[src_offset +(7) +(1)*stride]= (t5 + 2*t6 + t7 + 2) >> 2;
	    src[src_offset +(7) +(0)*stride]= (t6 + t7 + 1) >> 1;
	}
	public static void pred8x8l_horizontal_down_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		//int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		//                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;

	    int l0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset -1 +0*stride] ) 
                + 2*src[src_offset -1 +0*stride] + src[src_offset -1 +1*stride] + 2) >> 2; 
		int l1 = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 +1*stride] + src[src_offset -1 +2*stride] + 2) >> 2;
		int l2 = (src[src_offset -1 +1*stride] + 2*src[src_offset -1 +2*stride] + src[src_offset -1 +3*stride] + 2) >> 2;
		int l3 = (src[src_offset -1 +2*stride] + 2*src[src_offset -1 +3*stride] + src[src_offset -1 +4*stride] + 2) >> 2;
		int l4 = (src[src_offset -1 +3*stride] + 2*src[src_offset -1 +4*stride] + src[src_offset -1 +5*stride] + 2) >> 2;
		int l5 = (src[src_offset -1 +4*stride] + 2*src[src_offset -1 +5*stride] + src[src_offset -1 +6*stride] + 2) >> 2;
		int l6 = (src[src_offset -1 +5*stride] + 2*src[src_offset -1 +6*stride] + src[src_offset -1 +7*stride] + 2) >> 2;
		int l7 = (src[src_offset -1 +6*stride] + 3*src[src_offset -1 +7*stride] + 2) >> 2;

	    int lt = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 -1*stride] + src[src_offset +0 -1*stride] + 2) >> 2;		
		
	    src[src_offset +(0) +(7)*stride]= (l6 + l7 + 1) >> 1;
	    src[src_offset +(1) +(7)*stride]= (l5 + 2*l6 + l7 + 2) >> 2;
	    src[src_offset +(0) +(6)*stride]=
	    	src[src_offset +(2) +(7)*stride]= (l5 + l6 + 1) >> 1;
	    src[src_offset +(1) +(6)*stride]=
	    	src[src_offset +(3) +(7)*stride]= (l4 + 2*l5 + l6 + 2) >> 2;
	    src[src_offset +(0) +(5)*stride]=
	    	src[src_offset +(2) +(6)*stride]=
	    	src[src_offset +(4) +(7)*stride]= (l4 + l5 + 1) >> 1;
	    src[src_offset +(1) +(5)*stride]=
	    	src[src_offset +(3) +(6)*stride]=
	    	src[src_offset +(5) +(7)*stride]= (l3 + 2*l4 + l5 + 2) >> 2;
	    src[src_offset +(0) +(4)*stride]=
	    	src[src_offset +(2) +(5)*stride]=
	    	src[src_offset +(4) +(6)*stride]=
	    	src[src_offset +(6) +(7)*stride]= (l3 + l4 + 1) >> 1;
	    src[src_offset +(1) +(4)*stride]=
	    	src[src_offset +(3) +(5)*stride]=
	    	src[src_offset +(5) +(6)*stride]=
	    	src[src_offset +(7) +(7)*stride]= (l2 + 2*l3 + l4 + 2) >> 2;
	    src[src_offset +(0) +(3)*stride]=
	    	src[src_offset +(2) +(4)*stride]=
	    	src[src_offset +(4) +(5)*stride]=
	    	src[src_offset +(6) +(6)*stride]= (l2 + l3 + 1) >> 1;
	    src[src_offset +(1) +(3)*stride]=
	    	src[src_offset +(3) +(4)*stride]=
	    	src[src_offset +(5) +(5)*stride]=
	    	src[src_offset +(7) +(6)*stride]= (l1 + 2*l2 + l3 + 2) >> 2;
	    src[src_offset +(0) +(2)*stride]=
	    	src[src_offset +(2) +(3)*stride]=
	    	src[src_offset +(4) +(4)*stride]=
	    	src[src_offset +(6) +(5)*stride]= (l1 + l2 + 1) >> 1;
	    src[src_offset +(1) +(2)*stride]=
	    	src[src_offset +(3) +(3)*stride]=
	    	src[src_offset +(5) +(4)*stride]=
	    	src[src_offset +(7) +(5)*stride]= (l0 + 2*l1 + l2 + 2) >> 2;
	    src[src_offset +(0) +(1)*stride]=
	    	src[src_offset +(2) +(2)*stride]=
	    	src[src_offset +(4) +(3)*stride]=
	    	src[src_offset +(6) +(4)*stride]= (l0 + l1 + 1) >> 1;
	    src[src_offset +(1) +(1)*stride]=
	    	src[src_offset +(3) +(2)*stride]=
	    	src[src_offset +(5) +(3)*stride]=
	    	src[src_offset +(7) +(4)*stride]= (lt + 2*l0 + l1 + 2) >> 2;
	    src[src_offset +(0) +(0)*stride]=
	    	src[src_offset +(2) +(1)*stride]=
	    	src[src_offset +(4) +(2)*stride]=
	    	src[src_offset +(6) +(3)*stride]= (lt + l0 + 1) >> 1;
	    src[src_offset +(1) +(0)*stride]=
	    	src[src_offset +(3) +(1)*stride]=
	    	src[src_offset +(5) +(2)*stride]=
	    	src[src_offset +(7) +(3)*stride]= (l0 + 2*lt + t0 + 2) >> 2;
	    src[src_offset +(2) +(0)*stride]=
	    	src[src_offset +(4) +(1)*stride]=
	    	src[src_offset +(6) +(2)*stride]= (t1 + 2*t0 + lt + 2) >> 2;
	    src[src_offset +(3) +(0)*stride]=
	    	src[src_offset +(5) +(1)*stride]=
	    	src[src_offset +(7) +(2)*stride]= (t2 + 2*t1 + t0 + 2) >> 2;
	    src[src_offset +(4) +(0)*stride]=
	    	src[src_offset +(6) +(1)*stride]= (t3 + 2*t2 + t1 + 2) >> 2;
	    src[src_offset +(5) +(0)*stride]=
	    	src[src_offset +(7) +(1)*stride]= (t4 + 2*t3 + t2 + 2) >> 2;
	    src[src_offset +(6) +(0)*stride]= (t5 + 2*t4 + t3 + 2) >> 2;
	    src[src_offset +(7) +(0)*stride]= (t6 + 2*t5 + t4 + 2) >> 2;
	}
	public static void pred8x8l_vertical_left_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int t0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset +0 -1*stride])
                + 2*src[src_offset +0 -1*stride] + src[src_offset +1 -1*stride] + 2) >> 2;
		int t1 = (src[src_offset +0 -1*stride] + 2*src[src_offset +1 -1*stride] + src[src_offset +2 -1*stride] + 2) >> 2;
		int t2 = (src[src_offset +1 -1*stride] + 2*src[src_offset +2 -1*stride] + src[src_offset +3 -1*stride] + 2) >> 2;
		int t3 = (src[src_offset +2 -1*stride] + 2*src[src_offset +3 -1*stride] + src[src_offset +4 -1*stride] + 2) >> 2;
		int t4 = (src[src_offset +3 -1*stride] + 2*src[src_offset +4 -1*stride] + src[src_offset +5 -1*stride] + 2) >> 2;
		int t5 = (src[src_offset +4 -1*stride] + 2*src[src_offset +5 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;
		int t6 = (src[src_offset +5 -1*stride] + 2*src[src_offset +6 -1*stride] + src[src_offset +7 -1*stride] + 2) >> 2;		
		int t7 = ((has_topright!=0 ? src[src_offset +8 -1*stride] : src[src_offset +7 -1*stride])
		                + 2*src[src_offset +7 -1*stride] + src[src_offset +6 -1*stride] + 2) >> 2;

        int t8, t9, t10, t11, t12/*, t13, t14, t15*/; 
        if(has_topright!=0) { 
            t8 = (src[src_offset +7 -1*stride] + 2*src[src_offset +8 -1*stride] + src[src_offset +9 -1*stride] + 2) >> 2;
            t9 = (src[src_offset +8 -1*stride] + 2*src[src_offset +9 -1*stride] + src[src_offset +10 -1*stride] + 2) >> 2;
            t10 = (src[src_offset +9 -1*stride] + 2*src[src_offset +10 -1*stride] + src[src_offset +11 -1*stride] + 2) >> 2;
            t11 = (src[src_offset +10 -1*stride] + 2*src[src_offset +11 -1*stride] + src[src_offset +12 -1*stride] + 2) >> 2;
            t12 = (src[src_offset +11 -1*stride] + 2*src[src_offset +12 -1*stride] + src[src_offset +13 -1*stride] + 2) >> 2;
            //t13 = (src[src_offset +12 -1*stride] + 2*src[src_offset +13 -1*stride] + src[src_offset +14 -1*stride] + 2) >> 2;
            //t14 = (src[src_offset +13 -1*stride] + 2*src[src_offset +14 -1*stride] + src[src_offset +15 -1*stride] + 2) >> 2;
            //t15 = (src[src_offset +14 -1*stride] + 3*src[src_offset +15 -1*stride] + 2) >> 2;
        } else t8=t9=t10=t11=t12/*=t13=t14=t15*/= src[src_offset +7 -1*stride];

		src[src_offset +(0) +(0)*stride]= (t0 + t1 + 1) >> 1;
	    src[src_offset +(0) +(1)*stride]= (t0 + 2*t1 + t2 + 2) >> 2;
	    src[src_offset +(0) +(2)*stride]=
	    	src[src_offset +(1) +(0)*stride]= (t1 + t2 + 1) >> 1;
	    src[src_offset +(0) +(3)*stride]=
	    	src[src_offset +(1) +(1)*stride]= (t1 + 2*t2 + t3 + 2) >> 2;
	    src[src_offset +(0) +(4)*stride]=
	    	src[src_offset +(1) +(2)*stride]=
	    	src[src_offset +(2) +(0)*stride]= (t2 + t3 + 1) >> 1;
	    src[src_offset +(0) +(5)*stride]=
	    	src[src_offset +(1) +(3)*stride]=
	    	src[src_offset +(2) +(1)*stride]= (t2 + 2*t3 + t4 + 2) >> 2;
	    src[src_offset +(0) +(6)*stride]=
	    	src[src_offset +(1) +(4)*stride]=
	    	src[src_offset +(2) +(2)*stride]=
	    	src[src_offset +(3) +(0)*stride]= (t3 + t4 + 1) >> 1;
	    src[src_offset +(0) +(7)*stride]=
	    	src[src_offset +(1) +(5)*stride]=
	    	src[src_offset +(2) +(3)*stride]=
	    	src[src_offset +(3) +(1)*stride]= (t3 + 2*t4 + t5 + 2) >> 2;
	    src[src_offset +(1) +(6)*stride]=
	    	src[src_offset +(2) +(4)*stride]=
	    	src[src_offset +(3) +(2)*stride]=
	    	src[src_offset +(4) +(0)*stride]= (t4 + t5 + 1) >> 1;
	    src[src_offset +(1) +(7)*stride]=
	    	src[src_offset +(2) +(5)*stride]=
	    	src[src_offset +(3) +(3)*stride]=
	    	src[src_offset +(4) +(1)*stride]= (t4 + 2*t5 + t6 + 2) >> 2;
	    src[src_offset +(2) +(6)*stride]=
	    	src[src_offset +(3) +(4)*stride]=
	    	src[src_offset +(4) +(2)*stride]=
	    	src[src_offset +(5) +(0)*stride]= (t5 + t6 + 1) >> 1;
	    src[src_offset +(2) +(7)*stride]=
	    	src[src_offset +(3) +(5)*stride]=
	    	src[src_offset +(4) +(3)*stride]=
	    	src[src_offset +(5) +(1)*stride]= (t5 + 2*t6 + t7 + 2) >> 2;
	    src[src_offset +(3) +(6)*stride]=
	    	src[src_offset +(4) +(4)*stride]=
	    	src[src_offset +(5) +(2)*stride]=
	    	src[src_offset +(6) +(0)*stride]= (t6 + t7 + 1) >> 1;
	    src[src_offset +(3) +(7)*stride]=
	    	src[src_offset +(4) +(5)*stride]=
	    	src[src_offset +(5) +(3)*stride]=
	    	src[src_offset +(6) +(1)*stride]= (t6 + 2*t7 + t8 + 2) >> 2;
	    src[src_offset +(4) +(6)*stride]=
	    	src[src_offset +(5) +(4)*stride]=
	    	src[src_offset +(6) +(2)*stride]=
	    	src[src_offset +(7) +(0)*stride]= (t7 + t8 + 1) >> 1;
	    src[src_offset +(4) +(7)*stride]=
	    	src[src_offset +(5) +(5)*stride]=
	    	src[src_offset +(6) +(3)*stride]=
	    	src[src_offset +(7) +(1)*stride]= (t7 + 2*t8 + t9 + 2) >> 2;
	    src[src_offset +(5) +(6)*stride]=
	    	src[src_offset +(6) +(4)*stride]=
	    	src[src_offset +(7) +(2)*stride]= (t8 + t9 + 1) >> 1;
	    src[src_offset +(5) +(7)*stride]=
	    	src[src_offset +(6) +(5)*stride]=
	    	src[src_offset +(7) +(3)*stride]= (t8 + 2*t9 + t10 + 2) >> 2;
	    src[src_offset +(6) +(6)*stride]=
	    	src[src_offset +(7) +(4)*stride]= (t9 + t10 + 1) >> 1;
	    src[src_offset +(6) +(7)*stride]=
	    	src[src_offset +(7) +(5)*stride]= (t9 + 2*t10 + t11 + 2) >> 2;
	    src[src_offset +(7) +(6)*stride]= (t10 + t11 + 1) >> 1;
	    src[src_offset +(7) +(7)*stride]= (t10 + 2*t11 + t12 + 2) >> 2;
	}
	public static void pred8x8l_horizontal_up_c(int[] src, int src_offset, int has_topleft, int has_topright, int stride)
	{
	    int l0 = ((has_topleft!=0 ? src[src_offset -1 -1*stride] : src[src_offset -1 +0*stride] ) 
                + 2*src[src_offset -1 +0*stride] + src[src_offset -1 +1*stride] + 2) >> 2; 
		int l1 = (src[src_offset -1 +0*stride] + 2*src[src_offset -1 +1*stride] + src[src_offset -1 +2*stride] + 2) >> 2;
		int l2 = (src[src_offset -1 +1*stride] + 2*src[src_offset -1 +2*stride] + src[src_offset -1 +3*stride] + 2) >> 2;
		int l3 = (src[src_offset -1 +2*stride] + 2*src[src_offset -1 +3*stride] + src[src_offset -1 +4*stride] + 2) >> 2;
		int l4 = (src[src_offset -1 +3*stride] + 2*src[src_offset -1 +4*stride] + src[src_offset -1 +5*stride] + 2) >> 2;
		int l5 = (src[src_offset -1 +4*stride] + 2*src[src_offset -1 +5*stride] + src[src_offset -1 +6*stride] + 2) >> 2;
		int l6 = (src[src_offset -1 +5*stride] + 2*src[src_offset -1 +6*stride] + src[src_offset -1 +7*stride] + 2) >> 2;
		int l7 = (src[src_offset -1 +6*stride] + 3*src[src_offset -1 +7*stride] + 2) >> 2;

		src[src_offset +(0) +(0)*stride]= (l0 + l1 + 1) >> 1;
	    src[src_offset +(1) +(0)*stride]= (l0 + 2*l1 + l2 + 2) >> 2;
	    src[src_offset +(0) +(1)*stride]=
	    	src[src_offset +(2) +(0)*stride]= (l1 + l2 + 1) >> 1;
	    src[src_offset +(1) +(1)*stride]=
	    	src[src_offset +(3) +(0)*stride]= (l1 + 2*l2 + l3 + 2) >> 2;
	    src[src_offset +(0) +(2)*stride]=
	    	src[src_offset +(2) +(1)*stride]=
	    	src[src_offset +(4) +(0)*stride]= (l2 + l3 + 1) >> 1;
	    src[src_offset +(1) +(2)*stride]=
	    	src[src_offset +(3) +(1)*stride]=
	    	src[src_offset +(5) +(0)*stride]= (l2 + 2*l3 + l4 + 2) >> 2;
	    src[src_offset +(0) +(3)*stride]=
	    	src[src_offset +(2) +(2)*stride]=
	    	src[src_offset +(4) +(1)*stride]=
	    	src[src_offset +(6) +(0)*stride]= (l3 + l4 + 1) >> 1;
	    src[src_offset +(1) +(3)*stride]=
	    	src[src_offset +(3) +(2)*stride]=
	    	src[src_offset +(5) +(1)*stride]=
	    	src[src_offset +(7) +(0)*stride]= (l3 + 2*l4 + l5 + 2) >> 2;
	    src[src_offset +(0) +(4)*stride]=
	    	src[src_offset +(2) +(3)*stride]=
	    	src[src_offset +(4) +(2)*stride]=
	    	src[src_offset +(6) +(1)*stride]= (l4 + l5 + 1) >> 1;
	    src[src_offset +(1) +(4)*stride]=
	    	src[src_offset +(3) +(3)*stride]=
	    	src[src_offset +(5) +(2)*stride]=
	    	src[src_offset +(7) +(1)*stride]= (l4 + 2*l5 + l6 + 2) >> 2;
	    src[src_offset +(0) +(5)*stride]=
	    	src[src_offset +(2) +(4)*stride]=
	    	src[src_offset +(4) +(3)*stride]=
	    	src[src_offset +(6) +(2)*stride]= (l5 + l6 + 1) >> 1;
	    src[src_offset +(1) +(5)*stride]=
	    	src[src_offset +(3) +(4)*stride]=
	    	src[src_offset +(5) +(3)*stride]=
	    	src[src_offset +(7) +(2)*stride]= (l5 + 2*l6 + l7 + 2) >> 2;
	    src[src_offset +(0) +(6)*stride]=
	    	src[src_offset +(2) +(5)*stride]=
	    	src[src_offset +(4) +(4)*stride]=
	    	src[src_offset +(6) +(3)*stride]= (l6 + l7 + 1) >> 1;
	    src[src_offset +(1) +(6)*stride]=
	    	src[src_offset +(3) +(5)*stride]=
	    	src[src_offset +(5) +(4)*stride]=
	    	src[src_offset +(7) +(3)*stride]= (l6 + 3*l7 + 2) >> 2;
	    src[src_offset +(0) +(7)*stride]=
	    	src[src_offset +(1) +(7)*stride]=
	    	src[src_offset +(2) +(6)*stride]=
	    	src[src_offset +(2) +(7)*stride]=
	    	src[src_offset +(3) +(6)*stride]=
	    src[src_offset +(3) +(7)*stride]=
	    	src[src_offset +(4) +(5)*stride]=
	    	src[src_offset +(4) +(6)*stride]=
	    	src[src_offset +(4) +(7)*stride]=
	    	src[src_offset +(5) +(5)*stride]=
	    src[src_offset +(5) +(6)*stride]=
	    	src[src_offset +(5) +(7)*stride]=
	    	src[src_offset +(6) +(4)*stride]=
	    	src[src_offset +(6) +(5)*stride]=
	    	src[src_offset +(6) +(6)*stride]=
	    src[src_offset +(6) +(7)*stride]=
	    	src[src_offset +(7) +(4)*stride]=
	    	src[src_offset +(7) +(5)*stride]=
	    	src[src_offset +(7) +(6)*stride]=
	    	src[src_offset +(7) +(7)*stride]= l7;
	}

	public static void pred4x4_vertical_add_c(int[] pix, int pix_offset, short[] block, int block_offset, int stride){
	    int i;
	    pix_offset -= stride;
	    for(i=0; i<4; i++){
	        int v = pix[pix_offset +0];
	        pix[pix_offset +1*stride]= v += block[block_offset +0];
	        pix[pix_offset +2*stride]= v += block[block_offset +4];
	        pix[pix_offset +3*stride]= v += block[block_offset +8];
	        pix[pix_offset +4*stride]= v +  block[block_offset +12];
	        pix_offset++;
	        block_offset++;
	    }
	}

	public static void pred4x4_horizontal_add_c(int[] pix, int pix_offset, short[] block, int block_offset, int stride){
	    int i;
	    for(i=0; i<4; i++){
	        int v = pix[pix_offset -1];
	        pix[pix_offset +0]= v += block[block_offset +0];
	        pix[pix_offset +1]= v += block[block_offset +1];
	        pix[pix_offset +2]= v += block[block_offset +2];
	        pix[pix_offset +3]= v +  block[block_offset +3];
	        pix_offset += stride;
	        block_offset+= 4;
	    }
	}

	public static void pred8x8l_vertical_add_c(int[] pix, int pix_offset, short[] block, int block_offset, int stride){
	    int i;
	    pix_offset -= stride;
	    for(i=0; i<8; i++){
	        int v = pix[pix_offset +0];
	        pix[pix_offset +1*stride]= v += block[block_offset +0];
	        pix[pix_offset +2*stride]= v += block[block_offset +8];
	        pix[pix_offset +3*stride]= v += block[block_offset +16];
	        pix[pix_offset +4*stride]= v += block[block_offset +24];
	        pix[pix_offset +5*stride]= v += block[block_offset +32];
	        pix[pix_offset +6*stride]= v += block[block_offset +40];
	        pix[pix_offset +7*stride]= v += block[block_offset +48];
	        pix[pix_offset +8*stride]= v +  block[block_offset +56];
	        pix_offset++;
	        block_offset++;
	    }
	}

	public static void pred8x8l_horizontal_add_c(int[] pix, int pix_offset, short[] block, int block_offset, int stride){
	    int i;
	    for(i=0; i<8; i++){
	        int v = pix[pix_offset -1];
	        pix[pix_offset +0]= v += block[block_offset +0];
	        pix[pix_offset +1]= v += block[block_offset +1];
	        pix[pix_offset +2]= v += block[block_offset +2];
	        pix[pix_offset +3]= v += block[block_offset +3];
	        pix[pix_offset +4]= v += block[block_offset +4];
	        pix[pix_offset +5]= v += block[block_offset +5];
	        pix[pix_offset +6]= v += block[block_offset +6];
	        pix[pix_offset +7]= v +  block[block_offset +7];
	        pix_offset += stride;
	        block_offset+= 8;
	    }
	}

	public static void pred16x16_vertical_add_c(int[] pix, int pix_offset, int[] block_offset, int block_offset_offset, short[] block, int _block_offset, int stride){
	    int i;
	    for(i=0; i<16; i++)
	        pred4x4_vertical_add_c(pix, pix_offset + block_offset[block_offset_offset + i], block, _block_offset + i*16, stride);
	}

	public static void pred16x16_horizontal_add_c(int[] pix, int pix_offset, int[] block_offset, int block_offset_offset, short[] block, int _block_offset, int stride){
	    int i;
	    for(i=0; i<16; i++)
	        pred4x4_horizontal_add_c(pix, pix_offset + block_offset[block_offset_offset + i], block, _block_offset + i*16, stride);
	}

	public static void pred8x8_vertical_add_c(int[] pix, int pix_offset, int[] block_offset, int block_offset_offset, short[] block, int _block_offset, int stride){
	    int i;
	    for(i=0; i<4; i++)
	        pred4x4_vertical_add_c(pix, pix_offset + block_offset[block_offset_offset + i], block, _block_offset + i*16, stride);
	}

	public static void pred8x8_horizontal_add_c(int[] pix, int pix_offset, int[] block_offset, int block_offset_offset, short[] block, int _block_offset, int stride){
	    int i;
	    for(i=0; i<4; i++)
	        pred4x4_horizontal_add_c(pix, pix_offset + block_offset[block_offset_offset + i], block, _block_offset + i*16, stride);
	}

	
}
