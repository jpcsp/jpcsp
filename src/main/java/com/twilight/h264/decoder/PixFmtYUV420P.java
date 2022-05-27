package com.twilight.h264.decoder;

public class PixFmtYUV420P extends AVPixFmtDescriptor {

	public PixFmtYUV420P() {
        name = "yuv420p";
        nb_components= 3;
        log2_chroma_w= 1;
        log2_chroma_h= 1;
        comp = new AVComponentDescriptor[] {
        		new AVComponentDescriptor(0,0,1,0,7),        /* Y */
        		new AVComponentDescriptor(1,0,1,0,7),        /* U */
        		new AVComponentDescriptor(2,0,1,0,7),        /* V */
        		new AVComponentDescriptor(0,0,0,0,0),        /* Filler */
        };		
	}
	
}
