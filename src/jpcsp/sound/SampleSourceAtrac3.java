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

import jpcsp.Memory;
import jpcsp.HLE.modules150.sceAtrac3plus.AtracID;
import jpcsp.connector.AtracCodec;

/**
 * @author gid15
 *
 */
public class SampleSourceAtrac3 implements ISampleSource {
	private final AtracID id;
	private final AtracCodec codec;
	private final int maxSamples;
	private final int buffer;
	private int sampleIndex;
	private int currentSampleIndex;
	private int bufferedSamples;
	private final Memory mem;

	public SampleSourceAtrac3(AtracID id) {
		this.id = id;
		codec = id.getAtracCodec();
		maxSamples = id.getMaxSamples();
		id.createInternalBuffer(maxSamples * 4);
		buffer = id.getInternalBuffer().addr;
		sampleIndex = 0;
		bufferedSamples = 0;
		currentSampleIndex = -1;
		mem = Memory.getInstance();
	}

	private void decode() {
		bufferedSamples = codec.atracDecodeData(id.getAtracId(), buffer);
		sampleIndex = 0;
	}

	@Override
	public int getNextSample() {
		if (sampleIndex >= bufferedSamples) {
			decode();
			if (bufferedSamples <= 0) {
				return 0;
			}
		}

		int sample = mem.read32(buffer + (sampleIndex << 2));
		currentSampleIndex++;
		sampleIndex++;

		return sample;
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
		return id.getAtracEndSample();
	}
}
