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
package jpcsp.HLE.modules271;

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.RE.IRenderingEngine.sizeOfTextureType;
import static jpcsp.memory.ImageReader.colorARGBtoABGR;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.imageio.ImageIO;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.graphics.VideoEngine;
import jpcsp.hardware.Screen;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class sceJpeg extends HLEModule {
    protected static Logger log = Modules.getLogger("sceJpeg");

	@Override
	public String getName() {
		return "sceJpeg";
	}

	protected int jpegWidth = Screen.width;
	protected int jpegHeight = Screen.height;
	protected HashMap<Integer, BufferedImage> bufferedImages;
	protected static final String uidPurpose = "sceJpeg-BufferedImage";

	@Override
	public void start() {
		bufferedImages = new HashMap<Integer, BufferedImage>();
		super.start();
	}

	@Override
	public void stop() {
		bufferedImages.clear();
		super.stop();
	}

	protected BufferedImage readJpegImage(TPointer jpegBuffer, int jpegBufferSize) {
		BufferedImage bufferedImage = null;
		byte[] buffer = readJpegImageBytes(jpegBuffer, jpegBufferSize);
		InputStream imageInputStream = new ByteArrayInputStream(buffer);
		try {
			bufferedImage = ImageIO.read(imageInputStream);
			imageInputStream.close();
		} catch (IOException e) {
			log.error("Error reading Jpeg image", e);
		}

		return bufferedImage;
	}

	protected int addImage(TPointer jpegBuffer, int jpegBufferSize, TPointer yCbCrBuffer) {
		int result = getWidthHeight(jpegWidth, jpegHeight);

		BufferedImage bufferedImage = readJpegImage(jpegBuffer, jpegBufferSize);
		if (bufferedImage != null) {
			int uid = SceUidManager.getNewUid(uidPurpose);
			bufferedImages.put(uid, bufferedImage);

			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();
			result = getWidthHeight(width, height);

			// Store the uid in the yCbCrBuffer so that we can retrieve it while decoding
			yCbCrBuffer.setValue32(0, uid);
			// Set the starting line for decoding
			yCbCrBuffer.setValue32(4, 0);
		}

		return result;
	}

	protected void deleteImage(TPointer yCbCrBuffer) {
		int uid = yCbCrBuffer.getValue32();
		yCbCrBuffer.setValue32(0);

		bufferedImages.remove(uid);

		SceUidManager.releaseUid(uid, uidPurpose);
	}

	protected static int getWidthHeight(int width, int height) {
		return (width << 16) | height;
	}

	protected byte[] readJpegImageBytes(TPointer jpegBuffer, int jpegBufferSize) {
		byte[] buffer = new byte[jpegBufferSize];
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(jpegBuffer.getAddress(), jpegBufferSize, 1);
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) memoryReader.readNext();
		}

		return buffer;
	}

	protected void dumpJpegImage(TPointer jpegBuffer, int jpegBufferSize) {
		byte[] buffer = readJpegImageBytes(jpegBuffer, jpegBufferSize);
		try {
			OutputStream os = new FileOutputStream(String.format("%s%cImage%08X.jpeg", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, jpegBuffer.getAddress()));
			os.write(buffer, 0, buffer.length);
			os.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	protected void decodeImage(TPointer imageBuffer, BufferedImage bufferedImage, int width, int height, int bufferWidth, int pixelFormat, int startLine) {
		int bytesPerPixel = sizeOfTextureType[pixelFormat];
		int lineWidth = Math.min(width, bufferWidth);
		int skipEndOfLine = Math.max(0, bufferWidth - lineWidth); 
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(imageBuffer.getAddress(), height * bufferWidth * bytesPerPixel, bytesPerPixel);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = bufferedImage.getRGB(x, y + startLine);
				int abgr = colorARGBtoABGR(argb);
				memoryWriter.writeNext(abgr);
			}
			memoryWriter.skip(skipEndOfLine);
		}
		memoryWriter.flush();

		VideoEngine.getInstance().addVideoTexture(imageBuffer.getAddress(), imageBuffer.getAddress() + bufferWidth * height * sceDisplay.getPixelFormatBytes(pixelFormat));
	}

	protected void generateFakeImage(TPointer imageBuffer, int width, int height, int bufferWidth, int pixelFormat) {
		sceMpeg.generateFakeImage(imageBuffer.getAddress(), bufferWidth, width, height, pixelFormat);
		VideoEngine.getInstance().addVideoTexture(imageBuffer.getAddress(), imageBuffer.getAddress() + bufferWidth * height * sceDisplay.getPixelFormatBytes(pixelFormat));
	}

	protected void decodeImage(TPointer imageBuffer, TPointer yCbCrBuffer, int width, int height, int bufferWidth, int pixelFormat) {
		int uid = yCbCrBuffer.getValue32(0);
		if (!bufferedImages.containsKey(uid)) {
			// Return a fake image
			generateFakeImage(imageBuffer, width, height, bufferWidth, pixelFormat);
		} else {
			BufferedImage bufferedImage = bufferedImages.get(uid);
			int startLine = yCbCrBuffer.getValue32(4);
			decodeImage(imageBuffer, bufferedImage, width, height, bufferWidth, pixelFormat, startLine);

			int nextStartLine = startLine + height;
			yCbCrBuffer.setValue32(4, nextStartLine);
			if (nextStartLine >= bufferedImage.getHeight()) {
				deleteImage(yCbCrBuffer);
			}
		}
	}

	@HLEFunction(nid = 0x04B5AE02, version = 271)
	public int sceJpegMJpegCsc(TPointer imageBuffer, TPointer yCbCrBuffer, int widthHeight, int bufferWidth) {
		int height = widthHeight & 0xFFF;
		int width = (widthHeight >> 16) & 0xFFF;

		log.warn(String.format("PARTIAL sceJpegMJpegCsc imageBuffer=%s, yCbCrBuffer=%s, widthHeight=0x%08X(width=%d, height=%d), bufferWidth=%d", imageBuffer, yCbCrBuffer, widthHeight, width, height, bufferWidth));

		decodeImage(imageBuffer, yCbCrBuffer, width, height, bufferWidth, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);

        return 0;
	}

	/**
	 * Deletes the current decoder context.
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEFunction(nid = 0x48B602B7, version = 271)
	public int sceJpegDeleteMJpeg() {
		log.warn(String.format("Ignoring sceJpegDeleteMJpeg"));

		return 0;
	}

	/**
	 * Finishes the MJpeg library
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEFunction(nid = 0x7D2F3D7F, version = 271)
	public int sceJpegFinishMJpeg() {
		log.warn(String.format("Ignoring sceJpegFinishMJpeg"));

		return 0;
	}

	@HLEFunction(nid = 0x91EED83C, version = 271)
	public int sceJpegDecodeMJpegYCbCr(TPointer jpegBuffer, int jpegBufferSize, TPointer yCbCrBuffer, int yCbCrBufferSize, int unknown) {
		log.warn(String.format("Unimplemented sceJpegDecodeMJpegYCbCr jpegBuffer=%s, jpegBufferSize=%d, yCbCrBuffer=%s, yCbCrBufferSize=%d, unknown=%d", jpegBuffer, jpegBufferSize, yCbCrBuffer, yCbCrBufferSize, unknown));
		if (log.isTraceEnabled()) {
			log.trace(String.format("sceJpegDecodeMJpegYCbCr jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
		}

		// Return size of image
		return addImage(jpegBuffer, jpegBufferSize, yCbCrBuffer);
	}

	/**
	 * Creates the decoder context.
	 *
	 * @param wantedWidth - The width of the frame
	 * @param height - The height of the frame
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEFunction(nid = 0x9D47469C, version = 271)
	public int sceJpegCreateMJpeg(int width, int height) {
		log.warn(String.format("Unimplemented sceJpegCreateMJpeg width=%d, height=%d", width, height));

		jpegWidth = width;
		jpegHeight = height;

		return 0;
	}

	/**
	 * Inits the MJpeg library
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEFunction(nid = 0xAC9E70E6, version = 271)
	public int sceJpegInitMJpeg() {
		log.warn(String.format("Ignoring sceJpegInitMJpeg"));

		return 0;
	}

	/**
	 * Decodes a mjpeg frame.
	 *
	 * @param jpegbuf - the buffer with the mjpeg frame
	 * @param size - size of the buffer pointed by jpegbuf
	 * @param rgba - buffer where the decoded data in RGBA format will be stored.
	 *                                     It should have a size of (width * height * 4).
	 * @param unk - Unknown, pass 0
	 *
	 * @return (width * 65536) + height on success, < 0 on error
	 */
	@HLEFunction(nid = 0x04B93CEF, version = 271)
	public int sceJpegDecodeMJpeg(TPointer jpegBuffer, int jpegBufferSize, TPointer imageBuffer, int unknown) {
		log.warn(String.format("Unimplemented sceJpegDecodeMJpeg jpegBuffer=%s, jpegBufferSize=%d, imageBuffer=%s, unknown=%d", jpegBuffer, jpegBufferSize, imageBuffer, unknown));
		if (log.isTraceEnabled()) {
			log.trace(String.format("sceJpegDecodeMJpeg jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
		}

		int width;
		int height;
		int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
		BufferedImage bufferedImage = readJpegImage(jpegBuffer, jpegBufferSize);
		if (bufferedImage == null) {
			width = jpegWidth;
			height = jpegHeight;
			generateFakeImage(imageBuffer, width, height, width, pixelFormat);
		} else {
			width = bufferedImage.getWidth();
			height = bufferedImage.getHeight();
			decodeImage(imageBuffer, bufferedImage, width, height, width, pixelFormat, 0);
		}

		// Return size of image
		return getWidthHeight(width, height);
	}

	@HLEFunction(nid = 0x8F2BB012, version = 271)
	public int sceJpeg_8F2BB012(TPointer jpegBuffer, int jpegBufferSize, @CanBeNull TPointer32 unknown1, int unknown2) {
		log.warn(String.format("Unimplemented sceJpeg_8F2BB012 jpegBuffer=%s, jpegBufferSize=%d, unknown1=%s, unknown2=%d", jpegBuffer, jpegBufferSize, unknown1, unknown2));
		if (log.isTraceEnabled()) {
			log.trace(String.format("sceJpeg_8F2BB012 jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize)));
			dumpJpegImage(jpegBuffer, jpegBufferSize);
		}

		unknown1.setValue(0x00020202);

		int yCbCrBufferSize = 0xC000;

		return yCbCrBufferSize;
	}

	/**
	 * Unknown function.
	 * Used in relation with sceMpegAvcConvertToYuv420. Maybe converting a Yuv420 image to ABGR888?
	 * 
	 * @param imageBuffer
	 * @param yCbCrBuffer
	 * @param widthHeight
	 * @param bufferWidth
	 * @param unknown
	 * @return
	 */
	@HLEFunction(nid = 0x67F0ED84, version = 271)
	public int sceJpeg_67F0ED84(TPointer imageBuffer, TPointer yCbCrBuffer, int widthHeight, int bufferWidth, int unknown) {
		int height = widthHeight & 0xFFF;
		int width = (widthHeight >> 16) & 0xFFF;

		log.warn(String.format("PARTIAL sceJpeg_67F0ED84 imageBuffer=%s, yCbCrBuffer=%s, widthHeight=0x%08X(width=%d, height=%d), bufferWidth=%d, unknown=0x%08X", imageBuffer, yCbCrBuffer, widthHeight, width, height, bufferWidth, unknown));

		decodeImage(imageBuffer, yCbCrBuffer, width, height, bufferWidth, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);

        return 0;
	}
}