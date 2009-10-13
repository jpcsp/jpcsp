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

import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEThread;

public class sceAudio implements HLEModule, HLEThread {
    protected class pspChannelInfo {
    	public int index;
		public boolean reserved;
        public int allocatedSamples;
        public int format;
        public int leftVolume;
        public int rightVolume;
        public SourceDataLine outputDataLine;
    	public float outputDataLineSampleRate;
        public int waitingThreadId;
        public long referenceFramePosition;
        public int waitingAudioDataAddr;
        public int waitingVolumeLeft;
        public int waitingVolumeRight;
    	public byte[] buffer;
    	public int bufferIndex;
    	public List<byte[]> waitingBuffers;

        public pspChannelInfo(int index)
        {
        	this.index = index;
            reserved = false;
            allocatedSamples = 0;
            format = 0;
            leftVolume = 0x8000;
            rightVolume = 0x8000;
            outputDataLine = null;
            waitingThreadId = -1;
            referenceFramePosition = 0;
    		buffer = null;
    		bufferIndex = 0;
    		waitingBuffers = new LinkedList<byte[]>();
        }

    	public boolean IsEnded() {
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

    	private float getSampleRate() {
    		return sampleRate;
    	}

    	private void init() {
    		float wantedSampleRate = getSampleRate();
    		int wantedBufferSize = 0;
    		if (outputDataLine == null) {
            	//
            	// The PSP is using a buffer equal to the sampleSize.
            	// However, the Java sound system is not nicely handling buffer
            	// underflows, causing discontinuities in the audio
            	// that are perceived as "clicks".
            	//
            	// So, allocate a large buffer: 10 times the sampleSize is an
            	// empirical value.
            	// This has the disadvantage to introduce a small delay when playing
            	// a new sound: a PSP application is typically sending continuously
            	// sound data, even when nothing can be heard ("0" values are sent).
            	// And we have first to play these buffered blanks before hearing
            	// the real sound itself.
    			//
    			wantedBufferSize = allocatedSamples * 4 * 10;
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

    	public synchronized void release() {
            if (outputDataLine != null)
            {
                outputDataLine.stop();
                outputDataLine.close();
                outputDataLine = null;
            }
            waitingBuffers.clear();
            buffer = null;
            waitingAudioDataAddr = 0;
    	}

    	public synchronized void play(byte[] buffer) {
    		init();
    		if (outputDataLine != null) {
    			if (this.buffer == null) {
                	int length = Math.min(outputDataLine.available(), buffer.length);
                	outputDataLine.start();
                	int written = outputDataLine.write(buffer, 0, length);
                	if (Modules.log.isDebugEnabled()) {
                		Modules.log.debug("pspChannelInfo.play: written " + written + " bytes (" + toString() + ")");
                	}
                	bufferIndex = length;
    				this.buffer = buffer;
    			} else {
    				waitingBuffers.add(buffer);
    			}
        		check();
    		}
    	}

    	public synchronized boolean isOutputBlocking() {
    		init();
    		check();

    		return !waitingBuffers.isEmpty();
    	}

    	public synchronized void check() {
    		if (outputDataLine == null || buffer == null) {
    			return;
    		}

    		if (bufferIndex < buffer.length) {
    			int length = Math.min(outputDataLine.available(), buffer.length - bufferIndex);
    			if (length > 0) {
    				outputDataLine.start();
    				int written = outputDataLine.write(buffer, bufferIndex, length);
                	if (Modules.log.isDebugEnabled()) {
                		Modules.log.debug("pspChannelInfo.check: written " + written);
                	}
    				bufferIndex += length;
    			}
    		}

    		if (bufferIndex >= buffer.length) {
    			if (!waitingBuffers.isEmpty()) {
    				buffer = waitingBuffers.remove(0);
    				bufferIndex = 0;
    				check();
    			} else {
    				if (IsEnded()) {
        				buffer = null;
    					outputDataLine.stop();
    				}
    			}
    		}
    	}

        @Override
		public String toString() {
        	StringBuffer result = new StringBuffer();

        	result.append("pspChannelInfo[" + index + "](");
        	if (buffer != null) {
        		result.append("buffer " + bufferIndex + " of " + buffer.length + ", ");
        	}
        	if (outputDataLine != null) {
        		result.append("playing " + outputDataLine.available() + " of " + outputDataLine.getBufferSize() + " ,");
        	}
        	result.append("Waiting " + waitingBuffers.size() + ", ");
        	result.append("Ended " + IsEnded()+ ", ");
        	result.append("OutputBlocking " + isOutputBlocking() + ", ");
        	result.append("Reserved " + reserved);
        	result.append(")");

        	return result.toString();
        }
    }

    protected class ChannelsCheckerThread extends Thread {
    	private long delayMillis;

    	public ChannelsCheckerThread(long delayMillis) {
    		this.delayMillis = delayMillis;
    	}

    	@Override
		public void run() {
			while (true) {
				for (int i = 0; i < pspchannels.length; i++) {
					pspchannels[i].check();
				}

				try {
					sleep(delayMillis);
				} catch (InterruptedException e) {
					// Ignore the Exception
				}
			}
		}
    }

    protected static final int PSP_AUDIO_VOLUME_MAX = 0x8000;
    protected static final int PSP_AUDIO_CHANNEL_MAX = 8;
    protected static final int PSP_AUDIO_NEXT_CHANNEL = (-1);
    protected static final int PSP_AUDIO_SAMPLE_MIN = 64;
    protected static final int PSP_AUDIO_SAMPLE_MAX = 65472;

    protected static final int PSP_AUDIO_FORMAT_STEREO = 0;
    protected static final int PSP_AUDIO_FORMAT_MONO = 0x10;

    protected static final int PSP_AUDIO_FREQ_44K = 44100;
    protected static final int PSP_AUDIO_FREQ_48K = 48000;

    private static ChannelsCheckerThread channelsCheckerThread = null;

    protected pspChannelInfo[] pspchannels; // psp channels
    protected int sampleRate;

    protected boolean output2 = false;

    @Override
    public String getName() { return "sceAudio"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) {

        if (version >= 150) {

            mm.addFunction(sceAudioOutputFunction, 0x8C1009B2);
            mm.addFunction(sceAudioOutputBlockingFunction, 0x136CAF51);
            mm.addFunction(sceAudioOutputPannedFunction, 0xE2D56B2D);
            mm.addFunction(sceAudioOutputPannedBlockingFunction, 0x13F592BC);
            mm.addFunction(sceAudioChReserveFunction, 0x5EC81C55);
            mm.addFunction(sceAudioOneshotOutputFunction, 0x41EFADE7);
            mm.addFunction(sceAudioChReleaseFunction, 0x6FC46853);
            mm.addFunction(sceAudioGetChannelRestLengthFunction, 0xB011922F);
            mm.addFunction(sceAudioSetChannelDataLenFunction, 0xCB2E439E);
            mm.addFunction(sceAudioChangeChannelConfigFunction, 0x95FD0C2D);
            mm.addFunction(sceAudioChangeChannelVolumeFunction, 0xB7E1D8E7);
            mm.addFunction(sceAudioOutput2ReserveFunction, 0x01562BA3);
            mm.addFunction(sceAudioOutput2ReleaseFunction, 0x43196845);
            mm.addFunction(sceAudioOutput2OutputBlockingFunction, 0x2D53F36E);
            mm.addFunction(sceAudioOutput2GetRestSampleFunction, 0x647CEF33);
            mm.addFunction(sceAudioOutput2ChangeLengthFunction, 0x63F2889C);
            mm.addFunction(sceAudioSRCChReserveFunction, 0x38553111);
            mm.addFunction(sceAudioSRCChReleaseFunction, 0x5C37C0AE);
            mm.addFunction(sceAudioSRCOutputBlockingFunction, 0xE0727056);
            mm.addFunction(sceAudioInputBlockingFunction, 0x086E5895);
            mm.addFunction(sceAudioInputFunction, 0x6D4BEC68);
            mm.addFunction(sceAudioGetInputLengthFunction, 0xA708C6A6);
            mm.addFunction(sceAudioWaitInputEndFunction, 0x87B2E651);
            mm.addFunction(sceAudioInputInitFunction, 0x7DE61688);
            mm.addFunction(sceAudioInputInitExFunction, 0xE926D3FB);
            mm.addFunction(sceAudioPollInputEndFunction, 0xA633048E);
            mm.addFunction(sceAudioGetChannelRestLenFunction, 0xE9D97901);

            /* From sceAudio_driver */
            mm.addFunction(sceAudioInitFunction, 0x80F1F7E0);
            mm.addFunction(sceAudioEndFunction, 0x210567F7);
            mm.addFunction(sceAudioSetFrequencyFunction, 0xA2BEAA6C);
            mm.addFunction(sceAudioLoopbackTestFunction, 0xB61595C0);
            mm.addFunction(sceAudioSetVolumeOffsetFunction, 0x927AC32B);

            // unfortunately we get here before setBlockingEnabled is called
            //if (!disableBlockingAudio)
                mm.addThread(this);
        }

        pspchannels = new pspChannelInfo[8];
        for (int channel = 0; channel < pspchannels.length; channel++)
        {
            pspchannels[channel] = new pspChannelInfo(channel);
        }

        sampleRate = 48000;

        if (channelsCheckerThread == null) {
	        channelsCheckerThread = new ChannelsCheckerThread(500);
	        channelsCheckerThread.setDaemon(true);
	        channelsCheckerThread.setName("sceAudio Channels Checker");
	        channelsCheckerThread.start();
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {
            // unfortunately addThread is called before setBlockingEnabled
            //if (!disableBlockingAudio)
                mm.removeThread(this);

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

            /* From sceAudio_driver */
            mm.removeFunction(sceAudioInitFunction);
            mm.removeFunction(sceAudioEndFunction);
            mm.removeFunction(sceAudioSetFrequencyFunction);
            mm.removeFunction(sceAudioLoopbackTestFunction);
            mm.removeFunction(sceAudioSetVolumeOffsetFunction);
        }
    }

    protected boolean disableChReserve;
    protected boolean disableBlockingAudio;
    protected boolean audioMuted;

    public void setChReserveEnabled(boolean enabled) {
        disableChReserve = !enabled;
        Modules.log.info("Audio ChReserve disabled: " + disableChReserve);
    }

    public void setBlockingEnabled(boolean enabled) {
        disableBlockingAudio = !enabled;
        Modules.log.info("Audio Blocking disabled: " + disableBlockingAudio);
    }

    public void setAudioMuted(boolean muted) {
    	audioMuted = muted;
    	Modules.log.info("Audio muted: " + audioMuted);
    }

    @Override
    public void step() {
        if (disableBlockingAudio)
            return;

        for (int channel = 0; channel < pspchannels.length; channel++) {
            if (pspchannels[channel].waitingThreadId >= 0) {
                if (!pspchannels[channel].isOutputBlocking()) {
                    //Modules.log.info("Audio step - unblocking thread");
                	ThreadMan threadMan = ThreadMan.getInstance();
                	int waitingThreadId = pspchannels[channel].waitingThreadId;
                	SceKernelThreadInfo waitingThread = threadMan.getThreadById(waitingThreadId);
                	if (waitingThread != null) {
	                	if (pspchannels[channel].waitingAudioDataAddr != 0) {
	        	            sceAudioChangeChannelVolume(channel, pspchannels[channel].waitingVolumeLeft, pspchannels[channel].waitingVolumeRight);
	        	            int ret = doAudioOutput(channel, pspchannels[channel].waitingAudioDataAddr);
	        	            waitingThread.cpuContext.gpr[2] = ret;
	                	}
	                	threadMan.unblockThread(waitingThreadId);
                	}
                    pspchannels[channel].waitingThreadId = -1;
                    pspchannels[channel].waitingAudioDataAddr = 0;
                }
            }
        }
    }

    protected int doAudioOutput(int channel, int pvoid_buf)
    {
        int ret = -1;

        if(pspchannels[channel].reserved)
        {
            int channels = ((pspchannels[channel].format&0x10)==0x10)?1:2;
            int bytespersample = 4; //2*channels;

            int bytes = bytespersample * pspchannels[channel].allocatedSamples;

            byte[] data = new byte[bytes];

            // process audio volumes ourselves for now
            int nsamples = pspchannels[channel].allocatedSamples;
            int leftVolume = pspchannels[channel].leftVolume;
            int rightVolume = pspchannels[channel].rightVolume;
            Memory mem = Memory.getInstance();
            if(channels == 1)
            {
                for(int i=0;i<nsamples;i++)
                {
                    short lval = (short)mem.read16(pvoid_buf+i*2);
                    short rval = lval;

                    lval = (short)((((int)lval) * leftVolume ) >> 16);
                    rval = (short)((((int)rval) * rightVolume) >> 16);

                    data[i*4+0] = (byte)(lval);
                    data[i*4+1] = (byte)(lval>>8);
                    data[i*4+2] = (byte)(rval);
                    data[i*4+3] = (byte)(rval>>8);
                }
            }
            else
            {
                for(int i=0;i<nsamples;i++)
                {
                    short lval = (short)mem.read16(pvoid_buf+i*4);
                    short rval = (short)mem.read16(pvoid_buf+i*4+2);

                    lval = (short)((((int)lval) * leftVolume ) >> 16);
                    rval = (short)((((int)rval) * rightVolume) >> 16);

                    data[i*4+0] = (byte)(lval);
                    data[i*4+1] = (byte)(lval>>8);
                    data[i*4+2] = (byte)(rval);
                    data[i*4+3] = (byte)(rval>>8);
                }
            }

            pspchannels[channel].referenceFramePosition = pspchannels[channel].outputDataLine.getLongFramePosition();
            pspchannels[channel].play(data);

            ret = nsamples;
        } else {
        	Modules.log.warn("sceAudio.doAudioOutput: channel " + channel + " not reserved");
        }

        return ret;
    }

    protected int doAudioFlush(int channel)
    {
        if(pspchannels[channel].outputDataLine != null)
        {
            pspchannels[channel].outputDataLine.drain();
            return 0;
        }
        return -1;
    }

    private int sceAudioChangeChannelVolume(int channel, int leftvol, int rightvol)
    {
        int ret = -1;

        if(pspchannels[channel].reserved)
        {
            /* doing the audio processing myself on doAudioOutput for now

            if(pspchannels[channel].outputDataLine != null)
            {

                FloatControl cvolume  = (FloatControl)pspchannels[channel].outputDataLine.getControl(FloatControl.Type.VOLUME);
                FloatControl cpanning;

                if((pspchannels[channel].format&0x10)==0x10)
                     cpanning = (FloatControl)pspchannels[channel].outputDataLine.getControl(FloatControl.Type.PAN);
                else
                     cpanning = (FloatControl)pspchannels[channel].outputDataLine.getControl(FloatControl.Type.BALANCE);

                float vvolume  = Math.max(leftvol,rightvol);
                float balance = (rightvol-leftvol)/vvolume;

                cvolume.setValue(vvolume/32768.0f);
                cpanning.setValue(balance);

                ret = 0;
            }
            else*/
            {
                pspchannels[channel].leftVolume = leftvol;
                pspchannels[channel].rightVolume = rightvol;
                ret = 0;
            }
        }
        return ret;
    }

    public void sceAudioInit(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioInit [0x80F1F7E0]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceAudioEnd(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioEnd [0x210567F7]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceAudioSetFrequency(Processor processor) {
        CpuState cpu = processor.cpu;

        int frequency = cpu.gpr[4];

        int ret = -1;
        if(frequency == 44100 || frequency == 48000)
        {
            sampleRate = frequency;
            ret = 0;
        }
        cpu.gpr[2] = ret;
    }

    public void sceAudioLoopbackTest(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioLoopbackTest [0xB61595C0]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceAudioSetVolumeOffset(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioSetVolumeOffset [0x927AC32B]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceAudioOutput(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int channel = cpu.gpr[4], vol = cpu.gpr[5], pvoid_buf = cpu.gpr[6];

        if (!mem.isAddressGood(pvoid_buf)) {
            Modules.log.warn("sceAudioOutput bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = -1;
        } else {
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(String.format("sceAudioOutput 0x%08X", pvoid_buf));
            }

        	if (!pspchannels[channel].isOutputBlocking()) {
                sceAudioChangeChannelVolume(channel, vol, vol);
                cpu.gpr[2] = doAudioOutput(channel, pvoid_buf);
        	} else {
        		cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
        	}
        }
    }

    public void sceAudioOutputBlocking(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int channel = cpu.gpr[4], vol = cpu.gpr[5], pvoid_buf = cpu.gpr[6];

        if (!mem.isAddressGood(pvoid_buf)) {
            Modules.log.warn("sceAudioOutputBlocking bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = -1;
        } else {
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(String.format("sceAudioOutputBlocking 0x%08X", pvoid_buf));
            }

        	ThreadMan threadMan = ThreadMan.getInstance();
            if (!pspchannels[channel].isOutputBlocking() || disableBlockingAudio) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceAudioOutputBlocking[not blocking] " + pspchannels[channel].toString());
            	}
	            sceAudioChangeChannelVolume(channel, vol, vol);
	            cpu.gpr[2] = doAudioOutput(channel, pvoid_buf);
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceAudioOutputBlocking[not blocking] returning " + cpu.gpr[2] + " (" + pspchannels[channel].toString() + ")");
            	}
	            //threadMan.yieldCurrentThread();
            } else {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceAudioOutputBlocking[blocking] " + pspchannels[channel].toString());
            	}
	            pspchannels[channel].waitingThreadId = threadMan.getCurrentThreadID();
	            pspchannels[channel].waitingAudioDataAddr = pvoid_buf;
	            pspchannels[channel].waitingVolumeLeft  = vol;
	            pspchannels[channel].waitingVolumeRight = vol;
	            threadMan.blockCurrentThread();
            }
        }
    }

    public void sceAudioOutputPanned(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int channel = cpu.gpr[4], leftvol = cpu.gpr[5], rightvol = cpu.gpr[6], pvoid_buf = cpu.gpr[7];

        if (!mem.isAddressGood(pvoid_buf)) {
            Modules.log.warn("sceAudioOutputPanned bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = -1;
        } else {
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(String.format("sceAudioOutputPanned 0x%08X", pvoid_buf));
            }

        	if (!pspchannels[channel].isOutputBlocking()) {
                sceAudioChangeChannelVolume(channel, leftvol, rightvol);
                cpu.gpr[2] = doAudioOutput(channel, pvoid_buf);
        	} else {
        		cpu.gpr[2] = SceKernelErrors.ERROR_AUDIO_CHANNEL_BUSY;
        	}
        }
    }

    public void sceAudioOutputPannedBlocking(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int channel = cpu.gpr[4], leftvol = cpu.gpr[5], rightvol = cpu.gpr[6], pvoid_buf = cpu.gpr[7];

        if (!mem.isAddressGood(pvoid_buf)) {
            Modules.log.warn("sceAudioOutputPannedBlocking bad pointer " + String.format("0x%08X", pvoid_buf));
            cpu.gpr[2] = -1;
        } else {
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(String.format("sceAudioOutputPannedBlocking 0x%08X", pvoid_buf));
            }

        	ThreadMan threadMan = ThreadMan.getInstance();
            if (!pspchannels[channel].isOutputBlocking() || disableBlockingAudio) {
	            sceAudioChangeChannelVolume(channel, leftvol, rightvol);
	            cpu.gpr[2] = doAudioOutput(channel, pvoid_buf);
	            //threadMan.yieldCurrentThread();
            } else {
	            pspchannels[channel].waitingThreadId = threadMan.getCurrentThreadID();
	            pspchannels[channel].waitingAudioDataAddr = pvoid_buf;
	            pspchannels[channel].waitingVolumeLeft  = leftvol;
	            pspchannels[channel].waitingVolumeRight = rightvol;
	            threadMan.blockCurrentThread();
            }
        }
    }

    public void sceAudioChReserve(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4], samplecount = cpu.gpr[5], format = cpu.gpr[6];

        if (disableChReserve)
        {
            Modules.log.warn("IGNORED sceAudioChReserve channel= " + channel + " samplecount = " + samplecount + " format = " + format);
            cpu.gpr[2] = -1;
        }
        else
        {
            Modules.log.debug("sceAudioChReserve channel= " + channel + " samplecount = " + samplecount + " format = " + format);

            if (channel != -1) // use specified channel, if available
            {
                if (pspchannels[channel].reserved)
                {
                    Modules.log.warn("sceAudioChReserve failed - channel " + channel + " already in use");
                    channel = -1;
                }
            }
            else // find first free channel
            {
                for (int i = 0; i < pspchannels.length; i++)
                {
                    if (!pspchannels[i].reserved)
                    {
                        channel = i;
                        break;
                    }
                }

                if (channel == -1)
                    Modules.log.warn("sceAudioChReserve failed - no free channels available");
            }

            if (channel != -1) // if channel == -1 here, it means we couldn't use any.
            {
                pspchannels[channel].reserved = true;
                pspchannels[channel].outputDataLine = null; // delay creation until first use.
                pspchannels[channel].allocatedSamples = samplecount;
                pspchannels[channel].format = format;
            }

            cpu.gpr[2] = channel;
        }
    }

    public void sceAudioOneshotOutput(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioOneshotOutput [0x41EFADE7]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceAudioChRelease(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];
        int ret = -1;

        if(pspchannels[channel].reserved)
        {
        	pspchannels[channel].release();
            pspchannels[channel].reserved=false;
            ret = 0;
        }
        cpu.gpr[2] = ret;
    }

    public void sceAudioGetChannelRestLength(Processor processor) {
        sceAudioGetChannelRestLen(processor);
    }

    public void sceAudioSetChannelDataLen(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4], samplecount = cpu.gpr[5];

        pspchannels[channel].allocatedSamples = samplecount;
        cpu.gpr[2] = 0;
    }

    public void sceAudioChangeChannelConfig(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4], format = cpu.gpr[5];

        pspchannels[channel].format = format;
        cpu.gpr[2] = 0;
    }

    public void sceAudioChangeChannelVolume(Processor processor) {
        CpuState cpu = processor.cpu;

        cpu.gpr[2] = sceAudioChangeChannelVolume(cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]);
    }

    public void sceAudioOutput2Reserve(Processor processor) {
        output2 = true;
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

        int unplayed = 0;

        //Retrieve the unplayed samples' count only for the first channel.
        if(pspchannels[0].outputDataLine != null){
            unplayed = pspchannels[0].outputDataLine.getBufferSize() - pspchannels[0].outputDataLine.available();
        }

        cpu.gpr[2] = unplayed;
    }

    public void sceAudioOutput2ChangeLength(Processor processor) {
        CpuState cpu = processor.cpu;

        int samplecount = cpu.gpr[4];
        int ret = -1;

        for(int i = 0; i < pspchannels.length; i++) {
            if(pspchannels[i].reserved){
                pspchannels[i].allocatedSamples = samplecount;
                ret = 0;
            }
        }

        if(ret != 0){
            Modules.log.warn("sceAudioOutput2ChangeLength audio output is not reserved");
        }

        cpu.gpr[2] = ret;
    }

    public void sceAudioSRCChReserve(Processor processor) {
        CpuState cpu = processor.cpu;

        int samplecount = cpu.gpr[4], freq = cpu.gpr[5], channels = cpu.gpr[6];

        if(output2){
            freq = 44100;
            channels = 2;
        }

         if (disableChReserve)
         {
            Modules.log.warn("IGNORED sceAudioSRCChReserve channels count= " + channels + " samplecount = " + samplecount + " freq = " + freq);
            cpu.gpr[2] = -1;
         }

        //Reserve the specified number of channels (if free)
        //and set the sample count and frequency for each one.

        for (int i = 0; i < channels; i++) {
            if (!pspchannels[i].reserved){
                pspchannels[i].reserved = true;
            }
        }
        
        //Only setting the sample count for the first channel.
        pspchannels[0].allocatedSamples = samplecount;

        sampleRate = freq;

        output2 = false;

        cpu.gpr[2] = 0;
    }

    public void sceAudioSRCChRelease(Processor processor) {
       CpuState cpu = processor.cpu;

       for(int i = 0; i < pspchannels.length; i++) {
       if(pspchannels[i].reserved)
        {
        	pspchannels[i].release();
            pspchannels[i].reserved=false;
        }
       }

       cpu.gpr[2] = 0;
    }

    public void sceAudioSRCOutputBlocking(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;
        ThreadMan threadMan = ThreadMan.getInstance();

        int vol = cpu.gpr[4], buf = cpu.gpr[5];

        if (!mem.isAddressGood(buf)) {
            Modules.log.warn("sceAudioSRCOutputBlocking bad pointer " + String.format("0x%08X", buf));
            cpu.gpr[2] = -1;
        } else {
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(String.format("sceAudioSRCOutputBlocking 0x%08X", buf));
            }

            for(int i = 0; pspchannels[i].reserved; i++) {
            if (!pspchannels[i].isOutputBlocking() || disableBlockingAudio) {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceAudioSRCOutputBlocking[not blocking] " + pspchannels[i].toString());
            	}
	            sceAudioChangeChannelVolume(i, vol, vol);
	            doAudioOutput(i, buf);
                cpu.gpr[2] = 0;

            } else {
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceAudioSRCOutputBlocking[blocking] " + pspchannels[i].toString());
            	}
	            pspchannels[i].waitingThreadId = threadMan.getCurrentThreadID();
	            pspchannels[i].waitingAudioDataAddr = buf;
	            pspchannels[i].waitingVolumeLeft  = vol;
	            pspchannels[i].waitingVolumeRight = vol;
	            threadMan.blockCurrentThread();
            }
        }
        }

    }

    public void sceAudioInputBlocking(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioInputBlocking [0x086E5895]");

        cpu.gpr[2] = -1;
        ThreadMan.getInstance().yieldCurrentThread();
    }

    public void sceAudioInput(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioInput [0x6D4BEC68]");

        cpu.gpr[2] = -1;
    }

    public void sceAudioGetInputLength(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioGetInputLength [0xA708C6A6]");

        cpu.gpr[2] = -1;
    }

    public void sceAudioWaitInputEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioWaitInputEnd [0x87B2E651]");

        cpu.gpr[2] = -1;
    }

    public void sceAudioInputInit(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioInputInit [0x7DE61688]");

        cpu.gpr[2] = -1;
    }

    public void sceAudioInputInitEx(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioInputInitEx [0xE926D3FB]");

        cpu.gpr[2] = -1;
    }

    public void sceAudioPollInputEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        System.out.println("Unimplemented NID function sceAudioPollInputEnd [0xA633048E]");

        cpu.gpr[2] = -1;
    }

    protected int hleAudioGetChannelRestLen(int channel) {
        int len = 0;

        if (pspchannels[channel].outputDataLine != null)
        {
            /* the number of samples left to be consumed by the java sound library's own internal buffers
            int bytespersample = 4;

            if ((pspchannels[channel].format & 0x10) == 0x10)
                bytespersample = 2;

            len = pspchannels[channel].outputDataLine.available() / bytespersample;
            */

            // the number of samples left to play in real time, not the number left to be consumed
            // TODO this is really messed up with overflows and underflows but at least len eventually reaches 0 :)
            long framesPlayed = pspchannels[channel].outputDataLine.getLongFramePosition()
                - pspchannels[channel].referenceFramePosition;

            //if (framesPlayed < 0)
            //    Modules.log.debug("hleAudioGetChannelRestLen(channel=" + channel + ") framesPlayed=" + framesPlayed);

            len = pspchannels[channel].allocatedSamples - (int)framesPlayed;
            //if (len > 0 || pspchannels[channel].waitingThreadId >= 0) {
            //    Modules.log.debug("hleAudioGetChannelRestLen(channel=" + channel + ") len=" + len);
            //}

            if (len < 0) {
                //Modules.log.error("hleAudioGetChannelRestLen(channel=" + channel
                //    + ") clamping len=" + len + " to 0");

                len = 0;
            } else if (len > pspchannels[channel].allocatedSamples) {
                //Modules.log.error("hleAudioGetChannelRestLen(channel=" + channel
                //    + ") clamping len=" + len + " to " + pspchannels[channel].allocatedSamples);

                len = pspchannels[channel].allocatedSamples;
            }
        }

        return len;
    }

    public void sceAudioGetChannelRestLen(Processor processor) {
        CpuState cpu = processor.cpu;

        int channel = cpu.gpr[4];

        cpu.gpr[2] = hleAudioGetChannelRestLen(channel);
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

};
