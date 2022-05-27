package com.twilight.h264.decoder;

public class AVPicture {
	/**
	 * four components are given, that's all.
	 * the last component is alpha
	 */
    //uint8_t *data[4];
    //int linesize[4];       ///< number of bytes per line
    public int[][] data_base = new int[4][];
    public int[] data_offset = new int[4];
    public int[] linesize = new int[4];       ///< number of bytes per line
}
