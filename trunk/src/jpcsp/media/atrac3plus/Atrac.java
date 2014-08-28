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

public class Atrac {
	private float gainTab1[] = new float[16]; ///< gain compensation level table
	private float gainTab2[] = new float[31]; ///< gain compensation interpolation table
    private int   id2expOffset;               ///< offset for converting level index into level exponent
    private int   locScale;                   ///< scale of location code = 2^loc_scale samples
    private int   locSize;                    ///< size of location code in samples

    public void initGainCompensation(int id2expOffset, int locScale) {
    	this.locScale     = locScale;
    	this.locSize      = 1 << locScale;
    	this.id2expOffset = id2expOffset;

    	// Generate gain level table
    	for (int i = 0; i < 16; i++) {
    		gainTab1[i] = (float) Math.pow(2, id2expOffset - i);
    	}

    	// Generate gain interpolation table
    	for (int i = -15; i < 16; i++) {
    		gainTab2[i + 15] = (float) Math.pow(2, -1.0 / locSize * i);
    	}
    }

	public void gainCompensation(float in[], int inOffset, float prev[], int prevOffset, AtracGainInfo gcNow, AtracGainInfo gcNext, int numSamples, float out[], int outOffset) {
		float gcScale = (gcNext.numPoints > 0 ? gainTab1[gcNext.levCode[0]] : 1f);

		if (gcNow.numPoints == 0) {
			for (int pos = 0; pos < numSamples; pos++) {
				out[outOffset + pos] = in[inOffset + pos] * gcScale + prev[prevOffset + pos];
			}
		} else {
			int pos = 0;

			for (int i = 0; i < gcNow.numPoints; i++) {
				int lastpos = gcNow.locCode[i] << locScale;

				float lev = gainTab1[gcNow.levCode[i]];
				float gainInc = gainTab2[(i + 1 < gcNow.numPoints ? gcNow.levCode[i + 1] : id2expOffset) - gcNow.levCode[i] + 15];

				// apply constant gain level and overlap
				for (; pos < lastpos; pos++) {
					out[outOffset + pos] = (in[inOffset + pos] * gcScale + prev[prevOffset + pos]) * lev;
				}

				// interpolate between two different gain levels
				for (; pos < lastpos + locSize; pos++) {
					out[outOffset + pos] = (in[inOffset + pos] * gcScale + prev[prevOffset + pos]) * lev;
					lev *= gainInc;
				}
			}

			for (; pos < numSamples; pos++) {
				out[outOffset + pos] = in[inOffset + pos] * gcScale + prev[prevOffset + pos];
			}
		}

		// copy the overlapping part into the delay buffer
		System.arraycopy(in, inOffset + numSamples, prev, prevOffset, numSamples);
	}
}
