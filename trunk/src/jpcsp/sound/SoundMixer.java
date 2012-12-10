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

import static jpcsp.HLE.modules150.sceSasCore.PSP_SAS_OUTPUTMODE_STEREO;
import static jpcsp.sound.SoundChannel.MAX_VOLUME;
import jpcsp.HLE.Modules;
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

    private void mixStereo(int[] stereoSamples, ISampleSource sampleSource, int startIndex, int length, int leftVol, int rightVol) {
    	int endIndex = startIndex + length;
    	sampleSource.setSampleIndex(startIndex);
    	for (int i = startIndex, j = 0; i < endIndex; i++, j += 2) {
    		int sample = sampleSource.getNextSample();
    		stereoSamples[j] += SoundChannel.adjustSample(getSampleLeft(sample), leftVol);
    		stereoSamples[j + 1] += SoundChannel.adjustSample(getSampleRight(sample), rightVol);
    	}
    }

    private void mixMono(int[] monoSamples, ISampleSource sampleSource, int startIndex, int length, int monoVol) {
    	int endIndex = startIndex + length;
    	sampleSource.setSampleIndex(startIndex);
    	for (int i = startIndex, j = 0; i < endIndex; i++, j++) {
    		int sample = sampleSource.getNextSample();
    		monoSamples[j] += SoundChannel.adjustSample(getSampleLeft(sample), monoVol);
    	}
    }

    private void copyStereoSamplesToMem(int[] mixedSamples, int addr, int samples, int leftVol, int rightVol, boolean writeSamples) {
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

    	int lengthInBytes = mixedSamples.length << 1;
    	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, lengthInBytes, 4);
    	for (int i = 0, j = 0; i < samples; i++, j += 2) {
    		short sampleLeft  = clampSample(mixedSamples[j]);
    		short sampleRight = clampSample(mixedSamples[j + 1]);
    		sampleLeft = SoundChannel.adjustSample(sampleLeft, leftVol);
    		sampleRight = SoundChannel.adjustSample(sampleRight, rightVol);
    		int sampleStereo = getSampleStereo(sampleLeft, sampleRight);
    		memoryWriter.writeNext(sampleStereo);
    	}
    	memoryWriter.flush();
    }

    private void copyMonoSamplesToMem(int[] mixedSamples, int addr, int samples, int monoVol, boolean writeSamples) {
    	// Adjust the volume according to the global volume settings
    	monoVol = Audio.getVolume(monoVol);

    	if (!writeSamples) {
    		// If the samples have not been changed and the volume settings
    		// would also not adjust the samples, no need to copy them back to memory.
    		if (monoVol == MAX_VOLUME) {
    			return;
    		}
    	}

    	int lengthInBytes = mixedSamples.length << 1;
    	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, lengthInBytes, 2);
    	for (int i = 0, j = 0; i < samples; i++, j++) {
    		short sampleMono  = clampSample(mixedSamples[j]);
    		sampleMono = SoundChannel.adjustSample(sampleMono, monoVol);
    		memoryWriter.writeNext(sampleMono & 0xFFFF);
    	}
    	memoryWriter.flush();
    }

    private void mix(int[] mixedSamples, int addr, int samples, int leftVol, int rightVol, boolean writeSamples) {
    	boolean isStereo = Modules.sceSasCoreModule.getOutputMode() == PSP_SAS_OUTPUTMODE_STEREO;

    	for (int i = 0; i < voices.length; i++) {
    		SoundVoice voice = voices[i];

            if (voice.isPlaying() && !voice.isPaused()) {
            	ISampleSource sampleSource = synthesizers[i].getSampleSource();
            	int playSample = voice.getPlaySample();
            	int restPlay = sampleSource.getNumberSamples() - playSample;
            	if (restPlay <= 0) {
            		// End of voice sample reached
            		voice.setPlaying(false);
            	} else {
            		int numSamples = Math.min(samples, restPlay);
            		if (isStereo) {
            			mixStereo(mixedSamples, sampleSource, playSample, numSamples, voice.getLeftVolume(), voice.getRightVolume());
            		} else {
            			mixMono(mixedSamples, sampleSource, playSample, numSamples, voice.getLeftVolume());
            		}
            		writeSamples = true;

            		voice.setPlaySample(sampleSource.getSampleIndex());
            	}
            }
        }

    	if (isStereo) {
    		copyStereoSamplesToMem(mixedSamples, addr, samples, leftVol, rightVol, writeSamples);
    	} else {
    		copyMonoSamplesToMem(mixedSamples, addr, samples, leftVol, writeSamples);
    	}
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

    public static short getSampleLeft(int sample) {
    	return (short) (sample & 0x0000FFFF);
    }

    public static short getSampleRight(int sample) {
    	return (short) (sample >> 16);
    }

    public static int getSampleStereo(short left, short right) {
    	return (left & 0x0000FFFF) | ((right & 0x0000FFFF) << 16);
    }
}
