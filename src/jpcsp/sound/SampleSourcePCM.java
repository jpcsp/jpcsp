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
	private int addr;
	private int size;
	private IMemoryReader memoryReader;
	private int sampleIndex;
	private int currentSampleIndex;
	private int samples;

	public SampleSourcePCM(int addr, int samples) {
		this.addr = addr;
		this.samples = samples;
		size = samples << 1;
		sampleIndex = samples;
	}

	@Override
	public int getNextSample() {
		if (sampleIndex >= samples) {
			memoryReader = MemoryReader.getMemoryReader(addr, size, 2);
			sampleIndex = 0;
		}
		currentSampleIndex++;
		sampleIndex++;

		return memoryReader.readNext();
	}

	@Override
	public void setSampleIndex(int index) {
	}

	@Override
	public int getSampleIndex() {
		return currentSampleIndex;
	}

	@Override
	public int getNumberSamples() {
		return 0x10000000;
	}
}
