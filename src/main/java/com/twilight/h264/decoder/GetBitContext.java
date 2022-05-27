package com.twilight.h264.decoder;


// This class is implemented using #ifdef ALT_BITSTREAM_READER and #ifdef ALT_BITSTREAM_READER_LE

public class GetBitContext {
	
	public static final int MIN_CACHE_BITS = 25;
	public static final int INIT_VLC_LE         = 2;
	public static final int INIT_VLC_USE_NEW_STATIC = 4;
	
	public static final int[] av_reverse ={
			0x000,0x080,0x040,0x0C0,0x020,0x0A0,0x060,0x0E0,0x010,0x090,0x050,0x0D0,0x030,0x0B0,0x070,0x0F0,
			0x008,0x088,0x048,0x0C8,0x028,0x0A8,0x068,0x0E8,0x018,0x098,0x058,0x0D8,0x038,0x0B8,0x078,0x0F8,
			0x004,0x084,0x044,0x0C4,0x024,0x0A4,0x064,0x0E4,0x014,0x094,0x054,0x0D4,0x034,0x0B4,0x074,0x0F4,
			0x00C,0x08C,0x04C,0x0CC,0x02C,0x0AC,0x06C,0x0EC,0x01C,0x09C,0x05C,0x0DC,0x03C,0x0BC,0x07C,0x0FC,
			0x002,0x082,0x042,0x0C2,0x022,0x0A2,0x062,0x0E2,0x012,0x092,0x052,0x0D2,0x032,0x0B2,0x072,0x0F2,
			0x00A,0x08A,0x04A,0x0CA,0x02A,0x0AA,0x06A,0x0EA,0x01A,0x09A,0x05A,0x0DA,0x03A,0x0BA,0x07A,0x0FA,
			0x006,0x086,0x046,0x0C6,0x026,0x0A6,0x066,0x0E6,0x016,0x096,0x056,0x0D6,0x036,0x0B6,0x076,0x0F6,
			0x00E,0x08E,0x04E,0x0CE,0x02E,0x0AE,0x06E,0x0EE,0x01E,0x09E,0x05E,0x0DE,0x03E,0x0BE,0x07E,0x0FE,
			0x001,0x081,0x041,0x0C1,0x021,0x0A1,0x061,0x0E1,0x011,0x091,0x051,0x0D1,0x031,0x0B1,0x071,0x0F1,
			0x009,0x089,0x049,0x0C9,0x029,0x0A9,0x069,0x0E9,0x019,0x099,0x059,0x0D9,0x039,0x0B9,0x079,0x0F9,
			0x005,0x085,0x045,0x0C5,0x025,0x0A5,0x065,0x0E5,0x015,0x095,0x055,0x0D5,0x035,0x0B5,0x075,0x0F5,
			0x00D,0x08D,0x04D,0x0CD,0x02D,0x0AD,0x06D,0x0ED,0x01D,0x09D,0x05D,0x0DD,0x03D,0x0BD,0x07D,0x0FD,
			0x003,0x083,0x043,0x0C3,0x023,0x0A3,0x063,0x0E3,0x013,0x093,0x053,0x0D3,0x033,0x0B3,0x073,0x0F3,
			0x00B,0x08B,0x04B,0x0CB,0x02B,0x0AB,0x06B,0x0EB,0x01B,0x09B,0x05B,0x0DB,0x03B,0x0BB,0x07B,0x0FB,
			0x007,0x087,0x047,0x0C7,0x027,0x0A7,0x067,0x0E7,0x017,0x097,0x057,0x0D7,0x037,0x0B7,0x077,0x0F7,
			0x00F,0x08F,0x04F,0x0CF,0x02F,0x0AF,0x06F,0x0EF,0x01F,0x09F,0x05F,0x0DF,0x03F,0x0BF,0x07F,0x0FF,
			};
	
	public static final int[] ff_golomb_vlc_len = {
			19,17,15,15,13,13,13,13,11,11,11,11,11,11,11,11,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,
			7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
			5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
			5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
			3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
			3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
			3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
			3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
			1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1
			};

	public static final int[] ff_ue_golomb_vlc_code = {
			32,32,32,32,32,32,32,32,31,32,32,32,32,32,32,32,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,
			 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9,10,10,10,10,11,11,11,11,12,12,12,12,13,13,13,13,14,14,14,14,
			 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
			 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
			 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
			};

	public static final int[] ff_se_golomb_vlc_code = {
			 17, 17, 17, 17, 17, 17, 17, 17, 16, 17, 17, 17, 17, 17, 17, 17,  8, -8,  9, -9, 10,-10, 11,-11, 12,-12, 13,-13, 14,-14, 15,-15,
			  4,  4,  4,  4, -4, -4, -4, -4,  5,  5,  5,  5, -5, -5, -5, -5,  6,  6,  6,  6, -6, -6, -6, -6,  7,  7,  7,  7, -7, -7, -7, -7,
			  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2,
			  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,
			  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
			  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
			 -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			 -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
			};


	public static final int[] ff_ue_golomb_len = {
			 1, 3, 3, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 7, 7, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,11,
			11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,13,
			13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,
			13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,15,
			15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,
			15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,
			15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,
			15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,15,17,
			};

    public int[] buffer; // const uint8_t *buffer, *buffer_end;
    public int buffer_offset;
    public int buffer_end;
    public int index;
    //public int buffer_ptr;
    public long cache;
    //public int bit_count;
    public long cache0;
    public long cache1;
    public int size_in_bits;

    public static final long bitswap_32(long x) {
        return av_reverse[(int)(x&0x0FF)]<<24
             | av_reverse[(int)((x>>>8)&0x0FF)]<<16
             | av_reverse[(int)((x>>>16)&0x0FF)]<<8
             | av_reverse[(int)(x>>>24)];
    }
    
    // Copy ctor??
    public GetBitContext makeCopy() {
    	GetBitContext ret = new GetBitContext();
    	ret.buffer = buffer;
    	ret.buffer_offset = buffer_offset;
    	ret.buffer_end = buffer_end;
    	ret.index = index;
    	//ret.buffer_ptr = buffer_ptr;
    	ret.cache = cache;
    	//ret.bit_count = bit_count;
    	ret.cache0 = cache0;
    	ret.cache1 = cache1;
    	ret.size_in_bits = size_in_bits;
    	return ret;
    }
    
    public /*inline*/ int get_bits_count(){
        return this.index;
    }

    public /*inline*/ void skip_bits_long(int n){
        this.index += n;
    }
    
    /**
     * read mpeg1 dc style vlc (sign bit + mantisse with no MSB).
     * if MSB not set it is negative
     * @param n length in bits
     * @author BERO
     */
    public /*inline*/ int get_xbits(int n){
        int sign;
        long cache;
        
        //OPEN_READER(re, s)
        int re_index= this.index;
        long re_cache= 0;
        
        //UPDATE_CACHE(re, s)
        //re_cache= AV_RL32( ((const uint8_t *)(gb)->buffer)+(name##_index>>3) ) >> (name##_index&0x07);
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
        
        //cache = GET_CACHE(re,s);
        cache = (re_cache);

        //sign=(~cache)>>31;
        // we use long
        sign=(int)((~cache)>>63);
        
        //LAST_SKIP_BITS(re, s, n)
        re_index += (n);
        
        //CLOSE_READER(re, s)
        this.index= re_index;
        
        //return (NEG_USR32(sign ^ cache, n) ^ sign) - sign;
        //System.out.println("get_xbit: "+ (( (((int)(sign ^ cache))>>(32-(n))) ^ sign) - sign) );
        return  ( (((int)(sign ^ cache))>>(32-(n))) ^ sign) - sign;
    }

    public /*inline*/ int get_sbits(int n){
        int tmp;
        //OPEN_READER(re, s)
        int re_index= this.index;
        long re_cache= 0;

        //UPDATE_CACHE(re, s)
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
                
        //tmp= SHOW_SBITS(re, s, n);
        ////tmp = ((( int32_t)(re_cache))>>(32-(n)))
        tmp = ((( int)(re_cache))>>(32-(n)));
        
        //LAST_SKIP_BITS(re, s, n)
        re_index += (n);

        //CLOSE_READER(re, s)
        this.index= re_index;

        //System.out.println("get_sbits: "+ (tmp) );        
        return tmp;
    }

    /**
     * reads 1-17 bits.
     * Note, the alt bitstream reader can read up to 25 bits, but the libmpeg2 reader can't
     */
    public /*inline*/ long get_bits(int n, String message){
        long tmp;
        //OPEN_READER(re, s)
        int re_index= this.index;
        long re_cache= 0;

        //UPDATE_CACHE(re, s)
        int pos = buffer_offset + (re_index>>3);

        // DebugTool.printDebugString("---------   Reading 4 bytes from pos: "+pos+"\n");
        
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
       
        //tmp= SHOW_UBITS(re, s, n);
        ////tmp = NEG_USR32(re_cache, n)
        tmp = (((re_cache))>>(32-(n)));
        
        //LAST_SKIP_BITS(re, s, n)
        re_index += (n);

        //CLOSE_READER(re, s)
        this.index= re_index;

        //System.out.println("get_bits(,"+n+","+message+"): "+ (tmp) );        
        return tmp;
    }

    /**
     * shows 1-17 bits.
     * Note, the alt bitstream reader can read up to 25 bits, but the libmpeg2 reader can't
     */
    public /*inline*/ long show_bits(int n){
        long tmp;
        //OPEN_READER(re, s)
        int re_index= this.index;
        long re_cache= 0;

        //UPDATE_CACHE(re, s)
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
        
        //tmp= SHOW_UBITS(re, s, n);
        ////tmp = NEG_USR32(re_cache, n)
        tmp = (((re_cache))>>(32-(n)));

        return tmp;
    }

    public /*inline*/ void skip_bits(int n){
     //Note gcc seems to optimize this to s->index+=n for the ALT_READER :))
    	//OPEN_READER(re, s)
        int re_index= this.index;
        long re_cache= 0;

        //UPDATE_CACHE(re, s)
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
                
        //LAST_SKIP_BITS(re, s, n)
        re_index += (n);

        //CLOSE_READER(re, s)
        this.index= re_index;
    }

    public /*inline*/ long get_bits1(String message){
    //#ifdef ALT_BITSTREAM_READER
        int index= this.index;
        int result= this.buffer[ this.buffer_offset + (index>>3) ];
    //#ifdef ALT_BITSTREAM_READER_LE
    //    result>>= (index&0x07);
    //    result&= 1;
    //#else
        //result<<= (index&0x07);
        //result>>= (8 - 1);
        // we use int!
        result<<= ((index&0x07) + 24);
        result>>= (32 - 1);
    //#endif
        index++;
        this.index= index;

        //System.out.println("get_bits1("+message+"): "+ ((result!=0)?1:0) );        
        return (result!=0)?1:0;
    //#else
    //    return get_bits(s, 1);
    //#endif
    }

    public /*inline*/ long show_bits1(){
        return show_bits(1);
    }

    public /*inline*/ void skip_bits1(){
        skip_bits(1);
    }
        
    /**
     * reads 0-32 bits.
     */
    public /*inline*/ long get_bits_long(int n, String message){
        if(n<=MIN_CACHE_BITS) return get_bits(n, message);
        else{
//    #ifdef ALT_BITSTREAM_READER_LE
//            long ret= get_bits(16);
//            return ret | (get_bits(n-16) << 16);
//    #else
            long ret = get_bits(16, message) << (n-16);
            long ret2 = get_bits(n-16, message);

            ////System.out.println("get_bits_long(,"+n+","+message+"): "+ (ret | get_bits(n-16, message)) );        
            return (ret | ret2);
//    #endif
        }
    }
    
    /**
     * reads 0-32 bits as a signed integer.
     */
    public /*inline*/ int get_sbits_long(int n, String message) {
        //return sign_extend(get_bits_long(n), n);
        return (((int)get_bits_long(n, message)) << ((8 * 4) - n)) >> ((8 * 4) - n);
    }
    
    /**
     * shows 0-32 bits.
     */
    public /*inline*/ long show_bits_long(int n){
        if(n<=MIN_CACHE_BITS) return show_bits(n);
        else{
            GetBitContext gb= makeCopy();
            return gb.get_bits_long(n, "");
        }
    }
    
    public /*inline*/ long check_marker(String msg)
    {
        long bit= get_bits1(msg);
        if(bit == 0)
            System.err.println("Marker bit missing: " + msg);

        return bit;
    }

    /**
     * init GetBitContext.
     * @param buffer bitstream buffer, must be FF_INPUT_BUFFER_PADDING_SIZE bytes larger then the actual read bits
     * because some optimized bitstream readers read 32 or 64 bit at once and could read over the end
     * @param bit_size the size of the buffer in bits
     *
     * While GetBitContext stores the buffer size, for performance reasons you are
     * responsible for checking for the buffer end yourself (take advantage of the padding)!
     */
    public /*inline*/ void init_get_bits(
                       /*const uint8_t **/ int[] buffer, int buffer_offset, int bit_size)
    {
        int buffer_size= (bit_size+7)>>3;
        if(buffer_size < 0 || bit_size < 0) {
            buffer_size = bit_size = 0;
            buffer = null;
        }

        this.buffer= buffer;
        this.buffer_offset = buffer_offset;
        this.size_in_bits= bit_size;
        this.buffer_end= buffer_offset + buffer_size;
    //#ifdef ALT_BITSTREAM_READER
        this.index=0;
    //#elif defined LIBMPEG2_BITSTREAM_READER
    //    s->buffer_ptr = (uint8_t*)((intptr_t)buffer&(~1));
    //    s->bit_count = 16 + 8*((intptr_t)buffer&1);
    //    skip_bits_long(s, 0);
    //#elif defined A32_BITSTREAM_READER
    //    s->buffer_ptr = (uint32_t*)((intptr_t)buffer&(~3));
    //    s->bit_count = 32 + 8*((intptr_t)buffer&3);
    //    skip_bits_long(s, 0);
    //#endif
    }

    public /*inline*/ void align_get_bits()
    {
        int n= (-get_bits_count()) & 7;
        if(n != 0) skip_bits(n);
    }
    
    public /*inline*/ long decode012(){
        long n;
        n = get_bits1("");
        if (n == 0)
            return 0;
        else
            return get_bits1("") + 1;
    }

    public /*inline*/ long decode210(){
        if (get_bits1("")!=0)
            return 0;
        else
            return 2 - get_bits1("");
    }

    public /*inline*/ int get_bits_left()
    {
        return size_in_bits - get_bits_count();
    }
    
    public static int alloc_table(VLC vlc, int size, int use_static)
    {
        int index;
        index = vlc.table_size;
        vlc.table_size += size;
        if (vlc.table_size > vlc.table_allocated) {
            if(use_static !=0 ) {
                //abort(); //cant do anything, init_vlc() is used with too little memory
                return 0;
            } 
            vlc.table_allocated += (1 << vlc.bits);
            short[][/*2*/] newTab = new short[2 * vlc.table_allocated][2];
            for(int i=0;i<vlc.table_base.length;i++) {
            	newTab[i][0] = vlc.table_base[i][0];
            	newTab[i][1] = vlc.table_base[i][1];
            } // for i
            vlc.table_base = newTab;
            //vlc.table = av_realloc(vlc->table,
            //                        sizeof(VLC_TYPE) * 2 * vlc->table_allocated);
            if (null == vlc.table_base)
                return -1;
        }
        return index;
    }
    
        
    /**
     * Build VLC decoding tables suitable for use with get_vlc().
     *
     * @param vlc            the context to be initted
     *
     * @param table_nb_bits  max length of vlc codes to store directly in this table
     *                       (Longer codes are delegated to subtables.)
     *
     * @param nb_codes       number of elements in codes[]
     *
     * @param codes          descriptions of the vlc codes
     *                       These must be ordered such that codes going into the same subtable are contiguous.
     *                       Sorting by VLCcode.code is sufficient, though not necessary.
     */
    public static int build_table(VLC vlc, int table_nb_bits, int nb_codes,
                           VLCcode[] codes_base, int codes_offset, int flags)
    {
        int table_size, table_index, index, code_prefix, symbol, subtable_bits;
        int i, k, n, nb, inc;
        long j;
        //uint32_t code;
        long code;
        //VLC_TYPE (*table)[2];
        short[][] table_base;
        int table_offset;

        table_size = 1 << table_nb_bits;
        table_index = alloc_table(vlc, table_size, flags & INIT_VLC_USE_NEW_STATIC);
    /*
    #ifdef DEBUG_VLC
        av_log(NULL,AV_LOG_DEBUG,"new table index=%d size=%d\n",
               table_index, table_size);
    #endif
    */
        if (table_index < 0)
            return -1;
        table_base = vlc.table_base;
        table_offset = vlc.table_offset + table_index;

        for (i = 0; i < table_size; i++) {
            table_base[table_offset + i][1] = 0; //bits
            table_base[table_offset + i][0] = -1; //codes
        }

        /* first pass: map codes and compute auxillary table sizes */
        for (i = 0; i < nb_codes; i++) {
            n = codes_base[codes_offset + i].bits;
            code = codes_base[codes_offset + i].code;
            symbol = codes_base[codes_offset + i].symbol;

        	//System.out.println("i="+i+" n="+n+" code="+code);

            if (n <= table_nb_bits) {
                /* no need to add another table */
            	//!!! Prevent 32-Bit value in negative interval
            	long tmp = code & 0xffffffffL;
            	j = (int)(tmp >> (32 - table_nb_bits));
                nb = 1 << (table_nb_bits - n);
                inc = 1;
                if ((flags & INIT_VLC_LE)!=0) {
                    j = bitswap_32(code);
                    inc = 1 << n;
                }
                for (k = 0; k < nb; k++) {
                	
                	//System.out.println("["+j+"]: code="+i+" n="+n);
                    
                    if (table_base[(int)(table_offset + j)][1] /*bits*/ != 0) {
                    	//System.out.println("incorrect codes.");
                        return -1;
                    }
                    table_base[(int)(table_offset + j)][1] = (short)n; //bits
                    table_base[(int)(table_offset + j)][0] = (short)symbol;
                    j += inc;
                }
            } else {
                /* fill auxiliary table recursively */
                n -= table_nb_bits;
                long tmp = code & 0xffffffffL;
                code_prefix = (int)(tmp >> (32 - table_nb_bits));
                subtable_bits = n;
                codes_base[codes_offset + i].bits = n;
                codes_base[codes_offset + i].code = code << table_nb_bits;
                for (k = i+1; k < nb_codes; k++) {
                    n = codes_base[codes_offset + k].bits - table_nb_bits;
                    if (n <= 0)
                        break;
                    code = codes_base[codes_offset + k].code;
                    if (code >> (32 - table_nb_bits) != code_prefix)
                        break;
                    codes_base[codes_offset + k].bits = n;
                    codes_base[codes_offset + k].code = code << table_nb_bits;
                    subtable_bits = Math.max(subtable_bits, n);
                }
                subtable_bits = Math.min(subtable_bits, table_nb_bits);
                j = ((flags & INIT_VLC_LE)!=0) ? (bitswap_32(code_prefix) >> (32 - table_nb_bits)) : code_prefix;
                table_base[(int)(table_offset + j)][1] = (short)-subtable_bits;

                //System.out.println("["+j+"]: n="+codes_base[codes_offset + i].bits + table_nb_bits+"(subtable)");
                
                index = build_table(vlc, subtable_bits, k-i, codes_base, codes_offset+i, flags);
                if (index < 0)
                    return -1;
                /* note: realloc has been done, so reload tables */
                table_base = vlc.table_base;
                table_offset = vlc.table_offset + table_index;
                table_base[(int)(table_offset + j)][0] = (short)index; //code
                i = k-1;
            }
        }
        return table_index;
    }
            
    /**
     * parses a vlc code, faster then get_vlc()
     * @param bits is the number of bits which will be read at once, must be
     *             identical to nb_bits in init_vlc()
     * @param max_depth is the number of times bits bits must be read to completely
     *                  read the longest vlc code
     *                  = (max_vlc_length + bits - 1) / bits
     */
    public int get_vlc2(short[][/*2*/] table_base, int table_offset,
                                      int bits, int max_depth, String message)
    {
        int code;

        //OPEN_READER(re, s)
        int re_index= this.index;
        long re_cache= 0;
        
        //UPDATE_CACHE(re, s)
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
        
        //GET_VLC(code, re, s, table, bits, max_depth)
        /**
        *
        * If the vlc code is invalid and max_depth=1, then no bits will be removed.
        * If the vlc code is invalid and max_depth>1, then the number of bits removed
        * is undefined.
        */
       //#define GET_VLC(code, name, gb, table, bits, max_depth)
       {
           int n, nb_bits;
           /*unsigned*/ int index;
       
           //index= SHOW_UBITS(name, gb, bits);
           ////index = NEG_USR32(re_cache, bits)
           index = (int)((((re_cache))>>(32-(bits))));
           
           code = table_base[table_offset + index][0];
           n    = table_base[table_offset + index][1];

           //if(// DebugTool.logCount == 8635) {
        	   // DebugTool.printDebugString("  get_vlc2(LEVEL0)=> index="+index+", code="+code+", n="+n+", bits="+bits+"\n");	                	
           //} // if
           
           if(max_depth > 1 && n < 0){
               //LAST_SKIP_BITS(name, gb, bits)
               re_index += (bits);

               //UPDATE_CACHE(name, gb)
		        pos = buffer_offset + (re_index>>3);
		        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
		        re_cache = buffer[pos+0];
		        re_cache = (re_cache << 8) |  buffer[pos+1];
		        re_cache = (re_cache << 8) |  buffer[pos+2];
		        re_cache = (re_cache << 8) |  buffer[pos+3];
		        re_cache = (re_cache << (re_index & 0x07));
		        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
		        
       
               nb_bits = -n;
       
               //index= SHOW_UBITS(name, gb, nb_bits) + code;
               ////index = NEG_USR32(re_cache, nb_bits) + code
               index = (int)((((re_cache))>>(32-(nb_bits)))) + code;
               
               code = table_base[table_offset + index][0];
               n    = table_base[table_offset + index][1];

              // if(// DebugTool.logCount == 8635) {
            	   // DebugTool.printDebugString("  get_vlc2(LEVEL1)=> index="+index+", code="+code+", n="+n+"\n");	                	
              // } // if
               
               if(max_depth > 2 && n < 0){
                   //LAST_SKIP_BITS(name, gb, nb_bits)
                   re_index += (nb_bits);

                   //UPDATE_CACHE(name, gb)
                   pos = buffer_offset + (re_index>>3);
                   //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
                   re_cache = buffer[pos+0];
                   re_cache = (re_cache << 8) |  buffer[pos+1];
                   re_cache = (re_cache << 8) |  buffer[pos+2];
                   re_cache = (re_cache << 8) |  buffer[pos+3];
                   re_cache = (re_cache << (re_index & 0x07));
                   re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
       
                   nb_bits = -n;
       
                   //index= SHOW_UBITS(name, gb, nb_bits) + code;
                   ////index = NEG_USR32(re_cache, nb_bits) + code
                   index = (int)((((re_cache))>>(32-(nb_bits)))) + code;
                   
                   code = table_base[table_offset + index][0];
                   n    = table_base[table_offset + index][1];

                  // if(// DebugTool.logCount == 8635) {
                	   // DebugTool.printDebugString("  get_vlc2(LEVEL2)=> index="+index+", code="+code+", n="+n+"\n");	                	
                  // } // if
               
               }
           }
           //SKIP_BITS(name, gb, n)
           re_cache >>= (n);
           re_index += (n);

          // //System.out.println("get_vlc2(,"+n+","+message+"): "+ code);           
       }

        //CLOSE_READER(re, s)
        // DebugTool.printDebugString("  stream => update re_index = "+ re_index+"\n");
        this.index= re_index;
        return code;
    }
    
    
    /**
     * read unsigned exp golomb code.
     */
    public int get_ue_golomb(String message){
    	
        /*unsigned */long buf;
        int log;

//        OPEN_READER(re, gb);
        int re_index= this.index;
        long re_cache= 0;

//        UPDATE_CACHE(re, gb);
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = (re_cache & 0xffffffffl); // Prevent 32-Bit over flow.

//        buf=GET_CACHE(re, gb);
        buf = (re_cache);

        if(buf >= (1l<<27)){
            buf >>= 32 - 9;
//            LAST_SKIP_BITS(re, gb, ff_golomb_vlc_len[buf]);
        	re_index += (ff_golomb_vlc_len[(int)buf]);

//        	CLOSE_READER(re, gb);
            this.index= re_index;

            //System.out.println("get_ue_golomb(,"+ff_golomb_vlc_len[(int)buf]+","+message+"): "+ (ff_ue_golomb_vlc_code[(int)buf]) );        
            return ff_ue_golomb_vlc_code[(int)buf];
        }else{
            log= 2*CAVLCContext.av_log2(buf) - 31;
            buf>>= log;
            buf--;
//            LAST_SKIP_BITS(re, gb, 32 - log);
            re_index += (32 - log);

//            CLOSE_READER(re, gb);
            this.index= re_index;

            //System.out.println("get_ue_golomb(,"+(32 - log)+","+message+"): "+ ((int)buf) );                    
            return (int)buf;
        }
    }

     /**
     * read unsigned exp golomb code, constraint to a max of 31.
     * the return value is undefined if the stored value exceeds 31.
     */
    public int get_ue_golomb_31(String message){
        /*unsigned */long buf;

//        OPEN_READER(re, gb);
        int re_index= this.index;
        long re_cache= 0;

//        UPDATE_CACHE(re, gb);
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((long)(((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos]<<0)) & 0xffffffff)) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.

//        buf=GET_CACHE(re, gb);
        //buf = (int)(re_cache >> (32-9));
        buf = re_cache >> (32-9);

        //buf >>= 32 - 9;
//        LAST_SKIP_BITS(re, gb, ff_golomb_vlc_len[buf]);
        re_index += (ff_golomb_vlc_len[(int)buf]);

//        CLOSE_READER(re, gb);
        this.index= re_index;

        //System.out.println("get_ue_golomb_31(,"+(ff_golomb_vlc_len[(int)buf])+","+message+"): "+ (ff_ue_golomb_vlc_code[(int)buf]) );               
        return ff_ue_golomb_vlc_code[(int)buf];
    }

	/**
	 * read signed exp golomb code.
	 */
	public int get_se_golomb(String message){
	    /*unsigned */long buf;
	    int log;

//	    OPEN_READER(re, gb);
        int re_index= this.index;
        long re_cache= 0;

//	    UPDATE_CACHE(re, gb);
        int pos = buffer_offset + (re_index>>3);
        //re_cache = ((buffer[pos+3]<<24)|(buffer[pos+2]<<16)|(buffer[pos+1]<<8)|(buffer[pos])) >> (re_index&0x07);
        re_cache = buffer[pos+0];
        re_cache = (re_cache << 8) |  buffer[pos+1];
        re_cache = (re_cache << 8) |  buffer[pos+2];
        re_cache = (re_cache << 8) |  buffer[pos+3];
        re_cache = (re_cache << (re_index & 0x07));
        re_cache = re_cache & 0xffffffffl; // Prevent 32-Bit over flow.
        
//        buf=GET_CACHE(re, gb);
        buf = (re_cache);

	    if(buf >= (1<<27)){
	        buf >>= 32 - 9;
//	        LAST_SKIP_BITS(re, gb, ff_golomb_vlc_len[buf]);
	    	re_index += (ff_golomb_vlc_len[(int)buf]);

//	    	CLOSE_READER(re, gb);
	        this.index= re_index;

	        //System.out.println("get_se_golomb(,"+(ff_golomb_vlc_len[(int)buf])+","+message+"): "+ (ff_se_golomb_vlc_code[(int)buf]) );               	        
	        return ff_se_golomb_vlc_code[(int)buf];
	    }else{
	        log= 2*CAVLCContext.av_log2(buf) - 31;
	        buf>>= log;

//	        LAST_SKIP_BITS(re, gb, 32 - log);
        	re_index += (32 - log);

//        	CLOSE_READER(re, gb);
            this.index= re_index;

	        if((buf&1)!=0) buf= -(buf>>1);
	        else      buf=  (buf>>1);

	        //System.out.println("get_se_golomb(,"+(32 - log)+","+message+"): "+ ((int)buf) );               	        	        
	        return (int)buf;
	    }
	}
    
    
}
