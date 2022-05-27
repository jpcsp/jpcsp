package com.twilight.h264.decoder;

public class AVRational {
    public int num; ///< numerator
    public int den; ///< denominator
    
    public AVRational(int _num, int _den) {
    	num = _num;
    	den = _den;
    }
    public AVRational(long _num, long _den) {
    	num = (int)_num;
    	den = (int)_den;
    }
}
