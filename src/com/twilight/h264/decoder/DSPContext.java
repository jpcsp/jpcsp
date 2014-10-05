package com.twilight.h264.decoder;

import com.twilight.h264.util.*;

public class DSPContext {
	
	public static final int FF_NO_IDCT_PERM = 1; 
	
    /**
     * idct input permutation.
     * several optimized IDCTs need a permutated input (relative to the normal order of the reference
     * IDCT)
     * this permutation must be performed before the idct_put/add, note, normally this can be merged
     * with the zigzag/alternate scan<br>
     * an example to avoid confusion:
     * - (->decode coeffs -> zigzag reorder -> dequant -> reference idct ->...)
     * - (x -> referece dct -> reference idct -> x)
     * - (x -> referece dct -> simple_mmx_perm = idct_permutation -> simple_idct_mmx -> x)
     * - (->decode coeffs -> zigzag reorder -> simple_mmx_perm -> dequant -> simple_idct_mmx ->...)
     */
    /*uint8_t*/ public int[] idct_permutation = new int[64];
    public int idct_permutation_type;
	
	public static Ih264_qpel_mc_func[][] put_h264_qpel_pixels_tab = new Ih264_qpel_mc_func[4][16];
	public static Ih264_qpel_mc_func[][] avg_h264_qpel_pixels_tab = new Ih264_qpel_mc_func[4][16];

	/* draw the edges of width 'w' of an image of size width, height */
	//FIXME check that this is ok for mpeg4 interlaced
	public void draw_edges(/*uint8_t *buf,*/int[] buf_base, int buf_offset, int wrap, int width, int height, int w)
	{
	    //uint8_t *ptr, *last_line;
	    int[] ptr_base;
	    int[] last_line_base;
	    int ptr_offset;
	    int last_line_offset;

		int i;

	    last_line_base = buf_base;
	    last_line_offset = buf_offset + (height - 1) * wrap;
	    for(i=0;i<w;i++) {
	        /* top and bottom */
	    	System.arraycopy(buf_base, buf_offset, buf_base, buf_offset - (i + 1) * wrap, width);
	        //memcpy(buf - (i + 1) * wrap, buf, width);
	    	System.arraycopy(last_line_base, last_line_offset, last_line_base, last_line_offset + (i + 1) * wrap, width);
	        //memcpy(last_line + (i + 1) * wrap, last_line, width);
	    }
	    /* left and right */
	    ptr_base = buf_base;
	    ptr_offset = buf_offset;
	    for(i=0;i<height;i++) {
	    	Arrays.fill(ptr_base, ptr_offset -w, ptr_offset -w +w, ptr_base[ptr_offset]);
	        //memset(ptr - w, ptr[0], w);
	    	Arrays.fill(ptr_base, ptr_offset +width, ptr_offset +width +w, ptr_base[ptr_offset + width-1]);
	        //memset(ptr + width, ptr[width-1], w);
	        ptr_offset += wrap;
	    }
	    /* corners */
	    for(i=0;i<w;i++) {
	    	Arrays.fill(buf_base, buf_offset - (i + 1) * wrap - w, buf_offset - (i + 1) * wrap - w +w, buf_base[buf_offset]);
	        //memset(buf - (i + 1) * wrap - w, buf[0], w); /* top left */
	    	Arrays.fill(buf_base, buf_offset - (i + 1) * wrap + width, buf_offset - (i + 1) * wrap + width +w, buf_base[buf_offset + width-1]);
	    	//memset(buf - (i + 1) * wrap + width, buf[width-1], w); /* top right */
	    	Arrays.fill(last_line_base, last_line_offset + (i + 1) * wrap - w, last_line_offset + (i + 1) * wrap - w +w, last_line_base[last_line_offset]);
	        //memset(last_line + (i + 1) * wrap - w, last_line[0], w); /* top left */
	    	Arrays.fill(last_line_base, last_line_offset + (i + 1) * wrap + width, last_line_offset + (i + 1) * wrap + width +w, last_line_base[last_line_offset + width-1]);
	        //memset(last_line + (i + 1) * wrap + width, last_line[width-1], w); /* top right */
	    }
	}
	
	/**
	 * Copy a rectangular area of samples to a temporary buffer and replicate the border samples.
	 * @param buf destination buffer
	 * @param src source buffer
	 * @param linesize number of bytes between 2 vertically adjacent samples in both the source and destination buffers
	 * @param block_w width of block
	 * @param block_h height of block
	 * @param src_x x coordinate of the top left sample of the block in the source buffer
	 * @param src_y y coordinate of the top left sample of the block in the source buffer
	 * @param w width of the source buffer
	 * @param h height of the source buffer
	 */
	//void ff_emulated_edge_mc(uint8_t *buf, const uint8_t *src, int linesize, int block_w, int block_h,
	//                                    int src_x, int src_y, int w, int h){
	public void ff_emulated_edge_mc(int[] buf_base, int buf_offset, int[] src_base, int src_offset, int linesize, int block_w, int block_h,
            int src_x, int src_y, int w, int h){
	    int x, y;
	    int start_y, start_x, end_y, end_x;

	    if(src_y>= h){
	        src_offset += (h-1-src_y)*linesize;
	        src_y=h-1;
	    }else if(src_y<=-block_h){
	        src_offset += (1-block_h-src_y)*linesize;
	        src_y=1-block_h;
	    }
	    if(src_x>= w){
	        src_offset += (w-1-src_x);
	        src_x=w-1;
	    }else if(src_x<=-block_w){
	        src_offset += (1-block_w-src_x);
	        src_x=1-block_w;
	    }

	    start_y= Math.max(0, -src_y);
	    start_x= Math.max(0, -src_x);
	    end_y= Math.min(block_h, h-src_y);
	    end_x= Math.min(block_w, w-src_x);
	    
	    // DebugTool.printDebugString("start_x="+start_x+", start_y="+start_y+", end_x="+end_x+", end_y="+end_y+"\n");

	    // copy existing part
	    for(y=start_y; y<end_y; y++){
	        for(x=start_x; x<end_x; x++){
	            buf_base[buf_offset + x + y*linesize]= src_base[src_offset + x + y*linesize];
	        }
	    }

	    //top
	    for(y=0; y<start_y; y++){
	        for(x=start_x; x<end_x; x++){
	            buf_base[buf_offset + x + y*linesize]= buf_base[buf_offset + x + start_y*linesize];
	        }
	    }

	    //bottom
	    for(y=end_y; y<block_h; y++){
	        for(x=start_x; x<end_x; x++){
	            buf_base[buf_offset + x + y*linesize]= buf_base[buf_offset + x + (end_y-1)*linesize];
	        }
	    }

	    for(y=0; y<block_h; y++){
	       //left
	        for(x=0; x<start_x; x++){
	            buf_base[buf_offset + x + y*linesize]= buf_base[buf_offset + start_x + y*linesize];
	        }

	       //right
	        for(x=end_x; x<block_w; x++){
	            buf_base[buf_offset + x + y*linesize]= buf_base[buf_offset + end_x - 1 + y*linesize];
	        }
	    }
	}
	
//	public void add_pixels8(uint8_t *restrict pixels, DCTELEM *block, int line_size)
	public void add_pixels8(int[] pixels, int pixel_offset, short[] block, int block_offset, int line_size)
	{
	    int i;
	    for(i=0;i<8;i++) {
	        pixels[pixel_offset + 0] += block[block_offset + 0];
	        pixels[pixel_offset + 1] += block[block_offset + 1];
	        pixels[pixel_offset + 2] += block[block_offset + 2];
	        pixels[pixel_offset + 3] += block[block_offset + 3];
	        pixels[pixel_offset + 4] += block[block_offset + 4];
	        pixels[pixel_offset + 5] += block[block_offset + 5];
	        pixels[pixel_offset + 6] += block[block_offset + 6];
	        pixels[pixel_offset + 7] += block[block_offset + 7];
	        pixel_offset += line_size;
	        block_offset += 8;
	    }
	}

//	public static void add_pixels4(uint8_t *restrict pixels, DCTELEM *block, int line_size)
	public void add_pixels4(int[] pixels, int pixel_offset, short[] block, int block_offset, int line_size)
	{
	    int i;
	    for(i=0;i<4;i++) {
	        pixels[pixel_offset + 0] += block[block_offset + 0];
	        pixels[pixel_offset + 1] += block[block_offset + 1];
	        pixels[pixel_offset + 2] += block[block_offset + 2];
	        pixels[pixel_offset + 3] += block[block_offset + 3];
	        pixel_offset += line_size;
	        block_offset += 4;
	    }
	}	
	
	public void clear_block(short[] block)
	{
		Arrays.fill(block,0,64,(short)0);
	    //memset(block, 0, sizeof(DCTELEM)*64);
	}
	
	/**
	 * memset(blocks, 0, sizeof(DCTELEM)*6*64)
	 */
	public void clear_blocks(short[][]blocks, int first_block_index)
	{
		for(int i=0;i<6;i++)
			Arrays.fill(blocks[first_block_index],0,64,(short)0);
	    //memset(blocks, 0, sizeof(DCTELEM)*6*64);
	}

	public void clear_blocks(short[] blocks)
	{
		Arrays.fill(blocks,0,6*64,(short)0);
	    //memset(blocks, 0, sizeof(DCTELEM)*6*64);
	}

	/**
     * h264 Qpel MC
     */
	public interface Ih264_qpel_mc_func {
		//public void h264_qpel_mc_func(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int stride);
		public void h264_qpel_mc_func(
				int[] dst_base/*align 8*/, int dst_offset,
				int[] src_base/*align 1*/, int src_offset, 
				int stride);
	}
	
    /**
     * h264 Chroma MC
     */
	public interface Ih264_chroma_mc_func {
		//public void h264_chroma_mc_func(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int srcStride, int h, int x, int y);
		public void h264_chroma_mc_func(
				int[] dst_base/*align 8*/, int dst_offset,
				int[] src_base/*align 1*/, int src_offset, 
				int srcStride, int h, int x, int y);
	}
	
	private static int OP_AVG(int a, int b) { return (((a)+(((b) + 32)>>6)+1)>>1); }
	private static int OP_PUT(int a, int b) { return (((b) + 32)>>6); }

//	static void OPNAME ## h264_chroma_mc2_c(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int stride, int h, int x, int y){
	public void put_h264_chroma_mc2_c(int[] dst_base/*align 8*/, int dst_offset, 
			int[] src_base/*align 1*/, int src_offset, int stride, int h, int x, int y){
	    int A=(8-x)*(8-y);
	    int B=(  x)*(8-y);
	    int C=(8-x)*(  y);
	    int D=(  x)*(  y);
	    int i;
	    
	    ////assert(x<8 && y<8 && x>=0 && y>=0);
	
	    if(D!=0){
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_PUT(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + B*src_base[src_offset + 1] + C*src_base[src_offset + stride+0] + D*src_base[src_offset + stride+1]));
	        	dst_base[dst_offset + 1] = OP_PUT(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + B*src_base[src_offset + 2] + C*src_base[src_offset + stride+1] + D*src_base[src_offset + stride+2]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }else{
	        int E= B+C;
	        int step= (C!=0 ? stride : 1);
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_PUT(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + E*src_base[src_offset + step+0]));
	        	dst_base[dst_offset + 1] = OP_PUT(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + E*src_base[src_offset + step+1]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }
	}
	
//	static void OPNAME ## h264_chroma_mc4_c(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int stride, int h, int x, int y){
	public void put_h264_chroma_mc4_c(int[] dst_base/*align 8*/, int dst_offset, 
			int[] src_base/*align 1*/, int src_offset, int stride, int h, int x, int y){
	    int A=(8-x)*(8-y);
	    int B=(  x)*(8-y);
	    int C=(8-x)*(  y);
	    int D=(  x)*(  y);
	    int i;
	    
	    ////assert(x<8 && y<8 && x>=0 && y>=0);
	
	    if(D!=0){
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_PUT(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + B*src_base[src_offset + 1] + C*src_base[src_offset + stride+0] + D*src_base[src_offset + stride+1]));
	        	dst_base[dst_offset + 1] = OP_PUT(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + B*src_base[src_offset + 2] + C*src_base[src_offset + stride+1] + D*src_base[src_offset + stride+2]));
	        	dst_base[dst_offset + 2] = OP_PUT(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + B*src_base[src_offset + 3] + C*src_base[src_offset + stride+2] + D*src_base[src_offset + stride+3]));
	        	dst_base[dst_offset + 3] = OP_PUT(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + B*src_base[src_offset + 4] + C*src_base[src_offset + stride+3] + D*src_base[src_offset + stride+4]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }else{
	        int E= B+C;
	        int step= (C!=0 ? stride : 1);
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_PUT(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + E*src_base[src_offset + step+0]));
	        	dst_base[dst_offset + 1] = OP_PUT(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + E*src_base[src_offset + step+1]));
	        	dst_base[dst_offset + 2] = OP_PUT(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + E*src_base[src_offset + step+2]));
	        	dst_base[dst_offset + 3] = OP_PUT(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + E*src_base[src_offset + step+3]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }
	}
	
//	public void put_h264_chroma_mc8_c(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int stride, int h, int x, int y){\
	public void put_h264_chroma_mc8_c(int[] dst_base/*align 8*/, int dst_offset, 
			int[] src_base/*align 1*/, int src_offset, int stride, int h, int x, int y){
	    int A=(8-x)*(8-y);
	    int B=(  x)*(8-y);
	    int C=(8-x)*(  y);
	    int D=(  x)*(  y);
	    int i;	    
	    ////assert(x<8 && y<8 && x>=0 && y>=0);\	
	    if(D!=0){
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_PUT(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + B*src_base[src_offset + 1] + C*src_base[src_offset + stride+0] + D*src_base[src_offset + stride+1]));
	        	dst_base[dst_offset + 1] = OP_PUT(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + B*src_base[src_offset + 2] + C*src_base[src_offset + stride+1] + D*src_base[src_offset + stride+2]));
	        	dst_base[dst_offset + 2] = OP_PUT(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + B*src_base[src_offset + 3] + C*src_base[src_offset + stride+2] + D*src_base[src_offset + stride+3]));
	        	dst_base[dst_offset + 3] = OP_PUT(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + B*src_base[src_offset + 4] + C*src_base[src_offset + stride+3] + D*src_base[src_offset + stride+4]));
	        	dst_base[dst_offset + 4] = OP_PUT(dst_base[dst_offset + 4], (A*src_base[src_offset + 4] + B*src_base[src_offset + 5] + C*src_base[src_offset + stride+4] + D*src_base[src_offset + stride+5]));
	        	dst_base[dst_offset + 5] = OP_PUT(dst_base[dst_offset + 5], (A*src_base[src_offset + 5] + B*src_base[src_offset + 6] + C*src_base[src_offset + stride+5] + D*src_base[src_offset + stride+6]));
	        	dst_base[dst_offset + 6] = OP_PUT(dst_base[dst_offset + 6], (A*src_base[src_offset + 6] + B*src_base[src_offset + 7] + C*src_base[src_offset + stride+6] + D*src_base[src_offset + stride+7]));
	        	dst_base[dst_offset + 7] = OP_PUT(dst_base[dst_offset + 7], (A*src_base[src_offset + 7] + B*src_base[src_offset + 8] + C*src_base[src_offset + stride+7] + D*src_base[src_offset + stride+8]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }else{
	        int E= B+C;	      
	        int step= (C!=0 ? stride : 1);
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_PUT(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + E*src_base[src_offset + step+0]));
	        	dst_base[dst_offset + 1] = OP_PUT(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + E*src_base[src_offset + step+1]));
	        	dst_base[dst_offset + 2] = OP_PUT(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + E*src_base[src_offset + step+2]));
	        	dst_base[dst_offset + 3] = OP_PUT(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + E*src_base[src_offset + step+3]));
	        	dst_base[dst_offset + 4] = OP_PUT(dst_base[dst_offset + 4], (A*src_base[src_offset + 4] + E*src_base[src_offset + step+4]));
	        	dst_base[dst_offset + 5] = OP_PUT(dst_base[dst_offset + 5], (A*src_base[src_offset + 5] + E*src_base[src_offset + step+5]));
	        	dst_base[dst_offset + 6] = OP_PUT(dst_base[dst_offset + 6], (A*src_base[src_offset + 6] + E*src_base[src_offset + step+6]));
	        	dst_base[dst_offset + 7] = OP_PUT(dst_base[dst_offset + 7], (A*src_base[src_offset + 7] + E*src_base[src_offset + step+7]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }
	}

//	static void OPNAME ## h264_chroma_mc2_c(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int stride, int h, int x, int y){
	public void avg_h264_chroma_mc2_c(int[] dst_base/*align 8*/, int dst_offset, 
			int[] src_base/*align 1*/, int src_offset, int stride, int h, int x, int y){
	    int A=(8-x)*(8-y);
	    int B=(  x)*(8-y);
	    int C=(8-x)*(  y);
	    int D=(  x)*(  y);
	    int i;
	    
	    ////assert(x<8 && y<8 && x>=0 && y>=0);
	
	    if(D!=0){
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_AVG(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + B*src_base[src_offset + 1] + C*src_base[src_offset + stride+0] + D*src_base[src_offset + stride+1]));
	        	dst_base[dst_offset + 1] = OP_AVG(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + B*src_base[src_offset + 2] + C*src_base[src_offset + stride+1] + D*src_base[src_offset + stride+2]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }else{
	        int E= B+C;
	        int step= (C!=0 ? stride : 1);
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_AVG(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + E*src_base[src_offset + step+0]));
	        	dst_base[dst_offset + 1] = OP_AVG(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + E*src_base[src_offset + step+1]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }
	}
	
//	static void OPNAME ## h264_chroma_mc4_c(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int stride, int h, int x, int y){
	public void avg_h264_chroma_mc4_c(int[] dst_base/*align 8*/, int dst_offset, 
			int[] src_base/*align 1*/, int src_offset, int stride, int h, int x, int y){
	    int A=(8-x)*(8-y);
	    int B=(  x)*(8-y);
	    int C=(8-x)*(  y);
	    int D=(  x)*(  y);
	    int i;
	    
	    ////assert(x<8 && y<8 && x>=0 && y>=0);
	
	    if(D!=0){
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_AVG(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + B*src_base[src_offset + 1] + C*src_base[src_offset + stride+0] + D*src_base[src_offset + stride+1]));
	        	dst_base[dst_offset + 1] = OP_AVG(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + B*src_base[src_offset + 2] + C*src_base[src_offset + stride+1] + D*src_base[src_offset + stride+2]));
	        	dst_base[dst_offset + 2] = OP_AVG(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + B*src_base[src_offset + 3] + C*src_base[src_offset + stride+2] + D*src_base[src_offset + stride+3]));
	        	dst_base[dst_offset + 3] = OP_AVG(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + B*src_base[src_offset + 4] + C*src_base[src_offset + stride+3] + D*src_base[src_offset + stride+4]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }else{
	        int E= B+C;
	        int step= (C!=0 ? stride : 1);
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_AVG(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + E*src_base[src_offset + step+0]));
	        	dst_base[dst_offset + 1] = OP_AVG(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + E*src_base[src_offset + step+1]));
	        	dst_base[dst_offset + 2] = OP_AVG(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + E*src_base[src_offset + step+2]));
	        	dst_base[dst_offset + 3] = OP_AVG(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + E*src_base[src_offset + step+3]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }
	}
	
//	public void put_h264_chroma_mc8_c(uint8_t *dst/*align 8*/, uint8_t *src/*align 1*/, int stride, int h, int x, int y){\
	public void avg_h264_chroma_mc8_c(int[] dst_base/*align 8*/, int dst_offset, 
			int[] src_base/*align 1*/, int src_offset, int stride, int h, int x, int y){
	    int A=(8-x)*(8-y);
	    int B=(  x)*(8-y);
	    int C=(8-x)*(  y);
	    int D=(  x)*(  y);
	    int i;	    
	    ////assert(x<8 && y<8 && x>=0 && y>=0);\	
	    if(D!=0){
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_AVG(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + B*src_base[src_offset + 1] + C*src_base[src_offset + stride+0] + D*src_base[src_offset + stride+1]));
	        	dst_base[dst_offset + 1] = OP_AVG(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + B*src_base[src_offset + 2] + C*src_base[src_offset + stride+1] + D*src_base[src_offset + stride+2]));
	        	dst_base[dst_offset + 2] = OP_AVG(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + B*src_base[src_offset + 3] + C*src_base[src_offset + stride+2] + D*src_base[src_offset + stride+3]));
	        	dst_base[dst_offset + 3] = OP_AVG(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + B*src_base[src_offset + 4] + C*src_base[src_offset + stride+3] + D*src_base[src_offset + stride+4]));
	        	dst_base[dst_offset + 4] = OP_AVG(dst_base[dst_offset + 4], (A*src_base[src_offset + 4] + B*src_base[src_offset + 5] + C*src_base[src_offset + stride+4] + D*src_base[src_offset + stride+5]));
	        	dst_base[dst_offset + 5] = OP_AVG(dst_base[dst_offset + 5], (A*src_base[src_offset + 5] + B*src_base[src_offset + 6] + C*src_base[src_offset + stride+5] + D*src_base[src_offset + stride+6]));
	        	dst_base[dst_offset + 6] = OP_AVG(dst_base[dst_offset + 6], (A*src_base[src_offset + 6] + B*src_base[src_offset + 7] + C*src_base[src_offset + stride+6] + D*src_base[src_offset + stride+7]));
	        	dst_base[dst_offset + 7] = OP_AVG(dst_base[dst_offset + 7], (A*src_base[src_offset + 7] + B*src_base[src_offset + 8] + C*src_base[src_offset + stride+7] + D*src_base[src_offset + stride+8]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }else{
	        int E= B+C;	      
	        int step= (C!=0 ? stride : 1);
	        for(i=0; i<h; i++){
	        	dst_base[dst_offset + 0] = OP_AVG(dst_base[dst_offset + 0], (A*src_base[src_offset + 0] + E*src_base[src_offset + step+0]));
	        	dst_base[dst_offset + 1] = OP_AVG(dst_base[dst_offset + 1], (A*src_base[src_offset + 1] + E*src_base[src_offset + step+1]));
	        	dst_base[dst_offset + 2] = OP_AVG(dst_base[dst_offset + 2], (A*src_base[src_offset + 2] + E*src_base[src_offset + step+2]));
	        	dst_base[dst_offset + 3] = OP_AVG(dst_base[dst_offset + 3], (A*src_base[src_offset + 3] + E*src_base[src_offset + step+3]));
	        	dst_base[dst_offset + 4] = OP_AVG(dst_base[dst_offset + 4], (A*src_base[src_offset + 4] + E*src_base[src_offset + step+4]));
	        	dst_base[dst_offset + 5] = OP_AVG(dst_base[dst_offset + 5], (A*src_base[src_offset + 5] + E*src_base[src_offset + step+5]));
	        	dst_base[dst_offset + 6] = OP_AVG(dst_base[dst_offset + 6], (A*src_base[src_offset + 6] + E*src_base[src_offset + step+6]));
	        	dst_base[dst_offset + 7] = OP_AVG(dst_base[dst_offset + 7], (A*src_base[src_offset + 7] + E*src_base[src_offset + step+7]));
	            dst_offset+= stride;
	            src_offset+= stride;
	        }
	    }
	}
	
	public static void copy_block(int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int dstStride, int srcStride, int h)
	{
	    int i;
	    switch(size) {
	    	case 2:
	    	    for(i=0; i<h; i++)
	    	    {
	    	    	System.arraycopy(src_base, src_offset, dst_base, dst_offset, 2);
	    	        //AV_WN16(dst   , AV_RN16(src   ));
			        dst_offset+=dstStride;
			        src_offset+=srcStride;
	    	    }
	    	    break;
		    case 4:
			    for(i=0; i<h; i++)
			    {
	    	    	System.arraycopy(src_base, src_offset, dst_base, dst_offset, 4);
			        //AV_WN32(dst   , AV_RN32(src   ));
			        dst_offset+=dstStride;
			        src_offset+=srcStride;
			    }
	    	    break;
		    case 8:
			    for(i=0; i<h; i++)
			    {
			        //AV_WN32(dst   , AV_RN32(src   ));
			        //AV_WN32(dst+4 , AV_RN32(src+4 ));
	    	    	System.arraycopy(src_base, src_offset, dst_base, dst_offset, 8);

	    	    	dst_offset+=dstStride;
			        src_offset+=srcStride;
			    }
	    	    break;
		    case 16:
			    for(i=0; i<h; i++)
			    {
			        //AV_WN32(dst   , AV_RN32(src   ));
			        //AV_WN32(dst+4 , AV_RN32(src+4 ));
			        //AV_WN32(dst+8 , AV_RN32(src+8 ));
			        //AV_WN32(dst+12, AV_RN32(src+12));
	    	    	System.arraycopy(src_base, src_offset, dst_base, dst_offset, 16);
			        dst_offset+=dstStride;
			        src_offset+=srcStride;
			    }
	    	    break;
	    } // switch
	}
	
	public static long rnd_avg32(long a, long b) {
	    //return (long)( ((a | b)&0xffffffffl) - (( ((a ^ b)&0xffffffffl) & ((~((0x01)*0xffffffff01010101l)) >>> 1))) );
		a = a&0xffffffffl;
		b = b&0xffffffffl;
		long remainder = (a^b) & 0x01010101l;
		a = a & (~remainder);
		b = b & (~remainder);		
		long ret = ( (a + b) >>> 1 ) + remainder;
		return ret;
	}
	
	public static void pixels_l2(int opcode, int size, int[] dst_base, int dst_offset
			, int[] src1_base, int src1_offset, 
			int[] src2_base, int src2_offset,
			int dst_stride, 
            int src_stride1, int src_stride2, int h){
		int i;
		
		if(size==8) {
			for(i=0; i<h; i++){
				long a,b,c;
				//a= AV_RN32(&src1[i*src_stride1  ]);
				//b= AV_RN32(&src2[i*src_stride2  ]);
				a = ((long)src1_base[src1_offset + i*src_stride1] << 0)|((long)src1_base[src1_offset + i*src_stride1 +1] << 8)|((long)src1_base[src1_offset + i*src_stride1 +2] << 16)|((long)src1_base[src1_offset + i*src_stride1 +3] << 24);
				b = ((long)src2_base[src2_offset + i*src_stride2] << 0)|((long)src2_base[src2_offset + i*src_stride2 +1] << 8)|((long)src2_base[src2_offset + i*src_stride2 +2] << 16)|((long)src2_base[src2_offset + i*src_stride2 +3] << 24);
				c = ((long)dst_base[dst_offset + i*dst_stride] << 0)|((long)dst_base[dst_offset + i*dst_stride +1] << 8)|((long)dst_base[dst_offset + i*dst_stride +2] << 16)|((long)dst_base[dst_offset + i*dst_stride +3] << 24);
				if(opcode==0) // PUT
					c = rnd_avg32(a, b);
				
				else // AVG
					c = rnd_avg32(c, rnd_avg32(a, b));
								
				dst_base[dst_offset + i*dst_stride] = (int)(c & 0x000000ffl);
				dst_base[dst_offset + i*dst_stride +1] = (int)((c & 0x0000ff00l)>>>8);
				dst_base[dst_offset + i*dst_stride +2] = (int)((c & 0x00ff0000l)>>>16);
				dst_base[dst_offset + i*dst_stride +3] = (int)((c & 0xff000000l)>>>24);
				//a= AV_RN32(&src1[i*src_stride1+4]);
				//b= AV_RN32(&src2[i*src_stride2+4]);
				a = ((long)src1_base[4+ src1_offset + i*src_stride1] << 0)|((long)src1_base[4+ src1_offset + i*src_stride1 +1] << 8)|((long)src1_base[4+ src1_offset + i*src_stride1 +2] << 16)|((long)src1_base[4+ src1_offset + i*src_stride1 +3] << 24);
				b = ((long)src2_base[4+ src2_offset + i*src_stride2] << 0)|((long)src2_base[4+ src2_offset + i*src_stride2 +1] << 8)|((long)src2_base[4+ src2_offset + i*src_stride2 +2] << 16)|((long)src2_base[4+ src2_offset + i*src_stride2 +3] << 24);
				c = ((long)dst_base[4+ dst_offset + i*dst_stride] << 0)|((long)dst_base[4+ dst_offset + i*dst_stride +1] << 8)|((long)dst_base[4+ dst_offset + i*dst_stride +2] << 16)|((long)dst_base[4+ dst_offset + i*dst_stride +3] << 24);
				if(opcode==0) // PUT
					c = rnd_avg32(a, b);
				else // AVG
					c = rnd_avg32(c, rnd_avg32(a, b));
				
				dst_base[4+ dst_offset + i*dst_stride] = (int)(c & 0x000000ffl);
				dst_base[4+ dst_offset + i*dst_stride +1] = (int)((c & 0x0000ff00l)>>>8);
				dst_base[4+ dst_offset + i*dst_stride +2] = (int)((c & 0x00ff0000l)>>>16);
				dst_base[4+ dst_offset + i*dst_stride +3] = (int)((c & 0xff000000l)>>>24);
			} // for
		} // if
		else if(size==4) {
			for(i=0; i<h; i++){
				long a,b,c;
				//a= AV_RN32(&src1[i*src_stride1  ]);
				//b= AV_RN32(&src2[i*src_stride2  ]);
				a = (src1_base[src1_offset + i*src_stride1] << 0)|(src1_base[src1_offset + i*src_stride1 +1] << 8)|(src1_base[src1_offset + i*src_stride1 +2] << 16)|(src1_base[src1_offset + i*src_stride1 +3] << 24);
				b = (src2_base[src2_offset + i*src_stride2] << 0)|(src2_base[src2_offset + i*src_stride2 +1] << 8)|(src2_base[src2_offset + i*src_stride2 +2] << 16)|(src2_base[src2_offset + i*src_stride2 +3] << 24);
				c = (dst_base[dst_offset + i*dst_stride] << 0)|(dst_base[dst_offset + i*dst_stride +1] << 8)|(dst_base[dst_offset + i*dst_stride +2] << 16)|(dst_base[dst_offset + i*dst_stride +3] << 24);
				if(opcode==0) // PUT
					c = rnd_avg32(a, b);
				else // AVG
					c = rnd_avg32(c, rnd_avg32(a, b));
				dst_base[dst_offset + i*dst_stride] = (int)(c & 0x000000ffl);
				dst_base[dst_offset + i*dst_stride +1] = (int)((c & 0x0000ff00l)>>>8);
				dst_base[dst_offset + i*dst_stride +2] = (int)((c & 0x00ff0000l)>>>16);
				dst_base[dst_offset + i*dst_stride +3] = (int)((c & 0xff000000l)>>>24);
			}			
		} // if
		else if(size==2) {
			for(i=0; i<h; i++){
				long a,b,c;
				//a= AV_RN16(&src1[i*src_stride1  ]);
				//b= AV_RN16(&src2[i*src_stride2  ]);
				a = (src1_base[src1_offset + i*src_stride1] << 0)|(src1_base[src1_offset + i*src_stride1 +1] << 8);
				b = (src2_base[src2_offset + i*src_stride2] << 0)|(src2_base[src2_offset + i*src_stride2 +1] << 8);				
				c = (dst_base[dst_offset + i*dst_stride] << 0)|(dst_base[dst_offset + i*dst_stride +1] << 8);
				if(opcode==0) // PUT
					c = rnd_avg32(a, b);
				else // AVG
					c = rnd_avg32(c, rnd_avg32(a, b));
				dst_base[dst_offset + i*dst_stride] = (int)(c & 0x000000ffl);
				dst_base[dst_offset + i*dst_stride +1] = (int)((c & 0x0000ff00l)>>>8);
			}			
		} // if
		else if(size==16) {
			pixels_l2(opcode, 8, dst_base, dst_offset  , src1_base, src1_offset  , src2_base, src2_offset  , dst_stride, src_stride1, src_stride2, h);
			pixels_l2(opcode, 8, dst_base, dst_offset+8, src1_base, src1_offset+8, src2_base, src2_offset+8, dst_stride, src_stride1, src_stride2, h);			
		} // if
	}

	//????????????????//	
	// JAVA should has no problem with alignment
	public static void pixels_c(int opcode,int size,int[] dst_base,int dst_offset,int[] src_base,int src_offset,int stride,int height) {
	//(uint8_t *dest,const uint8_t *ref, const int stride,int height)
		switch(size) {
			case 2:
				if(opcode == 0) { // PUT
					for(int i=0;i<height;i++) {
						System.arraycopy(src_base, src_offset, dst_base, dst_offset, 2);
						dst_offset += stride;
						src_offset += stride;
					} // for
				} else { // AVG
					for(int i=0;i<height;i++) {
						long a = (src_base[src_offset] << 0)|(src_base[src_offset+1] << 8);
						long b = (dst_base[dst_offset] << 0)|(dst_base[dst_offset+1] << 8);
						b = rnd_avg32(b, a);
						dst_base[dst_offset] = (int)(b & 0x000000ffl);
						dst_base[dst_offset+1] = (int)((b & 0x0000ff00l)>>>8);
						dst_offset += stride;
						src_offset += stride;
					} // for
				} // if
				break;
			case 4:
				if(opcode == 0) { // PUT
					for(int i=0;i<height;i++) {
						System.arraycopy(src_base, src_offset, dst_base, dst_offset, 4);
						dst_offset += stride;
						src_offset += stride;
					} // for
				} else { // AVG
					for(int i=0;i<height;i++) {
						long a = (src_base[src_offset] << 0)|(src_base[src_offset+1] << 8)|(src_base[src_offset+2] << 16)|(src_base[src_offset+3] << 24);
						long b = (dst_base[dst_offset] << 0)|(dst_base[dst_offset+1] << 8)|(dst_base[dst_offset+2] << 16)|(dst_base[dst_offset+3] << 24);
						b = rnd_avg32(b, a);
						dst_base[dst_offset] = (int)(b & 0x000000ffl);
						dst_base[dst_offset+1] = (int)((b & 0x0000ff00l)>>>8);
						dst_base[dst_offset+2] = (int)((b & 0x00ff0000l)>>>16);
						dst_base[dst_offset+3] = (int)((b & 0xff000000l)>>>24);
						dst_offset += stride;
						src_offset += stride;
					} // for
				} // if
				break;
			case 8:
				if(opcode == 0) { // PUT
					for(int i=0;i<height;i++) {
						//// DebugTool.printDebugString("Copy offset: "+(src_offset-8208)+"=>"+(dst_offset-8208)+"\n");
						System.arraycopy(src_base, src_offset, dst_base, dst_offset, 8);
						dst_offset += stride;
						src_offset += stride;
					} // for
				} else { // AVG
					for(int i=0;i<height;i++) {
						long a = (src_base[src_offset] << 0)|(src_base[src_offset+1] << 8)|(src_base[src_offset+2] << 16)|(src_base[src_offset+3] << 24);
						long b = (dst_base[dst_offset] << 0)|(dst_base[dst_offset+1] << 8)|(dst_base[dst_offset+2] << 16)|(dst_base[dst_offset+3] << 24);
						b = rnd_avg32(b, a);
						dst_base[dst_offset] = (int)(b & 0x000000ff);
						dst_base[dst_offset+1] = (int)((b & 0x0000ff00l)>>>8);
						dst_base[dst_offset+2] = (int)((b & 0x00ff0000l)>>>16);
						dst_base[dst_offset+3] = (int)((b & 0xff000000l)>>>24);
						
						a = (src_base[4+ src_offset] << 0)|(src_base[4+ src_offset+1] << 8)|(src_base[4+ src_offset+2] << 16)|(src_base[4+ src_offset+3] << 24);
						b = (dst_base[4+ dst_offset] << 0)|(dst_base[4+ dst_offset+1] << 8)|(dst_base[4+ dst_offset+2] << 16)|(dst_base[4+ dst_offset+3] << 24);
						b = rnd_avg32(b, a);
						dst_base[4+ dst_offset] = (int)(b & 0x000000ffl);
						dst_base[4+ dst_offset+1] = (int)((b & 0x0000ff00l)>>>8);
						dst_base[4+ dst_offset+2] = (int)((b & 0x00ff0000l)>>>16);
						dst_base[4+ dst_offset+3] = (int)((b & 0xff000000l)>>>24);						
						
						dst_offset += stride;
						src_offset += stride;
					} // for
				} // if
				break;
			case 16: {
				pixels_c(opcode, 8, dst_base, dst_offset  , src_base, src_offset  , stride, height);
				pixels_c(opcode, 8, dst_base, dst_offset+8, src_base, src_offset+8, stride, height);			
				break;
			}
		} // switch
	}

	public static int op_avg(int a, int b, int[] cm_base, int cm_offset) { return (((a)+cm_base[cm_offset + (((b) + 16)>>5)]+1)>>1); }
	public static int op_put(int a, int b, int[] cm_base, int cm_offset) { return cm_base[cm_offset + (((b) + 16)>>5)]; }
	public static int op_avg2(int a, int b, int[] cm_base, int cm_offset) { return (((a)+cm_base[cm_offset + (((b) + 512)>>10)]+1)>>1); }
	public static int op_put2(int a, int b, int[] cm_base, int cm_offset) { 
		try {
			return cm_base[cm_offset + (((b) + 512)>>10)];
		} catch(Exception e) {
			e.printStackTrace();
			return 0;
		} // try
		}
	
	public static void h264_qpel_h_lowpass(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int dstStride, int srcStride){
		switch(size) {
		case 2: {
		    int h=2;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
		    for(i=0; i<h; i++)
		    {
		    	if(opcode == 0) { // PUT
		    		dst_base[dst_offset + 0] =  op_put(dst_base[dst_offset + 0], (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3]), cm_base, cm_offset);
		    		dst_base[dst_offset + 1] = op_put(dst_base[dst_offset + 1], (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4]), cm_base, cm_offset);
			        dst_offset+=dstStride;
		    	} else { // AVG
		    		dst_base[dst_offset + 0] = op_avg(dst_base[dst_offset + 0], (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3]), cm_base, cm_offset);
		    		dst_base[dst_offset + 1] = op_avg(dst_base[dst_offset + 1], (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4]), cm_base, cm_offset);
			        dst_offset+=dstStride;		    		
		    	} // if
		        src_offset+=srcStride;
		    }
		} // case
		break;
		case 4: {
		    int h=4;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
	        // DebugTool.printDebugString("h264_qpel4_h_lowpass:\n");
		    for(i=0; i<h; i++)
		    {
		    	if(opcode == 0) { // PUT
		    		dst_base[dst_offset + 0] = op_put(dst_base[dst_offset + 0], (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3]), cm_base, cm_offset);
		    		dst_base[dst_offset + 1] = op_put(dst_base[dst_offset + 1], (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4]), cm_base, cm_offset);
		    		dst_base[dst_offset + 2] = op_put(dst_base[dst_offset + 2], (src_base[src_offset + 2]+src_base[src_offset + 3])*20 - (src_base[src_offset + 1 ]+src_base[src_offset + 4])*5 + (src_base[src_offset + 0 ]+src_base[src_offset + 5]), cm_base, cm_offset);
		    		dst_base[dst_offset + 3] = op_put(dst_base[dst_offset + 3], (src_base[src_offset + 3]+src_base[src_offset + 4])*20 - (src_base[src_offset + 2 ]+src_base[src_offset + 5])*5 + (src_base[src_offset + 1 ]+src_base[src_offset + 6]), cm_base, cm_offset);
		    	} else { // AVG
		    		dst_base[dst_offset + 0] = op_avg(dst_base[dst_offset + 0], (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3]), cm_base, cm_offset);
		    		dst_base[dst_offset + 1] = op_avg(dst_base[dst_offset + 1], (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4]), cm_base, cm_offset);
		    		dst_base[dst_offset + 2] = op_avg(dst_base[dst_offset + 2], (src_base[src_offset + 2]+src_base[src_offset + 3])*20 - (src_base[src_offset + 1 ]+src_base[src_offset + 4])*5 + (src_base[src_offset + 0 ]+src_base[src_offset + 5]), cm_base, cm_offset);
		    		dst_base[dst_offset + 3] = op_avg(dst_base[dst_offset + 3], (src_base[src_offset + 3]+src_base[src_offset + 4])*20 - (src_base[src_offset + 2 ]+src_base[src_offset + 5])*5 + (src_base[src_offset + 1 ]+src_base[src_offset + 6]), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset+=dstStride;
		        src_offset+=srcStride;
		    }		
		} // case
		break;
		case 8: {
		    int h=8;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
	        // DebugTool.printDebugString("h264_qpel8_h_lowpass:\n");
		    for(i=0; i<h; i++)
		    {
		    	if(opcode == 0) { // PUT
		    		dst_base[dst_offset + 0] = op_put(dst_base[dst_offset + 0], (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3 ]), cm_base, cm_offset);
		    		dst_base[dst_offset + 1] = op_put(dst_base[dst_offset + 1], (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 2] =  op_put(dst_base[dst_offset + 2], (src_base[src_offset + 2]+src_base[src_offset + 3])*20 - (src_base[src_offset + 1 ]+src_base[src_offset + 4])*5 + (src_base[src_offset + 0 ]+src_base[src_offset + 5 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 3] =  op_put(dst_base[dst_offset + 3], (src_base[src_offset + 3]+src_base[src_offset + 4])*20 - (src_base[src_offset + 2 ]+src_base[src_offset + 5])*5 + (src_base[src_offset + 1 ]+src_base[src_offset + 6 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 4] =  op_put(dst_base[dst_offset + 4], (src_base[src_offset + 4]+src_base[src_offset + 5])*20 - (src_base[src_offset + 3 ]+src_base[src_offset + 6])*5 + (src_base[src_offset + 2 ]+src_base[src_offset + 7 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 5] =  op_put(dst_base[dst_offset + 5], (src_base[src_offset + 5]+src_base[src_offset + 6])*20 - (src_base[src_offset + 4 ]+src_base[src_offset + 7])*5 + (src_base[src_offset + 3 ]+src_base[src_offset + 8 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 6] =  op_put(dst_base[dst_offset + 6], (src_base[src_offset + 6]+src_base[src_offset + 7])*20 - (src_base[src_offset + 5 ]+src_base[src_offset + 8])*5 + (src_base[src_offset + 4 ]+src_base[src_offset + 9 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 7] =  op_put(dst_base[dst_offset + 7], (src_base[src_offset + 7]+src_base[src_offset + 8])*20 - (src_base[src_offset + 6 ]+src_base[src_offset + 9])*5 + (src_base[src_offset + 5 ]+src_base[src_offset + 10]), cm_base, cm_offset);
		    	} else { // AVG
			        dst_base[dst_offset + 0] = op_avg(dst_base[dst_offset + 0], (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 1] = op_avg(dst_base[dst_offset + 1], (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 2] = op_avg(dst_base[dst_offset + 2], (src_base[src_offset + 2]+src_base[src_offset + 3])*20 - (src_base[src_offset + 1 ]+src_base[src_offset + 4])*5 + (src_base[src_offset + 0 ]+src_base[src_offset + 5 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 3] = op_avg(dst_base[dst_offset + 3], (src_base[src_offset + 3]+src_base[src_offset + 4])*20 - (src_base[src_offset + 2 ]+src_base[src_offset + 5])*5 + (src_base[src_offset + 1 ]+src_base[src_offset + 6 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 4] = op_avg(dst_base[dst_offset + 4], (src_base[src_offset + 4]+src_base[src_offset + 5])*20 - (src_base[src_offset + 3 ]+src_base[src_offset + 6])*5 + (src_base[src_offset + 2 ]+src_base[src_offset + 7 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 5] = op_avg(dst_base[dst_offset + 5], (src_base[src_offset + 5]+src_base[src_offset + 6])*20 - (src_base[src_offset + 4 ]+src_base[src_offset + 7])*5 + (src_base[src_offset + 3 ]+src_base[src_offset + 8 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 6] = op_avg(dst_base[dst_offset + 6], (src_base[src_offset + 6]+src_base[src_offset + 7])*20 - (src_base[src_offset + 5 ]+src_base[src_offset + 8])*5 + (src_base[src_offset + 4 ]+src_base[src_offset + 9 ]), cm_base, cm_offset);
			        dst_base[dst_offset + 7] = op_avg(dst_base[dst_offset + 7], (src_base[src_offset + 7]+src_base[src_offset + 8])*20 - (src_base[src_offset + 6 ]+src_base[src_offset + 9])*5 + (src_base[src_offset + 5 ]+src_base[src_offset + 10]), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset+=dstStride;
		        src_offset+=srcStride;
		    }			
		} // case
		break;
		case 16: {
		    h264_qpel_h_lowpass(opcode, 8, dst_base, dst_offset  , src_base, src_offset  , dstStride, srcStride);
		    h264_qpel_h_lowpass(opcode, 8, dst_base, dst_offset+8, src_base, src_offset+8, dstStride, srcStride);
		    src_offset += 8*srcStride;
		    dst_offset += 8*dstStride;
		    h264_qpel_h_lowpass(opcode, 8, dst_base, dst_offset  , src_base, src_offset  , dstStride, srcStride);
		    h264_qpel_h_lowpass(opcode, 8, dst_base, dst_offset+8, src_base, src_offset+8, dstStride, srcStride);			
		} // case
		break;
		
		} // switch
	}
	
	public static void h264_qpel_v_lowpass(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int dstStride, int srcStride){
		switch(size) {
		case 2: {		
		    int w=2;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
		    for(i=0; i<w; i++)
		    {
		        int srcB= src_base[src_offset + -2*srcStride];
		        int srcA= src_base[src_offset + -1*srcStride];
		        int src0= src_base[src_offset + 0 *srcStride];
		        int src1= src_base[src_offset + 1 *srcStride];
		        int src2= src_base[src_offset + 2 *srcStride];
		        int src3= src_base[src_offset + 3 *srcStride];
		        int src4= src_base[src_offset + 4 *srcStride];
		    	if(opcode == 0) { // PUT
		    		dst_base[dst_offset + 0*dstStride] = op_put(dst_base[dst_offset + 0*dstStride], (src0+src1)*20 - (srcA+src2)*5 + (srcB+src3), cm_base, cm_offset);
		    		dst_base[dst_offset + 1*dstStride] = op_put(dst_base[dst_offset + 1*dstStride], (src1+src2)*20 - (src0+src3)*5 + (srcA+src4), cm_base, cm_offset);
		    	} else { // AVG
			        dst_base[dst_offset + 0*dstStride] = op_avg(dst_base[dst_offset + 0*dstStride], (src0+src1)*20 - (srcA+src2)*5 + (srcB+src3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_avg(dst_base[dst_offset + 1*dstStride], (src1+src2)*20 - (src0+src3)*5 + (srcA+src4), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset++;
		        src_offset++;
		    }
		} // case
		break;
		case 4: {
		    int w=4;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
	        // DebugTool.printDebugString("h264_qpel4_v_lowpass:\n");
		    for(i=0; i<w; i++)
		    {
		        int srcB= src_base[src_offset + -2*srcStride];
		        int srcA= src_base[src_offset + -1*srcStride];
		        int src0= src_base[src_offset + 0 *srcStride];
		        int src1= src_base[src_offset + 1 *srcStride];
		        int src2= src_base[src_offset + 2 *srcStride];
		        int src3= src_base[src_offset + 3 *srcStride];
		        int src4= src_base[src_offset + 4 *srcStride];
		        int src5= src_base[src_offset + 5 *srcStride];
		        int src6= src_base[src_offset + 6 *srcStride];
		    	if(opcode == 0) { // PUT
		    		dst_base[dst_offset + 0*dstStride] = op_put(dst_base[dst_offset + 0*dstStride], (src0+src1)*20 - (srcA+src2)*5 + (srcB+src3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_put(dst_base[dst_offset + 1*dstStride], (src1+src2)*20 - (src0+src3)*5 + (srcA+src4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_put(dst_base[dst_offset + 2*dstStride], (src2+src3)*20 - (src1+src4)*5 + (src0+src5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_put(dst_base[dst_offset + 3*dstStride], (src3+src4)*20 - (src2+src5)*5 + (src1+src6), cm_base, cm_offset);
		    	} else { // AVG
			        dst_base[dst_offset + 0*dstStride] = op_avg(dst_base[dst_offset + 0*dstStride], (src0+src1)*20 - (srcA+src2)*5 + (srcB+src3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_avg(dst_base[dst_offset + 1*dstStride], (src1+src2)*20 - (src0+src3)*5 + (srcA+src4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_avg(dst_base[dst_offset + 2*dstStride], (src2+src3)*20 - (src1+src4)*5 + (src0+src5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_avg(dst_base[dst_offset + 3*dstStride], (src3+src4)*20 - (src2+src5)*5 + (src1+src6), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset++;
		        src_offset++;
		    }
			
		} // case
		break;
		case 8: {
		    int w=8;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
		    for(i=0; i<w; i++)
		    {
		        int srcB= src_base[src_offset + -2*srcStride];
		        int srcA= src_base[src_offset + -1*srcStride];
		        int src0= src_base[src_offset + 0 *srcStride];
		        int src1= src_base[src_offset + 1 *srcStride];
		        int src2= src_base[src_offset + 2 *srcStride];
		        int src3= src_base[src_offset + 3 *srcStride];
		        int src4= src_base[src_offset + 4 *srcStride];
		        int src5= src_base[src_offset + 5 *srcStride];
		        int src6= src_base[src_offset + 6 *srcStride];
		        int src7= src_base[src_offset + 7 *srcStride];
		        int src8= src_base[src_offset + 8 *srcStride];
		        int src9= src_base[src_offset + 9 *srcStride];
		        int src10=src_base[src_offset + 10*srcStride];
		        // DebugTool.printDebugString("h264_qpel8_v_lowpass: src="+srcB+","+srcA+","+src0+","+src1+","+src2+","+src3+","+src4+","+src5+","+src6+","+src7+","+src8+","+src9+","+src10+",\n");
		    	if(opcode == 0) { // PUT
			        dst_base[dst_offset + 0*dstStride] = op_put(dst_base[dst_offset + 0*dstStride], (src0+src1)*20 - (srcA+src2)*5 + (srcB+src3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_put(dst_base[dst_offset + 1*dstStride], (src1+src2)*20 - (src0+src3)*5 + (srcA+src4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_put(dst_base[dst_offset + 2*dstStride], (src2+src3)*20 - (src1+src4)*5 + (src0+src5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_put(dst_base[dst_offset + 3*dstStride], (src3+src4)*20 - (src2+src5)*5 + (src1+src6), cm_base, cm_offset);
			        dst_base[dst_offset + 4*dstStride] = op_put(dst_base[dst_offset + 4*dstStride], (src4+src5)*20 - (src3+src6)*5 + (src2+src7), cm_base, cm_offset);
			        dst_base[dst_offset + 5*dstStride] = op_put(dst_base[dst_offset + 5*dstStride], (src5+src6)*20 - (src4+src7)*5 + (src3+src8), cm_base, cm_offset);
			        dst_base[dst_offset + 6*dstStride] = op_put(dst_base[dst_offset + 6*dstStride], (src6+src7)*20 - (src5+src8)*5 + (src4+src9), cm_base, cm_offset);
			        dst_base[dst_offset + 7*dstStride] = op_put(dst_base[dst_offset + 7*dstStride], (src7+src8)*20 - (src6+src9)*5 + (src5+src10), cm_base, cm_offset);
		    	} else { // AVG
			        dst_base[dst_offset + 0*dstStride] = op_avg(dst_base[dst_offset + 0*dstStride], (src0+src1)*20 - (srcA+src2)*5 + (srcB+src3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_avg(dst_base[dst_offset + 1*dstStride], (src1+src2)*20 - (src0+src3)*5 + (srcA+src4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_avg(dst_base[dst_offset + 2*dstStride], (src2+src3)*20 - (src1+src4)*5 + (src0+src5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_avg(dst_base[dst_offset + 3*dstStride], (src3+src4)*20 - (src2+src5)*5 + (src1+src6), cm_base, cm_offset);
			        dst_base[dst_offset + 4*dstStride] = op_avg(dst_base[dst_offset + 4*dstStride], (src4+src5)*20 - (src3+src6)*5 + (src2+src7), cm_base, cm_offset);
			        dst_base[dst_offset + 5*dstStride] = op_avg(dst_base[dst_offset + 5*dstStride], (src5+src6)*20 - (src4+src7)*5 + (src3+src8), cm_base, cm_offset);
			        dst_base[dst_offset + 6*dstStride] = op_avg(dst_base[dst_offset + 6*dstStride], (src6+src7)*20 - (src5+src8)*5 + (src4+src9), cm_base, cm_offset);
			        dst_base[dst_offset + 7*dstStride] = op_avg(dst_base[dst_offset + 7*dstStride], (src7+src8)*20 - (src6+src9)*5 + (src5+src10), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset++;
		        src_offset++;
		    }			
		} // case
		break;
		case 16: {
		    h264_qpel_v_lowpass(opcode, 8, dst_base, dst_offset  , src_base, src_offset  , dstStride, srcStride);
		    h264_qpel_v_lowpass(opcode, 8, dst_base, dst_offset+8, src_base, src_offset+8, dstStride, srcStride);
		    src_offset += 8*srcStride;
		    dst_offset += 8*dstStride;
		    h264_qpel_v_lowpass(opcode, 8, dst_base, dst_offset  , src_base, src_offset  , dstStride, srcStride);
		    h264_qpel_v_lowpass(opcode, 8, dst_base, dst_offset+8, src_base, src_offset+8, dstStride, srcStride);			
		} // case
		break;
		
		} // switch
	}
	
	public static void h264_qpel_hv_lowpass(int opcode, int size, int[] dst_base, int dst_offset, 
			/*int16_t *tmp,*/ int[] tmp_base, int tmp_offset,
			int[] src_base, int src_offset, int dstStride, int tmpStride, int srcStride){
		
		switch(size) {
		case 2: {
		    int h=2;
		    int w=2;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
		    src_offset -= 2*srcStride;
		    for(i=0; i<h+5; i++)
		    {
		        tmp_base[tmp_offset + 0]= (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3]);
		        tmp_base[tmp_offset + 1]= (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4]);
		        tmp_offset+=tmpStride;
		        src_offset+=srcStride;
		    }
		    tmp_offset -= tmpStride*(h+5-2);
		    for(i=0; i<w; i++)
		    {
		        int tmpB= tmp_base[tmp_offset + -2*tmpStride];
		        int tmpA= tmp_base[tmp_offset + -1*tmpStride];
		        int tmp0= tmp_base[tmp_offset + 0 *tmpStride];
		        int tmp1= tmp_base[tmp_offset + 1 *tmpStride];
		        int tmp2= tmp_base[tmp_offset + 2 *tmpStride];
		        int tmp3= tmp_base[tmp_offset + 3 *tmpStride];
		        int tmp4= tmp_base[tmp_offset + 4 *tmpStride];
		    	if(opcode == 0) { // PUT
			        dst_base[dst_offset + 0*dstStride] = op_put2(dst_base[dst_offset + 0*dstStride], (tmp0+tmp1)*20 - (tmpA+tmp2)*5 + (tmpB+tmp3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_put2(dst_base[dst_offset + 1*dstStride], (tmp1+tmp2)*20 - (tmp0+tmp3)*5 + (tmpA+tmp4), cm_base, cm_offset);
		    	} else { // AVG
			        dst_base[dst_offset + 0*dstStride] = op_avg2(dst_base[dst_offset + 0*dstStride], (tmp0+tmp1)*20 - (tmpA+tmp2)*5 + (tmpB+tmp3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_avg2(dst_base[dst_offset + 1*dstStride], (tmp1+tmp2)*20 - (tmp0+tmp3)*5 + (tmpA+tmp4), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset++;
		        tmp_offset++;
		    }
		} // case
		break;
		case 4: {
		    int h=4;
		    int w=4;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
	        // DebugTool.printDebugString("h264_qpel4_hv_lowpass:\n");
		    src_offset -= 2*srcStride;
		    for(i=0; i<h+5; i++)
		    {
		        tmp_base[tmp_offset + 0]= (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3]);
		        tmp_base[tmp_offset + 1]= (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4]);
		        tmp_base[tmp_offset + 2]= (src_base[src_offset + 2]+src_base[src_offset + 3])*20 - (src_base[src_offset + 1 ]+src_base[src_offset + 4])*5 + (src_base[src_offset + 0 ]+src_base[src_offset + 5]);
		        tmp_base[tmp_offset + 3]= (src_base[src_offset + 3]+src_base[src_offset + 4])*20 - (src_base[src_offset + 2 ]+src_base[src_offset + 5])*5 + (src_base[src_offset + 1 ]+src_base[src_offset + 6]);
		        tmp_offset+=tmpStride;
		        src_offset+=srcStride;
		    }
		    tmp_offset -= tmpStride*(h+5-2);
		    for(i=0; i<w; i++)
		    {
		        int tmpB= tmp_base[tmp_offset + -2*tmpStride];
		        int tmpA= tmp_base[tmp_offset + -1*tmpStride];
		        int tmp0= tmp_base[tmp_offset + 0 *tmpStride];
		        int tmp1= tmp_base[tmp_offset + 1 *tmpStride];
		        int tmp2= tmp_base[tmp_offset + 2 *tmpStride];
		        int tmp3= tmp_base[tmp_offset + 3 *tmpStride];
		        int tmp4= tmp_base[tmp_offset + 4 *tmpStride];
		        int tmp5= tmp_base[tmp_offset + 5 *tmpStride];
		        int tmp6= tmp_base[tmp_offset + 6 *tmpStride];
		    	if(opcode == 0) { // PUT
			        dst_base[dst_offset + 0*dstStride] = op_put2(dst_base[dst_offset + 0*dstStride], (tmp0+tmp1)*20 - (tmpA+tmp2)*5 + (tmpB+tmp3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_put2(dst_base[dst_offset + 1*dstStride], (tmp1+tmp2)*20 - (tmp0+tmp3)*5 + (tmpA+tmp4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_put2(dst_base[dst_offset + 2*dstStride], (tmp2+tmp3)*20 - (tmp1+tmp4)*5 + (tmp0+tmp5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_put2(dst_base[dst_offset + 3*dstStride], (tmp3+tmp4)*20 - (tmp2+tmp5)*5 + (tmp1+tmp6), cm_base, cm_offset);
		    	} else { // AVG
			        dst_base[dst_offset + 0*dstStride] = op_avg2(dst_base[dst_offset + 0*dstStride], (tmp0+tmp1)*20 - (tmpA+tmp2)*5 + (tmpB+tmp3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_avg2(dst_base[dst_offset + 1*dstStride], (tmp1+tmp2)*20 - (tmp0+tmp3)*5 + (tmpA+tmp4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_avg2(dst_base[dst_offset + 2*dstStride], (tmp2+tmp3)*20 - (tmp1+tmp4)*5 + (tmp0+tmp5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_avg2(dst_base[dst_offset + 3*dstStride], (tmp3+tmp4)*20 - (tmp2+tmp5)*5 + (tmp1+tmp6), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset++;
		        tmp_offset++;
		    }
		} // case
		break;
		case 8: {
		    int h=8;
		    int w=8;
		    int[] cm_base = H264DSPContext.ff_cropTbl;
		    int cm_offset = H264DSPContext.MAX_NEG_CROP;
		    int i;
	        // DebugTool.printDebugString("h264_qpel8_hv_lowpass:\n");
		    src_offset -= 2*srcStride;
		    for(i=0; i<h+5; i++)
		    {
		        tmp_base[tmp_offset + 0]= (src_base[src_offset + 0]+src_base[src_offset + 1])*20 - (src_base[src_offset + -1]+src_base[src_offset + 2])*5 + (src_base[src_offset + -2]+src_base[src_offset + 3 ]);
		        tmp_base[tmp_offset + 1]= (src_base[src_offset + 1]+src_base[src_offset + 2])*20 - (src_base[src_offset + 0 ]+src_base[src_offset + 3])*5 + (src_base[src_offset + -1]+src_base[src_offset + 4 ]);
		        tmp_base[tmp_offset + 2]= (src_base[src_offset + 2]+src_base[src_offset + 3])*20 - (src_base[src_offset + 1 ]+src_base[src_offset + 4])*5 + (src_base[src_offset + 0 ]+src_base[src_offset + 5 ]);
		        tmp_base[tmp_offset + 3]= (src_base[src_offset + 3]+src_base[src_offset + 4])*20 - (src_base[src_offset + 2 ]+src_base[src_offset + 5])*5 + (src_base[src_offset + 1 ]+src_base[src_offset + 6 ]);
		        tmp_base[tmp_offset + 4]= (src_base[src_offset + 4]+src_base[src_offset + 5])*20 - (src_base[src_offset + 3 ]+src_base[src_offset + 6])*5 + (src_base[src_offset + 2 ]+src_base[src_offset + 7 ]);
		        tmp_base[tmp_offset + 5]= (src_base[src_offset + 5]+src_base[src_offset + 6])*20 - (src_base[src_offset + 4 ]+src_base[src_offset + 7])*5 + (src_base[src_offset + 3 ]+src_base[src_offset + 8 ]);
		        tmp_base[tmp_offset + 6]= (src_base[src_offset + 6]+src_base[src_offset + 7])*20 - (src_base[src_offset + 5 ]+src_base[src_offset + 8])*5 + (src_base[src_offset + 4 ]+src_base[src_offset + 9 ]);
		        tmp_base[tmp_offset + 7]= (src_base[src_offset + 7]+src_base[src_offset + 8])*20 - (src_base[src_offset + 6 ]+src_base[src_offset + 9])*5 + (src_base[src_offset + 5 ]+src_base[src_offset + 10]);
		        tmp_offset+=tmpStride;
		        src_offset+=srcStride;
		    }
		    tmp_offset -= tmpStride*(h+5-2);
		    for(i=0; i<w; i++)
		    {
		        int tmpB= tmp_base[tmp_offset + -2*tmpStride];
		        int tmpA= tmp_base[tmp_offset + -1*tmpStride];
		        int tmp0= tmp_base[tmp_offset + 0 *tmpStride];
		        int tmp1= tmp_base[tmp_offset + 1 *tmpStride];
		        int tmp2= tmp_base[tmp_offset + 2 *tmpStride];
		        int tmp3= tmp_base[tmp_offset + 3 *tmpStride];
		        int tmp4= tmp_base[tmp_offset + 4 *tmpStride];
		        int tmp5= tmp_base[tmp_offset + 5 *tmpStride];
		        int tmp6= tmp_base[tmp_offset + 6 *tmpStride];
		        int tmp7= tmp_base[tmp_offset + 7 *tmpStride];
		        int tmp8= tmp_base[tmp_offset + 8 *tmpStride];
		        int tmp9= tmp_base[tmp_offset + 9 *tmpStride];
		        int tmp10=tmp_base[tmp_offset + 10*tmpStride];
		    	if(opcode == 0) { // PUT
			        dst_base[dst_offset + 0*dstStride] = op_put2(dst_base[dst_offset + 0*dstStride], (tmp0+tmp1)*20 - (tmpA+tmp2)*5 + (tmpB+tmp3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_put2(dst_base[dst_offset + 1*dstStride], (tmp1+tmp2)*20 - (tmp0+tmp3)*5 + (tmpA+tmp4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_put2(dst_base[dst_offset + 2*dstStride], (tmp2+tmp3)*20 - (tmp1+tmp4)*5 + (tmp0+tmp5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_put2(dst_base[dst_offset + 3*dstStride], (tmp3+tmp4)*20 - (tmp2+tmp5)*5 + (tmp1+tmp6), cm_base, cm_offset);
			        dst_base[dst_offset + 4*dstStride] = op_put2(dst_base[dst_offset + 4*dstStride], (tmp4+tmp5)*20 - (tmp3+tmp6)*5 + (tmp2+tmp7), cm_base, cm_offset);
			        dst_base[dst_offset + 5*dstStride] = op_put2(dst_base[dst_offset + 5*dstStride], (tmp5+tmp6)*20 - (tmp4+tmp7)*5 + (tmp3+tmp8), cm_base, cm_offset);
			        dst_base[dst_offset + 6*dstStride] = op_put2(dst_base[dst_offset + 6*dstStride], (tmp6+tmp7)*20 - (tmp5+tmp8)*5 + (tmp4+tmp9), cm_base, cm_offset);
			        dst_base[dst_offset + 7*dstStride] = op_put2(dst_base[dst_offset + 7*dstStride], (tmp7+tmp8)*20 - (tmp6+tmp9)*5 + (tmp5+tmp10), cm_base, cm_offset);
		    	} else { // AVG
			        dst_base[dst_offset + 0*dstStride] = op_avg2(dst_base[dst_offset + 0*dstStride], (tmp0+tmp1)*20 - (tmpA+tmp2)*5 + (tmpB+tmp3), cm_base, cm_offset);
			        dst_base[dst_offset + 1*dstStride] = op_avg2(dst_base[dst_offset + 1*dstStride], (tmp1+tmp2)*20 - (tmp0+tmp3)*5 + (tmpA+tmp4), cm_base, cm_offset);
			        dst_base[dst_offset + 2*dstStride] = op_avg2(dst_base[dst_offset + 2*dstStride], (tmp2+tmp3)*20 - (tmp1+tmp4)*5 + (tmp0+tmp5), cm_base, cm_offset);
			        dst_base[dst_offset + 3*dstStride] = op_avg2(dst_base[dst_offset + 3*dstStride], (tmp3+tmp4)*20 - (tmp2+tmp5)*5 + (tmp1+tmp6), cm_base, cm_offset);
			        dst_base[dst_offset + 4*dstStride] = op_avg2(dst_base[dst_offset + 4*dstStride], (tmp4+tmp5)*20 - (tmp3+tmp6)*5 + (tmp2+tmp7), cm_base, cm_offset);
			        dst_base[dst_offset + 5*dstStride] = op_avg2(dst_base[dst_offset + 5*dstStride], (tmp5+tmp6)*20 - (tmp4+tmp7)*5 + (tmp3+tmp8), cm_base, cm_offset);
			        dst_base[dst_offset + 6*dstStride] = op_avg2(dst_base[dst_offset + 6*dstStride], (tmp6+tmp7)*20 - (tmp5+tmp8)*5 + (tmp4+tmp9), cm_base, cm_offset);
			        dst_base[dst_offset + 7*dstStride] = op_avg2(dst_base[dst_offset + 7*dstStride], (tmp7+tmp8)*20 - (tmp6+tmp9)*5 + (tmp5+tmp10), cm_base, cm_offset);		    		
		    	} // if
		        dst_offset++;
		        tmp_offset++;
		    }			
		} // case
		break;
		case 16: {
		    h264_qpel_hv_lowpass(opcode, 8, dst_base, dst_offset  , tmp_base, tmp_offset  , src_base, src_offset  , dstStride, tmpStride, srcStride);
		    h264_qpel_hv_lowpass(opcode, 8, dst_base, dst_offset+8, tmp_base, tmp_offset+8, src_base, src_offset+8, dstStride, tmpStride, srcStride);
		    src_offset += 8*srcStride;
		    dst_offset += 8*dstStride;
		    h264_qpel_hv_lowpass(opcode, 8, dst_base, dst_offset  , tmp_base, tmp_offset  , src_base, src_offset  , dstStride, tmpStride, srcStride);
		    h264_qpel_hv_lowpass(opcode, 8, dst_base, dst_offset+8, tmp_base, tmp_offset+8, src_base, src_offset+8, dstStride, tmpStride, srcStride);			
		} // case
		break;
		
		} // switch
	}
	
	public static void h264_qpel_mc00_c (int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    pixels_c(opcode, size, dst_base, dst_offset, src_base, src_offset, stride, size);
	}
	
	public static void h264_qpel_mc10_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] half = new int[size*size];
	    h264_qpel_h_lowpass(0, size, half, 0, src_base, src_offset, size, stride);
	    pixels_l2(opcode, size, dst_base, dst_offset, src_base, src_offset, half, 0, stride, stride, size, size);
	}
	
	public static void h264_qpel_mc20_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    h264_qpel_h_lowpass(opcode, size, dst_base, dst_offset, src_base, src_offset, stride, stride);
	}
	
	public static void h264_qpel_mc30_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] half = new int[size*size];
	    h264_qpel_h_lowpass(0, size, half, 0, src_base, src_offset, size, stride);
	    pixels_l2(opcode, size, dst_base, dst_offset, src_base, src_offset+1, half, 0, stride, stride, size, size);
	}
	
	public static void h264_qpel_mc01_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    int[] half = new int[size*size];
	    copy_block (size, full, 0, src_base, src_offset - stride*2, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, half, 0, full, full_mid, size, size);
	    pixels_l2(opcode, size, dst_base, dst_offset, full, full_mid, half, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc02_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    copy_block (size, full, 0, src_base, src_offset - stride*2, size,  stride, size + 5);
	    h264_qpel_v_lowpass(opcode, size, dst_base, dst_offset, full, full_mid, stride, size);
	}
	
	public static void h264_qpel_mc03_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    int[] half = new int[size*size];
	    copy_block (size, full, 0, src_base, src_offset - stride*2, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, half, 0, full, full_mid, size, size);
	    pixels_l2(opcode, size, dst_base, dst_offset, full, full_mid+size, half, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc11_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    int[] halfH = new int[size*size];
	    int[] halfV = new int[size*size];
	    h264_qpel_h_lowpass(0, size, halfH, 0, src_base, src_offset, size, stride);
	    copy_block (size, full, 0, src_base, src_offset - stride*2, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, halfV, 0, full, full_mid, size, size);
	    pixels_l2(opcode, size, dst_base, dst_offset, halfH, 0, halfV, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc31_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    int[] halfH = new int[size*size];
	    int[] halfV = new int[size*size];
	    h264_qpel_h_lowpass(0, size, halfH, 0, src_base, src_offset, size, stride);
	    copy_block (size, full, 0, src_base, src_offset - stride*2 + 1, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, halfV, 0, full, full_mid, size, size);
	    pixels_l2(opcode, size, dst_base, dst_offset, halfH, 0, halfV, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc13_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    int[] halfH = new int[size*size];
	    int[] halfV = new int[size*size];
	    h264_qpel_h_lowpass(0, size, halfH, 0, src_base, src_offset + stride, size, stride);
	    copy_block (size, full, 0, src_base, src_offset - stride*2, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, halfV, 0, full, full_mid, size, size);
	    pixels_l2(opcode, size, dst_base, dst_offset, halfH, 0, halfV, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc33_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    int[] halfH = new int[size*size];
	    int[] halfV = new int[size*size];
	    h264_qpel_h_lowpass(0, size, halfH, 0, src_base, src_offset + stride, size, stride);
	    copy_block (size, full, 0, src_base, src_offset - stride*2 + 1, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, halfV, 0, full, full_mid, size, size);
	    pixels_l2(opcode, size, dst_base, dst_offset, halfH, 0, halfV, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc22_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    /*int16_t*/ int[] tmp = new int[size*(size+5)];
	    h264_qpel_hv_lowpass(opcode, size, dst_base, dst_offset, tmp, 0, src_base, src_offset, stride, size, stride);
	}
	
	public static void h264_qpel_mc21_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    /*int16_t*/ int[] tmp = new int[size*(size+5)];
	    int[] halfH = new int[size*size];
	    int[] halfHV = new int[size*size];
	    h264_qpel_h_lowpass(0, size, halfH, 0, src_base, src_offset, size, stride);
	    h264_qpel_hv_lowpass(0, size, halfHV, 0, tmp, 0, src_base, src_offset, size, size, stride);
	    pixels_l2(opcode, size, dst_base, dst_offset, halfH, 0, halfHV, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc23_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    /*int16_t*/ int[] tmp = new int[size*(size+5)];
	    int[] halfH = new int[size*size];
	    int[] halfHV = new int[size*size];
	    h264_qpel_h_lowpass(0, size, halfH, 0, src_base, src_offset + stride, size, stride);
	    h264_qpel_hv_lowpass(0, size, halfHV, 0, tmp, 0, src_base, src_offset, size, size, stride);
	    pixels_l2(opcode, size, dst_base, dst_offset, halfH, 0, halfHV, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc12_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    /*int16_t*/ int[] tmp = new int[size*(size+5)];
	    int[] halfV = new int[size*size];
	    int[] halfHV = new int[size*size];
	    copy_block (size, full, 0, src_base, src_offset - stride*2, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, halfV, 0, full, full_mid, size, size);
	    h264_qpel_hv_lowpass(0, size, halfHV, 0, tmp, 0, src_base, src_offset, size, size, stride);
	    
	    // DebugTool.printDebugString("halfV:");
	    for(int i=0;i<halfV.length;i++) {
	    	// DebugTool.printDebugString(","+halfV[i]);
	    }
	    // DebugTool.printDebugString("\n");
	    
	    // DebugTool.printDebugString("halfHV:");
	    for(int i=0;i<halfHV.length;i++) {
	    	// DebugTool.printDebugString(","+halfHV[i]);
	    }
	    // DebugTool.printDebugString("\n");

	    pixels_l2(opcode, size, dst_base, dst_offset, halfV, 0, halfHV, 0, stride, size, size, size);
	}
	
	public static void h264_qpel_mc32_c(int opcode, int size, int[] dst_base, int dst_offset, int[] src_base, int src_offset, int stride){
	    int[] full = new int[size*(size+5)];
	    int full_mid= size*2;
	    /*int16_t*/ int[] tmp = new int[size*(size+5)];
	    int[] halfV = new int[size*size];
	    int[] halfHV = new int[size*size];
	    copy_block (size, full, 0, src_base, src_offset - stride*2 + 1, size,  stride, size + 5);
	    h264_qpel_v_lowpass(0, size, halfV, 0, full, full_mid, size, size);
	    h264_qpel_hv_lowpass(0, size, halfHV, 0, tmp, 0, src_base, src_offset, size, size, stride);
	    pixels_l2(opcode, size, dst_base, dst_offset, halfV, 0, halfHV, 0, stride, size, size, size);
	}
	
	
    public Ih264_chroma_mc_func[] put_h264_chroma_pixels_tab = new Ih264_chroma_mc_func[3];
    public Ih264_chroma_mc_func[] avg_h264_chroma_pixels_tab = new Ih264_chroma_mc_func[3];

    public void dsputil_init(/*AVCodecContext *avctx*/ MpegEncContext s) {
        int i;

        // No Alignment Checking in JAVA
        //ff_check_alignment();

        //??????????????????????????
        // Why initialize it if we don't even use it?
        /*
        if(s.lowres==1){
            //if(s.idct_algo==FF_IDCT_INT || s.idct_algo==FF_IDCT_AUTO || !CONFIG_H264_DECODER){
            //    this.idct_put= ff_jref_idct4_put;
            //    this.idct_add= ff_jref_idct4_add;
            //}else{
                this.idct_put= ff_h264_lowres_idct_put_c;
                this.idct_add= ff_h264_lowres_idct_add_c;
            //}
            this.idct    = j_rev_dct4;
            this.idct_permutation_type= FF_NO_IDCT_PERM;
        }else if(s.lowres==2){
            this.idct_put= ff_jref_idct2_put;
            this.idct_add= ff_jref_idct2_add;
            this.idct    = j_rev_dct2;
            this.idct_permutation_type= FF_NO_IDCT_PERM;
        }else if(s.lowres==3){
            this.idct_put= ff_jref_idct1_put;
            this.idct_add= ff_jref_idct1_add;
            this.idct    = j_rev_dct1;
            this.idct_permutation_type= FF_NO_IDCT_PERM;
        }else{
            if(s.idct_algo==FF_IDCT_INT){
                this.idct_put= ff_jref_idct_put;
                this.idct_add= ff_jref_idct_add;
                this.idct    = j_rev_dct;
                this.idct_permutation_type= FF_LIBMPEG2_IDCT_PERM;
           // }else if((CONFIG_VP3_DECODER || CONFIG_VP5_DECODER || CONFIG_VP6_DECODER ) &&
           //         s.idct_algo==FF_IDCT_VP3){
           //     this.idct_put= ff_vp3_idct_put_c;
           //     this.idct_add= ff_vp3_idct_add_c;
           //     this.idct    = ff_vp3_idct_c;
           //     this.idct_permutation_type= FF_NO_IDCT_PERM;
           // }else if(s.idct_algo==FF_IDCT_WMV2){
           //     this.idct_put= ff_wmv2_idct_put_c;
           //     this.idct_add= ff_wmv2_idct_add_c;
           //     this.idct    = ff_wmv2_idct_c;
           //     this.idct_permutation_type= FF_NO_IDCT_PERM;
           // }else if(s.idct_algo==FF_IDCT_FAAN){
           //     this.idct_put= ff_faanidct_put;
           //     this.idct_add= ff_faanidct_add;
           //     this.idct    = ff_faanidct;
           //     this.idct_permutation_type= FF_NO_IDCT_PERM;
           // }else if(CONFIG_EATGQ_DECODER && s.idct_algo==FF_IDCT_EA) {
           //     this.idct_put= ff_ea_idct_put_c;
           //     this.idct_permutation_type= FF_NO_IDCT_PERM;
           // }else if(CONFIG_BINK_DECODER && s.idct_algo==FF_IDCT_BINK) {
           //     this.idct     = ff_bink_idct_c;
           //     this.idct_add = ff_bink_idct_add_c;
           //     this.idct_put = ff_bink_idct_put_c;
           //     this.idct_permutation_type = FF_NO_IDCT_PERM;
            }else{ //accurate/default
                this.idct_put= ff_simple_idct_put;
                this.idct_add= ff_simple_idct_add;
                this.idct    = ff_simple_idct;
                this.idct_permutation_type= FF_NO_IDCT_PERM;
            }
        }
        */
        
        // For JAVA software decoder, use simple IDCT Scanline:
        this.idct_permutation_type= FF_NO_IDCT_PERM;        
        
        put_h264_qpel_pixels_tab[0][ 0] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			pixels_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride,16);	
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 1] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc10_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 2] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc20_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 3] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc30_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 4] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc01_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 5] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc11_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 6] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc21_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 7] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc31_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 8] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc02_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][ 9] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc12_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][10] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc22_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][11] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc32_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][12] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc03_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][13] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc13_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][14] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc23_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[0][15] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc33_c(0, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 

        put_h264_qpel_pixels_tab[1][ 0] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			pixels_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride, 8); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 1] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc10_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 2] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc20_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 3] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc30_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 4] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc01_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 5] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc11_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 6] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc21_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 7] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc31_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 8] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc02_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][ 9] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc12_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][10] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc22_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][11] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc32_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][12] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc03_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][13] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc13_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][14] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc23_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[1][15] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc33_c(0, 8, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 

        put_h264_qpel_pixels_tab[2][ 0] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc00_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 1] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc10_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 2] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc20_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 3] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc30_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 4] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc01_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 5] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc11_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 6] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc21_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 7] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc31_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 8] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc02_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][ 9] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc12_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][10] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc22_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][11] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc32_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][12] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc03_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][13] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc13_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][14] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc23_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[2][15] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc33_c(0, 4, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 

        put_h264_qpel_pixels_tab[3][ 0] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc00_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 1] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc10_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 2] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc20_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 3] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc30_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 4] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc01_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 5] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc11_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 6] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc21_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 7] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc31_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 8] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc02_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][ 9] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc12_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][10] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc22_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][11] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc32_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][12] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc03_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][13] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc13_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][14] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc23_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        put_h264_qpel_pixels_tab[3][15] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc33_c(0, 2, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 

        avg_h264_qpel_pixels_tab[0][ 0] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			pixels_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride, 16); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 1] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc10_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 2] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc20_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 3] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc30_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 4] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc01_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 5] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc11_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 6] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc21_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 7] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc31_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 8] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc02_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][ 9] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc12_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][10] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc22_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][11] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc32_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][12] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc03_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][13] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc13_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][14] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc23_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[0][15] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc33_c(1, 16, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 

        avg_h264_qpel_pixels_tab[1][ 0] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			pixels_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride, 8); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 1] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc10_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 2] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc20_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 3] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc30_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 4] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc01_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 5] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc11_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 6] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc21_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 7] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc31_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 8] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc02_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][ 9] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc12_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][10] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc22_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][11] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc32_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][12] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc03_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][13] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc13_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][14] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc23_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[1][15] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc33_c(1, 8, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 

        avg_h264_qpel_pixels_tab[2][ 0] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc00_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 1] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc10_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 2] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc20_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 3] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc30_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 4] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc01_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 5] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc11_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 6] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc21_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 7] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc31_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 8] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc02_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][ 9] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc12_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][10] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc22_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][11] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc32_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][12] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc03_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][13] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc13_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][14] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc23_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride); 
    		}
        }; 
        avg_h264_qpel_pixels_tab[2][15] = new Ih264_qpel_mc_func() {
    		public void h264_qpel_mc_func(int[] dst_base, int dst_offset,int[] src_base, int src_offset,int stride) {
    			h264_qpel_mc33_c(1, 4, dst_base, dst_offset, src_base, src_offset, stride);
    		}
        }; 
    	
        put_h264_chroma_pixels_tab[0]= new Ih264_chroma_mc_func() {
        	public void h264_chroma_mc_func(
    				int[] dst_base/*align 8*/, int dst_offset,
    				int[] src_base/*align 1*/, int src_offset, 
    				int srcStride, int h, int x, int y){
        		put_h264_chroma_mc8_c(dst_base, dst_offset, src_base, src_offset, srcStride, h, x, y);
        	}
        };
        put_h264_chroma_pixels_tab[1]= new Ih264_chroma_mc_func() {
        	public void h264_chroma_mc_func(
    				int[] dst_base/*align 8*/, int dst_offset,
    				int[] src_base/*align 1*/, int src_offset, 
    				int srcStride, int h, int x, int y){
        		put_h264_chroma_mc4_c(dst_base, dst_offset, src_base, src_offset, srcStride, h, x, y);
        	}
        };
        put_h264_chroma_pixels_tab[2]= new Ih264_chroma_mc_func() {
        	public void h264_chroma_mc_func(
    				int[] dst_base/*align 8*/, int dst_offset,
    				int[] src_base/*align 1*/, int src_offset, 
    				int srcStride, int h, int x, int y){
        		put_h264_chroma_mc2_c(dst_base, dst_offset, src_base, src_offset, srcStride, h, x, y);
        	}
        };
        avg_h264_chroma_pixels_tab[0]= new Ih264_chroma_mc_func() {
        	public void h264_chroma_mc_func(
    				int[] dst_base/*align 8*/, int dst_offset,
    				int[] src_base/*align 1*/, int src_offset, 
    				int srcStride, int h, int x, int y){
        		avg_h264_chroma_mc8_c(dst_base, dst_offset, src_base, src_offset, srcStride, h, x, y);
        	}
        };
        avg_h264_chroma_pixels_tab[1]= new Ih264_chroma_mc_func() {
        	public void h264_chroma_mc_func(
    				int[] dst_base/*align 8*/, int dst_offset,
    				int[] src_base/*align 1*/, int src_offset, 
    				int srcStride, int h, int x, int y){
        		avg_h264_chroma_mc4_c(dst_base, dst_offset, src_base, src_offset, srcStride, h, x, y);
        	}
        };
        avg_h264_chroma_pixels_tab[2]= new Ih264_chroma_mc_func() {
        	public void h264_chroma_mc_func(
    				int[] dst_base/*align 8*/, int dst_offset,
    				int[] src_base/*align 1*/, int src_offset, 
    				int srcStride, int h, int x, int y){
        		avg_h264_chroma_mc2_c(dst_base, dst_offset, src_base, src_offset, srcStride, h, x, y);
        	}
        };
        
        switch(this.idct_permutation_type){
        case FF_NO_IDCT_PERM:
            for(i=0; i<64; i++)
            	this.idct_permutation[i]= i;
            break;
        } // switch
        
    }

	
    
}
