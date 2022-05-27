package com.twilight.h264.decoder;

public class AVPixFmtDescriptor {
    public String name;
    
    public int /*uint8_t */nb_components;      ///< The number of components each pixel has, (1-4)

    /**
     * Amount to shift the luma width right to find the chroma width.
     * For YV12 this is 1 for example.
     * chroma_width = -((-luma_width) >> log2_chroma_w)
     * The note above is needed to ensure rounding up.
     * This value only refers to the chroma components.
     */
    public int /*uint8_t */log2_chroma_w;      ///< chroma_width = -((-luma_width )>>log2_chroma_w)

    /**
     * Amount to shift the luma height right to find the chroma height.
     * For YV12 this is 1 for example.
     * chroma_height= -((-luma_height) >> log2_chroma_h)
     * The note above is needed to ensure rounding up.
     * This value only refers to the chroma components.
     */
    public int /*uint8_t */log2_chroma_h;
    public int /*uint8_t */flags;

    /**
     * Parameters that describe how pixels are packed. If the format
     * has chroma components, they must be stored in comp[1] and
     * comp[2].
     */
    public AVComponentDescriptor[] comp = new AVComponentDescriptor[4];
}
