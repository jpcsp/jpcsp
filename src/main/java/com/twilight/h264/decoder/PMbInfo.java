package com.twilight.h264.decoder;

public class PMbInfo{
    public int type;
    public short partition_count;
    
    public PMbInfo(int _type, int _partition_count) {
    	type = _type;
    	partition_count = (short)_partition_count;
    }
}
