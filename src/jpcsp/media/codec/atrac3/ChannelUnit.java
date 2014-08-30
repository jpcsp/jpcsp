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
package jpcsp.media.codec.atrac3;

import static jpcsp.media.codec.atrac3.Atrac3Decoder.SAMPLES_PER_FRAME;

public class ChannelUnit {
	public int bandsCoded;
	public int numComponents;
	public float prevFrame[] = new float[SAMPLES_PER_FRAME];
	public int gcBlkSwitch;
	public TonalComponent components[] = new TonalComponent[64];
	public GainBlock gainBlock[] = new GainBlock[2];

	public float spectrum[] = new float[SAMPLES_PER_FRAME];
	public float imdctBuf[] = new float[SAMPLES_PER_FRAME];

	public float delayBuf1[] = new float[46]; ///<qmf delay buffers
	public float delayBuf2[] = new float[46];
	public float delayBuf3[] = new float[46];

	public ChannelUnit() {
		for (int i = 0; i < components.length; i++) {
			components[i] = new TonalComponent();
		}
		for (int i = 0; i < gainBlock.length; i++) {
			gainBlock[i] = new GainBlock();
		}
	}
}
