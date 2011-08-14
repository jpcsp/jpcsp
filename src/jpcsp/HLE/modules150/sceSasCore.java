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
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.sound.SoundVoice;
import jpcsp.sound.SoundMixer;

import org.apache.log4j.Logger;

public class sceSasCore implements HLEModule, HLEStartModule {
    protected static Logger log = Modules.getLogger("sceSasCore");

    @Override
    public String getName() {
        return "sceSasCore";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    @Override
    public void start() {
        sasCoreUid = -1;
        isSasInit = false;
        voices = new SoundVoice[32];
        for (int i = 0; i < voices.length; i++) {
            voices[i] = new SoundVoice(i);
        }
        mixer = new SoundMixer(voices);
        grainSamples = PSP_SAS_GRAIN_SAMPLES;
        outputMode = PSP_SAS_OUTPUTMODE_STEREO;
    }

    @Override
    public void stop() {
    }

    public static final int PSP_SAS_VOICES_MAX = 32;
    public static final int PSP_SAS_GRAIN_SAMPLES = 256;
    public static final int PSP_SAS_VOL_MAX = 0x1000;
    public static final int PSP_SAS_LOOP_MODE_OFF = 0;
    public static final int PSP_SAS_LOOP_MODE_ON = 1;
    public static final int PSP_SAS_PITCH_MIN = 0x1;
    public static final int PSP_SAS_PITCH_BASE = 0x1000;
    public static final int PSP_SAS_PITCH_MAX = 0x4000;
    public static final int PSP_SAS_NOISE_FREQ_MAX = 0x3F;
    public static final int PSP_SAS_ENVELOPE_HEIGHT_MAX = 0x40000000;
    public static final int PSP_SAS_ENVELOPE_FREQ_MAX = 0x7FFFFFFF;
    public static final int PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE = 0;
    public static final int PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE = 1;
    public static final int PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT = 2;
    public static final int PSP_SAS_ADSR_CURVE_MODE_EXPONENT_REV = 3;
    public static final int PSP_SAS_ADSR_CURVE_MODE_EXPONENT = 4;
    public static final int PSP_SAS_ADSR_CURVE_MODE_DIRECT = 5;
    public static final int PSP_SAS_ADSR_ATTACK = 1;
    public static final int PSP_SAS_ADSR_DECAY = 2;
    public static final int PSP_SAS_ADSR_SUSTAIN = 4;
    public static final int PSP_SAS_ADSR_RELEASE = 8;
    public static final int PSP_SAS_OUTPUTMODE_STEREO = 0;
    public static final int PSP_SAS_OUTPUTMODE_MULTICHANNEL = 1;
    public static final int PSP_SAS_EFFECT_TYPE_OFF = -1;
    public static final int PSP_SAS_EFFECT_TYPE_ROOM = 0;
    public static final int PSP_SAS_EFFECT_TYPE_UNK1 = 1;
    public static final int PSP_SAS_EFFECT_TYPE_UNK2 = 2;
    public static final int PSP_SAS_EFFECT_TYPE_UNK3 = 3;
    public static final int PSP_SAS_EFFECT_TYPE_HALL = 4;
    public static final int PSP_SAS_EFFECT_TYPE_SPACE = 5;
    public static final int PSP_SAS_EFFECT_TYPE_ECHO = 6;
    public static final int PSP_SAS_EFFECT_TYPE_DELAY = 7;
    public static final int PSP_SAS_EFFECT_TYPE_PIPE = 8;

    protected int sasCoreUid;
    protected boolean isSasInit;
    protected SoundVoice[] voices;
    protected SoundMixer mixer;
    protected int grainSamples;
    protected int outputMode;
    protected static final int waveformBufMaxSize = 1024;  // 256 sound samples.
    protected int waveformEffectType;
    protected int waveformEffectLeftVol;
    protected int waveformEffectRightVol;
    protected int waveformEffectDelay;
    protected int waveformEffectFeedback;
    protected boolean waveformEffectIsDryOn;
    protected boolean waveformEffectIsWetOn;
    protected static final int sasCoreDelay = 5000; // Average microseconds, based on PSP tests.

    protected String makeLogParams(CpuState cpu) {
        return String.format("%08x %08x %08x %08x",
                cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    /** If sasCore isn't a valid handle this function will print a log message and set $v0 to -1.
     * @return true if sasCore is good. */
    protected boolean isSasHandleGood(int sasCore, String functionName, CpuState cpu) {
        if (Memory.isAddressGood(sasCore)) {
            if (Processor.memory.read32(sasCore) == sasCoreUid) {
                return true;
            }
            log.warn(functionName + " bad sasCoreUid 0x" + Integer.toHexString(Processor.memory.read32(sasCore)));
        } else {
            log.warn(functionName + " bad sasCore Address 0x" + Integer.toHexString(sasCore));
        }
        cpu.gpr[2] = -1;

        return false;
    }

    protected boolean isVoiceNumberGood(int voice, String functionName, CpuState cpu) {
        if (voice >= 0 && voice < voices.length) {
            return true;
        }

        log.warn(functionName + " bad voice number " + voice);
        cpu.gpr[2] = SceKernelErrors.ERROR_SAS_INVALID_VOICE;
        return false;
    }

    private void delayThread(long startMicros, int delayMicros) {
    	long now = Emulator.getClock().microTime();
    	int threadDelayMicros = delayMicros - (int) (now - startMicros);
    	if (threadDelayMicros > 0) {
    		Modules.ThreadManForUserModule.hleKernelDelayThread(threadDelayMicros, false);
    	} else {
    		Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
    	}
    }

    private void delayThreadSasCore(long startMicros) {
    	// Rough estimation of the required delay, based on the grainSamples
    	int delayMicros = grainSamples * 4;
    	delayThread(startMicros, delayMicros);
    }

    public void __sceSasSetADSR(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int flag = cpu.gpr[6];   // Bitfield to set each envelope on or off.
        int attack = cpu.gpr[7];   // ADSR Envelope's attack.
        int decay = cpu.gpr[8];   // ADSR Envelope's decay.
        int sustain = cpu.gpr[9];   // ADSR Envelope's sustain.
        int release = cpu.gpr[10];  // ADSR Envelope's release.

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetADSR " + String.format("sasCore=%08x, voice=%d flag=%08x a=%08x d=%08x s=%08x r%08x",
                    sasCore, voice, flag, attack, decay, sustain, release));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetADSR", cpu) && isVoiceNumberGood(voice, "__sceSasSetADSR", cpu)) {
            if ((flag & 0x1) == 0x1) {
                voices[voice].getEnvelope().AttackRate = attack;
            }
            if ((flag & 0x2) == 0x2) {
                voices[voice].getEnvelope().DecayRate = decay;
            }
            if ((flag & 0x4) == 0x4) {
                voices[voice].getEnvelope().SustainRate = sustain;
            }
            if ((flag & 0x8) == 0x8) {
                voices[voice].getEnvelope().ReleaseRate = release;
            }
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasRevParam(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int delay = cpu.gpr[5];
        int feedback = cpu.gpr[6];

        // Set waveform effect's delay and feedback levels.
        if (log.isDebugEnabled()) {
            log.debug("__sceSasRevParam(" + String.format("sasCore=0x%08x, delay=%d, feedback=%d)", sasCore, delay, feedback));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasRevParam", cpu)) {
            waveformEffectDelay = delay;
            waveformEffectFeedback = feedback;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasGetPauseFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasGetPauseFlag(sasCore=0x" + Integer.toHexString(sasCore) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetPauseFlag", cpu)) {
            int pauseFlag = 0;
            for (int i = 0; i < voices.length; i++) {
                if (voices[i].isPaused()) {
                    pauseFlag |= (1 << i);
                }
            }
            cpu.gpr[2] = pauseFlag;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasRevType(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int type = cpu.gpr[5];

        // Set waveform effect's type.
        if (log.isDebugEnabled()) {
            log.debug("__sceSasRevType(sasCore=0x" + Integer.toHexString(sasCore) + ", type=" + type + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasRevType", cpu)) {
            waveformEffectType = type;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasInit(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        int grain = cpu.gpr[5];
        int maxVoices = cpu.gpr[6];
        int outMode = cpu.gpr[7];
        int sampleRate = cpu.gpr[8];

        log.info(String.format("__sceSasInit(0x%08X, grain=%d, maxVoices=%d, outMode=%d, sampleRate=%d)", sasCore, grain, maxVoices, outMode, sampleRate));

        // Tested on PSP:
        // Only one instance at a time is supported.
        if(!isSasInit) {
            if (Memory.isAddressGood(sasCore)) {
                sasCoreUid = SceUidManager.getNewUid("sceSasCore-SasCore");
                mem.write32(sasCore, sasCoreUid);
            }
            grainSamples = grain;
            outputMode = outMode;
            for (int i = 0; i < voices.length; i++) {
                voices[i].setSampleRate(sampleRate); // Default.
            }
            isSasInit = true;
        }
        cpu.gpr[2] = 0;
    }

    public void __sceSasSetVolume(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int leftVolume = cpu.gpr[6];    // Left channel volume 0 - 0x1000.
        int rightVolume = cpu.gpr[7];   // Right channel volume 0 - 0x1000.

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetVolume " + makeLogParams(cpu));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetVolume", cpu) && isVoiceNumberGood(voice, "__sceSasSetVolume", cpu)) {
            voices[voice].setLeftVolume(leftVolume << 3);	// 0 - 0x8000
            voices[voice].setRightVolume(rightVolume << 3);	// 0 - 0x8000
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasCoreWithMix(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int sasInOut = cpu.gpr[5];
        int leftVol = cpu.gpr[6];
        int rightVol = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasCoreWithMix 0x%08X, sasInOut=0x%08X, leftVol=%d, rightVol=%d", sasCore, sasInOut, leftVol, rightVol));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasCoreWithMix", cpu)) {
        	long startTime = Emulator.getClock().microTime();
            mixer.synthesizeWithMix(sasInOut, grainSamples, leftVol << 3, rightVol << 3);
            cpu.gpr[2] = 0;
            delayThreadSasCore(startTime);
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetSL(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int level = cpu.gpr[6];  // Sustain level.

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetSL: " + String.format("sasCore=0x%08x, voice=0x%08x, unk=0x%08x", sasCore, voice, level));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetSL", cpu) && isVoiceNumberGood(voice, "__sceSasSetSL", cpu)) {
            voices[voice].getEnvelope().SustainLevel = level;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasGetEndFlag(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasGetEndFlag(sasCore=0x" + Integer.toHexString(sasCore) + ")");
        }

        if (isSasHandleGood(sasCore, "__sceSasGetEndFlag", cpu)) {
            int endFlag = 0;
            for (int i = 0; i < voices.length; i++) {
                if (voices[i].isEnded()) {
                    endFlag |= (1 << i);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("__sceSasGetEndFlag returning 0x%08X", endFlag));
            }
            cpu.gpr[2] = endFlag;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasGetEnvelopeHeight(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasGetEnvelopeHeight(sasCore=0x" + Integer.toHexString(sasCore) + ",voice=" + voice + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetEnvelopeHeight", cpu) && isVoiceNumberGood(voice, "__sceSasGetEnvelopeHeight", cpu)) {
            cpu.gpr[2] = voices[voice].getEnvelope().height;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetKeyOn(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetKeyOn: " + String.format("sasCore=%08x, voice=%d",
                    sasCore, voice));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetKeyOn", cpu) && isVoiceNumberGood(voice, "__sceSasSetKeyOn", cpu)) {
        	voices[voice].on();
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetPause(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice_bit = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetPause(sasCore=0x" + Integer.toHexString(sasCore) + "): 0x" + Integer.toHexString(voice_bit));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetPause", cpu)) {
            for (int i = 0; i < voices.length; i++) {
                if (((voice_bit >> i) & 1) != 0) {
                    voices[i].setPaused(true);
                } else {
                	voices[i].setPaused(false);
                }
            }
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetVoice(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int vagAddr = cpu.gpr[6];
        int size = cpu.gpr[7];
        int loopmode = cpu.gpr[8];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetVoice: " + String.format("sasCore=0x%08x, voice=%d, vagAddr=0x%08x, size=0x%08x, loopmode=%d",
                    sasCore, voice, vagAddr, size, loopmode));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        if (size <= 0 || (size & 0xF) != 0) {
        	log.warn(String.format("__sceSasSetVoice invalid size 0x%08X", size));
        	cpu.gpr[2] = SceKernelErrors.ERROR_SAS_INVALID_PARAMETER;
        } else if (isSasHandleGood(sasCore, "__sceSasSetVoice", cpu) && isVoiceNumberGood(voice, "__sceSasSetVoice", cpu)) {
        	voices[voice].setVAG(vagAddr, size);
            voices[voice].setLoopMode(loopmode);
            cpu.gpr[2] = 0;
        }
    }

    public void __sceSasSetADSRmode(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int flag = cpu.gpr[6];   // Bitfield to set each envelope on or off.
        int attackType = cpu.gpr[7];   // ADSR Envelope's attack curve shape.
        int decayType = cpu.gpr[8];   // ADSR Envelope's decay curve shape.
        int sustainType = cpu.gpr[9];   // ADSR Envelope's sustain curve shape.
        int releaseType = cpu.gpr[10];  // ADSR Envelope's release curve shape.

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetADSRmode" + String.format("sasCore=%08x, voice=%d flag=%08x a=%08x d=%08x s=%08x r%08x",
                    sasCore, voice, flag, attackType, decayType, sustainType, releaseType));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetADSR", cpu) && isVoiceNumberGood(voice, "__sceSasSetADSRmode", cpu)) {
            if ((flag & 0x1) == 0x1) {
                voices[voice].getEnvelope().AttackCurveType = attackType;
            }
            if ((flag & 0x2) == 0x2) {
                voices[voice].getEnvelope().DecayCurveType = decayType;
            }
            if ((flag & 0x4) == 0x4) {
                voices[voice].getEnvelope().SustainCurveType = sustainType;
            }
            if ((flag & 0x8) == 0x8) {
                voices[voice].getEnvelope().ReleaseCurveType = releaseType;
            }
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetKeyOff(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetKeyOff: " + String.format("sasCore=%08x, voice=%d",
                    sasCore, voice));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetKeyOff", cpu) && isVoiceNumberGood(voice, "__sceSasSetKeyOff", cpu)) {
        	voices[voice].off();
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetTrianglarWave(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function __sceSasSetTrianglarWave [0xA232CBE6] " + makeLogParams(cpu));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void __sceSasCore(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int sasOut = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasCore 0x%08X, out=0x%08X", sasCore, sasOut));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasCore", cpu)) {
        	long startTime = Emulator.getClock().microTime();
            mixer.synthesize(sasOut, grainSamples);
            cpu.gpr[2] = 0;
            delayThreadSasCore(startTime);
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetPitch(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int pitch = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetPitch: " + String.format("sasCore=%08x, voice=%d, pitch=0x%04x",
                    sasCore, voice, pitch));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetPitch", cpu) && isVoiceNumberGood(voice, "__sceSasSetPitch", cpu)) {
            voices[voice].setPitch(pitch);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetNoise(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int freq = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetNoise: " + String.format("sasCore=%08x, voice=%d, freq=0x%04x",
                    sasCore, voice, freq));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetNoise", cpu) && isVoiceNumberGood(voice, "__sceSasSetNoise", cpu)) {
            voices[voice].setNoise(freq);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasGetGrain(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasGetGrain(sasCore=0x" + Integer.toHexString(sasCore) + "): grain samples=" + grainSamples);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetGrain", cpu)) {
            cpu.gpr[2] = grainSamples;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetSimpleADSR(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int ADSREnv1 = cpu.gpr[6];
        int ADSREnv2 = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetSimpleADSR " + makeLogParams(cpu));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Only the low-order 16 bits are valid for both parameters.
        int env1Bitfield = (ADSREnv1 & 0xFFFF);
        int env2Bitfield = (ADSREnv2 & 0xFFFF);

        if (isSasHandleGood(sasCore, "__sceSasSetSimpleADSR", cpu) && isVoiceNumberGood(voice, "__sceSasSetSimpleADSR", cpu)) {
            // The bitfields represent every value except for the decay curve shape,
            // which seems to be unchanged in simple mode.
            voices[voice].getEnvelope().SustainLevel = (env1Bitfield & 0xF);
            voices[voice].getEnvelope().DecayRate = (env1Bitfield >> 4) & 0xF;
            voices[voice].getEnvelope().AttackRate = (env1Bitfield >> 8) & 0x7F;
            voices[voice].getEnvelope().AttackCurveType = (env1Bitfield >> 15);

            voices[voice].getEnvelope().ReleaseRate = (env2Bitfield & 0x1F);
            voices[voice].getEnvelope().ReleaseCurveType = (env2Bitfield >> 5) & 0x1;
            voices[voice].getEnvelope().SustainRate = (env2Bitfield >> 6) & 0x7F;
            voices[voice].getEnvelope().SustainCurveType = (env2Bitfield >> 13);

            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetGrain(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int grain = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetGrain(sasCore=0x" + Integer.toHexString(sasCore) + "): grain samples=" + grain);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetGrain", cpu)) {
            grainSamples = grain;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasRevEVOL(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int leftVol = cpu.gpr[5];
        int rightVol = cpu.gpr[6];

        // Set waveform effect's volume.
        if (log.isDebugEnabled()) {
            log.debug("__sceSasRevEVOL(" + String.format("sasCore=0x%08x,leftVol=0x%04x,rightVol=0x%04x)", sasCore, leftVol, rightVol));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasRevEVOL", cpu)) {
            waveformEffectLeftVol = leftVol;
            waveformEffectRightVol = rightVol;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetSteepWave(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function __sceSasSetSteepWave [0xD5EBBBCD] " + makeLogParams(cpu));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void __sceSasGetOutputmode(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasGetOutputmode(sasCore=0x" + Integer.toHexString(sasCore) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetOutputmode", cpu)) {
            cpu.gpr[2] = outputMode;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetOutputmode(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int mode = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasSetOutputmode(sasCore=0x" + Integer.toHexString(sasCore) + ", mode=" + mode + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetOutputmode", cpu)) {
            outputMode = mode;
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasRevVON(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int dry = cpu.gpr[5];
        int wet = cpu.gpr[6];

        // Set waveform effect's dry and wet status.
        if (log.isDebugEnabled()) {
            log.debug("__sceSasRevVON(" + String.format("sasCore=0x%08x,dry=%d,wet=%d)", sasCore, dry, wet));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasRevVON", cpu)) {
            waveformEffectIsDryOn = (dry > 0);
            waveformEffectIsWetOn = (wet > 0);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasGetAllEnvelopeHeights(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int heightsAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetAllEnvelopeHeights(sasCore=0x%08X, heightsAddr=0x%08X)", sasCore, heightsAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetAllEnvelopeHeights", cpu)) {
        	if (Memory.isAddressGood(heightsAddr)) {
        		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(heightsAddr, voices.length * 4, 4);
        		for (int i = 0; i < voices.length; i++) {
        			memoryWriter.writeNext(voices[i].getEnvelope().height);
        		}
        		memoryWriter.flush();
                cpu.gpr[2] = 0;
        	} else {
        		cpu.gpr[2] = -1;
        	}
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }    @HLEFunction(nid = 0x019B25EB, version = 150) public HLEModuleFunction __sceSasSetADSRFunction;
    @HLEFunction(nid = 0x267A6DD2, version = 150) public HLEModuleFunction __sceSasRevParamFunction;
    @HLEFunction(nid = 0x2C8E6AB3, version = 150) public HLEModuleFunction __sceSasGetPauseFlagFunction;
    @HLEFunction(nid = 0x33D4AB37, version = 150) public HLEModuleFunction __sceSasRevTypeFunction;
    @HLEFunction(nid = 0x42778A9F, version = 150) public HLEModuleFunction __sceSasInitFunction;
    @HLEFunction(nid = 0x440CA7D8, version = 150) public HLEModuleFunction __sceSasSetVolumeFunction;
    @HLEFunction(nid = 0x50A14DFC, version = 150) public HLEModuleFunction __sceSasCoreWithMixFunction;
    @HLEFunction(nid = 0x5F9529F6, version = 150) public HLEModuleFunction __sceSasSetSLFunction;
    @HLEFunction(nid = 0x68A46B95, version = 150) public HLEModuleFunction __sceSasGetEndFlagFunction;
    @HLEFunction(nid = 0x74AE582A, version = 150) public HLEModuleFunction __sceSasGetEnvelopeHeightFunction;
    @HLEFunction(nid = 0x76F01ACA, version = 150) public HLEModuleFunction __sceSasSetKeyOnFunction;
    @HLEFunction(nid = 0x787D04D5, version = 150) public HLEModuleFunction __sceSasSetPauseFunction;
    @HLEFunction(nid = 0x99944089, version = 150) public HLEModuleFunction __sceSasSetVoiceFunction;
    @HLEFunction(nid = 0x9EC3676A, version = 150) public HLEModuleFunction __sceSasSetADSRmodeFunction;
    @HLEFunction(nid = 0xA0CF2FA4, version = 150) public HLEModuleFunction __sceSasSetKeyOffFunction;
    @HLEFunction(nid = 0xA232CBE6, version = 150) public HLEModuleFunction __sceSasSetTrianglarWaveFunction;
    @HLEFunction(nid = 0xA3589D81, version = 150) public HLEModuleFunction __sceSasCoreFunction;
    @HLEFunction(nid = 0xAD84D37F, version = 150) public HLEModuleFunction __sceSasSetPitchFunction;
    @HLEFunction(nid = 0xB7660A23, version = 150) public HLEModuleFunction __sceSasSetNoiseFunction;
    @HLEFunction(nid = 0xBD11B7C2, version = 150) public HLEModuleFunction __sceSasGetGrainFunction;
    @HLEFunction(nid = 0xCBCD4F79, version = 150) public HLEModuleFunction __sceSasSetSimpleADSRFunction;
    @HLEFunction(nid = 0xD1E0A01E, version = 150) public HLEModuleFunction __sceSasSetGrainFunction;
    @HLEFunction(nid = 0xD5A229C9, version = 150) public HLEModuleFunction __sceSasRevEVOLFunction;
    @HLEFunction(nid = 0xD5EBBBCD, version = 150) public HLEModuleFunction __sceSasSetSteepWaveFunction;
    @HLEFunction(nid = 0xE175EF66, version = 150) public HLEModuleFunction __sceSasGetOutputmodeFunction;
    @HLEFunction(nid = 0xE855BF76, version = 150) public HLEModuleFunction __sceSasSetOutputmodeFunction;
    @HLEFunction(nid = 0xF983B186, version = 150) public HLEModuleFunction __sceSasRevVONFunction;
    @HLEFunction(nid = 0x07F58C24, version = 150) public HLEModuleFunction __sceSasGetAllEnvelopeHeightsFunction;

}