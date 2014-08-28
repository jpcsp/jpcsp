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
package jpcsp.media.atrac3plus;

import static jpcsp.media.atrac3plus.Atrac3plusDecoder.ATRAC3P_FRAME_SAMPLES;

public class Context {
	public BitReader br;
	public Atrac3plusDsp dsp;

	public ChannelUnit channelUnits[] = new ChannelUnit[16]; ///< global channel units
	public int numChannelBlocks = 2;                         ///< number of channel blocks

	public Atrac gaincCtx; ///< gain compensation context
	public FFT mdctCtx;
	public FFT ipqfDctCtx; ///< IDCT context used by IPQF

	public float samples[][] = new float[2][ATRAC3P_FRAME_SAMPLES]; ///< quantized MDCT sprectrum
	public float mdctBuf[][] = new float[2][ATRAC3P_FRAME_SAMPLES]; ///< output of the IMDCT
	public float timeBuf[][] = new float[2][ATRAC3P_FRAME_SAMPLES]; ///< output of the gain compensation
	public float outpBuf[][] = new float[2][ATRAC3P_FRAME_SAMPLES];
}
