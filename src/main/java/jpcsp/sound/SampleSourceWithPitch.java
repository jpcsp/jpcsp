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

/**
 * @author gid15
 *
 */
public class SampleSourceWithPitch implements ISampleSource {
	private ISampleSource sampleSource;
	private SoundVoice voice;
	private int pitchRest;
	private int currentSample;

	public SampleSourceWithPitch(ISampleSource sampleSource, SoundVoice voice) {
		this.sampleSource = sampleSource;
		this.voice = voice;
	}

	private int getPitch() {
		return voice.getPitch();
	}

	@Override
	public int getNextSample() {
		while (pitchRest <= 0) {
			currentSample = sampleSource.getNextSample();
			pitchRest += sceSasCore.PSP_SAS_PITCH_BASE;
		}
		pitchRest -= getPitch();

		return currentSample;
	}

	@Override
	public void resetToStart() {
		sampleSource.resetToStart();
		pitchRest = 0;
	}

	@Override
	public boolean isEnded() {
		return sampleSource.isEnded();
	}

	@Override
	public String toString() {
		return String.format("SampleSourceWithPitch[pitchRest=0x%X, pitch=0x%X, %s]", pitchRest, getPitch(), sampleSource.toString());
	}
}
