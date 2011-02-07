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
	private int pitch;
	private int sampleSourceIndex = -1;
	private int sampleIndex;
	private short currentSample;

	public SampleSourceWithPitch(ISampleSource sampleSource, int pitch) {
		this.sampleSource = sampleSource;
		this.pitch = pitch;
	}

	private int getSampleSourceIndexFromSampleIndex(int index) {
		return (int) (index * (long) pitch / sceSasCore.PSP_SAS_PITCH_BASE);
	}

	@Override
	public short getNextSample() {
		int nextSampleSourceIndex = getSampleSourceIndexFromSampleIndex(sampleIndex);
		if (nextSampleSourceIndex > sampleSourceIndex) {
			currentSample = sampleSource.getNextSample();
			sampleSourceIndex = nextSampleSourceIndex;
		}
		sampleIndex++;

		return currentSample;
	}

	private int getSampleIndexFromSampleSourceIndex(int index) {
		return (int) (index * (long) sceSasCore.PSP_SAS_PITCH_BASE / pitch);
	}

	@Override
	public int getNumberSamples() {
		if (pitch <= 0) {
			return 0;
		}

		return getSampleIndexFromSampleSourceIndex(sampleSource.getNumberSamples());
	}

	@Override
	public void setSampleIndex(int index) {
		if (index != sampleIndex) {
			sampleIndex = index;
			sampleSource.setSampleIndex(getSampleSourceIndexFromSampleIndex(sampleIndex));
			sampleSourceIndex = -1;
		}
	}

	@Override
	public int getSampleIndex() {
		return getSampleIndexFromSampleSourceIndex(sampleSource.getSampleIndex());
	}

	@Override
	public String toString() {
		return String.format("SampleSourceWithPitch[index=%d, pitch=%d, %s]", sampleIndex, pitch, sampleSource.toString());
	}
}
