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

import jpcsp.HLE.modules.sceSasCore;

public class SoftwareSynthesizer {
	private SoundVoice voice;
	private ISampleSource sampleSource;
	private static final int defaultDelay = 32;

	public SoftwareSynthesizer(SoundVoice voice) {
		this.voice = voice;
	}

	public ISampleSource getSampleSource() {
		if (sampleSource == null || voice.isChanged()) {
			voice.setChanged(false);

			// Currently we build the samples only based on the pitch.
			// ADSR has still to be added.
			sampleSource = new SampleSourceVAG(voice, voice.getVAGAddress(), voice.getVAGSize(), voice.getLoopMode() != sceSasCore.PSP_SAS_LOOP_MODE_OFF);

			if (voice.getPitch() != sceSasCore.PSP_SAS_PITCH_BASE) {
				// Modify the sample according to the pitch (only if not the default pitch)
				sampleSource = new SampleSourceWithPitch(sampleSource, voice.getPitch());
			}

			sampleSource = new SampleSourceWithADSR(sampleSource, voice, voice.getEnvelope());

			// PSP implementation always adds 32 samples delay before actually starting
			if (defaultDelay > 0) {
				sampleSource = new SampleSourceWithDelay(sampleSource, defaultDelay);
			}
		}

		return sampleSource;
	}
}
