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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUidClass;
import jpcsp.HLE.HLEUidObjectMapping;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.nio.ByteBuffer;
import java.util.HashMap;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class sceMp3 extends HLEModule {
    public static Logger log = Modules.getLogger("sceMp3");

	private class EnableMediaEngineSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnableMediaEngine(value);
		}
	}

	@Override
    public String getName() {
        return "sceMp3";
    }

    @Override
    public void start() {
        mp3Map = new HashMap<Integer, Mp3Stream>();

        setSettingsListener("emu.useMediaEngine", new EnableMediaEngineSettingsListerner());

        super.start();
    }


    protected int mp3HandleCount;
    protected HashMap<Integer, Mp3Stream> mp3Map;
    protected static final int compressionFactor = 10;
    protected static final int PSP_MP3_LOOP_NUM_INFINITE = -1;

    protected static final int mp3DecodeDelay = 4000;           // Microseconds

    // Media Engine based playback.
    private boolean useMediaEngine = false;
    
    protected boolean checkMediaEngineState() {
        return useMediaEngine;
    }

    private void setEnableMediaEngine(boolean state) {
        useMediaEngine = state;
    }

    protected int endianSwap(int x) {
        return (x << 24) | ((x << 8) & 0xFF0000) | ((x >> 8) & 0xFF00) | ((x >> 24) & 0xFF);
    }

    public int makeFakeMp3StreamHandle() {
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
    
    static final int ERROR_MP3_NOT_FOUND = 0;

    @HLEUidClass(moduleMethodUidGenerator = "makeFakeMp3StreamHandle", errorValueOnNotFound = ERROR_MP3_NOT_FOUND)
    protected class Mp3Stream {
    	private final static int ME_READ_AHEAD = 32 * 1024; // 32K
    	
    	// SceMp3InitArg struct.
        private final long mp3StreamStart;
        private final long mp3StreamEnd;
        private final int mp3Buf;
        private final int mp3BufSize;
        private final int mp3PcmBuf;
        private final int mp3PcmBufSize;
        private final int mp3CompleteBuf;
        private final int mp3CompleteBufSize;

        // MP3 internal file buffer vars.
        private long mp3InputFileSize;
        private int mp3InputFileReadPos;
        private int mp3InputBufWritePos;
        private int mp3InputBufSize;

        // MP3 decoding vars.
        private int mp3DecodedBytes;
        private int mp3SumDecodedSamples;

        // MP3 properties.
        private int mp3SampleRate;
        private int mp3LoopNum;
        private int mp3BitRate;
        private int mp3MaxSamples;
        private int mp3Channels;
        private int mp3Version;

        protected MediaEngine me;
        protected PacketChannel mp3Channel;
        private byte[] mp3PcmBuffer;
        private int decodeCount;

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
        // - Bits 20 to 19: MPEG Audio version;
        // - Bits 18 and 17: Layer;
        // - Bit 16: Protection bit;
        // - Bits 15 to 12: Bitrate;
        // - Bits 11 and 10: Sample rate;
        // - Bit 9: Padding;
        // - Bit 8: Reserved;
        // - Bits 7 and 6: Channels;
        // - Bits 5 and 4: Channel extension;
        // - Bit 3: Copyrigth;
        // - Bit 2: Original;
        // - Bits 1 and 0: Emphasis.
        //
        // NOTE: sceMp3 is only capable of handling MPEG Version 1 Layer III data.
        //

        public Mp3Stream(int args) {
            Memory mem = Memory.getInstance();

            // SceMp3InitArg struct.
            mp3StreamStart = mem.read64(args);
            mp3StreamEnd = mem.read64(args + 8);
            mp3CompleteBuf = mem.read32(args + 16);
            mp3CompleteBufSize = mem.read32(args + 20);
            mp3PcmBuf = mem.read32(args + 24);
            mp3PcmBufSize = mem.read32(args + 28);

            // 0x5C0 bytes seem to be reserved at the beginning of the buffer
            final int reservedSize = 0x5C0;
            mp3Buf = mp3CompleteBuf + reservedSize;
            mp3BufSize = mp3CompleteBufSize - reservedSize;

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceMp3ReserveMp3Handle Stream start=0x%X, end=0x%X", mp3StreamStart, mp3StreamEnd));
            	log.debug(String.format("sceMp3ReserveMp3Handle Mp3Buf 0x%08X, size=0x%X", mp3CompleteBuf, mp3CompleteBufSize));
            	log.debug(String.format("sceMp3ReserveMp3Handle PcmBuf 0x%08X, size=0x%X", mp3PcmBuf, mp3PcmBufSize));
            }

            mp3InputFileReadPos = 0;
            mp3InputFileSize = 0;

            // The MP3 buffer is currently empty.
            mp3InputBufWritePos = mp3InputFileReadPos;
            mp3InputBufSize = 0;

            // We do not know yet the number of channels, assume 2.
            // This will be overwritten as soon as we parse the MP3 header
            initMp3MaxSamples(2);

            // Set default properties.
            mp3LoopNum = PSP_MP3_LOOP_NUM_INFINITE;
            mp3DecodedBytes = 0;
            mp3SumDecodedSamples = 0;

            if (checkMediaEngineState()) {
                me = new MediaEngine();
                me.setAudioSamplesSize(mp3MaxSamples);
                mp3Channel = new PacketChannel();
            }

            mp3PcmBuffer = new byte[mp3PcmBufSize];
        }

        private void initMp3MaxSamples(int mp3Channels) {
            // In the JpcspTrace log of "Downstream Panic! - ULUS10322",
            // the PSP is returning 0x1200 bytes at each sceMp3Decode call.
            // Is this maybe depending on the MP3 file itself?
            final int maxSampleBytes = mp3Channels == 1 ? 0x900 : 0x1200;
            mp3MaxSamples = Math.min(mp3PcmBufSize, maxSampleBytes) / 4;
        }

        private void parseMp3FrameHeader() {
            Memory mem = Memory.getInstance();
            // Skip the ID3 tags, the MP3 stream starts at mp3StreamStart.
            int header = endianSwap(Utilities.readUnaligned32(mem, mp3Buf + (int) mp3StreamStart));
            if (log.isDebugEnabled()) {
            	log.debug(String.format("Mp3 header: 0x%08X", header));
            }
            mp3Version = (header >> 19) & 0x3;
            int mp3Layer = (header >> 17) & 0x3;
            mp3Channels = calculateMp3Channels((header >> 6) & 0x3);
            mp3SampleRate = calculateMp3SampleRate((header >> 10) & 0x3, mp3Version);
            mp3BitRate = calculateMp3Bitrate((header >> 12) & 0xF, mp3Version, mp3Layer);

            // Now, we know the real number of channels, update the max samples
            initMp3MaxSamples(mp3Channels);
        }

        private int calculateMp3Bitrate(int bitVal, int mp3Version, int mp3Layer) {
        	int[] valueMapping = null;
        	switch (mp3Version) {
        		case 3: // MPEG Version 1
        			switch (mp3Layer) {
	        			case 3: // Layer I
	        				valueMapping = new int[] { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1 };
	        				break;
	        			case 2: // Layer II
	        				valueMapping = new int[] { 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, -1 };
	        				break;
	        			case 1: // Layer III
	        				valueMapping = new int[] { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1 };
	        				break;
        			}
        			break;
        		case 2: // MPEG Version 2
        		case 0: // MPEG Version 2.5
        			switch (mp3Layer) {
	        			case 3: // Layer I
	        				valueMapping = new int[] { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1 };
	        				break;
	        			case 2: // Layer II
	        			case 1: // Layer III
	        				valueMapping = new int[] { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1 };
	        				break;
	    			}
        			break;
        	}

        	if (valueMapping == null) {
        		return -1;
        	}

        	return valueMapping[bitVal];
        }

        private int calculateMp3SampleRate(int bitVal, int mp3Version) {
        	int[] valueMapping = null;
        	switch (mp3Version) {
        		case 3: // MPEG Version 1
        			valueMapping = new int[] { 44100, 48000, 32000, -1 };
        			break;
        		case 2: // MPEG Version 2
        			valueMapping = new int[] { 22050, 24000, 16000, -1 };
        			break;
        		case 0: // MPEG Version 2.5
        			valueMapping = new int[] { 11025, 12000, 8000, -1 };
        			break;
        	}

        	if (valueMapping == null) {
        		return 0;
        	}

        	return valueMapping[bitVal];
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
         * The PSP is always adding data to the MP3 stream with this size.
         * 
         * @return the size used by the PSP to add data to the MP3 stream.
         */
        private int getMaxAddStreamDataSize() {
        	return mp3BufSize >> 1;
        }

        /**
         * @return number of bytes that can be written sequentially into the buffer
         */
        public int getMp3AvailableSequentialWriteSize() {
        	return Math.min(getMp3AvailableWriteSize(), mp3BufSize - mp3InputBufWritePos);
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
        		int minimumChannelLength = (int) Math.min(ME_READ_AHEAD, mp3StreamEnd - mp3StreamStart);
            	if (mp3Channel.length() < minimumChannelLength) {
            		int neededLength = ME_READ_AHEAD - mp3Channel.length();
            		consumeRead(neededLength);
            		return false;
            	}
        	}

        	return true;
        }

        /**
         * The PSP is using the PCM buffer to perform some double-buffering.
         * The first half of the buffer is used at the first sceMp3Decode call,
         * the second halt of the buffer is used at the second sceMp3Decode call.
         * The third call is reusing the first halt of the buffer and so on.
         * 
         * @return the address where to store the PCM data returned by sceMp3Decode.
         */
        public int getDecodeBuffer() {
    		int decodeBuffer = mp3PcmBuf;
    		if ((decodeCount & 1) != 0) {
    			decodeBuffer += getDecodeBufferSize();
    		}

    		return decodeBuffer;
        }

        public int getDecodeBufferSize() {
        	return mp3PcmBufSize >> 1;
        }

        public int decode(int decodeBuffer, int decodeBufferSize) {
    		decodeCount++;

        	if (checkMediaEngineState()) {
            	// Wait to fill the ME Channel with enough data before opening the
            	// audio channel, otherwise the decoding might stop and assume
            	// an "End Of File" condition.
            	if (me.getContainer() != null || checkMediaEngineChannel()) {
            		if (me.getContainer() == null) {
	            		me.init(mp3Channel, false, true, 0, 0);
	            	}
            		me.stepAudio(getMp3MaxSamples() * getBytesPerSample(), getMp3DecodeNumberOfChannels());
	                mp3DecodedBytes = copySamplesToMem(decodeBuffer, decodeBufferSize, mp3PcmBuffer);
	                if (log.isTraceEnabled()) {
	                	log.trace(String.format("decoded %d samples: %s", mp3DecodedBytes, Utilities.getMemoryDump(decodeBuffer, mp3DecodedBytes)));
	                }
            	} else {
            		// sceMp3Decode is not expected to return 0 samples at the start.
            		// Fake the return of 1 empty sample.
            		int fakeSamples = 1;
            		mp3DecodedBytes = fakeSamples * getBytesPerSample();
                    // Clear the whole PCM buffer, just in case the application is expecting
            		// mp3MaxSamples and not just 1 sample.
                    Memory.getInstance().memset(decodeBuffer, (byte) 0, decodeBufferSize);
            	}
            } else {
            	// Return mp3MaxSamples samples (all set to 0).
                mp3DecodedBytes = getMp3MaxSamples() * getBytesPerSample();
                Memory.getInstance().memset(decodeBuffer, (byte) 0, mp3DecodedBytes);

                int mp3BufReadConsumed = Math.min(mp3DecodedBytes / compressionFactor, getMp3AvailableReadSize());
                consumeRead(mp3BufReadConsumed);
            }

        	mp3SumDecodedSamples += mp3DecodedBytes / getBytesPerSample();

        	return mp3DecodedBytes;
        }

        public int getBytesPerSample() {
        	// 2 Bytes per decoded channel
        	return getMp3DecodeNumberOfChannels() * 2;
        }

        public boolean isStreamDataNeeded() {
        	if (isStreamDataEnd()) {
        		return false;
        	}

        	if (checkMediaEngineState()) {
        		checkMediaEngineChannel();
        		if (mp3Channel.length() >= ME_READ_AHEAD) {
        			// We have enough data into the channel
        			return false;
        		}
        	}

        	// We have not enough data into the channel,
        	// accept only in packets of maxAddStreamDataSize (like the PSP)
        	return getMp3AvailableSequentialWriteSize() >= getMaxAddStreamDataSize();
        }

        public boolean isStreamDataEnd() {
            return mp3InputFileSize >= mp3StreamEnd;
        }

        public int addMp3StreamData(int size) {
            if (checkMediaEngineState()) {
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
            return mp3DecodedBytes / getBytesPerSample();
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

        public int getMp3DecodeNumberOfChannels() {
        	// Always return 2 channels from decoding
        	return 2;
        }

        public int getMp3SamplingRate() {
            return mp3SampleRate;
        }

        public long getMp3InputFileSize() {
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

        public int getMp3Version() {
        	return mp3Version;
        }

		public int getMp3SumDecodedSamples() {
			return mp3SumDecodedSamples;
		}

        @Override
		public String toString() {
			return String.format("Mp3Stream(maxSize=%d, availableSize=%d, readPos=%d, writePos=%d)", mp3BufSize, mp3InputBufSize, mp3InputFileReadPos, mp3InputBufWritePos);
		}
    }

    @HLEFunction(nid = 0x07EC321A, version = 150, checkInsideInterrupt = true)
    public Mp3Stream sceMp3ReserveMp3Handle(TPointer mp3args) {
        Mp3Stream mp3Stream = new Mp3Stream(mp3args.getAddress());

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMp3ReserveMp3Handle returning %s", mp3Stream));
        }

        return mp3Stream;
    }

    @HLEFunction(nid = 0x0DB149F4, version = 150, checkInsideInterrupt = true)
    public int sceMp3NotifyAddStreamData(Mp3Stream mp3Stream, int size) {
        // New data has been written by the application.
        mp3Stream.addMp3StreamData(size);
        return 0;
    }

    @HLEFunction(nid = 0x2A368661, version = 150, checkInsideInterrupt = true)
    public int sceMp3ResetPlayPosition(Mp3Stream mp3Stream) {
        mp3Stream.setMp3BufCurrentPos(0);

        return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x35750070, version = 150, checkInsideInterrupt = true)
    public int sceMp3InitResource() {
        return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x3C2FA058, version = 150, checkInsideInterrupt = true)
    public int sceMp3TermResource() {
        return 0;
    }

    @HLEFunction(nid = 0x3CEF484F, version = 150, checkInsideInterrupt = true)
    public int sceMp3SetLoopNum(Mp3Stream mp3Stream, int loopNbr) {
        mp3Stream.setMp3LoopNum(loopNbr);

        return 0;
    }

    @HLEFunction(nid = 0x44E07129, version = 150, checkInsideInterrupt = true)
    public int sceMp3Init(Mp3Stream mp3Stream) {
    	mp3Stream.init();
        if (log.isInfoEnabled()) {
            log.info(String.format("Initializing Mp3 data: channels=%d, samplerate=%dkHz, bitrate=%dkbps.", mp3Stream.getMp3ChannelNum(), mp3Stream.getMp3SamplingRate(), mp3Stream.getMp3BitRate()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x7F696782, version = 150, checkInsideInterrupt = true)
    public int sceMp3GetMp3ChannelNum(Mp3Stream mp3Stream) {
        return mp3Stream.getMp3ChannelNum();
    }

    @HLEFunction(nid = 0x8F450998, version = 150, checkInsideInterrupt = true)
    public int sceMp3GetSamplingRate(Mp3Stream mp3Stream) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp3GetSamplingRate returning 0x%X", mp3Stream.getMp3SamplingRate()));
    	}
        return mp3Stream.getMp3SamplingRate();
    }

    @HLEFunction(nid = 0xA703FE0F, version = 150, checkInsideInterrupt = true)
    public int sceMp3GetInfoToAddStreamData(Mp3Stream mp3Stream, @CanBeNull TPointer32 mp3BufPtr, @CanBeNull TPointer32 mp3BufToWritePtr, @CanBeNull TPointer32 mp3PosPtr) {
        // Address where to write
        mp3BufPtr.setValue(mp3Stream.isStreamDataEnd() ? 0 : mp3Stream.getMp3BufWriteAddr());

        // Length that can be written from bufAddr
        int mp3BufToWrite;
        if (mp3Stream.isStreamDataEnd()) {
        	mp3BufToWrite = 0;
        } else if (mp3Stream.getMp3InputFileSize() == 0) {
        	mp3BufToWrite = mp3Stream.getMaxAddStreamDataSize() * 2;
        } else {
        	mp3BufToWrite = mp3Stream.getMaxAddStreamDataSize();
        }
        mp3BufToWrite = Math.min(mp3BufToWrite, mp3Stream.getMp3AvailableSequentialWriteSize());
        mp3BufToWritePtr.setValue(mp3BufToWrite);

        // Position in the source stream file to start reading from (seek position)
        mp3PosPtr.setValue((int) mp3Stream.getMp3InputFileSize());

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMp3GetInfoToAddStreamData returning mp3Buf=0x%08X, mp3BufToWrite=0x%X, mp3Pos=0x%X", mp3BufPtr.getValue(), mp3BufToWritePtr.getValue(), mp3PosPtr.getValue()));
        }

        return 0;
    }

    @HLEFunction(nid = 0xD021C0FB, version = 150, checkInsideInterrupt = true)
    public int sceMp3Decode(Mp3Stream mp3Stream, TPointer32 outPcmPtr) {
        long startTime = Emulator.getClock().microTime();

        int decodeBuffer = mp3Stream.getDecodeBuffer();
        int pcmBytes = mp3Stream.decode(decodeBuffer, mp3Stream.getDecodeBufferSize());
        outPcmPtr.setValue(decodeBuffer);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMp3Decode returning 0x%X bytes (%d samples) at 0x%08X", pcmBytes, mp3Stream.getMp3DecodedSamples(), outPcmPtr.getValue()));
        }

        delayThread(startTime, mp3DecodeDelay);

        return pcmBytes;
    }

    @HLEFunction(nid = 0xD0A56296, version = 150, checkInsideInterrupt = true)
    public boolean sceMp3CheckStreamDataNeeded(Mp3Stream mp3Stream) {
    	boolean dataNeeded = mp3Stream.isStreamDataNeeded();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp3CheckStreamDataNeeded returning %b", dataNeeded));
    	}

    	// 1 - Needs more data.
        // 0 - Doesn't need more data.
        return dataNeeded;
    }

    @HLEFunction(nid = 0xF5478233, version = 150, checkInsideInterrupt = true)
    public int sceMp3ReleaseMp3Handle(Mp3Stream mp3Stream) {
        HLEUidObjectMapping.removeObject(mp3Stream);

        return 0;
    }

    @HLEFunction(nid = 0x354D27EA, version = 150)
    public int sceMp3GetSumDecodedSample(Mp3Stream mp3Stream) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp3GetSumDecodedSample returning %d", mp3Stream.getMp3SumDecodedSamples()));
    	}
        return mp3Stream.getMp3SumDecodedSamples();
    }

    @HLEFunction(nid = 0x87677E40, version = 150, checkInsideInterrupt = true)
    public int sceMp3GetBitRate(Mp3Stream mp3Stream) {
        return mp3Stream.getMp3BitRate();
    }
    
    @HLEFunction(nid = 0x87C263D1, version = 150, checkInsideInterrupt = true)
    public int sceMp3GetMaxOutputSample(Mp3Stream mp3Stream) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMp3GetMaxOutputSample returning 0x%X", mp3Stream.getMp3MaxSamples()));
    	}
        return mp3Stream.getMp3MaxSamples();
    }

    @HLEFunction(nid = 0xD8F54A51, version = 150, checkInsideInterrupt = true)
    public int sceMp3GetLoopNum(Mp3Stream mp3Stream) {
        return mp3Stream.getMp3LoopNum();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3548AEC8, version = 150)
    public int sceMp3GetFrameNum(Mp3Stream mp3Stream) {
    	return 0;
    }

    @HLEFunction(nid = 0xAE6D2027, version = 150)
    public int sceMp3GetVersion(Mp3Stream mp3Stream) {
    	return mp3Stream.getMp3Version();
    }

    @HLEFunction(nid = 0x0840E808, version = 150, checkInsideInterrupt = true)
    public int sceMp3ResetPlayPosition2(Mp3Stream mp3Stream, int position) {
        mp3Stream.setMp3BufCurrentPos(position);

        return 0;
    }
}