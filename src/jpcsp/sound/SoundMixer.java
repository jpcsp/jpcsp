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

import static jpcsp.sound.SoundChannel.MAX_VOLUME;
import jpcsp.hardware.Audio;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

public class SoundMixer {
    private SoundVoice[] voices;
    private SoftwareSynthesizer[] synthesizers;

    public SoundMixer(SoundVoice[] voices) {
    	this.voices = voices;

    	synthesizers = new SoftwareSynthesizer[voices.length];
    	for (int i = 0; i < voices.length; i++) {
    		synthesizers[i] = new SoftwareSynthesizer(voices[i]);
    	}
    }

    private static short clampSample(int sample) {
    	if (sample < Short.MIN_VALUE) {
    		return Short.MIN_VALUE;
    	} else if (sample > Short.MAX_VALUE) {
    		return Short.MAX_VALUE;
    	}

    	return (short) sample;
    }

    private void mix(int[] stereoSamples, ISampleSource sampleSource, int startIndex, int length, int leftVol, int rightVol) {
    	int endIndex = startIndex + length;
    	sampleSource.setSampleIndex(startIndex);
    	for (int i = startIndex, j = 0; i < endIndex; i++, j += 2) {
    		short monoSample = sampleSource.getNextSample();
    		stereoSamples[j] += SoundChannel.adjustSample(monoSample, leftVol);
    		stereoSamples[j + 1] += SoundChannel.adjustSample(monoSample, rightVol);
    	}
    }

    private void copySamplesToMem(int[] mixedSamples, int addr, int samples, int leftVol, int rightVol, boolean writeSamples) {
    	// Adjust the volume according to the global volume settings
    	leftVol = Audio.getVolume(leftVol);
    	rightVol = Audio.getVolume(rightVol);

    	if (!writeSamples) {
    		// If the samples have not been changed and the volume settings
    		// would also not adjust the samples, no need to copy them back to memory.
    		if (leftVol == MAX_VOLUME && rightVol == MAX_VOLUME) {
    			return;
    		}
    	}

    	int lengthInBytes = mixedSamples.length * 2;
    	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, lengthInBytes, 2);
    	for (int i = 0, j = 0; i < samples; i++, j += 2) {
    		short sampleLeft  = clampSample(mixedSamples[j]);
    		short sampleRight = clampSample(mixedSamples[j + 1]);
    		memoryWriter.writeNext(SoundChannel.adjustSample(sampleLeft , leftVol ) & 0xFFFF);
    		memoryWriter.writeNext(SoundChannel.adjustSample(sampleRight, rightVol) & 0xFFFF);
    	}
    	memoryWriter.flush();
    }

    private void mix(int[] mixedSamples, int addr, int samples, int leftVol, int rightVol, boolean writeSamples) {
    	for (int i = 0; i < voices.length; i++) {
    		SoundVoice voice = voices[i];

            if (voice.isPlaying()) {
            	int playSample = voice.getPlaySample();
            	ISampleSource sampleSource = synthesizers[i].getSampleSource();
            	int restPlay = sampleSource.getNumberSamples() - playSample;
            	if (restPlay <= 0) {
            		// End of voice sample reached
            		voice.setPlaying(false);
            	} else {
            		int numSamples = Math.min(samples, restPlay);
            		mix(mixedSamples, sampleSource, playSample, numSamples, voice.getLeftVolume(), voice.getRightVolume());
            		voice.setPlaySample(playSample + numSamples);
            		writeSamples = true;
            	}
            }
        }

		copySamplesToMem(mixedSamples, addr, samples, leftVol, rightVol, writeSamples);
    }

    /**
     * Synthesizing audio function.
     * @param addr Output address for the PCM data (must be 64-byte aligned).
     * @param length Size of the PCM buffer in use (matches the sound granularity).
     */
    public void synthesize(int addr, int samples) {
    	int[] mixedSamples = new int[samples * 2];
    	for (int i = 0; i < mixedSamples.length; i++) {
    		mixedSamples[i] = 0;
    	}

    	mix(mixedSamples, addr, samples, MAX_VOLUME, MAX_VOLUME, true);
    }

    /**
     * Synthesizing audio function with mix.
     * @param addr Output address for the PCM data (must be 64-byte aligned).
     * @param length Size of the PCM buffer in use (matches the sound granularity).
     * @param mix Array containing the PCM mix data.
     */
    public void synthesizeWithMix(int addr, int samples, int leftVol, int rightVol) {
    	int[] mixedSamples = new int[samples * 2];
    	int lengthInBytes = mixedSamples.length * 2;
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 2);
    	for (int i = 0; i < mixedSamples.length; i++) {
    		mixedSamples[i] = (short) memoryReader.readNext();
    	}

    	mix(mixedSamples, addr, samples, leftVol, rightVol, false);
    }
}
