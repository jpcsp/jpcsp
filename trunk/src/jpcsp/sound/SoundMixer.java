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

public class SoundMixer {

    private SoundVoice[] sasVoices;

    public SoundMixer() {
    }

    private void mixSamples(short[] samples, int smplAddr, int smplLength) {
        Memory mem = Memory.getInstance();
        for (int i = 0; i < smplLength; i++) {
            int sAddr = (smplAddr + i * 2);
            short s1 = (short) mem.read16(sAddr);
            short s2 = samples[i];
            // Use floats for a better value approximation.
            float fSum = (float)(s1 / 32768.0f) + (float)(s2 / 32768.0f);
            // Clamp the sound values as required by PCM standards.
            if(fSum > 1.0f) fSum = 1.0f;
            if(fSum < -1.0f) fSum = -1.0f;
            short sOut = (short) (fSum * 32768.0f);
            mem.write16(sAddr, sOut);
        }
    }

    private short[] drainSamples(short[] samples, int smplLength) {
        short[] res = new short[samples.length - smplLength];
        for (int i = 0; i < res.length; i++) {
            res[i] = samples[smplLength + i];
        }
        return res;
    }

    public void updateVoices(SoundVoice[] voices) {
        sasVoices = voices;
    }

    /**
     * Synthesizing audio function.
     * @param addr Output address for the PCM data (must be 64-byte aligned).
     * @param length Size of the PCM buffer in use (matches the sound granularity).
     */
    public void synthesize(int addr, int length) {
        Memory mem = Memory.getInstance();
        for (int i = 0; i < sasVoices.length; i++) {
            // The voice is ON.
            if (sasVoices[i].isOn()) {
                if (sasVoices[i].isPlaying()) {
                    // If the sample length is superior to our granularity, then
                    // drain this voice's samples.
                    if (sasVoices[i].getSamples().length > length) {
                        sasVoices[i].setSamples(drainSamples(sasVoices[i].getSamples(), length));
                    }
                    // Use OpenAL to playback this voice's samples.
                    // TODO: Replace this with an improved and working version
                    // of mixSamples solely based on memory writing.
                    sasVoices[i].play(sasVoices[i].encodeSamples());
                    // Always perform one-shot playback.
                    sasVoices[i].setPlaying(false);
                } else {
                    // Flush OpenAL's buffers.
                    sasVoices[i].checkFreeBuffers();
                }
            // The voice is OFF.
            } else {
                if (!sasVoices[i].isPlaying()) {
                    // Erase the memory area supposedly used for
                    // audio synthesis and release OpenAL's buffers.
                    sasVoices[i].release();
                    mem.memset(addr, (byte) 0, length);
                }
            }
        }
    }

    /**
     * Synthesizing audio function with mix.
     * @param addr Output address for the PCM data (must be 64-byte aligned).
     * @param length Size of the PCM buffer in use (matches the sound granularity).
     * @param mix Array containing the PCM mix data.
     */
    public void synthesizeWithMix(int addr, int length, int[] mix) {
        for (int i = 0; i < sasVoices.length; i++) {
            // The voice is ON.
            if (sasVoices[i].isOn()) {
                if (sasVoices[i].isPlaying()) {
                    // If the sample length is superior to our granularity, then
                    // drain this voice's samples.
                    if (sasVoices[i].getSamples().length > length) {
                        sasVoices[i].setSamples(drainSamples(sasVoices[i].getSamples(), length));
                    }
                    // Use OpenAL to playback this voice's samples.
                    // TODO: Replace this with an improved and working version
                    // of addSamples solely based on memory writing.
                    sasVoices[i].play(sasVoices[i].encodeSamples());
                    // Always perform one-shot playback.
                    sasVoices[i].setPlaying(false);
                } else {
                    // Flush OpenAL's buffers.
                    sasVoices[i].checkFreeBuffers();
                }
            // The voice is OFF.
            } else {
                if (!sasVoices[i].isPlaying()) {
                    // Release OpenAL's buffers and don't change the PCM mix
                    // data area.
                    sasVoices[i].release();
                }
            }
        }
    }
}