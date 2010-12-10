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
package jpcsp.sound;

import static jpcsp.HLE.modules150.sceSasCore.PSP_SAS_PITCH_BASE;

public class SoftwareSynthesizer {
	private SoundVoice voice;
	private short[] synthSamples;

	public SoftwareSynthesizer(SoundVoice voice) {
		this.voice = voice;
	}

	private short getSynthSample(int synthSampleIndex, int sampleIndex) {
		// TODO Compute the sample based on the voice settings (ADSR)
		// Currently, we just use the raw sample values.
		return voice.getSample(sampleIndex);
	}

	private void generateSamples() {
		// Stretch the samples according to the pitch
		int pitch = voice.getPitch();
		int synthSamplesLength = voice.getSamples().length * PSP_SAS_PITCH_BASE / pitch;
		synthSamples = new short[synthSamplesLength];
		for (int i = 0; i < synthSamples.length; i++) {
			synthSamples[i] = getSynthSample(i, i * pitch / PSP_SAS_PITCH_BASE);
		}
	}

	public short[] getSynthSamples() {
		if (synthSamples == null || voice.isChanged()) {
			generateSamples();
			voice.setChanged(false);
		}

		return synthSamples;
	}
}
