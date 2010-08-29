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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

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
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

import org.apache.log4j.Logger;

public class sceSasCore implements HLEModule, HLEStartModule {

    protected static Logger log = Modules.getLogger("sceSasCore");

    @Override
    public String getName() {
        return "sceSasCore";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x019B25EB, __sceSasSetADSRFunction);
            mm.addFunction(0x267A6DD2, __sceSasRevParamFunction);
            mm.addFunction(0x2C8E6AB3, __sceSasGetPauseFlagFunction);
            mm.addFunction(0x33D4AB37, __sceSasRevTypeFunction);
            mm.addFunction(0x42778A9F, __sceSasInitFunction);
            mm.addFunction(0x440CA7D8, __sceSasSetVolumeFunction);
            mm.addFunction(0x50A14DFC, __sceSasCoreWithMixFunction);
            mm.addFunction(0x5F9529F6, __sceSasSetSLFunction);
            mm.addFunction(0x68A46B95, __sceSasGetEndFlagFunction);
            mm.addFunction(0x74AE582A, __sceSasGetEnvelopeHeightFunction);
            mm.addFunction(0x76F01ACA, __sceSasSetKeyOnFunction);
            mm.addFunction(0x787D04D5, __sceSasSetPauseFunction);
            mm.addFunction(0x99944089, __sceSasSetVoiceFunction);
            mm.addFunction(0x9EC3676A, __sceSasSetADSRmodeFunction);
            mm.addFunction(0xA0CF2FA4, __sceSasSetKeyOffFunction);
            mm.addFunction(0xA232CBE6, __sceSasSetTrianglarWaveFunction);
            mm.addFunction(0xA3589D81, __sceSasCoreFunction);
            mm.addFunction(0xAD84D37F, __sceSasSetPitchFunction);
            mm.addFunction(0xB7660A23, __sceSasSetNoiseFunction);
            mm.addFunction(0xBD11B7C2, __sceSasGetGrainFunction);
            mm.addFunction(0xCBCD4F79, __sceSasSetSimpleADSRFunction);
            mm.addFunction(0xD1E0A01E, __sceSasSetGrainFunction);
            mm.addFunction(0xD5A229C9, __sceSasRevEVOLFunction);
            mm.addFunction(0xD5EBBBCD, __sceSasSetSteepWaveFunction);
            mm.addFunction(0xE175EF66, __sceSasGetOutputmodeFunction);
            mm.addFunction(0xE855BF76, __sceSasSetOutputmodeFunction);
            mm.addFunction(0xF983B186, __sceSasRevVONFunction);
            mm.addFunction(0x07F58C24, __sceSasGetAllEnvelopeHeightsFunction);
        }


    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(__sceSasSetADSRFunction);
            mm.removeFunction(__sceSasRevParamFunction);
            mm.removeFunction(__sceSasGetPauseFlagFunction);
            mm.removeFunction(__sceSasRevTypeFunction);
            mm.removeFunction(__sceSasInitFunction);
            mm.removeFunction(__sceSasSetVolumeFunction);
            mm.removeFunction(__sceSasCoreWithMixFunction);
            mm.removeFunction(__sceSasSetSLFunction);
            mm.removeFunction(__sceSasGetEndFlagFunction);
            mm.removeFunction(__sceSasGetEnvelopeHeightFunction);
            mm.removeFunction(__sceSasSetKeyOnFunction);
            mm.removeFunction(__sceSasSetPauseFunction);
            mm.removeFunction(__sceSasSetVoiceFunction);
            mm.removeFunction(__sceSasSetADSRmodeFunction);
            mm.removeFunction(__sceSasSetKeyOffFunction);
            mm.removeFunction(__sceSasSetTrianglarWaveFunction);
            mm.removeFunction(__sceSasCoreFunction);
            mm.removeFunction(__sceSasSetPitchFunction);
            mm.removeFunction(__sceSasSetNoiseFunction);
            mm.removeFunction(__sceSasGetGrainFunction);
            mm.removeFunction(__sceSasSetSimpleADSRFunction);
            mm.removeFunction(__sceSasSetGrainFunction);
            mm.removeFunction(__sceSasRevEVOLFunction);
            mm.removeFunction(__sceSasSetSteepWaveFunction);
            mm.removeFunction(__sceSasGetOutputmodeFunction);
            mm.removeFunction(__sceSasSetOutputmodeFunction);
            mm.removeFunction(__sceSasRevVONFunction);
            mm.removeFunction(__sceSasGetAllEnvelopeHeightsFunction);

        }
    }

    @Override
    public void start() {
        sasCoreUid = -1;
        sampleRate = 48000;
        voices = new pspVoice[32];
        for (int i = 0; i < voices.length; i++) {
            voices[i] = new pspVoice();
        }

        grainSamples = 0x100; // Normal base value for sound processing.
        outputMode = 0; // Checked. 0 is default (STEREO).
        sasVolLevel = 0;

        if (voicesCheckerThread == null) {
            voicesCheckerThread = new VoicesCheckerThread(500);
            voicesCheckerThread.setDaemon(true);
            voicesCheckerThread.setName("sceSasCore Voices Checker");
            voicesCheckerThread.start();
        }
    }

    @Override
    public void stop() {
    }

    public void setAudioMuted(boolean muted) {
        audioMuted = muted;
    }

    public boolean isAudioMuted() {
        return audioMuted;
    }

    public void setSasVolUp() {
        if (sasVolLevel < sceAudio.PSP_AUDIO_VOLUME_MAX) {
            sasVolLevel += 0x100;
        }
    }

    public void setSasVolDown() {
        if (sasVolLevel > -sceAudio.PSP_AUDIO_VOLUME_MAX) {
            sasVolLevel -= 0x100;
        }
    }

    private int processVolume(int vol) {
        if (audioMuted) {
            return 0;
        } else {
            if ((vol + sasVolLevel) > sceAudio.PSP_AUDIO_VOLUME_MAX) {
                return sceAudio.PSP_AUDIO_VOLUME_MAX;
            } else if ((vol + sasVolLevel) < 0) {
                return 0;
            } else {
                return (vol + sasVolLevel);
            }
        }
    }

    protected class pspVoice {

        public SourceDataLine outputDataLine;
        public float outputDataLineSampleRate;
        public int leftVolume;
        public int rightVolume;
        public short[] samples;
        public int loopMode;
        public int pitch;
        public final int NORMAL_PITCH = 0x1000;
        public int noise;
        public byte[] buffer;
        public int bufferIndex;
        public boolean paused;
        public VoiceADSREnvelope envelope;

        protected class VoiceADSREnvelope {

            public int AttackRate;
            public int DecayRate;
            public int SustainRate;
            public int ReleaseRate;
            public int AttackCurveType;
            public int DecayCurveType;
            public int SustainCurveType;
            public int ReleaseCurveType;
            public int SustainLevel;
            public int height;

            public VoiceADSREnvelope() {
                AttackRate = 0;
                DecayRate = 0;
                SustainRate = 0;
                ReleaseRate = 0;
                SustainLevel = 0;
                height = 0x10000000;
            }
        }

        public pspVoice() {
            outputDataLine = null;
            leftVolume = 0x8000;
            rightVolume = 0x8000;
            samples = null;
            loopMode = 0;
            pitch = NORMAL_PITCH;
            noise = 0;
            buffer = null;
            bufferIndex = 0;
            paused = false;
            envelope = new VoiceADSREnvelope();
        }

        private float getSampleRate() {
            return (sampleRate * pitch) / (float) NORMAL_PITCH;
        }

        private void init() {
            float wantedSampleRate = getSampleRate();
            int wantedBufferSize = 0;
            if (samples != null) {
                wantedBufferSize = samples.length * 4;
            } else if (outputDataLine != null) {
                wantedBufferSize = outputDataLine.getBufferSize();
            }

            if (outputDataLine == null || wantedSampleRate != outputDataLineSampleRate || wantedBufferSize > outputDataLine.getBufferSize()) {
                if (outputDataLine != null) {
                    outputDataLine.close();
                    outputDataLine = null;
                }

                try {
                    AudioFormat format = new AudioFormat(wantedSampleRate, 16, 2, true, false);
                    outputDataLine = AudioSystem.getSourceDataLine(format);
                    outputDataLineSampleRate = wantedSampleRate;
                    if (wantedBufferSize > 0) {
                        outputDataLine.open(format, wantedBufferSize);
                    } else {
                        outputDataLine.open(format);
                    }
                } catch (LineUnavailableException e) {
                    log.info("sceSasCore.pspVoice.init: " + e.toString());
                }
            }
        }

        public synchronized int on() {
            init();

            if (samples != null) {
                outputDataLine.stop();
                buffer = encodeSamples();
                int length = Math.min(outputDataLine.available(), buffer.length);
                bufferIndex = length;
                outputDataLine.write(buffer, 0, length);
                outputDataLine.start();
            }

            return 0;
        }

        public synchronized int off() {
            if (outputDataLine != null) {
                outputDataLine.stop();
            }

            return 0;
        }

        public synchronized boolean IsEnded() {
            if (outputDataLine == null) {
                return true;
            }

            if (!outputDataLine.isRunning()) {
                return true;
            }

            if (buffer != null && bufferIndex < buffer.length) {
                return false;
            }

            if (outputDataLine.available() >= outputDataLine.getBufferSize()) {
                return true;
            }

            return false;
        }

        public synchronized boolean isPaused() {
            return paused;
        }

        public synchronized void pause() {
            paused = true;
        }

        private byte[] encodeSamples() {
            int numSamples = samples.length;
            byte[] samplesBuffer = new byte[numSamples * 4];
            int leftVol = processVolume(leftVolume);
            int rightVol = processVolume(rightVolume);
            for (int i = 0; i < numSamples; i++) {
                short sample = samples[i];
                short lval = (short) ((sample * leftVol) >> 16);
                short rval = (short) ((sample * rightVol) >> 16);
                samplesBuffer[i * 4 + 0] = (byte) (lval);
                samplesBuffer[i * 4 + 1] = (byte) (lval >> 8);
                samplesBuffer[i * 4 + 2] = (byte) (rval);
                samplesBuffer[i * 4 + 3] = (byte) (rval >> 8);
            }

            return samplesBuffer;
        }

        public synchronized void check() {
            if (outputDataLine == null || !outputDataLine.isActive() || buffer == null) {
                return;
            }

            if (bufferIndex < buffer.length) {
                int length = Math.min(outputDataLine.available(), buffer.length - bufferIndex);
                if (length > 0) {
                    outputDataLine.write(buffer, bufferIndex, length);
                    bufferIndex += length;
                }
            } else if (IsEnded()) {
                outputDataLine.stop();
            }
        }
    }

    protected class VoicesCheckerThread extends Thread {

        private long delayMillis;

        public VoicesCheckerThread(long delayMillis) {
            this.delayMillis = delayMillis;
        }

        @Override
        public void run() {
            while (true) {
                for (int i = 0; i < voices.length; i++) {
                    voices[i].check();
                }

                try {
                    sleep(delayMillis);
                } catch (InterruptedException e) {
                    // Ignore the Exception
                }
            }
        }
    }
    private static VoicesCheckerThread voicesCheckerThread = null;
    protected int sasCoreUid;
    protected pspVoice[] voices;
    protected int sampleRate;
    protected boolean audioMuted;
    protected int grainSamples;
    protected int outputMode;
    protected int sasVolLevel;
    protected static final int waveformBufMaxSize = 1024;  // 256 sound samples.
    protected int waveformEffectType;
    protected int waveformEffectLeftVol;
    protected int waveformEffectRightVol;
    protected int waveformEffectDelay;
    protected int waveformEffectFeedback;
    protected boolean waveformEffectIsDryOn;
    protected boolean waveformEffectIsWetOn;

    protected String makeLogParams(CpuState cpu) {
        return String.format("%08x %08x %08x %08x",
                cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]);
    }

    /** If sasCore isn't a valid handle this function will print a log message and set $v0 to -1.
     * @return true if sasCore is good. */
    protected boolean isSasHandleGood(int sasCore, String functionName, CpuState cpu) {
        if (Processor.memory.isAddressGood(sasCore)) {
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
        cpu.gpr[2] = -1;
        return false;
    }

    protected short[] decodeSamples(Processor processor, int vagAddr, int size) {
        Memory mem = Processor.memory;

        // Based on vgmstream
        short[] samples = new short[size / 16 * 28];
        int numSamples = 0;

        // VAG address can be null. In this case, just return empty samples.
        if (vagAddr != 0) {
            int headerCheck = mem.read32(vagAddr);
            if ((headerCheck & 0x00FFFFFF) == 0x00474156) { // VAGx
                vagAddr += 0x30;	// Skip the VAG header
            }

            int[] unpackedSamples = new int[28];
            int hist1 = 0;
            int hist2 = 0;
            final double[][] VAG_f = {
                {0.0, 0.0},
                {60.0 / 64.0, 0.0},
                {115.0 / 64.0, -52.0 / 64.0},
                {98.0 / 64.0, -55.0 / 64.0},
                {122.0 / 64.0, -60.0 / 64.0}
            };
            IMemoryReader memoryReader = MemoryReader.getMemoryReader(vagAddr, 1);
            for (int i = 0; i <= (size - 16); i += 16) {
                int n = memoryReader.readNext();
                int predict_nr = n >> 4;
                if (predict_nr >= VAG_f.length) {
                    if (predict_nr == 7) {
                        break; // A predict_nr of 7 indicates the end of audio data.
                    } else {
                        log.warn("sceSasCore.decodeSamples: Unknown value for predict_nr: " + predict_nr);
                        predict_nr = 0;
                    }
                }
                int shift_factor = n & 0x0F;
                int flag = memoryReader.readNext();
                if (flag == 0x07) {
                    break;	// End of stream flag
                }
                for (int j = 0; j < 28; j += 2) {
                    int d = memoryReader.readNext();
                    int s = (short) ((d & 0x0F) << 12);
                    unpackedSamples[j] = s >> shift_factor;
                    s = (short) ((d & 0xF0) << 8);
                    unpackedSamples[j + 1] = s >> shift_factor;
                }

                for (int j = 0; j < 28; j++) {
                    int sample = (int) (unpackedSamples[j] + hist1 * VAG_f[predict_nr][0] + hist2 * VAG_f[predict_nr][1]);
                    hist2 = hist1;
                    hist1 = sample;

                    if (sample < -32768) {
                        samples[numSamples] = -32768;
                    } else if (sample > 0x7FFF) {
                        samples[numSamples] = 0x7FFF;
                    } else {
                        samples[numSamples] = (short) sample;
                    }
                    numSamples++;
                }
            }

            if (samples.length != numSamples) {
                short[] resizedSamples = new short[numSamples];
                System.arraycopy(samples, 0, resizedSamples, 0, numSamples);
                samples = resizedSamples;
            }
        }

        return samples;
    }

    public void __sceSasSetADSR(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetADSR", cpu)) {
            if ((flag & 0x1) == 0x1) {
                voices[voice].envelope.AttackRate = attack;
            }
            if ((flag & 0x2) == 0x2) {
                voices[voice].envelope.DecayRate = decay;
            }
            if ((flag & 0x4) == 0x4) {
                voices[voice].envelope.SustainRate = sustain;
            }
            if ((flag & 0x8) == 0x8) {
                voices[voice].envelope.ReleaseRate = release;
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
        log.debug("__sceSasRevParam(" + String.format("sasCore=0x%08x, delay=%d, feedback=%d)", sasCore, delay, feedback));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
        // -1 -> No effect.
        // 0 -> "Room" effect.
        // 1 -> Unknown.
        // 2 -> Unknown.
        // 3 -> Unknown.
        // 4 -> Unknown.
        // 5 -> Unknown.
        // 6 -> Echo (uses feedback).
        // 7 -> Delay (uses delay).
        // 8 -> "Pipe" effect.
        log.debug("__sceSasRevType(sasCore=0x" + Integer.toHexString(sasCore) + ", type=" + type + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
        int unk1 = cpu.gpr[5]; // 0x00000100
        int unk2 = cpu.gpr[6]; // 0x00000020
        int unk3 = cpu.gpr[7]; // 0x00000000
        int sasSampleRate = cpu.gpr[8]; // 0x0000AC44 (44100 Hz)

        log.info("PARTIAL __sceSasInit: " + String.format("sasCore=0x%08x, unk1=0x%08x, unk2=0x%08x, unk3=0x%08x, sampleRate=%d",
                sasCore, cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], sampleRate));

        // We support only 1 sascore instance at a time.
        // Currently, we overwrite the previous sascore...
        if (sasCoreUid != -1) {
            log.warn("UNIMPLEMENTED:__sceSasInit multiple instances not yet supported");
        }

        if (mem.isAddressGood(sasCore)) {
            sasCoreUid = SceUidManager.getNewUid("sceMpeg-Mpeg");
            mem.write32(sasCore, sasCoreUid);
        }

        sampleRate = sasSampleRate;
        cpu.gpr[2] = 0;
    }

    public void __sceSasSetVolume(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int leftVolume = cpu.gpr[6]; // left channel volume 0 - 0x1000
        int rightVolume = cpu.gpr[7]; // right channel volume 0 - 0x1000

        log.debug("__sceSasSetVolume " + makeLogParams(cpu));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetVolume", cpu) && isVoiceNumberGood(voice, "__sceSasSetVolume", cpu)) {
            voices[voice].leftVolume = leftVolume << 3;	// 0 - 0x8000
            voices[voice].rightVolume = rightVolume << 3;	// 0 - 0x8000
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasCoreWithMix(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        int sasOut = cpu.gpr[5]; // Main SAS engine sound bank.
        int leftVol = cpu.gpr[6];
        int rightVol = cpu.gpr[7];

        log.debug("__sceSasCoreWithMix " + makeLogParams(cpu));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasCoreWithMix", cpu)) {
            mem.memset(sasOut, (byte) 0, waveformBufMaxSize);  // Empty waveform.
            cpu.gpr[2] = 0;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetSL", cpu)) {
            voices[voice].envelope.SustainLevel = level;
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

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetEndFlag", cpu)) {
            int endFlag = 0;
            for (int i = 0; i < voices.length; i++) {
                if (voices[i].IsEnded()) {
                    endFlag |= (1 << i);
                }
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
        int unk = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("__sceSasGetEnvelopeHeight(sasCore=0x" + Integer.toHexString(sasCore) + ",voice=" + voice + ",unk=0x" + Integer.toHexString(unk) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetAllEnvelopeHeights", cpu)) {
            cpu.gpr[2] = voices[voice].envelope.height;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetKeyOn", cpu) && isVoiceNumberGood(voice, "__sceSasSetKeyOn", cpu)) {
            cpu.gpr[2] = voices[voice].on();
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetPause", cpu)) {
            for (int i = 0; i < voices.length; i++) {
                if ((Integer.lowestOneBit(voice_bit >> i)) == 1) {
                    voices[i].pause();
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetVoice", cpu) && isVoiceNumberGood(voice, "__sceSasSetVoice", cpu)) {
            voices[voice].samples = decodeSamples(processor, vagAddr, size);
            voices[voice].loopMode = loopmode;

            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
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
            log.warn("__sceSasSetADSRmode" + String.format("sasCore=%08x, voice=%d flag=%08x a=%08x d=%08x s=%08x r%08x",
                    sasCore, voice, flag, attackType, decayType, sustainType, releaseType));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetADSR", cpu)) {
            if ((flag & 0x1) == 0x1) {
                voices[voice].envelope.AttackCurveType = attackType;
            }
            if ((flag & 0x2) == 0x2) {
                voices[voice].envelope.DecayCurveType = decayType;
            }
            if ((flag & 0x4) == 0x4) {
                voices[voice].envelope.SustainCurveType = sustainType;
            }
            if ((flag & 0x8) == 0x8) {
                voices[voice].envelope.ReleaseCurveType = releaseType;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetKeyOff", cpu) && isVoiceNumberGood(voice, "__sceSasSetKeyOff", cpu)) {
            cpu.gpr[2] = voices[voice].off();
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }

    public void __sceSasSetTrianglarWave(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function __sceSasSetTrianglarWave [0xA232CBE6] " + makeLogParams(cpu));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void __sceSasCore(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        int sasOut = cpu.gpr[5]; // Main SAS engine sound bank.

        log.debug("__sceSasCore " + makeLogParams(cpu));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasCore", cpu)) {
            mem.memset(sasOut, (byte) 0, waveformBufMaxSize);  // Empty waveform.
            cpu.gpr[2] = 0;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetPitch", cpu) && isVoiceNumberGood(voice, "__sceSasSetPitch", cpu)) {
            voices[voice].pitch = pitch;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasSetNoise", cpu) && isVoiceNumberGood(voice, "__sceSasSetNoise", cpu)) {
            voices[voice].noise = freq;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Only the low-order 16 bits are valid for both parameters.
        int env1Bitfield = (ADSREnv1 & 0xFFFF);
        int env2Bitfield = (ADSREnv2 & 0xFFFF);

        if (isSasHandleGood(sasCore, "__sceSasSetSimpleADSR", cpu)) {
            // The bitfields represent every value except for the decay curve shape,
            // which seems to be unchanged in simple mode.
            voices[voice].envelope.SustainLevel = (env1Bitfield & 0xF);
            voices[voice].envelope.DecayRate = (env1Bitfield >> 4) & 0xF;
            voices[voice].envelope.AttackRate = (env1Bitfield >> 8) & 0x7F;
            voices[voice].envelope.AttackCurveType = (env1Bitfield >> 15);

            voices[voice].envelope.ReleaseRate = (env2Bitfield & 0x1F);
            voices[voice].envelope.ReleaseCurveType = (env2Bitfield >> 5) & 0x1;
            voices[voice].envelope.SustainRate = (env2Bitfield >> 6) & 0xFF;
            voices[voice].envelope.SustainCurveType = (env2Bitfield >> 14);

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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
        log.debug("__sceSasRevEVOL(" + String.format("sasCore=0x%08x,leftVol=0x%04x,rightVol=0x%04x)", sasCore, leftVol, rightVol));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
        log.debug("__sceSasRevVON(" + String.format("sasCore=0x%08x,dry=%d,wet=%d)", sasCore, dry, wet));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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

        if (log.isDebugEnabled()) {
            log.debug("__sceSasGetAllEnvelopeHeights(" + String.format("sasCore=0x%08x)", sasCore));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSasHandleGood(sasCore, "__sceSasGetAllEnvelopeHeights", cpu)) {
            int res = 0;
            for (int i = 0; i < voices.length; i++) {
                if (!voices[i].IsEnded()) {
                    res += voices[i].envelope.height;
                }
            }
            cpu.gpr[2] = res;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SAS_NOT_INIT;
        }
    }
    public final HLEModuleFunction __sceSasSetADSRFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetADSR") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetADSR(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetADSR(processor);";
        }
    };
    public final HLEModuleFunction __sceSasRevParamFunction = new HLEModuleFunction("sceSasCore", "__sceSasRevParam") {

        @Override
        public final void execute(Processor processor) {
            __sceSasRevParam(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasRevParam(processor);";
        }
    };
    public final HLEModuleFunction __sceSasGetPauseFlagFunction = new HLEModuleFunction("sceSasCore", "__sceSasGetPauseFlag") {

        @Override
        public final void execute(Processor processor) {
            __sceSasGetPauseFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasGetPauseFlag(processor);";
        }
    };
    public final HLEModuleFunction __sceSasRevTypeFunction = new HLEModuleFunction("sceSasCore", "__sceSasRevType") {

        @Override
        public final void execute(Processor processor) {
            __sceSasRevType(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasRevType(processor);";
        }
    };
    public final HLEModuleFunction __sceSasInitFunction = new HLEModuleFunction("sceSasCore", "__sceSasInit") {

        @Override
        public final void execute(Processor processor) {
            __sceSasInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasInit(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetVolumeFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetVolume") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetVolume(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetVolume(processor);";
        }
    };
    public final HLEModuleFunction __sceSasCoreWithMixFunction = new HLEModuleFunction("sceSasCore", "__sceSasCoreWithMix") {

        @Override
        public final void execute(Processor processor) {
            __sceSasCoreWithMix(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasCoreWithMix(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetSLFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetSL") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetSL(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetSL(processor);";
        }
    };
    public final HLEModuleFunction __sceSasGetEndFlagFunction = new HLEModuleFunction("sceSasCore", "__sceSasGetEndFlag") {

        @Override
        public final void execute(Processor processor) {
            __sceSasGetEndFlag(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasGetEndFlag(processor);";
        }
    };
    public final HLEModuleFunction __sceSasGetEnvelopeHeightFunction = new HLEModuleFunction("sceSasCore", "__sceSasGetEnvelopeHeight") {

        @Override
        public final void execute(Processor processor) {
            __sceSasGetEnvelopeHeight(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasGetEnvelopeHeight(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetKeyOnFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetKeyOn") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetKeyOn(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetKeyOn(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetPauseFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetPause") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetPause(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetPause(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetVoiceFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetVoice") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetVoice(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetVoice(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetADSRmodeFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetADSRmode") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetADSRmode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetADSRmode(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetKeyOffFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetKeyOff") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetKeyOff(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetKeyOff(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetTrianglarWaveFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetTrianglarWave") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetTrianglarWave(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetTrianglarWave(processor);";
        }
    };
    public final HLEModuleFunction __sceSasCoreFunction = new HLEModuleFunction("sceSasCore", "__sceSasCore") {

        @Override
        public final void execute(Processor processor) {
            __sceSasCore(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasCore(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetPitchFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetPitch") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetPitch(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetPitch(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetNoiseFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetNoise") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetNoise(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetNoise(processor);";
        }
    };
    public final HLEModuleFunction __sceSasGetGrainFunction = new HLEModuleFunction("sceSasCore", "__sceSasGetGrain") {

        @Override
        public final void execute(Processor processor) {
            __sceSasGetGrain(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasGetGrain(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetSimpleADSRFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetSimpleADSR") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetSimpleADSR(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetSimpleADSR(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetGrainFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetGrain") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetGrain(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetGrain(processor);";
        }
    };
    public final HLEModuleFunction __sceSasRevEVOLFunction = new HLEModuleFunction("sceSasCore", "__sceSasRevEVOL") {

        @Override
        public final void execute(Processor processor) {
            __sceSasRevEVOL(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasRevEVOL(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetSteepWaveFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetSteepWave") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetSteepWave(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetSteepWave(processor);";
        }
    };
    public final HLEModuleFunction __sceSasGetOutputmodeFunction = new HLEModuleFunction("sceSasCore", "__sceSasGetOutputmode") {

        @Override
        public final void execute(Processor processor) {
            __sceSasGetOutputmode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasGetOutputmode(processor);";
        }
    };
    public final HLEModuleFunction __sceSasSetOutputmodeFunction = new HLEModuleFunction("sceSasCore", "__sceSasSetOutputmode") {

        @Override
        public final void execute(Processor processor) {
            __sceSasSetOutputmode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasSetOutputmode(processor);";
        }
    };
    public final HLEModuleFunction __sceSasRevVONFunction = new HLEModuleFunction("sceSasCore", "__sceSasRevVON") {

        @Override
        public final void execute(Processor processor) {
            __sceSasRevVON(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasRevVON(processor);";
        }
    };
    public final HLEModuleFunction __sceSasGetAllEnvelopeHeightsFunction = new HLEModuleFunction("sceSasCore", "__sceSasGetAllEnvelopeHeights") {

        @Override
        public final void execute(Processor processor) {
            __sceSasGetAllEnvelopeHeights(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSasCoreModule.__sceSasGetAllEnvelopeHeights(processor);";
        }
    };
}