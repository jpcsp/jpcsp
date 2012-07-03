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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.graphics.VideoEngine;
import jpcsp.hardware.Screen;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
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

	protected static int getWidthHeight(int width, int height) {
		return (width << 16) | height;
	}

	protected void dumpJpegImage(TPointer jpegBuffer, int jpegBufferSize) {
		byte[] buffer = new byte[jpegBufferSize];
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(jpegBuffer.getAddress(), jpegBufferSize, 1);
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) memoryReader.readNext();
		}
		try {
			OutputStream os = new FileOutputStream(String.format("%s%cImage%08X.jpeg", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, jpegBuffer.getAddress()));
			os.write(buffer, 2, buffer.length - 2);
			os.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	@HLEFunction(nid = 0x04B5AE02, version = 271)
	public int sceJpegMJpegCsc(TPointer imageBuffer, TPointer yCbCrBuffer, int widthHeight, int bufferWidth) {
		int height = widthHeight & 0xFFF;
		int width = (widthHeight >> 16) & 0xFFF;
		int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;

		log.warn(String.format("Unimplemented sceJpegMJpegCsc imageBuffer=%s, yCbCrBuffer=%s, widthHeight=0x%08X(width=%d, height=%d), bufferWidth=%d", imageBuffer, yCbCrBuffer, widthHeight, width, height, bufferWidth));

		// Return a fake image
		sceMpeg.generateFakeImage(imageBuffer.getAddress(), bufferWidth, width, height, pixelFormat);
        VideoEngine.getInstance().addVideoTexture(imageBuffer.getAddress(), imageBuffer.getAddress() + bufferWidth * height * sceDisplay.getPixelFormatBytes(pixelFormat));

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
			log.trace(String.format("sceJpegDecodeMJpegYCbCr jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize, 4, 16)));
		}

		int width = jpegWidth;
		int height = jpegHeight;

		// Return size of image
		return getWidthHeight(width, height);
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
			log.trace(String.format("sceJpegDecodeMJpeg jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize, 4, 16)));
		}

		int width = jpegWidth;
		int height = jpegHeight;
		int bufferWidth = width;
		int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;

		// Return a fake image
		sceMpeg.generateFakeImage(imageBuffer.getAddress(), bufferWidth, width, height, pixelFormat);
        VideoEngine.getInstance().addVideoTexture(imageBuffer.getAddress(), imageBuffer.getAddress() + bufferWidth * height * sceDisplay.getPixelFormatBytes(pixelFormat));

		// Return size of image
		return getWidthHeight(width, height);
	}

	@HLEFunction(nid = 0x8F2BB012, version = 271)
	public int sceJpeg_8F2BB012(TPointer jpegBuffer, int jpegBufferSize, @CanBeNull TPointer32 unknown1, int unknown2) {
		log.warn(String.format("Unimplemented sceJpeg_8F2BB012 jpegBuffer=%s, jpegBufferSize=%d, unknown1=%s, unknown2=%d", jpegBuffer, jpegBufferSize, unknown1, unknown2));
		if (log.isTraceEnabled()) {
			log.trace(String.format("sceJpeg_8F2BB012 jpegBuffer: %s", Utilities.getMemoryDump(jpegBuffer.getAddress(), jpegBufferSize, 4, 16)));
			dumpJpegImage(jpegBuffer, jpegBufferSize);
		}

		if (!unknown1.isNull()) {
			unknown1.setValue(0x00020202);
		}

		int yCbCrBufferSize = 0xC000;

		return yCbCrBufferSize;
	}

	/**
	 * Unknown function.
	 * Used in relation with sceMpegAvcConvertToYuv420. Maybe converting a Yuv420 image to ABGR888?
	 * 
	 * @param unknown1
	 * @param sourceBuffer
	 * @param widthHeight
	 * @param unknown4
	 * @param unknown5
	 * @return
	 */
	@HLEFunction(nid = 0x67F0ED84, version = 271)
	public int sceJpeg_67F0ED84(TPointer imageBuffer, TPointer sourceBuffer, int widthHeight, int bufferWidth, int unknown) {
		int height = widthHeight & 0xFFF;
		int width = (widthHeight >> 16) & 0xFFF;
		int pixelFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;

		log.warn(String.format("Unimplemented sceJpeg_67F0ED84 imageBuffer=%s, sourceBuffer=%s, widthHeight=0x%08X(width=%d, height=%d), bufferWidth=%d, unknown=0x%08X", imageBuffer, sourceBuffer, widthHeight, width, height, bufferWidth, unknown));

		// Return a fake image
		sceMpeg.generateFakeImage(imageBuffer.getAddress(), bufferWidth, width, height, pixelFormat);
        VideoEngine.getInstance().addVideoTexture(imageBuffer.getAddress(), imageBuffer.getAddress() + bufferWidth * height * sceDisplay.getPixelFormatBytes(pixelFormat));

        return 0;
	}
}