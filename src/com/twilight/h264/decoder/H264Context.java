package com.twilight.h264.decoder;

import com.twilight.h264.util.*;

public class H264Context {
	
	/**
	 * Value of Picture.reference when Picture is not a reference picture, but
	 * is held for delayed output.
	 */
	public static final int DELAYED_PIC_REF =4;

	public static final int[] sei_num_clock_ts_table={
	    1,  1,  1,  2,  2,  3,  3,  2,  3
	};
	
	/**
	 * SEI message types
	 */
	public static final int     SEI_BUFFERING_PERIOD             =  0; ///< buffering period (H.264, D.1.1)
	public static final int     SEI_TYPE_PIC_TIMING              =  1; ///< picture timing
	public static final int     SEI_TYPE_USER_DATA_UNREGISTERED  =  5; ///< unregistered user data
	public static final int     SEI_TYPE_RECOVERY_POINT          =  6; ///< recovery point (frame # to decoder sync)
	
	/* NAL unit types */
	//enum {
	public static final int NAL_SLICE=1;
	public static final int NAL_DPA=2;
	public static final int NAL_DPB=3;
	public static final int NAL_DPC=4;
	public static final int NAL_IDR_SLICE=5;
	public static final int NAL_SEI=6;
	public static final int NAL_SPS=7;
	public static final int NAL_PPS=8;
	public static final int NAL_AUD=9;
	public static final int NAL_END_SEQUENCE=10;
	public static final int NAL_END_STREAM=11;
	public static final int NAL_FILLER_DATA=12;
	public static final int NAL_SPS_EXT=13;
	public static final int NAL_AUXILIARY_SLICE=19;
	public static final int NAL_FUA=28;
	public static final int NAL_FUB=29;
	//};
	
	public static final int FF_I_TYPE  = 1; ///< Intra
	public static final int FF_P_TYPE  = 2; ///< Predicted
	public static final int FF_B_TYPE  = 3; ///< Bi-dir predicted
	public static final int FF_S_TYPE  = 4; ///< S(GMC)-VOP MPEG4
	public static final int FF_SI_TYPE = 5; ///< Switching Intra
	public static final int FF_SP_TYPE = 6; ///< Switching Predicted
	public static final int FF_BI_TYPE = 7;
	
	public static final int LUMA_DC_BLOCK_INDEX   =24;
	public static final int CHROMA_DC_BLOCK_INDEX =25;

	public static final int CHROMA_DC_COEFF_TOKEN_VLC_BITS =8;
	public static final int COEFF_TOKEN_VLC_BITS           =8;
	public static final int TOTAL_ZEROS_VLC_BITS           =9;
	public static final int CHROMA_DC_TOTAL_ZEROS_VLC_BITS =3;
	public static final int RUN_VLC_BITS                   =3;
	public static final int RUN7_VLC_BITS                  =6;
	
	public static final int MAX_SPS_COUNT = 32;
	public static final int MAX_PPS_COUNT = 256;
	public static final int MAX_SLICES = 16;
	public static final int MAX_DELAYED_PIC_COUNT = 16;
	public static final int MAX_MMCO_COUNT = 66;
	public static final int MAX_THREADS = 1;
	
	public static final int SEI_PIC_STRUCT_FRAME             = 0; ///<  0: %frame
	public static final int SEI_PIC_STRUCT_TOP_FIELD         = 1; ///<  1: top field
	public static final int SEI_PIC_STRUCT_BOTTOM_FIELD      = 2; ///<  2: bottom field
	public static final int SEI_PIC_STRUCT_TOP_BOTTOM        = 3; ///<  3: top field, bottom field, in that order
	public static final int SEI_PIC_STRUCT_BOTTOM_TOP        = 4; ///<  4: bottom field, top field, in that order
	public static final int SEI_PIC_STRUCT_TOP_BOTTOM_TOP    = 5; ///<  5: top field, bottom field, top field repeated, in that order
	public static final int SEI_PIC_STRUCT_BOTTOM_TOP_BOTTOM = 6; ///<  6: bottom field, top field, bottom field repeated, in that order
	public static final int SEI_PIC_STRUCT_FRAME_DOUBLING    = 7; ///<  7: %frame doubling
	public static final int SEI_PIC_STRUCT_FRAME_TRIPLING    = 8; ///<  8: %frame tripling
		
	//The following defines may change, don't expect compatibility if you use them.
	public static final int  MB_TYPE_INTRA4x4   =0x0001;
	public static final int  MB_TYPE_INTRA16x16 =0x0002; //FIXME H.264-specific
	public static final int  MB_TYPE_INTRA_PCM  =0x0004; //FIXME H.264-specific
	public static final int  MB_TYPE_16x16      =0x0008;
	public static final int  MB_TYPE_16x8       =0x0010;
	public static final int  MB_TYPE_8x16       =0x0020;
	public static final int  MB_TYPE_8x8        =0x0040;
	public static final int  MB_TYPE_INTERLACED =0x0080;
	public static final int  MB_TYPE_DIRECT2    =0x0100; //FIXME
	public static final int  MB_TYPE_ACPRED     =0x0200;
	public static final int  MB_TYPE_GMC        =0x0400;
	public static final int  MB_TYPE_SKIP       =0x0800;
	public static final int  MB_TYPE_P0L0       =0x01000;
	public static final int  MB_TYPE_P1L0       =0x02000;
	public static final int  MB_TYPE_P0L1       =0x04000;
	public static final int  MB_TYPE_P1L1       =0x08000;
	public static final int  MB_TYPE_L0         =(MB_TYPE_P0L0 | MB_TYPE_P1L0);
	public static final int  MB_TYPE_L1         =(MB_TYPE_P0L1 | MB_TYPE_P1L1);
	public static final int  MB_TYPE_L0L1       =(MB_TYPE_L0   | MB_TYPE_L1);
	public static final int  MB_TYPE_QUANT      =0x00010000;
	public static final int  MB_TYPE_CBP        =0x00020000;
	//Note bits 24-31 are reserved for codec specific use (h264 ref0, mpeg1 0mv, ...)

	public static final int[] /*uint8_t*/ rem6/*[52]*/={
		0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3,
		};

	public static final int[] /*uint8_t*/ div6/*[52]*/={
		0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8,
		};
	
	public static final int  EXTENDED_SAR          =255;

	public static final int  MB_TYPE_REF0       =MB_TYPE_ACPRED; //dirty but it fits in 16 bit
	public static final int  MB_TYPE_8x8DCT     =0x01000000;
	
	public static final int LIST_NOT_USED = -1; //FIXME rename?
	public static final int PART_NOT_AVAILABLE = -2;

    public MpegEncContext s;
    public H264DSPContext h264dsp = new H264DSPContext();
    public CAVLCContext cavlc = new CAVLCContext();
    public int[] chroma_qp = new int[2]; //QPc

    public int qp_thresh;      ///< QP threshold to skip loopfilter

    public int prev_mb_skipped;
    public int next_mb_skipped;

    //prediction stuff
    public int chroma_pred_mode;
    public int intra16x16_pred_mode;

    public int topleft_mb_xy;
    public int top_mb_xy;
    public int topright_mb_xy;
    public int[] left_mb_xy = new int[2];

    public int topleft_type;
    public int top_type;
    public int topright_type;
    public int[] left_type = new int[2];

    public int[] left_block;
    public int topleft_partition;

    public int[] intra4x4_pred_mode_cache = new int[5*8]; // int8_t intra4x4_pred_mode_cache[5*8];
    public int[] intra4x4_pred_mode; // int8_t (*intra4x4_pred_mode);
    public H264PredictionContext hpc = new H264PredictionContext();
    public long topleft_samples_available;
    public long top_samples_available;
    public long topright_samples_available;
    public long left_samples_available;
    public int[][][] top_borders = new int[2][][/*16+2*8*/]; // uint8_t (*top_borders[2])[16+2*8];
    // -- Array[2] of pointer of Array[16+2*8] of uint_8

    public static final int[] /*enum PixelFormat */hwaccel_pixfmt_list_h264_jpeg_420 = {
        MpegEncContext.PIX_FMT_DXVA2_VLD,
        MpegEncContext.PIX_FMT_VAAPI_VLD,
        MpegEncContext.PIX_FMT_YUVJ420P,
        MpegEncContext.PIX_FMT_NONE
    };

    
    /**
     * non zero coeff count cache.
     * is 64 if not available.
     */
    public int[] non_zero_count_cache = new int[6*8]; // DECLARE_ALIGNED(8, uint8_t, non_zero_count_cache)[6*8];

    /*
    .UU.YYYY
    .UU.YYYY
    .vv.YYYY
    .VV.YYYY
    */
    public int[][] non_zero_count; // uint8_t (*non_zero_count)[32];

    /**
     * Motion vector cache.
     */
    public int[][][] mv_cache = new int[2][5*8][2]; // DECLARE_ALIGNED(16, int16_t, mv_cache)[2][5*8][2];
    public int[][] ref_cache = new int[2][5*8]; // DECLARE_ALIGNED(8, int8_t, ref_cache)[2][5*8];

    /**
     * is 1 if the specific list MV&references are set to 0,0,-2.
     */
	public int[] mv_cache_clean = new int[2];

    /**
     * number of neighbors (top and/or left) that used 8x8 dct
     */
	public int neighbor_transform_size;

    /**
     * block_offset[ 0..23] for frame macroblocks
     * block_offset[24..47] for field macroblocks
     */
	public int[] block_offset = new int[2*(16+8)];

	public long[] mb2b_xy; // uint32_t *mb2b_xy; //FIXME are these 4 a good idea?
	public long[] mb2br_xy; // uint32_t *mb2br_xy;	
    
	public int b_stride; //FIXME use s->b4_stride

	public int mb_linesize;   ///< may be equal to s->linesize or s->linesize*2, for mbaff
	public int mb_uvlinesize;

	public int emu_edge_width;
	public int emu_edge_height;

	public SequenceParameterSet sps = new SequenceParameterSet(); ///< current sps

    /**
     * current pps
     */
	public PictureParameterSet pps = new PictureParameterSet(); //FIXME move to Picture perhaps? (->no) do we need that?

	public long[][][] dequant4_buffer = new long[6][52][16]; // uint32_t dequant4_buffer[6][52][16]; //FIXME should these be moved down?
	public long[][][] dequant8_buffer = new long[2][52][64]; // uint32_t dequant8_buffer[2][52][64];
	public long[][][] dequant4_coeff = new long[16][][]; // uint32_t (*dequant4_coeff[6])[16];
	public long[][][] dequant8_coeff = new long[64][][]; // uint32_t (*dequant8_coeff[2])[64];	
	
	public int slice_num;
	public int[] slice_table_base; // uint16_t*
	public int slice_table_offset; // uint16_t *slice_table;     ///< slice_table_base + 2*mb_stride + 1
	public int slice_type;
	public int slice_type_nos;        ///< S free slice type (SI/SP are remapped to I/P)
	public int slice_type_fixed;

    //interlacing specific flags
	public int mb_aff_frame;
	public int mb_field_decoding_flag;
	public int mb_mbaff;              ///< mb_aff_frame && mb_field_decoding_flag

	public int []sub_mb_type = new int[4]; // DECLARE_ALIGNED(8, uint16_t, sub_mb_type)[4];

    //Weighted pred stuff
	public int use_weight;
	public int use_weight_chroma;
	public int luma_log2_weight_denom;
	public int chroma_log2_weight_denom;
    //The following 2 can be changed to int8_t but that causes 10cpu cycles speedloss
	public int[][][] luma_weight = new int[48][2][2];
	public int[][][][] chroma_weight = new int[48][2][2][2];
	public int[][][] implicit_weight = new int[48][48][2];

	public int direct_spatial_mv_pred;
	public int col_parity;
	public int col_fieldoff;
	public int[] dist_scale_factor = new int[16];
	public int[][] dist_scale_factor_field = new int[2][32];
	public int[][] map_col_to_list0 = new int[2][16+32];
	public int[][][] map_col_to_list0_field = new int[2][2][16+32];

    /**
     * num_ref_idx_l0/1_active_minus1 + 1
     */
	public long[] ref_count = new long[2];   // unsigned int ref_count[2];   ///< counts frames or fields, depending on current mb mode
	public long list_count;
	public int[] list_counts;            ///< Array of list_count per MB specifying the slice type
	public AVFrame[][] ref_list = new AVFrame[2][48];         /**< 0..15: frame refs, 16..47: mbaff field refs.
                                          Reordered version of default_ref_list
                                          according to picture reordering in slice header */
	public int[][][] ref2frm = new int[MAX_SLICES][2][64];  ///< reference to frame number lists, used in the loop filter, the first 2 are for -2,-1

    //data partitioning
	public GetBitContext intra_gb;
	public GetBitContext inter_gb;
	public GetBitContext intra_gb_ptr;
	public GetBitContext inter_gb_ptr;

	public short[] mb = new short[16*24];
	public short[] mb_luma_dc = new short[16];
	public short[][] mb_chroma_dc = new short[2][4];
	public short[] mb_padding = new short[256];        ///< as mb is addressed by scantable[i] and scantable is uint8_t we can either check that i is not too large or ensure that there is some unused stuff after mb

    /**
     * Cabac
     */
	public CABACContext cabac = new CABACContext();
	public int[]      cabac_state = new int[460]; // uint8_t      cabac_state[460];

    /* 0x100 -> non null luma_dc, 0x80/0x40 -> non null chroma_dc (cb/cr), 0x?0 -> chroma_cbp(0,1,2), 0x0? luma_cbp */
	public int[]     cbp_table; // uint16_t     *cbp_table;
	public int cbp;
	public int top_cbp;
	public int left_cbp;
    /* chroma_pred_mode for i4x4 or i16x16, else 0 */
	public int[]     chroma_pred_mode_table; // uint8_t     *chroma_pred_mode_table;
	public int         last_qscale_diff;
	public int[][][]     mvd_table = new int[2][][]; // uint8_t     (*mvd_table[2])[2];
	public int[][][] mvd_cache = new int[2][5*8][2]; // DECLARE_ALIGNED(16, uint8_t, mvd_cache)[2][5*8][2];
	public int[]     direct_table; // uint8_t     *direct_table;
	public int[]     direct_cache = new int[5*8]; // uint8_t     direct_cache[5*8];

	public int[] zigzag_scan = new int[16];
	public int[] zigzag_scan8x8 = new int[64];
	public int[] zigzag_scan8x8_cavlc = new int[64];
	public int[] field_scan = new int[16];
	public int[] field_scan8x8 = new int[64];
	public int[] field_scan8x8_cavlc = new int[64];
	public int[] zigzag_scan_q0;
	public int[] zigzag_scan8x8_q0;
	public int[] zigzag_scan8x8_cavlc_q0;
	public int[] field_scan_q0;
	public int[] field_scan8x8_q0;
	public int[] field_scan8x8_cavlc_q0;
	/*
	uint8_t zigzag_scan[16];
    uint8_t zigzag_scan8x8[64];
    uint8_t zigzag_scan8x8_cavlc[64];
    uint8_t field_scan[16];
    uint8_t field_scan8x8[64];
    uint8_t field_scan8x8_cavlc[64];
    const uint8_t *zigzag_scan_q0;
    const uint8_t *zigzag_scan8x8_q0;
    const uint8_t *zigzag_scan8x8_cavlc_q0;
    const uint8_t *field_scan_q0;
    const uint8_t *field_scan8x8_q0;
    const uint8_t *field_scan8x8_cavlc_q0; 
	 */

	public int x264_build;

	public int mb_xy;

	public int is_complex;

    //deblock
	public int deblocking_filter;         ///< disable_deblocking_filter_idc with 1<->0
	public int slice_alpha_c0_offset;
	public int slice_beta_offset;

//=============================================================
    //Things below are not used in the MB or more inner code

	public int nal_ref_idc;
	public int nal_unit_type;
	public int[][] rbsp_buffer = new int[2][]; // uint8_t *rbsp_buffer[2];
	public long[] rbsp_buffer_size = new long[2]; // unsigned int rbsp_buffer_size[2];

    /**
     * Used to parse AVC variant of h264
     */
	public int is_avc; ///< this flag is != 0 if codec is avc1
	public int nal_length_size; ///< Number of bytes used for nal length (1, 2 or 4)
	public int got_first; ///< this flag is != 0 if we've parsed a frame

	public SequenceParameterSet[] sps_buffers = new SequenceParameterSet[MAX_SPS_COUNT];
	public PictureParameterSet[] pps_buffers = new PictureParameterSet[MAX_PPS_COUNT];

	public int dequant_coeff_pps;     ///< reinit tables when pps changes


    //POC stuff
	public int poc_lsb;
	public int poc_msb;
	public int delta_poc_bottom;
	public int[] delta_poc = new int[2];
	public int frame_num;
	public int prev_poc_msb;             ///< poc_msb of the last reference pic for POC type 0
	public int prev_poc_lsb;             ///< poc_lsb of the last reference pic for POC type 0
	public int frame_num_offset;         ///< for POC type 2
	public int prev_frame_num_offset;    ///< for POC type 2
	public int prev_frame_num;           ///< frame_num of the last pic for POC type 1/2

    /**
     * frame_num for frames or 2*frame_num+1 for field pics.
     */
	public int curr_pic_num;

    /**
     * max_frame_num or 2*max_frame_num for field pics.
     */
	public int max_pic_num;

	public int redundant_pic_count;

	public AVFrame[] short_ref = new AVFrame[32];
	public AVFrame[] long_ref = new AVFrame[32];
	public AVFrame[][] default_ref_list = new AVFrame[2][32]; ///< base reference list for all slices of a coded picture
	public AVFrame[] delayed_pic = new AVFrame[MAX_DELAYED_PIC_COUNT+2]; //FIXME size?
	public int outputed_poc;

    /**
     * memory management control operations buffer.
     */
	public MMCO[] mmco = new MMCO[MAX_MMCO_COUNT];
	public int mmco_index;

	public int long_ref_count;  ///< number of actual long term references
	public int short_ref_count; ///< number of actual short term references

	public int          cabac_init_idc;

    /**
     * @defgroup multithreading Members for slice based multithreading
     * @{
     */
	public H264Context[] thread_context = new H264Context[MAX_THREADS];

    /**
     * current slice number, used to initalize slice_num of each thread/context
     */
	public int current_slice;

    /**
     * Max number of threads / contexts.
     * This is equal to AVCodecContext.thread_count unless
     * multithreaded decoding is impossible, in which case it is
     * reduced to 1.
     */
	public int max_contexts;

    /**
     *  1 if the single thread fallback warning has already been
     *  displayed, 0 otherwise.
     */
	public int single_decode_warning;

	public int last_slice_type;
    /** @} */

    /**
     * pic_struct in picture timing SEI message
     */
	public int sei_pic_struct;

    /**
     * Complement sei_pic_struct
     * SEI_PIC_STRUCT_TOP_BOTTOM and SEI_PIC_STRUCT_BOTTOM_TOP indicate interlaced frames.
     * However, soft telecined frames may have these values.
     * This is used in an attempt to flag soft telecine progressive.
     */
	public int prev_interlaced_frame;

    /**
     * Bit set of clock types for fields/frames in picture timing SEI message.
     * For each found ct_type, appropriate bit is set (e.g., bit 1 for
     * interlaced).
     */
	public int sei_ct_type;

    /**
     * dpb_output_delay in picture timing SEI message, see H.264 C.2.2
     */
	public int sei_dpb_output_delay;

    /**
     * cpb_removal_delay in picture timing SEI message, see H.264 C.1.2
     */
	public int sei_cpb_removal_delay;

    /**
     * recovery_frame_cnt from SEI message
     *
     * Set to -1 if no recovery point SEI message found or to number of frames
     * before playback synchronizes. Frames having recovery point are key
     * frames.
     */
	public int sei_recovery_frame_cnt;

	public int[] luma_weight_flag = new int[2];   ///< 7.4.3.2 luma_weight_lX_flag
	public int[] chroma_weight_flag = new int[2]; ///< 7.4.3.2 chroma_weight_lX_flag

    // Timestamp stuff
	public int sei_buffering_period_present;  ///< Buffering period SEI flag
	public int[] initial_cpb_removal_delay = new int[32]; ///< Initial timestamps for CPBs

    //SVQ3 specific fields
	public int halfpel_flag;
	public int thirdpel_flag;
	public int unknown_svq3_flag;
	public int next_slice_index;
	public long svq3_watermark_key;
	
	public static final int[]/*uint8_t*/ ff_alternate_horizontal_scan = {
		    0,  1,   2,  3,  8,  9, 16, 17,
		    10, 11,  4,  5,  6,  7, 15, 14,
		    13, 12, 19, 18, 24, 25, 32, 33,
		    26, 27, 20, 21, 22, 23, 28, 29,
		    30, 31, 34, 35, 40, 41, 48, 49,
		    42, 43, 36, 37, 38, 39, 44, 45,
		    46, 47, 50, 51, 56, 57, 58, 59,
		    52, 53, 54, 55, 60, 61, 62, 63,
		};

	public static final int[]/*uint8_t*/ ff_alternate_vertical_scan = {
		    0,  8,  16, 24,  1,  9,  2, 10,
		    17, 25, 32, 40, 48, 56, 57, 49,
		    41, 33, 26, 18,  3, 11,  4, 12,
		    19, 27, 34, 42, 50, 58, 35, 43,
		    51, 59, 20, 28,  5, 13,  6, 14,
		    21, 29, 36, 44, 52, 60, 37, 45,
		    53, 61, 22, 30,  7, 15, 23, 31,
		    38, 46, 54, 62, 39, 47, 55, 63,
		};
	
	//////////////////////////////////
	// Decoder for H.264 Symbols
	public static final short[] scan8 = {
		 4+1*8, 5+1*8, 4+2*8, 5+2*8,
		 6+1*8, 7+1*8, 6+2*8, 7+2*8,
		 4+3*8, 5+3*8, 4+4*8, 5+4*8,
		 6+3*8, 7+3*8, 6+4*8, 7+4*8,
		 1+1*8, 2+1*8,
		 1+2*8, 2+2*8,
		 1+4*8, 2+4*8,
		 1+5*8, 2+5*8,
		 4+5*8, 5+5*8, 6+5*8
		};
	
	public static final AVRational[] pixel_aspect = {
		 new AVRational(0, 1),
		 new AVRational(1, 1),
		 new AVRational(12, 11),
		 new AVRational(10, 11),
		 new AVRational(16, 11),
		 new AVRational(40, 33),
		 new AVRational(24, 11),
		 new AVRational(20, 11),
		 new AVRational(32, 11),
		 new AVRational(80, 33),
		 new AVRational(18, 11),
		 new AVRational(15, 11),
		 new AVRational(64, 33),
		 new AVRational(160,99),
		 new AVRational(4, 3),
		 new AVRational(3, 2),
		 new AVRational(2, 1),
		};
	
	public static final int[][] default_scaling4 = {
		{   6,13,20,28,
		   13,20,28,32,
		   20,28,32,37,
		   28,32,37,42
		},{
		   10,14,20,24,
		   14,20,24,27,
		   20,24,27,30,
		   24,27,30,34
		}};

	public static final int[][] default_scaling8 = {
		{   6,10,13,16,18,23,25,27,
		   10,11,16,18,23,25,27,29,
		   13,16,18,23,25,27,29,31,
		   16,18,23,25,27,29,31,33,
		   18,23,25,27,29,31,33,36,
		   23,25,27,29,31,33,36,38,
		   25,27,29,31,33,36,38,40,
		   27,29,31,33,36,38,40,42
		},{
		    9,13,15,17,19,21,22,24,
		   13,13,17,19,21,22,24,25,
		   15,17,19,21,22,24,25,27,
		   17,19,21,22,24,25,27,28,
		   19,21,22,24,25,27,28,30,
		   21,22,24,25,27,28,30,32,
		   22,24,25,27,28,30,32,33,
		   24,25,27,28,30,32,33,35
		}};
	
	
	public static final int /*const uint8_t*/[] ff_zigzag_direct/*[64]*/ = {
		    0,   1,  8, 16,  9,  2,  3, 10,
		    17, 24, 32, 25, 18, 11,  4,  5,
		    12, 19, 26, 33, 40, 48, 41, 34,
		    27, 20, 13,  6,  7, 14, 21, 28,
		    35, 42, 49, 56, 57, 50, 43, 36,
		    29, 22, 15, 23, 30, 37, 44, 51,
		    58, 59, 52, 45, 38, 31, 39, 46,
		    53, 60, 61, 54, 47, 55, 62, 63
		};
	
	public static final int[] ff_h264_chroma_qp/*[52]*/={
		    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,
		   12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,
		   28,29,29,30,31,32,32,33,34,34,35,35,36,36,37,37,
		   37,38,38,38,39,39,39,39
		};
	
	// Store current image to be displayed.
	public AVFrame displayPicture = new AVFrame();
	
	// General Decoder Functions
	
	private /*inline*/ void MAP_F2F_1(int idx, int mb_type, int list) {
		if(0==(mb_type & MB_TYPE_INTERLACED) && this.ref_cache[list][idx] >= 0){
	        this.ref_cache[list][idx] <<= 1;
	        this.mv_cache[list][idx][1] /= 2;
	        this.mvd_cache[list][idx][1] >>=1;
		} // if
	}

	private /*inline*/ void MAP_F2F_2(int idx, int mb_type, int list) {
	    if(0!=(mb_type & MB_TYPE_INTERLACED) && this.ref_cache[list][idx] >= 0){
	        this.ref_cache[list][idx] >>= 1;
	        this.mv_cache[list][idx][1] <<= 1;
	        this.mvd_cache[list][idx][1] <<= 1;
	    }
	}
	
	public /*inline*/ static int pack16to32(int a, int b) {
		//#if HAVE_BIGENDIAN
		//   return (b&0x0000FFFF) + ((a&0x0000FFFF)<<16);
		//#else
		   return (a&0x0000FFFF) + ((b&0x0000FFFF)<<16);
		//#endif
	}

	public /*inline*/ static int pack8to16(int a, int b) {
		//#if HAVE_BIGENDIAN
		//   return (b&0x000000FF) + ((a&0x000000FF)<<8);
		//#else
		   return (a&0x000000FF) + ((b&0x000000FF)<<8);
		//#endif
	}
	
	private /*inline*/ static int mid_pred(int a, int b, int c) {
	    if(a>b){
	        if(c>b){
	            if(c>a) b=a;
	            else    b=c;
	        }
	    }else{
	        if(b>c){
	            if(c>a) b=c;
	            else    b=a;
	        }
	    }
	    return b;
	}

	public int pred_spatial_direct_motion(int mb_type) { // return modified mb_type
	    int b8_stride = 2;
	    int b4_stride = this.b_stride;
	    int mb_xy = this.mb_xy;
	    int[] mb_type_col = new int[2];
	    int[][] l1mv0_base; // [2]
	    int[][] l1mv1_base; // [2]
	    int l1mv0_offset;
	    int l1mv1_offset;
	    //int8_t *l1ref0, *l1ref1;
	    int[] l1ref0_base;
	    int[] l1ref1_base;
	    int l1ref0_offset, l1ref1_offset;
	    int is_b8x8 = (mb_type&MB_TYPE_8x8)!=0?1:0;
	    int sub_mb_type= MB_TYPE_L0L1;
	    int i8, i4;
	    int[] ref = new int[2];
	    int[] mv = new int[2];
	    int list;

//	    //assert(this.ref_list[1][0].reference&3);

	    int MB_TYPE_16x16_OR_INTRA = (MB_TYPE_16x16|MB_TYPE_INTRA4x4|MB_TYPE_INTRA16x16|MB_TYPE_INTRA_PCM);

	    /* ref = min(neighbors) */
	    for(list=0; list<2; list++){
	        int left_ref = this.ref_cache[list][scan8[0] - 1];
	        int top_ref  = this.ref_cache[list][scan8[0] - 8];
	        int refc = this.ref_cache[list][scan8[0] - 8 + 4];
	        int[] C= this.mv_cache[list][ scan8[0] - 8 + 4];
	        if(refc == PART_NOT_AVAILABLE){
	            refc = this.ref_cache[list][scan8[0] - 8 - 1];
	            C    = this. mv_cache[list][scan8[0] - 8 - 1];
	        }
	        ref[list] = Math.min(left_ref, Math.min(top_ref, refc));
	        if(ref[list] >= 0){
	            //this is just pred_motion() but with the cases removed that cannot happen for direct blocks
	            int[] A= this.mv_cache[list][ scan8[0] - 1 ];
	            int[] B= this.mv_cache[list][ scan8[0] - 8 ];

	            int match_count= ((left_ref==ref[list])?1:0) + ((top_ref==ref[list])?1:0) + ((refc==ref[list])?1:0);
	            if(match_count > 1){ //most common
	                mv[list]= pack16to32(mid_pred(A[0], B[0], C[0]),
	                                     mid_pred(A[1], B[1], C[1]) );
	            }else {
	                //assert(match_count==1);
	                if(left_ref==ref[list]){
	                    mv[list]= ((A[1] & 0x0000FFFF) << 16) | (A[0] & 0x0000FFFF);//AV_RN32A(A);
	                }else if(top_ref==ref[list]){
	                    mv[list]= ((B[1] & 0x0000FFFF) << 16) | (B[0] & 0x0000FFFF);//AV_RN32A(B);
	                }else{
	                    mv[list]= ((C[1] & 0x0000FFFF) << 16) | (C[0] & 0x0000FFFF);//AV_RN32A(C);
	                }
	            }
	        }else{
	            int mask= ~(MB_TYPE_L0 << (2*list));
	            mv[list] = 0;
	            ref[list] = -1;
	            if(is_b8x8 == 0)
	                mb_type &= mask;
	            sub_mb_type &= mask;
	        }
	    }
	    if(ref[0] < 0 && ref[1] < 0){
	        ref[0] = ref[1] = 0;
	        if(is_b8x8 == 0)
	            mb_type |= MB_TYPE_L0L1;
	        sub_mb_type |= MB_TYPE_L0L1;
	    }

	    if(0 == (is_b8x8|mv[0]|mv[1])){
	        Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[0], 4, 4, 8, (int)ref[0], 1);
	        Rectangle.fill_rectangle_sign(this.ref_cache[1], scan8[0], 4, 4, 8, (int)ref[1], 1);
	        Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[0], 4, 4, 8, 0, 4);
	        Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[0], 4, 4, 8, 0, 4);
	        mb_type= (mb_type & ~(MB_TYPE_8x8|MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_P1L0|MB_TYPE_P1L1))|MB_TYPE_16x16|MB_TYPE_DIRECT2;
	        return mb_type;
	    }

	    if(0 != (this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy] & MB_TYPE_INTERLACED)){ // AFL/AFR/FR/FL -> AFL/FL
	        if(0 == (mb_type & MB_TYPE_INTERLACED)){                    //     AFR/FR    -> AFL/FL
	            mb_xy= s.mb_x + ((s.mb_y&~1) + this.col_parity)*s.mb_stride;
	            b8_stride = 0;
	        }else{
	            mb_xy += this.col_fieldoff; // non zero for FL -> FL & differ parity
	        }
	        //goto single_col;
//	    	single_col:
            mb_type_col[0] =
            mb_type_col[1] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy];

            sub_mb_type |= MB_TYPE_16x16|MB_TYPE_DIRECT2; /* B_SUB_8x8 */
            if(0==is_b8x8 && 0!=(mb_type_col[0] & MB_TYPE_16x16_OR_INTRA)){
                mb_type   |= MB_TYPE_16x16|MB_TYPE_DIRECT2; /* B_16x16 */
            }else if(0==is_b8x8 && 0!=(mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16))){
                mb_type   |= MB_TYPE_DIRECT2 | (mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16));
            }else{
                if(0==this.sps.direct_8x8_inference_flag){
                    /* FIXME save sub mb types from previous frames (or derive from MVs)
                    * so we know exactly what block size to use */
                    sub_mb_type += (MB_TYPE_8x8-MB_TYPE_16x16); /* B_SUB_4x4 */
                }
                mb_type   |= MB_TYPE_8x8;
            }
	        
	    }else{                                               // AFL/AFR/FR/FL -> AFR/FR
	        if(0 != (mb_type & MB_TYPE_INTERLACED)){                     // AFL       /FL -> AFR/FR
	            mb_xy= s.mb_x + (s.mb_y&~1)*s.mb_stride;
	            mb_type_col[0] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy];
	            mb_type_col[1] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy + s.mb_stride];
	            b8_stride = 2+4*s.mb_stride;
	            b4_stride *= 6;

	            sub_mb_type |= MB_TYPE_16x16|MB_TYPE_DIRECT2; /* B_SUB_8x8 */
	            if(    0!=(mb_type_col[0] & MB_TYPE_16x16_OR_INTRA)
	                && 0!=(mb_type_col[1] & MB_TYPE_16x16_OR_INTRA)
	                && 0==is_b8x8){
	                mb_type   |= MB_TYPE_16x8 |MB_TYPE_DIRECT2; /* B_16x8 */
	            }else{
	                mb_type   |= MB_TYPE_8x8;
	            }
	        }else{                                           //     AFR/FR    -> AFR/FR
//	single_col:
	            mb_type_col[0] =
	            mb_type_col[1] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy];

	            sub_mb_type |= MB_TYPE_16x16|MB_TYPE_DIRECT2; /* B_SUB_8x8 */
	            if(0==is_b8x8 && 0!=(mb_type_col[0] & MB_TYPE_16x16_OR_INTRA)){
	                mb_type   |= MB_TYPE_16x16|MB_TYPE_DIRECT2; /* B_16x16 */
	            }else if(0==is_b8x8 && 0!=(mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16))){
	                mb_type   |= MB_TYPE_DIRECT2 | (mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16));
	            }else{
	                if(0==this.sps.direct_8x8_inference_flag){
	                    /* FIXME save sub mb types from previous frames (or derive from MVs)
	                    * so we know exactly what block size to use */
	                    sub_mb_type += (MB_TYPE_8x8-MB_TYPE_16x16); /* B_SUB_4x4 */
	                }
	                mb_type   |= MB_TYPE_8x8;
	            }
	        }
	    }

	    l1mv0_base  = this.ref_list[1][0].motion_val_base[0];
	    l1mv1_base  = this.ref_list[1][0].motion_val_base[1];
	    l1mv0_offset = this.ref_list[1][0].motion_val_offset[0] + (int)this.mb2b_xy [mb_xy];
	    l1mv1_offset = this.ref_list[1][0].motion_val_offset[1] + (int)this.mb2b_xy [mb_xy];
	    //l1ref0 = &this.ref_list[1][0].ref_index [0][4*mb_xy];
	    //l1ref1 = &this.ref_list[1][0].ref_index [1][4*mb_xy];
	    l1ref0_base = this.ref_list[1][0].ref_index [0];
	    l1ref1_base = this.ref_list[1][0].ref_index [1];
	    l1ref0_offset = 4*mb_xy;
	    l1ref1_offset = 4*mb_xy;
	    
	    if(0==b8_stride){
	        if(0!=(s.mb_y&1)){
	        	l1ref0_offset += 2;
	        	l1ref1_offset += 2;
	        	l1mv0_offset  +=  2*b4_stride;
	        	l1mv1_offset  +=  2*b4_stride;
	        }
	    }


	        if((mb_type & MB_TYPE_INTERLACED) != (mb_type_col[0] & MB_TYPE_INTERLACED)){
	            int n=0;
	            for(i8=0; i8<4; i8++){
	                int x8 = i8&1;
	                int y8 = i8>>1;
	                int xy8 = x8+y8*b8_stride;
	                int xy4 = 3*x8+y8*b4_stride;
	                int a,b;

	                if(is_b8x8 != 0 && 0 == (this.sub_mb_type[i8] & MB_TYPE_DIRECT2))
	                    continue;
	                this.sub_mb_type[i8] = sub_mb_type;

	                Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[i8*4], 2, 2, 8, (int)ref[0], 1);
	                Rectangle.fill_rectangle_sign(this.ref_cache[1], scan8[i8*4], 2, 2, 8, (int)ref[1], 1);
	                if(0 == (mb_type_col[y8] & 7) && 0 == this.ref_list[1][0].long_ref
	                   && (   (l1ref0_base[l1ref0_offset + xy8] == 0 && Math.abs(l1mv0_base[l1mv0_offset + xy4][0]) <= 1 && Math.abs(l1mv0_base[l1mv0_offset + xy4][1]) <= 1)
	                       || (l1ref0_base[l1ref0_offset + xy8]  < 0 && l1ref1_base[l1ref1_offset + xy8] == 0 && Math.abs(l1mv1_base[l1mv1_offset + xy4][0]) <= 1 && Math.abs(l1mv1_base[l1mv1_offset + xy4][1]) <= 1))){
	                    a=b=0;
	                    if(ref[0] > 0)
	                        a= mv[0];
	                    if(ref[1] > 0)
	                        b= mv[1];
	                    n++;
	                }else{
	                    a= mv[0];
	                    b= mv[1];
	                }
	                Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[i8*4], 2, 2, 8, a, 4);
	                Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[i8*4], 2, 2, 8, b, 4);
	            }
	            if(0==is_b8x8 && 0==(n&3))
	                mb_type= (mb_type & ~(MB_TYPE_8x8|MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_P1L0|MB_TYPE_P1L1))|MB_TYPE_16x16|MB_TYPE_DIRECT2;
	        }else if(0 != (mb_type&MB_TYPE_16x16)){
	            int a,b;

	            Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[0], 4, 4, 8, (int)ref[0], 1);
	            Rectangle.fill_rectangle_sign(this.ref_cache[1], scan8[0], 4, 4, 8, (int)ref[1], 1);
	            if(0==(mb_type_col[0] & 7) && 0==this.ref_list[1][0].long_ref
	               && (   (l1ref0_base[l1ref0_offset + 0] == 0 && Math.abs(l1mv0_base[l1mv0_offset + 0][0]) <= 1 && Math.abs(l1mv0_base[l1mv0_offset + 0][1]) <= 1)
	                   || (l1ref0_base[l1ref0_offset + 0]  < 0 && l1ref1_base[l1ref1_offset + 0] == 0 && Math.abs(l1mv1_base[l1mv1_offset + 0][0]) <= 1 && Math.abs(l1mv1_base[l1mv1_offset + 0][1]) <= 1
	                       && this.x264_build>33))){
	                a=b=0;
	                if(ref[0] > 0)
	                    a= mv[0];
	                if(ref[1] > 0)
	                    b= mv[1];
	            }else{
	                a= mv[0];
	                b= mv[1];
	            }
	            Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[0], 4, 4, 8, a, 4);
	            Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[0], 4, 4, 8, b, 4);
	        }else{
	            int n=0;
	            for(i8=0; i8<4; i8++){
	                int x8 = i8&1;
	                int y8 = i8>>1;

	                if(is_b8x8 != 0 && 0 == (this.sub_mb_type[i8] & MB_TYPE_DIRECT2))
	                    continue;
	                this.sub_mb_type[i8] = sub_mb_type;

	                Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[i8*4], 2, 2, 8, mv[0], 4);
	                Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[i8*4], 2, 2, 8, mv[1], 4);
	                Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[i8*4], 2, 2, 8, (int)ref[0], 1);
	                Rectangle.fill_rectangle_sign(this.ref_cache[1], scan8[i8*4], 2, 2, 8, (int)ref[1], 1);

	                //assert(b8_stride==2);
	                /* col_zero_flag */
	                if(0==(mb_type_col[0] & 7) && 0==this.ref_list[1][0].long_ref && (   l1ref0_base[l1ref0_offset + i8] == 0
	                                              || (l1ref0_base[l1ref0_offset + i8] < 0 && l1ref1_base[l1ref1_offset + i8] == 0
	                                                  && this.x264_build>33))){
	                    int[][] l1mv_base= l1ref0_base[l1ref0_offset + i8] == 0 ? l1mv0_base : l1mv1_base;
	                    int l1mv_offset= l1ref0_base[l1ref0_offset + i8] == 0 ? l1mv0_offset : l1mv1_offset;
	                    if(0 != (sub_mb_type & MB_TYPE_16x16)){
	                        int[] mv_col = l1mv_base[l1mv_offset + x8*3 + y8*3*b4_stride];
	                        if(Math.abs(mv_col[0]) <= 1 && Math.abs(mv_col[1]) <= 1){
	                            if(ref[0] == 0)
	                            	Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[i8*4], 2, 2, 8, 0, 4);
	                            if(ref[1] == 0)
	                            	Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[i8*4], 2, 2, 8, 0, 4);
	                            n+=4;
	                        }
	                    }else{
	                        int m=0;
	                    for(i4=0; i4<4; i4++){
	                        int[] mv_col = l1mv_base[l1mv_offset + x8*2 + (i4&1) + (y8*2 + (i4>>1))*b4_stride];
	                        if(Math.abs(mv_col[0]) <= 1 && Math.abs(mv_col[1]) <= 1){
	                            if(ref[0] == 0)
	                            	Arrays.fill(this.mv_cache[0][scan8[i8*4+i4]], 0, 2, 0);
	                                //AV_ZERO32(this.mv_cache[0][scan8[i8*4+i4]]);
	                            if(ref[1] == 0)
	                            	Arrays.fill(this.mv_cache[1][scan8[i8*4+i4]], 0, 2, 0);
	                            	//AV_ZERO32(this.mv_cache[1][scan8[i8*4+i4]]);
	                            m++;
	                        }
	                    }
	                    if(0 == (m&3))
	                        this.sub_mb_type[i8]+= MB_TYPE_16x16 - MB_TYPE_8x8;
	                    n+=m;
	                    }
	                }
	            }
	            if(0 == is_b8x8 && 0 == (n&15))
	                mb_type= (mb_type & ~(MB_TYPE_8x8|MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_P1L0|MB_TYPE_P1L1))|MB_TYPE_16x16|MB_TYPE_DIRECT2;
	        }
	        return mb_type;
	}

	public int pred_temp_direct_motion(int mb_type) { // return modified mb_type
	    int b8_stride = 2;
	    int b4_stride = this.b_stride;
	    int mb_xy = this.mb_xy;
	    int[] mb_type_col = new int[2];
	    //const int16_t (*l1mv0)[2], (*l1mv1)[2];
	    //const int8_t *l1ref0, *l1ref1;
	    int[][] l1mv0_base;
	    int[][] l1mv1_base;
	    int l1mv0_offset;
	    int l1mv1_offset;
	    int[] l1ref0_base;
	    int[] l1ref1_base;
	    int l1ref0_offset, l1ref1_offset;
	    
	    int is_b8x8 = (mb_type & MB_TYPE_8x8)!=0?1:0;
	    int sub_mb_type;
	    int i8, i4;

	    int MB_TYPE_16x16_OR_INTRA = (MB_TYPE_16x16|MB_TYPE_INTRA4x4|MB_TYPE_INTRA16x16|MB_TYPE_INTRA_PCM);
	    
	    ////assert(this.ref_list[1][0].reference&3);

	    if(0 != (this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy] & MB_TYPE_INTERLACED)){ // AFL/AFR/FR/FL -> AFL/FL
	        if(0 == (mb_type & MB_TYPE_INTERLACED)){                    //     AFR/FR    -> AFL/FL
	            mb_xy= s.mb_x + ((s.mb_y&~1) + this.col_parity)*s.mb_stride;
	            b8_stride = 0;
	        }else{
	            mb_xy += this.col_fieldoff; // non zero for FL -> FL & differ parity
	        }
//	        goto single_col;
	    	//single_col:
            mb_type_col[0] =
            mb_type_col[1] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy];

            sub_mb_type = MB_TYPE_16x16|MB_TYPE_P0L0|MB_TYPE_P0L1|MB_TYPE_DIRECT2; /* B_SUB_8x8 */
            if(0 == is_b8x8 && (mb_type_col[0] & MB_TYPE_16x16_OR_INTRA) != 0){
                mb_type   |= MB_TYPE_16x16|MB_TYPE_P0L0|MB_TYPE_P0L1|MB_TYPE_DIRECT2; /* B_16x16 */
            }else if(0 == is_b8x8 && (mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16)) != 0){
                mb_type   |= MB_TYPE_L0L1|MB_TYPE_DIRECT2 | (mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16));
            }else{
                if(0 == this.sps.direct_8x8_inference_flag){
                    /* FIXME save sub mb types from previous frames (or derive from MVs)
                    * so we know exactly what block size to use */
                    sub_mb_type = MB_TYPE_8x8|MB_TYPE_P0L0|MB_TYPE_P0L1|MB_TYPE_DIRECT2; /* B_SUB_4x4 */
                }
                mb_type   |= MB_TYPE_8x8|MB_TYPE_L0L1;
            }
	        
	    }else{                                               // AFL/AFR/FR/FL -> AFR/FR
	        if(0 != (mb_type & MB_TYPE_INTERLACED)){                     // AFL       /FL -> AFR/FR
	            mb_xy= s.mb_x + (s.mb_y&~1)*s.mb_stride;
	            mb_type_col[0] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy];
	            mb_type_col[1] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy + s.mb_stride];
	            b8_stride = 2+4*s.mb_stride;
	            b4_stride *= 6;

	            sub_mb_type = MB_TYPE_16x16|MB_TYPE_P0L0|MB_TYPE_P0L1|MB_TYPE_DIRECT2; /* B_SUB_8x8 */

	            if(    (mb_type_col[0] & MB_TYPE_16x16_OR_INTRA) != 0
	                && (mb_type_col[1] & MB_TYPE_16x16_OR_INTRA) != 0
	                && 0 == is_b8x8){
	                mb_type   |= MB_TYPE_16x8 |MB_TYPE_L0L1|MB_TYPE_DIRECT2; /* B_16x8 */
	            }else{
	                mb_type   |= MB_TYPE_8x8|MB_TYPE_L0L1;
	            }
	        }else{                                           //     AFR/FR    -> AFR/FR
//	single_col:
	            mb_type_col[0] =
	            mb_type_col[1] = (int)this.ref_list[1][0].mb_type_base[this.ref_list[1][0].mb_type_offset + mb_xy];

	            sub_mb_type = MB_TYPE_16x16|MB_TYPE_P0L0|MB_TYPE_P0L1|MB_TYPE_DIRECT2; /* B_SUB_8x8 */
	            if(0 == is_b8x8 && (mb_type_col[0] & MB_TYPE_16x16_OR_INTRA) != 0){
	                mb_type   |= MB_TYPE_16x16|MB_TYPE_P0L0|MB_TYPE_P0L1|MB_TYPE_DIRECT2; /* B_16x16 */
	            }else if(0 == is_b8x8 && (mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16)) != 0){
	                mb_type   |= MB_TYPE_L0L1|MB_TYPE_DIRECT2 | (mb_type_col[0] & (MB_TYPE_16x8|MB_TYPE_8x16));
	            }else{
	                if(0 == this.sps.direct_8x8_inference_flag){
	                    /* FIXME save sub mb types from previous frames (or derive from MVs)
	                    * so we know exactly what block size to use */
	                    sub_mb_type = MB_TYPE_8x8|MB_TYPE_P0L0|MB_TYPE_P0L1|MB_TYPE_DIRECT2; /* B_SUB_4x4 */
	                }
	                mb_type   |= MB_TYPE_8x8|MB_TYPE_L0L1;
	            }
	        }
	    }

	    //l1mv0  = &this.ref_list[1][0].motion_val[0][this.mb2b_xy [mb_xy]];
	    //l1mv1  = &this.ref_list[1][0].motion_val[1][this.mb2b_xy [mb_xy]];
	    //l1ref0 = &this.ref_list[1][0].ref_index [0][4*mb_xy];
	    //l1ref1 = &this.ref_list[1][0].ref_index [1][4*mb_xy];
	    l1mv0_base  = this.ref_list[1][0].motion_val_base[0];
	    l1mv1_base  = this.ref_list[1][0].motion_val_base[1];
	    l1mv0_offset = this.ref_list[1][0].motion_val_offset[0] + (int)this.mb2b_xy [mb_xy];
	    l1mv1_offset = this.ref_list[1][0].motion_val_offset[1] + (int)this.mb2b_xy [mb_xy];
	    l1ref0_base = this.ref_list[1][0].ref_index [0];
	    l1ref1_base = this.ref_list[1][0].ref_index [1];
	    l1ref0_offset = 4*mb_xy;
	    l1ref1_offset = 4*mb_xy;
	    	    
	    if(0 == b8_stride){
	        if((s.mb_y&1) != 0){
	            l1ref0_offset += 2;
	            l1ref1_offset += 2;
	            l1mv0_offset  +=  2*b4_stride;
	            l1mv1_offset  +=  2*b4_stride;
	        }
	    }

	    {
	        //int[][] map_col_to_list = new int[][] {this.map_col_to_list0[0], this.map_col_to_list0[1]};
	        int[] dist_scale_factor = this.dist_scale_factor;
	        int ref_offset;

	        if(0!= mb_aff_frame && 0 != (mb_type & MB_TYPE_INTERLACED)){
	            map_col_to_list0[0] = this.map_col_to_list0_field[s.mb_y&1][0];
	            map_col_to_list0[1] = this.map_col_to_list0_field[s.mb_y&1][1];
	            dist_scale_factor   =this.dist_scale_factor_field[s.mb_y&1];
	        }
	        ref_offset = (this.ref_list[1][0].mbaff<<4) & (mb_type_col[0]>>3); //if(this.ref_list[1][0].mbaff && IS_INTERLACED(mb_type_col[0])) ref_offset=16 else 0

	        if((mb_type & MB_TYPE_INTERLACED) != (mb_type_col[0] & MB_TYPE_INTERLACED)){
	            //int y_shift  = 2*!(mb_type & MB_TYPE_INTERLACED);
	        	int y_shift  = 2*((mb_type & MB_TYPE_INTERLACED)==0?1:0);
	            ////assert(this.sps.direct_8x8_inference_flag);

	            for(i8=0; i8<4; i8++){
	                int x8 = i8&1;
	                int y8 = i8>>1;
	                int ref0, scale;
	                int[][] l1mv_base = l1mv0_base;
	                int l1mv_offset = l1mv0_offset;

	                if(0!= is_b8x8 && 0 == (this.sub_mb_type[i8] & MB_TYPE_DIRECT2))
	                    continue;
	                this.sub_mb_type[i8] = sub_mb_type;

	                Rectangle.fill_rectangle_sign(this.ref_cache[1], scan8[i8*4], 2, 2, 8, 0, 1);
	                if(0!=(mb_type_col[y8] & 7)){
	                	Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[i8*4], 2, 2, 8, 0, 1);
	                	Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[i8*4], 2, 2, 8, 0, 4);
	                	Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[i8*4], 2, 2, 8, 0, 4);
	                    continue;
	                }

	                ref0 = l1ref0_base[l1ref0_offset + x8 + y8*b8_stride];
	                if(ref0 >= 0)
	                    ref0 = map_col_to_list0[0][ref0 + ref_offset];
	                else{
	                    ref0 = map_col_to_list0[1][l1ref1_base[l1ref1_offset + x8 + y8*b8_stride] + ref_offset];
	                    l1mv_base = l1mv1_base;
	                    l1mv_offset = l1mv1_offset;
	                }
	                scale = dist_scale_factor[ref0];
	                Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[i8*4], 2, 2, 8, ref0, 1);

	                {
	                    int[] mv_col = l1mv_base[l1mv_offset + x8*3 + y8*b4_stride];
	                    int my_col = (mv_col[1]<<y_shift)/2;
	                    int mx = (scale * mv_col[0] + 128) >> 8;
	                    int my = (scale * my_col + 128) >> 8;
	                    Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[i8*4], 2, 2, 8, pack16to32(mx,my), 4);
	                    Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[i8*4], 2, 2, 8, pack16to32(mx-mv_col[0],my-my_col), 4);
	                }
	            }
	            return mb_type;
	        }

	        /* one-to-one mv scaling */

	        if(0 != (mb_type & MB_TYPE_16x16)){
	            int ref, mv0, mv1;

	            Rectangle.fill_rectangle_sign(this.ref_cache[1], scan8[0], 4, 4, 8, 0, 1);
	            if(0 != (mb_type_col[0] & 7)){
	                ref=mv0=mv1=0;
	            }else{
	                int ref0 = l1ref0_base[l1ref0_offset + 0] >= 0 ? map_col_to_list0[0][l1ref0_base[l1ref0_offset + 0] + ref_offset]
	                                                : map_col_to_list0[1][l1ref1_base[l1ref1_offset + 0] + ref_offset];
	                int scale = dist_scale_factor[ref0];
	                int[] mv_col = l1ref0_base[l1ref0_offset + 0] >= 0 ? l1mv0_base[l1mv0_offset + 0] : l1mv1_base[l1mv1_offset + 0];
	                int[] mv_l0 = new int[2];
	                mv_l0[0] = (scale * mv_col[0] + 128) >> 8;
	                mv_l0[1] = (scale * mv_col[1] + 128) >> 8;
	                ref= ref0;
	                mv0= pack16to32(mv_l0[0],mv_l0[1]);
	                mv1= pack16to32(mv_l0[0]-mv_col[0],mv_l0[1]-mv_col[1]);
	            }
	            Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[0], 4, 4, 8, ref, 1);
	            Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[0], 4, 4, 8, mv0, 4);
	            Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[0], 4, 4, 8, mv1, 4);
	        }else{
	            for(i8=0; i8<4; i8++){
	                int x8 = i8&1;
	                int y8 = i8>>1;
	                int ref0, scale;
	                int[][] l1mv_base = l1mv0_base;
	                int l1mv_offset = l1mv0_offset;

	                if(0!= is_b8x8 && 0== (this.sub_mb_type[i8] & MB_TYPE_DIRECT2))
	                    continue;
	                this.sub_mb_type[i8] = sub_mb_type;
	                Rectangle.fill_rectangle_sign(this.ref_cache[1], scan8[i8*4], 2, 2, 8, 0, 1);
	                if(0!= (mb_type_col[0] & 7)){
	                	Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[i8*4], 2, 2, 8, 0, 1);
	                	Rectangle.fill_rectangle_mv_cache(this. mv_cache[0], scan8[i8*4], 2, 2, 8, 0, 4);
	                	Rectangle.fill_rectangle_mv_cache(this. mv_cache[1], scan8[i8*4], 2, 2, 8, 0, 4);
	                    continue;
	                }

	                ////assert(b8_stride == 2);
	                ref0 = l1ref0_base[l1ref0_offset + i8];
	                if(ref0 >= 0)
	                    ref0 = map_col_to_list0[0][ref0 + ref_offset];
	                else{
	                    ref0 = map_col_to_list0[1][l1ref1_base[l1ref1_offset + i8] + ref_offset];
	                    l1mv_base= l1mv1_base;
	                    l1mv_offset= l1mv1_offset;
	                }
	                scale = dist_scale_factor[ref0];

	                Rectangle.fill_rectangle_sign(this.ref_cache[0], scan8[i8*4], 2, 2, 8, ref0, 1);
	                if(0 != (sub_mb_type & MB_TYPE_16x16)){
	                    int[] mv_col = l1mv_base[l1mv_offset + x8*3 + y8*3*b4_stride];
	                    int mx = (scale * mv_col[0] + 128) >> 8;
	                    int my = (scale * mv_col[1] + 128) >> 8;
	                    Rectangle.fill_rectangle_mv_cache(this.mv_cache[0], scan8[i8*4], 2, 2, 8, pack16to32(mx,my), 4);
	                    Rectangle.fill_rectangle_mv_cache(this.mv_cache[1], scan8[i8*4], 2, 2, 8, pack16to32(mx-mv_col[0],my-mv_col[1]), 4);
	                }else
	                for(i4=0; i4<4; i4++){
	                    int[] mv_col = l1mv_base[l1mv_offset + x8*2 + (i4&1) + (y8*2 + (i4>>1))*b4_stride];
	                    int[] mv_l0 = this.mv_cache[0][scan8[i8*4+i4]];
	                    mv_l0[0] = (scale * mv_col[0] + 128) >> 8;
	                    mv_l0[1] = (scale * mv_col[1] + 128) >> 8;
	                    //AV_WN32A(this.mv_cache[1][scan8[i8*4+i4]],
	                    //    pack16to32(mv_l0[0]-mv_col[0],mv_l0[1]-mv_col[1]));
	                    this.mv_cache[1][scan8[i8*4+i4]][0] = mv_l0[0]-mv_col[0];
	                    this.mv_cache[1][scan8[i8*4+i4]][1] = mv_l0[1]-mv_col[1];
	                }
	            }
	        }
	    }
	    return mb_type;
	}
		
	public int ff_h264_pred_direct_motion(int mb_type) { // return modified mb_type
	    if(direct_spatial_mv_pred != 0){
	        return pred_spatial_direct_motion(mb_type);
	    }else{
	        return pred_temp_direct_motion(mb_type);
	    }
	}
	
	public void pred_pskip_motion(int[] mxmy /* {mx, my} as inout param */){
	    int top_ref = this.ref_cache[0][ scan8[0] - 8 ];
	    int left_ref= this.ref_cache[0][ scan8[0] - 1 ];

	    //tprintf(h->s.avctx, "pred_pskip: (%d) (%d) at %2d %2d\n", top_ref, left_ref, h->s.mb_x, h->s.mb_y);

	    if(top_ref == PART_NOT_AVAILABLE || left_ref == PART_NOT_AVAILABLE
	       || 0 == ( top_ref | ((this.mv_cache[0][ scan8[0] - 8 ][1] & 0x0000FFFF) << 16) | (this.mv_cache[0][ scan8[0] - 8 ][0] & 0x0000FFFF) )
	       || 0 == (left_ref | ((this.mv_cache[0][ scan8[0] - 1 ][1] & 0x0000FFFF) << 16) | (this.mv_cache[0][ scan8[0] - 1 ][0] & 0x0000FFFF) ) ) {

//	       || !( top_ref | AV_RN32A(h->mv_cache[0][ scan8[0] - 8 ]))
//	       || !(left_ref | AV_RN32A(h->mv_cache[0][ scan8[0] - 1 ]))){
	    	
	        //*mx = *my = 0;
	    	mxmy[0] = mxmy[1] = 0;
	        return;
	    }

	    pred_motion(0, 4, 0, 0, mxmy);
	    return;
	}

	public int fetch_diagonal_mv(int[][] pC /* int[] C as inout param */, int i, int list, int part_width) {
	    int topright_ref= this.ref_cache[list][ i - 8 + part_width ];

	    /* there is no consistent mapping of mvs to neighboring locations that will
	     * make mbaff happy, so we can't move all this logic to fill_caches */
	    if(mb_aff_frame != 0){
/*
	#define SET_DIAG_MV(MV_OP, REF_OP, XY, Y4)\
	                const int xy = XY, y4 = Y4;\
	                const int mb_type = mb_types[xy+(y4>>2)*s.mb_stride];\
	                if(!USES_LIST(mb_type,list))\
	                    return LIST_NOT_USED;\
	                mv = s.current_picture_ptr->motion_val[list][h->mb2b_xy[xy]+3 + y4*h->b_stride];\
	                h->mv_cache[list][scan8[0]-2][0] = mv[0];\
	                h->mv_cache[list][scan8[0]-2][1] = mv[1] MV_OP;\
	                return s.current_picture_ptr->ref_index[list][4*xy+1 + (y4&~1)] REF_OP;
*/
	        if(topright_ref == PART_NOT_AVAILABLE
	           && i >= scan8[0]+8 && (i&7)==4
	           && this.ref_cache[list][scan8[0]-1] != PART_NOT_AVAILABLE){
	            long[] mb_types_base = s.current_picture_ptr.mb_type_base;
	            int mb_types_offset = s.current_picture_ptr.mb_type_offset;
	            int[] mv;
	            Arrays.fill(this.mv_cache[list][scan8[0]-2], 0, 2, 0); // size = int_16 x 2
	            pC[0] = this.mv_cache[list][scan8[0]-2];

	            if(0 == mb_field_decoding_flag
	               && 0 != (this.left_type[0] & MB_TYPE_INTERLACED)){
	                //SET_DIAG_MV(*2, >>1, this.left_mb_xy[0]+s.mb_stride, (s.mb_y&1)*2+(i>>5));
	            	//#define SET_DIAG_MV(MV_OP, REF_OP, XY, Y4)\
	                int xy = this.left_mb_xy[0]+s.mb_stride, y4 = (s.mb_y&1)*2+(i>>5);
	                int mb_type = (int)mb_types_base[mb_types_offset + xy+(y4>>2)*s.mb_stride];
	                if(0 == ((mb_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) 
	                )
	                    return LIST_NOT_USED;
	                mv = s.current_picture_ptr.motion_val_base[list][s.current_picture_ptr.motion_val_offset[list] + (int)this.mb2b_xy[xy]+3 + y4*this.b_stride];
	                this.mv_cache[list][scan8[0]-2][0] = mv[0];
	                this.mv_cache[list][scan8[0]-2][1] = mv[1] *2;
	                return s.current_picture_ptr.ref_index[list][4*xy+1 + (y4&~1)] >>1;

	                ////assert(this.left_mb_xy[0] == this.left_mb_xy[1]);
	            }
	            if(0 != mb_field_decoding_flag
	               && 0 == (this.left_type[0] & MB_TYPE_INTERLACED)){
	                // left shift will turn LIST_NOT_USED into PART_NOT_AVAILABLE, but that's OK.
	                //SET_DIAG_MV(/2, <<1, this.left_mb_xy[i>=36], ((i>>2))&3);
	            	//#define SET_DIAG_MV(MV_OP, REF_OP, XY, Y4)\
	                int xy = this.left_mb_xy[i>=36?1:0], y4 = ((i>>2))&3;
	                int mb_type = (int)mb_types_base[mb_types_offset + xy+(y4>>2)*s.mb_stride];
	                if(0 == ((mb_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) 
	                	)
	                    return LIST_NOT_USED;
	                mv = s.current_picture_ptr.motion_val_base[list][s.current_picture_ptr.motion_val_offset[list] + (int)this.mb2b_xy[xy]+3 + y4*this.b_stride];
	                this.mv_cache[list][scan8[0]-2][0] = mv[0];
	                this.mv_cache[list][scan8[0]-2][1] = mv[1] /2;
	                return s.current_picture_ptr.ref_index[list][4*xy+1 + (y4&~1)] <<1;
	            }
	        }
	    }

	    if(topright_ref != PART_NOT_AVAILABLE){
	        pC[0]= this.mv_cache[list][ i - 8 + part_width ];
	        return topright_ref;
	    }else{
	        //tprintf(s.avctx, "topright MV not available\n");

	        pC[0]= this.mv_cache[list][ i - 8 - 1 ];
	        return this.ref_cache[list][ i - 8 - 1 ];
	    }
	}
	
	/**
	 * gets the predicted MV.
	 * @param n the block index
	 * @param part_width the width of the partition (4, 8,16) -> (1, 2, 4)
	 * @param mx the x component of the predicted motion vector
	 * @param my the y component of the predicted motion vector
	 */
	public void pred_motion(int n, int part_width, int list, int ref, int[] mxmy /* {mx, my} as inout param */){
	    int index8= scan8[n];
	    int top_ref=      this.ref_cache[list][ index8 - 8 ];
	    int left_ref=     this.ref_cache[list][ index8 - 1 ];
	    int[] A= this.mv_cache[list][ index8 - 1 ];
	    int[] B= this.mv_cache[list][ index8 - 8 ];
	    int[] C = new int[0];
	    int diagonal_ref, match_count;

	    //assert(part_width==1 || part_width==2 || part_width==4);

	/* mv_cache
	  B . . A T T T T
	  U . . L . . , .
	  U . . L . . . .
	  U . . L . . , .
	  . . . L . . . .
	*/

	    int[][] pC = new int[][] { C };
	    diagonal_ref= fetch_diagonal_mv(pC, index8, list, part_width);
	    C = pC[0];
	    match_count= ((diagonal_ref==ref)?1:0) + ((top_ref==ref)?1:0) + ((left_ref==ref)?1:0);
	    //tprintf(this.s.avctx, "pred_motion match_count=%d\n", match_count);
	    if(match_count > 1){ //most common
	        mxmy[0]= mid_pred(A[0], B[0], C[0]);
	        mxmy[1]= mid_pred(A[1], B[1], C[1]);
	    }else if(match_count==1){
	        if(left_ref==ref){
	            mxmy[0]= A[0];
	            mxmy[1]= A[1];
	        }else if(top_ref==ref){
	        	mxmy[0]= B[0];
	        	mxmy[1]= B[1];
	        }else{
	        	mxmy[0]= C[0];
	        	mxmy[1]= C[1];
	        }
	    }else{
	        if(top_ref == PART_NOT_AVAILABLE && diagonal_ref == PART_NOT_AVAILABLE && left_ref != PART_NOT_AVAILABLE){
	        	mxmy[0]= A[0];
	        	mxmy[1]= A[1];
	        }else{
	        	mxmy[0]= mid_pred(A[0], B[0], C[0]);
	        	mxmy[1]= mid_pred(A[1], B[1], C[1]);
	        }
	    }
	    //tprintf(this.s.avctx, "pred_motion (%2d %2d %2d) (%2d %2d %2d) (%2d %2d %2d) -> (%2d %2d %2d) at %2d %2d %d list %d\n", top_ref, B[0], B[1],                    diagonal_ref, C[0], C[1], left_ref, A[0], A[1], ref, *mx, *my, this.s.mb_x, this.s.mb_y, n, list);
	}
	
	/**
	 * gets the directionally predicted 16x8 MV.
	 * @param n the block index
	 * @param mx the x component of the predicted motion vector
	 * @param my the y component of the predicted motion vector
	 */
	public /*inline*/ void pred_16x8_motion(int n, int list, int ref, int[] mxmy){
	    if(n==0){
	        int top_ref=      this.ref_cache[list][ scan8[0] - 8 ];
	        int[] B= this.mv_cache[list][ scan8[0] - 8 ];

	        //tprintf(this.s.avctx, "pred_16x8: (%2d %2d %2d) at %2d %2d %d list %d\n", top_ref, B[0], B[1], h->s.mb_x, h->s.mb_y, n, list);

	        if(top_ref == ref){
	            mxmy[0]= B[0];
	            mxmy[1]= B[1];
	            return;
	        }
	    }else{
	        int left_ref=     this.ref_cache[list][ scan8[8] - 1 ];
	        int[] A= this.mv_cache[list][ scan8[8] - 1 ];

	        //tprintf(this.s.avctx, "pred_16x8: (%2d %2d %2d) at %2d %2d %d list %d\n", left_ref, A[0], A[1], h->s.mb_x, h->s.mb_y, n, list);

	        if(left_ref == ref){
	        	mxmy[0]= A[0];
	        	mxmy[1]= A[1];
	            return;
	        }
	    }

	    //RARE
	    pred_motion(n, 4, list, ref, mxmy);
	}
	
	/**
	 * gets the directionally predicted 8x16 MV.
	 * @param n the block index
	 * @param mx the x component of the predicted motion vector
	 * @param my the y component of the predicted motion vector
	 */
	public /*inline*/ void pred_8x16_motion(int n, int list, int ref, int[] mxmy) {
	    if(n==0){
	        int left_ref=      this.ref_cache[list][ scan8[0] - 1 ];
	        int[] A=  this.mv_cache[list][ scan8[0] - 1 ];

	        //tprintf(this.s.avctx, "pred_8x16: (%2d %2d %2d) at %2d %2d %d list %d\n", left_ref, A[0], A[1], h->s.mb_x, h->s.mb_y, n, list);

	        if(left_ref == ref){
	        	mxmy[0]= A[0];
	        	mxmy[1]= A[1];
	            return;
	        }
	    }else{
	        int[] C = null;
	        int diagonal_ref;
	        int[][] pC = new int[][] { C };
	        diagonal_ref= fetch_diagonal_mv(pC, (int)scan8[4], list, 2);
	        C = pC[0];

	        //tprintf(this.s.avctx, "pred_8x16: (%2d %2d %2d) at %2d %2d %d list %d\n", diagonal_ref, C[0], C[1], h->s.mb_x, h->s.mb_y, n, list);

	        if(diagonal_ref == ref){
	            mxmy[0]= C[0];
	            mxmy[1]= C[1];
	            return;
	        }
	    }

	    //RARE
	    pred_motion(n, 2, list, ref, mxmy);
	}
			
	public void fill_decode_neighbors(int mb_type) {
	    int mb_xy= this.mb_xy;
	    int topleft_xy, top_xy, topright_xy;
	    int[] left_xy = new int[2];
	    int[][] left_block_options = {
	        {0,1,2,3,7,10,8,11,7+0*8, 7+1*8, 7+2*8, 7+3*8, 2+0*8, 2+3*8, 2+1*8, 2+2*8},
	        {2,2,3,3,8,11,8,11,7+2*8, 7+2*8, 7+3*8, 7+3*8, 2+1*8, 2+2*8, 2+1*8, 2+2*8},
	        {0,0,1,1,7,10,7,10,7+0*8, 7+0*8, 7+1*8, 7+1*8, 2+0*8, 2+3*8, 2+0*8, 2+3*8},
	        {0,2,0,2,7,10,7,10,7+0*8, 7+2*8, 7+0*8, 7+2*8, 2+0*8, 2+3*8, 2+0*8, 2+3*8}
	    };

	    this.topleft_partition= -1;

	    top_xy     = mb_xy  - (s.mb_stride << mb_field_decoding_flag);

	    /* Wow, what a mess, why didn't they simplify the interlacing & intra
	     * stuff, I can't imagine that these complex rules are worth it. */

	    topleft_xy = top_xy - 1;
	    topright_xy= top_xy + 1;
	    left_xy[1] = left_xy[0] = mb_xy-1;
	    this.left_block = left_block_options[0];
	    if(mb_aff_frame != 0){
	        int left_mb_field_flag     = (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy-1] & MB_TYPE_INTERLACED);
	        int curr_mb_field_flag     = (int)(mb_type & MB_TYPE_INTERLACED);
	        if((s.mb_y&1) != 0){
	            if (left_mb_field_flag != curr_mb_field_flag) {
	                left_xy[1] = left_xy[0] = mb_xy - s.mb_stride - 1;
	                if (curr_mb_field_flag != 0) {
	                    left_xy[1] += s.mb_stride;
	                    this.left_block = left_block_options[3];
	                } else {
	                    topleft_xy += s.mb_stride;
	                    // take top left mv from the middle of the mb, as opposed to all other modes which use the bottom right partition
	                    this.topleft_partition = 0;
	                    this.left_block = left_block_options[1];
	                }
	            }
	        }else{
	            if(curr_mb_field_flag != 0){
	                topleft_xy  += s.mb_stride & (((s.current_picture.mb_type_base[s.current_picture.mb_type_offset + top_xy - 1]>>7)&1)-1);
	                topright_xy += s.mb_stride & (((s.current_picture.mb_type_base[s.current_picture.mb_type_offset + top_xy + 1]>>7)&1)-1);
	                top_xy      += s.mb_stride & (((s.current_picture.mb_type_base[s.current_picture.mb_type_offset + top_xy    ]>>7)&1)-1);
	            }
	            if (left_mb_field_flag != curr_mb_field_flag) {
	                if (curr_mb_field_flag != 0) {
	                    left_xy[1] += s.mb_stride;
	                    this.left_block = left_block_options[3];
	                } else {
	                    this.left_block = left_block_options[2];
	                }
	            }
	        }
	    }

	    this.topleft_mb_xy = topleft_xy;
	    this.top_mb_xy     = top_xy;
	    this.topright_mb_xy= topright_xy;
	    this.left_mb_xy[0] = left_xy[0];
	    this.left_mb_xy[1] = left_xy[1];
	    //FIXME do we need all in the context?

	    this.topleft_type = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + topleft_xy] ;
	    this.top_type     = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + top_xy]     ;
	    this.topright_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + topright_xy];
	    this.left_type[0] = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + left_xy[0]] ;
	    this.left_type[1] = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + left_xy[1]] ;

	    {
	        if(this.slice_table_base[this.slice_table_offset + topleft_xy ] != this.slice_num){
	            this.topleft_type = 0;
	            if(this.slice_table_base[this.slice_table_offset + top_xy     ] != this.slice_num) this.top_type     = 0;
	            if(this.slice_table_base[this.slice_table_offset + left_xy[0] ] != this.slice_num) this.left_type[0] = this.left_type[1] = 0;
	        }
	    }
	    if(this.slice_table_base[this.slice_table_offset + topright_xy] != this.slice_num) this.topright_type= 0;
	}
		
	public void fill_decode_caches(int mb_type){
	    int topleft_xy, top_xy, topright_xy;
	    int[] left_xy = new int[2];
	    int topleft_type, top_type, topright_type;
	    int[] left_type = new int[2];
	    int i;

	    topleft_xy   = this.topleft_mb_xy ;
	    top_xy       = this.top_mb_xy     ;
	    topright_xy  = this.topright_mb_xy;
	    left_xy[0]   = this.left_mb_xy[0] ;
	    left_xy[1]   = this.left_mb_xy[1] ;
	    topleft_type = this.topleft_type  ;
	    top_type     = this.top_type      ;
	    topright_type= this.topright_type ;
	    left_type[0] = this.left_type[0]  ;
	    left_type[1] = this.left_type[1]  ;

	    if(((mb_type)&MB_TYPE_SKIP) == 0){
	        if((mb_type & 7) != 0){
	            int type_mask = (this.pps.constrained_intra_pred!=0)? 1 : -1;
	            this.topleft_samples_available=
	            	this.top_samples_available=
	            		this.left_samples_available= 0x0000FFFF;
	            this.topright_samples_available= 0x0000EEEA;

	            if(0 == (top_type & type_mask)){
	            	this.topleft_samples_available= 0x0000B3FF;
	            	this.top_samples_available= 0x000033FF;
	            	this.topright_samples_available= 0x000026EA;
	            }
	            if((mb_type & MB_TYPE_INTERLACED) != (left_type[0] & MB_TYPE_INTERLACED)){
	                if((mb_type & MB_TYPE_INTERLACED) != 0 ){
	                    if(0 == (left_type[0] & type_mask)){
	                    	this.topleft_samples_available&= 0x0000DFFF;
	                    	this.left_samples_available&= 0x00005FFF;
	                    }
	                    if(0 == (left_type[1] & type_mask)){
	                    	this.topleft_samples_available&= 0x0000FF5F;
	                    	this.left_samples_available&= 0x0000FF5F;
	                    }
	                }else{
	                    int left_typei = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + left_xy[0] + s.mb_stride];

	                    //assert(left_xy[0] == left_xy[1]);
	                    if(!(((left_typei & type_mask)!=0) && ((left_type[0] & type_mask)!=0))){
	                    	this.topleft_samples_available&= 0x0000DF5F;
	                    	this.left_samples_available&= 0x00005F5F;
	                    }
	                }
	            }else{
	                if(0 == (left_type[0] & type_mask)){
	                	this.topleft_samples_available&= 0x0000DF5F;
	                	this.left_samples_available&= 0x00005F5F;
	                }
	            }

	            if(0 == (topleft_type & type_mask))
	            	this.topleft_samples_available&= 0x00007FFF;

	            if(0 == (topright_type & type_mask))
	            	this.topright_samples_available&= 0x0000FBFF;

	            if((mb_type & MB_TYPE_INTRA4x4) != 0){
	                if((top_type & MB_TYPE_INTRA4x4) != 0){
	                    this.intra4x4_pred_mode_cache[4+8*0] = this.intra4x4_pred_mode[0 + (int)this.mb2br_xy[top_xy]];
	                    this.intra4x4_pred_mode_cache[5+8*0] = this.intra4x4_pred_mode[1 + (int)this.mb2br_xy[top_xy]];
	                    this.intra4x4_pred_mode_cache[6+8*0] = this.intra4x4_pred_mode[2 + (int)this.mb2br_xy[top_xy]];
	                    this.intra4x4_pred_mode_cache[7+8*0] = this.intra4x4_pred_mode[3 + (int)this.mb2br_xy[top_xy]];
	                }else{
	                	this.intra4x4_pred_mode_cache[4+8*0]=
	                		this.intra4x4_pred_mode_cache[5+8*0]=
	                			this.intra4x4_pred_mode_cache[6+8*0]=
	                				this.intra4x4_pred_mode_cache[7+8*0]= 2 - 3*((top_type & type_mask)!=0?0:1);
	                }
	                for(i=0; i<2; i++){
	                    if((left_type[i] & MB_TYPE_INTRA4x4) != 0 ){
	                        int mode_offset = (int)mb2br_xy[left_xy[i]]; // intra4x4_pred_mode;
	                        this.intra4x4_pred_mode_cache[3+8*1 + 2*8*i]= intra4x4_pred_mode[mode_offset + 6-left_block[0+2*i]];
	                        this.intra4x4_pred_mode_cache[3+8*2 + 2*8*i]= intra4x4_pred_mode[mode_offset + 6-left_block[1+2*i]];
	                    }else{
	                    	this.intra4x4_pred_mode_cache[3+8*1 + 2*8*i]=
	                    		this.intra4x4_pred_mode_cache[3+8*2 + 2*8*i]= 2 - 3*((left_type[i] & type_mask)!=0?0:1);
	                    }
	                }
	            }
	        }


	/*
	0 . T T. T T T T
	1 L . .L . . . .
	2 L . .L . . . .
	3 . T TL . . . .
	4 L . .L . . . .
	5 L . .. . . . .
	*/
	//FIXME constraint_intra_pred & partitioning & nnz (let us hope this is just a typo in the spec)
	    if(top_type != 0){
	        non_zero_count_cache[4+8*0] = non_zero_count[top_xy][4+3*8];
	        non_zero_count_cache[5+8*0] = non_zero_count[top_xy][5+3*8];
	        non_zero_count_cache[6+8*0] = non_zero_count[top_xy][6+3*8];
	        non_zero_count_cache[7+8*0] = non_zero_count[top_xy][7+3*8];
	                                                             
        	non_zero_count_cache[1+8*0]= non_zero_count[top_xy][1+1*8];
            non_zero_count_cache[2+8*0]= non_zero_count[top_xy][2+1*8];

            non_zero_count_cache[1+8*3]= non_zero_count[top_xy][1+2*8];
            non_zero_count_cache[2+8*3]= non_zero_count[top_xy][2+2*8];
	    }else {
	    	/*
	            non_zero_count_cache[1+8*0]=
	            non_zero_count_cache[2+8*0]=

	            non_zero_count_cache[1+8*3]=
	            non_zero_count_cache[2+8*3]=
	            AV_WN32A(&non_zero_count_cache[4+8*0], pps.cabac != 0 && 0 == (mb_type & 7) ? 0 : 0x40404040);
	        */
	    		non_zero_count_cache[1+8*0]=
	            non_zero_count_cache[2+8*0]=

	            non_zero_count_cache[1+8*3]=
	            non_zero_count_cache[2+8*3]=
	            	
	            non_zero_count_cache[4+8*0]=
	            non_zero_count_cache[5+8*0]=
	            non_zero_count_cache[6+8*0]=
	            non_zero_count_cache[7+8*0]= (pps.cabac != 0 && 0 == (mb_type & 7) ? 0 : 0x040);
	    }

	    for (i=0; i<2; i++) {
	        if(left_type[i] != 0){
	            non_zero_count_cache[3+8*1 + 2*8*i]= non_zero_count[left_xy[i]][left_block[8+0+2*i]];
	            non_zero_count_cache[3+8*2 + 2*8*i]= non_zero_count[left_xy[i]][left_block[8+1+2*i]];
	                non_zero_count_cache[0+8*1 +   8*i]= non_zero_count[left_xy[i]][left_block[8+4+2*i]];
	                non_zero_count_cache[0+8*4 +   8*i]= non_zero_count[left_xy[i]][left_block[8+5+2*i]];
	        }else{
	                non_zero_count_cache[3+8*1 + 2*8*i]=
	                non_zero_count_cache[3+8*2 + 2*8*i]=
	                non_zero_count_cache[0+8*1 +   8*i]=
	                non_zero_count_cache[0+8*4 +   8*i]= pps.cabac != 0 && 0 == (mb_type & 7) ? 0 : 64;
	        }
	    }

	    if( pps.cabac != 0 ) {
	        // top_cbp
	        if(top_type != 0) {
	            this.top_cbp = this.cbp_table[top_xy];
	        } else {
	        	this.top_cbp = (mb_type & 7)!=0 ? 0x1CF : 0x00F;
	        }
	        // left_cbp
	        if (left_type[0] != 0) {
	        	this.left_cbp = (this.cbp_table[left_xy[0]] & 0x01f0)
	                        |  ((this.cbp_table[left_xy[0]]>>(left_block[0]&(~1)))&2)
	                        | (((this.cbp_table[left_xy[1]]>>(left_block[2]&(~1)))&2) << 2);
	        } else {
	        	this.left_cbp = (mb_type & 7) != 0 ? 0x1CF : 0x00F;
	        }
	    }
	    }

	    if(((mb_type)&(MB_TYPE_16x16|MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_8x8)) != 0 || ((mb_type & MB_TYPE_DIRECT2)!=0 && direct_spatial_mv_pred!=0)){
	        int list;
	        for(list=0; list<list_count; list++){
	            if( ((mb_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) == 0
	            		){
	                /*if(!this.mv_cache_clean[list]){
	                    memset(this.mv_cache [list],  0, 8*5*2*sizeof(int16_t)); //FIXME clean only input? clean at all?
	                    memset(this.ref_cache[list], PART_NOT_AVAILABLE, 8*5*sizeof(int8_t));
	                    this.mv_cache_clean[list]= 1;
	                }*/
	                continue;
	            }
	            ////assert(0 == ((mb_type & MB_TYPE_DIRECT2) != 0 && 0 == direct_spatial_mv_pred));

	            mv_cache_clean[list]= 0;

	            if(((top_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	            		){
	                int b_xy= (int)this.mb2b_xy[top_xy] + 3*this.b_stride;
	                //AV_COPY128(this.mv_cache[list][scan8[0] + 0 - 1*8], s.current_picture.motion_val[list][b_xy + 0]);
	                // Fill 8 int16_t in mv_cache
	                for(int j=0;j<4;j++) {
	                	this.mv_cache[list][scan8[0] + 0 - 1*8 + j][0] = s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] + b_xy + 0 + j][0];
	                	this.mv_cache[list][scan8[0] + 0 - 1*8 + j][1] = s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] + b_xy + 0 + j][1];
	                } // for
	                
	                this.ref_cache[list][scan8[0] + 0 - 1*8]=
	                	this.ref_cache[list][scan8[0] + 1 - 1*8]= s.current_picture.ref_index[list][4*top_xy + 2];
	                	this.ref_cache[list][scan8[0] + 2 - 1*8]=
	                		this.ref_cache[list][scan8[0] + 3 - 1*8]= s.current_picture.ref_index[list][4*top_xy + 3];
	            }else{
	            	//AV_ZERO128(h->mv_cache[list][scan8[0] + 0 - 1*8]);
	            	// Zero 8 int16_t in mv_cache
	            	for(int j=0;j<4;j++) {
	            		this.mv_cache[list][scan8[0] + 0 - 1*8 + j][0] = 0;
	            		this.mv_cache[list][scan8[0] + 0 - 1*8 + j][1] = 0;
	            	}
	                //AV_WN32A(&this.ref_cache[list][scan8[0] + 0 - 1*8], ((top_type !=0? LIST_NOT_USED : PART_NOT_AVAILABLE)&0xFF)*0x01010101);

	            	//// !!!????????????? Need to be unsigned
	            	Arrays.fill(this.ref_cache[list], scan8[0] + 0 - 1*8, scan8[0] + 0 - 1*8 +4, ((top_type !=0? LIST_NOT_USED : PART_NOT_AVAILABLE)/*&0xFF*/));
	            }

	            if((mb_type & (MB_TYPE_16x8|MB_TYPE_8x8)) != 0){
	            for(i=0; i<2; i++){
	                int cache_idx = scan8[0] - 1 + i*2*8;
	                if(((left_type[i]) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	                	){
	                    int b_xy= (int)this.mb2b_xy[left_xy[i]] + 3;
	                    int b8_xy= 4*left_xy[i] + 1;
	                    //AV_COPY32(this.mv_cache[list][cache_idx  ], s.current_picture.motion_val[list][b_xy + this.b_stride*left_block[0+i*2]]);
	                    //AV_COPY32(this.mv_cache[list][cache_idx+8], s.current_picture.motion_val[list][b_xy + this.b_stride*left_block[1+i*2]]);
	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] +b_xy + this.b_stride*left_block[0+i*2]], 0, this.mv_cache[list][cache_idx  ], 0, 2);
	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] +b_xy + this.b_stride*left_block[1+i*2]], 0, this.mv_cache[list][cache_idx+8], 0, 2);
	                    this.ref_cache[list][cache_idx  ]= s.current_picture.ref_index[list][b8_xy + (left_block[0+i*2]&~1)];
	                    this.ref_cache[list][cache_idx+8]= s.current_picture.ref_index[list][b8_xy + (left_block[1+i*2]&~1)];
	                }else{
	                    //AV_ZERO32(this.mv_cache [list][cache_idx  ]);
	                    //AV_ZERO32(this.mv_cache [list][cache_idx+8]);
		            	Arrays.fill(this.mv_cache [list][cache_idx  ],0,2,0);
		            	Arrays.fill(this.mv_cache [list][cache_idx+8  ],0,2,0);

	                    this.ref_cache[list][cache_idx  ]=
	                    	this.ref_cache[list][cache_idx+8]= (left_type[i]!=0) ? LIST_NOT_USED : PART_NOT_AVAILABLE;
	                }
	            }
	            }else{
	                if(((left_type[0]) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	                		){
	                    int b_xy= (int)this.mb2b_xy[left_xy[0]] + 3;
	                    int b8_xy= 4*left_xy[0] + 1;
	                    //AV_COPY32(this.mv_cache[list][scan8[0] - 1], s.current_picture.motion_val[list][b_xy + this.b_stride*left_block[0]]);
	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] + b_xy + this.b_stride*left_block[0]],0,this.mv_cache[list][scan8[0] - 1],0,2);

	                    this.ref_cache[list][scan8[0] - 1]= s.current_picture.ref_index[list][b8_xy + (left_block[0]&~1)];
	                }else{
	                    //AV_ZERO32(this.mv_cache [list][scan8[0] - 1]);
	                	Arrays.fill(this.mv_cache [list][scan8[0] - 1],0,2,0);
	                    this.ref_cache[list][scan8[0] - 1]= left_type[0]!=0 ? LIST_NOT_USED : PART_NOT_AVAILABLE;
	                }
	            }

	            if(((topright_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	            		){
	                int b_xy= (int)this.mb2b_xy[topright_xy] + 3*this.b_stride;
	                //AV_COPY32(this.mv_cache[list][scan8[0] + 4 - 1*8], s.current_picture.motion_val[list][b_xy]);
	                System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] + b_xy], 0, this.mv_cache[list][scan8[0] + 4 - 1*8], 0, 2);
	                this.ref_cache[list][scan8[0] + 4 - 1*8]= s.current_picture.ref_index[list][4*topright_xy + 2];
	            }else{
	                //AV_ZERO32(this.mv_cache [list][scan8[0] + 4 - 1*8]);
	            	Arrays.fill(this.mv_cache [list][scan8[0] + 4 - 1*8], 0, 2, 0);
	                this.ref_cache[list][scan8[0] + 4 - 1*8]= topright_type != 0? LIST_NOT_USED : PART_NOT_AVAILABLE;
	            }
	            if(this.ref_cache[list][scan8[0] + 4 - 1*8] < 0){
	                if(((topleft_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	                		){
	                    int b_xy = (int)this.mb2b_xy [topleft_xy] + 3 + this.b_stride + (this.topleft_partition & 2*this.b_stride);
	                    int b8_xy= 4*topleft_xy + 1 + (this.topleft_partition & 2);
	                    //AV_COPY32(this.mv_cache[list][scan8[0] - 1 - 1*8], s.current_picture.motion_val[list][b_xy]);
	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] + b_xy], 0, this.mv_cache[list][scan8[0] - 1 - 1*8], 0, 2);
	                    this.ref_cache[list][scan8[0] - 1 - 1*8]= s.current_picture.ref_index[list][b8_xy];
	                }else{
	                    //AV_ZERO32(this.mv_cache[list][scan8[0] - 1 - 1*8]);
	                	Arrays.fill(this.mv_cache[list][scan8[0] - 1 - 1*8], 0, 2, 0);
	                    this.ref_cache[list][scan8[0] - 1 - 1*8]= topleft_type!=0 ? LIST_NOT_USED : PART_NOT_AVAILABLE;
	                }
	            }

	            if(((mb_type&(MB_TYPE_SKIP|MB_TYPE_DIRECT2)))!=0 && 0 == mb_aff_frame)
	                continue;

	            if(0 ==(mb_type&(MB_TYPE_SKIP|MB_TYPE_DIRECT2))) {
	            	this.ref_cache[list][scan8[4 ]] =
	            		this.ref_cache[list][scan8[12]] = PART_NOT_AVAILABLE;
	            //AV_ZERO32(this.mv_cache [list][scan8[4 ]]);
	            //AV_ZERO32(this.mv_cache [list][scan8[12]]);
	            Arrays.fill(this.mv_cache [list][scan8[4 ]], 0, 2, 0);
	            Arrays.fill(this.mv_cache [list][scan8[12 ]], 0, 2, 0);

	            if( pps.cabac != 0 ) {
	                /* XXX beurk, Load mvd */
	                if(((top_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	                		){
	                    int b_xy= (int)this.mb2br_xy[top_xy];
	                    //AV_COPY64(this.mvd_cache[list][scan8[0] + 0 - 1*8], this.mvd_table[list][b_xy + 0]);
	                    // copy 4 uint_16
	                    for(int k=0;k<4;k++) {
	                    	this.mvd_cache[list][scan8[0] + 0 - 1*8 + k][0] = this.mvd_table[list][b_xy + 0 + k][0];
	                    	this.mvd_cache[list][scan8[0] + 0 - 1*8 + k][1] = this.mvd_table[list][b_xy + 0 + k][1];
	                    } // for
	                    
	                    // DebugTool.printDebugString(" ***** fill_mvd_caches: case 1.1\n");
	                }else{
	                    //AV_ZERO64(this.mvd_cache[list][scan8[0] + 0 - 1*8]);
	                    // fill 4 uint_16
	                    for(int k=0;k<4;k++) {
	                    	this.mvd_cache[list][scan8[0] + 0 - 1*8 + k][0] = 0;
	                    	this.mvd_cache[list][scan8[0] + 0 - 1*8 + k][1] = 0;
	                    } // for

	                    // DebugTool.printDebugString(" ***** fill_mvd_caches: case 1.2\n");
	                }
	                if(((left_type[0]) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	                	){
	                    int b_xy= (int)this.mb2br_xy[left_xy[0]] + 6;
	                    //AV_COPY16(this.mvd_cache[list][scan8[0] - 1 + 0*8], this.mvd_table[list][b_xy - left_block[0]]);
	                    //AV_COPY16(this.mvd_cache[list][scan8[0] - 1 + 1*8], this.mvd_table[list][b_xy - left_block[1]]);
	                    // copy 2 uint_16
	                    this.mvd_cache[list][scan8[0] - 1 + 0*8][0] = this.mvd_table[list][b_xy - left_block[0]][0];
	                    this.mvd_cache[list][scan8[0] - 1 + 0*8][1] = this.mvd_table[list][b_xy - left_block[0]][1];
	                    this.mvd_cache[list][scan8[0] - 1 + 1*8][0] = this.mvd_table[list][b_xy - left_block[1]][0];
	                    this.mvd_cache[list][scan8[0] - 1 + 1*8][1] = this.mvd_table[list][b_xy - left_block[1]][1];

	                    // DebugTool.printDebugString(" ***** fill_mvd_caches: case 2.1\n");

	                }else{
	                    //AV_ZERO16(this.mvd_cache [list][scan8[0] - 1 + 0*8]);
	                    //AV_ZERO16(this.mvd_cache [list][scan8[0] - 1 + 1*8]);
	                    this.mvd_cache[list][scan8[0] - 1 + 0*8][0] = 0;
	                    this.mvd_cache[list][scan8[0] - 1 + 0*8][1] = 0;
	                    this.mvd_cache[list][scan8[0] - 1 + 1*8][0] = 0;
	                    this.mvd_cache[list][scan8[0] - 1 + 1*8][1] = 0;
	                    
	                    // DebugTool.printDebugString(" ***** fill_mvd_caches: case 2.2\n");

	                }
	                if(((left_type[1]) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) != 0
	                		){
	                    int b_xy= (int)this.mb2br_xy[left_xy[1]] + 6;
	                    //AV_COPY16(this.mvd_cache[list][scan8[0] - 1 + 2*8], this.mvd_table[list][b_xy - left_block[2]]);
	                    //AV_COPY16(this.mvd_cache[list][scan8[0] - 1 + 3*8], this.mvd_table[list][b_xy - left_block[3]]);
	                    // copy 2 uint_16
	                    this.mvd_cache[list][scan8[0] - 1 + 2*8][0] = this.mvd_table[list][b_xy - left_block[2]][0];
	                    this.mvd_cache[list][scan8[0] - 1 + 2*8][1] = this.mvd_table[list][b_xy - left_block[2]][1];
	                    this.mvd_cache[list][scan8[0] - 1 + 3*8][0] = this.mvd_table[list][b_xy - left_block[3]][0];
	                    this.mvd_cache[list][scan8[0] - 1 + 3*8][1] = this.mvd_table[list][b_xy - left_block[3]][1];

	                    // DebugTool.printDebugString(" ***** fill_mvd_caches: case 3.1\n");

	                }else{
	                    //AV_ZERO16(this.mvd_cache [list][scan8[0] - 1 + 2*8]);
	                    //AV_ZERO16(this.mvd_cache [list][scan8[0] - 1 + 3*8]);
	                    this.mvd_cache[list][scan8[0] - 1 + 2*8][0] = 0;
	                    this.mvd_cache[list][scan8[0] - 1 + 2*8][1] = 0;
	                    this.mvd_cache[list][scan8[0] - 1 + 3*8][0] = 0;
	                    this.mvd_cache[list][scan8[0] - 1 + 3*8][1] = 0;

	                    // DebugTool.printDebugString(" ***** fill_mvd_caches: case 3.2\n");

	                }
	                //AV_ZERO16(this.mvd_cache [list][scan8[4 ]]);
	                //AV_ZERO16(this.mvd_cache [list][scan8[12]]);
	                this.mvd_cache[list][scan8[4 ]][0] = 0;
	                this.mvd_cache[list][scan8[4 ]][1] = 0;
	                this.mvd_cache[list][scan8[12 ]][0] = 0;
	                this.mvd_cache[list][scan8[12 ]][1] = 0;

	                if(this.slice_type_nos == FF_B_TYPE){
	                    Rectangle.fill_rectangle_unsign(this.direct_cache, scan8[0], 4, 4, 8, MB_TYPE_16x16>>1, 1);

	                    if((top_type & MB_TYPE_DIRECT2) != 0){
	                        //AV_WN32A(&this.direct_cache[scan8[0] - 1*8], 0x01010101u*(MB_TYPE_DIRECT2>>1));
	                        Arrays.fill(this.direct_cache, scan8[0] - 1*8, scan8[0] - 1*8 +4, (MB_TYPE_DIRECT2>>1));
	                    }else if((top_type & MB_TYPE_8x8) != 0){
	                        int b8_xy = 4*top_xy;
	                        this.direct_cache[scan8[0] + 0 - 1*8]= this.direct_table[b8_xy + 2];
	                        this.direct_cache[scan8[0] + 2 - 1*8]= this.direct_table[b8_xy + 3];
	                    }else{
	                        //AV_WN32A(&this.direct_cache[scan8[0] - 1*8], 0x01010101*(MB_TYPE_16x16>>1));
	                    	Arrays.fill(this.direct_cache, scan8[0] - 1*8, scan8[0] - 1*8 +4, (MB_TYPE_16x16>>1));
	                    }

	                    if((left_type[0] & MB_TYPE_DIRECT2) != 0)
	                        this.direct_cache[scan8[0] - 1 + 0*8]= MB_TYPE_DIRECT2>>1;
	                    else if((left_type[0] & MB_TYPE_8x8) != 0)
	                        this.direct_cache[scan8[0] - 1 + 0*8]= this.direct_table[4*left_xy[0] + 1 + (left_block[0]&~1)];
	                    else
	                        this.direct_cache[scan8[0] - 1 + 0*8]= MB_TYPE_16x16>>1;

	                    if((left_type[1] & MB_TYPE_DIRECT2) != 0)
	                        this.direct_cache[scan8[0] - 1 + 2*8]= MB_TYPE_DIRECT2>>1;
	                    else if((left_type[1] & MB_TYPE_8x8) != 0)
	                        this.direct_cache[scan8[0] - 1 + 2*8]= this.direct_table[4*left_xy[1] + 1 + (left_block[2]&~1)];
	                    else
	                        this.direct_cache[scan8[0] - 1 + 2*8]= MB_TYPE_16x16>>1;
	                }
	            }
	            }
	            if(mb_aff_frame != 0){
	                if(mb_field_decoding_flag != 0){
	                    //MAP_MVS
					    MAP_F2F_1(scan8[0] - 1 - 1*8, topleft_type, list);
					    MAP_F2F_1(scan8[0] + 0 - 1*8, top_type, list);
					    MAP_F2F_1(scan8[0] + 1 - 1*8, top_type, list);
					    MAP_F2F_1(scan8[0] + 2 - 1*8, top_type, list);
					    MAP_F2F_1(scan8[0] + 3 - 1*8, top_type, list);
					    MAP_F2F_1(scan8[0] + 4 - 1*8, topright_type, list);
					    MAP_F2F_1(scan8[0] - 1 + 0*8, left_type[0], list);
					    MAP_F2F_1(scan8[0] - 1 + 1*8, left_type[0], list);
					    MAP_F2F_1(scan8[0] - 1 + 2*8, left_type[1], list);
					    MAP_F2F_1(scan8[0] - 1 + 3*8, left_type[1], list);
	                }else{
	                    //MAP_MVS
					    MAP_F2F_2(scan8[0] - 1 - 1*8, topleft_type, list);
					    MAP_F2F_2(scan8[0] + 0 - 1*8, top_type, list);
					    MAP_F2F_2(scan8[0] + 1 - 1*8, top_type, list);
					    MAP_F2F_2(scan8[0] + 2 - 1*8, top_type, list);
					    MAP_F2F_2(scan8[0] + 3 - 1*8, top_type, list);
					    MAP_F2F_2(scan8[0] + 4 - 1*8, topright_type, list);
					    MAP_F2F_2(scan8[0] - 1 + 0*8, left_type[0], list);
					    MAP_F2F_2(scan8[0] - 1 + 1*8, left_type[0], list);
					    MAP_F2F_2(scan8[0] - 1 + 2*8, left_type[1], list);
					    MAP_F2F_2(scan8[0] - 1 + 3*8, left_type[1], list);
	                }
	            }
	        }
	    }

	    // DebugTool.printDebugString("top_type="+top_type+", left_type[0]="+left_type[0]+"\n");
	    
	    this.neighbor_transform_size= ((top_type & MB_TYPE_8x8DCT)>0?1:0) + ((left_type[0] & MB_TYPE_8x8DCT)>0?1:0);
	}
	
	public /*inline*/ void write_back_non_zero_count(){
	    int mb_xy= this.mb_xy;
	    /*
	    AV_COPY64(&h->non_zero_count[mb_xy][ 0], &h->non_zero_count_cache[0+8*1]);
	    AV_COPY64(&h->non_zero_count[mb_xy][ 8], &h->non_zero_count_cache[0+8*2]);
	    AV_COPY32(&h->non_zero_count[mb_xy][16], &h->non_zero_count_cache[0+8*5]);
	    AV_COPY32(&h->non_zero_count[mb_xy][20], &h->non_zero_count_cache[4+8*3]);
	    AV_COPY64(&h->non_zero_count[mb_xy][24], &h->non_zero_count_cache[0+8*4]);
		*/
        //// DebugTool.dumpDebugFrameData(this, "BEFORE-write_back_non_zero_count");
	    
	    System.arraycopy(non_zero_count_cache, 0+8*1, non_zero_count[mb_xy], 0, 8);
	    System.arraycopy(non_zero_count_cache, 0+8*2, non_zero_count[mb_xy], 8, 8);
	    System.arraycopy(non_zero_count_cache, 0+8*5, non_zero_count[mb_xy], 16, 4);
	    System.arraycopy(non_zero_count_cache, 4+8*3, non_zero_count[mb_xy], 20, 4);
	    System.arraycopy(non_zero_count_cache, 0+8*4, non_zero_count[mb_xy], 24, 8);	
	    
        //// DebugTool.dumpDebugFrameData(this, "AFTER-write_back_non_zero_count");

	}
		
	public /*inline*/ void write_back_motion(int mb_type) {
	    int b_xy = 4*s.mb_x + 4*s.mb_y*this.b_stride; //try mb2b(8)_xy
	    int b8_xy= 4*this.mb_xy;
	    int list;

	    if(0 == ((mb_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(0)) )) )
	        Rectangle.fill_rectangle_sign(s.current_picture.ref_index[0], b8_xy, 2, 2, 2, (int)LIST_NOT_USED, 1);

	    for(list=0; list<this.list_count; list++){
	        int y, b_stride;
	        //int16_t (*mv_dst)[2];
	        //int16_t (*mv_src)[2];
	        int[][] mv_dst_base;
	        int[][] mv_src_base;
	        int mv_dst_offset;
	        int mv_src_offset;

	        if(0 == ((mb_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) )
	            continue;

	        b_stride = this.b_stride;
	        mv_dst_base   = s.current_picture.motion_val_base[list];
	        mv_dst_offset = s.current_picture.motion_val_offset[list] + b_xy;
	        mv_src_base   = this.mv_cache[list];
	        mv_src_offset = scan8[0];
	        for(y=0; y<4; y++){
	            //AV_COPY128(mv_dst + y*b_stride, mv_src + 8*y);
	        	// copy 8 int16_t in mv_src_base
	        	for(int j=0;j<4;j++) {
	        		mv_dst_base[mv_dst_offset + y*b_stride +j][0] = mv_src_base[mv_src_offset + 8*y +j][0];
	        		mv_dst_base[mv_dst_offset + y*b_stride +j][1] = mv_src_base[mv_src_offset + 8*y +j][1];
	        	} // for j
	        }
	        if( 0 != pps.cabac ) {
	            // uint8_t (*mvd_dst)[2] = &this.mvd_table[list][FMO ? 8*this.mb_xy : this.mb2br_xy[this.mb_xy]];
	            // uint8_t (*mvd_src)[2] = &this.mvd_cache[list][scan8[0]];
	        	int[][] mvd_dst_base = this.mvd_table[list];
	        	int mvd_dst_offset = (int)this.mb2br_xy[this.mb_xy];
	            int[][] mvd_src_base = this.mvd_cache[list];
	            int mvd_src_offset = scan8[0];
	            if(0 != (mb_type & MB_TYPE_SKIP)) {
	                //AV_ZERO128(mvd_dst);
	            	// Fill 8 uint_16 with 0..
	            	for(int k=0;k<8;k++)
	            		Arrays.fill(mvd_dst_base[mvd_dst_offset + k], 0 , 2, 0);
	            } else {
	            	//AV_COPY64(mvd_dst, mvd_src + 8*3);
	                //AV_COPY16(mvd_dst + 3 + 3, mvd_src + 3 + 8*0);
	                //AV_COPY16(mvd_dst + 3 + 2, mvd_src + 3 + 8*1);
	                //AV_COPY16(mvd_dst + 3 + 1, mvd_src + 3 + 8*2);
	            	for(int k=0;k<4;k++)
	            		System.arraycopy(mvd_src_base[mvd_src_offset + 8*3 +k],0, mvd_dst_base[mvd_dst_offset +k], 0, 2);
	            	System.arraycopy(mvd_src_base[mvd_src_offset + 3 + 8*0],0, mvd_dst_base[mvd_dst_offset + 3 + 3],0, 2);
	            	System.arraycopy(mvd_src_base[mvd_src_offset + 3 + 8*1],0, mvd_dst_base[mvd_dst_offset + 3 + 2],0, 2);
	            	System.arraycopy(mvd_src_base[mvd_src_offset + 3 + 8*2],0, mvd_dst_base[mvd_dst_offset + 3 + 1],0, 2);
	            }
	        }

	        {
	            int[] ref_index = s.current_picture.ref_index[list];
	            ref_index[b8_xy + 0+0*2]= this.ref_cache[list][scan8[0]];
	            ref_index[b8_xy + 1+0*2]= this.ref_cache[list][scan8[4]];
	            ref_index[b8_xy + 0+1*2]= this.ref_cache[list][scan8[8]];
	            ref_index[b8_xy + 1+1*2]= this.ref_cache[list][scan8[12]];
	        }
	    }

	    if(this.slice_type_nos == FF_B_TYPE && pps.cabac != 0){
	        if(0 != (mb_type & MB_TYPE_8x8)){
	            this.direct_table[4*this.mb_xy + 1] = this.sub_mb_type[1]>>1;
	        	this.direct_table[4*this.mb_xy + 2] = this.sub_mb_type[2]>>1;
	            this.direct_table[4*this.mb_xy + 3] = this.sub_mb_type[3]>>1;
	        }
	    }
	}
	
	/**
	 * gets the predicted intra4x4 prediction mode.
	 */
	public /*inline*/ int pred_intra_mode(int n){
	    int index8= scan8[n];
	    int left= this.intra4x4_pred_mode_cache[index8 - 1];
	    int top = this.intra4x4_pred_mode_cache[index8 - 8];
	    int min= Math.min(left, top);

	    //tprintf(this.s.avctx, "mode:%d %d min:%d\n", left ,top, min);
	    if(min<0) return H264PredictionContext.DC_PRED;
	    else      return min;
	}	
	
	public void ff_h264_write_back_intra_pred_mode(){
	    //int8_t *mode= h->intra4x4_pred_mode + h->mb2br_xy[h->mb_xy];
		int[] mode_base = this.intra4x4_pred_mode;
		int mode_offset = (int)this.mb2br_xy[this.mb_xy];
	    //AV_COPY32(mode, this.intra4x4_pred_mode_cache + 4 + 8*4);
		System.arraycopy(this.intra4x4_pred_mode_cache, 4 + 8*4, mode_base, mode_offset, 4);
		mode_base[mode_offset + 4]= this.intra4x4_pred_mode_cache[7+8*3];
		mode_base[mode_offset + 5]= this.intra4x4_pred_mode_cache[7+8*2];
	    mode_base[mode_offset + 6]= this.intra4x4_pred_mode_cache[7+8*1];
	}	
	
	/**
	 * checks if the top & left blocks are available if needed & changes the dc mode so it only uses the available blocks.
	 */
	public int ff_h264_check_intra4x4_pred_mode() {
	    int[] top = new int[] {-1, 0,H264PredictionContext.LEFT_DC_PRED,-1,-1,-1,-1,-1, 0,0,0,0};
	    int[] left = new int[] { 0,-1, H264PredictionContext.TOP_DC_PRED, 0,-1,-1,-1, 0,-1,H264PredictionContext.DC_128_PRED,0,0};
	    int i;

	    if(0==(this.top_samples_available&0x08000)){
	        for(i=0; i<4; i++){
	            int status= top[ this.intra4x4_pred_mode_cache[scan8[0] + i] ];
	            if(status<0){
	                //av_log(this.s.avctx, AV_LOG_ERROR, "top block unavailable for requested intra4x4 mode %d at %d %d\n", status, s->mb_x, s->mb_y);
	                return -1;
	            } else if(status != 0){
	            	this.intra4x4_pred_mode_cache[scan8[0] + i]= status;
	            }
	        }
	    }

	    if((this.left_samples_available&0x08888)!=0x08888){
	        final int[] mask={0x08000,0x02000,0x080,0x020};
	        for(i=0; i<4; i++){
	            if(0==(this.left_samples_available&mask[i])){
	                int status= left[ this.intra4x4_pred_mode_cache[scan8[0] + 8*i] ];
	                if(status<0){
	                    //av_log(this.s.avctx, AV_LOG_ERROR, "left block unavailable for requested intra4x4 mode %d at %d %d\n", status, s->mb_x, s->mb_y);
	                    return -1;
	                } else if(status != 0){
	                	this.intra4x4_pred_mode_cache[scan8[0] + 8*i]= status;
	                }
	            }
	        }
	    }

	    return 0;
	} //FIXME cleanup like ff_h264_check_intra_pred_mode	
	
	/**
	 * checks if the top & left blocks are available if needed & changes the dc mode so it only uses the available blocks.
	 */
	public int ff_h264_check_intra_pred_mode(int mode){
	    int[] top = new int[] {H264PredictionContext.LEFT_DC_PRED8x8, 1,-1,-1,0,0,0};
	    int[] left = new int[] { H264PredictionContext.TOP_DC_PRED8x8,-1, 2,-1,H264PredictionContext.DC_128_PRED8x8,0,0};

	    if(mode > 6) {
	        //av_log(h->s.avctx, AV_LOG_ERROR, "out of range intra chroma pred mode at %d %d\n", s->mb_x, s->mb_y);
	        return -1;
	    }

	    if(0 == (this.top_samples_available&0x08000)){
	        mode= top[ mode ];
	        if(mode<0){
	            //av_log(h->s.avctx, AV_LOG_ERROR, "top block unavailable for requested intra mode at %d %d\n", s->mb_x, s->mb_y);
	            return -1;
	        }
	    }

	    if((this.left_samples_available&0x08080) != 0x08080){
	        mode= left[ mode ];
	        if(0 != (this.left_samples_available&0x08080)){ //mad cow disease mode, aka MBAFF + constrained_intra_pred
	            mode= H264PredictionContext.ALZHEIMER_DC_L0T_PRED8x8 + ((this.left_samples_available&0x08000)==0?1:0) + 2*(mode == H264PredictionContext.DC_128_PRED8x8?1:0);
	        }
	        if(mode<0){
	            //av_log(this.s.avctx, AV_LOG_ERROR, "left block unavailable for requested intra mode at %d %d\n", s->mb_x, s->mb_y);
	            return -1;
	        }
	    }

	    return mode;
	}	
	
//	static inline void backup_mb_border(H264Context *h, uint8_t *src_y, uint8_t *src_cb, uint8_t *src_cr, int linesize, int uvlinesize, int simple){
	public /*inline*/ void backup_mb_border(
			int[] src_y_base, int src_y_offset, 
			int[] src_cb_base, int src_cb_offset, 
			int[] src_cr_base, int src_cr_offset, 
			int linesize, int uvlinesize, int simple){
	    //uint8_t *top_border;
	    int[] top_border_base = null;
	    int top_border_offset = 0;

		int top_idx = 1;

	    src_y_offset  -=   linesize;
	    src_cb_offset -= uvlinesize;
	    src_cr_offset -= uvlinesize;

	    if(0==simple && 0!=mb_aff_frame){
	        if((s.mb_y&1)!=0){
	            if(0==mb_mbaff){
	                top_border_base = this.top_borders[0][s.mb_x];
	                top_border_offset = 0;
	                //AV_COPY128(top_border, src_y + 15*linesize);
	                System.arraycopy(src_y_base, src_y_offset + 15*linesize, top_border_base, top_border_offset, 16);
	                //if(0!=simple || 0==MpegEncContext.CONFIG_GRAY || 0==(s.flags&MpegEncContext.CODEC_FLAG_GRAY)){
	                    //AV_COPY64(top_border+16, src_cb+7*uvlinesize);
	                    //AV_COPY64(top_border+24, src_cr+7*uvlinesize);
		                System.arraycopy(src_cb_base, src_cb_offset + 7*uvlinesize, top_border_base, top_border_offset + 16, 8);
		                System.arraycopy(src_cr_base, src_cr_offset + 7*uvlinesize, top_border_base, top_border_offset + 24, 8);
	                //}
	            }
	        }else if(0!=mb_mbaff){
	            top_idx = 0;
	        }else
	            return;
	    }

	    top_border_base = this.top_borders[top_idx][s.mb_x];
	    top_border_offset = 0;
	    // There are two lines saved, the line above the the top macroblock of a pair,
	    // and the line above the bottom macroblock
	    //AV_COPY128(top_border, src_y + 16*linesize);
        System.arraycopy(src_y_base, src_y_offset + 16*linesize, top_border_base, top_border_offset, 16);

	    //if(0!=simple || 0==MpegEncContext.CONFIG_GRAY || 0==(s.flags&MpegEncContext.CODEC_FLAG_GRAY)){
	        //AV_COPY64(top_border+16, src_cb+8*uvlinesize);
	        //AV_COPY64(top_border+24, src_cr+8*uvlinesize);
            System.arraycopy(src_cb_base, src_cb_offset + 8*uvlinesize, top_border_base, top_border_offset + 16, 8);
            System.arraycopy(src_cr_base, src_cr_offset + 8*uvlinesize, top_border_base, top_border_offset + 24, 8);
	    //}
	}
	
	//public /*inline*/ void xchg_mb_border(uint8_t *src_y, uint8_t *src_cb, uint8_t *src_cr, int linesize, int uvlinesize, int xchg, int simple){
	public /*inline*/ void xchg_mb_border(
			int[] src_y_base, int src_y_offset, 
			int[] src_cb_base, int src_cb_offset, 
			int[] src_cr_base, int src_cr_offset, 
			int linesize, int uvlinesize, int xchg, int simple){
	    int deblock_left;
	    int deblock_top;
	    int top_idx = 1;
	    //uint8_t *top_border_m1;
	    //uint8_t *top_border;
	    int[] top_border_m1_base = null;
	    int[] top_border_base = null;
	    int top_border_m1_offset = 0;
	    int top_border_offset = 0;
	    int tmp;
	    
	    ////System.out.println("XChgBorder["+s.mb_x+","+s.mb_y+"]");

	    if(0==simple && 0!=mb_aff_frame){
	        if(0!=(s.mb_y&1)){
	            if(0==mb_mbaff)
	                return;
	        }else{
	            top_idx = (mb_mbaff!=0) ? 0 : 1;
	        }
	    }

	    if(this.deblocking_filter == 2) {
	        deblock_left = this.left_type[0];
	        deblock_top  = this.top_type;
	    } else {
	        deblock_left = (s.mb_x > 0)?1:0;
	        deblock_top =  (s.mb_y > (mb_field_decoding_flag!=0?1:0))?1:0;
	    }

	    src_y_offset  -=   linesize + 1;
	    src_cb_offset -= uvlinesize + 1;
	    src_cr_offset -= uvlinesize + 1;

	    if(s.mb_x <= 0)
	    	top_border_m1_base = null;
	    else
	    	top_border_m1_base = this.top_borders[top_idx][s.mb_x-1];
	    top_border_base    = this.top_borders[top_idx][s.mb_x];

	//#define XCHG(a,b,xchg)\
	//if (xchg) AV_SWAP64(b,a);\
	//else      AV_COPY64(b,a);

	    if(deblock_top!=0){
	        if(deblock_left!=0){
	        	for(int i=0;i<8;i++) {
		        	tmp = top_border_m1_base[top_border_m1_offset + 8 + i];
		        	top_border_m1_base[top_border_m1_offset + 8 + i] = src_y_base[src_y_offset - 7 + i];
		        	src_y_base[src_y_offset - 7 + i] = tmp;
	        	} // for i
	        }
	        
	        if(xchg!=0) {
	        	for(int i=0;i<8;i++) {
		        	tmp = top_border_base[top_border_offset + 0 + i];
		        	top_border_base[top_border_offset + 0 + i] = src_y_base[src_y_offset + 1 + i];
		        	src_y_base[src_y_offset + 1 + i] = tmp;
	        	} // for i	        	
	        } else {
	        	for(int i=0;i<8;i++) {
	        		top_border_base[top_border_offset + 0 + i] = src_y_base[src_y_offset + 1 + i];
	        	} // for i	        	
	        } // if

        	for(int i=0;i<8;i++) {
	        	tmp = top_border_base[top_border_offset + 8 + i];
	        	top_border_base[top_border_offset + 8 + i] = src_y_base[src_y_offset + 9 + i];
	        	src_y_base[src_y_offset + 9 + i] = tmp;
        	} // for i
	        
	        if(s.mb_x+1 < s.mb_width){
	  
	        	for(int i=0;i<8;i++) {
		        	tmp = this.top_borders[top_idx][s.mb_x+1][i];
		        	this.top_borders[top_idx][s.mb_x+1][i] = src_y_base[src_y_offset + 17 + i];
		        	src_y_base[src_y_offset + 17 + i] = tmp;
	        	} // for i	        	
	            //XCHG(this.top_borders[top_idx][s.mb_x+1], src_y +17, 1);
	        }
	    }

	    //if(simple !=0|| 0==MpegEncContext.CONFIG_GRAY || 0==(s.flags&MpegEncContext.CODEC_FLAG_GRAY)){
	        if(deblock_top!=0){
	            if(deblock_left!=0){
		        	for(int i=0;i<8;i++) {
			        	tmp = top_border_m1_base[top_border_m1_offset + 16 + i];
			        	top_border_m1_base[top_border_m1_offset + 16 + i] = src_cb_base[src_cb_offset + 1 + i];
			        	src_cb_base[src_cb_offset + 1 + i] = tmp;

			        	tmp = top_border_m1_base[top_border_m1_offset + 24 + i];
			        	top_border_m1_base[top_border_m1_offset + 24 + i] = src_cr_base[src_cr_offset + 1 + i];
			        	src_cr_base[src_cr_offset + 1 + i] = tmp;
		        	} // for i		        	
	            }
	            
	        	for(int i=0;i<8;i++) {
		        	tmp = top_border_base[top_border_offset + 16 + i];
		        	top_border_base[top_border_offset + 16 + i] = src_cb_base[src_cb_offset + 1 + i];
		        	src_cb_base[src_cb_offset + 1 + i] = tmp;

		        	tmp = top_border_base[top_border_offset + 24 + i];
		        	top_border_base[top_border_offset + 24 + i] = src_cr_base[src_cr_offset + 1 + i];
		        	src_cr_base[src_cr_offset + 1 + i] = tmp;
	        	} // for i
	        }
	    //}
	}	
	
	public /*av_always_inline*/ void hl_decode_mb_internal(int simple){
	    int mb_x= s.mb_x;
	    int mb_y= s.mb_y;
	    int mb_xy= this.mb_xy;
	    int mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
	    //uint8_t  *dest_y, *dest_cb, *dest_cr;
	    int[] dest_y_base, dest_cb_base, dest_cr_base;
	    int dest_y_offset, dest_cb_offset, dest_cr_offset;
	    int linesize, uvlinesize /*dct_offset*/;
	    int i;
	    //int *block_offset = &this.block_offset[0];
	    int[] block_offset_base = this.block_offset;
	    int block_offset_offset = 0;

	    boolean transform_bypass = ((0==simple) && (s.qscale == 0 && this.sps.transform_bypass!=0));
	    /* is_h264 should always be true if SVQ3 is disabled. */
	    int is_h264 = 1; //!CONFIG_SVQ3_DECODER || simple || s.codec_id == CODEC_ID_H264;
	    
	    // Change to use static coding in JAVA here.
	    // void (*idct_add)(uint8_t *dst, DCTELEM *block, int stride);
	    // void (*idct_dc_add)(uint8_t *dst, DCTELEM *block, int stride);

	    // DebugTool.printDebugString("*** BEFORE hl_decode_mb_internal: mb_x="+mb_x+", mb_y="+mb_y+", offset="+((mb_x + mb_y * s.linesize  ) * 16)+"\n");
	    // DebugTool.dumpFrameData(s.current_picture);
	    
	    dest_y_base  = s.current_picture.data_base[0];
	    dest_cb_base = s.current_picture.data_base[1];
	    dest_cr_base = s.current_picture.data_base[2];
	    dest_y_offset  = s.current_picture.data_offset[0] + (mb_x + mb_y * s.linesize  ) * 16;
	    dest_cb_offset = s.current_picture.data_offset[1] + (mb_x + mb_y * s.uvlinesize) * 8;
	    dest_cr_offset = s.current_picture.data_offset[2] + (mb_x + mb_y * s.uvlinesize) * 8;

	    // No prefetch optimiztion in JAVA
	    //s.dsp.prefetch(dest_y + (s.mb_x&3)*4*s.linesize + 64, s.linesize, 4);
	    //s.dsp.prefetch(dest_cb + (s.mb_x&7)*s.uvlinesize + 64, dest_cr - dest_cb, 2);

	    this.list_counts[mb_xy]= (int)this.list_count;

	    if (0==simple && 0!=mb_field_decoding_flag) {
	        linesize   = this.mb_linesize   = s.linesize * 2;
	        uvlinesize = this.mb_uvlinesize = s.uvlinesize * 2;
	        block_offset_base = this.block_offset;
	        block_offset_offset = 24;
	        if((mb_y&1)!=0){ //FIXME move out of this function?
	            dest_y_offset -= s.linesize*15;
	            dest_cb_offset-= s.uvlinesize*7;
	            dest_cr_offset-= s.uvlinesize*7;
	        }
	        if(0!=mb_aff_frame) {
	            int list;
	            for(list=0; list<this.list_count; list++){
	                if(0==((mb_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))))
	                    continue;
	                if(0!=(mb_type & MB_TYPE_16x16)){
	                    //int8_t *ref = &this.ref_cache[list][scan8[0]];
	                	int[] ref_base = this.ref_cache[list];
	                	int ref_offset = scan8[0];
	                    Rectangle.fill_rectangle_sign(ref_base, ref_offset, 4, 4, 8, (16+ref_base[ref_offset])^(s.mb_y&1), 1);
	                }else{
	                    for(i=0; i<16; i+=4){
	                        int ref = this.ref_cache[list][scan8[i]];
	                        if(ref >= 0)
	                            Rectangle.fill_rectangle_sign(this.ref_cache[list], scan8[i], 2, 2, 8, (16+ref)^(s.mb_y&1), 1);
	                    }
	                }
	            }
	        }
	    } else {
	        linesize   = this.mb_linesize   = s.linesize;
	        uvlinesize = this.mb_uvlinesize = s.uvlinesize;
//	        dct_offset = s.linesize * 16;
	    }

	    if (0==simple && 0!=(mb_type & MB_TYPE_INTRA_PCM)) {
	        for (i=0; i<16; i++) {
	            //memcpy(dest_y + i*  linesize, this.mb       + i*8, 16);
	        	// Copy 8 int_16t into 16 uint_8t
	        	for(int j=0;j<8;j++) {
	        		short val = this.mb[i*8];
	        		dest_y_base[dest_y_offset + i*  linesize + j*2] = val & 0x0ff;
	        		dest_y_base[dest_y_offset + i*  linesize + j*2 + 1] = (val & 0x0ff00)>>8;
	        	} // for j
	        }
	        for (i=0; i<8; i++) {
	            //memcpy(dest_cb+ i*uvlinesize, this.mb + 128 + i*4,  8);
	            //memcpy(dest_cr+ i*uvlinesize, this.mb + 160 + i*4,  8);
	        	// Copy 4 int_16t into 8 uint_8t
	        	for(int j=0;j<4;j++) {
	        		short val1 = this.mb[128 + i*4];
	        		short val2 = this.mb[160 + i*4];
	        		dest_cb_base[dest_cb_offset + i*  uvlinesize + j*2] = val1 & 0x0ff;
	        		dest_cb_base[dest_cb_offset + i*  uvlinesize + j*2 + 1] = (val1 & 0x0ff00)>>8;	        		
	        		dest_cr_base[dest_cr_offset + i*  uvlinesize + j*2] = val2 & 0x0ff;
	        		dest_cr_base[dest_cr_offset + i*  uvlinesize + j*2 + 1] = (val2 & 0x0ff00)>>8;	        		
	        	} // for j
	        }
	    } else {
	        if(0!=(mb_type & 7)){
	            if(0!=this.deblocking_filter)
	                xchg_mb_border(dest_y_base, dest_y_offset, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset, linesize, uvlinesize, 1, simple);
	            
	            //if(0!=simple || 0==MpegEncContext.CONFIG_GRAY || 0==(s.flags & MpegEncContext.CODEC_FLAG_GRAY)){
	                this.hpc.pred8x8[ this.chroma_pred_mode ].pred8x8(dest_cb_base, dest_cb_offset, uvlinesize);
	                this.hpc.pred8x8[ this.chroma_pred_mode ].pred8x8(dest_cr_base, dest_cr_offset, uvlinesize);
	            //}

	            if(0!=(mb_type & MB_TYPE_INTRA4x4)){
	                if(0!=simple || /*0==s.encoding*/ true ){ // Always decoding 
 	                    if(0!=(mb_type & MB_TYPE_8x8DCT)){
	                    	/*
	                        if(transform_bypass){
	                            idct_dc_add =
	                            idct_add    = s.dsp.add_pixels8;
	                        }else{
	                            idct_dc_add = this.h264dsp.h264_idct8_dc_add;
	                            idct_add    = this.h264dsp.h264_idct8_add;
	                        }
	                        */
	                        for(i=0; i<16; i+=4){
	                            //uint8_t * ptr= dest_y + block_offset[i];
	                        	int[] ptr_base= dest_y_base;
	                        	int ptr_offset = dest_y_offset + block_offset_base[block_offset_offset + i];
	                            int dir= this.intra4x4_pred_mode_cache[ scan8[i] ];
	                            if(transform_bypass && this.sps.profile_idc==244 && dir<=1){
	                                this.hpc.pred8x8l_add[dir].pred8x8l_add(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                            }else{
	                                int nnz = this.non_zero_count_cache[ scan8[i] ];
	                                this.hpc.pred8x8l[ dir ].pred8x8l(ptr_base, ptr_offset, (int)(this.topleft_samples_available<<i)&0x08000,
	                                                            (int)(this.topright_samples_available<<i)&0x04000, linesize);
	                                if(0!=nnz){
	                                    if(nnz == 1 && this.mb[i*16] != 0) {
	                                    	if(transform_bypass)
	                                    		s.dsp.add_pixels8(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                    	else
	                                    		this.h264dsp.h264_idct8_dc_add(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                    } else {
	                                    	if(transform_bypass)
	                                    		s.dsp.add_pixels8(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                    	else
	                                    		this.h264dsp.h264_idct8_add(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                    } // if
	                                }
	                            }
	                        }
	                    }else{
	                    	/*
	                        if(transform_bypass){
	                            idct_dc_add =
	                            idct_add    = s.dsp.add_pixels4;
	                        }else{
	                            idct_dc_add = this.h264dsp.h264_idct_dc_add;
	                            idct_add    = this.h264dsp.h264_idct_add;
	                        }
	                        */	                        
	                        for(i=0; i<16; i++){
	                            //uint8_t * ptr= dest_y + block_offset_base[block_offset_offset + i];
	                            int[] ptr_base = dest_y_base;
	                            int ptr_offset = dest_y_offset + block_offset_base[block_offset_offset + i];
	                            int dir= this.intra4x4_pred_mode_cache[ scan8[i] ];

	                            if(transform_bypass && this.sps.profile_idc==244 && dir<=1){
	                                this.hpc.pred4x4_add[dir].pred4x4_add(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                            }else{
	                                //uint8_t *topright;
	                            	int[] topright_base = null;
	                            	int topright_offset = 0;
	                                int nnz, tr;
	                                if(dir == H264PredictionContext.DIAG_DOWN_LEFT_PRED || dir == H264PredictionContext.VERT_LEFT_PRED){
	                                    int topright_avail= ((int)this.topright_samples_available<<i)&0x08000;
	                                    ////assert(mb_y || linesize <= block_offset[i]);
	                                    if(0==topright_avail){
	                                        tr= ptr_base[ptr_offset + 3 - linesize];//*0x01010101;
	                                        //topright= (uint8_t*) &tr;
	                                        topright_base = new int[] { tr, tr, tr, tr };
	                                        topright_offset = 0;
	                                    }else {
	                                        topright_base = ptr_base;
	                                    	topright_offset = ptr_offset + 4 - linesize;
	                                    } // if
	                                }else
	                                    topright_base = null;

	                                this.hpc.pred4x4[ dir ].pred4x4(ptr_base, ptr_offset, topright_base, topright_offset, linesize);
	                                nnz = this.non_zero_count_cache[ scan8[i] ];
	                                if(0!=nnz){
	                                    if(0!=is_h264){
	                                        if(nnz == 1 && 0!=this.mb[i*16]) {
	                                        	if(transform_bypass)
	                                        		s.dsp.add_pixels4(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                        	else
	                                        		this.h264dsp.h264_idct_dc_add(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                        } else {
	                                        	if(transform_bypass)
	                                        		s.dsp.add_pixels4(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                        	else 
	                                        		this.h264dsp.h264_idct_add(ptr_base, ptr_offset, this.mb, i*16, linesize);
	                                        } // if
	                                    }else {
	                                    	// No SVQ3
	                                        //ff_svq3_add_idct_c(ptr_base, ptr_offset, this.mb, i*16, linesize, s.qscale, 0);
	                                    } // if
	                                }
	                            }
	                        }
	                    }
	                }
	            }else{
	        	    // DebugTool.printDebugString("    **** intra16x16_pred_mode = "+this.intra16x16_pred_mode+"\n");

	                this.hpc.pred16x16[ this.intra16x16_pred_mode ].pred16x16(dest_y_base, dest_y_offset, linesize);

	                if(0!=is_h264){
	                    if(0!=this.non_zero_count_cache[ scan8[LUMA_DC_BLOCK_INDEX] ]){
	                        if(!transform_bypass)
	                            this.h264dsp.h264_luma_dc_dequant_idct(this.mb, 0, this.mb_luma_dc, 0, (int)this.dequant4_coeff[0][s.qscale][0]);
	                        else{
	                            //uint8_t dc_mapping[16] = { 0*16, 1*16, 4*16, 5*16, 2*16, 3*16, 6*16, 7*16,
	                            //                                        8*16, 9*16,12*16,13*16,10*16,11*16,14*16,15*16};
	                            final int[] dc_mapping = { 0*16, 1*16, 4*16, 5*16, 2*16, 3*16, 6*16, 7*16,
	                                                                    8*16, 9*16,12*16,13*16,10*16,11*16,14*16,15*16};
	                            for(i = 0; i < 16; i++)
	                                this.mb[dc_mapping[i]] = this.mb_luma_dc[i];
	                        }
	                    }
	                }else {
	                	// No SVQ3
	                    //ff_svq3_luma_dc_dequant_idct_c(this.mb, this.mb_luma_dc, s.qscale);
	                } // if
	            }
	            if(0!=this.deblocking_filter)
	            	xchg_mb_border(dest_y_base, dest_y_offset, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset, linesize, uvlinesize, 0, simple);

	        }else if(0!=is_h264){
	            hl_motion(dest_y_base, dest_y_offset, 
	            		  dest_cb_base, dest_cb_offset, 
	            		  dest_cr_base, dest_cr_offset,
	                      s.me.qpel_put, s.dsp.put_h264_chroma_pixels_tab,
	                      s.me.qpel_avg, s.dsp.avg_h264_chroma_pixels_tab,
	                      this.h264dsp.weight_h264_pixels_tab, this.h264dsp.biweight_h264_pixels_tab);
	      	            
	        }


	        if(0==(mb_type & MB_TYPE_INTRA4x4)){
	            if(0!=is_h264){
	                if(0!=(mb_type & MB_TYPE_INTRA16x16)){
	                    if(transform_bypass){
	                        if(this.sps.profile_idc==244 && (this.intra16x16_pred_mode==H264PredictionContext.VERT_PRED8x8 || this.intra16x16_pred_mode==H264PredictionContext.HOR_PRED8x8)){
	                            this.hpc.pred16x16_add[this.intra16x16_pred_mode].pred16x16_add(dest_y_base, dest_y_offset, block_offset_base, block_offset_offset, this.mb, 0, linesize);
	                        }else{
	                            for(i=0; i<16; i++){
	                                if(0!=this.non_zero_count_cache[ scan8[i] ] || 0!=this.mb[i*16])
	                                    s.dsp.add_pixels4(dest_y_base, dest_y_offset + block_offset_base[block_offset_offset + i], this.mb, i*16, linesize);
	                            }
	                        }
	                    }else{
	                         this.h264dsp.h264_idct_add16intra(dest_y_base, dest_y_offset, block_offset_base, block_offset_offset, this.mb, 0, linesize, this.non_zero_count_cache);

	                    }
	                }else if(0!=(this.cbp&15)){
	                    if(transform_bypass){
	                        int di = (mb_type & MB_TYPE_8x8DCT)!=0 ? 4 : 1;
	                        //idct_add= (mb_type & MB_TYPE_8x8DCT)!=0 ? s.dsp.add_pixels8 : s.dsp.add_pixels4;
	                        for(i=0; i<16; i+=di){
	                            if(0!=this.non_zero_count_cache[ scan8[i] ]){
	                            	if((mb_type & MB_TYPE_8x8DCT)!=0)
	                            		s.dsp.add_pixels8(dest_y_base, dest_y_offset + block_offset_base[block_offset_offset + i], this.mb, i*16, linesize);
	                            	else
	                            		s.dsp.add_pixels4(dest_y_base, dest_y_offset + block_offset_base[block_offset_offset + i], this.mb, i*16, linesize);
	                            }
	                        }
	                    }else{
	                        if(0!=(mb_type & MB_TYPE_8x8DCT)){
	                            this.h264dsp.h264_idct8_add4(dest_y_base, dest_y_offset, block_offset_base, block_offset_offset, this.mb, 0, linesize, this.non_zero_count_cache);
	                        }else{
	                            this.h264dsp.h264_idct_add16(dest_y_base, dest_y_offset, block_offset_base, block_offset_offset, this.mb, 0, linesize, this.non_zero_count_cache);	                            
	                        }
	                    }
	                }
	            }
	        }

	        if(/*(0!=simple || 0==MpegEncContext.CONFIG_GRAY || 0==(s.flags&MpegEncContext.CODEC_FLAG_GRAY)) &&*/
	        		0!=(this.cbp&0x030)){
	            //uint8_t *dest[2] = {dest_cb, dest_cr};
	        	int[][] dest_base = { dest_cb_base, dest_cr_base };
	        	int[] dest_offset = { dest_cb_offset, dest_cr_offset };
	            if(transform_bypass){
	                if((mb_type & 7) !=0 && this.sps.profile_idc==244 && (this.chroma_pred_mode==H264PredictionContext.VERT_PRED8x8 || this.chroma_pred_mode==H264PredictionContext.HOR_PRED8x8)){
	                    this.hpc.pred8x8_add[this.chroma_pred_mode].pred8x8_add(dest_base[0], dest_offset[0], block_offset_base, block_offset_offset + 16, this.mb, 16*16, uvlinesize);
	                    this.hpc.pred8x8_add[this.chroma_pred_mode].pred8x8_add(dest_base[1], dest_offset[1], block_offset_base, block_offset_offset + 20, this.mb, 20*16, uvlinesize);
	                }else{
	                    //idct_add = s.dsp.add_pixels4;
	                    for(i=16; i<16+8; i++){
	                        if(0!=this.non_zero_count_cache[ scan8[i] ] || 0!=this.mb[i*16])
	                        	s.dsp.add_pixels4(dest_base[(i&4)>>2], dest_offset[(i&4)>>2] + block_offset_base[block_offset_offset + i], this.mb, i*16, uvlinesize);
	                    }	                    
	                }
	            }else{
	                int chroma_qpu = (int)this.dequant4_coeff[(mb_type & 7)!=0 ? 1:4][this.chroma_qp[0]][0];
	                int chroma_qpv = (int)this.dequant4_coeff[(mb_type & 7)!=0 ? 2:5][this.chroma_qp[1]][0];
	                if(0!=is_h264){
	                    if(0!=this.non_zero_count_cache[ scan8[CHROMA_DC_BLOCK_INDEX+0] ])
	                        this.h264dsp.h264_chroma_dc_dequant_idct(this.mb, 16*16+0*16, this.mb_chroma_dc[0], 0, chroma_qpu );
	                    if(0!=this.non_zero_count_cache[ scan8[CHROMA_DC_BLOCK_INDEX+1] ])
	                        this.h264dsp.h264_chroma_dc_dequant_idct(this.mb, 16*16+4*16, this.mb_chroma_dc[1], 0, chroma_qpv );
	                    this.h264dsp.h264_idct_add8(dest_base, dest_offset, block_offset_base, block_offset_offset,
	                                              this.mb, 0, uvlinesize,
	                                              this.non_zero_count_cache);	                    
	                }else{
	                    this.h264dsp.h264_chroma_dc_dequant_idct(this.mb, 16*16+0*16, this.mb_chroma_dc[0], 0, chroma_qpu );
	                    this.h264dsp.h264_chroma_dc_dequant_idct(this.mb, 16*16+4*16, this.mb_chroma_dc[1], 0, chroma_qpv );
	                }
	            }
	        }
	    }
	    if(0!=this.cbp || 0!=(mb_type & 7))
	        s.dsp.clear_blocks(this.mb);
	    
	    // DebugTool.printDebugString("*** AFTER hl_decode_mb_internal\n");
	    // DebugTool.dumpFrameData(s.current_picture);

	}
	
	public void ff_h264_hl_decode_mb(){
	    int mb_xy= this.mb_xy;
	    int mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
	    boolean is_complex = /*CONFIG_SMALL || */(this.is_complex !=0)|| (mb_type & MB_TYPE_INTRA_PCM)!=0 || s.qscale == 0;

	    if (is_complex)
	    	hl_decode_mb_internal(0); //hl_decode_mb_complex();
	    else hl_decode_mb_internal(1); //hl_decode_mb_simple();
	}
	
	/**
	 * decodes a P_SKIP or B_SKIP macroblock
	 */
	public void decode_mb_skip() {
	    int mb_xy= this.mb_xy;
	    int mb_type=0;

	    Arrays.fill(non_zero_count[mb_xy], 0); // Fill 32 integers
	    Arrays.fill(non_zero_count_cache, 8, non_zero_count_cache.length, 0); // Fill 8*5 integers

	    // DebugTool.dumpDebugFrameData(this, "Reset non_zero_count_cache to 0s.", false);

	    if(mb_field_decoding_flag > 0)
	        mb_type|= MB_TYPE_INTERLACED;

	    if( slice_type_nos == FF_B_TYPE )
	    {
	        // just for fill_caches. pred_direct_motion will set the real mb_type
	        mb_type|= MB_TYPE_L0L1|MB_TYPE_DIRECT2|MB_TYPE_SKIP;
	        if(direct_spatial_mv_pred != 0){
	            fill_decode_neighbors(mb_type);
	        fill_decode_caches(mb_type); //FIXME check what is needed and what not ...
	        }
	        mb_type = ff_h264_pred_direct_motion(mb_type);
	        mb_type|= MB_TYPE_SKIP;
	    }
	    else
	    {
	        int mx = 0, my = 0;
	        mb_type|= MB_TYPE_16x16|MB_TYPE_P0L0|MB_TYPE_P1L0|MB_TYPE_SKIP;

	        fill_decode_neighbors(mb_type);
	        fill_decode_caches(mb_type); //FIXME check what is needed and what not ...
	        int[] mxmy = new int[] {mx,my};
	        pred_pskip_motion(mxmy);
	        mx = mxmy[0];
	        my = mxmy[1];
	        Rectangle.fill_rectangle_sign(ref_cache[0], scan8[0], 4, 4, 8, 0, 1);
	        Rectangle.fill_rectangle_mv_cache( mv_cache[0], scan8[0], 4, 4, 8, pack16to32(mx,my), 4);
	    }

	    write_back_motion(mb_type);
	    s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy] = mb_type;
	    s.current_picture.qscale_table[mb_xy] = s.qscale;
	    slice_table_base[this.slice_table_offset +  mb_xy ]= slice_num;
	    prev_mb_skipped= 1;
	}
		
/*
	#define DECODE_CABAC_MB_MVD( h,  list,  n )\
	{\
	    int amvd0 = this.mvd_cache[list][scan8[n] - 1][0] +
	                this.mvd_cache[list][scan8[n] - 8][0];
	    int amvd1 = this.mvd_cache[list][scan8[n] - 1][1] +
	                this.mvd_cache[list][scan8[n] - 8][1];
	\
	    mx += decode_cabac_mb_mvd( h, 40, amvd0, &mpx );
	    my += decode_cabac_mb_mvd( h, 47, amvd1, &mpy );
	}
*/
	public int get_cabac_cbf_ctx( int cat, int idx, int is_dc ) {
	    int nza, nzb;
	    int ctx = 0;

	    if( is_dc != 0 ) {
	        if( cat == 0 ) {
	            nza = left_cbp&0x0100;
	            nzb = top_cbp&0x0100;
	        } else {
	            idx -= CHROMA_DC_BLOCK_INDEX;
	            nza = (left_cbp>>(6+idx))&0x01;
	            nzb = (top_cbp>>(6+idx))&0x01;
	        }
	    } else {
	        //assert(cat == 1 || cat == 2 || cat == 4);
	        nza = non_zero_count_cache[scan8[idx] - 1];
	        nzb = non_zero_count_cache[scan8[idx] - 8];
	    }

	    if( nza > 0 )
	        ctx++;

	    if( nzb > 0 )
	        ctx += 2;

	    return ctx + 4 * cat;
	}

	public static final short[] last_coeff_flag_offset_8x8 = {
	    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
	    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
	    3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4,
	    5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8
	};

    static final int[][] significant_coeff_flag_offset = {
	      { 105+0, 105+15, 105+29, 105+44, 105+47, 402 },
	      { 277+0, 277+15, 277+29, 277+44, 277+47, 436 }
	    };
    static final int[][] last_coeff_flag_offset = {
      { 166+0, 166+15, 166+29, 166+44, 166+47, 417 },
      { 338+0, 338+15, 338+29, 338+44, 338+47, 451 }
    };
    static final int[] coeff_abs_level_m1_offset = {
        227+0, 227+10, 227+20, 227+30, 227+39, 426
    };
    static final short[][] significant_coeff_flag_offset_8x8 = {
      { 0, 1, 2, 3, 4, 5, 5, 4, 4, 3, 3, 4, 4, 4, 5, 5,
        4, 4, 4, 4, 3, 3, 6, 7, 7, 7, 8, 9,10, 9, 8, 7,
        7, 6,11,12,13,11, 6, 7, 8, 9,14,10, 9, 8, 6,11,
       12,13,11, 6, 9,14,10, 9,11,12,13,11,14,10,12 },
      { 0, 1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 7, 7, 8, 4, 5,
        6, 9,10,10, 8,11,12,11, 9, 9,10,10, 8,11,12,11,
        9, 9,10,10, 8,11,12,11, 9, 9,10,10, 8,13,13, 9,
        9,10,10, 8,13,13, 9, 9,10,10,14,14,14,14,14 }
    };
    /* node ctx: 0..3: abslevel1 (with abslevelgt1 == 0).
     * 4..7: abslevelgt1 + 3 (and abslevel1 doesn't matter).
     * map node ctx => cabac ctx for level=1 */
    static final short[] coeff_abs_level1_ctx = { 1, 2, 3, 4, 0, 0, 0, 0 };
    /* map node ctx => cabac ctx for level>1 */
    static final short[] coeff_abs_levelgt1_ctx = { 5, 5, 5, 5, 6, 7, 8, 9 };
    static final short[][] coeff_abs_level_transition = {
    /* update node ctx after decoding a level=1 */
        { 1, 2, 3, 3, 4, 5, 6, 7 },
    /* update node ctx after decoding a level>1 */
        { 4, 4, 4, 4, 5, 6, 7, 7 }
    };

    public /* inline */ int get_dct8x8_allowed(){
        if(0 != sps.direct_8x8_inference_flag) {
        	for(int i=0;i<4;i++) {
        		if((this.sub_mb_type[i] & ((MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_8x8))) != 0)
        			return 0;
        	} // for i
        	return 1;
        } else {
        	for(int i=0;i<4;i++) {
        		if((this.sub_mb_type[i] & ((MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_8x8|MB_TYPE_DIRECT2))) != 0)
        			return 0;
        	} // for i
        	return 1;
        } // if
    }
    
	public void decode_cabac_residual_internal( short[] block, int block_offset, int cat, int n, int[] scantable, int scan_offset, long[] qmul, int max_coeff, int is_dc ) {

	    int[] index = new int[64];

	    int last;
	    int coeff_count = 0;
	    int node_ctx = 0;

	    int significant_coeff_ctx_base_offset;
	    int last_coeff_ctx_base_offset;
	    int abs_level_m1_ctx_base_offset;

	    /*
	#define CC &h->cabac
*/
	    significant_coeff_ctx_base_offset = significant_coeff_flag_offset[mb_field_decoding_flag][cat];
	    last_coeff_ctx_base_offset = last_coeff_flag_offset[mb_field_decoding_flag][cat];
	    abs_level_m1_ctx_base_offset = coeff_abs_level_m1_offset[cat];

	    if( is_dc == 0 && cat == 5 ) {
	        
	        short[] sig_off = significant_coeff_flag_offset_8x8[mb_field_decoding_flag];
	        for(last= 0; last < 63; last++) { 
	            int sig_ctx_offset = significant_coeff_ctx_base_offset + sig_off[last]; 
	            if( cabac.get_cabac( cabac_state, sig_ctx_offset ) != 0 ) { 
	                int last_ctx_offset = last_coeff_ctx_base_offset + last_coeff_flag_offset_8x8[last]; 
	                index[coeff_count++] = last; 
	                if( cabac.get_cabac( cabac_state, last_ctx_offset ) != 0 ) { 
	                    last= max_coeff; 
	                    break; 
	                } 
	            } 
	        }
	        if( last == max_coeff -1 ) {
	            index[coeff_count++] = last;
	        }

	    } else {
	        for(last= 0; last < max_coeff - 1; last++) { 
	            int sig_ctx_offset = significant_coeff_ctx_base_offset + last; 
	            if( cabac.get_cabac( cabac_state, sig_ctx_offset ) != 0 ) { 
	                int last_ctx_offset = last_coeff_ctx_base_offset + last; 
	                index[coeff_count++] = last; 
	                if( cabac.get_cabac( cabac_state, last_ctx_offset ) != 0 ) { 
	                    last= max_coeff; 
	                    break; 
	                } 
	            } 
	        }
	        if( last == max_coeff -1 ) {
	            index[coeff_count++] = last;
	        }
	    
	    }
	    //assert(coeff_count > 0);

	    if( is_dc != 0) {
	        if( cat == 0 )
	            cbp_table[mb_xy] |= 0x0100;
	        else
	            cbp_table[mb_xy] |= 0x040 << (n - CHROMA_DC_BLOCK_INDEX);
	        non_zero_count_cache[scan8[n]] = coeff_count;
	    } else {
	        if( cat == 5 )
	        	Rectangle.fill_rectangle_unsign(non_zero_count_cache, scan8[n], 2, 2, 8, coeff_count, 1);
	        else {
	            //assert( cat == 1 || cat == 2 || cat == 4 );
	            non_zero_count_cache[scan8[n]] = coeff_count;
	        }
	    }

	    do {
	        int ctx_offset = coeff_abs_level1_ctx[node_ctx] + abs_level_m1_ctx_base_offset;

	        int j= scantable[scan_offset + index[--coeff_count]];

	        if( cabac.get_cabac( cabac_state, ctx_offset ) == 0 ) {
	            node_ctx = coeff_abs_level_transition[0][node_ctx];
	            if( is_dc != 0 ) {
	                block[block_offset +j] = (short)cabac.get_cabac_bypass_sign(-1);
	            }else{
	                block[block_offset +j] = (short)((cabac.get_cabac_bypass_sign( (int)-qmul[j]) + 32) >> 6);
	            }
	        } else {
	            int coeff_abs = 2;
	            ctx_offset = coeff_abs_levelgt1_ctx[node_ctx] + abs_level_m1_ctx_base_offset;
	            node_ctx = coeff_abs_level_transition[1][node_ctx];

	            while( coeff_abs < 15 && (cabac.get_cabac(cabac_state, ctx_offset ) != 0 ) ) {
	                coeff_abs++;
	            }

	            if( coeff_abs >= 15 ) {
	                int k = 0;
	                while( cabac.get_cabac_bypass() != 0) {
	                    k++;
	                }

	                coeff_abs=1;
	                while( k-- != 0) {
	                    coeff_abs += coeff_abs + cabac.get_cabac_bypass();
	                }
	                coeff_abs+= 14;
	            }

	            if( is_dc != 0 ) {
	                block[block_offset +j] = (short)cabac.get_cabac_bypass_sign( -coeff_abs );
	            }else{
	                block[block_offset +j] = (short)((cabac.get_cabac_bypass_sign((int)-coeff_abs ) * qmul[j] + 32) >> 6);
	            }
	        }
	    } while( coeff_count != 0);

	}

	public void decode_cabac_residual_dc_internal( short[] block, int block_offset, int cat, int n, int[] scantable, int scan_offset, int max_coeff ) {
	    decode_cabac_residual_internal(block, block_offset, cat, n, scantable, scan_offset, null, max_coeff, 1);
	}

	public void decode_cabac_residual_nondc_internal( short[] block, int block_offset, int cat, int n, int[] scantable, int scan_offset, long[] qmul, int max_coeff ) {
	    decode_cabac_residual_internal(block, block_offset, cat, n, scantable, scan_offset, qmul, max_coeff, 0);
	}

	/* cat: 0-> DC 16x16  n = 0
	 *      1-> AC 16x16  n = luma4x4idx
	 *      2-> Luma4x4   n = luma4x4idx
	 *      3-> DC Chroma n = iCbCr
	 *      4-> AC Chroma n = 16 + 4 * iCbCr + chroma4x4idx
	 *      5-> Luma8x8   n = 4 * luma8x8idx */

	/* Partially inline the CABAC residual decode: inline the coded block flag.
	 * This has very little impact on binary size and improves performance
	 * because it allows improved constant propagation into get_cabac_cbf_ctx,
	 * as well as because most blocks have zero CBFs. */

	public void decode_cabac_residual_dc( short[] block, int block_offset, int cat, int n, int[] scantable, int scan_offset, int max_coeff ) {
	    /* read coded block flag */
	    if( cabac.get_cabac( cabac_state, 85 + get_cabac_cbf_ctx( cat, n, 1 ) ) == 0 ) {
	        non_zero_count_cache[scan8[n]] = 0;
	        return;
	    }
	    decode_cabac_residual_dc_internal( block, block_offset, cat, n, scantable, scan_offset, max_coeff );
	}

	public void decode_cabac_residual_nondc(short[] block, int block_offset, int cat, int n, int[] scantable, int scan_offset, long[] qmul, int max_coeff ) {
	    /* read coded block flag */
	    if( cat != 5 && cabac.get_cabac( cabac_state, 85 + get_cabac_cbf_ctx( cat, n, 0 )  ) == 0 ) {
	        non_zero_count_cache[scan8[n]] = 0;
	        return;
	    }
	    decode_cabac_residual_nondc_internal(block, block_offset, cat, n, scantable, scan_offset, qmul, max_coeff );
	}

	/**
	 * decodes a macroblock
	 * @return 0 if OK, AC_ERROR / DC_ERROR / MV_ERROR if an error is noticed
	 */
	public int ff_h264_decode_mb_cabac() {
	    int mb_xy;
	    int mb_type, partition_count=0, cbp = 0;
	    int dct8x8_allowed= pps.transform_8x8_mode;

        // DebugTool.dumpDebugFrameData(this, "BEFORE-ff_h264_decode_mb_cabac");

	    mb_xy = this.mb_xy = s.mb_x + s.mb_y*s.mb_stride;

	    // DebugTool.printDebugString("pic:"+frame_num+" mb:"+s.mb_x+"/"+s.mb_y+", slice_type_nos="+slice_type_nos+"\n");
	    
	    if( slice_type_nos != FF_I_TYPE ) {
	        int skip;
	        /* a skipped mb needs the aff flag from the following mb */
	        if( mb_mbaff != 0 && (s.mb_y&1)==1 && prev_mb_skipped != 0)
	            skip = next_mb_skipped;
	        else
	            skip = cabac.decode_cabac_mb_skip( this, s.mb_x, s.mb_y );
	        /* read skip flags */
	        if( skip != 0) {
	            if( mb_mbaff != 0 && (s.mb_y&1)==0 ){
	                s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy] = MB_TYPE_SKIP;
	                next_mb_skipped = cabac.decode_cabac_mb_skip( this, s.mb_x, s.mb_y+1 );
	                if(0 == next_mb_skipped)
	                    mb_mbaff = mb_field_decoding_flag = cabac.decode_cabac_field_decoding_flag(this);
	            }

	            decode_mb_skip();

	            cbp_table[mb_xy] = 0;
	            chroma_pred_mode_table[mb_xy] = 0;
	            last_qscale_diff = 0;

	            return 0;

	        }
	    }
	    if(mb_mbaff != 0){
	        if( (s.mb_y&1) == 0 )
	            mb_mbaff =
	            mb_field_decoding_flag = cabac.decode_cabac_field_decoding_flag(this);
	    }

	    prev_mb_skipped = 0;

	    fill_decode_neighbors(-(mb_field_decoding_flag));

	    if( slice_type_nos == FF_B_TYPE ) {
	        int ctx = 0;
	        ////assert(h->slice_type_nos == FF_B_TYPE);

	        if( 0 == ( ( left_type[0]-1 ) & MB_TYPE_DIRECT2 ) )
	            ctx++;
	        if( 0 == ( ( top_type-1 )  & MB_TYPE_DIRECT2 ) )
	            ctx++;

	        boolean goto_decode_intra_mb = false;
	        if( 0 == cabac.get_cabac_noinline( cabac_state, 27+ctx ) ){
	            mb_type= 0; /* B_Direct_16x16 */
	        }else if( 0 == cabac.get_cabac_noinline( cabac_state, 27+3 ) ) {
	            mb_type= 1 + cabac.get_cabac_noinline( cabac_state, 27+5 ); /* B_L[01]_16x16 */
	        }else{
	            int bits;
	            bits = cabac.get_cabac_noinline( cabac_state, 27+4 ) << 3;
	            bits+= cabac.get_cabac_noinline( cabac_state, 27+5 ) << 2;
	            bits+= cabac.get_cabac_noinline( cabac_state, 27+5 ) << 1;
	            bits+= cabac.get_cabac_noinline( cabac_state, 27+5 );
	            if( bits < 8 ){
	                mb_type= bits + 3; /* B_Bi_16x16 through B_L1_L0_16x8 */
	            }else if( bits == 13 ){
	                mb_type= cabac.decode_cabac_intra_mb_type(this, 32, 0);
	                goto_decode_intra_mb = true;
//	                goto decode_intra_mb;
//	            	decode_intra_mb:
	    	        partition_count = 0;
	    	        cbp= H264Data.i_mb_type_info[mb_type].cbp;
	    	        intra16x16_pred_mode= H264Data.i_mb_type_info[mb_type].pred_mode;
	    	        mb_type= H264Data.i_mb_type_info[mb_type].type;
	            }else if( bits == 14 ){
	                mb_type= 11; /* B_L1_L0_8x16 */
	            }else if( bits == 15 ){
	                mb_type= 22; /* B_8x8 */
	            }else{
	                bits= ( bits<<1 ) + cabac.get_cabac_noinline( cabac_state, 27+5 );
	                mb_type= bits - 4; /* B_L0_Bi_* through B_Bi_Bi_* */
	            }
	        }
	        if(!goto_decode_intra_mb) {
	            partition_count= H264Data.b_mb_type_info[mb_type].partition_count;
	            mb_type=         H264Data.b_mb_type_info[mb_type].type;
	        } // if
	    } else if( slice_type_nos == FF_P_TYPE ) {
	        if( cabac.get_cabac_noinline( cabac_state, 14 ) == 0 ) {
	            /* P-type */
	            if( cabac.get_cabac_noinline( cabac_state, 15 ) == 0 ) {
	                /* P_L0_D16x16, P_8x8 */
	                mb_type= 3 * cabac.get_cabac_noinline( cabac_state, 16 );
	            } else {
	                /* P_L0_D8x16, P_L0_D16x8 */
	                mb_type= 2 - cabac.get_cabac_noinline( cabac_state, 17 );
	            }
	            partition_count= H264Data.p_mb_type_info[mb_type].partition_count;
	            mb_type=         H264Data.p_mb_type_info[mb_type].type;
	        } else {
	            mb_type= cabac.decode_cabac_intra_mb_type(this, 17, 0);
//	            goto decode_intra_mb;
//	        	decode_intra_mb:
		        partition_count = 0;
		        cbp= H264Data.i_mb_type_info[mb_type].cbp;
		        intra16x16_pred_mode= H264Data.i_mb_type_info[mb_type].pred_mode;
		        mb_type= H264Data.i_mb_type_info[mb_type].type;
	        }
	    } else {
	        mb_type= cabac.decode_cabac_intra_mb_type(this, 3, 1);
	        if(slice_type == FF_SI_TYPE && mb_type != 0)
	            mb_type--;
	        ////assert(h->slice_type_nos == FF_I_TYPE);
//	decode_intra_mb:
	        partition_count = 0;
	        cbp= H264Data.i_mb_type_info[mb_type].cbp;
	        intra16x16_pred_mode= H264Data.i_mb_type_info[mb_type].pred_mode;
	        mb_type= H264Data.i_mb_type_info[mb_type].type;
	    }
	    if(mb_field_decoding_flag != 0)
	        mb_type |= MB_TYPE_INTERLACED;

	    slice_table_base[this.slice_table_offset +  mb_xy ]= slice_num;

	    if((mb_type & MB_TYPE_INTRA_PCM) != 0) {
	        int ptr_offset = cabac.bytestream_current;
	        int[] ptr = cabac.bytestream;

	        // We assume these blocks are very rare so we do not optimize it.
	        // FIXME The two following lines get the bitstream position in the cabac
	        // decode, I think it should be done by a function in cabac.h (or cabac.c).
	        if((cabac.low&0x01) != 0) ptr_offset--;
	        if(CABACContext.CABAC_BITS==16){
	            if((cabac.low&0x01FF) != 0) ptr_offset--;
	        }

	        // The pixels are stored in the same order as levels in h->mb array.
	        for(int i=0;i<128;i++) {
	        	mb[i] = (short)(((ptr[ptr_offset+1] & 0x0ff) << 8) | (ptr[ptr_offset] & 0x0ff));
	        	ptr_offset += 2;
	        } // for i
	        if(sps.chroma_format_idc != 0){
		        for(int i=0;i<64;i++) {
		        	mb[128 + i] = (short)(((ptr[ptr_offset+1] & 0x0ff) << 8) | (ptr[ptr_offset] & 0x0ff));
		        	ptr_offset += 2;
		        } // for i
	        }

	        cabac.ff_init_cabac_decoder(ptr, ptr_offset, cabac.bytestream_end - ptr_offset);

	        // All blocks are present
	        cbp_table[mb_xy] = 0x01ef;
	        chroma_pred_mode_table[mb_xy] = 0;
	        // In deblocking, the quantizer is 0
	        s.current_picture.qscale_table[mb_xy]= 0;
	        // All coeffs are present
	        Arrays.fill(non_zero_count[mb_xy], 16); // 32 bytes fill
	        //memset(h->non_zero_count[mb_xy], 16, 32);
	        s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]= mb_type;
	        last_qscale_diff = 0;
	        return 0;
	    }

	    if(mb_mbaff != 0){
	        ref_count[0] <<= 1;
	        ref_count[1] <<= 1;
	    }

	    fill_decode_caches(mb_type);

	    if( (mb_type & 7) != 0 ) {
	        int i, pred_mode;
	        if( (mb_type & MB_TYPE_INTRA4x4) != 0) {

	        	// DebugTool.printDebugString("** MB_TYPE_INTRA4x4: neighbor_transform_size="+neighbor_transform_size+", dct8x8_allowed="+dct8x8_allowed+"\n");
	            
	        	if( dct8x8_allowed != 0 && ( cabac.get_cabac_noinline( cabac_state, 399 + neighbor_transform_size ) != 0 ) ) {
	                mb_type |= MB_TYPE_8x8DCT;
	                for( i = 0; i < 16; i+=4 ) {
	                    int pred = pred_intra_mode( i );
	                    int mode = cabac.decode_cabac_mb_intra4x4_pred_mode( this, pred );
	                    Rectangle.fill_rectangle_sign( intra4x4_pred_mode_cache, scan8[i], 2, 2, 8, mode, 1 );
	                }
	            } else {
	                for( i = 0; i < 16; i++ ) {
	                    int pred = pred_intra_mode( i );
	                    intra4x4_pred_mode_cache[ scan8[i] ] = (byte)cabac.decode_cabac_mb_intra4x4_pred_mode( this, pred );

	                //av_log( s.avctx, AV_LOG_ERROR, "i4x4 pred=%d mode=%d\n", pred, h->intra4x4_pred_mode_cache[ scan8[i] ] );
	                }
	            }
	            ff_h264_write_back_intra_pred_mode();
	            if( ff_h264_check_intra4x4_pred_mode() < 0 ) return -1;
	        } else {
	            intra16x16_pred_mode= ff_h264_check_intra_pred_mode( intra16x16_pred_mode );
	            if( intra16x16_pred_mode < 0 ) return -1;
	        }
	        if(sps.chroma_format_idc != 0){
	            chroma_pred_mode_table[mb_xy] =
	            pred_mode                        = cabac.decode_cabac_mb_chroma_pre_mode(this);

	            pred_mode= ff_h264_check_intra_pred_mode( pred_mode );
	            if( pred_mode < 0 ) return -1;
	            chroma_pred_mode= pred_mode;
	        }
	    } else if( partition_count == 4 ) {
	        int i, j; 
	        int[] sub_partition_count = new int[4];
	        int list;
	        int[][] ref = new int[2][4];

	        if( slice_type_nos == FF_B_TYPE ) {
	            for( i = 0; i < 4; i++ ) {
	                sub_mb_type[i] = cabac.decode_cabac_b_mb_sub_type(this);
	                sub_partition_count[i]= H264Data.b_sub_mb_type_info[ sub_mb_type[i] ].partition_count;
	                sub_mb_type[i]=      H264Data.b_sub_mb_type_info[ sub_mb_type[i] ].type;
	            }
	            if( ((sub_mb_type[0] | sub_mb_type[1] |
	                          sub_mb_type[2] | sub_mb_type[3]) & MB_TYPE_DIRECT2) != 0 ) {
	                mb_type = ff_h264_pred_direct_motion(mb_type);
	                ref_cache[0][scan8[4]] =
	                ref_cache[1][scan8[4]] =
	                ref_cache[0][scan8[12]] =
	                ref_cache[1][scan8[12]] = PART_NOT_AVAILABLE;
	                    for( i = 0; i < 4; i++ )
	                    	Rectangle.fill_rectangle_unsign( direct_cache, scan8[4*i], 2, 2, 8, (sub_mb_type[i]>>1)&0x0FF, 1 );
	            }
	        } else {
	            for( i = 0; i < 4; i++ ) {
	                sub_mb_type[i] = cabac.decode_cabac_p_mb_sub_type(this);
	                sub_partition_count[i]= H264Data.p_sub_mb_type_info[ sub_mb_type[i] ].partition_count;
	                sub_mb_type[i]=      H264Data.p_sub_mb_type_info[ sub_mb_type[i] ].type;
	            }
	        }

	        for( list = 0; list < list_count; list++ ) {
	                for( i = 0; i < 4; i++ ) {
	                    if((sub_mb_type[i] & MB_TYPE_DIRECT2) != 0) continue;
	                    if(((sub_mb_type[i]) & (MB_TYPE_P0L0<<((0)+2*(list)) ) ) !=0) {
	                        if( ref_count[list] > 1 ){
	                            ref[list][i] = cabac.decode_cabac_mb_ref( this, list, 4*i );
	                            if(ref[list][i] >= (int)ref_count[list]){
	                               // av_log(s.avctx, AV_LOG_ERROR, "Reference %d >= %d\n", ref[list][i], h->ref_count[list]);
	                                return -1;
	                            }
	                        }else
	                            ref[list][i] = 0;
	                    } else {
	                        ref[list][i] = -1;
	                    }
	                                                       ref_cache[list][ scan8[4*i]+1 ]=
	                    ref_cache[list][ scan8[4*i]+8 ]=ref_cache[list][ scan8[4*i]+9 ]= ref[list][i];
	                }
	        }

	        if(dct8x8_allowed != 0)
	            dct8x8_allowed = get_dct8x8_allowed();

	        for(list=0; list<list_count; list++){
	            for(i=0; i<4; i++){
	                ref_cache[list][ scan8[4*i]   ]=ref_cache[list][ scan8[4*i]+1 ];
	                if((sub_mb_type[i] &MB_TYPE_DIRECT2) != 0){
	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE1\n");
	                	Rectangle.fill_rectangle_mvd_cache(mvd_cache[list], scan8[4*i], 2, 2, 8, 0, 2);
	                    continue;
	                }

	                if( ((sub_mb_type[i]) & (MB_TYPE_P0L0<<((0)+2*(list)))) != 0 
	                		&& ((sub_mb_type[i]&MB_TYPE_DIRECT2) == 0)){
	                    int sub_mb_type= this.sub_mb_type[i];
	                    int block_width= (sub_mb_type & (MB_TYPE_16x16|MB_TYPE_16x8))!=0 ? 2 : 1;
	                    for(j=0; j<sub_partition_count[i]; j++){
	                        int mpx=0, mpy=0;
	                        int mx=0, my=0;
	                        int index= 4*i + block_width*j;
	                        int[][] mv_cache_base= this.mv_cache[list];
	                        int mv_cache_offset = scan8[index];
	                        int[][] mvd_cache_base= this.mvd_cache[list];
	                        int mvd_cache_offset = scan8[index];
	                        
	                        int[] args = new int[] { mx, my };
	                        pred_motion(index, block_width, list, ref_cache[list][ scan8[index] ], args);
	                        mx = args[0]; my = args[1];	                        
	                        {
	                            int amvd0 = this.mvd_cache[list][scan8[index] - 1][0] +
	                            			this.mvd_cache[list][scan8[index] - 8][0];
	                            int amvd1 = this.mvd_cache[list][scan8[index] - 1][1] +
	                            			this.mvd_cache[list][scan8[index] - 8][1];

	                            // DebugTool.dumpDebugFrameData(this, " -- BEFORE-decode_cabac_mb_mvd", false);
	                            // DebugTool.printDebugString("  -- ff_h264_decode_mb_cabac: CASE1: amvd0="+amvd0+", amvd1="+amvd1+", index="+index+"\n");
	                            args[0] = mpx;
	                            mx += cabac.decode_cabac_mb_mvd( this, 40, amvd0, args );
	                            mpx = args[0];
	                            args[0] = mpy;
	                            my += cabac.decode_cabac_mb_mvd( this, 47, amvd1, args );
	                            mpy = args[0];
	                        }
         
	                        //tprintf(s.avctx, "final mv:%d %d\n", mx, my);

	                        if(((sub_mb_type)&MB_TYPE_16x16)!=0) {
	                            mv_cache_base[mv_cache_offset + 1 ][0]=
	                            mv_cache_base[mv_cache_offset + 8 ][0]= mv_cache_base[mv_cache_offset + 9 ][0]= mx;
	                            mv_cache_base[mv_cache_offset + 1 ][1]=
	                            mv_cache_base[mv_cache_offset + 8 ][1]= mv_cache_base[mv_cache_offset + 9 ][1]= my;

	    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE2\n");

	                            mvd_cache_base[mvd_cache_offset + 1 ][0]=
	                            mvd_cache_base[mvd_cache_offset + 8 ][0]= mvd_cache_base[mvd_cache_offset + 9 ][0]= mpx;
	                            mvd_cache_base[mvd_cache_offset + 1 ][1]=
	                            mvd_cache_base[mvd_cache_offset + 8 ][1]= mvd_cache_base[mvd_cache_offset + 9 ][1]= mpy;
	                        }else if(((sub_mb_type)&MB_TYPE_16x8)!=0) {
	                        	mv_cache_base[mv_cache_offset + 1 ][0]= mx;
	                        	mv_cache_base[mv_cache_offset + 1 ][1]= my;

	    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE3\n");
	    	                	
	                        	mvd_cache_base[mvd_cache_offset + 1 ][0]=  mpx;
	                        	mvd_cache_base[mvd_cache_offset + 1 ][1]= mpy;
	                        }else if(((sub_mb_type)&MB_TYPE_8x16)!=0) {
	                        	
	                        	mv_cache_base[mv_cache_offset + 8 ][0]= mx;
	                        	mv_cache_base[mv_cache_offset + 8 ][1]= my;

	    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE4\n");

	                        	mvd_cache_base[mvd_cache_offset + 8 ][0]= mpx;
	                        	mvd_cache_base[mvd_cache_offset + 8 ][1]= mpy;
	                        }
	                        mv_cache_base[mv_cache_offset + 0 ][0]= mx;
	                        mv_cache_base[mv_cache_offset + 0 ][1]= my;

    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE5\n");

	                        mvd_cache_base[mvd_cache_offset + 0 ][0]= mpx;
	                        mvd_cache_base[mvd_cache_offset + 0 ][1]= mpy;
	                    }
	                }else{
	                	Rectangle.fill_rectangle_mv_cache(mv_cache [list], scan8[4*i] , 2, 2, 8, 0, 4);
	                	
	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE6\n");

	                	Rectangle.fill_rectangle_mvd_cache(mvd_cache[list], scan8[4*i], 2, 2, 8, 0, 2);
	                }
	            }
	        }
	    } else if( (mb_type&MB_TYPE_DIRECT2) != 0) {
	        mb_type = ff_h264_pred_direct_motion(mb_type);
	        
        	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE7\n");

	        Rectangle.fill_rectangle_mvd_cache(mvd_cache[0], scan8[0], 4, 4, 8, 0, 2);
	        Rectangle.fill_rectangle_mvd_cache(mvd_cache[1], scan8[0], 4, 4, 8, 0, 2);
	        dct8x8_allowed &= sps.direct_8x8_inference_flag;
	    } else {
	        int list, i;
	        if((mb_type&MB_TYPE_16x16) != 0){
	            for(list=0; list<list_count; list++){
	            	if(((mb_type) & (MB_TYPE_P0L0<<((0)+2*(list)))) != 0) {
	                    int ref;
	                    if(ref_count[list] > 1){
	                        ref= cabac.decode_cabac_mb_ref(this, list, 0);
	                        if(ref >= (int)ref_count[list]){
	                            //av_log(s.avctx, AV_LOG_ERROR, "Reference %d >= %d\n", ref, h->ref_count[list]);
	                            return -1;
	                        }
	                    }else
	                        ref=0;
	                    Rectangle.fill_rectangle_sign(ref_cache[list], scan8[0], 4, 4, 8, ref, 1);
	                }
	            }
	            for(list=0; list<list_count; list++){
	            	if(((mb_type) & (MB_TYPE_P0L0<<((0)+2*(list)))) != 0) {
	                    int mx=0,my=0,mpx=0,mpy=0;
	                    int[] args = new int[2];
	                    args[0] = mx;
	                    args[1] = my;
	                    pred_motion(0, 4, list, ref_cache[list][ scan8[0] ], args);
	                    mx = args[0];
	                    my = args[1];
						{
						    int amvd0 = this.mvd_cache[list][scan8[0] - 1][0] +
						    			this.mvd_cache[list][scan8[0] - 8][0];
						    int amvd1 = this.mvd_cache[list][scan8[0] - 1][1] +
						    			this.mvd_cache[list][scan8[0] - 8][1];						

                            // DebugTool.dumpDebugFrameData(this, " -- BEFORE-decode_cabac_mb_mvd", false);
						    // DebugTool.printDebugString("  -- ff_h264_decode_mb_cabac: CASE2: amvd0="+amvd0+", amvd1="+amvd1+", index=0\n");

						    args[0] = mpx;
						    mx += cabac.decode_cabac_mb_mvd( this, 40, amvd0, args );
						    mpx = args[0];
						    args[0] = mpy;
						    my += cabac.decode_cabac_mb_mvd( this, 47, amvd1, args );
						    mpy = args[0];
						}
	                    
	                    //tprintf(s.avctx, "final mv:%d %d\n", mx, my);

	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE8\n");

						Rectangle.fill_rectangle_mvd_cache(mvd_cache[list], scan8[0], 4, 4, 8, pack8to16(mpx,mpy), 2);
	                    Rectangle.fill_rectangle_mv_cache(mv_cache[list], scan8[0], 4, 4, 8, pack16to32(mx,my), 4);
	                }
	            }
	        }
	        else if((mb_type & MB_TYPE_16x8) != 0){
	            for(list=0; list<list_count; list++){
	                    for(i=0; i<2; i++){
	                        if(((mb_type) & (MB_TYPE_P0L0<<((i)+2*(list)))) != 0){
	                            int ref;
	                            if(ref_count[list] > 1){
	                                ref= cabac.decode_cabac_mb_ref( this, list, 8*i );
	                                if(ref >= (int)ref_count[list]){
	                                    //av_log(s.avctx, AV_LOG_ERROR, "Reference %d >= %d\n", ref, h->ref_count[list]);
	                                    return -1;
	                                }
	                            }else
	                                ref=0;
	                            Rectangle.fill_rectangle_sign(ref_cache[list], scan8[0] + 16*i, 4, 2, 8, ref, 1);
	                        }else {
	                        	// !!????????? Need Unsigned??
	                        	Rectangle.fill_rectangle_sign(ref_cache[list], scan8[0] + 16*i, 4, 2, 8, (LIST_NOT_USED/*&0xFF*/), 1);
	                        } // if
	                    }
	            }
	            for(list=0; list<list_count; list++){
	                for(i=0; i<2; i++){
		            	if(((mb_type) & (MB_TYPE_P0L0<<((i)+2*(list)))) != 0) {
	                        int mx=0,my=0,mpx=0,mpy=0;
		                    int[] args = new int[2];
		                    args[0] = mx;
		                    args[1] = my;
	                        pred_16x8_motion(8*i, list, ref_cache[list][scan8[0] + 16*i], args);
		                    mx = args[0];
		                    my = args[1];
							{
							    int amvd0 = this.mvd_cache[list][scan8[8*i] - 1][0] +
							    			this.mvd_cache[list][scan8[8*i] - 8][0];
							    int amvd1 = this.mvd_cache[list][scan8[8*i] - 1][1] +
							    			this.mvd_cache[list][scan8[8*i] - 8][1];
							    
	                            // DebugTool.dumpDebugFrameData(this, " -- BEFORE-decode_cabac_mb_mvd", false);
	                            // DebugTool.printDebugString("  -- ff_h264_decode_mb_cabac: CASE3: amvd0="+amvd0+", amvd1="+amvd1+", index="+(8*i)+"\n");

							    args[0] = mpx;
							    mx += cabac.decode_cabac_mb_mvd( this, 40, amvd0, args );
							    mpx = args[0];
							    args[0] = mpy;
							    my += cabac.decode_cabac_mb_mvd( this, 47, amvd1, args );
							    mpy = args[0];
							}

	                        //tprintf(s.avctx, "final mv:%d %d\n", mx, my);

    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE9\n");

							Rectangle.fill_rectangle_mvd_cache(mvd_cache[list], scan8[0] + 16*i, 4, 2, 8, pack8to16(mpx,mpy), 2);
							Rectangle.fill_rectangle_mv_cache(mv_cache[list], scan8[0] + 16*i, 4, 2, 8, pack16to32(mx,my), 4);
	                    }else{
	                    	
    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE10\n");

	                    	Rectangle.fill_rectangle_mvd_cache(mvd_cache[list], scan8[0] + 16*i, 4, 2, 8, 0, 2);
	                    	Rectangle.fill_rectangle_mv_cache(mv_cache[list], scan8[0] + 16*i, 4, 2, 8, 0, 4);
	                    }
	                }
	            }
	        }else{
	            //assert((mb_type & MB_TYPE_8x16) != 0);
	            for(list=0; list<list_count; list++){
	                    for(i=0; i<2; i++){
	    	            	if(((mb_type) & (MB_TYPE_P0L0<<((i)+2*(list)))) != 0) {
	                            int ref;
	                            if(ref_count[list] > 1){
	                                ref= cabac.decode_cabac_mb_ref( this, list, 4*i );
	                                if(ref >= (int)ref_count[list]){
	                                    //av_log(s.avctx, AV_LOG_ERROR, "Reference %d >= %d\n", ref, h->ref_count[list]);
	                                    return -1;
	                                }
	                            }else
	                                ref=0;
	                            Rectangle.fill_rectangle_sign(ref_cache[list], scan8[0] + 2*i, 2, 4, 8, ref, 1);
	                        }else {
	                        	// !!!????????????? Need to be unsigned
	                        	Rectangle.fill_rectangle_sign(ref_cache[list], scan8[0] + 2*i, 2, 4, 8, (LIST_NOT_USED/*&0xFF*/), 1);
	                        } // try
	                    }
	            }
	            for(list=0; list<list_count; list++){
	                for(i=0; i<2; i++){
		            	if(((mb_type) & (MB_TYPE_P0L0<<((i)+2*(list)))) != 0) {
	                        int mx=0,my=0,mpx=0,mpy=0;
		                    int[] args = new int[2];
		                    args[0] = mx;
		                    args[1] = my;
	                        pred_8x16_motion(i*4, list, ref_cache[list][ scan8[0] + 2*i ], args);
		                    mx = args[0];
		                    my = args[1];
							{
							    int amvd0 = this.mvd_cache[list][scan8[4*i] - 1][0] +
							    			this.mvd_cache[list][scan8[4*i] - 8][0];
							    int amvd1 = this.mvd_cache[list][scan8[4*i] - 1][1] +
							    			this.mvd_cache[list][scan8[4*i] - 8][1];
							    
	                            // DebugTool.dumpDebugFrameData(this, " -- BEFORE-decode_cabac_mb_mvd", false);
	                            // DebugTool.printDebugString("  -- ff_h264_decode_mb_cabac: CASE4: amvd0="+amvd0+", amvd1="+amvd1+", index="+(4*i)+"\n");

							    args[0] = mpx;
							    mx += cabac.decode_cabac_mb_mvd( this, 40, amvd0, args);
							    mpx = args[0];
							    args[0] = mpy;
							    my += cabac.decode_cabac_mb_mvd( this, 47, amvd1, args);
							    mpy = args[0];
							}

	                        //tprintf(s.avctx, "final mv:%d %d\n", mx, my);
    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE11\n");

							Rectangle.fill_rectangle_mvd_cache(mvd_cache[list], scan8[0] + 2*i, 2, 4, 8, pack8to16(mpx,mpy), 2);
							Rectangle.fill_rectangle_mv_cache(mv_cache[list], scan8[0] + 2*i, 2, 4, 8, pack16to32(mx,my), 4);
	                    }else{
    	                	// DebugTool.printDebugString(" *** FILL MVD_CACHE CASE12\n");

	                    	Rectangle.fill_rectangle_mvd_cache(mvd_cache[list], scan8[0] + 2*i, 2, 4, 8, 0, 2);
	                    	Rectangle.fill_rectangle_mv_cache(mv_cache[list], scan8[0] + 2*i, 2, 4, 8, 0, 4);
	                    }
	                }
	            }
	        }
	    }

	   if( ( mb_type &(MB_TYPE_16x16|MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_8x8) ) != 0) {
	        chroma_pred_mode_table[mb_xy] = 0;
	        write_back_motion( mb_type );
	   }

	    if( ( mb_type & MB_TYPE_INTRA16x16 ) == 0 ) {
	        cbp  = cabac.decode_cabac_mb_cbp_luma(this);
	        if(sps.chroma_format_idc != 0)
	            cbp |= cabac.decode_cabac_mb_cbp_chroma(this) << 4;
	    }

	    cbp_table[mb_xy] = this.cbp = cbp;

	    if( dct8x8_allowed != 0 && (cbp&15) != 0 && ( mb_type & 7 )==0 ) {
	        mb_type |= MB_TYPE_8x8DCT * cabac.get_cabac_noinline( cabac_state, 399 + neighbor_transform_size );
	    }
	    s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]= mb_type;

	    if( cbp !=0 || ( mb_type & MB_TYPE_INTRA16x16 ) != 0) {
	        int[] scan, scan8x8;
	        long[] qmul;

	        if( (mb_type & MB_TYPE_INTERLACED) != 0){
	            scan8x8= (s.qscale!=0) ? field_scan8x8 : field_scan8x8_q0;
	            scan= (s.qscale!=0) ? field_scan : field_scan_q0;
	        }else{
	            scan8x8= (s.qscale!=0) ? zigzag_scan8x8 : zigzag_scan8x8_q0;
	            scan= (s.qscale!=0) ? zigzag_scan : zigzag_scan_q0;
	        }

	        // decode_cabac_mb_dqp
	        if( cabac.get_cabac_noinline( cabac_state, 60 + (last_qscale_diff != 0?1:0)) != 0 ){
	            int val = 1;
	            int ctx= 2;

	            while( cabac.get_cabac_noinline( cabac_state, 60 + ctx) != 0 ) {
	                ctx= 3;
	                val++;
	                if(val > 102){ //prevent infinite loop
	                    //av_log(h->s.avctx, AV_LOG_ERROR, "cabac decode of qscale diff failed at %d %d\n", s.mb_x, s.mb_y);
	                    return -1;
	                }
	            }

	            if( (val&0x01) != 0 )
	                val=   (val + 1)>>1 ;
	            else
	                val= -((val + 1)>>1);
	            last_qscale_diff = val;
	            s.qscale += val;
	            if(((int)s.qscale) > 51){
	                if(s.qscale<0) s.qscale+= 52;
	                else            s.qscale-= 52;
	            }
	            chroma_qp[0] = pps.chroma_qp_table[0][s.qscale]; // get_chroma_qp(0, s.qscale);
	            chroma_qp[1] = pps.chroma_qp_table[1][s.qscale]; // get_chroma_qp(1, s.qscale);
	        }else
	            last_qscale_diff=0;

	        if(( mb_type & MB_TYPE_INTRA16x16) != 0 ) {
	            int i;
	            //av_log( s.avctx, AV_LOG_ERROR, "INTRA16x16 DC\n" );
	            //AV_ZERO128(mb_luma_dc+0);
	            //AV_ZERO128(mb_luma_dc+8);
	            Arrays.fill(mb_luma_dc, 0, 16, (short)0); // Fill entire 16 uint16_t with 0.
	            decode_cabac_residual_dc( mb_luma_dc, 0, 0, LUMA_DC_BLOCK_INDEX, scan, 0, 16);

	            if( (cbp&15) != 0 ) {
	                qmul = dequant4_coeff[0][s.qscale];
	                for( i = 0; i < 16; i++ ) {
	                    //av_log( s.avctx, AV_LOG_ERROR, "INTRA16x16 AC:%d\n", i );
	                    decode_cabac_residual_nondc(mb, 16*i, 1, i, scan, 1, qmul, 15);
	                }
	            } else {
	            	Rectangle.fill_rectangle_unsign(non_zero_count_cache, scan8[0], 4, 4, 8, 0, 1);
	            }
	        } else {
	            int i8x8, i4x4;
	            for( i8x8 = 0; i8x8 < 4; i8x8++ ) {
	                if( (cbp & (1<<i8x8)) != 0 ) {
	                    if( (mb_type & MB_TYPE_8x8DCT) != 0 ) {
	                        decode_cabac_residual_nondc(mb, 64*i8x8, 5, 4*i8x8,
	                            scan8x8, 0, dequant8_coeff[(( mb_type & 7)!=0) ? 0:1][s.qscale], 64);
	                    } else {
	                        qmul = dequant4_coeff[(( mb_type & 7)!=0) ? 0:3][s.qscale];
	                        for( i4x4 = 0; i4x4 < 4; i4x4++ ) {
	                            int index = 4*i8x8 + i4x4;
	                            //av_log( s.avctx, AV_LOG_ERROR, "Luma4x4: %d\n", index );
	//START_TIMER
	                            decode_cabac_residual_nondc(mb, 16*index, 2, index, scan, 0, qmul, 16);
	//STOP_TIMER("decode_residual")
	                        }
	                    }
	                } else {
	                	int[] nnz = non_zero_count_cache;
	                    int nnz_offset = scan8[4*i8x8]; // &h->non_zero_count_cache[ scan8[4*i8x8] ];
	                    nnz[nnz_offset +0] = nnz[nnz_offset +1] = nnz[nnz_offset +8] = nnz[nnz_offset +9] = 0;
	                }
	            }
	        }

	        if( (cbp&0x030) != 0 ){
	            int c;
	            //AV_ZERO128(mb_chroma_dc);
	            // Fill 4x2 uint_6 with 0 in mb_chroma_dc
	            for(int j=0;j<2;j++)
	            	for(int k=0;k<4;k++)
	            		mb_chroma_dc[j][k] = 0;
	            for( c = 0; c < 2; c++ ) {
	                //av_log( s.avctx, AV_LOG_ERROR, "INTRA C%d-DC\n",c );
	                decode_cabac_residual_dc(mb_chroma_dc[c], 0 , 3, CHROMA_DC_BLOCK_INDEX+c, H264Data.chroma_dc_scan, 0, 4);
	            }
	        }

	        if( (cbp&0x020) != 0 ) {
	            int c, i;
	            for( c = 0; c < 2; c++ ) {
	                qmul = dequant4_coeff[c+1+((( mb_type & 7)!= 0) ? 0:3)][chroma_qp[c]];
	                for( i = 0; i < 4; i++ ) {
	                    int index = 16 + 4 * c + i;
	                    //av_log( s.avctx, AV_LOG_ERROR, "INTRA C%d-AC %d\n",c, index - 16 );
	                    decode_cabac_residual_nondc(mb, 16*index, 4, index, scan, 1, qmul, 15);
	                }
	            }
	        } else {
	            int[] nnz = non_zero_count_cache;
	            nnz[ scan8[16]+0 ] = nnz[ scan8[16]+1 ] =nnz[ scan8[16]+8 ] =nnz[ scan8[16]+9 ] =
	            nnz[ scan8[20]+0 ] = nnz[ scan8[20]+1 ] =nnz[ scan8[20]+8 ] =nnz[ scan8[20]+9 ] = 0;
	        }
	    } else {
	        int[] nnz= non_zero_count_cache;
	        Rectangle.fill_rectangle_unsign(nnz, scan8[0], 4, 4, 8, 0, 1);
	        nnz[ scan8[16]+0 ] = nnz[ scan8[16]+1 ] =nnz[ scan8[16]+8 ] =nnz[ scan8[16]+9 ] =
	        nnz[ scan8[20]+0 ] = nnz[ scan8[20]+1 ] =nnz[ scan8[20]+8 ] =nnz[ scan8[20]+9 ] = 0;
	        last_qscale_diff = 0;
	    }

	    s.current_picture.qscale_table[mb_xy]= s.qscale;
	    write_back_non_zero_count();

	    if(mb_mbaff != 0){
	        ref_count[0] >>= 1;
	        ref_count[1] >>= 1;
	    }

        // DebugTool.dumpDebugFrameData(this, "AFTER-ff_h264_decode_mb_cabac");

	    return 0;
	}
	
	public void predict_field_decoding_flag(){
	    int mb_xy= s.mb_x + s.mb_y*s.mb_stride;
	    int mb_type = (int)((this.slice_table_base[this.slice_table_offset + mb_xy-1] == this.slice_num)
	                ? s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy-1]
	                : (this.slice_table_base[this.slice_table_offset + mb_xy-s.mb_stride] == this.slice_num)
	                ? s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy-s.mb_stride]
	                : 0);
	    this.mb_mbaff = this.mb_field_decoding_flag = ((mb_type & MB_TYPE_INTERLACED) != 0 ? 1 : 0);
	}

	/**
	 *
	 * @return non zero if the loop filter can be skiped
	 */
	public int fill_filter_caches(int mb_type){
	    int mb_xy= this.mb_xy;
	    int top_xy;
	    int[] left_xy = new int[2];
	    int top_type;
	    int[] left_type = new int[2];

	    top_xy     = mb_xy  - (s.mb_stride << mb_field_decoding_flag);

	    //FIXME deblocking could skip the intra and nnz parts.

	    /* Wow, what a mess, why didn't they simplify the interlacing & intra
	     * stuff, I can't imagine that these complex rules are worth it. */

	    left_xy[1] = left_xy[0] = mb_xy-1;
	    if(mb_aff_frame!=0){
	        int left_mb_field_flag     = ((int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy-1] & MB_TYPE_INTERLACED);
	        int curr_mb_field_flag     = (mb_type & MB_TYPE_INTERLACED);
	        if((s.mb_y&1)!=0){
	            if (left_mb_field_flag != curr_mb_field_flag) {
	                left_xy[0] -= s.mb_stride;
	            }
	        }else{
	            if(curr_mb_field_flag!=0){
	                top_xy      += s.mb_stride & (((s.current_picture.mb_type_base[s.current_picture.mb_type_offset + top_xy    ]>>7)&1)-1);
	            }
	            if (left_mb_field_flag != curr_mb_field_flag) {
	                left_xy[1] += s.mb_stride;
	            }
	        }
	    }

	    this.top_mb_xy = top_xy;
	    this.left_mb_xy[0] = left_xy[0];
	    this.left_mb_xy[1] = left_xy[1];
	    {
	        //for sufficiently low qp, filtering wouldn't do anything
	        //this is a conservative estimate: could also check beta_offset and more accurate chroma_qp
	        int qp_thresh = this.qp_thresh; //FIXME strictly we should store qp_thresh for each mb of a slice
	        int qp = s.current_picture.qscale_table[mb_xy];
	        if(qp <= qp_thresh
	           && (left_xy[0]<0 || ((qp + s.current_picture.qscale_table[left_xy[0]] + 1)>>1) <= qp_thresh)
	           && (top_xy   < 0 || ((qp + s.current_picture.qscale_table[top_xy    ] + 1)>>1) <= qp_thresh)){
	            if(0==mb_aff_frame)
	                return 1;
	            if(   (left_xy[0]< 0            || ((qp + s.current_picture.qscale_table[left_xy[1]             ] + 1)>>1) <= qp_thresh)
	               && (top_xy    < s.mb_stride || ((qp + s.current_picture.qscale_table[top_xy    -s.mb_stride] + 1)>>1) <= qp_thresh))
	                return 1;
	        }
	    }

	    top_type     = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + top_xy]    ;
	    left_type[0] = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + left_xy[0]];
	    left_type[1] = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + left_xy[1]];
	    if(this.deblocking_filter == 2){
	        if(this.slice_table_base[this.slice_table_offset + top_xy     ] != this.slice_num) top_type= 0;
	        if(this.slice_table_base[this.slice_table_offset + left_xy[0] ] != this.slice_num) left_type[0]= left_type[1]= 0;
	    }else{
	        if(this.slice_table_base[this.slice_table_offset + top_xy     ] == 0x0000FFFF) top_type= 0;
	        if(this.slice_table_base[this.slice_table_offset + left_xy[0] ] == 0x0000FFFF) left_type[0]= left_type[1] =0;
	    }
	    this.top_type    = top_type    ;
	    this.left_type[0]= left_type[0];
	    this.left_type[1]= left_type[1];

	    if(0!=(mb_type & 7))
	        return 0;

	    /*
	    AV_COPY64(&this.non_zero_count_cache[0+8*1], &this.non_zero_count[mb_xy][ 0]);
	    AV_COPY64(&this.non_zero_count_cache[0+8*2], &this.non_zero_count[mb_xy][ 8]);
	    AV_COPY32(&this.non_zero_count_cache[0+8*5], &this.non_zero_count[mb_xy][16]);
	    AV_COPY32(&this.non_zero_count_cache[4+8*3], &this.non_zero_count[mb_xy][20]);
	    AV_COPY64(&this.non_zero_count_cache[0+8*4], &this.non_zero_count[mb_xy][24]);
	    */
	    // DebugTool.printDebugString("    ----mb_xy="+mb_xy+", top_xy="+top_xy+", left_xy[0]="+left_xy[0]+", left_xy[1]="+left_xy[1]+"\n");
	    
	    System.arraycopy(this.non_zero_count[mb_xy], 0, this.non_zero_count_cache, 0+8*1, 8);
	    System.arraycopy(this.non_zero_count[mb_xy], 8, this.non_zero_count_cache, 0+8*2, 8);
	    System.arraycopy(this.non_zero_count[mb_xy], 16, this.non_zero_count_cache, 0+8*5, 4);
	    System.arraycopy(this.non_zero_count[mb_xy], 20, this.non_zero_count_cache, 4+8*3, 4);
	    System.arraycopy(this.non_zero_count[mb_xy], 24, this.non_zero_count_cache, 0+8*4, 8);

	    this.cbp= this.cbp_table[mb_xy];

	    {
	        int list;
	        for(list=0; list<this.list_count; list++){
	            //int8_t *ref;
	        	int[] ref_base;
	        	int ref_offset;
	            int y, b_stride;
	            //int16_t (*mv_dst)[2];
	            //int16_t (*mv_src)[2];
	            int[][] mv_dst_base;
	            int[][] mv_src_base;
	            int mv_dst_offset;
	            int mv_src_offset;

	            if(0==((mb_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list))))){
	            	//// DebugTool.printDebugString("    ---- fill_filter_caches CASE 1\n");
	            	
	                Rectangle.fill_rectangle_mv_cache(  this.mv_cache[list], scan8[0], 4, 4, 8, pack16to32(0,0), 4);
	                //AV_WN32A(&this.ref_cache[list][scan8[ 0]], ((LIST_NOT_USED)&0xFF)*0x01010101u);
	                //AV_WN32A(&this.ref_cache[list][scan8[ 2]], ((LIST_NOT_USED)&0xFF)*0x01010101u);
	                //AV_WN32A(&this.ref_cache[list][scan8[ 8]], ((LIST_NOT_USED)&0xFF)*0x01010101u);
	                //AV_WN32A(&this.ref_cache[list][scan8[10]], ((LIST_NOT_USED)&0xFF)*0x01010101u);

	                // !!!?????? Need to be unsigned
	                for(int i=0;i<4;i++) {
	                	this.ref_cache[list][scan8[ 0] + i] = (LIST_NOT_USED);
	                	this.ref_cache[list][scan8[ 2] + i] = (LIST_NOT_USED);
	                	this.ref_cache[list][scan8[ 8] + i] = (LIST_NOT_USED);
	                	this.ref_cache[list][scan8[ 10] + i] = (LIST_NOT_USED);
	                }
	                continue;
	            }

	            ref_base = s.current_picture.ref_index[list];
	            ref_offset = 4*mb_xy;
	            {
/* ?????????????????????????????
                int (*ref2frm)[64] = h->ref2frm[ h->slice_num&(MAX_SLICES-1) ][0] + (MB_MBAFF ? 20 : 2);
                AV_WN32A(&h->ref_cache[list][scan8[ 0]], (pack16to32(ref2frm[list][ref[0]],ref2frm[list][ref[1]])&0x00FF00FF)*0x0101);
                AV_WN32A(&h->ref_cache[list][scan8[ 2]], (pack16to32(ref2frm[list][ref[0]],ref2frm[list][ref[1]])&0x00FF00FF)*0x0101);
                ref += 2;
                AV_WN32A(&h->ref_cache[list][scan8[ 8]], (pack16to32(ref2frm[list][ref[0]],ref2frm[list][ref[1]])&0x00FF00FF)*0x0101);
                AV_WN32A(&h->ref_cache[list][scan8[10]], (pack16to32(ref2frm[list][ref[0]],ref2frm[list][ref[1]])&0x00FF00FF)*0x0101);
*/	            	
	            	int[][] ref2frm_base = this.ref2frm[ this.slice_num&(MAX_SLICES-1) ];
	                int ref2frm_offset = (mb_mbaff!=0 ? 20 : 2);

	                this.ref_cache[list][scan8[ 0] +0] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[ 0] +1] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[ 0] +2] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;
	                this.ref_cache[list][scan8[ 0] +3] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;

	                this.ref_cache[list][scan8[ 2] +0] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[ 2] +1] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[ 2] +2] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;
	                this.ref_cache[list][scan8[ 2] +3] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;
	                
	                ref_offset += 2;
	                this.ref_cache[list][scan8[ 8] +0] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[ 8] +1] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[ 8] +2] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;
	                this.ref_cache[list][scan8[ 8] +3] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;

	                this.ref_cache[list][scan8[10] +0] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[10] +1] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 0]]&0x0FF;
	                this.ref_cache[list][scan8[10] +2] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;
	                this.ref_cache[list][scan8[10] +3] = ref2frm_base[list][ref2frm_offset + ref_base[ref_offset + 1]]&0x0FF;
	            }

	            b_stride = this.b_stride;
	            mv_dst_base   = this.mv_cache[list];
	            mv_dst_offset = scan8[0];
	            mv_src_base   = s.current_picture.motion_val_base[list];
	            mv_src_offset = s.current_picture.motion_val_offset[list] + 4*s.mb_x + 4*s.mb_y*b_stride;
	            
            	//// DebugTool.printDebugString("    ---- fill_filter_caches CASE 2\n");

                /*           	
                b_stride = h->b_stride;
                mv_dst   = &h->mv_cache[list][scan8[0]];
                mv_src   = &s->current_picture.motion_val[list][4*s->mb_x + 4*s->mb_y*b_stride];
                for(y=0; y<4; y++){
                    AV_COPY128(mv_dst + 8*y, mv_src + y*b_stride);
                } 
                */           	
	            for(y=0; y<4; y++){
	            	// copy 8 int16_t
	                //AV_COPY128(mv_dst + 8*y, mv_src + y*b_stride);
	            	//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	            	for(int i=0;i<4;i++) {
	            		mv_dst_base[mv_dst_offset + 8*y + i][0] = mv_src_base[mv_src_offset + y*b_stride + i][0];
	            		mv_dst_base[mv_dst_offset + 8*y + i][1] = mv_src_base[mv_src_offset + y*b_stride + i][1];

	            		//// DebugTool.printDebugString("    ---- copy "+mv_src_base[mv_src_offset + y*b_stride + i][0]+", "+mv_src_base[mv_src_offset + y*b_stride + i][1]+"\n");
	            		
	            	} // for
	            }

	        }
	    }


	/*
	0 . T T. T T T T
	1 L . .L . . . .
	2 L . .L . . . .
	3 . T TL . . . .
	4 L . .L . . . .
	5 L . .. . . . .
	*/
	//FIXME constraint_intra_pred & partitioning & nnz (let us hope this is just a typo in the spec)
	    if(top_type!=0){
	        //AV_COPY32(&this.non_zero_count_cache[4+8*0], &this.non_zero_count[top_xy][4+3*8]);
	    	for(int i=0;i<4;i++)
	    		this.non_zero_count_cache[4+8*0 + i] = this.non_zero_count[top_xy][4+3*8 + i];
	    }

	    if(left_type[0]!=0){
	        this.non_zero_count_cache[3+8*1]= this.non_zero_count[left_xy[0]][7+0*8];
	        this.non_zero_count_cache[3+8*2]= this.non_zero_count[left_xy[0]][7+1*8];
	        this.non_zero_count_cache[3+8*3]= this.non_zero_count[left_xy[0]][7+2*8];
	        this.non_zero_count_cache[3+8*4]= this.non_zero_count[left_xy[0]][7+3*8];
	    }

	    // CAVLC 8x8dct requires NNZ values for residual decoding that differ from what the loop filter needs
	    if(0==pps.cabac && 0!=this.pps.transform_8x8_mode){
	        if(0!=(top_type & MB_TYPE_8x8DCT)){
	            this.non_zero_count_cache[4+8*0]=
	            this.non_zero_count_cache[5+8*0]= this.cbp_table[top_xy] & 4;
	            this.non_zero_count_cache[6+8*0]=
	            this.non_zero_count_cache[7+8*0]= this.cbp_table[top_xy] & 8;
	        }
	        if(0!=(left_type[0] & MB_TYPE_8x8DCT)){
	            this.non_zero_count_cache[3+8*1]=
	            this.non_zero_count_cache[3+8*2]= this.cbp_table[left_xy[0]]&2; //FIXME check MBAFF
	        }
	        if(0!=(left_type[1] & MB_TYPE_8x8DCT)){
	            this.non_zero_count_cache[3+8*3]=
	            this.non_zero_count_cache[3+8*4]= this.cbp_table[left_xy[1]]&8; //FIXME check MBAFF
	        }

	        if(0!=(mb_type & MB_TYPE_8x8DCT)){
	            this.non_zero_count_cache[scan8[0   ]]= this.non_zero_count_cache[scan8[1   ]]=
	            this.non_zero_count_cache[scan8[2   ]]= this.non_zero_count_cache[scan8[3   ]]= this.cbp & 1;

	            this.non_zero_count_cache[scan8[0+ 4]]= this.non_zero_count_cache[scan8[1+ 4]]=
	            this.non_zero_count_cache[scan8[2+ 4]]= this.non_zero_count_cache[scan8[3+ 4]]= this.cbp & 2;

	            this.non_zero_count_cache[scan8[0+ 8]]= this.non_zero_count_cache[scan8[1+ 8]]=
	            this.non_zero_count_cache[scan8[2+ 8]]= this.non_zero_count_cache[scan8[3+ 8]]= this.cbp & 4;

	            this.non_zero_count_cache[scan8[0+12]]= this.non_zero_count_cache[scan8[1+12]]=
	            this.non_zero_count_cache[scan8[2+12]]= this.non_zero_count_cache[scan8[3+12]]= this.cbp & 8;
	        }
	    }

	    if(0!=(mb_type & (MB_TYPE_16x16|MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_8x8)) || 0!=(mb_type & MB_TYPE_DIRECT2)){
	        int list;
	        for(list=0; list<this.list_count; list++){
	            if(0!=((top_type) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list))))){
	                int b_xy= (int)this.mb2b_xy[top_xy] + 3*this.b_stride;
	                int b8_xy= 4*top_xy + 2;
	                /*????????????????????????????/
	                int (*ref2frm)[64] = this.ref2frm[ this.slice_table_base[this.slice_table_offset + top_xy]&(MAX_SLICES-1) ][0] + (mb_mbaff!=0 ? 20 : 2);
	                AV_COPY128(this.mv_cache[list][scan8[0] + 0 - 1*8], s.current_picture.motion_val[list][b_xy + 0]);
	                this.ref_cache[list][scan8[0] + 0 - 1*8]=
	                this.ref_cache[list][scan8[0] + 1 - 1*8]= ref2frm[list][s.current_picture.ref_index[list][b8_xy + 0]];
	                this.ref_cache[list][scan8[0] + 2 - 1*8]=
	                this.ref_cache[list][scan8[0] + 3 - 1*8]= ref2frm[list][s.current_picture.ref_index[list][b8_xy + 1]];
	                */
	                int[][] ref2frm_base = this.ref2frm[ this.slice_table_base[this.slice_table_offset + top_xy]&(MAX_SLICES-1) ];
	                int ref2frm_offset = (mb_mbaff!=0 ? 20 : 2);
	                // copy 8 int16_t
	                //AV_COPY128(this.mv_cache[list][scan8[0] + 0 - 1*8], s.current_picture.motion_val[list][b_xy + 0]);
	            	//// DebugTool.printDebugString("    ---- fill_filter_caches CASE 3\n");
	            	
	                for(int i=0;i<4;i++) {
	                	this.mv_cache[list][scan8[0] + 0 - 1*8 + i][0] = s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] + b_xy + 0 + i][0];
	                	this.mv_cache[list][scan8[0] + 0 - 1*8 + i][1] = s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list] + b_xy + 0 + i][1];
	                } // for i
	                this.ref_cache[list][scan8[0] + 0 - 1*8]=
	                this.ref_cache[list][scan8[0] + 1 - 1*8]= ref2frm_base[list][ref2frm_offset + s.current_picture.ref_index[list][b8_xy + 0]];
	                this.ref_cache[list][scan8[0] + 2 - 1*8]=
	                this.ref_cache[list][scan8[0] + 3 - 1*8]= ref2frm_base[list][ref2frm_offset + s.current_picture.ref_index[list][b8_xy + 1]];

	            }else{
	                //AV_ZERO128(this.mv_cache[list][scan8[0] + 0 - 1*8]);
	                //AV_WN32A(&this.ref_cache[list][scan8[0] + 0 - 1*8], ((LIST_NOT_USED)&0xFF)*0x01010101u);
	            	//// DebugTool.printDebugString("    ---- fill_filter_caches CASE 4\n");

	                for(int i=0;i<4;i++) {
	                	this.mv_cache[list][scan8[0] + 0 - 1*8 + i][0] = 0;
	                	this.mv_cache[list][scan8[0] + 0 - 1*8 + i][1] = 0;
	                	
	                	// !!!?????? Need to be unsigned
	                	this.ref_cache[list][scan8[0] + 0 - 1*8 + i] = (LIST_NOT_USED)/*&0xFF*/;
	                } // for i
	            }

	            if(0==((mb_type^left_type[0])&MB_TYPE_INTERLACED)){
	                if(0!=((left_type[0]) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list))))){
	                    int b_xy= (int)this.mb2b_xy[left_xy[0]] + 3;
	                    int b8_xy= 4*left_xy[0] + 1;
	                    int[][] ref2frm_base = this.ref2frm[ this.slice_table_base[this.slice_table_offset + left_xy[0]]&(MAX_SLICES-1) ];
	                    int ref2frm_offset = (mb_mbaff != 0 ? 20 : 2);
	                    /*
	                    AV_COPY32(this.mv_cache[list][scan8[0] - 1 + 0 ], s.current_picture.motion_val[list][b_xy + this.b_stride*0]);
	                    AV_COPY32(this.mv_cache[list][scan8[0] - 1 + 8 ], s.current_picture.motion_val[list][b_xy + this.b_stride*1]);
	                    AV_COPY32(this.mv_cache[list][scan8[0] - 1 +16 ], s.current_picture.motion_val[list][b_xy + this.b_stride*2]);
	                    AV_COPY32(this.mv_cache[list][scan8[0] - 1 +24 ], s.current_picture.motion_val[list][b_xy + this.b_stride*3]);
	                    */
		            	//// DebugTool.printDebugString("    ---- fill_filter_caches CASE 5\n");

	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list]+ b_xy + this.b_stride*0], 0
	                    		, this.mv_cache[list][scan8[0] - 1 + 0 ], 0, 2);
	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list]+ b_xy + this.b_stride*1], 0
	                    		, this.mv_cache[list][scan8[0] - 1 + 8 ], 0, 2);
	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list]+ b_xy + this.b_stride*2], 0
	                    		, this.mv_cache[list][scan8[0] - 1 + 16 ], 0, 2);
	                    System.arraycopy(s.current_picture.motion_val_base[list][s.current_picture.motion_val_offset[list]+ b_xy + this.b_stride*3], 0
	                    		, this.mv_cache[list][scan8[0] - 1 + 24 ], 0, 2);
	                    this.ref_cache[list][scan8[0] - 1 + 0 ]=
	                    this.ref_cache[list][scan8[0] - 1 + 8 ]= ref2frm_base[list][ref2frm_offset + s.current_picture.ref_index[list][b8_xy + 2*0]];
	                    this.ref_cache[list][scan8[0] - 1 +16 ]=
	                    this.ref_cache[list][scan8[0] - 1 +24 ]= ref2frm_base[list][ref2frm_offset + s.current_picture.ref_index[list][b8_xy + 2*1]];
	                }else{
	                	/*
	                    AV_ZERO32(this.mv_cache [list][scan8[0] - 1 + 0 ]);
	                    AV_ZERO32(this.mv_cache [list][scan8[0] - 1 + 8 ]);
	                    AV_ZERO32(this.mv_cache [list][scan8[0] - 1 +16 ]);
	                    AV_ZERO32(this.mv_cache [list][scan8[0] - 1 +24 ]);
	                    */
		            	//// DebugTool.printDebugString("    ---- fill_filter_caches CASE 6\n");

	                	this.mv_cache [list][scan8[0] - 1 + 0 ][0]=this.mv_cache [list][scan8[0] - 1 + 0 ][1]=
		                this.mv_cache [list][scan8[0] - 1 + 8 ][0]=this.mv_cache [list][scan8[0] - 1 + 8 ][1]=
			            this.mv_cache [list][scan8[0] - 1 + 16 ][0]=this.mv_cache [list][scan8[0] - 1 + 16 ][1]=
				        this.mv_cache [list][scan8[0] - 1 + 24 ][0]=this.mv_cache [list][scan8[0] - 1 + 24 ][1]= 0;
	                	
	                    this.ref_cache[list][scan8[0] - 1 + 0  ]=
	                    this.ref_cache[list][scan8[0] - 1 + 8  ]=
	                    this.ref_cache[list][scan8[0] - 1 + 16 ]=
	                    this.ref_cache[list][scan8[0] - 1 + 24 ]= LIST_NOT_USED;
	                }
	            }
	        }
	    }

	    return 0;
	}

    private static int av_clip(int a, int amin, int amax) {
        if      (a < amin) return amin;
        else if (a > amax) return amax;
        else               return a;
    }
    
    private static int av_clip_uint8(int a) {
        if ((a&(~0x000000FF)) != 0) return (-a)>>31;
        else           return a;
    }    

    public void filter_mb_edgev( 
			//uint8_t *pix,
			int[] pix_base,
			int pix_offset,
			int stride, 
			//int16_t bS[4],
			int[] bS_base,
			int bS_offset,			
    		int qp) {
//        public void filter_mb_edgev( uint8_t *pix, int stride, int16_t bS[4], unsigned int qp, H264Context *h) {
        int index_a = qp + this.slice_alpha_c0_offset;
        int alpha = LoopFilter.alpha_table[index_a];
        int beta  = LoopFilter.beta_table[qp + this.slice_beta_offset];
        if (alpha ==0 || beta == 0) return;

        if( bS_base[bS_offset + 0] < 4 ) {
            //int8_t tc[4];
        	int[] tc = new int[4];
            tc[0] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 0]];
            tc[1] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 1]];
            tc[2] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 2]];
            tc[3] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 3]];
            
            // DebugTool.printDebugString("filter_mb_edgev: index_a="+index_a+", bS={"+bS_base[bS_offset + 0]+","+bS_base[bS_offset + 1]+","+bS_base[bS_offset + 2]+","+bS_base[bS_offset + 3]+"}, tc={"+tc[0]+","+tc[1]+","+tc[2]+","+tc[3]+"}\n");
            
            this.h264dsp.h264_h_loop_filter_luma(pix_base, pix_offset,  stride, alpha, beta, tc);
        } else {
            this.h264dsp.h264_h_loop_filter_luma_intra(pix_base, pix_offset, stride, alpha, beta);
        }
    }
    
    public void filter_mb_edgecv( 
			//uint8_t *pix,
			int[] pix_base,
			int pix_offset,
			int stride, 
			//int16_t bS[4],
			int[] bS_base,
			int bS_offset,			
    		int qp) {
//        public void filter_mb_edgecv( uint8_t *pix, int stride, int16_t bS[4], unsigned int qp, H264Context *h ) {
        int index_a = qp + this.slice_alpha_c0_offset;
        int alpha = LoopFilter.alpha_table[index_a];
        int beta  = LoopFilter.beta_table[qp + this.slice_beta_offset];
        if (alpha ==0 || beta == 0) return;

        if( bS_base[bS_offset + 0] < 4 ) {
            //int8_t tc[4];
        	int[] tc = new int[4];
            tc[0] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 0]]+1;
            tc[1] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 1]]+1;
            tc[2] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 2]]+1;
            tc[3] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 3]]+1;
            this.h264dsp.h264_h_loop_filter_chroma(pix_base, pix_offset, stride, alpha, beta, tc);
        } else {
            this.h264dsp.h264_h_loop_filter_chroma_intra(pix_base, pix_offset, stride, alpha, beta);
        }
    }
    
    public void filter_mb_edgeh( 
			//uint8_t *pix,
			int[] pix_base,
			int pix_offset,
			int stride, 
			//int16_t bS[4],
			int[] bS_base,
			int bS_offset,			
    		int qp) {
//        public void filter_mb_edgeh( uint8_t *pix, int stride, int16_t bS[4], unsigned int qp, H264Context *h ) {
        int index_a = qp + this.slice_alpha_c0_offset;
        int alpha = LoopFilter.alpha_table[index_a];
        int beta  = LoopFilter.beta_table[qp + this.slice_beta_offset];
        if (alpha ==0 || beta == 0) return;

        if( bS_base[bS_offset + 0] < 4 ) {
            //int8_t tc[4];
        	int[] tc = new int[4];
            tc[0] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 0]];
            tc[1] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 1]];
            tc[2] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 2]];
            tc[3] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 3]];

            // DebugTool.printDebugString("filter_mb_edgeh: tc={"+tc[0]+","+tc[1]+","+tc[2]+","+tc[3]+"}\n");

            this.h264dsp.h264_v_loop_filter_luma(pix_base, pix_offset, stride, alpha, beta, tc);
        } else {
            this.h264dsp.h264_v_loop_filter_luma_intra(pix_base, pix_offset, stride, alpha, beta);
        }
    }

    public void filter_mb_edgech( 
			//uint8_t *pix,
			int[] pix_base,
			int pix_offset,
			int stride, 
			//int16_t bS[4],
			int[] bS_base,
			int bS_offset,			
    		int qp) {
        //public void filter_mb_edgech( uint8_t *pix, int stride, int16_t bS[4], unsigned int qp, H264Context *h ) {
        int index_a = qp + this.slice_alpha_c0_offset;
        int alpha = LoopFilter.alpha_table[index_a];
        int beta  = LoopFilter.beta_table[qp + this.slice_beta_offset];
        if (alpha ==0 || beta == 0) return;

        if( bS_base[bS_offset + 0] < 4 ) {
            //int8_t tc[4];
        	int[] tc = new int[4];
            tc[0] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 0]]+1;
            tc[1] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 1]]+1;
            tc[2] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 2]]+1;
            tc[3] = LoopFilter.tc0_table[index_a][bS_base[bS_offset + 3]]+1;
            this.h264dsp.h264_v_loop_filter_chroma(pix_base, pix_offset, stride, alpha, beta, tc);
        } else {
            this.h264dsp.h264_v_loop_filter_chroma_intra(pix_base, pix_offset, stride, alpha, beta);
        }
    }

    public void ff_h264_filter_mb_fast( int mb_x, int mb_y, 
			int[] img_y_base, int img_y_offset, 
			int[] img_cb_base, int img_cb_offset, 
			int[] img_cr_base, int img_cr_offset, 
    		int linesize, int uvlinesize) {
        //int mb_xy;
        //int mb_type, left_type;
        //int qp, qp0, qp1, qpc, qpc0, qpc1, qp_thresh;

	    // DebugTool.printDebugString("**** ff_h264_filter_mb_fast called., top_type="+this.top_type+", chroma_qp_diff="+this.pps.chroma_qp_diff+"\n");
        
        //mb_xy = this.mb_xy;

        //if(0==this.top_type || true/*0==this.h264dsp.h264_loop_filter_strength*/ ||this.pps.chroma_qp_diff!=0) {
            ff_h264_filter_mb(mb_x, mb_y, img_y_base, img_y_offset, img_cb_base, img_cb_offset, img_cr_base, img_cr_offset, linesize, uvlinesize);
            return;
        //}
        ////assert(!FRAME_MBAFF);
        /*
        left_type= this.left_type[0];

        mb_type = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
        qp = s.current_picture.qscale_table[mb_xy];
        qp0 = s.current_picture.qscale_table[mb_xy-1];
        qp1 = s.current_picture.qscale_table[this.top_mb_xy];
        qpc = pps.chroma_qp_table[0][qp];
        qpc0 = pps.chroma_qp_table[0][qp0];
        qpc1 = pps.chroma_qp_table[0][qp1];
        qp0 = (qp + qp0 + 1) >> 1;
        qp1 = (qp + qp1 + 1) >> 1;
        qpc0 = (qpc + qpc0 + 1) >> 1;
        qpc1 = (qpc + qpc1 + 1) >> 1;
        qp_thresh = 15+52 - this.slice_alpha_c0_offset;
        if(qp <= qp_thresh && qp0 <= qp_thresh && qp1 <= qp_thresh &&
           qpc <= qp_thresh && qpc0 <= qp_thresh && qpc1 <= qp_thresh)
            return;

        if( (mb_type & 7)!=0 ) {
            //int16_t bS4[4] = {4,4,4,4};
            //int16_t bS3[4] = {3,3,3,3};
            //int16_t *bSH = FIELD_PICTURE ? bS3 : bS4;
        	int[] bS4 = new int[] {4,4,4,4};
        	int[] bS3 = new int[] {3,3,3,3};
        	int[] bSH = ((s.picture_structure != MpegEncContext.PICT_FRAME)? bS3: bS4);
        	if(left_type!=0)
                filter_mb_edgev( img_y_base, img_y_offset + 4*0, linesize, bS4, 0, qp0);
            if( (mb_type & MB_TYPE_8x8DCT) !=0) {
                filter_mb_edgev( img_y_base, img_y_offset + 4*2, linesize, bS3, 0, qp);
                filter_mb_edgeh( img_y_base, img_y_offset + 4*0*linesize, linesize, bSH, 0, qp1);
                filter_mb_edgeh( img_y_base, img_y_offset + 4*2*linesize, linesize, bS3, 0, qp);
            } else {
                filter_mb_edgev( img_y_base, img_y_offset + 4*1, linesize, bS3, 0, qp);
                filter_mb_edgev( img_y_base, img_y_offset + 4*2, linesize, bS3, 0, qp);
                filter_mb_edgev( img_y_base, img_y_offset + 4*3, linesize, bS3, 0, qp);
                filter_mb_edgeh( img_y_base, img_y_offset + 4*0*linesize, linesize, bSH, 0, qp1);
                filter_mb_edgeh( img_y_base, img_y_offset + 4*1*linesize, linesize, bS3, 0, qp);
                filter_mb_edgeh( img_y_base, img_y_offset + 4*2*linesize, linesize, bS3, 0, qp);
                filter_mb_edgeh( img_y_base, img_y_offset + 4*3*linesize, linesize, bS3, 0, qp);
            }
            if(left_type!=0){
                filter_mb_edgecv( img_cb_base, img_cb_offset + 2*0, uvlinesize, bS4, 0, qpc0);
                filter_mb_edgecv( img_cr_base, img_cr_offset + 2*0, uvlinesize, bS4, 0, qpc0);
            }
            filter_mb_edgecv( img_cb_base, img_cb_offset + 2*2, uvlinesize, bS3, 0, qpc);
            filter_mb_edgecv( img_cr_base, img_cr_offset + 2*2, uvlinesize, bS3, 0, qpc);
            filter_mb_edgech( img_cb_base, img_cb_offset + 2*0*uvlinesize, uvlinesize, bSH, 0, qpc1);
            filter_mb_edgech( img_cb_base, img_cb_offset + 2*2*uvlinesize, uvlinesize, bS3, 0, qpc);
            filter_mb_edgech( img_cr_base, img_cr_offset + 2*0*uvlinesize, uvlinesize, bSH, 0, qpc1);
            filter_mb_edgech( img_cr_base, img_cr_offset + 2*2*uvlinesize, uvlinesize, bS3, 0, qpc);
            return;
        } else {
            //LOCAL_ALIGNED_8(int16_t, bS, [2], [4][4]);
        	int[][][] bS = new int[2][4][4];
            int edges;
            if( (mb_type & MB_TYPE_8x8DCT) !=0 && (this.cbp&7) == 7 ) {
                edges = 4;
                for(int i=0;i<4;i++) {
                	bS[0][0][i] = 0x0002;
                	bS[0][2][i] = 0x0002;
                	bS[1][0][i] = 0x0002;
                	bS[1][2][i] = 0x0002;
                } // for
            } else {
                int mask_edge1 = (3*(((5*mb_type)>>5)&1)) | (mb_type>>4); //(mb_type & (MB_TYPE_16x16 | MB_TYPE_8x16)) ? 3 : (mb_type & MB_TYPE_16x8) ? 1 : 0;
                int mask_edge0 = 3*((mask_edge1>>1) & ((5*left_type)>>5)&1); // (mb_type & (MB_TYPE_16x16 | MB_TYPE_8x16)) && (this.left_type[0] & (MB_TYPE_16x16 | MB_TYPE_8x16)) ? 3 : 0;
                int step =  1+(mb_type>>24); //IS_8x8DCT(mb_type) ? 2 : 1;
                edges = 4 - 3*((mb_type>>3) & ((0==(this.cbp & 15))?1:0)); //(mb_type & MB_TYPE_16x16) && !(this.cbp & 15) ? 1 : 4;
                this.h264dsp.h264_loop_filter_strength( bS, this.non_zero_count_cache, this.ref_cache, this.mv_cache,
                                                  ((this.list_count==2)?1:0), edges, step, mask_edge0, mask_edge1, ((s.picture_structure != MpegEncContext.PICT_FRAME)?1:0));
            }
            if( (left_type & 7) != 0 )
            	for(int i=0;i<4;i++) {
            		bS[0][0][i] = 0x0004;
            	} // for
            if( (this.top_type & 7) !=0) {
                //AV_WN64A(bS[1][0], FIELD_PICTURE ? 0x0003000300030003ULL : 0x0004000400040004ULL);
            	if((s.picture_structure != MpegEncContext.PICT_FRAME)) {            	
	            	for(int i=0;i<4;i++) {
	            		bS[1][0][i] = 0x0003;
	            	} // for
            	} else {
	            	for(int i=0;i<4;i++) {
	            		bS[1][0][i] = 0x0004;
	            	} // for            		
            	} // if
            } // if
            if(left_type!=0) {
                //FILTER(v,0,0);
        		for(int p=0;p<4;p++) {
        			if(bS[0][0][p] !=0) {
    	                filter_mb_edgev( img_y_base, img_y_offset + 4*0*((0!=0)?linesize:1), linesize, bS[0][0], 0, (0!=0) ? qp : qp0);
    	                if(0==(0&1)) {
    	                    filter_mb_edgecv( img_cb_base, img_cb_offset + 2*0*((0!=0)?uvlinesize:1), uvlinesize, bS[0][0], 0, (0!=0)?qpc:qpc0);
    	                    filter_mb_edgecv( img_cr_base, img_cr_offset + 2*0*((0!=0)?uvlinesize:1), uvlinesize, bS[0][0], 0, (0!=0)?qpc:qpc0);
    	                } // if
        				break;
        			} // if
        		} // for
            } if( edges == 1 ) {
                //FILTER(h,1,0);
        		for(int p=0;p<4;p++) {
        			if(bS[1][0][p] !=0) {
    	                filter_mb_edgeh( img_y_base, img_y_offset + 4*0*((1!=0)?linesize:1), linesize, bS[1][0], 0, (0!=0) ? qp : qp1);
    	                if(0==(0&1)) {
    	                    filter_mb_edgech( img_cb_base, img_cb_offset + 2*0*((1!=0)?uvlinesize:1), uvlinesize, bS[1][0], 0, (0!=0)?qpc:qpc1);
    	                    filter_mb_edgech( img_cr_base, img_cr_offset + 2*0*((1!=0)?uvlinesize:1), uvlinesize, bS[1][0], 0, (0!=0)?qpc:qpc1);
    	                } // if
        				break;
        			} // if
        		} // for
        	} else if( (mb_type & MB_TYPE_8x8DCT) !=0) {
                //FILTER(v,0,2);
				for(int p=0;p<4;p++) {
					if(bS[0][2][p] !=0) {
			            filter_mb_edgev( img_y_base, img_y_offset + 4*2*((0!=0)?linesize:1), linesize, bS[0][2], 0, (2!=0) ? qp : qp0);
			            if(0==(2&1)) {
			                filter_mb_edgecv( img_cb_base, img_cb_offset + 2*2*((0!=0)?uvlinesize:1), uvlinesize, bS[0][2], 0, (2!=0)?qpc:qpc0);
			                filter_mb_edgecv( img_cr_base, img_cr_offset + 2*2*((0!=0)?uvlinesize:1), uvlinesize, bS[0][2], 0, (2!=0)?qpc:qpc0);
			            } // if
						break;
					} // if
				} // for    	   
                //FILTER(h,1,0);
				for(int p=0;p<4;p++) {
					if(bS[1][0][p] !=0) {
			            filter_mb_edgeh( img_y_base, img_y_offset + 4*0*((1!=0)?linesize:1), linesize, bS[1][0], 0, (0!=0) ? qp : qp1);
			            if(0==(0&1)) {
			                filter_mb_edgech( img_cb_base, img_cb_offset + 2*0*((1!=0)?uvlinesize:1), uvlinesize, bS[1][0], 0, (0!=0)?qpc:qpc1);
			                filter_mb_edgech( img_cr_base, img_cr_offset + 2*0*((1!=0)?uvlinesize:1), uvlinesize, bS[1][0], 0, (0!=0)?qpc:qpc1);
			            } // if
						break;
					} // if
				} // for
                //FILTER(h,1,2);
				for(int p=0;p<4;p++) {
					if(bS[1][2][p] !=0) {
			            filter_mb_edgeh( img_y_base, img_y_offset + 4*2*((1!=0)?linesize:1), linesize, bS[1][2], 0, (2!=0) ? qp : qp1);
			            if(0==(2&1)) {
			                filter_mb_edgech( img_cb_base, img_cb_offset + 2*2*((1!=0)?uvlinesize:1), uvlinesize, bS[1][2], 0, (2!=0)?qpc:qpc1);
			                filter_mb_edgech( img_cr_base, img_cr_offset + 2*2*((1!=0)?uvlinesize:1), uvlinesize, bS[1][2], 0, (2!=0)?qpc:qpc1);
			            } // if
						break;
					} // if
				} // for
            } else {
            	for(int edge=1;edge<4;edge++) {
                    //FILTER(v,0,edge);
    				for(int p=0;p<4;p++) {
    					if(bS[0][edge][p] !=0) {
    			            filter_mb_edgev( img_y_base, img_y_offset + 4*edge*((0!=0)?linesize:1), linesize, bS[0][edge], 0, (edge!=0) ? qp : qp0);
    			            if(0==(edge&1)) {
    			                filter_mb_edgecv( img_cb_base, img_cb_offset + 2*edge*((0!=0)?uvlinesize:1), uvlinesize, bS[0][edge], 0, (edge!=0)?qpc:qpc0);
    			                filter_mb_edgecv( img_cr_base, img_cr_offset + 2*edge*((0!=0)?uvlinesize:1), uvlinesize, bS[0][edge], 0, (edge!=0)?qpc:qpc0);
    			            } // if
    						break;
    					} // if
    				} // for
            	} // for edge
            	for(int edge=0;edge<4;edge++) {
                    //FILTER(h,1,edge);            		
    				for(int p=0;p<4;p++) {
    					if(bS[1][edge][p] !=0) {
    			            filter_mb_edgeh( img_y_base, img_y_offset + 4*edge*((1!=0)?linesize:1), linesize, bS[1][edge], 0, (edge!=0) ? qp : qp1);
    			            if(0==(edge&1)) {
    			                filter_mb_edgech( img_cb_base, img_cb_offset + 2*edge*((1!=0)?uvlinesize:1), uvlinesize, bS[1][edge], 0, (edge!=0)?qpc:qpc1);
    			                filter_mb_edgech( img_cr_base, img_cr_offset + 2*edge*((1!=0)?uvlinesize:1), uvlinesize, bS[1][edge], 0, (edge!=0)?qpc:qpc1);
    			            } // if
    						break;
    					} // if
    				} // for
            	} // for edge
            }
//    #undef FILTER
        }
        */
    }

    public int check_mv(int b_idx, int bn_idx, int mvy_limit){
        int v;
        
        v= ((this.ref_cache[0][b_idx] != this.ref_cache[0][bn_idx])?1:0);
        //// DebugTool.printDebugString(" -- v(1) = "+v+"\n");
        
        if(0==v && this.ref_cache[0][b_idx]!=-1)
            v= (( (((long)(this.mv_cache[0][b_idx][0] - this.mv_cache[0][bn_idx][0] + 3))&0x000000000000ffffl) >= 7l)?1:0) |
               ((Math.abs( this.mv_cache[0][b_idx][1] - this.mv_cache[0][bn_idx][1] ) >= mvy_limit)?1:0);
            // ??????????????????????????????
        //// DebugTool.printDebugString(" -- v(2) = "+v
        //		+", this.ref_cache[0][b_idx]="+(this.ref_cache[0][b_idx])
        //		+", this.mv_cache[0][b_idx][0]="+(this.mv_cache[0][b_idx][0])
        //		+", this.mv_cache[0][bn_idx][0]="+(this.mv_cache[0][bn_idx][0])
        //		+", this.mv_cache[0][b_idx][1]="+(this.mv_cache[0][b_idx][1])
        //		+", this.mv_cache[0][bn_idx][1]="+(this.mv_cache[0][bn_idx][1])
        //		+"\n");

        if(this.list_count==2){
            if(0==v)
                v = ((this.ref_cache[1][b_idx] != this.ref_cache[1][bn_idx])?1:0) |
                    ( ( ( ((long)(this.mv_cache[1][b_idx][0] - this.mv_cache[1][bn_idx][0] + 3)) & 0x00ffffl) >= 7l) ?1:0) |
                    ((Math.abs( this.mv_cache[1][b_idx][1] - this.mv_cache[1][bn_idx][1] ) >= mvy_limit)?1:0);

            //// DebugTool.printDebugString(" -- v(3) = "+v+"\n");

            if(0!=v){
                if(this.ref_cache[0][b_idx] != this.ref_cache[1][bn_idx] |
                   this.ref_cache[1][b_idx] != this.ref_cache[0][bn_idx])
                    return 1;
                return
                    (( (((long)(this.mv_cache[0][b_idx][0] - this.mv_cache[1][bn_idx][0] + 3)) & 0x00ffffl) >= 7l)?1:0) |
                    ((Math.abs( this.mv_cache[0][b_idx][1] - this.mv_cache[1][bn_idx][1] ) >= mvy_limit)?1:0) |
                    (( (((long)(this.mv_cache[1][b_idx][0] - this.mv_cache[0][bn_idx][0] + 3)) & 0x00ffffl) >= 7l)?1:0) |
                    ((Math.abs( this.mv_cache[1][b_idx][1] - this.mv_cache[0][bn_idx][1] ) >= mvy_limit)?1:0);
            }
        }

        // DebugTool.printDebugString("check_mv("+b_idx+","+bn_idx+","+mvy_limit+") = "+ v +"\n");
        return v;
    }
    
	public void filter_mb_mbaff_edgev( 
			//uint8_t *pix,
			int[] pix_base,
			int pix_offset,
			int stride, 
			//int16_t bS[4],
			int[] bS_base,
			int bS_offset,			
			int bsi, int qp ) {
	    int i;
	    int index_a = qp + this.slice_alpha_c0_offset;
	    int alpha = LoopFilter.alpha_table[index_a];
	    int beta  = LoopFilter.beta_table[qp + this.slice_beta_offset];
	    for( i = 0; i < 8; i++, pix_offset += stride) {
	        int bS_index = (i >> 1) * bsi;

	        if( bS_base[bS_offset + bS_index] == 0 ) {
	            continue;
	        }

	        if( bS_base[bS_offset + bS_index] < 4 ) {
	            int tc0 = LoopFilter.tc0_table[index_a][bS_base[bS_offset + bS_index]];
	            int p0 = pix_base[pix_offset + -1];
	            int p1 = pix_base[pix_offset + -2];
	            int p2 = pix_base[pix_offset + -3];
	            int q0 = pix_base[pix_offset + 0];
	            int q1 = pix_base[pix_offset + 1];
	            int q2 = pix_base[pix_offset + 2];

	            if( Math.abs( p0 - q0 ) < alpha &&
	                Math.abs( p1 - p0 ) < beta &&
	                Math.abs( q1 - q0 ) < beta ) {
	                int tc = tc0;
	                int i_delta;

	                if( Math.abs( p2 - p0 ) < beta ) {
	                    if(tc0!=0)
	                    pix_base[pix_offset + -2] = p1 + av_clip( ( p2 + ( ( p0 + q0 + 1 ) >> 1 ) - ( p1 << 1 ) ) >> 1, -tc0, tc0 );
	                    tc++;
	                }
	                if( Math.abs( q2 - q0 ) < beta ) {
	                    if(tc0!=0)
	                    pix_base[pix_offset + 1] = q1 + av_clip( ( q2 + ( ( p0 + q0 + 1 ) >> 1 ) - ( q1 << 1 ) ) >> 1, -tc0, tc0 );
	                    tc++;
	                }

	                i_delta = av_clip( (((q0 - p0 ) << 2) + (p1 - q1) + 4) >> 3, -tc, tc );
	                pix_base[pix_offset + -1] = av_clip_uint8( p0 + i_delta );    /* p0' */
	                pix_base[pix_offset + 0]  = av_clip_uint8( q0 - i_delta );    /* q0' */
	                //tprintf(this.s.avctx, "filter_mb_mbaff_edgev i:%d, qp:%d, indexA:%d, alpha:%d, beta:%d, tc:%d\n# bS:%d -> [%02x, %02x, %02x, %02x, %02x, %02x] =>[%02x, %02x, %02x, %02x]\n", i, qp[qp_index], index_a, alpha, beta, tc, bS_base[bS_offset + bS_index], pix_base[pix_offset + -3], p1, p0, q0, q1, pix_base[pix_offset + 2], p1, pix_base[pix_offset + -1], pix_base[pix_offset + 0], q1);
	            }
	        }else{
	            int p0 = pix_base[pix_offset + -1];
	            int p1 = pix_base[pix_offset + -2];
	            int p2 = pix_base[pix_offset + -3];

	            int q0 = pix_base[pix_offset + 0];
	            int q1 = pix_base[pix_offset + 1];
	            int q2 = pix_base[pix_offset + 2];

	            if( Math.abs( p0 - q0 ) < alpha &&
	                Math.abs( p1 - p0 ) < beta &&
	                Math.abs( q1 - q0 ) < beta ) {

	                if(Math.abs( p0 - q0 ) < (( alpha >> 2 ) + 2 )){
	                    if( Math.abs( p2 - p0 ) < beta)
	                    {
	                        int p3 = pix_base[pix_offset + -4];
	                        /* p0', p1', p2' */
	                        pix_base[pix_offset + -1] = ( p2 + 2*p1 + 2*p0 + 2*q0 + q1 + 4 ) >> 3;
	                        pix_base[pix_offset + -2] = ( p2 + p1 + p0 + q0 + 2 ) >> 2;
	                        pix_base[pix_offset + -3] = ( 2*p3 + 3*p2 + p1 + p0 + q0 + 4 ) >> 3;
	                    } else {
	                        /* p0' */
	                        pix_base[pix_offset + -1] = ( 2*p1 + p0 + q1 + 2 ) >> 2;
	                    }
	                    if( Math.abs( q2 - q0 ) < beta)
	                    {
	                        int q3 = pix_base[pix_offset + 3];
	                        /* q0', q1', q2' */
	                        pix_base[pix_offset + 0] = ( p1 + 2*p0 + 2*q0 + 2*q1 + q2 + 4 ) >> 3;
	                        pix_base[pix_offset + 1] = ( p0 + q0 + q1 + q2 + 2 ) >> 2;
	                        pix_base[pix_offset + 2] = ( 2*q3 + 3*q2 + q1 + q0 + p0 + 4 ) >> 3;
	                    } else {
	                        /* q0' */
	                        pix_base[pix_offset + 0] = ( 2*q1 + q0 + p1 + 2 ) >> 2;
	                    }
	                }else{
	                    /* p0', q0' */
	                    pix_base[pix_offset + -1] = ( 2*p1 + p0 + q1 + 2 ) >> 2;
	                    pix_base[pix_offset +  0] = ( 2*q1 + q0 + p1 + 2 ) >> 2;
	                }
	                //tprintf(this.s.avctx, "filter_mb_mbaff_edgev i:%d, qp:%d, indexA:%d, alpha:%d, beta:%d\n# bS:4 -> [%02x, %02x, %02x, %02x, %02x, %02x] =>[%02x, %02x, %02x, %02x, %02x, %02x]\n", i, qp[qp_index], index_a, alpha, beta, p2, p1, p0, q0, q1, q2, pix_base[pix_offset + -3], pix_base[pix_offset + -2], pix_base[pix_offset + -1], pix_base[pix_offset + 0], pix_base[pix_offset + 1], pix_base[pix_offset + 2]);
	            }
	        }
	    }
	}
	public void filter_mb_mbaff_edgecv( 
			//uint8_t *pix,
			int[] pix_base,
			int pix_offset,
			int stride, 
			//int16_t bS[4],
			int[] bS_base,
			int bS_offset,			
			int bsi, int qp ) {
	    int i;
	    int index_a = qp + this.slice_alpha_c0_offset;
	    int alpha = LoopFilter.alpha_table[index_a];
	    int beta  = LoopFilter.beta_table[qp + this.slice_beta_offset];
	    for( i = 0; i < 4; i++, pix_offset += stride) {
	        int bS_index = i*bsi;

	        if( bS_base[bS_offset + bS_index] == 0 ) {
	            continue;
	        }

	        if( bS_base[bS_offset + bS_index] < 4 ) {
	            int tc = LoopFilter.tc0_table[index_a][bS_base[bS_offset + bS_index]] + 1;
	            int p0 = pix_base[pix_offset + -1];
	            int p1 = pix_base[pix_offset + -2];
	            int q0 = pix_base[pix_offset + 0];
	            int q1 = pix_base[pix_offset + 1];

	            if( Math.abs( p0 - q0 ) < alpha &&
	                Math.abs( p1 - p0 ) < beta &&
	                Math.abs( q1 - q0 ) < beta ) {
	                int i_delta = av_clip( (((q0 - p0 ) << 2) + (p1 - q1) + 4) >> 3, -tc, tc );

	                pix_base[pix_offset + -1] = av_clip_uint8( p0 + i_delta );    /* p0' */
	                pix_base[pix_offset + 0]  = av_clip_uint8( q0 - i_delta );    /* q0' */
	                //tprintf(this.s.avctx, "filter_mb_mbaff_edgecv i:%d, qp:%d, indexA:%d, alpha:%d, beta:%d, tc:%d\n# bS:%d -> [%02x, %02x, %02x, %02x, %02x, %02x] =>[%02x, %02x, %02x, %02x]\n", i, qp[qp_index], index_a, alpha, beta, tc, bS_base[bS_offset + bS_index], pix_base[pix_offset + -3], p1, p0, q0, q1, pix_base[pix_offset + 2], p1, pix_base[pix_offset + -1], pix_base[pix_offset + 0], q1);
	            }
	        }else{
	            int p0 = pix_base[pix_offset + -1];
	            int p1 = pix_base[pix_offset + -2];
	            int q0 = pix_base[pix_offset + 0];
	            int q1 = pix_base[pix_offset + 1];

	            if( Math.abs( p0 - q0 ) < alpha &&
	                Math.abs( p1 - p0 ) < beta &&
	                Math.abs( q1 - q0 ) < beta ) {

	                pix_base[pix_offset + -1] = ( 2*p1 + p0 + q1 + 2 ) >> 2;   /* p0' */
	                pix_base[pix_offset + 0]  = ( 2*q1 + q0 + p1 + 2 ) >> 2;   /* q0' */
	                //tprintf(this.s.avctx, "filter_mb_mbaff_edgecv i:%d\n# bS:4 -> [%02x, %02x, %02x, %02x, %02x, %02x] =>[%02x, %02x, %02x, %02x, %02x, %02x]\n", i, pix_base[pix_offset + -3], p1, p0, q0, q1, pix_base[pix_offset + 2], pix_base[pix_offset + -3], pix_base[pix_offset + -2], pix_base[pix_offset + -1], pix_base[pix_offset + 0], pix_base[pix_offset + 1], pix_base[pix_offset + 2]);
	            }
	        }
	    }
	}
	
	public void filter_mb_dir(int mb_x, int mb_y, 
			int[] img_y_base, int img_y_offset, 
			int[] img_cb_base, int img_cb_offset, 
			int[] img_cr_base, int img_cr_offset, 
			int linesize, int uvlinesize, int mb_xy, int mb_type, int mvy_limit, int first_vertical_edge_done, int dir) {
	    int edge;
	    int mbm_xy = dir == 0 ? mb_xy -1 : this.top_mb_xy;
	    int mbm_type = dir == 0 ? this.left_type[0] : this.top_type;

	    // how often to recheck mv-based bS when iterating between edges
	    //static uint8_t mask_edge_tab[2][8]={{0,3,3,3,1,1,1,1},
	    //                                          {0,3,1,1,3,3,3,3}};
	    final int[][] mask_edge_tab ={{0,3,3,3,1,1,1,1},
	                                              {0,3,1,1,3,3,3,3}};
	    int mask_edge = mask_edge_tab[dir][(mb_type>>3)&7];
	    int edges = ((mask_edge== 3 && (0==(this.cbp&15)) )? 1 : 4);

	    // how often to recheck mv-based bS when iterating along each edge
	    int mask_par0 = mb_type & (MB_TYPE_16x16 | (MB_TYPE_8x16 >> dir));

	    if(mbm_type !=0 && 0==first_vertical_edge_done){

	        if (0!=mb_aff_frame && (dir == 1) && ((mb_y&1) == 0)
	            && 0!=(mbm_type&~mb_type & MB_TYPE_INTERLACED)
	            ) {
	            // This is a special case in the norm where the filtering must
	            // be done twice (one each of the field) even if we are in a
	            // frame macroblock.
	            //
	            int tmp_linesize   = 2 *   linesize;
	            int tmp_uvlinesize = 2 * uvlinesize;
	            int mbn_xy = mb_xy - 2 * s.mb_stride;
	            int j;

	            for(j=0; j<2; j++, mbn_xy += s.mb_stride){
	                //DECLARE_ALIGNED(8, int16_t, bS)[4];
	            	int[] bS_base = new int[4];
	            	int bS_offset = 0;
	                int qp;
	                if( 0!=(7&(mb_type|s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mbn_xy])) ) {
	                	for(int k=0;k<4;k++)
	                		bS_base[bS_offset + k] = 0x0003;
	                    //AV_WN64A(bS, 0x0003000300030003ULL);
	                } else {
	                    if(0==pps.cabac && 0!=(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mbn_xy] & MB_TYPE_8x8DCT)){
	                        bS_base[bS_offset + 0]= 1+(((this.cbp_table[mbn_xy] & 4)!=0||this.non_zero_count_cache[scan8[0]+0]!=0)?1:0);
	                        bS_base[bS_offset + 1]= 1+(((this.cbp_table[mbn_xy] & 4)!=0||this.non_zero_count_cache[scan8[0]+1]!=0)?1:0);
	                        bS_base[bS_offset + 2]= 1+(((this.cbp_table[mbn_xy] & 8)!=0||this.non_zero_count_cache[scan8[0]+2]!=0)?1:0);
	                        bS_base[bS_offset + 3]= 1+(((this.cbp_table[mbn_xy] & 8)!=0||this.non_zero_count_cache[scan8[0]+3]!=0)?1:0);
	                    }else{
	                    //uint8_t *mbn_nnz = this.non_zero_count[mbn_xy] + 4+3*8;
		                int[] mbn_nnz_base = this.non_zero_count[mbn_xy];
		                int mbn_nnz_offset = 4+3*8;
	                    int i;
	                    for( i = 0; i < 4; i++ ) {
	                        bS_base[bS_offset + i] = 1 + ((this.non_zero_count_cache[scan8[0]+i] | mbn_nnz_base[mbn_nnz_offset + i])!=0?1:0);
	                    }
	                    }
	                }
	                // Do not use s.qscale as luma quantizer because it has not the same
	                // value in IPCM macroblocks.
	                qp = ( s.current_picture.qscale_table[mb_xy] + s.current_picture.qscale_table[mbn_xy] + 1 ) >> 1;
	                //tprintf(s.avctx, "filter mb:%d/%d dir:%d edge:%d, QPy:%d ls:%d uvls:%d", mb_x, mb_y, dir, edge, qp, tmp_linesize, tmp_uvlinesize);
	                //{ int i; for (i = 0; i < 4; i++) tprintf(s.avctx, " bS_base[bS_offset + %d]:%d", i, bS_base[bS_offset + i]); tprintf(s.avctx, "\n"); }
	                filter_mb_edgeh( img_y_base, img_y_offset + j*linesize, tmp_linesize, bS_base, bS_offset, qp);
	                filter_mb_edgech( img_cb_base, img_cb_offset + j*uvlinesize, tmp_uvlinesize, bS_base, bS_offset,
	                                ( this.chroma_qp[0] + pps.chroma_qp_table[0][s.current_picture.qscale_table[mbn_xy]] + 1 ) >> 1);
	                filter_mb_edgech( img_cr_base, img_cr_offset + j*uvlinesize, tmp_uvlinesize, bS_base, bS_offset,
	                                ( this.chroma_qp[1] + pps.chroma_qp_table[1][s.current_picture.qscale_table[mbn_xy]] + 1 ) >> 1);
	            }
	        }else{
            	int[] bS_base = new int[4];
            	int bS_offset = 0;
	            int qp;

	            if( 0!= (7&(mb_type|mbm_type))) {
	                //AV_WN64A(bS, 0x0003000300030003ULL);
                	for(int k=0;k<4;k++)
                		bS_base[bS_offset + k] = 0x0003;

	                if (   (0==((mb_type|mbm_type)&MB_TYPE_INTERLACED))
	                    || ((0!=mb_aff_frame || (s.picture_structure != MpegEncContext.PICT_FRAME)) && (dir == 0))
	                )
	                    //AV_WN64A(bS, 0x0004000400040004ULL);
	                	for(int k=0;k<4;k++)
	                		bS_base[bS_offset + k] = 0x0004;
	            } else {
	                int i;
	                int mv_done;

	                if( dir!=0 && mb_aff_frame!=0 && ((mb_type ^ mbm_type)&MB_TYPE_INTERLACED)!=0) {
	                    //AV_WN64A(bS, 0x0001000100010001ULL);
	                	for(int k=0;k<4;k++)
	                		bS_base[bS_offset + k] = 0x0001;
	                    mv_done = 1;
	                }
	                else if( mask_par0!=0 && ((mbm_type & (MB_TYPE_16x16 | (MB_TYPE_8x16 >> dir))))!=0 ) {
	                    int b_idx= 8 + 4;
	                    int bn_idx= b_idx - (dir!=0 ? 8:1);

	                    bS_base[bS_offset + 0] = bS_base[bS_offset + 1] = bS_base[bS_offset + 2] = bS_base[bS_offset + 3] = check_mv(8 + 4, bn_idx, mvy_limit);
	                    mv_done = 1;
	                }
	                else
	                    mv_done = 0;

	                for( i = 0; i < 4; i++ ) {
	                    int x = dir == 0 ? 0 : i;
	                    int y = dir == 0 ? i    : 0;
	                    int b_idx= 8 + 4 + x + 8*y;
	                    int bn_idx= b_idx - (dir!=0 ? 8:1);

	                    if( (this.non_zero_count_cache[b_idx] |
	                        this.non_zero_count_cache[bn_idx]) !=0) {
	                        bS_base[bS_offset + i] = 2;
	                    }
	                    else if(0==mv_done)
	                    {
	                        bS_base[bS_offset + i] = check_mv(b_idx, bn_idx, mvy_limit);
	                    }
	                }
	            }

	            /* Filter edge */
	            // Do not use s.qscale as luma quantizer because it has not the same
	            // value in IPCM macroblocks.
	            if(0!=(bS_base[bS_offset + 0]+bS_base[bS_offset + 1]+bS_base[bS_offset + 2]+bS_base[bS_offset + 3])){
	                qp = ( s.current_picture.qscale_table[mb_xy] + s.current_picture.qscale_table[mbm_xy] + 1 ) >> 1;
	                //tprintf(s.avctx, "filter mb:%d/%d dir:%d edge:%d, QPy:%d, QPc:%d, QPcn:%d\n", mb_x, mb_y, dir, edge, qp, this.chroma_qp[0], s.current_picture.qscale_table[mbn_xy]);
	                //tprintf(s.avctx, "filter mb:%d/%d dir:%d edge:%d, QPy:%d ls:%d uvls:%d", mb_x, mb_y, dir, edge, qp, linesize, uvlinesize);
	                //{ int i; for (i = 0; i < 4; i++) tprintf(s.avctx, " bS_base[bS_offset + %d]:%d", i, bS_base[bS_offset + i]); tprintf(s.avctx, "\n"); }
	                if( dir == 0 ) {
	                    filter_mb_edgev( img_y_base, img_y_offset + 0, linesize, bS_base, bS_offset, qp);
	                    {
	                        qp= ( this.chroma_qp[0] + pps.chroma_qp_table[0][s.current_picture.qscale_table[mbm_xy]] + 1 ) >> 1;
	                        filter_mb_edgecv( img_cb_base, img_cb_offset + 0, uvlinesize, bS_base, bS_offset, qp);
	                        if(0!=this.pps.chroma_qp_diff)
	                            qp= ( this.chroma_qp[1] + pps.chroma_qp_table[1][s.current_picture.qscale_table[mbm_xy]] + 1 ) >> 1;
	                        filter_mb_edgecv( img_cr_base, img_cr_offset + 0, uvlinesize, bS_base, bS_offset, qp);
	                    }
	                } else {
	                    filter_mb_edgeh( img_y_base, img_y_offset + 0, linesize, bS_base, bS_offset, qp);
	                    {
	                        qp= ( this.chroma_qp[0] + pps.chroma_qp_table[0][s.current_picture.qscale_table[mbm_xy]] + 1 ) >> 1;
	                        filter_mb_edgech( img_cb_base, img_cb_offset + 0, uvlinesize, bS_base, bS_offset, qp);
	                        if(this.pps.chroma_qp_diff!=0)
	                            qp= ( this.chroma_qp[1] + pps.chroma_qp_table[1][s.current_picture.qscale_table[mbm_xy]] + 1 ) >> 1;
	                        filter_mb_edgech( img_cr_base, img_cr_offset + 0, uvlinesize, bS_base, bS_offset, qp);
	                    }
	                }
	            }
	        }
	    }

	    /* Calculate bS */
	    for( edge = 1; edge < edges; edge++ ) {
        	int[] bS_base = new int[4];
        	int bS_offset = 0;
	        int qp;

	        if( 0!= ((mb_type & (edge<<24)&MB_TYPE_8x8DCT)) ) // (edge&1) && IS_8x8DCT(mb_type)
	            continue;

	        if( 0!=(mb_type & 7)) {
	            //AV_WN64A(bS, 0x0003000300030003ULL);
            	for(int k=0;k<4;k++)
            		bS_base[bS_offset + k] = 0x0003;
	        } else {
	            int i;
	            int mv_done;

	            if( 0!=(edge & mask_edge) ) {
	                //AV_ZERO64(bS);
                	for(int k=0;k<4;k++)
                		bS_base[bS_offset + k] = 0x0000;	            	
	                mv_done = 1;
	            }
	            else if( 0!=mask_par0 ) {
	                int b_idx= 8 + 4 + edge * (0!=dir ? 8:1);
	                int bn_idx= b_idx - (0!=dir ? 8:1);

	                bS_base[bS_offset + 0] = bS_base[bS_offset + 1] = bS_base[bS_offset + 2] = bS_base[bS_offset + 3] = check_mv(b_idx, bn_idx, mvy_limit);
	                mv_done = 1;
	            }
	            else
	                mv_done = 0;

	            for( i = 0; i < 4; i++ ) {
	                int x = dir == 0 ? edge : i;
	                int y = dir == 0 ? i    : edge;
	                int b_idx= 8 + 4 + x + 8*y;
	                int bn_idx= b_idx - (0!=dir ? 8:1);

	                if( 0!=(this.non_zero_count_cache[b_idx] |
	                    this.non_zero_count_cache[bn_idx]) ) {
	                    bS_base[bS_offset + i] = 2;
	                }
	                else if(0==mv_done)
	                {
	                    bS_base[bS_offset + i] = check_mv(b_idx, bn_idx, mvy_limit);
	                }
	            }

	            if(bS_base[bS_offset + 0]+bS_base[bS_offset + 1]+bS_base[bS_offset + 2]+bS_base[bS_offset + 3] == 0)
	                continue;
	        }

	        /* Filter edge */
	        // Do not use s.qscale as luma quantizer because it has not the same
	        // value in IPCM macroblocks.
	        qp = s.current_picture.qscale_table[mb_xy];
	        //tprintf(s.avctx, "filter mb:%d/%d dir:%d edge:%d, QPy:%d, QPc:%d, QPcn:%d\n", mb_x, mb_y, dir, edge, qp, this.chroma_qp[0], s.current_picture.qscale_table[mbn_xy]);
	        //tprintf(s.avctx, "filter mb:%d/%d dir:%d edge:%d, QPy:%d ls:%d uvls:%d", mb_x, mb_y, dir, edge, qp, linesize, uvlinesize);
	        //{ int i; for (i = 0; i < 4; i++) tprintf(s.avctx, " bS_base[bS_offset + %d]:%d", i, bS_base[bS_offset + i]); tprintf(s.avctx, "\n"); }
	        if( dir == 0 ) {
	            filter_mb_edgev( img_y_base, img_y_offset + 4*edge, linesize, bS_base, bS_offset, qp);
	            if( (edge&1) == 0 ) {
	                filter_mb_edgecv( img_cb_base, img_cb_offset + 2*edge, uvlinesize, bS_base, bS_offset, this.chroma_qp[0]);
	                filter_mb_edgecv( img_cr_base, img_cr_offset + 2*edge, uvlinesize, bS_base, bS_offset, this.chroma_qp[1]);
	            }
	        } else {
	            filter_mb_edgeh( img_y_base, img_y_offset + 4*edge*linesize, linesize, bS_base, bS_offset, qp);
	            if( (edge&1) == 0 ) {
	                filter_mb_edgech( img_cb_base, img_cb_offset + 2*edge*uvlinesize, uvlinesize, bS_base, bS_offset, this.chroma_qp[0]);
	                filter_mb_edgech( img_cr_base, img_cr_offset + 2*edge*uvlinesize, uvlinesize, bS_base, bS_offset, this.chroma_qp[1]);
	            }
	        }
	    }
	}
		
//	public void ff_h264_filter_mb(int mb_x, int mb_y, uint8_t *img_y, uint8_t *img_cb, uint8_t *img_cr, unsigned int linesize, unsigned int uvlinesize) {
	public void ff_h264_filter_mb(int mb_x, int mb_y, 
			int[] img_y_base, int img_y_offset, 
			int[] img_cb_base, int img_cb_offset, 
			int[] img_cr_base, int img_cr_offset, 
			int linesize, 
			int uvlinesize) {
	    int mb_xy= mb_x + mb_y*s.mb_stride;
	    int mb_type = (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
	    int mvy_limit = ((mb_type&MB_TYPE_INTERLACED) !=0? 2 : 4);
	    int first_vertical_edge_done = 0;
	    int dir;
	    
	    // DebugTool.printDebugString("**** ff_h264_filter_mb called.\n");

	    if (mb_aff_frame !=0
	            // and current and left pair do not have the same interlaced type
	            && 0!= ((mb_type^this.left_type[0])&MB_TYPE_INTERLACED)
	            // and left mb is in available to us
	            && this.left_type[0]!=0) {
	        /* First vertical edge is different in MBAFF frames
	         * There are 8 different bS to compute and 2 different Qp
	         */
	        //DECLARE_ALIGNED(8, int16_t, bS)[8];
	    	int[] bS = new int[8];
	        int[] qp = new int[2];
	        int[] bqp = new int[2];
	        int[] rqp = new int[2];
	        int mb_qp, mbn0_qp, mbn1_qp;
	        int i;
	        first_vertical_edge_done = 1;

	        if( 0!=(mb_type & 7) ) {
	            //AV_WN64A(&bS_base[bS_offset + 0], 0x0004000400040004ULL);
	            //AV_WN64A(&bS_base[bS_offset + 4], 0x0004000400040004ULL);
	        	for(int j=0;j<8;j++)
	        		bS[j] = 0x0004;
	        } else {
	        	/*
	            final uint8_t offset[2][2][8]={
	                {
	                    {7+8*0, 7+8*0, 7+8*0, 7+8*0, 7+8*1, 7+8*1, 7+8*1, 7+8*1},
	                    {7+8*2, 7+8*2, 7+8*2, 7+8*2, 7+8*3, 7+8*3, 7+8*3, 7+8*3},
	                },{
	                    {7+8*0, 7+8*1, 7+8*2, 7+8*3, 7+8*0, 7+8*1, 7+8*2, 7+8*3},
	                    {7+8*0, 7+8*1, 7+8*2, 7+8*3, 7+8*0, 7+8*1, 7+8*2, 7+8*3},
	                }
	            };
	            */
	            final int[][][] offset = {
		                {
		                    {7+8*0, 7+8*0, 7+8*0, 7+8*0, 7+8*1, 7+8*1, 7+8*1, 7+8*1},
		                    {7+8*2, 7+8*2, 7+8*2, 7+8*2, 7+8*3, 7+8*3, 7+8*3, 7+8*3},
		                },{
		                    {7+8*0, 7+8*1, 7+8*2, 7+8*3, 7+8*0, 7+8*1, 7+8*2, 7+8*3},
		                    {7+8*0, 7+8*1, 7+8*2, 7+8*3, 7+8*0, 7+8*1, 7+8*2, 7+8*3},
		                }
		            };

	            //uint8_t *off_base = offset[mb_field_decoding_flag][mb_y&1];
	            int[] off = offset[mb_field_decoding_flag][mb_y&1];
	            for( i = 0; i < 8; i++ ) {
	                int j= (mb_field_decoding_flag !=0? i>>2 : i&1);
	                int mbn_xy = this.left_mb_xy[j];
	                int mbn_type= this.left_type[j];

	                if( ( mbn_type & 7) !=0)
	                    bS[i] = 4;
	                else{
	                    bS[i] = 1 + ((this.non_zero_count_cache[12+8*(i>>1)]!=0?1:0) |
	                         ((0==this.pps.cabac && (mbn_type & MB_TYPE_8x8DCT)!=0) ?
	                            ( ((this.cbp_table[mbn_xy] & ((mb_field_decoding_flag !=0? (i&2) : (mb_y&1)))) !=0? 8 : 2))
	                                                                       :
	                            this.non_zero_count[mbn_xy][ off[i] ]));
	                }
	            }
	        }

	        mb_qp = s.current_picture.qscale_table[mb_xy];
	        mbn0_qp = s.current_picture.qscale_table[this.left_mb_xy[0]];
	        mbn1_qp = s.current_picture.qscale_table[this.left_mb_xy[1]];
	        qp[0] = ( mb_qp + mbn0_qp + 1 ) >> 1;
	        bqp[0] = ( pps.chroma_qp_table[ 0 ][ mb_qp ] +
	        			pps.chroma_qp_table[ 0 ][ mbn0_qp ] + 1 ) >> 1;
	        rqp[0] = ( pps.chroma_qp_table[ 1][ mb_qp] +
	        			pps.chroma_qp_table[ 1 ][ mbn0_qp ] + 1 ) >> 1;
	        qp[1] = ( mb_qp + mbn1_qp + 1 ) >> 1;
	        bqp[1] = ( pps.chroma_qp_table[ 0 ][ mb_qp ] +
	        			pps.chroma_qp_table[ 0 ][ mbn1_qp ] + 1 ) >> 1;
	        rqp[1] = ( pps.chroma_qp_table[ 1 ][ mb_qp ] +
	        			pps.chroma_qp_table[ 1 ][ mbn1_qp ] + 1 ) >> 1;

	        /* Filter edge */
	        //tprintf(s.avctx, "filter mb:%d/%d MBAFF, QPy:%d/%d, QPb:%d/%d QPr:%d/%d ls:%d uvls:%d", mb_x, mb_y, qp[0], qp[1], bqp[0], bqp[1], rqp[0], rqp[1], linesize, uvlinesize);
	        //{ int i; for (i = 0; i < 8; i++) tprintf(s.avctx, " bS[%d]:%d", i, bS[i]); tprintf(s.avctx, "\n"); }
	        if(mb_field_decoding_flag!=0){
	            filter_mb_mbaff_edgev ( img_y_base,img_y_offset   ,   linesize, bS,0  , 1, qp [0] );
	            filter_mb_mbaff_edgev ( img_y_base,img_y_offset  + 8* linesize,   linesize, bS,4, 1, qp [1] );
	            filter_mb_mbaff_edgecv( img_cb_base,img_cb_offset,   uvlinesize, bS,0  , 1, bqp[0] );
	            filter_mb_mbaff_edgecv( img_cb_base,img_cb_offset + 4*uvlinesize, uvlinesize, bS,4, 1, bqp[1] );
	            filter_mb_mbaff_edgecv( img_cr_base,img_cr_offset,   uvlinesize, bS,0  , 1, rqp[0] );
	            filter_mb_mbaff_edgecv( img_cr_base,img_cr_offset + 4*uvlinesize, uvlinesize, bS,4, 1, rqp[1] );
	        }else{
	            filter_mb_mbaff_edgev ( img_y_base,img_y_offset, 2*  linesize, bS,0  , 2, qp [0] );
	            filter_mb_mbaff_edgev ( img_y_base,img_y_offset  +   linesize, 2*  linesize, bS,1, 2, qp [1] );
	            filter_mb_mbaff_edgecv( img_cb_base,img_cb_offset,2*uvlinesize, bS,0  , 2, bqp[0] );
	            filter_mb_mbaff_edgecv( img_cb_base,img_cb_offset + uvlinesize, 2*uvlinesize, bS,1, 2, bqp[1] );
	            filter_mb_mbaff_edgecv( img_cr_base,img_cr_offset,2*uvlinesize, bS,0  , 2, rqp[0] );
	            filter_mb_mbaff_edgecv( img_cr_base,img_cr_offset + uvlinesize, 2*uvlinesize, bS,1, 2, rqp[1] );
	        }
	    }

	    for( dir = 0; dir < 2; dir++ )
	        filter_mb_dir(mb_x, mb_y, 
	    			img_y_base, img_y_offset, 
	    			img_cb_base, img_cb_offset, 
	    			img_cr_base, img_cr_offset, 
	        		linesize, uvlinesize, mb_xy, mb_type, mvy_limit, (dir!=0 ? 0 : first_vertical_edge_done), dir);
	}
	
	public void loop_filter(){
	    //uint8_t  *dest_y, *dest_cb, *dest_cr;
		int[] dest_y_base; int dest_y_offset;
		int[] dest_cb_base; int dest_cb_offset;
		int[] dest_cr_base; int dest_cr_offset;		
	    int linesize, uvlinesize, mb_x, mb_y;
	    int end_mb_y= s.mb_y + mb_aff_frame;
	    int old_slice_type= this.slice_type;

	    if(this.deblocking_filter != 0) {
	        for(mb_x= 0; mb_x<s.mb_width; mb_x++){
	            for(mb_y=end_mb_y - mb_aff_frame; mb_y<= end_mb_y; mb_y++){
	                int mb_xy, mb_type;
	                mb_xy = this.mb_xy = mb_x + mb_y*s.mb_stride;
	                this.slice_num= this.slice_table_base[this.slice_table_offset + mb_xy];
	                mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
	                this.list_count= this.list_counts[mb_xy];

	                if(mb_aff_frame != 0)
	                    this.mb_mbaff = this.mb_field_decoding_flag = (0!=(mb_type & MB_TYPE_INTERLACED)?1:0);

	                s.mb_x= mb_x;
	                s.mb_y= mb_y;
	                dest_y_base  = s.current_picture.data_base[0];
	                dest_y_offset = s.current_picture.data_offset[0] + (mb_x + mb_y * s.linesize  ) * 16;
	                dest_cb_base = s.current_picture.data_base[1];
	                dest_cb_offset = s.current_picture.data_offset[1] + (mb_x + mb_y * s.uvlinesize) * 8;
	                dest_cr_base = s.current_picture.data_base[2];
	                dest_cr_offset = s.current_picture.data_offset[2] + (mb_x + mb_y * s.uvlinesize) * 8;
	                    //FIXME simplify above

	                //// DebugTool.dumpDebugFrameData(this, "BEFORE-loop_filter", false);
	                
	                if (mb_field_decoding_flag != 0) {
	                    linesize   = this.mb_linesize   = s.linesize * 2;
	                    uvlinesize = this.mb_uvlinesize = s.uvlinesize * 2;
	                    if((mb_y&1)!=0){ //FIXME move out of this function?
	                        dest_y_offset -= s.linesize*15;
	                        dest_cb_offset-= s.uvlinesize*7;
	                        dest_cr_offset-= s.uvlinesize*7;
	                    }
	                } else {
	                    linesize   = this.mb_linesize   = s.linesize;
	                    uvlinesize = this.mb_uvlinesize = s.uvlinesize;
	                }
	                backup_mb_border(
	    					dest_y_base, dest_y_offset,
	    					dest_cb_base, dest_cb_offset, 
	    					dest_cr_base, dest_cr_offset, 
	                		linesize, uvlinesize, 0);

	                //// DebugTool.dumpDebugFrameData(this, "INSIDE-loop_filter(1)", false);
	                
	                if(fill_filter_caches(mb_type)!=0)
	                    continue;

	                //// DebugTool.dumpDebugFrameData(this, "INSIDE-loop_filter(2)", false);
	                
	                this.chroma_qp[0] = pps.chroma_qp_table[0][s.current_picture.qscale_table[mb_xy]]; // get_chroma_qp(0, s.current_picture.qscale_table[mb_xy]);
	                this.chroma_qp[1] = pps.chroma_qp_table[1][s.current_picture.qscale_table[mb_xy]]; // get_chroma_qp(1, s.current_picture.qscale_table[mb_xy]);

	                if (mb_aff_frame != 0) {
	                    ff_h264_filter_mb     (mb_x, mb_y, 
	        					dest_y_base, dest_y_offset,
	        					dest_cb_base, dest_cb_offset, 
	        					dest_cr_base, dest_cr_offset, 
	                    		linesize, uvlinesize);
	                } else {
	                    ff_h264_filter_mb_fast(mb_x, mb_y, 
	        					dest_y_base, dest_y_offset,
	        					dest_cb_base, dest_cb_offset, 
	        					dest_cr_base, dest_cr_offset, 
	                    		linesize, uvlinesize);
	                }
	                
	                /*
	                if(// DebugTool.logCount == 9680) {
	                	// DebugTool.dumpDebugFrameData(this, "INSIDE-loop_filter(3)", false);
	                	// DebugTool.dumpFrameData(this.s.current_picture_ptr);
	                } // if
	                */
	            }
	        }
	    }
	    this.slice_type= old_slice_type;
	    s.mb_x= 0;
	    s.mb_y= end_mb_y - mb_aff_frame;
	    this.chroma_qp[0] = pps.chroma_qp_table[0][s.qscale];
	    this.chroma_qp[1] = pps.chroma_qp_table[1][s.qscale];
	}
	
	///////////////////////////////////////////////////////////
	// Level 3 Functions for decoder, it calls to decode slice
	public int decode_slice(/*struct AVCodecContext *avctx, void *arg*/){
	    int part_mask= (s.partitioned_frame !=0)? (MpegEncContext.AC_END|MpegEncContext.AC_ERROR) : 0x7F;

	    s.mb_skip_run= -1;

	    this.is_complex = (mb_aff_frame !=0 
	    		|| s.picture_structure != MpegEncContext.PICT_FRAME 
	    		|| s.codec_id != H264PredictionContext.CODEC_ID_H264 
	                    )?1:0;

	    if( this.pps.cabac != 0) {
	        /* realign */
	    	// DebugTool.printDebugString("BEFORE REALIGN: buf={"+s.gb.buffer[s.gb.buffer_offset]+","+s.gb.buffer[s.gb.buffer_offset+1]+","+s.gb.buffer[s.gb.buffer_offset+2]+","+s.gb.buffer[s.gb.buffer_offset+3]+"}\n");
	        s.gb.align_get_bits();
	    	// DebugTool.printDebugString("AFTER REALIGN: buf={"+s.gb.buffer[s.gb.buffer_offset]+","+s.gb.buffer[s.gb.buffer_offset+1]+","+s.gb.buffer[s.gb.buffer_offset+2]+","+s.gb.buffer[s.gb.buffer_offset+3]+"}\n");

	        /* init cabac */
	        this.cabac.ff_init_cabac_states();
	        this.cabac.ff_init_cabac_decoder(
	                               s.gb.buffer, s.gb.buffer_offset + s.gb.get_bits_count()/8,
	                               (s.gb.get_bits_left() + 7)/8);

	        cabac.ff_h264_init_cabac_states(this);

	        for(;;){
	//START_TIMER
	            int ret = ff_h264_decode_mb_cabac();
	            // DebugTool.printDebugString(" ---- ff_h264_decode_mb_cabac return "+ret+"\n");

	            int eos;
	//STOP_TIMER("decode_mb_cabac")

	            if(ret>=0) ff_h264_hl_decode_mb();
	            //// DebugTool.dumpDebugFrameData(this, "AFTER-ff_h264_hl_decode_mb", false);

	            if( ret >= 0 && mb_aff_frame != 0) { //FIXME optimal? or let mb_decode decode 16x32 ?
	                s.mb_y++;

	                ret = ff_h264_decode_mb_cabac();

	                if(ret>=0) ff_h264_hl_decode_mb();
	                s.mb_y--;
	            }
	            eos = this.cabac.get_cabac_terminate();

	            if((s.workaround_bugs & MpegEncContext.FF_BUG_TRUNCATED)!=0 && this.cabac.bytestream_current > this.cabac.bytestream_end + 2){
	            	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x-1, s.mb_y, (MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END)&part_mask);
	                return 0;
	            }
	            if( ret < 0 || this.cabac.bytestream_current > this.cabac.bytestream_end + 2) {
	                //av_log(this.s.avctx, AV_LOG_ERROR, "error while decoding MB %d %d, bytestream (%td)\n", s.mb_x, s.mb_y, this.cabac.bytestream_end - this.cabac.bytestream);
	            	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x, s.mb_y, (MpegEncContext.AC_ERROR|MpegEncContext.DC_ERROR|MpegEncContext.MV_ERROR)&part_mask);
	                return -1;
	            }

	            if( ++s.mb_x >= s.mb_width ) {
	                s.mb_x = 0;
	                loop_filter();
	                s.ff_draw_horiz_band(16*s.mb_y, 16);
	                ++s.mb_y;
	                if((mb_aff_frame!=0) || s.picture_structure != MpegEncContext.PICT_FRAME) {
	                    ++s.mb_y;
	                    if(mb_aff_frame != 0 && s.mb_y < s.mb_height)
	                        predict_field_decoding_flag();
	                }
	            }

	            if( eos !=0 || s.mb_y >= s.mb_height ) {
	                //tprintf(s.avctx, "slice end %d %d\n", get_bits_count(&s.gb), s.gb.size_in_bits);
	            	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x-1, s.mb_y, (MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END)&part_mask);
	                return 0;
	            }
	        }

	    } else {
	    	int mbCount = 0;
	        for(;;){
	            int ret = cavlc.ff_h264_decode_mb_cavlc(this);
	            
	            // DebugTool.printDebugString(" ---- ff_h264_decode_mb_cavlc return "+ret+"\n");

	            mbCount++;
	            //System.out.println("Reading Macro-Block ("+ mbCount +" )");

	            if(ret>=0) ff_h264_hl_decode_mb();
	            
	            if(ret>=0 && mb_aff_frame != 0){ //FIXME optimal? or let mb_decode decode 16x32 ?
	                s.mb_y++;
	                ret = cavlc.ff_h264_decode_mb_cavlc(this);

		            // DebugTool.dumpDebugFrameData(this, "AFTER-ff_h264_decode_mb_cavlc#2", false);

	                if(ret>=0) ff_h264_hl_decode_mb();
	                s.mb_y--;
	            }

	            if(ret<0){
	                //av_log(this.s.avctx, AV_LOG_ERROR, "error while decoding MB %d %d\n", s.mb_x, s.mb_y);
	            	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x, s.mb_y, (MpegEncContext.AC_ERROR|MpegEncContext.DC_ERROR|MpegEncContext.MV_ERROR)&part_mask);

	                return -1;
	            }

	            if(++s.mb_x >= s.mb_width){
	                s.mb_x=0;
	                loop_filter();

	                s.ff_draw_horiz_band(16*s.mb_y, 16);
	                ++s.mb_y;
	                if((mb_aff_frame !=0)|| (s.picture_structure != MpegEncContext.PICT_FRAME) ) {
	                    ++s.mb_y;
	                    if(mb_aff_frame !=0 && s.mb_y < s.mb_height)
	                        predict_field_decoding_flag();
	                }
	                if(s.mb_y >= s.mb_height){
	                    //tprintf(s.avctx, "slice end %d %d\n", get_bits_count(&s.gb), s.gb.size_in_bits);

	                    if(s.gb.get_bits_count() == s.gb.size_in_bits ) {
	                    	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x-1, s.mb_y, (MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END)&part_mask);

	                        return 0;
	                    }else{
	                    	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x, s.mb_y, (MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END)&part_mask);

	                        return -1;
	                    }
	                }
	            }

	            if(s.gb.get_bits_count() >= s.gb.size_in_bits && s.mb_skip_run<=0){
	                //tprintf(s.avctx, "slice end %d %d\n", get_bits_count(&s.gb), s.gb.size_in_bits);
	                if(s.gb.get_bits_count() == s.gb.size_in_bits ){
	                	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x-1, s.mb_y, (MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END)&part_mask);

	                    return 0;
	                }else{
	                	ErrorResilience.ff_er_add_slice(s, s.resync_mb_x, s.resync_mb_y, s.mb_x, s.mb_y, (MpegEncContext.AC_ERROR|MpegEncContext.DC_ERROR|MpegEncContext.MV_ERROR)&part_mask);

	                    return -1;
	                }
	            }
	        }
	    }

	    //return -1; //not reached
	}
	
	////////////////////////////////
	// Motion functions
	//public void mc_dir_part(Picture pic, int n, int square, int chroma_height, int delta, int list,
    //        uint8_t *dest_y, uint8_t *dest_cb, uint8_t *dest_cr,
    //        int src_x_offset, int src_y_offset,
    //        qpel_mc_func *qpix_op, h264_chroma_mc_func chroma_op){
	public void mc_dir_part(AVFrame pic, int n, int square, int chroma_height, int delta, int list,
			int[] dest_y_base, int dest_y_offset,
			int[] dest_cb_base, int dest_cb_offset,
			int[] dest_cr_base, int dest_cr_offset,
            int src_x_offset, int src_y_offset,
            DSPContext.Ih264_qpel_mc_func[] qpix_op, DSPContext.Ih264_chroma_mc_func chroma_op) {
		int mx= this.mv_cache[list][ scan8[n] ][0] + src_x_offset*8;
		int my=       this.mv_cache[list][ scan8[n] ][1] + src_y_offset*8;
		int luma_xy= (mx&3) + ((my&3)<<2);
		
		//uint8_t * src_y = pic.data[0] + (mx>>2) + (my>>2)*this.mb_linesize;
		//uint8_t * src_cb, * src_cr;
		int[] src_y_base = pic.data_base[0];
		int _src_y_offset = pic.data_offset[0] + (mx>>2) + (my>>2)*this.mb_linesize;
		int[] src_cb_base;
		int src_cb_offset;
		int[] src_cr_base;
		int src_cr_offset;
		
		int extra_width= this.emu_edge_width;
		int extra_height= this.emu_edge_height;
		int emu=0;
		int full_mx= mx>>2;
		int full_my= my>>2;
		int pic_width  = 16*s.mb_width;
		int pic_height = 16*s.mb_height >> mb_field_decoding_flag;

		// DebugTool.printDebugString("***mc_dir_part: src_x_offset="+src_x_offset+", src_y_offset="+src_y_offset+", list="+list+", n="+n+", mv_cache[0]="+this.mv_cache[list][ scan8[n] ][0]+", mv_cache[1]="+this.mv_cache[list][ scan8[n] ][1]+"\n");		        	
		// DebugTool.printDebugString("***mc_dir_part: mx="+mx+", my="+my+", luma_xy="+luma_xy+", _src_y_offset="+((mx>>2) + (my>>2)*this.mb_linesize)+"\n");
		
		if((mx&7)!=0) extra_width -= 3;
		if((my&7)!=0) extra_height -= 3;
		
		if(   full_mx < 0-extra_width
		|| full_my < 0-extra_height
		|| full_mx + 16/*FIXME*/ > pic_width + extra_width
		|| full_my + 16/*FIXME*/ > pic_height + extra_height){
			
			// DebugTool.printDebugString("***mc_dir_part: case 1\n");
			
			s.dsp.ff_emulated_edge_mc(s.allocated_edge_emu_buffer, s.edge_emu_buffer_offset
					, src_y_base, _src_y_offset - 2 - 2*this.mb_linesize
					, this.mb_linesize, 16+5, 16+5/*FIXME*/
					, full_mx-2, full_my-2
					, pic_width, pic_height);
			
			src_y_base = s.allocated_edge_emu_buffer;
			_src_y_offset = s.edge_emu_buffer_offset + 2 + 2*this.mb_linesize;
			emu=1;
		}
		
		qpix_op[luma_xy].h264_qpel_mc_func(dest_y_base, dest_y_offset, src_y_base, _src_y_offset, this.mb_linesize); //FIXME try variable height perhaps?
		
		if(0==square){
			// DebugTool.printDebugString("***mc_dir_part: case 2\n");
			
			qpix_op[luma_xy].h264_qpel_mc_func(dest_y_base, dest_y_offset + delta, src_y_base, _src_y_offset + delta, this.mb_linesize);
		}
		
		//if(MpegEncContext.CONFIG_GRAY !=0 && (s.flags&MpegEncContext.CODEC_FLAG_GRAY)!=0) return;
		
		if(mb_field_decoding_flag != 0){
			// DebugTool.printDebugString("***mc_dir_part: case 3\n");

			// chroma offset when predicting from a field of opposite parity
			my += 2 * ((s.mb_y & 1) - (pic.reference - 1));
			emu |= (((my>>3) < 0 || (my>>3) + 8 >= (pic_height>>1))?1:0);
		}
		
		src_cb_base = pic.data_base[1];
		src_cb_offset = pic.data_offset[1] + (mx>>3) + (my>>3)*this.mb_uvlinesize;
		src_cr_base = pic.data_base[2];
		src_cr_offset= pic.data_offset[2] + (mx>>3) + (my>>3)*this.mb_uvlinesize;
		
		if(emu!=0){
			// DebugTool.printDebugString("***mc_dir_part: case 4\n");

			s.dsp.ff_emulated_edge_mc(s.allocated_edge_emu_buffer, s.edge_emu_buffer_offset, src_cb_base, src_cb_offset, this.mb_uvlinesize, 9, 9/*FIXME*/, (mx>>3), (my>>3), pic_width>>1, pic_height>>1);
			src_cb_base = s.allocated_edge_emu_buffer;
			src_cb_offset = s.edge_emu_buffer_offset;
		}
		chroma_op.h264_chroma_mc_func(dest_cb_base, dest_cb_offset, src_cb_base, src_cb_offset, this.mb_uvlinesize, chroma_height, mx&7, my&7);

		if(emu!=0){
			// DebugTool.printDebugString("***mc_dir_part: case 5\n");

			s.dsp.ff_emulated_edge_mc(s.allocated_edge_emu_buffer, s.edge_emu_buffer_offset, src_cr_base, src_cr_offset, this.mb_uvlinesize, 9, 9/*FIXME*/, (mx>>3), (my>>3), pic_width>>1, pic_height>>1);
			src_cr_base = s.allocated_edge_emu_buffer;
			src_cr_offset = s.edge_emu_buffer_offset;
		}
		chroma_op.h264_chroma_mc_func(dest_cr_base, dest_cr_offset, src_cr_base, src_cr_offset, this.mb_uvlinesize, chroma_height, mx&7, my&7);

	}
		
	public void mc_part_std(int n, int square, int chroma_height, int delta,
			int[] dest_y_base, int dest_y_offset,
			int[] dest_cb_base, int dest_cb_offset,
			int[] dest_cr_base, int dest_cr_offset,
	            int x_offset, int y_offset,
	            DSPContext.Ih264_qpel_mc_func[] qpix_put, DSPContext.Ih264_chroma_mc_func chroma_put,
	            DSPContext.Ih264_qpel_mc_func[] qpix_avg, DSPContext.Ih264_chroma_mc_func chroma_avg,
	            int list0, int list1){
		DSPContext.Ih264_qpel_mc_func[] qpix_op=  qpix_put;
		DSPContext.Ih264_chroma_mc_func chroma_op= chroma_put;
		
		dest_y_offset  += 2*x_offset + 2*y_offset*this.  mb_linesize;
		dest_cb_offset +=   x_offset +   y_offset*this.mb_uvlinesize;
		dest_cr_offset +=   x_offset +   y_offset*this.mb_uvlinesize;
		x_offset += 8*s.mb_x;
		y_offset += 8*(s.mb_y >> mb_field_decoding_flag);
		
		if(list0!=0){
			AVFrame ref= this.ref_list[0][ this.ref_cache[0][ scan8[n] ] ];
			mc_dir_part(ref, n, square, chroma_height, delta, 0,
					dest_y_base, dest_y_offset,
					dest_cb_base, dest_cb_offset, 
					dest_cr_base, dest_cr_offset, 
			            x_offset, y_offset,
			            qpix_op, chroma_op);
			
			qpix_op=  qpix_avg;
			chroma_op= chroma_avg;
		}
		
		if(list1!=0){
			AVFrame ref= this.ref_list[1][ this.ref_cache[1][ scan8[n] ] ];
			mc_dir_part(ref, n, square, chroma_height, delta, 1,
					dest_y_base, dest_y_offset,
					dest_cb_base, dest_cb_offset, 
					dest_cr_base, dest_cr_offset, 
			            x_offset, y_offset,
			            qpix_op, chroma_op);
		}
	}
		
	public void mc_part_weighted(int n, int square, int chroma_height, int delta,
			int[] dest_y_base, int dest_y_offset,
			int[] dest_cb_base, int dest_cb_offset,
			int[] dest_cr_base, int dest_cr_offset,
	            int x_offset, int y_offset,
	            DSPContext.Ih264_qpel_mc_func[] qpix_put, DSPContext.Ih264_chroma_mc_func chroma_put,
	            H264DSPContext.IH264WeightFunctionStub luma_weight_op, H264DSPContext.IH264WeightFunctionStub chroma_weight_op,
	            H264DSPContext.IH264BiWeightFunctionStub luma_weight_avg, H264DSPContext.IH264BiWeightFunctionStub chroma_weight_avg,
	            int list0, int list1){
		
		dest_y_offset  += 2*x_offset + 2*y_offset*this.  mb_linesize;
		dest_cb_offset +=   x_offset +   y_offset*this.mb_uvlinesize;
		dest_cr_offset +=   x_offset +   y_offset*this.mb_uvlinesize;
		x_offset += 8*s.mb_x;
		y_offset += 8*(s.mb_y >> mb_field_decoding_flag);
		
		if(list0!=0 && list1!=0){
			/* don't optimize for luma-only case, since B-frames usually
			* use implicit weights => chroma too. */
			int[] tmp_cb_base = s.obmc_scratchpad;
			int tmp_cb_offset = 0;
			int[] tmp_cr_base = s.obmc_scratchpad;
			int tmp_cr_offset = 8;
			int[] tmp_y_base  = s.obmc_scratchpad;
			int tmp_y_offset = 8*this.mb_uvlinesize;
			int refn0 = this.ref_cache[0][ scan8[n] ];
			int refn1 = this.ref_cache[1][ scan8[n] ];
			
			mc_dir_part(this.ref_list[0][refn0], n, square, chroma_height, delta, 0,
					dest_y_base, dest_y_offset,
					dest_cb_base, dest_cb_offset, 
					dest_cr_base, dest_cr_offset, 
			     x_offset, y_offset, qpix_put, chroma_put);
			mc_dir_part(this.ref_list[1][refn1], n, square, chroma_height, delta, 1,
					tmp_y_base, tmp_y_offset,
					tmp_cb_base, tmp_cb_offset, 
					tmp_cr_base, tmp_cr_offset, 
			     x_offset, y_offset, qpix_put, chroma_put);
			
			if(this.use_weight == 2){
				int weight0 = this.implicit_weight[refn0][refn1][s.mb_y&1];
				int weight1 = 64 - weight0;
				luma_weight_avg.h264_biweight_func(  dest_y_base, dest_y_offset,  tmp_y_base, tmp_y_offset, this.  mb_linesize, 5, weight0, weight1, 0);
				chroma_weight_avg.h264_biweight_func(dest_cb_base, dest_cb_offset, tmp_cb_base, tmp_cb_offset, this.mb_uvlinesize, 5, weight0, weight1, 0);
				chroma_weight_avg.h264_biweight_func(dest_cr_base, dest_cr_offset, tmp_cr_base, tmp_cr_offset, this.mb_uvlinesize, 5, weight0, weight1, 0);
			}else{
				luma_weight_avg.h264_biweight_func(dest_y_base, dest_y_offset,  tmp_y_base, tmp_y_offset, this.mb_linesize, this.luma_log2_weight_denom,
				             this.luma_weight[refn0][0][0] , this.luma_weight[refn1][1][0],
				             this.luma_weight[refn0][0][1] + this.luma_weight[refn1][1][1]);
				chroma_weight_avg.h264_biweight_func(dest_cb_base, dest_cb_offset, tmp_cb_base, tmp_cb_offset, this.mb_uvlinesize, this.chroma_log2_weight_denom,
				             this.chroma_weight[refn0][0][0][0] , this.chroma_weight[refn1][1][0][0],
				             this.chroma_weight[refn0][0][0][1] + this.chroma_weight[refn1][1][0][1]);
				chroma_weight_avg.h264_biweight_func(dest_cr_base, dest_cr_offset, tmp_cr_base, tmp_cr_offset, this.mb_uvlinesize, this.chroma_log2_weight_denom,
				             this.chroma_weight[refn0][0][1][0] , this.chroma_weight[refn1][1][1][0],
				             this.chroma_weight[refn0][0][1][1] + this.chroma_weight[refn1][1][1][1]);
			}
		}else{
			int list = (list1!=0 ? 1 : 0);
			int refn = this.ref_cache[list][ scan8[n] ];
			AVFrame ref= this.ref_list[list][refn];
			mc_dir_part(ref, n, square, chroma_height, delta, list,
					dest_y_base, dest_y_offset,
					dest_cb_base, dest_cb_offset, 
					dest_cr_base, dest_cr_offset, 
			     x_offset, y_offset,
			     qpix_put, chroma_put);
			
			luma_weight_op.h264_weight_func(dest_y_base, dest_y_offset, this.mb_linesize, this.luma_log2_weight_denom,
			        this.luma_weight[refn][list][0], this.luma_weight[refn][list][1]);
			if(this.use_weight_chroma!=0){
				chroma_weight_op.h264_weight_func(dest_cb_base, dest_cb_offset, this.mb_uvlinesize, this.chroma_log2_weight_denom,
				              this.chroma_weight[refn][list][0][0], this.chroma_weight[refn][list][0][1]);
				chroma_weight_op.h264_weight_func(dest_cr_base, dest_cr_offset, this.mb_uvlinesize, this.chroma_log2_weight_denom,
				              this.chroma_weight[refn][list][1][0], this.chroma_weight[refn][list][1][1]);
			}
		}
	}

	/*
	public void mc_part(int n, int square, int chroma_height, int delta,
            uint8_t *dest_y, 
            uint8_t *dest_cb, 
            uint8_t *dest_cr,
            int x_offset, int y_offset,
            qpel_mc_func *qpix_put, h264_chroma_mc_func chroma_put,
            qpel_mc_func *qpix_avg, h264_chroma_mc_func chroma_avg,
            h264_weight_func *weight_op, h264_biweight_func *weight_avg,
            int list0, int list1){
	*/
	public void mc_part(int n, int square, int chroma_height, int delta,
			int[] dest_y_base, int dest_y_offset,
			int[] dest_cb_base, int dest_cb_offset,
			int[] dest_cr_base, int dest_cr_offset,
	            int x_offset, int y_offset,
	            DSPContext.Ih264_qpel_mc_func[] qpix_put, DSPContext.Ih264_chroma_mc_func chroma_put,
	            DSPContext.Ih264_qpel_mc_func[] qpix_avg, DSPContext.Ih264_chroma_mc_func chroma_avg,
	            H264DSPContext.IH264WeightFunctionStub[] weight_op_base, int weight_op_offset, 
	            H264DSPContext.IH264BiWeightFunctionStub[] weight_avg_base, int weight_avg_offset,
	            int list0, int list1){
		if((this.use_weight==2 && list0!=0 && list1!=0
		&& (this.implicit_weight[ this.ref_cache[0][scan8[n]] ][ this.ref_cache[1][scan8[n]] ][this.s.mb_y&1] != 32))
		|| this.use_weight==1)
			mc_part_weighted(n, square, chroma_height, delta,
					dest_y_base, dest_y_offset,
					dest_cb_base, dest_cb_offset, 
					dest_cr_base, dest_cr_offset, 
			          x_offset, y_offset, qpix_put, chroma_put,
			          weight_op_base[weight_op_offset + 0], weight_op_base[weight_op_offset + 3], 
			          weight_avg_base[weight_avg_offset + 0], weight_avg_base[weight_avg_offset + 3]
			          , list0, list1);
		else
			mc_part_std(n, square, chroma_height, delta, 
					dest_y_base, dest_y_offset,
					dest_cb_base, dest_cb_offset, 
					dest_cr_base, dest_cr_offset, 
			     x_offset, y_offset, qpix_put, chroma_put, qpix_avg, chroma_avg, list0, list1);
	}
		
	public void prefetch_motion(int list){
		// No prefetch
		/* fetch pixels for estimated mv 4 macroblocks ahead
		* optimized for 64byte cache lines */
		/*
		int refn = this.ref_cache[list][scan8[0]];
		if(refn >= 0){
			int mx= (this.mv_cache[list][scan8[0]][0]>>2) + 16*s.mb_x + 8;
			int my= (this.mv_cache[list][scan8[0]][1]>>2) + 16*s.mb_y;
			uint8_t **src= this.ref_list[list][refn].data;
			int off= mx + (my + (s.mb_x&3)*4)*this.mb_linesize + 64;
			s.dsp.prefetch(src[0]+off, s.linesize, 4);
			off= (mx>>1) + ((my>>1) + (s.mb_x&7))*s.uvlinesize + 64;
			s.dsp.prefetch(src[1]+off, src[2]-src[1], 2);
		}
		*/
	}
	
	private int IS_DIR(int a, int part, int list) { return ((a) & (MB_TYPE_P0L0<<((part)+2*(list)))); }
		
//	public void hl_motion(uint8_t *dest_y, uint8_t *dest_cb, uint8_t *dest_cr,
//	       qpel_mc_func (*qpix_put)[16], h264_chroma_mc_func (*chroma_put),
//	       qpel_mc_func (*qpix_avg)[16], h264_chroma_mc_func (*chroma_avg),
//	       h264_weight_func *weight_op, h264_biweight_func *weight_avg){
	public void hl_motion(
			int[] dest_y_base, int dest_y_offset,
			int[] dest_cb_base, int dest_cb_offset,
			int[] dest_cr_base, int dest_cr_offset,
			DSPContext.Ih264_qpel_mc_func[][] qpix_put, DSPContext.Ih264_chroma_mc_func[] chroma_put,
			DSPContext.Ih264_qpel_mc_func[][] qpix_avg, DSPContext.Ih264_chroma_mc_func[] chroma_avg,
			H264DSPContext.IH264WeightFunctionStub[] weight_op, H264DSPContext.IH264BiWeightFunctionStub[] weight_avg){

		int mb_xy= this.mb_xy;
		int mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
		
		////assert(IS_INTER(mb_type));
		
		prefetch_motion(0);
		
		if((mb_type & MB_TYPE_16x16)!=0){
			mc_part(0, 1, 8, 0, dest_y_base, dest_y_offset
					, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
					, 0, 0,
					qpix_put[0], chroma_put[0], qpix_avg[0], chroma_avg[0],
					weight_op, 0, weight_avg, 0,
					IS_DIR(mb_type, 0, 0)
					, IS_DIR(mb_type, 0, 1));
		}else if((mb_type & MB_TYPE_16x8)!=0){
			mc_part(0, 0, 4, 8, dest_y_base, dest_y_offset
					, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
		 			, 0, 0,
					qpix_put[1], chroma_put[0], qpix_avg[1], chroma_avg[0],
					weight_op, 1, weight_avg, 1,
					IS_DIR(mb_type, 0, 0)
					, IS_DIR(mb_type, 0, 1));
			mc_part(8, 0, 4, 8, dest_y_base, dest_y_offset
					, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
					,0, 4,
					qpix_put[1], chroma_put[0], qpix_avg[1], chroma_avg[0],
					weight_op, 1, weight_avg, 1,
					IS_DIR(mb_type, 1, 0)
					, IS_DIR(mb_type, 1, 1));
		}else if((mb_type & MB_TYPE_8x16)!=0){
			mc_part(0, 0, 8, 8*this.mb_linesize, dest_y_base, dest_y_offset
					, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
					, 0, 0,
					 qpix_put[1], chroma_put[1], qpix_avg[1], chroma_avg[1],
					 weight_op, 2, weight_avg, 2,
					 IS_DIR(mb_type, 0, 0)
					 , IS_DIR(mb_type, 0, 1));
			mc_part(4, 0, 8, 8*this.mb_linesize, dest_y_base, dest_y_offset
					, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
					, 4, 0,
					 qpix_put[1], chroma_put[1], qpix_avg[1], chroma_avg[1],
					 weight_op, 2, weight_avg, 2,
					 IS_DIR(mb_type, 1, 0)
					 , IS_DIR(mb_type, 1, 1));
		}else{
			int i;
			
			////assert(IS_8X8(mb_type));
			
			for(i=0; i<4; i++){
				int sub_mb_type= this.sub_mb_type[i];
				int n= 4*i;
				int x_offset= (i&1)<<2;
				int y_offset= (i&2)<<1;
				
				if((sub_mb_type & MB_TYPE_16x16)!=0){
					 mc_part(n, 1, 4, 0, dest_y_base, dest_y_offset
								, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
								, x_offset, y_offset,
							     qpix_put[1], chroma_put[1], qpix_avg[1], chroma_avg[1],
							     weight_op, 3, weight_avg, 3,
							     IS_DIR(sub_mb_type, 0, 0)
							     , IS_DIR(sub_mb_type, 0, 1));
				}else if((sub_mb_type & MB_TYPE_16x8)!=0){
					 mc_part(n  , 0, 2, 4, dest_y_base, dest_y_offset
								, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
								, x_offset, y_offset,
							     qpix_put[2], chroma_put[1], qpix_avg[2], chroma_avg[1],
							     weight_op, 4, weight_avg, 4,
							     IS_DIR(sub_mb_type, 0, 0)
							     , IS_DIR(sub_mb_type, 0, 1));
					 mc_part(n+2, 0, 2, 4, dest_y_base, dest_y_offset
								, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
								, x_offset, y_offset+2,
							     qpix_put[2], chroma_put[1], qpix_avg[2], chroma_avg[1],
							     weight_op, 4, weight_avg, 4,
							     IS_DIR(sub_mb_type, 0, 0)
							     , IS_DIR(sub_mb_type, 0, 1));
				}else if((sub_mb_type & MB_TYPE_8x16)!=0){
					 mc_part(n  , 0, 4, 4*this.mb_linesize, dest_y_base, dest_y_offset
								, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
								, x_offset, y_offset,
							     qpix_put[2], chroma_put[2], qpix_avg[2], chroma_avg[2],
							     weight_op, 5, weight_avg, 5,
							     IS_DIR(sub_mb_type, 0, 0)
							     , IS_DIR(sub_mb_type, 0, 1));
					 mc_part(n+1, 0, 4, 4*this.mb_linesize, dest_y_base, dest_y_offset
								, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
								, x_offset+2, y_offset,
							     qpix_put[2], chroma_put[2], qpix_avg[2], chroma_avg[2],
							     weight_op, 5, weight_avg, 5,
							     IS_DIR(sub_mb_type, 0, 0)
							     , IS_DIR(sub_mb_type, 0, 1));
				}else{
					 int j;
					 ////assert(IS_SUB_4X4(sub_mb_type));
					 for(j=0; j<4; j++){
					     int sub_x_offset= x_offset + 2*(j&1);
					     int sub_y_offset= y_offset +   (j&2);
					     mc_part(n+j, 1, 2, 0, dest_y_base, dest_y_offset
									, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset
									, sub_x_offset, sub_y_offset,
							         qpix_put[2], chroma_put[2], qpix_avg[2], chroma_avg[2],
							         weight_op, 6, weight_avg, 6,
							         IS_DIR(sub_mb_type, 0, 0)
							         , IS_DIR(sub_mb_type, 0, 1));
					 }
				}
			}
		}
		prefetch_motion(1);
	}
	
//	public static void av_fast_malloc(void *ptr, unsigned int *size, FF_INTERNALC_MEM_TYPE min_size)
//  param1[0] = ptr :int[] (in/out)
//  param2[0] = size :int (in/out)
	public static void av_fast_malloc(int[][]param1, int[] param2, int min_size)
	{
		int size = param2[0];
	    //void **p = ptr;
	    if (min_size < size)
	        return;
	    min_size= Math.max(17*min_size/16 + 32, min_size);
	    //av_free(*p);
	    param1[0] = new int[min_size];
	    //if (!*p) min_size = 0;
	    param2[0] = min_size;
	}
	
	/*
	 * dst_length_consumed[0] = dst_length
	 * dst_length_consumed[1] = consumed
	 * dst_length_consumed[2] = dst_offset
	 */
	public int[] ff_h264_decode_nal(/*const uint8_t *src*/int[] src_base, int src_offset, /*int *dst_length, int *consumed*/ int[] dst_length_consumed, int length){
	    int i, si, di;
	    //uint8_t *dst;
	    int[] dst;
	    int dst_offset = 0;
	    int bufidx;

//	    src[0]&0x80;                //forbidden bit
	    this.nal_ref_idc= src_base[src_offset + 0]>>5;
	    this.nal_unit_type= src_base[src_offset + 0]&0x01F;

	    // DebugTool.printDebugString("nal_ref_idc = "+ this.nal_ref_idc + "\n");
	    // DebugTool.printDebugString("nal_unit_type = "+ this.nal_unit_type + "\n");
	    /*
	    switch(this.nal_unit_type) {
			case NAL_SLICE: //System.out.println("((NAL_SLICE))"); break;
			case NAL_DPA: //System.out.println("((NAL_DPA))"); break;
			case NAL_DPB: //System.out.println("((NAL_DPB))"); break;
			case NAL_DPC: //System.out.println("((NAL_DPC))"); break;
			case NAL_IDR_SLICE: //System.out.println("((NAL_IDR_SLICE))"); break;
			case NAL_SEI: //System.out.println("((NAL_SEI))"); break;
			case NAL_SPS: //System.out.println("((NAL_SPS))"); break;
			case NAL_PPS: //System.out.println("((NAL_PPS))"); break;
			case NAL_AUD: //System.out.println("((NAL_AUD))"); break;
			case NAL_END_SEQUENCE: //System.out.println("((NAL_END_SEQUENCE))"); break;
			case NAL_END_STREAM: //System.out.println("((NAL_END_STREAM))"); break;
			case NAL_FILLER_DATA: //System.out.println("((NAL_FILLER_DATA))"); break;
			case NAL_SPS_EXT: //System.out.println("((NAL_SPS_EXT))"); break;
			case NAL_AUXILIARY_SLICE: //System.out.println("((NAL_AUXILIARY_SLICE))"); break;
			case NAL_FUA: //System.out.println("((NAL_FUA))"); break;
			case NAL_FUB: //System.out.println("((NAL_FUB))"); break;
			default: //System.out.println("((WARNING: UNKNOWN_SLICE))"); break;
	    } // switch
	    */
	    /*
	    switch(this.nal_unit_type) {
			case NAL_SLICE: // DebugTool.printDebugString("((NAL_SLICE))\n"); break;
			case NAL_DPA: // DebugTool.printDebugString("((NAL_DPA))\n"); break;
			case NAL_DPB: // DebugTool.printDebugString("((NAL_DPB))\n"); break;
			case NAL_DPC: // DebugTool.printDebugString("((NAL_DPC))\n"); break;
			case NAL_IDR_SLICE: // DebugTool.printDebugString("((NAL_IDR_SLICE))\n"); break;
			case NAL_SEI: // DebugTool.printDebugString("((NAL_SEI))\n"); break;
			case NAL_SPS: // DebugTool.printDebugString("((NAL_SPS))\n"); break;
			case NAL_PPS: // DebugTool.printDebugString("((NAL_PPS))\n"); break;
			case NAL_AUD: // DebugTool.printDebugString("((NAL_AUD))\n"); break;
			case NAL_END_SEQUENCE: // DebugTool.printDebugString("((NAL_END_SEQUENCE))\n"); break;
			case NAL_END_STREAM: // DebugTool.printDebugString("((NAL_END_STREAM))\n"); break;
			case NAL_FILLER_DATA: // DebugTool.printDebugString("((NAL_FILLER_DATA))\n"); break;
			case NAL_SPS_EXT: // DebugTool.printDebugString("((NAL_SPS_EXT))\n"); break;
			case NAL_AUXILIARY_SLICE: // DebugTool.printDebugString("((NAL_AUXILIARY_SLICE))\n"); break;
			case NAL_FUA: // DebugTool.printDebugString("((NAL_FUA))\n"); break;
			case NAL_FUB: // DebugTool.printDebugString("((NAL_FUB))\n"); break;
			default: // DebugTool.printDebugString("((WARNING: UNKNOWN_SLICE))\n"); break;
	    } // switch
	    */
	    src_offset++; length--;

	//#   define RS 0
	    for(i=0; i+1<length; i+=2){
	        if(src_base[src_offset + i]!=0) continue;
	        if(i>0 && src_base[src_offset + i-1]==0) i--;
	//#endif
	        if(i+2<length && src_base[src_offset + i+1]==0 && src_base[src_offset + i+2]<=3){
	            if(src_base[src_offset + i+2]!=3){
	                /* startcode, so we must be past the end */
	                length=i;
	            }
	            break;
	        }
	        i-= 0; //RS;
	    }

	    if(i>=length-1){ //no escaped 0
	        dst_length_consumed[0] = length;
	        dst_length_consumed[1] = length+1; //+1 for the header
	        dst_length_consumed[2] = src_offset;
	        return src_base;
	    }

	    bufidx = (this.nal_unit_type == NAL_DPC ? 1 : 0); // use second escape buffer for inter data
	    //av_fast_malloc(&this.rbsp_buffer[bufidx], &this.rbsp_buffer_size[bufidx], length+MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE);
	    int[][] param1 = new int[][] {this.rbsp_buffer[bufidx]};
	    int[] param2 = new int[] {(int)this.rbsp_buffer_size[bufidx]};
	    av_fast_malloc(param1, param2, length+MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE);
	    this.rbsp_buffer[bufidx] = param1[0];
	    this.rbsp_buffer_size[bufidx] = param2[0];
	    dst= this.rbsp_buffer[bufidx];

	    if (dst == null){
	        return null;
	    }

	//printf("decoding esc\n");
	    //memcpy(dst, src, i);
	    System.arraycopy(src_base, src_offset, dst, 0, i);
	    si=di=i;
	    boolean flag1 = true;
	    while(si+2<length){
	        //remove escapes (very rare 1:2^22)
	        if(src_base[src_offset + si+2]>3){
	            dst[di++]= src_base[src_offset + si++];
	            dst[di++]= src_base[src_offset + si++];
	        }else if(src_base[src_offset + si]==0 && src_base[src_offset + si+1]==0){
	            if(src_base[src_offset + si+2]==3){ //escape
	                dst[di++]= 0;
	                dst[di++]= 0;
	                si+=3;
	                continue;
	            }else {//next start code
	                //goto nsc;
	            	flag1 = false;
	            	break;
	            } // if
	        }

	        dst[di++]= src_base[src_offset + si++];
	    }
	    if(flag1) {
		    while(si<length)
		        dst[di++]= src_base[src_offset + si++];
	    } // if flag1
//	nsc:

	    //memset(dst+di, 0, FF_INPUT_BUFFER_PADDING_SIZE);
		Arrays.fill(dst, di, di+MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE, 0);

	    dst_length_consumed[0]= di;
	    dst_length_consumed[1]= si + 1;//+1 for the header
	    dst_length_consumed[2] = dst_offset;
	//FIXME store exact number of bits in the getbitcontext (it is needed for decoding)
	    return dst;
	}
	
	public int ff_h264_decode_rbsp_trailing(/*const uint8_t *src*/ int[] src_base, int src_offset){
	    int v= src_base[src_offset];
	    int r;

	    //tprintf(h->s.avctx, "rbsp trailing %X\n", v);

	    for(r=1; r<9; r++){
	        if((v&1)!=0) return r;
	        v>>=1;
	    }
	    return 0;
	}	
	
	/**
	 * Mark a picture as no longer needed for reference. The refmask
	 * argument allows unreferencing of individual fields or the whole frame.
	 * If the picture becomes entirely unreferenced, but is being held for
	 * display purposes, it is marked as such.
	 * @param refmask mask of fields to unreference; the mask is bitwise
	 *                anded with the reference marking of pic
	 * @return non-zero if pic becomes entirely unreferenced (except possibly
	 *         for display purposes) zero if one of the fields remains in
	 *         reference
	 */
	public /*static inline*/ int unreference_pic(AVFrame pic, int refmask){
	    int i;
	    if ((pic.reference &= refmask)!=0) {
	        return 0;
	    } else {
	        for(i = 0; delayed_pic[i]!=null; i++)
	            if(pic == delayed_pic[i]){
	                pic.reference=DELAYED_PIC_REF;
	                break;
	            }
	        return 1;
	    }
	}	
	
	/**
	 * Remove a picture from the short term reference list by its index in
	 * that list.  This does no checking on the provided index; it is assumed
	 * to be valid. Other list entries are shifted down.
	 * @param i index into h->short_ref of picture to remove.
	 */
	public void remove_short_at_index(int i){
	    //assert(i >= 0 && i < this.short_ref_count);
	    this.short_ref[i]= null;
	    if ((--this.short_ref_count)!=0) {
	    	for(int j=i;j<this.short_ref_count;j++)
	    		this.short_ref[i] = this.short_ref[i+1];
	        //memmove(&h->short_ref[i], &h->short_ref[i+1], (h->short_ref_count - i)*sizeof(Picture*));
	    } // if
	}
	
	/**
	 *
	 * @return the removed picture or NULL if an error occurs
	 */
	public AVFrame remove_short(int frame_num, int ref_mask){
	    AVFrame pic;
	    int i;
	    //if(s->avctx->debug&FF_DEBUG_MMCO)
	    //    av_log(h->s.avctx, AV_LOG_DEBUG, "remove short %d count %d\n", frame_num, h->short_ref_count);
	    int[] param = new int[1];
	    pic = find_short(frame_num, param);
	    i = param[0];
	    if (pic!=null){
	        if(unreference_pic(pic, ref_mask)!=0)
	        remove_short_at_index(i);
	    }
	    return pic;
	}
	
	
	/**
	 * Remove a picture from the long term reference list by its index in
	 * that list.
	 * @return the removed picture or NULL if an error occurs
	 */
	public AVFrame remove_long(int i, int ref_mask) {
	    AVFrame pic;
	    pic= long_ref[i];
	    if (pic!=null){
	        if(unreference_pic(pic, ref_mask)!=0){
	            ////assert(h->long_ref[i]->long_ref == 1);
	            long_ref[i].long_ref= 0;
	            long_ref[i]= null;
	            long_ref_count--;
	        }
	    }
	    return pic;
	}
	
	void ff_h264_remove_all_refs(){
	    int i;

	    for(i=0; i<16; i++){
	        remove_long(i, 0);
	    }
	    ////assert(h->long_ref_count==0);
	    for(i=0; i<short_ref_count; i++){
	        unreference_pic(short_ref[i], 0);
	        short_ref[i]= null;
	    }
	    short_ref_count=0;
	}	
	
	/**
	 * instantaneous decoder refresh.
	 */
	public void idr(){
	    ff_h264_remove_all_refs();
	    this.prev_frame_num= 0;
	    this.prev_frame_num_offset= 0;
	    this.prev_poc_msb=
	    	this.prev_poc_lsb= 0;
	}

	////////////////////////////////////////////
	// SEI Decode Functions
	public int decode_picture_timing() {
	    if(this.sps.nal_hrd_parameters_present_flag!=0 || this.sps.vcl_hrd_parameters_present_flag!=0){
	        this.sei_cpb_removal_delay = (int)s.gb.get_bits( this.sps.cpb_removal_delay_length,"sei_cpb_removal_delay");
	        this.sei_dpb_output_delay = (int)s.gb.get_bits( this.sps.dpb_output_delay_length,"sei_dpb_output_delay");
	    }
	    if(this.sps.pic_struct_present_flag!=0){
	        /*unsigned */int i, num_clock_ts;
	        this.sei_pic_struct = (int)s.gb.get_bits( 4,"sei_pic_struct");
	        this.sei_ct_type    = 0;

	        if (this.sei_pic_struct > SEI_PIC_STRUCT_FRAME_TRIPLING)
	            return -1;

	        num_clock_ts = sei_num_clock_ts_table[this.sei_pic_struct];

	        for (i = 0 ; i < num_clock_ts ; i++){
	            if((int)s.gb.get_bits( 1,"clock_timestamp_flag")!=0){                  /* clock_timestamp_flag */
	                /*unsigned */int full_timestamp_flag;
	                this.sei_ct_type |= 1<<(int)s.gb.get_bits( 2,"sei_ct_type");
	                s.gb.skip_bits( 1);                 /* nuit_field_based_flag */
	                s.gb.skip_bits( 5);                 /* counting_type */
	                full_timestamp_flag = (int)s.gb.get_bits( 1,"full_timestamp_flag");
	                s.gb.skip_bits( 1);                 /* discontinuity_flag */
	                s.gb.skip_bits( 1);                 /* cnt_dropped_flag */
	                s.gb.skip_bits( 8);                 /* n_frames */
	                if(full_timestamp_flag!=0){
	                    s.gb.skip_bits( 6);             /* seconds_value 0..59 */
	                    s.gb.skip_bits( 6);             /* minutes_value 0..59 */
	                    s.gb.skip_bits( 5);             /* hours_value 0..23 */
	                }else{
	                    if((int)s.gb.get_bits( 1,"seconds_flag")!=0){          /* seconds_flag */
	                        s.gb.skip_bits( 6);         /* seconds_value range 0..59 */
	                        if((int)s.gb.get_bits( 1,"minutes_flag")!=0){      /* minutes_flag */
	                            s.gb.skip_bits( 6);     /* minutes_value 0..59 */
	                            if((int)s.gb.get_bits( 1,"hours_flag")!=0)   /* hours_flag */
	                                s.gb.skip_bits( 5); /* hours_value 0..23 */
	                        }
	                    }
	                }
	                if(this.sps.time_offset_length > 0)
	                    s.gb.skip_bits( this.sps.time_offset_length); /* time_offset */
	            }
	        }

	        //if(s.avctx.debug & FF_DEBUG_PICT_INFO)
	        //    av_log(s.avctx, AV_LOG_DEBUG, "ct_type:%X pic_struct:%d\n", this.sei_ct_type, this.sei_pic_struct);
	    }
	    return 0;
	}

	public int decode_unregistered_user_data(int size){
	    //uint8_t user_data[16+256];
		int[] user_data = new int[16+256];
	    int e, build, i;

	    if(size<16)
	        return -1;

	    for(i=0; i<user_data.length-1 && i<size; i++){
	        user_data[i]= (int)s.gb.get_bits( 8,"user_data");
	    }

	    user_data[i]= 0;
	    //?????????????????????????????????
	    //e= sscanf(user_data+16, "x264 - core %d"/*%s - H.264/MPEG-4 AVC codec - Copyleft 2005 - http://www.videolan.org/x264.html*/, &build);
	    e = 0;
	    build = 0;
	    if(e==1 && build>0)
	        this.x264_build= build;

	    //if(s.avctx.debug & FF_DEBUG_BUGS)
	    //    av_log(s.avctx, AV_LOG_DEBUG, "user data:\"%s\"\n", user_data+16);

	    for(; i<size; i++)
	        s.gb.skip_bits( 8);

	    return 0;
	}

	public int decode_recovery_point(){

	    this.sei_recovery_frame_cnt = s.gb.get_ue_golomb("sei_recovery_frame_cnt");
	    s.gb.skip_bits( 4);       /* 1b exact_match_flag, 1b broken_link_flag, 2b changing_slice_group_idc */

	    return 0;
	}

	public int decode_buffering_period(){
	    /*unsigned */int sps_id;
	    int sched_sel_idx;
	    SequenceParameterSet sps;

	    sps_id = s.gb.get_ue_golomb_31("sps_id");
	    if(sps_id > 31 || null==this.sps_buffers[sps_id]) {
	        //av_log(this.s.avctx, AV_LOG_ERROR, "non-existing SPS %d referenced in buffering period\n", sps_id);
	        return -1;
	    }
	    sps = this.sps_buffers[sps_id];

	    // NOTE: This is really so duplicated in the standard... See H.264, D.1.1
	    if (sps.nal_hrd_parameters_present_flag!=0) {
	        for (sched_sel_idx = 0; sched_sel_idx < sps.cpb_cnt; sched_sel_idx++) {
	            this.initial_cpb_removal_delay[sched_sel_idx] = (int)s.gb.get_bits( sps.initial_cpb_removal_delay_length,"initial_cpb_removal_delay");
	            s.gb.skip_bits( sps.initial_cpb_removal_delay_length); // initial_cpb_removal_delay_offset
	        }
	    }
	    if (sps.vcl_hrd_parameters_present_flag!=0) {
	        for (sched_sel_idx = 0; sched_sel_idx < sps.cpb_cnt; sched_sel_idx++) {
	            this.initial_cpb_removal_delay[sched_sel_idx] = (int)s.gb.get_bits( sps.initial_cpb_removal_delay_length,"initial_cpb_removal_delay");
	            s.gb.skip_bits( sps.initial_cpb_removal_delay_length); // initial_cpb_removal_delay_offset
	        }
	    }

	    this.sei_buffering_period_present = 1;
	    return 0;
	}
	
	public int ff_h264_decode_sei() {

	    while(s.gb.get_bits_count() + 16 < s.gb.size_in_bits){
	        int size, type;

	        type=0;
	        do{
	            type+= s.gb.show_bits(8);
	        }while(s.gb.get_bits(8,"sei?") == 255);

	        size=0;
	        do{
	            size+= s.gb.show_bits(8);
	        }while(s.gb.get_bits(8,"sei?") == 255);

	        switch(type){
	        case SEI_TYPE_PIC_TIMING: // Picture timing SEI
	            if(this.decode_picture_timing() < 0)
	                return -1;
	            break;
	        case SEI_TYPE_USER_DATA_UNREGISTERED:
	            if(this.decode_unregistered_user_data(size) < 0)
	                return -1;
	            break;
	        case SEI_TYPE_RECOVERY_POINT:
	            if(this.decode_recovery_point() < 0)
	                return -1;
	            break;
	        case SEI_BUFFERING_PERIOD:
	            if(this.decode_buffering_period() < 0)
	                return -1;
	            break;
	        default:
	        	s.gb.skip_bits(8*size);
	        }

	        //FIXME check bits here
	        s.gb.align_get_bits();
	    }	    return 0;
	}
	
	public void decode_scaling_list(/* uint8_t * */int[] factors, int size,
	/* const uint8_t * */int[] jvt_list, /* const uint8_t * */int[] fallback_list) {
		int i, last = 8, next = 8;
		/* const uint8_t * */int[] scan = (size == 16) ? zigzag_scan
				: ff_zigzag_direct;
		if (0l == s.gb.get_bits1("01?")) /*
									 * matrix not written, we use the predicted
									 * one
									 */
			// memcpy(factors, fallback_list, size*sizeof(uint8_t));
			System.arraycopy(fallback_list, 0, factors, 0, size);
		else
			for (i = 0; i < size; i++) {
				if (next != 0)
					next = (last + s.gb.get_se_golomb("next?")) & 0x0ff;
				if (0 == i && 0 == next) { /*
											 * matrix not written, we use the
											 * preset one
											 */
					// memcpy(factors, jvt_list, size*sizeof(uint8_t));
					System.arraycopy(jvt_list, 0, factors, 0, size);
					break;
				}
				last = factors[scan[i]] = (next != 0) ? next : last;
			}
	}

	public void decode_scaling_matrices(SequenceParameterSet sps,
			PictureParameterSet pps, int is_sps,
			/* uint8_t (*scaling_matrix4)[16] */int[][] scaling_matrix4, /*
																		 * uint8_t
																		 * (*
																		 * scaling_matrix8
																		 * )[64]
																		 */
			int[][] scaling_matrix8) {
		int fallback_sps = (0 == is_sps && 0 != sps.scaling_matrix_present) ? 1
				: 0;
		/* const uint8_t * */int[][] fallback = new int[][] {
				(fallback_sps != 0) ? sps.scaling_matrix4[0]
						: default_scaling4[0],
				(fallback_sps != 0) ? sps.scaling_matrix4[3]
						: default_scaling4[1],
				(fallback_sps != 0) ? sps.scaling_matrix8[0]
						: default_scaling8[0],
				(fallback_sps != 0) ? sps.scaling_matrix8[1]
						: default_scaling8[1] };
		if (0l != s.gb.get_bits1("01?")) {
			sps.scaling_matrix_present |= is_sps;
			decode_scaling_list(scaling_matrix4[0], 16, default_scaling4[0],
					fallback[0]); // Intra, Y
			decode_scaling_list(scaling_matrix4[1], 16, default_scaling4[0],
					scaling_matrix4[0]); // Intra, Cr
			decode_scaling_list(scaling_matrix4[2], 16, default_scaling4[0],
					scaling_matrix4[1]); // Intra, Cb
			decode_scaling_list(scaling_matrix4[3], 16, default_scaling4[1],
					fallback[1]); // Inter, Y
			decode_scaling_list(scaling_matrix4[4], 16, default_scaling4[1],
					scaling_matrix4[3]); // Inter, Cr
			decode_scaling_list(scaling_matrix4[5], 16, default_scaling4[1],
					scaling_matrix4[4]); // Inter, Cb
			if (is_sps != 0 || pps.transform_8x8_mode != 0) {
				decode_scaling_list(scaling_matrix8[0], 64,
						default_scaling8[0], fallback[2]); // Intra, Y
				decode_scaling_list(scaling_matrix8[1], 64,
						default_scaling8[1], fallback[3]); // Inter, Y
			}
		}
	}
	
	public int decode_hrd_parameters(SequenceParameterSet sps){
	    int cpb_count, i;
	    cpb_count = s.gb.get_ue_golomb_31("cpb_count") + 1;

	    if(cpb_count > 32){
	        //av_log(this.s.avctx, AV_LOG_ERROR, "cpb_count %d invalid\n", cpb_count);
	        return -1;
	    }

	    s.gb.get_bits( 4,"bit_rate_scale"); /* bit_rate_scale */
	    s.gb.get_bits( 4,"cpb_size_scale"); /* cpb_size_scale */
	    for(i=0; i<cpb_count; i++){
	        s.gb.get_ue_golomb("bit_rate_value_minus1"); /* bit_rate_value_minus1 */
	        s.gb.get_ue_golomb("cpb_size_value_minus1"); /* cpb_size_value_minus1 */
	        s.gb.get_bits1("cbr_flag");     /* cbr_flag */
	    }
	    sps.initial_cpb_removal_delay_length = (int)s.gb.get_bits( 5,"initial_cpb_removal_delay_length") + 1;
	    sps.cpb_removal_delay_length = (int)s.gb.get_bits( 5,"cpb_removal_delay_length") + 1;
	    sps.dpb_output_delay_length = (int)s.gb.get_bits( 5,"dpb_output_delay_length") + 1;
	    sps.time_offset_length = (int)s.gb.get_bits( 5,"time_offset_length");
	    sps.cpb_cnt = cpb_count;
	    return 0;
	}
	
	public int decode_vui_parameters(SequenceParameterSet sps){
	    int aspect_ratio_info_present_flag;
	    /*unsigned */int aspect_ratio_idc;

	    // DebugTool.printDebugString("decode_vui_parameters called..\n");
	    
	    aspect_ratio_info_present_flag= (int)s.gb.get_bits1("aspect_ratio_info_present_flag");

	    // DebugTool.printDebugString("    -- SPS: aspect_ratio_info_present_flag = "+aspect_ratio_info_present_flag+"\n");
	    
	    if( aspect_ratio_info_present_flag !=0) {
	        aspect_ratio_idc= (int)s.gb.get_bits( 8,"aspect_ratio_idc");
	        
		    // DebugTool.printDebugString("    -- decoding aspect_ratio_info..\n");
		    // DebugTool.printDebugString("    -- SPS: aspect_ratio_idc = "+aspect_ratio_idc+"\n");
	        
	        if( aspect_ratio_idc == EXTENDED_SAR ) {
	            sps.sar.num= (int)s.gb.get_bits( 16,"sar_num");
	            sps.sar.den= (int)s.gb.get_bits( 16,"sar_den");

	            // DebugTool.printDebugString("    -- SPS: num = "+sps.sar.num+"\n");
			    // DebugTool.printDebugString("    -- SPS: den = "+sps.sar.den+"\n");
	            
	        }else if(aspect_ratio_idc < pixel_aspect.length){
	            sps.sar=  pixel_aspect[aspect_ratio_idc];
	        }else{
	            //av_log(this.s.avctx, AV_LOG_ERROR, "illegal aspect ratio\n");
	            return -1;
	        }
	    }else{
	        sps.sar.num=
	        sps.sar.den= 0;
	    }
//	            s.avctx.aspect_ratio= sar_width*s.width / (float)(s.height*sar_height);

	    if(0!=(int)s.gb.get_bits1("overscan_info_present_flag")){      /* overscan_info_present_flag */
	    	s.gb.get_bits1("overscan_appropriate_flag");      /* overscan_appropriate_flag */
	    	
		    // DebugTool.printDebugString("    -- decoding overscan_info..\n");	    
	    }

	    sps.video_signal_type_present_flag = (int)s.gb.get_bits1("video_signal_type_present_flag");
	    if(0!=sps.video_signal_type_present_flag){
		    // DebugTool.printDebugString("    -- decoding video_signal_type..\n");

	    	
	    	s.gb.get_bits( 3,"video_signal_type_present_flag?");    /* video_format */
	        sps.full_range = (int)s.gb.get_bits1("full_range"); /* video_full_range_flag */

            // DebugTool.printDebugString("    -- SPS: full_range = "+sps.full_range+"\n");
	        
	        sps.colour_description_present_flag = (int)s.gb.get_bits1("colour_description_present_flag");
	        if(0!=sps.colour_description_present_flag){

			    // DebugTool.printDebugString("    -- decoding colour_description..\n");

	        	sps.color_primaries = (int)s.gb.get_bits( 8,"color_primaries"); /* colour_primaries */
	            sps.color_trc       = (int)s.gb.get_bits( 8,"color_trc"); /* transfer_characteristics */
	            sps.colorspace      = (int)s.gb.get_bits( 8,"colorspace"); /* matrix_coefficients */
	            if (sps.color_primaries >= MpegEncContext.AVCOL_PRI_NB)
	                sps.color_primaries  = MpegEncContext.AVCOL_PRI_UNSPECIFIED;
	            if (sps.color_trc >= MpegEncContext.AVCOL_TRC_NB)
	                sps.color_trc  = MpegEncContext.AVCOL_TRC_UNSPECIFIED;
	            if (sps.colorspace >= MpegEncContext.AVCOL_SPC_NB)
	                sps.colorspace  = MpegEncContext.AVCOL_SPC_UNSPECIFIED;
	        }
	    }

	    if(0!=(int)s.gb.get_bits1("chroma_location_info_present_flag")){      /* chroma_location_info_present_flag */
		    // DebugTool.printDebugString("    -- decoding chroma_location_info..\n");

	    	s.chroma_sample_location = s.gb.get_ue_golomb("chroma_sample_location")+1;  /* chroma_sample_location_type_top_field */
	        s.gb.get_ue_golomb("chroma_sample_location_type_bottom_field");  /* chroma_sample_location_type_bottom_field */
	    }

	    sps.timing_info_present_flag = (int)s.gb.get_bits1("timing_info_present_flag");
        // DebugTool.printDebugString("    -- SPS: timing_info_present_flag = "+sps.timing_info_present_flag+"\n");

	    if(0!=sps.timing_info_present_flag){
		    // DebugTool.printDebugString("    -- decoding timing_info..\n");

	    	sps.num_units_in_tick = s.gb.get_bits_long( 32,"num_units_in_tick");
	        sps.time_scale = s.gb.get_bits_long( 32,"time_scale");
	        
	        // DebugTool.printDebugString("        -- num_units_in_tick = "+sps.num_units_in_tick+", time_scale = "+sps.time_scale+"\n");
	        
	        if(0==sps.num_units_in_tick || 0==sps.time_scale){
	            //av_log(this.s.avctx, AV_LOG_ERROR, "time_scale/num_units_in_tick invalid or unsupported (%d/%d)\n", sps.time_scale, sps.num_units_in_tick);
	            return -1;
	        }
	        sps.fixed_frame_rate_flag = (int)s.gb.get_bits1("fixed_frame_rate_flag");
	    }

	    sps.nal_hrd_parameters_present_flag = (int)s.gb.get_bits1("nal_hrd_parameters_present_flag");
	    if(0!=sps.nal_hrd_parameters_present_flag)
	        if(this.decode_hrd_parameters(sps) < 0)
	            return -1;
	    sps.vcl_hrd_parameters_present_flag = (int)s.gb.get_bits1("vcl_hrd_parameters_present_flag");
	    if(0!=sps.vcl_hrd_parameters_present_flag)
	        if(this.decode_hrd_parameters(sps) < 0)
	            return -1;
	    if(0!=sps.nal_hrd_parameters_present_flag || 0!=sps.vcl_hrd_parameters_present_flag)
	        s.gb.get_bits1("low_delay_hrd_flag");     /* low_delay_hrd_flag */
	    sps.pic_struct_present_flag = (int)s.gb.get_bits1("pic_struct_present_flag");

	    sps.bitstream_restriction_flag = (int)s.gb.get_bits1("bitstream_restriction_flag");
	    if(0!=sps.bitstream_restriction_flag){
	        s.gb.get_bits1("motion_vectors_over_pic_boundaries_flag");     /* motion_vectors_over_pic_boundaries_flag */
	        s.gb.get_ue_golomb("max_bytes_per_pic_denom"); /* max_bytes_per_pic_denom */
	        s.gb.get_ue_golomb("max_bits_per_mb_denom"); /* max_bits_per_mb_denom */
	        s.gb.get_ue_golomb("log2_max_mv_length_horizontal"); /* log2_max_mv_length_horizontal */
	        s.gb.get_ue_golomb("log2_max_mv_length_vertical"); /* log2_max_mv_length_vertical */
	        sps.num_reorder_frames= s.gb.get_ue_golomb("num_reorder_frames");
	        s.gb.get_ue_golomb("get_ue_golomb"); /*max_dec_frame_buffering*/

	        if(s.gb.size_in_bits < s.gb.get_bits_count()){
	            //av_log(this.s.avctx, AV_LOG_ERROR, "Overread VUI by %d bits\n", get_bits_count(&s.gb) - s.gb.size_in_bits);
	            sps.num_reorder_frames=0;
	            sps.bitstream_restriction_flag= 0;
	        }

	        if(sps.num_reorder_frames > 16 /*max_dec_frame_buffering || max_dec_frame_buffering > 16*/){
	            //av_log(this.s.avctx, AV_LOG_ERROR, "illegal num_reorder_frames %d\n", sps.num_reorder_frames);
	            return -1;
	        }
	    }

	    return 0;
	}
	
	public static void build_qp_table(PictureParameterSet pps, int t, int index)
	{
	    int i;
	    for(i = 0; i < 52; i++)
	        pps.chroma_qp_table[t][i] = ff_h264_chroma_qp[av_clip(i + index, 0, 51)];
	}	
		
	////////////////////////////
	// Decode PPS
	public int ff_h264_decode_picture_parameter_set(int bit_length){
	    /*unsigned */int pps_id= s.gb.get_ue_golomb("pps_id");
	    PictureParameterSet pps;
	
	    if(pps_id >= MAX_PPS_COUNT) {
	        //av_log(this.s.avctx, AV_LOG_ERROR, "pps_id (%d) out of range\n", pps_id);
	        return -1;
	    }
	
	    pps= new PictureParameterSet();
	    if(pps == null)
	        return -1;
	    pps.sps_id= s.gb.get_ue_golomb_31("sps_id");
	    if(/*(unsigned)*/pps.sps_id>=MAX_SPS_COUNT || this.sps_buffers[(int)pps.sps_id] == null){
	        //av_log(this.s.avctx, AV_LOG_ERROR, "sps_id out of range\n");
	        return -1;
	    }
	
	    pps.cabac= (int)s.gb.get_bits1("cabac");
	    pps.pic_order_present= (int)s.gb.get_bits1("pic_order_present");
	    pps.slice_group_count= s.gb.get_ue_golomb("slice_group_count") + 1;
	    if(pps.slice_group_count > 1 ){
	        pps.mb_slice_group_map_type= s.gb.get_ue_golomb("mb_slice_group_map_type");
	        //av_log(this.s.avctx, AV_LOG_ERROR, "FMO not supported\n");
	        switch(pps.mb_slice_group_map_type){
	        case 0:
	            break;
	        case 2:
	            break;
	        case 3:
	        case 4:
	        case 5:
	            break;
	        case 6:
	            break;
	        }
	    }
	    pps.ref_count[0]= s.gb.get_ue_golomb("ref_count[0]") + 1;
	    pps.ref_count[1]= s.gb.get_ue_golomb("ref_count[1]") + 1;
	    if(pps.ref_count[0]-1 > 32-1 || pps.ref_count[1]-1 > 32-1){
	        //av_log(this.s.avctx, AV_LOG_ERROR, "reference overflow (pps)\n");
	        return -1;
	    }
	
	    pps.weighted_pred= (int)s.gb.get_bits1("weighted_pred");
	    pps.weighted_bipred_idc= (int)s.gb.get_bits( 2,"weighted_bipred_idc");
	    pps.init_qp= s.gb.get_se_golomb("init_qp") + 26;
	    pps.init_qs= s.gb.get_se_golomb("init_qs") + 26;
	    pps.chroma_qp_index_offset[0]= s.gb.get_se_golomb("chroma_qp_index_offset[0]");
	    pps.deblocking_filter_parameters_present= (int)s.gb.get_bits1("deblocking_filter_parameters_present");
	    pps.constrained_intra_pred= (int)s.gb.get_bits1("constrained_intra_pred");
	    pps.redundant_pic_cnt_present = (int)s.gb.get_bits1("redundant_pic_cnt_present");
	
	    pps.transform_8x8_mode= 0;
	    this.dequant_coeff_pps= -1; //contents of sps/pps can change even if id doesn't, so reinit
	    //memcpy(pps.scaling_matrix4, this.sps_buffers[(int)pps.sps_id].scaling_matrix4, sizeof(pps.scaling_matrix4));
	    //memcpy(pps.scaling_matrix8, this.sps_buffers[(int)pps.sps_id].scaling_matrix8, sizeof(pps.scaling_matrix8));
	    for(int k=0;k<this.sps_buffers[(int)pps.sps_id].scaling_matrix4.length;k++)
	    	System.arraycopy(this.sps_buffers[(int)pps.sps_id].scaling_matrix4[k], 0, pps.scaling_matrix4[k], 0, this.sps_buffers[(int)pps.sps_id].scaling_matrix4[k].length);
	    for(int k=0;k<this.sps_buffers[(int)pps.sps_id].scaling_matrix8.length;k++)
	    	System.arraycopy(this.sps_buffers[(int)pps.sps_id].scaling_matrix8[k], 0, pps.scaling_matrix8[k], 0, this.sps_buffers[(int)pps.sps_id].scaling_matrix8[k].length);
	
	    if(s.gb.get_bits_count() < bit_length){
	        pps.transform_8x8_mode= (int)s.gb.get_bits1("transform_8x8_mode");
	        decode_scaling_matrices(this.sps_buffers[(int)pps.sps_id], pps, 0, pps.scaling_matrix4, pps.scaling_matrix8);
	        pps.chroma_qp_index_offset[1]= s.gb.get_se_golomb("chroma_qp_index_offset[1]"); //second_chroma_qp_index_offset
	    } else {
	        pps.chroma_qp_index_offset[1]= pps.chroma_qp_index_offset[0];
	    }
	
	    build_qp_table(pps, 0, pps.chroma_qp_index_offset[0]);
	    build_qp_table(pps, 1, pps.chroma_qp_index_offset[1]);
	    if(pps.chroma_qp_index_offset[0] != pps.chroma_qp_index_offset[1])
	        pps.chroma_qp_diff= 1;
	
	    /*
	    if(s.avctx.debug&FF_DEBUG_PICT_INFO){
	        av_log(this.s.avctx, AV_LOG_DEBUG, "pps:%u sps:%u %s slice_groups:%d ref:%d/%d %s qp:%d/%d/%d/%d %s %s %s %s\n",
	               pps_id, pps.sps_id,
	               pps.cabac ? "CABAC" : "CAVLC",
	               pps.slice_group_count,
	               pps.ref_count[0], pps.ref_count[1],
	               pps.weighted_pred ? "weighted" : "",
	               pps.init_qp, pps.init_qs, pps.chroma_qp_index_offset[0], pps.chroma_qp_index_offset[1],
	               pps.deblocking_filter_parameters_present ? "LPAR" : "",
	               pps.constrained_intra_pred ? "CONSTR" : "",
	               pps.redundant_pic_cnt_present ? "REDU" : "",
	               pps.transform_8x8_mode ? "8x8DCT" : ""
	               );
	    }
		*/
	    this.pps_buffers[pps_id]= null;
	    this.pps_buffers[pps_id]= pps;
	    return 0;
	}
		
	
	////////////////////////////
	// Decode SPS
	public int ff_h264_decode_seq_parameter_set(){
	    int profile_idc, level_idc;
	    /*unsigned */int sps_id;
	    int i;
	    SequenceParameterSet sps;

	    profile_idc= (int)s.gb.get_bits( 8,"profile_idc");
	    s.gb.get_bits1("constraint_set0_flag");   //constraint_set0_flag
	    s.gb.get_bits1("constraint_set1_flag");   //constraint_set1_flag
	    s.gb.get_bits1("constraint_set2_flag");   //constraint_set2_flag
	    s.gb.get_bits1("constraint_set3_flag");   //constraint_set3_flag
	    s.gb.get_bits( 4,"reserved"); // reserved
	    level_idc= (int)s.gb.get_bits( 8,"level_idc");
	    sps_id= s.gb.get_ue_golomb_31("sps_id");

	    if(sps_id >= MAX_SPS_COUNT) {
	        //av_log(this.s.avctx, AV_LOG_ERROR, "sps_id (%d) out of range\n", sps_id);
	        return -1;
	    }
	    sps= new SequenceParameterSet();
	    if(sps == null)
	        return -1;

	    sps.time_offset_length = 24;
	    sps.profile_idc= profile_idc;
	    sps.level_idc= level_idc;
	    //System.out.println("Decode SPS, Profile: "+profile_idc+", Level: "+level_idc);

	    //memset(sps.scaling_matrix4, 16, sizeof(sps.scaling_matrix4));
	    //memset(sps.scaling_matrix8, 16, sizeof(sps.scaling_matrix8));
	    for(int k=0;k<sps.scaling_matrix4.length;k++)
	    	Arrays.fill(sps.scaling_matrix4[k], 16);
	    for(int k=0;k<sps.scaling_matrix8.length;k++)
	    	Arrays.fill(sps.scaling_matrix8[k], 16);

	    sps.scaling_matrix_present = 0;

	    if(sps.profile_idc >= 100){ //high profile
	        sps.chroma_format_idc= s.gb.get_ue_golomb_31("chroma_format_idc");
	        if(sps.chroma_format_idc == 3)
	            sps.residual_color_transform_flag = (int)s.gb.get_bits1("residual_color_transform_flag");
	        sps.bit_depth_luma   = s.gb.get_ue_golomb("bit_depth_luma") + 8;
	        sps.bit_depth_chroma = s.gb.get_ue_golomb("bit_depth_chroma") + 8;
	        sps.transform_bypass = (int)s.gb.get_bits1("transform_bypass");
	        decode_scaling_matrices(sps, null, 1, sps.scaling_matrix4, sps.scaling_matrix8);
	    }else{
	        sps.chroma_format_idc= 1;
	        sps.bit_depth_luma   = 8;
	        sps.bit_depth_chroma = 8;
	    }

	    sps.log2_max_frame_num= s.gb.get_ue_golomb("log2_max_frame_num") + 4;
	    sps.poc_type= s.gb.get_ue_golomb_31("poc_type");
	    
	    // DebugTool.printDebugString("SPS: log2_max_frame_num = "+sps.log2_max_frame_num+"\n");
	    // DebugTool.printDebugString("SPS: poc_type = "+sps.poc_type+"\n");
	    

	    if(sps.poc_type == 0){ //FIXME #define
	        sps.log2_max_poc_lsb= s.gb.get_ue_golomb("log2_max_poc_lsb") + 4;
	        
		    // DebugTool.printDebugString("SPS: log2_max_poc_lsb = "+sps.log2_max_poc_lsb+"\n");
	    } else if(sps.poc_type == 1){//FIXME #define
	        sps.delta_pic_order_always_zero_flag= (int)s.gb.get_bits1("delta_pic_order_always_zero_flag");
	        sps.offset_for_non_ref_pic= s.gb.get_se_golomb("offset_for_non_ref_pic");
	        sps.offset_for_top_to_bottom_field= s.gb.get_se_golomb("offset_for_top_to_bottom_field");
	        sps.poc_cycle_length                = s.gb.get_ue_golomb("poc_cycle_length");

		    // DebugTool.printDebugString("SPS: delta_pic_order_always_zero_flag = "+sps.delta_pic_order_always_zero_flag+"\n");
		    // DebugTool.printDebugString("SPS: offset_for_non_ref_pic = "+sps.offset_for_non_ref_pic+"\n");
		    // DebugTool.printDebugString("SPS: offset_for_top_to_bottom_field = "+sps.offset_for_top_to_bottom_field+"\n");
		    // DebugTool.printDebugString("SPS: poc_cycle_length = "+sps.poc_cycle_length+"\n");
	        
	        if(/*(unsigned)*/sps.poc_cycle_length >= sps.offset_for_ref_frame.length){
	            //av_log(this.s.avctx, AV_LOG_ERROR, "poc_cycle_length overflow %u\n", sps.poc_cycle_length);
	            return -1;
	        }

	        for(i=0; i<sps.poc_cycle_length; i++) {
	            sps.offset_for_ref_frame[i]= (short)s.gb.get_se_golomb("offset_for_ref_frame");

	            // DebugTool.printDebugString("SPS: offset_for_ref_frame["+i+"] = "+sps.offset_for_ref_frame[i]+"\n");
	        } // if
	    }else if(sps.poc_type != 2){
	        //av_log(this.s.avctx, AV_LOG_ERROR, "illegal POC type %d\n", sps.poc_type);
	        return -1;
	    }

	    sps.ref_frame_count= s.gb.get_ue_golomb_31("ref_frame_count");
	    // DebugTool.printDebugString("SPS: ref_frame_count = "+sps.ref_frame_count+"\n");

	    if(sps.ref_frame_count > MpegEncContext.MAX_PICTURE_COUNT-2 || sps.ref_frame_count >= 32){
	        //av_log(this.s.avctx, AV_LOG_ERROR, "too many reference frames\n");
	        return -1;
	    }
	    sps.gaps_in_frame_num_allowed_flag= (int)s.gb.get_bits1("gaps_in_frame_num_allowed_flag");
	    sps.mb_width = s.gb.get_ue_golomb("mb_width") + 1;
	    sps.mb_height= s.gb.get_ue_golomb("mb_height") + 1;
	    if(/*(unsigned)*/sps.mb_width >= Integer.MAX_VALUE/16 || /*(unsigned)*/sps.mb_height >= Integer.MAX_VALUE/16 ||
	       MpegEncContext.av_image_check_size(16*sps.mb_width, 16*sps.mb_height, 0, this.s)!=0){
	        //av_log(this.s.avctx, AV_LOG_ERROR, "mb_width/height overflow\n");
	        return -1;
	    }
	    
	    // DebugTool.printDebugString("SPS: gaps_in_frame_num_allowed_flag = "+sps.gaps_in_frame_num_allowed_flag+"\n");
	    // DebugTool.printDebugString("SPS: mb_width = "+sps.mb_width+"\n");
	    // DebugTool.printDebugString("SPS: mb_height = "+sps.mb_height+"\n");


	    sps.frame_mbs_only_flag= (int)s.gb.get_bits1("frame_mbs_only_flag");
	    if(0==sps.frame_mbs_only_flag)
	        sps.mb_aff= (int)s.gb.get_bits1("mb_aff");
	    else
	        sps.mb_aff= 0;

	    // DebugTool.printDebugString("SPS: frame_mbs_only_flag = "+sps.frame_mbs_only_flag+"\n");
	    // DebugTool.printDebugString("SPS: mb_aff = "+sps.mb_aff+"\n");

	    sps.direct_8x8_inference_flag= (int)s.gb.get_bits1("direct_8x8_inference_flag");
	    if(0==sps.frame_mbs_only_flag && 0==sps.direct_8x8_inference_flag){
	        //av_log(this.s.avctx, AV_LOG_ERROR, "This stream was generated by a broken encoder, invalid 8x8 inference\n");
	        return -1;
	    }

	    // DebugTool.printDebugString("SPS: direct_8x8_inference_flag = "+sps.direct_8x8_inference_flag+"\n");
	    
//	    if(sps.mb_aff)
//	        av_log(this.s.avctx, AV_LOG_ERROR, "MBAFF support not included; enable it at compile-time.\n");
	    sps.crop= (int)s.gb.get_bits1("crop");
	    if(sps.crop!=0){
	        sps.crop_left  = s.gb.get_ue_golomb("crop_left");
	        sps.crop_right = s.gb.get_ue_golomb("crop_right");
	        sps.crop_top   = s.gb.get_ue_golomb("crop_top");
	        sps.crop_bottom= s.gb.get_ue_golomb("crop_bottom");
	        if(sps.crop_left!=0 || sps.crop_top!=0){
	            //av_log(this.s.avctx, AV_LOG_ERROR, "insane cropping not completely supported, this could look slightly wrong ...\n");
	        }
	        if(sps.crop_right >= 8 || sps.crop_bottom >= 8){
	            //av_log(this.s.avctx, AV_LOG_ERROR, "brainfart cropping not supported, this could look slightly wrong ...\n");
	        }
	    }else{
	        sps.crop_left  =
	        sps.crop_right =
	        sps.crop_top   =
	        sps.crop_bottom= 0;
	    }

	    // DebugTool.printDebugString("SPS: crop = "+sps.crop+"\n");
	    // DebugTool.printDebugString("SPS: crop_left = "+sps.crop_left+"\n");
	    // DebugTool.printDebugString("SPS: crop_right = "+sps.crop_right+"\n");
	    // DebugTool.printDebugString("SPS: crop_top = "+sps.crop_top+"\n");
	    // DebugTool.printDebugString("SPS: crop_bottom = "+sps.crop_bottom+"\n");
	    
	    sps.vui_parameters_present_flag= (int)s.gb.get_bits1("vui_parameters_present_flag");
	    if( 0!=sps.vui_parameters_present_flag )
	        if (this.decode_vui_parameters(sps) < 0)
	            return -1;

	    if(0==sps.sar.den)
	        sps.sar.den= 1;

	    /*
	    if(s.avctx.debug&FF_DEBUG_PICT_INFO){
	        av_log(this.s.avctx, AV_LOG_DEBUG, "sps:%u profile:%d/%d poc:%d ref:%d %dx%d %s %s crop:%d/%d/%d/%d %s %s %d/%d\n",
	               sps_id, sps.profile_idc, sps.level_idc,
	               sps.poc_type,
	               sps.ref_frame_count,
	               sps.mb_width, sps.mb_height,
	               sps.frame_mbs_only_flag ? "FRM" : (sps.mb_aff ? "MB-AFF" : "PIC-AFF"),
	               sps.direct_8x8_inference_flag ? "8B8" : "",
	               sps.crop_left, sps.crop_right,
	               sps.crop_top, sps.crop_bottom,
	               sps.vui_parameters_present_flag ? "VUI" : "",
	               ((char*[]){"Gray","420","422","444"})[sps.chroma_format_idc],
	               sps.timing_info_present_flag ? sps.num_units_in_tick : 0,
	               sps.timing_info_present_flag ? sps.time_scale : 0
	               );
	    }
	    */

	    //av_free(this.sps_buffers[sps_id]);
	    this.sps_buffers[sps_id]= sps;
	    this.sps = sps;
	    return 0;
	}
	
	
	////////////////////////////
	// Decode NAL Units		
	public int decode_nal_units( /*const uint8_t *buf*/int[] buf_base, int buf_offset, int buf_size){
	    int buf_index=0;
	    H264Context hx = this; ///< thread context
	    int context_count = 0;
	    int next_avc= ((this.is_avc!=0) ? 0 : buf_size);

	    this.max_contexts = /*avctx.thread_count*/ 1;
	    if(0==(s.flags2 & MpegEncContext.CODEC_FLAG2_CHUNKS)){
	        this.current_slice = 0;
	        if (0==s.first_field)
	            s.current_picture_ptr= null;
	        SEIDecoder.ff_h264_reset_sei(this);
	    }

	    for(;;){
	        int consumed;
	        int dst_length;
	        int bit_length;
	        //const uint8_t *ptr;
	        int[] ptr_base;
	        int ptr_offset;
	        int i, nalsize = 0;
	        int err;

	        if(buf_index >= next_avc) {
	            if(buf_index >= buf_size) break;
	            nalsize = 0;
	            for(i = 0; i < this.nal_length_size; i++)
	                nalsize = (nalsize << 8) | buf_base[buf_offset + buf_index++];
	            if(nalsize <= 0 || nalsize > buf_size - buf_index){
	                //av_log(this.s.avctx, AV_LOG_ERROR, "AVC: nal size %d\n", nalsize);
	                break;
	            }
	            next_avc= buf_index + nalsize;
	        } else {
	            // start code prefix search
	            for(; buf_index + 3 < next_avc; buf_index++){
	                // This should always succeed in the first iteration.
	                if(buf_base[buf_offset + buf_index] == 0 && buf_base[buf_offset + buf_index+1] == 0 && buf_base[buf_offset + buf_index+2] == 1)
	                    break;
	            }

	            if(buf_index+3 >= buf_size) break;

	            buf_index+=3;
	            if(buf_index >= next_avc) continue;
	        }

	        hx = this.thread_context[context_count];

	        int[] param = new int[3];
	        ptr_base= hx.ff_h264_decode_nal(buf_base, buf_offset + buf_index, param, next_avc - buf_index);
	        dst_length = param[0];
	        consumed = param[1];
	        ptr_offset = param[2];
	        if (ptr_base==null || dst_length < 0){
	            return -1;
	        }
	        i= buf_index + consumed;
	        if((s.workaround_bugs & MpegEncContext.FF_BUG_AUTODETECT)!=0 && i+3<next_avc &&
	           buf_base[buf_offset + i]==0x00 && buf_base[buf_offset + i+1]==0x00 && buf_base[buf_offset + i+2]==0x01 && buf_base[buf_offset + i+3]==0x0E0)
	            s.workaround_bugs |= MpegEncContext.FF_BUG_TRUNCATED;

	        if(0==(s.workaround_bugs & MpegEncContext.FF_BUG_TRUNCATED)){
	        while(ptr_base[ptr_offset + dst_length - 1] == 0 && dst_length > 0)
	            dst_length--;
	        }
	        bit_length= (dst_length==0) ? 0 : (8*dst_length - ff_h264_decode_rbsp_trailing(ptr_base, ptr_offset + dst_length - 1));

	        //if(s.s.debug!=0&FF_DEBUG_STARTCODE){
	            //av_log(this.s.avctx, AV_LOG_DEBUG, "NAL %d at %d/%d length %d\n", hx.nal_unit_type, buf_index, buf_size, dst_length);
	        //}

	        if (this.is_avc!=0 && (nalsize != consumed) && nalsize!=0){
	            //av_log(this.s.avctx, AV_LOG_DEBUG, "AVC: Consumed only %d bytes instead of %d\n", consumed, nalsize);
	        }

	        buf_index += consumed;

	        if(  (s.hurry_up == 1 && this.nal_ref_idc  == 0) //FIXME do not discard SEI id
	           ||(s.skip_frame >= MpegEncContext.AVDISCARD_NONREF && this.nal_ref_idc  == 0))
	            continue;

	      boolean doAgain = false;
	      do {
	    	  doAgain = false;
		      //again:
		        err = 0;
		        switch(hx.nal_unit_type){
		        case NAL_IDR_SLICE:
		        	// DebugTool.printDebugString("Decoding NAL_IDR_SLICE...\n");

		        	if (this.nal_unit_type != NAL_IDR_SLICE) {
			        	// DebugTool.printDebugString("*Decoding NAL_IDR_SLICE return -1...\n");
		                //av_log(this.s.avctx, AV_LOG_ERROR, "Invalid mix of idr and non-idr slices");
		                return -1;
		            }
		            idr(); //FIXME ensure we don't loose some frames if there is reordering
		        case NAL_SLICE:
		        	// DebugTool.printDebugString("Decoding NAL_SLICE...\n");

		        	hx.s.gb.init_get_bits(ptr_base, ptr_offset, bit_length);
		            hx.intra_gb_ptr=
		            hx.inter_gb_ptr= hx.s.gb;
		            hx.s.data_partitioning = 0;
	
		            err = decode_slice_header(hx, this);
		            // DebugTool.printDebugString("*decode_slice_header returns "+err+"...\n");
		            if((err)!=0)
		               break;

	
		            if (this.current_slice == 1) {
		            	/* No H/W Acceleration
		                if (s.s.hwaccel && s.s.hwaccel.start_frame(s.avctx, NULL, 0) < 0)
		                    return -1;
		                if(CONFIG_H264_VDPAU_DECODER && s.avctx,codec->capabilities&CODEC_CAP_HWACCEL_VDPAU)
		                    ff_vdpau_h264_picture_start(s);
		                 */
		            }
	
		            s.current_picture_ptr.key_frame |=
		                    (((hx.nal_unit_type == NAL_IDR_SLICE) ||
		                    (this.sei_recovery_frame_cnt >= 0))?1:0);
		            if(hx.redundant_pic_count==0 && hx.s.hurry_up < 5
		               && (s.skip_frame < MpegEncContext.AVDISCARD_NONREF || hx.nal_ref_idc!=0)
		               && (s.skip_frame < MpegEncContext.AVDISCARD_BIDIR  || hx.slice_type_nos!=FF_B_TYPE)
		               && (s.skip_frame < MpegEncContext.AVDISCARD_NONKEY || hx.slice_type_nos==FF_I_TYPE)
		               && s.skip_frame < MpegEncContext.AVDISCARD_ALL){
		                if(s.hwaccel!=0) {
		                	//System.out.println("Attempt to exec H/W Acceleration codes.");
		                    //if (avctx->hwaccel->decode_slice(avctx, &buf[buf_index - consumed], consumed) < 0)
		                    //    return -1;
		                }else
		                /*?????????????????????????????/
		                if(CONFIG_H264_VDPAU_DECODER!=- && s.s.codec.capabilities&CODEC_CAP_HWACCEL_VDPAU!=0){
		                    static const uint8_t start_code[] = {0x00, 0x00, 0x01};
		                    ff_vdpau_add_data_chunk(s, start_code, sizeof(start_code));
		                    ff_vdpau_add_data_chunk(s, &buf[buf_index - consumed], consumed );
		                }else
		                */ {
				        	// DebugTool.printDebugString("*context_count++\n");
		                    context_count++;
		                } // if
		            }
		            break;
		        case NAL_DPA:
		        	// DebugTool.printDebugString("Decoding NAL_DPA...\n");

		        	hx.s.gb.init_get_bits(ptr_base, ptr_offset, bit_length);
		            hx.intra_gb_ptr=
		            hx.inter_gb_ptr= null;

		            err = decode_slice_header(hx, this);
		            // DebugTool.printDebugString("*decode_slice_header returns "+err+"...\n");
		            if ((err) < 0)
		                break;
	
		            hx.s.data_partitioning = 1;
	
		            break;
		        case NAL_DPB:
		        	hx.intra_gb = new GetBitContext();
		        	hx.intra_gb.init_get_bits( ptr_base, ptr_offset, bit_length);
		            hx.intra_gb_ptr= hx.intra_gb;
		            break;
		        case NAL_DPC:
		        	// DebugTool.printDebugString("Decoding NAL_DPC...\n");

		        	hx.inter_gb.init_get_bits(ptr_base, ptr_offset, bit_length);
		            hx.inter_gb_ptr= hx.inter_gb;
	
		            if(hx.redundant_pic_count==0 && hx.intra_gb_ptr!=null && hx.s.data_partitioning!=0
		               && s.context_initialized!=0
		               && s.hurry_up < 5
		               && (s.skip_frame < MpegEncContext.AVDISCARD_NONREF || hx.nal_ref_idc!=0)
		               && (s.skip_frame < MpegEncContext.AVDISCARD_BIDIR  || hx.slice_type_nos!=FF_B_TYPE)
		               && (s.skip_frame < MpegEncContext.AVDISCARD_NONKEY || hx.slice_type_nos==FF_I_TYPE)
		               && s.skip_frame < MpegEncContext.AVDISCARD_ALL) {
		                context_count++;
			        	// DebugTool.printDebugString("*context_count++\n");
		            } // if 
		            break;
		        case NAL_SEI:
		        	// DebugTool.printDebugString("Decoding NAL_SEI...\n");

		        	s.gb.init_get_bits(ptr_base, ptr_offset, bit_length);
		            ff_h264_decode_sei();
		            break;
		        case NAL_SPS:
		        	// DebugTool.printDebugString("Decoding NAL_SPS...\n");

		        	s.gb.init_get_bits(ptr_base, ptr_offset, bit_length);
		            ff_h264_decode_seq_parameter_set();
	
		            if((s.flags& MpegEncContext.CODEC_FLAG_LOW_DELAY)!=0)
		                s.low_delay=1;
	
		            if(s.has_b_frames < 2)
		                s.has_b_frames= ((0==s.low_delay)?1:0);
		            break;
		        case NAL_PPS:
		        	// DebugTool.printDebugString("Decoding NAL_PPS...\n");
		        	
		        	s.gb.init_get_bits( ptr_base, ptr_offset, bit_length);
	
		            ff_h264_decode_picture_parameter_set(bit_length);
	
		            break;
		        case NAL_AUD:
		        case NAL_END_SEQUENCE:
		        case NAL_END_STREAM:
		        case NAL_FILLER_DATA:
		        case NAL_SPS_EXT:
		        case NAL_AUXILIARY_SLICE:
		            break;
		        default:
		            //av_log(avctx, AV_LOG_DEBUG, "Unknown NAL code: %d (%d bits)\n", hx.nal_unit_type, bit_length);
		        }

	        	// DebugTool.printDebugString(" ---- context_count="+context_count+", max_contexts="+this.max_contexts+"\n");
		        
		        if(context_count == this.max_contexts) {

		        	// DebugTool.printDebugString("decode_slice(2) context_count == h->max_contexts\n");

			    	decode_slice();
		            //execute_decode_slices(context_count); // This is for multi-thread only
		            context_count = 0;
		        }
		        
		        // DebugTool.printDebugString("**** result from execute_decode_slices = "+err+"\n");
	
		        if (err < 0) {
		            // av_log(this.s.avctx, AV_LOG_ERROR, "decode_slice_header error\n");
		        } else if(err == 1) {
		            /* Slice could not be decoded in parallel mode, copy down
		             * NAL unit stuff to context 0 and restart. Note that
		             * rbsp_buffer is not transferred, but since we no longer
		             * run in parallel mode this should not be an issue. */
		            this.nal_unit_type = hx.nal_unit_type;
		            this.nal_ref_idc   = hx.nal_ref_idc;
		            hx = this;
		            doAgain = true;
		        }
		        
		        // DebugTool.dumpDebugFrameData(this, "decode_nal_units");

	      } while(doAgain == true);

	    }
	    // DebugTool.printDebugString("context_count = "+context_count+"\n");
	    if(context_count!=0)
	    	decode_slice();
	        //execute_decode_slices(context_count); // This is for multi-thread only
	    return buf_index;
	}
	
	/**
	 * Find a Picture in the short term reference list by frame number.
	 * @param frame_num frame number to search for
	 * @param idx the index into this.short_ref where returned picture is found
	 *            undefined if no picture found.
	 * @return pointer to the found picture, or NULL if no pic with the provided
	 *                 frame number is found
	 */
	// idx[0] = idx:int (in/out) param
	public AVFrame find_short(int frame_num, int[] idx){
	    int i;

	    for(i=0; i<this.short_ref_count; i++){
	        AVFrame pic= this.short_ref[i];
	        //if(s->avctx->debug&FF_DEBUG_MMCO)
	        //    av_log(this.s.avctx, AV_LOG_DEBUG, "%d %d %p\n", i, pic->frame_num, pic);
	        if(pic.frame_num == frame_num) {
	            idx[0] = i;
	            return pic;
	        }
	    }
	    return null;
	}
	
	/**
	 * Extract structure information about the picture described by pic_num in
	 * the current decoding context (frame or field). Note that pic_num is
	 * picture number without wrapping (so, 0<=pic_num<max_pic_num).
	 * @param pic_num picture number for which to extract structure information
	 * @param structure one of PICT_XXX describing structure of picture
	 *                      with pic_num
	 * @return frame number (short term) or long term index of picture
	 *         described by pic_num
	 */
	public int pic_num_extract(int pic_num, int[] structure){
	    structure[0] = s.picture_structure;
	    if((s.picture_structure != MpegEncContext.PICT_FRAME)){
	        if (0==(pic_num & 1))
	            /* opposite field */
	            structure[0] ^= MpegEncContext.PICT_FRAME;
	        pic_num >>= 1;
	    }
	    return pic_num;
	}
	
	public int ff_h264_execute_ref_pic_marking(MMCO[] mmco, int mmco_count){
	    int i, j=0;
	    int current_ref_assigned=0;
	    AVFrame pic=null;

	    /*
	    if((s.debug&FF_DEBUG_MMCO) && mmco_count==0)
	        av_log(this.s.avctx, AV_LOG_DEBUG, "no mmco here\n");
		*/
	    for(i=0; i<mmco_count; i++){
	        int structure=0, frame_num=0;
	        //if(s.avctx->debug&FF_DEBUG_MMCO)
	        //    av_log(this.s.avctx, AV_LOG_DEBUG, "mmco:%d %d %d\n", this.mmco[i].opcode, this.mmco[i].short_pic_num, this.mmco[i].long_arg);

	        if(   mmco[i].opcode == MMCO.MMCO_SHORT2UNUSED
	           || mmco[i].opcode == MMCO.MMCO_SHORT2LONG){
	            int[] param = new int[1];
	        	frame_num = pic_num_extract(mmco[i].short_pic_num, param);
	        	structure = param[0];
	            pic = find_short(frame_num, param);
	            j = param[0];
	            
	            if(null == pic){
	                //if(mmco[i].opcode != MMCO_SHORT2LONG || !this.long_ref[mmco[i].long_arg]
	                //   || this.long_ref[mmco[i].long_arg]->frame_num != frame_num)
	                //av_log(this.s.avctx, AV_LOG_ERROR, "mmco: unref short failure\n");
	                continue;
	            }
	        }

	        switch(mmco[i].opcode){
	        case MMCO.MMCO_SHORT2UNUSED:
	            //if(s.debug&FF_DEBUG_MMCO)
	            //    av_log(this.s.avctx, AV_LOG_DEBUG, "mmco: unref short %d count %d\n", this.mmco[i].short_pic_num, this.short_ref_count);
	            remove_short(frame_num, structure ^ MpegEncContext.PICT_FRAME);
	            break;
	        case MMCO.MMCO_SHORT2LONG:
	                if (this.long_ref[mmco[i].long_arg] != pic)
	                    remove_long(mmco[i].long_arg, 0);

	                remove_short_at_index(j);
	                this.long_ref[ mmco[i].long_arg ]= pic;
	                if (this.long_ref[ mmco[i].long_arg ]!=null){
	                    this.long_ref[ mmco[i].long_arg ].long_ref=1;
	                    this.long_ref_count++;
	                }
	            break;
	        case MMCO.MMCO_LONG2UNUSED:
	        	int[] param = new int[] { structure };
	            j = pic_num_extract(mmco[i].long_arg, param);
	            structure = param[0];
	            pic = this.long_ref[j];
	            if (pic!=null) {
	                remove_long(j, structure ^ MpegEncContext.PICT_FRAME);
	            } //else if(s.avctx->debug&FF_DEBUG_MMCO)
	              //  av_log(this.s.avctx, AV_LOG_DEBUG, "mmco: unref long failure\n");
	            break;
	        case MMCO.MMCO_LONG:
	                    // Comment below left from previous code as it is an interresting note.
	                    /* First field in pair is in short term list or
	                     * at a different long term index.
	                     * This is not allowed; see 7.4.3.3, notes 2 and 3.
	                     * Report the problem and keep the pair where it is,
	                     * and mark this field valid.
	                     */

	            if (this.long_ref[mmco[i].long_arg] != s.current_picture_ptr) {
	                remove_long(mmco[i].long_arg, 0);

	                this.long_ref[ mmco[i].long_arg ]= s.current_picture_ptr;
	                this.long_ref[ mmco[i].long_arg ].long_ref=1;
	                this.long_ref_count++;
	            }

	            s.current_picture_ptr.reference |= s.picture_structure;
	            current_ref_assigned=1;
	            break;
	        case MMCO.MMCO_SET_MAX_LONG:
	            //assert(mmco[i].long_arg <= 16);
	            // just remove the long term which index is greater than new max
	            for(j = mmco[i].long_arg; j<16; j++){
	                remove_long(j, 0);
	            }
	            break;
	        case MMCO.MMCO_RESET:
	            while(this.short_ref_count!=0){
	                remove_short(this.short_ref[0].frame_num, 0);
	            }
	            for(j = 0; j < 16; j++) {
	                remove_long(j, 0);
	            }
	            s.current_picture_ptr.poc=
	            s.current_picture_ptr.field_poc[0]=
	            s.current_picture_ptr.field_poc[1]=
	            this.poc_lsb=
	            this.poc_msb=
	            this.frame_num=
	            s.current_picture_ptr.frame_num= 0;
	            s.current_picture_ptr.mmco_reset=1;
	            break;
	        default: //assert(false);
	        }
	    }

	    if (0==current_ref_assigned) {
	        /* Second field of complementary field pair; the first field of
	         * which is already referenced. If short referenced, it
	         * should be first entry in short_ref. If not, it must exist
	         * in long_ref; trying to put it on the short list here is an
	         * error in the encoded bit stream (ref: 7.4.3.3, NOTE 2 and 3).
	         */
	        if (0!=this.short_ref_count && this.short_ref[0] == s.current_picture_ptr) {
	            /* Just mark the second field valid */
	            s.current_picture_ptr.reference = MpegEncContext.PICT_FRAME;
	        } else if (0!=s.current_picture_ptr.long_ref) {
	            //av_log(this.s.avctx, AV_LOG_ERROR, "illegal short term reference "
	            //                                 "assignment for second field "
	            //                                 "in complementary field pair "
	            //                                 "(first field is long term)\n");
	        } else {
	            pic= remove_short(s.current_picture_ptr.frame_num, 0);
	            if(pic!=null){
	                //av_log(this.s.avctx, AV_LOG_ERROR, "illegal short term buffer state detected\n");
	            }

	            if(this.short_ref_count!=0) {
	            	for(int k=this.short_ref_count;k>=1;k--)
	            		this.short_ref[k] = this.short_ref[k-1];
	               // memmove(&this.short_ref[1], &this.short_ref[0], this.short_ref_count*sizeof(Picture*));
	            } // if
	            
	            this.short_ref[0]= s.current_picture_ptr;
	            this.short_ref_count++;
	            s.current_picture_ptr.reference |= s.picture_structure;
	        }
	    }

	    if (this.long_ref_count + this.short_ref_count > this.sps.ref_frame_count){

	        /* We have too many reference frames, probably due to corrupted
	         * stream. Need to discard one frame. Prevents overrun of the
	         * short_ref and long_ref buffers.
	         */
	        //av_log(this.s.avctx, AV_LOG_ERROR,
	        //       "number of reference frames exceeds max (probably "
	        //       "corrupt input), discarding one\n");

	        if (this.long_ref_count!=0 && 0==this.short_ref_count) {
	            for (i = 0; i < 16; ++i)
	                if (this.long_ref[i]!=null)
	                    break;

	            //assert(i < 16);
	            remove_long(i, 0);
	        } else {
	            pic = this.short_ref[this.short_ref_count - 1];
	            remove_short(pic.frame_num, 0);
	        }
	    }

	    // Debug???
	    //h.print_short_term();
	    //h.print_long_term();
	    return 0;
	}
		
	public void field_end(){
	    s.mb_y= 0;

	    s.current_picture_ptr.qscale_type= MpegEncContext.FF_QSCALE_TYPE_H264;
	    s.current_picture_ptr.pict_type= s.pict_type;

	    // No H/W Acceleration
	   // if (CONFIG_H264_VDPAU_DECODER && s.avctx->codec->capabilities&CODEC_CAP_HWACCEL_VDPAU)
	   //     ff_vdpau_h264_set_reference_frames(s);

	    if(0==s.dropable) {
	        ff_h264_execute_ref_pic_marking(this.mmco, this.mmco_index);
	        this.prev_poc_msb= this.poc_msb;
	        this.prev_poc_lsb= this.poc_lsb;
	    }
	    this.prev_frame_num_offset= this.frame_num_offset;
	    this.prev_frame_num= this.frame_num;

	    /* No H/W Acceleration
	    if (avctx->hwaccel) {
	        if (avctx->hwaccel->end_frame(avctx) < 0)
	            av_log(avctx, AV_LOG_ERROR, "hardware accelerator failed to decode picture\n");
	    }
	    */

	    /* No H/W Acceleration
	    if (CONFIG_H264_VDPAU_DECODER && s.avctx->codec->capabilities&CODEC_CAP_HWACCEL_VDPAU)
	        ff_vdpau_h264_picture_complete(s);
	     */
	    /*
	     * FIXME: Error handling code does not seem to support interlaced
	     * when slices span multiple rows
	     * The ff_er_add_slice calls don't work right for bottom
	     * fields; they cause massive erroneous error concealing
	     * Error marking covers both fields (top and bottom).
	     * This causes a mismatched s.error_count
	     * and a bad error table. Further, the error count goes to
	     * INT_MAX when called for bottom field, because mb_y is
	     * past end by one (callers fault) and resync_mb_y != 0
	     * causes problems for the first MB line, too.
	     */
	    if (!(s.picture_structure != MpegEncContext.PICT_FRAME))
	        ErrorResilience.ff_er_frame_end(s);

	    s.MPV_frame_end();

	    this.current_slice=0;
	}
	
	
	/**
	 * Compare two rationals.
	 * @param a first rational
	 * @param b second rational
	 * @return 0 if a==b, 1 if a>b, -1 if a<b, and INT_MIN if one of the
	 * values is of the form 0/0
	 */
	public static /*inline*/ int av_cmp_q(AVRational a, AVRational b){
	    long tmp= a.num * (long)b.den - b.num * (long)a.den;

	    if(tmp!=0) return (int)(((tmp ^ a.den ^ b.den)>>63)|1);
	    else if(b.den!=0 && a.den!=0) return 0;
	    else if(a.num!=0 && b.num!=0) return (a.num>>31) - (b.num>>31);
	    else                    return Integer.MIN_VALUE;
	}
			
	public void free_tables(){
	    int i;
	    H264Context hx;
	    this.intra4x4_pred_mode = null;
	    this.chroma_pred_mode_table = null;
	    this.cbp_table = null;
	    this.mvd_table[0] = null;
	    this.mvd_table[1] = null;
	    this.direct_table = null;
	    this.non_zero_count = null;
	    this.slice_table_base = null;
	    this.list_counts = null;

	    this.mb2b_xy = null;
	    this.mb2br_xy = null;

	    for(i = 0; i < MAX_THREADS; i++) {
	        hx = this.thread_context[i];
	        if(hx == null) continue;
	        hx.top_borders[1] = null;
	        hx.top_borders[0] = null;
	        hx.s.obmc_scratchpad = null;
	        hx.rbsp_buffer[1] = null;
	        hx.rbsp_buffer[0] = null;
	        hx.rbsp_buffer_size[0] = 0;
	        hx.rbsp_buffer_size[1] = 0;
	        if (i!=0) this.thread_context[i] = null;
	    }
	}
		
	public static long av_gcd(long a, long b){
	    if(b!=0) return av_gcd(b, a%b);
	    else  return a;
	}	
	
	public static int av_reduce(/*int *dst_num, int *dst_den,*/int[] num_den, long num, long den, long max){
	    AVRational a0= new AVRational(0,1);
	    AVRational a1= new AVRational(1,0);
	    int sign= ((num<0) ^ (den<0))?1:0;
	    long gcd= av_gcd(Math.abs(num), Math.abs(den));

	    if(gcd!=0){
	        num = Math.abs(num)/gcd;
	        den = Math.abs(den)/gcd;
	    }
	    if(num<=max && den<=max){
	        a1= new AVRational(num, den);
	        den=0;
	    }

	    while(den!=0){
	        /*uint64_t*/long x      = num / den;
	        long next_den= num - den*x;
	        long a2n= x*a1.num + a0.num;
	        long a2d= x*a1.den + a0.den;

	        if(a2n > max || a2d > max){
	            if(a1.num!=0) x= (max - a0.num) / a1.num;
	            if(a1.den!=0) x= Math.min(x, (max - a0.den) / a1.den);

	            if (den*(2*x*a1.den + a0.den) > num*a1.den)
	                a1 =  new AVRational(x*a1.num + a0.num, x*a1.den + a0.den);
	            break;
	        }

	        a0= a1;
	        a1= new AVRational(a2n, a2d);
	        num= den;
	        den= next_den;
	    }
	    //av_assert2(av_gcd(a1.num, a1.den) <= 1U);

	    num_den[0] = ((sign!=0) ? -a1.num : a1.num);
	    num_den[1] = a1.den;

	    return (den==0?1:0);
	}
	
	public void init_dequant8_coeff_table(){
	    int i,q,x;
	    this.dequant8_coeff[0] = this.dequant8_buffer[0];
	    this.dequant8_coeff[1] = this.dequant8_buffer[1];

	    for(i=0; i<2; i++ ){
	        if(i !=0 && Arrays.equals(this.pps.scaling_matrix8[0], this.pps.scaling_matrix8[1])){
	            this.dequant8_coeff[1] = this.dequant8_buffer[0];
	            break;
	        }

	        for(q=0; q<52; q++){
	            int shift = this.div6[q];
	            int idx = this.rem6[q];
	            for(x=0; x<64; x++)
	                this.dequant8_coeff[i][q][(x>>3)|((x&7)<<3)] =
	                    ((/*uint32_t*/int)H264Data.dequant8_coeff_init[idx][ H264Data.dequant8_coeff_init_scan[((x>>1)&12) | (x&3)] ] *
	                    this.pps.scaling_matrix8[i][x]) << shift;
	        }
	    }
	}

	public void init_dequant4_coeff_table(){
	    int i,j,q,x;
	    for(i=0; i<6; i++ ){
	        this.dequant4_coeff[i] = this.dequant4_buffer[i];
	        for(j=0; j<i; j++){
	            if(Arrays.equals(this.pps.scaling_matrix4[j], this.pps.scaling_matrix4[i])){
	                this.dequant4_coeff[i] = this.dequant4_buffer[j];
	                break;
	            }
	        }
	        if(j<i)
	            continue;

	        for(q=0; q<52; q++){
	            int shift = div6[q] + 2;
	            int idx = rem6[q];
	            for(x=0; x<16; x++)
	                this.dequant4_coeff[i][q][(x>>2)|((x<<2)&0x0F)] =
	                    ((/*uint32_t*/int)H264Data.dequant4_coeff_init[idx][(x&1) + ((x>>2)&1)] *
	                    this.pps.scaling_matrix4[i][x]) << shift;
	        }
	    }
	}
	
	public void init_dequant_tables(){
	    int i,x;
	    this.init_dequant4_coeff_table();
	    if(this.pps.transform_8x8_mode!=0)
	        this.init_dequant8_coeff_table();
	    if(this.sps.transform_bypass!=0){
	        for(i=0; i<6; i++)
	            for(x=0; x<16; x++)
	                this.dequant4_coeff[i][0][x] = 1<<6;
	        if(this.pps.transform_8x8_mode!=0)
	            for(i=0; i<2; i++)
	                for(x=0; x<64; x++)
	                    this.dequant8_coeff[i][0][x] = 1<<6;
	    }
	}	
	
	/**
	 * initialize scan tables
	 */
	public void init_scan_tables(){
	    int i;
	    for(i=0; i<16; i++){	    	
	//#define T(x) (x>>2) | ((x<<2) & 0xF)
	        this.zigzag_scan[i] = (H264Data.zigzag_scan[i]>>2) | ((H264Data.zigzag_scan[i]<<2) & 0x0F); // T(zigzag_scan[i]);
	        this. field_scan[i] = (H264Data.field_scan[i]>>2) | ((H264Data.field_scan[i]<<2) & 0x0F); // T( field_scan[i]);
	//#undef T
	    }
	    for(i=0; i<64; i++){
	//#define T(x) (x>>3) | ((x&7)<<3)
	        this.zigzag_scan8x8[i]       = (ff_zigzag_direct[i]>>3) | ((ff_zigzag_direct[i]&7)<<3);
	        this.zigzag_scan8x8_cavlc[i] = (H264Data.zigzag_scan8x8_cavlc[i]>>3) | ((H264Data.zigzag_scan8x8_cavlc[i]&7)<<3);
	        this.field_scan8x8[i]        = (H264Data.field_scan8x8[i]>>3) | ((H264Data.field_scan8x8[i]&7)<<3);
	        this.field_scan8x8_cavlc[i]  = (H264Data.field_scan8x8_cavlc[i]>>3) | ((H264Data.field_scan8x8_cavlc[i]&7)<<3);
	//#undef T
	    }
	    if(this.sps.transform_bypass!=0){ //FIXME same ugly
	        this.zigzag_scan_q0          = H264Data.zigzag_scan;
	        this.zigzag_scan8x8_q0       = ff_zigzag_direct;
	        this.zigzag_scan8x8_cavlc_q0 = H264Data.zigzag_scan8x8_cavlc;
	        this.field_scan_q0           = H264Data.field_scan;
	        this.field_scan8x8_q0        = H264Data.field_scan8x8;
	        this.field_scan8x8_cavlc_q0  = H264Data.field_scan8x8_cavlc;
	    }else{
	        this.zigzag_scan_q0          = this.zigzag_scan;
	        this.zigzag_scan8x8_q0       = this.zigzag_scan8x8;
	        this.zigzag_scan8x8_cavlc_q0 = this.zigzag_scan8x8_cavlc;
	        this.field_scan_q0           = this.field_scan;
	        this.field_scan8x8_q0        = this.field_scan8x8;
	        this.field_scan8x8_cavlc_q0  = this.field_scan8x8_cavlc;
	    }
	}
	
	public int ff_h264_alloc_tables(){
	    int big_mb_num= s.mb_stride * (s.mb_height+1);
	    int row_mb_num= 2*s.mb_stride*s.thread_count;
	    int x,y;

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.intra4x4_pred_mode, row_mb_num * 8  * sizeof(uint8_t), fail)
	    this.intra4x4_pred_mode = new int[row_mb_num * 8];
	    
	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.non_zero_count    , big_mb_num * 32 * sizeof(uint8_t), fail)
	    this.non_zero_count = new int[big_mb_num][32];

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.slice_table_base  , (big_mb_num+s.mb_stride) * sizeof(*this.slice_table_base), fail)
	    this.slice_table_base = new int[big_mb_num+s.mb_stride];
	    
	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.cbp_table, big_mb_num * sizeof(uint16_t), fail)
	    this.cbp_table = new int[big_mb_num];

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.chroma_pred_mode_table, big_mb_num * sizeof(uint8_t), fail)
	    this.chroma_pred_mode_table = new int[big_mb_num];

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.mvd_table[0], 16*row_mb_num * sizeof(uint8_t), fail);
	    this.mvd_table[0] = new int[8 * row_mb_num][2];

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.mvd_table[1], 16*row_mb_num * sizeof(uint8_t), fail);
	    this.mvd_table[1] = new int[8 * row_mb_num][2];

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.direct_table, 4*big_mb_num * sizeof(uint8_t) , fail);
	    this.direct_table = new int[4*big_mb_num];

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.list_counts, big_mb_num * sizeof(uint8_t), fail)
	    this.list_counts = new int[big_mb_num];

	    Arrays.fill(this.slice_table_base, -1);
	    //memset(this.slice_table_base, -1, (big_mb_num+s.mb_stride)  * sizeof(*this.slice_table_base));
	    this.slice_table_offset = s.mb_stride*2 + 1;

	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.mb2b_xy  , big_mb_num * sizeof(uint32_t), fail);
	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.mb2br_xy , big_mb_num * sizeof(uint32_t), fail);
	    mb2b_xy = new long[big_mb_num];
	    mb2br_xy = new long[big_mb_num];
	    for(y=0; y<s.mb_height; y++){
	        for(x=0; x<s.mb_width; x++){
	             int mb_xy= x + y*s.mb_stride;
	             int b_xy = 4*x + 4*y*this.b_stride;

	            this.mb2b_xy [mb_xy]= b_xy;
	            this.mb2br_xy[mb_xy]= 8*(mb_xy % (2*s.mb_stride));
	        }
	    }

	    s.obmc_scratchpad = null;

	    if(null==this.dequant4_coeff[0])
	        this.init_dequant_tables();

	    return 0;
	}

	/**
	 * Init context
	 * Allocate buffers which are not shared amongst multiple threads.
	 */
	public int context_init(){
	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.top_borders[0], this.s.mb_width * (16+8+8) * sizeof(uint8_t), fail)
	    //FF_ALLOCZ_OR_GOTO(this.s.avctx, this.top_borders[1], this.s.mb_width * (16+8+8) * sizeof(uint8_t), fail)
	    this.top_borders[0] = new int[this.s.mb_width][16+8+8];
	    this.top_borders[1] = new int[this.s.mb_width][16+8+8];

	    this.ref_cache[0][scan8[5 ]+1] = this.ref_cache[0][scan8[7 ]+1] = this.ref_cache[0][scan8[13]+1] =
	    this.ref_cache[1][scan8[5 ]+1] = this.ref_cache[1][scan8[7 ]+1] = this.ref_cache[1][scan8[13]+1] = PART_NOT_AVAILABLE;

	    return 0;
	}
	
	public int ff_h264_frame_start() {
	    int i;

	    if(s.MPV_frame_start() < 0) {
	    	// DebugTool.printDebugString("     ----- ff_h264_frame_start error case 0\n");
	    	return -1;
	    }
	    ErrorResilience.ff_er_frame_start(s);
	    /*
	     * MPV_frame_start uses pict_type to derive key_frame.
	     * This is incorrect for H.264; IDR markings must be used.
	     * Zero here; IDR markings per slice in frame or fields are ORed in later.
	     * See decode_nal_units().
	     */
	    s.current_picture_ptr.key_frame= 0;
	    s.current_picture_ptr.mmco_reset= 0;

	    //assert(s.linesize!=0 && s.uvlinesize!=0);

	    for(i=0; i<16; i++){
	        this.block_offset[i]= 4*((scan8[i] - scan8[0])&7) + 4*s.linesize*((scan8[i] - scan8[0])>>3);
	        this.block_offset[24+i]= 4*((scan8[i] - scan8[0])&7) + 8*s.linesize*((scan8[i] - scan8[0])>>3);
	    }
	    for(i=0; i<4; i++){
	        this.block_offset[16+i]=
	        this.block_offset[20+i]= 4*((scan8[i] - scan8[0])&7) + 4*s.uvlinesize*((scan8[i] - scan8[0])>>3);
	        this.block_offset[24+16+i]=
	        this.block_offset[24+20+i]= 4*((scan8[i] - scan8[0])&7) + 8*s.uvlinesize*((scan8[i] - scan8[0])>>3);
	    }

	    /* can't be in alloc_tables because linesize isn't known there.
	     * FIXME: redo bipred weight to not require extra buffer? */
	    for(i = 0; i < s.thread_count; i++)
	        if(this.thread_context[i] !=null && null==this.thread_context[i].s.obmc_scratchpad)
	            this.thread_context[i].s.obmc_scratchpad = new int[16*2*s.linesize + 8*2*s.uvlinesize];

	    /* some macroblocks can be accessed before they're available in case of lost slices, mbaff or threading*/
	    Arrays.fill(slice_table_base, slice_table_offset, slice_table_offset + (s.mb_height*s.mb_stride-1), -1);
	    //memset(this.slice_table, -1, (s.mb_height*s.mb_stride-1) * sizeof(*this.slice_table));

//	    s.decode= (s.flags&CODEC_FLAG_PSNR) || !s.encoding || s.current_picture.reference /*|| this.contains_intra*/ || 1;

	    // We mark the current picture as non-reference after allocating it, so
	    // that if we break out due to an error it can be released automatically
	    // in the next MPV_frame_start().
	    // SVQ3 as well as most other codecs have only last/next/current and thus
	    // get released even with set reference, besides SVQ3 and others do not
	    // mark frames as reference later "naturally".
	    if(s.codec_id != H264PredictionContext.CODEC_ID_SVQ3)
	        s.current_picture_ptr.reference= 0;

	    s.current_picture_ptr.field_poc[0]=
	    s.current_picture_ptr.field_poc[1]= Integer.MAX_VALUE;
	    //assert(s.current_picture_ptr.long_ref==0);

    	// DebugTool.printDebugString("     ----- ff_h264_frame_start OK.\n");	    
	    return 0;
	}
	
	public void ff_generate_sliding_window_mmcos() {
	    //assert(this.long_ref_count + this.short_ref_count <= this.sps.ref_frame_count);

	    this.mmco_index= 0;
	    if(this.short_ref_count!=0 && this.long_ref_count + this.short_ref_count == this.sps.ref_frame_count &&
	            !((s.picture_structure != MpegEncContext.PICT_FRAME) && 0==s.first_field && 0!=s.current_picture_ptr.reference)) {

	    	if(this.mmco[0] == null) this.mmco[0] = new MMCO();
	    	if(this.mmco[1] == null) this.mmco[1] = new MMCO();
	        
	    	this.mmco[0].opcode= MMCO.MMCO_SHORT2UNUSED;
	        this.mmco[0].short_pic_num= this.short_ref[ this.short_ref_count - 1 ].frame_num;
	        this.mmco_index= 1;
	        if ((s.picture_structure != MpegEncContext.PICT_FRAME)) {
	            this.mmco[0].short_pic_num *= 2;
	            this.mmco[1].opcode= MMCO.MMCO_SHORT2UNUSED;
	            this.mmco[1].short_pic_num= this.mmco[0].short_pic_num + 1;
	            this.mmco_index= 2;
	        }
	    }
	}

	/**
	 * Replicate H264 "master" context to thread contexts.
	 */
	public static void clone_slice(H264Context dst, H264Context src)
	{
	    //memcpy(dst.block_offset,     src.block_offset, sizeof(dst.block_offset));
	    System.arraycopy(src.block_offset,0,dst.block_offset,0,dst.block_offset.length);
	    
	    dst.s.current_picture_ptr  = src.s.current_picture_ptr;
	    dst.s.current_picture      = src.s.current_picture;
	    dst.s.linesize             = src.s.linesize;
	    dst.s.uvlinesize           = src.s.uvlinesize;
	    dst.s.first_field          = src.s.first_field;

	    dst.prev_poc_msb           = src.prev_poc_msb;
	    dst.prev_poc_lsb           = src.prev_poc_lsb;
	    dst.prev_frame_num_offset  = src.prev_frame_num_offset;
	    dst.prev_frame_num         = src.prev_frame_num;
	    dst.short_ref_count        = src.short_ref_count;

	    //memcpy(dst.short_ref,        src.short_ref,        sizeof(dst.short_ref));
	    //memcpy(dst.long_ref,         src.long_ref,         sizeof(dst.long_ref));
	    for(int i=0;i<src.short_ref.length;i++)
		    src.short_ref[i].copyTo(dst.short_ref[i]);

	    for(int i=0;i<src.long_ref.length;i++)
		    src.long_ref[i].copyTo(dst.long_ref[i]);
	    
	    //memcpy(dst.default_ref_list, src.default_ref_list, sizeof(dst.default_ref_list));
	    //memcpy(dst.ref_list,         src.ref_list,         sizeof(dst.ref_list));
	    for(int i=0;i<src.default_ref_list.length;i++)
		    for(int j=0;j<src.default_ref_list[i].length;j++)
		    	src.default_ref_list[i][j].copyTo(dst.default_ref_list[i][j]);

	    for(int i=0;i<src.ref_list.length;i++)
		    for(int j=0;j<src.ref_list[i].length;j++)
		    	src.ref_list[i][j].copyTo(dst.ref_list[i][j]);

	    //memcpy(dst.dequant4_coeff,   src.dequant4_coeff,   sizeof(src.dequant4_coeff));
	    //memcpy(dst.dequant8_coeff,   src.dequant8_coeff,   sizeof(src.dequant8_coeff));
	    for(int i=0;i<src.dequant4_coeff.length;i++)
		    for(int j=0;j<src.dequant4_coeff[i].length;j++)
			    for(int k=0;k<src.dequant4_coeff[i][j].length;k++)
			    	dst.dequant4_coeff[i][j][k] = src.dequant4_coeff[i][j][k];

	    for(int i=0;i<src.dequant8_coeff.length;i++)
		    for(int j=0;j<src.dequant8_coeff[i].length;j++)
			    for(int k=0;k<src.dequant8_coeff[i][j].length;k++)
			    	dst.dequant8_coeff[i][j][k] = src.dequant8_coeff[i][j][k];
	    
	}
	
	public int init_poc(){
	    int max_frame_num= 1<<this.sps.log2_max_frame_num;
	    int[] field_poc = new int[2];
	    AVFrame cur = s.current_picture_ptr;

	    this.frame_num_offset= this.prev_frame_num_offset;
	    if(this.frame_num < this.prev_frame_num)
	        this.frame_num_offset += max_frame_num;

	    if(this.sps.poc_type==0){
	        int max_poc_lsb= 1<<this.sps.log2_max_poc_lsb;

	        if     (this.poc_lsb < this.prev_poc_lsb && this.prev_poc_lsb - this.poc_lsb >= max_poc_lsb/2)
	            this.poc_msb = this.prev_poc_msb + max_poc_lsb;
	        else if(this.poc_lsb > this.prev_poc_lsb && this.prev_poc_lsb - this.poc_lsb < -max_poc_lsb/2)
	            this.poc_msb = this.prev_poc_msb - max_poc_lsb;
	        else
	            this.poc_msb = this.prev_poc_msb;
	//printf("poc: %d %d\n", this.poc_msb, this.poc_lsb);
	        field_poc[0] =
	        field_poc[1] = this.poc_msb + this.poc_lsb;
	        if(s.picture_structure == MpegEncContext.PICT_FRAME)
	            field_poc[1] += this.delta_poc_bottom;
	    }else if(this.sps.poc_type==1){
	        int abs_frame_num, expected_delta_per_poc_cycle, expectedpoc;
	        int i;

	        if(this.sps.poc_cycle_length != 0)
	            abs_frame_num = this.frame_num_offset + this.frame_num;
	        else
	            abs_frame_num = 0;

	        if(this.nal_ref_idc==0 && abs_frame_num > 0)
	            abs_frame_num--;

	        expected_delta_per_poc_cycle = 0;
	        for(i=0; i < this.sps.poc_cycle_length; i++)
	            expected_delta_per_poc_cycle += this.sps.offset_for_ref_frame[ i ]; //FIXME integrate during sps parse

	        if(abs_frame_num > 0){
	            int poc_cycle_cnt          = (abs_frame_num - 1) / this.sps.poc_cycle_length;
	            int frame_num_in_poc_cycle = (abs_frame_num - 1) % this.sps.poc_cycle_length;

	            expectedpoc = poc_cycle_cnt * expected_delta_per_poc_cycle;
	            for(i = 0; i <= frame_num_in_poc_cycle; i++)
	                expectedpoc = expectedpoc + this.sps.offset_for_ref_frame[ i ];
	        } else
	            expectedpoc = 0;

	        if(this.nal_ref_idc == 0)
	            expectedpoc = expectedpoc + this.sps.offset_for_non_ref_pic;

	        field_poc[0] = expectedpoc + this.delta_poc[0];
	        field_poc[1] = field_poc[0] + this.sps.offset_for_top_to_bottom_field;

	        if(s.picture_structure == MpegEncContext.PICT_FRAME)
	            field_poc[1] += this.delta_poc[1];
	    }else{
	        int poc= 2*(this.frame_num_offset + this.frame_num);

	        if(0==this.nal_ref_idc)
	            poc--;

	        field_poc[0]= poc;
	        field_poc[1]= poc;
	    }

	    if(s.picture_structure != MpegEncContext.PICT_BOTTOM_FIELD)
	        s.current_picture_ptr.field_poc[0]= field_poc[0];
	    if(s.picture_structure != MpegEncContext.PICT_TOP_FIELD)
	        s.current_picture_ptr.field_poc[1]= field_poc[1];
	    cur.poc= Math.min(cur.field_poc[0], cur.field_poc[1]);

	    return 0;
	}
	
	public int ff_h264_fill_default_ref_list(){
	    int i, len;
	    
	    for(int p=0;p<this.default_ref_list.length;p++)
	    	for(int k=0;k<this.default_ref_list[p].length;k++)
	    		if(this.default_ref_list[p][k] == null)
	    			this.default_ref_list[p][k] = new AVFrame();

	    if(this.slice_type_nos==FF_B_TYPE){
	        AVFrame[] sorted = new AVFrame[32];
	        int cur_poc, list;
	        int[] lens = new int[2];

	        if((s.picture_structure != MpegEncContext.PICT_FRAME))
	            cur_poc= s.current_picture_ptr.field_poc[ (s.picture_structure == MpegEncContext.PICT_BOTTOM_FIELD)?1:0 ];
	        else
	            cur_poc= s.current_picture_ptr.poc;

	        for(list= 0; list<2; list++){
	            len= AVFrame.add_sorted(sorted    , 0, this.short_ref, 0, this.short_ref_count, cur_poc, 1^list);
	            len+=AVFrame.add_sorted(sorted, len, this.short_ref, 0, this.short_ref_count, cur_poc, 0^list);
	            //assert(len<=32);
	            len= AVFrame.build_def_list(this.default_ref_list[list],0    , sorted,0     , len, 0, s.picture_structure);
	            len+=AVFrame.build_def_list(this.default_ref_list[list],len, this.long_ref,0, 16 , 1, s.picture_structure);
	            //assert(len<=32);

	            if(len < this.ref_count[list]) {
	                //memset(this.default_ref_list[list][len], 0, sizeof(Picture)*(this.ref_count[list] - len));
	            	for(int k=len;k<this.ref_count[list];k++)
	            		this.default_ref_list[list][k].resetToZero();
	            }
	            lens[list]= len;
	        }

	        if(lens[0] == lens[1] && lens[1] > 1){
	            for(i=0; this.default_ref_list[0][i].data_base[0] == this.default_ref_list[1][i].data_base[0] 
	             && this.default_ref_list[0][i].data_offset[0] == this.default_ref_list[1][i].data_offset[0] && i<lens[0]; i++);
	            if(i == lens[0]) {
	                //FFSWAP(Picture, this.default_ref_list[1][0], this.default_ref_list[1][1]);
	            	AVFrame tmp = this.default_ref_list[1][0];
	            	this.default_ref_list[1][0] = this.default_ref_list[1][1];
	            	this.default_ref_list[1][1] = tmp;
	            }
	        }
	    }else{
	        len = AVFrame.build_def_list(this.default_ref_list[0],0    , this.short_ref,0, this.short_ref_count, 0, s.picture_structure);
	        len+= AVFrame.build_def_list(this.default_ref_list[0],len, this. long_ref,0, 16                , 1, s.picture_structure);
	        //assert(len <= 32);
	        if(len < this.ref_count[0]) {
	            //memset(&this.default_ref_list[0][len], 0, sizeof(Picture)*(this.ref_count[0] - len));
            	for(int k=len;k<this.ref_count[0];k++)
            		this.default_ref_list[0][k].resetToZero();
	        }
	    }
	/*    
	#ifdef TRACE
	    for (i=0; i<this.ref_count[0]; i++) {
	        tprintf(this.s.avctx, "List0: %s fn:%d 0x%p\n", (this.default_ref_list[0][i].long_ref ? "LT" : "ST"), this.default_ref_list[0][i].pic_id, this.default_ref_list[0][i].data[0]);
	    }
	    if(this.slice_type_nos==FF_B_TYPE){
	        for (i=0; i<this.ref_count[1]; i++) {
	            tprintf(this.s.avctx, "List1: %s fn:%d 0x%p\n", (this.default_ref_list[1][i].long_ref ? "LT" : "ST"), this.default_ref_list[1][i].pic_id, this.default_ref_list[1][i].data[0]);
	        }
	    }
	#endif
	*/
	    return 0;
	}
	
	public int ff_h264_decode_ref_pic_list_reordering(){
	    int list, index, pic_structure;

	    //print_short_term(h);
	    //print_long_term(h);

	    for(list=0; list<this.list_count; list++){
	        //memcpy(this.ref_list[list], this.default_ref_list[list], sizeof(Picture)*this.ref_count[list]);
	    	for(int i=0;i<this.ref_count[list];i++) {
	    		if(this.ref_list[list][i] == null)
	    			this.ref_list[list][i] = new AVFrame();
	    		 this.default_ref_list[list][i].copyTo(this.ref_list[list][i]);
	    	} // for

	        if(s.gb.get_bits1("reordering?")!=0){
	            int pred= this.curr_pic_num;

	            for(index=0; ; index++){
	                /*unsigned*/ int reordering_of_pic_nums_idc= s.gb.get_ue_golomb_31("reordering_of_pic_nums_idc");
	                /*unsigned*/ int pic_id;
	                int i;
	                AVFrame ref = null;

	                if(reordering_of_pic_nums_idc==3)
	                    break;

	                if(index >= this.ref_count[list]){
	                    //av_log(this.s.avctx, AV_LOG_ERROR, "reference count overflow\n");
	                    return -1;
	                }

	                if(reordering_of_pic_nums_idc<3){
	                    if(reordering_of_pic_nums_idc<2){
	                        /*const unsigned*/ int abs_diff_pic_num= s.gb.get_ue_golomb("abs_diff_pic_num") + 1;
	                        int frame_num;

	                        if(abs_diff_pic_num > this.max_pic_num){
	                            //av_log(this.s.avctx, AV_LOG_ERROR, "abs_diff_pic_num overflow\n");
	                            return -1;
	                        }

	                        if(reordering_of_pic_nums_idc == 0) pred-= abs_diff_pic_num;
	                        else                                pred+= abs_diff_pic_num;
	                        pred &= this.max_pic_num - 1;

	                        int[] param = new int[]{0}; 
	                        frame_num = this.pic_num_extract(pred, param);
	                        pic_structure = param[0];

	                        for(i= this.short_ref_count-1; i>=0; i--){
	                            ref = this.short_ref[i];
	                            ////assert(ref.reference);
	                            ////assert(!ref.long_ref);
	                            if(
	                                   ref.frame_num == frame_num &&
	                                   (ref.reference & pic_structure)!=0
	                              )
	                                break;
	                        }
	                        if(i>=0)
	                            ref.pic_id= pred;
	                    }else{
	                        int long_idx;
	                        pic_id= s.gb.get_ue_golomb("long_term_pic_idx"); //long_term_pic_idx

	                        int[] param = new int[]{0}; 
	                        long_idx= this.pic_num_extract(pic_id, param);
	                        pic_structure = param[0];

	                        if(long_idx>31){
	                            //av_log(this.s.avctx, AV_LOG_ERROR, "long_term_pic_idx overflow\n");
	                            return -1;
	                        }
	                        ref = this.long_ref[long_idx];
	                        //assert(!(ref!=null && 0==ref.reference));
	                        if(ref!=null && (ref.reference & pic_structure)!=0){
	                            ref.pic_id= pic_id;
	                            //assert(ref.long_ref!=0);
	                            i=0;
	                        }else{
	                            i=-1;
	                        }
	                    }

	                    if (i < 0) {
	                        //av_log(this.s.avctx, AV_LOG_ERROR, "reference picture missing during reorder\n");
	                        //memset(&this.ref_list[list][index], 0, sizeof(Picture)); //FIXME
	                    	this.ref_list[list][index].resetToZero();
	                    } else {
	                        for(i=index; i+1<this.ref_count[list]; i++){
	                            if(ref.long_ref == this.ref_list[list][i].long_ref && ref.pic_id == this.ref_list[list][i].pic_id)
	                                break;
	                        }
	                        for(; i > index; i--){
	                            this.ref_list[list][i]= this.ref_list[list][i-1];
	                        }
	                        ref.copyTo(this.ref_list[list][index]);
	                        if ((s.picture_structure != MpegEncContext.PICT_FRAME)){
	                            AVFrame.pic_as_field(this.ref_list[list][index], pic_structure);
	                        }
	                    }
	                }else{
	                    //av_log(this.s.avctx, AV_LOG_ERROR, "illegal reordering_of_pic_nums_idc\n");
	                    return -1;
	                }
	            }
	        }
	    }
	    for(list=0; list<this.list_count; list++){
	        for(index= 0; index < this.ref_count[list]; index++){
	            if(null==this.ref_list[list][index].data_base[0]){
	                //av_log(this.s.avctx, AV_LOG_ERROR, "Missing reference picture\n");
	                if(null!=this.default_ref_list[list][0].data_base[0])
	                    this.ref_list[list][index]= this.default_ref_list[list][0];
	                else
	                    return -1;
	            }
	        }
	    }

	    return 0;
	}	
	
	public int pred_weight_table(){
	    int list, i;
	    int luma_def, chroma_def;

	    this.use_weight= 0;
	    this.use_weight_chroma= 0;
	    this.luma_log2_weight_denom= s.gb.get_ue_golomb("luma_log2_weight_denom");
	    if(this.sps.chroma_format_idc!=0)
	        this.chroma_log2_weight_denom= s.gb.get_ue_golomb("chroma_log2_weight_denom");
	    luma_def = 1<<this.luma_log2_weight_denom;
	    chroma_def = 1<<this.chroma_log2_weight_denom;

	    for(list=0; list<2; list++){
	        this.luma_weight_flag[list]   = 0;
	        this.chroma_weight_flag[list] = 0;
	        for(i=0; i<this.ref_count[list]; i++){
	            int luma_weight_flag, chroma_weight_flag;

	            luma_weight_flag= (int)s.gb.get_bits1("luma_weight_flag");
	            if(luma_weight_flag!=0){
	                this.luma_weight[i][list][0]= s.gb.get_se_golomb("luma_weight");
	                this.luma_weight[i][list][1]= s.gb.get_se_golomb("luma_weight");
	                if(   this.luma_weight[i][list][0] != luma_def
	                   || this.luma_weight[i][list][1] != 0) {
	                    this.use_weight= 1;
	                    this.luma_weight_flag[list]= 1;
	                }
	            }else{
	                this.luma_weight[i][list][0]= luma_def;
	                this.luma_weight[i][list][1]= 0;
	            }

	            if(this.sps.chroma_format_idc!=0){
	                chroma_weight_flag= (int)s.gb.get_bits1("chroma_weight_flag");
	                if(chroma_weight_flag!=0){
	                    int j;
	                    for(j=0; j<2; j++){
	                        this.chroma_weight[i][list][j][0]= s.gb.get_se_golomb("chroma_weight");
	                        this.chroma_weight[i][list][j][1]= s.gb.get_se_golomb("chroma_weight");
	                        if(   this.chroma_weight[i][list][j][0] != chroma_def
	                           || this.chroma_weight[i][list][j][1] != 0) {
	                            this.use_weight_chroma= 1;
	                            this.chroma_weight_flag[list]= 1;
	                        }
	                    }
	                }else{
	                    int j;
	                    for(j=0; j<2; j++){
	                        this.chroma_weight[i][list][j][0]= chroma_def;
	                        this.chroma_weight[i][list][j][1]= 0;
	                    }
	                }
	            }
	        }
	        if(this.slice_type_nos != FF_B_TYPE) break;
	    }
	    this.use_weight= ((this.use_weight!=0 || this.use_weight_chroma!=0)?1:0);
	    return 0;
	}
	
	/**
	 * Initialize implicit_weight table.
	 * @param field  0/1 initialize the weight for interlaced MBAFF
	 *                -1 initializes the rest
	 */
	public void implicit_weight_table(int field){
	    int ref0, ref1, i, cur_poc, ref_start, ref_count0, ref_count1;

	    for (i = 0; i < 2; i++) {
	        this.luma_weight_flag[i]   = 0;
	        this.chroma_weight_flag[i] = 0;
	    }

	    if(field < 0){
	        cur_poc = s.current_picture_ptr.poc;
	    if(   this.ref_count[0] == 1 && this.ref_count[1] == 1 && 0==this.mb_aff_frame
	       && this.ref_list[0][0].poc + this.ref_list[1][0].poc == 2*cur_poc){
	        this.use_weight= 0;
	        this.use_weight_chroma= 0;
	        return;
	    }
	        ref_start= 0;
	        ref_count0= (int)this.ref_count[0];
	        ref_count1= (int)this.ref_count[1];
	    }else{
	        cur_poc = s.current_picture_ptr.field_poc[field];
	        ref_start= 16;
	        ref_count0= (int)(16+2*this.ref_count[0]);
	        ref_count1= (int)(16+2*this.ref_count[1]);
	    }

	    this.use_weight= 2;
	    this.use_weight_chroma= 2;
	    this.luma_log2_weight_denom= 5;
	    this.chroma_log2_weight_denom= 5;

	    for(ref0=ref_start; ref0 < ref_count0; ref0++){
	        int poc0 = this.ref_list[0][ref0].poc;
	        for(ref1=ref_start; ref1 < ref_count1; ref1++){
	            int poc1 = this.ref_list[1][ref1].poc;
	            int td = av_clip(poc1 - poc0, -128, 127);
	            int w= 32;
	            if(td!=0){
	                int tb = av_clip(cur_poc - poc0, -128, 127);
	                int tx = (16384 + (Math.abs(td) >> 1)) / td;
	                int dist_scale_factor = (tb*tx + 32) >> 8;
	                if(dist_scale_factor >= -64 && dist_scale_factor <= 128)
	                    w = 64 - dist_scale_factor;
	            }
	            if(field<0){
	                this.implicit_weight[ref0][ref1][0]=
	                this.implicit_weight[ref0][ref1][1]= w;
	            }else{
	                this.implicit_weight[ref0][ref1][field]=w;
	            }
	        }
	    }
	}
	
	public int ff_h264_decode_ref_pic_marking(GetBitContext gb){
	    int i;

	    this.mmco_index= 0;
	    if(this.nal_unit_type == NAL_IDR_SLICE){ //FIXME fields
	        s.broken_link= (int)gb.get_bits1("broken_link") -1;
	        if(gb.get_bits1("MMCO_LONG?")!=0){
	            this.mmco[0].opcode= MMCO.MMCO_LONG;
	            this.mmco[0].long_arg= 0;
	            this.mmco_index= 1;
	        }
	    }else{
	        if(gb.get_bits1("adaptive_ref_pic_marking_mode_flag")!=0){ // adaptive_ref_pic_marking_mode_flag
	            for(i= 0; i<MAX_MMCO_COUNT; i++) {
	                /*MMCOOpcode*/int opcode= gb.get_ue_golomb_31("MMCOOpcode");

	                this.mmco[i].opcode= opcode;
	                if(opcode==MMCO.MMCO_SHORT2UNUSED || opcode==MMCO.MMCO_SHORT2LONG){
	                    this.mmco[i].short_pic_num= (this.curr_pic_num - gb.get_ue_golomb("?") - 1) & (this.max_pic_num - 1);
	/*                    if(this.mmco[i].short_pic_num >= this.short_ref_count || this.short_ref[ this.mmco[i].short_pic_num ] == NULL){
	                        av_log(s.avctx, AV_LOG_ERROR, "illegal short ref in memory management control operation %d\n", mmco);
	                        return -1;
	                    }*/
	                }
	                if(opcode==MMCO.MMCO_SHORT2LONG || opcode==MMCO.MMCO_LONG2UNUSED || opcode==MMCO.MMCO_LONG || opcode==MMCO.MMCO_SET_MAX_LONG){
	                    /*unsigned */int long_arg= gb.get_ue_golomb_31("long_arg");
	                    if(long_arg >= 32 || (long_arg >= 16 && !(opcode == MMCO.MMCO_LONG2UNUSED && (s.picture_structure != MpegEncContext.PICT_FRAME)))){
	                        //av_log(this.s.avctx, AV_LOG_ERROR, "illegal long ref in memory management control operation %d\n", opcode);
	                        return -1;
	                    }
	                    this.mmco[i].long_arg= long_arg;
	                }

	                if(opcode > /*(unsigned)*/MMCO.MMCO_LONG){
	                    //av_log(this.s.avctx, AV_LOG_ERROR, "illegal memory management control operation %d\n", opcode);
	                    return -1;
	                }
	                if(opcode == MMCO.MMCO_END)
	                    break;
	            }
	            this.mmco_index= i;
	        }else{
	            this.ff_generate_sliding_window_mmcos();
	        }
	    }

	    return 0;
	}
	
	public void ff_h264_fill_mbaff_ref_list(){
	    int list, i, j;
	    for(list=0; list<2; list++){ //FIXME try list_count
	        for(i=0; i<this.ref_count[list]; i++){
	            AVFrame frame = this.ref_list[list][i];
	            AVFrame[] field_base = this.ref_list[list];
	            int field_offset = 16+2*i;
	            frame.copyTo(field_base[field_offset + 0]);
	            for(j=0; j<3; j++)
	                field_base[field_offset + 0].linesize[j] <<= 1;
	            field_base[field_offset + 0].reference = MpegEncContext.PICT_TOP_FIELD;
	            field_base[field_offset + 0].poc= field_base[field_offset + 0].field_poc[0];
	            field_base[field_offset + 1] = field_base[field_offset + 0];
	            for(j=0; j<3; j++)
	                field_base[field_offset + 1].data_offset[j] += frame.linesize[j];
	            field_base[field_offset + 1].reference = MpegEncContext.PICT_BOTTOM_FIELD;
	            field_base[field_offset + 1].poc= field_base[field_offset + 1].field_poc[1];

	            this.luma_weight[16+2*i][list][0] = this.luma_weight[16+2*i+1][list][0] = this.luma_weight[i][list][0];
	            this.luma_weight[16+2*i][list][1] = this.luma_weight[16+2*i+1][list][1] = this.luma_weight[i][list][1];
	            for(j=0; j<2; j++){
	                this.chroma_weight[16+2*i][list][j][0] = this.chroma_weight[16+2*i+1][list][j][0] = this.chroma_weight[i][list][j][0];
	                this.chroma_weight[16+2*i][list][j][1] = this.chroma_weight[16+2*i+1][list][j][1] = this.chroma_weight[i][list][j][1];
	            }
	        }
	    }
	}
	
	public int get_scale_factor(int poc, int poc1, int i){
	    int poc0 = this.ref_list[0][i].poc;
	    int td = av_clip(poc1 - poc0, -128, 127);
	    if(td == 0 || this.ref_list[0][i].long_ref!=0){
	        return 256;
	    }else{
	        int tb = av_clip(poc - poc0, -128, 127);
	        int tx = (16384 + (Math.abs(td) >> 1)) / td;
	        return av_clip((tb*tx + 32) >> 6, -1024, 1023);
	    }
	}	
	
	public void ff_h264_direct_dist_scale_factor(){
	    int _poc = this.s.current_picture_ptr.field_poc[ (s.picture_structure == MpegEncContext.PICT_BOTTOM_FIELD)?1:0 ];
	    int _poc1 = this.ref_list[1][0].poc;
	    int i, field;
	    for(field=0; field<2; field++){
	        int poc  = this.s.current_picture_ptr.field_poc[field];
	        int poc1 = this.ref_list[1][0].field_poc[field];
	        for(i=0; i < 2*this.ref_count[0]; i++)
	            this.dist_scale_factor_field[field][i^field] = this.get_scale_factor( poc, poc1, i+16);
	    }

	    for(i=0; i<this.ref_count[0]; i++){
	        this.dist_scale_factor[i] = this.get_scale_factor(_poc, _poc1, i);
	    }
	}
	
	public void fill_colmap(int[][] map/*[2][16+32]*/, int list, int field, int colfield, int mbafi){
	    AVFrame ref1 = this.ref_list[1][0];
	    int j, old_ref, rfield;
	    int start= (mbafi!=0) ? 16                      : 0;
	    int end  = (int)((mbafi!=0) ? 16+2*this.ref_count[0]    : this.ref_count[0]);
	    int interl= (mbafi!=0 || s.picture_structure != MpegEncContext.PICT_FRAME)?1:0;

	    /* bogus; fills in for missing frames */
	    //memset(map[list], 0, sizeof(map[list]));

	    for(rfield=0; rfield<2; rfield++){
	        for(old_ref=0; old_ref<ref1.ref_count[colfield][list]; old_ref++){
	            int poc = ref1.ref_poc[colfield][list][old_ref];

	            if     (0==interl)
	                poc |= 3;
	            else if( 0!=interl && (poc&3) == 3) //FIXME store all MBAFF references so this isnt needed
	                poc= (poc&~3) + rfield + 1;

	            for(j=start; j<end; j++){
	                if(4*this.ref_list[0][j].frame_num + (this.ref_list[0][j].reference&3) == poc){
	                    int cur_ref= (mbafi!=0) ? (j-16)^field : j;
	                    map[list][2*old_ref + (rfield^field) + 16] = cur_ref;
	                    if(rfield == field || 0==interl)
	                        map[list][old_ref] = cur_ref;
	                    break;
	                }
	            }
	        }
	    }
	}
		
	public void ff_h264_direct_ref_list_init(){
		
		//???????? The correct place to new ref_list objs??
		for(int i=0;i<this.ref_list.length;i++)
			for(int j=0;j<this.ref_list[i].length;j++) {
				if(this.ref_list[i][j]==null)
					this.ref_list[i][j] = new AVFrame();
			} // for
		
	    AVFrame ref1 = this.ref_list[1][0];
	    AVFrame cur = s.current_picture_ptr;
	    int list, j, field;
	    int sidx= (s.picture_structure&1)^1;
	    int ref1sidx= (ref1.reference&1)^1;

	    for(list=0; list<2; list++){
	        cur.ref_count[sidx][list] = (int)this.ref_count[list];
	        for(j=0; j<this.ref_count[list]; j++)
	            cur.ref_poc[sidx][list][j] = 4*this.ref_list[list][j].frame_num + (this.ref_list[list][j].reference&3);
	    }

	    if(s.picture_structure == MpegEncContext.PICT_FRAME){
	        //memcpy(cur.ref_count[1], cur.ref_count[0], sizeof(cur.ref_count[0]));
	        //memcpy(cur.ref_poc  [1], cur.ref_poc  [0], sizeof(cur.ref_poc  [0]));
	    	System.arraycopy(cur.ref_count[0], 0, cur.ref_count[1], 0, cur.ref_count[0].length);
	    	System.arraycopy(cur.ref_poc[0], 0, cur.ref_poc[1], 0, cur.ref_poc[0].length);
	    }

	    cur.mbaff = this.mb_aff_frame;

	    this.col_fieldoff= 0;
	    if(s.picture_structure == MpegEncContext.PICT_FRAME){
	        int cur_poc = s.current_picture_ptr.poc;
	        //?????????????????????????????
	        //int[] col_poc = this.ref_list[1].field_poc;
	        int[] col_poc = this.ref_list[1][0].field_poc;
	        this.col_parity= ((Math.abs(col_poc[0] - cur_poc) >= Math.abs(col_poc[1] - cur_poc)))?1:0;
	        ref1sidx=sidx= this.col_parity;
	    }else if(0==(s.picture_structure & this.ref_list[1][0].reference) && 0==this.ref_list[1][0].mbaff){ // FL . FL & differ parity
	        this.col_fieldoff= s.mb_stride*(2*(this.ref_list[1][0].reference) - 3);
	    }

	    if(cur.pict_type != FF_B_TYPE || this.direct_spatial_mv_pred!=0)
	        return;

	    for(list=0; list<2; list++){
	        this.fill_colmap(this.map_col_to_list0, list, sidx, ref1sidx, 0);
	        if(this.mb_aff_frame!=0)
	        for(field=0; field<2; field++)
	            this.fill_colmap(this.map_col_to_list0_field[field], list, field, field, 1);
	    }
	}
	
	/**
	 * decodes a slice header.
	 * This will also call MPV_common_init() and frame_start() as needed.
	 *
	 * @param h h264context
	 * @param h0 h264 master context (differs from 'h' when doing sliced based parallel decoding)
	 *
	 * @return 0 if okay, <0 if an error occurred, 1 if decoding must not be multithreaded
	 */
	public static int decode_slice_header(H264Context h, H264Context h0){
	    MpegEncContext s = h.s;
	    MpegEncContext s0 = h0.s;
	    /*unsigned */int first_mb_in_slice;
	    /*unsigned */int pps_id;
	    int num_ref_idx_active_override_flag;
	    /*unsigned */int slice_type, tmp, i, j;
	    int default_ref_list_done = 0;
	    int last_pic_structure;

	    s.dropable= (h.nal_ref_idc == 0)?1:0;

        s.me.qpel_put= s.dsp.put_h264_qpel_pixels_tab;
        s.me.qpel_avg= s.dsp.avg_h264_qpel_pixels_tab;

	    first_mb_in_slice= s.gb.get_ue_golomb("first_mb_in_slice");

	    if(first_mb_in_slice == 0){ //FIXME better field boundary detection
	        if((h0.current_slice !=0 ) && (s.picture_structure != MpegEncContext.PICT_FRAME)){
	            h.field_end();
	        } // if

	        h0.current_slice = 0;
	        if (0 == s0.first_field)
	            s.current_picture_ptr= null;
	    }

	    slice_type= s.gb.get_ue_golomb_31("slice_type");
	    if(slice_type > 9){
	    	// DebugTool.printDebugString("   --- decode_slide_header error case 0\n");
	        //av_log(h.s.avctx, AV_LOG_ERROR, "slice type too large (%d) at %d %d\n", h.slice_type, s.mb_x, s.mb_y);
	        return -1;
	    }
	    if(slice_type > 4){
	        slice_type -= 5;
	        h.slice_type_fixed=1;
	    }else
	        h.slice_type_fixed=0;

	    slice_type= H264Data.golomb_to_pict_type[ slice_type ];

	    /*
	    if(slice_type == H264Context.FF_I_TYPE) {
	    	//System.out.println("Decoding I-Slice.");
	    } else if(slice_type == H264Context.FF_P_TYPE) {
		    //System.out.println("Decoding P-Slice.");
	    } else if(slice_type == H264Context.FF_B_TYPE) {
		    //System.out.println("Decoding B-Slice.");
	    } else if(slice_type == H264Context.FF_S_TYPE) {
		    //System.out.println("Decoding S-Slice.");
	    } else if(slice_type == H264Context.FF_SI_TYPE) {
		    //System.out.println("Decoding SI-Slice.");
	    } else if(slice_type == H264Context.FF_SP_TYPE) {
		    //System.out.println("Decoding SP-Slice.");
	    } else if(slice_type == H264Context.FF_BI_TYPE) {
		    //System.out.println("Decoding BI-Slice.");
	    } // if
	    */
	    
	    if (slice_type == FF_I_TYPE
	        || (h0.current_slice != 0 && slice_type == h0.last_slice_type) ) {
	        default_ref_list_done = 1;
	    }
	    h.slice_type= slice_type;
	    h.slice_type_nos= slice_type & 3;

	    s.pict_type= h.slice_type; // to make a few old functions happy, it's wrong though

	    pps_id= s.gb.get_ue_golomb("pps_id");
	    if(pps_id>=MAX_PPS_COUNT){
	        //av_log(h.s.avctx, AV_LOG_ERROR, "pps_id out of range\n");
	    	// DebugTool.printDebugString("   --- decode_slide_header error case 1\n");
	    	return -1;
	    }
	    if(null==h0.pps_buffers[pps_id]) {
	        //av_log(h.s.avctx, AV_LOG_ERROR, "non-existing PPS %u referenced\n", pps_id);
	    	// DebugTool.printDebugString("   --- decode_slide_header error case 2\n");
	    	return -1;
	    }
	    h0.pps_buffers[pps_id].copyTo(h.pps);

	    if(null==h0.sps_buffers[(int)h.pps.sps_id]) {
	        //av_log(h.s.avctx, AV_LOG_ERROR, "non-existing SPS %u referenced\n", h.pps.sps_id);
	    	// DebugTool.printDebugString("   --- decode_slide_header error case 3\n");
	    	return -1;
	    }
	    h0.sps_buffers[(int)h.pps.sps_id].copyTo(h.sps);

	    s.profile = h.sps.profile_idc;
	    s.level   = h.sps.level_idc;
	    s.refs    = h.sps.ref_frame_count;

	    if(h == h0 && h.dequant_coeff_pps != pps_id){
	        h.dequant_coeff_pps = pps_id;
	        h.init_dequant_tables();
	    }

	    s.mb_width= h.sps.mb_width;
	    s.mb_height= h.sps.mb_height * (2 - h.sps.frame_mbs_only_flag);

	    h.b_stride=  s.mb_width*4;

	    s.width = (int)(16*s.mb_width - 2*Math.min(h.sps.crop_right, 7));
	    if(h.sps.frame_mbs_only_flag!=0)
	        s.height= (int)(16*s.mb_height - 2*Math.min(h.sps.crop_bottom, 7));
	    else
	        s.height= (int)(16*s.mb_height - 4*Math.min(h.sps.crop_bottom, 7));

	   
	    if (s.context_initialized!=0
	        && (   /*s.width != s.avctx.width || s.height != s.avctx.height
	            ||*/ av_cmp_q(h.sps.sar, s.sample_aspect_ratio)!=0)) {
	        if(h != h0) {
		    	// DebugTool.printDebugString("   --- decode_slide_header error case 4\n");
	        	return -1;   // width / height changed during parallelized decoding
	        }
	        h.free_tables();
	        s.flush_dpb();
	        s.MPV_common_end();
	    }
	    
	    if (0==s.context_initialized) {
	        if(h != h0) {
		    	// DebugTool.printDebugString("   --- decode_slide_header error case 5\n");
	            return -1;  // we cant (re-)initialize context during parallel decoding
	        }

	        s.avcodec_set_dimensions(s.width, s.height);
	        s.sample_aspect_ratio= h.sps.sar;
	        //av_assert0(s.avctx.sample_aspect_ratio.den);

	        if(0!=h.sps.video_signal_type_present_flag){
	            s.color_range = ((h.sps.full_range!=0) ? MpegEncContext.AVCOL_RANGE_JPEG : MpegEncContext.AVCOL_RANGE_MPEG);
	            if(h.sps.colour_description_present_flag!=0){
	                s.color_primaries = h.sps.color_primaries;
	                s.color_trc       = h.sps.color_trc;
	                s.colorspace      = h.sps.colorspace;
	            }
	        }

	        if(h.sps.timing_info_present_flag!=0){
	            long den= h.sps.time_scale;
	            if(h.x264_build < 44)
	                den *= 2;
	            int[] param = new int[] { s.time_base.num, s.time_base.den, };
	            av_reduce(param,
	                      h.sps.num_units_in_tick, den, 1<<30);
	            s.time_base.num = param[0];
	            s.time_base.den = param[1];
	        }
	        s.pix_fmt = MpegEncContext.get_format(
	                                                 s.codec.pix_fmts !=null?
	                                                 s.codec.pix_fmts :
	                                                 s.color_range == MpegEncContext.AVCOL_RANGE_JPEG ?
	                                                 hwaccel_pixfmt_list_h264_jpeg_420 :
	                                                 MpegEncContext.ff_hwaccel_pixfmt_list_420);
	        s.hwaccel = 0; // No H/W Accel!! // ff_find_hwaccel(s.avctx.codec.id, s.avctx.pix_fmt);

	        if (s.MPV_common_init() < 0) {
		    	// DebugTool.printDebugString("   --- decode_slide_header error case 6\n");
	            return -1;
	        }
	        s.first_field = 0;
	        h.prev_interlaced_frame = 1;

	        h.init_scan_tables();
	        h.ff_h264_alloc_tables();

	        /*// We use single thread decoder
	        for(i = 1; i < 1; i++) {
	            H264Context c;
	            c = h.thread_context[i] = new H264Context();
	            memcpy(c, h.s.thread_context[i], sizeof(MpegEncContext));
	            memset(&c->s + 1, 0, sizeof(H264Context) - sizeof(MpegEncContext));
	            c.h264dsp = h.h264dsp;
	            c.sps = h.sps;
	            c.pps = h.pps;
	            c.init_scan_tables();
	            clone_tables(c, h, i);
	        }
			*/
	        for(i = 0; i < s.thread_count; i++)
	            if(h.thread_context[i].context_init() < 0) {
	    	    	// DebugTool.printDebugString("   --- decode_slide_header error case 7\n");
	                return -1;
	            }
	    }

	    h.frame_num= (int)s.gb.get_bits(h.sps.log2_max_frame_num,"frame_num");

	    h.mb_mbaff = 0;
	    h.mb_aff_frame = 0;
	    last_pic_structure = s0.picture_structure;
	    if(h.sps.frame_mbs_only_flag!=0){
	        s.picture_structure= MpegEncContext.PICT_FRAME;
	    }else{
	        if(s.gb.get_bits1("field_pic_flag")!=0) { //field_pic_flag
	            s.picture_structure= (int)(MpegEncContext.PICT_TOP_FIELD + s.gb.get_bits1("bottom_field_flag")); //bottom_field_flag
	        } else {
	            s.picture_structure= MpegEncContext.PICT_FRAME;
	            h.mb_aff_frame = h.sps.mb_aff;
	        }
	    }
	    h.mb_field_decoding_flag= ((s.picture_structure) != MpegEncContext.PICT_FRAME?1:0);

	    if(h0.current_slice == 0){
	        while(h.frame_num !=  h.prev_frame_num &&
	              h.frame_num != ((h.prev_frame_num+1)%(1<<h.sps.log2_max_frame_num))){
	            AVFrame prev = ((h.short_ref_count!=0) ? h.short_ref[0] : null);
	            //av_log(h.s.avctx, AV_LOG_DEBUG, "Frame num gap %d %d\n", h.frame_num, h.prev_frame_num);
	            if (h.ff_h264_frame_start() < 0) {
	    	    	// DebugTool.printDebugString("   --- decode_slide_header error case 8\n");
	                return -1;
	            }
	            h.prev_frame_num++;
	            h.prev_frame_num %= 1<<h.sps.log2_max_frame_num;
	            s.current_picture_ptr.frame_num= h.prev_frame_num;
	            h.ff_generate_sliding_window_mmcos();
	            h.ff_h264_execute_ref_pic_marking(h.mmco, h.mmco_index);
	            /* Error concealment: if a ref is missing, copy the previous ref in its place.
	             * FIXME: avoiding a memcpy would be nice, but ref handling makes many assumptions
	             * about there being no actual duplicates.
	             * FIXME: this doesn't copy padding for out-of-frame motion vectors.  Given we're
	             * concealing a lost frame, this probably isn't noticable by comparison, but it should
	             * be fixed. */
	            if (h.short_ref_count!=0) {
	                if (prev!=null) {
	                    ImageUtils.av_image_copy(h.short_ref[0].data_base, 
	                    		h.short_ref[0].data_offset,
	                    		h.short_ref[0].linesize,
	                                  /*( uint8_t**)*/prev.data_base, prev.data_offset, prev.linesize,
	                                  s.pix_fmt, s.mb_width*16, s.mb_height*16);
	                    h.short_ref[0].poc = prev.poc+2;
	                }
	                h.short_ref[0].frame_num = h.prev_frame_num;
	            }
	        }

	        /* See if we have a decoded first field looking for a pair... */
	        if (s0.first_field!=0) {
	            ////assert(s0.current_picture_ptr);
	            ////assert(s0.current_picture_ptr->data[0]);
	            ////assert(s0.current_picture_ptr->reference != DELAYED_PIC_REF);

	            /* figure out if we have a complementary field pair */
	            if (!(s.picture_structure != MpegEncContext.PICT_FRAME) || s.picture_structure == last_pic_structure) {
	                /*
	                 * Previous field is unmatched. Don't display it, but let it
	                 * remain for reference if marked as such.
	                 */
	                s0.current_picture_ptr = null;
	                s0.first_field = ((s.picture_structure != MpegEncContext.PICT_FRAME)?1:0);

	            } else {
	                if (h.nal_ref_idc!=0 &&
	                        s0.current_picture_ptr.reference != 0&&
	                        s0.current_picture_ptr.frame_num != h.frame_num) {
	                    /*
	                     * This and previous field were reference, but had
	                     * different frame_nums. Consider this field first in
	                     * pair. Throw away previous field except for reference
	                     * purposes.
	                     */
	                    s0.first_field = 1;
	                    s0.current_picture_ptr = null;

	                } else {
	                    /* Second field in complementary pair */
	                    s0.first_field = 0;
	                }
	            }

	        } else {
	            /* Frame or first field in a potentially complementary pair */
	            ////assert(!s0.current_picture_ptr);
	            s0.first_field = ((s.picture_structure != MpegEncContext.PICT_FRAME)?1:0);
	        }

	        if((0==((s.picture_structure != MpegEncContext.PICT_FRAME)?1:0) || 0!=s0.first_field) && h.ff_h264_frame_start() < 0) {
	            s0.first_field = 0;
		    	// DebugTool.printDebugString("   --- decode_slide_header error case 9\n");

	            return -1;
	        }
	    }
	    if(h != h0)
	        clone_slice(h, h0);

	    s.current_picture_ptr.frame_num= h.frame_num; //FIXME frame_num cleanup

	    ////assert(s.mb_num == s.mb_width * s.mb_height);
	    if(first_mb_in_slice << ((h.mb_aff_frame!=0 || (s.picture_structure != MpegEncContext.PICT_FRAME))?1:0) >= s.mb_num ||
	       first_mb_in_slice                    >= s.mb_num){
	        //av_log(h.s.avctx, AV_LOG_ERROR, "first_mb_in_slice overflow\n");
	    	// DebugTool.printDebugString("   --- decode_slide_header error case 10\n");
	        return -1;
	    }
	    s.resync_mb_x = s.mb_x = first_mb_in_slice % s.mb_width;
	    s.resync_mb_y = s.mb_y = (first_mb_in_slice / s.mb_width) << ((h.mb_aff_frame!=0 || (s.picture_structure != MpegEncContext.PICT_FRAME))?1:0);
	    if (s.picture_structure == MpegEncContext.PICT_BOTTOM_FIELD)
	        s.resync_mb_y = s.mb_y = s.mb_y + 1;
	    //assert(s.mb_y < s.mb_height);

	    if(s.picture_structure==MpegEncContext.PICT_FRAME){
	        h.curr_pic_num=   h.frame_num;
	        h.max_pic_num= 1<< h.sps.log2_max_frame_num;
	    }else{
	        h.curr_pic_num= 2*h.frame_num + 1;
	        h.max_pic_num= 1<<(h.sps.log2_max_frame_num + 1);
	    }

	    if(h.nal_unit_type == NAL_IDR_SLICE){
	        s.gb.get_ue_golomb("idr_pic_id"); /* idr_pic_id */
	    }

	    if(h.sps.poc_type==0){
	        h.poc_lsb= (int)s.gb.get_bits(h.sps.log2_max_poc_lsb,"poc_lsb");

	        if(h.pps.pic_order_present==1 && s.picture_structure==MpegEncContext.PICT_FRAME){
	            h.delta_poc_bottom= s.gb.get_se_golomb("delta_poc_bottom");
	        }
	    }

	    if(h.sps.poc_type==1 && 0==h.sps.delta_pic_order_always_zero_flag){
	        h.delta_poc[0]= s.gb.get_se_golomb("delta_poc[0]");

	        if(h.pps.pic_order_present==1 && s.picture_structure==MpegEncContext.PICT_FRAME)
	            h.delta_poc[1]= s.gb.get_se_golomb("delta_poc[1]");
	    }

	    h.init_poc();

	    if(h.pps.redundant_pic_cnt_present!=0){
	        h.redundant_pic_count= s.gb.get_ue_golomb("redundant_pic_count");
	    }

	    //set defaults, might be overridden a few lines later
	    h.ref_count[0]= h.pps.ref_count[0];
	    h.ref_count[1]= h.pps.ref_count[1];

	    if(h.slice_type_nos != FF_I_TYPE){
	        if(h.slice_type_nos == FF_B_TYPE){
	            h.direct_spatial_mv_pred= (int)s.gb.get_bits1("direct_spatial_mv_pred");
	        }
	        num_ref_idx_active_override_flag= (int)s.gb.get_bits1("num_ref_idx_active_override_flag");

	        if(num_ref_idx_active_override_flag!=0){
	            h.ref_count[0]= s.gb.get_ue_golomb("ref_count[0]") + 1;
	            if(h.slice_type_nos==FF_B_TYPE)
	                h.ref_count[1]= s.gb.get_ue_golomb("ref_count[1]") + 1;

	            if(h.ref_count[0]-1 > 32-1 || h.ref_count[1]-1 > 32-1){
	                //av_log(h.s.avctx, AV_LOG_ERROR, "reference overflow\n");
	                h.ref_count[0]= h.ref_count[1]= 1;
	    	    	// DebugTool.printDebugString("   --- decode_slide_header error case 11\n");

	                return -1;
	            }
	        }
	        if(h.slice_type_nos == FF_B_TYPE)
	            h.list_count= 2;
	        else
	            h.list_count= 1;
	    }else
	        h.list_count= 0;

	    if(0==default_ref_list_done){
	        h.ff_h264_fill_default_ref_list();
	    }

	    if(h.slice_type_nos!=FF_I_TYPE && h.ff_h264_decode_ref_pic_list_reordering() < 0) {
	    	// DebugTool.printDebugString("   --- decode_slide_header error case 12\n");

	        return -1;
	    }

	    if(h.slice_type_nos!=FF_I_TYPE){
	        s.last_picture_ptr= h.ref_list[0][0];
	        MpegEncContext.ff_copy_picture(s.last_picture, s.last_picture_ptr);
	    }
	    if(h.slice_type_nos==FF_B_TYPE){
	        s.next_picture_ptr= h.ref_list[1][0];
	        MpegEncContext.ff_copy_picture(s.next_picture, s.next_picture_ptr);
	    }

	    if(   (h.pps.weighted_pred!=0          && h.slice_type_nos == FF_P_TYPE )
	       ||  (h.pps.weighted_bipred_idc==1 && h.slice_type_nos== FF_B_TYPE ) )
	        h.pred_weight_table();
	    else if(h.pps.weighted_bipred_idc==2 && h.slice_type_nos== FF_B_TYPE){
	        h.implicit_weight_table(-1);
	    }else {
	        h.use_weight = 0;
	        for (i = 0; i < 2; i++) {
	            h.luma_weight_flag[i]   = 0;
	            h.chroma_weight_flag[i] = 0;
	        }
	    }

	    if(h.nal_ref_idc!=0)
	        h0.ff_h264_decode_ref_pic_marking(s.gb);

	    if(h.mb_aff_frame!=0){
	        h.ff_h264_fill_mbaff_ref_list();

	        if(h.pps.weighted_bipred_idc==2 && h.slice_type_nos== FF_B_TYPE){
	            h.implicit_weight_table(0);
	            h.implicit_weight_table(1);
	        }
	    }

	    if(h.slice_type_nos==FF_B_TYPE && 0==h.direct_spatial_mv_pred)
	        h.ff_h264_direct_dist_scale_factor();
	    h.ff_h264_direct_ref_list_init();

	    if( h.slice_type_nos != FF_I_TYPE && h.pps.cabac!=0 ){
	        tmp = s.gb.get_ue_golomb_31("cabac_init_idc");
	        if(tmp > 2){
	            //av_log(s.avctx, AV_LOG_ERROR, "cabac_init_idc overflow\n");
		    	// DebugTool.printDebugString("   --- decode_slide_header error case 13\n");

	            return -1;
	        }
	        h.cabac_init_idc= tmp;
	    }

	    h.last_qscale_diff = 0;
	    tmp = h.pps.init_qp + s.gb.get_se_golomb("init_qp");
	    if(tmp>51){
	        //av_log(s.avctx, AV_LOG_ERROR, "QP %u out of range\n", tmp);
	    	// DebugTool.printDebugString("   --- decode_slide_header error case 14\n");

	        return -1;
	    }
	    s.qscale= tmp;
	    h.chroma_qp[0] = h.pps.chroma_qp_table[0][s.qscale];
	    h.chroma_qp[1] = h.pps.chroma_qp_table[1][s.qscale];
	    //FIXME qscale / qp ... stuff
	    if(h.slice_type == FF_SP_TYPE){
	        s.gb.get_bits1("sp_for_switch_flag"); /* sp_for_switch_flag */
	    }
	    if(h.slice_type==FF_SP_TYPE || h.slice_type == FF_SI_TYPE){
	        s.gb.get_se_golomb("slice_qs_delta"); /* slice_qs_delta */
	    }

	    h.deblocking_filter = 1;
	    h.slice_alpha_c0_offset = 52;
	    h.slice_beta_offset = 52;
	    if( 0!=h.pps.deblocking_filter_parameters_present ) {
	        tmp= s.gb.get_ue_golomb_31("deblocking_filter_idc");
	        if(tmp > 2){
		    	// DebugTool.printDebugString("   --- decode_slide_header error case 15\n");

	            //av_log(s.avctx, AV_LOG_ERROR, "deblocking_filter_idc %u out of range\n", tmp);
	            return -1;
	        }
	        h.deblocking_filter= tmp;
	        if(h.deblocking_filter < 2)
	            h.deblocking_filter^= 1; // 1<->0

	        if( 0!= h.deblocking_filter ) {
	            h.slice_alpha_c0_offset += s.gb.get_se_golomb("slice_alpha_c0_offset") << 1;
	            h.slice_beta_offset     += s.gb.get_se_golomb("slice_beta_offset") << 1;
	            if(   h.slice_alpha_c0_offset > 104
	               || h.slice_beta_offset     > 104){
	    	    	// DebugTool.printDebugString("   --- decode_slide_header error case 16\n");

	                //av_log(s.avctx, AV_LOG_ERROR, "deblocking filter parameters %d %d out of range\n", h.slice_alpha_c0_offset, h.slice_beta_offset);
	                return -1;
	            }
	        }
	    }

	    if(   s.skip_loop_filter >= MpegEncContext.AVDISCARD_ALL
	       ||(s.skip_loop_filter >= MpegEncContext.AVDISCARD_NONKEY && h.slice_type_nos != FF_I_TYPE)
	       ||(s.skip_loop_filter >= MpegEncContext.AVDISCARD_BIDIR  && h.slice_type_nos == FF_B_TYPE)
	       ||(s.skip_loop_filter >= MpegEncContext.AVDISCARD_NONREF && h.nal_ref_idc == 0))
	        h.deblocking_filter= 0;

	    if(h.deblocking_filter == 1 && h0.max_contexts > 1) {
	        if((s.flags2 & MpegEncContext.CODEC_FLAG2_FAST)!=0) {
	            /* Cheat slightly for speed:
	               Do not bother to deblock across slices. */
	            h.deblocking_filter = 2;
	        } else {
	            h0.max_contexts = 1;
	            if(0==h0.single_decode_warning) {
	                //av_log(s.avctx, AV_LOG_INFO, "Cannot parallelize deblocking type 1, decoding such frames in sequential order\n");
	                h0.single_decode_warning = 1;
	            }
	            if(h != h0) {
	    	    	// DebugTool.printDebugString("   --- decode_slide_header error case 17\n");
	                return 1; // deblocking switched inside frame
	            }
	        }
	    }
	    h.qp_thresh= 15 + 52 - Math.min(h.slice_alpha_c0_offset, h.slice_beta_offset) - Math.max(Math.max(0, h.pps.chroma_qp_index_offset[0]), h.pps.chroma_qp_index_offset[1]);
/*
	#if 0 //FMO
	    if( h.pps.num_slice_groups > 1  && h.pps.mb_slice_group_map_type >= 3 && h.pps.mb_slice_group_map_type <= 5)
	        slice_group_change_cycle= s.gb.get_bits( ?);
	#endif
*/
	    h0.last_slice_type = slice_type;
	    h.slice_num = ++h0.current_slice;
	    if(h.slice_num >= MAX_SLICES){
	        //av_log(s.avctx, AV_LOG_ERROR, "Too many slices, increase MAX_SLICES and recompile\n");
	    }

	    for(j=0; j<2; j++){
	        int[] id_list = new int[16];
	        int[] ref2frm= h.ref2frm[h.slice_num&(MAX_SLICES-1)][j];
	        for(i=0; i<16; i++){
	            id_list[i]= 60;
	            if(h.ref_list[j][i].data_base[0]!=null){
	                int k;
	                //uint8_t *base= h.ref_list[j][i].base[0];
	                int[] base= h.ref_list[j][i].base[0];
	                for(k=0; k<h.short_ref_count; k++)
	                    if(h.short_ref[k].base[0] == base){
	                        id_list[i]= k;
	                        break;
	                    }
	                for(k=0; k<h.long_ref_count; k++)
	                    if(h.long_ref[k]!=null && h.long_ref[k].base[0] == base){
	                        id_list[i]= h.short_ref_count + k;
	                        break;
	                    }
	            }
	        }

	        ref2frm[0]=
	        ref2frm[1]= -1;
	        for(i=0; i<16; i++)
	            ref2frm[i+2]= 4*id_list[i]
	                          +(h.ref_list[j][i].reference&3);
	        ref2frm[18+0]=
	        ref2frm[18+1]= -1;
	        for(i=16; i<48; i++)
	            ref2frm[i+4]= 4*id_list[(i-16)>>1]
	                          +(h.ref_list[j][i].reference&3);
	    }

	    h.emu_edge_width= ((s.flags&MpegEncContext.CODEC_FLAG_EMU_EDGE)!=0 ? 0 : 16);
	    h.emu_edge_height= (0!=h.mb_aff_frame || (s.picture_structure != MpegEncContext.PICT_FRAME)) ? 0 : h.emu_edge_width;

	    /* No debug
	    if((s.debug&FF_DEBUG_PICT_INFO)!=0){
	        av_log(h.s.avctx, AV_LOG_DEBUG, "slice:%d %s mb:%d %c%s%s pps:%u frame:%d poc:%d/%d ref:%d/%d qp:%d loop:%d:%d:%d weight:%d%s %s\n",
	               h.slice_num,
	               (s.picture_structure==PICT_FRAME ? "F" : s.picture_structure==PICT_TOP_FIELD ? "T" : "B"),
	               first_mb_in_slice,
	               av_get_pict_type_char(h.slice_type), h.slice_type_fixed ? " fix" : "", h.nal_unit_type == NAL_IDR_SLICE ? " IDR" : "",
	               pps_id, h.frame_num,
	               s.current_picture_ptr->field_poc[0], s.current_picture_ptr->field_poc[1],
	               h.ref_count[0], h.ref_count[1],
	               s.qscale,
	               h.deblocking_filter, h.slice_alpha_c0_offset/2-26, h.slice_beta_offset/2-26,
	               h.use_weight,
	               h.use_weight==1 && h.use_weight_chroma ? "c" : "",
	               h.slice_type == FF_B_TYPE ? (h.direct_spatial_mv_pred ? "SPAT" : "TEMP") : ""
	               );
	    }
		*/
    	// DebugTool.printDebugString("   --- decode_slide_header OK\n");
	    return 0;
	}
	
	public void ff_h264_free_context() {
	    int i;
	    this.free_tables(); //FIXME cleanup init stuff perhaps
	    for(i = 0; i < MAX_SPS_COUNT; i++)
	    	this.sps_buffers[i] = null;
	    for(i = 0; i < MAX_PPS_COUNT; i++)
	    	this.pps_buffers[i] = null;
	}
	
	public int ff_h264_decode_extradata() {
	    if(s.extradata[0] == 1){
	        int i, cnt, nalsize;
	        /*unsigned char *p*/ int[] p_base = s.extradata;
	        int p_offset = 0;

	        this.is_avc = 1;

	        if(s.extradata_size < 7) {
	            //av_log(avctx, AV_LOG_ERROR, "avcC too short\n");
	            return -1;
	        }
	        /* sps and pps in the avcC always have length coded with 2 bytes,
	           so put a fake nal_length_size = 2 while parsing them */
	        this.nal_length_size = 2;
	        // Decode sps from avcC
	        cnt = p_base[p_offset + 5] & 0x01f; // Number of sps
	        p_offset += 6;
	        for (i = 0; i < cnt; i++) {
	            //nalsize = AV_RB16(p) + 2;
	        	nalsize = ((p_base[p_offset] & 0x0ff) | ((p_base[p_offset+1] & 0x0ff) << 8))+2;
	            if(decode_nal_units(p_base, p_offset, nalsize) < 0) {
	                //av_log(avctx, AV_LOG_ERROR, "Decoding sps %d from avcC failed\n", i);
	                return -1;
	            }
	            p_offset += nalsize;
	        }
	        // Decode pps from avcC
	        cnt = p_base[p_offset++]; // Number of pps
	        for (i = 0; i < cnt; i++) {
	            //nalsize = AV_RB16(p) + 2;
	        	nalsize = ((p_base[p_offset] & 0x0ff) | ((p_base[p_offset+1] & 0x0ff) << 8))+2;
	            if(decode_nal_units(p_base, p_offset, nalsize)  != nalsize) {
	                //av_log(avctx, AV_LOG_ERROR, "Decoding pps %d from avcC failed\n", i);
	                return -1;
	            }
	            p_offset += nalsize;
	        }
	        // Now store right nal length size, that will be use to parse all other nals
	        this.nal_length_size = (s.extradata[4]&0x03)+1;
	    } else {
	        this.is_avc = 0;
	        if(decode_nal_units(s.extradata, 0, s.extradata_size) < 0)
	            return -1;
	    }
	    return 0;
	}
	
	public void common_init(MpegEncContext _s){
		s = _s;
		
		//?????????????????????????
	    //s.width = s.avctx->width;
	    //s.height = s.avctx->height;
	    //s.codec_id= s.avctx->codec->id;

		this.h264dsp.ff_h264dsp_init();
		this.hpc.initializePredictionContext(s.codec_id);

	    this.dequant_coeff_pps= -1;
	    s.unrestricted_mv=1;
	    s.decode=1; //FIXME

	    s.dsp.dsputil_init(s); // needed so that idct permutation is known early

	    //memset(this.pps.scaling_matrix4, 16, 6*16*sizeof(uint8_t));
	    //memset(this.pps.scaling_matrix8, 16, 2*64*sizeof(uint8_t));
	    for(int i=0;i<this.pps.scaling_matrix4.length;i++)
	    	Arrays.fill(this.pps.scaling_matrix4[i], 16);
	    for(int i=0;i<this.pps.scaling_matrix8.length;i++)
	    	Arrays.fill(this.pps.scaling_matrix4[i], 16);
	}	
	
	public int decode_frame(AVFrame data, int[] data_size, AVPacket avpkt) {
		/* const uint8_t * */int[] buf_base = avpkt.data_base;
		int buf_offset = avpkt.data_offset;
		int buf_size = avpkt.size;
		//AVFrame pict = data;
		int buf_index;

		// ???????????????????????????????????????
		// s.flags= avctx.flags;
		// s.flags2= avctx.flags2;

		/* end of stream, output what is still in the buffers */
		boolean loop = true;
		// out:
		do {
			loop = false;

			if (buf_size == 0) {
				AVFrame out;
				int i, out_idx;

				// FIXME factorize this with the output code below
				out = this.delayed_pic[0];
				out_idx = 0;
				for (i = 1; this.delayed_pic[i] != null
						&& 0 == this.delayed_pic[i].key_frame
						&& 0 == this.delayed_pic[i].mmco_reset; i++)
					if (this.delayed_pic[i].poc < out.poc) {
						out = this.delayed_pic[i];
						out_idx = i;
					}

				for (i = out_idx; this.delayed_pic[i] != null; i++)
					this.delayed_pic[i] = this.delayed_pic[i + 1];

				if (out != null) {
					data_size[0] = 1;
					out.copyTo(displayPicture);
					
					// DebugTool.dumpFrameData(displayPicture);
				}

				return 0;
			}

			buf_index = this.decode_nal_units(buf_base, buf_offset, buf_size);
			if (buf_index < 0)
				return -1;

			if (null == s.current_picture_ptr
					&& this.nal_unit_type == NAL_END_SEQUENCE) {
				buf_size = 0;
				loop = true;
			}

		} while (loop);

		if (0 == (s.flags2 & MpegEncContext.CODEC_FLAG2_CHUNKS)
				&& null == s.current_picture_ptr) {
			if (s.skip_frame >= MpegEncContext.AVDISCARD_NONREF
					|| s.hurry_up != 0)
				return 0;
			// av_log(avctx, AV_LOG_ERROR, "no frame!\n");
			//System.out.println("!!!! NO FRAME !!!!");
			//return s.get_consumed_bytes(buf_index, buf_size); //-1;
			return -1;
		}

		if (0 == (s.flags2 & MpegEncContext.CODEC_FLAG2_CHUNKS)
				|| (s.mb_y >= s.mb_height && s.mb_height != 0)) {
			AVFrame out = s.current_picture_ptr;
			AVFrame cur = s.current_picture_ptr;
			int i, pics, out_of_order, out_idx;

			this.field_end();

			if (cur.field_poc[0] == Integer.MAX_VALUE
					|| cur.field_poc[1] == Integer.MAX_VALUE) {
				/* Wait for second field. */
				data_size[0] = 0;

			} else {
				cur.interlaced_frame = 0;
				cur.repeat_pict = 0;

				/* Signal interlacing information externally. */
				/*
				 * Prioritize picture timing SEI information over used decoding
				 * process if it exists.
				 */

				if (this.sps.pic_struct_present_flag != 0) {
					switch (this.sei_pic_struct) {
					case SEI_PIC_STRUCT_FRAME:
						break;
					case SEI_PIC_STRUCT_TOP_FIELD:
					case SEI_PIC_STRUCT_BOTTOM_FIELD:
						cur.interlaced_frame = 1;
						break;
					case SEI_PIC_STRUCT_TOP_BOTTOM:
					case SEI_PIC_STRUCT_BOTTOM_TOP:
						if ((mb_aff_frame != 0 || (s.picture_structure != MpegEncContext.PICT_FRAME)))
							cur.interlaced_frame = 1;
						else
							// try to flag soft telecine progressive
							cur.interlaced_frame = this.prev_interlaced_frame;
						break;
					case SEI_PIC_STRUCT_TOP_BOTTOM_TOP:
					case SEI_PIC_STRUCT_BOTTOM_TOP_BOTTOM:
						// Signal the possibility of telecined film externally
						// (pic_struct 5,6)
						// From these hints, let the applications decide if they
						// apply deinterlacing.
						cur.repeat_pict = 1;
						break;
					case SEI_PIC_STRUCT_FRAME_DOUBLING:
						// Force progressive here, as doubling interlaced frame
						// is a bad idea.
						cur.repeat_pict = 2;
						break;
					case SEI_PIC_STRUCT_FRAME_TRIPLING:
						cur.repeat_pict = 4;
						break;
					}

					if ((this.sei_ct_type & 3) != 0
							&& this.sei_pic_struct <= SEI_PIC_STRUCT_BOTTOM_TOP)
						cur.interlaced_frame = ((this.sei_ct_type & (1 << 1)) != 0) ? 1
								: 0;
				} else {
					/* Derive interlacing flag from used decoding process. */
					cur.interlaced_frame = (mb_aff_frame != 0 || (s.picture_structure != MpegEncContext.PICT_FRAME)) ? 1
							: 0;
				}
				this.prev_interlaced_frame = cur.interlaced_frame;

				if (cur.field_poc[0] != cur.field_poc[1]) {
					/* Derive top_field_first from field pocs. */
					cur.top_field_first = (cur.field_poc[0] < cur.field_poc[1]) ? 1
							: 0;
				} else {
					if (cur.interlaced_frame != 0
							|| this.sps.pic_struct_present_flag != 0) {
						/*
						 * Use picture timing SEI information. Even if it is a
						 * information of a past frame, better than nothing.
						 */
						if (this.sei_pic_struct == SEI_PIC_STRUCT_TOP_BOTTOM
								|| this.sei_pic_struct == SEI_PIC_STRUCT_TOP_BOTTOM_TOP)
							cur.top_field_first = 1;
						else
							cur.top_field_first = 0;
					} else {
						/* Most likely progressive */
						cur.top_field_first = 0;
					}
				}

				// FIXME do something with unavailable reference frames

				/* Sort B-frames into display order */

				if (this.sps.bitstream_restriction_flag != 0
						&& s.has_b_frames < this.sps.num_reorder_frames) {
					s.has_b_frames = this.sps.num_reorder_frames;
					s.low_delay = 0;
				}

				if (s.strict_std_compliance >= MpegEncContext.FF_COMPLIANCE_STRICT
						&& 0 == this.sps.bitstream_restriction_flag) {
					s.has_b_frames = MAX_DELAYED_PIC_COUNT;
					s.low_delay = 0;
				}

				pics = 0;
				while (this.delayed_pic[pics] != null)
					pics++;

				//assert(pics <= MAX_DELAYED_PIC_COUNT);

				this.delayed_pic[pics++] = cur;
				if (cur.reference == 0)
					cur.reference = DELAYED_PIC_REF;

				out = this.delayed_pic[0];
				out_idx = 0;
				for (i = 1; this.delayed_pic[i] != null
						&& 0 == this.delayed_pic[i].key_frame
						&& 0 == this.delayed_pic[i].mmco_reset; i++)
					if (this.delayed_pic[i].poc < out.poc) {
						out = this.delayed_pic[i];
						out_idx = i;
					}
				if (s.has_b_frames == 0
						&& (this.delayed_pic[0].key_frame != 0 || this.delayed_pic[0].mmco_reset != 0))
					this.outputed_poc = Integer.MIN_VALUE;
				out_of_order = (out.poc < this.outputed_poc) ? 1 : 0;

				if (this.sps.bitstream_restriction_flag != 0
						&& s.has_b_frames >= this.sps.num_reorder_frames) {
				} else if ((out_of_order != 0 && pics - 1 == s.has_b_frames && s.has_b_frames < MAX_DELAYED_PIC_COUNT)
						|| (s.low_delay != 0 && ((this.outputed_poc != Integer.MIN_VALUE && out.poc > this.outputed_poc + 2) || cur.pict_type == FF_B_TYPE))) {
					s.low_delay = 0;
					s.has_b_frames++;
				}

				if (0 != out_of_order || pics > s.has_b_frames) {
					out.reference &= ~DELAYED_PIC_REF;
					for (i = out_idx; this.delayed_pic[i] != null; i++)
						this.delayed_pic[i] = this.delayed_pic[i + 1];
				}
				if (0 == out_of_order && pics > s.has_b_frames) {
					data_size[0] = 1;

					if (out_idx == 0
							&& this.delayed_pic[0] != null
							&& (this.delayed_pic[0].key_frame != 0 || this.delayed_pic[0].mmco_reset != 0)) {
						this.outputed_poc = Integer.MIN_VALUE;
					} else
						this.outputed_poc = out.poc;
					out.copyTo(displayPicture);
					
					// DebugTool.dumpFrameData(displayPicture);
				} else {
					// av_log(avctx, AV_LOG_DEBUG, "no picture\n");
				}
			}
		}

		// //assert(pict.data[0] || !*data_size);
		// ff_print_debug_info(s, pict);
		// printf("out %d\n", (int)pict.data[0]);

		return s.get_consumed_bytes(buf_index, buf_size);
	}
		
}
