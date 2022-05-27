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

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/**
 * @author gid15
 *
 */
public class SampleSourcePCM implements ISampleSource {
	private SoundVoice voice;
	private int addr;
	private int size;
	private IMemoryReader memoryReader;
	private int sampleIndex;
	private int samples;
	private boolean looping;

	public SampleSourcePCM(SoundVoice voice, int addr, int samples, int loopMode) {
		this.voice = voice;
		this.addr = addr;
		this.samples = samples;
		size = samples << 1;
		sampleIndex = samples;

		looping = loopMode >= 0;
	}

	@Override
	public int getNextSample() {
		if (sampleIndex >= samples) {
			if (!voice.isOn()) {
				// Voice is off, stop playing
				looping = false;
				return 0;
			}
			resetToStart();
		}
		sampleIndex++;

		return memoryReader.readNext();
	}

	@Override
	public void resetToStart() {
		memoryReader = MemoryReader.getMemoryReader(addr, size, 2);
		sampleIndex = 0;
	}

	@Override
	public boolean isEnded() {
		if (looping) {
			// Never ending
			return false;
		}
		return sampleIndex >= samples;
	}
}
