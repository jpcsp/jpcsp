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

import static jpcsp.HLE.pspdisplay.PSP_DISPLAY_PIXEL_FORMAT_565;
import static jpcsp.HLE.pspdisplay.PSP_DISPLAY_PIXEL_FORMAT_8888;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.pspdisplay;
import jpcsp.HLE.modules.HLECallback;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceMpegRingbuffer;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.connector.MpegCodec;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import jpcsp.Allegrex.CpuState;

// TODO:
// 1.- We need to find out if any of these functions are blocking/invoke a context switch.
// 2.- We must allocate a real buffer in sceMpegMallocAvcEsBuf.
// 3.- Implement real stream struct writing.

public class sceMpeg implements HLEModule {
    @Override
    public String getName() { return "sceMpeg"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(sceMpegQueryStreamOffsetFunction, 0x21FF80E4);
            mm.addFunction(sceMpegQueryStreamSizeFunction, 0x611E9E11);
            mm.addFunction(sceMpegInitFunction, 0x682A619B);
            mm.addFunction(sceMpegFinishFunction, 0x874624D6);
            mm.addFunction(sceMpegQueryMemSizeFunction, 0xC132E22F);
            mm.addFunction(sceMpegCreateFunction, 0xD8C5F121);
            mm.addFunction(sceMpegDeleteFunction, 0x606A4649);
            mm.addFunction(sceMpegRegistStreamFunction, 0x42560F23);
            mm.addFunction(sceMpegUnRegistStreamFunction, 0x591A4AA2);
            mm.addFunction(sceMpegMallocAvcEsBufFunction, 0xA780CF7E);
            mm.addFunction(sceMpegFreeAvcEsBufFunction, 0xCEB870B1);
            mm.addFunction(sceMpegQueryAtracEsSizeFunction, 0xF8DCB679);
            mm.addFunction(sceMpegQueryPcmEsSizeFunction, 0xC02CF6B5);
            mm.addFunction(sceMpegInitAuFunction, 0x167AFD9E);
            mm.addFunction(sceMpegChangeGetAvcAuModeFunction, 0x234586AE);
            mm.addFunction(sceMpegChangeGetAuModeFunction, 0x9DCFB7EA);
            mm.addFunction(sceMpegGetAvcAuFunction, 0xFE246728);
            mm.addFunction(sceMpegGetPcmAuFunction, 0x8C1E027D);
            mm.addFunction(sceMpegGetAtracAuFunction, 0xE1CE83A7);
            mm.addFunction(sceMpegFlushStreamFunction, 0x500F0429);
            mm.addFunction(sceMpegFlushAllStreamFunction, 0x707B7629);
            mm.addFunction(sceMpegAvcDecodeFunction, 0x0E3C2E9D);
            mm.addFunction(sceMpegAvcDecodeDetailFunction, 0x0F6C18D7);
            mm.addFunction(sceMpegAvcDecodeModeFunction, 0xA11C7026);
            mm.addFunction(sceMpegAvcDecodeStopFunction, 0x740FCCD1);
            mm.addFunction(sceMpegAvcDecodeFlushFunction, 0x4571CC64);
            mm.addFunction(sceMpegAvcQueryYCbCrSizeFunction, 0x211A057C);
            mm.addFunction(sceMpegAvcInitYCbCrFunction, 0x67179B1B);
            mm.addFunction(sceMpegAvcDecodeYCbCrFunction, 0xF0EB1125);
            mm.addFunction(sceMpegAvcDecodeStopYCbCrFunction, 0xF2930C9C);
            mm.addFunction(sceMpegAvcCscFunction, 0x31BD0272);
            mm.addFunction(sceMpegAtracDecodeFunction, 0x800C44DF);
            mm.addFunction(sceMpegRingbufferQueryMemSizeFunction, 0xD7A29F46);
            mm.addFunction(sceMpegRingbufferConstructFunction, 0x37295ED8);
            mm.addFunction(sceMpegRingbufferDestructFunction, 0x13407F13);
            mm.addFunction(sceMpegRingbufferPutFunction, 0xB240A59E);
            mm.addFunction(sceMpegRingbufferAvailableSizeFunction, 0xB5F6DC87);
            mm.addFunction(sceMpeg_11CAB459Function, 0x11CAB459);
            mm.addFunction(sceMpegNextAvcRpAuFunction, 0x3C37A7A6);
            mm.addFunction(sceMpeg_B27711A8Function, 0xB27711A8);
            mm.addFunction(sceMpeg_D4DD6E75Function, 0xD4DD6E75);
            mm.addFunction(sceMpeg_C345DED2Function, 0xC345DED2);
            mm.addFunction(sceMpeg_AB0E9556Function, 0xAB0E9556);
            mm.addFunction(sceMpegAvcDecodeDetail2Function, 0xCF3547A2);
            mm.addFunction(sceMpeg_988E9E12Function, 0x988E9E12);
        }

        mpegHandle = 0;
        mpegRingbuffer = null;
        mpegRingbufferAddr = 0;
        mpegAtracCurrentTimestamp = 0;
        mpegAvcCurrentTimestamp = 0;
        avcAuAddr = 0;
        atracAuAddr = 0;
        if (isEnableConnector()) {
        	mpegCodec = new MpegCodec();
        }
        if (isEnableMediaEngine()) {
        	me = new MediaEngine();
            meChannel = new PacketChannel();
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceMpegQueryStreamOffsetFunction);
            mm.removeFunction(sceMpegQueryStreamSizeFunction);
            mm.removeFunction(sceMpegInitFunction);
            mm.removeFunction(sceMpegFinishFunction);
            mm.removeFunction(sceMpegQueryMemSizeFunction);
            mm.removeFunction(sceMpegCreateFunction);
            mm.removeFunction(sceMpegDeleteFunction);
            mm.removeFunction(sceMpegRegistStreamFunction);
            mm.removeFunction(sceMpegUnRegistStreamFunction);
            mm.removeFunction(sceMpegMallocAvcEsBufFunction);
            mm.removeFunction(sceMpegFreeAvcEsBufFunction);
            mm.removeFunction(sceMpegQueryAtracEsSizeFunction);
            mm.removeFunction(sceMpegQueryPcmEsSizeFunction);
            mm.removeFunction(sceMpegInitAuFunction);
            mm.removeFunction(sceMpegChangeGetAvcAuModeFunction);
            mm.removeFunction(sceMpegChangeGetAuModeFunction);
            mm.removeFunction(sceMpegGetAvcAuFunction);
            mm.removeFunction(sceMpegGetPcmAuFunction);
            mm.removeFunction(sceMpegGetAtracAuFunction);
            mm.removeFunction(sceMpegFlushStreamFunction);
            mm.removeFunction(sceMpegFlushAllStreamFunction);
            mm.removeFunction(sceMpegAvcDecodeFunction);
            mm.removeFunction(sceMpegAvcDecodeDetailFunction);
            mm.removeFunction(sceMpegAvcDecodeModeFunction);
            mm.removeFunction(sceMpegAvcDecodeStopFunction);
            mm.removeFunction(sceMpegAvcDecodeFlushFunction);
            mm.removeFunction(sceMpegAvcQueryYCbCrSizeFunction);
            mm.removeFunction(sceMpegAvcInitYCbCrFunction);
            mm.removeFunction(sceMpegAvcDecodeYCbCrFunction);
            mm.removeFunction(sceMpegAvcDecodeStopYCbCrFunction);
            mm.removeFunction(sceMpegAvcCscFunction);
            mm.removeFunction(sceMpegAtracDecodeFunction);
            mm.removeFunction(sceMpegRingbufferQueryMemSizeFunction);
            mm.removeFunction(sceMpegRingbufferConstructFunction);
            mm.removeFunction(sceMpegRingbufferDestructFunction);
            mm.removeFunction(sceMpegRingbufferPutFunction);
            mm.removeFunction(sceMpegRingbufferAvailableSizeFunction);
            mm.removeFunction(sceMpeg_11CAB459Function);
            mm.removeFunction(sceMpegNextAvcRpAuFunction);
            mm.removeFunction(sceMpeg_B27711A8Function);
            mm.removeFunction(sceMpeg_D4DD6E75Function);
            mm.removeFunction(sceMpeg_C345DED2Function);
            mm.removeFunction(sceMpeg_AB0E9556Function);
            mm.removeFunction(sceMpegAvcDecodeDetail2Function);
            mm.removeFunction(sceMpeg_988E9E12Function);

        }
    }

    public static boolean useMpegCodec = false;
    public static boolean enableMediaEngine = false;

    public static final int PSMF_MAGIC = 0x464D5350;
    public static final int PSMF_VERSION_0012 = 0x32313030;
    public static final int PSMF_VERSION_0013 = 0x33313030;
    public static final int PSMF_VERSION_0014 = 0x34313030;
    public static final int PSMF_VERSION_0015 = 0x35313030;
    protected static final int MPEG_MEMSIZE = 0x10000; // 64k
    protected static final int MPEG_ESBUF_HANDLE = 1; // assume we only need 1 esBuf
    protected static final int atracDecodeDelay = 50;       // milliseconds
    protected static final int avcDecodeDelay   = 1000 / 60; // milliseconds, decoding video at maximum 60 FPS
    protected static final int maxAheadTimestamp = 100000;
    protected static final int mpegTimestampPerSecond = 90000; // how many MPEG Timestamp unit in a second

    // for now we just support 1 instance of mpeg
    protected int mpegHandle; // it needs to be an address so a game can read from it (although it's probably not supposed to)
    protected SceMpegRingbuffer mpegRingbuffer;
    protected int mpegRingbufferAddr;
    protected int mpegStreamSize;
    protected int mpegAtracCurrentTimestamp;
    protected long lastAtracSystemTime;
    protected int mpegAvcCurrentTimestamp;
    protected long lastAvcSystemTime;
    protected int avcAuAddr;
    protected int atracAuAddr;
    protected long mpegLastTimestamp;
    protected Date mpegLastDate;
    protected final int numberAvcEsBuffers = 2;
    protected boolean[] avcEsBufferAllocated = new boolean[numberAvcEsBuffers];
    protected int videoFrameCount;
    protected int audioFrameCount;
    protected int videoPixelMode;
    protected int avcDetailFrameWidth;
    protected int avcDetailFrameHeight;
    protected int defaultFrameWidth;

    public static final int MPEG_VERSION_0012 = 0;
    public static final int MPEG_VERSION_0013 = 1;
    public static final int MPEG_VERSION_0014 = 2;
    public static final int MPEG_VERSION_0015 = 3;
    protected int mpegVersion;
    protected int mpegRawVersion;
    protected int mpegMagic;
    protected int mpegOffset;
    protected int mpegStreamAddr;  // Save the stream's address.

    protected static final int MPEG_AVC_STREAM = 0;
    protected static final int MPEG_ATRAC_STREAM = 1;
    protected static final int MPEG_PCM_STREAM = 2;

    protected boolean isAtracRegistered = false;
    protected boolean isAvcRegistered = false;
    protected boolean isPcmRegistered = false;

    protected static final int MPEG_AVC_DECODE_SUCCESS = 1;
    protected static final int MPEG_AVC_DECODE_ERROR = -8;  //May be other values.
    protected int avcDecodeResult;

    protected MpegCodec mpegCodec;
    protected MediaEngine me;
    protected PacketChannel meChannel;

    public static boolean isEnableConnector() {
		return useMpegCodec;
	}

	public static void setEnableConnector(boolean useConnector) {
		sceMpeg.useMpegCodec = useConnector;
        if (useConnector) {
			Modules.log.info("Using JPCSP connector");
		}
	}

    public static boolean isEnableMediaEngine() {
        return enableMediaEngine;
	}

	public static void setEnableMediaEngine(boolean enableMediaEngine) {
		sceMpeg.enableMediaEngine = enableMediaEngine;
		if (enableMediaEngine) {
			Modules.log.info("Media Engine enabled");
		}
	}

    protected Date convertTimestampToDate(long timestamp) {
    	long millis = timestamp / (mpegTimestampPerSecond / 1000);
    	return new Date(millis);
    }

    protected int makeFakeStreamHandle(int stream) {
        return 0x34340000 | (stream & 0xFFFF);
    }

    protected boolean isFakeStreamHandle(int handle) {
        return ((handle & 0xFFFF0000) == 0x34340000);
    }

    protected int getFakeStreamID(int handle) {
        return (handle & 0x0000FFFF);
    }

    protected boolean isFakeAuHandle(int handle) {
        return ((handle & 0xFFFF0000) == 0x56560000);
    }

    protected int getFakeAuType(int handle) {
        return (handle & 0x0000FFFF);
    }

    protected int getMpegHandle(int mpegAddr) {
        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(mpegAddr)) {
            return mem.read32(mpegAddr);
        }

        return -1;
    }

    protected int endianSwap(int x) {
        return (x << 24) | ((x << 8) &  0xFF0000) | ((x >> 8) &  0xFF00) | ((x >> 24) &  0xFF);
    }

    protected void analyseMpeg(int buffer_addr) {
    	Memory mem = Memory.getInstance();

        mpegStreamAddr = buffer_addr;

        mpegMagic = mem.read32(buffer_addr);

        mpegRawVersion = mem.read32(buffer_addr + 4);
    	switch (mpegRawVersion) {
    		case PSMF_VERSION_0012: mpegVersion = MPEG_VERSION_0012; break;
    		case PSMF_VERSION_0013: mpegVersion = MPEG_VERSION_0013; break;
    		case PSMF_VERSION_0014: mpegVersion = MPEG_VERSION_0014; break;
    		case PSMF_VERSION_0015: mpegVersion = MPEG_VERSION_0015; break;
    		default:                mpegVersion = -1;                break;
    	}

        mpegOffset = endianSwap(mem.read32(buffer_addr + 8));

    	mpegStreamSize = endianSwap(mem.read32(buffer_addr + 12));
        mpegLastTimestamp = endianSwap(mem.read32(buffer_addr + 80 + 12));
        mpegLastDate = convertTimestampToDate(mpegLastTimestamp);

        if (mpegRingbuffer != null) {
        	mpegRingbuffer.reset();
        	mpegRingbuffer.write(mem, mpegRingbufferAddr);
        }
        mpegAtracCurrentTimestamp = 0;
        mpegAvcCurrentTimestamp = 0;
        videoFrameCount = 0;
        audioFrameCount = 0;

        if(mpegStreamSize > 0) {
            if(isEnableMediaEngine()) {
                meChannel.writePacket(buffer_addr, mpegOffset);
            } else if(isEnableConnector()) {
                mpegCodec.init(mpegVersion, mpegStreamSize, mpegLastTimestamp);
                mpegCodec.writeVideo(buffer_addr, mpegOffset);
            }
        }
    }

    private void generateFakeMPEGVideo(int dest_addr, int frameWidth) {
        Memory mem = Memory.getInstance();

        Random random = new Random();
        final int pixelSize = 3;
        final int bytesPerPixel = pspdisplay.getPixelFormatBytes(videoPixelMode);
        for (int y = 0; y < 272 - pixelSize + 1; y += pixelSize) {
            int address = dest_addr + y * frameWidth * bytesPerPixel;
            final int width = Math.min(480, frameWidth);
            for (int x = 0; x < width; x += pixelSize) {
                int n = random.nextInt(256);
                int color = 0xFF000000 | (n << 16) | (n << 8) | n;
                int pixelColor = Debug.getPixelColor(color, videoPixelMode);
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

    private void writeVideoImage(int dest_addr, int frameWidth) {
        final int bytesPerPixel = pspdisplay.getPixelFormatBytes(videoPixelMode);
        final int width = Math.min(480, frameWidth);

        // Get the current generated image, convert it to pixels and write it
        // to memory.
        if (me != null && me.getCurrentImg() != null) {
            for (int y = 0; y < 272; y++) {
                int address = dest_addr + y * frameWidth * bytesPerPixel;
                IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, bytesPerPixel);

                for (int x = 0; x < width; x++) {
                    int colorARGB = me.getCurrentImg().getRGB(x, y);
                    // Convert from ARGB to ABGR.
                    int a = (colorARGB >>> 24) & 0xFF;
                    int r = (colorARGB >>> 16) & 0xFF;
                    int g = (colorARGB >>> 8) & 0xFF;
                    int b = colorARGB & 0xFF;
                    int colorABGR = a << 24 | b << 16 | g << 8 | r;

                    int pixelColor = Debug.getPixelColor(colorABGR, videoPixelMode);
                    memoryWriter.writeNext(pixelColor);
                }
            }
        }
    }

    public void sceMpegQueryStreamOffset(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int buffer_addr = cpu.gpr[5];
        int offset_addr = cpu.gpr[6];

        Modules.log.debug("sceMpegQueryStreamOffset(mpeg=0x" + Integer.toHexString(mpeg)
            + ",buffer=0x" + Integer.toHexString(buffer_addr)
            + ",offset=0x" + Integer.toHexString(offset_addr) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegQueryStreamOffset bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(buffer_addr) && mem.isAddressGood(offset_addr)) {
        	analyseMpeg(buffer_addr);
            Modules.log.debug(String.format("sceMpegQueryStreamOffset magic=0x%08X"
                + " version=0x%08X offset=0x%08X size=0x%08X", mpegMagic, mpegVersion, mpegOffset, mpegStreamSize));

            if (mpegMagic == PSMF_MAGIC) {
            	if (mpegVersion < 0) {
                    Modules.log.warn("sceMpegQueryStreamOffset bad version " + String.format("0x%08X", mpegRawVersion));
        			mem.write32(offset_addr, 0);
                    cpu.gpr[2] = 0x80610002;
            	} else {
            		if ((mpegOffset & 2047) != 0 || mpegOffset == 0) {
                        Modules.log.warn("sceMpegQueryStreamOffset bad offset " + String.format("0x%08X", mpegOffset));
            			mem.write32(offset_addr, 0);
            			cpu.gpr[2] = 0x806101FE;
            		} else {
            			mem.write32(offset_addr, mpegOffset);
            			cpu.gpr[2] = 0;
            		}
            	}
            } else {
                Modules.log.warn("sceMpegQueryStreamOffset bad magic " + String.format("0x%08X", mpegMagic));
    			mem.write32(offset_addr, 0);
                cpu.gpr[2] = 0x806101FE;
            }
        } else {
            Modules.log.warn("sceMpegQueryStreamOffset bad address "
                + String.format("0x%08X 0x%08X", buffer_addr, offset_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegQueryStreamSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer_addr = cpu.gpr[4];
        int size_addr = cpu.gpr[5];

        Modules.log.debug("sceMpegQueryStreamSize(buffer=0x" + Integer.toHexString(buffer_addr)
            + ",size=0x" + Integer.toHexString(size_addr) + ")");

        if (mem.isAddressGood(buffer_addr) && mem.isAddressGood(size_addr)) {
            analyseMpeg(buffer_addr);
            Modules.log.debug(String.format("sceMpegQueryStreamSize magic=0x%08X"
                + " version=0x%08X offset=0x%08X size=0x%08X", mpegMagic, mpegVersion, mpegOffset, mpegStreamSize));

            Modules.log.info("sceMpegQueryStreamSize lastTimeStamp=" + mpegLastTimestamp);

            if (mpegMagic == PSMF_MAGIC) {
            	if ((mpegStreamSize & 2047) == 0) {
	                mem.write32(size_addr, mpegStreamSize);
	                cpu.gpr[2] = 0;
            	} else {
            		mem.write32(size_addr, 0);
                    cpu.gpr[2] = 0x806101FE;
            	}
            } else {
                Modules.log.warn("sceMpegQueryStreamSize bad magic " + String.format("0x%08X", mpegMagic));
                cpu.gpr[2] = -1;
            }
        } else {
            Modules.log.warn("sceMpegQueryStreamSize bad address "
                + String.format("0x%08X 0x%08X", buffer_addr, size_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegInit(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("PARTIAL:sceMpegInit");

        // we'll support only 1 mpeg instance at a time, we can fix this later if needed
        if (mpegHandle != 0) {
            Modules.log.warn("UNIMPLEMENTED:sceMpegInit multiple instances not yet supported");
            cpu.gpr[2] = -1;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceMpegFinish(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("PARTIAL:sceMpegFinish");

        if(isEnableMediaEngine()) {
            me.finish();
        }else if (isEnableConnector()) {
        	mpegCodec.finish();
        }

        mpegHandle = 0;
        cpu.gpr[2] = 0;
    }

    // user app will malloc this amount of memory
    public void sceMpegQueryMemSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];

        Modules.log.warn("PARTIAL:sceMpegQueryMemSize(mode=" + mode + ")");

        // only tested on mode=0 -> 64k
        cpu.gpr[2] = MPEG_MEMSIZE;
    }

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

        Modules.log.warn("PARTIAL:sceMpegCreate(mpeg=0x" + Integer.toHexString(mpeg)
            + ",data=0x" + Integer.toHexString(data)
            + ",size=" + size
            + ",ringbuffer=0x" + Integer.toHexString(ringbuffer_addr)
            + ",frameWidth=" + frameWidth
            + ",mode=" + mode
            + ",ddrtop=0x" + Integer.toHexString(ddrtop) + ")");

        if (size < MPEG_MEMSIZE) {
            Modules.log.warn("sceMpegCreate bad size " + size);
            cpu.gpr[2] = 0x80610022; // bad param/size (actual name unknown)
        } else if (mem.isAddressGood(mpeg) && mem.isAddressGood(data) && mem.isAddressGood(ringbuffer_addr)) {

            // update the ring buffer struct
            SceMpegRingbuffer ringbuffer = SceMpegRingbuffer.fromMem(mem, ringbuffer_addr);
            // packetsFree field doesn't seem to be used on psp, but we need to store this info somewhere so might as well use it
            if (ringbuffer.packetSize == 0) {
            	ringbuffer.packetsFree = 0;
            } else {
            	ringbuffer.packetsFree = (ringbuffer.dataUpperBound - ringbuffer.data) / ringbuffer.packetSize;
            }
            ringbuffer.mpeg = mpeg;
            ringbuffer.write(mem, ringbuffer_addr);

            // write mpeg "outdata"
            mpegHandle = data + 0x30;
            mem.write32(mpeg, mpegHandle);
            Modules.log.debug("sceMpegCreate generated handle " + Integer.toHexString(mpegHandle));

            // initialise mpeg struct
            Utilities.writeStringZ(mem, mpegHandle, "LIBMPEG.001");
            mem.write32(mpegHandle + 12, -1);
            mem.write32(mpegHandle + 16, ringbuffer_addr);
            mem.write32(mpegHandle + 20, ringbuffer.dataUpperBound);

            for (int i = 0; i < numberAvcEsBuffers; i++) {
            	avcEsBufferAllocated[i] = false;
            }

            mpegRingbufferAddr = ringbuffer_addr;
            mpegRingbuffer = ringbuffer;
            mpegAtracCurrentTimestamp = 0;
            mpegAvcCurrentTimestamp = 0;
            videoFrameCount = 0;
            audioFrameCount = 0;
            videoPixelMode = PSP_DISPLAY_PIXEL_FORMAT_8888;
            defaultFrameWidth = frameWidth;

            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegCreate bad address "
                + String.format("0x%08X 0x%08X 0x%08X", mpeg, data, ringbuffer_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        Modules.log.warn("PARTIAL:sceMpegDelete(mpeg=0x" + Integer.toHexString(mpeg) + ")");

        if(me != null) {
            me.finish();
            meChannel.flush();
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegDelete bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceMpegRegistStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_type = cpu.gpr[5];
        int unk = cpu.gpr[6];

        Modules.log.warn("PARTIAL:sceMpegRegistStream(mpeg=0x" + Integer.toHexString(mpeg)
            + ",stream=" + Integer.toHexString(stream_type)
            + ",unk=0x" + Integer.toHexString(unk) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegRegistStream bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {

            // Regist the respective stream.
            switch(stream_type) {
                case MPEG_AVC_STREAM:
                    isAvcRegistered = true;
                    break;
                case MPEG_ATRAC_STREAM:
                    isAtracRegistered = true;
                    break;
                case MPEG_PCM_STREAM:
                    isPcmRegistered = true;
                    break;

                default: Modules.log.warn("sceMpegRegistStream unknown stream type=" + stream_type);
                break;
            }

            // fake allocate a stream struct
            cpu.gpr[2] = makeFakeStreamHandle(stream_type);
            Modules.log.debug("sceMpegRegistStream ret=0x" + Integer.toHexString(cpu.gpr[2]));
        }
    }

    public void sceMpegUnRegistStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];

        Modules.log.warn("PARTIAL:sceMpegUnRegistStream(mpeg=0x" + Integer.toHexString(mpeg)
            + ",stream=0x" + Integer.toHexString(stream_addr) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegUnRegistStream bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (isFakeStreamHandle(stream_addr)) {
            Modules.log.debug("sceMpegUnRegistStream got fake stream ID " + getFakeStreamID(stream_addr));
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceMpegMallocAvcEsBuf(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegMallocAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ") bad mpeg handle");
            cpu.gpr[2] = -1;
        } else {
        	int allocatedBuffer = -1;
        	for (int i = 0; i < numberAvcEsBuffers; i++) {
        		if (!avcEsBufferAllocated[i]) {
        			allocatedBuffer = i;
        			avcEsBufferAllocated[i] = true;
        			break;
        		}
        	}

        	if (allocatedBuffer < 0) {
        		cpu.gpr[2] = 0;
        	} else {
        		cpu.gpr[2] = allocatedBuffer + 1;
        	}
            Modules.log.debug("sceMpegMallocAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg) + ") ret=0x" + Integer.toHexString(cpu.gpr[2]));
        }
    }

    public void sceMpegFreeAvcEsBuf(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int esBuf = cpu.gpr[5];

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg)
            + ",esBuf=0x" + Integer.toHexString(esBuf) + ") bad mpeg handle");
            cpu.gpr[2] = -1;
        } else if (esBuf <= 0 || esBuf > numberAvcEsBuffers) {
            Modules.log.warn("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg)
                    + ",esBuf=0x" + Integer.toHexString(esBuf) + ") bad esBuf handle");
        	cpu.gpr[2] = 0x806101FE;
        } else {
            Modules.log.debug("sceMpegFreeAvcEsBuf(mpeg=0x" + Integer.toHexString(mpeg)
                + ",buffer=0x" + Integer.toHexString(esBuf) + ")");
            avcEsBufferAllocated[esBuf - 1] = false;
            cpu.gpr[2] = 0;
        }
    }

    public void sceMpegQueryAtracEsSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int esSize_addr = cpu.gpr[5];
        int outSize_addr = cpu.gpr[6];

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegQueryAtracEsSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(esSize_addr) && mem.isAddressGood(outSize_addr)) {
            Modules.log.debug("sceMpegQueryAtracEsSize(mpeg=0x" + Integer.toHexString(mpeg)
                + ",esSize=0x" + Integer.toHexString(esSize_addr)
                + ",size=0x" + Integer.toHexString(outSize_addr) + ")");

            // copied from noxa/pspplayer and checked on psp: always constant
            mem.write32(esSize_addr, 2112);
            mem.write32(outSize_addr, 8192);

            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegQueryAtracEsSize bad address "
                + String.format("0x%08X 0x%08X", esSize_addr, outSize_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegQueryPcmEsSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int esSize_addr = cpu.gpr[5];
        int outSize_addr = cpu.gpr[6];

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegQueryPcmEsSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(esSize_addr) && mem.isAddressGood(outSize_addr)) {
            Modules.log.debug("sceMpegQueryPcmEsSize(mpeg=0x" + Integer.toHexString(mpeg)
                + ",esSize=0x" + Integer.toHexString(esSize_addr)
                + ",size=0x" + Integer.toHexString(outSize_addr) + ")");

            // always constant
            mem.write32(esSize_addr, 320);
            mem.write32(outSize_addr, 320);

            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegQueryPcmEsSize bad address "
                + String.format("0x%08X 0x%08X", esSize_addr, outSize_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegInitAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int buffer_addr = cpu.gpr[5];
        int au_addr = cpu.gpr[6];

        Modules.log.warn("PARTIAL:sceMpegInitAu(mpeg=0x" + Integer.toHexString(mpeg)
            + ",buffer=0x" + Integer.toHexString(buffer_addr)
            + ",au=0x" + Integer.toHexString(au_addr) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegInitAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {
            // TODO
            // buffer_addr is from sceMpegMallocAvcEsBuf or a user allocated buffer

            // seems a bit hackish, psp must be doing it the same way or using the order sceMpegInitAu is called in
            // For example 1st call is for stream 0 and 2nd call is for steam 1,
            // or are we missing a parameter?

            // When registering a stream, the second parameter actually reflects it's type.
            // So, I'm guessing that the unknown parameter may be the number of the stream.
            // Most games attempt to regist only one AVC and one ATRAC stream, but others
            // try to regist one AVC stream and two ATRAC streams (using the unk parameter as 0 or 1).

            // Timestamp (-1 == not init).
            mem.write32(au_addr, 0xFFFFFFFF);
            mem.write32(au_addr + 4, 0xFFFFFFFF);
            mem.write32(au_addr + 8, 0xFFFFFFFF);
            mem.write32(au_addr + 12, 0xFFFFFFFF);
            //Allocated buffer.
            mem.write32(au_addr + 16, buffer_addr);
            mem.write32(au_addr + 20, 0);  //Seems to be buffer_addr's size...

            cpu.gpr[2] = 0;
        }
    }

    public void sceMpegChangeGetAvcAuMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];
        int mode = cpu.gpr[6];

        Modules.log.warn("UNIMPLEMENTED: sceMpegChangeGetAvcAuMode(mpeg=0x" + Integer.toHexString(mpeg)
            + ",stream_addr=0x" + Integer.toHexString(stream_addr)
            + ",mode=0x" + mode + ")");

        cpu.gpr[2] = 0;
    }

    public void sceMpegChangeGetAuMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];
        int mode = cpu.gpr[6];

        Modules.log.warn("UNIMPLEMENTED: sceMpegChangeGetAuMode(mpeg=0x" + Integer.toHexString(mpeg)
            + ",stream_addr=0x" + Integer.toHexString(stream_addr)
            + ",mode=0x" + mode + ")");

        cpu.gpr[2] = 0;
    }

    public void sceMpegGetAvcAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];
        int au_addr = cpu.gpr[6];
        int result_addr = cpu.gpr[7];

        Modules.log.warn("PARTIAL:sceMpegGetAvcAu(mpeg=0x" + Integer.toHexString(mpeg)
            + ",stream=0x" + Integer.toHexString(stream_addr)
            + ",au=0x" + Integer.toHexString(au_addr)
            + ",result=0x" + Integer.toHexString(result_addr) + ")");

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegGetAvcAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            Modules.log.warn("sceMpegGetAvcAu ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (mpegRingbuffer.isEmpty())) {
            Modules.log.debug("sceMpegGetAvcAu ringbuffer empty");
            // TODO not sure about this check, either we check atracAuAddr or we check stream 1 is registered

            // Checking for the stream seems to be more correct, but we still must check if there can be more
            // then one (which is quite probable).
            if (isAtracRegistered) {
                cpu.gpr[2] = 0x806101fe; // no audio data in ring buffer and atrac au/stream 1 has been registered (actual name unknown)
            } else {
                cpu.gpr[2] = 0x80618001; // no audio data in ring buffer (actual name unknown)
            }
        } else if (mem.isAddressGood(stream_addr) && mem.isAddressGood(au_addr)) {
            // TODO
            Modules.log.warn("sceMpegGetAvcAu didn't get a fake stream");
            mem.write32(au_addr, 0x00000001);
            cpu.gpr[2] = 0;
        } else if (isFakeStreamHandle(stream_addr)) {
            Modules.log.debug("sceMpegGetAvcAu got fake stream ID " + getFakeStreamID(stream_addr));
            if (mpegAvcCurrentTimestamp > mpegAtracCurrentTimestamp + maxAheadTimestamp) {
            	// Video is ahead of audio, deliver no video data to wait for audio
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.debug("sceMpegGetAvcAu video ahead of audio: " + mpegAvcCurrentTimestamp + " - " + mpegAtracCurrentTimestamp);
            	}
                cpu.gpr[2] = 0x80618001; // no video data in ring buffer (actual name unknown)
                ThreadMan.getInstance().yieldCurrentThread();
            } else {
                // Update the timestamp.
                mem.write32(au_addr, 0x00000001); // Looks like it just can't be -1.
                mem.write32(au_addr + 4, mpegAvcCurrentTimestamp);
                // The timestamp seems to be replicated.
                mem.write32(au_addr + 8, 0x00000001);
                mem.write32(au_addr + 12, mpegAvcCurrentTimestamp);

                if(result_addr != 0)   //Can be null. In this case, just ignore the result.
                    mem.write32(result_addr, 1);

	            cpu.gpr[2] = 0;
            }
        } else {
            Modules.log.warn("sceMpegGetAvcAu bad address "
                + String.format("0x%08X 0x%08X", stream_addr, au_addr));
            cpu.gpr[2] = -1;
        }

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.info("sceMpegGetAvcAu ret:0x" + Integer.toHexString(cpu.gpr[2]));
        }
    }

    public void sceMpegGetPcmAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];
        int au_addr = cpu.gpr[6];
        int result_addr = cpu.gpr[7];

        Modules.log.warn("PARTIAL:sceMpegGetPcmAu(mpeg=0x" + Integer.toHexString(mpeg)
            + ",stream=0x" + Integer.toHexString(stream_addr)
            + ",au=0x" + Integer.toHexString(au_addr)
            + ",result=0x" + Integer.toHexString(result_addr) + ")");

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegGetPcmAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            Modules.log.warn("sceMpegGetPcmAu ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (mpegRingbuffer.isEmpty())) {
            Modules.log.debug("sceMpegGetPcmAu ringbuffer empty");
            cpu.gpr[2] = -1; // TODO
        } else if (mem.isAddressGood(stream_addr) && mem.isAddressGood(au_addr)) {
            // TODO
            Modules.log.warn("sceMpegGetPcmAu didn't get a fake stream");
            mem.write32(au_addr, 0x00000001);
            cpu.gpr[2] = 0;
        } else if (isFakeStreamHandle(stream_addr)) {
            Modules.log.debug("sceMpegGetPcmAu got fake stream ID " + getFakeStreamID(stream_addr));

            // Update the timestamp.
            mem.write32(au_addr, 0x00000001);
            // Any mpegPcmCurrentTimestamp?
            mem.write32(au_addr + 8, 0x00000001);

            if(result_addr != 0)
                mem.write32(result_addr, 1); // check

            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegGetPcmAu bad address "
                + String.format("0x%08X 0x%08X", stream_addr, au_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegGetAtracAu(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];
        int au_addr = cpu.gpr[6];
        int result_addr = cpu.gpr[7];

        Modules.log.warn("PARTIAL:sceMpegGetAtracAu(mpeg=0x" + Integer.toHexString(mpeg)
            + ",stream=0x" + Integer.toHexString(stream_addr)
            + ",au=0x" + Integer.toHexString(au_addr)
            + ",result=0x" + Integer.toHexString(result_addr) + ")");

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegGetAtracAu bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            Modules.log.warn("sceMpegGetAtracAu ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (mpegRingbuffer.isEmpty())) {
            Modules.log.debug("sceMpegGetAtracAu ringbuffer empty");
            cpu.gpr[2] = 0x80618001; // no audio data in ring buffer (actual name unknown)
        } else if (mem.isAddressGood(stream_addr) && mem.isAddressGood(au_addr)) {
            // TODO
            Modules.log.warn("sceMpegGetAtracAu didn't get a fake stream");
            mem.write32(au_addr, 0x00000001);
            cpu.gpr[2] = 0;
        } else if (isFakeStreamHandle(stream_addr)) {
            Modules.log.debug("sceMpegGetAtracAu got fake stream ID " + getFakeStreamID(stream_addr));
            if (mpegAtracCurrentTimestamp > mpegAvcCurrentTimestamp + maxAheadTimestamp) {
            	// Audio is ahead of video, deliver no audio data to wait for video
            	if (Modules.log.isDebugEnabled()) {
            		Modules.log.info("sceMpegGetAtracAu audio ahead of video: " + mpegAtracCurrentTimestamp + " - " + mpegAvcCurrentTimestamp);
            	}
                cpu.gpr[2] = 0x80618001; // no audio data in ring buffer (actual name unknown)
                ThreadMan.getInstance().yieldCurrentThread();
            } else {
                // Update the timestamp.
                mem.write32(au_addr, 0x00000001);
                mem.write32(au_addr + 4, mpegAtracCurrentTimestamp);
                mem.write32(au_addr + 8, 0x00000001);
                mem.write32(au_addr + 12, mpegAtracCurrentTimestamp);

                if(result_addr != 0)
                    mem.write32(result_addr, 1);
                cpu.gpr[2] = 0;
            }
        } else {
            Modules.log.warn("sceMpegGetAtracAu bad address "
                + String.format("0x%08X 0x%08X", stream_addr, au_addr));
            cpu.gpr[2] = -1;
        }

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.info("sceMpegGetAtracAu ret:0x" + Integer.toHexString(cpu.gpr[2]));
        }
    }

    public void sceMpegFlushStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int stream_addr = cpu.gpr[5];

        Modules.log.warn("IGNORING:sceMpegFlushStream mpeg=0x" + Integer.toHexString(mpeg)
                + "stream_addr=0x" + Integer.toHexString(stream_addr));

        cpu.gpr[2] = 0;
    }

    public void sceMpegFlushAllStream(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        Modules.log.warn("IGNORING:sceMpegFlushAllStream");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegFlushAllStream bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceMpegAvcDecode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int au_addr = cpu.gpr[5];
        int frameWidth = cpu.gpr[6];
        int buffer_addr = cpu.gpr[7];
        int init_addr = cpu.gpr[8];

        Modules.log.warn("PARTIAL:sceMpegAvcDecode(mpeg=0x" + Integer.toHexString(mpeg)
            + ",au=0x" + Integer.toHexString(au_addr)
            + ",frameWidth=" + frameWidth
            + ",buffer=0x" + Integer.toHexString(buffer_addr)
            + ",init=0x" + Integer.toHexString(init_addr) + ")");

        // When frameWidth is 0, take the frameWidth specified at sceMpegCreate
        if (frameWidth == 0) {
            frameWidth = defaultFrameWidth;
        }

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAvcDecode bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            Modules.log.warn("sceMpegAvcDecode ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (mpegRingbuffer.isEmpty())) {
            Modules.log.debug("sceMpegAvcDecode ringbuffer empty");
            cpu.gpr[2] = 0x80628002; // no video data in ring buffer (actual name unknown)
        } else if (mem.isAddressGood(au_addr) && mem.isAddressGood(buffer_addr) && mem.isAddressGood(init_addr)) {
            int au = mem.read32(au_addr);
            int buffer = mem.read32(buffer_addr);
            int init = mem.read32(init_addr);

            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(String.format("sceMpegAvcDecode *au=0x%08X, *buffer=0x%08X, init=%d", au, buffer, init));
            }

            long currentSystemTime = Emulator.getClock().milliTime();
            int elapsedTime = (int) (currentSystemTime - lastAvcSystemTime);
            if (elapsedTime >= 0 && elapsedTime <= avcDecodeDelay) {
                int delayMillis = avcDecodeDelay - elapsedTime;
                if (Modules.log.isDebugEnabled()) {
                    Modules.log.debug("Delaying sceMpegAvcDecode for " + delayMillis + "ms");
                }
                ThreadMan.getInstance().hleKernelDelayThread(delayMillis * 1000, false);
                lastAvcSystemTime = currentSystemTime + delayMillis;
            } else {
                lastAvcSystemTime = currentSystemTime;
            }

            mpegAvcCurrentTimestamp += (int)(90000 / 29.97); // value based on pmfplayer

            int packetsInRingbuffer = mpegRingbuffer.packets - mpegRingbuffer.packetsFree;
            int processedPackets = mpegRingbuffer.packetsRead - packetsInRingbuffer;
            int processedSize = processedPackets * mpegRingbuffer.packetSize;

            // let's go with 3 packets per frame for now
            int packetsConsumed = 3;
            if (mpegStreamSize > 0 && mpegLastTimestamp > 0) {
                // Try a better approximation of the packets consumed based on the timestamp
                int processedSizeBasedOnTimestamp = (int) ((((float) mpegAvcCurrentTimestamp) / mpegLastTimestamp) * mpegStreamSize);
                if (processedSizeBasedOnTimestamp < processedSize) {
                    packetsConsumed = 0;
                } else {
                    packetsConsumed = (processedSizeBasedOnTimestamp - processedSize) / mpegRingbuffer.packetSize;
                    if (packetsConsumed > 10) {
                        packetsConsumed = 10;
                    }
                }
                if (Modules.log.isDebugEnabled()) {
                    Modules.log.debug(String.format("sceMpegAvcDecode consumed %d %d/%d %d", processedSizeBasedOnTimestamp, processedSize, mpegStreamSize, packetsConsumed));
                }
            }

            final int width = Math.min(480, frameWidth);
            final int height = 272;

            if (isFakeAuHandle(au)) {
                int type = getFakeAuType(au);
                switch(type) {
                    case 1: Modules.log.debug("sceMpegAvcDecode got fake avc au"); break;
                    case 2: Modules.log.debug("sceMpegAvcDecode got fake pcm au"); break;
                    case 3: Modules.log.debug("sceMpegAvcDecode got fake atrac au"); break;
                }
            }

            if(isEnableMediaEngine()) {
                if(me.getContainer() != null) {
                    me.step();
                    writeVideoImage(buffer, frameWidth);
                } else {
                    me.init(meChannel.getFilePath());
                }
            } else if (isEnableConnector() && mpegCodec.readVideoFrame(buffer, frameWidth, width, height, videoFrameCount)) {
                packetsConsumed = mpegCodec.getPacketsConsumed();
                mpegAvcCurrentTimestamp = mpegCodec.getMpegAvcCurrentTimestamp();
            } else {
                // Generate static.
                generateFakeMPEGVideo(buffer, frameWidth);

                if (isEnableConnector())
                    mpegCodec.postFakedVideo(buffer, frameWidth, videoPixelMode);

                Date currentDate = convertTimestampToDate(mpegAvcCurrentTimestamp);
                Modules.log.info("currentDate: " + currentDate.toString());
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                Debug.printFramebuffer(buffer, frameWidth, 10, 250, 0xFFFFFFFF, 0xFF000000, videoPixelMode, 1, " This is a faked MPEG video. ");

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

                if (Modules.log.isDebugEnabled()) {
                    Modules.log.debug("sceMpegAvcDecode currentTimestamp=" + mpegAvcCurrentTimestamp);
                }
            }
            videoFrameCount++;
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug("sceMpegAvcDecode currentTimestamp=" + mpegAvcCurrentTimestamp);
            }

            avcDetailFrameWidth = width;
            avcDetailFrameHeight = height;
            if (mpegRingbuffer.packetsFree < mpegRingbuffer.packets) {
                mpegRingbuffer.packetsFree = Math.min(mpegRingbuffer.packets, mpegRingbuffer.packetsFree + packetsConsumed);
                mpegRingbuffer.write(mem, mpegRingbufferAddr);
            }

            avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;
            mem.write32(init_addr, avcDecodeResult);

            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn(String.format("sceMpegAvcDecode bad address 0x%08X 0x%08X", au_addr, buffer_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegAvcDecodeDetail(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int detailAddr = cpu.gpr[5];

        if (Modules.log.isInfoEnabled()) {
        	Modules.log.info(String.format("PARTIAL sceMpegAvcDecodeDetail(mpeg=0x%08X, detailAddr=0x%08X)", mpeg, detailAddr));
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAvcDecodeDetail bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (!mem.isAddressGood(detailAddr)) {
            Modules.log.warn(String.format("sceMpegAvcDecodeDetail bad address 0x%08X", detailAddr));
        	cpu.gpr[2] = -1;
        } else {
            mem.write32(detailAddr, avcDecodeResult); //Stores the result.
        	// Other detailAddr structure members are unknown...
        	mem.write32(detailAddr + 8, avcDetailFrameWidth);
        	mem.write32(detailAddr + 12, avcDetailFrameHeight);
            cpu.gpr[2] = 0;
        }
    }

    public void sceMpegAvcDecodeMode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int mode_addr = cpu.gpr[5];

        Modules.log.debug("sceMpegAvcDecodeMode(mpeg=0x" + Integer.toHexString(mpeg)
            + ",mode_addr=0x" + Integer.toHexString(mode_addr) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn(String.format("sceMpegAvcDecodeMode bad mpeg handle 0x%08X", mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(mode_addr)) {
            // -1 seems to represent a defualt value.
            // It is also valid for the pixel mode, but it should be ignored
            // (probably represents "do nothing").
            int mode = mem.read32(mode_addr); // castlevania x: -1
            int pixelMode = mem.read32(mode_addr + 4);
            if (pixelMode >= PSP_DISPLAY_PIXEL_FORMAT_565 && pixelMode <= PSP_DISPLAY_PIXEL_FORMAT_8888) {
            	videoPixelMode = pixelMode;
                Modules.log.debug("sceMpegAvcDecodeMode mode=0x" + mode + " pixel mode=" + pixelMode);
            } else if(pixelMode == -1){
                Modules.log.debug("sceMpegAvcDecodeMode mode=0x" + mode + " pixel mode=" + pixelMode + ": not changing pixel mode");
            } else {
                 Modules.log.warn("sceMpegAvcDecodeMode mode=0x" + mode + " pixel mode=" + pixelMode + ": unknown mode");
            }
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn(String.format("sceMpegAvcDecodeMode bad address 0x%08X", mode_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegAvcDecodeStop(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int frameWidth = cpu.gpr[5];
        int buffer_addr = cpu.gpr[6];
        int status_addr = cpu.gpr[7];

        Modules.log.warn("PARTIAL:sceMpegAvcDecodeStop(mpeg=0x" + Integer.toHexString(mpeg)
            + ",frameWidth=" + frameWidth
            + ",buffer=0x" + Integer.toHexString(buffer_addr)
            + ",status=0x" + Integer.toHexString(status_addr) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAvcDecodeStop bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(buffer_addr) && mem.isAddressGood(status_addr)) {
            // Possibly has to return the decode result.
            // Needs to be checked.
            mem.write32(status_addr, avcDecodeResult);
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegAvcDecodeStop bad address "
                + String.format("0x%08X 0x%08X", buffer_addr, status_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegAvcDecodeFlush(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];

        // For MediaEngine.
        if(me != null) {
            me.finish();
            meChannel.flush();
        }

        Modules.log.warn("IGNORING:sceMpegAvcDecodeFlush mpeg=0x" + Integer.toHexString(mpeg));

        cpu.gpr[2] = 0;
    }

    public void sceMpegAvcQueryYCbCrSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mpeg = cpu.gpr[4];
        int mode = cpu.gpr[5];  // 1 -> Seems to mean if the data is loaded from a .pmf file or if it's already in a buffer.
        int width = cpu.gpr[6];    // 480
        int height = cpu.gpr[7];   // 272
        int resultAddr = cpu.gpr[8]; // where to store the result

        Modules.log.warn("PARTIAL:sceMpegAvcQueryYCbCrSize(mpeg=0x" + Integer.toHexString(mpeg)
                + ",mode=" + mode
                + ",width=" + width
                + ",height=" + height
                + ",resultAddr=0x" + Integer.toHexString(resultAddr)
                + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAvcQueryYCbCrSize bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(resultAddr)) {
            mem.write32(resultAddr, 0); // TODO
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegAvcQueryYCbCrSize bad result address 0x" + Integer.toHexString(resultAddr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegAvcInitYCbCr(Processor processor) {
        CpuState cpu = processor.cpu;

        int mpeg = cpu.gpr[4];
        int mode = cpu.gpr[5];
        int width = cpu.gpr[6];
        int height = cpu.gpr[7];
        int ycbcr_addr = cpu.gpr[8];  // Possibly wants the allocated Es buffer.

         Modules.log.warn("IGNORING:sceMpegAvcInitYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
            + ",YCbCr_addr=0x" + Integer.toHexString(ycbcr_addr)
            + ",mode=" + mode
            + ",width=" + width
            + ",height=" + height
            + ")");

        cpu.gpr[2] = 0;
    }

    public void sceMpegAvcDecodeYCbCr(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mpeg = cpu.gpr[4];
        int au_addr = cpu.gpr[5];
        int buffer_addr = cpu.gpr[6];
        int init_addr = cpu.gpr[7];

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAvcDecodeYCbCr bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            Modules.log.warn("sceMpegAvcDecodeYCbCr ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (mpegRingbuffer.isEmpty())) {
            Modules.log.debug("sceMpegAvcDecodeYCbCr ringbuffer empty");
            cpu.gpr[2] = 0x80628002;
        } else if (mem.isAddressGood(au_addr) && mem.isAddressGood(buffer_addr) && mem.isAddressGood(init_addr)) {
            int au = mem.read32(au_addr);

            avcDecodeResult = MPEG_AVC_DECODE_SUCCESS;
            mem.write32(init_addr, avcDecodeResult);

            if (isFakeAuHandle(au)) {
                int type = getFakeAuType(au);
                switch(type) {
                    case 1: Modules.log.debug("sceMpegAvcDecodeYCbCr got fake avc au"); break;
                    case 2: Modules.log.debug("sceMpegAvcDecodeYCbCr got fake pcm au"); break;
                    case 3: Modules.log.debug("sceMpegAvcDecodeYCbCr got fake atrac au"); break;
                }
            }
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegAvcDecodeYCbCr bad address "
                + String.format("0x%08X 0x%08X", au_addr, buffer_addr));

            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegAvcDecodeStopYCbCr(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int buffer_addr = cpu.gpr[5];
        int status_addr = cpu.gpr[6];

        Modules.log.warn("IGNORING:sceMpegAvcDecodeStopYCbCr(mpeg=0x" + Integer.toHexString(mpeg)
            + ",buffer=0x" + Integer.toHexString(buffer_addr)
            + ",status=0x" + Integer.toHexString(status_addr) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAvcDecodeStopYCbCr bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(buffer_addr) && mem.isAddressGood(status_addr)) {
            mem.write32(status_addr, avcDecodeResult);
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegAvcDecodeStopYCbCr bad address "
                + String.format("0x%08X 0x%08X", buffer_addr, status_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegAvcCsc(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int source_addr = cpu.gpr[5]; //YCbCr data
        int range_addr = cpu.gpr[6];  //YCbCr range
        int frameWidth = cpu.gpr[7];
        int dest_addr = cpu.gpr[8]; //Converted data (RGB?)

        Modules.log.warn("PARTIAL:sceMpegAvcCsc(mpeg=0x" + Integer.toHexString(mpeg)
            + ",source=0x" + Integer.toHexString(source_addr)
            + ",range_addr=0x" + Integer.toHexString(range_addr)
            + ",frameWidth=" + frameWidth
            + ",dest=0x" + Integer.toHexString(dest_addr) + ")");

        // When frameWidth is 0, take the frameWidth specified at sceMpegCreate
        if (frameWidth == 0) {
            frameWidth = defaultFrameWidth;
        }

        if (mpegRingbuffer != null) {
            mpegRingbuffer.read(mem, mpegRingbufferAddr);
        }

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAvcCsc bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer == null) {
            Modules.log.warn("sceMpegAvcCsc ringbuffer not created");
            cpu.gpr[2] = -1;
        } else if (mpegRingbuffer.packetsRead == 0 || (mpegRingbuffer.isEmpty())) {
            Modules.log.debug("sceMpegAvcCsc ringbuffer empty");
            cpu.gpr[2] = 0x80628002;
        } else if (mem.isAddressGood(dest_addr) && mem.isAddressGood(source_addr) && mem.isAddressGood(range_addr)) {
            int rangeWidthStart = mem.read32(range_addr);
            int rangeHeightStart = mem.read32(range_addr + 4);
            int rangeWidthEnd = mem.read32(range_addr + 8);
            int rangeHeigtEnd = mem.read32(range_addr + 12);

            Modules.log.info("sceMpegAvcCsc range -"
                    + " x:"+ rangeWidthStart
                    + " y:" + rangeHeightStart
                    + " xLen:" + rangeWidthEnd
                    + " yLen:" + rangeHeigtEnd);

            long currentSystemTime = Emulator.getClock().milliTime();
            int elapsedTime = (int) (currentSystemTime - lastAvcSystemTime);
            if (elapsedTime >= 0 && elapsedTime <= avcDecodeDelay) {
                int delayMillis = avcDecodeDelay - elapsedTime;
                Modules.log.info("Delaying sceMpegAvcCsc for " + delayMillis + "ms");
                ThreadMan.getInstance().hleKernelDelayThread(delayMillis * 1000, false);
                lastAvcSystemTime = currentSystemTime + delayMillis;
            } else {
                lastAvcSystemTime = currentSystemTime;
            }

            mpegAvcCurrentTimestamp += (int)(90000 / 29.97);

            int packetsInRingbuffer = mpegRingbuffer.packets - mpegRingbuffer.packetsFree;
            int processedPackets = mpegRingbuffer.packetsRead - packetsInRingbuffer;
            int processedSize = processedPackets * mpegRingbuffer.packetSize;

            // let's go with 3 packets per frame for now
            int packetsConsumed = 3;
            if (mpegStreamSize > 0 && mpegLastTimestamp > 0) {
                // Try a better approximation of the packets consumed based on the timestamp
                int processedSizeBasedOnTimestamp = (int) ((((float) mpegAvcCurrentTimestamp) / mpegLastTimestamp) * mpegStreamSize);
                if (processedSizeBasedOnTimestamp < processedSize) {
                    packetsConsumed = 0;
                } else {
                    packetsConsumed = (processedSizeBasedOnTimestamp - processedSize) / mpegRingbuffer.packetSize;
                    if (packetsConsumed > 10) {
                        packetsConsumed = 10;
                    }
                }
                Modules.log.info(String.format("sceMpegAvcCsc consumed %d %d/%d %d", processedSizeBasedOnTimestamp, processedSize, mpegStreamSize, packetsConsumed));
            }

            if(isEnableMediaEngine()) {
                if(me.getContainer() != null) {
                    me.step();
                    writeVideoImage(dest_addr, frameWidth);
                } else {
                    me.init(meChannel.getFilePath());
                }
            } else if (isEnableConnector() && mpegCodec.readVideoFrame(source_addr, frameWidth, rangeWidthEnd, rangeHeigtEnd, videoFrameCount)) {
                packetsConsumed = mpegCodec.getPacketsConsumed();
                mpegAvcCurrentTimestamp = mpegCodec.getMpegAvcCurrentTimestamp();
            } else {
                // Generate static.
                generateFakeMPEGVideo(dest_addr, frameWidth);

                if (isEnableConnector())
                    mpegCodec.postFakedVideo(dest_addr, frameWidth, videoPixelMode);

                Date currentDate = convertTimestampToDate(mpegAvcCurrentTimestamp);
                Modules.log.info("currentDate: " + currentDate.toString());
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

                if (Modules.log.isDebugEnabled()) {
                    Modules.log.debug("sceMpegAvcCsc currentTimestamp=" + mpegAvcCurrentTimestamp);
                }
            }
            videoFrameCount++;

            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug("sceMpegAvcCsc currentTimestamp=" + mpegAvcCurrentTimestamp);
            }

            if (mpegRingbuffer.packetsFree < mpegRingbuffer.packets) {
                mpegRingbuffer.packetsFree = Math.min(mpegRingbuffer.packets, mpegRingbuffer.packetsFree + packetsConsumed);
                mpegRingbuffer.write(mem, mpegRingbufferAddr);
            }
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegAvcCsc bad address "
                + String.format("0x%08X 0x%08X", source_addr, dest_addr));

            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegAtracDecode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int mpeg = cpu.gpr[4];
        int au_addr = cpu.gpr[5];
        int buffer_addr = cpu.gpr[6];
        int init = cpu.gpr[7];

        Modules.log.warn("PARTIAL:sceMpegAtracDecode(mpeg=0x" + Integer.toHexString(mpeg)
            + ",au=0x" + Integer.toHexString(au_addr)
            + ",buffer=0x" + Integer.toHexString(buffer_addr)
            + ",init=0x" + Integer.toHexString(init) + ")");

        if (getMpegHandle(mpeg) != mpegHandle) {
            Modules.log.warn("sceMpegAtracDecode bad mpeg handle 0x" + Integer.toHexString(mpeg));
            cpu.gpr[2] = -1;
        } else if (mem.isAddressGood(au_addr) && mem.isAddressGood(buffer_addr)) {
            mpegAtracCurrentTimestamp += 4180;      // value based on pmfplayer

            long currentSystemTime = Emulator.getClock().milliTime();
            int elapsedTime = (int) (currentSystemTime - lastAtracSystemTime);
            if (elapsedTime >= 0 && elapsedTime <= atracDecodeDelay) {
                int delayMillis = atracDecodeDelay - elapsedTime;
                if (Modules.log.isDebugEnabled()) {
                    Modules.log.debug("Delaying sceMpegAtracDecode for " + delayMillis + "ms");
                }
                ThreadMan.getInstance().hleKernelDelayThread(delayMillis * 1000, false);
                lastAtracSystemTime = currentSystemTime + delayMillis;
            } else {
                lastAtracSystemTime = currentSystemTime;
            }

            if(isEnableMediaEngine()) {
                String pmfExtAudioPath = "tmp/" + jpcsp.State.discId + "/Mpeg-" + mpegStreamSize + "/ExtAudio.wav";
                File f = null;
                try {
                    f = new File(pmfExtAudioPath);
                    if(f.exists()) {
                        if(me.getExtContainer() != null) {
                            me.stepExtAudio();
                        } else {
                           me.initExtAudio(pmfExtAudioPath);
                        }
                    }
                } catch (Exception e) {
                    // Ignore.
                }
            } else if (isEnableConnector()) {
                if(mpegCodec.readAudioFrame(buffer_addr, audioFrameCount))
                    mpegAtracCurrentTimestamp = mpegCodec.getMpegAtracCurrentTimestamp();
            } else {
                mem.memset(buffer_addr, (byte) 0, 8192);
            }
            audioFrameCount++;
            if (Modules.log.isDebugEnabled()) {
            	Modules.log.debug("sceMpegAtracDecode currentTimestamp=" + mpegAtracCurrentTimestamp);
            }
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegAtracDecode bad address "
                + String.format("0x%08X 0x%08X", au_addr, buffer_addr));
            cpu.gpr[2] = -1;
        }

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceMpegAtracDecode ret:0x" + Integer.toHexString(cpu.gpr[2]));
        }
    }

    private int getSizeFromPackets(int packets) {
    	int size = (packets * 104) + (packets * 2048);

    	return size;
    }

    public void sceMpegRingbufferQueryMemSize(Processor processor) {
        CpuState cpu = processor.cpu;

        int packets = cpu.gpr[4];

        int size = getSizeFromPackets(packets);
        Modules.log.debug("sceMpegRingbufferQueryMemSize packets=" + packets + ", size=0x" + Integer.toHexString(size));

        cpu.gpr[2] = size;
    }

    public void sceMpegRingbufferConstruct(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int ringbuffer_addr = cpu.gpr[4];
        int packets = cpu.gpr[5];
        int data = cpu.gpr[6];
        int size = cpu.gpr[7]; // matching sceMpegQueryMemSize
        int callback_addr = cpu.gpr[8];
        int callback_args = cpu.gpr[9];

        Modules.log.debug("sceMpegRingbufferConstruct(ringbuffer=0x" + Integer.toHexString(ringbuffer_addr)
            + ",packets=" + packets
            + ",data=0x" + Integer.toHexString(data)
            + ",size=" + size
            + ",callback=0x" + Integer.toHexString(callback_addr)
            + ",args=0x" + Integer.toHexString(callback_args) + ")");

        if (size < getSizeFromPackets(packets)) {
            Modules.log.warn("sceMpegRingbufferConstruct insufficient space: size=" + size + ", packets=" + packets);
        	cpu.gpr[2] = 0x80610022;
        } else if (mem.isAddressGood(ringbuffer_addr)) {
            SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer(packets, data, size, callback_addr, callback_args);
            ringbuffer.write(mem, ringbuffer_addr);
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceMpegRingbufferConstruct bad address "
                + String.format("0x%08X", ringbuffer_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceMpegRingbufferDestruct(Processor processor) {
        CpuState cpu = processor.cpu;
        // nothing to do...
        cpu.gpr[2] = 0;
    }

    protected int ringbufferCallback_ringbuffer_addr;

    //protected boolean testChainedCallback = true; // testing
    protected boolean testChainedCallback = false;

    public void hleMpegRingbufferPostPut(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        SceMpegRingbuffer ringbuffer = SceMpegRingbuffer.fromMem(mem, ringbufferCallback_ringbuffer_addr);

        int packetsAdded = cpu.gpr[2];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceMpegRingbufferPut packetsAdded=" + packetsAdded + ", packetsRead=" + ringbuffer.packetsRead);
        }
        if (packetsAdded > 0)
        {
            if(isEnableMediaEngine()) {
                meChannel.writePacket(ringbuffer.data, packetsAdded * ringbuffer.packetSize);
            } else if (isEnableConnector()) {
        		mpegCodec.writeVideo(ringbuffer.data, packetsAdded * ringbuffer.packetSize);
        	}

        	if (ringbuffer.packetsFree - packetsAdded < 0) {
                Modules.log.warn("sceMpegRingbufferPut clamping packetsAdded old=" + packetsAdded
                    + " new=" + ringbuffer.packetsFree);

                packetsAdded = ringbuffer.packetsFree;
            }

            ringbuffer.packetsRead += packetsAdded;
            ringbuffer.packetsWritten += packetsAdded;
            ringbuffer.packetsFree -= packetsAdded;
            ringbuffer.write(mem, ringbufferCallback_ringbuffer_addr);
        }

        // return exactly what the callback returned, even if it would have caused an overflow
        cpu.gpr[2] = packetsAdded;

        // If we implement a real ring buffer we may need to call the mpeg callback twice in one call to sceMpegRingbufferPut
        // This is so we can fill the tail of the ringbuffer, then wrap round and fill the head
        // testing:
        if (testChainedCallback) {
            testChainedCallback = false;
            ThreadMan threadMan = ThreadMan.getInstance();

            int[] gpr = Arrays.copyOf(cpu.gpr, 32);
            gpr[4] = ringbuffer.data;
            gpr[5] = 32;
            gpr[6] = ringbuffer.callback_args;

            threadMan.executeCallback(
                ringbuffer.callback_addr,
                gpr,
                new HLECallback() {
					@Override
                    public void execute(Processor processor, SceKernelThreadInfo thread) {
                        sceMpeg.this.hleMpegRingbufferPostPut(processor);
                    }
                });
        }
    }

    public void sceMpegRingbufferPut(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int ringbuffer_addr = cpu.gpr[4];
        int numPackets = cpu.gpr[5];
        int available = cpu.gpr[6];

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug(String.format("sceMpegRingbufferPut(ringbuffer=0x%08X,numPackets=%d,available=%d", ringbuffer_addr, numPackets, available));
        }

        if (numPackets < 0) {
            cpu.gpr[2] = 0;
        } else {
            ThreadMan threadMan = ThreadMan.getInstance();
            SceMpegRingbuffer ringbuffer = SceMpegRingbuffer.fromMem(mem, ringbuffer_addr);

            ringbufferCallback_ringbuffer_addr = ringbuffer_addr;

            // HLE to PSP and back again magic bridge
            int[] gpr = Arrays.copyOf(cpu.gpr, 32);
            gpr[4] = ringbuffer.data; // we don't actually care about this data, so we always pass the same address instead of implementing a real ring buffer
            gpr[5] = Math.min(available, numPackets);
            gpr[6] = ringbuffer.callback_args;

            // This should get overwritten when our HLE callback is executed
            cpu.gpr[2] = 0xDEADC0DE;

            threadMan.executeCallback(
                    ringbuffer.callback_addr,
                    gpr,
                    new HLECallback() {
                    	@Override
                        public void execute(Processor processor, SceKernelThreadInfo thread) {
                            sceMpeg.this.hleMpegRingbufferPostPut(processor);
                        }
                    });

                // When using the compiler: the callback has been already executed
                // (and hleMpegRingbufferPostPut() as well) when we return here
        }
    }

    // return unit is packets, not bytes
    public void sceMpegRingbufferAvailableSize(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int ringbuffer_addr = cpu.gpr[4];

        SceMpegRingbuffer ringbuffer = SceMpegRingbuffer.fromMem(mem, ringbuffer_addr);

        Modules.log.debug("sceMpegRingbufferAvailableSize(ringbuffer=0x"
                + Integer.toHexString(ringbuffer_addr) + ") ret:" + ringbuffer.packetsFree);

        cpu.gpr[2] = ringbuffer.packetsFree;
    }

    public void sceMpeg_11CAB459(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceMpeg_11CAB459 [0x11CAB459]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMpegNextAvcRpAu(Processor processor) {
        CpuState cpu = processor.cpu;

        Modules.log.warn("UNIMPLEMENTED:sceMpegNextAvcRpAu "
            + String.format("%08X %08X %08X %08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7]));

        cpu.gpr[2] = 0;
    }

    public void sceMpeg_B27711A8(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceMpeg_B27711A8 [0xB27711A8]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMpeg_D4DD6E75(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceMpeg_D4DD6E75 [0xD4DD6E75]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMpeg_C345DED2(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceMpeg_C345DED2 [0xC345DED2]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMpeg_AB0E9556(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceMpeg_AB0E9556 [0xAB0E9556]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMpegAvcDecodeDetail2(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceMpegAvcDecodeDetail2 [0xCF3547A2]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceMpeg_988E9E12(Processor processor) {
        CpuState cpu = processor.cpu;

        System.out.println("Unimplemented NID function sceMpeg_988E9E12 [0x988E9E12]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public final HLEModuleFunction sceMpegQueryStreamOffsetFunction = new HLEModuleFunction("sceMpeg", "sceMpegQueryStreamOffset") {
        @Override
        public final void execute(Processor processor) {
            sceMpegQueryStreamOffset(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegQueryStreamOffset(processor);";
        }
    };

    public final HLEModuleFunction sceMpegQueryStreamSizeFunction = new HLEModuleFunction("sceMpeg", "sceMpegQueryStreamSize") {
        @Override
        public final void execute(Processor processor) {
            sceMpegQueryStreamSize(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegQueryStreamSize(processor);";
        }
    };

    public final HLEModuleFunction sceMpegInitFunction = new HLEModuleFunction("sceMpeg", "sceMpegInit") {
        @Override
        public final void execute(Processor processor) {
            sceMpegInit(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegInit(processor);";
        }
    };

    public final HLEModuleFunction sceMpegFinishFunction = new HLEModuleFunction("sceMpeg", "sceMpegFinish") {
        @Override
        public final void execute(Processor processor) {
            sceMpegFinish(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegFinish(processor);";
        }
    };

    public final HLEModuleFunction sceMpegQueryMemSizeFunction = new HLEModuleFunction("sceMpeg", "sceMpegQueryMemSize") {
        @Override
        public final void execute(Processor processor) {
            sceMpegQueryMemSize(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegQueryMemSize(processor);";
        }
    };

    public final HLEModuleFunction sceMpegCreateFunction = new HLEModuleFunction("sceMpeg", "sceMpegCreate") {
        @Override
        public final void execute(Processor processor) {
            sceMpegCreate(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegCreate(processor);";
        }
    };

    public final HLEModuleFunction sceMpegDeleteFunction = new HLEModuleFunction("sceMpeg", "sceMpegDelete") {
        @Override
        public final void execute(Processor processor) {
            sceMpegDelete(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegDelete(processor);";
        }
    };

    public final HLEModuleFunction sceMpegRegistStreamFunction = new HLEModuleFunction("sceMpeg", "sceMpegRegistStream") {
        @Override
        public final void execute(Processor processor) {
            sceMpegRegistStream(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegRegistStream(processor);";
        }
    };

    public final HLEModuleFunction sceMpegUnRegistStreamFunction = new HLEModuleFunction("sceMpeg", "sceMpegUnRegistStream") {
        @Override
        public final void execute(Processor processor) {
            sceMpegUnRegistStream(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegUnRegistStream(processor);";
        }
    };

    public final HLEModuleFunction sceMpegMallocAvcEsBufFunction = new HLEModuleFunction("sceMpeg", "sceMpegMallocAvcEsBuf") {
        @Override
        public final void execute(Processor processor) {
            sceMpegMallocAvcEsBuf(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegMallocAvcEsBuf(processor);";
        }
    };

    public final HLEModuleFunction sceMpegFreeAvcEsBufFunction = new HLEModuleFunction("sceMpeg", "sceMpegFreeAvcEsBuf") {
        @Override
        public final void execute(Processor processor) {
            sceMpegFreeAvcEsBuf(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegFreeAvcEsBuf(processor);";
        }
    };

    public final HLEModuleFunction sceMpegQueryAtracEsSizeFunction = new HLEModuleFunction("sceMpeg", "sceMpegQueryAtracEsSize") {
        @Override
        public final void execute(Processor processor) {
            sceMpegQueryAtracEsSize(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegQueryAtracEsSize(processor);";
        }
    };

    public final HLEModuleFunction sceMpegQueryPcmEsSizeFunction = new HLEModuleFunction("sceMpeg", "sceMpegQueryPcmEsSize") {
        @Override
        public final void execute(Processor processor) {
            sceMpegQueryPcmEsSize(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegQueryPcmEsSize(processor);";
        }
    };

    public final HLEModuleFunction sceMpegInitAuFunction = new HLEModuleFunction("sceMpeg", "sceMpegInitAu") {
        @Override
        public final void execute(Processor processor) {
            sceMpegInitAu(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegInitAu(processor);";
        }
    };

    public final HLEModuleFunction sceMpegChangeGetAvcAuModeFunction = new HLEModuleFunction("sceMpeg", "sceMpegChangeGetAvcAuMode") {
        @Override
        public final void execute(Processor processor) {
            sceMpegChangeGetAvcAuMode(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegChangeGetAvcAuMode(processor);";
        }
    };

    public final HLEModuleFunction sceMpegChangeGetAuModeFunction = new HLEModuleFunction("sceMpeg", "sceMpegChangeGetAuMode") {
        @Override
        public final void execute(Processor processor) {
            sceMpegChangeGetAuMode(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegChangeGetAuMode(processor);";
        }
    };

    public final HLEModuleFunction sceMpegGetAvcAuFunction = new HLEModuleFunction("sceMpeg", "sceMpegGetAvcAu") {
        @Override
        public final void execute(Processor processor) {
            sceMpegGetAvcAu(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegGetAvcAu(processor);";
        }
    };

    public final HLEModuleFunction sceMpegGetPcmAuFunction = new HLEModuleFunction("sceMpeg", "sceMpegGetPcmAu") {
        @Override
        public final void execute(Processor processor) {
            sceMpegGetPcmAu(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegGetPcmAu(processor);";
        }
    };

    public final HLEModuleFunction sceMpegGetAtracAuFunction = new HLEModuleFunction("sceMpeg", "sceMpegGetAtracAu") {
        @Override
        public final void execute(Processor processor) {
            sceMpegGetAtracAu(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegGetAtracAu(processor);";
        }
    };

    public final HLEModuleFunction sceMpegFlushStreamFunction = new HLEModuleFunction("sceMpeg", "sceMpegFlushStream") {
        @Override
        public final void execute(Processor processor) {
            sceMpegFlushStream(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegFlushStream(processor);";
        }
    };

    public final HLEModuleFunction sceMpegFlushAllStreamFunction = new HLEModuleFunction("sceMpeg", "sceMpegFlushAllStream") {
        @Override
        public final void execute(Processor processor) {
            sceMpegFlushAllStream(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegFlushAllStream(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecode") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecode(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecode(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeDetailFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecodeDetail") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecodeDetail(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecodeDetail(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeModeFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecodeMode") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecodeMode(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecodeMode(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeStopFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecodeStop") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecodeStop(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecodeStop(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeFlushFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecodeFlush") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecodeFlush(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecodeFlush(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcQueryYCbCrSizeFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcQueryYCbCrSize") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcQueryYCbCrSize(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcQueryYCbCrSize(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcInitYCbCrFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcInitYCbCr") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcInitYCbCr(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcInitYCbCr(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeYCbCrFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecodeYCbCr") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecodeYCbCr(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecodeYCbCr(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeStopYCbCrFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecodeStopYCbCr") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecodeStopYCbCr(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecodeStopYCbCr(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcCscFunction = new HLEModuleFunction("sceMpeg", "sceMpegAvcCsc") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcCsc(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcCsc(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAtracDecodeFunction = new HLEModuleFunction("sceMpeg", "sceMpegAtracDecode") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAtracDecode(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAtracDecode(processor);";
        }
    };

    public final HLEModuleFunction sceMpegRingbufferQueryMemSizeFunction = new HLEModuleFunction("sceMpeg", "sceMpegRingbufferQueryMemSize") {
        @Override
        public final void execute(Processor processor) {
            sceMpegRingbufferQueryMemSize(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegRingbufferQueryMemSize(processor);";
        }
    };

    public final HLEModuleFunction sceMpegRingbufferConstructFunction = new HLEModuleFunction("sceMpeg", "sceMpegRingbufferConstruct") {
        @Override
        public final void execute(Processor processor) {
            sceMpegRingbufferConstruct(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegRingbufferConstruct(processor);";
        }
    };

    public final HLEModuleFunction sceMpegRingbufferDestructFunction = new HLEModuleFunction("sceMpeg", "sceMpegRingbufferDestruct") {
        @Override
        public final void execute(Processor processor) {
            sceMpegRingbufferDestruct(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegRingbufferDestruct(processor);";
        }
    };

    public final HLEModuleFunction sceMpegRingbufferPutFunction = new HLEModuleFunction("sceMpeg", "sceMpegRingbufferPut") {
        @Override
        public final void execute(Processor processor) {
            sceMpegRingbufferPut(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegRingbufferPut(processor);";
        }
    };

    public final HLEModuleFunction sceMpegRingbufferAvailableSizeFunction = new HLEModuleFunction("sceMpeg", "sceMpegRingbufferAvailableSize") {
        @Override
        public final void execute(Processor processor) {
            sceMpegRingbufferAvailableSize(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegRingbufferAvailableSize(processor);";
        }
    };

    public final HLEModuleFunction sceMpeg_11CAB459Function = new HLEModuleFunction("sceMpeg", "sceMpeg_11CAB459") {
        @Override
        public final void execute(Processor processor) {
            sceMpeg_11CAB459(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpeg_11CAB459(processor);";
        }
    };

    public final HLEModuleFunction sceMpegNextAvcRpAuFunction = new HLEModuleFunction("sceMpeg", "sceMpegNextAvcRpAu") {
        @Override
        public final void execute(Processor processor) {
            sceMpegNextAvcRpAu(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegNextAvcRpAu(processor);";
        }
    };

    public final HLEModuleFunction sceMpeg_B27711A8Function = new HLEModuleFunction("sceMpeg", "sceMpeg_B27711A8") {
        @Override
        public final void execute(Processor processor) {
            sceMpeg_B27711A8(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpeg_B27711A8(processor);";
        }
    };

    public final HLEModuleFunction sceMpeg_D4DD6E75Function = new HLEModuleFunction("sceMpeg", "sceMpeg_D4DD6E75") {
        @Override
        public final void execute(Processor processor) {
            sceMpeg_D4DD6E75(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpeg_D4DD6E75(processor);";
        }
    };

    public final HLEModuleFunction sceMpeg_C345DED2Function = new HLEModuleFunction("sceMpeg", "sceMpeg_C345DED2") {
        @Override
        public final void execute(Processor processor) {
            sceMpeg_C345DED2(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpeg_C345DED2(processor);";
        }
    };

    public final HLEModuleFunction sceMpeg_AB0E9556Function = new HLEModuleFunction("sceMpeg", "sceMpeg_AB0E9556") {
        @Override
        public final void execute(Processor processor) {
            sceMpeg_AB0E9556(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpeg_AB0E9556(processor);";
        }
    };

    public final HLEModuleFunction sceMpegAvcDecodeDetail2Function = new HLEModuleFunction("sceMpeg", "sceMpegAvcDecodeDetail2") {
        @Override
        public final void execute(Processor processor) {
            sceMpegAvcDecodeDetail2(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpegAvcDecodeDetail2(processor);";
        }
    };

    public final HLEModuleFunction sceMpeg_988E9E12Function = new HLEModuleFunction("sceMpeg", "sceMpeg_988E9E12") {
        @Override
        public final void execute(Processor processor) {
            sceMpeg_988E9E12(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMpegModule.sceMpeg_988E9E12(processor);";
        }
    };
}