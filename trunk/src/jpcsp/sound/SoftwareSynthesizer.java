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

import static jpcsp.HLE.modules150.sceSasCore.PSP_SAS_OUTPUTMODE_STEREO;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceSasCore;

public class SoftwareSynthesizer {
	public static Logger log = Logger.getLogger("sound");
	private SoundVoice voice;
	private ISampleSource sampleSource;
	private static final int defaultDelay = 32;

	public SoftwareSynthesizer(SoundVoice voice) {
		this.voice = voice;
	}

	public ISampleSource getSampleSource() {
		if (sampleSource == null || voice.isChanged()) {
			voice.setChanged(false);

			if (voice.getAtracId() != null) {
				sampleSource = new SampleSourceAtrac3(voice.getAtracId());
			} else if (voice.getPcmAddress() != 0) {
				sampleSource = new SampleSourcePCM(voice, voice.getPcmAddress(), voice.getPcmSize(), voice.getLoopMode());
				if (Modules.sceSasCoreModule.getOutputMode() == PSP_SAS_OUTPUTMODE_STEREO) {
					// Convert mono VAG to stereo
					sampleSource = new SampleSourceMono(sampleSource);
				}
			} else if (voice.getVAGAddress() != 0) {
				sampleSource = new SampleSourceVAG(voice, voice.getVAGAddress(), voice.getVAGSize(), voice.getLoopMode() != sceSasCore.PSP_SAS_LOOP_MODE_OFF);
				if (Modules.sceSasCoreModule.getOutputMode() == PSP_SAS_OUTPUTMODE_STEREO) {
					// Convert mono VAG to stereo
					sampleSource = new SampleSourceMono(sampleSource);
				}
			} else {
				sampleSource = new SampleSourceEmpty();
			}

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
