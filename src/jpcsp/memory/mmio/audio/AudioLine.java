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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
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

	public AudioLine() {
		initOpenAL();

		soundBufferManager = SoundBufferManager.getInstance();
		alSource = AL10.alGenSources();
		AL10.alSourcei(alSource, AL10.AL_LOOPING, AL10.AL_FALSE);
	}

	private static void initOpenAL() {
		// Initialize OpenAL
		if (!AL.isCreated()) {
			try {
				AL.create();
			} catch (LWJGLException e) {
				log.error(e);
			}
		}
	}

	private void alSourcePlay() {
		int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
		if (state != AL10.AL_PLAYING) {
			AL10.alSourcePlay(alSource);
		}
	}

	private void checkFreeBuffers() {
    	soundBufferManager.checkFreeBuffers(alSource);
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
			log.debug(String.format("AudioLine volume=0x%X, gain=%f", volume, gain));
		}
		AL10.alSourcef(alSource, AL10.AL_GAIN, gain);
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
		AL10.alBufferData(alBuffer, AL10.AL_FORMAT_STEREO16, directBuffer, frequency);
		AL10.alSourceQueueBuffers(alSource, alBuffer);
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

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		frequency = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(frequency);
	}
}
