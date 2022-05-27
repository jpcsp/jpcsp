package com.twilight.h264.decoder;

public class AVPacket {

    /**
     * Presentation timestamp in AVStream->time_base units; the time at which
     * the decompressed packet will be presented to the user.
     * Can be AV_NOPTS_VALUE if it is not stored in the file.
     * pts MUST be larger or equal to dts as presentation cannot happen before
     * decompression, unless one wants to view hex dumps. Some formats misuse
     * the terms dts and pts/cts to mean something different. Such timestamps
     * must be converted to true pts/dts before they are stored in AVPacket.
     */
	public long pts;
    /**
     * Decompression timestamp in AVStream->time_base units; the time at which
     * the packet is decompressed.
     * Can be AV_NOPTS_VALUE if it is not stored in the file.
     */
	public long dts;
    /*uint8_t **/public int[] data_base;
    public int data_offset;
    public int   size;
    public int   stream_index;
    public int   flags;
    /**
     * Duration of this packet in AVStream->time_base units, 0 if unknown.
     * Equals next_pts - this_pts in presentation order.
     */
    public int   duration;
    //void  (*destruct)(struct AVPacket *);
    //void  *priv;
    public long pos;                            ///< byte position in stream, -1 if unknown

    /**
     * Time difference in AVStream->time_base units from the pts of this
     * packet to the point at which the output from the decoder has converged
     * independent from the availability of previous frames. That is, the
     * frames are virtually identical no matter if decoding started from
     * the very first frame or from this keyframe.
     * Is AV_NOPTS_VALUE if unknown.
     * This field is not the display duration of the current packet.
     * This field has no meaning if the packet does not have AV_PKT_FLAG_KEY
     * set.
     *
     * The purpose of this field is to allow seeking in streams that have no
     * keyframes in the conventional sense. It corresponds to the
     * recovery point SEI in H.264 and match_time_delta in NUT. It is also
     * essential for some types of subtitle streams to ensure that all
     * subtitles are correctly displayed after seeking.
     */
    public long convergence_duration;
    
    public void av_init_packet() {
        this.pts   = MpegEncContext.AV_NOPTS_VALUE;
        this.dts   = MpegEncContext.AV_NOPTS_VALUE;
        this.pos   = -1;
        this.duration = 0;
        this.convergence_duration = 0;
        this.flags = 0;
        this.stream_index = 0;
        //this.destruct= null;
    }
    
	
}
