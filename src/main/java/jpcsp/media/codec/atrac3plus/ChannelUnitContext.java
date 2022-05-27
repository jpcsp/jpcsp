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

import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.ATRAC3P_PQF_FIR_LEN;
import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.ATRAC3P_SUBBANDS;

/** Channel unit parameters */
public class ChannelUnitContext {
	// Channel unit variables
	public int unitType;                                     ///< unit type (mono/stereo)
	public int numQuantUnits;
	public int numSubbands;
	public int usedQuantUnits;                               ///< number of quant units with coded spectrum
	public int numCodedSubbands;                             ///< number of subbands with coded spectrum
	public boolean muteFlag;                                 ///< mute flag
	public boolean useFullTable;                             ///< 1 - full table list, 0 - restricted one
	public boolean noisePresent;                             ///< 1 - global noise info present
	public int noiseLevelIndex;                              ///< global noise level index
	public int noiseTableIndex;                              ///< global noise RNG table index
	public boolean[] swapChannels = new boolean[ATRAC3P_SUBBANDS]; ///< 1 - perform subband-wise channel swapping
	public boolean[] negateCoeffs = new boolean[ATRAC3P_SUBBANDS]; ///< 1 - subband-wise IMDCT coefficients negation
	public Channel channels[] = new Channel[2];

	// Variables related to GHA tones
	public WaveSynthParams waveSynthHist[] = new WaveSynthParams[2]; ///< waves synth history for two frames
	public WaveSynthParams wavesInfo;
	public WaveSynthParams wavesInfoPrev;

	public IPQFChannelContext ipqfCtx[] = new IPQFChannelContext[2];
	public float[][] prevBuf = new float[2][Atrac3plusDecoder.ATRAC3P_FRAME_SAMPLES]; ///< overlapping buffer

	public static class IPQFChannelContext {
		public float[][] buf1 = new float[ATRAC3P_PQF_FIR_LEN * 2][8];
		public float[][] buf2 = new float[ATRAC3P_PQF_FIR_LEN * 2][8];
		public int pos;
	}

	public ChannelUnitContext() {
		for (int ch = 0; ch < channels.length; ch++) {
			channels[ch] = new Channel(ch);
		}

		for (int i = 0; i < waveSynthHist.length; i++) {
			waveSynthHist[i] = new WaveSynthParams();
		}
		wavesInfo = waveSynthHist[0];
		wavesInfoPrev = waveSynthHist[1];

		for (int i = 0; i < ipqfCtx.length; i++) {
			ipqfCtx[i] = new IPQFChannelContext();
		}
	}
}
