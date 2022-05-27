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

import static jpcsp.media.codec.atrac3plus.Atrac3plusDecoder.ATRAC3P_SUBBANDS;

/** Sound channel parameters */
public class Channel {
	public int chNum;
	public int numCodedVals;               ///< number of transmitted quant unit values
	public int fillMode;
	public int splitPoint;
	public int tableType;                  ///< table type: 0 - tone?, 1- noise?
	public int quWordlen[] = new int[32];  ///< array of word lengths for each quant unit
	public int quSfIdx[] = new int[32];    ///< array of scale factor indexes for each quant unit
	public int quTabIdx[] = new int[32];   ///< array of code table indexes for each quant unit
	public int spectrum[] = new int[2048]; ///< decoded IMDCT spectrum
	public int powerLevs[] = new int[5];   ///< power compensation levels

	// imdct window shape history (2 frames) for overlapping.
	public boolean wndShapeHist[][];       ///< IMDCT window shape, 0=sine/1=steep
	public boolean wndShape[];             ///< IMDCT window shape for current frame
	public boolean wndShapePrev[];         ///< IMDCT window shape for previous frame

	// gain control data history (2 frames) for overlapping.
	AtracGainInfo gainDataHist[][];        ///< gain control data for all subbands
	AtracGainInfo gainData[];              ///< gain control data for next frame
	AtracGainInfo gainDataPrev[];          ///< gain control data for previous frame
	public int numGainSubbands;            ///< number of subbands with gain control data

	// tones data history (2 frames) for overlapping.
	WavesData tonesInfoHist[][];
	WavesData tonesInfo[];
	WavesData tonesInfoPrev[];

	public Channel(int chNum) {
		this.chNum    = chNum;

		wndShapeHist  = new boolean[2][ATRAC3P_SUBBANDS];
		gainDataHist  = new AtracGainInfo[2][ATRAC3P_SUBBANDS];
		tonesInfoHist = new WavesData[2][ATRAC3P_SUBBANDS];
		for (int i = 0; i < 2; i++) {
			for (int sb = 0; sb < ATRAC3P_SUBBANDS; sb++) {
				gainDataHist [i][sb] = new AtracGainInfo();
				tonesInfoHist[i][sb] = new WavesData();
			}
		}

		wndShape      = wndShapeHist[0];
		wndShapePrev  = wndShapeHist[1];
		gainData      = gainDataHist[0];
		gainDataPrev  = gainDataHist[1];
		tonesInfo     = tonesInfoHist[0];
		tonesInfoPrev = tonesInfoHist[1];
	}
}
