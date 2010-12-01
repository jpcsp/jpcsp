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

import jpcsp.hardware.Audio;
import jpcsp.Memory;

public class SoundVoice extends SoundChannel {
    private static final int NORMAL_PITCH = 0x1000;
    private short[] samples;
    private int loopMode;
    private int pitch;
    private int noise;
    private boolean playing;
    private boolean paused;
    private boolean on;
    private boolean off;
    private VoiceADSREnvelope envelope;

    public class VoiceADSREnvelope {
        public int AttackRate;
        public int DecayRate;
        public int SustainRate;
        public int ReleaseRate;
        public int AttackCurveType;
        public int DecayCurveType;
        public int SustainCurveType;
        public int ReleaseCurveType;
        public int SustainLevel;
        public int height;

        public VoiceADSREnvelope() {
            AttackRate = 0;
            DecayRate = 0;
            SustainRate = 0;
            ReleaseRate = 0;
            SustainLevel = 0;
            height = 0;
        }
    }

	public SoundVoice(int index) {
		super(index);
        samples = null;
        loopMode = 0;
        pitch = NORMAL_PITCH;
        noise = 0;
        playing = false;
        paused = false;
        on = false;
        off = true;
        envelope = new VoiceADSREnvelope();
	}

    private byte[] encodeSamples() {
    	int nsamples = samples.length;
        byte[] samplesBuffer = new byte[nsamples * 4];
        int leftVol = Audio.getVolume(getLeftVolume());
        int rightVol = Audio.getVolume(getRightVolume());

        for (int i = 0; i < nsamples; i++) {
            short sample = samples[i];
            short lval = adjustSample(sample, leftVol);
            short rval = adjustSample(sample, rightVol);
            storeSample(lval, samplesBuffer, i * 4);
            storeSample(rval, samplesBuffer, i * 4 + 2);
        }

        return samplesBuffer;
    }

    public void synthesize(int addr, int length) {
        Memory mem = Memory.getInstance();
        if(samples != null) {
            if(isOn() && isPlaying() && !isPaused()) {
                play(encodeSamples());
                setPlaying(false);
            } else if(isOff()) {
                release();
                mem.memset(addr, (byte)0, length);
            } else {
                mem.memset(addr, (byte)0, length);
            }
        }
    }

    public void synthesizeWithMix(int addr, int length, int[] mix) {
        Memory mem = Memory.getInstance();
        if(samples != null) {
            if(isOn() && isPlaying() && !isPaused()) {
                play(encodeSamples());
                setPlaying(false);
            } else if(isOff()) {
                release();
                mem.memset(addr, (byte)0, length);
            } else {
                mem.memset(addr, (byte)0, length);
            }
        }
    }

    public void on() {
        on = true;
        off = false;
        envelope.height = 0x10000000;
        setPlaying(true);
	}

    public void off() {
        on = false;
        off = true;
        envelope.height = 0;
        setPlaying(false);
    }

    public VoiceADSREnvelope getEnvelope() {
    	return envelope;
    }

    public boolean isOn() {
		return on;
	}

    public boolean isOff() {
		return off;
	}

    public boolean isPlaying() {
		return playing;
	}

	public void setPlaying(boolean playing) {
		this.playing = playing;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public void setSamples(short[] samples) {
		this.samples = samples;
	}

    public short[] getSamples() {
		return samples;
	}

	public int getLoopMode() {
		return loopMode;
	}

	public void setLoopMode(int loopMode) {
		this.loopMode = loopMode;
	}

	public int getPitch() {
		return pitch;
	}

	public void setPitch(int pitch) {
		this.pitch = pitch;
	}

	public int getNoise() {
		return noise;
	}

	public void setNoise(int noise) {
		this.noise = noise;
	}

	@Override
	public int getSampleRate() {
		return super.getSampleRate() * getPitch() / NORMAL_PITCH;
	}
}