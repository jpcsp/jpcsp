package com.twilight.h264.decoder;

public class VLCcode {
	/*
    uint8_t bits;
    uint16_t symbol;
     * codeword, with the first bit-to-be-read in the msb
     * (even if intended for a little-endian bitstream reader) 
    uint32_t code;
    */
    
    public int bits;
    public int symbol;
    public long code;
    
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		if(arg0 == null || !(arg0 instanceof VLCcode) )
			return 0;
		return (int)(this.code - ((VLCcode)arg0).code);
	}
}
