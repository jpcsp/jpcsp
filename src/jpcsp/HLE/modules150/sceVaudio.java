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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Audio;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.sound.AudioBlockingOutputAction;
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
        pspVaudioChannel = new SoundChannel(9); // Use channel 9 for virtual audio.

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

    protected SoundChannel pspVaudioChannel;


    protected int doAudioOutput(SoundChannel channel, int pvoid_buf) {
        int ret = -1;

        if (channel.isReserved()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("doAudioOuput(%s, 0x%08X)", channel.toString(), pvoid_buf));
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

    protected int changeChannelVolume(SoundChannel channel, int leftvol, int rightvol) {
        int ret = -1;
        if (channel.isReserved()) {
            channel.setLeftVolume(leftvol);
            channel.setRightVolume(rightvol);
            ret = 0;
        }
        return ret;
    }

    @HLEFunction(nid = 0x67585DFD, version = 150, checkInsideInterrupt = true)
    public int sceVaudioChRelease() {
        if (!pspVaudioChannel.isReserved()) {
        	return SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }

        pspVaudioChannel.release();
        pspVaudioChannel.setReserved(false);

        return 0;
    }

    @HLEFunction(nid = 0x03B6807D, version = 150, checkInsideInterrupt = true)
    public int sceVaudioChReserve(int sampleCount, int freq, int format) {
        if (pspVaudioChannel.isReserved()) {
        	return -1;
        }

        pspVaudioChannel.setReserved(true);
        pspVaudioChannel.setSampleLength(sampleCount);
        pspVaudioChannel.setSampleRate(freq);
        pspVaudioChannel.setFormat(format);

        return 0;
    }

    @HLEFunction(nid = 0x8986295E, version = 150, checkInsideInterrupt = true)
    public int sceVaudioOutputBlocking(int vol, TPointer buf) {
    	int result = 0;

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