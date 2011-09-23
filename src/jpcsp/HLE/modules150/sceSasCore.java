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
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.sound.SoundVoice;
import jpcsp.sound.SoundMixer;

import org.apache.log4j.Logger;

public class sceSasCore extends HLEModule implements HLEStartModule {
    public static Logger log = Modules.getLogger("sceSasCore");

    @Override
    public String getName() {
        return "sceSasCore";
    }

    @Override
    public void start() {
        sasCoreUid = -1;
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
    protected static final String sasCodeUidPurpose = "sceSasCore-SasCore";

    protected void checkSasHandleGood(int sasCore) {
        if (Memory.isAddressGood(sasCore)) {
            if (Processor.memory.read32(sasCore) == sasCoreUid) {
                return;
            }
            log.warn(String.format("%s bad sasCoreUid 0x%08X", getCallingFunctionName(3), Processor.memory.read32(sasCore)));
        } else {
            log.warn(String.format("%s bad sasCore Address 0x%08X", getCallingFunctionName(3), sasCore));
        }

        throw(new SceKernelErrorException(SceKernelErrors.ERROR_SAS_NOT_INIT));
    }

    protected void checkVoiceNumberGood(int voice) {
        if (voice < 0 || voice >= voices.length) {
            log.warn(String.format("%s bad voice number %d", getCallingFunctionName(3), voice));
    		throw(new SceKernelErrorException(SceKernelErrors.ERROR_SAS_INVALID_VOICE));
        }
    }

    protected void checkSasAndVoiceHandlesGood(int sasCore, int voice) {
    	checkSasHandleGood(sasCore);
    	checkVoiceNumberGood(voice);
    }

    protected void checkADSRmode(int curveIndex, int flag, int curveType) {
        int[] validCurveTypes = new int[] {
    		(1 << PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE) | (1 << PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT) | (1 << PSP_SAS_ADSR_CURVE_MODE_EXPONENT),
        	(1 << PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE) | (1 << PSP_SAS_ADSR_CURVE_MODE_EXPONENT_REV) | (1 << PSP_SAS_ADSR_CURVE_MODE_DIRECT),
        	(1 << PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE) | (1 << PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE) | (1 << PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT) | (1 << PSP_SAS_ADSR_CURVE_MODE_EXPONENT_REV) | (1 << PSP_SAS_ADSR_CURVE_MODE_EXPONENT) | (1 << PSP_SAS_ADSR_CURVE_MODE_DIRECT),
        	(1 << PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE) | (1 << PSP_SAS_ADSR_CURVE_MODE_EXPONENT_REV) | (1 << PSP_SAS_ADSR_CURVE_MODE_DIRECT)
        };

        if ((flag & (1 << curveIndex)) != 0) {
        	if ((validCurveTypes[curveIndex] & (1 << curveType)) == 0) {
        		throw new SceKernelErrorException(SceKernelErrors.ERROR_SAS_INVALID_ADSR_CURVE_MODE);
        	}
        }
    }

    protected void checkVoiceNotPaused(int voice) {
        if (voices[voice].isPaused()) {
        	throw new SceKernelErrorException(SceKernelErrors.ERROR_SAS_VOICE_PAUSED);
        }
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

    /**
     * 
     * @param flag        Bitfield to set each envelope on or off.
     * @param attack      ADSR Envelope's attack.
     * @param decay       ADSR Envelope's decay.
     * @param sustain     ADSR Envelope's sustain.
     * @param release     ADSR Envelope's release.
     * @return
     */
    @HLEFunction(nid = 0x019B25EB, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetADSR(int sasCore, int voice, int flag, int attack, int decay, int sustain, int release) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetADSR sasCore=0x%08X, voice=%d flag=%1X a=0x%08X d=0x%08X s=0x%08X r=0x%08X",
                    sasCore, voice, flag, attack, decay, sustain, release));
        }

        checkSasAndVoiceHandlesGood(sasCore, voice);

        if ((flag & 0x1) != 0) voices[voice].getEnvelope().AttackRate = attack;
        if ((flag & 0x2) != 0) voices[voice].getEnvelope().DecayRate = decay;
        if ((flag & 0x4) != 0) voices[voice].getEnvelope().SustainRate = sustain;
        if ((flag & 0x8) != 0) voices[voice].getEnvelope().ReleaseRate = release;

        return 0;
    }

    @HLEFunction(nid = 0x267A6DD2, version = 150, checkInsideInterrupt = true)
    public int __sceSasRevParam(int sasCore, int delay, int feedback) {
        // Set waveform effect's delay and feedback levels.
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasRevParam sasCore=0x%08X, delay=%d, feedback=%d", sasCore, delay, feedback));
        }
        
        checkSasHandleGood(sasCore);

        waveformEffectDelay = delay;
        waveformEffectFeedback = feedback;
        
        return 0;
    }

    @HLEFunction(nid = 0x2C8E6AB3, version = 150, checkInsideInterrupt = true)
    public int __sceSasGetPauseFlag(int sasCore) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetPauseFlag sasCore=0x%08X", sasCore));
        }
        
        checkSasHandleGood(sasCore);

        int pauseFlag = 0;
        for (int i = 0; i < voices.length; i++) {
            if (voices[i].isPaused()) {
                pauseFlag |= (1 << i);
            }
        }

        return pauseFlag;
    }

    @HLEFunction(nid = 0x33D4AB37, version = 150, checkInsideInterrupt = true)
    public int __sceSasRevType(int sasCore, int type) {
        // Set waveform effect's type.
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasRevType sasCore=0x%08X, type=%d", sasCore, type));
        }
        
        checkSasHandleGood(sasCore);

        waveformEffectType = type;
        
        return 0;
    }

    @HLEFunction(nid = 0x42778A9F, version = 150)
    public int __sceSasInit(int sasCore, int grain, int maxVoices, int outMode, int sampleRate) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
        	log.debug(String.format("__sceSasInit sasCore=0x%08X, grain=%d, maxVoices=%d, outMode=%d, sampleRate=%d", sasCore, grain, maxVoices, outMode, sampleRate));
        }

        if (Memory.isAddressGood(sasCore)) {
        	if (sasCoreUid != -1) {
        		// Only one Sas core can be active at a time.
        		// If a previous Uid was allocated, release it.
        		SceUidManager.releaseUid(sasCoreUid, sasCodeUidPurpose);
        	}

        	sasCoreUid = SceUidManager.getNewUid(sasCodeUidPurpose);
            mem.write32(sasCore, sasCoreUid);
        }

        grainSamples = grain;
        outputMode = outMode;
        for (int i = 0; i < voices.length; i++) {
            voices[i].setSampleRate(sampleRate); // Set default sample rate
        }

        return 0;
    }

    /**
     * 
     * @param sasCore
     * @param voice
     * @param leftVolume     Left channel volume 0 - 0x1000.
     * @param rightVolume    Right channel volume 0 - 0x1000.
     * @param effectLeftVolumne    Left effect channel volume 0 - 0x1000.
     * @param effectRightVolume    Right effect channel volume 0 - 0x1000.
     * @return
     */
    @HLEFunction(nid = 0x440CA7D8, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetVolume(Processor processor, int sasCore, int voice, int leftVolume, int rightVolume, int effectLeftVolumne, int effectRightVolume) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetVolume sasCore=0x%08X, voice=%d, leftVolume=0x%04X, rightVolume=0x%04X, effectLeftVolume=0x%04X, effectRightVolume=0x%04X", sasCore, voice, leftVolume, rightVolume, effectLeftVolumne, effectRightVolume));
        }
        
        checkSasAndVoiceHandlesGood(sasCore, voice);

        voices[voice].setLeftVolume(leftVolume << 3);	// 0 - 0x8000
        voices[voice].setRightVolume(rightVolume << 3);	// 0 - 0x8000
        
        return 0;
    }

    @HLEFunction(nid = 0x50A14DFC, version = 150, checkInsideInterrupt = true)
    public int __sceSasCoreWithMix(int sasCore, int sasInOut, int leftVol, int rightVol) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasCoreWithMix 0x%08X, sasInOut=0x%08X, leftVol=0x%04X, rightVol=0x%04X", sasCore, sasInOut, leftVol, rightVol));
        }
        
        checkSasHandleGood(sasCore);

    	long startTime = Emulator.getClock().microTime();
        mixer.synthesizeWithMix(sasInOut, grainSamples, leftVol << 3, rightVol << 3);
        delayThreadSasCore(startTime);

        return 0;
    }

    @HLEFunction(nid = 0x5F9529F6, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetSL(int sasCore, int voice, int level) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetSL sasCore=0x%08X, voice=%d, level=0x%08X", sasCore, voice, level));
        }

        checkSasAndVoiceHandlesGood(sasCore, voice);

        voices[voice].getEnvelope().SustainLevel = level;

        return 0;
    }

    @HLEFunction(nid = 0x68A46B95, version = 150)
    public int __sceSasGetEndFlag(int sasCore) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetEndFlag sasCore=0x%08X", sasCore));
        }

        checkSasHandleGood(sasCore);

        int endFlag = 0;
        for (int i = 0; i < voices.length; i++) {
            if (voices[i].isEnded()) {
                endFlag |= (1 << i);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetEndFlag returning 0x%08X", endFlag));
        }

        return endFlag;
    }

    @HLEFunction(nid = 0x74AE582A, version = 150, checkInsideInterrupt = true)
    public int __sceSasGetEnvelopeHeight(int sasCore, int voice) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetEnvelopeHeight sasCore=0x%08X ,voice=%d", sasCore, voice));
        }
        
        checkSasAndVoiceHandlesGood(sasCore, voice);

        return voices[voice].getEnvelope().height;
    }

    @HLEFunction(nid = 0x76F01ACA, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetKeyOn(int sasCore, int voice) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetKeyOn: sasCore=0x%08X, voice=%d", sasCore, voice));
        }
        
        checkSasAndVoiceHandlesGood(sasCore, voice);
        checkVoiceNotPaused(voice);

        voices[voice].on();
    	
        return 0;
    }

    @HLEFunction(nid = 0x787D04D5, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetPause(int sasCore, int voice_bit, boolean setPause) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetPause sasCore=0x%08X voice_bit=0x%X setPause=%b", sasCore, voice_bit, setPause));
        }
        
        checkSasHandleGood(sasCore);

    	// Update only the pause flag of the voices
    	// where the corresponding bit is set:
    	// set or reset the pause flag according to the "setPause" parameter.
        for (int i = 0; voice_bit != 0; i++, voice_bit >>>= 1) {
            if ((voice_bit & 1) != 0) {
            	voices[i].setPaused(setPause);
            }
        }
        
        return 0;
    }

    @HLEFunction(nid = 0x99944089, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetVoice(int sasCore, int voice, int vagAddr, int size, int loopmode) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetVoice sasCore=0x%08X, voice=%d, vagAddr=0x%08X, size=0x%08X, loopmode=%d", sasCore, voice, vagAddr, size, loopmode));
        }

        if (size <= 0 || (size & 0xF) != 0) {
        	log.warn(String.format("__sceSasSetVoice invalid size 0x%08X", size));
        	throw(new SceKernelErrorException(SceKernelErrors.ERROR_SAS_INVALID_PARAMETER));
        }

        checkSasAndVoiceHandlesGood(sasCore, voice);
        
    	voices[voice].setVAG(vagAddr, size);
        voices[voice].setLoopMode(loopmode);
        
        return 0;
    }

    /**
     * 
     * @param sasCore
     * @param voice
     * @param flag          Bitfield to set each envelope on or off.
     * @param attackType    ADSR Envelope's attack curve shape.
     * @param decayType     ADSR Envelope's decay curve shape.
     * @param sustainType   ADSR Envelope's sustain curve shape.
     * @param releaseType   ADSR Envelope's release curve shape.
     * @return
     */
    @HLEFunction(nid = 0x9EC3676A, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetADSRmode(int sasCore, int voice, int flag, int attackType, int decayType, int sustainType, int releaseType) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetADSRmode sasCore=0x%08X, voice=%d, flag=%1X, attackType=%d, decayType=%d, sustainType=%d, releaseType=%d", sasCore, voice, flag, attackType, decayType, sustainType, releaseType));
        }

        checkSasAndVoiceHandlesGood(sasCore, voice);

        checkADSRmode(0, flag, attackType);
        checkADSRmode(1, flag, decayType);
        checkADSRmode(2, flag, sustainType);
        checkADSRmode(3, flag, releaseType);

        if ((flag & 0x1) != 0) voices[voice].getEnvelope().AttackCurveType = attackType;
        if ((flag & 0x2) != 0) voices[voice].getEnvelope().DecayCurveType = decayType;
        if ((flag & 0x4) != 0) voices[voice].getEnvelope().SustainCurveType = sustainType;
        if ((flag & 0x8) != 0) voices[voice].getEnvelope().ReleaseCurveType = releaseType;

        return 0;
    }

    @HLEFunction(nid = 0xA0CF2FA4, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetKeyOff(int sasCore, int voice) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetKeyOff sasCore=%08X, voice=%d", sasCore, voice));
        }
        
        checkSasAndVoiceHandlesGood(sasCore, voice);
        checkVoiceNotPaused(voice);

    	voices[voice].off();

    	return 0;
    }

    @HLEFunction(nid = 0xA232CBE6, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetTrianglarWave(int sasCore, int voice, int unknown) {
        log.warn(String.format("Unimplemented __sceSasSetTrianglarWave sasCore=0x%08X, voice=%d, unknown=0x%08X", sasCore, voice, unknown));

        checkSasAndVoiceHandlesGood(sasCore, voice);

        return 0;
    }

    @HLEFunction(nid = 0xA3589D81, version = 150, checkInsideInterrupt = true)
    public int __sceSasCore(int sasCore, int sasOut) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasCore 0x%08X, out=0x%08X", sasCore, sasOut));
        }
        
        checkSasHandleGood(sasCore);

    	long startTime = Emulator.getClock().microTime();
        mixer.synthesize(sasOut, grainSamples);
        delayThreadSasCore(startTime);

        return 0;
    }

    @HLEFunction(nid = 0xAD84D37F, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetPitch(int sasCore, int voice, int pitch) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetPitch sasCore=%08X, voice=%d, pitch=0x%04X", sasCore, voice, pitch));
        }
        
        checkSasAndVoiceHandlesGood(sasCore, voice);
        
        voices[voice].setPitch(pitch);
        
        return 0;
    }

    @HLEFunction(nid = 0xB7660A23, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetNoise(int sasCore, int voice, int freq) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetNoise sasCore=%08X, voice=%d, freq=0x%04X", sasCore, voice, freq));
        }
        
        checkSasAndVoiceHandlesGood(sasCore, voice);

        voices[voice].setNoise(freq);

        return 0;
    }

    @HLEFunction(nid = 0xBD11B7C2, version = 150, checkInsideInterrupt = true)
    public int __sceSasGetGrain(int sasCore) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetGrain sasCore=0x%08X returning grain samples=%d", sasCore, grainSamples));
        }
        
        checkSasHandleGood(sasCore);
        
        return grainSamples;
    }

    @HLEFunction(nid = 0xCBCD4F79, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetSimpleADSR(Processor processor, int sasCore, int voice, int ADSREnv1, int ADSREnv2) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetSimpleADSR sasCore=0x%08X, voice=%d, ADSREnv1=0x%04X, ADSREnv2=0x%04X", sasCore, voice, ADSREnv1, ADSREnv2));
        }

        checkSasAndVoiceHandlesGood(sasCore, voice);

        // Only the low-order 16 bits are valid for both parameters.
        int env1Bitfield = (ADSREnv1 & 0xFFFF);
        int env2Bitfield = (ADSREnv2 & 0xFFFF);

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

        return 0;
    }

    @HLEFunction(nid = 0xD1E0A01E, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetGrain(int sasCore, int grain) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetGrain sasCore=0x%08X, grain=%d", sasCore, grain));
        }
        
        checkSasHandleGood(sasCore);
        
        grainSamples = grain;
        
        return 0;
    }

    @HLEFunction(nid = 0xD5A229C9, version = 150, checkInsideInterrupt = true)
    public int __sceSasRevEVOL(int sasCore, int leftVol, int rightVol) {
        // Set waveform effect's volume.
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasRevEVOL sasCore=0x%08X, leftVol=0x%04X, rightVol=0x%04X", sasCore, leftVol, rightVol));
        }
        
        checkSasHandleGood(sasCore);

        waveformEffectLeftVol = leftVol;
        waveformEffectRightVol = rightVol;

        return 0;
    }

    @HLEFunction(nid = 0xD5EBBBCD, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetSteepWave(int sasCore, int voice, int unknown) {
        log.warn(String.format("Unimplemented __sceSasSetSteepWave sasCore=0x%08X, voice=%d, unknown=0x%08X", sasCore, voice, unknown));

        checkSasAndVoiceHandlesGood(sasCore, voice);

        return 0;
    }

    @HLEFunction(nid = 0xE175EF66, version = 150, checkInsideInterrupt = true)
    public int __sceSasGetOutputmode(int sasCore) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetOutputmode sasCore=0x%08X, returning output mode=%d", sasCore, outputMode));
        }

        checkSasHandleGood(sasCore);
    	
    	return outputMode;
    }

    @HLEFunction(nid = 0xE855BF76, version = 150, checkInsideInterrupt = true)
    public int __sceSasSetOutputmode(int sasCore, int mode) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasSetOutputmode sasCore=0x%08X, mode=%d", sasCore, mode));
        }
        
        checkSasHandleGood(sasCore);
        outputMode = mode;

        return 0;
    }

    @HLEFunction(nid = 0xF983B186, version = 150, checkInsideInterrupt = true)
    public int __sceSasRevVON(int sasCore, int dry, int wet) {
        // Set waveform effect's dry and wet status.
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasRevVON sasCore=0x%08X, dry=%d, wet=%d", sasCore, dry, wet));
        }
        
        checkSasHandleGood(sasCore);

        waveformEffectIsDryOn = (dry > 0);
        waveformEffectIsWetOn = (wet > 0);

        return 0;
    }

    @HLEFunction(nid = 0x07F58C24, version = 150, checkInsideInterrupt = true)
    public int __sceSasGetAllEnvelopeHeights(int sasCore, int heightsAddr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("__sceSasGetAllEnvelopeHeights sasCore=0x%08X, heightsAddr=0x%08X", sasCore, heightsAddr));
        }
        
        checkSasHandleGood(sasCore);

        if (!Memory.isAddressGood(heightsAddr)) {
        	return -1;
        }

		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(heightsAddr, voices.length * 4, 4);
		for (int i = 0; i < voices.length; i++) {
			memoryWriter.writeNext(voices[i].getEnvelope().height);
		}
		memoryWriter.flush();

		return 0;
    }
}