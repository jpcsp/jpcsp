package com.twilight.h264.decoder;

public class PictureParameterSet {

    public long sps_id;
    public int cabac;                  ///< entropy_coding_mode_flag
    public int pic_order_present;      ///< pic_order_present_flag
    public int slice_group_count;      ///< num_slice_groups_minus1 + 1
    public int mb_slice_group_map_type;
    public long[] ref_count = new long[2];  ///< num_ref_idx_l0/1_active_minus1 + 1
    public int weighted_pred;          ///< weighted_pred_flag
    public int weighted_bipred_idc;
    public int init_qp;                ///< pic_init_qp_minus26 + 26
    public int init_qs;                ///< pic_init_qs_minus26 + 26
    public int[] chroma_qp_index_offset = new int[2];
    public int deblocking_filter_parameters_present; ///< deblocking_filter_parameters_present_flag
    public int constrained_intra_pred; ///< constrained_intra_pred_flag
    public int redundant_pic_cnt_present; ///< redundant_pic_cnt_present_flag
    public int transform_8x8_mode;     ///< transform_8x8_mode_flag
    public int[][] scaling_matrix4 = new int[6][16];
    public int[][] scaling_matrix8 = new int[2][64];
    public int[][] chroma_qp_table = new int[2][64];  ///< pre-scaled (with chroma_qp_index_offset) version of qp_table
    public int chroma_qp_diff;
    
    public void copyTo(PictureParameterSet pps) {
    	if (this == pps) {
    		// Do not copy to myself
    		return;
    	}
        pps.sps_id = sps_id;
        pps.cabac = cabac;                  ///< entropy_coding_mode_flag
        pps.pic_order_present = pic_order_present;      ///< pic_order_present_flag
        pps.slice_group_count = slice_group_count;      ///< num_slice_groups_minus1 + 1
        pps.mb_slice_group_map_type = mb_slice_group_map_type;
        pps.ref_count = new long[2];  ///< num_ref_idx_l0/1_active_minus1 + 1
        pps.ref_count[0] = ref_count[0];
        pps.ref_count[1] = ref_count[1];
        pps.weighted_pred = weighted_pred;          ///< weighted_pred_flag
        pps.weighted_bipred_idc = weighted_bipred_idc;
        pps.init_qp = init_qp;                ///< pic_init_qp_minus26 + 26
        pps.init_qs = init_qs;                ///< pic_init_qs_minus26 + 26
        pps.chroma_qp_index_offset = new int[2];
        pps.chroma_qp_index_offset[0] = chroma_qp_index_offset[0];
        pps.chroma_qp_index_offset[1] = chroma_qp_index_offset[1];
        pps.deblocking_filter_parameters_present = deblocking_filter_parameters_present; ///< deblocking_filter_parameters_present_flag
        pps.constrained_intra_pred = constrained_intra_pred; ///< constrained_intra_pred_flag
        pps.redundant_pic_cnt_present = redundant_pic_cnt_present; ///< redundant_pic_cnt_present_flag
        pps.transform_8x8_mode = transform_8x8_mode;     ///< transform_8x8_mode_flag
        pps.scaling_matrix4 = new int[6][16];
        for(int i=0;i<6;i++)
        	System.arraycopy(scaling_matrix4[i], 0, pps.scaling_matrix4[i], 0, 16);
        pps.scaling_matrix8 = new int[2][64];
        pps.chroma_qp_table = new int[2][64];  ///< pre-scaled (with chroma_qp_index_offset) version of qp_table
        for(int i=0;i<2;i++)
        	System.arraycopy(scaling_matrix8[i], 0, pps.scaling_matrix8[i], 0, 64);
        for(int i=0;i<2;i++)
        	System.arraycopy(chroma_qp_table[i], 0, pps.chroma_qp_table[i], 0, 64);
        pps.chroma_qp_diff = chroma_qp_diff;
    }
		
}
