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
package jpcsp.memory.mmio.audio;

import static jpcsp.HLE.modules.sceAudio.PSP_AUDIO_VOLUME_MAX;
import static jpcsp.sound.SoundChannel.alCheckError;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.lwjgl.openal.AL10;

import jpcsp.memory.mmio.MMIOHandlerAudio;
import jpcsp.sound.SoundBufferManager;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class AudioLine implements IState {
	public static Logger log = MMIOHandlerAudio.log;
	private static final int STATE_VERSION = 0;
	private final SoundBufferManager soundBufferManager;
	private final int alSource;
	private int frequency = 44100;
	private int waitingBufferSamples;
	private final Map<Integer, Integer> waitingBufferSizes = new HashMap<Integer, Integer>();

	public AudioLine() {
		soundBufferManager = SoundBufferManager.getInstance();

		alSource = AL10.alGenSources();
		alCheckError("alGenSources");
		if (log.isDebugEnabled()) {
			log.debug(String.format("alGenSources returning alSource=%d", alSource));
		}

		AL10.alSourcei(alSource, AL10.AL_LOOPING, AL10.AL_FALSE);
		alCheckError("alSourcei AL_LOOPING");
	}

	private void alSourcePlay() {
		int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
		if (state != AL10.AL_PLAYING) {
			AL10.alSourcePlay(alSource);
			alCheckError("alSourcePlay");
		}
	}

	private void checkFreeBuffers() {
		while (true) {
			int alBuffer = soundBufferManager.checkFreeBuffer(alSource);
			if (alBuffer < 0) {
				break;
			}

			Integer bufferSizeInSamples = waitingBufferSizes.get(alBuffer);
			if (bufferSizeInSamples != null) {
				waitingBufferSamples -= bufferSizeInSamples.intValue();
				if (waitingBufferSamples < 0) {
					waitingBufferSamples = 0;
				}
			}
		}
    }

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
		if (log.isDebugEnabled()) {
			log.debug(String.format("AudioLine frequency=0x%X", frequency));
		}
	}

	public void setVolume(int volume) {
		float gain = volume / (float) PSP_AUDIO_VOLUME_MAX;
		if (log.isDebugEnabled()) {
			log.debug(String.format("AudioLine alSource=%d, volume=0x%X, gain=%f", alSource, volume, gain));
		}
		AL10.alSourcef(alSource, AL10.AL_GAIN, gain);
		alCheckError("alSourcef AL_GAIN");
	}

	public void writeAudioData(int[] data, int offset, int length) {
		int audioBytesLength = length * 4;
    	ByteBuffer directBuffer = soundBufferManager.getDirectBuffer(audioBytesLength);
    	directBuffer.order(ByteOrder.LITTLE_ENDIAN);
		directBuffer.clear();
		directBuffer.limit(audioBytesLength);
		directBuffer.asIntBuffer().put(data, offset, length);
		directBuffer.rewind();

    	int alBuffer = soundBufferManager.getBuffer();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("writeAudioData alBuffer=%d", alBuffer));
    	}
    	waitingBufferSizes.put(alBuffer, length);
    	waitingBufferSamples += length;
		AL10.alBufferData(alBuffer, AL10.AL_FORMAT_STEREO16, directBuffer, frequency);
		alCheckError("alBufferData");
		AL10.alSourceQueueBuffers(alSource, alBuffer);
		alCheckError("alSourceQueueBuffers");
		soundBufferManager.releaseDirectBuffer(directBuffer);

		alSourcePlay();
		poll();
	}

	public void poll() {
		checkFreeBuffers();
	}

	public int getWaitingBuffers() {
		checkFreeBuffers();

		return AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED) - AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_PROCESSED);
	}

	public int getWaitingBufferSamples() {
		checkFreeBuffers();

		return waitingBufferSamples;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		frequency = stream.readInt();

		waitingBufferSamples = 0;
		waitingBufferSizes.clear();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(frequency);
	}
}
