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

import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState;

public class sceSasCore implements HLEModule {

    @Override
    public String getName() {
        return "sceSasCore";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(__sceSasSetADSRFunction, 0x019B25EB);
            mm.addFunction(__sceSasRevParamFunction, 0x267A6DD2);
            mm.addFunction(__sceSasGetPauseFlagFunction, 0x2C8E6AB3);
            mm.addFunction(__sceSasRevTypeFunction, 0x33D4AB37);
            mm.addFunction(__sceSasInitFunction, 0x42778A9F);
            mm.addFunction(__sceSasSetVolumeFunction, 0x440CA7D8);
            mm.addFunction(__sceSasCoreWithMixFunction, 0x50A14DFC);
            mm.addFunction(__sceSasSetSLFunction, 0x5F9529F6);
            mm.addFunction(__sceSasGetEndFlagFunction, 0x68A46B95);
            mm.addFunction(__sceSasGetEnvelopeHeightFunction, 0x74AE582A);
            mm.addFunction(__sceSasSetKeyOnFunction, 0x76F01ACA);
            mm.addFunction(__sceSasSetPauseFunction, 0x787D04D5);
            mm.addFunction(__sceSasSetVoiceFunction, 0x99944089);
            mm.addFunction(__sceSasSetADSRmodeFunction, 0x9EC3676A);
            mm.addFunction(__sceSasSetKeyOffFunction, 0xA0CF2FA4);
            mm.addFunction(__sceSasSetTrianglarWaveFunction, 0xA232CBE6);
            mm.addFunction(__sceSasCoreFunction, 0xA3589D81);
            mm.addFunction(__sceSasSetPitchFunction, 0xAD84D37F);
            mm.addFunction(__sceSasSetNoiseFunction, 0xB7660A23);
            mm.addFunction(__sceSasGetGrainFunction, 0xBD11B7C2);
            mm.addFunction(__sceSasSetSimpleADSRFunction, 0xCBCD4F79);
            mm.addFunction(__sceSasSetGrainFunction, 0xD1E0A01E);
            mm.addFunction(__sceSasRevEVOLFunction, 0xD5A229C9);
            mm.addFunction(__sceSasSetSteepWaveFunction, 0xD5EBBBCD);
            mm.addFunction(__sceSasGetOutputmodeFunction, 0xE175EF66);
            mm.addFunction(__sceSasSetOutputmodeFunction, 0xE855BF76);
            mm.addFunction(__sceSasRevVONFunction, 0xF983B186);
        }

        sasCoreUid = -1;
        sampleRate = 48000;
        voices = new pspVoice[32];
        for (int i = 0; i < voices.length; i++) {
        	voices[i] = new pspVoice();
        }

        if (voicesCheckerThread == null) {
	        voicesCheckerThread = new VoicesCheckerThread(500);
	        voicesCheckerThread.setDaemon(true);
	        voicesCheckerThread.setName("sceSasCore Voices Checker");
	        voicesCheckerThread.start();
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
    	public byte[] buffer;
    	public int bufferIndex;

    	public pspVoice() {
    		outputDataLine = null;
    		leftVolume  = 0x8000;
    		rightVolume = 0x8000;
    		samples = null;
    		loopMode = 0;
    		pitch = NORMAL_PITCH;
    		buffer = null;
    		bufferIndex = 0;
    	}

    	private float getSampleRate() {
    		return (sampleRate * pitch) / (float) NORMAL_PITCH;
    	}

    	private void init() {
    		float wantedSampleRate = getSampleRate();
    		int wantedBufferSize = 0;
    		if (samples != null) {
    			wantedBufferSize = samples.length * 4;
    		} else {
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
            		if (!audioMuted) {
	            		if (wantedBufferSize > 0) {
	            			outputDataLine.open(format, wantedBufferSize);
	            		} else {
	            			outputDataLine.open(format);
	            		}
            		}
    			} catch (LineUnavailableException e) {
    				Modules.log.error("sceSasCore.pspVoice.init: " + e.toString());
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

            return 0;	// TODO Check the return value
    	}

    	public synchronized int off() {
    		if (outputDataLine != null) {
    			outputDataLine.stop();
    		}

    		return 0;	// TODO Check the return value
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

    	private byte[] encodeSamples() {
        	int numSamples = samples.length;
            byte[] buffer = new byte[numSamples * 4];
            for (int i = 0; i < numSamples; i++) {
            	short sample = samples[i];
            	short lval = (short) ((sample * leftVolume ) >> 16);
            	short rval = (short) ((sample * rightVolume) >> 16);
            	buffer[i*4+0] = (byte) (lval);
            	buffer[i*4+1] = (byte) (lval >> 8);
            	buffer[i*4+2] = (byte) (rval);
            	buffer[i*4+3] = (byte) (rval >> 8);
            }

            return buffer;
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

    public void setAudioMuted(boolean muted) {
    	audioMuted = muted;
    }

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
    		} else {
    	        Modules.log.warn(functionName + " bad sasCoreUid 0x" + Integer.toHexString(Processor.memory.read32(sasCore)));
    		}
    	} else {
	        Modules.log.warn(functionName + " bad sasCore Address 0x" + Integer.toHexString(sasCore));
    	}
        cpu.gpr[2] = -1;

        return false;
    }

    protected boolean isVoiceNumberGood(int voice, String functionName, CpuState cpu) {
    	if (voice >= 0 && voice < voices.length) {
    		return true;
    	}

        Modules.log.warn(functionName + " bad voice number " + voice);
        cpu.gpr[2] = -1;
        return false;
    }

    public void __sceSasSetADSR(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        //int unk2 = cpu.gpr[6]; // 8, 0xf
        //int unk3 = cpu.gpr[7]; // 0, 0x40000000
        //int unk4 = cpu.gpr[8]; // 0x64
        //int unk5 = cpu.gpr[9]; // 0x64
        //int unk6 = cpu.gpr[10]; // 0x10000000

        Modules.log.warn("Unimplemented NID function __sceSasSetADSR [0x019B25EB] "
            + String.format("sasCore=%08x, voice=%d %08x %08x %08x %08x %08x",
            sasCore, voice, cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9], cpu.gpr[10]));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasRevParam(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        int sasCore = cpu.gpr[4];
        int unk1 = cpu.gpr[5]; // 0, 64
        int unk2 = cpu.gpr[6]; // 0, 64
        // 99% sure there are no more parameters

        Modules.log.warn("IGNORING:__sceSasRevParam("
            + String.format("sasCore=0x%08x,unk1=0x%x,unk2=0x%x)", sasCore, unk1, unk2));

        cpu.gpr[2] = 0;
    }

    // we could do some trickery in here too
    // 2C8E6AB3
    public void __sceSasGetPauseFlag(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        //int unk1 = cpu.gpr[5]; // set to 1, if this matches __sceSasSetPause then it's a voice bitfield
        //int unk2 = cpu.gpr[6]; // looks like a heap address, but so far 0x10000 aligned
        // 99% sure there are no more parameters
        // probably matches __sceSasGetEndFlag

        Modules.log.debug("IGNORING:__sceSasGetPauseFlag(sasCore=0x" + Integer.toHexString(sasCore) + ") " + makeLogParams(cpu));

        if (isSasHandleGood(sasCore, "__sceSasGetPauseFlag", cpu)) {
            // Fake all voices NOT paused
            cpu.gpr[2] = 0x0;
        }
    }

    public void __sceSasRevType(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        int sasCore = cpu.gpr[4];

        // -1 = any?
        // 0 = ?
        // 1 = ?
        // 2 = ?
        // 3 = ?
        // 4 = ?
        // 5 = ?
        // 6 = ?
        int type = cpu.gpr[5];
        //int unk2 = cpu.gpr[6]; // unused or 1 or the return code from some other function (0xdeadc0de)
        //int unk3 = cpu.gpr[7]; // unused or 0, 1, 0x1000

        Modules.log.warn("IGNORING:__sceSasRevType(type=" + type + ") " + makeLogParams(cpu));

        cpu.gpr[2] = 0;
    }

    public void __sceSasInit(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        // int unk1 = cpu.gpr[5]; // 0x00000100
        // int unk2 = cpu.gpr[6]; // 0x00000020
        // int unk3 = cpu.gpr[7]; // 0x00000000
        int sampleRate = cpu.gpr[8]; // 0x0000AC44 (44100 Hz)
        // 100% sure there are no more parameters

        Modules.log.info("PARTIAL __sceSasInit: "
                + String.format("sasCore=0x%08x, unk1=0x%08x, unk2=0x%08x, unk3=0x%08x, sampleRate=%d",
                sasCore, cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], sampleRate));

        // we'll support only 1 sascore instance at a time, we can fix this later if needed
        if (sasCoreUid != -1) {
            Modules.log.warn("UNIMPLEMENTED:__sceSasInit multiple instances not yet supported");
            cpu.gpr[2] = -1;
        } else {
            if (mem.isAddressGood(sasCore)) {
                sasCoreUid = SceUidManager.getNewUid("sceMpeg-Mpeg");
                mem.write32(sasCore, sasCoreUid);
            }

            this.sampleRate = sampleRate;
            cpu.gpr[2] = 0;
        }
    }

    public void __sceSasSetVolume(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int leftVolume = cpu.gpr[6]; // left channel volume 0 - 0x1000
        int rightVolume = cpu.gpr[7]; // right channel volume 0 - 0x1000

        if (isSasHandleGood(sasCore, "__sceSasSetVolume", cpu) && isVoiceNumberGood(voice, "__sceSasSetVolume", cpu)) {
	        voices[voice].leftVolume  = leftVolume  << 3;	// 0 - 0x8000
	        voices[voice].rightVolume = rightVolume << 3;	// 0 - 0x8000

	        cpu.gpr[2] = 0;
        }
    }

    // 50A14DFC
    public void __sceSasCoreWithMix(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        //int unk1 = cpu.gpr[5]; // looks like a heap address
        //int unk2 = cpu.gpr[6]; // looks like a bitfield

        Modules.log.debug("IGNORING:__sceSasCoreWithMix " + makeLogParams(cpu));

        if (isSasHandleGood(sasCore, "__sceSasCoreWithMix", cpu)) {
            // nothing to do ... ?
            cpu.gpr[2] = 0;
        }
    }

    public void __sceSasSetSL(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function __sceSasSetSL [0x5F9529F6] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    // 68A46B95
    public void __sceSasGetEndFlag(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int sasCore = cpu.gpr[4];
        // 99% sure there are no more parameters

        if (isSasHandleGood(sasCore, "__sceSasGetEndFlag", cpu)) {
        	// Returns a 32 bits bitfield (tested using "if (result & (1 << n))")
        	int endFlag = 0;
        	for (int i = 0; i < voices.length; i++) {
        		if (voices[i].IsEnded()) {
        			endFlag |= (1 << i);
        		}
        	}
            Modules.log.info("__sceSasGetEndFlag(sasCore=0x" + Integer.toHexString(sasCore) + "): 0x" + Integer.toHexString(endFlag));
            cpu.gpr[2] = endFlag;
        }
    }

    public void __sceSasGetEnvelopeHeight(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int unk1 = cpu.gpr[6]; // set to 1
        // 99% sure there are no more parameters

        Modules.log.warn("IGNORING:__sceSasGetEnvelopeHeight(sasCore=0x" + Integer.toHexString(sasCore) + ",voice=" + voice + ",unk1=0x" + Integer.toHexString(unk1) + ")");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    // I think this means start playing this channel, key off would then mean stop or pause the playback (fiveofhearts)
    public void __sceSasSetKeyOn(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];

        Modules.log.info("PARTIAL __sceSasSetKeyOn: "
            + String.format("sasCore=%08x, voice=%d",
            sasCore, voice));

        if (isSasHandleGood(sasCore, "__sceSasSetKeyOn", cpu) && isVoiceNumberGood(voice, "__sceSasSetKeyOn", cpu)) {
        	cpu.gpr[2] = voices[voice].on();
        }
    }

    public void __sceSasSetPause(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        int sasCore = cpu.gpr[4];
        int voice_bit = cpu.gpr[5]; // bitfield instead of index, example: 0x08 is voice 3
        int pause = cpu.gpr[6]; // 0

        Modules.log.warn("Unimplemented NID function __sceSasSetPause [0x787D04D5] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;
    }

    protected short[] decodeSamples(Processor processor, int vagAddr, int size) {
    	Memory mem = Processor.memory;

    	// Based on vgmstream
        short[] samples = new short[size / 16 * 28];
        int numSamples = 0;

        int headerCheck = mem.read32(vagAddr);
        if ((headerCheck & 0x00FFFFFF) == 0x00474156)	{ // VAGx
        	vagAddr += 0x30;	// Skip the VAG header
        }

        int[] unpackedSamples = new int[28];
        int hist1 = 0;
        int hist2 = 0;
        final double[][] VAG_f = {
        		{   0.0       ,   0.0 },
        		{  60.0 / 64.0,   0.0 },
        		{ 115.0 / 64.0, -52.0 / 64.0 },
        		{  98.0 / 64.0, -55.0 / 64.0 },
        		{ 122.0 / 64.0, -60.0 / 64.0 }
        		};
        for (int i = 0; i <= (size - 16); ) {
        	int n = mem.read8(vagAddr + i);
        	i++;
        	int predict_nr = n >> 4;
        	int shift_factor = n & 0x0F;
        	int flag = mem.read8(vagAddr + i);
        	i++;
        	if (flag == 0x07) {
        		break;	// End of stream flag
        	}
        	for (int j = 0; j < 28; j += 2) {
        		int d = mem.read8(vagAddr + i);
        		i++;
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
        	for (int i = 0; i < numSamples; i++) {
        		resizedSamples[i] = samples[i];
        	}
        	samples = resizedSamples;
        }

        return samples;
    }

    public void __sceSasSetVoice(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int vagAddr = cpu.gpr[6]; // may have uncached bit set
        int size = cpu.gpr[7];
        int loopmode = cpu.gpr[8];

        Modules.log.info("PARTIAL __sceSasSetVoice: "
            + String.format("sasCore=0x%08x, voice=%d, vagAddr=0x%08x, size=0x%08x, loopmode=%d",
            sasCore, voice, vagAddr, size, loopmode));

        if (isSasHandleGood(sasCore, "__sceSasSetVoice", cpu) && isVoiceNumberGood(voice, "__sceSasSetVoice", cpu)) {
            voices[voice].samples = decodeSamples(processor, vagAddr, size);
            voices[voice].loopMode = loopmode;

            cpu.gpr[2] = 0;
        }

    }

    public void __sceSasSetADSRmode(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        //int unk2 = cpu.gpr[6]; // 8/0xf
        //int unk3 = cpu.gpr[7]; // 0
        // may be more parameters

        Modules.log.warn("Unimplemented NID function __sceSasSetADSRmode [0x9EC3676A] "
            + String.format("%08x %08x %08x %08x %08x %08x",
            cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasSetKeyOff(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];

        Modules.log.info("PARTIAL __sceSasSetKeyOff: "
                + String.format("sasCore=%08x, voice=%d",
                sasCore, voice));

        if (isSasHandleGood(sasCore, "__sceSasSetKeyOff", cpu) && isVoiceNumberGood(voice, "__sceSasSetKeyOff", cpu)) {
        	cpu.gpr[2] = voices[voice].off();
        }
    }

    public void __sceSasSetTrianglarWave(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function __sceSasSetTrianglarWave [0xA232CBE6] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    // A3589D81
    public void __sceSasCore(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int sasCore = cpu.gpr[4];
        //int unk1 = cpu.gpr[5]; // looks like a heap address, bss, 0x40 aligned
        // 99% sure there are no more parameters

        Modules.log.debug("IGNORING:__sceSasCore " + makeLogParams(cpu));

        if (isSasHandleGood(sasCore, "__sceSasCore", cpu)) {
            // noxa/pspplayer blocks in __sceSasCore
            // some games protect __sceSasCore with locks, suggesting it may context switch.
        	// Games seems to run better when delaying the thread instead of just yielding.
        	ThreadMan.getInstance().hleKernelDelayThread(1000000, false);
        	// ThreadMan.getInstance().yieldCurrentThread();
            cpu.gpr[2] = 0;
        }
    }

    public void __sceSasSetPitch(Processor processor) {
        CpuState cpu = processor.cpu;

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        int pitch = cpu.gpr[6]; // 0x6e4/0x800/0x1000/0x2000 large values may be clamped

        Modules.log.info("PARTIAL __sceSasSetPitch: "
            + String.format("sasCore=%08x, voice=%d, pitch=0x%04x",
            sasCore, voice, pitch));

        if (isSasHandleGood(sasCore, "__sceSasSetPitch", cpu) && isVoiceNumberGood(voice, "__sceSasSetPitch", cpu)) {
        	voices[voice].pitch = pitch;
        	cpu.gpr[2] = 0;
        }
    }

    public void __sceSasSetNoise(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function __sceSasSetNoise [0xB7660A23] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasGetGrain(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function __sceSasGetGrain [0xBD11B7C2] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasSetSimpleADSR(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        int sasCore = cpu.gpr[4];
        int voice = cpu.gpr[5];
        //int unk1 = cpu.gpr[6]; // 0xff
        //int unk2 = cpu.gpr[7]; // 0x1fc6
        // doesn't look like any more parameters, they look like error codes

        Modules.log.warn("Unimplemented NID function __sceSasSetSimpleADSR [0xCBCD4F79] "
            + String.format("%08x %08x %08x %08x %08x %08x",
            cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasSetGrain(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function __sceSasSetGrain [0xD1E0A01E] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasRevEVOL(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int sasCore = cpu.gpr[4];
        int left = cpu.gpr[5]; // left channel volume 0 - 0x1000
        int right = cpu.gpr[6]; // right channel volume 0 - 0x1000

        Modules.log.debug("IGNORING:__sceSasRevEVOL("
            + String.format("sasCore=0x%08x,left=0x%04x,right=0x%04x)", sasCore, left, right));

        cpu.gpr[2] = 0;
    }

    public void __sceSasSetSteepWave(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function __sceSasSetSteepWave [0xD5EBBBCD] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasGetOutputmode(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        int sasCore = cpu.gpr[4];
        //int unk1 = cpu.gpr[5]; // 0, 1 (most common), 0x1f
        // 99% sure there are no more parameters

        Modules.log.warn("Unimplemented NID function __sceSasGetOutputmode [0xE175EF66] " + makeLogParams(cpu));

        // beq t0 (t0=1)
        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasSetOutputmode(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function __sceSasSetOutputmode [0xE855BF76] " + makeLogParams(cpu));

        cpu.gpr[2] = 0xDEADC0DE;

    // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result >>> 32); cpu.fpr[0] = result;
    }

    public void __sceSasRevVON(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor

        int sasCore = cpu.gpr[4];
        int unk1 = cpu.gpr[5]; // set to 1
        int unk2 = cpu.gpr[6]; // 0 or 1
        // 99% sure there are no more parameters

        Modules.log.debug("IGNORING:__sceSasRevVON("
            + String.format("sasCore=0x%08x,unk1=0x%x,unk2=0x%x)", sasCore, unk1, unk2));

        cpu.gpr[2] = 0;
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
};
