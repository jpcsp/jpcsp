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

import static java.lang.Math.min;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules150.sceAtrac3plus.AtracID;
import jpcsp.HLE.modules150.sceSasCore;

/**
 * @author gid15
 *
 */
public class SampleSourceAtrac3 implements ISampleSource {
	private Logger log = sceSasCore.log;
	private final AtracID id;
	private final int maxSamples;
	private final int buffer;
	private int sampleIndex;
	private int currentSampleIndex;
	private int bufferedSamples;
	private final Memory mem;

	public SampleSourceAtrac3(AtracID id) {
		this.id = id;
		maxSamples = id.getMaxSamples();
		id.createInternalBuffer(maxSamples * 4);
		buffer = id.getInternalBuffer().addr;
		sampleIndex = 0;
		bufferedSamples = 0;
		currentSampleIndex = -1;
		mem = Memory.getInstance();
	}

	private void decode() {
		int result = id.decodeData(buffer, TPointer32.NULL);
		if (result < 0) {
			log.error(String.format("SampleSourceAtrac3 decodeData returned 0x%08X", result));
			bufferedSamples = 0;
		} else {
			bufferedSamples = id.getCodec().getNumberOfSamples();
		}

		if (!id.getInputBuffer().isFileEnd()) {
			int requestedSize = min(id.getInputFileSize() - id.getInputBuffer().getFilePosition(), id.getInputBuffer().getMaxSize());
			id.setContextDecodeResult(-1, requestedSize);
		} else {
			id.setContextDecodeResult(0, 0);
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("SampleSourceAtrac3 decode: bufferedSamples=%d, currentSample=%d, endSample=%d", bufferedSamples, getSampleIndex(), id.getAtracEndSample()));
		}

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
