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

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.hardware.Audio;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.sound.AudioBlockingOutputAction;
import jpcsp.sound.SoundChannel;

import org.apache.log4j.Logger;

public class sceVaudio implements HLEModule, HLEStartModule {

    protected static Logger log = Modules.getLogger("sceVaudio");

    @Override
    public String getName() {
        return "sceVaudio";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x67585DFD, sceVaudioChReleaseFunction);
            mm.addFunction(0x03B6807D, sceVaudioChReserveFunction);
            mm.addFunction(0x8986295E, sceVaudioOutputBlockingFunction);
            mm.addFunction(0x346FBE94, sceVaudioSetEffectTypeFunction);
            mm.addFunction(0xCBD4AC51, sceVaudioSetAlcModeFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceVaudioChReleaseFunction);
            mm.removeFunction(sceVaudioChReserveFunction);
            mm.removeFunction(sceVaudioOutputBlockingFunction);
            mm.removeFunction(sceVaudioSetEffectTypeFunction);
            mm.removeFunction(sceVaudioSetAlcModeFunction);

        }
    }

    @Override
	public void start() {
		SoundChannel.init();
        pspVaudioChannel = new SoundChannel(9); // Use channel 9 for virtual audio.
	}

	@Override
	public void stop() {
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
    	int delayMicros = channel.getUnblockOutputDelayMicros();
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

    public void sceVaudioChRelease(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("PARTIAL: sceVaudioChRelease");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (pspVaudioChannel.isReserved()) {
            pspVaudioChannel.release();
            pspVaudioChannel.setReserved(false);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_CHANNEL_NOT_RESERVED;
        }
    }

    public void sceVaudioChReserve(Processor processor) {
        CpuState cpu = processor.cpu;

        int samplecount = cpu.gpr[4];
        int freq = cpu.gpr[5];
        int format = cpu.gpr[6];

        log.warn("PARTIAL: sceVaudioChReserve: samplecount=" + samplecount + ", freq=" + freq + ", format=" + format);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!pspVaudioChannel.isReserved()) {
            pspVaudioChannel.setReserved(true);
            pspVaudioChannel.setSampleLength(samplecount);
            pspVaudioChannel.setSampleRate(freq);
            pspVaudioChannel.setFormat(format);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceVaudioOutputBlocking(Processor processor) {
        CpuState cpu = processor.cpu;

        int vol = cpu.gpr[4];
        int buf = cpu.gpr[5];

        log.warn("PARTIAL: sceVaudioOutputBlocking: vol=0x" + Integer.toHexString(vol)
                    + ", buf=0x" + Integer.toHexString(buf));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!Memory.isAddressGood(buf)) {
            log.warn("sceVaudioOutputBlocking bad pointer " + String.format("0x%08X", buf));
            cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
        } else {
            if (!pspVaudioChannel.isOutputBlocking()) {
                if (log.isDebugEnabled()) {
                    log.debug("sceVaudioOutputBlocking[not blocking] " + pspVaudioChannel.toString());
                }
                if((vol & PSP_VAUDIO_VOLUME_BASE) != PSP_VAUDIO_VOLUME_BASE) {
                    changeChannelVolume(pspVaudioChannel, vol, vol);
                }
                cpu.gpr[2] = doAudioOutput(pspVaudioChannel, buf);
                if (log.isDebugEnabled()) {
                    log.debug("sceVaudioOutputBlocking[not blocking] returning " + cpu.gpr[2] + " (" + pspVaudioChannel.toString() + ")");
                }
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("sceVaudioOutputBlocking[blocking] " + pspVaudioChannel.toString());
                }
                blockThreadOutput(pspVaudioChannel, buf, vol, vol);
            }
        }
    }

    public void sceVaudioSetEffectType(Processor processor) {
        CpuState cpu = processor.cpu;

        int type = cpu.gpr[4];
        int vol = cpu.gpr[5];

        log.warn("UNIMPLEMENTED: sceVaudioSetEffectType: type=" + type
                    + ", vol=0x" + Integer.toHexString(vol));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceVaudioSetAlcMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int alcMode = cpu.gpr[4];

        log.warn("UNIMPLEMENTED: sceVaudioSetAlcMode: alcMode=" + alcMode);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction sceVaudioChReleaseFunction = new HLEModuleFunction("sceVaudio", "sceVaudioChRelease") {

        @Override
        public final void execute(Processor processor) {
            sceVaudioChRelease(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceVaudioModule.sceVaudioChRelease(processor);";
        }
    };

    public final HLEModuleFunction sceVaudioChReserveFunction = new HLEModuleFunction("sceVaudio", "sceVaudioChReserve") {

        @Override
        public final void execute(Processor processor) {
            sceVaudioChReserve(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceVaudioModule.sceVaudioChReserve(processor);";
        }
    };

    public final HLEModuleFunction sceVaudioOutputBlockingFunction = new HLEModuleFunction("sceVaudio", "sceVaudioOutputBlocking") {

        @Override
        public final void execute(Processor processor) {
            sceVaudioOutputBlocking(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceVaudioModule.sceVaudioOutputBlocking(processor);";
        }
    };

    public final HLEModuleFunction sceVaudioSetEffectTypeFunction = new HLEModuleFunction("sceVaudio", "sceVaudioSetEffectType") {

        @Override
        public final void execute(Processor processor) {
            sceVaudioSetEffectType(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceVaudioModule.sceVaudioSetEffectType(processor);";
        }
    };

    public final HLEModuleFunction sceVaudioSetAlcModeFunction = new HLEModuleFunction("sceVaudio", "sceVaudioSetAlcMode") {

        @Override
        public final void execute(Processor processor) {
            sceVaudioSetAlcMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceVaudioModule.sceVaudioSetAlcMode(processor);";
        }
    };
}