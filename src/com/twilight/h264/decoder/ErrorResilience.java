package com.twilight.h264.decoder;

import com.twilight.h264.util.*;

public class ErrorResilience {

	public static final int MV_FROZEN    =3;
	public static final int MV_CHANGED   =2;
	public static final int MV_UNCHANGED =1;
	
	public static final int MV_TYPE_16X16 = 0;
	public static final int MV_TYPE_8X8 = 1;
	public static final int MV_TYPE_16X8 = 2;
	public static final int MV_TYPE_FIELD = 3;
	public static final int MV_TYPE_DMV = 4;

	public static final int FF_EC_GUESS_MVS = 1;
	public static final int FF_EC_DEBLOCK = 2;
	public static int error_concealment = 3;
	
	public static int[] dc_val_base;
	public static int[] dc_val = new int[3];
	public static int[][][] mv = new int[2][4][2];

	// Number of MacroBlock rows at top/bottom those are skipped, Set by User. 
	public static int skip_top;
	public static int skip_bottom;
	
	public static void decode_mb(MpegEncContext s, int ref){
		//!!??????????? We need these?
	    //s.dest[0] = s.current_picture.data[0] + (s.mb_y * 16* s.linesize  ) + s.mb_x * 16;
	    //s.dest[1] = s.current_picture.data[1] + (s.mb_y * (16>>s.chroma_y_shift) * s.uvlinesize) + s.mb_x * (16>>s.chroma_x_shift);
	    //s.dest[2] = s.current_picture.data[2] + (s.mb_y * (16>>s.chroma_y_shift) * s.uvlinesize) + s.mb_x * (16>>s.chroma_x_shift);

	    if(true && s.codec_id == H264PredictionContext.CODEC_ID_H264){
	        H264Context h= s.priv_data;
	        h.mb_xy= s.mb_x + s.mb_y*s.mb_stride;
	        //memset(h.non_zero_count_cache, 0, sizeof(h.non_zero_count_cache));
	        Arrays.fill(h.non_zero_count_cache, 0);
	        //assert(ref>=0);
	        if(ref >= h.ref_count[0]) //FIXME it is posible albeit uncommon that slice references differ between slices, we take the easy approuch and ignore it for now. If this turns out to have any relevance in practice then correct remapping should be added
	            ref=0;
	        Rectangle.fill_rectangle_sign(s.current_picture.ref_index[0], 4*h.mb_xy, 2, 2, 2, ref, 1);
	        Rectangle.fill_rectangle_sign(h.ref_cache[0], h.scan8[0], 4, 4, 8, ref, 1);
	        Rectangle.fill_rectangle_mv_cache(h.mv_cache[0],  h.scan8[0] , 4, 4, 8, h.pack16to32(mv[0][0][0],mv[0][0][1]), 4);
	        ////assert(!FRAME_MBAFF);
	        h.ff_h264_hl_decode_mb();
	    }else{
	    // Support only H264.
	    //    //assert(ref==0);
	    //    MPV_decode_mb(s, s.block);
	    }
	}
	
	/**
	 * @param stride the number of MVs to get to the next row
	 * @param mv_step the number of MVs per row or column in a macroblock
	 */
	//public static void set_mv_strides(MpegEncContext s, int *mv_step, int *stride){
	public static void set_mv_strides(MpegEncContext s, int[] inout){
	    if(s.codec_id == H264PredictionContext.CODEC_ID_H264){
	        H264Context h= s.priv_data;
	        //assert(s.quarter_sample!=0);
	        inout[0]= 4;
	        inout[1]= h.b_stride;
	    }else{
	    	inout[0]= 2;
	    	inout[1]= s.b8_stride;
	    }
	}

	/**
	 * replaces the current MB with a flat dc only version.
	 */
	public static void put_dc(MpegEncContext s, 
			int[] dest_y_base, int dest_y_offset, 
			int[] dest_cb_base, int dest_cb_offset, 
			int[] dest_cr_base, int dest_cr_offset, 
			int mb_x, int mb_y)
	{
		/* H264 does not use this function ???*/
	    int dc, dcu, dcv, y, i;
	    for(i=0; i<4; i++){
	        dc= dc_val_base[dc_val[0] + mb_x*2 + (i&1) + (mb_y*2 + (i>>1))*s.b8_stride];
	        if(dc<0) dc=0;
	        else if(dc>2040) dc=2040;
	        for(y=0; y<8; y++){
	            int x;
	            for(x=0; x<8; x++){
	                dest_y_base[dest_y_offset + x + (i&1)*8 + (y + (i>>1)*8)*s.linesize]= dc/8;
	            }
	        }
	    }
	    dcu = dc_val_base[dc_val[1] + mb_x + mb_y*s.mb_stride];
	    dcv = dc_val_base[dc_val[2] + mb_x + mb_y*s.mb_stride];
	    if     (dcu<0   ) dcu=0;
	    else if(dcu>2040) dcu=2040;
	    if     (dcv<0   ) dcv=0;
	    else if(dcv>2040) dcv=2040;
	    for(y=0; y<8; y++){
	        int x;
	        for(x=0; x<8; x++){
	            dest_cb_base[dest_cb_offset + x + y*(s.uvlinesize)]= dcu/8;
	            dest_cr_base[dest_cr_offset + x + y*(s.uvlinesize)]= dcv/8;
	        }
	    }
	}

	//public static void filter181(int16_t *data, int width, int height, int stride){
	public static void filter181(int[] data_base, int data_offset, int width, int height, int stride){
	    int x,y;

	    /* horizontal filter */
	    for(y=1; y<height-1; y++){
	        int prev_dc= data_base[data_offset + 0 + y*stride];

	        for(x=1; x<width-1; x++){
	            int dc;

	            dc= - prev_dc
	                + data_base[data_offset + x     + y*stride]*8
	                - data_base[data_offset + x + 1 + y*stride];
	            dc= (dc*10923 + 32768)>>16;
	            prev_dc= data_base[data_offset + x + y*stride];
	            
	            // DebugTool.printDebugString("HFilter: dc_value["+(x + y*stride)+"] = "+dc+"\n");
	            data_base[data_offset + x + y*stride]= dc;
	        }
	    }

	    /* vertical filter */
	    for(x=1; x<width-1; x++){
	        int prev_dc= data_base[data_offset + x];

	        for(y=1; y<height-1; y++){
	            int dc;

	            dc= - prev_dc
	                + data_base[data_offset + x +  y   *stride]*8
	                - data_base[data_offset + x + (y+1)*stride];
	            dc= (dc*10923 + 32768)>>16;
	            prev_dc= data_base[data_offset + x + y*stride];

	            // DebugTool.printDebugString("VFilter: dc_value["+(x + y*stride)+"] = "+dc+"\n");

	            data_base[data_offset + x + y*stride]= dc;
	        }
	    }
	}

	/**
	 * guess the dc of blocks which do not have an undamaged dc
	 * @param w     width in 8 pixel blocks
	 * @param h     height in 8 pixel blocks
	 */
	public static void guess_dc(MpegEncContext s, int[] dc_base, int dc_offset, 
			int w, int h, int stride, int is_luma){
	    int b_x, b_y;

	    for(b_y=0; b_y<h; b_y++){
	        for(b_x=0; b_x<w; b_x++){
	            int color[]={1024,1024,1024,1024};
	            int distance[]={9999,9999,9999,9999};
	            int mb_index, error, j;
	            long guess, weight_sum;

	            mb_index= (b_x>>is_luma) + (b_y>>is_luma)*s.mb_stride;

	            error= s.error_status_table[mb_index];

	            if(( ((s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_index])) &(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_16x8|H264Context.MB_TYPE_8x16|H264Context.MB_TYPE_8x8))!=0) continue; //inter
	            if(0==(error&MpegEncContext.DC_ERROR)) continue;           //dc-ok

	            /* right block */
	            for(j=b_x+1; j<w; j++){
	                int mb_index_j= (j>>is_luma) + (b_y>>is_luma)*s.mb_stride;
	                int error_j= s.error_status_table[mb_index_j];
	                int intra_j= (7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_index_j]) );
	                if(intra_j==0 || 0==(error_j&MpegEncContext.DC_ERROR)){
	                    color[0]= dc_base[dc_offset + j + b_y*stride];
	                    distance[0]= j-b_x;
	                    break;
	                }
	            }

	            /* left block */
	            for(j=b_x-1; j>=0; j--){
	                int mb_index_j= (j>>is_luma) + (b_y>>is_luma)*s.mb_stride;
	                int error_j= s.error_status_table[mb_index_j];
	                int intra_j= 7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_index_j]);
	                if(intra_j==0 || 0==(error_j&MpegEncContext.DC_ERROR)){
	                    color[1]= dc_base[dc_offset + j + b_y*stride];
	                    distance[1]= b_x-j;
	                    break;
	                }
	            }

	            /* bottom block */
	            for(j=b_y+1; j<h; j++){
	                int mb_index_j= (b_x>>is_luma) + (j>>is_luma)*s.mb_stride;
	                int error_j= s.error_status_table[mb_index_j];
	                int intra_j= 7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_index_j]);
	                if(intra_j==0 || 0==(error_j&MpegEncContext.DC_ERROR)){
	                    color[2]= dc_base[dc_offset + b_x + j*stride];
	                    distance[2]= j-b_y;
	                    break;
	                }
	            }

	            /* top block */
	            for(j=b_y-1; j>=0; j--){
	                int mb_index_j= (b_x>>is_luma) + (j>>is_luma)*s.mb_stride;
	                int error_j= s.error_status_table[mb_index_j];
	                int intra_j= 7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_index_j]);
	                if(intra_j==0 || 0==(error_j&MpegEncContext.DC_ERROR)){
	                    color[3]= dc_base[dc_offset + b_x + j*stride];
	                    distance[3]= b_y-j;
	                    break;
	                }
	            }

	            weight_sum=0;
	            guess=0;
	            for(j=0; j<4; j++){
	                long weight= 256*256*256*16/distance[j];
	                guess+= weight*(long)color[j];
	                weight_sum+= weight;
	            }
	            guess= (guess + weight_sum/2) / weight_sum;

	            // DebugTool.printDebugString("GUESS dc_base["+(b_x + b_y*stride)+"] = "+((int)guess)+"\n");
	            dc_base[dc_offset + b_x + b_y*stride]= (int) guess;
	        }
	    }
	}

	/**
	 * simple horizontal deblocking filter used for error resilience
	 * @param w     width in 8 pixel blocks
	 * @param h     height in 8 pixel blocks
	 */
	public static void h_block_filter(MpegEncContext s, 
			int[] dst_base, int dst_offset,
			int w, int h, int stride, int is_luma){
	    int b_x, b_y, mvx_stride, mvy_stride;
	    int[] cm_base = H264DSPContext.ff_cropTbl;
	    int cm_offset = H264DSPContext.MAX_NEG_CROP;
	    int[] param = new int[2];
	    set_mv_strides(s, param);
	    mvx_stride = param[0];
	    mvy_stride = param[1];
	    mvx_stride >>= is_luma;
	    mvy_stride *= mvx_stride;
	    
	    // DebugTool.printDebugString("running h_block_filter\n");

	    for(b_y=0; b_y<h; b_y++){
	        for(b_x=0; b_x<w-1; b_x++){
	            int y;
	            int left_status = s.error_status_table[( b_x   >>is_luma) + (b_y>>is_luma)*s.mb_stride];
	            int right_status= s.error_status_table[((b_x+1)>>is_luma) + (b_y>>is_luma)*s.mb_stride];
	            int left_intra=   7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + ( b_x   >>is_luma) + (b_y>>is_luma)*s.mb_stride]);
	            int right_intra=  7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + ((b_x+1)>>is_luma) + (b_y>>is_luma)*s.mb_stride]);
	            int left_damage =  left_status&(MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR);
	            int right_damage= right_status&(MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR);
	            int offset= b_x*8 + b_y*stride*8;
	            int[] left_mv=  s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mvy_stride*b_y + mvx_stride* b_x   ];
	            int[] right_mv= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mvy_stride*b_y + mvx_stride*(b_x+1)];

	            // DebugTool.printDebugString("("+b_x+","+b_y+") left_damage="+left_damage+", right_damage="+right_damage+"\n");
	            
	            if(!(left_damage!=0||right_damage!=0)) continue; // both undamaged

	            if(   (0==left_intra) && (0==right_intra)
	               && Math.abs(left_mv[0]-right_mv[0]) + Math.abs(left_mv[1]+right_mv[1]) < 2) continue;

	            for(y=0; y<8; y++){
	                int a,b,c,d;

	                a= dst_base[dst_offset + offset + 7 + y*stride] - dst_base[dst_offset + offset + 6 + y*stride];
	                b= dst_base[dst_offset + offset + 8 + y*stride] - dst_base[dst_offset + offset + 7 + y*stride];
	                c= dst_base[dst_offset + offset + 9 + y*stride] - dst_base[dst_offset + offset + 8 + y*stride];

	                d= Math.abs(b) - ((Math.abs(a) + Math.abs(c) + 1)>>1);
	                d= Math.max(d, 0);
	                if(b<0) d= -d;

	                if(d==0) continue;

	                if(!(left_damage!=0 && right_damage!=0))
	                    d= d*16/9;

	                if(left_damage!=0){
	                    dst_base[dst_offset + offset + 7 + y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + 7 + y*stride] + ((d*7)>>4)];
	                    dst_base[dst_offset + offset + 6 + y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + 6 + y*stride] + ((d*5)>>4)];
	                    dst_base[dst_offset + offset + 5 + y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + 5 + y*stride] + ((d*3)>>4)];
	                    dst_base[dst_offset + offset + 4 + y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + 4 + y*stride] + ((d*1)>>4)];
	                }
	                if(right_damage!=0){
	                    dst_base[dst_offset + offset + 8 + y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + 8 + y*stride] - ((d*7)>>4)];
	                    dst_base[dst_offset + offset + 9 + y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + 9 + y*stride] - ((d*5)>>4)];
	                    dst_base[dst_offset + offset + 10+ y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset +10 + y*stride] - ((d*3)>>4)];
	                    dst_base[dst_offset + offset + 11+ y*stride] = cm_base[cm_offset + dst_base[dst_offset + offset +11 + y*stride] - ((d*1)>>4)];
	                }
	            }
	        }
	    }
	}

	/**
	 * simple vertical deblocking filter used for error resilience
	 * @param w     width in 8 pixel blocks
	 * @param h     height in 8 pixel blocks
	 */
	public static void v_block_filter(MpegEncContext s, 
			int[] dst_base, int dst_offset,
			int w, int h, int stride, int is_luma){
	    int b_x, b_y, mvx_stride, mvy_stride;
	    int[] cm_base = H264DSPContext.ff_cropTbl;
	    int cm_offset = H264DSPContext.MAX_NEG_CROP;
	    int[] param = new int[2];
	    set_mv_strides(s, param);
	    mvx_stride = param[0];
	    mvy_stride = param[1];
	    mvx_stride >>= is_luma;
	    mvy_stride *= mvx_stride;

	    // DebugTool.printDebugString("running v_block_filter\n");
	    
	    for(b_y=0; b_y<h-1; b_y++){
	        for(b_x=0; b_x<w; b_x++){
	            int x;
	            int top_status   = s.error_status_table[(b_x>>is_luma) + ( b_y   >>is_luma)*s.mb_stride];
	            int bottom_status= s.error_status_table[(b_x>>is_luma) + ((b_y+1)>>is_luma)*s.mb_stride];
	            int top_intra=     7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + (b_x>>is_luma) + ( b_y   >>is_luma)*s.mb_stride]);
	            int bottom_intra=  7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + (b_x>>is_luma) + ((b_y+1)>>is_luma)*s.mb_stride]);
	            int top_damage =      top_status&(MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR);
	            int bottom_damage= bottom_status&(MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR);
	            int offset= b_x*8 + b_y*stride*8;
	            int[] top_mv=    s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mvy_stride* b_y    + mvx_stride*b_x];
	            int[] bottom_mv= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mvy_stride*(b_y+1) + mvx_stride*b_x];

	            // DebugTool.printDebugString("("+b_x+","+b_y+") top_damage="+top_damage+", bottom_damage="+bottom_damage+"\n");
	            
	            if(!(top_damage!=0||bottom_damage!=0)) continue; // both undamaged

	            if(   (0==top_intra) && (0==bottom_intra)
	               && Math.abs(top_mv[0]-bottom_mv[0]) + Math.abs(top_mv[1]+bottom_mv[1]) < 2) continue;

	            for(x=0; x<8; x++){
	                int a,b,c,d;

	                a= dst_base[dst_offset + offset + x + 7*stride] - dst_base[dst_offset + offset + x + 6*stride];
	                b= dst_base[dst_offset + offset + x + 8*stride] - dst_base[dst_offset + offset + x + 7*stride];
	                c= dst_base[dst_offset + offset + x + 9*stride] - dst_base[dst_offset + offset + x + 8*stride];

	                d= Math.abs(b) - ((Math.abs(a) + Math.abs(c)+1)>>1);
	                d= Math.max(d, 0);
	                if(b<0) d= -d;

	                if(d==0) continue;

	                if(!(0!=top_damage && 0!=bottom_damage))
	                    d= d*16/9;

	                if(0!=top_damage){
	                    dst_base[dst_offset + offset + x +  7*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x +  7*stride] + ((d*7)>>4)];
	                    dst_base[dst_offset + offset + x +  6*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x +  6*stride] + ((d*5)>>4)];
	                    dst_base[dst_offset + offset + x +  5*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x +  5*stride] + ((d*3)>>4)];
	                    dst_base[dst_offset + offset + x +  4*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x +  4*stride] + ((d*1)>>4)];
	                }
	                if(0!=bottom_damage){
	                    dst_base[dst_offset + offset + x +  8*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x +  8*stride] - ((d*7)>>4)];
	                    dst_base[dst_offset + offset + x +  9*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x +  9*stride] - ((d*5)>>4)];
	                    dst_base[dst_offset + offset + x + 10*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x + 10*stride] - ((d*3)>>4)];
	                    dst_base[dst_offset + offset + x + 11*stride] = cm_base[cm_offset + dst_base[dst_offset + offset + x + 11*stride] - ((d*1)>>4)];
	                }
	            }
	        }
	    }
	}

	public static void guess_mv(MpegEncContext s){
	    //uint8_t fixed[s.mb_stride * s.mb_height];
		int[] fixed = new int[s.mb_stride * s.mb_height];
	    int mb_stride = s.mb_stride;
	    int mb_width = s.mb_width;
	    int mb_height= s.mb_height;
	    int i, depth, num_avail;
	    int mb_x, mb_y, mot_step, mot_stride;

	    int[] param = new int[2];
	    set_mv_strides(s, param);
	    mot_step = param[0];
	    mot_stride = param[1];

	    num_avail=0;
	    for(i=0; i<s.mb_num; i++){
	        int mb_xy= s.mb_index2xy[ i ];
	        int f=0;
	        int error= s.error_status_table[mb_xy];

	        if(0!=(7&(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]))) 
	        	f=MV_FROZEN; //intra //FIXME check
	        if(0==(error&MpegEncContext.MV_ERROR)) f=MV_FROZEN;           //inter with undamaged MV

	        fixed[mb_xy]= f;
	        if(f==MV_FROZEN)
	            num_avail++;
	    }

	    if((0==(error_concealment&FF_EC_GUESS_MVS)) || num_avail <= mb_width/2){
	        for(mb_y=0; mb_y<s.mb_height; mb_y++){
	            for(mb_x=0; mb_x<s.mb_width; mb_x++){
	                int mb_xy= mb_x + mb_y*s.mb_stride;

	                if(0!=(7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy])))  continue;
	                if(0==(s.error_status_table[mb_xy]&MpegEncContext.MV_ERROR)) continue;

	                s.mv_dir = (s.last_picture.data_base[0]!=null) ? MpegEncContext.MV_DIR_FORWARD : MpegEncContext.MV_DIR_BACKWARD;
	                s.mb_intra=0;
	                s.mv_type = MV_TYPE_16X16;
	                s.mb_skipped=0;

	                s.dsp.clear_blocks(s.block, 0);

	                s.mb_x= mb_x;
	                s.mb_y= mb_y;
	                mv[0][0][0]= 0;
	                mv[0][0][1]= 0;
	                decode_mb(s, 0);
	            }
	        }
	        return;
	    }

	    for(depth=0;; depth++){
	        int changed, pass, none_left;

	        none_left=1;
	        changed=1;
	        for(pass=0; (changed!=0 || pass<2) && pass<10; pass++){
	            //int mb_x, mb_y;
	            int score_sum=0;

	            changed=0;
	            for(mb_y=0; mb_y<s.mb_height; mb_y++){
	                for(mb_x=0; mb_x<s.mb_width; mb_x++){
	                    int mb_xy= mb_x + mb_y*s.mb_stride;
	                    int[][] mv_predictor = new int[8][2];
	                    int[] ref = new int[8];
	                    int pred_count=0;
	                    int j;
	                    int best_score=256*256*256*64;
	                    int best_pred=0;
	                    int mot_index= (mb_x + mb_y*mot_stride) * mot_step;
	                    int prev_x= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index][0];
	                    int prev_y= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index][1];

	                    if(((mb_x^mb_y^pass)&1)!=0) continue;

	                    if(fixed[mb_xy]==MV_FROZEN) continue;
	                    ////assert(0== 7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]));
	                    ////assert(s.last_picture_ptr && s.last_picture_ptr.data[0]);

	                    j=0;
	                    if(mb_x>0           && fixed[mb_xy-1        ]==MV_FROZEN) j=1;
	                    if(mb_x+1<mb_width  && fixed[mb_xy+1        ]==MV_FROZEN) j=1;
	                    if(mb_y>0           && fixed[mb_xy-mb_stride]==MV_FROZEN) j=1;
	                    if(mb_y+1<mb_height && fixed[mb_xy+mb_stride]==MV_FROZEN) j=1;
	                    if(j==0) continue;

	                    j=0;
	                    if(mb_x>0           && fixed[mb_xy-1        ]==MV_CHANGED) j=1;
	                    if(mb_x+1<mb_width  && fixed[mb_xy+1        ]==MV_CHANGED) j=1;
	                    if(mb_y>0           && fixed[mb_xy-mb_stride]==MV_CHANGED) j=1;
	                    if(mb_y+1<mb_height && fixed[mb_xy+mb_stride]==MV_CHANGED) j=1;
	                    if(j==0 && pass>1) continue;

	                    none_left=0;

	                    if(mb_x>0 && fixed[mb_xy-1]!=0){
	                        mv_predictor[pred_count][0]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index - mot_step][0];
	                        mv_predictor[pred_count][1]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index - mot_step][1];
	                        ref         [pred_count]   = s.current_picture.ref_index[0][4*(mb_xy-1)];
	                        pred_count++;
	                    }
	                    if(mb_x+1<mb_width && fixed[mb_xy+1]!=0){
	                        mv_predictor[pred_count][0]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index + mot_step][0];
	                        mv_predictor[pred_count][1]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index + mot_step][1];
	                        ref         [pred_count]   = s.current_picture.ref_index[0][4*(mb_xy+1)];
	                        pred_count++;
	                    }
	                    if(mb_y>0 && fixed[mb_xy-mb_stride]!=0){
	                        mv_predictor[pred_count][0]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index - mot_stride*mot_step][0];
	                        mv_predictor[pred_count][1]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index - mot_stride*mot_step][1];
	                        ref         [pred_count]   = s.current_picture.ref_index[0][4*(mb_xy-s.mb_stride)];
	                        pred_count++;
	                    }
	                    if(mb_y+1<mb_height && fixed[mb_xy+mb_stride]!=0){
	                        mv_predictor[pred_count][0]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index + mot_stride*mot_step][0];
	                        mv_predictor[pred_count][1]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index + mot_stride*mot_step][1];
	                        ref         [pred_count]   = s.current_picture.ref_index[0][4*(mb_xy+s.mb_stride)];
	                        pred_count++;
	                    }
	                    if(pred_count==0) continue;

	                    boolean skip_mean_and_median = false;
	                    
	                    if(pred_count>1){
	                        int sum_x=0, sum_y=0, sum_r=0;
	                        int max_x, max_y, min_x, min_y, max_r, min_r;

	                        for(j=0; j<pred_count; j++){
	                            sum_x+= mv_predictor[j][0];
	                            sum_y+= mv_predictor[j][1];
	                            sum_r+= ref[j];
	                            if(j!=0 && ref[j] != ref[j-1]) {
	                                //goto skip_mean_and_median;
	                            	skip_mean_and_median = true;
	                            	break;
	                            } // if
	                        }

	                        if(!skip_mean_and_median) {

		                        /* mean */
		                        mv_predictor[pred_count][0] = sum_x/j;
		                        mv_predictor[pred_count][1] = sum_y/j;
		                        ref         [pred_count]    = sum_r/j;
	
		                        /* median */
		                        if(pred_count>=3){
		                            min_y= min_x= min_r= 99999;
		                            max_y= max_x= max_r=-99999;
		                        }else{
		                            min_x=min_y=max_x=max_y=min_r=max_r=0;
		                        }
		                        for(j=0; j<pred_count; j++){
		                            max_x= Math.max(max_x, mv_predictor[j][0]);
		                            max_y= Math.max(max_y, mv_predictor[j][1]);
		                            max_r= Math.max(max_r, ref[j]);
		                            min_x= Math.min(min_x, mv_predictor[j][0]);
		                            min_y= Math.min(min_y, mv_predictor[j][1]);
		                            min_r= Math.min(min_r, ref[j]);
		                        }
		                        mv_predictor[pred_count+1][0] = sum_x - max_x - min_x;
		                        mv_predictor[pred_count+1][1] = sum_y - max_y - min_y;
		                        ref         [pred_count+1]    = sum_r - max_r - min_r;
	
		                        if(pred_count==4){
		                            mv_predictor[pred_count+1][0] /= 2;
		                            mv_predictor[pred_count+1][1] /= 2;
		                            ref         [pred_count+1]    /= 2;
		                        }
		                        pred_count+=2;
		                    } // if not skip_mean_and_median
	                    }
	//skip_mean_and_median:
	                    /* zero MV */
	                    pred_count++;

	                    /* last MV */
	                    mv_predictor[pred_count][0]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index][0];
	                    mv_predictor[pred_count][1]= s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index][1];
	                    ref         [pred_count]   = s.current_picture.ref_index[0][4*mb_xy];
	                    pred_count++;

	                    s.mv_dir = MpegEncContext.MV_DIR_FORWARD;
	                    s.mb_intra=0;
	                    s.mv_type = MV_TYPE_16X16;
	                    s.mb_skipped=0;

	                    s.dsp.clear_blocks(s.block, 0);

	                    s.mb_x= mb_x;
	                    s.mb_y= mb_y;

	                    for(j=0; j<pred_count; j++){
	                        int score=0;
	                        int[] src_base= s.current_picture.data_base[0];
	                        int src_offset = s.current_picture.data_offset[0] + mb_x*16 + mb_y*16*s.linesize;

	                        s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index][0]
	                            = mv[0][0][0]= mv_predictor[j][0];
	                        s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index][1]
	                            = mv[0][0][1]= mv_predictor[j][1];

	                        if(ref[j]<0) //predictor intra or otherwise not available
	                            continue;

	                        decode_mb(s, ref[j]);

	                        if(mb_x>0 && fixed[mb_xy-1]!=0){
	                            int k;
	                            for(k=0; k<16; k++)
	                                score += Math.abs(src_base[src_offset + k*s.linesize-1 ]-src_base[src_offset + k*s.linesize   ]);
	                        }
	                        if(mb_x+1<mb_width && fixed[mb_xy+1]!=0){
	                            int k;
	                            for(k=0; k<16; k++)
	                                score += Math.abs(src_base[src_offset + k*s.linesize+15]-src_base[src_offset + k*s.linesize+16]);
	                        }
	                        if(mb_y>0 && fixed[mb_xy-mb_stride]!=0){
	                            int k;
	                            for(k=0; k<16; k++)
	                                score += Math.abs(src_base[src_offset + k-s.linesize   ]-src_base[src_offset + k               ]);
	                        }
	                        if(mb_y+1<mb_height && fixed[mb_xy+mb_stride]!=0){
	                            int k;
	                            for(k=0; k<16; k++)
	                                score += Math.abs(src_base[src_offset + k+s.linesize*15]-src_base[src_offset + k+s.linesize*16]);
	                        }

	                        if(score <= best_score){ // <= will favor the last MV
	                            best_score= score;
	                            best_pred= j;
	                        }
	                    }
	                    score_sum+= best_score;
	                    mv[0][0][0]= mv_predictor[best_pred][0];
	                    mv[0][0][1]= mv_predictor[best_pred][1];

	                    for(i=0; i<mot_step; i++)
	                        for(j=0; j<mot_step; j++){
	                            s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index+i+j*mot_stride][0]
	                                = mv[0][0][0];
	                            s.current_picture.motion_val_base[0][s.current_picture.motion_val_offset[0] + mot_index+i+j*mot_stride][1]
	                                = mv[0][0][1];
	                        }

	                    decode_mb(s, ref[best_pred]);


	                    if(mv[0][0][0] != prev_x || mv[0][0][1] != prev_y){
	                        fixed[mb_xy]=MV_CHANGED;
	                        changed++;
	                    }else
	                        fixed[mb_xy]=MV_UNCHANGED;
	                }
	            }

//	            printf(".%d/%d", changed, score_sum); fflush(stdout);
	        }

	        if(none_left!=0)
	            return;

	        for(i=0; i<s.mb_num; i++){
	            int mb_xy= s.mb_index2xy[i];
	            if(fixed[mb_xy]!=0)
	                fixed[mb_xy]=MV_FROZEN;
	        }
//	        printf(":"); fflush(stdout);
	    }
	}
	
	public static int pix_abs16_c(Object v, 
			int[] pix1_base, int pix1_offset,
			int[] pix2_base, int pix2_offset,
			int line_size, int h)
	{
	    int s, i;

	    s = 0;
	    for(i=0;i<h;i++) {
	        s += Math.abs(pix1_base[pix1_offset + 0] - pix2_base[pix2_offset + 0]);
	        s += Math.abs(pix1_base[pix1_offset + 1] - pix2_base[pix2_offset + 1]);
	        s += Math.abs(pix1_base[pix1_offset + 2] - pix2_base[pix2_offset + 2]);
	        s += Math.abs(pix1_base[pix1_offset + 3] - pix2_base[pix2_offset + 3]);
	        s += Math.abs(pix1_base[pix1_offset + 4] - pix2_base[pix2_offset + 4]);
	        s += Math.abs(pix1_base[pix1_offset + 5] - pix2_base[pix2_offset + 5]);
	        s += Math.abs(pix1_base[pix1_offset + 6] - pix2_base[pix2_offset + 6]);
	        s += Math.abs(pix1_base[pix1_offset + 7] - pix2_base[pix2_offset + 7]);
	        s += Math.abs(pix1_base[pix1_offset + 8] - pix2_base[pix2_offset + 8]);
	        s += Math.abs(pix1_base[pix1_offset + 9] - pix2_base[pix2_offset + 9]);
	        s += Math.abs(pix1_base[pix1_offset + 10] - pix2_base[pix2_offset + 10]);
	        s += Math.abs(pix1_base[pix1_offset + 11] - pix2_base[pix2_offset + 11]);
	        s += Math.abs(pix1_base[pix1_offset + 12] - pix2_base[pix2_offset + 12]);
	        s += Math.abs(pix1_base[pix1_offset + 13] - pix2_base[pix2_offset + 13]);
	        s += Math.abs(pix1_base[pix1_offset + 14] - pix2_base[pix2_offset + 14]);
	        s += Math.abs(pix1_base[pix1_offset + 15] - pix2_base[pix2_offset + 15]);
	        pix1_offset += line_size;
	        pix2_offset += line_size;
	    }
	    return s;
	}
	
	public static int is_intra_more_likely(MpegEncContext s){
	    int is_intra_likely, i, j, undamaged_count, skip_amount, mb_x, mb_y;

	    if(null==s.last_picture_ptr || null==s.last_picture_ptr.data_base[0]) return 1; //no previous frame available . use spatial prediction

	    undamaged_count=0;
	    for(i=0; i<s.mb_num; i++){
	        int mb_xy= s.mb_index2xy[i];
	        int error= s.error_status_table[mb_xy];
	        if(!((error&MpegEncContext.DC_ERROR)!=0 && (error&MpegEncContext.MV_ERROR)!=0))
	            undamaged_count++;
	    }

	    if(s.codec_id == H264PredictionContext.CODEC_ID_H264){
	        H264Context h= s.priv_data;
	        if(h.ref_count[0] <= 0 || null == h.ref_list[0][0].data_base[0])
	            return 1;
	    }

	    if(undamaged_count < 5) return 0; //almost all MBs damaged . use temporal prediction

	    //prevent dsp.sad() check, that requires access to the image
	    /*
	    if(MCONFIG_MPEG_XVMC_DECODER && s.avctx.xvmc_acceleration && s.pict_type == FF_I_TYPE)
	        return 1;
		*/
	    
	    skip_amount= Math.max(undamaged_count/50, 1); //check only upto 50 MBs
	    is_intra_likely=0;

	    j=0;
	    for(mb_y= 0; mb_y<s.mb_height-1; mb_y++){
	        for(mb_x= 0; mb_x<s.mb_width; mb_x++){
	            int error;
	            int mb_xy= mb_x + mb_y*s.mb_stride;

	            error= s.error_status_table[mb_xy];
	            if((error&MpegEncContext.DC_ERROR)!=0 && (error&MpegEncContext.MV_ERROR)!=0)
	                continue; //skip damaged

	            j++;
	            if((j%skip_amount) != 0) continue; //skip a few to speed things up

	            if(s.pict_type==H264Context.FF_I_TYPE){
	                //uint8_t *mb_ptr     = s.current_picture.data[0] + mb_x*16 + mb_y*16*s.linesize;
	                //uint8_t *last_mb_ptr= s.last_picture.data   [0] + mb_x*16 + mb_y*16*s.linesize;
	                int[] mb_ptr_base     = s.current_picture.data_base[0];
	                int[] last_mb_ptr_base= s.last_picture.data_base   [0];
	                int mb_ptr_offset     = s.current_picture.data_offset[0] + mb_x*16 + mb_y*16*s.linesize;
	                int last_mb_ptr_offset= s.last_picture.data_offset   [0] + mb_x*16 + mb_y*16*s.linesize;

	            	is_intra_likely += pix_abs16_c(null, last_mb_ptr_base, last_mb_ptr_offset, mb_ptr_base, mb_ptr_offset, s.linesize, 16);
	                is_intra_likely -= pix_abs16_c(null, last_mb_ptr_base, last_mb_ptr_offset, last_mb_ptr_base, last_mb_ptr_offset +s.linesize*16, s.linesize, 16);
	            }else{
	                if(0!= (7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy])))
	                   is_intra_likely++;
	                else
	                   is_intra_likely--;
	            }
	        }
	    }
	//printf("is_intra_likely: %d type:%d\n", is_intra_likely, s.pict_type);
	    return (is_intra_likely > 0)?1:0;
	}

    public static void ff_er_frame_start(MpegEncContext s){
        if(0==s.error_recognition) return;

        //memset(this.error_status_table, MV_ERROR|AC_ERROR|DC_ERROR|VP_START|AC_END|DC_END|MV_END, this.mb_stride*this.mb_height*sizeof(uint8_t));
        Arrays.fill(s.error_status_table, 0, s.mb_stride*s.mb_height, MpegEncContext.MV_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.DC_ERROR|MpegEncContext.VP_START|MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END);
        s.error_count= 3*s.mb_num;
    }

	/**
	 * adds a slice.
	 * @param endx x component of the last macroblock, can be -1 for the last of the previous line
	 * @param status the status at the end (MV_END, AC_ERROR, ...), it is assumed that no earlier end or
	 *               error of the same type occurred
	 */
    public static void ff_er_add_slice(MpegEncContext s, int startx, int starty, int endx, int endy, int status){
	    int start_i= H264DSPContext.av_clip(startx + starty * s.mb_width    , 0, s.mb_num-1);
	    int end_i  = H264DSPContext.av_clip(endx   + endy   * s.mb_width    , 0, s.mb_num);
	    int start_xy= s.mb_index2xy[start_i];
	    int end_xy  = s.mb_index2xy[end_i];
	    int mask= -1;

	    // DebugTool.printDebugString("....E1.0....\n");

	    //if(s.avctx.hwaccel)
	    //    return;

	    if(start_i > end_i || start_xy > end_xy){
	        //av_log(s.avctx, AV_LOG_ERROR, "internal error, slice end before start\n");
	        return;
	    }

	    if(0==s.error_recognition) return;

	    // DebugTool.printDebugString("....E1.1....\n");

	    mask &= ~MpegEncContext.VP_START;
	    if(0!=(status & (MpegEncContext.AC_ERROR|MpegEncContext.AC_END))){
	        mask &= ~(MpegEncContext.AC_ERROR|MpegEncContext.AC_END);
	        s.error_count -= end_i - start_i + 1;
	    }
	    if(0!=(status & (MpegEncContext.DC_ERROR|MpegEncContext.DC_END))){
	        mask &= ~(MpegEncContext.DC_ERROR|MpegEncContext.DC_END);
	        s.error_count -= end_i - start_i + 1;
	    }
	    if(0!=(status & (MpegEncContext.MV_ERROR|MpegEncContext.MV_END))){
	        mask &= ~(MpegEncContext.MV_ERROR|MpegEncContext.MV_END);
	        s.error_count -= end_i - start_i + 1;
	    }

	    if(0!=(status & (MpegEncContext.AC_ERROR|MpegEncContext.DC_ERROR|MpegEncContext.MV_ERROR))) 
	    	s.error_count= Integer.MAX_VALUE;

	    if(mask == ~0x7F){
	        //memset(&s.error_status_table[start_xy], 0, (end_xy - start_xy) * sizeof(uint8_t));
	    	Arrays.fill(s.error_status_table, start_xy, end_xy, 0);
	    }else{
	        int i;
	        for(i=start_xy; i<end_xy; i++){
	            s.error_status_table[ i ] &= mask;
	        }
	    }

	    if(end_i == s.mb_num)
	        s.error_count= Integer.MAX_VALUE;
	    else{
	        s.error_status_table[end_xy] &= mask;
	        s.error_status_table[end_xy] |= status;
	    }

	    s.error_status_table[start_xy] |= MpegEncContext.VP_START;

	    if(start_xy > 0 && skip_top*s.mb_width < start_i){
	        int prev_status= s.error_status_table[ s.mb_index2xy[start_i - 1] ];

	        prev_status &= ~ MpegEncContext.VP_START;
	        if(prev_status != (MpegEncContext.MV_END|MpegEncContext.DC_END|MpegEncContext.AC_END)) s.error_count= Integer.MAX_VALUE;
	    }
	}
	
	public static void ff_er_frame_end(MpegEncContext s){
	    int i, mb_x, mb_y, error, error_type, dc_error, mv_error, ac_error;
	    int distance;
	    int[] threshold_part = {100,100,100};
	    int threshold= 50;
	    int is_intra_likely;
	    int size = s.b8_stride * 2 * s.mb_height;
	    AVFrame pic= s.current_picture_ptr;

	    // DebugTool.printDebugString("....F1.0....\n");

	    if(0==s.error_recognition || s.error_count==0 || 0!=s.lowres ||
	       s.picture_structure != MpegEncContext.PICT_FRAME || // we dont support ER of field pictures yet, though it should not crash if enabled
	       s.error_count==3*s.mb_width*(skip_top + skip_bottom)) return;

	    // DebugTool.printDebugString("....F1.1....\n");

	    if(s.current_picture.motion_val_base[0] == null){
	        //av_log(s.avctx, AV_LOG_ERROR, "Warning MVs not available\n");

	        for(i=0; i<2; i++){
	            //pic.ref_index[i]= av_mallocz(s.mb_stride * s.mb_height * 4 * sizeof(uint8_t));
	            pic.ref_index[i]= new int[s.mb_stride * s.mb_height * 4];
	            pic.motion_val_base[i]= new int[(size+4)][2];
	            pic.motion_val_offset[i]= 4;
	        }
	        pic.motion_subsample_log2= 3;
	        s.current_picture_ptr.copyTo(s.current_picture);
	    }

	    // DebugTool.printDebugString("....F1.2....\n");
	    
	    /*
	    if(s.debug&FF_DEBUG_ER){
	        for(mb_y=0; mb_y<s.mb_height; mb_y++){
	            for(mb_x=0; mb_x<s.mb_width; mb_x++){
	                int status= s.error_status_table[mb_x + mb_y*s.mb_stride];

	                av_log(s.avctx, AV_LOG_DEBUG, "%2X ", status);
	            }
	            av_log(s.avctx, AV_LOG_DEBUG, "\n");
	        }
	    }
	    */

	    // DebugTool.printDebugString("....F1.3....\n");

	    // handle overlapping slices 
	    for(error_type=1; error_type<=3; error_type++){
	        int end_ok=0;

	        for(i=s.mb_num-1; i>=0; i--){
	            int mb_xy= s.mb_index2xy[i];
	            error= s.error_status_table[mb_xy];

	            if(0!=(error&(1<<error_type)))
	                end_ok=1;
	            if(0!=(error&(8<<error_type)))
	                end_ok=1;

	            if(0==end_ok)
	                s.error_status_table[mb_xy]|= 1<<error_type;

	            if(0!=(error&MpegEncContext.VP_START))
	                end_ok=0;
	        }
	    }
	    
	    // DebugTool.printDebugString("....F1.4....\n");

	    // handle slices with partitions of different length 
	    if(0!=s.partitioned_frame){
	        int end_ok=0;

	        for(i=s.mb_num-1; i>=0; i--){
	            int mb_xy= s.mb_index2xy[i];
	            error= s.error_status_table[mb_xy];

	            if(0!=(error&MpegEncContext.AC_END))
	                end_ok=0;
	            if(0!=(error&MpegEncContext.MV_END) || 0!=(error&MpegEncContext.DC_END) || 0!=(error&MpegEncContext.AC_ERROR))
	                end_ok=1;

	            if(0==end_ok)
	                s.error_status_table[mb_xy]|= MpegEncContext.AC_ERROR;

	            if(0!=(error&MpegEncContext.VP_START))
	                end_ok=0;
	        }
	    }

	    // DebugTool.printDebugString("....F1.4....\n");
	    
	    // handle missing slices 
	    if(s.error_recognition>=4){
	        int end_ok=1;

	        for(i=s.mb_num-2; i>=s.mb_width+100; i--){ //FIXME +100 hack
	            int mb_xy= s.mb_index2xy[i];
	            int error1= s.error_status_table[mb_xy  ];
	            int error2= s.error_status_table[s.mb_index2xy[i+1]];

	            if(0!=(error1&MpegEncContext.VP_START))
	                end_ok=1;

	            if(   error2==(MpegEncContext.VP_START|MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR|MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END)
	               && error1!=(MpegEncContext.VP_START|MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR|MpegEncContext.AC_END|MpegEncContext.DC_END|MpegEncContext.MV_END)
	               && (0!=(error1&MpegEncContext.AC_END) || 0!=(error1&MpegEncContext.DC_END) || 0!=(error1&MpegEncContext.MV_END) ) ){ //end & uninit
	                end_ok=0;
	            }

	            if(0==end_ok)
	                s.error_status_table[mb_xy]|= MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR;
	        }
	    }

	    // DebugTool.printDebugString("....F1.5....\n");
	    
	    // backward mark errors 
	    distance=9999999;
	    for(error_type=1; error_type<=3; error_type++){
	        for(i=s.mb_num-1; i>=0; i--){
	            int mb_xy= s.mb_index2xy[i];
	            error= s.error_status_table[mb_xy];

	            if(0==s.mbskip_table[mb_xy]) //FIXME partition specific
	                distance++;
	            if(0!=(error&(1<<error_type)))
	                distance= 0;

	            if(0!=s.partitioned_frame){
	                if(distance < threshold_part[error_type-1])
	                    s.error_status_table[mb_xy]|= 1<<error_type;
	            }else{
	                if(distance < threshold)
	                    s.error_status_table[mb_xy]|= 1<<error_type;
	            }

	            if(0!=(error&MpegEncContext.VP_START))
	                distance= 9999999;
	        }
	    }

	    // DebugTool.printDebugString("....F1.6....\n");
	    
	    // forward mark errors 
	    error=0;
	    for(i=0; i<s.mb_num; i++){
	        int mb_xy= s.mb_index2xy[i];
	        int old_error= s.error_status_table[mb_xy];

	        if(0!=(old_error&MpegEncContext.VP_START))
	            error= old_error& (MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR);
	        else{
	            error|= old_error& (MpegEncContext.DC_ERROR|MpegEncContext.AC_ERROR|MpegEncContext.MV_ERROR);
	            s.error_status_table[mb_xy]|= error;
	        }
	    }

	    // DebugTool.printDebugString("....F1.7....\n");

	    // handle not partitioned case 
	    if(0==s.partitioned_frame){
	        for(i=0; i<s.mb_num; i++){
	            int mb_xy= s.mb_index2xy[i];
	            error= s.error_status_table[mb_xy];
	            if(0!=(error&(MpegEncContext.AC_ERROR|MpegEncContext.DC_ERROR|MpegEncContext.MV_ERROR)))
	                error|= MpegEncContext.AC_ERROR|MpegEncContext.DC_ERROR|MpegEncContext.MV_ERROR;
	            s.error_status_table[mb_xy]= error;
	        }
	    }

	    // DebugTool.printDebugString("....F1.7....\n");

	    dc_error= ac_error= mv_error=0;
	    for(i=0; i<s.mb_num; i++){
	        int mb_xy= s.mb_index2xy[i];
	        error= s.error_status_table[mb_xy];
	        if(0!=(error&MpegEncContext.DC_ERROR)) dc_error ++;
	        if(0!=(error&MpegEncContext.AC_ERROR)) ac_error ++;
	        if(0!=(error&MpegEncContext.MV_ERROR)) mv_error ++;
	    }
	    //av_log(s.avctx, AV_LOG_INFO, "concealing %d DC, %d AC, %d MV errors\n", dc_error, ac_error, mv_error);

	    is_intra_likely= is_intra_more_likely(s);

	    // DebugTool.printDebugString("....F1.8....\n");

	    // set unknown mb-type to most likely 
	    for(i=0; i<s.mb_num; i++){
	        int mb_xy= s.mb_index2xy[i];
	        error= s.error_status_table[mb_xy];
	        if(!(0!=(error&MpegEncContext.DC_ERROR) && 0!=(error&MpegEncContext.MV_ERROR)))
	            continue;

	        if(0!=is_intra_likely)
	            s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]= H264Context.MB_TYPE_INTRA4x4;
	        else
	            s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]= H264Context.MB_TYPE_16x16 | H264Context.MB_TYPE_L0;
	    }

	    // DebugTool.printDebugString("....F1.9....\n");

	    // change inter to intra blocks if no reference frames are available
	    if (null==s.last_picture.data_base[0] && null==s.next_picture.data_base[0])
	        for(i=0; i<s.mb_num; i++){
	            int mb_xy= s.mb_index2xy[i];
	            if(0==(7 & (int)(s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy])))
	                s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]= H264Context.MB_TYPE_INTRA4x4;
	        }

	    // DebugTool.printDebugString("....F1.10....\n");

	    // handle inter blocks with damaged AC 
	    for(mb_y=0; mb_y<s.mb_height; mb_y++){
	        for(mb_x=0; mb_x<s.mb_width; mb_x++){
	            int mb_xy= mb_x + mb_y * s.mb_stride;
	            int mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
	            int dir = (s.last_picture.data_base[0]==null?1:0);
	            error= s.error_status_table[mb_xy];

	            if(0!=(7 & (int)(mb_type))) continue; //intra
	            if(0!=(error&MpegEncContext.MV_ERROR)) continue;              //inter with damaged MV
	            if(0==(error&MpegEncContext.AC_ERROR)) continue;           //undamaged inter

	            s.mv_dir = (dir!=0) ? MpegEncContext.MV_DIR_BACKWARD : MpegEncContext.MV_DIR_FORWARD;
	            s.mb_intra=0;
	            s.mb_skipped=0;
	            if(0!=(mb_type & H264Context.MB_TYPE_8x8)){
	                int mb_index= mb_x*2 + mb_y*2*s.b8_stride;
	                int j;
	                s.mv_type = MV_TYPE_8X8;
	                for(j=0; j<4; j++){
	                    mv[0][j][0] = s.current_picture.motion_val_base[dir][s.current_picture.motion_val_offset[dir] + mb_index + (j&1) + (j>>1)*s.b8_stride ][0];
	                    mv[0][j][1] = s.current_picture.motion_val_base[dir][s.current_picture.motion_val_offset[dir] + mb_index + (j&1) + (j>>1)*s.b8_stride ][1];
	                }
	            }else{
	                s.mv_type = MV_TYPE_16X16;
	                mv[0][0][0] = s.current_picture.motion_val_base[dir][s.current_picture.motion_val_offset[dir] + mb_x*2 + mb_y*2*s.b8_stride ][0];
	                mv[0][0][1] = s.current_picture.motion_val_base[dir][s.current_picture.motion_val_offset[dir] + mb_x*2 + mb_y*2*s.b8_stride ][1];
	            }

	            s.dsp.clear_blocks(s.block, 0);

	            s.mb_x= mb_x;
	            s.mb_y= mb_y;
	            decode_mb(s, 0);
	        }
	    }

	    // DebugTool.printDebugString("....F1.11....\n");

	    // guess MVs 
	    if(s.pict_type==H264Context.FF_B_TYPE){
	        for(mb_y=0; mb_y<s.mb_height; mb_y++){
	            for(mb_x=0; mb_x<s.mb_width; mb_x++){
	                int xy= mb_x*2 + mb_y*2*s.b8_stride;
	                int mb_xy= mb_x + mb_y * s.mb_stride;
	                int mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];
	                error= s.error_status_table[mb_xy];

	                if(0 != ( 7 & (mb_type) )) continue;
	                if(0==(error&MpegEncContext.MV_ERROR)) continue;           //inter with undamaged MV
	                if(0==(error&MpegEncContext.AC_ERROR)) continue;           //undamaged inter

	                s.mv_dir = MpegEncContext.MV_DIR_FORWARD|MpegEncContext.MV_DIR_BACKWARD;
	                if(null == s.last_picture.data_base[0]) s.mv_dir &= ~MpegEncContext.MV_DIR_FORWARD;
	                if(null == s.next_picture.data_base[0]) s.mv_dir &= ~MpegEncContext.MV_DIR_BACKWARD;
	                s.mb_intra=0;
	                s.mv_type = MV_TYPE_16X16;
	                s.mb_skipped=0;

	                if(s.pp_time!=0){
	                    int time_pp= s.pp_time;
	                    int time_pb= s.pb_time;

	                    mv[0][0][0] = s.next_picture.motion_val_base[0][s.next_picture.motion_val_offset[0] + xy][0]*time_pb/time_pp;
	                    mv[0][0][1] = s.next_picture.motion_val_base[0][s.next_picture.motion_val_offset[0] + xy][1]*time_pb/time_pp;
	                    mv[1][0][0] = s.next_picture.motion_val_base[0][s.next_picture.motion_val_offset[0] + xy][0]*(time_pb - time_pp)/time_pp;
	                    mv[1][0][1] = s.next_picture.motion_val_base[0][s.next_picture.motion_val_offset[0] + xy][1]*(time_pb - time_pp)/time_pp;
	                }else{
	                    mv[0][0][0]= 0;
	                    mv[0][0][1]= 0;
	                    mv[1][0][0]= 0;
	                    mv[1][0][1]= 0;
	                }

	                s.dsp.clear_blocks(s.block, 0);
	                s.mb_x= mb_x;
	                s.mb_y= mb_y;
	                decode_mb(s, 0);
	            }
	        }
	    }else
	        guess_mv(s);

	    // the filters below are not XvMC compatible, skip them 
	    //if(CONFIG_MPEG_XVMC_DECODER && s.avctx.xvmc_acceleration)
	    //    goto ec_clean;
	    // DebugTool.printDebugString("****Before ENter F1.12\n");
	    // DebugTool.dumpFrameData(s.current_picture_ptr);
	    
	    // DebugTool.printDebugString("....F1.12....\n");

	    // fill DC for inter blocks 
	    for(mb_y=0; mb_y<s.mb_height; mb_y++){
	        for(mb_x=0; mb_x<s.mb_width; mb_x++){
	            int dc, dcu, dcv, y, n;
	            //int16_t *dc_ptr;
	            int[] dc_ptr_base;
	            int dc_ptr_offset;
	            //uint8_t *dest_y, *dest_cb, *dest_cr;
	            int[] dest_y_base,dest_cb_base,dest_cr_base;
	            int dest_y_offset,dest_cb_offset,dest_cr_offset;
	            int mb_xy= mb_x + mb_y * s.mb_stride;
	            int mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];

	            error= s.error_status_table[mb_xy];

	            if(0!=(7&(int)(mb_type)) && 0!=s.partitioned_frame) continue;
//	            if(error&MV_ERROR) continue; //inter data damaged FIXME is this good?

	            dest_y_base = s.current_picture.data_base[0];
	            dest_y_offset = s.current_picture.data_offset[0] + mb_x*16 + mb_y*16*s.linesize;
	            dest_cb_base= s.current_picture.data_base[1];
	            dest_cb_offset = s.current_picture.data_offset[1] + mb_x*8  + mb_y*8 *s.uvlinesize;
	            dest_cr_base= s.current_picture.data_base[2];
	            dest_cr_offset = s.current_picture.data_offset[2] + mb_x*8  + mb_y*8 *s.uvlinesize;

	            dc_ptr_base = dc_val_base;
	            dc_ptr_offset = dc_val[0] + mb_x*2 + mb_y*2*s.b8_stride;
	            
	            for(n=0; n<4; n++){
	                dc=0;
	                for(y=0; y<8; y++){
	                    int x;
	                    for(x=0; x<8; x++){
	                       dc+= dest_y_base[dest_y_offset + x + (n&1)*8 + (y + (n>>1)*8)*s.linesize];
	                    }
	                }
	                // DebugTool.printDebugString("dc_ptr_base["+((n&1) + (n>>1)*s.b8_stride)+"]="+((dc+4)>>3)+"\n");
	                dc_ptr_base[dc_ptr_offset + (n&1) + (n>>1)*s.b8_stride]= (dc+4)>>3;
	            }

	            dcu=dcv=0;
	            for(y=0; y<8; y++){
	                int x;
	                for(x=0; x<8; x++){
	                    dcu+=dest_cb_base[dest_cb_offset + x + y*(s.uvlinesize)];
	                    dcv+=dest_cr_base[dest_cr_offset + x + y*(s.uvlinesize)];
	                }
	            }
	            dc_val_base[dc_val[1] + mb_x + mb_y*s.mb_stride]= (dcu+4)>>3;
	            dc_val_base[dc_val[2] + mb_x + mb_y*s.mb_stride]= (dcv+4)>>3;
	        }
	    }

	    // DebugTool.printDebugString("****Before ENter F1.13\n");
	    // DebugTool.dumpFrameData(s.current_picture_ptr);
	    // DebugTool.printDebugString("....F1.13....\n");

	    // guess DC for damaged blocks 
	    guess_dc(s, dc_val_base, dc_val[0], s.mb_width*2, s.mb_height*2, s.b8_stride, 1);
	    guess_dc(s, dc_val_base, dc_val[1], s.mb_width  , s.mb_height  , s.mb_stride, 0);
	    guess_dc(s, dc_val_base, dc_val[2], s.mb_width  , s.mb_height  , s.mb_stride, 0);

	    // DebugTool.printDebugString("****Before ENter F1.14\n");
	    // DebugTool.dumpFrameData(s.current_picture_ptr);
	    
	    // DebugTool.printDebugString("....F1.14....\n");
	    // filter luma DC 
	    filter181(dc_val_base, dc_val[0], s.mb_width*2, s.mb_height*2, s.b8_stride);

	    // DebugTool.printDebugString("****Before ENter F1.15\n");
	    // DebugTool.dumpFrameData(s.current_picture_ptr);
	    
	    // DebugTool.printDebugString("....F1.15....\n");
	    // render DC only intra 
	    for(mb_y=0; mb_y<s.mb_height; mb_y++){
	        for(mb_x=0; mb_x<s.mb_width; mb_x++){
	            //uint8_t *dest_y, *dest_cb, *dest_cr;
	            int[] dest_y_base,dest_cb_base,dest_cr_base;
	            int dest_y_offset,dest_cb_offset,dest_cr_offset;

	        	int mb_xy= mb_x + mb_y * s.mb_stride;
	            int mb_type= (int)s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy];

	            error= s.error_status_table[mb_xy];

	            if(0!=((mb_type) &  (H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_16x8|H264Context.MB_TYPE_8x16|H264Context.MB_TYPE_8x8) )) continue;
	            if(0==(error&MpegEncContext.AC_ERROR)) continue;              //undamaged

	            dest_y_base = s.current_picture.data_base[0];
	            dest_y_offset = s.current_picture.data_offset[0] + mb_x*16 + mb_y*16*s.linesize;
	            dest_cb_base= s.current_picture.data_base[1];
	            dest_cb_offset = s.current_picture.data_offset[1] + mb_x*8  + mb_y*8 *s.uvlinesize;
	            dest_cr_base= s.current_picture.data_base[2];
	            dest_cr_offset = s.current_picture.data_offset[2] + mb_x*8  + mb_y*8 *s.uvlinesize;
	            
	            // DebugTool.printDebugString("Running put_dc for error type:"+error+" of mblock ("+mb_x+", "+mb_y+")\n");
	            put_dc(s, dest_y_base, dest_y_offset, dest_cb_base, dest_cb_offset, dest_cr_base, dest_cr_offset, mb_x, mb_y);
	        }
	    }
	    
	    // DebugTool.printDebugString("****Before ENter F1.16\n");
	    // DebugTool.dumpFrameData(s.current_picture_ptr);
	    
	    // DebugTool.printDebugString("....F1.16....\n");

	    if(0!=(error_concealment&FF_EC_DEBLOCK)){
	        // filter horizontal block boundaries 
	        h_block_filter(s, s.current_picture.data_base[0], s.current_picture.data_offset[0], s.mb_width*2, s.mb_height*2, s.linesize  , 1);
	        h_block_filter(s, s.current_picture.data_base[1], s.current_picture.data_offset[1], s.mb_width  , s.mb_height  , s.uvlinesize, 0);
	        h_block_filter(s, s.current_picture.data_base[2], s.current_picture.data_offset[2], s.mb_width  , s.mb_height  , s.uvlinesize, 0);

	        // filter vertical block boundaries 
	        v_block_filter(s, s.current_picture.data_base[0], s.current_picture.data_offset[0], s.mb_width*2, s.mb_height*2, s.linesize  , 1);
	        v_block_filter(s, s.current_picture.data_base[1], s.current_picture.data_offset[1], s.mb_width  , s.mb_height  , s.uvlinesize, 0);
	        v_block_filter(s, s.current_picture.data_base[2], s.current_picture.data_offset[2], s.mb_width  , s.mb_height  , s.uvlinesize, 0);
	    }

	    // DebugTool.printDebugString("****Before ENter F1.17\n");
	    // DebugTool.dumpFrameData(s.current_picture_ptr);
	    
	    // DebugTool.printDebugString("....F1.17....\n");

	//ec_clean:
	    // clean a few tables 
	    for(i=0; i<s.mb_num; i++){
	        int mb_xy= s.mb_index2xy[i];
	        error= s.error_status_table[mb_xy];

	        if(s.pict_type!=H264Context.FF_B_TYPE && 0!=(error&(MpegEncContext.DC_ERROR|MpegEncContext.MV_ERROR|MpegEncContext.AC_ERROR))){
	            s.mbskip_table[mb_xy]=0;
	        }
	        s.mbintra_table[mb_xy]=1;
	    }
	}
	
}
