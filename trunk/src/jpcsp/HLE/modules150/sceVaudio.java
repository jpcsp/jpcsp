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

import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.sound.SoundChannel;

import org.apache.log4j.Logger;

@HLELogging
public class sceVaudio extends HLEModule {
    public static Logger log = Modules.getLogger("sceVaudio");

    @Override
    public String getName() {
        return "sceVaudio";
    }

    @Override
	public void start() {
		SoundChannel.init();

		// The PSP is using the same channel as the SRC channel(s)
        pspVaudio1Channel = Modules.sceAudioModule.pspSRC1Channel;
        pspVaudio2Channel = Modules.sceAudioModule.pspSRC2Channel;
        pspVaudioChannelReserved = false;

        super.start();
	}

    protected static final int PSP_VAUDIO_VOLUME_BASE = 0x8000;
    protected static final int PSP_VAUDIO_SAMPLE_MIN = 256;
    protected static final int PSP_VAUDIO_SAMPLE_MAX = 2048;
    protected static final int PSP_VAUDIO_FORMAT_MONO = 0x0;
    protected static final int PSP_VAUDIO_FORMAT_STEREO = 0x2;

    protected static final int PSP_VAUDIO_EFFECT_TYPE_NONE = 0;
    protected static final int PSP_VAUDIO_EFFECT_TYPE_1 = 1;
    protected static final int PSP_VAUDIO_EFFECT_TYPE_2 = 2;
    protected static final int PSP_VAUDIO_EFFECT_TYPE_3 = 3;
    protected static final int PSP_VAUDIO_EFFECT_TYPE_4 = 4;

    protected static final int PSP_VAUDIO_ALC_MODE_NONE = 0;
    protected static final int PSP_VAUDIO_ALC_MODE_1 = 1;

    protected SoundChannel pspVaudio1Channel;
    protected SoundChannel pspVaudio2Channel;
    protected boolean pspVaudioChannelReserved;

    public int checkSampleCount(int sampleCount) {
    	if (sampleCount < 256 || sampleCount > 2048) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Invalid sampleCount 0x%X", sampleCount));
    		}
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_SIZE);
    	}

    	return sampleCount;
    }

    public int checkFrequency(int frequency) {
    	switch (frequency) {
	    	case 0:
	    	case 8000:
	    	case 11025:
	    	case 12000:
	    	case 16000:
	    	case 22050:
	    	case 24000:
	    	case 32000:
	    	case 44100:
	    	case 48000:
	    		// OK
	    		break;
    		default:
    			// PSP is yielding in this error code
    	        Modules.ThreadManForUserModule.hleYieldCurrentThread();
        		throw new SceKernelErrorException(SceKernelErrors.ERROR_AUDIO_INVALID_FREQUENCY);
    	}

    	return frequency;
    }

    public int checkChannelCount(int channelCount) {
    	if (channelCount != 2 && channelCount != 4) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_FORMAT);
    	}

    	return channelCount;
    }

    protected int doAudioOutput(SoundChannel channel, int pvoid_buf) {
    	return sceAudio.doAudioOutput(channel, pvoid_buf);
    }

    protected void blockThreadOutput(SoundChannel channel, int addr, int leftVolume, int rightVolume) {
    	sceAudio.blockThreadOutput(channel, addr, leftVolume, rightVolume);
    }

    protected int changeChannelVolume(SoundChannel channel, int leftvol, int rightvol) {
    	return sceAudio.changeChannelVolume(channel, leftvol, rightvol);
    }

    @HLEFunction(nid = 0x67585DFD, version = 150, checkInsideInterrupt = true)
    public int sceVaudioChRelease() {
        if (!pspVaudio1Channel.isReserved()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }

        pspVaudioChannelReserved = false;
        pspVaudio1Channel.release();
        pspVaudio1Channel.setReserved(false);
        pspVaudio2Channel.release();
        pspVaudio2Channel.setReserved(false);

        return 0;
    }

    @HLEFunction(nid = 0x03B6807D, version = 150, checkInsideInterrupt = true)
    public int sceVaudioChReserve(@CheckArgument("checkSampleCount") int sampleCount, @CheckArgument("checkFrequency") int freq, @CheckArgument("checkChannelCount") int format) {
    	// Returning a different error code if the channel has been reserved by sceVaudioChReserve or by sceAudioSRCChReserve
    	if (pspVaudioChannelReserved) {
        	return SceKernelErrors.ERROR_BUSY;
    	}
        if (pspVaudio1Channel.isReserved()) {
        	// PSP is yielding in this error case
            Modules.ThreadManForUserModule.hleYieldCurrentThread();
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_ALREADY_RESERVED;
        }

        pspVaudioChannelReserved = true;
        pspVaudio1Channel.setReserved(true);
        pspVaudio1Channel.setSampleLength(sampleCount);
        pspVaudio1Channel.setSampleRate(freq);
        pspVaudio1Channel.setFormat(format == PSP_VAUDIO_FORMAT_MONO ? sceAudio.PSP_AUDIO_FORMAT_MONO : sceAudio.PSP_AUDIO_FORMAT_STEREO);

        pspVaudio2Channel.setReserved(true);
        pspVaudio2Channel.setSampleLength(sampleCount);
        pspVaudio2Channel.setSampleRate(freq);
        pspVaudio2Channel.setFormat(format == PSP_VAUDIO_FORMAT_MONO ? sceAudio.PSP_AUDIO_FORMAT_MONO : sceAudio.PSP_AUDIO_FORMAT_STEREO);

        Modules.ThreadManForUserModule.hleYieldCurrentThread();

        return 0;
    }

    @HLEFunction(nid = 0x8986295E, version = 150, checkInsideInterrupt = true)
    public int sceVaudioOutputBlocking(int vol, TPointer buf) {
    	int result = 0;

    	SoundChannel pspVaudioChannel = Modules.sceAudioModule.getFreeSRCChannel();
    	if (!pspVaudioChannel.isOutputBlocking()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceVaudioOutputBlocking[not blocking] %s", pspVaudioChannel));
            }
            if ((vol & PSP_VAUDIO_VOLUME_BASE) != PSP_VAUDIO_VOLUME_BASE) {
                changeChannelVolume(pspVaudioChannel, vol, vol);
            }
            result = doAudioOutput(pspVaudioChannel, buf.getAddress());
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceVaudioOutputBlocking[not blocking] returning %d (%s)", result, pspVaudioChannel));
            }
            Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceVaudioOutputBlocking[blocking] %s", pspVaudioChannel));
            }
            blockThreadOutput(pspVaudioChannel, buf.getAddress(), vol, vol);
        }

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x346FBE94, version = 150, checkInsideInterrupt = true)
    public int sceVaudioSetEffectType(int type, int vol) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCBD4AC51, version = 150, checkInsideInterrupt = true)
    public int sceVaudioSetAlcMode(int alcMode) {
    	return 0;
    }
}