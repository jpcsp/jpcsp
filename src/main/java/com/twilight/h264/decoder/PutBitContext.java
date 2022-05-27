package com.twilight.h264.decoder;

public class PutBitContext {

	public int index;
	public long bit_buf;
	public int bit_left;
    public int[] buf;
    public int buf_ptr, buf_end;
    public int size_in_bits;
	
}
