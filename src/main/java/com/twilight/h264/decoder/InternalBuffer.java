package com.twilight.h264.decoder;

public class InternalBuffer {
    public int last_pic_num;
    //uint8_t *base[4];
    //uint8_t *data[4];
    public int[][] base = new int[4][];
    public int[] data_offset = new int[4]; // offset into base ptr
    public int[] linesize = new int[4];
    public int width, height;
    public int /*enum PixelFormat */pix_fmt;
    
    public void copyInto(InternalBuffer ano) {
    	ano.last_pic_num = this.last_pic_num;
    	for(int i=0;i<4;i++) {
    		ano.base[i] = this.base[i];
    		ano.data_offset[i] = this.data_offset[i];
    		ano.linesize[i] = this.linesize[i];
    	} // for i
    	ano.width = this.width;
    	ano.height = this.height;
    	ano.pix_fmt = this.pix_fmt;
    }

}
