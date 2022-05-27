package com.twilight.h264.decoder;

public class IMbInfo{
    public int type;
    public short pred_mode;
    public short cbp;
    
    public IMbInfo(int _type, int _pred_mode, int _cbp) {
    	type = _type;
    	pred_mode = (short)_pred_mode;
    	cbp = (short)_cbp;
    }
}
