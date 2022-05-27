/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.media.codec.atrac3plus;

/*
 * Based on the FFmpeg version from Maxim Poliakovski.
 * All credits go to him.
 */
public class Atrac3plusData2 {
	/** VLC tables for wordlen */
	static final int atrac3p_wl_huff_code1[] = new int[] { 0, 2, 3 };

	static final int atrac3p_wl_huff_bits1[] = new int[] { 1, 2, 2 };

	static final int atrac3p_wl_huff_xlat1[] = new int[] { 0, 1, 7 };

	static final int atrac3p_wl_huff_code2[] = new int[] { 0, 4, 5, 6, 7 };

	static final int atrac3p_wl_huff_bits2[] = new int[] { 1, 3, 3, 3, 3 };

	static final int atrac3p_wl_huff_xlat2[] = new int[] { 0, 1, 2, 6, 7 };

	static final int atrac3p_wl_huff_code3[] = new int[] {
	    0, 4, 0xC, 0x1E, 0x1F, 0xD, 0xE, 5
	};

	static final int atrac3p_wl_huff_bits3[] = new int[] { 1, 3, 4, 5, 5, 4, 4, 3 };

	static final int atrac3p_wl_huff_code4[] = new int[] {
	    0, 4, 0xC, 0xD, 0x1E, 0x1F, 0xE, 5
	};

	static final int atrac3p_wl_huff_bits4[] = new int[] { 1, 3, 4, 4, 5, 5, 4, 3 };

	/** VLC tables for scale factor indexes */
	static final int atrac3p_sf_huff_code1[] = new int[] {
	        0,     2,     3,     4,     5,   0xC,   0xD,  0xE0,
	     0xE1,  0xE2,  0xE3,  0xE4,  0xE5,  0xE6, 0x1CE, 0x1CF,
	    0x1D0, 0x1D1, 0x1D2, 0x1D3, 0x1D4, 0x1D5, 0x1D6, 0x1D7,
	    0x1D8, 0x1D9, 0x1DA, 0x1DB, 0x1DC, 0x1DD, 0x1DE, 0x1DF,
	    0x1E0, 0x1E1, 0x1E2, 0x1E3, 0x1E4, 0x1E5, 0x1E6, 0x1E7,
	    0x1E8, 0x1E9, 0x1EA, 0x1EB, 0x1EC, 0x1ED, 0x1EE, 0x1EF,
	    0x1F0, 0x1F1, 0x1F2, 0x1F3, 0x1F4, 0x1F5, 0x1F6, 0x1F7,
	    0x1F8, 0x1F9, 0x1FA, 0x1FB, 0x1FC, 0x1FD, 0x1FE, 0x1FF
	};

	static final int atrac3p_sf_huff_bits1[] = new int[] {
	    2, 3, 3, 3, 3, 4, 4, 8, 8, 8, 8, 8, 8, 8, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9
	};

	static final int atrac3p_sf_huff_xlat1[] = new int[] {
	     0,  1, 61, 62, 63,  2, 60,  3,  4,  5,  6, 57, 58, 59,  7,  8,
	     9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
	    25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
	    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56
	};

	static final int atrac3p_sf_huff_xlat2[] = new int[] {
	    0,   1,  2, 62, 63,  3, 61,  4,  5,  6, 57, 58, 59, 60,  7,  8,
	    9,  10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
	    25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
	    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56
	};

	static final int atrac3p_sf_huff_code2[] = new int[] {
	        0,     4,  0x18,  0x19,  0x70, 0x1CA, 0x1CB, 0x1CC,
	    0x1CD, 0x1CE, 0x1CF, 0x1D0, 0x1D1, 0x1D2, 0x1D3, 0x1D4,
	    0x1D5, 0x1D6, 0x1D7, 0x1D8, 0x1D9, 0x1DA, 0x1DB, 0x1DC,
	    0x1DD, 0x1DE, 0x1DF, 0x1E0, 0x1E1, 0x1E2, 0x1E3, 0x1E4,
	    0x1E5, 0x1E6, 0x1E7, 0x1E8, 0x1E9, 0x1EA, 0x1EB, 0x1EC,
	    0x1ED, 0x1EE, 0x1EF, 0x1F0, 0x1F1, 0x1F2, 0x1F3, 0x1F4,
	    0x1F5, 0x1F6, 0x1F7, 0x1F8, 0x1F9, 0x1FA, 0x1FB, 0x1FC,
	    0x1FD, 0x1FE, 0x1FF,  0xE4,  0x71,  0x1A,  0x1B,     5
	};

	static final int atrac3p_sf_huff_bits2[] = new int[] {
	    1, 3, 5, 5, 7, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 8, 7, 5, 5, 3
	};

	static final int atrac3p_sf_huff_code3[] = new int[] {
	        0,     2,     3,  0x18,  0x19,  0x70, 0x1CC, 0x1CD,
	    0x1CE, 0x1CF, 0x1D0, 0x1D1, 0x1D2, 0x1D3, 0x1D4, 0x1D5,
	    0x1D6, 0x1D7, 0x1D8, 0x1D9, 0x1DA, 0x1DB, 0x1DC, 0x1DD,
	    0x1DE, 0x1DF, 0x1E0, 0x1E1, 0x1E2, 0x1E3, 0x1E4, 0x1E5,
	    0x1E6, 0x1E7, 0x1E8, 0x1E9, 0x1EA, 0x1EB, 0x1EC, 0x1ED,
	    0x1EE, 0x1EF, 0x1F0, 0x1F1, 0x1F2, 0x1F3, 0x1F4, 0x1F5,
	    0x1F6, 0x1F7, 0x1F8, 0x1F9, 0x1FA, 0x1FB, 0x1FC, 0x1FD,
	    0x1FE, 0x1FF,  0x71,  0x72,  0x1A,  0x1B,     4,     5
	};

	static final int atrac3p_sf_huff_bits3[] = new int[] {
	    2, 3, 3, 5, 5, 7, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
	    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 7, 7, 5, 5, 3, 3
	};

	static final int atrac3p_sf_huff_code4[] = new int[] {
	    0, 2, 3, 4, 5, 0xC, 0xD, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0, 0x3D, 0x3E, 0x3F
	};

	static final int atrac3p_sf_huff_bits4[] = new int[] {
	    2, 3, 3, 3, 3, 4, 4, 6, 6, 6, 6, 6, 0, 6, 6, 6
	};

	static final int atrac3p_sf_huff_xlat4[] = new int[] {
	    0, 1, 13, 14, 15, 2, 12, 3, 4, 5, 6, 7, 8, 9, 10, 11
	};

	static final int atrac3p_sf_huff_xlat5[] = new int[] {
	    0, 1, 2, 14, 15, 3, 13, 4, 5, 6, 7, 9, 8, 10, 11, 12
	};

	static final int atrac3p_sf_huff_code5[] = new int[] {
	    0,    4,  0xC, 0x1C, 0x78, 0x79, 0x7A, 0x7B,
	    0, 0x7C, 0x7D, 0x7E, 0x7F, 0x1D, 0xD,     5
	};

	static final int atrac3p_sf_huff_bits5[] = new int[] {
	    1, 3, 4, 5, 7, 7, 7, 7, 0, 7, 7, 7, 7, 5, 4, 3
	};

	static final int atrac3p_sf_huff_code6[] = new int[] {
	    0, 2, 3, 0xC, 0x1C, 0x3C, 0x7C, 0x7D, 0, 0x7E, 0x7F, 0x3D, 0x1D, 0xD, 4, 5
	};

	static final int atrac3p_sf_huff_bits6[] = new int[] {
	    2, 3, 3, 4, 5, 6, 7, 7, 0, 7, 7, 6, 5, 4, 3, 3
	};

	/** VLC tables for code table indexes */
	static final int atrac3p_ct_huff_code1[] = new int[] { 0, 2, 6, 7 };

	static final int atrac3p_ct_huff_bits1[] = new int[] { 1, 2, 3, 3 };

	static final int atrac3p_ct_huff_code2[] = new int[] { 0, 2, 3, 4, 5, 6, 0xE, 0xF };

	static final int atrac3p_ct_huff_bits2[] = new int[] { 2, 3, 3, 3, 3, 3, 4, 4 };

	static final int atrac3p_ct_huff_xlat1[] = new int[] { 0, 1, 2, 3, 6, 7, 4, 5 };

	static final int atrac3p_ct_huff_code3[] = new int[] {
	    0, 4, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF
	};

	static final int atrac3p_ct_huff_bits3[] = new int[] { 1, 3, 4, 4, 4, 4, 4, 4 };

	/* weights for quantized word lengths */
	static final int atrac3p_wl_weights[][] = new int[][] {
	    { 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0,
	      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	    { 5, 5, 5, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0,
	      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	    { 6, 5, 5, 5, 4, 4, 4, 4, 3, 3, 3, 3, 2, 2, 1, 1,
	      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 },
	    { 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0, 0,
	      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	    { 5, 5, 5, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1, 0,
	      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	    { 6, 5, 5, 5, 5, 5, 5, 5, 3, 3, 3, 3, 2, 2, 1, 1,
	      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
	};

	/* weights for quantized scale factors
	 * sf_weights[i] = i / (tab_idx + 1)
	 * where tab_idx = [1,2] */
	static final int atrac3p_sf_weights[][] = new int[][] {
	    { 0, 0, 1, 1,  2,  2,  3,  3,  4,  4,  5,  5,  6,  6,  7,  7,
	      8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15 },
	    { 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4,  4,  5,
	      5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10 }
	};

	/** Ungroup table for word length segments.
	 *  Numbers in this table tell which coeff belongs to which segment. */
	static final int atrac3p_qu_num_to_seg[] = new int[] {
	    0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5,
	    5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9, 9
	};

	/** Map quant unit number to subband number */
	static final int atrac3p_qu_to_subband[] = new int[] {
	    0, 0, 0, 0, 0, 0, 0, 0, 1, 1,  1,  1,  2,  2,  2, 2,
	    3, 3, 4, 4, 5, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
	};

	/** Map subband number to number of power compensation groups */
	static final int atrac3p_subband_to_num_powgrps[] = new int[] {
	    1, 2, 2, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5
	};

	/** 3D base shape tables. The values are grouped together as follows:
	 *  [num_start_values = 8][num_shape_tables = 16][num_seg_coeffs = 9]
	 *  For each of the 8 start values there are 16 different shapes each
	 *  9 coefficients long. */
	static final int atrac3p_wl_shapes[][][] = new int[][][] {
	    { {  0,  0,  0,  0,  0,  0,  0, -2, -1 },
	      {  0,  0,  0,  0,  0,  0,  0, -5, -1 },
	      {  0,  0,  0, -7,  0,  0,  0,  0,  0 },
	      {  0,  0,  0,  0,  0, -7,  0,  0,  0 },
	      {  0,  0,  0,  0,  0,  0, -5,  0,  0 },
	      {  0,  0,  0,  0, -5,  0,  0,  0,  0 },
	      { -7, -7,  0,  0,  0,  0,  0,  0,  0 },
	      {  0, -7,  0,  0,  0,  0,  0,  0,  0 },
	      { -2, -2, -5,  0,  0,  0,  0,  0,  0 },
	      {  0,  0,  0, -2, -5,  0,  0,  0,  0 },
	      {  0,  0,  0,  0,  0, -2, -5,  0,  0 },
	      {  0,  0,  0, -5,  0,  0,  0,  0,  0 },
	      {  0, -2, -7, -2,  0,  0,  0,  0,  0 },
	      {  0,  0,  0,  0, -2, -5,  0,  0,  0 },
	      {  0,  0,  0, -5, -5,  0,  0,  0,  0 },
	      {  0,  0,  0, -5, -2,  0,  0,  0,  0 } },
	    { { -1, -5, -3, -2, -1, -1,  0,  0,  0 },
	      { -2, -5, -3, -3, -2, -1, -1,  0,  0 },
	      {  0, -1, -1, -1,  0,  0,  0,  0,  0 },
	      { -1, -3,  0,  0,  0,  0,  0,  0,  0 },
	      { -1, -2,  0,  0,  0,  0,  0,  0,  0 },
	      { -1, -3, -1,  0,  0,  0,  0,  1,  1 },
	      { -1, -5, -3, -3, -2, -1,  0,  0,  0 },
	      { -1, -1, -4, -2, -2, -1, -1,  0,  0 },
	      { -1, -1, -3, -2, -3, -1, -1, -1,  0 },
	      { -1, -4, -2, -3, -1,  0,  0,  0,  0 },
	      {  0, -1, -2, -2, -1, -1,  0,  0,  0 },
	      {  0, -2, -1,  0,  0,  0,  0,  0,  0 },
	      { -1, -1,  0,  0,  0,  0,  0,  0,  0 },
	      { -1, -1, -3, -2, -2, -1, -1, -1,  0 },
	      {  0,  0,  0,  0,  0,  0,  0,  0,  0 },
	      {  0, -1, -3, -2, -2, -1, -1, -1,  0 }, },
	    { { -1, -2,  0,  1,  1,  1,  1,  1,  1 },
	      {  0, -1,  1,  1,  1,  1,  1,  1,  1 },
	      {  0, -2,  1,  1,  1,  1,  1,  1,  1 },
	      {  0, -2,  0,  1,  1,  1,  1,  1,  1 },
	      { -1, -1,  0,  1,  1,  1,  1,  1,  1 },
	      {  0,  0, -1,  0,  1,  1,  1,  1,  1 },
	      { -1, -1,  1,  1,  1,  1,  1,  1,  1 },
	      {  0,  0, -1,  1,  1,  1,  1,  1,  1 },
	      {  0, -1,  0,  1,  1,  1,  1,  1,  1 },
	      { -1, -1, -1,  1,  1,  1,  1,  1,  1 },
	      {  0,  0,  0,  0,  1,  1,  1,  1,  1 },
	      {  0,  0,  0,  1,  1,  1,  1,  1,  1 },
	      {  0, -1, -1,  1,  1,  1,  1,  1,  1 },
	      {  0,  1,  0,  1,  1,  1,  1,  1,  1 },
	      {  0, -3, -2,  1,  1,  1,  1,  2,  2 },
	      { -3, -5, -3,  2,  2,  2,  2,  2,  2 }, },
	    { { -1, -2,  0,  2,  2,  2,  2,  2,  2 },
	      { -1, -2,  0,  1,  2,  2,  2,  2,  2 },
	      {  0, -2,  0,  2,  2,  2,  2,  2,  2 },
	      { -1,  0,  1,  2,  2,  2,  2,  2,  2 },
	      {  0,  0,  1,  2,  2,  2,  2,  2,  2 },
	      {  0, -2,  0,  1,  2,  2,  2,  2,  2 },
	      {  0, -1,  1,  2,  2,  2,  2,  2,  2 },
	      { -1, -1,  0,  2,  2,  2,  2,  2,  2 },
	      { -1, -1,  0,  1,  2,  2,  2,  2,  2 },
	      { -1, -2, -1,  2,  2,  2,  2,  2,  2 },
	      {  0, -1,  0,  2,  2,  2,  2,  2,  2 },
	      {  1,  1,  0,  1,  2,  2,  2,  2,  2 },
	      {  0,  1,  2,  2,  2,  2,  2,  2,  2 },
	      {  1,  0,  0,  1,  2,  2,  2,  2,  2 },
	      {  0,  0,  0,  1,  2,  2,  2,  2,  2 },
	      { -1, -1, -1,  1,  2,  2,  2,  2,  2 }, },
	    { {  0,  1,  2,  3,  3,  3,  3,  3,  3 },
	      {  1,  1,  2,  3,  3,  3,  3,  3,  3 },
	      { -1,  0,  1,  2,  3,  3,  3,  3,  3 },
	      {  0,  0,  2,  3,  3,  3,  3,  3,  3 },
	      { -1,  0,  1,  3,  3,  3,  3,  3,  3 },
	      {  0,  0,  1,  3,  3,  3,  3,  3,  3 },
	      {  1,  2,  3,  3,  3,  3,  3,  3,  3 },
	      {  1,  2,  2,  3,  3,  3,  3,  3,  3 },
	      {  0,  1,  1,  3,  3,  3,  3,  3,  3 },
	      {  0,  0,  1,  2,  3,  3,  3,  3,  3 },
	      { -1,  1,  2,  3,  3,  3,  3,  3,  3 },
	      { -1,  0,  2,  3,  3,  3,  3,  3,  3 },
	      {  2,  2,  3,  3,  3,  3,  3,  3,  3 },
	      {  1,  1,  3,  3,  3,  3,  3,  3,  3 },
	      {  0,  2,  3,  3,  3,  3,  3,  3,  3 },
	      {  0,  1,  1,  2,  3,  3,  3,  3,  3 }, },
	    { {  0,  1,  2,  3,  4,  4,  4,  4,  4 },
	      {  1,  2,  3,  4,  4,  4,  4,  4,  4 },
	      {  0,  0,  2,  3,  4,  4,  4,  4,  4 },
	      {  1,  1,  2,  4,  4,  4,  4,  4,  4 },
	      {  0,  1,  2,  4,  4,  4,  4,  4,  4 },
	      { -1,  0,  1,  3,  4,  4,  4,  4,  4 },
	      {  0,  0,  1,  3,  4,  4,  4,  4,  4 },
	      {  1,  1,  2,  3,  4,  4,  4,  4,  4 },
	      {  0,  1,  1,  3,  4,  4,  4,  4,  4 },
	      {  2,  2,  3,  4,  4,  4,  4,  4,  4 },
	      {  1,  1,  3,  4,  4,  4,  4,  4,  4 },
	      {  1,  2,  2,  4,  4,  4,  4,  4,  4 },
	      { -1,  0,  2,  3,  4,  4,  4,  4,  4 },
	      {  0,  1,  3,  4,  4,  4,  4,  4,  4 },
	      {  1,  2,  2,  3,  4,  4,  4,  4,  4 },
	      {  0,  2,  3,  4,  4,  4,  4,  4,  4 }, },
	    { {  1,  2,  3,  4,  5,  5,  5,  5,  5 },
	      {  0,  1,  2,  3,  4,  5,  5,  5,  5 },
	      {  0,  1,  2,  3,  5,  5,  5,  5,  5 },
	      {  1,  1,  3,  4,  5,  5,  5,  5,  5 },
	      {  1,  1,  2,  4,  5,  5,  5,  5,  5 },
	      {  1,  2,  2,  4,  5,  5,  5,  5,  5 },
	      {  1,  1,  2,  3,  5,  5,  5,  5,  5 },
	      {  2,  2,  3,  4,  5,  5,  5,  5,  5 },
	      {  0,  1,  2,  4,  5,  5,  5,  5,  5 },
	      {  2,  2,  3,  5,  5,  5,  5,  5,  5 },
	      {  1,  2,  3,  5,  5,  5,  5,  5,  5 },
	      {  0,  1,  3,  4,  5,  5,  5,  5,  5 },
	      {  1,  2,  2,  3,  5,  5,  5,  5,  5 },
	      {  2,  3,  4,  5,  5,  5,  5,  5,  5 },
	      {  0,  2,  3,  4,  5,  5,  5,  5,  5 },
	      {  1,  1,  1,  3,  4,  5,  5,  5,  5 }, },
	    { {  1,  2,  3,  4,  5,  5,  5,  6,  6 },
	      {  1,  2,  3,  4,  5,  6,  6,  6,  6 },
	      {  2,  3,  4,  5,  6,  6,  6,  6,  6 },
	      {  1,  2,  3,  4,  6,  6,  6,  6,  6 },
	      {  2,  2,  3,  4,  5,  5,  5,  6,  6 },
	      {  1,  2,  3,  4,  5,  5,  6,  6,  6 },
	      {  2,  2,  3,  4,  6,  6,  6,  6,  6 },
	      {  2,  2,  3,  4,  5,  6,  6,  6,  6 },
	      {  2,  2,  4,  5,  6,  6,  6,  6,  6 },
	      {  2,  2,  3,  5,  6,  6,  6,  6,  6 },
	      {  1,  2,  3,  5,  6,  6,  6,  6,  6 },
	      {  2,  3,  3,  5,  6,  6,  6,  6,  6 },
	      {  1,  2,  4,  5,  6,  6,  6,  6,  6 },
	      {  2,  2,  3,  4,  5,  5,  6,  6,  6 },
	      {  2,  3,  3,  4,  6,  6,  6,  6,  6 },
	      {  1,  3,  4,  5,  6,  6,  6,  6,  6 } }
	};

	/** 2D base shape tables for scale factor coding.
	 *  The values are grouped together as follows:
	 *  [num_shape_tables = 64][num_seg_coeffs = 9] */
	static final int atrac3p_sf_shapes[][] = new int[][] {
	    { -3, -2, -1,  0,  3,  5,  6,  8, 40 },
	    { -3, -2,  0,  1,  7,  9, 11, 13, 20 },
	    { -1,  0,  0,  1,  6,  8, 10, 13, 41 },
	    {  0,  0,  0,  2,  5,  5,  6,  8, 14 },
	    {  0,  0,  0,  2,  6,  7,  8, 11, 47 },
	    {  0,  0,  1,  2,  5,  7,  8, 10, 32 },
	    {  0,  0,  1,  3,  8, 10, 12, 14, 47 },
	    {  0,  0,  2,  4,  9, 10, 12, 14, 40 },
	    {  0,  0,  3,  5,  9, 10, 12, 14, 22 },
	    {  0,  1,  3,  5, 10, 14, 18, 22, 31 },
	    {  0,  2,  5,  6, 10, 10, 10, 12, 46 },
	    {  0,  2,  5,  7, 12, 14, 15, 18, 44 },
	    {  1,  1,  4,  5,  7,  7,  8,  9, 15 },
	    {  1,  2,  2,  2,  4,  5,  7,  9, 26 },
	    {  1,  2,  2,  3,  6,  7,  7,  8, 47 },
	    {  1,  2,  2,  3,  6,  8, 10, 13, 22 },
	    {  1,  3,  4,  7, 13, 17, 21, 24, 41 },
	    {  1,  4,  0,  4, 10, 12, 13, 14, 17 },
	    {  2,  3,  3,  3,  6,  8, 10, 13, 48 },
	    {  2,  3,  3,  4,  9, 12, 14, 17, 47 },
	    {  2,  3,  3,  5, 10, 12, 14, 17, 25 },
	    {  2,  3,  5,  7,  8,  9,  9,  9, 13 },
	    {  2,  3,  5,  9, 16, 21, 25, 28, 33 },
	    {  2,  4,  5,  8, 12, 14, 17, 19, 26 },
	    {  2,  4,  6,  8, 12, 13, 13, 15, 20 },
	    {  2,  4,  7, 12, 20, 26, 30, 32, 35 },
	    {  3,  3,  5,  6, 12, 14, 16, 19, 34 },
	    {  3,  4,  4,  5,  7,  9, 10, 11, 48 },
	    {  3,  4,  5,  6,  8,  9, 10, 11, 16 },
	    {  3,  5,  5,  5,  7,  9, 10, 13, 35 },
	    {  3,  5,  5,  7, 10, 12, 13, 15, 49 },
	    {  3,  5,  7,  7,  8,  7,  9, 12, 21 },
	    {  3,  5,  7,  8, 12, 14, 15, 15, 24 },
	    {  3,  5,  7, 10, 16, 21, 24, 27, 44 },
	    {  3,  5,  8, 14, 21, 26, 28, 29, 42 },
	    {  3,  6, 10, 13, 18, 19, 20, 22, 27 },
	    {  3,  6, 11, 16, 24, 27, 28, 29, 31 },
	    {  4,  5,  4,  3,  4,  6,  8, 11, 18 },
	    {  4,  6,  5,  6,  9, 10, 12, 14, 20 },
	    {  4,  6,  7,  6,  6,  6,  7,  8, 46 },
	    {  4,  6,  7,  9, 13, 16, 18, 20, 48 },
	    {  4,  6,  7,  9, 14, 17, 20, 23, 31 },
	    {  4,  6,  9, 11, 14, 15, 15, 17, 21 },
	    {  4,  8, 13, 20, 27, 32, 35, 36, 38 },
	    {  5,  6,  6,  4,  5,  6,  7,  6,  6 },
	    {  5,  7,  7,  8,  9,  9, 10, 12, 49 },
	    {  5,  8,  9,  9, 10, 11, 12, 13, 42 },
	    {  5,  8, 10, 12, 15, 16, 17, 19, 42 },
	    {  5,  8, 12, 17, 26, 31, 32, 33, 44 },
	    {  5,  9, 13, 16, 20, 22, 23, 23, 35 },
	    {  6,  8,  8,  7,  6,  5,  6,  8, 15 },
	    {  6,  8,  8,  8,  9, 10, 12, 16, 24 },
	    {  6,  8,  8,  9, 10, 10, 11, 11, 13 },
	    {  6,  8, 10, 13, 19, 21, 24, 26, 32 },
	    {  6,  9, 10, 11, 13, 13, 14, 16, 49 },
	    {  7,  9,  9, 10, 13, 14, 16, 19, 27 },
	    {  7, 10, 12, 13, 16, 16, 17, 17, 27 },
	    {  7, 10, 12, 14, 17, 19, 20, 22, 48 },
	    {  8,  9, 10,  9, 10, 11, 11, 11, 19 },
	    {  8, 11, 12, 12, 13, 13, 13, 13, 17 },
	    {  8, 11, 13, 14, 16, 17, 19, 20, 27 },
	    {  8, 12, 17, 22, 26, 28, 29, 30, 33 },
	    { 10, 14, 16, 19, 21, 22, 22, 24, 28 },
	    { 10, 15, 17, 18, 21, 22, 23, 25, 43 }
	};

	static final int atrac3p_ct_restricted_to_full[][][] = new int[][][] {
	    { { 0, 5, 4, 1 },
	      { 0, 1, 2, 3 },
	      { 3, 0, 4, 2 },
	      { 4, 0, 1, 2 },
	      { 1, 0, 4, 3 },
	      { 3, 0, 2, 1 },
	      { 0, 3, 1, 2 } },
	    { { 4, 0, 1, 2 },
	      { 0, 3, 2, 1 },
	      { 0, 1, 2, 3 },
	      { 0, 1, 2, 4 },
	      { 0, 1, 2, 3 },
	      { 1, 4, 2, 0 },
	      { 0, 1, 2, 3 } }
	};

	/* Huffman tables for gain control data. */
	static final int atrac3p_huff_gain_npoints1_cb[] = new int[] {
	    1, 7, 1, 1, 1, 1, 1, 1, 2
	};

	static final int atrac3p_huff_gain_npoints2_xlat[] = new int[] {
	    0, 1, 7, 2, 6, 3, 4, 5
	};

	static final int atrac3p_huff_gain_lev1_cb[] = new int[] { 1, 7, 1, 0, 2, 2, 1, 2, 8 };
	static final int atrac3p_huff_gain_lev1_xlat[] = new int[] {
	    7, 5, 8, 6, 9, 4, 10, 11, 0, 1, 2, 3, 12, 13, 14, 15
	};

	static final int atrac3p_huff_gain_lev2_cb[] = new int[] {
	    1, 9, 1, 1, 1, 1, 1, 0, 2, 0, 8
	};

	static final int atrac3p_huff_gain_lev2_xlat[] = new int[] {
	    15, 14, 1, 13, 2, 3, 12, 4, 5, 6, 7, 8, 9, 10, 11
	};

	static final int atrac3p_huff_gain_lev3_cb[] = new int[] {
	    1, 9, 1, 0, 3, 1, 1, 0, 2, 0, 8
	};

	static final int atrac3p_huff_gain_lev3_xlat[] = new int[] {
	    0, 1, 14, 15, 2, 13, 3, 12, 4, 5, 6, 7, 8, 9, 10, 11
	};

	static final int atrac3p_huff_gain_lev4_cb[] = new int[] {
	    1, 9, 1, 1, 1, 1, 1, 0, 1, 2, 8
	};

	static final int atrac3p_huff_gain_lev4_xlat[] = new int[] {
	    0, 1, 15, 14, 2, 13, 3, 12, 4, 5, 6, 7, 8, 9, 10, 11
	};

	static final int atrac3p_huff_gain_loc1_cb[] = new int[] { 2, 8, 1, 2, 4, 4, 4, 0, 16 };
	static final int atrac3p_huff_gain_loc1_xlat[] = new int[] {
	     1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
	    17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
	};

	static final int atrac3p_huff_gain_loc2_cb[] = new int[] { 3, 8, 5, 3, 2, 3, 2, 16 };
	static final int atrac3p_huff_gain_loc2_xlat[] = new int[] {
	     2,  3,  4,  5,  6,  1,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
	    17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
	};

	static final int atrac3p_huff_gain_loc3_cb[] = new int[] { 2, 6, 1, 0, 2, 11, 18 };
	static final int atrac3p_huff_gain_loc3_xlat[] = new int[] {
	    0,   1, 31,  2,  3,  4,  5,  6,  7, 26, 27, 28, 29, 30,  8,  9,
	    10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
	};

	static final int atrac3p_huff_gain_loc4_cb[] = new int[] { 4, 6, 3, 23, 6 };
	static final int atrac3p_huff_gain_loc4_xlat[] = new int[] {
	    0,  28, 29,  1,  2,  3,  4, 11, 12, 13, 14, 15, 16, 17, 18, 19,
	    20, 21, 22, 23, 24, 25, 26, 27, 30, 31,  5,  6,  7,  8,  9, 10
	};

	static final int atrac3p_huff_gain_loc5_cb[] = new int[] { 1, 7, 1, 0, 0, 3, 2, 6, 20 };
	static final int atrac3p_huff_gain_loc5_xlat[] = new int[] {
	    0,   1,  2, 31,  3,  4,  5,  6,  7,  8, 29, 30,  9, 10, 11, 12,
	    13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28
	};

	/* Huffman tables for GHA waves data. */
	static final int atrac3p_huff_tonebands_cb[] = new int[] { 1, 6, 1, 0, 1, 2, 4, 8 };
	static final int atrac3p_huff_numwavs1_cb[] = new int[] { 1, 7, 1, 1, 1, 1, 1, 1, 2 };
	static final int atrac3p_huff_numwavs2_cb[] = new int[] { 1, 6, 1, 1, 1, 1, 0, 4 };
	static final int atrac3p_huff_numwavs2_xlat[] = new int[] { 0, 1, 7, 2, 3, 4, 5, 6 };
	static final int atrac3p_huff_wav_ampsf1_cb[] = new int[] { 4, 8, 10, 8, 6, 0, 8 };
	static final int atrac3p_huff_wav_ampsf1_xlat[] = new int[] {
	     8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 5, 6,  7, 18, 19, 20,
	    21, 22,  4, 23, 24, 25, 26, 27,  0,  1, 2, 3, 28, 29, 30, 31
	};

	static final int atrac3p_huff_wav_ampsf2_cb[] = new int[] { 4, 8, 11, 5, 6, 6, 4 };
	static final int atrac3p_huff_wav_ampsf2_xlat[] = new int[] {
	    18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 14, 15, 16, 17, 29,
	     9, 10, 11, 12, 13, 30,  4,  5,  6,  7,  8, 31,  0,  1,  2,  3
	};

	static final int atrac3p_huff_wav_ampsf3_cb[] = new int[] { 2, 8, 1, 3, 3, 1, 4, 4, 16 };
	static final int atrac3p_huff_wav_ampsf3_xlat[] = new int[] {
	    0, 1,  2, 31,  3, 29, 30,  4,  5,  6, 27, 28,  7, 24, 25, 26,
	    8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
	};

	static final int atrac3p_huff_freq_cb[] = new int[] {
	    1, 11, 1, 0, 0, 2, 2, 0, 9, 9, 29, 104, 100
	};

	static final int atrac3p_huff_freq_xlat[] = new int[] {
	      0,   1, 255,   2, 254,   3,   4,   5,   6,   7,   8, 251, 252, 253,   9,  10,
	     11,  12, 246, 247, 248, 249, 250,  13,  14,  15,  16,  17,  18,  19,  20,  21,
	     22,  23,  24,  25,  26,  27,  28,  29,  30,  31,  32,  33,  34,  35,  36,  37,
	     38, 243, 244, 245,  39,  40,  41,  42,  43,  44,  45,  46,  47,  48,  49,  50,
	     51,  52,  53,  54,  55,  56,  57,  58,  59,  60,  61,  62,  63,  64,  65,  66,
	     67,  68,  69,  70,  71,  72,  73,  74,  75,  76,  77,  78,  79,  80,  81,  82,
	     83,  84,  85,  86,  87,  88,  89,  90,  91,  92,  93, 194, 195, 196, 197, 198,
	    199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214,
	    215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230,
	    231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242,  94,  95,  96,  97,
	     98,  99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113,
	    114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129,
	    130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145,
	    146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161,
	    162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177,
	    178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193
	};
}
