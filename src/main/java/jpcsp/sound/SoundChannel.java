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

import static jpcsp.Emulator.exitCalled;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jpcsp.HLE.modules.sceAudio;

import org.apache.log4j.Logger;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.EXTThreadLocalContext;

public class SoundChannel {
	private static Logger log = sceAudio.log;
	private static final boolean doCheckError = false;
	private static volatile boolean isExit = false;
	private static volatile boolean isInit = false;
	private static long initDevice;
	private static long initContext;
	public static final int FORMAT_MONO = 0x10;
	public static final int FORMAT_STEREO = 0x00;
    //
    // The PSP is using a buffer equal to the sampleSize.
    // However, the audio data is not always streamed as fast on Jpcsp as on
    // a real PSP which can lead to buffer underflows,
    // causing discontinuities in the audio that are perceived as "clicks".
    //
    // So, we allocate several buffers of sampleSize, just enough to store
    // a given amount of audio time.
    // This has the disadvantage to introduce a small delay when playing
    // a new sound: a PSP application is typically sending continuously
    // sound data, even when nothing can be heard ("0" values are sent).
    // And we have first to play these buffered blanks before hearing
    // the real sound itself.
	// E.g. BUFFER_SIZE_IN_MILLIS = 100 gives a 0.1 second delay.
	private static final int BUFFER_SIZE_IN_MILLIS = 100;
	public  static final int MAX_VOLUME = 0x8000;
	private static final int DEFAULT_VOLUME = MAX_VOLUME;
	private static final int DEFAULT_SAMPLE_RATE = 44100;
	private SoundBufferManager soundBufferManager;
	private int index;
	private boolean reserved;
	private int leftVolume;
	private int rightVolume;
    private int alSource;
    private int sampleRate;
    private int sampleLength;
    private int format;
    private int numberBlockingBuffers;
    private int minimumNumberBuffers;
    private boolean busy;

    public static void init() {
    	if (!isInit) {
	    	initDevice = ALC10.alcOpenDevice((String) null);
	    	ALCCapabilities deviceCapabilities = ALC.createCapabilities(initDevice);
	    	initContext = ALC10.alcCreateContext(initDevice, (IntBuffer) null);
	    	setThreadInitContext();
	    	AL.createCapabilities(deviceCapabilities);

	    	// Check if a required extension is available
	    	final String extensionName = "ALC_EXT_thread_local_context";
	    	if (!ALC10.alcIsExtensionPresent(initDevice, extensionName)) {
	    		log.error(String.format("Required extension %s is not available", extensionName));
	    	}

	    	isInit = true;
    	} else {
    		setThreadInitContext();
    	}

    	isExit = false;
    }

    public static void exit() {
    	if (isInit) {
    		clearThreadInitContext();
    		ALC10.alcDestroyContext(initContext);
    		initContext = 0L;
    		ALC10.alcCloseDevice(initDevice);
    		initDevice = 0L;

    		isInit = false;
    	}

    	isExit = true;
    }

    public static void setThreadInitContext() {
    	if (!EXTThreadLocalContext.alcSetThreadContext(initContext)) {
    		if (!exitCalled()) {
    			log.error(String.format("setThreadInitContext alcMakeContextCurrent failed with error 0x%X", ALC10.alcGetError(initDevice)));
    		}
    	} else if (log.isDebugEnabled()) {
    		log.debug(String.format("setThreadInitContext initContext=0x%X, thread=0x%X", initContext, Thread.currentThread().getId()));
    	}
    }

    public static void clearThreadInitContext() {
    	if (!EXTThreadLocalContext.alcSetThreadContext(0L)) {
    		log.error(String.format("clearThreadInitContext alcMakeContextCurrent failed with error 0x%X", ALC10.alcGetError(initDevice)));
    	} else if (log.isDebugEnabled()) {
    		log.debug(String.format("clearThreadInitContext thread=0x%X", Thread.currentThread().getId()));
    	}
    }

    public SoundChannel(int index) {
		soundBufferManager = SoundBufferManager.getInstance();
		this.index = index;
		reserved = false;
		leftVolume = DEFAULT_VOLUME;
		rightVolume = DEFAULT_VOLUME;
		alSource = AL10.alGenSources();
		sampleRate = DEFAULT_SAMPLE_RATE;
		updateNumberBlockingBuffers();

		AL10.alSourcei(alSource, AL10.AL_LOOPING, AL10.AL_FALSE);
		alCheckError("alSourcei AL_LOOPING");
	}

	public static void alCheckError(String comment) {
		if (doCheckError) {
			int alError = AL10.alGetError();
			if (alError != AL10.AL_NO_ERROR) {
				String errorString;
				switch (alError) {
					case AL10.AL_INVALID_ENUM:      errorString = "AL_INVALID_ENUM";      break;
					case AL10.AL_INVALID_NAME:      errorString = "AL_INVALID_NAME";      break;
					case AL10.AL_INVALID_OPERATION: errorString = "AL_INVALID_OPERATION"; break;
					case AL10.AL_INVALID_VALUE:     errorString = "AL_INVALID_VALUE";     break;
					default:
						errorString = String.format("0x%X", alError);
						break;
				}
				log.error(String.format("%s returning error %s", comment, errorString));
			}
		}
	}

    private void updateNumberBlockingBuffers() {
    	if (getSampleLength() > 0) {
	    	// Compute the number of buffers required to store the required
	    	// amount of audio time
	    	float bufferSizeInSamples = getSampleRate() * BUFFER_SIZE_IN_MILLIS / 1000.f;
	    	numberBlockingBuffers = Math.round(bufferSizeInSamples / getSampleLength());
    	}

    	// At least 1 blocking buffer
    	numberBlockingBuffers = Math.max(numberBlockingBuffers, 1);

    	// For very small sample length, wait for a minimum number of buffers
    	// before starting playing the audio otherwise, small cracks can be produced.
    	if (getSampleLength() <= 0x40) {
    		minimumNumberBuffers = 10;
    	} else {
    		minimumNumberBuffers = 0;
    	}
    }

    public int getIndex() {
		return index;
	}

	public boolean isReserved() {
		return reserved;
	}

	public void setReserved(boolean reserved) {
		this.reserved = reserved;
	}

	public int getLeftVolume() {
		return leftVolume;
	}

	public void setLeftVolume(int leftVolume) {
		this.leftVolume = leftVolume;
	}

	public int getRightVolume() {
		return rightVolume;
	}

	public void setRightVolume(int rightVolume) {
		this.rightVolume = rightVolume;
	}

	public void setVolume(int volume) {
		setLeftVolume(volume);
		setRightVolume(volume);
	}

	public int getSampleLength() {
		return sampleLength;
	}

	public void setSampleLength(int sampleLength) {
		if (this.sampleLength != sampleLength) {
			this.sampleLength = sampleLength;
			updateNumberBlockingBuffers();
		}
	}

	public int getFormat() {
		return format;
	}

	public void setFormat(int format) {
		this.format = format;
	}

	public boolean isFormatStereo() {
		return (format & FORMAT_MONO) == FORMAT_STEREO;
	}

	public boolean isFormatMono() {
		return (format & FORMAT_MONO) == FORMAT_MONO;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		if (this.sampleRate != sampleRate) {
			this.sampleRate = sampleRate;
			updateNumberBlockingBuffers();
		}
	}

	private void alSourcePlay() {
		int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
		if (state != AL10.AL_PLAYING) {
			if (minimumNumberBuffers <= 0 || getWaitingBuffers() >= minimumNumberBuffers) {
				AL10.alSourcePlay(alSource);
				alCheckError("alSourcePlay");
			}
		}
    }

    private void alSourceQueueBuffer(byte[] buffer) {
    	int alBuffer = soundBufferManager.getBuffer();
    	ByteBuffer directBuffer = soundBufferManager.getDirectBuffer(buffer.length);
		directBuffer.clear();
		directBuffer.limit(buffer.length);
		directBuffer.put(buffer);
		directBuffer.rewind();
		int alFormat = isFormatStereo() ? AL10.AL_FORMAT_STEREO16 : AL10.AL_FORMAT_MONO16;
		AL10.alBufferData(alBuffer, alFormat, directBuffer, getSampleRate());
		alCheckError("alBufferData");
		AL10.alSourceQueueBuffers(alSource, alBuffer);
		alCheckError("alSourceQueueBuffers");
		soundBufferManager.releaseDirectBuffer(directBuffer);
		alSourcePlay();
		checkFreeBuffers();

		if (log.isDebugEnabled()) {
			log.debug(String.format("alSourceQueueBuffer buffer=%d, %s", alBuffer, toString()));
		}
    }

    public void checkFreeBuffers() {
    	soundBufferManager.checkFreeBuffers(alSource);
    }

    public void release() {
    	AL10.alSourceStop(alSource);
		alCheckError("alSourceStop");
    	checkFreeBuffers();
    }

    public void play(byte[] buffer) {
    	alSourceQueueBuffer(buffer);
    }

    private int getWaitingBuffers() {
    	checkFreeBuffers();

    	return AL10.alGetSourcei(alSource, AL10.AL_BUFFERS_QUEUED);
    }

    private int getSourceSampleOffset() {
    	int sampleOffset = AL10.alGetSourcei(alSource, AL11.AL_SAMPLE_OFFSET);
		alCheckError("alGetSourcei AL_SAMPLE_OFFSET");
    	if (isFormatStereo()) {
    		sampleOffset /= 2;
    	}

    	return sampleOffset;
    }

    public boolean isOutputBlocking() {
    	if (isExit) {
    		return true;
    	}

    	return getWaitingBuffers() >= numberBlockingBuffers;
    }

    public boolean isDrained() {
    	if (isEnded()) {
    		return true;
    	}

    	if (getWaitingBuffers() > 1) {
    		return false;
    	}

    	return true;
    }

    public int getUnblockOutputDelayMicros(boolean waitForCompleteDrain) {
    	// Return the delay required for the processing of the playing buffer
    	if (isExit || isEnded()) {
    		return 0;
    	}

    	int samples;
    	if (waitForCompleteDrain) {
    		samples = getDrainLength();
    	} else {
    		samples = getSampleLength() - getSourceSampleOffset();
    	}
    	float delaySecs = samples / (float) getSampleRate();
    	int delayMicros = (int) (delaySecs * 1000000);

    	return delayMicros;
    }

    public int getDrainLength() {
    	int waitingBuffers = getWaitingBuffers();
    	if (waitingBuffers > 0) {
    		// getWaitingBuffers also returns the currently playing buffer,
    		// do not count it
    		waitingBuffers--;
    	}
    	int restLength = waitingBuffers * getSampleLength();

    	return restLength;
    }

    public int getRestLength() {
    	int restLength = getDrainLength();
    	if (!isEnded()) {
    		restLength += getSampleLength() - getSourceSampleOffset();
    	}

    	return restLength;
    }

    public boolean isEnded() {
    	checkFreeBuffers();

    	int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
		alCheckError("alGetSourcei AL_SOURCE_STATE");
		if (state == AL10.AL_PLAYING) {
			return false;
		}

		return true;
    }

    public static short adjustSample(short sample, int volume) {
        return (short) ((((int) sample) * volume) >> 15);
    }

    public static void storeSample(short sample, byte[] data, int index) {
    	data[index] = (byte) sample;
    	data[index + 1] = (byte) (sample >> 8);
    }

	public boolean isBusy() {
		return busy;
	}

	public void setBusy(boolean busy) {
		this.busy = busy;
	}

    @Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		s.append(String.format("SoundChannel[%d](", index));
		if (!isExit) {
			s.append(String.format("sourceSampleOffset=%d", getSourceSampleOffset()));
			s.append(String.format(", restLength=%d", getRestLength()));
			s.append(String.format(", buffers queued=%d", getWaitingBuffers()));
			s.append(String.format(", isOutputBlock=%b", isOutputBlocking()));
			s.append(String.format(", %s", isFormatStereo() ? "Stereo" : "Mono"));
			s.append(String.format(", reserved=%b", reserved));
			s.append(String.format(", sampleLength=%d", getSampleLength()));
			s.append(String.format(", sampleRate=%d", getSampleRate()));
			s.append(String.format(", busy=%b", busy));
		}
		s.append(")");

		return s.toString();
	}
}