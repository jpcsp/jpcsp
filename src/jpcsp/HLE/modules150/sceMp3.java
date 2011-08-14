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
import java.nio.ByteBuffer;
import java.util.HashMap;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;

import org.apache.log4j.Logger;

public class sceMp3 implements HLEModule, HLEStartModule {

    private static Logger log = Modules.getLogger("sceMp3");

    @Override
    public String getName() {
        return "sceMp3";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    @Override
    public void start() {
        mp3Map = new HashMap<Integer, Mp3Stream>();
    }

    @Override
    public void stop() {
    }
    protected int mp3HandleCount;
    protected HashMap<Integer, Mp3Stream> mp3Map;
    protected static final int compressionFactor = 10;
    protected static final int PSP_MP3_LOOP_NUM_INFINITE = -1;

    protected static final int mp3DecodeDelay = 4000;           // Microseconds

    // Media Engine based playback.
    protected static boolean useMediaEngine = false;

    public static boolean checkMediaEngineState() {
        return useMediaEngine;
    }

    public static void setEnableMediaEngine(boolean state) {
        useMediaEngine = state;
    }

    protected int endianSwap(int x) {
        return (x << 24) | ((x << 8) & 0xFF0000) | ((x >> 8) & 0xFF00) | ((x >> 24) & 0xFF);
    }

    protected int makeFakeMp3StreamHandle() {
        // The stream can't be negative.
        return 0x0000A300 | (mp3HandleCount++ & 0xFFFF);
    }

    private void delayThread(long startMicros, int delayMicros) {
    	long now = Emulator.getClock().microTime();
    	int threadDelayMicros = delayMicros - (int) (now - startMicros);
    	if (threadDelayMicros > 0) {
    		Modules.ThreadManForUserModule.hleKernelDelayThread(threadDelayMicros, false);
    	}
    }

    protected class Mp3Stream {
    	private final static int ME_READ_AHEAD = 7 * 32 * 1024; // 224K

    	// SceMp3InitArg struct.
        private final long mp3StreamStart;
        private final long mp3StreamEnd;
        private final int mp3Buf;
        private final int mp3BufSize;
        private final int mp3PcmBuf;
        private final int mp3PcmBufSize;

        // MP3 handle.
        private final int mp3Handle;

        // MP3 internal file buffer vars.
        private int mp3InputFileSize;
        private int mp3InputFileReadPos;
        private int mp3InputBufWritePos;
        private int mp3InputBufSize;

        // MP3 decoding vars.
        private int mp3DecodedBytes;

        // MP3 properties.
        private int mp3SampleRate;
        private int mp3LoopNum;
        private int mp3BitRate;
        private int mp3MaxSamples;
        private int mp3Channels;

        protected MediaEngine me;
        protected PacketChannel mp3Channel;
        private byte[] mp3PcmBuffer;

        //
        // The Buffer layout is the following:
        // - mp3BufSize: maximum buffer size, cannot be changed
        // - mp3InputBufSize: the number of bytes available for reading
        // - mp3InputFileReadPos: the index of the first byte available for reading
        // - mp3InputBufWritePos: the index of the first byte available for writing
        //                          (i.e. the index of the first byte after the last byte
        //                           available for reading)
        // The buffer is cyclic, i.e. the byte following the last byte is the first byte.
        // The following conditions are always true:
        // - 0 <= mp3InputFileReadPos < mp3BufSize
        // - 0 <= mp3InputBufWritePos < mp3BufSize
        // - mp3InputFileReadPos + mp3InputBufSize == mp3InputBufWritePos
        //   or (for cyclic buffer)
        //   mp3InputFileReadPos + mp3InputBufSize == mp3InputBufWritePos + mp3BufSize
        //
        // For example:
        //   [................R..........W.......]
        //                    |          +-> mp3InputBufWritePos
        //                    +-> mp3InputFileReadPos
        //                    <----------> mp3InputBufSize
        //   <-----------------------------------> mp3BufSize
        //
        //   mp3BufSize = 8192
        //   mp3InputFileReadPos = 4096
        //   mp3InputBufWritePos = 6144
        //   mp3InputBufSize = 2048
        //
        // MP3 Frame Header (4 bytes):
        // - Bits 31 to 21: Frame sync (all 1);
        // - Bits 20 and 19: MPEG Audio version;
        // - Bits 18 and 17: Layer;
        // - Bit 16: Protection bit;
        // - Bits 15 to 12: Bitrate;
        // - Bits 11 and 10: Sample rate;
        // - Bit 9: Padding;
        // - Bit 8: Reserved;
        // - Bits 7 and 6: Channels;
        // - Bits 5 and 4: Channel extension;
        // - Bit 3: Copyrigth;
        // - Bit 2 and 4: Original;
        // - Bits 1 and 0: Emphasis.
        //
        // NOTE: sceMp3 is only capable of handling MPEG Version 1 Layer III data.
        //

        public Mp3Stream(int args) {
            Memory mem = Memory.getInstance();

            // SceMp3InitArg struct.
            mp3StreamStart = mem.read64(args);
            mp3StreamEnd = mem.read64(args + 8);
            mp3Buf = mem.read32(args + 16);
            mp3BufSize = mem.read32(args + 20);
            mp3PcmBuf = mem.read32(args + 24);
            mp3PcmBufSize = mem.read32(args + 28);

            // The MP3 file reading position starts at mp3StreamStart.
            mp3InputFileReadPos = (int) mp3StreamStart;
            mp3InputFileSize = 0;

            // The MP3 buffer is currently empty.
            mp3InputBufWritePos = mp3InputFileReadPos;
            mp3InputBufSize = 0;

            // Set default properties.
            mp3MaxSamples = mp3PcmBufSize / 4;
            mp3LoopNum = PSP_MP3_LOOP_NUM_INFINITE;
            mp3DecodedBytes = 0;

            mp3Handle = makeFakeMp3StreamHandle();

            if (checkMediaEngineState()) {
                me = new MediaEngine();
                me.setAudioSamplesSize(mp3MaxSamples);
                mp3Channel = new PacketChannel();
            }

            mp3PcmBuffer = new byte[mp3PcmBufSize];
        }

        private void parseMp3FrameHeader() {
            Memory mem = Memory.getInstance();
            int header = endianSwap(mem.read32(mp3Buf));
            mp3Channels = calculateMp3Channels((header >> 6) & 0x3);
            mp3SampleRate = calculateMp3SampleRate((header >> 10) & 0x3);
            mp3BitRate = calculateMp3Bitrate((header >> 12) & 0xF);
        }

        private int calculateMp3Bitrate(int bitVal) {
            switch (bitVal) {
                case 0: return 0;  // Variable Bitrate.
                case 1: return 32;
                case 2: return 40;
                case 3: return 48;
                case 4: return 56;
                case 5: return 64;
                case 6: return 80;
                case 7: return 96;
                case 8: return 112;
                case 9: return 128;
                case 10: return 160;
                case 11: return 192;
                case 12: return 224;
                case 13: return 256;
                case 14: return 320;
                default: return -1;
            }
        }

        private int calculateMp3SampleRate(int bitVal) {
            if (bitVal == 0) {
                return 44100;
            } else if (bitVal == 1) {
                return 48000;
            } else if (bitVal == 2) {
                return 32000;
            } else {
                return 0;
            }
        }

        private int calculateMp3Channels(int bitVal) {
            if (bitVal == 0 || bitVal == 1 || bitVal == 2) {
                return 2;  // Stereo / Joint Stereo / Dual Channel.
            } else if (bitVal == 3) {
                return 1;  // Mono.
            } else {
                return 0;
            }
        }

        /**
         * @return number of bytes that can be read from the buffer
         */
        public int getMp3AvailableReadSize() {
            return mp3InputBufSize;
        }

        /**
         * @return number of bytes that can be written into the buffer
         */
        public int getMp3AvailableWriteSize() {
            return mp3BufSize - getMp3AvailableReadSize();
        }

        /**
         * @return number of bytes that can be written sequentially into the buffer
         */
        public int getMp3AvailableSequentialWriteSize() {
            return (mp3BufSize - mp3InputBufWritePos);
        }

        /**
         * Read bytes from the buffer.
         *
         * @param size    number of byte read
         * @return        number of bytes actually read
         */
        private int consumeRead(int size) {
            size = Math.min(size, getMp3AvailableReadSize());
            mp3InputBufSize -= size;
            mp3InputFileReadPos += size;
            return size;
        }

        /**
         * Write bytes into the buffer.
         *
         * @param size    number of byte written
         * @return        number of bytes actually written
         */
        private int consumeWrite(int size) {
            size = Math.min(size, getMp3AvailableWriteSize());
            mp3InputBufSize += size;
            mp3InputBufWritePos += size;
            if (mp3InputBufWritePos >= mp3BufSize) {
                mp3InputBufWritePos -= mp3BufSize;
            }
            return size;
        }

        public void init() {
            parseMp3FrameHeader();
            if (checkMediaEngineState()) {
                me.finish();
            }
        }

        private boolean checkMediaEngineChannel() {
        	if (checkMediaEngineState()) {
            	if (mp3Channel.length() < ME_READ_AHEAD) {
            		int neededLength = ME_READ_AHEAD - mp3Channel.length();
            		if (getMp3AvailableWriteSize() < neededLength) {
            			consumeRead(neededLength - getMp3AvailableWriteSize());
            		}
            		return false;
            	}
        	}

        	return true;
        }

        public int decode() {
            if (checkMediaEngineState()) {
            	// Wait to fill the ME Channel with enough data before opening the
            	// audio channel, otherwise the decoding might stop and assume
            	// an "End Of File" condition.
            	if (checkMediaEngineChannel()) {
            		if (me.getContainer() == null) {
	            		me.init(mp3Channel, false, true);
	            	}
            		me.stepAudio(0);
	                mp3DecodedBytes = copySamplesToMem(mp3PcmBuf, mp3PcmBufSize, mp3PcmBuffer);
            	} else {
            		mp3DecodedBytes = 0;
            	}
            } else {
                mp3DecodedBytes = mp3MaxSamples * 4;
                int mp3BufReadConsumed = Math.min(mp3DecodedBytes / compressionFactor, getMp3AvailableReadSize());
                consumeRead(mp3BufReadConsumed);
            }

            return mp3DecodedBytes;
        }

        public boolean isStreamDataNeeded() {
        	if (checkMediaEngineState()) {
        		checkMediaEngineChannel();
        		if (mp3Channel.length() >= ME_READ_AHEAD) {
        			// We have enough data into the channel
        			return false;
        		}
        	}

        	// We have not enough data into the channel, accept as much as we can...
        	return getMp3AvailableWriteSize() > 0;
        }

        public boolean isStreamDataEnd() {
            return (mp3InputFileSize >= (int) mp3StreamEnd);
        }

        public int addMp3StreamData(int size) {
            if(checkMediaEngineState()) {
                mp3Channel.write(getMp3BufWriteAddr(), size);
            }
            mp3InputFileSize += size;
            return consumeWrite(size);
        }

        private int copySamplesToMem(int address, int maxLength, byte[] buffer) {
            Memory mem = Memory.getInstance();

            int bytes = me.getCurrentAudioSamples(buffer);

        	if (bytes > 0) {
        		mem.copyToMemory(address, ByteBuffer.wrap(buffer, 0, bytes), bytes);
            }

            return bytes;
        }

        public int getMp3BufWriteAddr() {
        	return getMp3BufAddr() + getMp3InputBufWritePos();
        }

        public int getMp3Handle() {
            return mp3Handle;
        }

        public int getMp3LoopNum() {
            return mp3LoopNum;
        }

        public int getMp3BufAddr() {
            return mp3Buf;
        }

        public int getMp3PcmBufAddr() {
            return mp3PcmBuf;
        }

        public int getMp3PcmBufSize() {
            return mp3PcmBufSize;
        }

        public int getMp3InputFileReadPos() {
            return mp3InputFileReadPos;
        }

        public int getMp3InputBufWritePos() {
            return mp3InputBufWritePos;
        }

        public int getMp3BufSize() {
            return mp3BufSize;
        }

        public int getMp3DecodedSamples() {
            return mp3DecodedBytes / 4;
        }

        public int getMp3MaxSamples() {
            return mp3MaxSamples;
        }

        public int getMp3BitRate() {
            return mp3BitRate;
        }

        public int getMp3ChannelNum() {
            return mp3Channels;
        }

        public int getMp3SamplingRate() {
            return mp3SampleRate;
        }

        public int getMp3InputFileSize() {
        	return mp3InputFileSize;
        }
        
        public void setMp3LoopNum(int n) {
            mp3LoopNum = n;
        }

        public void setMp3BufCurrentPos(int pos) {
            mp3InputFileReadPos = pos;
            mp3InputBufWritePos = pos;
            mp3InputBufSize = 0;
            mp3InputFileSize = 0;
            mp3DecodedBytes = 0;
        }

		@Override
		public String toString() {
			return String.format("Mp3Stream(maxSize=%d, availableSize=%d, readPos=%d, writePos=%d)", mp3BufSize, mp3InputBufSize, mp3InputFileReadPos, mp3InputBufWritePos);
		}
    }

    public void sceMp3ReserveMp3Handle(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3args = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3ReserveMp3Handle " + String.format("mp3args=0x%08x", cpu.gpr[4]));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        Mp3Stream stream = new Mp3Stream(mp3args);
        int streamHandle = stream.getMp3Handle();
        mp3Map.put(streamHandle, stream);

        cpu.gpr[2] = streamHandle;
    }

    public void sceMp3NotifyAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];
        int size = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3NotifyAddStreamData " + String.format("mp3handle=0x%08x, size=%d", mp3handle, size));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // New data has been written by the application.
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.get(mp3handle).addMp3StreamData(size);
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3ResetPlayPosition(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3ResetPlayPosition " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.get(mp3handle).setMp3BufCurrentPos(0);
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3InitResource(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isInfoEnabled()) {
            log.info("sceMp3InitResource");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3TermResource(Processor processor) {
        CpuState cpu = processor.cpu;

        if(log.isInfoEnabled()) {
            log.info("sceMp3TermResource");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3SetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];
        int loopNbr = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3SetLoopNum " + String.format("mp3handle=0x%08x, loopNbr=%d", mp3handle, loopNbr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.get(mp3handle).setMp3LoopNum(loopNbr);
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3Init(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3Init " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.get(mp3handle).init();
        }
        if(log.isInfoEnabled()) {
            log.info("Initializing Mp3 data: channels = " + mp3Map.get(mp3handle).getMp3ChannelNum()
                    + ", samplerate = " + mp3Map.get(mp3handle).getMp3SamplingRate() + "kHz, bitrate = "
                    + mp3Map.get(mp3handle).getMp3BitRate() + "kbps.");
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetMp3ChannelNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3GetMp3ChannelNum " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int chNum = 0;
        if (mp3Map.containsKey(mp3handle)) {
            chNum = mp3Map.get(mp3handle).getMp3ChannelNum();
        }

        cpu.gpr[2] = chNum;
    }

    public void sceMp3GetSamplingRate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3GetSamplingRate " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int sampleRate = 0;
        if (mp3Map.containsKey(mp3handle)) {
            sampleRate = mp3Map.get(mp3handle).getMp3SamplingRate();
        }

        cpu.gpr[2] = sampleRate;
    }

    public void sceMp3GetInfoToAddStreamData(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mp3handle = cpu.gpr[4];
        int mp3BufAddr = cpu.gpr[5];
        int mp3BufToWriteAddr = cpu.gpr[6];
        int mp3PosAddr = cpu.gpr[7];

        if(log.isDebugEnabled()) {
            log.debug(String.format("sceMp3GetInfoToAddStreamData mp3handle=0x%08x, mp3BufAddr=0x%08x, mp3BufToWriteAddr=0x%08x, mp3PosAddr=0x%08x", mp3handle, mp3BufAddr, mp3BufToWriteAddr, mp3PosAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int bufAddr = 0;
        int bufToWrite = 0;
        int bufPos = 0;
        Mp3Stream stream = null;
        if (mp3Map.containsKey(mp3handle)) {
            stream = mp3Map.get(mp3handle);
            // Address where to write
            bufAddr = stream.isStreamDataEnd() ? 0 : stream.getMp3BufWriteAddr();
            // Length that can be written from bufAddr
            bufToWrite = stream.isStreamDataEnd() ? 0: stream.getMp3AvailableSequentialWriteSize();
            // Position in the source stream file to start reading from (seek position)
            bufPos = stream.getMp3InputFileSize();
        }
        mem.write32(mp3BufAddr, bufAddr);
        mem.write32(mp3BufToWriteAddr, bufToWrite);
        mem.write32(mp3PosAddr, bufPos);

        cpu.gpr[2] = 0;
    }

    public void sceMp3Decode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mp3handle = cpu.gpr[4];
        int outPcmAddr = cpu.gpr[5];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3Decode " + String.format("mp3handle=0x%08x, outPcmAddr=0x%08x", mp3handle, outPcmAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int pcmSamples = 0;
        Mp3Stream stream = null;
        if (mp3Map.containsKey(mp3handle)) {
            long startTime = Emulator.getClock().microTime();

            stream = mp3Map.get(mp3handle);
            pcmSamples = stream.decode();
            mem.write32(outPcmAddr, stream.getMp3PcmBufAddr());

            delayThread(startTime, mp3DecodeDelay);
        }

        cpu.gpr[2] = pcmSamples;
    }

    public void sceMp3CheckStreamDataNeeded(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3CheckStreamDataNeeded " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }

        boolean needsData = false;
        if (mp3Map.containsKey(mp3handle)) {
            needsData = mp3Map.get(mp3handle).isStreamDataNeeded();
        }

        // 1 - Needs more data.
        // 0 - Doesn't need more data.
        cpu.gpr[2] = needsData ? 1 : 0;
    }

    public void sceMp3ReleaseMp3Handle(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3ReleaseMp3Handle " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mp3Map.containsKey(mp3handle)) {
            mp3Map.remove(mp3handle);
        }

        cpu.gpr[2] = 0;
    }

    public void sceMp3GetSumDecodedSample(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3GetSumDecodedSample " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int samples = 0;
        if (mp3Map.containsKey(mp3handle)) {
            samples = mp3Map.get(mp3handle).getMp3DecodedSamples();
        }

        cpu.gpr[2] = samples;
    }

    public void sceMp3GetBitRate(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3GetBitRate " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int bitRate = 0;
        if (mp3Map.containsKey(mp3handle)) {
            bitRate = mp3Map.get(mp3handle).getMp3BitRate();
        }

        cpu.gpr[2] = bitRate;
    }

    public void sceMp3GetMaxOutputSample(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3GetMaxOutputSample " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int maxSamples = 0;
        if (mp3Map.containsKey(mp3handle)) {
            maxSamples = mp3Map.get(mp3handle).getMp3MaxSamples();
        }

        cpu.gpr[2] = maxSamples;
    }

    public void sceMp3GetLoopNum(Processor processor) {
        CpuState cpu = processor.cpu;

        int mp3handle = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMp3GetLoopNum " + String.format("mp3handle=0x%08x", mp3handle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int loopNum = 0;
        if (mp3Map.containsKey(mp3handle)) {
            loopNum = mp3Map.get(mp3handle).getMp3LoopNum();
        }

        cpu.gpr[2] = loopNum;
    }
    @HLEFunction(nid = 0x07EC321A, version = 150) public HLEModuleFunction sceMp3ReserveMp3HandleFunction;
    @HLEFunction(nid = 0x0DB149F4, version = 150) public HLEModuleFunction sceMp3NotifyAddStreamDataFunction;
    @HLEFunction(nid = 0x2A368661, version = 150) public HLEModuleFunction sceMp3ResetPlayPositionFunction;
    @HLEFunction(nid = 0x35750070, version = 150) public HLEModuleFunction sceMp3InitResourceFunction;
    @HLEFunction(nid = 0x3C2FA058, version = 150) public HLEModuleFunction sceMp3TermResourceFunction;
    @HLEFunction(nid = 0x3CEF484F, version = 150) public HLEModuleFunction sceMp3SetLoopNumFunction;
    @HLEFunction(nid = 0x44E07129, version = 150) public HLEModuleFunction sceMp3InitFunction;
    @HLEFunction(nid = 0x7F696782, version = 150) public HLEModuleFunction sceMp3GetMp3ChannelNumFunction;
    @HLEFunction(nid = 0x8F450998, version = 150) public HLEModuleFunction sceMp3GetSamplingRateFunction;
    @HLEFunction(nid = 0xA703FE0F, version = 150) public HLEModuleFunction sceMp3GetInfoToAddStreamDataFunction;
    @HLEFunction(nid = 0xD021C0FB, version = 150) public HLEModuleFunction sceMp3DecodeFunction;
    @HLEFunction(nid = 0xD0A56296, version = 150) public HLEModuleFunction sceMp3CheckStreamDataNeededFunction;
    @HLEFunction(nid = 0xF5478233, version = 150) public HLEModuleFunction sceMp3ReleaseMp3HandleFunction;
    @HLEFunction(nid = 0x354D27EA, version = 150) public HLEModuleFunction sceMp3GetSumDecodedSampleFunction;
    @HLEFunction(nid = 0x87677E40, version = 150) public HLEModuleFunction sceMp3GetBitRateFunction;
    @HLEFunction(nid = 0x87C263D1, version = 150) public HLEModuleFunction sceMp3GetMaxOutputSampleFunction;
    @HLEFunction(nid = 0xD8F54A51, version = 150) public HLEModuleFunction sceMp3GetLoopNumFunction;

}