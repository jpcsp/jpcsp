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

import static jpcsp.HLE.modules150.sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_565;
import static jpcsp.HLE.modules150.sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned32;

import jpcsp.HLE.HLEFunction;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.Debugger.MemoryViewer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.kernel.types.SceMpegRingbuffer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.connector.MpegCodec;
import jpcsp.graphics.VideoEngine;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceMpeg extends HLEModule implements HLEStartModule {

    protected static Logger log = Modules.getLogger("sceMpeg");

    @Override
    public String getName() {
        return "sceMpeg";
    }

    @Override
    public void start() {
        mpegHandle = 0;
        isCurrentMpegAnalyzed = false;
        mpegRingbuffer = null;
        mpegRingbufferAddr = 0;
        avcAuAddr = 0;
        atracAuAddr = 0;
        atracStreamsMap = new HashMap<Integer, Integer>();
        avcStreamsMap = new HashMap<Integer, Integer>();
        pcmStreamsMap = new HashMap<Integer, Integer>();
        mpegAtracAu = new SceMpegAu();
        mpegAvcAu = new SceMpegAu();
        afterRingbufferPutCallback = new AfterRingbufferPutCallback();
        if (isEnableConnector()) {
            mpegCodec = new MpegCodec();
        }
        if (checkMediaEngineState()) {
            me = new MediaEngine();
            meChannel = null;
        }

        encodedVideoFramesYCbCr = new HashMap<Integer, byte[]>();
        audioDecodeBuffer = new byte[MPEG_ATRAC_ES_OUTPUT_SIZE];
        esBufferMap = new HashMap<Integer, SysMemInfo>();
        streamMap = new HashMap<Integer, StreamInfo>();
    }

    @Override
    public void stop() {
    }
    public static boolean useMpegCodec = false;
    public static boolean enableMediaEngine = false;

    // MPEG statics.
    public static final int PSMF_MAGIC = 0x464D5350;
    public static final int PSMF_VERSION_0012 = 0x32313030;
    public static final int PSMF_VERSION_0013 = 0x33313030;
    public static final int PSMF_VERSION_0014 = 0x34313030;
    public static final int PSMF_VERSION_0015 = 0x35313030;
    public static final int PSMF_STREAM_VERSION_OFFSET = 4;
    public static final int PSMF_STREAM_OFFSET_OFFSET = 8;
    public static final int PSMF_STREAM_SIZE_OFFSET = 12;
    public static final int PSMF_FIRST_TIMESTAMP_OFFSET = 86;
    public static final int PSMF_LAST_TIMESTAMP_OFFSET = 92;
    public static final int PSMF_AUDIO_CHANNEL_CONFIG_MONO = 1;
    public static final int PSMF_AUDIO_CHANNEL_CONFIG_STEREO = 2;
    public static final int PSMF_AUDIO_DEFAULT_SAMPLE_FREQUENCY = 2;
    protected static final int MPEG_MEMSIZE = 0x10000;          // 64k.
    public static final int atracDecodeDelay = 3000;         // Microseconds
    public static final int avcDecodeDelay = 5400;           // Microseconds
    public static final int mpegDecodeErrorDelay = 100;      // Delay in Microseconds in case of decode error
    public static final int mpegTimestampPerSecond = 90000; // How many MPEG Timestamp units in a second.
    public static final int videoTimestampStep = 3003;      // Value based on pmfplayer (mpegTimestampPerSecond / 29.970 (fps)).
    public static final int audioTimestampStep = 4180;      // For audio play at 44100 Hz (2048 samples / 44100 * mpegTimestampPerSecond == 4180)
    //public static final int audioFirstTimestamp = 89249;    // The first MPEG audio AU has always this timestamp
    public static final int audioFirstTimestamp = 90000;    // The first MPEG audio AU has always this timestamp
    public static final long UNKNOWN_TIMESTAMP = -1;

    // At least 2048 bytes of MPEG data is provided when analysing the MPEG header
    public static final int MPEG_HEADER_BUFFER_MINIMUM_SIZE = 2048;

    // MPEG processing vars.
    protected int mpegHandle;
    protected SceMpegRingbuffer mpegRingbuffer;
    protected AfterRingbufferPutCallback afterRingbufferPutCallback;
    protected int mpegRingbufferAddr;
    protected int mpegStreamSize;
    protected SceMpegAu mpegAtracAu;
    protected SceMpegAu mpegAvcAu;
    protected long lastAtracSystemTime;
    protected long lastAvcSystemTime;
    protected int avcAuAddr;
    protected int atracAuAddr;
    protected boolean endOfAudioReached;
    protected boolean endOfVideoReached;
    protected long mpegLastTimestamp;
    protected long mpegFirstTimestamp;
    protected Date mpegFirstDate;
    protected Date mpegLastDate;
    protected int videoFrameCount;
    protected int audioFrameCount;
    protected int videoPixelMode;
    protected int avcDetailFrameWidth;
    protected int avcDetailFrameHeight;
    protected int defaultFrameWidth;
    protected boolean isCurrentMpegAnalyzed;;
    public int maxAheadTimestamp = 40000;
    // MPEG AVC elementary stream.
    protected static final int MPEG_AVC_ES_SIZE = 2048;          // MPEG packet size.
    // MPEG ATRAC elementary stream.
    protected static final int MPEG_ATRAC_ES_SIZE = 2112;
    public    static final int MPEG_ATRAC_ES_OUTPUT_SIZE = 8192;
    // MPEG PCM elementary stream.
    protected static final int MPEG_PCM_ES_SIZE = 320;
    protected static final int MPEG_PCM_ES_OUTPUT_SIZE = 320;
    // MPEG analysis results.
    public static final int MPEG_VERSION_0012 = 0;
    public static final int MPEG_VERSION_0013 = 1;
    public static final int MPEG_VERSION_0014 = 2;
    public static final int MPEG_VERSION_0015 = 3;
    protected int mpegVersion;
    protected int mpegRawVersion;
    protected int mpegMagic;
    protected int mpegOffset;
    protected int mpegStreamAddr;
    // MPEG streams.
    public static final int MPEG_AVC_STREAM = 0;
    public static final int MPEG_ATRAC_STREAM = 1;
    public static final int MPEG_PCM_STREAM = 2;
    public static final int MPEG_DATA_STREAM = 3;      // Arbitrary user defined type. Can represent audio or video.
    public static final int MPEG_AUDIO_STREAM = 15;
    protected static final int MPEG_AU_MODE_DECODE = 0;
    protected static final int MPEG_AU_MODE_SKIP = 1;
    protected HashMap<Integer, Integer> atracStreamsMap;
    protected HashMap<Integer, Integer> avcStreamsMap;
    protected HashMap<Integer, Integer> pcmStreamsMap;
    protected boolean isAtracRegistered = false;
    protected boolean isAvcRegistered = false;
    protected boolean isPcmRegistered = false;
    protected boolean ignoreAtrac = false;
    protected boolean ignoreAvc = false;
    protected boolean ignorePcm = false;
    // MPEG decoding results.
    protected static final int MPEG_AVC_DECODE_SUCCESS = 1;       // Internal value.
    protected static final int MPEG_AVC_DECODE_ERROR_FATAL = -8;
    protected int avcDecodeResult;
    protected int avcFrameStatus;
    protected MpegCodec mpegCodec;
    protected MediaEngine me;
    protected PacketChannel meChannel;
    protected HashMap<Integer, byte[]> encodedVideoFramesYCbCr;
    protected byte[] audioDecodeBuffer;
    protected HashMap<Integer, SysMemInfo> esBufferMap;
    protected HashMap<Integer, StreamInfo> streamMap;
    protected static final String streamPurpose = "sceMpeg-Stream";

    private class StreamInfo {
    	private int uid;
    	private int type;

    	public StreamInfo(int type) {
    		this.type = type;
    		uid = SceUidManager.getNewUid(streamPurpose);
    		streamMap.put(uid, this);
    	}

    	public int getUid() {
    		return uid;
    	}

    	public int getType() {
    		return type;
    	}

    	public void release() {
    		SceUidManager.releaseId(uid, streamPurpose);
    		uid = -1;
    		type = -1;
    	}
    }

    public static boolean isEnableConnector() {
        return useMpegCodec;
    }

    public static void setEnableConnector(boolean useConnector) {
        sceMpeg.useMpegCodec = useConnector;
        if (useConnector) {
            log.info("Using JPCSP connector");
        }
    }

    public static boolean checkMediaEngineState() {
        return enableMediaEngine;
    }

    public static void setEnableMediaEngine(boolean enableMediaEngine) {
        sceMpeg.enableMediaEngine = enableMediaEngine;
        if (enableMediaEngine) {
            log.info("Media Engine enabled");
        }
    }

    protected Date convertTimestampToDate(long timestamp) {
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

    protected void writeTimestamp(Memory mem, int address, long ts) {
        mem.write32(address, (int) ((ts >> 32) & 0x1));
        mem.write32(address + 4, (int) ts);
    }

    protected boolean isCurrentMpegAnalyzed() {
        return isCurrentMpegAnalyzed;
    }

    public void setCurrentMpegAnalyzed(boolean status) {
        isCurrentMpegAnalyzed = status;
    }

    protected void analyseMpeg(int buffer_addr) {
        Memory mem = Memory.getInstance();

        mpegStreamAddr = buffer_addr;
        mpegMagic = mem.read32(buffer_addr);
        mpegRawVersion = mem.read32(buffer_addr + PSMF_STREAM_VERSION_OFFSET);
        switch (mpegRawVersion) {
            case PSMF_VERSION_0012:
                mpegVersion = MPEG_VERSION_0012;
                break;
            case PSMF_VERSION_0013:
                mpegVersion = MPEG_VERSION_0013;
                break;
            case PSMF_VERSION_0014:
                mpegVersion = MPEG_VERSION_0014;
                break;
            case PSMF_VERSION_0015:
                mpegVersion = MPEG_VERSION_0015;
                break;
            default:
                mpegVersion = -1;
                break;
        }
        mpegOffset = endianSwap32(mem.read32(buffer_addr + PSMF_STREAM_OFFSET_OFFSET));
        mpegStreamSize = endianSwap32(mem.read32(buffer_addr + PSMF_STREAM_SIZE_OFFSET));
        mpegFirstTimestamp = endianSwap32(readUnaligned32(mem, buffer_addr + PSMF_FIRST_TIMESTAMP_OFFSET));
        mpegLastTimestamp = endianSwap32(readUnaligned32(mem, buffer_addr + PSMF_LAST_TIMESTAMP_OFFSET));
        mpegFirstDate = convertTimestampToDate(mpegFirstTimestamp);
        mpegLastDate = convertTimestampToDate(mpegLastTimestamp);
        avcDetailFrameWidth = (mem.read8(buffer_addr + 142) * 0x10);
        avcDetailFrameHeight = (mem.read8(buffer_addr + 143) * 0x10);
        avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;
        avcFrameStatus = 0;
        if ((mpegRingbuffer != null) && !isCurrentMpegAnalyzed()) {
            mpegRingbuffer.reset();
            mpegRingbuffer.write(mem, mpegRingbufferAddr);
        }
        mpegAtracAu.dts = UNKNOWN_TIMESTAMP;
        mpegAtracAu.pts = 0;
        mpegAvcAu.dts = 0;
        mpegAvcAu.pts = 0;
        videoFrameCount = 0;
        audioFrameCount = 0;
        endOfAudioReached = false;
        endOfVideoReached = false;
        if ((mpegStreamSize > 0) && !isCurrentMpegAnalyzed()) {
            if (checkMediaEngineState()) {
            	me.init(buffer_addr, mpegStreamSize, mpegOffset);
            	meChannel = new PacketChannel();
                meChannel.write(buffer_addr, mpegOffset);
            } else if (isEnableConnector()) {
                mpegCodec.init(mpegVersion, mpegStreamSize, mpegLastTimestamp);
                mpegCodec.writeVideo(buffer_addr, mpegOffset);
            }
        }
        // When used with scePsmf, some applications attempt to use sceMpegQueryStreamOffset
        // and sceMpegQueryStreamSize, which forces a packet overwrite in the Media Engine and in
        // the MPEG ringbuffer.
        // Mark the current MPEG as analyzed to filter this, and restore it at sceMpegFinish.
        setCurrentMpegAnalyzed(true);

        if (log.isDebugEnabled()) {
	    	log.debug(String.format("Stream offset: %d, Stream size: 0x%X", mpegOffset, mpegStreamSize));
	    	log.debug(String.format("First timestamp: %d, Last timestamp: %d", mpegFirstTimestamp, mpegLastTimestamp));
	        if (log.isTraceEnabled()) {
	        	for (int i = 0; i < MPEG_HEADER_BUFFER_MINIMUM_SIZE; i+= 16) {
	        		log.trace(MemoryViewer.getMemoryView(buffer_addr + i));
	        	}
	        }
        }
    }

    public static int getMaxAheadTimestamp(int packets) {
        return Math.max(40000, packets * 700); // Empiric value based on tests using JpcspConnector
    }

    private void generateFakeMPEGVideo(int dest_addr, int frameWidth) {
    	generateFakeImage(dest_addr, frameWidth, avcDetailFrameWidth, avcDetailFrameHeight, videoPixelMode);
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

    protected void updateAvcDts() {
        mpegAvcAu.dts = mpegAvcAu.pts - videoTimestampStep; // DTS is always 1 frame before PTS
    }

    protected void finishMpeg() {
        if (checkMediaEngineState()) {
            me.finish();
            if (meChannel != null) {
            	meChannel.clear();
            }
        } else if (isEnableConnector()) {
            mpegCodec.finish();
        }
        setCurrentMpegAnalyzed(false);
        VideoEngine.getInstance().resetVideoTextures();
    }

    @HLEFunction(nid = 0x21FF80E4, version = 150)
    public void sceMpegQueryStreamOffset(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int buffer_addr = cpu.gpr[5];
        int offset_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryStreamOffset(mpeg=0x" + Integer.toHexString(mpeg) + ", buffer=0x" + Integer.toHexString(buffer_addr) + ", offset=0x" + Integer.toHexString(offset_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegQueryStreamOffset bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (Memory.isAddressGood(buffer_addr) && Memory.isAddressGood(offset_addr)) {
            analyseMpeg(buffer_addr);
            if (mpegMagic == PSMF_MAGIC) {
                if (mpegVersion < 0) {
                    log.warn("sceMpegQueryStreamOffset bad version " + String.format("0x%08X", mpegRawVersion));
                    mem.write32(offset_addr, 0);
                    cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_BAD_VERSION;
                } else {
                    if ((mpegOffset & 2047) != 0 || mpegOffset == 0) {
                        log.warn("sceMpegQueryStreamOffset bad offset " + String.format("0x%08X", mpegOffset));
                        mem.write32(offset_addr, 0);
                        cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
                    } else {
                        mem.write32(offset_addr, mpegOffset);
                        cpu.gpr[2] = 0;
                    }
                }
            } else {
                log.warn("sceMpegQueryStreamOffset bad magic " + String.format("0x%08X", mpegMagic));
                mem.write32(offset_addr, 0);
                cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
            }
        } else {
            log.warn("sceMpegQueryStreamOffset bad address " + String.format("0x%08X 0x%08X", buffer_addr, offset_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x611E9E11, version = 150)
    public void sceMpegQueryStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer_addr = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryStreamSize(buffer=0x" + Integer.toHexString(buffer_addr) + ", size=0x" + Integer.toHexString(size_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(buffer_addr) && Memory.isAddressGood(size_addr)) {
            analyseMpeg(buffer_addr);
            if (mpegMagic == PSMF_MAGIC) {
                if ((mpegStreamSize & 2047) == 0) {
                    mem.write32(size_addr, mpegStreamSize);
                    cpu.gpr[2] = 0;
                } else {
                    mem.write32(size_addr, 0);
                    cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
                }
            } else {
                log.warn("sceMpegQueryStreamSize bad magic " + String.format("0x%08X", mpegMagic));
                cpu.gpr[2] = -1;
            }
        } else {
            log.warn("sceMpegQueryStreamSize bad address " + String.format("0x%08X 0x%08X", buffer_addr, size_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x682A619B, version = 150)
    public void sceMpegInit(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isInfoEnabled()) {
            log.info("sceMpegInit");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (checkMediaEngineState()) {
            meChannel = null;
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x874624D6, version = 150)
    public void sceMpegFinish(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isInfoEnabled()) {
            log.info("sceMpegFinish");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        finishMpeg();

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xC132E22F, version = 150)
    public void sceMpegQueryMemSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryMemSize(mode=" + mode + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Mode = 0 -> 64k (constant).
        cpu.gpr[2] = MPEG_MEMSIZE;
    }

    @HLEFunction(nid = 0xD8C5F121, version = 150)
    public void sceMpegCreate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int data = cpu.gpr[5];
        int size = cpu.gpr[6];
        int ringbuffer_addr = cpu.gpr[7];
        int frameWidth = cpu.gpr[8];
        int mode = cpu.gpr[9];
        int ddrtop = cpu.gpr[10];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegCreate(mpeg=0x" + Integer.toHexString(mpeg) + ", data=0x" + Integer.toHexString(data) + ", size=" + size + ", ringbuffer=0x" + Integer.toHexString(ringbuffer_addr) + ", frameWidth=" + frameWidth + ", mode=" + mode + ", ddrtop=0x" + Integer.toHexString(ddrtop) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (size < MPEG_MEMSIZE) {
            log.warn("sceMpegCreate bad size " + size);
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_MEMORY;
        } else if (Memory.isAddressGood(mpeg) && Memory.isAddressGood(data) && Memory.isAddressGood(ringbuffer_addr)) {
            // Update the ring buffer struct.
            SceMpegRingbuffer ringbuffer = SceMpegRingbuffer.fromMem(mem, ringbuffer_addr);
            if (ringbuffer.packetSize == 0) {
                ringbuffer.packetsFree = 0;
            } else {
                ringbuffer.packetsFree = (ringbuffer.dataUpperBound - ringbuffer.data) / ringbuffer.packetSize;
            }
            ringbuffer.mpeg = mpeg;
            ringbuffer.write(mem, ringbuffer_addr);
            // Write mpeg system handle.
            mpegHandle = data + 0x30;
            mem.write32(mpeg, mpegHandle);
            // Initialise fake mpeg struct.
            Utilities.writeStringZ(mem, mpegHandle, "LIBMPEG.001");
            mem.write32(mpegHandle + 12, -1);
            mem.write32(mpegHandle + 16, ringbuffer_addr);
            mem.write32(mpegHandle + 20, ringbuffer.dataUpperBound);
            // Initialise mpeg values.
            mpegRingbufferAddr = ringbuffer_addr;
            mpegRingbuffer = ringbuffer;
            videoFrameCount = 0;
            audioFrameCount = 0;
            videoPixelMode = PSP_DISPLAY_PIXEL_FORMAT_8888;
            defaultFrameWidth = frameWidth;

            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegCreate bad address " + String.format("0x%08X 0x%08X 0x%08X", mpeg, data, ringbuffer_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x606A4649, version = 150)
    public void sceMpegDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegDelete(mpeg=0x" + Integer.toHexString(mpeg) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        finishMpeg();
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegDelete bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x42560F23, version = 150)
    public void sceMpegRegistStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_type = cpu.gpr[5];
        int stream_num = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegRegistStream(mpeg=0x" + Integer.toHexString(mpeg) + ", stream_type=" + stream_type + ", stream_num=" + stream_num + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegRegistStream bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {
        	StreamInfo info = new StreamInfo(stream_type);
        	int uid = info.getUid();
            // Register the respective stream.
            switch (stream_type) {
                case MPEG_AVC_STREAM:
                    isAvcRegistered = true;
                    avcStreamsMap.put(uid, stream_num);
                    break;
                case MPEG_AUDIO_STREAM:  // Unknown purpose. Use Atrac anyway.
                case MPEG_ATRAC_STREAM:
                    isAtracRegistered = true;
                    atracStreamsMap.put(uid, stream_num);
                    break;
                case MPEG_PCM_STREAM:
                    isPcmRegistered = true;
                    pcmStreamsMap.put(uid, stream_num);
                    break;
                default:
                    log.warn("sceMpegRegistStream unknown stream type=" + stream_type);
                    break;
            }
            cpu.gpr[2] = uid;
        }
    }

    @HLEFunction(nid = 0x591A4AA2, version = 150)
    public void sceMpegUnRegistStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int streamUid = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegUnRegistStream(mpeg=0x" + Integer.toHexString(mpeg) + ", stream=0x" + Integer.toHexString(streamUid) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegUnRegistStream bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {
        	StreamInfo info = getStreamInfo(streamUid);
        	if (info != null) {
	            // Unregister the respective stream.
	            switch (info.getType()) {
	                case MPEG_AVC_STREAM:
	                    isAvcRegistered = false;
	                    avcStreamsMap.remove(streamUid);
	                    break;
	                case MPEG_AUDIO_STREAM:  // Unknown purpose. Use Atrac anyway.
	                case MPEG_ATRAC_STREAM:
	                    isAtracRegistered = false;
	                    atracStreamsMap.remove(streamUid);
	                    break;
	                case MPEG_PCM_STREAM:
	                    isPcmRegistered = false;
	                    pcmStreamsMap.remove(streamUid);
	                    break;
	                default:
	                    log.warn("sceMpegUnRegistStream unknown stream=0x" + Integer.toHexString(streamUid));
	                    break;
	            }
        	} else {
                log.warn("sceMpegUnRegistStream unknown stream=0x" + Integer.toHexString(streamUid));
        	}
            setCurrentMpegAnalyzed(false);
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xA780CF7E, version = 150)
    public void sceMpegMallocAvcEsBuf(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegMallocAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegMallocAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ") bad mpeg handle");
            cpu.gpr[2] = -1;
        } else {
        	SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "sceMpegMallocAvcEsBuf", SysMemUserForUser.PSP_SMEM_Low, MPEG_ATRAC_ES_SIZE, 0);
        	if (info != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceMpegMallocAvcEsBuf allocated 0x%08X(size=%d)", info.addr, MPEG_ATRAC_ES_SIZE));
                }
        		esBufferMap.put(info.addr, info);
        		cpu.gpr[2] = info.addr;
        	} else {
        		cpu.gpr[2] = 0;
        	}
        }
    }

    @HLEFunction(nid = 0xCEB870B1, version = 150)
    public void sceMpegFreeAvcEsBuf(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int esBuf = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ", esBuf=0x" + Integer.toHexString(esBuf) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ", esBuf=0x" + Integer.toHexString(esBuf) + ") bad mpeg handle");
            cpu.gpr[2] = -1;
        } else if (esBuf == 0) {
            log.warn("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ", esBuf=0x" + Integer.toHexString(esBuf) + ") bad esBuf handle");
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        } else {
        	SysMemInfo info = esBufferMap.remove(esBuf);
        	if (info != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceMpegFreeAvcEsBuf freeing 0x%08X(size=%d)", info.addr, info.size));
                }
        		Modules.SysMemUserForUserModule.free(info);
        	}
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xF8DCB679, version = 150)
    public void sceMpegQueryAtracEsSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int esSize_addr = cpu.gpr[5];
        int outSize_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryAtracEsSize(mpeg=0x" + Integer.toHexString(mpeg) + ", esSize_addr=0x" + Integer.toHexString(esSize_addr) + ", outSize_addr=0x" + Integer.toHexString(outSize_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegQueryAtracEsSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (Memory.isAddressGood(esSize_addr) && Memory.isAddressGood(outSize_addr)) {
            mem.write32(esSize_addr, MPEG_ATRAC_ES_SIZE);
            mem.write32(outSize_addr, MPEG_ATRAC_ES_OUTPUT_SIZE);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegQueryAtracEsSize bad address " + String.format("0x%08X 0x%08X", esSize_addr, outSize_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0xC02CF6B5, version = 150)
    public void sceMpegQueryPcmEsSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int esSize_addr = cpu.gpr[5];
        int outSize_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryPcmEsSize(mpeg=0x" + Integer.toHexString(mpeg) + ", esSize_addr=0x" + Integer.toHexString(esSize_addr) + ", outSize_addr=0x" + Integer.toHexString(outSize_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegQueryPcmEsSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (Memory.isAddressGood(esSize_addr) && Memory.isAddressGood(outSize_addr)) {
            mem.write32(esSize_addr, MPEG_PCM_ES_SIZE);
            mem.write32(outSize_addr, MPEG_PCM_ES_OUTPUT_SIZE);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegQueryPcmEsSize bad address " + String.format("0x%08X 0x%08X", esSize_addr, outSize_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x167AFD9E, version = 150)
    public void sceMpegInitAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int buffer_addr = cpu.gpr[5];
        int au_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegInitAu(mpeg=0x" + Integer.toHexString(mpeg) + ", buffer=0x" + Integer.toHexString(buffer_addr) + ", au=0x" + Integer.toHexString(au_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegInitAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {
            // Check if sceMpegInitAu is being called for AVC or ATRAC
            // and write the proper AU (access unit) struct.
            if (esBufferMap.containsKey(buffer_addr)) {
            	mpegAvcAu.esBuffer = buffer_addr;
            	mpegAvcAu.esSize = MPEG_AVC_ES_SIZE;
            	mpegAvcAu.write(mem, au_addr);
            } else {
            	mpegAtracAu.esBuffer = buffer_addr;
            	mpegAtracAu.esSize = MPEG_ATRAC_ES_SIZE;
            	mpegAtracAu.write(mem, au_addr);
            }
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x234586AE, version = 150)
    public void sceMpegChangeGetAvcAuMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];
        int mode = cpu.gpr[6];

        log.warn("UNIMPLEMENTED: sceMpegChangeGetAvcAuMode(mpeg=0x" + Integer.toHexString(mpeg) + ",stream_addr=0x" + Integer.toHexString(stream_addr) + ",mode=0x" + mode + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x9DCFB7EA, version = 150)
    public void sceMpegChangeGetAuMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int streamUid = cpu.gpr[5];
        int mode = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegChangeGetAuMode(mpeg=0x" + Integer.toHexString(mpeg) + ",stream_addr=0x" + Integer.toHexString(streamUid) + ",mode=0x" + mode + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        StreamInfo info = getStreamInfo(streamUid);
        if (info != null) {
	        switch (info.getType()) {
	            case MPEG_AVC_STREAM:
	                if(mode == MPEG_AU_MODE_DECODE) {
	                    ignoreAvc = false;
	                } else if (mode == MPEG_AU_MODE_SKIP) {
	                    ignoreAvc = true;
	                }
	                break;
	            case MPEG_AUDIO_STREAM:
	            case MPEG_ATRAC_STREAM:
	                if(mode == MPEG_AU_MODE_DECODE) {
	                    ignoreAtrac = false;
	                } else if (mode == MPEG_AU_MODE_SKIP) {
	                    ignoreAvc = true;
	                }
	                break;
	            case MPEG_PCM_STREAM:
	                if(mode == MPEG_AU_MODE_DECODE) {
	                    ignorePcm = false;
	                } else if (mode == MPEG_AU_MODE_SKIP) {
	                    ignorePcm = true;
	                }
	                break;
	            default:
	                log.warn("sceMpegChangeGetAuMode unknown stream=0x" + Integer.toHexString(streamUid));
	                break;
	        }
        } else {
            log.warn("sceMpegChangeGetAuMode unknown stream=0x" + Integer.toHexString(streamUid));
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xFE246728, version = 150)
    public void sceMpegGetAvcAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int streamUid = cpu.gpr[5];
        int au_addr = cpu.gpr[6];
        int attr_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegGetAvcAu(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream=0x" + Integer.toHexString(streamUid)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", attr_addr=0x" + Integer.toHexString(attr_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegGetAvcAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
            delayThread(mpegDecodeErrorDelay);
        } else if (Memory.isAddressGood(streamUid) && Memory.isAddressGood(au_addr)) {
            log.warn("sceMpegGetAvcAu didn't get a fake stream");
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        } else if (streamMap.containsKey(streamUid)) {
            if ((mpegAvcAu.pts > mpegAtracAu.pts + maxAheadTimestamp) && isAtracRegistered) {
                // Video is ahead of audio, deliver no video data to wait for audio.
                if (log.isDebugEnabled()) {
                    log.debug("sceMpegGetAvcAu video ahead of audio: " + mpegAvcAu.pts + " - " + mpegAtracAu.pts);
                }
                cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
                delayThread(mpegDecodeErrorDelay);
            } else {
                cpu.gpr[2] = 0;
                // Update the video timestamp (AVC).
                if (!ignoreAvc) {
                	// Read Au of next Avc frame
                    if (checkMediaEngineState()) {
                    	Emulator.getClock().pause();
                        if (me.getContainer() == null) {
                            me.init(meChannel, true, true);
                        }
                    	if (!me.readVideoAu(mpegAvcAu)) {
                    		// Returning "ERROR_MPEG_NO_DATA" only last timestamp has been reached
                    		if (mpegLastTimestamp <= 0 || mpegAvcAu.pts >= mpegLastTimestamp) {
                    			endOfVideoReached = true;
                    			cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
                    		}
                    	} else {
                    		endOfVideoReached = false;
                    	}
                    	Emulator.getClock().resume();
                    } else if (isEnableConnector()) {
                    	if (!mpegCodec.readVideoAu(mpegAvcAu, videoFrameCount)) {
                    		// Avc Au was not updated by the MpegCodec
                            mpegAvcAu.pts += videoTimestampStep;
                    	}
                		updateAvcDts();
                    }
                	mpegAvcAu.write(mem, au_addr);
                	if (log.isDebugEnabled()) {
                		log.debug(String.format("sceMpegGetAvcAu returning 0x%08X, AvcAu=%s", cpu.gpr[2], mpegAvcAu.toString()));
                	}
                }
                // Bitfield used to store data attributes.
                if (Memory.isAddressGood(attr_addr)) {
                    mem.write32(attr_addr, 1);     // Unknown.
                }

                if (cpu.gpr[2] != 0) {
                	delayThread(mpegDecodeErrorDelay);
                }
            }
        } else {
            log.warn("sceMpegGetAvcAu bad address " + String.format("0x%08X 0x%08X", streamUid, au_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x8C1E027D, version = 150)
    public void sceMpegGetPcmAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int streamUid = cpu.gpr[5];
        int au_addr = cpu.gpr[6];
        int attr_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegGetPcmAu(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream=0x" + Integer.toHexString(streamUid)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", attr_addr=0x" + Integer.toHexString(attr_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegGetPcmAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        }else if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
            delayThread(mpegDecodeErrorDelay);
        } else if (Memory.isAddressGood(streamUid) && Memory.isAddressGood(au_addr)) {
            log.warn("sceMpegGetPcmAu didn't get a fake stream");
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        } else if (streamMap.containsKey(streamUid)) {
            cpu.gpr[2] = 0;
            // Update the audio timestamp (Atrac).
            if (!ignorePcm) {
            	// Read Au of next Atrac frame
                if (checkMediaEngineState()) {
                	Emulator.getClock().pause();
                	if (me.getContainer() == null) {
                		me.init(meChannel, true, true);
                	}
                	if (!me.readAudioAu(mpegAtracAu)) {
                        cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
                	}
                	Emulator.getClock().resume();
                } else if (isEnableConnector() && mpegCodec.readAudioAu(mpegAtracAu, audioFrameCount)) {
            		// Atrac Au updated by the MpegCodec
            	}
            	mpegAtracAu.write(mem, au_addr);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("sceMpegGetPcmAu returning AtracAu=%s", mpegAtracAu.toString()));
            	}
            }
            // Bitfield used to store data attributes.
            if (Memory.isAddressGood(attr_addr)) {
                // Uses same bitfield as the one in the PSMF header.
                mem.write32(attr_addr, (1 << 7));     // Sampling rate (1 = 44.1kHz).
                mem.write32(attr_addr, 2);            // Number of channels (1 - MONO / 2 - STEREO).
            }
            if (cpu.gpr[2] != 0) {
            	delayThread(mpegDecodeErrorDelay);
            }
        } else {
            log.warn("sceMpegGetPcmAu bad address " + String.format("0x%08X 0x%08X", streamUid, au_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0xE1CE83A7, version = 150)
    public void sceMpegGetAtracAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int streamUid = cpu.gpr[5];
        int au_addr = cpu.gpr[6];
        int attr_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegGetAtracAu(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream=0x" + Integer.toHexString(streamUid)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", attr_addr=0x" + Integer.toHexString(attr_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegGetAtracAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegGetAtracAu ringbuffer empty");
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
            delayThread(mpegDecodeErrorDelay);
        } else if (Memory.isAddressGood(streamUid) && Memory.isAddressGood(au_addr)) {
            log.warn("sceMpegGetAtracAu didn't get a fake stream");
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        } else if (streamMap.containsKey(streamUid)) {
        	if (endOfAudioReached && endOfVideoReached) {
        		if (log.isDebugEnabled()) {
        			log.debug("sceMpegGetAtracAu end of audio and video reached");
        		}

        		// Consume all the remaining packets, if any.
        		if (mpegRingbuffer.packetsFree < mpegRingbuffer.packets) {
        			mpegRingbuffer.packetsFree = mpegRingbuffer.packets;
        			mpegRingbuffer.write(mem, mpegRingbufferAddr);
        		}

        		cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
        	} else if ((mpegAtracAu.pts > mpegAvcAu.pts + maxAheadTimestamp) && isAvcRegistered && !endOfAudioReached) {
                // Audio is ahead of video, deliver no audio data to wait for video.
            	// This error is not returned when the end of audio has been reached (Patapon 3).
                if (log.isDebugEnabled()) {
                    log.debug("sceMpegGetAtracAu audio ahead of video: " + mpegAtracAu.pts + " - " + mpegAvcAu.pts);
                }
                cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
                delayThread(mpegDecodeErrorDelay);
            } else {
                // Update the audio timestamp (Atrac).
                cpu.gpr[2] = 0;
                if (!ignoreAtrac) {
                	// Read Au of next Atrac frame
                    if (checkMediaEngineState()) {
                    	Emulator.getClock().pause();
                    	if (me.getContainer() == null) {
                    		me.init(meChannel, true, true);
                    	}
                    	if (!me.readAudioAu(mpegAtracAu)) {
                    		endOfAudioReached = true;
                    		// If the audio could not be decoded or the
                    		// end of audio has been reached (Patapon 3),
                    		// simulate a successful return
                    	} else {
                    		endOfAudioReached = false;
                    	}
                    	Emulator.getClock().resume();
                    } else if (isEnableConnector() && mpegCodec.readAudioAu(mpegAtracAu, audioFrameCount)) {
                		// Atrac Au updated by the MpegCodec
                	}
                	mpegAtracAu.write(mem, au_addr);
                	if (log.isDebugEnabled()) {
                		log.debug(String.format("sceMpegGetAtracAu returning 0x%08X, AtracAu=%s", cpu.gpr[2], mpegAtracAu.toString()));
                	}
                }
                // Bitfield used to store data attributes.
                if (Memory.isAddressGood(attr_addr)) {
                    mem.write32(attr_addr, 0);     // Pointer to ATRAC3plus stream (from PSMF file).
                }

                if (cpu.gpr[2] != 0) {
                	delayThread(mpegDecodeErrorDelay);
                }
            }
        } else {
            log.warn("sceMpegGetAtracAu bad address " + String.format("0x%08X 0x%08X", streamUid, au_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x500F0429, version = 150)
    public void sceMpegFlushStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegFlushStream mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream_addr=0x" + Integer.toHexString(stream_addr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        finishMpeg();
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x707B7629, version = 150)
    public void sceMpegFlushAllStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegFlushAllStream mpeg=0x" + Integer.toHexString(mpeg));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        finishMpeg();
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x0E3C2E9D, version = 150)
    public void sceMpegAvcDecode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int au_addr = cpu.gpr[5];
        int frameWidth = cpu.gpr[6];
        int buffer_addr = cpu.gpr[7];
        int init_addr = cpu.gpr[8];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecode(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", frameWidth=" + frameWidth
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", init=0x" + Integer.toHexString(init_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // When frameWidth is 0, take the frameWidth specified at sceMpegCreate.
        if (frameWidth == 0) {
            if (defaultFrameWidth == 0) {
                frameWidth = avcDetailFrameWidth;
            } else {
                frameWidth = defaultFrameWidth;
            }
        }
        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecode bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcDecode ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegAvcDecode ringbuffer empty");
            cpu.gpr[2] = SceKernelErrors.ERROR_AVC_VIDEO_FATAL;
        } else if (Memory.isAddressGood(au_addr) && Memory.isAddressGood(buffer_addr) && Memory.isAddressGood(init_addr)) {
            int au = mem.read32(au_addr);
            int buffer = mem.read32(buffer_addr);
            int init = mem.read32(init_addr);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceMpegAvcDecode *au=0x%08X, *buffer=0x%08X, init=%d", au, buffer, init));
            }

            final int width = Math.min(480, frameWidth);
            final int height = 272;

            // Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
            VideoEngine.getInstance().addVideoTexture(buffer, buffer + height * frameWidth * sceDisplay.getPixelFormatBytes(videoPixelMode));

            long startTime = Emulator.getClock().microTime();

            int packetsInRingbuffer = mpegRingbuffer.packets - mpegRingbuffer.packetsFree;
            int processedPackets = mpegRingbuffer.packetsRead - packetsInRingbuffer;
            int processedSize = processedPackets * mpegRingbuffer.packetSize;

            // let's go with 3 packets per frame for now
            int packetsConsumed = 3;
            if (mpegStreamSize > 0 && mpegLastTimestamp > 0) {
                // Try a better approximation of the packets consumed based on the timestamp
                int processedSizeBasedOnTimestamp = (int) ((((float) mpegAvcAu.pts) / mpegLastTimestamp) * mpegStreamSize);
                if (processedSizeBasedOnTimestamp < processedSize) {
                    packetsConsumed = 0;
                } else {
                    packetsConsumed = (processedSizeBasedOnTimestamp - processedSize) / mpegRingbuffer.packetSize;
                    if (packetsConsumed > 10) {
                        packetsConsumed = 10;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceMpegAvcDecode consumed %d %d/%d %d", processedSizeBasedOnTimestamp, processedSize, mpegStreamSize, packetsConsumed));
                }
            }

            if (checkMediaEngineState()) {
            	// Suspend the emulator clock to perform time consuming HLE operation,
            	// in order to improve the timing compatibility with the PSP.
            	Emulator.getClock().pause();
                if (me.stepVideo()) {
                	me.writeVideoImage(buffer, frameWidth, videoPixelMode);
                	packetsConsumed = meChannel.getReadLength() / mpegRingbuffer.packetSize;

                	// The MediaEngine is already consuming all the remaining
                	// packets when approaching the end of the video. The PSP
                	// is only consuming the last packet when reaching the end,
                	// not before.
                	// Consuming all the remaining packets?
                	if (mpegRingbuffer.packetsFree + packetsConsumed >= mpegRingbuffer.packets) {
                		// Having not yet reached the last timestamp?
                		if (mpegLastTimestamp > 0 && mpegAvcAu.pts < mpegLastTimestamp) {
                			// Do not yet consume all the remaining packets.
                			packetsConsumed = 0;
                		}
                	}

                	meChannel.setReadLength(meChannel.getReadLength() - packetsConsumed * mpegRingbuffer.packetSize);
                } else {
                	// Consume all the remaining packets
                	packetsConsumed = mpegRingbuffer.packets - mpegRingbuffer.packetsFree;
                }
            	Emulator.getClock().resume();
                avcFrameStatus = 1;
            } else if (isEnableConnector() && mpegCodec.readVideoFrame(buffer, frameWidth, width, height, videoPixelMode, videoFrameCount)) {
                packetsConsumed = mpegCodec.getPacketsConsumed();
                avcFrameStatus = 1;
            } else {
                mpegAvcAu.pts += videoTimestampStep;
                updateAvcDts();

                // Generate static.
                generateFakeMPEGVideo(buffer, frameWidth);
                if (isEnableConnector()) {
                    mpegCodec.postFakedVideo(buffer, frameWidth, videoPixelMode);
                }
                Date currentDate = convertTimestampToDate(mpegAvcAu.pts);
                if(log.isDebugEnabled()) {
                    log.debug("currentDate: " + currentDate.toString());
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                Debug.printFramebuffer(buffer, frameWidth, 10, avcDetailFrameHeight - 22, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 1, " This is a faked MPEG video. ");
                String displayedString;
                if (mpegLastDate != null) {
                    displayedString = String.format(" %s / %s ", dateFormat.format(currentDate), dateFormat.format(mpegLastDate));
                    Debug.printFramebuffer(buffer, frameWidth, 10, 10, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 2, displayedString);
                }
                if (mpegStreamSize > 0) {
                    displayedString = String.format(" %d/%d (%.0f%%) ", processedSize, mpegStreamSize, processedSize * 100f / mpegStreamSize);
                } else {
                    displayedString = String.format(" %d ", processedSize);
                }
                Debug.printFramebuffer(buffer, frameWidth, 10, 30, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 2, displayedString);
                if (log.isDebugEnabled()) {
                    log.debug("sceMpegAvcDecode currentTimestamp=" + mpegAvcAu.pts);
                }
                avcFrameStatus = 1;
            }

            videoFrameCount++;

            if (log.isDebugEnabled()) {
                log.debug("sceMpegAvcDecode currentTimestamp=" + mpegAvcAu.pts);
            }
            if (mpegRingbuffer.packetsFree < mpegRingbuffer.packets && packetsConsumed > 0) {
                mpegRingbuffer.packetsFree = Math.min(mpegRingbuffer.packets, mpegRingbuffer.packetsFree + packetsConsumed);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceMpegAvcDecode consumed %d packets, remaining %d packets", packetsConsumed, mpegRingbuffer.packets - mpegRingbuffer.packetsFree));
                }
                mpegRingbuffer.write(mem, mpegRingbufferAddr);
            }

            // Correct decoding.
            avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;
            // Save the current frame's status (0 - not showing / 1 - showing).
            mem.write32(init_addr, avcFrameStatus);

            cpu.gpr[2] = 0;
            delayThread(startTime, avcDecodeDelay);
        } else {
            log.warn(String.format("sceMpegAvcDecode bad address 0x%08X 0x%08X", au_addr, buffer_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x0F6C18D7, version = 150)
    public void sceMpegAvcDecodeDetail(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int detailAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegAvcDecodeDetail(mpeg=0x%08X, detailAddr=0x%08X)", mpeg, detailAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeDetail bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (!Memory.isAddressGood(detailAddr)) {
            log.warn(String.format("sceMpegAvcDecodeDetail bad address 0x%08X", detailAddr));
            cpu.gpr[2] = -1;
        } else {
            mem.write32(detailAddr, avcDecodeResult);             // Stores the result.
            mem.write32(detailAddr + 4, videoFrameCount);         // Last decoded frame.
            mem.write32(detailAddr + 8, avcDetailFrameWidth);     // Frame width.
            mem.write32(detailAddr + 12, avcDetailFrameHeight);   // Frame height.
            mem.write32(detailAddr + 16, 0);                      // Frame crop rect (left).
            mem.write32(detailAddr + 20, 0);                      // Frame crop rect (right).
            mem.write32(detailAddr + 24, 0);                      // Frame crop rect (top).
            mem.write32(detailAddr + 28, 0);                      // Frame crop rect (bottom).
            mem.write32(detailAddr + 32, avcFrameStatus);         // Status of the last decoded frame.
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xA11C7026, version = 150)
    public void sceMpegAvcDecodeMode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int mode_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeMode(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", mode_addr=0x" + Integer.toHexString(mode_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn(String.format("sceMpegAvcDecodeMode bad mpeg handle 0x%08X", mpeg));
            cpu.gpr[2] = -1;
        } else if (Memory.isAddressGood(mode_addr)) {
            // -1 is a default value.
            int mode = mem.read32(mode_addr);
            int pixelMode = mem.read32(mode_addr + 4);
            if (pixelMode >= PSP_DISPLAY_PIXEL_FORMAT_565 && pixelMode <= PSP_DISPLAY_PIXEL_FORMAT_8888) {
                videoPixelMode = pixelMode;
            } else {
                log.warn("sceMpegAvcDecodeMode mode=0x" + mode + " pixel mode=" + pixelMode + ": unknown mode");
            }
            cpu.gpr[2] = 0;
        } else {
            log.warn(String.format("sceMpegAvcDecodeMode bad address 0x%08X", mode_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x740FCCD1, version = 150)
    public void sceMpegAvcDecodeStop(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int frameWidth = cpu.gpr[5];
        int buffer_addr = cpu.gpr[6];
        int status_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeStop(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", frameWidth=" + frameWidth
                    + ", buffer_addr=0x" + Integer.toHexString(buffer_addr)
                    + ", status_addr=0x" + Integer.toHexString(status_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeStop bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (Memory.isAddressGood(buffer_addr) && Memory.isAddressGood(status_addr)) {
            // No last frame generated
            mem.write32(status_addr, 0);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegAvcDecodeStop bad address " + String.format("0x%08X 0x%08X", buffer_addr, status_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x4571CC64, version = 150)
    public void sceMpegAvcDecodeFlush(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeFlush mpeg=0x" + Integer.toHexString(mpeg));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        finishMpeg();
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x211A057C, version = 150)
    public void sceMpegAvcQueryYCbCrSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mpeg = cpu.gpr[4];
        int mode = cpu.gpr[5];       // 1 -> Loaded from file. 2 -> Loaded from memory.
        int width = cpu.gpr[6];      // 480.
        int height = cpu.gpr[7];     // 272.
        int resultAddr = cpu.gpr[8]; // Where to store the result.

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcQueryYCbCrSize(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", mode=" + mode
                    + ", width=" + width
                    + ", height=" + height
                    + ", resultAddr=0x" + Integer.toHexString(resultAddr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcQueryYCbCrSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if ((width & 15) != 0 || (height & 15) != 0 || width > 480 || height > 272) {
            log.warn("sceMpegAvcQueryYCbCrSize invalid size width=" + width + ", height=" + height);
        	cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        } else if (Memory.isAddressGood(resultAddr)) {
        	// Write the size of the buffer used by sceMpegAvcDecodeYCbCr
    		int size = (width / 2) * (height / 2) * 6 + 128;
            mem.write32(resultAddr, size);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegAvcQueryYCbCrSize bad result address 0x" + Integer.toHexString(resultAddr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x67179B1B, version = 150)
    public void sceMpegAvcInitYCbCr(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int mode = cpu.gpr[5];
        int width = cpu.gpr[6];
        int height = cpu.gpr[7];
        int ycbcr_addr = cpu.gpr[8];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcInitYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", YCbCr_addr=0x" + Integer.toHexString(ycbcr_addr)
                    + ", mode=" + mode
                    + ", width=" + width
                    + ", height=" + height + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        encodedVideoFramesYCbCr.remove(ycbcr_addr);
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xF0EB1125, version = 150)
    public void sceMpegAvcDecodeYCbCr(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mpeg = cpu.gpr[4];
        int au_addr = cpu.gpr[5];
        int buffer_addr = cpu.gpr[6];
        int init_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", init=0x" + Integer.toHexString(init_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeYCbCr bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcDecodeYCbCr ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegAvcDecodeYCbCr ringbuffer empty");
            cpu.gpr[2] = SceKernelErrors.ERROR_AVC_VIDEO_FATAL;
        } else if (Memory.isAddressGood(au_addr) && Memory.isAddressGood(buffer_addr) && Memory.isAddressGood(init_addr)) {
            // Decode the video data in YCbCr mode.
        	long startTime = Emulator.getClock().microTime();

            int packetsInRingbuffer = mpegRingbuffer.packets - mpegRingbuffer.packetsFree;
            int processedPackets = mpegRingbuffer.packetsRead - packetsInRingbuffer;
            int processedSize = processedPackets * mpegRingbuffer.packetSize;

            // let's go with 3 packets per frame for now
            int packetsConsumed = 3;
            if (mpegStreamSize > 0 && mpegLastTimestamp > 0) {
                // Try a better approximation of the packets consumed based on the timestamp
                int processedSizeBasedOnTimestamp = (int) ((((float) mpegAvcAu.pts) / mpegLastTimestamp) * mpegStreamSize);
                if (processedSizeBasedOnTimestamp < processedSize) {
                    packetsConsumed = 0;
                } else {
                    packetsConsumed = (processedSizeBasedOnTimestamp - processedSize) / mpegRingbuffer.packetSize;
                    if (packetsConsumed > 10) {
                        packetsConsumed = 10;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceMpegAvcDecodeYCbCr consumed %d %d/%d %d", processedSizeBasedOnTimestamp, processedSize, mpegStreamSize, packetsConsumed));
                }
            }

            // sceMpegAvcDecodeYCbCr() is performing the video decoding and
            // sceMpegAvcCsc() is transforming the YCbCr image into ABGR.
            // Both the MediaEngine and the JpcspConnector are supporting
            // this 2 steps approach.
            if (checkMediaEngineState()) {
                if (me.stepVideo()) {
                	packetsConsumed = meChannel.getReadLength() / mpegRingbuffer.packetSize;
                	meChannel.setReadLength(meChannel.getReadLength() - packetsConsumed * mpegRingbuffer.packetSize);
                } else {
                	// Consume all the remaining packets
                	packetsConsumed = mpegRingbuffer.packets - mpegRingbuffer.packetsFree;
                }
                avcFrameStatus = 1;
            } else if (isEnableConnector()) {
            	// Store the encoded video frame for real decoding in sceMpegAvcCsc()
            	byte[] encodedVideoFrame = mpegCodec.readEncodedVideoFrame(videoFrameCount);
            	if (encodedVideoFrame != null) {
	            	int buffer = mem.read32(buffer_addr);
	            	encodedVideoFramesYCbCr.put(buffer, encodedVideoFrame);
            	}
            	packetsConsumed = 0;
            	avcFrameStatus = 1;
            } else {
                mpegAvcAu.pts += videoTimestampStep;
                updateAvcDts();

                packetsConsumed = 0;
                avcFrameStatus = 1;
            }

            videoFrameCount++;

            if (log.isDebugEnabled()) {
                log.debug("sceMpegAvcDecodeYCbCr currentTimestamp=" + mpegAvcAu.pts);
            }

            if (mpegRingbuffer.packetsFree < mpegRingbuffer.packets && packetsConsumed > 0) {
                mpegRingbuffer.packetsFree = Math.min(mpegRingbuffer.packets, mpegRingbuffer.packetsFree + packetsConsumed);
                mpegRingbuffer.write(mem, mpegRingbufferAddr);
            }

            // Correct decoding.
            avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;
            // Save the current frame's status (0 - not showing / 1 - showing).
            mem.write32(init_addr, avcFrameStatus);
            cpu.gpr[2] = 0;
            delayThread(startTime, avcDecodeDelay);
        } else {
            log.warn("sceMpegAvcDecodeYCbCr bad address " + String.format("0x%08X 0x%08X", au_addr, buffer_addr));

            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0xF2930C9C, version = 150)
    public void sceMpegAvcDecodeStopYCbCr(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int buffer_addr = cpu.gpr[5];
        int status_addr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeStopYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", status=0x" + Integer.toHexString(status_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeStopYCbCr bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (Memory.isAddressGood(buffer_addr) && Memory.isAddressGood(status_addr)) {
            // No last frame generated
            mem.write32(status_addr, 0);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegAvcDecodeStopYCbCr bad address " + String.format("0x%08X 0x%08X", buffer_addr, status_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x31BD0272, version = 150)
    public void sceMpegAvcCsc(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int source_addr = cpu.gpr[5]; // YCbCr data.
        int range_addr = cpu.gpr[6];  // YCbCr range.
        int frameWidth = cpu.gpr[7];
        int dest_addr = cpu.gpr[8];   // Converted data (RGB).

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcCsc(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", source=0x" + Integer.toHexString(source_addr)
                    + ", range_addr=0x" + Integer.toHexString(range_addr)
                    + ", frameWidth=" + frameWidth
                    + ", dest=0x" + Integer.toHexString(dest_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // When frameWidth is 0, take the frameWidth specified at sceMpegCreate.
        if (frameWidth == 0) {
            if (defaultFrameWidth == 0) {
                frameWidth = avcDetailFrameWidth;
            } else {
                frameWidth = defaultFrameWidth;
            }
        }
        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcCsc bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcCsc ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegAvcCsc ringbuffer empty");
            cpu.gpr[2] = SceKernelErrors.ERROR_AVC_VIDEO_FATAL;
        } else if (Memory.isAddressGood(dest_addr) && Memory.isAddressGood(source_addr) && Memory.isAddressGood(range_addr)) {
            int rangeWidthStart = mem.read32(range_addr);
            int rangeHeightStart = mem.read32(range_addr + 4);
            int rangeWidthEnd = mem.read32(range_addr + 8);
            int rangeHeightEnd = mem.read32(range_addr + 12);
            if (log.isDebugEnabled()) {
                log.debug("sceMpegAvcCsc range -" + " x:" + rangeWidthStart + " y:" + rangeHeightStart + " xLen:" + rangeWidthEnd + " yLen:" + rangeHeightEnd);
            }

            // Do not cache the video image as a texture in the VideoEngine to allow fluid rendering
            VideoEngine.getInstance().addVideoTexture(dest_addr, dest_addr + (rangeHeightStart + rangeHeightEnd) * frameWidth * sceDisplay.getPixelFormatBytes(videoPixelMode));

            // sceMpegAvcDecodeYCbCr() is performing the video decoding and
            // sceMpegAvcCsc() is transforming the YCbCr image into ABGR.
            // Currently, only the MediaEngine is supporting these 2 steps approach.
            // The other methods (JpcspConnector and Faked video) are performing
            // both steps together: this is done in here.
            if (checkMediaEngineState()) {
                if (me.getContainer() != null) {
                    me.writeVideoImageWithRange(dest_addr, frameWidth, videoPixelMode, rangeWidthStart, rangeHeightStart, rangeWidthEnd, rangeHeightEnd);
                }
            } else {
            	long startTime = Emulator.getClock().microTime();

                int packetsInRingbuffer = mpegRingbuffer.packets - mpegRingbuffer.packetsFree;
                int processedPackets = mpegRingbuffer.packetsRead - packetsInRingbuffer;
                int processedSize = processedPackets * mpegRingbuffer.packetSize;

                // let's go with 3 packets per frame for now
                int packetsConsumed = 3;
                if (mpegStreamSize > 0 && mpegLastTimestamp > 0) {
                    // Try a better approximation of the packets consumed based on the timestamp
                    int processedSizeBasedOnTimestamp = (int) ((((float) mpegAvcAu.pts) / mpegLastTimestamp) * mpegStreamSize);
                    if (processedSizeBasedOnTimestamp < processedSize) {
                        packetsConsumed = 0;
                    } else {
                        packetsConsumed = (processedSizeBasedOnTimestamp - processedSize) / mpegRingbuffer.packetSize;
                        if (packetsConsumed > 10) {
                            packetsConsumed = 10;
                        }
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("sceMpegAvcCsc consumed %d %d/%d %d", processedSizeBasedOnTimestamp, processedSize, mpegStreamSize, packetsConsumed));
                    }
                }

                if (isEnableConnector() && encodedVideoFramesYCbCr.containsKey(source_addr)) {
                	byte[] encodedVideoFrame = encodedVideoFramesYCbCr.get(source_addr);
                    mpegCodec.decodeVideoFrame(encodedVideoFrame, dest_addr, frameWidth, rangeWidthEnd, rangeHeightEnd, videoPixelMode, videoFrameCount);
                    packetsConsumed = mpegCodec.getPacketsConsumed();
                } else {
                    // Generate static.
                    generateFakeMPEGVideo(dest_addr, frameWidth);
                    if (isEnableConnector()) {
                        mpegCodec.postFakedVideo(dest_addr, frameWidth, videoPixelMode);
                    }
                    Date currentDate = convertTimestampToDate(mpegAvcAu.pts);
                    if(log.isDebugEnabled()) {
                        log.debug("currentDate: " + currentDate.toString());
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Debug.printFramebuffer(dest_addr, frameWidth, 10, 250, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 1, " This is a faked MPEG video (in YCbCr mode). ");
                    String displayedString;
                    if (mpegLastDate != null) {
                        displayedString = String.format(" %s / %s ", dateFormat.format(currentDate), dateFormat.format(mpegLastDate));
                        Debug.printFramebuffer(dest_addr, frameWidth, 10, 10, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 2, displayedString);
                    }
                    if (mpegStreamSize > 0) {
                        displayedString = String.format(" %d/%d (%.0f%%) ", processedSize, mpegStreamSize, processedSize * 100f / mpegStreamSize);
                    } else {
                        displayedString = String.format(" %d ", processedSize);
                    }
                    Debug.printFramebuffer(dest_addr, frameWidth, 10, 30, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 2, displayedString);
                    if (log.isDebugEnabled()) {
                        log.debug("sceMpegAvcCsc currentTimestamp=" + mpegAvcAu.pts);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("sceMpegAvcCsc currentTimestamp=" + mpegAvcAu.pts);
                }

                if (mpegRingbuffer.packetsFree < mpegRingbuffer.packets && packetsConsumed > 0) {
                    mpegRingbuffer.packetsFree = Math.min(mpegRingbuffer.packets, mpegRingbuffer.packetsFree + packetsConsumed);
                    mpegRingbuffer.write(mem, mpegRingbufferAddr);
                }
                delayThread(startTime, avcDecodeDelay);
            }
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegAvcCsc bad address " + String.format("0x%08X 0x%08X", source_addr, dest_addr));

            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x800C44DF, version = 150)
    public void sceMpegAtracDecode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int au_addr = cpu.gpr[5];
        int buffer_addr = cpu.gpr[6];
        int init = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAtracDecode(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", init=" + init + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAtracDecode bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (Memory.isAddressGood(au_addr) && Memory.isAddressGood(buffer_addr)) {
        	long startTime = Emulator.getClock().microTime();
            mpegAtracAu.pts += audioTimestampStep;

            // External audio setup.
            if (checkMediaEngineState()) {
            	Emulator.getClock().pause();
            	int bytes = 0;
            	if (me.stepAudio(MPEG_ATRAC_ES_OUTPUT_SIZE)) {
	                bytes = me.getCurrentAudioSamples(audioDecodeBuffer);
                    mem.copyToMemory(buffer_addr, ByteBuffer.wrap(audioDecodeBuffer, 0, bytes), bytes);
            	}
            	// Fill the rest of the buffer with 0's
            	mem.memset(buffer_addr + bytes, (byte) 0, MPEG_ATRAC_ES_OUTPUT_SIZE - bytes);
            	Emulator.getClock().resume();
            } else if (isEnableConnector() && mpegCodec.readAudioFrame(buffer_addr, audioFrameCount)) {
                mpegAtracAu.pts = mpegCodec.getMpegAtracCurrentTimestamp();
            } else {
                mem.memset(buffer_addr, (byte) 0, MPEG_ATRAC_ES_OUTPUT_SIZE);
            }
            audioFrameCount++;
            if (log.isDebugEnabled()) {
                log.debug("sceMpegAtracDecode currentTimestamp=" + mpegAtracAu.pts);
            }
            cpu.gpr[2] = 0;
            delayThread(startTime, atracDecodeDelay);
        } else {
            log.warn("sceMpegAtracDecode bad address " + String.format("0x%08X 0x%08X", au_addr, buffer_addr));
            cpu.gpr[2] = -1;
        }
    }

    protected int getPacketsFromSize(int size) {
    	int packets = size / (2048 + 104);

    	return packets;
    }

    private int getSizeFromPackets(int packets) {
        int size = (packets * 104) + (packets * 2048);

        return size;
    }

    @HLEFunction(nid = 0xD7A29F46, version = 150)
    public void sceMpegRingbufferQueryMemSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int packets = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferQueryMemSize packets=" + packets);
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = getSizeFromPackets(packets);
    }

    @HLEFunction(nid = 0x37295ED8, version = 150)
    public void sceMpegRingbufferConstruct(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int ringbuffer_addr = cpu.gpr[4];
        int packets = cpu.gpr[5];
        int data = cpu.gpr[6];
        int size = cpu.gpr[7];
        int callback_addr = cpu.gpr[8];
        int callback_args = cpu.gpr[9];

        if(log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferConstruct(ringbuffer=0x" + Integer.toHexString(ringbuffer_addr)
                    + ", packets=" + packets
                    + ", data=0x" + Integer.toHexString(data)
                    + ", size=" + size
                    + ", callback=0x" + Integer.toHexString(callback_addr)
                    + ", args=0x" + Integer.toHexString(callback_args) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (size < getSizeFromPackets(packets)) {
            log.warn("sceMpegRingbufferConstruct insufficient space: size=" + size + ", packets=" + packets);
            cpu.gpr[2] = SceKernelErrors.ERROR_MPEG_NO_MEMORY;
        } else if (Memory.isAddressGood(ringbuffer_addr)) {
            SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer(packets, data, size, callback_addr, callback_args);
            ringbuffer.write(mem, ringbuffer_addr);
            maxAheadTimestamp = getMaxAheadTimestamp(packets);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceMpegRingbufferConstruct bad address " + String.format("0x%08X", ringbuffer_addr));
            cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0x13407F13, version = 150)
    public void sceMpegRingbufferDestruct(Processor processor) {
        CpuState cpu = processor.cpu;

        int ringbuffer_addr = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferDestruct(ringbuffer=0x" + Integer.toHexString(ringbuffer_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public class AfterRingbufferPutCallback implements IAction {

        @Override
        public void execute() {
            hleMpegRingbufferPostPut();
        }
    }

    protected void hleMpegRingbufferPostPut() {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int packetsAdded = cpu.gpr[2];
        mpegRingbuffer.read(mem, mpegRingbufferAddr);

        if (packetsAdded > 0) {
            if (checkMediaEngineState() && (meChannel != null)) {
                meChannel.write(mpegRingbuffer.data, packetsAdded * mpegRingbuffer.packetSize);
            } else if (isEnableConnector()) {
                mpegCodec.writeVideo(mpegRingbuffer.data, packetsAdded * mpegRingbuffer.packetSize);
            }
            if (packetsAdded > mpegRingbuffer.packetsFree) {
                log.warn("sceMpegRingbufferPut clamping packetsAdded old=" + packetsAdded + " new=" + mpegRingbuffer.packetsFree);
                packetsAdded = mpegRingbuffer.packetsFree;
            }
            mpegRingbuffer.packetsRead += packetsAdded;
            mpegRingbuffer.packetsWritten += packetsAdded;
            mpegRingbuffer.packetsFree -= packetsAdded;
            mpegRingbuffer.write(mem, mpegRingbufferAddr);
        }

        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferPut packetsAdded=" + packetsAdded + ", packetsRead=" + mpegRingbuffer.packetsRead);
        }

        cpu.gpr[2] = packetsAdded;
    }

    @HLEFunction(nid = 0xB240A59E, version = 150)
    public void sceMpegRingbufferPut(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        mpegRingbufferAddr = cpu.gpr[4];
        int numPackets = cpu.gpr[5];
        int available = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegRingbufferPut ringbuffer=0x%08X, numPackets=%d, available=%d",
            		mpegRingbufferAddr, numPackets, available));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (numPackets < 0) {
            cpu.gpr[2] = 0;
        } else {
            int numberPackets = Math.min(available, numPackets);
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
            Modules.ThreadManForUserModule.executeCallback(null, mpegRingbuffer.callback_addr, afterRingbufferPutCallback, false, mpegRingbuffer.data, numberPackets, mpegRingbuffer.callback_args);
        }
    }

    @HLEFunction(nid = 0xB5F6DC87, version = 150)
    public void sceMpegRingbufferAvailableSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        mpegRingbufferAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferAvailableSize(ringbuffer=0x" + Integer.toHexString(mpegRingbufferAddr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
    	mpegRingbuffer.read(mem, mpegRingbufferAddr);
        cpu.gpr[2] = mpegRingbuffer.packetsFree;
        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferAvailableSize returning " + mpegRingbuffer.packetsFree);
        }
    }

    @HLEFunction(nid = 0x11CAB459, version = 150)
    public void sceMpeg_11CAB459(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceMpeg_11CAB459 [0x11CAB459]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x3C37A7A6, version = 150)
    public void sceMpegNextAvcRpAu(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceMpegNextAvcRpAu " + String.format("%08X %08X %08X %08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xB27711A8, version = 150)
    public void sceMpeg_B27711A8(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceMpeg_B27711A8 [0xB27711A8]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xD4DD6E75, version = 150)
    public void sceMpeg_D4DD6E75(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceMpeg_D4DD6E75 [0xD4DD6E75]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC345DED2, version = 150)
    public void sceMpeg_C345DED2(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceMpeg_C345DED2 [0xC345DED2]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xAB0E9556, version = 150)
    public void sceMpeg_AB0E9556(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceMpeg_AB0E9556 [0xAB0E9556]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xCF3547A2, version = 150)
    public void sceMpegAvcDecodeDetail2(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceMpegAvcDecodeDetail2 [0xCF3547A2]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x988E9E12, version = 150)
    public void sceMpeg_988E9E12(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceMpeg_988E9E12 [0x988E9E12]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}