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

import static jpcsp.media.codec.aac.AacDecoder.MAX_CHANNELS;
import static jpcsp.media.codec.aac.AacDecoder.MAX_ELEM_ID;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.FFT;

public class Context {
	public BitReader br;
	public int frameSize;
	public int channels;
	public int skipSamples;
	public int nbSamples;
	public int randomState;
	public int sampleRate;
	public int outputChannels;

	boolean isSaved; ///< Set if elements have stored overlap from previous frame
	DynamicRangeControl cheDrc = new DynamicRangeControl();

	// Channel element related data
	ChannelElement che[][] = new ChannelElement[4][MAX_ELEM_ID];
	ChannelElement tagCheMap[][] = new ChannelElement[4][MAX_ELEM_ID];
	public int tagsMapped;

	public float bufMdct[] = new float[1024];

	FFT mdct;
	FFT mdctSmall;
	FFT mdctLd;
	FFT mdctLtp;

	// Members user for output
	SingleChannelElement outputElement[] = new SingleChannelElement[MAX_CHANNELS]; ///< Points to each SingleChannelElement

	public int dmonoMode;

	public float temp[] = new float[128];

	public OutputConfiguration oc[] = new OutputConfiguration[2];
	public float samples[][] = new float[2][2048];

	public Context() {
		for (int i = 0; i < oc.length; i++) {
			oc[i] = new OutputConfiguration();
		}
	}
}
