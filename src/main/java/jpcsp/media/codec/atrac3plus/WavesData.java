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

/** Parameters of a group of sine waves */
public class WavesData {
	WaveEnvelope pendEnv; ///< pending envelope from the previous frame
	WaveEnvelope currEnv; ///< group envelope from the current frame
	int numWavs;          ///< number of sine waves in the group
	int startIndex;       ///< start index into global tones table for that subband

	public WavesData() {
		pendEnv = new WaveEnvelope();
		currEnv = new WaveEnvelope();
	}

	public void clear() {
		pendEnv.clear();
		currEnv.clear();
		numWavs = 0;
		startIndex = 0;
	}

	public void copy(WavesData from) {
		this.pendEnv.copy(from.pendEnv);
		this.currEnv.copy(from.currEnv);
		this.numWavs = from.numWavs;
		this.startIndex = from.startIndex;
	}
}
