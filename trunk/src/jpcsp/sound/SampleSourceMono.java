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

import static jpcsp.sound.SoundMixer.getSampleLeft;
import static jpcsp.sound.SoundMixer.getSampleStereo;

/**
 * @author gid15
 *
 * Converts a mono sample source to the requested stereo.
 */
public class SampleSourceMono implements ISampleSource {
	private ISampleSource sampleSource;

	public SampleSourceMono(ISampleSource sampleSource) {
		this.sampleSource = sampleSource;
	}

	@Override
	public int getNextSample() {
		short mono = getSampleLeft(sampleSource.getNextSample());

		return getSampleStereo(mono, mono);
	}

	@Override
	public void setSampleIndex(int index) {
		sampleSource.setSampleIndex(index);
	}

	@Override
	public int getSampleIndex() {
		return sampleSource.getSampleIndex();
	}

	@Override
	public int getNumberSamples() {
		return sampleSource.getNumberSamples();
	}
}
