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
import jpcsp.HLE.Modules;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class SampleSourceVAG implements ISampleSource {
	private SoundVoice voice;
	private int address;
	private int numberSamples;
	private IMemoryReader memoryReader;
    private final int[] unpackedSamples = new int[28];
    private final short[] samples = new short[28];
    private int sampleIndex = samples.length;
    private int numberVGABlocks;
    private int currentVAGBlock;
    private int currentSampleIndex;
    private int hist1;
    private int hist2;
    private boolean loopMode;
    private int loopStartVAGBlock;
    private boolean loopAtNextVAGBlock;
    private static final double[][] VAG_f = {
        {0.0, 0.0},
        {60.0 / 64.0, 0.0},
        {115.0 / 64.0, -52.0 / 64.0},
        {98.0 / 64.0, -55.0 / 64.0},
        {122.0 / 64.0, -60.0 / 64.0}
    };

	public SampleSourceVAG(SoundVoice voice, int address, int size, boolean loopMode) {
		this.voice = voice;
		this.address = address;
		this.loopMode = loopMode;

		if (address == 0) {
			numberSamples = 0;
			numberVGABlocks = 0;
		} else {
			readHeader();

			numberVGABlocks = size / 16;
			numberSamples = numberVGABlocks * 28;
			currentSampleIndex = -1;
			setSampleIndex(0);
		}
	}

	private void readHeader() {
		Memory mem = Memory.getInstance();

        int header = mem.read32(address);
		if ((header & 0x00FFFFFF) == 0x00474156) { // VAGx.
            int version = Integer.reverseBytes(mem.read32(address + 4));
            int dataSize = Integer.reverseBytes(mem.read32(address + 12));
            int sampleRate = Integer.reverseBytes(mem.read32(address + 16));
            String dataName = new StringBuffer(Utilities.readStringNZ(address + 32, 16)).reverse().toString();
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug("SampleSourceVAG found VAG/ADPCM data: version=" + version
                        + ", size=" + dataSize
                        + ", sampleRate=" + sampleRate
                        + ", dataName=" + dataName);
            }
            address += 0x30;
        }
	}

	private boolean unpackNextVAGBlock() {
		if (currentVAGBlock >= numberVGABlocks) {
			return false;
		}

		sampleIndex = 0;

		int n = memoryReader.readNext();
        int predict_nr = n >> 4;
        if (predict_nr >= VAG_f.length) {
            predict_nr = 0;
        }
        int shift_factor = n & 0x0F;
        int flag = memoryReader.readNext();
        if (flag == 0x03) {
            // If loop mode is enabled, this flag indicates
            // the final block of the loop.
        	// Do not loop if the voice has been keyed Off.
        	if (loopMode && voice.isOn()) {
        		loopAtNextVAGBlock = true;
        	}
        } else if (flag == 0x06) {
            // If loop mode is enabled, this flag indicates
            // the first block of the loop.
            // TODO: Implement loop processing by decoding
            // the same samples within the loop flags
            // when loop mode is on.
        	loopStartVAGBlock = currentVAGBlock;
        } else if (flag == 0x07) {
        	numberVGABlocks = currentVAGBlock;
        	numberSamples = numberVGABlocks * 28;
        	sampleIndex = samples.length;
            return false;	// End of stream flag.
        }

        for (int j = 0; j < 28; j += 2) {
            int d = memoryReader.readNext();
            int s = (short) ((d & 0x0F) << 12);
            unpackedSamples[j] = s >> shift_factor;
            s = (short) ((d & 0xF0) << 8);
            unpackedSamples[j + 1] = s >> shift_factor;
        }

        for (int j = 0; j < 28; j++) {
            int sample = (int) (unpackedSamples[j] + hist1 * VAG_f[predict_nr][0] + hist2 * VAG_f[predict_nr][1]);
            hist2 = hist1;
            hist1 = sample;
            if (sample < -32768) {
            	samples[j] = -32768;
            } else if (sample > 0x7FFF) {
                samples[j] = 0x7FFF;
            } else {
                samples[j] = (short) sample;
            }
        }

        currentVAGBlock++;

        return true;
	}

	@Override
	public int getNextSample() {
		if (sampleIndex >= samples.length) {
			if (!unpackNextVAGBlock()) {
				return 0;
			}
		}

		short sample = samples[sampleIndex];
		sampleIndex++;
		currentSampleIndex++;

		if (loopAtNextVAGBlock && sampleIndex >= samples.length) {
			loopAtNextVAGBlock = false;
			setSampleIndex(loopStartVAGBlock * 28);
		}

		return sample & 0x0000FFFF;
	}

	@Override
	public int getNumberSamples() {
		return numberSamples;
	}

	@Override
	public void setSampleIndex(int index) {
		if (index == currentSampleIndex) {
			// Reading in sequence, nothing to do
			return;
		}

		currentSampleIndex = index;
		currentVAGBlock = index / 28;
		if (currentVAGBlock >= numberVGABlocks) {
			sampleIndex = samples.length;
		} else {
			int restSamples = numberSamples - index;
			memoryReader = MemoryReader.getMemoryReader(address + (currentVAGBlock << 4), restSamples << 2, 1);
			if (unpackNextVAGBlock()) {
				sampleIndex = index % 28;
			}
		}
	}

	@Override
	public int getSampleIndex() {
		return currentSampleIndex;
	}

	@Override
	public String toString() {
		return String.format("SampleSourceVAG[index=%d,VAG=%d[%d],loopStart=%d,loop at next=%b]", currentSampleIndex, currentVAGBlock, sampleIndex, loopStartVAGBlock, loopAtNextVAGBlock);
	}
}
