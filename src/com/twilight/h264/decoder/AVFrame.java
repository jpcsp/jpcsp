package com.twilight.h264.decoder;

public class AVFrame {
	
	public int imageWidthWOEdge;
	public int imageHeightWOEdge;
	public int imageWidth;
	public int imageHeight;
	
	// FF_COMMON_FRAME
    /**\
     * pointer to the picture planes.\
     * This might be different from the first allocated byte\
     * - encoding: \
     * - decoding: \
     */	
	public int[][] data_base = new int[4][]; //uint8_t *data[4];\
	public int[] data_offset = new int[4]; // Offset in each data_base
	public int[] linesize = new int[4];
    /**\
     * pointer to the first allocated byte of the picture. Can be used in get_buffer/release_buffer.\
     * This isn't used by libavcodec unless the default get/release_buffer() is used.\
     * - encoding: \
     * - decoding: \
     */
     public int[][] base = new int[4][]; //uint8_t *base[4];\
    /**\
     * 1 -> keyframe, 0-> not\
     * - encoding: Set by libavcodec.\
     * - decoding: Set by libavcodec.\
     */
     public int key_frame;
    /**\
     * Picture type of the frame, see ?_TYPE below.\
     * - encoding: Set by libavcodec. for coded_picture (and set by user for input).\
     * - decoding: Set by libavcodec.\
     */
     public int pict_type;
    /**\
     * presentation timestamp in time_base units (time when frame should be shown to user)\
     * If AV_NOPTS_VALUE then frame_rate = 1/time_base will be assumed.\
     * - encoding: MUST be set by user.\
     * - decoding: Set by libavcodec.\
     */
     public long pts;
    /**\
     * picture number in bitstream order\
     * - encoding: set by\
     * - decoding: Set by libavcodec.\
     */
     public int coded_picture_number;
    /**\
     * picture number in display order\
     * - encoding: set by\
     * - decoding: Set by libavcodec.\
     */
     public int display_picture_number;
    /**\
     * quality (between 1 (good) and FF_LAMBDA_MAX (bad)) \
     * - encoding: Set by libavcodec. for coded_picture (and set by user for input).\
     * - decoding: Set by libavcodec.\
     */
     public int quality; 
    /**\
     * buffer age (1->was last buffer and dint change, 2->..., ...).\
     * Set to INT_MAX if the buffer has not been used yet.\
     * - encoding: unused\
     * - decoding: MUST be set by get_buffer().\
     */
     public int age;
    /**\
     * is this picture used as reference\
     * The values for this are the same as the MpegEncContext.picture_structure\
     * variable, that is 1->top field, 2->bottom field, 3->frame/both fields.\
     * Set to 4 for delayed, non-reference frames.\
     * - encoding: unused\
     * - decoding: Set by libavcodec. (before get_buffer() call)).\
     */
     public int reference;
    /**\
     * QP table\
     * - encoding: unused\
     * - decoding: Set by libavcodec.\
     */
     public int[] qscale_table; //int8_t *qscale_table;\
    /**\
     * QP store stride\
     * - encoding: unused\
     * - decoding: Set by libavcodec.\
     */
     public int qstride;
    /**\
     * mbskip_table[mb]>=1 if MB didn't change\
     * stride= mb_width = (width+15)>>4\
     * - encoding: unused\
     * - decoding: Set by libavcodec.\
     */
     public int[] mbskip_table; //uint8_t *mbskip_table;\
    /**\
     * motion vector table\
     * @code\
     * example:\
     * int mv_sample_log2= 4 - motion_subsample_log2;\
     * int mb_width= (width+15)>>4;\
     * int mv_stride= (mb_width << mv_sample_log2) + 1;\
     * motion_val[direction][x + y*mv_stride][0->mv_x, 1->mv_y];\
     * @endcode\
     * - encoding: Set by user.\
     * - decoding: Set by libavcodec.\
     */
     //public int[][][] motion_val = new int[2][][]; // int16_t (*motion_val[2])[2];\
     // Change to segment/offset style on motion_val_base[2][offset][2]
     public int[] motion_val_offset = new int[2]; // int16_t (*motion_val[2])[2];\

     /**\
     * macroblock type table\
     * mb_type_base + mb_width + 2\
     * - encoding: Set by user.\
     * - decoding: Set by libavcodec.\
     */
     public int mb_type_offset; //uint32_t *mb_type;\
    /**\
     * log2 of the size of the block which a single vector in motion_val represents: \
     * (4->16x16, 3->8x8, 2-> 4x4, 1-> 2x2)\
     * - encoding: unused\
     * - decoding: Set by libavcodec.\
     */
     public int motion_subsample_log2; //uint8_t motion_subsample_log2;\
    /**\
     * for some private data of the user\
     * - encoding: unused\
     * - decoding: Set by user.\
     */
     public Object opaque; //void *opaque;\
    /**\
     * error\
     * - encoding: Set by libavcodec. if flags&CODEC_FLAG_PSNR.\
     * - decoding: unused\
     */
     public long[] error = new long[4]; //uint64_t error[4];\
    /**\
     * type of the buffer (to keep track of who has to deallocate data[*])\
     * - encoding: Set by the one who allocates it.\
     * - decoding: Set by the one who allocates it.\
     * Note: User allocated (direct rendering) & internal buffers cannot coexist currently.\
     */
     public int type;
    /**\
     * When decoding, this signals how much the picture must be delayed.\
     * extra_delay = repeat_pict / (2*fps)\
     * - encoding: unused\
     * - decoding: Set by libavcodec.\
     */
     public int repeat_pict;
    /**\
     * \
     */
     public int qscale_type;
    /**\
     * The content of the picture is interlaced.\
     * - encoding: Set by user.\
     * - decoding: Set by libavcodec. (default 0)\
     */
     public int interlaced_frame;
    /**\
     * If the content is interlaced, is top field displayed first.\
     * - encoding: Set by user.\
     * - decoding: Set by libavcodec.\
     */
     public int top_field_first;
    /**\
     * Pan scan.\
     * - encoding: Set by user.\
     * - decoding: Set by libavcodec.\
     */
     public AVPanScan pan_scan;
    /**\
     * Tell user application that palette has changed from previous frame.\
     * - encoding: ??? (no palette-enabled encoder yet)\
     * - decoding: Set by libavcodec. (default 0).\
     */
     public int palette_has_changed;
    /**\
     * codec suggestion on buffer type if != 0\
     * - encoding: unused\
     * - decoding: Set by libavcodec. (before get_buffer() call)).\
     */
     public int buffer_hints;
    /**\
     * DCT coefficients\
     * - encoding: unused\
     * - decoding: Set by libavcodec.\
     */
     public short[] dct_coeff; //short *dct_coeff;\
    /**\
     * motion reference frame index\
     * the order in which these are stored can depend on the codec.\
     * - encoding: Set by user.\
     * - decoding: Set by libavcodec.\
     */
     public int[][] ref_index = new int[2][]; //int8_t *ref_index[2];\
    /**\
     * reordered opaque 64bit (generally an integer or a double precision float\
     * PTS but can be anything). \
     * The user sets AVCodecContext.reordered_opaque to represent the input at\
     * that time,\
     * the decoder reorders values as needed and sets AVFrame.reordered_opaque\
     * to exactly one of the values provided by the user through AVCodecContext.reordered_opaque \
     * - encoding: unused\
     * - decoding: Read by user.\
     */
     public long reordered_opaque; //int64_t reordered_opaque;\
    /**\
     * hardware accelerator private data (FFmpeg allocated)\
     * - encoding: unused\
     * - decoding: Set by libavcodec\
     */
     public Object hwaccel_picture_private;
    /**\
     * reordered pts from the last AVPacket that has been input into the decoder\
     * - encoding: unused\
     * - decoding: Read by user.\
     */
     public long pkt_pts; //int64_t pkt_pts;\
    /**\
     * dts from the last AVPacket that has been input into the decoder\
     * - encoding: unused\
     * - decoding: Read by user.\
     */
     public long pkt_dts; //int64_t pkt_dts;\


     /**
      * halfpel luma planes.
      */
     public int[] interpolated = new int[3]; //uint8_t *interpolated[3];
     public int[][][] motion_val_base = new int[2][][]; // int16_t (*motion_val_base[2])[2];
     public long[] mb_type_base; // uint32_t *mb_type_base;
     public int[] field_poc = new int[2];           ///< h264 top/bottom POC
     public int poc;                    ///< h264 frame POC
     public int frame_num;              ///< h264 frame_num (raw frame_num from slice header)
     public int mmco_reset;             ///< h264 MMCO_RESET set this 1. Reordering code must not mix pictures before and after MMCO_RESET.
     public int pic_id;                 /**< h264 pic_num (short -> no wrap version of pic_num,
                                      pic_num & max_pic_num; long -> long_pic_num) */
     public int long_ref;               ///< 1->long term reference 0->short term reference
     public int[][][] ref_poc = new int[2][2][16];      ///< h264 POCs of the frames used as reference (FIXME need per slice)
     public int[][] ref_count = new int[2][2];        ///< number of entries in ref_poc              (FIXME need per slice)
     public int mbaff;                  ///< h264 1 -> MBAFF frame 0-> not MBAFF

     public int mb_var_sum;             ///< sum of MB variance for current frame
     public int mc_mb_var_sum;          ///< motion compensated MB variance for current frame

     public int[] mb_var;           ///< Table for MB variances
     public int[] mc_mb_var;        ///< Table for motion compensated MB variances
     public int[] mb_mean;           ///< Table for MB luminance
     public int[] mb_cmp_score;      ///< Table for MB cmp scores, for mb decision FIXME remove
/*
     uint16_t *mb_var;           ///< Table for MB variances
     uint16_t *mc_mb_var;        ///< Table for motion compensated MB variances
     uint8_t *mb_mean;           ///< Table for MB luminance
     int32_t *mb_cmp_score;      ///< Table for MB cmp scores, for mb decision FIXME remove
*/
     public int b_frame_score;          /* */

     public AVFrame copyTo(AVFrame ret) {
    	 /////////////???????????????????????????
    	 //To do: Implement this method! 
    	 ret.age = age;
    	 ret.b_frame_score = b_frame_score;
    	 for(int i=0;i<base.length;i++)
    		 ret.base[i] = base[i];
    	 ret.buffer_hints = buffer_hints;
    	 ret.coded_picture_number = coded_picture_number;
    	 for(int i=0;i<data_base.length;i++) {
    		ret.data_base[i] = data_base[i];
    	 	ret.data_offset[i] = data_offset[i];
    	 } // for
    	 ret.dct_coeff = dct_coeff;
    	 ret.display_picture_number = display_picture_number;
    	 System.arraycopy(error, 0, ret.error, 0, error.length);
    	 System.arraycopy(field_poc, 0, ret.field_poc, 0, field_poc.length);
    	 ret.frame_num = frame_num;
    	 ret.imageWidth = imageWidth;
    	 ret.imageHeight = imageHeight;
    	 ret.imageWidthWOEdge = imageWidthWOEdge;
    	 ret.imageHeightWOEdge = imageHeightWOEdge;
    	 ret.interlaced_frame = interlaced_frame;
    	 System.arraycopy(interpolated, 0, ret.interpolated, 0, interpolated.length);
    	 ret.key_frame = key_frame;
    	 System.arraycopy(linesize, 0, ret.linesize, 0, linesize.length);
    	 ret.long_ref = long_ref;
    	 ret.mb_cmp_score = mb_cmp_score;
    	 ret.mb_mean = mb_mean;
    	 ret.mb_type_base = mb_type_base;
    	 ret.mb_type_offset = mb_type_offset;
    	 ret.mb_var = mb_var;
    	 ret.mb_var_sum = mb_var_sum;
    	 ret.mbaff = mbaff;
    	 ret.mbskip_table = mbskip_table;
    	 ret.mc_mb_var = mc_mb_var;
    	 ret.mc_mb_var_sum = mc_mb_var_sum;
    	 ret.mmco_reset = mmco_reset;
    	 ret.motion_subsample_log2 = motion_subsample_log2;
    	 //??????????????? Can we copy it at this depth?
    	 System.arraycopy(motion_val_base, 0, ret.motion_val_base, 0, motion_val_base.length);
    	 System.arraycopy(motion_val_offset, 0, ret.motion_val_offset, 0, motion_val_offset.length);
    	 ret.opaque = opaque;
    	 ret.palette_has_changed = palette_has_changed;
    	 ret.pan_scan = pan_scan;
    	 ret.pic_id = pic_id;
    	 ret.pict_type = pict_type;
    	 ret.pkt_dts = pkt_dts;
    	 ret.pkt_pts = pkt_pts;
    	 ret.poc = poc;
    	 ret.pts = pts;
    	 ret.qscale_table = qscale_table;
    	 ret.qscale_type = qscale_type;
    	 ret.qstride = qstride;
    	 ret.quality = quality;
    	 for(int i=0;i<ref_count.length;i++)
    		 System.arraycopy(ref_count[i], 0, ret.ref_count[i], 0, ref_count[i].length);
    	 System.arraycopy(ref_index, 0, ret.ref_index, 0, ref_index.length);
    	 for(int i=0;i<ref_poc.length;i++)
    		 for(int j=0;j<ref_poc[i].length;j++)
    	    	 System.arraycopy(ref_poc[i][j], 0, ret.ref_poc[i][j], 0, ref_poc[i][j].length);
    	 ret.reference = reference;
    	 ret.reordered_opaque = reordered_opaque;
    	 ret.repeat_pict = repeat_pict;
    	 ret.top_field_first = top_field_first;
    	 ret.type = type;
    	 return ret;
     }

     public void resetToZero() {
    	 /////////////???????????????????????????
    	 //To do: Implement this method! 
     }
     
     public static void pic_as_field(AVFrame pic, int parity) {
		int i;
		for (i = 0; i < 4; ++i) {
			if (parity == MpegEncContext.PICT_BOTTOM_FIELD)
				pic.data_offset[i] += pic.linesize[i];
			pic.reference = parity;
			pic.linesize[i] *= 2;
		}
		pic.poc = pic.field_poc[(parity == MpegEncContext.PICT_BOTTOM_FIELD) ? 1
				: 0];
	}
     
     public static int split_field_copy(AVFrame dest, AVFrame src,
             int parity, int id_add){
		int match = ((src.reference & parity) != 0 ? 1 : 0);
		if (match != 0) {
			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			src.copyTo(dest);
			if (parity != MpegEncContext.PICT_FRAME) {
				pic_as_field(dest, parity);
				dest.pic_id *= 2;
				dest.pic_id += id_add;
			}
		}
		return match;
	}
     
     public static int build_def_list(AVFrame[] def_base, int def_offset, AVFrame[] in_base, int in_offset, int len, int is_long, int sel){
    	    int[] i = new int[]{0,0};
    	    int index=0;

    	    while(i[0]<len || i[1]<len){
    	        while(i[0]<len && !(in_base[in_offset +  i[0] ]!=null && (in_base[in_offset +  i[0] ].reference & sel)!=0))
    	            i[0]++;
    	        while(i[1]<len && !(in_base[in_offset +  i[1] ]!=null && (in_base[in_offset +  i[1] ].reference & (sel^3))!=0))
    	            i[1]++;
    	        if(i[0] < len){
    	            in_base[in_offset +  i[0] ].pic_id= (is_long!=0 ? i[0] : in_base[in_offset +  i[0] ].frame_num);
    	            split_field_copy(def_base[def_offset + index++], in_base[in_offset +  i[0]++ ], sel  , 1);
    	        }
    	        if(i[1] < len){
    	            in_base[in_offset +  i[1] ].pic_id= (is_long!=0 ? i[1] : in_base[in_offset +  i[1] ].frame_num);
    	            split_field_copy(def_base[def_offset + index++], in_base[in_offset +  i[1]++ ], sel^3, 0);
    	        }
    	    }

    	    return index;
    	}

    	public static int add_sorted(AVFrame[] sorted_base, int sorted_offset, AVFrame[] src_base, int src_offset, int len, int limit, int dir){
    	    int i, best_poc;
    	    int out_i= 0;

    	    for(;;){
    	        best_poc= (dir!=0 ? Integer.MIN_VALUE : Integer.MAX_VALUE);

    	        for(i=0; i<len; i++){
    	            int poc= src_base[src_offset + i].poc;
    	            if(((poc > limit) ^ (dir!=0)) && ((poc < best_poc) ^ (dir!=0))){
    	                best_poc= poc;
    	                sorted_base[sorted_offset + out_i]= src_base[src_offset + i];
    	            }
    	        }
    	        if(best_poc == (dir!=0 ? Integer.MIN_VALUE : Integer.MAX_VALUE))
    	            break;
    	        limit= sorted_base[sorted_offset + out_i++].poc - dir;
    	    }
    	    return out_i;
    	}
     
     
     /*
     #define MB_TYPE_INTRA MB_TYPE_INTRA4x4 //default mb_type if there is just one type
     #define IS_INTRA4x4(a)   ((a)&MB_TYPE_INTRA4x4)
     #define IS_INTRA16x16(a) ((a)&MB_TYPE_INTRA16x16)
     #define IS_PCM(a)        ((a)&MB_TYPE_INTRA_PCM)
     #define IS_INTRA(a)      ((a)&7)
     #define IS_INTER(a)      ((a)&(MB_TYPE_16x16|MB_TYPE_16x8|MB_TYPE_8x16|MB_TYPE_8x8))
     #define IS_SKIP(a)       ((a)&MB_TYPE_SKIP)
     #define IS_INTRA_PCM(a)  ((a)&MB_TYPE_INTRA_PCM)
     #define IS_INTERLACED(a) ((a)&MB_TYPE_INTERLACED)
     #define IS_DIRECT(a)     ((a)&MB_TYPE_DIRECT2)
     #define IS_GMC(a)        ((a)&MB_TYPE_GMC)
     #define IS_16X16(a)      ((a)&MB_TYPE_16x16)
     #define IS_16X8(a)       ((a)&MB_TYPE_16x8)
     #define IS_8X16(a)       ((a)&MB_TYPE_8x16)
     #define IS_8X8(a)        ((a)&MB_TYPE_8x8)
     #define IS_SUB_8X8(a)    ((a)&MB_TYPE_16x16) //note reused
     #define IS_SUB_8X4(a)    ((a)&MB_TYPE_16x8)  //note reused
     #define IS_SUB_4X8(a)    ((a)&MB_TYPE_8x16)  //note reused
     #define IS_SUB_4X4(a)    ((a)&MB_TYPE_8x8)   //note reused
     #define IS_ACPRED(a)     ((a)&MB_TYPE_ACPRED)
     #define IS_QUANT(a)      ((a)&MB_TYPE_QUANT)
     #define IS_DIR(a, part, list) ((a) & (MB_TYPE_P0L0<<((part)+2*(list))))
     #define USES_LIST(a, list) ((a) & ((MB_TYPE_P0L0|MB_TYPE_P1L0)<<(2*(list)))) ///< does this mb use listX, note does not work if subMBs
     #define HAS_CBP(a)        ((a)&MB_TYPE_CBP)
     */
    	
	public static AVFrame avcodec_alloc_frame() {
		AVFrame ret = new AVFrame();
	    ret.pts= MpegEncContext.AV_NOPTS_VALUE;
	    ret.key_frame= 1;
		return ret;
	}
    	
}
