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

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AVC_INVALID_VALUE;
import static jpcsp.HLE.modules.sceMpegbase.getIntBuffer;
import static jpcsp.HLE.modules.sceMpegbase.releaseIntBuffer;
import static jpcsp.util.Utilities.alignUp;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.modules.sceMpegbase.YCbCrImageState;
import jpcsp.media.codec.CodecFactory;
import jpcsp.media.codec.IVideoCodec;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.scheduler.DelayThreadAction;
import jpcsp.scheduler.UnblockThreadAction;
import jpcsp.util.Utilities;

public class sceVideocodec extends HLEModule {
    public static Logger log = Modules.getLogger("sceVideocodec");
    public static final int videocodecBufferSize = 96;
    private static final int videocodecDecodeDelay = 4000;
    // Based on JpcspTrace tests, sceVideocodecDelete delays for 40ms
    public static final int videocodecDeleteDelay = 40000;
    public static final int EDRAM_MEMORY_MASK = 0x03FFFFFF;
    public static final int VIDEOCODEC_OPEN_TYPE0_UNKNOWN24 = 0x3C2C;
    public static final int VIDEOCODEC_OPEN_TYPE0_UNKNOWN0 = 0x1F6400;
    public static final int VIDEOCODEC_OPEN_TYPE0_UNKNOWN4 = 0x15C00;
    public static final int VIDEOCODEC_OPEN_TYPE1_UNKNOWN24 = 0x264C;
    public static final int VIDEOCODEC_OPEN_TYPE1_UNKNOWN32 = 0xB69E3;
    protected SysMemInfo memoryInfo;
    protected SysMemInfo edramInfo;
    protected int frameCount;
    protected int bufferY1;
    protected int bufferY2;
    protected int bufferCr1;
    protected int bufferCr2;
    protected int bufferCb1;
    protected int bufferCb2;
    protected final TPointer buffers[][] = new TPointer[4][8];
    protected TPointer defaultBufferUnknown1;
    protected TPointer defaultBufferUnknown2;
    protected IVideoCodec videoCodec;
    private VideocodecDecoderThread videocodecDecoderThread;
    private int[] videocodecExtraData;

	@Override
	public void start() {
    	videocodecExtraData = null;

		super.start();
	}

	private class VideocodecDecoderThread extends Thread {
    	private volatile boolean exit = false;
    	private volatile boolean done = false;
    	private Semaphore sema = new Semaphore(1);
    	private TPointer buffer;
    	private int type;
    	private IAction afterDecodeAction;
    	private int threadUid;
    	private long threadWakeupMicroTime;

    	@Override
		public void run() {
    		setLog4jMDC();

    		while (!exit) {
    			if (waitForTrigger(100) && !exit) {
    				videocodecDecoderStep(buffer, type, afterDecodeAction, threadUid, threadWakeupMicroTime);
    			}
    		}

    		if (log.isDebugEnabled()) {
    			log.debug("Exiting the VideocodecDecoderThread");
    		}
    		done = true;
    	}

    	public void exit() {
    		exit = true;

    		while (!done) {
    			Utilities.sleep(1);
    		}
    	}

    	public void trigger(TPointer buffer, int type, IAction afterDecodeAction, int threadUid, long threadWakeupMicroTime) {
    		this.buffer = buffer;
    		this.type = type;
    		this.afterDecodeAction = afterDecodeAction;
    		this.threadUid = threadUid;
			this.threadWakeupMicroTime = threadWakeupMicroTime;

			trigger();
    	}

    	private void trigger() {
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

    public int[] getVideocodecExtraData() {
		return videocodecExtraData;
	}

	public void setVideocodecExtraData(int[] videocodecExtraData) {
		this.videocodecExtraData = videocodecExtraData;
	}

	public void clearVideocodecExtraData() {
		videocodecExtraData = null;
	}

	public void videocodecDelete() {
    	if (videocodecDecoderThread != null) {
    		videocodecDecoderThread.exit();
    		videocodecDecoderThread = null;
    	}

		clearVideocodecExtraData();

		if (videoCodec != null) {
    		videoCodec = null;
    	}

    	if (memoryInfo != null) {
    		Modules.SysMemUserForUserModule.free(memoryInfo);
    		memoryInfo = null;
    	}

    	for (int i = 0; i < buffers.length; i++) {
    		for (int j = 0; j < buffers[i].length; j++) {
    			buffers[i][j] = null;
    		}
    	}

    	if (edramInfo != null) {
    		Modules.SysMemUserForUserModule.free(edramInfo);
    		edramInfo = null;
    	}
	}

	private int videocodecDecode(Memory mp4Memory, int mp4Data, int mp4Size) {
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceVideocodecDecode mp4Data:%s", Utilities.getMemoryDump(mp4Memory, mp4Data, mp4Size)));
    	}

    	if (videoCodec == null) {
    		videoCodec = CodecFactory.getVideoCodec();
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("videocodecDecode init Codec with extraData=%s", getVideocodecExtraData()));
    		}
    		videoCodec.init(getVideocodecExtraData());
    		clearVideocodecExtraData();
    	}

    	// The H264 video decoder can read up to 2 bytes past the end
    	// of the video data for the read-ahead buffer used for CABAC.
    	// Provide 2 dummy bytes after the video data.
    	int[] mp4Buffer = getIntBuffer(mp4Size + 2);
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(mp4Memory, mp4Data, mp4Size, 1);
    	for (int i = 0; i < mp4Size; i++) {
    		mp4Buffer[i] = memoryReader.readNext();
    	}

    	int result = videoCodec.decode(mp4Buffer, 0, mp4Size);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceVideocodecDecode videoCodec returned 0x%X from 0x%X data bytes", result, mp4Size));
    	}

    	releaseIntBuffer(mp4Buffer);

    	return result;
	}

	public int videocodecDecodeType0(Memory mp4Memory, int mp4Data, int mp4Size, TPointer buffer2, TPointer mpegAvcYuvStruct, TPointer buffer3, TPointer decodeSEI) {
		int result = videocodecDecode(mp4Memory, mp4Data, mp4Size);

    	int frameWidth = videoCodec.getImageWidth();
    	int frameHeight = videoCodec.getImageHeight();

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceVideocodecDecode codec image size %dx%d, frame size %dx%d", videoCodec.getImageWidth(), videoCodec.getImageHeight(), frameWidth, frameHeight));
    	}

    	buffer2.setValue32(8, frameWidth);
    	buffer2.setValue32(12, frameHeight);
    	buffer2.setValue32(28, 1);
    	buffer2.setValue32(32, videoCodec.hasImage()); // Number of decoded images
    	buffer2.setValue32(36, !videoCodec.hasImage());

    	if (videoCodec.hasImage()) {
    		if (buffers[0][0] == null) {
    			int sizeY1 = alignUp(((frameWidth + 16) >> 5) * (frameHeight >> 1) * 16, 0x1FF);
    			int sizeY2 = alignUp((frameWidth >> 5) * (frameHeight >> 1) * 16, 0x1FF);
        		int sizeCr1 = alignUp(((frameWidth + 16) >> 5) * (frameHeight >> 1) * 8, 0x1FF);
        		int sizeCr2 = alignUp((frameWidth >> 5) * (frameHeight >> 1) * 8, 0x1FF);
        		int size = 256 + (sizeY1 + sizeY2 + sizeCr1 + sizeCr2) * 2 * buffers.length;

        		TPointer base;
        		if (mp4Memory == Memory.getInstance()) {
        			memoryInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceVideocodecDecode", SysMemUserForUser.PSP_SMEM_Low, size, 0);
        			base = new TPointer(mp4Memory, memoryInfo.addr);
        		} else {
        			base = new TPointer(mp4Memory, 0x00000000).forceNonNull();
        		}

        		defaultBufferUnknown1 = new TPointer(base);
        		defaultBufferUnknown1.clear(36);

        		defaultBufferUnknown2 = new TPointer(base, 36);
        		defaultBufferUnknown2.clear(32);

        		TPointer yuvBuffersBase = new TPointer(base, 256); // Add 256 to keep aligned
        		TPointer base1 = new TPointer(yuvBuffersBase);
        		TPointer base2 = new TPointer(base1, (sizeY1 + sizeY2) * buffers.length);
        		int step = (sizeY1 + sizeY2 + sizeCr1 + sizeCr2) * buffers.length;
        		for (int i = 0; i < buffers.length; i++) {
        			buffers[i][0] = new TPointer(base1);
        			buffers[i][0].memset((byte) 0x00, sizeY1);
        			buffers[i][1] = new TPointer(buffers[i][0], step);
        			buffers[i][1].memset((byte) 0x00, sizeY1);
        			buffers[i][2] = new TPointer(base1, sizeY1);
        			buffers[i][2].memset((byte) 0x00, sizeY2);
        			buffers[i][3] = new TPointer(buffers[i][2], step);
        			buffers[i][3].memset((byte) 0x00, sizeY2);
        			buffers[i][4] = new TPointer(base2);
        			buffers[i][4].memset((byte) 0x80, sizeCr1);
        			buffers[i][5] = new TPointer(buffers[i][4], step);
        			buffers[i][5].memset((byte) 0x80, sizeCr1);
        			buffers[i][6] = new TPointer(base2, sizeCr1);
        			buffers[i][6].memset((byte) 0x80, sizeCr2);
        			buffers[i][7] = new TPointer(buffers[i][6], step);
        			buffers[i][7].memset((byte) 0x80, sizeCr2);

        			base1.add(sizeY1 + sizeY2);
        			base2.add(sizeCr1 + sizeCr2);

        			if (log.isDebugEnabled()) {
        				for (int j = 0; j < buffers[i].length; j++) {
        					log.debug(String.format("sceVideocodecDecode allocated buffers[%d][%d]=%s", i, j, buffers[i][j]));
        				}
        			}
        		}
        	}

        	int buffersIndex = frameCount % 3;
    		int width = videoCodec.getImageWidth();
    		int height = videoCodec.getImageHeight();

    		int[] luma = getIntBuffer(width * height);
            int[] cb = getIntBuffer((width * height) >> 2);
            int[] cr = getIntBuffer((width * height) >> 2);
        	if (videoCodec.getImage(luma, cb, cr) == 0) {
        		// The PSP is storing the YCbCr information in a non-linear format.
        		// By analyzing the output of sceMpegBaseYCrCbCopy on a real PSP,
        		// the following format for the YCbCr was found:
        		// the image is divided vertically into bands of 32 pixels.
        		// Each band is stored vertically into different buffers.
        		// The Y information is stored as 1 byte per pixel.
        		// The Cb information is stored as 1 byte for a square of four pixels (2x2).
        		// The Cr information is stored as 1 byte for a square of four pixels (2x2).
        		// For a square of four pixels, the one Cb byte is stored first,
        		// followed by the one Cr byte.
        		//
        		// - buffer0:
        		//     storing the Y information of the first block
        		//     of 16 pixels of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=0,y=0),
        		//     16 horizontal pixels are stored sequentially in the buffer,
        		//     followed by 16 pixels of the next next image row (i.e. every 2nd row).
        		//     The rows are stored from the image top to the image bottom.
        		//     [x=0-15,y=0], [x=0-15,y=2], [x=0-15,y=4]...
        		//     [x=32-47,y=0], [x=32-47,y=2], [x=32-47,y=4]...
        		//     [x=64-79,y=0], [x=64-79,y=2], [x=64-79,y=4]...
        		// - buffer1:
        		//     storing the Y information of the second block
        		//     of 16 pixels of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=16,y=0),
        		//     16 horizontal pixels are stored sequentially in the buffer,
        		//     followed by 16 pixels of the next next image row (i.e. every 2nd row).
        		//     The rows are stored from the image top to the image bottom.
        		//     [x=16-31,y=0], [x=16-31,y=2], [x=16-31,y=4]...
        		//     [x=48-63,y=0], [x=48-63,y=2], [x=48-63,y=4]...
        		//     [x=80-95,y=0], [x=80-95,y=2], [x=80-95,y=4]...
        		// - buffer2:
        		//     storing the Y information of the first block of 16 pixels
        		//     of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=0,y=1),
        		//     16 horizontal pixels are stored sequentially in the buffer,
        		//     followed by 16 pixels of the next next image row (i.e. every 2nd row).
        		//     The rows are stored from the image top to the image bottom.
        		//     [x=0-15,y=1], [x=0-15,y=3], [x=0-15,y=5]...
        		//     [x=32-47,y=1], [x=32-47,y=3], [x=32-47,y=5]...
        		//     [x=64-79,y=1], [x=64-79,y=3], [x=64-79,y=5]...
        		// - buffer3:
        		//     storing the Y information of the second block of 16 pixels
        		//     of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=16,y=1),
        		//     16 horizontal pixels are stored sequentially in the buffer,
        		//     followed by 16 pixels of the next next image row (i.e. every 2nd row).
        		//     The rows are stored from the image top to the image bottom.
        		//     [x=16-31,y=1], [x=16-31,y=3], [x=16-31,y=5]...
        		//     [x=48-63,y=1], [x=48-63,y=3], [x=48-63,y=5]...
        		//     [x=80-95,y=1], [x=80-95,y=3], [x=80-95,y=5]...
        		// - buffer4:
        		//     storing the Cb and Cr information of the first block
        		//     of 16 pixels of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=0,y=0),
        		//     8 byte pairs of (Cb,Cr) are stored sequentially in the buffer
        		//     (representing 16 horizontal pixels),
        		//     then the next 3 rows are being skipped,
        		//     and then followed by 8 byte pairs of the next image row (i.e. every 4th row).
        		//     The rows are stored from the image top to the image bottom.
        		//     CbCr[x=0,y=0], CbCr[x=2,y=0], CbCr[x=4,y=0], CbCr[x=6,y=0], CbCr[x=8,y=0], CbCr[x=10,y=0], CbCr[x=12,y=0], CbCr[x=14,y=0]
        		//     CbCr[x=32,y=0], CbCr[x=34,y=0], CbCr[x=36,y=0], CbCr[x=38,y=0], CbCr[x=40,y=0], CbCr[x=42,y=0], CbCr[x=44,y=0], CbCr[x=46,y=0]
        		//     ...
        		//     CbCr[x=0,y=4], CbCr[x=2,y=4], CbCr[x=4,y=4], CbCr[x=6,y=4], CbCr[x=8,y=4], CbCr[x=10,y=4], CbCr[x=12,y=4], CbCr[x=14,y=4]
        		//     CbCr[x=32,y=4], CbCr[x=34,y=4], CbCr[x=36,y=4], CbCr[x=38,y=4], CbCr[x=40,y=4], CbCr[x=42,y=4], CbCr[x=44,y=4], CbCr[x=46,y=4]
        		//     ...
        		// - buffer5:
        		//     storing the Cb and Cr information of the first block
        		//     of 16 pixels of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=0,y=2),
        		//     8 byte pairs of (Cb,Cr) are stored sequentially in the buffer
        		//     (representing 16 horizontal pixels),
        		//     then the next 3 rows are being skipped,
        		//     and then followed by 8 byte pairs of the next image row (i.e. every 4th row).
        		//     The rows are stored from the image top to the image bottom.
        		//     CbCr[x=0,y=2], CbCr[x=2,y=2], CbCr[x=4,y=2], CbCr[x=6,y=2], CbCr[x=8,y=2], CbCr[x=10,y=2], CbCr[x=12,y=2], CbCr[x=14,y=2]
        		//     CbCr[x=32,y=2], CbCr[x=34,y=2], CbCr[x=36,y=2], CbCr[x=38,y=2], CbCr[x=40,y=2], CbCr[x=42,y=2], CbCr[x=44,y=2], CbCr[x=46,y=2]
        		//     ...
        		//     CbCr[x=0,y=6], CbCr[x=2,y=6], CbCr[x=4,y=6], CbCr[x=6,y=6], CbCr[x=8,y=6], CbCr[x=10,y=6], CbCr[x=12,y=6], CbCr[x=14,y=6]
        		//     CbCr[x=32,y=6], CbCr[x=34,y=6], CbCr[x=36,y=6], CbCr[x=38,y=6], CbCr[x=40,y=6], CbCr[x=42,y=6], CbCr[x=44,y=6], CbCr[x=46,y=6]
        		//     ...
        		// - buffer6:
        		//     storing the Cb and Cr information of the second block
        		//     of 16 pixels of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=16,y=0),
        		//     8 byte pairs of (Cb,Cr) are stored sequentially in the buffer
        		//     (representing 16 horizontal pixels),
        		//     then the next 3 rows are being skipped,
        		//     and then followed by 8 byte pairs of the next image row (i.e. every 4th row).
        		//     The rows are stored from the image top to the image bottom.
        		//     CbCr[x=16,y=0], CbCr[x=18,y=0], CbCr[x=20,y=0], CbCr[x=22,y=0], CbCr[x=24,y=0], CbCr[x=26,y=0], CbCr[x=28,y=0], CbCr[x=30,y=0]
        		//     CbCr[x=48,y=0], CbCr[x=50,y=0], CbCr[x=52,y=0], CbCr[x=54,y=0], CbCr[x=56,y=0], CbCr[x=58,y=0], CbCr[x=60,y=0], CbCr[x=62,y=0]
        		//     ...
        		//     CbCr[x=16,y=4], CbCr[x=18,y=4], CbCr[x=20,y=4], CbCr[x=22,y=4], CbCr[x=24,y=4], CbCr[x=26,y=4], CbCr[x=28,y=4], CbCr[x=30,y=4]
        		//     CbCr[x=48,y=4], CbCr[x=50,y=4], CbCr[x=52,y=4], CbCr[x=54,y=4], CbCr[x=56,y=4], CbCr[x=58,y=4], CbCr[x=60,y=4], CbCr[x=62,y=4]
        		//     ...
        		// - buffer7:
        		//     storing the Cb and Cr information of the second block
        		//     of 16 pixels of a 32 pixels wide vertical band.
        		//     Starting at the image pixel (x=16,y=2),
        		//     8 byte pairs of (Cb,Cr) are stored sequentially in the buffer
        		//     (representing 16 horizontal pixels),
        		//     then the next 3 rows are being skipped,
        		//     and then followed by 8 byte pairs of the next image row (i.e. every 4th row).
        		//     The rows are stored from the image top to the image bottom.
        		//     CbCr[x=16,y=2], CbCr[x=18,y=2], CbCr[x=20,y=2], CbCr[x=22,y=2], CbCr[x=24,y=2], CbCr[x=26,y=2], CbCr[x=28,y=2], CbCr[x=30,y=2]
        		//     CbCr[x=48,y=2], CbCr[x=50,y=2], CbCr[x=52,y=2], CbCr[x=54,y=2], CbCr[x=56,y=2], CbCr[x=58,y=2], CbCr[x=60,y=2], CbCr[x=62,y=2]
        		//     ...
        		//     CbCr[x=16,y=6], CbCr[x=18,y=6], CbCr[x=20,y=6], CbCr[x=22,y=6], CbCr[x=24,y=6], CbCr[x=26,y=6], CbCr[x=28,y=6], CbCr[x=30,y=6]
        		//     CbCr[x=48,y=6], CbCr[x=50,y=6], CbCr[x=52,y=6], CbCr[x=54,y=6], CbCr[x=56,y=6], CbCr[x=58,y=6], CbCr[x=60,y=6], CbCr[x=62,y=6]
        		//     ...
        		int width2 = width / 2;
        		int height2 = height / 2;
        		// Reserve a buffer large enough for all sections
        		int[] buffer = getIntBuffer(((width + 31) >> 5) * ((height + 1) >> 1) * 16);
        		int n = 0;
        		for (int x = 0; x < width; x += 32) {
        			for (int y = 0, i = x; y < height; y += 2, n += 16, i += 2 * width) {
        				System.arraycopy(luma, i, buffer, n, 16);
        			}
        		}
        		write(buffers[buffersIndex][0], n, buffer, 0);

        		n = 0;
        		for (int x = 16; x < width; x += 32) {
        			for (int y = 0, i = x; y < height; y += 2, n += 16, i += 2 * width) {
        				System.arraycopy(luma, i, buffer, n, 16);
        			}
        		}
        		write(buffers[buffersIndex][1], n, buffer, 0);

        		n = 0;
        		for (int x = 0; x < width2; x += 16) {
    				for (int y = 0; y < height2; y += 2) {
    					for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
    						buffer[n++] = cb[i];
    						buffer[n++] = cr[i];
    					}
        			}
        		}
        		write(buffers[buffersIndex][4], n, buffer, 0);

        		n = 0;
        		for (int x = 0; x < width2; x += 16) {
    				for (int y = 1; y < height2; y += 2) {
    					for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
    						buffer[n++] = cb[i];
    						buffer[n++] = cr[i];
    					}
        			}
        		}
        		write(buffers[buffersIndex][5], n, buffer, 0);

        		n = 0;
        		for (int x = 0; x < width; x += 32) {
        			for (int y = 1, i = x + width; y < height; y += 2, n += 16, i += 2 * width) {
        				System.arraycopy(luma, i, buffer, n, 16);
        			}
        		}
        		write(buffers[buffersIndex][2], n, buffer, 0);

        		n = 0;
        		for (int x = 16; x < width; x += 32) {
        			for (int y = 1, i = x + width; y < height; y += 2, n += 16, i += 2 * width) {
        				System.arraycopy(luma, i, buffer, n, 16);
        			}
        		}
        		write(buffers[buffersIndex][3], n, buffer, 0);

        		n = 0;
        		for (int x = 8; x < width2; x += 16) {
    				for (int y = 0; y < height2; y += 2) {
    					for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
    						buffer[n++] = cb[i];
    						buffer[n++] = cr[i];
    					}
        			}
        		}
        		write(buffers[buffersIndex][6], n, buffer, 0);

        		n = 0;
        		for (int x = 8; x < width2; x += 16) {
    				for (int y = 1; y < height2; y += 2) {
    					for (int xx = 0, i = y * width2 + x; xx < 8; xx++, i++) {
    						buffer[n++] = cb[i];
    						buffer[n++] = cr[i];
    					}
        			}
        		}
        		write(buffers[buffersIndex][7], n, buffer, 0);

        		releaseIntBuffer(buffer);
        	}
            releaseIntBuffer(luma);
            releaseIntBuffer(cb);
            releaseIntBuffer(cr);

        	for (int i = 0; i < 8; i++) {
		    	mpegAvcYuvStruct.setValue32(i * 4, buffers[buffersIndex][i].getAddress() & EDRAM_MEMORY_MASK);
		    	if (log.isTraceEnabled()) {
		    		log.trace(String.format("sceVideocodecDecode YUV buffer[%d]=%s", i, buffers[buffersIndex][i]));
		    	}
        	}

        	mpegAvcYuvStruct.setValue32(32, videoCodec.hasImage()); // 0 or 1

        	TPointer bufferUnknown1 = mpegAvcYuvStruct.getPointer(36);
        	if (bufferUnknown1.isNull()) {
        		bufferUnknown1 = defaultBufferUnknown1;
        		mpegAvcYuvStruct.setPointer(36, bufferUnknown1);
        	}
        	bufferUnknown1.setUnsignedValue8(0, 0x02); // 0x00 or 0x04
        	bufferUnknown1.setValue32(8, sceMpeg.mpegTimestampPerSecond);
        	bufferUnknown1.setValue32(16, sceMpeg.mpegTimestampPerSecond);
        	bufferUnknown1.setValue32(24, frameCount * 2);
        	bufferUnknown1.setValue32(28, 2);
        	bufferUnknown1.setUnsignedValue8(32, 0x00); // 0x00 or 0x01 or 0x02
        	bufferUnknown1.setUnsignedValue8(33, 0x01);

        	TPointer bufferUnknown2 = mpegAvcYuvStruct.getPointer(40);
        	if (bufferUnknown2.isNull()) {
        		bufferUnknown2 = defaultBufferUnknown2;
        		mpegAvcYuvStruct.setPointer(40, bufferUnknown2);
        	}
        	bufferUnknown2.setUnsignedValue8(0, 0x00); // 0x00 or 0x04
        	bufferUnknown2.setValue32(24, 0);
        	bufferUnknown2.setValue32(28, 0);

			buffer3.setValue8(0, (byte) 0x01);
			buffer3.setValue8(1, (byte) 0xFF);
        	buffer3.setValue32(4, 3);
        	buffer3.setValue32(8, 4);
        	buffer3.setValue32(12, 1);
        	buffer3.setValue8(16, (byte) 0);
        	buffer3.setValue32(20, 0x10000);
        	buffer3.setValue32(32, 4004); // 4004 or 5005
        	buffer3.setValue32(36, 240000);

        	frameCount++;

        	videocodecGetSEIType0(decodeSEI);
    	}

    	return result;
	}

    public void videocodecGetSEIType0(TPointer decodeSEI) {
    	if (decodeSEI.isNotNull()) {
	    	decodeSEI.setValue8(0, (byte) 0x02);
	    	decodeSEI.setValue32(8, sceMpeg.mpegTimestampPerSecond);
	    	decodeSEI.setValue32(16, sceMpeg.mpegTimestampPerSecond);
	    	decodeSEI.setValue32(24, (frameCount - 1) * 2);
	    	decodeSEI.setValue32(28, 2);
	    	decodeSEI.setValue8(32, (byte) 0x00);
	    	decodeSEI.setValue8(33, (byte) 0x01);
    	}
	}

	public int videocodecDecodeType1(Memory mp4Memory, int mp4Data, int mp4Size, TPointer buffer2, int value) {
		int result = videocodecDecode(mp4Memory, mp4Data, mp4Size);

    	int frameWidth = videoCodec.getImageWidth();
    	int frameHeight = videoCodec.getImageHeight();

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceVideocodecDecode codec image size %dx%d, frame size %dx%d", videoCodec.getImageWidth(), videoCodec.getImageHeight(), frameWidth, frameHeight));
    	}
    	int frameBufferWidthY = videoCodec.getImageWidth();
    	int frameBufferWidthCr = frameBufferWidthY / 2;
    	int frameBufferWidthCb = frameBufferWidthY / 2;

		if (videoCodec.hasImage()) {
        	if (memoryInfo == null) {
        		int sizeY = frameBufferWidthY * frameHeight;
        		int sizeCr = frameBufferWidthCr * (frameHeight / 2);
        		int sizeCb = frameBufferWidthCr * (frameHeight / 2);
        		int size = (sizeY + sizeCr + sizeCb) * 2;

        		memoryInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceVideocodecDecode", SysMemUserForUser.PSP_SMEM_Low, size, 0);

        		bufferY1 = memoryInfo.addr & EDRAM_MEMORY_MASK;
        		bufferY2 = bufferY1 + sizeY;
        		bufferCr1 = bufferY1 + sizeY;
        		bufferCb1 = bufferCr1 + sizeCr;
        		bufferCr2 = bufferY2 + sizeY;
        		bufferCb2 = bufferCr2 + sizeCr;
        	}
		}

    	boolean buffer1 = (frameCount & 1) == 0;
    	int bufferY = buffer1 ? bufferY1 : bufferY2;
    	int bufferCr = buffer1 ? bufferCr1 : bufferCr2;
    	int bufferCb = buffer1 ? bufferCb1 : bufferCb2;

    	if (videoCodec.hasImage()) {
    		Memory mem = Memory.getInstance();
        	mem.memset(bufferY | MemoryMap.START_RAM, (byte) 0x80, frameBufferWidthY * frameHeight);
        	mem.memset(bufferCr | MemoryMap.START_RAM, (byte) (buffer1 ? 0x50 : 0x80), frameBufferWidthCr * (frameHeight / 2));
        	mem.memset(bufferCb | MemoryMap.START_RAM, (byte) 0x80, frameBufferWidthCb * (frameHeight / 2));

        	frameCount++;
    	}

    	buffer2.setValue32(0, mp4Data);
    	buffer2.setValue32(4, mp4Size);
    	buffer2.setValue32(8, value);
    	buffer2.setValue32(12, 0x40);
    	buffer2.setValue32(16, 0);
    	buffer2.setValue32(44, mp4Size);
    	buffer2.setValue32(48, frameWidth);
    	buffer2.setValue32(52, frameHeight);
    	buffer2.setValue32(60, videoCodec.hasImage() ? 2 : 1);
    	buffer2.setValue32(64, 1);
    	buffer2.setValue32(72, -1);
    	buffer2.setValue32(76, frameCount * 0x64);
    	buffer2.setValue32(80, 2997);
    	buffer2.setValue32(84, bufferY);
    	buffer2.setValue32(88, bufferCr);
    	buffer2.setValue32(92, bufferCb);
    	buffer2.setValue32(96, frameBufferWidthY);
    	buffer2.setValue32(100, frameBufferWidthCr);
    	buffer2.setValue32(104, frameBufferWidthCb);

    	return result;
	}

	private void videocodecDecoderStep(TPointer buffer, int type, IAction afterDecodeAction, int threadUid, long threadWakeupMicroTime) {
    	if (buffer == null) {
    		return;
    	}

    	Memory mp4Memory = buffer.getMemory();
    	int mp4Data = buffer.getValue32(36) | MemoryMap.START_RAM;
    	int mp4Size = buffer.getValue32(40);

    	buffer.setValue32(8, 0);

    	TPointer buffer2 = buffer.getPointer(16);
    	switch (type) {
    		case 0:
            	TPointer mpegAvcYuvStruct = buffer.getPointer(44);
            	TPointer buffer3 = buffer.getPointer(48);
            	TPointer decodeSEI = buffer.getPointer(80);
    			videocodecDecodeType0(mp4Memory, mp4Data, mp4Size, buffer2, mpegAvcYuvStruct, buffer3, decodeSEI);
		    	break;
    		case 1:
    			videocodecDecodeType1(mp4Memory, mp4Data, mp4Size, buffer2, buffer.getValue32(56));
		    	break;
	    	default:
	    		log.warn(String.format("sceVideocodecDecode unknown type=0x%X", type));
	    		break;
    	}

    	if (afterDecodeAction != null) {
    		afterDecodeAction.execute();
    	}

    	IAction action;
    	long delayMicros = threadWakeupMicroTime - Emulator.getClock().microTime();
    	if (delayMicros > 0L) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Further delaying thread=0x%X by %d microseconds", threadUid, delayMicros));
    		}
    		action = new DelayThreadAction(threadUid, (int) delayMicros, false, true);
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

    public static void write(TPointer addr, int length, int[] buffer, int offset) {
    	length = Math.min(length, buffer.length - offset);
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("write addr=%s, length=0x%X", addr, length));
    	}

    	// Optimize the most common case
    	Memory mem = addr.getMemory();
    	int address = addr.getAddress();
        if (mem.hasMemoryInt(address)) {
        	int length4 = length >> 2;
        	int addrOffset = mem.getMemoryIntOffset(address);
    		int[] memoryInt = mem.getMemoryInt(address);
	        for (int i = 0, j = offset; i < length4; i++) {
	        	int value = buffer[j++] & 0xFF;
	        	value += (buffer[j++] & 0xFF) << 8;
	        	value += (buffer[j++] & 0xFF) << 16;
	        	value += buffer[j++] << 24;
	        	memoryInt[addrOffset++] = value;
	        }
        } else {
        	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, length, 1);
        	for (int i = 0, j = offset; i < length; i++) {
        		memoryWriter.writeNext(buffer[j++] & 0xFF);
        	}
        	memoryWriter.flush();
        }
    }

    @HLEFunction(nid = 0xC01EC829, version = 150)
    public int sceVideocodecOpen(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	TPointer buffer2 = buffer.getPointer(16);

    	buffer.setValue32(0, 0x05100601);

    	switch (type) {
    		case 0:
	        	buffer.setValue32(8, 1);
	        	buffer.setValue32(24, VIDEOCODEC_OPEN_TYPE0_UNKNOWN24);
	        	buffer.setValue32(32, VIDEOCODEC_OPEN_TYPE0_UNKNOWN4);

	        	buffer2.setValue32(0, VIDEOCODEC_OPEN_TYPE0_UNKNOWN0);
	        	buffer2.setValue32(4, VIDEOCODEC_OPEN_TYPE0_UNKNOWN4);
	        	break;
    		case 1:
    			buffer.setValue32(8, 0);
            	buffer.setValue32(24, VIDEOCODEC_OPEN_TYPE1_UNKNOWN24);
            	buffer.setValue32(32, VIDEOCODEC_OPEN_TYPE1_UNKNOWN32);
    			break;
			default:
	    		log.warn(String.format("sceVideocodecOpen unknown type %d", type));
	    		return -1;
    	}

    	if (videocodecDecoderThread == null) {
	    	videocodecDecoderThread = new VideocodecDecoderThread();
	    	videocodecDecoderThread.setDaemon(true);
	    	videocodecDecoderThread.setName("Videocodec Decoder Thread");
	    	videocodecDecoderThread.start();
    	}

    	return 0;
    }

    public int hleVideocodecDecode(TPointer buffer, int type, IAction afterDecodeAction) {
		int threadUid = Modules.ThreadManForUserModule.getCurrentThreadID();
		Modules.ThreadManForUserModule.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_VIDEO_DECODER);
    	videocodecDecoderThread.trigger(buffer, type, afterDecodeAction, threadUid, Emulator.getClock().microTime() + videocodecDecodeDelay);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA2F0564E, version = 150)
    public int sceVideocodecStop(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	if (videoCodec != null) {
    		videoCodec = null;
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x17099F0A, version = 150)
    public int sceVideocodecInit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	buffer.setValue32(12, buffer.getValue32(20) + 8);

    	return 0;
    }

    @HLEFunction(nid = 0x2D31F5B1, version = 150)
    public int sceVideocodecGetEDRAM(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	int size = (buffer.getValue32(24) + 63) | 0x3F;
    	edramInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceVideocodecEDRAM", SysMemUserForUser.PSP_SMEM_Low, size, 0);
    	if (edramInfo == null) {
    		return -1;
    	}

    	int addrEDRAM = edramInfo.addr & EDRAM_MEMORY_MASK;
    	buffer.setValue32(20, alignUp(addrEDRAM, 63));
    	buffer.setValue32(92, addrEDRAM);

    	return 0;
    }

    @HLEFunction(nid = 0x4F160BF4, version = 150)
    public int sceVideocodecReleaseEDRAM(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer) {
    	buffer.setValue32(20, 0);
    	buffer.setValue32(92, 0);

    	if (edramInfo != null) {
    		Modules.SysMemUserForUserModule.free(edramInfo);
    		edramInfo = null;
    	}

    	return 0;
    }

    @HLEFunction(nid = 0xDBA273FA, version = 150)
    public int sceVideocodecDecode(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	if (type != 0 && type != 1) {
    		log.warn(String.format("sceVideocodecDecode unknown type=0x%X", type));
    		return -1;
    	}

    	return hleVideocodecDecode(buffer, type, null);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x17CF7D2C, version = 150)
    public int sceVideocodecGetFrameCrop() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x26927D19, version = 150)
    public int sceVideocodecGetVersion(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	// This is the value returned on my PSP according to JpcspTrace.
    	buffer.setValue32(4, 0x78);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2F385E7F, version = 150)
    public int sceVideocodecScanHeader() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x307E6E1C, version = 150)
    public int sceVideocodecDelete(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	videocodecDelete();

    	Modules.ThreadManForUserModule.hleKernelDelayThread(videocodecDeleteDelay, false);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x627B7D42, version = 150)
    public int sceVideocodecGetSEI(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	TPointer decodeSEI = buffer.getPointer(80);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceVideocodecGetSEI storing decodeSEI to %s", decodeSEI));
    	}
    	decodeSEI.setValue32(28, 0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x745A7B7A, version = 150)
    public int sceVideocodecSetMemory(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=videocodecBufferSize, usage=Usage.inout) TPointer buffer, int type) {
    	int unknown1 = buffer.getValue32(64);
    	int unknown2 = buffer.getValue32(68);
    	int unknown3 = buffer.getValue32(72);
    	int unknown4 = buffer.getValue32(76);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceVideocodecSetMemory unknown1=0x%08X, unknown2=0x%08X, unknown3=0x%08X, unknown4=0x%08X", unknown1, unknown2, unknown3, unknown4));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x893B32B1, version = 150)
    public int sceVideocodec_893B32B1() {
    	return 0;
    }

    @HLEFunction(nid = 0xD95C24D5, version = 150)
    public int sceVideocodec_D95C24D5(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=56, usage=Usage.in) TPointer buffer) {
    	int width = buffer.getValue32(0);
    	int height = buffer.getValue32(4);
    	if (((width | height) & 0xF) != 0 || width == 0 || height == 0 || width > 720 || height > 576) {
    		return ERROR_AVC_INVALID_VALUE;
    	}

    	// Read input the Y, Cb and Cr data which is stored
    	// in the format produced by sceVideocodec,
    	// i.e. using 8 buffers.
    	int size0;
		int size1 = (height >> 1) * (width >> 1);
    	if ((width & 0x10) == 0) {
	        size0 = size1;
    	} else {
    		size0 = (height >> 1) * ((width + 32) >> 1);
    	}
    	int size2 = size0 >> 1;
		int size3 = size1 >> 1;

	    TPointer buffer0 = buffer.getPointer(12);
	    TPointer buffer1 = buffer.getPointer(28);
	    TPointer buffer2 = buffer.getPointer(16);
	    TPointer buffer3 = buffer.getPointer(32);
	    TPointer buffer4 = buffer.getPointer(20);
	    TPointer buffer5 = buffer.getPointer(36);
	    TPointer buffer6 = buffer.getPointer(24);
	    TPointer buffer7 = buffer.getPointer(40);

	    if (log.isDebugEnabled()) {
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer0=%s, size=0x%X", buffer0, size0));
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer1=%s, size=0x%X", buffer1, size0));
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer2=%s, size=0x%X", buffer2, size1));
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer3=%s, size=0x%X", buffer3, size1));
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer4=%s, size=0x%X", buffer4, size2));
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer5=%s, size=0x%X", buffer5, size2));
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer6=%s, size=0x%X", buffer6, size3));
	    	log.debug(String.format("sceVideocodec_D95C24D5 input buffer7=%s, size=0x%X", buffer7, size3));
	    }

	    YCbCrImageState imageState = new YCbCrImageState();
	    sceMpegbase.read(imageState, width, height, buffer0.getMemory(), buffer0.getAddress(), buffer1.getAddress(), buffer2.getAddress(), buffer3.getAddress(), buffer4.getAddress(), buffer5.getAddress(), buffer6.getAddress(), buffer7.getAddress());

	    // Write the output Y, Cb and Cr data into the format
	    // used by sceJpegCsc, i.e. using only 3 buffers where
	    // the data is written sequentially instead of using
	    // vertical pixel bands.
		int sizeY = width * height;
		int sizeCbCr = sizeY >> 2;
	    if ((width & 0x70) == 0x70) {
	    	sizeY += height << 4;
	    	sizeCbCr += height << 2;
	    }

	    TPointer bufferY = buffer.getPointer(44);
	    TPointer bufferCb = buffer.getPointer(48);
	    TPointer bufferCr = buffer.getPointer(52);

	    if (log.isDebugEnabled()) {
	    	log.debug(String.format("sceVideocodec_D95C24D5 output bufferY=%s, size=0x%X", bufferY, sizeY));
	    	log.debug(String.format("sceVideocodec_D95C24D5 output bufferCb=%s, size=0x%X", bufferCb, sizeCbCr));
	    	log.debug(String.format("sceVideocodec_D95C24D5 output bufferCr=%s, size=0x%X", bufferCr, sizeCbCr));
	    }

	    sceMpegbase.write(bufferY, sizeY, imageState.luma, 0);
	    sceMpegbase.write(bufferCb, sizeCbCr, imageState.cb, 0);
	    sceMpegbase.write(bufferCr, sizeCbCr, imageState.cr, 0);

        imageState.releaseIntBuffers();

	    return 0;
    }
}
