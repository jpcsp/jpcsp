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

import static jpcsp.media.codec.aac.AacDecoder.MAX_LTP_LONG_SFB;
import jpcsp.util.Utilities;

/**
 * Long Term Prediction
 */
public class LongTermPrediction {
	public boolean present;
	public int lag;
	public float coef;
	public boolean used[] = new boolean[MAX_LTP_LONG_SFB];

	public void copy(LongTermPrediction that) {
		present = that.present;
		lag     = that.lag;
		coef    = that.coef;
		Utilities.copy(used, that.used);
	}
}
