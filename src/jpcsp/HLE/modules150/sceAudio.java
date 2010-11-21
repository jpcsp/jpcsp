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
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
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

public class sceAudio implements HLEModule, HLEStartModule {
    private static Logger log = Modules.getLogger("sceAudio");

    @Override
    public String getName() {
        return "sceAudio";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x8C1009B2, sceAudioOutputFunction);
            mm.addFunction(0x136CAF51, sceAudioOutputBlockingFunction);
            mm.addFunction(0xE2D56B2D, sceAudioOutputPannedFunction);
            mm.addFunction(0x13F592BC, sceAudioOutputPannedBlockingFunction);
            mm.addFunction(0x5EC81C55, sceAudioChReserveFunction);
            mm.addFunction(0x41EFADE7, sceAudioOneshotOutputFunction);
            mm.addFunction(0x6FC46853, sceAudioChReleaseFunction);
            mm.addFunction(0xB011922F, sceAudioGetChannelRestLengthFunction);
            mm.addFunction(0xCB2E439E, sceAudioSetChannelDataLenFunction);
            mm.addFunction(0x95FD0C2D, sceAudioChangeChannelConfigFunction);
            mm.addFunction(0xB7E1D8E7, sceAudioChangeChannelVolumeFunction);
            mm.addFunction(0x01562BA3, sceAudioOutput2ReserveFunction);
            mm.addFunction(0x43196845, sceAudioOutput2ReleaseFunction);
            mm.addFunction(0x2D53F36E, sceAudioOutput2OutputBlockingFunction);
            mm.addFunction(0x647CEF33, sceAudioOutput2GetRestSampleFunction);
            mm.addFunction(0x63F2889C, sceAudioOutput2ChangeLengthFunction);
            mm.addFunction(0x38553111, sceAudioSRCChReserveFunction);
            mm.addFunction(0x5C37C0AE, sceAudioSRCChReleaseFunction);
            mm.addFunction(0xE0727056, sceAudioSRCOutputBlockingFunction);
            mm.addFunction(0x086E5895, sceAudioInputBlockingFunction);
            mm.addFunction(0x6D4BEC68, sceAudioInputFunction);
            mm.addFunction(0xA708C6A6, sceAudioGetInputLengthFunction);
            mm.addFunction(0x87B2E651, sceAudioWaitInputEndFunction);
            mm.addFunction(0x7DE61688, sceAudioInputInitFunction);
            mm.addFunction(0xE926D3FB, sceAudioInputInitExFunction);
            mm.addFunction(0xA633048E, sceAudioPollInputEndFunction);
            mm.addFunction(0xE9D97901, sceAudioGetChannelRestLenFunction);
            mm.addFunction(0x80F1F7E0, sceAudioInitFunction);
            mm.addFunction(0x210567F7, sceAudioEndFunction);
            mm.addFunction(0xA2BEAA6C, sceAudioSetFrequencyFunction);
            mm.addFunction(0xB61595C0, sceAudioLoopbackTestFunction);
            mm.addFunction(0x927AC32B, sceAudioSetVolumeOffsetFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

        	mm.removeFunction(sceAudioOutputFunction);
            mm.removeFunction(sceAudioOutputBlockingFunction);
            mm.removeFunction(sceAudioOutputPannedFunction);
            mm.removeFunction(sceAudioOutputPannedBlockingFunction);
            mm.removeFunction(sceAudioChReserveFunction);
            mm.removeFunction(sceAudioOneshotOutputFunction);
            mm.removeFunction(sceAudioChReleaseFunction);
            mm.removeFunction(sceAudioGetChannelRestLengthFunction);
            mm.removeFunction(sceAudioSetChannelDataLenFunction);
            mm.removeFunction(sceAudioChangeChannelConfigFunction);
            mm.removeFunction(sceAudioChangeChannelVolumeFunction);
            mm.removeFunction(sceAudioOutput2ReserveFunction);
            mm.removeFunction(sceAudioOutput2ReleaseFunction);
            mm.removeFunction(sceAudioOutput2OutputBlockingFunction);
            mm.removeFunction(sceAudioOutput2GetRestSampleFunction);
            mm.removeFunction(sceAudioOutput2ChangeLengthFunction);
            mm.removeFunction(sceAudioSRCChReserveFunction);
            mm.removeFunction(sceAudioSRCChReleaseFunction);
            mm.removeFunction(sceAudioSRCOutputBlockingFunction);
            mm.removeFunction(sceAudioInputBlockingFunction);
            mm.removeFunction(sceAudioInputFunction);
            mm.removeFunction(sceAudioGetInputLengthFunction);
            mm.removeFunction(sceAudioWaitInputEndFunction);
            mm.removeFunction(sceAudioInputInitFunction);
            mm.removeFunction(sceAudioInputInitExFunction);
            mm.removeFunction(sceAudioPollInputEndFunction);
            mm.removeFunction(sceAudioGetChannelRestLenFunction);
            mm.removeFunction(sceAudioInitFunction);
            mm.removeFunction(sceAudioEndFunction);
            mm.removeFunction(sceAudioSetFrequencyFunction);
            mm.removeFunction(sceAudioLoopbackTestFunction);
            mm.removeFunction(sceAudioSetVolumeOffsetFunction);
        }
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
	}

	@Override
	public void stop() {
	}

	protected static final int PSP_AUDIO_VOLUME_MAX = 0x8000;
    protected static final int PSP_AUDIO_CHANNEL_MAX = 8;
    protected static final int PSP_AUDIO_SAMPLE_MIN = 64;
    protected static final int PSP_AUDIO_SAMPLE_MAX = 65472;
    protected static final int PSP_AUDIO_FORMAT_STEREO = 0;
    protected static final int PSP_AUDIO_FORMAT_MONO = 0x10;

    protected SoundChannel[] pspPCMChannels;
    protected SoundChannel pspSRCChannel;

    protected boolean isAudioOutput2 = false;

    protected boolean disableChReserve;
    protected boolean disableBlockingAudio;

    public void setChReserveEnabled(boolean enabled) {
        disableChReserve = !enabled;
        log.info("Audio ChReserve disabled: " + disableChReserve);
    }

    public void setBlockingEnabled(boolean enabled) {
        disableBlockingAudio = !enabled;
        log.info("Audio Blocking disabled: " + disableBlockingAudio);
    }

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

    public void hleAudioBlockingOutput(int threadId, SoundChannel channel, int addr, int leftVolume, int rightVolume) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleAudioBlockingOutput %s", channel.toString()));
    	}

    	if (addr == 0 || !channel.isOutputBlocking()) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo thread = threadMan.getThreadById(threadId);
            if (thread != null) {
                if (addr != 0) {
                    changeChannelVolume(channel, leftVolume, rightVolume);
                    int ret = doAudioOutput(channel, addr);
                    thread.cpuContext.gpr[2] = ret;
                }
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

    public void sceAudioInit(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioInit [0x80F1F7E0]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioEnd [0x210567F7]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioSetFrequency(Processor processor) {
        CpuState cpu = processor.cpu;

        int frequency = cpu.gpr[4];

        if (frequency == 44100 || frequency == 48000) {
        	for (int i = 0; i < pspPCMChannels.length; i++) {
        		pspPCMChannels[i].setSampleRate(frequency);
        	}
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceAudioLoopbackTest(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioLoopbackTest [0xB61595C0]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioSetVolumeOffset(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioSetVolumeOffset [0x927AC32B]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioOutput(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int vol = cpu.gpr[5];
        int pvoid_buf = cpu.gpr[6];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!Memory.isAddressGood(pvoid_buf)) {
            log.warn("sceAudioOutput bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
        } else {
            if (!pspPCMChannels[channel].isOutputBlocking()) {
                changeChannelVolume(pspPCMChannels[channel], vol, vol);
                cpu.gpr[2] = doAudioOutput(pspPCMChannels[channel], pvoid_buf);
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
            }
        }
    }

    public void sceAudioOutputBlocking(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int vol = cpu.gpr[5];
        int pvoid_buf = cpu.gpr[6];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (pvoid_buf == 0) {
            if (hleAudioGetChannelRestLen(pspPCMChannels[channel]) != 0) {
                log.warn("sceAudioOutputBlocking (pvoid_buf==0): delaying current thread");
                Modules.ThreadManForUserModule.hleKernelDelayThread(100000, false);
            } else {
                log.warn("sceAudioOutputBlocking (pvoid_buf==0): not delaying current thread");
                cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
            }
        } else if (!Memory.isAddressGood(pvoid_buf)) {
            log.warn("sceAudioOutputBlocking bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
        } else {
            if (!pspPCMChannels[channel].isOutputBlocking() || disableBlockingAudio) {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioOutputBlocking[not blocking] " + pspPCMChannels[channel].toString());
                }
                changeChannelVolume(pspPCMChannels[channel], vol, vol);
                cpu.gpr[2] = doAudioOutput(pspPCMChannels[channel], pvoid_buf);
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioOutputBlocking[not blocking] returning " + cpu.gpr[2] + " (" + pspPCMChannels[channel].toString() + ")");
                }
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioOutputBlocking[blocking] " + pspPCMChannels[channel].toString());
                }
                blockThreadOutput(pspPCMChannels[channel], pvoid_buf, vol, vol);
            }
        }
    }

    public void sceAudioOutputPanned(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int leftvol = cpu.gpr[5];
        int rightvol = cpu.gpr[6];
        int pvoid_buf = cpu.gpr[7];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!Memory.isAddressGood(pvoid_buf)) {
            log.warn("sceAudioOutputPanned bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
        } else {
            if (!pspPCMChannels[channel].isOutputBlocking()) {
                changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
                cpu.gpr[2] = doAudioOutput(pspPCMChannels[channel], pvoid_buf);
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
            }
        }
    }

    public void sceAudioOutputPannedBlocking(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int leftvol = cpu.gpr[5];
        int rightvol = cpu.gpr[6];
        int pvoid_buf = cpu.gpr[7];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (pvoid_buf == 0) {
            // Tested on PSP:
            // An output adress of 0 is actually a special code for the PSP.
            // It means that we must stall processing until all the previous unplayed samples' data
            // is output. The safest way to mimic this, is to delay the current thread (which will be
            // an audio processing one) if there are still samples to play.
            if (hleAudioGetChannelRestLen(pspPCMChannels[channel]) != 0) {
                log.warn("sceAudioOutputPannedBlocking (pvoid_buf==0): delaying current thread");
                Modules.ThreadManForUserModule.hleKernelDelayThread(100000, false);
            } else {
                log.warn("sceAudioOutputPannedBlocking (pvoid_buf==0): not delaying current thread");
                cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
            }
        } else if (!Memory.isAddressGood(pvoid_buf)) {
            log.warn("sceAudioOutputPannedBlocking bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
        } else {
            if (!pspPCMChannels[channel].isOutputBlocking() || disableBlockingAudio) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceAudioOutputPannedBlocking[not blocking] leftVol=%d, rightVol=%d, channel=%s", leftvol, rightvol, pspPCMChannels[channel].toString()));
                }
                changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
                cpu.gpr[2] = doAudioOutput(pspPCMChannels[channel], pvoid_buf);
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceAudioOutputPannedBlocking[blocking] leftVol=%d, rightVol=%d, channel=%s", leftvol, rightvol, pspPCMChannels[channel].toString()));
                }
                blockThreadOutput(pspPCMChannels[channel], pvoid_buf, leftvol, rightvol);
            }
        }
    }

    public void sceAudioChReserve(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int samplecount = cpu.gpr[5];
        int format = cpu.gpr[6];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (disableChReserve) {
            log.warn("IGNORED sceAudioChReserve channel= " + channel + " samplecount = " + samplecount + " format = " + format);
            cpu.gpr[2] = -1;
        } else {
            log.debug("sceAudioChReserve channel= " + channel + " samplecount = " + samplecount + " format = " + format);

            if (channel != -1) {
                if (pspPCMChannels[channel].isReserved()) {
                    log.warn("sceAudioChReserve failed - channel " + channel + " already in use");
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
                pspPCMChannels[channel].setSampleLength(samplecount);
                pspPCMChannels[channel].setFormat(format);
            }

            cpu.gpr[2] = channel;
        }
    }

    public void sceAudioOneshotOutput(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioOneshotOutput [0x41EFADE7]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioChRelease(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (pspPCMChannels[channel].isReserved()) {
            pspPCMChannels[channel].release();
            pspPCMChannels[channel].setReserved(false);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceAudioGetChannelRestLength(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = hleAudioGetChannelRestLen(pspPCMChannels[channel]);
    }

    public void sceAudioSetChannelDataLen(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int samplecount = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAudioSetChannelDataLen channel=%d, sampleCount=%d", channel, samplecount));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        pspPCMChannels[channel].setSampleLength(samplecount);
        cpu.gpr[2] = 0;
    }

    public void sceAudioChangeChannelConfig(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int format = cpu.gpr[5];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        pspPCMChannels[channel].setFormat(format);
        cpu.gpr[2] = 0;
    }

    public void sceAudioChangeChannelVolume(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int leftvol = cpu.gpr[5];
        int rightvol = cpu.gpr[6];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = changeChannelVolume(pspPCMChannels[channel], leftvol, rightvol);
    }

    public void sceAudioOutput2Reserve(Processor processor) {
        isAudioOutput2 = true;
        sceAudioSRCChReserve(processor);
    }

    public void sceAudioOutput2Release(Processor processor) {
        sceAudioSRCChRelease(processor);
    }

    public void sceAudioOutput2OutputBlocking(Processor processor) {
        sceAudioSRCOutputBlocking(processor);
    }

    public void sceAudioOutput2GetRestSample(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = hleAudioGetChannelRestLen(pspSRCChannel);
    }

    public void sceAudioOutput2ChangeLength(Processor processor) {
        CpuState cpu = processor.cpu;

        int samplecount = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (pspSRCChannel.isReserved()) {
            pspSRCChannel.setSampleLength(samplecount);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceAudioSRCChReserve(Processor processor) {
        CpuState cpu = processor.cpu;

        int samplecount = cpu.gpr[4];
        int freq = cpu.gpr[5];
        int format = cpu.gpr[6];

        if (log.isDebugEnabled()) {
        	if (isAudioOutput2) {
        		log.debug(String.format("sceAudioOutput2Reserve sampleCount=%d", samplecount));
        	} else {
        		log.debug(String.format("sceAudioSRCChReserve sampleCount=%d, freq=%d, format=%d", samplecount, freq, format));
        	}
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isAudioOutput2) {
            freq = 44100;
            format = 0;
            isAudioOutput2 = false;
        }
        if (disableChReserve) {
            log.warn("IGNORED sceAudioSRCChReserve samplecount= " + samplecount + " freq= " + freq + " format=" + format);
            cpu.gpr[2] = -1;
        } else {
            if (!pspSRCChannel.isReserved()) {
            	pspSRCChannel.setSampleRate(freq);
                pspSRCChannel.setReserved(true);
                pspSRCChannel.setSampleLength(samplecount);
                pspSRCChannel.setFormat(format);
            }
        }
        cpu.gpr[2] = 0;
    }

    public void sceAudioSRCChRelease(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (pspSRCChannel.isReserved()) {
            pspSRCChannel.release();
            pspSRCChannel.setReserved(false);
        }

        cpu.gpr[2] = 0;
    }

    public void sceAudioSRCOutputBlocking(Processor processor) {
        CpuState cpu = processor.cpu;

        int vol = cpu.gpr[4];
        int buf = cpu.gpr[5];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (buf == 0) {
            // Tested on PSP:
            // SRC audio also delays when buf == 0, in order to drain all
            // audio samples from the audio driver.
            if (hleAudioGetChannelRestLen(pspSRCChannel) != 0) {
                log.warn("sceAudioSRCOutputBlocking (buf==0): delaying current thread");
                Modules.ThreadManForUserModule.hleKernelDelayThread(100000, false);
            } else {
                log.warn("sceAudioSRCOutputBlocking (buf==0): not delaying current thread");
                cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
            }
        } else if (!Memory.isAddressGood(buf)) {
            log.warn("sceAudioSRCOutputBlocking bad pointer " + String.format("0x%08X", buf));
            cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_PRIV_REQUIRED;
        } else {
            if (!pspSRCChannel.isOutputBlocking() || disableBlockingAudio) {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioSRCOutputBlocking[not blocking] " + pspSRCChannel.toString());
                }
                changeChannelVolume(pspSRCChannel, vol, vol);
                cpu.gpr[2] = doAudioOutput(pspSRCChannel, buf);
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("sceAudioSRCOutputBlocking[blocking] " + pspSRCChannel.toString());
                }
                blockThreadOutput(pspSRCChannel, buf, vol, vol);
            }
        }
    }

    public void sceAudioInputBlocking(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioInputBlocking [0x086E5895]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioInput(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioInput [0x6D4BEC68]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioGetInputLength(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioGetInputLength [0xA708C6A6]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioWaitInputEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioWaitInputEnd [0x87B2E651]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioInputInit(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioInputInit [0x7DE61688]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioInputInitEx(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioInputInitEx [0xE926D3FB]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioPollInputEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceAudioPollInputEnd [0xA633048E]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceAudioGetChannelRestLen(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = hleAudioGetChannelRestLen(pspPCMChannels[channel]);
    }
    public final HLEModuleFunction sceAudioInitFunction = new HLEModuleFunction("sceAudio_driver", "sceAudioInit") {

        @Override
        public final void execute(Processor processor) {
            sceAudioInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudio_driverModule.sceAudioInit(processor);";
        }
    };
    public final HLEModuleFunction sceAudioEndFunction = new HLEModuleFunction("sceAudio_driver", "sceAudioEnd") {

        @Override
        public final void execute(Processor processor) {
            sceAudioEnd(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudio_driverModule.sceAudioEnd(processor);";
        }
    };
    public final HLEModuleFunction sceAudioSetFrequencyFunction = new HLEModuleFunction("sceAudio_driver", "sceAudioSetFrequency") {

        @Override
        public final void execute(Processor processor) {
            sceAudioSetFrequency(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudio_driverModule.sceAudioSetFrequency(processor);";
        }
    };
    public final HLEModuleFunction sceAudioLoopbackTestFunction = new HLEModuleFunction("sceAudio_driver", "sceAudioLoopbackTest") {

        @Override
        public final void execute(Processor processor) {
            sceAudioLoopbackTest(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudio_driverModule.sceAudioLoopbackTest(processor);";
        }
    };
    public final HLEModuleFunction sceAudioSetVolumeOffsetFunction = new HLEModuleFunction("sceAudio_driver", "sceAudioSetVolumeOffset") {

        @Override
        public final void execute(Processor processor) {
            sceAudioSetVolumeOffset(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudio_driverModule.sceAudioSetVolumeOffset(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutputFunction = new HLEModuleFunction("sceAudio", "sceAudioOutput") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutput(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutput(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutputBlockingFunction = new HLEModuleFunction("sceAudio", "sceAudioOutputBlocking") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutputBlocking(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutputBlocking(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutputPannedFunction = new HLEModuleFunction("sceAudio", "sceAudioOutputPanned") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutputPanned(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutputPanned(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutputPannedBlockingFunction = new HLEModuleFunction("sceAudio", "sceAudioOutputPannedBlocking") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutputPannedBlocking(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutputPannedBlocking(processor);";
        }
    };
    public final HLEModuleFunction sceAudioChReserveFunction = new HLEModuleFunction("sceAudio", "sceAudioChReserve") {

        @Override
        public final void execute(Processor processor) {
            sceAudioChReserve(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioChReserve(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOneshotOutputFunction = new HLEModuleFunction("sceAudio", "sceAudioOneshotOutput") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOneshotOutput(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOneshotOutput(processor);";
        }
    };
    public final HLEModuleFunction sceAudioChReleaseFunction = new HLEModuleFunction("sceAudio", "sceAudioChRelease") {

        @Override
        public final void execute(Processor processor) {
            sceAudioChRelease(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioChRelease(processor);";
        }
    };
    public final HLEModuleFunction sceAudioGetChannelRestLengthFunction = new HLEModuleFunction("sceAudio", "sceAudioGetChannelRestLength") {

        @Override
        public final void execute(Processor processor) {
            sceAudioGetChannelRestLength(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioGetChannelRestLength(processor);";
        }
    };
    public final HLEModuleFunction sceAudioSetChannelDataLenFunction = new HLEModuleFunction("sceAudio", "sceAudioSetChannelDataLen") {

        @Override
        public final void execute(Processor processor) {
            sceAudioSetChannelDataLen(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioSetChannelDataLen(processor);";
        }
    };
    public final HLEModuleFunction sceAudioChangeChannelConfigFunction = new HLEModuleFunction("sceAudio", "sceAudioChangeChannelConfig") {

        @Override
        public final void execute(Processor processor) {
            sceAudioChangeChannelConfig(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioChangeChannelConfig(processor);";
        }
    };
    public final HLEModuleFunction sceAudioChangeChannelVolumeFunction = new HLEModuleFunction("sceAudio", "sceAudioChangeChannelVolume") {

        @Override
        public final void execute(Processor processor) {
            sceAudioChangeChannelVolume(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioChangeChannelVolume(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutput2ReserveFunction = new HLEModuleFunction("sceAudio", "sceAudioOutput2Reserve") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutput2Reserve(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutput2Reserve(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutput2ReleaseFunction = new HLEModuleFunction("sceAudio", "sceAudioOutput2Release") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutput2Release(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutput2Release(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutput2OutputBlockingFunction = new HLEModuleFunction("sceAudio", "sceAudioOutput2OutputBlocking") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutput2OutputBlocking(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutput2OutputBlocking(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutput2GetRestSampleFunction = new HLEModuleFunction("sceAudio", "sceAudioOutput2GetRestSample") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutput2GetRestSample(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutput2GetRestSample(processor);";
        }
    };
    public final HLEModuleFunction sceAudioOutput2ChangeLengthFunction = new HLEModuleFunction("sceAudio", "sceAudioOutput2ChangeLength") {

        @Override
        public final void execute(Processor processor) {
            sceAudioOutput2ChangeLength(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioOutput2ChangeLength(processor);";
        }
    };
    public final HLEModuleFunction sceAudioSRCChReserveFunction = new HLEModuleFunction("sceAudio", "sceAudioSRCChReserve") {

        @Override
        public final void execute(Processor processor) {
            sceAudioSRCChReserve(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioSRCChReserve(processor);";
        }
    };
    public final HLEModuleFunction sceAudioSRCChReleaseFunction = new HLEModuleFunction("sceAudio", "sceAudioSRCChRelease") {

        @Override
        public final void execute(Processor processor) {
            sceAudioSRCChRelease(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioSRCChRelease(processor);";
        }
    };
    public final HLEModuleFunction sceAudioSRCOutputBlockingFunction = new HLEModuleFunction("sceAudio", "sceAudioSRCOutputBlocking") {

        @Override
        public final void execute(Processor processor) {
            sceAudioSRCOutputBlocking(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioSRCOutputBlocking(processor);";
        }
    };
    public final HLEModuleFunction sceAudioInputBlockingFunction = new HLEModuleFunction("sceAudio", "sceAudioInputBlocking") {

        @Override
        public final void execute(Processor processor) {
            sceAudioInputBlocking(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioInputBlocking(processor);";
        }
    };
    public final HLEModuleFunction sceAudioInputFunction = new HLEModuleFunction("sceAudio", "sceAudioInput") {

        @Override
        public final void execute(Processor processor) {
            sceAudioInput(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioInput(processor);";
        }
    };
    public final HLEModuleFunction sceAudioGetInputLengthFunction = new HLEModuleFunction("sceAudio", "sceAudioGetInputLength") {

        @Override
        public final void execute(Processor processor) {
            sceAudioGetInputLength(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioGetInputLength(processor);";
        }
    };
    public final HLEModuleFunction sceAudioWaitInputEndFunction = new HLEModuleFunction("sceAudio", "sceAudioWaitInputEnd") {

        @Override
        public final void execute(Processor processor) {
            sceAudioWaitInputEnd(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioWaitInputEnd(processor);";
        }
    };
    public final HLEModuleFunction sceAudioInputInitFunction = new HLEModuleFunction("sceAudio", "sceAudioInputInit") {

        @Override
        public final void execute(Processor processor) {
            sceAudioInputInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioInputInit(processor);";
        }
    };
    public final HLEModuleFunction sceAudioInputInitExFunction = new HLEModuleFunction("sceAudio", "sceAudioInputInitEx") {

        @Override
        public final void execute(Processor processor) {
            sceAudioInputInitEx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioInputInitEx(processor);";
        }
    };
    public final HLEModuleFunction sceAudioPollInputEndFunction = new HLEModuleFunction("sceAudio", "sceAudioPollInputEnd") {

        @Override
        public final void execute(Processor processor) {
            sceAudioPollInputEnd(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioPollInputEnd(processor);";
        }
    };
    public final HLEModuleFunction sceAudioGetChannelRestLenFunction = new HLEModuleFunction("sceAudio", "sceAudioGetChannelRestLen") {

        @Override
        public final void execute(Processor processor) {
            sceAudioGetChannelRestLen(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceAudioModule.sceAudioGetChannelRestLen(processor);";
        }
    };
}