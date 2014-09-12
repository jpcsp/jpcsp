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
package jpcsp.media.codec.aac;

/**
 * Spectral Band Replication header - spectrum parameters that invoke a reset if they differ from the previous header.
 */
public class SpectrumParameters {
	public int bsStartFreq;
	public int bsStopFreq;
	public int bsXoverBand;

	public int bsFreqScale;
	public int bsAlterScale;
	public int bsNoiseBands;

	public void copy(SpectrumParameters that) {
		bsStartFreq  = that.bsStartFreq;
		bsStopFreq   = that.bsStopFreq;
		bsXoverBand  = that.bsXoverBand;
		bsFreqScale  = that.bsFreqScale;
		bsAlterScale = that.bsAlterScale;
		bsNoiseBands = that.bsNoiseBands;
	}

	public void reset() {
		bsStartFreq  = -1;
		bsStopFreq   = -1;
		bsXoverBand  = -1;
		bsFreqScale  = -1;
		bsAlterScale = -1;
		bsNoiseBands = -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SpectrumParameters) {
			SpectrumParameters that = (SpectrumParameters) obj;

			if (bsStartFreq != that.bsStartFreq) {
				return false;
			}
			if (bsStopFreq != that.bsStopFreq) {
				return false;
			}
			if (bsXoverBand != that.bsXoverBand) {
				return false;
			}
			if (bsFreqScale != that.bsFreqScale) {
				return false;
			}
			if (bsAlterScale != that.bsAlterScale) {
				return false;
			}
			if (bsNoiseBands != that.bsNoiseBands) {
				return false;
			}

			return true;
		}

		return super.equals(obj);
	}
}
