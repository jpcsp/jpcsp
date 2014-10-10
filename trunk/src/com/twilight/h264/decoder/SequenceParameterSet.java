package com.twilight.h264.decoder;

public class SequenceParameterSet {
	
    public static final int AVCOL_PRI_BT709      =1; ///< also ITU-R BT1361 / IEC 61966-2-4 / SMPTE RP177 Annex B
    public static final int AVCOL_PRI_UNSPECIFIED=2;
    public static final int AVCOL_PRI_BT470M     =4;
    public static final int AVCOL_PRI_BT470BG    =5; ///< also ITU-R BT601-6 625 / ITU-R BT1358 625 / ITU-R BT1700 625 PAL & SECAM
    public static final int AVCOL_PRI_SMPTE170M  =6; ///< also ITU-R BT601-6 525 / ITU-R BT1358 525 / ITU-R BT1700 NTSC
    public static final int AVCOL_PRI_SMPTE240M  =7; ///< functionally identical to above
    public static final int AVCOL_PRI_FILM       =8;
    public static final int AVCOL_PRI_NB         =9;   ///< Not part of ABI
	
    public static final int AVCOL_TRC_BT709      =1; ///< also ITU-R BT1361
    public static final int AVCOL_TRC_UNSPECIFIED=2;
    public static final int AVCOL_TRC_GAMMA22    =4; ///< also ITU-R BT470M / ITU-R BT1700 625 PAL & SECAM
    public static final int AVCOL_TRC_GAMMA28    =5; ///< also ITU-R BT470BG
    public static final int AVCOL_TRC_NB         =6; ///< Not part of ABI
    
    public static final int AVCOL_SPC_RGB        =0;
    public static final int AVCOL_SPC_BT709      =1; ///< also ITU-R BT1361 / IEC 61966-2-4 xvYCC709 / SMPTE RP177 Annex B
    public static final int AVCOL_SPC_UNSPECIFIED=2;
    public static final int AVCOL_SPC_FCC        =4;
    public static final int AVCOL_SPC_BT470BG    =5; ///< also ITU-R BT601-6 625 / ITU-R BT1358 625 / ITU-R BT1700 625 PAL & SECAM / IEC 61966-2-4 xvYCC601
    public static final int AVCOL_SPC_SMPTE170M  =6; ///< also ITU-R BT601-6 525 / ITU-R BT1358 525 / ITU-R BT1700 NTSC / functionally identical to above
    public static final int AVCOL_SPC_SMPTE240M  =7;
    public static final int AVCOL_SPC_NB         =8; ///< Not part of ABI
        
    public int profile_idc;
    public int level_idc;
    public int chroma_format_idc;
    public int transform_bypass;              ///< qpprime_y_zero_transform_bypass_flag
    public int log2_max_frame_num;            ///< log2_max_frame_num_minus4 + 4
    public int poc_type;                      ///< pic_order_cnt_type
    public int log2_max_poc_lsb;              ///< log2_max_pic_order_cnt_lsb_minus4
    public int delta_pic_order_always_zero_flag;
    public int offset_for_non_ref_pic;
    public int offset_for_top_to_bottom_field;
    public int poc_cycle_length;              ///< num_ref_frames_in_pic_order_cnt_cycle
    public int ref_frame_count;               ///< num_ref_frames
    public int gaps_in_frame_num_allowed_flag;
    public int mb_width;                      ///< pic_width_in_mbs_minus1 + 1
    public int mb_height;                     ///< pic_height_in_map_units_minus1 + 1
    public int frame_mbs_only_flag;
    public int mb_aff;                        ///<mb_adaptive_frame_field_flag
    public int direct_8x8_inference_flag;
    public int crop;                   ///< frame_cropping_flag
    public long crop_left;            ///< frame_cropping_rect_left_offset
    public long crop_right;           ///< frame_cropping_rect_right_offset
    public long crop_top;             ///< frame_cropping_rect_top_offset
    public long crop_bottom;          ///< frame_cropping_rect_bottom_offset
    public int vui_parameters_present_flag;
    public AVRational sar = new AVRational(0,0);
    public int video_signal_type_present_flag;
    public int full_range;
    public int colour_description_present_flag;
    public int color_primaries;
    public int color_trc;
    public int colorspace;
    public int timing_info_present_flag;
    public long num_units_in_tick;
    public long time_scale;
    public int fixed_frame_rate_flag;
    public short[] offset_for_ref_frame = new short[256]; //FIXME dyn aloc?
    public int bitstream_restriction_flag;
    public int num_reorder_frames;
    public int scaling_matrix_present;
    public int[][] scaling_matrix4 = new int[6][16];
    public int[][] scaling_matrix8 = new int[2][64];
    public int nal_hrd_parameters_present_flag;
    public int vcl_hrd_parameters_present_flag;
    public int pic_struct_present_flag;
    public int time_offset_length;
    public int cpb_cnt;                       ///< See H.264 E.1.2
    public int initial_cpb_removal_delay_length; ///< initial_cpb_removal_delay_length_minus1 +1
    public int cpb_removal_delay_length;      ///< cpb_removal_delay_length_minus1 + 1
    public int dpb_output_delay_length;       ///< dpb_output_delay_length_minus1 + 1
    public int bit_depth_luma;                ///< bit_depth_luma_minus8 + 8
    public int bit_depth_chroma;              ///< bit_depth_chroma_minus8 + 8
    public int residual_color_transform_flag; ///< residual_colour_transform_flag
    
    public void copyTo(SequenceParameterSet sps) {
    	if (this == sps) {
    		// Do not copy to myself
    		return;
    	}
        sps.profile_idc = profile_idc;
        sps.level_idc = level_idc;
        sps.chroma_format_idc = chroma_format_idc;
        sps.transform_bypass = transform_bypass;              ///< qpprime_y_zero_transform_bypass_flag
        sps.log2_max_frame_num = log2_max_frame_num;            ///< log2_max_frame_num_minus4 + 4
        sps.poc_type = poc_type;                      ///< pic_order_cnt_type
        sps.log2_max_poc_lsb = log2_max_poc_lsb;              ///< log2_max_pic_order_cnt_lsb_minus4
        sps.delta_pic_order_always_zero_flag = delta_pic_order_always_zero_flag;
        sps.offset_for_non_ref_pic = offset_for_non_ref_pic;
        sps.offset_for_top_to_bottom_field = offset_for_top_to_bottom_field;
        sps.poc_cycle_length = poc_cycle_length;              ///< num_ref_frames_in_pic_order_cnt_cycle
        sps.ref_frame_count = ref_frame_count;               ///< num_ref_frames
        sps.gaps_in_frame_num_allowed_flag = gaps_in_frame_num_allowed_flag;
        sps.mb_width = mb_width;                      ///< pic_width_in_mbs_minus1 + 1
        sps.mb_height = mb_height;                     ///< pic_height_in_map_units_minus1 + 1
        sps.frame_mbs_only_flag = frame_mbs_only_flag;
        sps.mb_aff = mb_aff;                        ///<mb_adaptive_frame_field_flag
        sps.direct_8x8_inference_flag = direct_8x8_inference_flag;
        sps.crop = crop;                   ///< frame_cropping_flag
        sps.crop_left = crop_left;            ///< frame_cropping_rect_left_offset
        sps.crop_right = crop_right;           ///< frame_cropping_rect_right_offset
        sps.crop_top = crop_top;             ///< frame_cropping_rect_top_offset
        sps.crop_bottom = crop_bottom;          ///< frame_cropping_rect_bottom_offset
        sps.vui_parameters_present_flag = vui_parameters_present_flag;
        //sps.sar = null;
        sps.sar.num = sar.num;
        sps.sar.den = sar.den;
        sps.video_signal_type_present_flag = video_signal_type_present_flag;
        sps.full_range = full_range;
        sps.colour_description_present_flag = colour_description_present_flag;
        sps.color_primaries = color_primaries;
        sps.color_trc = color_trc;
        sps.colorspace = colorspace;
        sps.timing_info_present_flag = timing_info_present_flag;
        sps.num_units_in_tick = num_units_in_tick;
        sps.time_scale = time_scale;
        sps.fixed_frame_rate_flag = fixed_frame_rate_flag;
        sps.offset_for_ref_frame = new short[256]; //FIXME dyn aloc?
        System.arraycopy(offset_for_ref_frame, 0, sps.offset_for_ref_frame, 0, 256);
        sps.bitstream_restriction_flag = bitstream_restriction_flag;
        sps.num_reorder_frames = num_reorder_frames;
        sps.scaling_matrix_present = scaling_matrix_present;
        sps.scaling_matrix4 = new int[6][16];
        for(int i=0;i<6;i++)
            System.arraycopy(scaling_matrix4[i], 0, sps.scaling_matrix4[i], 0, 16);
        sps.scaling_matrix8 = new int[2][64];
        for(int i=0;i<2;i++)
            System.arraycopy(scaling_matrix8[i], 0, sps.scaling_matrix8[i], 0, 64);
        sps.nal_hrd_parameters_present_flag = nal_hrd_parameters_present_flag;
        sps.vcl_hrd_parameters_present_flag = vcl_hrd_parameters_present_flag;
        sps.pic_struct_present_flag = pic_struct_present_flag;
        sps.time_offset_length = time_offset_length;
        sps.cpb_cnt = cpb_cnt;                       ///< See H.264 E.1.2
        sps.initial_cpb_removal_delay_length = initial_cpb_removal_delay_length; ///< initial_cpb_removal_delay_length_minus1 +1
        sps.cpb_removal_delay_length = cpb_removal_delay_length;      ///< cpb_removal_delay_length_minus1 + 1
        sps.dpb_output_delay_length = dpb_output_delay_length;       ///< dpb_output_delay_length_minus1 + 1
        sps.bit_depth_luma = bit_depth_luma;                ///< bit_depth_luma_minus8 + 8
        sps.bit_depth_chroma = bit_depth_chroma;              ///< bit_depth_chroma_minus8 + 8
        sps.residual_color_transform_flag = residual_color_transform_flag; ///< residual_colour_transform_flag

    }
}
