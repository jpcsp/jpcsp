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

/**
 *  Gain control parameters for one subband.
 */
public class AtracGainInfo {
	public int numPoints;             ///< number of gain control points
	public int levCode[] = new int[7]; ///< level at corresponding control point
	public int locCode[] = new int[7]; ///< location of gain control points

	public void clear() {
		numPoints = 0;
		for (int i = 0; i < 7; i++) {
			levCode[i] = 0;
			locCode[i] = 0;
		}
	}

	public void copy(AtracGainInfo from) {
		this.numPoints = from.numPoints;
		System.arraycopy(from.levCode, 0, this.levCode, 0, levCode.length);
		System.arraycopy(from.locCode, 0, this.locCode, 0, locCode.length);
	}
}
