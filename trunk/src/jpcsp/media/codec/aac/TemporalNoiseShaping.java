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

import static jpcsp.media.codec.aac.AacDecoder.TNS_MAX_ORDER;
import jpcsp.util.Utilities;

/**
 * Temporal Noise Shaping
 */
public class TemporalNoiseShaping {
	public boolean present;
	public int nFilt[] = new int[8];
	public int length[][] = new int[8][4];
	public boolean direction[][] = new boolean[8][4];
	public int order[][] = new int[8][4];
	public float coef[][][] = new float[8][4][TNS_MAX_ORDER];

	public void copy(TemporalNoiseShaping that) {
		present = that.present;
		Utilities.copy(nFilt, that.nFilt);
		Utilities.copy(length, that.length);
		Utilities.copy(direction, that.direction);
		Utilities.copy(order, that.order);
		Utilities.copy(coef, that.coef);
	}
}
