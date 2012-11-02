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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Audio;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.sound.AudioBlockingOutputAction;
import jpcsp.sound.SoundChannel;

import org.apache.log4j.Logger;

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

    private void setChReserveEnabled(boolean enabled) {
        disableChReserve = !enabled;
        log.info("Audio ChReserve disabled: " + disableChReserve);
    }

    private void setBlockingEnabled(boolean enabled) {
        disableBlockingAudio = !enabled;
        log.info("Audio Blocking disabled: " + disableBlockingAudio);
    }

    protected int doAudioOutput(SoundChannel channel, int pvoid_buf) {
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

    protected void blockThreadOutput(SoundChannel channel, int addr, int leftVolume, int rightVolume) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
    	blockThreadOutput(threadMan.getCurrentThreadID(), channel, addr, leftVolume, rightVolume);
    	threadMan.hleBlockCurrentThread();
    }

    protected void blockThreadOutput(int threadId, SoundChannel channel, int addr, int leftVolume, int rightVolume) {
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

    protected int changeChannelVolume(SoundChannel channel, int leftvol, int rightvol) {
        int ret = -1;

        if (channel.isReserved()) {
            channel.setLeftVolume(leftvol);
            channel.setRightVolume(rightvol);
            ret = 0;
        }

        return ret;
    }

    protected int hleAudioGetChannelRestLen(SoundChannel channel) {
        int len = channel.getRestLength();

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleAudioGetChannelRestLen(%d) = %d", channel.getIndex(), len));
        }

        return len;
    }

    protected int hleAudioSRCChReserve(int sampleCount, int freq, int format) {
        if (disableChReserve) {
            log.warn(String.format("IGNORED hleAudioSRCChReserve sampleCount=%d, freq=%d, format=%d", sampleCount, freq, format));
            return -1;
        }

        if (!pspSRCChannel.isReserved()) {
        	pspSRCChannel.setSampleRate(freq);
            pspSRCChannel.setReserved(true);
            pspSRCChannel.setSampleLength(sampleCount);
            pspSRCChannel.setFormat(format);
        }

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
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioSetFrequency frequency=%d", frequency));
    	}

    	if (frequency != 44100 && frequency != 48000) {
        	return -1;
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
    public int sceAudioOutput(@CheckArgument("checkReservedChannel") int channel, int vol, @CanBeNull TPointer pvoid_buf) {
        if (pspPCMChannels[channel].isOutputBlocking()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
        }

        changeChannelVolume(pspPCMChannels[channel], vol, vol);
        int result = doAudioOutput(pspPCMChannels[channel], pvoid_buf.getAddress());
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();

        return result;
    }

    @HLEFunction(nid = 0x136CAF51, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutputBlocking(@CheckArgument("checkReservedChannel") int channel, int vol, @CanBeNull TPointer pvoid_buf) {
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
    public int sceAudioOutputPanned(@CheckArgument("checkReservedChannel") int channel, int leftvol, int rightvol, @CanBeNull TPointer pvoid_buf) {
        if (pspPCMChannels[channel].isOutputBlocking()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
        }

        changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
        int result = doAudioOutput(pspPCMChannels[channel], pvoid_buf.getAddress());
        Modules.ThreadManForUserModule.hleRescheduleCurrentThread();

        return result;
    }

    @HLEFunction(nid = 0x13F592BC, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutputPannedBlocking(@CheckArgument("checkReservedChannel") int channel, int leftvol, int rightvol, @CanBeNull TPointer pvoid_buf) {
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
            return -1;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAudioChReserve channel=%d, sampleCount=%d, format=%d", channel, sampleCount, format));
        }

        if (channel != -1) {
        	channel = checkChannel(channel);
            if (pspPCMChannels[channel].isReserved()) {
                log.warn(String.format("sceAudioChReserve failed - channel %d already in use", channel));
                channel = -1;
            }
        } else {
            for (int i = 0; i < pspPCMChannels.length; i++) {
                if (!pspPCMChannels[i].isReserved()) {
                    channel = i;
                    break;
                }
            }
            if (channel == -1) {
                log.warn("sceAudioChReserve failed - no free channels available");
            }
        }

        if (channel != -1) {
            pspPCMChannels[channel].setReserved(true);
            pspPCMChannels[channel].setSampleLength(sampleCount);
            pspPCMChannels[channel].setFormat(format);
        }

        return channel;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x41EFADE7, version = 150, checkInsideInterrupt = true)
    public int sceAudioOneshotOutput() {
        return 0;
    }

    @HLEFunction(nid = 0x6FC46853, version = 150, checkInsideInterrupt = true)
    public int sceAudioChRelease(@CheckArgument("checkChannel") int channel) {
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceAudioChRelease channel=%d", channel));
        }

        if (!pspPCMChannels[channel].isReserved()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }

        pspPCMChannels[channel].release();
        pspPCMChannels[channel].setReserved(false);

        return 0;
    }

    @HLEFunction(nid = 0xB011922F, version = 150, checkInsideInterrupt = true)
    public int sceAudioGetChannelRestLength(@CheckArgument("checkChannel") int channel) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioGetChannelRestLength channel=%d", channel));
    	}

    	return hleAudioGetChannelRestLen(pspPCMChannels[channel]);
    }

    @HLEFunction(nid = 0xCB2E439E, version = 150, checkInsideInterrupt = true)
    public int sceAudioSetChannelDataLen(@CheckArgument("checkReservedChannel") int channel, int sampleCount) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAudioSetChannelDataLen channel=%d, sampleCount=%d", channel, sampleCount));
        }

        pspPCMChannels[channel].setSampleLength(sampleCount);

        return 0;
    }

    @HLEFunction(nid = 0x95FD0C2D, version = 150, checkInsideInterrupt = true)
    public int sceAudioChangeChannelConfig(@CheckArgument("checkReservedChannel") int channel, int format) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioChangeChannelConfig channel=%d, format=%d", channel, format));
    	}

    	pspPCMChannels[channel].setFormat(format);

        return 0;
    }

    @HLEFunction(nid = 0xB7E1D8E7, version = 150, checkInsideInterrupt = true)
    public int sceAudioChangeChannelVolume(@CheckArgument("checkReservedChannel") int channel, int leftvol, int rightvol) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioChangeChannelVolume channel=%d, leftvol=0x%X, rightvol=0x%X", channel, leftvol, rightvol));
    	}

    	return changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
    }

    @HLEFunction(nid = 0x01562BA3, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2Reserve(int sampleCount) {
        if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioOutput2Reserve sampleCount=%d", sampleCount));
        }

        return hleAudioSRCChReserve(sampleCount, 44100, SoundChannel.FORMAT_STEREO);
    }

    @HLEFunction(nid = 0x43196845, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2Release() {
    	if (log.isDebugEnabled()) {
    		log.debug("sceAudioOutput2Release redirecting to sceAudioSRCChRelease");
    	}
        return sceAudioSRCChRelease();
    }

    @HLEFunction(nid = 0x2D53F36E, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2OutputBlocking(int vol, @CanBeNull TPointer buf) {
    	if (log.isDebugEnabled()) {
    		log.debug("sceAudioOutput2OutputBlocking redirecting to sceAudioSRCOutputBlocking");
    	}
        return sceAudioSRCOutputBlocking(vol, buf);
    }

    @HLEFunction(nid = 0x647CEF33, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2GetRestSample() {
    	if (log.isDebugEnabled()) {
    		log.debug("sceAudioOutput2GetRestSample");
    	}
        return hleAudioGetChannelRestLen(pspSRCChannel);
    }

    @HLEFunction(nid = 0x63F2889C, version = 150, checkInsideInterrupt = true)
    public int sceAudioOutput2ChangeLength(int sampleCount) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioOutput2ChangeLength sampleCount=%d", sampleCount));
    	}

    	if (!pspSRCChannel.isReserved()) {
        	return -1;
        }

        pspSRCChannel.setSampleLength(sampleCount);

        return 0;
    }

    @HLEFunction(nid = 0x38553111, version = 150, checkInsideInterrupt = true)
    public int sceAudioSRCChReserve(int sampleCount, int freq, int format) {
        if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioSRCChReserve sampleCount=%d, freq=%d, format=%d", sampleCount, freq, format));
        }

        return hleAudioSRCChReserve(sampleCount, freq, format);
    }

    @HLEFunction(nid = 0x5C37C0AE, version = 150, checkInsideInterrupt = true)
    public int sceAudioSRCChRelease() {
    	if (log.isDebugEnabled()) {
    		log.debug("sceAudioSRCChRelease");
    	}

    	if (pspSRCChannel.isReserved()) {
            pspSRCChannel.release();
            pspSRCChannel.setReserved(false);
        }

        return 0;
    }

    @HLEFunction(nid = 0xE0727056, version = 150, checkInsideInterrupt = true)
    public int sceAudioSRCOutputBlocking(int vol, @CanBeNull TPointer buf) {
        if (buf.isNull()) {
            // Tested on PSP:
            // SRC audio also delays when buf == 0, in order to drain all
            // audio samples from the audio driver.
            if (!pspSRCChannel.isDrained()) {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioSRCOutputBlocking[buf==0] blocking " + pspSRCChannel);
                }
                blockThreadOutput(pspSRCChannel, buf.getAddress(), vol, vol);
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
                changeChannelVolume(pspSRCChannel, vol, vol);
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
                return doAudioOutput(pspSRCChannel, buf.getAddress());
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("sceAudioSRCOutputBlocking[blocking] %s to %s", buf, pspSRCChannel.toString()));
            }
            blockThreadOutput(pspSRCChannel, buf.getAddress(), vol, vol);
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x086E5895, version = 150, checkInsideInterrupt = true)
    public int sceAudioInputBlocking() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D4BEC68, version = 150, checkInsideInterrupt = true)
    public int sceAudioInput() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA708C6A6, version = 150, checkInsideInterrupt = true)
    public int sceAudioGetInputLength() {
        return 0;
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
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceAudioGetChannelRestLen channel=%d", channel));
    	}

    	return hleAudioGetChannelRestLen(pspPCMChannels[channel]);
    }

}