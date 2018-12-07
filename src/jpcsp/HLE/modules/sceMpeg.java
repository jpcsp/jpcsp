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
package jpcsp.HLE.modules;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MPEG_ALREADY_USED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MPEG_ILLEGAL_STREAM;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MPEG_NO_DATA;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MPEG_UNKNOWN_STREAM_ID;
import static jpcsp.HLE.modules.SysMemUserForUser.USER_PARTITION_ID;
import static jpcsp.HLE.modules.sceAudiocodec.PSP_CODEC_AT3PLUS;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceMp4AvcCscStruct;
import jpcsp.HLE.kernel.types.SceMpegAu;
import jpcsp.HLE.kernel.types.SceMpegRingbuffer;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.hardware.Screen;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.IBitReader;
import jpcsp.util.Utilities;

/**
 * HLE implementation of the module mpeg.prx
 *
 * @author gid15
 *
 */
public class sceMpeg extends HLEModule {
    public static Logger log = Modules.getLogger("sceMpeg");
    // MPEG statics.
    public static final int PSMF_MAGIC = 0x464D5350;
    public static final int PSMF_VERSION_0012 = 0x32313030;
    public static final int PSMF_VERSION_0013 = 0x33313030;
    public static final int PSMF_VERSION_0014 = 0x34313030;
    public static final int PSMF_VERSION_0015 = 0x35313030;
    public static final int MPEG_VERSION_0012 = 0;
    public static final int MPEG_VERSION_0013 = 1;
    public static final int MPEG_VERSION_0014 = 2;
    public static final int MPEG_VERSION_0015 = 3;
    public static final int PSMF_MAGIC_OFFSET = 0x0;
    public static final int PSMF_STREAM_VERSION_OFFSET = 0x4;
    public static final int PSMF_STREAM_OFFSET_OFFSET = 0x8;
    public static final int PSMF_STREAM_SIZE_OFFSET = 0xC;
    public static final int PSMF_FIRST_TIMESTAMP_OFFSET = 0x54;
    public static final int PSMF_LAST_TIMESTAMP_OFFSET = 0x5A;
    public static final int PSMF_NUMBER_STREAMS_OFFSET = 0x80;
    public static final int PSMF_FRAME_WIDTH_OFFSET = 0x8E;
    public static final int PSMF_FRAME_HEIGHT_OFFSET = 0x8F;
    public static final int MPEG_AVC_ES_SIZE = 2048;        // MPEG packet size.
    public static final int MPEG_MEMSIZE = 0x10000;         // 64k.
    public static final int MPEG_MEMSIZE_260 = 0xB3DB;      // PSP v2.60 is using a using a smaller memory size
    public static final long UNKNOWN_TIMESTAMP = -1;
    public static final int mpegTimestampPerSecond = 90000; // How many MPEG Timestamp units in a second.
    public static final int PSMF_AVC_STREAM = 0;
    public static final int PSMF_ATRAC_STREAM = 1;
    public static final int PSMF_PCM_STREAM = 2;
    public static final int PSMF_DATA_STREAM = 3;
    public static final int PSMF_AUDIO_STREAM = 15;
    public static final int PSMF_VIDEO_STREAM_ID = 0xE0;
    public static final int PSMF_AUDIO_STREAM_ID = 0xBD;
    private static final int MAX_STREAMS = 112;
    // The YCbCr buffer is starting with 128 bytes of unknown data
    private static final int YCBCR_DATA_OFFSET = 128;
    // MPEG ATRAC elementary stream.
    private static final int MPEG_ATRAC_ES_SIZE = 2112;
    private static final int MPEG_ATRAC_ES_OUTPUT_SIZE = 8192;
    // MPEG PCM elementary stream.
    private static final int MPEG_PCM_ES_SIZE = 320;
    private static final int MPEG_PCM_ES_OUTPUT_SIZE = 320;
    // MPEG Userdata elementary stream.
    private static final int MPEG_DATA_ES_SIZE = 0xA0000;
    private static final int MPEG_DATA_ES_OUTPUT_SIZE = 0xA0000;
    private boolean hleInitialized = false;
    private TPointer initVideocodecBuffer;
    private TPointer initVideocodecBuffer2;
    private TPointer initAudiocodecBuffer;
    private TPointer emptyBuffer;
    private TPointer sceMpegAvcResource;
    private TPointer sceMpegAvcCscBuffer1;
    private TPointer32 sceMpegAvcCscBuffer2;
    private TPointer sceMpegAvcDecodeYCbCrBuffer1;
    private TPointer sceMpegAvcDecodeYCbCrBuffer2;
    private TPointer sceMpegAvcConvertToYuv420Buffer;
    private int globalFlags;
    private static final StreamTypeDescriptor[] streamTypeDescriptors = new StreamTypeDescriptor[19];
    private static final StreamChannelDescriptor streamChannelDescriptors[] = new StreamChannelDescriptor[6];
	private static final int streamDataOffsets[] = { 0, 4, 4, 2, 0, 2, 0 };
	private static final int esSizemaxLengths[] = { 0x18400, 0x800, 0x140, 0xA0000, 0, 0x19100, 0x800 };
	private static final int sceMpegAvcCscOffsets[] = { 0, 0, 0, 0 };

    private static class StreamTypeDescriptor {
    	private final int code;
    	private final int mask;
    	private final int codeMask;
    	private final int shift;

    	public StreamTypeDescriptor(int code, int mask, int codeMask) {
    		this.code = code;
    		this.mask = mask;
    		this.codeMask = codeMask;

    		switch (mask) {
				case 0xFF000000: shift = 24; break;
				case 0xFFFF0000: shift = 16; break;
				case 0xFFFFFF00: shift =  8; break;
				case 0xFFFFFFFF: shift =  0; break;
				default:         shift =  0; break;
    		}
    	}

    	public int getMask() {
    		return mask;
    	}

    	public int getChannelCode(int channelNumber) {
    		return code | (channelNumber << shift);
    	}

    	public boolean isMatching(int channelCode) {
    		return (channelCode & codeMask) == code;
    	}
    }

    private static class StreamChannelDescriptor {
    	private final int code;
    	private final int mask;
    	private final int channelShift;
    	private final int channelMask;
    	private final int streamBase;
    	private final int streamDataBase;

    	public StreamChannelDescriptor(int code, int mask, int channelShift, int channelMask, int streamIndex) {
			this.code = code;
			this.mask = mask;
			this.channelShift = channelShift;
			this.channelMask = channelMask;
			this.streamBase = streamIndex * 68;
			this.streamDataBase = streamIndex * 16;
		}

    	public boolean isMatching(int streamChannelCode) {
    		return (streamChannelCode & mask) == code;
    	}

    	private TPointer getAddr(TPointer ptr, int streamChannelCode, int size, int base) {
    		int channel = (streamChannelCode >> channelShift) & channelMask;
    		ptr.add(channel * size + base);

    		return ptr;
    	}

    	public TPointer getStreamAddr(TPointer data, int streamChannelCode) {
    		return getAddr(data.getPointer(28), streamChannelCode, 68, streamBase);
    	}

    	public TPointer getDataStreamAddr(TPointer data, int streamChannelCode) {
    		return getAddr(data.getPointer(24), streamChannelCode, 16, streamDataBase);
    	}
    }

    static {
    	streamTypeDescriptors[ 0] = new StreamTypeDescriptor(0xE0000000, 0xFF000000, 0xE0000000); // Video stream
    	streamTypeDescriptors[ 1] = new StreamTypeDescriptor(0xBD000000, 0xFFFF0000, 0xFFF00000); // Atrac stream
    	streamTypeDescriptors[ 2] = new StreamTypeDescriptor(0xBD100000, 0xFFFF0000, 0xFFF00000); // PCM stream
    	streamTypeDescriptors[ 3] = new StreamTypeDescriptor(0xBD200000, 0xFFFF0000, 0xFFF00000); // Data stream
    	streamTypeDescriptors[ 4] = new StreamTypeDescriptor(0xC0000000, 0xFF000000, 0xC0000000);
    	streamTypeDescriptors[ 5] = new StreamTypeDescriptor(0xBD800000, 0xFFFF0000, 0xFFE00000);
    	streamTypeDescriptors[ 6] = new StreamTypeDescriptor(0xBF01E000, 0xFFFFFF00, 0xFFFFE000);
    	streamTypeDescriptors[ 7] = new StreamTypeDescriptor(0xBD300000, 0xFFFF0000, 0xFFF00000);
    	streamTypeDescriptors[ 8] = new StreamTypeDescriptor(0xBD400000, 0xFFFF0000, 0xFFF00000);
    	streamTypeDescriptors[ 9] = new StreamTypeDescriptor(0xE0000000, 0xFF000000, 0xE0000000);
    	streamTypeDescriptors[10] = new StreamTypeDescriptor(0xBDFFC000, 0xFFFFFFFF, 0xFFFFFF00);
    	streamTypeDescriptors[11] = new StreamTypeDescriptor(0xBDFFA000, 0xFFFFFFFF, 0xFFFFFF00);
    	streamTypeDescriptors[12] = new StreamTypeDescriptor(0xBDFFA100, 0xFFFFFFFF, 0xFFFFFF00);
    	streamTypeDescriptors[13] = new StreamTypeDescriptor(0xBDFF9000, 0xFFFFFFFF, 0xFFFFFF00);
    	streamTypeDescriptors[14] = new StreamTypeDescriptor(0xC0000000, 0xFF000000, 0xC0000000);
    	streamTypeDescriptors[15] = new StreamTypeDescriptor(0xBD800000, 0xFFFF0000, 0xFFE00000);
    	streamTypeDescriptors[16] = new StreamTypeDescriptor(0xBDA00000, 0xFFFF0000, 0xFFF80000);
    	streamTypeDescriptors[17] = new StreamTypeDescriptor(0xBD880000, 0xFFFF0000, 0xFFF80000);
    	streamTypeDescriptors[18] = new StreamTypeDescriptor(0xBD900000, 0xFFFF0000, 0xFFF80000);

    	streamChannelDescriptors[0] = new StreamChannelDescriptor(0xBF01E000, 0xFFFFE000,  8, 0x1F,  0);
    	streamChannelDescriptors[1] = new StreamChannelDescriptor(0xE0000000, 0xE0000000, 24, 0x1F, 16);
    	streamChannelDescriptors[2] = new StreamChannelDescriptor(0xBD000000, 0xFFF00000, 16, 0x0F, 32);
    	streamChannelDescriptors[3] = new StreamChannelDescriptor(0xBD100000, 0xFFF00000, 16, 0x0F, 48);
    	streamChannelDescriptors[4] = new StreamChannelDescriptor(0xBD800000, 0xFFE00000, 16, 0x1F, 64);
    	streamChannelDescriptors[5] = new StreamChannelDescriptor(0xBD200000, 0xFFF00000, 16, 0x0F, 96);
    }

    @Override
	public int getMemoryUsage() {
		// No need to allocate additional memory when the module has been
		// loaded using sceKernelLoadModuleToBlock()
		// by the PSP "flash0:/kd/utility.prx".
		// The memory has already been allocated in that case.
		if (Modules.ModuleMgrForKernelModule.isMemoryAllocatedForModule("flash0:/kd/mpeg.prx")) {
			return 0;
		}

		return 0xC000;
	}

    private static class SceMpegStreamPacketInfo extends pspAbstractMemoryMappedStructure {
    	public int startCode;            // Offset 0
    	public int packetLength;         // Offset 4
    	public int pesScramblingControl; // Offset 8
    	public long pts;                 // Offset 12
    	public long dts;                 // Offset 20
    	public int packetDataAddr;       // Offset 28
    	public int packetDataLength;     // Offset 32
    	public int packetHeaderAddr;     // Offset 36
    	public int unused;               // Offset 40

		@Override
		protected void read() {
			startCode = read32();
			packetLength = read32();
			pesScramblingControl = read32();
			pts = longSwap64(read64());
			dts = longSwap64(read64());
			packetDataAddr = read32();
			packetDataLength = read32();
			packetHeaderAddr = read32();
			unused = read32();
		}

		@Override
		protected void write() {
			write32(startCode);
			write32(packetLength);
			write32(pesScramblingControl);
			write64(longSwap64(pts));
			write64(longSwap64(dts));
			write32(packetDataAddr);
			write32(packetDataLength);
			write32(packetHeaderAddr);
			write32(unused);
		}

		@Override
		public int sizeof() {
			return 44;
		}

		@Override
		public String toString() {
			return String.format("startCode=0x%08X, packetLength=0x%X, pesScramblingCode=0x%X, pts=%d, dts=%d, packetDataAddr=0x%08X, packetDataLength=0x%X, packetHeaderAddr=0x%08X", startCode, packetLength, pesScramblingControl, pts, dts, packetDataAddr, packetDataLength, packetHeaderAddr);
		}
    }

    private class AfterVideocodecDecodeAction implements IAction {
    	private SceKernelThreadInfo thread;
    	private TPointer data;
    	private TPointer auAddr;
    	private int frameWidth;
    	private TPointer32 bufferAddr;
    	private TPointer32 gotFrameAddr;

		public AfterVideocodecDecodeAction(TPointer data, TPointer auAddr, int frameWidth, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
			this.thread = Modules.ThreadManForUserModule.getCurrentThread();
			this.data = data;
			this.auAddr = auAddr;
			this.frameWidth = frameWidth;
			this.bufferAddr = bufferAddr;
			this.gotFrameAddr = gotFrameAddr;
		}

		@Override
		public void execute() {
			afterVideocodecDecode(thread, data, auAddr, frameWidth, bufferAddr, gotFrameAddr);
		}
    }

    private class AfterVideocodecDecodeYCbCrAction implements IAction {
    	private SceKernelThreadInfo thread;
    	private TPointer data;
    	private TPointer auAddr;
    	private TPointer32 bufferAddr;
    	private TPointer32 gotFrameAddr;

		public AfterVideocodecDecodeYCbCrAction(TPointer data, TPointer auAddr, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
			this.thread = Modules.ThreadManForUserModule.getCurrentThread();
			this.data = data;
			this.auAddr = auAddr;
			this.bufferAddr = bufferAddr;
			this.gotFrameAddr = gotFrameAddr;
		}

		@Override
		public void execute() {
			afterVideocodecDecodeYCbCr(thread, data, auAddr, bufferAddr, gotFrameAddr);
		}
    }

    private static class RingbufferBitReader extends BitReader {
    	private final int addrMax;
    	private final int addrMin;

		public RingbufferBitReader(int addr, int addrMin, int addrMax, int size) {
			super(addr, size);

			this.addrMin = addrMin;
			this.addrMax = addrMax;
		}

		@Override
		protected int nextAddr() {
			int addr = super.nextAddr();
			if (addr >= addrMax) {
				addr = addrMin;
			}
			return addr;
		}

		@Override
		protected int previousAddr() {
			int addr = super.previousAddr();
			if (addr < addrMin) {
				addr = addrMax - 1;
			}
			return addr;
		}
    }

    public static boolean isMpeg260() {
    	return Emulator.getInstance().getFirmwareVersion() <= 260;
    }

    private void initHLE() {
    	if (hleInitialized) {
    		return;
    	}

    	int moduleMemory = getModuleMemory();

		Memory mem = getMemory();
		initVideocodecBuffer = new TPointer(mem, moduleMemory);
		moduleMemory += sceVideocodec.videocodecBufferSize;
		initVideocodecBuffer2 = new TPointer(mem, moduleMemory);
		moduleMemory += 0x28;
		initAudiocodecBuffer = new TPointer(mem, moduleMemory);
		moduleMemory += sceAudiocodec.audiocodecBufferSize;
		emptyBuffer = new TPointer(mem, moduleMemory);
		moduleMemory += 128;
		sceMpegAvcResource = new TPointer(mem, moduleMemory);
		moduleMemory += 8;
		sceMpegAvcCscBuffer1 = new TPointer(mem, moduleMemory);
		moduleMemory += SceMp4AvcCscStruct.SIZE_OF;
		sceMpegAvcCscBuffer2 = new TPointer32(mem, moduleMemory);
		moduleMemory += 16;
		sceMpegAvcDecodeYCbCrBuffer1 = new TPointer(mem, moduleMemory);
		moduleMemory += SceMp4AvcCscStruct.SIZE_OF;
		sceMpegAvcDecodeYCbCrBuffer2 = new TPointer(mem, moduleMemory);
		moduleMemory += 56;
		sceMpegAvcConvertToYuv420Buffer = new TPointer(mem, moduleMemory);
		moduleMemory += 24;

		hleInitialized = true;
    }

    private long readTimestamp(IBitReader reader) {
		long a = reader.read(3);
		reader.skip(1);
		long b = reader.read(15);
		reader.skip(1);
		long c = reader.read(15);
		reader.skip(1);

		return (a << 30) | (b << 15) | c;
    }

    private int readStreamPacketInfo(SceMpegStreamPacketInfo packetInfo, IBitReader reader) {
    	int length = reader.read(16);
    	packetInfo.packetLength = length;
    	packetInfo.pesScramblingControl = -1;
    	packetInfo.pts = UNKNOWN_TIMESTAMP;
    	packetInfo.dts = UNKNOWN_TIMESTAMP;
    	packetInfo.packetDataAddr = reader.getReadAddr();
    	packetInfo.packetDataLength = length;
    	packetInfo.packetHeaderAddr = reader.getReadAddr() - 6;
    	switch (packetInfo.startCode) {
    		case 0xBF000000: // Private Stream 2
    			packetInfo.startCode |= reader.read(24);
    			length -= 3;
    			break;
			case 0xBE000000: // Padding Stream
    		case 0xBC000000: // Program Stream Map
    		case 0xF0000000: // ECM Stream
    		case 0xF1000000: // EMM Stream
    		case 0xF2000000:
    		case 0xF8000000:
    		case 0xFF000000: // Program Stream Directory
    			break;
			default:
				// Read PES header
				reader.skip(2);
				packetInfo.pesScramblingControl = reader.read(2);
				reader.skip(4);
				int ptsDtsFlags = reader.read(2);
				boolean escrFlag = reader.readBool();
				boolean esRateFlag = reader.readBool();
				boolean dsmTrickModeFlag = reader.readBool();
				boolean additionalCopyInfoFlag = reader.readBool();
				boolean crcFlag = reader.readBool();
				boolean extensionFlag = reader.readBool();
				int headerLength = reader.read(8);
				length -= 3 + headerLength;
				if ((ptsDtsFlags & 0x2) != 0) {
					reader.skip(4);
					packetInfo.pts = readTimestamp(reader);
					headerLength -= 5;
					if (ptsDtsFlags == 0x3) {
						reader.skip(4);
						packetInfo.dts = readTimestamp(reader);
						headerLength -= 5;
					}
				}
				if (escrFlag) {
					reader.skip(48);
					headerLength -= 6;
				}
				if (esRateFlag) {
					reader.skip(24);
					headerLength -= 3;
				}
				if (dsmTrickModeFlag) {
					reader.skip(8);
					headerLength -= 1;
				}
				if (additionalCopyInfoFlag) {
					reader.skip(8);
					headerLength -= 1;
				}
				if (crcFlag) {
					reader.skip(16);
					headerLength -= 2;
				}
				if (extensionFlag) {
					boolean privateDataFlag = reader.readBool();
					boolean packHeaderFieldFlag = reader.readBool();
					boolean programPacketSequenceCounterFlag = reader.readBool();
					boolean pStdBufferFlag = reader.readBool();
					reader.skip(3);
					boolean extensionFlag2 = reader.readBool();
					headerLength--;
					if (privateDataFlag) {
						reader.skip(48);
						reader.skip(48);
						reader.skip(32);
						headerLength -= 16;
					}
					if (packHeaderFieldFlag) {
						return ERROR_MPEG_ILLEGAL_STREAM;
					}
					if (programPacketSequenceCounterFlag) {
						reader.skip(16);
						headerLength -= 2;
					}
					if (pStdBufferFlag) {
						reader.skip(16);
						headerLength -= 2;
					}
					if (extensionFlag2) {
						reader.skip(1);
						int extensionFieldLength = reader.read(7);
						headerLength--;
						reader.skip(8 * extensionFieldLength);
						headerLength -= extensionFieldLength;
					}
				}
				if (headerLength > 0) {
					reader.skip(8 * headerLength);
				}

				packetInfo.packetDataAddr = reader.getReadAddr();
		    	packetInfo.packetDataLength = length;

		    	if (packetInfo.startCode == 0xBD000000) {
					packetInfo.startCode |= reader.read(24);
					reader.skip(24);
					length -= 6;
				}

				break;
    	}

    	if (length > 0) {
    		reader.skip(length * 8);
    	}

    	return 1;
    }

    private class AfterRingbufferPutCallback implements IAction {
    	private TPointer ringbufferAddr;
    	private int remainingPackets;
    	private int gp;
    	private int processedPackets;
    	private int returnValue;

    	public AfterRingbufferPutCallback(TPointer ringbufferAddr, int remainingPackets, int gp) {
    		this.ringbufferAddr = ringbufferAddr;
    		this.remainingPackets = remainingPackets;
    		this.gp = gp;
    	}

    	@Override
        public void execute() {
    		hleMpegRingbufferPostPut(ringbufferAddr, this, getProcessor().cpu._v0, gp);
        }

		public int getRemainingPackets() {
			return remainingPackets;
		}

		public boolean hasRemainingPackets() {
			return remainingPackets > 0;
		}

		public void addProcessedPackets(int packets) {
			processedPackets += packets;
			remainingPackets -= packets;
		}

		public boolean hasProcessedPackets() {
			return processedPackets > 0;
		}

		public int getProcessedPackets() {
			return processedPackets;
		}

		public int getReturnValue() {
			return returnValue;
		}

		public void setReturnValue(int returnValue) {
			this.returnValue = returnValue;
		}
    }

    protected void hleMpegRingbufferPostPut(TPointer ringbufferAddr, AfterRingbufferPutCallback afterRingbufferPutCallback, int packetsAdded, int gp) {
    	getProcessor().cpu._gp = gp;

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleMpegRingbufferPostPut callback returned 0x%X", packetsAdded));
    	}

    	if (packetsAdded < 0) {
        	if (!afterRingbufferPutCallback.hasProcessedPackets()) {
        		afterRingbufferPutCallback.setReturnValue(packetsAdded);
        		return;
        	}
        	packetsAdded = 0;
        }

        SceMpegRingbuffer sceMpegRingbuffer = new SceMpegRingbuffer();
        sceMpegRingbuffer.read(ringbufferAddr);

        sceMpegRingbuffer.packetsWritten += packetsAdded;
        if (sceMpegRingbuffer.packetsWritten >= sceMpegRingbuffer.packets) {
            sceMpegRingbuffer.packetsWritten -= sceMpegRingbuffer.packets;
        }
        sceMpegRingbuffer.packetsInRingbuffer += packetsAdded;
        sceMpegRingbuffer.write(ringbufferAddr);

        afterRingbufferPutCallback.addProcessedPackets(packetsAdded);
        if (packetsAdded > 0 && afterRingbufferPutCallback.hasRemainingPackets()) {
        	int callbackNumPackets = afterRingbufferPutCallback.getRemainingPackets();
        	if (sceMpegRingbuffer.packets < sceMpegRingbuffer.packetsWritten + callbackNumPackets) {
        		callbackNumPackets = sceMpegRingbuffer.packets - sceMpegRingbuffer.packetsWritten;
        	}
        	int callbackDataAddress = sceMpegRingbuffer.data + sceMpegRingbuffer.packetsWritten * sceMpegRingbuffer.packetSize;
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleMpegRingbufferPostPut calling callback 0x%08X with dataAddress=0x%08X, numPackets=0x%X", sceMpegRingbuffer.callbackAddr, callbackDataAddress, callbackNumPackets));
        	}

            getProcessor().cpu._gp = sceMpegRingbuffer.gp;
        	Modules.ThreadManForUserModule.executeCallback(null, sceMpegRingbuffer.callbackAddr, afterRingbufferPutCallback, false, callbackDataAddress, callbackNumPackets, sceMpegRingbuffer.callbackArgs);
        } else {
        	int processedPackets = afterRingbufferPutCallback.getProcessedPackets();
        	processedPackets = Math.min(processedPackets, sceMpegRingbuffer.packetsInRingbuffer);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("hleMpegRingbufferPostPut returning processedPackets=0x%X", processedPackets));
        	}

        	int result = scanStreamPackets(sceMpegRingbuffer, processedPackets);
        	if (result < 0) {
        		afterRingbufferPutCallback.setReturnValue(result);
        	}

    		sceMpegRingbuffer.packetsInRingbuffer -= processedPackets;
    		sceMpegRingbuffer.packetsRead += processedPackets;
    		if (sceMpegRingbuffer.packetsRead >= sceMpegRingbuffer.packets) {
    			sceMpegRingbuffer.packetsRead -= sceMpegRingbuffer.packets;
    		}
            sceMpegRingbuffer.write(ringbufferAddr);

    		afterRingbufferPutCallback.setReturnValue(processedPackets);
        }
    }

    private int scanStreamPackets(SceMpegRingbuffer sceMpegRingbuffer, int numberOfPackets) {
    	TPointer data = sceMpegRingbuffer.mpeg.getPointer();

    	int readAddr = sceMpegRingbuffer.data + sceMpegRingbuffer.packetsRead * sceMpegRingbuffer.packetSize;
    	RingbufferBitReader reader = new RingbufferBitReader(readAddr, sceMpegRingbuffer.data, sceMpegRingbuffer.dataUpperBound, numberOfPackets * sceMpegRingbuffer.packetSize);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("scanStreamPackets packets %d to %d", sceMpegRingbuffer.packetsRead, (sceMpegRingbuffer.packetsRead + numberOfPackets) % sceMpegRingbuffer.packets));
    	}

    	SceMpegStreamPacketInfo packetInfo = new SceMpegStreamPacketInfo();
    	int packetIndex = 0;
    	while (reader.getBitsLeft() >= 32) {
    		int startBit = reader.read(24);
    		packetInfo.startCode = reader.read(8) << 24;
    		if (startBit != 0x000001) {
    			return ERROR_MPEG_ILLEGAL_STREAM;
    		}
    		if (packetInfo.startCode == 0xBA000000) {
    			// Program Stream Pack Header
    			reader.skip(2);
    			long systemClockReference = readTimestamp(reader);
    			int scrExtension = reader.read(9);
    			if (log.isTraceEnabled()) {
    				log.trace(String.format("StartCode 0x%08X: System Clock Reference (SCR)=0x%X, SCR extension=0x%X", packetInfo.startCode, systemClockReference, scrExtension));
    			}
    			reader.skip(30);
    			int unknownLength = reader.read(3);
    			reader.skip(unknownLength * 8);

    			int systemHeaderStartCode = reader.read(32);
    			if (systemHeaderStartCode == 0x1BB) {
    				// Program Stream System Header
    				reader.skip(16); // Header length
    				reader.skip(24); // Rate bound
    				reader.skip(24); // Flags
    				// By definition the processing of the Program Stream System Header will continue
    				// so long as the most significant bit of the next available byte is set,
    				// regardless of the header length.
    				while (true) {
	    				boolean unknownFlag = reader.readBool();
	    				if (!unknownFlag) {
	    					reader.skip(-1);
	    					break;
	    				}
    					reader.skip(23);
    				}
    			} else {
    				reader.skip(-32);
    			}

    			packetIndex = 0;
    		} else if (packetInfo.startCode == 0xB9000000) {
    			// Program Stream End
    			break;
    		} else {
    			int result = readStreamPacketInfo(packetInfo, reader);
    			if (result < 0) {
    				return result;
    			}

    			TPointer dataStreamAddr = getDataStreamAddress(data, packetInfo.startCode);
    			if (dataStreamAddr != null && dataStreamAddr.getValue32(8) != -1) {
    				if (packetIndex >= 2) {
    					return ERROR_MPEG_ILLEGAL_STREAM;
    				}
    				int packetByteIndex = packetInfo.packetHeaderAddr - sceMpegRingbuffer.data;
    				int packetNumber = packetByteIndex / sceMpegRingbuffer.packetSize;
    				if (log.isTraceEnabled()) {
    					log.trace(String.format("Found data for stream 0x%08X/%s in ringbuffer packet #%d offset 0x%X", packetInfo.startCode, dataStreamAddr, packetNumber, packetByteIndex % sceMpegRingbuffer.packetSize));
    				}
    				TPointer ringbufferPacketInfo = new TPointer(sceMpegRingbuffer.mpeg.getMemory(), sceMpegRingbuffer.dataUpperBound + packetNumber * 104 + packetIndex * 52);
    				ringbufferPacketInfo.setValue32(0, -1);
    				ringbufferPacketInfo.setValue32(4, dataStreamAddr.getValue32(0));
    				packetInfo.write(ringbufferPacketInfo, 8);
    				if (dataStreamAddr.getValue32(0) != -1) {
    					dataStreamAddr.getPointer(0).setPointer(0, ringbufferPacketInfo);
    				}
    				dataStreamAddr.setPointer(0, ringbufferPacketInfo);
    				if (dataStreamAddr.getValue32(4) == -1) {
    					dataStreamAddr.setValue32(4, dataStreamAddr.getValue32(0));
    				}
    				dataStreamAddr.setValue32(12, dataStreamAddr.getValue32(12) + 1);
    				verifyDataStream(dataStreamAddr, "scanStreamPackets: ");
    			}

    			packetIndex++;
    		}
    	}

    	return 0;
    }

    private int copyYCbCr(TPointer data, TPointer destinationAddr, TPointer sourceAddr, int imageIndex) {
    	int result = 0;

    	TPointer videocodecBuffer2 = data.getPointer(1732);
    	TPointer mpegAvcYuvStruct = data.getPointer(1736);

    	if (imageIndex == -1) {
    		int mode = sourceAddr.getValue32(16);
    		if (mode == 2) {
    			imageIndex = sourceAddr.getValue32(48);
    		}
    	}

		int mode = destinationAddr.getValue32(16);
    	int width;
    	int height;
    	if (imageIndex == -1) {
    		if (mode == 2) {
    			return ERROR_MPEG_INVALID_VALUE;
    		}
    		mpegAvcYuvStruct = new TPointer(sourceAddr, 16);
    		width = sourceAddr.getValue32(4) << 4;
    		height = sourceAddr.getValue32(0) << 4;
    	} else {
    		mpegAvcYuvStruct.add(44 * imageIndex);
        	width = videocodecBuffer2.getValue32(8);
    		height = videocodecBuffer2.getValue32(12);
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("copyYCbCr width=%d, height=%d, mode=0x%X, destinationAddr=%s, imageIndex=%d", width, height, mode, destinationAddr, imageIndex));
    	}

		if (mode == 3) {
			int size1 = width * height;
			int size2 = size1 >>> 2;
			int returnValue = (width << 16) | height;
			if ((width & 0x70) == 0x70) {
				size1 += height << 4;
				size2 += height << 2;
				returnValue = ((width + 16) << 16) | height;
			}
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(0, width);
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(4, height);
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(8, destinationAddr.getValue32(12));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(12, mpegAvcYuvStruct.getValue32( 0));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(16, mpegAvcYuvStruct.getValue32( 4));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(20, mpegAvcYuvStruct.getValue32( 8));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(24, mpegAvcYuvStruct.getValue32(12));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(28, mpegAvcYuvStruct.getValue32(16));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(32, mpegAvcYuvStruct.getValue32(20));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(36, mpegAvcYuvStruct.getValue32(24));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(40, mpegAvcYuvStruct.getValue32(28));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(44, destinationAddr.getValue32(20));
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(48, destinationAddr.getValue32(20) + size1);
			sceMpegAvcDecodeYCbCrBuffer2.setValue32(52, destinationAddr.getValue32(20) + size1 + size2);
			result = Modules.sceVideocodecModule.sceVideocodec_D95C24D5(sceMpegAvcDecodeYCbCrBuffer2);
			if (log.isTraceEnabled()) {
				log.trace(String.format("copyYCbCr: sceVideocodec_D95C24D5 returned 0x%X", result));
			}
			if (result == 0) {
				result = returnValue;
			}
		} else {
			destinationAddr.setValue32(0, height >>> 4);
			destinationAddr.setValue32(4, width >>> 4);
			destinationAddr.setValue32(8, 0);
			destinationAddr.setValue32(12, 0);
			if (mode == 2) {
				destinationAddr.setValue32(48, imageIndex);
			} else {
	            SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
	            mp4AvcCscStruct.height = height;
	    		mp4AvcCscStruct.width = width;
	    		mp4AvcCscStruct.mode0 = 0;
	    		mp4AvcCscStruct.mode1 = 0;
	    		mp4AvcCscStruct.buffer0 = mpegAvcYuvStruct.getValue32( 0) + sceMpegAvcCscOffsets[0];
	    		mp4AvcCscStruct.buffer1 = mpegAvcYuvStruct.getValue32( 4) + sceMpegAvcCscOffsets[1];
	    		mp4AvcCscStruct.buffer2 = mpegAvcYuvStruct.getValue32( 8) + sceMpegAvcCscOffsets[0];
	    		mp4AvcCscStruct.buffer3 = mpegAvcYuvStruct.getValue32(12) + sceMpegAvcCscOffsets[1];
	    		mp4AvcCscStruct.buffer4 = mpegAvcYuvStruct.getValue32(16) + sceMpegAvcCscOffsets[2];
	    		mp4AvcCscStruct.buffer5 = mpegAvcYuvStruct.getValue32(20) + sceMpegAvcCscOffsets[3];
	    		mp4AvcCscStruct.buffer6 = mpegAvcYuvStruct.getValue32(24) + sceMpegAvcCscOffsets[2];
	    		mp4AvcCscStruct.buffer7 = mpegAvcYuvStruct.getValue32(28) + sceMpegAvcCscOffsets[3];
	    		mp4AvcCscStruct.write(sceMpegAvcDecodeYCbCrBuffer1);

	    		result = Modules.sceMpegbaseModule.sceMpegBaseYCrCbCopy(destinationAddr, sceMpegAvcDecodeYCbCrBuffer1, 0x3);
				if (log.isTraceEnabled()) {
					log.trace(String.format("copyYCbCr: sceMpegBaseYCrCbCopy returned 0x%X", result));
				}
			}
		}

		return result;
    }

    private int completeAvcDecodeYCbCr(TPointer data, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
    	int result = 0;

    	TPointer videocodecBuffer2 = data.getPointer(1732);

    	int numberOfDecodedImages = videocodecBuffer2.getValue32(32);
    	gotFrameAddr.setValue(numberOfDecodedImages);

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("completeAvcDecodeYCbCr bufferAddr=%s, numberOfDecodedImages=%d", bufferAddr, numberOfDecodedImages));
    	}

    	for (int i = 0; i < numberOfDecodedImages; i++) {
    		TPointer buffer = bufferAddr.getPointer(i * 4);
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("completeAvcDecodeYCbCr i=%d: buffer=%s", i, buffer));
    		}
    		if (buffer.isNull()) {
    			result = ERROR_MPEG_INVALID_VALUE;
    			break;
    		}

    		result = copyYCbCr(data, buffer, null, i);
    		if (result != 0) {
    			break;
    		}
    	}

    	return result;
    }

    private void afterVideocodecDecodeYCbCr(SceKernelThreadInfo thread, TPointer data, TPointer auAddr, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);
    	au.esSize = 0;
    	au.write(auAddr);

    	int result = thread.cpuContext._v0;
    	if (result != 0) {
    		return;
    	}

    	result = completeAvcDecodeYCbCr(data, bufferAddr, gotFrameAddr);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegAvcDecodeYCbCr numberOfDecodedImages=%d returning 0x%X", gotFrameAddr.getValue(), result));
    	}
    	thread.cpuContext._v0 = result;
    }

    private void afterVideocodecDecode(SceKernelThreadInfo thread, TPointer data, TPointer auAddr, int frameWidth, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);
    	au.esSize = 0;
    	au.write(auAddr);

    	int result = thread.cpuContext._v0;
    	if (result != 0) {
    		return;
    	}

    	TPointer videocodecBuffer2 = data.getPointer(1732);
    	TPointer mpegAvcYuvStruct = data.getPointer(1736);

    	int numberOfDecodedImages = videocodecBuffer2.getValue32(32);
    	gotFrameAddr.setValue(numberOfDecodedImages);

    	int mode = data.getValue32(1708);
    	if (mode < 3 || mode > 6) {
	    	for (int i = 0; i < numberOfDecodedImages; i++) {
	            SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
	            mp4AvcCscStruct.height = videocodecBuffer2.getValue32(12) >> 4;
	    		mp4AvcCscStruct.width = videocodecBuffer2.getValue32(8) >> 4;
	    		mp4AvcCscStruct.mode0 = 0;
	    		mp4AvcCscStruct.mode1 = 0;
	    		mp4AvcCscStruct.buffer0 = mpegAvcYuvStruct.getValue32(0);
	    		mp4AvcCscStruct.buffer1 = mpegAvcYuvStruct.getValue32(4);
	    		mp4AvcCscStruct.buffer2 = mpegAvcYuvStruct.getValue32(8);
	    		mp4AvcCscStruct.buffer3 = mpegAvcYuvStruct.getValue32(12);
	    		mp4AvcCscStruct.buffer4 = mpegAvcYuvStruct.getValue32(16);
	    		mp4AvcCscStruct.buffer5 = mpegAvcYuvStruct.getValue32(20);
	    		mp4AvcCscStruct.buffer6 = mpegAvcYuvStruct.getValue32(24);
	    		mp4AvcCscStruct.buffer7 = mpegAvcYuvStruct.getValue32(28);
	    		TPointer buffer = bufferAddr.getPointer(i * 4);
	    		result = Modules.sceMpegbaseModule.hleMpegBaseCscAvc(buffer, 0, frameWidth, mp4AvcCscStruct);
	        	if (result != 0) {
	        		break;
	        	}

	        	if (log.isTraceEnabled()) {
	        		log.trace(String.format("sceMpegAvcDecode numberOfDecodedImages=%d, buffer=%s", numberOfDecodedImages, buffer));
	        	}

	        	mpegAvcYuvStruct.add(44);
	    	}
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegAvcDecode numberOfDecodedImages=%d returning 0x%08X", numberOfDecodedImages, result));
    	}
    	thread.cpuContext._v0 = result;
    }

    private static int getSizeFromPackets(int packets) {
        int size = (packets * 104) + (packets * SceMpegRingbuffer.ringbufferPacketSize);

        return size;
    }

    private static int getPacketsFromSize(int size) {
    	int packets = size / (MPEG_AVC_ES_SIZE + 104);

    	return packets;
    }

    private void checkLibmpeg(TPointer data) {
    	if (!"LIBMPEG".equals(data.getStringNZ(8))) {
    		throw new SceKernelErrorException(ERROR_MPEG_INVALID_VALUE);
    	}
    }

    public TPointer32 checkMpeg(TPointer32 mpeg) {
    	TPointer data = mpeg.getPointer();
    	checkLibmpeg(data);

    	return mpeg;
    }

    public TPointer checkRingbuffer(TPointer ringbuffer) {
    	TPointer mpeg = ringbuffer.getPointer(40);
    	TPointer data = mpeg.getPointer();
    	checkLibmpeg(data);

    	return ringbuffer;
    }

    private TPointer getDataStreamAddress(TPointer data, TPointer streamAddr) {
    	return getDataStreamAddress(data, streamAddr.getValue32(0));
    }

    private TPointer getDataStreamAddress(TPointer data, int streamChannelCode) {
    	for (StreamChannelDescriptor descriptor : streamChannelDescriptors) {
    		if (descriptor.isMatching(streamChannelCode)) {
    			return descriptor.getDataStreamAddr(data, streamChannelCode);
    		}
    	}

    	return null;
    }

    private TPointer getStreamAddress(TPointer data, int streamChannelCode) {
    	for (StreamChannelDescriptor descriptor : streamChannelDescriptors) {
    		if (descriptor.isMatching(streamChannelCode)) {
    			return descriptor.getStreamAddr(data, streamChannelCode);
    		}
    	}

    	return null;
    }

    private void verifyDataStream(TPointer dataStreamAddr, String message) {
    	int count = dataStreamAddr.getValue32(12);
    	if (dataStreamAddr.getValue32(4) == -1) {
    		if (count != 0) {
    			log.error(String.format("%sEmpty data stream %s with count=%d", message, dataStreamAddr, count));
    		}
    		return;
    	}
    	if (count == 0) {
			log.error(String.format("%sNon-empty data stream %s with count=%d", message, dataStreamAddr, count));
			return;
    	}

    	TPointer queue = dataStreamAddr.getPointer(4);
    	int addr = dataStreamAddr.getValue32(4);
    	for (int i = 1; i < count; i++) {
    		addr = queue.getValue32(0);
    		if (addr == -1) {
    			log.error(String.format("%sFound end of queue in %s at %d/%d", message, dataStreamAddr, i, count));
    			return;
    		}
    		queue = queue.getPointer(0);
    	}

    	if (queue.getValue32(0) != -1) {
    		log.error(String.format("%sNot end of queue in %s with count=%d", message, dataStreamAddr, count));
    		return;
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("verifyDataStream %s, count=%d: successful", dataStreamAddr, count));
    	}
    }

    private int getFirstStreamPacket(TPointer dataStreamAddr, SceMpegStreamPacketInfo streamPacketInfo) {
    	return getStreamPacket(dataStreamAddr, 0, streamPacketInfo);
    }

    private int getStreamPacket(TPointer dataStreamAddr, int index, SceMpegStreamPacketInfo streamPacketInfo) {
    	verifyDataStream(dataStreamAddr, "getStreamPacket: ");
    	if (dataStreamAddr.getValue32(4) == -1) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	int count = dataStreamAddr.getValue32(12);
    	if (count <= index) {
    		return ERROR_MPEG_NO_DATA;
    	}

    	TPointer queue = dataStreamAddr.getPointer(4);
    	for (int i = 0; i < index; i++) {
    		queue = queue.getPointer(0);
    	}
    	streamPacketInfo.read(queue, 8);

		return 0;
    }

    private int dequeueStreamPackets(TPointer dataStreamAddr, int count) {
    	verifyDataStream(dataStreamAddr, "dequeueStreamPackets: ");
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("dequeueStreamPackets dataStreamAddr=%s, count=%d", dataStreamAddr, count));
    	}

    	if (count <= 0) {
    		return 0;
    	}

    	if (dataStreamAddr.getValue32(4) == -1) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	int currentCount = dataStreamAddr.getValue32(12);
    	count = Math.min(count, currentCount);
    	int newCount = currentCount - count;
    	TPointer queue = dataStreamAddr.getPointer(4);
    	for (; count > 0; count--) {
    		TPointer next = queue.getPointer(0);
    		queue.setValue32(0, -1);
    		queue.setValue32(4, -1);
    		queue = next;
    	}

    	dataStreamAddr.setValue32(12, newCount);
    	if (newCount <= 0) {
    		dataStreamAddr.setValue32(0, -1);
    		dataStreamAddr.setValue32(4, -1);
    	} else {
        	dataStreamAddr.setPointer(4, queue);
    	}

    	return 0;
    }

    private int getEsBufferAddress(TPointer data, int esBuffer) {
    	if (esBuffer <= 0) {
    		return esBuffer;
    	}

    	int numberEsBuffers = data.getValue32(1712);
    	if (esBuffer > numberEsBuffers) {
    		return esBuffer;
    	}

    	int esBufferState = data.getValue8(1716 + esBuffer - 1) & 0xFF;
    	if (esBufferState != 1) {
    		return esBuffer;
    	}

    	return data.getValue32(1720 + (esBuffer - 1) * 4);
    }

    private TPointer getEsBufferPointer(TPointer data, int esBuffer) {
    	int esBufferAddr = getEsBufferAddress(data, esBuffer);
    	if (esBufferAddr == 0) {
    		return TPointer.NULL;
    	}

    	return new TPointer(data.getMemory(), esBufferAddr | MemoryMap.START_RAM);
    }

    private int copyStreamData(TPointer data, TPointer streamAddr, TPointer auAddr, int mode) {
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null || dataStreamAddr.getValue32(4) == -1) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);

		SceMpegStreamPacketInfo streamPacketInfo = new SceMpegStreamPacketInfo();
    	TPointer esBufferAddr = getEsBufferPointer(data, au.esBuffer);
    	int requiredLength = streamAddr.getValue32(36);
    	int totalLength = 0;
    	int count = dataStreamAddr.getValue32(12);
    	TPointer dataPacketsQueue = dataStreamAddr.getPointer(4);
		int processedCount = 0;

		if (log.isTraceEnabled()) {
			log.trace(String.format("copyStreamData streamAddr=%s, dataStreamAddr=%s, requiredLength=0x%X, packets count=%d", streamAddr, dataStreamAddr, requiredLength, count));
		}

		TPointer pesPacketCopyListStart = new TPointer(data, 44);
		TPointer pesPacketCopyList = new TPointer(pesPacketCopyListStart);
		for (int i = 0; i < count && requiredLength > 0; i++) {
        	streamPacketInfo.read(dataPacketsQueue, 8);

    		int sourceOffset = streamAddr.getValue32(20) + streamAddr.getValue16(28);
    		int sourceAddr = streamPacketInfo.packetDataAddr + sourceOffset;
    		int length = streamPacketInfo.packetDataLength - sourceOffset;

			requiredLength -= length;
			if (requiredLength <= 0) {
				if (requiredLength < 0) {
					length += requiredLength; // This will reduce the length as requiredLength is < 0
					streamAddr.setValue16(28, (short) (streamAddr.getValue16(28) + length));
				} else {
		    		streamAddr.setValue16(28, (short) 0);
		    		processedCount++;
				}

				if (processedCount > 0 || streamAddr.getValue32(32) > 0 || streamAddr.getValue32(52) == 1) {
					streamAddr.setValue32(52, 0);
					streamAddr.setValue32(24, streamAddr.getValue32(24) | 0x1);
				}
			} else {
				processedCount++;
			}

			pesPacketCopyList.setValue32(0, sourceAddr);
			pesPacketCopyList.setValue32(4, esBufferAddr.getAddress() + au.esSize + totalLength);
			pesPacketCopyList.setValue32(8, pesPacketCopyList.getAddress() + 16);
			pesPacketCopyList.setValue32(12, length);
			pesPacketCopyList.add(16);
			if (log.isTraceEnabled()) {
				log.trace(String.format("copyStreamData memcpy(0x%08X, 0x%08X, 0x%X)", esBufferAddr.getAddress() + au.esSize + totalLength, sourceAddr, length));
			}
			totalLength += length;

			if (requiredLength <= 0) {
				int alignment = streamAddr.getValue32(40);
				if (alignment != 0) {
					int rest = totalLength % alignment;
					if (rest > 0) {
						pesPacketCopyList.setPointer(0, emptyBuffer);
						pesPacketCopyList.setValue32(4, esBufferAddr.getAddress() + au.esSize + totalLength);
						pesPacketCopyList.setValue32(8, pesPacketCopyList.getAddress() + 16);
						pesPacketCopyList.setValue32(12, alignment - rest);
						pesPacketCopyList.add(16);
						if (log.isTraceEnabled()) {
							log.trace(String.format("copyStreamData clear(0x%08X, 0x%X)", esBufferAddr.getAddress() + au.esSize + totalLength, alignment - rest));
						}
					}
				}
				streamAddr.setValue32(44, 0);
				break;
			}

			streamAddr.setValue16(28, (short) 0);

    		// next packet
    		dataPacketsQueue = dataPacketsQueue.getPointer(0);
    	}

		if (pesPacketCopyList.getAddress() != pesPacketCopyListStart.getAddress()) {
			pesPacketCopyList.setValue32(-8, 0); // Set "next" field of last packet item to 0
		}

    	if (requiredLength > 0) {
    		streamAddr.setValue32(44, streamAddr.getValue32(44) + totalLength);
    		streamAddr.setValue32(32, streamAddr.getValue32(32) + processedCount);
    		streamAddr.setValue32(36, requiredLength);
    	} else {
    		streamAddr.setValue32(32, 0);
    	}

    	int result = 0;
    	int flags = streamAddr.getValue32(48);
    	if (au.esSize + totalLength <= esSizemaxLengths[mode] && (flags & 0x5) == 0 && ((flags & 0x2) == 0 || flags < 0)) {
    		result = Modules.sceMpegbaseModule.sceMpegBasePESpacketCopy(pesPacketCopyListStart);
    	}

    	au.esSize += totalLength;
    	au.write(auAddr);

    	dequeueStreamPackets(dataStreamAddr, processedCount);

    	return result;
    }

    private static int getYCbCrSize1(int width, int height) {
    	int incr = (width & 31) != 0 ? 32 : 0;
    	return (((width & ~31) + incr) >> 1) * (height >> 1);
    }

    private static int getYCbCrSize2(int width, int height) {
    	return ((width & ~31) >> 1) * (height >> 1);
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

    private boolean isOutsideRingbufferDataBuffer(SceMpegRingbuffer sceMpegRingbuffer, int addr) {
    	if (addr < sceMpegRingbuffer.data) {
    		return true;
    	}
    	if (addr > sceMpegRingbuffer.data + sceMpegRingbuffer.packets * sceMpegRingbuffer.packetSize) {
    		return true;
    	}
    	return false;
    }

    private int compareAddresses(SceMpegRingbuffer sceMpegRingbuffer, int addr1, int addr2) {
    	if (isOutsideRingbufferDataBuffer(sceMpegRingbuffer, addr1)) {
    		return SceKernelErrors.ERROR_MPEG_ILLEGAL_STREAM;
    	}
    	if (isOutsideRingbufferDataBuffer(sceMpegRingbuffer, addr2)) {
    		return SceKernelErrors.ERROR_MPEG_ILLEGAL_STREAM;
    	}

    	int readAddr = sceMpegRingbuffer.data + sceMpegRingbuffer.packetsRead * sceMpegRingbuffer.packetSize;
    	int writeAddr = sceMpegRingbuffer.data + sceMpegRingbuffer.packetsWritten * sceMpegRingbuffer.packetSize;
    	if (writeAddr < readAddr || (readAddr >= addr1 && readAddr >= addr2) || (addr1 >= writeAddr && addr2 >= writeAddr)) {
    		if (addr1 < addr2) {
    			return 1;
    		} else if (addr1 > addr2) {
    			return 3;
    		}
    		return 2;
    	}

    	return addr1 < addr2 ? 3 : 1;
    }

    /**
     * sceMpegInit
     * 
     * @return
     */
    @HLEFunction(nid = 0x682A619B, version = 150, stackUsage = 0x48)
    public int sceMpegInit() {
    	initHLE();

		initVideocodecBuffer.clear(sceVideocodec.videocodecBufferSize);
		initVideocodecBuffer2.clear(0x28);
		initVideocodecBuffer.setPointer(16, initVideocodecBuffer2);

		// Initialize the video codec
		int result = Modules.sceVideocodecModule.sceVideocodecOpen(initVideocodecBuffer, 0);
		if (result != 0) {
			return result;
		}

		result = Modules.sceVideocodecModule.sceVideocodecGetEDRAM(initVideocodecBuffer, 0);
		if (result != 0) {
			return result;
		}

		// Initialize the audio AT3+ codec
		initAudiocodecBuffer.clear(sceAudiocodec.audiocodecBufferSize);
		result = Modules.sceAudiocodecModule.sceAudiocodecCheckNeedMem(initAudiocodecBuffer, PSP_CODEC_AT3PLUS);
		if (result != 0) {
			return result;
		}

		result = Modules.sceAudiocodecModule.sceAudiocodecGetEDRAM(initAudiocodecBuffer, PSP_CODEC_AT3PLUS);
		if (result != 0) {
			return result;
		}

		return 0;
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
    @HLEFunction(nid = 0x21FF80E4, version = 150, checkInsideInterrupt = true, stackUsage = 0x18)
    public int sceMpegQueryStreamOffset(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.in) TPointer bufferAddr, @BufferInfo(usage=Usage.out) TPointer32 offsetAddr) {
		TPointer data = mpeg.getPointer();

        // Check magic
        if (bufferAddr.getUnalignedValue32(PSMF_MAGIC_OFFSET) != PSMF_MAGIC) {
            offsetAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

        // Check version
        int version = getMpegVersion(bufferAddr.getUnalignedValue32(PSMF_STREAM_VERSION_OFFSET));
        data.setValue32(12, version);
        if (version < 0) {
            offsetAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_BAD_VERSION;
        }

        // Check offset
        int streamOffset = endianSwap32(bufferAddr.getUnalignedValue32(PSMF_STREAM_OFFSET_OFFSET));
        offsetAddr.setValue(streamOffset);
        if ((streamOffset & 0x7FF) != 0 || streamOffset == 0) {
            offsetAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
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
    @HLEFunction(nid = 0x611E9E11, version = 150, checkInsideInterrupt = true, stackUsage = 0x8)
    public int sceMpegQueryStreamSize(TPointer bufferAddr, @BufferInfo(usage=Usage.out) TPointer32 sizeAddr) {
        int streamSize = endianSwap32(bufferAddr.getUnalignedValue32(PSMF_STREAM_SIZE_OFFSET));
        sizeAddr.setValue(streamSize);
        if ((streamSize & 0x7FF) != 0) {
        	sizeAddr.setValue(0);
            return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

        return 0;
    }

    /**
     * sceMpegFinish
     * 
     * @return
     */
    @HLEFunction(nid = 0x874624D6, version = 150, checkInsideInterrupt = true, stackUsage = 0x18)
    public int sceMpegFinish() {
		// Has no parameters
		if (initAudiocodecBuffer.getValue32(12) != 0) { // EDRAM
			Modules.sceAudiocodecModule.sceAudiocodecReleaseEDRAM(initAudiocodecBuffer);
		}
		Modules.sceVideocodecModule.sceVideocodecReleaseEDRAM(initVideocodecBuffer);

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
    	if (isMpeg260()) {
    		return MPEG_MEMSIZE_260;
    	}

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
    @HLEFunction(nid = 0xD8C5F121, version = 150, checkInsideInterrupt = true, stackUsage = 0xA8)
    public int sceMpegCreate(@BufferInfo(usage=Usage.out) TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer data, int size, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.inout) @CanBeNull TPointer ringbufferAddr, int frameWidth, int mode, int ddrtop) {
    	initHLE();

    	data.clear(size);

    	TPointer dataAligned = new TPointer(data.getMemory(), Utilities.alignUp(data.getAddress(), 63));
    	dataAligned.setStringNZ(0, 8, "LIBMPEG");
    	dataAligned.setStringNZ(8, 4, "001");
    	dataAligned.setValue32(12, -1);

    	if (mode == 1) {
    		dataAligned.setValue32(16, 0);
    		dataAligned.setValue32(20, 0);
    	} else {
        	SceMpegRingbuffer sceMpegRingbuffer = new SceMpegRingbuffer();
        	sceMpegRingbuffer.read(ringbufferAddr);

    		dataAligned.setPointer(16, ringbufferAddr);
    		dataAligned.setValue32(20, sceMpegRingbuffer.dataUpperBound);

    		// Set the mpeg value in the ringbuffer
        	sceMpegRingbuffer.mpeg = mpeg;
        	sceMpegRingbuffer.write(ringbufferAddr);
    	}

    	// Linked list used as input to sceMpegBasePESpacketCopy()
    	final int pesPacketCopyListSize = 99;
    	dataAligned.setValue32(40, pesPacketCopyListSize);
    	int offset = 44;
    	for (int i = 0; i < pesPacketCopyListSize; i++, offset += 16) {
    		dataAligned.setValue32(offset + 0, 0);
    		dataAligned.setValue32(offset + 4, 0);
    		dataAligned.setValue32(offset + 8, dataAligned.getAddress() + offset + 16);
    		dataAligned.setValue32(offset + 12, 0);
    	}
    	dataAligned.setValue32(offset - 16 + 8, 0);

    	TPointer dataStreamsAddr = new TPointer(dataAligned, 0x740);
    	dataAligned.setPointer(24, dataStreamsAddr);
    	for (int i = 0; i < MAX_STREAMS; i++) {
    		TPointer dataStreamAddr = new TPointer(dataStreamsAddr, i * 16);
    		dataStreamAddr.setValue32(0, -1);
    		dataStreamAddr.setValue32(4, -1);
    		dataStreamAddr.setValue32(8, -1);
    		dataStreamAddr.setValue32(12, 0);
    	}

    	TPointer streamsAddr2 = new TPointer(dataStreamsAddr, 0x740);
    	dataAligned.setPointer(28, streamsAddr2);

    	dataAligned.setPointer(32, new TPointer(streamsAddr2, 0x1E00));
    	dataAligned.setValue32(36, 0);

    	dataAligned.setValue32(1708, mode);

    	final int numberEsBuffers = 2;
    	dataAligned.setValue32(1712, numberEsBuffers);
    	for (int i = 0; i < numberEsBuffers; i++) {
    		dataAligned.setValue8(1716 + i, (byte) 0);
    	}
    	dataAligned.setValue32(1720, 0x0004A000); // First esBuffer, fixed address in kernel memory
    	dataAligned.setValue32(1724, 0x00062400); // Second esBuffer, fixed address in kernel memory

    	TPointer videocodecBuffer = new TPointer(dataAligned, 0xB300);
    	TPointer audiocodecBuffer = new TPointer(dataAligned, 0xB500);
    	if (isMpeg260()) {
    		final int reducedSize260 = 0x928;
    		videocodecBuffer.sub(reducedSize260);
    		audiocodecBuffer.sub(reducedSize260);
    	}

    	dataAligned.setPointer(1728, videocodecBuffer);

    	TPointer videocodecBuffer2 = new TPointer(videocodecBuffer, 0x80);
    	dataAligned.setPointer(1732, videocodecBuffer2);
    	videocodecBuffer2.clear(0x28);
    	videocodecBuffer.setPointer(16, videocodecBuffer2);

    	TPointer mpegAvcYuvStruct = new TPointer(videocodecBuffer2, 0x40);
    	dataAligned.setPointer(1736, mpegAvcYuvStruct);
    	mpegAvcYuvStruct.clear(0xB0);
    	videocodecBuffer.setPointer(44, mpegAvcYuvStruct);

    	TPointer videocodecBuffer3 = new TPointer(mpegAvcYuvStruct, 0xC0);
    	dataAligned.setPointer(1740, videocodecBuffer3);
    	videocodecBuffer3.clear(0x64);
    	videocodecBuffer.setPointer(48, videocodecBuffer3);

    	videocodecBuffer.setValue32(60, 4); // Unknown value

    	int result = Modules.sceVideocodecModule.sceVideocodecOpen(videocodecBuffer, 0);
    	if (result != 0) {
    		return result;
    	}

    	result = Modules.sceVideocodecModule.sceVideocodecInit(videocodecBuffer, 0);
    	if (result != 0) {
    		return result;
    	}

    	result = Modules.sceVideocodecModule.sceVideocodecGetVersion(videocodecBuffer, 0);
    	if (result != 0) {
    		return result;
    	}

    	videocodecBuffer.setValue32(64, Screen.width);
    	videocodecBuffer.setValue32(68, Screen.height);
    	videocodecBuffer.setValue32(72, 2);
    	videocodecBuffer.setValue32(76, 0);
    	result = Modules.sceVideocodecModule.sceVideocodecSetMemory(videocodecBuffer, 0);
    	if (result != 0) {
    		return result;
    	}

    	result = Modules.sceMpegbaseModule.sceMpegBaseCscInit(frameWidth);
    	if (result != 0) {
    		return result;
    	}

    	if (mode == 1) {
    		dataAligned.setValue32(1780, 0);
    		dataAligned.setValue32(1784, 0);
    		dataAligned.setValue32(1788, 0);
    	} else {
    		initAudiocodecBuffer.setValue16(40, (short) (mode == 3 ? 0x5C5C : 0x5C28));
    		initAudiocodecBuffer.setValue32(44, 0);
    		result = Modules.sceAudiocodecModule.sceAudiocodecCheckNeedMem(initAudiocodecBuffer, PSP_CODEC_AT3PLUS);
    		if (result != 0) {
    			return result;
    		}

    		dataAligned.setValue32(1780, initAudiocodecBuffer.getValue32(12)); // EDRAM
    		dataAligned.setValue32(1784, initAudiocodecBuffer.getValue32(16));
    		dataAligned.setPointer(1788, audiocodecBuffer); // audiocodecBuffer
    	}

		mpeg.setValue(dataAligned.getAddress());

		return 0;
    }

    /**
     * sceMpegDelete
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0x606A4649, version = 150, checkInsideInterrupt = true, stackUsage = 0x28)
    public int sceMpegDelete(@CheckArgument("checkMpeg") TPointer32 mpeg) {
    	TPointer data = mpeg.getPointer();

    	TPointer videocodecBuffer = data.getPointer(1728);
    	Modules.sceVideocodecModule.sceVideocodecDelete(videocodecBuffer, 0);

		TPointer dataStreamAddr = data.getPointer(24);
    	for (int i = 0; i < MAX_STREAMS; i++) {
    		dataStreamAddr.setValue32(0, -1);
    		dataStreamAddr.setValue32(4, -1);
    		dataStreamAddr.setValue32(8, -1);
    		dataStreamAddr.setValue32(12, 0);

    		// Next data stream
    		dataStreamAddr.add(16);
    	}

    	return 0;
    }

    /**
     * sceMpegRegistStream
     * 
     * @param mpeg
     * @param streamType
     * @param streamChannelNum
     * 
     * @return stream address
     */
    @HLEFunction(nid = 0x42560F23, version = 150, checkInsideInterrupt = true, stackUsage = 0x48)
    public int sceMpegRegistStream(@CheckArgument("checkMpeg") TPointer32 mpeg, int streamType, int streamChannelNum) {
    	if (streamType < 0 || streamType >= streamTypeDescriptors.length) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}
    	TPointer data = mpeg.getPointer();
    	int streamChannelCode = streamTypeDescriptors[streamType].getChannelCode(streamChannelNum);
    	TPointer streamAddr = getStreamAddress(data, streamChannelCode);
    	if (streamAddr == null) {
    		return 0;
    	}

    	if (streamType >= 7) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	streamAddr.setValue32(0, streamChannelCode);
    	streamAddr.setValue32(4, streamTypeDescriptors[streamType].getMask());
    	streamAddr.setValue32(12, streamType == 0 ? data.getValue32(32) + (streamChannelNum * MPEG_AVC_ES_SIZE) : 0);
    	streamAddr.setValue32(16, 0x7FFF);
		streamAddr.setValue32(20, streamDataOffsets[streamType]);
    	streamAddr.setValue32(24, 1);
		streamAddr.setValue32(32, 0);
		streamAddr.setValue32(40, 0);
		streamAddr.setValue32(44, 0);
		streamAddr.setValue32(48, 0);
		streamAddr.setValue32(52, 0);
		streamAddr.setValue32(56, 0);
		streamAddr.setValue32(64, 0);

		TPointer dataStreamAddr = getDataStreamAddress(data, streamChannelCode);
		if (dataStreamAddr == null) {
			return ERROR_MPEG_INVALID_VALUE;
		}
		if (dataStreamAddr.getValue32(8) != -1) {
			return ERROR_MPEG_ALREADY_USED;
		}
		dataStreamAddr.setValue32(0, -1);
		dataStreamAddr.setValue32(4, -1);
		dataStreamAddr.setPointer(8, streamAddr);
		dataStreamAddr.setValue32(12, 0);
		if (streamType != 0) {
			streamAddr.setValue32(8, -1);
		} else {
			int dataStream6ChannelCode = streamTypeDescriptors[6].getChannelCode(streamChannelNum);
			TPointer dataStream6Addr = getDataStreamAddress(data, dataStream6ChannelCode);
			if (dataStream6Addr == null) {
				return ERROR_MPEG_INVALID_VALUE;
			}
			if (dataStream6Addr.getValue32(8) != -1) {
				return ERROR_MPEG_ALREADY_USED;
			}
			dataStream6Addr.setValue32(0, -1);
			dataStream6Addr.setValue32(4, -1);
			dataStream6Addr.setPointer(8, streamAddr);
			dataStream6Addr.setValue32(12, 0);
			streamAddr.setPointer(8, dataStream6Addr);
		}

    	return streamAddr.getAddress();
    }

    /**
     * sceMpegUnRegistStream
     * 
     * @param mpeg
     * @param streamAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x591A4AA2, version = 150, checkInsideInterrupt = true, stackUsage = 0x18)
    public int sceMpegUnRegistStream(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.inout) TPointer streamAddr) {
    	TPointer data = mpeg.getPointer();
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (dataStreamAddr.getValue32(8) != -1) {
    		dataStreamAddr.setValue32(0, -1);
    		dataStreamAddr.setValue32(4, -1);
    		dataStreamAddr.setValue32(8, -1);
    		dataStreamAddr.setValue32(12, 0);
    		if (streamAddr.getValue32(8) != -1) {
    			TPointer dataStream6Addr = streamAddr.getPointer(8);
    			dataStream6Addr.setValue32(0, -1);
    			dataStream6Addr.setValue32(4, -1);
    			dataStream6Addr.setValue32(8, -1);
    			dataStream6Addr.setValue32(12, 0);
    		}
    	}

    	return 0;
    }

    /**
     * sceMpegMallocAvcEsBuf
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0xA780CF7E, version = 150, checkInsideInterrupt = true, stackUsage = 0x18)
    public int sceMpegMallocAvcEsBuf(@CheckArgument("checkMpeg") TPointer32 mpeg) {
    	TPointer data = mpeg.getPointer();
    	int result = 0;

    	int numberEsBuffers = data.getValue32(1712);
    	for (int i = 0; i < numberEsBuffers; i++) {
    		int esBufferState = data.getValue8(1716 + i) & 0xFF;
    		if (esBufferState == 0) {
    			result = i + 1;
    			data.setValue8(1716 + i, (byte) 1);
    			break;
    		}
    	}

    	return result;
    }

    /**
     * sceMpegFreeAvcEsBuf
     * 
     * @param mpeg
     * @param esBuffer
     * 
     * @return
     */
    @HLEFunction(nid = 0xCEB870B1, version = 150, checkInsideInterrupt = true, stackUsage = 0x28)
    public int sceMpegFreeAvcEsBuf(@CheckArgument("checkMpeg") TPointer32 mpeg, int esBuffer) {
    	TPointer data = mpeg.getPointer();

    	int numberEsBuffers = data.getValue32(1712);
    	if (esBuffer <= 0 || esBuffer > numberEsBuffers) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}
    	if (esBuffer > numberEsBuffers) {
    		return esBuffer;
    	}

    	data.setValue8(1716 + esBuffer - 1, (byte) 0); // esBufferState

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
    @HLEFunction(nid = 0xF8DCB679, version = 150, checkInsideInterrupt = true, stackUsage = 0x18)
    public int sceMpegQueryAtracEsSize(@CheckArgument("checkMpeg") TPointer32 mpeg, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 esSizeAddr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 outSizeAddr) {
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
    public int sceMpegQueryPcmEsSize(@CheckArgument("checkMpeg") TPointer32 mpeg, TPointer32 esSizeAddr, TPointer32 outSizeAddr) {
        esSizeAddr.setValue(MPEG_PCM_ES_SIZE);
        outSizeAddr.setValue(MPEG_PCM_ES_OUTPUT_SIZE);

        return 0;
    }

    /**
     * sceMpegInitAu
     * 
     * @param mpeg
     * @param esBuffer
     * @param auAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x167AFD9E, version = 150, checkInsideInterrupt = true, stackUsage = 0x18)
    public int sceMpegInitAu(@CheckArgument("checkMpeg") TPointer32 mpeg, int esBuffer, TPointer auAddr) {
    	int esSize = 0;

    	auAddr.setValue32(16, esBuffer);
    	auAddr.setValue32(20, esSize);

    	return 0;
    }

    /**
     * sceMpegChangeGetAvcAuMode
     * 
     * @param mpeg
     * @param streamAddr
     * @param mode
     * 
     * @return
     */
    @HLEFunction(nid = 0x234586AE, version = 150, checkInsideInterrupt = true)
    public int sceMpegChangeGetAvcAuMode(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer streamAddr, int mode) {
    	TPointer data = mpeg.getPointer();

    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (dataStreamAddr.getValue32(8) == -1) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (mode == 0) {
    		streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x2);
    	} else if (mode == 2) {
    		streamAddr.setValue32(48, streamAddr.getValue32(48) | 0x2);
    	} else {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	return 0;
    }

    /**
     * sceMpegChangeGetAuMode
     * 
     * @param mpeg
     * @param streamAddr
     * @param mode
     * 
     * @return
     */
    @HLEFunction(nid = 0x9DCFB7EA, version = 150, checkInsideInterrupt = true)
    public int sceMpegChangeGetAuMode(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer streamAddr, int mode) {
    	TPointer data = mpeg.getPointer();

    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (mode == 0) {
    		streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x1);
    	} else if (mode == 1) {
    		streamAddr.setValue32(48, streamAddr.getValue32(48) | 0x1);
    	} else {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	return 0;
    }

    /**
     * sceMpegGetAvcAu
     * 
     * @param mpeg
     * @param streamAddr
     * @param au_addr
     * @param attr_addr
     * 
     * @return
     */
    @HLEFunction(nid = 0xFE246728, version = 150, checkInsideInterrupt = true)
    public int sceMpegGetAvcAu(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer streamAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.out) TPointer auAddr, @BufferInfo(usage=Usage.out) @CanBeNull TPointer32 attrAddr) {
    	if (streamAddr.getValue32(8) == -1) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	TPointer data = mpeg.getPointer();
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_NO_DATA;
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMpegGetAvcAu dataStreamAddr=%s", dataStreamAddr));
    	}

    	SceMpegStreamPacketInfo packetInfo = new SceMpegStreamPacketInfo();

		int result;
    	while (true) {
    		result = getFirstStreamPacket(dataStreamAddr, packetInfo);
    		if (result < 0) {
    			result = ERROR_MPEG_NO_DATA;
    			break;
    		}

			TPointer dataAddr = streamAddr.getPointer(12);
			if (streamAddr.getValue32(44) == 0) {
				SceMpegAu au = new SceMpegAu();
				au.read(auAddr);

				au.esSize = 0;
				if (streamAddr.getValue32(16) == 0x7FFF) {
					au.pts = packetInfo.pts;
					au.dts = packetInfo.dts;

					TPointer dataStream6Addr = streamAddr.getPointer(8);
					SceMpegStreamPacketInfo stream6PacketInfo = new SceMpegStreamPacketInfo();
					result = getFirstStreamPacket(dataStream6Addr, stream6PacketInfo);
					if (result < 0) {
		    			result = ERROR_MPEG_NO_DATA;
						break;
					}

					SceMpegRingbuffer sceMpegRingbuffer = new SceMpegRingbuffer();
					sceMpegRingbuffer.read(data.getPointer(16));
					if (compareAddresses(sceMpegRingbuffer, packetInfo.packetDataAddr, stream6PacketInfo.packetDataAddr) == 1) {
						result = sceMpegNextAvcRpAu(mpeg, streamAddr);
						if (log.isTraceEnabled()) {
							log.trace(String.format("sceMpegGetAvcAu skipped next frame, returning 0x%X", result));
						}
						if (result != 0) {
							if (result == SceKernelErrors.ERROR_MPEG_NO_NEXT_DATA) {
								result = ERROR_MPEG_NO_DATA;
							}
							break;
						}
						continue;
					}

					dequeueStreamPackets(dataStream6Addr, 1);

					dataAddr.memcpy(stream6PacketInfo.packetDataAddr, stream6PacketInfo.packetDataLength);
					streamAddr.setValue32(16, 0);
					streamAddr.setValue16(28, (short) 0);
				} else {
					au.pts = UNKNOWN_TIMESTAMP;
					au.dts = UNKNOWN_TIMESTAMP;
				}

				au.write(auAddr);

				// Fetch the required data length
				int value = endianSwap32(dataAddr.getUnalignedValue32(14 + 4 + 4 * streamAddr.getValue32(16)));
				streamAddr.setValue32(36, value & 0x001FFFFF);
			}

			int value = endianSwap32(dataAddr.getUnalignedValue32(14 + 4 + 4 * streamAddr.getValue32(16)));
			boolean setBit;
			if ((value & 0x00800000) == 0) {
				setBit = false;
			} else {
				if (data.getValue32(12) == 0) { // 0 => PSMF Version 0012
					if (((dataAddr.getValue8(14 + 3) & 0xFF) - 1) == streamAddr.getValue32(16)) {
						setBit = false;
					} else {
						setBit = true;
					}
				} else {
					setBit = true;
				}
			}

			if (setBit) {
				streamAddr.setValue32(48, streamAddr.getValue32(48) | 0x80000000);
			} else {
				streamAddr.setValue32(48, streamAddr.getValue32(48) & 0x7FFFFFFF);
			}

			if (attrAddr.isNotNull()) {
				int attrValue = streamAddr.getValue32(48) >>> 31;
				attrValue |= (dataAddr.getValue8(14 + 4 + 4 * streamAddr.getValue32(16)) & 0x0F) << 1;
				attrAddr.setValue(attrValue);
			}

			result = copyStreamData(data, streamAddr, auAddr, 0);
			if (result == 0) {
				if (streamAddr.getValue32(16) < (dataAddr.getValue8(14 + 3) & 0xFF) - 1) {
					streamAddr.setValue32(16, streamAddr.getValue32(16) + 1);
				} else {
					streamAddr.setValue32(16, 0x7FFF);
				}
			}

			if (result != 0 || (streamAddr.getValue32(48) & 0x4) == 0) {
				break;
			}
			streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x4);
    	}

		return result;
    }

    /**
     * sceMpegGetPcmAu
     * 
     * @param mpeg
     * @param streamAddr
     * @param auAddr
     * @param attrAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x8C1E027D, version = 150, checkInsideInterrupt = true)
    public int sceMpegGetPcmAu(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer streamAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.out) TPointer auAddr, @BufferInfo(usage=Usage.out) @CanBeNull TPointer32 attrAddr) {
    	TPointer data = mpeg.getPointer();
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMpegGetPcmAu dataStreamAddr=%s", dataStreamAddr));
    	}

    	SceMpegStreamPacketInfo packetInfo = new SceMpegStreamPacketInfo();

		SceMpegAu au = new SceMpegAu();
		au.read(auAddr);

		int result;
    	while (true) {
    		result = getFirstStreamPacket(dataStreamAddr, packetInfo);
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("sceMpegGetPcmAu stream packet 0x%X: %s", result, packetInfo));
    		}

    		if (result < 0) {
    			result = ERROR_MPEG_NO_DATA;
    			break;
    		}

			if (streamAddr.getValue32(44) == 0) {
				au.esSize = 0;
				TPointer dataAddr;
				while (true) {
		    		result = getFirstStreamPacket(dataStreamAddr, packetInfo);
		    		if (result < 0) {
		    			au.write(auAddr);
		    			return ERROR_MPEG_NO_DATA;
		    		}

		    		dataAddr = new TPointer(data.getMemory(), packetInfo.packetDataAddr);

		    		if ((streamAddr.getValue32(24) & 0x1) == 0) {
		    			au.pts = UNKNOWN_TIMESTAMP;
		    			au.dts = UNKNOWN_TIMESTAMP;
		    			break;
		    		}

					short value = (short) endianSwap16(dataAddr.getUnalignedValue16(2));
					streamAddr.setValue16(28, value);
					if (value != -1) {
						au.pts = packetInfo.pts;
						au.dts = packetInfo.dts;
						streamAddr.setValue32(24, 0);
						break;
					}
					dequeueStreamPackets(dataStreamAddr, 1);
				}
				au.write(auAddr);

				int value = dataAddr.getValue8(1) & 0xFF;
				streamAddr.setValue32(64, value);
				streamAddr.setValue32(36, (value & 0x0F) * 160);
			}

			if (attrAddr.isNotNull()) {
				attrAddr.setValue(streamAddr.getValue32(64));
			}

			result = copyStreamData(data, streamAddr, auAddr, 2);

			if (result != 0 || (streamAddr.getValue32(48) & 0x4) == 0) {
				break;
			}
			streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x4);
    	}

		return result;
    }

    /**
     * sceMpegGetAtracAu
     * 
     * @param mpeg
     * @param streamAddr
     * @param auAddr
     * @param attrAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0xE1CE83A7, version = 150, checkInsideInterrupt = true)
    public int sceMpegGetAtracAu(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer streamAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.out) TPointer auAddr, @BufferInfo(usage=Usage.out) @CanBeNull TPointer32 attrAddr) {
    	TPointer data = mpeg.getPointer();
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMpegGetAtracAu dataStreamAddr=%s", dataStreamAddr));
    	}

    	SceMpegStreamPacketInfo packetInfo = new SceMpegStreamPacketInfo();

		SceMpegAu au = new SceMpegAu();
		au.read(auAddr);

		int result;
    	while (true) {
    		result = getFirstStreamPacket(dataStreamAddr, packetInfo);
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("sceMpegGetAtracAu stream packet 0x%X: %s", result, packetInfo));
    		}

    		if (result < 0) {
    			result = ERROR_MPEG_NO_DATA;
    			break;
    		}

			if (streamAddr.getValue32(44) == 0) {
				au.esSize = 0;
			}

			if (streamAddr.getValue32(44) == 0 || streamAddr.getValue32(56) != 0) {
				while (true) {
		    		result = getFirstStreamPacket(dataStreamAddr, packetInfo);
		    		if (result < 0) {
		    			au.write(auAddr);
		    			return ERROR_MPEG_NO_DATA;
		    		}

		    		if ((streamAddr.getValue32(24) & 0x1) == 0) {
		    			au.pts = UNKNOWN_TIMESTAMP;
		    			au.dts = UNKNOWN_TIMESTAMP;
		    			break;
		    		}

		    		TPointer dataAddr = new TPointer(data.getMemory(), packetInfo.packetDataAddr);
					short value = (short) endianSwap16(dataAddr.getUnalignedValue16(2));
					streamAddr.setValue16(28, value);
					if (value != -1) {
						au.pts = packetInfo.pts;
						au.dts = packetInfo.dts;
						streamAddr.setValue32(24, 0);
						break;
					}
					dequeueStreamPackets(dataStreamAddr, 1);
				}
				au.write(auAddr);

				if (streamAddr.getValue32(56) == 0) {
					streamAddr.setValue32(56, 4);
				}
				int sourceOffset = streamAddr.getValue16(28) + 4;
				int length = packetInfo.packetDataLength - sourceOffset;
				TPointer destinationAddr = new TPointer(streamAddr, 60 + (4 - streamAddr.getValue32(56)));
				for (int packetIndex = 1; true; packetIndex++) {
					if (length >= streamAddr.getValue32(56)) {
						length = streamAddr.getValue32(56);
						destinationAddr.memcpy(packetInfo.packetDataAddr + sourceOffset, length);
						if (log.isTraceEnabled()) {
							log.trace(String.format("sceMpegGetAtracAu memcpy(%s, 0x%08X, 0x%X)", destinationAddr, packetInfo.packetDataAddr, length));
						}
						streamAddr.setValue32(56, 0);
						int audioFrameLength = ((endianSwap16(streamAddr.getUnalignedValue16(60 + 2)) & 0x3FF) << 3) + 8;
						streamAddr.setValue32(36, audioFrameLength - streamAddr.getValue32(44) + 8);
						break;
					}
					destinationAddr.memcpy(packetInfo.packetDataAddr + sourceOffset, length);
					if (log.isTraceEnabled()) {
						log.trace(String.format("sceMpegGetAtracAu memcpy(%s, 0x%08X, 0x%X)", destinationAddr, packetInfo.packetDataAddr, length));
					}
					destinationAddr.add(length);
					streamAddr.setValue32(56, streamAddr.getValue32(56) - length);

					result = getStreamPacket(dataStreamAddr, packetIndex, packetInfo);
					if (result < 0) {
						streamAddr.setValue32(36, 4);
						streamAddr.setValue32(52, 1);
						break;
					}
					sourceOffset = 4;
					length = packetInfo.packetDataLength - 4;
				}
			}

			result = copyStreamData(data, streamAddr, auAddr, 1);

			if (attrAddr.isNotNull() && result == 0) {
				attrAddr.setValue(au.esBuffer + 8);
			}

			if (result != 0 || (streamAddr.getValue32(48) & 0x4) == 0) {
				break;
			}
			streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x4);
    	}

		return result;
    }

    /**
     * sceMpegFlushStream
     * 
     * @param mpeg
     * @param streamAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x500F0429, version = 150, checkInsideInterrupt = true)
    public int sceMpegFlushStream(@CheckArgument("checkMpeg") TPointer32 mpeg, TPointer streamAddr) {
    	TPointer data = mpeg.getPointer();
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	int channelCode = streamAddr.getValue32(0);
    	int streamType = -1;
    	for (int i = 0; i < streamTypeDescriptors.length; i++) {
    		if (streamTypeDescriptors[i].isMatching(channelCode)) {
    			streamType = i;
    			break;
    		}
    	}

    	if (streamType < 0 || streamType >= streamDataOffsets.length) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (dataStreamAddr.getValue32(8) != -1) {
    		dataStreamAddr.setValue32(0, -1);
    		dataStreamAddr.setValue32(4, -1);
    		dataStreamAddr.setValue32(12, 0);
    		if (streamAddr.getValue32(8) != -1) {
    			TPointer dataStream6Addr = streamAddr.getPointer(8);
    			dataStream6Addr.setValue32(0, -1);
    			dataStream6Addr.setValue32(4, -1);
    			dataStream6Addr.setValue32(12, 0);
    		}
    	}

    	streamAddr.setValue32(16, 0x7FFF);
    	streamAddr.setValue32(20, streamDataOffsets[streamType]);
    	streamAddr.setValue32(24, 1);
    	streamAddr.setValue32(32, 0);
    	streamAddr.setValue32(40, 0);
    	streamAddr.setValue32(44, 0);
    	streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x4);
    	streamAddr.setValue32(52, 0);
    	streamAddr.setValue32(56, 0);
    	streamAddr.setValue32(64, 0);

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
    public int sceMpegFlushAllStream(@CheckArgument("checkMpeg") TPointer32 mpeg) {
    	TPointer data = mpeg.getPointer();

		TPointer dataStreamAddr = data.getPointer(24);
    	for (int i = 0; i < MAX_STREAMS; i++) {
    		if (dataStreamAddr.getValue32(8) != -1) {
    			int result = sceMpegFlushStream(mpeg, dataStreamAddr.getPointer(8));
    			if (result != 0) {
    				return result;
    			}
    		}

    		// Next data stream
    		dataStreamAddr.add(16);
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
    public int sceMpegAvcDecode(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.inout) TPointer auAddr, int frameWidth, @CanBeNull TPointer32 bufferAddr, @BufferInfo(usage=Usage.out) TPointer32 gotFrameAddr) {
    	TPointer data = mpeg.getPointer();
    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);

    	TPointer videocodecBuffer = data.getPointer(1728);
    	videocodecBuffer.setValue32(36, getEsBufferAddress(data, au.esBuffer) & 0x0FFFFFFF);
    	videocodecBuffer.setValue32(40, au.esSize);

    	IAction afterVideocodecAction = new AfterVideocodecDecodeAction(data, auAddr, frameWidth, bufferAddr, gotFrameAddr);
    	return Modules.sceVideocodecModule.hleVideocodecDecode(videocodecBuffer, 0, afterVideocodecAction);
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
    public int sceMpegAvcDecodeDetail(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=52, usage=Usage.out) TPointer detailPointer) {
    	TPointer data = mpeg.getPointer();

    	final int imageIndex = 0;
    	TPointer videocodecBuffer = data.getPointer(1728);
    	detailPointer.setValue32(0, videocodecBuffer.getValue32(8)); // decode result
    	TPointer videocodecBuffer2 = videocodecBuffer.getPointer(16);
    	int numberOfDecodedImages = videocodecBuffer2.getValue32(32);
    	if (numberOfDecodedImages > 0 && numberOfDecodedImages > imageIndex) {
        	TPointer mpegAvcYuvStruct = videocodecBuffer.getPointer(44);
        	mpegAvcYuvStruct.add(imageIndex * 44);
    		detailPointer.setValue32(4, mpegAvcYuvStruct.getValue32(32));
    		detailPointer.setValue32(8, videocodecBuffer2.getValue32(8)); // frameWidth
    		detailPointer.setValue32(12, videocodecBuffer2.getValue32(12)); // frameHeight
    		detailPointer.setValue32(16, videocodecBuffer2.getValue32(16)); // Frame crop rect (left)
    		detailPointer.setValue32(20, videocodecBuffer2.getValue32(20)); // Frame crop rect (right)
    		detailPointer.setValue32(24, videocodecBuffer2.getValue32(24)); // Frame crop rect (top)
    		detailPointer.setValue32(28, videocodecBuffer2.getValue32(28)); // Frame crop rect (bottom)
    		detailPointer.setValue32(32, numberOfDecodedImages); 
    		detailPointer.setValue32(36, videocodecBuffer2.getValue32(36));
    		detailPointer.setValue32(40, videocodecBuffer.getValue32(48));
    		detailPointer.setValue32(44, mpegAvcYuvStruct.getValue32(36));
    		detailPointer.setValue32(48, mpegAvcYuvStruct.getValue32(40));
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
    public int sceMpegAvcDecodeMode(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.in) TPointer32 modeAddr) {
    	TPointer data = mpeg.getPointer();

        int mode = modeAddr.getValue(0);
        if (mode != -1) {
        	if (mode < 0 || mode > 2) {
        		return ERROR_MPEG_INVALID_VALUE;
        	}
        	TPointer videocodecBuffer = data.getPointer(1728);
        	videocodecBuffer.setValue32(52, mode);
        }

        int pixelMode = modeAddr.getValue(4);
        if (pixelMode != -1) {
        	int internalPixelMode;
        	switch (pixelMode) {
        		case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650 : internalPixelMode = 1; break;
        		case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551: internalPixelMode = 2; break;
        		case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: internalPixelMode = 3; break;
        		case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: internalPixelMode = 0; break;
        		default: return ERROR_MPEG_INVALID_VALUE;
        	}
        	Modules.sceMpegbaseModule.sceMpegbase_0530BE4E(internalPixelMode);
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
    public int sceMpegAvcDecodeStop(@CheckArgument("checkMpeg") TPointer32 mpeg, int frameWidth, @BufferInfo(usage=Usage.in) @CanBeNull TPointer32 bufferAddr, @BufferInfo(usage=Usage.out) TPointer32 gotFrameAddr) {
    	TPointer data = mpeg.getPointer();

    	int result = sceMpegAvcDecodeFlush(mpeg);
    	if (result != 0) {
    		return result;
    	}

    	TPointer videocodecBuffer2 = data.getPointer(1732);
    	TPointer mpegAvcYuvStruct = data.getPointer(1736);

    	int numberOfDecodedImages = videocodecBuffer2.getValue32(32);
    	gotFrameAddr.setValue(numberOfDecodedImages);

    	int mode = data.getValue32(1708);
    	if (mode < 3 || mode > 6) {
	    	for (int i = 0; i < numberOfDecodedImages; i++) {
	            SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
	            mp4AvcCscStruct.height = videocodecBuffer2.getValue32(12) >> 4;
	    		mp4AvcCscStruct.width = videocodecBuffer2.getValue32(8) >> 4;
	    		mp4AvcCscStruct.mode0 = 0;
	    		mp4AvcCscStruct.mode1 = 0;
	    		mp4AvcCscStruct.buffer0 = mpegAvcYuvStruct.getValue32(0);
	    		mp4AvcCscStruct.buffer1 = mpegAvcYuvStruct.getValue32(4);
	    		mp4AvcCscStruct.buffer2 = mpegAvcYuvStruct.getValue32(8);
	    		mp4AvcCscStruct.buffer3 = mpegAvcYuvStruct.getValue32(12);
	    		mp4AvcCscStruct.buffer4 = mpegAvcYuvStruct.getValue32(16);
	    		mp4AvcCscStruct.buffer5 = mpegAvcYuvStruct.getValue32(20);
	    		mp4AvcCscStruct.buffer6 = mpegAvcYuvStruct.getValue32(24);
	    		mp4AvcCscStruct.buffer7 = mpegAvcYuvStruct.getValue32(28);
	    		TPointer buffer = bufferAddr.getPointer(i * 4);
	    		if (buffer.isNull()) {
	    			return ERROR_MPEG_INVALID_VALUE;
	    		}
	    		result = Modules.sceMpegbaseModule.hleMpegBaseCscAvc(buffer, 0, frameWidth, mp4AvcCscStruct);
	        	if (result != 0) {
	        		break;
	        	}

	        	if (log.isTraceEnabled()) {
	        		log.trace(String.format("sceMpegAvcDecode numberOfDecodedImages=%d, buffer=%s", numberOfDecodedImages, buffer));
	        	}

	        	mpegAvcYuvStruct.add(44);
	    	}
    	}

    	return result;
    }

    /**
     * sceMpegAvcDecodeFlush
     * 
     * @param mpeg
     * 
     * @return
     */
    @HLEFunction(nid = 0x4571CC64, version = 150, checkInsideInterrupt = true)
    public int sceMpegAvcDecodeFlush(@CheckArgument("checkMpeg") TPointer32 mpeg) {
    	TPointer data = mpeg.getPointer();
    	TPointer videocodecBuffer = data.getPointer(1728);

    	return Modules.sceVideocodecModule.sceVideocodecStop(videocodecBuffer, 0);
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
    public int sceMpegAvcQueryYCbCrSize(@CheckArgument("checkMpeg") TPointer32 mpeg, int mode, int width, int height, @BufferInfo(usage=Usage.out) TPointer32 resultAddr) {
    	resultAddr.setValue(0);

    	int size;
    	switch (mode) {
    		case 1:
    	        if ((width & 15) != 0 || (height & 15) != 0 || width > 480 || width <= 16 || height > 272 || height <= 0) {
    	        	return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
    	        }
    	        size = YCBCR_DATA_OFFSET + (getYCbCrSize1(width, height) + getYCbCrSize2(width, height)) * 3;
    			break;
    		case 2:
    			size = 128;
    			break;
    		case 3:
    			size = (width * height * 3) >> 1;
    			break;
			default:
				return ERROR_MPEG_INVALID_VALUE;
    	}

    	// Write the size of the buffer used by sceMpegAvcDecodeYCbCr
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
    public int sceMpegAvcInitYCbCr(@CheckArgument("checkMpeg") TPointer32 mpeg, int mode, int width, int height, TPointer yCbCrBuffer) {
    	switch (mode) {
    		case 1:
    			TPointer ptr = new TPointer(yCbCrBuffer);
        		int size1 = getYCbCrSize1(width, height);
        		int size2 = getYCbCrSize2(width, height);
        		ptr.clear(YCBCR_DATA_OFFSET);
        		ptr.add(YCBCR_DATA_OFFSET);
    			yCbCrBuffer.setPointer(16, ptr);
    			ptr.add(size1);
    			yCbCrBuffer.setPointer(24, ptr);
    			ptr.add(size1);
    			yCbCrBuffer.setPointer(32, ptr);
    			size1 >>= 1;
				ptr.add(size1);
    			yCbCrBuffer.setPointer(40, ptr);
    			ptr.add(size1);
    			yCbCrBuffer.setPointer(20, ptr);
    			ptr.add(size2);
    			yCbCrBuffer.setPointer(28, ptr);
    			ptr.add(size2);
    			yCbCrBuffer.setPointer(36, ptr);
    			size2 >>= 1;
    			ptr.add(size2);
    			yCbCrBuffer.setPointer(44, ptr);
        		break;
    		case 2:
        		yCbCrBuffer.clear(YCBCR_DATA_OFFSET);
    			yCbCrBuffer.setValue32(16, mode);
    			yCbCrBuffer.setValue32(20, mode);
    			yCbCrBuffer.setValue32(24, mode);
    			yCbCrBuffer.setValue32(28, mode);
    			yCbCrBuffer.setValue32(32, mode);
    			yCbCrBuffer.setValue32(36, mode);
    			yCbCrBuffer.setValue32(40, mode);
    			yCbCrBuffer.setValue32(44, mode);
    			break;
    		case 3:
        		int length = width * height;
        		yCbCrBuffer.memset((byte) 0x10, length);
        		yCbCrBuffer.add(length);
        		int length4 = length >> 2;
        		yCbCrBuffer.memset((byte) 0x80, length4);
        		yCbCrBuffer.add(length4);
        		yCbCrBuffer.memset((byte) 0x80, length4);
        		break;
			default:
				return ERROR_MPEG_INVALID_VALUE;
    	}

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
    public int sceMpegAvcDecodeYCbCr(@CheckArgument("checkMpeg") TPointer32 mpeg, TPointer auAddr, TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
    	TPointer data = mpeg.getPointer();
    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);

    	TPointer videocodecBuffer = data.getPointer(1728);
    	videocodecBuffer.setValue32(36, getEsBufferAddress(data, au.esBuffer) & 0x0FFFFFFF);
    	videocodecBuffer.setValue32(40, au.esSize);

    	IAction afterVideocodecAction = new AfterVideocodecDecodeYCbCrAction(data, auAddr, bufferAddr, gotFrameAddr);
    	return Modules.sceVideocodecModule.hleVideocodecDecode(videocodecBuffer, 0, afterVideocodecAction);
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
    public int sceMpegAvcDecodeStopYCbCr(@CheckArgument("checkMpeg") TPointer32 mpeg, @CanBeNull TPointer32 bufferAddr, TPointer32 gotFrameAddr) {
    	TPointer data = mpeg.getPointer();

    	int result = sceMpegAvcDecodeFlush(mpeg);
    	if (result != 0) {
    		return result;
    	}

    	return completeAvcDecodeYCbCr(data, bufferAddr, gotFrameAddr);
    }

    /**
     * sceMpegAvcCsc
     * 
     * sceMpegAvcDecodeYCbCr() is performing the video decoding and
     * sceMpegAvcCsc() is transforming the YCbCr image into ABGR.
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
    public int sceMpegAvcCsc(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=52, usage=Usage.in) TPointer sourceAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer32 rangeAddr, int frameWidth, TPointer destAddr) {
    	TPointer data = mpeg.getPointer();

    	SceMp4AvcCscStruct sceMp4AvcCscStruct = new SceMp4AvcCscStruct();
    	sceMp4AvcCscStruct.height = sourceAddr.getValue32(0);
    	sceMp4AvcCscStruct.width = sourceAddr.getValue32(4);
    	sceMp4AvcCscStruct.mode0 = sourceAddr.getValue32(8);
    	sceMp4AvcCscStruct.mode1 = sourceAddr.getValue32(12);
    	if (sourceAddr.getValue32(16) == 2) {
        	TPointer mpegAvcYuvStruct = data.getPointer(1736);
        	int index = sourceAddr.getValue32(48);
        	mpegAvcYuvStruct.add(index * 44);
        	sceMp4AvcCscStruct.buffer0 = mpegAvcYuvStruct.getValue32( 0) + sceMpegAvcCscOffsets[0];
        	sceMp4AvcCscStruct.buffer1 = mpegAvcYuvStruct.getValue32( 4) + sceMpegAvcCscOffsets[1];
        	sceMp4AvcCscStruct.buffer2 = mpegAvcYuvStruct.getValue32( 8) + sceMpegAvcCscOffsets[0];
        	sceMp4AvcCscStruct.buffer3 = mpegAvcYuvStruct.getValue32(12) + sceMpegAvcCscOffsets[1];
        	sceMp4AvcCscStruct.buffer4 = mpegAvcYuvStruct.getValue32(16) + sceMpegAvcCscOffsets[2];
        	sceMp4AvcCscStruct.buffer5 = mpegAvcYuvStruct.getValue32(20) + sceMpegAvcCscOffsets[3];
        	sceMp4AvcCscStruct.buffer6 = mpegAvcYuvStruct.getValue32(24) + sceMpegAvcCscOffsets[2];
        	sceMp4AvcCscStruct.buffer7 = mpegAvcYuvStruct.getValue32(28) + sceMpegAvcCscOffsets[3];
    	} else {
    		sceMp4AvcCscStruct.buffer0 = sourceAddr.getValue32(16) + sceMpegAvcCscOffsets[0];
    		sceMp4AvcCscStruct.buffer1 = sourceAddr.getValue32(20) + sceMpegAvcCscOffsets[1];
    		sceMp4AvcCscStruct.buffer2 = sourceAddr.getValue32(24) + sceMpegAvcCscOffsets[0];
    		sceMp4AvcCscStruct.buffer3 = sourceAddr.getValue32(28) + sceMpegAvcCscOffsets[1];
    		sceMp4AvcCscStruct.buffer4 = sourceAddr.getValue32(32) + sceMpegAvcCscOffsets[2];
    		sceMp4AvcCscStruct.buffer5 = sourceAddr.getValue32(36) + sceMpegAvcCscOffsets[3];
    		sceMp4AvcCscStruct.buffer6 = sourceAddr.getValue32(40) + sceMpegAvcCscOffsets[2];
    		sceMp4AvcCscStruct.buffer7 = sourceAddr.getValue32(44) + sceMpegAvcCscOffsets[3];
    	}
    	sceMp4AvcCscStruct.write(sceMpegAvcCscBuffer1);

        int rangeX = rangeAddr.getValue(0);
        int rangeY = rangeAddr.getValue(4);
        int rangeWidth = rangeAddr.getValue(8);
        int rangeHeight = rangeAddr.getValue(12);
        if (((rangeX | rangeY | rangeWidth | rangeHeight) & 0xF) != 0) {
        	return ERROR_MPEG_INVALID_VALUE;
        }

    	TPointer32 sceMpegBaseCscAvcRangeAddr = sceMpegAvcCscBuffer2;
    	sceMpegBaseCscAvcRangeAddr.setValue(0, rangeX >>> 4);
    	sceMpegBaseCscAvcRangeAddr.setValue(4, rangeY >>> 4);
    	sceMpegBaseCscAvcRangeAddr.setValue(8, rangeWidth >>> 4);
    	sceMpegBaseCscAvcRangeAddr.setValue(12, rangeHeight >>> 4);

    	return Modules.sceMpegbaseModule.sceMpegBaseCscAvcRange(destAddr, 0, sceMpegBaseCscAvcRangeAddr, frameWidth, sceMpegAvcCscBuffer1);
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
    public int sceMpegAtracDecode(@CheckArgument("checkMpeg") TPointer32 mpeg, TPointer auAddr, TPointer bufferAddr, int init) {
    	int result;
    	TPointer data = mpeg.getPointer();

    	if (data.getValue32(1708) == 1) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	SceMpegAu au = new SceMpegAu();
    	au.read(auAddr);

		TPointer audiocodecBuffer = data.getPointer(1788);
    	if (init == 1) {
    		audiocodecBuffer.setValue32(12, data.getValue32(1780)); // EDRAM
    		audiocodecBuffer.setValue32(20, 1);
    		audiocodecBuffer.memcpy(40, au.esBuffer + 2, 2);
    		audiocodecBuffer.memcpy(44, au.esBuffer + 4, 4);
    		audiocodecBuffer.setValue32(48, 1);

    		result = Modules.sceAudiocodecModule.sceAudiocodecCheckNeedMem(audiocodecBuffer, PSP_CODEC_AT3PLUS);
    		if (result != 0) {
    			return result;
    		}

    		if (data.getValue32(1784) < audiocodecBuffer.getValue32(16)) {
    			return SceKernelErrors.ERROR_CODEC_AUDIO_UNKNOWN_ERROR;
    		}

    		result = Modules.sceAudiocodecModule.sceAudiocodecInit(audiocodecBuffer, PSP_CODEC_AT3PLUS);
    		if (result != 0) {
    			return result;
    		}
    	}

    	audiocodecBuffer.setValue32(24, au.esBuffer); // input buffer
    	audiocodecBuffer.setPointer(32, bufferAddr); // output buffer

    	return Modules.sceAudiocodecModule.sceAudiocodecDecode(audiocodecBuffer, PSP_CODEC_AT3PLUS);
    }

    /**
     * sceMpegRingbufferQueryMemSize
     * 
     * @param packets
     * 
     * @return
     */
    @HLEFunction(nid = 0xD7A29F46, version = 150, checkInsideInterrupt = true, stackUsage = 0x8)
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
    @HLEFunction(nid = 0x37295ED8, version = 150, checkInsideInterrupt = true, stackUsage = 0x38)
    public int sceMpegRingbufferConstruct(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=48, usage=Usage.out) TPointer ringbufferAddr, int packets, @CanBeNull TPointer data, int size, @CanBeNull TPointer callbackAddr, int callbackArgs) {
        if (size < getSizeFromPackets(packets)) {
            log.warn(String.format("sceMpegRingbufferConstruct insufficient space: size=%d, packets=%d", size, packets));
            return SceKernelErrors.ERROR_MPEG_NO_MEMORY;
        }

        SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer();
        ringbuffer.packets = packets;
        ringbuffer.packetSize = SceMpegRingbuffer.ringbufferPacketSize;
        ringbuffer.data = data.getAddress();
        ringbuffer.callbackAddr = callbackAddr.getAddress();
        ringbuffer.callbackArgs = callbackArgs;
        ringbuffer.dataUpperBound = ringbuffer.data + ringbuffer.packets * ringbuffer.packetSize;
        ringbuffer.gp = RuntimeContext.processor.cpu._gp;
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
    @HLEFunction(nid = 0x13407F13, version = 150, checkInsideInterrupt = true, stackUsage = 0x8)
    public int sceMpegRingbufferDestruct(TPointer ringbufferAddr) {
    	// Nothing to do
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
    public int sceMpegRingbufferPut(@CheckArgument("checkRingbuffer") TPointer ringbufferAddr, int numPackets, int available) {
    	numPackets = Math.min(numPackets, available);
    	if (numPackets <= 0) {
    		return 0;
    	}

    	SceMpegRingbuffer sceMpegRingbuffer = new SceMpegRingbuffer();
    	sceMpegRingbuffer.read(ringbufferAddr);

    	int callbackNumPackets = numPackets;
    	if (sceMpegRingbuffer.packets < sceMpegRingbuffer.packetsWritten + numPackets) {
    		callbackNumPackets = sceMpegRingbuffer.packets - sceMpegRingbuffer.packetsWritten;
    	}
    	int callbackDataAddress = sceMpegRingbuffer.data + sceMpegRingbuffer.packetsWritten * sceMpegRingbuffer.packetSize;
        AfterRingbufferPutCallback afterRingbufferPutCallback = new AfterRingbufferPutCallback(ringbufferAddr, numPackets - callbackNumPackets, getProcessor().cpu._gp);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMpegRingbufferPut calling callback 0x%08X with dataAddress=0x%08X, numPackets=0x%X", sceMpegRingbuffer.callbackAddr, callbackDataAddress, callbackNumPackets));
    	}
        getProcessor().cpu._gp = sceMpegRingbuffer.gp;
    	Modules.ThreadManForUserModule.executeCallback(null, sceMpegRingbuffer.callbackAddr, afterRingbufferPutCallback, false, callbackDataAddress, callbackNumPackets, sceMpegRingbuffer.callbackArgs);

        return afterRingbufferPutCallback.getReturnValue();
    }

    /**
     * sceMpegRingbufferAvailableSize
     * 
     * @param _mpegRingbufferAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0xB5F6DC87, version = 150, checkInsideInterrupt = true)
    public int sceMpegRingbufferAvailableSize(@CheckArgument("checkRingbuffer") TPointer ringbufferAddr) {
    	SceMpegRingbuffer sceMpegRingbuffer = new SceMpegRingbuffer();
    	sceMpegRingbuffer.read(ringbufferAddr);

    	// Ringbuffer already full?
    	if (sceMpegRingbuffer.packets == sceMpegRingbuffer.packetsInRingbuffer) {
    		return 0;
    	}

    	int minPacketIndex = sceMpegRingbuffer.packetsRead;

    	TPointer data = sceMpegRingbuffer.mpeg.getPointer();
		TPointer dataStreamAddr = data.getPointer(24);
    	for (int i = 0; i < MAX_STREAMS; i++) {
    		if (dataStreamAddr.getValue32(8) != -1 && dataStreamAddr.getValue32(4) != -1) {
    			int size = dataStreamAddr.getPointer(4).getValue32(36) - sceMpegRingbuffer.data;
    			int packetIndex = size / sceMpegRingbuffer.packetSize;
    			if (packetIndex == sceMpegRingbuffer.packetsWritten && size > 0) {
    				minPacketIndex = -1;
    				break;
    			}

    			if (sceMpegRingbuffer.packetsWritten < sceMpegRingbuffer.packetsRead) {
    				if (packetIndex < minPacketIndex) {
    					minPacketIndex = packetIndex;
    				}
    			} else if (sceMpegRingbuffer.packetsWritten < packetIndex) {
    				if (sceMpegRingbuffer.packetsRead >= minPacketIndex || packetIndex < minPacketIndex) {
						minPacketIndex = packetIndex;
    				}
    			} else {
    				if (sceMpegRingbuffer.packetsRead >= minPacketIndex && packetIndex < sceMpegRingbuffer.packetsRead && packetIndex < minPacketIndex) {
    					minPacketIndex = packetIndex;
    				}
    			}
    		}

    		// Next data stream
    		dataStreamAddr.add(16);
    	}

    	if (minPacketIndex == -1) {
    		return 0;
    	}

    	int limit;
		if (sceMpegRingbuffer.packetsWritten < sceMpegRingbuffer.packetsRead || sceMpegRingbuffer.packetsRead >= minPacketIndex) {
			limit = 0;
		} else {
			limit = sceMpegRingbuffer.packets;
		}

    	return sceMpegRingbuffer.packets - sceMpegRingbuffer.packetsInRingbuffer - sceMpegRingbuffer.packetsRead + minPacketIndex - limit;
    }

    /**
     * sceMpegNextAvcRpAu - skip one video frame
     * 
     * @param mpeg
     * @param streamAddr
     * 
     * @return
     */
    @HLEFunction(nid = 0x3C37A7A6, version = 150, checkInsideInterrupt = true)
    public int sceMpegNextAvcRpAu(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer streamAddr) {
    	TPointer data = mpeg.getPointer();
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMpegNextAvcRpAu dataStreamAddr=%s", dataStreamAddr));
    	}

    	SceMpegStreamPacketInfo packetInfo = new SceMpegStreamPacketInfo();
		int result = getFirstStreamPacket(streamAddr.getPointer(8), packetInfo);
		int dataStream6DataAddr;
    	if (result != 0) {
    		result = SceKernelErrors.ERROR_MPEG_NO_NEXT_DATA;
    		dataStream6DataAddr = 0;
    	} else {
    		dataStream6DataAddr = packetInfo.packetDataAddr;
    	}

		int count = dataStreamAddr.getValue32(12);
		if (count > 0) {
			SceMpegRingbuffer sceMpegRingbuffer = new SceMpegRingbuffer();
			sceMpegRingbuffer.read(data.getPointer(16));

	    	int processed = 0;
	    	for (int i = 0; i < count; i++, processed++) {
	    		getFirstStreamPacket(dataStreamAddr, packetInfo);
	    		if (dataStream6DataAddr != 0) {
	    			if (compareAddresses(sceMpegRingbuffer, dataStream6DataAddr, packetInfo.packetDataAddr) == 1) {
	    				break;
	    			}
	    		}
	    		dequeueStreamPackets(dataStreamAddr, 1);
	    	}

	    	if (log.isTraceEnabled()) {
	    		log.trace(String.format("sceMpegNextAvcRpAu removed %d packets from data stream %s", processed, dataStreamAddr));
	    	}
		}

    	streamAddr.setValue32(16, 0x7FFF);
    	streamAddr.setValue32(44, 0);
		streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x4);

		return result;
    }

    @HLEFunction(nid = 0x01977054, version = 150)
    public int sceMpegGetUserdataAu(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer streamAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.out) TPointer auAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.out) @CanBeNull TPointer headerAddr) {
    	TPointer data = mpeg.getPointer();
    	TPointer dataStreamAddr = getDataStreamAddress(data, streamAddr);
    	if (dataStreamAddr == null) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceMpegGetUserdataAu dataStreamAddr=%s", dataStreamAddr));
    	}

    	SceMpegStreamPacketInfo packetInfo = new SceMpegStreamPacketInfo();

		SceMpegAu au = new SceMpegAu();
		au.read(auAddr);

		int result;
    	while (true) {
    		result = getFirstStreamPacket(dataStreamAddr, packetInfo);
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("sceMpegGetUserdataAu stream packet 0x%X: %s", result, packetInfo));
    		}

    		if (result < 0) {
    			result = ERROR_MPEG_NO_DATA;
    			break;
    		}

			if (streamAddr.getValue32(56) != 0 || (streamAddr.getValue32(44) == 0 && streamAddr.getValue32(52) != 1)) {
				if (streamAddr.getValue32(44) == 0) {
					au.esSize = 0;
					while (true) {
			    		result = getFirstStreamPacket(dataStreamAddr, packetInfo);
			    		if (result < 0) {
			    			au.write(auAddr);
			    			return ERROR_MPEG_NO_DATA;
			    		}

			    		if (packetInfo.dts != UNKNOWN_TIMESTAMP || packetInfo.pts != UNKNOWN_TIMESTAMP) {
				    		if ((streamAddr.getValue32(24) & 0x1) != 0) {
				    			au.pts = packetInfo.pts;
				    			au.dts = packetInfo.dts;
				    			streamAddr.setValue32(24, 0);
				    			streamAddr.setValue16(28, (short) 0);
				    		}
			    			break;
			    		}
	
						dequeueStreamPackets(dataStreamAddr, 1);
					}
					au.write(auAddr);
				}

				result = getFirstStreamPacket(dataStreamAddr, packetInfo);
	    		if (result < 0) {
	    			return ERROR_MPEG_NO_DATA;
	    		}

				if (streamAddr.getValue32(56) == 0) {
					streamAddr.setValue32(56, 8);
				}
				int sourceOffset = streamAddr.getValue16(28) + 2;
				int length = packetInfo.packetDataLength - sourceOffset;
				final byte[] header = new byte[8];
				int startHeaderOffset = 8 - streamAddr.getValue32(56);
				int headerOffset = startHeaderOffset;
				TPointer sourceAddr = new TPointer(data.getMemory(), packetInfo.packetDataAddr);
				while (true) {
					if (length >= streamAddr.getValue32(56)) {
						length = streamAddr.getValue32(56);
						sourceAddr.getArray8(sourceOffset, header, headerOffset, length);

						streamAddr.setValue16(28, (short) (streamAddr.getValue16(28) + streamAddr.getValue16(56)));
						streamAddr.setValue32(44, streamAddr.getValue32(44) + length);
						streamAddr.setValue32(56, 0);
						break;
					}
					sourceAddr.getArray8(sourceOffset, header, headerOffset, length);
					headerOffset += length;
					streamAddr.setValue32(56, streamAddr.getValue32(56) - length);

					result = getStreamPacket(dataStreamAddr, 1, packetInfo);
					if (result < 0) {
						streamAddr.setValue32(52, 1);
						break;
					}

					streamAddr.setValue32(44, streamAddr.getValue32(44) + length);
					length = packetInfo.packetDataLength - 2;

					dequeueStreamPackets(dataStreamAddr, 1);
				}

				int n = 8 - streamAddr.getValue32(56);
				if (startHeaderOffset < 4) {
					TPointer destinationAddr = new TPointer(streamAddr, 60 + startHeaderOffset);
					if (n < 5) {
						length = n - startHeaderOffset;
					} else {
						length = 4 - startHeaderOffset;
					}
					destinationAddr.setArray(0, header, startHeaderOffset, length);
				}

				if (n >= 5 && headerAddr.isNotNull()) {
					int offset = Math.max(startHeaderOffset, 4);
					length = n - offset;
					headerAddr.setArray(offset, header, offset, length);
				}

				if (streamAddr.getValue32(56) == 0) {
					streamAddr.setValue32(36, endianSwap32(streamAddr.getValue32(60)) - 4);
					if (headerAddr.isNotNull()) {
						headerAddr.memcpy(0, streamAddr.getAddress() + 60, 4);
						headerAddr.setArray(4, header, 4, 4);
					}
				}
			}

			result = copyStreamData(data, streamAddr, auAddr, 3);

			if (result != 0 || (streamAddr.getValue32(48) & 0x4) == 0) {
				break;
			}
			streamAddr.setValue32(48, streamAddr.getValue32(48) & ~0x4);
    	}

		return result;
    }

    @HLEFunction(nid = 0xC45C99CC, version = 150)
    public int sceMpegQueryUserdataEsSize(@CheckArgument("checkMpeg") TPointer32 mpeg, TPointer32 esSizeAddr, TPointer32 outSizeAddr) {
    	esSizeAddr.setValue(MPEG_DATA_ES_SIZE);
    	outSizeAddr.setValue(MPEG_DATA_ES_OUTPUT_SIZE);

    	return 0;
    }

    @HLEFunction(nid = 0x0558B075, version = 150)
    public int sceMpegAvcCopyYCbCr(@CheckArgument("checkMpeg") TPointer32 mpeg, TPointer destinationAddr, TPointer sourceAddr) {
    	TPointer data = mpeg.getPointer();

    	int mode = destinationAddr.getValue32(16);
    	if (mode == 2) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	return copyYCbCr(data, destinationAddr, sourceAddr, -1);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x11F95CF1, version = 150)
    public int sceMpegGetAvcNalAu(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer mp4AvcNalStructAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.out) TPointer auAddr) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x921FCCCF, version = 150)
    public int sceMpegGetAvcEsAu() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6F314410, version = 150)
    public int sceMpegAvcDecodeGetDecodeSEI(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(usage=Usage.out) TPointer32 decodeSEIAddr) {
    	decodeSEIAddr.setValue(0);
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAB0E9556, version = 150)
    public int sceMpegAvcDecodeDetailIndex(@CheckArgument("checkMpeg") TPointer32 mpeg, int index, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=52, usage=Usage.out) TPointer32 detail) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCF3547A2, version = 150)
    public int sceMpegAvcDecodeDetail2(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(usage=Usage.out) TPointer32 detail) {
        return 0;
    }

    @HLEFunction(nid = 0xF5E7EA31, version = 150)
    public int sceMpegAvcConvertToYuv420(@CheckArgument("checkMpeg") TPointer32 mpeg, TPointer yuv420Buffer, TPointer sourceAddr, int unknown) {
    	TPointer data = mpeg.getPointer();

    	final int mode = 3;
    	sceMpegAvcConvertToYuv420Buffer.setValue32(12, unknown);
    	sceMpegAvcConvertToYuv420Buffer.setValue32(16, mode);
    	sceMpegAvcConvertToYuv420Buffer.setPointer(20, yuv420Buffer);

    	return copyYCbCr(data, sceMpegAvcConvertToYuv420Buffer, sourceAddr, -1);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1CE4950, version = 150)
    public int sceMpegAvcCscMode(@CheckArgument("checkMpeg") TPointer32 mpeg, @BufferInfo(usage=Usage.in) TPointer32 modeAddr) {
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

    @HLEFunction(nid = 0x769BEBB6, version = 250)
    public int sceMpegRingbufferQueryPackNum(int memorySize) {
        return getPacketsFromSize(memorySize);
    }

    @HLEFunction(nid = 0x63B9536A, version = 600)
    public int sceMpegAvcResourceGetAvcDecTopAddr() {
    	// Has no parameters
    	if ((globalFlags & 0x4) == 0) {
    		return 0;
    	}

    	int addr = sceMpegAvcResource.getValue32(4) & 0xFFC00000;

    	return addr;
    }

    @HLEFunction(nid = 0x8160A2FE, version = 600)
    public int sceMpegAvcResourceFinish() {
    	if ((globalFlags & 0x4) == 0) {
    		return ERROR_MPEG_UNKNOWN_STREAM_ID;
    	}

    	int fplUid = sceMpegAvcResource.getValue32(0);
    	Modules.ThreadManForUserModule.sceKernelFreeFpl(fplUid, sceMpegAvcResource.getPointer(4));

    	int result = Modules.ThreadManForUserModule.sceKernelDeleteFpl(fplUid);

    	sceMpegAvcResource.setValue32(0, 0);
    	sceMpegAvcResource.setValue32(4, 0);

    	globalFlags &= ~0x4;

    	return result;
    }

    @HLEFunction(nid = 0xAF26BB01, version = 600)
    public int sceMpegAvcResourceGetAvcEsBuf() {
    	// Has no parameters
    	if ((globalFlags & 0x4) == 0) {
    		return 0;
    	}

    	return sceMpegAvcResource.getValue32(4);
    }

    @HLEFunction(nid = 0xFCBDB5AD, version = 600)
    public int sceMpegAvcResourceInit(int unknown) {
    	if (unknown != 1) {
    		return ERROR_MPEG_INVALID_VALUE;
    	}

    	if ((globalFlags & 0x4) != 0) {
    		return ERROR_MPEG_ALREADY_USED;
    	}

    	int result = Modules.ThreadManForUserModule.sceKernelCreateFpl(new PspString("SceMpegAvcResource"), USER_PARTITION_ID, 0, 0x20000, 1, TPointer.NULL);
    	if (result < 0) {
    		return result;
    	}
    	int fplUid = result;

    	TPointer32 dataAddr = new TPointer32(sceMpegAvcResource.getMemory(), sceMpegAvcResource.getAddress() + 4);
    	result = Modules.ThreadManForUserModule.sceKernelAllocateFpl(fplUid, dataAddr, TPointer32.NULL);
    	if (result < 0) {
    		Modules.ThreadManForUserModule.sceKernelDeleteFpl(fplUid);
    		return result;
    	}
    	sceMpegAvcResource.setValue32(0, fplUid);

    	globalFlags |= 0x4;

    	return 0;
    }
}
