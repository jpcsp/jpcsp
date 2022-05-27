package com.twilight.h264.decoder;

import com.twilight.h264.util.*;

public class MpegEncContext {
	
	public static final int AVMEDIA_TYPE_VIDEO = 0;

	public static final int AV_OPT_FLAG_DECODING_PARAM  = 2;   ///< a generic parameter which can be set by the user for demuxing or decoding
	public static final int AV_OPT_FLAG_VIDEO_PARAM     = 16;
	
	/////////////////
	// AVCODEC PARAMS

	public static final int /*uint8_t*/[] ff_default_chroma_qscale_table/*[32]*/={
	//  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
	    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
	};

	public static final int /*uint8_t*/[] ff_mpeg1_dc_scale_table/*[128]*/={
	//  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
	    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
	    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
	    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
	    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
	};	
	
    /**
     * Size of the frame reordering buffer in the decoder.
     * For MPEG-2 it is 1 IPB or 0 low delay IP.
     * - encoding: Set by libavcodec.
     * - decoding: Set by libavcodec.
     */
    public int has_b_frames;
	
	
    public static final int FF_QSCALE_TYPE_H264  =2;
    
	/////////////////

	public static final int VP_START            =1;          ///< current MB is the first after a resync marker
	public static final int AC_ERROR            =2;
	public static final int DC_ERROR            =4;
	public static final int MV_ERROR            =8;
	public static final int AC_END              =16;
	public static final int DC_END              =32;
	public static final int MV_END              =64;
	
	public static final int FF_BUFFER_TYPE_INTERNAL =1;
	public static final int FF_BUFFER_TYPE_USER     =2; ///< direct rendering buffers (image is (de)allocated by user)
	public static final int FF_BUFFER_TYPE_SHARED   =4; ///< Buffer from somewhere else; don't deallocate image (data/base), all other tables are not shared.
	public static final int FF_BUFFER_TYPE_COPY     =8; ///< Just a (modified) copy of some other buffer, don't deallocate anything.
	
	/* picture type */
	public static final int PICT_TOP_FIELD     =1;
	public static final int PICT_BOTTOM_FIELD  =2;
	public static final int PICT_FRAME         =3;
	
	/* encoding support
	   These flags can be passed in AVCodecContext.flags before initialization.
	   Note: Not everything is supported yet.
	*/

	public static final int CODEC_FLAG_QSCALE =0x0002;  ///< Use fixed qscale.
	public static final int CODEC_FLAG_4MV    =0x0004;  ///< 4 MV per MB allowed / advanced prediction for H.263.
	public static final int CODEC_FLAG_QPEL   =0x0010;  ///< Use qpel MC.
	public static final int CODEC_FLAG_GMC    =0x0020;  ///< Use GMC.
	public static final int CODEC_FLAG_MV0    =0x0040;  ///< Always try a MB with MV=<0,0>.
	public static final int CODEC_FLAG_PART   =0x0080;  ///< Use data partitioning.

	/**
	 * The parent program guarantees that the input for B-frames containing
	 * streams is not written to for at least s->max_b_frames+1 frames, if
	 * this is not set the input will be copied.
	 */
	public static final int CODEC_FLAG_INPUT_PRESERVED =0x0100;
	public static final int CODEC_FLAG_PASS1           =0x0200;   ///< Use internal 2pass ratecontrol in first pass mode.
	public static final int CODEC_FLAG_PASS2           =0x0400;   ///< Use internal 2pass ratecontrol in second pass mode.
	public static final int CODEC_FLAG_EXTERN_HUFF     =0x01000;   ///< Use external Huffman table (for MJPEG).
	public static final int CODEC_FLAG_GRAY            =0x02000;   ///< Only decode/encode grayscale.
	public static final int CODEC_FLAG_EMU_EDGE        =0x04000;   ///< Don't draw edges.
	public static final int CODEC_FLAG_PSNR            =0x08000;   ///< error[?] variables will be set during encoding.
	public static final int CODEC_FLAG_TRUNCATED       =0x00010000; /** Input bitstream might be truncated at a random
	                                                  location instead of only at frame boundaries. */
	public static final int CODEC_FLAG_NORMALIZE_AQP  =0x00020000; ///< Normalize adaptive quantization.
	public static final int CODEC_FLAG_INTERLACED_DCT =0x00040000; ///< Use interlaced DCT.
	public static final int CODEC_FLAG_LOW_DELAY      =0x00080000; ///< Force low delay.
	public static final int CODEC_FLAG_ALT_SCAN       =0x00100000; ///< Use alternate scan.
	public static final int CODEC_FLAG_GLOBAL_HEADER  =0x00400000; ///< Place global headers in extradata instead of every keyframe.
	public static final int CODEC_FLAG_BITEXACT       =0x00800000; ///< Use only bitexact stuff (except (I)DCT).
	/* Fx : Flag for h263+ extra options */
	public static final int CODEC_FLAG_AC_PRED        =0x01000000; ///< H.263 advanced intra coding / MPEG-4 AC prediction
	public static final int CODEC_FLAG_H263P_UMV      =0x02000000; ///< unlimited motion vector
	public static final int CODEC_FLAG_CBP_RD         =0x04000000; ///< Use rate distortion optimization for cbp.
	public static final int CODEC_FLAG_QP_RD          =0x08000000; ///< Use rate distortion optimization for qp selectioon.
	public static final int CODEC_FLAG_H263P_AIV      =0x00000008; ///< H.263 alternative inter VLC
	public static final int CODEC_FLAG_OBMC           =0x00000001; ///< OBMC
	public static final int CODEC_FLAG_LOOP_FILTER    =0x00000800; ///< loop filter
	public static final int CODEC_FLAG_H263P_SLICE_STRUCT =0x10000000;
	public static final int CODEC_FLAG_INTERLACED_ME  =0x20000000; ///< interlaced motion estimation
	public static final int CODEC_FLAG_SVCD_SCAN_OFFSET =0x40000000; ///< Will reserve space for SVCD scan offset user data.
	public static final int CODEC_FLAG_CLOSED_GOP     =0x80000000;
	public static final int CODEC_FLAG2_FAST          =0x00000001; ///< Allow non spec compliant speedup tricks.
	public static final int CODEC_FLAG2_STRICT_GOP    =0x00000002; ///< Strictly enforce GOP size.
	public static final int CODEC_FLAG2_NO_OUTPUT     =0x00000004; ///< Skip bitstream encoding.
	public static final int CODEC_FLAG2_LOCAL_HEADER  =0x00000008; ///< Place global headers at every keyframe instead of in extradata.
	public static final int CODEC_FLAG2_BPYRAMID      =0x00000010; ///< H.264 allow B-frames to be used as references.
	public static final int CODEC_FLAG2_WPRED         =0x00000020; ///< H.264 weighted biprediction for B-frames
	public static final int CODEC_FLAG2_MIXED_REFS    =0x00000040; ///< H.264 one reference per partition, as opposed to one reference per macroblock
	public static final int CODEC_FLAG2_8X8DCT        =0x00000080; ///< H.264 high profile 8x8 transform
	public static final int CODEC_FLAG2_FASTPSKIP     =0x00000100; ///< H.264 fast pskip
	public static final int CODEC_FLAG2_AUD           =0x00000200; ///< H.264 access unit delimiters
	public static final int CODEC_FLAG2_BRDO          =0x00000400; ///< B-frame rate-distortion optimization
	public static final int CODEC_FLAG2_INTRA_VLC     =0x00000800; ///< Use MPEG-2 intra VLC table.
	public static final int CODEC_FLAG2_MEMC_ONLY     =0x00001000; ///< Only do ME/MC (I frames -> ref, P frame -> ME+MC).
	public static final int CODEC_FLAG2_DROP_FRAME_TIMECODE =0x00002000; ///< timecode is in drop frame format.
	public static final int CODEC_FLAG2_SKIP_RD       =0x00004000; ///< RD optimal MB level residual skipping
	public static final int CODEC_FLAG2_CHUNKS        =0x00008000; ///< Input bitstream might be truncated at a packet boundaries instead of only at frame boundaries.
	public static final int CODEC_FLAG2_NON_LINEAR_QUANT =0x00010000; ///< Use MPEG-2 nonlinear quantizer.
	public static final int CODEC_FLAG2_BIT_RESERVOIR =0x00020000; ///< Use a bit reservoir when encoding if possible
	public static final int CODEC_FLAG2_MBTREE        =0x00040000; ///< Use macroblock tree ratecontrol (x264 only)
	public static final int CODEC_FLAG2_PSY           =0x00080000; ///< Use psycho visual optimizations.
	public static final int CODEC_FLAG2_SSIM          =0x00100000; ///< Compute SSIM during encoding, error[] values are undefined.
	public static final int CODEC_FLAG2_INTRA_REFRESH =0x00200000; ///< Use periodic insertion of intra blocks instead of keyframes.
	
	public static final int FF_BUG_AUTODETECT       =1;  ///< autodetection
	public static final int FF_BUG_OLD_MSMPEG4      =2;
	public static final int FF_BUG_XVID_ILACE       =4;
	public static final int FF_BUG_UMP4             =8;
	public static final int FF_BUG_NO_PADDING       =16;
	public static final int FF_BUG_AMV              =32;
	public static final int FF_BUG_AC_VLC           =0;  ///< Will be removed, libavcodec can now handle these non-compliant files by default.
	public static final int FF_BUG_QPEL_CHROMA      =64;
	public static final int FF_BUG_STD_QPEL         =128;
	public static final int FF_BUG_QPEL_CHROMA2     =256;
	public static final int FF_BUG_DIRECT_BLOCKSIZE =512;
	public static final int FF_BUG_EDGE             =1024;
	public static final int FF_BUG_HPEL_CHROMA      =2048;
	public static final int FF_BUG_DC_CLIP          =4096;
	public static final int FF_BUG_MS               =8192; ///< Work around various bugs in Microsoft's broken decoders.
	public static final int FF_BUG_TRUNCATED       =16384;
	
	//enum AVDiscard{
	    /* We leave some space between them for extensions (drop some
	     * keyframes for intra-only or drop just some bidir frames). */
	public static final int     AVDISCARD_NONE   =-16; ///< discard nothing
	public static final int     AVDISCARD_DEFAULT=  0; ///< discard useless packets like 0 size packets in avi
	public static final int     AVDISCARD_NONREF =  8; ///< discard all non reference
	public static final int     AVDISCARD_BIDIR  = 16; ///< discard all bidirectional frames
	public static final int     AVDISCARD_NONKEY = 32; ///< discard all frames except keyframes
	public static final int     AVDISCARD_ALL    = 48; ///< discard all
	//};
	
	public static final int AVCOL_RANGE_UNSPECIFIED=0;
	public static final int AVCOL_RANGE_MPEG       =1; ///< the normal 219*2^(n-8) "MPEG" YUV ranges
	public static final int AVCOL_RANGE_JPEG       =2; ///< the normal     2^n-1   "JPEG" YUV ranges
	public static final int AVCOL_RANGE_NB         =3; ///< Not part of ABI
	
	//enum AVColorPrimaries{
	public static final int    AVCOL_PRI_BT709      =1; ///< also ITU-R BT1361 / IEC 61966-2-4 / SMPTE RP177 Annex B
	public static final int    AVCOL_PRI_UNSPECIFIED=2;
	public static final int    AVCOL_PRI_BT470M     =4;
	public static final int    AVCOL_PRI_BT470BG    =5; ///< also ITU-R BT601-6 625 / ITU-R BT1358 625 / ITU-R BT1700 625 PAL & SECAM
	public static final int    AVCOL_PRI_SMPTE170M  =6; ///< also ITU-R BT601-6 525 / ITU-R BT1358 525 / ITU-R BT1700 NTSC
	public static final int    AVCOL_PRI_SMPTE240M  =7; ///< functionally identical to above
	public static final int    AVCOL_PRI_FILM       =8;
	public static final int    AVCOL_PRI_NB         =9; ///< Not part of ABI
	//};
	
	//enum AVColorTransferCharacteristic{
	public static final int     AVCOL_TRC_BT709      =1; ///< also ITU-R BT1361
	public static final int     AVCOL_TRC_UNSPECIFIED=2;
	public static final int     AVCOL_TRC_GAMMA22    =4; ///< also ITU-R BT470M / ITU-R BT1700 625 PAL & SECAM
	public static final int     AVCOL_TRC_GAMMA28    =5; ///< also ITU-R BT470BG
	public static final int     AVCOL_TRC_NB         =6; ///< Not part of ABI
	//};
	
	//enum AVColorSpace{
	public static final int    AVCOL_SPC_RGB        =0;
	public static final int    AVCOL_SPC_BT709      =1; ///< also ITU-R BT1361 / IEC 61966-2-4 xvYCC709 / SMPTE RP177 Annex B
	public static final int    AVCOL_SPC_UNSPECIFIED=2;
	public static final int    AVCOL_SPC_FCC        =4;
	public static final int    AVCOL_SPC_BT470BG    =5; ///< also ITU-R BT601-6 625 / ITU-R BT1358 625 / ITU-R BT1700 625 PAL & SECAM / IEC 61966-2-4 xvYCC601
	public static final int    AVCOL_SPC_SMPTE170M  =6; ///< also ITU-R BT601-6 525 / ITU-R BT1358 525 / ITU-R BT1700 NTSC / functionally identical to above
	public static final int    AVCOL_SPC_SMPTE240M  =7;
	public static final int    AVCOL_SPC_NB         =8; ///< Not part of ABI
	//};
	
	//enum PixelFormat {
    public static final int PIX_FMT_NONE= -1;
    public static final int PIX_FMT_YUV420P = 1;   ///< planar YUV 4:2:0 = 0; 12bpp = 0; (1 Cr & Cb sample per 2x2 Y samples)
    public static final int PIX_FMT_YUYV422 = 2;   ///< packed YUV 4:2:2 = 0; 16bpp = 0; Y0 Cb Y1 Cr
    public static final int PIX_FMT_RGB24 = 3;     ///< packed RGB 8:8:8 = 0; 24bpp = 0; RGBRGB...
    public static final int PIX_FMT_BGR24 = 4;     ///< packed RGB 8:8:8 = 0; 24bpp = 0; BGRBGR...
    public static final int PIX_FMT_YUV422P = 5;   ///< planar YUV 4:2:2 = 0; 16bpp = 0; (1 Cr & Cb sample per 2x1 Y samples)
    public static final int PIX_FMT_YUV444P = 6;   ///< planar YUV 4:4:4 = 0; 24bpp = 0; (1 Cr & Cb sample per 1x1 Y samples)
    public static final int PIX_FMT_YUV410P = 7;   ///< planar YUV 4:1:0 = 0;  9bpp = 0; (1 Cr & Cb sample per 4x4 Y samples)
    public static final int PIX_FMT_YUV411P = 8;   ///< planar YUV 4:1:1 = 0; 12bpp = 0; (1 Cr & Cb sample per 4x1 Y samples)
    public static final int PIX_FMT_GRAY8 = 9;     ///<        Y         = 0;  8bpp
    public static final int PIX_FMT_MONOWHITE = 10; ///<        Y         = 0;  1bpp = 0; 0 is white = 0; 1 is black = 0; in each byte pixels are ordered from the msb to the lsb
    public static final int PIX_FMT_MONOBLACK = 11; ///<        Y         = 0;  1bpp = 0; 0 is black = 0; 1 is white = 0; in each byte pixels are ordered from the msb to the lsb
    public static final int PIX_FMT_PAL8 = 12;      ///< 8 bit with public static final int PIX_FMT_RGB32 palette
    public static final int PIX_FMT_YUVJ420P = 13;  ///< planar YUV 4:2:0 = 0; 12bpp = 0; full scale (JPEG) = 0; deprecated in favor of public static final int PIX_FMT_YUV420P and setting color_range
    public static final int PIX_FMT_YUVJ422P = 14;  ///< planar YUV 4:2:2 = 0; 16bpp = 0; full scale (JPEG) = 0; deprecated in favor of public static final int PIX_FMT_YUV422P and setting color_range
    public static final int PIX_FMT_YUVJ444P = 15;  ///< planar YUV 4:4:4 = 0; 24bpp = 0; full scale (JPEG) = 0; deprecated in favor of public static final int PIX_FMT_YUV444P and setting color_range
    public static final int PIX_FMT_XVMC_MPEG2_MC = 16;///< XVideo Motion Acceleration via common packet passing
    public static final int PIX_FMT_XVMC_MPEG2_IDCT = 17;
    public static final int PIX_FMT_UYVY422 = 18;   ///< packed YUV 4:2:2 = 0; 16bpp = 0; Cb Y0 Cr Y1
    public static final int PIX_FMT_UYYVYY411 = 19; ///< packed YUV 4:1:1 = 0; 12bpp = 0; Cb Y0 Y1 Cr Y2 Y3
    public static final int PIX_FMT_BGR8 = 20;      ///< packed RGB 3:3:2 = 0;  8bpp = 0; (msb)2B 3G 3R(lsb)
    public static final int PIX_FMT_BGR4 = 21;      ///< packed RGB 1:2:1 bitstream = 0;  4bpp = 0; (msb)1B 2G 1R(lsb) = 0; a byte contains two pixels = 0; the first pixel in the byte is the one composed by the 4 msb bits
    public static final int PIX_FMT_BGR4_BYTE = 22; ///< packed RGB 1:2:1 = 0;  8bpp = 0; (msb)1B 2G 1R(lsb)
    public static final int PIX_FMT_RGB8 = 23;      ///< packed RGB 3:3:2 = 0;  8bpp = 0; (msb)2R 3G 3B(lsb)
    public static final int PIX_FMT_RGB4 = 24;      ///< packed RGB 1:2:1 bitstream = 0;  4bpp = 0; (msb)1R 2G 1B(lsb) = 0; a byte contains two pixels = 0; the first pixel in the byte is the one composed by the 4 msb bits
    public static final int PIX_FMT_RGB4_BYTE = 25; ///< packed RGB 1:2:1 = 0;  8bpp = 0; (msb)1R 2G 1B(lsb)
    public static final int PIX_FMT_NV12 = 26;      ///< planar YUV 4:2:0 = 0; 12bpp = 0; 1 plane for Y and 1 plane for the UV components = 0; which are interleaved (first byte U and the following byte V)
    public static final int PIX_FMT_NV21 = 27;      ///< as above = 0; but U and V bytes are swapped

    public static final int PIX_FMT_ARGB = 28;      ///< packed ARGB 8:8:8:8 = 0; 32bpp = 0; ARGBARGB...
    public static final int PIX_FMT_RGBA = 29;      ///< packed RGBA 8:8:8:8 = 0; 32bpp = 0; RGBARGBA...
    public static final int PIX_FMT_ABGR = 30;      ///< packed ABGR 8:8:8:8 = 0; 32bpp = 0; ABGRABGR...
    public static final int PIX_FMT_BGRA = 31;      ///< packed BGRA 8:8:8:8 = 0; 32bpp = 0; BGRABGRA...

    public static final int PIX_FMT_GRAY16BE = 32;  ///<        Y         = 0; 16bpp = 0; big-endian
    public static final int PIX_FMT_GRAY16LE = 33;  ///<        Y         = 0; 16bpp = 0; little-endian
    public static final int PIX_FMT_YUV440P = 34;   ///< planar YUV 4:4:0 (1 Cr & Cb sample per 1x2 Y samples)
    public static final int PIX_FMT_YUVJ440P = 35;  ///< planar YUV 4:4:0 full scale (JPEG) = 0; deprecated in favor of public static final int PIX_FMT_YUV440P and setting color_range
    public static final int PIX_FMT_YUVA420P = 36;  ///< planar YUV 4:2:0 = 0; 20bpp = 0; (1 Cr & Cb sample per 2x2 Y & A samples)
    public static final int PIX_FMT_VDPAU_H264 = 37;///< H.264 HW decoding with VDPAU = 0; data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
    public static final int PIX_FMT_VDPAU_MPEG1 = 38;///< MPEG-1 HW decoding with VDPAU = 0; data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
    public static final int PIX_FMT_VDPAU_MPEG2 = 39;///< MPEG-2 HW decoding with VDPAU = 0; data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
    public static final int PIX_FMT_VDPAU_WMV3 = 40;///< WMV3 HW decoding with VDPAU = 0; data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
    public static final int PIX_FMT_VDPAU_VC1 = 41; ///< VC-1 HW decoding with VDPAU = 0; data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
    public static final int PIX_FMT_RGB48BE = 42;   ///< packed RGB 16:16:16 = 0; 48bpp = 0; 16R = 0; 16G = 0; 16B = 0; the 2-byte value for each R/G/B component is stored as big-endian
    public static final int PIX_FMT_RGB48LE = 43;   ///< packed RGB 16:16:16 = 0; 48bpp = 0; 16R = 0; 16G = 0; 16B = 0; the 2-byte value for each R/G/B component is stored as little-endian

    public static final int PIX_FMT_RGB565BE = 44;  ///< packed RGB 5:6:5 = 0; 16bpp = 0; (msb)   5R 6G 5B(lsb) = 0; big-endian
    public static final int PIX_FMT_RGB565LE = 45;  ///< packed RGB 5:6:5 = 0; 16bpp = 0; (msb)   5R 6G 5B(lsb) = 0; little-endian
    public static final int PIX_FMT_RGB555BE = 46;  ///< packed RGB 5:5:5 = 0; 16bpp = 0; (msb)1A 5R 5G 5B(lsb) = 0; big-endian = 0; most significant bit to 0
    public static final int PIX_FMT_RGB555LE = 47;  ///< packed RGB 5:5:5 = 0; 16bpp = 0; (msb)1A 5R 5G 5B(lsb) = 0; little-endian = 0; most significant bit to 0

    public static final int PIX_FMT_BGR565BE = 48;  ///< packed BGR 5:6:5 = 0; 16bpp = 0; (msb)   5B 6G 5R(lsb) = 0; big-endian
    public static final int PIX_FMT_BGR565LE = 49;  ///< packed BGR 5:6:5 = 0; 16bpp = 0; (msb)   5B 6G 5R(lsb) = 0; little-endian
    public static final int PIX_FMT_BGR555BE = 50;  ///< packed BGR 5:5:5 = 0; 16bpp = 0; (msb)1A 5B 5G 5R(lsb) = 0; big-endian = 0; most significant bit to 1
    public static final int PIX_FMT_BGR555LE = 51;  ///< packed BGR 5:5:5 = 0; 16bpp = 0; (msb)1A 5B 5G 5R(lsb) = 0; little-endian = 0; most significant bit to 1

    public static final int PIX_FMT_VAAPI_MOCO = 52; ///< HW acceleration through VA API at motion compensation entry-point = 0; Picture.data[3] contains a vaapi_render_state struct which contains macroblocks as well as various fields extracted from headers
    public static final int PIX_FMT_VAAPI_IDCT = 53; ///< HW acceleration through VA API at IDCT entry-point = 0; Picture.data[3] contains a vaapi_render_state struct which contains fields extracted from headers
    public static final int PIX_FMT_VAAPI_VLD = 54;  ///< HW decoding through VA API = 0; Picture.data[3] contains a vaapi_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers

    public static final int PIX_FMT_YUV420P16LE = 55;  ///< planar YUV 4:2:0 = 0; 24bpp = 0; (1 Cr & Cb sample per 2x2 Y samples) = 0; little-endian
    public static final int PIX_FMT_YUV420P16BE = 56;  ///< planar YUV 4:2:0 = 0; 24bpp = 0; (1 Cr & Cb sample per 2x2 Y samples) = 0; big-endian
    public static final int PIX_FMT_YUV422P16LE = 57;  ///< planar YUV 4:2:2 = 0; 32bpp = 0; (1 Cr & Cb sample per 2x1 Y samples) = 0; little-endian
    public static final int PIX_FMT_YUV422P16BE = 58;  ///< planar YUV 4:2:2 = 0; 32bpp = 0; (1 Cr & Cb sample per 2x1 Y samples) = 0; big-endian
    public static final int PIX_FMT_YUV444P16LE = 59;  ///< planar YUV 4:4:4 = 0; 48bpp = 0; (1 Cr & Cb sample per 1x1 Y samples) = 0; little-endian
    public static final int PIX_FMT_YUV444P16BE = 60;  ///< planar YUV 4:4:4 = 0; 48bpp = 0; (1 Cr & Cb sample per 1x1 Y samples) = 0; big-endian
    public static final int PIX_FMT_VDPAU_MPEG4 = 61;  ///< MPEG4 HW decoding with VDPAU = 0; data[0] contains a vdpau_render_state struct which contains the bitstream of the slices as well as various fields extracted from headers
    public static final int PIX_FMT_DXVA2_VLD = 62;    ///< HW decoding through DXVA2 = 0; Picture.data[3] contains a LPDIRECT3DSURFACE9 pointer

    public static final int PIX_FMT_RGB444BE = 63;  ///< packed RGB 4:4:4 = 0; 16bpp = 0; (msb)4A 4R 4G 4B(lsb) = 0; big-endian = 0; most significant bits to 0
    public static final int PIX_FMT_RGB444LE = 64;  ///< packed RGB 4:4:4 = 0; 16bpp = 0; (msb)4A 4R 4G 4B(lsb) = 0; little-endian = 0; most significant bits to 0
    public static final int PIX_FMT_BGR444BE = 65;  ///< packed BGR 4:4:4 = 0; 16bpp = 0; (msb)4A 4B 4G 4R(lsb) = 0; big-endian = 0; most significant bits to 1
    public static final int PIX_FMT_BGR444LE = 66;  ///< packed BGR 4:4:4 = 0; 16bpp = 0; (msb)4A 4B 4G 4R(lsb) = 0; little-endian = 0; most significant bits to 1
    public static final int PIX_FMT_Y400A = 67;     ///< 8bit gray = 0; 8bit alpha
    public static final int PIX_FMT_NB = 68;        ///< number of pixel formats = 0; DO NOT USE THIS if you want to link with shared libav* because the number of formats might differ between versions
//};
	
	public static final int[] /*enum PixelFormat*/ ff_hwaccel_pixfmt_list_420 = {
	    PIX_FMT_DXVA2_VLD,
	    PIX_FMT_VAAPI_VLD,
	    PIX_FMT_YUV420P,
	    PIX_FMT_NONE
	};

	public static final int SLICE_FLAG_CODED_ORDER    =0x0001; ///< draw_horiz_band() is called in coded order instead of display
	public static final int SLICE_FLAG_ALLOW_FIELD    =0x0002; ///< allow draw_horiz_band() with field slices (MPEG2 field pics)
	public static final int SLICE_FLAG_ALLOW_PLANE    =0x0004; ///< allow draw_horiz_band() with 1 component at a time (SVQ1)
	
	public static final int EDGE_WIDTH = 16;
	public static final long AV_NOPTS_VALUE          = (0x8000000000000000l);
	public static final int AV_TIME_BASE            =1000000;
	//public static final int AV_TIME_BASE_Q          (AVRational){1, AV_TIME_BASE}
	
	public static final int INTERNAL_BUFFER_SIZE = 32;
	public static final int STRIDE_ALIGN = 8;
	
	public static final int AVCHROMA_LOC_LEFT       =1; ///< mpeg2/4, h264 default
	
	/**
	 * Required number of additionally allocated bytes at the end of the input bitstream for decoding.
	 * This is mainly needed because some optimized bitstream readers read
	 * 32 or 64 bit at once and could read over the end.<br>
	 * Note: If the first 23 bits of the additional bytes are not 0, then damaged
	 * MPEG bitstreams could cause overread and segfault.
	 */
	// Nok: Prevent bug?? in ffmpeg that the decoder may overflow?
	public static final int FF_INPUT_BUFFER_PADDING_SIZE = 80;
	
	/// mysterious variable, I think it's not used.
	public static final int CONFIG_GRAY = 0;
	
	public static final int MAX_PICTURE_COUNT = 32;
	
	public static final int FMT_H264 = 4;

	public static final int FF_COMPLIANCE_VERY_STRICT   =2; ///< Strictly conform to an older more strict version of the spec or reference software.
	public static final int FF_COMPLIANCE_STRICT        =1; ///< Strictly conform to all the things in the spec no matter what consequences.
	public static final int FF_COMPLIANCE_NORMAL        =0;
	
	// Back pointer to decoder class
	public H264Decoder codec;
	
	/**
	 * strictly follow the standard (MPEG4, ...).
	 * - encoding: Set by user.
	 * - decoding: Set by user.
	 * Setting this to STRICT or higher means the encoder and decoder will
	 * generally do stupid things, whereas setting it to unofficial or lower
	 * will mean the encoder might produce output that is not supported by all
	 * spec-compliant decoders. Decoders don't differentiate between normal,
	 * unofficial and experimental (that is, they always try to decode things
	 * when they can) unless they are explicitly asked to behave stupidly
	 * (=strictly conform to the specs)
	 */
	public int strict_std_compliance;

	public int alternate_scan;

	public int qscale;                 ///< QP
    public int[] block_wrap = new int[6];
    public int codec_tag;             ///< internal codec_tag upper case converted from avctx codec_tag
    public int stream_codec_tag;      ///< internal stream_codec_tag upper case converted from avctx stream_codec_tag

    /* The following data should not be initialized. */
    /**
     * Samples per packet, initialized when calling 'init'.
     */
    public int frame_size;
    public int frame_number;   ///< audio or video frame number
    
    public static int MV_DIR_FORWARD = 1;
    public static int MV_DIR_BACKWARD = 2;
    public static int MV_DIR_DIRECT = 4;
    public int mv_dir; // direction mode of movie
    
    /* scantables */
    public ScanTable intra_scantable = new ScanTable();
    public ScanTable intra_h_scantable = new ScanTable();
    public ScanTable intra_v_scantable = new ScanTable();
    public ScanTable inter_scantable = new ScanTable(); ///< if inter == intra then intra should be used to reduce tha cache usage
    
    /**
     * This defines the location of chroma samples.
     * - encoding: Set by user
     * - decoding: Set by libavcodec
     */
    /*enum AVChromaLocation*/public int chroma_sample_location;
	
    /* noise reduction */
    /*
    int (*dct_error_sum)[64];
    int dct_count[2];
    uint16_t (*dct_offset)[64];
    */
    public int[][] dct_error_sum;
    public int[] dct_count = new int[2];
    public int[][] dct_offset;
    
    /* macroblock layer */
	public int mb_x, mb_y;
	public int mb_skip_run;
	public int mb_intra;
	public int thread_count = 1;
    public int dropable;
    public int pict_type;              ///< FF_I_TYPE, FF_P_TYPE, FF_B_TYPE, ...
    
    public int width, height;///< picture size. must be a multiple of 16
    public AVRational sample_aspect_ratio;
    
    public int decode; // Always be 1
    
    /**
     * This is the fundamental unit of time (in seconds) in terms
     * of which frame timestamps are represented. For fixed-fps content,
     * timebase should be 1/framerate and timestamp increments should be
     * identically 1.
     * - encoding: MUST be set by user.
     * - decoding: Set by libavcodec.
     */
    public AVRational time_base;    

    /**
     * IDCT algorithm, see FF_IDCT_* below.
     * - encoding: Set by user.
     * - decoding: Set by user.
     */
    public int idct_algo;
    
    /**
    *
    * - encoding: unused
    * - decoding: Set by user.
    */
   /*enum AVDiscard */public int skip_loop_filter;
    
    /**
     * MPEG vs JPEG YUV range.
     * - encoding: Set by user
     * - decoding: Set by libavcodec
     */
    /*enum AVColorRange*/ public int color_range;
    
    /**
     * Chromaticity coordinates of the source primaries.
     * - encoding: Set by user
     * - decoding: Set by libavcodec
     */
    /*enum AVColorPrimaries*/ public int color_primaries;
    
    /**
     * profile
     * - encoding: Set by user.
     * - decoding: Set by libavcodec.
     */
    public int profile;
    
    /*enum AVColorTransferCharacteristic*/ public int color_trc;
    
    /**
     * YUV colorspace type.
     * - encoding: Set by user
     * - decoding: Set by libavcodec
     */
    /*enum AVColorSpace*/ public int colorspace;
    public int[] mb_index2xy;        ///< mb_index -> mb_x + mb_y*mb_stride


     /**
      * level
      * - encoding: Set by user.
      * - decoding: Set by libavcodec.
      */
    public int level;

      /**
       * number of reference frames
       * - encoding: Set by user.
       * - decoding: Set by lavc.
       */
    public int refs;
  
    
    public int /*enum OutputFormat*/out_format; ///< output format

    
    /**
     * Pixel format, see PIX_FMT_xxx.
     * May be set by the demuxer if known from headers.
     * May be overriden by the decoder if it knows better.
     * - encoding: Set by user.
     * - decoding: Set by user if known, overridden by libavcodec if known
     */
    /*enum PixelFormat*/ public int pix_fmt;

    /**
     * Error recognization; higher values will detect more errors but may
     * misdetect some more or less valid parts as errors.
     * - encoding: unused
     * - decoding: Set by user.
     */
    public int error_recognition;
      
    /* sequence parameters */
    public int context_initialized;
    public int input_picture_number;  ///< used to set pic->display_picture_number, should not be used for/by anything else
    public int coded_picture_number;  ///< used to set pic->coded_picture_number, should not be used for/by anything else
    public int picture_number;       //FIXME remove, unclear definition
    public int picture_in_gop_number; ///< 0-> first pic in gop, ...
    public int b_frames_since_non_b;  ///< used for encoding, relative to not yet reordered input
    public int user_specified_pts;///< last non zero pts from AVFrame which was passed into avcodec_encode_video()
    public int mb_width, mb_height;   ///< number of MBs horizontally & vertically
    public int mb_stride;             ///< mb_width+1 used for some arrays to allow simple addressing of left & top MBs without sig11
    public int b8_stride;             ///< 2*mb_width+1 used for some 8x8 block arrays to allow simple addressing
    public int b4_stride;             ///< 4*mb_width+1 used for some 4x4 block arrays to allow simple addressing
    public int h_edge_pos, v_edge_pos;///< horizontal / vertical position of the right/bottom edge (pixel replication)
    public int mb_num;                ///< number of MBs of a picture
    public int linesize;              ///< line size, in bytes, may be different from width
    public int uvlinesize;            ///< line size, for chroma in bytes, may be different from width
	
	
    public int f_code;                 ///< forward MV resolution
    public int b_code;                 ///< backward MV resolution for B Frames (mpeg4)
    
    public int bitstream_buffer_size;
    public int chroma_x_shift;//depend on pix_format, that depend on chroma_format
    public int chroma_y_shift;
	
    public int low_delay;                   ///< no reordering needed / has no b-frames
    
	public int picture_structure;
    public int data_partitioning;           ///< data partitioning flag from header
	public int partitioned_frame;           ///< is current frame partitioned	

	public int codec_id; // enum CodecID codec_id;     /* see CODEC_ID_xxx */
	
    public int first_slice;
    public int first_field;         ///< is 1 for the first field of a field picture 0 otherwise
	
    public int hurry_up;     /**< when set to 1 during decoding, b frames will be skipped
    when set to 2 idct/dequant will be skipped too */
    
    /**
     * internal_buffers
     * Don't touch, used by libavcodec default_get_buffer().
     */
    public InternalBuffer[] internal_buffer;
    
    /**
     * slice flags
     * - encoding: unused
     * - decoding: Set by user.
     */
    public int slice_flags;
    
    /**
     * For some codecs, the time base is closer to the field rate than the frame rate.
     * Most notably, H.264 and MPEG-2 specify time_base as half of frame duration
     * if no telecine is used ...
     *
     * Set to time_base ticks per frame. Default 1, e.g., H.264/MPEG-2 set it to 2.
     */
    public int ticks_per_frame;
	
    /* decompression specific */
	public GetBitContext gb = new GetBitContext();
	public DSPContext dsp = new DSPContext(); 			///< pointers for accelerated dsp functions
	public MotionEstContext me = new MotionEstContext(); 

	public int resync_mb_x;                 ///< x position of last resync marker
	public int resync_mb_y;                 ///< y position of last resync marker
    
	public int hwaccel = 0;
	
    public int closed_gop;             ///< MPEG1/2 GOP is closed
    public /*uint16_t*/ int pp_time;               ///< time distance between the last 2 p,s,i frames
    public /*uint16_t*/ int pb_time;               
	
	public int skip_frame = AVDISCARD_DEFAULT; // Super lazy!!! :D
	public int flags;
	public int flags2;       ///< AVCodecContext.flags2	
	public int workaround_bugs;       ///< workaround bugs in encoders which cannot be detected automatically
	
    //uint8_t *edge_emu_buffer;     ///< points into the middle of allocated_edge_emu_buffer
    //uint8_t *allocated_edge_emu_buffer;
	public int[] allocated_edge_emu_buffer;
    public int edge_emu_buffer_offset;     ///< points into the middle of allocated_edge_emu_buffer

    //uint8_t *obmc_scratchpad;
    public int[] obmc_scratchpad;
    
    public int intra_only;   ///< if true, only intra pictures are generated
    
    /* motion compensation */
    public int unrestricted_mv;        ///< mv can point outside of the coded picture
    
    public int quarter_sample;              ///< 1->qpel, 0->half pel ME/MC
    
    public int last_pict_type; //FIXME removes
    public int[] last_lambda_for = new int[5];     ///< last lambda for a specific pict type
    public int last_non_b_pict_type;   ///< used for mpeg4 gmc b-frames & ratecontrol

    /*
    uint8_t *prev_pict_types;     ///< previous picture types in bitstream order, used for mb skip
    #define PREV_PICT_TYPES_BUFFER_SIZE 256
        int mb_skipped;                ///< MUST BE SET only during DECODING
        uint8_t *mbskip_table;        //< used to avoid copy if macroblock skipped (for black regions for example)
                                      // and used for b-frame encoding & decoding (contains skip table of next P Frame) 
        uint8_t *mbintra_table;       ///< used to avoid setting {ac, dc, cbp}-pred stuff to zero on inter MB decoding
    */
    public int[] prev_pict_types;     ///< previous picture types in bitstream order, used for mb skip
    public static final int PREV_PICT_TYPES_BUFFER_SIZE = 256;
    public int mb_skipped;                ///< MUST BE SET only during DECODING
    public int[] mbskip_table;        //< used to avoid copy if macroblock skipped (for black regions for example)
                                      // and used for b-frame encoding & decoding (contains skip table of next P Frame) 
    public int[] mbintra_table;       ///< used to avoid setting {ac, dc, cbp}-pred stuff to zero on inter MB decoding
    
    /**
     * low resolution decoding, 1-> 1/2 size, 2->1/4 size
     * - encoding: unused
     * - decoding: Set by user.
     */
    public int lowres;

    /**
     * Bitstream width / height, may be different from width/height if lowres
     * or other things are used.
     * - encoding: unused
     * - decoding: Set by user before init if known. Codec should override / dynamically change if needed.
     */
    public int coded_width, coded_height;
    
    public int start_mb_y;            ///< start mb_y of this thread (so current thread should process start_mb_y <= row < end_mb_y)
    public int end_mb_y;              ///< end   mb_y of this thread (so current thread should process start_mb_y <= row < end_mb_y)    
    public MpegEncContext[] thread_context = new MpegEncContext[H264Context.MAX_THREADS];    
    /**
     * the picture in the bitstream
     * - encoding: Set by libavcodec.
     * - decoding: Set by libavcodec.
     */
    public AVFrame coded_frame;
    public ParseContext parse_context = new ParseContext();
    
    public /*uint8_t*/ int[] rd_scratchpad;       ///< scratchpad for rate distortion mb decision
    public /*uint8_t*/ int obmc_scratchpad_offset;
    public /*uint8_t*/ int[] b_scratchpad;        ///< scratchpad used for writing into write only buffers
    
    public AVFrame[] picture;          ///< main picture buffer
    /**
     * copy of the current picture structure.
     * note, linesize & data, might not match the current picture (for field pictures)
     */	
	public AVFrame current_picture = new AVFrame();    ///< buffer to store the decompressed current picture

    /**
     * internal_buffer count
     * Don't touch, used by libavcodec default_get_buffer().
     */
    public int internal_buffer_count;
	
	/**
     * copy of the previous picture structure.
     * note, linesize & data, might not match the previous picture (for field pictures)
     */
	public AVFrame last_picture = new AVFrame();

    /**
     * copy of the next picture structure.
     * note, linesize & data, might not match the next picture (for field pictures)
     */
	public AVFrame next_picture =  new AVFrame();
	
	public int top_field_first;
	
	/**
     * Current packet as passed into the decoder, to avoid having
     * to pass the packet into every function. Currently only valid
     * inside lavc and get/release_buffer callbacks.
     * - decoding: set by avcodec_decode_*, read by get_buffer() for setting pkt_pts
     * - encoding: unused
     */
    public AVPacket pkt;	
    
	public AVFrame current_picture_ptr;  ///< pointer to the current picture    
	public AVFrame last_picture_ptr;     ///< pointer to the previous picture.
	public AVFrame next_picture_ptr;     ///< pointer to the next picture (for bidir pred)
	
    public int broken_link;         ///< no_output_of_prior_pics_flag

    public H264Context priv_data;
    public int mv_type;
    public /*uint8_t **/int[][/*3*/] visualization_buffer; //< temporary buffer vor MV visualization

    /* error concealment / resync */
    public int error_count;
    public /*&uint8_t **/ int[] error_status_table;       ///< table of the error status of each MB
    
    /**
     * opaque 64bit number (generally a PTS) that will be reordered and
     * output in AVFrame.reordered_opaque
     * @deprecated in favor of pkt_pts
     * - encoding: unused
     * - decoding: Set by user.
     */
    public long reordered_opaque;
    public int progressive_frame;
    public int progressive_sequence;

    public int[] /*uint8_t **/y_dc_scale_table;     ///< qscale -> y_dc_scale table
    public int[] /*uint8_t **/c_dc_scale_table;     ///< qscale -> c_dc_scale table
    public int[] /*uint8_t **/chroma_qscale_table;  ///< qscale -> chroma_qscale (h263)
    
    /*
    DCTELEM (*pblocks[12])[64];
    DCTELEM (*block)[64]; ///< points to one of the following blocks
    DCTELEM (*blocks)[8][64]; // for HQ mode we need to keep the best block
    */
    public int[] pblocks_offset; // offset into blocks
    public short[][] block; ///< points to one of the following blocks
    public short[][][]blocks; // for HQ mode we need to keep the best block
    // it should be [12][8][64]
    
    /**
     * some codecs need / can use extradata like Huffman tables.
     * mjpeg: Huffman tables
     * rv10: additional flags
     * mpeg4: global headers (they can be in the bitstream or here)
     * The allocated memory should be FF_INPUT_BUFFER_PADDING_SIZE bytes larger
     * than extradata_size to avoid prolems if it is read with the bitstream reader.
     * The bytewise contents of extradata must not depend on the architecture or CPU endianness.
     * - encoding: Set/allocated/freed by libavcodec.
     * - decoding: Set/allocated/freed by user.
     */
    /*uint8_t **/public int[] extradata;
    public int extradata_size;
    
	public static /*enum PixelFormat*/ int get_format(/*const enum PixelFormat*/int[] fmt){
		// For JAVA .H264 Decoder, we support only Software Accelerated Format (PIX_FMT_YUV420P) 
		return PIX_FMT_YUV420P;
		/*
		int i=0;
	    while (fmt[i] != PIX_FMT_NONE && 0!= ff_is_hwaccel_pix_fmt(fmt[i]))
	        ++i;
	    return fmt[i];
	    */
	}
        
    public static int av_image_check_size(/*unsigned*/ int w, /*unsigned*/ int h, int log_offset, /*void **/MpegEncContext log_ctx) {
        //ImgUtils imgutils = { &imgutils_class, log_offset, log_ctx };

        if ((int)w>0 && (int)h>0 && (w+128)*(long)(h+128) < Integer.MAX_VALUE/8)
            return 0;

        //av_log(&imgutils, AV_LOG_ERROR, "Picture size %ux%u is invalid\n", w, h);
        return -1; // AVERROR(EINVAL);
    }
    
    /**
     * returns the number of bytes consumed for building the current frame
     */
    public int get_consumed_bytes(int pos, int buf_size){
            if(pos==0) pos=1; //avoid infinite loops (i doubt that is needed but ...)
            if(pos+10>buf_size) pos=buf_size; // oops ;)
            return pos;
    }    
    
    public void ff_init_scantable(int[] permutation, ScanTable st, int[] src_scantable) {
    //(uint8_t *permutation, ScanTable *st, const uint8_t *src_scantable){
        int i;
        int end;

        st.scantable= src_scantable;

        for(i=0; i<64; i++){
            int j;
            j = src_scantable[i];
            st.permutated[i] = permutation[j];
        }

        end=-1;
        for(i=0; i<64; i++){
            int j;
            j = st.permutated[i];
            if(j>end) end=j;
            st.raster_end[i]= end;
        }
    }
    
    /* init common dct for both encoder and decoder */
    public int ff_dct_common_init() {
    	// Use only H264
        //this.dct_unquantize_h263_intra = dct_unquantize_h263_intra_c;
        //this.dct_unquantize_h263_inter = dct_unquantize_h263_inter_c;
        //this.dct_unquantize_mpeg1_intra = dct_unquantize_mpeg1_intra_c;
        //this.dct_unquantize_mpeg1_inter = dct_unquantize_mpeg1_inter_c;
        //this.dct_unquantize_mpeg2_intra = dct_unquantize_mpeg2_intra_c;
        //if(this.flags & CODEC_FLAG_BITEXACT)
        //    this.dct_unquantize_mpeg2_intra = dct_unquantize_mpeg2_intra_bitexact;
        //this.dct_unquantize_mpeg2_inter = dct_unquantize_mpeg2_inter_c;

        /* load & permutate scantables
           note: only wmv uses different ones
        */
        if(this.alternate_scan!=0){
            ff_init_scantable(this.dsp.idct_permutation, this.inter_scantable  , H264Context.ff_alternate_vertical_scan);
            ff_init_scantable(this.dsp.idct_permutation, this.intra_scantable  , H264Context.ff_alternate_vertical_scan);
        }else{
            ff_init_scantable(this.dsp.idct_permutation, this.inter_scantable  , H264Context.ff_zigzag_direct);
            ff_init_scantable(this.dsp.idct_permutation, this.intra_scantable  , H264Context.ff_zigzag_direct);
        }
        ff_init_scantable(this.dsp.idct_permutation, this.intra_h_scantable, H264Context.ff_alternate_horizontal_scan);
        ff_init_scantable(this.dsp.idct_permutation, this.intra_v_scantable, H264Context.ff_alternate_vertical_scan);

        return 0;
    }
    

	public void avcodec_align_dimensions2(int[/* inout */] widthheight,
			int[/* 4 */] linesize_align) {
		int w_align = 1;
		int h_align = 1;

		switch (this.pix_fmt) {
		case PIX_FMT_YUV420P:
		case PIX_FMT_YUYV422:
		case PIX_FMT_UYVY422:
		case PIX_FMT_YUV422P:
		case PIX_FMT_YUV440P:
		case PIX_FMT_YUV444P:
		case PIX_FMT_GRAY8:
		case PIX_FMT_GRAY16BE:
		case PIX_FMT_GRAY16LE:
		case PIX_FMT_YUVJ420P:
		case PIX_FMT_YUVJ422P:
		case PIX_FMT_YUVJ440P:
		case PIX_FMT_YUVJ444P:
		case PIX_FMT_YUVA420P:
			w_align = 16; // FIXME check for non mpeg style codecs and use less
							// alignment
			h_align = 16;
			if (this.codec_id == H264PredictionContext.CODEC_ID_MPEG2VIDEO
					|| this.codec_id == H264PredictionContext.CODEC_ID_MJPEG
					|| this.codec_id == H264PredictionContext.CODEC_ID_AMV
					|| this.codec_id == H264PredictionContext.CODEC_ID_THP
					|| this.codec_id == H264PredictionContext.CODEC_ID_H264)
				h_align = 32; // interlaced is rounded up to 2 MBs
			break;
		case PIX_FMT_YUV411P:
		case PIX_FMT_UYYVYY411:
			w_align = 32;
			h_align = 8;
			break;
		case PIX_FMT_YUV410P:
			if (this.codec_id == H264PredictionContext.CODEC_ID_SVQ1) {
				w_align = 64;
				h_align = 64;
			}
			/*
			 * ????????????????// case PIX_FMT_RGB555: if(this.codec_id ==
			 * H264PredictionContext.CODEC_ID_RPZA){ w_align=4; h_align=4; }
			 */
		case PIX_FMT_PAL8:
		case PIX_FMT_BGR8:
		case PIX_FMT_RGB8:
			if (this.codec_id == H264PredictionContext.CODEC_ID_SMC) {
				w_align = 4;
				h_align = 4;
			}
			break;
		case PIX_FMT_BGR24:
			if ((this.codec_id == H264PredictionContext.CODEC_ID_MSZH)
					|| (this.codec_id == H264PredictionContext.CODEC_ID_ZLIB)) {
				w_align = 4;
				h_align = 4;
			}
			break;
		default:
			w_align = 1;
			h_align = 1;
			break;
		}

		widthheight[0] = (((widthheight[0]) + (w_align) - 1) & ~((w_align) - 1)); // FFALIGN(*width
																					// ,
																					// w_align);
		widthheight[1] = (((widthheight[1]) + (h_align) - 1) & ~((h_align) - 1)); // FFALIGN(*height,
																					// h_align);
		if (this.codec_id == H264PredictionContext.CODEC_ID_H264
				|| this.lowres != 0)
			widthheight[1] += 2; // some of the optimized chroma MC reads one
									// line too much
		// which is also done in mpeg decoders with lowres > 0

		linesize_align[0] = linesize_align[1] = linesize_align[2] = linesize_align[3] = STRIDE_ALIGN;
	}
    
	public void av_image_fill_max_pixsteps(int[] max_pixsteps/*[4]*/, int[] max_pixstep_comps/*[4]*/,
            AVPixFmtDescriptor pixdesc)
	{
		int i;
		//memset(max_pixsteps, 0, 4*sizeof(max_pixsteps[0]));
		Arrays.fill(max_pixsteps, 0);
		if (max_pixstep_comps!=null)
		//memset(max_pixstep_comps, 0, 4*sizeof(max_pixstep_comps[0]));
			Arrays.fill(max_pixstep_comps, 0);
		
		for (i = 0; i < 4; i++) {
			AVComponentDescriptor comp = (pixdesc.comp[i]);
			if ((comp.step_minus1+1) > max_pixsteps[comp.plane]) {
				max_pixsteps[comp.plane] = comp.step_minus1+1;
				if (max_pixstep_comps!=null)
					max_pixstep_comps[comp.plane] = i;
			}
		}
	}
	
	public int av_image_fill_linesizes(int[] linesizes/*[4]*/, int pix_fmt, int width)
	{
	    int i;
	    AVPixFmtDescriptor desc = ImageUtils.av_pix_fmt_descriptors[pix_fmt];
	    int[] max_step = new int[4];       /* max pixel step for each plane */
	    int[] max_step_comp = new int[4];       /* the component for each plane which has the max pixel step */

	    //memset(linesizes, 0, 4*sizeof(linesizes[0]));
	    Arrays.fill(linesizes, 0);

	    if (/*(unsigned)*/pix_fmt >= PIX_FMT_NB || (desc.flags & ImageUtils.PIX_FMT_HWACCEL)!=0)
	        return -1;

	    if ((desc.flags & ImageUtils.PIX_FMT_BITSTREAM)!=0) {
	        if (width > (Integer.MAX_VALUE -7) / (desc.comp[0].step_minus1+1))
	            return -1;
	        linesizes[0] = (width * (desc.comp[0].step_minus1+1) + 7) >> 3;
	        return 0;
	    }

	    av_image_fill_max_pixsteps(max_step, max_step_comp, desc);
	    for (i = 0; i < 4; i++) {
	        int s = (max_step_comp[i] == 1 || max_step_comp[i] == 2) ? desc.log2_chroma_w : 0;
	        int shifted_w = ((width + (1 << s) - 1)) >> s;
	        if (max_step[i] > Integer.MAX_VALUE / shifted_w)
	            return -1;
	        linesizes[i] = max_step[i] * shifted_w;
	    }

	    return 0;
	}

	public int av_image_fill_pointers(/*uint8_t */int[][] data_base/*[4]*/, int[] data_offset, int pix_fmt, int height,
	                           /*uint8_t **/int[] ptr, int[] linesizes/*[4]*/)
	{
	    int i, total_size;
	    int[] size = new int[4];
	    int[] has_plane = new int[4];

	    AVPixFmtDescriptor desc = ImageUtils.av_pix_fmt_descriptors[pix_fmt];
	    for(int k=0;k<4;k++) {
	    	data_base[k] = null;
	    	data_offset[k] = 0;
	    } // for
	    //memset(size     , 0, sizeof(size));
	    //memset(has_plane, 0, sizeof(has_plane));

	    if (/*(unsigned)*/pix_fmt >= PIX_FMT_NB || (desc.flags & ImageUtils.PIX_FMT_HWACCEL)!=0)
	        return -1;

	    data_base[0] = ptr;
	    data_offset[0] = 0;
	    if (linesizes[0] > (Integer.MAX_VALUE - 1024) / height)
	        return -1;
	    size[0] = linesizes[0] * height;

	    if ((desc.flags & ImageUtils.PIX_FMT_PAL)!=0) {
	        size[0] = (size[0] + 3) & ~3;
		    data_base[1] = ptr;
	        data_offset[1] = size[0]; /* palette is stored here as 256 32 bits words */
	        return size[0] + 256 * 4;
	    }

	    for (i = 0; i < 4; i++)
	        has_plane[desc.comp[i].plane] = 1;

	    total_size = size[0];
	    for (i = 1; has_plane[i]!=0 && i < 4; i++) {
	        int h, s = (i == 1 || i == 2) ? desc.log2_chroma_h : 0;
		    data_base[i] = data_base[i-1];
		    data_offset[i] = data_offset[i-1] + size[i-1];
	        h = (height + (1 << s) - 1) >> s;
	        if (linesizes[i] > Integer.MAX_VALUE / h)
	            return -1;
	        size[i] = h * linesizes[i];
	        if (total_size > Integer.MAX_VALUE - size[i])
	            return -1;
	        total_size += size[i];
	    }

	    return total_size;
	}

	public int ff_set_systematic_pal2(/*uint32_t => int8_t*/int[] pal/*[256]*/, int pal_offset, int pix_fmt)
	{
	    int i;

	    for (i = 0; i < 256; i++) {
	        int r, g, b;

	        switch (pix_fmt) {
	        case PIX_FMT_RGB8:
	            r = (i>>5    )*36;
	            g = ((i>>2)&7)*36;
	            b = (i&3     )*85;
	            break;
	        case PIX_FMT_BGR8:
	            b = (i>>6    )*85;
	            g = ((i>>3)&7)*36;
	            r = (i&7     )*36;
	            break;
	        case PIX_FMT_RGB4_BYTE:
	            r = (i>>3    )*255;
	            g = ((i>>1)&3)*85;
	            b = (i&1     )*255;
	            break;
	        case PIX_FMT_BGR4_BYTE:
	            b = (i>>3    )*255;
	            g = ((i>>1)&3)*85;
	            r = (i&1     )*255;
	            break;
	        case PIX_FMT_GRAY8:
	            r = b = g = i;
	            break;
	        default:
	            return -1;
	        }
	        //pal[i] = b + (g<<8) + (r<<16);
	        pal[pal_offset + i*4 + 0] = b;
	        pal[pal_offset + i*4 + 1] = g;
	        pal[pal_offset + i*4 + 2] = r;
	        pal[pal_offset + i*4 + 3] = 0x00; // a?	        
	    }

	    return 0;
	}
   
    public int get_buffer(AVFrame pic){
        int i;
        int w= this.width;
        int h= this.height;
        InternalBuffer buf;
        //int[] picture_number;

        if(pic.data_base[0]!=null) {
        	// DebugTool.printDebugString("     ----- get_buffer error case 0\n");        	
            //av_log(s, AV_LOG_ERROR, "pic.data[0]!=NULL in avcodec_default_get_buffer\n");
            return -1;
        }
        if(this.internal_buffer_count >= INTERNAL_BUFFER_SIZE) {
            //av_log(s, AV_LOG_ERROR, "internal_buffer_count overflow (missing release_buffer?)\n");
        	// DebugTool.printDebugString("     ----- get_buffer error case 1\n");        	
        	return -1;
        }

        if(av_image_check_size(w, h, 0, this)!=0) {
        	// DebugTool.printDebugString("     ----- get_buffer error case 2\n");        	
        	return -1;
        }

        if(this.internal_buffer==null){
            //this.internal_buffer= av_mallocz((INTERNAL_BUFFER_SIZE+1)*sizeof(InternalBuffer));
        	internal_buffer = new InternalBuffer[INTERNAL_BUFFER_SIZE+1];
        	for(i=0;i<INTERNAL_BUFFER_SIZE+1;i++)
        		internal_buffer[i] = new InternalBuffer();
        }

        buf= internal_buffer[this.internal_buffer_count];
        //picture_number= &(((InternalBuffer*)this.internal_buffer)[INTERNAL_BUFFER_SIZE]).last_pic_num; //FIXME ugly hack
        //(*picture_number)++;
        this.internal_buffer[INTERNAL_BUFFER_SIZE].last_pic_num++;

        if(buf.base[0]!=null && (buf.width != w || buf.height != h || buf.pix_fmt != this.pix_fmt)){
            for(i=0; i<4; i++){
                //av_freep(&buf.base[i]);
                buf.base[i]= null;
                buf.data_offset[i] = 0;
            }
        }

        if(buf.base[0]!=null){
            pic.age= this.internal_buffer[INTERNAL_BUFFER_SIZE].last_pic_num - buf.last_pic_num;
            buf.last_pic_num= this.internal_buffer[INTERNAL_BUFFER_SIZE].last_pic_num;
        }else{
            int h_chroma_shift, v_chroma_shift;
            int[] size = new int[4];
            int tmpsize;
            int unaligned;
            AVPicture picture = new AVPicture();
            int[] stride_align = new int[4];

            //avcodec_get_chroma_sub_sample(this.pix_fmt, &h_chroma_shift, &v_chroma_shift);
            h_chroma_shift = ImageUtils.av_pix_fmt_descriptors[pix_fmt].log2_chroma_w;
            v_chroma_shift = ImageUtils.av_pix_fmt_descriptors[pix_fmt].log2_chroma_h;

            int[] param = new int[] {w,h};
            avcodec_align_dimensions2(param, stride_align);
            w = param[0];
            h = param[1];
            pic.imageWidthWOEdge = w;
            pic.imageHeightWOEdge = h;

            if(0==(this.flags&CODEC_FLAG_EMU_EDGE)){
                w+= EDGE_WIDTH*2;
                h+= EDGE_WIDTH*2;
            }
            pic.imageWidth = w;
            pic.imageHeight = h;

            do {
                // NOTE: do not align linesizes individually, this breaks e.g. assumptions
                // that linesize[0] == 2*linesize[1] in the MPEG-encoder for 4:2:2
                av_image_fill_linesizes(picture.linesize, this.pix_fmt, w);
                // increase alignment of w for next try (rhs gives the lowest bit set in w)
                w += w & ~(w-1);

                unaligned = 0;
                for (i=0; i<4; i++){
                    unaligned |= (picture.linesize[i] % stride_align[i]);
                }
            } while (unaligned!=0);

            tmpsize = av_image_fill_pointers(picture.data_base, picture.data_offset, this.pix_fmt, h, null, picture.linesize);
            if (tmpsize < 0) {
            	// DebugTool.printDebugString("     ----- get_buffer error case 3\n");        	
            	return -1;
            }

            for (i=0; i<3 && picture.data_offset[i+1]!=0; i++)
                size[i] = picture.data_offset[i+1] - picture.data_offset[i];
            size[i] = tmpsize - (picture.data_offset[i] - picture.data_offset[0]);

            buf.last_pic_num= -256*256*256*64;
            //memset(buf.base, 0, sizeof(buf.base));
            //memset(buf.data, 0, sizeof(buf.data));
            for(int k=0;k<buf.base.length;k++)
            	buf.base[k] = null;
            	//Arrays.fill(buf.base[k], 0);
            for(int k=0;k<buf.data_offset.length;k++)
            	buf.data_offset[k] = 0;
            //	Arrays.fill(buf.data[k], 0);

            for(i=0; i<4 && size[i]!=0; i++){
                int h_shift= i==0 ? 0 : h_chroma_shift;
                int v_shift= i==0 ? 0 : v_chroma_shift;

                buf.linesize[i]= picture.linesize[i];

                //buf.base[i]= av_malloc(size[i]+16); //FIXME 16
                buf.base[i] = new int[size[i]+16];
                if(buf.base[i]==null) return -1;
                //memset(buf.base[i], 128, size[i]);
                Arrays.fill(buf.base[i], 0, size[i], 128);

                // no edge if EDGE EMU or not planar YUV
                if((this.flags&CODEC_FLAG_EMU_EDGE)!=0 || 0==size[2])
                    buf.data_offset[i] = 0;
                else
                    buf.data_offset[i] = ((((buf.linesize[i]*EDGE_WIDTH>>v_shift) + (EDGE_WIDTH>>h_shift))+(stride_align[i])-1)&~((stride_align[i])-1)); 
                      //+ FFALIGN((buf.linesize[i]*EDGE_WIDTH>>v_shift) + (EDGE_WIDTH>>h_shift), stride_align[i]);
            }
            if(size[1]!=0 && 0==size[2])
                ff_set_systematic_pal2(buf.base[1], buf.data_offset[1], this.pix_fmt);
            buf.width  = this.width;
            buf.height = this.height;
            buf.pix_fmt= this.pix_fmt;
            pic.age= 256*256*256*64;
        }
        pic.type= FF_BUFFER_TYPE_INTERNAL;

        for(i=0; i<4; i++){
            pic.base[i]= buf.base[i];
            pic.data_base[i]= buf.base[i];
            pic.data_offset[i]= buf.data_offset[i];
            pic.linesize[i]= buf.linesize[i];
        }
        this.internal_buffer_count++;
        
        // DebugTool.printDebugString("****Internal_Buffer_Count = "+this.internal_buffer_count);

        if(this.pkt!=null) pic.pkt_pts= this.pkt.pts;
        else       pic.pkt_pts= AV_NOPTS_VALUE;
        pic.reordered_opaque= this.reordered_opaque;

        /*
        if(this.debug&FF_DEBUG_BUFFERS)
            av_log(s, AV_LOG_DEBUG, "default_get_buffer called on pic %p, %d buffers used\n", pic, this.internal_buffer_count);
		*/
    	// DebugTool.printDebugString("     ----- get_buffer OK.\n");        	
        return 0;
    }
        
    /**
     * Allocate a frame buffer
     */
    public int alloc_frame_buffer(AVFrame pic)
    {
        int r;

        /*?????????????????????????/
        if (this.avctx.hwaccel) {
            //assert(!pic.hwaccel_picture_private);
            if (this.avctx.hwaccel.priv_data_size) {
                pic.hwaccel_picture_private = av_mallocz(this.avctx.hwaccel.priv_data_size);
                if (!pic.hwaccel_picture_private) {
                    av_log(this.avctx, AV_LOG_ERROR, "alloc_frame_buffer() failed (hwaccel private data allocation)\n");
                    return -1;
                }
            }
        }
        */

        // TODO: get_buffer seems to capture only first component?
        r = this.get_buffer((AVFrame)pic);

        if (r<0 || 0==pic.age || 0==pic.type || null==pic.data_base[0]) {
            //av_log(this.avctx, AV_LOG_ERROR, "get_buffer() failed (%d %d %d %p)\n", r, pic.age, pic.type, pic.data[0]);
            //av_freep(&pic.hwaccel_picture_private);
            return -1;
        }

        if (this.linesize!=0 && (this.linesize != pic.linesize[0] || this.uvlinesize != pic.linesize[1])) {
            //av_log(this.avctx, AV_LOG_ERROR, "get_buffer() failed (stride changed)\n");
        	// DebugTool.printDebugString("     ----- alloc_frame_buffer error case 0\n");
            free_frame_buffer(pic);
            return -1;
        }

        if (pic.linesize[1] != pic.linesize[2]) {
            //av_log(this.avctx, AV_LOG_ERROR, "get_buffer() failed (uv stride mismatch)\n");
        	// DebugTool.printDebugString("     ----- alloc_frame_buffer error case 1\n");
            free_frame_buffer(pic);
            return -1;
        }

    	// DebugTool.printDebugString("     ----- alloc_frame_buffer OK.\n");        
        return 0;
    }
    
    /**
     * allocates a Picture
     * The pixels are allocated/set by calling get_buffer() if shared=0
     */
    public int ff_alloc_picture(AVFrame pic, int shared){
        int big_mb_num= this.mb_stride*(this.mb_height+1) + 1; //the +1 is needed so memset(,,stride*height) does not sig11
        int mb_array_size= this.mb_stride*this.mb_height;
        //int b8_array_size= this.b8_stride*this.mb_height*2;
        int b4_array_size= this.b4_stride*this.mb_height*4;
        int i;
        //int r= -1;

        if(shared!=0){
            ////assert(pic.data[0]);
            ////assert(pic.type == 0 || pic.type == FF_BUFFER_TYPE_SHARED);
            pic.type= FF_BUFFER_TYPE_SHARED;
        }else{
            ////assert(!pic.data[0]);

            if (alloc_frame_buffer(pic) < 0) {
    	    	// DebugTool.printDebugString("     ----- ff_alloc_picture error case 0\n");            	
                return -1;
            }

            this.linesize  = pic.linesize[0];
            this.uvlinesize= pic.linesize[1];
        }

        if(pic.qscale_table==null){
        	/*
            if (this.encoding) {
                FF_ALLOCZ_OR_GOTO(this.avctx, pic.mb_var   , mb_array_size * sizeof(int16_t)  , fail)
                FF_ALLOCZ_OR_GOTO(this.avctx, pic.mc_mb_var, mb_array_size * sizeof(int16_t)  , fail)
                FF_ALLOCZ_OR_GOTO(this.avctx, pic.mb_mean  , mb_array_size * sizeof(int8_t )  , fail)
            }
            */

            //FF_ALLOCZ_OR_GOTO(this.avctx, pic.mbskip_table , mb_array_size * sizeof(uint8_t)+2, fail) //the +2 is for the slice end check
        	pic.mbskip_table = new int[mb_array_size + 2];
        	
            //FF_ALLOCZ_OR_GOTO(this.avctx, pic.qscale_table , mb_array_size * sizeof(uint8_t)  , fail)
           	pic.qscale_table = new int[mb_array_size];
            
        	//FF_ALLOCZ_OR_GOTO(this.avctx, pic.mb_type_base , (big_mb_num + this.mb_stride) * sizeof(uint32_t), fail)
           	pic.mb_type_base = new long[(big_mb_num + this.mb_stride)];
            
           	pic.mb_type_offset = /*pic.mb_type_base + */2*this.mb_stride+1;
            if(this.out_format == FMT_H264){
                for(i=0; i<2; i++){
                    //FF_ALLOCZ_OR_GOTO(this.avctx, pic.motion_val_base[i], 2 * (b4_array_size+4)  * sizeof(int16_t), fail)
                	pic.motion_val_base[i] = new int[(b4_array_size+4)][2];
           
                    pic.motion_val_offset[i]= 4;
                    
                    //FF_ALLOCZ_OR_GOTO(this.avctx, pic.ref_index[i], 4*mb_array_size * sizeof(uint8_t), fail)
                    pic.ref_index[i] = new int[4*mb_array_size];
                }
                pic.motion_subsample_log2= 2;
            }/*////??????????????
              else if(this.out_format == FMT_H263 || this.encoding || (this.avctx.debug&FF_DEBUG_MV) || (this.avctx.debug_mv)){
                for(i=0; i<2; i++){
                    FF_ALLOCZ_OR_GOTO(this.avctx, pic.motion_val_base[i], 2 * (b8_array_size+4) * sizeof(int16_t), fail)
                    pic.motion_val[i]= pic.motion_val_base[i]+4;
                    FF_ALLOCZ_OR_GOTO(this.avctx, pic.ref_index[i], 4*mb_array_size * sizeof(uint8_t), fail)
                }
                pic.motion_subsample_log2= 3;
            }
            */
            //???????????????????
            /* No DEBUG
            if(this.avctx.debug&FF_DEBUG_DCT_COEFF) {
                FF_ALLOCZ_OR_GOTO(this.avctx, pic.dct_coeff, 64 * mb_array_size * sizeof(DCTELEM)*6, fail)
            }
            */
            pic.qstride= this.mb_stride;
            //FF_ALLOCZ_OR_GOTO(this.avctx, pic.pan_scan , 1 * sizeof(AVPanScan), fail)
            pic.pan_scan = new AVPanScan();
        }

        /* It might be nicer if the application would keep track of these
         * but it would require an API change. */
        //memmove(this.prev_pict_types+1, this.prev_pict_types, PREV_PICT_TYPES_BUFFER_SIZE-1);
        for(int k=1;k<PREV_PICT_TYPES_BUFFER_SIZE;k++)
        	this.prev_pict_types[k] = this.prev_pict_types[k-1];
        	
        this.prev_pict_types[0]= (this.dropable!=0) ? H264Context.FF_B_TYPE : this.pict_type;
        if(pic.age < PREV_PICT_TYPES_BUFFER_SIZE && this.prev_pict_types[pic.age] == H264Context.FF_B_TYPE)
            pic.age= Integer.MAX_VALUE; // Skipped MBs in B-frames are quite rare in MPEG-1/2 and it is a bit tricky to skip them anyway.

    	// DebugTool.printDebugString("     ----- ff_alloc_picture error OK.\n");            	
        return 0;
    }
    
    public int ff_find_unused_picture(int shared){
        int i;

        if(shared!=0){
            for(i=0; i<MAX_PICTURE_COUNT; i++){
                if(this.picture[i].data_base[0]==null && this.picture[i].type==0) return i;
            }
        }else{
            for(i=0; i<MAX_PICTURE_COUNT; i++){
                if(this.picture[i].data_base[0]==null && this.picture[i].type!=0) return i; //FIXME
            }
            for(i=0; i<MAX_PICTURE_COUNT; i++){
                if(this.picture[i].data_base[0]==null) return i;
            }
        }

        //av_log(this.avctx, AV_LOG_FATAL, "Internal error, picture buffer overflow\n");
        /* We could return -1, but the codec would crash trying to draw into a
         * non-existing frame anyway. This is safer than waiting for a random crash.
         * Also the return of this is never useful, an encoder must only allocate
         * as much as allowed in the specification. This has no relationship to how
         * much libavcodec could allocate (and MAX_PICTURE_COUNT is always large
         * enough for such valid streams).
         * Plus, a decoder has to check stream validity and remove frames if too
         * many reference frames are around. Waiting for "OOM" is not correct at
         * all. Similarly, missing reference frames have to be replaced by
         * interpolated/MC frames, anything else is a bug in the codec ...
         */
        //abort();
        return -1;
    }
        
    /**
     * generic function for encode/decode called after coding/decoding the header and before a frame is coded/decoded
     */
    public int MPV_frame_start()
    {
        int i;
        AVFrame pic;
        this.mb_skipped = 0;

        ////assert(this.last_picture_ptr==NULL || this.out_format != FMT_H264 || this.codec_id == CODEC_ID_SVQ3);

        /* mark&release old frames */
       // if (this.pict_type != FF_B_TYPE && this.last_picture_ptr && this.last_picture_ptr != this.next_picture_ptr && this.last_picture_ptr.data[0]) {
          /* Support H264
          if(this.out_format != FMT_H264 || this.codec_id == CODEC_ID_SVQ3){
              free_frame_buffer(s, this.last_picture_ptr);

            // release forgotten pictures 
            // if(mpeg124/h263) 
            if(!this.encoding){
                for(i=0; i<MAX_PICTURE_COUNT; i++){
                    if(this.picture[i].data[0] && &this.picture[i] != this.next_picture_ptr && this.picture[i].reference){
                        av_log(avctx, AV_LOG_ERROR, "releasing zombie picture\n");
                        free_frame_buffer(s, &this.picture[i]);
                    }
                }
            }
          }
          */
        //}

        if(true){
            /* release non reference frames */
            for(i=0; i<MAX_PICTURE_COUNT; i++){
                if(this.picture[i].data_base[0]!=null && 0==this.picture[i].reference && this.picture[i].type!=FF_BUFFER_TYPE_SHARED){
                	// DebugTool.printDebugString("****free_frame_buffer[picture:"+i+"].\n");
                    free_frame_buffer(this.picture[i]);
                }
            }

            if(this.current_picture_ptr !=null&& this.current_picture_ptr.data_base[0]==null) {
            	// DebugTool.printDebugString("****reuse cuurent_picture_ptr.\n");
            	pic= this.current_picture_ptr; //we already have a unused image (maybe it was set before reading the header)
            } else {
                i= ff_find_unused_picture(0);
            	// DebugTool.printDebugString("****reuse picture:"+i+".\n");
                pic= this.picture[i];
            }

            pic.reference= 0;
            if (0==this.dropable){
                if (this.codec_id == H264PredictionContext.CODEC_ID_H264)
                    pic.reference = this.picture_structure;
                else if (this.pict_type != H264Context.FF_B_TYPE)
                    pic.reference = 3;
            }

            pic.coded_picture_number= this.coded_picture_number++;

            if(this.ff_alloc_picture(pic, 0) < 0) {
    	    	// DebugTool.printDebugString("     ----- MPV_frame_start error case 0\n");
                return -1;
            }

            this.current_picture_ptr= pic;
            //FIXME use only the vars from current_pic
            this.current_picture_ptr.top_field_first= this.top_field_first;
            //???????????????????????????????
            /* Only Support H264
            if(this.codec_id == H264PredictionContext.CODEC_ID_MPEG1VIDEO || this.codec_id == H264PredictionContext.CODEC_ID_MPEG2VIDEO) {
                if(this.picture_structure != PICT_FRAME)
                    this.current_picture_ptr.top_field_first= (this.picture_structure == PICT_TOP_FIELD) == this.first_field;
            }
            */
            this.current_picture_ptr.interlaced_frame= (0==this.progressive_frame && 0==this.progressive_sequence)?1:0;
        }

        this.current_picture_ptr.pict_type= this.pict_type;
//        if(this.flags && CODEC_FLAG_QSCALE)
      //      this.current_picture_ptr.quality= this.new_picture_ptr.quality;
        this.current_picture_ptr.key_frame= (this.pict_type == H264Context.FF_I_TYPE)?1:0;

        ff_copy_picture(this.current_picture, this.current_picture_ptr);

        if (this.pict_type != H264Context.FF_B_TYPE) {
            this.last_picture_ptr= this.next_picture_ptr;
            if(0==this.dropable)
                this.next_picture_ptr= this.current_picture_ptr;
        }
    /*    av_log(this.avctx, AV_LOG_DEBUG, "L%p N%p C%p L%p N%p C%p type:%d drop:%d\n", this.last_picture_ptr, this.next_picture_ptr,this.current_picture_ptr,
            this.last_picture_ptr    ? this.last_picture_ptr.data[0] : NULL,
            this.next_picture_ptr    ? this.next_picture_ptr.data[0] : NULL,
            this.current_picture_ptr ? this.current_picture_ptr.data[0] : NULL,
            this.pict_type, this.dropable);*/

        /* Only suport H264
        if(this.codec_id != CODEC_ID_H264){
            if((this.last_picture_ptr==NULL || this.last_picture_ptr.data[0]==NULL) && this.pict_type!=FF_I_TYPE){
                //av_log(avctx, AV_LOG_ERROR, "warning: first frame is no keyframe\n");
                // Allocate a dummy frame 
                i= ff_find_unused_picture(s, 0);
                this.last_picture_ptr= &this.picture[i];
                if(ff_alloc_picture(s, this.last_picture_ptr, 0) < 0)
                    return -1;
            }
            if((this.next_picture_ptr==NULL || this.next_picture_ptr.data[0]==NULL) && this.pict_type==FF_B_TYPE){
                // Allocate a dummy frame 
                i= ff_find_unused_picture(s, 0);
                this.next_picture_ptr= &this.picture[i];
                if(ff_alloc_picture(s, this.next_picture_ptr, 0) < 0)
                    return -1;
            }
        }
        */

        if(this.last_picture_ptr!=null) ff_copy_picture(this.last_picture, this.last_picture_ptr);
        if(this.next_picture_ptr!=null) ff_copy_picture(this.next_picture, this.next_picture_ptr);

        ////assert(this.pict_type == FF_I_TYPE || (this.last_picture_ptr && this.last_picture_ptr.data[0]));
        //??????????????????????
        /* Only support H264
        if(this.picture_structure!=PICT_FRAME && this.out_format != FMT_H264){
            int i;
            for(i=0; i<4; i++){
                if(this.picture_structure == PICT_BOTTOM_FIELD){
                     this.current_picture.data_offset[i] += this.current_picture.linesize[i];
                }
                this.current_picture.linesize[i] *= 2;
                this.last_picture.linesize[i] *=2;
                this.next_picture.linesize[i] *=2;
            }
        }
        */

        //??????????????????????/
        //this.hurry_up= this.avctx.hurry_up;
        this.error_recognition= 1;

        /* set dequantizer, we can't do it during init as it might change for mpeg4
           and we can't do it in the header decode as init is not called for mpeg4 there yet */
        //????????????????????????
        /*
        if(this.mpeg_quant!=0 || this.codec_id == CODEC_ID_MPEG2VIDEO){
            this.dct_unquantize_intra = this.dct_unquantize_mpeg2_intra;
            this.dct_unquantize_inter = this.dct_unquantize_mpeg2_inter;
        }else if(this.out_format == FMT_H263 || this.out_format == FMT_H261){
            this.dct_unquantize_intra = this.dct_unquantize_h263_intra;
            this.dct_unquantize_inter = this.dct_unquantize_h263_inter;
        }else{
            this.dct_unquantize_intra = this.dct_unquantize_mpeg1_intra;
            this.dct_unquantize_inter = this.dct_unquantize_mpeg1_inter;
        }
        */

        /*??????????????
         * No way in encoding
        if(this.dct_error_sum!=0){
            ////assert(this.avctx.noise_reduction && this.encoding);

            this.update_noise_reduction();
        }
        */

        //if(CONFIG_MPEG_XVMC_DECODER && this.avctx.xvmc_acceleration)
        //    return ff_xvmc_field_start(s, avctx);
    	// DebugTool.printDebugString("     ----- MPV_frame_start error OK.\n");
        return 0;
    }
    
    public static void ff_copy_picture(AVFrame dst, AVFrame src){
    	//////////////!!!!!!!!!!!!!!!
        src.copyTo(dst);
        dst.type= FF_BUFFER_TYPE_COPY;
        //return dst;
    }
    
    public static int ff_is_hwaccel_pix_fmt(/*enum PixelFormat*/int pix_fmt)
    {
        return ImageUtils.av_pix_fmt_descriptors[pix_fmt].flags & ImageUtils.PIX_FMT_HWACCEL;
    }
    
    public int /*enum PixelFormat*/ avcodec_default_get_format(/*enum PixelFormat **/int[] fmt){
    	int i=0;
        while (fmt[i] != PIX_FMT_NONE && ff_is_hwaccel_pix_fmt(fmt[i])!=0)
            ++i;
        return fmt[i];
    }    
    
    public static int init_duplicate_context(MpegEncContext s, MpegEncContext base){
        //int y_size = s.b8_stride * (2 * s.mb_height + 1);
        //int c_size = s.mb_stride * (s.mb_height + 1);
        //int yc_size = y_size + 2 * c_size;
        int i;

        // edge emu needs blocksize + filter length - 1 (=17x17 for halfpel / 21x21 for h264)
        s.allocated_edge_emu_buffer = new int[(s.width+64)*2*21*2];
        //FF_ALLOCZ_OR_GOTO(s.avctx, s.allocated_edge_emu_buffer, (s.width+64)*2*21*2, fail); //(width + edge + align)*interlaced*MBsize*tolerance
        s.edge_emu_buffer_offset= (s.width+64)*2*21;

         //FIXME should be linesize instead of s.width*2 but that is not known before get_buffer()
        //FF_ALLOCZ_OR_GOTO(s.avctx, s.me.scratchpad,  (s.width+64)*4*16*2*sizeof(uint8_t), fail)
        s.me.scratchpad = new int[(s.width+64)*4*16*2];
        s.me.temp=         s.me.scratchpad;
        s.rd_scratchpad=   s.me.scratchpad;
        s.b_scratchpad=    s.me.scratchpad;
        s.obmc_scratchpad_offset = 16;
        /* Only Decode
        if (s.encoding) {
            FF_ALLOCZ_OR_GOTO(s.avctx, s.me.map      , ME_MAP_SIZE*sizeof(uint32_t), fail)
            FF_ALLOCZ_OR_GOTO(s.avctx, s.me.score_map, ME_MAP_SIZE*sizeof(uint32_t), fail)
            if(s.noise_reduction!=0){
                FF_ALLOCZ_OR_GOTO(s.avctx, s.dct_error_sum, 2 * 64 * sizeof(int), fail)
            }
        }
        */
        // FF_ALLOCZ_OR_GOTO(s.avctx, s.blocks, 64*12*2 * sizeof(DCTELEM), fail)

        //s.blocks = new short[64*12*2];?? Size seems not sync
        s.blocks = new short[2][12][64];
        s.block= s.blocks[0];
        s.pblocks_offset = new int[12];
        for(i=0;i<12;i++){
            s.pblocks_offset[i] = i;
        }

        /*
        if (s.out_format == FMT_H263) {
            FF_ALLOCZ_OR_GOTO(s.avctx, s.ac_val_base, yc_size * sizeof(int16_t) * 16, fail);
            s.ac_val[0] = s.ac_val_base + s.b8_stride + 1;
            s.ac_val[1] = s.ac_val_base + y_size + s.mb_stride + 1;
            s.ac_val[2] = s.ac_val[1] + c_size;
        }
        */

        return 0;
    }

    public static void free_duplicate_context(MpegEncContext s){
        if(s==null) return;

        s.allocated_edge_emu_buffer= null;
        s.me.scratchpad = null;
        s.me.temp=
        s.rd_scratchpad=
        s.b_scratchpad= null;

        s.dct_error_sum = null;
        s.me.map = null;
        s.me.score_map = null;
        s.blocks = null;
        //??????????? used only in MPEG4
        //s.ac_val_base = null;
        s.block = null;
    }
            
    /**
     * init common structure for both encoder and decoder.
     * this assumes that some variables like width/height are already set
     */
    public int MPV_common_init()
    {
        int y_size, c_size, yc_size, i, mb_array_size, x, y, threads;

        // Always H264
        //if(this.codec_id == CODEC_ID_MPEG2VIDEO && !this.progressive_sequence)
        //    this.mb_height = (this.height + 31) / 32 * 2;
        //else if (this.codec_id != CODEC_ID_H264)
        //    this.mb_height = (this.height + 15) / 16;

        if(this.pix_fmt == PIX_FMT_NONE){
            //av_log(this.avctx, AV_LOG_ERROR, "decoding to PIX_FMT_NONE is not supported.\n");
            return -1;
        }

        if(this.thread_count > H264Context.MAX_THREADS || (this.thread_count > this.mb_height && this.mb_height!=0)){
            //av_log(this.avctx, AV_LOG_ERROR, "too many threads\n");
            return -1;
        }

        if((this.width!=0 || this.height!=0) && av_image_check_size(this.width, this.height, 0, this)!=0)
            return -1;

        this.dsp.dsputil_init(this);
        this.ff_dct_common_init();

        //this.flags= this.avctx.flags;
        //this.flags2= this.avctx.flags2;

        this.mb_width  = (this.width  + 15) / 16;
        this.mb_stride = this.mb_width + 1;
        this.b8_stride = this.mb_width*2 + 1;
        this.b4_stride = this.mb_width*4 + 1;
        mb_array_size= this.mb_height * this.mb_stride;
        //mv_table_size= (this.mb_height+2) * this.mb_stride + 1;

        /* set chroma shifts */
        //avcodec_get_chroma_sub_sample(this.pix_fmt,&(this.chroma_x_shift),
        //                                                &(this.chroma_y_shift) );
        this.chroma_x_shift = ImageUtils.av_pix_fmt_descriptors[pix_fmt].log2_chroma_w;
        this.chroma_y_shift = ImageUtils.av_pix_fmt_descriptors[pix_fmt].log2_chroma_h;

        /* set default edge pos, will be overriden in decode_header if needed */
        this.h_edge_pos= this.mb_width*16;
        this.v_edge_pos= this.mb_height*16;

        this.mb_num = this.mb_width * this.mb_height;

        this.block_wrap[0]=
        this.block_wrap[1]=
        this.block_wrap[2]=
        this.block_wrap[3]= this.b8_stride;
        this.block_wrap[4]=
        this.block_wrap[5]= this.mb_stride;

        y_size = this.b8_stride * (2 * this.mb_height + 1);
        c_size = this.mb_stride * (this.mb_height + 1);
        yc_size = y_size + 2 * c_size;

        /* convert fourcc to upper case */
        //?????????????????????????????????????????????????????????
        // We need this????
        //this.codec_tag = ff_toupper4(this.codec_tag);
        //this.stream_codec_tag = ff_toupper4(this.stream_codec_tag);

        this.coded_frame= (AVFrame)this.current_picture;

        //FF_ALLOCZ_OR_GOTO(this.avctx, this.mb_index2xy, (this.mb_num+1)*sizeof(int), fail) //error ressilience code looks cleaner with this
        this.mb_index2xy = new int[this.mb_num+1];
        for(y=0; y<this.mb_height; y++){
            for(x=0; x<this.mb_width; x++){
                this.mb_index2xy[ x + y*this.mb_width ] = x + y*this.mb_stride;
            }
        }
        this.mb_index2xy[ this.mb_height*this.mb_width ] = (this.mb_height-1)*this.mb_stride + this.mb_width; //FIXME really needed?

        /* NOT ENCODE! */
        /*
        if (this.encoding) {
            // Allocate MV tables 
            FF_ALLOCZ_OR_GOTO(this.avctx, this.p_mv_table_base            , mv_table_size * 2 * sizeof(int16_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.b_forw_mv_table_base       , mv_table_size * 2 * sizeof(int16_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.b_back_mv_table_base       , mv_table_size * 2 * sizeof(int16_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.b_bidir_forw_mv_table_base , mv_table_size * 2 * sizeof(int16_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.b_bidir_back_mv_table_base , mv_table_size * 2 * sizeof(int16_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.b_direct_mv_table_base     , mv_table_size * 2 * sizeof(int16_t), fail)
            this.p_mv_table           = this.p_mv_table_base            + this.mb_stride + 1;
            this.b_forw_mv_table      = this.b_forw_mv_table_base       + this.mb_stride + 1;
            this.b_back_mv_table      = this.b_back_mv_table_base       + this.mb_stride + 1;
            this.b_bidir_forw_mv_table= this.b_bidir_forw_mv_table_base + this.mb_stride + 1;
            this.b_bidir_back_mv_table= this.b_bidir_back_mv_table_base + this.mb_stride + 1;
            this.b_direct_mv_table    = this.b_direct_mv_table_base     + this.mb_stride + 1;

            if(this.msmpeg4_version){
                FF_ALLOCZ_OR_GOTO(this.avctx, this.ac_stats, 2*2*(MAX_LEVEL+1)*(MAX_RUN+1)*2*sizeof(int), fail);
            }
            FF_ALLOCZ_OR_GOTO(this.avctx, this.avctx.stats_out, 256, fail);

            // Allocate MB type table 
            FF_ALLOCZ_OR_GOTO(this.avctx, this.mb_type  , mb_array_size * sizeof(uint16_t), fail) //needed for encoding

            FF_ALLOCZ_OR_GOTO(this.avctx, this.lambda_table, mb_array_size * sizeof(int), fail)

            FF_ALLOCZ_OR_GOTO(this.avctx, this.q_intra_matrix  , 64*32   * sizeof(int), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.q_inter_matrix  , 64*32   * sizeof(int), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.q_intra_matrix16, 64*32*2 * sizeof(uint16_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.q_inter_matrix16, 64*32*2 * sizeof(uint16_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.input_picture, MAX_PICTURE_COUNT * sizeof(Picture*), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.reordered_input_picture, MAX_PICTURE_COUNT * sizeof(Picture*), fail)

            if(this.avctx->noise_reduction){
                FF_ALLOCZ_OR_GOTO(this.avctx, this.dct_offset, 2 * 64 * sizeof(uint16_t), fail)
            }
        }
        */
        //FF_ALLOCZ_OR_GOTO(this.avctx, this.picture, MAX_PICTURE_COUNT * sizeof(Picture), fail)
        this.picture = new AVFrame[MAX_PICTURE_COUNT];
        for(i = 0; i < MAX_PICTURE_COUNT; i++) {
            //avcodec_get_frame_defaults((AVFrame)this.picture[i]);
        	this.picture[i] = new AVFrame();
        	this.picture[i].pts= AV_NOPTS_VALUE;
        	this.picture[i].key_frame= 1;
        	//????????????????????????????
        	// set everything in picture[i] to zero
        }

        //FF_ALLOCZ_OR_GOTO(this.avctx, this.error_status_table, mb_array_size*sizeof(uint8_t), fail)
        this.error_status_table = new int[mb_array_size];
        /*
        if(this.codec_id==CODEC_ID_MPEG4 || (this.flags & CODEC_FLAG_INTERLACED_ME)){
            // interlaced direct mode decoding tables 
                for(i=0; i<2; i++){
                    int j, k;
                    for(j=0; j<2; j++){
                        for(k=0; k<2; k++){
                            FF_ALLOCZ_OR_GOTO(this.avctx,    this.b_field_mv_table_base[i][j][k], mv_table_size * 2 * sizeof(int16_t), fail)
                            this.b_field_mv_table[i][j][k] = this.b_field_mv_table_base[i][j][k] + this.mb_stride + 1;
                        }
                        FF_ALLOCZ_OR_GOTO(this.avctx, this.b_field_select_table [i][j], mb_array_size * 2 * sizeof(uint8_t), fail)
                        FF_ALLOCZ_OR_GOTO(this.avctx, this.p_field_mv_table_base[i][j], mv_table_size * 2 * sizeof(int16_t), fail)
                        this.p_field_mv_table[i][j] = this.p_field_mv_table_base[i][j]+ this.mb_stride + 1;
                    }
                    FF_ALLOCZ_OR_GOTO(this.avctx, this.p_field_select_table[i], mb_array_size * 2 * sizeof(uint8_t), fail)
                }
        }
        */
        /*
        if (this.out_format == FMT_H263) {
            // cbp values 
            FF_ALLOCZ_OR_GOTO(this.avctx, this.coded_block_base, y_size, fail);
            this.coded_block= this.coded_block_base + this.b8_stride + 1;

            // cbp, ac_pred, pred_dir 
            FF_ALLOCZ_OR_GOTO(this.avctx, this.cbp_table     , mb_array_size * sizeof(uint8_t), fail)
            FF_ALLOCZ_OR_GOTO(this.avctx, this.pred_dir_table, mb_array_size * sizeof(uint8_t), fail)
        }
		*/
        //if (this.h263_pred || this.h263_plus || !this.encoding) {
            // dc values 
            //MN: we need these for error resilience of intra-frames
            //FF_ALLOCZ_OR_GOTO(this.avctx, this.dc_val_base, yc_size * sizeof(int16_t), fail);
            ErrorResilience.dc_val_base = new int[yc_size];
            ErrorResilience.dc_val[0] = this.b8_stride + 1;
            ErrorResilience.dc_val[1] = y_size + this.mb_stride + 1;
            ErrorResilience.dc_val[2] = ErrorResilience.dc_val[1] + c_size;
            for(i=0;i<yc_size;i++)
            	ErrorResilience.dc_val_base[i] = 1024;
        //}
		
        /* which mb is a intra block */
        //FF_ALLOCZ_OR_GOTO(this.avctx, this.mbintra_table, mb_array_size, fail);
        this.mbintra_table = new int[mb_array_size];
        //memset(this.mbintra_table, 1, mb_array_size);
        Arrays.fill(this.mbintra_table, 1);

        /* init macroblock skip table */
        //FF_ALLOCZ_OR_GOTO(this.avctx, this.mbskip_table, mb_array_size+2, fail);
        this.mbskip_table = new int[mb_array_size+2];
        
        //Note the +1 is for a quicker mpeg4 slice_end detection
        //FF_ALLOCZ_OR_GOTO(this.avctx, this.prev_pict_types, PREV_PICT_TYPES_BUFFER_SIZE, fail);
        this.prev_pict_types = new int[PREV_PICT_TYPES_BUFFER_SIZE];
        	
        this.parse_context.state= -1;
        /*// No debug
        if((this.debug&(FF_DEBUG_VIS_QP|FF_DEBUG_VIS_MB_TYPE)) || (this.avctx->debug_mv)){
           this.visualization_buffer[0] = av_malloc((this.mb_width*16 + 2*EDGE_WIDTH) * this.mb_height*16 + 2*EDGE_WIDTH);
           this.visualization_buffer[1] = av_malloc((this.mb_width*16 + 2*EDGE_WIDTH) * this.mb_height*16 + 2*EDGE_WIDTH);
           this.visualization_buffer[2] = av_malloc((this.mb_width*16 + 2*EDGE_WIDTH) * this.mb_height*16 + 2*EDGE_WIDTH);
        }
        */

        this.context_initialized = 1;

        this.thread_context[0]= this;
        threads = this.thread_count;

        /*
        for(i=1; i<threads; i++){
            this.thread_context[i]= av_malloc(sizeof(MpegEncContext));
            memcpy(this.thread_context[i], s, sizeof(MpegEncContext));
        }
        */

        for(i=0; i<threads; i++){
            if(init_duplicate_context(this.thread_context[i], this) < 0) {
                this.MPV_common_end();
                return -1;
            } // if
            this.thread_context[i].start_mb_y= (this.mb_height*(i  ) + this.thread_count/2) / this.thread_count;
            this.thread_context[i].end_mb_y  = (this.mb_height*(i+1) + this.thread_count/2) / this.thread_count;
        }

        return 0;
    }
    
	/* forget old pics after a seek */
	public void flush_dpb(){
	    H264Context h= (H264Context)this.priv_data;
	    int i;
	    for(i=0; i<H264Context.MAX_DELAYED_PIC_COUNT; i++) {
	        if(h.delayed_pic[i]!=null)
	            h.delayed_pic[i].reference= 0;
	        h.delayed_pic[i]= null;
	    }
	    h.outputed_poc= Integer.MIN_VALUE;
	    h.prev_interlaced_frame = 1;
	    h.idr();
	    if(h.s.current_picture_ptr!=null)
	        h.s.current_picture_ptr.reference= 0;
	    h.s.first_field= 0;
	    SEIDecoder.ff_h264_reset_sei(h);
	    this.ff_mpeg_flush();
	}
	
	public void release_buffer(AVFrame pic) {
	    int i;
	    InternalBuffer buf, last;

	    ////assert(pic->type==FF_BUFFER_TYPE_INTERNAL);
	    ////assert(s->internal_buffer_count);

	    buf = null; /* avoids warning */
	    for(i=0; i<this.internal_buffer_count; i++){ //just 3-5 checks so is not worth to optimize
	        buf= this.internal_buffer[i];
	        if(buf.base[0] == pic.data_base[0] && buf.data_offset[0] == pic.data_offset[0])
	            break;
	    }
	    ////assert(i < s->internal_buffer_count);
	    this.internal_buffer_count--;
	    last = this.internal_buffer[this.internal_buffer_count];

	    //FFSWAP(InternalBuffer, *buf, *last);
	    InternalBuffer tmp = new InternalBuffer();
	    buf.copyInto(tmp);
	    last.copyInto(buf);
	    tmp.copyInto(last);

	    for(i=0; i<4; i++){
	        pic.data_base[i] = null;
	        pic.data_offset[i] = 0;
//	        pic->base[i]=NULL;
	    }
	//printf("R%X\n", pic->opaque);

	    //if(s->debug&FF_DEBUG_BUFFERS)
	    //    av_log(s, AV_LOG_DEBUG, "default_release_buffer called on pic %p, %d buffers used\n", pic, s->internal_buffer_count);		
	}
	
	/**
	 * Release a frame buffer
	 */
	public void free_frame_buffer(AVFrame pic)
	{
	    //this.avctx->release_buffer(this.avctx, (AVFrame*)pic);
		release_buffer(pic);	    
		//av_freep(&pic->hwaccel_picture_private); // No H/W Acceleration
	}
	
	public void ff_mpeg_flush(){
	    int i;
	    //????????????????????????????????
	    MpegEncContext s = this;//(MpegEncContext)this.priv_data;

	    if(s==null || s.picture==null)
	        return;

	    for(i=0; i<MAX_PICTURE_COUNT; i++){
	       if(s.picture[i].data_base[0]!=null && (   s.picture[i].type == FF_BUFFER_TYPE_INTERNAL
	                                    || s.picture[i].type == FF_BUFFER_TYPE_USER))
	        s.free_frame_buffer(s.picture[i]);
	    }
	    s.current_picture_ptr = s.last_picture_ptr = s.next_picture_ptr = null;

	    s.mb_x= s.mb_y= 0;
	    s.closed_gop= 0;

	    s.parse_context.state= -1;
	    s.parse_context.frame_start_found= 0;
	    s.parse_context.overread= 0;
	    s.parse_context.overread_index= 0;
	    s.parse_context.index= 0;
	    s.parse_context.last_index= 0;
	    s.bitstream_buffer_size=0;
	    s.pp_time=0;
	}

	/* init common structure for both encoder and decoder */
	public void MPV_common_end() {
	    int i;
	    //, j, k;

	    for(i=0; i<this.thread_count; i++){
	        //??????
	    	//free_duplicate_context(this.thread_context[i] = null;
	    }
	    /*
	    for(i=1; i<this.avctx->thread_count; i++){
	        av_freep(&this.thread_context[i] = null;
	    }
	    */

	    this.parse_context.buffer_base = null;
	    this.parse_context.buffer_size=0;

	    /* Encoding param??
	    this.mb_type = null;
	    this.p_mv_table_base = null;
	    this.b_forw_mv_table_base = null;
	    this.b_back_mv_table_base = null;
	    this.b_bidir_forw_mv_table_base = null;
	    this.b_bidir_back_mv_table_base = null;
	    this.b_direct_mv_table_base = null;
	    this.p_mv_table= NULL;
	    this.b_forw_mv_table= NULL;
	    this.b_back_mv_table= NULL;
	    this.b_bidir_forw_mv_table= NULL;
	    this.b_bidir_back_mv_table= NULL;
	    this.b_direct_mv_table= NULL;
	    for(i=0; i<2; i++){
	        for(j=0; j<2; j++){
	            for(k=0; k<2; k++){
	                this.b_field_mv_table_base[i][j][k] = null;
	                this.b_field_mv_table[i][j][k]=NULL;
	            }
	            this.b_field_select_table[i][j] = null;
	            this.p_field_mv_table_base[i][j] = null;
	            this.p_field_mv_table[i][j]=NULL;
	        }
	        this.p_field_select_table[i] = null;
	    }

	    this.dc_val_base = null;
	    this.coded_block_base = null;
	    this.mbintra_table = null;
	    this.cbp_table = null;
	    this.pred_dir_table = null;

	    this.mbskip_table = null;
	    this.prev_pict_types = null;
	    this.bitstream_buffer = null;
	    this.allocated_bitstream_buffer_size=0;

	    this.avctx->stats_out = null;
	    this.ac_stats = null;
	    this.error_status_table = null;
	    this.mb_index2xy = null;
	    this.lambda_table = null;
	    this.q_intra_matrix = null;
	    this.q_inter_matrix = null;
	    this.q_intra_matrix16 = null;
	    this.q_inter_matrix16 = null;
	    this.input_picture = null;
	    this.reordered_input_picture = null;
	    this.dct_offset = null;
      */
	    if(this.picture!=null){
	        for(i=0; i<MAX_PICTURE_COUNT; i++){
	            //this.free_picture(s, &this.picture[i] = null;
	        }
	    }
	    this.picture = null;
	    this.context_initialized = 0;
	    this.last_picture_ptr=
	    this.next_picture_ptr=
	    this.current_picture_ptr= null;
	    this.linesize= this.uvlinesize= 0;

	    //for(i=0; i<3; i++)
	    //    this.visualization_buffer[i] = null;

	    //avcodec_default_free_buffers(this.avctx = null;
	}
	
	
	/* generic function for encode/decode called after a frame has been coded/decoded */
	public void MPV_frame_end()
	{
	    //int i;
	    /* draw edge for correct motion prediction if outside */
	    //just to make sure that all data is rendered.
	    /*if(CONFIG_MPEG_XVMC_DECODER && this.avctx->xvmc_acceleration){
	        ff_xvmc_field_end(s);
	    }else*/ if(//0==this.hwaccel
	       //&& 0==(this.codec->capabilities&CODEC_CAP_HWACCEL_VDPAU)
	       0!=this.unrestricted_mv
	       && 0!=this.current_picture.reference
	       && 0==this.intra_only
	       && 0==(this.flags&CODEC_FLAG_EMU_EDGE)) {
	            this.dsp.draw_edges(this.current_picture.data_base[0], this.current_picture.data_offset[0], this.linesize  , this.h_edge_pos   , this.v_edge_pos   , EDGE_WIDTH  );
	            this.dsp.draw_edges(this.current_picture.data_base[1], this.current_picture.data_offset[1], this.uvlinesize, this.h_edge_pos>>1, this.v_edge_pos>>1, EDGE_WIDTH/2);
	            this.dsp.draw_edges(this.current_picture.data_base[2], this.current_picture.data_offset[2], this.uvlinesize, this.h_edge_pos>>1, this.v_edge_pos>>1, EDGE_WIDTH/2);
	    }
	    //emms_c(); // not using MMX

	    this.last_pict_type    = this.pict_type;
	    this.last_lambda_for[this.pict_type]= this.current_picture_ptr.quality;
	    if(this.pict_type!=H264Context.FF_B_TYPE){
	        this.last_non_b_pict_type= this.pict_type;
	    }

	    // clear copies, to avoid confusion
	    this.coded_frame= (AVFrame)this.current_picture_ptr;
	}
	
	public void avcodec_set_dimensions(int width, int height){
		this.coded_width = width;
	    this.coded_height= height;
	    this.width = -((-width )>>this.lowres);
	    this.height= -((-height)>>this.lowres);
	}
	
	public int ff_h264_decode_end() {
	    H264Context h = priv_data;
	    h.ff_h264_free_context();
	    this.MPV_common_end();
//	    memset(h, 0, sizeof(H264Context));
	    return 0;
	}

	/**
	 * sets the given MpegEncContext to common defaults (same for encoding and decoding).
	 * the changed fields will not depend upon the prior state of the MpegEncContext.
	 */
	public void MPV_common_defaults(){
	    this.y_dc_scale_table=
	    this.c_dc_scale_table= ff_mpeg1_dc_scale_table;
	    this.chroma_qscale_table= ff_default_chroma_qscale_table;
	    this.progressive_frame= 1;
	    this.progressive_sequence= 1;
	    this.picture_structure= PICT_FRAME;

	    this.coded_picture_number = 0;
	    this.picture_number = 0;
	    this.input_picture_number = 0;

	    this.picture_in_gop_number = 0;

	    this.f_code = 1;
	    this.b_code = 1;
	}
	
	public int ff_h264_decode_init(){
	    H264Context h= priv_data;

	    this.MPV_common_defaults();

	    h.common_init(this);

	    this.out_format = FMT_H264;
	    //???????????????????
	    //this.workaround_bugs= this.workaround_bugs;

	    // set defaults
//	    this.decode_mb= ff_h263_decode_mb;
	    this.quarter_sample = 1;
	    if(0==this.has_b_frames)
	    this.low_delay= 1;

	    this.chroma_sample_location = AVCHROMA_LOC_LEFT;

	    h.cavlc.ff_h264_decode_init_vlc();

	    h.thread_context[0] = h;
	    h.outputed_poc = Integer.MIN_VALUE;
	    h.prev_poc_msb= 1<<16;
	    h.x264_build = -1;
	    SEIDecoder.ff_h264_reset_sei(h);
	    if(this.codec_id == H264PredictionContext.CODEC_ID_H264){
	        if(this.ticks_per_frame == 1){
	            this.time_base.den *=2;
	        }
	        this.ticks_per_frame = 2;
	    }

	    if(this.extradata_size > 0 && this.extradata!=null &&
	        h.ff_h264_decode_extradata()!=0)
	        return -1;

	    if(h.sps.bitstream_restriction_flag!=0 && this.has_b_frames < h.sps.num_reorder_frames){
	        this.has_b_frames = h.sps.num_reorder_frames;
	        this.low_delay = 0;
	    }

	    return 0;
	}
					
	public void ff_draw_horiz_band(int y, int h){
		//??????? H264 Does not support CODEC_CAP_DRAW_HORIZ_BAND
		/*
	    if (this.draw_horiz_band!=0) {
	        AVFrame src;
	        int field_pic= (this.picture_structure != PICT_FRAME)?1:0;
	        int[] offset = new int[4];

	        h= Math.min(h, (this.height>>field_pic) - y);

	        if(field_pic !=0&& 0==(this.slice_flags&SLICE_FLAG_ALLOW_FIELD)){
	            h <<= 1;
	            y <<= 1;
	            if(this.first_field!=0) return;
	        }

	        if(this.pict_type==H264Context.FF_B_TYPE || this.low_delay!=0 || (this.slice_flags&SLICE_FLAG_CODED_ORDER)!=0)
	            src= (AVFrame)this.current_picture_ptr;
	        else if(this.last_picture_ptr!=null)
	            src= (AVFrame)this.last_picture_ptr;
	        else
	            return;

	        if(this.pict_type==H264Context.FF_B_TYPE && this.picture_structure == PICT_FRAME && this.out_format != FMT_H264){
	            offset[0]=
	            offset[1]=
	            offset[2]=
	            offset[3]= 0;
	        }else{
	            offset[0]= y * this.linesize;
	            offset[1]=
	            offset[2]= (y >> this.chroma_y_shift) * this.uvlinesize;
	            offset[3]= 0;
	        }

	        //emms_c(); // no MMX H/W Acceleration

	        this.draw_horiz_band(src, offset,
	                             y, this.picture_structure, h);
	    }
	    */
	}
	
	public static MpegEncContext avcodec_alloc_context(){
		
		MpegEncContext s = new MpegEncContext();
		
	    //int flags=0;
	    //memset(s, 0, sizeof(AVCodecContext));

	    //s.av_class= &av_codec_context_class;

	    //????????????????????????
	    // Many unused parameters
	    //s.codec_type = codec_type;
	    //flags= AV_OPT_FLAG_VIDEO_PARAM;
	    //s.av_opt_set_defaults2(flags, flags);

	    s.time_base= new AVRational(0,1);
	    
	    //?????????????????????????????????????????/
	    //s.get_buffer= avcodec_default_get_buffer;
	    //s.release_buffer= avcodec_default_release_buffer;
	    //s.get_format= avcodec_default_get_format;
	    //s.execute= avcodec_default_execute;
	    //s.execute2= avcodec_default_execute2;
	    
	    s.sample_aspect_ratio= new AVRational(0,1);
	    s.pix_fmt= PIX_FMT_NONE;
	    //s.sample_fmt= AV_SAMPLE_FMT_NONE;

	    //s.palctrl = null;
	    //s.reget_buffer= avcodec_default_reget_buffer;
	    s.reordered_opaque= AV_NOPTS_VALUE;
	    return s;
	}

	public int avcodec_open(H264Decoder codec) {
	    int ret= -1;

	    /* If there is a user-supplied mutex locking routine, call it. */
	    /*
	    if (ff_lockmgr_cb) {
	        if ((*ff_lockmgr_cb)(&codec_mutex, AV_LOCK_OBTAIN))
	            return -1;
	    }
	    */

	    /*
	    entangled_thread_counter++;
	    if(entangled_thread_counter != 1){
	        av_log(avctx, AV_LOG_ERROR, "insufficient thread locking around avcodec_open/close()\n");
	        goto end;
	    }
	    */

	    //if(this.codec!=null || codec == null)
	    //    return ret;

	    this.priv_data = new H264Context();
	    //priv_data.av_opt_set_defaults();

	    if(this.coded_width!=0 && this.coded_height!=0)
	        this.avcodec_set_dimensions(this.coded_width, this.coded_height);
	    else if(this.width!=0 && this.height!=0)
	        this.avcodec_set_dimensions(this.width, this.height);

	    if ((this.coded_width!=0 || this.coded_height!=0 || this.width!=0 || this.height!=0)
	        && (  av_image_check_size(this.coded_width, this.coded_height, 0, this) < 0
	           || av_image_check_size(this.width,       this.height,       0, this) < 0)) {
	        //av_log(avctx, AV_LOG_WARNING, "ignoring invalid width/height values\n");
	        this.avcodec_set_dimensions(0, 0);
	    }

	    /* if the decoder init function was already called previously,
	       free the already allocated subtitle_header before overwriting it */
	    //??????????????????????
	    //if (codec.decode!=0)
	     //   av_freep(&this.subtitle_header);

	//#define SANE_NB_CHANNELS 128U
	    //if (this.channels > 128) {
	    //    ret = -1;
	    //    return ret;
	   // }

	    this.codec = codec;
	    //if ((this.codec_type == AVMEDIA_TYPE_UNKNOWN || this.codec_type == codec.type) &&
	    //    this.codec_id == CODEC_ID_NONE) {
	    //    this.codec_type = codec.type;
	        this.codec_id   = codec.id;
	    //}
	    //if (this.codec_id != codec.id || (this.codec_type != codec.type
	    //                       && this.codec_type != AVMEDIA_TYPE_ATTACHMENT)) {
	        //av_log(avctx, AV_LOG_ERROR, "codec type or id mismatches\n");
	    //    return ret;
	    //}
	    this.frame_number = 0;
	    if (this.codec.max_lowres < this.lowres) {
	        //av_log(avctx, AV_LOG_ERROR, "The maximum value for lowres supported by the decoder is %d\n",
	        //       this.codec.max_lowres);
	        return ret;
	    }

        ret = this.codec.init(this);
        if (ret < 0) {
        	return ret;
	    } // this
	    ret=0;
	    return ret;
	}

	public void avcodec_default_free_buffers() {
	    int i, j;

	    if(this.internal_buffer==null) return;

	    //if (this.internal_buffer_count != 0)
	    //    av_log(s, AV_LOG_WARNING, "Found %i unreleased buffers!\n", this.internal_buffer_count);
	    for(i=0; i<INTERNAL_BUFFER_SIZE; i++){
	        InternalBuffer buf= this.internal_buffer[i];
	        for(j=0; j<4; j++){
	        	buf.base[j] = null;
	            buf.data_offset[j]= 0;
	        }
	    }
	    this.internal_buffer = null;
	    this.internal_buffer_count=0;
	}
	
	public int avcodec_close() {
	    if (this.codec!=null)
	        this.codec.close(this);
	    avcodec_default_free_buffers();
	    this.coded_frame = null;
	    this.priv_data = null;
	    this.codec = null;
	    return 0;
	}
	
	public int avcodec_decode_video2(AVFrame picture,
			int[] got_picture_ptr /* [0] = in/out param */, AVPacket avpkt) {
		int ret;

		got_picture_ptr[0] = 0;
		if ((this.coded_width != 0 || this.coded_height != 0)
				&& av_image_check_size(this.coded_width, this.coded_height, 0,
						this) != 0)
			return -1;

		this.pkt = avpkt;

		if ((this.codec.capabilities & H264Decoder.CODEC_CAP_DELAY) != 0
				|| avpkt.size != 0) {
			ret = this.codec.decode(this, picture, got_picture_ptr, avpkt);

			// emms_c(); //needed to avoid an emms_c() call before every return;

			picture.pkt_dts = avpkt.dts;

			if (got_picture_ptr[0] != 0)
				this.frame_number++;
		} else
			ret = 0;

		return ret;
	}
	
}

