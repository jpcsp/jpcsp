package com.twilight.h264.decoder;

import com.twilight.h264.util.*;;

public class CAVLCContext {

	/*
	 * H.26L/H.264/AVC/JVT/14496-10/... cavlc bitstream decoding
	 * Copyright (c) 2003 Michael Niedermayer <michaelni@gmx.at>
	 *
	 * This file is part of FFmpeg.
	 *
	 * FFmpeg is free software; you can redistribute it and/or
	 * modify it under the terms of the GNU Lesser General Public
	 * License as published by the Free Software Foundation; either
	 * version 2.1 of the License, or (at your option) any later version.
	 *
	 * FFmpeg is distributed in the hope that it will be useful,
	 * but WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	 * Lesser General Public License for more details.
	 *
	 * You should have received a copy of the GNU Lesser General Public
	 * License along with FFmpeg; if not, write to the Free Software
	 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
	 */

	/**
	 * @file
	 * H.264 / AVC / MPEG4 part10 cavlc bitstream decoding.
	 * @author Michael Niedermayer <michaelni@gmx.at>
	 */

	/*
	#define CABAC 0

	#include "internal.h"
	#include "avcodec.h"
	#include "mpegvideo.h"
	#include "h264.h"
	#include "h264data.h" // FIXME FIXME FIXME
	#include "h264_mvpred.h"
	#include "golomb.h"

	//#undef NDEBUG
	#include <assert.h>
	*/
	//static const uint8_t golomb_to_inter_cbp_gray[16]= {
	public static final int[] golomb_to_inter_cbp_gray = {
	 0, 1, 2, 4, 8, 3, 5,10,12,15, 7,11,13,14, 6, 9,
	};

	//static const uint8_t golomb_to_intra4x4_cbp_gray[16]={
	public static final int[] golomb_to_intra4x4_cbp_gray={
		15, 0, 7,11,13,14, 3, 5,10,12, 1, 2, 4, 8, 6, 9,
	};

	//static const uint8_t chroma_dc_coeff_token_len[4*5]={
	public static final int[] chroma_dc_coeff_token_len={
	 2, 0, 0, 0,
	 6, 1, 0, 0,
	 6, 6, 3, 0,
	 6, 7, 7, 6,
	 6, 8, 8, 7,
	};

	public static final int[] chroma_dc_coeff_token_bits={
//		static const uint8_t chroma_dc_coeff_token_bits[4*5]={
	 1, 0, 0, 0,
	 7, 1, 0, 0,
	 4, 6, 1, 0,
	 3, 3, 2, 5,
	 2, 3, 2, 0,
	};

	public static final int[][] coeff_token_len={
//		static const uint8_t coeff_token_len[4][4*17]={
	{
	     1, 0, 0, 0,
	     6, 2, 0, 0,     8, 6, 3, 0,     9, 8, 7, 5,    10, 9, 8, 6,
	    11,10, 9, 7,    13,11,10, 8,    13,13,11, 9,    13,13,13,10,
	    14,14,13,11,    14,14,14,13,    15,15,14,14,    15,15,15,14,
	    16,15,15,15,    16,16,16,15,    16,16,16,16,    16,16,16,16,
	},
	{
	     2, 0, 0, 0,
	     6, 2, 0, 0,     6, 5, 3, 0,     7, 6, 6, 4,     8, 6, 6, 4,
	     8, 7, 7, 5,     9, 8, 8, 6,    11, 9, 9, 6,    11,11,11, 7,
	    12,11,11, 9,    12,12,12,11,    12,12,12,11,    13,13,13,12,
	    13,13,13,13,    13,14,13,13,    14,14,14,13,    14,14,14,14,
	},
	{
	     4, 0, 0, 0,
	     6, 4, 0, 0,     6, 5, 4, 0,     6, 5, 5, 4,     7, 5, 5, 4,
	     7, 5, 5, 4,     7, 6, 6, 4,     7, 6, 6, 4,     8, 7, 7, 5,
	     8, 8, 7, 6,     9, 8, 8, 7,     9, 9, 8, 8,     9, 9, 9, 8,
	    10, 9, 9, 9,    10,10,10,10,    10,10,10,10,    10,10,10,10,
	},
	{
	     6, 0, 0, 0,
	     6, 6, 0, 0,     6, 6, 6, 0,     6, 6, 6, 6,     6, 6, 6, 6,
	     6, 6, 6, 6,     6, 6, 6, 6,     6, 6, 6, 6,     6, 6, 6, 6,
	     6, 6, 6, 6,     6, 6, 6, 6,     6, 6, 6, 6,     6, 6, 6, 6,
	     6, 6, 6, 6,     6, 6, 6, 6,     6, 6, 6, 6,     6, 6, 6, 6,
	}
	};

	public static final int[][] coeff_token_bits={
//		static const uint8_t coeff_token_bits[4][4*17]={
	{
	     1, 0, 0, 0,
	     5, 1, 0, 0,     7, 4, 1, 0,     7, 6, 5, 3,     7, 6, 5, 3,
	     7, 6, 5, 4,    15, 6, 5, 4,    11,14, 5, 4,     8,10,13, 4,
	    15,14, 9, 4,    11,10,13,12,    15,14, 9,12,    11,10,13, 8,
	    15, 1, 9,12,    11,14,13, 8,     7,10, 9,12,     4, 6, 5, 8,
	},
	{
	     3, 0, 0, 0,
	    11, 2, 0, 0,     7, 7, 3, 0,     7,10, 9, 5,     7, 6, 5, 4,
	     4, 6, 5, 6,     7, 6, 5, 8,    15, 6, 5, 4,    11,14,13, 4,
	    15,10, 9, 4,    11,14,13,12,     8,10, 9, 8,    15,14,13,12,
	    11,10, 9,12,     7,11, 6, 8,     9, 8,10, 1,     7, 6, 5, 4,
	},
	{
	    15, 0, 0, 0,
	    15,14, 0, 0,    11,15,13, 0,     8,12,14,12,    15,10,11,11,
	    11, 8, 9,10,     9,14,13, 9,     8,10, 9, 8,    15,14,13,13,
	    11,14,10,12,    15,10,13,12,    11,14, 9,12,     8,10,13, 8,
	    13, 7, 9,12,     9,12,11,10,     5, 8, 7, 6,     1, 4, 3, 2,
	},
	{
	     3, 0, 0, 0,
	     0, 1, 0, 0,     4, 5, 6, 0,     8, 9,10,11,    12,13,14,15,
	    16,17,18,19,    20,21,22,23,    24,25,26,27,    28,29,30,31,
	    32,33,34,35,    36,37,38,39,    40,41,42,43,    44,45,46,47,
	    48,49,50,51,    52,53,54,55,    56,57,58,59,    60,61,62,63,
	}
	};

	public static final int[][] total_zeros_len= {
//		static const uint8_t total_zeros_len[16][16]= {
	    {1,3,3,4,4,5,5,6,6,7,7,8,8,9,9,9},
	    {3,3,3,3,3,4,4,4,4,5,5,6,6,6,6,0},
	    {4,3,3,3,4,4,3,3,4,5,5,6,5,6,0,0},
	    {5,3,4,4,3,3,3,4,3,4,5,5,5,0,0,0},
	    {4,4,4,3,3,3,3,3,4,5,4,5,0,0,0,0},
	    {6,5,3,3,3,3,3,3,4,3,6,0,0,0,0,0},
	    {6,5,3,3,3,2,3,4,3,6,0,0,0,0,0,0},
	    {6,4,5,3,2,2,3,3,6,0,0,0,0,0,0,0},
	    {6,6,4,2,2,3,2,5,0,0,0,0,0,0,0,0},
	    {5,5,3,2,2,2,4,0,0,0,0,0,0,0,0,0},
	    {4,4,3,3,1,3,0,0,0,0,0,0,0,0,0,0},
	    {4,4,2,1,3,0,0,0,0,0,0,0,0,0,0,0},
	    {3,3,1,2,0,0,0,0,0,0,0,0,0,0,0,0},
	    {2,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
	    {1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
	};

	public static final int[][] total_zeros_bits= {
//		static const uint8_t total_zeros_bits[16][16]= {
	    {1,3,2,3,2,3,2,3,2,3,2,3,2,3,2,1},
	    {7,6,5,4,3,5,4,3,2,3,2,3,2,1,0,0},
	    {5,7,6,5,4,3,4,3,2,3,2,1,1,0,0,0},
	    {3,7,5,4,6,5,4,3,3,2,2,1,0,0,0,0},
	    {5,4,3,7,6,5,4,3,2,1,1,0,0,0,0,0},
	    {1,1,7,6,5,4,3,2,1,1,0,0,0,0,0,0},
	    {1,1,5,4,3,3,2,1,1,0,0,0,0,0,0,0},
	    {1,1,1,3,3,2,2,1,0,0,0,0,0,0,0,0},
	    {1,0,1,3,2,1,1,1,0,0,0,0,0,0,0,0},
	    {1,0,1,3,2,1,1,0,0,0,0,0,0,0,0,0},
	    {0,1,1,2,1,3,0,0,0,0,0,0,0,0,0,0},
	    {0,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0},
	    {0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0},
	    {0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
	    {0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
	};

	public static final int[][] chroma_dc_total_zeros_len= {
//		static uint8_t chroma_dc_total_zeros_len[3][4]= {
	    { 1, 2, 3, 3,},
	    { 1, 2, 2, 0,},
	    { 1, 1, 0, 0,},
	};

	public static final int[][] chroma_dc_total_zeros_bits= {
//		static uint8_t chroma_dc_total_zeros_bits[3][4]= {
	    { 1, 1, 1, 0,},
	    { 1, 1, 0, 0,},
	    { 1, 0, 0, 0,},
	};

	public static final int[][] run_len={
//		static uint8_t run_len[7][16]={
	    {1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
	    {1,2,2,0,0,0,0,0,0,0,0,0,0,0,0,0},
	    {2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,0},
	    {2,2,2,3,3,0,0,0,0,0,0,0,0,0,0,0},
	    {2,2,3,3,3,3,0,0,0,0,0,0,0,0,0,0},
	    {2,3,3,3,3,3,3,0,0,0,0,0,0,0,0,0},
	    {3,3,3,3,3,3,3,4,5,6,7,8,9,10,11,0},
	};

	public static final int[][] run_bits={
//		static uint8_t run_bits[7][16]={
	    {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
	    {1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
	    {3,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
	    {3,2,1,1,0,0,0,0,0,0,0,0,0,0,0,0},
	    {3,2,3,2,1,0,0,0,0,0,0,0,0,0,0,0},
	    {3,0,1,3,2,5,4,0,0,0,0,0,0,0,0,0},
	    {7,6,5,4,3,2,1,1,1,1,1,1,1,1,1,0},
	};

	//#define VLC_TYPE int16_t
	
	public static VLC[] coeff_token_vlc = new VLC[4];
	//public static short[][] coeff_token_vlc_tables = new short[520+332+280+256][2];
	public static final int[] coeff_token_vlc_tables_size={520,332,280,256};

	public static VLC chroma_dc_coeff_token_vlc = new VLC();
	//public static short[][] chroma_dc_coeff_token_vlc_table = new short[256][2];
	public static final int chroma_dc_coeff_token_vlc_table_size = 256;

	public static VLC[] total_zeros_vlc = new VLC[15];
	//public static short[][][] total_zeros_vlc_tables = new short[15][512][2];
	public static final int total_zeros_vlc_tables_size = 512;

	public static VLC[] chroma_dc_total_zeros_vlc = new VLC[3];
	//public static short[][][] chroma_dc_total_zeros_vlc_tables = new short[3][8][2];
	public static final int chroma_dc_total_zeros_vlc_tables_size = 8;

	public static VLC[] run_vlc = new VLC[6];
	//public static short[][][] run_vlc_tables = new short[6][8][2];
	public static final int run_vlc_tables_size = 8;

	public static VLC run7_vlc = new VLC();
	//public static short[][] run7_vlc_table = new short[96][2];
	public static final int run7_vlc_table_size = 96;

	public static final int LEVEL_TAB_BITS = 8;
	//static int8_t cavlc_level_tab[7][1<<LEVEL_TAB_BITS][2];
	public static int[][][] cavlc_level_tab = new int[7][1<<LEVEL_TAB_BITS][2];

	public static final int[/*256*/] ff_log2_tab = {
	        0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
	        5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
	        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
	        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
	        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
	};
	
	public static /*inline av_const*/ int av_log2(long v)
	{
	    int n = 0;
	    if ((v & 0xffff0000L)!=0) {
	        v >>= 16;
	        n += 16;
	    }
	    if ((v & 0xff00L)!=0) {
	        v >>= 8;
	        n += 8;
	    }
	    n += ff_log2_tab[(int)v];
	    return n;
	}
	

	/**
	 * gets the predicted number of non-zero coefficients.
	 * @param n block index
	 */
	public static /*inline*/ int pred_non_zero_count(H264Context  h, int n){
	    int index8= H264Context.scan8[n];
	    int left= h.non_zero_count_cache[index8 - 1];
	    int top = h.non_zero_count_cache[index8 - 8];
	    int i= left + top;

	    if(i<64) i= (i+1)>>1;

	    //tprintf(h.s.avctx, "pred_nnz L%X T%X n%d s%d P%X\n", left, top, n, scan8[n], i&31);

	    return i&31;
	}

	public static void init_cavlc_level_tab(){
	    int suffix_length, mask;
	    int i;

	    for(suffix_length=0; suffix_length<7; suffix_length++){
	        for(i=0; i<(1<<LEVEL_TAB_BITS); i++){
	            int prefix= LEVEL_TAB_BITS - av_log2(2*i);
	            int level_code= (prefix<<suffix_length) + (i>>(LEVEL_TAB_BITS-prefix-1-suffix_length)) - (1<<suffix_length);

	            mask= -(level_code&1);
	            level_code= (((2+level_code)>>1) ^ mask) - mask;
	            if(prefix + 1 + suffix_length <= LEVEL_TAB_BITS){
	                cavlc_level_tab[suffix_length][i][0]= level_code;
	                cavlc_level_tab[suffix_length][i][1]= prefix + 1 + suffix_length;
	            }else if(prefix + 1 <= LEVEL_TAB_BITS){
	                cavlc_level_tab[suffix_length][i][0]= prefix+100;
	                cavlc_level_tab[suffix_length][i][1]= prefix + 1;
	            }else{
	                cavlc_level_tab[suffix_length][i][0]= LEVEL_TAB_BITS+100;
	                cavlc_level_tab[suffix_length][i][1]= LEVEL_TAB_BITS;
	            }
	        }
	    }
	}

	public static int calvc_inited = 0;
	public void ff_h264_decode_init_vlc(){

	    if (0==calvc_inited) {
	        int i;
	        int offset;
	        calvc_inited = 1;

	        //chroma_dc_coeff_token_vlc.table_base = chroma_dc_coeff_token_vlc_table;
	        chroma_dc_coeff_token_vlc.table_base = CAVLCTables.expandTable(CAVLCTables.chroma_dc_coeff_token_vlc_table);
	        chroma_dc_coeff_token_vlc.table_offset = 0;
	        chroma_dc_coeff_token_vlc.table_allocated = chroma_dc_coeff_token_vlc_table_size;
	        /*
	        GetBitContext.init_vlc(chroma_dc_coeff_token_vlc, H264Context.CHROMA_DC_COEFF_TOKEN_VLC_BITS, 4*5,
	                 chroma_dc_coeff_token_len,0, 1, 1,
	                 chroma_dc_coeff_token_bits,0, 1, 1,
	                 GetBitContext.INIT_VLC_USE_NEW_STATIC);
			*/
	        
	        offset = 0;
	        for(i=0; i<4; i++){
	        	coeff_token_vlc[i] = new VLC();
//	            coeff_token_vlc[i].table_base = coeff_token_vlc_tables;
//	            coeff_token_vlc[i].table_offset = offset;
	            coeff_token_vlc[i].table_base = CAVLCTables.expandTable(CAVLCTables.coeff_token_vlc_table[i]);
	            coeff_token_vlc[i].table_offset = 0;
	            coeff_token_vlc[i].table_allocated = coeff_token_vlc_tables_size[i];
	            /*
	            GetBitContext.init_vlc(coeff_token_vlc[i], H264Context.COEFF_TOKEN_VLC_BITS, 4*17,
	                     coeff_token_len [i],0, 1, 1,
	                     coeff_token_bits[i],0, 1, 1,
	                     GetBitContext.INIT_VLC_USE_NEW_STATIC);
	            */
	            offset += coeff_token_vlc_tables_size[i];
	        }
	        /*
	         * This is a one time safety check to make sure that
	         * the packed static coeff_token_vlc table sizes
	         * were initialized correctly.
	         */
	        ////assert(offset == FF_ARRAY_ELEMS(coeff_token_vlc_tables));

	        for(i=0; i<3; i++){
	        	chroma_dc_total_zeros_vlc[i] = new VLC();
//	            chroma_dc_total_zeros_vlc[i].table_base = chroma_dc_total_zeros_vlc_tables[i];
	            chroma_dc_total_zeros_vlc[i].table_base = CAVLCTables.expandTable(CAVLCTables.chroma_dc_total_zeros_vlc_table[i]);
	            chroma_dc_total_zeros_vlc[i].table_offset = 0;
	            chroma_dc_total_zeros_vlc[i].table_allocated = chroma_dc_total_zeros_vlc_tables_size;
	            /*
	            GetBitContext.init_vlc(chroma_dc_total_zeros_vlc[i],
	            		H264Context.CHROMA_DC_TOTAL_ZEROS_VLC_BITS, 4,
	                     chroma_dc_total_zeros_len [i],0, 1, 1,
	                     chroma_dc_total_zeros_bits[i],0, 1, 1,
	                     GetBitContext.INIT_VLC_USE_NEW_STATIC);
	            */
	        }
	        for(i=0; i<15; i++){
	        	total_zeros_vlc[i] = new VLC();
//	            total_zeros_vlc[i].table_base = total_zeros_vlc_tables[i];
	            total_zeros_vlc[i].table_base = CAVLCTables.expandTable(CAVLCTables.total_zeros_vlc[i]);
	            total_zeros_vlc[i].table_offset = 0;
	            total_zeros_vlc[i].table_allocated = total_zeros_vlc_tables_size;
	            /*
	            GetBitContext.init_vlc(total_zeros_vlc[i],
	            		H264Context.TOTAL_ZEROS_VLC_BITS, 16,
	                     total_zeros_len [i],0, 1, 1,
	                     total_zeros_bits[i],0, 1, 1,
	                     GetBitContext.INIT_VLC_USE_NEW_STATIC);
	            */
	        }

	        for(i=0; i<6; i++){
	        	run_vlc[i] = new VLC();
//	            run_vlc[i].table_base = run_vlc_tables[i];
	            run_vlc[i].table_base = CAVLCTables.expandTable(CAVLCTables.run_vlc_table[i]);
	            run_vlc[i].table_offset = 0;
	            run_vlc[i].table_allocated = run_vlc_tables_size;
	            /*
	            GetBitContext.init_vlc(run_vlc[i],
	            		H264Context.RUN_VLC_BITS, 7,
	                     run_len [i],0, 1, 1,
	                     run_bits[i],0, 1, 1,
	                     GetBitContext.INIT_VLC_USE_NEW_STATIC);
	            */
	        }
//	        run7_vlc.table_base = run7_vlc_table;
	        run7_vlc.table_base = CAVLCTables.expandTable(CAVLCTables.run7_vlc_table);
	        run7_vlc.table_offset = 0;
	        run7_vlc.table_allocated = run7_vlc_table_size;
	        /*
	        GetBitContext.init_vlc(run7_vlc, H264Context.RUN7_VLC_BITS, 16,
	                 run_len [6],0, 1, 1,
	                 run_bits[6],0, 1, 1,
	                 GetBitContext.INIT_VLC_USE_NEW_STATIC);
			*/
	        init_cavlc_level_tab();
	    }
	}

	/**
	 *
	 */
	public static /*inline*/ int get_level_prefix(GetBitContext gb){
	    long buf;
	    int log;

	    //OPEN_READER(re, gb);
        int re_index= gb.index;
        long re_cache= 0;
	    
	    //UPDATE_CACHE(re, gb);
        int pos = gb.buffer_offset + (re_index>>3);
        //re_cache = ((gb.buffer[pos+3]<<24)|(gb.buffer[pos+2]<<16)|(gb.buffer[pos+1]<<8)|(gb.buffer[pos])) >> (re_index&0x07);
        re_cache = gb.buffer[pos+0];
        re_cache = (re_cache << 8) |  gb.buffer[pos+1];
        re_cache = (re_cache << 8) |  gb.buffer[pos+2];
        re_cache = (re_cache << 8) |  gb.buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
        
        
	    //buf=GET_CACHE(re, gb);
	    buf = (re_cache);

	    log= 32 - av_log2(buf);
	    /*
	#ifdef TRACE
	    print_bin(buf>>(32-log), log);
	    av_log(NULL, AV_LOG_DEBUG, "%5d %2d %3d lpr @%5d in %s get_level_prefix\n", buf>>(32-log), log, log-1, get_bits_count(gb), __FILE__);
	#endif
	*/

	    //LAST_SKIP_BITS(re, gb, log);
        re_index += (log);
	    
        //System.out.println("get_level_prefix(,"+log+"): "+ (log-1));
	    //CLOSE_READER(re, gb);
        gb.index= re_index;

	    return log-1;
	}

	/**
	 * decodes a residual block.
	 * @param n block index
	 * @param scantable scantable
	 * @param max_coeff number of coefficients in the block
	 * @return <0 if an error occurred
	 */
	public static int decode_residual(H264Context  h, GetBitContext gb, short[] block_base, int block_offset
			, int n, int[] scantable_base, int scantable_offset, long[] qmul_base, int qmul_offset, int max_coeff){
//			, int n, uint8_t *scantable, uint32_t *qmul, int max_coeff){
	    //MpegEncContext s = h.s;
	    final int[/*17*/] coeff_token_table_index= {0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3};
	    int[] level = new int[16];
	    int zeros_left, coeff_token, total_coeff, i, trailing_ones, run_before;

	    //FIXME put trailing_onex into the context

	    if(n >= H264Context.CHROMA_DC_BLOCK_INDEX){
	        coeff_token= gb.get_vlc2( 
	        		chroma_dc_coeff_token_vlc.table_base, chroma_dc_coeff_token_vlc.table_offset,
	        		H264Context.CHROMA_DC_COEFF_TOKEN_VLC_BITS, 1, "coeff_token_CROMA_DC");
	        total_coeff= coeff_token>>2;
        	//System.out.println("get_vlc2(CHROMA_DC_COEFF_TOKEN) => total_coeff = " + total_coeff );
	    }else{
	        if(n == H264Context.LUMA_DC_BLOCK_INDEX){
	            total_coeff= pred_non_zero_count(h, 0);
	            //System.out.println("Prediected Non-Zero count = "+total_coeff);
	            coeff_token= gb.get_vlc2( 
	            		coeff_token_vlc[ coeff_token_table_index[total_coeff] ].table_base,
	            		coeff_token_vlc[ coeff_token_table_index[total_coeff] ].table_offset,
	            		H264Context.COEFF_TOKEN_VLC_BITS, 2, "coeff_token_LUMA_DC");
	            total_coeff= coeff_token>>2;
	        	//System.out.println("get_vlc2(LUMA_DC_COEFF_TOKEN) => total_coeff = " + total_coeff );
	        }else{
	            total_coeff= pred_non_zero_count(h, n);

	            // DebugTool.printDebugString("predicted non_zero_count("+n+") = "+total_coeff+"\n");	                	
	            
	            	            
	            coeff_token= gb.get_vlc2( 
	            		coeff_token_vlc[ coeff_token_table_index[total_coeff] ].table_base, 
	            		coeff_token_vlc[ coeff_token_table_index[total_coeff] ].table_offset, 
	            		H264Context.COEFF_TOKEN_VLC_BITS, 2, "coeff_token_LUMA(2)_DC");
	            total_coeff= coeff_token>>2;

        		// DebugTool.printDebugString("predicted non_zero_count-2 = "+total_coeff+"\n");	                	
        
	        }
	    }
	    h.non_zero_count_cache[ H264Context.scan8[n] ]= total_coeff;

	    //FIXME set last_non_zero?

	    if(total_coeff==0)
	        return 0;
	    if(total_coeff > /*(unsigned)*/max_coeff) {
//	        av_log(h.s.avctx, AV_LOG_ERROR, "corrupted macroblock %d %d (total_coeff=%d)\n", s.mb_x, s.mb_y, total_coeff);
        	// DebugTool.printDebugString("  --------  Error type 1.0\n");
	    	return -1;
	    }

	    trailing_ones= coeff_token&3;
//	    tprintf(h.s.avctx, "trailing:%d, total:%d\n", trailing_ones, total_coeff);
	    //assert(total_coeff<=16);
	    
	    // DebugTool.printDebugString("   - trailing_ones = "+trailing_ones+"\n");

	    i = (int)gb.show_bits(3);
	    gb.skip_bits(trailing_ones);
	    level[0] = 1-((i&4)>>1);
	    level[1] = 1-((i&2)   );
	    level[2] = 1-((i&1)<<1);

	    // DebugTool.printDebugString("   - i = "+i+"\n");

	    if(trailing_ones<total_coeff) {
	        int mask, prefix;
	        int suffix_length = ((total_coeff > 10)?1:0) & ((trailing_ones < 3)?1:0);
	        int bitsi= (int)gb.show_bits(LEVEL_TAB_BITS);
	        int level_code= cavlc_level_tab[suffix_length][bitsi][0];

		    // DebugTool.printDebugString("   - bitsi = "+bitsi+", level_code = "+level_code+", skip_bit = "+cavlc_level_tab[suffix_length][bitsi][1]+"\n");

	        gb.skip_bits(cavlc_level_tab[suffix_length][bitsi][1]);
	        if(level_code >= 100){
	            prefix= level_code - 100;
	            if(prefix == LEVEL_TAB_BITS)
	                prefix += get_level_prefix(gb);

			    // DebugTool.printDebugString("   - prefix = "+prefix+"\n");
	            
	            //first coefficient has suffix_length equal to 0 or 1
	            if(prefix<14){ //FIXME try to build a large unified VLC table for all this
	                if(suffix_length!=0)
	                    level_code= (int)((prefix<<1) + gb.get_bits1("level_code")); //part
	                else
	                    level_code= prefix; //part
	            }else if(prefix==14){
	                if(suffix_length!=0)
	                    level_code= (int)((prefix<<1) + gb.get_bits1("level_code")); //part
	                else
	                    level_code= (int)(prefix + gb.get_bits(4,"level_code")); //part
	            }else{
	            	// Nok: Prevent bug?? in ffmpeg that the decoder may overflow?
	            	level_code= 30 + (int)(gb.get_bits(prefix-3,"level_code")); //part
	                if(prefix>=16){
	                    if(prefix > 25+3){
	                    	// DebugTool.printDebugString("  --------  Error type 1.1\n");
	                        //av_log(h.s.avctx, AV_LOG_ERROR, "Invalid level prefix\n");
	                        return -1;
	                    }
	                    level_code += (1<<(prefix-3))-4096;
	                }
	            }

	            if(trailing_ones < 3) level_code += 2;

			    // DebugTool.printDebugString("   - level_code(1) = "+level_code+"\n");

	            suffix_length = 2;
	            mask= -(level_code&1);
	            level[trailing_ones]= (((2+level_code)>>1) ^ mask) - mask;
	        }else{
	            level_code += ((level_code>>31)|1) & -((trailing_ones < 3)?1:0);

			    // DebugTool.printDebugString("   - level_code(2) = "+level_code+"\n");

	            // Fix: for difference between JAVA and C, we need to convert signed to unsigned when we compare
	            //      signed and unsigned number because, in C, such comparing will be done in common part.
	            //      In which, common part in C in unsigned, while java is signed. -= Nok =-			    
	            suffix_length = 1 + (( (0xffffffffL & (level_code + 3)) > 6)?1:0);
	            level[trailing_ones]= level_code;
	        }

		    // DebugTool.printDebugString("   - suffix_length(1) = "+suffix_length+"\n");

	        //remaining coefficients have suffix_length > 0
	        for(i=trailing_ones+1;i<total_coeff;i++) {
	            final int[/*7*/] suffix_limit = {0,3,6,12,24,48, Integer.MAX_VALUE };
	            bitsi= (int)gb.show_bits(LEVEL_TAB_BITS);
	            level_code= cavlc_level_tab[suffix_length][bitsi][0];

			    // DebugTool.printDebugString("       - cavlc_level_tab["+suffix_length+"]["+bitsi+"][0] = "+level_code+"\n");

	            gb.skip_bits(cavlc_level_tab[suffix_length][bitsi][1]);
	            if(level_code >= 100){
	                prefix= level_code - 100;
	                if(prefix == LEVEL_TAB_BITS){
	                    prefix += get_level_prefix(gb);
	                }
	                if(prefix<15){
	                    level_code = (int)((prefix<<suffix_length) + gb.get_bits(suffix_length,"level_code"));
	                }else{
	                    level_code = (int)((15<<suffix_length) + gb.get_bits(prefix-3,"level_code"));
	                    if(prefix>=16)
	                        level_code += (1<<(prefix-3))-4096;
	                }
	                mask= -(level_code&1);
	                level_code= (((2+level_code)>>1) ^ mask) - mask;
	            }
	            level[i]= level_code;

	            // DebugTool.printDebugString("       - prefix(6) = "+level_code+"\n");

	            // Fix: for difference between JAVA and C, we need to convert signed to unsigned when we compare
	            //      signed and unsigned number because, in C, such comparing will be done in common part.
	            //      In which, common part in C in unsigned, while java is signed. -= Nok =-
	            suffix_length+= (( (0xffffffffL & (suffix_limit[suffix_length] + level_code)) > (2L*suffix_limit[suffix_length]))?1:0);
	        }
	        
		    // DebugTool.printDebugString("   - suffix_length(2) = "+suffix_length+"\n");
	    }

	    if(total_coeff == max_coeff)
	        zeros_left=0;
	    else{
	        if(n >= H264Context.CHROMA_DC_BLOCK_INDEX) {
	            zeros_left= gb.get_vlc2(chroma_dc_total_zeros_vlc[ total_coeff-1 ].table_base,
	            		chroma_dc_total_zeros_vlc[ total_coeff-1 ].table_offset,
	            		H264Context.CHROMA_DC_TOTAL_ZEROS_VLC_BITS, 1, "coeff_token_CROMA_DC_TOTAL_ZERO");
	        	// DebugTool.printDebugString("get_vlc2(CHROMA_DC_ZERO_LEFT) => zeros_left = " + zeros_left +"\n");
	        } else {
	            zeros_left= gb.get_vlc2(total_zeros_vlc[ total_coeff-1 ].table_base,
	            		total_zeros_vlc[ total_coeff-1 ].table_offset,
	            		H264Context.TOTAL_ZEROS_VLC_BITS, 1, "coeff_token_TOTAL_ZEROS");
	            // DebugTool.printDebugString("get_vlc2(LUMA_DC_ZERO_LEFT) => zeros_left = " + zeros_left +"\n");
	        } // if
	    }

	    /*
	    // DebugTool.printDebugString("scantable_offset(before) = {"
	    		+scantable_base[scantable_offset+0]+","
	    		+scantable_base[scantable_offset+1]+","
	    		+scantable_base[scantable_offset+2]+","
	    		+scantable_base[scantable_offset+3]+","
	    		+scantable_base[scantable_offset+4]
	    		+"}\n");
	    */
	    scantable_offset += zeros_left + total_coeff - 1;

	    /*
	    // DebugTool.printDebugString("scantable_offset(after) = {"
	    		+scantable_base[scantable_offset+0]+","
	    		+scantable_base[scantable_offset+1]+","
	    		+scantable_base[scantable_offset+2]+","
	    		+scantable_base[scantable_offset+3]+","
	    		+scantable_base[scantable_offset+4]
	    		+"}\n");
	     */
	   
	    //!!????????????????????????????????? Magic about array resizing??
	    if(scantable_offset < 0) {
	    	int[] new_scantable_base = new int[scantable_base.length + (-scantable_offset)];
	    	System.arraycopy(scantable_base, 0, new_scantable_base, -scantable_offset, scantable_base.length);
	    	scantable_base = new_scantable_base;
	    	scantable_offset = 0;
	    }
	    	
	    if(n >= H264Context.LUMA_DC_BLOCK_INDEX){
	    	// DebugTool.printDebugString("****RESE-CASE 1\n");
	    	
	        block_base[block_offset + scantable_base[scantable_offset]] = (short)level[0];
	        for(i=1;i<total_coeff && zeros_left > 0;i++) {
	            if(zeros_left < 7) {
	            	//????????????????????????????????????????/
	               // run_before= gb.get_vlc2((run_vlc-1)[zeros_left].table, H264Context.RUN_VLC_BITS, 1);
		            run_before= gb.get_vlc2(run_vlc[zeros_left-1].table_base, run_vlc[zeros_left-1].table_offset, H264Context.RUN_VLC_BITS, 1, "RUN_VLC");
		        	//System.out.println("get_vlc2(LUMA_RUN_VLC) => run_before = " + run_before );
		        }  else {
	                run_before= gb.get_vlc2(run7_vlc.table_base, run7_vlc.table_offset, H264Context.RUN7_VLC_BITS, 2, "RUN7_VLC");
		        	//System.out.println("get_vlc2(LUMA_RUN_VLC7) => run_before = " + run_before );
		        } // if
	            ////System.out.println("run_before = "+run_before);
	            zeros_left -= run_before;
	            scantable_offset -= 1 + run_before;
	            block_base[block_offset + scantable_base[scantable_offset]]= (short)level[i];
	        }
	        for(;i<total_coeff;i++) {
	            scantable_offset--;
	            block_base[block_offset + scantable_base[scantable_offset]]= (short)level[i];
	        }
	    }else{
	    	// DebugTool.printDebugString("****RESE-CASE 2\n");

	    	block_base[block_offset + scantable_base[scantable_offset]] = (short)((level[0] * qmul_base[qmul_offset + scantable_base[scantable_offset]] + 32)>>6);
	        for(i=1;i<total_coeff && zeros_left > 0;i++) {
	            if(zeros_left < 7) {
	                run_before= gb.get_vlc2(run_vlc[zeros_left-1].table_base, run_vlc[zeros_left-1].table_offset, H264Context.RUN_VLC_BITS, 1, "RUN_VLC");
		        	//System.out.println("get_vlc2(CHROMA_RUN_VLC) => run_before = " + run_before );
	            } else {
	                run_before= gb.get_vlc2(run7_vlc.table_base, run7_vlc.table_offset, H264Context.RUN7_VLC_BITS, 2, "RUN7_VLC");
		        	//System.out.println("get_vlc2(CHROMA_RUN_VLC7) => run_before = " + run_before );
	            } // if
				////System.out.println("run_before = "+run_before);
	            zeros_left -= run_before;
	            scantable_offset -= (1 + run_before);
	            block_base[block_offset + scantable_base[scantable_offset]]= (short)((level[i] * qmul_base[qmul_offset + scantable_base[scantable_offset]] + 32)>>6);
	        }
	        for(;i<total_coeff;i++) {
	            scantable_offset--;
	            block_base[block_offset + scantable_base[scantable_offset]]= (short)((level[i] * qmul_base[qmul_offset + scantable_base[scantable_offset]] + 32)>>6);
	        }
	    }

	    if(zeros_left<0){
	        //av_log(h.s.avctx, AV_LOG_ERROR, "negative number of zero coeffs at %d %d\n", s.mb_x, s.mb_y);
        	// DebugTool.printDebugString("  --------  Error type 1.2\n");
	    	return -1;
	    }

	    return 0;
	}

	public int ff_h264_decode_mb_cavlc(H264Context  h){
	    MpegEncContext s = h.s;
	    int mb_xy;
	    int partition_count;
	    int mb_type, cbp;
	    int dct8x8_allowed= h.pps.transform_8x8_mode;

        // DebugTool.dumpDebugFrameData(h, "BEFORE-ff_h264_decode_mb_cavlc");
	    
	    mb_xy = h.mb_xy = s.mb_x + s.mb_y*s.mb_stride;

	    //tprintf(s.avctx, "pic:%d mb:%d/%d\n", h.frame_num, s.mb_x, s.mb_y);
	    cbp = 0; /* avoid warning. FIXME: find a solution without slowing
	                down the code */
	    if(h.slice_type_nos != H264Context.FF_I_TYPE){
	        if(s.mb_skip_run==-1)
	            s.mb_skip_run= s.gb.get_ue_golomb("mb_skip_run");

	        if (0 != s.mb_skip_run--) {
	            if(h.mb_aff_frame !=0 && (s.mb_y&1) == 0){
	                if(s.mb_skip_run==0)
	                    h.mb_mbaff = h.mb_field_decoding_flag = (int)s.gb.get_bits1("mb_mbaff");
	            }
	            h.decode_mb_skip();
	            return 0;
	        }
	    }
	    if(h.mb_aff_frame != 0){
	        if( (s.mb_y&1) == 0 )
	            h.mb_mbaff = h.mb_field_decoding_flag = (int)s.gb.get_bits1("mb_field_decoding_flag");
	    }

	    h.prev_mb_skipped= 0;

	    mb_type= s.gb.get_ue_golomb("mb_type");
	    if(h.slice_type_nos == H264Context.FF_B_TYPE){
	        if(mb_type < 23){
	            partition_count= H264Data.b_mb_type_info[mb_type].partition_count;
	            mb_type=         H264Data.b_mb_type_info[mb_type].type;
	        }else{
	            mb_type -= 23;
//	            goto decode_intra_mb;
//	        	decode_intra_mb:
	    	        if(mb_type > 25){
	    	            //av_log(h.s.avctx, AV_LOG_ERROR, "mb_type %d in %c slice too large at %d %d\n", mb_type, av_get_pict_type_char(h.slice_type), s.mb_x, s.mb_y);
	    	            return -1;
	    	        }
	    	        partition_count=0;
	    	        cbp= H264Data.i_mb_type_info[mb_type].cbp;
	    	        h.intra16x16_pred_mode= H264Data.i_mb_type_info[mb_type].pred_mode;
	    	        mb_type= H264Data.i_mb_type_info[mb_type].type;	            
	        }
	    }else if(h.slice_type_nos == H264Context.FF_P_TYPE){
	        if(mb_type < 5){
	            partition_count= H264Data.p_mb_type_info[mb_type].partition_count;
	            mb_type=         H264Data.p_mb_type_info[mb_type].type;
	        }else{
	            mb_type -= 5;
//	            goto decode_intra_mb;
//	        	decode_intra_mb:
	    	        if(mb_type > 25){
	    	            //av_log(h.s.avctx, AV_LOG_ERROR, "mb_type %d in %c slice too large at %d %d\n", mb_type, av_get_pict_type_char(h.slice_type), s.mb_x, s.mb_y);
	    	            return -1;
	    	        }
	    	        partition_count=0;
	    	        cbp= H264Data.i_mb_type_info[mb_type].cbp;
	    	        h.intra16x16_pred_mode= H264Data.i_mb_type_info[mb_type].pred_mode;
	    	        mb_type= H264Data.i_mb_type_info[mb_type].type;
	        }
	    }else{
	       //assert(h.slice_type_nos == H264Context.FF_I_TYPE);
	        if(h.slice_type == H264Context.FF_SI_TYPE && mb_type!=0)
	            mb_type--;
	//decode_intra_mb:
	        if(mb_type > 25){
	            //av_log(h.s.avctx, AV_LOG_ERROR, "mb_type %d in %c slice too large at %d %d\n", mb_type, av_get_pict_type_char(h.slice_type), s.mb_x, s.mb_y);
	            return -1;
	        }
	        partition_count=0;
	        cbp= H264Data.i_mb_type_info[mb_type].cbp;
	        h.intra16x16_pred_mode= H264Data.i_mb_type_info[mb_type].pred_mode;
	        mb_type= H264Data.i_mb_type_info[mb_type].type;
	    }

	    if(h.mb_field_decoding_flag != 0)
	        mb_type |= H264Context.MB_TYPE_INTERLACED;

	    h.slice_table_base[h.slice_table_offset +  mb_xy ]= h.slice_num;

	    if(0!= (mb_type & H264Context.MB_TYPE_INTRA_PCM)){
	        int x;

	        // We assume these blocks are very rare so we do not optimize it.
	        s.gb.align_get_bits();

	        // The pixels are stored in the same order as levels in h.mb array.
	        for(x=0; x < ((h.sps.chroma_format_idc!=0) ? 384 : 256); x+=2){
	        	// Combine 2 byte to get a short.
	            //((uint8_t*)h.mb)[x]= (int)s.gb.get_bits(8);
	        	int val = (int)s.gb.get_bits(8,"val");
	        	val = val | (((int)s.gb.get_bits(8,"val"))<<8);
	        	h.mb[x/2] = (short)val;
	        }

	        // In deblocking, the quantizer is 0
	        s.current_picture.qscale_table[mb_xy]= 0;
	        // All coeffs are present
	        //memset(h.non_zero_count[mb_xy], 16, 32);
	        Arrays.fill(h.non_zero_count[mb_xy],0,32,16);

	        s.current_picture.mb_type_base[s.current_picture.mb_type_offset + mb_xy]= mb_type;
	        return 0;
	    }

	    if(h.mb_mbaff != 0){
	        h.ref_count[0] <<= 1;
	        h.ref_count[1] <<= 1;
	    }

	    h.fill_decode_neighbors(mb_type);
	    h.fill_decode_caches(mb_type);

	    //mb_pred
	    if(0!= (mb_type & 7)){
	        int pred_mode;
//	            init_top_left_availability(h);
	        if(0!=(mb_type & H264Context.MB_TYPE_INTRA4x4)){
	            int i;
	            int di = 1;
	            if(dct8x8_allowed!=0 && s.gb.get_bits1("MB_TYPE_8x8DCT?")!=0){
	                mb_type |= H264Context.MB_TYPE_8x8DCT;
	                di = 4;
	            }

//	                fill_intra4x4_pred_table(h);
	            for(i=0; i<16; i+=di){
	                int mode= h.pred_intra_mode(i);

	                if(0==s.gb.get_bits1("rem_mode")){
	                    int rem_mode= (int)s.gb.get_bits(3,"rem_mode");
	                    mode = rem_mode + ((rem_mode >= mode)?1:0);
	                }

	                if(di==4)
	                    Rectangle.fill_rectangle_sign( h.intra4x4_pred_mode_cache, h.scan8[i] , 2, 2, 8, mode, 1 );
	                else
	                    h.intra4x4_pred_mode_cache[ h.scan8[i] ] = mode;
	            }
	            h.ff_h264_write_back_intra_pred_mode();
	            if( h.ff_h264_check_intra4x4_pred_mode() < 0)
	                return -1;
	        }else{
	            h.intra16x16_pred_mode= h.ff_h264_check_intra_pred_mode(h.intra16x16_pred_mode);
	            if(h.intra16x16_pred_mode < 0)
	                return -1;
	        }
	        if(h.sps.chroma_format_idc != 0){
	            pred_mode= h.ff_h264_check_intra_pred_mode(s.gb.get_ue_golomb_31("pred_mode"));
	            if(pred_mode < 0)
	                return -1;
	            h.chroma_pred_mode= pred_mode;
	        }
	    }else if(partition_count==4){
	        int i, j;
	        int[] sub_partition_count = new int[4];
	        int list;
	        int[][]ref = new int[2][4];

	        if(h.slice_type_nos == H264Context.FF_B_TYPE){
	            for(i=0; i<4; i++){
	                h.sub_mb_type[i]= s.gb.get_ue_golomb_31("sub_mb_type");
	                if(h.sub_mb_type[i] >=13){
	                    //av_log(h.s.avctx, AV_LOG_ERROR, "B sub_mb_type %u out of range at %d %d\n", h.sub_mb_type[i], s.mb_x, s.mb_y);
	                    return -1;
	                }
	                sub_partition_count[i]= H264Data.b_sub_mb_type_info[ h.sub_mb_type[i] ].partition_count;
	                h.sub_mb_type[i]=      H264Data.b_sub_mb_type_info[ h.sub_mb_type[i] ].type;
	            }
	            if( 0!=((h.sub_mb_type[0]|h.sub_mb_type[1]|h.sub_mb_type[2]|h.sub_mb_type[3])&H264Context.MB_TYPE_DIRECT2)) {
	            	mb_type = h.ff_h264_pred_direct_motion(mb_type);
	                h.ref_cache[0][h.scan8[4]] =
	                h.ref_cache[1][h.scan8[4]] =
	                h.ref_cache[0][h.scan8[12]] =
	                h.ref_cache[1][h.scan8[12]] = H264Context.PART_NOT_AVAILABLE;
	            }
	        }else{
	            //assert(h.slice_type_nos == H264Context.FF_P_TYPE); //FIXME SP correct ?
	            for(i=0; i<4; i++){
	                h.sub_mb_type[i]= s.gb.get_ue_golomb_31("sub_mb_type");
	                if(h.sub_mb_type[i] >=4){
	                    //av_log(h.s.avctx, AV_LOG_ERROR, "P sub_mb_type %u out of range at %d %d\n", h.sub_mb_type[i], s.mb_x, s.mb_y);
	                    return -1;
	                }
	                sub_partition_count[i]= H264Data.p_sub_mb_type_info[ h.sub_mb_type[i] ].partition_count;
	                h.sub_mb_type[i]=      H264Data.p_sub_mb_type_info[ h.sub_mb_type[i] ].type;
	            }
	        }

	        for(list=0; list<h.list_count; list++){
	            int ref_count= ((mb_type & H264Context.MB_TYPE_REF0)!=0) ? 1 : (int)h.ref_count[list];
	            for(i=0; i<4; i++){
	                if(0!=(h.sub_mb_type[i] & H264Context.MB_TYPE_DIRECT2)) continue;
	                if(0!=((h.sub_mb_type[i]) & (H264Context.MB_TYPE_P0L0<<((0)+2*(list))))){
	                    int tmp;
	                    if(ref_count == 1){
	                        tmp= 0;
	                    }else if(ref_count == 2){
	                        tmp= (int)s.gb.get_bits1("ref_count?")^1;
	                    }else{
	                        tmp= s.gb.get_ue_golomb_31("ref_count?");
	                        if(tmp>=ref_count){
	                            //av_log(h.s.avctx, AV_LOG_ERROR, "ref %u overflow\n", tmp);
	                            return -1;
	                        }
	                    }
	                    ref[list][i]= tmp;
	                }else{
	                 //FIXME
	                    ref[list][i] = -1;
	                }
	            }
	        }

	        if(dct8x8_allowed!=0)
	            dct8x8_allowed = h.get_dct8x8_allowed();

	        for(list=0; list<h.list_count; list++){
	            for(i=0; i<4; i++){
	                if(0!=(h.sub_mb_type[i] & H264Context.MB_TYPE_DIRECT2)) {
	                    h.ref_cache[list][ h.scan8[4*i] ] = h.ref_cache[list][ h.scan8[4*i]+1 ];
	                    continue;
	                }
	                h.ref_cache[list][ h.scan8[4*i]   ]=h.ref_cache[list][ h.scan8[4*i]+1 ]=
	                h.ref_cache[list][ h.scan8[4*i]+8 ]=h.ref_cache[list][ h.scan8[4*i]+9 ]= ref[list][i];

	                if(((h.sub_mb_type[i]) & (H264Context.MB_TYPE_P0L0<<((0)+2*(list)))) != 0 ) {//IS_DIR(h.sub_mb_type[i], 0, list)){
	                    int sub_mb_type= h.sub_mb_type[i];
	                    int block_width= ((sub_mb_type & (H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_16x8))!=0 ? 2 : 1);
	                    for(j=0; j<sub_partition_count[i]; j++){
	                        int mx, my;
	                        int index= 4*i + block_width*j;
	                        //int16_t (* mv_cache)[2]= &h.mv_cache[list][ h.scan8[index] ];
	                        int[][] mv_cache_base = h.mv_cache[list];
	                        int my_cache_offset = h.scan8[index];
	                        int[] mxmy = new int[2];
	                        h.pred_motion(index, block_width, list, h.ref_cache[list][ h.scan8[index] ], mxmy);
	                        mx = mxmy[0];
	                        my = mxmy[1];
	                        mx += s.gb.get_se_golomb("mx?");
	                        my += s.gb.get_se_golomb("my?");
	                        //tprintf(s.avctx, "final mv:%d %d\n", mx, my);

	                        if(0!=(sub_mb_type&H264Context.MB_TYPE_16x16)){
	                            mv_cache_base[my_cache_offset + 1 ][0]=
	                            mv_cache_base[my_cache_offset + 8 ][0]= mv_cache_base[my_cache_offset + 9 ][0]= mx;
	                            mv_cache_base[my_cache_offset + 1 ][1]=
	                            mv_cache_base[my_cache_offset + 8 ][1]= mv_cache_base[my_cache_offset + 9 ][1]= my;
	                        }else if(0!=(sub_mb_type&H264Context.MB_TYPE_16x8)){
	                            mv_cache_base[my_cache_offset + 1 ][0]= mx;
	                            mv_cache_base[my_cache_offset + 1 ][1]= my;
	                        }else if(0!=(sub_mb_type&H264Context.MB_TYPE_8x16)){
	                            mv_cache_base[my_cache_offset + 8 ][0]= mx;
	                            mv_cache_base[my_cache_offset + 8 ][1]= my;
	                        }
	                        mv_cache_base[my_cache_offset + 0 ][0]= mx;
	                        mv_cache_base[my_cache_offset + 0 ][1]= my;
	                    }
	                }else{
	                	/*
	                    uint32_t *p= (uint32_t *)&h.mv_cache[list][ scan8[4*i] ][0];
	                    p[0] = p[1]=
	                    p[8] = p[9]= 0;
	                    */
	                	h.mv_cache[list][h.scan8[4*i] + 0][0] = (short)0;
	                	h.mv_cache[list][h.scan8[4*i] + 1][0] = (short)0;
	                	h.mv_cache[list][h.scan8[4*i] + 8][0] = (short)0;
	                	h.mv_cache[list][h.scan8[4*i] + 9][0] = (short)0;
	                	h.mv_cache[list][h.scan8[4*i] + 0][1] = (short)0;
	                	h.mv_cache[list][h.scan8[4*i] + 1][1] = (short)0;
	                	h.mv_cache[list][h.scan8[4*i] + 8][1] = (short)0;
	                	h.mv_cache[list][h.scan8[4*i] + 9][1] = (short)0;
	                }
	            }
	        }
	    }else if(0!=(mb_type & H264Context.MB_TYPE_DIRECT2)){
	        mb_type = h.ff_h264_pred_direct_motion(mb_type);
	        dct8x8_allowed &= h.sps.direct_8x8_inference_flag;
	    }else{
	        int list, mx=0, my=0, i;
	         //FIXME we should set ref_idx_l? to 0 if we use that later ...
	        if(0!=(mb_type&H264Context.MB_TYPE_16x16)){
	            for(list=0; list<h.list_count; list++){
	                    int val;
	                    if(((mb_type) & (H264Context.MB_TYPE_P0L0<<((0)+2*(list)))) != 0 ){
	                        if(h.ref_count[list]==1){
	                            val= 0;
	                        }else if(h.ref_count[list]==2){
	                            val= (int)(s.gb.get_bits1("ref_count?")^1);
	                        }else{
	                            val= s.gb.get_ue_golomb_31("ref_count?");
	                            if(val >= h.ref_count[list]){
	                                //av_log(h.s.avctx, AV_LOG_ERROR, "ref %u overflow\n", val);
	                                return -1;
	                            }
	                        }
	                    Rectangle.fill_rectangle_sign(h.ref_cache[list], h.scan8[0], 4, 4, 8, val, 1);
	                    }
	            }
	            for(list=0; list<h.list_count; list++){
	                if(((mb_type) & (H264Context.MB_TYPE_P0L0<<((0)+2*(list)))) != 0 ){
                    	int[] mxmy = new int[] { mx, my };
	                    h.pred_motion(0, 4, list, h.ref_cache[list][ h.scan8[0] ], mxmy);
                        mx = mxmy[0];
                        my = mxmy[1];
	                    mx += s.gb.get_se_golomb("mx?");
	                    my += s.gb.get_se_golomb("my?");
	                    //int val = h.pack16to32(mx,my);
	                    
	                    // DebugTool.printDebugString("    ****(1) mx="+mx+", my="+my+", val="+val+"\n");
	                    //tprintf(s.avctx, "final mv:%d %d\n", mx, my);

	                    Rectangle.fill_rectangle_mv_cache(h.mv_cache[list], h.scan8[0] , 4, 4, 8, h.pack16to32(mx,my), 4);
	                }
	            }
	        }
	        else if(0!=(mb_type&H264Context.MB_TYPE_16x8)){
	            for(list=0; list<h.list_count; list++){
	                    for(i=0; i<2; i++){
	                        int val;
	                        if(((mb_type) & (H264Context.MB_TYPE_P0L0<<((i)+2*(list)))) != 0 ){
	                            if(h.ref_count[list] == 1){
	                                val= 0;
	                            }else if(h.ref_count[list] == 2){
	                                val= (int)(s.gb.get_bits1("ref_count?")^1);
	                            }else{
	                                val= s.gb.get_ue_golomb_31("ref_count?");
	                                if(val >= h.ref_count[list]){
	                                    //av_log(h.s.avctx, AV_LOG_ERROR, "ref %u overflow\n", val);
	                                    return -1;
	                                }
	                            }
	                        }else {
	                        	//!!?????????????????????? Need unsigneded??
	                            val= H264Context.LIST_NOT_USED;
	                        } // if
	                        Rectangle.fill_rectangle_sign(h.ref_cache[list], h.scan8[0] + 16*i , 4, 2, 8, val, 1);
	                    }
	            }
	            for(list=0; list<h.list_count; list++){
	                for(i=0; i<2; i++){
	                    int val;
	                    if(((mb_type) & (H264Context.MB_TYPE_P0L0<<((i)+2*(list)))) != 0 ){
	                    	int[] mxmy = new int[] { mx, my };
	                        h.pred_16x8_motion(8*i, list, h.ref_cache[list][h.scan8[0] + 16*i], mxmy);
	                        mx = mxmy[0];
	                        my = mxmy[1];
	                        mx += s.gb.get_se_golomb("mx?");
	                        my += s.gb.get_se_golomb("my?");
	                        //tprintf(s.avctx, "final mv:%d %d\n", mx, my);

	                        val= h.pack16to32(mx,my);

		                    // DebugTool.printDebugString("    ****(2) mx="+mx+", my="+my+", val="+val+"\n");

	                    }else
	                        val=0;
	                    Rectangle.fill_rectangle_mv_cache(h.mv_cache[list], h.scan8[0] + 16*i, 4, 2, 8, val, 4);
	                }
	            }
	        }else{
	            ////assert(IS_8X16(mb_type));
	            for(list=0; list<h.list_count; list++){
	                    for(i=0; i<2; i++){
	                        int val;
	                        if(((mb_type) & (H264Context.MB_TYPE_P0L0<<((i)+2*(list)))) != 0 ){ //FIXME optimize
	                            if(h.ref_count[list]==1){
	                                val= 0;
	                            }else if(h.ref_count[list]==2){
	                                val= (int)(s.gb.get_bits1("ref-count?")^1);
	                            }else{
	                                val= s.gb.get_ue_golomb_31("ref-count?");
	                                if(val >= h.ref_count[list]){
	                                    //av_log(h.s.avctx, AV_LOG_ERROR, "ref %u overflow\n", val);
	                                    return -1;
	                                }
	                            }
	                        }else {
	                        	// !!?????????? Need Unsigned
	                        	val= h.LIST_NOT_USED;	
	                        } // if
	                            
	                        Rectangle.fill_rectangle_sign(h.ref_cache[list], h.scan8[0] + 2*i , 2, 4, 8, val, 1);
	                    }
	            }
	            for(list=0; list<h.list_count; list++){
	                for(i=0; i<2; i++){
	                    int val;
	                    if(((mb_type) & (H264Context.MB_TYPE_P0L0<<((i)+2*(list)))) != 0 ){
	                    	int[] mxmy = new int[] { mx, my };
	                    	h.pred_8x16_motion(i*4, list, h.ref_cache[list][ h.scan8[0] + 2*i ], mxmy);
	                    	mx = mxmy[0];
	                    	my = mxmy[1];
	                        mx += s.gb.get_se_golomb("mx?");
	                        my += s.gb.get_se_golomb("my?");
	                        //tprintf(s.avctx, "final mv:%d %d\n", mx, my);

	                        val= h.pack16to32(mx,my);
	                        
		                    // DebugTool.printDebugString("    ****(3) mx="+mx+", my="+my+", val="+val+"\n");

	                    }else
	                        val=0;
	                    Rectangle.fill_rectangle_mv_cache(h.mv_cache[list], h.scan8[0] + 2*i , 2, 4, 8, val, 4);
	                }
	            }
	        }
	    }

	    if(0!=(mb_type &(H264Context.MB_TYPE_16x16|H264Context.MB_TYPE_16x8|H264Context.MB_TYPE_8x16|H264Context.MB_TYPE_8x8)))
	        h.write_back_motion(mb_type);

	    if(0==(mb_type &H264Context.MB_TYPE_INTRA16x16)){
	        cbp= s.gb.get_ue_golomb("cbp");
	        if(cbp > 47){
	            //av_log(h.s.avctx, AV_LOG_ERROR, "cbp too large (%u) at %d %d\n", cbp, s.mb_x, s.mb_y);
	            return -1;
	        }

	        if(h.sps.chroma_format_idc != 0){
	            if(0!=(mb_type & H264Context.MB_TYPE_INTRA4x4)) cbp= H264Data.golomb_to_intra4x4_cbp[cbp];
	            else                     cbp= H264Data.golomb_to_inter_cbp   [cbp];
	        }else{
	            if(0!=(mb_type & H264Context.MB_TYPE_INTRA4x4)) cbp= golomb_to_intra4x4_cbp_gray[cbp];
	            else                     cbp= golomb_to_inter_cbp_gray[cbp];
	        }
	    }

	    if(dct8x8_allowed!=0 && (cbp&15)!=0 && 0==(mb_type & 7)){
	        mb_type |= H264Context.MB_TYPE_8x8DCT*s.gb.get_bits1("mb_type");
	    }
	    h.cbp=
	    h.cbp_table[mb_xy]= cbp;
	    s.current_picture.mb_type_base[s.current_picture.mb_type_offset +mb_xy]= mb_type;

	    if(cbp !=0 ||0!= (mb_type & H264Context.MB_TYPE_INTRA16x16)){
	        int i8x8, i4x4, chroma_idx;
	        int dquant;
	        GetBitContext gb= (((mb_type & 7)!=0) ? h.intra_gb_ptr : h.inter_gb_ptr);
	        //uint8_t *scan, *scan8x8;
	        int[] scan;
	        int[] scan8x8;

	        if(0!=(mb_type & H264Context.MB_TYPE_INTERLACED)){
	            scan8x8= (s.qscale!=0 ? h.field_scan8x8_cavlc : h.field_scan8x8_cavlc_q0);
	            scan= (s.qscale!=0 ? h.field_scan : h.field_scan_q0);
	        }else{
	            scan8x8= (s.qscale!=0 ? h.zigzag_scan8x8_cavlc : h.zigzag_scan8x8_cavlc_q0);
	            scan= (s.qscale!=0 ? h.zigzag_scan : h.zigzag_scan_q0);
	        }

	        dquant= s.gb.get_se_golomb("dquant");

	        s.qscale += dquant;

	        if((/*(unsigned)*/s.qscale) > 51 || s.qscale < 0){
	            if(s.qscale<0) s.qscale+= 52;
	            else            s.qscale-= 52;
	            if((/*(unsigned)*/s.qscale) > 51 || s.qscale < 0){
	                //av_log(h.s.avctx, AV_LOG_ERROR, "dquant out of range (%d) at %d %d\n", dquant, s.mb_x, s.mb_y);
	                return -1;
	            }
	        }

	        h.chroma_qp[0]= h.pps.chroma_qp_table[0][s.qscale];
	        h.chroma_qp[1]= h.pps.chroma_qp_table[1][s.qscale];
	        if(0!=(mb_type & H264Context.MB_TYPE_INTRA16x16)){
	        	// Fill 16 uint16_t with 0
	        	Arrays.fill(h.mb_luma_dc, 0, 16, (short)0);
	            //AV_ZERO128(h.mb_luma_dc+0);
	            //AV_ZERO128(h.mb_luma_dc+8);
	            if( decode_residual(h, h.intra_gb_ptr, h.mb_luma_dc, 0, H264Context.LUMA_DC_BLOCK_INDEX, scan, 0, h.dequant4_coeff[0][s.qscale], 0, 16) < 0){
	                return -1; //FIXME continue if partitioned and other return -1 too
	            }

	            //assert((cbp&15) == 0 || (cbp&15) == 15);

	            if((cbp&15)!=0){
	                /////////////////////////////////

	            	for(i8x8=0; i8x8<4; i8x8++){
	                    for(i4x4=0; i4x4<4; i4x4++){
	                        int index= i4x4 + 4*i8x8;
	                        if( decode_residual(h, h.intra_gb_ptr, h.mb, 16*index, index, scan, 1, h.dequant4_coeff[0][s.qscale], 0, 15) < 0 ){
	                            return -1;
	                        }

	                    }
	                }
	                	
	            }else{
	                Rectangle.fill_rectangle_unsign(h.non_zero_count_cache, h.scan8[0], 4, 4, 8, 0, 1);
	            }
	        }else{
	            for(i8x8=0; i8x8<4; i8x8++){
	                if((cbp & (1<<i8x8))!=0){
	                    if(0!= (mb_type & H264Context.MB_TYPE_8x8DCT)){
	                        short[] buf_base = h.mb;
	                        int buf_offset = 64*i8x8;
	                        for(i4x4=0; i4x4<4; i4x4++){
	                            if( decode_residual(h, gb, buf_base, buf_offset, i4x4+4*i8x8, scan8x8, 16*i4x4,
	                                                h.dequant8_coeff[(0!=( mb_type & 7)) ? 0:1][s.qscale], 0, 16) <0 )
	                                return -1;
	                        }
	                        int[] nnz_base = h.non_zero_count_cache;
	                        int nnz_offset = h.scan8[4*i8x8];
	                        nnz_base[nnz_offset + 0] += nnz_base[nnz_offset + 1] + nnz_base[nnz_offset + 8] + nnz_base[nnz_offset + 9];
	                    }else{
	                        for(i4x4=0; i4x4<4; i4x4++){
	                            int index= i4x4 + 4*i8x8;

	                            if( decode_residual(h, gb, h.mb, 16*index, index, scan, 0, h.dequant4_coeff[(mb_type &7)!=0 ? 0:3][s.qscale], 0, 16) <0 ){
	                                return -1;
	                            }
	                        }
	                    }
	                }else{
                        int[] nnz_base = h.non_zero_count_cache;
                        int nnz_offset = h.scan8[4*i8x8];
                        nnz_base[nnz_offset + 0] = nnz_base[nnz_offset + 1] = nnz_base[nnz_offset + 8] = nnz_base[nnz_offset + 9] = 0;
	                }
	            }
	        }

	        if((cbp&0x030)!=0){
	        	// Fill 2x4 uint_16t with 0
	        	Arrays.fill(h.mb_chroma_dc[0], (short)0);
	        	Arrays.fill(h.mb_chroma_dc[1], (short)0);
	            //AV_ZERO128(h.mb_chroma_dc);
	            for(chroma_idx=0; chroma_idx<2; chroma_idx++) {
	                if( decode_residual(h, gb, h.mb_chroma_dc[chroma_idx], 0, H264Context.CHROMA_DC_BLOCK_INDEX+chroma_idx, H264Data.chroma_dc_scan, 0, null, 0 , 4) < 0){

	                	// DebugTool.printDebugString("----Error type 16\n");
	                    
	                	return -1;
	                }
	            }
	        }

	        if((cbp&0x020)!=0){
	            for(chroma_idx=0; chroma_idx<2; chroma_idx++){
	                long[] qmul = h.dequant4_coeff[chroma_idx+1+(( (mb_type & 7)!=0 ) ? 0:3)][h.chroma_qp[chroma_idx]];
	                for(i4x4=0; i4x4<4; i4x4++){
	                    int index= 16 + 4*chroma_idx + i4x4;
	                    if( decode_residual(h, gb, h.mb, 16*index, index, scan, 1, qmul, 0, 15) < 0){
	                        return -1;
	                    }
	                }
	            }
	        }else{
		        int[] nnz= h.non_zero_count_cache;
		        nnz[ h.scan8[16]+0 ] = nnz[ h.scan8[16]+1 ] =nnz[ h.scan8[16]+8 ] =nnz[ h.scan8[16]+9 ] =
		        nnz[ h.scan8[20]+0 ] = nnz[ h.scan8[20]+1 ] =nnz[ h.scan8[20]+8 ] =nnz[ h.scan8[20]+9 ] = 0;
	        }
	    }else{
	        int[] nnz= h.non_zero_count_cache;
	        Rectangle.fill_rectangle_unsign(nnz, h.scan8[0], 4, 4, 8, 0, 1);
	        nnz[ h.scan8[16]+0 ] = nnz[ h.scan8[16]+1 ] =nnz[ h.scan8[16]+8 ] =nnz[ h.scan8[16]+9 ] =
	        nnz[ h.scan8[20]+0 ] = nnz[ h.scan8[20]+1 ] =nnz[ h.scan8[20]+8 ] =nnz[ h.scan8[20]+9 ] = 0;
	    }
	    s.current_picture.qscale_table[mb_xy]= s.qscale;
	    h.write_back_non_zero_count();

	    if(h.mb_mbaff != 0){
	        h.ref_count[0] >>= 1;
	        h.ref_count[1] >>= 1;
	    }
	    
        // DebugTool.dumpDebugFrameData(h, "AFTER-ff_h264_decode_mb_cavlc");

	    return 0;
	}
	
}
