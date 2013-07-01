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
package jpcsp.HLE.modules150;

import static java.lang.Math.min;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Audio;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.sound.AudioBlockingOutputAction;
import jpcsp.sound.SoundChannel;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCdevice;

@HLELogging
public class sceAudio extends HLEModule {
    public static Logger log = Modules.getLogger("sceAudio");

	private class DisableAudioSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setChReserveEnabled(!value);
		}
	}

	private class DisableBlockingAudioSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setBlockingEnabled(!value);
		}
	}

    @Override
    public String getName() {
        return "sceAudio";
    }

	@Override
	public void start() {
		SoundChannel.init();

        // The audio driver is capable of handling PCM and VAG (ADPCM) playback,
        // but it uses the same channels for this processing.
        // E.g.: Use channels 0 to 4 to playback 4 VAG files or use channels 0 to 2
        // to playback raw PCM data.
        // Note: Currently, working with pspPCMChannels only is enough.
        pspPCMChannels = new SoundChannel[PSP_AUDIO_CHANNEL_MAX];
        for (int channel = 0; channel < pspPCMChannels.length; channel++) {
            pspPCMChannels[channel] = new SoundChannel(channel);
        }
        pspSRCChannel = new SoundChannel(8);  // Use a special channel 8 to handle SRC functions.

        setSettingsListener("emu.disablesceAudio", new DisableAudioSettingsListerner());
        setSettingsListener("emu.disableblockingaudio", new DisableBlockingAudioSettingsListerner());

        super.start();
	}

	@Override
	public void stop() {
		if (inputDevice != null) {
			ALC11.alcCaptureCloseDevice(inputDevice);
			inputDevice = null;
		}
		inputDeviceInitialized = false;
		captureBuffer = null;

		super.stop();
	}

	protected static final int PSP_AUDIO_VOLUME_MAX = 0x8000;
    protected static final int PSP_AUDIO_CHANNEL_MAX = 8;
    protected static final int PSP_AUDIO_SAMPLE_MIN = 64;
    protected static final int PSP_AUDIO_SAMPLE_MAX = 65472;
    protected static final int PSP_AUDIO_FORMAT_STEREO = 0;
    protected static final int PSP_AUDIO_FORMAT_MONO = 0x10;

    protected SoundChannel[] pspPCMChannels;
    protected SoundChannel pspSRCChannel;

    protected boolean disableChReserve;
    protected boolean disableBlockingAudio;

	protected ALCdevice inputDevice;
	protected ByteBuffer captureBuffer;
	protected IntBuffer samplesBuffer;
	protected boolean inputDeviceInitialized;

	protected static class AudioBlockingInputAction implements IAction {
		private int threadId;
		private int addr;
		private int samples;
		private int frequency;

		public AudioBlockingInputAction(int threadId, int addr, int samples, int frequency) {
			this.threadId = threadId;
			this.addr = addr;
			this.samples = samples;
			this.frequency = frequency;
		}

		@Override
		public void execute() {
			Modules.sceAudioModule.hleAudioBlockingInput(threadId, addr, samples, frequency);
		}
	}

    private void setChReserveEnabled(boolean enabled) {
        disableChReserve = !enabled;
        log.info("Audio ChReserve disabled: " + disableChReserve);
    }

    private void setBlockingEnabled(boolean enabled) {
        disableBlockingAudio = !enabled;
        log.info("Audio Blocking disabled: " + disableBlockingAudio);
    }

    protected static int doAudioOutput(SoundChannel channel, int pvoid_buf) {
        int ret = -1;

        if (channel.isReserved()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("doAudioOutput(%s, 0x%08X)", channel.toString(), pvoid_buf));
        	}
            int bytesPerSample = channel.isFormatStereo() ? 4 : 2;
            int nbytes = bytesPerSample * channel.getSampleLength();
            byte[] data = new byte[nbytes];

            IMemoryReader memoryReader = MemoryReader.getMemoryReader(pvoid_buf, nbytes, 2);
            if (channel.isFormatMono()) {
                int volume = Audio.getVolume(channel.getLeftVolume());
                for (int i = 0; i < nbytes; i += 2) {
                    short sample = (short) memoryReader.readNext();

                    sample = SoundChannel.adjustSample(sample, volume);

                    SoundChannel.storeSample(sample, data, i);
                }
            } else {
                int leftVolume = Audio.getVolume(channel.getLeftVolume());
                int rightVolume = Audio.getVolume(channel.getRightVolume());
                for (int i = 0; i < nbytes; i += 4) {
                    short lsample = (short) memoryReader.readNext();
                    short rsample = (short) memoryReader.readNext();

                    lsample = SoundChannel.adjustSample(lsample, leftVolume);
                    rsample = SoundChannel.adjustSample(rsample, rightVolume);

                    SoundChannel.storeSample(lsample, data, i);
                    SoundChannel.storeSample(rsample, data, i + 2);
                }
            }
            channel.play(data);
            ret = channel.getSampleLength();
        } else {
            log.warn("doAudioOutput: channel " + channel.getIndex() + " not reserved");
        }
        return ret;
    }

    protected static void blockThreadOutput(SoundChannel channel, int addr, int leftVolume, int rightVolume) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
    	blockThreadOutput(threadMan.getCurrentThreadID(), channel, addr, leftVolume, rightVolume);
    	threadMan.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_AUDIO);
    }

    protected static void blockThreadOutput(int threadId, SoundChannel channel, int addr, int leftVolume, int rightVolume) {
    	IAction action = new AudioBlockingOutputAction(threadId, channel, addr, leftVolume, rightVolume);
    	int delayMicros = channel.getUnblockOutputDelayMicros(addr == 0);
    	long schedule = Emulator.getClock().microTime() + delayMicros;
    	Emulator.getScheduler().addAction(schedule, action);
    }

    public void hleAudioBlockingOutput(int threadId, SoundChannel channel, int addr, int leftVolume, int rightVolume) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleAudioBlockingOutput %s", channel.toString()));
    	}

    	if (addr == 0) {
    		// If another thread is also sending audio data on this channel,
    		// do not wait for the channel to be drained, unblock the thread now.
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo thread = threadMan.getThreadById(threadId);
            if (thread != null) {
            	thread.cpuContext._v0 = 0;
                threadMan.hleUnblockThread(threadId);
            }
    	} else if (!channel.isOutputBlocking()) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo thread = threadMan.getThreadById(threadId);
            if (thread != null) {
                changeChannelVolume(channel, leftVolume, rightVolume);
                int ret = doAudioOutput(channel, addr);
                thread.cpuContext._v0 = ret;
                threadMan.hleUnblockThread(threadId);
            }
        } else {
        	blockThreadOutput(threadId, channel, addr, leftVolume, rightVolume);
        }
    }

    protected static int changeChannelVolume(SoundChannel channel, int leftvol, int rightvol) {
        int ret = -1;

        if (channel.isReserved()) {
        	// Negative volume means no change
        	if (leftvol >= 0) {
        		channel.setLeftVolume(leftvol);
        	}
        	if (rightvol >= 0) {
        		channel.setRightVolume(rightvol);
        	}
            ret = 0;
        }

        return ret;
    }

    protected int hleAudioGetChannelRestLength(SoundChannel channel) {
        int len = channel.getRestLength();

        // To avoid small "clicks" in the sound, simulate a rest length of 0
        // when approaching the end of the buffered samples.
        // 2048 is an empirical value.
        if (len <= 2048) {
        	len = 0;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleAudioGetChannelRestLength(%d) = %d", channel.getIndex(), len));
        }

        return len;
    }

    protected int hleAudioSRCChReserve(int sampleCount, int freq, int format) {
        if (disableChReserve) {
            log.warn(String.format("IGNORED hleAudioSRCChReserve sampleCount=%d, freq=%d, format=%d", sampleCount, freq, format));
            return -1;
        }

        if (pspSRCChannel.isReserved()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleAudioSRCChReserve returning ERROR_AUDIO_CHANNEL_ALREADY_RESERVED"));
        	}
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_ALREADY_RESERVED;
        }

        pspSRCChannel.setSampleRate(freq);
        pspSRCChannel.setReserved(true);
        pspSRCChannel.setSampleLength(sampleCount);
        pspSRCChannel.setFormat(format);

        return 0;
    }

    public int checkChannel(int channel) {
    	if (channel < 0 || channel >= PSP_AUDIO_CHANNEL_MAX) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Invalid channel number %d", channel));
    		}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_INVALID_CHANNEL);
    	}

    	return channel;
    }

    public int checkReservedChannel(int channel) {
    	channel = checkChannel(channel);
    	if (!pspPCMChannels[channel].isReserved()) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Channel not reserved %d", channel));
    		}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_INIT);
    	}

    	return channel;
    }

    public int checkSampleCount(int sampleCount) {
        if (sampleCount <= 0 || sampleCount > 0xFFC0 || (sampleCount & 0x3F) != 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Invalid sampleCount 0x%X", sampleCount));
    		}
        	throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_OUTPUT_SAMPLE_DATA_SIZE_NOT_ALIGNED);
        }

    	return sampleCount;
    }

    public int checkSmallSampleCount(int sampleCount) {
    	if (sampleCount < 17 || sampleCount >= 4095 + 17) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Invalid small sampleCount 0x%X", sampleCount));
    		}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_OUTPUT_SAMPLE_DATA_SIZE_NOT_ALIGNED);
    	}

    	return sampleCount;
    }

    public int checkVolume(int volume) {
    	// Negative volume is allowed
    	if (volume > 0xFFFF) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Invalid volume 0x%X", volume));
    		}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_INVALID_VOLUME);
    	}

    	return volume;
    }

    public int checkVolume2(int volume) {
    	if (volume < 0 || volume > 0xFFFFF) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Invalid volume 0x%X", volume));
    		}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_INVALID_VOLUME);
    	}

    	return volume;
    }

    public int checkFormat(int format) {
    	if (format != PSP_AUDIO_FORMAT_STEREO && format != PSP_AUDIO_FORMAT_MONO) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Invalid format 0x%X", format));
    		}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_INVALID_FORMAT);
    	}

    	return format;
    }

    protected void hleAudioBlockingInput(int threadId, int addr, int samples, int frequency) {
		int availableSamples = hleAudioGetInputLength();
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleAudioBlockingInput available samples: %d from %d", availableSamples, samples));
		}

		int bufferBytes = samples << 1;
		if (inputDevice == null) {
			// No input device available, fake device input
			Memory.getInstance().memset(addr, (byte) 0, bufferBytes);
			Modules.ThreadManForUserModule.hleUnblockThread(threadId);
		} else if (availableSamples >= samples) {
			if (captureBuffer == null || captureBuffer.capacity() < bufferBytes) {
				captureBuffer = BufferUtils.createByteBuffer(bufferBytes);
			} else {
				captureBuffer.rewind();
			}

			ALC11.alcCaptureSamples(inputDevice, captureBuffer, samples);

			captureBuffer.rewind();
			IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, samples, 2);
			for (int i = 0; i < samples; i++) {
				short sample = captureBuffer.getShort();
				memoryWriter.writeNext(sample & 0xFFFF);
			}

			if (log.isTraceEnabled()) {
				log.trace(String.format("hleAudioBlockingInput returning %d samples: %s", samples, Utilities.getMemoryDump(addr, bufferBytes, 2, 16)));
			}
			Modules.ThreadManForUserModule.hleUnblockThread(threadId);
		} else {
			blockThreadInput(threadId, addr, samples, frequency, availableSamples);
		}
	}

	public int hleAudioGetInputLength() {
		if (inputDevice == null) {
			return 0;
		}

		if (samplesBuffer == null) {
			samplesBuffer = BufferUtils.createIntBuffer(1);
		}

		ALC10.alcGetInteger(inputDevice, ALC11.ALC_CAPTURE_SAMPLES, samplesBuffer);

		return samplesBuffer.get(0);
	}

	protected int getUnblockInputDelayMicros(int availableSamples, int samples, int frequency) {
		if (availableSamples >= samples) {
			return 0;
		}

		int missingSamples = samples - availableSamples;
		int delayMicros = (int) (missingSamples * 1000000L / frequency);

		return delayMicros;
	}

	protected void blockThreadInput(int addr, int samples, int frequency) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        int threadId = threadMan.getCurrentThreadID();
		threadMan.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_AUDIO);
		blockThreadInput(threadId, addr, samples, frequency, hleAudioGetInputLength());
	}

	protected void blockThreadInput(int threadId, int addr, int samples, int frequency, int availableSamples) {
		int delayMicros = getUnblockInputDelayMicros(availableSamples, samples, frequency);
		if (log.isTraceEnabled()) {
			log.trace(String.format("blockThreadInput waiting %d micros", delayMicros));
		}
		Emulator.getScheduler().addAction(Emulator.getClock().microTime() + delayMicros, new AudioBlockingInputAction(threadId, addr, samples, frequency));
	}

	public int hleAudioInputBlocking(int maxSamples, int frequency, TPointer buffer) {
		if (!inputDeviceInitialized) {
			IntBuffer majorVersion = BufferUtils.createIntBuffer(1);
			IntBuffer minorVersion = BufferUtils.createIntBuffer(1);
			ALC10.alcGetInteger(null, ALC10.ALC_MAJOR_VERSION, majorVersion);
			ALC10.alcGetInteger(null, ALC10.ALC_MINOR_VERSION, minorVersion);
			log.info(String.format("OpenAL Version %d.%d, extensions %s", majorVersion.get(0), minorVersion.get(0), ALC10.alcGetString(null, ALC10.ALC_EXTENSIONS)));

			inputDevice = ALC11.alcCaptureOpenDevice(null, frequency, AL10.AL_FORMAT_MONO16, 10 * 1024);
			if (inputDevice != null) {
				ALC11.alcCaptureStart(inputDevice);
			} else {
				log.warn(String.format("No audio input device available, faking."));
			}

			inputDeviceInitialized = true;
		}

		blockThreadInput(buffer.getAddress(), maxSamples, frequency);

		return 0;
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0x80F1F7E0, version = 150, moduleName = "sceAudio_driver", checkInsideInterrupt = true)
    public int sceAudioInit() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x210567F7, version = 150, moduleName = "sceAudio_driver", checkInsideInterrupt = true)
    public int sceAudioEnd() {
        return 0;
    }

    @HLEFunction(nid = 0xA2BEAA6C, version = 150, moduleName = "sceAudio_driver", checkInsideInterrupt = true)
    public int sceAudioSetFrequency(int frequency) {
    	if (frequency != 44100 && frequency != 48000) {
        	return SceKernelErrors.ERROR_AUDIO_INVALID_FREQUENCY;
        }

        for (int i = 0; i < pspPCMChannels.length; i++) {
    		pspPCMChannels[i].setSampleRate(frequency);
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB61595C0, version = 150, moduleName = "sceAudio_driver", checkInsideInterrupt = true)
    public int sceAudioLoopbackTest() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x927AC32B, version = 150, moduleName = "sceAudio_driver", checkInsideInterrupt = true)
    public int sceAudioSetVolumeOffset() {
        return 0;
    }

    @HLEFunction(nid = 0x8C1009B2, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput(@CheckArgument("checkReservedChannel") int channel, @CheckArgument("checkVolume") int vol, @CanBeNull TPointer pvoid_buf) {
        if (pspPCMChannels[channel].isOutputBlocking()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
        }

        changeChannelVolume(pspPCMChannels[channel], vol, vol);
        int result = doAudioOutput(pspPCMChannels[channel], pvoid_buf.getAddress());
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();

        return result;
    }

    @HLEFunction(nid = 0x136CAF51, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutputBlocking(@CheckArgument("checkReservedChannel") int channel, @CheckArgument("checkVolume") int vol, @CanBeNull TPointer pvoid_buf) {
        if (pvoid_buf.isNull()) {
            if (!pspPCMChannels[channel].isDrained()) {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioOutputBlocking[pvoid_buf==0] blocking " + pspPCMChannels[channel].toString());
                }
                blockThreadOutput(pspPCMChannels[channel], pvoid_buf.getAddress(), vol, vol);
            }
            return 0;
        }

        int result = 0;
        if (!pspPCMChannels[channel].isOutputBlocking() || disableBlockingAudio) {
            if (log.isDebugEnabled()) {
                log.debug("sceAudioOutputBlocking[not blocking] " + pspPCMChannels[channel].toString());
            }
            changeChannelVolume(pspPCMChannels[channel], vol, vol);
            result = doAudioOutput(pspPCMChannels[channel], pvoid_buf.getAddress());
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAudioOutputBlocking[not blocking] returning %d (%s)", result, pspPCMChannels[channel]));
            }
            Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("sceAudioOutputBlocking[blocking] " + pspPCMChannels[channel].toString());
            }
            blockThreadOutput(pspPCMChannels[channel], pvoid_buf.getAddress(), vol, vol);
        }

        return result;
    }

    @HLEFunction(nid = 0xE2D56B2D, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutputPanned(@CheckArgument("checkReservedChannel") int channel, @CheckArgument("checkVolume") int leftvol, @CheckArgument("checkVolume") int rightvol, @CanBeNull TPointer pvoid_buf) {
        if (pspPCMChannels[channel].isOutputBlocking()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
        }

        changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
        int result = doAudioOutput(pspPCMChannels[channel], pvoid_buf.getAddress());
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();

        return result;
    }

    @HLEFunction(nid = 0x13F592BC, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutputPannedBlocking(@CheckArgument("checkReservedChannel") int channel, @CheckArgument("checkVolume") int leftvol, @CheckArgument("checkVolume") int rightvol, @CanBeNull TPointer pvoid_buf) {
        if (pvoid_buf.isNull()) {
            // Tested on PSP:
            // An output adress of 0 is actually a special code for the PSP.
            // It means that we must stall processing until all the previous
        	// unplayed samples' data is output.
            if (!pspPCMChannels[channel].isDrained()) {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioOutputPannedBlocking[pvoid_buf==0] blocking " + pspPCMChannels[channel].toString());
                }
                blockThreadOutput(pspPCMChannels[channel], pvoid_buf.getAddress(), leftvol, rightvol);
            }
            return 0;
        }

        int result = 0;
        if (!pspPCMChannels[channel].isOutputBlocking() || disableBlockingAudio) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAudioOutputPannedBlocking[not blocking] leftVol=%d, rightVol=%d, channel=%s", leftvol, rightvol, pspPCMChannels[channel].toString()));
            }
            changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
            result = doAudioOutput(pspPCMChannels[channel], pvoid_buf.getAddress());
            Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAudioOutputPannedBlocking[blocking] leftVol=%d, rightVol=%d, channel=%s", leftvol, rightvol, pspPCMChannels[channel].toString()));
            }
            blockThreadOutput(pspPCMChannels[channel], pvoid_buf.getAddress(), leftvol, rightvol);
        }

        return result;
    }

    @HLEFunction(nid = 0x5EC81C55, version = 150, checkInsideInterrupt = true)
    public int sceAudioChReserve(int channel, int sampleCount, int format) {
        if (disableChReserve) {
            log.warn(String.format("IGNORED sceAudioChReserve channel=%d, sampleCount=%d, format=%d", channel, sampleCount, format));
            return SceKernelErrors.ERROR_AUDIO_NO_CHANNELS_AVAILABLE;
        }

        if (channel >= 0) {
        	channel = checkChannel(channel);
            if (pspPCMChannels[channel].isReserved()) {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("sceAudioChReserve failed - channel %d already in use", channel));
            	}
                return SceKernelErrors.ERROR_AUDIO_INVALID_CHANNEL;
            }
        } else {
        	// The PSP is searching for a free channel, starting with the highest channel number.
            for (int i = pspPCMChannels.length - 1; i >= 0; i--) {
                if (!pspPCMChannels[i].isReserved()) {
                    channel = i;
                    break;
                }
            }

            if (channel < 0) {
                log.debug("sceAudioChReserve failed - no free channels available");
                return SceKernelErrors.ERROR_AUDIO_NO_CHANNELS_AVAILABLE;
            }
        }

        // The validity of the sampleCount and format parameters is only checked after the channel check
        sampleCount = checkSampleCount(sampleCount);
        format = checkFormat(format);

        pspPCMChannels[channel].setReserved(true);
        pspPCMChannels[channel].setSampleLength(sampleCount);
        pspPCMChannels[channel].setFormat(format);

        return channel;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x41EFADE7, version = 150, checkInsideInterrupt = true)
    public int sceAudioOneshotOutput() {
        return 0;
    }

    @HLEFunction(nid = 0x6FC46853, version = 150, checkInsideInterrupt = true)
    public int sceAudioChRelease(@CheckArgument("checkChannel") int channel) {
        if (!pspPCMChannels[channel].isReserved()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }

        pspPCMChannels[channel].release();
        pspPCMChannels[channel].setReserved(false);

        return 0;
    }

    @HLEFunction(nid = 0xB011922F, version = 150, checkInsideInterrupt = true)
    public int sceAudioGetChannelRestLength(@CheckArgument("checkChannel") int channel) {
    	return hleAudioGetChannelRestLength(pspPCMChannels[channel]);
    }

    @HLEFunction(nid = 0xCB2E439E, version = 150, checkInsideInterrupt = true)
    public int sceAudioSetChannelDataLen(@CheckArgument("checkReservedChannel") int channel, @CheckArgument("checkSampleCount") int sampleCount) {
        pspPCMChannels[channel].setSampleLength(sampleCount);

        return 0;
    }

    @HLEFunction(nid = 0x95FD0C2D, version = 150, checkInsideInterrupt = true)
    public int sceAudioChangeChannelConfig(@CheckArgument("checkReservedChannel") int channel, @CheckArgument("checkFormat") int format) {
    	pspPCMChannels[channel].setFormat(format);

        return 0;
    }

    @HLEFunction(nid = 0xB7E1D8E7, version = 150, checkInsideInterrupt = true)
    public int sceAudioChangeChannelVolume(@CheckArgument("checkReservedChannel") int channel, @CheckArgument("checkVolume") int leftvol, @CheckArgument("checkVolume") int rightvol) {
    	return changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
    }

    @HLEFunction(nid = 0x01562BA3, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2Reserve(int sampleCount) {
        return hleAudioSRCChReserve(sampleCount, 44100, SoundChannel.FORMAT_STEREO);
    }

    @HLEFunction(nid = 0x43196845, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2Release() {
        return sceAudioSRCChRelease();
    }

    @HLEFunction(nid = 0x2D53F36E, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2OutputBlocking(@CheckArgument("checkVolume2") int vol, @CanBeNull TPointer buf) {
        return sceAudioSRCOutputBlocking(vol, buf);
    }

    @HLEFunction(nid = 0x647CEF33, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2GetRestSample() {
    	if (!pspSRCChannel.isReserved()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceAudioOutput2GetRestSample returning ERROR_AUDIO_CHANNEL_NOT_RESERVED"));
        	}
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }

        return hleAudioGetChannelRestLength(pspSRCChannel);
    }

    @HLEFunction(nid = 0x63F2889C, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2ChangeLength(@CheckArgument("checkSmallSampleCount") int sampleCount) {
    	if (!pspSRCChannel.isReserved()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceAudioOutput2ChangeLength returning ERROR_AUDIO_CHANNEL_NOT_RESERVED"));
        	}
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }

        pspSRCChannel.setSampleLength(sampleCount);

        return 0;
    }

    @HLEFunction(nid = 0x38553111, version = 150, checkInsideInterrupt = true)
    public int sceAudioSRCChReserve(int sampleCount, int freq, int format) {
        return hleAudioSRCChReserve(sampleCount, freq, format);
    }

    @HLEFunction(nid = 0x5C37C0AE, version = 150, checkInsideInterrupt = true)
    public int sceAudioSRCChRelease() {
    	if (!pspSRCChannel.isReserved()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceAudioSRCChRelease returning ERROR_AUDIO_CHANNEL_NOT_RESERVED"));
        	}
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }

        pspSRCChannel.release();
        pspSRCChannel.setReserved(false);

        return 0;
    }

    @HLEFunction(nid = 0xE0727056, version = 150, checkInsideInterrupt = true)
    public int sceAudioSRCOutputBlocking(@CheckArgument("checkVolume2") int vol, @CanBeNull TPointer buf) {
    	// Tested on PSP: any sound volume above MAX_VOLUME has the same effect as MAX_VOLUME.
    	int channelVolume = min(SoundChannel.MAX_VOLUME, vol);
    	pspSRCChannel.setVolume(channelVolume);

        if (buf.isNull()) {
            // Tested on PSP:
            // SRC audio also delays when buf == 0, in order to drain all
            // audio samples from the audio driver.
            if (!pspSRCChannel.isDrained()) {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioSRCOutputBlocking[buf==0] blocking " + pspSRCChannel);
                }
                // Do not update volume, it has already been updated above
                blockThreadOutput(pspSRCChannel, buf.getAddress(), -1, -1);
            }
        } else if (!pspSRCChannel.isReserved() && !disableChReserve) {
        	// Channel is automatically reserved. The audio data (buf) is not used in this case.
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAudioSRCOutputBlocking automatically reserving channel %s", pspSRCChannel));
            }
            pspSRCChannel.setReserved(true);
        } else {
            if (!pspSRCChannel.isOutputBlocking() || disableBlockingAudio) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceAudioSRCOutputBlocking[not blocking] %s to %s", buf, pspSRCChannel.toString()));
                }
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
                return doAudioOutput(pspSRCChannel, buf.getAddress());
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAudioSRCOutputBlocking[blocking] %s to %s", buf, pspSRCChannel.toString()));
            }
            // Do not update volume, it has already been updated above
            blockThreadOutput(pspSRCChannel, buf.getAddress(), -1, -1);
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x086E5895, version = 150, checkInsideInterrupt = true)
    public int sceAudioInputBlocking(int maxSamples, int frequency, TPointer buffer) {
		if (frequency != 44100 && frequency != 22050 && frequency != 11025) {
			return SceKernelErrors.ERROR_AUDIO_INVALID_FREQUENCY;
		}

		return hleAudioInputBlocking(maxSamples, frequency, buffer);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D4BEC68, version = 150, checkInsideInterrupt = true)
    public int sceAudioInput() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA708C6A6, version = 150, checkInsideInterrupt = true)
    public int sceAudioGetInputLength() {
        return hleAudioGetInputLength();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x87B2E651, version = 150, checkInsideInterrupt = true)
    public int sceAudioWaitInputEnd() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7DE61688, version = 150, checkInsideInterrupt = true)
    public int sceAudioInputInit() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE926D3FB, version = 150, checkInsideInterrupt = true)
    public int sceAudioInputInitEx() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA633048E, version = 150, checkInsideInterrupt = true)
    public int sceAudioPollInputEnd() {
        return 0;
    }

    @HLEFunction(nid = 0xE9D97901, version = 150, checkInsideInterrupt = true)
    public int sceAudioGetChannelRestLen(@CheckArgument("checkChannel") int channel) {
    	return hleAudioGetChannelRestLength(pspPCMChannels[channel]);
    }
}