package com.twilight.h264.decoder;

public class MotionEstContext {
	/**
	 * Motion estimation context.
	 */
    public DSPContext.Ih264_qpel_mc_func[][] qpel_put; //[16][4];
    public DSPContext.Ih264_qpel_mc_func[][]qpel_avg;  //[16][4];
   
    /*
    uint8_t *scratchpad;               ///< data area for the ME algo, so that the ME does not need to malloc/free
    uint8_t *best_mb;
    uint8_t *temp_mb[2];
    uint8_t *temp;
    */
    
    public int[] scratchpad;               ///< data area for the ME algo, so that the ME does not need to malloc/free
    public int[] best_mb;
    public int[][] temp_mb;
    public int[] temp;
    
    //uint32_t *map;                     ///< map to avoid duplicate evaluations
    //uint32_t *score_map;               ///< map to store the scores
    public int[] map;                     ///< map to avoid duplicate evaluations
    public int[] score_map;               ///< map to store the scores

	
}
