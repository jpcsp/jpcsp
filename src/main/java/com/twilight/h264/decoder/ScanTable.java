package com.twilight.h264.decoder;

public class ScanTable {
	/**
	 * Scantable.
	 */
    //const uint8_t *scantable;
    //uint8_t permutated[64];
    //uint8_t raster_end[64];
	public int[] scantable;
	public int[] permutated = new int[64];
	public int[] raster_end = new int[64];
}
