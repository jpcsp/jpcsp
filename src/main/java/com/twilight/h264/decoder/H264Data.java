package com.twilight.h264.decoder;

public class H264Data {

	public static final int[] golomb_to_pict_type =
	{H264Context.FF_P_TYPE, H264Context.FF_B_TYPE, H264Context.FF_I_TYPE, H264Context.FF_SP_TYPE, H264Context.FF_SI_TYPE};

	public static final short[] golomb_to_intra4x4_cbp = {
	 47, 31, 15,  0, 23, 27, 29, 30,  7, 11, 13, 14, 39, 43, 45, 46,
	 16,  3,  5, 10, 12, 19, 21, 26, 28, 35, 37, 42, 44,  1,  2,  4,
	  8, 17, 18, 20, 24,  6,  9, 22, 25, 32, 33, 34, 36, 40, 38, 41
	};

	public static final short[] golomb_to_inter_cbp = {
	  0, 16,  1,  2,  4,  8, 32,  3,  5, 10, 12, 15, 47,  7, 11, 13,
	 14,  6,  9, 31, 35, 37, 42, 44, 33, 34, 36, 40, 39, 43, 45, 46,
	 17, 18, 20, 24, 19, 21, 26, 28, 23, 27, 29, 30, 22, 25, 38, 41
	};

	public static final int[] zigzag_scan = {
	 0+0*4, 1+0*4, 0+1*4, 0+2*4,
	 1+1*4, 2+0*4, 3+0*4, 2+1*4,
	 1+2*4, 0+3*4, 1+3*4, 2+2*4,
	 3+1*4, 3+2*4, 2+3*4, 3+3*4,
	};

	public static final int[] field_scan = {
	 0+0*4, 0+1*4, 1+0*4, 0+2*4,
	 0+3*4, 1+1*4, 1+2*4, 1+3*4,
	 2+0*4, 2+1*4, 2+2*4, 2+3*4,
	 3+0*4, 3+1*4, 3+2*4, 3+3*4,
	};

	public static final short[] luma_dc_zigzag_scan = {
	 0*16 + 0*64, 1*16 + 0*64, 2*16 + 0*64, 0*16 + 2*64,
	 3*16 + 0*64, 0*16 + 1*64, 1*16 + 1*64, 2*16 + 1*64,
	 1*16 + 2*64, 2*16 + 2*64, 3*16 + 2*64, 0*16 + 3*64,
	 3*16 + 1*64, 1*16 + 3*64, 2*16 + 3*64, 3*16 + 3*64,
	};

	public static final short[] luma_dc_field_scan = {
	 0*16 + 0*64, 2*16 + 0*64, 1*16 + 0*64, 0*16 + 2*64,
	 2*16 + 2*64, 3*16 + 0*64, 1*16 + 2*64, 3*16 + 2*64,
	 0*16 + 1*64, 2*16 + 1*64, 0*16 + 3*64, 2*16 + 3*64,
	 1*16 + 1*64, 3*16 + 1*64, 1*16 + 3*64, 3*16 + 3*64,
	};

	public static final int[] chroma_dc_scan = {
	 0,1,2,3
	};

	// zigzag_scan8x8_cavlc[i] = zigzag_scan8x8[(i/4) + 16*(i%4)]
	public static final int[] zigzag_scan8x8_cavlc = {
	 0+0*8, 1+1*8, 1+2*8, 2+2*8,
	 4+1*8, 0+5*8, 3+3*8, 7+0*8,
	 3+4*8, 1+7*8, 5+3*8, 6+3*8,
	 2+7*8, 6+4*8, 5+6*8, 7+5*8,
	 1+0*8, 2+0*8, 0+3*8, 3+1*8,
	 3+2*8, 0+6*8, 4+2*8, 6+1*8,
	 2+5*8, 2+6*8, 6+2*8, 5+4*8,
	 3+7*8, 7+3*8, 4+7*8, 7+6*8,
	 0+1*8, 3+0*8, 0+4*8, 4+0*8,
	 2+3*8, 1+5*8, 5+1*8, 5+2*8,
	 1+6*8, 3+5*8, 7+1*8, 4+5*8,
	 4+6*8, 7+4*8, 5+7*8, 6+7*8,
	 0+2*8, 2+1*8, 1+3*8, 5+0*8,
	 1+4*8, 2+4*8, 6+0*8, 4+3*8,
	 0+7*8, 4+4*8, 7+2*8, 3+6*8,
	 5+5*8, 6+5*8, 6+6*8, 7+7*8,
	};

	public static final int[] field_scan8x8 = {
	 0+0*8, 0+1*8, 0+2*8, 1+0*8,
	 1+1*8, 0+3*8, 0+4*8, 1+2*8,
	 2+0*8, 1+3*8, 0+5*8, 0+6*8,
	 0+7*8, 1+4*8, 2+1*8, 3+0*8,
	 2+2*8, 1+5*8, 1+6*8, 1+7*8,
	 2+3*8, 3+1*8, 4+0*8, 3+2*8,
	 2+4*8, 2+5*8, 2+6*8, 2+7*8,
	 3+3*8, 4+1*8, 5+0*8, 4+2*8,
	 3+4*8, 3+5*8, 3+6*8, 3+7*8,
	 4+3*8, 5+1*8, 6+0*8, 5+2*8,
	 4+4*8, 4+5*8, 4+6*8, 4+7*8,
	 5+3*8, 6+1*8, 6+2*8, 5+4*8,
	 5+5*8, 5+6*8, 5+7*8, 6+3*8,
	 7+0*8, 7+1*8, 6+4*8, 6+5*8,
	 6+6*8, 6+7*8, 7+2*8, 7+3*8,
	 7+4*8, 7+5*8, 7+6*8, 7+7*8,
	};

	public static final int[] field_scan8x8_cavlc = {
	 0+0*8, 1+1*8, 2+0*8, 0+7*8,
	 2+2*8, 2+3*8, 2+4*8, 3+3*8,
	 3+4*8, 4+3*8, 4+4*8, 5+3*8,
	 5+5*8, 7+0*8, 6+6*8, 7+4*8,
	 0+1*8, 0+3*8, 1+3*8, 1+4*8,
	 1+5*8, 3+1*8, 2+5*8, 4+1*8,
	 3+5*8, 5+1*8, 4+5*8, 6+1*8,
	 5+6*8, 7+1*8, 6+7*8, 7+5*8,
	 0+2*8, 0+4*8, 0+5*8, 2+1*8,
	 1+6*8, 4+0*8, 2+6*8, 5+0*8,
	 3+6*8, 6+0*8, 4+6*8, 6+2*8,
	 5+7*8, 6+4*8, 7+2*8, 7+6*8,
	 1+0*8, 1+2*8, 0+6*8, 3+0*8,
	 1+7*8, 3+2*8, 2+7*8, 4+2*8,
	 3+7*8, 5+2*8, 4+7*8, 5+4*8,
	 6+3*8, 6+5*8, 7+3*8, 7+7*8,
	};

	public static IMbInfo[] i_mb_type_info = {
	new IMbInfo(H264Context.MB_TYPE_INTRA4x4  , -1, -1),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  2,  0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  1,  0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  0,  0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  3,  0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  2,  16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  1,  16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  0,  16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  3,  16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  2,  32),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  1,  32),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  0,  32),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  3,  32),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  2,  15+0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  1,  15+0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  0,  15+0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  3,  15+0),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  2,  15+16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  1,  15+16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  0,  15+16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  3,  15+16),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  2,  15+32),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  1,  15+32),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  0,  15+32),
	new IMbInfo(H264Context.MB_TYPE_INTRA16x16,  3,  15+32),
	new IMbInfo(H264Context.MB_TYPE_INTRA_PCM , -1, -1),
	};

	public static final short[][] dequant4_coeff_init = {
	  {10,13,16},
	  {11,14,18},
	  {13,16,20},
	  {14,18,23},
	  {16,20,25},
	  {18,23,29},
	};

	public static final short[] dequant8_coeff_init_scan = {
	  0,3,4,3, 3,1,5,1, 4,5,2,5, 3,1,5,1
	};
	public static final short[][] dequant8_coeff_init = {
	  {20,18,32,19,25,24},
	  {22,19,35,21,28,26},
	  {26,23,42,24,33,31},
	  {28,25,45,26,35,33},
	  {32,28,51,30,40,38},
	  {36,32,58,34,46,43},
	};

	public static PMbInfo[] p_mb_type_info = {
		new PMbInfo(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_P0L0             , 1),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P1L0, 2),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P1L0, 2),
		new PMbInfo(H264Context.MB_TYPE_8x8  |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P1L0, 4),
		new PMbInfo(H264Context.MB_TYPE_8x8  |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_REF0, 4),
		};

	public static PMbInfo[] p_sub_mb_type_info = {
		new PMbInfo(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_P0L0             , 1),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0             , 2),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0             , 2),
		new PMbInfo(H264Context.MB_TYPE_8x8  |H264Context.MB_TYPE_P0L0             , 4),
	};
	
	public static PMbInfo[] b_mb_type_info = {
		new PMbInfo(H264Context.MB_TYPE_DIRECT2|H264Context.MB_TYPE_L0L1                                      , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_P0L0                                       , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x16             |H264Context.MB_TYPE_P0L1                          , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1                          , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0             |H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0             |H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8              |H264Context.MB_TYPE_P0L1             |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16              |H264Context.MB_TYPE_P0L1             |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0                          |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0                          |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8              |H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16              |H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0             |H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0             |H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8              |H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16              |H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1             |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1             |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x8  |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 4 ),
		};
	
	public static PMbInfo[] b_sub_mb_type_info = {
		new PMbInfo(H264Context.MB_TYPE_DIRECT2                                                   , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_P0L0                                       , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x16             |H264Context.MB_TYPE_P0L1                          , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1                          , 1 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0             |H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0             |H264Context.MB_TYPE_P1L0             , 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8              |H264Context.MB_TYPE_P0L1             |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16              |H264Context.MB_TYPE_P0L1             |H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_16x8 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x16 |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 2 ),
		new PMbInfo(H264Context.MB_TYPE_8x8  |H264Context.MB_TYPE_P0L0             |H264Context.MB_TYPE_P1L0             , 4 ),
		new PMbInfo(H264Context.MB_TYPE_8x8               |H264Context.MB_TYPE_P0L1             |H264Context.MB_TYPE_P1L1, 4 ),
		new PMbInfo(H264Context.MB_TYPE_8x8  |H264Context.MB_TYPE_P0L0|H264Context.MB_TYPE_P0L1|H264Context.MB_TYPE_P1L0|H264Context.MB_TYPE_P1L1, 4 ),
		};
	
}
