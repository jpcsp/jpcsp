package com.twilight.h264.decoder;

public class H264DSPContext {

	public static final int MAX_NEG_CROP = 1024;
	public static final int MAX_NEG_CLIP = 32768;
	public static int[] ff_cropTbl = new int[256 + 2 * MAX_NEG_CROP];
	public static int[] av_clip_pixelTbl = new int[256 + 2 * MAX_NEG_CLIP];

	static {
	    int i;
	    for(i=0;i<256;i++) ff_cropTbl[i + MAX_NEG_CROP] = i;
	    for(i=0;i<MAX_NEG_CROP;i++) {
	        ff_cropTbl[i] = 0;
	        ff_cropTbl[i + MAX_NEG_CROP + 256] = 255;
	    }
	    for(i=0;i<256;i++) av_clip_pixelTbl[i + MAX_NEG_CLIP] = i;
	    for(i=0;i<MAX_NEG_CLIP;i++) {
	    	av_clip_pixelTbl[i] = 0;
	    	av_clip_pixelTbl[i + MAX_NEG_CLIP + 256] = 255;
	    }
	}
	
	public void weight_h264_pixels_c(int W, int H, int[] block, int _block_offset, int stride, int log2_denom, int weight, int offset){ 
	    int y; 
	    int block_offset = _block_offset;
	    offset <<= log2_denom; 
	    if(log2_denom != 0) offset += 1<<(log2_denom-1); 
	    for(y=0; y<H; y++, block_offset += stride){ 
	        block[block_offset + 0] = av_clip_uint8( (block[block_offset + 0]*weight + offset) >> log2_denom );
	        block[block_offset + 1] = av_clip_uint8( (block[block_offset + 1]*weight + offset) >> log2_denom );
	        if(W==2) continue; 
	        block[block_offset + 2] = av_clip_uint8( (block[block_offset + 2]*weight + offset) >> log2_denom );
	        block[block_offset + 3] = av_clip_uint8( (block[block_offset + 3]*weight + offset) >> log2_denom );
	        if(W==4) continue; 
	        block[block_offset + 4] = av_clip_uint8( (block[block_offset + 4]*weight + offset) >> log2_denom );
	        block[block_offset + 5] = av_clip_uint8( (block[block_offset + 5]*weight + offset) >> log2_denom );
	        block[block_offset + 6] = av_clip_uint8( (block[block_offset + 6]*weight + offset) >> log2_denom );
	        block[block_offset + 7] = av_clip_uint8( (block[block_offset + 7]*weight + offset) >> log2_denom );
	        if(W==8) continue; 
	        block[block_offset + 8] = av_clip_uint8( (block[block_offset + 8]*weight + offset) >> log2_denom );
	        block[block_offset + 9] = av_clip_uint8( (block[block_offset + 9]*weight + offset) >> log2_denom );
	        block[block_offset + 10] = av_clip_uint8( (block[block_offset + 10]*weight + offset) >> log2_denom );
	        block[block_offset + 11] = av_clip_uint8( (block[block_offset + 11]*weight + offset) >> log2_denom );
	        block[block_offset + 12] = av_clip_uint8( (block[block_offset + 12]*weight + offset) >> log2_denom );
	        block[block_offset + 13] = av_clip_uint8( (block[block_offset + 13]*weight + offset) >> log2_denom );
	        block[block_offset + 14] = av_clip_uint8( (block[block_offset + 14]*weight + offset) >> log2_denom );
	        block[block_offset + 15] = av_clip_uint8( (block[block_offset + 15]*weight + offset) >> log2_denom );
	    } 
	} 
	
	public void biweight_h264_pixels_c(int W, int H, int[] dst, int _dst_offset, int[] src, int _src_offset, int stride, int log2_denom, int weightd, int weights, int offset){ 
	    int y; 
	    int src_offset = _src_offset;
	    int dst_offset = _dst_offset;
	    offset = ((offset + 1) | 1) << log2_denom; 
	    for(y=0; y<H; y++, dst_offset += stride, src_offset += stride){ 
	        dst[dst_offset + 0] = av_clip_uint8( (src[src_offset + 0]*weights + dst[dst_offset + 0]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 1] = av_clip_uint8( (src[src_offset + 1]*weights + dst[dst_offset + 1]*weightd + offset) >> (log2_denom+1));
	        if(W==2) continue; 
	        dst[dst_offset + 2] = av_clip_uint8( (src[src_offset + 2]*weights + dst[dst_offset + 2]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 3] = av_clip_uint8( (src[src_offset + 3]*weights + dst[dst_offset + 3]*weightd + offset) >> (log2_denom+1));
	        if(W==4) continue; 
	        dst[dst_offset + 4] = av_clip_uint8( (src[src_offset + 4]*weights + dst[dst_offset + 4]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 5] = av_clip_uint8( (src[src_offset + 5]*weights + dst[dst_offset + 5]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 6] = av_clip_uint8( (src[src_offset + 6]*weights + dst[dst_offset + 6]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 7] = av_clip_uint8( (src[src_offset + 7]*weights + dst[dst_offset + 7]*weightd + offset) >> (log2_denom+1));
	        if(W==8) continue; 
	        dst[dst_offset + 8] = av_clip_uint8( (src[src_offset + 8]*weights + dst[dst_offset + 8]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 9] = av_clip_uint8( (src[src_offset + 9]*weights + dst[dst_offset + 9]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 10] = av_clip_uint8( (src[src_offset + 10]*weights + dst[dst_offset + 10]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 11] = av_clip_uint8( (src[src_offset + 11]*weights + dst[dst_offset + 11]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 12] = av_clip_uint8( (src[src_offset + 12]*weights + dst[dst_offset + 12]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 13] = av_clip_uint8( (src[src_offset + 13]*weights + dst[dst_offset + 13]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 14] = av_clip_uint8( (src[src_offset + 14]*weights + dst[dst_offset + 14]*weightd + offset) >> (log2_denom+1));
	        dst[dst_offset + 15] = av_clip_uint8( (src[src_offset + 15]*weights + dst[dst_offset + 15]*weightd + offset) >> (log2_denom+1));
	    } 
	}
	
	public interface IH264WeightFunctionStub {
		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset);
	}

	public interface IH264BiWeightFunctionStub {
		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset);
	}
	
	public H264DSPContext() {
		
	}
	
    /* weighted MC */
    public IH264WeightFunctionStub[] weight_h264_pixels_tab = new IH264WeightFunctionStub[] {
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(16, 16, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(16, 8, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(8, 16, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(8, 8, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(8, 4, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(4, 8, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(4, 4, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(4, 2, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(2, 4, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    	new IH264WeightFunctionStub() {
    		public void h264_weight_func(int []block, int block_offset, int stride, int log2_denom, int weight, int offset) {
    			weight_h264_pixels_c(2, 2, block, block_offset, stride, log2_denom, weight, offset);
    		}
    	},
    };
    
    public IH264BiWeightFunctionStub[] biweight_h264_pixels_tab = new IH264BiWeightFunctionStub[] {
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(16, 16, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(16, 8, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(8, 16, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(8, 8, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(8, 4, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(4, 8, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(4, 4, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(4, 2, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(2, 4, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},
        	new IH264BiWeightFunctionStub() {
        		public void h264_biweight_func(int []dst, int dst_offset, int []src, int src_offset, int stride, int log2_denom, int weightd, int weights, int offset) {
        			biweight_h264_pixels_c(2, 2, dst, dst_offset, src, src_offset, stride, log2_denom, weightd, weights, offset);
        		}
        	},    		
    };

    public static int av_clip(int a, int amin, int amax) {
        if      (a < amin) return amin;
        else if (a > amax) return amax;
        else               return a;
    }
    
    private int av_clip_uint8(int a) {
        if ((a&(~0x000000FF)) != 0) return ((-a)>>31) & 0xFF;
        else           return a;
    }    
    
    /* loop filter */
    private void h264_loop_filter_luma_c(int[] pix, int _pix_offset, int xstride, int ystride, int alpha, int beta, int[] tc0) {
        int i, d;
        int pix_offset = _pix_offset;
        for( i = 0; i < 4; i++ ) {
            if( tc0[i] < 0 ) {
            	pix_offset += 4*ystride;
                continue;
            }
            for( d = 0; d < 4; d++ ) {
                int p0 = pix[pix_offset -1*xstride];
                int p1 = pix[pix_offset -2*xstride];
                int p2 = pix[pix_offset -3*xstride];
                int q0 = pix[pix_offset ];
                int q1 = pix[pix_offset +1*xstride];
                int q2 = pix[pix_offset +2*xstride];
                
                // DebugTool.printDebugString("p={"+p0+","+p1+","+p2+"}, q={"+q0+","+q1+","+q2+"}, alpha="+alpha+", beta="+beta+", pix_offset="+pix_offset+", xstride="+xstride+"\n");

                if( Math.abs( p0 - q0 ) < alpha &&
                		Math.abs( p1 - p0 ) < beta &&
                		Math.abs( q1 - q0 ) < beta ) {

                    int tc = tc0[i];
                    int i_delta;

                    if( Math.abs( p2 - p0 ) < beta ) {
                        if(tc0[i] != 0)
                        pix[pix_offset -2*xstride] = p1 + av_clip( (( p2 + ( ( p0 + q0 + 1 ) >> 1 ) ) >> 1) - p1, -tc0[i], tc0[i] );
                        tc++;
                    }
                    if( Math.abs( q2 - q0 ) < beta ) {
                        if(tc0[i] != 0)
                        pix[pix_offset +   xstride] = q1 + av_clip( (( q2 + ( ( p0 + q0 + 1 ) >> 1 ) ) >> 1) - q1, -tc0[i], tc0[i] );
                        tc++;
                    }

                    i_delta = av_clip( (((q0 - p0 ) << 2) + (p1 - q1) + 4) >> 3, -tc, tc );
                    pix[pix_offset -xstride] = av_clip_uint8( p0 + i_delta );    /* p0' */
                    pix[pix_offset]        = av_clip_uint8( q0 - i_delta );    /* q0' */
                }
                pix_offset += ystride;
            }
        }
    }
    
    public void h264_v_loop_filter_luma(int []pix_base/*align 16*/, int pix_offset, int stride, int alpha, int beta, int []tc0) {
        h264_loop_filter_luma_c(pix_base, pix_offset, stride, 1, alpha, beta, tc0);    	
    }
    
    public void h264_h_loop_filter_luma(int []pix_base/*align 4 */, int pix_offset, int stride, int alpha, int beta, int []tc0) {
        h264_loop_filter_luma_c(pix_base, pix_offset, 1, stride, alpha, beta, tc0);    	
    }
    
    private void h264_loop_filter_luma_intra_c(int[] pix, int _pix_offset, int xstride, int ystride, int alpha, int beta) {
        int d;
        int pix_offset = _pix_offset;
        for( d = 0; d < 16; d++ ) {
             int p2 = pix[pix_offset -3*xstride];
             int p1 = pix[pix_offset -2*xstride];
             int p0 = pix[pix_offset -1*xstride];

             int q0 = pix[pix_offset +0*xstride];
             int q1 = pix[pix_offset +1*xstride];
             int q2 = pix[pix_offset +2*xstride];

            if( Math.abs( p0 - q0 ) < alpha &&
            		Math.abs( p1 - p0 ) < beta &&
            		Math.abs( q1 - q0 ) < beta ) {

                if(Math.abs( p0 - q0 ) < (( alpha >> 2 ) + 2 )){
                    if( Math.abs( p2 - p0 ) < beta)
                    {
                         int p3 = pix[pix_offset -4*xstride];
                        /* p0', p1', p2' */
                        pix[pix_offset -1*xstride] = ( p2 + 2*p1 + 2*p0 + 2*q0 + q1 + 4 ) >> 3;
                        pix[pix_offset -2*xstride] = ( p2 + p1 + p0 + q0 + 2 ) >> 2;
                        pix[pix_offset -3*xstride] = ( 2*p3 + 3*p2 + p1 + p0 + q0 + 4 ) >> 3;
                    } else {
                        /* p0' */
                        pix[pix_offset -1*xstride] = ( 2*p1 + p0 + q1 + 2 ) >> 2;
                    }
                    if( Math.abs( q2 - q0 ) < beta)
                    {
                         int q3 = pix[pix_offset +3*xstride];
                        /* q0', q1', q2' */
                        pix[pix_offset +0*xstride] = ( p1 + 2*p0 + 2*q0 + 2*q1 + q2 + 4 ) >> 3;
                        pix[pix_offset +1*xstride] = ( p0 + q0 + q1 + q2 + 2 ) >> 2;
                        pix[pix_offset +2*xstride] = ( 2*q3 + 3*q2 + q1 + q0 + p0 + 4 ) >> 3;
                    } else {
                        /* q0' */
                        pix[pix_offset +0*xstride] = ( 2*q1 + q0 + p1 + 2 ) >> 2;
                    }
                }else{
                    /* p0', q0' */
                    pix[pix_offset -1*xstride] = ( 2*p1 + p0 + q1 + 2 ) >> 2;
                    pix[pix_offset +0*xstride] = ( 2*q1 + q0 + p1 + 2 ) >> 2;
                }
            }
            pix_offset += ystride;
        }
    }
    
    /* v/h_loop_filter_luma_intra: align 16 */
    public void h264_v_loop_filter_luma_intra(int[] pix, int pix_offset, int stride, int alpha, int beta) {
        h264_loop_filter_luma_intra_c(pix, pix_offset, stride, 1, alpha, beta);    	
    }
    
    public void h264_h_loop_filter_luma_intra(int[] pix, int pix_offset, int stride, int alpha, int beta) {
        h264_loop_filter_luma_intra_c(pix, pix_offset, 1, stride, alpha, beta);
    }
    
    private void h264_loop_filter_chroma_c(int[] pix, int _pix_offset, int xstride, int ystride, int alpha, int beta, int[] tc0) {
        int i, d;
        int pix_offset = _pix_offset;
        for( i = 0; i < 4; i++ ) {
            int tc = tc0[i];
            if( tc <= 0 ) {
            	pix_offset += 2*ystride;
                continue;
            }
            for( d = 0; d < 2; d++ ) {
                int p0 = pix[pix_offset -1*xstride];
                int p1 = pix[pix_offset -2*xstride];
                int q0 = pix[pix_offset];
                int q1 = pix[pix_offset +1*xstride];

                if( Math.abs( p0 - q0 ) < alpha &&
                		Math.abs( p1 - p0 ) < beta &&
                		Math.abs( q1 - q0 ) < beta ) {

                    int delta = av_clip( (((q0 - p0 ) << 2) + (p1 - q1) + 4) >> 3, -tc, tc );

                    pix[pix_offset -xstride] = av_clip_uint8( p0 + delta );    /* p0' */
                    pix[pix_offset]        = av_clip_uint8( q0 - delta );    /* q0' */
                }
                pix_offset += ystride;
            }
        }
    }
    
    public void h264_v_loop_filter_chroma(int[] pix/*align 8*/, int pix_offset, int stride, int alpha, int beta, int[] tc0) {
        h264_loop_filter_chroma_c(pix, pix_offset, stride, 1, alpha, beta, tc0);    	
    }
    
    public void h264_h_loop_filter_chroma(int[] pix/*align 4*/, int pix_offset, int stride, int alpha, int beta, int[] tc0) {
        h264_loop_filter_chroma_c(pix, pix_offset, 1, stride, alpha, beta, tc0);    	
    }
    
    private void h264_loop_filter_chroma_intra_c(int[] pix, int _pix_offset, int xstride, int ystride, int alpha, int beta)
    {
        int d;
        int pix_offset = _pix_offset;
        for( d = 0; d < 8; d++ ) {
             int p0 = pix[pix_offset -1*xstride];
             int p1 = pix[pix_offset -2*xstride];
             int q0 = pix[pix_offset];
             int q1 = pix[pix_offset +1*xstride];

            if( Math.abs( p0 - q0 ) < alpha &&
            		Math.abs( p1 - p0 ) < beta &&
            		Math.abs( q1 - q0 ) < beta ) {

                pix[pix_offset -xstride] = ( 2*p1 + p0 + q1 + 2 ) >> 2;   /* p0' */
                pix[pix_offset]        = ( 2*q1 + q0 + p1 + 2 ) >> 2;   /* q0' */
            }
            pix_offset += ystride;
        }
    }
        
    public void h264_v_loop_filter_chroma_intra(int[] pix/*align 8*/, int pix_offset, int stride, int alpha, int beta) {
        h264_loop_filter_chroma_intra_c(pix, pix_offset, stride, 1, alpha, beta);    	
    }
    
    public void h264_h_loop_filter_chroma_intra(int[] pix/*align 8*/, int pix_offset, int stride, int alpha, int beta) {
        h264_loop_filter_chroma_intra_c(pix, pix_offset, 1, stride, alpha, beta);    	
    }
    
    // h264_loop_filter_strength: simd only. the C version is inlined in h264.c
    public void h264_loop_filter_strength(int[][][] bS, int[] nnz, int[][] ref, int[][][] mv,
                                      int bidir, int edges, int step, int mask_mv0, int mask_mv1, int field) {
    	
    }

    /* IDCT */
    public void h264_idct_add(int[] dst/*align 4*/,int offset, short[] block/*align 16*/, int block_offset, int stride) {
    	idct_internal(dst, offset, block, block_offset, stride, 4, 6, 1);
    }
    
    public void idct_internal(int[] dst/*align 4*/,int offset, short[] block/*align 16*/, int block_offset, int stride, int block_stride, int shift, int add){
        int i;
        final int cm_pos = MAX_NEG_CLIP;

        block[block_offset + 0] += 1<<(shift-1);

        for(i=0; i<4; i++){
             int z0=  block[block_offset + i + block_stride*0]     +  block[block_offset + i + block_stride*2];
             int z1=  block[block_offset + i + block_stride*0]     -  block[block_offset + i + block_stride*2];
             int z2= (block[block_offset + i + block_stride*1]>>1) -  block[block_offset + i + block_stride*3];
             int z3=  block[block_offset + i + block_stride*1]     + (block[block_offset + i + block_stride*3]>>1);

            block[block_offset + i + block_stride*0]= (short)(z0 + z3);
            block[block_offset + i + block_stride*1]= (short)(z1 + z2);
            block[block_offset + i + block_stride*2]= (short)(z1 - z2);
            block[block_offset + i + block_stride*3]= (short)(z0 - z3);
        }

        for(i=0; i<4; i++){
             int z0=  block[block_offset + 0 + block_stride*i]     +  block[block_offset + 2 + block_stride*i];
             int z1=  block[block_offset + 0 + block_stride*i]     -  block[block_offset + 2 + block_stride*i];
             int z2= (block[block_offset + 1 + block_stride*i]>>1) -  block[block_offset + 3 + block_stride*i];
             int z3=  block[block_offset + 1 + block_stride*i]     + (block[block_offset + 3 + block_stride*i]>>1);

            dst[offset + i + 0*stride]= av_clip_pixelTbl[cm_pos + add*dst[offset + i + 0*stride] + ((z0 + z3) >> shift) ];
            dst[offset + i + 1*stride]= av_clip_pixelTbl[cm_pos + add*dst[offset + i + 1*stride] + ((z1 + z2) >> shift) ];
            dst[offset + i + 2*stride]= av_clip_pixelTbl[cm_pos + add*dst[offset + i + 2*stride] + ((z1 - z2) >> shift) ];
            dst[offset + i + 3*stride]= av_clip_pixelTbl[cm_pos + add*dst[offset + i + 3*stride] + ((z0 - z3) >> shift) ];
        }
    }
    
    
    public void h264_idct8_add(int[] dst/*align 8*/, int offset, short[] block/*align 16*/, int block_offset, int stride) {
        int i;
        final int cm_pos = MAX_NEG_CLIP;

        block[block_offset + 0] += 32;

        for( i = 0; i < 8; i++ )
        {
			 int a0 =  block[block_offset + i+0*8] + block[block_offset + i+4*8];
			 int a2 =  block[block_offset + i+0*8] - block[block_offset + i+4*8];
			 int a4 = (block[block_offset + i+2*8]>>1) - block[block_offset + i+6*8];
			 int a6 = (block[block_offset + i+6*8]>>1) + block[block_offset + i+2*8];
			
			 int b0 = a0 + a6;
			 int b2 = a2 + a4;
			 int b4 = a2 - a4;
			 int b6 = a0 - a6;
			
			 int a1 = -block[block_offset + i+3*8] + block[block_offset + i+5*8] - block[block_offset + i+7*8] - (block[block_offset + i+7*8]>>1);
			 int a3 =  block[block_offset + i+1*8] + block[block_offset + i+7*8] - block[block_offset + i+3*8] - (block[block_offset + i+3*8]>>1);
			 int a5 = -block[block_offset + i+1*8] + block[block_offset + i+7*8] + block[block_offset + i+5*8] + (block[block_offset + i+5*8]>>1);
			 int a7 =  block[block_offset + i+3*8] + block[block_offset + i+5*8] + block[block_offset + i+1*8] + (block[block_offset + i+1*8]>>1);
			
			 int b1 = (a7>>2) + a1;
			 int b3 =  a3 + (a5>>2);
			 int b5 = (a3>>2) - a5;
			 int b7 =  a7 - (a1>>2);

	        block[block_offset + i+0*8] = (short)(b0 + b7);
	        block[block_offset + i+7*8] = (short)(b0 - b7);
	        block[block_offset + i+1*8] = (short)(b2 + b5);
	        block[block_offset + i+6*8] = (short)(b2 - b5);
	        block[block_offset + i+2*8] = (short)(b4 + b3);
	        block[block_offset + i+5*8] = (short)(b4 - b3);
	        block[block_offset + i+3*8] = (short)(b6 + b1);
	        block[block_offset + i+4*8] = (short)(b6 - b1);
        }
        for( i = 0; i < 8; i++ )
        {
            int a0 =  block[block_offset + 0+i*8] + block[block_offset + 4+i*8];
             int a2 =  block[block_offset + 0+i*8] - block[block_offset + 4+i*8];
             int a4 = (block[block_offset + 2+i*8]>>1) - block[block_offset + 6+i*8];
             int a6 = (block[block_offset + 6+i*8]>>1) + block[block_offset + 2+i*8];

             int b0 = a0 + a6;
             int b2 = a2 + a4;
             int b4 = a2 - a4;
             int b6 = a0 - a6;

             int a1 = -block[block_offset + 3+i*8] + block[block_offset + 5+i*8] - block[block_offset + 7+i*8] - (block[block_offset + 7+i*8]>>1);
             int a3 =  block[block_offset + 1+i*8] + block[block_offset + 7+i*8] - block[block_offset + 3+i*8] - (block[block_offset + 3+i*8]>>1);
             int a5 = -block[block_offset + 1+i*8] + block[block_offset + 7+i*8] + block[block_offset + 5+i*8] + (block[block_offset + 5+i*8]>>1);
             int a7 =  block[block_offset + 3+i*8] + block[block_offset + 5+i*8] + block[block_offset + 1+i*8] + (block[block_offset + 1+i*8]>>1);

             int b1 = (a7>>2) + a1;
             int b3 =  a3 + (a5>>2);
             int b5 = (a3>>2) - a5;
             int b7 =  a7 - (a1>>2);

            dst[offset + i + 0*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 0*stride] + ((b0 + b7) >> 6) ];
            dst[offset + i + 1*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 1*stride] + ((b2 + b5) >> 6) ];
            dst[offset + i + 2*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 2*stride] + ((b4 + b3) >> 6) ];
            dst[offset + i + 3*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 3*stride] + ((b6 + b1) >> 6) ];
            dst[offset + i + 4*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 4*stride] + ((b6 - b1) >> 6) ];
            dst[offset + i + 5*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 5*stride] + ((b4 - b3) >> 6) ];
            dst[offset + i + 6*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 6*stride] + ((b2 - b5) >> 6) ];
            dst[offset + i + 7*stride] = av_clip_pixelTbl[cm_pos + dst[offset + i + 7*stride] + ((b0 - b7) >> 6) ];
        }    	
    }
    
    public void h264_idct_dc_add(int[] dst/*align 4*/, int offset, short[] block/*align 16*/, int block_offset, int stride) {
        int i, j;
        int dc = (block[block_offset + 0] + 32) >> 6;
        int cm_pos = MAX_NEG_CROP + dc;
        int dst_pos = 0;
        for( j = 0; j < 4; j++ )
        {
            for( i = 0; i < 4; i++ )
                dst[offset + dst_pos + i] = ff_cropTbl[cm_pos + dst[offset + dst_pos + i] ];
            dst_pos += stride;
        }    	
    }
    
    public void h264_idct8_dc_add(int[] dst/*align 8*/, int offset, short[] block/*align 16*/, int block_offset, int stride) {
        int i, j;
        int dc = (block[block_offset + 0] + 32) >> 6;
        int cm_pos = MAX_NEG_CROP + dc;
        int dst_pos = 0;
        for( j = 0; j < 8; j++ )
        {
            for( i = 0; i < 8; i++ )
                dst[offset + dst_pos + i] = ff_cropTbl[cm_pos + dst[offset + dst_pos + i] ];
            dst_pos += stride;
        }    	
    }

    public void h264_dct(short block[][]) {
    	
    }
    
    public static final short[] scan8 = {
    	 4+1*8, 5+1*8, 4+2*8, 5+2*8,
    	 6+1*8, 7+1*8, 6+2*8, 7+2*8,
    	 4+3*8, 5+3*8, 4+4*8, 5+4*8,
    	 6+3*8, 7+3*8, 6+4*8, 7+4*8,
    	 1+1*8, 2+1*8,
    	 1+2*8, 2+2*8,
    	 1+4*8, 2+4*8,
    	 1+5*8, 2+5*8,
    	};
    
    public void h264_idct_add16(int[] dst/*align 16*/,int dst_offset, int[] blockoffset, int blockoffset_offset, short[] block/*align 16*/, int block_offset, int stride, int[] nnzc) {
        int i;
        for(i=0; i<16; i++){
            int nnz = nnzc[ scan8[i] ];
            if(nnz != 0){
                if(nnz==1 && block[i*16] != 0) h264_idct_dc_add(dst, dst_offset + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride);
                else                      idct_internal        (dst, dst_offset + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride, 4, 6, 1);
            }
        }    	
    }
    
    public void h264_idct8_add4(int[] dst/*align 16*/,int dst_offset, int[]blockoffset, int blockoffset_offset, short[] block/*align 16*/, int block_offset, int stride, int[] nnzc) {
        int i;
        for(i=0; i<16; i+=4){
            int nnz = nnzc[ scan8[i] ];
            if(nnz != 0){
                if(nnz==1 && block[i*16] != 0) h264_idct8_dc_add(dst, dst_offset + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride);
                else                      h264_idct8_add   (dst, dst_offset + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride);
            }
        }
    	
    }
    
    public void h264_idct_add8(int[][] dst/*align 16*/,int[] dst_offset, int[] blockoffset, int blockoffset_offset, short[] block/*align 16*/, int block_offset, int stride, int[] nnzc) {
        int i;
        for(i=16; i<16+8; i++){
            if(nnzc[ scan8[i] ] != 0)
            	idct_internal   (dst[(i&4)>>2], dst_offset[(i&4)>>2] + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride,4,6,1);
            else if(block[i*16] != 0)
                h264_idct_dc_add(dst[(i&4)>>2], dst_offset[(i&4)>>2] + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride);
        }    	
    }
    
    public void h264_idct_add16intra(int[] dst/*align 16*/,int dst_offset, int[] blockoffset, int blockoffset_offset, short[] block/*align 16*/, int block_offset, int stride, int[] nnzc) {
        int i;
        for(i=0; i<16; i++){
            if(nnzc[ scan8[i] ]!=0) idct_internal        (dst, dst_offset + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride, 4, 6, 1);
            else if(block[i*16]!=0) h264_idct_dc_add(dst, dst_offset + blockoffset[blockoffset_offset + i], block, block_offset + i*16, stride);
        }    	
    }
    
    public void h264_luma_dc_dequant_idct(short[] output, int output_offset, short[] input/*align 16*/, int input_offset, int qmul) {
        int i;
        int[] temp = new int[16];
        final short[] x_offset={0, 2*16, 8*16, 10*16};

        for(i=0; i<4; i++){
            int z0= input[input_offset + 4*i+0] + input[input_offset + 4*i+1];
            int z1= input[input_offset + 4*i+0] - input[input_offset + 4*i+1];
            int z2= input[input_offset + 4*i+2] - input[input_offset + 4*i+3];
            int z3= input[input_offset + 4*i+2] + input[input_offset + 4*i+3];

            temp[4*i+0]= z0+z3;
            temp[4*i+1]= z0-z3;
            temp[4*i+2]= z1-z2;
            temp[4*i+3]= z1+z2;
        }

        for(i=0; i<4; i++){
            int offset= x_offset[i];
            int z0= temp[4*0+i] + temp[4*2+i];
            int z1= temp[4*0+i] - temp[4*2+i];
            int z2= temp[4*1+i] - temp[4*3+i];
            int z3= temp[4*1+i] + temp[4*3+i];

            output[output_offset + 16* 0+offset]= (short)((((z0 + z3)*qmul + 128 ) >> 8));
            output[output_offset + 16* 1+offset]= (short)((((z1 + z2)*qmul + 128 ) >> 8));
            output[output_offset + 16* 4+offset]= (short)((((z1 - z2)*qmul + 128 ) >> 8));
            output[output_offset + 16* 5+offset]= (short)((((z0 - z3)*qmul + 128 ) >> 8));
        }    	
    }
    
    public void h264_chroma_dc_dequant_idct(short[] output, int output_offset, short[] input/*align 16*/, int input_offset, int qmul) {
        int stride= 16*2;
        int xStride= 16;
        int a,b,c,d,e;

        a= input[input_offset + 0];
        b= input[input_offset + 1];
        c= input[input_offset + 2];
        d= input[input_offset + 3];

        e= a-b;
        a= a+b;
        b= c-d;
        c= c+d;

        output[output_offset + stride*0 + xStride*0]= (short)(((a+c)*qmul) >> 7);
        output[output_offset + stride*0 + xStride*1]= (short)(((e+b)*qmul) >> 7);
        output[output_offset + stride*1 + xStride*0]= (short)(((a-c)*qmul) >> 7);
        output[output_offset + stride*1 + xStride*1]= (short)(((e-b)*qmul) >> 7);    	
    }
    
    public void ff_h264dsp_init() { /*Do nothing.*/ }

}
