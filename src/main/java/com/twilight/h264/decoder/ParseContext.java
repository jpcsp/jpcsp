package com.twilight.h264.decoder;

public class ParseContext {
    public int[] /*uint8_t **/buffer_base;
    public int buffer_offset;
    public int index;
    public int last_index;
    public int buffer_size;
    public int /*uint32_t*/state;             ///< contains the last few bytes in MSB order
    public int frame_start_found;
    public int overread;               ///< the number of bytes which where irreversibly read from the next frame
    public int overread_index;         ///< the index into ParseContext.buffer of the overread bytes
    public long state64;           ///< contains the last 8 bytes in MSB order
}
