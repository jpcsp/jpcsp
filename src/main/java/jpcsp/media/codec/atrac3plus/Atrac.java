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

import static java.lang.Math.pow;

public class Atrac {
    public static final float[] ff_atrac_sf_table = new float[64];
    private static final float[] qmf_window = new float[48];
    private static final float qmf_48tap_half[] = new float[] {
    	   -0.00001461907f, -0.00009205479f,-0.000056157569f,0.00030117269f,
    	    0.0002422519f,  -0.00085293897f,-0.0005205574f,  0.0020340169f,
    	    0.00078333891f, -0.0042153862f, -0.00075614988f, 0.0078402944f,
    	   -0.000061169922f,-0.01344162f,    0.0024626821f,  0.021736089f,
    	   -0.007801671f,   -0.034090221f,   0.01880949f,    0.054326009f,
    	   -0.043596379f,   -0.099384367f,   0.13207909f,    0.46424159f
	};
	private float gainTab1[] = new float[16]; ///< gain compensation level table
	private float gainTab2[] = new float[31]; ///< gain compensation interpolation table
    private int   id2expOffset;               ///< offset for converting level index into level exponent
    private int   locScale;                   ///< scale of location code = 2^loc_scale samples
    private int   locSize;                    ///< size of location code in samples

    public static void generateTables() {
    	// Generate scale factors
    	if (ff_atrac_sf_table[63] == 0f) {
    		for (int i = 0; i < 64; i++) {
    			ff_atrac_sf_table[i] = (float) pow(2.0f, (i - 15) / 3.0);
    		}
    	}

    	// Generate the QMF window
    	if (qmf_window[47] == 0f) {
    		for (int i = 0; i < 24; i++) {
    			float s = qmf_48tap_half[i] * 2.0f;
    			qmf_window[i] = s;
    			qmf_window[47 - i] = s;
    		}
    	}
    }

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
		float gcScale = (gcNext.numPoints != 0 ? gainTab1[gcNext.levCode[0]] : 1f);

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

	public static void iqmf(float[] inlo, int inloOffset, float[] inhi, int inhiOffset, int nIn, float[] out, int outOffset, float[] delayBuf, float[] temp) {
		System.arraycopy(delayBuf, 0, temp, 0, 46);

		// loop1
		for (int i = 0; i < nIn; i += 2) {
			temp[46 + 2 * i + 0] = inlo[inloOffset + i    ] + inhi[inhiOffset + i    ];
			temp[46 + 2 * i + 1] = inlo[inloOffset + i    ] - inhi[inhiOffset + i    ];
			temp[46 + 2 * i + 2] = inlo[inloOffset + i + 1] + inhi[inhiOffset + i + 1];
			temp[46 + 2 * i + 3] = inlo[inloOffset + i + 1] - inhi[inhiOffset + i + 1];
		}

		// loop2
		int p1 = 0;
		for (int j = nIn; j != 0; j--) {
			float s1 = 0f;
			float s2 = 0f;

			for (int i = 0; i < 48; i += 2) {
				s1 += temp[p1 + i] * qmf_window[i];
				s2 += temp[p1 + i + 1] * qmf_window[i + 1];
			}

			out[outOffset + 0] = s2;
			out[outOffset + 1] = s1;

			p1 += 2;
			outOffset += 2;
		}

		// Update the delay buffer.
		System.arraycopy(temp, nIn * 2, delayBuf, 0, 46);
	}
}
