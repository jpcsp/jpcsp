package com.twilight.h264.decoder;

public class Rectangle {
	
	// Assume 1 vp is 1 uint_8 = 1 int, which infact represents 1 byte of data in C++
	public static void fill_rectangle_unsign(int[] vp, int vp_offset, 
			 int w, int h, int stride, int val, int size){

		int p_offset = vp_offset;
	    w   = w * size;
	    stride *= size;
	    
	    if(w == 2) {
    		int v1;
    		int v2;
    		if(size == 4) {
		    	v1 = val & 0x0ff;
		    	v2 = (val >> 8) & 0x0ff;    			
    		} else {
		    	v1 = val & 0x0ff;
		    	v2 = v1;
    		} // if
	    	vp[p_offset + 0*stride]= v1;
	    	vp[p_offset + 0*stride + 1]= v2;
	        if(h==1) return;
	        vp[p_offset + 1*stride]= v1;
	        vp[p_offset + 1*stride + 1]= v2;
	        if(h==2) return;
	        vp[p_offset + 2*stride]= v1;
	        vp[p_offset + 2*stride + 1]= v2;
	        vp[p_offset + 3*stride]= v1;	    	
	        vp[p_offset + 3*stride + 1]= v2;	    	
	    } else if(w==4) {
    		int v1;
    		int v2;
    		int v3;
    		int v4 ;
	    	if(size == 4) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = (val & 0x0ff0000) >>> 16;
	    		v4 = (val & 0xff000000) >>> 24;
	    	} else if(size == 2) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = v1;
	    		v4 = v2;
	    	} else {
	    		v1 = val & 0x0ff;
	    		v2 = v1;
	    		v3 = v1;
	    		v4 = v1;	    		
	    	}
    		vp[p_offset + 0*stride] = v1;
    		vp[p_offset + 0*stride + 1] = v2;
    		vp[p_offset + 0*stride + 2] = v3;
    		vp[p_offset + 0*stride + 3] = v4;
	        if(h==1) return;
    		vp[p_offset + 1*stride] = v1;
    		vp[p_offset + 1*stride + 1] = v2;
    		vp[p_offset + 1*stride + 2] = v3;
    		vp[p_offset + 1*stride + 3] = v4;
	        if(h==2) return;
    		vp[p_offset + 2*stride] = v1;
    		vp[p_offset + 2*stride + 1] = v2;
    		vp[p_offset + 2*stride + 2] = v3;
    		vp[p_offset + 2*stride + 3] = v4;
    		vp[p_offset + 3*stride] = v1;
    		vp[p_offset + 3*stride + 1] = v2;
    		vp[p_offset + 3*stride + 2] = v3;
    		vp[p_offset + 3*stride + 3] = v4;

	    } else if(w==8) {
	    	int v1;
	    	int v2;
	    	int v3;
	    	int v4;
	    	if(size == 2) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = v1;
	    		v4 = v2;
	    	} else {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = (val & 0x0ff0000) >>> 16;
	    		v4 = (val & 0xff000000) >>> 24;	    		
	    	} // if
    		vp[p_offset + 0*stride] = v1;
    		vp[p_offset + 0*stride + 1] = v2;
    		vp[p_offset + 0*stride + 2] = v3;
    		vp[p_offset + 0*stride + 3] = v4;
    		vp[p_offset + 0*stride +4] = v1;
    		vp[p_offset + 0*stride +4 + 1] = v2;
    		vp[p_offset + 0*stride +4 + 2] = v3;
    		vp[p_offset + 0*stride +4 + 3] = v4;
	        if(h==1) return;
    		vp[p_offset + 1*stride] = v1;
    		vp[p_offset + 1*stride + 1] = v2;
    		vp[p_offset + 1*stride + 2] = v3;
    		vp[p_offset + 1*stride + 3] = v4;
    		vp[p_offset + 1*stride +4] = v1;
    		vp[p_offset + 1*stride +4 + 1] = v2;
    		vp[p_offset + 1*stride +4 + 2] = v3;
    		vp[p_offset + 1*stride +4 + 3] = v4;
	        if(h==2) return;
    		vp[p_offset + 2*stride] = v1;
    		vp[p_offset + 2*stride + 1] = v2;
    		vp[p_offset + 2*stride + 2] = v3;
    		vp[p_offset + 2*stride + 3] = v4;
    		vp[p_offset + 2*stride +4] = v1;
    		vp[p_offset + 2*stride +4 + 1] = v2;
    		vp[p_offset + 2*stride +4 + 2] = v3;
    		vp[p_offset + 2*stride +4 + 3] = v4;
    		vp[p_offset + 3*stride] = v1;
    		vp[p_offset + 3*stride + 1] = v2;
    		vp[p_offset + 3*stride + 2] = v3;
    		vp[p_offset + 3*stride + 3] = v4;
    		vp[p_offset + 3*stride +4] = v1;
    		vp[p_offset + 3*stride +4 + 1] = v2;
    		vp[p_offset + 3*stride +4 + 2] = v3;
    		vp[p_offset + 3*stride +4 + 3] = v4;
	    
	    } else if(w==16) {
    		int v1 = val & 0x0ff;
    		int v2 = (val & 0x0ff00) >>> 8;
    		int v3 = (val & 0x0ff0000) >>> 16;
    		int v4 = (val & 0xff000000) >>> 24;	    		

    		for(int i=0;i<4;i++) {
	    		vp[p_offset + i*4 + 0*stride] = v1;
	    		vp[p_offset + i*4 + 0*stride + 1] = v2;
	    		vp[p_offset + i*4 + 0*stride + 2] = v3;
	    		vp[p_offset + i*4 + 0*stride + 3] = v4;

	    		vp[p_offset + i*4 + 1*stride] = v1;
	    		vp[p_offset + i*4 + 1*stride + 1] = v2;
	    		vp[p_offset + i*4 + 1*stride + 2] = v3;
	    		vp[p_offset + i*4 + 1*stride + 3] = v4;
    		} // for   		
	        if(h==2) return;
    		for(int i=0;i<4;i++) {
	    		vp[p_offset + i*4 + 2*stride] = v1;
	    		vp[p_offset + i*4 + 2*stride + 1] = v2;
	    		vp[p_offset + i*4 + 2*stride + 2] = v3;
	    		vp[p_offset + i*4 + 2*stride + 3] = v4;

	    		vp[p_offset + i*4 + 3*stride] = v1;
	    		vp[p_offset + i*4 + 3*stride + 1] = v2;
	    		vp[p_offset + i*4 + 3*stride + 2] = v3;
	    		vp[p_offset + i*4 + 3*stride + 3] = v4;
    		} // for   		
	        	    		    
	    } // if
	}

	// Assume 1 vp is 1 int_8 = 1 int, which infact represents 1 byte of data in C++
	public static void fill_rectangle_sign(int[] vp, int vp_offset, 
			 int w, int h, int stride, int val, int size){

		int p_offset = vp_offset;
	    w   = w * size;
	    stride *= size;
	    
	    if(w == 2) {
    		int v1;
    		int v2;
    		if(size == 4) {
		    	v1 = val & 0x0ff;
		    	v2 = (val >> 8) & 0x0ff;    			
    		} else {
		    	v1 = val;
		    	v2 = v1;
    		} // if
	    	vp[p_offset + 0*stride]= v1;
	    	vp[p_offset + 0*stride + 1]= v2;
	        if(h==1) return;
	        vp[p_offset + 1*stride]= v1;
	        vp[p_offset + 1*stride + 1]= v2;
	        if(h==2) return;
	        vp[p_offset + 2*stride]= v1;
	        vp[p_offset + 2*stride + 1]= v2;
	        vp[p_offset + 3*stride]= v1;	    	
	        vp[p_offset + 3*stride + 1]= v2;	    	
	    } else if(w==4) {
    		int v1;
    		int v2;
    		int v3;
    		int v4 ;
	    	if(size == 4) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = (val & 0x0ff0000) >>> 16;
	    		v4 = (val & 0xff000000) >>> 24;
	    	} else if(size == 2) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = v1;
	    		v4 = v2;
	    	} else {
	    		v1 = val;
	    		v2 = v1;
	    		v3 = v1;
	    		v4 = v1;	    		
	    	}
    		vp[p_offset + 0*stride] = v1;
    		vp[p_offset + 0*stride + 1] = v2;
    		vp[p_offset + 0*stride + 2] = v3;
    		vp[p_offset + 0*stride + 3] = v4;
	        if(h==1) return;
    		vp[p_offset + 1*stride] = v1;
    		vp[p_offset + 1*stride + 1] = v2;
    		vp[p_offset + 1*stride + 2] = v3;
    		vp[p_offset + 1*stride + 3] = v4;
	        if(h==2) return;
    		vp[p_offset + 2*stride] = v1;
    		vp[p_offset + 2*stride + 1] = v2;
    		vp[p_offset + 2*stride + 2] = v3;
    		vp[p_offset + 2*stride + 3] = v4;
    		vp[p_offset + 3*stride] = v1;
    		vp[p_offset + 3*stride + 1] = v2;
    		vp[p_offset + 3*stride + 2] = v3;
    		vp[p_offset + 3*stride + 3] = v4;

	    } else if(w==8) {
	    	int v1;
	    	int v2;
	    	int v3;
	    	int v4;
	    	if(size == 2) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = v1;
	    		v4 = v2;
	    	} else {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = (val & 0x0ff0000) >>> 16;
	    		v4 = (val & 0xff000000) >>> 24;	    		
	    	} // if
    		vp[p_offset + 0*stride] = v1;
    		vp[p_offset + 0*stride + 1] = v2;
    		vp[p_offset + 0*stride + 2] = v3;
    		vp[p_offset + 0*stride + 3] = v4;
    		vp[p_offset + 0*stride +4] = v1;
    		vp[p_offset + 0*stride +4 + 1] = v2;
    		vp[p_offset + 0*stride +4 + 2] = v3;
    		vp[p_offset + 0*stride +4 + 3] = v4;
	        if(h==1) return;
    		vp[p_offset + 1*stride] = v1;
    		vp[p_offset + 1*stride + 1] = v2;
    		vp[p_offset + 1*stride + 2] = v3;
    		vp[p_offset + 1*stride + 3] = v4;
    		vp[p_offset + 1*stride +4] = v1;
    		vp[p_offset + 1*stride +4 + 1] = v2;
    		vp[p_offset + 1*stride +4 + 2] = v3;
    		vp[p_offset + 1*stride +4 + 3] = v4;
	        if(h==2) return;
    		vp[p_offset + 2*stride] = v1;
    		vp[p_offset + 2*stride + 1] = v2;
    		vp[p_offset + 2*stride + 2] = v3;
    		vp[p_offset + 2*stride + 3] = v4;
    		vp[p_offset + 2*stride +4] = v1;
    		vp[p_offset + 2*stride +4 + 1] = v2;
    		vp[p_offset + 2*stride +4 + 2] = v3;
    		vp[p_offset + 2*stride +4 + 3] = v4;
    		vp[p_offset + 3*stride] = v1;
    		vp[p_offset + 3*stride + 1] = v2;
    		vp[p_offset + 3*stride + 2] = v3;
    		vp[p_offset + 3*stride + 3] = v4;
    		vp[p_offset + 3*stride +4] = v1;
    		vp[p_offset + 3*stride +4 + 1] = v2;
    		vp[p_offset + 3*stride +4 + 2] = v3;
    		vp[p_offset + 3*stride +4 + 3] = v4;
	    
	    } else if(w==16) {
    		int v1 = val & 0x0ff;
    		int v2 = (val & 0x0ff00) >>> 8;
    		int v3 = (val & 0x0ff0000) >>> 16;
    		int v4 = (val & 0xff000000) >>> 24;	    		

    		for(int i=0;i<4;i++) {
	    		vp[p_offset + i*4 + 0*stride] = v1;
	    		vp[p_offset + i*4 + 0*stride + 1] = v2;
	    		vp[p_offset + i*4 + 0*stride + 2] = v3;
	    		vp[p_offset + i*4 + 0*stride + 3] = v4;

	    		vp[p_offset + i*4 + 1*stride] = v1;
	    		vp[p_offset + i*4 + 1*stride + 1] = v2;
	    		vp[p_offset + i*4 + 1*stride + 2] = v3;
	    		vp[p_offset + i*4 + 1*stride + 3] = v4;
    		} // for   		
	        if(h==2) return;
    		for(int i=0;i<4;i++) {
	    		vp[p_offset + i*4 + 2*stride] = v1;
	    		vp[p_offset + i*4 + 2*stride + 1] = v2;
	    		vp[p_offset + i*4 + 2*stride + 2] = v3;
	    		vp[p_offset + i*4 + 2*stride + 3] = v4;

	    		vp[p_offset + i*4 + 3*stride] = v1;
	    		vp[p_offset + i*4 + 3*stride + 1] = v2;
	    		vp[p_offset + i*4 + 3*stride + 2] = v3;
	    		vp[p_offset + i*4 + 3*stride + 3] = v4;
    		} // for   		
	        	    		    
	    } // if
	}
	
	// For use with (*mv_cache) [][2] (int16_t) -- Note that it is considered to be signed.
	public static void fill_rectangle_mv_cache(int[][/*2*/] vp, int vp_offset, 
			 int w, int h, int stride, int val, int size){

		int p_offset = vp_offset;
	    w   = w * size;
	    stride *= size;
	    
	    if(w == 2) {
	    	int val16;
	   		if(size == 4) {
	   			val16 = (short)(val);
	   		} else {
	   			val16 = (short)((val & 0x0ff << 8) | (val & 0x0ff));
	   		} // if
	    	vp[p_offset + 0*stride/4][0]= val16;
	        if(h==1) return;
	        vp[p_offset + 1*stride/4][0]= val16;
	        if(h==2) return;
	        vp[p_offset + 2*stride/4][0]= val16;
	    } else if(w==4) {
	   		int v1;
	   		int v2;
	    	if(size == 4) {
	    		v1 = (short)(val & 0x0ffff);
	    		v2 = (short)((val & 0xffff0000) >>> 16);
	    	} else if(size == 2) {
	    		v1 = (short)(val & 0x0ffff);
	    		v2 = v1;
	    	} else {
	    		v1 = (short)((val & 0x0ff << 8) | (val & 0x0ff));
	    		v2 = (short)(v1);
	    	}
	   		vp[p_offset + 0*stride/4][0] = v1;
	   		vp[p_offset + 0*stride/4][1] = v2;
		        if(h==1) return;
	   		vp[p_offset + 1*stride/4][0] = v1;
	   		vp[p_offset + 1*stride/4][1] = v2;
		        if(h==2) return;
	   		vp[p_offset + 2*stride/4][0] = v1;
	   		vp[p_offset + 2*stride/4][1] = v2;
	   		vp[p_offset + 3*stride/4][0] = v1;
	   		vp[p_offset + 3*stride/4][1] = v2;

	    } else if(w==8) {
	    	int v1;
	    	int v2;
	    	if(size == 2) {
	    		v1 = (short)(val & 0x0ffff);
	    		v2 = v1;
	    	} else {
	    		v1 = (short)(val & 0x0ffff);
	    		v2 = (short)((val & 0xffff0000) >>> 16);
	    	} // if
	   		vp[p_offset + 0*stride/4][0] = v1;
	   		vp[p_offset + 0*stride/4][1] = v2;
	   		vp[p_offset + 0*stride/4 +1][0] = v1;
	   		vp[p_offset + 0*stride/4 +1][1] = v2;
		    if(h==1) return;
	   		vp[p_offset + 1*stride/4][0] = v1;
	   		vp[p_offset + 1*stride/4][1] = v2;
	   		vp[p_offset + 1*stride/4 +1][0] = v1;
	   		vp[p_offset + 1*stride/4 +1][1] = v2;
		    if(h==2) return;
	   		vp[p_offset + 2*stride/4][0] = v1;
	   		vp[p_offset + 2*stride/4][1] = v2;
	   		vp[p_offset + 2*stride/4 +1][0] = v1;
	   		vp[p_offset + 2*stride/4 +1][1] = v2;
	   		vp[p_offset + 3*stride/4][0] = v1;
	   		vp[p_offset + 3*stride/4][1] = v2;
	   		vp[p_offset + 3*stride/4 +1][0] = v1;
	   		vp[p_offset + 3*stride/4 +1][1] = v2;
	    
	    } else if(w==16) {
    		int v1 = (short)(val & 0x0ffff);
    		int v2 = (short)((val & 0xffff0000) >>> 16);
	   		for(int i=0;i<4;i++) {
	    		vp[p_offset + i + 0*stride/4][0] = v1;
	    		vp[p_offset + i + 0*stride/4][1] = v2;

	    		vp[p_offset + i + 1*stride/4][0] = v1;
	    		vp[p_offset + i + 1*stride/4][1] = v2;
	   		} // for   		
	        if(h==2) return;
	        for(int i=0;i<4;i++) {
	    		vp[p_offset + i + 2*stride/4][0] = v1;
	    		vp[p_offset + i + 2*stride/4][1] = v2;

	    		vp[p_offset + i + 3*stride/4][0] = v1;
	    		vp[p_offset + i + 3*stride/4][1] = v2;
	        } // for   		
	        	    		    
	    } // if
	}

	// For use with (*mvd_cache) [][2] (uint8_t) -- Should it be signed?
	public static void fill_rectangle_mvd_cache(int[][/*2*/] vp, int vp_offset, 
			 int w, int h, int stride, int val, int size){

		int p_offset = vp_offset;
	    w   = w * size;
	    stride *= size;
	    
	    if(w == 2) {
    		int v1;
    		int v2;
    		if(size == 4) {
		    	v1 = val & 0x0ff;
		    	v2 = (val >> 8) & 0x0ff;    			
    		} else {
		    	v1 = val & 0x0ff;
		    	v2 = v1;
    		} // if
	    	vp[p_offset + 0*stride/2][0]= v1;
	    	vp[p_offset + 0*stride/2][1]= v2;
	        if(h==1) return;
	        vp[p_offset + 1*stride/2][0]= v1;
	        vp[p_offset + 1*stride/2][1]= v2;
	        if(h==2) return;
	        vp[p_offset + 2*stride/2][0]= v1;
	        vp[p_offset + 2*stride/2][1]= v2;
	        vp[p_offset + 3*stride/2][0]= v1;	    	
	        vp[p_offset + 3*stride/2][1]= v2;	    	
	    } else if(w==4) {
    		int v1;
    		int v2;
    		int v3;
    		int v4 ;
	    	if(size == 4) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = (val & 0x0ff0000) >>> 16;
	    		v4 = (val & 0xff000000) >>> 24;
	    	} else if(size == 2) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = v1;
	    		v4 = v2;
	    	} else {
	    		v1 = val & 0x0ff;
	    		v2 = v1;
	    		v3 = v1;
	    		v4 = v1;	    		
	    	}
    		vp[p_offset + 0*stride/2][0] = v1;
    		vp[p_offset + 0*stride/2][1] = v2;
    		vp[p_offset + 0*stride/2 + 1][0] = v3;
    		vp[p_offset + 0*stride/2 + 1][1] = v4;
	        if(h==1) return;
    		vp[p_offset + 1*stride/2][0] = v1;
    		vp[p_offset + 1*stride/2][1] = v2;
    		vp[p_offset + 1*stride/2 + 1][0] = v3;
    		vp[p_offset + 1*stride/2 + 1][1] = v4;
	        if(h==2) return;
    		vp[p_offset + 2*stride/2][0] = v1;
    		vp[p_offset + 2*stride/2][1] = v2;
    		vp[p_offset + 2*stride/2 + 1][0] = v3;
    		vp[p_offset + 2*stride/2 + 1][1] = v4;
    		vp[p_offset + 3*stride/2][0] = v1;
    		vp[p_offset + 3*stride/2][1] = v2;
    		vp[p_offset + 3*stride/2 + 1][0] = v3;
    		vp[p_offset + 3*stride/2 + 1][1] = v4;

	    } else if(w==8) {
	    	int v1;
	    	int v2;
	    	int v3;
	    	int v4;
	    	if(size == 2) {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = v1;
	    		v4 = v2;
	    	} else {
	    		v1 = val & 0x0ff;
	    		v2 = (val & 0x0ff00) >>> 8;
	    		v3 = (val & 0x0ff0000) >>> 16;
	    		v4 = (val & 0xff000000) >>> 24;	    		
	    	} // if
    		vp[p_offset + 0*stride/2][0] = v1;
    		vp[p_offset + 0*stride/2][1] = v2;
    		vp[p_offset + 0*stride/2 + 1][0] = v3;
    		vp[p_offset + 0*stride/2 + 1][1] = v4;
    		vp[p_offset + 0*stride/2 + 2][0] = v1;
    		vp[p_offset + 0*stride/2 + 2][1] = v2;
    		vp[p_offset + 0*stride/2 + 3][0] = v3;
    		vp[p_offset + 0*stride/2 + 3][1] = v4;
	        if(h==1) return;
    		vp[p_offset + 1*stride/2][0] = v1;
    		vp[p_offset + 1*stride/2][1] = v2;
    		vp[p_offset + 1*stride/2 + 1][0] = v3;
    		vp[p_offset + 1*stride/2 + 1][1] = v4;
    		vp[p_offset + 1*stride/2 + 2][0] = v1;
    		vp[p_offset + 1*stride/2 + 2][1] = v2;
    		vp[p_offset + 1*stride/2 + 3][0] = v3;
    		vp[p_offset + 1*stride/2 + 3][1] = v4;
	        if(h==2) return;
    		vp[p_offset + 2*stride/2][0] = v1;
    		vp[p_offset + 2*stride/2][1] = v2;
    		vp[p_offset + 2*stride/2 + 1][0] = v3;
    		vp[p_offset + 2*stride/2 + 1][1] = v4;
    		vp[p_offset + 2*stride/2 + 2][0] = v1;
    		vp[p_offset + 2*stride/2 + 2][1] = v2;
    		vp[p_offset + 2*stride/2 + 3][0] = v3;
    		vp[p_offset + 2*stride/2 + 3][1] = v4;
    		vp[p_offset + 3*stride/2][0] = v1;
    		vp[p_offset + 3*stride/2][1] = v2;
    		vp[p_offset + 3*stride/2 + 1][0] = v3;
    		vp[p_offset + 3*stride/2 + 1][1] = v4;
    		vp[p_offset + 3*stride/2 + 2][0] = v1;
    		vp[p_offset + 3*stride/2 + 2][1] = v2;
    		vp[p_offset + 3*stride/2 + 3][0] = v3;
    		vp[p_offset + 3*stride/2 + 3][1] = v4;
	    
	    } else if(w==16) {
    		int v1 = val & 0x0ff;
    		int v2 = (val & 0x0ff00) >>> 8;
    		int v3 = (val & 0x0ff0000) >>> 16;
    		int v4 = (val & 0xff000000) >>> 24;	    		

    		for(int i=0;i<4;i++) {
	    		vp[p_offset + i*2 + 0*stride/2][0] = v1;
	    		vp[p_offset + i*2 + 0*stride/2][1] = v2;
	    		vp[p_offset + i*2 + 0*stride/2 + 1][0] = v3;
	    		vp[p_offset + i*2 + 0*stride/2 + 1][1] = v4;

	    		vp[p_offset + i*2 + 1*stride/2][0] = v1;
	    		vp[p_offset + i*2 + 1*stride/2][1] = v2;
	    		vp[p_offset + i*2 + 1*stride/2 + 1][0] = v3;
	    		vp[p_offset + i*2 + 1*stride/2 + 1][1] = v4;
    		} // for   		
	        if(h==2) return;
    		for(int i=0;i<4;i++) {
	    		vp[p_offset + i*2 + 2*stride/2][0] = v1;
	    		vp[p_offset + i*2 + 2*stride/2][1] = v2;
	    		vp[p_offset + i*2 + 2*stride/2 + 1][0] = v3;
	    		vp[p_offset + i*2 + 2*stride/2 + 1][1] = v4;

	    		vp[p_offset + i*2 + 3*stride/2][0] = v1;
	    		vp[p_offset + i*2 + 3*stride/2][1] = v2;
	    		vp[p_offset + i*2 + 3*stride/2 + 1][0] = v3;
	    		vp[p_offset + i*2 + 3*stride/2 + 1][1] = v4;
    		} // for   		
	        	    		    
	    } // if
	    
	}
	
}
