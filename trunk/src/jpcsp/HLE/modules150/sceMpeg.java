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

import static jpcsp.Allegrex.compiler.RuntimeContext.memoryInt;
import static jpcsp.HLE.modules150.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.HLE.modules600.sceMpeg.AVC_ES_BUF_SIZE;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PACK_START_CODE;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PADDING_STREAM;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_1;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.PRIVATE_STREAM_2;
import static jpcsp.format.psmf.PsmfAudioDemuxVirtualFile.SYSTEM_HEADER_START_CODE;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned16;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.kernel.types.SceMpegRingbuffer;
import jpcsp.HLE.kernel.types.pspFileBuffer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.format.psmf.PesHeader;
import jpcsp.graphics.VideoEngine;
import jpcsp.hardware.Screen;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.IVideoCodec;
import jpcsp.media.codec.h264.H264Utils;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.scheduler.DelayThreadAction;
import jpcsp.scheduler.UnblockThreadAction;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

import com.twilight.h264.decoder.H264Context;

@HLELogging
public class sceMpeg extends HLEModule {
    public static Logger log = Modules.getLogger("sceMpeg");

    @Override
    public String getName() {
        return "sceMpeg";
    }

    @Override
    public void start() {
        mpegHandle = 0;
        mpegRingbuffer = null;
        mpegRingbufferAddr = null;
        avcAuAddr = 0;
        atracAuAddr = 0;
        mpegAtracAu = new SceMpegAu();
        mpegAvcAu = new SceMpegAu();
        mpegUserDataAu = new SceMpegAu();
        psmfHeader = null;

        intBuffers = new HashSet<int[]>();
        lastFrameABGR = null;

        audioDecodeBuffer = new byte[MPEG_ATRAC_ES_OUTPUT_SIZE];
        allocatedEsBuffers = new boolean[2];
        streamMap = new HashMap<Integer, StreamInfo>();

        videoCodecExtraData = null;

        super.start();
    }

    @Override
    public void stop() {
    	// Free the temporary arrays
    	intBuffers.clear();

    	// Free objects no longer used
    	audioDecodeBuffer = null;
    	allocatedEsBuffers = null;
    	streamMap = null;
    	mpegAtracAu = null;
    	mpegAvcAu = null;

    	super.stop();
    }

    // MPEG statics.
    public static final int PSMF_MAGIC = 0x464D5350;
    public static final int PSMF_VERSION_0012 = 0x32313030;
    public static final int PSMF_VERSION_0013 = 0x33313030;
    public static final int PSMF_VERSION_0014 = 0x34313030;
    public static final int PSMF_VERSION_0015 = 0x35313030;
    public static final int PSMF_MAGIC_OFFSET = 0x0;
    public static final int PSMF_STREAM_VERSION_OFFSET = 0x4;
    public static final int PSMF_STREAM_OFFSET_OFFSET = 0x8;
    public static final int PSMF_STREAM_SIZE_OFFSET = 0xC;
    public static final int PSMF_FIRST_TIMESTAMP_OFFSET = 0x56;
    public static final int PSMF_LAST_TIMESTAMP_OFFSET = 0x5C;
    public static final int PSMF_NUMBER_STREAMS_OFFSET = 0x80;
    public static final int PSMF_FRAME_WIDTH_OFFSET = 0x8E;
    public static final int PSMF_FRAME_HEIGHT_OFFSET = 0x8F;
    protected static final int MPEG_MEMSIZE = 0x10000;      // 64k.
    private static final int AUDIO_BUFFER_OFFSET = 0x100;   // Offset of the audio buffer inside MPEG structure
    private static final int AUDIO_BUFFER_SIZE   = 0x1000;  // Size of the audio buffer
    public static final int atracDecodeDelay = 3000;        // Microseconds
    public static final int avcDecodeDelay = 5400;          // Microseconds
    public static final int mpegDecodeErrorDelay = 100;     // Delay in Microseconds in case of decode error
    public static final int mpegTimestampPerSecond = 90000; // How many MPEG Timestamp units in a second.
    public static final int videoTimestampStep = 3003;      // Value based on pmfplayer (mpegTimestampPerSecond / 29.970 (fps)).
    public static final int audioTimestampStep = 4180;      // For audio play at 44100 Hz (2048 samples / 44100 * mpegTimestampPerSecond == 4180)
    public static final long UNKNOWN_TIMESTAMP = -1;
    public static final int PSMF_AVC_STREAM = 0;
    public static final int PSMF_ATRAC_STREAM = 1;
    public static final int PSMF_PCM_STREAM = 2;
    public static final int PSMF_DATA_STREAM = 3;
    public static final int PSMF_AUDIO_STREAM = 15;
    public static final int PSMF_VIDEO_STREAM_ID = 0xE0;
    public static final int PSMF_AUDIO_STREAM_ID = 0xBD;
    // The YCbCr buffer is starting with 128 bytes of unknown data
    protected static final int YCBCR_DATA_OFFSET = 128;

    // At least 2048 bytes of MPEG data is provided when analysing the MPEG header
    public static final int MPEG_HEADER_BUFFER_MINIMUM_SIZE = 2048;

    // MPEG processing vars.
    protected int mpegHandle;
    protected SceMpegRingbuffer mpegRingbuffer;
    protected TPointer mpegRingbufferAddr;
    protected SceMpegAu mpegAtracAu;
    protected SceMpegAu mpegAvcAu;
    protected SceMpegAu mpegUserDataAu;
    protected long lastAtracSystemTime;
    protected long lastAvcSystemTime;
    protected int avcAuAddr;
    protected int atracAuAddr;
    protected int videoFrameCount;
    protected int audioFrameCount;
    private long currentVideoTimestamp;
    private long currentAudioTimestamp;
    protected int videoPixelMode;
    protected int defaultFrameWidth;
    // MPEG AVC elementary stream.
    protected static final int MPEG_AVC_ES_SIZE = 2048;          // MPEG packet size.
    // MPEG ATRAC elementary stream.
    protected static final int MPEG_ATRAC_ES_SIZE = 2112;
    public    static final int MPEG_ATRAC_ES_OUTPUT_SIZE = 8192;
    // MPEG PCM elementary stream.
    protected static final int MPEG_PCM_ES_SIZE = 320;
    protected static final int MPEG_PCM_ES_OUTPUT_SIZE = 320;
    // MPEG Userdata elementary stream.
    protected static final int MPEG_DATA_ES_SIZE = 0xA0000;
    protected static final int MPEG_DATA_ES_OUTPUT_SIZE = 0xA0000;
    // MPEG analysis results.
    public static final int MPEG_VERSION_0012 = 0;
    public static final int MPEG_VERSION_0013 = 1;
    public static final int MPEG_VERSION_0014 = 2;
    public static final int MPEG_VERSION_0015 = 3;
    protected static final int MPEG_AU_MODE_DECODE = 0;
    protected static final int MPEG_AU_MODE_SKIP = 1;
    protected int registeredVideoChannel = -1;
    protected int registeredAudioChannel = -1;
    // MPEG decoding results.
    protected static final int MPEG_AVC_DECODE_SUCCESS = 1;       // Internal value.
    protected static final int MPEG_AVC_DECODE_ERROR_FATAL = -8;
    protected int avcDecodeResult;
    protected int avcGotFrame;
    protected boolean startedMpeg;
    protected byte[] audioDecodeBuffer;
    protected boolean[] allocatedEsBuffers;
    protected HashMap<Integer, StreamInfo> streamMap;
    protected static final String streamPurpose = "sceMpeg-Stream";
    protected static final int mpegAudioOutputChannels = 2;
    protected SysMemInfo avcEsBuf;
    public PSMFHeader psmfHeader;
    private AudioBuffer audioBuffer;
    private int audioFrameLength;
    private final int frameHeader[] = new int[8];
    private int frameHeaderLength;
    private ICodec audioCodec;
    private VideoBuffer videoBuffer;
    private IVideoCodec videoCodec;
    private int videoCodecExtraData[];
    private static final int MAX_INT_BUFFERS_SIZE = 12;
    private Set<int[]> intBuffers;
    private PesHeader audioPesHeader;
    private PesHeader videoPesHeader;
    private final PesHeader dummyPesHeader = new PesHeader(0);
    private VideoDecoderThread videoDecoderThread;
    private LinkedList<DecodedImageInfo> decodedImages;
    private int lastFrameABGR[];
    private int lastFrameWidth;
    private int lastFrameHeight;
    private PesHeader userDataPesHeader;
    private UserDataBuffer userDataBuffer;
    private final int userDataHeader[] = new int[8];
    private int userDataLength;
    private int videoFrameHeight;

    private static class DecodedImageInfo {
    	public PesHeader pesHeader;
    	public int frameEnd;
    	public boolean gotFrame;
    	public int imageWidth;
    	public int imageHeight;
    	public int luma[];
    	public int cr[];
    	public int cb[];
    	public int abgr[];

    	@Override
		public String toString() {
			return String.format("pesHeader=%s, frameEnd=0x%X, gotFrame=%b, image %dx%d", pesHeader, frameEnd, gotFrame, imageWidth, imageHeight);
		}
    }

    private static class AudioBuffer {
    	private int addr;
    	private int size;
    	private int length;

    	public AudioBuffer(int addr, int size) {
    		this.addr = addr;
    		this.size = size;
    		length = 0;
    	}

    	public int write(Memory mem, int dataAddr, int size) {
    		size = Math.min(size, getFreeLength());
    		mem.memcpy(addr + length, dataAddr, size);
    		length += size;

    		return size;
    	}

    	public int getLength() {
    		return length;
    	}

    	public int getReadAddr() {
    		return addr;
    	}

    	public int getFreeLength() {
    		return size - length;
    	}

    	public int notifyRead(Memory mem, int size) {
    		size = Math.min(size, length);
    		length -= size;
    		mem.memcpy(addr, addr + size, length);

    		return size;
    	}

    	public boolean isEmpty() {
    		return length == 0;
    	}

    	public void reset() {
    		length = 0;
    	}
    }

    private static class VideoBuffer {
    	private int buffer[] = new int[10000];
    	private int length;
    	private static int quickSearch[];
    	private int frameSizes[];
    	private int frame;

    	public VideoBuffer() {
    		length = 0;
    		frame = 0;
    		frameSizes = null;

    		initQuickSearch();
    	}

    	private static void initQuickSearch() {
    		if (quickSearch != null) {
    			return;
    		}

    		quickSearch = new int[256];
    		Arrays.fill(quickSearch, 5);
    		quickSearch[0] = 2;
    		quickSearch[1] = 1;
    	}

    	public synchronized void write(Memory mem, int dataAddr, int size) {
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("VideoBuffer.write addr=0x%08X, size=0x%X, %s", dataAddr, size, this));
    		}

    		if (size + length > buffer.length) {
    			int extendedBuffer[] = new int[size + length];
    			System.arraycopy(buffer, 0, extendedBuffer, 0, length);
    			buffer = extendedBuffer;
    		}

    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(dataAddr, size, 1);
    		for (int i = 0; i < size; i++) {
    			buffer[length++] = memoryReader.readNext();
    		}
    	}

    	public int[] getBuffer() {
    		return buffer;
    	}

    	public int getBufferOffset() {
    		return 0;
    	}

    	public synchronized void notifyRead(int size) {
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("VideoBuffer.notifyRead size=0x%X, %s", size, this));
    		}

    		size = Math.min(size, length);
    		length -= size;
    		System.arraycopy(buffer, size, buffer, 0, length);

    		frame++;
    	}

    	public synchronized void reset() {
    		length = 0;
    	}

    	public int getLength() {
    		return length;
    	}

    	public boolean isEmpty() {
    		return length == 0;
    	}

    	public synchronized int findFrameEnd() {
    		if (frameSizes != null && frame < frameSizes.length) {
    			if (log.isTraceEnabled()) {
    				log.trace(String.format("VideoBuffer.findFrameEnd frameSize=0x%X, %s", frameSizes[frame], this));
    			}

    			return frameSizes[frame];
    		}

    		for (int i = 5; i < length; ) {
    			int value = buffer[i];
    			if (buffer[i - 4] == 0x00 &&
    			    buffer[i - 3] == 0x00 &&
    			    buffer[i - 2] == 0x00 &&
    			    buffer[i - 1] == 0x01) {
    				int nalUnitType = value & 0x1F;
    				if (nalUnitType == H264Context.NAL_AUD) {
    					return i - 4;
    				}
    			}
    			i += quickSearch[value];
    		}

    		return -1;
    	}

    	public void setFrameSizes(int frameSizes[]) {
    		this.frameSizes = frameSizes;
    		frame = 0;
    	}

    	public synchronized void setFrame(int newFrame) {
    		if (frameSizes != null) {
    			if (log.isTraceEnabled()) {
    				log.trace(String.format("VideoBuffer.setFrame newFrame=0x%X, %s", newFrame, this));
    			}

    			if (newFrame < frame) {
    				reset();
    			} else {
	    			// Skip frames up to new frame
	    			while (frame < newFrame && !isEmpty()) {
	    				notifyRead(frameSizes[frame]);
	    			}
    			}
    		}
    		this.frame = newFrame;
    	}

		@Override
		public String toString() {
			return String.format("VideoBuffer[length=0x%X, frame=0x%X]", length, frame);
		}
    }

    private static class UserDataBuffer {
    	private int addr;
    	private int size;
    	private int length;

    	public UserDataBuffer(int addr, int size) {
    		this.addr = addr;
    		this.size = size;
    		length = 0;
    	}

    	public int write(Memory mem, int dataAddr, int size) {
    		size = Math.min(size, getFreeLength());
    		mem.memcpy(addr + length, dataAddr, size);
    		length += size;

    		return size;
    	}

    	public int getLength() {
    		return length;
    	}

    	public int getFreeLength() {
    		return size - length;
    	}

    	public int notifyRead(Memory mem, int size) {
    		size = Math.min(size, length);
    		length -= size;
    		mem.memcpy(addr, addr + size, length);

    		return size;
    	}
    }

    // Entry class for the PSMF streams.
    public static class PSMFStream {
        private int streamType = -1;
        private int streamChannel = -1;
        private int streamNumber;
    	private int EPMapNumEntries;
    	private int EPMapOffset;
    	private List<PSMFEntry> EPMap;
    	private int frameWidth;
    	private int frameHeight;

        public PSMFStream(int streamNumber) {
        	this.streamNumber = streamNumber;
        }

        public int getStreamType() {
            return streamType;
        }

        public int getStreamChannel() {
            return streamChannel;
        }

		public int getStreamNumber() {
			return streamNumber;
		}

		public boolean isStreamOfType(int type) {
			if (streamType == type) {
				return true;
			}
			if (type == PSMF_AUDIO_STREAM) {
				// Atrac or PCM
				return streamType == PSMF_ATRAC_STREAM || streamType == PSMF_PCM_STREAM;
			}

			return false;
		}

		public void readMPEGVideoStreamParams(Memory mem, int addr, byte[] mpegHeader, int offset, PSMFHeader psmfHeader) {
            int streamID = read8(mem, addr, mpegHeader, offset);                // 0xE0
            int privateStreamID = read8(mem, addr, mpegHeader, offset + 1);     // 0x00
            int unk1 = read8(mem, addr, mpegHeader, offset + 2);                // Found values: 0x20/0x21 
            int unk2 = read8(mem, addr, mpegHeader, offset + 3);                // Found values: 0x44/0xFB/0x75
            EPMapOffset = endianSwap32(sceMpeg.readUnaligned32(mem, addr, mpegHeader, offset + 4));
            EPMapNumEntries = endianSwap32(sceMpeg.readUnaligned32(mem, addr, mpegHeader, offset + 8));
            frameWidth = read8(mem, addr, mpegHeader, offset + 12) * 0x10;  // PSMF video width (bytes per line).
            frameHeight = read8(mem, addr, mpegHeader, offset + 13) * 0x10; // PSMF video heigth (bytes per line).

            if (log.isInfoEnabled()) {
	            log.info(String.format("Found PSMF MPEG video stream data: streamID=0x%X, privateStreamID=0x%X, unk1=0x%X, unk2=0x%X, EPMapOffset=0x%x, EPMapNumEntries=%d, frameWidth=%d, frameHeight=%d", streamID, privateStreamID, unk1, unk2, EPMapOffset, EPMapNumEntries, frameWidth, frameHeight));
            }

            streamType = PSMF_AVC_STREAM;
            streamChannel = streamID & 0x0F;
        }

        public void readPrivateAudioStreamParams(Memory mem, int addr, byte[] mpegHeader, int offset, PSMFHeader psmfHeader) {
            int streamID = read8(mem, addr, mpegHeader, offset);                  // 0xBD
            int privateStreamID = read8(mem, addr, mpegHeader, offset + 1);       // 0x00
            int unk1 = read8(mem, addr, mpegHeader, offset + 2);                  // Always 0x20
            int unk2 = read8(mem, addr, mpegHeader, offset + 3);                  // Always 0x04
            int audioChannelConfig = read8(mem, addr, mpegHeader, offset + 14);   // 1 - mono, 2 - stereo
            int audioSampleFrequency = read8(mem, addr, mpegHeader, offset + 15); // 2 - 44khz

            if (log.isInfoEnabled()) {
	            log.info(String.format("Found PSMF MPEG audio stream data: streamID=0x%X, privateStreamID=0x%X, unk1=0x%X, unk2=0x%X, audioChannelConfig=%d, audioSampleFrequency=%d", streamID, privateStreamID, unk1, unk2, audioChannelConfig, audioSampleFrequency));
            }

            if (psmfHeader != null) {
            	psmfHeader.audioChannelConfig = audioChannelConfig;
            	psmfHeader.audioSampleFrequency = audioSampleFrequency;
            }

            streamType = ((privateStreamID & 0xF0) == 0 ? PSMF_ATRAC_STREAM : PSMF_PCM_STREAM);
            streamChannel = privateStreamID & 0x0F;
        }

        public void readUserDataStreamParams(Memory mem, int addr, byte[] mpegHeader, int offset, PSMFHeader psmfHeader) {
        	log.warn(String.format("Unknown User Data stream format"));
        	streamType = PSMF_DATA_STREAM;
        }
    }

    // Entry class for the EPMap.
    protected static class PSMFEntry {
        private int EPIndex;
        private int EPPicOffset;
        private int EPPts;
        private int EPOffset;
        private int id;

        public PSMFEntry(int id, int index, int picOffset, int pts, int offset) {
        	this.id = id;
            EPIndex = index;
            EPPicOffset = picOffset;
            EPPts = pts;
            EPOffset = offset;
        }

        public int getEntryIndex() {
            return EPIndex;
        }

        public int getEntryPicOffset() {
            return EPPicOffset;
        }

        public int getEntryPTS() {
            return EPPts;
        }

        public int getEntryOffset() {
            return EPOffset * MPEG_AVC_ES_SIZE;
        }

        public int getId() {
        	return id;
        }

		@Override
		public String toString() {
			return String.format("id=%d, index=0x%X, picOffset=0x%X, PTS=0x%X, offset=0x%X", getId(), getEntryIndex(), getEntryPicOffset(), getEntryPTS(), getEntryOffset());
		}
    }

    public static class PSMFHeader {
        private static final int size = 2048;

        // Header vars.
        public int mpegMagic;
        public int mpegRawVersion;
        public int mpegVersion;
        public int mpegOffset;
        public int mpegStreamSize;
        public int mpegFirstTimestamp;
        public int mpegLastTimestamp;
        public Date mpegFirstDate;
        public Date mpegLastDate;
        private int streamNum;
        private int audioSampleFrequency;
        private int audioChannelConfig;
        private int avcDetailFrameWidth;
        private int avcDetailFrameHeight;

        // Stream map.
        public List<PSMFStream> psmfStreams;
        private PSMFStream currentStream = null;
        private PSMFStream currentVideoStream = null;

        public PSMFHeader() {
        }

        public PSMFHeader(int bufferAddr, byte[] mpegHeader) {
            Memory mem = Memory.getInstance();

            int streamDataTotalSize = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, 0x50));
            int unk = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, 0x60));
            int streamDataNextBlockSize = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, 0x6A));      // General stream information block size.
            int streamDataNextInnerBlockSize = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, 0x7C)); // Inner stream information block size.
            streamNum = endianSwap16(read16(mem, bufferAddr, mpegHeader, PSMF_NUMBER_STREAMS_OFFSET));                  // Number of total registered streams.

            mpegMagic = read32(mem, bufferAddr, mpegHeader, PSMF_MAGIC_OFFSET);
            mpegRawVersion = read32(mem, bufferAddr, mpegHeader, PSMF_STREAM_VERSION_OFFSET);
            mpegVersion = getMpegVersion(mpegRawVersion);
            mpegOffset = endianSwap32(read32(mem, bufferAddr, mpegHeader, PSMF_STREAM_OFFSET_OFFSET));
            mpegStreamSize = endianSwap32(read32(mem, bufferAddr, mpegHeader, PSMF_STREAM_SIZE_OFFSET));
            mpegFirstTimestamp = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, PSMF_FIRST_TIMESTAMP_OFFSET));
            mpegLastTimestamp = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, PSMF_LAST_TIMESTAMP_OFFSET));
            avcDetailFrameWidth = read8(mem, bufferAddr, mpegHeader, PSMF_FRAME_WIDTH_OFFSET) << 4;
            avcDetailFrameHeight = read8(mem, bufferAddr, mpegHeader, PSMF_FRAME_HEIGHT_OFFSET) << 4;

            mpegFirstDate = convertTimestampToDate(mpegFirstTimestamp);
            mpegLastDate = convertTimestampToDate(mpegLastTimestamp);

            if (log.isDebugEnabled()) {
            	log.debug(String.format("PSMFHeader: version=0x%04X, firstTimestamp=%d, lastTimestamp=%d, streamDataTotalSize=%d, unk=0x%08X, streamDataNextBlockSize=%d, streamDataNextInnerBlockSize=%d, streamNum=%d", getVersion(), mpegFirstTimestamp, mpegLastTimestamp, streamDataTotalSize, unk, streamDataNextBlockSize, streamDataNextInnerBlockSize, streamNum));
            }

            if (isValid()) {
            	psmfStreams = readPsmfStreams(mem, bufferAddr, mpegHeader, this);

	            // PSP seems to default to stream 0.
	            if (psmfStreams.size() > 0) {
	            	setStreamNum(0);
	            }

	            // EPMap info:
	            // - Located at EPMapOffset (set by the AVC stream);
	            // - Each entry is composed by a total of 10 bytes:
	            //      - 1 byte: Reference picture index (RAPI);
	            //      - 1 byte: Reference picture offset from the current index;
	            //      - 4 bytes: PTS of the entry point;
	            //      - 4 bytes: Relative offset of the entry point in the MPEG data.
	            for (PSMFStream stream : psmfStreams) {
	            	stream.EPMap = new LinkedList<sceMpeg.PSMFEntry>();
	            	int EPMapOffset = stream.EPMapOffset;
		            for (int i = 0; i < stream.EPMapNumEntries; i++) {
		                int index = read8(mem, bufferAddr, mpegHeader, EPMapOffset + i * 10);
		                int picOffset = read8(mem, bufferAddr, mpegHeader, EPMapOffset + 1 + i * 10);
		                int pts = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, EPMapOffset + 2 + i * 10));
		                int offset = endianSwap32(readUnaligned32(mem, bufferAddr, mpegHeader, EPMapOffset + 6 + i * 10));
		                PSMFEntry psmfEntry = new PSMFEntry(i, index, picOffset, pts, offset);
		                stream.EPMap.add(psmfEntry);
		                if (log.isDebugEnabled()) {
		                	log.debug(String.format("EPMap stream %d, entry#%d: %s", stream.getStreamChannel(), i, psmfEntry));
		                }
		            }
	            }
            }
        }

        public boolean isValid() {
            return mpegFirstTimestamp == 90000 && mpegFirstTimestamp < mpegLastTimestamp && mpegLastTimestamp > 0;
        }

        public boolean isInvalid() {
        	return !isValid();
        }

        public int getVersion() {
            return mpegRawVersion;
        }

        public int getHeaderSize() {
            return size;
        }

        public int getStreamOffset() {
            return mpegOffset;
        }

        public int getStreamSize() {
            return mpegStreamSize;
        }

        public int getPresentationStartTime() {
            return (int) mpegFirstTimestamp;
        }

        public int getPresentationEndTime() {
            return (int) mpegLastTimestamp;
        }

        public int getVideoWidth() {
            return avcDetailFrameWidth;
        }

        public int getVideoHeight() {
            return avcDetailFrameHeight;
        }

        public int getAudioSampleFrequency() {
            return audioSampleFrequency;
        }

        public int getAudioChannelConfig() {
            return audioChannelConfig;
        }

        public int getEPMapEntriesNum() {
        	if (currentVideoStream == null) {
        		return 0;
        	}
            return currentVideoStream.EPMapNumEntries;
        }

        public boolean hasEPMap() {
        	return getEPMapEntriesNum() > 0;
        }

        public PSMFEntry getEPMapEntry(int id) {
        	if (!hasEPMap()) {
        		return null;
        	}
        	if (id < 0 || id >= currentVideoStream.EPMap.size()) {
        		return null;
        	}
            return currentVideoStream.EPMap.get(id);
        }

        public PSMFEntry getEPMapEntryWithTimestamp(int ts) {
        	if (!hasEPMap()) {
        		return null;
        	}

        	PSMFEntry foundEntry = null;
            for (PSMFEntry entry : currentVideoStream.EPMap) {
            	if (foundEntry == null || entry.getEntryPTS() <= ts) {
            		foundEntry = entry;
            	} else if (entry.getEntryPTS() > ts) {
                    break;
                }
            }

            return foundEntry;
        }

        public PSMFEntry getEPMapEntryWithOffset(int offset) {
        	if (!hasEPMap()) {
        		return null;
        	}

        	PSMFEntry foundEntry = null;
            for (PSMFEntry entry : currentVideoStream.EPMap) {
            	if (foundEntry == null || entry.getEntryOffset() <= offset) {
            		foundEntry = entry;
            	} else if (entry.getEntryOffset() > offset) {
                    break;
                }
            }

            return foundEntry;
        }

        public int getNumberOfStreams() {
            return streamNum;
        }

        public int getCurrentStreamNumber() {
        	if (!isValidCurrentStreamNumber()) {
        		return -1;
        	}
            return currentStream.getStreamNumber();
        }

        public boolean isValidCurrentStreamNumber() {
        	return currentStream != null;
        }

        public int getCurrentStreamType() {
        	if (!isValidCurrentStreamNumber()) {
        		return -1;
        	}
        	return currentStream.getStreamType();
        }

        public int getCurrentStreamChannel() {
        	if (!isValidCurrentStreamNumber()) {
        		return -1;
        	}
        	return currentStream.getStreamChannel();
        }

        public int getSpecificStreamNum(int type) {
        	int num = 0;
        	if (psmfStreams != null) {
	        	for (PSMFStream stream : psmfStreams) {
	        		if (stream.isStreamOfType(type)) {
	        			num++;
	        		}
	        	}
        	}

        	return num;
        }

        public void setStreamNum(int id) {
        	if (id < 0 || id >= psmfStreams.size()) {
        		currentStream = null;
        	} else {
        		currentStream = psmfStreams.get(id);

	            int type = getCurrentStreamType();
	            int channel = getCurrentStreamChannel();
	            switch (type) {
	            	case PSMF_AVC_STREAM:
	            		currentVideoStream = currentStream;
	            		Modules.sceMpegModule.setRegisteredVideoChannel(channel);
	            		break;
	            	case PSMF_PCM_STREAM:
	            	case PSMF_ATRAC_STREAM:
	            		Modules.sceMpegModule.setRegisteredAudioChannel(channel);
	            		break;
	            }
            }
        }

        private int getStreamNumber(int type, int typeNum, int channel) {
        	if (psmfStreams != null) {
	        	for (PSMFStream stream : psmfStreams) {
	        		if (stream.isStreamOfType(type)) {
	        			if (typeNum <= 0) {
	        				if (channel < 0 || stream.getStreamChannel() == channel) {
	        					return stream.getStreamNumber();
	        				}
	        			}
	    				typeNum--;
	        		}
	        	}
        	}

        	return -1;
        }

        public boolean setStreamWithType(int type, int channel) {
        	int streamNumber = getStreamNumber(type, 0, channel);
        	if (streamNumber < 0) {
        		return false;
        	}
        	setStreamNum(streamNumber);

    		return true;
        }

        public boolean setStreamWithTypeNum(int type, int typeNum) {
        	int streamNumber = getStreamNumber(type, typeNum, -1);
        	if (streamNumber < 0) {
        		return false;
        	}
        	setStreamNum(streamNumber);

    		return true;
        }
    }

    public static int getPsmfNumStreams(Memory mem, int addr, byte[] mpegHeader) {
    	return endianSwap16(read16(mem, addr, mpegHeader, sceMpeg.PSMF_NUMBER_STREAMS_OFFSET));    	
    }

    public static LinkedList<PSMFStream> readPsmfStreams(Memory mem, int addr, byte[] mpegHeader, PSMFHeader psmfHeader) {
    	int numStreams = getPsmfNumStreams(mem, addr, mpegHeader);

    	// Stream area:
        // At offset 0x82, each 16 bytes represent one stream.
        LinkedList<PSMFStream> streams = new LinkedList<PSMFStream>();

        // Parse the stream field and assign each one to it's type.
        int numberOfStreams = 0;
        for (int i = 0; i < numStreams; i++) {
            PSMFStream stream = null;
            int currentStreamOffset = 0x82 + i * 16;
            int streamID = read8(mem, addr, mpegHeader, currentStreamOffset);
            int subStreamID = read8(mem, addr, mpegHeader, currentStreamOffset + 1);
            if ((streamID & 0xF0) == PSMF_VIDEO_STREAM_ID) {
                stream = new PSMFStream(numberOfStreams);
                stream.readMPEGVideoStreamParams(mem, addr, mpegHeader, currentStreamOffset, psmfHeader);
            } else if (streamID == PSMF_AUDIO_STREAM_ID && subStreamID < 0x20) {
                stream = new PSMFStream(numberOfStreams);
                stream.readPrivateAudioStreamParams(mem, addr, mpegHeader, currentStreamOffset, psmfHeader);
            } else {
            	stream = new PSMFStream(numberOfStreams);
            	stream.readUserDataStreamParams(mem, addr, mpegHeader, currentStreamOffset, psmfHeader);
            }

            if (stream != null) {
                streams.add(stream);
                numberOfStreams++;
            }
        }

        return streams;
    }

    private class StreamInfo {
    	private int uid;
    	private final int type;
    	private final int channel;
    	private int auMode;

    	public StreamInfo(int type, int channel) {
    		this.type = type;
    		this.channel = channel;
    		uid = SceUidManager.getNewUid(streamPurpose);
    		setAuMode(MPEG_AU_MODE_DECODE);

    		streamMap.put(uid, this);
    	}

    	public int getUid() {
    		return uid;
    	}

    	public int getType() {
    		return type;
    	}

		public int getChannel() {
			return channel;
		}

		public void release() {
    		SceUidManager.releaseUid(uid, streamPurpose);
    		streamMap.remove(uid);
    	}

		public int getAuMode() {
			return auMode;
		}

		public void setAuMode(int auMode) {
			this.auMode = auMode;
		}

		public boolean isStreamType(int type) {
			if (this.type == type) {
				return true;
			}

			if (this.type == PSMF_ATRAC_STREAM && type == PSMF_AUDIO_STREAM) {
				return true;
			}

			return false;
		}

		@Override
		public String toString() {
			return String.format("StreamInfo(uid=0x%X, type=%d, auMode=%d)", getUid(), getType(), getAuMode());
		}
    }

    public class AfterRingbufferPutCallback implements IAction {
    	private int putDataAddr;
    	private int remainingPackets;
    	private int totalPacketsAdded;

    	public AfterRingbufferPutCallback(int putDataAddr, int remainingPackets) {
    		this.putDataAddr = putDataAddr;
    		this.remainingPackets = remainingPackets;
    	}

    	@Override
        public void execute() {
    		hleMpegRingbufferPostPut(this, Emulator.getProcessor().cpu._v0);
        }

		public int getPutDataAddr() {
			return putDataAddr;
		}

		public void setPutDataAddr(int putDataAddr) {
			this.putDataAddr = putDataAddr;
		}

		public int getRemainingPackets() {
			return remainingPackets;
		}

		public void setRemainingPackets(int remainingPackets) {
			this.remainingPackets = remainingPackets;
		}

		public int getTotalPacketsAdded() {
			return totalPacketsAdded;
		}

		public void addPacketsAdded(int packetsAdded) {
			if (packetsAdded > 0) {
				totalPacketsAdded += packetsAdded;
			}
		}
    }

    /**
     * Always decode one frame in advance so that sceMpegAvcDecode
     * can be timed like on a real PSP.
     */
    private class VideoDecoderThread extends Thread {
    	private volatile boolean exit = false;
    	private volatile boolean done = false;
    	private Semaphore sema = new Semaphore(0);
    	private int threadUid = -1;
    	private int buffer;
    	private int frameWidth;
    	private int pixelMode;
    	private TPointer32 gotFrameAddr;
    	private boolean writeAbgr;
    	private long threadWakeupMicroTime;

    	@Override
		public void run() {
    		while (!exit) {
    			if (waitForTrigger(100) && !exit) {
					hleVideoDecoderStep(threadUid, buffer, frameWidth, pixelMode, gotFrameAddr, writeAbgr, threadWakeupMicroTime);
    			}
    		}

    		done = true;
    	}

    	public void exit() {
    		exit = true;
    		trigger(-1, 0, 0, -1, null, false, 0L);

    		while (!done) {
    			Utilities.sleep(1);
    		}
    	}

    	public void trigger(int threadUid, int buffer, int frameWidth, int pixelMode, TPointer32 gotFrameAddr, boolean writeAbgr, long threadWakeupMicroTime) {
			this.threadUid = threadUid;
			this.buffer = buffer;
			this.frameWidth = frameWidth;
			this.pixelMode = pixelMode;
			this.gotFrameAddr = gotFrameAddr;
			this.writeAbgr = writeAbgr;
			this.threadWakeupMicroTime = threadWakeupMicroTime;

			trigger();
    	}

    	public void resetWaitingThreadInfo() {
			threadUid = -1;
			buffer = 0;
			frameWidth = 0;
			pixelMode = -1;
			gotFrameAddr = null;
			threadWakeupMicroTime = 0;
    	}

    	public void trigger() {
    		if (sema != null) {
    			sema.release();
    		}
    	}

    	private boolean waitForTrigger(int millis) {
    		while (true) {
    			try {
    				int availablePermits = sema.drainPermits();
    				if (availablePermits > 0) {
    					break;
    				}

    				if (sema.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
    					break;
    				}

    				return false;
    			} catch (InterruptedException e) {
    				// Ignore exception and retry
    			}
    		}

    		return true;
    	}
    }

    protected void mpegRingbufferWrite() {
    	if (mpegRingbuffer != null && mpegRingbufferAddr != null && mpegRingbufferAddr.isNotNull()) {
    		synchronized (mpegRingbuffer) {
    			mpegRingbuffer.notifyConsumed();
        		mpegRingbuffer.write(mpegRingbufferAddr);
			}
    	}
    }

    protected void mpegRingbufferNotifyRead() {
		synchronized (mpegRingbuffer) {
    		mpegRingbuffer.notifyRead();
    	}
    }

    protected void mpegRingbufferRead() {
    	if (mpegRingbuffer != null) {
    		if (mpegRingbufferAddr != null && mpegRingbufferAddr.isNotNull()) {
				synchronized (mpegRingbuffer) {
		    		mpegRingbuffer.read(mpegRingbufferAddr);
		    	}
    		}
        	mpegRingbuffer.setHasAudio(isRegisteredAudioChannel());
        	mpegRingbuffer.setHasVideo(isRegisteredVideoChannel());
        	mpegRingbuffer.setHasUserData(isRegisteredUserDataChannel());
    	}
    }

    private void rememberLastFrame(int imageWidth, int imageHeight, int abgr[]) {
    	if (abgr != null) {
    		releaseIntBuffer(lastFrameABGR);
    		int length = imageWidth * imageHeight;
    		lastFrameABGR = getIntBuffer(length);
    		System.arraycopy(abgr, 0, lastFrameABGR, 0, length);
    		lastFrameWidth = imageWidth;
    		lastFrameHeight = imageHeight;
    	}
    }

    public void writeLastFrameABGR(int buffer, int frameWidth, int pixelMode) {
    	if (lastFrameABGR != null) {
    		writeImageABGR(buffer, frameWidth, lastFrameWidth, lastFrameHeight, pixelMode, lastFrameABGR);
    	}
    }

    private boolean decodeImage(int buffer, int frameWidth, int pixelMode, TPointer32 gotFrameAddr, boolean writeAbgr) {
    	DecodedImageInfo decodedImageInfo;
    	synchronized (decodedImages) {
        	decodedImageInfo = decodedImages.pollFirst();
		}

    	if (decodedImageInfo == null) {
    		avcGotFrame = 0;
    		if (gotFrameAddr != null) {
    			gotFrameAddr.setValue(avcGotFrame);
    		}
    		return false;
    	}

    	avcGotFrame = decodedImageInfo.gotFrame ? 1 : 0;
    	if (gotFrameAddr != null) {
    		gotFrameAddr.setValue(avcGotFrame);
    	}

    	if (decodedImageInfo.gotFrame) {
    		if (writeAbgr) {
    			writeImageABGR(buffer, frameWidth, decodedImageInfo.imageWidth, decodedImageInfo.imageHeight, pixelMode, decodedImageInfo.abgr);
    		} else {
    			writeImageYCbCr(buffer, decodedImageInfo.imageWidth, decodedImageInfo.imageHeight, decodedImageInfo.luma, decodedImageInfo.cb, decodedImageInfo.cr);
    		}

    		rememberLastFrame(decodedImageInfo.imageWidth, decodedImageInfo.imageHeight, decodedImageInfo.abgr);

    		releaseIntBuffer(decodedImageInfo.luma);
    		releaseIntBuffer(decodedImageInfo.cb);
    		releaseIntBuffer(decodedImageInfo.cr);
    		releaseIntBuffer(decodedImageInfo.abgr);
    		decodedImageInfo.luma = null;
    		decodedImageInfo.cb = null;
    		decodedImageInfo.cr = null;
    		decodedImageInfo.abgr = null;

    		videoFrameCount++;
    	}

    	return true;
    }

    private void restartThread(int threadUid, int buffer, int frameWidth, int pixelMode, TPointer32 gotFrameAddr, boolean writeAbgr, long threadWakeupMicroTime) {
    	if (threadUid < 0) {
    		return;
    	}

    	if (!decodeImage(buffer, frameWidth, pixelMode, gotFrameAddr, writeAbgr)) {
    		return;
    	}

    	videoDecoderThread.resetWaitingThreadInfo();

		IAction action;
    	int delayMicros = (int) (threadWakeupMicroTime - Emulator.getClock().microTime());
    	if (delayMicros > 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Further delaying thread=0x%X by %d microseconds", threadUid, delayMicros));
    		}
    		action = new DelayThreadAction(threadUid, delayMicros, false, true);
    	} else {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Unblocking thread=0x%X", threadUid));
    		}
    		action = new UnblockThreadAction(threadUid);
    	}
    	// The action cannot be executed immediately as we are running
    	// in a non-PSP thread. The action has to be executed by the scheduler
    	// as soon as possible.
		Emulator.getScheduler().addAction(action);
    }

    private boolean isDecoderInErrorCondition() {
    	synchronized (decodedImages) {
	    	if (decodedImages.size() == 0) {
	    		return false;
	    	}
	
	    	DecodedImageInfo decodedImageInfo = decodedImages.peek();
	    	if (decodedImageInfo.frameEnd >= 0 || decodedImageInfo.gotFrame) {
	    		return false;
	    	}

	    	return true;
    	}
    }

    private void removeErrorImages() {
    	synchronized (decodedImages) {
	    	while (isDecoderInErrorCondition()) {
	    		if (log.isDebugEnabled()) {
	    			log.debug(String.format("Removing error image %s", decodedImages.peek()));
	    		}
				decodedImages.remove();
	    	}
    	}
    }

    private void decodeNextImage() {
		PesHeader pesHeader = new PesHeader(getRegisteredVideoChannel());
		pesHeader.setDtsPts(UNKNOWN_TIMESTAMP);

		DecodedImageInfo decodedImageInfo = new DecodedImageInfo();
    	decodedImageInfo.frameEnd = readNextVideoFrame(pesHeader);

		if (decodedImageInfo.frameEnd >= 0) {
			if (videoBuffer.getLength() < decodedImageInfo.frameEnd) {
				// The content of the frame is not yet completely available in the videoBuffer
				return;
			}

			if (videoCodec == null) {
    			videoCodec = CodecFactory.getVideoCodec();
    			videoCodec.init(videoCodecExtraData);
    		}

			synchronized (videoBuffer) {
				int result = videoCodec.decode(videoBuffer.getBuffer(), videoBuffer.getBufferOffset(), decodedImageInfo.frameEnd);

	    		if (log.isTraceEnabled()) {
					byte bytes[] = new byte[decodedImageInfo.frameEnd];
					int inputBuffer[] = videoBuffer.getBuffer();
					int inputOffset = videoBuffer.getBufferOffset();
					for (int i = 0; i < decodedImageInfo.frameEnd; i++) {
						bytes[i] = (byte) inputBuffer[inputOffset + i];
					}
					log.trace(String.format("decodeNextImage codec returned 0x%X. Decoding 0x%X bytes from %s", result, decodedImageInfo.frameEnd, Utilities.getMemoryDump(bytes, 0, decodedImageInfo.frameEnd)));
				}

				if (result < 0) {
					log.error(String.format("decodeNextImage codec returned 0x%08X", result));
					// Skip this incorrect frame
					videoBuffer.notifyRead(decodedImageInfo.frameEnd);
					decodedImageInfo.gotFrame = false;
				} else {
					videoBuffer.notifyRead(result);

					decodedImageInfo.gotFrame = videoCodec.hasImage();
					if (decodedImageInfo.gotFrame) {
						decodedImageInfo.imageWidth = videoCodec.getImageWidth();
						decodedImageInfo.imageHeight = videoCodec.getImageHeight();
						if (!getImage(decodedImageInfo)) {
							return;
						}
					}
				}
			}
    	}

    	if (videoPesHeader == null) {
    		videoPesHeader = new PesHeader(pesHeader);
    		pesHeader.setDtsPts(UNKNOWN_TIMESTAMP);
    	}

		decodedImageInfo.pesHeader = videoPesHeader;

		videoPesHeader = pesHeader;

		if (decodedImageInfo.gotFrame) {
			removeErrorImages();
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Adding decoded image %s", decodedImageInfo));
		}
    	synchronized (decodedImages) {
    		decodedImages.add(decodedImageInfo);
    	}
    }

    private void hleVideoDecoderStep(int threadUid, int buffer, int frameWidth, int pixelMode, TPointer32 gotFrameAddr, boolean writeAbgr, long threadWakeupMicroTime) {
    	if (log.isDebugEnabled()) {
    		if (threadUid >= 0) {
    			log.debug(String.format("hleVideoDecoderStep threadUid=0x%X, buffer=0x%08X, frameWidth=%d, pixelMode=%d, gotFrameAddr=%s, writeAbgr=%b, %d decoded images", threadUid, buffer, frameWidth, pixelMode, gotFrameAddr, writeAbgr, decodedImages.size()));
    		} else {
    			log.debug(String.format("hleVideoDecoderStep %d decoded images", decodedImages.size()));
    		}
    	}

    	restartThread(threadUid, buffer, frameWidth, pixelMode, gotFrameAddr, writeAbgr, threadWakeupMicroTime);

    	// Always decode one frame in advance
		if (decodedImages.size() <= 1 || isDecoderInErrorCondition()) {
			decodeNextImage();
		}
    }

    private int read32(Memory mem, pspFileBuffer buffer) {
    	if (buffer.getCurrentSize() < 4) {
    		return 0;
    	}

    	int addr = buffer.getReadAddr();
    	int value;

    	if (buffer.getReadSize() >= 4) {
    		value = endianSwap32(Utilities.readUnaligned32(mem, addr));
    		buffer.notifyRead(4);
    	} else {
    		value = read8(mem, buffer);
    		value = (value << 8) | read8(mem, buffer);
    		value = (value << 8) | read8(mem, buffer);
    		value = (value << 8) | read8(mem, buffer);
    	}

    	return value;
    }

    private int read16(Memory mem, pspFileBuffer buffer) {
    	if (buffer.getCurrentSize() < 2) {
    		return 0;
    	}

    	int addr = buffer.getReadAddr();
    	int value;

    	if (buffer.getReadSize() >= 2) {
    		value = endianSwap16(readUnaligned16(mem, addr));
    		buffer.notifyRead(2);
    	} else {
    		value = (read8(mem, buffer) << 8) | read8(mem, buffer);
    	}

    	return value;
    }

    private int read8(Memory mem, pspFileBuffer buffer) {
    	if (buffer.getCurrentSize() < 1) {
    		return 0;
    	}

    	int addr = buffer.getReadAddr();
    	int value = mem.read8(addr);
    	buffer.notifyRead(1);

    	return value;
    }

    private void skip(pspFileBuffer buffer, int n) {
    	buffer.notifyRead(n);
    }

    private void addToAudioBuffer(Memory mem, pspFileBuffer buffer, int length) {
    	while (length > 0) {
    		int currentFrameLength = audioFrameLength == 0 ? 0 : audioBuffer.getLength() % audioFrameLength;
    		if (currentFrameLength == 0) {
    			// 8 bytes header:
    			// - byte 0: 0x0F
    			// - byte 1: 0xD0
    			// - byte 2: 0x28
    			// - byte 3: (frameLength - 8) / 8
    			// - bytes 4-7: 0x00
    			if (log.isTraceEnabled()) {
    				log.trace(String.format("Reading an audio frame from 0x%08X (length=0x%X) to the Audio buffer (already read %d)", buffer.getReadAddr(), length, frameHeaderLength));
    			}

    			while (frameHeaderLength < frameHeader.length && length > 0) {
    				frameHeader[frameHeaderLength++] = read8(mem, buffer);
    				length--;
    			}
    			if (frameHeaderLength < frameHeader.length) {
    				// Frame header not yet complete
    				break;
    			}
    			if (length == 0) {
    				// Frame header is complete but no data is following the header.
    				// Retry when some data is available
    				break;
    			}

    			int frameHeader23 = (frameHeader[2] << 8) | frameHeader[3];
    			audioFrameLength = ((frameHeader23 & 0x3FF) << 3) + 8;
    			if (frameHeader[0] != 0x0F || frameHeader[1] != 0xD0) {
    				if (log.isInfoEnabled()) {
    					log.warn(String.format("Audio frame length 0x%X with incorrect header (header: %02X %02X %02X %02X %02X %02X %02X %02X)", audioFrameLength, frameHeader[0], frameHeader[1], frameHeader[2], frameHeader[3], frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7]));
    				}
    			} else if (log.isTraceEnabled()) {
    				log.trace(String.format("Audio frame length 0x%X (header: %02X %02X %02X %02X %02X %02X %02X %02X)", audioFrameLength, frameHeader[0], frameHeader[1], frameHeader[2], frameHeader[3], frameHeader[4], frameHeader[5], frameHeader[6], frameHeader[7]));
    			}

    			frameHeaderLength = 0;
    		}
    		int lengthToNextFrame = audioFrameLength - currentFrameLength;
    		int readLength = Utilities.min(length, buffer.getReadSize(), lengthToNextFrame);
    		int addr = buffer.getReadAddr();
    		if (audioBuffer.write(mem, addr, readLength) != readLength) {
    			log.error(String.format("AudioBuffer too small"));
    		}
    		buffer.notifyRead(readLength);
    		length -= readLength;
    	}
    }

    private void addToVideoBuffer(Memory mem, pspFileBuffer buffer, int length) {
    	while (length > 0) {
    		int readLength = Math.min(length, buffer.getReadSize());
    		int addr = buffer.getReadAddr();
    		addToVideoBuffer(mem, addr, readLength);
    		buffer.notifyRead(readLength);
    		length -= readLength;
    	}
    }

    public void addToVideoBuffer(Memory mem, int addr, int length) {
		videoBuffer.write(mem, addr, length);
    }

    private void addToUserDataBuffer(Memory mem, pspFileBuffer buffer, int length) {
    	while (length > 0) {
    		int readLength = Math.min(length, buffer.getReadSize());
    		int addr = buffer.getReadAddr();
    		userDataBuffer.write(mem, addr, readLength);
    		buffer.notifyRead(readLength);
    		length -= readLength;
    	}
    }

    private long readPts(Memory mem, pspFileBuffer buffer) {
		return readPts(mem, buffer, read8(mem, buffer));
	}

	private long readPts(Memory mem, pspFileBuffer buffer, int c) {
		return (((long) (c & 0x0E)) << 29) | ((read16(mem, buffer) >> 1) << 15) | (read16(mem, buffer) >> 1);
	}

    private int readPesHeader(Memory mem, pspFileBuffer buffer, PesHeader pesHeader, int length, int startCode) {
		int c = 0;
		while (length > 0) {
			c = read8(mem, buffer);
			length--;
			if (c != 0xFF) {
				break;
			}
		}

		if ((c & 0xC0) == 0x40) {
			skip(buffer, 1);
			c = read8(mem, buffer);
			length -= 2;
		}
		pesHeader.setDtsPts(UNKNOWN_TIMESTAMP);
		if ((c & 0xE0) == 0x20) {
			pesHeader.setDtsPts(readPts(mem, buffer, c));
			length -= 4;
			if ((c & 0x10) != 0) {
				pesHeader.setDts(readPts(mem, buffer));
				length -= 5;
			}
		} else if ((c & 0xC0) == 0x80) {
			int flags = read8(mem, buffer);
			int headerLength = read8(mem, buffer);
			length -= 2;
			length -= headerLength;
			if ((flags & 0x80) != 0) {
				pesHeader.setDtsPts(readPts(mem, buffer));
				headerLength -= 5;
				if ((flags & 0x40) != 0) {
					pesHeader.setDts(readPts(mem, buffer));
					headerLength -= 5;
				}
			}
			if ((flags & 0x3F) != 0 && headerLength == 0) {
				flags &= 0xC0;
			}
			if ((flags & 0x01) != 0) {
				int pesExt = read8(mem, buffer);
				headerLength--;
				int skip = (pesExt >> 4) & 0x0B;
				skip += skip & 0x09;
				if ((pesExt & 0x40) != 0 || skip > headerLength) {
					pesExt = skip = 0;
				}
				skip(buffer, skip);
				headerLength -= skip;
				if ((pesExt & 0x01) != 0) {
					int ext2Length = read8(mem, buffer);
					headerLength--;
					 if ((ext2Length & 0x7F) != 0) {
						 int idExt = read8(mem, buffer);
						 headerLength--;
						 if ((idExt & 0x80) == 0) {
							 startCode = ((startCode & 0xFF) << 8) | idExt;
						 }
					 }
				}
			}
			skip(buffer, headerLength);
		}

		if (startCode == PRIVATE_STREAM_1) {
			int channel = read8(mem, buffer);
			pesHeader.setChannel(channel);
			length--;
			if (channel >= 0x80 && channel <= 0xCF) {
				// Skip audio header
				skip(buffer, 3);
				length -= 3;
				if (channel >= 0xB0 && channel <= 0xBF) {
					skip(buffer, 1);
					length--;
				}
			} else if (channel >= 0x20) {
				// Userdata
				skip(buffer, 1);
				length--;
			} else {
				// PSP audio has additional 3 bytes in header
				skip(buffer, 3);
				length -= 3;
			}
		}

		return length;
    }

    private void readNextAudioFrame(PesHeader pesHeader) {
    	if (mpegRingbuffer == null) {
    		return;
    	}

    	Memory mem = Memory.getInstance();
    	pspFileBuffer buffer = mpegRingbuffer.getAudioBuffer();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readNextAudioFrame %s", mpegRingbuffer));
    	}
    	int audioChannel = getRegisteredAudioChannel();

    	while (!buffer.isEmpty() && (audioFrameLength == 0 || audioBuffer.getLength() < audioFrameLength)) {
    		int startCode = read32(mem, buffer);
    		int codeLength;
    		switch (startCode) {
    			case PACK_START_CODE:
    				skip(buffer, 10);
    				break;
    			case SYSTEM_HEADER_START_CODE:
    				skip(buffer, 14);
    				break;
				case PADDING_STREAM:
				case PRIVATE_STREAM_2:
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3: // Video streams
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF:
					codeLength = read16(mem, buffer);
					skip(buffer, codeLength);
					break;
				case PRIVATE_STREAM_1:
					// Audio stream
					codeLength = read16(mem, buffer);
					codeLength = readPesHeader(mem, buffer, pesHeader, codeLength, startCode);
					if (pesHeader.getChannel() == audioChannel || audioChannel < 0) {
						addToAudioBuffer(mem, buffer, codeLength);
					} else {
						skip(buffer, codeLength);
					}
					break;
				default:
					log.warn(String.format("Unknown StartCode 0x%08X at 0x%08X", startCode, buffer.getReadAddr() - 4));
					break;
    		}
    	}

    	mpegRingbufferNotifyRead();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("After readNextAudioFrame %s", mpegRingbuffer));
    	}
    }

    private int readNextVideoFrame(PesHeader pesHeader) {
    	if (mpegRingbuffer == null) {
    		return -1;
    	}

    	Memory mem = Memory.getInstance();
    	pspFileBuffer buffer = mpegRingbuffer.getVideoBuffer();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readNextVideoFrame %s", mpegRingbuffer));
    	}
    	int videoChannel = getRegisteredVideoChannel();

    	int frameEnd = videoBuffer.findFrameEnd();
    	while (!buffer.isEmpty() && frameEnd < 0) {
    		int startCode = read32(mem, buffer);
    		int codeLength;
    		switch (startCode) {
    			case PACK_START_CODE:
    				skip(buffer, 10);
    				break;
    			case SYSTEM_HEADER_START_CODE:
    				skip(buffer, 14);
    				break;
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3: // Video streams
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF:
					codeLength = read16(mem, buffer);
					codeLength = readPesHeader(mem, buffer, pesHeader, codeLength, startCode);
					if (videoChannel < 0 || videoChannel == startCode - 0x1E0) {
						addToVideoBuffer(mem, buffer, codeLength);
			    		frameEnd = videoBuffer.findFrameEnd();
			    		// Ignore next PES headers for this current video frame
			    		pesHeader = dummyPesHeader;
					} else {
						skip(buffer, codeLength);
					}
					break;
				case PADDING_STREAM:
				case PRIVATE_STREAM_2:
				case PRIVATE_STREAM_1: // Audio stream
					codeLength = read16(mem, buffer);
					skip(buffer, codeLength);
					break;
				default:
					log.warn(String.format("Unknown StartCode 0x%08X at 0x%08X", startCode, buffer.getReadAddr() - 4));
					break;
    		}
    	}

    	// Reaching the last frame?
    	if (frameEnd < 0 && buffer.isEmpty() && !videoBuffer.isEmpty()) {
    		if (psmfHeader == null || currentVideoTimestamp >= psmfHeader.mpegLastTimestamp) {
	    		// There is no next frame any more but the video buffer is not yet empty,
    			// so use the rest of the video buffer
	    		frameEnd = videoBuffer.getLength();
    		}
    	}

    	mpegRingbufferNotifyRead();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("After readNextVideoFrame frameEnd=0x%X, %s", frameEnd, mpegRingbuffer));
    	}

    	return frameEnd;
    }

    private void readNextUserDataFrame(PesHeader pesHeader) {
    	if (mpegRingbuffer == null) {
    		return;
    	}

    	Memory mem = Memory.getInstance();
    	pspFileBuffer buffer = mpegRingbuffer.getUserDataBuffer();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("readNextUserDataFrame %s", mpegRingbuffer));
    	}
    	int userDataChannel = 0x20 + getRegisteredUserDataChannel();

    	while (!buffer.isEmpty() && (userDataLength == 0 || userDataBuffer.getLength() < userDataLength)) {
    		int startCode = read32(mem, buffer);
    		int codeLength;
    		switch (startCode) {
    			case PACK_START_CODE:
    				skip(buffer, 10);
    				break;
    			case SYSTEM_HEADER_START_CODE:
    				skip(buffer, 14);
    				break;
				case PADDING_STREAM:
				case PRIVATE_STREAM_2:
				case 0x1E0: case 0x1E1: case 0x1E2: case 0x1E3: // Video streams
				case 0x1E4: case 0x1E5: case 0x1E6: case 0x1E7:
				case 0x1E8: case 0x1E9: case 0x1EA: case 0x1EB:
				case 0x1EC: case 0x1ED: case 0x1EE: case 0x1EF:
					codeLength = read16(mem, buffer);
					skip(buffer, codeLength);
					break;
				case PRIVATE_STREAM_1:
					// Audio/Userdata stream
					if (userDataLength > 0) {
			    		// Keep only the PES header of the first data chunk
			    		pesHeader = dummyPesHeader;
					}
					codeLength = read16(mem, buffer);
					codeLength = readPesHeader(mem, buffer, pesHeader, codeLength, startCode);
					if (pesHeader.getChannel() == userDataChannel) {
						if (userDataLength == 0) {
							for (int i = 0; i < userDataHeader.length; i++) {
								userDataHeader[i] = read8(mem, buffer);
								codeLength--;
							}
							userDataLength = ((userDataHeader[0] << 24) |
							                  (userDataHeader[1] << 16) |
							                  (userDataHeader[2] <<  8) |
							                  (userDataHeader[3] <<  0)) - 4;
						}
						addToUserDataBuffer(mem, buffer, codeLength);
					} else {
						skip(buffer, codeLength);
					}
					break;
				default:
					log.warn(String.format("Unknown StartCode 0x%08X at 0x%08X", startCode, buffer.getReadAddr() - 4));
					break;
    		}
    	}

    	mpegRingbufferNotifyRead();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("After readNextUserDataFrame %s", mpegRingbuffer));
    	}
    }

    public void setVideoFrameHeight(int videoFrameHeight) {
    	this.videoFrameHeight = videoFrameHeight;
    }

    private int getFrameHeight(int imageHeight) {
    	int frameHeight = imageHeight;
		if (psmfHeader != null) {
			// The decoded image height can be 290 while the header
			// gives an height of 272.
			frameHeight = Math.min(frameHeight, psmfHeader.getVideoHeight());
		} else if (videoFrameHeight >= 0) {
			// The decoded image height can be 290 while the MP4 header
			// gives an height of 272.
			frameHeight = Math.min(frameHeight, videoFrameHeight);
		}

		return frameHeight;
    }

    private void writeImageABGR(int addr, int frameWidth, int imageWidth, int imageHeight, int pixelMode, int[] abgr) {
    	int frameHeight = getFrameHeight(imageHeight);
		int bytesPerPixel = sceDisplay.getPixelFormatBytes(pixelMode);

		if (log.isDebugEnabled()) {
			log.debug(String.format("writeImageABGR addr=0x%08X-0x%08X, frameWidth=%d, frameHeight=%d, width=%d, height=%d, pixelMode=%d", addr, addr + frameWidth * frameHeight * bytesPerPixel, frameWidth, frameHeight, imageWidth, imageHeight, pixelMode));
		}

		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, frameWidth * frameHeight * bytesPerPixel, bytesPerPixel);
		int lineWidth = Math.min(imageWidth, frameWidth);
		int lineSkip = frameWidth - lineWidth;

		if (pixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 && memoryInt != null) {
			// Optimize the most common case
			int offset = 0;
			int memoryIntOffset = addr >> 2;
			for (int y = 0; y < frameHeight; y++) {
				System.arraycopy(abgr, offset, memoryInt, memoryIntOffset, lineWidth);
				memoryIntOffset += frameWidth;
				offset += imageWidth;
			}
		} else {
			// The general case with color format transformation
			for (int y = 0; y < frameHeight; y++) {
				int offset = y * imageWidth;
				for (int x = 0; x < lineWidth; x++, offset++) {
	                int pixelColor = Debug.getPixelColor(abgr[offset], pixelMode);
					memoryWriter.writeNext(pixelColor);
				}
				memoryWriter.skip(lineSkip);
			}
		}
		memoryWriter.flush();
    }

    private void writeImageYCbCr(int addr, int imageWidth, int imageHeight, int[] luma, int[] cb, int[] cr) {
    	int frameWidth = imageWidth;
    	int frameHeight = imageHeight;
		if (psmfHeader != null) {
			// The decoded image height can be 290 while the header
			// gives an height of 272.
			frameHeight = Math.min(frameHeight, psmfHeader.getVideoHeight());
		}

		int width2 = frameWidth >> 1;
		int height2 = frameHeight >> 1;
		int length = frameWidth * frameHeight;
		int length2 = width2 * height2;

		if (log.isDebugEnabled()) {
			log.debug(String.format("writeImageYCbCr addr=0x%08X-0x%08X, frameWidth=%d, frameHeight=%d", addr, addr + length + length2 + length2, frameWidth, frameHeight));
		}

		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, length, 1);
		for (int i = 0; i < length; i++) {
			memoryWriter.writeNext(luma[i] & 0xFF);
		}
		for (int i = 0; i < length2; i++) {
			memoryWriter.writeNext(cb[i] & 0xFF);
		}
		for (int i = 0; i < length2; i++) {
			memoryWriter.writeNext(cr[i] & 0xFF);
		}
		memoryWriter.flush();
    }

    private int[] getIntBuffer(int length) {
    	synchronized (intBuffers) {
        	for (int[] intBuffer : intBuffers) {
        		if (intBuffer.length >= length) {
        			intBuffers.remove(intBuffer);
        			return intBuffer;
        		}
        	}
		}

    	return new int[length];
    }

    private void releaseIntBuffer(int[] intBuffer) {
    	if (intBuffer == null) {
    		return;
    	}

    	synchronized (intBuffers) {
        	intBuffers.add(intBuffer);

	    	if (intBuffers.size() > MAX_INT_BUFFERS_SIZE) {
	    		// Remove the smallest int buffer
	    		int[] smallestIntBuffer = null;
	    		for (int[] buffer : intBuffers) {
	    			if (smallestIntBuffer == null || buffer.length < smallestIntBuffer.length) {
	    				smallestIntBuffer = buffer;
	    			}
	    		}
	
	    		intBuffers.remove(smallestIntBuffer);
	    	}
    	}
    }

    private boolean getImage(DecodedImageInfo decodedImageInfo) {
    	int width = videoCodec.getImageWidth();
    	int height = videoCodec.getImageHeight();
    	int width2 = width >> 1;
		int height2 = height >> 1;
    	int length = width * height;
		int length2 = width2 * height2;

		// Allocate buffers
		decodedImageInfo.luma = getIntBuffer(length);
		decodedImageInfo.cb = getIntBuffer(length2);
		decodedImageInfo.cr = getIntBuffer(length2);
		int result = videoCodec.getImage(decodedImageInfo.luma, decodedImageInfo.cb, decodedImageInfo.cr);
		if (result < 0) {
			log.error(String.format("VideoCodec error 0x%08X while retrieving the image", result));
			return false;
		}

		decodedImageInfo.abgr = getIntBuffer(length);
		H264Utils.YUV2ABGR(width, height, decodedImageInfo.luma, decodedImageInfo.cb, decodedImageInfo.cr, decodedImageInfo.abgr);

		return true;
    }

    public int hleMpegCreate(TPointer mpeg, TPointer data, int size, @CanBeNull TPointer ringbufferAddr, int frameWidth, int mode, int ddrtop) {
        Memory mem = data.getMemory();

        // Check size.
        if (size < MPEG_MEMSIZE) {
            log.warn("sceMpegCreate bad size " + size);
            return SceKernelErrors.ERROR_MPEG_NO_MEMORY;
        }

        finishStreams();

        // Update the ring buffer struct.
        if (ringbufferAddr != null && ringbufferAddr.isNotNull()) {
        	mpegRingbuffer = SceMpegRingbuffer.fromMem(ringbufferAddr);
        	mpegRingbuffer.reset();
	        mpegRingbuffer.setMpeg(mpeg.getAddress());
	    	mpegRingbufferWrite();
        }

        // Write mpeg system handle.
        mpegHandle = data.getAddress() + 0x30;
        mpeg.setValue32(mpegHandle);

        // Initialize fake mpeg struct.
        Utilities.writeStringZ(mem, mpegHandle, "LIBMPEG.001");
        mem.write32(mpegHandle + 12, -1);
        if (ringbufferAddr != null) {
        	mem.write32(mpegHandle + 16, ringbufferAddr.getAddress());
        }
        if (mpegRingbuffer != null) {
        	mem.write32(mpegHandle + 20, mpegRingbuffer.getUpperDataAddr());
        }

        // Initialize mpeg values.
        mpegRingbufferAddr = ringbufferAddr;
        videoFrameCount = 0;
        audioFrameCount = 0;
        currentVideoTimestamp = 0;
        currentAudioTimestamp = 0;
        videoPixelMode = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
        defaultFrameWidth = frameWidth;

        audioBuffer = new AudioBuffer(data.getAddress() + AUDIO_BUFFER_OFFSET, AUDIO_BUFFER_SIZE);
        videoBuffer = new VideoBuffer();

        decodedImages = new LinkedList<sceMpeg.DecodedImageInfo>();

        if (videoDecoderThread == null) {
	        videoDecoderThread = new VideoDecoderThread();
	        videoDecoderThread.setDaemon(true);
	        videoDecoderThread.setName("Video Decoder Thread");
	        videoDecoderThread.start();
        } else {
        	videoDecoderThread.resetWaitingThreadInfo();
        }

        return 0;
    }

    public void hleMpegNotifyVideoDecoderThread() {
        if (videoDecoderThread != null) {
        	videoDecoderThread.trigger();
        }
    }

    protected void hleMpegRingbufferPostPut(AfterRingbufferPutCallback afterRingbufferPutCallback, int packetsAdded) {
        int putDataAddr = afterRingbufferPutCallback.getPutDataAddr();
        int remainingPackets = afterRingbufferPutCallback.getRemainingPackets();
        mpegRingbufferRead();

        if (packetsAdded > 0) {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("hleMpegRingbufferPostPut:%s", Utilities.getMemoryDump(putDataAddr, packetsAdded * mpegRingbuffer.getPacketSize())));
        	}

            if (packetsAdded > mpegRingbuffer.getFreePackets()) {
                log.warn(String.format("sceMpegRingbufferPut clamping packetsAdded old=%d, new=%d", packetsAdded, mpegRingbuffer.getFreePackets()));
                packetsAdded = mpegRingbuffer.getFreePackets();
            }
            mpegRingbuffer.addPackets(packetsAdded);
        	mpegRingbufferWrite();

            afterRingbufferPutCallback.addPacketsAdded(packetsAdded);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceMpegRingbufferPut packetsAdded=0x%X, packetsRead=0x%X, new availableSize=0x%X", packetsAdded, mpegRingbuffer.getReadPackets(), mpegRingbuffer.getFreePackets()));
            }

            removeErrorImages();
            hleMpegNotifyVideoDecoderThread();

            if (remainingPackets > 0) {
                int putNumberPackets = Math.min(remainingPackets, mpegRingbuffer.getPutSequentialPackets());
                putDataAddr = mpegRingbuffer.getPutDataAddr();
                afterRingbufferPutCallback.setPutDataAddr(putDataAddr);
                afterRingbufferPutCallback.setRemainingPackets(remainingPackets - putNumberPackets);

                if (log.isDebugEnabled()) {
                	log.debug(String.format("sceMpegRingbufferPut executing callback 0x%08X to read 0x%X packets at 0x%08X", mpegRingbuffer.getCallbackAddr(), putNumberPackets, putDataAddr));
                }
                Modules.ThreadManForUserModule.executeCallback(null, mpegRingbuffer.getCallbackAddr(), afterRingbufferPutCallback, false, putDataAddr, putNumberPackets, mpegRingbuffer.getCallbackArgs());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceMpegRingbufferPut callback returning packetsAdded=0x%X", packetsAdded));
            }
        }
    }

    public void hleCreateRingbuffer() {
		mpegRingbuffer = new SceMpegRingbuffer(0, 0, 0, 0, 0);
		mpegRingbuffer.setReadPackets(1);
		mpegRingbufferAddr = null;
    }

    public void setVideoCodecExtraData(int videoCodecExtraData[]) {
    	this.videoCodecExtraData = videoCodecExtraData;
    }

    public void setVideoFrameSizes(int videoFrameSizes[]) {
    	videoBuffer.setFrameSizes(videoFrameSizes);
    }

    public void flushVideoFrameData() {
    	videoBuffer.reset();
    }

    public void setVideoFrame(int frame) {
    	videoBuffer.setFrame(frame);
    }

    public void hleCreateRingbuffer(int packets, int data, int size) {
		mpegRingbuffer = new SceMpegRingbuffer(packets, data, size, 0, 0);
		mpegRingbufferAddr = null;
    }

    public SceMpegRingbuffer getMpegRingbuffer() {
    	return mpegRingbuffer;
    }

    public PSMFHeader getPsmfHeader() {
    	return psmfHeader;
    }

    private int getRegisteredChannel(int streamType, int registeredChannel) {
    	int channel = -1;
    	for (StreamInfo stream : streamMap.values()) {
    		if (stream != null && stream.isStreamType(streamType) && stream.getAuMode() == MPEG_AU_MODE_DECODE) {
	    		if (channel < 0 || stream.getChannel() < channel) {
	    			channel = stream.getChannel();
	    			if (channel == registeredChannel) {
	    				// We have found the registered channel
	    				break;
	    			}
	    		}
    		}
    	}

    	if (channel < 0) {
    		channel = registeredChannel;
    	}

    	return channel;
    }

    public int getRegisteredAudioChannel() {
    	return getRegisteredChannel(PSMF_ATRAC_STREAM, registeredAudioChannel);
    }

    public boolean isRegisteredAudioChannel() {
    	return getRegisteredAudioChannel() >= 0;
    }

    public int getRegisteredVideoChannel() {
    	return getRegisteredChannel(PSMF_AVC_STREAM, registeredVideoChannel);
    }

    public boolean isRegisteredVideoChannel() {
    	return getRegisteredVideoChannel() >= 0;
    }

    public boolean isRegisteredUserDataChannel() {
    	return getRegisteredUserDataChannel() >= 0;
    }

    public int getRegisteredPcmChannel() {
    	return getRegisteredChannel(PSMF_PCM_STREAM, -1);
    }

    public int getRegisteredUserDataChannel() {
    	return getRegisteredChannel(PSMF_DATA_STREAM, -1);
    }

    public void setRegisteredVideoChannel(int registeredVideoChannel) {
    	if (this.registeredVideoChannel != registeredVideoChannel) {
    		this.registeredVideoChannel = registeredVideoChannel;
    	}
    }

    public void setRegisteredAudioChannel(int registeredAudioChannel) {
    	this.registeredAudioChannel = registeredAudioChannel;
    }

    public long getCurrentVideoTimestamp() {
    	return currentVideoTimestamp;
    }

    public long getCurrentAudioTimestamp() {
    	return currentAudioTimestamp;
    }

    public int hleMpegGetAvcAu(TPointer auAddr) {
    	int result = 0;

    	// Read Au of next Avc frame
        if (isRegisteredVideoChannel()) {
        	DecodedImageInfo decodedImageInfo;
        	while (true) {
            	synchronized (decodedImages) {
            		decodedImageInfo = decodedImages.peek();
            	}
        		if (decodedImageInfo != null) {
        			break;
        		}
        		// Wait for the video decoder thread
        		if (log.isDebugEnabled()) {
        			log.debug(String.format("hleMpegGetAvcAu waiting for the video decoder thread..."));
        		}
        		Utilities.sleep(1);
        	}

        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleMpegGetAvcAu decodedImageInfo: %s", decodedImageInfo));
        	}
    		mpegAvcAu.pts = decodedImageInfo.pesHeader.getPts();
    		mpegAvcAu.dts = decodedImageInfo.pesHeader.getDts();
    		mpegAvcAu.esSize = Math.max(0, decodedImageInfo.frameEnd);
        	if (auAddr != null && auAddr.isNotNull()) {
            	mpegAvcAu.write(auAddr);
            }
        	mpegRingbufferWrite();

        	if (decodedImageInfo.frameEnd < 0) {
        		// Return an error only past the last video timestamp
        		if (psmfHeader == null || currentVideoTimestamp > psmfHeader.mpegLastTimestamp || !isRegisteredAudioChannel()) {
	    			result = SceKernelErrors.ERROR_MPEG_NO_DATA;
        		}
        	}
        }

        if (result == 0) {
	        if (mpegAvcAu.pts != UNKNOWN_TIMESTAMP) {
	        	currentVideoTimestamp = mpegAvcAu.pts;
	        } else {
	        	currentVideoTimestamp += videoTimestampStep;
	        }
        }

        if (log.isDebugEnabled()) {
    		log.debug(String.format("hleMpegGetAvcAu returning 0x%08X, AvcAu=%s", result, mpegAvcAu));
    	}

    	if (result != 0) {
        	delayThread(mpegDecodeErrorDelay);
        }

    	startedMpeg = true;

    	return result;
    }

    public int hleMpegGetAtracAu(TPointer auAddr) {
        int result = 0;
        if (isRegisteredAudioChannel()) {
        	mpegAtracAu.esSize = audioFrameLength == 0 ? 0 : audioFrameLength + 8;

        	if (audioFrameLength == 0 || audioBuffer == null || audioBuffer.getLength() < audioFrameLength) {
        		boolean needUpdateAu;
        		if (audioPesHeader == null) {
        			audioPesHeader = new PesHeader(getRegisteredAudioChannel());
        			needUpdateAu = true;
        		} else {
        			// Take the PTS from the previous PES header.
	        		mpegAtracAu.pts = audioPesHeader.getPts();
	        		// On PSP, the audio DTS is always set to -1
	        		mpegAtracAu.dts = UNKNOWN_TIMESTAMP;
	        		if (auAddr != null && auAddr.isNotNull()) {
	        			mpegAtracAu.write(auAddr);
	        		}
        			needUpdateAu = false;
        		}

        		audioPesHeader.setDtsPts(UNKNOWN_TIMESTAMP);
	        	readNextAudioFrame(audioPesHeader);

	        	if (needUpdateAu) {
	    			// Take the PTS from the first PES header and reset it.
	        		mpegAtracAu.pts = audioPesHeader.getPts();
	        		audioPesHeader.setDtsPts(UNKNOWN_TIMESTAMP);
	        		// On PSP, the audio DTS is always set to -1
	        		mpegAtracAu.dts = UNKNOWN_TIMESTAMP;
	        		if (auAddr != null && auAddr.isNotNull()) {
	        			mpegAtracAu.write(auAddr);
	        		}
	        	}

	        	if (audioBuffer.getLength() < audioFrameLength) {
	        		result = SceKernelErrors.ERROR_MPEG_NO_DATA;
	        	} else {
	        		// Update the ringbuffer only in case of no error
	        		mpegRingbufferWrite();
	        	}
        	} else {
    			// Take the PTS from the previous PES header and reset it.
        		mpegAtracAu.pts = audioPesHeader.getPts();
        		audioPesHeader.setDtsPts(UNKNOWN_TIMESTAMP);
        		// On PSP, the audio DTS is always set to -1
        		mpegAtracAu.dts = UNKNOWN_TIMESTAMP;
        		if (auAddr != null && auAddr.isNotNull()) {
        			mpegAtracAu.write(auAddr);
        		}
        		mpegRingbufferWrite();
        	}
        }

        if (result == 0) {
	        if (mpegAtracAu.pts != UNKNOWN_TIMESTAMP) {
	        	currentAudioTimestamp = mpegAtracAu.pts;
	        } else {
	        	currentAudioTimestamp += audioTimestampStep;
	        }
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("hleMpegGetAtracAu returning result=0x%08X, pts=%d, dts=%d", result, mpegAtracAu.pts, mpegAtracAu.dts));
        }

        return result;
    }

    public int hleMpegAtracDecode(TPointer auAddr, TPointer bufferAddr, int bufferSize) {
    	int result = 0;
    	int bytes = 0;

    	if (audioBuffer != null && audioFrameLength > 0 && audioBuffer.getLength() >= audioFrameLength) {
    		int channels = psmfHeader == null ? 2 : psmfHeader.getAudioChannelConfig();
        	if (audioCodec == null) {
        		audioCodec = CodecFactory.getCodec(PSP_CODEC_AT3PLUS);
        		result = audioCodec.init(audioFrameLength, channels, mpegAudioOutputChannels, 0);
        	}
        	result = audioCodec.decode(audioBuffer.getReadAddr(), audioFrameLength, bufferAddr.getAddress());
        	if (result < 0) {
        		log.error(String.format("Error received from codec.decode: 0x%08X", result));
        	} else {
        		if (log.isTraceEnabled()) {
        			log.trace(String.format("sceMpegAtracDecode codec returned 0x%X. Decoding from %s", result, Utilities.getMemoryDump(audioBuffer.getReadAddr(), audioFrameLength)));
        		}
        		bytes = audioCodec.getNumberOfSamples() * 2 * channels;
        	}
        	if (audioBuffer.notifyRead(Memory.getInstance(), audioFrameLength) != audioFrameLength) {
        		log.error(String.format("Internal error while consuming from the audio buffer"));
        	}

        	startedMpeg = true;
            audioFrameCount++;

        	delayThread(atracDecodeDelay);

        	result = 0;
        }

    	// Fill the rest of the buffer with 0's
    	bufferAddr.clear(bytes, bufferSize - bytes);

    	if (auAddr != null && auAddr.isNotNull()) {
    		mpegAtracAu.write(auAddr);
    	}

    	return 0;
    }

    public int hleMpegAvcDecode(int buffer, int frameWidth, int pixelMode, TPointer32 gotFrameAddr, boolean writeAbgr) {
		int threadUid = Modules.ThreadManForUserModule.getCurrentThreadID();
		Modules.ThreadManForUserModule.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_VIDEO_DECODER);
		videoDecoderThread.trigger(threadUid, buffer, frameWidth, pixelMode, gotFrameAddr, writeAbgr, Emulator.getClock().microTime() + avcDecodeDelay);

		return 0;
    }

    public static Date convertTimestampToDate(long timestamp) {
        long millis = timestamp / (mpegTimestampPerSecond / 1000);
        return new Date(millis);
    }

    protected StreamInfo getStreamInfo(int uid) {
    	return streamMap.get(uid);
    }

    protected int getMpegHandle(int mpegAddr) {
        if (Memory.isAddressGood(mpegAddr)) {
            return Processor.memory.read32(mpegAddr);
        }

        return -1;
    }

    public int checkMpegHandle(int mpeg) {
    	if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn(String.format("checkMpegHandler bad mpeg handle 0x%08X", mpeg));
    		throw new SceKernelErrorException(-1);
    	}
    	return mpeg;
    }

    protected void writeTimestamp(Memory mem, int address, long ts) {
        mem.write32(address, (int) ((ts >> 32) & 0x1));
        mem.write32(address + 4, (int) ts);
    }

    public static int getMpegVersion(int mpegRawVersion) {
        switch (mpegRawVersion) {
	        case PSMF_VERSION_0012: return MPEG_VERSION_0012;
	        case PSMF_VERSION_0013: return MPEG_VERSION_0013;
	        case PSMF_VERSION_0014: return MPEG_VERSION_0014;
	        case PSMF_VERSION_0015: return MPEG_VERSION_0015;
        }

        return -1;
    }

    public static int read8(Memory mem, int bufferAddr, byte[] buffer, int offset) {
    	if (buffer != null) {
    		return Utilities.read8(buffer, offset);
    	}
    	return mem.read8(bufferAddr + offset);
    }

    public static int readUnaligned32(Memory mem, int bufferAddr, byte[] buffer, int offset) {
    	if (buffer != null) {
    		return Utilities.readUnaligned32(buffer, offset);
    	}
    	return Utilities.readUnaligned32(mem, bufferAddr + offset);
    }

    public static int read32(Memory mem, int bufferAddr, byte[] buffer, int offset) {
    	if (buffer != null) {
    		return Utilities.readUnaligned32(buffer, offset);
    	}
    	return mem.read32(bufferAddr + offset);
    }

    public static int read16(Memory mem, int bufferAddr, byte[] buffer, int offset) {
    	if (buffer != null) {
    		return Utilities.readUnaligned16(buffer, offset);
    	}
    	return mem.read16(bufferAddr + offset);
    }

    protected void analyseMpeg(int bufferAddr, byte[] mpegHeader) {
		psmfHeader = new PSMFHeader(bufferAddr, mpegHeader);

        avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;
        avcGotFrame = 0;
        if (mpegRingbuffer != null) {
            mpegRingbuffer.reset();
        	mpegRingbufferWrite();
        }
        mpegAtracAu.dts = UNKNOWN_TIMESTAMP;
        mpegAtracAu.pts = 0;
        mpegAvcAu.dts = 0;
        mpegAvcAu.pts = 0;
        videoFrameCount = 0;
        audioFrameCount = 0;
        currentVideoTimestamp = 0;
        currentAudioTimestamp = 0;
    }

    protected void analyseMpeg(int bufferAddr) {
        analyseMpeg(bufferAddr, null);

        if (log.isDebugEnabled()) {
	    	log.debug(String.format("Stream offset: 0x%X, Stream size: 0x%X", psmfHeader.mpegOffset, psmfHeader.mpegStreamSize));
	    	log.debug(String.format("First timestamp: %d, Last timestamp: %d", psmfHeader.mpegFirstTimestamp, psmfHeader.mpegLastTimestamp));
	        if (log.isTraceEnabled()) {
	        	log.trace(Utilities.getMemoryDump(bufferAddr, MPEG_HEADER_BUFFER_MINIMUM_SIZE));
	        }
        }
    }

    protected boolean hasPsmfStream(int streamType) {
    	if (psmfHeader == null || psmfHeader.psmfStreams == null) {
    		// Header not analyzed, assume that the PSMF has the given stream
    		return true;
    	}

    	for (PSMFStream stream : psmfHeader.psmfStreams) {
    		if (stream.isStreamOfType(streamType)) {
    			return true;
    		}
    	}

    	return false;
    }

    protected boolean hasPsmfVideoStream() {
    	return hasPsmfStream(PSMF_AVC_STREAM);
    }

    protected boolean hasPsmfAudioStream() {
    	return hasPsmfStream(PSMF_AUDIO_STREAM);
    }

    protected boolean hasPsmfUserdataStream() {
    	return hasPsmfStream(PSMF_DATA_STREAM);
    }

    public static void generateFakeImage(int dest_addr, int frameWidth, int imageWidth, int imageHeight, int pixelMode) {
        Memory mem = Memory.getInstance();

        Random random = new Random();
        final int pixelSize = 3;
        final int bytesPerPixel = sceDisplay.getPixelFormatBytes(pixelMode);
        for (int y = 0; y < imageHeight - pixelSize + 1; y += pixelSize) {
            int address = dest_addr + y * frameWidth * bytesPerPixel;
            final int width = Math.min(imageWidth, frameWidth);
            for (int x = 0; x < width; x += pixelSize) {
                int n = random.nextInt(256);
                int color = 0xFF000000 | (n << 16) | (n << 8) | n;
                int pixelColor = Debug.getPixelColor(color, pixelMode);
                if (bytesPerPixel == 4) {
                    for (int i = 0; i < pixelSize; i++) {
                        for (int j = 0; j < pixelSize; j++) {
                            mem.write32(address + (i * frameWidth + j) * 4, pixelColor);
                        }
                    }
                } else if (bytesPerPixel == 2) {
                    for (int i = 0; i < pixelSize; i++) {
                        for (int j = 0; j < pixelSize; j++) {
                            mem.write16(address + (i * frameWidth + j) * 2, (short) pixelColor);
                        }
                    }
                }
                address += pixelSize * bytesPerPixel;
            }
        }
    }

    public static void delayThread(long startMicros, int delayMicros) {
    	long now = Emulator.getClock().microTime();
    	int threadDelayMicros = delayMicros - (int) (now - startMicros);
    	delayThread(threadDelayMicros);
    }

    public static void delayThread(int delayMicros) {
    	if (delayMicros > 0) {
    		Modules.ThreadManForUserModule.hleKernelDelayThread(delayMicros, false);
    	} else {
    		Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
    	}
    }

    protected void finishStreams() {
    	if (log.isDebugEnabled()) {
    		log.debug("finishStreams");
    	}

    	// Release all the streams (can't loop on streamMap as release() modifies it)
        List<StreamInfo> streams = new LinkedList<sceMpeg.StreamInfo>();
        streams.addAll(streamMap.values());
        for (StreamInfo stream : streams) {
        	stream.release();
        }
    }

    protected void finishMpeg() {
    	if (log.isDebugEnabled()) {
    		log.debug("finishMpeg");
    	}

        if (mpegRingbuffer != null) {
        	mpegRingbuffer.reset();
        	mpegRingbufferWrite();
        }
        VideoEngine.getInstance().resetVideoTextures();

        registeredVideoChannel = -1;
        registeredAudioChannel = -1;
        mpegAtracAu.dts = UNKNOWN_TIMESTAMP;
        mpegAtracAu.pts = 0;
        mpegAvcAu.dts = 0;
        mpegAvcAu.pts = 0;
        videoFrameCount = 0;
        audioFrameCount = 0;
        currentVideoTimestamp = 0;
        currentAudioTimestamp = 0;
        startedMpeg = false;

        if (audioBuffer != null) {
        	audioBuffer.reset();
        }
        if (videoBuffer != null) {
        	videoBuffer.reset();
        }
        userDataBuffer = null;
        audioFrameLength = 0;
        frameHeaderLength = 0;
        audioPesHeader = null;
        videoPesHeader = null;
        userDataPesHeader = null;
        userDataLength = 0;
        videoFrameHeight = -1;

        if (decodedImages != null) {
        	synchronized (decodedImages) {
        		decodedImages.clear();
        	}
        }
        if (videoDecoderThread != null) {
        	videoDecoderThread.resetWaitingThreadInfo();
        }
        videoCodec = null;
    }

    protected void checkEmptyVideoRingbuffer() {
        if (!mpegRingbuffer.hasReadPackets() || (mpegRingbuffer.isEmpty() && videoBuffer.isEmpty() && decodedImages.isEmpty())) {
            delayThread(mpegDecodeErrorDelay);
            log.debug("ringbuffer and video buffer are empty");
            throw new SceKernelErrorException(SceKernelErrors.ERROR_MPEG_NO_DATA); // No more data in ringbuffer.
        }
    }

    protected void checkEmptyAudioRingbuffer() {
        if (!mpegRingbuffer.hasReadPackets() || (mpegRingbuffer.isEmpty() && audioBuffer.isEmpty())) {
            log.debug("ringbuffer and audio buffer are empty");
            delayThread(mpegDecodeErrorDelay);
            throw new SceKernelErrorException(SceKernelErrors.ERROR_MPEG_NO_DATA); // No more data in ringbuffer.
        }
    }

    protected int getYCbCrSize() {
    	int width  = psmfHeader == null ? Screen.width  : psmfHeader.getVideoWidth();
        int height = psmfHeader == null ? Screen.height : psmfHeader.getVideoHeight();

        return getYCbCrSize(width, height);
    }

    protected int getYCbCrSize(int width, int height) {
        return (width / 2) * (height / 2) * 6; // 12 bits per pixel
    }

    public void setMpegAvcAu(SceMpegAu au) {
    	mpegAvcAu.esBuffer = au.esBuffer;
    	mpegAvcAu.esSize = au.esSize;
    }

    /**
     * sceMpegQueryStreamOffset
     * 
     * @param mpeg
     * @param bufferAddr
     * @param offsetAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x21FF80E4, version = 150, checkInsideInterrupt = true)
    public int sceMpegQueryStreamOffset(@CheckArgument("checkMpegHandle") int mpeg, TPointer bufferAddr, TPointer32 offsetAddr) {
        analyseMpeg(bufferAddr.getAddress());

        // Check magic.
        if (psmfHeader.mpegMagic != PSMF_MAGIC) {
            log.warn("sceMpegQueryStreamOffset bad magic " + String.format("0x%08X", psmfHeader.mpegMagic));
            offsetAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

        // Check version.
        if (psmfHeader.mpegVersion < 0) {
            log.warn("sceMpegQueryStreamOffset bad version " + String.format("0x%08X", psmfHeader.mpegRawVersion));
            offsetAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_BAD_VERSION;
        }

        // Check offset.
        if ((psmfHeader.mpegOffset & 2047) != 0 || psmfHeader.mpegOffset == 0) {
            log.warn("sceMpegQueryStreamOffset bad offset " + String.format("0x%08X", psmfHeader.mpegOffset));
            offsetAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

    	offsetAddr.setValue(psmfHeader.mpegOffset);
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegQueryStreamOffset returning 0x%X", offsetAddr.getValue()));
        }

        return 0;
    }

    /**
     * sceMpegQueryStreamSize
     * 
     * @param bufferAddr
     * @param sizeAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x611E9E11, version = 150, checkInsideInterrupt = true)
    public int sceMpegQueryStreamSize(TPointer bufferAddr, TPointer32 sizeAddr) {
        analyseMpeg(bufferAddr.getAddress());

        // Check magic.
        if (psmfHeader.mpegMagic != PSMF_MAGIC) {
            log.warn(String.format("sceMpegQueryStreamSize bad magic 0x%08X", psmfHeader.mpegMagic));
            return -1;
        }

        // Check alignment.
        if ((psmfHeader.mpegStreamSize & 2047) != 0) {
        	sizeAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

    	sizeAddr.setValue(psmfHeader.mpegStreamSize);
        return 0;
    }

    /**
     * sceMpegInit
     * 
     * @return
     */
    @HLELogging(level="info")
    @HLEFunction(nid = 0x682A619B, version = 150, checkInsideInterrupt = true)
    public int sceMpegInit() {
    	finishMpeg();
    	finishStreams();

    	return 0;
    }

    /**
     * sceMpegFinish
     * 
     * @return
     */
    @HLELogging(level="info")
    @HLEFunction(nid = 0x874624D6, version = 150, checkInsideInterrupt = true)
    public int sceMpegFinish() {
        finishMpeg();
        finishStreams();

        return 0;
    }

    /**
     * sceMpegQueryMemSize
     * 
     * @param mode
     * 
     * @return
     */
    @HLEFunction(nid = 0xC132E22F, version = 150, checkInsideInterrupt = true)
    public int sceMpegQueryMemSize(int mode) {
        // Mode = 0 -> 64k (constant).
        return MPEG_MEMSIZE;
    }

    /**
     * sceMpegCreate
     * 
     * @param mpeg
     * @param data
     * @param size
     * @param ringbufferAddr
     * @param frameWidth
     * @param mode
     * @param ddrtop
     * 
     * @return
     */
    @HLEFunction(nid = 0xD8C5F121, version = 150, checkInsideInterrupt = true)
    public int sceMpegCreate(TPointer mpeg, TPointer data, int size, @CanBeNull TPointer ringbufferAddr, int frameWidth, int mode, int ddrtop) {
    	return hleMpegCreate(mpeg, data, size, ringbufferAddr, frameWidth, mode, ddrtop);
    }

    /**
     * sceMpegDelete
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0x606A4649, version = 150, checkInsideInterrupt = true)
    public int sceMpegDelete(@CheckArgument("checkMpegHandle") int mpeg) {
        if (videoDecoderThread != null) {
        	videoDecoderThread.exit();
        	videoDecoderThread = null;
        }

        finishMpeg();
        finishStreams();

        return 0;
    }

    /**
     * sceMpegRegistStream
     * 
     * @param mpeg
     * @param streamType
     * @param streamNum
     * 
     * @return stream Uid
     */
    @HLEFunction(nid = 0x42560F23, version = 150, checkInsideInterrupt = true)
    public int sceMpegRegistStream(@CheckArgument("checkMpegHandle") int mpeg, int streamType, int streamChannelNum) {
    	StreamInfo info = new StreamInfo(streamType, streamChannelNum);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegRegistStream returning 0x%X", info.getUid()));
    	}

        return info.getUid();
    }

    /**
     * sceMpegUnRegistStream
     * 
     * @param mpeg
     * @param streamUid
     * 
     * @return
     */
    @HLEFunction(nid = 0x591A4AA2, version = 150, checkInsideInterrupt = true)
    public int sceMpegUnRegistStream(@CheckArgument("checkMpegHandle") int mpeg, int streamUid) {
    	StreamInfo info = getStreamInfo(streamUid);
    	if (info == null) {
            log.warn(String.format("sceMpegUnRegistStream unknown stream=0x%X", streamUid));
            return SceKernelErrors.ERROR_MPEG_UNKNOWN_STREAM_ID;
    	}

        info.release();

        return 0;
    }

    /**
     * sceMpegMallocAvcEsBuf
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0xA780CF7E, version = 150, checkInsideInterrupt = true)
    public int sceMpegMallocAvcEsBuf(@CheckArgument("checkMpegHandle") int mpeg) {
        // sceMpegMallocAvcEsBuf does not allocate any memory.
    	// It returns 0x00000001 for the first call,
    	// 0x00000002 for the second call
    	// and 0x00000000 for subsequent calls.
    	int esBufferId = 0;
    	for (int i = 0; i < allocatedEsBuffers.length; i++) {
    		if (!allocatedEsBuffers[i]) {
    			esBufferId = i + 1;
    			allocatedEsBuffers[i] = true;
    			break;
    		}
    	}

    	return esBufferId;
    }

    /**
     * sceMpegFreeAvcEsBuf
     * 
     * @param mpeg
     * @param esBuf
     * 
     * @return
     */
    @HLEFunction(nid = 0xCEB870B1, version = 150, checkInsideInterrupt = true)
    public int sceMpegFreeAvcEsBuf(@CheckArgument("checkMpegHandle") int mpeg, int esBuf) {
        if (esBuf == 0) {
            log.warn("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ", esBuf=0x" + Integer.toHexString(esBuf) + ") bad esBuf handle");
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }
        
    	if (esBuf >= 1 && esBuf <= allocatedEsBuffers.length) {
    		allocatedEsBuffers[esBuf - 1] = false;
    	}
    	return 0;
    }

    /**
     * sceMpegQueryAtracEsSize
     * 
     * @param mpeg
     * @param esSize_addr
     * @param outSize_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xF8DCB679, version = 150, checkInsideInterrupt = true)
    public int sceMpegQueryAtracEsSize(@CheckArgument("checkMpegHandle") int mpeg, @CanBeNull TPointer32 esSizeAddr, @CanBeNull TPointer32 outSizeAddr) {
        esSizeAddr.setValue(MPEG_ATRAC_ES_SIZE);
        outSizeAddr.setValue(MPEG_ATRAC_ES_OUTPUT_SIZE);

        return 0;
    }

    /**
     * sceMpegQueryPcmEsSize
     * 
     * @param mpeg
     * @param esSize_addr
     * @param outSize_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xC02CF6B5, version = 150, checkInsideInterrupt = true)
    public int sceMpegQueryPcmEsSize(@CheckArgument("checkMpegHandle") int mpeg, TPointer32 esSizeAddr, TPointer32 outSizeAddr) {
        esSizeAddr.setValue(MPEG_PCM_ES_SIZE);
        outSizeAddr.setValue(MPEG_PCM_ES_OUTPUT_SIZE);

        return 0;
    }

    /**
     * sceMpegInitAu
     * 
     * @param mpeg
     * @param buffer_addr
     * @param auAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x167AFD9E, version = 150, checkInsideInterrupt = true)
    public int sceMpegInitAu(@CheckArgument("checkMpegHandle") int mpeg, int buffer_addr, TPointer auAddr) {
        // Check if sceMpegInitAu is being called for AVC or ATRAC
        // and write the proper AU (access unit) struct.
        if (buffer_addr >= 1 && buffer_addr <= allocatedEsBuffers.length && allocatedEsBuffers[buffer_addr - 1]) {
        	mpegAvcAu.esBuffer = buffer_addr;
        	mpegAvcAu.esSize = 0;
        	mpegAvcAu.write(auAddr);
        } else {
        	mpegAtracAu.esBuffer = buffer_addr;
        	mpegAtracAu.esSize = 0;
        	mpegAtracAu.write(auAddr);
        }
        return 0;
    }

    /**
     * sceMpegChangeGetAvcAuMode
     * 
     * @param mpeg
     * @param stream_addr
     * @param mode
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x234586AE, version = 150, checkInsideInterrupt = true)
    public int sceMpegChangeGetAvcAuMode(int mpeg, int stream_addr, int mode) {
        return 0;
    }

    /**
     * sceMpegChangeGetAuMode
     * 
     * @param mpeg
     * @param streamUid
     * @param mode
     * 
     * @return
     */
    @HLEFunction(nid = 0x9DCFB7EA, version = 150, checkInsideInterrupt = true)
    public int sceMpegChangeGetAuMode(int mpeg, int streamUid, int mode) {
        StreamInfo info = getStreamInfo(streamUid);
        if (info == null) {
            log.warn(String.format("sceMpegChangeGetAuMode unknown stream=0x%X", streamUid));
            return -1;
        }

        // When changing a stream from SKIP to DECODE mode,
        // change all the other streams of the same type to SKIP mode.
        // There is only on stream of a given type in DECODE mode.
        if (info.getAuMode() == MPEG_AU_MODE_SKIP && mode == MPEG_AU_MODE_DECODE) {
	        for (StreamInfo stream : streamMap.values()) {
	    		if (stream != null && stream != info && stream.isStreamType(info.getType()) && stream.getAuMode() == MPEG_AU_MODE_DECODE) {
	    			stream.setAuMode(MPEG_AU_MODE_SKIP);
	    		}
	        }
        }

        info.setAuMode(mode);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegChangeGetAuMode mode=%s: %s", mode == MPEG_AU_MODE_DECODE ? "DECODE" : "SKIP", info));
        }

        return 0;
    }

    /**
     * sceMpegGetAvcAu
     * 
     * @param mpeg
     * @param streamUid
     * @param au_addr
     * @param attr_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xFE246728, version = 150, checkInsideInterrupt = true)
    public int sceMpegGetAvcAu(@CheckArgument("checkMpegHandle") int mpeg, int streamUid, TPointer auAddr, @CanBeNull TPointer32 attrAddr) {
        mpegRingbufferRead();

        checkEmptyVideoRingbuffer();

        // @NOTE: Shouldn't this be negated?
        if (Memory.isAddressGood(streamUid)) {
            log.warn("sceMpegGetAvcAu didn't get a fake stream");
            return SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        }

        if (!streamMap.containsKey(streamUid)) {
            log.warn(String.format("sceMpegGetAvcAu bad stream 0x%X", streamUid));
            return -1;
        }

        int result = 0;
        // Update the video timestamp (AVC).
        if (isRegisteredVideoChannel()) {
        	result = hleMpegGetAvcAu(auAddr);
        }

        attrAddr.setValue(1); // Unknown.

        if (log.isDebugEnabled()) {
        	log.debug(String.format("videoFrameCount=%d(pts=%d), audioFrameCount=%d(pts=%d), pts difference %d, vcount=%d", videoFrameCount, currentVideoTimestamp, audioFrameCount, currentAudioTimestamp, currentAudioTimestamp - currentVideoTimestamp, Modules.sceDisplayModule.getVcount()));
        }

        return result;
    }

    /**
     * sceMpegGetPcmAu
     * 
     * @param mpeg
     * @param streamUid
     * @param au_addr
     * @param attr_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x8C1E027D, version = 150, checkInsideInterrupt = true)
    public int sceMpegGetPcmAu(@CheckArgument("checkMpegHandle") int mpeg, int streamUid, TPointer auAddr, @CanBeNull TPointer32 attrAddr) {
        mpegRingbufferRead();

        if (!mpegRingbuffer.hasReadPackets() || mpegRingbuffer.isEmpty()) {
            delayThread(mpegDecodeErrorDelay);
            return SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
        }
        
        // Should be negated?
        if (Memory.isAddressGood(streamUid)) {
            log.warn("sceMpegGetPcmAu didn't get a fake stream");
            return SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        } 
        
        if (!streamMap.containsKey(streamUid)) {
            log.warn(String.format("sceMpegGetPcmAu bad streamUid 0x%08X", streamUid));
            return -1;
        }
        int result = 0;
        // Update the audio timestamp (Atrac).
        if (getRegisteredPcmChannel() >= 0) {
        	// Read Au of next Atrac frame
        	mpegAtracAu.write(auAddr);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceMpegGetPcmAu returning AtracAu=%s", mpegAtracAu.toString()));
        	}
        }
        // Bitfield used to store data attributes.
        // Uses same bitfield as the one in the PSMF header.
    	int attr = 1 << 7; // Sampling rate (1 = 44.1kHz).
    	attr |= 2;         // Number of channels (1 - MONO / 2 - STEREO).
        attrAddr.setValue(attr);

        if (result != 0) {
        	delayThread(mpegDecodeErrorDelay);
        }
        return result;
    }

    /**
     * sceMpegGetAtracAu
     * 
     * @param mpeg
     * @param streamUid
     * @param auAddr
     * @param attrAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0xE1CE83A7, version = 150, checkInsideInterrupt = true)
    public int sceMpegGetAtracAu(@CheckArgument("checkMpegHandle") int mpeg, int streamUid, TPointer auAddr, @CanBeNull TPointer32 attrAddr) {
        mpegRingbufferRead();

        checkEmptyAudioRingbuffer();

        if (Memory.isAddressGood(streamUid)) {
            log.warn("sceMpegGetAtracAu didn't get a fake stream");
            return SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        }

        if (!streamMap.containsKey(streamUid)) {
            log.warn("sceMpegGetAtracAu bad address " + String.format("0x%08X 0x%08X", streamUid, auAddr));
            return -1;
        }

        // Update the audio timestamp (Atrac).
        int result = hleMpegGetAtracAu(auAddr);

        // Bitfield used to store data attributes.
        attrAddr.setValue(0);     // Pointer to ATRAC3plus stream (from PSMF file).

        if (log.isDebugEnabled()) {
        	log.debug(String.format("videoFrameCount=%d(pts=%d), audioFrameCount=%d(pts=%d), pts difference %d, vcount=%d", videoFrameCount, currentVideoTimestamp, audioFrameCount, currentAudioTimestamp, currentAudioTimestamp - currentVideoTimestamp, Modules.sceDisplayModule.getVcount()));
        }

        return result;
    }

    /**
     * sceMpegFlushStream
     * 
     * @param mpeg
     * @param stream_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x500F0429, version = 150, checkInsideInterrupt = true)
    public int sceMpegFlushStream(int mpeg, int stream_addr) {
    	// This call is not deleting the registered streams.
        finishMpeg();
        return 0;
    }

    /**
     * sceMpegFlushAllStream
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0x707B7629, version = 150, checkInsideInterrupt = true)
    public int sceMpegFlushAllStream(int mpeg) {
        // Finish the Mpeg only if we are not at the start of a new video,
        // otherwise the analyzed video could be lost.
    	// This call is not deleting the registered streams.
        if (startedMpeg) {
        	finishMpeg();
        }

        return 0;
    }

    /**
     * sceMpegAvcDecode
     * 
     * @param mpeg
     * @param au_addr
     * @param frameWidth
     * @param buffer_addr
     * @param init_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x0E3C2E9D, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecode(@CheckArgument("checkMpegHandle") int mpeg, TPointer32 auAddr, int frameWidth, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
        int au = auAddr.getValue();
        int buffer = bufferAddr.getValue();

        if (avcEsBuf != null && au == -1 && mpegRingbuffer == null) {
        	final int width = frameWidth;
        	final int height = width < 480 ? 160 : 272; // How to retrieve the real video height?

        	// The application seems to stream the MPEG data into the avcEsBuf.addr buffer,
        	// probably only one frame at a time.
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceMpegAvcDecode buffer=0x%08X, avcEsBuf: %s", buffer, Utilities.getMemoryDump(avcEsBuf.addr, AVC_ES_BUF_SIZE)));
        	}

        	// Generate a faked image. We cannot use the MediaEngine at this point
        	// as we have not enough MPEG data buffered in advance.
			generateFakeImage(buffer, frameWidth, width, height, videoPixelMode);

			// Clear the avcEsBuf buffer to better recognize the new MPEG data sent next time
			Processor.memory.memset(avcEsBuf.addr, (byte) 0, AVC_ES_BUF_SIZE);

    		return 0;
		}

		// When frameWidth is 0, take the frameWidth specified at sceMpegCreate.
        if (frameWidth == 0) {
            if (defaultFrameWidth == 0) {
                frameWidth = psmfHeader.getVideoWidth();
            } else {
                frameWidth = defaultFrameWidth;
            }
        }
        mpegRingbufferRead();

        if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcDecode ringbuffer not created");
            return -1;
        }

        checkEmptyVideoRingbuffer();

    	if (auAddr != null && auAddr.isNotNull()) {
    		mpegAvcAu.read(auAddr);
    	}

        hleMpegAvcDecode(buffer, frameWidth, videoPixelMode, gotFrameAddr, true);

    	startedMpeg = true;

        // Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
    	final int height = psmfHeader != null ? psmfHeader.getVideoHeight() : 272;
        VideoEngine.getInstance().addVideoTexture(buffer, buffer + height * frameWidth * sceDisplay.getPixelFormatBytes(videoPixelMode));

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegAvcDecode buffer=0x%08X, dts=0x%X, pts=0x%X, gotFrame=%d", buffer, mpegAvcAu.dts, mpegAvcAu.pts, avcGotFrame));
        }

        // Correct decoding.
        avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;

    	if (auAddr != null && auAddr.isNotNull()) {
    		mpegAvcAu.write(auAddr);
    	}

        return 0;
    }

    /**
     * sceMpegAvcDecodeDetail
     * 
     * @param mpeg
     * @param detailPointer
     * 
     * @return
     */
    @HLEFunction(nid = 0x0F6C18D7, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecodeDetail(@CheckArgument("checkMpegHandle") int mpeg, TPointer detailPointer) {
        detailPointer.setValue32( 0, avcDecodeResult); // Stores the result
        detailPointer.setValue32( 4, videoFrameCount); // Last decoded frame
        detailPointer.setValue32( 8, psmfHeader != null ? psmfHeader.getVideoWidth()  : 0); // Frame width
        detailPointer.setValue32(12, psmfHeader != null ? psmfHeader.getVideoHeight() : (videoFrameHeight < 0 ? 0 : videoFrameHeight)); // Frame height
        detailPointer.setValue32(16, 0              ); // Frame crop rect (left)
        detailPointer.setValue32(20, 0              ); // Frame crop rect (right)
        detailPointer.setValue32(24, 0              ); // Frame crop rect (top)
        detailPointer.setValue32(28, 0              ); // Frame crop rect (bottom)
        detailPointer.setValue32(32, avcGotFrame    ); // Status of the last decoded frame

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegAvcDecodeDetail returning decodeResult=0x%X, frameCount=%d, width=%d, height=%d, gotFrame=0x%X", detailPointer.getValue32(0), detailPointer.getValue32(4), detailPointer.getValue32(8), detailPointer.getValue32(12), detailPointer.getValue32(32)));
        }
        return 0;
    }

    /**
     * sceMpegAvcDecodeMode
     * 
     * @param mpeg
     * @param mode_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xA11C7026, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecodeMode(@CheckArgument("checkMpegHandle") int mpeg, TPointer32 modeAddr) {
        // -1 is a default value.
        int mode = modeAddr.getValue(0);
        int pixelMode = modeAddr.getValue(4);
        if (pixelMode >= TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650 && pixelMode <= TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceMpegAvcDecodeMode mode=0x%X, pixelMode=0x%X", mode, pixelMode));
        	}
            videoPixelMode = pixelMode;
        } else {
            log.warn(String.format("sceMpegAvcDecodeMode mode=0x%X, pixel mode=0x%X: unknown pixel mode", mode, pixelMode));
        }
        return 0;
    }

    /**
     * sceMpegAvcDecodeStop
     * 
     * @param mpeg
     * @param frameWidth
     * @param buffer_addr
     * @param status_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x740FCCD1, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecodeStop(@CheckArgument("checkMpegHandle") int mpeg, int frameWidth, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
    	// Decode any pending image
    	decodeImage(bufferAddr.getValue(), frameWidth, videoPixelMode, gotFrameAddr, true);

        return 0;
    }

    /**
     * sceMpegAvcDecodeFlush
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0x4571CC64, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecodeFlush(int mpeg) {
    	// Finish the Mpeg if it had no audio.
        // Finish the Mpeg only if we are not at the start of a new video,
        // otherwise the analyzed video could be lost.
        if (startedMpeg && audioFrameCount <= 0) {
        	finishMpeg();
        }

        return 0;
    }

    /**
     * sceMpegAvcQueryYCbCrSize
     * 
     * @param mpeg
     * @param mode         - 1 -> Loaded from file. 2 -> Loaded from memory.
     * @param width        - 480.
     * @param height       - 272.
     * @param resultAddr   - Where to store the result.
     * 
     * @return
     */
    @HLEFunction(nid = 0x211A057C, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcQueryYCbCrSize(@CheckArgument("checkMpegHandle") int mpeg, int mode, int width, int height, TPointer32 resultAddr) {
        if ((width & 15) != 0 || (height & 15) != 0 || width > 480 || height > 272) {
            log.warn("sceMpegAvcQueryYCbCrSize invalid size width=" + width + ", height=" + height);
        	return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

    	// Write the size of the buffer used by sceMpegAvcDecodeYCbCr
		int size = YCBCR_DATA_OFFSET + getYCbCrSize(width, height);
        resultAddr.setValue(size);

        return 0;
    }

    /**
     * sceMpegAvcInitYCbCr
     * 
     * @param mpeg
     * @param mode
     * @param width
     * @param height
     * @param ycbcr_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x67179B1B, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcInitYCbCr(@CheckArgument("checkMpegHandle") int mpeg, int mode, int width, int height, TPointer yCbCrBuffer) {
    	yCbCrBuffer.memset((byte) 0, YCBCR_DATA_OFFSET);

    	return 0;
    }

    /**
     * sceMpegAvcDecodeYCbCr
     * 
     * @param mpeg
     * @param au_addr
     * @param buffer_addr
     * @param init_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xF0EB1125, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecodeYCbCr(@CheckArgument("checkMpegHandle") int mpeg, TPointer auAddr, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
        mpegRingbufferRead();

        if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcDecodeYCbCr ringbuffer not created");
            return -1;
        }

        checkEmptyVideoRingbuffer();

        // sceMpegAvcDecodeYCbCr() is performing the video decoding and
        // sceMpegAvcCsc() is transforming the YCbCr image into ABGR.
        hleMpegAvcDecode(bufferAddr.getValue() + YCBCR_DATA_OFFSET, 0, videoPixelMode, gotFrameAddr, false);

    	startedMpeg = true;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegAvcDecodeYCbCr *buffer=0x%08X, currentTimestamp=%d, avcGotFrame=%d", bufferAddr.getValue(), mpegAvcAu.pts, avcGotFrame));
        }

        // Correct decoding.
        avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;

    	if (auAddr != null && auAddr.isNotNull()) {
        	mpegAvcAu.esSize = 0;
    		mpegAvcAu.write(auAddr);
    	}

        return 0;
    }

    /**
     * sceMpegAvcDecodeStopYCbCr
     * 
     * @param mpeg
     * @param buffer_addr
     * @param status_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xF2930C9C, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecodeStopYCbCr(@CheckArgument("checkMpegHandle") int mpeg, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
    	// Decode any pending image
    	decodeImage(bufferAddr.getValue(), 0, videoPixelMode, gotFrameAddr, false);

        return 0;
    }

    /**
     * sceMpegAvcCsc
     * 
     * @param mpeg          -
     * @param source_addr   - YCbCr data.
     * @param range_addr    - YCbCr range.
     * @param frameWidth    -
     * @param dest_addr     - Converted data (RGB).
     * 
     * @return
     */
    @HLEFunction(nid = 0x31BD0272, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcCsc(@CheckArgument("checkMpegHandle") int mpeg, TPointer sourceAddr, TPointer32 rangeAddr, int frameWidth, TPointer destAddr) {
        // When frameWidth is 0, take the frameWidth specified at sceMpegCreate.
        if (frameWidth == 0) {
            if (defaultFrameWidth == 0) {
                frameWidth = psmfHeader.getVideoWidth();
            } else {
                frameWidth = defaultFrameWidth;
            }
        }

        int rangeX = rangeAddr.getValue(0);
        int rangeY = rangeAddr.getValue(4);
        int rangeWidth = rangeAddr.getValue(8);
        int rangeHeight = rangeAddr.getValue(12);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegAvcCsc range x=%d, y=%d, width=%d, height=%d", rangeX, rangeY, rangeWidth, rangeHeight));
        }

        // sceMpegAvcDecodeYCbCr() is performing the video decoding and
        // sceMpegAvcCsc() is transforming the YCbCr image into ABGR.
        int width  = psmfHeader == null ? Screen.width  : psmfHeader.getVideoWidth();
        int height = psmfHeader == null ? Screen.height : psmfHeader.getVideoHeight();
        int width2 = width >> 1;
    	int height2 = height >> 1;
        int length = width * height;
        int length2 = width2 * height2;

        // Read the YCbCr image
        int[] luma = getIntBuffer(length);
        int[] cb = getIntBuffer(length2);
        int[] cr = getIntBuffer(length2);
        int dataAddr = sourceAddr.getAddress() + YCBCR_DATA_OFFSET;
        if (memoryInt != null) {
        	// Optimize the most common case
        	int length4 = length >> 2;
            int offset = dataAddr >> 2;
            for (int i = 0, j = 0; i < length4; i++) {
            	int value = memoryInt[offset++];
            	luma[j++] = (value      ) & 0xFF;
            	luma[j++] = (value >>  8) & 0xFF;
            	luma[j++] = (value >> 16) & 0xFF;
            	luma[j++] = (value >> 24) & 0xFF;
            }
            int length16 = length2 >> 2;
            for (int i = 0, j = 0; i < length16; i++) {
            	int value = memoryInt[offset++];
            	cb[j++] = (value      ) & 0xFF;
            	cb[j++] = (value >>  8) & 0xFF;
            	cb[j++] = (value >> 16) & 0xFF;
            	cb[j++] = (value >> 24) & 0xFF;
            }
            for (int i = 0, j = 0; i < length16; i++) {
            	int value = memoryInt[offset++];
            	cr[j++] = (value      ) & 0xFF;
            	cr[j++] = (value >>  8) & 0xFF;
            	cr[j++] = (value >> 16) & 0xFF;
            	cr[j++] = (value >> 24) & 0xFF;
            }
        } else {
	        IMemoryReader memoryReader = MemoryReader.getMemoryReader(dataAddr, length + length2 + length2, 1);
	        for (int i = 0; i < length; i++) {
	        	luma[i] = memoryReader.readNext();
	        }
	        for (int i = 0; i < length2; i++) {
	        	cb[i] = memoryReader.readNext();
	        }
	        for (int i = 0; i < length2; i++) {
	        	cr[i] = memoryReader.readNext();
	        }
        }

        // Convert YCbCr to ABGR
        int[] abgr = getIntBuffer(length);
        H264Utils.YUV2ABGR(width, height, luma, cb, cr, abgr);

        // Write the ABGR image
		final int bytesPerPixel = sceDisplay.getPixelFormatBytes(videoPixelMode);
		if (videoPixelMode == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 && memoryInt != null) {
			// Optimize the most common case
			int pixelIndex = (rangeY * frameWidth + rangeX) * bytesPerPixel;
	        for (int i = 0; i < rangeHeight; i++) {
	        	int addr = destAddr.getAddress() + (i * frameWidth) * bytesPerPixel;
	        	System.arraycopy(abgr, pixelIndex, memoryInt, addr >> 2, rangeWidth);
	        	pixelIndex += width;
	        }
		} else {
        	int addr = destAddr.getAddress();
	        for (int i = 0; i < rangeHeight; i++) {
	        	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, rangeWidth * bytesPerPixel, bytesPerPixel);
	        	int pixelIndex = (i + rangeY) * width + rangeX;
	        	for (int j = 0; j < rangeWidth; j++, pixelIndex++) {
	        		int abgr8888 = abgr[pixelIndex];
	        		int pixelColor = Debug.getPixelColor(abgr8888, videoPixelMode);
	        		memoryWriter.writeNext(pixelColor);
	        	}
	        	memoryWriter.flush();
	        	addr += frameWidth * bytesPerPixel;
	        }
		}

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegAvcCsc writing to 0x%08X-0x%08X, vcount=%d", destAddr.getAddress(), destAddr.getAddress() + (rangeY + rangeHeight) * frameWidth * bytesPerPixel, Modules.sceDisplayModule.getVcount()));
        }

        // Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
        VideoEngine.getInstance().addVideoTexture(destAddr.getAddress(), destAddr.getAddress() + (rangeY + rangeHeight) * frameWidth * bytesPerPixel);

        delayThread(avcDecodeDelay);

        return 0;
    }

    /**
     * sceMpegAtracDecode
     * 
     * @param mpeg
     * @param au_addr
     * @param buffer_addr
     * @param init
     * 
     * @return
     */
    @HLEFunction(nid = 0x800C44DF, version = 150, checkInsideInterrupt = true)
    public int sceMpegAtracDecode(@CheckArgument("checkMpegHandle") int mpeg, TPointer auAddr, TPointer bufferAddr, int init) {
        int result = hleMpegAtracDecode(auAddr, bufferAddr, MPEG_ATRAC_ES_SIZE);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegAtracDecode currentTimestamp=%d", mpegAtracAu.pts));
        }

        return result;
    }

    protected int getPacketsFromSize(int size) {
    	int packets = size / (2048 + 104);

    	return packets;
    }

    private int getSizeFromPackets(int packets) {
        int size = (packets * 104) + (packets * 2048);

        return size;
    }

    /**
     * sceMpegRingbufferQueryMemSize
     * 
     * @param packets
     * 
     * @return
     */
    @HLEFunction(nid = 0xD7A29F46, version = 150, checkInsideInterrupt = true)
    public int sceMpegRingbufferQueryMemSize(int packets) {
        return getSizeFromPackets(packets);
    }

    /**
     * sceMpegRingbufferConstruct
     * 
     * @param ringbuffer_addr
     * @param packets
     * @param data
     * @param size
     * @param callback_addr
     * @param callback_args
     * 
     * @return
     */
    @HLEFunction(nid = 0x37295ED8, version = 150, checkInsideInterrupt = true)
    public int sceMpegRingbufferConstruct(TPointer ringbufferAddr, int packets, @CanBeNull TPointer data, int size, @CanBeNull TPointer callbackAddr, int callbackArgs) {
        if (size < getSizeFromPackets(packets)) {
            log.warn(String.format("sceMpegRingbufferConstruct insufficient space: size=%d, packets=%d", size, packets));
            return SceKernelErrors.ERROR_MPEG_NO_MEMORY;
        }

        SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer(packets, data.getAddress(), size, callbackAddr.getAddress(), callbackArgs);
        ringbuffer.write(ringbufferAddr);

        return 0;
    }

    /**
     * sceMpegRingbufferDestruct
     * 
     * @param ringbuffer_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x13407F13, version = 150, checkInsideInterrupt = true)
    public int sceMpegRingbufferDestruct(TPointer ringbufferAddr) {
    	if (mpegRingbuffer != null) {
	    	mpegRingbuffer.read(ringbufferAddr);
	    	mpegRingbuffer.reset();
	    	mpegRingbuffer.write(ringbufferAddr);
	    	mpegRingbuffer = null;
	    	mpegRingbufferAddr = null;
    	}

    	return 0;
    }

    /**
     * sceMpegRingbufferPut
     * 
     * @param _mpegRingbufferAddr
     * @param numPackets
     * @param available
     * 
     * @return
     */
    @HLEFunction(nid = 0xB240A59E, version = 150, checkInsideInterrupt = true)
    public int sceMpegRingbufferPut(TPointer ringbufferAddr, int numPackets, int available) {
        mpegRingbufferAddr = ringbufferAddr;
        mpegRingbufferRead();

        if (numPackets < 0) {
            return 0;
        }

        int numberPackets = Math.min(available, numPackets);
        if (numberPackets <= 0) {
        	return 0;
        }

        // Note: we can read more packets than available in the Mpeg stream: the application
        // can loop the video by putting previous packets back into the ringbuffer.

        int putNumberPackets = Math.min(numberPackets, mpegRingbuffer.getPutSequentialPackets());
        int putDataAddr = mpegRingbuffer.getPutDataAddr();
        AfterRingbufferPutCallback afterRingbufferPutCallback = new AfterRingbufferPutCallback(putDataAddr, numberPackets - putNumberPackets);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegRingbufferPut executing callback 0x%08X to read 0x%X packets at 0x%08X, Ringbuffer=%s", mpegRingbuffer.getCallbackAddr(), putNumberPackets, putDataAddr, mpegRingbuffer));
        }
        Modules.ThreadManForUserModule.executeCallback(null, mpegRingbuffer.getCallbackAddr(), afterRingbufferPutCallback, false, putDataAddr, putNumberPackets, mpegRingbuffer.getCallbackArgs());

        return afterRingbufferPutCallback.getTotalPacketsAdded();
    }

    /**
     * sceMpegRingbufferAvailableSize
     * 
     * @param _mpegRingbufferAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0xB5F6DC87, version = 150, checkInsideInterrupt = true)
    public int sceMpegRingbufferAvailableSize(TPointer ringbufferAddr) {
        mpegRingbufferAddr = ringbufferAddr;

        mpegRingbufferRead();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegRingbufferAvailableSize returning 0x%X, vcount=%d", mpegRingbuffer.getFreePackets(), Modules.sceDisplayModule.getVcount()));
        }

        return mpegRingbuffer.getFreePackets();
    }

    /**
     * sceMpegNextAvcRpAu - skip one video frame
     * 
     * @param mpeg
     * @param unknown
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x3C37A7A6, version = 150, checkInsideInterrupt = true)
    public int sceMpegNextAvcRpAu(@CheckArgument("checkMpegHandle") int mpeg, int streamUid) {
        if (!streamMap.containsKey(streamUid)) {
            log.warn(String.format("sceMpegNextAvcRpAu bad stream 0x%X", streamUid));
            return -1;
        }

        int result = hleMpegGetAvcAu(null);
        if (result != 0) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceMpegNextAvcRpAu returning 0x%08X", result));
        	}
        	return result;
        }

    	videoFrameCount++;
    	startedMpeg = true;

    	return 0;
    }

    @HLEFunction(nid = 0x01977054, version = 150)
    public int sceMpegGetUserdataAu(@CheckArgument("checkMpegHandle") int mpeg, int streamUid, TPointer auAddr, @CanBeNull TPointer headerAddr) {
    	if (!hasPsmfUserdataStream()) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMpegGetUserdataAu no registered user data stream, returning 0x%08X", SceKernelErrors.ERROR_MPEG_NO_DATA));
    		}
    		return SceKernelErrors.ERROR_MPEG_NO_DATA;
    	}

    	if (userDataPesHeader == null) {
    		userDataPesHeader = new PesHeader(getRegisteredUserDataChannel());
    		userDataPesHeader.setDtsPts(UNKNOWN_TIMESTAMP);
    	}

    	mpegUserDataAu.read(auAddr);

    	if (userDataBuffer == null) {
    		userDataBuffer = new UserDataBuffer(mpegUserDataAu.esBuffer, MPEG_DATA_ES_SIZE);
    	}

        readNextUserDataFrame(userDataPesHeader);
    	if (userDataLength == 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMpegGetUserdataAu no user data available, returning 0x%08X", SceKernelErrors.ERROR_MPEG_NO_DATA));
    		}
    		return SceKernelErrors.ERROR_MPEG_NO_DATA;
    	}
    	if (userDataBuffer.getLength() < userDataLength) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceMpegGetUserdataAu no enough user data available (0x%X from 0x%X), returning 0x%08X", userDataBuffer.getLength(), userDataLength, SceKernelErrors.ERROR_MPEG_NO_DATA));
    		}
    		return SceKernelErrors.ERROR_MPEG_NO_DATA;
    	}

    	Memory mem = auAddr.getMemory();
    	mpegUserDataAu.pts = userDataPesHeader.getPts();
    	mpegUserDataAu.dts = UNKNOWN_TIMESTAMP; // dts is always -1
    	mpegUserDataAu.esSize = userDataLength;
    	mpegUserDataAu.write(auAddr);
    	userDataBuffer.notifyRead(mem, mpegUserDataAu.esSize);
		userDataLength = 0;

    	if (headerAddr.isNotNull()) {
	    	// First 8 bytes of the user data header
	    	for (int i = 0; i < userDataHeader.length; i++) {
	    		headerAddr.setValue8(i, (byte) userDataHeader[i]);
	    	}
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegGetUserdataAu returning au=%s", mpegUserDataAu));
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("mpegUserDataAu.esBuffer: %s", Utilities.getMemoryDump(mpegUserDataAu.esBuffer, mpegUserDataAu.esSize)));
    			if (headerAddr.isNotNull()) {
    				log.trace(String.format("headerAddr: %s", Utilities.getMemoryDump(headerAddr.getAddress(), 8)));
    			}
    		}
    	}

    	return 0;
    }

    @HLEFunction(nid = 0xC45C99CC, version = 150)
    public int sceMpegQueryUserdataEsSize(@CheckArgument("checkMpegHandle") int mpeg, TPointer32 esSizeAddr, TPointer32 outSizeAddr) {
    	esSizeAddr.setValue(MPEG_DATA_ES_SIZE);
    	outSizeAddr.setValue(MPEG_DATA_ES_OUTPUT_SIZE);

    	return 0;
    }

    @HLEFunction(nid = 0x0558B075, version = 150)
    public int sceMpegAvcCopyYCbCr(@CheckArgument("checkMpegHandle") int mpeg, TPointer destinationAddr, TPointer sourceAddr) {
        int size = getYCbCrSize() + YCBCR_DATA_OFFSET;

        destinationAddr.getMemory().memcpy(destinationAddr.getAddress(), sourceAddr.getAddress(), size);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegAvcCopyYCbCr from 0x%08X-0x%08X to 0x%08X-0x%08X", sourceAddr.getAddress(), sourceAddr.getAddress() + size, destinationAddr.getAddress(), destinationAddr.getAddress() + size));
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x11F95CF1, version = 150)
    public int sceMpegGetAvcNalAu() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x921FCCCF, version = 150)
    public int sceMpegGetAvcEsAu() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6F314410, version = 150)
    public int sceMpegAvcDecodeGetDecodeSEI() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAB0E9556, version = 150)
    public int sceMpegAvcDecodeDetailIndex() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCF3547A2, version = 150)
    public int sceMpegAvcDecodeDetail2() {
        return 0;
    }

    @HLEFunction(nid = 0xF5E7EA31, version = 150)
    public int sceMpegAvcConvertToYuv420(int mpeg, TPointer yuv420Buffer, TPointer yCbCrBuffer, int unknown2) {
        int size = getYCbCrSize();

        yCbCrBuffer.getMemory().memcpy(yuv420Buffer.getAddress(), yCbCrBuffer.getAddress() + YCBCR_DATA_OFFSET, size);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceMpegAvcConvertToYuv420 from 0x%08X-0x%08X to 0x%08X-0x%08X", yCbCrBuffer.getAddress(), yCbCrBuffer.getAddress() + size, yuv420Buffer.getAddress(), yuv420Buffer.getAddress() + size));
        }

        // The YUV420 image will be decoded and saved to memory by sceJpegCsc

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1CE4950, version = 150)
    public int sceMpegAvcCscMode() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDBB60658, version = 150)
    public int sceMpegFlushAu() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE95838F6, version = 150)
    public int sceMpegAvcCscInfo() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x11CAB459, version = 150)
    public int sceMpeg_11CAB459() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB27711A8, version = 150)
    public int sceMpeg_B27711A8() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD4DD6E75, version = 150)
    public int sceMpeg_D4DD6E75() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC345DED2, version = 150)
    public int sceMpeg_C345DED2() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x988E9E12, version = 150)
    public int sceMpeg_988E9E12() {
        return 0;
    }
}