package com.twilight.h264.decoder;

public class AVComponentDescriptor {
    public int /*uint16_t */plane        =2;            ///< which of the 4 planes contains the component

	/**
	 * Number of elements between 2 horizontally consecutive pixels minus 1.
	 * Elements are bits for bitstream formats, bytes otherwise.
	 */
    public int /*uint16_t */step_minus1  =3;
	
	/**
	 * Number of elements before the component of the first pixel plus 1.
	 * Elements are bits for bitstream formats, bytes otherwise.
	 */
    public int /*uint16_t */offset_plus1 =3;
    public int /*uint16_t */shift        =3;            ///< number of least significant bits that must be shifted away to get the value
    public int /*uint16_t */depth_minus1 =4;            ///< number of bits in the component minus 1
    
    public AVComponentDescriptor(int _plane, int _step_minus1, int _offset_plus1
    		, int _shift, int _depth_minus1) {
    	plane = _plane;
    	step_minus1 = _step_minus1;
    	offset_plus1 = _offset_plus1;
    	shift = _shift;
    	depth_minus1 = _depth_minus1;
    }

}
