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

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned32;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.io.IOException;
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
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.kernel.types.SceMpegRingbuffer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.IoFileMgrForUser.IIoListener;
import jpcsp.connector.MpegCodec;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.graphics.VideoEngine;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceMpeg extends HLEModule {
    protected static Logger log = Modules.getLogger("sceMpeg");

    private class EnableConnectorSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnableConnector(value);
		}
    }

    private class EnableMediaEngineSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnableMediaEngine(value);
		}
    }

    @Override
    public String getName() {
        return "sceMpeg";
    }

    @Override
    public void start() {
        setSettingsListener("emu.useConnector", new EnableConnectorSettingsListener());
        setSettingsListener("emu.useMediaEngine", new EnableMediaEngineSettingsListener());

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
        allocatedEsBuffers = new boolean[2];
        streamMap = new HashMap<Integer, StreamInfo>();

        super.start();
    }

    public static boolean useMpegCodec = false;
    public static boolean enableMediaEngine = false;

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
    protected boolean[] allocatedEsBuffers;
    protected HashMap<Integer, StreamInfo> streamMap;
    protected static final String streamPurpose = "sceMpeg-Stream";
    private MpegIoListener ioListener;
    private boolean insideRingbufferPut;

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
    		streamMap.remove(uid);
    		uid = -1;
    		type = -1;
    	}
    }

    private static class MpegIoListener implements IIoListener {
		@Override
		public void sceIoSync(int result, int device_addr, String device, int unknown) {
		}

		@Override
		public void sceIoPollAsync(int result, int uid, int res_addr) {
		}

		@Override
		public void sceIoWaitAsync(int result, int uid, int res_addr) {
		}

		@Override
		public void sceIoOpen(int result, int filename_addr, String filename, int flags, int permissions, String mode) {
		}

		@Override
		public void sceIoClose(int result, int uid) {
		}

		@Override
		public void sceIoWrite(int result, int uid, int data_addr, int size, int bytesWritten) {
		}

		@Override
		public void sceIoRead(int result, int uid, int data_addr, int size, int bytesRead, long position, SeekableDataInput dataInput, IVirtualFile vFile) {
			Modules.sceMpegModule.onIoRead(dataInput, vFile, bytesRead);
		}

		@Override
		public void sceIoCancel(int result, int uid) {
		}

		@Override
		public void sceIoSeek32(int result, int uid, int offset, int whence) {
		}

		@Override
		public void sceIoSeek64(long result, int uid, long offset, int whence) {
		}

		@Override
		public void sceIoMkdir(int result, int path_addr, String path, int permissions) {
		}

		@Override
		public void sceIoRmdir(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoChdir(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoDopen(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoDread(int result, int uid, int dirent_addr) {
		}

		@Override
		public void sceIoDclose(int result, int uid) {
		}

		@Override
		public void sceIoDevctl(int result, int device_addr, String device, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
		}

		@Override
		public void sceIoIoctl(int result, int uid, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
		}

		@Override
		public void sceIoAssign(int result, int dev1_addr, String dev1, int dev2_addr, String dev2, int dev3_addr, String dev3, int mode, int unk1, int unk2) {
		}

		@Override
		public void sceIoGetStat(int result, int path_addr, String path, int stat_addr) {
		}

		@Override
		public void sceIoRemove(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoChstat(int result, int path_addr, String path, int stat_addr, int bits) {
		}

		@Override
		public void sceIoRename(int result, int path_addr, String path, int new_path_addr, String newpath) {
		}
    }

    public static boolean isEnableConnector() {
        return useMpegCodec;
    }

    private static void setEnableConnector(boolean useConnector) {
        sceMpeg.useMpegCodec = useConnector;
        if (useConnector) {
            log.info("Using JPCSP connector");
        }
    }

    public static boolean checkMediaEngineState() {
        return enableMediaEngine;
    }

    private static void setEnableMediaEngine(boolean enableMediaEngine) {
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

    private void unregisterIoListener() {
    	if (ioListener != null) {
    		Modules.IoFileMgrForUserModule.unregisterIoListener(ioListener);
    		ioListener = null;
    	}
    }

    protected void onIoRead(SeekableDataInput dataInput, IVirtualFile vFile, int bytesRead) {
    	// if we are in the first sceMpegRingbufferPut and the MPEG header has not yet
    	// been analyzed, try to read the MPEG header.
		if (!isCurrentMpegAnalyzed() && insideRingbufferPut) {
			if (dataInput instanceof UmdIsoFile) {
		    	// Assume the MPEG header is located in the sector just before the
				// data currently read
				UmdIsoFile umdIsoFile = (UmdIsoFile) dataInput;
				int headerSectorNumber = umdIsoFile.getCurrentSectorNumber();
				headerSectorNumber -= bytesRead / UmdIsoFile.sectorLength;
				// One sector before the data current read
				headerSectorNumber--;

				UmdIsoReader umdIsoReader = Modules.IoFileMgrForUserModule.getIsoReader();
				if (umdIsoReader != null) {
					try {
						// Read the MPEG header sector and analyze it
						byte[] headerSector = umdIsoReader.readSector(headerSectorNumber);
						int tmpAddress = mpegRingbuffer.dataUpperBound - headerSector.length;
						IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(tmpAddress, headerSector.length, 1);
						for (int i = 0; i < headerSector.length; i++) {
							memoryWriter.writeNext(headerSector[i] & 0xFF);
						}
						memoryWriter.flush();
						Memory mem = Memory.getInstance();
						if (mem.read32(tmpAddress + PSMF_MAGIC_OFFSET) == PSMF_MAGIC) {
							analyseMpeg(tmpAddress);
						}

						// We do no longer need the IoListener...
						if (isCurrentMpegAnalyzed()) {
							unregisterIoListener();
						}
					} catch (IOException e) {
						if (log.isDebugEnabled()) {
							log.debug("onIoRead", e);
						}
					}
				}
			}
		}
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

    protected void analyseMpeg(int bufferAddr) {
        Memory mem = Memory.getInstance();

        mpegStreamAddr = bufferAddr;
        mpegMagic = mem.read32(bufferAddr + PSMF_MAGIC_OFFSET);
        mpegRawVersion = mem.read32(bufferAddr + PSMF_STREAM_VERSION_OFFSET);
        mpegVersion = getMpegVersion(mpegRawVersion);
        mpegOffset = endianSwap32(mem.read32(bufferAddr + PSMF_STREAM_OFFSET_OFFSET));
        mpegStreamSize = endianSwap32(mem.read32(bufferAddr + PSMF_STREAM_SIZE_OFFSET));
        mpegFirstTimestamp = endianSwap32(readUnaligned32(mem, bufferAddr + PSMF_FIRST_TIMESTAMP_OFFSET));
        mpegLastTimestamp = endianSwap32(readUnaligned32(mem, bufferAddr + PSMF_LAST_TIMESTAMP_OFFSET));
        mpegFirstDate = convertTimestampToDate(mpegFirstTimestamp);
        mpegLastDate = convertTimestampToDate(mpegLastTimestamp);
        avcDetailFrameWidth = (mem.read8(bufferAddr + 142) * 0x10);
        avcDetailFrameHeight = (mem.read8(bufferAddr + 143) * 0x10);
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
        if (!isCurrentMpegAnalyzed() && mpegStreamSize > 0 && mpegOffset > 0 && mpegOffset <= mpegStreamSize) {
            if (checkMediaEngineState()) {
            	me.init(bufferAddr, mpegStreamSize, mpegOffset);
            	meChannel = new PacketChannel();
                meChannel.write(bufferAddr, MPEG_HEADER_BUFFER_MINIMUM_SIZE);
            } else if (isEnableConnector()) {
                mpegCodec.init(mpegVersion, mpegStreamSize, mpegLastTimestamp);
                mpegCodec.writeVideo(bufferAddr, MPEG_HEADER_BUFFER_MINIMUM_SIZE);
            }

            // Mpeg header has already been initialized/processed
            setCurrentMpegAnalyzed(true);
        }

        if (log.isDebugEnabled()) {
	    	log.debug(String.format("Stream offset: %d, Stream size: 0x%X", mpegOffset, mpegStreamSize));
	    	log.debug(String.format("First timestamp: %d, Last timestamp: %d", mpegFirstTimestamp, mpegLastTimestamp));
	        if (log.isTraceEnabled()) {
	        	log.trace(String.format("%s", Utilities.getMemoryDump(bufferAddr, MPEG_HEADER_BUFFER_MINIMUM_SIZE, 4, 16)));
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
        unregisterIoListener();
        VideoEngine.getInstance().resetVideoTextures();
    }

    /**
     * sceMpegQueryStreamOffset
     * 
     * @param mpeg
     * @param buffer_addr
     * @param offset_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0x21FF80E4, version = 150, checkInsideInterrupt = true)
    public int sceMpegQueryStreamOffset(int mpeg, int buffer_addr, TPointer32 offset_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryStreamOffset(mpeg=0x" + Integer.toHexString(mpeg) + ", buffer=0x" + Integer.toHexString(buffer_addr) + ", offset=0x" + Integer.toHexString(offset_addr.getAddress()) + ")");
        }

        // Check handler.
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegQueryStreamOffset bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        // Check pointers.
        if (!Memory.isAddressGood(buffer_addr) || !offset_addr.isAddressGood()) {
            log.warn("sceMpegQueryStreamOffset bad address " + String.format("0x%08X 0x%08X", buffer_addr, offset_addr));
            return -1;
        }

        analyseMpeg(buffer_addr);

        // Check magic.
        if (mpegMagic != PSMF_MAGIC) {
            log.warn("sceMpegQueryStreamOffset bad magic " + String.format("0x%08X", mpegMagic));
            offset_addr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

        // Check version.
        if (mpegVersion < 0) {
            log.warn("sceMpegQueryStreamOffset bad version " + String.format("0x%08X", mpegRawVersion));
            offset_addr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_BAD_VERSION;
        }

        // Check offset.
        if ((mpegOffset & 2047) != 0 || mpegOffset == 0) {
            log.warn("sceMpegQueryStreamOffset bad offset " + String.format("0x%08X", mpegOffset));
            offset_addr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }
        
    	offset_addr.setValue(mpegOffset);
        return 0;
    }

    /**
     * sceMpegQueryStreamSize
     * 
     * @param bufferPointer
     * @param sizePointer
     * 
     * @return
     */
    @HLEFunction(nid = 0x611E9E11, version = 150, checkInsideInterrupt = true)
    public int sceMpegQueryStreamSize(TPointer bufferPointer, TPointer32 sizePointer) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryStreamSize(buffer=0x" + Integer.toHexString(bufferPointer.getAddress()) + ", size=0x" + Integer.toHexString(sizePointer.getAddress()) + ")");
        }

        // Check pointers.
        if (!bufferPointer.isAddressGood() || !sizePointer.isAddressGood()) {
            log.warn("sceMpegQueryStreamSize bad address " + String.format("0x%08X 0x%08X", bufferPointer, sizePointer));
            return -1;
        }

        analyseMpeg(bufferPointer.getAddress());
        
        // Check magic.
        if (mpegMagic != PSMF_MAGIC) {
            log.warn("sceMpegQueryStreamSize bad magic " + String.format("0x%08X", mpegMagic));
            return -1;
        }

        // Check alignment.
        if ((mpegStreamSize & 2047) != 0) {
        	sizePointer.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }
        
    	sizePointer.setValue(mpegStreamSize);
        return 0;
    }

    /**
     * sceMpegInit
     * 
     * @return
     */
    @HLEFunction(nid = 0x682A619B, version = 150, checkInsideInterrupt = true)
    public int sceMpegInit() {
        if (log.isInfoEnabled()) {
            log.info("sceMpegInit");
        }

        if (checkMediaEngineState()) {
            meChannel = null;
        }
        return 0;
    }

    /**
     * sceMpegFinish
     * 
     * @return
     */
    @HLEFunction(nid = 0x874624D6, version = 150, checkInsideInterrupt = true)
    public int sceMpegFinish() {
        if (log.isInfoEnabled()) {
            log.info("sceMpegFinish");
        }

        finishMpeg();

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
        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryMemSize(mode=" + mode + ")");
        }

        // Mode = 0 -> 64k (constant).
        return MPEG_MEMSIZE;
    }

    /**
     * sceMpegCreate
     * 
     * @param processor
     * @param mpeg
     * @param data
     * @param size
     * @param ringbuffer_addr
     * @param frameWidth
     * @param mode
     * @param ddrtop
     * 
     * @return
     */
    @HLEFunction(nid = 0xD8C5F121, version = 150, checkInsideInterrupt = true)
    public int sceMpegCreate(TPointer mpeg, TPointer data, int size, @CanBeNull TPointer ringbuffer_addr, int frameWidth, int mode, int ddrtop) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegCreate mpeg=%s, data=%s, size=%d, ringbuffer=%s, frameWidth=%d, mode=%d, ddrtop=0x%08X", mpeg, data, size, ringbuffer_addr, frameWidth, mode, ddrtop));
        }

        // Check size.
        if (size < MPEG_MEMSIZE) {
            log.warn("sceMpegCreate bad size " + size);
            return SceKernelErrors.ERROR_MPEG_NO_MEMORY;
        }
        
        // Update the ring buffer struct.
        if (!ringbuffer_addr.isNull()) {
        	mpegRingbuffer = SceMpegRingbuffer.fromMem(mem, ringbuffer_addr.getAddress());
	        if (mpegRingbuffer.packetSize == 0) {
	        	mpegRingbuffer.packetsFree = 0;
	        } else {
	        	mpegRingbuffer.packetsFree = (mpegRingbuffer.dataUpperBound - mpegRingbuffer.data) / mpegRingbuffer.packetSize;
	        }
	        mpegRingbuffer.mpeg = mpeg.getAddress();
	        mpegRingbuffer.write(mem, ringbuffer_addr.getAddress());
        }

        // Write mpeg system handle.
        mpegHandle = data.getAddress() + 0x30;
        mpeg.setValue32(mpegHandle);

        // Initialize fake mpeg struct.
        Utilities.writeStringZ(mem, mpegHandle, "LIBMPEG.001");
        mem.write32(mpegHandle + 12, -1);
        mem.write32(mpegHandle + 16, ringbuffer_addr.getAddress());
        if (mpegRingbuffer != null) {
        	mem.write32(mpegHandle + 20, mpegRingbuffer.dataUpperBound);
        }

        // Initialize mpeg values.
        mpegRingbufferAddr = ringbuffer_addr.getAddress();
        videoFrameCount = 0;
        audioFrameCount = 0;
        videoPixelMode = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
        defaultFrameWidth = frameWidth;
        insideRingbufferPut = false;

        return 0;
    }

    /**
     * sceMpegDelete
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0x606A4649, version = 150, checkInsideInterrupt = true)
    public int sceMpegDelete(int mpeg) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegDelete(mpeg=0x" + Integer.toHexString(mpeg) + ")");
        }

        finishMpeg();
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegDelete bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        return 0;
    }

    /**
     * sceMpegRegistStream
     * 
     * @param processor
     * @param mpeg
     * @param stream_type
     * @param stream_num
     * 
     * @return
     */
    @HLEFunction(nid = 0x42560F23, version = 150, checkInsideInterrupt = true)
    public int sceMpegRegistStream(Processor processor, int mpeg, int stream_type, int stream_num) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegRegistStream(mpeg=0x" + Integer.toHexString(mpeg) + ", stream_type=" + stream_type + ", stream_num=" + stream_num + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegRegistStream bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
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

        return uid;
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
    public int sceMpegUnRegistStream(int mpeg, int streamUid) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegUnRegistStream(mpeg=0x" + Integer.toHexString(mpeg) + ", stream=0x" + Integer.toHexString(streamUid) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegUnRegistStream bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }

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
            info.release();
    	} else {
            log.warn("sceMpegUnRegistStream unknown stream=0x" + Integer.toHexString(streamUid));
    	}
        setCurrentMpegAnalyzed(false);
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
    public int sceMpegMallocAvcEsBuf(int mpeg) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegMallocAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegMallocAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ") bad mpeg handle");
            return -1;
        }

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
    public int sceMpegFreeAvcEsBuf(int mpeg, int esBuf) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ", esBuf=0x" + Integer.toHexString(esBuf) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ", esBuf=0x" + Integer.toHexString(esBuf) + ") bad mpeg handle");
            return -1;
        }
        
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
    public int sceMpegQueryAtracEsSize(int mpeg, int esSize_addr, int outSize_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryAtracEsSize(mpeg=0x" + Integer.toHexString(mpeg) + ", esSize_addr=0x" + Integer.toHexString(esSize_addr) + ", outSize_addr=0x" + Integer.toHexString(outSize_addr) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegQueryAtracEsSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (Memory.isAddressGood(esSize_addr) && Memory.isAddressGood(outSize_addr)) {
            mem.write32(esSize_addr, MPEG_ATRAC_ES_SIZE);
            mem.write32(outSize_addr, MPEG_ATRAC_ES_OUTPUT_SIZE);
            return 0;
        }
        
        log.warn("sceMpegQueryAtracEsSize bad address " + String.format("0x%08X 0x%08X", esSize_addr, outSize_addr));
        return -1;
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
    public int sceMpegQueryPcmEsSize(int mpeg, int esSize_addr, int outSize_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegQueryPcmEsSize(mpeg=0x" + Integer.toHexString(mpeg) + ", esSize_addr=0x" + Integer.toHexString(esSize_addr) + ", outSize_addr=0x" + Integer.toHexString(outSize_addr) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegQueryPcmEsSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (Memory.isAddressGood(esSize_addr) && Memory.isAddressGood(outSize_addr)) {
            mem.write32(esSize_addr, MPEG_PCM_ES_SIZE);
            mem.write32(outSize_addr, MPEG_PCM_ES_OUTPUT_SIZE);
            return 0;
        }
        
        log.warn("sceMpegQueryPcmEsSize bad address " + String.format("0x%08X 0x%08X", esSize_addr, outSize_addr));
        return -1;
    }

    /**
     * sceMpegInitAu
     * 
     * @param mpeg
     * @param buffer_addr
     * @param auPointer
     * 
     * @return
     */
    @HLEFunction(nid = 0x167AFD9E, version = 150, checkInsideInterrupt = true)
    public int sceMpegInitAu(int mpeg, int buffer_addr, TPointer auPointer) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegInitAu(mpeg=0x" + Integer.toHexString(mpeg) + ", buffer=0x" + Integer.toHexString(buffer_addr) + ", au=0x" + Integer.toHexString(auPointer.getAddress()) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegInitAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        // Check if sceMpegInitAu is being called for AVC or ATRAC
        // and write the proper AU (access unit) struct.
        if (buffer_addr >= 1 && buffer_addr <= allocatedEsBuffers.length && allocatedEsBuffers[buffer_addr - 1]) {
        	mpegAvcAu.esBuffer = buffer_addr;
        	mpegAvcAu.esSize = MPEG_AVC_ES_SIZE;
        	mpegAvcAu.write(auPointer.getMemory(), auPointer.getAddress());
        } else {
        	mpegAtracAu.esBuffer = buffer_addr;
        	mpegAtracAu.esSize = MPEG_ATRAC_ES_SIZE;
        	mpegAtracAu.write(auPointer.getMemory(), auPointer.getAddress());
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
    @HLEFunction(nid = 0x234586AE, version = 150, checkInsideInterrupt = true)
    public int sceMpegChangeGetAvcAuMode(int mpeg, int stream_addr, int mode) {
        log.warn("UNIMPLEMENTED: sceMpegChangeGetAvcAuMode(mpeg=0x" + Integer.toHexString(mpeg) + ",stream_addr=0x" + Integer.toHexString(stream_addr) + ",mode=0x" + mode + ")");

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
        if (log.isDebugEnabled()) {
            log.debug("sceMpegChangeGetAuMode(mpeg=0x" + Integer.toHexString(mpeg) + ",stream_addr=0x" + Integer.toHexString(streamUid) + ",mode=0x" + mode + ")");
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
    public int sceMpegGetAvcAu(int mpeg, int streamUid, int au_addr, int attr_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegGetAvcAu(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream=0x" + Integer.toHexString(streamUid)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", attr_addr=0x" + Integer.toHexString(attr_addr) + ")");
        }

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegGetAvcAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            delayThread(mpegDecodeErrorDelay);
            return SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
        }
        
        // @NOTE: Shouldn't this be negated?
        if (Memory.isAddressGood(streamUid) && Memory.isAddressGood(au_addr)) {
            log.warn("sceMpegGetAvcAu didn't get a fake stream");
            return SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        }
        
        if (!streamMap.containsKey(streamUid)) {
            log.warn("sceMpegGetAvcAu bad address " + String.format("0x%08X 0x%08X", streamUid, au_addr));
            return -1;
        }

        if ((mpegAvcAu.pts > mpegAtracAu.pts + maxAheadTimestamp) && isAtracRegistered) {
            // Video is ahead of audio, deliver no video data to wait for audio.
            if (log.isDebugEnabled()) {
                log.debug("sceMpegGetAvcAu video ahead of audio: " + mpegAvcAu.pts + " - " + mpegAtracAu.pts);
            }
            delayThread(mpegDecodeErrorDelay);
            return SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
        }
        
        int result = 0;
        // Update the video timestamp (AVC).
        if (!ignoreAvc) {
        	// Read Au of next Avc frame
            if (checkMediaEngineState()) {
            	Emulator.getClock().pause();
                if (me.getContainer() == null) {
                    me.init(meChannel, true, true);
                }
            	if (!me.readVideoAu(mpegAvcAu)) {
            		// end of video reached only when last timestamp has been reached
            		if (mpegLastTimestamp <= 0 || mpegAvcAu.pts >= mpegLastTimestamp) {
            			endOfVideoReached = true;
            		}
            		// No more data in ringbuffer.
        			result = SceKernelErrors.ERROR_MPEG_NO_DATA;
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
        		log.debug(String.format("sceMpegGetAvcAu returning 0x%08X, AvcAu=%s", result, mpegAvcAu.toString()));
        	}
        }
        // Bitfield used to store data attributes.
        if (Memory.isAddressGood(attr_addr)) {
            mem.write32(attr_addr, 1);     // Unknown.
        }

        if (result != 0) {
        	delayThread(mpegDecodeErrorDelay);
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
    public int sceMpegGetPcmAu(int mpeg, int streamUid, int au_addr, int attr_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegGetPcmAu(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream=0x" + Integer.toHexString(streamUid)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", attr_addr=0x" + Integer.toHexString(attr_addr) + ")");
        }

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegGetPcmAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            delayThread(mpegDecodeErrorDelay);
            return SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
        }
        
        // Should be negated?
        if (Memory.isAddressGood(streamUid) && Memory.isAddressGood(au_addr)) {
            log.warn("sceMpegGetPcmAu didn't get a fake stream");
            return SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        } 
        
        if (!streamMap.containsKey(streamUid)) {
            log.warn("sceMpegGetPcmAu bad address " + String.format("0x%08X 0x%08X", streamUid, au_addr));
            return -1;
        }
        int result = 0;
        // Update the audio timestamp (Atrac).
        if (!ignorePcm) {
        	// Read Au of next Atrac frame
            if (checkMediaEngineState()) {
            	Emulator.getClock().pause();
            	if (me.getContainer() == null) {
            		me.init(meChannel, true, true);
            	}
            	if (!me.readAudioAu(mpegAtracAu)) {
            		result = SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
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
     * @param au_addr
     * @param attr_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xE1CE83A7, version = 150, checkInsideInterrupt = true)
    public int sceMpegGetAtracAu(int mpeg, int streamUid, int au_addr, int attr_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegGetAtracAu(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream=0x" + Integer.toHexString(streamUid)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", attr_addr=0x" + Integer.toHexString(attr_addr) + ")");
        }

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegGetAtracAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegGetAtracAu ringbuffer empty");
            delayThread(mpegDecodeErrorDelay);
            return SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
        }
        
        if (Memory.isAddressGood(streamUid) && Memory.isAddressGood(au_addr)) {
            log.warn("sceMpegGetAtracAu didn't get a fake stream");
            return SceKernelErrors.ERROR_MPEG_INVALID_ADDR;
        }
        
        if (!streamMap.containsKey(streamUid)) {
            log.warn("sceMpegGetAtracAu bad address " + String.format("0x%08X 0x%08X", streamUid, au_addr));
            return -1;
        }

    	if (endOfAudioReached && endOfVideoReached) {
    		if (log.isDebugEnabled()) {
    			log.debug("sceMpegGetAtracAu end of audio and video reached");
    		}

    		// Consume all the remaining packets, if any.
    		if (mpegRingbuffer.packetsFree < mpegRingbuffer.packets) {
    			mpegRingbuffer.packetsFree = mpegRingbuffer.packets;
    			mpegRingbuffer.write(mem, mpegRingbufferAddr);
    		}

    		return SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
    	}
    	
    	if ((mpegAtracAu.pts > mpegAvcAu.pts + maxAheadTimestamp) && isAvcRegistered && !endOfAudioReached) {
            // Audio is ahead of video, deliver no audio data to wait for video.
        	// This error is not returned when the end of audio has been reached (Patapon 3).
            if (log.isDebugEnabled()) {
                log.debug("sceMpegGetAtracAu audio ahead of video: " + mpegAtracAu.pts + " - " + mpegAvcAu.pts);
            }
            delayThread(mpegDecodeErrorDelay);
            return SceKernelErrors.ERROR_MPEG_NO_DATA; // No more data in ringbuffer.
        }
    	
        // Update the audio timestamp (Atrac).
        int result = 0;
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
        		log.debug(String.format("sceMpegGetAtracAu returning 0x%08X, AtracAu=%s", result, mpegAtracAu.toString()));
        	}
        }
        // Bitfield used to store data attributes.
        if (Memory.isAddressGood(attr_addr)) {
            mem.write32(attr_addr, 0);     // Pointer to ATRAC3plus stream (from PSMF file).
        }

        if (result != 0) {
        	delayThread(mpegDecodeErrorDelay);
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
        if (log.isDebugEnabled()) {
            log.debug("sceMpegFlushStream mpeg=0x" + Integer.toHexString(mpeg)
                    + ", stream_addr=0x" + Integer.toHexString(stream_addr));
        }

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
        if (log.isDebugEnabled()) {
            log.debug("sceMpegFlushAllStream mpeg=0x" + Integer.toHexString(mpeg));
        }

        // Finish the Mpeg only if we are not at the start of a new video,
        // otherwise the analyzed video could be lost.
        if (videoFrameCount > 0 || audioFrameCount > 0) {
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
    public int sceMpegAvcDecode(int mpeg, int au_addr, int frameWidth, int buffer_addr, int init_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecode(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", frameWidth=" + frameWidth
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", init=0x" + Integer.toHexString(init_addr) + ")");
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
            return -1;
        }
        
        if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcDecode ringbuffer not created");
            return -1;
        }
        
        if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegAvcDecode ringbuffer empty");
            return SceKernelErrors.ERROR_AVC_VIDEO_FATAL;
        }
        
        if (!Memory.isAddressGood(au_addr) || !Memory.isAddressGood(buffer_addr) || !Memory.isAddressGood(init_addr)) {
            log.warn(String.format("sceMpegAvcDecode bad address 0x%08X 0x%08X", au_addr, buffer_addr));
            return -1;
        }
    
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

        delayThread(startTime, avcDecodeDelay);
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
    public int sceMpegAvcDecodeDetail(int mpeg, TPointer detailPointer) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegAvcDecodeDetail(mpeg=0x%08X, detailAddr=0x%08X)", mpeg, detailPointer.getAddress()));
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeDetail bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (!detailPointer.isAddressGood()) {
            log.warn(String.format("sceMpegAvcDecodeDetail bad address 0x%08X", detailPointer.getAddress()));
            return -1;
        }
        
        detailPointer.setValue32( 0, avcDecodeResult     ); // Stores the result.
        detailPointer.setValue32( 4, videoFrameCount     ); // Last decoded frame.
        detailPointer.setValue32( 8, avcDetailFrameWidth ); // Frame width.
        detailPointer.setValue32(12, avcDetailFrameHeight); // Frame height.
        detailPointer.setValue32(16, 0                   ); // Frame crop rect (left).
        detailPointer.setValue32(20, 0                   ); // Frame crop rect (right).
        detailPointer.setValue32(24, 0                   ); // Frame crop rect (top).
        detailPointer.setValue32(28, 0                   ); // Frame crop rect (bottom).
        detailPointer.setValue32(32, avcFrameStatus      ); // Status of the last decoded frame.

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
    public int sceMpegAvcDecodeMode(int mpeg, int mode_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeMode(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", mode_addr=0x" + Integer.toHexString(mode_addr) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn(String.format("sceMpegAvcDecodeMode bad mpeg handle 0x%08X", mpeg));
            return -1;
        }
        
        if (!Memory.isAddressGood(mode_addr)) {
            log.warn(String.format("sceMpegAvcDecodeMode bad address 0x%08X", mode_addr));
            return -1;
        }

        // -1 is a default value.
        int mode = mem.read32(mode_addr);
        int pixelMode = mem.read32(mode_addr + 4);
        if (pixelMode >= TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650 && pixelMode <= TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
            videoPixelMode = pixelMode;
        } else {
            log.warn("sceMpegAvcDecodeMode mode=0x" + mode + " pixel mode=" + pixelMode + ": unknown mode");
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
    public int sceMpegAvcDecodeStop(int mpeg, int frameWidth, int buffer_addr, int status_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeStop(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", frameWidth=" + frameWidth
                    + ", buffer_addr=0x" + Integer.toHexString(buffer_addr)
                    + ", status_addr=0x" + Integer.toHexString(status_addr) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeStop bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (!Memory.isAddressGood(buffer_addr) || !Memory.isAddressGood(status_addr)) {
            log.warn("sceMpegAvcDecodeStop bad address " + String.format("0x%08X 0x%08X", buffer_addr, status_addr));
            return -1;
        }
        
        // No last frame generated
        mem.write32(status_addr, 0);
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
        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeFlush mpeg=0x" + Integer.toHexString(mpeg));
        }

        // Finish the Mpeg only if we are not at the start of a new video,
        // otherwise the analyzed video could be lost.
        if (videoFrameCount > 0 || audioFrameCount > 0) {
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
    public int sceMpegAvcQueryYCbCrSize(int mpeg, int mode, int width, int height, int resultAddr) {
        Memory mem = Memory.getInstance();

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcQueryYCbCrSize(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", mode=" + mode
                    + ", width=" + width
                    + ", height=" + height
                    + ", resultAddr=0x" + Integer.toHexString(resultAddr) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcQueryYCbCrSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if ((width & 15) != 0 || (height & 15) != 0 || width > 480 || height > 272) {
            log.warn("sceMpegAvcQueryYCbCrSize invalid size width=" + width + ", height=" + height);
        	return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }
        
        if (!Memory.isAddressGood(resultAddr)) {
            log.warn("sceMpegAvcQueryYCbCrSize bad result address 0x" + Integer.toHexString(resultAddr));
            return -1;
        }
        
    	// Write the size of the buffer used by sceMpegAvcDecodeYCbCr
		int size = (width / 2) * (height / 2) * 6 + 128;
        mem.write32(resultAddr, size);
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
    public int sceMpegAvcInitYCbCr(int mpeg, int mode, int width, int height, int ycbcr_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcInitYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", YCbCr_addr=0x" + Integer.toHexString(ycbcr_addr)
                    + ", mode=" + mode
                    + ", width=" + width
                    + ", height=" + height + ")");
        }

        encodedVideoFramesYCbCr.remove(ycbcr_addr);
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
    public int sceMpegAvcDecodeYCbCr(int mpeg, int au_addr, int buffer_addr, int init_addr) {
        Memory mem = Memory.getInstance();

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", init=0x" + Integer.toHexString(init_addr) + ")");
        }

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }
        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeYCbCr bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcDecodeYCbCr ringbuffer not created");
            return -1;
        }
        
        if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegAvcDecodeYCbCr ringbuffer empty");
            return SceKernelErrors.ERROR_AVC_VIDEO_FATAL;
        }
        
        if (!Memory.isAddressGood(au_addr) || !Memory.isAddressGood(buffer_addr) || !Memory.isAddressGood(init_addr)) {
            log.warn("sceMpegAvcDecodeYCbCr bad address " + String.format("0x%08X 0x%08X", au_addr, buffer_addr));

            return -1;
        }
        
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
        delayThread(startTime, avcDecodeDelay);

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
    public int sceMpegAvcDecodeStopYCbCr(int mpeg, int buffer_addr, int status_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcDecodeStopYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", status=0x" + Integer.toHexString(status_addr) + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAvcDecodeStopYCbCr bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (!Memory.isAddressGood(buffer_addr) || !Memory.isAddressGood(status_addr)) {
            log.warn("sceMpegAvcDecodeStopYCbCr bad address " + String.format("0x%08X 0x%08X", buffer_addr, status_addr));
            return -1;
        }

        // No last frame generated
        mem.write32(status_addr, 0);
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
    public int sceMpegAvcCsc(int mpeg, int source_addr, int range_addr, int frameWidth, int dest_addr) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAvcCsc(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", source=0x" + Integer.toHexString(source_addr)
                    + ", range_addr=0x" + Integer.toHexString(range_addr)
                    + ", frameWidth=" + frameWidth
                    + ", dest=0x" + Integer.toHexString(dest_addr) + ")");
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
            return -1;
        }
        
        if (mpegRingbuffer == null) {
            log.warn("sceMpegAvcCsc ringbuffer not created");
            return -1;
        }
        
        if (mpegRingbuffer.packetsRead == 0 || (!checkMediaEngineState() && mpegRingbuffer.isEmpty())) {
            log.debug("sceMpegAvcCsc ringbuffer empty");
            return SceKernelErrors.ERROR_AVC_VIDEO_FATAL;
        }
        
        if (!Memory.isAddressGood(dest_addr) || !Memory.isAddressGood(source_addr) || !Memory.isAddressGood(range_addr)) {
            log.warn("sceMpegAvcCsc bad address " + String.format("0x%08X 0x%08X", source_addr, dest_addr));
            return -1;
        }
        
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
    public int sceMpegAtracDecode(int mpeg, int au_addr, int buffer_addr, int init) {
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegAtracDecode(mpeg=0x" + Integer.toHexString(mpeg)
                    + ", au=0x" + Integer.toHexString(au_addr)
                    + ", buffer=0x" + Integer.toHexString(buffer_addr)
                    + ", init=" + init + ")");
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            log.warn("sceMpegAtracDecode bad mpeg handle 0x" + Integer.toHexString(mpeg));
            return -1;
        }
        
        if (!Memory.isAddressGood(au_addr) || !Memory.isAddressGood(buffer_addr)) {
            log.warn("sceMpegAtracDecode bad address " + String.format("0x%08X 0x%08X", au_addr, buffer_addr));
            return -1;
        }

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
        delayThread(startTime, atracDecodeDelay);
        return 0;
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
        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferQueryMemSize packets=" + packets);
        }

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
    public int sceMpegRingbufferConstruct(int ringbuffer_addr, int packets, int data, int size, int callback_addr, int callback_args) {
        Memory mem = Processor.memory;

        if(log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferConstruct(ringbuffer=0x" + Integer.toHexString(ringbuffer_addr)
                    + ", packets=" + packets
                    + ", data=0x" + Integer.toHexString(data)
                    + ", size=" + size
                    + ", callback=0x" + Integer.toHexString(callback_addr)
                    + ", args=0x" + Integer.toHexString(callback_args) + ")");
        }

        if (size < getSizeFromPackets(packets)) {
            log.warn("sceMpegRingbufferConstruct insufficient space: size=" + size + ", packets=" + packets);
            return SceKernelErrors.ERROR_MPEG_NO_MEMORY;
        }
        
        if (!Memory.isAddressGood(ringbuffer_addr)) {
            log.warn("sceMpegRingbufferConstruct bad address " + String.format("0x%08X", ringbuffer_addr));
            return -1;
        }

        SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer(packets, data, size, callback_addr, callback_args);
        ringbuffer.write(mem, ringbuffer_addr);
        maxAheadTimestamp = getMaxAheadTimestamp(packets);
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
    public int sceMpegRingbufferDestruct(int ringbuffer_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferDestruct(ringbuffer=0x" + Integer.toHexString(ringbuffer_addr) + ")");
        }

        return 0;
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
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("hleMpegRingbufferPostPut:%s", Utilities.getMemoryDump(mpegRingbuffer.data, packetsAdded * mpegRingbuffer.packetSize, 4, 16)));
        	}
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

        insideRingbufferPut = false;
        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferPut packetsAdded=" + packetsAdded + ", packetsRead=" + mpegRingbuffer.packetsRead);
        }

        cpu.gpr[2] = packetsAdded;
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
    public int sceMpegRingbufferPut(int _mpegRingbufferAddr, int numPackets, int available) {
        Memory mem = Processor.memory;
        
        this.mpegRingbufferAddr = _mpegRingbufferAddr;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceMpegRingbufferPut ringbuffer=0x%08X, numPackets=%d, available=%d",
            		mpegRingbufferAddr, numPackets, available));
        }

        if (numPackets < 0) {
            return 0;
        }

        int numberPackets = Math.min(available, numPackets);

        if (!isCurrentMpegAnalyzed()) {
        	// The MPEG header has not yet been analyzed, try to read it using an IoListener...
        	if (ioListener == null) {
        		ioListener = new MpegIoListener();
        		Modules.IoFileMgrForUserModule.registerIoListener(ioListener);
        	}
        }

        // Note: we can read more packets than available in the Mpeg stream: the application
        // can loop the video by putting previous packets back into the ringbuffer.

        mpegRingbuffer.read(mem, mpegRingbufferAddr);
        insideRingbufferPut = true;
        Modules.ThreadManForUserModule.executeCallback(null, mpegRingbuffer.callback_addr, afterRingbufferPutCallback, false, mpegRingbuffer.data, numberPackets, mpegRingbuffer.callback_args);

        return Emulator.getProcessor().cpu.gpr[2];
    }

    /**
     * sceMpegRingbufferAvailableSize
     * 
     * @param _mpegRingbufferAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0xB5F6DC87, version = 150, checkInsideInterrupt = true)
    public int sceMpegRingbufferAvailableSize(int _mpegRingbufferAddr) {
        Memory mem = Processor.memory;

        this.mpegRingbufferAddr = _mpegRingbufferAddr;

        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferAvailableSize(ringbuffer=0x" + Integer.toHexString(mpegRingbufferAddr) + ")");
        }

    	mpegRingbuffer.read(mem, mpegRingbufferAddr);
        if (log.isDebugEnabled()) {
            log.debug("sceMpegRingbufferAvailableSize returning " + mpegRingbuffer.packetsFree);
        }
        return mpegRingbuffer.packetsFree;
    }

    /**
     * sceMpegNextAvcRpAu
     * 
     * @param p1
     * @param p2
     * @param p3
     * @param p4
     * 
     * @return
     */
    @HLEFunction(nid = 0x3C37A7A6, version = 150, checkInsideInterrupt = true)
    public int sceMpegNextAvcRpAu(int p1, int p2, int p3, int p4) {
        log.warn(String.format("Unimplemented: sceMpegNextAvcRpAu %08X %08X %08X %08X", p1, p2, p3, p4));

        return 0;
    }

    @HLEFunction(nid = 0x01977054, version = 150)
    public int sceMpegGetUserdataAu() {
        log.warn("Unimplemented NID function sceMpegGetUserdataAu [0x01977054]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC45C99CC, version = 150)
    public int sceMpegQueryUserdataEsSize() {
        log.warn("Unimplemented NID function sceMpegQueryUserdataEsSize [0xC45C99CC]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x0558B075, version = 150)
    public int sceMpegAvcCopyYCbCr(int mpeg, int source_addr, int YCbCr_addr) {
        log.warn(String.format("Unimplemented sceMpegAvcCopyYCbCr mpeg=0x%08X, source=0x%08X, YCbCr_addr=0x%08X", mpeg, source_addr, YCbCr_addr));

        return 0;
    }

    @HLEFunction(nid = 0x11F95CF1, version = 150)
    public int sceMpegGetAvcNalAu() {
        log.warn("Unimplemented NID function sceMpegGetAvcNalAu [0x11F95CF1]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x921FCCCF, version = 150)
    public int sceMpegGetAvcEsAu() {
        log.warn("Unimplemented NID function sceMpegGetAvcEsAu [0x921FCCCF]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x6F314410, version = 150)
    public int sceMpegAvcDecodeGetDecodeSEI() {
        log.warn("Unimplemented NID function sceMpegAvcDecodeGetDecodeSEI [0x6F314410]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xAB0E9556, version = 150)
    public int sceMpegAvcDecodeDetailIndex() {
        log.warn("Unimplemented NID function sceMpegAvcDecodeDetailIndex [0xAB0E9556]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xCF3547A2, version = 150)
    public int sceMpegAvcDecodeDetail2() {
        log.warn("Unimplemented NID function sceMpegAvcDecodeDetail2 [0xCF3547A2]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xF5E7EA31, version = 150)
    public int sceMpegAvcConvertToYuv420(int mpeg, TPointer bufferOutput, TPointer unknown1, int unknown2) {
        log.warn(String.format("Unimplemented sceMpegAvcConvertToYuv420 mpeg=0x%X, bufferOutput=%s, unknown1=%s, unknown2=0x%08X", mpeg, bufferOutput, unknown1, unknown2));

        return 0;
    }

    @HLEFunction(nid = 0xD1CE4950, version = 150)
    public int sceMpegAvcCscMode() {
        log.warn("Unimplemented NID function sceMpegAvcCscMode [0xD1CE4950]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xDBB60658, version = 150)
    public int sceMpegFlushAu() {
        log.warn("Unimplemented NID function sceMpegFlushAu [0xDBB60658]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xE95838F6, version = 150)
    public int sceMpegAvcCscInfo() {
        log.warn("Unimplemented NID function sceMpegAvcCscInfo [0xE95838F6]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x11CAB459, version = 150)
    public int sceMpeg_11CAB459() {
        log.warn("Unimplemented NID function sceMpeg_11CAB459 [0x11CAB459]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB27711A8, version = 150)
    public int sceMpeg_B27711A8() {
        log.warn("Unimplemented NID function sceMpeg_B27711A8 [0xB27711A8]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xD4DD6E75, version = 150)
    public int sceMpeg_D4DD6E75() {
        log.warn("Unimplemented NID function sceMpeg_D4DD6E75 [0xD4DD6E75]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC345DED2, version = 150)
    public int sceMpeg_C345DED2() {
        log.warn("Unimplemented NID function sceMpeg_C345DED2 [0xC345DED2]");

        return 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x988E9E12, version = 150)
    public int sceMpeg_988E9E12() {
        log.warn("Unimplemented NID function sceMpeg_988E9E12 [0x988E9E12]");

        return 0xDEADC0DE;
    }
}