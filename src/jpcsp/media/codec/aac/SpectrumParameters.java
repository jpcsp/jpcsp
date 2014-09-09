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
}
