package com.twilight.h264.decoder;

public class H264Decoder {
	
	public static final int CODEC_CAP_DR1             =0x0002;
	/* If 'parse_only' field is true, then avcodec_parse_frame() can be used. */
	public static final int CODEC_CAP_PARSE_ONLY      =0x0004;
	public static final int CODEC_CAP_TRUNCATED       =0x0008;
	/**
	 * Codec has a nonzero delay and needs to be fed with NULL at the end to get the delayed data.
	 * If this is not set, the codec is guaranteed to never be fed with NULL data.
	 */
	public static final int CODEC_CAP_DELAY           =0x0020;

	
	/**
     * Name of the codec implementation.
     * The name is globally unique among encoders and among decoders (but an
     * encoder and a decoder can share the same name).
     * This is the primary way to find a codec from the user perspective.
     */
    public String name = "h264";
    public int id = H264PredictionContext.CODEC_ID_H264;
    //public int priv_data_size;
    
    public int init(MpegEncContext s) {
    	return s.ff_h264_decode_init();
    }
    
    public int close(MpegEncContext s) {
    	return s.ff_h264_decode_end();
    }
    
    public int decode(MpegEncContext s, AVFrame outdata, int[] outdata_size, AVPacket avpkt) {
    	return s.priv_data.decode_frame(outdata, outdata_size, avpkt);
    }
    
    /**
     * Codec capabilities.
     * see CODEC_CAP_*
     */
    public int capabilities = CODEC_CAP_DR1 | CODEC_CAP_DELAY;
    //struct AVCodec *next;
    /**
     * Flush buffers.
     * Will be called when seeking
     */
    public void flush(MpegEncContext s){
    	s.flush_dpb();
    }
    
    public AVRational[] supported_framerates; ///< array of supported framerates, or NULL if any, array is terminated by {0,0}
    public int[] pix_fmts;       ///< array of supported pixel formats, or NULL if unknown, array is terminated by -1
    /**
     * Descriptive name for the codec, meant to be more human readable than name.
     * You should use the NULL_IF_CONFIG_SMALL() macro to define it.
     */
    public String long_name = "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10";
    public int[] supported_samplerates;       ///< array of supported audio samplerates, or NULL if unknown, array is terminated by 0
    public int[] sample_fmts; ///< array of supported sample formats, or NULL if unknown, array is terminated by -1
    public long[] channel_layouts;         ///< array of support channel layouts, or NULL if unknown. array is terminated by 0
    /*uint8_t */public int max_lowres;                     ///< maximum value for lowres supported by the decoder
    //AVClass *priv_class;                    ///< AVClass for the private context
    //const AVProfile *profiles;              ///< array of recognized profiles, or NULL if unknown, array is terminated by {FF_PROFILE_UNKNOWN}	
}
