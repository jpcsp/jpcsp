package com.twilight.h264.decoder;

public class SEIDecoder {

	public static void ff_h264_reset_sei(H264Context h) {
	    h.sei_recovery_frame_cnt       = -1;
	    h.sei_dpb_output_delay         =  0;
	    h.sei_cpb_removal_delay        = -1;
	    h.sei_buffering_period_present =  0;
	}
	
}
