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

/**
 * @author gid15
 *
 */
public class SampleSourceWithDelay implements ISampleSource {
	private ISampleSource sampleSource;
	private int delay;
	private int sampleIndex;

	public SampleSourceWithDelay(ISampleSource sampleSource, int delay) {
		this.sampleSource = sampleSource;
		this.delay = delay;
	}

	@Override
	public int getNextSample() {
		int sample;

		if (sampleIndex < delay) {
			sample = 0;
			sampleIndex++;
		} else {
			sample = sampleSource.getNextSample();
		}

		return sample;
	}

	@Override
	public void resetToStart() {
		sampleSource.resetToStart();
		sampleIndex = 0;
	}

	@Override
	public boolean isEnded() {
		return sampleSource.isEnded();
	}
}
